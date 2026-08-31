-- =====================================================================================
-- ACUERDOS DE CONDONACION — pagar cruzando con las cuentas de aportes
-- FECHA: 2026-08-30
--
-- REQUERIMIENTO (usuario, 2026-08-30): "En el acuerdo de pagos debe permitir cruzar con
-- los valores de sus cuentas también como en la pantalla de cobro, para que en ese momento
-- pueda cruzar con valores de sus cuentas de aportes más hacer un pago por depósito o
-- transferencia para cubrir todo el acuerdo de pago".
--
-- MODELO: el monto a pagar del acuerdo (ACCNVLPG) se compone de DOS FUENTES:
--   - ACCNVLAP: lo que se cubre cruzando saldos de aportes del propio socio.
--   - ACCNVLDP: lo que se cubre con depósito o transferencia.
--   Invariante: ACCNVLAP + ACCNVLDP = ACCNVLPG (tolerancia $0.01, validada en el servicio).
--
-- ⚠️ SOLO LA PARTE DE DEPOSITO GENERA COBRO EN CRD.CBCR Y APROBACION DE CONTABILIDAD.
-- Es la única donde entra dinero al banco, y por lo tanto la única que contabilidad puede
-- verificar. Mismo criterio que dejó `pagarConAportes` fuera del circuito. Si el acuerdo se
-- cubre ENTERO con aportes (ACCNVLDP = 0) no hay CBCR ni aprobación: no hay depósito que
-- verificar, y esperar no protegería de nada.
--
-- POR QUE HACE FALTA LA TABLA HIJA, y no alcanzan los dos totales: para ejecutar el cruce
-- hay que saber DE QUE TIPOS DE APORTE y CUANTO de cada uno. Con solo el total, esa
-- composición se pierde entre el registro y el proceso — y el proceso ocurre después, con
-- la aprobación de contabilidad en el medio. Es el mismo desglose que hoy viaja en
-- SolicitudPagoConAportes.aportes.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS
-- =====================================================================================

-- 0.1 CRD.ACCN debe existir (DDL-ACUERDOS-PAGO-CONDONACION.sql ya corrido). Esperado: 1 fila.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME = 'ACCN';

-- 0.2 Las columnas nuevas NO deben existir. Esperado: 0 filas.
SELECT c.COLUMN_NAME FROM ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'ACCN'
AND    c.COLUMN_NAME IN ('ACCNVLAP', 'ACCNVLDP');

-- 0.3 La tabla nueva NO debe existir, en NINGUN esquema. Esperado: 0 filas.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t WHERE t.TABLE_NAME = 'DAAP';


-- =====================================================================================
-- 1. CRD.ACCN — las dos fuentes del monto a pagar
-- =====================================================================================
-- Nullable con DEFAULT 0: los acuerdos que ya existan (si los hubiera) quedan como
-- 100% depósito, que es como se registraron.

ALTER TABLE CRD.ACCN ADD (
    ACCNVLAP NUMBER(18,2) DEFAULT 0,
    ACCNVLDP NUMBER(18,2) DEFAULT 0
);

ALTER TABLE CRD.ACCN ADD CONSTRAINT CK_ACCN_VLAP CHECK (ACCNVLAP >= 0);
ALTER TABLE CRD.ACCN ADD CONSTRAINT CK_ACCN_VLDP CHECK (ACCNVLDP >= 0);

COMMENT ON COLUMN CRD.ACCN.ACCNVLAP IS
    'Parte de ACCNVLPG que se cubre cruzando saldos de aportes del socio. Su composicion por tipo de aporte esta en CRD.DAAP. NO genera cobro en CBCR: ahi no entra dinero al banco.';
COMMENT ON COLUMN CRD.ACCN.ACCNVLDP IS
    'Parte de ACCNVLPG que se cubre con deposito o transferencia. ES LA UNICA que genera cobro en CRD.CBCR y aprobacion de contabilidad. Si vale 0, el acuerdo se aplica sin pasar por la bandeja.';

-- ⚠️ NO se agrega un CHECK de ACCNVLAP + ACCNVLDP = ACCNVLPG a proposito: los tres se
-- calculan y graban juntos en registrarAcuerdo, y un CHECK aritmetico sobre importes con
-- redondeo a 2 decimales rechaza filas legitimas por diferencias de centavos. La igualdad
-- (tolerancia $0.01) la valida el servicio, como ya hace con la cabecera contra el detalle.


-- =====================================================================================
-- 2. CRD.DAAP — desglose por tipo de aporte del cruce
-- =====================================================================================
-- Mismo patrón que CRD.DACC: D + las tres primeras letras de la cabecera no alcanzaba
-- (DACC ya está tomada por el detalle de conceptos), así que DAAP = Detalle de Aportes del
-- Acuerdo de Pago. Verificado libre en todo el proyecto el 2026-08-30.

CREATE SEQUENCE CRD.SQ_DAAPCDGO START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE CRD.DAAP (
    DAAPCDGO NUMBER          NOT NULL,   -- PK
    ACCNCDGO NUMBER          NOT NULL,   -- FK CRD.ACCN
    TPAPCDGO NUMBER          NOT NULL,   -- FK CRD.TPAP, el tipo de aporte que se consume
    DAAPVLOR NUMBER(18,2)    NOT NULL    -- cuanto se consume de ese tipo
);

ALTER TABLE CRD.DAAP ADD CONSTRAINT PK_DAAP PRIMARY KEY (DAAPCDGO);
ALTER TABLE CRD.DAAP ADD CONSTRAINT FK_DAAP_ACCN FOREIGN KEY (ACCNCDGO) REFERENCES CRD.ACCN(ACCNCDGO);
ALTER TABLE CRD.DAAP ADD CONSTRAINT FK_DAAP_TPAP FOREIGN KEY (TPAPCDGO) REFERENCES CRD.TPAP(TPAPCDGO);

ALTER TABLE CRD.DAAP ADD CONSTRAINT CK_DAAP_VLOR CHECK (DAAPVLOR > 0);
-- Un tipo de aporte no puede aparecer dos veces en el mismo acuerdo: si se consume de
-- cesantia, se consume UNA vez por el total. Es la misma regla que validarDesgloseAportes
-- ya aplica en pagarConAportes/precancelar.
ALTER TABLE CRD.DAAP ADD CONSTRAINT UK_DAAP_ACCN_TPAP UNIQUE (ACCNCDGO, TPAPCDGO);

-- Sin indice propio por ACCNCDGO: el UNIQUE ya crea uno con ACCNCDGO como columna lider.

COMMENT ON TABLE  CRD.DAAP IS
    'Desglose por tipo de aporte del cruce que cubre parte de un acuerdo de condonacion. Sin esto solo quedaria el total (ACCNVLAP) y se perderia DE DONDE sale el dinero entre el registro y el proceso, que ocurren en momentos distintos.';
COMMENT ON COLUMN CRD.DAAP.DAAPVLOR IS
    'Cuanto se consume de ese tipo de aporte. El saldo se revalida DENTRO de la transaccion al procesar (consumirAportes), no al registrar: entre los dos momentos el saldo pudo cambiar.';


-- =====================================================================================
-- 3. CONTROLES POSTERIORES
-- =====================================================================================

-- 3.1 Las dos columnas nuevas. Esperado: 2 filas, NULLABLE = 'Y', DATA_DEFAULT = 0.
SELECT c.COLUMN_NAME, c.DATA_TYPE, c.DATA_PRECISION, c.DATA_SCALE, c.NULLABLE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'ACCN'
AND    c.COLUMN_NAME IN ('ACCNVLAP', 'ACCNVLDP')
ORDER  BY c.COLUMN_NAME;

-- 3.2 La tabla y sus constraints. Esperado: 1 PK + 2 FK + 1 UNIQUE + 1 CHECK, todos ENABLED.
SELECT c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.STATUS, c.SEARCH_CONDITION
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'DAAP'
ORDER  BY c.CONSTRAINT_TYPE, c.CONSTRAINT_NAME;

-- 3.3 La secuencia existe.
SELECT s.SEQUENCE_NAME, s.LAST_NUMBER FROM ALL_SEQUENCES s
WHERE  s.SEQUENCE_OWNER = 'CRD' AND s.SEQUENCE_NAME = 'SQ_DAAPCDGO';

-- 3.4 Arranca vacia. Esperado: 0.
SELECT COUNT(*) AS FILAS FROM CRD.DAAP;

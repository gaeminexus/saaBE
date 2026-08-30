-- =====================================================================================
-- DDL — COBRO DE PETRO EN DOS PASOS (asiento transitorio + reparto)
-- Implementa la regla 11 de docs/logica-negocio/crd/LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md
-- FECHA: 2026-08-28
--
-- =====================================================================================
-- ESTADO: NO EJECUTADO. Escrito por el orquestador, pendiente de correr en local.
-- =====================================================================================
--
-- QUE RESUELVE
--
-- El cobro del archivo Petro se contabiliza en DOS PASOS (decision del usuario 2026-08-28):
--
--   PASO 1  Contabilidad confirma que el dinero llego al banco
--           D Banco(s)  ->  H 2.3.01.15.01 (transitoria)      plantilla alterno 19
--
--   PASO 2  Se procesa el archivo Petro
--           D 2.3.01.15.01  ->  H 1.4.05.05 / 1.4.05.10       plantilla alterno 20
--           y a continuacion la aplicacion a cuentas reales    plantilla alterno 21
--
-- Faltan dos cosas en la base para poder hacerlo:
--
--   a) DONDE GUARDAR LAS TRANSFERENCIAS. La pizarra dice literal "Petro puede pagar con
--      mas de 1 transferencia" y el ejemplo de §3.3 muestra DOS bancos al Debe. Hoy
--      CRD.CRAR solo tiene CRARNMTF (UN numero de transferencia, sin FK a cuenta
--      bancaria). Se crea CRD.TRCR, tabla hija de CRAR, una fila por transferencia.
--
--   b) DONDE GUARDAR LOS ASIENTOS GENERADOS. CRD.CRAR no tiene ninguna columna de
--      asiento (verificado en CargaArchivo.java). Se crea CRD.ANCP, espejo de la
--      CRD.ANCC que ya funciona para el cierre de cartera: un asiento por sub-proceso.
--
-- LO QUE **NO** HACE FALTA CREAR, y es importante no duplicarlo:
--
--   CRD.CRAR YA TIENE mapeadas y sin usar CRARUSCC (usuario contabilidad que confirma),
--   CRARFCAC (fecha de autorizacion de contabilidad) y CRARNMTF (numero de
--   transferencia). Son el andamiaje del "visto de contabilidad" que se diseño al
--   principio y nunca se conecto — el rubro 166 ASPEstadoCargaArchivoPetro incluso
--   define el estado 3 APROBADO_CONTABILIDAD, que ninguna clase usa. El paso 1 REACTIVA
--   ese camino (decision del usuario): no se crean columnas nuevas para eso.
--
--   CRARNMTF queda como esta. Con TRCR pasa a ser redundante, pero NO SE BORRA en este
--   script: dropear una columna es irreversible y hay que confirmar antes que nadie la
--   lea. Ver el bloque 5 (control), y el 6 (drop) que va COMENTADO a proposito.
--
-- DIRECCION DE DEPENDENCIAS — verificada contra la restriccion permanente del proyecto
--
--   CRD.TRCR referencia TSR.BNCO, TSR.CNBC y TSR.BEXT. Es la direccion PERMITIDA:
--   crd -> tsr. Lo prohibido es tsr/cxp/cnt -> crd (el sistema se comercializa sin crd).
--   Ninguna tabla de TSR se modifica en este script.
--
--   TSR.CobroTransferencia (TSR.CTRN) se evaluo y se DESCARTO como alternativa: su
--   javadoc dice "es detalle de la entidad cobro", cuelga de TSR.CBRO, que arrastra
--   CierreCaja, CajaLogica, UsuarioPorCaja, Deposito y su PROPIO Asiento. Es el modelo
--   de un cobro en ventanilla con cierre de caja, no el de una transferencia
--   institucional mensual; usarlo obligaria a inventar una caja falsa y su asiento
--   competiria con el de este flujo.
--
-- EJECUCION MANUAL, como owner del esquema CRD. Requiere que el owner de TSR haya dado
-- GRANT REFERENCES (bloque 0.4): el rol DBA NO habilita REFERENCES por si solo.
--
-- ORDEN RESPECTO DEL WAR: este DDL va ANTES del despliegue. Las entidades JPA nuevas
-- mapean estas tablas y el arranque de WildFly falla si no existen.
--
-- IDEMPOTENCIA: los CREATE fallan si ya se ejecutaron. Es deliberado: se prefiere el
-- error a un script que "parece" haber corrido. Si hay que repetir, revisar antes con
-- los controles del bloque 0.
--
-- Contenido:
--   0. Controles PREVIOS (leer antes de ejecutar nada)
--   1. CREATE CRD.TRCR — transferencias recibidas de una carga Petro
--   2. CREATE CRD.ANCP — asiento por sub-proceso de la carga Petro
--   3. Grants
--   4. Controles POSTERIORES
--   5. Control de CRARNMTF (informativo)
--   6. Drop de CRARNMTF — COMENTADO, no ejecutar
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS — ejecutar y leer ANTES de correr el resto
-- =====================================================================================

-- 0.1 Las tablas nuevas no deben existir todavia. Esperado: 0 filas.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME IN ('TRCR', 'ANCP');

SELECT s.SEQUENCE_OWNER, s.SEQUENCE_NAME FROM ALL_SEQUENCES s
WHERE  s.SEQUENCE_OWNER = 'CRD' AND s.SEQUENCE_NAME IN ('SQ_TRCRCDGO', 'SQ_ANCPCDGO');

-- 0.2 Las tablas de TSR a las que se apunta SI deben existir. Esperado: 3 filas.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  t.OWNER = 'TSR' AND t.TABLE_NAME IN ('BNCO', 'CNBC', 'BEXT')
ORDER BY t.TABLE_NAME;

-- 0.3 Confirmar el nombre real de la PK de cada tabla referenciada, ANTES de escribir la
--     FK. No asumir: si alguna difiere, corregir la FK correspondiente mas abajo.
SELECT c.TABLE_NAME, c.CONSTRAINT_NAME, cc.COLUMN_NAME
FROM   ALL_CONSTRAINTS c
JOIN   ALL_CONS_COLUMNS cc ON cc.OWNER = c.OWNER AND cc.CONSTRAINT_NAME = c.CONSTRAINT_NAME
WHERE  c.CONSTRAINT_TYPE = 'P'
AND  ( (c.OWNER = 'TSR' AND c.TABLE_NAME IN ('BNCO','CNBC','BEXT'))
    OR (c.OWNER = 'CRD' AND c.TABLE_NAME = 'CRAR')
    OR (c.OWNER = 'CNT' AND c.TABLE_NAME = 'ASNT') )   -- ASNT es de CNT, no de CRD
ORDER BY c.OWNER, c.TABLE_NAME;

-- 0.4 ⚠ GRANT REFERENCES CROSS-SCHEMA — lo corre el OWNER DE TSR, no CRD.
--     El rol DBA NO habilita REFERENCES: sin estos grants las FK del bloque 1 fallan con
--     ORA-01031. Ejecutar CONECTADO COMO TSR y despues volver a CRD.
--
--     GRANT REFERENCES ON TSR.BNCO TO CRD;
--     GRANT REFERENCES ON TSR.CNBC TO CRD;
--     GRANT REFERENCES ON TSR.BEXT TO CRD;
--
--     Verificacion (esperado: 3 filas con PRIVILEGE = 'REFERENCES'):
SELECT p.TABLE_NAME, p.PRIVILEGE, p.GRANTEE
FROM   ALL_TAB_PRIVS p
WHERE  p.TABLE_SCHEMA = 'TSR' AND p.TABLE_NAME IN ('BNCO','CNBC','BEXT')
AND    p.PRIVILEGE = 'REFERENCES' AND p.GRANTEE = 'CRD';

-- 0.5 Cuantas cargas hay hoy y en que estado, para dimensionar el backfill posterior.
--     CrdEstadoCargaArchivo: 1 CARGADO, 3 PROCESADO (el 3 del rubro 166 dice
--     APROBADO_CONTABILIDAD pero NINGUNA clase usa ese rubro; los valores vivos son los
--     de com.saa.rubros.CrdEstadoCargaArchivo).
SELECT c.CRARESTD, COUNT(*) AS CARGAS,
       MIN(c.CRARANAF || '-' || LPAD(c.CRARMSAF, 2, '0')) AS PERIODO_MIN,
       MAX(c.CRARANAF || '-' || LPAD(c.CRARMSAF, 2, '0')) AS PERIODO_MAX
FROM   CRD.CRAR c GROUP BY c.CRARESTD ORDER BY 1;

-- 0.6 CRARNMTF: cuantas cargas historicas traen numero de transferencia.
--     Si da 0, nadie lo lleno nunca (es lo esperado) y no hay dato que migrar a TRCR.
SELECT COUNT(*) AS CARGAS_TOTAL,
       SUM(CASE WHEN c.CRARNMTF IS NOT NULL THEN 1 ELSE 0 END) AS CON_NUM_TRANSFERENCIA,
       SUM(CASE WHEN c.CRARUSCC IS NOT NULL THEN 1 ELSE 0 END) AS CON_USUARIO_CONTAB,
       SUM(CASE WHEN c.CRARFCAC IS NOT NULL THEN 1 ELSE 0 END) AS CON_FECHA_AUTORIZ
FROM   CRD.CRAR c;


-- =====================================================================================
-- 1. CREATE TABLE: CRD.TRCR — transferencias recibidas de una carga Petro
-- =====================================================================================
-- Una fila por transferencia. N filas por carga: "Petro puede pagar con mas de 1
-- transferencia" (pizarra), y el asiento del paso 1 lleva un Debe por cada una.
--
-- POR QUE TABLA HIJA Y NO COLUMNAS EN CRAR: con columnas se soportaria UNA transferencia
-- (es lo que hace hoy CRARNMTF y por eso no alcanza). El dia que Petro pague en dos
-- depositos, el asiento tiene que llevar dos lineas de Debe con importes distintos —
-- eso es una relacion 1:N, no un campo.
--
-- LA SUMA DE TRCRVLRR DE UNA CARGA ES EL TOTAL DEL ASIENTO DEL PASO 1. El servicio debe
-- validar esa suma contra el total cobrado del archivo antes de contabilizar; la base no
-- puede garantizarlo con un CHECK (involucra otra tabla).
-- =====================================================================================

CREATE SEQUENCE CRD.SQ_TRCRCDGO START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE CRD.TRCR (
    TRCRCDGO NUMBER          NOT NULL,   -- PK
    CRARCDGO NUMBER          NOT NULL,   -- FK CRD.CRAR, la carga a la que pertenece
    CNBCCDGO NUMBER          NOT NULL,   -- FK TSR.CNBC, cuenta bancaria DESTINO (la nuestra)
    BNCOCDGO NUMBER,                     -- FK TSR.BNCO, banco de la cuenta destino
    BEXTCDGO NUMBER,                     -- FK TSR.BEXT, banco externo ORIGEN (de Petro)
    TRCRCTOR VARCHAR2(50),               -- cuenta origen de la transferencia (texto, igual que TSR.CTRN)
    TRCRNMRO VARCHAR2(50),               -- numero/referencia de la transferencia
    TRCRVLRR NUMBER(18,2)    NOT NULL,   -- valor recibido en ESTA transferencia
    TRCRFCHA DATE            NOT NULL,   -- fecha en que el dinero entro al banco
    TRCROBSR VARCHAR2(2000),
    TRCRIDST NUMBER          DEFAULT 1 NOT NULL,  -- 1 activo, 0 anulado
    TRCRUSRG VARCHAR2(50),
    TRCRFCRG TIMESTAMP,
    TRCRIPRG VARCHAR2(50)
);

ALTER TABLE CRD.TRCR ADD CONSTRAINT PK_TRCR PRIMARY KEY (TRCRCDGO);

ALTER TABLE CRD.TRCR ADD CONSTRAINT FK_TRCR_CRAR
    FOREIGN KEY (CRARCDGO) REFERENCES CRD.CRAR(CRARCDGO);
ALTER TABLE CRD.TRCR ADD CONSTRAINT FK_TRCR_CNBC
    FOREIGN KEY (CNBCCDGO) REFERENCES TSR.CNBC(CNBCCDGO);
ALTER TABLE CRD.TRCR ADD CONSTRAINT FK_TRCR_BNCO
    FOREIGN KEY (BNCOCDGO) REFERENCES TSR.BNCO(BNCOCDGO);
ALTER TABLE CRD.TRCR ADD CONSTRAINT FK_TRCR_BEXT
    FOREIGN KEY (BEXTCDGO) REFERENCES TSR.BEXT(BEXTCDGO);

ALTER TABLE CRD.TRCR ADD CONSTRAINT CK_TRCR_VALOR CHECK (TRCRVLRR > 0);
ALTER TABLE CRD.TRCR ADD CONSTRAINT CK_TRCR_IDST  CHECK (TRCRIDST IN (0, 1));

-- El acceso natural es "todas las transferencias de esta carga".
CREATE INDEX CRD.IDX_TRCR_CARGA ON CRD.TRCR (CRARCDGO, TRCRIDST);

COMMENT ON TABLE  CRD.TRCR IS
    'Transferencias con las que Petro/ARCH pago una carga. N por carga. Alimenta las lineas de Debe del asiento transitorio (paso 1).';
COMMENT ON COLUMN CRD.TRCR.TRCRVLRR IS
    'Valor de ESTA transferencia. La suma por carga debe cuadrar con el total cobrado del archivo; lo valida el servicio, no la base.';
COMMENT ON COLUMN CRD.TRCR.TRCRFCHA IS
    'Fecha en que el dinero entro al banco. Es la fecha del asiento del paso 1, no la de la carga del archivo.';
COMMENT ON COLUMN CRD.TRCR.CNBCCDGO IS
    'Cuenta bancaria DESTINO (la de la empresa). FK a TSR.CNBC: crd -> tsr es la direccion permitida.';


-- =====================================================================================
-- 2. CREATE TABLE: CRD.ANCP — asiento por sub-proceso de la carga Petro
-- =====================================================================================
-- ESPEJO DELIBERADO de CRD.ANCC (asiento por sub-proceso del cierre de cartera), que ya
-- esta en produccion y funciona. Misma forma, mismo criterio: un asiento por cada
-- sub-proceso contable, con su valor y su conteo de lineas, para poder consultarlo y
-- reversarlo sin adivinar cual asiento salio de donde.
--
-- CRD.CRAR NO TIENE NINGUNA COLUMNA DE ASIENTO (verificado en CargaArchivo.java): sin
-- esta tabla no habria forma de saber que asiento genero cada paso.
--
-- ANCPTPOO — sub-proceso, con las constantes que definira el agente de backend:
--     1 = TRANSITORIO  (paso 1: D Banco(s) -> H 2.3.01.15.01, plantilla 19)
--     2 = REPARTO      (paso 2a: D 2.3.01.15.01 -> H 1.4.05.05/1.4.05.10, plantilla 20)
--     3 = APLICACION   (paso 2b: D 2.3.02.05/2.3.02.10 -> H cuentas reales, plantilla 21)
-- Los numeros salen de una interfaz de com.saa.rubros, NO de literales en el codigo.
-- =====================================================================================

CREATE SEQUENCE CRD.SQ_ANCPCDGO START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE CRD.ANCP (
    ANCPCDGO NUMBER          NOT NULL,   -- PK
    CRARCDGO NUMBER          NOT NULL,   -- FK CRD.CRAR
    ANCPTPOO NUMBER          NOT NULL,   -- sub-proceso: 1 transitorio, 2 reparto, 3 aplicacion
    ANCPASNT NUMBER,                     -- FK CNT.ASNT, asiento generado
    ANCPNMAS VARCHAR2(50),               -- numero del asiento, denormalizado para consulta
    ANCPFCHA DATE,                       -- fecha contable del asiento
    ANCPVLRR NUMBER(18,2),               -- total del asiento (Debe = Haber)
    ANCPCNTD NUMBER,                     -- cantidad de lineas del asiento
    ANCPOBSR VARCHAR2(2000),
    ANCPIDST NUMBER          DEFAULT 1 NOT NULL,  -- 1 vigente, 0 reversado
    ANCPUSRG VARCHAR2(50),
    ANCPFCRG TIMESTAMP,
    ANCPIPRG VARCHAR2(50)
);

ALTER TABLE CRD.ANCP ADD CONSTRAINT PK_ANCP PRIMARY KEY (ANCPCDGO);

ALTER TABLE CRD.ANCP ADD CONSTRAINT FK_ANCP_CRAR
    FOREIGN KEY (CRARCDGO) REFERENCES CRD.CRAR(CRARCDGO);

-- ⚠ Confirmar primero con el control 0.3 que CNT.ASNT tiene PK ASNTCDGO. La FK a ASNT es
--   la que ANCC recibio EN REVISION (no estaba en su primera version) — no omitirla aca.
ALTER TABLE CRD.ANCP ADD CONSTRAINT FK_ANCP_ASNT
    FOREIGN KEY (ANCPASNT) REFERENCES CNT.ASNT(ASNTCDGO);

ALTER TABLE CRD.ANCP ADD CONSTRAINT CK_ANCP_TPOO CHECK (ANCPTPOO IN (1, 2, 3));
ALTER TABLE CRD.ANCP ADD CONSTRAINT CK_ANCP_IDST CHECK (ANCPIDST IN (0, 1));

-- Como maximo UN asiento vigente por carga y sub-proceso. El CASE deja fuera del indice
-- las filas ya reversadas, igual que UK_VGCN_ABIERTA.
CREATE UNIQUE INDEX CRD.UK_ANCP_VIGENTE ON CRD.ANCP (
    CRARCDGO, ANCPTPOO, CASE WHEN ANCPIDST = 1 THEN 1 END);

CREATE INDEX CRD.IDX_ANCP_CARGA ON CRD.ANCP (CRARCDGO, ANCPTPOO, ANCPIDST);

COMMENT ON TABLE  CRD.ANCP IS
    'Asiento contable por sub-proceso de una carga Petro. Espejo de CRD.ANCC (cierre de cartera). 1 transitorio, 2 reparto, 3 aplicacion.';
COMMENT ON COLUMN CRD.ANCP.ANCPTPOO IS
    'Sub-proceso: 1 TRANSITORIO (plantilla 19), 2 REPARTO (plantilla 20), 3 APLICACION (plantilla 21). Constantes en com.saa.rubros.';
COMMENT ON COLUMN CRD.ANCP.ANCPIDST IS
    '1 vigente, 0 reversado. Un asiento reversado sale del indice unico y permite volver a contabilizar ese sub-proceso.';


-- =====================================================================================
-- 3. GRANTS
-- =====================================================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON CRD.TRCR TO ROLE_CRD;
GRANT SELECT, INSERT, UPDATE, DELETE ON CRD.ANCP TO ROLE_CRD;
GRANT SELECT ON CRD.SQ_TRCRCDGO TO ROLE_CRD;
GRANT SELECT ON CRD.SQ_ANCPCDGO TO ROLE_CRD;


-- =====================================================================================
-- 4. CONTROLES POSTERIORES
-- =====================================================================================

-- 4.1 Las dos tablas existen. Esperado: 2 filas.
SELECT t.OWNER, t.TABLE_NAME, t.NUM_ROWS FROM ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME IN ('TRCR', 'ANCP') ORDER BY t.TABLE_NAME;

-- 4.2 Constraints. Esperado: TRCR con 1 PK + 4 R + 2 C(propios); ANCP con 1 PK + 2 R + 2 C.
--     (Oracle agrega ademas un CHECK por cada columna NOT NULL: no alarmarse por el conteo.)
SELECT c.TABLE_NAME, c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.SEARCH_CONDITION
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME IN ('TRCR', 'ANCP')
ORDER  BY c.TABLE_NAME, c.CONSTRAINT_TYPE, c.CONSTRAINT_NAME;

-- 4.3 Indices, y que quedaron en el schema CRD (no en el del usuario de la sesion).
SELECT i.OWNER, i.TABLE_NAME, i.INDEX_NAME, i.UNIQUENESS
FROM   ALL_INDEXES i
WHERE  i.TABLE_OWNER = 'CRD' AND i.TABLE_NAME IN ('TRCR', 'ANCP')
ORDER  BY i.TABLE_NAME, i.INDEX_NAME;

-- 4.4 Las secuencias existen. Esperado: 2 filas.
SELECT s.SEQUENCE_OWNER, s.SEQUENCE_NAME, s.LAST_NUMBER FROM ALL_SEQUENCES s
WHERE  s.SEQUENCE_OWNER = 'CRD' AND s.SEQUENCE_NAME IN ('SQ_TRCRCDGO', 'SQ_ANCPCDGO');


-- =====================================================================================
-- 5. CRARNMTF — control informativo, NO se toca en este script
-- =====================================================================================
-- Con CRD.TRCR, la columna CRARNMTF (un solo numero de transferencia por carga) queda
-- redundante. NO SE BORRA todavia: dropear una columna es irreversible y primero hay que
-- confirmar que ningun codigo la lee. Al 2026-08-28, verificado con grep sobre todo src:
-- solo existen su getter y su setter en CargaArchivo.java, ninguna linea la escribe ni
-- la consulta. Repetir esta comprobacion antes de considerar el bloque 6.

SELECT COUNT(*) AS CARGAS_CON_CRARNMTF
FROM   CRD.CRAR c WHERE c.CRARNMTF IS NOT NULL;


-- =====================================================================================
-- 6. ⛔ NO EJECUTAR — drop de CRARNMTF, solo cuando se confirme que nadie la lee
-- =====================================================================================
-- Queda escrito para que la decision sea explicita el dia que se tome, no para correrlo
-- ahora. Antes de descomentar: (a) el control 5 debe dar 0, (b) repetir el grep sobre
-- src, (c) confirmar que la entidad CargaArchivo.java ya no mapea el campo.
--
-- ALTER TABLE CRD.CRAR DROP COLUMN CRARNMTF;

-- =====================================================================================
-- PRECANCELACION MIXTA — desglose de aportes consumidos
-- FECHA: 2026-08-30
--
-- REQUERIMIENTO (usuario, 2026-08-30): "no hay precancelacion con efectivo, siempre es
-- aportes y deposito o transferencia. Migra tambien la precancelacion mixta."
--
-- O sea: la precancelacion 100% efectivo que ya se migro casi no ocurre en la practica. La
-- real mezcla saldos de aportes del socio con un deposito, y hasta hoy entraba plata al
-- banco SIN que contabilidad la viera.
--
-- MODELO: la linea de PRECANCELACION en CRD.DCBC lleva, como en todos los demas tipos, la
-- parte de DEPOSITO en DCBCVLRR — que es lo unico que contabilidad puede verificar. La
-- parte que se cubre consumiendo aportes vive en esta tabla.
--
-- ⚠️ CONSUMIR NO ES REGISTRAR, Y LA DIFERENCIA ES EL SIGNO DEL DINERO.
-- En COBRO_MIXTO, una linea con TPAPCDGO significa que el socio ENTREGA plata que se le
-- acredita como aporte: movimiento POSITIVO, su saldo SUBE.
-- Aca es al reves: se CONSUME saldo que el socio ya tenia para pagar la deuda: movimiento
-- NEGATIVO, su saldo BAJA.
-- Por eso el desglose va en tabla propia y NO se reusa la forma de linea de COBRO_MIXTO:
-- un mismo campo que significa "entra" en un tipo de operacion y "sale" en otro es como se
-- corrompen saldos sin que nada de un error.
--
-- ⚠️ CBCR/PRECANCELACION EXIGE DEPOSITO > 0, SIEMPRE.
-- Si el socio cubre TODO con aportes, no hay plata externa que verificar y la operacion va
-- por el endpoint directo que ya existe desde antes de este frente — el mismo lugar donde
-- ya viven `pagarConAportes` y el cruce de valores, y por la misma razon. No es una
-- regresion ni una puerta lateral: es la regla de siempre, aplicada.
--
-- POR QUE NO HACE FALTA NINGUN CAMPO NUEVO EN CBCR/DCBC (a diferencia de ACCN, que si
-- necesito ACCNVLAP/ACCNVLDP): el "total" de una precancelacion NUNCA se guarda — se
-- recalcula fresco con simularPrecancelacion en cada paso, al registrar y al procesar.
-- ACCNVLPG en cambio es un total que el operador decide y que existe por si mismo.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS
-- =====================================================================================

-- 0.1 CRD.DCBC debe existir. Esperado: 1 fila.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME = 'DCBC';

-- 0.2 La tabla nueva NO debe existir, en NINGUN esquema. Esperado: 0 filas.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t WHERE t.TABLE_NAME = 'DAPR';


-- =====================================================================================
-- 1. CRD.DAPR — aportes consumidos por una linea de precancelacion
-- =====================================================================================

CREATE SEQUENCE CRD.SQ_DAPRCDGO START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE CRD.DAPR (
    DAPRCDGO NUMBER          NOT NULL,   -- PK
    DCBCCDGO NUMBER          NOT NULL,   -- FK CRD.DCBC, la linea del cobro
    TPAPCDGO NUMBER          NOT NULL,   -- FK CRD.TPAP, el tipo de aporte que se consume
    DAPRVLOR NUMBER(18,2)    NOT NULL    -- cuanto se consume de ese tipo
);

ALTER TABLE CRD.DAPR ADD CONSTRAINT PK_DAPR PRIMARY KEY (DAPRCDGO);
ALTER TABLE CRD.DAPR ADD CONSTRAINT FK_DAPR_DCBC FOREIGN KEY (DCBCCDGO) REFERENCES CRD.DCBC(DCBCCDGO);
ALTER TABLE CRD.DAPR ADD CONSTRAINT FK_DAPR_TPAP FOREIGN KEY (TPAPCDGO) REFERENCES CRD.TPAP(TPAPCDGO);

ALTER TABLE CRD.DAPR ADD CONSTRAINT CK_DAPR_VLOR CHECK (DAPRVLOR > 0);
-- Un tipo de aporte no puede aparecer dos veces en la misma linea: si se consume de
-- cesantia, se consume UNA vez por el total. Misma regla que validarDesgloseAportes ya
-- aplica en pagarConAportes/precancelar, y misma que UK_DAAP_ACCN_TPAP en el acuerdo.
ALTER TABLE CRD.DAPR ADD CONSTRAINT UK_DAPR_DCBC_TPAP UNIQUE (DCBCCDGO, TPAPCDGO);

-- Sin indice propio por DCBCCDGO: el UNIQUE ya crea uno con DCBCCDGO como columna lider.

COMMENT ON TABLE  CRD.DAPR IS
    'Aportes del socio que se CONSUMEN (movimiento negativo, su saldo baja) para cubrir parte de una precancelacion registrada en CRD.CBCR. NO confundir con las lineas de aporte de COBRO_MIXTO, donde el socio ENTREGA plata y su saldo SUBE: son direcciones opuestas del dinero.';
COMMENT ON COLUMN CRD.DAPR.DCBCCDGO IS
    'Linea del cobro. Una PRECANCELACION siempre trae exactamente una linea; DCBCVLRR es la parte de DEPOSITO y esta tabla la parte de aportes. El total no se guarda: se recalcula con simularPrecancelacion.';
COMMENT ON COLUMN CRD.DAPR.DAPRVLOR IS
    'Cuanto se consume de ese tipo de aporte. El saldo se revalida DENTRO de la transaccion al PROCESAR, no al registrar: entre los dos momentos pasa la aprobacion de contabilidad y el socio pudo haber gastado ese saldo.';


-- =====================================================================================
-- 2. CONTROLES POSTERIORES
-- =====================================================================================

-- 2.1 La tabla y sus constraints. Esperado: 1 PK + 2 FK + 1 UNIQUE + 1 CHECK, ENABLED.
SELECT c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.STATUS, c.SEARCH_CONDITION
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'DAPR'
ORDER  BY c.CONSTRAINT_TYPE, c.CONSTRAINT_NAME;

-- 2.2 La secuencia existe.
SELECT s.SEQUENCE_NAME, s.LAST_NUMBER FROM ALL_SEQUENCES s
WHERE  s.SEQUENCE_OWNER = 'CRD' AND s.SEQUENCE_NAME = 'SQ_DAPRCDGO';

-- 2.3 Arranca vacia. Esperado: 0.
SELECT COUNT(*) AS FILAS FROM CRD.DAPR;

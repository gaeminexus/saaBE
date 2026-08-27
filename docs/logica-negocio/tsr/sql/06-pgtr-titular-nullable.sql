-- =====================================================================
-- PAGOS: el titular deja de ser obligatorio en PGS.PGTR
-- Módulo: CXP / TSR
-- Fecha:  2026-08-27
-- Autor:  orquestador
--
-- MOTIVO (detectado al probar la apertura de una caja chica en el navegador):
--
--   PGS.PGTR.PGTRTTLR es NOT NULL, pero `registrarPagoDeOrigenExterno`
--   nunca setea el titular: solo lo hacen las ramas de factura, egreso y
--   anticipo. Cualquier pago de origen externo revienta con
--   ORA-01400 antes de grabarse.
--
--   Y es correcto que no lo setee: un pago de origen externo no siempre
--   tiene un tercero detrás. Una caja chica NO es un titular — el dinero
--   sale del banco hacia el fondo de la propia empresa. El nombre del
--   beneficiario ya se guarda en PGTRBFNM.
--
--   Verificado en la base: no existe ningún pago con PGTRORGN informado,
--   asi que este camino nunca se ejercito. La devolucion de aportes de
--   CRD (el otro origen externo) fallaria igual en su primer uso real.
--
--   Verificado: 0 filas con PGTRTTLR nulo, asi que relajar la restriccion
--   no toca ningun dato existente y no rompe los pagos con titular.
--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 1: control previo
--   (a) PGTRTTLR debe figurar como NOT NULL ('N')
--   (b) no debe haber ninguna fila con titular nulo (deberia dar 0)
-- ---------------------------------------------------------------------
SELECT COLUMN_NAME, NULLABLE
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'PGS' AND TABLE_NAME = 'PGTR' AND COLUMN_NAME = 'PGTRTTLR';

SELECT COUNT(*) AS PAGOS_SIN_TITULAR FROM PGS.PGTR WHERE PGTRTTLR IS NULL;

-- ---------------------------------------------------------------------
-- BLOQUE 2: relajar la restriccion
--   La FK a TSR.TTLR se mantiene: si viene titular, tiene que existir.
-- ---------------------------------------------------------------------
ALTER TABLE PGS.PGTR MODIFY (PGTRTTLR NULL);

COMMENT ON COLUMN PGS.PGTR.PGTRTTLR IS 'Titular beneficiario del pago. Opcional: los pagos de origen externo (caja chica) pueden no tener un tercero detras; en ese caso el beneficiario va en PGTRBFNM';

-- ---------------------------------------------------------------------
-- BLOQUE 3: control final — NULLABLE debe pasar a 'Y', y la FK seguir viva
-- ---------------------------------------------------------------------
SELECT COLUMN_NAME, NULLABLE
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'PGS' AND TABLE_NAME = 'PGTR' AND COLUMN_NAME = 'PGTRTTLR';

SELECT CONSTRAINT_NAME, STATUS, VALIDATED
  FROM ALL_CONSTRAINTS
 WHERE OWNER = 'PGS' AND TABLE_NAME = 'PGTR' AND CONSTRAINT_TYPE = 'R'
   AND CONSTRAINT_NAME LIKE '%TTLR%';

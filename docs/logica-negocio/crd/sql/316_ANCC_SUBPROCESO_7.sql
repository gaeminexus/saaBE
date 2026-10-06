-- =====================================================================================
-- 316 — CRD.ANCC: el CHECK del sub-proceso admite el 7 (Provision de intereses)
-- FECHA: 2026-10-05 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- INCIDENTE (produccion, 2026-10-05): «Ejecutar y contabilizar» del cierre de septiembre fallo con
-- ORA-02290 CK_ANCC_TPOO. El CHECK (DDL-CIERRE-CARTERA.sql:190) solo admite los sub-procesos 1-6, y el
-- 7 (Provision de intereses, sql/309/310, WAR 629de9c2) no se agrego. Faltaba en el 309: es error
-- del arbitro, no del codigo.
-- La ejecucion corre en UNA transaccion, asi que no deberia haber quedado nada grabado: el bloque 0
-- lo comprueba ANTES de tocar el CHECK.
-- SQL PURO. Correr por bloques.
-- =====================================================================================

-- 0. Que no haya quedado una corrida a medias de septiembre. Esperado: 0 filas.
--    Si sale una fila, PARAR y pasarsela al arbitro.
SELECT c.CRCTCDGO, c.CRCTANOO, c.CRCTMESS, c.CRCTIDST, c.CRCTFCRG
  FROM CRD.CRCT c
 WHERE c.CRCTANOO = 2026 AND c.CRCTMESS = 9;

-- 0.1 La definicion actual del CHECK. Esperado: ANCCTPOO IN (1, 2, 3, 4, 5, 6)
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION_VC
  FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.CONSTRAINT_NAME = 'CK_ANCC_TPOO';

-- 1. El cambio.
ALTER TABLE CRD.ANCC DROP CONSTRAINT CK_ANCC_TPOO;
ALTER TABLE CRD.ANCC ADD CONSTRAINT CK_ANCC_TPOO CHECK (ANCCTPOO IN (1, 2, 3, 4, 5, 6, 7));

-- 2. Control. Esperado: ANCCTPOO IN (1, 2, 3, 4, 5, 6, 7), ENABLED, VALIDATED.
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION_VC, c.STATUS, c.VALIDATED
  FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.CONSTRAINT_NAME = 'CK_ANCC_TPOO';

-- 3. REVERSO — COMENTADO (solo si no hay ninguna fila ANCC con sub-proceso 7)
-- ALTER TABLE CRD.ANCC DROP CONSTRAINT CK_ANCC_TPOO;
-- ALTER TABLE CRD.ANCC ADD CONSTRAINT CK_ANCC_TPOO CHECK (ANCCTPOO IN (1, 2, 3, 4, 5, 6));

-- =====================================================================================
-- C34-07c — Hecho de crédito: la diferencia que falta se cruza con los aportes del partícipe (pedido del líder, 18-nov-2026).
-- Cuadre Superintendencia · Diseño: expediente cuadreSuper, 03-trabajo/cambios-saa/c34-novedades-v2/C34D-HECHOS-DE-CREDITO-DISENO.md §9.
--
-- QUE HACE (incremental sobre C34-07b):
--   CNT.HCNV  + HCNVTPAP: tipo de aporte (CRD.TPAP) con que se cruza la diferencia que falta;
--             CK_HCNV_TRDF admite CRUCE_APORTES (la v8 debita esa diferencia de la cuenta individual del partícipe por el
--             motor de crédito, en la fecha del hecho); CK_HCNV_TPAP exige el tipo de aporte en ese caso.
-- ⛔ VA DESPUÉS DE C34-07b Y ANTES DEL WAR que lo usa. SQL PURO, solo DDL. Si el CONTROL 1.1 devuelve filas, NO correr.
--    No cambia ninguna fila existente ni ninguna cifra contable.
-- =====================================================================================

-- 1.1 Nada de C34-07c existe. ESPERADO: 0 filas.
SELECT COLUMN_NAME FROM ALL_TAB_COLUMNS WHERE OWNER = 'CNT' AND TABLE_NAME = 'HCNV' AND COLUMN_NAME = 'HCNVTPAP';
-- 1.2 C34-07b aplicado. ESPERADO: 1 fila (HCNVTRDF).
SELECT COLUMN_NAME FROM ALL_TAB_COLUMNS WHERE OWNER = 'CNT' AND TABLE_NAME = 'HCNV' AND COLUMN_NAME = 'HCNVTRDF';

-- 2. Cambio
ALTER TABLE CNT.HCNV ADD (HCNVTPAP NUMBER);
COMMENT ON COLUMN CNT.HCNV.HCNVTPAP IS 'C34-07c: tipo de aporte (CRD.TPAP, sin FK) con que se cruza la diferencia que falta (HCNVTRDF = CRUCE_APORTES)';
ALTER TABLE CNT.HCNV DROP CONSTRAINT CK_HCNV_TRDF;
ALTER TABLE CNT.HCNV ADD CONSTRAINT CK_HCNV_TRDF CHECK (HCNVTRDF IS NULL OR HCNVTRDF IN ('NO_SE_DEBITA', 'SE_DEVUELVE', 'PARTICIPE_DEBE', 'CRUCE_APORTES'));
ALTER TABLE CNT.HCNV ADD CONSTRAINT CK_HCNV_TPAP CHECK (NVL(HCNVTRDF, '-') <> 'CRUCE_APORTES' OR HCNVTPAP IS NOT NULL);
COMMENT ON COLUMN CNT.HCNV.HCNVTRDF IS 'C34d: qué pasa con la diferencia orígenes - valor: NO_SE_DEBITA, SE_DEVUELVE (sobra), PARTICIPE_DEBE o CRUCE_APORTES (falta)';

-- 3. Después. ESPERADO: HCNV 21 columnas; CK_HCNV_TRDF y CK_HCNV_TPAP ENABLED / VALIDATED.
SELECT COUNT(*) COLUMNAS_HCNV FROM ALL_TAB_COLUMNS WHERE OWNER = 'CNT' AND TABLE_NAME = 'HCNV';
SELECT CONSTRAINT_NAME, STATUS, VALIDATED FROM ALL_CONSTRAINTS WHERE OWNER = 'CNT' AND CONSTRAINT_NAME IN ('CK_HCNV_TRDF', 'CK_HCNV_TPAP') ORDER BY 1;

-- REVERSO (comentado; solo con el WAR anterior y sin hechos con CRUCE_APORTES):
-- ALTER TABLE CNT.HCNV DROP CONSTRAINT CK_HCNV_TPAP;
-- ALTER TABLE CNT.HCNV DROP CONSTRAINT CK_HCNV_TRDF;
-- ALTER TABLE CNT.HCNV ADD CONSTRAINT CK_HCNV_TRDF CHECK (HCNVTRDF IS NULL OR HCNVTRDF IN ('NO_SE_DEBITA', 'SE_DEVUELVE', 'PARTICIPE_DEBE'));
-- ALTER TABLE CNT.HCNV DROP (HCNVTPAP);

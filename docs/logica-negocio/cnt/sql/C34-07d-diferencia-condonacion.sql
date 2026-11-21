-- =====================================================================================
-- C34-07d — Hecho de crédito: lo que falta se condona (diseño C34D §10, aprobado por el líder el 18-nov-2026).
--
-- QUE HACE (incremental sobre C34-07c):
--   CNT.HCNV  + HCNVCNDN (CLOB JSON): condonación del faltante, por cuota y concepto (adeudado, pagado, condonado),
--             conceptos marcados (mora, interés, capital), fecha del cálculo, referencia a la parametrización de la cuenta
--             (plantilla de código alterno 25, línea auxiliar 70) y la cuenta vigente al registrar (solo informativa: la v8
--             resuelve la cuenta al registrar con el motor de condonación del SAA, así sigue cualquier cambio de Contabilidad).
--             + HCNVDCSN: decisión de ASOPREP que respalda la condonación (regla 4: constancia escrita).
--             CK_HCNV_TRDF admite CONDONACION; CK_HCNV_CNDN exige la decisión y el detalle cuando se condona.
-- ⛔ VA DESPUÉS DE C34-07c Y ANTES DEL WAR que lo usa. SQL PURO, solo DDL. Si el CONTROL 1.1 devuelve filas, NO correr.
--    No cambia ninguna fila existente ni ninguna cifra contable.
-- =====================================================================================

-- 1.1 Nada de C34-07d existe. ESPERADO: 0 filas.
SELECT COLUMN_NAME FROM ALL_TAB_COLUMNS WHERE OWNER = 'CNT' AND TABLE_NAME = 'HCNV' AND COLUMN_NAME IN ('HCNVCNDN', 'HCNVDCSN');
-- 1.2 C34-07c aplicado. ESPERADO: 1 fila (HCNVTPAP).
SELECT COLUMN_NAME FROM ALL_TAB_COLUMNS WHERE OWNER = 'CNT' AND TABLE_NAME = 'HCNV' AND COLUMN_NAME = 'HCNVTPAP';

-- 2. Cambio
ALTER TABLE CNT.HCNV ADD (HCNVCNDN CLOB, HCNVDCSN NUMBER);
ALTER TABLE CNT.HCNV ADD CONSTRAINT FK_HCNV_DCSN FOREIGN KEY (HCNVDCSN) REFERENCES CNT.DCSN(DCSNCDGO);
CREATE INDEX IDX_HCNV_DCSN ON CNT.HCNV(HCNVDCSN);
COMMENT ON COLUMN CNT.HCNV.HCNVCNDN IS 'C34-07d: condonación del faltante (JSON): cuotas y conceptos adeudado/pagado/condonado, conceptos marcados, referencia plantilla 25 / línea 70 y cuenta al registrar (informativa)';
COMMENT ON COLUMN CNT.HCNV.HCNVDCSN IS 'C34-07d: decisión de ASOPREP que respalda la condonación (CNT.DCSN)';
ALTER TABLE CNT.HCNV DROP CONSTRAINT CK_HCNV_TRDF;
ALTER TABLE CNT.HCNV ADD CONSTRAINT CK_HCNV_TRDF CHECK (HCNVTRDF IS NULL OR HCNVTRDF IN ('NO_SE_DEBITA', 'SE_DEVUELVE', 'PARTICIPE_DEBE', 'CRUCE_APORTES', 'CONDONACION'));
ALTER TABLE CNT.HCNV ADD CONSTRAINT CK_HCNV_CNDN CHECK (NVL(HCNVTRDF, '-') <> 'CONDONACION' OR (HCNVDCSN IS NOT NULL AND HCNVCNDN IS NOT NULL));
COMMENT ON COLUMN CNT.HCNV.HCNVTRDF IS 'C34d: qué pasa con la diferencia orígenes - valor: NO_SE_DEBITA, SE_DEVUELVE (sobra), PARTICIPE_DEBE, CRUCE_APORTES o CONDONACION (falta)';

-- 3. Después. ESPERADO: HCNV 23 columnas; FK_HCNV_DCSN, CK_HCNV_TRDF y CK_HCNV_CNDN ENABLED / VALIDATED.
SELECT COUNT(*) COLUMNAS_HCNV FROM ALL_TAB_COLUMNS WHERE OWNER = 'CNT' AND TABLE_NAME = 'HCNV';
SELECT CONSTRAINT_NAME, STATUS, VALIDATED FROM ALL_CONSTRAINTS WHERE OWNER = 'CNT' AND CONSTRAINT_NAME IN ('FK_HCNV_DCSN', 'CK_HCNV_TRDF', 'CK_HCNV_CNDN') ORDER BY 1;

-- REVERSO (comentado; solo con el WAR anterior y sin hechos con CONDONACION):
-- ALTER TABLE CNT.HCNV DROP CONSTRAINT CK_HCNV_CNDN;
-- ALTER TABLE CNT.HCNV DROP CONSTRAINT CK_HCNV_TRDF;
-- ALTER TABLE CNT.HCNV ADD CONSTRAINT CK_HCNV_TRDF CHECK (HCNVTRDF IS NULL OR HCNVTRDF IN ('NO_SE_DEBITA', 'SE_DEVUELVE', 'PARTICIPE_DEBE', 'CRUCE_APORTES'));
-- ALTER TABLE CNT.HCNV DROP CONSTRAINT FK_HCNV_DCSN; DROP INDEX CNT.IDX_HCNV_DCSN; ALTER TABLE CNT.HCNV DROP (HCNVCNDN, HCNVDCSN);

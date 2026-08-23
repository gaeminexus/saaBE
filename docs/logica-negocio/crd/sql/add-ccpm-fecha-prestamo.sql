-- ============================================================
-- Agrega el campo CCPMFCPR (fecha del préstamo = PRST.PRSTFCHA)
-- a la tabla RPR.CCPM
-- ============================================================

ALTER TABLE RPR.CCPM
    ADD CCPMFCPR DATE NULL;

COMMENT ON COLUMN RPR.CCPM.CCPMFCPR IS 'Fecha del préstamo (PRST.PRSTFCHA)';

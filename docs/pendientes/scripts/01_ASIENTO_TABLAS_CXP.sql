-- =============================================================================
-- SCRIPT: Agregar campo ASIENTO (FK a CNT.ASNT) en tablas cabecera CXP
-- Schema:  PGS
-- Fecha:   2026-07-25
-- Tarea:   Prioridad 1.1 - Plan de Implementación ASOPREP
-- Nota:    El campo es nullable (NULL = documento aún no contabilizado).
--          Mismo patrón usado exitosamente en CXC.
-- =============================================================================

-- 1. FACTURA DE COMPRA (PGS.FCTC)
ALTER TABLE PGS.FCTC
    ADD ASIENTO NUMBER(19) NULL;

ALTER TABLE PGS.FCTC
    ADD CONSTRAINT FK_FCTC_ASNTCDGO
    FOREIGN KEY (ASIENTO)
    REFERENCES CNT.ASNT (ASNTCDGO);

COMMENT ON COLUMN PGS.FCTC.ASIENTO IS 'FK al asiento contable generado al contabilizar la factura de compra (CNT.ASNT.ASNTCDGO)';

-- 2. NOTA DE CRÉDITO DE COMPRA (PGS.NTCC)
ALTER TABLE PGS.NTCC
    ADD ASIENTO NUMBER(19) NULL;

ALTER TABLE PGS.NTCC
    ADD CONSTRAINT FK_NTCC_ASNTCDGO
    FOREIGN KEY (ASIENTO)
    REFERENCES CNT.ASNT (ASNTCDGO);

COMMENT ON COLUMN PGS.NTCC.ASIENTO IS 'FK al asiento contable generado al contabilizar la nota de crédito de compra (CNT.ASNT.ASNTCDGO)';

-- 3. LIQUIDACIÓN DE COMPRA - COMPRA (PGS.LQCC)
ALTER TABLE PGS.LQCC
    ADD ASIENTO NUMBER(19) NULL;

ALTER TABLE PGS.LQCC
    ADD CONSTRAINT FK_LQCC_ASNTCDGO
    FOREIGN KEY (ASIENTO)
    REFERENCES CNT.ASNT (ASNTCDGO);

COMMENT ON COLUMN PGS.LQCC.ASIENTO IS 'FK al asiento contable generado al contabilizar la liquidación de compra (CNT.ASNT.ASNTCDGO)';

-- 4. RETENCIÓN DE COMPRA (PGS.RTCM)
ALTER TABLE PGS.RTCM
    ADD ASIENTO NUMBER(19) NULL;

ALTER TABLE PGS.RTCM
    ADD CONSTRAINT FK_RTCM_ASNTCDGO
    FOREIGN KEY (ASIENTO)
    REFERENCES CNT.ASNT (ASNTCDGO);

COMMENT ON COLUMN PGS.RTCM.ASIENTO IS 'FK al asiento contable generado al contabilizar la retención de compra (CNT.ASNT.ASNTCDGO)';

-- 5. NOTA DE DÉBITO DE COMPRA (PGS.NTDC)
ALTER TABLE PGS.NTDC
    ADD ASIENTO NUMBER(19) NULL;

ALTER TABLE PGS.NTDC
    ADD CONSTRAINT FK_NTDC_ASNTCDGO
    FOREIGN KEY (ASIENTO)
    REFERENCES CNT.ASNT (ASNTCDGO);

COMMENT ON COLUMN PGS.NTDC.ASIENTO IS 'FK al asiento contable generado al contabilizar la nota de débito de compra (CNT.ASNT.ASNTCDGO)';

-- 6. RETENCIÓN DE COMPRA V2 (PGS.RTCV)  -- verificar nombre real de la tabla en BD
ALTER TABLE PGS.RTCV
    ADD ASIENTO NUMBER(19) NULL;

ALTER TABLE PGS.RTCV
    ADD CONSTRAINT FK_RTCV_ASNTCDGO
    FOREIGN KEY (ASIENTO)
    REFERENCES CNT.ASNT (ASNTCDGO);

COMMENT ON COLUMN PGS.RTCV.ASIENTO IS 'FK al asiento contable generado al contabilizar la retención de compra V2 (CNT.ASNT.ASNTCDGO)';

-- =============================================================================
-- VERIFICACIÓN (ejecutar después del ALTER para confirmar)
-- =============================================================================
-- SELECT COLUMN_NAME, TABLE_NAME, DATA_TYPE, NULLABLE
-- FROM ALL_TAB_COLUMNS
-- WHERE TABLE_NAME IN ('FCTC','NTCC','LQCC','RTCM','NTDC','RTCV')
--   AND OWNER = 'PGS'
--   AND COLUMN_NAME = 'ASIENTO'
-- ORDER BY TABLE_NAME;

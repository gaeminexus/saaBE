-- ============================================================
-- Migración: Ruta del documento de respaldo digitalizado en pagos
-- Módulo:    CRD - Créditos
-- Schema:    CRD
-- Fecha:     2026-08-14
--
-- Propósito: Guardar la ruta del comprobante escaneado (documento
--            de respaldo digitalizado) del pago de préstamo
--            (CRD.PGPR) y del pago de aporte (CRD.PGAP).
--
--            Campo Java / JSON: rutaDocumentoRespaldo
--            (las entidades se serializan directo a JSON, así que
--            el frontend envía y recibe ese nombre en /pgpr y /pgap).
-- ============================================================

-- 1. PagoPrestamo (CRD.PGPR)
ALTER TABLE CRD.PGPR ADD PGPRRTRS VARCHAR2(2000) NULL;

COMMENT ON COLUMN CRD.PGPR.PGPRRTRS
    IS 'Ruta del documento de respaldo digitalizado (comprobante escaneado) del pago.';

-- 2. PagoAporte (CRD.PGAP)
ALTER TABLE CRD.PGAP ADD PGAPRTRS VARCHAR2(2000) NULL;

COMMENT ON COLUMN CRD.PGAP.PGAPRTRS
    IS 'Ruta del documento de respaldo digitalizado (comprobante escaneado) del pago de aporte.';

COMMIT;


-- ============================================================
-- Verificación
-- ============================================================

-- SELECT OWNER, TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE
--   FROM ALL_TAB_COLUMNS
--  WHERE OWNER = 'CRD'
--    AND ((TABLE_NAME = 'PGPR' AND COLUMN_NAME = 'PGPRRTRS')
--      OR (TABLE_NAME = 'PGAP' AND COLUMN_NAME = 'PGAPRTRS'));

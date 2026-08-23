-- ============================================================
-- Agrega campos de auditoría de anulación a CBR.FCTR y CNT.ASNT
-- Fecha: 2026-07-21
-- ============================================================

-- Tabla de Factura
ALTER TABLE CBR.FCTR ADD MOTIVOANULACION  VARCHAR2(1000);
ALTER TABLE CBR.FCTR ADD FECHAANULACION   TIMESTAMP;
ALTER TABLE CBR.FCTR ADD USUARIOANULACION VARCHAR2(200);

COMMENT ON COLUMN CBR.FCTR.MOTIVOANULACION  IS 'Motivo por el cual se anuló la factura';
COMMENT ON COLUMN CBR.FCTR.FECHAANULACION   IS 'Fecha y hora en que se realizó la anulación';
COMMENT ON COLUMN CBR.FCTR.USUARIOANULACION IS 'Usuario que realizó la anulación';

-- Tabla de Asiento Contable
ALTER TABLE CNT.ASNT ADD ASNTMTAN VARCHAR2(1000);
ALTER TABLE CNT.ASNT ADD ASNTFCAN TIMESTAMP;
ALTER TABLE CNT.ASNT ADD ASNTUSAN VARCHAR2(200);

COMMENT ON COLUMN CNT.ASNT.ASNTMTAN IS 'Motivo por el cual se anuló el asiento';
COMMENT ON COLUMN CNT.ASNT.ASNTFCAN IS 'Fecha y hora en que se anuló el asiento';
COMMENT ON COLUMN CNT.ASNT.ASNTUSAN IS 'Usuario que realizó la anulación del asiento';

COMMIT;

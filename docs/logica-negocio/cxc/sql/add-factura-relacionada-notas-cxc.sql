-- ============================================================
-- SCRIPT: Agregar campo FACTURA a Notas de Crédito y Débito CXC
-- Descripción: Toda nota de crédito/débito de CXC debe estar 
--              relacionada a una factura del mismo cliente.
-- Tablas afectadas:
--   CBR.NTCR  -> Notas de Crédito CXC
--   CBR.NTDB  -> Notas de Débito CXC
-- Fecha: 2026-07-22
-- Base de datos: Oracle
-- ============================================================

-- -------------------------------------------------------
-- 1. NOTA DE CRÉDITO (CBR.NTCR)
-- -------------------------------------------------------

-- Agregar columna FACTURA (FK hacia CBR.FCTR)
ALTER TABLE CBR.NTCR 
    ADD (FACTURA NUMBER NULL);

-- Comentario de la columna
COMMENT ON COLUMN CBR.NTCR.FACTURA IS 'ID de la factura CXC relacionada con esta nota de crédito';

-- Agregar llave foránea
ALTER TABLE CBR.NTCR
    ADD CONSTRAINT FK_NTCR_FACTURA
    FOREIGN KEY (FACTURA) 
    REFERENCES CBR.FCTR (ID);

-- Crear índice para mejorar performance en consultas por factura
CREATE INDEX IDX_NTCR_FACTURA ON CBR.NTCR (FACTURA);


-- -------------------------------------------------------
-- 2. NOTA DE DÉBITO (CBR.NTDB)
-- -------------------------------------------------------

-- Agregar columna FACTURA (FK hacia CBR.FCTR)
ALTER TABLE CBR.NTDB 
    ADD (FACTURA NUMBER NULL);

-- Comentario de la columna
COMMENT ON COLUMN CBR.NTDB.FACTURA IS 'ID de la factura CXC relacionada con esta nota de débito';

-- Agregar llave foránea
ALTER TABLE CBR.NTDB
    ADD CONSTRAINT FK_NTDB_FACTURA
    FOREIGN KEY (FACTURA) 
    REFERENCES CBR.FCTR (ID);

-- Crear índice para mejorar performance en consultas por factura
CREATE INDEX IDX_NTDB_FACTURA ON CBR.NTDB (FACTURA);


-- -------------------------------------------------------
-- VERIFICACIÓN
-- -------------------------------------------------------
-- Verificar columna en NTCR
-- SELECT COLUMN_NAME, DATA_TYPE, NULLABLE FROM ALL_TAB_COLUMNS WHERE TABLE_NAME = 'NTCR' AND OWNER = 'CBR' AND COLUMN_NAME = 'FACTURA';

-- Verificar columna en NTDB
-- SELECT COLUMN_NAME, DATA_TYPE, NULLABLE FROM ALL_TAB_COLUMNS WHERE TABLE_NAME = 'NTDB' AND OWNER = 'CBR' AND COLUMN_NAME = 'FACTURA';

-- -------------------------------------------------------
-- NOTAS IMPORTANTES
-- -------------------------------------------------------
-- * El campo FACTURA es NULL para permitir migración de datos existentes.
-- * Una vez migrados los datos históricos, considerar poner NOT NULL:
--   ALTER TABLE CBR.NTCR MODIFY (FACTURA NUMBER NOT NULL);
--   ALTER TABLE CBR.NTDB MODIFY (FACTURA NUMBER NOT NULL);
-- * La FK referencia a la tabla CBR.FCTR (Facturas CXC).
-- * La relación es: una Factura puede tener muchas Notas de Crédito/Débito.
-- ============================================================
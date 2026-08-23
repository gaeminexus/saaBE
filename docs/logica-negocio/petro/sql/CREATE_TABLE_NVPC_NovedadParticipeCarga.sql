-- =====================================================
-- Tabla: NVPC (NovedadParticipeCarga)
-- Descripción: Almacena las novedades encontradas para cada partícipe durante la carga
-- Un partícipe puede tener MÚLTIPLES novedades, incluso más de una por producto
-- Fecha: 2026-03-24
-- =====================================================

-- Crear la tabla
CREATE TABLE CRD.NVPC (
    NVPCCDGO NUMBER NOT NULL,                    -- PK: Código de la novedad
    PXCACDGO NUMBER NOT NULL,                    -- FK: Partícipe X Carga Archivo
    NVPCTPNV NUMBER NOT NULL,                    -- Tipo de novedad (rubro ASPNovedadesCargaArchivo)
    NVPCDSCR VARCHAR2(4000),                     -- Descripción de la novedad
    NVPCCDPR NUMBER,                             -- Código del producto relacionado (si aplica)
    NVPCCDPS NUMBER,                             -- Código del préstamo relacionado (si aplica)
    NVPCCDCT NUMBER,                             -- Código de la cuota relacionada (si aplica)
    NVPCMNES NUMBER,                             -- Monto esperado del sistema
    NVPCMNRC NUMBER,                             -- Monto recibido del archivo
    NVPCMNDF NUMBER,                             -- Diferencia entre montos
    NVPCESTD NUMBER DEFAULT 1 NOT NULL,          -- Estado del registro (1=ACTIVO, 2=INACTIVO)
    CONSTRAINT PK_NVPC PRIMARY KEY (NVPCCDGO),
    CONSTRAINT FK_NVPC_PXCA FOREIGN KEY (PXCACDGO) REFERENCES CRD.PXCA(PXCACDGO)
);

-- Crear índice para búsquedas por partícipe
CREATE INDEX IDX_NVPC_PXCA ON CRD.NVPC (PXCACDGO);

-- Crear índice para búsquedas por tipo de novedad
CREATE INDEX IDX_NVPC_TIPO ON CRD.NVPC (NVPCTPNV);

-- Crear índice único para la clave primaria
CREATE UNIQUE INDEX PK_NVPC ON CRD.NVPC (NVPCCDGO);

-- Comentarios en la tabla
COMMENT ON TABLE CRD.NVPC IS 'Novedades encontradas para cada partícipe durante la carga del archivo Petrocomercial. Permite múltiples novedades por partícipe.';

-- Comentarios en las columnas
COMMENT ON COLUMN CRD.NVPC.NVPCCDGO IS 'Código único de la novedad (PK)';
COMMENT ON COLUMN CRD.NVPC.PXCACDGO IS 'FK: Código del partícipe al que pertenece la novedad';
COMMENT ON COLUMN CRD.NVPC.NVPCTPNV IS 'Tipo de novedad según rubro ASPNovedadesCargaArchivo (9-16)';
COMMENT ON COLUMN CRD.NVPC.NVPCDSCR IS 'Descripción detallada de la novedad encontrada';
COMMENT ON COLUMN CRD.NVPC.NVPCCDPR IS 'Código del producto relacionado con la novedad (opcional)';
COMMENT ON COLUMN CRD.NVPC.NVPCCDPS IS 'Código del préstamo relacionado con la novedad (opcional)';
COMMENT ON COLUMN CRD.NVPC.NVPCCDCT IS 'Código de la cuota relacionada con la novedad (opcional)';
COMMENT ON COLUMN CRD.NVPC.NVPCMNES IS 'Monto esperado según el sistema';
COMMENT ON COLUMN CRD.NVPC.NVPCMNRC IS 'Monto recibido del archivo Petrocomercial';
COMMENT ON COLUMN CRD.NVPC.NVPCMNDF IS 'Diferencia entre monto esperado y recibido';
COMMENT ON COLUMN CRD.NVPC.NVPCESTD IS 'Estado del registro (1=ACTIVO, 2=INACTIVO)';

-- =====================================================
-- Script de verificación
-- Ejecuta esto después de crear la tabla para verificar:
-- =====================================================
-- SELECT table_name, column_name, data_type, nullable, data_default
-- FROM user_tab_columns
-- WHERE table_name = 'NVPC'
-- ORDER BY column_id;
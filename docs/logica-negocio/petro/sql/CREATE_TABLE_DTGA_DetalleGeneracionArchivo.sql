-- ============================================================================
-- TABLA: DTGA - Detalle Generación Archivo (por tipo de producto)
-- Descripción: Almacena el resumen por tipo de producto de cada generación
-- ============================================================================

CREATE TABLE CRD.DTGA (
    -- Código único del detalle
    DTGACDGO NUMBER(10) NOT NULL,
    
    -- FK - Código de la generación (GNAP)
    GNAPCDGO NUMBER(10) NOT NULL,
    
    -- Código del producto Petrocomercial (AH, HS, PE, PH, PQ, PP)
    DTGACDPT VARCHAR2(2) NOT NULL,
    
    -- Total de registros de este tipo de producto
    DTGATRRG NUMBER(10) DEFAULT 0,
    
    -- Total monto de este tipo de producto
    DTGATMTO NUMBER(15,2) DEFAULT 0,
    
    -- Descripción del tipo de producto
    DTGADSCP VARCHAR2(200),
    
    -- Auditoría: Usuario ingreso
    DTGAUSIN VARCHAR2(50),
    
    -- Auditoría: Fecha ingreso
    DTGAFCIN DATE DEFAULT SYSDATE,
    
    -- Auditoría: Usuario modificación
    DTGAUSMO VARCHAR2(50),
    
    -- Auditoría: Fecha modificación
    DTGAFCMO DATE,
    
    -- CONSTRAINT: Primary Key
    CONSTRAINT PK_DTGA PRIMARY KEY (DTGACDGO),
    
    -- CONSTRAINT: Foreign Key a GNAP
    CONSTRAINT FK_DTGA_GNAP FOREIGN KEY (GNAPCDGO) 
        REFERENCES CRD.GNAP(GNAPCDGO) ON DELETE CASCADE,
    
    -- CONSTRAINT: Unique por generación-producto
    CONSTRAINT UK_DTGA_GNAP_PROD UNIQUE (GNAPCDGO, DTGACDPT),
    
    -- CONSTRAINT: Check de códigos válidos
    CONSTRAINT CK_DTGA_CODIGO_PRODUCTO CHECK (
        DTGACDPT IN ('AH', 'HS', 'PE', 'PH', 'PQ', 'PP')
    )
);

-- Comentarios de la tabla
COMMENT ON TABLE CRD.DTGA IS 'Detalle Generación Archivo - Resumen por tipo de producto';
COMMENT ON COLUMN CRD.DTGA.DTGACDGO IS 'Código único del detalle';
COMMENT ON COLUMN CRD.DTGA.GNAPCDGO IS 'FK a Generación Archivo';
COMMENT ON COLUMN CRD.DTGA.DTGACDPT IS 'Código producto Petrocomercial (AH/HS/PE/PH/PQ/PP)';
COMMENT ON COLUMN CRD.DTGA.DTGATRRG IS 'Total registros del producto';
COMMENT ON COLUMN CRD.DTGA.DTGATMTO IS 'Total monto del producto';
COMMENT ON COLUMN CRD.DTGA.DTGADSCP IS 'Descripción del producto';

-- Sequence para el código
CREATE SEQUENCE CRD.SEQ_DTGA
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Índices adicionales
CREATE INDEX IDX_DTGA_GNAP ON CRD.DTGA(GNAPCDGO);
CREATE INDEX IDX_DTGA_PRODUCTO ON CRD.DTGA(DTGACDPT);

-- Grant de permisos
GRANT SELECT, INSERT, UPDATE, DELETE ON CRD.DTGA TO ROLE_CRD;
GRANT SELECT ON CRD.SEQ_DTGA TO ROLE_CRD;

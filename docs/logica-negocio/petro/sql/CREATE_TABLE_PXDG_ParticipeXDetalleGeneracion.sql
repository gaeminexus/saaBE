-- ============================================================================
-- TABLA: PXDG - Partícipe por Detalle Generación
-- Descripción: Almacena el detalle de cada partícipe-producto enviado
--              Una línea por cada registro del archivo TXT generado
-- ============================================================================

CREATE TABLE CRD.PXDG (
    -- Código único del registro
    PXDGCDGO NUMBER(10) NOT NULL,
    
    -- FK - Código del detalle de generación (DTGA)
    DTGACDGO NUMBER(10) NOT NULL,
    
    -- FK - Código de la entidad (partícipe)
    ENTDCDGO NUMBER(10) NOT NULL,
    
    -- Rol Petrocomercial del partícipe
    PXDGRLPT NUMBER(10),
    
    -- Código producto Petrocomercial (AH, HS, PE, PH, PQ, PP)
    PXDGCDPT VARCHAR2(2) NOT NULL,
    
    -- Monto enviado a descontar
    PXDGMNEN NUMBER(15,2) NOT NULL,
    
    -- FK - Código del préstamo (NULL para AH)
    PRSTCDGO NUMBER(10),
    
    -- Número de línea en el archivo TXT
    PXDGNLIN NUMBER(10),
    
    -- Observaciones
    PXDGOBSR VARCHAR2(500),
    
    -- Auditoría: Usuario ingreso
    PXDGUSIN VARCHAR2(50),
    
    -- Auditoría: Fecha ingreso
    PXDGFCIN DATE DEFAULT SYSDATE,
    
    -- Auditoría: Usuario modificación
    PXDGUSMO VARCHAR2(50),
    
    -- Auditoría: Fecha modificación
    PXDGFCMO DATE,
    
    -- CONSTRAINT: Primary Key
    CONSTRAINT PK_PXDG PRIMARY KEY (PXDGCDGO),
    
    -- CONSTRAINT: Foreign Key a DTGA
    CONSTRAINT FK_PXDG_DTGA FOREIGN KEY (DTGACDGO) 
        REFERENCES CRD.DTGA(DTGACDGO) ON DELETE CASCADE,
    
    -- CONSTRAINT: Foreign Key a Entidad
    CONSTRAINT FK_PXDG_ENTD FOREIGN KEY (ENTDCDGO) 
        REFERENCES CRD.ENTD(ENTDCDGO),
    
    -- CONSTRAINT: Foreign Key a Prestamo (opcional)
    CONSTRAINT FK_PXDG_PRST FOREIGN KEY (PRSTCDGO) 
        REFERENCES CRD.PRST(PRSTCDGO),
    
    -- CONSTRAINT: Check de códigos válidos
    CONSTRAINT CK_PXDG_CODIGO_PRODUCTO CHECK (
        PXDGCDPT IN ('AH', 'HS', 'PE', 'PH', 'PQ', 'PP')
    ),
    
    -- CONSTRAINT: Monto debe ser mayor a 0
    CONSTRAINT CK_PXDG_MONTO_POSITIVO CHECK (PXDGMNEN > 0),
    
    -- CONSTRAINT: Unique por detalle-entidad-producto
    CONSTRAINT UK_PXDG_DETALLE_ENT_PROD UNIQUE (DTGACDGO, ENTDCDGO, PXDGCDPT)
);

-- Comentarios de la tabla
COMMENT ON TABLE CRD.PXDG IS 'Partícipe por Detalle Generación - Registro individual por línea del archivo';
COMMENT ON COLUMN CRD.PXDG.PXDGCDGO IS 'Código único del registro';
COMMENT ON COLUMN CRD.PXDG.DTGACDGO IS 'FK a Detalle Generación';
COMMENT ON COLUMN CRD.PXDG.ENTDCDGO IS 'FK a Entidad (partícipe)';
COMMENT ON COLUMN CRD.PXDG.PXDGRLPT IS 'Rol Petrocomercial del partícipe';
COMMENT ON COLUMN CRD.PXDG.PXDGCDPT IS 'Código producto Petrocomercial';
COMMENT ON COLUMN CRD.PXDG.PXDGMNEN IS 'Monto enviado a descontar';
COMMENT ON COLUMN CRD.PXDG.PRSTCDGO IS 'FK a Préstamo (NULL para AH)';
COMMENT ON COLUMN CRD.PXDG.PXDGNLIN IS 'Número de línea en el archivo TXT';

-- Sequence para el código
CREATE SEQUENCE CRD.SEQ_PXDG
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Índices adicionales
CREATE INDEX IDX_PXDG_DTGA ON CRD.PXDG(DTGACDGO);
CREATE INDEX IDX_PXDG_ENTIDAD ON CRD.PXDG(ENTDCDGO);
CREATE INDEX IDX_PXDG_PRESTAMO ON CRD.PXDG(PRSTCDGO);
CREATE INDEX IDX_PXDG_PRODUCTO ON CRD.PXDG(PXDGCDPT);
CREATE INDEX IDX_PXDG_ROL_PETRO ON CRD.PXDG(PXDGRLPT);

-- Grant de permisos
GRANT SELECT, INSERT, UPDATE, DELETE ON CRD.PXDG TO ROLE_CRD;
GRANT SELECT ON CRD.SEQ_PXDG TO ROLE_CRD;

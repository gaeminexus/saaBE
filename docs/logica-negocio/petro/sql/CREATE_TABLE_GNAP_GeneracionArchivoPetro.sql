-- ============================================================================
-- TABLA: GNAP - Generación Archivo Petrocomercial
-- Descripción: Almacena la cabecera de cada generación mensual del archivo
--              de descuentos que se envía a Petrocomercial
-- ============================================================================

CREATE TABLE CRD.GNAP (
    -- Código único de la generación
    GNAPCDGO NUMBER(10) NOT NULL,
    
    -- Mes del periodo (1-12)
    GNAPMSPE NUMBER(2) NOT NULL,
    
    -- Año del periodo (ej: 2025)
    GNAPANPE NUMBER(4) NOT NULL,
    
    -- Fecha de generación del archivo
    GNAPFCGN DATE NOT NULL,
    
    -- Usuario que generó el archivo
    GNAPUSGN VARCHAR2(50) NOT NULL,
    
    -- Total de registros en el archivo
    GNAPTRGN NUMBER(10) DEFAULT 0,
    
    -- Total monto enviado a descontar (suma de todos los productos)
    GNAPTMEN NUMBER(15,2) DEFAULT 0,
    
    -- Estado: GENERADO, ENVIADO, PROCESADO
    GNAPESTA VARCHAR2(20) DEFAULT 'GENERADO',
    
    -- Ruta física del archivo generado
    GNAPRTAA VARCHAR2(500),
    
    -- Nombre del archivo generado
    GNAPNMAR VARCHAR2(200),
    
    -- Fecha de envío a Petrocomercial
    GNAPFCEN DATE,
    
    -- Fecha de procesamiento (respuesta recibida)
    GNAPFCPR DATE,
    
    -- Observaciones
    GNAPOBSR VARCHAR2(4000),
    
    -- FK - Código Filial
    FLLLCDGO NUMBER(10),
    
    -- Auditoría: Usuario ingreso
    GNAPUSIN VARCHAR2(50),
    
    -- Auditoría: Fecha ingreso
    GNAPFCIN DATE DEFAULT SYSDATE,
    
    -- Auditoría: Usuario modificación
    GNAPUSMO VARCHAR2(50),
    
    -- Auditoría: Fecha modificación
    GNAPFCMO DATE,
    
    -- CONSTRAINT: Primary Key
    CONSTRAINT PK_GNAP PRIMARY KEY (GNAPCDGO),
    
    -- CONSTRAINT: Foreign Key a Filial
    CONSTRAINT FK_GNAP_FLLL FOREIGN KEY (FLLLCDGO) 
        REFERENCES CRD.FLLL(FLLLCDGO),
    
    -- CONSTRAINT: Unique por periodo (mes-año-filial)
    CONSTRAINT UK_GNAP_PERIODO UNIQUE (GNAPMSPE, GNAPANPE, FLLLCDGO)
);

-- Comentarios de la tabla
COMMENT ON TABLE CRD.GNAP IS 'Generación Archivo Petrocomercial - Cabecera';
COMMENT ON COLUMN CRD.GNAP.GNAPCDGO IS 'Código único de generación';
COMMENT ON COLUMN CRD.GNAP.GNAPMSPE IS 'Mes del periodo (1-12)';
COMMENT ON COLUMN CRD.GNAP.GNAPANPE IS 'Año del periodo';
COMMENT ON COLUMN CRD.GNAP.GNAPFCGN IS 'Fecha de generación';
COMMENT ON COLUMN CRD.GNAP.GNAPUSGN IS 'Usuario generador';
COMMENT ON COLUMN CRD.GNAP.GNAPTRGN IS 'Total registros';
COMMENT ON COLUMN CRD.GNAP.GNAPTMEN IS 'Total monto enviado';
COMMENT ON COLUMN CRD.GNAP.GNAPESTA IS 'Estado: GENERADO/ENVIADO/PROCESADO';
COMMENT ON COLUMN CRD.GNAP.GNAPRTAA IS 'Ruta del archivo';
COMMENT ON COLUMN CRD.GNAP.GNAPNMAR IS 'Nombre del archivo';

-- Sequence para el código
CREATE SEQUENCE CRD.SEQ_GNAP
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Índices adicionales
CREATE INDEX IDX_GNAP_PERIODO ON CRD.GNAP(GNAPMSPE, GNAPANPE);
CREATE INDEX IDX_GNAP_ESTADO ON CRD.GNAP(GNAPESTA);
CREATE INDEX IDX_GNAP_FECHA ON CRD.GNAP(GNAPFCGN);

-- Grant de permisos (ajustar según sea necesario)
GRANT SELECT, INSERT, UPDATE ON CRD.GNAP TO ROLE_CRD;
GRANT SELECT ON CRD.SEQ_GNAP TO ROLE_CRD;

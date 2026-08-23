-- ============================================================
-- SCRIPTS DE CREACIÓN DE TABLAS
-- Sistema de Generación de Archivos Petrocomercial
-- Fecha: 9 de abril de 2026
-- ============================================================

-- ============================================================
-- 1. TABLA GNAP - Generación Archivo Petrocomercial (CABECERA)
-- ============================================================
-- Almacena la información de cada generación mensual del archivo de descuentos

CREATE TABLE CRD.GNAP (
    GNAPCDGO    NUMBER(19,0)    NOT NULL,   -- Código único (PK)
    GNAPMSPE    NUMBER(10,0)    NOT NULL,   -- Mes del periodo (1-12)
    GNAPANPE    NUMBER(10,0)    NOT NULL,   -- Año del periodo (ej: 2026)
    GNAPFCGN    TIMESTAMP       NOT NULL,   -- Fecha de generación del archivo
    GNAPUSGN    VARCHAR2(50)    NOT NULL,   -- Usuario que generó el archivo
    GNAPTRGN    NUMBER(19,0),               -- Total de registros en el archivo
    GNAPTMEN    NUMBER(19,2),               -- Total monto enviado a descontar
    GNAPESTA    VARCHAR2(20),               -- Estado: GENERADO, ENVIADO, PROCESADO
    GNAPRTAA    VARCHAR2(500),              -- Ruta física del archivo generado
    GNAPNMAR    VARCHAR2(200),              -- Nombre del archivo generado
    GNAPFCEN    TIMESTAMP,                  -- Fecha de envío a Petrocomercial
    GNAPFCPR    TIMESTAMP,                  -- Fecha de procesamiento (respuesta recibida)
    GNAPOBSR    VARCHAR2(4000),             -- Observaciones
    FLLLCDGO    NUMBER(19,0),               -- FK: Filial
    GNAPUSIN    VARCHAR2(50),               -- Usuario ingreso
    GNAPFCIN    TIMESTAMP,                  -- Fecha ingreso
    GNAPUSMO    VARCHAR2(50),               -- Usuario modificación
    GNAPFCMO    TIMESTAMP,                  -- Fecha modificación
    
    -- Primary Key
    CONSTRAINT PK_GNAP PRIMARY KEY (GNAPCDGO),
    
    -- Foreign Key a Filial
    CONSTRAINT FK_GNAP_FLLL FOREIGN KEY (FLLLCDGO) 
        REFERENCES CRD.FLLL(FLLLCDGO),
    
    -- Constraint: mes entre 1 y 12
    CONSTRAINT CHK_GNAP_MES CHECK (GNAPMSPE BETWEEN 1 AND 12),
    
    -- Constraint: año válido
    CONSTRAINT CHK_GNAP_ANIO CHECK (GNAPANPE >= 2000 AND GNAPANPE <= 2100),
    
    -- Constraint: estado válido
    CONSTRAINT CHK_GNAP_ESTADO CHECK (GNAPESTA IN ('GENERADO', 'ENVIADO', 'PROCESADO', 'ERROR'))
);

-- Índices para mejorar performance
CREATE INDEX IDX_GNAP_PERIODO ON CRD.GNAP(GNAPMSPE, GNAPANPE, FLLLCDGO);
CREATE INDEX IDX_GNAP_ESTADO ON CRD.GNAP(GNAPESTA);
CREATE INDEX IDX_GNAP_FECHA ON CRD.GNAP(GNAPFCGN);
CREATE INDEX IDX_GNAP_FILIAL ON CRD.GNAP(FLLLCDGO);

-- Secuencia para generar códigos
CREATE SEQUENCE CRD.SEQ_GNAP
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Comentarios en la tabla
COMMENT ON TABLE CRD.GNAP IS 'Generación Archivo Petrocomercial - Cabecera de cada generación mensual';
COMMENT ON COLUMN CRD.GNAP.GNAPCDGO IS 'Código único de la generación';
COMMENT ON COLUMN CRD.GNAP.GNAPMSPE IS 'Mes del periodo (1-12)';
COMMENT ON COLUMN CRD.GNAP.GNAPANPE IS 'Año del periodo';
COMMENT ON COLUMN CRD.GNAP.GNAPFCGN IS 'Fecha de generación del archivo';
COMMENT ON COLUMN CRD.GNAP.GNAPUSGN IS 'Usuario que generó el archivo';
COMMENT ON COLUMN CRD.GNAP.GNAPTRGN IS 'Total de registros en el archivo';
COMMENT ON COLUMN CRD.GNAP.GNAPTMEN IS 'Total monto enviado a descontar';
COMMENT ON COLUMN CRD.GNAP.GNAPESTA IS 'Estado: GENERADO, ENVIADO, PROCESADO, ERROR';
COMMENT ON COLUMN CRD.GNAP.GNAPRTAA IS 'Ruta física del archivo generado';
COMMENT ON COLUMN CRD.GNAP.GNAPNMAR IS 'Nombre del archivo generado';


-- ============================================================
-- 2. TABLA DTGA - Detalle Generación Archivo (POR PRODUCTO)
-- ============================================================
-- Agrupa los totales por tipo de producto (AH, HS, PE, PH, PQ, PP)

CREATE TABLE CRD.DTGA (
    DTGACDGO    NUMBER(19,0)    NOT NULL,   -- Código único (PK)
    GNAPCDGO    NUMBER(19,0)    NOT NULL,   -- FK: Generación Archivo Petrocomercial
    DTGACDPT    VARCHAR2(2)     NOT NULL,   -- Código producto Petro (AH, HS, PE, PH, PQ, PP)
    DTGATRRG    NUMBER(19,0),               -- Total registros de este tipo de producto
    DTGATMTO    NUMBER(19,2),               -- Total monto de este tipo de producto
    DTGADSCP    VARCHAR2(200),              -- Descripción del tipo de producto
    DTGAUSIN    VARCHAR2(50),               -- Usuario ingreso
    DTGAFCIN    TIMESTAMP,                  -- Fecha ingreso
    DTGAUSMO    VARCHAR2(50),               -- Usuario modificación
    DTGAFCMO    TIMESTAMP,                  -- Fecha modificación
    
    -- Primary Key
    CONSTRAINT PK_DTGA PRIMARY KEY (DTGACDGO),
    
    -- Foreign Key a Generación
    CONSTRAINT FK_DTGA_GNAP FOREIGN KEY (GNAPCDGO) 
        REFERENCES CRD.GNAP(GNAPCDGO) ON DELETE CASCADE,
    
    -- Constraint: código de producto válido
    CONSTRAINT CHK_DTGA_PRODUCTO CHECK (DTGACDPT IN ('AH', 'HS', 'PE', 'PH', 'PQ', 'PP')),
    
    -- Constraint: único producto por generación
    CONSTRAINT UK_DTGA_GEN_PROD UNIQUE (GNAPCDGO, DTGACDPT)
);

-- Índices para mejorar performance
CREATE INDEX IDX_DTGA_GENERACION ON CRD.DTGA(GNAPCDGO);
CREATE INDEX IDX_DTGA_PRODUCTO ON CRD.DTGA(DTGACDPT);

-- Secuencia para generar códigos
CREATE SEQUENCE CRD.SEQ_DTGA
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Comentarios en la tabla
COMMENT ON TABLE CRD.DTGA IS 'Detalle Generación Archivo - Resumen por tipo de producto';
COMMENT ON COLUMN CRD.DTGA.DTGACDGO IS 'Código único del detalle';
COMMENT ON COLUMN CRD.DTGA.GNAPCDGO IS 'FK: Código de la generación';
COMMENT ON COLUMN CRD.DTGA.DTGACDPT IS 'Código producto Petrocomercial (AH=Aporte, HS=Hipotecario Salud, PE=Emergencia, etc)';
COMMENT ON COLUMN CRD.DTGA.DTGATRRG IS 'Total de registros de este producto';
COMMENT ON COLUMN CRD.DTGA.DTGATMTO IS 'Total monto de este producto';


-- ============================================================
-- 3. TABLA PDTG - Partícipe Detalle Generación (LÍNEAS INDIVIDUALES)
-- ============================================================
-- Detalla cada línea del archivo por partícipe (una línea = un descuento)

CREATE TABLE CRD.PDTG (
    PDTGCDGO    NUMBER(19,0)    NOT NULL,   -- Código único (PK)
    DTGACDGO    NUMBER(19,0)    NOT NULL,   -- FK: Detalle Generación Archivo
    ENTDCDGO    NUMBER(19,0)    NOT NULL,   -- FK: Entidad (partícipe)
    PRSTCDGO    NUMBER(19,0),               -- FK: Préstamo (si aplica, null para aportes)
    PDTGRLPC    NUMBER(19,0),               -- Rol Petrocomercial del partícipe
    PDTGCDPT    VARCHAR2(2)     NOT NULL,   -- Código producto Petro
    PDTGMNEN    NUMBER(19,2)    NOT NULL,   -- Monto enviado a descontar
    PDTGNMLN    NUMBER(19,0),               -- Número de línea en el archivo
    PDTGOBSR    VARCHAR2(500),              -- Observaciones (ej: "Cuota #5", "Aporte personal")
    PDTGESTA    VARCHAR2(20),               -- Estado del descuento: ENVIADO, DESCONTADO, ERROR
    PDTGMNDC    NUMBER(19,2),               -- Monto efectivamente descontado (respuesta Petro)
    PDTGFCDC    TIMESTAMP,                  -- Fecha en que se descontó
    PDTGUSIN    VARCHAR2(50),               -- Usuario ingreso
    PDTGFCIN    TIMESTAMP,                  -- Fecha ingreso
    PDTGUSMO    VARCHAR2(50),               -- Usuario modificación
    PDTGFCMO    TIMESTAMP,                  -- Fecha modificación
    
    -- Primary Key
    CONSTRAINT PK_PDTG PRIMARY KEY (PDTGCDGO),
    
    -- Foreign Key a Detalle Generación
    CONSTRAINT FK_PDTG_DTGA FOREIGN KEY (DTGACDGO) 
        REFERENCES CRD.DTGA(DTGACDGO) ON DELETE CASCADE,
    
    -- Foreign Key a Entidad
    CONSTRAINT FK_PDTG_ENTD FOREIGN KEY (ENTDCDGO) 
        REFERENCES CRD.ENTD(ENTDCDGO),
    
    -- Foreign Key a Préstamo (opcional)
    CONSTRAINT FK_PDTG_PRST FOREIGN KEY (PRSTCDGO) 
        REFERENCES CRD.PRST(PRSTCDGO),
    
    -- Constraint: código de producto válido
    CONSTRAINT CHK_PDTG_PRODUCTO CHECK (PDTGCDPT IN ('AH', 'HS', 'PE', 'PH', 'PQ', 'PP')),
    
    -- Constraint: monto positivo
    CONSTRAINT CHK_PDTG_MONTO CHECK (PDTGMNEN > 0),
    
    -- Constraint: estado válido
    CONSTRAINT CHK_PDTG_ESTADO CHECK (PDTGESTA IS NULL OR PDTGESTA IN ('ENVIADO', 'DESCONTADO', 'ERROR', 'DEVUELTO'))
);

-- Índices para mejorar performance
CREATE INDEX IDX_PDTG_DETALLE ON CRD.PDTG(DTGACDGO);
CREATE INDEX IDX_PDTG_ENTIDAD ON CRD.PDTG(ENTDCDGO);
CREATE INDEX IDX_PDTG_PRESTAMO ON CRD.PDTG(PRSTCDGO);
CREATE INDEX IDX_PDTG_PRODUCTO ON CRD.PDTG(PDTGCDPT);
CREATE INDEX IDX_PDTG_ESTADO ON CRD.PDTG(PDTGESTA);
CREATE INDEX IDX_PDTG_NUMLINEA ON CRD.PDTG(PDTGNMLN);

-- Secuencia para generar códigos
CREATE SEQUENCE CRD.SEQ_PDTG
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Comentarios en la tabla
COMMENT ON TABLE CRD.PDTG IS 'Partícipe Detalle Generación - Líneas individuales del archivo';
COMMENT ON COLUMN CRD.PDTG.PDTGCDGO IS 'Código único del registro';
COMMENT ON COLUMN CRD.PDTG.DTGACDGO IS 'FK: Código del detalle de generación';
COMMENT ON COLUMN CRD.PDTG.ENTDCDGO IS 'FK: Código de la entidad (partícipe)';
COMMENT ON COLUMN CRD.PDTG.PRSTCDGO IS 'FK: Código del préstamo (null si es aporte personal)';
COMMENT ON COLUMN CRD.PDTG.PDTGRLPC IS 'Rol Petrocomercial del partícipe';
COMMENT ON COLUMN CRD.PDTG.PDTGCDPT IS 'Código producto Petrocomercial';
COMMENT ON COLUMN CRD.PDTG.PDTGMNEN IS 'Monto enviado a descontar';
COMMENT ON COLUMN CRD.PDTG.PDTGNMLN IS 'Número de línea en el archivo TXT';
COMMENT ON COLUMN CRD.PDTG.PDTGOBSR IS 'Observaciones';
COMMENT ON COLUMN CRD.PDTG.PDTGESTA IS 'Estado: ENVIADO, DESCONTADO, ERROR, DEVUELTO';
COMMENT ON COLUMN CRD.PDTG.PDTGMNDC IS 'Monto efectivamente descontado';


-- ============================================================
-- GRANTS (Ajustar según usuarios de la base de datos)
-- ============================================================

-- Dar permisos al usuario de la aplicación (ajustar nombre de usuario)
GRANT SELECT, INSERT, UPDATE, DELETE ON CRD.GNAP TO SAABE_USER;
GRANT SELECT, INSERT, UPDATE, DELETE ON CRD.DTGA TO SAABE_USER;
GRANT SELECT, INSERT, UPDATE, DELETE ON CRD.PDTG TO SAABE_USER;

-- Permisos en las secuencias
GRANT SELECT ON CRD.SEQ_GNAP TO SAABE_USER;
GRANT SELECT ON CRD.SEQ_DTGA TO SAABE_USER;
GRANT SELECT ON CRD.SEQ_PDTG TO SAABE_USER;


-- ============================================================
-- VISTAS ÚTILES PARA CONSULTAS
-- ============================================================

-- Vista resumen de generaciones con totales
CREATE OR REPLACE VIEW CRD.VW_GENERACIONES_RESUMEN AS
SELECT 
    g.GNAPCDGO AS codigo,
    g.GNAPMSPE AS mes,
    g.GNAPANPE AS anio,
    CASE g.GNAPMSPE
        WHEN 1 THEN 'ENERO'
        WHEN 2 THEN 'FEBRERO'
        WHEN 3 THEN 'MARZO'
        WHEN 4 THEN 'ABRIL'
        WHEN 5 THEN 'MAYO'
        WHEN 6 THEN 'JUNIO'
        WHEN 7 THEN 'JULIO'
        WHEN 8 THEN 'AGOSTO'
        WHEN 9 THEN 'SEPTIEMBRE'
        WHEN 10 THEN 'OCTUBRE'
        WHEN 11 THEN 'NOVIEMBRE'
        WHEN 12 THEN 'DICIEMBRE'
    END || ' ' || g.GNAPANPE AS periodo_texto,
    g.GNAPFCGN AS fecha_generacion,
    g.GNAPUSGN AS usuario,
    g.GNAPTRGN AS total_registros,
    g.GNAPTMEN AS total_monto,
    g.GNAPESTA AS estado,
    g.GNAPNMAR AS nombre_archivo,
    f.FLLLNMBR AS filial,
    COUNT(DISTINCT d.DTGACDGO) AS total_productos
FROM CRD.GNAP g
LEFT JOIN CRD.FLLL f ON g.FLLLCDGO = f.FLLLCDGO
LEFT JOIN CRD.DTGA d ON g.GNAPCDGO = d.GNAPCDGO
GROUP BY 
    g.GNAPCDGO, g.GNAPMSPE, g.GNAPANPE, g.GNAPFCGN,
    g.GNAPUSGN, g.GNAPTRGN, g.GNAPTMEN, g.GNAPESTA,
    g.GNAPNMAR, f.FLLLNMBR
ORDER BY g.GNAPFCGN DESC;

COMMENT ON VIEW CRD.VW_GENERACIONES_RESUMEN IS 'Vista resumen de generaciones de archivos Petrocomercial';


-- Vista detalle completo de líneas
CREATE OR REPLACE VIEW CRD.VW_LINEAS_DETALLE AS
SELECT 
    pd.PDTGCDGO AS codigo_linea,
    pd.PDTGNMLN AS numero_linea,
    g.GNAPCDGO AS codigo_generacion,
    g.GNAPMSPE || '/' || g.GNAPANPE AS periodo,
    e.ENTDCDGO AS codigo_entidad,
    e.ENTDNMID AS identificacion,
    e.ENTDRZNS AS nombre_participe,
    pd.PDTGRLPC AS rol_petrocomercial,
    pd.PDTGCDPT AS codigo_producto,
    CASE pd.PDTGCDPT
        WHEN 'AH' THEN 'Aporte Habitacional'
        WHEN 'HS' THEN 'Hipotecario Salud'
        WHEN 'PE' THEN 'Préstamo Emergencia'
        WHEN 'PH' THEN 'Préstamo Hipotecario'
        WHEN 'PQ' THEN 'Préstamo Quirografario'
        WHEN 'PP' THEN 'Préstamo Prendario'
    END AS nombre_producto,
    pd.PDTGMNEN AS monto_enviado,
    pd.PDTGMNDC AS monto_descontado,
    pd.PDTGESTA AS estado,
    pd.PDTGOBSR AS observaciones,
    p.PRSTCDGO AS codigo_prestamo,
    pd.PDTGFCDC AS fecha_descuento
FROM CRD.PDTG pd
INNER JOIN CRD.DTGA d ON pd.DTGACDGO = d.DTGACDGO
INNER JOIN CRD.GNAP g ON d.GNAPCDGO = g.GNAPCDGO
INNER JOIN CRD.ENTD e ON pd.ENTDCDGO = e.ENTDCDGO
LEFT JOIN CRD.PRST p ON pd.PRSTCDGO = p.PRSTCDGO
ORDER BY pd.PDTGNMLN;

COMMENT ON VIEW CRD.VW_LINEAS_DETALLE IS 'Vista detalle completo de líneas de archivos Petrocomercial';


-- ============================================================
-- VERIFICACIÓN DE INSTALACIÓN
-- ============================================================

-- Query para verificar que las tablas se crearon correctamente
SELECT 
    table_name,
    num_rows,
    last_analyzed
FROM user_tables
WHERE table_name IN ('GNAP', 'DTGA', 'PDTG')
ORDER BY table_name;

-- Query para verificar las secuencias
SELECT 
    sequence_name,
    min_value,
    max_value,
    increment_by,
    last_number
FROM user_sequences
WHERE sequence_name IN ('SEQ_GNAP', 'SEQ_DTGA', 'SEQ_PDTG')
ORDER BY sequence_name;

-- Query para verificar las constraints
SELECT 
    constraint_name,
    constraint_type,
    table_name,
    status
FROM user_constraints
WHERE table_name IN ('GNAP', 'DTGA', 'PDTG')
ORDER BY table_name, constraint_type;


-- ============================================================
-- FIN DEL SCRIPT
-- ============================================================

PROMPT '============================================';
PROMPT 'Tablas creadas exitosamente:';
PROMPT '  - CRD.GNAP (Generación Archivo Petrocomercial)';
PROMPT '  - CRD.DTGA (Detalle Generación Archivo)';
PROMPT '  - CRD.PDTG (Partícipe Detalle Generación)';
PROMPT '============================================';
PROMPT 'Secuencias creadas:';
PROMPT '  - CRD.SEQ_GNAP';
PROMPT '  - CRD.SEQ_DTGA';
PROMPT '  - CRD.SEQ_PDTG';
PROMPT '============================================';
PROMPT 'Vistas creadas:';
PROMPT '  - CRD.VW_GENERACIONES_RESUMEN';
PROMPT '  - CRD.VW_LINEAS_DETALLE';
PROMPT '============================================';
PROMPT 'NOTA: Los códigos de productos Petrocomercial';
PROMPT 'se obtienen de la tabla PRDC (Productos) existente,';
PROMPT 'campo PRDCCDPT (codigoPetro)';
PROMPT '============================================';

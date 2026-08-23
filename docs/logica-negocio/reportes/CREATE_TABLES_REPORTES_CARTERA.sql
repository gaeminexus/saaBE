-- ================================================================
-- SCRIPTS DE CREACIÓN DE TABLAS PARA REPORTES DE CARTERA
-- Esquema: RPR (Oracle)
-- Fecha: 2026-06-05
-- Tablas: EJCC, CPRM, CJBM, CCPM
-- NOTA: Las tablas históricas HMPR, HMJB, HMCP fueron eliminadas
--       del script porque no se usan en ningún proceso:
--       - HMJB: el CJBM lee de RPR.HM44 (mismo histórico que G44)
--       - HMCP: la provisionConstituida siempre va en 0 en el CCPM
--       - HMPR: nunca se escribió ni leyó
-- ================================================================

-- ================================================================
-- PASO 1: GRANTS de CRD a RPR
-- Ejecutar con usuario DBA o con el usuario CRD
-- ================================================================
GRANT REFERENCES, SELECT ON CRD.TPAP TO RPR;
GRANT REFERENCES, SELECT ON CRD.ENTD TO RPR;

-- ================================================================
-- PASO 2: DROP previo para permitir recreación limpia
-- Solo las 4 tablas que sí usamos (hijas primero, padre al final)
-- ================================================================
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE RPR.CCPM CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE RPR.CJBM CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE RPR.CPRM CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE RPR.EJCC CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

-- ================================================================
-- TABLA: EJCC - CONTROL DE EJECUCIÓN DE REPORTES DE CARTERA
-- ================================================================
CREATE TABLE RPR.EJCC (
    EJCCCDGO NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    EJCCMESS NUMBER(2)      NOT NULL,
    EJCCANOO NUMBER(4)      NOT NULL,
    EJCCUSRO VARCHAR2(50)   NOT NULL,
    EJCCFCGN DATE DEFAULT SYSDATE NOT NULL,
    EJCCOBSR VARCHAR2(500),
    CONSTRAINT CHK_EJCC_MES CHECK (EJCCMESS BETWEEN 1 AND 12)
);
COMMENT ON TABLE  RPR.EJCC          IS 'Control de ejecución de reportes de cartera (CPRM, CJBM, CCPM)';
COMMENT ON COLUMN RPR.EJCC.EJCCCDGO IS 'Código único de ejecución (Identity)';
COMMENT ON COLUMN RPR.EJCC.EJCCMESS IS 'Mes de ejecución (1-12)';
COMMENT ON COLUMN RPR.EJCC.EJCCANOO IS 'Año de ejecución';
COMMENT ON COLUMN RPR.EJCC.EJCCUSRO IS 'Usuario que ejecutó los reportes';
COMMENT ON COLUMN RPR.EJCC.EJCCFCGN IS 'Fecha de generación';
COMMENT ON COLUMN RPR.EJCC.EJCCOBSR IS 'Observaciones generales';
CREATE INDEX IDX_EJCC_MESANIO ON RPR.EJCC(EJCCMESS, EJCCANOO);

-- ================================================================
-- TABLA: CPRM - CRÉDITO PARTÍCIPES MENSUAL (similar a G42)
-- Un registro por cada combinación entidad + tipo de aporte.
-- ================================================================
CREATE TABLE RPR.CPRM (
    CPRMCDGO NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    CPRMTIDP VARCHAR2(50),
    CPRMIDPR VARCHAR2(50),
    TPAPCDGO NUMBER         NOT NULL,
    CPRMTTAL NUMBER(20,2)   DEFAULT 0,
    CPRMEJCC NUMBER         NOT NULL,
    ENTDCDGO NUMBER,
    CPRMSTEN VARCHAR2(50),                         -- Nombre del estado de la entidad
    CONSTRAINT FK_CPRM_EJCC FOREIGN KEY (CPRMEJCC) REFERENCES RPR.EJCC(EJCCCDGO),
    CONSTRAINT FK_CPRM_TPAP FOREIGN KEY (TPAPCDGO) REFERENCES CRD.TPAP(TPAPCDGO)
);
COMMENT ON TABLE  RPR.CPRM          IS 'CPRM - Crédito Partícipes Mensual. Un registro por entidad+tipoAporte.';
COMMENT ON COLUMN RPR.CPRM.CPRMCDGO IS 'Código único del registro (Identity)';
COMMENT ON COLUMN RPR.CPRM.CPRMTIDP IS 'Tipo de identificación del partícipe';
COMMENT ON COLUMN RPR.CPRM.CPRMIDPR IS 'Identificación del partícipe';
COMMENT ON COLUMN RPR.CPRM.TPAPCDGO IS 'FK al tipo de aporte (CRD.TPAP)';
COMMENT ON COLUMN RPR.CPRM.CPRMTTAL IS 'Total acumulado de aportes para este tipo hasta la fecha de corte';
COMMENT ON COLUMN RPR.CPRM.CPRMEJCC IS 'FK a control de ejecución (RPR.EJCC)';
COMMENT ON COLUMN RPR.CPRM.ENTDCDGO IS 'FK a entidad (CRD.ENTD)';
COMMENT ON COLUMN RPR.CPRM.CPRMSTEN IS 'Nombre del estado del partícipe, obtenido de CRD.ESPR.ESPRNMBR según el idEstado de CRD.ENTD';
CREATE INDEX IDX_CPRM_EJCC ON RPR.CPRM(CPRMEJCC);
CREATE INDEX IDX_CPRM_ENTD ON RPR.CPRM(ENTDCDGO);
CREATE INDEX IDX_CPRM_IDPR ON RPR.CPRM(CPRMIDPR);
CREATE INDEX IDX_CPRM_TPAP ON RPR.CPRM(TPAPCDGO);

-- ================================================================
-- TABLA: CJBM - CRÉDITO JUBILADOS MENSUAL (similar a G44)
-- ================================================================
CREATE TABLE RPR.CJBM (
    CJBMCDGO NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    CJBMTIDJ VARCHAR2(50),
    CJBMIDJB VARCHAR2(50),
    CJBMTPJB VARCHAR2(50),
    CJBMFCJB DATE,
    CJBMIAJB NUMBER(10)     DEFAULT 0,
    CJBMVLPN NUMBER(20,2)   DEFAULT 0,
    CJBMVNAR NUMBER(20,2)   DEFAULT 0,
    CJBMSCJB NUMBER(20,2)   DEFAULT 0,
    CJBMVCAP NUMBER(20,2)   DEFAULT 0,
    CJBMJEIS VARCHAR2(50),
    CJBMVLJB NUMBER(20,2)   DEFAULT 0,             -- Valor de jubilación (VPPC.valorPagar)
    CJBMVLSG NUMBER(20,2)   DEFAULT 0,             -- Valor del seguro (VPPC.valorSeguro)
    CJBMEJCC NUMBER         NOT NULL,
    CONSTRAINT FK_CJBM_EJCC FOREIGN KEY (CJBMEJCC) REFERENCES RPR.EJCC(EJCCCDGO)
);
COMMENT ON TABLE  RPR.CJBM          IS 'CJBM - Crédito Jubilados Mensual (similar a G44)';
COMMENT ON COLUMN RPR.CJBM.CJBMCDGO IS 'Código único del registro (Identity)';
COMMENT ON COLUMN RPR.CJBM.CJBMTIDJ IS 'Tipo de identificación del jubilado';
COMMENT ON COLUMN RPR.CJBM.CJBMIDJB IS 'Identificación del jubilado';
COMMENT ON COLUMN RPR.CJBM.CJBMTPJB IS 'Tipo de jubilación';
COMMENT ON COLUMN RPR.CJBM.CJBMFCJB IS 'Fecha de jubilación';
COMMENT ON COLUMN RPR.CJBM.CJBMIAJB IS 'Imposiciones acumuladas';
COMMENT ON COLUMN RPR.CJBM.CJBMVLPN IS 'Valor de la pensión';
COMMENT ON COLUMN RPR.CJBM.CJBMVNAR IS 'Valor neto a recibir';
COMMENT ON COLUMN RPR.CJBM.CJBMSCJB IS 'Saldo de cuenta del jubilado';
COMMENT ON COLUMN RPR.CJBM.CJBMVCAP IS 'Valores compensados';
COMMENT ON COLUMN RPR.CJBM.CJBMJEIS IS 'Jubilación en IESS (S/N)';
COMMENT ON COLUMN RPR.CJBM.CJBMVLJB IS 'Valor de jubilación (VPPC.VPPCVLRR - valorPagar)';
COMMENT ON COLUMN RPR.CJBM.CJBMVLSG IS 'Valor del seguro (VPPC.VPPCVLSR - valorSeguro)';
COMMENT ON COLUMN RPR.CJBM.CJBMEJCC IS 'FK a control de ejecución (RPR.EJCC)';
CREATE INDEX IDX_CJBM_EJCC ON RPR.CJBM(CJBMEJCC);
CREATE INDEX IDX_CJBM_IDJB ON RPR.CJBM(CJBMIDJB);

-- ================================================================
-- TABLA: CCPM - CRÉDITO CUOTAS PRÉSTAMOS MENSUAL (similar a G48)
-- Campos adicionales respecto a G48: CCPMVLDG, CCPMVLIN
-- ================================================================
CREATE TABLE RPR.CCPM (
    CCPMCDGO NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    CCPMTIDS VARCHAR2(50),
    CCPMIDSJ VARCHAR2(50),
    CCPMNMOP VARCHAR2(50),
    CCPMTPCR VARCHAR2(100),                        -- Nombre completo del producto
    CCPMDDMR NUMBER(10)     DEFAULT 0,
    CCPMCLPR VARCHAR2(50),
    CCPMTDIN NUMBER(20,6)   DEFAULT 0,
    CCPMVPVN NUMBER(20,2)   DEFAULT 0,
    CCPMVLVN NUMBER(20,2)   DEFAULT 0,
    CCPMCSPR NUMBER(20,2)   DEFAULT 0,
    CCPMINRD NUMBER(20,2)   DEFAULT 0,
    CCPMISMR NUMBER(20,2)   DEFAULT 0,
    CCPMVEDJ NUMBER(20,2)   DEFAULT 0,
    CCPMCRCS NUMBER(20,2)   DEFAULT 0,
    CCPMPRRO NUMBER(20,2)   DEFAULT 0,
    CCPMPRCN NUMBER(20,2)   DEFAULT 0,
    CCPMVTCI NUMBER(20,2)   DEFAULT 0,
    CCPMVSAP NUMBER(20,2)   DEFAULT 0,
    CCPMTDSA VARCHAR2(50),
    CCPMCDCR NUMBER(20,2)   DEFAULT 0,
    CCPMDVDN NUMBER(20,2)   DEFAULT 0,
    CCPMFDEC DATE,
    CCPMVLDG NUMBER(20,2)   DEFAULT 0,             -- Valor desgravamen (campo adicional)
    CCPMVLIN NUMBER(20,2)   DEFAULT 0,             -- Valor incendio (campo adicional)
    CCPMEJCC NUMBER         NOT NULL,
    CONSTRAINT FK_CCPM_EJCC FOREIGN KEY (CCPMEJCC) REFERENCES RPR.EJCC(EJCCCDGO)
);
COMMENT ON TABLE  RPR.CCPM          IS 'CCPM - Crédito Cuotas Préstamos Mensual (similar a G48 + desgravamen + incendio)';
COMMENT ON COLUMN RPR.CCPM.CCPMCDGO IS 'Código único del registro (Identity)';
COMMENT ON COLUMN RPR.CCPM.CCPMTIDS IS 'Tipo de identificación del sujeto';
COMMENT ON COLUMN RPR.CCPM.CCPMIDSJ IS 'Identificación del sujeto';
COMMENT ON COLUMN RPR.CCPM.CCPMNMOP IS 'Número de operación';
COMMENT ON COLUMN RPR.CCPM.CCPMTPCR IS 'Nombre completo del producto/tipo de crédito';
COMMENT ON COLUMN RPR.CCPM.CCPMDDMR IS 'Días de morosidad';
COMMENT ON COLUMN RPR.CCPM.CCPMCLPR IS 'Calificación propia';
COMMENT ON COLUMN RPR.CCPM.CCPMTDIN IS 'Tasa de interés';
COMMENT ON COLUMN RPR.CCPM.CCPMVPVN IS 'Valor por vencer';
COMMENT ON COLUMN RPR.CCPM.CCPMVLVN IS 'Valor vencido';
COMMENT ON COLUMN RPR.CCPM.CCPMCSPR IS 'Costos operativos';
COMMENT ON COLUMN RPR.CCPM.CCPMINRD IS 'Interés ordinario';
COMMENT ON COLUMN RPR.CCPM.CCPMISMR IS 'Interés sobre mora';
COMMENT ON COLUMN RPR.CCPM.CCPMVEDJ IS 'Valor en demanda judicial';
COMMENT ON COLUMN RPR.CCPM.CCPMCRCS IS 'Cartera castigada';
COMMENT ON COLUMN RPR.CCPM.CCPMPRRO IS 'Provisión requerida original';
COMMENT ON COLUMN RPR.CCPM.CCPMPRCN IS 'Provisión constituida (siempre 0 en primera generación)';
COMMENT ON COLUMN RPR.CCPM.CCPMVTCI IS 'Valor total cuenta individual';
COMMENT ON COLUMN RPR.CCPM.CCPMVSAP IS 'Valor sujeto a provisión';
COMMENT ON COLUMN RPR.CCPM.CCPMTDSA IS 'Tipo de sistema de amortización';
COMMENT ON COLUMN RPR.CCPM.CCPMCDCR IS 'Cuota del crédito';
COMMENT ON COLUMN RPR.CCPM.CCPMDVDN IS 'Dividendo';
COMMENT ON COLUMN RPR.CCPM.CCPMFDEC IS 'Fecha de exigibilidad de la cuota';
COMMENT ON COLUMN RPR.CCPM.CCPMVLDG IS 'Valor desgravamen (campo adicional vs G48)';
COMMENT ON COLUMN RPR.CCPM.CCPMVLIN IS 'Valor incendio (campo adicional vs G48)';
COMMENT ON COLUMN RPR.CCPM.CCPMEJCC IS 'FK a control de ejecución (RPR.EJCC)';
CREATE INDEX IDX_CCPM_EJCC ON RPR.CCPM(CCPMEJCC);
CREATE INDEX IDX_CCPM_NMOP ON RPR.CCPM(CCPMNMOP);
CREATE INDEX IDX_CCPM_IDSJ ON RPR.CCPM(CCPMIDSJ);

-- ================================================================
-- FIN DEL SCRIPT
-- ================================================================


-- ================================================================
-- PASO 1: GRANTS de CRD a RPR
-- Ejecutar con usuario DBA o con el usuario CRD
-- Necesarios para que RPR pueda:
--   - Crear FOREIGN KEYs hacia tablas de CRD (REFERENCES)
--   - Hacer SELECT en JPA sobre objetos de CRD (SELECT)
-- ================================================================
GRANT REFERENCES, SELECT ON CRD.TPAP TO RPR;
GRANT REFERENCES, SELECT ON CRD.ENTD TO RPR;

-- ================================================================
-- PASO 2: DROP previo para permitir recreación limpia
-- ================================================================

-- Eliminar en orden inverso de dependencias (hijas primero)
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE RPR.HMCP CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE RPR.HMJB CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE RPR.HMPR CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE RPR.CCPM CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE RPR.CJBM CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE RPR.CPRM CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE RPR.EJCC CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

-- ================================================================
-- TABLA DE CONTROL DE EJECUCIÓN DE REPORTES DE CARTERA
-- ================================================================
CREATE TABLE RPR.EJCC (
    EJCCCDGO NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    EJCCMESS NUMBER(2)      NOT NULL,              -- Mes de ejecución (1-12)
    EJCCANOO NUMBER(4)      NOT NULL,              -- Año de ejecución
    EJCCUSRO VARCHAR2(50)   NOT NULL,              -- Usuario que ejecutó
    EJCCFCGN DATE DEFAULT SYSDATE NOT NULL,        -- Fecha de generación
    EJCCOBSR VARCHAR2(500),                        -- Observaciones
    CONSTRAINT CHK_EJCC_MES CHECK (EJCCMESS BETWEEN 1 AND 12)
);
COMMENT ON TABLE  RPR.EJCC         IS 'Control de ejecución de reportes de cartera (CPRM, CJBM, CCPM)';
COMMENT ON COLUMN RPR.EJCC.EJCCCDGO IS 'Código único de ejecución (Identity)';
COMMENT ON COLUMN RPR.EJCC.EJCCMESS IS 'Mes de ejecución (1-12)';
COMMENT ON COLUMN RPR.EJCC.EJCCANOO IS 'Año de ejecución';
COMMENT ON COLUMN RPR.EJCC.EJCCUSRO IS 'Usuario que ejecutó los reportes';
COMMENT ON COLUMN RPR.EJCC.EJCCFCGN IS 'Fecha y hora de generación';
COMMENT ON COLUMN RPR.EJCC.EJCCOBSR IS 'Observaciones generales';
CREATE INDEX IDX_EJCC_MESANIO ON RPR.EJCC(EJCCMESS, EJCCANOO);

-- ================================================================
-- TABLA: CPRM - CRÉDITO PARTÍCIPES MENSUAL
-- Similar al G42, PERO con una fila por cada combinación
-- entidad + tipo de aporte (en lugar de columnas por tipo).
-- Campos: tipo de aporte (FK a CRD.TPAP) y total acumulado.
-- ================================================================
CREATE TABLE RPR.CPRM (
    CPRMCDGO NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    CPRMTIDP VARCHAR2(50),                         -- Tipo de identificación del partícipe
    CPRMIDPR VARCHAR2(50),                         -- Identificación del partícipe
    TPAPCDGO NUMBER         NOT NULL,              -- FK al tipo de aporte (CRD.TPAP)
    CPRMTTAL NUMBER(20,2)   DEFAULT 0,             -- Total acumulado de aportes para este tipo
    CPRMEJCC NUMBER         NOT NULL,              -- FK a control de ejecución (RPR.EJCC)
    ENTDCDGO NUMBER,                               -- FK a entidad (CRD.ENTD) para búsqueda rápida
    CONSTRAINT FK_CPRM_EJCC  FOREIGN KEY (CPRMEJCC) REFERENCES RPR.EJCC(EJCCCDGO),
    CONSTRAINT FK_CPRM_TPAP  FOREIGN KEY (TPAPCDGO) REFERENCES CRD.TPAP(TPAPCDGO)
);
COMMENT ON TABLE  RPR.CPRM          IS 'CPRM - Crédito Partícipes Mensual. Un registro por entidad+tipoAporte.';
COMMENT ON COLUMN RPR.CPRM.CPRMCDGO IS 'Código único del registro (Identity)';
COMMENT ON COLUMN RPR.CPRM.CPRMTIDP IS 'Tipo de identificación del partícipe';
COMMENT ON COLUMN RPR.CPRM.CPRMIDPR IS 'Identificación del partícipe';
COMMENT ON COLUMN RPR.CPRM.TPAPCDGO IS 'FK al tipo de aporte (CRD.TPAP)';
COMMENT ON COLUMN RPR.CPRM.CPRMTTAL IS 'Total acumulado de aportes para este tipo hasta la fecha de corte';
COMMENT ON COLUMN RPR.CPRM.CPRMEJCC IS 'FK a control de ejecución (RPR.EJCC)';
COMMENT ON COLUMN RPR.CPRM.ENTDCDGO IS 'FK a entidad (CRD.ENTD) para búsqueda rápida';
CREATE INDEX IDX_CPRM_EJCC ON RPR.CPRM(CPRMEJCC);
CREATE INDEX IDX_CPRM_ENTD ON RPR.CPRM(ENTDCDGO);
CREATE INDEX IDX_CPRM_IDPR ON RPR.CPRM(CPRMIDPR);
CREATE INDEX IDX_CPRM_TPAP ON RPR.CPRM(TPAPCDGO);

-- ================================================================
-- TABLA: CJBM - CRÉDITO JUBILADOS MENSUAL (similar a G44)
-- ================================================================
CREATE TABLE RPR.CJBM (
    CJBMCDGO NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    CJBMTIDJ VARCHAR2(50),                         -- Tipo de identificación
    CJBMIDJB VARCHAR2(50),                         -- Identificación del jubilado
    CJBMTPJB VARCHAR2(50),                         -- Tipo de jubilación
    CJBMFCJB DATE,                                 -- Fecha de jubilación
    CJBMIAJB NUMBER(10)     DEFAULT 0,             -- Imposiciones acumuladas
    CJBMVLPN NUMBER(20,2)   DEFAULT 0,             -- Valor de la pensión
    CJBMVNAR NUMBER(20,2)   DEFAULT 0,             -- Valor neto a recibir
    CJBMSCJB NUMBER(20,2)   DEFAULT 0,             -- Saldo de cuenta
    CJBMVCAP NUMBER(20,2)   DEFAULT 0,             -- Valores compensados
    CJBMJEIS VARCHAR2(50),                         -- Jubilación en IESS (S/N)
    CJBMEJCC NUMBER         NOT NULL,              -- FK a control de ejecución
    CONSTRAINT FK_CJBM_EJCC FOREIGN KEY (CJBMEJCC) REFERENCES RPR.EJCC(EJCCCDGO)
);
COMMENT ON TABLE  RPR.CJBM          IS 'CJBM - Crédito Jubilados Mensual (similar a G44)';
COMMENT ON COLUMN RPR.CJBM.CJBMCDGO IS 'Código único del registro (Identity)';
COMMENT ON COLUMN RPR.CJBM.CJBMTIDJ IS 'Tipo de identificación del jubilado';
COMMENT ON COLUMN RPR.CJBM.CJBMIDJB IS 'Identificación del jubilado';
COMMENT ON COLUMN RPR.CJBM.CJBMTPJB IS 'Tipo de jubilación';
COMMENT ON COLUMN RPR.CJBM.CJBMFCJB IS 'Fecha de jubilación';
COMMENT ON COLUMN RPR.CJBM.CJBMIAJB IS 'Imposiciones acumuladas por jubilación';
COMMENT ON COLUMN RPR.CJBM.CJBMVLPN IS 'Valor de la pensión';
COMMENT ON COLUMN RPR.CJBM.CJBMVNAR IS 'Valor neto a recibir';
COMMENT ON COLUMN RPR.CJBM.CJBMSCJB IS 'Saldo de cuenta del jubilado';
COMMENT ON COLUMN RPR.CJBM.CJBMVCAP IS 'Valores compensados al partícipe';
COMMENT ON COLUMN RPR.CJBM.CJBMJEIS IS 'Jubilación en IESS (S/N)';
COMMENT ON COLUMN RPR.CJBM.CJBMEJCC IS 'FK a control de ejecución (RPR.EJCC)';
CREATE INDEX IDX_CJBM_EJCC ON RPR.CJBM(CJBMEJCC);
CREATE INDEX IDX_CJBM_IDJB ON RPR.CJBM(CJBMIDJB);

-- ================================================================
-- TABLA: CCPM - CRÉDITO CUOTAS PRÉSTAMOS MENSUAL
-- Similar al G48 + valorDesgravamen + valorIncendio
-- ================================================================
CREATE TABLE RPR.CCPM (
    CCPMCDGO NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    CCPMTIDS VARCHAR2(50),                         -- Tipo de identificación del sujeto
    CCPMIDSJ VARCHAR2(50),                         -- Identificación del sujeto
    CCPMNMOP VARCHAR2(50),                         -- Número de operación
    CCPMTPCR VARCHAR2(50),                         -- Tipo de crédito
    CCPMDDMR NUMBER(10)     DEFAULT 0,             -- Días de morosidad
    CCPMCLPR VARCHAR2(50),                         -- Calificación propia
    CCPMTDIN NUMBER(20,6)   DEFAULT 0,             -- Tasa de interés
    CCPMVPVN NUMBER(20,2)   DEFAULT 0,             -- Valor por vencer
    CCPMVLVN NUMBER(20,2)   DEFAULT 0,             -- Valor vencido
    CCPMCSPR NUMBER(20,2)   DEFAULT 0,             -- Costos operativos
    CCPMINRD NUMBER(20,2)   DEFAULT 0,             -- Interés ordinario
    CCPMISMR NUMBER(20,2)   DEFAULT 0,             -- Interés sobre mora
    CCPMVEDJ NUMBER(20,2)   DEFAULT 0,             -- Valor en demanda judicial
    CCPMCRCS NUMBER(20,2)   DEFAULT 0,             -- Cartera castigada
    CCPMPRRO NUMBER(20,2)   DEFAULT 0,             -- Provisión requerida original
    CCPMPRCN NUMBER(20,2)   DEFAULT 0,             -- Provisión constituida
    CCPMVTCI NUMBER(20,2)   DEFAULT 0,             -- Valor total cuenta individual
    CCPMVSAP NUMBER(20,2)   DEFAULT 0,             -- Valor sujeto a provisión
    CCPMTDSA VARCHAR2(50),                         -- Tipo de sistema de amortización
    CCPMCDCR NUMBER(20,2)   DEFAULT 0,             -- Cuota del crédito
    CCPMDVDN NUMBER(20,2)   DEFAULT 0,             -- Dividendo
    CCPMFDEC DATE,                                 -- Fecha de exigibilidad de la cuota
    CCPMVLDG NUMBER(20,2)   DEFAULT 0,             -- Valor desgravamen (campo adicional)
    CCPMVLIN NUMBER(20,2)   DEFAULT 0,             -- Valor incendio (campo adicional)
    CCPMEJCC NUMBER         NOT NULL,              -- FK a control de ejecución
    CONSTRAINT FK_CCPM_EJCC FOREIGN KEY (CCPMEJCC) REFERENCES RPR.EJCC(EJCCCDGO)
);
COMMENT ON TABLE  RPR.CCPM          IS 'CCPM - Crédito Cuotas Préstamos Mensual (similar a G48 con campos adicionales)';
COMMENT ON COLUMN RPR.CCPM.CCPMCDGO IS 'Código único del registro (Identity)';
COMMENT ON COLUMN RPR.CCPM.CCPMTIDS IS 'Tipo de identificación del sujeto';
COMMENT ON COLUMN RPR.CCPM.CCPMIDSJ IS 'Identificación del sujeto';
COMMENT ON COLUMN RPR.CCPM.CCPMNMOP IS 'Número de operación';
COMMENT ON COLUMN RPR.CCPM.CCPMTPCR IS 'Tipo de crédito';
COMMENT ON COLUMN RPR.CCPM.CCPMDDMR IS 'Días de morosidad';
COMMENT ON COLUMN RPR.CCPM.CCPMCLPR IS 'Calificación propia';
COMMENT ON COLUMN RPR.CCPM.CCPMTDIN IS 'Tasa de interés';
COMMENT ON COLUMN RPR.CCPM.CCPMVPVN IS 'Valor por vencer';
COMMENT ON COLUMN RPR.CCPM.CCPMVLVN IS 'Valor vencido';
COMMENT ON COLUMN RPR.CCPM.CCPMCSPR IS 'Costos operativos';
COMMENT ON COLUMN RPR.CCPM.CCPMINRD IS 'Interés ordinario';
COMMENT ON COLUMN RPR.CCPM.CCPMISMR IS 'Interés sobre mora';
COMMENT ON COLUMN RPR.CCPM.CCPMVEDJ IS 'Valor en demanda judicial';
COMMENT ON COLUMN RPR.CCPM.CCPMCRCS IS 'Cartera castigada';
COMMENT ON COLUMN RPR.CCPM.CCPMPRRO IS 'Provisión requerida original';
COMMENT ON COLUMN RPR.CCPM.CCPMPRCN IS 'Provisión constituida';
COMMENT ON COLUMN RPR.CCPM.CCPMVTCI IS 'Valor total cuenta individual';
COMMENT ON COLUMN RPR.CCPM.CCPMVSAP IS 'Valor sujeto a provisión';
COMMENT ON COLUMN RPR.CCPM.CCPMTDSA IS 'Tipo de sistema de amortización';
COMMENT ON COLUMN RPR.CCPM.CCPMCDCR IS 'Cuota del crédito';
COMMENT ON COLUMN RPR.CCPM.CCPMDVDN IS 'Dividendo';
COMMENT ON COLUMN RPR.CCPM.CCPMFDEC IS 'Fecha de exigibilidad de la cuota';
COMMENT ON COLUMN RPR.CCPM.CCPMVLDG IS 'Valor desgravamen (campo adicional vs G48)';
COMMENT ON COLUMN RPR.CCPM.CCPMVLIN IS 'Valor incendio (campo adicional vs G48)';
COMMENT ON COLUMN RPR.CCPM.CCPMEJCC IS 'FK a control de ejecución (RPR.EJCC)';
CREATE INDEX IDX_CCPM_EJCC ON RPR.CCPM(CCPMEJCC);
CREATE INDEX IDX_CCPM_NMOP ON RPR.CCPM(CCPMNMOP);
CREATE INDEX IDX_CCPM_IDSJ ON RPR.CCPM(CCPMIDSJ);

-- ================================================================
-- TABLA: HMPR - HISTÓRICO CPRM
-- Un registro por cada combinación identificación + tipo de aporte.
-- ================================================================
CREATE TABLE RPR.HMPR (
    HMPRCDGO NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    HMPRIDPR VARCHAR2(50),                         -- Identificación del partícipe
    HMPRTIDP VARCHAR2(50),                         -- Tipo de identificación
    TPAPCDGO NUMBER,                               -- FK al tipo de aporte (CRD.TPAP)
    HMPRTTL  NUMBER(20,2)   DEFAULT 0,             -- Total acumulado de aportes
    CONSTRAINT FK_HMPR_TPAP FOREIGN KEY (TPAPCDGO) REFERENCES CRD.TPAP(TPAPCDGO)
);
COMMENT ON TABLE  RPR.HMPR          IS 'Histórico CPRM - un registro por identificación+tipoAporte';
COMMENT ON COLUMN RPR.HMPR.HMPRCDGO IS 'Código único (Identity)';
COMMENT ON COLUMN RPR.HMPR.HMPRIDPR IS 'Identificación del partícipe';
COMMENT ON COLUMN RPR.HMPR.HMPRTIDP IS 'Tipo de identificación';
COMMENT ON COLUMN RPR.HMPR.TPAPCDGO IS 'FK al tipo de aporte (CRD.TPAP)';
COMMENT ON COLUMN RPR.HMPR.HMPRTTL  IS 'Total acumulado de aportes para este tipo';
CREATE INDEX IDX_HMPR_IDPR ON RPR.HMPR(HMPRIDPR);
CREATE INDEX IDX_HMPR_TPAP ON RPR.HMPR(TPAPCDGO);

-- ================================================================
-- TABLA: HMJB - HISTÓRICO CJBM (similar a HM44)
-- ================================================================
CREATE TABLE RPR.HMJB (
    HMJBIDJB VARCHAR2(50) PRIMARY KEY,             -- Identificación del jubilado (PK)
    HMJBTIDJ VARCHAR2(50),                         -- Tipo de identificación
    HMJBTPJB VARCHAR2(50),                         -- Tipo de jubilación
    HMJBFCJB VARCHAR2(100),                        -- Fecha de jubilación (String)
    HMJBIAJB NUMBER(10)   DEFAULT 0,               -- Imposiciones acumuladas
    HMJBVLPN NUMBER(20,2) DEFAULT 0,               -- Valor de la pensión
    HMJBVNAR NUMBER(20,2) DEFAULT 0,               -- Valor neto a recibir
    HMJBSCJB NUMBER(20,2) DEFAULT 0,               -- Saldo de cuenta
    HMJBVCAP NUMBER(20,2) DEFAULT 0,               -- Valores compensados
    HMJBJEIS VARCHAR2(50)                          -- Jubilación en IESS
);
COMMENT ON TABLE RPR.HMJB IS 'Histórico de CJBM para consulta de períodos anteriores';

-- ================================================================
-- TABLA: HMCP - HISTÓRICO CCPM (similar a HM48)
-- ================================================================
CREATE TABLE RPR.HMCP (
    HMCPNMOP VARCHAR2(50) PRIMARY KEY,             -- Número de operación (PK)
    HMCPTIDS VARCHAR2(50),                         -- Tipo de identificación
    HMCPIDSJ VARCHAR2(50),                         -- Identificación del sujeto
    HMCPTPCR VARCHAR2(50),                         -- Tipo de crédito
    HMCPDDMR NUMBER(10)   DEFAULT 0,               -- Días de morosidad
    HMCPCLPR VARCHAR2(50),                         -- Calificación propia
    HMCPTDIN NUMBER(20,6) DEFAULT 0,               -- Tasa de interés
    HMCPVPVN NUMBER(20,2) DEFAULT 0,               -- Valor por vencer
    HMCPVLVN NUMBER(20,2) DEFAULT 0,               -- Valor vencido
    HMCPCSPR NUMBER(20,2) DEFAULT 0,               -- Costos operativos
    HMCPINRD NUMBER(20,2) DEFAULT 0,               -- Interés ordinario
    HMCPISMR NUMBER(20,2) DEFAULT 0,               -- Interés sobre mora
    HMCPVEDJ NUMBER(20,2) DEFAULT 0,               -- Valor en demanda judicial
    HMCPCRCS NUMBER(20,2) DEFAULT 0,               -- Cartera castigada
    HMCPPRRO NUMBER(20,2) DEFAULT 0,               -- Provisión requerida original
    HMCPPRCN NUMBER(20,2) DEFAULT 0,               -- Provisión constituida
    HMCPVTCI NUMBER(20,2) DEFAULT 0,               -- Valor total cuenta individual
    HMCPVSAP NUMBER(20,2) DEFAULT 0,               -- Valor sujeto a provisión
    HMCPTDSA VARCHAR2(50),                         -- Tipo de sistema de amortización
    HMCPCDCR NUMBER(20,2) DEFAULT 0,               -- Cuota del crédito
    HMCPDVDN NUMBER(20,2) DEFAULT 0,               -- Dividendo
    HMCPFDEC VARCHAR2(100),                        -- Fecha de exigibilidad (String)
    HMCPVLDG NUMBER(20,2) DEFAULT 0,               -- Valor desgravamen
    HMCPVLIN NUMBER(20,2) DEFAULT 0                -- Valor incendio
);
COMMENT ON TABLE RPR.HMCP IS 'Histórico de CCPM para consulta de períodos anteriores';

-- ================================================================
-- FIN DE SCRIPTS DE CREACIÓN
-- ================================================================
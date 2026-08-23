-- =============================================================================
-- SCRIPT: Tabla de anticipos de clientes
-- Esquema: CBR  |  Tabla: ANTC
-- Fecha: 2026-07-14
-- =============================================================================

-- Secuencia
CREATE SEQUENCE CBR.SQ_ANTCCDGO
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Tabla principal
CREATE TABLE CBR.ANTC (
    ID              NUMBER(38,0)    NOT NULL,   -- PK
    TITULAR         NUMBER(38,0)    NOT NULL,   -- FK TSR.TTLR (cliente)
    FECHAANTICIPO   DATE            NOT NULL,   -- Fecha del anticipo (documento)
    FECHARECEPCION  DATE            NULL,       -- Fecha en que se recibió el anticipo
    USUARIO         NUMBER(38,0)    NULL,       -- FK SCP.PJRQ (usuario que registra)
    FECHAREGISTRO   TIMESTAMP       NULL,       -- Fecha/hora de registro en el sistema
    NUMERODOC       VARCHAR2(100)   NULL,       -- Número de documento de referencia
    VALOR           NUMBER(18,2)    NOT NULL,   -- Valor del anticipo
    ASIENTO         NUMBER(38,0)    NULL,       -- FK CNT.ASNT (asiento contable generado)
    ESTADO          NUMBER(38,0)    NOT NULL,   -- 1=Activo, 2=Anulado
    EMPRESA         NUMBER(38,0)    NULL,       -- FK SCP.PJRQ (empresa contable)
    OBSERVACION     VARCHAR2(2000)  NULL,       -- Observaciones adicionales
    CONSTRAINT PK_ANTC PRIMARY KEY (ID)
);

-- Comentarios
COMMENT ON TABLE  CBR.ANTC                IS 'Anticipos recibidos de clientes';
COMMENT ON COLUMN CBR.ANTC.ID             IS 'Identificador único del anticipo';
COMMENT ON COLUMN CBR.ANTC.TITULAR        IS 'FK a TSR.TTLR: cliente que entrega el anticipo';
COMMENT ON COLUMN CBR.ANTC.FECHAANTICIPO  IS 'Fecha del documento de anticipo';
COMMENT ON COLUMN CBR.ANTC.FECHARECEPCION IS 'Fecha en que se recibió físicamente el anticipo';
COMMENT ON COLUMN CBR.ANTC.USUARIO        IS 'FK a SCP.PJRQ: usuario que registra el anticipo';
COMMENT ON COLUMN CBR.ANTC.FECHAREGISTRO  IS 'Fecha y hora en que se registró en el sistema';
COMMENT ON COLUMN CBR.ANTC.NUMERODOC      IS 'Número de documento de referencia del anticipo';
COMMENT ON COLUMN CBR.ANTC.VALOR          IS 'Valor monetario del anticipo';
COMMENT ON COLUMN CBR.ANTC.ASIENTO        IS 'FK a CNT.ASNT: asiento contable generado';
COMMENT ON COLUMN CBR.ANTC.ESTADO         IS '1=Activo, 2=Anulado';
COMMENT ON COLUMN CBR.ANTC.EMPRESA        IS 'FK a SCP.PJRQ: empresa contable';
COMMENT ON COLUMN CBR.ANTC.OBSERVACION    IS 'Observaciones adicionales del anticipo';

-- Claves foráneas
ALTER TABLE CBR.ANTC ADD CONSTRAINT FK_ANTC_TITULAR  FOREIGN KEY (TITULAR)  REFERENCES TSR.TTLR(TTLRCDGO);
ALTER TABLE CBR.ANTC ADD CONSTRAINT FK_ANTC_USUARIO  FOREIGN KEY (USUARIO)  REFERENCES SCP.PJRQ(PJRQCDGO);
ALTER TABLE CBR.ANTC ADD CONSTRAINT FK_ANTC_ASIENTO  FOREIGN KEY (ASIENTO)  REFERENCES CNT.ASNT(ASNTCDGO);
ALTER TABLE CBR.ANTC ADD CONSTRAINT FK_ANTC_EMPRESA  FOREIGN KEY (EMPRESA)  REFERENCES SCP.PJRQ(PJRQCDGO);

-- Índices de búsqueda frecuente
CREATE INDEX IDX_ANTC_TITULAR   ON CBR.ANTC(TITULAR);
CREATE INDEX IDX_ANTC_FECHA     ON CBR.ANTC(FECHAANTICIPO);
CREATE INDEX IDX_ANTC_EMPRESA   ON CBR.ANTC(EMPRESA);
CREATE INDEX IDX_ANTC_ESTADO    ON CBR.ANTC(ESTADO);

COMMIT;

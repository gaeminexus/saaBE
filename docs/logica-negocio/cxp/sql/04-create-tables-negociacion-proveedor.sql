-- =============================================================================
-- NEGOCIACIONES CON PROVEEDORES - CXP
-- Módulo: Cuentas por Pagar (PGS)
-- Fecha: 2026-07-20
--
-- Orden de ejecución: 4 de 5
-- Anterior: 03-rename-columnas-crtx-dcxp-dctx.sql
-- Siguiente: 05-insert-rubros-proceso-carga.sql
--
-- Tablas creadas:
--   NGCP - Negociación con Proveedor (cabecera)
--   FPNG - Forma de Pago de Negociación (cuotas/hitos acordados)
--   PGNG - Pagos realizados sobre cuotas de negociación
--   ADNG - Adendums de Negociación
--   PTNG - Paths / Documentos digitalizados de negociación
-- Vistas creadas:
--   V_ESTADO_NEGOCIACION
--   V_ESTADO_CUOTAS_NEGOCIACION
-- =============================================================================

-- =============================================================================
-- TABLA: NGCP - Negociación con Proveedor (cabecera)
-- =============================================================================
CREATE SEQUENCE PGS.SQ_NGCPCDGO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE PGS.NGCP (
    ID                NUMBER(19)      NOT NULL,
    EMPRESA           NUMBER(19)      NOT NULL,           -- FK -> SCP.PJRQ (empresa)
    TITULAR           NUMBER(19)      NOT NULL,           -- FK -> TSR.TTLR (proveedor)
    FECHANEGOCIACION  DATE            NOT NULL,
    FECHAINICIO       DATE,
    FECHAFIN          DATE,
    NUMCONTRATO       VARCHAR2(200),
    DESCRIPCION       VARCHAR2(2000)  NOT NULL,
    VALORTOTAL        NUMBER(18,2)    NOT NULL,
    TIPOFINANCIACION  VARCHAR2(50),                       -- FIJO | HITO | PORCENTAJE | UNICO
    NUMEROPAGOS       NUMBER(5),
    OBSERVACION       VARCHAR2(2000),
    ESTADO            NUMBER(1)       DEFAULT 1 NOT NULL, -- 1=Activa, 0=Inactiva, 2=Suspendida
    USUARIO           NUMBER(19)      NOT NULL,
    FECHAREGISTRO     TIMESTAMP       NOT NULL,
    USUARIOMODIF      NUMBER(19),
    FECHAMODIF        TIMESTAMP,
    CONSTRAINT PK_NGCP PRIMARY KEY (ID),
    CONSTRAINT FK_NGCP_EMPRESA  FOREIGN KEY (EMPRESA)      REFERENCES SCP.PJRQ(PJRQCDGO),
    CONSTRAINT FK_NGCP_TITULAR  FOREIGN KEY (TITULAR)      REFERENCES TSR.TTLR(TTLRCDGO),
    CONSTRAINT FK_NGCP_USUARIO  FOREIGN KEY (USUARIO)      REFERENCES SCP.PJRQ(PJRQCDGO),
    CONSTRAINT FK_NGCP_USRMOD   FOREIGN KEY (USUARIOMODIF) REFERENCES SCP.PJRQ(PJRQCDGO),
    CONSTRAINT CK_NGCP_TIPO CHECK (TIPOFINANCIACION IN ('FIJO','HITO','PORCENTAJE','UNICO'))
);

COMMENT ON TABLE  PGS.NGCP IS 'Negociaciones con proveedores - Cabecera del acuerdo comercial';
COMMENT ON COLUMN PGS.NGCP.ID               IS 'Identificador único de la negociación';
COMMENT ON COLUMN PGS.NGCP.EMPRESA          IS 'Empresa a la que pertenece la negociación';
COMMENT ON COLUMN PGS.NGCP.TITULAR          IS 'Proveedor titular de la negociación (debe tener rol proveedor)';
COMMENT ON COLUMN PGS.NGCP.FECHANEGOCIACION IS 'Fecha en que se realizó/firmó la negociación';
COMMENT ON COLUMN PGS.NGCP.FECHAINICIO      IS 'Fecha de inicio de vigencia del acuerdo';
COMMENT ON COLUMN PGS.NGCP.FECHAFIN         IS 'Fecha estimada de finalización del acuerdo';
COMMENT ON COLUMN PGS.NGCP.NUMCONTRATO      IS 'Número o referencia del contrato físico asociado';
COMMENT ON COLUMN PGS.NGCP.DESCRIPCION      IS 'Descripción del objeto o negocio de la negociación';
COMMENT ON COLUMN PGS.NGCP.VALORTOTAL       IS 'Valor total original pactado en la negociación';
COMMENT ON COLUMN PGS.NGCP.TIPOFINANCIACION IS 'Tipo de plan de pagos: FIJO, HITO, PORCENTAJE, UNICO';
COMMENT ON COLUMN PGS.NGCP.NUMEROPAGOS      IS 'Número total de cuotas o hitos acordados';
COMMENT ON COLUMN PGS.NGCP.ESTADO           IS '1=Activa, 0=Inactiva/Cerrada, 2=Suspendida';
COMMENT ON COLUMN PGS.NGCP.USUARIO          IS 'Usuario que registró la negociación';
COMMENT ON COLUMN PGS.NGCP.FECHAREGISTRO    IS 'Fecha y hora en que se registró en el sistema';


-- =============================================================================
-- TABLA: FPNG - Forma de Pago de Negociación (cuotas/hitos acordados)
-- =============================================================================
CREATE SEQUENCE PGS.SQ_FPNGCDGO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE PGS.FPNG (
    ID            NUMBER(19)      NOT NULL,
    NEGOCIACION   NUMBER(19)      NOT NULL,           -- FK -> PGS.NGCP
    NUMEROCUOTA   NUMBER(5)       NOT NULL,
    DESCRIPCION   VARCHAR2(1000),
    FECHAPAGO     DATE,
    PORCENTAJE    NUMBER(6,2),
    VALORCUOTA    NUMBER(18,2)    NOT NULL,
    ESTADO        NUMBER(1)       DEFAULT 1 NOT NULL, -- 1=Pendiente, 2=Pago parcial, 3=Pagado, 0=Anulado
    ORDEN         NUMBER(5)       DEFAULT 1,
    CONSTRAINT PK_FPNG PRIMARY KEY (ID),
    CONSTRAINT FK_FPNG_NEGOCIACION FOREIGN KEY (NEGOCIACION) REFERENCES PGS.NGCP(ID)
);

COMMENT ON TABLE  PGS.FPNG IS 'Forma de pago de negociación - Detalle de cuotas/hitos acordados';
COMMENT ON COLUMN PGS.FPNG.ID          IS 'Identificador único de la cuota';
COMMENT ON COLUMN PGS.FPNG.NEGOCIACION IS 'Negociación a la que pertenece esta cuota';
COMMENT ON COLUMN PGS.FPNG.NUMEROCUOTA IS 'Número secuencial de la cuota dentro de la negociación';
COMMENT ON COLUMN PGS.FPNG.DESCRIPCION IS 'Descripción del hito o cuota (ej: Anticipo, Entrega parcial)';
COMMENT ON COLUMN PGS.FPNG.FECHAPAGO   IS 'Fecha acordada para el pago de esta cuota';
COMMENT ON COLUMN PGS.FPNG.PORCENTAJE  IS 'Porcentaje del total que representa la cuota (para tipo PORCENTAJE)';
COMMENT ON COLUMN PGS.FPNG.VALORCUOTA  IS 'Valor monetario pactado para esta cuota';
COMMENT ON COLUMN PGS.FPNG.ESTADO      IS '1=Pendiente, 2=Pago parcial, 3=Pagado total, 0=Anulado';


-- =============================================================================
-- TABLA: PGNG - Pagos realizados sobre cuotas de negociación
-- =============================================================================
CREATE SEQUENCE PGS.SQ_PGNGCDGO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE PGS.PGNG (
    ID              NUMBER(19)      NOT NULL,
    FORMAPAGO       NUMBER(19)      NOT NULL,           -- FK -> PGS.FPNG
    FECHAPAGO       DATE            NOT NULL,
    VALORPAGO       NUMBER(18,2)    NOT NULL,
    DESCRIPCION     VARCHAR2(1000),
    TIPOPAGO        VARCHAR2(50)    NOT NULL,           -- ANTICIPO | FACTURA
    FACTURACOMPRA   NUMBER(19),                         -- FK -> PGS.FCTC (opcional)
    FACTURADO       NUMBER(1)       DEFAULT 0 NOT NULL,
    PAGADO          NUMBER(1)       DEFAULT 0 NOT NULL,
    REFCOMPROBANTE  VARCHAR2(200),
    ESTADO          NUMBER(1)       DEFAULT 1 NOT NULL,
    USUARIO         NUMBER(19)      NOT NULL,
    FECHAREGISTRO   TIMESTAMP       NOT NULL,
    CONSTRAINT PK_PGNG PRIMARY KEY (ID),
    CONSTRAINT FK_PGNG_FORMAPAGO     FOREIGN KEY (FORMAPAGO)     REFERENCES PGS.FPNG(ID),
    CONSTRAINT FK_PGNG_FACTURACOMPRA FOREIGN KEY (FACTURACOMPRA) REFERENCES PGS.FCTC(ID),
    CONSTRAINT FK_PGNG_USUARIO       FOREIGN KEY (USUARIO)       REFERENCES SCP.PJRQ(PJRQCDGO),
    CONSTRAINT CK_PGNG_TIPO CHECK (TIPOPAGO IN ('ANTICIPO','FACTURA'))
);

COMMENT ON TABLE  PGS.PGNG IS 'Pagos realizados sobre cuotas de negociación con proveedores';
COMMENT ON COLUMN PGS.PGNG.ID             IS 'Identificador único del pago';
COMMENT ON COLUMN PGS.PGNG.FORMAPAGO      IS 'Cuota de la negociación a la que se aplica este pago';
COMMENT ON COLUMN PGS.PGNG.FECHAPAGO      IS 'Fecha en que se realizó o entregó el pago';
COMMENT ON COLUMN PGS.PGNG.VALORPAGO      IS 'Valor del pago realizado';
COMMENT ON COLUMN PGS.PGNG.TIPOPAGO       IS 'ANTICIPO=sin factura aún emitida, FACTURA=con factura del proveedor';
COMMENT ON COLUMN PGS.PGNG.FACTURACOMPRA  IS 'Factura de compra del proveedor relacionada a este pago';
COMMENT ON COLUMN PGS.PGNG.FACTURADO      IS '1=El valor ya fue facturado por el proveedor, 0=Anticipo sin factura';
COMMENT ON COLUMN PGS.PGNG.PAGADO         IS '1=Pago liquidado/cancelado, 0=Pendiente de liquidar';
COMMENT ON COLUMN PGS.PGNG.REFCOMPROBANTE IS 'Número de transferencia, cheque u otro comprobante de pago';
COMMENT ON COLUMN PGS.PGNG.ESTADO         IS '1=Activo, 0=Anulado';


-- =============================================================================
-- TABLA: ADNG - Adendums de Negociación
-- =============================================================================
CREATE SEQUENCE PGS.SQ_ADNGCDGO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE PGS.ADNG (
    ID                    NUMBER(19)      NOT NULL,
    NEGOCIACION           NUMBER(19)      NOT NULL,           -- FK -> PGS.NGCP
    NUMADENDUM            VARCHAR2(200),
    FECHAADENDUM          DATE            NOT NULL,
    DESCRIPCION           VARCHAR2(2000)  NOT NULL,
    VALORAJUSTE           NUMBER(18,2)    NOT NULL,           -- positivo=incremento, negativo=reducción
    VALORTOTALRESULTANTE  NUMBER(18,2)    NOT NULL,
    OBSERVACION           VARCHAR2(2000),
    ESTADO                NUMBER(1)       DEFAULT 1 NOT NULL,
    USUARIO               NUMBER(19)      NOT NULL,
    FECHAREGISTRO         TIMESTAMP       NOT NULL,
    CONSTRAINT PK_ADNG PRIMARY KEY (ID),
    CONSTRAINT FK_ADNG_NEGOCIACION FOREIGN KEY (NEGOCIACION) REFERENCES PGS.NGCP(ID),
    CONSTRAINT FK_ADNG_USUARIO     FOREIGN KEY (USUARIO)     REFERENCES SCP.PJRQ(PJRQCDGO)
);

COMMENT ON TABLE  PGS.ADNG IS 'Adendums de negociación - Modificaciones acordadas al valor original';
COMMENT ON COLUMN PGS.ADNG.ID                   IS 'Identificador único del adendum';
COMMENT ON COLUMN PGS.ADNG.NEGOCIACION          IS 'Negociación sobre la que aplica el adendum';
COMMENT ON COLUMN PGS.ADNG.NUMADENDUM           IS 'Número o referencia del documento de adendum';
COMMENT ON COLUMN PGS.ADNG.FECHAADENDUM         IS 'Fecha en que se firmó o acordó el adendum';
COMMENT ON COLUMN PGS.ADNG.DESCRIPCION          IS 'Motivo o razón del adendum (ej: trabajos adicionales, descuento)';
COMMENT ON COLUMN PGS.ADNG.VALORAJUSTE          IS 'Valor del ajuste: positivo=incremento, negativo=reducción';
COMMENT ON COLUMN PGS.ADNG.VALORTOTALRESULTANTE IS 'Valor total de la negociación luego de aplicar el adendum';
COMMENT ON COLUMN PGS.ADNG.ESTADO               IS '1=Activo, 0=Anulado';


-- =============================================================================
-- TABLA: PTNG - Paths / Documentos digitalizados de negociación
-- =============================================================================
CREATE SEQUENCE PGS.SQ_PTNGCDGO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE PGS.PTNG (
    ID          NUMBER(19)      NOT NULL,
    NEGOCIACION NUMBER(19)      NOT NULL,           -- FK -> PGS.NGCP
    PATH        VARCHAR2(1000)  NOT NULL,
    NOMBREDOC   VARCHAR2(500),
    TIPODOC     VARCHAR2(50),                       -- CONTRATO | ADENDUM | ANEXO | OTRO
    PRINCIPAL   NUMBER(1)       DEFAULT 0,
    ADENDUM     NUMBER(19),                         -- FK -> PGS.ADNG (opcional)
    CONSTRAINT PK_PTNG PRIMARY KEY (ID),
    CONSTRAINT FK_PTNG_NEGOCIACION FOREIGN KEY (NEGOCIACION) REFERENCES PGS.NGCP(ID),
    CONSTRAINT FK_PTNG_ADENDUM     FOREIGN KEY (ADENDUM)     REFERENCES PGS.ADNG(ID),
    CONSTRAINT CK_PTNG_TIPO CHECK (TIPODOC IN ('CONTRATO','ADENDUM','ANEXO','OTRO'))
);

COMMENT ON TABLE  PGS.PTNG IS 'Documentos digitalizados asociados a negociaciones con proveedores';
COMMENT ON COLUMN PGS.PTNG.ID          IS 'Identificador único del documento';
COMMENT ON COLUMN PGS.PTNG.NEGOCIACION IS 'Negociación a la que pertenece el documento';
COMMENT ON COLUMN PGS.PTNG.PATH        IS 'Ruta o URL del archivo digitalizado';
COMMENT ON COLUMN PGS.PTNG.NOMBREDOC   IS 'Nombre descriptivo del documento (ej: Contrato principal, Adendum 1)';
COMMENT ON COLUMN PGS.PTNG.TIPODOC     IS 'Tipo: CONTRATO, ADENDUM, ANEXO, OTRO';
COMMENT ON COLUMN PGS.PTNG.PRINCIPAL   IS '1=Documento principal de la negociación, 0=Complementario';
COMMENT ON COLUMN PGS.PTNG.ADENDUM     IS 'Adendum específico al que corresponde este documento (opcional)';


-- =============================================================================
-- VISTA: V_ESTADO_NEGOCIACION
-- Estado de cuenta de cada negociación: valor vigente, pagado, saldo.
-- =============================================================================
CREATE OR REPLACE VIEW PGS.V_ESTADO_NEGOCIACION AS
SELECT
    ng.ID                                               AS ID_NEGOCIACION,
    ng.EMPRESA,
    ng.TITULAR,
    ng.NUMCONTRATO,
    ng.DESCRIPCION                                      AS DESC_NEGOCIACION,
    ng.VALORTOTAL                                       AS VALOR_ORIGINAL,
    ng.TIPOFINANCIACION,
    ng.FECHANEGOCIACION,
    ng.FECHAINICIO,
    ng.FECHAFIN,
    ng.ESTADO                                           AS ESTADO_NEGOCIACION,
    NVL((SELECT SUM(ad.VALORAJUSTE) FROM PGS.ADNG ad
         WHERE ad.NEGOCIACION = ng.ID AND ad.ESTADO = 1), 0)         AS TOTAL_ADENDUMS,
    ng.VALORTOTAL +
    NVL((SELECT SUM(ad.VALORAJUSTE) FROM PGS.ADNG ad
         WHERE ad.NEGOCIACION = ng.ID AND ad.ESTADO = 1), 0)         AS VALOR_TOTAL_VIGENTE,
    NVL((SELECT SUM(pg.VALORPAGO) FROM PGS.PGNG pg
         JOIN PGS.FPNG fp ON pg.FORMAPAGO = fp.ID
         WHERE fp.NEGOCIACION = ng.ID AND pg.ESTADO = 1), 0)         AS TOTAL_PAGADO,
    NVL((SELECT SUM(pg.VALORPAGO) FROM PGS.PGNG pg
         JOIN PGS.FPNG fp ON pg.FORMAPAGO = fp.ID
         WHERE fp.NEGOCIACION = ng.ID AND pg.ESTADO = 1
           AND pg.FACTURADO = 1), 0)                                  AS TOTAL_FACTURADO,
    NVL((SELECT SUM(pg.VALORPAGO) FROM PGS.PGNG pg
         JOIN PGS.FPNG fp ON pg.FORMAPAGO = fp.ID
         WHERE fp.NEGOCIACION = ng.ID AND pg.ESTADO = 1
           AND pg.FACTURADO = 0), 0)                                  AS TOTAL_ANTICIPO_SIN_FACTURA,
    NVL((SELECT SUM(pg.VALORPAGO) FROM PGS.PGNG pg
         JOIN PGS.FPNG fp ON pg.FORMAPAGO = fp.ID
         WHERE fp.NEGOCIACION = ng.ID AND pg.ESTADO = 1
           AND pg.PAGADO = 1), 0)                                     AS TOTAL_LIQUIDADO,
    (ng.VALORTOTAL +
     NVL((SELECT SUM(ad.VALORAJUSTE) FROM PGS.ADNG ad
          WHERE ad.NEGOCIACION = ng.ID AND ad.ESTADO = 1), 0)) -
    NVL((SELECT SUM(pg.VALORPAGO) FROM PGS.PGNG pg
         JOIN PGS.FPNG fp ON pg.FORMAPAGO = fp.ID
         WHERE fp.NEGOCIACION = ng.ID AND pg.ESTADO = 1), 0)         AS SALDO_PENDIENTE
FROM PGS.NGCP ng;

COMMENT ON TABLE PGS.V_ESTADO_NEGOCIACION IS 'Vista de estado de cuenta de negociaciones con proveedores';


-- =============================================================================
-- VISTA: V_ESTADO_CUOTAS_NEGOCIACION
-- Detalle por cuota: valor pactado, pagado, saldo.
-- =============================================================================
CREATE OR REPLACE VIEW PGS.V_ESTADO_CUOTAS_NEGOCIACION AS
SELECT
    fp.ID                                                    AS ID_CUOTA,
    fp.NEGOCIACION                                           AS ID_NEGOCIACION,
    ng.TITULAR,
    ng.NUMCONTRATO,
    fp.NUMEROCUOTA,
    fp.DESCRIPCION                                           AS DESC_CUOTA,
    fp.FECHAPAGO                                             AS FECHA_PAGO_ACORDADA,
    fp.PORCENTAJE,
    fp.VALORCUOTA                                            AS VALOR_CUOTA_PACTADO,
    fp.ESTADO                                                AS ESTADO_CUOTA,
    NVL((SELECT SUM(pg.VALORPAGO) FROM PGS.PGNG pg
         WHERE pg.FORMAPAGO = fp.ID AND pg.ESTADO = 1), 0)  AS TOTAL_PAGADO_CUOTA,
    NVL((SELECT SUM(pg.VALORPAGO) FROM PGS.PGNG pg
         WHERE pg.FORMAPAGO = fp.ID AND pg.ESTADO = 1
           AND pg.FACTURADO = 1), 0)                         AS TOTAL_FACTURADO_CUOTA,
    NVL((SELECT SUM(pg.VALORPAGO) FROM PGS.PGNG pg
         WHERE pg.FORMAPAGO = fp.ID AND pg.ESTADO = 1
           AND pg.FACTURADO = 0), 0)                         AS TOTAL_ANTICIPO_CUOTA,
    NVL((SELECT SUM(pg.VALORPAGO) FROM PGS.PGNG pg
         WHERE pg.FORMAPAGO = fp.ID AND pg.ESTADO = 1
           AND pg.PAGADO = 1), 0)                            AS TOTAL_LIQUIDADO_CUOTA,
    fp.VALORCUOTA -
    NVL((SELECT SUM(pg.VALORPAGO) FROM PGS.PGNG pg
         WHERE pg.FORMAPAGO = fp.ID AND pg.ESTADO = 1), 0)  AS SALDO_CUOTA
FROM PGS.FPNG fp
JOIN PGS.NGCP ng ON fp.NEGOCIACION = ng.ID;

COMMENT ON TABLE PGS.V_ESTADO_CUOTAS_NEGOCIACION IS 'Vista detallada por cuota del estado de negociaciones con proveedores';


-- =============================================================================
-- ÍNDICES
-- =============================================================================
CREATE INDEX PGS.IDX_NGCP_TITULAR   ON PGS.NGCP(TITULAR);
CREATE INDEX PGS.IDX_NGCP_EMPRESA   ON PGS.NGCP(EMPRESA);
CREATE INDEX PGS.IDX_NGCP_ESTADO    ON PGS.NGCP(ESTADO);
CREATE INDEX PGS.IDX_FPNG_NEGOC     ON PGS.FPNG(NEGOCIACION);
CREATE INDEX PGS.IDX_PGNG_FORMAPAGO ON PGS.PGNG(FORMAPAGO);
CREATE INDEX PGS.IDX_PGNG_FACTURA   ON PGS.PGNG(FACTURACOMPRA);
CREATE INDEX PGS.IDX_PGNG_FECHAPAGO ON PGS.PGNG(FECHAPAGO);
CREATE INDEX PGS.IDX_ADNG_NEGOC     ON PGS.ADNG(NEGOCIACION);
CREATE INDEX PGS.IDX_PTNG_NEGOC     ON PGS.PTNG(NEGOCIACION);
CREATE INDEX PGS.IDX_PTNG_ADENDUM   ON PGS.PTNG(ADENDUM);

COMMIT;

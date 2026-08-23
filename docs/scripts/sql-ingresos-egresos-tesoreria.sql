-- ============================================================
-- Migración: Ingresos y Egresos de tesorería sin documento físico
-- Módulo:    TSR - Tesorería (+ ALTER en PGS.PGTR)
-- Schemas:   TSR, PGS
-- Fecha:     2026-08-12
--
-- Propósito: Registrar pagos (egresos) y cobros (ingresos) que no
--            tienen respaldo en un documento físico: comisiones y
--            débitos por administración de cuentas bancarias,
--            intereses ganados, etc.
--
--            La contrapartida contable NO se configura por registro:
--            cada ingreso/egreso apunta a un producto de CXC/CXP y la
--            cuenta contable sale del grupo del producto
--            (GrupoProductoCobro/GrupoProductoPago.planCuenta).
--
--            EGRESOS: sí pasan por el circuito de pagos (listado de
--            pagos a realizar, lote, archivo al banco, respuesta) —
--            se crea un PGS.PGTR con FK al egreso en vez de a una
--            factura. El débito automático (PGTRDBAT=1) también aplica:
--            nace confirmado y contabiliza al registrarse.
--
--            INGRESOS: se registran ya recibidos, en un solo paso
--            (asiento + movimiento bancario inmediatos).
-- ============================================================


-- ============================================================
-- 0. PRERREQUISITO — Grants entre schemas (ejecutar como DBA)
-- ============================================================
-- Las FKs cruzan schemas: el dueño de la tabla necesita el privilegio
-- REFERENCES sobre la tabla referenciada (ORA-00942 si falta).

GRANT REFERENCES ON PGS.PRDP TO TSR;     -- TSR.EGRS → producto CXP
GRANT REFERENCES ON CBR.PRDC TO TSR;     -- TSR.INGR → producto CXC

-- Solo si fallan las demás FKs (normalmente ya existen, otras tablas TSR
-- referencian estos schemas):
-- GRANT REFERENCES ON SCP.PJRQ TO TSR;
-- GRANT REFERENCES ON CNT.ASNT TO TSR;

-- Después de crear TSR.EGRS, para el ALTER de PGS.PGTR (bloque 3):
-- GRANT REFERENCES ON TSR.EGRS TO PGS;

-- Si el usuario del datasource de WildFly no es TSR, además:
-- GRANT SELECT, INSERT, UPDATE, DELETE ON TSR.EGRS TO <usuario_app>;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON TSR.INGR TO <usuario_app>;
-- GRANT SELECT ON TSR.SQ_EGRSCDGO TO <usuario_app>;
-- GRANT SELECT ON TSR.SQ_INGRCDGO TO <usuario_app>;


-- ============================================================
-- 1. TSR.EGRS — Egreso de tesorería
-- ============================================================

CREATE TABLE TSR.EGRS (
    EGRSCDGO NUMBER(19)     NOT NULL,           -- PK
    EGRSPJRQ NUMBER(19)     NOT NULL,           -- FK SCP.PJRQ empresa
    EGRSTTLR NUMBER(19)         NULL,           -- FK TSR.TTLR beneficiario (obligatorio si va por transferencia)
    EGRSPRDP NUMBER(19)     NOT NULL,           -- FK PGS.PRDP producto CXP (define la cuenta contable vía su grupo)
    EGRSDSCR VARCHAR2(500)  NOT NULL,           -- Concepto del egreso
    EGRSDBAT NUMBER(1)      DEFAULT 0    NULL,  -- 0=Transferencia, 1=Débito automático (ver sql-alter-egrs-debito-automatico.sql)
    EGRSVLOR NUMBER(15,2)   NOT NULL,           -- Valor
    EGRSFCHA DATE           NOT NULL,           -- Fecha del egreso
    EGRSESTD NUMBER(2)      DEFAULT 1 NOT NULL, -- 1=Pendiente de pago, 2=Pagado, 3=Anulado
    EGRSASNT NUMBER(19)         NULL,           -- FK CNT.ASNT asiento generado al pagar
    EGRSOBSR VARCHAR2(2000)     NULL,           -- Observaciones
    EGRSUSAR NUMBER(19)         NULL,           -- FK SCP.PJRQ usuario que registra
    EGRSFCRG TIMESTAMP          NULL,           -- Fecha/hora de registro
    CONSTRAINT PK_EGRS PRIMARY KEY (EGRSCDGO),
    CONSTRAINT FK_EGRS_EMPRESA  FOREIGN KEY (EGRSPJRQ) REFERENCES SCP.PJRQ (PJRQCDGO),
    CONSTRAINT FK_EGRS_TITULAR  FOREIGN KEY (EGRSTTLR) REFERENCES TSR.TTLR (TTLRCDGO),
    CONSTRAINT FK_EGRS_PRODUCTO FOREIGN KEY (EGRSPRDP) REFERENCES PGS.PRDP (ID),
    CONSTRAINT FK_EGRS_ASIENTO  FOREIGN KEY (EGRSASNT) REFERENCES CNT.ASNT (ASNTCDGO),
    CONSTRAINT FK_EGRS_USUARIO  FOREIGN KEY (EGRSUSAR) REFERENCES SCP.PJRQ (PJRQCDGO)
);

CREATE SEQUENCE TSR.SQ_EGRSCDGO START WITH 1 INCREMENT BY 1 NOCACHE;

COMMENT ON TABLE  TSR.EGRS IS 'Egresos de tesorería sin documento físico (comisiones, administración de cuenta, etc.). Se pagan a través del circuito de PGS.PGTR.';
COMMENT ON COLUMN TSR.EGRS.EGRSPRDP IS 'FK a PGS.PRDP. La cuenta contable del gasto sale del grupo del producto (PGS.GRPP.PLNNCDGO).';
COMMENT ON COLUMN TSR.EGRS.EGRSESTD IS '1=Pendiente de pago, 2=Pagado (con asiento), 3=Anulado.';
COMMENT ON COLUMN TSR.EGRS.EGRSDBAT IS '0=Transferencia (pago por lote + archivo al banco), 1=Débito automático ya ejecutado por el banco. Espejo de PGS.PGTR.PGTRDBAT del pago del egreso.';
COMMENT ON COLUMN TSR.EGRS.EGRSASNT IS 'FK a CNT.ASNT. Asiento del pago: DEBE cuenta del grupo del producto / HABER cuenta contable del banco. Nulo mientras no se pague.';

CREATE INDEX IX_EGRS_EMPRESA_ESTADO ON TSR.EGRS (EGRSPJRQ, EGRSESTD);


-- ============================================================
-- 2. TSR.INGR — Ingreso de tesorería
-- ============================================================

CREATE TABLE TSR.INGR (
    INGRCDGO NUMBER(19)     NOT NULL,           -- PK
    INGRPJRQ NUMBER(19)     NOT NULL,           -- FK SCP.PJRQ empresa
    INGRTTLR NUMBER(19)         NULL,           -- FK TSR.TTLR quien origina el ingreso (opcional)
    INGRPRDC NUMBER(19)     NOT NULL,           -- FK CBR.PRDC producto CXC (define la cuenta contable vía su grupo)
    INGRDSCR VARCHAR2(500)  NOT NULL,           -- Concepto del ingreso
    INGRVLOR NUMBER(15,2)   NOT NULL,           -- Valor
    INGRFCHA DATE           NOT NULL,           -- Fecha en que entró el dinero
    INGRCNBC NUMBER(19)     NOT NULL,           -- FK TSR.CNBC cuenta bancaria propia que recibió
    INGRREFR VARCHAR2(200)      NULL,           -- Referencia (nro. de crédito, nota bancaria, etc.)
    INGRESTD NUMBER(2)      DEFAULT 1 NOT NULL, -- 1=Activo (contabilizado), 2=Anulado
    INGRASNT NUMBER(19)         NULL,           -- FK CNT.ASNT asiento generado al registrar
    INGROBSR VARCHAR2(2000)     NULL,           -- Observaciones
    INGRUSAR NUMBER(19)         NULL,           -- FK SCP.PJRQ usuario que registra
    INGRFCRG TIMESTAMP          NULL,           -- Fecha/hora de registro
    CONSTRAINT PK_INGR PRIMARY KEY (INGRCDGO),
    CONSTRAINT FK_INGR_EMPRESA  FOREIGN KEY (INGRPJRQ) REFERENCES SCP.PJRQ (PJRQCDGO),
    CONSTRAINT FK_INGR_TITULAR  FOREIGN KEY (INGRTTLR) REFERENCES TSR.TTLR (TTLRCDGO),
    CONSTRAINT FK_INGR_PRODUCTO FOREIGN KEY (INGRPRDC) REFERENCES CBR.PRDC (ID),
    CONSTRAINT FK_INGR_CUENTA   FOREIGN KEY (INGRCNBC) REFERENCES TSR.CNBC (CNBCCDGO),
    CONSTRAINT FK_INGR_ASIENTO  FOREIGN KEY (INGRASNT) REFERENCES CNT.ASNT (ASNTCDGO),
    CONSTRAINT FK_INGR_USUARIO  FOREIGN KEY (INGRUSAR) REFERENCES SCP.PJRQ (PJRQCDGO)
);

CREATE SEQUENCE TSR.SQ_INGRCDGO START WITH 1 INCREMENT BY 1 NOCACHE;

COMMENT ON TABLE  TSR.INGR IS 'Ingresos de tesorería sin documento físico (intereses ganados, créditos bancarios, etc.). Se registran ya recibidos: asiento y movimiento bancario en el mismo paso.';
COMMENT ON COLUMN TSR.INGR.INGRPRDC IS 'FK a CBR.PRDC. La cuenta contable del ingreso sale del grupo del producto (CBR.GRPC.PLNNCDGO).';
COMMENT ON COLUMN TSR.INGR.INGRESTD IS '1=Activo (contabilizado), 2=Anulado.';
COMMENT ON COLUMN TSR.INGR.INGRASNT IS 'FK a CNT.ASNT. Asiento del ingreso: DEBE cuenta contable del banco / HABER cuenta del grupo del producto.';

CREATE INDEX IX_INGR_EMPRESA_ESTADO ON TSR.INGR (INGRPJRQ, INGRESTD);


-- ============================================================
-- 3. ALTER PGS.PGTR — el pago puede apuntar a un egreso
-- ============================================================

-- 3.1 La factura deja de ser obligatoria (el pago paga UNA de dos cosas:
--     una factura de compra O un egreso de tesorería).
--     Si da ORA-01451 es que la columna YA es nullable: ignorar y continuar.
--     Verificar con:
--     SELECT NULLABLE FROM ALL_TAB_COLUMNS
--      WHERE OWNER='PGS' AND TABLE_NAME='PGTR' AND COLUMN_NAME='PGTRFCTC';
ALTER TABLE PGS.PGTR MODIFY (PGTRFCTC NULL);

-- 3.2 FK al egreso
ALTER TABLE PGS.PGTR ADD PGTREGRS NUMBER(19) NULL;

ALTER TABLE PGS.PGTR
    ADD CONSTRAINT FK_PGTR_EGRESO
        FOREIGN KEY (PGTREGRS)
        REFERENCES TSR.EGRS (EGRSCDGO);

COMMENT ON COLUMN PGS.PGTR.PGTREGRS
    IS 'FK a TSR.EGRS. Egreso de tesorería que se paga. Excluyente con PGTRFCTC: el pago referencia una factura O un egreso, nunca ambos (lo valida el backend).';

CREATE INDEX IX_PGTR_EGRESO ON PGS.PGTR (PGTREGRS);

COMMIT;


-- ============================================================
-- Verificación
-- ============================================================

-- Tablas y secuencias creadas
-- SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER = 'TSR' AND TABLE_NAME IN ('EGRS','INGR');
-- SELECT SEQUENCE_NAME FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER = 'TSR' AND SEQUENCE_NAME IN ('SQ_EGRSCDGO','SQ_INGRCDGO');

-- PGTR: factura nullable y FK a egreso
-- SELECT COLUMN_NAME, NULLABLE FROM ALL_TAB_COLUMNS
--  WHERE OWNER = 'PGS' AND TABLE_NAME = 'PGTR' AND COLUMN_NAME IN ('PGTRFCTC','PGTREGRS');

-- Egresos con su pago y estado
-- SELECT e.EGRSCDGO, e.EGRSDSCR, e.EGRSVLOR, e.EGRSESTD, p.PGTRCDGO, p.PGTRESTD, p.PGTRDBAT
--   FROM TSR.EGRS e
--   LEFT JOIN PGS.PGTR p ON p.PGTREGRS = e.EGRSCDGO
--  ORDER BY e.EGRSCDGO DESC;

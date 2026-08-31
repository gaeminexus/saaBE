-- =====================================================================================
-- CRD.PGPC — pago mensual de la pension complementaria
-- FECHA: 2026-08-31
--
-- PARA QUE: el pago de pensiones es un PROCESO mensual, no una tabla de valores.
-- CRD.VPPC (ValorPagoPensionComplementaria) es configuracion pura —cuanto se le paga a cada
-- jubilado— y NO tiene ningun campo de periodo. Sin esta tabla no hay forma de responder
-- "¿ya se genero el pago de agosto de este jubilado?", y correr el proceso dos veces
-- duplicaria el pago.
--
-- ⚠️ LA IDEMPOTENCIA LA DA EL UNIQUE, NO EL CODIGO. `UK_PGPC_ENTD_PERIODO` es lo que hace
-- que el proceso sea seguro de correr dos veces AUNQUE alguien agregue una rama nueva y se
-- olvide del chequeo en Java. Un chequeo en codigo se puede saltear; una constraint no.
--
-- ⚠️ COLUMNAS VERIFICADAS UNA POR UNA contra com.saa.model.crd.PagoPensionComplementaria
-- (18 columnas mapeadas). Hibernate incluye TODA columna @Column en el SELECT que genera:
-- una que falte aca rompe CUALQUIER lectura de la entidad con ORA-00904, no solo la funcion
-- nueva.
--
-- ⚠️ LA PK ES IDENTITY, NO SECUENCIA. La entidad declara
-- @GeneratedValue(strategy = GenerationType.IDENTITY) — verificado. Si se creara con
-- secuencia, el INSERT desde la aplicacion fallaria por PK nula.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS — si alguno falla, PARAR
-- =====================================================================================

-- 0.1 El codigo de 4 letras PGPC libre en TODA la base. Esperado: 0 filas.
--     (Ya verificado libre en src/main/java/com/saa/model/ y en ALL_TABLES el 2026-08-31.)
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t WHERE t.TABLE_NAME = 'PGPC';

-- 0.2 Las tablas de las que depende. Esperado: 2 filas (CRD.ENTD y CRD.FLLL).
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME IN ('ENTD','FLLL')
ORDER  BY t.TABLE_NAME;

-- 0.3 El catalogo de tipo de movimiento. Esperado: los alternos 1-8 del rubro 235.
--     El alterno 9 (PAGO_PENSION) lo crea la seccion 2 de este script.
SELECT d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR
FROM   SCP.PDTR d
JOIN   SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
WHERE  r.PRBRALTR = 235
ORDER  BY d.PDTRALTR;

-- 0.4 Control de reservas, obligatorio antes de insertar un PDTR con clave explicita.
--     Esperado: MAX_PDTR = 1180. Si da otra cosa, PARAR y revisar
--     docs/logica-negocio/REGISTRO-RESERVAS-EQUIPOS.md.
SELECT MAX(PDTRCDGO) AS MAX_PDTR FROM SCP.PDTR;


-- =====================================================================================
-- 1. LA TABLA
-- =====================================================================================

CREATE TABLE CRD.PGPC (
    PGPCCDGO NUMBER GENERATED ALWAYS AS IDENTITY,
    ENTDCDGO NUMBER          NOT NULL,
    FLLLCDGO NUMBER,
    PGPCANNO NUMBER          NOT NULL,
    PGPCMESS NUMBER          NOT NULL,
    PGPCVLPN NUMBER(18,2),
    PGPCVLSG NUMBER(18,2),
    PGPCVLRR NUMBER(18,2)    NOT NULL,
    PGPCFCHA DATE,
    PGPCESTD NUMBER          NOT NULL,
    PGPCIDPG NUMBER,
    PGPCIDAP NUMBER,
    PGPCNMAS NUMBER,
    PGPCUSRG VARCHAR2(50),
    PGPCFCRG TIMESTAMP,
    PGPCFCPG DATE,
    PGPCUSAN VARCHAR2(50),
    PGPCFCAN TIMESTAMP,
    PGPCMTAN VARCHAR2(500)
);

ALTER TABLE CRD.PGPC ADD CONSTRAINT PK_PGPC PRIMARY KEY (PGPCCDGO);

ALTER TABLE CRD.PGPC ADD CONSTRAINT FK_PGPC_ENTD
    FOREIGN KEY (ENTDCDGO) REFERENCES CRD.ENTD(ENTDCDGO);
ALTER TABLE CRD.PGPC ADD CONSTRAINT FK_PGPC_FLLL
    FOREIGN KEY (FLLLCDGO) REFERENCES CRD.FLLL(FLLLCDGO);

-- ⭐ La constraint que hace idempotente al proceso. NO quitarla "para poder regenerar":
-- si hace falta regenerar un mes, se anula la fila (PGPCESTD) y se inserta otra, no se
-- permite un duplicado.
ALTER TABLE CRD.PGPC ADD CONSTRAINT UK_PGPC_ENTD_PERIODO
    UNIQUE (ENTDCDGO, PGPCANNO, PGPCMESS);

ALTER TABLE CRD.PGPC ADD CONSTRAINT CK_PGPC_MESS CHECK (PGPCMESS BETWEEN 1 AND 12);
ALTER TABLE CRD.PGPC ADD CONSTRAINT CK_PGPC_VLRR CHECK (PGPCVLRR > 0);

CREATE INDEX CRD.IDX_PGPC_PERIODO ON CRD.PGPC (PGPCANNO, PGPCMESS);
CREATE INDEX CRD.IDX_PGPC_ESTADO  ON CRD.PGPC (PGPCESTD);

COMMENT ON TABLE  CRD.PGPC          IS 'Bitacora del pago mensual de pension complementaria a un jubilado. Una fila por jubilado y periodo. El UNIQUE (ENTDCDGO, ANNO, MESS) es lo que hace idempotente al proceso de generacion: correrlo dos veces no puede duplicar el pago, aunque el chequeo en Java se saltee.';
COMMENT ON COLUMN CRD.PGPC.PGPCVLPN IS 'Valor de la pension del periodo, de CRD.VPPC.';
COMMENT ON COLUMN CRD.PGPC.PGPCVLSG IS 'Valor del seguro del periodo, de CRD.VPPC.';
COMMENT ON COLUMN CRD.PGPC.PGPCVLRR IS 'Total pagado = VLPN + VLSG. Se guarda calculado a proposito: es el monto que efectivamente se mando a pagar, y tiene que sobrevivir a un cambio posterior de la configuracion en VPPC.';
COMMENT ON COLUMN CRD.PGPC.PGPCIDPG IS 'Orden de pago en CXP (PGS.PGTR). Sin FK, mismo criterio que CRD.DVAP.DVAPIDPG.';
COMMENT ON COLUMN CRD.PGPC.PGPCIDAP IS 'Aporte NEGATIVO generado en CRD.APRT por este pago. Sin FK, mismo criterio.';
COMMENT ON COLUMN CRD.PGPC.PGPCNMAS IS 'Asiento (ASNTCDGO, la PK, no el correlativo ASNTNMRO). Hoy lo puebla el reconciliador con el asiento que genera CXP al confirmar el pago; CRD no genera uno propio para el pago mensual. Ver la duda abierta del §3.1 del levantamiento contable.';


-- =====================================================================================
-- 2. EL CATALOGO — rubro 235 alterno 9, PAGO_PENSION
-- =====================================================================================
-- Va APARTE de JUBILACION (alterno 7) a proposito: ese es el traslado inicial, unico e
-- irrepetible; este es el descuento mensual recurrente. Bajo el mismo tipo, el historico de
-- CRD.APRT no permitiria distinguir el traslado de los pagos.
--
-- PDTR 1200 = primer codigo del rango 1200-1299 del equipo A, reservado en
-- REGISTRO-RESERVAS-EQUIPOS.md el 2026-08-31.

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
SELECT 1200, r.PRBRCDGO, 'PAGO PENSION', 9, 'PAGO_PENSION', 9, 1
FROM   SCP.PRBR r
WHERE  r.PRBRALTR = 235
AND    NOT EXISTS (
         SELECT 1 FROM SCP.PDTR x
         WHERE  x.PRBRCDGO = r.PRBRCDGO AND x.PDTRALTR = 9
       );

COMMIT;


-- =====================================================================================
-- 3. CONTROLES POSTERIORES
-- =====================================================================================

-- 3.1 Las 18 columnas mapeadas por la entidad. Esperado: 19 filas (las 18 + nada de mas).
--     Si falta una, CUALQUIER lectura de PagoPensionComplementaria revienta con ORA-00904.
SELECT c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'PGPC'
ORDER  BY c.COLUMN_ID;

-- 3.2 La PK es IDENTITY, no secuencia. Esperado: IDENTITY_COLUMN = 'YES'.
--     Si dijera 'NO', el INSERT desde la aplicacion fallaria por PK nula: la entidad
--     declara GenerationType.IDENTITY y no da el codigo.
SELECT c.COLUMN_NAME, c.IDENTITY_COLUMN, c.DATA_DEFAULT
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'PGPC' AND c.COLUMN_NAME = 'PGPCCDGO';

-- 3.3 Constraints e indices. Esperado: PK, 2 FK, el UNIQUE y los 2 CHECK, todos ENABLED.
SELECT c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.STATUS, c.VALIDATED
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'PGPC'
ORDER  BY c.CONSTRAINT_TYPE, c.CONSTRAINT_NAME;

-- 3.4 Los indices en CRD, no en el schema de la sesion. Esperado: OWNER = 'CRD'.
SELECT i.OWNER, i.INDEX_NAME, i.TABLE_NAME, i.STATUS
FROM   ALL_INDEXES i
WHERE  i.TABLE_OWNER = 'CRD' AND i.TABLE_NAME = 'PGPC'
ORDER  BY i.INDEX_NAME;

-- 3.5 El catalogo. Esperado: 9 filas, alternos 1 a 9, con el 9 = PAGO PENSION.
SELECT d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRN, d.PDTRVLRV, d.PDTRESTD
FROM   SCP.PDTR d
JOIN   SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
WHERE  r.PRBRALTR = 235
ORDER  BY d.PDTRALTR;


-- =====================================================================================
-- 4. REVERSO — comentado a proposito. Leer antes de descomentar.
-- =====================================================================================
-- Solo si el WAR con el proceso de pension NO se desplego. Si ya se generaron pagos, borrar
-- la tabla pierde el rastro de que mes se pago a quien, y el proceso deja de ser idempotente
-- (volveria a generar todo).
--
-- DELETE FROM SCP.PDTR WHERE PDTRCDGO = 1200;
-- DROP TABLE CRD.PGPC CASCADE CONSTRAINTS;
-- COMMIT;
-- =====================================================================================

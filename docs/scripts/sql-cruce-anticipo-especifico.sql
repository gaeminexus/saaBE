-- ============================================================
-- Migración: cruce de anticipos contra un anticipo ESPECÍFICO
-- Módulos:   CXP (PGS.APLP, PGS.ANTP) y CXC (CBR.APLC, CBR.ANTC)
-- Fecha:     2026-08-20
--
-- Propósito
-- ---------
-- Hasta ahora el cruce de un anticipo con una factura se hacía
-- "por valor" contra el saldo GLOBAL de anticipos del titular
-- (TSR.PRCC.PRCCSLIN). La aplicación (APLP/APLC) no guardaba de
-- qué anticipo salía el dinero: su FK APLPANTP/APLCANTC apuntaba
-- al movimiento NEGATIVO que el cruce dejaba en la propia tabla
-- de anticipos, no al anticipo de origen.
--
-- Consecuencia: al anular un anticipo no se podía saber con
-- exactitud qué abonos a facturas había que deshacer; solo se
-- podía estimar por LIFO contra el saldo global.
--
-- Con este cambio:
--   * APLP.APLPANTO / APLC.APLCANTO  → FK al anticipo de ORIGEN.
--     Una aplicación de tipo 4 (ANTICIPO) consume UN anticipo.
--     Cruzar una factura contra dos anticipos genera DOS
--     aplicaciones, cada una con su asiento.
--   * ANTP.ANTPSALD / ANTC.ANTCSALD  → cambian de significado:
--     dejan de ser "saldo global acumulado al momento del
--     movimiento" y pasan a ser el SALDO DISPONIBLE REAL de ese
--     anticipo (valor − cruces activos). Es lo que el nombre del
--     campo siempre dio a entender y lo que el frontend ya
--     asumía en el estado de cuenta del titular.
--   * ANTP.ANTPESTD / ANTC.ESTADO admiten el estado 4 = Migrado,
--     para las filas negativas históricas que dejaron los cruces
--     viejos: se conservan como historial pero las pantallas ya
--     no las leen (el cruce se lee de APLP/APLC).
--
-- Este archivo contiene SOLO el DDL. La corrección de los datos
-- ya existentes en producción va aparte, con sus SELECT de
-- control, en:
--   docs/logica-negocio/pagos/MIGRACION-CRUCES-ANTICIPO.md
--
-- ORDEN OBLIGATORIO: primero este DDL, después el documento de
-- migración de datos, y recién entonces desplegar el WAR nuevo.
-- ============================================================


-- ============================================================
-- 1. CXP — PGS.APLP: anticipo de origen del cruce
-- ============================================================

ALTER TABLE PGS.APLP ADD APLPANTO NUMBER(19) NULL;

ALTER TABLE PGS.APLP
    ADD CONSTRAINT FK_APLP_ANTICIPO_ORIGEN
        FOREIGN KEY (APLPANTO)
        REFERENCES PGS.ANTP (ANTPCDGO);

COMMENT ON COLUMN PGS.APLP.APLPANTO
    IS 'FK a PGS.ANTP: anticipo del que sale el dinero de este cruce. Obligatorio en las aplicaciones con APLPTDPG=4 creadas desde 2026-08-20; nulo en los cruces anteriores que no se pudieron atribuir. NO confundir con APLPANTP, que apunta al movimiento negativo histórico.';

CREATE INDEX IX_APLP_ANTICIPO_ORIGEN ON PGS.APLP (APLPANTO);

-- Índice de apoyo para "cruces activos de este anticipo"
CREATE INDEX IX_APLP_ANTO_ESTADO ON PGS.APLP (APLPANTO, APLPESTD);


-- ============================================================
-- 2. CXC — CBR.APLC: anticipo de origen del cruce
-- ============================================================

ALTER TABLE CBR.APLC ADD APLCANTO NUMBER(19) NULL;

ALTER TABLE CBR.APLC
    ADD CONSTRAINT FK_APLC_ANTICIPO_ORIGEN
        FOREIGN KEY (APLCANTO)
        REFERENCES CBR.ANTC (ID);

COMMENT ON COLUMN CBR.APLC.APLCANTO
    IS 'FK a CBR.ANTC: anticipo del que sale el dinero de este cruce. Obligatorio en las aplicaciones con APLCTDPG=4 creadas desde 2026-08-20; nulo en los cruces anteriores que no se pudieron atribuir. NO confundir con APLCANTC, que apunta al movimiento negativo histórico.';

CREATE INDEX IX_APLC_ANTICIPO_ORIGEN ON CBR.APLC (APLCANTO);

CREATE INDEX IX_APLC_ANTO_ESTADO ON CBR.APLC (APLCANTO, APLCESTD);


-- ============================================================
-- 3. Nueva semántica del saldo por anticipo
-- ============================================================
-- No hay cambio de tipo: solo se redefine qué guarda la columna.
-- El backfill de los valores lo hace el documento de migración.

COMMENT ON COLUMN PGS.ANTP.ANTPSALD
    IS 'Saldo DISPONIBLE de este anticipo: valor menos los cruces activos que lo consumen (PGS.APLP con APLPTDPG=4, APLPESTD=1, APLPANTO=este anticipo). 0 cuando está agotado o anulado. Hasta 2026-08-20 guardaba el saldo global acumulado del titular.';

COMMENT ON COLUMN CBR.ANTC.ANTCSALD
    IS 'Saldo DISPONIBLE de este anticipo: valor menos los cruces activos que lo consumen (CBR.APLC con APLCTDPG=4, APLCESTD=1, APLCANTO=este anticipo). 0 cuando está agotado o anulado. Hasta 2026-08-20 guardaba el saldo global acumulado del titular.';

COMMENT ON COLUMN PGS.ANTP.ANTPESTD
    IS '1=Ingresado (pago pendiente), 2=Confirmado (con asiento y saldo), 3=Anulado, 4=Migrado (movimiento negativo histórico de un cruce anterior a 2026-08-20; se conserva como historial y las pantallas no lo leen).';

COMMENT ON COLUMN CBR.ANTC.ESTADO
    IS '1=Ingresado, 2=Confirmado (con asiento y saldo), 3=Anulado, 4=Migrado (movimiento negativo histórico de un cruce anterior a 2026-08-20; se conserva como historial y las pantallas no lo leen).';


-- ============================================================
-- 4. Índices de apoyo para la selección de anticipos disponibles
-- ============================================================
-- La pantalla de cruce pide "anticipos confirmados con saldo > 0
-- de este titular en esta empresa, del más antiguo al más nuevo".

CREATE INDEX IX_ANTP_DISPONIBLES ON PGS.ANTP (ANTPTTLR, ANTPPJRQ, ANTPESTD);
CREATE INDEX IX_ANTC_DISPONIBLES ON CBR.ANTC (TITULAR, EMPRESA, ESTADO);

COMMIT;


-- ============================================================
-- Verificación del DDL
-- ============================================================

-- Columnas nuevas
-- SELECT OWNER, TABLE_NAME, COLUMN_NAME, DATA_TYPE, NULLABLE
--   FROM ALL_TAB_COLUMNS
--  WHERE (OWNER = 'PGS' AND TABLE_NAME = 'APLP' AND COLUMN_NAME = 'APLPANTO')
--     OR (OWNER = 'CBR' AND TABLE_NAME = 'APLC' AND COLUMN_NAME = 'APLCANTO');

-- Constraints nuevas
-- SELECT OWNER, CONSTRAINT_NAME, TABLE_NAME, STATUS
--   FROM ALL_CONSTRAINTS
--  WHERE CONSTRAINT_NAME IN ('FK_APLP_ANTICIPO_ORIGEN','FK_APLC_ANTICIPO_ORIGEN');

-- Índices nuevos
-- SELECT OWNER, INDEX_NAME, TABLE_NAME, STATUS
--   FROM ALL_INDEXES
--  WHERE INDEX_NAME IN ('IX_APLP_ANTICIPO_ORIGEN','IX_APLP_ANTO_ESTADO',
--                       'IX_APLC_ANTICIPO_ORIGEN','IX_APLC_ANTO_ESTADO',
--                       'IX_ANTP_DISPONIBLES','IX_ANTC_DISPONIBLES');


-- ============================================================
-- Rollback (solo si hay que dar marcha atrás ANTES de migrar datos)
-- ============================================================
-- ALTER TABLE PGS.APLP DROP CONSTRAINT FK_APLP_ANTICIPO_ORIGEN;
-- DROP INDEX IX_APLP_ANTICIPO_ORIGEN;
-- DROP INDEX IX_APLP_ANTO_ESTADO;
-- ALTER TABLE PGS.APLP DROP COLUMN APLPANTO;
--
-- ALTER TABLE CBR.APLC DROP CONSTRAINT FK_APLC_ANTICIPO_ORIGEN;
-- DROP INDEX IX_APLC_ANTICIPO_ORIGEN;
-- DROP INDEX IX_APLC_ANTO_ESTADO;
-- ALTER TABLE CBR.APLC DROP COLUMN APLCANTO;
--
-- DROP INDEX IX_ANTP_DISPONIBLES;
-- DROP INDEX IX_ANTC_DISPONIBLES;
-- COMMIT;

-- =====================================================================
-- RHH.ODBS -- orden de pago de beneficio social (decimos acumulados)
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-01
--
-- QUE HACE
--   1. Secuencia RHH.SQ_ODBSCDGO
--   2. Tabla RHH.ODBS -- cabecera consolidada que agrupa las liquidaciones
--      de RHH.LQBS de un tipo de beneficio y un anio, y las paga con UN solo
--      pago en tesoreria.
--   3. Columna nueva RHH.LQBS.LQBSODBS -- enlaza cada liquidacion con su orden
--   4. Rubro 310 RHH_ESTADO_ORDEN_BENEFICIO + sus 4 detalles (PDTR 1500-1503)
--
--   Diseno completo:
--   docs/logica-negocio/rhh/PLAN-PAGO-BENEFICIOS-Y-SALIDA-POR-TESORERIA.md #3
--   Contrato: docs/logica-negocio/rhh/API-PAGO-BENEFICIOS-SOCIALES.md
--
-- POR QUE HACE FALTA UNA CABECERA Y NO ALCANZA CON LQBS
--   El usuario decidio el 2026-09-01 que el decimo acumulado se paga
--   CONSOLIDADO: un pago por el total con el detalle por empleado colgando,
--   igual que RHH.RDPG hace con la nomina. Un PagoProgramado guarda un solo
--   PGTRIDOR, asi que necesita un unico documento de origen al que apuntar.
--   Sin cabecera habria que registrar N pagos, uno por empleado -- que es la
--   opcion que el usuario descarto.
--
-- POR QUE ESTE FRENTE NO ES PREVENTIVO
--   Verificado contra la base el 2026-09-01: RHH.LQBS esta VACIA (cero
--   liquidaciones generadas nunca) pero RHH.PVNM ya acumula 140 provisiones
--   de decimo tercero ($10.849,11) y 140 de decimo cuarto ($5.580,95). O sea
--   que el pasivo se viene acumulando y NADA lo liquida. No se esta creando
--   una funcion nueva: se esta cerrando un ciclo que quedo cortado.
--
-- ORDEN RESPECTO DEL WAR
--   ESTE SCRIPT VA ANTES DEL WAR. La entidad ODBS y la columna LQBSODBS se
--   mapean en Java, y Hibernate incluye toda columna @Column en el SELECT que
--   genera: si la tabla o la columna no existen, cualquier lectura de
--   LiquidacionBeneficioSocial revienta con ORA-00904 / ORA-00942.
--
-- ES SEGURO CORRERLO DE CORRIDO. Tiene bloques de control ANTES y DESPUES, y
-- el bloque de reverso al final esta COMENTADO a proposito.
-- =====================================================================

SET PAGESIZE 200
SET LINESIZE 200


-- =====================================================================
-- BLOQUE 0 -- CONTROLES ANTES. Correr y LEER antes de seguir.
-- =====================================================================

-- 0.1 La tabla NO debe existir todavia. ESPERADO: 0 filas.
--     Si devuelve algo, PARAR: alguien la creo y hay que ver por que.
SELECT owner, table_name FROM all_tables WHERE table_name = 'ODBS';

-- 0.2 La columna NO debe existir todavia. ESPERADO: 0 filas.
SELECT column_name FROM all_tab_columns
 WHERE owner = 'RHH' AND table_name = 'LQBS' AND column_name = 'LQBSODBS';

-- 0.3 Revalidar el MAX justo antes de ejecutar -- regla 2 de
--     REGISTRO-RESERVAS-EQUIPOS.md. El rango reservado dice que me
--     corresponde; el MAX real dice que hay.
--     ESPERADO: MAX_PRBR < 310 y MAX_PDTR < 1500.
--     Si alguno los alcanzo, PARAR Y AVISAR. No forzar.
SELECT MAX(PRBRCDGO) AS MAX_PRBR FROM SCP.PRBR;
SELECT MAX(PDTRCDGO) AS MAX_PDTR FROM SCP.PDTR;

-- 0.4 Los codigos concretos deben estar libres. ESPERADO: 0 filas las dos.
SELECT PRBRCDGO FROM SCP.PRBR WHERE PRBRCDGO = 310;
SELECT PDTRCDGO FROM SCP.PDTR WHERE PDTRCDGO BETWEEN 1500 AND 1503;


-- =====================================================================
-- BLOQUE 1 -- SECUENCIA
-- =====================================================================

CREATE SEQUENCE RHH.SQ_ODBSCDGO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;


-- =====================================================================
-- BLOQUE 2 -- TABLA RHH.ODBS
-- =====================================================================

CREATE TABLE RHH.ODBS (
    ODBSCDGO  NUMBER          NOT NULL,   -- PK
    PJRQCDGO  NUMBER          NOT NULL,   -- empresa
    ODBSTPBN  NUMBER          NOT NULL,   -- tipo de beneficio (RHH_TIPO_BENEFICIO_SOCIAL)
    ODBSANOO  NUMBER          NOT NULL,   -- anio del beneficio
    ODBSRGON  NUMBER          NULL,       -- region: SOLO decimo cuarto
    ODBSNMRO  VARCHAR2(50)    NULL,       -- numero de la orden
    ODBSFCEM  DATE            NOT NULL,   -- fecha de emision
    ODBSFCPG  DATE            NULL,       -- fecha de acreditacion, al confirmar
    ODBSTTAL  NUMBER(18,2)    NOT NULL,   -- total consolidado
    ODBSNMEM  NUMBER          NOT NULL,   -- cantidad de empleados
    PGTRCDGO  NUMBER          NULL,       -- pago en tesoreria, al enviar
    ASNTCDGO  NUMBER          NULL,       -- asiento de baja de provision
    ODBSESTD  NUMBER          NOT NULL,   -- estado (rubro 310)
    ODBSOBSR  VARCHAR2(500)   NULL,
    ODBSFCHR  TIMESTAMP       NULL,
    ODBSUSRR  VARCHAR2(60)    NULL,
    CONSTRAINT PK_ODBS PRIMARY KEY (ODBSCDGO)
);

-- Empresa. PJRQ vive en SCP, asi que hace falta el GRANT REFERENCES ANTES
-- del ALTER. Si el usuario que corre esto no es SCP, pedirselo al DBA.
-- GRANT REFERENCES ON SCP.PJRQ TO RHH;
ALTER TABLE RHH.ODBS ADD CONSTRAINT FK_ODBS_PJRQ
    FOREIGN KEY (PJRQCDGO) REFERENCES SCP.PJRQ (PJRQCDGO);

-- Indice de la consulta de la bandeja: GET /odbs/listar filtra por
-- (empresa, anio, tipo) -- ver contrato #1.3bis.
CREATE INDEX IX_ODBS_EMPR_ANIO ON RHH.ODBS (PJRQCDGO, ODBSANOO, ODBSTPBN);

-- Una sola orden viva por combinacion. NO es un UNIQUE simple: una orden
-- ANULADA(4) no debe bloquear que se genere otra, asi que el indice unico
-- es FUNCIONAL y solo indexa las vivas (1,2,3); las anuladas entran como
-- NULL y Oracle no las considera.
CREATE UNIQUE INDEX UQ_ODBS_VIVA ON RHH.ODBS (
    CASE WHEN ODBSESTD IN (1,2,3) THEN PJRQCDGO END,
    CASE WHEN ODBSESTD IN (1,2,3) THEN ODBSTPBN END,
    CASE WHEN ODBSESTD IN (1,2,3) THEN ODBSANOO END,
    CASE WHEN ODBSESTD IN (1,2,3) THEN NVL(ODBSRGON, -1) END
);

COMMENT ON TABLE  RHH.ODBS           IS 'Orden de pago consolidada de beneficios sociales (decimos acumulados, fondos de reserva)';
COMMENT ON COLUMN RHH.ODBS.ODBSTPBN  IS 'Tipo de beneficio: detalle del rubro RHH_TIPO_BENEFICIO_SOCIAL (1 decimo tercero, 2 decimo cuarto, 3 fondos de reserva)';
COMMENT ON COLUMN RHH.ODBS.ODBSRGON  IS 'Region del decimo cuarto. NULL para los demas tipos';
COMMENT ON COLUMN RHH.ODBS.PGTRCDGO  IS 'PagoProgramado en PGS.PGTR. Se escribe al enviar a tesoreria';
COMMENT ON COLUMN RHH.ODBS.ASNTCDGO  IS 'Asiento de baja de provision. Lo genera RRHH al confirmar el pago, NO tesoreria';


-- =====================================================================
-- BLOQUE 3 -- COLUMNA NUEVA EN RHH.LQBS
-- =====================================================================
-- Nullable a proposito: las liquidaciones que se generen antes de armar la
-- orden nacen sin ella, y anular una orden las devuelve a NULL para que
-- puedan volver a agruparse.

ALTER TABLE RHH.LQBS ADD (LQBSODBS NUMBER NULL);

ALTER TABLE RHH.LQBS ADD CONSTRAINT FK_LQBS_ODBS
    FOREIGN KEY (LQBSODBS) REFERENCES RHH.ODBS (ODBSCDGO);

CREATE INDEX IX_LQBS_ODBS ON RHH.LQBS (LQBSODBS);

COMMENT ON COLUMN RHH.LQBS.LQBSODBS IS 'Orden de pago que paga esta liquidacion. NULL mientras no se agrupe';


-- =====================================================================
-- BLOQUE 4 -- RUBRO 310 Y SUS DETALLES
-- =====================================================================
-- Reservado en REGISTRO-RESERVAS-EQUIPOS.md el 2026-09-01. Primeros codigos
-- del bloque 310-329 / 1500-1599 de este equipo.
--
-- NOTA: no se sincroniza ninguna secuencia despues de estos INSERT. La regla
-- que lo pedia fue DEROGADA el 2026-08-31 (ver #1bis del registro): las
-- secuencias SCP.SQ_PRBRCDGO y SCP.SQ_PDTRCDGO NO EXISTEN, y la aplicacion
-- no crea rubros -- DetalleRubroRest solo expone dos @GET.

INSERT INTO SCP.PRBR (PRBRCDGO, PRBRNMBR, PRBRDSCR, PRBRESTD)
VALUES (310, 'RHH_ESTADO_ORDEN_BENEFICIO', 'Estado de la orden de pago de beneficios sociales (RHH.ODBS)', 1);

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (1500, 310, 'GENERADA',             1, 'GENERADA',             1, 1);
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (1501, 310, 'ENVIADA A TESORERIA',  2, 'ENVIADA_A_TESORERIA',  2, 1);
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (1502, 310, 'PAGADA',               3, 'PAGADA',               3, 1);
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (1503, 310, 'ANULADA',              4, 'ANULADA',              4, 1);

COMMIT;


-- =====================================================================
-- BLOQUE 5 -- CONTROLES DESPUES. Correr y LEER.
-- =====================================================================

-- 5.1 La tabla existe con sus 16 columnas. ESPERADO: 16.
SELECT COUNT(*) AS COLUMNAS_ODBS FROM all_tab_columns
 WHERE owner = 'RHH' AND table_name = 'ODBS';

-- 5.2 La columna nueva existe. ESPERADO: 1 fila, NUMBER, nullable Y.
SELECT column_name, data_type, nullable FROM all_tab_columns
 WHERE owner = 'RHH' AND table_name = 'LQBS' AND column_name = 'LQBSODBS';

-- 5.3 LQBS conserva sus 16 columnas + la nueva. ESPERADO: 17.
SELECT COUNT(*) AS COLUMNAS_LQBS FROM all_tab_columns
 WHERE owner = 'RHH' AND table_name = 'LQBS';

-- 5.4 El rubro y sus 4 detalles. ESPERADO: 4 filas.
SELECT d.PDTRCDGO, d.PDTRDSCR, d.PDTRALTR
  FROM SCP.PDTR d WHERE d.PRBRCDGO = 310 ORDER BY d.PDTRALTR;

-- 5.5 La secuencia existe. ESPERADO: 1 fila.
SELECT sequence_name FROM all_sequences
 WHERE sequence_owner = 'RHH' AND sequence_name = 'SQ_ODBSCDGO';

-- 5.6 Los indices. ESPERADO: 3 filas (PK_ODBS, IX_ODBS_EMPR_ANIO, UQ_ODBS_VIVA).
SELECT index_name, uniqueness FROM all_indexes
 WHERE owner = 'RHH' AND table_name = 'ODBS' ORDER BY index_name;


-- =====================================================================
-- BLOQUE 6 -- REVERSO. COMENTADO A PROPOSITO.
-- Descomentar SOLO si hay que deshacer, y en este orden.
-- =====================================================================
--
-- DELETE FROM SCP.PDTR WHERE PDTRCDGO BETWEEN 1500 AND 1503;
-- DELETE FROM SCP.PRBR WHERE PRBRCDGO = 310;
--
-- ALTER TABLE RHH.LQBS DROP CONSTRAINT FK_LQBS_ODBS;
-- DROP INDEX RHH.IX_LQBS_ODBS;
-- ALTER TABLE RHH.LQBS DROP COLUMN LQBSODBS;
--
-- DROP TABLE RHH.ODBS CASCADE CONSTRAINTS;
-- DROP SEQUENCE RHH.SQ_ODBSCDGO;
--
-- COMMIT;
--
-- OJO: el DROP de la columna LQBSODBS pierde el enlace de las liquidaciones
-- con su orden. Si ya se pago alguna, ese dato no se recupera.
-- =====================================================================

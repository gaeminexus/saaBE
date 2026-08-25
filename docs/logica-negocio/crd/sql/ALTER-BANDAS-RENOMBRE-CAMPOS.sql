-- =====================================================================================
-- MIGRACION — RENOMBRE DE CAMPOS AL ESTANDAR DEL SISTEMA (CRD.CBPR + CRD.BNDP)
-- FECHA: 2026-08-25
--
-- SOLO para ambientes donde ya se ejecuto la version ANTERIOR de
--   sql/DDL-BANDAS-PRODUCTO.sql (con CBPRFCDE / CBPRFCHS / BNDPPRDS).
-- Si el DDL aun NO se ha ejecutado en el ambiente, NO correr esto: el DDL actual
--   ya crea las columnas con los nombres correctos.
--
-- Motivo: los descriptores originales no seguian el estandar de nombres del sistema.
--   Estandar real verificado en las entidades del codigo (44 usos de FCIN/FCFN para
--   vigencias — p.ej. CNT.DTPL — y CNTD como descriptor establecido de cantidad):
--     CBPRFCDE (fecha desde)  -> CBPRFCIN (fecha inicio)
--     CBPRFCHS (fecha hasta)  -> CBPRFCFN (fecha fin)
--     BNDPPRDS (periodos)     -> BNDPCNTD (cantidad de periodos de 30 dias)
--
-- SQL PURO (sin comandos SQL*Plus). El rename conserva los datos: si la carga inicial
--   ya corrio, NO hay que recargarla.
-- =====================================================================================

-- Control previo: deben existir las columnas VIEJAS (esperado: 3 filas).
-- Si salen 0 filas, este ambiente ya tiene los nombres nuevos: no correr los ALTER.
SELECT table_name, column_name FROM all_tab_columns
WHERE owner = 'CRD'
  AND ((table_name = 'CBPR' AND column_name IN ('CBPRFCDE','CBPRFCHS'))
    OR (table_name = 'BNDP' AND column_name = 'BNDPPRDS'))
ORDER BY table_name, column_name;

ALTER TABLE CRD.CBPR RENAME COLUMN CBPRFCDE TO CBPRFCIN;

ALTER TABLE CRD.CBPR RENAME COLUMN CBPRFCHS TO CBPRFCFN;

ALTER TABLE CRD.BNDP RENAME COLUMN BNDPPRDS TO BNDPCNTD;

ALTER TABLE CRD.BNDP RENAME CONSTRAINT CK_BNDP_PRDS TO CK_BNDP_CNTD;

-- Comentarios de columna (el rename no los mueve de nombre, pero se re-aplican
-- para que queden asociados y con el texto vigente):
COMMENT ON COLUMN CRD.CBPR.CBPRFCIN IS 'Inicio de vigencia de esta configuracion.';
COMMENT ON COLUMN CRD.CBPR.CBPRFCFN IS 'Fin de vigencia. NULL = configuracion vigente. Un cambio normativo cierra esta fecha y crea una configuracion nueva; la anterior queda para reprocesos/auditoria.';
COMMENT ON COLUMN CRD.BNDP.BNDPCNTD IS 'Periodos de 30 dias que abarca. NULL = banda abierta (el resto); solo puede serlo la ultima banda.';

-- Control posterior: columnas nuevas presentes (esperado: 3 filas) y viejas ausentes.
SELECT table_name, column_name FROM all_tab_columns
WHERE owner = 'CRD'
  AND ((table_name = 'CBPR' AND column_name IN ('CBPRFCIN','CBPRFCFN','CBPRFCDE','CBPRFCHS'))
    OR (table_name = 'BNDP' AND column_name IN ('BNDPCNTD','BNDPPRDS')))
ORDER BY table_name, column_name;

-- Los datos se conservan: verificar conteos (esperado 28 / 143 si la carga ya corrio).
SELECT (SELECT COUNT(*) FROM CRD.CBPR) CBPR, (SELECT COUNT(*) FROM CRD.BNDP) BNDP FROM dual;

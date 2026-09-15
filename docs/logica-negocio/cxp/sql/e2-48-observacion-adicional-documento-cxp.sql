-- =====================================================================
-- e2-48 — PGS.DCXP.DCXPOBAD: observacion adicional que el usuario escribe
--          al registrar un documento cargado por XML
-- Modulo: cxp  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-15
--
-- ⚠️ DDL. Va ANTES del WAR: la entidad DocumentoCxp mapea esta columna y,
--    sin ella, toda lectura de PGS.DCXP muere con ORA-00904.
--    Diseño: cxp/PLAN-OBSERVACION-ASIENTO-DOCUMENTOS-CXP.md
--
-- Por que una columna nueva y no DCXPOBSR: DCXPOBSR la escribe y la borra
-- el propio proceso (motivos del sistema). Por que en DCXP y no en la tabla
-- de destino: si el registro se corta por productos pendientes, el documento
-- de destino todavia no existe.
-- 500 CHAR, no 500: con semantica de bytes 500 letras con tildes no entran.
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — Control ANTES
-- ESPERADO: TABLA_EXISTE = 1 y COLUMNA_EXISTE = 0.
--   Si COLUMNA_EXISTE = 1 -> ya se corrio: NO correr el bloque 1.
--   Si TABLA_EXISTE = 0   -> se esta en otro esquema/usuario: DETENERSE.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - antes' AS bloque,
       (SELECT COUNT(*) FROM all_tables
         WHERE owner = 'PGS' AND table_name = 'DCXP')                          AS tabla_existe,
       (SELECT COUNT(*) FROM all_tab_columns
         WHERE owner = 'PGS' AND table_name = 'DCXP' AND column_name = 'DCXPOBAD') AS columna_existe
  FROM dual;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — DDL
-- ---------------------------------------------------------------------
ALTER TABLE PGS.DCXP ADD (DCXPOBAD VARCHAR2(500 CHAR));

COMMENT ON COLUMN PGS.DCXP.DCXPOBAD IS
  'Observacion adicional escrita por el usuario al registrar el documento; se agrega a la observacion del asiento';


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Control DESPUES
-- ESPERADO: una fila, DATA_TYPE = VARCHAR2, CHAR_LENGTH = 500,
--           CHAR_USED = 'C', NULLABLE = 'Y'.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - despues' AS bloque,
       column_name, data_type, char_length, char_used, nullable
  FROM all_tab_columns
 WHERE owner = 'PGS' AND table_name = 'DCXP' AND column_name = 'DCXPOBAD';


-- ---------------------------------------------------------------------
-- REVERSO — comentado. Solo con el WAR anterior desplegado: el WAR nuevo
-- mapea la columna y sin ella no lee PGS.DCXP.
-- ---------------------------------------------------------------------
-- ALTER TABLE PGS.DCXP DROP COLUMN DCXPOBAD;

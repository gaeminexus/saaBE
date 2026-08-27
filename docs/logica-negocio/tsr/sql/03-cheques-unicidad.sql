-- =====================================================================
-- NOTA 2026-08-27: este script YA NO HACE FALTA en una base nueva.
-- El script 01 fue consolidado y ahora crea directamente el indice UNICO
-- PGS.UQ_PGTR_DTCH. Este archivo se conserva solo como registro de la
-- correccion aplicada a la base LOCAL, donde el 01 se habia ejecutado en
-- su version anterior (que creaba un indice no unico y sin prefijo de
-- schema, quedando como SCP.IDX_PGTR_DTCH).
--
-- En PRODUCCION: ejecutar solo el 01 consolidado. NO ejecutar este.
-- =====================================================================

-- =====================================================================
-- CHEQUES: red de seguridad contra doble asignación de un mismo cheque
-- Módulo: TSR / CXP
-- Fecha:  2026-08-27  (corregido el mismo día, dos veces: ver notas)
-- Autor:  orquestador
--
-- Complementa a 01-cheques-pago-programado.sql (ya ejecutado en local).
-- Motivo: la selección del siguiente cheque disponible es un SELECT sin
-- lock; dos registros simultáneos desde la misma cuenta podrían tomar el
-- mismo cheque. El backend agrega un lock pesimista, y este índice único
-- garantiza que la base rechace el segundo aunque el lock falle.
-- Oracle permite múltiples NULL en un índice único, así que los pagos sin
-- cheque (transferencia, débito automático) no se ven afectados.
--
-- DOS TRAMPAS QUE YA MORDIERON, documentadas para no repetirlas:
--
-- 1) El script 01 creó el índice como `CREATE INDEX IDX_PGTR_DTCH ON
--    PGS.PGTR(...)` — SIN prefijo de schema. Oracle lo creó en el schema
--    de la SESIÓN (SCP), no en el de la tabla (PGS). El índice igual ocupa
--    la columna, así que `CREATE UNIQUE INDEX` falla con ORA-01408 (such
--    column list already indexed). El 01 ya quedó corregido con prefijo
--    para cuando se ejecute en producción.
-- 2) Por lo anterior, el control NO puede filtrar por `OWNER` (dueño del
--    índice) sino por `TABLE_OWNER` (dueño de la tabla). Filtrando por
--    OWNER = 'PGS' el índice es invisible y parece que no existe.
--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 1: diagnóstico — todos los índices sobre PGS.PGTR, sea cual sea
--   el schema al que pertenezcan. Se espera PGS.PK_PGTR y SCP.IDX_PGTR_DTCH.
-- ---------------------------------------------------------------------
SELECT I.OWNER, I.INDEX_NAME, I.UNIQUENESS, I.STATUS, C.COLUMN_NAME, C.COLUMN_POSITION
  FROM ALL_INDEXES I
  JOIN ALL_IND_COLUMNS C
    ON C.INDEX_OWNER = I.OWNER AND C.INDEX_NAME = I.INDEX_NAME
 WHERE I.TABLE_OWNER = 'PGS' AND I.TABLE_NAME = 'PGTR'
 ORDER BY I.OWNER, I.INDEX_NAME, C.COLUMN_POSITION;

-- ---------------------------------------------------------------------
-- BLOQUE 2: control previo — no debe haber un mismo cheque en dos pagos.
--   Debe devolver 0 filas. Si devuelve alguna, NO crear el índice: hay
--   que depurar esos pagos primero.
-- ---------------------------------------------------------------------
SELECT PGTRDTCH, COUNT(*) AS VECES
  FROM PGS.PGTR
 WHERE PGTRDTCH IS NOT NULL
 GROUP BY PGTRDTCH
HAVING COUNT(*) > 1;

-- ---------------------------------------------------------------------
-- BLOQUE 3: reemplazar el índice no único por el único.
--   Usar el OWNER que haya mostrado el BLOQUE 1 en el DROP.
--   Si el CREATE falla por privilegios (la sesión no puede crear objetos
--   en PGS), ejecutarlo sin prefijo: queda en el schema de la sesión y
--   cumple la misma función.
-- ---------------------------------------------------------------------
DROP INDEX SCP.IDX_PGTR_DTCH;

CREATE UNIQUE INDEX PGS.UQ_PGTR_DTCH ON PGS.PGTR(PGTRDTCH);

-- ---------------------------------------------------------------------
-- BLOQUE 4: control final — UQ_PGTR_DTCH UNIQUE/VALID, y ya no debe
--   quedar ningún IDX_PGTR_DTCH en ningún schema.
-- ---------------------------------------------------------------------
SELECT I.OWNER, I.INDEX_NAME, I.UNIQUENESS, I.STATUS, C.COLUMN_NAME
  FROM ALL_INDEXES I
  JOIN ALL_IND_COLUMNS C
    ON C.INDEX_OWNER = I.OWNER AND C.INDEX_NAME = I.INDEX_NAME
 WHERE I.TABLE_OWNER = 'PGS' AND I.TABLE_NAME = 'PGTR'
 ORDER BY I.OWNER, I.INDEX_NAME, C.COLUMN_POSITION;

-- Control: la FK sigue habilitada y validada (los constraints sí viven
-- en el schema de la tabla, aquí OWNER es correcto)
SELECT CONSTRAINT_NAME, STATUS, VALIDATED
  FROM ALL_CONSTRAINTS
 WHERE OWNER = 'PGS' AND TABLE_NAME = 'PGTR' AND CONSTRAINT_NAME = 'FK_PGTR_DTCH';

-- =====================================================================================
-- c20-03 — SOLO LECTURA (paso 1 de 2). omen-saa-1, 2026-11-01.
-- Decisión del usuario: crear un usuario técnico «REPROCESO» con el que el reproceso grabe los
-- periodos HASTA agosto 2026 (la operación todavía no estaba en el SAA). Desde septiembre 2026 el
-- reproceso graba con los usuarios reales. Ver cnt/DISENO-USUARIO-OBLIGATORIO.md §8.
-- Antes de escribir el INSERT hay que ver la estructura real de SCP.PJRQ (la entidad Usuario solo
-- mapea 6 columnas) y cómo está armado el usuario 38 que usan los cargadores históricos.
-- Pasar TODA la salida al árbitro; con ella escribe el paso 2 (c20-04, el INSERT).
-- =====================================================================================

-- 1. Columnas de SCP.PJRQ (cuáles son NOT NULL y sus valores por defecto).
SELECT COLUMN_ID, COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE, DATA_DEFAULT
  FROM ALL_TAB_COLUMNS WHERE OWNER = 'SCP' AND TABLE_NAME = 'PJRQ' ORDER BY COLUMN_ID;

-- 2. El usuario 38 («usuario 1», el de los cargadores históricos), su padre y la empresa 1236.
SELECT * FROM SCP.PJRQ WHERE PJRQCDGO IN (38, 1236)
   OR PJRQCDGO = (SELECT PJRQCDPD FROM SCP.PJRQ WHERE PJRQCDGO = 38);

-- 3. ¿Ya existe un REPROCESO o un SISTEMA? ESPERADO: 0 filas.
SELECT PJRQCDGO, PJRQNMBR FROM SCP.PJRQ WHERE UPPER(PJRQNMBR) IN ('REPROCESO', 'SISTEMA', 'SAA_REPROCESO');

-- 4. Secuencia frente al MAX (para no chocar con ORA-00001).
SELECT (SELECT MAX(PJRQCDGO) FROM SCP.PJRQ) AS MAX_PK, s.LAST_NUMBER AS SECUENCIA
  FROM ALL_SEQUENCES s WHERE s.SEQUENCE_OWNER = 'SCP' AND s.SEQUENCE_NAME = 'SQ_PJRQCDGO';

-- 5. Tablas que cuelgan de un usuario (permisos, clave, estado), para saber qué más hace falta o qué NO
--    darle (un usuario técnico no debe poder entrar al sistema).
SELECT c.TABLE_NAME, c.CONSTRAINT_NAME
  FROM ALL_CONSTRAINTS c JOIN ALL_CONSTRAINTS p ON p.CONSTRAINT_NAME = c.R_CONSTRAINT_NAME AND p.OWNER = c.R_OWNER
 WHERE c.CONSTRAINT_TYPE = 'R' AND p.OWNER = 'SCP' AND p.TABLE_NAME = 'PJRQ'
 ORDER BY c.TABLE_NAME;

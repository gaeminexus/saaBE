-- =====================================================================================
-- e3-14 — Árbol de permisos (SCP.PJRQ, PGSPCDGO = 11): «Horas extra» pasa a colgar de
--          «Importación de marcaciones» y se retira el duplicado de «Procesos»
-- Equipo omen-saa-3 · 2026-10-27 · Solo datos de SCP.PJRQ, sin DDL
--
-- Por qué: el menú de RRHH (saaFE, menurecursoshumanos.component.ts) dejó de mostrar
-- «Horas extra» en Procesos y la puso como submenú de «Importación de marcaciones»
-- (Asistencia). El árbol de permisos espeja el menú (lap1-15), así que:
--   - 1526 HORAS EXTRA: padre 1522 (ASISTENCIA), nivel 4 -> padre 1524 (IMPORTACION DE
--     MARCACIONES), nivel 5. Mismo id: los permisos ya asignados a 1526 se conservan.
--   - 1538 HORAS EXTRA (Procesos): ningún nodo del menú lo usa. Se borra SOLO si nadie lo
--     tiene asignado en SCP.PENP y no tiene hijos.
--
-- ⚠ SCP.PJRQ es también la tabla de EMPRESAS (5, 12) y USUARIOS (9). Todo lleva
--   PGSPCDGO = 11 y el id exacto. No ampliar esos filtros.
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES
-- 0.1 Se esperan 3 filas: 1524 (nivel 4, padre 1522), 1526 (nivel 4, padre 1522),
--     1538 (nivel 4, padre 1530), las tres 'HORAS EXTRA'/'IMPORTACION DE MARCACIONES'
SELECT PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD, PJRQINGR
  FROM SCP.PJRQ
 WHERE PGSPCDGO = 11 AND PJRQCDGO IN (1524, 1526, 1538)
 ORDER BY PJRQCDGO;

-- 0.2 Asignaciones de 1526 y 1538 en SCP.PENP. Las de 1526 se conservan.
--     Si 1538 tiene filas, el DELETE del BLOQUE 1 no borra nada (queda huérfano): avisar.
SELECT PJRQCDGO, COUNT(*) AS ASIGNACIONES
  FROM SCP.PENP
 WHERE PJRQCDGO IN (1526, 1538)
 GROUP BY PJRQCDGO;

-- 0.3 Ni 1526 ni 1538 tienen hijos: CERO
SELECT COUNT(*) AS HIJOS FROM SCP.PJRQ WHERE PGSPCDGO = 11 AND PJRQCDPD IN (1526, 1538);

-- BLOQUE 1 — CORRECCIÓN
-- 1.1 Debe decir «1 fila actualizada»
UPDATE SCP.PJRQ
   SET PJRQCDPD = 1524,
       PJRQNVLL = 5
 WHERE PGSPCDGO = 11
   AND PJRQCDGO = 1526
   AND PJRQCDPD = 1522;

-- 1.2 «1 fila suprimida» si nadie lo tiene asignado; «0 filas» si sí (no es error, avisar).
--     Si da ORA-02292, otra tabla lo referencia: no insistir, avisar.
DELETE FROM SCP.PJRQ p
 WHERE p.PGSPCDGO = 11
   AND p.PJRQCDGO = 1538
   AND p.PJRQCDPD = 1530
   AND NOT EXISTS (SELECT 1 FROM SCP.PENP e WHERE e.PJRQCDGO = 1538)
   AND NOT EXISTS (SELECT 1 FROM SCP.PJRQ h WHERE h.PGSPCDGO = 11 AND h.PJRQCDPD = 1538);

COMMIT;

-- BLOQUE 2 — CONTROL DESPUÉS
-- 2.1 1524 nivel 4 padre 1522; 1526 nivel 5 padre 1524; 1538 ya no aparece (o sigue igual si 0.2 tenía filas)
SELECT PJRQCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD
  FROM SCP.PJRQ
 WHERE PGSPCDGO = 11 AND PJRQCDGO IN (1524, 1526, 1538)
 ORDER BY PJRQCDGO;

-- 2.2 Subárbol de ASISTENCIA tal como lo dibuja la pantalla de permisos
SELECT LPAD(' ', 2 * (LEVEL - 1)) || PJRQNMBR AS NODO, PJRQCDGO, PJRQNVLL
  FROM SCP.PJRQ
 WHERE PGSPCDGO = 11
 START WITH PJRQCDGO = 1522
 CONNECT BY PRIOR PJRQCDGO = PJRQCDPD AND PGSPCDGO = 11
 ORDER SIBLINGS BY PJRQCDGO;

-- Sin secuencia que resincronizar: no se inserta nada (SCP.SQ_PJRQCDGO no existe, ver lap1-14).

-- REVERSO (comentado) — vuelve exactamente a lo de lap1-15
-- UPDATE SCP.PJRQ SET PJRQCDPD = 1522, PJRQNVLL = 4
--  WHERE PGSPCDGO = 11 AND PJRQCDGO = 1526 AND PJRQCDPD = 1524;
-- INSERT INTO SCP.PJRQ (PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD, PJRQINGR)
--   SELECT 1538, 11, 'HORAS EXTRA', 4, 1530, 1 FROM DUAL
--    WHERE NOT EXISTS (SELECT 1 FROM SCP.PJRQ WHERE PJRQCDGO = 1538);
-- COMMIT;

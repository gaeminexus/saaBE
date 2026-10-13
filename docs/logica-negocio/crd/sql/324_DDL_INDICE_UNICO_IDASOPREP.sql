-- =====================================================================================
-- 324 - INDICE UNICO sobre CRD.PRST.PRSTIDAS (idAsoprep)
-- FECHA: 2026-10-13 · EQUIPO: omen-saa-1 (crd)
-- Diseno: PLAN-CICLO-OTORGAMIENTO.md §7, regla U6.3 — es la garantia de que el numero que
-- asigna "Aprobar" (MAX + 1) jamas se repita, aunque dos personas aprueben a la vez.
--
-- ⛔ ESTE SCRIPT SI ESCRIBE (DDL). Va ANTES de usar en produccion el WAR con U6 (b9e8cf31 o posterior).
-- Sin comandos de SQL*Plus.
--
-- Medido con el sql/323 el 2026-10-13 (salida del usuario):
--   5.664 prestamos · 5.644 con idAsoprep · 20 sin (NULL) · 1 en cero (8361, CANCELADO, 2008)
--   MAX = 71180 -> el proximo que asignara Aprobar es 71181
--   Bloque 2 (duplicados) VACIO · Bloque 4 (indices previos) VACIO
--   Bloque 5: los 10 mas altos son correlativos (71171..71180), sin saltos raros
-- => El indice unico se puede crear directo:
--    - los 20 NULL no chocan (Oracle no indexa filas con la clave entera en NULL);
--    - el unico 0 tampoco choca (es uno solo). No se toca: es historico de 2008.
-- =====================================================================================


-- ==========================================================================
-- BLOQUE 0 - (!) CONTROL PREVIO: re-medir justo antes (pudo entrar un prestamo desde el 323)
-- ==========================================================================
SELECT PRSTIDAS, COUNT(*) AS VECES
  FROM CRD.PRST
 WHERE PRSTIDAS IS NOT NULL
 GROUP BY PRSTIDAS
HAVING COUNT(*) > 1;
-- (!) CERO filas. Si devuelve algo: PARAR y avisar (el CREATE fallaria con ORA-01452).

SELECT INDEX_NAME FROM ALL_INDEXES
 WHERE OWNER = 'CRD' AND INDEX_NAME = 'UX_PRST_PRSTIDAS';
-- (!) CERO filas. Si ya existe, el script ya se corrio: no repetir.


-- ==========================================================================
-- BLOQUE 1 - Crear el indice unico
-- ==========================================================================
CREATE UNIQUE INDEX CRD.UX_PRST_PRSTIDAS ON CRD.PRST (PRSTIDAS);


-- ==========================================================================
-- BLOQUE 2 - (!) CONTROL POSTERIOR
-- ==========================================================================
SELECT i.INDEX_NAME, i.UNIQUENESS, i.STATUS, c.COLUMN_NAME
  FROM ALL_INDEXES i
  JOIN ALL_IND_COLUMNS c ON c.INDEX_OWNER = i.OWNER AND c.INDEX_NAME = i.INDEX_NAME
 WHERE i.OWNER = 'CRD' AND i.INDEX_NAME = 'UX_PRST_PRSTIDAS';
-- (!) Una fila: UNIQUE, VALID, PRSTIDAS.

SELECT MAX(PRSTIDAS) + 1 AS PROXIMO_IDASOPREP FROM CRD.PRST;
-- (!) Deberia seguir en 71181 si nadie aprobo nada desde el 323.


-- ==========================================================================
-- BLOQUE 3 - REVERSO (comentado)
-- ==========================================================================
-- Quitar el indice deja a "Aprobar" sin la garantia contra duplicados simultaneos (U6.3).
-- Solo si el indice estorba algo que hay que investigar, y avisando.
--
-- DROP INDEX CRD.UX_PRST_PRSTIDAS;

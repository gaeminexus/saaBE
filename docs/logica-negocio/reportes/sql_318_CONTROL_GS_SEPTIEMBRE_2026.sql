-- =====================================================================================
-- 318 — CONTROL (solo lectura), ANTES de reintentar los G de septiembre 2026
-- FECHA: 2026-10-05 · EQUIPO: omen-saa-1
--
-- El intento fallido («No result found ... DetalleEjecucionReporte») revirtio el EJRC y los EJRD,
-- pero el G40 corre en su propia transaccion y pudo haber confirmado filas. Con la FK
-- FK_CG40_EJRD eso no deberia ser posible; el script lo comprueba. Esperado: TODO en 0 / vacio.
-- Si algo NO da 0, PARAR y pasarselo al arbitro antes de reintentar.
-- =====================================================================================

-- 1. Ejecuciones de septiembre 2026. Esperado: 0 filas.
SELECT e.EJRCCDGO, e.EJRCMESS, e.EJRCANOO, e.EJRCESTD, e.EJRCFCGN FROM RPR.EJRC e
 WHERE e.EJRCMESS = 9 AND e.EJRCANOO = 2026;

-- 2. Filas de los G sin detalle de ejecucion valido (huerfanas). Esperado: 0 en todas.
SELECT 'CG40' AS TABLA, COUNT(*) AS HUERFANAS FROM RPR.CG40 t
 WHERE t.CG40EJRD IS NULL OR NOT EXISTS (SELECT 1 FROM RPR.EJRD d WHERE d.EJRDCDGO = t.CG40EJRD)
UNION ALL
SELECT 'CG41', COUNT(*) FROM RPR.CG41 t
 WHERE t.CG41EJRD IS NULL OR NOT EXISTS (SELECT 1 FROM RPR.EJRD d WHERE d.EJRDCDGO = t.CG41EJRD);

-- 3. La FK de CG40 existe y esta activa. Esperado: 1 fila, ENABLED.
SELECT c.CONSTRAINT_NAME, c.STATUS FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'RPR' AND c.CONSTRAINT_NAME = 'FK_CG40_EJRD';

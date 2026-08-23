-- ============================================================
-- SCRIPT: Limpiar datos de tablas de reportes G para pruebas
-- ORDEN IMPORTANTE: respetar FKs — primero hijos, luego padres
--
-- Paso 1: Vaciar las 12 tablas G (hijos de EJRD)
-- Paso 2: Vaciar EJRD (hijo de EJRC)
-- Paso 3: Vaciar EJRC (cabecera)
-- ============================================================

-- Paso 1: Tablas de reportes G (todas tienen FK a EJRD)
DELETE FROM RPR.CG40;
DELETE FROM RPR.CG41;
DELETE FROM RPR.CG42;
DELETE FROM RPR.CG43;
DELETE FROM RPR.CG44;
DELETE FROM RPR.CG45;
DELETE FROM RPR.CG46;
DELETE FROM RPR.CG47;
DELETE FROM RPR.CG48;
DELETE FROM RPR.CG49;
DELETE FROM RPR.CG50;
DELETE FROM RPR.CG51;

-- Paso 2: Detalle de ejecución (hijo de EJRC)
DELETE FROM RPR.EJRD;

-- Paso 3: Cabecera de ejecución
DELETE FROM RPR.EJRC;

COMMIT;

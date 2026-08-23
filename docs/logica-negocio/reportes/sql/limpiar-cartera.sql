-- ============================================================
-- SCRIPT: Limpiar tablas del proceso de Reportes de Cartera
-- Orden: primero hijos (FK → EJCC), luego padre (EJCC)
-- ============================================================

-- 1. Cuotas préstamos mensual (similar al G48)
DELETE FROM RPR.CCPM;

-- 2. Jubilados mensual (similar al G44)
DELETE FROM RPR.CJBM;

-- 3. Partícipes mensual (similar al G42)
DELETE FROM RPR.CPRM;

-- 4. Control de ejecución (tabla padre - va al final)
DELETE FROM RPR.EJCC;

-- Confirmar
COMMIT;

-- Verificar que quedaron vacías
SELECT 'EJCC' AS TABLA, COUNT(*) AS REGISTROS FROM RPR.EJCC
UNION ALL
SELECT 'CPRM', COUNT(*) FROM RPR.CPRM
UNION ALL
SELECT 'CJBM', COUNT(*) FROM RPR.CJBM
UNION ALL
SELECT 'CCPM', COUNT(*) FROM RPR.CCPM;

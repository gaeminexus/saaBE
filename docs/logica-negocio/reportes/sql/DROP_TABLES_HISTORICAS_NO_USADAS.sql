-- ================================================================
-- SCRIPT: Eliminar tablas históricas de reportes de cartera
--         que nunca se usaron y son código muerto.
--
-- Tablas a eliminar:
--   RPR.HMJB  (Histórico CJBM) — nunca se escribe. El CJBM
--             lee directamente de RPR.HM44 (mismo histórico G44).
--   RPR.HMCP  (Histórico CCPM) — nunca se escribe. La columna
--             provisionConstituida siempre va en 0 en el CCPM.
--   RPR.HMPR  (Histórico CPRM) — nunca se escribió ni leyó.
--
-- NOTA: RPR.HM44 NO se elimina — tiene datos reales de jubilados
--       y es leída tanto por G44 como por CJBM.
-- ================================================================

-- Verificar contenido antes de borrar (deben estar vacías)
SELECT 'HMJB' AS TABLA, COUNT(*) AS REGISTROS FROM RPR.HMJB
UNION ALL
SELECT 'HMCP', COUNT(*) FROM RPR.HMCP
UNION ALL
SELECT 'HMPR', COUNT(*) FROM RPR.HMPR;

-- ================================================================
-- ELIMINAR TABLAS
-- ================================================================

-- Histórico CJBM (vacía, nunca usada)
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE RPR.HMJB CASCADE CONSTRAINTS';
    DBMS_OUTPUT.PUT_LINE('RPR.HMJB eliminada correctamente');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('RPR.HMJB no existe o error: ' || SQLERRM);
END;
/

-- Histórico CCPM (vacía, nunca usada)
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE RPR.HMCP CASCADE CONSTRAINTS';
    DBMS_OUTPUT.PUT_LINE('RPR.HMCP eliminada correctamente');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('RPR.HMCP no existe o error: ' || SQLERRM);
END;
/

-- Histórico CPRM (vacía, nunca usada)
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE RPR.HMPR CASCADE CONSTRAINTS';
    DBMS_OUTPUT.PUT_LINE('RPR.HMPR eliminada correctamente');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('RPR.HMPR no existe o error: ' || SQLERRM);
END;
/

-- ================================================================
-- VERIFICAR QUE YA NO EXISTEN
-- ================================================================
SELECT TABLE_NAME
FROM   ALL_TABLES
WHERE  OWNER = 'RPR'
AND    TABLE_NAME IN ('HMJB', 'HMCP', 'HMPR')
ORDER BY TABLE_NAME;
-- Resultado esperado: 0 filas

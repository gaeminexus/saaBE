-- =====================================================================================
-- MEDICION-USAP-ELIMINADOS-APP-MOVIL.sql — equipo app1 (omen-app-1-arb), 2026-11-19
--
-- Para qué: desde ORDEN-ELIMINAR-CUENTA-APP-MOVIL-2026-11-19.md, «eliminar cuenta» BORRA la fila
-- de CRD.USAP (la regla de Google Play pide eliminar la cuenta, no desactivarla). Las cuentas que
-- se «eliminaron» ANTES de ese cambio siguen en la tabla con USAPESTD = 3 y con su clave.
-- Esta consulta las mide. SOLO LECTURA.
--
-- Estados (com.saa.rubros.EstadoUsuarioApp): 1 ACTIVO, 2 BLOQUEADO, 3 ELIMINADO.
-- =====================================================================================

-- 1. Distribución por estado
SELECT u.USAPESTD AS estado,
       CASE u.USAPESTD WHEN 1 THEN 'ACTIVO' WHEN 2 THEN 'BLOQUEADO' WHEN 3 THEN 'ELIMINADO'
                       ELSE 'OTRO' END AS nombre,
       COUNT(*) AS cuentas
  FROM CRD.USAP u
 GROUP BY u.USAPESTD
 ORDER BY u.USAPESTD;

-- 2. Detalle de las ELIMINADAS (sin la clave)
SELECT u.USAPCDGO, u.USAPIDNT, u.USAPFCRG AS fecha_creacion, u.USAPFCUA AS fecha_ultimo_acceso,
       u.USAPUSAR AS usuario_oficina
  FROM CRD.USAP u
 WHERE u.USAPESTD = 3
 ORDER BY u.USAPCDGO;

-- 3. TODAS las cuentas, con a quién pertenecen. Según el usuario (2026-11-19), TODAS las cuentas
--    de producción son de PRUEBA. Ver si están atadas a partícipes reales.
SELECT u.USAPCDGO, u.USAPIDNT, u.USAPESTD, u.USAPFCRG, u.USAPFCUA, u.USAPUSAR,
       e.ENTDCDGO
  FROM CRD.USAP u
  LEFT JOIN CRD.ENTD e ON e.ENTDNMID = u.USAPIDNT
 ORDER BY u.USAPCDGO;

-- =====================================================================================
-- LIMPIEZA ANTES DEL LANZAMIENTO — NO CORRER sin el sí del usuario.
-- Las cuentas de prueba usan cédulas de partícipes reales con claves que conoce quien probó: con
-- la app en las tiendas, quien sepa la cédula y esa clave entra a los datos de ese partícipe.
-- Se borran TODAS, menos la del revisor de las tiendas (completar su USAPCDGO cuando exista).
-- Correr JUSTO ANTES de publicar, no antes (mientras tanto el equipo las sigue usando para probar).
-- =====================================================================================
-- CREATE TABLE CRD.USAP_BK_LANZAMIENTO AS SELECT * FROM CRD.USAP;
-- DELETE FROM CRD.USAP WHERE USAPCDGO <> :usapcdgo_revisor;
-- SELECT COUNT(*) FROM CRD.USAP;   -- debe dar 1 (la del revisor)
-- COMMIT;

-- =====================================================================================
-- PURGA — NO CORRER sin el sí del usuario, y solo después de ver el resultado de arriba.
-- Deja estas cuentas igual que las que se eliminen con el código nuevo: sin fila.
-- Respaldo previo obligatorio.
-- =====================================================================================
-- CREATE TABLE CRD.USAP_BK_20261119 AS SELECT * FROM CRD.USAP WHERE USAPESTD = 3;
-- DELETE FROM CRD.USAP WHERE USAPESTD = 3;
-- -- filas borradas = filas del respaldo:
-- SELECT COUNT(*) FROM CRD.USAP_BK_20261119;
-- COMMIT;

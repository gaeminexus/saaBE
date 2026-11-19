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

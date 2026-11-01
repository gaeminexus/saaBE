-- =====================================================================================
-- 353 — SOLO LECTURA. omen-saa-1, 2026-11-01. Precondición para desplegar el B3 (commit de B3 en main):
-- desde B3, los aportes de una carga se cuentan con «APRTIDAS = carga AND CRARCDGO IS NOT NULL».
-- Un aporte de una carga real (APRTIDAS positivo) que NO tenga CRARCDGO dejaría de contarse en el
-- control recibido = aplicado, el asiento de aplicación y el reproceso de esa carga.
-- (El control 3.1 de crd/sql/78 cuenta también los APRTIDAS NEGATIVOS de los cargadores viejos
-- -99/-98/-88, que no son cargas y no importan; por eso esta consulta filtra > 0.)
-- ESPERADO: 0 filas en las dos. Si sale algo, NO desplegar B3 y pasar la salida al árbitro.
-- =====================================================================================

SELECT a.APRTIDAS AS CARGA, COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS TOTAL, MIN(a.APRTFCRG) AS DESDE, MAX(a.APRTFCRG) AS HASTA
  FROM CRD.APRT a
 WHERE a.APRTIDAS > 0
   AND a.CRARCDGO IS NULL
 GROUP BY a.APRTIDAS
 ORDER BY a.APRTIDAS;

-- ¿Esas cargas existen en CRD.CRAR? (si APRTIDAS > 0 no es una carga, no importa)
SELECT a.APRTIDAS AS CARGA, (SELECT COUNT(*) FROM CRD.CRAR c WHERE c.CRARCDGO = a.APRTIDAS) AS EXISTE_CARGA, COUNT(*) AS FILAS
  FROM CRD.APRT a
 WHERE a.APRTIDAS > 0 AND a.CRARCDGO IS NULL
 GROUP BY a.APRTIDAS
 ORDER BY a.APRTIDAS;

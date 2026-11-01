-- =====================================================================================
-- 342 — CORRECCIÓN DE DATOS: borra CRD.APRT.APRTIDAS en los aportes MIGRADOS.
-- omen-saa-1, 2026-10-31. Decisión del usuario: «Borra ese campo de los registros migrados (pero
-- solo de los migrados). En realidad no nos sirve».
--
-- Por qué: el control recibido = aplicado y el asiento de cada carga suman los aportes de la
-- carga por APRTIDAS. Los aportes migrados del sistema anterior (APRTUSRG = 'BATCH', sin carga:
-- CRARCDGO nulo) traen ahí un número de ese sistema, de 0 a 999995. Cuando coincide con el
-- número de una carga, se suma a ella: la carga ARCH 510 abortó por 0,10 (crd/sql/340-341), y
-- vienen choques en la 525, 549, 554... (CONTROL 2 del 341).
--
-- Qué hace: guarda un respaldo (APRTCDGO, APRTIDAS) y pone APRTIDAS = NULL SOLO donde
-- APRTUSRG = 'BATCH' AND CRARCDGO IS NULL. No toca ningún aporte generado por una carga, ni
-- montos, fechas o entidades.
--
-- ORDEN: CONTROL 0 -> si el CONTROL 0b da 0, BLOQUE 1 (respaldo) -> BLOQUE 2 (UPDATE + COMMIT)
--        -> CONTROL 1. El reverso usa el respaldo.
-- =====================================================================================

-- ---------------------------------------------------------------------
-- CONTROL 0 — quién tiene APRTIDAS sin carga real, por usuario. Solo 'BATCH' se toca.
-- ---------------------------------------------------------------------
SELECT a.APRTUSRG, COUNT(*) AS FILAS, MIN(a.APRTFCRG) AS DESDE, MAX(a.APRTFCRG) AS HASTA
  FROM CRD.APRT a
 WHERE a.APRTIDAS IS NOT NULL
   AND a.CRARCDGO IS NULL
 GROUP BY a.APRTUSRG
 ORDER BY 2 DESC;

-- CONTROL 0b — ningún aporte BATCH tiene carga real (debe dar 0). Si da > 0, AVISAR y no seguir.
SELECT COUNT(*) AS BATCH_CON_CARGA
  FROM CRD.APRT a
 WHERE a.APRTUSRG = 'BATCH'
   AND a.CRARCDGO IS NOT NULL;

-- CONTROL 0c — cuántas filas se van a tocar.
SELECT COUNT(*) AS A_BORRAR
  FROM CRD.APRT a
 WHERE a.APRTUSRG = 'BATCH' AND a.CRARCDGO IS NULL AND a.APRTIDAS IS NOT NULL;

-- ---------------------------------------------------------------------
-- BLOQUE 1 — respaldo para el reverso.
-- ---------------------------------------------------------------------
CREATE TABLE CRD.BK_APRT_APRTIDAS_20261031 AS
SELECT a.APRTCDGO, a.APRTIDAS
  FROM CRD.APRT a
 WHERE a.APRTUSRG = 'BATCH' AND a.CRARCDGO IS NULL AND a.APRTIDAS IS NOT NULL;

-- Debe dar lo mismo que el CONTROL 0c.
SELECT COUNT(*) AS RESPALDADAS FROM CRD.BK_APRT_APRTIDAS_20261031;

-- ---------------------------------------------------------------------
-- BLOQUE 2 — borrar el campo en los migrados.
-- ---------------------------------------------------------------------
UPDATE CRD.APRT a
   SET a.APRTIDAS = NULL
 WHERE a.APRTUSRG = 'BATCH'
   AND a.CRARCDGO IS NULL
   AND a.APRTIDAS IS NOT NULL;

COMMIT;

-- ---------------------------------------------------------------------
-- CONTROL 1 — después.
--   a) migrados con APRTIDAS: 0
--   b) choques con números de carga 500-1000 sin carga real: solo usuarios distintos de BATCH (si
--      sale alguno, AVISAR: sería otro origen con el mismo problema)
-- ---------------------------------------------------------------------
SELECT COUNT(*) AS MIGRADOS_CON_APRTIDAS
  FROM CRD.APRT a
 WHERE a.APRTUSRG = 'BATCH' AND a.APRTIDAS IS NOT NULL;

SELECT a.APRTUSRG, a.APRTIDAS, COUNT(*) AS FILAS
  FROM CRD.APRT a
 WHERE a.APRTIDAS BETWEEN 500 AND 1000
   AND a.CRARCDGO IS NULL
 GROUP BY a.APRTUSRG, a.APRTIDAS
 ORDER BY a.APRTIDAS;

-- =====================================================================================
-- REVERSO (comentado): devuelve el valor original desde el respaldo.
-- =====================================================================================
-- UPDATE CRD.APRT a
--    SET a.APRTIDAS = (SELECT b.APRTIDAS FROM CRD.BK_APRT_APRTIDAS_20261031 b WHERE b.APRTCDGO = a.APRTCDGO)
--  WHERE a.APRTCDGO IN (SELECT APRTCDGO FROM CRD.BK_APRT_APRTIDAS_20261031);
-- COMMIT;
-- (Cuando ya no haga falta: DROP TABLE CRD.BK_APRT_APRTIDAS_20261031;)

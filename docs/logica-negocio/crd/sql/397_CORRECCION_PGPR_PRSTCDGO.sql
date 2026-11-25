-- =====================================================================================
-- 397 — CORRECCIÓN DEL DATO: CRD.PGPR.PRSTCDGO nulo = el préstamo de su cuota (DTPR.PRSTCDGO)
-- Equipo omen-saa-1 · 2026-11-25 · Lote 10. REVISABLE: NO está ejecutado en ninguna base. SIN COMMIT (salvo el CTAS del respaldo, que es DDL).
-- Corre DESPUÉS de revisar los resultados de sql/396.
--
-- Qué hace:
--   · Solo corrige los PGPR con PRSTCDGO NULO: toma el préstamo de su cuota (todo PGPR tiene cuota y toda cuota su préstamo).
--   · NO toca los que tienen un PRSTCDGO DISTINTO (no nulo) del de su cuota: pueden ser un pago mal asignado de verdad. Se listan aparte (BLOQUE 4).
--   · No cambia ningún otro campo; no toca cuotas, saldos ni asientos.
-- Orden: BLOQUE 0 (controles) → BLOQUE 1 (respaldo, DDL, hace commit implícito de nada pendiente) → BLOQUE 2 (UPDATE) → BLOQUE 3 (controles)
-- → decidir COMMIT o ROLLBACK. El reverso está al final, comentado.
-- =====================================================================================

-- BLOQUE 0 — Controles previos (solo lectura).
-- 0a. Situación de los PGPR (todos, vigentes y anulados). Esperado antes: nulos > 0.
SELECT CASE WHEN g.PRSTCDGO IS NULL THEN 'NULO' WHEN g.PRSTCDGO <> d.PRSTCDGO THEN 'DISTINTO' ELSE 'CORRECTO' END AS situacion,
       COUNT(*) AS pagos, SUM(CASE WHEN NVL(g.PGPRANUL, 0) = 0 THEN 1 ELSE 0 END) AS vigentes
  FROM CRD.PGPR g LEFT JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
 GROUP BY CASE WHEN g.PRSTCDGO IS NULL THEN 'NULO' WHEN g.PRSTCDGO <> d.PRSTCDGO THEN 'DISTINTO' ELSE 'CORRECTO' END ORDER BY 1;
-- 0b. PGPR sin cuota o con cuota inexistente (no se pueden corregir: se listan).
SELECT g.PGPRCDGO, g.DTPRCDGO, g.PRSTCDGO, g.PGPRFCHA, g.PGPRVLRR, g.PGPRANUL FROM CRD.PGPR g
 WHERE g.DTPRCDGO IS NULL OR NOT EXISTS (SELECT 1 FROM CRD.DTPR d WHERE d.DTPRCDGO = g.DTPRCDGO);

-- BLOQUE 1 — Respaldo de las filas que se van a cambiar (DDL: crea la tabla y hace commit implícito; no hay nada pendiente antes de esto).
CREATE TABLE CRD.BK_PGPR_PRSTCDGO_20261125 AS
SELECT g.PGPRCDGO, g.DTPRCDGO, g.PRSTCDGO AS PRSTCDGO_ANTES, d.PRSTCDGO AS PRSTCDGO_DESPUES, SYSDATE AS RESPALDADO
  FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
 WHERE g.PRSTCDGO IS NULL;
-- 1a. Control del respaldo: debe tener tantas filas como nulos con cuota (0a, menos 0b).
SELECT COUNT(*) AS filas_respaldadas FROM CRD.BK_PGPR_PRSTCDGO_20261125;

-- BLOQUE 2 — Corrección (ESCRIBE; sin commit). Solo los nulos.
UPDATE CRD.PGPR g
   SET g.PRSTCDGO = (SELECT d.PRSTCDGO FROM CRD.DTPR d WHERE d.DTPRCDGO = g.DTPRCDGO)
 WHERE g.PRSTCDGO IS NULL
   AND g.PGPRCDGO IN (SELECT b.PGPRCDGO FROM CRD.BK_PGPR_PRSTCDGO_20261125 b);
-- Filas actualizadas: debe ser igual a filas_respaldadas (1a).

-- BLOQUE 3 — Controles posteriores (dentro de la misma sesión, antes de COMMIT).
-- 3a. Nulos restantes: debe ser 0 salvo los de 0b. Los DISTINTOS deben seguir iguales que en 0a.
SELECT CASE WHEN g.PRSTCDGO IS NULL THEN 'NULO' WHEN g.PRSTCDGO <> d.PRSTCDGO THEN 'DISTINTO' ELSE 'CORRECTO' END AS situacion,
       COUNT(*) AS pagos, SUM(CASE WHEN NVL(g.PGPRANUL, 0) = 0 THEN 1 ELSE 0 END) AS vigentes
  FROM CRD.PGPR g LEFT JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
 GROUP BY CASE WHEN g.PRSTCDGO IS NULL THEN 'NULO' WHEN g.PRSTCDGO <> d.PRSTCDGO THEN 'DISTINTO' ELSE 'CORRECTO' END ORDER BY 1;
-- 3b. Lo cambiado coincide con el respaldo: 0 filas con diferencia.
SELECT COUNT(*) AS filas_con_diferencia
  FROM CRD.PGPR g JOIN CRD.BK_PGPR_PRSTCDGO_20261125 b ON b.PGPRCDGO = g.PGPRCDGO WHERE g.PRSTCDGO IS NULL OR g.PRSTCDGO <> b.PRSTCDGO_DESPUES;
-- COMMIT;   -- ← solo si 3a y 3b son los esperados.
-- ROLLBACK; -- ← si algo no cuadra.

-- BLOQUE 4 — PGPR con PRSTCDGO DISTINTO del de su cuota (NO se corrigen: revisar a mano; puede ser un pago mal asignado de verdad).
SELECT g.PGPRCDGO AS pgpr, g.PRSTCDGO AS prestamo_del_pago, d.PRSTCDGO AS prestamo_de_la_cuota, d.DTPRNMCT AS cuota, TRUNC(g.PGPRFCHA) AS fecha,
       g.PGPRVLRR AS valor, g.PGPRTPOO AS tipo, g.PGPRANUL AS anulado, g.PGPRFCRG AS registrado, SUBSTR(g.PGPROBSR, 1, 80) AS observacion
  FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
 WHERE g.PRSTCDGO IS NOT NULL AND g.PRSTCDGO <> d.PRSTCDGO
 ORDER BY g.PGPRCDGO;

-- =====================================================================================
-- REVERSO (comentado; solo si ya se hizo COMMIT y hay que deshacer): devuelve los PRSTCDGO a nulo desde el respaldo.
-- UPDATE CRD.PGPR g SET g.PRSTCDGO = NULL WHERE g.PGPRCDGO IN (SELECT b.PGPRCDGO FROM CRD.BK_PGPR_PRSTCDGO_20261125 b WHERE b.PRSTCDGO_ANTES IS NULL);
-- -- verificar: SELECT COUNT(*) FROM CRD.PGPR WHERE PRSTCDGO IS NULL;  (debe volver al número de 0a)
-- -- el respaldo se conserva; borrarlo (DROP TABLE CRD.BK_PGPR_PRSTCDGO_20261125) solo cuando el usuario lo decida.
-- =====================================================================================

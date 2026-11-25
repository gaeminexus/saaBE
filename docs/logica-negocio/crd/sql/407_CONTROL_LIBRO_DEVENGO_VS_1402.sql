-- =====================================================================================
-- 407 — CONTROL (solo SELECT): el libro por cuota del devengo contra el movimiento de la 1.4.02 de los mismos asientos (lote 19, regla de oro)
-- Equipo omen-saa-1 · 2026-11-25 · NO está ejecutado. Requiere el 406 y que el ④ y los reversos ya escriban los tipos 11 y 12.
--
-- Regla: por asiento, Σ(devengo − reverso) del libro = movimiento de la 1.4.02 del asiento, al centavo. En el asiento del ④ la línea de interés
-- y la de mora comparten cuenta, así que el libro de ese asiento es interés (11 − 12) + mora (5 − 6). Parámetros: :empresa (PJRQCDGO).
-- OJO: la cuenta 1.4.02 es dato de este control (el prefijo de abajo), no del código de la aplicación.
-- =====================================================================================

-- BLOQUE 1 — Por asiento: libro contra 1.4.02. Esperado: DIFERENCIA = 0,00 en todas las filas.
SELECT l.ASNTCDGO,
       l.MES,
       ROUND(l.LIBRO, 2)                          AS LIBRO,
       ROUND(NVL(c.MOVIMIENTO_1402, 0), 2)        AS MOV_1402,
       ROUND(l.LIBRO - NVL(c.MOVIMIENTO_1402, 0), 2) AS DIFERENCIA
  FROM (SELECT m.ASNTCDGO, TO_CHAR(m.MVICFCCT, 'YYYY-MM') AS MES,
               SUM(CASE WHEN m.MVICTPMV IN (5, 11) THEN m.MVICVLOR WHEN m.MVICTPMV IN (6, 12) THEN -m.MVICVLOR ELSE 0 END) AS LIBRO
          FROM CRD.MVIC m
         WHERE m.MVICANUL = 0 AND m.MVICTPMV IN (5, 6, 11, 12) AND m.ASNTCDGO IS NOT NULL
         GROUP BY m.ASNTCDGO, TO_CHAR(m.MVICFCCT, 'YYYY-MM')) l
  LEFT JOIN (SELECT d.ASNTCDGO, SUM(NVL(d.DTASDBEE, 0) - NVL(d.DTASHBRR, 0)) AS MOVIMIENTO_1402
               FROM CNT.DTAS d
               JOIN CNT.ASNT s ON s.ASNTCDGO = d.ASNTCDGO
              WHERE s.PJRQCDGO = :empresa AND s.ASNTESTD = 1 AND d.DTASCNTA LIKE '1.4.02%'
              GROUP BY d.ASNTCDGO) c ON c.ASNTCDGO = l.ASNTCDGO
 ORDER BY l.MES, l.ASNTCDGO;

-- BLOQUE 2 — Por mes y tipo de préstamo del libro (interés y mora por separado): Σ(11 − 12) y Σ(5 − 6). Se cruza a mano contra la 1.4.02 por tipo del mes.
SELECT TO_CHAR(m.MVICFCCT, 'YYYY-MM') AS MES, pr.TPPRCDGO AS TIPO_PRESTAMO,
       ROUND(SUM(CASE WHEN m.MVICTPMV = 11 THEN m.MVICVLOR WHEN m.MVICTPMV = 12 THEN -m.MVICVLOR ELSE 0 END), 2) AS INTERES,
       ROUND(SUM(CASE WHEN m.MVICTPMV = 5  THEN m.MVICVLOR WHEN m.MVICTPMV = 6  THEN -m.MVICVLOR ELSE 0 END), 2) AS MORA
  FROM CRD.MVIC m
  JOIN CRD.PRST p  ON p.PRSTCDGO = m.PRSTCDGO
  JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
 WHERE m.MVICANUL = 0 AND m.MVICTPMV IN (5, 6, 11, 12)
 GROUP BY TO_CHAR(m.MVICFCCT, 'YYYY-MM'), pr.TPPRCDGO
 ORDER BY 1, 2;

-- BLOQUE 3 — Saldo del libro por tipo de préstamo al corte :corte (devengo − reverso de interés, todas las cuotas). Se cruza contra el saldo de la 1.4.02 por tipo.
SELECT pr.TPPRCDGO AS TIPO_PRESTAMO,
       ROUND(SUM(CASE WHEN m.MVICTPMV = 11 THEN m.MVICVLOR WHEN m.MVICTPMV = 12 THEN -m.MVICVLOR ELSE 0 END), 2) AS SALDO_INTERES_LIBRO
  FROM CRD.MVIC m
  JOIN CRD.PRST p  ON p.PRSTCDGO = m.PRSTCDGO
  JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
 WHERE m.MVICANUL = 0 AND m.MVICTPMV IN (11, 12) AND m.MVICFCCT <= :corte
 GROUP BY pr.TPPRCDGO ORDER BY 1;

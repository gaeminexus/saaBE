-- =====================================================================================
-- 408 — Comparación (solo SELECT): el devengo de INTERÉS por cuota contra el agregado por tipo (lote 20, ④ y ④b)
-- Equipo omen-saa-1 · 2026-11-25 · NO está ejecutado. Para correrlo en una COPIA, antes del EAR.
--
-- El ④ con el devengo completo y el ④b dejan de armar el asiento con una consulta agregada por tipo de préstamo y pasan a una consulta POR CUOTA
-- (misma expresión, sin SUM ni GROUP BY); el asiento es la suma de las cuotas redondeadas a 2 decimales y de ESA lista sale el MVIC tipo 11.
-- Aquí cada bloque reproduce, con las constantes de la aplicación ya expandidas, la expresión por cuota y compara por tipo de préstamo:
--   AGREGADO  = Σ valor sin redondear (lo que daba la consulta agregada de siempre)
--   POR_CUOTA = Σ ROUND(valor, 2) (lo que graba el lote 20)
--   DIFERENCIA = POR_CUOTA − AGREGADO; ESPERADO: centavos (como máximo del orden de la cantidad de cuotas × 0,005), nunca más.
-- Constantes expandidas: cuota PAGADA 4, CANCELADA_ANTICIPADA 7, ANULADA 9; préstamos cancelados 3,4,5,9,13; fuera de cartera 0,1,6,7,12,14;
-- declaración PLVN en estado 1 o 2.
-- Parámetros: :desde y :hasta (primer y último día del mes que abre), :corte (último día del mes que cierra), :ancla (corte de la corrida de arranque),
-- :inicioMes (primer día del mes del corte, para el ④b). :corteSig = :corte + 1.
-- =====================================================================================

-- BLOQUE 1 — ④ con el devengo completo: por tipo de préstamo.
WITH pg AS (
    SELECT DTPRCDGO,
           SUM(CASE WHEN LEAST(PGPRFCHA, NVL(PGPRFCRG, PGPRFCHA)) < :ancla THEN NVL(PGPRINPG, 0) ELSE 0 END) pre,
           SUM(CASE WHEN LEAST(PGPRFCHA, NVL(PGPRFCRG, PGPRFCHA)) >= :ancla AND LEAST(PGPRFCHA, NVL(PGPRFCRG, PGPRFCHA)) < :corte + 1 THEN NVL(PGPRINPG, 0) ELSE 0 END) post
      FROM CRD.PGPR
     WHERE (PGPRANUL IS NULL OR PGPRANUL = 0)
     GROUP BY DTPRCDGO
), x AS (
    SELECT d.DTPRCDGO, p.PRSTCDGO, pr.TPPRCDGO,
           GREATEST(
             CASE WHEN ( (NVL(p.PRSTIDST, 0) IN (3, 4, 5, 9, 13)
                          AND NOT EXISTS (SELECT 1 FROM CRD.DTPR cx WHERE cx.PRSTCDGO = p.PRSTCDGO AND NVL(cx.DTPRESTD, 0) NOT IN (4, 7, 9))
                          AND NOT EXISTS (SELECT 1 FROM CRD.DTPR cy WHERE cy.PRSTCDGO = p.PRSTCDGO AND cy.DTPRESTD = 4 AND cy.DTPRFCPG >= (:corte + 1) - 1)
                          AND NOT EXISTS (SELECT 1 FROM CRD.PGPR cz WHERE cz.PRSTCDGO = p.PRSTCDGO AND NVL(cz.PGPRANUL, 0) = 0
                                          AND LEAST(cz.PGPRFCHA, NVL(cz.PGPRFCRG, cz.PGPRFCHA)) >= (:corte + 1)))
                         OR NVL(d.DTPRESTD, 0) = 7 )
                  THEN NVL(g.post, 0)
                  WHEN d.DTPRFCVN > (SELECT MIN(v.PLVNFCCR) FROM CRD.PLVN v
                                      WHERE v.PRSTCDGO = p.PRSTCDGO AND v.PLVNESTD IN (1, 2) AND v.PLVNFCCR < (:corte + 1))
                  THEN 0
                  ELSE GREATEST(NVL(d.DTPRINTR, 0) - NVL(g.pre, 0), 0) END,
             NVL(g.post, 0)) AS VALOR
      FROM CRD.DTPR d
      JOIN CRD.PRST p  ON p.PRSTCDGO = d.PRSTCDGO
      JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
      LEFT JOIN pg g   ON g.DTPRCDGO = d.DTPRCDGO
     WHERE p.PRSTFCHA < (:corte + 1) AND NVL(p.PRSTIDST, 0) NOT IN (0, 1, 6, 7, 12, 14)
       AND NVL(d.DTPRESTD, 0) <> 9
       AND TRUNC(d.DTPRFCVN) BETWEEN :desde AND :hasta
       AND pr.TPPRCDGO IS NOT NULL
)
SELECT TPPRCDGO AS TIPO_PRESTAMO,
       COUNT(CASE WHEN VALOR > 0 THEN 1 END)               AS CUOTAS,
       ROUND(SUM(VALOR), 4)                                AS AGREGADO,
       SUM(CASE WHEN VALOR > 0 THEN ROUND(VALOR, 2) ELSE 0 END) AS POR_CUOTA,
       ROUND(SUM(CASE WHEN VALOR > 0 THEN ROUND(VALOR, 2) ELSE 0 END) - SUM(VALOR), 4) AS DIFERENCIA
  FROM x
 GROUP BY TPPRCDGO
 ORDER BY TPPRCDGO;

-- BLOQUE 2 — ④b primera cuota de los préstamos concedidos en el mes: por tipo de préstamo.
WITH pg AS (
    SELECT DTPRCDGO, SUM(NVL(PGPRINPG, 0)) post
      FROM CRD.PGPR
     WHERE (PGPRANUL IS NULL OR PGPRANUL = 0) AND LEAST(PGPRFCHA, NVL(PGPRFCRG, PGPRFCHA)) < :corte + 1
     GROUP BY DTPRCDGO
), x AS (
    SELECT d.DTPRCDGO, p.PRSTCDGO, pr.TPPRCDGO,
           GREATEST(
             CASE WHEN ( (NVL(p.PRSTIDST, 0) IN (3, 4, 5, 9, 13)
                          AND NOT EXISTS (SELECT 1 FROM CRD.DTPR cx WHERE cx.PRSTCDGO = p.PRSTCDGO AND NVL(cx.DTPRESTD, 0) NOT IN (4, 7, 9))
                          AND NOT EXISTS (SELECT 1 FROM CRD.DTPR cy WHERE cy.PRSTCDGO = p.PRSTCDGO AND cy.DTPRESTD = 4 AND cy.DTPRFCPG >= (:corte + 1) - 1)
                          AND NOT EXISTS (SELECT 1 FROM CRD.PGPR cz WHERE cz.PRSTCDGO = p.PRSTCDGO AND NVL(cz.PGPRANUL, 0) = 0
                                          AND LEAST(cz.PGPRFCHA, NVL(cz.PGPRFCRG, cz.PGPRFCHA)) >= (:corte + 1)))
                         OR NVL(d.DTPRESTD, 0) = 7 )
                  THEN NVL(g.post, 0)
                  WHEN d.DTPRFCVN > (SELECT MIN(v.PLVNFCCR) FROM CRD.PLVN v
                                      WHERE v.PRSTCDGO = p.PRSTCDGO AND v.PLVNESTD IN (1, 2) AND v.PLVNFCCR < (:corte + 1))
                  THEN 0
                  ELSE NVL(d.DTPRINTR, 0) END,
             NVL(g.post, 0)) AS VALOR
      FROM CRD.DTPR d
      JOIN CRD.PRST p  ON p.PRSTCDGO = d.PRSTCDGO
      JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
      LEFT JOIN pg g   ON g.DTPRCDGO = d.DTPRCDGO
     WHERE p.PRSTFCHA < (:corte + 1) AND NVL(p.PRSTIDST, 0) NOT IN (0, 1, 6, 7, 12, 14)
       AND p.PRSTFCHA >= :inicioMes
       AND NVL(d.DTPRESTD, 0) <> 9
       AND TRUNC(d.DTPRFCVN) BETWEEN :inicioMes AND :corte
       AND pr.TPPRCDGO IS NOT NULL
)
SELECT TPPRCDGO AS TIPO_PRESTAMO,
       COUNT(CASE WHEN VALOR > 0 THEN 1 END)               AS CUOTAS,
       ROUND(SUM(VALOR), 4)                                AS AGREGADO,
       SUM(CASE WHEN VALOR > 0 THEN ROUND(VALOR, 2) ELSE 0 END) AS POR_CUOTA,
       ROUND(SUM(CASE WHEN VALOR > 0 THEN ROUND(VALOR, 2) ELSE 0 END) - SUM(VALOR), 4) AS DIFERENCIA
  FROM x
 GROUP BY TPPRCDGO
 ORDER BY TPPRCDGO;

-- BLOQUE 3 — Después de un cierre ejecutado con el lote 20 (y el 406): lo grabado en el libro contra el asiento de la corrida :idCorrida (④ = subproceso 5, ④b = 11).
-- El ④ lleva interés y mora en las mismas cuentas, así que se compara (MVIC 11 + MVIC 5) contra el DEBE de la 1.4.02. ESPERADO: DIFERENCIA = 0,00.
SELECT a.ANCCTPOO AS SUBPROCESO, a.ANCCASNT AS ASIENTO,
       ROUND(NVL((SELECT SUM(m.MVICVLOR) FROM CRD.MVIC m WHERE m.ASNTCDGO = a.ANCCASNT AND m.MVICTPMV IN (5, 11) AND m.MVICANUL = 0), 0), 2) AS LIBRO,
       ROUND(NVL((SELECT SUM(NVL(d.DTASDBEE, 0)) FROM CNT.DTAS d WHERE d.ASNTCDGO = a.ANCCASNT AND d.DTASCNTA LIKE '1.4.02%'), 0), 2) AS DEBE_1402,
       ROUND(NVL((SELECT SUM(m.MVICVLOR) FROM CRD.MVIC m WHERE m.ASNTCDGO = a.ANCCASNT AND m.MVICTPMV IN (5, 11) AND m.MVICANUL = 0), 0)
           - NVL((SELECT SUM(NVL(d.DTASDBEE, 0)) FROM CNT.DTAS d WHERE d.ASNTCDGO = a.ANCCASNT AND d.DTASCNTA LIKE '1.4.02%'), 0), 2) AS DIFERENCIA
  FROM CRD.ANCC a
 WHERE a.CRCTCDGO = :idCorrida AND a.ANCCTPOO IN (5, 11)
 ORDER BY a.ANCCTPOO;

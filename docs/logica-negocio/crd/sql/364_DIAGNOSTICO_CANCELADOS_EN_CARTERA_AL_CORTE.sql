-- =====================================================================================================
-- 364 — DIAGNÓSTICO (SOLO LECTURA): préstamos CANCELADOS que el cierre «a fecha de corte» cuenta como cartera
-- omen-saa-1-arb · 2026-11-05
--
-- Síntoma: cierre de octubre 2026 (al corte 31-10, saa.cartera.alCorte=siempre en producción) deja saldos
-- altos en «más de 360 días» de prendarios vencidos y de quirografarios de novación, cuando los novados
-- están todos cancelados. Avisos ⑨: Q −14,6 M, H −3,5 M, P −129.306,93 (contabilidad − módulo).
--
-- Causa en el código (CierreCarteraDaoServiceImpl, modo al corte):
--   universo  = PRSTFCHA <= corte y PRSTIDST NOT IN (0,1,6,7,12)   -> ENTRAN los cancelados 3,4,5,9,13
--   pendiente = la cuota NO está (DTPRESTD IN (4 PAGADA, 7 CANC. ANTICIPADA) con DTPRFCPG <= corte)
--   capital   = DTPRCPTL − pagos PGPR vigentes con fecha <= corte
-- Un préstamo cancelado cuyas cuotas no quedaron en 4/7 con fecha de pago (novación, migración,
-- cancelación por dato) y sin PGPR que las cubra, vuelve a la cartera con TODO su capital, envejecido
-- al corte: cae en «más de 360 días».
--
-- No modifica nada. Correr bloque por bloque.
-- =====================================================================================================

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 1 — Resumen: capital que el modo al corte cuenta, por estado del PRÉSTAMO.
-- Esperado si la hipótesis es cierta: filas con PRSTIDST 3/4/5/9/13 con capital grande.
-- ---------------------------------------------------------------------------------------------------
SELECT p.PRSTIDST                                              AS ESTADO_PRESTAMO,
       COUNT(DISTINCT p.PRSTCDGO)                              AS PRESTAMOS,
       COUNT(*)                                                AS CUOTAS,
       ROUND(SUM(GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0)), 2) AS CAPITAL_AL_CORTE
FROM   CRD.DTPR d
JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
LEFT JOIN (SELECT DTPRCDGO,
                  SUM(CASE WHEN PGPRTPOO = 'ABONO_CAPITAL' THEN NVL(PGPRCPPG,0)
                           WHEN NVL(PGPRSLOT,0) > 0 THEN PGPRSLOT ELSE NVL(PGPRCPPG,0) END) cap
           FROM   CRD.PGPR
           WHERE  (PGPRANUL IS NULL OR PGPRANUL = 0) AND PGPRFCHA < DATE '2026-11-01'
           GROUP BY DTPRCDGO) g ON g.DTPRCDGO = d.DTPRCDGO
WHERE  p.PRSTFCHA < DATE '2026-11-01'
AND    NVL(p.PRSTIDST,0) NOT IN (0, 1, 6, 7, 12)
AND    NOT (NVL(d.DTPRESTD,0) IN (4, 7) AND NVL(d.DTPRFCPG, DATE '2026-11-01') < DATE '2026-11-01')
AND    d.DTPRFCVN IS NOT NULL
AND    p.PRDCCDGO IS NOT NULL
AND    GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0) > 0.004
GROUP BY p.PRSTIDST
ORDER BY p.PRSTIDST;

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 2 — Solo los CANCELADOS (3,4,5,9,13): por producto, con el estado de sus cuotas «pendientes».
-- Muestra en qué estado quedaron las cuotas (DTPRESTD) y si tienen fecha de pago (DTPRFCPG).
-- ---------------------------------------------------------------------------------------------------
SELECT p.PRDCCDGO, pr.PRDCNMBR, p.PRSTIDST AS ESTADO_PRESTAMO, d.DTPRESTD AS ESTADO_CUOTA,
       CASE WHEN d.DTPRFCPG IS NULL THEN 'SIN FECHA PAGO' ELSE 'CON FECHA PAGO' END AS FECHA_PAGO,
       COUNT(DISTINCT p.PRSTCDGO) AS PRESTAMOS, COUNT(*) AS CUOTAS,
       ROUND(SUM(GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0)), 2) AS CAPITAL_AL_CORTE
FROM   CRD.DTPR d
JOIN   CRD.PRST p  ON p.PRSTCDGO = d.PRSTCDGO
LEFT JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
LEFT JOIN (SELECT DTPRCDGO,
                  SUM(CASE WHEN PGPRTPOO = 'ABONO_CAPITAL' THEN NVL(PGPRCPPG,0)
                           WHEN NVL(PGPRSLOT,0) > 0 THEN PGPRSLOT ELSE NVL(PGPRCPPG,0) END) cap
           FROM   CRD.PGPR
           WHERE  (PGPRANUL IS NULL OR PGPRANUL = 0) AND PGPRFCHA < DATE '2026-11-01'
           GROUP BY DTPRCDGO) g ON g.DTPRCDGO = d.DTPRCDGO
WHERE  p.PRSTFCHA < DATE '2026-11-01'
AND    p.PRSTIDST IN (3, 4, 5, 9, 13)
AND    NOT (NVL(d.DTPRESTD,0) IN (4, 7) AND NVL(d.DTPRFCPG, DATE '2026-11-01') < DATE '2026-11-01')
AND    d.DTPRFCVN IS NOT NULL
AND    p.PRDCCDGO IS NOT NULL
AND    GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0) > 0.004
GROUP BY p.PRDCCDGO, pr.PRDCNMBR, p.PRSTIDST, d.DTPRESTD,
         CASE WHEN d.DTPRFCPG IS NULL THEN 'SIN FECHA PAGO' ELSE 'CON FECHA PAGO' END
ORDER BY CAPITAL_AL_CORTE DESC;

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 3 — Los 30 préstamos cancelados que más capital meten (para revisar uno a mano).
-- ---------------------------------------------------------------------------------------------------
SELECT * FROM (
  SELECT p.PRSTCDGO, p.PRDCCDGO, p.PRSTIDST, TRUNC(p.PRSTFCHA) AS FECHA_PRESTAMO,
         COUNT(*) AS CUOTAS_PENDIENTES_AL_CORTE,
         ROUND(SUM(GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0)), 2) AS CAPITAL_AL_CORTE,
         MIN(TRUNC(d.DTPRFCVN)) AS PRIMER_VTO
  FROM   CRD.DTPR d
  JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
  LEFT JOIN (SELECT DTPRCDGO,
                    SUM(CASE WHEN PGPRTPOO = 'ABONO_CAPITAL' THEN NVL(PGPRCPPG,0)
                             WHEN NVL(PGPRSLOT,0) > 0 THEN PGPRSLOT ELSE NVL(PGPRCPPG,0) END) cap
             FROM   CRD.PGPR
             WHERE  (PGPRANUL IS NULL OR PGPRANUL = 0) AND PGPRFCHA < DATE '2026-11-01'
             GROUP BY DTPRCDGO) g ON g.DTPRCDGO = d.DTPRCDGO
  WHERE  p.PRSTFCHA < DATE '2026-11-01'
  AND    p.PRSTIDST IN (3, 4, 5, 9, 13)
  AND    NOT (NVL(d.DTPRESTD,0) IN (4, 7) AND NVL(d.DTPRFCPG, DATE '2026-11-01') < DATE '2026-11-01')
  AND    d.DTPRFCVN IS NOT NULL
  AND    p.PRDCCDGO IS NOT NULL
  AND    GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0) > 0.004
  GROUP BY p.PRSTCDGO, p.PRDCCDGO, p.PRSTIDST, TRUNC(p.PRSTFCHA)
  ORDER BY CAPITAL_AL_CORTE DESC
) WHERE ROWNUM <= 30;

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 4 — Lo que el cierre contabilizó: asientos del cierre de octubre (para saber qué revertir).
-- Ajustar el filtro de glosa si el rótulo difiere.
-- ---------------------------------------------------------------------------------------------------
SELECT a.ASNTCDGO, a.ASNTNMAL, TRUNC(a.ASNTFCHA) AS FECHA, a.ASNTESTD, a.MYRZCDGO,
       SUBSTR(a.ASNTOBSR, 1, 90) AS GLOSA
FROM   CNT.ASNT a
WHERE  a.ASNTFCHA >= DATE '2026-10-31'
AND    (UPPER(a.ASNTOBSR) LIKE '%CIERRE DE CARTERA 2026-10%'
        OR UPPER(a.ASNTOBSR) LIKE '%AJUSTE DE BANDAS A SALDO 2026-10%'
        OR UPPER(a.ASNTOBSR) LIKE '%2026-10%CARTERA%')
ORDER BY a.ASNTCDGO;

-- =====================================================================================================
-- 365 — DIAGNÓSTICO (SOLO LECTURA): ¿qué fecha de cancelación se puede deducir de los préstamos cancelados
--        que el cierre «a fecha de corte» revive? (complemento del 364)
-- omen-saa-1-arb · 2026-11-05
--
-- CRD.PRST no guarda la fecha de cancelación. El arreglo de fondo tiene que:
--   * seguir contando un préstamo cancelado DESPUÉS del corte (lo necesita el reproceso de meses viejos);
--   * dejar fuera el cancelado ANTES del corte.
-- Este script mide qué evidencia de fecha tiene cada uno, para elegir la regla con datos y no a ojo:
--   F_SUSTITUTO : fecha del préstamo nuevo que lo sustituye (PRSTPROR = este préstamo; novación/reestructuración)
--   F_ULT_CUOTA : última DTPRFCPG de sus cuotas
--   F_ULT_PAGO  : último PGPRFCHA vigente
--   F_ULT_EVENTO: último EVPRFCHA vigente
--   F_MODIFICA  : PRSTFCMD (cualquier modificación; la menos confiable)
-- No modifica nada. Correr bloque por bloque.
-- =====================================================================================================

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 1 — Resumen por estado: cuántos tienen cada evidencia, y cuántos tienen alguna fecha > 31-10.
-- ---------------------------------------------------------------------------------------------------
WITH pagos AS (
  SELECT DTPRCDGO,
         SUM(CASE WHEN PGPRTPOO = 'ABONO_CAPITAL' THEN NVL(PGPRCPPG,0)
                  WHEN NVL(PGPRSLOT,0) > 0 THEN PGPRSLOT ELSE NVL(PGPRCPPG,0) END) cap
  FROM   CRD.PGPR
  WHERE  (PGPRANUL IS NULL OR PGPRANUL = 0) AND PGPRFCHA < DATE '2026-11-01'
  GROUP BY DTPRCDGO),
revividos AS (
  SELECT p.PRSTCDGO, p.PRSTIDST, p.PRDCCDGO,
         ROUND(SUM(GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0)), 2) AS CAPITAL_AL_CORTE
  FROM   CRD.DTPR d
  JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
  LEFT JOIN pagos g ON g.DTPRCDGO = d.DTPRCDGO
  WHERE  p.PRSTFCHA < DATE '2026-11-01'
  AND    p.PRSTIDST IN (3, 4, 5, 9, 13)
  AND    NOT (NVL(d.DTPRESTD,0) IN (4, 7) AND NVL(d.DTPRFCPG, DATE '2026-11-01') < DATE '2026-11-01')
  AND    d.DTPRFCVN IS NOT NULL
  AND    p.PRDCCDGO IS NOT NULL
  AND    GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0) > 0.004
  GROUP BY p.PRSTCDGO, p.PRSTIDST, p.PRDCCDGO),
evidencia AS (
  SELECT r.*,
         (SELECT MIN(TRUNC(n.PRSTFCHA)) FROM CRD.PRST n WHERE n.PRSTPROR = r.PRSTCDGO
                                                         AND NVL(n.PRSTIDST,0) NOT IN (0,1,6,7,12)) AS F_SUSTITUTO,
         (SELECT MAX(TRUNC(d.DTPRFCPG)) FROM CRD.DTPR d WHERE d.PRSTCDGO = r.PRSTCDGO)            AS F_ULT_CUOTA,
         (SELECT MAX(TRUNC(g.PGPRFCHA)) FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
           WHERE d.PRSTCDGO = r.PRSTCDGO AND (g.PGPRANUL IS NULL OR g.PGPRANUL = 0))              AS F_ULT_PAGO,
         (SELECT MAX(TRUNC(e.EVPRFCHA)) FROM CRD.EVPR e WHERE e.PRSTCDGO = r.PRSTCDGO
                                                         AND e.EVPRFCAN IS NULL)                  AS F_ULT_EVENTO,
         (SELECT TRUNC(p.PRSTFCMD) FROM CRD.PRST p WHERE p.PRSTCDGO = r.PRSTCDGO)                 AS F_MODIFICA
  FROM revividos r)
SELECT PRSTIDST                                    AS ESTADO,
       COUNT(*)                                    AS PRESTAMOS,
       ROUND(SUM(CAPITAL_AL_CORTE), 2)             AS CAPITAL,
       COUNT(F_SUSTITUTO)                          AS CON_SUSTITUTO,
       COUNT(F_ULT_CUOTA)                          AS CON_FECHA_CUOTA,
       COUNT(F_ULT_PAGO)                           AS CON_PAGO,
       COUNT(F_ULT_EVENTO)                         AS CON_EVENTO,
       COUNT(F_MODIFICA)                           AS CON_FCMD,
       SUM(CASE WHEN COALESCE(F_SUSTITUTO, F_ULT_CUOTA, F_ULT_PAGO, F_ULT_EVENTO) IS NULL THEN 1 ELSE 0 END)
                                                   AS SIN_NINGUNA_FECHA,
       SUM(CASE WHEN GREATEST(NVL(F_SUSTITUTO, DATE '1900-01-01'), NVL(F_ULT_CUOTA, DATE '1900-01-01'),
                              NVL(F_ULT_PAGO, DATE '1900-01-01'), NVL(F_ULT_EVENTO, DATE '1900-01-01'))
                     > DATE '2026-10-31' THEN 1 ELSE 0 END)
                                                   AS ALGUNA_FECHA_DESPUES_CORTE,
       ROUND(SUM(CASE WHEN GREATEST(NVL(F_SUSTITUTO, DATE '1900-01-01'), NVL(F_ULT_CUOTA, DATE '1900-01-01'),
                              NVL(F_ULT_PAGO, DATE '1900-01-01'), NVL(F_ULT_EVENTO, DATE '1900-01-01'))
                     > DATE '2026-10-31' THEN CAPITAL_AL_CORTE ELSE 0 END), 2)
                                                   AS CAPITAL_FECHA_DESPUES_CORTE
FROM   evidencia
GROUP BY PRSTIDST
ORDER BY PRSTIDST;

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 2 — Detalle de los 40 que más capital meten, con todas sus fechas.
-- ---------------------------------------------------------------------------------------------------
WITH pagos AS (
  SELECT DTPRCDGO,
         SUM(CASE WHEN PGPRTPOO = 'ABONO_CAPITAL' THEN NVL(PGPRCPPG,0)
                  WHEN NVL(PGPRSLOT,0) > 0 THEN PGPRSLOT ELSE NVL(PGPRCPPG,0) END) cap
  FROM   CRD.PGPR
  WHERE  (PGPRANUL IS NULL OR PGPRANUL = 0) AND PGPRFCHA < DATE '2026-11-01'
  GROUP BY DTPRCDGO),
revividos AS (
  SELECT p.PRSTCDGO, p.PRSTIDST, p.PRDCCDGO, TRUNC(p.PRSTFCHA) AS F_PRESTAMO,
         ROUND(SUM(GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0)), 2) AS CAPITAL_AL_CORTE
  FROM   CRD.DTPR d
  JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
  LEFT JOIN pagos g ON g.DTPRCDGO = d.DTPRCDGO
  WHERE  p.PRSTFCHA < DATE '2026-11-01'
  AND    p.PRSTIDST IN (3, 4, 5, 9, 13)
  AND    NOT (NVL(d.DTPRESTD,0) IN (4, 7) AND NVL(d.DTPRFCPG, DATE '2026-11-01') < DATE '2026-11-01')
  AND    d.DTPRFCVN IS NOT NULL
  AND    p.PRDCCDGO IS NOT NULL
  AND    GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0) > 0.004
  GROUP BY p.PRSTCDGO, p.PRSTIDST, p.PRDCCDGO, TRUNC(p.PRSTFCHA))
SELECT * FROM (
  SELECT r.*,
         (SELECT MIN(n.PRSTCDGO) FROM CRD.PRST n WHERE n.PRSTPROR = r.PRSTCDGO)                   AS SUSTITUTO,
         (SELECT MIN(TRUNC(n.PRSTFCHA)) FROM CRD.PRST n WHERE n.PRSTPROR = r.PRSTCDGO
                                                         AND NVL(n.PRSTIDST,0) NOT IN (0,1,6,7,12)) AS F_SUSTITUTO,
         (SELECT MAX(TRUNC(d.DTPRFCPG)) FROM CRD.DTPR d WHERE d.PRSTCDGO = r.PRSTCDGO)            AS F_ULT_CUOTA,
         (SELECT MAX(TRUNC(g.PGPRFCHA)) FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
           WHERE d.PRSTCDGO = r.PRSTCDGO AND (g.PGPRANUL IS NULL OR g.PGPRANUL = 0))              AS F_ULT_PAGO,
         (SELECT MAX(TRUNC(e.EVPRFCHA)) FROM CRD.EVPR e WHERE e.PRSTCDGO = r.PRSTCDGO
                                                         AND e.EVPRFCAN IS NULL)                  AS F_ULT_EVENTO,
         (SELECT TRUNC(p.PRSTFCMD) FROM CRD.PRST p WHERE p.PRSTCDGO = r.PRSTCDGO)                 AS F_MODIFICA,
         (SELECT LISTAGG(x.ESTD, ',') WITHIN GROUP (ORDER BY x.ESTD)
            FROM (SELECT DISTINCT NVL(TO_CHAR(d.DTPRESTD),'NULL') ESTD
                    FROM CRD.DTPR d WHERE d.PRSTCDGO = r.PRSTCDGO) x)                            AS ESTADOS_CUOTAS
  FROM revividos r
  ORDER BY r.CAPITAL_AL_CORTE DESC
) WHERE ROWNUM <= 40;

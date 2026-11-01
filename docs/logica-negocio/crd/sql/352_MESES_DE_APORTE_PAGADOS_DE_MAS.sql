-- =====================================================================================
-- 352 — SOLO LECTURA. omen-saa-1, 2026-11-01.
-- El 351 mostró que el reparto por devengo de las cargas mandó a junio 2026 aportes LEGÍTIMOS (pares
-- 448 + 449 que completan junio exacto), salvo 3 casos donde junio quedó pagado DE MÁS:
-- rol 657 (doble), 9835 (triple) y 43568 (doble), todos de la carga 448. El reparto solo llena cada
-- mes hasta lo que falta, así que eso no debería pasar.
-- Este script busca, en TODA la cartera y TODOS los meses desde 2025-06, los meses de aporte (por
-- período efectivo) que suman más que el esperado de la vigencia vigente, cuando al menos una de las
-- filas la puso una carga (SAA_AH).
-- =====================================================================================

WITH mes AS (
  SELECT a.ENTDCDGO, a.TPAPCDGO,
         (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
               WHEN a.APRTVLRR > 0 THEN TRUNC(a.APRTFCTR, 'MM') END) AS PERIODO,
         SUM(a.APRTVLRR) AS APORTADO,
         SUM(CASE WHEN a.APRTUSRG = 'SAA_AH' THEN 1 ELSE 0 END) AS FILAS_CARGA,
         COUNT(*) AS FILAS,
         LISTAGG(a.APRTUSRG || NVL2(a.APRTIDAS, '#' || a.APRTIDAS, '') || '=' || a.APRTVLRR, ', ')
           WITHIN GROUP (ORDER BY a.APRTCDGO) AS DETALLE
    FROM CRD.APRT a
   WHERE a.TPAPCDGO IN (9, 11)
     AND NVL(a.APRTIDST, 4) <> 0
   GROUP BY a.ENTDCDGO, a.TPAPCDGO,
            (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                  WHEN a.APRTVLRR > 0 THEN TRUNC(a.APRTFCTR, 'MM') END)
),
esperado AS (
  SELECT k.ENTDCDGO, v.TPAPCDGO, v.VGCNFCIN, v.VGCNFCFN, v.VGCNMNTO
    FROM CRD.VGCN v JOIN CRD.CNTR k ON k.CNTRCDGO = v.CNTRCDGO AND k.CNTRESTD = 1
   WHERE v.VGCNIDST = 1
)
SELECT e.ENTDRLPC AS ROL, e.ENTDRZNS, m.TPAPCDGO, TO_CHAR(m.PERIODO, 'YYYY-MM') AS MES,
       x.VGCNMNTO AS ESPERADO, m.APORTADO, ROUND(m.APORTADO - x.VGCNMNTO, 2) AS DE_MAS,
       m.FILAS, m.FILAS_CARGA, m.DETALLE
  FROM mes m
  JOIN CRD.ENTD e ON e.ENTDCDGO = m.ENTDCDGO
  JOIN esperado x ON x.ENTDCDGO = m.ENTDCDGO AND x.TPAPCDGO = m.TPAPCDGO
                 AND x.VGCNFCIN <= LAST_DAY(m.PERIODO)
                 AND (x.VGCNFCFN IS NULL OR x.VGCNFCFN >= LAST_DAY(m.PERIODO))
 WHERE m.PERIODO >= DATE '2025-06-01'
   AND m.FILAS_CARGA > 0
   AND m.APORTADO > x.VGCNMNTO + 0.01
 ORDER BY m.PERIODO, e.ENTDRLPC, m.TPAPCDGO;

-- Resumen por mes: cuántos partícipes y cuánto de más
WITH mes AS (
  SELECT a.ENTDCDGO, a.TPAPCDGO,
         (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
               WHEN a.APRTVLRR > 0 THEN TRUNC(a.APRTFCTR, 'MM') END) AS PERIODO,
         SUM(a.APRTVLRR) AS APORTADO,
         SUM(CASE WHEN a.APRTUSRG = 'SAA_AH' THEN 1 ELSE 0 END) AS FILAS_CARGA
    FROM CRD.APRT a
   WHERE a.TPAPCDGO IN (9, 11) AND NVL(a.APRTIDST, 4) <> 0
   GROUP BY a.ENTDCDGO, a.TPAPCDGO,
            (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                  WHEN a.APRTVLRR > 0 THEN TRUNC(a.APRTFCTR, 'MM') END)
)
SELECT TO_CHAR(m.PERIODO, 'YYYY-MM') AS MES, COUNT(*) AS CASOS, ROUND(SUM(m.APORTADO - v.VGCNMNTO), 2) AS TOTAL_DE_MAS
  FROM mes m
  JOIN CRD.CNTR k ON k.ENTDCDGO = m.ENTDCDGO AND k.CNTRESTD = 1
  JOIN CRD.VGCN v ON v.CNTRCDGO = k.CNTRCDGO AND v.TPAPCDGO = m.TPAPCDGO AND v.VGCNIDST = 1
                 AND v.VGCNFCIN <= LAST_DAY(m.PERIODO)
                 AND (v.VGCNFCFN IS NULL OR v.VGCNFCFN >= LAST_DAY(m.PERIODO))
 WHERE m.PERIODO >= DATE '2025-06-01'
   AND m.FILAS_CARGA > 0
   AND m.APORTADO > v.VGCNMNTO + 0.01
 GROUP BY TO_CHAR(m.PERIODO, 'YYYY-MM')
 ORDER BY 1;

-- =====================================================================================
-- 351 — SOLO LECTURA. omen-saa-1, 2026-11-01. CONTROL 2b del 345: las cargas 448, 449, 450 y 492
-- mandaron aportes al devengo JUNIO 2026. ¿Legítimo (el partícipe de verdad debía junio) o
-- corrimiento (junio ya estaba pagado por otra vía sin devengo y el reparto no lo vio)?
-- Criterio: por cada aporte de carga con devengo 2026-06, cuánto MÁS había en junio de otras fuentes
-- (por período efectivo) y el esperado de la vigencia. Si OTROS_JUNIO + este >= 2 × esperado, junio
-- quedó pagado de más: candidato a corrimiento.
-- =====================================================================================

WITH carga_junio AS (
  SELECT a.APRTCDGO, a.ENTDCDGO, a.TPAPCDGO, a.APRTVLRR, a.APRTIDAS
    FROM CRD.APRT a
   WHERE a.APRTUSRG = 'SAA_AH' AND a.APRTPRDV = DATE '2026-06-01'
     AND a.APRTIDAS IN (448, 449, 450, 492)
)
SELECT c.APRTIDAS AS CARGA, e.ENTDRLPC AS ROL, e.ENTDRZNS, c.TPAPCDGO, c.APRTVLRR AS ESTE_APORTE,
       (SELECT NVL(SUM(o.APRTVLRR), 0) FROM CRD.APRT o
         WHERE o.ENTDCDGO = c.ENTDCDGO AND o.TPAPCDGO = c.TPAPCDGO AND o.APRTCDGO <> c.APRTCDGO
           AND (CASE WHEN o.APRTPRDV IS NOT NULL THEN o.APRTPRDV
                     WHEN o.APRTVLRR > 0 THEN TRUNC(o.APRTFCTR, 'MM') END) = DATE '2026-06-01') AS OTROS_JUNIO,
       (SELECT MAX(v.VGCNMNTO) FROM CRD.VGCN v JOIN CRD.CNTR k ON k.CNTRCDGO = v.CNTRCDGO AND k.CNTRESTD = 1
         WHERE k.ENTDCDGO = c.ENTDCDGO AND v.TPAPCDGO = c.TPAPCDGO AND v.VGCNIDST = 1) AS ESPERADO_MES,
       (SELECT LISTAGG(o.APRTUSRG || ':' || NVL(TO_CHAR(o.APRTPRDV, 'YYYY-MM'), 'caja ' || TO_CHAR(o.APRTFCTR, 'YYYY-MM')), ', ')
               WITHIN GROUP (ORDER BY o.APRTCDGO) FROM CRD.APRT o
         WHERE o.ENTDCDGO = c.ENTDCDGO AND o.TPAPCDGO = c.TPAPCDGO AND o.APRTCDGO <> c.APRTCDGO
           AND (CASE WHEN o.APRTPRDV IS NOT NULL THEN o.APRTPRDV
                     WHEN o.APRTVLRR > 0 THEN TRUNC(o.APRTFCTR, 'MM') END) = DATE '2026-06-01') AS FUENTES_JUNIO
  FROM carga_junio c
  JOIN CRD.ENTD e ON e.ENTDCDGO = c.ENTDCDGO
 ORDER BY c.APRTIDAS, e.ENTDRLPC, c.TPAPCDGO;

-- Resumen: cuántos parecen legítimos (junio solo con este aporte) y cuántos candidatos a corrimiento
WITH carga_junio AS (
  SELECT a.APRTCDGO, a.ENTDCDGO, a.TPAPCDGO, a.APRTVLRR, a.APRTIDAS
    FROM CRD.APRT a
   WHERE a.APRTUSRG = 'SAA_AH' AND a.APRTPRDV = DATE '2026-06-01' AND a.APRTIDAS IN (448, 449, 450, 492)
), x AS (
  SELECT c.APRTIDAS,
         (SELECT NVL(SUM(o.APRTVLRR), 0) FROM CRD.APRT o
           WHERE o.ENTDCDGO = c.ENTDCDGO AND o.TPAPCDGO = c.TPAPCDGO AND o.APRTCDGO <> c.APRTCDGO
             AND (CASE WHEN o.APRTPRDV IS NOT NULL THEN o.APRTPRDV
                       WHEN o.APRTVLRR > 0 THEN TRUNC(o.APRTFCTR, 'MM') END) = DATE '2026-06-01') AS otros
    FROM carga_junio c)
SELECT APRTIDAS AS CARGA, COUNT(*) AS FILAS,
       SUM(CASE WHEN otros < 0.01 THEN 1 ELSE 0 END) AS JUNIO_SOLO_CON_ESTE,
       SUM(CASE WHEN otros >= 0.01 THEN 1 ELSE 0 END) AS JUNIO_CON_OTRAS_FUENTES
  FROM x GROUP BY APRTIDAS ORDER BY 1;

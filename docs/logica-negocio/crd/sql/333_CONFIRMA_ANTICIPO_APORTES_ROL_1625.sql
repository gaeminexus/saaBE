-- =====================================================================================
-- 333 — CONFIRMACIÓN (SOLO LECTURA). Equipo omen-saa-1, 2026-10-30.
--
-- Hipótesis del árbitro sobre la carga 492 / rol 1625 (entidad 4762), a partir del 332:
--   El partícipe debía cesantía (tipo 11, vigencia 2651 de 66,95, sin fecha fin) y Petro le
--   descontó 334,75 = 5 x 66,95 por AH. El proceso repartió 264,25 en los meses pendientes
--   entre 2025-06 y 2026-10 y quiso ANTICIPAR los 70,50 restantes a noviembre/diciembre 2026.
--   No pudo: el esperado en lote (VigenciaContratoServiceImpl.esperadoEnLotePorFilial) solo
--   se arma hasta el mes de la carga, y para una entidad cubierta por el lote un mes posterior
--   da esperado 0 -> sobrante -> novedad "vigencia no cubre", que es FALSA (la vigencia sí
--   cubre). Esto lo confirma si la suma de FALTANTE de abajo da 264,25.
--
-- Mismo cálculo que el proceso: aportado(m, tipo) por PERIODO EFECTIVO
--   (APRTPRDV; si es nulo y el valor es positivo, el mes de APRTFCTR; si no, no cuenta).
-- =====================================================================================

WITH meses AS (
  SELECT ADD_MONTHS(DATE '2025-06-01', LEVEL - 1) AS MES
    FROM DUAL CONNECT BY LEVEL <= 17                      -- 2025-06 .. 2026-10
),
aportado AS (
  SELECT (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
               WHEN a.APRTVLRR > 0 THEN TRUNC(a.APRTFCTR, 'MM') END) AS MES,
         SUM(a.APRTVLRR) AS APORTADO
    FROM CRD.APRT a
   WHERE a.ENTDCDGO = 4762 AND a.TPAPCDGO = 11
   GROUP BY (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                  WHEN a.APRTVLRR > 0 THEN TRUNC(a.APRTFCTR, 'MM') END)
)
SELECT m.MES, 66.95 AS ESPERADO, NVL(x.APORTADO, 0) AS APORTADO,
       GREATEST(66.95 - NVL(x.APORTADO, 0), 0) AS FALTANTE
  FROM meses m
  LEFT JOIN aportado x ON x.MES = m.MES
 ORDER BY m.MES;

-- Total de lo que faltaba hasta octubre. Si da 264,25, la hipótesis queda confirmada.
WITH meses AS (
  SELECT ADD_MONTHS(DATE '2025-06-01', LEVEL - 1) AS MES FROM DUAL CONNECT BY LEVEL <= 17
),
aportado AS (
  SELECT (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
               WHEN a.APRTVLRR > 0 THEN TRUNC(a.APRTFCTR, 'MM') END) AS MES,
         SUM(a.APRTVLRR) AS APORTADO
    FROM CRD.APRT a
   WHERE a.ENTDCDGO = 4762 AND a.TPAPCDGO = 11
   GROUP BY (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                  WHEN a.APRTVLRR > 0 THEN TRUNC(a.APRTFCTR, 'MM') END)
)
SELECT SUM(GREATEST(66.95 - NVL(x.APORTADO, 0), 0)) AS TOTAL_FALTANTE_HASTA_OCT
  FROM meses m LEFT JOIN aportado x ON x.MES = m.MES;

-- Fechas de caja de los aportes de cesantía sin devengo (para entender de dónde salen los meses
-- que el proceso da por pagados).
SELECT TRUNC(a.APRTFCTR, 'MM') AS MES_CAJA, COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS TOTAL
  FROM CRD.APRT a
 WHERE a.ENTDCDGO = 4762 AND a.TPAPCDGO = 11 AND a.APRTPRDV IS NULL
 GROUP BY TRUNC(a.APRTFCTR, 'MM')
 ORDER BY 1;

-- No hay reverso: no modifica nada.

-- =============================================================================
-- 67 - COMPARACION G44 "IMPOSICIONES ACUMULADAS": FILAS (ANTES) vs MESES (DESPUES)
-- =============================================================================
-- Acompaña la decision del usuario (2026-08-27, Fase 5 del plan de devengo de aportes):
-- AporteDaoServiceImpl.selectCountImposicionesJubilacionPorEntidad pasa de COUNT(*) (filas)
-- a COUNT(DISTINCT periodo efectivo) (meses de devengo). "Imposiciones acumuladas" significa
-- meses aportados, no filas — con anticipos y meses pagados a medias, filas <> meses.
--
-- ESTA CIFRA SE REPORTA A LA SUPERINTENDENCIA. Correr esto ANTES de emitir el proximo G44
-- con el cambio activo, para ver a que entidades les cambia el numero y por cuanto.
--
-- SOLO LECTURA. Ningun DML. Ajustar FECHA_CORTE (bloque 0) a la fecha de corte real del G44
-- que se va a comparar antes de correr el resto.
--
-- MISMO FILTRO que el metodo (sin cambios): TPAP.TPAPIDST = 1 (tipo de aporte activo),
-- APRTVLRR > 0, APRTFCTR <= FECHA_CORTE. Lo unico que cambia es COUNT(*) vs
-- COUNT(DISTINCT periodo efectivo) — igual que AporteDaoServiceImpl.PERIODO_EFECTIVO_SQL
-- (com.saa.ejb.crd.daoImpl.PeriodoEfectivoAporteSql).
--
-- INDICE
--   0. Fecha de corte a comparar (ajustar aqui)
--   1. Comparacion por entidad — solo las que cambian
--   2. Resumen por tramos de diferencia
--   3. Totales generales (una fila, para citar en el reporte al usuario)
-- =============================================================================


-- =============================================================================
-- 0. FECHA DE CORTE — AJUSTAR antes de correr el resto
-- =============================================================================
-- Cambiar la fecha en los tres bloques de abajo (aparece repetida porque cada uno es
-- independiente) por la fecha de corte real del G44 a comparar.
-- Ejemplo usado en este archivo: fin del mes de agosto 2026.
-- =============================================================================


-- =============================================================================
-- 1. COMPARACION POR ENTIDAD — solo las que cambian (esperado: la mayoria NO aparece aqui)
-- =============================================================================
WITH BASE AS (
        SELECT  a.ENTDCDGO,
                a.APRTCDGO,
                (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                      WHEN a.APRTVLRR > 0         THEN TRUNC(a.APRTFCTR, 'MM')
                      ELSE NULL END) AS PERIODO_EFECTIVO
        FROM    CRD.APRT a
        JOIN    CRD.TPAP ta ON ta.TPAPCDGO = a.TPAPCDGO
        WHERE   ta.TPAPIDST = 1
        AND     a.APRTVLRR > 0
        AND     a.APRTFCTR <= DATE '2026-08-31'   -- <-- AJUSTAR a la fecha de corte real
)
SELECT  e.ENTDNMID  AS NUMERO_IDENTIFICACION,
        e.ENTDRZNS  AS RAZON_SOCIAL,
        b.ENTDCDGO  AS ID_ENTIDAD,
        COUNT(*)                                       AS ANTES_FILAS,
        COUNT(DISTINCT b.PERIODO_EFECTIVO)              AS DESPUES_MESES,
        COUNT(*) - COUNT(DISTINCT b.PERIODO_EFECTIVO)   AS DIFERENCIA
FROM    BASE b
JOIN    CRD.ENTD e ON e.ENTDCDGO = b.ENTDCDGO
GROUP BY e.ENTDNMID, e.ENTDRZNS, b.ENTDCDGO
HAVING  COUNT(*) <> COUNT(DISTINCT b.PERIODO_EFECTIVO)
ORDER BY DIFERENCIA DESC, e.ENTDNMID;


-- =============================================================================
-- 2. RESUMEN POR TRAMOS DE DIFERENCIA — cuantas entidades caen en cada tramo
-- =============================================================================
WITH BASE AS (
        SELECT  a.ENTDCDGO,
                a.APRTCDGO,
                (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                      WHEN a.APRTVLRR > 0         THEN TRUNC(a.APRTFCTR, 'MM')
                      ELSE NULL END) AS PERIODO_EFECTIVO
        FROM    CRD.APRT a
        JOIN    CRD.TPAP ta ON ta.TPAPCDGO = a.TPAPCDGO
        WHERE   ta.TPAPIDST = 1
        AND     a.APRTVLRR > 0
        AND     a.APRTFCTR <= DATE '2026-08-31'   -- <-- AJUSTAR a la fecha de corte real
),
POR_ENTIDAD AS (
        SELECT  b.ENTDCDGO,
                COUNT(*)                          AS ANTES_FILAS,
                COUNT(DISTINCT b.PERIODO_EFECTIVO) AS DESPUES_MESES
        FROM    BASE b
        GROUP BY b.ENTDCDGO
)
SELECT  CASE
            WHEN ANTES_FILAS - DESPUES_MESES = 0            THEN '0 (sin cambio)'
            WHEN ANTES_FILAS - DESPUES_MESES BETWEEN 1 AND 2 THEN '1-2 meses menos'
            WHEN ANTES_FILAS - DESPUES_MESES BETWEEN 3 AND 5 THEN '3-5 meses menos'
            WHEN ANTES_FILAS - DESPUES_MESES BETWEEN 6 AND 10 THEN '6-10 meses menos'
            ELSE '11+ meses menos'
        END                                        AS TRAMO_DIFERENCIA,
        COUNT(*)                                   AS ENTIDADES,
        MIN(ANTES_FILAS - DESPUES_MESES)            AS DIF_MINIMA_TRAMO,
        MAX(ANTES_FILAS - DESPUES_MESES)            AS DIF_MAXIMA_TRAMO
FROM    POR_ENTIDAD
GROUP BY CASE
            WHEN ANTES_FILAS - DESPUES_MESES = 0            THEN '0 (sin cambio)'
            WHEN ANTES_FILAS - DESPUES_MESES BETWEEN 1 AND 2 THEN '1-2 meses menos'
            WHEN ANTES_FILAS - DESPUES_MESES BETWEEN 3 AND 5 THEN '3-5 meses menos'
            WHEN ANTES_FILAS - DESPUES_MESES BETWEEN 6 AND 10 THEN '6-10 meses menos'
            ELSE '11+ meses menos'
         END
ORDER BY DIF_MINIMA_TRAMO;


-- =============================================================================
-- 3. TOTALES GENERALES — una sola fila, la cifra a citar en el reporte al usuario
-- =============================================================================
WITH BASE AS (
        SELECT  a.ENTDCDGO,
                a.APRTCDGO,
                (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                      WHEN a.APRTVLRR > 0         THEN TRUNC(a.APRTFCTR, 'MM')
                      ELSE NULL END) AS PERIODO_EFECTIVO
        FROM    CRD.APRT a
        JOIN    CRD.TPAP ta ON ta.TPAPCDGO = a.TPAPCDGO
        WHERE   ta.TPAPIDST = 1
        AND     a.APRTVLRR > 0
        AND     a.APRTFCTR <= DATE '2026-08-31'   -- <-- AJUSTAR a la fecha de corte real
),
POR_ENTIDAD AS (
        SELECT  b.ENTDCDGO,
                COUNT(*)                          AS ANTES_FILAS,
                COUNT(DISTINCT b.PERIODO_EFECTIVO) AS DESPUES_MESES
        FROM    BASE b
        GROUP BY b.ENTDCDGO
)
SELECT  COUNT(*)                                                        AS TOTAL_ENTIDADES,
        SUM(CASE WHEN ANTES_FILAS <> DESPUES_MESES THEN 1 ELSE 0 END)   AS ENTIDADES_QUE_CAMBIAN,
        SUM(ANTES_FILAS)                                                AS SUMA_ANTES_FILAS,
        SUM(DESPUES_MESES)                                              AS SUMA_DESPUES_MESES,
        SUM(ANTES_FILAS) - SUM(DESPUES_MESES)                           AS DIFERENCIA_TOTAL,
        MAX(ANTES_FILAS - DESPUES_MESES)                                AS MAYOR_DIFERENCIA_UNA_ENTIDAD
FROM    POR_ENTIDAD;

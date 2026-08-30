-- =============================================================================
-- 68 - COMPARACION COMBINADA: "IMPOSICIONES/APORTES" DE LOS REPORTES G QUE CONTABAN
--      FILAS EN VEZ DE MESES DE DEVENGO (G43 personales, G43 patronales, G44)
-- =============================================================================
-- Decisión del usuario (2026-08-27): bajo el modelo de devengo, FILAS <> MESES. Un
-- partícipe puede tener varias filas del mismo mes (pago parcial completado después,
-- anticipos, ajustes) y una sola fila puede cubrir un mes distinto al de su fecha de caja.
-- "Imposiciones" / "número de aportes" significa MESES aportados, no filas. La regla está
-- escrita como tal (no como corrección puntual) en
-- docs/logica-negocio/reportes/REGLAS_GENERACION_REPORTES_G.md — leer esa sección antes de
-- interpretar este script.
--
-- CUBRE LOS TRES MÉTODOS YA CORREGIDOS (COUNT(*) -> COUNT(DISTINCT periodo efectivo)):
--   G43 personales  AporteDaoServiceImpl.selectCountImposicionesPersonalesPorEntidad
--                   tipoAporte IN (9,11), valor > 0           (SIN corte de fecha)
--   G43 patronales  AporteDaoServiceImpl.selectCountImposicionesPatronalesPorEntidad
--                   tipoAporte IN (13,14), valor > 0          (SIN corte de fecha)
--   G44             AporteDaoServiceImpl.selectCountImposicionesJubilacionPorEntidad
--                   TPAP.TPAPIDST = 1, valor > 0, fechaTransaccion <= FECHA_CORTE
--
-- Cada uno mantiene EXACTAMENTE el filtro que ya tenía en producción (ninguno se amplía ni
-- se recorta): lo único que cambia es COUNT(*) por COUNT(DISTINCT periodo efectivo). G43 no
-- tiene corte de fecha en el código real -- por eso no lo lleva aquí tampoco.
--
-- RPRT_ESCT_APRT.jrxml quedó fuera de este barrido a propósito: su NUMERO_MOVIMIENTOS está
-- bien etiquetado (cuenta movimientos, no meses) y el campo ni siquiera se imprime en el
-- reporte -- ver la nota en REGLAS_GENERACION_REPORTES_G.md, no se toca.
--
-- SOLO LECTURA. Ningún DML. Ajustar FECHA_CORTE (bloque 0, usado solo por G44) antes de
-- correr el resto.
--
-- ÍNDICE
--   0. Fecha de corte de G44 (ajustar)
--   1. Comparación por entidad y por reporte — solo las que cambian
--   2. Resumen por tramos de diferencia, desglosado por reporte
--   3. Totales generales por reporte (una fila por reporte)
-- =============================================================================


-- =============================================================================
-- 1. COMPARACIÓN POR ENTIDAD Y POR REPORTE — solo las que cambian
-- =============================================================================
WITH G43_PERSONALES AS (
        SELECT  a.ENTDCDGO,
                a.APRTCDGO,
                (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                      WHEN a.APRTVLRR > 0         THEN TRUNC(a.APRTFCTR, 'MM')
                      ELSE NULL END) AS PERIODO_EFECTIVO
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (9, 11)
        AND     a.APRTVLRR > 0
),
G43_PATRONALES AS (
        SELECT  a.ENTDCDGO,
                a.APRTCDGO,
                (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                      WHEN a.APRTVLRR > 0         THEN TRUNC(a.APRTFCTR, 'MM')
                      ELSE NULL END) AS PERIODO_EFECTIVO
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (13, 14)
        AND     a.APRTVLRR > 0
),
G44 AS (
        SELECT  a.ENTDCDGO,
                a.APRTCDGO,
                (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                      WHEN a.APRTVLRR > 0         THEN TRUNC(a.APRTFCTR, 'MM')
                      ELSE NULL END) AS PERIODO_EFECTIVO
        FROM    CRD.APRT a
        JOIN    CRD.TPAP ta ON ta.TPAPCDGO = a.TPAPCDGO
        WHERE   ta.TPAPIDST = 1
        AND     a.APRTVLRR > 0
        AND     a.APRTFCTR <= DATE '2026-08-31'   -- <-- AJUSTAR a la fecha de corte real de G44
),
COMBINADO AS (
        SELECT 'G43 PERSONALES' AS REPORTE, ENTDCDGO, APRTCDGO, PERIODO_EFECTIVO FROM G43_PERSONALES
        UNION ALL
        SELECT 'G43 PATRONALES' AS REPORTE, ENTDCDGO, APRTCDGO, PERIODO_EFECTIVO FROM G43_PATRONALES
        UNION ALL
        SELECT 'G44'            AS REPORTE, ENTDCDGO, APRTCDGO, PERIODO_EFECTIVO FROM G44
)
SELECT  c.REPORTE,
        e.ENTDNMID AS NUMERO_IDENTIFICACION,
        e.ENTDRZNS AS RAZON_SOCIAL,
        c.ENTDCDGO AS ID_ENTIDAD,
        COUNT(*)                                       AS ANTES_FILAS,
        COUNT(DISTINCT c.PERIODO_EFECTIVO)              AS DESPUES_MESES,
        COUNT(*) - COUNT(DISTINCT c.PERIODO_EFECTIVO)   AS DIFERENCIA
FROM    COMBINADO c
JOIN    CRD.ENTD e ON e.ENTDCDGO = c.ENTDCDGO
GROUP BY c.REPORTE, e.ENTDNMID, e.ENTDRZNS, c.ENTDCDGO
HAVING  COUNT(*) <> COUNT(DISTINCT c.PERIODO_EFECTIVO)
ORDER BY c.REPORTE, DIFERENCIA DESC, e.ENTDNMID;


-- =============================================================================
-- 2. RESUMEN POR TRAMOS DE DIFERENCIA, DESGLOSADO POR REPORTE
-- =============================================================================
WITH G43_PERSONALES AS (
        SELECT  a.ENTDCDGO, a.APRTCDGO,
                (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                      WHEN a.APRTVLRR > 0         THEN TRUNC(a.APRTFCTR, 'MM')
                      ELSE NULL END) AS PERIODO_EFECTIVO
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
),
G43_PATRONALES AS (
        SELECT  a.ENTDCDGO, a.APRTCDGO,
                (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                      WHEN a.APRTVLRR > 0         THEN TRUNC(a.APRTFCTR, 'MM')
                      ELSE NULL END) AS PERIODO_EFECTIVO
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (13, 14) AND a.APRTVLRR > 0
),
G44 AS (
        SELECT  a.ENTDCDGO, a.APRTCDGO,
                (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                      WHEN a.APRTVLRR > 0         THEN TRUNC(a.APRTFCTR, 'MM')
                      ELSE NULL END) AS PERIODO_EFECTIVO
        FROM    CRD.APRT a
        JOIN    CRD.TPAP ta ON ta.TPAPCDGO = a.TPAPCDGO
        WHERE   ta.TPAPIDST = 1 AND a.APRTVLRR > 0
        AND     a.APRTFCTR <= DATE '2026-08-31'   -- <-- AJUSTAR a la fecha de corte real de G44
),
COMBINADO AS (
        SELECT 'G43 PERSONALES' AS REPORTE, ENTDCDGO, PERIODO_EFECTIVO FROM G43_PERSONALES
        UNION ALL
        SELECT 'G43 PATRONALES' AS REPORTE, ENTDCDGO, PERIODO_EFECTIVO FROM G43_PATRONALES
        UNION ALL
        SELECT 'G44'            AS REPORTE, ENTDCDGO, PERIODO_EFECTIVO FROM G44
),
POR_ENTIDAD AS (
        SELECT  c.REPORTE, c.ENTDCDGO,
                COUNT(*)                          AS ANTES_FILAS,
                COUNT(DISTINCT c.PERIODO_EFECTIVO) AS DESPUES_MESES
        FROM    COMBINADO c
        GROUP BY c.REPORTE, c.ENTDCDGO
)
SELECT  REPORTE,
        CASE
            WHEN ANTES_FILAS - DESPUES_MESES = 0             THEN '0 (sin cambio)'
            WHEN ANTES_FILAS - DESPUES_MESES BETWEEN 1 AND 2 THEN '1-2 meses menos'
            WHEN ANTES_FILAS - DESPUES_MESES BETWEEN 3 AND 5 THEN '3-5 meses menos'
            WHEN ANTES_FILAS - DESPUES_MESES BETWEEN 6 AND 10 THEN '6-10 meses menos'
            ELSE '11+ meses menos'
        END                                        AS TRAMO_DIFERENCIA,
        COUNT(*)                                   AS ENTIDADES,
        MIN(ANTES_FILAS - DESPUES_MESES)            AS DIF_MINIMA_TRAMO,
        MAX(ANTES_FILAS - DESPUES_MESES)            AS DIF_MAXIMA_TRAMO
FROM    POR_ENTIDAD
GROUP BY REPORTE,
         CASE
            WHEN ANTES_FILAS - DESPUES_MESES = 0             THEN '0 (sin cambio)'
            WHEN ANTES_FILAS - DESPUES_MESES BETWEEN 1 AND 2 THEN '1-2 meses menos'
            WHEN ANTES_FILAS - DESPUES_MESES BETWEEN 3 AND 5 THEN '3-5 meses menos'
            WHEN ANTES_FILAS - DESPUES_MESES BETWEEN 6 AND 10 THEN '6-10 meses menos'
            ELSE '11+ meses menos'
         END
ORDER BY REPORTE, DIF_MINIMA_TRAMO;


-- =============================================================================
-- 3. TOTALES GENERALES POR REPORTE — una fila por reporte
-- =============================================================================
WITH G43_PERSONALES AS (
        SELECT  a.ENTDCDGO, a.APRTCDGO,
                (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                      WHEN a.APRTVLRR > 0         THEN TRUNC(a.APRTFCTR, 'MM')
                      ELSE NULL END) AS PERIODO_EFECTIVO
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
),
G43_PATRONALES AS (
        SELECT  a.ENTDCDGO, a.APRTCDGO,
                (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                      WHEN a.APRTVLRR > 0         THEN TRUNC(a.APRTFCTR, 'MM')
                      ELSE NULL END) AS PERIODO_EFECTIVO
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (13, 14) AND a.APRTVLRR > 0
),
G44 AS (
        SELECT  a.ENTDCDGO, a.APRTCDGO,
                (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                      WHEN a.APRTVLRR > 0         THEN TRUNC(a.APRTFCTR, 'MM')
                      ELSE NULL END) AS PERIODO_EFECTIVO
        FROM    CRD.APRT a
        JOIN    CRD.TPAP ta ON ta.TPAPCDGO = a.TPAPCDGO
        WHERE   ta.TPAPIDST = 1 AND a.APRTVLRR > 0
        AND     a.APRTFCTR <= DATE '2026-08-31'   -- <-- AJUSTAR a la fecha de corte real de G44
),
COMBINADO AS (
        SELECT 'G43 PERSONALES' AS REPORTE, ENTDCDGO, PERIODO_EFECTIVO FROM G43_PERSONALES
        UNION ALL
        SELECT 'G43 PATRONALES' AS REPORTE, ENTDCDGO, PERIODO_EFECTIVO FROM G43_PATRONALES
        UNION ALL
        SELECT 'G44'            AS REPORTE, ENTDCDGO, PERIODO_EFECTIVO FROM G44
),
POR_ENTIDAD AS (
        SELECT  c.REPORTE, c.ENTDCDGO,
                COUNT(*)                          AS ANTES_FILAS,
                COUNT(DISTINCT c.PERIODO_EFECTIVO) AS DESPUES_MESES
        FROM    COMBINADO c
        GROUP BY c.REPORTE, c.ENTDCDGO
)
SELECT  REPORTE,
        COUNT(*)                                                        AS TOTAL_ENTIDADES,
        SUM(CASE WHEN ANTES_FILAS <> DESPUES_MESES THEN 1 ELSE 0 END)   AS ENTIDADES_QUE_CAMBIAN,
        SUM(ANTES_FILAS)                                                AS SUMA_ANTES_FILAS,
        SUM(DESPUES_MESES)                                              AS SUMA_DESPUES_MESES,
        SUM(ANTES_FILAS) - SUM(DESPUES_MESES)                           AS DIFERENCIA_TOTAL,
        MAX(ANTES_FILAS - DESPUES_MESES)                                AS MAYOR_DIFERENCIA_UNA_ENTIDAD
FROM    POR_ENTIDAD
GROUP BY REPORTE
ORDER BY REPORTE;

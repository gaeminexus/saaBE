-- ============================================================================
-- 71_COMPARACION_GENERACION_VIEJO_VS_NUEVO.sql
-- Fase 4 del plan de devengo de aportes (docs/logica-negocio/crd/PLAN-APORTES-DEVENGO-CONTRATOS.md §4.3)
-- Fecha: 2026-08-27
--
-- SQL PURO, SOLO LECTURA (no hay ningun INSERT/UPDATE/DELETE en este documento). Para
-- correr en el plugin JDBC de VS Code. Es la consulta que el usuario mira para decidir si
-- enciende la bandera CRD_GENERACION_POR_FALTANTE (rubro 242, PUT /rest/cnfg/generacionPorFaltanteAh).
--
-- QUE COMPARA: para el periodo indicado en el parametro de abajo, por cada partícipe
-- ACTIVO/ACTIVO_EN_MORA, lo que cobraria el camino VIEJO (HistorialSueldo x meses
-- adeudados) contra lo que cobraria el camino NUEVO (faltante mes a mes contra CRD.VGCN).
--
-- ⚠ REQUISITO: esta consulta solo es informativa DESPUES de correr
-- 64_MIGRACION_CONTRATOS_VIGENCIAS.sql. Mientras CRD.VGCN este vacia, MONTO_NUEVO sale 0
-- para todos y la diferencia es simplemente -MONTO_VIEJO — eso no es un hallazgo, es la
-- tabla sin migrar. Se probo tal cual (SQL valido, resultado 0 esperado) contra la base
-- local el 2026-08-27, antes de que exista la migracion.
--
-- COMO USARLA: cambiar SOLO la fecha en el CTE "parametros" (primer dia del mes que se va
-- a generar) y correr. No hace falta editar nada mas.
-- ============================================================================


-- ============================================================================
-- BLOQUE 1 — DETALLE POR PARTICIPE, ordenado por |diferencia| descendente
-- ============================================================================

WITH parametros AS (
    SELECT DATE '2026-09-01' AS periodo FROM DUAL   -- <<< CAMBIAR AQUI el periodo a generar (primer dia del mes)
),
meses AS (
    SELECT ADD_MONTHS(DATE '2025-06-01', LEVEL - 1) AS mes   -- piso D11, igual que el resto de la Fase 3/4
    FROM DUAL, parametros
    CONNECT BY ADD_MONTHS(DATE '2025-06-01', LEVEL - 1) <= parametros.periodo
),
aportado AS (
    -- Mismo PERIODO EFECTIVO que PeriodoEfectivoAporteSql (duplicado aqui a proposito: es
    -- SQL de un documento de consulta, no codigo Java -- no hay una segunda fuente en Java).
    SELECT a.ENTDCDGO,
           (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                 WHEN a.APRTVLRR > 0         THEN TRUNC(a.APRTFCTR, 'MM')
                 ELSE NULL END) AS periodo,
           a.TPAPCDGO,
           SUM(a.APRTVLRR) AS suma
    FROM CRD.APRT a
    WHERE a.TPAPCDGO IN (9, 11)
    GROUP BY a.ENTDCDGO,
             (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                   WHEN a.APRTVLRR > 0         THEN TRUNC(a.APRTFCTR, 'MM')
                   ELSE NULL END),
             a.TPAPCDGO
),
universo AS (
    SELECT ENTDCDGO, ENTDIDST, ENTDNMID AS NUMEROIDENTIFICACION, ENTDRZNS AS RAZONSOCIAL, FLLLCDGO
    FROM CRD.ENTD
    WHERE ENTDIDST IN (1, 8)   -- ACTIVO, ACTIVO_EN_MORA
),
ultimo_aporte_previo AS (
    SELECT a.ENTDCDGO, MAX(TRUNC(a.APRTFCTR,'MM')) AS ultimo_mes
    FROM CRD.APRT a, parametros p
    WHERE a.TPAPCDGO IN (9,11) AND a.APRTFCTR < p.periodo
    GROUP BY a.ENTDCDGO
),
viejo AS (
    -- Replica GeneracionArchivoPetroServiceImpl.recopilarAportesPorHistorialSueldo +
    -- calcularMesesACobrarMorosos: monto fijo de HSTR, x meses adeudados si esta en mora.
    SELECT h.ENTDCDGO,
           NVL(h.HSTRMNAJ,0) + NVL(h.HSTRMNAC,0) AS monto_base,
           CASE WHEN u.ENTDIDST = 8 THEN
               GREATEST(1, NVL(MONTHS_BETWEEN(TRUNC(p.periodo,'MM'), ua.ultimo_mes), 1))
           ELSE 1 END AS meses_a_cobrar
    FROM (
        SELECT ENTDCDGO, HSTRMNAJ, HSTRMNAC,
               ROW_NUMBER() OVER (PARTITION BY ENTDCDGO ORDER BY HSTRFCIN DESC, HSTRCDGO DESC) rn
        FROM CRD.HSTR WHERE HSTRESTD = 99
    ) h
    JOIN universo u ON u.ENTDCDGO = h.ENTDCDGO AND h.rn = 1
    CROSS JOIN parametros p
    LEFT JOIN ultimo_aporte_previo ua ON ua.ENTDCDGO = h.ENTDCDGO
),
nuevo AS (
    -- Replica GeneracionArchivoPetroServiceImpl.recopilarAportesPorFaltante:
    -- Σ max(0, esperado(m,tipo) - aportado(m,tipo)) para m desde el piso hasta el periodo.
    SELECT ca.ENTDCDGO,
           SUM(GREATEST(0, NVL(v.VGCNMNTO,0) - NVL(ap.suma,0))) AS monto_faltante
    FROM meses m
    CROSS JOIN (
        SELECT CNTRCDGO, ENTDCDGO,
               ROW_NUMBER() OVER (PARTITION BY ENTDCDGO ORDER BY CNTRCDGO DESC) rn
        FROM CRD.CNTR WHERE CNTRESTD = 1
    ) ca
    CROSS JOIN (SELECT 9 AS tipo FROM DUAL UNION ALL SELECT 11 FROM DUAL) tipos
    LEFT JOIN CRD.VGCN v ON v.CNTRCDGO = ca.CNTRCDGO AND v.TPAPCDGO = tipos.tipo
           AND v.VGCNIDST = 1 AND v.VGCNFCIN <= LAST_DAY(m.mes)
           AND (v.VGCNFCFN IS NULL OR v.VGCNFCFN >= LAST_DAY(m.mes))
    LEFT JOIN aportado ap ON ap.ENTDCDGO = ca.ENTDCDGO AND ap.periodo = m.mes AND ap.TPAPCDGO = tipos.tipo
    WHERE ca.rn = 1
    GROUP BY ca.ENTDCDGO
)
SELECT u.ENTDCDGO, u.NUMEROIDENTIFICACION, u.RAZONSOCIAL, u.FLLLCDGO,
       ROUND(NVL(vj.monto_base * vj.meses_a_cobrar, 0), 2) AS MONTO_VIEJO,
       ROUND(NVL(nv.monto_faltante, 0), 2)                 AS MONTO_NUEVO,
       ROUND(NVL(nv.monto_faltante,0) - NVL(vj.monto_base * vj.meses_a_cobrar,0), 2) AS DIFERENCIA
FROM universo u
LEFT JOIN viejo vj ON vj.ENTDCDGO = u.ENTDCDGO
LEFT JOIN nuevo nv ON nv.ENTDCDGO = u.ENTDCDGO
ORDER BY ABS(NVL(nv.monto_faltante,0) - NVL(vj.monto_base * vj.meses_a_cobrar,0)) DESC;


-- ============================================================================
-- BLOQUE 2 — RESUMEN POR TRAMOS de |diferencia|
-- ============================================================================

WITH parametros AS (
    SELECT DATE '2026-09-01' AS periodo FROM DUAL   -- <<< MISMO periodo que el bloque 1
),
meses AS (
    SELECT ADD_MONTHS(DATE '2025-06-01', LEVEL - 1) AS mes
    FROM DUAL, parametros
    CONNECT BY ADD_MONTHS(DATE '2025-06-01', LEVEL - 1) <= parametros.periodo
),
aportado AS (
    SELECT a.ENTDCDGO,
           (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                 WHEN a.APRTVLRR > 0         THEN TRUNC(a.APRTFCTR, 'MM')
                 ELSE NULL END) AS periodo,
           a.TPAPCDGO,
           SUM(a.APRTVLRR) AS suma
    FROM CRD.APRT a
    WHERE a.TPAPCDGO IN (9, 11)
    GROUP BY a.ENTDCDGO,
             (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                   WHEN a.APRTVLRR > 0         THEN TRUNC(a.APRTFCTR, 'MM')
                   ELSE NULL END),
             a.TPAPCDGO
),
universo AS (
    SELECT ENTDCDGO, ENTDIDST FROM CRD.ENTD WHERE ENTDIDST IN (1, 8)
),
ultimo_aporte_previo AS (
    SELECT a.ENTDCDGO, MAX(TRUNC(a.APRTFCTR,'MM')) AS ultimo_mes
    FROM CRD.APRT a, parametros p
    WHERE a.TPAPCDGO IN (9,11) AND a.APRTFCTR < p.periodo
    GROUP BY a.ENTDCDGO
),
viejo AS (
    SELECT h.ENTDCDGO,
           NVL(h.HSTRMNAJ,0) + NVL(h.HSTRMNAC,0) AS monto_base,
           CASE WHEN u.ENTDIDST = 8 THEN
               GREATEST(1, NVL(MONTHS_BETWEEN(TRUNC(p.periodo,'MM'), ua.ultimo_mes), 1))
           ELSE 1 END AS meses_a_cobrar
    FROM (
        SELECT ENTDCDGO, HSTRMNAJ, HSTRMNAC,
               ROW_NUMBER() OVER (PARTITION BY ENTDCDGO ORDER BY HSTRFCIN DESC, HSTRCDGO DESC) rn
        FROM CRD.HSTR WHERE HSTRESTD = 99
    ) h
    JOIN universo u ON u.ENTDCDGO = h.ENTDCDGO AND h.rn = 1
    CROSS JOIN parametros p
    LEFT JOIN ultimo_aporte_previo ua ON ua.ENTDCDGO = h.ENTDCDGO
),
nuevo AS (
    SELECT ca.ENTDCDGO,
           SUM(GREATEST(0, NVL(v.VGCNMNTO,0) - NVL(ap.suma,0))) AS monto_faltante
    FROM meses m
    CROSS JOIN (
        SELECT CNTRCDGO, ENTDCDGO,
               ROW_NUMBER() OVER (PARTITION BY ENTDCDGO ORDER BY CNTRCDGO DESC) rn
        FROM CRD.CNTR WHERE CNTRESTD = 1
    ) ca
    CROSS JOIN (SELECT 9 AS tipo FROM DUAL UNION ALL SELECT 11 FROM DUAL) tipos
    LEFT JOIN CRD.VGCN v ON v.CNTRCDGO = ca.CNTRCDGO AND v.TPAPCDGO = tipos.tipo
           AND v.VGCNIDST = 1 AND v.VGCNFCIN <= LAST_DAY(m.mes)
           AND (v.VGCNFCFN IS NULL OR v.VGCNFCFN >= LAST_DAY(m.mes))
    LEFT JOIN aportado ap ON ap.ENTDCDGO = ca.ENTDCDGO AND ap.periodo = m.mes AND ap.TPAPCDGO = tipos.tipo
    WHERE ca.rn = 1
    GROUP BY ca.ENTDCDGO
),
comparacion AS (
    SELECT u.ENTDCDGO,
           NVL(vj.monto_base * vj.meses_a_cobrar, 0) AS monto_viejo,
           NVL(nv.monto_faltante, 0) AS monto_nuevo,
           NVL(nv.monto_faltante,0) - NVL(vj.monto_base * vj.meses_a_cobrar,0) AS diferencia
    FROM universo u
    LEFT JOIN viejo vj ON vj.ENTDCDGO = u.ENTDCDGO
    LEFT JOIN nuevo nv ON nv.ENTDCDGO = u.ENTDCDGO
)
SELECT
    CASE
        WHEN ABS(diferencia) < 0.01   THEN '1. IGUAL (< $0.01)'
        WHEN ABS(diferencia) <= 5     THEN '2. HASTA $5'
        WHEN ABS(diferencia) <= 20    THEN '3. $5 a $20'
        WHEN ABS(diferencia) <= 100   THEN '4. $20 a $100'
        WHEN ABS(diferencia) <= 500   THEN '5. $100 a $500'
        ELSE                               '6. MAS DE $500'
    END AS TRAMO,
    COUNT(*)                                   AS PARTICIPES,
    ROUND(SUM(monto_viejo), 2)                 AS TOTAL_VIEJO,
    ROUND(SUM(monto_nuevo), 2)                 AS TOTAL_NUEVO,
    ROUND(SUM(diferencia), 2)                  AS TOTAL_DIFERENCIA
FROM comparacion
GROUP BY
    CASE
        WHEN ABS(diferencia) < 0.01   THEN '1. IGUAL (< $0.01)'
        WHEN ABS(diferencia) <= 5     THEN '2. HASTA $5'
        WHEN ABS(diferencia) <= 20    THEN '3. $5 a $20'
        WHEN ABS(diferencia) <= 100   THEN '4. $20 a $100'
        WHEN ABS(diferencia) <= 500   THEN '5. $100 a $500'
        ELSE                               '6. MAS DE $500'
    END
ORDER BY 1;

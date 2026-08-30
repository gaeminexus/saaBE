-- ============================================================================
-- 73_DIAGNOSTICO_SALDO_CAPITAL_ANOMALIAS.sql
-- Segunda ola (docs/logica-negocio/crd/PENDIENTES-SEGUNDA-OLA.md §1, pedidos 6/8)
-- Fecha: 2026-08-27
--
-- SQL PURO, SOLO LECTURA (ningun INSERT/UPDATE/DELETE en este documento). Para correr en el
-- plugin JDBC de VS Code.
--
-- QUE ES: lista los préstamos cuyo saldo de capital DECLARADO (DTPRSLCP de la mínima cuota
-- NO pagada ni cancelada anticipada) NO cuadra con el saldo RECONSTRUIDO desde CRD.PGPR
-- (pagos vigentes, PGPRANUL = 0) — el mismo método que ya usan
-- MotorPagoPrestamoServiceImpl.calcularSaldosRealesCuota y
-- CierreCarteraDaoServiceImpl.PAGOS_VIGENTES, y el que ahora usa
-- SimulacionPrestamoServiceImpl.reconstruirSaldoCapitalPendiente (pedido 8).
--
-- NO PROPONE NINGUNA CORRECCIÓN. Es exclusivamente para ver qué son estos casos antes de
-- decidir si hace falta limpiar datos.
--
-- ⚠ SOBRE LOS NÚMEROS: el pedido cita una medición previa ("1.111 de 1.312 cuadran al
-- centavo", 92 hasta $100, 94 por más, 15 con declarado=0 y reconstruido positivo). Esta
-- consulta se escribió y probó desde cero contra la base local el 2026-08-27 (no se tuvo
-- acceso a la consulta original) y da, sobre el mismo universo de 1.313 préstamos con
-- cuotas pendientes: 1.113 CUADRA, 105 DIFIERE_HASTA_100, 94 DIFIERE_MAS_DE_100, 1
-- DECLARADO_CERO_RECONSTRUIDO_POSITIVO. DIFIERE_MAS_DE_100 coincide exacto (94); el resto es
-- cercano pero no idéntico — la métrica "DECLARADO_CERO..." de abajo usa una variante del
-- cálculo (ver Bloque 1) que si se aplica en vez de la de "CUADRA" da 15, exacto también.
-- La diferencia remanente no se investigó más a fondo: no cambia la naturaleza del
-- diagnóstico ni justifica inventar una tercera fórmula sin evidencia de cuál es "la buena".
--
-- METODOLOGÍA (dos variantes del mismo saldo reconstruido, según qué se compara):
--   - RECONSTRUIDO_DESPUES: suma, sobre las cuotas pendientes con numeroCuota MAYOR que el
--     de la mínima pendiente, de GREATEST(DTPRCPTL - pagado_vigente, 0). Se compara contra
--     DTPRSLCP de la mínima (que por definición es "el saldo DESPUÉS de pagarla" — pedidos
--     6/8, ya confirmado con el usuario) para la clasificación CUADRA/DIFIERE.
--   - RECONSTRUIDO_TOTAL: igual pero INCLUYENDO la mínima pendiente misma. Se usa solo para
--     detectar "declarado en cero pero en realidad debe algo, empezando por esta cuota".
-- ============================================================================


-- ============================================================================
-- BLOQUE 1 — Resumen por clasificación (para confirmar el universo antes de ver el detalle)
-- ============================================================================

WITH pendientes AS (
    SELECT d.DTPRCDGO, d.PRSTCDGO, d.DTPRNMCT, d.DTPRCPTL, d.DTPRSLCP, d.DTPRESTD,
           ROW_NUMBER() OVER (PARTITION BY d.PRSTCDGO ORDER BY d.DTPRNMCT) rn
    FROM CRD.DTPR d
    WHERE (d.DTPRESTD IS NULL OR d.DTPRESTD NOT IN (4,7))
),
vigente_por_cuota AS (
    SELECT DTPRCDGO, SUM(NVL(PGPRCPPG,0)) capital_pagado, COUNT(*) NUM_PAGOS, MAX(PGPRFCHA) ULTIMO_PAGO
    FROM CRD.PGPR
    WHERE PGPRANUL IS NULL OR PGPRANUL = 0
    GROUP BY DTPRCDGO
),
reconstruido_despues AS (
    SELECT p.PRSTCDGO,
           SUM(GREATEST(NVL(p.DTPRCPTL,0) - NVL(v.capital_pagado,0), 0)) AS saldo_despues
    FROM pendientes p
    LEFT JOIN vigente_por_cuota v ON v.DTPRCDGO = p.DTPRCDGO
    WHERE p.rn > 1
    GROUP BY p.PRSTCDGO
),
reconstruido_total AS (
    SELECT p.PRSTCDGO,
           SUM(GREATEST(NVL(p.DTPRCPTL,0) - NVL(v.capital_pagado,0), 0)) AS saldo_total
    FROM pendientes p
    LEFT JOIN vigente_por_cuota v ON v.DTPRCDGO = p.DTPRCDGO
    GROUP BY p.PRSTCDGO
),
declarado AS (
    SELECT PRSTCDGO, DTPRCDGO, DTPRNMCT AS NUMERO_MINIMA, DTPRSLCP AS SALDO_DECLARADO
    FROM pendientes
    WHERE rn = 1
)
SELECT
    CASE WHEN NVL(d.SALDO_DECLARADO,0) = 0 AND NVL(rt.saldo_total,0) > 0.01 THEN 'DECLARADO_CERO_RECONSTRUIDO_POSITIVO'
         WHEN ABS(NVL(rd.saldo_despues,0) - NVL(d.SALDO_DECLARADO,0)) <= 0.01 THEN 'CUADRA'
         WHEN ABS(NVL(rd.saldo_despues,0) - NVL(d.SALDO_DECLARADO,0)) <= 100 THEN 'DIFIERE_HASTA_100'
         ELSE 'DIFIERE_MAS_DE_100' END AS CLASIFICACION,
    COUNT(*) PRESTAMOS
FROM declarado d
LEFT JOIN reconstruido_despues rd ON rd.PRSTCDGO = d.PRSTCDGO
LEFT JOIN reconstruido_total rt ON rt.PRSTCDGO = d.PRSTCDGO
GROUP BY
    CASE WHEN NVL(d.SALDO_DECLARADO,0) = 0 AND NVL(rt.saldo_total,0) > 0.01 THEN 'DECLARADO_CERO_RECONSTRUIDO_POSITIVO'
         WHEN ABS(NVL(rd.saldo_despues,0) - NVL(d.SALDO_DECLARADO,0)) <= 0.01 THEN 'CUADRA'
         WHEN ABS(NVL(rd.saldo_despues,0) - NVL(d.SALDO_DECLARADO,0)) <= 100 THEN 'DIFIERE_HASTA_100'
         ELSE 'DIFIERE_MAS_DE_100' END
ORDER BY 1;


-- ============================================================================
-- BLOQUE 2 — Detalle de las anomalías (los 186 + 15, con lo necesario para entender cada caso)
-- ============================================================================

WITH pendientes AS (
    SELECT d.DTPRCDGO, d.PRSTCDGO, d.DTPRNMCT, d.DTPRCPTL, d.DTPRSLCP, d.DTPRESTD,
           ROW_NUMBER() OVER (PARTITION BY d.PRSTCDGO ORDER BY d.DTPRNMCT) rn
    FROM CRD.DTPR d
    WHERE (d.DTPRESTD IS NULL OR d.DTPRESTD NOT IN (4,7))
),
vigente_por_cuota AS (
    SELECT DTPRCDGO, SUM(NVL(PGPRCPPG,0)) capital_pagado, COUNT(*) NUM_PAGOS, MAX(PGPRFCHA) ULTIMO_PAGO
    FROM CRD.PGPR
    WHERE PGPRANUL IS NULL OR PGPRANUL = 0
    GROUP BY DTPRCDGO
),
reconstruido_despues AS (
    SELECT p.PRSTCDGO,
           SUM(GREATEST(NVL(p.DTPRCPTL,0) - NVL(v.capital_pagado,0), 0)) AS saldo_despues,
           SUM(NVL(v.NUM_PAGOS,0)) AS pagos_vigentes_resto,
           MAX(v.ULTIMO_PAGO) AS ultimo_pago_resto
    FROM pendientes p
    LEFT JOIN vigente_por_cuota v ON v.DTPRCDGO = p.DTPRCDGO
    WHERE p.rn > 1
    GROUP BY p.PRSTCDGO
),
reconstruido_total AS (
    SELECT p.PRSTCDGO,
           SUM(GREATEST(NVL(p.DTPRCPTL,0) - NVL(v.capital_pagado,0), 0)) AS saldo_total
    FROM pendientes p
    LEFT JOIN vigente_por_cuota v ON v.DTPRCDGO = p.DTPRCDGO
    GROUP BY p.PRSTCDGO
),
minima AS (
    SELECT p.PRSTCDGO, p.DTPRCDGO, p.DTPRNMCT AS NUMERO_MINIMA, p.DTPRCPTL AS CAPITAL_MINIMA,
           p.DTPRSLCP AS SALDO_DECLARADO, p.DTPRESTD AS ESTADO_MINIMA,
           v.capital_pagado AS PAGADO_VIGENTE_MINIMA, v.NUM_PAGOS AS PAGOS_VIGENTES_MINIMA,
           v.ULTIMO_PAGO AS ULTIMO_PAGO_MINIMA
    FROM pendientes p
    LEFT JOIN vigente_por_cuota v ON v.DTPRCDGO = p.DTPRCDGO
    WHERE p.rn = 1
),
totales_prestamo AS (
    SELECT PRSTCDGO, COUNT(*) TOTAL_CUOTAS_PENDIENTES
    FROM pendientes
    GROUP BY PRSTCDGO
)
SELECT
    pr.PRSTCDGO, pr.PRSTIDST AS ESTADO_PRESTAMO, pr.PRSTFCIN AS FECHA_INICIO,
    pr.PRSTPLZO AS PLAZO_ORIGINAL, pr.PRSTTPAM AS TIPO_AMORTIZACION,
    e.ENTDNMID AS IDENTIFICACION, e.ENTDRZNS AS RAZON_SOCIAL, e.FLLLCDGO AS FILIAL,
    prd.PRDCNMBR AS PRODUCTO,
    m.NUMERO_MINIMA, m.ESTADO_MINIMA, m.CAPITAL_MINIMA, m.SALDO_DECLARADO,
    m.PAGADO_VIGENTE_MINIMA, m.PAGOS_VIGENTES_MINIMA, m.ULTIMO_PAGO_MINIMA,
    tp.TOTAL_CUOTAS_PENDIENTES,
    ROUND(NVL(rd.saldo_despues,0), 2) AS SALDO_RECONSTRUIDO_DESPUES,
    ROUND(NVL(rt.saldo_total,0), 2) AS SALDO_RECONSTRUIDO_TOTAL,
    ROUND(NVL(rd.saldo_despues,0) - NVL(m.SALDO_DECLARADO,0), 2) AS DIFERENCIA,
    rd.pagos_vigentes_resto, rd.ultimo_pago_resto,
    CASE WHEN NVL(m.SALDO_DECLARADO,0) = 0 AND NVL(rt.saldo_total,0) > 0.01 THEN 'DECLARADO_CERO_RECONSTRUIDO_POSITIVO'
         WHEN ABS(NVL(rd.saldo_despues,0) - NVL(m.SALDO_DECLARADO,0)) <= 100 THEN 'DIFIERE_HASTA_100'
         ELSE 'DIFIERE_MAS_DE_100' END AS CLASIFICACION
FROM minima m
JOIN CRD.PRST pr ON pr.PRSTCDGO = m.PRSTCDGO
LEFT JOIN CRD.ENTD e ON e.ENTDCDGO = pr.ENTDCDGO
LEFT JOIN CRD.PRDC prd ON prd.PRDCCDGO = pr.PRDCCDGO
LEFT JOIN reconstruido_despues rd ON rd.PRSTCDGO = m.PRSTCDGO
LEFT JOIN reconstruido_total rt ON rt.PRSTCDGO = m.PRSTCDGO
JOIN totales_prestamo tp ON tp.PRSTCDGO = m.PRSTCDGO
WHERE NOT (
    ABS(NVL(rd.saldo_despues,0) - NVL(m.SALDO_DECLARADO,0)) <= 0.01
    AND NOT (NVL(m.SALDO_DECLARADO,0) = 0 AND NVL(rt.saldo_total,0) > 0.01)
)
ORDER BY
    CASE WHEN NVL(m.SALDO_DECLARADO,0) = 0 AND NVL(rt.saldo_total,0) > 0.01 THEN 1 ELSE 2 END,
    ABS(NVL(rd.saldo_despues,0) - NVL(m.SALDO_DECLARADO,0)) DESC;

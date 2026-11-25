-- =====================================================================================
-- 401 — MEDICIÓN (SOLO LECTURA) D-196: (1) la mora condonada que el asiento de condonación acredita a 1.4.02 aunque nunca se devengó;
-- (2) la mora COBRADA por encima de lo devengado que acreditó 1.4.02 en vez de ir a ingreso (el camino de cobro que no separa).
-- Equipo omen-saa-1 · 2026-11-25. No modifica nada. Se puede correr en producción o en el reproceso.
-- PARAMS (cambiar en cada bloque): MES_DESDE = 2025-11-01 · MES_HASTA = 2025-11-30.
-- Concepto de DACC: 1 capital, 2 interés (ordinario), 3 mora. ACCN estado 2 = APLICADO.
-- =====================================================================================

-- ============================ (1) CONDONACIÓN ========================================
-- 1a. Por acuerdo aplicado en el mes: lo condonado de mora, y de eso cuánto estaba DEVENGADO (pendiente contabilizada del préstamo a la fecha del acuerdo, regla del lote 7b:
-- devengo MVIC 5−6 hasta la fecha, menos mora cobrada hasta la fecha, menos mora condonada por acuerdos APLICADOS anteriores) y cuánto NO. Hoy el asiento acredita a 1.4.02
-- el TOTAL (interés ordinario + mora en una sola línea); lo que debería acreditar de mora es solo la parte devengada. Operación = PRSTIDAS (sin datos personales).
WITH params AS (SELECT DATE '2025-11-01' AS desde, DATE '2025-11-30' AS hasta FROM DUAL),
ac AS (
    SELECT a.ACCNCDGO, a.PRSTCDGO, TRUNC(a.ACCNFCHA) AS fecha,
           SUM(CASE WHEN c.DACCCPTO = 3 THEN NVL(c.DACCVLCN, 0) ELSE 0 END) AS mora_condonada,
           SUM(CASE WHEN c.DACCCPTO = 2 THEN NVL(c.DACCVLCN, 0) ELSE 0 END) AS interes_condonado
      FROM CRD.ACCN a JOIN CRD.DACC c ON c.ACCNCDGO = a.ACCNCDGO, params pa
     WHERE a.ACCNESTD = 2 AND TRUNC(a.ACCNFCHA) BETWEEN pa.desde AND pa.hasta GROUP BY a.ACCNCDGO, a.PRSTCDGO, TRUNC(a.ACCNFCHA)
),
pend AS (
    SELECT ac.ACCNCDGO,
           NVL((SELECT SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) FROM CRD.MVIC m
                 WHERE m.PRSTCDGO = ac.PRSTCDGO AND m.MVICCMPN = 2 AND m.MVICTPMV IN (5, 6) AND NVL(m.MVICANUL, 0) = 0 AND TRUNC(m.MVICFCCT) <= ac.fecha), 0)
         - NVL((SELECT SUM(NVL(g.PGPRMRPG, 0)) FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
                 WHERE d.PRSTCDGO = ac.PRSTCDGO AND NVL(g.PGPRANUL, 0) = 0 AND g.PGPRFCHA < ac.fecha + 1), 0)
         - NVL((SELECT SUM(NVL(c2.DACCVLCN, 0)) FROM CRD.ACCN a2 JOIN CRD.DACC c2 ON c2.ACCNCDGO = a2.ACCNCDGO
                 WHERE a2.PRSTCDGO = ac.PRSTCDGO AND a2.ACCNESTD = 2 AND c2.DACCCPTO = 3 AND a2.ACCNCDGO <> ac.ACCNCDGO AND a2.ACCNFCHA < ac.fecha + 1), 0) AS pendiente_contabilizada
      FROM ac
)
SELECT t.TPPRCSPB AS tipo, COUNT(*) AS acuerdos, COUNT(DISTINCT ac.PRSTCDGO) AS prestamos,
       ROUND(SUM(ac.mora_condonada), 2) AS mora_condonada,
       ROUND(SUM(LEAST(ac.mora_condonada, GREATEST(0, p.pendiente_contabilizada))), 2) AS parte_devengada,
       ROUND(SUM(ac.mora_condonada - LEAST(ac.mora_condonada, GREATEST(0, p.pendiente_contabilizada))), 2) AS parte_no_devengada_acreditada_de_mas,
       ROUND(SUM(ac.interes_condonado), 2) AS interes_ordinario_condonado
  FROM ac JOIN pend p ON p.ACCNCDGO = ac.ACCNCDGO JOIN CRD.PRST pr ON pr.PRSTCDGO = ac.PRSTCDGO JOIN CRD.PRDC prd ON prd.PRDCCDGO = pr.PRDCCDGO JOIN CRD.TPPR t ON t.TPPRCDGO = prd.TPPRCDGO
 GROUP BY t.TPPRCSPB ORDER BY 1;

-- 1b. El INTERÉS ORDINARIO condonado: ¿estaba devengado? El ④ (C27b) devenga el interés de las cuotas con vencimiento en el mes que abre. Una cuota condonada cuyo mes ya abrió
-- (vencimiento <= fin del mes siguiente al último cierre) tiene su interés devengado; la de un mes que todavía no abre, no. Por acuerdo: interés condonado de cuotas con vencimiento
-- posterior al fin del mes del acuerdo (probablemente no devengado). DTPRINTR de las cuotas del préstamo hoy cerradas en estado 7 por ese acuerdo no se conserva por separado:
-- esta es una aproximación por vencimiento.
WITH params AS (SELECT DATE '2025-11-01' AS desde, DATE '2025-11-30' AS hasta FROM DUAL)
SELECT a.ACCNCDGO AS acuerdo, a.PRSTCDGO AS prestamo, TRUNC(a.ACCNFCHA) AS fecha,
       SUM(NVL(c.DACCVLCN, 0)) AS interes_condonado_total,
       (SELECT ROUND(SUM(NVL(d.DTPRINTR, 0)), 2) FROM CRD.DTPR d WHERE d.PRSTCDGO = a.PRSTCDGO AND NVL(d.DTPRESTD, 0) = 7 AND TRUNC(d.DTPRFCVN) > LAST_DAY(TRUNC(a.ACCNFCHA))) AS interes_de_cuotas_de_meses_posteriores
  FROM CRD.ACCN a JOIN CRD.DACC c ON c.ACCNCDGO = a.ACCNCDGO AND c.DACCCPTO = 2, params pa
 WHERE a.ACCNESTD = 2 AND TRUNC(a.ACCNFCHA) BETWEEN pa.desde AND pa.hasta
 GROUP BY a.ACCNCDGO, a.PRSTCDGO, TRUNC(a.ACCNFCHA) ORDER BY 1;

-- ============================ (2) COBRO DE MORA QUE NO SEPARA ========================
-- 2a. Pagos con mora del mes y lo que `separarMoraNoDevengada` habría separado a ingreso: excedente = mora pagada − devengado pendiente de la cuota (MVIC 5−6 hasta el pago, menos mora
-- pagada por pagos anteriores). Si el excedente es > 0 debería existir un MVIC tipo 5 de origen COBRO ligado al pago (que `separarMoraNoDevengada` graba). Sin ese MVIC = el cobro NO pasó por la separación.
WITH params AS (SELECT DATE '2025-11-01' AS desde, DATE '2025-11-30' AS hasta FROM DUAL),
pagos AS (
    SELECT g.PGPRCDGO, g.DTPRCDGO, d.PRSTCDGO, g.PGPRFCHA, NVL(g.PGPRMRPG, 0) AS mora, g.PGPRTPOO AS tipo_pago, g.CRARCDGO, g.PGPRASNT, g.EVPRCDGO
      FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO, params pa
     WHERE NVL(g.PGPRANUL, 0) = 0 AND NVL(g.PGPRMRPG, 0) > 0.005 AND TRUNC(g.PGPRFCHA) BETWEEN pa.desde AND pa.hasta
),
calc AS (
    SELECT p.*,
           NVL((SELECT SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) FROM CRD.MVIC m
                 WHERE m.DTPRCDGO = p.DTPRCDGO AND m.MVICCMPN = 2 AND m.MVICTPMV IN (5, 6) AND NVL(m.MVICANUL, 0) = 0 AND NOT (m.MVICORGN = 'COBRO' AND m.PGPRCDGO = p.PGPRCDGO)
                 AND TRUNC(m.MVICFCCT) <= TRUNC(p.PGPRFCHA)), 0) AS devengado,
           NVL((SELECT SUM(NVL(x.PGPRMRPG, 0)) FROM CRD.PGPR x WHERE x.DTPRCDGO = p.DTPRCDGO AND NVL(x.PGPRANUL, 0) = 0 AND x.PGPRCDGO < p.PGPRCDGO), 0) AS pagado_previo,
           (SELECT COUNT(*) FROM CRD.MVIC m2 WHERE m2.PGPRCDGO = p.PGPRCDGO AND m2.MVICTPMV = 5 AND m2.MVICORGN = 'COBRO' AND NVL(m2.MVICANUL, 0) = 0) AS mvic_cobro
      FROM pagos p
)
SELECT NVL(c.tipo_pago, '(sin tipo)') AS tipo_pago, CASE WHEN c.CRARCDGO IS NOT NULL THEN 'carga Petro' WHEN c.EVPRCDGO IS NOT NULL THEN 'evento (precancelación/acuerdo/abono)' ELSE 'cobro/CBCR u otro' END AS camino,
       COUNT(*) AS pagos, COUNT(DISTINCT c.PRSTCDGO) AS prestamos, ROUND(SUM(c.mora), 2) AS mora_cobrada,
       ROUND(SUM(GREATEST(0, c.mora - GREATEST(0, c.devengado - c.pagado_previo))), 2) AS excedente_que_debio_ir_a_ingreso,
       ROUND(SUM(CASE WHEN c.mora - GREATEST(0, c.devengado - c.pagado_previo) > 0.005 AND c.mvic_cobro = 0 THEN c.mora - GREATEST(0, c.devengado - c.pagado_previo) ELSE 0 END), 2) AS excedente_sin_separar
  FROM calc c GROUP BY NVL(c.tipo_pago, '(sin tipo)'), CASE WHEN c.CRARCDGO IS NOT NULL THEN 'carga Petro' WHEN c.EVPRCDGO IS NOT NULL THEN 'evento (precancelación/acuerdo/abono)' ELSE 'cobro/CBCR u otro' END
 ORDER BY excedente_sin_separar DESC;

-- 2b. El asiento de esos pagos: su descripción dice por qué camino se contabilizó (cruce con aportes, precancelación con aportes, CBCR, Petro…). Los 100 con mayor excedente sin separar.
-- Pegar los CTE del 2a (params, pagos, calc) antes de este SELECT.
--   SELECT * FROM (
--     SELECT c.PGPRCDGO, c.tipo_pago, c.PGPRASNT AS asiento, SUBSTR(a.ASNTOBSR, 1, 90) AS descripcion_asiento, ROUND(c.mora, 2) AS mora,
--            ROUND(c.mora - GREATEST(0, c.devengado - c.pagado_previo), 2) AS excedente
--       FROM calc c LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = c.PGPRASNT
--      WHERE c.mora - GREATEST(0, c.devengado - c.pagado_previo) > 0.005 AND c.mvic_cobro = 0 ORDER BY 6 DESC
--   ) WHERE ROWNUM <= 100;

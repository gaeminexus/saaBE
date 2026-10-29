-- 333 — Control del modo re-proceso del motor de cobros (D-019 en CBCR, 2026-10-28)
-- Doc: docs/logica-negocio/crd/API-REPROCESO-COBRO-FECHA-PASADA.md
-- Solo SELECT. Cambiar el mes (2025-06) y la lista de préstamos según el lote.

-- 1) ANTES de procesar: ¿cada préstamo del lote estaba vigente al corte? (mismo criterio que
--    PrestamoDaoService.selectByEntidadYProductoVigentesAlCorte). Las tres columnas de conteo deben ser > 0
--    para un préstamo terminal hoy (estado 3, 4, 5, 9, 13); si alguna es 0, procesar da PRESTAMO_NO_VIGENTE_AL_CORTE.
SELECT p.PRSTCDGO, p.PRSTIDST estado_hoy, TO_CHAR(p.PRSTFCIN, 'YYYY-MM-DD') inicio,
       CASE WHEN p.PRSTFCIN IS NULL OR p.PRSTFCIN <= TIMESTAMP '2025-06-30 23:59:59' THEN 1 ELSE 0 END empezo_al_corte,
       (SELECT COUNT(*) FROM CRD.DTPR d0 WHERE d0.PRSTCDGO = p.PRSTCDGO
           AND d0.DTPRFCVN <= TIMESTAMP '2025-06-30 23:59:59') cuotas_vencidas_al_corte,
       (SELECT COUNT(*) FROM CRD.DTPR d WHERE d.PRSTCDGO = p.PRSTCDGO
           AND (d.DTPRESTD IS NULL OR d.DTPRESTD NOT IN (4, 7) OR d.DTPRFCPG IS NULL
                OR d.DTPRFCPG > TIMESTAMP '2025-06-30 23:59:59')) cuotas_no_saldadas_al_corte,
       (SELECT COUNT(*) FROM CRD.EVPR e WHERE e.PRSTCDGO = p.PRSTCDGO AND e.EVPRFCAN IS NULL
           AND e.EVPRFCHA > TIMESTAMP '2025-06-30 23:59:59') eventos_posteriores
FROM CRD.PRST p
WHERE p.PRSTCDGO IN (2937, 2959, 6949, 8069, 8085, 8140, 8183, 6201)
ORDER BY p.PRSTCDGO;

-- 2) DESPUÉS: cobros del mes, préstamo, estado de hoy, valor aplicado (PGPR vigentes del evento) y si D-019
--    lo reabrió (marca "REPROCESO CBCR <id>" en PRSTOBSR). Esperado: CBCRESTD = 3 (PROCESADO), aplicado = DCBCVLRR
--    (salvo PRECANCELACION/ABONO con aportes, donde el PGPR lleva también lo cruzado con aportes).
SELECT c.CBCRCDGO, c.CBCRTPOO tipo, c.CBCRESTD estado, TO_CHAR(c.CBCRFCHA, 'YYYY-MM-DD') fecha_pago,
       TO_CHAR(c.CBCRFCAF, 'YYYY-MM-DD') fecha_afectacion, c.CBCRRFRN referencia, d.PRSTCDGO, p.PRSTIDST estado_hoy,
       d.DCBCVLRR valor_linea, d.EVPRCDGO,
       (SELECT ROUND(SUM(g.PGPRVLRR), 2) FROM CRD.PGPR g WHERE g.EVPRCDGO = d.EVPRCDGO AND NVL(g.PGPRANUL, 0) = 0) aplicado,
       CASE WHEN INSTR(p.PRSTOBSR, 'REPROCESO CBCR ' || c.CBCRCDGO || ' ') > 0 THEN 'REABIERTO D-019' END d019
FROM CRD.CBCR c
JOIN CRD.DCBC d ON d.CBCRCDGO = c.CBCRCDGO
LEFT JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
WHERE c.CBCRFCHA >= DATE '2025-06-01' AND c.CBCRFCHA < DATE '2025-07-01'
ORDER BY c.CBCRCDGO, d.DCBCCDGO;

-- 3) Préstamos reabiertos por el modo re-proceso del CBCR y eventos posteriores marcados (no anulados).
SELECT p.PRSTCDGO, p.PRSTIDST estado_hoy,
       REGEXP_SUBSTR(p.PRSTOBSR, 'REPROCESO CBCR [0-9]+ \(corte [0-9-]+\)[^|]*') marca,
       (SELECT COUNT(*) FROM CRD.EVPR e WHERE e.PRSTCDGO = p.PRSTCDGO AND e.EVPROBSR LIKE '%REPROCESO CBCR%') eventos_marcados
FROM CRD.PRST p
WHERE p.PRSTOBSR LIKE '%REPROCESO CBCR%'
ORDER BY p.PRSTCDGO;

-- 4) Cuotas del mes de esos préstamos: estado, saldo, fecha de pago y pagos vigentes.
SELECT d.PRSTCDGO, d.DTPRCDGO, TO_CHAR(d.DTPRFCVN, 'YYYY-MM-DD') vence, d.DTPRESTD, d.DTPRIDST, d.DTPRSLDO,
       TO_CHAR(d.DTPRFCPG, 'YYYY-MM-DD') fecha_pago,
       (SELECT COUNT(*) FROM CRD.PGPR g WHERE g.DTPRCDGO = d.DTPRCDGO AND NVL(g.PGPRANUL, 0) = 0) pagos_vigentes,
       (SELECT ROUND(SUM(g.PGPRVLRR), 2) FROM CRD.PGPR g WHERE g.DTPRCDGO = d.DTPRCDGO AND NVL(g.PGPRANUL, 0) = 0) pagado
FROM CRD.DTPR d
WHERE d.PRSTCDGO IN (SELECT DISTINCT dc.PRSTCDGO FROM CRD.DCBC dc JOIN CRD.CBCR c ON c.CBCRCDGO = dc.CBCRCDGO
                     WHERE c.CBCRFCHA >= DATE '2025-06-01' AND c.CBCRFCHA < DATE '2025-07-01' AND dc.PRSTCDGO IS NOT NULL)
  AND d.DTPRFCVN >= DATE '2025-06-01' AND d.DTPRFCVN < DATE '2025-07-01'
ORDER BY d.PRSTCDGO, d.DTPRFCVN;

-- 5) Los tres asientos de cada cobro del mes (transitorio, reparto, definitivo) con su fecha: debe ser la de
--    afectación (CBCRFCAF) y estar en el período abierto. Estado del asiento: 2 = anulado.
SELECT c.CBCRCDGO, 'TRANSITORIO' asiento, a.ASNTCDGO, TO_CHAR(a.ASNTFCHA, 'YYYY-MM-DD') fecha, a.ASNTESTD
FROM CRD.CBCR c JOIN CNT.ASNT a ON a.ASNTCDGO = c.CBCRASN1
WHERE c.CBCRFCHA >= DATE '2025-06-01' AND c.CBCRFCHA < DATE '2025-07-01'
UNION ALL
SELECT c.CBCRCDGO, 'REPARTO', a.ASNTCDGO, TO_CHAR(a.ASNTFCHA, 'YYYY-MM-DD'), a.ASNTESTD
FROM CRD.CBCR c JOIN CNT.ASNT a ON a.ASNTCDGO = c.CBCRASRP
WHERE c.CBCRFCHA >= DATE '2025-06-01' AND c.CBCRFCHA < DATE '2025-07-01'
UNION ALL
SELECT c.CBCRCDGO, 'DEFINITIVO', a.ASNTCDGO, TO_CHAR(a.ASNTFCHA, 'YYYY-MM-DD'), a.ASNTESTD
FROM CRD.CBCR c JOIN CNT.ASNT a ON a.ASNTCDGO = c.CBCRASN2
WHERE c.CBCRFCHA >= DATE '2025-06-01' AND c.CBCRFCHA < DATE '2025-07-01'
ORDER BY 1, 2;

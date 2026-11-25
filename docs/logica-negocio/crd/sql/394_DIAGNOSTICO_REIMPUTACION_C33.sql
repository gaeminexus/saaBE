-- =====================================================================================
-- 394 — DIAGNÓSTICO (SOLO LECTURA) de la re-imputación C33 del 2026-11-25: por qué quedaron préstamos «pagada de más»
-- Equipo omen-saa-1 · 2026-11-25. No modifica nada. Se puede correr en producción.
--
-- HIPÓTESIS PRINCIPAL (ver el diagnóstico): PrelacionCuotasServiceImpl lee los pagos con PagoPrestamoDaoService.selectVigentesByPrestamo, que
-- filtra por PGPR.PRSTCDGO (p.prestamo.codigo). El control del cierre (DetallePrestamoDaoServiceImpl.selectControlPrelacionAlCorte) agrupa
-- por PGPR.DTPRCDGO (la cuota). Un PGPR vigente con PRSTCDGO nulo (o de otro préstamo) lo ve el control y NO lo ve la herramienta.
--
-- PARAMS (cambiar en cada bloque): CORTE 2026-11-30 · DESDE 2025-06-01 (saa.cartera.prelacionDesde) · EJEC = día de la ejecución (2026-11-25).
-- =====================================================================================


-- BLOQUE 1 — ¿Hay PGPR vigentes que la herramienta no ve? Global. Esperado si la hipótesis es cierta: sin_prestamo > 0.
SELECT COUNT(*) AS pgpr_vigentes,
       SUM(CASE WHEN g.PRSTCDGO IS NULL THEN 1 ELSE 0 END) AS sin_prestamo,
       SUM(CASE WHEN g.PRSTCDGO IS NOT NULL AND g.PRSTCDGO <> d.PRSTCDGO THEN 1 ELSE 0 END) AS de_otro_prestamo,
       ROUND(SUM(CASE WHEN g.PRSTCDGO IS NULL OR g.PRSTCDGO <> d.PRSTCDGO THEN NVL(g.PGPRCPPG,0) + NVL(g.PGPRINPG,0) ELSE 0 END), 2) AS capital_interes_invisible
  FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
 WHERE NVL(g.PGPRANUL, 0) = 0;


-- BLOQUE 2 — CASO 2904 (cuotas 5 a 21). 2a: la cuota y lo que ve cada uno (control = todos los pagos de la cuota; herramienta = solo los de PRSTCDGO = 2904).
-- Si la hipótesis es cierta: cuota 5 → nucleo_pend_herramienta = 318.00 y nucleo_pend_control = 318.00 − (invisible) = −950.21 + … (el exceso inicial, más lo que la herramienta aplicó).
SELECT d.DTPRNMCT AS cuota, TRUNC(d.DTPRFCVN) AS vence, d.DTPRESTD AS estado,
       d.DTPRCPTL AS capital, d.DTPRINTR AS interes, d.DTPRDSGR AS desgravamen, d.DTPRVLSI AS seguro, d.DTPRMRAA AS mora, d.DTPRINVN AS int_vencido,
       d.DTPRTTLL AS total, d.DTPRSLOT AS slot, d.DTPRSLCP AS saldo_capital, d.DTPRSLIN AS saldo_interes,
       COUNT(g.PGPRCDGO) AS pagos_vigentes,
       ROUND(NVL(SUM(g.PGPRCPPG),0), 2) AS cap_pagado, ROUND(NVL(SUM(g.PGPRINPG),0), 2) AS int_pagado,
       ROUND(NVL(SUM(CASE WHEN g.PRSTCDGO = d.PRSTCDGO THEN NVL(g.PGPRCPPG,0) + NVL(g.PGPRINPG,0) END), 0), 2) AS cap_int_visto_por_herramienta,
       ROUND(NVL(SUM(CASE WHEN g.PRSTCDGO IS NULL OR g.PRSTCDGO <> d.PRSTCDGO THEN NVL(g.PGPRCPPG,0) + NVL(g.PGPRINPG,0) END), 0), 2) AS cap_int_invisible,
       ROUND(NVL(d.DTPRCPTL,0) + NVL(d.DTPRINTR,0) - NVL(SUM(NVL(g.PGPRCPPG,0) + NVL(g.PGPRINPG,0)), 0), 2) AS nucleo_pend_control,
       ROUND(NVL(d.DTPRCPTL,0) + NVL(d.DTPRINTR,0) - NVL(SUM(CASE WHEN g.PRSTCDGO = d.PRSTCDGO THEN NVL(g.PGPRCPPG,0) + NVL(g.PGPRINPG,0) END), 0), 2) AS nucleo_pend_herramienta
  FROM CRD.DTPR d
  LEFT JOIN CRD.PGPR g ON g.DTPRCDGO = d.DTPRCDGO AND NVL(g.PGPRANUL, 0) = 0 AND (g.PGPRFCHA IS NULL OR g.PGPRFCHA < DATE '2026-11-30' + 1)
 WHERE d.PRSTCDGO = 2904 AND d.DTPRNMCT BETWEEN 5 AND 21
 GROUP BY d.DTPRNMCT, d.DTPRFCVN, d.DTPRESTD, d.DTPRCPTL, d.DTPRINTR, d.DTPRDSGR, d.DTPRVLSI, d.DTPRMRAA, d.DTPRINVN, d.DTPRTTLL, d.DTPRSLOT, d.DTPRSLCP, d.DTPRSLIN, d.PRSTCDGO
 ORDER BY d.DTPRNMCT;

-- 2b: TODOS los PGPR (vigentes y anulados) de esas cuotas, con su rastro (anulados por C33: PGPRMTAN empieza por «C33-REIMPUTADO»).
SELECT d.DTPRNMCT AS cuota, g.PGPRCDGO AS pgpr, g.PRSTCDGO AS prst_del_pago, TRUNC(g.PGPRFCHA) AS fecha, g.PGPRVLRR AS valor,
       g.PGPRCPPG AS capital, g.PGPRINPG AS interes, g.PGPRMRPG AS mora, g.PGPRINVP AS int_venc, g.PGPRDSGR AS desgravamen, g.PGPRVLSI AS seguro,
       g.PGPRSLOT AS slot, g.PGPRTPOO AS tipo, g.PGPRANUL AS anulado, g.PGPRUSAN AS usuario_anula, g.PGPRFCAN AS fecha_anula,
       SUBSTR(g.PGPRMTAN, 1, 80) AS motivo_anulacion, SUBSTR(g.PGPROBSR, -80) AS observacion_fin, g.PGPRFCRG AS registrado
  FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
 WHERE d.PRSTCDGO = 2904 AND d.DTPRNMCT BETWEEN 5 AND 21
 ORDER BY d.DTPRNMCT, g.PGPRCDGO;

-- 2c: el cierre de la cuenta 5: Σ por componente de los pagos vigentes (control) frente a la cuota. Debe dar el exceso 950.21 del control.
SELECT ROUND(SUM(NVL(g.PGPRCPPG,0)) - MAX(d.DTPRCPTL), 2) AS exceso_capital, ROUND(SUM(NVL(g.PGPRINPG,0)) - MAX(d.DTPRINTR), 2) AS exceso_interes
  FROM CRD.DTPR d JOIN CRD.PGPR g ON g.DTPRCDGO = d.DTPRCDGO AND NVL(g.PGPRANUL, 0) = 0
 WHERE d.PRSTCDGO = 2904 AND d.DTPRNMCT = 5;


-- BLOQUE 3 — LOS PRÉSTAMOS QUE HOY BLOQUEAN (la misma consulta del control del cierre, copiada de selectControlPrelacionAlCorte), con tres columnas más:
--   invisible_cap_int  = capital + interés de pagos vigentes que la herramienta NO ve (PRSTCDGO nulo o distinto);
--   reimputado         = tiene PGPR anulados por la re-imputación C33 (motivo «C33-REIMPUTADO…») desde el día EJEC;
--   origen             = SIGUE_DE_ANTES (no se tocó: ya estaba en la lista) | TOCADO_Y_SIGUE_ROTO (se re-imputó y sigue o quedó roto).
WITH params AS (SELECT DATE '2026-11-30' AS corte, DATE '2025-06-01' AS desde, DATE '2026-11-25' AS ejec FROM DUAL),
pg AS (
    SELECT g.DTPRCDGO, SUM(NVL(g.PGPRCPPG,0) + NVL(g.PGPRINPG,0)) nuc, SUM(NVL(g.PGPRCPPG,0)) cap, SUM(NVL(g.PGPRINPG,0)) intr,
           SUM(CASE WHEN (NVL(g.PGPRTPOO,'-') IN ('PRECANCELACION','ABONO_CAPITAL','ACUERDO_CONDONACION') OR NVL(g.PGPRSLOT,0) > 0.01)
                    THEN 0 ELSE NVL(g.PGPRCPPG,0) + NVL(g.PGPRINPG,0) END) nne,
           SUM(CASE WHEN (NVL(g.PGPRTPOO,'-') IN ('PRECANCELACION','ABONO_CAPITAL','ACUERDO_CONDONACION') OR NVL(g.PGPRSLOT,0) > 0.01)
                    THEN 1 ELSE 0 END) nex
      FROM CRD.PGPR g, params pa
     WHERE NVL(g.PGPRANUL,0) = 0 AND (g.PGPRFCHA IS NULL OR g.PGPRFCHA < pa.corte + 1)
     GROUP BY g.DTPRCDGO),
c AS (
    SELECT d.PRSTCDGO, p.PRSTIDAS, p.PRSTIDST, d.DTPRNMCT, d.DTPRESTD, d.DTPRFCVN,
           NVL(d.DTPRCPTL,0) + NVL(d.DTPRINTR,0) - NVL(pg.nuc,0) nucleo, NVL(pg.nne,0) nne, NVL(pg.nex,0) nex,
           GREATEST(ROUND(NVL(pg.cap,0) - NVL(d.DTPRCPTL,0),2),0) + GREATEST(ROUND(NVL(pg.intr,0) - NVL(d.DTPRINTR,0),2),0) exceso
      FROM CRD.DTPR d JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO LEFT JOIN pg ON pg.DTPRCDGO = d.DTPRCDGO, params pa
     WHERE p.PRSTFCHA < pa.corte + 1 AND NVL(p.PRSTIDST,0) NOT IN (0, 1, 6, 7, 12, 14, 3, 4, 5, 9, 13)),
a AS (
    SELECT c.PRSTCDGO, MIN(c.DTPRNMCT) n0 FROM c, params pa
     WHERE c.nucleo > 0.01 AND ((c.DTPRESTD IS NULL OR c.DTPRESTD NOT IN (4, 7)) OR (c.DTPRESTD = 4 AND c.DTPRFCVN >= pa.desde))
     GROUP BY c.PRSTCDGO),
ctl AS (
    SELECT a.PRSTCDGO, MAX(c0.PRSTIDAS) AS operacion, 'PRELACION' AS tipo, a.n0 AS cuota, ROUND(MAX(c0.nucleo),2) AS monto, MIN(c.DTPRNMCT) AS cuota_posterior
      FROM a JOIN c c0 ON c0.PRSTCDGO = a.PRSTCDGO AND c0.DTPRNMCT = a.n0
             JOIN c ON c.PRSTCDGO = a.PRSTCDGO AND c.DTPRNMCT > a.n0 AND c.nne > 0.01
     GROUP BY a.PRSTCDGO, a.n0
    UNION ALL
    SELECT c.PRSTCDGO, MAX(c.PRSTIDAS), 'SOBREIMPUTADA', MIN(c.DTPRNMCT), ROUND(SUM(c.exceso),2), NULL
      FROM c, params pa
     WHERE c.DTPRFCVN >= pa.desde AND c.exceso > 0.01 AND c.nne > 0.01 AND c.nex = 0
     GROUP BY c.PRSTCDGO),
inv AS (
    SELECT d.PRSTCDGO, ROUND(SUM(NVL(g.PGPRCPPG,0) + NVL(g.PGPRINPG,0)), 2) AS invisible_cap_int, COUNT(*) AS pagos_invisibles
      FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO, params pa
     WHERE NVL(g.PGPRANUL,0) = 0 AND (g.PGPRFCHA IS NULL OR g.PGPRFCHA < pa.corte + 1) AND (g.PRSTCDGO IS NULL OR g.PRSTCDGO <> d.PRSTCDGO)
     GROUP BY d.PRSTCDGO),
rei AS (
    SELECT d.PRSTCDGO, COUNT(*) AS pagos_reimputados
      FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO, params pa
     WHERE g.PGPRANUL = 1 AND g.PGPRMTAN LIKE 'C33-REIMPUTADO%' AND TRUNC(g.PGPRFCAN) >= pa.ejec
     GROUP BY d.PRSTCDGO)
SELECT l.PRSTCDGO AS prestamo, l.operacion, l.tipo, l.cuota, l.monto, l.cuota_posterior,
       NVL(i.pagos_invisibles, 0) AS pagos_invisibles, NVL(i.invisible_cap_int, 0) AS invisible_cap_int,
       NVL(r.pagos_reimputados, 0) AS pagos_reimputados,
       CASE WHEN NVL(r.pagos_reimputados, 0) = 0 THEN 'SIGUE_DE_ANTES' ELSE 'TOCADO_Y_SIGUE_ROTO' END AS origen
  FROM ctl l LEFT JOIN inv i ON i.PRSTCDGO = l.PRSTCDGO LEFT JOIN rei r ON r.PRSTCDGO = l.PRSTCDGO
 ORDER BY l.tipo, l.PRSTCDGO;


-- BLOQUE 4 — RASTRO de lo que la re-imputación del día EJEC movió (sirve para la vuelta atrás; ver 395).
-- 4a: PGPR anulados por C33 (los ORIGINALES: conservan su cuota y sus componentes de antes).
SELECT d.PRSTCDGO AS prestamo, g.PGPRCDGO AS pgpr_original, d.DTPRNMCT AS cuota_original, g.DTPRCDGO AS id_cuota_original,
       g.PGPRCPPG AS capital, g.PGPRINPG AS interes, g.PGPRMRPG AS mora, g.PGPRINVP AS int_venc, g.PGPRDSGR AS desgravamen, g.PGPRVLSI AS seguro,
       g.PGPRVLRR AS valor, g.PGPRUSAN AS usuario, g.PGPRFCAN AS fecha_anula
  FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
 WHERE g.PGPRANUL = 1 AND g.PGPRMTAN LIKE 'C33-REIMPUTADO%' AND TRUNC(g.PGPRFCAN) >= DATE '2026-11-25'
 ORDER BY d.PRSTCDGO, g.PGPRCDGO;

-- 4b: los PGPR NUEVOS (creados por la re-imputación; la observación dice «[C33-REIMPUTADO de PGPR <original>, cuota #n]»).
SELECT d.PRSTCDGO AS prestamo, g.PGPRCDGO AS pgpr_nuevo, d.DTPRNMCT AS cuota_nueva,
       TO_NUMBER(REGEXP_SUBSTR(g.PGPROBSR, 'C33-REIMPUTADO de PGPR ([0-9]+)', 1, 1, NULL, 1)) AS pgpr_original,
       g.PGPRCPPG AS capital, g.PGPRINPG AS interes, g.PGPRMRPG AS mora, g.PGPRINVP AS int_venc, g.PGPRDSGR AS desgravamen, g.PGPRVLSI AS seguro,
       g.PGPRVLRR AS valor, g.PGPRANUL AS anulado
  FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
 WHERE g.PGPROBSR LIKE '%C33-REIMPUTADO de PGPR %'
 ORDER BY d.PRSTCDGO, g.PGPRCDGO;

-- 4c: movimientos de provisión (MVIC) y asientos que generó la re-imputación (origen C33_REIMPUTACION; idEmpresa 1236 ⇒ HAY asientos).
SELECT m.MVICCDGO, m.DTPRCDGO, m.MVICTPMV AS tipo_mov, m.MVICCMPN AS componente, m.MVICVLOR AS valor, TRUNC(m.MVICFCCT) AS fecha_contable,
       m.MVICORGN AS origen, m.MVICIDOR AS id_origen, m.ASNTCDGO AS asiento, m.MVICANUL AS anulado, m.MVICFCRG AS registrado
  FROM CRD.MVIC m
 WHERE m.MVICORGN = 'C33_REIMPUTACION'
 ORDER BY m.ASNTCDGO, m.MVICCDGO;
SELECT m.ASNTCDGO AS asiento, m.MVICTPMV AS tipo_mov, COUNT(*) AS filas, ROUND(SUM(m.MVICVLOR), 2) AS valor
  FROM CRD.MVIC m WHERE m.MVICORGN = 'C33_REIMPUTACION' AND m.MVICANUL = 0 GROUP BY m.ASNTCDGO, m.MVICTPMV ORDER BY 1, 2;

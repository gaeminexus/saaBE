-- =====================================================================================
-- 391 — MEDICIÓN (SOLO LECTURA): tope de la provisión de mora del paso ⑦ (D-186), POR PRÉSTAMO, reproducible a un corte pasado
-- Equipo omen-saa-1 · 2026-11-25 · Lote 7b. No modifica nada. Se puede correr en producción.
--
-- Regla (la que programan ProvisionInteresServiceImpl.calcularItems y AjusteTopeProvisionService, con TopeProvisionMora.repartir):
--   1. Tope del PRÉSTAMO = max(0, Σ cuotas del préstamo de (devengo 5 − 6, mora, fecha contable ≤ corte) − Σ mora cobrada vigente (≤ corte)
--                                  − mora condonada)           ← SIN piso por cuota: el negativo de una cuota rebaja 1.4.02 y compensa
--      la mora pendiente de las otras cuotas del mismo préstamo (lo midió el cuadre, control 3 de plantilla-conciliacion-1402.sql).
--      · mora condonada = DACC (concepto 3 = mora) .DACCVLCN de acuerdos APLICADOS (ACCNESTD = 2) con fecha ≤ corte (variante «DACC»).
--        El bloque 5 compara con la variante «tipo 9» (MVIC tipo 9 mora, la que usa el cuadre).
--   2. Reparto entre las cuotas: el tope del préstamo se reparte entre sus cuotas con mora calculada pendiente, de la más vieja a la más
--      nueva (vencimiento y código), sin pasar de la mora calculada pendiente de cada una. Σ tope repartido ≤ tope del préstamo.
--   3. Exceso de la cuota = max(0, saldo provisionado de mora − tope repartido). El ajuste tipo 10 es ese exceso.
-- Saldo provisionado de mora = Σ tipo (1, 4) − Σ tipo (2, 3, 9, 10), vigentes, fecha contable ≤ corte. Antes del ajuste, el exceso es lo
-- que hay que ajustar; después del ajuste y del ⑦ nuevo, los bloques 1 y 2 tienen que dar 0.
-- Interés: la provisión sigue al 100 % del interés pendiente (sin tope): el BLOQUE 4 lo verifica.
--
-- PARAMS: CORTE = fecha de corte (fin de mes). Cambiarla en cada bloque (CTE `params` al inicio de cada uno).
-- Aproximaciones declaradas: (a) la mora calculada usa la fórmula simple (capital × tasa/36000 × días) y no reproduce
-- saa.cartera.moraSobreCapitalPendiente; (b) el universo del reparto son las cuotas vencidas no saldadas al corte (estado distinto de 4/7/9, o
-- pagadas después del corte) más las que tienen saldo de provisión; el código usa selectCuotasProvisionablesAlCorte.
-- =====================================================================================


-- BLOQUE 1 — Exceso de provisión de mora por TIPO DE PRÉSTAMO (el monto del asiento del cuadre al corte).
WITH params AS (SELECT DATE '2025-10-31' AS corte FROM DUAL)
,
dev AS (
    SELECT m.DTPRCDGO, m.PRSTCDGO,
           SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) AS devengo
      FROM CRD.MVIC m, params pa
     WHERE m.MVICANUL = 0 AND m.MVICCMPN = 2 AND m.MVICTPMV IN (5, 6) AND m.MVICFCCT <= pa.corte
     GROUP BY m.DTPRCDGO, m.PRSTCDGO
),
pag AS (
    SELECT g.DTPRCDGO, d.PRSTCDGO, SUM(NVL(g.PGPRMRPG, 0)) AS mora_pagada
      FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO, params pa
     WHERE NVL(g.PGPRANUL, 0) = 0 AND g.PGPRFCHA < pa.corte + 1 AND d.PRSTCDGO IN (SELECT PRSTCDGO FROM dev)
     GROUP BY g.DTPRCDGO, d.PRSTCDGO
),
cond AS (
    SELECT a.PRSTCDGO, SUM(NVL(c.DACCVLCN, 0)) AS condonada
      FROM CRD.DACC c JOIN CRD.ACCN a ON a.ACCNCDGO = c.ACCNCDGO, params pa
     WHERE a.ACCNESTD = 2 AND c.DACCCPTO = 3 AND a.ACCNFCHA < pa.corte + 1
     GROUP BY a.PRSTCDGO
),
tope_pres AS (
    SELECT dv.PRSTCDGO, GREATEST(0, ROUND(dv.devengo - NVL(pg.pagada, 0) - NVL(cd.condonada, 0), 2)) AS tope
      FROM (SELECT PRSTCDGO, SUM(devengo) AS devengo FROM dev GROUP BY PRSTCDGO) dv
      LEFT JOIN (SELECT PRSTCDGO, SUM(mora_pagada) AS pagada FROM pag GROUP BY PRSTCDGO) pg ON pg.PRSTCDGO = dv.PRSTCDGO
      LEFT JOIN cond cd ON cd.PRSTCDGO = dv.PRSTCDGO
),
prov AS (
    SELECT m.DTPRCDGO, m.PRSTCDGO,
           SUM(CASE WHEN m.MVICTPMV IN (1, 4) THEN m.MVICVLOR WHEN m.MVICTPMV IN (2, 3, 9, 10) THEN -m.MVICVLOR ELSE 0 END) AS prov_mora
      FROM CRD.MVIC m, params pa
     WHERE m.MVICANUL = 0 AND m.MVICCMPN = 2 AND m.MVICFCCT <= pa.corte
     GROUP BY m.DTPRCDGO, m.PRSTCDGO
),
univ AS (
    SELECT d.DTPRCDGO, d.PRSTCDGO, d.DTPRFCVN, d.DTPRCPTL, NVL(pr.prov_mora, 0) AS prov_mora
      FROM params pa
      JOIN CRD.DTPR d ON d.PRSTCDGO IN (SELECT PRSTCDGO FROM tope_pres UNION SELECT PRSTCDGO FROM prov WHERE prov_mora > 0.009)
      LEFT JOIN prov pr ON pr.DTPRCDGO = d.DTPRCDGO
     WHERE (d.DTPRFCVN < pa.corte + 1 AND (NVL(d.DTPRESTD, 0) NOT IN (4, 7, 9) OR d.DTPRFCPG >= pa.corte + 1))
        OR NVL(pr.prov_mora, 0) > 0.009
),
calc AS (
    SELECT u.DTPRCDGO, u.PRSTCDGO, u.DTPRFCVN, u.prov_mora,
           GREATEST(0, ROUND(NVL(u.DTPRCPTL, 0) * (CASE WHEN NVL(p.PRSTINNM, 0) > 0 THEN p.PRSTINNM ELSE 9 END) / 36000
                             * GREATEST(pa.corte - TRUNC(u.DTPRFCVN), 0) - NVL(pg.mora_pagada, 0), 2)) AS calc_pend
      FROM univ u
      JOIN CRD.PRST p ON p.PRSTCDGO = u.PRSTCDGO
      LEFT JOIN pag pg ON pg.DTPRCDGO = u.DTPRCDGO
      CROSS JOIN params pa
),
rep AS (
    SELECT c.DTPRCDGO, c.PRSTCDGO, c.DTPRFCVN, c.prov_mora, c.calc_pend, NVL(tp.tope, 0) AS tope_prestamo,
           LEAST(c.calc_pend, GREATEST(0, NVL(tp.tope, 0) - (SUM(c.calc_pend) OVER (PARTITION BY c.PRSTCDGO
                 ORDER BY c.DTPRFCVN, c.DTPRCDGO) - c.calc_pend))) AS tope_cuota
      FROM calc c LEFT JOIN tope_pres tp ON tp.PRSTCDGO = c.PRSTCDGO
)
SELECT pr.TPPRCDGO AS tipo, COUNT(*) AS cuotas_con_provision_de_mora,
       ROUND(SUM(r.prov_mora), 2) AS mora_provisionada,
       ROUND(SUM(r.tope_cuota), 2) AS tope_repartido,
       ROUND(SUM(GREATEST(0, r.prov_mora - r.tope_cuota)), 2) AS exceso_a_ajustar,
       SUM(CASE WHEN r.prov_mora - r.tope_cuota > 0.004 THEN 1 ELSE 0 END) AS cuotas_con_exceso
  FROM rep r
  JOIN CRD.PRST p ON p.PRSTCDGO = r.PRSTCDGO
  JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
 WHERE r.prov_mora > 0.009
 GROUP BY pr.TPPRCDGO ORDER BY pr.TPPRCDGO;


-- BLOQUE 2 — Las 50 cuotas con más exceso, y de cuándo vencieron.
WITH params AS (SELECT DATE '2025-10-31' AS corte FROM DUAL)
,
dev AS (
    SELECT m.DTPRCDGO, m.PRSTCDGO,
           SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) AS devengo
      FROM CRD.MVIC m, params pa
     WHERE m.MVICANUL = 0 AND m.MVICCMPN = 2 AND m.MVICTPMV IN (5, 6) AND m.MVICFCCT <= pa.corte
     GROUP BY m.DTPRCDGO, m.PRSTCDGO
),
pag AS (
    SELECT g.DTPRCDGO, d.PRSTCDGO, SUM(NVL(g.PGPRMRPG, 0)) AS mora_pagada
      FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO, params pa
     WHERE NVL(g.PGPRANUL, 0) = 0 AND g.PGPRFCHA < pa.corte + 1 AND d.PRSTCDGO IN (SELECT PRSTCDGO FROM dev)
     GROUP BY g.DTPRCDGO, d.PRSTCDGO
),
cond AS (
    SELECT a.PRSTCDGO, SUM(NVL(c.DACCVLCN, 0)) AS condonada
      FROM CRD.DACC c JOIN CRD.ACCN a ON a.ACCNCDGO = c.ACCNCDGO, params pa
     WHERE a.ACCNESTD = 2 AND c.DACCCPTO = 3 AND a.ACCNFCHA < pa.corte + 1
     GROUP BY a.PRSTCDGO
),
tope_pres AS (
    SELECT dv.PRSTCDGO, GREATEST(0, ROUND(dv.devengo - NVL(pg.pagada, 0) - NVL(cd.condonada, 0), 2)) AS tope
      FROM (SELECT PRSTCDGO, SUM(devengo) AS devengo FROM dev GROUP BY PRSTCDGO) dv
      LEFT JOIN (SELECT PRSTCDGO, SUM(mora_pagada) AS pagada FROM pag GROUP BY PRSTCDGO) pg ON pg.PRSTCDGO = dv.PRSTCDGO
      LEFT JOIN cond cd ON cd.PRSTCDGO = dv.PRSTCDGO
),
prov AS (
    SELECT m.DTPRCDGO, m.PRSTCDGO,
           SUM(CASE WHEN m.MVICTPMV IN (1, 4) THEN m.MVICVLOR WHEN m.MVICTPMV IN (2, 3, 9, 10) THEN -m.MVICVLOR ELSE 0 END) AS prov_mora
      FROM CRD.MVIC m, params pa
     WHERE m.MVICANUL = 0 AND m.MVICCMPN = 2 AND m.MVICFCCT <= pa.corte
     GROUP BY m.DTPRCDGO, m.PRSTCDGO
),
univ AS (
    SELECT d.DTPRCDGO, d.PRSTCDGO, d.DTPRFCVN, d.DTPRCPTL, NVL(pr.prov_mora, 0) AS prov_mora
      FROM params pa
      JOIN CRD.DTPR d ON d.PRSTCDGO IN (SELECT PRSTCDGO FROM tope_pres UNION SELECT PRSTCDGO FROM prov WHERE prov_mora > 0.009)
      LEFT JOIN prov pr ON pr.DTPRCDGO = d.DTPRCDGO
     WHERE (d.DTPRFCVN < pa.corte + 1 AND (NVL(d.DTPRESTD, 0) NOT IN (4, 7, 9) OR d.DTPRFCPG >= pa.corte + 1))
        OR NVL(pr.prov_mora, 0) > 0.009
),
calc AS (
    SELECT u.DTPRCDGO, u.PRSTCDGO, u.DTPRFCVN, u.prov_mora,
           GREATEST(0, ROUND(NVL(u.DTPRCPTL, 0) * (CASE WHEN NVL(p.PRSTINNM, 0) > 0 THEN p.PRSTINNM ELSE 9 END) / 36000
                             * GREATEST(pa.corte - TRUNC(u.DTPRFCVN), 0) - NVL(pg.mora_pagada, 0), 2)) AS calc_pend
      FROM univ u
      JOIN CRD.PRST p ON p.PRSTCDGO = u.PRSTCDGO
      LEFT JOIN pag pg ON pg.DTPRCDGO = u.DTPRCDGO
      CROSS JOIN params pa
),
rep AS (
    SELECT c.DTPRCDGO, c.PRSTCDGO, c.DTPRFCVN, c.prov_mora, c.calc_pend, NVL(tp.tope, 0) AS tope_prestamo,
           LEAST(c.calc_pend, GREATEST(0, NVL(tp.tope, 0) - (SUM(c.calc_pend) OVER (PARTITION BY c.PRSTCDGO
                 ORDER BY c.DTPRFCVN, c.DTPRCDGO) - c.calc_pend))) AS tope_cuota
      FROM calc c LEFT JOIN tope_pres tp ON tp.PRSTCDGO = c.PRSTCDGO
)
SELECT * FROM (
    SELECT NVL(p.PRSTIDAS, p.PRSTCDGO) AS operacion, d.DTPRNMCT AS cuota, TRUNC(d.DTPRFCVN) AS vence,
           EXTRACT(YEAR FROM d.DTPRFCVN) AS anio_vence, ROUND(r.prov_mora, 2) AS mora_provisionada,
           ROUND(r.calc_pend, 2) AS mora_calculada_pendiente, ROUND(r.tope_prestamo, 2) AS tope_del_prestamo,
           ROUND(r.tope_cuota, 2) AS tope_repartido, ROUND(r.prov_mora - r.tope_cuota, 2) AS exceso
      FROM rep r
      JOIN CRD.DTPR d ON d.DTPRCDGO = r.DTPRCDGO
      JOIN CRD.PRST p ON p.PRSTCDGO = r.PRSTCDGO
     WHERE r.prov_mora - r.tope_cuota > 0.004
     ORDER BY r.prov_mora - r.tope_cuota DESC
) WHERE ROWNUM <= 50;


-- BLOQUE 3 — PRÉSTAMOS CUYA SUMA POR CUOTA ES NEGATIVA EN ALGUNA CUOTA: el neto (devengo − cobrado) de algunas cuotas es negativo y compensa a
-- las demás. Muestra, por préstamo, la suma de los negativos, la de los positivos, la mora condonada, el tope CON compensación (la regla
-- del lote 7b) y el que daría con piso por cuota (la regla anterior, para ver la diferencia).
WITH params AS (SELECT DATE '2025-10-31' AS corte FROM DUAL)
,
dev AS (
    SELECT m.DTPRCDGO, m.PRSTCDGO,
           SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) AS devengo
      FROM CRD.MVIC m, params pa
     WHERE m.MVICANUL = 0 AND m.MVICCMPN = 2 AND m.MVICTPMV IN (5, 6) AND m.MVICFCCT <= pa.corte
     GROUP BY m.DTPRCDGO, m.PRSTCDGO
),
pag AS (
    SELECT g.DTPRCDGO, d.PRSTCDGO, SUM(NVL(g.PGPRMRPG, 0)) AS mora_pagada
      FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO, params pa
     WHERE NVL(g.PGPRANUL, 0) = 0 AND g.PGPRFCHA < pa.corte + 1 AND d.PRSTCDGO IN (SELECT PRSTCDGO FROM dev)
     GROUP BY g.DTPRCDGO, d.PRSTCDGO
),
cond AS (
    SELECT a.PRSTCDGO, SUM(NVL(c.DACCVLCN, 0)) AS condonada
      FROM CRD.DACC c JOIN CRD.ACCN a ON a.ACCNCDGO = c.ACCNCDGO, params pa
     WHERE a.ACCNESTD = 2 AND c.DACCCPTO = 3 AND a.ACCNFCHA < pa.corte + 1
     GROUP BY a.PRSTCDGO
),
tope_pres AS (
    SELECT dv.PRSTCDGO, GREATEST(0, ROUND(dv.devengo - NVL(pg.pagada, 0) - NVL(cd.condonada, 0), 2)) AS tope
      FROM (SELECT PRSTCDGO, SUM(devengo) AS devengo FROM dev GROUP BY PRSTCDGO) dv
      LEFT JOIN (SELECT PRSTCDGO, SUM(mora_pagada) AS pagada FROM pag GROUP BY PRSTCDGO) pg ON pg.PRSTCDGO = dv.PRSTCDGO
      LEFT JOIN cond cd ON cd.PRSTCDGO = dv.PRSTCDGO
),
prov AS (
    SELECT m.DTPRCDGO, m.PRSTCDGO,
           SUM(CASE WHEN m.MVICTPMV IN (1, 4) THEN m.MVICVLOR WHEN m.MVICTPMV IN (2, 3, 9, 10) THEN -m.MVICVLOR ELSE 0 END) AS prov_mora
      FROM CRD.MVIC m, params pa
     WHERE m.MVICANUL = 0 AND m.MVICCMPN = 2 AND m.MVICFCCT <= pa.corte
     GROUP BY m.DTPRCDGO, m.PRSTCDGO
),
univ AS (
    SELECT d.DTPRCDGO, d.PRSTCDGO, d.DTPRFCVN, d.DTPRCPTL, NVL(pr.prov_mora, 0) AS prov_mora
      FROM params pa
      JOIN CRD.DTPR d ON d.PRSTCDGO IN (SELECT PRSTCDGO FROM tope_pres UNION SELECT PRSTCDGO FROM prov WHERE prov_mora > 0.009)
      LEFT JOIN prov pr ON pr.DTPRCDGO = d.DTPRCDGO
     WHERE (d.DTPRFCVN < pa.corte + 1 AND (NVL(d.DTPRESTD, 0) NOT IN (4, 7, 9) OR d.DTPRFCPG >= pa.corte + 1))
        OR NVL(pr.prov_mora, 0) > 0.009
),
calc AS (
    SELECT u.DTPRCDGO, u.PRSTCDGO, u.DTPRFCVN, u.prov_mora,
           GREATEST(0, ROUND(NVL(u.DTPRCPTL, 0) * (CASE WHEN NVL(p.PRSTINNM, 0) > 0 THEN p.PRSTINNM ELSE 9 END) / 36000
                             * GREATEST(pa.corte - TRUNC(u.DTPRFCVN), 0) - NVL(pg.mora_pagada, 0), 2)) AS calc_pend
      FROM univ u
      JOIN CRD.PRST p ON p.PRSTCDGO = u.PRSTCDGO
      LEFT JOIN pag pg ON pg.DTPRCDGO = u.DTPRCDGO
      CROSS JOIN params pa
),
rep AS (
    SELECT c.DTPRCDGO, c.PRSTCDGO, c.DTPRFCVN, c.prov_mora, c.calc_pend, NVL(tp.tope, 0) AS tope_prestamo,
           LEAST(c.calc_pend, GREATEST(0, NVL(tp.tope, 0) - (SUM(c.calc_pend) OVER (PARTITION BY c.PRSTCDGO
                 ORDER BY c.DTPRFCVN, c.DTPRCDGO) - c.calc_pend))) AS tope_cuota
      FROM calc c LEFT JOIN tope_pres tp ON tp.PRSTCDGO = c.PRSTCDGO
)
, neto AS (
    SELECT DTPRCDGO, PRSTCDGO, SUM(devengo) AS devengo, SUM(pagada) AS pagada
      FROM (SELECT DTPRCDGO, PRSTCDGO, devengo, 0 AS pagada FROM dev
            UNION ALL SELECT DTPRCDGO, PRSTCDGO, 0, mora_pagada FROM pag)
     GROUP BY DTPRCDGO, PRSTCDGO
)
SELECT * FROM (
    SELECT NVL(p.PRSTIDAS, p.PRSTCDGO) AS operacion, COUNT(*) AS cuotas,
           SUM(CASE WHEN n.devengo - n.pagada < -0.004 THEN 1 ELSE 0 END) AS cuotas_negativas,
           ROUND(SUM(CASE WHEN n.devengo - n.pagada < 0 THEN n.devengo - n.pagada ELSE 0 END), 2) AS suma_negativos,
           ROUND(SUM(CASE WHEN n.devengo - n.pagada > 0 THEN n.devengo - n.pagada ELSE 0 END), 2) AS suma_positivos,
           ROUND(NVL(MAX(cd.condonada), 0), 2) AS mora_condonada,
           GREATEST(0, ROUND(SUM(n.devengo - n.pagada) - NVL(MAX(cd.condonada), 0), 2)) AS tope_con_compensacion,
           GREATEST(0, ROUND(SUM(CASE WHEN n.devengo - n.pagada > 0 THEN n.devengo - n.pagada ELSE 0 END) - NVL(MAX(cd.condonada), 0), 2)) AS tope_con_piso_por_cuota
      FROM neto n
      JOIN CRD.PRST p ON p.PRSTCDGO = n.PRSTCDGO
      LEFT JOIN cond cd ON cd.PRSTCDGO = n.PRSTCDGO
     GROUP BY NVL(p.PRSTIDAS, p.PRSTCDGO)
    HAVING SUM(CASE WHEN n.devengo - n.pagada < -0.004 THEN 1 ELSE 0 END) > 0
     ORDER BY ROUND(SUM(CASE WHEN n.devengo - n.pagada < 0 THEN n.devengo - n.pagada ELSE 0 END), 2)
) WHERE ROWNUM <= 200;


-- BLOQUE 4 — INTERÉS: la provisión sigue al 100 % del interés pendiente. Esperado: 0 filas con diferencia (salvo cuotas en
-- DE_PLAZO_VENCIDO con interés futuro condonado, que el código excluye y este SQL no).
WITH params AS (SELECT DATE '2025-10-31' AS corte FROM DUAL),
mv AS (
    SELECT m.DTPRCDGO,
           SUM(CASE WHEN m.MVICTPMV IN (1, 4) THEN m.MVICVLOR WHEN m.MVICTPMV IN (2, 3, 9, 10) THEN -m.MVICVLOR ELSE 0 END) AS prov_interes
      FROM CRD.MVIC m, params pa
     WHERE m.MVICANUL = 0 AND m.MVICCMPN = 1 AND m.MVICFCCT <= pa.corte
     GROUP BY m.DTPRCDGO
),
pg AS (
    SELECT g.DTPRCDGO, SUM(NVL(g.PGPRINPG, 0) + NVL(g.PGPRINVP, 0)) AS interes_pagado
      FROM CRD.PGPR g, params pa
     WHERE NVL(g.PGPRANUL, 0) = 0 AND g.PGPRFCHA < pa.corte + 1
     GROUP BY g.DTPRCDGO
)
SELECT * FROM (
    SELECT NVL(p.PRSTIDAS, p.PRSTCDGO) AS operacion, d.DTPRNMCT AS cuota, TRUNC(d.DTPRFCVN) AS vence,
           ROUND(mv.prov_interes, 2) AS interes_provisionado,
           GREATEST(0, ROUND(NVL(d.DTPRINTR, 0) + NVL(d.DTPRINVN, 0) - NVL(pg.interes_pagado, 0), 2)) AS interes_pendiente,
           ROUND(mv.prov_interes - GREATEST(0, NVL(d.DTPRINTR, 0) + NVL(d.DTPRINVN, 0) - NVL(pg.interes_pagado, 0)), 2) AS diferencia
      FROM params pa
      JOIN mv ON ABS(mv.prov_interes) > 0.004
      JOIN CRD.DTPR d ON d.DTPRCDGO = mv.DTPRCDGO
      JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
      LEFT JOIN pg ON pg.DTPRCDGO = d.DTPRCDGO
     WHERE ABS(mv.prov_interes - GREATEST(0, NVL(d.DTPRINTR, 0) + NVL(d.DTPRINVN, 0) - NVL(pg.interes_pagado, 0))) > 0.01
     ORDER BY ABS(mv.prov_interes - GREATEST(0, NVL(d.DTPRINTR, 0) + NVL(d.DTPRINVN, 0) - NVL(pg.interes_pagado, 0))) DESC
) WHERE ROWNUM <= 50;


-- BLOQUE 4b — Ajustes tipo 10 ya grabados (después del ajuste): por asiento y tipo de préstamo. Antes del ajuste: 0 filas.
SELECT m.ASNTCDGO AS asiento, pr.TPPRCDGO AS tipo, COUNT(*) AS cuotas, ROUND(SUM(m.MVICVLOR), 2) AS ajuste
  FROM CRD.MVIC m
  JOIN CRD.PRST p ON p.PRSTCDGO = m.PRSTCDGO
  JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
 WHERE m.MVICTPMV = 10 AND m.MVICANUL = 0
 GROUP BY m.ASNTCDGO, pr.TPPRCDGO ORDER BY 1, 2;


-- BLOQUE 5 — LA MORA CONTABILIZADA por tipo de préstamo con las DOS variantes de mora condonada, para compararla con el saldo de 1.4.02 y con el
-- control 3 del cuadre (42.566,11 al 30-sep-2025):
--   · «DACC»   : la mora condonada real de los acuerdos APLICADOS (DACCVLCN, concepto mora).
--   · «tipo 9» : el MVIC tipo 9 (REVERSO_POR_CONDONACION) de mora, que la usa el cuadre (acotado a lo provisionado).
-- Por tipo de préstamo y, en la segunda consulta, por préstamo (los 50 con más diferencia entre las dos variantes).
WITH params AS (SELECT DATE '2025-09-30' AS corte FROM DUAL),
dev AS (
    SELECT m.PRSTCDGO, SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) AS devengo
      FROM CRD.MVIC m, params pa
     WHERE m.MVICANUL = 0 AND m.MVICCMPN = 2 AND m.MVICTPMV IN (5, 6) AND m.MVICFCCT <= pa.corte
     GROUP BY m.PRSTCDGO
),
pag AS (
    SELECT d.PRSTCDGO, SUM(NVL(g.PGPRMRPG, 0)) AS pagada
      FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO, params pa
     WHERE NVL(g.PGPRANUL, 0) = 0 AND g.PGPRFCHA < pa.corte + 1 AND d.PRSTCDGO IN (SELECT PRSTCDGO FROM dev)
     GROUP BY d.PRSTCDGO
),
cdacc AS (
    SELECT a.PRSTCDGO, SUM(NVL(c.DACCVLCN, 0)) AS condonada
      FROM CRD.DACC c JOIN CRD.ACCN a ON a.ACCNCDGO = c.ACCNCDGO, params pa
     WHERE a.ACCNESTD = 2 AND c.DACCCPTO = 3 AND a.ACCNFCHA < pa.corte + 1
     GROUP BY a.PRSTCDGO
),
c9 AS (
    SELECT m.PRSTCDGO, SUM(m.MVICVLOR) AS condonada
      FROM CRD.MVIC m, params pa
     WHERE m.MVICANUL = 0 AND m.MVICCMPN = 2 AND m.MVICTPMV = 9 AND m.MVICFCCT <= pa.corte
     GROUP BY m.PRSTCDGO
),
pres AS (
    SELECT dv.PRSTCDGO, dv.devengo, NVL(pg.pagada, 0) AS pagada, NVL(cd.condonada, 0) AS cond_dacc, NVL(c9.condonada, 0) AS cond_9,
           GREATEST(0, ROUND(dv.devengo - NVL(pg.pagada, 0) - NVL(cd.condonada, 0), 2)) AS mora_dacc,
           GREATEST(0, ROUND(dv.devengo - NVL(pg.pagada, 0) - NVL(c9.condonada, 0), 2)) AS mora_tipo9
      FROM dev dv LEFT JOIN pag pg ON pg.PRSTCDGO = dv.PRSTCDGO LEFT JOIN cdacc cd ON cd.PRSTCDGO = dv.PRSTCDGO
      LEFT JOIN c9 ON c9.PRSTCDGO = dv.PRSTCDGO
)
SELECT pr.TPPRCDGO AS tipo, COUNT(*) AS prestamos, ROUND(SUM(x.devengo), 2) AS devengo_neto, ROUND(SUM(x.pagada), 2) AS mora_cobrada,
       ROUND(SUM(x.cond_dacc), 2) AS condonada_dacc, ROUND(SUM(x.cond_9), 2) AS condonada_tipo9,
       ROUND(SUM(x.mora_dacc), 2) AS mora_contabilizada_variante_dacc, ROUND(SUM(x.mora_tipo9), 2) AS mora_contabilizada_variante_tipo9
  FROM pres x
  JOIN CRD.PRST p ON p.PRSTCDGO = x.PRSTCDGO
  JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
 GROUP BY pr.TPPRCDGO ORDER BY pr.TPPRCDGO;

-- Los 50 préstamos con más diferencia entre las dos variantes (donde la condonación real y el tipo 9 se separan).
WITH params AS (SELECT DATE '2025-09-30' AS corte FROM DUAL),
dev AS (
    SELECT m.PRSTCDGO, SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) AS devengo
      FROM CRD.MVIC m, params pa
     WHERE m.MVICANUL = 0 AND m.MVICCMPN = 2 AND m.MVICTPMV IN (5, 6) AND m.MVICFCCT <= pa.corte
     GROUP BY m.PRSTCDGO
),
pag AS (
    SELECT d.PRSTCDGO, SUM(NVL(g.PGPRMRPG, 0)) AS pagada
      FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO, params pa
     WHERE NVL(g.PGPRANUL, 0) = 0 AND g.PGPRFCHA < pa.corte + 1 AND d.PRSTCDGO IN (SELECT PRSTCDGO FROM dev)
     GROUP BY d.PRSTCDGO
),
cdacc AS (
    SELECT a.PRSTCDGO, SUM(NVL(c.DACCVLCN, 0)) AS condonada
      FROM CRD.DACC c JOIN CRD.ACCN a ON a.ACCNCDGO = c.ACCNCDGO, params pa
     WHERE a.ACCNESTD = 2 AND c.DACCCPTO = 3 AND a.ACCNFCHA < pa.corte + 1
     GROUP BY a.PRSTCDGO
),
c9 AS (
    SELECT m.PRSTCDGO, SUM(m.MVICVLOR) AS condonada
      FROM CRD.MVIC m, params pa
     WHERE m.MVICANUL = 0 AND m.MVICCMPN = 2 AND m.MVICTPMV = 9 AND m.MVICFCCT <= pa.corte
     GROUP BY m.PRSTCDGO
)
SELECT * FROM (
    SELECT NVL(p.PRSTIDAS, p.PRSTCDGO) AS operacion, ROUND(dv.devengo, 2) AS devengo_neto, ROUND(NVL(pg.pagada, 0), 2) AS mora_cobrada,
           ROUND(NVL(cd.condonada, 0), 2) AS condonada_dacc, ROUND(NVL(c9.condonada, 0), 2) AS condonada_tipo9,
           ROUND(NVL(cd.condonada, 0) - NVL(c9.condonada, 0), 2) AS diferencia
      FROM dev dv
      JOIN CRD.PRST p ON p.PRSTCDGO = dv.PRSTCDGO
      LEFT JOIN pag pg ON pg.PRSTCDGO = dv.PRSTCDGO
      LEFT JOIN cdacc cd ON cd.PRSTCDGO = dv.PRSTCDGO
      LEFT JOIN c9 ON c9.PRSTCDGO = dv.PRSTCDGO
     WHERE ABS(NVL(cd.condonada, 0) - NVL(c9.condonada, 0)) > 0.004
     ORDER BY ABS(NVL(cd.condonada, 0) - NVL(c9.condonada, 0)) DESC
) WHERE ROWNUM <= 50;

-- Saldo de las cuentas 1.4.02 al corte (activos o mayorizados, fecha ≤ corte), por cuenta. (La 1.4.02 también lleva el interés ordinario por
-- cobrar: comparar con la mora que el cuadre separó, no con el saldo entero.)
SELECT d.DTASCNTA AS cuenta, ROUND(SUM(NVL(d.DTASDBEE, 0)) - SUM(NVL(d.DTASHBRR, 0)), 2) AS saldo_deudor
  FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
 WHERE a.PJRQCDGO = 1236 AND a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < DATE '2025-09-30' + 1 AND d.DTASCNTA LIKE '1.4.02%'
 GROUP BY d.DTASCNTA ORDER BY d.DTASCNTA;

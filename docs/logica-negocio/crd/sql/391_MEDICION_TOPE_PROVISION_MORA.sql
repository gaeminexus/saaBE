-- =====================================================================================
-- 391 — MEDICIÓN (SOLO LECTURA): tope de la provisión de mora del paso ⑦ (D-186), reproducible a un corte pasado
-- Equipo omen-saa-1 · 2026-11-25 · Lote 7. No modifica nada. Se puede correr en producción.
--
-- Regla (por cuota, componente MORA del libro CRD.MVIC), la misma que programa ProvisionInteresServiceImpl.calcularItems y que usa
-- AjusteTopeProvisionService:
--   mora contabilizada pendiente = max(0, devengo(5 − 6, fecha contable ≤ corte) − mora cobrada vigente (≤ corte) − mora condonada asignada)
--      · mora condonada = DACC (concepto 3 = mora) .DACCVLCN de los acuerdos APLICADOS (ACCNESTD = 2) con fecha ≤ corte, por préstamo,
--        asignada a las cuotas del préstamo de la más vieja a la más nueva (vencimiento y código) contra su mora contabilizada.
--   mora calculada pendiente     = max(0, capital × tasa/36000 × días vencida − mora cobrada)   (fórmula simple de calcularMoraCuota)
--   tope                         = min(calculada pendiente, contabilizada pendiente)
--   exceso                       = max(0, saldo provisionado de mora − tope)
-- Saldo provisionado de mora = Σ tipo (1, 4) − Σ tipo (2, 3, 9, 10), vigentes, fecha contable ≤ corte. El tipo 10
-- «AJUSTE_TOPE_PROVISION» no existe hasta correr sql/392: antes del ajuste el exceso es lo que hay que ajustar; después del ajuste y del
-- ⑦ nuevo, los bloques 1 y 2 tienen que dar 0.
-- Interés: la provisión sigue al 100 % del interés pendiente (sin tope): el BLOQUE 3 lo verifica.
--
-- PARAMS: CORTE = fecha de corte (fin de mes). Cambiarla en cada bloque (aparece en un CTE `params` al inicio de cada uno).
-- Aproximación declarada: con saa.cartera.moraSobreCapitalPendiente encendida, la mora calculada del código difiere de la fórmula
-- simple de este SQL; el tope por la mora contabilizada (lo que importa para el exceso) no depende de esa fórmula.
-- =====================================================================================


-- BLOQUE 1 — Exceso de provisión de mora por TIPO DE PRÉSTAMO (lo que el ajuste tipo 10 suma, y lo que el cuadre contabiliza).
WITH params AS (SELECT DATE '2025-10-31' AS corte FROM DUAL),
dev AS (
    SELECT m.DTPRCDGO, m.PRSTCDGO,
           SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) AS devengo
      FROM CRD.MVIC m, params pa
     WHERE m.MVICANUL = 0 AND m.MVICCMPN = 2 AND m.MVICTPMV IN (5, 6) AND m.MVICFCCT <= pa.corte
     GROUP BY m.DTPRCDGO, m.PRSTCDGO
    HAVING SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) > 0
),
pag AS (
    SELECT g.DTPRCDGO, SUM(NVL(g.PGPRMRPG, 0)) AS mora_pagada
      FROM CRD.PGPR g, params pa
     WHERE NVL(g.PGPRANUL, 0) = 0 AND g.PGPRFCHA < pa.corte + 1
     GROUP BY g.DTPRCDGO
),
cond AS (
    SELECT a.PRSTCDGO, SUM(NVL(c.DACCVLCN, 0)) AS condonada
      FROM CRD.DACC c JOIN CRD.ACCN a ON a.ACCNCDGO = c.ACCNCDGO, params pa
     WHERE a.ACCNESTD = 2 AND c.DACCCPTO = 3 AND a.ACCNFCHA < pa.corte + 1
     GROUP BY a.PRSTCDGO
),
base AS (
    SELECT dv.DTPRCDGO, dv.PRSTCDGO, d.DTPRFCVN,
           GREATEST(0, ROUND(dv.devengo - NVL(pg.mora_pagada, 0), 2)) AS pend, NVL(cd.condonada, 0) AS condonada
      FROM dev dv JOIN CRD.DTPR d ON d.DTPRCDGO = dv.DTPRCDGO
      LEFT JOIN pag pg ON pg.DTPRCDGO = dv.DTPRCDGO
      LEFT JOIN cond cd ON cd.PRSTCDGO = dv.PRSTCDGO
),
contab AS (
    SELECT b.DTPRCDGO,
           GREATEST(0, ROUND(b.pend - LEAST(b.pend, GREATEST(0, b.condonada - (SUM(b.pend) OVER (PARTITION BY b.PRSTCDGO
                 ORDER BY b.DTPRFCVN, b.DTPRCDGO) - b.pend))), 2)) AS contab_pend
      FROM base b
),
prov AS (
    SELECT m.DTPRCDGO,
           SUM(CASE WHEN m.MVICTPMV IN (1, 4) THEN m.MVICVLOR WHEN m.MVICTPMV IN (2, 3, 9, 10) THEN -m.MVICVLOR ELSE 0 END) AS prov_mora
      FROM CRD.MVIC m, params pa
     WHERE m.MVICANUL = 0 AND m.MVICCMPN = 2 AND m.MVICFCCT <= pa.corte
     GROUP BY m.DTPRCDGO
    HAVING SUM(CASE WHEN m.MVICTPMV IN (1, 4) THEN m.MVICVLOR WHEN m.MVICTPMV IN (2, 3, 9, 10) THEN -m.MVICVLOR ELSE 0 END) > 0.009
),
cuota AS (
    SELECT d.DTPRCDGO, pr.TPPRCDGO AS tipo, pv.prov_mora, NVL(ct.contab_pend, 0) AS contab_pend,
           GREATEST(0, ROUND(NVL(d.DTPRCPTL, 0) * (CASE WHEN NVL(p.PRSTINNM, 0) > 0 THEN p.PRSTINNM ELSE 9 END) / 36000
                             * GREATEST(pa.corte - TRUNC(d.DTPRFCVN), 0) - NVL(pg.mora_pagada, 0), 2)) AS calc_pend
      FROM params pa
      JOIN prov pv ON 1 = 1
      JOIN CRD.DTPR d ON d.DTPRCDGO = pv.DTPRCDGO
      JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
      JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
      LEFT JOIN contab ct ON ct.DTPRCDGO = d.DTPRCDGO
      LEFT JOIN pag pg ON pg.DTPRCDGO = d.DTPRCDGO
)
SELECT tipo, COUNT(*) AS cuotas_con_provision_de_mora,
       ROUND(SUM(prov_mora), 2) AS mora_provisionada,
       ROUND(SUM(contab_pend), 2) AS mora_contabilizada_pendiente,
       ROUND(SUM(LEAST(calc_pend, contab_pend)), 2) AS tope_total,
       ROUND(SUM(GREATEST(0, prov_mora - LEAST(calc_pend, contab_pend))), 2) AS exceso_a_ajustar,
       SUM(CASE WHEN prov_mora - LEAST(calc_pend, contab_pend) > 0.004 THEN 1 ELSE 0 END) AS cuotas_con_exceso
  FROM cuota
 GROUP BY tipo ORDER BY tipo;


-- BLOQUE 2 — Las 50 cuotas con más exceso, y de cuándo vencieron.
WITH params AS (SELECT DATE '2025-10-31' AS corte FROM DUAL),
dev AS (
    SELECT m.DTPRCDGO, m.PRSTCDGO,
           SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) AS devengo
      FROM CRD.MVIC m, params pa
     WHERE m.MVICANUL = 0 AND m.MVICCMPN = 2 AND m.MVICTPMV IN (5, 6) AND m.MVICFCCT <= pa.corte
     GROUP BY m.DTPRCDGO, m.PRSTCDGO
    HAVING SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) > 0
),
pag AS (
    SELECT g.DTPRCDGO, SUM(NVL(g.PGPRMRPG, 0)) AS mora_pagada
      FROM CRD.PGPR g, params pa
     WHERE NVL(g.PGPRANUL, 0) = 0 AND g.PGPRFCHA < pa.corte + 1
     GROUP BY g.DTPRCDGO
),
cond AS (
    SELECT a.PRSTCDGO, SUM(NVL(c.DACCVLCN, 0)) AS condonada
      FROM CRD.DACC c JOIN CRD.ACCN a ON a.ACCNCDGO = c.ACCNCDGO, params pa
     WHERE a.ACCNESTD = 2 AND c.DACCCPTO = 3 AND a.ACCNFCHA < pa.corte + 1
     GROUP BY a.PRSTCDGO
),
base AS (
    SELECT dv.DTPRCDGO, dv.PRSTCDGO, d.DTPRFCVN,
           GREATEST(0, ROUND(dv.devengo - NVL(pg.mora_pagada, 0), 2)) AS pend, NVL(cd.condonada, 0) AS condonada
      FROM dev dv JOIN CRD.DTPR d ON d.DTPRCDGO = dv.DTPRCDGO
      LEFT JOIN pag pg ON pg.DTPRCDGO = dv.DTPRCDGO
      LEFT JOIN cond cd ON cd.PRSTCDGO = dv.PRSTCDGO
),
contab AS (
    SELECT b.DTPRCDGO,
           GREATEST(0, ROUND(b.pend - LEAST(b.pend, GREATEST(0, b.condonada - (SUM(b.pend) OVER (PARTITION BY b.PRSTCDGO
                 ORDER BY b.DTPRFCVN, b.DTPRCDGO) - b.pend))), 2)) AS contab_pend
      FROM base b
),
prov AS (
    SELECT m.DTPRCDGO,
           SUM(CASE WHEN m.MVICTPMV IN (1, 4) THEN m.MVICVLOR WHEN m.MVICTPMV IN (2, 3, 9, 10) THEN -m.MVICVLOR ELSE 0 END) AS prov_mora
      FROM CRD.MVIC m, params pa
     WHERE m.MVICANUL = 0 AND m.MVICCMPN = 2 AND m.MVICFCCT <= pa.corte
     GROUP BY m.DTPRCDGO
    HAVING SUM(CASE WHEN m.MVICTPMV IN (1, 4) THEN m.MVICVLOR WHEN m.MVICTPMV IN (2, 3, 9, 10) THEN -m.MVICVLOR ELSE 0 END) > 0.009
)
SELECT * FROM (
    SELECT NVL(p.PRSTIDAS, p.PRSTCDGO) AS operacion, d.DTPRNMCT AS cuota, TRUNC(d.DTPRFCVN) AS vence,
           EXTRACT(YEAR FROM d.DTPRFCVN) AS anio_vence, ROUND(pv.prov_mora, 2) AS mora_provisionada,
           NVL(ct.contab_pend, 0) AS mora_contabilizada_pendiente,
           ROUND(pv.prov_mora - NVL(ct.contab_pend, 0), 2) AS exceso_vs_contabilizada
      FROM params pa
      JOIN prov pv ON 1 = 1
      JOIN CRD.DTPR d ON d.DTPRCDGO = pv.DTPRCDGO
      JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
      LEFT JOIN contab ct ON ct.DTPRCDGO = d.DTPRCDGO
     WHERE pv.prov_mora - NVL(ct.contab_pend, 0) > 0.004
     ORDER BY pv.prov_mora - NVL(ct.contab_pend, 0) DESC
) WHERE ROWNUM <= 50;


-- BLOQUE 3 — INTERÉS: la provisión sigue al 100 % del interés pendiente. Esperado: 0 filas con diferencia (salvo cuotas en
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


-- BLOQUE 4 — Ajustes tipo 10 ya grabados (después del ajuste): por asiento y tipo de préstamo. Antes del ajuste: 0 filas.
SELECT m.ASNTCDGO AS asiento, pr.TPPRCDGO AS tipo, COUNT(*) AS cuotas, ROUND(SUM(m.MVICVLOR), 2) AS ajuste
  FROM CRD.MVIC m
  JOIN CRD.PRST p ON p.PRSTCDGO = m.PRSTCDGO
  JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
 WHERE m.MVICTPMV = 10 AND m.MVICANUL = 0
 GROUP BY m.ASNTCDGO, pr.TPPRCDGO ORDER BY 1, 2;


-- BLOQUE 5 — LA MORA CONTABILIZADA calculada así, POR TIPO DE PRÉSTAMO, para compararla con el saldo de 1.4.02 al corte
-- (el cuadre midió 42.566,11 al 30-sep-2025). Primera consulta: la mora contabilizada pendiente por tipo (Σ de las cuotas);
-- segunda: el saldo de las cuentas 1.4.02 por cuenta a la misma fecha, para ver si coinciden (la 1.4.02 también lleva el interés
-- ordinario por cobrar, no solo la mora: comparar con el desglose del cuadre).
WITH params AS (SELECT DATE '2025-09-30' AS corte FROM DUAL),
dev AS (
    SELECT m.DTPRCDGO, m.PRSTCDGO,
           SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) AS devengo
      FROM CRD.MVIC m, params pa
     WHERE m.MVICANUL = 0 AND m.MVICCMPN = 2 AND m.MVICTPMV IN (5, 6) AND m.MVICFCCT <= pa.corte
     GROUP BY m.DTPRCDGO, m.PRSTCDGO
    HAVING SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) > 0
),
pag AS (
    SELECT g.DTPRCDGO, SUM(NVL(g.PGPRMRPG, 0)) AS mora_pagada
      FROM CRD.PGPR g, params pa
     WHERE NVL(g.PGPRANUL, 0) = 0 AND g.PGPRFCHA < pa.corte + 1
     GROUP BY g.DTPRCDGO
),
cond AS (
    SELECT a.PRSTCDGO, SUM(NVL(c.DACCVLCN, 0)) AS condonada
      FROM CRD.DACC c JOIN CRD.ACCN a ON a.ACCNCDGO = c.ACCNCDGO, params pa
     WHERE a.ACCNESTD = 2 AND c.DACCCPTO = 3 AND a.ACCNFCHA < pa.corte + 1
     GROUP BY a.PRSTCDGO
),
base AS (
    SELECT dv.DTPRCDGO, dv.PRSTCDGO, d.DTPRFCVN, dv.devengo,
           GREATEST(0, ROUND(dv.devengo - NVL(pg.mora_pagada, 0), 2)) AS pend, NVL(cd.condonada, 0) AS condonada
      FROM dev dv JOIN CRD.DTPR d ON d.DTPRCDGO = dv.DTPRCDGO
      LEFT JOIN pag pg ON pg.DTPRCDGO = dv.DTPRCDGO
      LEFT JOIN cond cd ON cd.PRSTCDGO = dv.PRSTCDGO
),
contab AS (
    SELECT b.DTPRCDGO, b.PRSTCDGO, b.devengo, b.pend,
           GREATEST(0, ROUND(b.pend - LEAST(b.pend, GREATEST(0, b.condonada - (SUM(b.pend) OVER (PARTITION BY b.PRSTCDGO
                 ORDER BY b.DTPRFCVN, b.DTPRCDGO) - b.pend))), 2)) AS contab_pend
      FROM base b
)
SELECT pr.TPPRCDGO AS tipo, COUNT(*) AS cuotas, ROUND(SUM(c.devengo), 2) AS devengo_neto,
       ROUND(SUM(c.pend), 2) AS menos_mora_cobrada, ROUND(SUM(c.contab_pend), 2) AS menos_mora_condonada_mora_contabilizada
  FROM contab c
  JOIN CRD.PRST p ON p.PRSTCDGO = c.PRSTCDGO
  JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
 GROUP BY pr.TPPRCDGO ORDER BY pr.TPPRCDGO;

-- Saldo de las cuentas 1.4.02 al corte (activos o mayorizados, fecha ≤ corte), por cuenta.
SELECT d.DTASCNTA AS cuenta, ROUND(SUM(NVL(d.DTASDBEE, 0)) - SUM(NVL(d.DTASHBRR, 0)), 2) AS saldo_deudor
  FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
 WHERE a.PJRQCDGO = 1236 AND a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < DATE '2025-09-30' + 1 AND d.DTASCNTA LIKE '1.4.02%'
 GROUP BY d.DTASCNTA ORDER BY d.DTASCNTA;

-- D-206: asiento de ajuste al 1-dic-2025 para que 1.4.02 = 1.4.99.05 al centavo con la regla nueva, sobre el estado al 30-nov-2025.
-- Regla: la 1.4.02 vale lo devengado y no cobrado por cuota al corte (interés de las cuotas con vencimiento <= corte, impagas,
-- de préstamos vivos = «iv_corte» de la conciliación C27b) más la mora contabilizada pendiente; la provisión vale lo mismo.
-- Base: plantilla-conciliacion-1402.sql (C27b), bloque 01, con __CORTE__ = 30-nov-2025. Solo SELECT. Sin datos personales.
-- Bloque 01: por tipo, saldo contable, objetivo y ajuste de 1.4.02, y su separación (reverso de más oct–nov, arranque, resto).
-- Bloque 02: provisión 1.4.99.05 contable contra el objetivo (suma de los tres tipos).

-- 01 1.4.02 por tipo: contable, objetivo y ajuste
WITH pr AS (SELECT DATE '2025-11-30' corte, DATE '2025-11-30' + 1 corteSig, DATE '2025-05-31' ancla, DATE '2025-06-01' anclaSig FROM dual),
fe AS (SELECT ADD_MONTHS(DATE '2025-05-31', LEVEL - 1) f FROM dual CONNECT BY ADD_MONTHS(DATE '2025-05-31', LEVEL - 1) <= (SELECT corte FROM pr)),
lu AS (SELECT p.PRSTCDGO, fe.f,
  CASE WHEN p.PRSTFCHA < fe.f + 1 AND NVL(p.PRSTIDST,0) NOT IN (0, 1, 6, 7, 12, 14)
   AND NOT (NVL(p.PRSTIDST,0) IN (3, 4, 5, 9, 13)
    AND NOT EXISTS (SELECT 1 FROM CRD.DTPR cx WHERE cx.PRSTCDGO = p.PRSTCDGO AND NVL(cx.DTPRESTD,0) NOT IN (4, 7, 9))
    AND NOT EXISTS (SELECT 1 FROM CRD.DTPR cy WHERE cy.PRSTCDGO = p.PRSTCDGO AND cy.DTPRESTD = 4 AND cy.DTPRFCPG >= fe.f)
    AND NOT EXISTS (SELECT 1 FROM CRD.PGPR cz WHERE cz.PRSTCDGO = p.PRSTCDGO AND NVL(cz.PGPRANUL,0) = 0 AND cz.PGPRFCHA >= fe.f + 1))
  THEN 1 ELSE 0 END u,
  (SELECT MIN(v.PLVNFCCR) FROM CRD.PLVN v WHERE v.PRSTCDGO = p.PRSTCDGO AND v.PLVNESTD IN (1, 2) AND v.PLVNFCCR < fe.f + 1) plvn
  FROM CRD.PRST p CROSS JOIN fe),
pgc AS (SELECT x.DTPRCDGO,
  SUM(CASE WHEN x.PGPRFCHA < pr.anclaSig THEN NVL(x.PGPRINPG,0) ELSE 0 END) i0,
  SUM(CASE WHEN x.PGPRFCHA < pr.anclaSig THEN NVL(x.PGPRINVP,0) ELSE 0 END) v0,
  SUM(CASE WHEN x.PGPRFCHA < pr.corteSig THEN NVL(x.PGPRINPG,0) ELSE 0 END) i1,
  SUM(CASE WHEN x.PGPRFCHA < pr.corteSig THEN NVL(x.PGPRINVP,0) ELSE 0 END) v1,
  SUM(CASE WHEN x.PGPRFCHA < TRUNC(d.DTPRFCVN,'MM') THEN NVL(x.PGPRINPG,0) ELSE 0 END) iV,
  SUM(CASE WHEN x.PGPRFCHA >= pr.anclaSig AND x.PGPRFCHA < pr.corteSig THEN NVL(x.PGPRINPG,0) + NVL(x.PGPRINVP,0) ELSE 0 END) pag,
  SUM(CASE WHEN x.PGPRFCHA >= pr.anclaSig AND x.PGPRFCHA < pr.corteSig AND x.PGPRFCHA < TRUNC(d.DTPRFCVN,'MM')
       AND d.DTPRFCVN >= pr.anclaSig THEN NVL(x.PGPRINPG,0) + NVL(x.PGPRINVP,0) ELSE 0 END) ant
  FROM CRD.PGPR x JOIN CRD.DTPR d ON d.DTPRCDGO = x.DTPRCDGO CROSS JOIN pr WHERE NVL(x.PGPRANUL,0) = 0 GROUP BY x.DTPRCDGO),
cnd AS (SELECT m.DTPRCDGO, SUM(m.MVICVLOR) cond FROM CRD.MVIC m, pr WHERE m.MVICTPMV = 9 AND m.MVICCMPN = 1
  AND NVL(m.MVICANUL,0) = 0 AND TRUNC(m.MVICFCCT) <= pr.corte GROUP BY m.DTPRCDGO),
cu AS (SELECT d.DTPRCDGO, d.PRSTCDGO, p.PRSTIDAS op, p.PRSTIDST est, p.PRSTFCHA fch, t.TPPRCSPB tipo, d.DTPRFCVN vto, d.DTPRESTD estd, d.DTPRFCPG fpg,
  NVL(d.DTPRINTR,0) intr, NVL(d.DTPRINVN,0) invn,
  CASE WHEN d.DTPRFCVN < DATE '2025-06-01' THEN 'A' WHEN TRUNC(d.DTPRFCVN) <= pr.corte THEN 'B' ELSE 'C' END coh,
  NVL(g.i0,0) i0, NVL(g.v0,0) v0, NVL(g.i1,0) i1, NVL(g.v1,0) v1, NVL(g.iV,0) iV, NVL(g.pag,0) pag, NVL(g.ant,0) ant,
  NVL(n.cond,0) cond,
  l0.u u0, l0.plvn plvn0, l1.u u1, l1.plvn plvn1, lv.u uv, lv.plvn plvnv
  FROM CRD.DTPR d JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO JOIN CRD.PRDC prd ON prd.PRDCCDGO = p.PRDCCDGO
  JOIN CRD.TPPR t ON t.TPPRCDGO = prd.TPPRCDGO CROSS JOIN pr
  JOIN lu l0 ON l0.PRSTCDGO = p.PRSTCDGO AND l0.f = pr.ancla
  JOIN lu l1 ON l1.PRSTCDGO = p.PRSTCDGO AND l1.f = pr.corte
  LEFT JOIN lu lv ON lv.PRSTCDGO = p.PRSTCDGO AND lv.f = TRUNC(d.DTPRFCVN,'MM') - 1
  LEFT JOIN pgc g ON g.DTPRCDGO = d.DTPRCDGO LEFT JOIN cnd n ON n.DTPRCDGO = d.DTPRCDGO
  WHERE d.DTPRFCVN IS NOT NULL AND (l0.u = 1 OR l1.u = 1 OR lv.u = 1 OR NVL(g.pag,0) <> 0 OR NVL(n.cond,0) <> 0)),
cc AS (SELECT cu.*,
  -- interes vencido al ancla (cohorte A): mismo calculo que plantilla-intereses-cohortes.sql
  CASE WHEN coh = 'A' AND u0 = 1 AND NOT (NVL(estd,0) IN (4, 7) AND NVL(fpg, DATE '2025-06-01') < DATE '2025-06-01')
       THEN CASE WHEN est = 8 AND vto > plvn0 THEN 0 ELSE GREATEST(intr + invn - i0 - v0, 0) END ELSE 0 END iv0,
  CASE WHEN coh IN ('A','B') AND u1 = 1 AND NOT (NVL(estd,0) IN (4, 7) AND NVL(fpg, (SELECT corteSig FROM pr)) < (SELECT corteSig FROM pr))
       THEN CASE WHEN est = 8 AND vto > plvn1 THEN 0 ELSE GREATEST(intr + invn - i1 - v1, 0) END ELSE 0 END iv1,
  -- devengo (4) del cierre del mes anterior al vencimiento (fechado el dia 1 del mes de vencimiento, <= corte)
  CASE WHEN coh = 'B' AND uv = 1 AND NOT (NVL(estd,0) IN (4, 7) AND NVL(fpg, TRUNC(vto,'MM')) < TRUNC(vto,'MM'))
       THEN CASE WHEN vto > plvnv THEN 0 ELSE GREATEST(intr - iV, 0) END ELSE 0 END dev
  FROM cu),
cr AS (SELECT cc.*, iv0 + dev - pag - cond libro, iv1 - ant expl,
  CASE WHEN ABS(iv0 + dev - pag - cond - iv1 + ant) <= 0.004 THEN '0 sin residuo'
       WHEN coh = 'B' AND NVL(uv,0) = 0 AND fch >= TRUNC(vto,'MM') THEN '1 primera cuota de prestamo del mes, sin devengo'
       WHEN NVL(estd,0) = 7 THEN '2 cuota cancelada anticipada'
       WHEN NVL(estd,0) = 4 AND NVL(fpg, DATE '9999-12-31') <= (SELECT corte FROM pr) AND iv1 = 0 THEN '3 pagada con interes cobrado distinto del pendiente'
       WHEN iv0 + dev - pag - cond < -0.004 AND iv1 = 0 THEN '4 cobro mayor que lo devengado'
       ELSE '9 otro' END cat
  FROM cc),
cta AS (SELECT CASE t.DTASCNTA WHEN '1.4.02.05' THEN 'PQ' WHEN '1.4.02.10' THEN 'PP' ELSE 'PH' END tipo,
  SUM(CASE WHEN a.ASNTOBSR LIKE 'Asiento de apertura%' THEN NVL(t.DTASDBEE,0)-NVL(t.DTASHBRR,0) ELSE 0 END) apertura,
  SUM(CASE WHEN a.ASNTOBSR LIKE 'CRD devengado de interes%' THEN NVL(t.DTASDBEE,0)-NVL(t.DTASHBRR,0) ELSE 0 END) dev_asiento,
  SUM(NVL(t.DTASDBEE,0)-NVL(t.DTASHBRR,0)) saldo
  FROM CNT.DTAS t JOIN CNT.ASNT a ON a.ASNTCDGO = t.ASNTCDGO CROSS JOIN pr
  WHERE a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < pr.corteSig AND t.DTASCNTA IN ('1.4.02.05','1.4.02.10','1.4.02.15')
  GROUP BY CASE t.DTASCNTA WHEN '1.4.02.05' THEN 'PQ' WHEN '1.4.02.10' THEN 'PP' ELSE 'PH' END),
-- mora contabilizada pendiente en 1.4.02 (mora-pendiente-1402.sql): devengo 5 - reverso 6 - condonado 9 - mora cobrada desde el ancla
mo AS (SELECT t.TPPRCSPB tipo, SUM(x.v) mora FROM (
   SELECT m.PRSTCDGO, CASE m.MVICTPMV WHEN 5 THEN m.MVICVLOR ELSE -m.MVICVLOR END v FROM CRD.MVIC m, pr
    WHERE m.MVICCMPN = 2 AND m.MVICTPMV IN (5, 6, 9) AND NVL(m.MVICANUL,0) = 0 AND TRUNC(m.MVICFCCT) <= pr.corte
   UNION ALL
   SELECT g.PRSTCDGO, -NVL(g.PGPRMRPG,0) FROM CRD.PGPR g, pr
    WHERE NVL(g.PGPRANUL,0) = 0 AND g.PGPRFCHA >= pr.anclaSig AND g.PGPRFCHA < pr.corteSig) x
  JOIN CRD.PRST p ON p.PRSTCDGO = x.PRSTCDGO JOIN CRD.PRDC prd ON prd.PRDCCDGO = p.PRDCCDGO
  JOIN CRD.TPPR t ON t.TPPRCDGO = prd.TPPRCDGO GROUP BY t.TPPRCSPB),
rt AS (SELECT tipo, SUM(iv0) iv0, SUM(iv1) iv1, SUM(dev) dev, SUM(pag) pag, SUM(cond) cond, SUM(ant) ant,
  SUM(CASE WHEN coh = 'C' THEN ant ELSE 0 END) ant_c, SUM(libro - expl) res_cuota FROM cr GROUP BY tipo)
SELECT c.tipo,
  ROUND(c.saldo,2) saldo_1402, ROUND(NVL(mo.mora,0),2) mora_pend, ROUND(rt.iv1,2) interes_pend,
  ROUND(rt.iv1 + NVL(mo.mora,0),2) objetivo_1402, ROUND(rt.iv1 + NVL(mo.mora,0) - c.saldo,2) ajuste_1402,
  ROUND(c.apertura - rt.iv0,2) arranque
FROM cta c CROSS JOIN pr LEFT JOIN mo ON mo.tipo = c.tipo LEFT JOIN rt ON rt.tipo = c.tipo
ORDER BY 1;

-- 02 provisión 1.4.99.05: contable al 30-nov contra el objetivo (1.4.02 objetivo de los tres tipos) y libro por componente
WITH pr AS (SELECT DATE '2025-11-30' corte, DATE '2025-11-30' + 1 corteSig, DATE '2025-05-31' ancla, DATE '2025-06-01' anclaSig FROM dual),
fe AS (SELECT ADD_MONTHS(DATE '2025-05-31', LEVEL - 1) f FROM dual CONNECT BY ADD_MONTHS(DATE '2025-05-31', LEVEL - 1) <= (SELECT corte FROM pr)),
lu AS (SELECT p.PRSTCDGO, fe.f,
  CASE WHEN p.PRSTFCHA < fe.f + 1 AND NVL(p.PRSTIDST,0) NOT IN (0, 1, 6, 7, 12, 14)
   AND NOT (NVL(p.PRSTIDST,0) IN (3, 4, 5, 9, 13)
    AND NOT EXISTS (SELECT 1 FROM CRD.DTPR cx WHERE cx.PRSTCDGO = p.PRSTCDGO AND NVL(cx.DTPRESTD,0) NOT IN (4, 7, 9))
    AND NOT EXISTS (SELECT 1 FROM CRD.DTPR cy WHERE cy.PRSTCDGO = p.PRSTCDGO AND cy.DTPRESTD = 4 AND cy.DTPRFCPG >= fe.f)
    AND NOT EXISTS (SELECT 1 FROM CRD.PGPR cz WHERE cz.PRSTCDGO = p.PRSTCDGO AND NVL(cz.PGPRANUL,0) = 0 AND cz.PGPRFCHA >= fe.f + 1))
  THEN 1 ELSE 0 END u,
  (SELECT MIN(v.PLVNFCCR) FROM CRD.PLVN v WHERE v.PRSTCDGO = p.PRSTCDGO AND v.PLVNESTD IN (1, 2) AND v.PLVNFCCR < fe.f + 1) plvn
  FROM CRD.PRST p CROSS JOIN fe),
pgc AS (SELECT x.DTPRCDGO,
  SUM(CASE WHEN x.PGPRFCHA < pr.anclaSig THEN NVL(x.PGPRINPG,0) ELSE 0 END) i0,
  SUM(CASE WHEN x.PGPRFCHA < pr.anclaSig THEN NVL(x.PGPRINVP,0) ELSE 0 END) v0,
  SUM(CASE WHEN x.PGPRFCHA < pr.corteSig THEN NVL(x.PGPRINPG,0) ELSE 0 END) i1,
  SUM(CASE WHEN x.PGPRFCHA < pr.corteSig THEN NVL(x.PGPRINVP,0) ELSE 0 END) v1,
  SUM(CASE WHEN x.PGPRFCHA < TRUNC(d.DTPRFCVN,'MM') THEN NVL(x.PGPRINPG,0) ELSE 0 END) iV,
  SUM(CASE WHEN x.PGPRFCHA >= pr.anclaSig AND x.PGPRFCHA < pr.corteSig THEN NVL(x.PGPRINPG,0) + NVL(x.PGPRINVP,0) ELSE 0 END) pag,
  SUM(CASE WHEN x.PGPRFCHA >= pr.anclaSig AND x.PGPRFCHA < pr.corteSig AND x.PGPRFCHA < TRUNC(d.DTPRFCVN,'MM')
       AND d.DTPRFCVN >= pr.anclaSig THEN NVL(x.PGPRINPG,0) + NVL(x.PGPRINVP,0) ELSE 0 END) ant
  FROM CRD.PGPR x JOIN CRD.DTPR d ON d.DTPRCDGO = x.DTPRCDGO CROSS JOIN pr WHERE NVL(x.PGPRANUL,0) = 0 GROUP BY x.DTPRCDGO),
cnd AS (SELECT m.DTPRCDGO, SUM(m.MVICVLOR) cond FROM CRD.MVIC m, pr WHERE m.MVICTPMV = 9 AND m.MVICCMPN = 1
  AND NVL(m.MVICANUL,0) = 0 AND TRUNC(m.MVICFCCT) <= pr.corte GROUP BY m.DTPRCDGO),
cu AS (SELECT d.DTPRCDGO, d.PRSTCDGO, p.PRSTIDAS op, p.PRSTIDST est, p.PRSTFCHA fch, t.TPPRCSPB tipo, d.DTPRFCVN vto, d.DTPRESTD estd, d.DTPRFCPG fpg,
  NVL(d.DTPRINTR,0) intr, NVL(d.DTPRINVN,0) invn,
  CASE WHEN d.DTPRFCVN < DATE '2025-06-01' THEN 'A' WHEN TRUNC(d.DTPRFCVN) <= pr.corte THEN 'B' ELSE 'C' END coh,
  NVL(g.i0,0) i0, NVL(g.v0,0) v0, NVL(g.i1,0) i1, NVL(g.v1,0) v1, NVL(g.iV,0) iV, NVL(g.pag,0) pag, NVL(g.ant,0) ant,
  NVL(n.cond,0) cond,
  l0.u u0, l0.plvn plvn0, l1.u u1, l1.plvn plvn1, lv.u uv, lv.plvn plvnv
  FROM CRD.DTPR d JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO JOIN CRD.PRDC prd ON prd.PRDCCDGO = p.PRDCCDGO
  JOIN CRD.TPPR t ON t.TPPRCDGO = prd.TPPRCDGO CROSS JOIN pr
  JOIN lu l0 ON l0.PRSTCDGO = p.PRSTCDGO AND l0.f = pr.ancla
  JOIN lu l1 ON l1.PRSTCDGO = p.PRSTCDGO AND l1.f = pr.corte
  LEFT JOIN lu lv ON lv.PRSTCDGO = p.PRSTCDGO AND lv.f = TRUNC(d.DTPRFCVN,'MM') - 1
  LEFT JOIN pgc g ON g.DTPRCDGO = d.DTPRCDGO LEFT JOIN cnd n ON n.DTPRCDGO = d.DTPRCDGO
  WHERE d.DTPRFCVN IS NOT NULL AND (l0.u = 1 OR l1.u = 1 OR lv.u = 1 OR NVL(g.pag,0) <> 0 OR NVL(n.cond,0) <> 0)),
cc AS (SELECT cu.*,
  -- interes vencido al ancla (cohorte A): mismo calculo que plantilla-intereses-cohortes.sql
  CASE WHEN coh = 'A' AND u0 = 1 AND NOT (NVL(estd,0) IN (4, 7) AND NVL(fpg, DATE '2025-06-01') < DATE '2025-06-01')
       THEN CASE WHEN est = 8 AND vto > plvn0 THEN 0 ELSE GREATEST(intr + invn - i0 - v0, 0) END ELSE 0 END iv0,
  CASE WHEN coh IN ('A','B') AND u1 = 1 AND NOT (NVL(estd,0) IN (4, 7) AND NVL(fpg, (SELECT corteSig FROM pr)) < (SELECT corteSig FROM pr))
       THEN CASE WHEN est = 8 AND vto > plvn1 THEN 0 ELSE GREATEST(intr + invn - i1 - v1, 0) END ELSE 0 END iv1,
  -- devengo (4) del cierre del mes anterior al vencimiento (fechado el dia 1 del mes de vencimiento, <= corte)
  CASE WHEN coh = 'B' AND uv = 1 AND NOT (NVL(estd,0) IN (4, 7) AND NVL(fpg, TRUNC(vto,'MM')) < TRUNC(vto,'MM'))
       THEN CASE WHEN vto > plvnv THEN 0 ELSE GREATEST(intr - iV, 0) END ELSE 0 END dev
  FROM cu),
cr AS (SELECT cc.*, iv0 + dev - pag - cond libro, iv1 - ant expl,
  CASE WHEN ABS(iv0 + dev - pag - cond - iv1 + ant) <= 0.004 THEN '0 sin residuo'
       WHEN coh = 'B' AND NVL(uv,0) = 0 AND fch >= TRUNC(vto,'MM') THEN '1 primera cuota de prestamo del mes, sin devengo'
       WHEN NVL(estd,0) = 7 THEN '2 cuota cancelada anticipada'
       WHEN NVL(estd,0) = 4 AND NVL(fpg, DATE '9999-12-31') <= (SELECT corte FROM pr) AND iv1 = 0 THEN '3 pagada con interes cobrado distinto del pendiente'
       WHEN iv0 + dev - pag - cond < -0.004 AND iv1 = 0 THEN '4 cobro mayor que lo devengado'
       ELSE '9 otro' END cat
  FROM cc),
cta AS (SELECT CASE t.DTASCNTA WHEN '1.4.02.05' THEN 'PQ' WHEN '1.4.02.10' THEN 'PP' ELSE 'PH' END tipo,
  SUM(CASE WHEN a.ASNTOBSR LIKE 'Asiento de apertura%' THEN NVL(t.DTASDBEE,0)-NVL(t.DTASHBRR,0) ELSE 0 END) apertura,
  SUM(CASE WHEN a.ASNTOBSR LIKE 'CRD devengado de interes%' THEN NVL(t.DTASDBEE,0)-NVL(t.DTASHBRR,0) ELSE 0 END) dev_asiento,
  SUM(NVL(t.DTASDBEE,0)-NVL(t.DTASHBRR,0)) saldo
  FROM CNT.DTAS t JOIN CNT.ASNT a ON a.ASNTCDGO = t.ASNTCDGO CROSS JOIN pr
  WHERE a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < pr.corteSig AND t.DTASCNTA IN ('1.4.02.05','1.4.02.10','1.4.02.15')
  GROUP BY CASE t.DTASCNTA WHEN '1.4.02.05' THEN 'PQ' WHEN '1.4.02.10' THEN 'PP' ELSE 'PH' END),
-- mora contabilizada pendiente en 1.4.02 (mora-pendiente-1402.sql): devengo 5 - reverso 6 - condonado 9 - mora cobrada desde el ancla
mo AS (SELECT t.TPPRCSPB tipo, SUM(x.v) mora FROM (
   SELECT m.PRSTCDGO, CASE m.MVICTPMV WHEN 5 THEN m.MVICVLOR ELSE -m.MVICVLOR END v FROM CRD.MVIC m, pr
    WHERE m.MVICCMPN = 2 AND m.MVICTPMV IN (5, 6, 9) AND NVL(m.MVICANUL,0) = 0 AND TRUNC(m.MVICFCCT) <= pr.corte
   UNION ALL
   SELECT g.PRSTCDGO, -NVL(g.PGPRMRPG,0) FROM CRD.PGPR g, pr
    WHERE NVL(g.PGPRANUL,0) = 0 AND g.PGPRFCHA >= pr.anclaSig AND g.PGPRFCHA < pr.corteSig) x
  JOIN CRD.PRST p ON p.PRSTCDGO = x.PRSTCDGO JOIN CRD.PRDC prd ON prd.PRDCCDGO = p.PRDCCDGO
  JOIN CRD.TPPR t ON t.TPPRCDGO = prd.TPPRCDGO GROUP BY t.TPPRCSPB),
rt AS (SELECT tipo, SUM(iv0) iv0, SUM(iv1) iv1, SUM(dev) dev, SUM(pag) pag, SUM(cond) cond, SUM(ant) ant,
  SUM(CASE WHEN coh = 'C' THEN ant ELSE 0 END) ant_c, SUM(libro - expl) res_cuota FROM cr GROUP BY tipo)
, prv AS (SELECT ROUND(SUM(NVL(D.DTASDBEE,0)-NVL(D.DTASHBRR,0)),2) prov FROM CNT.DTAS D JOIN CNT.ASNT A ON A.ASNTCDGO=D.ASNTCDGO
   WHERE A.ASNTESTD IN (1,3) AND A.ASNTFCHA < DATE '2025-12-01' AND D.DTASCNTA = '1.4.99.05'),
lib AS (SELECT ROUND(SUM(CASE WHEN m.MVICCMPN=1 THEN CASE WHEN MVICTPMV IN (1,4) THEN MVICVLOR WHEN MVICTPMV IN (2,3,9,10) THEN -MVICVLOR ELSE 0 END END),2) li,
   ROUND(SUM(CASE WHEN m.MVICCMPN=2 THEN CASE WHEN MVICTPMV IN (1,4) THEN MVICVLOR WHEN MVICTPMV IN (2,3,9,10) THEN -MVICVLOR ELSE 0 END END),2) lm
   FROM CRD.MVIC m WHERE NVL(m.MVICANUL,0)=0 AND m.MVICFCCT < DATE '2025-12-01')
SELECT prv.prov provision_contable, ROUND(SUM(rt.iv1),2) objetivo_interes, ROUND(SUM(NVL(mo.mora,0)),2) objetivo_mora,
  ROUND(-(SUM(rt.iv1) + SUM(NVL(mo.mora,0))),2) objetivo_provision, ROUND(-(SUM(rt.iv1) + SUM(NVL(mo.mora,0))) - prv.prov,2) ajuste_provision,
  lib.li libro_interes, lib.lm libro_mora
FROM rt LEFT JOIN mo ON mo.tipo = rt.tipo CROSS JOIN prv CROSS JOIN lib GROUP BY prv.prov, lib.li, lib.lm;

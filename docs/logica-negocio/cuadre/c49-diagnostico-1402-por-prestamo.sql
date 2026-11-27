-- =====================================================================================================================================
-- C49 - Diagnóstico de la 1.4.02 por préstamo: por qué la 1.4.02 se queda corta contra el objetivo D-206 casi lo mismo cada mes.
-- Equipo omen-saa-1 - 2026-11-27 - SOLO LECTURA (un único SELECT, sin sentencias de entorno de sqlplus). Sin nombres de partícipes (operación = PRSTIDAS).
-- Para correr en el respaldo v9 de cada mes. El ÚNICO parámetro es la fecha de corte de la primera línea (fin de mes >= 2025-06-30): cámbiela ahí.
--
-- Base: plantilla-conciliacion-1402.sql (C27b) del cuadre, copiada tal cual (mismas CTE pr, fe, lu, pgc, cnd, cu, cc, cr) para que el objetivo sea EXACTAMENTE el de
-- def9-NN-control-1402-provision-*.sql. Se agregan: el estado del préstamo al cierre del mes anterior (up), el libro de devengo de interés MVIC 11/12 por cuota (mvl), las
-- cuotas 7 sin fecha (C48), y los asientos del cierre por descripción (paso 11, paso 4b, paso 10).
--
-- Qué devuelve (formato largo; una fila por nivel/tipo/operación/causa; sumar VALOR por tipo da el AJUSTE_1402 = objetivo - saldo de la columna AJUSTE_1402 del control):
--   NIVEL 0  CONTROL        valor = AJUSTE_1402 del tipo = (interés pendiente iv_corte + mora pendiente) - saldo contable 1.4.02. (+ = falta interés en la contabilidad).
--   NIVEL 1  POR PRÉSTAMO   valor = Σ por cuota (iv_corte - libro emulado), separado por causa candidata (ver abajo). libro emulado = iv_ancla + devengo(4) - cobrado - condonado,
--                           la misma emulación de la plantilla (lo que el código debería haber dejado en 1.4.02 por esa cuota SIN contar el paso 11).
--   NIVEL 2  POR TIPO       lo que NO se puede atribuir a un préstamo, con signo de ajuste (- lo que ya hay en el saldo): arranque (apertura - iv_ancla), paso 11 asentado,
--                           paso 4b asentado, diferencia del paso 4 asentado contra el emulado, y el resto (paso 10/mora, ajustes manuales, otros).
--   NIVEL 9  CHECK          por tipo: Σ(nivel 1 + nivel 2) - nivel 0. DEBE SER 0,00 (puede traer centavos de redondeo por cuota).
--
-- Causas candidatas del nivel 1 (excluyentes, en este orden):
--   1 ANTICIPADO               cuota que vence después del corte (cohorte C): lo cobrado antes de su mes. Es lo que el paso 11 debe restituir a la 1.4.02 (nivel 2, fila paso 11).
--   2 CANCELADO_EN_EL_MES      el préstamo estaba en cartera al cierre del mes anterior y ya no lo está al corte (cancelado, terminado, sustituido en el mes).
--   5 FUERA_DE_CARTERA_ANTES   el préstamo no estaba en cartera ni al cierre anterior ni al corte, pero la cuota tiene cobros o condonaciones en el rango.
--   3 PARCIAL                  cuota con interés cobrado en parte y todavía pendiente (o estado PARCIAL 6).
--   4 SIN_CAUSA                el resto.
-- Columnas informativas por grupo: dev_emulado (devengo del paso 4 por la regla del código al corte), dev_libro_mvic (Σ MVIC 11 - 12 de esas cuotas al corte; 0 si el libro no existe
-- en ese respaldo), cuotas7_sin_fecha (C48: cuota en estado 7 sin DTPRFCPG), cat_plantilla (la categoría de residuo de la plantilla C27b, para cruzar con ella).
-- =====================================================================================================================================
WITH pr AS (SELECT DATE '2026-07-31' corte, DATE '2026-07-31' + 1 corteSig, DATE '2025-05-31' ancla, DATE '2025-06-01' anclaSig FROM dual),
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
-- C49: libro de devengo de interés (MVIC 11 - 12) por cuota al corte; vacío en un respaldo sin el libro (lotes 19/20).
mvl AS (SELECT m.DTPRCDGO, SUM(CASE m.MVICTPMV WHEN 11 THEN m.MVICVLOR WHEN 12 THEN -m.MVICVLOR ELSE 0 END) dev_libro
  FROM CRD.MVIC m, pr WHERE m.MVICCMPN = 1 AND m.MVICTPMV IN (11, 12) AND NVL(m.MVICANUL,0) = 0 AND TRUNC(m.MVICFCCT) <= pr.corte GROUP BY m.DTPRCDGO),
cu AS (SELECT d.DTPRCDGO, d.PRSTCDGO, p.PRSTIDAS op, p.PRSTIDST est, p.PRSTFCHA fch, CASE t.TPPRCSPB WHEN 'PRE' THEN 'PP' ELSE t.TPPRCSPB END tipo, d.DTPRFCVN vto, d.DTPRESTD estd, d.DTPRFCPG fpg,
  NVL(d.DTPRINTR,0) intr, NVL(d.DTPRINVN,0) invn,
  CASE WHEN d.DTPRFCVN < DATE '2025-06-01' THEN 'A' WHEN TRUNC(d.DTPRFCVN) <= pr.corte THEN 'B' ELSE 'C' END coh,
  NVL(g.i0,0) i0, NVL(g.v0,0) v0, NVL(g.i1,0) i1, NVL(g.v1,0) v1, NVL(g.iV,0) iV, NVL(g.pag,0) pag, NVL(g.ant,0) ant,
  NVL(n.cond,0) cond, NVL(ml.dev_libro,0) dev_libro,
  l0.u u0, l0.plvn plvn0, l1.u u1, l1.plvn plvn1, lv.u uv, lv.plvn plvnv, lp.u up
  FROM CRD.DTPR d JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO JOIN CRD.PRDC prd ON prd.PRDCCDGO = p.PRDCCDGO
  JOIN CRD.TPPR t ON t.TPPRCDGO = prd.TPPRCDGO CROSS JOIN pr
  JOIN lu l0 ON l0.PRSTCDGO = p.PRSTCDGO AND l0.f = pr.ancla
  JOIN lu l1 ON l1.PRSTCDGO = p.PRSTCDGO AND l1.f = pr.corte
  LEFT JOIN lu lv ON lv.PRSTCDGO = p.PRSTCDGO AND lv.f = TRUNC(d.DTPRFCVN,'MM') - 1
  LEFT JOIN lu lp ON lp.PRSTCDGO = p.PRSTCDGO AND lp.f = ADD_MONTHS(pr.corte, -1)
  LEFT JOIN pgc g ON g.DTPRCDGO = d.DTPRCDGO LEFT JOIN cnd n ON n.DTPRCDGO = d.DTPRCDGO LEFT JOIN mvl ml ON ml.DTPRCDGO = d.DTPRCDGO
  WHERE d.DTPRFCVN IS NOT NULL AND (l0.u = 1 OR l1.u = 1 OR lv.u = 1 OR NVL(g.pag,0) <> 0 OR NVL(n.cond,0) <> 0)),
cc AS (SELECT cu.*,
  CASE WHEN coh = 'A' AND u0 = 1 AND NOT (NVL(estd,0) IN (4, 7) AND NVL(fpg, DATE '2025-06-01') < DATE '2025-06-01')
       THEN CASE WHEN est = 8 AND vto > plvn0 THEN 0 ELSE GREATEST(intr + invn - i0 - v0, 0) END ELSE 0 END iv0,
  CASE WHEN coh IN ('A','B') AND u1 = 1 AND NOT (NVL(estd,0) IN (4, 7) AND NVL(fpg, (SELECT corteSig FROM pr)) < (SELECT corteSig FROM pr))
       THEN CASE WHEN est = 8 AND vto > plvn1 THEN 0 ELSE GREATEST(intr + invn - i1 - v1, 0) END ELSE 0 END iv1,
  CASE WHEN coh = 'B' AND uv = 1 AND NOT (NVL(estd,0) IN (4, 7) AND NVL(fpg, TRUNC(vto,'MM')) < TRUNC(vto,'MM'))
       THEN CASE WHEN vto > plvnv THEN 0 ELSE GREATEST(intr - iV, 0) END ELSE 0 END dev
  FROM cu),
cr AS (SELECT cc.*, iv0 + dev - pag - cond libro, iv1 - ant expl,
  CASE WHEN ABS(iv0 + dev - pag - cond - iv1 + ant) <= 0.004 THEN '0 sin residuo'
       WHEN coh = 'B' AND NVL(uv,0) = 0 AND fch >= TRUNC(vto,'MM') THEN '1 primera cuota de prestamo del mes, sin devengo'
       WHEN NVL(estd,0) = 7 THEN '2 cuota cancelada anticipada'
       WHEN NVL(estd,0) = 4 AND NVL(fpg, DATE '9999-12-31') <= (SELECT corte FROM pr) AND iv1 = 0 THEN '3 pagada con interes cobrado distinto del pendiente'
       WHEN iv0 + dev - pag - cond < -0.004 AND iv1 = 0 THEN '4 cobro mayor que lo devengado'
       ELSE '9 otro' END cat,
  -- C49: causa candidata de la diferencia de la cuota (iv1 - libro), excluyente y en este orden.
  CASE WHEN coh = 'C' THEN '1 ANTICIPADO'
       WHEN NVL(u1,0) = 0 AND NVL(up,0) = 1 THEN '2 CANCELADO_EN_EL_MES'
       WHEN NVL(u1,0) = 0 THEN '5 FUERA_DE_CARTERA_ANTES'
       WHEN NVL(estd,0) = 6 OR ((i1 + v1) > 0.004 AND iv1 > 0.004) THEN '3 PARCIAL'
       ELSE '4 SIN_CAUSA' END causa
  FROM cc),
-- 1.4.02 contable por tipo y los asientos del cierre por descripción (la descripción sale del código: CierreCarteraServiceImpl.nuevoSubProceso).
cta AS (SELECT CASE t.DTASCNTA WHEN '1.4.02.05' THEN 'PQ' WHEN '1.4.02.10' THEN 'PP' ELSE 'PH' END tipo,
  SUM(CASE WHEN a.ASNTOBSR LIKE 'Asiento de apertura%' THEN NVL(t.DTASDBEE,0)-NVL(t.DTASHBRR,0) ELSE 0 END) apertura,
  SUM(CASE WHEN a.ASNTOBSR LIKE 'CRD devengado de interes%' THEN NVL(t.DTASDBEE,0)-NVL(t.DTASHBRR,0) ELSE 0 END) dev_asiento,
  SUM(CASE WHEN a.ASNTOBSR LIKE 'CRD intereses recibidos por anticipado%' THEN NVL(t.DTASDBEE,0)-NVL(t.DTASHBRR,0) ELSE 0 END) a11,
  SUM(CASE WHEN a.ASNTOBSR LIKE 'CRD devengado de la primera cuota%' THEN NVL(t.DTASDBEE,0)-NVL(t.DTASHBRR,0) ELSE 0 END) a4b,
  SUM(CASE WHEN a.ASNTOBSR LIKE 'CRD devengo de mora al corte%' THEN NVL(t.DTASDBEE,0)-NVL(t.DTASHBRR,0) ELSE 0 END) a10,
  SUM(NVL(t.DTASDBEE,0)-NVL(t.DTASHBRR,0)) saldo
  FROM CNT.DTAS t JOIN CNT.ASNT a ON a.ASNTCDGO = t.ASNTCDGO CROSS JOIN pr
  WHERE a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < pr.corteSig AND t.DTASCNTA IN ('1.4.02.05','1.4.02.10','1.4.02.15')
  GROUP BY CASE t.DTASCNTA WHEN '1.4.02.05' THEN 'PQ' WHEN '1.4.02.10' THEN 'PP' ELSE 'PH' END),
mo AS (SELECT CASE t.TPPRCSPB WHEN 'PRE' THEN 'PP' ELSE t.TPPRCSPB END tipo, SUM(x.v) mora FROM (
   SELECT m.PRSTCDGO, CASE m.MVICTPMV WHEN 5 THEN m.MVICVLOR ELSE -m.MVICVLOR END v FROM CRD.MVIC m, pr
    WHERE m.MVICCMPN = 2 AND m.MVICTPMV IN (5, 6, 9) AND NVL(m.MVICANUL,0) = 0 AND TRUNC(m.MVICFCCT) <= pr.corte
   UNION ALL
   SELECT g.PRSTCDGO, -NVL(g.PGPRMRPG,0) FROM CRD.PGPR g, pr
    WHERE NVL(g.PGPRANUL,0) = 0 AND g.PGPRFCHA >= pr.anclaSig AND g.PGPRFCHA < pr.corteSig) x
  JOIN CRD.PRST p ON p.PRSTCDGO = x.PRSTCDGO JOIN CRD.PRDC prd ON prd.PRDCCDGO = p.PRDCCDGO
  JOIN CRD.TPPR t ON t.TPPRCDGO = prd.TPPRCDGO GROUP BY CASE t.TPPRCSPB WHEN 'PRE' THEN 'PP' ELSE t.TPPRCSPB END),
rt AS (SELECT tipo, SUM(iv0) iv0, SUM(iv1) iv1, SUM(dev) dev, SUM(pag) pag, SUM(cond) cond, SUM(iv1 - libro) s_cuotas FROM cr GROUP BY tipo),
-- Nivel 1: por préstamo y causa.
n1 AS (SELECT 1 nivel, tipo, op operacion, causa, COUNT(*) cuotas, ROUND(SUM(iv1 - libro), 2) valor,
       ROUND(SUM(dev), 2) dev_emulado, ROUND(SUM(dev_libro), 2) dev_libro_mvic,
       SUM(CASE WHEN estd = 7 AND fpg IS NULL THEN 1 ELSE 0 END) cuotas7_sin_fecha, MIN(cat) cat_plantilla
  FROM cr GROUP BY tipo, op, causa HAVING ROUND(SUM(iv1 - libro), 4) <> 0),
-- Nivel 2: lo no atribuible a un préstamo, con signo de ajuste (menos lo que ya está en el saldo).
n2 AS (
  SELECT 2 nivel, c.tipo, TO_NUMBER(NULL) operacion, 'NA1 ARRANQUE: apertura menos interes pendiente al ancla' causa, TO_NUMBER(NULL) cuotas,
         ROUND(-(c.apertura - NVL(r.iv0,0)), 2) valor, TO_NUMBER(NULL) dev_emulado, TO_NUMBER(NULL) dev_libro_mvic, TO_NUMBER(NULL) cuotas7_sin_fecha, CAST(NULL AS VARCHAR2(60)) cat_plantilla
    FROM cta c LEFT JOIN rt r ON r.tipo = c.tipo
  UNION ALL
  SELECT 2, c.tipo, NULL, 'NA2 ASENTADO paso 11 interes anticipado (debe restituir lo cobrado por anticipado)', NULL, ROUND(-c.a11, 2), NULL, NULL, NULL, NULL FROM cta c
  UNION ALL
  SELECT 2, c.tipo, NULL, 'NA3 ASENTADO paso 4b primera cuota de prestamos nuevos', NULL, ROUND(-c.a4b, 2), NULL, NULL, NULL, NULL FROM cta c
  UNION ALL
  SELECT 2, c.tipo, NULL, 'NA4 DIFERENCIA del paso 4 asentado menos el paso 4 emulado', NULL, ROUND(-(c.dev_asiento - NVL(r.dev,0)), 2), NULL, NULL, NULL, NULL
    FROM cta c LEFT JOIN rt r ON r.tipo = c.tipo
  UNION ALL
  SELECT 2, c.tipo, NULL, 'NA5 RESTO no atribuible (paso 10/mora asentada, ajustes manuales D206 vigentes, otros movimientos)', NULL,
         ROUND(-((c.saldo - NVL(m.mora,0) - (NVL(r.iv0,0) + NVL(r.dev,0) - NVL(r.pag,0) - NVL(r.cond,0)))
                 - (c.apertura - NVL(r.iv0,0)) - c.a11 - c.a4b - (c.dev_asiento - NVL(r.dev,0))), 2), NULL, NULL, NULL, NULL
    FROM cta c LEFT JOIN rt r ON r.tipo = c.tipo LEFT JOIN mo m ON m.tipo = c.tipo),
-- Nivel 0: el D206 del mes por tipo (AJUSTE_1402 del control).
n0 AS (SELECT 0 nivel, c.tipo, TO_NUMBER(NULL) operacion, 'D206 = objetivo (interes pendiente + mora pendiente) menos saldo 1.4.02' causa, TO_NUMBER(NULL) cuotas,
       ROUND(NVL(r.iv1,0) + NVL(m.mora,0) - c.saldo, 2) valor, TO_NUMBER(NULL) dev_emulado, TO_NUMBER(NULL) dev_libro_mvic, TO_NUMBER(NULL) cuotas7_sin_fecha, CAST(NULL AS VARCHAR2(60)) cat_plantilla
  FROM cta c LEFT JOIN rt r ON r.tipo = c.tipo LEFT JOIN mo m ON m.tipo = c.tipo),
base AS (SELECT * FROM n0 UNION ALL SELECT * FROM n1 UNION ALL SELECT * FROM n2)
SELECT nivel, tipo, operacion, causa, cuotas, valor, dev_emulado, dev_libro_mvic, cuotas7_sin_fecha, cat_plantilla FROM base
UNION ALL
SELECT 9, tipo, TO_NUMBER(NULL), 'CHECK: suma de causas (niveles 1 y 2) menos D206 (debe ser 0,00)', TO_NUMBER(NULL),
       ROUND(SUM(CASE WHEN nivel IN (1, 2) THEN valor ELSE 0 END) - SUM(CASE WHEN nivel = 0 THEN valor ELSE 0 END), 2),
       TO_NUMBER(NULL), TO_NUMBER(NULL), TO_NUMBER(NULL), CAST(NULL AS VARCHAR2(60))
  FROM base GROUP BY tipo
ORDER BY 2, 1, 4, 6 DESC;

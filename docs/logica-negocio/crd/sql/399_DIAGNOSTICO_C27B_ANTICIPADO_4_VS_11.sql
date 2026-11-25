-- =====================================================================================
-- 399 — DIAGNÓSTICO (SOLO LECTURA) D-194 · C27b: ¿por qué el ④ del 1-nov devengó MENOS de lo que el ⑪ pasó a 2.9.01.05 el 31-oct?
-- Equipo omen-saa-1 · 2026-11-25. No modifica nada. Se puede correr en producción o en el reproceso.
--
-- Cómo funciona la pareja (leído del código):
--   ⑪ al 31-oct   : Σ PGPRINPG vigente con ANCLA <= fecha < corte+1 de las cuotas con vencimiento >= 1-nov (CierreCarteraDaoServiceImpl.selectInteresAnticipadoPorTipo).
--                   Universo: PRSTFCHA < corte+1 y PRSTIDST fuera de {0,1,6,7,12,14} (entran cancelados y novados); cuota no anulada.
--   ④ del 1-nov   : por cuota con vencimiento en el mes que se abre (selectInteresDevengoPorTipoEnRango, corte = 31-oct):
--                   · préstamo «cancelado terminado al corte» O cuota estado 7  →  solo lo cobrado (post = ancla <= fecha < corte+1)
--                   · si no, y la cuota es de plazo vencido condonado (PLVN)    →  0
--                   · si no                                                      →  GREATEST(DTPRINTR − pre, 0)  (pre = cobrado antes del ancla)
--   Condición para que el ⑪ del 30-nov libere 2.9.01.05 SIN dejar 1.4.02 acreedor: por cada cuota de noviembre, ④ >= lo cobrado por anticipado (post).
--   Las cuotas con ④ < post son las que faltan.
--   OJO (hallazgo de lectura de código): ④ usa el estado de la cuota de HOY (`DTPRESTD = 7`) sin fecha. En un reproceso, una cuota que un acuerdo, una
--   precancelación, una novación o una reestructuración cerró DESPUÉS del corte se trata como cancelada al corte (rama «solo lo cobrado»): la regla A2 lo marca. Este script las lista, las clasifica y las suma por tipo.
-- PARAMS (cambiar en cada bloque): CORTE = 2025-10-31 · ANCLA = 2025-05-31 (saa.cartera.anclaArranque) · MES = el mes que abre el ④ (noviembre de 2025).
-- =====================================================================================

-- BLOQUE 1 — Por tipo de préstamo, para NOVIEMBRE: lo que ⑪ parqueó, lo que ④ devengó y la diferencia (es el faltante). Compararlo con lo que mide la
-- conciliación del cuadre: la columna anticipado_11 debe coincidir con su «anticipado ya devengable de cuotas de noviembre» y devengo_4 con lo que el ④ del 1-nov devengó.
WITH params AS (SELECT DATE '2025-10-31' AS corte, DATE '2025-05-31' AS ancla, DATE '2025-11-01' AS mes_desde, DATE '2025-11-30' AS mes_hasta FROM DUAL),
pg AS (
    SELECT g.DTPRCDGO,
           SUM(CASE WHEN g.PGPRFCHA < pa.ancla THEN NVL(g.PGPRINPG, 0) ELSE 0 END) AS pre,
           SUM(CASE WHEN g.PGPRFCHA >= pa.ancla AND g.PGPRFCHA < pa.corte + 1 THEN NVL(g.PGPRINPG, 0) ELSE 0 END) AS post,
           SUM(CASE WHEN g.PGPRFCHA >= pa.corte AND g.PGPRFCHA < pa.corte + 1 THEN NVL(g.PGPRINPG, 0) ELSE 0 END) AS post_dia_corte
      FROM CRD.PGPR g, params pa WHERE NVL(g.PGPRANUL, 0) = 0 GROUP BY g.DTPRCDGO
),
cuota AS (
    SELECT d.DTPRCDGO, d.PRSTCDGO, d.DTPRNMCT, d.DTPRESTD, NVL(d.DTPRINTR, 0) AS interes, p.PRSTIDST, pr.TPPRCDGO AS tipo,
           NVL(pg.pre, 0) AS pre, NVL(pg.post, 0) AS post, NVL(pg.post_dia_corte, 0) AS post_dia_corte,
           CASE WHEN NVL(p.PRSTIDST, 0) IN (3, 4, 5, 9, 13)
                 AND NOT EXISTS (SELECT 1 FROM CRD.DTPR cx WHERE cx.PRSTCDGO = p.PRSTCDGO AND NVL(cx.DTPRESTD, 0) NOT IN (4, 7, 9))
                 AND NOT EXISTS (SELECT 1 FROM CRD.DTPR cy WHERE cy.PRSTCDGO = p.PRSTCDGO AND cy.DTPRESTD = 4 AND cy.DTPRFCPG >= pa.corte)
                 AND NOT EXISTS (SELECT 1 FROM CRD.PGPR cz WHERE cz.PRSTCDGO = p.PRSTCDGO AND NVL(cz.PGPRANUL, 0) = 0 AND cz.PGPRFCHA >= pa.corte + 1)
                THEN 1 ELSE 0 END AS terminado,
           CASE WHEN d.DTPRFCVN > (SELECT MIN(v.PLVNFCCR) FROM CRD.PLVN v WHERE v.PRSTCDGO = p.PRSTCDGO AND v.PLVNESTD IN (1, 2) AND v.PLVNFCCR < pa.corte + 1)
                THEN 1 ELSE 0 END AS condonado,
           CASE WHEN EXISTS (SELECT 1 FROM CRD.EVPR e WHERE e.PRSTCDGO = p.PRSTCDGO AND e.EVPRESTD = 1 AND e.EVPRFCHA >= pa.corte + 1) THEN 1 ELSE 0 END AS evento_despues_del_corte
      FROM params pa, CRD.DTPR d
      JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
      LEFT JOIN pg ON pg.DTPRCDGO = d.DTPRCDGO
     WHERE p.PRSTFCHA < pa.corte + 1 AND NVL(p.PRSTIDST, 0) NOT IN (0, 1, 6, 7, 12, 14) AND NVL(d.DTPRESTD, 0) <> 9
       AND pr.TPPRCDGO IS NOT NULL AND TRUNC(d.DTPRFCVN) BETWEEN pa.mes_desde AND pa.mes_hasta
),
calc AS (
    SELECT c.*,
           CASE WHEN c.terminado = 1 OR NVL(c.DTPRESTD, 0) = 7 THEN c.post WHEN c.condonado = 1 THEN 0 ELSE GREATEST(c.interes - c.pre, 0) END AS devengo_4,
           CASE WHEN c.terminado = 1 OR NVL(c.DTPRESTD, 0) = 7 THEN (CASE WHEN c.evento_despues_del_corte = 1 THEN 'A2_ESTADO_7_POR_UN_EVENTO_POSTERIOR_AL_CORTE' ELSE 'A_TERMINADO_O_ESTADO_7' END) WHEN c.condonado = 1 THEN 'B_PLAZO_VENCIDO_CONDONADO'
                WHEN c.post > GREATEST(c.interes - c.pre, 0) + 0.005 THEN 'C_COBRADO_MAYOR_QUE_LA_CUOTA' ELSE 'D_OTRO' END AS regla
      FROM cuota c
)
SELECT tipo, ROUND(SUM(post), 2) AS anticipado_11, ROUND(SUM(devengo_4), 2) AS devengo_4, ROUND(SUM(GREATEST(post - devengo_4, 0)), 2) AS faltante_4,
       COUNT(CASE WHEN post - devengo_4 > 0.005 THEN 1 END) AS cuotas_con_faltante
  FROM calc GROUP BY tipo ORDER BY tipo;

-- NOTA: la columna evento_despues_del_corte busca un evento vigente (CRD.EVPR, EVPRESTD = 1) con fecha posterior al corte.
-- BLOQUE 2 — Los faltantes por regla y tipo: cuál de las ramas del ④ se queda corta (A terminado/estado 7 · B plazo vencido condonado · C cobrado mayor que el interés de la
-- cuota · D otro). Repetir el CTE del BLOQUE 1 (params, pg, cuota, calc) y cambiar solo el SELECT final.
-- (Se deja el SELECT final aquí; pegar los cuatro CTE del bloque 1 antes.)
--   SELECT tipo, regla, COUNT(*) AS cuotas, ROUND(SUM(post), 2) AS anticipado_11, ROUND(SUM(devengo_4), 2) AS devengo_4, ROUND(SUM(post - devengo_4), 2) AS faltante_4,
--          ROUND(SUM(post_dia_corte), 2) AS de_los_cuales_cobrado_el_dia_del_corte
--     FROM calc WHERE post - devengo_4 > 0.005 GROUP BY tipo, regla ORDER BY tipo, regla;

-- BLOQUE 3 — El detalle por cuota de los faltantes (los 200 mayores), con las banderas que explican el caso. Pegar los cuatro CTE del bloque 1 antes.
--   SELECT * FROM (
--     SELECT c.tipo, c.PRSTCDGO AS prestamo, c.PRSTIDST AS estado_prestamo, c.DTPRNMCT AS cuota, c.DTPRESTD AS estado_cuota, c.interes AS interes_cuota, c.pre, c.post,
--            c.post_dia_corte, c.terminado, c.condonado, c.regla, c.devengo_4, ROUND(c.post - c.devengo_4, 2) AS faltante
--       FROM calc c WHERE c.post - c.devengo_4 > 0.005 ORDER BY c.post - c.devengo_4 DESC
--   ) WHERE ROWNUM <= 200;

-- BLOQUE 4 — Cuotas con cobro por anticipado cuyo préstamo desapareció de la medición (por si las dos reglas difieren en el universo, no en la fórmula):
-- pagos de interés post-ancla de cuotas de noviembre cuyo préstamo está FUERA del universo (estados 0,1,6,7,12,14) o cuya cuota está anulada. Debe dar 0 en el ⑪ y en el ④.
SELECT pr.TPPRCDGO AS tipo, COUNT(*) AS cuotas, ROUND(SUM(NVL(g.PGPRINPG, 0)), 2) AS interes_cobrado
  FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
 WHERE NVL(g.PGPRANUL, 0) = 0 AND g.PGPRFCHA >= DATE '2025-05-31' AND g.PGPRFCHA < DATE '2025-11-01'
   AND TRUNC(d.DTPRFCVN) BETWEEN DATE '2025-11-01' AND DATE '2025-11-30'
   AND (NVL(p.PRSTIDST, 0) IN (0, 1, 6, 7, 12, 14) OR NVL(d.DTPRESTD, 0) = 9 OR p.PRSTFCHA >= DATE '2025-11-01')
 GROUP BY pr.TPPRCDGO ORDER BY 1;

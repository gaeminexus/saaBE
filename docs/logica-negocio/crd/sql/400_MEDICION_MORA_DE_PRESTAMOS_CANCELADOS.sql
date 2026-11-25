-- =====================================================================================
-- 400 — MEDICIÓN (SOLO LECTURA) D-195: ¿quién reversa la mora devengada y no cobrada de un préstamo que se cancela?
-- Equipo omen-saa-1 · 2026-11-25. No modifica nada. Se puede correr en producción o en el reproceso.
--
-- Lo que se mide: préstamos en estado cancelado (3, 4, 5, 9, 13) que conservan, al corte, mora CONTABILIZADA PENDIENTE (devengo MVIC 5 − 6, menos lo cobrado, menos lo
-- condonado: la misma regla por préstamo del lote 7b) y/o saldo PROVISIONADO de mora (MVIC 1,4 − 2,3,9,10). Un préstamo cancelado ya no cobra nada: todo lo que quede ahí
-- es mora que nadie reversó de 1.4.02 (D 5.1.02 / H 1.4.02) o de 1.4.99.05 (D 1.4.99.05 / H 4.7.05.10).
-- «cancelado por flujo» = el último evento vigente del préstamo (CRD.EVPR, EVPRESTD = 1): precancelación, acuerdo de condonación, novación, reestructuración… o ninguno
-- (la última cuota la pagó el motor: C43 retira la mora por cuota al pagarla sin mora).
-- PARAMS (cambiar en cada bloque): CORTE = 2025-11-30 · MES_DESDE = 2025-11-01 (solo los cancelados en ese mes) · ANCLA = 2025-05-31 (primer día = 2025-06-01).
-- =====================================================================================

-- BLOQUE 1 — Por flujo de cancelación y tipo de préstamo: cuántos préstamos cancelados en el mes dejan mora sin reversar, y cuánto.
WITH params AS (SELECT DATE '2025-11-30' AS corte, DATE '2025-11-01' AS mes_desde FROM DUAL),
mv AS (
    SELECT m.PRSTCDGO,
           SUM(CASE WHEN m.MVICCMPN = 2 AND m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICCMPN = 2 AND m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) AS devengo,
           SUM(CASE WHEN m.MVICCMPN = 2 AND m.MVICTPMV = 9 THEN m.MVICVLOR ELSE 0 END) AS condonado_9,
           SUM(CASE WHEN m.MVICCMPN = 2 AND m.MVICTPMV IN (1, 4) THEN m.MVICVLOR WHEN m.MVICCMPN = 2 AND m.MVICTPMV IN (2, 3, 9, 10) THEN -m.MVICVLOR ELSE 0 END) AS prov_mora
      FROM CRD.MVIC m, params pa WHERE NVL(m.MVICANUL, 0) = 0 AND TRUNC(m.MVICFCCT) <= pa.corte GROUP BY m.PRSTCDGO
),
pg AS (
    SELECT d.PRSTCDGO, SUM(NVL(g.PGPRMRPG, 0)) AS mora_cobrada
      FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO, params pa
     WHERE NVL(g.PGPRANUL, 0) = 0 AND g.PGPRFCHA < pa.corte + 1 GROUP BY d.PRSTCDGO
),
ev AS (
    SELECT e.PRSTCDGO, MAX(e.EVPRTPOO) KEEP (DENSE_RANK LAST ORDER BY e.EVPRFCHA, e.EVPRCDGO) AS ultimo_tipo, MAX(e.EVPRFCHA) AS ultima_fecha
      FROM CRD.EVPR e, params pa WHERE e.EVPRESTD = 1 AND e.EVPRFCHA < pa.corte + 1 GROUP BY e.PRSTCDGO
),
fin AS (
    SELECT d.PRSTCDGO, MAX(NVL(d.DTPRFCPG, d.DTPRFCVN)) AS ultima_cuota FROM CRD.DTPR d GROUP BY d.PRSTCDGO
),
base AS (
    SELECT p.PRSTCDGO, p.PRSTIDAS AS operacion, p.PRSTIDST AS estado, t.TPPRCSPB AS tipo, NVL(ev.ultimo_tipo, '(sin evento: motor/Petro)') AS flujo,
           GREATEST(0, ROUND(NVL(mv.devengo, 0) - NVL(pg.mora_cobrada, 0) - NVL(mv.condonado_9, 0), 2)) AS mora_contabilizada_pendiente,
           GREATEST(0, ROUND(NVL(mv.prov_mora, 0), 2)) AS mora_provisionada,
           NVL(ev.ultima_fecha, fin.ultima_cuota) AS cancelado_el
      FROM CRD.PRST p JOIN CRD.PRDC prd ON prd.PRDCCDGO = p.PRDCCDGO JOIN CRD.TPPR t ON t.TPPRCDGO = prd.TPPRCDGO
      LEFT JOIN mv ON mv.PRSTCDGO = p.PRSTCDGO LEFT JOIN pg ON pg.PRSTCDGO = p.PRSTCDGO LEFT JOIN ev ON ev.PRSTCDGO = p.PRSTCDGO LEFT JOIN fin ON fin.PRSTCDGO = p.PRSTCDGO
     WHERE NVL(p.PRSTIDST, 0) IN (3, 4, 5, 9, 13)
)
SELECT b.flujo, b.tipo, COUNT(*) AS prestamos, ROUND(SUM(b.mora_contabilizada_pendiente), 2) AS mora_en_1402_sin_reversar, ROUND(SUM(b.mora_provisionada), 2) AS provision_de_mora_sin_reversar,
       SUM(CASE WHEN b.mora_contabilizada_pendiente > 0.005 THEN 1 ELSE 0 END) AS con_mora_en_1402, SUM(CASE WHEN b.mora_provisionada > 0.005 THEN 1 ELSE 0 END) AS con_provision
  FROM base b, params pa
 WHERE b.cancelado_el >= pa.mes_desde AND b.cancelado_el < pa.corte + 1 AND (b.mora_contabilizada_pendiente > 0.005 OR b.mora_provisionada > 0.005)
 GROUP BY b.flujo, b.tipo ORDER BY b.flujo, b.tipo;

-- BLOQUE 2 — El detalle por préstamo (los 200 mayores), de TODOS los cancelados (sin filtrar por mes) para ver también los de meses anteriores que siguen con mora.
-- Pegar los CTE del BLOQUE 1 (params, mv, pg, ev, fin, base) antes de este SELECT.
--   SELECT * FROM (
--     SELECT b.operacion, b.estado, b.tipo, b.flujo, TO_CHAR(b.cancelado_el, 'YYYY-MM-DD') AS cancelado_el, b.mora_contabilizada_pendiente, b.mora_provisionada
--       FROM base b WHERE b.mora_contabilizada_pendiente > 0.005 OR b.mora_provisionada > 0.005
--      ORDER BY b.mora_contabilizada_pendiente + b.mora_provisionada DESC
--   ) WHERE ROWNUM <= 200;

-- BLOQUE 3 — Operaciones que el cuadre señaló (PRSTIDAS): su estado, su último evento y su mora. Poner las operaciones en la lista.
-- Pegar los CTE del BLOQUE 1 antes de este SELECT.
--   SELECT b.operacion, b.estado, b.tipo, b.flujo, TO_CHAR(b.cancelado_el, 'YYYY-MM-DD') AS cancelado_el, b.mora_contabilizada_pendiente, b.mora_provisionada
--     FROM base b WHERE b.operacion IN (64015, 66155, 70474) ORDER BY b.operacion;

-- BLOQUE 4 — ¿Algún cancelado tiene ya un reverso de mora registrado por algún flujo? MVIC tipo 6 (reverso de devengo) o tipo 3 (reverso de provisión) con origen distinto de CIERRE/COBRO:
-- si el origen 'MORA_NO_COBRADA' (C43) es el único, solo el motor lo hace; si aparece uno de PRECANCELACION / CONDONACION / NOVACION / REESTRUCTURACION, ya hay quien lo haga.
SELECT m.MVICORGN AS origen, m.MVICTPMV AS tipo_mov, COUNT(*) AS filas, ROUND(SUM(m.MVICVLOR), 2) AS valor
  FROM CRD.MVIC m WHERE NVL(m.MVICANUL, 0) = 0 AND m.MVICCMPN = 2 AND m.MVICTPMV IN (3, 6, 9)
 GROUP BY m.MVICORGN, m.MVICTPMV ORDER BY 1, 2;

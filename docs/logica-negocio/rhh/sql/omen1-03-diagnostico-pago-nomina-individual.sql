-- =====================================================================================
-- omen1-03 — SOLO LECTURA. omen-saa-1, 2026-11-01. Reporte del usuario: los pagos de nómina de
-- OCTUBRE que salieron INDIVIDUALIZADOS (origen RHH_NOMINA_EMPLEADO, un pago por empleado en la bandeja
-- de Tesorería) se contabilizaron en HONORARIOS POR PAGAR en vez de REMUNERACIONES POR PAGAR.
--
-- Causa en el código (verificada por el árbitro):
--   - el pago CONSOLIDADO (camino viejo) toma la cuenta del DEBE de la plantilla de pago de nómina
--     (RHH.CFNM.CFNMPLPG), línea 50 SUELDOS_POR_PAGAR_DEBE → correcto;
--   - el pago INDIVIDUALIZADO (GeneracionOrdenPagoServiceImpl, desde 2026-10-05) NO usa esa plantilla:
--     la cuenta sale del grupo de producto de pago «PAGO DE NOMINA» (PGS.GRPP.PLNNCDGO), que rhh/sql/15
--     creó apuntando a una cuenta MARCADORA (9678) «hasta el plan de cuentas definitivo».
-- Este script confirma la cuenta de cada lado y mide lo que hay que reclasificar.
-- =====================================================================================

-- 1. El grupo «PAGO DE NOMINA» y su cuenta (la que hoy usa el pago individualizado).
SELECT g.GRPPCDGO, g.GRPPNMBR, g.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR, g.PJRQCDGO, g.GRPPESTD
  FROM PGS.GRPP g LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
 WHERE UPPER(g.GRPPNMBR) LIKE '%NOMINA%';

-- 2. La plantilla de pago de nómina de la configuración y su línea 50 (la cuenta CORRECTA del debe).
SELECT c.CFNMCDGO, c.CFNMPLPG AS ALTERNO_PLANTILLA_PAGO, c.CFNMCTMR AS CUENTA_MARCADORA,
       d.DTPLAXL1 AS AUX1, d.DTPLMVMN AS MOV, n.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR
  FROM RHH.CFNM c
  JOIN CNT.PLNS p ON p.PLNSCDAL = c.CFNMPLPG AND p.PJRQCDGO = c.PJRQCDGO
  JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO AND d.DTPLAXL1 IN (18, 50)
  LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 ORDER BY c.CFNMCDGO, d.DTPLAXL1;

-- 3. ¿Qué cuenta es la marcadora 9678? ¿Y cuáles son «honorarios por pagar» y «remuneraciones por pagar»?
SELECT n.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR, n.PLNNESTD FROM CNT.PLNN n
 WHERE n.PJRQCDGO = 1236
   AND (n.PLNNCDGO = 9678 OR UPPER(n.PLNNNMBR) LIKE '%HONORARIO%POR PAGAR%' OR UPPER(n.PLNNNMBR) LIKE '%REMUNERACION%POR PAGAR%'
        OR UPPER(n.PLNNNMBR) LIKE '%SUELDO%POR PAGAR%')
 ORDER BY n.PLNNCNTA;

-- 4. Pagos individualizados de nómina con asiento, por mes y por cuenta del DEBE. Lo que salga en
--    octubre en honorarios es lo que hay que reclasificar.
SELECT TO_CHAR(a.ASNTFCHA, 'YYYY-MM') AS MES, d.DTASCNTA AS CUENTA, COUNT(DISTINCT t.PGTRCDGO) AS PAGOS,
       SUM(NVL(d.DTASDBEE, 0)) AS DEBE, SUM(NVL(d.DTASHBRR, 0)) AS HABER
  FROM PGS.PGTR t
  JOIN CNT.ASNT a ON a.ASNTCDGO = t.PGTRASNT
  JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO
 WHERE t.PGTRORGN = 'RHH_NOMINA_EMPLEADO' AND a.ASNTESTD IN (1, 3)
 GROUP BY TO_CHAR(a.ASNTFCHA, 'YYYY-MM'), d.DTASCNTA
 ORDER BY 1, 2;

-- 5. Detalle de los pagos individualizados de octubre (para la reclasificación y su soporte).
SELECT t.PGTRCDGO, t.PGTRIDOR AS DRPG, t.PGTRESTD, a.ASNTCDGO, a.ASNTFCHA, d.DTASCNTA, d.DTASDBEE, d.DTASHBRR
  FROM PGS.PGTR t
  JOIN CNT.ASNT a ON a.ASNTCDGO = t.PGTRASNT
  JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO
 WHERE t.PGTRORGN = 'RHH_NOMINA_EMPLEADO' AND a.ASNTESTD IN (1, 3)
   AND a.ASNTFCHA >= DATE '2026-10-01' AND a.ASNTFCHA < DATE '2026-11-01'
 ORDER BY a.ASNTCDGO, d.DTASDBEE DESC;

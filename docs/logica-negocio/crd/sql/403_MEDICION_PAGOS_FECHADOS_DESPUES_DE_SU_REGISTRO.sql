-- =====================================================================================================================================
-- 403 - MEDICIÓN (solo lectura) de los pagos vigentes cuya FECHA (PGPRFCHA) es posterior a su REGISTRO (PGPRFCRG) y de la rotura FALSA que el control
-- C33 da por eso. Origen: préstamo 3492 (pagos de MIGRACIÓN fechados en el vencimiento futuro de la cuota, registrados el 22-04-2026).
-- Corte a medir: cambiar la línea DEFINE (por defecto el cierre de noviembre).
-- =====================================================================================================================================
DEFINE corte = '2026-11-30'

-- A. Pagos vigentes con fecha posterior a su registro, por tipo de pago y por si la fecha cae DESPUÉS del corte.
SELECT NVL(g.PGPRTPOO, '(sin tipo)') AS tipo, COUNT(*) AS pagos, COUNT(DISTINCT g.DTPRCDGO) AS cuotas,
       COUNT(DISTINCT d.PRSTCDGO) AS prestamos, ROUND(SUM(NVL(g.PGPRCPPG,0) + NVL(g.PGPRINPG,0)),2) AS capital_mas_interes,
       SUM(CASE WHEN g.PGPRFCHA >= TO_DATE('&corte','YYYY-MM-DD') + 1 THEN 1 ELSE 0 END) AS con_fecha_despues_del_corte
  FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
 WHERE NVL(g.PGPRANUL,0) = 0 AND g.PGPRFCHA IS NOT NULL AND g.PGPRFCRG IS NOT NULL AND g.PGPRFCHA > g.PGPRFCRG
 GROUP BY NVL(g.PGPRTPOO, '(sin tipo)') ORDER BY pagos DESC;

-- B. Los que importan al corte: fecha DESPUÉS del corte pero registrados ANTES (o el mismo día). Por tipo y usuario que los registró.
SELECT NVL(g.PGPRTPOO, '(sin tipo)') AS tipo, g.PGPRUSRG AS usuario_registro, COUNT(*) AS pagos, COUNT(DISTINCT d.PRSTCDGO) AS prestamos,
       MIN(g.PGPRFCRG) AS primer_registro, MAX(g.PGPRFCRG) AS ultimo_registro, MAX(g.PGPRFCHA) AS fecha_mas_lejana
  FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
 WHERE NVL(g.PGPRANUL,0) = 0 AND g.PGPRFCHA >= TO_DATE('&corte','YYYY-MM-DD') + 1 AND g.PGPRFCRG < TO_DATE('&corte','YYYY-MM-DD') + 1
 GROUP BY NVL(g.PGPRTPOO, '(sin tipo)'), g.PGPRUSRG ORDER BY pagos DESC;

-- C. Préstamos afectados (con al menos un pago del bloque B), cuántos y cuántos en cartera vigente.
SELECT COUNT(DISTINCT d.PRSTCDGO) AS prestamos_con_pagos_fechados_despues_del_corte
  FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
 WHERE NVL(g.PGPRANUL,0) = 0 AND g.PGPRFCHA >= TO_DATE('&corte','YYYY-MM-DD') + 1 AND g.PGPRFCRG < TO_DATE('&corte','YYYY-MM-DD') + 1;

-- D. EL CONTROL C33 AL CORTE EN DOS VARIANTES (misma definición que DetallePrestamoDaoServiceImpl.selectControlPrelacionAlCorte):
--    modo 0 = regla actual (cuentan los pagos con fecha <= corte)   modo 1 = regla nueva (cuentan también los REGISTRADOS hasta el corte).
--    Resultado: una fila por préstamo y tipo de rotura con el modo en que aparece. «SOLO_ACTUAL» = rotura FALSA que desaparece con la regla nueva.
--    «SOLO_NUEVA» = rotura que la regla nueva ve y la actual no (revisar). «AMBAS» = rotura real en los dos modos.
WITH modos AS (SELECT 0 AS modo FROM DUAL UNION ALL SELECT 1 FROM DUAL),
pg AS (
  SELECT m.modo, g.DTPRCDGO,
         SUM(NVL(g.PGPRCPPG,0) + NVL(g.PGPRINPG,0)) nuc, SUM(NVL(g.PGPRCPPG,0)) cap, SUM(NVL(g.PGPRINPG,0)) intr,
         SUM(CASE WHEN (NVL(g.PGPRTPOO,'-') IN ('PRECANCELACION', 'ABONO_CAPITAL', 'ACUERDO_CONDONACION') OR NVL(g.PGPRSLOT,0) > 0.01)
                  THEN 0 ELSE NVL(g.PGPRCPPG,0) + NVL(g.PGPRINPG,0) END) nne,
         SUM(CASE WHEN (NVL(g.PGPRTPOO,'-') IN ('PRECANCELACION', 'ABONO_CAPITAL', 'ACUERDO_CONDONACION') OR NVL(g.PGPRSLOT,0) > 0.01)
                  THEN 1 ELSE 0 END) nex
    FROM CRD.PGPR g CROSS JOIN modos m
   WHERE NVL(g.PGPRANUL,0) = 0
     AND (g.PGPRFCHA IS NULL OR g.PGPRFCHA < TO_DATE('&corte','YYYY-MM-DD') + 1
          OR (m.modo = 1 AND g.PGPRFCRG < TO_DATE('&corte','YYYY-MM-DD') + 1))
   GROUP BY m.modo, g.DTPRCDGO),
c AS (
  SELECT m.modo, d.PRSTCDGO, p.PRSTIDAS, p.PRSTIDST, d.DTPRNMCT, d.DTPRESTD, d.DTPRFCVN,
         NVL(d.DTPRCPTL,0) + NVL(d.DTPRINTR,0) - NVL(pg.nuc,0) nucleo, NVL(pg.nne,0) nne, NVL(pg.nex,0) nex,
         GREATEST(ROUND(NVL(pg.cap,0) - NVL(d.DTPRCPTL,0),2),0) + GREATEST(ROUND(NVL(pg.intr,0) - NVL(d.DTPRINTR,0),2),0) exceso
    FROM CRD.DTPR d JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO CROSS JOIN modos m
    LEFT JOIN pg ON pg.DTPRCDGO = d.DTPRCDGO AND pg.modo = m.modo
   WHERE p.PRSTFCHA < TO_DATE('&corte','YYYY-MM-DD') + 1 AND NVL(p.PRSTIDST,0) NOT IN (0, 1, 6, 7, 12, 14, 3, 4, 5, 9, 13)),
a AS (
  SELECT modo, PRSTCDGO, MIN(DTPRNMCT) n0 FROM c
   WHERE nucleo > 0.01 AND ((DTPRESTD IS NULL OR DTPRESTD NOT IN (4, 7)) OR (DTPRESTD = 4 AND DTPRFCVN >= DATE '2025-06-01'))
   GROUP BY modo, PRSTCDGO),
rotos AS (
  SELECT a.modo, a.PRSTCDGO, 'PRELACION' AS tipo, a.n0 AS cuota, ROUND(MAX(c0.nucleo),2) AS monto
    FROM a JOIN c c0 ON c0.modo = a.modo AND c0.PRSTCDGO = a.PRSTCDGO AND c0.DTPRNMCT = a.n0
           JOIN c ON c.modo = a.modo AND c.PRSTCDGO = a.PRSTCDGO AND c.DTPRNMCT > a.n0 AND c.nne > 0.01
   GROUP BY a.modo, a.PRSTCDGO, a.n0
  UNION ALL
  SELECT c.modo, c.PRSTCDGO, 'SOBREIMPUTADA', MIN(c.DTPRNMCT), ROUND(SUM(c.exceso),2)
    FROM c WHERE c.DTPRFCVN >= DATE '2025-06-01' AND c.exceso > 0.01 AND c.nne > 0.01 AND c.nex = 0
   GROUP BY c.modo, c.PRSTCDGO)
SELECT NVL(r0.PRSTCDGO, r1.PRSTCDGO) AS prestamo, NVL(r0.tipo, r1.tipo) AS tipo,
       r0.cuota AS cuota_actual, r0.monto AS monto_actual, r1.cuota AS cuota_nueva, r1.monto AS monto_nuevo,
       CASE WHEN r0.PRSTCDGO IS NOT NULL AND r1.PRSTCDGO IS NOT NULL THEN 'AMBAS'
            WHEN r0.PRSTCDGO IS NOT NULL THEN 'SOLO_ACTUAL (rotura falsa)' ELSE 'SOLO_NUEVA (revisar)' END AS veredicto
  FROM (SELECT * FROM rotos WHERE modo = 0) r0 FULL OUTER JOIN (SELECT * FROM rotos WHERE modo = 1) r1
    ON r0.PRSTCDGO = r1.PRSTCDGO AND r0.tipo = r1.tipo
 ORDER BY veredicto, prestamo;

-- E. Resumen del bloque D (cuántos préstamos de cada veredicto): se obtiene contando las filas anteriores por veredicto.
--    El préstamo 3492 debe salir «SOLO_ACTUAL (rotura falsa)» si sus cuotas de migración están pagadas.

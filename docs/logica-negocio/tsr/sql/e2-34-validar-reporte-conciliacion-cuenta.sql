-- =====================================================================
-- e2-34 — Validar RPRT_CNCL_CNTA contra la base antes de usarlo
-- Modulo: TSR  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-08
--
-- ✅ SOLO LECTURA. No inserta, no borra, no hace COMMIT.
--
-- 🔴 POR QUE EXISTE
--   §6 del diseno (DISENO-REPORTES-CONCILIACION-BANCARIA.md): antes de dar por
--   bueno el reporte hay que comprobar dos cosas con una cuenta/periodo REALES:
--     1. La cabecera devuelve UNA sola fila (los LEFT JOIN con MAX() estan para
--        eso; si devuelve 2, hay un cierre anulado o un segundo extracto que
--        los esta duplicando).
--     2. Los pendientes que imprime coinciden con los que muestra la pantalla
--        de Conciliacion Contable: en el resumen del periodo, cada cuenta dice
--        "N pendiente(s) extracto · M pendiente(s) contabilidad". Los puntos
--        3 y 4 de este script calculan N y M con LA MISMA REGLA que el .jrxml.
--
--   Las consultas son copia literal de las del RPRT_CNCL_CNTA.jrxml (secciones
--   0, 3 y 4), con los parametros del reporte reemplazados por :cuenta y
--   :periodo. DBeaver los pide al ejecutar.
--
--   :cuenta  = TSR.CNBC.CNBCCDGO (codigo interno de la cuenta bancaria)
--   :periodo = CNT.PRDO.PRDOCDGO (codigo interno del periodo contable)
--   El punto 0 te los da.
-- =====================================================================


-- =====================================================================
-- 0 — Que cuenta/periodo usar: las combinaciones que YA tienen cierre CERRADO.
--     Elegir una y anotar CNBCCDGO y PRDOCDGO.
-- =====================================================================
SELECT '0 - cierres cerrados' AS control,
       ci.CNBCCDGO AS cuenta, b.BNCONMBR AS banco, c.CNBCNMRO AS numero,
       ci.CNCLPRDO AS periodo, p.PRDONMBR AS nombre_periodo,
       ci.CNCLFCCR AS fecha_cierre, ci.CNCLUSCR AS cerrado_por
  FROM TSR.CNCL ci
  JOIN TSR.CNBC c ON c.CNBCCDGO = ci.CNBCCDGO
  JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO
  JOIN CNT.PRDO p ON p.PRDOCDGO = ci.CNCLPRDO
 WHERE ci.CNCLESTD = 2
 ORDER BY p.PRDOANNN DESC, p.PRDOMSSS DESC, b.BNCONMBR;


-- =====================================================================
-- 1 — LA CABECERA. ESPERADO: exactamente 1 fila.
--     Si devuelve 0: la cuenta o el periodo no existen (o el periodo es de
--     otra empresa). Si devuelve 2 o mas: AVISAR con el resultado, el reporte
--     imprimiria la cabecera duplicada.
-- =====================================================================
SELECT '1 - cabecera' AS control,
       e.PJRQNMBR AS empresa, p.PRDONMBR AS periodo, b.BNCONMBR AS banco, c.CNBCNMRO AS numero_cuenta,
       pc.PLNNCNTA AS cuenta_contable,
       ci.CNCLCDGO AS id_cierre, ci.CNCLESTD AS estado_cierre, ci.CNCLFCCR AS fecha_cierre,
       ci.CNCLSLDF AS saldo_libros, ci.CNCLDPTR AS dep_transito, ci.CNCLCHNC AS chq_no_cobrados,
       ci.CNCLNCTR AS nc_no_reg, ci.CNCLNDTR AS nd_no_reg,
       (NVL(ci.CNCLSLDF,0) - NVL(ci.CNCLDPTR,0) + NVL(ci.CNCLCHNC,0) + NVL(ci.CNCLNCTR,0) - NVL(ci.CNCLNDTR,0)) AS extracto_esperado,
       ci.CNCLSLDE AS extracto_declarado,
       (NVL(ci.CNCLSLDF,0) - NVL(ci.CNCLDPTR,0) + NVL(ci.CNCLCHNC,0) + NVL(ci.CNCLNCTR,0) - NVL(ci.CNCLNDTR,0)) - NVL(ci.CNCLSLDE,0) AS diferencia_debe_ser_0,
       cc.CNCTESTR AS estado_revision, cc.CNCTPDEX AS pend_extracto_guardado, cc.CNCTPDAS AS pend_asiento_guardado,
       ex.EXBCARCH AS extracto_archivo, ct.CTEBCRRE AS mes_cerrado
  FROM TSR.CNBC c
  JOIN TSR.BNCO b   ON b.BNCOCDGO = c.BNCOCDGO
  JOIN CNT.PLNN pc  ON pc.PLNNCDGO = c.PLNNCDGO
  JOIN CNT.PRDO p   ON p.PRDOCDGO = :periodo
  JOIN SCP.PJRQ e   ON e.PJRQCDGO = p.PJRQCDGO
  LEFT JOIN TSR.CNCL ci ON ci.CNCLCDGO = (
      SELECT MAX(x.CNCLCDGO) KEEP (DENSE_RANK LAST ORDER BY x.CNCLFCCR)
        FROM TSR.CNCL x
       WHERE x.CNBCCDGO = c.CNBCCDGO AND x.CNCLPRDO = p.PRDOCDGO AND x.CNCLESTD = 2)
  LEFT JOIN TSR.CNCT cc ON cc.CNBCCDGO = c.CNBCCDGO AND cc.PRDOCDGO = p.PRDOCDGO
  LEFT JOIN TSR.EXBC ex ON ex.EXBCCDGO = (
      SELECT MAX(y.EXBCCDGO) FROM TSR.EXBC y
       WHERE y.CNBCCDGO = c.CNBCCDGO AND y.PRDOCDGO = p.PRDOCDGO AND y.EXBCESTD = 1)
  LEFT JOIN TSR.CTEB ct ON ct.PJRQCDGO = p.PJRQCDGO AND ct.PRDOCDGO = p.PRDOCDGO
 WHERE c.CNBCCDGO = :cuenta;


-- =====================================================================
-- 3 — PENDIENTES DEL EXTRACTO sin conciliar (misma regla que el .jrxml y que
--     GrupoConciliacionExtractoDaoServiceImpl). Comparar "total" con el "N
--     pendiente(s) extracto" de la pantalla para esa cuenta/periodo.
-- =====================================================================
SELECT '3 - pendientes extracto' AS control,
       COUNT(*) AS total,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM TSR.DTCN t WHERE t.DTCNIDEX = de.DEXBCDGO AND t.DTCNESTD = 1) THEN 1 ELSE 0 END) AS declarados_en_transito,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM TSR.DTCN t WHERE t.DTCNIDEX = de.DEXBCDGO AND t.DTCNESTD = 1) THEN 0 ELSE 1 END) AS sin_declarar
  FROM TSR.DEXB de
 WHERE de.CNBCCDGO = :cuenta
   AND de.DEXBESTD = 1
   AND (   de.PRDOCDGO = :periodo
        OR de.DEXBCDGO IN (SELECT t.DTCNIDEX FROM TSR.DTCN t WHERE t.DTCNTPOO IN (3,4) AND t.DTCNESTD = 1))
   AND de.DEXBCDGO NOT IN (SELECT x.DEXBCDGO FROM TSR.GCEX x JOIN TSR.GRCC g ON g.GRCCCDGO = x.GRCCCDGO
                            WHERE g.GRCCESTD = 1);


-- =====================================================================
-- 4 — PENDIENTES DE CONTABILIDAD sin conciliar (misma regla que el .jrxml y
--     que GrupoConciliacionAsientoDaoServiceImpl: arrastre por MVCB -> ASNT).
--     Comparar "total" con el "M pendiente(s) contabilidad" de la pantalla.
-- =====================================================================
SELECT '4 - pendientes contabilidad' AS control,
       COUNT(*) AS total,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM TSR.DTCN t WHERE t.DTCNDTAS = da.DTASCDGO AND t.DTCNESTD = 1) THEN 1 ELSE 0 END) AS declarados_en_transito,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM TSR.DTCN t WHERE t.DTCNDTAS = da.DTASCDGO AND t.DTCNESTD = 1) THEN 0 ELSE 1 END) AS sin_declarar
  FROM CNT.DTAS da
  JOIN CNT.ASNT a  ON a.ASNTCDGO = da.ASNTCDGO
  JOIN CNT.PRDO p2 ON p2.PRDOCDGO = :periodo
 WHERE da.PLNNCDGO = (SELECT c2.PLNNCDGO FROM TSR.CNBC c2 WHERE c2.CNBCCDGO = :cuenta)
   AND a.PJRQCDGO = p2.PJRQCDGO
   AND a.ASNTESTD IN (1, 3)
   AND (   a.ASNTFCHA BETWEEN p2.PRDOINCO AND p2.PRDOFNN
        OR a.ASNTCDGO IN (SELECT m.ASNTCDGO FROM TSR.MVCB m JOIN TSR.DTCN t2 ON t2.MVCBCDGO = m.MVCBCDGO
                           WHERE t2.DTCNTPOO IN (1,2) AND t2.DTCNESTD = 1))
   AND da.DTASCDGO NOT IN (SELECT y2.DTASCDGO FROM TSR.GCAS y2 JOIN TSR.GRCC g2 ON g2.GRCCCDGO = y2.GRCCCDGO
                            WHERE g2.GRCCESTD = 1);


-- =====================================================================
-- QUE SIGUE, segun lo que devuelva
-- =====================================================================
-- - 1 devuelve 1 fila y diferencia_debe_ser_0 = 0 (o dentro de la tolerancia):
--   la ecuacion guardada cuadra y la cabecera es unica. OK.
-- - 3 y 4 coinciden con la pantalla: los pendientes del reporte son los de la
--   pantalla. OK. Si difieren, AVISAR con los dos numeros: gana la pantalla y
--   se corrige el .jrxml.
-- - Si "sin_declarar" > 0 en 3 o 4, el reporte los va a imprimir en rojo como
--   "SIN DECLARAR": es informacion, no error del reporte.
-- =====================================================================

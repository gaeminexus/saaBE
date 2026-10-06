-- =====================================================================================
-- e2-92 — Banco Pacífico 1048142735: el depósito en tránsito de agosto (866,70) no aparece en la
-- conciliación de septiembre. SOLO LECTURA. Equipo omen-saa-2 · 2026-10-06.
--
-- Hipótesis (leída en el código): el cierre de agosto declaró la partida en TSR.DTCN anclada a la LÍNEA DE
-- ASIENTO (DTCNDTAS), que es el ancla vigente desde la corrección del 2026-08-27
-- (DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md §10.4). Pero la consulta que arrastra los pendientes de
-- contabilidad al mes siguiente (GrupoConciliacionAsientoDaoServiceImpl.FRAGMENTO_ARRASTRE_ASIENTO) se quedó
-- con el ancla VIEJA: exige dt.movimientoBanco no nulo y arrastra por dt.movimientoBanco.asiento. Una partida
-- sin MVCBCDGO no se arrastra nunca. La del extracto (FRAGMENTO_ARRASTRE_EXTRACTO) y la que la salda
-- (selectPendientePorAsiento) ya usan el ancla nueva: solo falla el arrastre de contabilidad.
-- ESPERADO en el BLOQUE 1: una fila tipo 1 (depósito en tránsito), 866,70, estado 1 (pendiente), con
-- DTCNDTAS lleno y MVCBCDGO NULO. Si MVCBCDGO viene lleno, la hipótesis es falsa: pasarme la salida.
-- Columnas copiadas de DetalleTransito (DTCN), Conciliacion (CNCL), DetalleAsiento (DTAS) y Asiento (ASNT).
-- =====================================================================================

-- BLOQUE 1 — Las partidas en tránsito pendientes de la cuenta 1048142735.
SELECT 'BLOQUE 1 - partidas pendientes' AS bloque, dt.DTCNCDGO, dt.DTCNTPOO AS tipo, dt.DTCNVLOR, dt.DTCNESTD,
       dt.DTCNDTAS, dt.MVCBCDGO, dt.DTCNIDEX, c.CNCLCDGO, c.CNCLPRDO, a.ASNTNMAL, TRUNC(a.ASNTFCHA) AS fecha_asiento,
       d.DTASDBEE, d.DTASHBRR, SUBSTR(d.DTASDSCR, 1, 60) AS descripcion
  FROM TSR.DTCN dt
  JOIN TSR.CNCL c ON c.CNCLCDGO = dt.CNCLCDGO
  JOIN TSR.CNBC b ON b.CNBCCDGO = c.CNBCCDGO
  LEFT JOIN CNT.DTAS d ON d.DTASCDGO = dt.DTCNDTAS
  LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
 WHERE b.CNBCNMRO = '1048142735'
   AND dt.DTCNESTD = 1
 ORDER BY dt.DTCNCDGO;

-- BLOQUE 2 — Cuántas partidas pendientes, en TODAS las cuentas, tienen el mismo problema (ancla en la línea de
-- asiento y sin movimiento bancario). Cada una es un depósito o un cheque que no va a aparecer en el mes
-- siguiente hasta que se corrija el código.
SELECT 'BLOQUE 2 - sin arrastre, todas las cuentas' AS bloque, b.CNBCNMRO, dt.DTCNTPOO AS tipo, COUNT(*) AS partidas,
       SUM(dt.DTCNVLOR) AS valor
  FROM TSR.DTCN dt
  JOIN TSR.CNCL c ON c.CNCLCDGO = dt.CNCLCDGO
  JOIN TSR.CNBC b ON b.CNBCCDGO = c.CNBCCDGO
 WHERE dt.DTCNESTD = 1
   AND dt.DTCNTPOO IN (1, 2)
   AND dt.DTCNDTAS IS NOT NULL
   AND dt.MVCBCDGO IS NULL
 GROUP BY b.CNBCNMRO, dt.DTCNTPOO
 ORDER BY b.CNBCNMRO, dt.DTCNTPOO;

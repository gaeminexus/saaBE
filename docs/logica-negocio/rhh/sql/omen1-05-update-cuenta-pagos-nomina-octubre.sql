-- =====================================================================================
-- omen1-05 — UPDATE DIRECTO AUTORIZADO POR EL USUARIO (2026-11-01). omen-saa-1.
-- Corrige la cuenta del DEBE de los 20 pagos de nómina individualizados del 28-10-2026
-- (origen RHH_NOMINA_EMPLEADO, asientos 12129 a 12148, total 17.244,97):
--     2.3.90.15 HONORARIOS POR PAGAR (PLNNCDGO 9678)  →  2.5.01 REMUNERACIONES POR PAGAR (PLNNCDGO 9689)
-- Solo toca las líneas DEBE en 2.3.90.15 de esos asientos. La línea del banco no se toca.
-- En CNT.DTAS la cuenta vive en TRES columnas (PLNNCDGO, DTASCNTA y DTASNMCT); se actualizan las tres.
-- Correr ANTES o DESPUÉS de omen1-04 (son independientes). Diagnóstico: omen1-03.
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES
-- 0.1 Las líneas a corregir. ESPERADO: 20 filas, todas PLNNCDGO 9678 / 2.3.90.15, debe total 17.244,97.
SELECT COUNT(*) AS LINEAS, SUM(d.DTASDBEE) AS DEBE, MIN(d.PLNNCDGO) AS PLNN_MIN, MAX(d.PLNNCDGO) AS PLNN_MAX,
       MIN(d.DTASCNTA) AS CTA_MIN, MAX(d.DTASCNTA) AS CTA_MAX
  FROM CNT.DTAS d
 WHERE d.ASNTCDGO IN (SELECT t.PGTRASNT FROM PGS.PGTR t WHERE t.PGTRORGN = 'RHH_NOMINA_EMPLEADO' AND t.PGTRASNT IS NOT NULL)
   AND d.DTASCNTA = '2.3.90.15';

-- 0.2 Los asientos, su estado y si ya están mayorizados (ASNTESTD y MYRZCDGO).
SELECT a.ASNTCDGO, a.ASNTFCHA, a.ASNTESTD, a.MYRZCDGO
  FROM CNT.ASNT a
 WHERE a.ASNTCDGO IN (SELECT t.PGTRASNT FROM PGS.PGTR t WHERE t.PGTRORGN = 'RHH_NOMINA_EMPLEADO' AND t.PGTRASNT IS NOT NULL)
 ORDER BY a.ASNTCDGO;

-- 0.3 La cuenta destino existe y su nombre. ESPERADO: 9689 / 2.5.01 / REMUNERACIONES POR PAGAR.
SELECT n.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR FROM CNT.PLNN n WHERE n.PLNNCDGO = 9689;


-- BLOQUE 1 — RESPALDO de las filas que se van a tocar (para el reverso).
CREATE TABLE CNT.BK_DTAS_NOMINA_OCT26 AS
SELECT d.* FROM CNT.DTAS d
 WHERE d.ASNTCDGO IN (SELECT t.PGTRASNT FROM PGS.PGTR t WHERE t.PGTRORGN = 'RHH_NOMINA_EMPLEADO' AND t.PGTRASNT IS NOT NULL)
   AND d.DTASCNTA = '2.3.90.15';
-- (!) Debe crear la tabla con 20 filas:
SELECT COUNT(*) AS RESPALDADAS, SUM(DTASDBEE) AS DEBE FROM CNT.BK_DTAS_NOMINA_OCT26;


-- BLOQUE 2 — (ESCRIBE) El UPDATE.
UPDATE CNT.DTAS d
   SET d.PLNNCDGO = 9689,
       d.DTASCNTA = '2.5.01',
       d.DTASNMCT = (SELECT n.PLNNNMBR FROM CNT.PLNN n WHERE n.PLNNCDGO = 9689)
 WHERE d.DTASCDGO IN (SELECT b.DTASCDGO FROM CNT.BK_DTAS_NOMINA_OCT26 b);
-- (!) "20 filas actualizadas". Si dice otro número: ROLLBACK; y pasar la salida.
COMMIT;


-- BLOQUE 3 — CONTROL DESPUÉS
-- 3.1 Ya no queda ninguna línea de esos pagos en 2.3.90.15. ESPERADO: 0.
SELECT COUNT(*) AS QUEDAN_EN_HONORARIOS FROM CNT.DTAS d
 WHERE d.ASNTCDGO IN (SELECT t.PGTRASNT FROM PGS.PGTR t WHERE t.PGTRORGN = 'RHH_NOMINA_EMPLEADO' AND t.PGTRASNT IS NOT NULL)
   AND d.DTASCNTA = '2.3.90.15';
-- 3.2 Por cuenta, octubre. ESPERADO: 2.5.01 debe 17.244,97 y 1.1.02.05.80 haber 17.244,97.
SELECT d.DTASCNTA, d.PLNNCDGO, d.DTASNMCT, SUM(NVL(d.DTASDBEE, 0)) AS DEBE, SUM(NVL(d.DTASHBRR, 0)) AS HABER
  FROM CNT.DTAS d
 WHERE d.ASNTCDGO IN (SELECT t.PGTRASNT FROM PGS.PGTR t WHERE t.PGTRORGN = 'RHH_NOMINA_EMPLEADO' AND t.PGTRASNT IS NOT NULL)
 GROUP BY d.DTASCNTA, d.PLNNCDGO, d.DTASNMCT ORDER BY 1;
-- 3.3 Cada asiento sigue cuadrado. ESPERADO: 0 filas.
SELECT d.ASNTCDGO, SUM(NVL(d.DTASDBEE, 0)) - SUM(NVL(d.DTASHBRR, 0)) AS DIFERENCIA FROM CNT.DTAS d
 WHERE d.ASNTCDGO IN (SELECT t.PGTRASNT FROM PGS.PGTR t WHERE t.PGTRORGN = 'RHH_NOMINA_EMPLEADO' AND t.PGTRASNT IS NOT NULL)
 GROUP BY d.ASNTCDGO HAVING ABS(SUM(NVL(d.DTASDBEE, 0)) - SUM(NVL(d.DTASHBRR, 0))) > 0.005;

-- ⚠ Si en 0.2 los asientos salieron con MYRZCDGO (ya mayorizados), los saldos del mayor de octubre
--    quedaron calculados con la cuenta vieja: hay que VOLVER A MAYORIZAR octubre 2026 desde la pantalla de
--    mayorización después de este UPDATE.


-- REVERSO (comentado):
-- UPDATE CNT.DTAS d
--    SET (d.PLNNCDGO, d.DTASCNTA, d.DTASNMCT) =
--        (SELECT b.PLNNCDGO, b.DTASCNTA, b.DTASNMCT FROM CNT.BK_DTAS_NOMINA_OCT26 b WHERE b.DTASCDGO = d.DTASCDGO)
--  WHERE d.DTASCDGO IN (SELECT b.DTASCDGO FROM CNT.BK_DTAS_NOMINA_OCT26 b);
-- COMMIT;
-- (Cuando ya no haga falta: DROP TABLE CNT.BK_DTAS_NOMINA_OCT26;)

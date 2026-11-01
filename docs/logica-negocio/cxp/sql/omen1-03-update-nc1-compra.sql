-- =====================================================================================
-- omen1-03 (cxp) — UPDATE DIRECTO AUTORIZADO POR EL USUARIO (2026-11-01). omen-saa-1.
-- NC de compra 1 (072-143-000143071, 20,68), sobre la factura NORMAL 072-109-000486746, cuyo asiento cargó el
-- gasto en 4.8.90.90.05 ALIMENTACIÓN, CAFETERÍA Y REFRIGERIO (más IVA 1.9.01.90.15 y proveedor 2.3.90.10).
-- La NC acreditó el reverso del gasto en 4.4.01.05 SERVICIOS BÁSICOS (la «cuenta por defecto», el defecto que
-- corrige N2). Se lleva esa línea a 4.8.90.90.05. La línea de IVA y la del proveedor están bien: no se tocan.
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES
-- 0.1 La línea a corregir. ESPERADO: 1 fila, 4.4.01.05, HABER 20,68, descripción «NC Compra reverso gasto».
SELECT d.DTASCDGO, d.ASNTCDGO, d.PLNNCDGO, d.DTASCNTA, d.DTASDSCR, d.DTASDBEE, d.DTASHBRR
  FROM PGS.NTCC n JOIN CNT.DTAS d ON d.ASNTCDGO = n.ASIENTO
 WHERE n.ID = 1 AND n.NUMERO = '072-143-000143071'
 ORDER BY d.DTASCDGO;
-- 0.2 La cuenta destino. ESPERADO: 1 fila, 4.8.90.90.05.
SELECT n.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR FROM CNT.PLNN n WHERE n.PJRQCDGO = 1236 AND n.PLNNCNTA = '4.8.90.90.05';
-- 0.3 Mayorización del asiento.
SELECT a.ASNTCDGO, a.ASNTFCHA, a.ASNTESTD, a.MYRZCDGO
  FROM PGS.NTCC n JOIN CNT.ASNT a ON a.ASNTCDGO = n.ASIENTO WHERE n.ID = 1;


-- BLOQUE 1 — RESPALDO
CREATE TABLE CNT.BK_DTAS_NC1_OCT26 AS
SELECT d.* FROM PGS.NTCC n JOIN CNT.DTAS d ON d.ASNTCDGO = n.ASIENTO
 WHERE n.ID = 1 AND d.DTASCNTA = '4.4.01.05' AND d.DTASDSCR = 'NC Compra reverso gasto';
SELECT COUNT(*) AS RESPALDADAS, SUM(DTASHBRR) AS HABER FROM CNT.BK_DTAS_NC1_OCT26;   -- 1 / 20,68


-- BLOQUE 2 — (ESCRIBE)
UPDATE CNT.DTAS d
   SET d.PLNNCDGO = (SELECT n.PLNNCDGO FROM CNT.PLNN n WHERE n.PJRQCDGO = 1236 AND n.PLNNCNTA = '4.8.90.90.05'),
       d.DTASCNTA = '4.8.90.90.05',
       d.DTASNMCT = (SELECT n.PLNNNMBR FROM CNT.PLNN n WHERE n.PJRQCDGO = 1236 AND n.PLNNCNTA = '4.8.90.90.05')
 WHERE d.DTASCDGO IN (SELECT b.DTASCDGO FROM CNT.BK_DTAS_NC1_OCT26 b);
-- (!) "1 fila actualizada". Si no: ROLLBACK; y pasar la salida.
COMMIT;


-- BLOQUE 3 — CONTROL DESPUÉS. ESPERADO: la NC 1 con DEBE 2.3.90.10 y HABER 4.8.90.90.05 (20,68) más su IVA; cuadrada.
SELECT d.DTASCNTA, d.DTASNMCT, d.DTASDBEE, d.DTASHBRR
  FROM PGS.NTCC n JOIN CNT.DTAS d ON d.ASNTCDGO = n.ASIENTO WHERE n.ID = 1 ORDER BY d.DTASCDGO;
SELECT SUM(NVL(d.DTASDBEE, 0)) - SUM(NVL(d.DTASHBRR, 0)) AS DIFERENCIA
  FROM PGS.NTCC n JOIN CNT.DTAS d ON d.ASNTCDGO = n.ASIENTO WHERE n.ID = 1;   -- 0
-- ⚠ Si en 0.3 el asiento tenía MYRZCDGO, volver a mayorizar octubre 2026 después.


-- REVERSO (comentado):
-- UPDATE CNT.DTAS d
--    SET (d.PLNNCDGO, d.DTASCNTA, d.DTASNMCT) =
--        (SELECT b.PLNNCDGO, b.DTASCNTA, b.DTASNMCT FROM CNT.BK_DTAS_NC1_OCT26 b WHERE b.DTASCDGO = d.DTASCDGO)
--  WHERE d.DTASCDGO IN (SELECT b.DTASCDGO FROM CNT.BK_DTAS_NC1_OCT26 b);
-- COMMIT;

-- RESULTADO (2026-11-01, producción): corrido. NC 1 → DEBE 2.3.90.10 23,78 / HABER 4.8.90.90.05 20,68 + IVA
-- 1.9.01.90.15 3,10; diferencia 0. Con esto quedan corregidas las 3 NC de compra que el motor viejo había mandado
-- a servicios básicos (omen1-01, bloque 5).

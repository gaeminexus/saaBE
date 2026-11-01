-- =====================================================================================
-- omen1-02 (cxp) — UPDATE DIRECTO AUTORIZADO POR EL USUARIO (2026-11-01). omen-saa-1.
-- Corrige el asiento de las 2 NC de compra sobre facturas de INTERMEDIARIO (diagnóstico omen1-01):
--   NC 41 002-002-000035370 (asiento 12118, 85,32), factura 002-002-000137309 → producto 702,
--         cuenta correcta 1.4.90.90.10 (PLNN 10592) CUENTA POR COBRAR SEGURO DE DESGRAVAMEN PARTICIPES.
--         Línea 30812 (HABER 85,32 en 4.4.01.05) → 10592.
--   NC 42 002-002-000035420 (asiento 12119, 1.108,26), factura 002-002-000137724 → producto 850,
--         cuenta correcta 1.4.90.15.02 (PLNN 10306) SEGURO PRESTAMOS HIPOTECARIOS.
--         Línea 30814 (HABER 963,70 en 4.4.01.05) y línea 30815 (HABER 144,56 en IVA 1.9.01.90.15) → 10306.
--         La factura de intermediario se contabilizó por el TOTAL con impuestos, sin crédito de IVA; la NC
--         es su espejo: ninguna línea de IVA. Se reasignan las dos líneas a 10306 (suman 1.108,26), en vez de
--         borrar una, para no tocar importes.
-- Las líneas del proveedor (30811 y 30813, DEBE 2.3.90.10) están bien y no se tocan.
-- La NC 1 (072-143-000143071, 20,68) NO es de intermediario: este script NO la toca; el bloque 0.3 muestra
-- la cuenta del gasto de su factura para decidir aparte si quedó bien.
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES
-- 0.1 Las 3 líneas a corregir. ESPERADO: 30812 (85,32), 30814 (963,70), 30815 (144,56), en los asientos 12118/12119.
SELECT d.DTASCDGO, d.ASNTCDGO, d.PLNNCDGO, d.DTASCNTA, d.DTASDSCR, d.DTASDBEE, d.DTASHBRR
  FROM CNT.DTAS d WHERE d.DTASCDGO IN (30812, 30814, 30815) ORDER BY d.DTASCDGO;
-- 0.2 Las cuentas destino. ESPERADO: 10592 1.4.90.90.10 y 10306 1.4.90.15.02.
SELECT n.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR FROM CNT.PLNN n WHERE n.PLNNCDGO IN (10592, 10306);
-- 0.3 NC 1 (no intermediario): cuentas del DEBE del asiento de su factura 072-109-000486746. Si la factura
--     está en 4.4.01.05 SERVICIOS BÁSICOS, la NC 1 quedó bien y no se toca.
SELECT f.NUMERO AS FC, d.DTASCNTA, d.DTASNMCT, d.DTASDBEE, d.DTASHBRR
  FROM PGS.FCTC f JOIN CNT.DTAS d ON d.ASNTCDGO = f.ASIENTO
 WHERE f.NUMERO = '072-109-000486746' AND f.ESTADO <> 0
 ORDER BY d.DTASDBEE DESC;
-- 0.4 Estado y mayorización de los dos asientos.
SELECT a.ASNTCDGO, a.ASNTFCHA, a.ASNTESTD, a.MYRZCDGO FROM CNT.ASNT a WHERE a.ASNTCDGO IN (12118, 12119);


-- BLOQUE 1 — RESPALDO
CREATE TABLE CNT.BK_DTAS_NC_INTERM_OCT26 AS
SELECT d.* FROM CNT.DTAS d WHERE d.DTASCDGO IN (30812, 30814, 30815);
SELECT COUNT(*) AS RESPALDADAS, SUM(DTASHBRR) AS HABER FROM CNT.BK_DTAS_NC_INTERM_OCT26;   -- 3 / 1.193,58


-- BLOQUE 2 — (ESCRIBE)
UPDATE CNT.DTAS d
   SET d.PLNNCDGO = 10592, d.DTASCNTA = '1.4.90.90.10',
       d.DTASNMCT = (SELECT n.PLNNNMBR FROM CNT.PLNN n WHERE n.PLNNCDGO = 10592),
       d.DTASDSCR = 'NC intermediario: reverso a la cuenta del producto de la factura 002-002-000137309'
 WHERE d.DTASCDGO = 30812 AND d.ASNTCDGO = 12118 AND d.PLNNCDGO = 9784;
-- (!) "1 fila actualizada".
UPDATE CNT.DTAS d
   SET d.PLNNCDGO = 10306, d.DTASCNTA = '1.4.90.15.02',
       d.DTASNMCT = (SELECT n.PLNNNMBR FROM CNT.PLNN n WHERE n.PLNNCDGO = 10306),
       d.DTASDSCR = 'NC intermediario: reverso a la cuenta del producto de la factura 002-002-000137724'
 WHERE d.DTASCDGO IN (30814, 30815) AND d.ASNTCDGO = 12119 AND d.PLNNCDGO IN (9784, 10615);
-- (!) "2 filas actualizadas". Si algún número no coincide: ROLLBACK; y pasar la salida.
COMMIT;


-- BLOQUE 3 — CONTROL DESPUÉS
-- 3.1 Por asiento y cuenta. ESPERADO: 12118 → DEBE 2.3.90.10 85,32 / HABER 1.4.90.90.10 85,32;
--                                    12119 → DEBE 2.3.90.10 1.108,26 / HABER 1.4.90.15.02 1.108,26.
SELECT d.ASNTCDGO, d.DTASCNTA, d.DTASNMCT, SUM(NVL(d.DTASDBEE, 0)) AS DEBE, SUM(NVL(d.DTASHBRR, 0)) AS HABER
  FROM CNT.DTAS d WHERE d.ASNTCDGO IN (12118, 12119)
 GROUP BY d.ASNTCDGO, d.DTASCNTA, d.DTASNMCT ORDER BY 1, 2;
-- 3.2 Cuadre. ESPERADO: 0 filas.
SELECT d.ASNTCDGO FROM CNT.DTAS d WHERE d.ASNTCDGO IN (12118, 12119)
 GROUP BY d.ASNTCDGO HAVING ABS(SUM(NVL(d.DTASDBEE, 0)) - SUM(NVL(d.DTASHBRR, 0))) > 0.005;
-- ⚠ Si en 0.4 algún asiento tenía MYRZCDGO, volver a mayorizar octubre 2026 después.


-- REVERSO (comentado):
-- UPDATE CNT.DTAS d
--    SET (d.PLNNCDGO, d.DTASCNTA, d.DTASNMCT, d.DTASDSCR) =
--        (SELECT b.PLNNCDGO, b.DTASCNTA, b.DTASNMCT, b.DTASDSCR FROM CNT.BK_DTAS_NC_INTERM_OCT26 b WHERE b.DTASCDGO = d.DTASCDGO)
--  WHERE d.DTASCDGO IN (SELECT b.DTASCDGO FROM CNT.BK_DTAS_NC_INTERM_OCT26 b);
-- COMMIT;

-- RESULTADO (2026-11-01, producción): corrido. 12118 → 2.3.90.10 debe 85,32 / 1.4.90.90.10 haber 85,32;
-- 12119 → 2.3.90.10 debe 1.108,26 / 1.4.90.15.02 haber 1.108,26. 3.2 vacío (cuadrados).
-- Pendiente: la salida del bloque 0.3 (NC 1, no intermediario) para decidir si se corrige.

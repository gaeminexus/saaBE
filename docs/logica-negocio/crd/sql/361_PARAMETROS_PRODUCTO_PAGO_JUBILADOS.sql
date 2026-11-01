-- =====================================================================================
-- 361 — SEMILLA de DOS filas. omen-saa-1, 2026-11-01. P9b (commit 785c80b6).
-- El producto de pago de la PENSIÓN y del SEGURO de jubilados deja de estar escrito (516/517) y de resolverse
-- por cuenta (la cuenta 2.3.01.10.03 tiene dos productos: 411 DEVPECO y 517 — crd/sql/360). Ahora son
-- parámetros del rubro 237 (CRD_PARAMETROS_CONTABILIDAD), por alterno, con el id de PGS.PRDP en PDTRVLRN:
--   detalle 2 = producto de la PENSIÓN → 517 (Pension Complementaria Jubilados)
--   detalle 3 = producto del SEGURO   → 516 (Seguros Por Pagar Jubilados ASOPREP a Petroecuador)
-- El código verifica que la cuenta del grupo del producto sea la de la plantilla PAGO_PENSION_COMPLEMENTARIA.
-- Son los MISMOS productos que usa hoy el código: el resultado no cambia.
-- ⛔ VA ANTES DEL EAR ÚNICO: sin ellos, todo pago de pensión o seguro de jubilados se detiene con
--    «falta el parámetro (rubro 237, detalle N)».
-- El rubro se busca por ALTERNO (PRBRALTR = 237); PDTRCDGO = MAX + 1 (sin secuencia), como c20-02.
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES
-- 0.1 El rubro 237 existe. ESPERADO: 1 fila.
SELECT r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR FROM SCP.PRBR r WHERE r.PRBRALTR = 237;
-- 0.2 Detalles actuales del rubro 237. (!) Si ya existen los alternos 2 o 3 con OTRO uso, PARAR y pasar la salida.
SELECT d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRN, d.PDTRVLRV, d.PDTRESTD
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO WHERE r.PRBRALTR = 237 ORDER BY d.PDTRALTR;
-- 0.3 Los productos 516 y 517 existen.
SELECT p.ID, p.CODIGO, p.NOMBRE FROM PGS.PRDP p WHERE p.ID IN (516, 517);


-- BLOQUE 1 — (ESCRIBE) El NOT EXISTS impide duplicarlos.
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
SELECT (SELECT NVL(MAX(PDTRCDGO), 0) + 1 FROM SCP.PDTR), r.PRBRCDGO,
       'PRODUCTO DE PAGO DE LA PENSION COMPLEMENTARIA DE JUBILADOS (PGS.PRDP)', 517, NULL, 2, 1
  FROM SCP.PRBR r
 WHERE r.PRBRALTR = 237
   AND NOT EXISTS (SELECT 1 FROM SCP.PDTR d WHERE d.PRBRCDGO = r.PRBRCDGO AND d.PDTRALTR = 2);
-- (!) "1 fila insertada".
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
SELECT (SELECT NVL(MAX(PDTRCDGO), 0) + 1 FROM SCP.PDTR), r.PRBRCDGO,
       'PRODUCTO DE PAGO DEL SEGURO DE JUBILADOS (PGS.PRDP)', 516, NULL, 3, 1
  FROM SCP.PRBR r
 WHERE r.PRBRALTR = 237
   AND NOT EXISTS (SELECT 1 FROM SCP.PDTR d WHERE d.PRBRCDGO = r.PRBRCDGO AND d.PDTRALTR = 3);
-- (!) "1 fila insertada".
COMMIT;


-- BLOQUE 2 — CONTROL DESPUÉS. ESPERADO: alterno 2 = 517 y alterno 3 = 516, activos.
SELECT d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRN, d.PDTRESTD
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 237 AND d.PDTRALTR IN (2, 3) ORDER BY d.PDTRALTR;


-- REVERSO (comentado):
-- DELETE FROM SCP.PDTR d WHERE d.PDTRALTR IN (2, 3)
--    AND d.PRBRCDGO = (SELECT r.PRBRCDGO FROM SCP.PRBR r WHERE r.PRBRALTR = 237);
-- COMMIT;

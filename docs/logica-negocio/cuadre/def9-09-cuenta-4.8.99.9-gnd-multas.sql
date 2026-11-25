-- def9-09 — D-180: cuenta de movimiento 4.8.99.9 GND MULTAS (hija de 4.8.99 GASTOS NO DEDUCIBLES).
-- El líder la creó en ASOPREP CUADRE el 24-nov-2026 con ese código, que se replica tal cual. Las hermanas usan dos dígitos
-- (4.8.99.08), pero se deja igual que en CUADRE para que las dos bases coincidan.
-- Primer uso: septiembre 2025, saldo deudor de 2.3.04.90.01 RETENCIONES EN LA FUENTE IMPUESTO A LA RENTA (1.234,24).
-- B17: renglón 489090 OTROS, con una fila en CNT.CRRP como el resto de las 4.8.99.xx (copia la de 4.8.99.08, D-053).
-- VA TAMBIÉN A PRODUCCIÓN (PASE-A-PRODUCCION.md §4.22). En producción, el bloque 02 no hace nada si la cuenta ya existe.
-- Copia la naturaleza, el tipo y el nivel de 4.8.99.08. Es idempotente.
-- SQL puro: correr bloque por bloque y confirmar con COMMIT si el control 04 da lo esperado.

-- 01 Antes: padre, hermana de referencia y la cuenta nueva (esperado en la base de trabajo: no existe)
SELECT PLNNCDGO, PLNNCNTA, PLNNNMBR, NTRLCDGO, PLNNTPOO, PLNNNVLL, PLNNCDPD, PLNNESTD FROM CNT.PLNN
WHERE PJRQCDGO = 1236 AND PLNNCNTA IN ('4.8.99', '4.8.99.08', '4.8.99.9') ORDER BY PLNNCNTA;
SELECT CRRPCDGO, RPRTCDGO, CRRPCNTA, CRRPCDRN, CRRPESTD FROM CNT.CRRP WHERE CRRPCNTA IN ('4.8.99.08', '4.8.99.9');

-- 02 Alta de la cuenta (no hace nada si ya existe)
INSERT INTO CNT.PLNN (PLNNCDGO, NTRLCDGO, PLNNCNTA, PLNNNMBR, PLNNTPOO, PLNNNVLL, PLNNCDPD, PLNNESTD, PLNNFCDS, PJRQCDGO, PLNNFCHA)
SELECT CNT.SQ_PLNNCDGO.NEXTVAL, H.NTRLCDGO, '4.8.99.9', 'GND MULTAS', H.PLNNTPOO, H.PLNNNVLL, P.PLNNCDGO, 1, NULL,
       P.PJRQCDGO, SYSDATE
FROM CNT.PLNN P
JOIN CNT.PLNN H ON H.PJRQCDGO = P.PJRQCDGO AND H.PLNNCNTA = '4.8.99.08'
WHERE P.PJRQCDGO = 1236 AND P.PLNNCNTA = '4.8.99'
  AND NOT EXISTS (SELECT 1 FROM CNT.PLNN X WHERE X.PJRQCDGO = 1236 AND X.PLNNCNTA = '4.8.99.9');

-- 03 B17: renglón 489090, igual que 4.8.99.08 (no hace nada si ya existe)
INSERT INTO CNT.CRRP (CRRPCDGO, RPRTCDGO, CRRPCNTA, CRRPALCN, CRRPCDRN, CRRPMDSG, CRRPRPRT, CRRPFCDS, CRRPFCHS, CRRPOBSR, CRRPESTD)
SELECT CNT.SQ_CRRPCDGO.NEXTVAL, h.RPRTCDGO, '4.8.99.9', h.CRRPALCN, '489090', h.CRRPMDSG, h.CRRPRPRT, h.CRRPFCDS, h.CRRPFCHS,
       'D-180: GND multas al renglon 489090 OTROS', 1
FROM CNT.CRRP h
WHERE h.CRRPCNTA = '4.8.99.08' AND NOT EXISTS (SELECT 1 FROM CNT.CRRP x WHERE x.CRRPCNTA = '4.8.99.9' AND x.RPRTCDGO = h.RPRTCDGO);

-- 04 Después. Esperado: la cuenta existe, con padre 4.8.99, tipo y nivel como 4.8.99.08, y activa; además, una fila CRRP
--    con 489090 por cada reporte que tenga 4.8.99.08.
SELECT C.PLNNCDGO, C.PLNNCNTA, C.PLNNNMBR, C.NTRLCDGO, C.PLNNTPOO, C.PLNNNVLL, P.PLNNCNTA PADRE, C.PLNNESTD
FROM CNT.PLNN C JOIN CNT.PLNN P ON P.PLNNCDGO = C.PLNNCDPD WHERE C.PJRQCDGO = 1236 AND C.PLNNCNTA = '4.8.99.9';
SELECT CRRPCDGO, RPRTCDGO, CRRPCNTA, CRRPCDRN, CRRPESTD FROM CNT.CRRP WHERE CRRPCNTA IN ('4.8.99.08', '4.8.99.9') ORDER BY CRRPCNTA;

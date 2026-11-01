-- =====================================================================================
-- c20-06 — SEMILLA de UNA línea. omen-saa-1, 2026-11-01. Plan cuentas a plantilla, P11.
-- Desde P11, la nota de débito de compra y la liquidación de compra con detalles SIN producto toman la
-- cuenta de gasto de la plantilla 38 «CXP COMPRAS - CUENTAS DE AJUSTE», aux1 2
-- (CxpLineaAsiento.GASTO_SIN_CLASIFICAR). Antes el código usaba el grupo de producto de MENOR código de la
-- empresa = 7 «Servicios Básicos» → 9784 4.4.01.05 SERVICIOS BÁSICOS (diagnóstico cxp/omen1-01 bloque 4).
-- Se siembra con ESA MISMA cuenta para que el resultado no cambie. Contabilidad la cambia cuando decida,
-- desde la pantalla de plantillas (plantilla 38, línea 2), sin tocar código.
-- ⛔ VA ANTES DEL EAR ÚNICO: sin esta línea, toda ND de compra y toda liquidación con detalle sin producto
--    se detiene con «falta la línea aux1 2 de la plantilla de compras (alterno 38)».
-- Requiere la plantilla 38 (crd/sql/356, ya corrido).
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES. ESPERADO: la plantilla 38 existe y no tiene aux1 2; la cuenta 9784 existe.
SELECT p.PLNSCDGO, p.PLNSNMBR,
       (SELECT COUNT(*) FROM CNT.DTPL d WHERE d.PLNSCDGO = p.PLNSCDGO AND d.DTPLAXL1 = 2) AS LINEAS_AUX1_2
  FROM CNT.PLNS p WHERE p.PLNSCDAL = 38 AND p.PJRQCDGO = 1236;
SELECT n.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR, n.PLNNESTD FROM CNT.PLNN n WHERE n.PLNNCDGO = 9784;


-- BLOQUE 1 — (ESCRIBE)
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLAXL1, DTPLAXL2, DTPLMVMN, DTPLDSCR, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, 9784, 2, 0, 1,
       'Gasto sin clasificar en compras (ND y liquidacion sin producto); cuenta a confirmar por contabilidad', 1
  FROM CNT.PLNS p
 WHERE p.PLNSCDAL = 38 AND p.PJRQCDGO = 1236
   AND NOT EXISTS (SELECT 1 FROM CNT.DTPL d WHERE d.PLNSCDGO = p.PLNSCDGO AND d.DTPLAXL1 = 2);
-- (!) "1 fila insertada".
COMMIT;


-- BLOQUE 2 — CONTROL DESPUÉS. ESPERADO: 38 / aux1 2 / 4.4.01.05.
SELECT p.PLNSCDAL, d.DTPLAXL1, d.DTPLAXL2, n.PLNNCNTA, n.PLNNNMBR, d.DTPLESTD
  FROM CNT.PLNS p JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PLNSCDAL = 38 AND p.PJRQCDGO = 1236 ORDER BY d.DTPLAXL1;


-- REVERSO (comentado):
-- DELETE FROM CNT.DTPL d WHERE d.DTPLAXL1 = 2
--    AND d.PLNSCDGO = (SELECT p.PLNSCDGO FROM CNT.PLNS p WHERE p.PLNSCDAL = 38 AND p.PJRQCDGO = 1236);
-- COMMIT;

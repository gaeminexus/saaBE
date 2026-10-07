-- =====================================================================================
-- e2-93 — Notas de venta de septiembre que salen con valor 0 en el ATS. SOLO LECTURA.
-- Equipo omen-saa-2 · 2026-10-07.
--
-- Caso: QUIÑONES 001-001-000004626, 4627 y 4628 (IDs 749, 739 y 744, 23-09-2026) salen en el ATS con todas
-- las bases en 0. Antes del arreglo, medir.
-- Cómo se forman las bases: el ATS NO recalcula, lee la cabecera de PGS.FCTC (SUBTOTAL, SUBCERO,
-- SUBNOOBJ, SUBEXENT, VIVA; gravada = SUBTOTAL - SUBCERO - SUBNOOBJ - SUBEXENT). La nota de venta se
-- registra a mano (FacturaCompraServiceImpl.registrarNotaVentaManual), que graba SUBTOTAL tal como lo manda
-- la pantalla y reparte SUBCERO = SUBTOTAL - no objeto - exento. Si SUBTOTAL llegó en 0 o vacío, todas las
-- bases quedan en 0 aunque el TOTAL y el detalle tengan valor.
-- Hipótesis: SUBTOTAL 0/nulo con TOTAL > 0 y suma del detalle > 0. Si el BLOQUE 1 muestra SUBTOTAL
-- correcto, la hipótesis es falsa: pasarme la salida.
-- Columnas copiadas de FacturaCompra (FCTC) y DetalleFacturaCompra (DFCC).
-- =====================================================================================

-- BLOQUE 1 — Las tres del caso, cabecera contra detalle.
SELECT 'BLOQUE 1 - caso' AS bloque, f.ID, f.NUMERO, f.TIPOCOMPROBANTE, TRUNC(f.FECHA) AS fecha, f.ESTADO,
       f.SUBTOTAL, f.SUBCERO, f.SUBNOOBJ, f.SUBEXENT, f.VIVA, f.TOTAL,
       (SELECT SUM(d.BASEIMPONIBLE) FROM PGS.DFCC d WHERE d.FACTURA = f.ID) AS suma_base_detalle,
       (SELECT SUM(d.TOTAL) FROM PGS.DFCC d WHERE d.FACTURA = f.ID) AS suma_total_detalle,
       (SELECT COUNT(*) FROM PGS.DFCC d WHERE d.FACTURA = f.ID) AS lineas
  FROM PGS.FCTC f
 WHERE f.ID IN (739, 744, 749)
 ORDER BY f.ID;

-- BLOQUE 2 — TODAS las compras activas desde agosto con TOTAL > 0 que el ATS declararía en 0 (SUBTOTAL
-- nulo o 0). Notas de venta y cualquier otro tipo: si aparecen facturas electrónicas, es otro problema.
SELECT 'BLOQUE 2 - bases en cero' AS bloque, f.ID, f.NUMERO, f.TIPOCOMPROBANTE, TRUNC(f.FECHA) AS fecha,
       NVL(t.TTLRRZSC, t.TTLRNMBR) AS proveedor, f.SUBTOTAL, f.SUBCERO, f.VIVA, f.TOTAL,
       (SELECT SUM(d.BASEIMPONIBLE) FROM PGS.DFCC d WHERE d.FACTURA = f.ID) AS suma_base_detalle,
       CASE WHEN f.CLAVE IS NULL THEN 'MANUAL' ELSE 'XML' END AS origen
  FROM PGS.FCTC f
  LEFT JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01'
   AND NVL(f.SUBTOTAL, 0) = 0
   AND NVL(f.TOTAL, 0) > 0
 ORDER BY f.FECHA, f.ID;

-- BLOQUE 3 — Para comparar: las notas de venta de septiembre que SÍ salen bien (SUBTOTAL > 0).
SELECT 'BLOQUE 3 - notas de venta correctas' AS bloque, f.ID, f.NUMERO, TRUNC(f.FECHA) AS fecha,
       f.SUBTOTAL, f.SUBCERO, f.VIVA, f.TOTAL
  FROM PGS.FCTC f
 WHERE f.ESTADO = 1
   AND f.TIPOCOMPROBANTE = '02'
   AND f.FECHA >= DATE '2026-09-01' AND f.FECHA < DATE '2026-10-01'
   AND NVL(f.SUBTOTAL, 0) > 0
 ORDER BY f.FECHA, f.ID;

-- =====================================================================================
-- e2-94 — Las 10 notas de venta de septiembre que salen en 0 en el ATS: SUBTOTAL y SUBCERO = suma del detalle.
-- ⚠️ EL BLOQUE 1 ESCRIBE. Equipo omen-saa-2 · 2026-10-07.
--
-- Causa (medida en el e2-93): en la pantalla de nota de venta manual el subtotal se rotula «Subtotal (grav.)»;
-- como una nota de venta no es gravada, se dejó en 0 y el valor se puso en «Subtotal (0%)». El backend
-- recalcula SUBCERO = SUBTOTAL - no objeto - exento y lo dejó en 0. El total y el detalle están bien en las 10:
--   GUERRERO 740 (4,50) · 742 (3,75) · 748 (5,00)        QUIÑONES 739 (10,00) · 744 (20,00) · 749 (15,00)
--   ARANA 737 (10,00) · 738 (8,25) · 743 (12,50) · 750 (14,00)                               TOTAL 103,00
-- Arreglo: SUBTOTAL = suma de la base del detalle y SUBCERO = SUBTOTAL (sin IVA, sin no objeto ni exento:
-- el mismo criterio de registrarNotaVentaManual y del e2-53). No toca el total, el detalle, el asiento ni los
-- pagos: el asiento se generó con el total, que ya estaba bien.
-- Guardas por nota, todo o nada: tipo 02, activa, SUBTOTAL 0, sin IVA, suma del detalle = TOTAL, y ninguna
-- línea con código de IVA gravado (solo 0, 7 o vacío).
-- DESPUÉS: regenerar el ATS de SEPTIEMBRE.
-- Columnas copiadas de FacturaCompra (FCTC) y DetalleFacturaCompra (DFCC).
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES. ESPERADO: 10 filas, SUBTOTAL y SUBCERO 0, suma_detalle = TOTAL, gravadas 0.
SELECT 'BLOQUE 0 - antes' AS bloque, f.ID, f.NUMERO, f.TIPOCOMPROBANTE, f.ESTADO, f.SUBTOTAL, f.SUBCERO,
       f.SUBNOOBJ, f.SUBEXENT, f.VIVA, f.TOTAL,
       (SELECT SUM(d.BASEIMPONIBLE) FROM PGS.DFCC d WHERE d.FACTURA = f.ID) AS suma_detalle,
       (SELECT COUNT(*) FROM PGS.DFCC d WHERE d.FACTURA = f.ID
           AND NVL(d.CODIGOIVASRI, 0) NOT IN (0, 7)) AS lineas_gravadas
  FROM PGS.FCTC f
 WHERE f.ID IN (737, 738, 739, 740, 742, 743, 744, 748, 749, 750)
 ORDER BY f.ID;

-- BLOQUE 1 — El arreglo. Primero se validan las 10; recién después se escribe.
DECLARE
    TYPE t_ids IS TABLE OF NUMBER;
    v_ids t_ids := t_ids(737, 738, 739, 740, 742, 743, 744, 748, 749, 750);
    v_tipo VARCHAR2(10); v_estado NUMBER; v_subtotal NUMBER; v_viva NUMBER; v_total NUMBER;
    v_suma NUMBER; v_gravadas NUMBER; v_filas NUMBER := 0;
BEGIN
    FOR i IN 1 .. v_ids.COUNT LOOP
        SELECT f.TIPOCOMPROBANTE, f.ESTADO, NVL(f.SUBTOTAL, 0), NVL(f.VIVA, 0), NVL(f.TOTAL, 0)
          INTO v_tipo, v_estado, v_subtotal, v_viva, v_total
          FROM PGS.FCTC f WHERE f.ID = v_ids(i);
        SELECT NVL(SUM(d.BASEIMPONIBLE), 0),
               COUNT(CASE WHEN NVL(d.CODIGOIVASRI, 0) NOT IN (0, 7) THEN 1 END)
          INTO v_suma, v_gravadas
          FROM PGS.DFCC d WHERE d.FACTURA = v_ids(i);
        IF v_tipo <> '02' OR v_estado <> 1 THEN
            RAISE_APPLICATION_ERROR(-20001, 'FCTC ' || v_ids(i) || ': tipo ' || v_tipo || ', estado ' || v_estado
                || '. Se esperaba una nota de venta (02) activa. No se toca nada.');
        END IF;
        IF v_subtotal <> 0 THEN
            RAISE_APPLICATION_ERROR(-20002, 'FCTC ' || v_ids(i) || ': SUBTOTAL ' || v_subtotal
                || ', ya no esta en 0 (¿ya corrio?). No se toca nada.');
        END IF;
        IF v_viva > 0.005 OR v_gravadas > 0 THEN
            RAISE_APPLICATION_ERROR(-20003, 'FCTC ' || v_ids(i) || ': tiene IVA (' || v_viva || ') o ' || v_gravadas
                || ' linea(s) gravada(s): no es todo base 0%. No se toca nada.');
        END IF;
        IF v_suma <= 0 OR ABS(v_suma - v_total) > 0.005 THEN
            RAISE_APPLICATION_ERROR(-20004, 'FCTC ' || v_ids(i) || ': suma del detalle ' || v_suma || ' y total '
                || v_total || ' no cuadran. No se toca nada.');
        END IF;
    END LOOP;

    FOR i IN 1 .. v_ids.COUNT LOOP
        UPDATE PGS.FCTC f
           SET f.SUBTOTAL = (SELECT ROUND(SUM(d.BASEIMPONIBLE), 2) FROM PGS.DFCC d WHERE d.FACTURA = f.ID),
               f.SUBCERO  = (SELECT ROUND(SUM(d.BASEIMPONIBLE), 2) FROM PGS.DFCC d WHERE d.FACTURA = f.ID),
               f.SUBNOOBJ = 0,
               f.SUBEXENT = 0
         WHERE f.ID = v_ids(i);
        v_filas := v_filas + SQL%ROWCOUNT;
    END LOOP;
    IF v_filas <> 10 THEN
        RAISE_APPLICATION_ERROR(-20005, 'Se actualizaron ' || v_filas || ' notas (se esperaban 10). Se deshace todo.');
    END IF;
END;
/

COMMIT;

-- BLOQUE 2 — CONTROL DESPUÉS.
-- 2.1 ESPERADO: 10 filas con SUBTOTAL = SUBCERO = TOTAL; suma 103,00.
SELECT 'BLOQUE 2.1 - despues' AS bloque, f.ID, f.NUMERO, f.SUBTOTAL, f.SUBCERO, f.TOTAL
  FROM PGS.FCTC f WHERE f.ID IN (737, 738, 739, 740, 742, 743, 744, 748, 749, 750) ORDER BY f.ID;
SELECT 'BLOQUE 2.2 - total' AS bloque, SUM(f.SUBCERO) AS base_cero, SUM(f.TOTAL) AS total
  FROM PGS.FCTC f WHERE f.ID IN (737, 738, 739, 740, 742, 743, 744, 748, 749, 750);
-- 2.3 ESPERADO: CERO filas. Ya no queda ninguna compra desde agosto con SUBTOTAL 0 y TOTAL > 0.
SELECT 'BLOQUE 2.3 - restantes' AS bloque, f.ID, f.NUMERO, f.SUBTOTAL, f.TOTAL
  FROM PGS.FCTC f
 WHERE f.ESTADO = 1 AND f.FECHA >= DATE '2026-08-01' AND NVL(f.SUBTOTAL, 0) = 0 AND NVL(f.TOTAL, 0) > 0;

-- REVERSO (comentado): devolver las 10 al estado medido en el BLOQUE 0.
-- UPDATE PGS.FCTC SET SUBTOTAL = 0, SUBCERO = 0 WHERE ID IN (737, 738, 739, 740, 742, 743, 744, 748, 749, 750);
-- COMMIT;

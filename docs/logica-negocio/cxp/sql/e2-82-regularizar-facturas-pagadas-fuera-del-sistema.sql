-- =====================================================================================
-- e2-82 — Regularizar seis facturas de compra que figuran con saldo en «Cuentas por Pagar» aunque ya
--          se pagaron, con la contabilidad hecha a mano en asientos de diario.
-- ⚠️ EL BLOQUE 1 ESCRIBE. Equipo omen-saa-2 · 2026-10-02 · Patrón: e2-70 (factura 203), que ya corrió.
--
-- Pedido del usuario, con el reporte de Cuentas por Pagar:
--   | ID  | Factura            | Proveedor           | Saldo    | Cómo se pagó                      |
--   | 106 | 004-904-000601282  | COMERCIAL KYWI      |     3,83 | caja chica · DIA-2026-07-0002      |
--   | 132 | 001-010-000192320  | ALMACEN EL FOCO     |    10,51 | caja chica · DIA-2026-07-0002      |
--   | 138 | 072-112-000279800  | CORPORACION FAVORITA|    36,42 | caja chica · DIA-2026-07-0002      |
--   | 178 | 072-108-000585367  | CORPORACION FAVORITA|    21,55 | caja chica · DIA-2026-07-0002      |
--   | 166 | 072-128-000348916  | CORPORACION FAVORITA|    15,27 | caja chica · DIA-2026-07-0002      |
--   | 150 | 001-001-000000717  | POLIT HERRERIA      | 2.250,00 | cruce de anticipo · DIA-2026-07-0036 |
--
-- POR QUÉ TIENEN SALDO: el saldo de una factura se calcula como total − aplicaciones (PGS.APLP). Estas se
-- pagaron en julio, antes de que la caja chica y el cruce de anticipos registraran la aplicación, y la
-- contabilidad se hizo con asientos de diario. El dinero ya salió y el asiento ya está: falta solo la
-- aplicación.
--
-- QUÉ HACE: una aplicación por factura, por su saldo exacto, SIN ASIENTO (APLPASNT nulo: el e2-70 ya
-- comprobó que la base lo admite), con fecha = la del asiento de diario, o la de la factura si es posterior (que la aplicación no quede
-- anterior a su propia factura). Así la factura queda en saldo 0
-- en el estado de cuenta, la cartera y los combos de pago. NO toca contabilidad.
-- ⛔ POLIT HERRERIA (150) NO SE TOCA en el BLOQUE 1: fue un cruce de ANTICIPO. Si ese anticipo existe en el
--    sistema, su saldo también hay que bajarlo, o quedaría «saldo a favor» falso. Lo decide el BLOQUE 0c.
--
-- Columnas copiadas de AplicacionPagoCxp, FacturaCompra, PagoProgramado, Asiento y AnticipoProveedor.
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES. ESPERADO: 6 filas, con los saldos de la tabla de arriba, estado 1, y
-- pagos_vigentes = 0 en todas. Si una factura tiene un pago vigente, NO se regulariza: avisar al árbitro.
SELECT 'BLOQUE 0 - facturas' AS bloque, f.ID, f.NUMERO, NVL(t.TTLRRZSC, t.TTLRNMBR) AS proveedor,
       f.TOTAL, f.ESTADO, f.FCTCEPAG,
       NVL((SELECT SUM(a.APLPMAPL) FROM PGS.APLP a WHERE a.APLPFCTC = f.ID AND a.APLPESTD = 1), 0) AS aplicado,
       f.TOTAL - NVL((SELECT SUM(a.APLPMAPL) FROM PGS.APLP a WHERE a.APLPFCTC = f.ID AND a.APLPESTD = 1), 0) AS saldo,
       (SELECT COUNT(*) FROM PGS.PGTR p WHERE p.PGTRFCTC = f.ID AND p.PGTRESTD NOT IN (4, 5)) AS pagos_vigentes
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ID IN (106, 132, 138, 166, 178, 150)
 ORDER BY f.ID;

-- BLOQUE 0b — Los dos asientos de diario. ESPERADO: 2 filas, con su fecha. Esa fecha va a ser la fecha
-- de la aplicación.
SELECT 'BLOQUE 0b - asientos de diario' AS bloque, s.ASNTCDGO, s.ASNTNMAL, s.ASNTFCHA, s.ASNTESTD
  FROM CNT.ASNT s
 WHERE s.ASNTNMAL IN ('DIA-2026-07-0002', 'DIA-2026-07-0036');

-- BLOQUE 0c — POLIT HERRERIA: ¿el anticipo cruzado existe en el sistema y todavía tiene saldo?
--   · CERO filas con saldo > 0 → el anticipo nunca se registró en el sistema (se pagó y contabilizó
--     fuera). Se regulariza la factura igual que las otras: avisar al árbitro para habilitar el BLOQUE 2.
--   · Una fila con saldo ≈ 2.250 → hay que bajar TAMBIÉN el saldo del anticipo y el PRCC. Avisar al
--     árbitro: el BLOQUE 2 cambia.
SELECT 'BLOQUE 0c - anticipos de POLIT' AS bloque,
       a.ANTPCDGO, a.ANTPESTD, a.ANTPFANT, a.ANTPVLOR, a.ANTPSALD, a.ANTPNDOC, a.ANTPOBSR
  FROM PGS.ANTP a
 WHERE a.ANTPTTLR = (SELECT f.TITULAR FROM PGS.FCTC f WHERE f.ID = 150)
 ORDER BY a.ANTPCDGO;

-- BLOQUE 1 — REGULARIZACIÓN DE LAS CINCO PAGADAS CON CAJA CHICA. Un solo bloque: si cualquier
-- factura no está como se midió, la excepción revierte TODO el bloque y no queda nada a medias.
DECLARE
    v_fecha DATE;

    PROCEDURE regulariza(p_id NUMBER, p_saldo NUMBER, p_fecha DATE) IS
        v_saldo   NUMBER;
        v_estado  NUMBER;
        v_vigente NUMBER;
    BEGIN
        SELECT f.ESTADO,
               f.TOTAL - NVL((SELECT SUM(a.APLPMAPL) FROM PGS.APLP a WHERE a.APLPFCTC = f.ID AND a.APLPESTD = 1), 0),
               (SELECT COUNT(*) FROM PGS.PGTR p WHERE p.PGTRFCTC = f.ID AND p.PGTRESTD NOT IN (4, 5))
          INTO v_estado, v_saldo, v_vigente
          FROM PGS.FCTC f WHERE f.ID = p_id;

        IF v_estado <> 1 THEN
            RAISE_APPLICATION_ERROR(-20001, 'Factura ' || p_id || ' no está activa (ESTADO=' || v_estado || '). No se toca nada.');
        END IF;
        IF ABS(v_saldo - p_saldo) > 0.001 THEN
            RAISE_APPLICATION_ERROR(-20002, 'Factura ' || p_id || ': saldo ' || v_saldo || ', se esperaba ' || p_saldo
                || '. Cambió desde el reporte: no se toca nada, avisar al árbitro.');
        END IF;
        IF v_vigente > 0 THEN
            RAISE_APPLICATION_ERROR(-20003, 'Factura ' || p_id || ' tiene ' || v_vigente
                || ' pago(s) vigente(s) en Tesorería: no se regulariza, avisar al árbitro.');
        END IF;

        INSERT INTO PGS.APLP (APLPCDGO, APLPPJRQ, APLPFCTC, APLPTDPG, APLPMAPL,
                              APLPFAPL, APLPESTD, APLPUSAR, APLPASNT, APLPOBSR, APLPFCRG)
        SELECT PGS.SQ_APLPCDGO.NEXTVAL, f.EMPRESA, f.ID, 1, p_saldo,
               GREATEST(p_fecha, TRUNC(f.FECHA)), 1, f.USUARIO, NULL,
               'Regularizacion de datos (e2-82, 2026-10-02): factura pagada con CAJA CHICA en julio, antes '
               || 'de que la caja chica registrara la aplicacion. Contabilidad ya hecha en el asiento de diario '
               || 'DIA-2026-07-0002. Este abono NO es un movimiento de dinero nuevo y NO genera asiento.',
               SYSTIMESTAMP
          FROM PGS.FCTC f WHERE f.ID = p_id;

        UPDATE PGS.FCTC SET FCTCEPAG = 3 WHERE ID = p_id;
    END;
BEGIN
    SELECT s.ASNTFCHA INTO v_fecha FROM CNT.ASNT s WHERE s.ASNTNMAL = 'DIA-2026-07-0002';

    regulariza(106,  3.83, v_fecha);   -- COMERCIAL KYWI
    regulariza(132, 10.51, v_fecha);   -- ALMACEN EL FOCO
    regulariza(138, 36.42, v_fecha);   -- CORPORACION FAVORITA
    regulariza(178, 21.55, v_fecha);   -- CORPORACION FAVORITA
    regulariza(166, 15.27, v_fecha);   -- CORPORACION FAVORITA
END;
/

COMMIT;

-- BLOQUE 1b — CONTROL DESPUÉS. ESPERADO: las cinco con saldo 0,00 y FCTCEPAG 3. POLIT (150) sigue en
-- 2.250,00, y está bien: espera al BLOQUE 2. Si las cinco siguen con saldo, el COMMIT no corrió.
SELECT 'BLOQUE 1b - despues' AS bloque, f.ID, f.NUMERO, f.FCTCEPAG,
       f.TOTAL - NVL((SELECT SUM(a.APLPMAPL) FROM PGS.APLP a WHERE a.APLPFCTC = f.ID AND a.APLPESTD = 1), 0) AS saldo
  FROM PGS.FCTC f
 WHERE f.ID IN (106, 132, 138, 166, 178, 150)
 ORDER BY f.ID;

-- =====================================================================================
-- BLOQUE 2 — POLIT HERRERIA (factura 150, 2.250,00). ⛔ NO USAR: reemplazado por el e2-85 (2026-10-02),
-- después de que el BLOQUE 0c mostró que el anticipo de julio no existe en el sistema.
-- Versión para el caso «el anticipo NO existe en el sistema» (cero filas con saldo en el 0c). Si el 0c
-- muestra un anticipo con saldo ≈ 2.250, NO usar esto: el árbitro escribe la otra versión.
-- =====================================================================================
-- DECLARE
--     v_fecha DATE;
--     v_saldo NUMBER;
-- BEGIN
--     SELECT s.ASNTFCHA INTO v_fecha FROM CNT.ASNT s WHERE s.ASNTNMAL = 'DIA-2026-07-0036';
--     SELECT f.TOTAL - NVL((SELECT SUM(a.APLPMAPL) FROM PGS.APLP a WHERE a.APLPFCTC = f.ID AND a.APLPESTD = 1), 0)
--       INTO v_saldo FROM PGS.FCTC f WHERE f.ID = 150;
--     IF ABS(v_saldo - 2250) > 0.001 THEN
--         RAISE_APPLICATION_ERROR(-20002, 'Factura 150: saldo ' || v_saldo || ', se esperaba 2250. No se toca nada.');
--     END IF;
--     INSERT INTO PGS.APLP (APLPCDGO, APLPPJRQ, APLPFCTC, APLPTDPG, APLPMAPL,
--                           APLPFAPL, APLPESTD, APLPUSAR, APLPASNT, APLPOBSR, APLPFCRG)
--     SELECT PGS.SQ_APLPCDGO.NEXTVAL, f.EMPRESA, f.ID, 1, 2250,
--            GREATEST(v_fecha, TRUNC(f.FECHA)), 1, f.USUARIO, NULL,
--            'Regularizacion de datos (e2-82, 2026-10-02): factura pagada por CRUCE DE ANTICIPO registrado '
--            || 'fuera del sistema. Contabilidad ya hecha en el asiento de diario DIA-2026-07-0036. Este abono '
--            || 'NO es un movimiento de dinero nuevo y NO genera asiento.',
--            SYSTIMESTAMP
--       FROM PGS.FCTC f WHERE f.ID = 150;
--     UPDATE PGS.FCTC SET FCTCEPAG = 3 WHERE ID = 150;
-- END;
-- /
-- COMMIT;

-- REVERSO (comentado): las aplicaciones de este script se identifican por la observación.
-- UPDATE PGS.APLP SET APLPESTD = 2 WHERE APLPOBSR LIKE 'Regularizacion de datos (e2-82%';
-- UPDATE PGS.FCTC SET FCTCEPAG = 1 WHERE ID IN (106, 132, 138, 166, 178, 150);
-- COMMIT;

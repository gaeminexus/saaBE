-- =====================================================================================
-- e2-110 — Reversar el abono que la retención recibida 1294 (MRFOX, 218,84) sigue haciendo a la factura de
--          VENTA 001-001-000000830, después de que el e2-108 (bloque 2) inactivó la retención y anuló el
--          documento de la bandeja.
-- Equipo omen-saa-2 · 2026-10-27. BLOQUE 1 SOLO LECTURA; BLOQUE 2 CORRIGE; BLOQUE 3 control después.
--
-- Por qué: el e2-108 apagó RCV2/DRC2 (fuera del ATS) y pasó DCXP 1294 a ANULADO (7), pero NO reversó la
-- aplicación CBR.APLC 52, que sigue ACTIVA y descuenta 218,84 del saldo de la factura 830 (BLOQUE 1d del
-- e2-108, corrido el 2026-10-27: APLC 52, tipo 3, 218,84, estado 1, asiento 11205, factura 277).
-- Con el DCXP ya en ANULADO, el botón «Anular» nuevo de la bandeja no la alcanza: por eso este script.
--
-- Hace lo mismo que AplicacionPagoCxcServiceImpl.revertirAplicacionesDeDocumentoSinAsiento (saaBE a4586614):
--   APLC 52 -> REVERSADO (2) + « | REVERSADA: motivo» en la observación, y recalcula FCTR.FCTREPAG de la factura
--   con la regla de calculaEstadoPago: saldo <= 0,01 -> 3 PAGADA_TOTAL; aplicado > 0 -> 2 PAGADA_PARCIAL; si no 1.
--   NO toca el asiento 11205 (ya debe estar anulado: lo verifica el BLOQUE 1).
-- Columnas copiadas de AplicacionPagoCxc (CBR.APLC: APLCCDGO, APLCFCTR, APLCTDPG, APLCRCV2, APLCRTCM, APLCMAPL,
-- APLCOBSR, APLCESTD, APLCASNT), Factura (CBR.FCTR: ID, NUMERO, TOTAL, FCTREPAG), Asiento (CNT.ASNT: ASNTCDGO,
-- ASNTESTD; 2 = ANULADO), DocumentoCxp (PGS.DCXP: DCXPCDGO, DCXPESTD, DCXPIDBD, DCXPTBTD).
-- Sin INSERT, sin secuencias.
-- =====================================================================================

-- BLOQUE 1 — CONTROL ANTES. ESPERADO, en una sola fila:
--   APLCESTD = 1, APLCMAPL = 218.84, APLCFCTR = 277, factura 001-001-000000830, total 1677.75,
--   aplicado_activo = 364.73, estado_asiento = 2 (ANULADO), dcxp_estado = 7, retencion_estado = 0.
-- Si algo difiere (sobre todo estado_asiento distinto de 2, o dcxp_estado distinto de 7): NO correr el BLOQUE 2.
SELECT 'BLOQUE 1' AS bloque, ap.APLCCDGO, ap.APLCTDPG, ap.APLCMAPL, ap.APLCESTD, ap.APLCFCTR,
       f.NUMERO AS factura_venta, f.TOTAL AS total_factura, f.FCTREPAG AS estado_pago,
       (SELECT NVL(SUM(x.APLCMAPL), 0) FROM CBR.APLC x WHERE x.APLCFCTR = f.ID AND x.APLCESTD = 1) AS aplicado_activo,
       a.ASNTCDGO, a.ASNTESTD AS estado_asiento,
       (SELECT d.DCXPESTD FROM PGS.DCXP d WHERE d.DCXPCDGO = 1294) AS dcxp_estado,
       (SELECT r.ESTADO FROM PGS.RCV2 r WHERE r.ID = ap.APLCRCV2) AS retencion_estado
  FROM CBR.APLC ap
  LEFT JOIN CBR.FCTR f ON f.ID = ap.APLCFCTR
  LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = ap.APLCASNT
 WHERE ap.APLCCDGO = 52;

-- =====================================================================================
-- BLOQUE 2 — CORRECCIÓN, todo o nada. Correr SOLO si el BLOQUE 1 salió como se espera.
-- =====================================================================================
DECLARE
    v_n NUMBER;
BEGIN
    UPDATE CBR.APLC ap
       SET ap.APLCESTD = 2,
           ap.APLCOBSR = SUBSTR(NVL(ap.APLCOBSR, '') || ' | REVERSADA: retencion 1294 anulada por el cliente ante el SRI '
                             || '(e2-110 2026-10-27)', 1, 2000)
     WHERE ap.APLCCDGO = 52
       AND ap.APLCESTD = 1
       AND ap.APLCMAPL = 218.84
       AND ap.APLCFCTR = 277
       AND EXISTS (SELECT 1 FROM CNT.ASNT a WHERE a.ASNTCDGO = ap.APLCASNT AND a.ASNTESTD = 2)
       AND EXISTS (SELECT 1 FROM PGS.DCXP d WHERE d.DCXPCDGO = 1294 AND d.DCXPESTD = 7);
    v_n := SQL%ROWCOUNT;
    IF v_n <> 1 THEN
        ROLLBACK;
        RAISE_APPLICATION_ERROR(-20001, 'APLC 52: se esperaba 1 fila y se actualizaron ' || v_n
            || '. No cambia nada; revisar el BLOQUE 1.');
    END IF;

    UPDATE CBR.FCTR f
       SET f.FCTREPAG = (
             SELECT CASE
                      WHEN f.TOTAL - NVL(SUM(x.APLCMAPL), 0) <= 0.01 THEN 3
                      WHEN NVL(SUM(x.APLCMAPL), 0) > 0               THEN 2
                      ELSE 1
                    END
               FROM CBR.APLC x
              WHERE x.APLCFCTR = f.ID AND x.APLCESTD = 1)
     WHERE f.ID = 277;
    IF SQL%ROWCOUNT <> 1 THEN
        ROLLBACK;
        RAISE_APPLICATION_ERROR(-20002, 'Factura 277: no se pudo recalcular el estado de pago. No cambia nada.');
    END IF;

    COMMIT;
END;
/

-- BLOQUE 3 — CONTROL DESPUÉS. ESPERADO: APLCESTD = 2; aplicado_activo = 145.89; saldo = 1531.86;
-- estado_pago = 2 (PAGADA_PARCIAL). El asiento 11205 sigue igual (2).
SELECT 'BLOQUE 3' AS bloque, ap.APLCCDGO, ap.APLCESTD, ap.APLCOBSR,
       f.NUMERO AS factura_venta, f.TOTAL AS total_factura, f.FCTREPAG AS estado_pago,
       (SELECT NVL(SUM(x.APLCMAPL), 0) FROM CBR.APLC x WHERE x.APLCFCTR = f.ID AND x.APLCESTD = 1) AS aplicado_activo,
       f.TOTAL - (SELECT NVL(SUM(x.APLCMAPL), 0) FROM CBR.APLC x WHERE x.APLCFCTR = f.ID AND x.APLCESTD = 1) AS saldo,
       (SELECT a.ASNTESTD FROM CNT.ASNT a WHERE a.ASNTCDGO = ap.APLCASNT) AS estado_asiento
  FROM CBR.APLC ap
  JOIN CBR.FCTR f ON f.ID = ap.APLCFCTR
 WHERE ap.APLCCDGO = 52;

-- REVERSO (comentado): vuelve a activar la aplicación 52 y recalcula la factura.
-- UPDATE CBR.APLC SET APLCESTD = 1,
--        APLCOBSR = REPLACE(APLCOBSR, ' | REVERSADA: retencion 1294 anulada por el cliente ante el SRI (e2-110 2026-10-27)', '')
--  WHERE APLCCDGO = 52 AND APLCESTD = 2;
-- (y repetir el UPDATE de CBR.FCTR del BLOQUE 2)
-- COMMIT;

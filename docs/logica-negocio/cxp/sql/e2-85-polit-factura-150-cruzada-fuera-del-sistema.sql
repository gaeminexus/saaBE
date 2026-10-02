-- =====================================================================================
-- e2-85 — POLIT HERRERIA: la factura 150 (001-001-000000717, saldo 2.250,00) se pagó en julio por un
--          cruce de anticipo hecho FUERA del sistema. ⚠️ EL BLOQUE 1 ESCRIBE.
-- Equipo omen-saa-2 · 2026-10-02 · Pedido: correo de Ana Lalangui del 2026-10-02, punto 3 («Al Dr. Polit a
-- la fecha no se le debe nada»), y el reporte de saldos del 2026-10-01.
--
-- Lo medido con el BLOQUE 0c del e2-82: POLIT tiene UN solo anticipo en el sistema, el #9, del 21/09/2026
-- (viáticos a Esmeraldas, 430,00, saldo 203,17). NO es el anticipo de julio con que se pagó esta factura:
-- ese cruce se hizo solo con el asiento de diario DIA-2026-07-0036, y el anticipo de julio nunca se
-- registró en PGS.ANTP. Por eso aquí basta con la aplicación que falta, igual que las cinco de caja
-- chica del e2-82: SIN asiento (la contabilidad ya está en DIA-2026-07-0036) y SIN tocar ningún anticipo.
--
-- ⛔ El anticipo #9 (203,17) NO se toca aquí: es otro tema (punto 3, segunda parte del correo), y su baja
--    depende de una decisión contable.
-- Es el BLOQUE 2 del e2-82, que quedó comentado hasta medir el 0c, aquí en su propio script.
-- Columnas copiadas de AplicacionPagoCxp, FacturaCompra, PagoProgramado y Asiento.
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES. ESPERADO: factura 150 activa, saldo 2.250,00 (total 2.875 − retención 625),
-- sin pagos vigentes; y el asiento DIA-2026-07-0036 existe.
SELECT 'BLOQUE 0 - factura' AS bloque, f.ID, f.NUMERO, f.TOTAL, f.ESTADO, f.FCTCEPAG,
       f.TOTAL - NVL((SELECT SUM(a.APLPMAPL) FROM PGS.APLP a WHERE a.APLPFCTC = f.ID AND a.APLPESTD = 1), 0) AS saldo,
       (SELECT COUNT(*) FROM PGS.PGTR p WHERE p.PGTRFCTC = f.ID AND p.PGTRESTD NOT IN (4, 5)) AS pagos_vigentes
  FROM PGS.FCTC f WHERE f.ID = 150;
SELECT 'BLOQUE 0 - asiento' AS bloque, s.ASNTCDGO, s.ASNTNMAL, s.ASNTFCHA, s.ASNTESTD
  FROM CNT.ASNT s WHERE s.ASNTNMAL = 'DIA-2026-07-0036';

-- BLOQUE 1 — REGULARIZACIÓN, con guardas.
DECLARE
    v_fecha   DATE;
    v_saldo   NUMBER;
    v_estado  NUMBER;
    v_vigente NUMBER;
BEGIN
    SELECT s.ASNTFCHA INTO v_fecha FROM CNT.ASNT s WHERE s.ASNTNMAL = 'DIA-2026-07-0036';

    SELECT f.ESTADO,
           f.TOTAL - NVL((SELECT SUM(a.APLPMAPL) FROM PGS.APLP a WHERE a.APLPFCTC = f.ID AND a.APLPESTD = 1), 0),
           (SELECT COUNT(*) FROM PGS.PGTR p WHERE p.PGTRFCTC = f.ID AND p.PGTRESTD NOT IN (4, 5))
      INTO v_estado, v_saldo, v_vigente
      FROM PGS.FCTC f WHERE f.ID = 150;

    IF v_estado <> 1 THEN
        RAISE_APPLICATION_ERROR(-20001, 'Factura 150 no está activa (ESTADO=' || v_estado || '). No se toca nada.');
    END IF;
    IF ABS(v_saldo - 2250) > 0.001 THEN
        RAISE_APPLICATION_ERROR(-20002, 'Factura 150: saldo ' || v_saldo || ', se esperaba 2250. No se toca nada, avisar al árbitro.');
    END IF;
    IF v_vigente > 0 THEN
        RAISE_APPLICATION_ERROR(-20003, 'Factura 150 tiene ' || v_vigente || ' pago(s) vigente(s) en Tesorería. No se toca nada.');
    END IF;

    INSERT INTO PGS.APLP (APLPCDGO, APLPPJRQ, APLPFCTC, APLPTDPG, APLPMAPL,
                          APLPFAPL, APLPESTD, APLPUSAR, APLPASNT, APLPOBSR, APLPFCRG)
    SELECT PGS.SQ_APLPCDGO.NEXTVAL, f.EMPRESA, f.ID, 1, 2250,
           GREATEST(v_fecha, TRUNC(f.FECHA)), 1, f.USUARIO, NULL,
           'Regularizacion de datos (e2-85, 2026-10-02): factura pagada en julio por CRUCE DE ANTICIPO hecho '
           || 'fuera del sistema (el anticipo de julio no esta en PGS.ANTP). Contabilidad ya hecha en el asiento '
           || 'de diario DIA-2026-07-0036. Este abono NO es un movimiento de dinero nuevo y NO genera asiento.',
           SYSTIMESTAMP
      FROM PGS.FCTC f WHERE f.ID = 150;

    UPDATE PGS.FCTC SET FCTCEPAG = 3 WHERE ID = 150;
END;
/

COMMIT;

-- BLOQUE 2 — CONTROL DESPUÉS. ESPERADO: saldo 0,00 y FCTCEPAG 3. En Cuentas por Pagar, POLIT ya no
-- aparece por esta factura (su anticipo #9 sigue mostrando 203,17 a favor: es el otro tema).
SELECT 'BLOQUE 2 - despues' AS bloque, f.ID, f.NUMERO, f.FCTCEPAG,
       f.TOTAL - NVL((SELECT SUM(a.APLPMAPL) FROM PGS.APLP a WHERE a.APLPFCTC = f.ID AND a.APLPESTD = 1), 0) AS saldo
  FROM PGS.FCTC f WHERE f.ID = 150;

-- REVERSO (comentado)
-- UPDATE PGS.APLP SET APLPESTD = 2 WHERE APLPOBSR LIKE 'Regularizacion de datos (e2-85%';
-- UPDATE PGS.FCTC SET FCTCEPAG = 1 WHERE ID = 150;
-- COMMIT;

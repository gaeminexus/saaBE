-- =====================================================================================
-- e2-81 — Cuadrar el saldo global de anticipos (TSR.PRCC tipo 2) de EXPOAUTOPARTS y JLCONTROL con la
--          suma de los saldos de sus anticipos. ⚠️ ESCRIBE DATOS. Equipo omen-saa-2, 2026-10-02.
--
-- Medido con el e2-80 (2026-10-02):
--   EXPOAUTOPARTS (1791242963001): PRCC 15,30 · anticipos 7 (8,64) + 10 (15,30) = 23,94. La historia de
--     los dos cuadra exacta (valor − cruzado = saldo): el PRCC se tocó por fuera, probablemente con
--     «Editar saldo inicial» de Titulares.
--   JLCONTROL (1391904274001): PRCC 0 · anticipo 6 (3,05), sin cruces. Patrón de
--     AnticipoProveedorServiceImpl.actualizarSaldoInicialPrcc, que se traga el error y no suma.
--
-- SOLO estos dos. Los otros cuatro del e2-80 (GAEMII 6.000 sin anticipos, VYM 13,15, VARGAS −320,
-- ALANIS −2,50) NO se tocan: pueden tener saldos de apertura o anticipos migrados, y hay que medirlos.
-- El UPDATE exige que el PRCC y la suma de anticipos sigan valiendo lo medido: si algo cambió desde el
-- e2-80, no toca nada y lo dice.
-- No genera asiento: el asiento de cada anticipo ya se hizo al confirmarlo. Se corrige solo el
-- auxiliar que la aplicación usa para validar cruces y devoluciones.
-- Nombres de columna copiados de PersonaCuentaContable, PersonaRol y AnticipoProveedor.
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES. ESPERADO: 2 filas: EXPOAUTOPARTS 15,30 / 23,94 y JLCONTROL 0 / 3,05.
SELECT 'BLOQUE 0 - antes' AS bloque, t.TTLRIDNT, c.PRCCCDGO, c.PRCCSLIN AS prcc_saldo,
       (SELECT NVL(SUM(a.ANTPSALD), 0) FROM PGS.ANTP a
         WHERE a.ANTPTTLR = r.PRSNCDGO AND a.ANTPPJRQ = c.PJRQCDGO AND a.ANTPESTD = 2 AND a.ANTPVLOR > 0)
         AS suma_anticipos
  FROM TSR.PRCC c
  JOIN TSR.PRRL r ON r.PRRLCDGO = c.PRRLCDGO
  JOIN TSR.TTLR t ON t.TTLRCDGO = r.PRSNCDGO
 WHERE t.TTLRIDNT IN ('1791242963001', '1391904274001') AND c.PRCCTPOO = 2 AND r.PRRLRZZA = 2
   AND c.PJRQCDGO = 1236;

-- BLOQUE 1 — CORRECCIÓN, con guardas. Cada proveedor se corrige solo si encuentra exactamente una
-- fila PRCC y los dos valores siguen siendo los medidos.
DECLARE
    PROCEDURE cuadrar(p_ruc VARCHAR2, p_prcc_esperado NUMBER, p_suma_esperada NUMBER) IS
        v_filas NUMBER;
        v_prcc  NUMBER;
        v_suma  NUMBER;
        v_id    NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_filas
          FROM TSR.PRCC c JOIN TSR.PRRL r ON r.PRRLCDGO = c.PRRLCDGO JOIN TSR.TTLR t ON t.TTLRCDGO = r.PRSNCDGO
         WHERE t.TTLRIDNT = p_ruc AND c.PRCCTPOO = 2 AND r.PRRLRZZA = 2 AND c.PJRQCDGO = 1236;
        IF v_filas <> 1 THEN
            RAISE_APPLICATION_ERROR(-20001, p_ruc || ': se esperaba 1 fila PRCC tipo 2 y hay ' || v_filas || '. No se toca nada.');
        END IF;

        SELECT c.PRCCCDGO, NVL(c.PRCCSLIN, 0),
               (SELECT NVL(SUM(a.ANTPSALD), 0) FROM PGS.ANTP a
                 WHERE a.ANTPTTLR = r.PRSNCDGO AND a.ANTPPJRQ = c.PJRQCDGO AND a.ANTPESTD = 2 AND a.ANTPVLOR > 0)
          INTO v_id, v_prcc, v_suma
          FROM TSR.PRCC c JOIN TSR.PRRL r ON r.PRRLCDGO = c.PRRLCDGO JOIN TSR.TTLR t ON t.TTLRCDGO = r.PRSNCDGO
         WHERE t.TTLRIDNT = p_ruc AND c.PRCCTPOO = 2 AND r.PRRLRZZA = 2 AND c.PJRQCDGO = 1236;

        IF ABS(v_prcc - p_prcc_esperado) > 0.001 OR ABS(v_suma - p_suma_esperada) > 0.001 THEN
            RAISE_APPLICATION_ERROR(-20002, p_ruc || ': PRCC=' || v_prcc || ' y suma=' || v_suma
                || ' ya no son los medidos (' || p_prcc_esperado || ' / ' || p_suma_esperada
                || '). Algo cambió desde el e2-80: no se toca nada, avisar al árbitro.');
        END IF;

        UPDATE TSR.PRCC SET PRCCSLIN = p_suma_esperada WHERE PRCCCDGO = v_id;
    END;
BEGIN
    cuadrar('1791242963001', 15.30, 23.94);   -- EXPOAUTOPARTS
    cuadrar('1391904274001', 0,     3.05);    -- JLCONTROL
END;
/

COMMIT;

-- BLOQUE 2 — CONTROL DESPUÉS. ESPERADO: 2 filas con prcc_saldo = suma_anticipos (23,94 y 3,05).
-- Si muestra los valores viejos, el COMMIT no se ejecutó.
SELECT 'BLOQUE 2 - despues' AS bloque, t.TTLRIDNT, c.PRCCCDGO, c.PRCCSLIN AS prcc_saldo,
       (SELECT NVL(SUM(a.ANTPSALD), 0) FROM PGS.ANTP a
         WHERE a.ANTPTTLR = r.PRSNCDGO AND a.ANTPPJRQ = c.PJRQCDGO AND a.ANTPESTD = 2 AND a.ANTPVLOR > 0)
         AS suma_anticipos
  FROM TSR.PRCC c
  JOIN TSR.PRRL r ON r.PRRLCDGO = c.PRRLCDGO
  JOIN TSR.TTLR t ON t.TTLRCDGO = r.PRSNCDGO
 WHERE t.TTLRIDNT IN ('1791242963001', '1391904274001') AND c.PRCCTPOO = 2 AND r.PRRLRZZA = 2
   AND c.PJRQCDGO = 1236;

-- REVERSO (comentado)
-- UPDATE TSR.PRCC SET PRCCSLIN = 15.30 WHERE PRCCCDGO = 323;   -- EXPOAUTOPARTS
-- UPDATE TSR.PRCC SET PRCCSLIN = 0 WHERE PRCCCDGO = <el PRCCCDGO de JLCONTROL del BLOQUE 0>;
-- COMMIT;

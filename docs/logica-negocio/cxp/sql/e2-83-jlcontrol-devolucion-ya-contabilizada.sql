-- =====================================================================================
-- e2-83 — JLCONTROL: la devolución del anticipo 6 (3,05) YA se contabilizó en agosto como un Ingreso
--          de tesorería. Falta solo bajar el saldo del anticipo. ⚠️ EL BLOQUE 1 ESCRIBE.
-- Equipo omen-saa-2 · 2026-10-02 · Pedido: correo de Ana Lalangui del 2026-10-02, punto 1.
--
-- Lo que mostró el correo: el asiento T-I-2026-08-0036 (14/08/2026, tipo T-INGRESOS, usuario ALALANGUI),
-- «Ingreso tesorería | Concepto: Devolucion por retencion aplicada en julio del 2026 proveedor SISTEMAS
-- CONTABLES JLCONTROL S.A. | Ref: 6710110 | Valor: $3.05»:
--   DEBE  1.1.02.05.65 BANCO PACIFICO CTA. AH. # 1048142735   3,05
--   HABER 1.9.01.10    ANTICIPOS A PROVEEDORES                 3,05
-- La contabilidad está correcta y el dinero ya entró. Pero el anticipo 6 sigue con saldo 3,05, y el
-- Estado de Cuenta lo muestra como «Saldo a favor (anticipos) 3,05».
--
-- QUÉ HACE: saldo del anticipo 6 → 0, con una nota en su observación que apunta al asiento, y el PRCC
-- tipo 2 del proveedor = la suma de los saldos de sus anticipos confirmados (queda en 0). NO genera
-- asiento, NO crea movimiento bancario: los dos ya existen desde el ingreso de agosto.
-- POR QUÉ NO se registra como «Devolución de Anticipo» (PGS.DVPR): quedaría enlazada al asiento del
-- INGRESO, y anular esa devolución anularía el asiento con el ingreso todavía activo.
-- Funciona igual se haya corrido o no el e2-81: el PRCC se recalcula desde los anticipos.
-- Columnas copiadas de AnticipoProveedor, PersonaCuentaContable, PersonaRol y Asiento.
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES.
-- ESPERADO: el asiento existe y está ACTIVO; el anticipo 6 está CONFIRMADO (2) con saldo 3,05.
SELECT 'BLOQUE 0 - asiento' AS bloque, s.ASNTCDGO, s.ASNTNMAL, s.ASNTFCHA, s.ASNTESTD
  FROM CNT.ASNT s WHERE s.ASNTNMAL = 'T-I-2026-08-0036';

SELECT 'BLOQUE 0 - anticipo y PRCC' AS bloque, a.ANTPCDGO, a.ANTPESTD, a.ANTPVLOR, a.ANTPSALD,
       (SELECT c.PRCCSLIN FROM TSR.PRCC c JOIN TSR.PRRL r ON r.PRRLCDGO = c.PRRLCDGO
         WHERE r.PRSNCDGO = a.ANTPTTLR AND c.PRCCTPOO = 2 AND r.PRRLRZZA = 2 AND c.PJRQCDGO = a.ANTPPJRQ) AS prcc_saldo
  FROM PGS.ANTP a
  JOIN TSR.TTLR t ON t.TTLRCDGO = a.ANTPTTLR
 WHERE a.ANTPCDGO = 6 AND t.TTLRIDNT = '1391904274001';

-- BLOQUE 1 — CORRECCIÓN. Un solo bloque: si algo no está como se midió, no toca nada.
DECLARE
    v_asientos NUMBER;
    v_saldo    NUMBER;
    v_estado   NUMBER;
    v_titular  NUMBER;
    v_empresa  NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_asientos FROM CNT.ASNT s WHERE s.ASNTNMAL = 'T-I-2026-08-0036';
    IF v_asientos <> 1 THEN
        RAISE_APPLICATION_ERROR(-20001, 'El asiento T-I-2026-08-0036 no existe o está repetido (' || v_asientos || '). No se toca nada.');
    END IF;

    SELECT a.ANTPSALD, a.ANTPESTD, a.ANTPTTLR, a.ANTPPJRQ INTO v_saldo, v_estado, v_titular, v_empresa
      FROM PGS.ANTP a JOIN TSR.TTLR t ON t.TTLRCDGO = a.ANTPTTLR
     WHERE a.ANTPCDGO = 6 AND t.TTLRIDNT = '1391904274001';
    IF v_estado <> 2 OR ABS(v_saldo - 3.05) > 0.001 THEN
        RAISE_APPLICATION_ERROR(-20002, 'Anticipo 6: estado ' || v_estado || ', saldo ' || v_saldo
            || '. Se esperaba CONFIRMADO con 3,05. No se toca nada, avisar al árbitro.');
    END IF;

    UPDATE PGS.ANTP
       SET ANTPSALD = 0,
           ANTPOBSR = SUBSTR(NVL(ANTPOBSR, '') || ' | DEVUELTO por el proveedor (e2-83, 2026-10-02): ingreso de '
                      || 'tesorería, asiento T-I-2026-08-0036 del 14/08/2026, ref. 6710110.', 1, 2000)
     WHERE ANTPCDGO = 6;

    UPDATE TSR.PRCC c
       SET c.PRCCSLIN = (SELECT NVL(SUM(a.ANTPSALD), 0) FROM PGS.ANTP a
                          WHERE a.ANTPTTLR = v_titular AND a.ANTPPJRQ = v_empresa AND a.ANTPESTD = 2 AND a.ANTPVLOR > 0)
     WHERE c.PRCCTPOO = 2 AND c.PJRQCDGO = v_empresa
       AND c.PRRLCDGO IN (SELECT r.PRRLCDGO FROM TSR.PRRL r WHERE r.PRSNCDGO = v_titular AND r.PRRLRZZA = 2);
END;
/

COMMIT;

-- BLOQUE 2 — CONTROL DESPUÉS. ESPERADO: anticipo 6 con saldo 0 y prcc_saldo 0.
-- En el Estado de Cuenta de JLCONTROL: «Saldo a favor (anticipos)» = 0,00.
SELECT 'BLOQUE 2 - despues' AS bloque, a.ANTPCDGO, a.ANTPSALD, a.ANTPOBSR,
       (SELECT c.PRCCSLIN FROM TSR.PRCC c JOIN TSR.PRRL r ON r.PRRLCDGO = c.PRRLCDGO
         WHERE r.PRSNCDGO = a.ANTPTTLR AND c.PRCCTPOO = 2 AND r.PRRLRZZA = 2 AND c.PJRQCDGO = a.ANTPPJRQ) AS prcc_saldo
  FROM PGS.ANTP a WHERE a.ANTPCDGO = 6;

-- REVERSO (comentado)
-- UPDATE PGS.ANTP SET ANTPSALD = 3.05 WHERE ANTPCDGO = 6;
-- (y volver a correr el UPDATE del PRCC del BLOQUE 1 con el mismo titular)
-- COMMIT;

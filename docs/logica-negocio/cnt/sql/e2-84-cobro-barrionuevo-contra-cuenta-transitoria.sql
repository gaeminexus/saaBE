-- =====================================================================================
-- e2-84 — El cobro de crédito de BARRIONUEVO DIAZ SILVIO LUIS (72,26) contabilizó el banco DOS veces:
--          el asiento de septiembre pasa a debitar la CUENTA TRANSITORIA, no el banco.
-- ⚠️ EL BLOQUE 1 ESCRIBE. Equipo omen-saa-2 · 2026-10-02 · Pedido: correo de Ana Lalangui del
-- 2026-10-02, punto 5. El usuario decidió que lo corrija este equipo, aunque el asiento lo generó crédito.
--
-- Los hechos, con las imágenes del correo:
--   · 03/08/2026: el partícipe transfiere 72,26 desde Banco Pichincha a la cuenta del Banco Pacífico
--     1048142735 (comprobante 900038003).
--   · 31/07/2026: contabilidad registra el depósito sin identificar, asiento ING-2026-07-0077:
--       DEBE  1.1.02.05.65  BANCO PACIFICO CTA. AH. # 1048142735   72,26
--       HABER 2.3.01.15.01  CUENTA TRANSITORIA                      72,26
--   · 08/09/2026: crédito registra el cobro de la cuota (Préstamo 6022, «Cobro crédito 80»), asiento
--     CRE-2026-09-0024, que DEBITA OTRA VEZ el Banco Pacífico por 72,26.
--   Resultado: el banco tiene 72,26 de más (una sola transferencia contada dos veces) y la cuenta
--   transitoria sigue con 72,26 sin cerrar.
--
-- LA CORRECCIÓN (lo que pidió contabilidad: «que el valor salga de la cuenta transitoria»): en el asiento
-- CRE-2026-09-0024, la línea DEBE 1.1.02.05.65 BANCO PACIFICO 72,26 pasa a DEBE 2.3.01.15.01 CUENTA
-- TRANSITORIA 72,26. El resto del asiento (el haber del cobro) NO cambia. Y el movimiento bancario de ese
-- asiento se ANULA: en la conciliación, el depósito del 03/08 ya está representado por ING-2026-07-0077.
-- Se corrige la línea en lugar de borrar y recrear el asiento: el cobro de crédito sigue enlazado al mismo
-- asiento, y su número no cambia.
--
-- ⚠️ Si crédito REVERSA algún día este cobro, el reverso anula este asiento tal como queda (con la
--    transitoria), y la transitoria vuelve a quedar con 72,26 a identificar. Eso es correcto.
-- Columnas copiadas de Asiento (ASNT), DetalleAsiento (DTAS), PlanCuenta (PLNN), Periodo (PRDO),
-- MovimientoBanco (MVCB), GrupoConciliacionAsiento (GCAS) y GrupoConciliacionContable (GRCC).
-- Estados: asiento 1 ACTIVO · período 2 MAYORIZADO, 4 CERRADO · MVCB 0 ANULADO.
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES (solo lectura)

-- 0.1 Los dos asientos. ESPERADO: 2 filas ACTIVAS (ASNTESTD 1). Anotar el estado del período de
--     CRE-2026-09-0024: si es 2 (MAYORIZADO) o 4 (CERRADO), el BLOQUE 1 se detiene. Hay que
--     desmayorizar septiembre primero, y volver a mayorizarlo después.
SELECT 'BLOQUE 0.1 - asientos' AS bloque, s.ASNTCDGO, s.ASNTNMAL, s.ASNTFCHA, s.ASNTESTD, s.PJRQCDGO AS empresa,
       p.PRDOCDGO, p.PRDOESTD AS estado_periodo
  FROM CNT.ASNT s LEFT JOIN CNT.PRDO p ON p.PRDOCDGO = s.PRDOCDGO
 WHERE s.ASNTNMAL IN ('CRE-2026-09-0024', 'ING-2026-07-0077');

-- 0.2 Las líneas de CRE-2026-09-0024. ESPERADO: una línea DEBE 72,26 en la cuenta 1.1.02.05.65, y el
--     resto, el haber del cobro.
SELECT 'BLOQUE 0.2 - lineas CRE' AS bloque, d.DTASCDGO, d.DTASCNTA, d.DTASNMCT, d.DTASDBEE, d.DTASHBRR, d.DTASDSCR,
       (SELECT COUNT(*) FROM TSR.GCAS g JOIN TSR.GRCC gr ON gr.GRCCCDGO = g.GRCCCDGO
         WHERE g.DTASCDGO = d.DTASCDGO AND gr.GRCCESTD = 1) AS en_grupo_conciliacion
  FROM CNT.DTAS d
 WHERE d.ASNTCDGO = (SELECT s.ASNTCDGO FROM CNT.ASNT s WHERE s.ASNTNMAL = 'CRE-2026-09-0024')
 ORDER BY d.DTASCDGO;

-- 0.3 El movimiento bancario de CRE-2026-09-0024. ESPERADO: una fila activa (MVCBESTD 1), sin conciliar
--     (MVCBCNCL distinto de 1). Si está conciliado, el BLOQUE 1 se detiene: desconciliar primero.
SELECT 'BLOQUE 0.3 - movimiento bancario' AS bloque, m.MVCBCDGO, m.MVCBVLRR, m.MVCBCNCL, m.MVCBESTD, m.MVCBDSCR
  FROM TSR.MVCB m
 WHERE m.ASNTCDGO = (SELECT s.ASNTCDGO FROM CNT.ASNT s WHERE s.ASNTNMAL = 'CRE-2026-09-0024');

-- 0.4 La cuenta transitoria de esa empresa. ESPERADO: exactamente 1 fila.
SELECT 'BLOQUE 0.4 - cuenta transitoria' AS bloque, c.PLNNCDGO, c.PLNNCNTA, c.PLNNNMBR, c.PJRQCDGO
  FROM CNT.PLNN c
 WHERE c.PLNNCNTA = '2.3.01.15.01'
   AND c.PJRQCDGO = (SELECT s.PJRQCDGO FROM CNT.ASNT s WHERE s.ASNTNMAL = 'CRE-2026-09-0024');

-- BLOQUE 1 — CORRECCIÓN, en un solo bloque. Si cualquier condición no se cumple, no toca nada.
DECLARE
    v_asiento   NUMBER;
    v_empresa   NUMBER;
    v_est_asnt  NUMBER;
    v_est_prdo  NUMBER;
    v_linea     NUMBER;
    v_lineas    NUMBER;
    v_transit   NUMBER;
    v_transits  NUMBER;
    v_conc      NUMBER;
BEGIN
    SELECT s.ASNTCDGO, s.PJRQCDGO, s.ASNTESTD, p.PRDOESTD
      INTO v_asiento, v_empresa, v_est_asnt, v_est_prdo
      FROM CNT.ASNT s LEFT JOIN CNT.PRDO p ON p.PRDOCDGO = s.PRDOCDGO
     WHERE s.ASNTNMAL = 'CRE-2026-09-0024';

    IF v_est_asnt <> 1 THEN
        RAISE_APPLICATION_ERROR(-20001, 'CRE-2026-09-0024 no está ACTIVO (estado ' || v_est_asnt || '). No se toca nada.');
    END IF;
    IF v_est_prdo IN (2, 4) THEN
        RAISE_APPLICATION_ERROR(-20002, 'El período de CRE-2026-09-0024 está MAYORIZADO o CERRADO (estado ' || v_est_prdo
            || '). Desmayorizar septiembre primero. No se toca nada.');
    END IF;

    SELECT COUNT(*) INTO v_lineas FROM CNT.DTAS d
     WHERE d.ASNTCDGO = v_asiento AND d.DTASCNTA = '1.1.02.05.65' AND ABS(NVL(d.DTASDBEE, 0) - 72.26) < 0.001;
    IF v_lineas <> 1 THEN
        RAISE_APPLICATION_ERROR(-20003, 'Se esperaba UNA línea DEBE 72,26 en 1.1.02.05.65 y hay ' || v_lineas || '. No se toca nada.');
    END IF;
    SELECT d.DTASCDGO INTO v_linea FROM CNT.DTAS d
     WHERE d.ASNTCDGO = v_asiento AND d.DTASCNTA = '1.1.02.05.65' AND ABS(NVL(d.DTASDBEE, 0) - 72.26) < 0.001;

    SELECT COUNT(*) INTO v_conc FROM TSR.GCAS g JOIN TSR.GRCC gr ON gr.GRCCCDGO = g.GRCCCDGO
     WHERE g.DTASCDGO = v_linea AND gr.GRCCESTD = 1;
    IF v_conc > 0 THEN
        RAISE_APPLICATION_ERROR(-20004, 'La línea del banco ya está en un grupo de conciliación activo: desconciliar primero. No se toca nada.');
    END IF;
    SELECT COUNT(*) INTO v_conc FROM TSR.MVCB m WHERE m.ASNTCDGO = v_asiento AND NVL(m.MVCBCNCL, 0) = 1;
    IF v_conc > 0 THEN
        RAISE_APPLICATION_ERROR(-20005, 'El movimiento bancario del asiento ya está conciliado: desconciliar primero. No se toca nada.');
    END IF;

    SELECT COUNT(*) INTO v_transits FROM CNT.PLNN c WHERE c.PLNNCNTA = '2.3.01.15.01' AND c.PJRQCDGO = v_empresa;
    IF v_transits <> 1 THEN
        RAISE_APPLICATION_ERROR(-20006, 'Se esperaba UNA cuenta 2.3.01.15.01 para la empresa y hay ' || v_transits || '. No se toca nada.');
    END IF;
    SELECT c.PLNNCDGO INTO v_transit FROM CNT.PLNN c WHERE c.PLNNCNTA = '2.3.01.15.01' AND c.PJRQCDGO = v_empresa;

    -- La línea del banco pasa a la cuenta transitoria. Mismo valor, mismo lado (DEBE).
    UPDATE CNT.DTAS
       SET PLNNCDGO = v_transit,
           DTASCNTA = '2.3.01.15.01',
           DTASNMCT = 'CUENTA TRANSITORIA',
           DTASDSCR = SUBSTR('Cierre de transitoria: deposito del 03/08/2026 ya contabilizado en ING-2026-07-0077 '
                      || '(e2-84). Antes: ' || NVL(DTASDSCR, ''), 1, 200)
     WHERE DTASCDGO = v_linea;

    -- El movimiento bancario de este asiento se anula: el depósito real ya está en ING-2026-07-0077.
    UPDATE TSR.MVCB SET MVCBESTD = 0 WHERE ASNTCDGO = v_asiento AND NVL(MVCBESTD, 1) = 1;
END;
/

COMMIT;

-- BLOQUE 2 — CONTROL DESPUÉS.
-- ESPERADO: la línea que era del banco ahora dice 2.3.01.15.01 CUENTA TRANSITORIA, con DEBE 72,26; el
-- asiento sigue cuadrado (suma debe = suma haber); el movimiento bancario queda con MVCBESTD 0.
-- En el mayor de la 2.3.01.15.01: el haber de julio (ING-2026-07-0077) y el debe de septiembre se cancelan.
-- Si el período de septiembre estaba mayorizado y se desmayorizó para correr esto, volver a mayorizarlo.
SELECT 'BLOQUE 2 - lineas' AS bloque, d.DTASCDGO, d.DTASCNTA, d.DTASNMCT, d.DTASDBEE, d.DTASHBRR
  FROM CNT.DTAS d
 WHERE d.ASNTCDGO = (SELECT s.ASNTCDGO FROM CNT.ASNT s WHERE s.ASNTNMAL = 'CRE-2026-09-0024')
 ORDER BY d.DTASCDGO;
SELECT 'BLOQUE 2 - cuadre' AS bloque, SUM(NVL(d.DTASDBEE, 0)) AS debe, SUM(NVL(d.DTASHBRR, 0)) AS haber
  FROM CNT.DTAS d
 WHERE d.ASNTCDGO = (SELECT s.ASNTCDGO FROM CNT.ASNT s WHERE s.ASNTNMAL = 'CRE-2026-09-0024');
SELECT 'BLOQUE 2 - movimiento' AS bloque, m.MVCBCDGO, m.MVCBVLRR, m.MVCBESTD
  FROM TSR.MVCB m
 WHERE m.ASNTCDGO = (SELECT s.ASNTCDGO FROM CNT.ASNT s WHERE s.ASNTNMAL = 'CRE-2026-09-0024');

-- REVERSO (comentado): volver la línea al banco y reactivar el movimiento.
-- UPDATE CNT.DTAS SET PLNNCDGO = (SELECT c.PLNNCDGO FROM CNT.PLNN c WHERE c.PLNNCNTA = '1.1.02.05.65'
--                                  AND c.PJRQCDGO = <empresa del BLOQUE 0.1>),
--                     DTASCNTA = '1.1.02.05.65', DTASNMCT = 'BANCO PACIFICO CTA. AH. # 1048142735'
--  WHERE DTASCDGO = <DTASCDGO de la línea, del BLOQUE 0.2>;
-- UPDATE TSR.MVCB SET MVCBESTD = 1 WHERE ASNTCDGO = <ASNTCDGO de CRE-2026-09-0024>;
-- COMMIT;

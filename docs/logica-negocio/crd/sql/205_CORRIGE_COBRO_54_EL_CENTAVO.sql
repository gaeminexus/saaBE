-- =====================================================================================
-- CORRIGE EL COBRO 54 — el centavo que impide procesarlo
-- FECHA: 2026-09-07   EQUIPO: omen-saa-1 (omen1)   SCRIPT: 205 (rango 200-249)
--
-- ESCRIBE. Dos UPDATE sobre CRD.CBCR y CRD.DCBC del cobro 54. Reverso al final, comentado.
--
-- MEDIDO CON sql/204, NO DEDUCIDO:
--   - La cuota 660140 (#18 del prestamo 4524) debe 171,85 y es internamente coherente:
--     DTPRTTLL = 171,85 = suma de sus 6 componentes. DIF = 0. La cuota NO se toca.
--   - El cobro 54 dice 171,86 en cabecera y en su unico detalle (DCBCCDGO = 57).
--   - El centavo esta en el COBRO, no en la cuota.
--   - Radio de impacto: un solo prestamo. Los cobros 38, 46 y 49 son el MISMO pago
--     intentado antes y ANULADO (estado 5). El unico vivo es el 54 (estado 2, APROBADO).
--   - Los reintentos NO dejaron basura: 0 pagos grabados, CBCRASRP y CBCRASN2 en NULL.
--     IncomeException es @ApplicationException(rollback = true) y cumple.
--
-- QUE HACE ESTA CORRECCION Y QUE NO
--   Baja el cobro de 171,86 a 171,85, que es lo que la cuota puede absorber. Con eso el
--   Debe del asiento definitivo (que sale de DCBC) coincide con el Haber (que sale de los
--   PGPR grabados) y el proceso pasa.
--
--   ⚠️ EL CENTAVO NO DESAPARECE, Y ESO ESTA BIEN. El asiento transitorio del cobro
--   (CBCRASN1 = 8572) ya acredito lo depositado. Al aplicar 171,85 queda 0,01 vivo en la
--   cuenta transitoria: es dinero recibido y no aplicado, que es exactamente lo que paso.
--   NO es un descuadre — no confundirlo con el incidente del 2026-08-31, donde la
--   transitoria acumulaba porque el asiento 2 nunca corria.
--
--   ⛔ ESTO NO ARREGLA LA CAUSA. La causa son dos cosas de codigo, ninguna de este script:
--     1. MotorPagoPrestamoServiceImpl:286  while (valorRestante > TOLERANCIA), con
--        TOLERANCIA = 0.01 (linea 54). Con 43 cuotas pendientes por delante, 0.01 > 0.01
--        da falso y el centavo queda huerfano. Habia donde ponerlo.
--     2. CobroCreditoServiceImpl:1799  el Debe del asiento definitivo sale de DCBC y el
--        Haber de los PGPR. Cualquier excedente del motor descuadra el asiento por ese
--        monto exacto. Volvera a pasar con el proximo cobro que no calce al centavo.
--
-- ⛔ ANTES DE CORRER: leer el BLOQUE 0. Si el asiento 8572 NO es por 171,86, PARAR y
--    avisar al arbitro — significaria que el deposito fue por otro monto y esta
--    correccion estaria escondiendo otra cosa.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 240
SET FEEDBACK ON


-- =====================================================================================
-- BLOQUE 0 — CONTROL ANTES
-- =====================================================================================

-- 0.1 El cobro y su detalle. Esperado: 171,86 / 171,86 / estado 2 / un solo detalle.
SELECT c.CBCRCDGO AS COBRO, c.CBCRESTD AS ESTADO, c.CBCRVLRR AS VALOR_CABECERA,
       c.CBCRFCHA AS FECHA, c.CBCRRFRN AS REFERENCIA,
       c.CBCRASN1 AS ASNT_TRANSITORIO, c.CBCRASRP AS ASNT_REPARTO, c.CBCRASN2 AS ASNT_DEFINITIVO,
       d.DCBCCDGO AS DETALLE, d.PRSTCDGO AS PRESTAMO, d.DCBCVLRR AS VALOR_DETALLE
  FROM CRD.CBCR c
  JOIN CRD.DCBC d ON d.CBCRCDGO = c.CBCRCDGO
 WHERE c.CBCRCDGO = 54;

-- 0.2 El asiento transitorio ya grabado. ⛔ Si el total NO es 171,86 -> PARAR.
SELECT a.ASNTCDGO AS ASIENTO, a.ASNTNMAL AS NUMERO_ALTERNO, a.ASNTFCHA AS FECHA,
       l.DTASCNTA AS CUENTA, l.DTASNMCT AS NOMBRE, l.DTASDSCR AS DESCRIPCION,
       l.DTASDBEE AS DEBE, l.DTASHBRR AS HABER
  FROM CNT.ASNT a
  JOIN CNT.DTAS l ON l.ASNTCDGO = a.ASNTCDGO
 WHERE a.ASNTCDGO = 8572
 ORDER BY l.DTASCDGO;

-- 0.3 Los tres intentos anulados del mismo pago, para dejarlos registrados.
SELECT c.CBCRCDGO AS COBRO, c.CBCRESTD AS ESTADO, c.CBCRVLRR AS VALOR,
       c.CBCRFCHA AS FECHA, c.CBCRMTAN AS MOTIVO_ANULACION
  FROM CRD.CBCR c
 WHERE c.CBCRCDGO IN (38, 46, 49)
 ORDER BY c.CBCRCDGO;


-- =====================================================================================
-- BLOQUE 1 — LA CORRECCION
-- Los dos UPDATE llevan el valor actual en el WHERE: si algo cambio desde el 204,
-- afectan 0 filas en vez de escribir sobre un estado que no es el medido.
-- Esperado: 1 fila cada uno.
-- =====================================================================================

UPDATE CRD.CBCR
   SET CBCRVLRR = 171.85
 WHERE CBCRCDGO = 54
   AND CBCRVLRR = 171.86
   AND CBCRESTD = 2;

UPDATE CRD.DCBC
   SET DCBCVLRR = 171.85
 WHERE DCBCCDGO = 57
   AND CBCRCDGO = 54
   AND DCBCVLRR = 171.86;


-- =====================================================================================
-- BLOQUE 2 — CONTROL DESPUES (mirar ANTES de confirmar)
-- Esperado: 171,85 en las dos columnas, estado 2, DIF = 0,00 contra la cuota.
-- Si no da eso: ROLLBACK y avisar.
-- =====================================================================================

SELECT c.CBCRCDGO AS COBRO, c.CBCRESTD AS ESTADO, c.CBCRVLRR AS VALOR_CABECERA,
       d.DCBCVLRR AS VALOR_DETALLE, t.DTPRTTLL AS DEBE_LA_CUOTA,
       ROUND(d.DCBCVLRR - t.DTPRTTLL, 2) AS DIF
  FROM CRD.CBCR c
  JOIN CRD.DCBC d ON d.CBCRCDGO = c.CBCRCDGO
  JOIN CRD.DTPR t ON t.DTPRCDGO = 660140
 WHERE c.CBCRCDGO = 54;


-- =====================================================================================
-- BLOQUE 3 — CONFIRMAR
-- Descomentar el COMMIT recien despues de leer el BLOQUE 2.
-- =====================================================================================

-- COMMIT;


-- =====================================================================================
-- BLOQUE 4 — REVERSO (comentado). Deja el cobro exactamente como estaba.
-- Solo sirve ANTES del COMMIT (ahi alcanza con ROLLBACK) o para deshacerlo despues.
-- =====================================================================================

-- UPDATE CRD.CBCR SET CBCRVLRR = 171.86 WHERE CBCRCDGO = 54 AND CBCRVLRR = 171.85;
-- UPDATE CRD.DCBC SET DCBCVLRR = 171.86 WHERE DCBCCDGO = 57 AND DCBCVLRR = 171.85;
-- COMMIT;


-- =====================================================================================
-- DESPUES DE CONFIRMAR
--   1. Procesar el cobro 54 desde la pantalla. Deberia pasar sin descuadre.
--   2. El 0,01 queda vivo en la transitoria contra el asiento 8572. Es correcto y esta
--      explicado arriba, pero hay que saberlo: no es un descuadre, es dinero recibido
--      y no aplicado.
--   3. La causa de codigo sigue abierta. Volvera a pasar.
-- =====================================================================================

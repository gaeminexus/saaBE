-- ============================================================================
-- VERIFICACION (SOLO LECTURA) -- pagos de cuota aplicados por /prst/pagarCuota
-- o /prst/pagarMultiplesCuotas que quedaron SIN asiento contable.
-- U-5b #18 (decision del arbitro, DISENO-USUARIO-OBLIGATORIO.md §12 (a)).
--
-- Contexto: esos dos endpoints se cerraron (responden 410). Antes aplicaban el pago
-- de la cuota y, si la contabilizacion devolvia null, el EVPR y los PGPR quedaban
-- sin asiento (aplicarAsiento() retorna sin hacer nada cuando numeroAsiento es null).
-- Este script SOLO LISTA; no corrige nada. NO ejecuta INSERT/UPDATE/DELETE.
-- Lo corre el usuario.
-- ============================================================================

-- 1) Eventos PAGO_MANUAL sin asiento (EVPRNMAS guarda la PK del asiento, no el correlativo).
SELECT e.EVPRCDGO   AS evento,
       e.EVPRFCHA   AS fecha_evento,
       e.EVPRVLRR   AS valor,
       e.EVPRUSAR   AS usuario,
       e.EVPRESTD   AS estado_evento,
       e.EVPRNMAS   AS asiento_evento
  FROM CRD.EVPR e
 WHERE e.EVPRTPOO = 'PAGO_MANUAL'
   AND e.EVPRNMAS IS NULL
 ORDER BY e.EVPRFCHA;

-- 2) Pagos (PGPR) de esos eventos que quedaron sin asiento, con su prestamo y cuota.
--    PGPRANUL se muestra (no se filtra) para que se vea si el pago ya fue anulado.
SELECT p.PGPRCDGO   AS pago,
       p.EVPRCDGO   AS evento,
       p.PRSTCDGO   AS prestamo,
       p.DTPRCDGO   AS cuota,
       p.PGPRFCHA   AS fecha_pago,
       p.PGPRVLRR   AS valor,
       p.PGPRUSRG   AS usuario_registro,
       p.PGPRANUL   AS anulado,
       p.PGPRASNT   AS asiento_pago
  FROM CRD.PGPR p
  JOIN CRD.EVPR e ON e.EVPRCDGO = p.EVPRCDGO
 WHERE e.EVPRTPOO = 'PAGO_MANUAL'
   AND p.PGPRASNT IS NULL
 ORDER BY p.PRSTCDGO, p.DTPRCDGO, p.PGPRFCHA;

-- 3) Resumen por prestamo: cuanto se aplico sin asiento (para decidir la correccion).
SELECT p.PRSTCDGO   AS prestamo,
       COUNT(*)     AS pagos_sin_asiento,
       SUM(p.PGPRVLRR) AS valor_sin_asiento
  FROM CRD.PGPR p
  JOIN CRD.EVPR e ON e.EVPRCDGO = p.EVPRCDGO
 WHERE e.EVPRTPOO = 'PAGO_MANUAL'
   AND p.PGPRASNT IS NULL
 GROUP BY p.PRSTCDGO
 ORDER BY valor_sin_asiento DESC;

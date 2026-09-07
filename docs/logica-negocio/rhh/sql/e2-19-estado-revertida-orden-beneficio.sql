-- =====================================================================
-- e2-19 — Estado REVERTIDA (5) para la orden de beneficio social
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- ⚠️ NO ES SOLO LECTURA. Inserta una fila. Correr el BLOQUE 0 primero.
--
-- ⚠️ REESCRITO el 2026-09-07, por los mismos errores que el e2-18 y el e2-20:
--    nombres escritos de memoria en vez de sacados de las entidades.
--    Lo real (DetalleRubro.java / Rubro.java):
--      SCP.PDTR: PDTRCDGO · PRBRCDGO(FK) · PDTRDSCR · PDTRVLRN · PDTRVLRV · PDTRALTR · PDTRESTD
--      SCP.PRBR: PRBRCDGO · PRBRDSCR · PRBRFCHA · PRBRALTR · PRBRTPOO
--    O sea PDTRDSCR y PRBRDSCR, no *NMBR.
--    Y el rubro se identifica por CODIGO ALTERNO (PRBRALTR), no por la PK: se
--    midio que el rubro con PK 199 es "ESTADO DEL DESCUENTO RECURRENTE" mientras
--    el de tipo de cuenta tiene PK 200 y alterno 199. Filtrar por PK devuelve
--    el catalogo equivocado.
--
-- POR QUE EXISTE
--   Una orden de beneficio social PAGADA era un callejon sin salida: `anular`
--   rechaza las pagadas y NADA sacaba la orden de PAGADA. El contrato prometia
--   "revierta el pago en tesoreria y despues anule", y ese camino no existia —
--   revertir en tesoreria ni siquiera toca RRHH, porque el pago va sin
--   desglose. La orden quedaba PAGADA para siempre, con la provision dada de
--   baja contra un pago que ya no existe.
--
--   POST /rest/odbs/revertirPago/{id} deja la orden en REVERTIDA, y desde ahi
--   se puede anular (que libera las LQBS para volver a generar la orden).
--
-- QUE INSERTA
--   Una fila en SCP.PDTR: el detalle con alterno 5 del rubro 310
--   (RhhEstadoOrdenBeneficio, el que creo el e2-03). Los alternos 1 a 4 ya
--   existen: GENERADA, ENVIADA_A_TESORERIA, PAGADA, ANULADA.
--
-- ORDEN: va ANTES del WAR.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 — CONTROL PREVIO. Correr esto solo, LEER, y recien despues seguir.
-- =====================================================================

-- 0.1 El rubro 310 por ALTERNO, con su PK al lado para que se vea si difieren.
--     ESPERADO: una fila, con descripcion de estado de orden de beneficio.
--     Si no aparece, PARAR: el e2-03 no lo creo o lo creo con otro alterno.
SELECT 'BLOQUE 0.1 - rubro 310' AS control,
       r.PRBRCDGO AS rubro_pk, r.PRBRALTR AS rubro_alterno, r.PRBRDSCR AS descripcion
  FROM SCP.PRBR r WHERE r.PRBRALTR = 310;

-- 0.2 Los detalles que ya tiene. ESPERADO: alternos 1, 2, 3 y 4.
SELECT 'BLOQUE 0.2 - estados actuales' AS control,
       d.PDTRALTR AS alterno, d.PDTRDSCR AS descripcion, d.PDTRESTD AS estado
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 310
 ORDER BY d.PDTRALTR;

-- 0.3 ¿Ya existe el alterno 5? Si devuelve filas, NO seguir.
SELECT 'BLOQUE 0.3 - conflicto' AS control, d.PDTRALTR, d.PDTRDSCR
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 310 AND d.PDTRALTR = 5;

-- 0.4 ¿Hay ordenes PAGADA hoy? Son las que el endpoint nuevo podra revertir.
--     Informativo: dimensiona a que le estamos abriendo la puerta.
SELECT 'BLOQUE 0.4 - ordenes pagadas hoy' AS control, COUNT(*) AS cuantas
  FROM RHH.ODBS WHERE ODBSESTD = 3;


-- =====================================================================
-- BLOQUE 1 — El detalle nuevo
-- =====================================================================
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRALTR, PDTRDSCR, PDTRESTD)
SELECT SCP.SQ_PDTRCDGO.NEXTVAL, r.PRBRCDGO, 5, 'REVERTIDA', 1
  FROM SCP.PRBR r
 WHERE r.PRBRALTR = 310
   AND NOT EXISTS (SELECT 1 FROM SCP.PDTR d
                    WHERE d.PRBRCDGO = r.PRBRCDGO AND d.PDTRALTR = 5);

COMMIT;


-- =====================================================================
-- BLOQUE 2 — CONTROL DE SECUENCIA (regla 8 del equipo)
-- =====================================================================
-- El INSERT usa NEXTVAL, asi que la secuencia avanza sola: esto es control.
-- ESPERADO: secuencia POR ENCIMA del maximo de la tabla.
SELECT 'BLOQUE 2 - secuencia PDTR' AS control,
       (SELECT MAX(PDTRCDGO) FROM SCP.PDTR)                              AS max_tabla,
       (SELECT last_number FROM all_sequences
         WHERE sequence_owner = 'SCP' AND sequence_name = 'SQ_PDTRCDGO') AS secuencia
  FROM DUAL;


-- =====================================================================
-- BLOQUE 3 — CONTROL POSTERIOR
-- =====================================================================
-- ESPERADO: cinco filas, alternos 1 a 5, con el 5 = REVERTIDA.
SELECT 'BLOQUE 3 - estados finales' AS control,
       d.PDTRALTR AS alterno, d.PDTRDSCR AS descripcion, d.PDTRESTD AS estado
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 310
 ORDER BY d.PDTRALTR;


-- =====================================================================
-- BLOQUE 4 — REVERSO. COMENTADO A PROPOSITO.
-- =====================================================================
-- Control antes de borrar (esto SI es lectura). Tiene que dar 0:
--   SELECT COUNT(*) FROM RHH.ODBS WHERE ODBSESTD = 5;
--
-- DELETE FROM SCP.PDTR
--  WHERE PDTRALTR = 5
--    AND PRBRCDGO IN (SELECT PRBRCDGO FROM SCP.PRBR WHERE PRBRALTR = 310);
-- COMMIT;

-- =====================================================================
-- FIN
-- =====================================================================

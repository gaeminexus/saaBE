-- =====================================================================
-- e2-19 — Estado REVERTIDA (5) para la orden de beneficio social
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- ⚠️ NO ES SOLO LECTURA. Inserta una fila.
--
-- POR QUE EXISTE
--   Hasta hoy una orden de beneficio social PAGADA era un callejon sin salida:
--   `anular` rechaza las pagadas, y NADA sacaba la orden de PAGADA — los unicos
--   setEstado del servicio son GENERADA, ENVIADA_A_TESORERIA, PAGADA y ANULADA.
--   El contrato §1.6 prometia "revierta el pago en tesoreria y despues anule", y
--   ese camino no existia: se revertia en tesoreria y la orden quedaba PAGADA
--   para siempre, con la provision dada de baja contra un pago que ya no existe.
--
--   Y desde que confirmarPago crea la novedad del decimo en el rol, el costo
--   subio: el rol del mes quedaba informando un pago revertido.
--
--   El endpoint nuevo POST /rest/odbs/revertirPago/{id} deja la orden en
--   REVERTIDA, y desde ahi se puede anular (que libera las LQBS para volver a
--   generar la orden).
--
-- QUE INSERTA
--   Una fila en SCP.PDTR: el detalle con alterno 5 del rubro
--   RHH_ESTADO_ORDEN_BENEFICIO. Los alternos 1 a 4 ya existen
--   (GENERADA, ENVIADA_A_TESORERIA, PAGADA, ANULADA).
--
--   Sin esta fila, la constante Java queda sin su detalle de rubro — que es
--   exactamente lo que costo el e2-08 y el e2-13.
--
-- ORDEN: va ANTES del WAR.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 — CONTROL PREVIO. Correr esto solo, LEER, y recien despues seguir.
-- =====================================================================

-- 0.1 ¿Cual es el codigo del rubro RHH_ESTADO_ORDEN_BENEFICIO?
--     El script de abajo lo resuelve por NOMBRE, no por un numero inventado.
--     ESPERADO: una sola fila.
SELECT 'BLOQUE 0.1 - el rubro' AS control, PRBRCDGO, PRBRNMBR
  FROM SCP.PRBR
 WHERE UPPER(PRBRNMBR) LIKE '%ESTADO%ORDEN%BENEFICIO%';

-- 0.2 Los detalles que ya tiene. ESPERADO: alternos 1, 2, 3 y 4.
SELECT 'BLOQUE 0.2 - detalles actuales' AS control, d.PDTRCDGO, d.PDTRALTR, d.PDTRNMBR
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE UPPER(r.PRBRNMBR) LIKE '%ESTADO%ORDEN%BENEFICIO%'
 ORDER BY d.PDTRALTR;

-- 0.3 ¿Ya existe el alterno 5? Si devuelve filas, NO seguir: ya esta hecho,
--     o alguien uso ese alterno para otra cosa.
SELECT 'BLOQUE 0.3 - conflicto' AS control, d.PDTRCDGO, d.PDTRALTR, d.PDTRNMBR
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE UPPER(r.PRBRNMBR) LIKE '%ESTADO%ORDEN%BENEFICIO%' AND d.PDTRALTR = 5;

-- 0.4 ¿Hay ordenes PAGADA hoy? Son las que el endpoint nuevo podra revertir.
--     Es informativo: dimensiona a que le estamos abriendo la puerta.
SELECT 'BLOQUE 0.4 - ordenes pagadas hoy' AS control, COUNT(*) AS cuantas
  FROM RHH.ODBS WHERE ODBSESTD = 3;


-- =====================================================================
-- BLOQUE 1 — El detalle nuevo
-- =====================================================================
-- Se resuelve el rubro por nombre a proposito: escribir el numero de memoria es
-- como se inventa un dato que se podia verificar (§9, §28 del estado).

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRALTR, PDTRNMBR, PDTRESTD)
SELECT SCP.SQ_PDTRCDGO.NEXTVAL, r.PRBRCDGO, 5, 'REVERTIDA', 1
  FROM SCP.PRBR r
 WHERE UPPER(r.PRBRNMBR) LIKE '%ESTADO%ORDEN%BENEFICIO%'
   AND NOT EXISTS (SELECT 1 FROM SCP.PDTR d
                    WHERE d.PRBRCDGO = r.PRBRCDGO AND d.PDTRALTR = 5);

COMMIT;


-- =====================================================================
-- BLOQUE 2 — CONTROL DE SECUENCIA
-- =====================================================================
-- Regla 8 del esquema de este equipo, la que ya tuvo su ejemplar con
-- SQ_BEXTCDGO en 95 contra una tabla en 389 (§30.11 y §33 del estado).
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

SELECT 'BLOQUE 3 - estados finales' AS control, d.PDTRALTR, d.PDTRNMBR, d.PDTRESTD
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE UPPER(r.PRBRNMBR) LIKE '%ESTADO%ORDEN%BENEFICIO%'
 ORDER BY d.PDTRALTR;


-- =====================================================================
-- BLOQUE 4 — REVERSO. COMENTADO A PROPOSITO.
-- =====================================================================
-- Descomentar SOLO si hay que deshacer, y unicamente si NINGUNA orden quedo ya
-- en estado 5. Control antes de borrar (esto SI es lectura):
--
--   SELECT COUNT(*) FROM RHH.ODBS WHERE ODBSESTD = 5;   -- tiene que dar 0
--
-- DELETE FROM SCP.PDTR
--  WHERE PDTRALTR = 5
--    AND PRBRCDGO IN (SELECT PRBRCDGO FROM SCP.PRBR
--                      WHERE UPPER(PRBRNMBR) LIKE '%ESTADO%ORDEN%BENEFICIO%');
-- COMMIT;

-- =====================================================================
-- FIN
-- =====================================================================

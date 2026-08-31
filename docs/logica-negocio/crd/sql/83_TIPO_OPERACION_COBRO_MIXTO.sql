-- =====================================================================================
-- RUBRO 245 — nuevo tipo de operación de cobro: COBRO_MIXTO
-- FECHA: 2026-08-30
--
-- POR QUE
-- Un depósito puede repartirse entre APORTES y VARIOS PRESTAMOS a la vez — es lo que hace
-- hoy la pantalla de cobros personales. En el modelo actual eso no cabe: un CobroCredito
-- tiene UN solo tipoOperacion, PAGO_MULTIPLE asume que todas las lineas son prestamos y
-- REGISTRO_APORTE rechaza cualquier linea con idPrestamo.
--
-- Sin este tipo, ese depósito habría que partirlo en DOS cobros. Y eso reproduce
-- exactamente el defecto que el usuario reportó el 2026-08-30: una sola operación suya
-- convertida en varias piezas del backend, con dos aprobaciones para un solo depósito y un
-- reverso que deja la mitad aplicada.
--
-- REGLA DE DISEÑO: UN DEPOSITO = UN COBRO = UNA APROBACION = UN REVERSO.
--
-- Se eligió un tipo NUEVO en vez de relajar PAGO_MULTIPLE para no cambiarle el significado
-- a un valor que ya está en uso: todo lo que hoy lee PAGO_MULTIPLE puede seguir asumiendo
-- que sus lineas son prestamos.
--
-- PDTRCDGO 1179 sale del colchón 1179-1199 que REGISTRO-RESERVAS-EQUIPOS.md dejó libre a
-- propósito — NO del rango del equipo 4, para que no se pisen.
--
-- Del lado Java hay que agregar la constante en
-- src/main/java/com/saa/rubros/CrdTipoOperacionCobro.java — eso lo hace el agente de
-- backend, no este script.
-- =====================================================================================


-- =====================================================================================
-- 1. CONTROLES PREVIOS
-- =====================================================================================

-- 1.1 El rubro 245 y sus detalles actuales. Esperado: 6 filas, alternos 1..6
--     (PAGO_CUOTA, PAGO_MULTIPLE, ABONO_CAPITAL, PRECANCELACION, REGISTRO_APORTE,
--      ACUERDO_CONDONACION).
SELECT  r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR,
        d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRV, d.PDTRESTD
FROM    SCP.PRBR r
LEFT    JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
WHERE   r.PRBRALTR = 245
ORDER   BY d.PDTRALTR;

-- 1.2 El alterno 7 NO debe existir todavía. Esperado: 0 filas.
SELECT  d.PDTRCDGO, d.PDTRDSCR
FROM    SCP.PDTR d
JOIN    SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
WHERE   r.PRBRALTR = 245 AND d.PDTRALTR = 7;

-- 1.3 El PDTRCDGO 1179 debe estar libre. Esperado: 0 filas.
--     Si está ocupado, usar el siguiente libre DEL COLCHON (1179-1199) y anotarlo en
--     REGISTRO-RESERVAS-EQUIPOS.md.
SELECT PDTRCDGO FROM SCP.PDTR WHERE PDTRCDGO = 1179;


-- =====================================================================================
-- 2. EL DETALLE NUEVO
-- =====================================================================================

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
SELECT 1179, r.PRBRCDGO, 'COBRO MIXTO', 7, 'COBRO_MIXTO', 7, 1
FROM   SCP.PRBR r
WHERE  r.PRBRALTR = 245
AND    NOT EXISTS (
         SELECT 1 FROM SCP.PDTR x
         WHERE  x.PRBRCDGO = r.PRBRCDGO AND x.PDTRALTR = 7
       );

COMMIT;

-- Esperado: 1 fila la primera vez, 0 en la segunda.


-- =====================================================================================
-- 3. SINCRONIZAR LA SECUENCIA
-- =====================================================================================
-- Correr la consulta y ejecutar el ALTER SOLO si la secuencia quedó en 1179 o por debajo.

SELECT  s.SEQUENCE_NAME, s.LAST_NUMBER AS SIGUIENTE, 1179 AS PK_USADO
FROM    ALL_SEQUENCES s
WHERE   s.SEQUENCE_OWNER = 'SCP' AND s.SEQUENCE_NAME = 'SQ_PDTRCDGO';

-- ALTER SEQUENCE SCP.SQ_PDTRCDGO RESTART START WITH 1180;


-- =====================================================================================
-- 4. CONTROL POSTERIOR
-- =====================================================================================
-- Esperado: 7 filas, la 7 con descripción COBRO MIXTO y valor COBRO_MIXTO, estado 1.

SELECT  d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRV, d.PDTRESTD
FROM    SCP.PDTR d
JOIN    SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
WHERE   r.PRBRALTR = 245
ORDER   BY d.PDTRALTR;

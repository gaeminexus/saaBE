-- =====================================================================================
-- RUBRO 235 — nuevo tipo de movimiento de aporte: JUBILACION
-- FECHA: 2026-08-30
--
-- POR QUE
-- Al jubilar a un participe se generan movimientos de aporte NEGATIVOS en cesantia y
-- jubilacion, y uno POSITIVO en PENSION COMPLEMENTARIA (tipo de aporte 23). Esos
-- movimientos necesitan un tipo propio.
--
-- ⚠️ SIN ESTA FILA, el traslado de la jubilacion queda marcado como AJUSTE_MANUAL(2) —
-- indistinguible de una correccion hecha a mano. Justo en la operacion que mas hace falta
-- poder auditar despues: la que mueve el saldo entero de las cuentas de un participe.
--
-- El rubro 235 YA EXISTE (CRD_TIPO_MOVIMIENTO_APORTE). Esto agrega SOLO un detalle.
-- Valores actuales: 1 APORTE_MENSUAL, 2 AJUSTE_MANUAL, 3 DEVOLUCION, 4 PAGO_PRESTAMO,
-- 5 REVERSO, 6 MIGRADO. El nuevo es el 7.
--
-- Del lado Java hay que agregar la constante en
-- src/main/java/com/saa/rubros/CrdTipoMovimientoAporte.java — eso lo hace el agente de
-- backend, no este script.
-- =====================================================================================


-- =====================================================================================
-- 1. CONTROLES PREVIOS — mirar la salida antes de seguir
-- =====================================================================================

-- 1.1 El rubro 235 existe y trae sus 6 detalles. Esperado: 6 filas, alternos 1..6.
SELECT  r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR,
        d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRN, d.PDTRVLRV, d.PDTRESTD
FROM    SCP.PRBR r
LEFT    JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
WHERE   r.PRBRALTR = 235
ORDER   BY d.PDTRALTR;

-- 1.2 El alterno 7 NO debe existir todavia en ese rubro. Esperado: 0 filas.
SELECT  d.PDTRCDGO, d.PDTRDSCR
FROM    SCP.PDTR d
JOIN    SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
WHERE   r.PRBRALTR = 235 AND d.PDTRALTR = 7;

-- 1.3 Siguiente PDTRCDGO libre. Se espera 1178 o mas (1169-1177 los ocupo el DDL de
--     acuerdos de condonacion). Si el MAX es distinto, USAR EL VALOR REAL en el bloque 2.
SELECT MAX(PDTRCDGO) AS MAX_PDTR FROM SCP.PDTR;

-- 1.4 Confirmar que PENSION COMPLEMENTARIA es el tipo de aporte 23, como indico el usuario.
--     No lo usa este script, pero el proceso de jubilacion depende de ello.
SELECT t.TPAPCDGO, t.TPAPNMBR, t.TPAPIDST
FROM   CRD.TPAP t
WHERE  t.TPAPCDGO = 23;


-- =====================================================================================
-- 2. EL DETALLE NUEVO — solo despues de confirmar el bloque 1
-- =====================================================================================
-- Idempotente por el NOT EXISTS. Si el 1178 estuviera ocupado, cambiar el literal por el
-- MAX real + 1 del control 1.3.

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
SELECT 1178, r.PRBRCDGO, 'JUBILACION', 7, 'JUBILACION', 7, 1
FROM   SCP.PRBR r
WHERE  r.PRBRALTR = 235
AND    NOT EXISTS (
         SELECT 1 FROM SCP.PDTR x
         WHERE  x.PRBRCDGO = r.PRBRCDGO AND x.PDTRALTR = 7
       );

COMMIT;

-- Esperado: 1 fila la primera vez, 0 en la segunda.


-- =====================================================================================
-- 3. SINCRONIZAR LA SECUENCIA — el paso que se olvida
-- =====================================================================================
-- Correr PRIMERO la consulta. Ejecutar el ALTER SOLO si la secuencia quedo en 1178 o por
-- debajo. Si ya esta adelantada, NO TOCARLA.
--
-- Si queda por debajo, el proximo rubro creado DESDE LA APLICACION recibe un PDTRCDGO ya
-- usado y el INSERT muere por PK duplicada — en una pantalla que no tiene nada que ver.

SELECT  s.SEQUENCE_NAME, s.LAST_NUMBER AS SIGUIENTE, 1178 AS PK_USADO
FROM    ALL_SEQUENCES s
WHERE   s.SEQUENCE_OWNER = 'SCP' AND s.SEQUENCE_NAME = 'SQ_PDTRCDGO';

-- ALTER SEQUENCE SCP.SQ_PDTRCDGO RESTART START WITH 1179;


-- =====================================================================================
-- 4. CONTROL POSTERIOR
-- =====================================================================================
-- Esperado: 7 filas, alternos 1..7, la 7 con descripcion JUBILACION y estado 1.

SELECT  d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRN, d.PDTRVLRV, d.PDTRESTD
FROM    SCP.PDTR d
JOIN    SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
WHERE   r.PRBRALTR = 235
ORDER   BY d.PDTRALTR;

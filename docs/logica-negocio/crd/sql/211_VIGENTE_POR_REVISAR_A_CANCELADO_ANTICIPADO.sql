-- =====================================================================================
-- PASA LOS PRESTAMOS "VIGENTE POR REVISAR" A "CANCELADO ANTICIPADO"
-- y sus cuotas no pagadas a "CANCELADA ANTICIPADA"
--
-- FECHA: 2026-09-07   EQUIPO: omen-saa-1 (omen1)   SCRIPT: 211 (rango 200-249)
--
-- ⛔⛔ ESCRIBE, EN MASA, SOBRE DEUDA DE SOCIOS. LEER TODO EL ENCABEZADO ANTES DE CORRER.
--
-- =====================================================================================
-- LO QUE HACE, EN EL ORDEN QUE PIDIO EL USUARIO (y el orden IMPORTA)
--   1. Respalda los estados previos en dos tablas auxiliares (bloque 1).
--   2. Cuotas NO PAGADAS de los prestamos en VIGENTE_POR_REVISAR -> CANCELADA_ANTICIPADA.
--   3. Recien despues, esos prestamos -> CANCELADO_ANTICIPADO.
--
--   ⚠️ EL ORDEN NO ES PREFERENCIA, ES OBLIGATORIO: las cuotas se seleccionan por el estado
--      del prestamo. Si se moviera el prestamo primero, el paso de las cuotas no encontraria
--      ninguna y quedarian PENDIENTES colgando de un prestamo cancelado.
--
-- =====================================================================================
-- ⛔ LO MAS IMPORTANTE, Y NO ES TECNICO: ESTO NO GENERA NINGUN ASIENTO CONTABLE
--
--   Un UPDATE de SQL cambia la CARTERA y no toca la CONTABILIDAD. Si estos prestamos tienen
--   saldo pendiente, el mayor los va a seguir mostrando como cuentas por cobrar mientras la
--   cartera dice que estan cancelados. Las dos fuentes quedan desalineadas y **nada avisa**.
--
--   Si estos prestamos deben ademas darse de baja contablemente, eso es otro trabajo: no lo
--   hace este script y no se puede improvisar con un UPDATE.
--
-- ⛔ Y TAMPOCO: no crea PagoPrestamo, no toca saldos (DTPRSLDO), no mueve aportes, no
--    registra evento ni huella. Solo cambia estados.
--
-- =====================================================================================
-- LOS CODIGOS, LEIDOS DEL CATALOGO Y NO DE MEMORIA
--   com.saa.rubros.EstadoPrestamo:       VIGENTE_POR_REVISAR = 10 · CANCELADO_ANTICIPADO = 4
--   com.saa.rubros.EstadoCuotaPrestamo:  PAGADA = 4 · CANCELADA_ANTICIPADA = 7
--
-- LAS DOS TRAMPAS DE COLUMNA (documentadas en el CLAUDE.md del proyecto)
--   - PRESTAMO: el estado operativo es **PRSTIDST**, NO `ESPSCDGO`. ESPSCDGO es la FK al
--     catalogo CRD.ESPS y NO se toca: moverla seria cambiar otra cosa.
--   - CUOTA: el vigente es **DTPRESTD**; `DTPRIDST` se escribe como copia y puede quedar
--     desfasada. La aplicacion mueve LOS DOS (AcuerdoCondonacionServiceImpl:569-570), asi que
--     este script tambien, o la cuota quedaria diciendo dos cosas distintas.
--   - `DTPRFCPG` (fechaPagado) se pone en NULL, igual que hace la aplicacion al marcar una
--     cuota como cancelada anticipada: no se pago, se cancelo.
--
-- QUE CUENTA COMO "NO PAGADA": todo lo que no sea PAGADA(4) ni ya CANCELADA_ANTICIPADA(7).
--   Incluye PENDIENTE(1), ACTIVA(2), EMITIDA(3), EN_MORA(5), PARCIAL(6) y VENCIDA(8).
--   ⚠️ PARCIAL entra: una cuota pagada a medias NO esta pagada. Si el criterio del negocio
--      fuera otro, PARAR y avisar — el bloque 0.2 muestra cuantas hay de cada estado.
--
-- COMO CORRERLO: bloque 0 primero y MANDARME LA SALIDA antes de seguir. Es una operacion
-- masiva y el numero de prestamos afectados decide si esto es lo que se queria.
-- =====================================================================================

SET PAGESIZE 300
SET LINESIZE 260
SET FEEDBACK ON


-- =====================================================================================
-- BLOQUE 0 — CONTROL ANTES. SOLO LECTURA. **CORRER Y REVISAR ANTES DE TODO LO DEMAS.**
-- =====================================================================================

-- 0.1 Cuantos prestamos estan en VIGENTE_POR_REVISAR
SELECT COUNT(*) AS PRESTAMOS_VIGENTE_POR_REVISAR
  FROM CRD.PRST p
 WHERE p.PRSTIDST = 10;

-- 0.2 Sus cuotas, agrupadas por estado. Aca se ve exactamente que se va a mover.
SELECT d.DTPRESTD AS ESTADO_CUOTA,
       CASE d.DTPRESTD
            WHEN 1 THEN 'PENDIENTE'  WHEN 2 THEN 'ACTIVA'   WHEN 3 THEN 'EMITIDA'
            WHEN 4 THEN 'PAGADA'     WHEN 5 THEN 'EN MORA'  WHEN 6 THEN 'PARCIAL'
            WHEN 7 THEN 'CANCELADA ANTICIPADA'              WHEN 8 THEN 'VENCIDA'
            ELSE 'OTRO' END AS NOMBRE,
       COUNT(*) AS CUOTAS,
       ROUND(SUM(NVL(d.DTPRTTLL, 0)), 2) AS TOTAL,
       CASE WHEN d.DTPRESTD IN (4, 7) THEN 'no se toca' ELSE '>>> SE MUEVE A 7' END AS ACCION
  FROM CRD.DTPR d
  JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
 WHERE p.PRSTIDST = 10
 GROUP BY d.DTPRESTD
 ORDER BY d.DTPRESTD;

-- 0.3 ⛔ CUANTA DEUDA SE DA POR CANCELADA. Este es el numero que hay que mirar dos veces.
SELECT COUNT(*) AS CUOTAS_A_CANCELAR,
       COUNT(DISTINCT d.PRSTCDGO) AS PRESTAMOS_INVOLUCRADOS,
       ROUND(SUM(NVL(d.DTPRTTLL, 0)), 2) AS DEUDA_QUE_DEJA_DE_FIGURAR
  FROM CRD.DTPR d
  JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
 WHERE p.PRSTIDST = 10
   AND d.DTPRESTD NOT IN (4, 7);

-- 0.4 Detalle por prestamo, para revisarlo con ojos de negocio
SELECT p.PRSTCDGO AS PRESTAMO, p.ENTDCDGO AS ENTIDAD, e.ENTDRZNS AS PARTICIPE,
       COUNT(d.DTPRCDGO) AS CUOTAS_A_CANCELAR,
       ROUND(SUM(NVL(d.DTPRTTLL, 0)), 2) AS DEUDA
  FROM CRD.PRST p
  JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO AND d.DTPRESTD NOT IN (4, 7)
  LEFT JOIN CRD.ENTD e ON e.ENTDCDGO = p.ENTDCDGO
 WHERE p.PRSTIDST = 10
 GROUP BY p.PRSTCDGO, p.ENTDCDGO, e.ENTDRZNS
 ORDER BY p.PRSTCDGO;


-- =====================================================================================
-- BLOQUE 1 — RESPALDO. Sin esto NO HAY VUELTA ATRAS.
--
-- Un UPDATE masivo de estados no se puede revertir "a mano": no hay forma de saber despues
-- cual era el estado previo de cada cuota. Estas dos tablas lo guardan.
--
-- ⚠️ CREATE TABLE es DDL: hace COMMIT implicito. Correrlas ANTES de los UPDATE.
-- ⚠️ Son tablas AUXILIARES, fuera del modelo. No entran en el esquema de nombres de 4
--    letras del proyecto. Borrarlas recien cuando el resultado este validado (bloque 6).
-- =====================================================================================

CREATE TABLE CRD.BKP_211_DTPR AS
SELECT d.DTPRCDGO, d.PRSTCDGO, d.DTPRESTD, d.DTPRIDST, d.DTPRFCPG
  FROM CRD.DTPR d
  JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
 WHERE p.PRSTIDST = 10
   AND d.DTPRESTD NOT IN (4, 7);

CREATE TABLE CRD.BKP_211_PRST AS
SELECT p.PRSTCDGO, p.PRSTIDST
  FROM CRD.PRST p
 WHERE p.PRSTIDST = 10;

-- Control del respaldo: estos dos conteos tienen que coincidir con el 0.3 y el 0.1.
SELECT (SELECT COUNT(*) FROM CRD.BKP_211_DTPR) AS CUOTAS_RESPALDADAS,
       (SELECT COUNT(*) FROM CRD.BKP_211_PRST) AS PRESTAMOS_RESPALDADOS
  FROM DUAL;


-- =====================================================================================
-- BLOQUE 2 — LAS CUOTAS PRIMERO (el orden importa, ver encabezado)
-- Esperado: el mismo numero que dio el bloque 0.3.
-- =====================================================================================

UPDATE CRD.DTPR d
   SET d.DTPRESTD = 7,
       d.DTPRIDST = 7,
       d.DTPRFCPG = NULL
 WHERE d.DTPRESTD NOT IN (4, 7)
   AND EXISTS (SELECT 1 FROM CRD.PRST p
                WHERE p.PRSTCDGO = d.PRSTCDGO
                  AND p.PRSTIDST = 10);


-- =====================================================================================
-- BLOQUE 3 — Y RECIEN AHORA LOS PRESTAMOS
-- Esperado: el mismo numero que dio el bloque 0.1.
-- =====================================================================================

UPDATE CRD.PRST p
   SET p.PRSTIDST = 4
 WHERE p.PRSTIDST = 10;


-- =====================================================================================
-- BLOQUE 4 — CONTROL DESPUES (mirar ANTES de confirmar)
-- Esperado 4.1: 0 prestamos en estado 10.
-- Esperado 4.2: 0 cuotas sin pagar colgando de un prestamo cancelado anticipado del lote.
-- Esperado 4.3: tantos prestamos en estado 4 como respaldados.
-- Si algo no da: ROLLBACK y avisar. El respaldo del bloque 1 permite volver igual.
-- =====================================================================================

-- 4.1 No debe quedar ninguno en VIGENTE_POR_REVISAR
SELECT COUNT(*) AS QUEDAN_EN_10 FROM CRD.PRST WHERE PRSTIDST = 10;

-- 4.2 Ninguna cuota del lote debe haber quedado sin mover
SELECT COUNT(*) AS CUOTAS_SIN_MOVER
  FROM CRD.DTPR d
  JOIN CRD.BKP_211_PRST b ON b.PRSTCDGO = d.PRSTCDGO
 WHERE d.DTPRESTD NOT IN (4, 7);

-- 4.3 Como quedo el lote
SELECT p.PRSTIDST AS ESTADO_PRESTAMO, COUNT(*) AS PRESTAMOS
  FROM CRD.PRST p
  JOIN CRD.BKP_211_PRST b ON b.PRSTCDGO = p.PRSTCDGO
 GROUP BY p.PRSTIDST;

-- 4.4 Y las cuotas del lote, por estado
SELECT d.DTPRESTD AS ESTADO_CUOTA, COUNT(*) AS CUOTAS
  FROM CRD.DTPR d
  JOIN CRD.BKP_211_PRST b ON b.PRSTCDGO = d.PRSTCDGO
 GROUP BY d.DTPRESTD
 ORDER BY d.DTPRESTD;


-- =====================================================================================
-- BLOQUE 5 — CONFIRMAR
-- Descomentar recien despues de leer el BLOQUE 4.
-- =====================================================================================

-- COMMIT;


-- =====================================================================================
-- BLOQUE 6 — REVERSO (comentado). Devuelve TODO al estado exacto previo, desde el respaldo.
-- Sirve antes del COMMIT (ahi alcanza ROLLBACK) y tambien despues, mientras las tablas
-- auxiliares existan.
-- =====================================================================================

-- UPDATE CRD.DTPR d
--    SET (d.DTPRESTD, d.DTPRIDST, d.DTPRFCPG) =
--        (SELECT b.DTPRESTD, b.DTPRIDST, b.DTPRFCPG
--           FROM CRD.BKP_211_DTPR b WHERE b.DTPRCDGO = d.DTPRCDGO)
--  WHERE EXISTS (SELECT 1 FROM CRD.BKP_211_DTPR b WHERE b.DTPRCDGO = d.DTPRCDGO);
--
-- UPDATE CRD.PRST p
--    SET p.PRSTIDST = (SELECT b.PRSTIDST FROM CRD.BKP_211_PRST b WHERE b.PRSTCDGO = p.PRSTCDGO)
--  WHERE EXISTS (SELECT 1 FROM CRD.BKP_211_PRST b WHERE b.PRSTCDGO = p.PRSTCDGO);
--
-- COMMIT;


-- =====================================================================================
-- BLOQUE 7 — LIMPIEZA (comentado). Recien cuando el resultado este validado por el negocio.
-- Mientras estas tablas existan, el reverso del bloque 6 sigue siendo posible.
-- =====================================================================================

-- DROP TABLE CRD.BKP_211_DTPR;
-- DROP TABLE CRD.BKP_211_PRST;

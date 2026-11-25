-- =====================================================================================
-- C19-06 — Comparacion ANTES / DESPUES del WAR de C19b de los balances que ya generaba el motor (balance general,
--          estado de resultados, balance de comprobacion, por centro de costo, distribuido, acumulado y por periodo).
-- Cuadre Superintendencia, tarea C19b (D-031) · 2026-10-29. SOLO SELECT. NO SE HA EJECUTADO (requiere desplegar).
--
-- PROCEDIMIENTO (con curl/Postman contra /SaaBE/rest/dtmt/generarBalance, NO desde la pantalla: la pantalla borra
-- su ejecucion al salir). Mes con datos en la base de trabajo: septiembre 2026 (1-sep..30-sep, empresa 1236).
--   1. Con el WAR ANTERIOR, generar y anotar idEjecucion de cada combinacion:
--        alterno 1, 2, 3, 5 · acumulacion 0 y 1 · incluyeCentrosCosto true (alterno 3) · reporteDistribuido true
--        (alterno 3) · eliminarSaldosCero false (para comparar todas las filas).
--   2. Correr C19-05 (base de trabajo) y desplegar el WAR de C19b.
--   3. Repetir exactamente las mismas llamadas y anotar los nuevos idEjecucion.
--   4. Por cada par, reemplazar 111111 (antes) y 222222 (despues) abajo y correr. ESPERADO: 0 filas en las dos
--      consultas de diferencias y los mismos conteos/sumas en la de resumen.
--   5. Borrar las ejecuciones de prueba: DELETE /SaaBE/rest/dtmt/resultado/{id}.
-- =====================================================================================

-- Resumen de cada ejecucion: filas y sumas
SELECT DTMTSCRP, COUNT(*) filas, SUM(DTMTSLAN) saldo_anterior, SUM(DTMTDBEE) debe, SUM(DTMTHBRR) haber, SUM(DTMTSLAC) saldo_actual
  FROM CNT.DTMT WHERE DTMTSCRP IN (111111, 222222) GROUP BY DTMTSCRP;

-- Filas del ANTES que no estan igual en el DESPUES. ESPERADO: 0 filas.
SELECT DTMTCTCN, PLNNCDGO, NVL(DTMTCSNM, '-') centro, PLNNNMBR, PLNNNVLL, PLNNTPOO, DTMTSLAN, DTMTDBEE, DTMTHBRR, DTMTSLAC
  FROM CNT.DTMT WHERE DTMTSCRP = 111111
MINUS
SELECT DTMTCTCN, PLNNCDGO, NVL(DTMTCSNM, '-') centro, PLNNNMBR, PLNNNVLL, PLNNTPOO, DTMTSLAN, DTMTDBEE, DTMTHBRR, DTMTSLAC
  FROM CNT.DTMT WHERE DTMTSCRP = 222222;

-- Filas del DESPUES que no estaban en el ANTES. ESPERADO: 0 filas.
SELECT DTMTCTCN, PLNNCDGO, NVL(DTMTCSNM, '-') centro, PLNNNMBR, PLNNNVLL, PLNNTPOO, DTMTSLAN, DTMTDBEE, DTMTHBRR, DTMTSLAC
  FROM CNT.DTMT WHERE DTMTSCRP = 222222
MINUS
SELECT DTMTCTCN, PLNNCDGO, NVL(DTMTCSNM, '-') centro, PLNNNMBR, PLNNNVLL, PLNNTPOO, DTMTSLAN, DTMTDBEE, DTMTHBRR, DTMTSLAC
  FROM CNT.DTMT WHERE DTMTSCRP = 111111;

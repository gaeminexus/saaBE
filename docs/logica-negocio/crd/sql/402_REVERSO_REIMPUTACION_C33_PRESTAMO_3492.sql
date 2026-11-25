-- =====================================================================================================================================
-- 402 - REVERSO de la re-imputación C33 del PRÉSTAMO 3492 (ejecución de 2026-11-25 05:36, usuario «USUARIO 1»). SOLO este préstamo.
-- Defecto: el control C33 y la re-imputación no veían los pagos de MIGRACIÓN fechados en el vencimiento futuro de las cuotas 45-68 (PGPRFCHA > corte,
-- pero registrados el 22-04-2026). Creyeron que la cuota 49 tenía saldo antes de la 69, y movieron los pagos Petro de las 69/70/71 a la 49/50
-- (que quedaron pagadas al doble). Este script devuelve los pagos a su cuota original.
--
-- NO HACE COMMIT. Correr en SQL Developer / sqlplus con autocommit apagado, revisar las salidas de cada bloque, y recién entonces COMMIT o ROLLBACK.
-- La parte CONTABLE (asientos de re-provisión del bloque 6) NO se revierte por SQL: ver el bloque 6 y el .md DISENO-CORRECCION-C33-PAGOS-REGISTRADOS-ANTES-DEL-CORTE.md.
--
-- Datos del ejecucion_reimputacion.json: creados 312586..312591 (el 312591 va a la cuota 51, desgravamen 0,08); anulados 289354, 289355, 292586, 292587; asientoReProvision y asientoReversoProvision = null. Cuotas afectadas: 49, 50, 51, 69, 70 y 71.
-- =====================================================================================================================================
SET SERVEROUTPUT ON;

-- ---------- 0. DIAGNÓSTICO (solo lectura) ---------------------------------------------------------------------------------------
-- 0a. Cuotas del préstamo 3492 afectadas y su estado actual.
SELECT d.DTPRCDGO, d.DTPRNMCT, d.DTPRFCVN, d.DTPRESTD, d.DTPRCPTL, d.DTPRINTR, d.DTPRCPPG, d.DTPRINPG, d.DTPRSLCP, d.DTPRSLIN, d.DTPRSLDO, d.DTPRFCPG
  FROM CRD.DTPR d WHERE d.PRSTCDGO = 3492 AND d.DTPRNMCT IN (49, 50, 69, 70, 71) ORDER BY d.DTPRNMCT;
-- 0b. Todos los pagos tocados por la re-imputación en este préstamo (nuevos y anulados C33), con sus fechas.
SELECT g.PGPRCDGO, g.DTPRCDGO, g.PGPRNMCT, g.PGPRFCHA, g.PGPRFCRG, g.PGPRTPOO, g.PGPRCPPG, g.PGPRINPG, g.PGPRVLRR, NVL(g.PGPRANUL,0) AS anulado,
       g.PGPRUSAN, g.PGPRFCAN, SUBSTR(g.PGPRMTAN, 1, 80) AS motivo, SUBSTR(g.PGPROBSR, 1, 80) AS observacion
  FROM CRD.PGPR g
 WHERE g.DTPRCDGO IN (SELECT x.DTPRCDGO FROM CRD.DTPR x WHERE x.PRSTCDGO = 3492)
   AND (g.PGPRMTAN LIKE '%C33-REIMPUTADO%' OR g.PGPROBSR LIKE '%C33-REIMPUTADO de P%')
 ORDER BY g.PGPRNMCT, g.PGPRCDGO;
-- 0c. Lo que escribió la re-provisión / el reverso de provisión de la re-imputación (MVIC y asientos): origen C33_REIMPUTACION, idOrigen = préstamo.
SELECT m.MVICCDGO, m.DTPRCDGO, m.MVICTPMV, m.MVICCMPN, m.MVICVLOR, m.MVICFCCT, m.ASNTCDGO, NVL(m.MVICANUL,0) AS anulado, m.MVICFCRG
  FROM CRD.MVIC m WHERE m.MVICORGN = 'C33_REIMPUTACION' AND m.MVICIDOR = 3492 ORDER BY m.MVICCDGO;

-- ---------- 1. RESPALDO (tablas nuevas; si ya existen el CREATE falla y NO se pisa un respaldo anterior) -----------------------
CREATE TABLE CRD.PGPR_RESP_402 AS SELECT g.*, SYSTIMESTAMP AS RESPALDADO_EN FROM CRD.PGPR g
  WHERE g.DTPRCDGO IN (SELECT x.DTPRCDGO FROM CRD.DTPR x WHERE x.PRSTCDGO = 3492);
CREATE TABLE CRD.DTPR_RESP_402 AS SELECT d.*, SYSTIMESTAMP AS RESPALDADO_EN FROM CRD.DTPR d WHERE d.PRSTCDGO = 3492;
CREATE TABLE CRD.MVIC_RESP_402 AS SELECT m.*, SYSTIMESTAMP AS RESPALDADO_EN FROM CRD.MVIC m
  WHERE m.DTPRCDGO IN (SELECT x.DTPRCDGO FROM CRD.DTPR x WHERE x.PRSTCDGO = 3492);

-- ---------- 2. CONTROLES ANTES (abortan si algo no es lo esperado) -------------------------------------------------------------
DECLARE
    v_nuevos   NUMBER;
    v_anulados NUMBER;
    v_huerfanos NUMBER;
BEGIN
    -- Los 6 pagos nuevos conocidos (ejecucion_reimputacion.json): vigentes, de este préstamo y con la marca de la re-imputación.
    SELECT COUNT(*) INTO v_nuevos FROM CRD.PGPR g
     WHERE g.PGPRCDGO IN (312586, 312587, 312588, 312589, 312590, 312591) AND NVL(g.PGPRANUL,0) = 0
       AND g.PGPROBSR LIKE '%C33-REIMPUTADO de P%'
       AND g.DTPRCDGO IN (SELECT x.DTPRCDGO FROM CRD.DTPR x WHERE x.PRSTCDGO = 3492);
    IF v_nuevos <> 6 THEN
        RAISE_APPLICATION_ERROR(-20401, 'Se esperaban 6 pagos nuevos vigentes (312586..312591) y hay ' || v_nuevos || '. No se toca nada.');
    END IF;
    -- Otros pagos vigentes con la marca de re-imputación en este préstamo que NO estén en la lista (no se revierten sin revisarlos).
    SELECT COUNT(*) INTO v_huerfanos FROM CRD.PGPR g
     WHERE g.PGPROBSR LIKE '%C33-REIMPUTADO de P%' AND NVL(g.PGPRANUL,0) = 0
       AND g.PGPRCDGO NOT IN (312586, 312587, 312588, 312589, 312590, 312591)
       AND g.DTPRCDGO IN (SELECT x.DTPRCDGO FROM CRD.DTPR x WHERE x.PRSTCDGO = 3492);
    IF v_huerfanos > 0 THEN
        RAISE_APPLICATION_ERROR(-20402, 'Hay ' || v_huerfanos || ' pago(s) vigente(s) re-imputados del 3492 fuera de la lista: revisar antes.');
    END IF;
    -- Anulados por la ejecución de hoy (los originales a reactivar).
    SELECT COUNT(*) INTO v_anulados FROM CRD.PGPR g
     WHERE NVL(g.PGPRANUL,0) = 1 AND g.PGPRMTAN LIKE '%C33-REIMPUTADO%' AND g.PGPRFCAN >= TIMESTAMP '2026-11-25 00:00:00'
       AND g.DTPRCDGO IN (SELECT x.DTPRCDGO FROM CRD.DTPR x WHERE x.PRSTCDGO = 3492);
    IF v_anulados <> 4 THEN
        RAISE_APPLICATION_ERROR(-20403, 'Se esperaban 4 pagos anulados por la re-imputación del 25-nov (289354, 289355, 292586, 292587) y hay ' || v_anulados || '. No se toca nada.');
    END IF;
    DBMS_OUTPUT.PUT_LINE('Controles previos OK: 6 pagos nuevos, ' || v_anulados || ' anulado(s) a reactivar.');
END;
/

-- ---------- 3. REVERSO DE PAGOS ------------------------------------------------------------------------------------------------
-- 3a. Guarda qué cuotas se tocan (para recalcularlas en el bloque 4).
CREATE GLOBAL TEMPORARY TABLE CRD.TMP_402_CUOTAS (DTPRCDGO NUMBER) ON COMMIT PRESERVE ROWS;
INSERT INTO CRD.TMP_402_CUOTAS
SELECT DISTINCT g.DTPRCDGO FROM CRD.PGPR g
 WHERE (g.PGPRCDGO IN (312586, 312587, 312588, 312589, 312590, 312591)
        OR (NVL(g.PGPRANUL,0) = 1 AND g.PGPRMTAN LIKE '%C33-REIMPUTADO%' AND g.PGPRFCAN >= TIMESTAMP '2026-11-25 00:00:00'))
   AND g.DTPRCDGO IN (SELECT x.DTPRCDGO FROM CRD.DTPR x WHERE x.PRSTCDGO = 3492);
-- 3b. Anula los 6 nuevos.
UPDATE CRD.PGPR SET PGPRANUL = 1, PGPRUSAN = 'REVERSO_SQL402', PGPRFCAN = SYSTIMESTAMP, PGPRMTAN = 'Reverso del defecto C33 del préstamo 3492 (sql 402)'
 WHERE PGPRCDGO IN (312586, 312587, 312588, 312589, 312590, 312591) AND NVL(PGPRANUL,0) = 0;
-- 3c. Reactiva los originales anulados por la re-imputación de hoy.
UPDATE CRD.PGPR SET PGPRANUL = 0, PGPRUSAN = NULL, PGPRFCAN = NULL, PGPRMTAN = NULL
 WHERE NVL(PGPRANUL,0) = 1 AND PGPRMTAN LIKE '%C33-REIMPUTADO%' AND PGPRFCAN >= TIMESTAMP '2026-11-25 00:00:00'
   AND DTPRCDGO IN (SELECT x.DTPRCDGO FROM CRD.DTPR x WHERE x.PRSTCDGO = 3492);

-- ---------- 4. CUOTAS: se reconstruyen desde sus pagos vigentes, con la MISMA fórmula de MotorPagoPrestamoService.recalcularCuotaDesdePagos ----
-- (la cuota es función de sus pagos vigentes: pagados = Σ pagos, saldos = componente − pagado, estado = PAGADA 4 / PARCIAL 6 / EN_MORA 5 | PENDIENTE 1).
-- La mora (DTPRMRAA) NO se recalcula aquí: se deja como está y el bloque 7 la muestra; las cuotas 69-71 vencen en el futuro.
UPDATE CRD.DTPR d SET
    (d.DTPRCPPG, d.DTPRINPG, d.DTPRDSPG, d.DTPRMRPG, d.DTPRINVP) =
        (SELECT ROUND(NVL(SUM(g.PGPRCPPG),0),2), ROUND(NVL(SUM(g.PGPRINPG),0),2), ROUND(NVL(SUM(g.PGPRDSGR),0),2),
                ROUND(NVL(SUM(g.PGPRMRPG),0),2), ROUND(NVL(SUM(g.PGPRINVP),0),2)
           FROM CRD.PGPR g WHERE g.DTPRCDGO = d.DTPRCDGO AND NVL(g.PGPRANUL,0) = 0)
 WHERE d.DTPRCDGO IN (SELECT DTPRCDGO FROM CRD.TMP_402_CUOTAS);
UPDATE CRD.DTPR d SET
    d.DTPRSLIN = GREATEST(0, ROUND(NVL(d.DTPRINTR,0) - NVL(d.DTPRINPG,0), 2)),
    d.DTPRSLMR = GREATEST(0, ROUND(NVL(d.DTPRMRAA,0) - NVL(d.DTPRMRPG,0), 2)),
    d.DTPRSLIV = GREATEST(0, ROUND(NVL(d.DTPRINVN,0) - NVL(d.DTPRINVP,0), 2)),
    d.DTPRSLCP = GREATEST(0, ROUND(NVL(d.DTPRSICP,0) - NVL(d.DTPRCPPG,0), 2))
 WHERE d.DTPRCDGO IN (SELECT DTPRCDGO FROM CRD.TMP_402_CUOTAS);
UPDATE CRD.DTPR d SET d.DTPRSLDO = GREATEST(0, ROUND(
        NVL(d.DTPRCPTL,0) + NVL(d.DTPRINTR,0) + NVL(d.DTPRDSGR,0) + NVL(d.DTPRVLSI,0) + NVL(d.DTPRMRAA,0) + NVL(d.DTPRINVN,0)
      - (NVL(d.DTPRCPPG,0) + NVL(d.DTPRINPG,0) + NVL(d.DTPRDSPG,0) + NVL(d.DTPRMRPG,0) + NVL(d.DTPRINVP,0)
         + NVL((SELECT SUM(g.PGPRVLSI) FROM CRD.PGPR g WHERE g.DTPRCDGO = d.DTPRCDGO AND NVL(g.PGPRANUL,0) = 0), 0)), 2))
 WHERE d.DTPRCDGO IN (SELECT DTPRCDGO FROM CRD.TMP_402_CUOTAS);
-- Estado: total pendiente por componente (cada saldo acotado en 0) como el motor.
UPDATE CRD.DTPR d SET
    d.DTPRESTD = CASE
        WHEN (GREATEST(0, ROUND(NVL(d.DTPRDSGR,0) - NVL(d.DTPRDSPG,0), 2)) + d.DTPRSLMR + d.DTPRSLIV + d.DTPRSLIN + d.DTPRSLCP
              + GREATEST(0, ROUND(NVL(d.DTPRVLSI,0) - NVL((SELECT SUM(g.PGPRVLSI) FROM CRD.PGPR g WHERE g.DTPRCDGO = d.DTPRCDGO AND NVL(g.PGPRANUL,0) = 0), 0), 2))) <= 0.01 THEN 4
        WHEN (NVL(d.DTPRCPPG,0) + NVL(d.DTPRINPG,0) + NVL(d.DTPRDSPG,0) + NVL(d.DTPRMRPG,0) + NVL(d.DTPRINVP,0)
              + NVL((SELECT SUM(g.PGPRVLSI) FROM CRD.PGPR g WHERE g.DTPRCDGO = d.DTPRCDGO AND NVL(g.PGPRANUL,0) = 0), 0)) > 0.005 THEN 6
        WHEN d.DTPRFCVN < TRUNC(SYSDATE) THEN 5
        ELSE 1 END
 WHERE d.DTPRCDGO IN (SELECT DTPRCDGO FROM CRD.TMP_402_CUOTAS);
UPDATE CRD.DTPR d SET d.DTPRIDST = d.DTPRESTD WHERE d.DTPRCDGO IN (SELECT DTPRCDGO FROM CRD.TMP_402_CUOTAS);
-- Fecha de pagado: PAGADA conserva la que tenga o toma la del último pago vigente; las demás, nula (como el motor).
UPDATE CRD.DTPR d SET d.DTPRFCPG =
        CASE WHEN d.DTPRESTD = 4 THEN NVL(d.DTPRFCPG, (SELECT MAX(g.PGPRFCHA) FROM CRD.PGPR g WHERE g.DTPRCDGO = d.DTPRCDGO AND NVL(g.PGPRANUL,0) = 0))
             ELSE NULL END
 WHERE d.DTPRCDGO IN (SELECT DTPRCDGO FROM CRD.TMP_402_CUOTAS);

-- ---------- 5. CONTROLES DESPUÉS (revisar antes de COMMIT) --------------------------------------------------------------------
-- 5a. Cada cuota tocada: lo pagado por capital/interés no puede superar lo que la cuota debe (las 49 y 50 ya no pagadas al doble).
SELECT d.DTPRNMCT, d.DTPRESTD, d.DTPRCPTL, d.DTPRCPPG, d.DTPRINTR, d.DTPRINPG, d.DTPRSLDO, d.DTPRFCPG,
       CASE WHEN NVL(d.DTPRCPPG,0) > NVL(d.DTPRCPTL,0) + 0.01 OR NVL(d.DTPRINPG,0) > NVL(d.DTPRINTR,0) + 0.01 THEN 'PAGADA DE MÁS' ELSE 'OK' END AS control
  FROM CRD.DTPR d WHERE d.DTPRCDGO IN (SELECT DTPRCDGO FROM CRD.TMP_402_CUOTAS) ORDER BY d.DTPRNMCT;
-- 5b. Ninguna cuota del préstamo pagada de más (debe dar 0 filas).
SELECT d.DTPRNMCT, d.DTPRCPTL, d.DTPRCPPG, d.DTPRINTR, d.DTPRINPG FROM CRD.DTPR d
 WHERE d.PRSTCDGO = 3492 AND (NVL(d.DTPRCPPG,0) > NVL(d.DTPRCPTL,0) + 0.01 OR NVL(d.DTPRINPG,0) > NVL(d.DTPRINTR,0) + 0.01);
-- 5c. Lo pagado en las cuotas = Σ pagos vigentes (debe dar 0 filas).
SELECT d.DTPRNMCT, d.DTPRCPPG, s.cap FROM CRD.DTPR d
  JOIN (SELECT g.DTPRCDGO, ROUND(SUM(NVL(g.PGPRCPPG,0)),2) cap FROM CRD.PGPR g WHERE NVL(g.PGPRANUL,0) = 0 GROUP BY g.DTPRCDGO) s ON s.DTPRCDGO = d.DTPRCDGO
 WHERE d.PRSTCDGO = 3492 AND ABS(NVL(d.DTPRCPPG,0) - s.cap) > 0.01;
-- 5d. El total pagado del préstamo (Σ pagos vigentes) debe ser igual al del respaldo MENOS lo que la re-imputación no cambió: se espera igual.
SELECT (SELECT ROUND(SUM(NVL(PGPRCPPG,0) + NVL(PGPRINPG,0)),2) FROM CRD.PGPR WHERE NVL(PGPRANUL,0) = 0 AND DTPRCDGO IN (SELECT DTPRCDGO FROM CRD.DTPR WHERE PRSTCDGO = 3492)) AS vigente_despues,
       (SELECT ROUND(SUM(NVL(PGPRCPPG,0) + NVL(PGPRINPG,0)),2) FROM CRD.PGPR_RESP_402 WHERE NVL(PGPRANUL,0) = 0) AS vigente_antes
  FROM DUAL;
-- 5e. Con el control del cierre (lo ejecuta la aplicación): POST /rest/prelacioncuotas/... al corte 2026-11-30 todavía dará rotura FALSA hasta desplegar la
--     corrección del código (ver el .md): omitir el préstamo 3492 con motivo en el cierre de noviembre.

-- ---------- 6. PROVISIÓN / ASIENTOS ----------------------------------------------------------------------------------------------------
-- ejecucion_reimputacion.json: asientoReProvision y asientoReversoProvision = null para el 3492: NO hay asiento ni MVIC de la re-imputación que revertir.
-- El bloque 0c lo comprueba: debe dar 0 filas. Si diera filas, NO se tocan por SQL (asientos vivos): pasar la lista al árbitro.

-- ---------- 7. MORA de las cuotas tocadas (solo lectura; si 69/70/71 muestran mora distinta de 0 se corrige aparte) ---------------------------
SELECT d.DTPRNMCT, d.DTPRFCVN, d.DTPRMRAA, d.DTPRMRPG, d.DTPRSLMR, d.DTPRMRCL, d.DTPRDSMR FROM CRD.DTPR d
 WHERE d.DTPRCDGO IN (SELECT DTPRCDGO FROM CRD.TMP_402_CUOTAS) ORDER BY d.DTPRNMCT;

-- SIN COMMIT. Si todo está bien:  COMMIT;   Si algo no cuadra:  ROLLBACK;
-- ---------- REVERSO DE ESTE SCRIPT (después de un COMMIT; solo con orden del árbitro) ------------------------------------------------
--   UPDATE CRD.PGPR g SET (g.PGPRANUL, g.PGPRUSAN, g.PGPRFCAN, g.PGPRMTAN) =
--          (SELECT r.PGPRANUL, r.PGPRUSAN, r.PGPRFCAN, r.PGPRMTAN FROM CRD.PGPR_RESP_402 r WHERE r.PGPRCDGO = g.PGPRCDGO)
--    WHERE g.PGPRCDGO IN (SELECT PGPRCDGO FROM CRD.PGPR_RESP_402);
--   UPDATE CRD.DTPR d SET (d.DTPRCPPG, d.DTPRINPG, d.DTPRDSPG, d.DTPRMRPG, d.DTPRINVP, d.DTPRSLIN, d.DTPRSLMR, d.DTPRSLIV, d.DTPRSLCP, d.DTPRSLDO,
--          d.DTPRESTD, d.DTPRIDST, d.DTPRFCPG) =
--          (SELECT r.DTPRCPPG, r.DTPRINPG, r.DTPRDSPG, r.DTPRMRPG, r.DTPRINVP, r.DTPRSLIN, r.DTPRSLMR, r.DTPRSLIV, r.DTPRSLCP, r.DTPRSLDO,
--                  r.DTPRESTD, r.DTPRIDST, r.DTPRFCPG FROM CRD.DTPR_RESP_402 r WHERE r.DTPRCDGO = d.DTPRCDGO)
--    WHERE d.DTPRCDGO IN (SELECT DTPRCDGO FROM CRD.DTPR_RESP_402);

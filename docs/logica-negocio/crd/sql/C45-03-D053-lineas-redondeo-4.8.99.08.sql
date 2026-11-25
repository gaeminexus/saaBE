-- =====================================================================================
-- C45-03 · D-053 — línea de redondeo (4.8.99.08 GND POR AJUSTES) en las plantillas del cierre
--           de cartera y de la nómina
-- FECHA: 2026-11-24 · rama cuadre/c45-bloqueantes-produccion · empresa 1236
--
-- ⛔ NO SE EJECUTA DESDE LA RAMA. Va ANTES del EAR de C45: con el EAR nuevo, un asiento del cierre
--    de cartera o de la nómina que tenga diferencia de redondeo y cuya plantilla no tenga la línea
--    se DETIENE con mensaje (ya no absorbe en otra línea).
--
-- POR QUÉ. D-053 (líder, 9-nov-2026): toda diferencia de centavos por redondeo va a 4.8.99.08 GND
-- POR AJUSTES, en todos los motores, sin absorberla en otra línea; la única excepción es el SRI
-- (4.8.90.90.35, sin cambio). Con C45 el código ya no ajusta la línea de negocio: agrega una línea
-- propia con la cuenta de la línea de redondeo de la plantilla del mismo asiento.
--   · CRD, CrdLineaAsiento.REDONDEO_AJUSTES = 91, en las plantillas alterno 1 (apertura), 17
--     (devengo de intereses y mora al corte), 33 (neteo de planillas) y 36 (provisión de intereses):
--     CierreCarteraServiceImpl.cuadra.
--   · RHH, RhhLineaAsiento.REDONDEO_AJUSTES = 90, en las plantillas de RHH.CFNM (rol, provisiones,
--     pago y liquidación; hoy 163, 164, 165 y 166): ContabilizacionNominaServiceImpl.comprobarCuadre.
-- DTPLMVMN se siembra 1 (DEBE): el lado lo decide el código según el signo de la diferencia.
-- La cuenta 4.8.99.08 ya existe en producción (C24 §2; local PLNNCDGO 11069): este script no la crea.
-- =====================================================================================


-- =====================================================================================
-- BLOQUE 0 — SOLO LECTURA (control ANTES)
-- =====================================================================================

-- 0.1 La cuenta existe y es de movimiento. Esperado: 1 fila, PLNNTPOO 2.
SELECT PLNNCDGO, PLNNCNTA, PLNNNMBR, PLNNTPOO FROM CNT.PLNN WHERE PJRQCDGO = 1236 AND PLNNCNTA = '4.8.99.08';

-- 0.2 Las 8 plantillas existen una sola vez. Esperado: 8 filas, CUANTAS = 1 en todas.
SELECT p.PLNSCDAL, COUNT(*) AS CUANTAS, MIN(p.PLNSCDGO) AS PLNSCDGO, MIN(p.PLNSNMBR) AS NOMBRE
  FROM CNT.PLNS p
 WHERE p.PJRQCDGO = 1236
   AND (p.PLNSCDAL IN (1, 17, 33, 36)
        OR p.PLNSCDAL IN (SELECT c.CFNMPLRL FROM RHH.CFNM c WHERE c.PJRQCDGO = 1236
                          UNION SELECT c.CFNMPLPR FROM RHH.CFNM c WHERE c.PJRQCDGO = 1236
                          UNION SELECT c.CFNMPLPG FROM RHH.CFNM c WHERE c.PJRQCDGO = 1236
                          UNION SELECT c.CFNMPLLQ FROM RHH.CFNM c WHERE c.PJRQCDGO = 1236))
 GROUP BY p.PLNSCDAL ORDER BY 1;

-- 0.3 Los auxiliares están libres. Esperado: 0 filas (aux1 91 en las de crédito, 90 en las de nómina).
SELECT p.PLNSCDAL, d.DTPLAXL1, d.DTPLCDGO
  FROM CNT.PLNS p JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
 WHERE p.PJRQCDGO = 1236
   AND ((p.PLNSCDAL IN (1, 17, 33, 36) AND d.DTPLAXL1 = 91)
        OR (p.PLNSCDAL IN (SELECT c.CFNMPLRL FROM RHH.CFNM c UNION SELECT c.CFNMPLPR FROM RHH.CFNM c
                           UNION SELECT c.CFNMPLPG FROM RHH.CFNM c UNION SELECT c.CFNMPLLQ FROM RHH.CFNM c)
            AND d.DTPLAXL1 = 90));

-- 0.4 La secuencia de DTPL va por delante del MAX (si no, ORA-00001; ver crd/sql/356 bloque 0.4).
SELECT (SELECT MAX(DTPLCDGO) FROM CNT.DTPL) AS MAX_PK,
       (SELECT LAST_NUMBER FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER = 'CNT' AND SEQUENCE_NAME = 'SQ_DTPLCDGO') AS SECUENCIA
  FROM DUAL;

-- 0.5 Detalle 90 del rubro 214 (catálogo RHH_LINEA_ASIENTO). Esperado: 0 filas.
SELECT d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 214 AND d.PDTRALTR = 90;


-- =====================================================================================
-- BLOQUE 1 — Línea 91 en las plantillas de crédito 1, 17, 33 y 36
-- =====================================================================================

INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLAXL1, DTPLAXL2, DTPLMVMN, DTPLDSCR, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, n.PLNNCDGO, 91, 0, 1,
       'Diferencia por redondeo del asiento (D-053, GND por ajustes)', 1
  FROM CNT.PLNS p
  JOIN CNT.PLNN n ON n.PJRQCDGO = p.PJRQCDGO AND n.PLNNCNTA = '4.8.99.08'
 WHERE p.PJRQCDGO = 1236 AND p.PLNSCDAL IN (1, 17, 33, 36)
   AND NOT EXISTS (SELECT 1 FROM CNT.DTPL d WHERE d.PLNSCDGO = p.PLNSCDGO AND d.DTPLAXL1 = 91);
-- (!) 4 filas insertadas.


-- =====================================================================================
-- BLOQUE 2 — Línea 90 en las plantillas de nómina de RHH.CFNM
-- =====================================================================================

INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLAXL1, DTPLAXL2, DTPLMVMN, DTPLDSCR, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, n.PLNNCDGO, 90, 0, 1,
       'Diferencia por redondeo del asiento (D-053, GND por ajustes)', 1
  FROM CNT.PLNS p
  JOIN CNT.PLNN n ON n.PJRQCDGO = p.PJRQCDGO AND n.PLNNCNTA = '4.8.99.08'
 WHERE p.PJRQCDGO = 1236
   AND p.PLNSCDAL IN (SELECT c.CFNMPLRL FROM RHH.CFNM c WHERE c.PJRQCDGO = 1236
                      UNION SELECT c.CFNMPLPR FROM RHH.CFNM c WHERE c.PJRQCDGO = 1236
                      UNION SELECT c.CFNMPLPG FROM RHH.CFNM c WHERE c.PJRQCDGO = 1236
                      UNION SELECT c.CFNMPLLQ FROM RHH.CFNM c WHERE c.PJRQCDGO = 1236)
   AND NOT EXISTS (SELECT 1 FROM CNT.DTPL d WHERE d.PLNSCDGO = p.PLNSCDGO AND d.DTPLAXL1 = 90);
-- (!) 4 filas insertadas.


-- =====================================================================================
-- BLOQUE 3 — Detalle 90 del rubro 214 (catálogo de líneas de RHH)
-- =====================================================================================

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
SELECT (SELECT NVL(MAX(PDTRCDGO), 0) + 1 FROM SCP.PDTR), r.PRBRCDGO,
       'DIFERENCIA POR REDONDEO (D-053, 4.8.99.08 GND POR AJUSTES)', NULL, NULL, 90, 1
  FROM SCP.PRBR r WHERE r.PRBRALTR = 214
   AND NOT EXISTS (SELECT 1 FROM SCP.PDTR d WHERE d.PRBRCDGO = r.PRBRCDGO AND d.PDTRALTR = 90);
-- (!) 1 fila insertada.

COMMIT;


-- =====================================================================================
-- BLOQUE 4 — CONTROL DESPUÉS
-- =====================================================================================

-- 4.1 Esperado: 8 filas (alternos 1, 17, 33, 36 con aux1 91 y los 4 de nómina con aux1 90), todas
--     con 4.8.99.08 y estado 1.
SELECT p.PLNSCDAL, p.PLNSCDGO, d.DTPLAXL1, d.DTPLAXL2, d.DTPLMVMN, n.PLNNCNTA, d.DTPLESTD
  FROM CNT.PLNS p JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PJRQCDGO = 1236 AND n.PLNNCNTA = '4.8.99.08'
 ORDER BY p.PLNSCDAL;

-- 4.2 Esperado: 1 fila.
SELECT d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 214 AND d.PDTRALTR = 90;


-- =====================================================================================
-- VUELTA ATRÁS (solo si hace falta; no se corre con el resto). Con el EAR de C45 desplegado,
-- quitar las líneas hace que un asiento con redondeo se detenga.
-- =====================================================================================
-- DELETE FROM CNT.DTPL d WHERE d.DTPLDSCR = 'Diferencia por redondeo del asiento (D-053, GND por ajustes)';
-- DELETE FROM SCP.PDTR d WHERE d.PDTRALTR = 90 AND d.PRBRCDGO = (SELECT r.PRBRCDGO FROM SCP.PRBR r WHERE r.PRBRALTR = 214);
-- COMMIT;

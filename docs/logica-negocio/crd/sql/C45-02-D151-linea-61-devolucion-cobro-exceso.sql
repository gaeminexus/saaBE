-- =====================================================================================
-- C45-02 · D-151 — la devolución al partícipe del descuento Petro/ARCH de más va a
--           2.3.01.15.04 DEVOLUCION POR COBRO EN EXCESO PARTICIPES (no a 2.3.90.90.10)
-- FECHA: 2026-11-24 · rama cuadre/c45-bloqueantes-produccion · empresa 1236
--
-- ⛔ NO SE EJECUTA DESDE LA RAMA. Lo revisa y lo corre el responsable del pase, por bloques.
--
-- POR QUÉ. D-151 (líder, 23-nov-2026): lo que Petro descontó de más a un partícipe y ASOPREP
-- le devuelve con respaldo NO va a aportes (D-010) sino a la cuenta por pagar 2.3.01.15.04, y
-- la transferencia de devolución se registra contra esa misma cuenta.
-- En el SAA ese caso es el destino del sobrante 3 = DEVOLUCIÓN (CRD.DSSB, B8): la aplicación
-- Petro (plantilla alterno 21) acredita la LÍNEA 61 y crea una orden de pago con el producto
-- del parámetro rubro 237 detalle 4. CobroPetroContableServiceImpl toma la cuenta de la línea
-- 61 y verifica que la cuenta del GRUPO del producto sea la misma (ProductoPagoParametro-
-- ServiceImpl.resolver). Hoy los dos apuntan a 2.3.90.90.10 (PLNNCDGO 10398): el grupo 79
-- «DESEMBOLSO DE PRESTAMOS A SOCIOS» es además el del desembolso, así que NO se toca su cuenta.
--
-- QUÉ HACE.
--   1. Crea el grupo de pago «DEVOLUCION POR COBRO EN EXCESO PARTICIPES» (copia del grupo del
--      producto del parámetro 4) con la cuenta 2.3.01.15.04.
--   2. Pasa el producto del parámetro 4 (hoy 1222 «DEVOLUCION DE DESCUENTO PETRO/ARCH A
--      PARTICIPE», solo usado por esta devolución) a ese grupo.
--   3. Cambia la cuenta de la línea 61 de la plantilla 21 a 2.3.01.15.04.
-- Ningún cambio de código hace falta: la cuenta sale de la plantilla (D-151 cumple así la
-- regla «la cuenta tiene que salir de la plantilla, no del código»).
--
-- NO CAMBIA: los excedentes sin devolución documentada (siguen a aportes por 2.3.02.10, D-010)
-- ni los meses ya presentados (lo de jun–jul 2025 se reclasifica a mano, D-151 / D-077 B-1).
-- =====================================================================================


-- =====================================================================================
-- BLOQUE 0 — SOLO LECTURA (control ANTES)
-- =====================================================================================

-- 0.1 La línea 61 hoy. Esperado: 1 fila, cuenta 2.3.90.90.10.
SELECT d.DTPLCDGO, d.DTPLAXL1, d.DTPLAXL2, d.DTPLMVMN, n.PLNNCDGO, n.PLNNCNTA, d.DTPLDSCR, d.DTPLESTD
  FROM CNT.PLNS p
  JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
  JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PLNSCDAL = 21 AND p.PJRQCDGO = 1236 AND d.DTPLAXL1 = 61;

-- 0.2 La cuenta destino existe y es de movimiento. Esperado: 1 fila (local: PLNNCDGO 10565, tipo 2).
SELECT PLNNCDGO, PLNNCNTA, PLNNNMBR, PLNNTPOO FROM CNT.PLNN WHERE PJRQCDGO = 1236 AND PLNNCNTA = '2.3.01.15.04';

-- 0.3 El producto del parámetro 4 y su grupo. Esperado: producto 1222, grupo 79, cuenta 2.3.90.90.10.
SELECT d.PDTRVLRN AS PRODUCTO, p.NOMBRE, p.ESTADO, g.GRPPCDGO, g.GRPPNMBR, n.PLNNCNTA
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
  JOIN PGS.PRDP p ON p.ID = d.PDTRVLRN
  JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO
  LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
 WHERE r.PRBRALTR = 237 AND d.PDTRALTR = 4;

-- 0.4 ⛔ LA CONSULTA QUE DECIDE: devoluciones (destino 3) con orden de pago. Si hay órdenes de
--     ese producto todavía sin pagar, se crearon acreditando 2.3.90.90.10 y al pagarlas con el
--     grupo nuevo se debitaría 2.3.01.15.04: pagarlas (o resolverlas) ANTES de correr el bloque 1.
--     Esperado: 0 filas (en la copia del 15-nov CRD.DSSB está vacía).
SELECT s.DSSBCDGO, s.CRARCDGO, s.DSSBVLAH + s.DSSBVLPR AS VALOR, s.DSSBPGTR, g.PGTRESTD
  FROM CRD.DSSB s
  LEFT JOIN PGS.PGTR g ON g.PGTRCDGO = s.DSSBPGTR
 WHERE s.DSSBTPDS = 3;

-- 0.5 No existe ya el grupo nuevo. Esperado: 0 filas (si hay 1, saltar el bloque 1).
SELECT GRPPCDGO, GRPPNMBR, PLNNCDGO FROM PGS.GRPP WHERE GRPPNMBR = 'DEVOLUCION POR COBRO EN EXCESO PARTICIPES';

-- 0.6 La secuencia del grupo va por delante del MAX (si NEXTVAL <= MAX, adelantarla como en
--     crd/sql/322 bloque 0.D antes del bloque 1).
SELECT NVL(MAX(GRPPCDGO), 0) AS MAX_GRPP FROM PGS.GRPP;


-- =====================================================================================
-- BLOQUE 1 — Grupo de pago con la cuenta 2.3.01.15.04 (copia del grupo del producto)
-- =====================================================================================

INSERT INTO PGS.GRPP (GRPPCDGO, GRPPNMBR, GRPPRYYA, GRPPRZZA, PLNNCDGO, GRPPCSUS, GRPPESTD, PJRQCDGO)
SELECT PGS.SQ_GRPPCDGO.NEXTVAL, 'DEVOLUCION POR COBRO EN EXCESO PARTICIPES',
       g.GRPPRYYA, g.GRPPRZZA, n.PLNNCDGO, g.GRPPCSUS, g.GRPPESTD, g.PJRQCDGO
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
  JOIN PGS.PRDP p ON p.ID = d.PDTRVLRN
  JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO
  JOIN CNT.PLNN n ON n.PJRQCDGO = g.PJRQCDGO AND n.PLNNCNTA = '2.3.01.15.04'
 WHERE r.PRBRALTR = 237 AND d.PDTRALTR = 4
   AND NOT EXISTS (SELECT 1 FROM PGS.GRPP x WHERE x.GRPPNMBR = 'DEVOLUCION POR COBRO EN EXCESO PARTICIPES');
-- (!) 1 fila insertada.


-- =====================================================================================
-- BLOQUE 2 — El producto del parámetro 4 pasa al grupo nuevo
-- =====================================================================================

UPDATE PGS.PRDP p
   SET p.GRUPOPRODUCTO = (SELECT g.GRPPCDGO FROM PGS.GRPP g WHERE g.GRPPNMBR = 'DEVOLUCION POR COBRO EN EXCESO PARTICIPES')
 WHERE p.ID = (SELECT d.PDTRVLRN FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
                WHERE r.PRBRALTR = 237 AND d.PDTRALTR = 4);
-- (!) 1 fila actualizada.


-- =====================================================================================
-- BLOQUE 3 — La línea 61 de la plantilla 21 a 2.3.01.15.04
-- =====================================================================================

UPDATE CNT.DTPL d
   SET d.PLNNCDGO = (SELECT n.PLNNCDGO FROM CNT.PLNN n WHERE n.PJRQCDGO = 1236 AND n.PLNNCNTA = '2.3.01.15.04'),
       d.DTPLDSCR = 'Devolucion al participe de un descuento Petro/ARCH de mas (D-151, cuenta por pagar)'
 WHERE d.DTPLAXL1 = 61
   AND d.PLNSCDGO = (SELECT p.PLNSCDGO FROM CNT.PLNS p WHERE p.PLNSCDAL = 21 AND p.PJRQCDGO = 1236);
-- (!) 1 fila actualizada.

COMMIT;


-- =====================================================================================
-- BLOQUE 4 — CONTROL DESPUÉS
-- =====================================================================================

-- 4.1 Esperado: línea 61 con 2.3.01.15.04.
SELECT d.DTPLAXL1, n.PLNNCNTA, d.DTPLDSCR
  FROM CNT.PLNS p JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PLNSCDAL = 21 AND p.PJRQCDGO = 1236 AND d.DTPLAXL1 = 61;

-- 4.2 Esperado: el producto del parámetro 4 en el grupo nuevo, con la MISMA cuenta que la línea 61
--     (si no coinciden, la aplicación Petro con devoluciones falla con PRODUCTO_PAGO_DEVOLUCION_NO_RESUELTO).
SELECT d.PDTRVLRN AS PRODUCTO, g.GRPPCDGO, g.GRPPNMBR, n.PLNNCNTA
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
  JOIN PGS.PRDP p ON p.ID = d.PDTRVLRN JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO
  JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
 WHERE r.PRBRALTR = 237 AND d.PDTRALTR = 4;

-- 4.3 Esperado: el grupo 79 (desembolso) sigue con 2.3.90.90.10 y sus otros productos intactos.
SELECT p.ID, p.NOMBRE, g.GRPPCDGO, n.PLNNCNTA
  FROM PGS.PRDP p JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
 WHERE n.PLNNCNTA = '2.3.90.90.10';


-- =====================================================================================
-- VUELTA ATRÁS (solo si hace falta; no se corre con el resto)
-- =====================================================================================
-- UPDATE CNT.DTPL d SET d.PLNNCDGO = (SELECT n.PLNNCDGO FROM CNT.PLNN n WHERE n.PJRQCDGO = 1236 AND n.PLNNCNTA = '2.3.90.90.10'),
--        d.DTPLDSCR = 'Devolucion al participe de un descuento Petro/ARCH (socios por pagar)'
--  WHERE d.DTPLAXL1 = 61 AND d.PLNSCDGO = (SELECT p.PLNSCDGO FROM CNT.PLNS p WHERE p.PLNSCDAL = 21 AND p.PJRQCDGO = 1236);
-- UPDATE PGS.PRDP SET GRUPOPRODUCTO = 79 WHERE ID = 1222;
-- DELETE FROM PGS.GRPP WHERE GRPPNMBR = 'DEVOLUCION POR COBRO EN EXCESO PARTICIPES';
-- COMMIT;

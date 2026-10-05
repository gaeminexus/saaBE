-- =====================================================================================
-- 314 — PRODUCCION: deja coherente la mora de la cuota 88 del prestamo idAsoprep 67023
-- FECHA: 2026-10-05 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- QUE PASO (sql/313): el cobro 126 (pago del 11-09, procesado el 22-09) pago la cuota 88 con
-- 5,30 de mora. Hoy se proceso el cobro tardio 174 (pago del 12-08), y el recalculo de mora a la
-- fecha de pago le bajo la mora de la 88 a 1,20 aunque ya estaba pagada con 5,30. Quedo
-- DTPRMRAA = 1,20 con DTPRMRPG = 5,30: incoherente.
--
-- REGLA DECIDIDA POR EL USUARIO (2026-10-05): el pago tardio NO toca la mora de las cuotas ya
-- pagadas por otro cobro con fecha posterior. Esas cuotas se quedan con la mora tal como se pago.
-- Este script aplica esa regla a la cuota 88: DTPRMRAA = lo pagado (5,30), saldo de mora 0.
-- No toca pagos, asientos ni otras cuotas. La regla en el codigo va en el WAR siguiente.
-- SQL PURO. Correr por bloques.
-- =====================================================================================

-- 0. Antes. Esperado: 1 fila, cuota 88, DTPRMRAA 1.2, DTPRMRPG 5.3, DTPRSLMR 0, estado 4.
SELECT d.DTPRCDGO, d.DTPRNMCT, d.DTPRESTD, d.DTPRMRAA, d.DTPRMRPG, d.DTPRSLMR
  FROM CRD.DTPR d
 WHERE d.DTPRCDGO = 656749;

-- 0.1 Respaldo.
CREATE TABLE CRD.BKP_314_DTPR AS
SELECT d.DTPRCDGO, d.DTPRMRAA, d.DTPRSLMR FROM CRD.DTPR d WHERE d.DTPRCDGO = 656749;

-- 1. La correccion. Esperado: 1 fila actualizada.
UPDATE CRD.DTPR d
   SET d.DTPRMRAA = d.DTPRMRPG,
       d.DTPRSLMR = 0
 WHERE d.DTPRCDGO = 656749
   AND NVL(d.DTPRMRPG, 0) > NVL(d.DTPRMRAA, 0);
COMMIT;

-- 2. Control. Esperado: DTPRMRAA 5.3 = DTPRMRPG 5.3, DTPRSLMR 0.
SELECT d.DTPRCDGO, d.DTPRNMCT, d.DTPRESTD, d.DTPRMRAA, d.DTPRMRPG, d.DTPRSLMR
  FROM CRD.DTPR d
 WHERE d.DTPRCDGO = 656749;

-- 3. Hay otras cuotas en la misma situacion? (mora persistida menor que la ya pagada). Solo lectura.
--    Esperado: 0 filas. Si sale alguna, pasarsela al arbitro.
SELECT p.PRSTIDAS, d.DTPRCDGO, d.DTPRNMCT, d.DTPRESTD, d.DTPRMRAA, d.DTPRMRPG
  FROM CRD.DTPR d JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
 WHERE NVL(d.DTPRMRPG, 0) > NVL(d.DTPRMRAA, 0) + 0.01
 ORDER BY p.PRSTIDAS, d.DTPRNMCT;

-- 4. REVERSO — COMENTADO
-- UPDATE CRD.DTPR d SET (d.DTPRMRAA, d.DTPRSLMR) =
--        (SELECT b.DTPRMRAA, b.DTPRSLMR FROM CRD.BKP_314_DTPR b WHERE b.DTPRCDGO = d.DTPRCDGO)
--  WHERE d.DTPRCDGO IN (SELECT b.DTPRCDGO FROM CRD.BKP_314_DTPR b);
-- COMMIT;  -- y luego: DROP TABLE CRD.BKP_314_DTPR;

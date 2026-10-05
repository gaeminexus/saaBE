-- =====================================================================================
-- 311 — CRD.PLVN: rellenar la «ultima fecha de cobro» de las declaraciones de plazo vencido
--       hechas ANTES de que el sistema la calculara
-- FECHA: 2026-10-05 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- RESULTADO DEL 308 (corrido por el usuario en produccion, 2026-10-05):
--   * 16 declaraciones (ids 1 y 17-31) tienen pagos en PGPR (MAX_FECHA_PGPR con valor) pero la foto
--     quedo vacia: se declararon el 30-09 entre las 15:30 y las 16:10, ANTES del WAR c005649d, que
--     agrego el calculo desde PGPR. Hoy saldrian bien; la foto no se recalcula sola. → CAUSA B.
--   * 3 declaraciones (34, 63, 125) no tienen ningun pago ni cuotas pagadas: el prestamo NUNCA
--     se pago. Vacia es lo correcto. → CAUSA D, no se tocan.
--   * No hubo ningun caso de causa A (pagos sin cuota) ni C (migracion sin fechas).
--
-- QUE HACE: llena PLVNFCUC con MAX(PGPRFCHA) de los pagos NO anulados del prestamo, SOLO donde hoy
-- esta vacia y SOLO si el prestamo tiene pagos. Es la misma regla que aplica el sistema desde el
-- WAR c005649d. Asi el memorando y la liquidacion se reimprimen con la fecha.
-- SQL PURO. Correr por bloques.
-- =====================================================================================

-- 0. Lo que se va a llenar. Esperado: las 16 filas de causa B (ids 1, 17-31), con su fecha.
SELECT v.PLVNCDGO, v.PLVNNMPS AS PRESTAMO, v.PLVNESTD AS ESTADO,
       (SELECT MAX(g.PGPRFCHA) FROM CRD.PGPR g
         WHERE g.PRSTCDGO = v.PRSTCDGO AND NVL(g.PGPRANUL, 0) = 0) AS FECHA_A_PONER
  FROM CRD.PLVN v
 WHERE v.PLVNFCUC IS NULL
   AND EXISTS (SELECT 1 FROM CRD.PGPR g WHERE g.PRSTCDGO = v.PRSTCDGO AND NVL(g.PGPRANUL, 0) = 0)
 ORDER BY v.PLVNCDGO;

-- 0.1 Respaldo (los ids que se tocan; el valor anterior es NULL en todos).
CREATE TABLE CRD.BKP_311_PLVN AS
SELECT v.PLVNCDGO FROM CRD.PLVN v
 WHERE v.PLVNFCUC IS NULL
   AND EXISTS (SELECT 1 FROM CRD.PGPR g WHERE g.PRSTCDGO = v.PRSTCDGO AND NVL(g.PGPRANUL, 0) = 0);

-- 1. El relleno.
UPDATE CRD.PLVN v
   SET v.PLVNFCUC = (SELECT TRUNC(MAX(g.PGPRFCHA)) FROM CRD.PGPR g
                      WHERE g.PRSTCDGO = v.PRSTCDGO AND NVL(g.PGPRANUL, 0) = 0)
 WHERE v.PLVNFCUC IS NULL
   AND EXISTS (SELECT 1 FROM CRD.PGPR g WHERE g.PRSTCDGO = v.PRSTCDGO AND NVL(g.PGPRANUL, 0) = 0);
-- Esperado: 16 filas actualizadas.
COMMIT;

-- 2. Control. Esperado: solo quedan vacias las 3 de prestamos nunca pagados (34, 63, 125).
SELECT v.PLVNCDGO, v.PLVNNMPS, v.PLVNFCUC FROM CRD.PLVN v WHERE v.PLVNFCUC IS NULL ORDER BY 1;

-- 3. REVERSO — COMENTADO
-- UPDATE CRD.PLVN v SET v.PLVNFCUC = NULL WHERE v.PLVNCDGO IN (SELECT b.PLVNCDGO FROM CRD.BKP_311_PLVN b);
-- COMMIT;  -- y luego: DROP TABLE CRD.BKP_311_PLVN;

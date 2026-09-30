-- =====================================================================================
-- 302 — CRD.PLVN: la revertida (id 1) cede el numero GR-046-2026 a la vigente (id 23)
-- FECHA: 2026-09-30 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- EL CASO (bloque 0.2 del 301, corrido por el usuario en produccion):
--   id 23 · prestamo 62439 · estado 2 LIQUIDADA (VIGENTE) · numero guardado "46"      (pelado)
--   id  1 · prestamo 62439 · estado 3 REVERTIDA           · ASOPREP-FCPC-CREDITO-GR-046-2026
-- El 301 asumia que la pelada era la revertida; aca es AL REVES, y por eso dijo PARAR.
--
-- REGLA (la misma del 301): el numero limpio es de la declaracion VIGENTE. La revertida queda
-- con sufijo -REV-{id}. Nada se borra; la revertida se sigue pudiendo reimprimir.
--
-- DESPUES DE ESTE SCRIPT: volver a correr el 301 COMPLETO, por bloques. Su 0.2 ya no tiene
-- que mostrar este choque, y sus bloques 1 y 2 dejan el id 23 (y las demas peladas) con el
-- compuesto limpio.
--
-- SQL PURO. Correr por bloques.
-- =====================================================================================

-- 0.1 Las dos filas, tal como estan. Esperado: exactamente estas dos, con estos estados y
--     numeros. Si algo es distinto, PARAR.
SELECT v.PLVNCDGO, v.PLVNNMPS, v.PLVNESTD, v.PLVNNMMM
  FROM CRD.PLVN v
 WHERE v.PLVNCDGO IN (1, 23)
 ORDER BY v.PLVNCDGO;

-- 0.2 Nadie mas usa el numero con sufijo que se va a poner. Esperado: 0.
SELECT COUNT(*) AS YA_EXISTE
  FROM CRD.PLVN v
 WHERE UPPER(TRIM(v.PLVNNMMM)) = 'ASOPREP-FCPC-CREDITO-GR-046-2026-REV-1';


-- 1. La revertida cede el numero. Las condiciones del WHERE son la red: si la fila no es la
--    que se espera, no toca nada (0 filas actualizadas → PARAR y avisar).
UPDATE CRD.PLVN v
   SET v.PLVNNMMM = 'ASOPREP-FCPC-CREDITO-GR-046-2026-REV-1'
 WHERE v.PLVNCDGO = 1
   AND v.PLVNESTD = 3
   AND UPPER(TRIM(v.PLVNNMMM)) = 'ASOPREP-FCPC-CREDITO-GR-046-2026';
-- Esperado: 1 fila actualizada.

COMMIT;


-- 2. Control. Esperado: id 1 con -REV-1; id 23 todavia con "46" (lo compone el 301).
SELECT v.PLVNCDGO, v.PLVNNMPS, v.PLVNESTD, v.PLVNNMMM
  FROM CRD.PLVN v
 WHERE v.PLVNCDGO IN (1, 23)
 ORDER BY v.PLVNCDGO;


-- 3. REVERSO — COMENTADO
-- UPDATE CRD.PLVN v SET v.PLVNNMMM = 'ASOPREP-FCPC-CREDITO-GR-046-2026'
--  WHERE v.PLVNCDGO = 1 AND v.PLVNNMMM = 'ASOPREP-FCPC-CREDITO-GR-046-2026-REV-1';
-- COMMIT;

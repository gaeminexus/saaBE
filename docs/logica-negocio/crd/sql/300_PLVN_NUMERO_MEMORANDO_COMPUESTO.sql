-- =====================================================================================
-- 300 — CRD.PLVN: el numero de memorando pasa al formato completo (D26)
-- FECHA: 2026-09-30 · EQUIPO: omen-saa-1 (CRD · EQUIPO B) · primer numero del rango 300-349
--
-- PARA QUE: desde la decision D26 el usuario escribe en pantalla solo el numero (46), y el
-- sistema lo compone como ASOPREP-FCPC-CREDITO-GR-046-2026. Las declaraciones grabadas ANTES
-- de ese cambio guardaron el numero pelado ("46") en PLVNNMMM, y asi se imprimen. Este script
-- las lleva al formato completo, con el anio de SU declaracion (PLVNFCDC), no el de hoy.
--
-- SOLO toca filas cuyo PLVNNMMM son solo digitos. Una fila ya compuesta, o con cualquier otro
-- texto, no se toca.
--
-- ⚠️ CUANDO: DESPUES del WAR que compone el numero. Si se corre antes, la pantalla seguiria
--    mandando el numero pelado y convivirian los dos formatos.
--
-- SQL PURO. Correr por bloques y revisar la salida antes de seguir.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS
-- =====================================================================================

-- 0.1 Lo que se va a cambiar: el antes y el despues, fila por fila. Leerlo.
SELECT v.PLVNCDGO, v.PLVNNMPS AS PRESTAMO, v.PLVNESTD AS ESTADO, v.PLVNFCDC,
       v.PLVNNMMM AS ANTES,
       'ASOPREP-FCPC-CREDITO-GR-' || LPAD(TRIM(v.PLVNNMMM), 3, '0') || '-'
         || TO_CHAR(NVL(v.PLVNFCDC, SYSDATE), 'YYYY') AS DESPUES
  FROM CRD.PLVN v
 WHERE REGEXP_LIKE(TRIM(v.PLVNNMMM), '^[0-9]+$')
 ORDER BY v.PLVNCDGO;

-- 0.2 ⛔ El nuevo valor NO puede chocar con uno que ya exista: hay indice unico sobre
--     UPPER(TRIM(PLVNNMMM)). Esperado: 0 filas. Si devuelve algo, PARAR: dos declaraciones
--     terminarian con el mismo memorando, y hay que decidir cual se renumera.
SELECT x.DESPUES, COUNT(*) AS VECES
  FROM (SELECT 'ASOPREP-FCPC-CREDITO-GR-' || LPAD(TRIM(v.PLVNNMMM), 3, '0') || '-'
                 || TO_CHAR(NVL(v.PLVNFCDC, SYSDATE), 'YYYY') AS DESPUES
          FROM CRD.PLVN v
         WHERE REGEXP_LIKE(TRIM(v.PLVNNMMM), '^[0-9]+$')
        UNION ALL
        SELECT UPPER(TRIM(v.PLVNNMMM))
          FROM CRD.PLVN v
         WHERE NOT REGEXP_LIKE(TRIM(v.PLVNNMMM), '^[0-9]+$')) x
 GROUP BY x.DESPUES
HAVING COUNT(*) > 1;

-- 0.3 Respaldo de lo que se toca, para el reverso.
CREATE TABLE CRD.BKP_300_PLVN AS
SELECT v.PLVNCDGO, v.PLVNNMMM
  FROM CRD.PLVN v
 WHERE REGEXP_LIKE(TRIM(v.PLVNNMMM), '^[0-9]+$');

SELECT COUNT(*) AS RESPALDADAS FROM CRD.BKP_300_PLVN;   -- tiene que ser igual a las filas del 0.1


-- =====================================================================================
-- 1. EL CAMBIO
-- =====================================================================================
UPDATE CRD.PLVN v
   SET v.PLVNNMMM = 'ASOPREP-FCPC-CREDITO-GR-' || LPAD(TRIM(v.PLVNNMMM), 3, '0') || '-'
                    || TO_CHAR(NVL(v.PLVNFCDC, SYSDATE), 'YYYY')
 WHERE REGEXP_LIKE(TRIM(v.PLVNNMMM), '^[0-9]+$');

COMMIT;


-- =====================================================================================
-- 2. CONTROLES POSTERIORES
-- =====================================================================================

-- 2.1 Ya no queda ningun numero pelado. Esperado: 0.
SELECT COUNT(*) AS PELADOS FROM CRD.PLVN v WHERE REGEXP_LIKE(TRIM(v.PLVNNMMM), '^[0-9]+$');

-- 2.2 Como quedaron. Esperado: todos con el formato ASOPREP-FCPC-CREDITO-GR-nnn-aaaa.
SELECT v.PLVNCDGO, v.PLVNNMPS AS PRESTAMO, v.PLVNESTD AS ESTADO, v.PLVNNMMM
  FROM CRD.PLVN v
 ORDER BY v.PLVNCDGO;


-- =====================================================================================
-- 3. REVERSO — COMENTADO
-- =====================================================================================
-- UPDATE CRD.PLVN v
--    SET v.PLVNNMMM = (SELECT b.PLVNNMMM FROM CRD.BKP_300_PLVN b WHERE b.PLVNCDGO = v.PLVNCDGO)
--  WHERE v.PLVNCDGO IN (SELECT b.PLVNCDGO FROM CRD.BKP_300_PLVN b);
-- COMMIT;
--
-- Cuando ya no haga falta:  DROP TABLE CRD.BKP_300_PLVN;

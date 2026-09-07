-- =====================================================================
-- COMPLETA lo que el e2-06 dejo a medias: la FK y el indice de RHH.CBEM
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- QUE PASO, reportado por el usuario desde produccion
--   El e2-06 se corrio y sus bloques 1 y 2 funcionaron: se borro BNCOCDGO,
--   se agrego BEXTCDGO, y hoy hay 19 cuentas apuntando a bancos externos.
--   PERO el control 0.2 devuelve una sola constraint R: FK_CBEM_MPLD.
--   FK_CBEM_BEXT NO EXISTE.
--
-- POR QUE FALLO, y estaba avisado
--   El BLOQUE 3 del e2-06 tiene el GRANT REFERENCES COMENTADO:
--
--       -- GRANT REFERENCES ON TSR.BEXT TO RHH;
--       ALTER TABLE RHH.CBEM ADD CONSTRAINT FK_CBEM_BEXT ...
--
--   Oracle NO considera los privilegios heredados por ROL al crear un
--   constraint: hace falta el GRANT directo, y ni siendo DBA alcanza. Sin el,
--   el ALTER falla con ORA-01031 y el CREATE INDEX de la linea siguiente
--   tambien puede no haber corrido.
--
--   Los bloques que BORRAN y AGREGAN columnas si pasaron. Por eso el script
--   "parecio" correr: la aplicacion funciona, la columna esta, y lo unico que
--   falta es la garantia de integridad, que no se nota hasta que falla.
--
--   ⚠️ A e2-07 se le promovio el GRANT a bloque ejecutable (commit ae317c3).
--   A e2-06 y e2-03 no. Es la misma leccion aplicada a un script de tres.
--
-- QUE HACE ESTE SCRIPT
--   Bloques 0 y 1: SOLO LECTURA. Diagnostico y control de huerfanos.
--   Bloques 2 a 4: DDL. Crean el GRANT, la FK y el indice.
--   Bloque 5: controles despues.
--   Reverso comentado al final.
--
-- ⛔ NO CORRER DE CORRIDO SIN LEER EL BLOQUE 1.
--   Con 19 filas ya cargadas y SIN FK, nada valido que esos BEXTCDGO existan
--   de verdad. Si hay uno solo huerfano, el ALTER del bloque 3 falla con
--   ORA-02298 y hay que limpiar el dato ANTES.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 -- DIAGNOSTICO. Solo lectura.
-- =====================================================================

-- 0.1 Estado de la columna. ESPERADO: BEXTCDGO presente, BNCOCDGO ausente.
SELECT column_name, data_type, nullable
  FROM all_tab_columns
 WHERE owner = 'RHH' AND table_name = 'CBEM'
   AND column_name IN ('BNCOCDGO','BEXTCDGO');

-- 0.2 Constraints R de CBEM. ESPERADO HOY: solo FK_CBEM_MPLD.
--     Si ya apareciera FK_CBEM_BEXT, este script ya se corrio: PARAR.
SELECT c.constraint_name, c.constraint_type, r.table_name AS referencia
  FROM all_constraints c
  LEFT JOIN all_constraints r
         ON r.owner = c.r_owner AND r.constraint_name = c.r_constraint_name
 WHERE c.owner = 'RHH' AND c.table_name = 'CBEM' AND c.constraint_type = 'R';

-- 0.3 ¿El indice existe? El e2-06 lo creaba en la linea siguiente a la FK, asi
--     que pudo no haber corrido tampoco.
--     ⚠️ Se filtra por TABLE_OWNER, no por OWNER: un CREATE INDEX sin prefijo
--     de schema deja el indice en el schema del ejecutor y seria invisible
--     filtrando por OWNER. Es la trampa que costo el e2-04.
SELECT owner, index_name, table_owner, table_name
  FROM all_indexes
 WHERE table_owner = 'RHH' AND table_name = 'CBEM';

-- 0.4 ¿Tiene el usuario que ejecuta el privilegio REFERENCES sobre TSR.BEXT?
--     Si devuelve 0 filas, el bloque 2 es OBLIGATORIO y lo tiene que correr
--     TSR o un DBA, no RHH.
SELECT grantee, owner, table_name, privilege
  FROM all_tab_privs
 WHERE owner = 'TSR' AND table_name = 'BEXT' AND privilege = 'REFERENCES';


-- =====================================================================
-- BLOQUE 1 -- ⭐ EL CONTROL QUE NO SE SALTEA: huerfanos.
--
--   Durante todo el tiempo que la tabla estuvo SIN FK, nada impidio grabar un
--   BEXTCDGO que no existe en TSR.BEXT. Estas 19 filas se cargaron
--   exactamente en esa ventana.
--
--   ESPERADO: 0 filas. Cada fila que salga hace fallar el bloque 3 con
--   ORA-02298 (parent keys not found) y hay que corregirla ANTES.
--
--   NULL no es huerfano: la columna es NULL-able y la FK lo permite.
-- =====================================================================
SELECT c.CBEMCDGO      AS ID_CUENTA,
       c.BEXTCDGO      AS BANCO_INEXISTENTE
  FROM RHH.CBEM c
 WHERE c.BEXTCDGO IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM TSR.BEXT b WHERE b.BEXTCDGO = c.BEXTCDGO);

-- 1.2 Panorama de las 19, para mirarlas de una. Util aunque 1.1 de vacio.
SELECT c.CBEMCDGO AS ID_CUENTA,
       c.BEXTCDGO AS ID_BANCO,
       b.BEXTNMBR AS BANCO,
       b.BEXTESTD AS ESTADO_BANCO
  FROM RHH.CBEM c
  LEFT JOIN TSR.BEXT b ON b.BEXTCDGO = c.BEXTCDGO
 ORDER BY c.CBEMCDGO;


-- =====================================================================
-- BLOQUE 2 -- EL GRANT. VA ANTES DE LA FK Y LO CORRE OTRO USUARIO.
--
--   ⛔ Esto lo ejecuta el dueño del schema TSR (o un DBA), NO el usuario de
--   la aplicacion. Es el paso que falto en el e2-06 y por eso quedo a medias.
--   Si el control 0.4 devolvio la fila, este bloque ya esta hecho: saltearlo.
-- =====================================================================

GRANT REFERENCES ON TSR.BEXT TO RHH;


-- =====================================================================
-- BLOQUE 3 -- LA FK
--   Correr SOLO si el bloque 1.1 devolvio 0 filas.
-- =====================================================================

ALTER TABLE RHH.CBEM ADD CONSTRAINT FK_CBEM_BEXT
    FOREIGN KEY (BEXTCDGO) REFERENCES TSR.BEXT (BEXTCDGO);


-- =====================================================================
-- BLOQUE 4 -- EL INDICE
--   Correr SOLO si el control 0.3 no lo mostro.
--   El prefijo de schema va tambien en el INDICE, no solo en la tabla: sin el,
--   el indice queda en el schema del ejecutor (trampa del e2-04).
-- =====================================================================

CREATE INDEX RHH.IX_CBEM_BEXT ON RHH.CBEM (BEXTCDGO);


-- =====================================================================
-- BLOQUE 5 -- CONTROLES DESPUES. Correr y LEER.
-- =====================================================================

-- 5.1 La FK existe y apunta a BEXT. ESPERADO: FK_CBEM_BEXT ... BEXT
SELECT c.constraint_name, c.constraint_type, c.status, r.table_name AS referencia
  FROM all_constraints c
  LEFT JOIN all_constraints r
         ON r.owner = c.r_owner AND r.constraint_name = c.r_constraint_name
 WHERE c.owner = 'RHH' AND c.table_name = 'CBEM' AND c.constraint_type = 'R';

-- 5.2 El indice quedo en el schema RHH. ESPERADO: OWNER = 'RHH'.
SELECT owner, index_name, table_owner, table_name
  FROM all_indexes
 WHERE table_owner = 'RHH' AND table_name = 'CBEM' AND index_name = 'IX_CBEM_BEXT';

-- 5.3 Las 19 cuentas siguen ahi. ESPERADO: 19 (o el numero que haya hoy).
SELECT COUNT(*) AS CUENTAS FROM RHH.CBEM;


-- =====================================================================
-- REVERSO -- comentado a proposito. Descomentar solo si hace falta.
-- =====================================================================
-- DROP INDEX RHH.IX_CBEM_BEXT;
-- ALTER TABLE RHH.CBEM DROP CONSTRAINT FK_CBEM_BEXT;
-- REVOKE REFERENCES ON TSR.BEXT FROM RHH;   -- lo corre TSR o el DBA

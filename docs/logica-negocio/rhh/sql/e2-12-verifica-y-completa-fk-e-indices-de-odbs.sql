-- =====================================================================
-- VERIFICA y COMPLETA lo que pudo quedar a medias del e2-03 / e2-04
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- POR QUE EXISTE
--   El usuario confirmo que el e2-03 se corrio completo. Se le cree, y aun asi
--   hay que mirarlo, por dos razones concretas:
--
--   1) EL PRECEDENTE. El e2-06 tambien estaba "corrido completamente", y lo
--      estaba: lo que fallo fue UN BLOQUE ADENTRO, en silencio, porque el
--      GRANT REFERENCES era un comentario. Oracle no acepta el privilegio
--      heredado por ROL al crear un constraint, asi que el ALTER murio con
--      ORA-01031 mientras el resto del script pasaba. Se descubrio 3 dias
--      despues, en produccion. El e2-03 tiene EXACTAMENTE la misma forma.
--
--      ⚠️ Puede que en e2-03 SI haya funcionado: muchas tablas viejas tienen
--      el GRANT concedido a PUBLIC, y SCP.PJRQ podria ser una de esas — a
--      diferencia de TSR.BEXT, que no lo tenia. Pero eso es plausible, no
--      verificado. Este script lo verifica.
--
--   2) EL E2-04 ARREGLO 2 DE 3. El e2-03 crea TRES indices sin prefijo de
--      schema, y el e2-04 solo corrigio dos:
--
--        IX_ODBS_EMPR_ANIO  sobre RHH.ODBS  -> corregido por e2-04
--        UQ_ODBS_VIVA       sobre RHH.ODBS  -> corregido por e2-04
--        IX_LQBS_ODBS       sobre RHH.LQBS  -> NO CORREGIDO  (e2-03 linea 164)
--
--      Un CREATE INDEX sin prefijo deja el indice en el schema del USUARIO QUE
--      EJECUTA, no en el de la tabla. Sigue funcionando como indice —Oracle lo
--      usa igual— pero queda en el lugar equivocado, es invisible si se filtra
--      por OWNER, y se va si alguna vez se toca ese usuario.
--
-- QUE HACE ESTE SCRIPT
--   BLOQUE 0: SOLO LECTURA. Diagnostico. Correr y LEER antes de seguir.
--   BLOQUES 1 a 3: DDL CONDICIONAL. Cada uno dice cuando corresponde correrlo
--                  y cuando saltearlo. NO se corren a ciegas.
--   BLOQUE 4: controles despues.
--   Reverso comentado al final.
--
-- ⛔ NO CORRER DE CORRIDO. El bloque 0 decide que hace falta y que no.
--    Si todo sale bien en el 0, este script no tiene nada que hacer y se cierra
--    ahi: es el mejor resultado posible.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 -- DIAGNOSTICO. Solo lectura. Correr y LEER.
-- =====================================================================

-- 0.1 ⭐ LA FK DE ODBS.
--     ESPERADO: una fila con FK_ODBS_PJRQ, ENABLED, referencia PJRQ.
--     Si NO aparece -> le paso lo mismo que al e2-06: correr bloques 1 y 2.
SELECT c.constraint_name, c.constraint_type, c.status,
       r.table_name AS REFERENCIA
  FROM all_constraints c
  LEFT JOIN all_constraints r
         ON r.owner = c.r_owner AND r.constraint_name = c.r_constraint_name
 WHERE c.owner = 'RHH' AND c.table_name = 'ODBS' AND c.constraint_type = 'R';

-- 0.2 ⭐ LOS INDICES DE LAS DOS TABLAS, CON SU DUEÑO.
--     ⚠️ El filtro va por TABLE_OWNER, NO por OWNER. Si se filtra por OWNER no
--     se ven justamente los que quedaron mal puestos — es la trampa que
--     origino el e2-04.
--
--     ESPERADO: los tres con OWNER = 'RHH'.
--       IX_ODBS_EMPR_ANIO  (ODBS)
--       UQ_ODBS_VIVA       (ODBS, UNIQUE)
--       IX_LQBS_ODBS       (LQBS)   <-- el sospechoso
--     Cualquiera con OWNER distinto de RHH -> correr el bloque 3 para ese.
SELECT owner AS DUENO_DEL_INDICE, index_name, uniqueness,
       table_owner, table_name
  FROM all_indexes
 WHERE table_owner = 'RHH' AND table_name IN ('ODBS','LQBS')
 ORDER BY table_name, index_name;

-- 0.3 ¿Existe el privilegio REFERENCES sobre SCP.PJRQ?
--     Si 0.1 fallo, esto dice si hace falta el bloque 1 o si el problema es
--     otro. Ojo: puede estar concedido a PUBLIC, y ahi la FK habria funcionado.
SELECT grantee, owner, table_name, privilege
  FROM all_tab_privs
 WHERE owner = 'SCP' AND table_name = 'PJRQ' AND privilege = 'REFERENCES';

-- 0.4 Contexto: ¿la tabla ODBS tiene datos? Informativo, no bloquea nada.
SELECT COUNT(*) AS FILAS_ODBS FROM RHH.ODBS;


-- =====================================================================
-- BLOQUE 1 -- EL GRANT
--   ⛔ CORRER SOLO SI el 0.1 no devolvio FK_ODBS_PJRQ.
--   ⛔ Lo ejecuta el dueño del schema SCP (o un DBA), NO el usuario de la
--      aplicacion. Es el paso que falto en el e2-06.
--   Si ya esta concedido, re-otorgarlo funciona en silencio: no falla.
-- =====================================================================

GRANT REFERENCES ON SCP.PJRQ TO RHH;


-- =====================================================================
-- BLOQUE 2 -- LA FK
--   ⛔ CORRER SOLO SI el 0.1 no devolvio FK_ODBS_PJRQ, y despues del bloque 1.
--
--   ⚠️ Si ODBS ya tiene filas (ver 0.4), este ALTER falla con ORA-02298 si
--   alguna tiene un PJRQCDGO que no existe en SCP.PJRQ. Es improbable —la
--   empresa se elige de un combo— pero si pasa, hay que limpiar el dato antes.
-- =====================================================================

ALTER TABLE RHH.ODBS ADD CONSTRAINT FK_ODBS_PJRQ
    FOREIGN KEY (PJRQCDGO) REFERENCES SCP.PJRQ (PJRQCDGO);


-- =====================================================================
-- BLOQUE 3 -- REUBICAR IX_LQBS_ODBS
--   ⛔ CORRER SOLO SI el 0.2 lo mostro con OWNER distinto de 'RHH'.
--
--   Mismo patron que uso el e2-04 con los otros dos: el DROP va SIN calificar
--   porque el indice pertenece al usuario que lo creo, que es el mismo que
--   corre esto. El CREATE va CON prefijo de schema, que es lo que faltaba.
--
--   ⚠️ Si el DROP falla con ORA-01418 ("el indice especificado no existe"),
--   significa que el indice esta en OTRO usuario: calificarlo con el owner que
--   mostro el 0.2  ->  DROP INDEX <owner>.IX_LQBS_ODBS;
--
--   Es un indice de RENDIMIENTO, no una regla de negocio: dropearlo un momento
--   no rompe nada, solo hace mas lenta alguna consulta mientras tanto. (No es
--   el caso de UQ_ODBS_VIVA, que si es regla de negocio — ese ya lo arreglo el
--   e2-04.)
-- =====================================================================

DROP INDEX IX_LQBS_ODBS;

CREATE INDEX RHH.IX_LQBS_ODBS ON RHH.LQBS (LQBSODBS);


-- =====================================================================
-- BLOQUE 4 -- CONTROLES DESPUES. Correr y LEER.
-- =====================================================================

-- 4.1 La FK existe y esta habilitada. ESPERADO: FK_ODBS_PJRQ ... ENABLED ... PJRQ
SELECT c.constraint_name, c.constraint_type, c.status,
       r.table_name AS REFERENCIA
  FROM all_constraints c
  LEFT JOIN all_constraints r
         ON r.owner = c.r_owner AND r.constraint_name = c.r_constraint_name
 WHERE c.owner = 'RHH' AND c.table_name = 'ODBS' AND c.constraint_type = 'R';

-- 4.2 ⭐ LOS TRES INDICES CON OWNER = 'RHH'. Es el control que cierra el tema.
SELECT owner AS DUENO_DEL_INDICE, index_name, uniqueness, table_name
  FROM all_indexes
 WHERE table_owner = 'RHH' AND table_name IN ('ODBS','LQBS')
 ORDER BY table_name, index_name;

-- 4.3 Nada se perdio. ESPERADO: el mismo numero que dio el 0.4.
SELECT COUNT(*) AS FILAS_ODBS FROM RHH.ODBS;


-- =====================================================================
-- REVERSO -- comentado a proposito. Descomentar solo si hace falta.
-- =====================================================================
-- DROP INDEX RHH.IX_LQBS_ODBS;
-- CREATE INDEX IX_LQBS_ODBS ON RHH.LQBS (LQBSODBS);   -- vuelve a quedar sin prefijo
-- ALTER TABLE RHH.ODBS DROP CONSTRAINT FK_ODBS_PJRQ;
-- REVOKE REFERENCES ON SCP.PJRQ FROM RHH;             -- lo corre SCP o el DBA

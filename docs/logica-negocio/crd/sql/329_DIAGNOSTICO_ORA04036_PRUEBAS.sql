-- =====================================================================================
-- 329 — DIAGNOSTICO y ARREGLO del ORA-04036 (PGA_AGGREGATE_LIMIT) en la base de pruebas .2.3
-- Equipo omen-saa-1 · 2026-10-14
--
-- Sintoma (log de WildFly de pruebas, 2026-10-14 08:26): al cargar UN PagoProgramado por id
-- (PGS.PGTR, "where pp1_0.PGTRCDGO=?") Hibernate arma un SELECT con ~117 LEFT JOIN y ~2.000
-- columnas, y Oracle corta con ORA-04036.
--
-- Por que en pruebas y no en produccion: es una busqueda por PK; con estadisticas Oracle la
-- resuelve con nested loops y casi sin PGA. Una base recien clonada por impdp puede quedar
-- SIN estadisticas (el paso de estadisticas se agrego al procedimiento recien en 19d390c6), y
-- sin ellas el optimizador puede elegir hash joins sobre tablas completas: con 117 joins eso
-- revienta la PGA. Ademas el contenedor es Oracle Database FREE, con memoria limitada.
--
-- Correr CONECTADO A LA BASE DE PRUEBAS (.2.3, FREEPDB1). NUNCA en produccion.
-- Bloques 1-3 solo leen. El bloque 4 escribe estadisticas (no toca datos).
-- =====================================================================================


-- BLOQUE 0 — (!) Confirmar que estoy en PRUEBAS.
SELECT SYS_CONTEXT('USERENV', 'CON_NAME')  AS PDB,
       SYS_CONTEXT('USERENV', 'SERVER_HOST') AS HOST,
       (SELECT PJRQNMBR FROM SCP.PJRQ WHERE PJRQCDGO = 1236) AS EMPRESA_1236
  FROM DUAL;
-- (!) EMPRESA_1236 tiene que decir "ASOPREP PRUEBAS". Si dice "ASOPREP" a secas: PARAR.


-- BLOQUE 1 — Parametros de memoria (los muestra; requiere ver V$PARAMETER).
SELECT NAME, DISPLAY_VALUE
  FROM V$PARAMETER
 WHERE NAME IN ('pga_aggregate_limit', 'pga_aggregate_target', 'sga_target', 'memory_target')
 ORDER BY NAME;


-- BLOQUE 2 — ¿Las tablas del join tienen estadisticas?
--   LAST_ANALYZED nulo o anterior al clon = sin estadisticas utiles.
SELECT OWNER, TABLE_NAME, NUM_ROWS, LAST_ANALYZED
  FROM ALL_TAB_STATISTICS
 WHERE (OWNER, TABLE_NAME) IN (('PGS','PGTR'), ('PGS','ANTP'), ('PGS','APLP'), ('PGS','FCTC'),
                               ('PGS','LQCC'), ('PGS','NTCC'), ('PGS','NTDC'), ('PGS','LTPG'),
                               ('PGS','PRDP'), ('PGS','GRPP'), ('CNT','ASNT'), ('CNT','PLNN'),
                               ('CNT','PRDO'), ('CNT','MYRZ'), ('TSR','TTLR'), ('TSR','EGRS'),
                               ('TSR','DTCH'), ('TSR','CNBC'), ('CBR','RTNC'), ('CBR','RTV2'),
                               ('SCP','PJRQ'), ('SCP','PGSP'))
   AND PARTITION_NAME IS NULL
 ORDER BY LAST_ANALYZED NULLS FIRST, OWNER, TABLE_NAME;


-- BLOQUE 3 — Cuantas tablas de cada esquema estan sin estadisticas.
SELECT OWNER, COUNT(*) AS TABLAS, SUM(CASE WHEN LAST_ANALYZED IS NULL THEN 1 ELSE 0 END) AS SIN_ESTADISTICAS
  FROM ALL_TAB_STATISTICS
 WHERE OWNER IN ('PGS', 'CNT', 'TSR', 'CBR', 'SCP', 'CRD', 'RHH', 'RPR')
   AND PARTITION_NAME IS NULL
 GROUP BY OWNER
 ORDER BY OWNER;


-- BLOQUE 4 — (ESCRIBE ESTADISTICAS) Recalcular. Solo si el bloque 2 o el 3 muestran huecos.
--   Tarda unos minutos por esquema. Despues, reintentar la operacion en la aplicacion.
BEGIN
  FOR e IN (SELECT COLUMN_VALUE AS ESQUEMA
              FROM TABLE(SYS.ODCIVARCHAR2LIST('PGS','CNT','TSR','CBR','SCP','CRD','RHH','RPR'))) LOOP
    DBMS_STATS.GATHER_SCHEMA_STATS(ownname => e.ESQUEMA, cascade => TRUE, degree => 2);
    DBMS_OUTPUT.PUT_LINE('estadisticas OK: ' || e.ESQUEMA);
  END LOOP;
END;
/
-- Volver a correr el bloque 3: SIN_ESTADISTICAS tiene que quedar en 0 (o casi).
--
-- Si DESPUES de esto el error sigue: el limite de memoria del contenedor es el problema.
-- Subir PGA_AGGREGATE_LIMIT (ALTER SYSTEM SET pga_aggregate_limit = 3G SCOPE=BOTH; desde
-- CDB$ROOT como SYSDBA) solo si el bloque 1 lo muestra bajo y la edicion lo permite; Oracle
-- Free tiene tope de RAM, asi que puede no alcanzar. Avisar al arbitro con la salida.
--
-- REVERSO: no aplica (las estadisticas no cambian datos).

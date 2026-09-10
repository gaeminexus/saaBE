--------------------------------------------------------------------------------
-- lap1-13 — DIAGNOSTICO del arbol de permisos SCP.PJRQ (PGSPCDGO = 11)
--
-- Equipo: lap-saa-1 (laptop)            Fecha: 2026-09-10
-- Frente: activacion del modulo de seguridades para SAA
--
-- *** SOLO LECTURA. NO MODIFICA NADA. NO HAY NINGUN DML EN ESTE ARCHIVO. ***
--
-- Para que sirve: antes de borrar los sistemas PCC / CID / ACT y de insertar el
-- arbol de SAA con ids quemados desde el 1253, hay cinco cosas que NO se pueden
-- deducir leyendo el codigo Java y que, si se asumen mal, rompen en produccion:
--
--   A) Que tablas referencian SCP.PJRQ. Si hay FK, el DELETE falla; si NO hay FK,
--      es peor: los permisos ya asignados quedan apuntando a codigos que despues
--      van a ser OTRA pantalla, y conceden algo distinto sin ningun error.
--   B) Que columnas tiene PJRQ de verdad. El CSV exportado trae 6; si la tabla
--      tiene una setima NOT NULL, todos los INSERT mueren con ORA-01400.
--   C) Cual es el MAX(PJRQCDGO) real de TODA la tabla — no solo del sistema 11 —
--      para confirmar que arrancar en 1253 no pisa nada.
--   D) Cuanto mide PJRQNMBR, para no pasarme de largo en 700 nombres.
--   E) Que valor tienen hoy los dos rubros que gobiernan esto: el interruptor
--      (alterno 7 / detalle 1) y el "permiso de ingreso" que UsuarioDaoServiceImpl
--      lee en cada login por sucursal.
--
-- Como se corre: de arriba a abajo, un bloque por vez, en el cliente JDBC.
-- Pegame el resultado de los 10 bloques (o los que devuelvan algo raro).
--------------------------------------------------------------------------------


--------------------------------------------------------------------------------
-- CONTROL 1 — Las jerarquias que existen. Confirmar que 11 es la del arbol.
--
-- Esperado: una fila con PGSPCDGO = 11 y un nombre tipo "NIVELES DE SISTEMA".
-- Si hay MAS de una jerarquia que parezca de permisos, PARAR y avisarme:
-- estaria borrando de la equivocada.
--------------------------------------------------------------------------------
SELECT PGSPCDGO,
       PGSPNMBR,
       PGSPNVLL,
       PGSPCDPD,
       PGSPDSCP,
       PGSPCDAL
  FROM SCP.PGSP
 ORDER BY PGSPCDGO;


--------------------------------------------------------------------------------
-- CONTROL 2 — TODAS las columnas reales de SCP.PJRQ.
--
-- Esto es lo que decide si el INSERT que te voy a dar funciona o revienta.
-- Miro dos cosas:
--   * Si aparece alguna columna que NO esta en el CSV (el CSV trae PJRQCDGO,
--     PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD, PJRQINGR).
--   * Si esa columna extra tiene NULLABLE = 'N' y no tiene DATA_DEFAULT.
--     Ahi es obligatoria y el INSERT la necesita.
--
-- Es exactamente el ORA-01400 que le costo una correccion en produccion al
-- equipo omen-saa-2 con CPNMTPCL: nombraron 7 columnas de 29.
--------------------------------------------------------------------------------
SELECT COLUMN_ID,
       COLUMN_NAME,
       DATA_TYPE,
       DATA_LENGTH,
       DATA_PRECISION,
       DATA_SCALE,
       NULLABLE,
       DATA_DEFAULT
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'SCP'
   AND TABLE_NAME = 'PJRQ'
 ORDER BY COLUMN_ID;


--------------------------------------------------------------------------------
-- CONTROL 3 — Constraints propias de PJRQ (PK, UNIQUE, CHECK, NOT NULL).
--
-- Busco sobre todo un UNIQUE sobre (PGSPCDGO, PJRQNMBR) o similar: si existe,
-- no puedo repetir nombres de pantalla dentro del mismo sistema, y en el arbol
-- de SAA hay nombres que se repiten en modulos distintos ("CONSULTA", "INGRESO").
--------------------------------------------------------------------------------
SELECT c.CONSTRAINT_NAME,
       c.CONSTRAINT_TYPE,
       c.STATUS,
       c.SEARCH_CONDITION,
       cc.COLUMN_NAME,
       cc.POSITION
  FROM ALL_CONSTRAINTS c
  LEFT JOIN ALL_CONS_COLUMNS cc
         ON cc.OWNER = c.OWNER
        AND cc.CONSTRAINT_NAME = c.CONSTRAINT_NAME
 WHERE c.OWNER = 'SCP'
   AND c.TABLE_NAME = 'PJRQ'
 ORDER BY c.CONSTRAINT_TYPE, c.CONSTRAINT_NAME, cc.POSITION;


--------------------------------------------------------------------------------
-- CONTROL 4 — *** EL BLOQUE QUE MAS IMPORTA ***
-- Quien apunta a SCP.PJRQ: todas las FK de cualquier schema hacia esta tabla.
--
-- Tres resultados posibles y cada uno cambia el plan:
--
--   a) DEVUELVE FILAS con DELETE_RULE = 'NO ACTION'  -> el DELETE va a fallar
--      con ORA-02292 hasta que borre primero las filas hijas. Es el caso BUENO:
--      la base me avisa.
--   b) DEVUELVE FILAS con DELETE_RULE = 'CASCADE'    -> el DELETE se lleva los
--      permisos asignados solo. Tambien esta bien, pero hay que saberlo ANTES
--      de correrlo, no despues.
--   c) NO DEVUELVE NADA                              -> el caso PELIGROSO: las
--      asignaciones de permisos referencian PJRQCDGO sin FK declarada. El
--      DELETE no falla, y quedan permisos huerfanos apuntando a codigos que
--      el INSERT nuevo va a reutilizar para OTRAS pantallas.
--      Si es (c), el CONTROL 5 encuentra las tablas igual, por nombre de columna.
--------------------------------------------------------------------------------
SELECT c.OWNER          AS SCHEMA_HIJO,
       c.TABLE_NAME     AS TABLA_HIJA,
       c.CONSTRAINT_NAME,
       c.DELETE_RULE,
       c.STATUS,
       cc.COLUMN_NAME   AS COLUMNA_HIJA,
       cc.POSITION
  FROM ALL_CONSTRAINTS c
  JOIN ALL_CONSTRAINTS p
    ON p.OWNER = c.R_OWNER
   AND p.CONSTRAINT_NAME = c.R_CONSTRAINT_NAME
  JOIN ALL_CONS_COLUMNS cc
    ON cc.OWNER = c.OWNER
   AND cc.CONSTRAINT_NAME = c.CONSTRAINT_NAME
 WHERE c.CONSTRAINT_TYPE = 'R'
   AND p.OWNER = 'SCP'
   AND p.TABLE_NAME = 'PJRQ'
 ORDER BY c.OWNER, c.TABLE_NAME, cc.POSITION;


--------------------------------------------------------------------------------
-- CONTROL 5 — La red de seguridad del CONTROL 4: buscar por NOMBRE de columna.
--
-- Cualquier tabla del sistema que tenga una columna llamada PJRQCDGO (o que
-- termine en PJRQ) esta referenciando el arbol, tenga FK declarada o no.
-- Asi aparecen las tablas de "ASIGNACION DE PERMISOS" y "ASIGNACION DE
-- EXCEPCIONES" aunque nadie haya declarado la integridad referencial.
--
-- OJO: PJRQ se llama igual en todas las jerarquias (PGSPCDGO 1..N), asi que una
-- tabla que aparezca aca puede estar guardando permisos de OTROS sistemas
-- ademas del 11. El CONTROL 7 separa eso.
--------------------------------------------------------------------------------
SELECT OWNER,
       TABLE_NAME,
       COLUMN_NAME,
       DATA_TYPE,
       NULLABLE
  FROM ALL_TAB_COLUMNS
 WHERE (COLUMN_NAME LIKE '%PJRQ%' OR COLUMN_NAME = 'PJRQCDGO')
   AND NOT (OWNER = 'SCP' AND TABLE_NAME = 'PJRQ')
 ORDER BY OWNER, TABLE_NAME, COLUMN_NAME;


--------------------------------------------------------------------------------
-- CONTROL 6 — MAX(PJRQCDGO) de TODA la tabla, no solo del sistema 11.
--
-- Vos me dijiste que el proximo id es el 1253. Esto lo confirma o lo desmiente.
-- El CSV que me pasaste llega hasta 1220, pero es solo PGSPCDGO = 11: si otra
-- jerarquia tiene codigos mas altos, arrancar en 1253 pisa filas ajenas.
--
-- Esperado: MAXIMO_GLOBAL menor a 1253. Si sale 1253 o mas, PARAR y decirme
-- desde que numero arranco de verdad.
--------------------------------------------------------------------------------
SELECT MAX(PJRQCDGO)                                              AS MAXIMO_GLOBAL,
       MAX(CASE WHEN PGSPCDGO = 11 THEN PJRQCDGO END)             AS MAXIMO_SISTEMA_11,
       COUNT(*)                                                   AS FILAS_TOTALES,
       COUNT(CASE WHEN PGSPCDGO = 11 THEN 1 END)                  AS FILAS_SISTEMA_11,
       COUNT(DISTINCT PGSPCDGO)                                   AS JERARQUIAS_DISTINTAS,
       MAX(LENGTH(PJRQNMBR))                                      AS LARGO_MAX_NOMBRE_USADO
  FROM SCP.PJRQ;


--------------------------------------------------------------------------------
-- CONTROL 7 — Cuantos nodos cuelgan de cada sistema de nivel 1 del arbol 11.
--
-- Es el inventario de lo que se va a borrar y de lo que se conserva.
--
-- Esperado, segun el CSV que me pasaste:
--   93  RAIZ SISTEMAS          -> SE CONSERVA (es la raiz; sin ella no dibuja nada)
--   42  SEGURIDAD Y AUDITORIA  -> SE CONSERVA (~60 nodos: es el arbol del propio
--                                 modulo de seguridades, con el que administras esto)
--   482 PCC                    -> SE BORRA
--   315 CID                    -> SE BORRA
--   979 ACT                    -> SE BORRA
--
-- Si aparece un sexto sistema que no esta en esta lista, PARAR y avisarme.
--------------------------------------------------------------------------------
SELECT raiz.PJRQCDGO                       AS CODIGO_SISTEMA,
       raiz.PJRQNMBR                       AS NOMBRE_SISTEMA,
       raiz.PJRQNVLL                       AS NIVEL,
       raiz.PJRQCDPD                       AS PADRE,
       (SELECT COUNT(*)
          FROM SCP.PJRQ d
         WHERE d.PGSPCDGO = 11
         START WITH d.PJRQCDGO = raiz.PJRQCDGO
       CONNECT BY PRIOR d.PJRQCDGO = d.PJRQCDPD
                    AND d.PGSPCDGO = 11)   AS NODOS_EN_EL_SUBARBOL
  FROM SCP.PJRQ raiz
 WHERE raiz.PGSPCDGO = 11
   AND (raiz.PJRQCDPD = 93 OR raiz.PJRQCDGO = 93)
 ORDER BY raiz.PJRQCDGO;


--------------------------------------------------------------------------------
-- CONTROL 8 — Nodos huerfanos y nodos sueltos del arbol 11.
--
-- Un nodo cuyo PJRQCDPD apunta a un codigo que no existe no se dibuja en la
-- pantalla, pero SI ocupa un codigo y SI puede tener permisos asignados.
-- Si hay muchos, el "borrar todo menos 93 y 42" los deja adentro sin querer.
--------------------------------------------------------------------------------
SELECT h.PJRQCDGO,
       h.PJRQNMBR,
       h.PJRQNVLL,
       h.PJRQCDPD  AS PADRE_INEXISTENTE
  FROM SCP.PJRQ h
 WHERE h.PGSPCDGO = 11
   AND h.PJRQCDPD <> 0
   AND NOT EXISTS (SELECT 1
                     FROM SCP.PJRQ p
                    WHERE p.PGSPCDGO = h.PGSPCDGO
                      AND p.PJRQCDGO = h.PJRQCDPD)
 ORDER BY h.PJRQCDGO;


--------------------------------------------------------------------------------
-- CONTROL 9 — El interruptor y el permiso de ingreso, que viven en RUBROS.
--
-- Dos filas, y las dos gobiernan el comportamiento del sistema entero:
--
--   * Rubro alterno 7 / detalle alterno 1  -> el interruptor que vos mencionaste.
--     '0' = NO se validan permisos. '1' = SI se validan.
--     Confirmar que hoy esta en '0' antes de que empecemos: si ya esta en '1',
--     el dia que borre PCC/CID/ACT alguien puede quedarse afuera de una pantalla.
--
--     *** OJO CON ESTE, QUE NO ES DONDE PARECE ***
--     Vos me dijiste "si en la DESCRIPCION tiene un 0...". El unico lector que
--     existe en el backend es DetalleRubroDaoServiceImpl:100-104, y su JPQL
--     devuelve  t.valorAlfanumerico  ->  la columna PDTRVLRV, NO PDTRDSCR.
--     Si el 0/1 esta escrito en la descripcion y el codigo lee PDTRVLRV, el
--     interruptor no va a funcionar y no va a dar ningun error: simplemente va
--     a leer null o vacio. Por eso el bloque trae las DOS columnas.
--
--   * Rubro alterno 15 / detalle alterno 99 (MODULO_SISTEMA / INGRESO) -> guarda
--     un PJRQCDGO quemado en la base. Lo lee UsuarioDaoServiceImpl:151 dentro de
--     validaUsuarioSucursal, en cada login por sucursal, para preguntarle al SP
--     si el usuario tiene permiso de INGRESO al sistema.
--     Hoy el frontend NO usa ese login (usa validaUsuario, el simple), asi que
--     no rompe nada todavia. Pero es una trampa dormida: el dia que se active la
--     validacion por sucursal, si ese codigo apunta a un nodo que borramos,
--     NADIE ENTRA AL SISTEMA. Necesito saber a que numero apunta hoy.
--
-- Nombres de tabla y columna copiados de las entidades JPA reales
-- (model/scp/Rubro.java y model/scp/DetalleRubro.java), NO de memoria:
-- la tabla de rubros se llama PRBR y sus columnas son PRBRCDGO / PRBRALTR /
-- PRBRDSCR. No existe PRBRNMBR — es el error que el equipo omen-saa-2 escribio
-- dos veces y dejo anotado en el §28 de su documento de estado.
--------------------------------------------------------------------------------
SELECT r.PRBRCDGO,
       r.PRBRALTR   AS RUBRO_ALTERNO,
       r.PRBRDSCR   AS RUBRO_DESCRIPCION,
       d.PDTRCDGO,
       d.PDTRALTR   AS DETALLE_ALTERNO,
       d.PDTRDSCR   AS DETALLE_DESCRIPCION,
       d.PDTRVLRV   AS DETALLE_VALOR_ALFANUMERICO,
       d.PDTRVLRN   AS DETALLE_VALOR_NUMERICO,
       d.PDTRESTD   AS DETALLE_ESTADO
  FROM SCP.PRBR r
  JOIN SCP.PDTR d
    ON d.PRBRCDGO = r.PRBRCDGO
 WHERE (r.PRBRALTR = 7  AND d.PDTRALTR = 1)
    OR (r.PRBRALTR = 15 AND d.PDTRALTR = 99)
 ORDER BY r.PRBRALTR, d.PDTRALTR;


--------------------------------------------------------------------------------
-- CONTROL 10 — Generador: emite las consultas de conteo del CONTROL 5.
--
-- No cuenta nada por si mismo: DEVUELVE TEXTO. Cada fila del resultado es un
-- SELECT listo para copiar y pegar, uno por cada tabla que referencia PJRQ.
-- Existe porque no puedo escribir esas consultas a mano sin saber primero como
-- se llaman las tablas, y ese nombre sale recien del CONTROL 4 / CONTROL 5.
--
-- Corre este bloque, copia las lineas de la columna SENTENCIA, pegalas abajo y
-- corrélas. Con eso sabemos exactamente cuantas asignaciones de permisos se
-- pierden al borrar PCC / CID / ACT, y cuantas sobreviven en el subarbol de
-- SEGURIDAD Y AUDITORIA.
--------------------------------------------------------------------------------
SELECT 'SELECT ''' || c.OWNER || '.' || c.TABLE_NAME || ''' AS TABLA, '
       || 'COUNT(*) AS FILAS_TOTALES, '
       || 'COUNT(CASE WHEN x.' || c.COLUMN_NAME || ' IN (SELECT PJRQCDGO FROM SCP.PJRQ WHERE PGSPCDGO = 11 START WITH PJRQCDGO IN (482,315,979) CONNECT BY PRIOR PJRQCDGO = PJRQCDPD) THEN 1 END) AS APUNTAN_A_LO_QUE_SE_BORRA, '
       || 'COUNT(CASE WHEN x.' || c.COLUMN_NAME || ' IN (SELECT PJRQCDGO FROM SCP.PJRQ WHERE PGSPCDGO = 11 START WITH PJRQCDGO = 42 CONNECT BY PRIOR PJRQCDGO = PJRQCDPD) THEN 1 END) AS APUNTAN_A_SEGURIDAD '
       || 'FROM ' || c.OWNER || '.' || c.TABLE_NAME || ' x;'
       AS SENTENCIA
  FROM ALL_TAB_COLUMNS c
 WHERE c.COLUMN_NAME LIKE '%PJRQ%'
   AND NOT (c.OWNER = 'SCP' AND c.TABLE_NAME = 'PJRQ')
 ORDER BY c.OWNER, c.TABLE_NAME;


--------------------------------------------------------------------------------
-- FIN DEL DIAGNOSTICO.
--
-- No hay bloque de reverso porque no hay nada que revertir: este archivo no
-- ejecuta un solo DML ni DDL. Es seguro correrlo de corrido.
--
-- Con los resultados de los 10 bloques escribo:
--   * lap1-14 — el DELETE de PCC / CID / ACT y de sus permisos asignados,
--               con sus controles antes y despues y su reverso comentado.
--   * lap1-15 — el INSERT del arbol de SAA con ids quemados desde el 1253.
--------------------------------------------------------------------------------

--------------------------------------------------------------------------------
-- lap1-14 — CHEQUEO REFERENCIAL CONSOLIDADO + LA SECUENCIA DE PJRQ
--
-- Equipo: lap-saa-1 (laptop)            Fecha: 2026-09-10
-- Frente: activacion del modulo de seguridades para SAA
-- Continua a: lap1-13 (ya corrido; sus resultados estan analizados)
--
-- *** SOLO LECTURA sobre los datos del sistema. ***
-- Crea UNA tabla de trabajo propia (SCP.LAP1_CHK_REFS) para poder mostrar el
-- resultado; el DROP esta al final, comentado.
--
--------------------------------------------------------------------------------
-- POR QUE EXISTE ESTE ARCHIVO, Y ES LO MAS IMPORTANTE QUE SALIO DEL lap1-13
--
-- *** SCP.PJRQ NO ES LA TABLA DEL ARBOL DE PERMISOS. ***
-- *** ES TAMBIEN LA TABLA DE EMPRESAS Y DE USUARIOS DEL SISTEMA. ***
--
-- Verificado en las entidades JPA, no deducido:
--   com.saa.model.scp.Empresa  -> @Table(name = "PJRQ", schema = "SCP")
--   com.saa.model.scp.Usuario  -> @Table(name = "PJRQ", schema = "SCP")
--
-- SCP.PGSP resulto ser el arbol de pantallas del propio modulo de seguridades, y
-- cada una de sus hojas define UN TIPO DE JERARQUIA que se guarda en PJRQ,
-- discriminada por PGSPCDGO:
--
--   PGSPCDGO = 5   EMPRESA (estructura legal)
--   PGSPCDGO = 6   ESTRUCTURA ORGANIZACIONAL
--   PGSPCDGO = 9   USUARIO  <- los usuarios del sistema viven aca
--   PGSPCDGO = 11  NIVELES DEL SISTEMA  <- EL ARBOL DE PERMISOS, el nuestro
--   PGSPCDGO = 12  EMPRESA tal como se ingreso en el sistema
--   PGSPCDGO = 17  AREA
--   PGSPCDGO = 18  NIVEL
--
-- El lap1-13 conto 786 filas en 8 jerarquias distintas; 742 de ellas son del 11.
--
-- CONSECUENCIA OPERATIVA, y no admite descuido:
--   *** NINGUN DELETE SOBRE SCP.PJRQ PUEDE ESCRIBIRSE SIN "PGSPCDGO = 11". ***
-- Un DELETE mas ancho no borra "sistemas viejos de permisos": borra empresas y
-- usuarios, y con ellos revienta media base por integridad referencial. Las 48
-- FK que el CONTROL 4 del lap1-13 encontro apuntando a PJRQ se llaman
-- FK_..._EMPRESA y FK_..._USUARIO precisamente por esto: son la empresa y el
-- usuario de cada caja chica, cada pago, cada asiento.
--------------------------------------------------------------------------------


--------------------------------------------------------------------------------
-- CONTROL 1 — *** LA SECUENCIA EXISTE, Y ESO CAMBIA EL PLAN ***
--
-- Se habia asumido que PJRQ no maneja secuencia y que por eso se podian quemar
-- los ids desde el 1253. La primera mitad es falsa:
--
--   Empresa.java:19   @SequenceGenerator(sequenceName = "SCP.SQ_PJRQCDGO")
--   Usuario.java:29   @SequenceGenerator(sequenceName = "SCP.SQ_PJRQCDGO")
--
-- Los dos con GenerationType.SEQUENCE. O sea: cada vez que alguien crea un
-- USUARIO o una EMPRESA desde SAA, el id sale de SCP.SQ_PJRQCDGO.
--
-- Quemar 700 ids desde el 1253 SIN adelantar la secuencia deja la secuencia por
-- debajo de lo usado, y entonces:
--   *** EL PROXIMO USUARIO O EMPRESA QUE SE CREE MUERE CON ORA-00001, ***
--   *** EN UNA PANTALLA QUE NO TIENE NADA QUE VER CON PERMISOS. ***
--
-- Este control dice desde donde arranca hoy. LAST_NUMBER es el proximo valor que
-- la secuencia va a entregar (con CACHE, es el proximo bloque cacheado).
--
-- Esperado: LAST_NUMBER cerca de 1253. El lap1-13 midio MAX(PJRQCDGO) = 1252
-- sobre TODA la tabla, asi que el 1253 que dio el usuario es correcto — pero hay
-- que adelantar la secuencia despues del INSERT, y eso va en el lap1-15.
--------------------------------------------------------------------------------
SELECT SEQUENCE_OWNER,
       SEQUENCE_NAME,
       MIN_VALUE,
       MAX_VALUE,
       INCREMENT_BY,
       CACHE_SIZE,
       LAST_NUMBER
  FROM ALL_SEQUENCES
 WHERE SEQUENCE_OWNER = 'SCP'
   AND SEQUENCE_NAME = 'SQ_PJRQCDGO';


--------------------------------------------------------------------------------
-- CONTROL 2 — Qué se va a borrar, exactamente, y cuánto.
--
-- Los tres subarboles a eliminar, contados de nuevo con el mismo CONNECT BY que
-- va a usar el DELETE del lap1-15. Si estos numeros no coinciden con el CONTROL 7
-- del lap1-13 (PCC 325, CID 150, ACT 191 = 666), PARAR: algo cambio en el medio.
--
-- Y confirma lo que NO se toca: la raiz 93, el subarbol 42 de SEGURIDAD Y
-- AUDITORIA (75 nodos), y las otras 7 jerarquias (empresas, usuarios, areas...).
--------------------------------------------------------------------------------
SELECT 'A BORRAR — PCC/CID/ACT del arbol 11'          AS QUE,
       COUNT(*)                                        AS FILAS
  FROM SCP.PJRQ
 WHERE PGSPCDGO = 11
 START WITH PJRQCDGO IN (482, 315, 979)
CONNECT BY PRIOR PJRQCDGO = PJRQCDPD
       AND PGSPCDGO = 11
UNION ALL
SELECT 'SE CONSERVA — subarbol SEGURIDAD Y AUDITORIA',
       COUNT(*)
  FROM SCP.PJRQ
 WHERE PGSPCDGO = 11
 START WITH PJRQCDGO = 42
CONNECT BY PRIOR PJRQCDGO = PJRQCDPD
       AND PGSPCDGO = 11
UNION ALL
SELECT 'SE CONSERVA — la raiz 93',
       COUNT(*)
  FROM SCP.PJRQ
 WHERE PGSPCDGO = 11 AND PJRQCDGO = 93
UNION ALL
SELECT 'NI SE MIRA — empresas, usuarios y demas jerarquias',
       COUNT(*)
  FROM SCP.PJRQ
 WHERE PGSPCDGO <> 11;


--------------------------------------------------------------------------------
-- CONTROL 3 — La restriccion que va a condicionar los nombres del arbol nuevo.
--
-- El lap1-13 encontro UN_PJRQ_01 UNIQUE sobre (PGSPCDGO, PJRQNMBR, PJRQCDPD).
-- Traducido: DOS HERMANOS NO PUEDEN LLAMARSE IGUAL.
--
-- No es un detalle: en SAA se repiten "CONSULTA", "INGRESO", "PARAMETRIZACION" y
-- "REPORTES" en varios modulos. Entre modulos distintos no hay problema (padres
-- distintos); el problema seria dos hijos con el mismo nombre bajo el MISMO
-- padre, y eso hay que evitarlo al generar el arbol.
--
-- Este control lo comprueba sobre lo que ya existe, para confirmar que la
-- restriccion se viene respetando y que no hay filas raras.
-- Esperado: NINGUNA FILA.
--------------------------------------------------------------------------------
SELECT PGSPCDGO,
       PJRQCDPD  AS PADRE,
       PJRQNMBR  AS NOMBRE_REPETIDO,
       COUNT(*)  AS VECES
  FROM SCP.PJRQ
 GROUP BY PGSPCDGO, PJRQCDPD, PJRQNMBR
HAVING COUNT(*) > 1
 ORDER BY 1, 2, 3;


--------------------------------------------------------------------------------
-- CONTROL 4 — *** EL CHEQUEO QUE FALTA: quien apunta a lo que vamos a borrar ***
--
-- El CONTROL 10 del lap1-13 GENERABA ~90 sentencias sueltas para correr a mano.
-- Fue un mal diseño de mi parte: son 90 pegadas y 90 resultados que leer.
-- Esto hace lo mismo en un solo paso.
--
-- Que hace: recorre TODAS las columnas numericas del diccionario que se llamen
-- %PJRQ%, arma la consulta dinamicamente, y cuenta cuantas filas de cada una
-- apuntan a un nodo que el lap1-15 va a borrar.
--
-- Por que importa: las 48 FK del lap1-13 protegen a sus tablas (el DELETE
-- fallaria con ORA-02292 y no borraria nada, que es el caso bueno). Pero hay
-- ~50 columnas %PJRQ% SIN FK declarada, y esas no avisan: el DELETE pasa, y
-- quedan apuntando a un codigo que el INSERT nuevo va a reutilizar para OTRA
-- pantalla. Un permiso viejo concediendo algo nuevo, sin un solo error.
--
-- Se excluyen a proposito:
--   * las columnas VARCHAR2 llamadas PJRQNMBR (son nombres, no referencias);
--   * SCP.PJRQ (es la tabla origen);
--   * SCP.HTE_PJRQ (es la tabla de historial de PJRQ: sus columnas SON la fila
--     historizada, no una referencia a otra fila).
--
-- COMO SE CORRE: los tres pasos, en orden. El PASO 2 es un bloque PL/SQL —
-- seleccionalo entero y ejecutalo como UNA sola sentencia, no linea por linea.
--------------------------------------------------------------------------------

-- PASO 1 de 3 — tabla de trabajo donde el bloque deja el resultado.
CREATE TABLE SCP.LAP1_CHK_REFS (
  ESQUEMA          VARCHAR2(128),
  TABLA            VARCHAR2(128),
  COLUMNA          VARCHAR2(128),
  TIENE_FK_A_PJRQ  VARCHAR2(2),
  FILAS_APUNTANDO  NUMBER,
  ERROR_AL_MEDIR   VARCHAR2(400)
);

-- PASO 2 de 3 — llena la tabla. Seleccionar el bloque COMPLETO y ejecutarlo de una.
DECLARE
  v_cuenta   NUMBER;
  v_sql      VARCHAR2(4000);
  v_tiene_fk VARCHAR2(2);
BEGIN
  FOR c IN (
      SELECT t.OWNER, t.TABLE_NAME, t.COLUMN_NAME
        FROM ALL_TAB_COLUMNS t
       WHERE t.COLUMN_NAME LIKE '%PJRQ%'
         AND t.DATA_TYPE = 'NUMBER'
         AND NOT (t.OWNER = 'SCP' AND t.TABLE_NAME IN ('PJRQ', 'HTE_PJRQ', 'LAP1_CHK_REFS'))
  ) LOOP

    -- ¿esta columna tiene FK declarada contra SCP.PJRQ?
    SELECT CASE WHEN COUNT(*) > 0 THEN 'SI' ELSE 'NO' END
      INTO v_tiene_fk
      FROM ALL_CONSTRAINTS  fk
      JOIN ALL_CONS_COLUMNS fkc ON fkc.OWNER = fk.OWNER
                               AND fkc.CONSTRAINT_NAME = fk.CONSTRAINT_NAME
      JOIN ALL_CONSTRAINTS  p   ON p.OWNER = fk.R_OWNER
                               AND p.CONSTRAINT_NAME = fk.R_CONSTRAINT_NAME
     WHERE fk.CONSTRAINT_TYPE = 'R'
       AND p.OWNER = 'SCP' AND p.TABLE_NAME = 'PJRQ'
       AND fkc.OWNER = c.OWNER
       AND fkc.TABLE_NAME = c.TABLE_NAME
       AND fkc.COLUMN_NAME = c.COLUMN_NAME;

    v_sql := 'SELECT COUNT(*) FROM ' || c.OWNER || '.' || c.TABLE_NAME || ' x '
          || ' WHERE x.' || c.COLUMN_NAME || ' IN ('
          || '   SELECT PJRQCDGO FROM SCP.PJRQ WHERE PGSPCDGO = 11 '
          || '    START WITH PJRQCDGO IN (482, 315, 979) '
          || '  CONNECT BY PRIOR PJRQCDGO = PJRQCDPD AND PGSPCDGO = 11)';

    BEGIN
      EXECUTE IMMEDIATE v_sql INTO v_cuenta;
      INSERT INTO SCP.LAP1_CHK_REFS
        (ESQUEMA, TABLA, COLUMNA, TIENE_FK_A_PJRQ, FILAS_APUNTANDO, ERROR_AL_MEDIR)
      VALUES (c.OWNER, c.TABLE_NAME, c.COLUMN_NAME, v_tiene_fk, v_cuenta, NULL);
    EXCEPTION
      WHEN OTHERS THEN
        -- Una tabla sin permiso de lectura o una vista rota NO puede quedar como
        -- "0 filas": eso seria indistinguible de "medido y esta limpio". Se
        -- registra el error para que se vea que esa columna NO se midio.
        INSERT INTO SCP.LAP1_CHK_REFS
          (ESQUEMA, TABLA, COLUMNA, TIENE_FK_A_PJRQ, FILAS_APUNTANDO, ERROR_AL_MEDIR)
        VALUES (c.OWNER, c.TABLE_NAME, c.COLUMN_NAME, v_tiene_fk, NULL,
                SUBSTR(SQLERRM, 1, 400));
    END;

  END LOOP;
  COMMIT;
END;

-- PASO 3 de 3 — el resultado. Tres consultas: el resumen, los problemas, y lo no medido.

-- 3a. Resumen. Esperado: TODO_LIMPIO con el total, y CON_REFERENCIAS = 0.
SELECT COUNT(*)                                                        AS COLUMNAS_MEDIDAS,
       SUM(CASE WHEN FILAS_APUNTANDO > 0 THEN 1 ELSE 0 END)            AS COLUMNAS_CON_REFERENCIAS,
       SUM(CASE WHEN ERROR_AL_MEDIR IS NOT NULL THEN 1 ELSE 0 END)     AS COLUMNAS_QUE_NO_SE_PUDIERON_MEDIR,
       NVL(SUM(FILAS_APUNTANDO), 0)                                    AS FILAS_APUNTANDO_EN_TOTAL
  FROM SCP.LAP1_CHK_REFS;

-- 3b. LO QUE IMPORTA. Si esto devuelve filas, PARAR y avisar antes del lap1-15.
--     TIENE_FK_A_PJRQ = 'NO' es el caso peligroso: nadie va a avisar.
SELECT ESQUEMA, TABLA, COLUMNA, TIENE_FK_A_PJRQ, FILAS_APUNTANDO
  FROM SCP.LAP1_CHK_REFS
 WHERE FILAS_APUNTANDO > 0
 ORDER BY TIENE_FK_A_PJRQ, FILAS_APUNTANDO DESC;

-- 3c. Lo que NO se pudo medir. Una lista vacia aca es indistinguible de una
--     medicion completa si no se mira, asi que se mira.
SELECT ESQUEMA, TABLA, COLUMNA, ERROR_AL_MEDIR
  FROM SCP.LAP1_CHK_REFS
 WHERE ERROR_AL_MEDIR IS NOT NULL
 ORDER BY 1, 2, 3;


--------------------------------------------------------------------------------
-- LIMPIEZA — descomentar y correr DESPUES de haber leido los tres resultados
-- del PASO 3. Mientras la tabla exista, el resultado se puede volver a mirar.
--------------------------------------------------------------------------------
-- DROP TABLE SCP.LAP1_CHK_REFS;


--------------------------------------------------------------------------------
-- FIN.
--
-- Lo unico que este archivo modifica es su propia tabla de trabajo. No toca ni
-- una fila de datos del sistema.
--
-- Con el CONTROL 4 en verde queda escrito el lap1-15: DELETE de los tres
-- subarboles, INSERT del arbol de SAA con ids quemados desde el 1253, y
-- ADELANTO DE LA SECUENCIA SCP.SQ_PJRQCDGO, que es el paso que faltaba y sin el
-- cual el proximo usuario nuevo del sistema no se puede crear.
--------------------------------------------------------------------------------

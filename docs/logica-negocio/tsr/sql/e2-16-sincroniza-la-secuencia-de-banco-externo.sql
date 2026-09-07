-- =====================================================================
-- 🔴 SQ_BEXTCDGO ESTA EN 95 Y LA TABLA LLEGA A 389
--    Dar de alta un banco externo desde la aplicacion MUERE con PK duplicada
-- Modulo: TSR  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- COMO APARECIO
--   Buscando otra cosa. El e2-15 preguntaba si BEXTCDGO era el codigo del BCE
--   (no lo es). Su BLOQUE 5 miraba la secuencia solo como testigo, para saber
--   si las filas se habian cargado con PK explicita. La respuesta fue que si,
--   y con eso aparecio un defecto que no se estaba buscando:
--
--       all_sequences.last_number = 95
--       MAX(BEXTCDGO)             = 389        (389 filas, 1..389 sin huecos)
--
--   Las 389 instituciones se cargaron con PK explicita y NADIE SINCRONIZO LA
--   SECUENCIA despues. Quedo casi 300 valores por debajo.
--
-- QUE ROMPE, HOY, EN PRODUCCION
--   BancoExterno.java declara:
--       @GeneratedValue(strategy = SEQUENCE, generator = "SQ_BEXTCDGO")
--   Asi que el alta de un banco externo desde la pantalla pide el NEXTVAL, le
--   toca un numero alrededor de 95, y ESE CODIGO YA EXISTE:
--       ORA-00001: restriccion unica violada
--   Y no falla una vez: falla en CADA intento hasta pasar el 389, o sea unas
--   300 veces seguidas.
--
--   Nadie lo reporto porque el catalogo vino cargado completo y nadie necesito
--   agregar un banco. Es un defecto latente, no uno inocuo: el dia que llegue
--   una cooperativa nueva, la pantalla no va a andar y el mensaje de Oracle no
--   va a decir nada sobre secuencias.
--
--   ⚠️ Y es EXACTAMENTE la regla 8 del esquema de trabajo de este equipo, la
--      que dice que despues de insertar PKs explicitas hay que sincronizar la
--      secuencia porque "el proximo insert desde la aplicacion muere por PK
--      duplicada, en una pantalla sin relacion aparente con lo que hiciste".
--      Estaba escrita antes de encontrar este caso.
--
-- ES INDEPENDIENTE DEL e2-14
--   No comparten nada: el e2-14 agrega una columna, este corrige una secuencia.
--   Se puede correr antes, despues, o sin el otro. Se separan a proposito para
--   que este no quede esperando a que lleguen los codigos del BCE.
--
-- ⚠️ NO TOCA NI UNA FILA DE DATOS. Solo mueve el contador de la secuencia.
--    No hay riesgo de perder informacion.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 -- DIAGNOSTICO. Solo lectura.
-- =====================================================================

-- 0.1 El desfase, en una sola consulta.
--     ESPERADO ANTES: ultimo_secuencia MUY por debajo de maximo_tabla.
--     Si ya viene por encima, este script YA SE CORRIO: no hacer nada.
SELECT (SELECT last_number FROM all_sequences
         WHERE sequence_owner = 'TSR' AND sequence_name = 'SQ_BEXTCDGO') AS ultimo_secuencia,
       (SELECT MAX(bextcdgo) FROM tsr.bext)                              AS maximo_tabla,
       (SELECT COUNT(*)      FROM tsr.bext)                              AS filas
  FROM dual;

-- 0.2 ¿Existe la secuencia y de quien es?
--     Si devuelve 0 filas, la entidad apunta a una secuencia que no existe y
--     el problema es otro: PARAR y avisar.
SELECT sequence_owner, sequence_name, min_value, increment_by, cache_size, last_number
  FROM all_sequences
 WHERE sequence_owner = 'TSR' AND sequence_name = 'SQ_BEXTCDGO';

-- 0.3 Privilegio para alterarla. La verificacion va JUNTO al paso que puede
--     fallar. Si el ejecutor no es TSR y no tiene ALTER sobre el objeto, el
--     BLOQUE 1 muere con ORA-01031 y el script "parece" haber corrido.
SELECT USER AS ejecutor FROM dual;


-- =====================================================================
-- BLOQUE 1 -- LA CORRECCION.
-- =====================================================================

-- Se reinicia en 390: el maximo de la tabla es 389.
-- ⚠️ NUMERO A REVALIDAR CON EL 0.1 ANTES DE EJECUTAR. Si entre que se escribio
--    este script y que se corre alguien cargo bancos nuevos a mano, 390 ya no
--    alcanza y hay que usar maximo_tabla + 1. Es la misma regla de revalidar
--    el MAX antes de ejecutar que el registro de reservas exige para PRBR/PDTR.
--
-- Sintaxis RESTART START WITH: es la que ya usa este repositorio para las
-- secuencias de SCP (ver crd/sql/70, 81, 83, 87). Oracle 18c+, y la base es 23ai.
ALTER SEQUENCE TSR.SQ_BEXTCDGO RESTART START WITH 390;


-- =====================================================================
-- BLOQUE 2 -- CONTROLES DESPUES.
-- =====================================================================

-- 2.1 El desfase desaparecio.
--     ESPERADO: ultimo_secuencia >= 390 y por encima de maximo_tabla.
SELECT (SELECT last_number FROM all_sequences
         WHERE sequence_owner = 'TSR' AND sequence_name = 'SQ_BEXTCDGO') AS ultimo_secuencia,
       (SELECT MAX(bextcdgo) FROM tsr.bext)                              AS maximo_tabla
  FROM dual;

-- 2.2 ⭐ LA PRUEBA DE VERDAD: pedirle un valor y ver que no choque.
--     ESPERADO: 390 (o el siguiente libre). Si devuelve algo <= 389, el ALTER
--     no surtio efecto: PARAR, no dar el alta por arreglada.
--     ⚠️ Esto CONSUME un valor de la secuencia. Es correcto y es barato: deja
--        un hueco de un numero en el catalogo, que no le importa a nadie
--        porque BEXTCDGO no significa nada (ver e2-15). Comprobar que la
--        secuencia entrega un valor usable vale mucho mas que ese hueco.
SELECT TSR.SQ_BEXTCDGO.NEXTVAL AS proximo_codigo FROM dual;

-- 2.3 Que ese valor no exista ya en la tabla. ESPERADO: 0 filas.
--     (Correr con el numero que devolvio el 2.2.)
-- SELECT bextcdgo, bextnmbr FROM tsr.bext WHERE bextcdgo = <valor del 2.2>;


-- =====================================================================
-- DESPUES DE ESTO -- vale la pena mirar si hay mas secuencias asi
-- =====================================================================
-- Este catalogo no puede ser el unico que se cargo con PKs explicitas. La
-- consulta de abajo lista TODAS las secuencias de TSR con su ultimo valor,
-- para contrastarlas contra el maximo de su tabla. Es solo lectura y sirve
-- para contar la familia antes de arreglar el ejemplar que se tiene en la
-- mano, que es la leccion del §29 de este equipo.
SELECT sequence_owner, sequence_name, last_number
  FROM all_sequences
 WHERE sequence_owner = 'TSR'
 ORDER BY sequence_name;


-- =====================================================================
-- REVERSO -- comentado.
-- =====================================================================
-- No hay reverso que valga la pena: volver la secuencia hacia atras la
-- devolveria al estado roto. Si hiciera falta, con el valor original:
-- ALTER SEQUENCE TSR.SQ_BEXTCDGO RESTART START WITH 95;

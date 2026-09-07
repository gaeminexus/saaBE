-- =====================================================================
-- ¿TSR.BEXT.BEXTCDGO YA ES EL CODIGO DEL BCE?  --  SOLO LECTURA
-- Modulo: TSR  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- POR QUE EXISTE
--   Escribi el e2-14 para AGREGAR una columna con el codigo de institucion
--   financiera, dando por hecho que no existia. El usuario respondio que la
--   tabla de bancos externos YA TIENE esos codigos. Puede tener razon, y el
--   dato que la respalda es fuerte:
--
--     · TSR.BEXT tiene 389 bancos activos (medido por el usuario el 2026-09-03,
--       ver e2-06). 389 no es un catalogo cargado a mano: es el sistema
--       financiero ecuatoriano completo, o sea una carga desde una lista
--       oficial. Y si se cargo de una lista oficial, lo mas probable es que se
--       haya cargado CON sus codigos.
--
--   Pero NO lo puedo confirmar desde el codigo, y no lo voy a afirmar sin
--   confirmarlo, porque este es exactamente el error que ya cometi dos veces:
--   confundir el codigo alterno con la PK (§8 y §9 del estado del equipo). Ahi
--   me equivoque suponiendo que la PK NO era el codigo. Suponer lo contrario
--   sin medirlo es el mismo error con el signo cambiado.
--
-- COMO SE RESUELVE, Y POR QUE ESTA PRUEBA ES CONCLUYENTE
--   Del manual del BizBank Light del Banco del Pacifico salen DOS codigos
--   escritos con todas las letras:
--       30 = Banco del Pacifico
--       25 = Banco de Machala
--   Son dos anclas independientes. Si en la base BEXTCDGO=30 resulta ser el
--   Pacifico y BEXTCDGO=25 el Machala, la PK ES el codigo del BCE y el e2-14
--   NO HACE FALTA. Si no coinciden, o coincide una sola, hay que agregar la
--   columna.
--
-- ⚠️ TODO ESTE SCRIPT ES SELECT. No modifica nada. Se puede correr en
--    produccion sin ninguna precaucion.
--
-- QUE DEVOLVER: la salida de los seis bloques, completa.
-- =====================================================================


-- =====================================================================
-- BLOQUE 1 -- ¿Hay alguna columna que la entidad Java NO este mapeando?
-- =====================================================================
-- La entidad BancoExterno.java mapea CINCO columnas:
--     BEXTCDGO · BEXTNMBR · BEXTTRJT · BEXTESTD · BEXTFCIN
-- Si la tabla tiene mas, ahi puede estar el codigo y nadie lo esta leyendo.
-- ESPERADO SI LA ENTIDAD ESTA COMPLETA: exactamente esas cinco filas.
SELECT column_id, column_name, data_type, data_length, nullable
  FROM all_tab_columns
 WHERE owner = 'TSR' AND table_name = 'BEXT'
 ORDER BY column_id;


-- =====================================================================
-- BLOQUE 2 -- ⭐ LA PRUEBA. Las dos anclas del manual del Pacifico.
-- =====================================================================
-- ESPERADO SI BEXTCDGO ES EL CODIGO DEL BCE:
--     codigo 30 -> un nombre que diga PACIFICO
--     codigo 25 -> un nombre que diga MACHALA
-- Si los dos dan en el clavo, esta cerrado: la PK es el codigo.
-- Si dan cualquier otro banco, la PK es una secuencia y el e2-14 va.
SELECT bextcdgo, bextnmbr, bextestd
  FROM tsr.bext
 WHERE bextcdgo IN (25, 30)
 ORDER BY bextcdgo;

-- 2b. La misma pregunta al reves, por si el nombre esta escrito distinto.
--     ESPERADO SI LA PK ES EL CODIGO: Pacifico devuelve 30, Machala 25.
SELECT bextcdgo, bextnmbr
  FROM tsr.bext
 WHERE UPPER(bextnmbr) LIKE '%PACIFICO%'
    OR UPPER(bextnmbr) LIKE '%MACHALA%'
 ORDER BY bextnmbr;


-- =====================================================================
-- BLOQUE 3 -- Los otros cinco codigos que aparecen en la muestra real
--             del Banco Internacional.
-- =====================================================================
-- El archivo que entrego el usuario usa 10, 17, 32, 36 y 213 sin decir a que
-- banco corresponde cada uno. Si la PK es el codigo del BCE, estos nombres
-- tienen que ser bancos reconocibles y grandes (los mas usados por la gente).
-- Es una tercera confirmacion, mas debil que el bloque 2 pero util: si el 32
-- NO es el Banco Internacional, algo no cuadra, porque el propio formato dice
-- que 32 es su valor por defecto.
SELECT bextcdgo, bextnmbr, bextestd
  FROM tsr.bext
 WHERE bextcdgo IN (10, 17, 32, 36, 213)
 ORDER BY bextcdgo;


-- =====================================================================
-- BLOQUE 4 -- ¿La PK parece una secuencia o una lista externa?
-- =====================================================================
-- Una PK de secuencia sobre 389 filas va de 1 a ~389 sin huecos grandes.
-- Un catalogo cargado con codigos oficiales tiene HUECOS y llega mas alto.
-- ESPERADO SI ES CODIGO OFICIAL: max muy por encima del total de filas.
SELECT COUNT(*)        AS total_filas,
       MIN(bextcdgo)   AS codigo_minimo,
       MAX(bextcdgo)   AS codigo_maximo,
       COUNT(DISTINCT bextcdgo) AS codigos_distintos
  FROM tsr.bext;


-- =====================================================================
-- BLOQUE 5 -- La secuencia, que es el testigo mas honesto.
-- =====================================================================
-- Si TSR.SQ_BEXTCDGO existe y su last_number esta MUY POR DEBAJO del maximo
-- del bloque 4, entonces las filas se insertaron con PK explicita y la
-- secuencia casi no se uso: eso confirma carga desde lista externa.
-- ⚠️ Y si ese es el caso, hay un problema aparte que hay que mirar: dar de
--    alta un banco nuevo desde la aplicacion tomaria el siguiente valor de la
--    secuencia, que puede chocar con un codigo ya usado (PK duplicada) o peor,
--    crear un banco con un codigo que el BCE asigno a otra institucion.
SELECT sequence_owner, sequence_name, last_number, increment_by
  FROM all_sequences
 WHERE sequence_owner = 'TSR' AND sequence_name = 'SQ_BEXTCDGO';


-- =====================================================================
-- BLOQUE 6 -- Los bancos que de verdad importan hoy.
-- =====================================================================
-- Sirve para dos cosas: ver si los codigos de los bancos EN USO son
-- razonables, y saber sobre cuantos beneficiarios impacta esto.
SELECT b.bextcdgo, b.bextnmbr,
       (SELECT COUNT(*) FROM tsr.ctbn c WHERE c.bextcdgo = b.bextcdgo) AS cuentas_titular,
       (SELECT COUNT(*) FROM rhh.cbem e WHERE e.bextcdgo = b.bextcdgo) AS cuentas_empleado
  FROM tsr.bext b
 WHERE EXISTS (SELECT 1 FROM tsr.ctbn c WHERE c.bextcdgo = b.bextcdgo)
    OR EXISTS (SELECT 1 FROM rhh.cbem e WHERE e.bextcdgo = b.bextcdgo)
 ORDER BY cuentas_titular DESC, cuentas_empleado DESC;


-- =====================================================================
-- COMO SE LEE EL RESULTADO
-- =====================================================================
-- CASO A -- bloque 2 da Pacifico=30 y Machala=25:
--     La PK ES el codigo del BCE. ⛔ EL e2-14 NO SE CORRE, se descarta.
--     Los formateadores leen banco.getCodigo() y no hace falta columna nueva.
--     PERO queda abierto lo del bloque 5: el alta de un banco nuevo por
--     secuencia es un riesgo real y hay que decidir que hacer con eso.
--
-- CASO B -- bloque 2 da otros bancos:
--     La PK es una secuencia. El e2-14 va tal como esta escrito.
--
-- CASO C -- el bloque 1 muestra una columna de mas que la entidad no mapea:
--     El codigo ya existe pero nadie lo lee. No se agrega nada: se mapea esa
--     columna en BancoExterno.java. PARAR y avisarme el nombre de la columna.
-- =====================================================================

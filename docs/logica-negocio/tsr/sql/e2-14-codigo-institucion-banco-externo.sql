-- =====================================================================
-- CODIGO DE INSTITUCION FINANCIERA (BCE) EN TSR.BEXT
-- Modulo: TSR  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
-- Reescrito el 2026-09-07 con el resultado del e2-15.
--
-- ✅ CONFIRMADO POR EL e2-15: LA COLUMNA HACE FALTA.
--
--   Se sospechaba que BEXTCDGO ya era el codigo del BCE. NO LO ES, y la
--   medicion no deja lugar a dudas:
--
--     · Anclas del manual del Pacifico, que son las que decidian:
--         BEXTCDGO 25 -> "COOPERATIVA ANDALUCIA"      (deberia ser MACHALA)
--         BEXTCDGO 30 -> "BANCO COOPNACIONAL S.A."    (deberia ser PACIFICO)
--       Y al reves: Machala es 5 y Pacifico es 8. Fallan las dos.
--     · Los codigos de la muestra del Internacional tampoco dan: 10 es "BANCO
--       AMAZONAS", 17 "BANCO SOLIDARIO", 32 "COOPERATIVA PABLO MUÑOZ VEGA".
--     · Bloque 4: 389 filas, minimo 1, maximo 389, 389 distintos. Es una
--       secuencia CORRIDA SIN HUECOS. Un catalogo con codigos oficiales tiene
--       huecos; este no tiene ninguno.
--
--   La leccion, para que no se repita: que la tabla tenga el catalogo COMPLETO
--   (389 instituciones, o sea una carga desde una lista oficial) NO implica que
--   traiga los codigos de esa lista. Se cargaron los nombres y se numeraron de
--   1 a 389 con la secuencia. Completo no es lo mismo que codificado.
--
-- ⚠️ HAY UN DEFECTO APARTE, MAS URGENTE QUE ESTE SCRIPT: la secuencia
--    SQ_BEXTCDGO quedo en 95 con el maximo de la tabla en 389, asi que dar de
--    alta un banco externo desde la aplicacion muere con PK duplicada. Lo
--    arregla el e2-16, que es INDEPENDIENTE de este archivo y se puede correr
--    antes, despues o sin este.
--
-- ⚠️ EL SQL VA ANTES DEL WAR. En cuanto la entidad BancoExterno mapee BEXTCDBC,
--    Hibernate la incluye en TODO SELECT de esa entidad: si la columna no
--    existe, revienta con ORA-00904 cualquier pantalla que liste bancos
--    externos, incluidas las que no muestran el codigo.
--
-- QUE HACE
--   BLOQUE 0: diagnostico. SOLO LECTURA.
--   BLOQUE 1: agrega TSR.BEXT.BEXTCDBC (nullable).
--   BLOQUE 2: comentario de columna.
--   BLOQUE 3: carga los DOS codigos verificados en el manual. El resto queda
--             como plantilla comentada, a completar por el usuario.
--   BLOQUE 4: controles despues.
--   Reverso comentado al final.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 -- DIAGNOSTICO. Solo lectura.
-- =====================================================================

-- 0.1 Columnas actuales. ESPERADO ANTES: 5 filas, ninguna BEXTCDBC.
--     Si BEXTCDBC ya aparece, este script YA SE CORRIO: pasar al bloque 4.
SELECT column_name, data_type, data_length, nullable
  FROM all_tab_columns
 WHERE owner = 'TSR' AND table_name = 'BEXT'
 ORDER BY column_id;

-- 0.2 Privilegio del ejecutor. La verificacion va JUNTO al paso que puede
--     fallar (leccion del §27: un GRANT comentado hizo fallar dos scripts en
--     silencio). Si el ejecutor no es TSR y no tiene ALTER, el BLOQUE 1 muere
--     con ORA-01031 y el script "parece" haber corrido.
SELECT USER AS ejecutor FROM dual;

SELECT grantee, table_schema, table_name, privilege
  FROM all_tab_privs
 WHERE table_schema = 'TSR' AND table_name = 'BEXT'
   AND privilege IN ('ALTER','SELECT','UPDATE');


-- =====================================================================
-- BLOQUE 1 -- DDL. Agrega la columna.
-- =====================================================================

-- Nullable a proposito: hay 389 bancos cargados y solo se conoce el codigo de
-- unos pocos. Un NOT NULL aca dejaria el ALTER imposible de correr.
-- VARCHAR2 y no NUMBER: el Internacional acepta "0032", "032" y "32" como el
-- mismo valor, y guardar texto preserva lo que la persona cargo.
ALTER TABLE TSR.BEXT ADD (BEXTCDBC VARCHAR2(4) NULL);


-- =====================================================================
-- BLOQUE 2 -- Comentario de columna.
-- =====================================================================

COMMENT ON COLUMN TSR.BEXT.BEXTCDBC IS
  'Codigo de la institucion financiera asignado por el Banco Central del Ecuador para camara de compensacion. Lo piden el campo 12 del archivo del Banco Internacional y la columna B de la macro BizBank Light del Banco del Pacifico. NO confundir con BEXTCDGO, que es una PK de secuencia sin relacion con el BCE.';


-- =====================================================================
-- BLOQUE 3 -- CARGA. Solo los DOS codigos verificados en el manual.
-- =====================================================================

-- Verificados textualmente en el manual de la macro BizBank Light:
--   "30 corresponde a Banco del Pacifico, 25 corresponde al Banco de Machala"
-- Se apuntan por BEXTCDGO, que el e2-15 ya devolvio, y no por LIKE sobre el
-- nombre: con 389 filas un LIKE '%PACIFICO%' puede pescar una cooperativa.
UPDATE TSR.BEXT SET BEXTCDBC = '30' WHERE BEXTCDGO = 8;   -- BANCO DEL PACIFICO
UPDATE TSR.BEXT SET BEXTCDBC = '25' WHERE BEXTCDGO = 5;   -- BANCO DE MACHALA

COMMIT;

-- ---------------------------------------------------------------------
-- ⛔ PLANTILLA -- LA COMPLETA EL USUARIO CON LA TABLA OFICIAL DEL BCE.
--
-- Son SOLO SEIS. El e2-15 midio que de los 389 bancos del catalogo apenas
-- SIETE tienen cuentas colgando, y uno de esos siete (Pacifico) ya quedo
-- cargado arriba. Estos seis son todo lo que separa al archivo del banco de
-- estar completo:
--
--   BEXTCDGO  BANCO                    CTAS TITULAR  CTAS EMPLEADO
--   --------  -----------------------  ------------  -------------
--      1      BANCO PICHINCHA               14            10
--      2      BANCO DE GUAYAQUIL             7             2
--     12      PRODUBANCO-PROMERICA           6             2
--      9      BANCO INTERNACIONAL            1             2
--     11      BANCO DEL AUSTRO               1             0
--     13      BANCO BOLIVARIANO              0             1
--
-- ⛔ NO DESCOMENTAR SIN EL CODIGO OFICIAL A LA VISTA. Un codigo equivocado no
--    hace rebotar la transferencia: la manda al banco equivocado.
-- ---------------------------------------------------------------------
-- UPDATE TSR.BEXT SET BEXTCDBC = '__' WHERE BEXTCDGO = 1;   -- BANCO PICHINCHA
-- UPDATE TSR.BEXT SET BEXTCDBC = '__' WHERE BEXTCDGO = 2;   -- BANCO DE GUAYAQUIL
-- UPDATE TSR.BEXT SET BEXTCDBC = '__' WHERE BEXTCDGO = 12;  -- PRODUBANCO-PROMERICA
-- UPDATE TSR.BEXT SET BEXTCDBC = '__' WHERE BEXTCDGO = 11;  -- BANCO DEL AUSTRO
-- UPDATE TSR.BEXT SET BEXTCDBC = '__' WHERE BEXTCDGO = 13;  -- BANCO BOLIVARIANO
--
-- BANCO INTERNACIONAL (BEXTCDGO 9) -- CANDIDATO '32', A CONFIRMAR:
--   La especificacion del propio Banco Internacional dice que el campo 12
--   vacio "se coloca por defecto el valor de 32". Un banco que rutea a si
--   mismo por defecto sugiere fuertemente que 32 es SU codigo. Es una
--   INFERENCIA razonable, NO un dato leido: confirmarla antes de usarla.
-- UPDATE TSR.BEXT SET BEXTCDBC = '32' WHERE BEXTCDGO = 9;   -- BANCO INTERNACIONAL
-- COMMIT;


-- =====================================================================
-- BLOQUE 4 -- CONTROLES DESPUES.
-- =====================================================================

-- 4.1 La columna existe. ESPERADO: 1 fila -- BEXTCDBC, VARCHAR2, 4, Y.
SELECT column_name, data_type, data_length, nullable
  FROM all_tab_columns
 WHERE owner = 'TSR' AND table_name = 'BEXT' AND column_name = 'BEXTCDBC';

-- 4.2 Que quedo cargado. ESPERADO tras el bloque 3: Pacifico=30, Machala=25.
SELECT bextcdgo, bextnmbr, bextcdbc
  FROM tsr.bext
 WHERE bextcdbc IS NOT NULL
 ORDER BY bextnmbr;

-- 4.3 ⭐ EL CONTROL QUE DECIDE SI EL ARCHIVO PUEDE SALIR A PRODUCCION.
--     Bancos CON CUENTAS y SIN codigo. Cada fila de aca es un beneficiario
--     cuyo pago sale con el campo 12 vacio, y el Banco Internacional lee el
--     campo 12 vacio como "32 = Banco Internacional": la transferencia NO
--     rebota, se va a otro banco.
--     ⛔ MIENTRAS ESTA CONSULTA DEVUELVA UNA SOLA FILA, EL ARCHIVO DEL
--        INTERNACIONAL NO SE GENERA PARA PRODUCCION.
--     (El formateador ya aborta por pago en ese caso, asi que el efecto real
--      es que no se puede generar el lote — que es lo correcto.)
SELECT b.bextcdgo, b.bextnmbr,
       (SELECT COUNT(*) FROM tsr.ctbn c WHERE c.bextcdgo = b.bextcdgo) AS cuentas_titular,
       (SELECT COUNT(*) FROM rhh.cbem e WHERE e.bextcdgo = b.bextcdgo) AS cuentas_empleado
  FROM tsr.bext b
 WHERE b.bextcdbc IS NULL
   AND ( EXISTS (SELECT 1 FROM tsr.ctbn c WHERE c.bextcdgo = b.bextcdgo)
      OR EXISTS (SELECT 1 FROM rhh.cbem e WHERE e.bextcdgo = b.bextcdgo) )
 ORDER BY cuentas_titular DESC, cuentas_empleado DESC;


-- =====================================================================
-- REVERSO -- comentado. Solo si hay que dar marcha atras.
-- =====================================================================
-- ⚠️ Borrar la columna borra los codigos cargados a mano. Antes de correr
--    esto, guardar el resultado del control 4.2.
-- ALTER TABLE TSR.BEXT DROP COLUMN BEXTCDBC;

-- =====================================================================
-- ⛔⛔ NO CORRER TODAVIA -- 2026-09-07, el mismo dia que se escribio.
--
--   El usuario respondio que TSR.BEXT **YA TIENE** los codigos de banco. Puede
--   tener razon: la tabla tiene 389 filas, que es el sistema financiero
--   ecuatoriano completo, o sea una carga desde una lista oficial.
--
--   Antes de agregar una columna que quiza sobra, hay que correr
--       e2-15-verifica-si-bextcdgo-ya-es-el-codigo-bce.sql   (SOLO LECTURA)
--   que contrasta la PK contra las dos anclas verificadas del manual del
--   Pacifico (30 = Pacifico, 25 = Machala).
--
--   · Si coinciden -> la PK ES el codigo. ESTE SCRIPT SE DESCARTA.
--   · Si no coinciden -> este script va tal como esta.
--
--   Se deja escrito y no se borra porque el analisis de POR QUE hace falta un
--   codigo de institucion vale igual en los dos casos.
-- =====================================================================

-- =====================================================================
-- CODIGO DE INSTITUCION FINANCIERA (BCE) EN TSR.BEXT
-- Modulo: TSR  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- POR QUE
--   Los dos formatos de archivo bancario que entrego el usuario piden el
--   codigo de la institucion financiera del BENEFICIARIO:
--     · Banco Internacional, campo 12 -- "codigo asignado por el Banco Central
--       del Ecuador para el procesamiento de camara".
--     · Banco del Pacifico (macro BizBank Light), columna B -- "codigo de
--       banco. 30 = Pacifico, 25 = Machala".
--   Ese dato NO EXISTE en la base. TSR.BEXT tiene cinco columnas y ninguna es
--   un codigo de institucion:
--       BEXTCDGO (PK) · BEXTNMBR · BEXTTRJT · BEXTESTD · BEXTFCIN
--
-- Y NO ES UN HUECO NUEVO -- ya esta mordiendo en produccion
--   GeneracionOrdenPagoServiceImpl:591-594, comentario del propio autor:
--       // Sale del snapshot, que guarda el NOMBRE del banco: TSR.BNCO no
--       // tiene codigo de institucion.
--       return texto(detalle.getBanco());
--   O sea que el archivo bancario de la NOMINA esta mandando el nombre del
--   banco donde el banco espera un numero. Esta columna cierra ese agujero
--   tambien, no solo los dos formatos nuevos.
--
-- ⛔ LO QUE ESTE SCRIPT NO HACE: cargar los codigos.
--   De los dos documentos oficiales salen SOLO DOS codigos verificados:
--       30 = Banco del Pacifico   (manual BizBank Light, textual)
--       25 = Banco de Machala     (manual BizBank Light, textual)
--   La muestra del Internacional trae ademas 10, 17, 32, 36 y 213 pero NO dice
--   a que banco corresponde cada uno. Ponerlos de memoria es mandar plata al
--   banco equivocado. El BLOQUE 3 deja el UPDATE de esos dos y una plantilla
--   comentada para el resto, que completa el usuario contra la tabla del BCE.
--
-- QUE HACE
--   BLOQUE 0: diagnostico. SOLO LECTURA.
--   BLOQUE 1: agrega TSR.BEXT.BEXTCDBC (nullable).
--   BLOQUE 2: comentario de columna.
--   BLOQUE 3: carga los DOS codigos verificados. El resto queda comentado.
--   BLOQUE 4: controles despues.
--   Reverso comentado al final.
--
-- ⚠️ NO HAY SECUENCIA QUE SINCRONIZAR: no se insertan filas con PK explicita,
--    solo se agrega una columna y se actualizan filas existentes.
--
-- ⚠️ EL SQL VA ANTES DEL WAR. En cuanto la entidad BancoExterno mapee
--    BEXTCDBC, Hibernate la incluye en TODO SELECT de esa entidad: si la
--    columna no existe, revienta con ORA-00904 cualquier pantalla que liste
--    bancos externos, incluidas las que no muestran el codigo.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 -- DIAGNOSTICO. Solo lectura.
-- =====================================================================

-- 0.1 Columnas actuales de TSR.BEXT.
--     ESPERADO ANTES: 5 filas, ninguna llamada BEXTCDBC.
--     Si BEXTCDBC ya aparece, este script YA SE CORRIO: pasar al bloque 4.
SELECT column_name, data_type, data_length, nullable
  FROM all_tab_columns
 WHERE owner = 'TSR' AND table_name = 'BEXT'
 ORDER BY column_id;

-- 0.2 Que bancos externos hay hoy, y cuantas cuentas cuelgan de cada uno.
--     Sirve para saber cuantos codigos hay que conseguir de verdad: los que
--     tienen cuentas son los urgentes.
SELECT b.bextcdgo, b.bextnmbr, b.bextestd,
       (SELECT COUNT(*) FROM tsr.ctbn c WHERE c.bextcdgo = b.bextcdgo) AS cuentas_titular,
       (SELECT COUNT(*) FROM rhh.cbem e WHERE e.bextcdgo = b.bextcdgo) AS cuentas_empleado
  FROM tsr.bext b
 ORDER BY cuentas_titular DESC, cuentas_empleado DESC, b.bextnmbr;

-- 0.3 Privilegio del ejecutor sobre TSR.BEXT.
--     ⚠️ La verificacion va JUNTO al paso que puede fallar (leccion del §27:
--     un GRANT comentado hizo fallar dos scripts en silencio). Si el usuario
--     que corre esto NO es TSR y no tiene ALTER, el BLOQUE 1 falla con
--     ORA-01031 y el script "parece" haber corrido.
--     ESPERADO: o el ejecutor es TSR, o aparece una fila con privilege=ALTER.
SELECT USER AS ejecutor FROM dual;

SELECT grantee, table_schema, table_name, privilege
  FROM all_tab_privs
 WHERE table_schema = 'TSR' AND table_name = 'BEXT'
   AND privilege IN ('ALTER','SELECT','UPDATE');


-- =====================================================================
-- BLOQUE 1 -- DDL. Agrega la columna.
-- =====================================================================

-- Nullable a proposito: hay bancos externos cargados y no tenemos el codigo de
-- todos todavia. Un NOT NULL aca dejaria el ALTER imposible de correr.
-- VARCHAR2 y no NUMBER: el Internacional acepta "0032", "032" y "32" como el
-- mismo valor, y guardar texto preserva lo que la persona cargo.
ALTER TABLE TSR.BEXT ADD (BEXTCDBC VARCHAR2(4) NULL);


-- =====================================================================
-- BLOQUE 2 -- Comentario de columna.
-- =====================================================================

COMMENT ON COLUMN TSR.BEXT.BEXTCDBC IS
  'Codigo de la institucion financiera asignado por el Banco Central del Ecuador para camara de compensacion. Lo piden el campo 12 del archivo del Banco Internacional y la columna B de la macro BizBank Light del Banco del Pacifico. Verificados: 30=Pacifico, 25=Machala.';


-- =====================================================================
-- BLOQUE 3 -- CARGA. Solo los dos codigos VERIFICADOS en el manual.
-- =====================================================================

-- ⚠️ El LIKE es deliberadamente amplio porque no sabemos como esta escrito el
--    nombre en la base ("BANCO DEL PACIFICO", "PACIFICO S.A.", "Pacifico").
--    CONTROLAR con el 0.2 y con el 4.2 que haya tocado la fila que se esperaba
--    y NO otra. Si toca 0 filas, el banco esta escrito distinto: mirar el 0.2.
UPDATE TSR.BEXT
   SET BEXTCDBC = '30'
 WHERE UPPER(BEXTNMBR) LIKE '%PACIFICO%'
   AND BEXTCDBC IS NULL;

UPDATE TSR.BEXT
   SET BEXTCDBC = '25'
 WHERE UPPER(BEXTNMBR) LIKE '%MACHALA%'
   AND BEXTCDBC IS NULL;

COMMIT;

-- ---------------------------------------------------------------------
-- PLANTILLA PARA EL RESTO -- la completa el usuario con la tabla del BCE.
-- ⛔ NO DESCOMENTAR SIN EL CODIGO OFICIAL A LA VISTA. Los numeros de abajo
--    son los que APARECEN en la muestra del Internacional, pero esa muestra
--    NO dice a que banco corresponde cada uno. Estan puestos como huecos a
--    llenar, no como respuesta.
-- ---------------------------------------------------------------------
-- UPDATE TSR.BEXT SET BEXTCDBC = '__' WHERE BEXTCDGO = ___;  -- ???
-- UPDATE TSR.BEXT SET BEXTCDBC = '__' WHERE BEXTCDGO = ___;  -- ???
-- COMMIT;


-- =====================================================================
-- BLOQUE 4 -- CONTROLES DESPUES.
-- =====================================================================

-- 4.1 La columna existe y es VARCHAR2(4) nullable.
--     ESPERADO: 1 fila -- BEXTCDBC, VARCHAR2, 4, Y.
SELECT column_name, data_type, data_length, nullable
  FROM all_tab_columns
 WHERE owner = 'TSR' AND table_name = 'BEXT' AND column_name = 'BEXTCDBC';

-- 4.2 Que quedo cargado, y sobre que banco.
--     ESPERADO: las filas de Pacifico y Machala con 30 y 25. Si aparece un
--     banco que no es ninguno de esos dos, el LIKE del bloque 3 pesco de mas:
--     REVERTIR esa fila a NULL antes de seguir.
SELECT bextcdgo, bextnmbr, bextcdbc
  FROM tsr.bext
 WHERE bextcdbc IS NOT NULL
 ORDER BY bextnmbr;

-- 4.3 CUANTOS QUEDAN SIN CODIGO, y cuantas cuentas cuelgan de ellos.
--     ⚠️ ESTE ES EL CONTROL QUE IMPORTA. Cada banco de esta lista con
--     cuentas > 0 es un beneficiario cuyo pago va a salir con el campo 12
--     vacio, y el Banco Internacional interpreta el campo 12 vacio como
--     "32 = Banco Internacional". O sea que la transferencia NO rebota:
--     se manda a otro banco. Mientras esta consulta devuelva filas con
--     cuentas > 0, el archivo del Internacional NO esta listo para produccion.
SELECT b.bextcdgo, b.bextnmbr,
       (SELECT COUNT(*) FROM tsr.ctbn c WHERE c.bextcdgo = b.bextcdgo) AS cuentas_titular,
       (SELECT COUNT(*) FROM rhh.cbem e WHERE e.bextcdgo = b.bextcdgo) AS cuentas_empleado
  FROM tsr.bext b
 WHERE b.bextcdbc IS NULL
 ORDER BY cuentas_titular DESC, cuentas_empleado DESC;


-- =====================================================================
-- REVERSO -- comentado. Solo si hay que dar marcha atras.
-- =====================================================================
-- ⚠️ Borrar la columna borra los codigos ya cargados a mano. Antes de correr
--    esto, guardar el resultado del control 4.2.
-- ALTER TABLE TSR.BEXT DROP COLUMN BEXTCDBC;

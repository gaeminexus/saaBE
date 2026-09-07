-- =====================================================================
-- ¿TSR.BEXT.BEXTTRJT ES EL CODIGO DEL BCE?  --  SOLO LECTURA
-- Modulo: TSR  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- ⚠️ REESCRITO el 2026-09-07: la primera version usaba PROMPT / SET / COLUMN,
--    que son comandos de SQL*Plus y el cliente del usuario NO los interpreta —
--    los escupia como texto. Ningun otro script de este equipo los usa (ni el
--    e2-15 ni el e2-08 tienen uno solo). Esta version es SQL puro.
--
-- POR QUE EXISTE
--   El usuario aviso que el codigo del BCE SI esta en TSR.BEXT, pero en otra
--   columna: BEXTTRJT. Si tiene razon, el e2-14 se cancela entero: no hay DDL,
--   no hacen falta los seis codigos que estabamos esperando, y el unico cambio
--   de codigo es que CodigoBancoBeneficiarioResolver devuelva getTarjeta() en
--   vez de getCodigo().
--
--   NO SE DA POR BUENO SIN MEDIRLO, y esta vez hay un motivo concreto ademas
--   del metodo: EL FRONTEND TRATA ESA COLUMNA COMO UN BOOLEANO.
--
--     saaFE/src/app/modules/tsr/model/banco-externo.model.ts:6
--         tarjeta: boolean;
--     saaFE/.../bancos/bancos-nacionales-extranjeros.component.ts:179
--         tarjeta: this.tarjetaCredito ? 1 : 0,   // Backend espera Long (1 o 0)
--
--   O sea que la pantalla de mantenimiento de bancos externos escribe 1 o 0 en
--   BEXTTRJT cada vez que alguien guarda un banco. En el backend, en cambio,
--   BancoExterno.tarjeta no lo lee NADIE: solo tiene getter y setter.
--
--   Las dos lecturas no pueden ser ciertas a la vez:
--     · Si BEXTTRJT es un flag  -> el e2-14 sigue haciendo falta.
--     · Si BEXTTRJT es el codigo -> la pantalla lo viene DESTRUYENDO, y cada
--       banco que alguien edito quedo con 1 o 0 en lugar de su codigo. Eso es
--       corrupcion silenciosa, no un frente parado. El bloque 5 lo mide.
--
-- COMO SE RESUELVE
--   Igual que el e2-15, que cerro la discusion anterior en una sola corrida:
--   las DOS anclas del manual del BizBank Light del Pacifico, escritas con
--   todas las letras en el documento oficial:
--       30 = Banco del Pacifico
--       25 = Banco de Machala
--   Son anclas independientes. Si las dos dan, esta cerrado. Si da una sola,
--   no alcanza: una coincidencia suelta puede ser casualidad.
--
-- Columnas verificadas contra la entidad com.saa.model.tsr.BancoExterno
--   (BEXTCDGO · BEXTNMBR · BEXTTRJT · BEXTESTD · BEXTFCIN). Ninguna inventada.
--
-- ⚠️ TODO ESTE SCRIPT ES SELECT. No modifica nada. Se puede correr en
--    produccion sin ninguna precaucion.
--
-- QUE DEVOLVER: la salida de los cinco bloques, completa.
-- =====================================================================


-- =====================================================================
-- BLOQUE 1 -- La FORMA del dato: ¿catalogo o flag?
-- =====================================================================
-- Un booleano da 2 o 3 valores distintos. El catalogo del BCE da decenas.
-- Este bloque solo ya casi decide la pregunta.
--
-- ESPERADO SI ES EL CODIGO DEL BCE: decenas de valores distintos, maximo en
--   el orden de los cientos.
-- ESPERADO SI ES UN FLAG: 2 o 3 valores distintos, maximo 1.
SELECT 'BLOQUE 1 - forma del dato'  AS bloque,
       COUNT(*)                     AS filas_total,
       COUNT(BEXTTRJT)              AS con_valor,
       COUNT(*) - COUNT(BEXTTRJT)   AS en_null,
       COUNT(DISTINCT BEXTTRJT)     AS valores_distintos,
       MIN(BEXTTRJT)                AS minimo,
       MAX(BEXTTRJT)                AS maximo
  FROM TSR.BEXT;


-- Los 15 valores mas repetidos, para ver la forma de la distribucion.
-- ESPERADO SI ES UN FLAG: dos filas (0 y 1) que se reparten los 389 bancos.
SELECT 'BLOQUE 1b - distribucion' AS bloque, valor, cuantos_bancos
  FROM (SELECT BEXTTRJT AS valor, COUNT(*) AS cuantos_bancos
          FROM TSR.BEXT
         GROUP BY BEXTTRJT
         ORDER BY COUNT(*) DESC)
 WHERE ROWNUM <= 15;


-- =====================================================================
-- BLOQUE 2 -- ⭐ LA PRUEBA. Las dos anclas del manual del Pacifico.
-- =====================================================================
-- ESPERADO SI BEXTTRJT ES EL CODIGO DEL BCE:
--     el banco que se llame PACIFICO -> BEXTTRJT = 30
--     el banco que se llame MACHALA  -> BEXTTRJT = 25
-- Si los dos dan en el clavo, esta cerrado y el e2-14 se cancela.
-- Si dan cualquier otra cosa, BEXTTRJT no es el codigo.
SELECT 'BLOQUE 2 - anclas'  AS bloque,
       BEXTCDGO             AS pk,
       BEXTNMBR             AS nombre,
       BEXTTRJT             AS trjt,
       CASE
         WHEN UPPER(BEXTNMBR) LIKE '%PACIFICO%' AND BEXTTRJT = 30
              THEN 'OK - Pacifico tiene 30, como el manual'
         WHEN UPPER(BEXTNMBR) LIKE '%MACHALA%'  AND BEXTTRJT = 25
              THEN 'OK - Machala tiene 25, como el manual'
         ELSE '*** NO COINCIDE con el manual ***'
       END                  AS veredicto
  FROM TSR.BEXT
 WHERE UPPER(BEXTNMBR) LIKE '%PACIFICO%'
    OR UPPER(BEXTNMBR) LIKE '%MACHALA%'
 ORDER BY BEXTNMBR;


-- Y la prueba al reves: quien tiene 25 y quien tiene 30.
-- ESPERADO SI ES EL CODIGO: exactamente Machala y Pacifico, uno cada uno.
SELECT 'BLOQUE 2b - prueba inversa' AS bloque,
       BEXTCDGO AS pk, BEXTNMBR AS nombre, BEXTTRJT AS trjt
  FROM TSR.BEXT
 WHERE BEXTTRJT IN (25, 30)
 ORDER BY BEXTTRJT, BEXTNMBR;


-- =====================================================================
-- BLOQUE 3 -- Lo unico que importa operativamente: los bancos CON cuentas
-- =====================================================================
-- De los 389 del catalogo solo unos siete tienen cuentas de beneficiario.
-- Si esos siete tienen un BEXTTRJT plausible, el frente se destraba aunque el
-- resto del catalogo este sucio.
SELECT 'BLOQUE 3 - bancos con cuentas' AS bloque,
       b.BEXTCDGO AS pk,
       b.BEXTNMBR AS nombre,
       b.BEXTTRJT AS trjt,
       (SELECT COUNT(*) FROM TSR.CTBN c WHERE c.BEXTCDGO = b.BEXTCDGO) AS cuentas_titular,
       (SELECT COUNT(*) FROM RHH.CBEM e WHERE e.BEXTCDGO = b.BEXTCDGO) AS cuentas_empleado
  FROM TSR.BEXT b
 WHERE (SELECT COUNT(*) FROM TSR.CTBN c WHERE c.BEXTCDGO = b.BEXTCDGO) > 0
    OR (SELECT COUNT(*) FROM RHH.CBEM e WHERE e.BEXTCDGO = b.BEXTCDGO) > 0
 ORDER BY cuentas_titular DESC, cuentas_empleado DESC;


-- =====================================================================
-- BLOQUE 4 -- Los codigos que aparecen en la muestra del Internacional
-- =====================================================================
-- En el archivo real que entrego el usuario aparecen 10, 17, 32, 36 y 213 como
-- codigo de banco, PERO sin decir a que banco corresponde cada uno.
-- ESPERADO SI BEXTTRJT ES EL CODIGO: los cinco resuelven a bancos reales y
--   conocidos, y el 32 deberia ser el Banco Internacional.
SELECT 'BLOQUE 4 - muestra internacional' AS bloque,
       BEXTTRJT AS trjt, BEXTCDGO AS pk, BEXTNMBR AS nombre
  FROM TSR.BEXT
 WHERE BEXTTRJT IN (10, 17, 32, 36, 213)
 ORDER BY BEXTTRJT, BEXTNMBR;


-- =====================================================================
-- BLOQUE 5 -- ¿La pantalla de bancos vino DESTRUYENDO el dato?
-- =====================================================================
-- La pantalla escribe 1 o 0 en BEXTTRJT al guardar. Si esta columna es el
-- codigo del BCE, todo banco con 0 o 1 es un banco al que ya le borraron el
-- codigo.
SELECT 'BLOQUE 5 - posible daño' AS bloque,
       COUNT(*)                                      AS bancos_con_0_o_1,
       SUM(CASE WHEN BEXTTRJT = 0 THEN 1 ELSE 0 END) AS en_cero,
       SUM(CASE WHEN BEXTTRJT = 1 THEN 1 ELSE 0 END) AS en_uno
  FROM TSR.BEXT
 WHERE BEXTTRJT IN (0, 1);


-- Los que tienen 0/1 Y ADEMAS tienen cuentas: estos son los urgentes, porque
-- son los que sacan el archivo bancario mal HOY.
-- ESPERADO SI BEXTTRJT ES UN FLAG: esta consulta devuelve los mismos bancos
--   que el bloque 3, y no significa nada.
-- ESPERADO SI ES EL CODIGO: cada fila aca es un beneficiario al que se le
--   manda plata con codigo de institucion invalido.
SELECT 'BLOQUE 5b - urgentes' AS bloque,
       b.BEXTCDGO AS pk, b.BEXTNMBR AS nombre, b.BEXTTRJT AS trjt,
       (SELECT COUNT(*) FROM TSR.CTBN c WHERE c.BEXTCDGO = b.BEXTCDGO) AS cuentas_titular,
       (SELECT COUNT(*) FROM RHH.CBEM e WHERE e.BEXTCDGO = b.BEXTCDGO) AS cuentas_empleado
  FROM TSR.BEXT b
 WHERE b.BEXTTRJT IN (0, 1)
   AND ( (SELECT COUNT(*) FROM TSR.CTBN c WHERE c.BEXTCDGO = b.BEXTCDGO) > 0
      OR (SELECT COUNT(*) FROM RHH.CBEM e WHERE e.BEXTCDGO = b.BEXTCDGO) > 0 )
 ORDER BY cuentas_titular DESC;


-- =====================================================================
-- COMO LEER EL RESULTADO
-- =====================================================================
--  · Bloque 1 con 2 o 3 valores distintos (0/1/NULL)
--       -> BEXTTRJT es un flag. Se confundio de columna y el e2-14 sigue
--          haciendo falta, con sus seis codigos.
--
--  · Bloque 1 con decenas de valores distintos Y bloque 2 con las dos anclas OK
--       -> BEXTTRJT ES el codigo del BCE. Se cancela el e2-14, no hay DDL, no
--          hacen falta los seis codigos, y el unico cambio de codigo es que
--          CodigoBancoBeneficiarioResolver devuelva getTarjeta().
--
--  · Bloque 2 con UNA sola ancla OK
--       -> No alcanza. Por eso son dos anclas independientes. Avisar y parar.
--
--  · Bloque 5b devolviendo filas
--       -> 🔴 URGENTE si BEXTTRJT resulto ser el codigo: la pantalla de bancos
--          externos viene borrando codigos del BCE. Hay que arreglar la
--          pantalla ANTES de recargar ningun dato, o se vuelve a perder en la
--          siguiente edicion.
--
-- FIN -- no se modifico nada.
-- =====================================================================

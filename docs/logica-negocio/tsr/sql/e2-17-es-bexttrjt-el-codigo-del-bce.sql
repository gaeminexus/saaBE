-- =====================================================================
-- e2-17 — ¿Es TSR.BEXT.BEXTTRJT el código de institución del BCE?
-- =====================================================================
-- Equipo:  omen-saa-2          Fecha: 2026-09-07
-- Tipo:    ⚪ SOLO LECTURA. No modifica absolutamente nada.
--
-- POR QUÉ EXISTE
-- El usuario avisó que el código del BCE sí está en TSR.BEXT, pero en otra
-- columna: BEXTTRJT. Si tiene razón, el e2-14 (que agrega BEXTCDBC) NO hace
-- falta, no hay DDL, no hacen falta los seis códigos que estábamos esperando,
-- y todo el frente de archivos bancarios se destraba hoy.
--
-- NO SE DA POR BUENO SIN MEDIRLO, y esta vez hay un motivo concreto además
-- del método: EL FRONTEND TRATA ESA COLUMNA COMO UN BOOLEANO.
--
--   saaFE/src/app/modules/tsr/model/banco-externo.model.ts:6
--       tarjeta: boolean;
--   saaFE/.../bancos/bancos-nacionales-extranjeros.component.ts:179
--       tarjeta: this.tarjetaCredito ? 1 : 0,   // Backend espera Long (1 o 0)
--
-- O sea que la pantalla de mantenimiento de bancos externos escribe 1 o 0 en
-- BEXTTRJT cada vez que alguien guarda un banco. Las dos lecturas no pueden
-- ser ciertas a la vez, y de cuál sea depende algo peor que el frente:
--
--   * Si BEXTTRJT es un booleano  -> el usuario se confundió de columna y
--     seguimos necesitando el e2-14.
--   * Si BEXTTRJT es el código BCE -> la pantalla de bancos lo viene
--     DESTRUYENDO: cada banco que alguien editó quedó con 1 o 0 en lugar de
--     su código, y eso es corrupción de datos silenciosa, no un frente parado.
--
-- El bloque 5 existe para eso: contar el daño si lo hubo.
--
-- Columnas verificadas contra la entidad com.saa.model.tsr.BancoExterno
-- (BEXTCDGO, BEXTNMBR, BEXTTRJT, BEXTESTD, BEXTFCIN). No se inventó ninguna.
-- =====================================================================

SET LINESIZE 200
SET PAGESIZE 100
COLUMN nombre  FORMAT A45
COLUMN veredicto FORMAT A50

PROMPT
PROMPT =====================================================================
PROMPT BLOQUE 1 — La FORMA del dato: ¿parece un catálogo o parece un flag?
PROMPT =====================================================================
PROMPT Un booleano da 2 o 3 valores distintos. El catálogo del BCE da decenas.

SELECT COUNT(*)                                             AS filas_total,
       COUNT(BEXTTRJT)                                      AS con_valor,
       COUNT(*) - COUNT(BEXTTRJT)                           AS en_null,
       COUNT(DISTINCT BEXTTRJT)                             AS valores_distintos,
       MIN(BEXTTRJT)                                        AS minimo,
       MAX(BEXTTRJT)                                        AS maximo
  FROM TSR.BEXT;

PROMPT
PROMPT --- Los 15 valores más repetidos, para ver la forma de la distribucion ---

SELECT * FROM (
  SELECT BEXTTRJT                                           AS valor,
         COUNT(*)                                           AS cuantos_bancos
    FROM TSR.BEXT
   GROUP BY BEXTTRJT
   ORDER BY COUNT(*) DESC
) WHERE ROWNUM <= 15;

PROMPT
PROMPT =====================================================================
PROMPT BLOQUE 2 — LA PRUEBA DECISIVA: las dos anclas del manual del Pacifico
PROMPT =====================================================================
PROMPT El manual dice, textual: 30 = Banco del Pacifico, 25 = Banco de Machala.
PROMPT Son dos anclas INDEPENDIENTES. Si las dos dan OK, BEXTTRJT es el codigo
PROMPT del BCE y no hay mas discusion. Si las dos fallan, no lo es.

SELECT BEXTCDGO                                             AS pk,
       BEXTNMBR                                             AS nombre,
       BEXTTRJT                                             AS trjt,
       CASE
         WHEN UPPER(BEXTNMBR) LIKE '%PACIFICO%' AND BEXTTRJT = 30
              THEN 'OK — Pacifico tiene 30, como dice el manual'
         WHEN UPPER(BEXTNMBR) LIKE '%MACHALA%'  AND BEXTTRJT = 25
              THEN 'OK — Machala tiene 25, como dice el manual'
         ELSE '*** NO COINCIDE con el manual ***'
       END                                                  AS veredicto
  FROM TSR.BEXT
 WHERE UPPER(BEXTNMBR) LIKE '%PACIFICO%'
    OR UPPER(BEXTNMBR) LIKE '%MACHALA%'
 ORDER BY BEXTNMBR;

PROMPT
PROMPT --- Y la prueba al reves: quien tiene 25 y quien tiene 30 ---

SELECT BEXTCDGO AS pk, BEXTNMBR AS nombre, BEXTTRJT AS trjt
  FROM TSR.BEXT
 WHERE BEXTTRJT IN (25, 30)
 ORDER BY BEXTTRJT, BEXTNMBR;

PROMPT
PROMPT =====================================================================
PROMPT BLOQUE 3 — Lo unico que importa operativamente: los bancos CON cuentas
PROMPT =====================================================================
PROMPT De los 389 del catalogo solo unos siete tienen cuentas de beneficiario.
PROMPT Si esos siete tienen un BEXTTRJT plausible, el frente se destraba aunque
PROMPT el resto del catalogo este sucio.

SELECT b.BEXTCDGO                                           AS pk,
       b.BEXTNMBR                                           AS nombre,
       b.BEXTTRJT                                           AS trjt,
       (SELECT COUNT(*) FROM TSR.CTBN c WHERE c.BEXTCDGO = b.BEXTCDGO) AS cuentas_titular,
       (SELECT COUNT(*) FROM RHH.CBEM e WHERE e.BEXTCDGO = b.BEXTCDGO) AS cuentas_empleado
  FROM TSR.BEXT b
 WHERE (SELECT COUNT(*) FROM TSR.CTBN c WHERE c.BEXTCDGO = b.BEXTCDGO) > 0
    OR (SELECT COUNT(*) FROM RHH.CBEM e WHERE e.BEXTCDGO = b.BEXTCDGO) > 0
 ORDER BY cuentas_titular DESC, cuentas_empleado DESC;

PROMPT
PROMPT =====================================================================
PROMPT BLOQUE 4 — Los codigos que aparecen en la muestra del Internacional
PROMPT =====================================================================
PROMPT En el archivo real que entrego el usuario aparecen 10, 17, 32, 36 y 213
PROMPT como codigo de banco, PERO sin decir a que banco corresponde cada uno.
PROMPT Si BEXTTRJT es el codigo del BCE, estos cinco tienen que resolver a
PROMPT bancos reales y conocidos (32 deberia ser el Banco Internacional).

SELECT BEXTTRJT AS trjt, BEXTCDGO AS pk, BEXTNMBR AS nombre
  FROM TSR.BEXT
 WHERE BEXTTRJT IN (10, 17, 32, 36, 213)
 ORDER BY BEXTTRJT, BEXTNMBR;

PROMPT
PROMPT =====================================================================
PROMPT BLOQUE 5 — ¿La pantalla de bancos vino DESTRUYENDO el dato?
PROMPT =====================================================================
PROMPT La pantalla escribe 1 o 0 en BEXTTRJT al guardar. Si esta columna es el
PROMPT codigo del BCE, todo banco con 0 o 1 es un banco al que ya le borraron
PROMPT el codigo. Y si ademas tiene cuentas, su archivo bancario sale mal HOY.

SELECT COUNT(*)                                             AS bancos_con_0_o_1,
       SUM(CASE WHEN BEXTTRJT = 0 THEN 1 ELSE 0 END)        AS en_cero,
       SUM(CASE WHEN BEXTTRJT = 1 THEN 1 ELSE 0 END)        AS en_uno
  FROM TSR.BEXT
 WHERE BEXTTRJT IN (0, 1);

PROMPT
PROMPT --- Los que tienen 0/1 Y ADEMAS tienen cuentas: estos son los urgentes ---

SELECT b.BEXTCDGO AS pk, b.BEXTNMBR AS nombre, b.BEXTTRJT AS trjt,
       (SELECT COUNT(*) FROM TSR.CTBN c WHERE c.BEXTCDGO = b.BEXTCDGO) AS cuentas_titular,
       (SELECT COUNT(*) FROM RHH.CBEM e WHERE e.BEXTCDGO = b.BEXTCDGO) AS cuentas_empleado
  FROM TSR.BEXT b
 WHERE b.BEXTTRJT IN (0, 1)
   AND ( (SELECT COUNT(*) FROM TSR.CTBN c WHERE c.BEXTCDGO = b.BEXTCDGO) > 0
      OR (SELECT COUNT(*) FROM RHH.CBEM e WHERE e.BEXTCDGO = b.BEXTCDGO) > 0 )
 ORDER BY cuentas_titular DESC;

PROMPT
PROMPT --- Y la otra cara: bancos editados recientemente, por si se ve el patron ---

SELECT * FROM (
  SELECT BEXTCDGO AS pk, BEXTNMBR AS nombre, BEXTTRJT AS trjt, BEXTFCIN AS fecha_ingreso
    FROM TSR.BEXT
   WHERE BEXTFCIN IS NOT NULL
   ORDER BY BEXTFCIN DESC
) WHERE ROWNUM <= 10;

PROMPT
PROMPT =====================================================================
PROMPT COMO LEER EL RESULTADO
PROMPT =====================================================================
PROMPT
PROMPT  * Bloque 1 con 2 o 3 valores distintos (0/1/NULL)
PROMPT       -> BEXTTRJT es un flag. El usuario se confundio de columna y el
PROMPT          e2-14 sigue haciendo falta, con sus seis codigos.
PROMPT
PROMPT  * Bloque 1 con decenas de valores distintos Y bloque 2 con los dos OK
PROMPT       -> BEXTTRJT ES el codigo del BCE. Se cancela el e2-14, no hay DDL,
PROMPT          no hacen falta los seis codigos, y el unico cambio de codigo es
PROMPT          que CodigoBancoBeneficiarioResolver devuelva getTarjeta() en vez
PROMPT          de getCodigo(). El frente se destraba hoy.
PROMPT
PROMPT  * Bloque 2 con UNA sola ancla OK
PROMPT       -> No alcanza. Una coincidencia sola puede ser casualidad; por eso
PROMPT          son dos anclas independientes. Avisar y no avanzar.
PROMPT
PROMPT  * Bloque 5 devolviendo filas con cuentas > 0
PROMPT       -> 🔴 URGENTE, y es peor que el frente parado: la pantalla de
PROMPT          bancos externos viene borrando codigos del BCE. Hay que
PROMPT          arreglar la pantalla ANTES de recargar ningun dato, o se vuelve
PROMPT          a perder en la siguiente edicion.
PROMPT
PROMPT =====================================================================
PROMPT FIN — no se modifico nada.
PROMPT =====================================================================

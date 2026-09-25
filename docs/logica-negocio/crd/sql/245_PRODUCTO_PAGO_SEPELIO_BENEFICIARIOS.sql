-- =====================================================================================
-- 245 - CREAR el grupo y el producto de pago del SEPELIO, y asignarlo al tipo 27
-- FECHA: 2026-09-25 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- Sin comandos de SQL*Plus. Comentarios ARRIBA, nunca intercalados.
-- ⛔ ESTE SCRIPT ESCRIBE. Los bloques 1, 2 y 3 hacen INSERT/UPDATE y van COMENTADOS:
--    correr primero el bloque 0 entero y LEER cada salida.
--
-- =====================================================================================
-- QUE DESBLOQUEA
-- =====================================================================================
-- Al intentar pagar el sepelio, el sistema responde:
--
--   "TIPO_APORTE_SIN_PRODUCTO: la devolucion mezcla tipos con y sin producto de pago
--    parametrizado, y eso generaria un asiento descuadrado. Sin producto de pago:
--    27 (VALOR DE SEGURO POR ENTREGAR A BENEFICIARIOS)."
--
-- No es un defecto: es la guarda de "todo o nada" de DevolucionAporteServiceImpl
-- funcionando. Prefiere no pagar antes que dejar la contabilidad descuadrada.
--
-- El tipo 27 se creo con el sql/231 dejando TPAPPRDP en NULL a proposito, y ese mismo
-- script lo dejo escrito: "es el producto de pago de CXP, y todavia no existe uno contra
-- 2.3.90.90.11 — hace falta para la FASE 2 (el pago a los beneficiarios), no para
-- recibir". Este script crea ese producto.
--
-- =====================================================================================
-- ⚠️ ESTE SCRIPT TOCA EL ESQUEMA PGS, QUE NO ES DE ESTE EQUIPO
-- =====================================================================================
-- PGS.GRPP y PGS.PRDP son parametria del modulo de pagos (alcance de omen-saa-2).
-- Se toca igual, y con estos tres motivos:
--   1. Es CONFIGURACION, no codigo: dos filas de catalogo, sin cambiar una linea de su
--      logica ni el comportamiento de ningun producto existente.
--   2. Hay PRECEDENTE PROPIO: este mismo equipo creo asi el producto 517 con el sql/202,
--      y este script copia su estructura exacta — no inventa nada.
--   3. Lo decide el usuario, que es el dueño del sistema, y hay un pago real frenado.
-- ⇒ Se le AVISA a omen-saa-2 apenas corra, con el detalle de lo que se creo. No es
--   pedir permiso despues: es que la parametria de pagos es suya y tienen que saberlo.
--
-- =====================================================================================
-- DE DONDE SALE CADA VALOR — ninguno escrito a mano
-- =====================================================================================
--   El GRUPO se copia del 43 (DEVOLUCION PENSIONES COMPLEMENTARIAS), que es el molde que
--   ya funciona, y SOLO se le cambia el nombre y la CUENTA CONTABLE.
--   ⚠️ Diferencia con el sql/202: alli el grupo 43 ya tenia la cuenta correcta y se
--   copiaba tal cual. Aca NO: el sepelio va contra 2.3.90.90.11, que es otra cuenta, asi
--   que el PLNNCDGO se resuelve por su numero de cuenta y se verifica en el bloque 0.
--
--   El PRODUCTO se copia del 516, el unico molde probado en produccion, y solo cambian el
--   nombre, la descripcion y el grupo. El ID no se lista: PGS.PRDP.ID es IDENTITY.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS — si alguno no da lo esperado, PARAR
-- =====================================================================================

-- 0.1 La cuenta contable del sepelio tiene que existir. Esperado: 1 fila.
--     ⚠️ Anotá el PLNNCDGO que devuelve: es el que va a usar el grupo nuevo.
SELECT n.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR
  FROM CNT.PLNN n
 WHERE REPLACE(n.PLNNCNTA, '.', '') = '23909011';

-- 0.2 El grupo molde (43) tiene que existir. Esperado: 1 fila.
SELECT g.GRPPCDGO, g.GRPPNMBR, g.PLNNCDGO, g.GRPPESTD, g.PJRQCDGO
  FROM PGS.GRPP g
 WHERE g.GRPPCDGO = 43;

-- 0.3 El producto molde (516) tiene que existir. Esperado: 1 fila.
SELECT p.ID, p.NOMBRE, p.GRUPOPRODUCTO, p.EMPRESA, p.ESTADO
  FROM PGS.PRDP p
 WHERE p.ID = 516;

-- 0.4 PGS.PRDP.ID tiene que ser IDENTITY (por eso el INSERT no lo lista).
--     Esperado: 1 fila. Si NO devuelve fila, PARAR y avisar: el INSERT fallaria.
SELECT i.COLUMN_NAME, i.GENERATION_TYPE
  FROM ALL_TAB_IDENTITY_COLS i
 WHERE i.OWNER = 'PGS' AND i.TABLE_NAME = 'PRDP' AND i.COLUMN_NAME = 'ID';

-- 0.5 La secuencia del grupo. Esperado: 1 fila.
SELECT s.SEQUENCE_NAME FROM ALL_SEQUENCES s
 WHERE s.SEQUENCE_OWNER = 'PGS' AND s.SEQUENCE_NAME = 'SQ_GRPPCDGO';

-- 0.6 ⭐ Que NO exista ya un grupo o un producto apuntando a esa cuenta. Esperado: 0 filas.
--     Dos productos validos contra la misma cuenta es peor que ninguno: el operador
--     elegiria uno u otro sin saber cual vale.
SELECT g.GRPPCDGO AS ID, g.GRPPNMBR AS NOMBRE, 'GRUPO' AS QUE_ES
  FROM PGS.GRPP g
  JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
 WHERE REPLACE(n.PLNNCNTA, '.', '') = '23909011'
UNION ALL
SELECT p.ID, p.NOMBRE, 'PRODUCTO'
  FROM PGS.PRDP p
  JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO
  JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
 WHERE REPLACE(n.PLNNCNTA, '.', '') = '23909011';

-- 0.7 El tipo de aporte 27, y que su producto de pago este en NULL. Esperado: 1 fila,
--     TPAPPRDP nulo. Si ya tuviera producto, PARAR: el error seria por otra cosa.
SELECT t.TPAPCDGO, t.TPAPNMBR, t.TPAPPRDP, t.TPAPIDST
  FROM CRD.TPAP t
 WHERE t.TPAPCDGO = 27;


-- =====================================================================================
-- 1. EL GRUPO — COMENTADO. Copia del 43, con la cuenta del sepelio.
-- =====================================================================================
-- INSERT INTO PGS.GRPP (GRPPCDGO, GRPPNMBR, GRPPRYYA, GRPPRZZA, PLNNCDGO,
--                       GRPPCSUS, GRPPESTD, PJRQCDGO)
-- SELECT PGS.SQ_GRPPCDGO.NEXTVAL,
--        'VALORES DE SEGURO A BENEFICIARIOS',
--        g.GRPPRYYA,
--        g.GRPPRZZA,
--        (SELECT n.PLNNCDGO FROM CNT.PLNN n
--          WHERE REPLACE(n.PLNNCNTA, '.', '') = '23909011'),
--        g.GRPPCSUS,
--        g.GRPPESTD,
--        g.PJRQCDGO
--   FROM PGS.GRPP g
--  WHERE g.GRPPCDGO = 43;


-- =====================================================================================
-- 2. EL PRODUCTO — COMENTADO. Copia del 516, colgado del grupo nuevo.
-- =====================================================================================
-- ⛔ El ID no se lista: lo asigna Oracle (ver control 0.4).
--
-- INSERT INTO PGS.PRDP (EMPRESA, GRUPOPRODUCTO, NOMBRE, CODIGO, CODIGOAUX,
--                       PRECIOUNITARIO, DESCUENTO, TIPODESCUENTO, INCLUYEIVA, TIPOIVA,
--                       TIPOICE, ICE, DESCRIPCION, SUBSIDIO, PRECIOSINSUB, IRBPNR,
--                       MULTIPRECIO, STOCK, MANEJAUNIDAD, UNIDAD, ESTADO)
-- SELECT p.EMPRESA,
--        (SELECT g.GRPPCDGO FROM PGS.GRPP g
--          WHERE g.GRPPNMBR = 'VALORES DE SEGURO A BENEFICIARIOS'),
--        'Valores de Seguro a Beneficiarios',
--        p.CODIGO,
--        p.CODIGOAUX,
--        p.PRECIOUNITARIO,
--        p.DESCUENTO,
--        p.TIPODESCUENTO,
--        p.INCLUYEIVA,
--        p.TIPOIVA,
--        p.TIPOICE,
--        p.ICE,
--        'Entrega del valor de seguro (sepelio) a los beneficiarios del participe',
--        p.SUBSIDIO,
--        p.PRECIOSINSUB,
--        p.IRBPNR,
--        p.MULTIPRECIO,
--        p.STOCK,
--        p.MANEJAUNIDAD,
--        p.UNIDAD,
--        p.ESTADO
--   FROM PGS.PRDP p
--  WHERE p.ID = 516;


-- =====================================================================================
-- 3. ASIGNAR EL PRODUCTO AL TIPO DE APORTE 27 — COMENTADO
-- =====================================================================================
-- Esto es lo que apaga el error TIPO_APORTE_SIN_PRODUCTO.
--
-- UPDATE CRD.TPAP t
--    SET t.TPAPPRDP = (SELECT p.ID FROM PGS.PRDP p
--                       WHERE p.NOMBRE = 'Valores de Seguro a Beneficiarios')
--  WHERE t.TPAPCDGO = 27
--    AND t.TPAPPRDP IS NULL;
--
-- -- Esperado: 1 fila. Si dice 0, el tipo ya tenia producto: PARAR y revisar.
-- COMMIT;


-- =====================================================================================
-- 4. CONTROLES POSTERIORES
-- =====================================================================================

-- 4.1 El grupo y el producto nuevos, con su cuenta. Esperado: 2 filas, las dos con
--     la cuenta 2.3.90.90.11.
SELECT 'GRUPO' AS QUE_ES, g.GRPPCDGO AS ID, g.GRPPNMBR AS NOMBRE, n.PLNNCNTA AS CUENTA
  FROM PGS.GRPP g
  JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
 WHERE g.GRPPNMBR = 'VALORES DE SEGURO A BENEFICIARIOS'
UNION ALL
SELECT 'PRODUCTO', p.ID, p.NOMBRE, n.PLNNCNTA
  FROM PGS.PRDP p
  JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO
  JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
 WHERE p.NOMBRE = 'Valores de Seguro a Beneficiarios';

-- 4.2 El tipo 27 ya apunta al producto. Esperado: 1 fila con TPAPPRDP no nulo.
SELECT t.TPAPCDGO, t.TPAPNMBR, t.TPAPPRDP
  FROM CRD.TPAP t
 WHERE t.TPAPCDGO = 27;

-- 4.3 ⭐ Y que NO haya quedado mas de un producto contra esa cuenta. Esperado: 1.
SELECT COUNT(*) AS PRODUCTOS_CONTRA_23909011
  FROM PGS.PRDP p
  JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO
  JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
 WHERE REPLACE(n.PLNNCNTA, '.', '') = '23909011';


-- =====================================================================================
-- 5. DESPUES DE ESTO, EN LA PANTALLA
-- =====================================================================================
-- Reintentar el pago del sepelio. El error TIPO_APORTE_SIN_PRODUCTO no deberia volver.
-- ⚠️ Si aparece OTRO error, NO lo fuerces: pasamelo. El siguiente eslabon del camino es
--    la orden de pago en CXP, que es de otro equipo, y ahi conviene mirar antes de tocar.
--
-- ⚠️ Y avisar a omen-saa-2 que estas dos filas se crearon en su parametria.


-- =====================================================================================
-- 6. REVERSO — comentado
-- =====================================================================================
-- En orden inverso, y SOLO si todavia no se pago ningun sepelio con este producto:
--
-- UPDATE CRD.TPAP SET TPAPPRDP = NULL WHERE TPAPCDGO = 27;
-- DELETE FROM PGS.PRDP WHERE NOMBRE = 'Valores de Seguro a Beneficiarios';
-- DELETE FROM PGS.GRPP WHERE GRPPNMBR = 'VALORES DE SEGURO A BENEFICIARIOS';
-- COMMIT;

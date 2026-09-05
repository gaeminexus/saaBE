-- =====================================================================================
-- 201 - Que crear para el producto de pago de PENSION A JUBILADOS (H41)
-- FECHA: 2026-09-05 · EQUIPO: CRD / Equipo B (eqB, omen-saa-1)
--
-- SOLO SELECT. No modifica una sola fila. El usuario crea el producto por pantalla.
-- Sin comandos de SQL*Plus: no usa PROMPT, DEFINE, SET ni &variables.
--
-- =====================================================================================
-- PARA QUE SIRVE
-- =====================================================================================
-- La corrida de jubilados tiene que mandar un DESGLOSE contable en la orden de pago de
-- cada jubilado. Sin desglose, tesoreria NO genera el asiento del pago: es su diseno,
-- no un defecto (H41). Ese desglose se clasifica con un PRODUCTO DE PAGO, igual que el
-- 516 clasifica el pago del seguro medico al proveedor.
--
-- Entonces hace falta UN producto de pago nuevo, apuntando a la cuenta
-- 2.3.01.10.03 - PENSIONES COMPLEMENTARIAS POR PAGAR.
--
-- Esa es la cuenta que el DEVENGO acredita (plantilla 35, aux1=2). El pago la DEBE y la
-- cierra. Si el producto apuntara a otra cuenta, el pasivo quedaria abierto para siempre
-- y nadie se enteraria hasta cerrar el ejercicio.
--
-- ⚠️ NO adivinar la configuracion copiando de memoria. El bloque 1 muestra como quedo
-- el 516 realmente, que es el unico molde probado en produccion.
-- =====================================================================================

-- ==========================================================================
-- BLOQUE 1 - (!) EL MOLDE: como esta configurado el producto 516 (seguro)
-- ==========================================================================
-- Es el unico producto de pago de este proceso que ya funciono en produccion: genero el
-- asiento del pago a PETROECUADOR. Lo que se cree para pensiones tiene que ser igual en
-- todo, cambiando SOLO la cuenta del grupo y el nombre.

SELECT p.ID                     AS ID_PRODUCTO,
       p.NOMBRE                 AS NOMBRE,
       p.CODIGO                 AS CODIGO,
       p.EMPRESA                AS ID_EMPRESA,
       p.ESTADO                 AS ESTADO,
       p.GRUPOPRODUCTO          AS ID_GRUPO,
       g.GRPPNMBR               AS GRUPO_NOMBRE,
       g.GRPPESTD               AS GRUPO_ESTADO,
       n.PLNNCNTA               AS CUENTA_CONTABLE,
       n.PLNNNMBR               AS CUENTA_NOMBRE,
       p.PRECIOUNITARIO         AS PRECIO_UNITARIO,
       p.INCLUYEIVA             AS INCLUYE_IVA,
       p.TIPOIVA                AS TIPO_IVA,
       p.TIPOICE                AS TIPO_ICE,
       p.ICE                    AS ICE,
       p.DESCUENTO              AS DESCUENTO,
       p.SUBSIDIO               AS SUBSIDIO,
       p.IRBPNR                 AS IRBPNR,
       p.STOCK                  AS STOCK,
       p.MANEJAUNIDAD           AS MANEJA_UNIDAD,
       p.DESCRIPCION            AS DESCRIPCION
  FROM PGS.PRDP p
  LEFT JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO
  LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
 WHERE p.ID = 516;

--
-- COMO SE LEE:
-- (!) CUENTA_CONTABLE deberia ser la del seguro por pagar (2.3.90.90.06 segun la
--     plantilla 35, aux1=4). Si sale otra, avisar ANTES de crear nada: significa que el
--     516 no esta donde creemos y el molde no sirve.
-- (!) Anotar ID_EMPRESA, ESTADO y todos los campos de impuestos/precio: el producto
--     nuevo va con LOS MISMOS. Un producto de pago aca solo clasifica un asiento, no
--     vende nada, asi que los de precio/IVA/ICE deberian ir en cero o sin aplicar.
-- (!) Si no devuelve ninguna fila, el 516 no existe en ESTA base — se esta mirando la
--     base equivocada. Parar.
--

-- ==========================================================================
-- BLOQUE 2 - (!) Ya existe un grupo apuntando a 2.3.01.10.03?
-- ==========================================================================
-- Antes de crear un grupo nuevo hay que ver si ya hay uno. Puede haberse creado por otro
-- motivo, y tener dos grupos sobre la misma cuenta es una fuente de ambiguedad futura.

SELECT g.GRPPCDGO              AS ID_GRUPO,
       g.GRPPNMBR              AS GRUPO_NOMBRE,
       g.GRPPESTD              AS GRUPO_ESTADO,
       n.PLNNCDGO              AS ID_CUENTA,
       n.PLNNCNTA              AS CUENTA_CONTABLE,
       n.PLNNNMBR              AS CUENTA_NOMBRE
  FROM PGS.GRPP g
  JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
 WHERE n.PLNNCNTA = '2.3.01.10.03';

--
-- (!) Si devuelve UNA fila activa  -> reusar ese grupo, no crear otro.
-- (!) Si devuelve NINGUNA          -> crear el grupo nuevo sobre esa cuenta.
-- (!) Si devuelve MAS DE UNA       -> avisar antes de elegir. Elegir "el primero" es el
--     mismo defecto del get(0) sin ORDER BY que ya mordio con los dos CERTIFICADO
--     BANCARIO de CRD.TPDJ.
--

-- ==========================================================================
-- BLOQUE 3 - La cuenta destino existe y esta activa?
-- ==========================================================================
-- Si la cuenta no existiera, el grupo no se puede crear y conviene saberlo ahora y no
-- a mitad de la pantalla.

SELECT n.PLNNCDGO              AS ID_CUENTA,
       n.PLNNCNTA              AS CUENTA_CONTABLE,
       n.PLNNNMBR              AS NOMBRE
  FROM CNT.PLNN n
 WHERE n.PLNNCNTA IN ('2.3.01.10.03', '2.3.90.90.06', '2.1.02.25.01')
 ORDER BY n.PLNNCNTA;

--
-- (!) Se esperan las TRES, y son las tres patas del proceso:
--     2.1.02.25.01  CTA INDIVIDUAL DE PENSIONES COMPLEMENTARIAS  (la que consume)
--     2.3.01.10.03  PENSIONES COMPLEMENTARIAS POR PAGAR          (la del pago, la nueva)
--     2.3.90.90.06  SEGURO DE SALUD POR PAGAR                    (la del 516)
-- (!) Si falta alguna, parar y avisar.
--

-- ==========================================================================
-- BLOQUE 4 - CONTROL POSTERIOR: correr DESPUES de crear el producto
-- ==========================================================================
-- Reemplazar el 516 por el ID del producto recien creado y comparar contra el bloque 1.
-- Tienen que salir identicos en todo, salvo ID, NOMBRE y la cuenta.

SELECT p.ID                     AS ID_PRODUCTO,
       p.NOMBRE                 AS NOMBRE,
       p.EMPRESA                AS ID_EMPRESA,
       p.ESTADO                 AS ESTADO,
       n.PLNNCNTA               AS CUENTA_CONTABLE,
       n.PLNNNMBR               AS CUENTA_NOMBRE
  FROM PGS.PRDP p
  LEFT JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO
  LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
 WHERE n.PLNNCNTA = '2.3.01.10.03';

--
-- (!) CUENTA_CONTABLE tiene que decir 2.3.01.10.03. Si dice otra cosa, el producto quedo
--     colgado del grupo equivocado y la corrida generaria el asiento contra una cuenta
--     que no es — sin dar ningun error.
-- (!) Anotar el ID_PRODUCTO que salga y pasarselo al equipo: va como constante
--     ID_PRODUCTO_PAGO_PENSION_JUBILADOS en el codigo, igual que el 516.
--
-- El codigo ademas trae un guard que verifica esta misma coincidencia antes de tocar al
-- primer jubilado, asi que si quedara mal la corrida se frena sola. Este bloque es para
-- enterarse antes y no perder el viaje.
--

-- =====================================================================================
-- No hay bloque de reverso: este script no escribe nada.
-- =====================================================================================

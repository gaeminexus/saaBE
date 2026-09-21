-- =====================================================================
-- e2-64 — Marcar como intermediario la factura 343 para que NO salga en
--          el ATS, SIN tocar su contabilidad
-- Modulo: cxp / sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-21
--
-- ⚠️ NO ES SOLO LECTURA: el BLOQUE 1 hace UPDATE sobre PGS.FCTC.
--    Correr el BLOQUE 0, LEERLO, y recien ahi el 1.
--
-- ⛔⛔ EL COMMIT DEL FINAL NO ES OPCIONAL ⛔⛔
--    Correr el script no guarda nada. El e2-52 se perdio exactamente asi.
--
-- POR QUE EXISTE — pedido del usuario, 2026-09-21, textual:
--   «Respecto a la factura 2868 solo no quiero que aparezca en el ATS, la
--    contabilidad si estuvo bien.»
--
--   El e2-63 midio que esa factura (ID 343, 001-001-000002868, AGENCIA DE
--   VIAJES R&M WORLDTRAVEL, 1521.17) tiene FCTCESIN = 0: la marca de
--   intermediario NUNCA SE GUARDO. El ATS la declaraba correctamente
--   segun la regla -- no habia ningun defecto que corregir, faltaba la
--   marca.
--
--   Se marca al PROCESAR el documento y en ningun otro lado
--   (ProcesoCargaDocumentosServiceImpl:1628, unico punto del backend), asi
--   que una factura ya registrada solo se marca por SQL.
--
-- QUE HACE Y QUE NO HACE ESTE SCRIPT
--   HACE : pone FCTCESIN = 1 en esa unica factura.
--   NO HACE: nada sobre el asiento contable. No anula ni recontabiliza.
--            El asiento que ya existe queda exactamente como esta, que es
--            lo que pidio el usuario.
--
--   ⚠️ POR ESO ESTE SCRIPT NO ES EL lap1-09. Ese es el procedimiento del
--      equipo de la laptop para cuando el asiento TAMBIEN esta mal: marca,
--      elige el producto (FCTCPRIN) y manda a anular contabilidad y
--      recontabilizar desde la pantalla. Aca no corresponde: la
--      contabilidad esta bien.
--
-- ⛔⛔ LA CONSECUENCIA QUE HAY QUE SABER ANTES DE CORRERLO ⛔⛔
--
--   Queda FCTCPRIN (producto de intermediario) en NULO, porque elegirlo es
--   una decision contable y aca no se necesita.
--
--   `generarAsientoFacturaCompra` elige su rama mirando FCTCESIN. Mientras
--   nadie toque el asiento de esta factura, no pasa nada. PERO si alguna
--   vez alguien pulsa "Anular contabilidad" + "Recontabilizar" sobre ella,
--   el generador va a tomar la rama de intermediario y va a buscar un
--   producto que no esta. **En ese momento hay que correr el lap1-09 para
--   elegir el producto ANTES de recontabilizar.**
--
--   Queda dicho aca y en el documento de estado para que quien lo
--   encuentre dentro de seis meses no lo descubra con el error en pantalla.
--
-- DOS EFECTOS MAS, que son los buscados y conviene tener presentes:
--   1. Sale del ATS (GeneradorAtsServiceImpl.comprasFacturaCompra filtra
--      `and (f.esIntermediario is null or f.esIntermediario <> 1)`).
--   2. Sale TAMBIEN del cuadre 104, desde el commit 8aaafe62 -- que es lo
--      que el usuario pidio el mismo dia: que los dos reportes declaren lo
--      mismo. Si por algun motivo el 104 debia seguir incluyendola, este
--      script NO es lo que hace falta: avisar al arbitro.
--
-- Columnas copiadas de la entidad FacturaCompra (PGS.FCTC: ID, NUMERO,
--   FECHA, SUBTOTAL, SUBCERO, ESTADO, TITULAR, FCTCESIN, FCTCPRIN).
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — Control previo. ANOTAR ESTA SALIDA: es el respaldo del
--            reverso (aunque el valor original es 0 y el reverso es
--            trivial, conviene dejar constancia de como estaba).
-- ESPERADO: UNA sola fila, con ID 343, ES_INTERMEDIARIO = 0, SUBTOTAL
--           1521.17 y BASE_0 1441.17.
-- ⛔ Si devuelve mas de una fila, o ninguna, PARAR y avisar: el bloque 1
--    apunta por ID y por numero justamente para no tocar otra cosa, pero
--    si el universo no es el esperado hay algo que no entendemos.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - antes' AS bloque,
       f.ID, f.NUMERO, t.TTLRIDNT AS ruc, t.TTLRNMBR AS proveedor, f.FECHA,
       f.FCTCESIN AS es_intermediario,
       f.FCTCPRIN AS producto_intermediario,
       f.ESTADO,
       NVL(f.SUBTOTAL,0) AS subtotal,
       NVL(f.SUBCERO,0)  AS base_0
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ID = 343
   AND f.NUMERO = '001-001-000002868';


-- ---------------------------------------------------------------------
-- BLOQUE 1 — UPDATE. Solo la marca, solo esa factura.
--            La doble condicion (ID y NUMERO) es a proposito: si el ID no
--            fuera el que creemos, no toca nada en vez de tocar otra fila.
-- ESPERADO: 1 fila actualizada. Si dice 0, el BLOQUE 0 ya lo habia dicho.
-- ---------------------------------------------------------------------
UPDATE PGS.FCTC f
   SET f.FCTCESIN = 1
 WHERE f.ID = 343
   AND f.NUMERO = '001-001-000002868'
   AND NVL(f.FCTCESIN,0) = 0;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Control posterior, ANTES del COMMIT.
-- ESPERADO: ES_INTERMEDIARIO = 1, y todo lo demas IGUAL que en el BLOQUE 0
--           (subtotal, base 0%, estado: este script no los toca).
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - despues' AS bloque,
       f.ID, f.NUMERO,
       f.FCTCESIN AS es_intermediario,
       f.FCTCPRIN AS producto_intermediario,
       f.ESTADO,
       NVL(f.SUBTOTAL,0) AS subtotal,
       NVL(f.SUBCERO,0)  AS base_0
  FROM PGS.FCTC f
 WHERE f.ID = 343;


-- ---------------------------------------------------------------------
-- BLOQUE 3 — Como queda el universo de marcadas, para que no sorprenda
--            despues: estas son TODAS las que el ATS y el cuadre 104 van
--            a dejar afuera.
-- ESPERADO: las 4 que ya habia + esta = 5. En agosto: 470 (AIG), 497 y 498
--           (Empresa Electrica, confirmadas correctas por el usuario) y la
--           343. Y una de septiembre por 158275.32.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 3 - todas las marcadas' AS bloque,
       TO_CHAR(f.FECHA, 'YYYY-MM') AS periodo,
       f.ID, f.NUMERO, t.TTLRNMBR AS proveedor,
       NVL(f.SUBTOTAL,0) AS subtotal
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ESTADO = 1
   AND NVL(f.FCTCESIN,0) = 1
 ORDER BY f.FECHA DESC, f.ID;


-- ⛔ SIN ESTE COMMIT NO SE GUARDA NADA DEL BLOQUE 1.
-- COMMIT;


-- =====================================================================
-- DESPUES DEL COMMIT: regenerar el ATS de 08/2026. La factura 343 ya no
-- tiene que aparecer, y el total de compras baja en 1521.17.
--
-- REVERSO — comentado. El valor original era 0:
--   UPDATE PGS.FCTC SET FCTCESIN = 0 WHERE ID = 343;
--   COMMIT;
-- Antes del COMMIT alcanza con:
--   ROLLBACK;
-- =====================================================================

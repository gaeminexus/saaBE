-- =====================================================================
-- e2-37 — Por que el ATS de agosto salio con CERO ventas, y que son los
--         18 anulados que declaramos
-- Modulo: sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-09
--
-- SOLO LECTURA. No inserta, no borra, no actualiza, no hace COMMIT.
-- Correr entero y pegar la salida completa.
--
-- POR QUE EXISTE
--   El AT082026.xml que genero el sistema trae <totalVentas>0.00</totalVentas>
--   y NINGUN <detalleVentas>. El ATS de julio, autorizado por el SRI, traia 19
--   ventas por 26.445,16. O agosto no tuvo ventas, o la consulta del generador
--   no las encuentra. Las dos posibilidades se ven distinto aca abajo y no hay
--   forma de distinguirlas leyendo el codigo.
--
--   El mismo archivo declaro 18 documentos en <anulados>, 8 de ellos sin
--   numero de autorizacion. El bloque 4 dice de que tabla salio cada uno.
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 1 — El facturador: a que empresa contable apunta, y su nombre corto
-- ESPERADO SI TODO ESTA BIEN: una fila, con EMPRESA distinta de NULL.
-- SI EMPRESA VIENE NULL O CON UN CODIGO QUE NO ES EL DE ASOPREP, ese es el
-- defecto: el generador filtra las ventas por f.facturador.empresa.codigo.
-- ---------------------------------------------------------------------
select 'BLOQUE 1 - facturador'    as bloque,
       f.ID                       as id_facturador,
       f.NUMDOC                   as ruc,
       f.NOMBRECOMERCIAL          as nombre_comercial,
       length(f.NOMBRECOMERCIAL)  as largo_nombre_comercial,
       length(f.RAZONSOCIAL)      as largo_razon_social,
       f.EMPRESA                  as empresa_contable
  from CBR.FCDR f
 where f.NUMDOC = '1791367596001';


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Facturas de AGOSTO 2026, sin ningun filtro de estado
-- ESPERADO SI AGOSTO TUVO VENTAS: una o mas filas con TOTAL_FACTURAS > 0.
-- Si devuelve CERO FILAS, agosto realmente no tuvo facturas y el ATS esta
-- bien en ese punto. Si devuelve filas, la consulta del generador las esta
-- descartando y las columnas de abajo dicen por cual de los tres filtros.
-- ---------------------------------------------------------------------
select 'BLOQUE 2 - facturas de agosto' as bloque,
       fc.EMPRESA                      as empresa_del_facturador,
       fa.ESTADO                       as estado,
       fa.ESTADOEMISION                as estado_emision,
       case when fa.COMPRADOR is null then 'SIN TITULAR' else 'CON TITULAR' end as titular,
       count(*)                        as total_facturas,
       sum(nvl(fa.SUBTOTAL,0) + nvl(fa.SUBCERO,0)) as suma_bases
  from CBR.FCTR fa
  join CBR.FCDR fc on fc.ID = fa.FACTURADOR
 where fa.FECHA >= date '2026-08-01'
   and fa.FECHA <  date '2026-09-01'
 group by fc.EMPRESA, fa.ESTADO, fa.ESTADOEMISION,
          case when fa.COMPRADOR is null then 'SIN TITULAR' else 'CON TITULAR' end
 order by 6 desc;


-- ---------------------------------------------------------------------
-- BLOQUE 3 — Lo mismo para JULIO 2026, que SI se declaro y fue autorizado
-- ESPERADO: 19 facturas o cerca. Sirve de ancla: los valores de ESTADO y
-- ESTADOEMISION que aparecen aca son los de una factura realmente emitida y
-- declarada. Si en julio y en agosto son los mismos, el problema NO es el
-- estado; si difieren, ahi esta.
-- ---------------------------------------------------------------------
select 'BLOQUE 3 - facturas de julio' as bloque,
       fc.EMPRESA                     as empresa_del_facturador,
       fa.ESTADO                      as estado,
       fa.ESTADOEMISION               as estado_emision,
       count(*)                       as total_facturas,
       sum(nvl(fa.SUBTOTAL,0) + nvl(fa.SUBCERO,0)) as suma_bases
  from CBR.FCTR fa
  join CBR.FCDR fc on fc.ID = fa.FACTURADOR
 where fa.FECHA >= date '2026-07-01'
   and fa.FECHA <  date '2026-08-01'
 group by fc.EMPRESA, fa.ESTADO, fa.ESTADOEMISION
 order by 5 desc;


-- ---------------------------------------------------------------------
-- BLOQUE 4 — Los 18 <anulados>: de que tabla salio cada uno
-- ESPERADO: el generador los busca en SIETE tablas, cuatro de ellas de COMPRA.
-- Un documento de compra es el que nos emitio un proveedor y NO puede ir en
-- <anulados>, que declara los secuenciales que emitimos nosotros.
-- Cada fila dice ademas si le falta la autorizacion, que es lo que hace que
-- el XML salga con <autorizacion></autorizacion> vacia.
-- ---------------------------------------------------------------------
select 'BLOQUE 4 - anulados' as bloque, 'CBR.FCTR (venta)' as tabla,
       d.TIPOCOMPROBANTE as tipo, d.SECUENCIAL as secuencial,
       case when d.AUTORIZACION is null then 'SIN AUTORIZACION' else 'ok' end as autorizacion
  from CBR.FCTR d
 where d.ESTADOEMISION = 3
   and d.FECHAANULACION >= date '2026-08-01' and d.FECHAANULACION < date '2026-09-01'
union all
select 'BLOQUE 4 - anulados', 'PGS.FCTC (COMPRA - no deberia ir)',
       d.TIPOCOMPROBANTE, d.SECUENCIAL,
       case when d.AUTORIZACION is null then 'SIN AUTORIZACION' else 'ok' end
  from PGS.FCTC d
 where d.ESTADOEMISION = 3
   and d.FCTCFCAN >= date '2026-08-01' and d.FCTCFCAN < date '2026-09-01';


-- ---------------------------------------------------------------------
-- BLOQUE 5 — Retenciones de compra de agosto, que el ATS hoy NO declara
-- ESPERADO: una o mas filas. Es el insumo del bloque <air> y de los seis
-- campos de retencion de IVA. CODIMPUESTO distingue renta de IVA; el reparto
-- por CODRETENCION esta en el anexo A.3 del diagnostico.
-- Si esto devuelve filas, confirma que nuestro ATS declaro CERO retenciones
-- teniendo retenciones que declarar.
-- ---------------------------------------------------------------------
select 'BLOQUE 5 - retenciones de compra' as bloque,
       d.CODIMPUESTO      as cod_impuesto,
       d.CODRETENCION     as cod_retencion,
       count(*)           as lineas,
       sum(nvl(d.VALORRETEN,0)) as valor_retenido
  from PGS.DRC2 d
  join PGS.RCV2 r on r.ID = d.RETENCIONV2
 where r.FECHA >= date '2026-08-01'
   and r.FECHA <  date '2026-09-01'
 group by d.CODIMPUESTO, d.CODRETENCION
 order by 2, 3;


-- ---------------------------------------------------------------------
-- BLOQUE 6 — Cuantos titulares no tienen parteRelacionada capturada
-- ESPERADO: casi todos en NULL. Es lo que hace que <parteRel> viaje vacio y
-- el validador lo rechace. El arreglo de hoy escribe 'NO' cuando esta NULL;
-- este bloque mide cuantos quedan pendientes de que contabilidad los marque.
-- ---------------------------------------------------------------------
select 'BLOQUE 6 - parteRel' as bloque,
       nvl(t.TTLRPREL, '(NULL)') as parte_relacionada,
       count(*)                  as titulares
  from TSR.TTLR t
 group by nvl(t.TTLRPREL, '(NULL)')
 order by 3 desc;

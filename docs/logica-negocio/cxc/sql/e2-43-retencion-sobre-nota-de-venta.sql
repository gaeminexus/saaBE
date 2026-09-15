-- =====================================================================
-- e2-43 — Antes de emitir retenciones sobre notas de venta: catalogo,
--          autorizaciones compartidas y retenciones ya emitidas mal
-- Modulo: cxc (retenciones) / cxp (notas de venta)
-- Equipo: omen-saa-2  ·  Fecha: 2026-09-15
--
-- ✅ SOLO LECTURA. No inserta, no actualiza, no borra, no hace COMMIT.
--    Correr entero y pegar la salida completa, CON la columna BLOQUE.
--
-- POR QUE EXISTE — ver cxc/PLAN-RETENCION-SOBRE-NOTA-DE-VENTA.md
--   1) El combo "Doc. que se retiene" sale de CBR.TSRI con LSRI = '3'.
--      Si no hay un '02' activo, la pantalla no puede ofrecer la nota de
--      venta aunque el codigo este listo.
--   2) El ATS enlaza retencion <-> compra por AUTORIZACION. La de una nota
--      de venta preimpresa es la del talonario y la comparten varias notas
--      del mismo proveedor: la retencion de una se pegaria a todas.
--   3) La validacion previa busca la compra por numero + proveedor, sin
--      mirar el tipo: una factura y una nota de venta con el mismo numero
--      bloquean la emision con "mas de una factura".
--   4) El selector de "Factura" HOY ya lista las notas de venta mezcladas:
--      alguna retencion pudo salir al SRI declarando '01' sobre una '02'.
--
-- Nombres de columna copiados de las entidades, no de memoria:
--   Tsri (CBR.TSRI: ID, LSRI->LSRI.TABLA, CODIGO, DETALLE, ESTADO)
--   FacturaCompra (PGS.FCTC: ID, TIPOCOMPROBANTE, EMPRESA, TITULAR, NUMERO,
--                  AUTORIZACION, FECHA, TOTAL, ESTADO, FCTCEPAG)
--   RetencionV2 (CBR.RTV2: ID, PROVEEDOR, NUMERO, FECHA, ESTADO, ESTADOEMISION)
--   DetalleRetencionV2 (CBR.DRV2: RETENCIONV2, TIPODOCRETEN, NUMDOCRETEN,
--                  DOCRESAUTORIZACION, VALORRETEN, ESTADO)
--   Titular (TSR.TTLR: TTLRCDGO, TTLRIDNT, TTLRNMBR)
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 1 — Tipos de documento que ofrece el combo de la retencion
-- ESPERADO: una fila con CODIGO = '02' y ESTADO = 1.
--           Si no aparece, o aparece con ESTADO distinto de 1, la opcion
--           "nota de venta" NO va a salir en pantalla: avisar al arbitro.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - tipos de documento (LSRI 3)' AS bloque,
       t.ID, t.CODIGO, t.DETALLE, t.ESTADO
  FROM CBR.TSRI t
 WHERE t.LSRI = '3'
 ORDER BY t.CODIGO;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Cuantos documentos de compra hay por tipo
-- ESPERADO: '01' con la mayoria, '02' con las notas de venta manuales.
--           Filas con TIPOCOMPROBANTE nulo: el frontend las va a tratar
--           como factura. Anotar cuantas son.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - FCTC por tipo' AS bloque,
       NVL(f.TIPOCOMPROBANTE, '(nulo)') AS tipo_comprobante,
       f.ESTADO,
       COUNT(*)     AS cantidad,
       SUM(f.TOTAL) AS total
  FROM PGS.FCTC f
 GROUP BY NVL(f.TIPOCOMPROBANTE, '(nulo)'), f.ESTADO
 ORDER BY 2, 3;


-- ---------------------------------------------------------------------
-- BLOQUE 3 — Autorizaciones compartidas por mas de un documento que
--            incluye al menos una nota de venta (la trampa del ATS)
-- ESPERADO: CERO filas = hoy la trampa no se da.
--           Cada fila es una autorizacion con la que el ATS mezclaria
--           retenciones entre CANTIDAD documentos del mismo proveedor.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 3 - autorizacion compartida' AS bloque,
       f.EMPRESA,
       f.TITULAR,
       t.TTLRIDNT,
       t.TTLRNMBR,
       f.AUTORIZACION,
       COUNT(*) AS cantidad,
       SUM(CASE WHEN f.TIPOCOMPROBANTE = '02' THEN 1 ELSE 0 END) AS notas_de_venta,
       MIN(f.NUMERO) AS primer_numero,
       MAX(f.NUMERO) AS ultimo_numero
  FROM PGS.FCTC f
  LEFT JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.AUTORIZACION IS NOT NULL
 GROUP BY f.EMPRESA, f.TITULAR, t.TTLRIDNT, t.TTLRNMBR, f.AUTORIZACION
HAVING COUNT(*) > 1
   AND SUM(CASE WHEN f.TIPOCOMPROBANTE = '02' THEN 1 ELSE 0 END) > 0
 ORDER BY cantidad DESC;


-- ---------------------------------------------------------------------
-- BLOQUE 3b — Notas de venta SIN autorizacion
-- ESPERADO: CANTIDAD = 0. Sin autorizacion el ATS no puede enlazarles
--           ninguna retencion.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 3b - notas de venta sin autorizacion' AS bloque,
       COUNT(*) AS cantidad
  FROM PGS.FCTC f
 WHERE f.TIPOCOMPROBANTE = '02'
   AND f.AUTORIZACION IS NULL;


-- ---------------------------------------------------------------------
-- BLOQUE 4 — Mismo numero en mas de un documento del mismo proveedor,
--            con al menos una nota de venta (bloquea la validacion previa)
-- ESPERADO: CERO filas. Si hay filas, la retencion sobre esa nota de venta
--           se va a rechazar con "Existe mas de una factura de compra".
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 4 - numero repetido' AS bloque,
       f.EMPRESA,
       f.TITULAR,
       REPLACE(f.NUMERO, '-', '') AS numero_sin_guiones,
       COUNT(*) AS cantidad,
       LISTAGG(NVL(f.TIPOCOMPROBANTE, '(nulo)') || ':' || f.ID, ', ')
         WITHIN GROUP (ORDER BY f.ID) AS tipo_e_id
  FROM PGS.FCTC f
 GROUP BY f.EMPRESA, f.TITULAR, REPLACE(f.NUMERO, '-', '')
HAVING COUNT(*) > 1
   AND SUM(CASE WHEN f.TIPOCOMPROBANTE = '02' THEN 1 ELSE 0 END) > 0;


-- ---------------------------------------------------------------------
-- BLOQUE 5 — Retenciones YA emitidas sobre una nota de venta
--            (por captura manual o eligiendola en la lista de "Factura")
-- ESPERADO: idealmente CERO filas.
--   TIPODOCRETEN = '02' -> se capturo a mano, declarada bien.
--   TIPODOCRETEN = '01' -> 🔴 el SRI recibio una nota de venta declarada
--                          como factura. Anotar RTV2.ID, ESTADO y NUMERO.
--   ESTADO de RTV2: 5 = autorizada (las que ya cuentan ante el SRI).
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 5 - retenciones sobre notas de venta' AS bloque,
       r.ID            AS id_retencion,
       r.NUMERO        AS numero_retencion,
       r.FECHA,
       r.ESTADO,
       r.ESTADOEMISION,
       d.TIPODOCRETEN,
       d.NUMDOCRETEN,
       f.ID            AS id_nota_venta,
       f.NUMERO        AS numero_nota_venta,
       SUM(d.VALORRETEN) AS valor_retenido
  FROM CBR.DRV2 d
  JOIN CBR.RTV2 r ON r.ID = d.RETENCIONV2
  JOIN PGS.FCTC f ON f.TITULAR = r.PROVEEDOR
                 AND REPLACE(f.NUMERO, '-', '') = REPLACE(d.NUMDOCRETEN, '-', '')
                 AND f.TIPOCOMPROBANTE = '02'
 GROUP BY r.ID, r.NUMERO, r.FECHA, r.ESTADO, r.ESTADOEMISION,
          d.TIPODOCRETEN, d.NUMDOCRETEN, f.ID, f.NUMERO
 ORDER BY r.ID;

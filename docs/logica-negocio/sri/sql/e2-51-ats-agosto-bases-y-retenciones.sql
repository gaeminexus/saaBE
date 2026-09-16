-- =====================================================================
-- e2-51 — ATS de agosto: por que la base 0% sale como "base IVA diferente
--          de 0%", y por que las retenciones salen en 0
-- Modulo: sri / cxp  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-16
--
-- ✅ SOLO LECTURA. Correr entero y pegar la salida con la columna BLOQUE.
--
-- POR QUE EXISTE — dos reportes del usuario sobre el ATS de agosto:
--   (1) "las facturas que tienen base 0 se estan reportando en la columna
--        BASE IVA DIFERENTE 0% cuando deberian aparecer en BASE IVA 0%"
--        (captura del DIMM: BASE IVA 0% = 7.23 en todas las filas del
--        proveedor 1790053881001, y el resto en la otra columna);
--   (2) "no esta incluyendo los valores de retenciones, salen siempre en 0".
--
--   El XML del ATS escribe baseImponible = PGS.FCTC.SUBCERO y
--   baseImpGrav = SUBTOTAL - SUBCERO (GeneradorAtsServiceImpl:828-829).
--   O sea: si la base 0% sale mal, es porque SUBCERO esta mal grabado en la
--   carga -- es la consecuencia conocida de la decision "NO se recalculan las
--   compras ya cargadas" (§40.7 del estado). Estos bloques lo miden contra el
--   detalle, que SI guarda el codigo de tarifa por linea (DFCC.CODIGOIVASRI:
--   0 = 0%, 6 = no objeto, 7 = exento, el resto gravado).
--
-- Nombres de columna copiados de las entidades:
--   FacturaCompra (PGS.FCTC), DetalleFacturaCompra (PGS.DFCC),
--   RetencionV2 (CBR.RTV2), DetalleRetencionV2 (CBR.DRV2),
--   RetencionCompraV2 (PGS.RCV2), DetalleRetencionCompraV2 (PGS.DRC2),
--   Titular (TSR.TTLR).
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 1 — Las 8 compras de la captura: cabecera contra detalle
-- ESPERADO: BASE_0_CABECERA (lo que el ATS declara como BASE IVA 0%) igual a
--           BASE_0_DETALLE. Si difieren, la cabecera quedo mal en la carga.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - proveedor de la captura' AS bloque,
       f.ID, f.NUMERO, f.FECHA,
       NVL(f.SUBTOTAL, 0)                       AS subtotal_cabecera,
       NVL(f.SUBCERO, 0)                        AS base_0_cabecera,
       NVL(f.SUBTOTAL, 0) - NVL(f.SUBCERO, 0)   AS base_gravada_que_declara_ats,
       NVL(f.VIVA, 0)                           AS iva_cabecera,
       NVL(f.TOTAL, 0)                          AS total,
       (SELECT NVL(SUM(d.BASEIMPONIBLE), 0) FROM PGS.DFCC d
         WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI, -1) = 0)  AS base_0_detalle,
       (SELECT NVL(SUM(d.BASEIMPONIBLE), 0) FROM PGS.DFCC d
         WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI, -1) NOT IN (0, 6, 7)) AS base_gravada_detalle,
       (SELECT NVL(SUM(d.VALORIVA), 0) FROM PGS.DFCC d WHERE d.FACTURA = f.ID) AS iva_detalle,
       (SELECT COUNT(*) FROM PGS.DFCC d WHERE d.FACTURA = f.ID)  AS lineas
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE REPLACE(t.TTLRIDNT, ' ', '') = '1790053881001'
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
 ORDER BY f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Todas las compras de agosto: cuantas tienen la base mal
-- ESPERADO: CANTIDAD = 0 en la fila 'DIFIEREN'. Lo que salga ahi es lo que
--           el ATS de agosto esta declarando en la columna equivocada.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - compras de agosto' AS bloque,
       CASE WHEN ABS(NVL(f.SUBCERO, 0) - (SELECT NVL(SUM(d.BASEIMPONIBLE), 0) FROM PGS.DFCC d
              WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI, -1) = 0)) < 0.005
            THEN 'COINCIDEN' ELSE 'DIFIEREN' END AS estado_base_0,
       COUNT(*)                  AS cantidad,
       SUM(NVL(f.SUBCERO, 0))    AS suma_base_0_cabecera,
       SUM((SELECT NVL(SUM(d.BASEIMPONIBLE), 0) FROM PGS.DFCC d
             WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI, -1) = 0)) AS suma_base_0_detalle
  FROM PGS.FCTC f
 WHERE f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
   AND f.ESTADO = 1
 GROUP BY CASE WHEN ABS(NVL(f.SUBCERO, 0) - (SELECT NVL(SUM(d.BASEIMPONIBLE), 0) FROM PGS.DFCC d
              WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI, -1) = 0)) < 0.005
            THEN 'COINCIDEN' ELSE 'DIFIEREN' END;


-- ---------------------------------------------------------------------
-- BLOQUE 3 — Las retenciones que ASOPREP emitio en agosto, y si enlazan
-- El ATS las pega a la compra por AUTORIZACION + NUMERO del documento
-- sustento. Este bloque muestra las dos puntas.
-- ESPERADO: ENLAZA = 'SI' en todas. Cada 'NO' es una retencion que el ATS
--           declara en 0.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 3 - retenciones emitidas de agosto' AS bloque,
       r.ID AS id_retencion, r.NUMERO AS numero_retencion, r.FECHA, r.ESTADO,
       d.TIPODOCRETEN, d.NUMDOCRETEN, d.DOCRESAUTORIZACION,
       d.CODIMPUESTO, d.CODRETENCION, d.VALORRETEN,
       (SELECT COUNT(*) FROM PGS.FCTC f
         WHERE f.AUTORIZACION = d.DOCRESAUTORIZACION)                       AS compras_con_esa_autorizacion,
       (SELECT COUNT(*) FROM PGS.FCTC f
         WHERE f.AUTORIZACION = d.DOCRESAUTORIZACION
           AND REPLACE(f.NUMERO, '-', '') = REPLACE(d.NUMDOCRETEN, '-', '')) AS compras_con_autorizacion_y_numero,
       CASE WHEN EXISTS (SELECT 1 FROM PGS.FCTC f
                          WHERE f.AUTORIZACION = d.DOCRESAUTORIZACION
                            AND REPLACE(f.NUMERO, '-', '') = REPLACE(d.NUMDOCRETEN, '-', ''))
            THEN 'SI' ELSE 'NO' END AS enlaza
  FROM CBR.RTV2 r
  JOIN CBR.DRV2 d ON d.RETENCIONV2 = r.ID
 WHERE r.FECHA >= DATE '2026-08-01' AND r.FECHA < DATE '2026-09-01'
 ORDER BY r.ID, d.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 4 — Retenciones RECIBIDAS (PGS.RCV2/DRC2): ¿el detalle tiene datos?
-- El usuario reporta que en Consulta de documentos, al abrir una retencion,
-- las columnas del detalle salen en 0.
-- ESPERADO: BASEIMPONIBLE y VALORRETEN con valores reales. Si estan en 0 o
--           nulos en la BASE, el defecto es de la carga, no de la pantalla.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 4 - detalle de retenciones recibidas' AS bloque,
       d.RETENCIONV2 AS id_retencion, d.ID AS id_detalle,
       d.CODIMPUESTO, d.CODRETENCION, d.TIPODOCRETEN, d.NUMDOCRETEN,
       d.BASEIMPONIBLE, d.PORCENTAJERETEN, d.VALORRETEN, d.ESTADO
  FROM PGS.DRC2 d
 WHERE d.RETENCIONV2 IN (
         SELECT r.ID FROM PGS.RCV2 r
          WHERE r.FECHA >= DATE '2026-08-01' AND r.FECHA < DATE '2026-09-01')
 ORDER BY d.RETENCIONV2, d.ID
 FETCH FIRST 40 ROWS ONLY;

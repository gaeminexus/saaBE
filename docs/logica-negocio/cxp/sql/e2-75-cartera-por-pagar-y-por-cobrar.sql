-- =====================================================================================
-- e2-75 — Cartera por pagar y por cobrar: los números de control para contrastar las dos
--         pantallas nuevas (API-CARTERA-CXP-CXC.md), y los datos que definen la antigüedad.
-- SOLO LECTURA. No modifica nada. Equipo omen-saa-2, 2026-09-30.
--
-- Saldo de un documento = TOTAL − suma de sus aplicaciones ACTIVAS (estado 1) con fecha de
-- aplicación <= corte. Es la misma fórmula de /aplp/saldo y /aplc/saldo
-- (AplicacionPagoCxpServiceImpl:1089, AplicacionPagoCxcServiceImpl:817) más el corte.
-- Pagos, NC, retenciones, anticipos cruzados, ND (negativas) y caja chica: TODO es una fila de
-- PGS.APLP / CBR.APLC. Nombres de columna copiados de las entidades.
--
-- ⚠️ La fecha de corte está escrita en cada bloque como DATE '2026-09-30'. Para otro corte,
--    reemplazarla en TODOS los bloques (buscar y reemplazar).
-- =====================================================================================

-- BLOQUE 1 — POR PAGAR: facturas de compra (incluye notas de venta, tipo 02) y liquidaciones
-- Documentos activos, no anulados, emitidos hasta el corte, con saldo > 0,01.
-- Estos totales son los que la pantalla «Cuentas por pagar» tiene que dar con el mismo corte.
SELECT 'BLOQUE 1 - por pagar' AS bloque, tipo, COUNT(*) AS documentos, SUM(saldo) AS saldo_total
  FROM (
        SELECT CASE WHEN f.TIPOCOMPROBANTE = '02' THEN 'NOTA DE VENTA' ELSE 'FACTURA' END AS tipo,
               f.TOTAL - NVL((SELECT SUM(a.APLPMAPL) FROM PGS.APLP a
                               WHERE a.APLPFCTC = f.ID AND a.APLPESTD = 1
                                 AND a.APLPFAPL <= DATE '2026-09-30'), 0) AS saldo
          FROM PGS.FCTC f
         WHERE f.ESTADO = 1 AND NVL(f.ESTADOEMISION, 0) <> 3
           AND TRUNC(f.FECHA) <= DATE '2026-09-30'
        UNION ALL
        SELECT 'LIQUIDACION' AS tipo,
               l.TOTAL - NVL((SELECT SUM(a.APLPMAPL) FROM PGS.APLP a
                               WHERE a.APLPLQCC = l.ID AND a.APLPESTD = 1
                                 AND a.APLPFAPL <= DATE '2026-09-30'), 0) AS saldo
          FROM PGS.LQCC l
         WHERE l.ESTADO = 1 AND NVL(l.ESTADOEMISION, 0) <> 3
           AND TRUNC(l.FECHA) <= DATE '2026-09-30'
       )
 WHERE ABS(saldo) > 0.01
 GROUP BY tipo
 ORDER BY tipo;

-- BLOQUE 2 — POR COBRAR: facturas de venta vigentes
-- «Vigente» = CriterioVentaVigente: ESTADO = 5 (autorizada) y ESTADOEMISION <> 3 (anulada).
-- El javadoc de ese criterio dice «SIN CONFIRMAR contra la base»: el bloque 4 lo contrasta.
SELECT 'BLOQUE 2 - por cobrar' AS bloque, COUNT(*) AS documentos, SUM(saldo) AS saldo_total
  FROM (
        SELECT f.TOTAL - NVL((SELECT SUM(a.APLCMAPL) FROM CBR.APLC a
                               WHERE a.APLCFCTR = f.ID AND a.APLCESTD = 1
                                 AND a.APLCFAPL <= DATE '2026-09-30'), 0) AS saldo
          FROM CBR.FCTR f
         WHERE f.ESTADO = 5 AND NVL(f.ESTADOEMISION, 0) <> 3
           AND TRUNC(f.FECHA) <= DATE '2026-09-30'
       )
 WHERE ABS(saldo) > 0.01;

-- BLOQUE 3 — Los plazos de pago que definen el vencimiento (antigüedad)
-- No hay columna de vencimiento: sale de FECHA + PLAZO de la forma de pago del XML.
-- Muestra qué valores de UNIDADTIEMPO existen y cuántos documentos traen plazo.
-- ESPERADO: UNIDADTIEMPO en «dias» (o parecido). Cualquier otro valor hay que mapearlo.
SELECT 'BLOQUE 3 - plazos' AS bloque, origen, UPPER(TRIM(unidad)) AS unidad,
       COUNT(*) AS filas, MIN(plazo) AS plazo_min, MAX(plazo) AS plazo_max
  FROM (
        SELECT 'FPFM compra' AS origen, p.UNIDADTIEMPO AS unidad, p.PLAZO AS plazo FROM PGS.FPFM p
        UNION ALL
        SELECT 'FPLM liquidacion', p.UNIDADTIEMPO, p.PLAZO FROM PGS.FPLM p
        UNION ALL
        SELECT 'FPFC venta', p.UNIDADTIEMPO, p.PLAZO FROM CBR.FPFC p
       )
 GROUP BY origen, UPPER(TRIM(unidad))
 ORDER BY origen, filas DESC;

-- BLOQUE 4 — Contraste del criterio de venta vigente
-- Distribución de ESTADO/ESTADOEMISION en las facturas de venta. Si hay facturas COBRADAS o con
-- aplicaciones en un estado distinto de 5, el criterio deja cartera afuera: avisar al árbitro.
SELECT 'BLOQUE 4 - estados de venta' AS bloque, f.ESTADO, f.ESTADOEMISION,
       COUNT(*) AS facturas,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM CBR.APLC a WHERE a.APLCFCTR = f.ID AND a.APLCESTD = 1)
                THEN 1 ELSE 0 END) AS con_aplicaciones
  FROM CBR.FCTR f
 GROUP BY f.ESTADO, f.ESTADOEMISION
 ORDER BY f.ESTADO, f.ESTADOEMISION;

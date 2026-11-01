-- =====================================================================================
-- omen1-01 (cxp) — SOLO LECTURA. omen-saa-1, 2026-11-01.
-- Correo de contabilidad (Katherine Pardo, 29-10-2026): las NC de compra 002-002-000035370 y
-- 002-002-000035420 afectan a facturas de INTERMEDIARIO (contabilizadas a la cuenta del grupo del producto
-- intermediario, p. ej. 1.4.90.90.10), pero su asiento se fue a «servicios básicos».
-- Causa en el código (verificada por el árbitro): AsientoContableServiceImpl.generarAsientoNotaCreditoCompra
-- acredita SIEMPRE la «cuenta de gasto por defecto» (obtenerCuentaGastoDefaultCxp = el grupo de producto
-- activo de MENOR código de la empresa) por el subtotal, más IVA aparte. No mira la factura que modifica:
-- para una factura de intermediario (DEBE grupo del producto intermediario por el TOTAL, sin IVA) la NC
-- tendría que acreditar ESA cuenta por el TOTAL, sin IVA.
-- Este script mide los dos casos del correo y TODOS los demás afectados (para el UPDATE).
-- =====================================================================================

-- 1. Los dos casos del correo: la NC, su asiento y la factura que modifica.
SELECT n.ID AS NC_ID, n.NUMERO AS NC, n.TOTAL AS NC_TOTAL, n.ESTADO AS NC_ESTADO, n.ASIENTO,
       n.TIPODOCMODIFICADO, n.NUMDOCMODIFICADO,
       f.ID AS FC_ID, f.NUMERO AS FC, f.TOTAL AS FC_TOTAL, f.FCTCESIN AS ES_INTERMEDIARIO, f.FCTCPRIN AS PRODUCTO_INTERM,
       g.GRPPNMBR AS GRUPO_INTERM, ng.PLNNCDGO AS PLNN_INTERM, ng.PLNNCNTA AS CUENTA_INTERM, ng.PLNNNMBR AS NOMBRE_INTERM
  FROM PGS.NTCC n
  LEFT JOIN PGS.FCTC f ON f.TITULAR = n.TITULAR AND f.NUMERO = n.NUMDOCMODIFICADO AND f.ESTADO <> 0
  LEFT JOIN PGS.PRDP p ON p.ID = f.FCTCPRIN
  LEFT JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO
  LEFT JOIN CNT.PLNN ng ON ng.PLNNCDGO = g.PLNNCDGO
 WHERE n.NUMERO IN ('002-002-000035370', '002-002-000035420')
 ORDER BY n.NUMERO;

-- 2. Las líneas de sus asientos tal como quedaron.
SELECT n.NUMERO AS NC, d.DTASCDGO, d.PLNNCDGO, d.DTASCNTA, d.DTASNMCT, d.DTASDSCR, d.DTASDBEE, d.DTASHBRR
  FROM PGS.NTCC n JOIN CNT.DTAS d ON d.ASNTCDGO = n.ASIENTO
 WHERE n.NUMERO IN ('002-002-000035370', '002-002-000035420')
 ORDER BY n.NUMERO, d.DTASCDGO;

-- 3. TODAS las NC de compra vivas con asiento cuya factura modificada es de INTERMEDIARIO (los casos a
--    corregir, hoy y cualquier otro que ya exista).
SELECT n.ID AS NC_ID, n.NUMERO AS NC, n.TOTAL, n.ASIENTO, a.ASNTFCHA, a.ASNTESTD, a.MYRZCDGO,
       f.NUMERO AS FC, ng.PLNNCNTA AS CUENTA_CORRECTA
  FROM PGS.NTCC n
  JOIN CNT.ASNT a ON a.ASNTCDGO = n.ASIENTO
  JOIN PGS.FCTC f ON f.TITULAR = n.TITULAR AND f.NUMERO = n.NUMDOCMODIFICADO AND f.ESTADO <> 0
  JOIN PGS.PRDP p ON p.ID = f.FCTCPRIN
  JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO
  JOIN CNT.PLNN ng ON ng.PLNNCDGO = g.PLNNCDGO
 WHERE f.FCTCESIN = 1 AND n.ESTADO <> 0 AND a.ASNTESTD IN (1, 3)
 ORDER BY n.ID;

-- 4. La «cuenta de gasto por defecto» que usa el código hoy (debería ser servicios básicos).
SELECT * FROM (
  SELECT g.GRPPCDGO, g.GRPPNMBR, n.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR
    FROM PGS.GRPP g JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
   WHERE g.PJRQCDGO = 1236 AND g.GRPPESTD = 1 ORDER BY g.GRPPCDGO)
 WHERE ROWNUM = 1;

-- 5. Para dimensionar: TODAS las NC de compra (intermediario o no) cuyo asiento acreditó esa cuenta por
--    defecto con la glosa del código («NC Compra reverso gasto»). Las de facturas NO intermediarias también
--    pueden estar mal (el reverso debería ir a la cuenta del gasto original de la factura).
SELECT n.ID AS NC_ID, n.NUMERO AS NC, n.NUMDOCMODIFICADO AS FC, d.DTASCNTA, d.DTASHBRR, a.ASNTFCHA
  FROM PGS.NTCC n JOIN CNT.ASNT a ON a.ASNTCDGO = n.ASIENTO JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO
 WHERE n.ESTADO <> 0 AND a.ASNTESTD IN (1, 3) AND d.DTASDSCR = 'NC Compra reverso gasto'
 ORDER BY n.ID;

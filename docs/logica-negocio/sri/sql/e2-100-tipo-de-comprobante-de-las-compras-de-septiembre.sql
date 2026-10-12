-- =====================================================================================
-- e2-100 — ¿Con qué tipo de comprobante quedaron grabadas las compras de septiembre? SOLO LECTURA.
-- Equipo omen-saa-2 · 2026-10-12.
--
-- Ana reporta el 2026-10-12, después del despliegue, que en el ATS «siguen sin salir las notas de venta».
-- El ATS (GeneradorAtsServiceImpl:591) NO deduce el tipo: copia PGS.FCTC.TIPOCOMPROBANTE tal cual, al XML y a
-- la pantalla. El arreglo de pantalla (saaFE 2a640c9, «02 — Nota de venta») todavía NO está desplegado.
-- Hay dos causas posibles y este script las separa:
--   A) La nota de venta SÍ está grabada con '02' → es solo pantalla; se resuelve desplegando 2a640c9.
--   B) La nota de venta está grabada con '01' (como factura) → el XML también la declara como factura,
--      el despliegue NO lo arregla, y hay que corregir los datos (y ver por qué entró con '01').
-- Columnas copiadas de FacturaCompra (FCTC).
-- =====================================================================================

-- BLOQUE 1 — Resumen de septiembre por tipo y por origen. ESPERADO: notas de venta con TIPOCOMPROBANTE '02'
-- (las 10 manuales del e2-93/e2-94 por lo menos). Si no aparece ninguna fila '02', es la causa B.
SELECT 'BLOQUE 1 - resumen' AS bloque, f.TIPOCOMPROBANTE, f.TIPODOC,
       CASE WHEN f.CLAVE IS NULL THEN 'MANUAL' ELSE 'XML' END AS origen,
       COUNT(*) AS documentos, SUM(f.TOTAL) AS total
  FROM PGS.FCTC f
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-09-01' AND f.FECHA < DATE '2026-10-01'
 GROUP BY f.TIPOCOMPROBANTE, f.TIPODOC, CASE WHEN f.CLAVE IS NULL THEN 'MANUAL' ELSE 'XML' END
 ORDER BY f.TIPOCOMPROBANTE, origen;

-- BLOQUE 2 — Las 10 notas de venta del e2-93/e2-94. ESPERADO: TIPOCOMPROBANTE '02' y SUBTOTAL = SUBCERO = TOTAL.
-- Si SUBTOTAL sigue en 0, el e2-94 no quedó comiteado y siguen en cero en el ATS.
SELECT 'BLOQUE 2 - las del e2-93' AS bloque, f.ID, f.NUMERO, f.TIPOCOMPROBANTE, f.TIPODOC, TRUNC(f.FECHA) AS fecha,
       f.SUBTOTAL, f.SUBCERO, f.TOTAL, f.AUTORIZACION
  FROM PGS.FCTC f
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-09-01' AND f.FECHA < DATE '2026-10-01'
   AND f.CLAVE IS NULL
 ORDER BY f.TIPOCOMPROBANTE, f.ID;

-- BLOQUE 3 — Candidatas a nota de venta grabadas como factura: manuales (sin clave de acceso) con tipo '01'.
-- Una nota de venta preimpresa trae autorización de 10 dígitos; una factura electrónica, de 49.
-- ESPERADO: ninguna con autorización de 10 dígitos. Las que salgan son la causa B: pasarme la lista.
SELECT 'BLOQUE 3 - manuales tipo 01' AS bloque, f.ID, f.NUMERO, TRUNC(f.FECHA) AS fecha, f.TIPOCOMPROBANTE,
       f.TIPODOC, LENGTH(TRIM(f.AUTORIZACION)) AS largo_autorizacion, f.TOTAL, f.OBSERVACION
  FROM PGS.FCTC f
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-09-01' AND f.FECHA < DATE '2026-10-01'
   AND f.CLAVE IS NULL
   AND f.TIPOCOMPROBANTE = '01'
 ORDER BY f.FECHA, f.ID;

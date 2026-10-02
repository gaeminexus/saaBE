-- =====================================================================================
-- e2-87 — Todas las facturas de compra de JULIO 2026 que todavía figuran con saldo. SOLO LECTURA.
-- Equipo omen-saa-2 · 2026-10-02.
--
-- Por qué: contabilidad respondió el 02-10 que «los saldos de las facturas de julio siguen con saldo», con
-- un ejemplo de CNEL EP (092-999-010966955 por 4,02 y 092-999-010968443 por 16,66) que NO estaba en la
-- lista original de seis (e2-82/e2-85, ya en saldo 0). Antes de regularizar otras, hay que saber CÓMO se
-- pagó cada una: un pago sin asiento solo se justifica si la contabilidad del pago ya existe (asiento de
-- diario, caja chica, débito). Esta lista es para que contabilidad la complete.
-- Columnas copiadas de FacturaCompra, AplicacionPagoCxp y PagoProgramado.
-- =====================================================================================

-- BLOQUE 1 — Facturas de compra emitidas en julio 2026, activas, con saldo > 0,01 hoy.
-- Para cada una, contabilidad indica: cómo se pagó y con qué asiento (o si de verdad se debe).
SELECT 'BLOQUE 1 - julio con saldo' AS bloque,
       f.ID, f.NUMERO, TRUNC(f.FECHA) AS fecha, NVL(t.TTLRRZSC, t.TTLRNMBR) AS proveedor, t.TTLRIDNT,
       f.TOTAL,
       NVL((SELECT SUM(a.APLPMAPL) FROM PGS.APLP a WHERE a.APLPFCTC = f.ID AND a.APLPESTD = 1), 0) AS aplicado,
       f.TOTAL - NVL((SELECT SUM(a.APLPMAPL) FROM PGS.APLP a WHERE a.APLPFCTC = f.ID AND a.APLPESTD = 1), 0) AS saldo,
       (SELECT COUNT(*) FROM PGS.PGTR p WHERE p.PGTRFCTC = f.ID AND p.PGTRESTD NOT IN (4, 5)) AS pagos_vigentes,
       f.FCTCESIN AS intermediario
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ESTADO = 1
   AND NVL(f.ESTADOEMISION, 0) <> 3
   AND f.FECHA >= DATE '2026-07-01' AND f.FECHA < DATE '2026-08-01'
   AND f.TOTAL - NVL((SELECT SUM(a.APLPMAPL) FROM PGS.APLP a WHERE a.APLPFCTC = f.ID AND a.APLPESTD = 1), 0) > 0.01
 ORDER BY proveedor, f.FECHA;

-- BLOQUE 2 — El total por proveedor, para dimensionar.
SELECT 'BLOQUE 2 - por proveedor' AS bloque, NVL(t.TTLRRZSC, t.TTLRNMBR) AS proveedor, COUNT(*) AS facturas,
       SUM(f.TOTAL - NVL((SELECT SUM(a.APLPMAPL) FROM PGS.APLP a WHERE a.APLPFCTC = f.ID AND a.APLPESTD = 1), 0)) AS saldo
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ESTADO = 1
   AND NVL(f.ESTADOEMISION, 0) <> 3
   AND f.FECHA >= DATE '2026-07-01' AND f.FECHA < DATE '2026-08-01'
   AND f.TOTAL - NVL((SELECT SUM(a.APLPMAPL) FROM PGS.APLP a WHERE a.APLPFCTC = f.ID AND a.APLPESTD = 1), 0) > 0.01
 GROUP BY NVL(t.TTLRRZSC, t.TTLRNMBR)
 ORDER BY saldo DESC;

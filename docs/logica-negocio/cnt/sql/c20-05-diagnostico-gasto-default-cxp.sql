-- =====================================================================================
-- c20-05 — SOLO LECTURA. omen-saa-1, 2026-11-01. Plan cuentas a plantilla, P11.
-- Hallazgo de la segunda revisión: AsientoContableServiceImpl.obtenerCuentaGastoDefaultCxp toma la cuenta
-- del GRUPO DE PRODUCTO DE PAGO ACTIVO CON EL MENOR CÓDIGO de la empresa (sin filtrar el tipo), y la usa
-- como gasto en:
--   - la nota de crédito de compra (HABER «reverso gasto»);
--   - la nota de débito de compra (DEBE de gasto);
--   - la liquidación de compra con detalles sin clasificar (DEBE «SIN CLASIFICAR»).
-- Va a pasar a una línea de la plantilla 38 (CXP COMPRAS - CUENTAS DE AJUSTE), aux1 2. Antes hay que
-- saber qué cuenta sale HOY, y contabilidad decide si es la correcta.
-- =====================================================================================

-- 1. La cuenta que el código elige hoy (el grupo activo con cuenta y menor código).
SELECT * FROM (
  SELECT g.GRPPCDGO, g.GRPPNMBR, g.GRPPRYYA, g.GRPPRZZA, n.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR
    FROM PGS.GRPP g JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
   WHERE g.PJRQCDGO = 1236 AND g.GRPPESTD = 1
   ORDER BY g.GRPPCDGO)
 WHERE ROWNUM = 1;

-- 2. Los grupos de tipo GASTOS GENERALES (lo que dice el javadoc que debería usar), para comparar.
--    (GRPPRZZA = detalle del rubro de tipo de grupo; ver TipoGrupoProductos.)
SELECT g.GRPPCDGO, g.GRPPNMBR, g.GRPPRYYA, g.GRPPRZZA, n.PLNNCNTA, n.PLNNNMBR, g.GRPPESTD
  FROM PGS.GRPP g LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
 WHERE g.PJRQCDGO = 1236 AND UPPER(g.GRPPNMBR) LIKE '%GASTO%'
 ORDER BY g.GRPPCDGO;

-- 3. Cuánto se ha contabilizado ya en esa cuenta desde los tres documentos (glosas típicas de esos
--    asientos). Si sale 0, el respaldo nunca se usó.
SELECT TO_CHAR(a.ASNTFCHA, 'YYYY-MM') AS MES, COUNT(DISTINCT a.ASNTCDGO) AS ASIENTOS,
       SUM(NVL(d.DTASDBEE, 0)) AS DEBE, SUM(NVL(d.DTASHBRR, 0)) AS HABER
  FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
 WHERE a.PJRQCDGO = 1236 AND a.ASNTESTD IN (1, 3)
   AND d.DTASCNTA = (SELECT n.PLNNCNTA FROM (
                       SELECT g.PLNNCDGO FROM PGS.GRPP g
                        WHERE g.PJRQCDGO = 1236 AND g.GRPPESTD = 1 AND g.PLNNCDGO IS NOT NULL
                        ORDER BY g.GRPPCDGO) x JOIN CNT.PLNN n ON n.PLNNCDGO = x.PLNNCDGO WHERE ROWNUM = 1)
   AND (UPPER(d.DTASDSCR) LIKE '%SIN CLASIFICAR%' OR UPPER(d.DTASDSCR) LIKE '%REVERSO GASTO%'
        OR UPPER(a.ASNTOBSR) LIKE '%NOTA DE D%BITO%COMPRA%' OR UPPER(a.ASNTOBSR) LIKE '%NOTA DE CR%DITO%COMPRA%'
        OR UPPER(a.ASNTOBSR) LIKE '%LIQUIDACI%N DE COMPRA%')
 GROUP BY TO_CHAR(a.ASNTFCHA, 'YYYY-MM')
 ORDER BY 1;

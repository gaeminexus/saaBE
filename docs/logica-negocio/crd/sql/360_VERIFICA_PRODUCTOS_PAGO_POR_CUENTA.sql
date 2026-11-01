-- =====================================================================================
-- 360 — SOLO LECTURA. omen-saa-1, 2026-11-01. Verifica que, con el EAR único, los pagos que ahora
-- resuelven su producto de pago POR LA CUENTA de una línea de plantilla encuentren EXACTAMENTE UNO.
-- Si alguna cuenta da 0 o más de 1 producto activo, ese proceso se detiene con un error claro.
--   N1  (rhh): pago de nómina por empleado      → cuenta de la línea 50 de la plantilla de pago (2.5.01).
--   P9  (crd): pago de jubilados, pensión         → cuenta 2.3.01.10.03 (antes producto 517 escrito).
--   P9  (crd): pago de jubilados, seguro médico   → cuenta 2.3.90.90.06 (antes producto 516 escrito).
--   B8  (crd): devolución de un descuento Petro   → cuenta de la línea 61 de la plantilla 21 (2.3.90.90.10).
-- ESPERADO: PRODUCTOS_ACTIVOS = 1 en las cuatro filas.
-- =====================================================================================

SELECT n.PLNNCNTA AS CUENTA, n.PLNNNMBR,
       (SELECT COUNT(*) FROM PGS.PRDP p JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO
         WHERE g.PLNNCDGO = n.PLNNCDGO AND g.PJRQCDGO = 1236 AND g.GRPPESTD = 1) AS PRODUCTOS_ACTIVOS,
       (SELECT LISTAGG(p.ID || ' ' || p.CODIGO || ' ' || p.NOMBRE, ' | ') WITHIN GROUP (ORDER BY p.ID)
          FROM PGS.PRDP p JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO
         WHERE g.PLNNCDGO = n.PLNNCDGO AND g.PJRQCDGO = 1236 AND g.GRPPESTD = 1) AS PRODUCTOS
  FROM CNT.PLNN n
 WHERE n.PJRQCDGO = 1236 AND n.PLNNCNTA IN ('2.5.01', '2.3.01.10.03', '2.3.90.90.06', '2.3.90.90.10')
 ORDER BY n.PLNNCNTA;

-- Si la columna de estado del producto existe, conviene mirarla también (el código exige producto ACTIVO):
SELECT p.* FROM PGS.PRDP p WHERE p.ID IN (516, 517);

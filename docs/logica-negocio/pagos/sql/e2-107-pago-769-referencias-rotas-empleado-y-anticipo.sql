-- =====================================================================================
-- e2-107 — Pago 769 (anticipo #181): cuarta ronda. SOLO LECTURA. Equipo omen-saa-2 · 2026-10-15.
--
-- e2-104/105/106 limpios: configuración, secuencias, período, índices únicos, CHECK y triggers sin novedad.
-- Hipótesis que queda y que SOLO afecta a este pago: una referencia ROTA. Al confirmar, Hibernate carga el anticipo
-- con sus relaciones (empleado → centro de costo; pago; descuento) y si alguna columna apunta a una fila que no
-- existe (no hay FK que lo impida) lanza una excepción de sistema que marca la transacción como rollback-only:
-- exactamente el STATUS_MARKED_ROLLBACK que se ve. Se compara contra el 730 (Katherine), que sí se confirmó.
-- Columnas copiadas de Empleado (RHH.MPLD: MPLDCNCS → CNT.CNCS), AnticipoEmpleado (RHH.ANTE: PGTRCDGO, DSRCCDGO)
-- y PagoProgramado (PGS.PGTR: PGTRCNBC).
-- =====================================================================================

-- BLOQUE 1 — Centro de costo de los empleados de los anticipos 181 (769) y 161 (730).
-- ESPERADO: centro_costo nulo, o con existe = 'SI'. Si el del 56 sale con existe = 'NO' → ÉSA ES LA CAUSA.
SELECT 'BLOQUE 1 - centro de costo' AS bloque, a.ANTECDGO, e.MPLDCDGO, e.MPLDAPLL, e.MPLDCNCS AS centro_costo,
       CASE WHEN e.MPLDCNCS IS NULL THEN '(nulo)'
            WHEN EXISTS (SELECT 1 FROM CNT.CNCS c WHERE c.CNCSCDGO = e.MPLDCNCS) THEN 'SI' ELSE 'NO' END AS existe
  FROM RHH.ANTE a
  JOIN RHH.MPLD e ON e.MPLDCDGO = a.MPLDCDGO
 WHERE a.ANTECDGO IN (161, 181);

-- BLOQUE 2 — Referencias del anticipo. ESPERADO: pago_vinculado nulo o igual a su pago (769 / 730) y existente;
-- descuento nulo en el 181. Si apunta a un pago o descuento que no existe → causa.
SELECT 'BLOQUE 2 - referencias del anticipo' AS bloque, a.ANTECDGO, a.PGTRCDGO AS pago_vinculado,
       CASE WHEN a.PGTRCDGO IS NULL THEN '(nulo)'
            WHEN EXISTS (SELECT 1 FROM PGS.PGTR p WHERE p.PGTRCDGO = a.PGTRCDGO) THEN 'SI' ELSE 'NO' END AS pago_existe,
       a.DSRCCDGO AS descuento,
       CASE WHEN a.DSRCCDGO IS NULL THEN '(nulo)'
            WHEN EXISTS (SELECT 1 FROM RHH.DSRC d WHERE d.DSRCCDGO = a.DSRCCDGO) THEN 'SI' ELSE 'NO' END AS descuento_existe
  FROM RHH.ANTE a
 WHERE a.ANTECDGO IN (161, 181);

-- BLOQUE 3 — Cuenta bancaria de los dos pagos. Si el 769 sale de una cuenta distinta del 730, anotar cuál.
SELECT 'BLOQUE 3 - cuenta bancaria' AS bloque, p.PGTRCDGO, p.PGTRCNBC, c.CNBCNMRO, c.PLNNCDGO,
       CASE WHEN EXISTS (SELECT 1 FROM CNT.PLNN n WHERE n.PLNNCDGO = c.PLNNCDGO) THEN 'SI' ELSE 'NO' END AS cuenta_contable_existe
  FROM PGS.PGTR p
  LEFT JOIN TSR.CNBC c ON c.CNBCCDGO = p.PGTRCNBC
 WHERE p.PGTRCDGO IN (730, 769);

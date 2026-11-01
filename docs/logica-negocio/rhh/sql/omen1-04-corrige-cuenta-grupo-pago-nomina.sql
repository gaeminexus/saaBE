-- =====================================================================================
-- omen1-04 — CORRECCIÓN DE DATO (1 fila). omen-saa-1, 2026-11-01.
-- Diagnóstico omen1-03 (corrido en producción el 2026-11-01):
--   grupo PGS.GRPP 38 «PAGO DE NOMINA» → PLNNCDGO 9678 = 2.3.90.15 HONORARIOS POR PAGAR (la marcadora de
--   rhh/sql/15, nunca reemplazada). La plantilla de pago de nómina (alterno 165) línea 50
--   SUELDOS_POR_PAGAR_DEBE → 9689 = 2.5.01 REMUNERACIONES POR PAGAR. Los 20 pagos individualizados del
--   28-10-2026 (17.244,97) se debitaron a honorarios.
-- Este UPDATE deja el grupo en la MISMA cuenta que la plantilla. SE PUEDE CORRER YA, con el EAR de hoy:
-- el código actual toma la cuenta del grupo, así que los pagos de nómina de noviembre ya salen bien.
-- El EAR único trae además N1 (el pago individualizado toma la cuenta de la plantilla, no del grupo).
-- NO corrige los asientos de octubre: eso es una reclasificación de contabilidad (ver
-- rhh/RECLASIFICACION-NOMINA-OCTUBRE-2026.md).
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES
-- 0.1 El grupo 38 apunta a 9678. ESPERADO: 1 fila con PLNNCDGO = 9678.
SELECT g.GRPPCDGO, g.GRPPNMBR, g.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR
  FROM PGS.GRPP g LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO WHERE g.GRPPCDGO = 38;
-- 0.2 Productos que cuelgan del grupo 38. ESPERADO: SOLO el producto NOMINA.
--     (!) Si sale otro producto (p. ej. un pago de honorarios real), PARAR y pasar la salida: ese
--     producto se quedaría también con remuneraciones por pagar.
SELECT p.ID, p.CODIGO, p.NOMBRE FROM PGS.PRDP p WHERE p.GRUPOPRODUCTO = 38;
-- 0.3 ¿Algún otro grupo ya apunta a 2.5.01? (con N1, el producto se resuelve desde la cuenta de la línea
--     50; dos grupos con la misma cuenta lo harían ambiguo). ESPERADO: 0 filas.
SELECT g.GRPPCDGO, g.GRPPNMBR FROM PGS.GRPP g WHERE g.PLNNCDGO = 9689 AND g.GRPPCDGO <> 38;


-- BLOQUE 1 — (ESCRIBE) Solo si sigue en la marcadora.
UPDATE PGS.GRPP SET PLNNCDGO = 9689 WHERE GRPPCDGO = 38 AND PLNNCDGO = 9678;
-- (!) "1 fila actualizada".
COMMIT;


-- BLOQUE 2 — CONTROL DESPUÉS. ESPERADO: 38 PAGO DE NOMINA → 9689 2.5.01 REMUNERACIONES POR PAGAR.
SELECT g.GRPPCDGO, g.GRPPNMBR, g.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR
  FROM PGS.GRPP g LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO WHERE g.GRPPCDGO = 38;


-- REVERSO (comentado):
-- UPDATE PGS.GRPP SET PLNNCDGO = 9678 WHERE GRPPCDGO = 38 AND PLNNCDGO = 9689;
-- COMMIT;

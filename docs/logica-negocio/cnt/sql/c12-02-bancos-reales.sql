-- =====================================================================================
-- c12-02 — VERIFICACION (solo SELECT) de la marca «banco real» (TSR.BNCO.BNCOREAL) de la empresa 1236 para la
--          regla IN-A / EG-A / DI-A. Cuadre Superintendencia, tarea C12 · 2026-10-28 · DISENO §2.1, §7 y §8.
--
-- El lider del proyecto actualizo TSR.BNCO el mismo 28-oct en la base de trabajo. Resultado verificado:
--   BNCOREAL = 1 (12): 393 Pacifico (cnbc 418 ahorros 1048142735 -> 1.1.02.05.65; 419 corriente 7713878 ->
--     1.1.02.05.70), 396 Internacional (417 corriente 520612384 -> 1.1.02.05.80), 398 Austro (415), 400 Amazonas
--     (413 ahorros 4502425973 -> 1.1.02.05.40), 401 Guayaquil (416), 402 Atlantida (412), 403 Comercial de Manabi
--     (414), 404 Alianza del Valle (420), 405 JEP (421), 406 Policia Nacional (423), 407 Oscus (422),
--     408 Mutualista Pichincha (424 y 430). Todas con plan de cuenta 1.1.02.05.xx (14 planes).
--   BNCOREAL = 0 (6): 425 caja chica oficinas (1.1.01.10.01 y la cnbc 431 inactiva de la transitoria), 427 caja
--     chica pendiente legal, 432 cuenta transitoria (2.3.01.15.01), 433 arriendos recibidos por anticipado
--     (2.9.01.10), 435 anticipo a proveedores (1.9.01.10), 438 depositos por confirmar (2.3.90.90.09).
--   Ninguna cuenta bancaria de la 1236 sin plan de cuenta enlazado (21 cuentas, 21 con PLNNCDGO).
-- Con esto: cobro a la transitoria o gasto de caja chica -> DI-A; deposito al banco -> IN-A; pago desde banco -> EG-A.
-- Este script NO cambia nada: sirve para volver a comprobarlo (p. ej. tras recargar el dump) antes de re-procesar.
-- =====================================================================================

-- 1 Bancos de la 1236 con su marca y sus cuentas. ESPERADO: 12 en 1 y 6 en 0, como arriba.
SELECT b.BNCOCDGO, b.BNCONMBR, b.BNCOESTD, b.BNCOREAL,
       c.CNBCCDGO, c.CNBCNMRO, c.CNBCESTD, p.PLNNCNTA, p.PLNNNMBR
  FROM TSR.BNCO b
  LEFT JOIN TSR.CNBC c ON c.BNCOCDGO = b.BNCOCDGO
  LEFT JOIN CNT.PLNN p ON p.PLNNCDGO = c.PLNNCDGO
 WHERE b.PJRQCDGO = 1236
 ORDER BY b.BNCOREAL DESC, b.BNCOCDGO, c.CNBCCDGO;

-- 2 Resumen. ESPERADO: 1 -> 12 bancos (393,396,398,400,401,402,403,404,405,406,407,408); 0 -> 6 (425,427,432,433,435,438).
SELECT b.BNCOREAL, COUNT(*) AS bancos, LISTAGG(b.BNCOCDGO, ',') WITHIN GROUP (ORDER BY b.BNCOCDGO) AS codigos
  FROM TSR.BNCO b WHERE b.PJRQCDGO = 1236 GROUP BY b.BNCOREAL ORDER BY b.BNCOREAL DESC;

-- 3 Cuentas bancarias SIN plan de cuenta (la regla no las ve). ESPERADO: 0 filas.
SELECT c.CNBCCDGO, c.CNBCNMRO, c.BNCOCDGO
  FROM TSR.CNBC c JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO
 WHERE b.PJRQCDGO = 1236 AND c.PLNNCDGO IS NULL;

-- 4 Las tres cuentas de ASOPREP nombradas en C12. ESPERADO: 3 filas, BNCOREAL = 1, con cuenta contable.
SELECT b.BNCOCDGO, b.BNCONMBR, b.BNCOREAL, c.CNBCCDGO, c.CNBCNMRO, p.PLNNCNTA
  FROM TSR.CNBC c JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO LEFT JOIN CNT.PLNN p ON p.PLNNCDGO = c.PLNNCDGO
 WHERE c.CNBCNMRO IN ('1048142735', '520612384', '4502425973')
 ORDER BY b.BNCOCDGO;

-- 5 Planes de cuenta que la regla considera «de banco real» (misma consulta que clasificarTipoAutomatico).
--   ESPERADO: 14 planes, todos 1.1.02.05.xx; ninguno de caja (1.1.01) ni de pasivo (2.x).
SELECT DISTINCT p.PLNNCDGO, p.PLNNCNTA, p.PLNNNMBR
  FROM TSR.CNBC c JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO JOIN CNT.PLNN p ON p.PLNNCDGO = c.PLNNCDGO
 WHERE b.PJRQCDGO = 1236 AND b.BNCOREAL = 1 AND c.PLNNCDGO IS NOT NULL
 ORDER BY p.PLNNCNTA;

-- =====================================================================================
-- c12-02 — Marca «banco real» (TSR.BNCO.BNCOREAL) de la empresa 1236 para la regla IN-A / EG-A / DI-A.
-- Cuadre Superintendencia, tarea C12 · 2026-10-28 · Diseño: DISENO-TIPOS-ASIENTO-AUTOMATICOS.md §2.1 y §7.
--
-- LO QUE SE VIO EN LA BASE DE TRABAJO (dump de produccion del 28-oct, solo SELECT, 2026-10-28):
--   El e2-111 dejo los 18 bancos de la 1236 con BNCOREAL = 1 (default) y nadie corrio su BLOQUE 2 opcional.
--   Las 21 cuentas bancarias (TSR.CNBC) tienen plan de cuenta enlazado (PLNNCDGO no nulo en todas).
--   Bancos y cuentas reales (BNCOREAL = 1 es CORRECTO), por cuenta contable 1.1.02.05.xx:
--     393 BANCO PACIFICO            cnbc 418 ahorros   1048142735   1.1.02.05.65  (847 asientos la tocan)
--                                   cnbc 419 corriente 7713878      1.1.02.05.70
--     396 BANCO INTERNACIONAL       cnbc 417 corriente 520612384    1.1.02.05.80  (1.130 asientos)
--     400 BANCO AMAZONAS            cnbc 413 ahorros   4502425973   1.1.02.05.40
--     398 AUSTRO 415 · 401 GUAYAQUIL 416 · 402 ATLANTIDA 412 · 403 COMERCIAL DE MANABI 414 ·
--     404 ALIANZA DEL VALLE 420 · 405 JEP 421 · 406 POLICIA NACIONAL 423 · 407 OSCUS 422 ·
--     408 MUTUALISTA PICHINCHA 424 (ahorros) y 430 (flexicuenta)   -> 1.1.02.05.49 ... 1.1.02.05.98
--   «Bancos» que NO son bancos (su cuenta contable no es de bancos) y hoy estan en 1, lo que haria que un asiento
--   que los toque salga IN-A / EG-A en vez de DI-A (el diseño dice: caja chica -> DI-A):
--     425 CAJA CHICA OFICINAS           cnbc 428 -> 1.1.01.10.01 CAJA CHICA OFICINAS
--                                       cnbc 431 (estado 3) -> 2.3.01.15.01 CUENTA TRANSITORIA  (!)
--     427 CAJA CHICA PENDIENTE LEGAL    cnbc 429 (estado 3) -> 1.1.01.10.02
--     432 CUENTA TRANSITORIA            cnbc 437 -> 2.3.01.15.01 CUENTA TRANSITORIA (894 asientos)
--     433 ARRIENDO RECIBIDOS POR ANTICIPADO  cnbc 434 -> 2.9.01.10
--     435 ANTICIPO A PROVEEDORES        cnbc 436 -> 1.9.01.10
--     438 DEPOSITOS POR CONFIRMAR       cnbc 439 -> 2.3.90.90.09
--   (!) clasificarTipoAutomatico no filtra por CNBCESTD: mientras el banco 425 sea «real», la cuenta 2.3.01.15.01
--       cuenta como banco real por la cnbc 431 aunque este inactiva. Marcar 425 en 0 lo resuelve.
--
-- PROPUESTA (la decide el lider del proyecto / el responsable de ASOPREP; este script NO SE HA EJECUTADO):
--   BNCOREAL = 0 en 425, 427, 432, 433, 435, 438. Los 12 bancos y cooperativas quedan en 1.
--   Efecto en la regla: un cobro que entra a la transitoria (2.3.01.15.01) y despues se deposita en el banco sale
--   DI-A al registrarse y IN-A al depositarse; la reposicion de caja chica desde el banco sale EG-A; el gasto de
--   caja chica sale DI-A.
--
-- SQL PURO, SIN COMMIT. Idempotente (el UPDATE solo toca filas en 1).
-- =====================================================================================

-- BLOQUE 1 — CONTROL ANTES: todos los bancos de la 1236 con sus cuentas y cuenta contable.
-- ESPERADO hoy: 18 bancos, todos BNCOREAL = 1, 21 cuentas, ninguna sin PLNNCDGO.
SELECT b.BNCOCDGO, b.BNCONMBR, b.BNCOESTD, b.BNCOREAL,
       c.CNBCCDGO, c.CNBCNMRO, c.CNBCESTD, p.PLNNCNTA, p.PLNNNMBR
  FROM TSR.BNCO b
  LEFT JOIN TSR.CNBC c ON c.BNCOCDGO = b.BNCOCDGO
  LEFT JOIN CNT.PLNN p ON p.PLNNCDGO = c.PLNNCDGO
 WHERE b.PJRQCDGO = 1236
 ORDER BY b.BNCOCDGO, c.CNBCCDGO;

-- 1.2 Cuentas bancarias SIN plan de cuenta (la regla no las ve). ESPERADO: 0 filas.
SELECT c.CNBCCDGO, c.CNBCNMRO, c.BNCOCDGO
  FROM TSR.CNBC c JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO
 WHERE b.PJRQCDGO = 1236 AND c.PLNNCDGO IS NULL;

-- 1.3 Las tres cuentas de ASOPREP nombradas en C12. ESPERADO: 3 filas, BNCOREAL = 1, con cuenta contable.
SELECT b.BNCOCDGO, b.BNCONMBR, b.BNCOREAL, c.CNBCCDGO, c.CNBCNMRO, p.PLNNCNTA
  FROM TSR.CNBC c JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO LEFT JOIN CNT.PLNN p ON p.PLNNCDGO = c.PLNNCDGO
 WHERE c.CNBCNMRO IN ('1048142735', '520612384', '4502425973')
 ORDER BY b.BNCOCDGO;

-- 1.4 Cuantos asientos de la 1236 tocan hoy la cuenta contable de cada «banco» (dimensiona el efecto).
SELECT b.BNCOCDGO, b.BNCONMBR, b.BNCOREAL, p.PLNNCNTA, COUNT(DISTINCT d.ASNTCDGO) AS asientos
  FROM CNT.DTAS d
  JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
  JOIN TSR.CNBC c ON c.PLNNCDGO = d.PLNNCDGO
  JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO
  JOIN CNT.PLNN p ON p.PLNNCDGO = c.PLNNCDGO
 WHERE a.PJRQCDGO = 1236
 GROUP BY b.BNCOCDGO, b.BNCONMBR, b.BNCOREAL, p.PLNNCNTA
 ORDER BY b.BNCOCDGO, p.PLNNCNTA;

-- BLOQUE 2 — CAMBIO PROPUESTO: los que no son bancos dejan de ser «reales». ESPERADO: 6 filas actualizadas.
UPDATE TSR.BNCO
   SET BNCOREAL = 0
 WHERE PJRQCDGO = 1236
   AND BNCOCDGO IN (425, 427, 432, 433, 435, 438)
   AND BNCOREAL = 1;

-- (sin COMMIT: lo da quien lo corre despues de revisar el BLOQUE 3)

-- BLOQUE 3 — CONTROL DESPUES
-- 3.1 ESPERADO: 12 bancos en 1 (393, 396, 398, 400..408) y 6 en 0 (425, 427, 432, 433, 435, 438).
SELECT b.BNCOREAL, COUNT(*) AS bancos, LISTAGG(b.BNCOCDGO, ',') WITHIN GROUP (ORDER BY b.BNCOCDGO) AS codigos
  FROM TSR.BNCO b WHERE b.PJRQCDGO = 1236 GROUP BY b.BNCOREAL ORDER BY b.BNCOREAL DESC;

-- 3.2 Planes de cuenta que la regla considerara «de banco real» (misma consulta que clasificarTipoAutomatico).
--     ESPERADO: solo cuentas 1.1.02.05.xx (14 planes).
SELECT DISTINCT p.PLNNCDGO, p.PLNNCNTA, p.PLNNNMBR
  FROM TSR.CNBC c JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO JOIN CNT.PLNN p ON p.PLNNCDGO = c.PLNNCDGO
 WHERE b.PJRQCDGO = 1236 AND b.BNCOREAL = 1 AND c.PLNNCDGO IS NOT NULL
 ORDER BY p.PLNNCNTA;

-- REVERSO (sin COMMIT)
-- UPDATE TSR.BNCO SET BNCOREAL = 1 WHERE PJRQCDGO = 1236 AND BNCOCDGO IN (425, 427, 432, 433, 435, 438);

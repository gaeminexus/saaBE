-- =====================================================================================
-- e2-98 — Mover la garantía y el anticipo de arriendo al cliente correcto. CORRECCIÓN.
-- Equipo omen-saa-2 · 2026-10-12. Sigue al e2-96 (corrido por el usuario el 2026-10-12).
--
-- Lo que dio el e2-96:
--   - Titular EQUIVOCADO 145 (RUC 1792458560001, «MAC - ACCESS»); CORRECTO 204 (RUC 1793221709001,
--     «MACACCESS EC S.A.», cliente). Sin duplicados por espacios.
--   - Los dos documentos viven en CBR.ANTC (no en TSR.INGR), ambos CONFIRMADOS (estado 2), sin cruzar:
--       ANTC 7  · 26-08 · garantía piso 9  · 1.305,00 · asiento T-I-2026-08-0007 (8330)
--       ANTC 11 · 08-09 · arriendo octubre · 1.370,25 · asiento T-I-2026-09-0008 (9674)
--   - Factura 001-001-000000834 (FCTR 284, 06-10, 1.500,75) del 204, con retención de 130,50 aplicada
--     (APLC 57): le quedan 1.370,25, que es EXACTAMENTE el anticipo 11.
--   - Los asientos usan cuentas GENÉRICAS (banco 1.1.02.05.65 y 2.9.01.10 Arriendos recibidos por
--     anticipado), no una cuenta propia del cliente → la contabilidad NO cambia. Solo el auxiliar.
--   - El saldo global de anticipos vive en TSR.PRCC 331 (rol 93 del 145, tipo 2 = anticipos,
--     PRCCSLIN 2.675,25 = 1.305 + 1.370,25). El 204 NO tiene cuenta de anticipos (solo la 384, tipo 1).
--
-- Por qué hay que mover también la PRCC 331 (verificado en el código el 2026-10-12):
--   - AplicacionPagoCxcServiceImpl:388 exige que el anticipo sea del mismo titular que la factura.
--   - AplicacionPagoCxcServiceImpl:476-500 y :1069 leen el saldo de la cuenta tipo 2 del titular de la
--     FACTURA y cortan si no alcanza. Si se mueve solo el ANTC, el cruce del 204 falla por falta de
--     cuenta de anticipos y el 145 se queda con un saldo de 2.675,25 que ya no es suyo.
--   - Por eso se re-cuelga la fila 331 (con su cuenta 2.9.01.10 y su saldo) del rol 157 del 204, en vez
--     de insertar una nueva: no hay INSERT, no hace falta sincronizar ninguna secuencia.
--
-- Qué NO se toca:
--   - Los asientos 8330 y 9674 ni sus líneas: son hechos contables de agosto y septiembre, con cuentas
--     genéricas. El texto «MAC - ACCESS» de la glosa queda como está.
--   - La factura 284 ni la aplicación 57. El cruce del anticipo 11 contra la factura lo hace Ana desde la
--     pantalla, DESPUÉS de este script.
--
-- PASO 3 — el usuario elige UNA de dos opciones (por defecto A, que va activa):
--   A) Dejar el titular 145 y su rol 93 INACTIVOS (TTLRESTD / PRRLESTD = 0). Recomendado.
--   B) Borrarlo «sin dejar rastro», como pidió Ana. Solo si el BLOQUE C3 da 0 en TODAS las tablas.
--      Va comentado; para usarlo se comenta el A y se descomenta el B.
--
-- Orden: correr el CONTROL ANTES y revisar contra lo ESPERADO → PASOS → CONTROL DESPUÉS → COMMIT a mano.
-- Si cualquier UPDATE afecta un número de filas distinto al indicado: ROLLBACK y avisar.
-- =====================================================================================

-- ─── CONTROL ANTES ───────────────────────────────────────────────────────────────────

-- C1 — Los anticipos. ESPERADO: 2 filas, TITULAR 145, ESTADO 2, ANTCSALD = VALOR, ANTCAPLC 0.
SELECT 'C1 - anticipos' AS control, x.ID, x.TITULAR, x.ESTADO, x.VALOR, x.ANTCSALD, x.ANTCAPLC
  FROM CBR.ANTC x WHERE x.ID IN (7, 11) ORDER BY x.ID;

-- C2 — Roles y cuentas de los dos titulares. ESPERADO:
--   · PRRL 93 → titular 145 y PRRL 157 → titular 204, con el MISMO PRRLRZZA (rol cliente).
--   · PRCC 331 en el rol 93, PRCCTPOO 2, PRCCSLIN 2675.25.
--   · El rol 157 SIN ninguna fila con PRCCTPOO 2. Si ya tiene una, DETENTE y avisa: hay que sumar saldos
--     en vez de mover la fila, y ese es otro script.
SELECT 'C2 - roles y cuentas' AS control, r.PRRLCDGO, r.PRSNCDGO, r.PRRLRZZA, r.PRRLESTD,
       c.PRCCCDGO, c.PRCCTPOO, c.PJRQCDGO, c.PLNNCDGO, c.PRCCSLIN
  FROM TSR.PRRL r
  LEFT JOIN TSR.PRCC c ON c.PRRLCDGO = r.PRRLCDGO
 WHERE r.PRRLCDGO IN (93, 157)
 ORDER BY r.PRRLCDGO, c.PRCCTPOO;

-- C3 — Todo lo que apunta al titular 145, tabla por tabla (las 23 FKs del bloque 8 del e2-96).
-- ESPERADO hoy: ANTC 2 y PRRL 1; el resto 0. Después del PASO 1, ANTC pasa a 0.
-- Si alguna OTRA tabla da > 0, la opción B (borrar) queda descartada.
SELECT 'C3 - referencias al 145' AS control, tabla, filas FROM (
  SELECT 'CBR.ANTC' tabla, COUNT(*) filas FROM CBR.ANTC WHERE TITULAR = 145 UNION ALL
  SELECT 'CBR.FCTR', COUNT(*) FROM CBR.FCTR WHERE COMPRADOR = 145 UNION ALL
  SELECT 'CBR.LQCS', COUNT(*) FROM CBR.LQCS WHERE TITULAR = 145 UNION ALL
  SELECT 'CBR.NTCR', COUNT(*) FROM CBR.NTCR WHERE TITULAR = 145 UNION ALL
  SELECT 'CBR.NTDB', COUNT(*) FROM CBR.NTDB WHERE TITULAR = 145 UNION ALL
  SELECT 'CBR.RTNC', COUNT(*) FROM CBR.RTNC WHERE PROVEEDOR = 145 UNION ALL
  SELECT 'CBR.RTV2', COUNT(*) FROM CBR.RTV2 WHERE PROVEEDOR = 145 UNION ALL
  SELECT 'PGS.ANTP', COUNT(*) FROM PGS.ANTP WHERE ANTPTTLR = 145 UNION ALL
  SELECT 'PGS.FCTC', COUNT(*) FROM PGS.FCTC WHERE TITULAR = 145 UNION ALL
  SELECT 'PGS.LQCC', COUNT(*) FROM PGS.LQCC WHERE TITULAR = 145 UNION ALL
  SELECT 'PGS.NGCP', COUNT(*) FROM PGS.NGCP WHERE TITULAR = 145 UNION ALL
  SELECT 'PGS.NTCC', COUNT(*) FROM PGS.NTCC WHERE TITULAR = 145 UNION ALL
  SELECT 'PGS.NTDC', COUNT(*) FROM PGS.NTDC WHERE TITULAR = 145 UNION ALL
  SELECT 'PGS.PGTR', COUNT(*) FROM PGS.PGTR WHERE PGTRTTLR = 145 UNION ALL
  SELECT 'PGS.RCV2', COUNT(*) FROM PGS.RCV2 WHERE PROVEEDOR = 145 UNION ALL
  SELECT 'PGS.RTCM', COUNT(*) FROM PGS.RTCM WHERE PROVEEDOR = 145 UNION ALL
  SELECT 'TSR.CTBN', COUNT(*) FROM TSR.CTBN WHERE TTLRCDGO = 145 UNION ALL
  SELECT 'TSR.DTCH', COUNT(*) FROM TSR.DTCH WHERE PRSNCDGO = 145 OR DTCHIDBN = 145 UNION ALL
  SELECT 'TSR.EGRS', COUNT(*) FROM TSR.EGRS WHERE EGRSTTLR = 145 UNION ALL
  SELECT 'TSR.INGR', COUNT(*) FROM TSR.INGR WHERE INGRTTLR = 145 UNION ALL
  SELECT 'TSR.MVCH', COUNT(*) FROM TSR.MVCH WHERE TTLRCDGO = 145 UNION ALL
  SELECT 'TSR.PRRL', COUNT(*) FROM TSR.PRRL WHERE PRSNCDGO = 145
) ORDER BY tabla;

-- ─── PASO 1 — Los dos anticipos pasan al 204. ESPERADO: 2 filas actualizadas. ──────────
UPDATE CBR.ANTC
   SET TITULAR = 204,
       OBSERVACION = SUBSTR(OBSERVACION || ' | Titular corregido del RUC 1792458560001 al 1793221709001'
                            || ' (registrado al cliente equivocado; e2-98, 2026-10-12)', 1, 2000)
 WHERE ID IN (7, 11) AND TITULAR = 145 AND ESTADO = 2;

-- ─── PASO 2 — La cuenta de anticipos (con su saldo de 2.675,25) pasa al rol 157 del 204. ──
-- ESPERADO: 1 fila. El NOT EXISTS impide dejar al 204 con dos cuentas de anticipos.
UPDATE TSR.PRCC c
   SET c.PRRLCDGO = 157
 WHERE c.PRCCCDGO = 331 AND c.PRRLCDGO = 93 AND c.PRCCTPOO = 2
   AND NOT EXISTS (SELECT 1 FROM TSR.PRCC o WHERE o.PRRLCDGO = 157 AND o.PRCCTPOO = 2);

-- ─── PASO 3 — OPCIÓN A (por defecto): titular 145 y rol 93 inactivos. ESPERADO: 1 y 1. ──
UPDATE TSR.TTLR SET TTLRESTD = 0 WHERE TTLRCDGO = 145 AND TRIM(TTLRIDNT) = '1792458560001';
UPDATE TSR.PRRL SET PRRLESTD = 0 WHERE PRRLCDGO = 93 AND PRSNCDGO = 145;

-- ─── PASO 3 — OPCIÓN B (solo si el usuario lo decide y C3, ya sin ANTC, da 0 fuera de PRRL). ─
-- Orden por FK: cuentas del rol → rol → titular. ESPERADO: 1 (PRCC 330), 1, 1.
-- DELETE FROM TSR.PRCC WHERE PRRLCDGO = 93;
-- DELETE FROM TSR.PRRL WHERE PRRLCDGO = 93 AND PRSNCDGO = 145;
-- DELETE FROM TSR.TTLR WHERE TTLRCDGO = 145 AND TRIM(TTLRIDNT) = '1792458560001';

-- ─── CONTROL DESPUÉS ─────────────────────────────────────────────────────────────────
-- D1 — ESPERADO: ANTC 7 y 11 con TITULAR 204, mismo ESTADO, VALOR y ANTCSALD que en C1.
SELECT 'D1 - anticipos' AS control, x.ID, x.TITULAR, x.ESTADO, x.VALOR, x.ANTCSALD, x.OBSERVACION
  FROM CBR.ANTC x WHERE x.ID IN (7, 11) ORDER BY x.ID;

-- D2 — ESPERADO: el rol 157 con la 384 (tipo 1) y la 331 (tipo 2, 2675.25); el rol 93 solo con la 330
-- (o sin filas, en la opción B).
SELECT 'D2 - roles y cuentas' AS control, r.PRRLCDGO, r.PRSNCDGO, r.PRRLESTD, c.PRCCCDGO, c.PRCCTPOO, c.PRCCSLIN
  FROM TSR.PRRL r
  LEFT JOIN TSR.PRCC c ON c.PRRLCDGO = r.PRRLCDGO
 WHERE r.PRRLCDGO IN (93, 157)
 ORDER BY r.PRRLCDGO, c.PRCCTPOO;

-- D3 — ESPERADO: el saldo de anticipos del 204 (2.675,25) cuadra con la suma de ANTCSALD de sus anticipos
-- confirmados. Si no cuadra, NO COMMIT.
SELECT 'D3 - cuadre 204' AS control,
       (SELECT SUM(x.ANTCSALD) FROM CBR.ANTC x WHERE x.TITULAR = 204 AND x.ESTADO = 2) AS suma_anticipos,
       (SELECT SUM(c.PRCCSLIN) FROM TSR.PRCC c JOIN TSR.PRRL r ON r.PRRLCDGO = c.PRRLCDGO
         WHERE r.PRSNCDGO = 204 AND c.PRCCTPOO = 2) AS saldo_cuenta_anticipos
  FROM DUAL;

-- D4 — ESPERADO: 145 con TTLRESTD 0 (opción A) o sin fila (opción B).
SELECT 'D4 - titular 145' AS control, t.TTLRCDGO, t.TTLRIDNT, t.TTLRESTD FROM TSR.TTLR t WHERE t.TTLRCDGO = 145;

-- COMMIT;   -- a mano, solo si D1 a D4 dan lo esperado.

-- ─── REVERSO (opción A; la B no tiene reverso desde este script) ─────────────────────
-- UPDATE TSR.PRRL SET PRRLESTD = 1 WHERE PRRLCDGO = 93 AND PRSNCDGO = 145;
-- UPDATE TSR.TTLR SET TTLRESTD = 1 WHERE TTLRCDGO = 145;
-- UPDATE TSR.PRCC SET PRRLCDGO = 93 WHERE PRCCCDGO = 331 AND PRRLCDGO = 157;
-- UPDATE CBR.ANTC
--    SET TITULAR = 145,
--        OBSERVACION = REPLACE(OBSERVACION, ' | Titular corregido del RUC 1792458560001 al 1793221709001'
--                              || ' (registrado al cliente equivocado; e2-98, 2026-10-12)', '')
--  WHERE ID IN (7, 11) AND TITULAR = 204;
-- (El reverso solo vale si Ana todavía no cruzó el anticipo 11 contra la factura 834.)

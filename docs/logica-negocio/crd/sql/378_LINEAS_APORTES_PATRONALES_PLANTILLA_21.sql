-- =====================================================================================
-- 378 — Cuentas de los aportes 13 (JUBILACION PATRONAL) y 14 (CESANTIA PATRONAL) en la
--       plantilla 21 (aux1 = 58 y 59), para el CRUCE de aportes contra préstamos (C29)
-- 2026-11-11 · expediente cuadreSuper, decisión D-069
--
-- Por que: el cruce con aportes (acuerdos de condonación, precancelación y abono con aportes)
--   resuelve la cuenta de cada tipo consumido con ContabilizacionIndividualCreditoServiceImpl
--   .aux1ParaTipoAporte. Los tipos 13 y 14 no tenian linea y el motor rechazaba los cruces de
--   cesantías que traen aportes patronales ("El tipo de aporte 13/14 no tiene cuenta contable
--   parametrizada"). Cuentas = las de CRD.CTAP de cada tipo (y las del cruce manual
--   CRU-2025-10-001 del re-proceso):
--     13 JUBILACION PATRONAL -> 2.1.02.10.01 APORTES PATRONALES JUBILACION  (aux1 58)
--     14 CESANTIA PATRONAL   -> 2.1.01.10.01 APORTES PATRONALES CESANTIA    (aux1 59)
--   Igual que sus hermanas 50-57: DTPLMVMN = 2 (HABER) y DTPLAXL2 = 0; en el cruce el BE la
--   fuerza al DEBE. El registro/cobro de aportes 13/14 sigue rechazado (no cambia).
--
-- ORDEN: correr ESTE script ANTES de desplegar el WAR que trae el mapeo de 13/14.
-- Empresa 1236. Correr bloque por bloque y mirar cada salida antes del siguiente.
-- =====================================================================================


-- BLOQUE 1 — Las dos cuentas existen y son de movimiento. Tienen que salir DOS filas (nivel 5, tipo 2).
SELECT n.PLNNCDGO, n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS NOMBRE, n.PLNNNVLL AS NIVEL,
       n.PLNNTPOO AS TIPO, n.PLNNESTD AS ESTADO
  FROM CNT.PLNN n
 WHERE n.PJRQCDGO = 1236
   AND n.PLNNCNTA IN ('2.1.02.10.01', '2.1.01.10.01')
 ORDER BY n.PLNNCNTA;


-- BLOQUE 2 — Lineas actuales de la plantilla 21.
--   (!) Si alguna fila ya apunta a 2.1.02.10.01 o 2.1.01.10.01, o si AUX1 58/59 ya existe:
--       NO correr el bloque 3 (el BE usaria otro numero).
SELECT p.PLNSCDGO, p.PLNSCDAL AS ALTERNO, d.DTPLCDGO, d.DTPLAXL1 AS AUX1, d.DTPLAXL2 AS AUX2,
       d.DTPLMVMN AS MOVIMIENTO, n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS CUENTA_NOMBRE, d.DTPLESTD AS ESTADO_LINEA
  FROM CNT.PLNS p
  JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
  LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PLNSCDAL = 21
   AND p.PJRQCDGO = 1236
 ORDER BY d.DTPLAXL1;


-- BLOQUE 2b — La secuencia va por delante del MAX (si no, el bloque 3 da ORA-00001).
SELECT (SELECT MAX(DTPLCDGO) FROM CNT.DTPL) AS MAX_DTPLCDGO, s.LAST_NUMBER AS SECUENCIA
  FROM ALL_SEQUENCES s
 WHERE s.SEQUENCE_OWNER = 'CNT' AND s.SEQUENCE_NAME = 'SQ_DTPLCDGO';
-- (!) Si SECUENCIA <= MAX_DTPLCDGO: adelantar la secuencia antes del bloque 3.


-- BLOQUE 3 — (ESCRIBE) Crea las dos lineas. El NOT EXISTS impide duplicarlas.
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLAXL1, DTPLAXL2, DTPLMVMN, DTPLDSCR, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, n.PLNNCDGO, 58, 0, 2, 'Aportes JUBILACION PATRONAL (tipo 13)', 1
  FROM CNT.PLNS p
  JOIN CNT.PLNN n ON n.PJRQCDGO = p.PJRQCDGO AND n.PLNNCNTA = '2.1.02.10.01'
 WHERE p.PLNSCDAL = 21
   AND p.PJRQCDGO = 1236
   AND NOT EXISTS (SELECT 1 FROM CNT.DTPL d WHERE d.PLNSCDGO = p.PLNSCDGO AND d.DTPLAXL1 = 58);
-- (!) "1 fila insertada".
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLAXL1, DTPLAXL2, DTPLMVMN, DTPLDSCR, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, n.PLNNCDGO, 59, 0, 2, 'Aportes CESANTIA PATRONAL (tipo 14)', 1
  FROM CNT.PLNS p
  JOIN CNT.PLNN n ON n.PJRQCDGO = p.PJRQCDGO AND n.PLNNCNTA = '2.1.01.10.01'
 WHERE p.PLNSCDAL = 21
   AND p.PJRQCDGO = 1236
   AND NOT EXISTS (SELECT 1 FROM CNT.DTPL d WHERE d.PLNSCDGO = p.PLNSCDGO AND d.DTPLAXL1 = 59);
-- (!) "1 fila insertada". Si alguno dice 0 o mas de 1: ROLLBACK.
COMMIT;


-- BLOQUE 4 — Verificacion. Tienen que salir dos filas:
--   21 | 58 | 2 | 2.1.02.10.01 | APORTES PATRONALES JUBILACION
--   21 | 59 | 2 | 2.1.01.10.01 | APORTES PATRONALES CESANTIA
SELECT p.PLNSCDAL AS ALTERNO, d.DTPLAXL1 AS AUX1, d.DTPLMVMN AS MOVIMIENTO,
       n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS CUENTA_NOMBRE, d.DTPLDSCR AS DESCRIPCION
  FROM CNT.PLNS p
  JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
  JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PLNSCDAL = 21
   AND p.PJRQCDGO = 1236
   AND d.DTPLAXL1 IN (58, 59)
 ORDER BY d.DTPLAXL1;


-- BLOQUE 5 — CTAP y plantilla 21 coinciden para los tipos del cruce (9, 11, 13, 14, 21, 22).
--   COINCIDE = 'S' en las seis filas.
SELECT c.TPAPCDGO, n.PLNNCNTA AS CUENTA_CTAP,
       (SELECT n2.PLNNCNTA FROM CNT.PLNS p JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
          JOIN CNT.PLNN n2 ON n2.PLNNCDGO = d.PLNNCDGO
         WHERE p.PLNSCDAL = 21 AND p.PJRQCDGO = 1236
           AND d.DTPLAXL1 = DECODE(c.TPAPCDGO, 11, 50, 9, 51, 22, 54, 21, 55, 13, 58, 14, 59)) AS CUENTA_PLANTILLA_21,
       CASE WHEN n.PLNNCNTA = (SELECT n2.PLNNCNTA FROM CNT.PLNS p JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
          JOIN CNT.PLNN n2 ON n2.PLNNCDGO = d.PLNNCDGO
         WHERE p.PLNSCDAL = 21 AND p.PJRQCDGO = 1236
           AND d.DTPLAXL1 = DECODE(c.TPAPCDGO, 11, 50, 9, 51, 22, 54, 21, 55, 13, 58, 14, 59)) THEN 'S' ELSE 'N' END AS COINCIDE
  FROM CRD.CTAP c
  JOIN CNT.PLNN n ON n.PLNNCDGO = c.CTAPPLNP
 WHERE c.PJRQCDGO = 1236
   AND c.TPAPCDGO IN (9, 11, 13, 14, 21, 22)
 ORDER BY c.TPAPCDGO;

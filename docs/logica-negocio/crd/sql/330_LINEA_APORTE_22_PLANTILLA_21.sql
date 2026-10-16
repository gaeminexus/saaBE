-- =====================================================================================
-- 330 — Cuenta del aporte 22 (JUBILACION RETIRO VOLUNTARIO) en la plantilla 21 (aux1 = 54)
-- Equipo omen-saa-1 · 2026-10-16
--
-- Sintoma: el cruce de una cuota con el saldo de aportes falla con
--   "El tipo de aporte 22 no tiene cuenta contable parametrizada (hoy solo 9 jubilacion,
--    11 cesantia, 2 adicional, 23 pension complementaria)".
-- Decision del usuario (2026-10-16): la cuenta del aporte 22 es 2.1.02.25.05
--   CTA INDIVIDUAL PASIVOS JUBILACION (cierra el "?" de MAPEO-CUENTAS-TIPO-APORTE.md §3).
--
-- Se agrega la linea en la plantilla 21 con aux1 = 54 (proximo libre despues del 53 de la
-- pension complementaria, ver sql/199). El BE mapea el tipo 22 a ese aux1 en
-- ContabilizacionIndividualCreditoServiceImpl.aux1ParaTipoAporte. Igual que sus hermanas
-- 50-53: DTPLMVMN = 2 (HABER) y DTPLAXL2 = 0; en el cruce el BE la fuerza al DEBE.
--
-- ORDEN: correr ESTE script ANTES de desplegar el WAR que trae el mapeo del 22.
-- Empresa 1236. Correr bloque por bloque y mirar cada salida antes del siguiente.
-- =====================================================================================


-- BLOQUE 1 — La cuenta existe y es de movimiento. Tiene que salir UNA fila.
SELECT n.PLNNCDGO, n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS NOMBRE, n.PLNNNVLL AS NIVEL,
       n.PLNNTPOO AS TIPO, n.PLNNESTD AS ESTADO
  FROM CNT.PLNN n
 WHERE n.PJRQCDGO = 1236
   AND n.PLNNCNTA = '2.1.02.25.05';
-- (!) Cero filas o mas de una: PARAR y avisar al arbitro.


-- BLOQUE 2 — Lineas actuales de la plantilla 21.
--   (!) Si alguna fila ya apunta a 2.1.02.25.05: NO correr el bloque 3, avisar al arbitro
--       con su AUX1 (el BE usara ese numero en vez de 54).
--   (!) Si AUX1 = 54 ya existe: NO correr el bloque 3, avisar al arbitro.
SELECT p.PLNSCDGO, p.PLNSCDAL AS ALTERNO, p.PJRQCDGO AS EMPRESA,
       d.DTPLCDGO, d.DTPLAXL1 AS AUX1, d.DTPLAXL2 AS AUX2, d.DTPLMVMN AS MOVIMIENTO,
       n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS CUENTA_NOMBRE, d.DTPLDSCR AS DESCRIPCION,
       d.DTPLESTD AS ESTADO_LINEA
  FROM CNT.PLNS p
  JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
  LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PLNSCDAL = 21
   AND p.PJRQCDGO = 1236
 ORDER BY d.DTPLAXL1;


-- BLOQUE 2b — La secuencia va por delante del MAX (si no, el bloque 3 da ORA-00001).
SELECT (SELECT MAX(DTPLCDGO) FROM CNT.DTPL) AS MAX_DTPLCDGO,
       s.LAST_NUMBER AS SECUENCIA
  FROM ALL_SEQUENCES s
 WHERE s.SEQUENCE_OWNER = 'CNT' AND s.SEQUENCE_NAME = 'SQ_DTPLCDGO';
-- (!) Si SECUENCIA <= MAX_DTPLCDGO: avisar al arbitro antes del bloque 3 (hay que adelantarla).


-- BLOQUE 3 — (ESCRIBE) Crea la linea. PK por secuencia explicita (leccion del sql/199).
--   El NOT EXISTS impide duplicarla si se corre dos veces.
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLAXL1, DTPLAXL2, DTPLMVMN, DTPLDSCR, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL,
       p.PLNSCDGO,
       n.PLNNCDGO,
       54,
       0,
       2,
       'Aportes JUBILACION RETIRO VOLUNTARIO (tipo 22)',
       1
  FROM CNT.PLNS p
  JOIN CNT.PLNN n ON n.PJRQCDGO = p.PJRQCDGO AND n.PLNNCNTA = '2.1.02.25.05'
 WHERE p.PLNSCDAL = 21
   AND p.PJRQCDGO = 1236
   AND NOT EXISTS (SELECT 1 FROM CNT.DTPL d WHERE d.PLNSCDGO = p.PLNSCDGO AND d.DTPLAXL1 = 54);
-- (!) Tiene que decir "1 fila insertada". Si dice 0 o mas de 1: ROLLBACK y avisar.
COMMIT;


-- BLOQUE 4 — Verificacion. Tiene que salir: 21 | 54 | 2 | 2.1.02.25.05 | CTA INDIVIDUAL PASIVOS JUBILACION
SELECT p.PLNSCDAL AS ALTERNO, d.DTPLAXL1 AS AUX1, d.DTPLMVMN AS MOVIMIENTO,
       n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS CUENTA_NOMBRE, d.DTPLDSCR AS DESCRIPCION
  FROM CNT.PLNS p
  JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
  LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PLNSCDAL = 21
   AND p.PJRQCDGO = 1236
   AND d.DTPLAXL1 = 54;


-- REVERSO (solo si hay que deshacer; antes, retirar del WAR el mapeo del tipo 22):
-- DELETE FROM CNT.DTPL d
--  WHERE d.DTPLAXL1 = 54
--    AND d.PLNSCDGO = (SELECT p.PLNSCDGO FROM CNT.PLNS p WHERE p.PLNSCDAL = 21 AND p.PJRQCDGO = 1236);
-- COMMIT;

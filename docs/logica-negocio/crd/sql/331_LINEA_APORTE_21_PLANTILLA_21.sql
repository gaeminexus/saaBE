-- =====================================================================================
-- 331 — Cuenta del aporte 21 (CESANTIA RETIRO VOLUNTARIO) en la plantilla 21 (aux1 = 55)
-- Equipo omen-saa-1 · 2026-10-19
--
-- ✅ EJECUTADO EN PRODUCCION el 2026-10-19 (salida verificada por omen-saa-1-arb). NO volver a correr
--    el bloque 3. Bloque 5 del 331: CTAP y plantilla 21 coinciden para 9, 11, 21, 22 y 23.
--
-- Por que: el usuario (2026-10-19) senalo la cuenta del aporte 21 junto con la del 22
--   (sql/330), para operar las cuentas de RETIRO VOLUNTARIO desde la pantalla de cobros:
--   21 CESANTIA RETIRO VOLUNTARIO -> 2.1.01.20.05 CTA INDIVIDUAL PASIVOS CESANTIA
--   (coincide con la propuesta de MAPEO-CUENTAS-TIPO-APORTE.md §3).
--
-- Se agrega la linea en la plantilla 21 con aux1 = 55 (proximo libre despues del 54 del
-- aporte 22, sql/330). El BE mapea el tipo 21 a ese aux1 en
-- ContabilizacionIndividualCreditoServiceImpl.aux1ParaTipoAporte. Igual que sus hermanas
-- 50-54: DTPLMVMN = 2 (HABER) y DTPLAXL2 = 0; en el cruce el BE la fuerza al DEBE.
--
-- ORDEN: correr ESTE script ANTES de desplegar el WAR que trae el mapeo del 21.
-- Empresa 1236. Correr bloque por bloque y mirar cada salida antes del siguiente.
-- =====================================================================================


-- BLOQUE 1 — La cuenta existe y es de movimiento. Tiene que salir UNA fila.
SELECT n.PLNNCDGO, n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS NOMBRE, n.PLNNNVLL AS NIVEL,
       n.PLNNTPOO AS TIPO, n.PLNNESTD AS ESTADO
  FROM CNT.PLNN n
 WHERE n.PJRQCDGO = 1236
   AND n.PLNNCNTA = '2.1.01.20.05';
-- (!) Cero filas o mas de una: PARAR y avisar al arbitro.


-- BLOQUE 2 — Lineas actuales de la plantilla 21.
--   (!) Si alguna fila ya apunta a 2.1.01.20.05: NO correr el bloque 3, avisar al arbitro
--       con su AUX1 (el BE usara ese numero en vez de 55).
--   (!) Si AUX1 = 55 ya existe: NO correr el bloque 3, avisar al arbitro.
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
       55,
       0,
       2,
       'Aportes CESANTIA RETIRO VOLUNTARIO (tipo 21)',
       1
  FROM CNT.PLNS p
  JOIN CNT.PLNN n ON n.PJRQCDGO = p.PJRQCDGO AND n.PLNNCNTA = '2.1.01.20.05'
 WHERE p.PLNSCDAL = 21
   AND p.PJRQCDGO = 1236
   AND NOT EXISTS (SELECT 1 FROM CNT.DTPL d WHERE d.PLNSCDGO = p.PLNSCDGO AND d.DTPLAXL1 = 55);
-- (!) Tiene que decir "1 fila insertada". Si dice 0 o mas de 1: ROLLBACK y avisar.
COMMIT;


-- BLOQUE 4 — Verificacion. Tiene que salir: 21 | 55 | 2 | 2.1.01.20.05 | CTA INDIVIDUAL PASIVOS CESANTIA
SELECT p.PLNSCDAL AS ALTERNO, d.DTPLAXL1 AS AUX1, d.DTPLMVMN AS MOVIMIENTO,
       n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS CUENTA_NOMBRE, d.DTPLDSCR AS DESCRIPCION
  FROM CNT.PLNS p
  JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
  LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PLNSCDAL = 21
   AND p.PJRQCDGO = 1236
   AND d.DTPLAXL1 = 55;


-- REVERSO (solo si hay que deshacer; antes, retirar del WAR el mapeo del tipo 21):
-- DELETE FROM CNT.DTPL d
--  WHERE d.DTPLAXL1 = 55
--    AND d.PLNSCDGO = (SELECT p.PLNSCDGO FROM CNT.PLNS p WHERE p.PLNSCDAL = 21 AND p.PJRQCDGO = 1236);
-- COMMIT;


-- BLOQUE 5 — (solo lectura, 2026-10-19) Coherencia con CRD.CTAP (devolucion de aportes).
--   La devolucion debita la cuenta de PASIVO de CRD.CTAP; el cobro y el cruce acreditan/debitan la
--   de la plantilla 21. Para los tipos 9, 11, 21, 22 y 23 las dos tienen que ser LA MISMA cuenta.
--   (!) Si COINCIDE sale 'NO' en alguna fila, o un tipo no sale: avisar al arbitro con la salida
--       (no se corrige aca: CTAP tiene su propia pantalla).
SELECT t.TPAPCDGO AS TIPO, t.TPAPNMBR AS TIPO_NOMBRE,
       np.PLNNCNTA AS CTAP_PASIVO, c.CTAPESTD AS CTAP_ESTADO,
       nd.PLNNCNTA AS PLANTILLA21_CUENTA, d.DTPLAXL1 AS AUX1,
       CASE WHEN np.PLNNCNTA = nd.PLNNCNTA THEN 'SI' ELSE 'NO' END AS COINCIDE
  FROM CRD.TPAP t
  LEFT JOIN CRD.CTAP c ON c.TPAPCDGO = t.TPAPCDGO AND c.PJRQCDGO = 1236
  LEFT JOIN CNT.PLNN np ON np.PLNNCDGO = c.CTAPPLNP
  LEFT JOIN CNT.PLNS p ON p.PLNSCDAL = 21 AND p.PJRQCDGO = 1236
  LEFT JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
                      AND d.DTPLAXL1 = CASE t.TPAPCDGO WHEN 11 THEN 50 WHEN 9 THEN 51 WHEN 23 THEN 53
                                                       WHEN 22 THEN 54 WHEN 21 THEN 55 END
  LEFT JOIN CNT.PLNN nd ON nd.PLNNCDGO = d.PLNNCDGO
 WHERE t.TPAPCDGO IN (9, 11, 21, 22, 23)
 ORDER BY t.TPAPCDGO;

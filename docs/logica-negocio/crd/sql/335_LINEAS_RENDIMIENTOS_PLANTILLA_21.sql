-- =====================================================================================
-- 335 — Cuentas de los rendimientos 12 y 24 en la plantilla 21 (aux1 = 56 y 57)
-- Cuadre Superintendencia, C4 · 2026-10-29 · SIN EJECUTAR
--
-- Por que: en el re-proceso de la carga 352 (corrida 2) el reparto del AH creo un aporte de 0,09
--   tipo 12 (RENDIMIENTO CESANTIA PERSONAL, entidad 5079) y el asiento de aplicacion solo
--   contabilizaba 9 y 11: el aporte quedo sin linea y 2.3.02.05 en -0,09. El reparto ya no crea
--   rendimientos (C4, regla de tipo D-022 corregida), pero el asiento de aplicacion ahora exige
--   que TODO tipo de aporte de la carga tenga su linea (CrdLineaAsiento.aux1ParaTipoAporte) y
--   se detiene si falta. Este script crea las de los rendimientos personales, con la misma cuenta
--   que CRD.CTAP y que la apertura del 31-may-2025:
--     12 RENDIMIENTO CESANTIA PERSONAL   -> 2.1.01.05.02 RENDIMIENTOS PERSONALES CESANTIA   (aux1 56)
--     24 RENDIMIENTO JUBILACION PERSONAL -> 2.1.02.05.02 RENDIMIENTOS PERSONALES JUBILACION (aux1 57)
--   Igual que sus hermanas 50-55: DTPLMVMN = 2 (HABER), DTPLAXL2 = 0.
--
-- No es requisito de la corrida 3 de la 352 (ya no se crean aportes 12/24); se corre para que el
-- asiento nunca vuelva a omitir un tipo. Empresa 1236. Correr bloque por bloque.
-- =====================================================================================


-- BLOQUE 1 — Las cuentas existen y son de movimiento. Tienen que salir DOS filas (nivel 5).
SELECT n.PLNNCDGO, n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS NOMBRE, n.PLNNNVLL AS NIVEL,
       n.PLNNTPOO AS TIPO, n.PLNNESTD AS ESTADO
  FROM CNT.PLNN n
 WHERE n.PJRQCDGO = 1236
   AND n.PLNNCNTA IN ('2.1.01.05.02', '2.1.02.05.02');


-- BLOQUE 2 — Lineas actuales de la plantilla 21.
--   (!) Si alguna fila ya apunta a 2.1.01.05.02 o 2.1.02.05.02, o AUX1 56/57 ya existen: NO
--       correr el bloque 3, avisar con su AUX1.
SELECT p.PLNSCDGO, p.PLNSCDAL AS ALTERNO, d.DTPLCDGO, d.DTPLAXL1 AS AUX1, d.DTPLAXL2 AS AUX2,
       d.DTPLMVMN AS MOVIMIENTO, n.PLNNCNTA AS CUENTA, d.DTPLDSCR AS DESCRIPCION, d.DTPLESTD AS ESTADO_LINEA
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


-- BLOQUE 3 — (ESCRIBE) Crea las dos lineas. El NOT EXISTS impide duplicarlas.
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLAXL1, DTPLAXL2, DTPLMVMN, DTPLDSCR, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, n.PLNNCDGO, 56, 0, 2,
       'Rendimientos personales CESANTIA (tipo 12)', 1
  FROM CNT.PLNS p
  JOIN CNT.PLNN n ON n.PJRQCDGO = p.PJRQCDGO AND n.PLNNCNTA = '2.1.01.05.02'
 WHERE p.PLNSCDAL = 21
   AND p.PJRQCDGO = 1236
   AND NOT EXISTS (SELECT 1 FROM CNT.DTPL d WHERE d.PLNSCDGO = p.PLNSCDGO AND d.DTPLAXL1 = 56);
-- (!) "1 fila insertada".

INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLAXL1, DTPLAXL2, DTPLMVMN, DTPLDSCR, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, n.PLNNCDGO, 57, 0, 2,
       'Rendimientos personales JUBILACION (tipo 24)', 1
  FROM CNT.PLNS p
  JOIN CNT.PLNN n ON n.PJRQCDGO = p.PJRQCDGO AND n.PLNNCNTA = '2.1.02.05.02'
 WHERE p.PLNSCDAL = 21
   AND p.PJRQCDGO = 1236
   AND NOT EXISTS (SELECT 1 FROM CNT.DTPL d WHERE d.PLNSCDGO = p.PLNSCDGO AND d.DTPLAXL1 = 57);
-- (!) "1 fila insertada".
COMMIT;


-- BLOQUE 4 — Verificacion y coherencia con CRD.CTAP: dos filas con COINCIDE = 'SI'.
SELECT t.TPAPCDGO AS TIPO, t.TPAPNMBR AS TIPO_NOMBRE, np.PLNNCNTA AS CTAP_PASIVO,
       nd.PLNNCNTA AS PLANTILLA21_CUENTA, d.DTPLAXL1 AS AUX1,
       CASE WHEN np.PLNNCNTA = nd.PLNNCNTA THEN 'SI' ELSE 'NO' END AS COINCIDE
  FROM CRD.TPAP t
  LEFT JOIN CRD.CTAP c ON c.TPAPCDGO = t.TPAPCDGO AND c.PJRQCDGO = 1236
  LEFT JOIN CNT.PLNN np ON np.PLNNCDGO = c.CTAPPLNP
  LEFT JOIN CNT.PLNS p ON p.PLNSCDAL = 21 AND p.PJRQCDGO = 1236
  LEFT JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
                      AND d.DTPLAXL1 = CASE t.TPAPCDGO WHEN 12 THEN 56 WHEN 24 THEN 57 END
  LEFT JOIN CNT.PLNN nd ON nd.PLNNCDGO = d.PLNNCDGO
 WHERE t.TPAPCDGO IN (12, 24)
 ORDER BY t.TPAPCDGO;


-- REVERSO (solo si hay que deshacer):
-- DELETE FROM CNT.DTPL d
--  WHERE d.DTPLAXL1 IN (56, 57)
--    AND d.PLNSCDGO = (SELECT p.PLNSCDGO FROM CNT.PLNS p WHERE p.PLNSCDAL = 21 AND p.PJRQCDGO = 1236);
-- COMMIT;

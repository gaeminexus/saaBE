-- =====================================================================================
-- 356 — SEMILLAS. omen-saa-1, 2026-11-01. Plan cuentas a plantilla (cnt/PLAN-CUENTAS-A-PLANTILLA-2026-11-01.md)
-- P3: ajuste de bandas del cierre de cartera (⑨). Antes: cuentas 1.3.01.25 / 1.3.05.25 / 1.3.09.25
--     escritas en el código. Ahora: plantilla 37 (provisión de cartera), aux1 85
--     (CrdLineaAsiento.AJUSTE_BANDA_ABSORBE), aux2 = tipo de préstamo (1 quirografario = grupo Q,
--     2 hipotecario = grupo H, 3 prendario = grupo P), igual que las líneas 82-84.
-- P5: redondeo SRI del asiento de compra cxp. Antes: 4.8.90.90.35 en el código. Ahora: plantilla
--     NUEVA alterno 38 «CXP COMPRAS - CUENTAS DE AJUSTE», aux1 1 (CxpLineaAsiento.REDONDEO_SRI).
--
-- LAS CUENTAS QUE SE SIEMBRAN SON EXACTAMENTE LAS QUE HOY ESTÁN EN EL CÓDIGO: el resultado contable no
-- cambia, solo pasa a ser configurable desde la pantalla de plantillas.
--
-- ⛔ VA ANTES DEL EAR que trae P3 y P5. Con ese EAR y sin esta semilla:
--    - el cierre de cartera se DETIENE en el ajuste de bandas (mensaje claro);
--    - todo documento de compra con diferencia de redondeo >= 0,01 deja de contabilizarse.
-- Con el EAR de hoy, las líneas nuevas sobran pero no estorban (nadie las lee).
-- Empresa: 1236 (la de producción, como todos los scripts de plantillas de CRD).
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES
-- 0.1 Plantilla 37 existe (1 fila) y no tiene aux1 85 (0 en LINEAS_85).
SELECT p.PLNSCDGO, p.PLNSNMBR,
       (SELECT COUNT(*) FROM CNT.DTPL d WHERE d.PLNSCDGO = p.PLNSCDGO AND d.DTPLAXL1 = 85) AS LINEAS_85
  FROM CNT.PLNS p WHERE p.PLNSCDAL = 37 AND p.PJRQCDGO = 1236;
-- 0.2 El alterno 38 está libre. ESPERADO: 0 filas. (!) Si sale algo, PARAR y pasar la salida.
SELECT p.PLNSCDGO, p.PLNSNMBR, p.PJRQCDGO FROM CNT.PLNS p WHERE p.PLNSCDAL = 38;
-- 0.3 Las cuatro cuentas existen en la empresa. ESPERADO: 4 filas.
SELECT n.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR, n.PLNNESTD
  FROM CNT.PLNN n
 WHERE n.PJRQCDGO = 1236 AND n.PLNNCNTA IN ('1.3.01.25', '1.3.05.25', '1.3.09.25', '4.8.90.90.35')
 ORDER BY n.PLNNCNTA;
-- 0.4 Las secuencias van por delante del MAX (si no, ORA-00001).
SELECT 'PLNS' AS TABLA, (SELECT MAX(PLNSCDGO) FROM CNT.PLNS) AS MAX_PK,
       (SELECT LAST_NUMBER FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER = 'CNT' AND SEQUENCE_NAME = 'SQ_PLNSCDGO') AS SECUENCIA FROM DUAL
UNION ALL
SELECT 'DTPL', (SELECT MAX(DTPLCDGO) FROM CNT.DTPL),
       (SELECT LAST_NUMBER FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER = 'CNT' AND SEQUENCE_NAME = 'SQ_DTPLCDGO') FROM DUAL;


-- BLOQUE 1 — (ESCRIBE) P3: tres líneas aux1 85 en la plantilla 37. El NOT EXISTS impide duplicarlas.
--   MVMN: el código decide el lado por el signo de la diferencia; se siembra 1 (DEBE).
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLAXL1, DTPLAXL2, DTPLMVMN, DTPLDSCR, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, n.PLNNCDGO, 85, x.TIPO, 1, x.DSCR, 1
  FROM CNT.PLNS p
  JOIN (SELECT 1 AS TIPO, '1.3.01.25' AS CUENTA, 'Ajuste de bandas a saldo: cuenta que absorbe, grupo Q (quirografario)' AS DSCR FROM DUAL
        UNION ALL SELECT 2, '1.3.09.25', 'Ajuste de bandas a saldo: cuenta que absorbe, grupo H (hipotecario)' FROM DUAL
        UNION ALL SELECT 3, '1.3.05.25', 'Ajuste de bandas a saldo: cuenta que absorbe, grupo P (prendario)' FROM DUAL) x ON 1 = 1
  JOIN CNT.PLNN n ON n.PJRQCDGO = p.PJRQCDGO AND n.PLNNCNTA = x.CUENTA
 WHERE p.PLNSCDAL = 37 AND p.PJRQCDGO = 1236
   AND NOT EXISTS (SELECT 1 FROM CNT.DTPL d WHERE d.PLNSCDGO = p.PLNSCDGO AND d.DTPLAXL1 = 85 AND d.DTPLAXL2 = x.TIPO);
-- (!) "3 filas insertadas".


-- BLOQUE 2 — (ESCRIBE) P5: plantilla nueva alterno 38 y su línea aux1 1.
--   PLNSSSTM se copia de la plantilla 37 de la misma empresa (como hizo crd/sql/310): el código no lo
--   usa para resolver la plantilla.
INSERT INTO CNT.PLNS (PLNSCDGO, PLNSNMBR, PLNSCDAL, PLNSESTD, PJRQCDGO, PLNSOBSR, PLNSSSTM)
SELECT CNT.SQ_PLNSCDGO.NEXTVAL, 'CXP COMPRAS - CUENTAS DE AJUSTE', 38, 1, p37.PJRQCDGO,
       'Cuentas de ajuste del asiento de compra cxp (ese asiento no usa plantilla para sus lineas normales). aux1 1 = diferencia por redondeo SRI (CxpLineaAsiento.REDONDEO_SRI). Creada 2026-11-01, crd/sql/356.',
       p37.PLNSSSTM
  FROM CNT.PLNS p37
 WHERE p37.PLNSCDAL = 37 AND p37.PJRQCDGO = 1236
   AND NOT EXISTS (SELECT 1 FROM CNT.PLNS q WHERE q.PLNSCDAL = 38 AND q.PJRQCDGO = p37.PJRQCDGO);
-- (!) "1 fila insertada".

INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLAXL1, DTPLAXL2, DTPLMVMN, DTPLDSCR, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, n.PLNNCDGO, 1, 0, 1, 'Diferencia por redondeo SRI en compras', 1
  FROM CNT.PLNS p
  JOIN CNT.PLNN n ON n.PJRQCDGO = p.PJRQCDGO AND n.PLNNCNTA = '4.8.90.90.35'
 WHERE p.PLNSCDAL = 38 AND p.PJRQCDGO = 1236
   AND NOT EXISTS (SELECT 1 FROM CNT.DTPL d WHERE d.PLNSCDGO = p.PLNSCDGO AND d.DTPLAXL1 = 1);
-- (!) "1 fila insertada".
COMMIT;


-- BLOQUE 3 — CONTROL DESPUÉS. ESPERADO: 4 filas, cada una con su cuenta de hoy:
--   37/85/1 = 1.3.01.25 · 37/85/2 = 1.3.09.25 · 37/85/3 = 1.3.05.25 · 38/1/0 = 4.8.90.90.35
SELECT p.PLNSCDAL AS ALTERNO, d.DTPLAXL1 AS AUX1, d.DTPLAXL2 AS AUX2, n.PLNNCNTA AS CUENTA, d.DTPLDSCR, d.DTPLESTD
  FROM CNT.PLNS p JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
  JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PJRQCDGO = 1236
   AND ((p.PLNSCDAL = 37 AND d.DTPLAXL1 = 85) OR (p.PLNSCDAL = 38 AND d.DTPLAXL1 = 1))
 ORDER BY p.PLNSCDAL, d.DTPLAXL1, d.DTPLAXL2;


-- REVERSO (comentado; solo con el EAR anterior a P3/P5 desplegado):
-- DELETE FROM CNT.DTPL d WHERE d.DTPLAXL1 = 85
--    AND d.PLNSCDGO = (SELECT p.PLNSCDGO FROM CNT.PLNS p WHERE p.PLNSCDAL = 37 AND p.PJRQCDGO = 1236);
-- DELETE FROM CNT.DTPL d
--  WHERE d.PLNSCDGO = (SELECT p.PLNSCDGO FROM CNT.PLNS p WHERE p.PLNSCDAL = 38 AND p.PJRQCDGO = 1236);
-- DELETE FROM CNT.PLNS p WHERE p.PLNSCDAL = 38 AND p.PJRQCDGO = 1236;
-- COMMIT;

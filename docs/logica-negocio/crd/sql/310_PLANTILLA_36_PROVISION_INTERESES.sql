-- =====================================================================================
-- 310 — CNT.PLNS alterno 36 «CRD PROVISION INTERESES» y sus lineas por tipo de prestamo
-- FECHA: 2026-10-05 · EQUIPO: omen-saa-1 (CRD · EQUIPO B) · alterno 36 reservado en el registro §2c
--
-- PARA QUE: el paso 7 del cierre de cartera (provision de intereses no cobrados) y sus reversos
-- por cobro. Asiento de la Superintendencia (correo de contabilidad del 2026-09-17):
--     provision:  D 4.7.05.10 Intereses inversiones privativas / H 1.4.99.05 Provisiones intereses
--     reverso:    D 1.4.99.05 / H 4.7.05.10
--
-- LAS LINEAS VAN POR TIPO DE PRESTAMO (R4): DTPLAXL2 = CRD.TPPR.TPPRCDGO (1 quirografario,
-- 2 hipotecario, 3 prendario), como las lineas de interes de la plantilla 21. DECISION DEL USUARIO
-- (2026-10-05): HOY LA MISMA CUENTA para los tres tipos. Si manana contabilidad abre subcuentas por
-- tipo, se cambia el PLNNCDGO de la linea de ese tipo (UPDATE de una fila), sin tocar codigo.
--
-- DTPLAXL1 = papel del catalogo semantico CrdLineaAsiento:
--     80 PROVISION_INTERESES_GASTO  → 4.7.05.10   (DEBE en la provision)
--     81 PROVISION_INTERESES        → 1.4.99.05   (HABER en la provision)
-- El reverso usa las mismas lineas con los lados invertidos: lo decide el codigo, no la plantilla.
--
-- SQL PURO. Correr por bloques.
-- =====================================================================================

-- 0. CONTROLES PREVIOS ------------------------------------------------------------------
-- 0.1 Las dos cuentas existen, una vez cada una, ACTIVAS y de movimiento. Esperado: EXACTAMENTE 2 filas.
--     Si una no aparece o aparece repetida, PARAR: el insert de abajo no sabria cual tomar.
SELECT c.PLNNCDGO, c.PLNNCNTA, c.PLNNNMBR, c.PLNNESTD
  FROM CNT.PLNN c
 WHERE REPLACE(c.PLNNCNTA, '.', '') IN ('470510', '149905')
 ORDER BY c.PLNNCNTA;

-- 0.2 El alterno 36 esta libre. Esperado: 0 filas.
SELECT p.PLNSCDGO, p.PLNSCDAL, p.PLNSNMBR FROM CNT.PLNS p WHERE p.PLNSCDAL = 36;

-- 0.3 La plantilla molde (29) de la que se toman empresa y sistema. Esperado: 1 fila.
SELECT p.PLNSCDGO, p.PLNSCDAL, p.PLNSNMBR, p.PJRQCDGO, p.PLNSSSTM FROM CNT.PLNS p WHERE p.PLNSCDAL = 29;

-- 0.4 Los tres tipos de prestamo. Esperado: 1, 2 y 3.
SELECT t.TPPRCDGO, t.TPPRNMBR FROM CRD.TPPR t WHERE t.TPPRCDGO IN (1, 2, 3) ORDER BY 1;


-- 1. LA PLANTILLA -----------------------------------------------------------------------
INSERT INTO CNT.PLNS (PLNSCDGO, PLNSNMBR, PLNSCDAL, PLNSESTD, PJRQCDGO, PLNSOBSR, PLNSSSTM)
SELECT  CNT.SQ_PLNSCDGO.NEXTVAL,
        'CRD PROVISION INTERESES',
        36,
        1,
        p29.PJRQCDGO,
        'Provision mensual de intereses (ordinario y mora) no cobrados y sus reversos por cobro. Catalogo de la Super: D 470510 / H 149905. Lineas por tipo de prestamo (DTPLAXL2 = TPPRCDGO); papeles 80 y 81 de CrdLineaAsiento. Creada 2026-10-05, sql/310.',
        p29.PLNSSSTM
  FROM CNT.PLNS p29
 WHERE p29.PLNSCDAL = 29;


-- 2. LAS SEIS LINEAS: 2 papeles x 3 tipos de prestamo. MVMN: 1 = DEBE, 2 = HABER (sentido de la provision).
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1,
                      DTPLAXL2, DTPLAXL3, DTPLAXL4, DTPLAXL5, DTPLESTD)
SELECT  CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, c.PLNNCDGO,
        'INTERESES INVERSIONES PRIVATIVAS - ' || t.TPPRNMBR, 1, 80, t.TPPRCDGO, 0, 0, 0, 1
  FROM CNT.PLNS p, CNT.PLNN c, CRD.TPPR t
 WHERE p.PLNSCDAL = 36
   AND REPLACE(c.PLNNCNTA, '.', '') = '470510'
   AND t.TPPRCDGO IN (1, 2, 3);

INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1,
                      DTPLAXL2, DTPLAXL3, DTPLAXL4, DTPLAXL5, DTPLESTD)
SELECT  CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, c.PLNNCDGO,
        'PROVISIONES INTERESES INVERSIONES PRIVATIVAS - ' || t.TPPRNMBR, 2, 81, t.TPPRCDGO, 0, 0, 0, 1
  FROM CNT.PLNS p, CNT.PLNN c, CRD.TPPR t
 WHERE p.PLNSCDAL = 36
   AND REPLACE(c.PLNNCNTA, '.', '') = '149905'
   AND t.TPPRCDGO IN (1, 2, 3);

COMMIT;


-- 3. CONTROLES POSTERIORES -----------------------------------------------------------------
-- 3.1 Las seis lineas. Esperado: 6 filas, 80/81 x tipos 1/2/3, con la cuenta correcta en cada una.
SELECT d.DTPLAXL1 AS PAPEL, d.DTPLAXL2 AS TIPO_PRESTAMO, d.DTPLMVMN AS MVMN_1DEBE_2HABER,
       c.PLNNCNTA AS CUENTA, c.PLNNNMBR AS NOMBRE_CUENTA, d.DTPLDSCR, d.DTPLESTD
  FROM CNT.DTPL d
  JOIN CNT.PLNS p ON p.PLNSCDGO = d.PLNSCDGO
  JOIN CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
 WHERE p.PLNSCDAL = 36
 ORDER BY d.DTPLAXL1, d.DTPLAXL2;


-- 4. CAMBIO FUTURO — si contabilidad abre una subcuenta por tipo (ejemplo, NO correr hoy):
-- UPDATE CNT.DTPL d SET d.PLNNCDGO = (SELECT c.PLNNCDGO FROM CNT.PLNN c WHERE REPLACE(c.PLNNCNTA,'.','') = '47051002')
--  WHERE d.PLNSCDGO = (SELECT p.PLNSCDGO FROM CNT.PLNS p WHERE p.PLNSCDAL = 36) AND d.DTPLAXL1 = 80 AND d.DTPLAXL2 = 2;
-- COMMIT;


-- 5. REVERSO — COMENTADO (solo si todavia no se genero ningun asiento con la plantilla 36)
-- DELETE FROM CNT.DTPL d WHERE d.PLNSCDGO = (SELECT p.PLNSCDGO FROM CNT.PLNS p WHERE p.PLNSCDAL = 36);
-- DELETE FROM CNT.PLNS p WHERE p.PLNSCDAL = 36;
-- COMMIT;

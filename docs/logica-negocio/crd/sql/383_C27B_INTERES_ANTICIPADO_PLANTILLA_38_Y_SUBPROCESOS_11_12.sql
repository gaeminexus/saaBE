-- =====================================================================================
-- 383 — C27b (D-172): interés cobrado antes de su devengo — pase de base de datos.
-- Equipo omen-saa-1 · 2026-11-24 · REVISABLE: NO está ejecutado en ninguna base.
-- Diseño: docs/logica-negocio/crd/C27b-DISENO-INTERES-ANTICIPADO.md. Propiedades: docs/logica-negocio/crd/C27b-PROPIEDADES.md.
--
-- Qué hace (SQL puro, sin DDL de tablas):
--   (A) Amplía CK_ANCC_TPOO a 1..12: ④b = 11 (devengo de la primera cuota) y ⑪ = 12 (intereses recibidos por anticipado).
--   (B) Crea la plantilla alterno 38 «CRD INTERES ANTICIPADO» (empresa 1236) con dos líneas por tipo de préstamo
--       (aux2 = TPPRCDGO: 1 quirografario, 2 hipotecario, 3 prendario, igual que las plantillas 36 y 37):
--         aux1 86 interés por cobrar  1.4.02.xx — se COPIA del papel 10 de la plantilla 17, la misma cuenta del devengo.
--         aux1 87 intereses recibidos por anticipado — PASIVO: la cuenta la decide el contador (marcador abajo).
--
-- ⛔ La cuenta del pasivo NO está decidida. El bloque B2 trae el marcador «[DECIDE EL CONTADOR]»: el script se detiene
--    (ORA-20001) mientras no se lo reemplace por la cuenta real en LOS DOS lugares donde aparece (bloque B0 y B2).
--    Es una cuenta que ya exista en CNT.PLNN del proyecto 1236 (p. ej. una subcuenta propia de 2.9.01.05 por tipo, o
--    2.9.01.05 compartida con las inversiones: el código ajusta contra lo que el ⑪ ya asentó, no contra el saldo).
--
-- Antes de correr: backup de CNT.PLNS, CNT.DTPL y de la definición de CK_ANCC_TPOO. Correr bloque por bloque.
-- =====================================================================================


-- BLOQUE 0 — Controles previos (solo lectura).
-- 0a. La definición actual del CHECK. Esperado: ANCCTPOO IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10).
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION_VC, c.STATUS
  FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'ANCC' AND c.CONSTRAINT_NAME = 'CK_ANCC_TPOO';

-- 0b. La plantilla 38 no debe existir todavía. Esperado: 0 filas.
SELECT PLNSCDGO, PLNSNMBR, PLNSCDAL FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 38;

-- 0c. Las tres líneas de interés por cobrar de la plantilla 17 (papel 10). Esperado: 3 filas (tipos 1, 2, 3).
SELECT d.DTPLAXL2 AS TIPO, n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS NOMBRE
  FROM CNT.PLNS p JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PJRQCDGO = 1236 AND p.PLNSCDAL = 17 AND d.DTPLAXL1 = 10
 ORDER BY d.DTPLAXL2;

-- 0d. ¿Hay corridas de cierre con subproceso 11 o 12? Esperado: 0 (aún no existen).
SELECT ANCCTPOO, COUNT(*) FROM CRD.ANCC WHERE ANCCTPOO IN (11, 12) GROUP BY ANCCTPOO;


-- BLOQUE A — (ESCRIBE) CK_ANCC_TPOO 1..12.
ALTER TABLE CRD.ANCC DROP CONSTRAINT CK_ANCC_TPOO;
ALTER TABLE CRD.ANCC ADD CONSTRAINT CK_ANCC_TPOO CHECK (ANCCTPOO IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
-- (!) Control: la definición debe decir ... 11, 12) y STATUS ENABLED.
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION_VC, c.STATUS
  FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'ANCC' AND c.CONSTRAINT_NAME = 'CK_ANCC_TPOO';


-- BLOQUE B0 — Guarda del marcador. Reemplazar '[DECIDE EL CONTADOR]' por la cuenta del pasivo (la MISMA que en B2).
DECLARE
    v_cuenta_pasivo VARCHAR2(60) := '[DECIDE EL CONTADOR]';
    v_existe        NUMBER;
BEGIN
    IF v_cuenta_pasivo LIKE '[%' THEN
        RAISE_APPLICATION_ERROR(-20001, 'Falta la cuenta del pasivo de intereses recibidos por anticipado: la decide el contador. Reemplace el marcador en B0 y en B2.');
    END IF;
    SELECT COUNT(*) INTO v_existe FROM CNT.PLNN WHERE PJRQCDGO = 1236 AND PLNNCNTA = v_cuenta_pasivo;
    IF v_existe = 0 THEN
        RAISE_APPLICATION_ERROR(-20002, 'La cuenta ' || v_cuenta_pasivo || ' no existe en CNT.PLNN del proyecto 1236.');
    END IF;
END;
/


-- BLOQUE B1 — (ESCRIBE) cabecera de la plantilla 38. Las columnas, como la plantilla 37 (sql/332).
INSERT INTO CNT.PLNS (PLNSCDGO, PLNSNMBR, PLNSCDAL, PLNSESTD, PJRQCDGO, PLNSOBSR, PLNSSSTM)
SELECT CNT.SQ_PLNSCDGO.NEXTVAL, 'CRD INTERES ANTICIPADO', 38, 1, 1236,
       'Intereses recibidos por anticipado (paso 11 del cierre de cartera, C27b/D-172): D 1.4.02.xx / H pasivo, por tipo de prestamo (DTPLAXL2 = TPPRCDGO); papeles 86 y 87 de CrdLineaAsiento. El lado lo decide el codigo segun el signo del ajuste. Creada 2026-11-24, sql/383.',
       1
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 38);
-- (!) "1 fila insertada".


-- BLOQUE B2 — (ESCRIBE) las 6 líneas: 86 (copiadas del papel 10 de la plantilla 17) y 87 (cuenta del contador).
-- 86: interés por cobrar, la misma cuenta 1.4.02.xx del devengo, por tipo.
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1, DTPLAXL2, DTPLAXL3, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, p38.PLNSCDGO, d17.PLNNCDGO,
       'INTERES POR COBRAR (ANTICIPADO) - ' || n.PLNNNMBR, 1, 86, d17.DTPLAXL2, 0, 1
  FROM CNT.PLNS p38
  JOIN CNT.PLNS p17 ON p17.PJRQCDGO = p38.PJRQCDGO AND p17.PLNSCDAL = 17
  JOIN CNT.DTPL d17 ON d17.PLNSCDGO = p17.PLNSCDGO AND d17.DTPLAXL1 = 10 AND d17.DTPLESTD = 1
  JOIN CNT.PLNN n   ON n.PLNNCDGO = d17.PLNNCDGO
 WHERE p38.PJRQCDGO = 1236 AND p38.PLNSCDAL = 38
   AND NOT EXISTS (SELECT 1 FROM CNT.DTPL x WHERE x.PLNSCDGO = p38.PLNSCDGO AND x.DTPLAXL1 = 86 AND x.DTPLAXL2 = d17.DTPLAXL2);
-- (!) "3 filas insertadas".

-- 87: pasivo, la cuenta que decide el contador (la misma en los tres tipos, o una por tipo: cambiar el JOIN).
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1, DTPLAXL2, DTPLAXL3, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, p38.PLNSCDGO, n.PLNNCDGO,
       'INTERESES RECIBIDOS POR ANTICIPADO', 2, 87, t.TIPO, 0, 1
  FROM CNT.PLNS p38
  JOIN (SELECT 1 AS TIPO FROM DUAL UNION ALL SELECT 2 FROM DUAL UNION ALL SELECT 3 FROM DUAL) t ON 1 = 1
  JOIN CNT.PLNN n ON n.PJRQCDGO = p38.PJRQCDGO AND n.PLNNCNTA = '[DECIDE EL CONTADOR]'
 WHERE p38.PJRQCDGO = 1236 AND p38.PLNSCDAL = 38
   AND NOT EXISTS (SELECT 1 FROM CNT.DTPL x WHERE x.PLNSCDGO = p38.PLNSCDGO AND x.DTPLAXL1 = 87 AND x.DTPLAXL2 = t.TIPO);
-- (!) "3 filas insertadas". Si no son 3: ROLLBACK y avisar.
COMMIT;


-- BLOQUE C — Verificación. Esperado: 6 filas; 86 DEBE (1) y 87 HABER (2); tipos 1, 2, 3 en cada papel.
SELECT p.PLNSCDAL AS ALTERNO, d.DTPLAXL1 AS PAPEL, d.DTPLAXL2 AS TIPO, d.DTPLMVMN AS MOVIMIENTO,
       n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS NOMBRE
  FROM CNT.PLNS p JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PJRQCDGO = 1236 AND p.PLNSCDAL = 38
 ORDER BY d.DTPLAXL1, d.DTPLAXL2;


-- =====================================================================================
-- REVERSO (solo si hay que deshacer, y sin corridas que usen 11 o 12; ver el bloque 0d):
-- DELETE FROM CNT.DTPL WHERE PLNSCDGO = (SELECT PLNSCDGO FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 38);
-- DELETE FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 38;
-- ALTER TABLE CRD.ANCC DROP CONSTRAINT CK_ANCC_TPOO;
-- ALTER TABLE CRD.ANCC ADD CONSTRAINT CK_ANCC_TPOO CHECK (ANCCTPOO IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
-- COMMIT;
-- =====================================================================================

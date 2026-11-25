-- =====================================================================================
-- 383 — C27b (D-172): interés cobrado antes de su devengo — pase de base de datos.
-- Equipo omen-saa-1 · 2026-11-24 · REVISADO 2026-11-24 (alterno 39: el 38 es PlantillasCxp.COMPRAS) · REVISABLE: NO está
-- ejecutado en ninguna base.
-- Diseño: docs/logica-negocio/crd/C27b-DISENO-INTERES-ANTICIPADO.md. Propiedades: docs/logica-negocio/crd/C27b-PROPIEDADES.md.
--
-- Qué hace (SQL puro, sin DDL de tablas):
--   (A) Amplía CK_ANCC_TPOO a 1..12: ④b = 11 (devengo de la primera cuota) y ⑪ = 12 (intereses recibidos por anticipado).
--   (B) Crea la plantilla alterno 39 «CRD INTERES ANTICIPADO» (empresa 1236) con dos líneas por tipo de préstamo
--       (aux2 = TPPRCDGO: 1 quirografario, 2 hipotecario, 3 prendario, igual que las plantillas 36 y 37):
--         aux1 86 interés por cobrar  1.4.02.xx — se COPIA del papel 10 de la plantilla 17, la misma cuenta del devengo.
--         aux1 87 intereses recibidos por anticipado — PASIVO 2.9.01.05, compartida y la misma para los tres tipos
--                 (D2 decidido: la cuenta del ⑪ es 2.9.01.05; se busca en CNT.PLNN por su número, sin puntos).
--
-- ⛔ El alterno 38 es PlantillasCxp.COMPRAS («CXP COMPRAS - CUENTAS DE AJUSTE», sql/356). Una versión anterior de este script usaba
--    el 38: encontró la cabecera de CxP y metió ahí las líneas 86/87. Por eso este script:
--      · SE DETIENE (ORA-20003) si ya existe CUALQUIER plantilla con alterno 39 en la empresa, se llame como se llame;
--      · inserta las líneas 86/87 SOLO en la plantilla que él mismo crea, buscada por alterno 39 Y nombre «CRD INTERES ANTICIPADO»,
--        nunca solo por alterno.
--
-- Antes de correr: backup de CNT.PLNS, CNT.DTPL y de la definición de CK_ANCC_TPOO. Correr bloque por bloque.
-- =====================================================================================


-- BLOQUE 0 — Controles previos (solo lectura).
-- 0a. La definición actual del CHECK. Esperado: ANCCTPOO IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10).
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION_VC, c.STATUS
  FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'ANCC' AND c.CONSTRAINT_NAME = 'CK_ANCC_TPOO';

-- 0b. El alterno 39 tiene que estar LIBRE. Esperado: 0 filas (el bloque de guarda de abajo lo exige y se detiene si no).
SELECT PLNSCDGO, PLNSNMBR, PLNSCDAL FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 39;

-- 0c. Las tres líneas de interés por cobrar de la plantilla 17 (papel 10). Esperado: 3 filas (tipos 1, 2, 3).
SELECT d.DTPLAXL2 AS TIPO, n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS NOMBRE
  FROM CNT.PLNS p JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PJRQCDGO = 1236 AND p.PLNSCDAL = 17 AND d.DTPLAXL1 = 10
 ORDER BY d.DTPLAXL2;

-- 0d. La cuenta del pasivo 2.9.01.05 (por su número sin puntos). Esperado: 1 fila.
SELECT PLNNCDGO, PLNNCNTA, PLNNNMBR, PLNNTPOO FROM CNT.PLNN
 WHERE PJRQCDGO = 1236 AND REPLACE(PLNNCNTA, '.', '') = '290105';

-- 0e. ¿Hay corridas de cierre con subproceso 11 o 12? Esperado: 0 (aún no existen).
SELECT ANCCTPOO, COUNT(*) FROM CRD.ANCC WHERE ANCCTPOO IN (11, 12) GROUP BY ANCCTPOO;


-- BLOQUE G — GUARDA. Corre ANTES de cualquier escritura. Se detiene si el alterno 39 ya existe en la empresa (con cualquier
-- nombre) o si la cuenta 2.9.01.05 no existe o no es única.
DECLARE
    v_existe  NUMBER;
    v_cuentas NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_existe FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 39;
    IF v_existe > 0 THEN
        RAISE_APPLICATION_ERROR(-20003, 'El alterno 39 ya existe en la empresa 1236 (' || v_existe || ' plantilla/s): este script no escribe sobre una plantilla ajena. Revise CNT.PLNS y REGISTRO-RESERVAS-EQUIPOS.');
    END IF;
    SELECT COUNT(*) INTO v_cuentas FROM CNT.PLNN WHERE PJRQCDGO = 1236 AND REPLACE(PLNNCNTA, '.', '') = '290105';
    IF v_cuentas <> 1 THEN
        RAISE_APPLICATION_ERROR(-20002, 'La cuenta 2.9.01.05 debe existir una sola vez en CNT.PLNN del proyecto 1236 (hay ' || v_cuentas || ').');
    END IF;
END;
/


-- BLOQUE A — (ESCRIBE) CK_ANCC_TPOO 1..12.
ALTER TABLE CRD.ANCC DROP CONSTRAINT CK_ANCC_TPOO;
ALTER TABLE CRD.ANCC ADD CONSTRAINT CK_ANCC_TPOO CHECK (ANCCTPOO IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
-- (!) Control: la definición debe decir ... 11, 12) y STATUS ENABLED.
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION_VC, c.STATUS
  FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'ANCC' AND c.CONSTRAINT_NAME = 'CK_ANCC_TPOO';


-- BLOQUE B1 — (ESCRIBE) cabecera de la plantilla 39. Las columnas, como la plantilla 37 (sql/332).
-- Sin NOT EXISTS a propósito: el bloque G ya garantizó que el alterno 39 está libre; si algo cambió entre G y este INSERT,
-- el índice único de alterno, o el control de abajo, lo delatan.
INSERT INTO CNT.PLNS (PLNSCDGO, PLNSNMBR, PLNSCDAL, PLNSESTD, PJRQCDGO, PLNSOBSR, PLNSSSTM)
SELECT CNT.SQ_PLNSCDGO.NEXTVAL, 'CRD INTERES ANTICIPADO', 39, 1, 1236,
       'Intereses recibidos por anticipado (paso 11 del cierre de cartera, C27b/D-172): D 1.4.02.xx / H 2.9.01.05, por tipo de prestamo (DTPLAXL2 = TPPRCDGO); papeles 86 y 87 de CrdLineaAsiento. El lado lo decide el codigo segun el signo del ajuste. Creada 2026-11-24, sql/383.',
       1
  FROM DUAL;
-- (!) "1 fila insertada". Control: una sola plantilla con alterno 39, y es la nuestra.
SELECT PLNSCDGO, PLNSNMBR, PLNSCDAL FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 39;


-- BLOQUE B2 — (ESCRIBE) las 6 líneas, SOLO en la plantilla que crea este script (alterno 39 Y nombre «CRD INTERES ANTICIPADO»).
-- 86: interés por cobrar, la misma cuenta 1.4.02.xx del devengo, por tipo (copiada del papel 10 de la plantilla 17).
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1, DTPLAXL2, DTPLAXL3, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, p39.PLNSCDGO, d17.PLNNCDGO,
       'INTERES POR COBRAR (ANTICIPADO) - ' || n.PLNNNMBR, 1, 86, d17.DTPLAXL2, 0, 1
  FROM CNT.PLNS p39
  JOIN CNT.PLNS p17 ON p17.PJRQCDGO = p39.PJRQCDGO AND p17.PLNSCDAL = 17
  JOIN CNT.DTPL d17 ON d17.PLNSCDGO = p17.PLNSCDGO AND d17.DTPLAXL1 = 10 AND d17.DTPLESTD = 1
  JOIN CNT.PLNN n   ON n.PLNNCDGO = d17.PLNNCDGO
 WHERE p39.PJRQCDGO = 1236 AND p39.PLNSCDAL = 39 AND p39.PLNSNMBR = 'CRD INTERES ANTICIPADO'
   AND NOT EXISTS (SELECT 1 FROM CNT.DTPL x WHERE x.PLNSCDGO = p39.PLNSCDGO AND x.DTPLAXL1 = 86 AND x.DTPLAXL2 = d17.DTPLAXL2);
-- (!) "3 filas insertadas".

-- 87: pasivo 2.9.01.05, compartida y la misma para los tres tipos (D2 decidido), buscada por número sin puntos.
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1, DTPLAXL2, DTPLAXL3, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, p39.PLNSCDGO, n.PLNNCDGO,
       'INTERESES RECIBIDOS POR ANTICIPADO', 2, 87, t.TIPO, 0, 1
  FROM CNT.PLNS p39
  JOIN (SELECT 1 AS TIPO FROM DUAL UNION ALL SELECT 2 FROM DUAL UNION ALL SELECT 3 FROM DUAL) t ON 1 = 1
  JOIN CNT.PLNN n ON n.PJRQCDGO = p39.PJRQCDGO AND REPLACE(n.PLNNCNTA, '.', '') = '290105'
 WHERE p39.PJRQCDGO = 1236 AND p39.PLNSCDAL = 39 AND p39.PLNSNMBR = 'CRD INTERES ANTICIPADO'
   AND NOT EXISTS (SELECT 1 FROM CNT.DTPL x WHERE x.PLNSCDGO = p39.PLNSCDGO AND x.DTPLAXL1 = 87 AND x.DTPLAXL2 = t.TIPO);
-- (!) "3 filas insertadas". Si no son 3 en cada INSERT: ROLLBACK y avisar.
COMMIT;


-- BLOQUE C — Verificación. Esperado: 6 filas en la plantilla 39; 86 DEBE (1) y 87 HABER (2); tipos 1, 2, 3 en cada papel; el papel 87
-- con la cuenta 2.9.01.05 en los tres. Y ninguna línea 86/87 en OTRA plantilla (segunda consulta: 0 filas).
SELECT p.PLNSCDAL AS ALTERNO, d.DTPLAXL1 AS PAPEL, d.DTPLAXL2 AS TIPO, d.DTPLMVMN AS MOVIMIENTO,
       n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS NOMBRE
  FROM CNT.PLNS p JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PJRQCDGO = 1236 AND p.PLNSCDAL = 39
 ORDER BY d.DTPLAXL1, d.DTPLAXL2;

SELECT p.PLNSCDAL AS ALTERNO, p.PLNSNMBR, d.DTPLAXL1 AS PAPEL, d.DTPLAXL2 AS TIPO
  FROM CNT.PLNS p JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
 WHERE p.PJRQCDGO = 1236 AND d.DTPLAXL1 IN (86, 87) AND NOT (p.PLNSCDAL = 39 AND p.PLNSNMBR = 'CRD INTERES ANTICIPADO');


-- =====================================================================================
-- REVERSO (solo si hay que deshacer, y sin corridas que usen 11 o 12; ver el bloque 0e):
-- DELETE FROM CNT.DTPL WHERE PLNSCDGO = (SELECT PLNSCDGO FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 39 AND PLNSNMBR = 'CRD INTERES ANTICIPADO');
-- DELETE FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 39 AND PLNSNMBR = 'CRD INTERES ANTICIPADO';
-- ALTER TABLE CRD.ANCC DROP CONSTRAINT CK_ANCC_TPOO;
-- ALTER TABLE CRD.ANCC ADD CONSTRAINT CK_ANCC_TPOO CHECK (ANCCTPOO IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
-- COMMIT;
-- Si una corrida anterior de este script (versión con el alterno 38) metió líneas 86/87 en la plantilla de compras de CxP
-- (alterno 38, «CXP COMPRAS - CUENTAS DE AJUSTE»), retirarlas con:
-- DELETE FROM CNT.DTPL WHERE DTPLAXL1 IN (86, 87) AND PLNSCDGO IN (SELECT PLNSCDGO FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 38);
-- (el cuadre ya las retiró de su base; correr solo en las bases donde se haya aplicado la versión anterior).
-- =====================================================================================

-- =====================================================================================
-- 332 — C15 (cuadre Superintendencia, D-015/D-016): plantilla 37 "CRD PROVISION CARTERA" y
--       sub-procesos 8 y 9 del cierre de cartera.
-- 2026-10-28 · REVISABLE: NO está ejecutado en ninguna base.
--
-- Qué hace:
--   (A) Amplía CK_ANCC_TPOO para aceptar los sub-procesos nuevos del cierre:
--         8 = Provisión de cartera (1.3.99, ajuste a saldo a la requerida de la G48 al corte)
--        10 = Devengo de mora al corte (D-017, H4: D 1.4.02.xx / H 5.1.02.xx con la plantilla 17)
--         9 = Ajuste de bandas a saldo (1.3.xx por cuenta de banda contra el módulo al corte;
--             en la corrida de ARRANQUE es el reparto de la banda .25 de D-001)
--   (B) Crea la plantilla alterno 37 con las cuentas que el diseño PV1–PV7
--       (DISENO-GARANTIA-BIEN-Y-PROVISION-CARTERA.md) dejó pendientes del contador:
--         aux1 82 GASTO por provisión          (DEBE)  4.7.04.05 / 4.7.04.15 / 4.7.04.10
--         aux1 83 PROVISION 1.3.99             (HABER) 1.3.99.05 / 1.3.99.15 / 1.3.99.10
--         aux1 84 REVERSION DE PROVISIONES     (HABER) 5.3.90.05 (D-017, definida por el contador:
--                                                      las cuentas de su asiento manual 9529 del
--                                                      31-ago-2026)
--       aux2 = tipo de préstamo (CRD.TPPR): 1 quirografario, 2 hipotecario, 3 prendario — igual
--       que la plantilla 36.
--   Sin la plantilla 37 el cierre NO falla: omite el ⑧ con una advertencia.
--
-- ORDEN: correr ANTES de ejecutar un cierre con el WAR de C15 que tenga el ⑧/⑨ activos (el
-- ⑨ sí se ejecuta sin plantilla, pero ANCC rechaza el sub-proceso 9 sin el bloque A).
-- Empresa 1236. Correr bloque por bloque y mirar cada salida. SQL puro (sin SET/DEFINE).
-- =====================================================================================


-- BLOQUE 0 — Control previo: las 7 cuentas existen y son de movimiento (PLNNTPOO = 2). Deben salir 7 filas.
SELECT n.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR, n.PLNNTPOO, n.PLNNESTD
  FROM CNT.PLNN n
 WHERE n.PJRQCDGO = 1236
   AND n.PLNNCNTA IN ('4.7.04.05', '4.7.04.10', '4.7.04.15', '1.3.99.05', '1.3.99.10', '1.3.99.15', '5.3.90.05')
 ORDER BY n.PLNNCNTA;

-- BLOQUE 0b — No existe todavía una plantilla 37 en la empresa (debe salir 0).
SELECT COUNT(*) AS PLANTILLAS_37 FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 37;

-- BLOQUE 0c — Restricción actual (debe decir ANCCTPOO IN (1, 2, 3, 4, 5, 6, 7)).
SELECT CONSTRAINT_NAME, SEARCH_CONDITION_VC
  FROM ALL_CONSTRAINTS WHERE OWNER = 'CRD' AND TABLE_NAME = 'ANCC' AND CONSTRAINT_NAME = 'CK_ANCC_TPOO';


-- BLOQUE A — (DDL) sub-procesos 8 y 9.
ALTER TABLE CRD.ANCC DROP CONSTRAINT CK_ANCC_TPOO;
ALTER TABLE CRD.ANCC ADD CONSTRAINT CK_ANCC_TPOO CHECK (ANCCTPOO IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10));


-- BLOQUE B1 — (ESCRIBE) cabecera de la plantilla 37.
INSERT INTO CNT.PLNS (PLNSCDGO, PLNSNMBR, PLNSCDAL, PLNSESTD, PJRQCDGO, PLNSOBSR, PLNSSSTM)
SELECT CNT.SQ_PLNSCDGO.NEXTVAL, 'CRD PROVISION CARTERA', 37, 1, 1236,
       'Provision de cartera de inversiones privativas por calificacion (paso 8 del cierre, ajuste a saldo de 1.3.99 a la provision requerida de la G48 al corte, regla del SAA, D-015). Lineas por tipo de prestamo (DTPLAXL2 = TPPRCDGO); papeles 82 gasto, 83 provision, 84 reverso de CrdLineaAsiento. Creada 2026-10-28, sql/332 (C15).',
       1
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 37);
-- (!) "1 fila insertada".

-- BLOQUE B2 — (ESCRIBE) las 9 líneas. Columnas como la plantilla 36 (sql/310).
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1, DTPLAXL2, DTPLAXL3, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, n.PLNNCDGO, v.DESCRIPCION, v.MOVIMIENTO, v.AUX1, v.AUX2, 0, 1
  FROM CNT.PLNS p
  JOIN (SELECT 82 AUX1, 1 AUX2, 1 MOVIMIENTO, '4.7.04.05' CUENTA, 'GASTO PROVISION CARTERA - QUIROGRAFARIO' DESCRIPCION FROM DUAL UNION ALL
        SELECT 82, 2, 1, '4.7.04.15', 'GASTO PROVISION CARTERA - HIPOTECARIO' FROM DUAL UNION ALL
        SELECT 82, 3, 1, '4.7.04.10', 'GASTO PROVISION CARTERA - PRENDARIO' FROM DUAL UNION ALL
        SELECT 83, 1, 2, '1.3.99.05', 'PROVISION CARTERA - QUIROGRAFARIO' FROM DUAL UNION ALL
        SELECT 83, 2, 2, '1.3.99.15', 'PROVISION CARTERA - HIPOTECARIO' FROM DUAL UNION ALL
        SELECT 83, 3, 2, '1.3.99.10', 'PROVISION CARTERA - PRENDARIO' FROM DUAL UNION ALL
        SELECT 84, 1, 2, '5.3.90.05', 'REVERSION DE PROVISIONES CARTERA - QUIROGRAFARIO' FROM DUAL UNION ALL
        SELECT 84, 2, 2, '5.3.90.05', 'REVERSION DE PROVISIONES CARTERA - HIPOTECARIO' FROM DUAL UNION ALL
        SELECT 84, 3, 2, '5.3.90.05', 'REVERSION DE PROVISIONES CARTERA - PRENDARIO' FROM DUAL) v ON 1 = 1
  JOIN CNT.PLNN n ON n.PJRQCDGO = p.PJRQCDGO AND n.PLNNCNTA = v.CUENTA
 WHERE p.PJRQCDGO = 1236 AND p.PLNSCDAL = 37
   AND NOT EXISTS (SELECT 1 FROM CNT.DTPL d WHERE d.PLNSCDGO = p.PLNSCDGO AND d.DTPLAXL1 = v.AUX1 AND d.DTPLAXL2 = v.AUX2);
-- (!) "9 filas insertadas". Si no: ROLLBACK y avisar.
COMMIT;


-- BLOQUE C — Verificación: 9 filas, 82 DEBE (1), 83 y 84 HABER (2).
SELECT p.PLNSCDAL AS ALTERNO, d.DTPLAXL1 AS AUX1, d.DTPLAXL2 AS AUX2, d.DTPLMVMN AS MOVIMIENTO,
       n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS NOMBRE
  FROM CNT.PLNS p JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PJRQCDGO = 1236 AND p.PLNSCDAL = 37
 ORDER BY d.DTPLAXL1, d.DTPLAXL2;


-- REVERSO (solo si hay que deshacer, y sin corridas que usen 8/9):
-- DELETE FROM CNT.DTPL WHERE PLNSCDGO = (SELECT PLNSCDGO FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 37);
-- DELETE FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 37;
-- ALTER TABLE CRD.ANCC DROP CONSTRAINT CK_ANCC_TPOO;
-- ALTER TABLE CRD.ANCC ADD CONSTRAINT CK_ANCC_TPOO CHECK (ANCCTPOO IN (1, 2, 3, 4, 5, 6, 7));
-- COMMIT;

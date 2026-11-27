-- =====================================================================================
-- 415 — C52 (D-150 / D-222): plantilla alterno 40 «CNT REGULARIZACION SALDOS CONTRARIOS» (semilla; las cuentas por plantilla, nunca en el código).
-- Equipo omen-saa-1 · 2026-11-28 · REVISABLE: NO está ejecutado en ninguna base. Va ANTES del EAR que lleve el C52.
--
-- Las usa SaldoContrarioCuentaServiceImpl (cnt): POST /prdo/regularizarRedondeos (papeles 1 y 2) y el asiento SUGERIDO del control SALDO_CONTRARIO_CUENTA
-- (papeles 3 y 4). Líneas (DTPLAXL1 = papel, DTPLAXL2 = orden dentro del papel; DTPLMVMN 1 = DEBE, 2 = HABER):
--   papel 1 (DEBE),  aux2 1: 4.8.99.08     GND POR AJUSTES            cuenta de redondeo con saldo acreedor
--   papel 1 (DEBE),  aux2 2: 4.8.90.90.35  cuenta de redondeo         cuenta de redondeo con saldo acreedor
--   papel 2 (HABER), aux2 0: 5.3.90.90     OTROS INGRESOS             destino de los redondeos (regla del líder del cuadre, 26-nov)
--   papel 3 (DEBE),  aux2 0: 1.4.90.90.05  contrapartida sugerida de un pasivo/patrimonio con saldo deudor
--   papel 4 (HABER), aux2 0: 2.3.90.90.09  contrapartida sugerida de un activo con saldo acreedor (D-066)
-- Las cuentas se buscan por su número sin puntos y TIENEN que existir una sola vez en CNT.PLNN del proyecto 1236: si no, el script se detiene.
--
-- ⛔ Lección del 38 (ver 383): este script SE DETIENE (ORA-20003) si ya existe CUALQUIER plantilla con alterno 40 en la empresa, se llame como se llame, e inserta
--    las líneas SOLO en la plantilla que él mismo crea (alterno 40 Y nombre «CNT REGULARIZACION SALDOS CONTRARIOS»). El alterno 40 está reservado en
--    docs/logica-negocio/REGISTRO-RESERVAS-EQUIPOS.md §2c.
-- Antes de correr: backup de CNT.PLNS y CNT.DTPL. Correr bloque por bloque.
-- =====================================================================================

-- BLOQUE 0 — Controles previos (solo lectura).
-- 0a. El alterno 40 tiene que estar LIBRE. Esperado: 0 filas.
SELECT PLNSCDGO, PLNSNMBR, PLNSCDAL FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 40;
-- 0b. Las cinco cuentas, cada una UNA vez y de movimiento (PLNNTPOO = 2). Esperado: 5 filas.
SELECT PLNNCDGO, PLNNCNTA, PLNNNMBR, PLNNTPOO FROM CNT.PLNN
 WHERE PJRQCDGO = 1236 AND REPLACE(PLNNCNTA, '.', '') IN ('489908', '48909035', '539090', '14909005', '23909009')
 ORDER BY PLNNCNTA;

-- BLOQUE G — GUARDA. Se detiene si el alterno 40 ya existe o si alguna de las cinco cuentas no existe una sola vez.
DECLARE
    v_existe  NUMBER;
    v_cuentas NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_existe FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 40;
    IF v_existe > 0 THEN
        RAISE_APPLICATION_ERROR(-20003, 'El alterno 40 ya existe en la empresa 1236 (' || v_existe || ' plantilla/s): este script no escribe sobre una plantilla ajena. Revise CNT.PLNS y REGISTRO-RESERVAS-EQUIPOS.');
    END IF;
    SELECT COUNT(*) INTO v_cuentas FROM CNT.PLNN
     WHERE PJRQCDGO = 1236 AND REPLACE(PLNNCNTA, '.', '') IN ('489908', '48909035', '539090', '14909005', '23909009');
    IF v_cuentas <> 5 THEN
        RAISE_APPLICATION_ERROR(-20002, 'Se esperaban 5 cuentas distintas (4.8.99.08, 4.8.90.90.35, 5.3.90.90, 1.4.90.90.05, 2.3.90.90.09), cada una una vez en CNT.PLNN del proyecto 1236; hay ' || v_cuentas || '.');
    END IF;
END;
/

-- BLOQUE B1 — (ESCRIBE) cabecera de la plantilla 40. Columnas como el 383.
INSERT INTO CNT.PLNS (PLNSCDGO, PLNSNMBR, PLNSCDAL, PLNSESTD, PJRQCDGO, PLNSOBSR, PLNSSSTM)
SELECT CNT.SQ_PLNSCDGO.NEXTVAL, 'CNT REGULARIZACION SALDOS CONTRARIOS', 40, 1, 1236,
       'Regularizacion de saldos contrarios (C52, D-150/D-222): papel 1 redondeos con saldo acreedor (DEBE) a otros ingresos (papel 2, HABER); papeles 3 y 4 = contrapartida del asiento sugerido para pasivo deudor / activo acreedor.',
       1
  FROM DUAL;
SELECT PLNSCDGO, PLNSNMBR, PLNSCDAL FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 40;

-- BLOQUE B2 — (ESCRIBE) las cinco líneas, SOLO en la plantilla que crea este script.
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1, DTPLAXL2, DTPLAXL3, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, n.PLNNCDGO, x.DESCRIPCION, x.MOVIMIENTO, x.PAPEL, x.ORDEN, 0, 1
  FROM CNT.PLNS p
  JOIN (SELECT '489908' AS CUENTA, 'REDONDEO CON SALDO ACREEDOR (ORIGEN)' AS DESCRIPCION, 1 AS MOVIMIENTO, 1 AS PAPEL, 1 AS ORDEN FROM DUAL
        UNION ALL SELECT '48909035', 'REDONDEO CON SALDO ACREEDOR (ORIGEN)', 1, 1, 2 FROM DUAL
        UNION ALL SELECT '539090', 'OTROS INGRESOS (DESTINO DE LOS REDONDEOS)', 2, 2, 0 FROM DUAL
        UNION ALL SELECT '14909005', 'CONTRAPARTIDA SUGERIDA DE UN PASIVO CON SALDO DEUDOR', 1, 3, 0 FROM DUAL
        UNION ALL SELECT '23909009', 'CONTRAPARTIDA SUGERIDA DE UN ACTIVO CON SALDO ACREEDOR', 2, 4, 0 FROM DUAL) x ON 1 = 1
  JOIN CNT.PLNN n ON n.PJRQCDGO = p.PJRQCDGO AND REPLACE(n.PLNNCNTA, '.', '') = x.CUENTA
 WHERE p.PJRQCDGO = 1236 AND p.PLNSCDAL = 40 AND p.PLNSNMBR = 'CNT REGULARIZACION SALDOS CONTRARIOS'
   AND NOT EXISTS (SELECT 1 FROM CNT.DTPL y WHERE y.PLNSCDGO = p.PLNSCDGO AND y.DTPLAXL1 = x.PAPEL AND y.DTPLAXL2 = x.ORDEN);
-- (!) "5 filas insertadas". Si no son 5: ROLLBACK y avisar.
COMMIT;

-- BLOQUE C — Verificación. Esperado: 5 filas; papel 1 aux2 1 y 2 DEBE; papel 2 HABER; papel 3 DEBE; papel 4 HABER.
SELECT p.PLNSCDAL AS ALTERNO, d.DTPLAXL1 AS PAPEL, d.DTPLAXL2 AS ORDEN, d.DTPLMVMN AS MOVIMIENTO, n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS NOMBRE
  FROM CNT.PLNS p JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PJRQCDGO = 1236 AND p.PLNSCDAL = 40
 ORDER BY d.DTPLAXL1, d.DTPLAXL2;

-- =====================================================================================
-- REVERSO (solo si nada usó la plantilla; el asiento de regularización de un mes ya asentado se anula con AsientoService.reversionAsiento, no por SQL):
-- DELETE FROM CNT.DTPL WHERE PLNSCDGO = (SELECT PLNSCDGO FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 40 AND PLNSNMBR = 'CNT REGULARIZACION SALDOS CONTRARIOS');
-- DELETE FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 40 AND PLNSNMBR = 'CNT REGULARIZACION SALDOS CONTRARIOS';
-- COMMIT;
-- =====================================================================================

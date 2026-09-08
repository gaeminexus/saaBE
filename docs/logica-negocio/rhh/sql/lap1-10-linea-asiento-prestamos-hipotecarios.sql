-- =====================================================================
-- lap1-10  ·  Separar la cuenta por pagar de los prestamos hipotecarios
-- Equipo lap-saa-1 · 2026-09-07
-- =====================================================================
--
-- POR QUE
-- Decision del usuario del 2026-09-07: los prestamos hipotecarios y los
-- quirografarios del IESS van a CUENTAS DIFERENTES. Hoy comparten una sola
-- linea de asiento, la 12 (IESS_POR_PAGAR_PRESTAMOS), asi que sus dos
-- planillas debitan el mismo saldo y una diferencia en una se compensa con
-- la otra sin que nadie la vea.
--
-- El motor de nomina YA los distingue como conceptos
-- (RhhRolConceptoMotor.PRESTAMO_QUIROGRAFARIO 12 / PRESTAMO_HIPOTECARIO 13);
-- lo unico que los junta es lineaDeDescuento en la contabilizacion.
--
-- ORDEN: este script va ANTES de lap1-09 (que necesita la cuenta nueva para
-- crear el grupo de IESS-PRSH) y ANTES del WAR con la linea nueva.
--
-- =====================================================================
-- ⛔⛔ ADVERTENCIA QUE HAY QUE LEER ENTERA ANTES DE CORRER ESTO
-- =====================================================================
--
-- 1. ESTO CAMBIA EL ASIENTO MENSUAL DE NOMINA. El modulo RRHH esta en
--    calibracion y hay meses CERRADOS en produccion con diferencia cero.
--    Aplicar desde un periodo NUEVO. NUNCA reprocesar un mes ya cerrado
--    con la linea nueva: dejaria de cuadrar contra lo que ya se declaro.
--
-- 2. LA PROVISION HISTORICA NO SE REPARTE SOLA. Todo lo acumulado hasta
--    hoy en la cuenta de la linea 12 es de los dos prestamos mezclados.
--    Separar de aqui en adelante NO separa el saldo anterior: ese saldo se
--    arrastra hasta consumirse o hasta que alguien lo reclasifique a mano.
--    Es una decision de la contadora, no algo que el sistema deba adivinar.
--
-- 3. LA CUENTA CONTABLE NUEVA LA CREA LA CONTADORA, no este script. Aca
--    solo se parametriza la linea de asiento contra una cuenta que ya
--    exista en CNT.PLNN.
-- =====================================================================


-- ---------------------------------------------------------------------
-- CONTROL 1 — como esta hoy: que cuenta usa la linea 12 y en que
--             plantillas aparece.
-- ---------------------------------------------------------------------
SELECT d.PLNSCDGO   AS ID_PLANTILLA,
       s.PLNSNMBR   AS PLANTILLA,
       d.DTPLLNEA   AS LINEA,
       p.PLNNCDGO   AS ID_CUENTA,
       p.PLNNCNTA   AS CUENTA,
       p.PLNNNMBR   AS NOMBRE_CUENTA
  FROM CNT.DTPL d
  JOIN CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
  JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO
 WHERE d.DTPLLNEA = 12
 ORDER BY d.PLNSCDGO;

-- ⚠️ Los nombres de columna de CNT.DTPL / CNT.PLNS no se verificaron contra
--    el DDL real. Si la consulta falla, sacar la lista con:
--      SELECT COLUMN_NAME, DATA_TYPE FROM ALL_TAB_COLUMNS
--       WHERE OWNER='CNT' AND TABLE_NAME IN ('DTPL','PLNS')
--       ORDER BY TABLE_NAME, COLUMN_ID;
--    y ajustar. NO adivinar.


-- ---------------------------------------------------------------------
-- CONTROL 2 — la cuenta destino de los hipotecarios debe EXISTIR ya.
--             Ajustar el filtro al nombre real que use el plan.
-- ---------------------------------------------------------------------
SELECT PLNNCDGO, PLNNCNTA, PLNNNMBR
  FROM CNT.PLNN
 WHERE UPPER(PLNNNMBR) LIKE '%HIPOTEC%'
    OR UPPER(PLNNNMBR) LIKE '%PRESTAMO%IESS%'
 ORDER BY PLNNCNTA;


-- ---------------------------------------------------------------------
-- CONTROL 3 — cuanto saldo hay hoy mezclado en la cuenta de la linea 12.
--             Es el numero del que habla la advertencia 2: no se reparte
--             solo. Reemplazar <ID_CUENTA_LINEA_12> con lo del CONTROL 1.
-- ---------------------------------------------------------------------
-- SELECT SUM(NVL(dt.DTASDEBE,0)) AS TOTAL_DEBE,
--        SUM(NVL(dt.DTASHABR,0)) AS TOTAL_HABER,
--        SUM(NVL(dt.DTASHABR,0)) - SUM(NVL(dt.DTASDEBE,0)) AS SALDO_ACREEDOR
--   FROM CNT.DTAS dt
--  WHERE dt.PLNNCDGO = <ID_CUENTA_LINEA_12>;
--
-- ⚠️ Nombres de CNT.DTAS sin verificar: confirmarlos antes, igual que arriba.


-- =====================================================================
-- CORRECCION — parametrizar la linea nueva. COMENTADA A PROPOSITO.
-- =====================================================================
-- La linea 19 es el numero libre siguiente en RhhLineaAsiento (usados: 1-7,
-- 10-18, 30-35, 40-45, 50, 51, 60-63, 70). El WAR con
-- IESS_POR_PAGAR_PRESTAMOS_HIPOTECARIOS = 19 tiene que estar desplegado
-- para que la nomina la use, pero la fila de parametrizacion puede existir
-- antes sin romper nada: una linea que nadie pide no se lee.
--
-- Completar <ID_PLANTILLA> con la del CONTROL 1 (la misma donde vive hoy la
-- linea 12) y <ID_CUENTA_HIPOTECARIOS> con la del CONTROL 2.
--
-- INSERT INTO CNT.DTPL (PLNSCDGO, DTPLLNEA, PLNNCDGO)
-- VALUES (<ID_PLANTILLA>, 19, <ID_CUENTA_HIPOTECARIOS>);
--
-- COMMIT;
--
-- ⚠️ VERIFICAR las columnas NOT NULL de CNT.DTPL antes de descomentar: las
--    de arriba son las minimas y esta tabla puede tener mas (naturaleza,
--    orden, estado). Sacar la lista con ALL_TAB_COLUMNS y completarlas.


-- ---------------------------------------------------------------------
-- CONTROL 4 — despues: las lineas 12 y 19 conviven, con cuentas DISTINTAS.
--             Si las dos apuntan a la misma cuenta, la separacion no
--             sirve de nada y hay que corregir antes de seguir.
-- ---------------------------------------------------------------------
SELECT d.DTPLLNEA AS LINEA,
       CASE d.DTPLLNEA WHEN 12 THEN 'QUIROGRAFARIOS' WHEN 19 THEN 'HIPOTECARIOS' END AS CONCEPTO,
       p.PLNNCNTA AS CUENTA,
       p.PLNNNMBR AS NOMBRE_CUENTA
  FROM CNT.DTPL d
  JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO
 WHERE d.DTPLLNEA IN (12, 19)
 ORDER BY d.DTPLLNEA;


-- =====================================================================
-- REVERSO (comentado a proposito — no correr salvo que haga falta)
-- =====================================================================
-- Solo si el WAR con la linea 19 NO esta desplegado y ninguna nomina se
-- calculo con ella. Si ya se calculo un periodo, borrar la parametrizacion
-- deja ese asiento sin cuenta para el descuento hipotecario.
--
-- DELETE FROM CNT.DTPL WHERE DTPLLNEA = 19;
-- COMMIT;

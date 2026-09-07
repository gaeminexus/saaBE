-- =====================================================================
-- lap1-09  ·  Parametrizacion contable del pago de las planillas del IESS
-- Equipo lap-saa-1 · 2026-09-07
-- =====================================================================
--
-- QUE HACE Y POR QUE
-- El pago de la planilla del IESS va por el circuito de pagos de tesoreria
-- (decision del usuario del 2026-09-07: todo pago por TSR, para poder
-- conciliar contra el banco). Ese circuito contabiliza el pago armando una
-- linea DEBE por cada fila del desglose PGS.DPGT, y toma la cuenta contable
-- del GRUPO del producto de esa fila, no del producto:
--     PagoProgramadoServiceImpl.cuentaDelProducto:3066-3080
--         -> producto.getGrupoProducto().getPlanCuenta()
--
-- Asi que hacen falta grupos de producto que representen las cuentas por
-- pagar del IESS, y un producto por cada uno.
--
-- ⛔ LA CUENTA TIENE QUE SER LA MISMA QUE PROVISIONA LA NOMINA.
--    Si el pago debita una cuenta distinta de aquella donde la nomina
--    acumulo el pasivo, el pasivo NO SE SALDA NUNCA y nadie lo ve hasta un
--    cierre. Por eso este script NO teclea codigos de cuenta: los LEE de las
--    plantillas contables de RRHH, que es donde ya estan configurados.
--
-- VA DESPUES de lap1-08 y ANTES del WAR de la Fase 2.
-- Diseno: docs/logica-negocio/rhh/API-PLANILLA-IESS.md §6
-- =====================================================================


-- ---------------------------------------------------------------------
-- CONTROL 1 — QUE CUENTAS USA HOY LA NOMINA. Es el insumo de todo el
--             script: si alguna linea no aparece aca, PARAR y avisar.
--             Lineas de RhhLineaAsiento: 10 aporte personal, 11 patronal,
--             12 prestamos IESS, 16 fondos de reserva por pagar.
-- ---------------------------------------------------------------------
SELECT d.DTPLLNEA        AS LINEA,
       CASE d.DTPLLNEA
            WHEN 10 THEN 'IESS POR PAGAR APORTE PERSONAL'
            WHEN 11 THEN 'IESS POR PAGAR APORTE PATRONAL'
            WHEN 12 THEN 'IESS POR PAGAR PRESTAMOS'
            WHEN 16 THEN 'FONDOS DE RESERVA POR PAGAR'
            ELSE 'OTRA' END AS CONCEPTO,
       p.PLNNCDGO        AS ID_CUENTA,
       p.PLNNCNTA        AS CUENTA_CONTABLE,
       p.PLNNNMBR        AS NOMBRE_CUENTA
  FROM CNT.DTPL d
  JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO
 WHERE d.DTPLLNEA IN (10, 11, 12, 16)
 ORDER BY d.DTPLLNEA;

-- ⚠️ VERIFICAR los nombres de columna de CNT.DTPL antes de correr lo de
--    arriba: DTPLLNEA es el numero de linea de RhhLineaAsiento segun el uso
--    que hace ContabilizacionNominaServiceImpl, pero NO se verifico contra
--    el DDL real de la tabla. Si la consulta falla por columna inexistente,
--    sacar la lista con:
--      SELECT COLUMN_NAME, DATA_TYPE FROM ALL_TAB_COLUMNS
--       WHERE OWNER='CNT' AND TABLE_NAME='DTPL' ORDER BY COLUMN_ID;
--    y ajustar. NO adivinar el nombre.


-- ---------------------------------------------------------------------
-- CONTROL 2 — no deben existir ya estos grupos (0 filas esperadas).
-- ---------------------------------------------------------------------
SELECT GRPPCDGO, GRPPNMBR, PLNNCDGO
  FROM PGS.GRPP
 WHERE UPPER(GRPPNMBR) LIKE '%IESS%'
    OR UPPER(GRPPNMBR) LIKE '%FONDO%RESERVA%';


-- ---------------------------------------------------------------------
-- CONTROL 3 — foto de los MAX antes de insertar.
-- ---------------------------------------------------------------------
SELECT (SELECT MAX(GRPPCDGO) FROM PGS.GRPP) AS MAX_GRUPO,
       (SELECT MAX(ID)       FROM PGS.PRDP) AS MAX_PRODUCTO
  FROM DUAL;


-- =====================================================================
-- CORRECCION — grupos y productos
-- =====================================================================
--
-- ⛔ ESTE BLOQUE ESTA COMENTADO A PROPOSITO Y NO SE PUEDE CORRER DE
--    CORRIDO. Depende de dos cosas que solo se saben al correr el
--    CONTROL 1:
--      1. El ID_CUENTA real de cada linea (PLNNCDGO), que cambia por
--         instalacion — este es un producto multicliente, no la base de
--         un solo cliente.
--      2. Si quirografarios e hipotecarios comparten cuenta o no. Hoy
--         comparten la linea 12; si la contadora confirma que son dos
--         cuentas distintas, hay que separar la linea de asiento primero
--         (§2.1 del diseno) y este script cambia.
--
--    Completar los <ID_CUENTA_*> con lo que devolvio el CONTROL 1 y
--    descomentar. Si algun ID_CUENTA quedara vacio, NO inventar uno:
--    un grupo sin cuenta correcta hace que el pago debite el pasivo
--    equivocado, y eso no se ve hasta que alguien cuadre el mes.
--
-- INSERT INTO PGS.GRPP (GRPPNMBR, PLNNCDGO, PJRQCDGO, GRPPESTD)
-- VALUES ('IESS APORTE PERSONAL POR PAGAR', <ID_CUENTA_LINEA_10>, <ID_EMPRESA>, 1);
--
-- INSERT INTO PGS.GRPP (GRPPNMBR, PLNNCDGO, PJRQCDGO, GRPPESTD)
-- VALUES ('IESS APORTE PATRONAL POR PAGAR', <ID_CUENTA_LINEA_11>, <ID_EMPRESA>, 1);
--
-- INSERT INTO PGS.GRPP (GRPPNMBR, PLNNCDGO, PJRQCDGO, GRPPESTD)
-- VALUES ('IESS PRESTAMOS POR PAGAR', <ID_CUENTA_LINEA_12>, <ID_EMPRESA>, 1);
--
-- INSERT INTO PGS.GRPP (GRPPNMBR, PLNNCDGO, PJRQCDGO, GRPPESTD)
-- VALUES ('IESS FONDOS DE RESERVA POR PAGAR', <ID_CUENTA_LINEA_16>, <ID_EMPRESA>, 1);
--
-- COMMIT;
--
-- -- Un producto por grupo. El desglose del pago (PGS.DPGT) apunta al
-- -- PRODUCTO; la cuenta la pone su grupo.
-- INSERT INTO PGS.PRDP (EMPRESA, GRUPOPRODUCTO, NOMBRE, CODIGO, ESTADO)
-- SELECT <ID_EMPRESA>, GRPPCDGO, 'IESS - Aporte personal', 'IESS-APER', 1
--   FROM PGS.GRPP WHERE GRPPNMBR = 'IESS APORTE PERSONAL POR PAGAR';
--
-- INSERT INTO PGS.PRDP (EMPRESA, GRUPOPRODUCTO, NOMBRE, CODIGO, ESTADO)
-- SELECT <ID_EMPRESA>, GRPPCDGO, 'IESS - Aporte patronal', 'IESS-APAT', 1
--   FROM PGS.GRPP WHERE GRPPNMBR = 'IESS APORTE PATRONAL POR PAGAR';
--
-- INSERT INTO PGS.PRDP (EMPRESA, GRUPOPRODUCTO, NOMBRE, CODIGO, ESTADO)
-- SELECT <ID_EMPRESA>, GRPPCDGO, 'IESS - Prestamos', 'IESS-PRST', 1
--   FROM PGS.GRPP WHERE GRPPNMBR = 'IESS PRESTAMOS POR PAGAR';
--
-- INSERT INTO PGS.PRDP (EMPRESA, GRUPOPRODUCTO, NOMBRE, CODIGO, ESTADO)
-- SELECT <ID_EMPRESA>, GRPPCDGO, 'IESS - Fondos de reserva', 'IESS-FRES', 1
--   FROM PGS.GRPP WHERE GRPPNMBR = 'IESS FONDOS DE RESERVA POR PAGAR';
--
-- COMMIT;
--
-- ⚠️ VERIFICAR las columnas NOT NULL de PGS.GRPP y PGS.PRDP antes de
--    descomentar; las de arriba son las que usa el resto del sistema, pero
--    si esta instalacion tiene mas obligatorias el INSERT falla:
--      SELECT COLUMN_NAME, NULLABLE, DATA_TYPE FROM ALL_TAB_COLUMNS
--       WHERE OWNER='PGS' AND TABLE_NAME IN ('GRPP','PRDP')
--       ORDER BY TABLE_NAME, COLUMN_ID;


-- ---------------------------------------------------------------------
-- CONTROL 4 — despues: los cuatro grupos con su cuenta, y sus productos.
-- ---------------------------------------------------------------------
SELECT g.GRPPCDGO AS ID_GRUPO,
       g.GRPPNMBR AS GRUPO,
       c.PLNNCNTA AS CUENTA_CONTABLE,
       c.PLNNNMBR AS NOMBRE_CUENTA,
       p.ID       AS ID_PRODUCTO,
       p.NOMBRE   AS PRODUCTO
  FROM PGS.GRPP g
  LEFT JOIN CNT.PLNN c ON c.PLNNCDGO = g.PLNNCDGO
  LEFT JOIN PGS.PRDP p ON p.GRUPOPRODUCTO = g.GRPPCDGO
 WHERE UPPER(g.GRPPNMBR) LIKE 'IESS%'
 ORDER BY g.GRPPCDGO;


-- ---------------------------------------------------------------------
-- CONTROL 5 — el que de verdad importa: que la cuenta del grupo sea la
--             MISMA que la de la plantilla de nomina. Toda fila debe
--             decir COINCIDE. Una que diga NO COINCIDE significa que el
--             pago va a debitar un pasivo distinto del que se provisiono.
-- ---------------------------------------------------------------------
SELECT g.GRPPNMBR AS GRUPO,
       g.PLNNCDGO AS CUENTA_DEL_GRUPO,
       d.PLNNCDGO AS CUENTA_DE_LA_PLANTILLA,
       CASE WHEN g.PLNNCDGO = d.PLNNCDGO THEN 'COINCIDE' ELSE 'NO COINCIDE' END AS DIAGNOSTICO
  FROM PGS.GRPP g
  JOIN CNT.DTPL d
    ON d.PLNNCDGO = g.PLNNCDGO
    OR d.DTPLLNEA IN (10, 11, 12, 16)
 WHERE UPPER(g.GRPPNMBR) LIKE 'IESS%'
 ORDER BY g.GRPPNMBR;


-- =====================================================================
-- REVERSO (comentado a proposito — no correr salvo que haga falta)
-- =====================================================================
-- Solo si todavia no se registro ningun pago de planilla con estos
-- productos: borrarlos con un DPGT apuntando a ellos rompe el desglose de
-- un pago ya contabilizado.
--
-- DELETE FROM PGS.PRDP WHERE CODIGO IN ('IESS-APER','IESS-APAT','IESS-PRST','IESS-FRES');
-- DELETE FROM PGS.GRPP WHERE UPPER(GRPPNMBR) LIKE 'IESS%';
-- COMMIT;

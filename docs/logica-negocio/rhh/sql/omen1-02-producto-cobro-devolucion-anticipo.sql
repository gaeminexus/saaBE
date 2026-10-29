-- =====================================================================================
-- omen1-02 — Producto de cobro RHH-DVAN para "Registrar devolución de anticipo" (RRHH)
-- Equipo omen-saa-1, 2026-10-29. REEMPLAZA los BLOQUES 2 en adelante de
-- rhh/sql/lap1-11-devolucion-anticipo.sql (el BLOQUE 1, las tablas RHH.DVAN y RHH.DVCT, se
-- verifica abajo en el CONTROL 1).
--
-- Error en pantalla: "No existe el producto de cobro 'RHH-DVAN' para la empresa 1236".
--
-- Por qué el lap1-11 no sirve, verificado contra el código:
--   1. CNT.DTPL no tiene DTPLLNEA (ORA-00904). La "línea 14" (Cuentas por Cobrar Empleados) es
--      DTPLAXL1 = 14 dentro de la PLANTILLA DE ROL de la empresa: CNT.PLNS con
--      PLNSCDAL = RHH.CFNM.CFNMPLRL y PJRQCDGO = empresa, detalle activo (DTPLESTD = 1).
--      Es lo que hace AsientoContableServiceImpl.obtenerCuentaCuentasPorCobrarEmpleados.
--   2. El lap1-11 creaba el producto en PGS.PRDP / PGS.GRPP (productos de PAGO). El código
--      busca un ProductoCobro = CBR.PRDC, y la cuenta sale de su grupo CBR.GRPC.PLNNCDGO
--      (DevolucionAnticipoServiceImpl.buscaProductoCobro → IngresoServiceImpl.procesarIngreso
--      → generarAsientoIngresoTesoreria). Un producto en PGS.PRDP no lo encuentra nunca.
--
-- Cómo correrlo: de corrido. Los CONTROLES son solo lectura; el BLOQUE 2 es un PL/SQL que se
-- detiene con un error claro si falta algo, no duplica si se corre dos veces y hace COMMIT
-- solo si todo salió bien. Pasar al árbitro las salidas de los CONTROLES 1, 2, 3 y 6.
-- =====================================================================================

-- ---------------------------------------------------------------------
-- CONTROL 1 — ¿quedaron creadas las tablas del BLOQUE 1 del lap1-11?
--             Esperado: DVAN y DVCT. Si falta alguna, AVISAR antes de seguir.
-- ---------------------------------------------------------------------
SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER = 'RHH' AND TABLE_NAME IN ('DVAN', 'DVCT');

-- ---------------------------------------------------------------------
-- CONTROL 2 — la cuenta de la línea 14 en la plantilla de rol de la empresa 1236.
--             Esperado: UNA fila, con una cuenta real (no la marcadora).
-- ---------------------------------------------------------------------
SELECT c.PJRQCDGO AS EMPRESA, c.CFNMPLRL AS ALTERNO_PLANTILLA_ROL, s.PLNSCDGO,
       d.DTPLCDGO, d.DTPLDSCR, d.DTPLAXL1, d.DTPLESTD,
       p.PLNNCDGO AS ID_CUENTA, p.PLNNCNTA AS CUENTA, p.PLNNNMBR AS NOMBRE_CUENTA,
       c.CFNMCTMR AS CUENTA_MARCADORA,
       CASE WHEN d.PLNNCDGO = c.CFNMCTMR THEN '⛔ ES LA MARCADORA' ELSE 'OK' END AS DIAGNOSTICO
  FROM RHH.CFNM c
  JOIN CNT.PLNS s ON s.PLNSCDAL = c.CFNMPLRL AND s.PJRQCDGO = c.PJRQCDGO
  JOIN CNT.DTPL d ON d.PLNSCDGO = s.PLNSCDGO AND d.DTPLAXL1 = 14 AND d.DTPLESTD = 1
  LEFT JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO
 WHERE c.PJRQCDGO = 1236;

-- ---------------------------------------------------------------------
-- CONTROL 3 — lo que ya existe en CBR para la empresa 1236:
--             ¿hay ya un grupo de cobro con esa cuenta? ¿ya hay un RHH-DVAN?
-- ---------------------------------------------------------------------
SELECT g.GRPCCDGO, g.GRPCNMBR, g.GRPCRYYA, g.GRPCRZZA, g.PLNNCDGO, n.PLNNCNTA, g.GRPCESTD
  FROM CBR.GRPC g
  LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
 WHERE g.PJRQCDGO = 1236
 ORDER BY g.GRPCCDGO;

SELECT p.ID, p.CODIGO, p.NOMBRE, p.GRUPOPRODUCTO, p.ESTADO
  FROM CBR.PRDC p
 WHERE p.EMPRESA = 1236 AND p.CODIGO = 'RHH-DVAN';

-- ---------------------------------------------------------------------
-- CONTROL 4 — columnas obligatorias de CBR.GRPC y CBR.PRDC. Si el BLOQUE 2 falla por
--             "cannot insert NULL", pasar esta salida.
-- ---------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, NULLABLE, DATA_DEFAULT
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'CBR' AND TABLE_NAME IN ('GRPC', 'PRDC') AND NULLABLE = 'N'
 ORDER BY TABLE_NAME, COLUMN_ID;

-- ---------------------------------------------------------------------
-- CONTROL 5 — la secuencia del grupo no debe estar por debajo del máximo.
--             Si SECUENCIA_ATRASADA = 'SI', AVISAR y no correr el BLOQUE 2.
-- ---------------------------------------------------------------------
SELECT s.LAST_NUMBER, (SELECT NVL(MAX(GRPCCDGO), 0) FROM CBR.GRPC) AS MAX_GRPC,
       CASE WHEN s.LAST_NUMBER <= (SELECT NVL(MAX(GRPCCDGO), 0) FROM CBR.GRPC)
            THEN 'SI' ELSE 'NO' END AS SECUENCIA_ATRASADA
  FROM ALL_SEQUENCES s
 WHERE s.SEQUENCE_OWNER = 'CBR' AND s.SEQUENCE_NAME = 'SQ_GRPCCDGO';


-- =====================================================================
-- BLOQUE 2 — grupo de cobro + producto RHH-DVAN (idempotente)
-- =====================================================================
-- - Cuenta: la de la línea 14 de la plantilla de rol (mismo cálculo que el CONTROL 2).
-- - Grupo: si la empresa ya tiene un grupo de cobro ACTIVO con esa cuenta, se reusa; si no,
--   se crea 'CUENTAS POR COBRAR EMPLEADOS' copiando el tipo de grupo (GRPCRYYA/GRPCRZZA,
--   rubro 74) del grupo activo más usado de la empresa.
-- - Producto: si ya existe RHH-DVAN para la empresa, no se toca.
-- ---------------------------------------------------------------------
DECLARE
  v_empresa    CONSTANT NUMBER := 1236;
  v_cuenta     NUMBER;
  v_marcadora  NUMBER;
  v_grupo      NUMBER;
  v_ryya       NUMBER;
  v_rzza       NUMBER;
  v_existe     NUMBER;
BEGIN
  SELECT d.PLNNCDGO, c.CFNMCTMR
    INTO v_cuenta, v_marcadora
    FROM RHH.CFNM c
    JOIN CNT.PLNS s ON s.PLNSCDAL = c.CFNMPLRL AND s.PJRQCDGO = c.PJRQCDGO
    JOIN CNT.DTPL d ON d.PLNSCDGO = s.PLNSCDGO AND d.DTPLAXL1 = 14 AND d.DTPLESTD = 1
   WHERE c.PJRQCDGO = v_empresa;

  IF v_cuenta IS NULL OR v_cuenta = v_marcadora THEN
    RAISE_APPLICATION_ERROR(-20001, 'La linea 14 de la plantilla de rol no tiene cuenta real'
      || ' (o es la marcadora). Asignarla en Nomina > Plantillas contables y volver a correr.');
  END IF;

  SELECT COUNT(*) INTO v_existe FROM CBR.PRDC WHERE EMPRESA = v_empresa AND CODIGO = 'RHH-DVAN';
  IF v_existe > 0 THEN
    DBMS_OUTPUT.PUT_LINE('RHH-DVAN ya existe para la empresa ' || v_empresa || ': no se crea nada.');
    RETURN;
  END IF;

  BEGIN
    SELECT GRPCCDGO INTO v_grupo FROM (
      SELECT GRPCCDGO FROM CBR.GRPC
       WHERE PJRQCDGO = v_empresa AND PLNNCDGO = v_cuenta AND GRPCESTD = 1
       ORDER BY GRPCCDGO)
     WHERE ROWNUM = 1;
    DBMS_OUTPUT.PUT_LINE('Se reusa el grupo de cobro ' || v_grupo || ' (ya apunta a la cuenta).');
  EXCEPTION WHEN NO_DATA_FOUND THEN
    BEGIN
      SELECT GRPCRYYA, GRPCRZZA INTO v_ryya, v_rzza FROM (
        SELECT GRPCRYYA, GRPCRZZA, COUNT(*) n FROM CBR.GRPC
         WHERE PJRQCDGO = v_empresa AND GRPCESTD = 1
         GROUP BY GRPCRYYA, GRPCRZZA ORDER BY n DESC)
       WHERE ROWNUM = 1;
    EXCEPTION WHEN NO_DATA_FOUND THEN
      v_ryya := NULL; v_rzza := NULL;
    END;
    v_grupo := CBR.SQ_GRPCCDGO.NEXTVAL;
    INSERT INTO CBR.GRPC (GRPCCDGO, GRPCNMBR, GRPCRYYA, GRPCRZZA, PLNNCDGO, GRPCESTD, PJRQCDGO)
    VALUES (v_grupo, 'CUENTAS POR COBRAR EMPLEADOS', v_ryya, v_rzza, v_cuenta, 1, v_empresa);
    DBMS_OUTPUT.PUT_LINE('Grupo de cobro creado: ' || v_grupo);
  END;

  INSERT INTO CBR.PRDC (EMPRESA, GRUPOPRODUCTO, NOMBRE, CODIGO, DESCRIPCION, ESTADO)
  VALUES (v_empresa, v_grupo, 'Devolucion de anticipo a empleado', 'RHH-DVAN',
          'Lo usa RRHH > Registrar devolucion de anticipo (ingreso de tesoreria)', 1);
  DBMS_OUTPUT.PUT_LINE('Producto RHH-DVAN creado en el grupo ' || v_grupo);

  COMMIT;
EXCEPTION
  WHEN NO_DATA_FOUND THEN
    ROLLBACK;
    RAISE_APPLICATION_ERROR(-20002, 'No se encontro la configuracion de nomina, la plantilla de rol'
      || ' o su linea 14 para la empresa 1236: ver el CONTROL 2.');
  WHEN TOO_MANY_ROWS THEN
    ROLLBACK;
    RAISE_APPLICATION_ERROR(-20003, 'Hay mas de una linea 14 activa en la plantilla de rol: ver el'
      || ' CONTROL 2 y avisar.');
  WHEN OTHERS THEN
    ROLLBACK;
    RAISE;
END;
/

-- ---------------------------------------------------------------------
-- CONTROL 6 — después: el producto existe y su cuenta es la MISMA de la línea 14.
--             Esperado: una fila con COINCIDE.
-- ---------------------------------------------------------------------
SELECT p.ID, p.CODIGO, g.GRPCCDGO, g.PLNNCDGO AS CUENTA_DEL_PRODUCTO,
       d.PLNNCDGO AS CUENTA_LINEA_14,
       CASE WHEN g.PLNNCDGO = d.PLNNCDGO THEN 'COINCIDE' ELSE 'NO COINCIDE' END AS DIAGNOSTICO
  FROM CBR.PRDC p
  JOIN CBR.GRPC g ON g.GRPCCDGO = p.GRUPOPRODUCTO
  JOIN RHH.CFNM c ON c.PJRQCDGO = p.EMPRESA
  JOIN CNT.PLNS s ON s.PLNSCDAL = c.CFNMPLRL AND s.PJRQCDGO = c.PJRQCDGO
  JOIN CNT.DTPL d ON d.PLNSCDGO = s.PLNSCDGO AND d.DTPLAXL1 = 14 AND d.DTPLESTD = 1
 WHERE p.EMPRESA = 1236 AND p.CODIGO = 'RHH-DVAN';


-- =====================================================================
-- REVERSO (comentado a propósito). Solo si no se registró ninguna devolución
-- (SELECT COUNT(*) FROM RHH.DVAN debe dar 0 para este producto):
-- =====================================================================
-- DELETE FROM CBR.PRDC WHERE EMPRESA = 1236 AND CODIGO = 'RHH-DVAN';
-- DELETE FROM CBR.GRPC WHERE PJRQCDGO = 1236 AND GRPCNMBR = 'CUENTAS POR COBRAR EMPLEADOS'
--    AND NOT EXISTS (SELECT 1 FROM CBR.PRDC p WHERE p.GRUPOPRODUCTO = CBR.GRPC.GRPCCDGO);
-- COMMIT;

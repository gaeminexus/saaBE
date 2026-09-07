-- =====================================================================
-- lap1-06  ·  TSR.CJCH — columna de motivo de anulacion
-- Equipo lap-saa-1 · 2026-09-07
-- =====================================================================
--
-- VA ANTES DEL WAR. La entidad CajaChica mapea CJCHMTAN; si el WAR sube
-- primero, Hibernate nombra la columna en el SELECT de cualquier lectura
-- de la caja y toda pantalla de caja chica muere con ORA-00904.
--
-- Da de baja una caja chica se hace con CJCHESTD = 2 (INACTIVA), que ya
-- existe. Lo unico que falta es dejar registrado POR QUE se dio de baja:
-- mismo criterio y mismo descriptor que TSR.MVCH.MVCHMTAN.
-- =====================================================================


-- ---------------------------------------------------------------------
-- CONTROL 1 — la columna no debe existir todavia (0 filas esperadas).
-- ---------------------------------------------------------------------
SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'TSR' AND TABLE_NAME = 'CJCH' AND COLUMN_NAME = 'CJCHMTAN';


-- ---------------------------------------------------------------------
-- CONTROL 2 — distribucion de estados antes del cambio.
-- ---------------------------------------------------------------------
SELECT NVL(TO_CHAR(CJCHESTD), 'NULL') AS ESTADO,
       COUNT(*) AS CAJAS
  FROM TSR.CJCH
 GROUP BY CJCHESTD
 ORDER BY 1;


-- ---------------------------------------------------------------------
-- DDL
-- ---------------------------------------------------------------------
ALTER TABLE TSR.CJCH ADD CJCHMTAN VARCHAR2(500);

COMMENT ON COLUMN TSR.CJCH.CJCHMTAN IS 'Motivo de la baja de la caja (se llena al pasar CJCHESTD a 2)';


-- ---------------------------------------------------------------------
-- CONTROL 3 — despues: la columna existe, VARCHAR2(500), nullable.
-- ---------------------------------------------------------------------
SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'TSR' AND TABLE_NAME = 'CJCH' AND COLUMN_NAME = 'CJCHMTAN';


-- ---------------------------------------------------------------------
-- CONTROL 4 — ninguna caja activa debe tener motivo, y ninguna caja
--             dada de baja deberia quedar sin el (0 filas en ambas
--             columnas a partir de ahora; hoy las dos dan 0).
-- ---------------------------------------------------------------------
SELECT SUM(CASE WHEN CJCHESTD = 1 AND CJCHMTAN IS NOT NULL THEN 1 ELSE 0 END) AS ACTIVAS_CON_MOTIVO,
       SUM(CASE WHEN CJCHESTD = 2 AND CJCHMTAN IS NULL     THEN 1 ELSE 0 END) AS BAJAS_SIN_MOTIVO
  FROM TSR.CJCH;


-- =====================================================================
-- REVERSO (comentado a proposito — no correr salvo que haga falta)
-- =====================================================================
-- Solo tiene sentido si el WAR con la entidad nueva NO esta desplegado:
-- con el WAR arriba, quitar la columna rompe toda lectura de caja chica.
--
-- ALTER TABLE TSR.CJCH DROP COLUMN CJCHMTAN;

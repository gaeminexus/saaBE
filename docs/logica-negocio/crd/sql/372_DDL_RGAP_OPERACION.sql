-- =====================================================================================
-- 372 — DDL. omen-saa-1, 2026-11-08. Criterio «Operación» en las reglas de niveles de aprobación (CRD.RGAP).
-- Diseño: docs/logica-negocio/crd/DISENO-CRITERIO-OPERACION-NIVELES-APROBACION.md.
-- ⛔ Va ANTES del EAR que mapee RGAP.RGAPTPOP (sin la columna, toda lectura de reglas da ORA-00904).
--    Requiere el 370 (tabla CRD.RGAP). Aditivo: las reglas existentes quedan con NULL = cualquier operación.
-- Correr bloque por bloque, en orden.
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES
-- 0.1 La tabla existe (ESPERADO: 1 fila) y la columna NO (ESPERADO: 0 filas).
SELECT OWNER, TABLE_NAME FROM ALL_TABLES WHERE OWNER = 'CRD' AND TABLE_NAME = 'RGAP';
SELECT COLUMN_NAME FROM ALL_TAB_COLUMNS WHERE OWNER = 'CRD' AND TABLE_NAME = 'RGAP' AND COLUMN_NAME = 'RGAPTPOP';
-- 0.2 Reglas actuales (informativo: siguen igual, con operación = cualquiera).
SELECT RGAPCDGO, RGAPNMBR, TPPRCDGO, RGAPPRRD, RGAPESTD FROM CRD.RGAP ORDER BY RGAPCDGO;

-- BLOQUE 1 — RESPALDO (por si hubiera que volver; la tabla es chica)
CREATE TABLE CRD.RGAP_BK372 AS SELECT * FROM CRD.RGAP;

-- BLOQUE 2 — COLUMNA
ALTER TABLE CRD.RGAP ADD (RGAPTPOP NUMBER);
ALTER TABLE CRD.RGAP ADD CONSTRAINT CK_RGAP_OPERACION CHECK (RGAPTPOP IS NULL OR RGAPTPOP IN (1, 2, 3));
COMMENT ON COLUMN CRD.RGAP.RGAPTPOP IS 'Operacion: NULL cualquiera, 1 nuevo, 2 novacion, 3 reestructuracion (sql/372)';

-- BLOQUE 3 — CONTROL DESPUÉS
-- 3.1 Columna creada (ESPERADO: 1 fila, NUMBER, nullable Y).
SELECT COLUMN_NAME, DATA_TYPE, NULLABLE FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'CRD' AND TABLE_NAME = 'RGAP' AND COLUMN_NAME = 'RGAPTPOP';
-- 3.2 CHECK habilitado (ESPERADO: 1 fila ENABLED).
SELECT CONSTRAINT_NAME, STATUS FROM ALL_CONSTRAINTS WHERE OWNER = 'CRD' AND CONSTRAINT_NAME = 'CK_RGAP_OPERACION';
-- 3.3 Mismas filas que el respaldo y todas con NULL (ESPERADO: CON_OPERACION = 0, TOTAL = filas del 0.2).
SELECT COUNT(*) AS TOTAL, COUNT(RGAPTPOP) AS CON_OPERACION FROM CRD.RGAP;

-- BLOQUE 4 — Cuando el EAR esté estable y verificado:
-- DROP TABLE CRD.RGAP_BK372;

-- REVERSO (comentado; solo con un EAR que no mapee RGAPTPOP):
-- ALTER TABLE CRD.RGAP DROP CONSTRAINT CK_RGAP_OPERACION;
-- ALTER TABLE CRD.RGAP DROP COLUMN RGAPTPOP;

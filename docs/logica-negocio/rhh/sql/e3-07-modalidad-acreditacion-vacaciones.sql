-- =====================================================================================
-- e3-07 — Modalidad de acreditación de vacaciones (RHH.PRNM.PRNMMDVC)
-- Equipo omen-saa-3 · 2026-10-02 · Diseño: docs/logica-negocio/rhh/API-VACACIONES-MODALIDAD-ACREDITACION.md
--
-- 1 = por aniversario (lo de hoy) · 2 = devengo mensual (1,25 días por mes). ASOPREP: 2 desde 2026.
-- ⛔ VA ANTES DEL WAR que mapea ParametroNomina.modalidadVacaciones: con el WAR primero, toda
--    lectura de RHH.PRNM (el cálculo de la nómina entero) revienta con ORA-00904.
-- Correr de corrido. El BLOQUE 1 se detiene solo si la columna ya existe.
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES (se espera CERO filas: la columna no existe)
SELECT COLUMN_NAME FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'RHH' AND TABLE_NAME = 'PRNM' AND COLUMN_NAME = 'PRNMMDVC';

-- BLOQUE 1 — DDL y dato de ASOPREP
DECLARE
    v_existe NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_existe FROM ALL_TAB_COLUMNS
     WHERE OWNER = 'RHH' AND TABLE_NAME = 'PRNM' AND COLUMN_NAME = 'PRNMMDVC';
    IF v_existe > 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'RHH.PRNM.PRNMMDVC ya existe: no se hace nada.');
    END IF;

    EXECUTE IMMEDIATE 'ALTER TABLE RHH.PRNM ADD (PRNMMDVC NUMBER DEFAULT 1)';
    EXECUTE IMMEDIATE 'UPDATE RHH.PRNM SET PRNMMDVC = 1 WHERE PRNMMDVC IS NULL';
    EXECUTE IMMEDIATE 'ALTER TABLE RHH.PRNM ADD CONSTRAINT CK_PRNMMDVC CHECK (PRNMMDVC IN (1, 2))';
    EXECUTE IMMEDIATE q'[COMMENT ON COLUMN RHH.PRNM.PRNMMDVC IS 'Modalidad de acreditacion de vacaciones: 1 por aniversario, 2 devengo mensual (RhhModalidadVacaciones)']';

    UPDATE RHH.PRNM SET PRNMMDVC = 2 WHERE PJRQCDGO = 1236 AND PRNMANOO = 2026;
    IF SQL%ROWCOUNT <> 1 THEN
        RAISE_APPLICATION_ERROR(-20002, 'Se esperaba exactamente 1 fila de PRNM 2026 para la empresa 1236 y se tocaron '
            || SQL%ROWCOUNT || '. Nada se confirma.');
    END IF;
    COMMIT;
END;
/

-- BLOQUE 2 — CONTROL DESPUÉS (2025 -> 1, 2026 -> 2, y la restricción CK_PRNMMDVC)
SELECT p.PRNMANOO, p.PJRQCDGO, p.PRNMMDVC FROM RHH.PRNM p ORDER BY p.PJRQCDGO, p.PRNMANOO;
SELECT CONSTRAINT_NAME, SEARCH_CONDITION_VC FROM ALL_CONSTRAINTS
 WHERE OWNER = 'RHH' AND TABLE_NAME = 'PRNM' AND CONSTRAINT_NAME = 'CK_PRNMMDVC';

-- REVERSO (comentado; sólo antes de desplegar el WAR que la mapea)
-- ALTER TABLE RHH.PRNM DROP CONSTRAINT CK_PRNMMDVC;
-- ALTER TABLE RHH.PRNM DROP COLUMN PRNMMDVC;

-- =====================================================================================
-- VARIANTE SIN PL/SQL — la que se usó el 2026-10-05 en producción
-- En DBeaver el BLOQUE 1 no se ejecutó (probablemente por el comentario con q'[...]'), sin dar
-- error. Con el BLOQUE 0 en cero filas, estas sentencias sueltas hacen lo mismo. El último
-- UPDATE tiene que afectar exactamente 1 fila. Resultado del 2026-10-05: 2025 -> 1, 2026 -> 2,
-- CK_PRNMMDVC creada.
-- =====================================================================================
-- ALTER TABLE RHH.PRNM ADD (PRNMMDVC NUMBER DEFAULT 1);
-- UPDATE RHH.PRNM SET PRNMMDVC = 1 WHERE PRNMMDVC IS NULL;
-- ALTER TABLE RHH.PRNM ADD CONSTRAINT CK_PRNMMDVC CHECK (PRNMMDVC IN (1, 2));
-- COMMENT ON COLUMN RHH.PRNM.PRNMMDVC IS 'Modalidad de acreditacion de vacaciones: 1 por aniversario, 2 devengo mensual';
-- UPDATE RHH.PRNM SET PRNMMDVC = 2 WHERE PJRQCDGO = 1236 AND PRNMANOO = 2026;
-- COMMIT;

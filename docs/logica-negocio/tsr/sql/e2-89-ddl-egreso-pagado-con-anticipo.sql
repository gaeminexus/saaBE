-- =====================================================================================
-- e2-89 — DDL: el egreso de tesorería puede pagarse con el SALDO DE UN ANTICIPO (TSR.EGRS.EGRSANTP).
-- Equipo omen-saa-2 · 2026-10-05 · Diseño: docs/logica-negocio/tsr/DISENO-EGRESO-CON-SALDO-DE-ANTICIPO.md
--
-- ⛔⛔ VA ANTES DEL WAR. La entidad Egreso mapea la columna nueva: sin este script, TODA lectura de egresos
--    (registro, consulta, estado de cuenta, pagos) revienta con ORA-00904. No es solo la función nueva.
-- Sin FK: EGRS es de TSR y ANTP de PGS (una FK entre esquemas exige GRANT REFERENCES, §27).
-- Idempotente: si la columna ya existe, no hace nada.
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES. ESPERADO: CERO filas.
SELECT 'BLOQUE 0 - ya existe?' AS bloque, OWNER, TABLE_NAME, COLUMN_NAME
  FROM ALL_TAB_COLUMNS WHERE OWNER = 'TSR' AND TABLE_NAME = 'EGRS' AND COLUMN_NAME = 'EGRSANTP';

-- BLOQUE 1 — DDL
DECLARE
    v_existe NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_existe FROM ALL_TAB_COLUMNS
     WHERE OWNER = 'TSR' AND TABLE_NAME = 'EGRS' AND COLUMN_NAME = 'EGRSANTP';
    IF v_existe = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE TSR.EGRS ADD (EGRSANTP NUMBER)';
        EXECUTE IMMEDIATE 'CREATE INDEX TSR.IX_EGRS_ANTP ON TSR.EGRS (EGRSANTP)';
    END IF;
    EXECUTE IMMEDIATE q'[COMMENT ON COLUMN TSR.EGRS.EGRSANTP IS 'PGS.ANTP.ANTPCDGO: anticipo cuyo saldo pago este egreso (gasto sin sustento, p. ej. viaticos no justificados). Nulo = egreso pagado por banco. Sin FK: otro esquema']';
END;
/

-- BLOQUE 2 — CONTROL DESPUÉS. ESPERADO: 1 fila, NUMBER, nullable.
SELECT 'BLOQUE 2 - columna' AS bloque, TABLE_NAME, COLUMN_NAME, DATA_TYPE, NULLABLE
  FROM ALL_TAB_COLUMNS WHERE OWNER = 'TSR' AND TABLE_NAME = 'EGRS' AND COLUMN_NAME = 'EGRSANTP';

-- REVERSO (comentado; solo ANTES de desplegar un WAR que mapee la columna)
-- DROP INDEX TSR.IX_EGRS_ANTP;
-- ALTER TABLE TSR.EGRS DROP (EGRSANTP);

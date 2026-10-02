-- =====================================================================================
-- e3-10 — Liquidaciones de ex-colaboradores: cuenta contable por concepto (revisión con Contabilidad)
-- Equipo omen-saa-3 · 2026-10-02 · Diseño: rhh/API-LIQUIDACION-EXCOLABORADORES.md, REVISIÓN 2026-10-02
--
-- ⛔ VA ANTES DEL WAR que mapea DetalleLiquidacionExterna.cuentaContable (DLEXPLNN).
-- Requiere RHH.LQEX/DLEX VACÍAS: los códigos de concepto 2-9 cambian de significado.
-- Correr de corrido; el BLOQUE 1 se detiene solo si algo no cuadra.
-- =====================================================================================

-- BLOQUE 0 — CONTROLES ANTES (se esperan 0, 0 y 0)
SELECT (SELECT COUNT(*) FROM RHH.LQEX) AS LIQUIDACIONES,
       (SELECT COUNT(*) FROM RHH.DLEX) AS CONCEPTOS,
       (SELECT COUNT(*) FROM ALL_TAB_COLUMNS
         WHERE OWNER = 'RHH' AND TABLE_NAME = 'DLEX' AND COLUMN_NAME = 'DLEXPLNN') AS COLUMNA_YA_EXISTE
  FROM DUAL;

-- BLOQUE 1
DECLARE
    v_filas NUMBER;
    v_col   NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_filas FROM RHH.DLEX;
    IF v_filas > 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'RHH.DLEX tiene ' || v_filas || ' filas: los codigos de concepto cambian '
            || 'de significado y no se pueden reinterpretar. No se hace nada; avisar al arbitro.');
    END IF;
    SELECT COUNT(*) INTO v_col FROM ALL_TAB_COLUMNS
     WHERE OWNER = 'RHH' AND TABLE_NAME = 'DLEX' AND COLUMN_NAME = 'DLEXPLNN';
    IF v_col > 0 THEN
        RAISE_APPLICATION_ERROR(-20002, 'DLEXPLNN ya existe: no se hace nada.');
    END IF;

    EXECUTE IMMEDIATE 'ALTER TABLE RHH.DLEX ADD (DLEXPLNN NUMBER)';
    EXECUTE IMMEDIATE 'COMMENT ON COLUMN RHH.DLEX.DLEXPLNN IS ''Cuenta contable CNT.PLNN del concepto; obligatoria para enviar a Tesoreria. Sin FK: otro esquema''';
    EXECUTE IMMEDIATE 'ALTER TABLE RHH.LQEX MODIFY (LQEXPRDP NULL)';
    EXECUTE IMMEDIATE 'ALTER TABLE RHH.DLEX DROP CONSTRAINT CK_DLEXTPCN';
    EXECUTE IMMEDIATE 'ALTER TABLE RHH.DLEX ADD CONSTRAINT CK_DLEXTPCN CHECK (DLEXTPCN IN '
        || '(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 20, 21, 22, 23, 24, 25))';
END;
/

-- BLOQUE 2 — CONTROL DESPUÉS
-- Se espera: DLEXPLNN con NULLABLE = Y, y LQEXPRDP con NULLABLE = Y.
SELECT TABLE_NAME, COLUMN_NAME, NULLABLE FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'RHH' AND ((TABLE_NAME = 'DLEX' AND COLUMN_NAME = 'DLEXPLNN')
                       OR (TABLE_NAME = 'LQEX' AND COLUMN_NAME = 'LQEXPRDP'));
-- Se espera la lista 1..11 y 20..25.
SELECT SEARCH_CONDITION_VC FROM ALL_CONSTRAINTS
 WHERE OWNER = 'RHH' AND CONSTRAINT_NAME = 'CK_DLEXTPCN';

-- REVERSO (comentado; sólo con las tablas vacías y antes del WAR)
-- ALTER TABLE RHH.DLEX DROP CONSTRAINT CK_DLEXTPCN;
-- ALTER TABLE RHH.DLEX ADD CONSTRAINT CK_DLEXTPCN CHECK (DLEXTPCN IN (1,2,3,4,5,6,7,8,9,20,21,22,23));
-- ALTER TABLE RHH.DLEX DROP COLUMN DLEXPLNN;
-- ALTER TABLE RHH.LQEX MODIFY (LQEXPRDP NOT NULL);

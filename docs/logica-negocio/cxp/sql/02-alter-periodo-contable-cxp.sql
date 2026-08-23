-- ============================================================
-- Migración: Período contable en tablas CXP (CRTX y DCXP)
-- Módulo:    CXP - Cuentas por Pagar
-- Schema:    PGS
-- Fecha:     2026-07-24
--
-- Orden de ejecución: 2 de 5
-- Anterior: 01-create-tables-crtx-dcxp-dctx.sql
-- Siguiente: 03-rename-columnas-crtx-dcxp-dctx.sql
--
-- Propósito: Relacionar cargas y documentos con el período
--            contable del módulo CNT para poder detectar
--            documentos que ya no aparecen entre cargas del
--            mismo período.
-- ============================================================

-- 1. Agregar PERIODOCONTABLE a la tabla de cargas (CRTX)
--    FK → CNT.PRDO (PRDOCDGO). Nullable: cargas antiguas no tienen período.
ALTER TABLE PGS.CRTX
    ADD PERIODOCONTABLE NUMBER(11) NULL;

ALTER TABLE PGS.CRTX
    ADD CONSTRAINT FK_CRTX_PERIODO
        FOREIGN KEY (PERIODOCONTABLE)
        REFERENCES CNT.PRDO (PRDOCDGO);

COMMENT ON COLUMN PGS.CRTX.PERIODOCONTABLE
    IS 'FK a CNT.PRDO. Período contable al que pertenece esta carga del TXT.';


-- 2. Agregar PERIODOCONTABLE a la tabla de documentos únicos (DCXP)
--    FK → CNT.PRDO (PRDOCDGO). Nullable: documentos anteriores a la migración.
ALTER TABLE PGS.DCXP
    ADD PERIODOCONTABLE NUMBER(11) NULL;

ALTER TABLE PGS.DCXP
    ADD CONSTRAINT FK_DCXP_PERIODO
        FOREIGN KEY (PERIODOCONTABLE)
        REFERENCES CNT.PRDO (PRDOCDGO);

COMMENT ON COLUMN PGS.DCXP.PERIODOCONTABLE
    IS 'FK a CNT.PRDO. Período contable del documento, basado en su fecha de emisión.';


-- 3. Índices para acelerar las consultas de detección de desaparecidos
CREATE INDEX IDX_DCXP_EMPRESA_PERIODO
    ON PGS.DCXP (EMPRESA, PERIODOCONTABLE);

CREATE INDEX IDX_CRTX_EMPRESA_PERIODO
    ON PGS.CRTX (EMPRESA, PERIODOCONTABLE);


-- 4. (Opcional) Migrar documentos existentes al período según su FECHAEMISION
--    Solo asigna el período cuando existe un período CNT.PRDO con el mismo mes/año
--    y la misma empresa.
--
-- UPDATE PGS.DCXP d
-- SET d.PERIODOCONTABLE = (
--     SELECT p.PRDOCDGO
--     FROM CNT.PRDO p
--     WHERE p.PJRQCDGO = d.EMPRESA
--       AND p.PRDOMSSS = EXTRACT(MONTH FROM d.FECHAEMISION)
--       AND p.PRDOANNN = EXTRACT(YEAR  FROM d.FECHAEMISION)
--       AND ROWNUM = 1
-- )
-- WHERE d.FECHAEMISION IS NOT NULL
--   AND d.PERIODOCONTABLE IS NULL;
--
-- COMMIT;

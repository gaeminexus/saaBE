-- =====================================================================================
-- 327 — DDL: enlace del prestamo nuevo con el prestamo que sustituye
--       (reestructuracion y novacion)
-- FECHA: 2026-10-13 · EQUIPO: omen-saa-1 (crd)
-- Diseno: DISENO-REESTRUCTURACION-NOVACION.md RN12 y §5
--
-- ⛔ ESTE SCRIPT SI ESCRIBE (DDL). ⛔ VA ANTES DEL WAR que traiga reestructuracion/novacion:
--    el WAR mapea las dos columnas y sin ellas toda lectura de CRD.PRST revienta (ORA-00904).
-- Sin comandos de SQL*Plus.
-- =====================================================================================

-- BLOQUE 0 — (!) No existen todavia
SELECT COLUMN_NAME FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'CRD' AND TABLE_NAME = 'PRST' AND COLUMN_NAME IN ('PRSTPROR', 'PRSTVLSS');
-- (!) CERO filas. Si salen las dos, ya se corrio: no repetir el bloque 1.

SELECT INDEX_NAME FROM ALL_INDEXES WHERE OWNER = 'CRD' AND INDEX_NAME = 'IX_PRST_PRSTPROR';
-- (!) CERO filas.

-- BLOQUE 1 — Columnas e indice
ALTER TABLE CRD.PRST ADD (
    PRSTPROR NUMBER,          -- prestamo ORIGINAL que este sustituye (reestructuracion / novacion)
    PRSTVLSS NUMBER(18,2)     -- valor de sustitucion: lo que se cancelo del original al aprobar
);
COMMENT ON COLUMN CRD.PRST.PRSTPROR IS 'Prestamo original sustituido por reestructuracion o novacion';
COMMENT ON COLUMN CRD.PRST.PRSTVLSS IS 'Valor de sustitucion cancelado del original al aprobar';

CREATE INDEX CRD.IX_PRST_PRSTPROR ON CRD.PRST (PRSTPROR);

-- BLOQUE 2 — (!) CONTROL POSTERIOR
SELECT COLUMN_NAME, DATA_TYPE, DATA_PRECISION, DATA_SCALE FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'CRD' AND TABLE_NAME = 'PRST' AND COLUMN_NAME IN ('PRSTPROR', 'PRSTVLSS');
-- (!) 2 filas.
SELECT INDEX_NAME, STATUS FROM ALL_INDEXES WHERE OWNER = 'CRD' AND INDEX_NAME = 'IX_PRST_PRSTPROR';
-- (!) 1 fila, VALID.

-- BLOQUE 3 — REVERSO (comentado). Solo con el WAR anterior y sin ninguna sustitucion hecha.
-- SELECT COUNT(*) FROM CRD.PRST WHERE PRSTPROR IS NOT NULL;   -- tiene que dar 0
-- DROP INDEX CRD.IX_PRST_PRSTPROR;
-- ALTER TABLE CRD.PRST DROP (PRSTPROR, PRSTVLSS);

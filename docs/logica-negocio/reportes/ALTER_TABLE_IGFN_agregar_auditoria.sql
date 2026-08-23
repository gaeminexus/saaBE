-- ============================================================
-- ALTER TABLE CRD.IGFN
-- Agregar campos de auditoría y estado para control del G40
-- ============================================================

ALTER TABLE CRD.IGFN ADD (IGFNESTD NUMBER(1,0)    DEFAULT 1 NOT NULL);
ALTER TABLE CRD.IGFN ADD (IGFNUSRM VARCHAR2(50));
ALTER TABLE CRD.IGFN ADD (IGFNFCMD DATE);

COMMENT ON COLUMN CRD.IGFN.IGFNESTD IS 'Estado del registro: 1=Sin cambios, 2=Modificado. Si es 2 se genera el G40, si es 1 el G40 queda vacio y OK';
COMMENT ON COLUMN CRD.IGFN.IGFNUSRM IS 'Usuario que realizó la última modificación';
COMMENT ON COLUMN CRD.IGFN.IGFNFCMD IS 'Fecha de la última modificación';

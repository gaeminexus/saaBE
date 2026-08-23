-- ============================================================
-- Agrega la columna ENTDCDGO (FK a CRD.ENTD) en RPR.CG42
-- Permite búsqueda rápida por entidad + detalleEjecucion
-- para la lógica INSERT-or-UPDATE del G42
-- ============================================================

-- Paso 1: Agregar la columna
ALTER TABLE RPR.CG42
    ADD ENTDCDGO Long;

-- Paso 2: Otorgar privilegio de referencia entre esquemas (ejecutar como DBA o propietario de CRD)
GRANT REFERENCES ON CRD.ENTD TO RPR;

-- Paso 3: Crear la FK
ALTER TABLE RPR.CG42
    ADD CONSTRAINT FK_CG42_ENTD
    FOREIGN KEY (ENTDCDGO)
    REFERENCES CRD.ENTD (ENTDCDGO);

-- Paso 4: Índice compuesto para búsqueda rápida por entidad + detalle ejecución
CREATE INDEX IDX_CG42_ENTD_EJRD
    ON RPR.CG42 (ENTDCDGO, CG42EJRD);


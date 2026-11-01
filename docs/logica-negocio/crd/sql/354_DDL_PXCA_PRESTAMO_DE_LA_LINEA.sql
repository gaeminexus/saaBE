-- =====================================================================================
-- 354 — DDL. omen-saa-1, 2026-11-01. Plan de correcciones B4-A5 (carga ARCH).
-- La fila de préstamo que arma el lector de ARCH tiene que aplicar al préstamo EXACTO de su línea de la
-- generación (PDGA), no al primero del producto: con dos préstamos del mismo producto, el aplicador
-- hoy los mezcla. CRD.PXCA (ParticipeXCargaArchivo) no tiene dónde guardarlo: se agrega la columna
-- opcional PRSTCDGO (FK a CRD.PRST).
--
-- ⛔ VA ANTES DEL EAR que la mapea: con el EAR primero, toda lectura de PXCA da ORA-00904.
-- Aditivo: el código anterior no la lee; las filas existentes quedan con NULL (= «el primero del
-- producto», el comportamiento de siempre, que es lo que hace Petrocomercial).
-- =====================================================================================

-- CONTROL ANTES — la columna no existe. ESPERADO: 0 filas.
SELECT column_name FROM all_tab_columns WHERE owner = 'CRD' AND table_name = 'PXCA' AND column_name = 'PRSTCDGO';

-- CAMBIO
ALTER TABLE CRD.PXCA ADD (PRSTCDGO NUMBER NULL);
ALTER TABLE CRD.PXCA ADD CONSTRAINT FK_PXCA_PRST FOREIGN KEY (PRSTCDGO) REFERENCES CRD.PRST (PRSTCDGO);
COMMENT ON COLUMN CRD.PXCA.PRSTCDGO IS 'Préstamo exacto de la línea (respuesta ARCH, desde PDGA); NULL = el primero del producto';

-- CONTROL DESPUÉS — ESPERADO: 1 columna NUMBER nullable y la FK habilitada.
SELECT column_name, data_type, nullable FROM all_tab_columns WHERE owner = 'CRD' AND table_name = 'PXCA' AND column_name = 'PRSTCDGO';
SELECT constraint_name, constraint_type, status FROM all_constraints WHERE owner = 'CRD' AND constraint_name = 'FK_PXCA_PRST';

-- REVERSO (comentado; solo con el EAR anterior desplegado)
-- ALTER TABLE CRD.PXCA DROP CONSTRAINT FK_PXCA_PRST;
-- ALTER TABLE CRD.PXCA DROP COLUMN PRSTCDGO;

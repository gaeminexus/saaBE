-- =====================================================================================
-- 377 — DDL + backfill. omen-saa-1-arb, 2026-11-10. Campos de la ficha del partícipe para el Anexo I del contrato de
-- adhesión. Diseño: crd/DISENO-FICHA-PARTICIPE-CAMPOS-ANEXO-I.md.
--   CRD.ENTD.ENTDSEXO  VARCHAR2(1)   'M'/'F'
--   CRD.PRTC.PRTCMDLB  NUMBER(1)     1 NOMBRAMIENTO, 2 CONTRATO
--   CRD.PRTC.PRTCTTPR  VARCHAR2(300) título profesional
--   CRD.DRCC.DRCCBRRO  VARCHAR2(300) barrio / sector
-- ⛔ Va ANTES del EAR que mapee estas columnas: sin ellas toda lectura de ENTD/PRTC/DRCC da ORA-00904.
-- Aditivo: columnas nullable. Correr bloque por bloque leyendo cada control. SQL puro.
-- =====================================================================================

-- BLOQUE 0 — CONTROLES ANTES
-- 0.1 Las columnas NO existen. ESPERADO: 0 filas.
SELECT TABLE_NAME, COLUMN_NAME FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'CRD'
   AND ((TABLE_NAME = 'ENTD' AND COLUMN_NAME = 'ENTDSEXO')
     OR (TABLE_NAME = 'PRTC' AND COLUMN_NAME IN ('PRTCMDLB', 'PRTCTTPR'))
     OR (TABLE_NAME = 'DRCC' AND COLUMN_NAME = 'DRCCBRRO'));
-- 0.2 Totales (anotar: el 4.1 debe dar lo mismo).
SELECT (SELECT COUNT(*) FROM CRD.ENTD) AS ENTD, (SELECT COUNT(*) FROM CRD.PRTC) AS PRTC,
       (SELECT COUNT(*) FROM CRD.DRCC) AS DRCC FROM DUAL;
-- 0.3 Valores reales del género en PRAS (fuente del backfill). PASAR LA SALIDA AL ÁRBITRO antes del bloque 3:
--     si hay valores distintos de M/F (p. ej. H, MASCULINO, 1), el bloque 3 se ajusta antes de correr.
SELECT NVL(UPPER(TRIM(PRASGNRO)), '(nulo)') AS GENERO, COUNT(*) AS FILAS FROM CRD.PRAS
 GROUP BY NVL(UPPER(TRIM(PRASGNRO)), '(nulo)') ORDER BY 2 DESC;
-- 0.4 Entidades que se llenarían: cédula con un ÚNICO valor M/F en PRAS.
SELECT COUNT(*) AS ENTIDADES_A_LLENAR FROM CRD.ENTD e
 WHERE (SELECT COUNT(DISTINCT UPPER(TRIM(a.PRASGNRO))) FROM CRD.PRAS a
         WHERE TRIM(a.PRASCDLA) = TRIM(e.ENTDNMID) AND UPPER(TRIM(a.PRASGNRO)) IN ('M', 'F')) = 1;
-- 0.5 Cédulas con género contradictorio en PRAS (NO se llenan). ESPERADO: pocas o 0.
SELECT TRIM(a.PRASCDLA) AS CEDULA, COUNT(DISTINCT UPPER(TRIM(a.PRASGNRO))) AS VALORES FROM CRD.PRAS a
 WHERE UPPER(TRIM(a.PRASGNRO)) IN ('M', 'F')
 GROUP BY TRIM(a.PRASCDLA) HAVING COUNT(DISTINCT UPPER(TRIM(a.PRASGNRO))) > 1;

-- BLOQUE 1 — RESPALDO (solo ENTD: es la única tabla con UPDATE; PRTC y DRCC solo ganan columnas vacías)
CREATE TABLE CRD.ENTD_BK377 AS SELECT ENTDCDGO, ENTDNMID FROM CRD.ENTD;

-- BLOQUE 2 — COLUMNAS
ALTER TABLE CRD.ENTD ADD (ENTDSEXO VARCHAR2(1));
ALTER TABLE CRD.ENTD ADD CONSTRAINT CK_ENTD_SEXO CHECK (ENTDSEXO IN ('M', 'F'));
ALTER TABLE CRD.PRTC ADD (PRTCMDLB NUMBER(1), PRTCTTPR VARCHAR2(300));
ALTER TABLE CRD.PRTC ADD CONSTRAINT CK_PRTC_MDLB CHECK (PRTCMDLB IN (1, 2));
ALTER TABLE CRD.DRCC ADD (DRCCBRRO VARCHAR2(300));
COMMENT ON COLUMN CRD.ENTD.ENTDSEXO IS 'Sexo M/F (Anexo I del contrato de adhesion, sql/377)';
COMMENT ON COLUMN CRD.PRTC.PRTCMDLB IS 'Modalidad laboral: 1 NOMBRAMIENTO, 2 CONTRATO (sql/377)';
COMMENT ON COLUMN CRD.PRTC.PRTCTTPR IS 'Titulo profesional (sql/377)';
COMMENT ON COLUMN CRD.DRCC.DRCCBRRO IS 'Barrio / sector (sql/377)';

-- BLOQUE 3 — BACKFILL DEL SEXO desde PRAS (solo M/F y cédula con un único valor). ⛔ Correr SOLO si el 0.3 muestra
-- M/F; con otros valores, avisar al árbitro primero.
UPDATE CRD.ENTD e
   SET e.ENTDSEXO = (SELECT MAX(UPPER(TRIM(a.PRASGNRO))) FROM CRD.PRAS a
                      WHERE TRIM(a.PRASCDLA) = TRIM(e.ENTDNMID) AND UPPER(TRIM(a.PRASGNRO)) IN ('M', 'F'))
 WHERE e.ENTDSEXO IS NULL
   AND (SELECT COUNT(DISTINCT UPPER(TRIM(a.PRASGNRO))) FROM CRD.PRAS a
         WHERE TRIM(a.PRASCDLA) = TRIM(e.ENTDNMID) AND UPPER(TRIM(a.PRASGNRO)) IN ('M', 'F')) = 1;
-- ESPERADO: filas actualizadas = ENTIDADES_A_LLENAR del 0.4. Si difiere: ROLLBACK y avisar.
COMMIT;

-- BLOQUE 4 — CONTROLES DESPUÉS
-- 4.1 Mismos totales que el 0.2.
SELECT (SELECT COUNT(*) FROM CRD.ENTD) AS ENTD, (SELECT COUNT(*) FROM CRD.PRTC) AS PRTC,
       (SELECT COUNT(*) FROM CRD.DRCC) AS DRCC FROM DUAL;
-- 4.2 Columnas creadas. ESPERADO: 4 filas, NULLABLE = Y.
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'CRD' AND COLUMN_NAME IN ('ENTDSEXO', 'PRTCMDLB', 'PRTCTTPR', 'DRCCBRRO');
-- 4.3 Distribución del sexo (M + F = filas actualizadas en el bloque 3).
SELECT NVL(ENTDSEXO, '(sin dato)') AS SEXO, COUNT(*) FROM CRD.ENTD GROUP BY NVL(ENTDSEXO, '(sin dato)');
-- 4.4 Constraints. ESPERADO: 2 filas ENABLED.
SELECT CONSTRAINT_NAME, STATUS FROM ALL_CONSTRAINTS
 WHERE OWNER = 'CRD' AND CONSTRAINT_NAME IN ('CK_ENTD_SEXO', 'CK_PRTC_MDLB');

-- BLOQUE 5 — Cuando el EAR esté estable:
-- DROP TABLE CRD.ENTD_BK377;

-- REVERSO (comentado; solo con un EAR que no mapee estas columnas):
-- ALTER TABLE CRD.ENTD DROP CONSTRAINT CK_ENTD_SEXO;
-- ALTER TABLE CRD.ENTD DROP (ENTDSEXO);
-- ALTER TABLE CRD.PRTC DROP CONSTRAINT CK_PRTC_MDLB;
-- ALTER TABLE CRD.PRTC DROP (PRTCMDLB, PRTCTTPR);
-- ALTER TABLE CRD.DRCC DROP (DRCCBRRO);

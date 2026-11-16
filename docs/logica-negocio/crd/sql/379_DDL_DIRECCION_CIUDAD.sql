-- =====================================================================================================================
-- 379_DDL_DIRECCION_CIUDAD.sql — omen-saa-1-arb, 2026-11-16
-- Diseño: docs/logica-negocio/crd/DISENO-DIRECCION-DOMICILIO-UNICA-Y-CIUDAD.md
-- Agrega CRD.DRCC.CDDDCDGO (ciudad de la dirección, FK a CRD.CDDD) y la llena desde la parroquia donde exista.
-- Correr ANTES del EAR que trae el cambio: sin esta columna, toda lectura de Direccion da ORA-00904.
-- Bloque 0 es solo lectura: corre primero y pásale el resultado al árbitro.
-- =====================================================================================================================

-- ---------------------------------------------------------------------------------------------------------------------
-- BLOQUE 0 — DIAGNÓSTICO (solo lectura)
-- ---------------------------------------------------------------------------------------------------------------------
-- 0.1 ¿Existe ya la columna? (esperado: 0 filas)
SELECT column_name FROM all_tab_columns WHERE owner = 'CRD' AND table_name = 'DRCC' AND column_name = 'CDDDCDGO';

-- 0.2 Catálogo de ciudades y provincias (el combo sale de CRD.CDDD)
SELECT (SELECT COUNT(*) FROM CRD.CDDD) AS ciudades,
       (SELECT COUNT(*) FROM CRD.CDDD WHERE PRVNCDGO IS NULL) AS ciudades_sin_provincia,
       (SELECT COUNT(*) FROM CRD.PRVN) AS provincias,
       (SELECT COUNT(*) FROM CRD.PRRQ) AS parroquias
  FROM DUAL;

-- 0.3 Muestra del catálogo (primeras 30 por nombre)
SELECT * FROM (SELECT c.CDDDCDGO, c.CDDDNMBR, p.PRVNNMBR
                 FROM CRD.CDDD c LEFT JOIN CRD.PRVN p ON p.PRVNCDGO = c.PRVNCDGO
                ORDER BY c.CDDDNMBR) WHERE ROWNUM <= 30;

-- 0.4 Direcciones de domicilio activas y cuántas tienen parroquia (de ahí sale el respaldo de la ciudad)
SELECT COUNT(*) AS domicilios_activos,
       COUNT(d.PRRQCDGO) AS con_parroquia
  FROM CRD.DRCC d
 WHERE NVL(d.DRCCIDST, 1) = 1 AND NVL(d.DRCCTRBJ, 0) <> 1
   AND NOT EXISTS (SELECT 1 FROM CRD.DRTR t WHERE t.DRCCCDGO = d.DRCCCDGO);

-- 0.5 Socios con MÁS DE UNA dirección de domicilio por defecto (los que hoy ven una en pantalla y otra en la solicitud)
SELECT COUNT(*) AS entidades_con_varias_por_defecto
  FROM (SELECT d.ENTDCDGO
          FROM CRD.DRCC d
         WHERE NVL(d.DRCCIDST, 1) = 1 AND NVL(d.DRCCTRBJ, 0) <> 1 AND NVL(d.DRCCPRDF, 0) = 1
           AND NOT EXISTS (SELECT 1 FROM CRD.DRTR t WHERE t.DRCCCDGO = d.DRCCCDGO)
         GROUP BY d.ENTDCDGO HAVING COUNT(*) > 1);

-- 0.6 Quién crea esas direcciones extra (usuario de ingreso de la de mayor código, por usuario)
SELECT NVL(d.DRCCUSIN, '(nulo)') AS usuario_ingreso, COUNT(*) AS filas
  FROM CRD.DRCC d
 WHERE NVL(d.DRCCIDST, 1) = 1 AND NVL(d.DRCCTRBJ, 0) <> 1 AND NVL(d.DRCCPRDF, 0) = 1
   AND NOT EXISTS (SELECT 1 FROM CRD.DRTR t WHERE t.DRCCCDGO = d.DRCCCDGO)
   AND d.DRCCCDGO <> (SELECT MIN(d2.DRCCCDGO) FROM CRD.DRCC d2
                       WHERE d2.ENTDCDGO = d.ENTDCDGO AND NVL(d2.DRCCIDST, 1) = 1 AND NVL(d2.DRCCTRBJ, 0) <> 1
                         AND NVL(d2.DRCCPRDF, 0) = 1
                         AND NOT EXISTS (SELECT 1 FROM CRD.DRTR t2 WHERE t2.DRCCCDGO = d2.DRCCCDGO))
 GROUP BY NVL(d.DRCCUSIN, '(nulo)') ORDER BY 2 DESC;

-- ---------------------------------------------------------------------------------------------------------------------
-- BLOQUE 1 — DDL (solo si 0.1 dio 0 filas)
-- ---------------------------------------------------------------------------------------------------------------------
ALTER TABLE CRD.DRCC ADD (CDDDCDGO NUMBER);
COMMENT ON COLUMN CRD.DRCC.CDDDCDGO IS 'Ciudad de la dirección (FK CRD.CDDD). Se elige por combo en la ficha del partícipe';
ALTER TABLE CRD.DRCC ADD CONSTRAINT FK_DRCC_CDDD FOREIGN KEY (CDDDCDGO) REFERENCES CRD.CDDD (CDDDCDGO);
CREATE INDEX CRD.IX_DRCC_CDDD ON CRD.DRCC (CDDDCDGO);

-- ---------------------------------------------------------------------------------------------------------------------
-- BLOQUE 2 — Respaldo de la ciudad desde la parroquia (solo la columna nueva; no toca datos existentes)
-- ---------------------------------------------------------------------------------------------------------------------
UPDATE CRD.DRCC d
   SET d.CDDDCDGO = (SELECT pq.CDDDCDGO FROM CRD.PRRQ pq WHERE pq.PRRQCDGO = d.PRRQCDGO)
 WHERE d.PRRQCDGO IS NOT NULL AND d.CDDDCDGO IS NULL;
-- Esperado: igual o menor que «con_parroquia» de 0.4 (incluye también direcciones de trabajo o inactivas).
COMMIT;

-- ---------------------------------------------------------------------------------------------------------------------
-- BLOQUE 3 — CONTROLES
-- ---------------------------------------------------------------------------------------------------------------------
SELECT column_name, data_type, nullable FROM all_tab_columns
 WHERE owner = 'CRD' AND table_name = 'DRCC' AND column_name = 'CDDDCDGO';            -- 1 fila, NUMBER, Y
SELECT constraint_name, status FROM all_constraints
 WHERE owner = 'CRD' AND constraint_name = 'FK_DRCC_CDDD';                              -- ENABLED
SELECT COUNT(*) AS total, COUNT(CDDDCDGO) AS con_ciudad FROM CRD.DRCC;

-- ---------------------------------------------------------------------------------------------------------------------
-- REVERSO (comentado; solo con el EAR anterior desplegado)
-- ---------------------------------------------------------------------------------------------------------------------
-- DROP INDEX CRD.IX_DRCC_CDDD;
-- ALTER TABLE CRD.DRCC DROP CONSTRAINT FK_DRCC_CDDD;
-- ALTER TABLE CRD.DRCC DROP COLUMN CDDDCDGO;

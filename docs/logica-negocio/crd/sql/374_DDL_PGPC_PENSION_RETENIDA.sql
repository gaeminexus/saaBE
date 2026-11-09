-- =====================================================================================
-- 374 — DDL + backfill. omen-saa-1, 2026-11-09. Pensión retenida de jubilados sin cuenta bancaria (CRD.PGPC).
-- Diseño: docs/logica-negocio/crd/DISENO-PENSION-RETENIDA-SIN-CUENTA.md (§3.1 y §3.3, decisión D3-B).
-- ⛔ Va ANTES del EAR que mapee PGPCVLRT / PGPCDVRT (sin las columnas, toda lectura de PGPC da ORA-00904:
--    pantalla de pago de jubilados, corridas de seguro y pensiones).
-- Aditivo: las filas existentes quedan con retenido 0 salvo las que mide crd/sql/373 (backfill del bloque 3).
-- Correr bloque por bloque, en orden, leyendo cada control.
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES
-- 0.1 Las columnas NO existen (ESPERADO: 0 filas).
SELECT COLUMN_NAME FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'CRD' AND TABLE_NAME = 'PGPC' AND COLUMN_NAME IN ('PGPCVLRT', 'PGPCDVRT');
-- 0.2 Total de filas (anotar: el 4.1 debe dar lo mismo).
SELECT COUNT(*) AS TOTAL_PGPC FROM CRD.PGPC;
-- 0.3 Candidatas al backfill: deben coincidir con el BLOQUE 1 del 373 (FILAS_PGPC y VALOR_DEVENGADO_RETENIDO).
SELECT COUNT(*) AS FILAS, SUM(d.DTASHBRR) AS VALOR
FROM   CRD.PGPC p
JOIN   CNT.DTAS d ON d.ASNTCDGO = p.PGPCNMDV
WHERE  p.PGPCESTD = 1
AND    p.PGPCIDPG IS NULL
AND    d.DTASCNTA = '2.3.01.10.03'
AND    d.DTASHBRR > 0
AND    NOT EXISTS (SELECT 1 FROM CRD.APRT x
                   WHERE x.APRTCDGO = p.PGPCIDAP
                   AND   x.APRTVLRR < 0
                   AND   x.APRTGLSA NOT LIKE '% - SEGURO MEDICO');

-- BLOQUE 1 — RESPALDO
CREATE TABLE CRD.PGPC_BK374 AS SELECT * FROM CRD.PGPC;

-- BLOQUE 2 — COLUMNAS
ALTER TABLE CRD.PGPC ADD (PGPCVLRT NUMBER(18,2) DEFAULT 0 NOT NULL,
                          PGPCDVRT NUMBER(1)    DEFAULT 0 NOT NULL);
ALTER TABLE CRD.PGPC ADD CONSTRAINT CK_PGPC_VLRT CHECK (PGPCVLRT >= 0);
ALTER TABLE CRD.PGPC ADD CONSTRAINT CK_PGPC_DVRT CHECK (PGPCDVRT IN (0, 1));
COMMENT ON COLUMN CRD.PGPC.PGPCVLRT IS 'Pension retenida (sin cuenta/certificado) pendiente de pago, queda en el aporte 23 (sql/374)';
COMMENT ON COLUMN CRD.PGPC.PGPCDVRT IS '1 = el retenido ya se devengo antes del cambio (historico, D3-B): al pagarlo no se devenga otra vez (sql/374)';

-- BLOQUE 3 — BACKFILL de los históricos (los mismos del 0.3)
UPDATE CRD.PGPC p
   SET p.PGPCVLRT = (SELECT SUM(d.DTASHBRR) FROM CNT.DTAS d
                      WHERE d.ASNTCDGO = p.PGPCNMDV
                      AND   d.DTASCNTA = '2.3.01.10.03'
                      AND   d.DTASHBRR > 0),
       p.PGPCDVRT = 1
 WHERE p.PGPCESTD = 1
   AND p.PGPCIDPG IS NULL
   AND EXISTS (SELECT 1 FROM CNT.DTAS d
                WHERE d.ASNTCDGO = p.PGPCNMDV
                AND   d.DTASCNTA = '2.3.01.10.03'
                AND   d.DTASHBRR > 0)
   AND NOT EXISTS (SELECT 1 FROM CRD.APRT x
                    WHERE x.APRTCDGO = p.PGPCIDAP
                    AND   x.APRTVLRR < 0
                    AND   x.APRTGLSA NOT LIKE '% - SEGURO MEDICO');
-- ESPERADO: filas actualizadas = FILAS del 0.3. Si difiere: ROLLBACK y avisar al árbitro.
COMMIT;

-- BLOQUE 4 — CONTROL DESPUÉS
-- 4.1 Mismas filas que el 0.2.
SELECT COUNT(*) AS TOTAL_PGPC FROM CRD.PGPC;
-- 4.2 Retenidos marcados (ESPERADO: FILAS y VALOR iguales al 0.3).
SELECT COUNT(*) AS FILAS, SUM(PGPCVLRT) AS VALOR FROM CRD.PGPC WHERE PGPCDVRT = 1;
-- 4.3 Ninguna fila no histórica con retenido (ESPERADO: 0).
SELECT COUNT(*) AS INESPERADAS FROM CRD.PGPC WHERE PGPCDVRT = 0 AND PGPCVLRT > 0;
-- 4.4 Constraints habilitados (ESPERADO: 2 filas ENABLED).
SELECT CONSTRAINT_NAME, STATUS FROM ALL_CONSTRAINTS
 WHERE OWNER = 'CRD' AND CONSTRAINT_NAME IN ('CK_PGPC_VLRT', 'CK_PGPC_DVRT');

-- BLOQUE 5 — Cuando el EAR esté estable y verificado:
-- DROP TABLE CRD.PGPC_BK374;

-- REVERSO (comentado; solo con un EAR que no mapee PGPCVLRT / PGPCDVRT):
-- ALTER TABLE CRD.PGPC DROP CONSTRAINT CK_PGPC_DVRT;
-- ALTER TABLE CRD.PGPC DROP CONSTRAINT CK_PGPC_VLRT;
-- ALTER TABLE CRD.PGPC DROP (PGPCVLRT, PGPCDVRT);

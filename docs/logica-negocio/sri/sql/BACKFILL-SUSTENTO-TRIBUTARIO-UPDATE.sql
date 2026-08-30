-- =====================================================================
-- Backfill REAL de codSustento (PGS.FCTC.FCTCCSUS) en facturas de compra
-- ya registradas
-- =====================================================================
-- Contexto: docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md §6.4/§6.5.
-- Complementa a sql/BACKFILL-SUSTENTO-TRIBUTARIO.sql, que es SOLO SELECT
-- (medicion). Este archivo es el UPDATE real, escrito a partir de esos
-- numeros: 103 facturas -> '01', 28 -> '02', 0 sin resolver, verificado
-- contra la base local (copia de produccion) el 2026-08-27.
--
-- MISMA REGLA que el script de medicion, sin cambios:
--   1) EXCEPCION: si el grupo de producto con mayor base imponible
--      acumulada en la factura tiene GRPPCSUS en (03,04,06,07,08), ese
--      codigo gana. Hoy NINGUN grupo tiene excepcion configurada -> esta
--      rama no aporta filas todavia, pero el UPDATE la deja lista para
--      cuando se configure alguna.
--   2) BASE (si no hay excepcion): PGS.FCTC.VIVA > 0 -> '01'; si no -> '02'.
--
-- ALCANCE: solo facturas ACTIVAS (ESTADO=1) con FCTCCSUS todavia NULL. No
-- toca facturas anuladas ni las que ya tengan un sustento asignado a mano
-- (correccion manual via corregirSustento, si alguna se hizo entre el
-- 27-08 y hoy).
--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 0: control previo
--   (a) cuantas facturas activas tienen FCTCCSUS NULL hoy (universo real
--       del backfill -- compara contra 131; puede ser distinto si hubo
--       facturas nuevas o correcciones manuales desde el 27-08)
--   (b) distribucion esperada por codigo, para comparar contra el
--       resultado real despues del UPDATE
-- ---------------------------------------------------------------------
SELECT COUNT(*) AS FACTURAS_A_RESOLVER
FROM PGS.FCTC
WHERE ESTADO = 1 AND FCTCCSUS IS NULL;

WITH EXCEPCION_POR_GRUPO AS (
    SELECT
        DF.FACTURA                      AS ID_FACTURA,
        G.GRPPCSUS                      AS SUSTENTO_GRUPO,
        SUM(DF.BASEIMPONIBLE)           AS BASE_ACUMULADA
    FROM PGS.DFCC DF
    JOIN PGS.PRDP P ON P.ID = DF.PRODUCTO
    JOIN PGS.GRPP G ON G.GRPPCDGO = P.GRUPOPRODUCTO
    WHERE G.GRPPCSUS IN ('03','04','06','07','08')
    GROUP BY DF.FACTURA, G.GRPPCDGO, G.GRPPCSUS
),
EXCEPCION_GANADORA AS (
    SELECT ID_FACTURA, SUSTENTO_GRUPO,
           ROW_NUMBER() OVER (PARTITION BY ID_FACTURA ORDER BY BASE_ACUMULADA DESC) AS ORDEN
    FROM EXCEPCION_POR_GRUPO
)
SELECT
    NVL(EG.SUSTENTO_GRUPO, CASE WHEN NVL(F.VIVA, 0) > 0 THEN '01' ELSE '02' END) AS SUSTENTO_RESUELTO,
    COUNT(*)                                                                     AS NUM_FACTURAS
FROM PGS.FCTC F
LEFT JOIN EXCEPCION_GANADORA EG ON EG.ID_FACTURA = F.ID AND EG.ORDEN = 1
WHERE F.ESTADO = 1 AND F.FCTCCSUS IS NULL
GROUP BY NVL(EG.SUSTENTO_GRUPO, CASE WHEN NVL(F.VIVA, 0) > 0 THEN '01' ELSE '02' END)
ORDER BY 1;
-- Esperado (verificado 27-08 sobre el universo de ese dia): 103 -> '01', 28 -> '02'.

-- ---------------------------------------------------------------------
-- BLOQUE 1: respaldo
-- ---------------------------------------------------------------------
CREATE TABLE PGS.BKP_FCTC_SUSTENTO_20260828 AS
SELECT ID, NUMERO, FECHA, TITULAR, VIVA, FCTCCSUS
FROM PGS.FCTC
WHERE ESTADO = 1 AND FCTCCSUS IS NULL;

-- ---------------------------------------------------------------------
-- BLOQUE 2: el UPDATE real, misma regla, misma logica de las CTE de
--   arriba pero como subconsulta correlacionada (un UPDATE no puede usar
--   WITH directamente sobre la tabla que actualiza en todas las
--   versiones de Oracle sin MERGE; se resuelve con MERGE, mas simple y
--   mas seguro que dos UPDATE encadenados).
-- ---------------------------------------------------------------------
MERGE INTO PGS.FCTC F
USING (
    WITH EXCEPCION_POR_GRUPO AS (
        SELECT
            DF.FACTURA                      AS ID_FACTURA,
            G.GRPPCSUS                      AS SUSTENTO_GRUPO,
            SUM(DF.BASEIMPONIBLE)           AS BASE_ACUMULADA
        FROM PGS.DFCC DF
        JOIN PGS.PRDP P ON P.ID = DF.PRODUCTO
        JOIN PGS.GRPP G ON G.GRPPCDGO = P.GRUPOPRODUCTO
        WHERE G.GRPPCSUS IN ('03','04','06','07','08')
        GROUP BY DF.FACTURA, G.GRPPCDGO, G.GRPPCSUS
    ),
    EXCEPCION_GANADORA AS (
        SELECT ID_FACTURA, SUSTENTO_GRUPO,
               ROW_NUMBER() OVER (PARTITION BY ID_FACTURA ORDER BY BASE_ACUMULADA DESC) AS ORDEN
        FROM EXCEPCION_POR_GRUPO
    )
    SELECT
        FC.ID AS ID_FACTURA,
        NVL(EG.SUSTENTO_GRUPO, CASE WHEN NVL(FC.VIVA, 0) > 0 THEN '01' ELSE '02' END) AS SUSTENTO_RESUELTO
    FROM PGS.FCTC FC
    LEFT JOIN EXCEPCION_GANADORA EG ON EG.ID_FACTURA = FC.ID AND EG.ORDEN = 1
    WHERE FC.ESTADO = 1 AND FC.FCTCCSUS IS NULL
) RESUELTO
ON (F.ID = RESUELTO.ID_FACTURA)
WHEN MATCHED THEN UPDATE SET F.FCTCCSUS = RESUELTO.SUSTENTO_RESUELTO;

-- ---------------------------------------------------------------------
-- BLOQUE 3: control posterior
--   (a) no debe quedar ninguna factura activa sin resolver
--   (b) distribucion final, debe coincidir con el bloque 0(b)
-- ---------------------------------------------------------------------
SELECT COUNT(*) AS FACTURAS_ACTIVAS_SIN_RESOLVER
FROM PGS.FCTC
WHERE ESTADO = 1 AND FCTCCSUS IS NULL;
-- Debe dar 0.

SELECT FCTCCSUS, COUNT(*) AS NUM_FACTURAS
FROM PGS.FCTC
WHERE ESTADO = 1
GROUP BY FCTCCSUS
ORDER BY 1;

COMMIT;

-- ---------------------------------------------------------------------
-- BLOQUE 4: reverso (comentado a proposito -- descomentar a mano solo si
--   hace falta deshacer el backfill)
-- ---------------------------------------------------------------------
-- MERGE INTO PGS.FCTC F
-- USING PGS.BKP_FCTC_SUSTENTO_20260828 B
-- ON (F.ID = B.ID)
-- WHEN MATCHED THEN UPDATE SET F.FCTCCSUS = B.FCTCCSUS;
-- COMMIT;

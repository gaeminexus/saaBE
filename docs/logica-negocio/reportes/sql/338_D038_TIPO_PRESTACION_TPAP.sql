-- 338_D038_TIPO_PRESTACION_TPAP.sql — D-038 (29-oct-2026): grupo de prestación de cada tipo de aporte.
--
-- Por qué: el tipo de prestación de la G42 (campo 3, tabla 194: C cesantía, J jubilación, M mixto) es el de la cuenta
-- individual del partícipe (Manual de Catastros FCPC v1.2, §2.3). Se decide por el saldo de cada GRUPO de prestación:
-- M si los dos grupos tienen saldo distinto de cero, C o J si solo uno. La regla reproduce 4.564 de los 4.585 sujetos de
-- la G42 presentada en mayo 2025 (la anterior, "alguna fila de tipo 9/11", solo 1.745).
--
-- Por qué una columna y no una lista en el código: el grupo es un atributo del tipo de aporte (dato del catálogo, no
-- regla de negocio). No se puede sacar de TPAPCSBC porque los rendimientos de los dos grupos comparten 'RE'. Con la
-- columna, un tipo de aporte nuevo se clasifica con un UPDATE (o en la pantalla, cuando se agregue) sin cambiar código;
-- un tipo sin grupo (nulo) no cuenta para el tipo de prestación. El generador (AporteDaoServiceImpl.
-- selectSumaPorGrupoPrestacionPorEntidad) la lee por SQL nativo: la entidad TipoAporte no la mapea, así que el resto
-- del módulo de aportes no depende de este script. Si la G42 se genera sin aplicarlo, falla con un mensaje claro.
--
-- Se aplica UNA vez, ANTES de desplegar el WAR que trae D-038. SQL puro (cliente JDBC).

-- 1. DDL
ALTER TABLE CRD.TPAP ADD (TPAPTPPR VARCHAR2(1));
ALTER TABLE CRD.TPAP ADD CONSTRAINT CK_TPAP_TPPR CHECK (TPAPTPPR IS NULL OR TPAPTPPR IN ('C','J'));
COMMENT ON COLUMN CRD.TPAP.TPAPTPPR IS 'D-038: grupo de prestación del tipo de aporte para la G42 (C cesantía, J jubilación; nulo = no cuenta)';

-- 2. Datos (catálogo al 29-oct-2026)
--    J: 9 jubilación personal, 10 jubilación personal (RE), 13 jubilación patronal, 15 rendimiento jubilación patronal,
--       22 jubilación retiro voluntario, 23 pensión complementaria, 24 rendimiento jubilación personal, 25 aporte jubilar
--    C: 11 cesantía personal, 12 rendimiento cesantía personal, 14 cesantía patronal, 16 rendimiento cesantía patronal,
--       21 cesantía retiro voluntario
--    Sin grupo: 1-5 (aportes genéricos de Delta), 17-20 (reservas, inactivos), 27 (seguro por entregar)
UPDATE CRD.TPAP SET TPAPTPPR = 'J' WHERE TPAPCDGO IN (9, 10, 13, 15, 22, 23, 24, 25);
UPDATE CRD.TPAP SET TPAPTPPR = 'C' WHERE TPAPCDGO IN (11, 12, 14, 16, 21);
COMMIT;

-- 3. Controles: 8 J, 5 C, el resto nulo
SELECT TPAPTPPR, COUNT(*) tipos, LISTAGG(TPAPCDGO, ',') WITHIN GROUP (ORDER BY TPAPCDGO) codigos
FROM CRD.TPAP GROUP BY TPAPTPPR ORDER BY 1;

-- 3.b Regla contra la G42 presentada en mayo 2025 (esperado: 4.585 sujetos, 4.564 coinciden)
WITH s AS (
  SELECT a.ENTDCDGO,
         SUM(CASE WHEN t.TPAPTPPR = 'J' THEN a.APRTVLRR ELSE 0 END) sj,
         SUM(CASE WHEN t.TPAPTPPR = 'C' THEN a.APRTVLRR ELSE 0 END) sc
  FROM CRD.APRT a JOIN CRD.TPAP t ON t.TPAPCDGO = a.TPAPCDGO
  WHERE t.TPAPIDST = 1 AND t.TPAPTPPR IN ('J','C')
    AND (NVL(a.APRTIDST, 4) <> 0 AND NOT (NVL(a.APRTTPMV, 0) = 5 AND NVL(a.APRTGLSA, ' ') LIKE '%reverso APRT %'))
    AND a.APRTFCTR < DATE '2025-06-01'
  GROUP BY a.ENTDCDGO)
SELECT COUNT(*) sujetos_hm42,
       SUM(CASE WHEN (CASE WHEN ABS(s.sj) >= 0.005 AND ABS(s.sc) >= 0.005 THEN 'M'
                          WHEN ABS(s.sj) >= 0.005 THEN 'J' ELSE 'C' END) = h.HM42TPPR THEN 1 ELSE 0 END) coinciden
FROM RPR.HM42 h LEFT JOIN CRD.ENTD e ON TRIM(e.ENTDNMID) = TRIM(h.HM42IDPR) LEFT JOIN s ON s.ENTDCDGO = e.ENTDCDGO;

-- Reversa (solo antes del despliegue): ALTER TABLE CRD.TPAP DROP CONSTRAINT CK_TPAP_TPPR; ALTER TABLE CRD.TPAP DROP COLUMN TPAPTPPR;

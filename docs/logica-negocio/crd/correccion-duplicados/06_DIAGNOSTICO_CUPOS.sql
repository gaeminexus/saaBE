-- =============================================================================
-- 06 — ¿POR QUE HAY TANTOS "SOBRANTES"? DIAGNOSTICO DE LOS CUPOS
-- =============================================================================
-- FECHA: 2026-08-31 · Equipo omen-saa-2
--
-- ⛔ SOLO LECTURA. Sin DML, sin bloque de reverso.
-- ⛔ TODO ESTE SCRIPT DEVUELVE POCAS FILAS A PROPOSITO. Nada de listados largos:
--    se corre y se pega el resultado entero.
--
-- -----------------------------------------------------------------------------
-- POR QUE EXISTE
-- -----------------------------------------------------------------------------
--   El bloque 4 del 03 (SOBRANTES: participes con mas filas moviles que cupos)
--   devolvio "muchisimos registros". Eso NO deberia pasar: un sobrante es un
--   participe con mas aportes que meses esperados, o sea una anomalia. Que sean
--   muchos significa una de dos cosas, y son opuestas:
--
--     (a) De verdad hay muchisimos aportes de mas  -> el frente es enorme.
--     (b) CUPOS esta SUBCONTANDO los meses esperados -> el frente es normal y
--         la propuesta del bloque 2 esta mal calculada.
--
--   La (b) es mucho mas probable, porque los cupos salen de la vigencia del
--   contrato (CRD.CNTR + CRD.VGCN) y esos datos se migraron desde CRD.HSTR. Si a
--   un participe le falta el contrato ACTIVO, o la vigencia, o tiene VGCNMNTO en
--   0, sus cupos dan CERO y TODAS sus filas salen como sobrantes.
--
--   Contra eso, el bloque 5 del 03 midio 30.308 meses esperados en total para
--   apenas 748 participes de jubilacion y 1.418 de cesantia — cuando las cargas
--   tocan a mas de 2.000. Ya ahi hay un hueco de cobertura.
--
--   ⚠ Si gana la (b), la propuesta del 03 NO SE PUEDE APLICAR TAL CUAL: estaria
--     compactando contra una grilla de meses incompleta y dejando fuera del
--     alcance a los participes sin vigencia, en silencio.
--
-- INDICE
--   1  Cobertura: cuantos participes con filas moviles tienen contrato y vigencia
--   2  Sobrantes, partidos por causa (sin cupos / con cupos insuficientes)
--   3  Distribucion del sobrante (¿son de a 1-2 filas o de a muchas?)
--   4  Los 15 casos mas grandes, con su contexto — la unica lista, y es corta
--   5  Vigencias: por que un participe puede no tener cupos
-- =============================================================================


-- =============================================================================
-- 1. COBERTURA — ¿cuantos de los que tienen filas moviles tienen grilla de meses?
-- =============================================================================
-- Si CON_CONTRATO_ACTIVO o CON_VIGENCIA quedan MUY por debajo de CON_FILAS, la
-- causa (b) esta confirmada y el problema es de datos de contrato, no de aportes.
-- =============================================================================
WITH MOVILES AS (
        SELECT  DISTINCT a.ENTDCDGO
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (9, 11)
        AND     a.APRTVLRR > 0
        AND     a.APRTFCTR >= DATE '2025-06-01'
        AND     (   a.CRARCDGO IS NOT NULL
                 OR EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
                 OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                 OR a.APRTGLSA LIKE 'Abono al aporte%')
)
SELECT  (SELECT COUNT(*) FROM MOVILES)                                  AS CON_FILAS_MOVILES,
        (SELECT COUNT(*) FROM MOVILES m
          WHERE EXISTS (SELECT 1 FROM CRD.CNTR c
                        WHERE c.ENTDCDGO = m.ENTDCDGO AND c.CNTRESTD = 1))  AS CON_CONTRATO_ACTIVO,
        (SELECT COUNT(*) FROM MOVILES m
          WHERE EXISTS (SELECT 1 FROM CRD.CNTR c
                        JOIN CRD.VGCN v ON v.CNTRCDGO = c.CNTRCDGO
                        WHERE c.ENTDCDGO = m.ENTDCDGO AND c.CNTRESTD = 1
                        AND   v.TPAPCDGO IN (9, 11)))                       AS CON_ALGUNA_VIGENCIA,
        (SELECT COUNT(*) FROM MOVILES m
          WHERE EXISTS (SELECT 1 FROM CRD.CNTR c
                        JOIN CRD.VGCN v ON v.CNTRCDGO = c.CNTRCDGO
                        WHERE c.ENTDCDGO = m.ENTDCDGO AND c.CNTRESTD = 1
                        AND   v.TPAPCDGO IN (9, 11)
                        AND   v.VGCNIDST = 1 AND NVL(v.VGCNMNTO, 0) > 0))   AS CON_VIGENCIA_UTIL
FROM    DUAL;


-- =============================================================================
-- 2. SOBRANTES PARTIDOS POR CAUSA
-- =============================================================================
-- SIN NINGUN CUPO  = el participe no tiene grilla de meses: contrato o vigencia
--                    faltante. Sus filas salen sobrantes por un problema de
--                    datos de contrato, NO por tener aportes de mas.
-- CON CUPOS CORTOS = tiene grilla, pero mas filas que meses. Estos si son
--                    candidatos reales a exceso.
-- =============================================================================
WITH PARAM AS (
        SELECT  DATE '2025-06-01' AS PISO,
                (SELECT MAX(TO_DATE(TO_CHAR(c.CRARANAF) || LPAD(TO_CHAR(c.CRARMSAF), 2, '0'), 'YYYYMM'))
                   FROM CRD.CRAR c WHERE c.CRARESTD = 3) AS TECHO
        FROM DUAL
),
MESES AS (
        SELECT ADD_MONTHS(p.PISO, LEVEL - 1) AS MES FROM PARAM p
        CONNECT BY LEVEL <= MONTHS_BETWEEN(p.TECHO, p.PISO) + 1
),
APORTES AS (
        SELECT  a.ENTDCDGO, a.TPAPCDGO, a.APRTVLRR,
                CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                     ELSE TRUNC(a.APRTFCTR, 'MM') END       AS PERIODO_EFECTIVO,
                CASE WHEN (    a.APRTFCTR >= DATE '2025-06-01'
                           AND (   a.CRARCDGO IS NOT NULL
                                OR EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
                                OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                                OR a.APRTGLSA LIKE 'Abono al aporte%'))
                     THEN 'MOVIL' ELSE 'FIJA' END           AS CLASE
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0 AND NVL(a.APRTTPMV, 1) <> 8
),
CONTRATO_ACTIVO AS (
        SELECT c.ENTDCDGO, MAX(c.CNTRCDGO) AS CNTRCDGO FROM CRD.CNTR c WHERE c.CNTRESTD = 1 GROUP BY c.ENTDCDGO
),
ESPERADO AS (
        SELECT  DISTINCT ca.ENTDCDGO, v.TPAPCDGO, m.MES
        FROM    MESES m
        CROSS   JOIN CONTRATO_ACTIVO ca
        JOIN    CRD.VGCN v ON v.CNTRCDGO = ca.CNTRCDGO
        WHERE   v.TPAPCDGO IN (9, 11) AND v.VGCNIDST = 1 AND NVL(v.VGCNMNTO, 0) > 0
        AND     v.VGCNFCIN <= LAST_DAY(m.MES)
        AND     (v.VGCNFCFN IS NULL OR v.VGCNFCFN >= LAST_DAY(m.MES))
),
OCUPADO_FIJO AS (
        SELECT DISTINCT a.ENTDCDGO, a.TPAPCDGO, a.PERIODO_EFECTIVO AS MES
        FROM   APORTES a WHERE a.CLASE = 'FIJA' AND a.PERIODO_EFECTIVO IS NOT NULL
),
CUPOS_N AS (
        SELECT  e.ENTDCDGO, e.TPAPCDGO, COUNT(*) AS CUPOS
        FROM    ESPERADO e
        WHERE   NOT EXISTS ( SELECT 1 FROM OCUPADO_FIJO o
                             WHERE o.ENTDCDGO = e.ENTDCDGO AND o.TPAPCDGO = e.TPAPCDGO AND o.MES = e.MES )
        GROUP BY e.ENTDCDGO, e.TPAPCDGO
),
MOVILES_N AS (
        SELECT  a.ENTDCDGO, a.TPAPCDGO, COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS VALOR
        FROM    APORTES a CROSS JOIN PARAM p
        WHERE   a.CLASE = 'MOVIL' AND a.PERIODO_EFECTIVO BETWEEN p.PISO AND p.TECHO
        GROUP BY a.ENTDCDGO, a.TPAPCDGO
)
SELECT  CASE WHEN NVL(cn.CUPOS, 0) = 0 THEN '1. SIN NINGUN CUPO — falta contrato o vigencia'
             WHEN mn.FILAS > cn.CUPOS  THEN '2. CON CUPOS, PERO CORTOS — candidato real'
             ELSE                           '3. ALCANZAN LOS CUPOS — sin sobrante'
        END                                             AS SITUACION,
        mn.TPAPCDGO                                     AS TIPO,
        COUNT(*)                                        AS PARES_ENTIDAD_TIPO,
        COUNT(DISTINCT mn.ENTDCDGO)                     AS PARTICIPES,
        SUM(mn.FILAS)                                   AS FILAS_MOVILES,
        SUM(NVL(cn.CUPOS, 0))                           AS CUPOS,
        SUM(GREATEST(mn.FILAS - NVL(cn.CUPOS, 0), 0))   AS SOBRANTES,
        ROUND(SUM(mn.VALOR), 2)                         AS VALOR
FROM    MOVILES_N mn
LEFT    JOIN CUPOS_N cn ON cn.ENTDCDGO = mn.ENTDCDGO AND cn.TPAPCDGO = mn.TPAPCDGO
GROUP BY CASE WHEN NVL(cn.CUPOS, 0) = 0 THEN '1. SIN NINGUN CUPO — falta contrato o vigencia'
              WHEN mn.FILAS > cn.CUPOS  THEN '2. CON CUPOS, PERO CORTOS — candidato real'
              ELSE                           '3. ALCANZAN LOS CUPOS — sin sobrante'
         END, mn.TPAPCDGO
ORDER BY 1, 2;


-- =============================================================================
-- 3. DISTRIBUCION DEL SOBRANTE — ¿de a una fila o de a muchas?
-- =============================================================================
-- Un sobrante de 1-2 filas es ruido de borde (un mes de anticipo, una vigencia
-- que arranca tarde). Un sobrante de 5+ filas es otra cosa.
-- =============================================================================
WITH PARAM AS (
        SELECT  DATE '2025-06-01' AS PISO,
                (SELECT MAX(TO_DATE(TO_CHAR(c.CRARANAF) || LPAD(TO_CHAR(c.CRARMSAF), 2, '0'), 'YYYYMM'))
                   FROM CRD.CRAR c WHERE c.CRARESTD = 3) AS TECHO
        FROM DUAL
),
MESES AS (
        SELECT ADD_MONTHS(p.PISO, LEVEL - 1) AS MES FROM PARAM p
        CONNECT BY LEVEL <= MONTHS_BETWEEN(p.TECHO, p.PISO) + 1
),
APORTES AS (
        SELECT  a.ENTDCDGO, a.TPAPCDGO,
                CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV ELSE TRUNC(a.APRTFCTR, 'MM') END AS PERIODO_EFECTIVO,
                CASE WHEN (    a.APRTFCTR >= DATE '2025-06-01'
                           AND (   a.CRARCDGO IS NOT NULL
                                OR EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
                                OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                                OR a.APRTGLSA LIKE 'Abono al aporte%'))
                     THEN 'MOVIL' ELSE 'FIJA' END AS CLASE
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0 AND NVL(a.APRTTPMV, 1) <> 8
),
CONTRATO_ACTIVO AS (
        SELECT c.ENTDCDGO, MAX(c.CNTRCDGO) AS CNTRCDGO FROM CRD.CNTR c WHERE c.CNTRESTD = 1 GROUP BY c.ENTDCDGO
),
ESPERADO AS (
        SELECT  DISTINCT ca.ENTDCDGO, v.TPAPCDGO, m.MES
        FROM    MESES m CROSS JOIN CONTRATO_ACTIVO ca
        JOIN    CRD.VGCN v ON v.CNTRCDGO = ca.CNTRCDGO
        WHERE   v.TPAPCDGO IN (9, 11) AND v.VGCNIDST = 1 AND NVL(v.VGCNMNTO, 0) > 0
        AND     v.VGCNFCIN <= LAST_DAY(m.MES)
        AND     (v.VGCNFCFN IS NULL OR v.VGCNFCFN >= LAST_DAY(m.MES))
),
OCUPADO_FIJO AS (
        SELECT DISTINCT a.ENTDCDGO, a.TPAPCDGO, a.PERIODO_EFECTIVO AS MES
        FROM   APORTES a WHERE a.CLASE = 'FIJA' AND a.PERIODO_EFECTIVO IS NOT NULL
),
CUPOS_N AS (
        SELECT  e.ENTDCDGO, e.TPAPCDGO, COUNT(*) AS CUPOS FROM ESPERADO e
        WHERE   NOT EXISTS (SELECT 1 FROM OCUPADO_FIJO o
                            WHERE o.ENTDCDGO = e.ENTDCDGO AND o.TPAPCDGO = e.TPAPCDGO AND o.MES = e.MES)
        GROUP BY e.ENTDCDGO, e.TPAPCDGO
),
MOVILES_N AS (
        SELECT  a.ENTDCDGO, a.TPAPCDGO, COUNT(*) AS FILAS
        FROM    APORTES a CROSS JOIN PARAM p
        WHERE   a.CLASE = 'MOVIL' AND a.PERIODO_EFECTIVO BETWEEN p.PISO AND p.TECHO
        GROUP BY a.ENTDCDGO, a.TPAPCDGO
)
SELECT  CASE WHEN NVL(cn.CUPOS, 0) = 0 THEN 'sin cupos'
             ELSE TO_CHAR(LEAST(mn.FILAS - cn.CUPOS, 6)) END  AS SOBRANTE_POR_PAR,
        COUNT(*)                                              AS PARES,
        COUNT(DISTINCT mn.ENTDCDGO)                           AS PARTICIPES
FROM    MOVILES_N mn
LEFT    JOIN CUPOS_N cn ON cn.ENTDCDGO = mn.ENTDCDGO AND cn.TPAPCDGO = mn.TPAPCDGO
WHERE   mn.FILAS > NVL(cn.CUPOS, 0)
GROUP BY CASE WHEN NVL(cn.CUPOS, 0) = 0 THEN 'sin cupos'
              ELSE TO_CHAR(LEAST(mn.FILAS - cn.CUPOS, 6)) END
ORDER BY 1;


-- =============================================================================
-- 4. LOS 15 CASOS MAS GRANDES — la unica lista del script, y es corta
-- =============================================================================
WITH PARAM AS (
        SELECT  DATE '2025-06-01' AS PISO,
                (SELECT MAX(TO_DATE(TO_CHAR(c.CRARANAF) || LPAD(TO_CHAR(c.CRARMSAF), 2, '0'), 'YYYYMM'))
                   FROM CRD.CRAR c WHERE c.CRARESTD = 3) AS TECHO
        FROM DUAL
),
MESES AS (
        SELECT ADD_MONTHS(p.PISO, LEVEL - 1) AS MES FROM PARAM p
        CONNECT BY LEVEL <= MONTHS_BETWEEN(p.TECHO, p.PISO) + 1
),
APORTES AS (
        SELECT  a.ENTDCDGO, a.TPAPCDGO, a.APRTVLRR,
                CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV ELSE TRUNC(a.APRTFCTR, 'MM') END AS PERIODO_EFECTIVO,
                CASE WHEN (    a.APRTFCTR >= DATE '2025-06-01'
                           AND (   a.CRARCDGO IS NOT NULL
                                OR EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
                                OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                                OR a.APRTGLSA LIKE 'Abono al aporte%'))
                     THEN 'MOVIL' ELSE 'FIJA' END AS CLASE
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0 AND NVL(a.APRTTPMV, 1) <> 8
),
CONTRATO_ACTIVO AS (
        SELECT c.ENTDCDGO, MAX(c.CNTRCDGO) AS CNTRCDGO FROM CRD.CNTR c WHERE c.CNTRESTD = 1 GROUP BY c.ENTDCDGO
),
ESPERADO AS (
        SELECT  DISTINCT ca.ENTDCDGO, v.TPAPCDGO, m.MES
        FROM    MESES m CROSS JOIN CONTRATO_ACTIVO ca
        JOIN    CRD.VGCN v ON v.CNTRCDGO = ca.CNTRCDGO
        WHERE   v.TPAPCDGO IN (9, 11) AND v.VGCNIDST = 1 AND NVL(v.VGCNMNTO, 0) > 0
        AND     v.VGCNFCIN <= LAST_DAY(m.MES)
        AND     (v.VGCNFCFN IS NULL OR v.VGCNFCFN >= LAST_DAY(m.MES))
),
CUPOS_N AS (
        SELECT e.ENTDCDGO, e.TPAPCDGO, COUNT(*) AS CUPOS FROM ESPERADO e GROUP BY e.ENTDCDGO, e.TPAPCDGO
),
MOVILES_N AS (
        SELECT  a.ENTDCDGO, a.TPAPCDGO, COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS VALOR,
                MIN(a.PERIODO_EFECTIVO) AS DESDE, MAX(a.PERIODO_EFECTIVO) AS HASTA
        FROM    APORTES a CROSS JOIN PARAM p
        WHERE   a.CLASE = 'MOVIL' AND a.PERIODO_EFECTIVO BETWEEN p.PISO AND p.TECHO
        GROUP BY a.ENTDCDGO, a.TPAPCDGO
)
SELECT * FROM (
    SELECT  e.ENTDNMID                              AS IDENTIFICACION,
            SUBSTR(e.ENTDRZNS, 1, 35)               AS PARTICIPE,
            mn.TPAPCDGO                             AS TIPO,
            mn.FILAS                                AS FILAS_MOVILES,
            NVL(cn.CUPOS, 0)                        AS CUPOS,
            mn.FILAS - NVL(cn.CUPOS, 0)             AS SOBRANTE,
            TO_CHAR(mn.DESDE, 'MM/YYYY')            AS DESDE,
            TO_CHAR(mn.HASTA, 'MM/YYYY')            AS HASTA,
            ROUND(mn.VALOR, 2)                      AS VALOR,
            CASE WHEN NOT EXISTS (SELECT 1 FROM CRD.CNTR c WHERE c.ENTDCDGO = mn.ENTDCDGO AND c.CNTRESTD = 1)
                 THEN 'sin contrato ACTIVO'
                 WHEN NVL(cn.CUPOS, 0) = 0 THEN 'contrato si, vigencia util no'
                 ELSE 'tiene grilla, filas de mas' END AS CAUSA
    FROM    MOVILES_N mn
    LEFT    JOIN CUPOS_N cn ON cn.ENTDCDGO = mn.ENTDCDGO AND cn.TPAPCDGO = mn.TPAPCDGO
    JOIN    CRD.ENTD e ON e.ENTDCDGO = mn.ENTDCDGO
    WHERE   mn.FILAS > NVL(cn.CUPOS, 0)
    ORDER BY mn.FILAS - NVL(cn.CUPOS, 0) DESC, mn.FILAS DESC
) WHERE ROWNUM <= 15;


-- =============================================================================
-- 5. VIGENCIAS — por que un participe puede no tener cupos
-- =============================================================================
-- Reparte TODAS las vigencias de tipos 9/11 segun por que servirian o no para
-- construir la grilla de meses. Si "monto en cero o nulo" es grande, el criterio
-- VGCNMNTO > 0 es el que esta recortando los cupos y hay que revisarlo: puede que
-- el esperado de esos participes salga del porcentaje (VGCNPRCN) y no del monto.
-- =============================================================================
SELECT  CASE WHEN c.CNTRESTD <> 1              THEN '1. contrato no ACTIVO'
             WHEN v.VGCNIDST <> 1              THEN '2. vigencia no activa'
             WHEN NVL(v.VGCNMNTO, 0) <= 0
                  AND NVL(v.VGCNPRCN, 0) > 0   THEN '3. monto en cero PERO con porcentaje — REVISAR'
             WHEN NVL(v.VGCNMNTO, 0) <= 0      THEN '4. monto en cero o nulo, sin porcentaje'
             ELSE                                   '5. sirve para la grilla'
        END                                     AS SITUACION,
        v.TPAPCDGO                              AS TIPO,
        COUNT(*)                                AS VIGENCIAS,
        COUNT(DISTINCT c.ENTDCDGO)              AS PARTICIPES,
        MIN(v.VGCNFCIN)                         AS MIN_INICIO,
        MAX(NVL(v.VGCNFCFN, DATE '2099-12-31')) AS MAX_FIN,
        SUM(CASE WHEN v.VGCNFCFN IS NULL THEN 1 ELSE 0 END) AS ABIERTAS
FROM    CRD.VGCN v
JOIN    CRD.CNTR c ON c.CNTRCDGO = v.CNTRCDGO
WHERE   v.TPAPCDGO IN (9, 11)
GROUP BY CASE WHEN c.CNTRESTD <> 1              THEN '1. contrato no ACTIVO'
              WHEN v.VGCNIDST <> 1              THEN '2. vigencia no activa'
              WHEN NVL(v.VGCNMNTO, 0) <= 0
                   AND NVL(v.VGCNPRCN, 0) > 0   THEN '3. monto en cero PERO con porcentaje — REVISAR'
              WHEN NVL(v.VGCNMNTO, 0) <= 0      THEN '4. monto en cero o nulo, sin porcentaje'
              ELSE                                   '5. sirve para la grilla'
         END, v.TPAPCDGO
ORDER BY 1, 2;


-- =============================================================================
-- FIN. Nada de este script modifica datos.
-- =============================================================================

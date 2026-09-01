-- =============================================================================
-- 10 — LAS 8 FILAS QUE EL CONTROL 3.3b DEJO EN "REVISAR"
-- =============================================================================
-- FECHA: 2026-08-31 · Equipo omen-saa-2
--
-- ⛔ SOLO LECTURA. Sin DML. Pocas filas a proposito.
-- ✅ SE PUEDE CORRER CON LA TRANSACCION DEL 08 ABIERTA O YA COMMITEADA: no
--    escribe nada y ve el estado actual de la sesion.
--
-- -----------------------------------------------------------------------------
-- QUE PASO
-- -----------------------------------------------------------------------------
--   El 08 corrio y su control 3.3b devolvio 8 filas todavia con devengo bajo el
--   piso, rotuladas "REVISAR — no encaja en ninguna exclusion declarada":
--
--     BRITO MALDONADO ANGEL EDUARDO    tipo  9   991147   $79,85   05/2025
--     MUÑOZ VILLALTA JUAN ELIAS        tipo 11   992646  $136,80   05/2025
--     SUAREZ BUSTOS ABRAHAM            tipo  9  1000100  $129,95   05/2025
--     ERAZO ROMAN CARLOS ANDRES        tipo 11   991664   $50,75   05/2025
--     AGUILAR VALENCIA VICTOR HUGO     tipo 11  1019450  $149,15   05/2025
--     AGUILAR VALENCIA VICTOR HUGO     tipo 11  1019449  $149,15   04/2025
--     AGUILAR VALENCIA VICTOR HUGO     tipo 11  1019448  $149,15   03/2025
--     CAIZA GAVILANES BAYRUN MARCELO   tipo 11  1003053   $47,50   05/2025
--
--   Son 6 participes. Tienen contrato ACTIVO y vigencia util del tipo (por eso no
--   cayeron en ninguna de las dos exclusiones declaradas), asi que SI tenian
--   cupos. El MERGE igual no las movio.
--
-- -----------------------------------------------------------------------------
-- LA HIPOTESIS, Y POR QUE CAMBIARIA LA CONCLUSION
-- -----------------------------------------------------------------------------
--   La explicacion mas probable es SOBRANTE: el participe tiene mas filas moviles
--   que meses esperados en la ventana 2025-06 .. 2026-07. El MERGE asigna fila i
--   al cupo i; las que sobran no reciben cupo y se quedan donde estaban.
--
--   Y si es eso, ESTAS 8 FILAS NO SON UN PROBLEMA:
--     · Si TODOS los meses en alcance del participe quedaron cubiertos, entonces
--       no hay ningun mes que se le pueda volver a cobrar. El dinero de estas
--       filas es pago de meses ANTERIORES a 2025-06 — atrasos de la epoca previa
--       al devengo—, y esos meses el motor no los mira nunca
--       (ALCANCE_MINIMO_DEVENGO). No hay doble cobro que prevenir.
--     · O sea que el riesgo que motivo todo el frente (854 filas invisibles = un
--       mes en alcance sin cubrir) NO aplica a estas.
--
--   ⚠ PERO SI ALGUN MES EN ALCANCE QUEDO VACIO mientras estas filas siguen abajo
--     del piso, entonces si hay problema: habia donde ponerlas y no se pusieron.
--     Eso seria un defecto del algoritmo, no un sobrante.
--
--   El bloque 2 distingue los dos casos. Es la unica pregunta que importa.
--
-- INDICE
--   1  Los 6 participes: filas moviles contra cupos
--   2  ⛔ LA PREGUNTA: ¿les quedo algun mes EN ALCANCE sin cubrir?
--   3  El detalle mes a mes de los 6, para mirarlo con los ojos
-- =============================================================================


-- =============================================================================
-- 1. LOS 6 PARTICIPES: FILAS MOVILES CONTRA CUPOS
-- =============================================================================
-- Si FILAS > CUPOS, la explicacion es sobrante y se confirma la hipotesis.
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
AFECTADOS AS (
        SELECT DISTINCT a.ENTDCDGO, a.TPAPCDGO
        FROM   CRD.APRT a
        WHERE  a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
        AND    a.APRTPRDV IS NOT NULL AND a.APRTPRDV < DATE '2025-06-01'
),
CONTRATO_ACTIVO AS (
        SELECT c.ENTDCDGO, MAX(c.CNTRCDGO) AS CNTRCDGO
        FROM   CRD.CNTR c WHERE c.CNTRESTD = 1 GROUP BY c.ENTDCDGO
),
CUPOS AS (
        SELECT  ca.ENTDCDGO, v.TPAPCDGO, COUNT(DISTINCT m.MES) AS CUPOS
        FROM    MESES m
        CROSS   JOIN CONTRATO_ACTIVO ca
        JOIN    CRD.VGCN v ON v.CNTRCDGO = ca.CNTRCDGO
        WHERE   v.TPAPCDGO IN (9, 11) AND v.VGCNIDST = 1 AND NVL(v.VGCNMNTO, 0) > 0
        AND     v.VGCNFCIN <= LAST_DAY(m.MES)
        AND     (v.VGCNFCFN IS NULL OR v.VGCNFCFN >= LAST_DAY(m.MES))
        GROUP BY ca.ENTDCDGO, v.TPAPCDGO
),
MOVILES AS (
        SELECT  a.ENTDCDGO, a.TPAPCDGO, COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS VALOR,
                SUM(CASE WHEN a.APRTPRDV < DATE '2025-06-01' THEN 1 ELSE 0 END) AS BAJO_PISO
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
        AND     a.APRTFCTR >= DATE '2025-06-01'
        AND     (   a.CRARCDGO IS NOT NULL
                 OR EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
                 OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                 OR a.APRTGLSA LIKE 'Abono al aporte%')
        GROUP BY a.ENTDCDGO, a.TPAPCDGO
)
SELECT  e.ENTDNMID                              AS IDENTIFICACION,
        SUBSTR(e.ENTDRZNS, 1, 32)               AS PARTICIPE,
        af.TPAPCDGO                             AS TIPO,
        mv.FILAS                                AS FILAS_MOVILES,
        NVL(cu.CUPOS, 0)                        AS CUPOS,
        mv.FILAS - NVL(cu.CUPOS, 0)             AS SOBRANTE,
        mv.BAJO_PISO                            AS FILAS_BAJO_PISO,
        ROUND(mv.VALOR, 2)                      AS VALOR,
        CASE WHEN mv.FILAS > NVL(cu.CUPOS, 0)
             THEN 'SOBRANTE — mas filas que meses esperados'
             ELSE 'NO ES SOBRANTE — REVISAR EL ALGORITMO' END AS DIAGNOSTICO
FROM    AFECTADOS af
JOIN    MOVILES mv ON mv.ENTDCDGO = af.ENTDCDGO AND mv.TPAPCDGO = af.TPAPCDGO
LEFT    JOIN CUPOS cu ON cu.ENTDCDGO = af.ENTDCDGO AND cu.TPAPCDGO = af.TPAPCDGO
JOIN    CRD.ENTD e ON e.ENTDCDGO = af.ENTDCDGO
ORDER BY 9, 6 DESC;


-- =============================================================================
-- 2. ⛔ LA PREGUNTA QUE DECIDE: ¿QUEDO ALGUN MES EN ALCANCE SIN CUBRIR?
-- =============================================================================
-- Para los mismos 6 participes: meses esperados que NO tienen ninguna fila.
--
--   0 filas  ->  todos sus meses en alcance estan cubiertos. Las 8 filas de abajo
--                del piso son pago de atrasos anteriores a 2025-06, que el motor
--                no cobra nunca. NO HAY DOBLE COBRO. Son benignas y se declaran
--                como tercera exclusion: "sobrante sin mes en alcance libre".
--
--   > 0      ->  habia donde ponerlas y no se pusieron. Eso es un defecto del
--                algoritmo del 08, no un sobrante: NO COMMITEAR y avisar.
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
AFECTADOS AS (
        SELECT DISTINCT a.ENTDCDGO, a.TPAPCDGO
        FROM   CRD.APRT a
        WHERE  a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
        AND    a.APRTPRDV IS NOT NULL AND a.APRTPRDV < DATE '2025-06-01'
),
CONTRATO_ACTIVO AS (
        SELECT c.ENTDCDGO, MAX(c.CNTRCDGO) AS CNTRCDGO
        FROM   CRD.CNTR c WHERE c.CNTRESTD = 1 GROUP BY c.ENTDCDGO
),
ESPERADO AS (
        SELECT  DISTINCT ca.ENTDCDGO, v.TPAPCDGO, m.MES
        FROM    MESES m
        CROSS   JOIN CONTRATO_ACTIVO ca
        JOIN    CRD.VGCN v ON v.CNTRCDGO = ca.CNTRCDGO
        WHERE   v.TPAPCDGO IN (9, 11) AND v.VGCNIDST = 1 AND NVL(v.VGCNMNTO, 0) > 0
        AND     v.VGCNFCIN <= LAST_DAY(m.MES)
        AND     (v.VGCNFCFN IS NULL OR v.VGCNFCFN >= LAST_DAY(m.MES))
)
SELECT  e.ENTDNMID                      AS IDENTIFICACION,
        SUBSTR(e.ENTDRZNS, 1, 32)       AS PARTICIPE,
        esp.TPAPCDGO                    AS TIPO,
        TO_CHAR(esp.MES, 'MM/YYYY')     AS MES_EN_ALCANCE_SIN_CUBRIR
FROM    ESPERADO esp
JOIN    AFECTADOS af ON af.ENTDCDGO = esp.ENTDCDGO AND af.TPAPCDGO = esp.TPAPCDGO
JOIN    CRD.ENTD e   ON e.ENTDCDGO = esp.ENTDCDGO
WHERE   NOT EXISTS (
            SELECT 1 FROM CRD.APRT a
            WHERE  a.ENTDCDGO = esp.ENTDCDGO
            AND    a.TPAPCDGO = esp.TPAPCDGO
            AND    a.APRTVLRR > 0
            AND    COALESCE(a.APRTPRDV, TRUNC(a.APRTFCTR, 'MM')) = esp.MES )
ORDER BY 1, 3, 4;


-- =============================================================================
-- 3. EL DETALLE MES A MES DE LOS 6 — para mirarlo con los ojos
-- =============================================================================
SELECT  e.ENTDNMID                                  AS IDENTIFICACION,
        SUBSTR(e.ENTDRZNS, 1, 28)                   AS PARTICIPE,
        a.TPAPCDGO                                  AS TIPO,
        a.APRTCDGO                                  AS ID_APORTE,
        ROUND(a.APRTVLRR, 2)                        AS VALOR,
        TO_CHAR(TRUNC(a.APRTFCTR, 'MM'), 'MM/YYYY') AS MES_CAJA,
        TO_CHAR(a.APRTPRDV, 'MM/YYYY')              AS DEVENGO,
        CASE WHEN a.APRTGLSA LIKE '%(reubicado desde%' THEN 'SI' ELSE 'no' END AS REUBICADA,
        SUBSTR(a.APRTGLSA, 1, 55)                   AS GLOSA
FROM    CRD.APRT a
JOIN    CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
WHERE   a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
AND     a.APRTFCTR >= DATE '2025-06-01'
AND     a.ENTDCDGO IN ( SELECT DISTINCT b.ENTDCDGO FROM CRD.APRT b
                        WHERE  b.TPAPCDGO IN (9, 11) AND b.APRTVLRR > 0
                        AND    b.APRTPRDV IS NOT NULL AND b.APRTPRDV < DATE '2025-06-01' )
ORDER BY e.ENTDNMID, a.TPAPCDGO, a.APRTPRDV;


-- =============================================================================
-- FIN. Nada de este script modifica datos.
-- =============================================================================

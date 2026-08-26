-- =============================================================================
-- 59 - MESES SIN APORTE DE LOS PARTICIPES QUE APARECEN EN LOS DUPLICADOS
-- =============================================================================
--
-- OBJETIVO
--   Tomar los participes que salieron en 58_APORTES_DUPLICADOS_MES.sql y, para
--   cada uno y cada tipo de aporte, decir en que meses del rango NO hubo
--   aporte, cuantos son y cuales son. Y, del otro lado, aislar los que tienen
--   duplicados pero NINGUN mes faltante.
--
-- SOLO LECTURA. Ningun DML.
--
-- -----------------------------------------------------------------------------
-- PARAMETROS FIJOS DE ESTE SCRIPT
-- -----------------------------------------------------------------------------
--   RANGO DE ANALISIS : 2025-06  ..  2026-07   (14 meses, ambos inclusive)
--                       El corte superior es fijo, NO es SYSDATE.
--                       Aparece de dos formas y las dos hay que cambiar juntas
--                       si se mueve el rango:
--                         - MONTHS_BETWEEN(DATE '2026-07-01', DATE '2025-06-01') + 1
--                         - a.APRTFCTR < DATE '2026-08-01'   (primer dia del mes
--                           siguiente al ultimo mes del rango)
--
--   TIPOS DE APORTE   : 9 y 11 unicamente.
--                       OJO CON LAS ETIQUETAS: segun las constantes de
--                       CargaArchivoPetroServiceImpl:145-146 y
--                       docs/logica-negocio/petro/REGLAS-GENERALES-PETRO.md:135,
--                           9  = JUBILACION
--                           11 = CESANTIA
--                       (es al reves de como suelen nombrarse). El conjunto es
--                       el mismo, pero al leer el resultado hay que fiarse de la
--                       columna TIPO_APORTE, que trae el nombre real de CRD.TPAP.
--
-- -----------------------------------------------------------------------------
-- COMO SE DEFINE EL UNIVERSO
-- -----------------------------------------------------------------------------
--   El CTE DUPLICADOS repite el criterio del script 58:
--     fecha de transaccion = ultimo dia del mes, usuario SAA_AH, tipo 9 u 11,
--     y mas de una fila para el mismo (entidad, tipo de aporte, dia).
--   De ahi salen pares DISTINCT (entidad, tipo de aporte). El analisis se hace
--   por PAR, no por entidad: un participe puede tener la cesantia completa y el
--   hueco justo en el tipo que se duplico.
--
--   NOTA: el universo de duplicados arranca en 2025-05-31 (igual que el 58),
--   mientras que el analisis de meses faltantes arranca en 2025-06. Es
--   deliberado: un duplicado de mayo hace entrar al participe al analisis, pero
--   mayo no se evalua como mes faltante.
--
-- -----------------------------------------------------------------------------
-- QUE CUENTA COMO "SI APORTO" EN UN MES
-- -----------------------------------------------------------------------------
--   Cualquier fila de CRD.APRT de ese participe y ese tipo cuya APRTFCTR caiga
--   dentro del mes, SIN importar el usuario ni el dia. Es deliberado: si el
--   aporte se registro a mano desde la pantalla, el mes esta cubierto igual.
--   Se excluyen, en cambio, las filas que no son aportes del mes:
--     - APRTVLRR <= 0  -> la fila NEGATIVA que inserta la devolucion de aportes
--                         (DevolucionAporteServiceImpl:412, valor = -valor).
--     - glosa 'REVERSO DEVOLUCION%' -> el reverso inserta una fila POSITIVA
--                         fechada con LocalDateTime.now() (:869), no con el mes
--                         al que corresponde. Sin este filtro, un reverso
--                         reciente marcaria un mes como aportado sin serlo.
--
-- -----------------------------------------------------------------------------
-- COMO LEER EL RESULTADO - LOS TRES TIPOS DE HUECO NO SIGNIFICAN LO MISMO
-- -----------------------------------------------------------------------------
--   1) HUECO INTERNO (MESES_SIN_APORTE_INTERNOS > 0)
--      Falta un mes que esta ENTRE el primer y el ultimo mes con aporte del
--      participe. Este es el hallazgo real: estaba aportando antes y despues.
--
--   2) HUECO DE BORDE AL INICIO
--      No hay aportes desde junio-2025 hasta cierto mes y despues si. Suele ser
--      un participe que ingreso despues, o un tipo de aporte que empezo a
--      descontarse despues. No necesariamente es un defecto.
--
--   3) HUECO DE BORDE AL FINAL
--      Aporto hasta cierto mes y despues nada. Contrastar con ESTADO_PARTICIPE:
--      un CESANTE DESAFILIADO o un CESANTE FALLECIDO deja de aportar y es
--      correcto. CRD.ENTD no guarda fecha de salida, asi que el estado es lo
--      unico que hay para explicarlo.
--
-- -----------------------------------------------------------------------------
-- ADVERTENCIA SOBRE EL CATALOGO DE ESTADOS
-- -----------------------------------------------------------------------------
--   ESTADO_PARTICIPE se resuelve con  esp.ESPRCDEX = e.ENTDIDST  (codigo
--   alterno), que es el modelo POSTERIOR a la migracion documentada en
--   docs/logica-negocio/crd/MIGRACION-ESTADO-PARTICIPE.md. Si la columna sale
--   en blanco para todos, la migracion no se ejecuto: cambiar ESPRCDEX por
--   ESPRCDGO en el JOIN.
--
-- -----------------------------------------------------------------------------
-- INDICE
-- -----------------------------------------------------------------------------
--   CONSULTA 0 : control previo - aportes por mes y tipo en todo el sistema
--   CONSULTA 1 : CON huecos - un renglon por participe + tipo
--   CONSULTA 2 : CON huecos - un renglon por mes faltante (para exportar)
--   CONSULTA 3 : CON huecos - resumen por mes (cuantos participes por periodo)
--   CONSULTA 4 : SIN huecos - un renglon por participe + tipo, con los meses
--                que si tienen duplicado
--   CONSULTA 5 : SIN huecos - las filas de CRD.APRT de los meses duplicados
--   VARIANTES
-- =============================================================================


-- =============================================================================
-- CONSULTA 0 - CONTROL PREVIO: aportes por mes y tipo en TODO el sistema
-- =============================================================================
-- Correr esta primero. Detecta un mes que le falta a todo el mundo, que seria
-- una carga que nunca corrio y no un problema de participes sueltos. Deben
-- aparecer los 14 meses del rango por cada tipo; si falta alguno, todo lo que
-- salga en las consultas 1-3 para ese mes es consecuencia de eso.
-- =============================================================================
SELECT  TO_CHAR(TRUNC(a.APRTFCTR, 'MM'), 'YYYY-MM')      AS PERIODO,
        a.TPAPCDGO                                        AS ID_TIPO_APORTE,
        t.TPAPNMBR                                        AS TIPO_APORTE,
        COUNT(*)                                          AS FILAS_APRT,
        COUNT(DISTINCT a.ENTDCDGO)                        AS PARTICIPES,
        COUNT(DISTINCT a.APRTIDAS)                        AS CARGAS_DISTINTAS,
        SUM(a.APRTVLRR)                                   AS VALOR_TOTAL
FROM    CRD.APRT a
JOIN    CRD.TPAP t ON t.TPAPCDGO = a.TPAPCDGO
WHERE   a.APRTFCTR >= DATE '2025-06-01'
AND     a.APRTFCTR <  DATE '2026-08-01'
AND     a.TPAPCDGO IN (9, 11)
AND     a.APRTVLRR > 0
AND     (a.APRTGLSA IS NULL OR a.APRTGLSA NOT LIKE 'REVERSO DEVOLUCION%')
GROUP BY TRUNC(a.APRTFCTR, 'MM'), a.TPAPCDGO, t.TPAPNMBR
ORDER BY 1, 2;


-- =============================================================================
-- CONSULTA 1 - CON HUECOS: un renglon por participe + tipo de aporte
--              con cuantos y cuales meses no aporto
-- =============================================================================
WITH DUPLICADOS AS (
        SELECT  DISTINCT d.ENTDCDGO, d.TPAPCDGO
        FROM  ( SELECT  a.ENTDCDGO,
                        a.TPAPCDGO,
                        COUNT(*) OVER (PARTITION BY a.ENTDCDGO,
                                                    a.TPAPCDGO,
                                                    TRUNC(a.APRTFCTR)) AS VECES
                FROM    CRD.APRT a
                WHERE   a.APRTFCTR >= DATE '2025-05-31'
                AND     a.APRTFCTR <  DATE '2026-08-01'
                AND     TRUNC(a.APRTFCTR) = LAST_DAY(TRUNC(a.APRTFCTR))
                AND     a.APRTUSRG = 'SAA_AH'
                AND     a.TPAPCDGO IN (9, 11) ) d
        WHERE   d.VECES > 1
),
MESES AS (
        SELECT  ADD_MONTHS(DATE '2025-06-01', LEVEL - 1) AS MES
        FROM    DUAL
        CONNECT BY LEVEL <= MONTHS_BETWEEN(DATE '2026-07-01', DATE '2025-06-01') + 1
),
APORTADO AS (
        SELECT  a.ENTDCDGO,
                a.TPAPCDGO,
                TRUNC(a.APRTFCTR, 'MM') AS MES
        FROM    CRD.APRT a
        WHERE   a.APRTFCTR >= DATE '2025-06-01'
        AND     a.APRTFCTR <  DATE '2026-08-01'
        AND     a.TPAPCDGO IN (9, 11)
        AND     a.APRTVLRR > 0
        AND     (a.APRTGLSA IS NULL OR a.APRTGLSA NOT LIKE 'REVERSO DEVOLUCION%')
        GROUP BY a.ENTDCDGO, a.TPAPCDGO, TRUNC(a.APRTFCTR, 'MM')
),
DETALLE AS (
        SELECT  e.ENTDCDGO,
                e.TPAPCDGO,
                m.MES,
                CASE WHEN ap.MES IS NULL THEN 0 ELSE 1 END AS APORTO
        FROM    DUPLICADOS e
        CROSS   JOIN MESES m
        LEFT    JOIN APORTADO ap
                ON  ap.ENTDCDGO = e.ENTDCDGO
                AND ap.TPAPCDGO = e.TPAPCDGO
                AND ap.MES      = m.MES
),
MARCADO AS (
        SELECT  d.ENTDCDGO,
                d.TPAPCDGO,
                d.MES,
                d.APORTO,
                MIN(CASE WHEN d.APORTO = 1 THEN d.MES END)
                    OVER (PARTITION BY d.ENTDCDGO, d.TPAPCDGO) AS PRIMER_MES,
                MAX(CASE WHEN d.APORTO = 1 THEN d.MES END)
                    OVER (PARTITION BY d.ENTDCDGO, d.TPAPCDGO) AS ULTIMO_MES
        FROM    DETALLE d
)
SELECT  e.ENTDNMID                                        AS NUMERO_IDENTIFICACION,
        e.ENTDRZNS                                        AS RAZON_SOCIAL,
        NVL(esp.ESPRNMBR, TO_CHAR(e.ENTDIDST))            AS ESTADO_PARTICIPE,
        t.TPAPNMBR                                        AS TIPO_APORTE,
        COUNT(*)                                          AS MESES_EN_RANGO,
        SUM(m.APORTO)                                     AS MESES_CON_APORTE,
        SUM(1 - m.APORTO)                                 AS MESES_SIN_APORTE,
        SUM(CASE WHEN m.APORTO = 0
                  AND m.MES > m.PRIMER_MES
                  AND m.MES < m.ULTIMO_MES
                 THEN 1 ELSE 0 END)                       AS MESES_SIN_APORTE_INTERNOS,
        LISTAGG(CASE WHEN m.APORTO = 0
                     THEN TO_CHAR(m.MES, 'YYYY-MM') END, ', ')
                WITHIN GROUP (ORDER BY m.MES)             AS MESES_SIN_APORTE_LISTA,
        LISTAGG(CASE WHEN m.APORTO = 0
                      AND m.MES > m.PRIMER_MES
                      AND m.MES < m.ULTIMO_MES
                     THEN TO_CHAR(m.MES, 'YYYY-MM') END, ', ')
                WITHIN GROUP (ORDER BY m.MES)             AS MESES_INTERNOS_LISTA,
        TO_CHAR(MIN(m.PRIMER_MES), 'YYYY-MM')             AS PRIMER_MES_CON_APORTE,
        TO_CHAR(MAX(m.ULTIMO_MES), 'YYYY-MM')             AS ULTIMO_MES_CON_APORTE,
        m.ENTDCDGO                                        AS ID_ENTIDAD,
        m.TPAPCDGO                                        AS ID_TIPO_APORTE
FROM    MARCADO m
JOIN    CRD.ENTD e   ON e.ENTDCDGO = m.ENTDCDGO
JOIN    CRD.TPAP t   ON t.TPAPCDGO = m.TPAPCDGO
LEFT    JOIN CRD.ESPR esp ON esp.ESPRCDEX = e.ENTDIDST
GROUP BY m.ENTDCDGO, m.TPAPCDGO, e.ENTDNMID, e.ENTDRZNS,
         e.ENTDIDST, esp.ESPRNMBR, t.TPAPNMBR
HAVING  SUM(1 - m.APORTO) > 0
ORDER BY MESES_SIN_APORTE_INTERNOS DESC,
         MESES_SIN_APORTE DESC,
         NUMERO_IDENTIFICACION,
         TIPO_APORTE;


-- =============================================================================
-- CONSULTA 2 - CON HUECOS, DETALLE: un renglon por cada mes faltante
-- =============================================================================
-- Misma informacion que la CONSULTA 1 pero desnormalizada: una fila por
-- (participe, tipo, mes sin aporte). Util para pegar en Excel y filtrar.
-- La columna TIPO_HUECO clasifica cada mes segun las tres categorias del
-- encabezado.
-- =============================================================================
WITH DUPLICADOS AS (
        SELECT  DISTINCT d.ENTDCDGO, d.TPAPCDGO
        FROM  ( SELECT  a.ENTDCDGO,
                        a.TPAPCDGO,
                        COUNT(*) OVER (PARTITION BY a.ENTDCDGO,
                                                    a.TPAPCDGO,
                                                    TRUNC(a.APRTFCTR)) AS VECES
                FROM    CRD.APRT a
                WHERE   a.APRTFCTR >= DATE '2025-05-31'
                AND     a.APRTFCTR <  DATE '2026-08-01'
                AND     TRUNC(a.APRTFCTR) = LAST_DAY(TRUNC(a.APRTFCTR))
                AND     a.APRTUSRG = 'SAA_AH'
                AND     a.TPAPCDGO IN (9, 11) ) d
        WHERE   d.VECES > 1
),
MESES AS (
        SELECT  ADD_MONTHS(DATE '2025-06-01', LEVEL - 1) AS MES
        FROM    DUAL
        CONNECT BY LEVEL <= MONTHS_BETWEEN(DATE '2026-07-01', DATE '2025-06-01') + 1
),
APORTADO AS (
        SELECT  a.ENTDCDGO,
                a.TPAPCDGO,
                TRUNC(a.APRTFCTR, 'MM') AS MES
        FROM    CRD.APRT a
        WHERE   a.APRTFCTR >= DATE '2025-06-01'
        AND     a.APRTFCTR <  DATE '2026-08-01'
        AND     a.TPAPCDGO IN (9, 11)
        AND     a.APRTVLRR > 0
        AND     (a.APRTGLSA IS NULL OR a.APRTGLSA NOT LIKE 'REVERSO DEVOLUCION%')
        GROUP BY a.ENTDCDGO, a.TPAPCDGO, TRUNC(a.APRTFCTR, 'MM')
),
MARCADO AS (
        SELECT  p.ENTDCDGO,
                p.TPAPCDGO,
                m.MES,
                CASE WHEN ap.MES IS NULL THEN 0 ELSE 1 END AS APORTO,
                MIN(CASE WHEN ap.MES IS NOT NULL THEN m.MES END)
                    OVER (PARTITION BY p.ENTDCDGO, p.TPAPCDGO) AS PRIMER_MES,
                MAX(CASE WHEN ap.MES IS NOT NULL THEN m.MES END)
                    OVER (PARTITION BY p.ENTDCDGO, p.TPAPCDGO) AS ULTIMO_MES
        FROM    DUPLICADOS p
        CROSS   JOIN MESES m
        LEFT    JOIN APORTADO ap
                ON  ap.ENTDCDGO = p.ENTDCDGO
                AND ap.TPAPCDGO = p.TPAPCDGO
                AND ap.MES      = m.MES
)
SELECT  e.ENTDNMID                              AS NUMERO_IDENTIFICACION,
        e.ENTDRZNS                              AS RAZON_SOCIAL,
        NVL(esp.ESPRNMBR, TO_CHAR(e.ENTDIDST))  AS ESTADO_PARTICIPE,
        t.TPAPNMBR                              AS TIPO_APORTE,
        TO_CHAR(m.MES, 'YYYY-MM')               AS MES_SIN_APORTE,
        CASE
            WHEN m.PRIMER_MES IS NULL   THEN 'SIN NINGUN APORTE EN EL RANGO'
            WHEN m.MES < m.PRIMER_MES   THEN 'BORDE INICIAL'
            WHEN m.MES > m.ULTIMO_MES   THEN 'BORDE FINAL'
            ELSE                             'HUECO INTERNO'
        END                                     AS TIPO_HUECO,
        TO_CHAR(m.PRIMER_MES, 'YYYY-MM')        AS PRIMER_MES_CON_APORTE,
        TO_CHAR(m.ULTIMO_MES, 'YYYY-MM')        AS ULTIMO_MES_CON_APORTE,
        m.ENTDCDGO                              AS ID_ENTIDAD,
        m.TPAPCDGO                              AS ID_TIPO_APORTE
FROM    MARCADO m
JOIN    CRD.ENTD e   ON e.ENTDCDGO = m.ENTDCDGO
JOIN    CRD.TPAP t   ON t.TPAPCDGO = m.TPAPCDGO
LEFT    JOIN CRD.ESPR esp ON esp.ESPRCDEX = e.ENTDIDST
WHERE   m.APORTO = 0
ORDER BY NUMERO_IDENTIFICACION, TIPO_APORTE, m.MES;


-- =============================================================================
-- CONSULTA 3 - CON HUECOS, RESUMEN POR MES: a cuantos les falta cada mes
-- =============================================================================
-- El eje contrario. Si un mes concentra a casi todos los participes del
-- universo, no es un problema individual: es una carga que no corrio o que
-- corrio incompleta. Contrastar con la CONSULTA 0.
-- =============================================================================
WITH DUPLICADOS AS (
        SELECT  DISTINCT d.ENTDCDGO, d.TPAPCDGO
        FROM  ( SELECT  a.ENTDCDGO,
                        a.TPAPCDGO,
                        COUNT(*) OVER (PARTITION BY a.ENTDCDGO,
                                                    a.TPAPCDGO,
                                                    TRUNC(a.APRTFCTR)) AS VECES
                FROM    CRD.APRT a
                WHERE   a.APRTFCTR >= DATE '2025-05-31'
                AND     a.APRTFCTR <  DATE '2026-08-01'
                AND     TRUNC(a.APRTFCTR) = LAST_DAY(TRUNC(a.APRTFCTR))
                AND     a.APRTUSRG = 'SAA_AH'
                AND     a.TPAPCDGO IN (9, 11) ) d
        WHERE   d.VECES > 1
),
MESES AS (
        SELECT  ADD_MONTHS(DATE '2025-06-01', LEVEL - 1) AS MES
        FROM    DUAL
        CONNECT BY LEVEL <= MONTHS_BETWEEN(DATE '2026-07-01', DATE '2025-06-01') + 1
),
APORTADO AS (
        SELECT  a.ENTDCDGO,
                a.TPAPCDGO,
                TRUNC(a.APRTFCTR, 'MM') AS MES
        FROM    CRD.APRT a
        WHERE   a.APRTFCTR >= DATE '2025-06-01'
        AND     a.APRTFCTR <  DATE '2026-08-01'
        AND     a.TPAPCDGO IN (9, 11)
        AND     a.APRTVLRR > 0
        AND     (a.APRTGLSA IS NULL OR a.APRTGLSA NOT LIKE 'REVERSO DEVOLUCION%')
        GROUP BY a.ENTDCDGO, a.TPAPCDGO, TRUNC(a.APRTFCTR, 'MM')
)
SELECT  TO_CHAR(m.MES, 'YYYY-MM')                         AS PERIODO,
        t.TPAPNMBR                                        AS TIPO_APORTE,
        COUNT(*)                                          AS PARES_ESPERADOS,
        SUM(CASE WHEN ap.MES IS NULL THEN 1 ELSE 0 END)   AS SIN_APORTE,
        SUM(CASE WHEN ap.MES IS NULL THEN 0 ELSE 1 END)   AS CON_APORTE,
        ROUND(100 * SUM(CASE WHEN ap.MES IS NULL THEN 1 ELSE 0 END)
                  / COUNT(*), 2)                          AS PCT_SIN_APORTE
FROM    DUPLICADOS p
CROSS   JOIN MESES m
JOIN    CRD.TPAP t ON t.TPAPCDGO = p.TPAPCDGO
LEFT    JOIN APORTADO ap
        ON  ap.ENTDCDGO = p.ENTDCDGO
        AND ap.TPAPCDGO = p.TPAPCDGO
        AND ap.MES      = m.MES
GROUP BY m.MES, t.TPAPNMBR
ORDER BY m.MES, t.TPAPNMBR;


-- =============================================================================
-- CONSULTA 4 - SIN HUECOS: los que tienen duplicados y NINGUN mes faltante
-- =============================================================================
-- El complemento exacto de la CONSULTA 1: mismo universo, condicion invertida
-- (MESES_SIN_APORTE = 0). Los 14 meses del rango tienen aporte Y ademas hay
-- al menos un mes con mas de una fila.
--
-- POR QUE IMPORTA LA DISTINCION
--   En estos casos el duplicado NO esta compensando un mes que falto: es
--   excedente puro. Es el grupo sobre el que tendria sentido plantear una
--   correccion, si es que llega a plantearse.
--
-- COLUMNAS QUE HAY QUE MIRAR ANTES DE CONCLUIR
--   MESES_MULTICARGA / MESES_MULTICARGA_LISTA -> meses cuyas filas vienen de
--     cargas DISTINTAS (APRTIDAS distinto). Ese es el duplicado duro: el mismo
--     mes procesado dos veces.
--   Un mes con 2 filas y UNA sola carga es, normalmente, la pareja legitima
--     aporte-del-mes + excedente-del-mes-siguiente que documenta el script 58
--     (crearAporteExcedenteMesSiguiente estampa el aporte del mes SIGUIENTE con
--     el ultimo dia del mes de CARGA). No es un duplicado.
--   EXCESO_FILAS = filas de mas respecto de una fila por mes.
-- =============================================================================
WITH DUP_MES AS (
        SELECT  a.ENTDCDGO,
                a.TPAPCDGO,
                TRUNC(a.APRTFCTR)          AS DIA,
                COUNT(*)                   AS FILAS,
                COUNT(DISTINCT a.APRTIDAS) AS CARGAS,
                SUM(a.APRTVLRR)            AS VALOR
        FROM    CRD.APRT a
        WHERE   a.APRTFCTR >= DATE '2025-05-31'
        AND     a.APRTFCTR <  DATE '2026-08-01'
        AND     TRUNC(a.APRTFCTR) = LAST_DAY(TRUNC(a.APRTFCTR))
        AND     a.APRTUSRG = 'SAA_AH'
        AND     a.TPAPCDGO IN (9, 11)
        GROUP BY a.ENTDCDGO, a.TPAPCDGO, TRUNC(a.APRTFCTR)
        HAVING  COUNT(*) > 1
),
DUPLICADOS AS (
        SELECT  DISTINCT d.ENTDCDGO, d.TPAPCDGO FROM DUP_MES d
),
MESES AS (
        SELECT  ADD_MONTHS(DATE '2025-06-01', LEVEL - 1) AS MES
        FROM    DUAL
        CONNECT BY LEVEL <= MONTHS_BETWEEN(DATE '2026-07-01', DATE '2025-06-01') + 1
),
APORTADO AS (
        SELECT  a.ENTDCDGO,
                a.TPAPCDGO,
                TRUNC(a.APRTFCTR, 'MM') AS MES
        FROM    CRD.APRT a
        WHERE   a.APRTFCTR >= DATE '2025-06-01'
        AND     a.APRTFCTR <  DATE '2026-08-01'
        AND     a.TPAPCDGO IN (9, 11)
        AND     a.APRTVLRR > 0
        AND     (a.APRTGLSA IS NULL OR a.APRTGLSA NOT LIKE 'REVERSO DEVOLUCION%')
        GROUP BY a.ENTDCDGO, a.TPAPCDGO, TRUNC(a.APRTFCTR, 'MM')
),
SIN_HUECOS AS (
        SELECT  p.ENTDCDGO,
                p.TPAPCDGO,
                COUNT(*) AS MESES_EN_RANGO
        FROM    DUPLICADOS p
        CROSS   JOIN MESES m
        LEFT    JOIN APORTADO ap
                ON  ap.ENTDCDGO = p.ENTDCDGO
                AND ap.TPAPCDGO = p.TPAPCDGO
                AND ap.MES      = m.MES
        GROUP BY p.ENTDCDGO, p.TPAPCDGO
        HAVING  SUM(CASE WHEN ap.MES IS NULL THEN 1 ELSE 0 END) = 0
)
SELECT  e.ENTDNMID                                        AS NUMERO_IDENTIFICACION,
        e.ENTDRZNS                                        AS RAZON_SOCIAL,
        NVL(esp.ESPRNMBR, TO_CHAR(e.ENTDIDST))            AS ESTADO_PARTICIPE,
        t.TPAPNMBR                                        AS TIPO_APORTE,
        s.MESES_EN_RANGO                                  AS MESES_EN_RANGO,
        s.MESES_EN_RANGO                                  AS MESES_CON_APORTE,
        0                                                 AS MESES_SIN_APORTE,
        COUNT(*)                                          AS MESES_DUPLICADOS,
        SUM(d.FILAS)                                      AS FILAS_EN_ESOS_MESES,
        SUM(d.FILAS) - COUNT(*)                           AS EXCESO_FILAS,
        SUM(CASE WHEN d.CARGAS > 1 THEN 1 ELSE 0 END)     AS MESES_MULTICARGA,
        LISTAGG(TO_CHAR(d.DIA, 'YYYY-MM') || ' (' || d.FILAS || 'f/' || d.CARGAS || 'c)', ', ')
                WITHIN GROUP (ORDER BY d.DIA)             AS MESES_DUPLICADOS_LISTA,
        LISTAGG(CASE WHEN d.CARGAS > 1
                     THEN TO_CHAR(d.DIA, 'YYYY-MM') END, ', ')
                WITHIN GROUP (ORDER BY d.DIA)             AS MESES_MULTICARGA_LISTA,
        SUM(d.VALOR)                                      AS VALOR_EN_ESOS_MESES,
        s.ENTDCDGO                                        AS ID_ENTIDAD,
        s.TPAPCDGO                                        AS ID_TIPO_APORTE
FROM    SIN_HUECOS s
JOIN    DUP_MES d    ON d.ENTDCDGO = s.ENTDCDGO AND d.TPAPCDGO = s.TPAPCDGO
JOIN    CRD.ENTD e   ON e.ENTDCDGO = s.ENTDCDGO
JOIN    CRD.TPAP t   ON t.TPAPCDGO = s.TPAPCDGO
LEFT    JOIN CRD.ESPR esp ON esp.ESPRCDEX = e.ENTDIDST
GROUP BY s.ENTDCDGO, s.TPAPCDGO, s.MESES_EN_RANGO, e.ENTDNMID, e.ENTDRZNS,
         e.ENTDIDST, esp.ESPRNMBR, t.TPAPNMBR
ORDER BY MESES_MULTICARGA DESC,
         EXCESO_FILAS DESC,
         NUMERO_IDENTIFICACION,
         TIPO_APORTE;


-- =============================================================================
-- CONSULTA 5 - SIN HUECOS, FILA A FILA: los aportes de los meses duplicados
-- =============================================================================
-- El detalle crudo de CRD.APRT que respalda la CONSULTA 4: cada una de las
-- filas involucradas en un mes duplicado, para los participes que no tienen
-- ningun mes faltante. Es lo que hay que mirar para decidir cual fila sobra.
-- La columna ORIGEN sale de la glosa, igual que en el script 58.
-- =============================================================================
WITH DUP_MES AS (
        SELECT  a.ENTDCDGO,
                a.TPAPCDGO,
                TRUNC(a.APRTFCTR)          AS DIA,
                COUNT(*)                   AS FILAS,
                COUNT(DISTINCT a.APRTIDAS) AS CARGAS
        FROM    CRD.APRT a
        WHERE   a.APRTFCTR >= DATE '2025-05-31'
        AND     a.APRTFCTR <  DATE '2026-08-01'
        AND     TRUNC(a.APRTFCTR) = LAST_DAY(TRUNC(a.APRTFCTR))
        AND     a.APRTUSRG = 'SAA_AH'
        AND     a.TPAPCDGO IN (9, 11)
        GROUP BY a.ENTDCDGO, a.TPAPCDGO, TRUNC(a.APRTFCTR)
        HAVING  COUNT(*) > 1
),
DUPLICADOS AS (
        SELECT  DISTINCT d.ENTDCDGO, d.TPAPCDGO FROM DUP_MES d
),
MESES AS (
        SELECT  ADD_MONTHS(DATE '2025-06-01', LEVEL - 1) AS MES
        FROM    DUAL
        CONNECT BY LEVEL <= MONTHS_BETWEEN(DATE '2026-07-01', DATE '2025-06-01') + 1
),
APORTADO AS (
        SELECT  a.ENTDCDGO,
                a.TPAPCDGO,
                TRUNC(a.APRTFCTR, 'MM') AS MES
        FROM    CRD.APRT a
        WHERE   a.APRTFCTR >= DATE '2025-06-01'
        AND     a.APRTFCTR <  DATE '2026-08-01'
        AND     a.TPAPCDGO IN (9, 11)
        AND     a.APRTVLRR > 0
        AND     (a.APRTGLSA IS NULL OR a.APRTGLSA NOT LIKE 'REVERSO DEVOLUCION%')
        GROUP BY a.ENTDCDGO, a.TPAPCDGO, TRUNC(a.APRTFCTR, 'MM')
),
SIN_HUECOS AS (
        SELECT  p.ENTDCDGO, p.TPAPCDGO
        FROM    DUPLICADOS p
        CROSS   JOIN MESES m
        LEFT    JOIN APORTADO ap
                ON  ap.ENTDCDGO = p.ENTDCDGO
                AND ap.TPAPCDGO = p.TPAPCDGO
                AND ap.MES      = m.MES
        GROUP BY p.ENTDCDGO, p.TPAPCDGO
        HAVING  SUM(CASE WHEN ap.MES IS NULL THEN 1 ELSE 0 END) = 0
)
SELECT  e.ENTDNMID                              AS NUMERO_IDENTIFICACION,
        e.ENTDRZNS                              AS RAZON_SOCIAL,
        t.TPAPNMBR                              AS TIPO_APORTE,
        TO_CHAR(d.DIA, 'YYYY-MM')               AS PERIODO,
        d.FILAS                                 AS FILAS_EN_EL_MES,
        d.CARGAS                                AS CARGAS_DISTINTAS,
        a.APRTCDGO                              AS ID_APORTE,
        CASE WHEN a.APRTGLSA LIKE 'Abono al aporte%'
             THEN 'EXCEDENTE MES SIGUIENTE'
             ELSE 'APORTE DEL MES'
        END                                     AS ORIGEN,
        a.APRTVLRR                              AS VALOR,
        a.APRTVLPG                              AS VALOR_PAGADO,
        a.APRTSLDO                              AS SALDO,
        CASE a.APRTIDST WHEN 1 THEN 'PENDIENTE'
                        WHEN 2 THEN 'ACTIVA'
                        WHEN 3 THEN 'EMITIDA'
                        WHEN 4 THEN 'PAGADA'
                        WHEN 5 THEN 'EN MORA'
                        WHEN 6 THEN 'PARCIAL'
                        WHEN 7 THEN 'CANCELADA ANTICIPADA'
                        WHEN 8 THEN 'VENCIDA'
                        ELSE TO_CHAR(a.APRTIDST) END AS ESTADO,
        a.APRTIDAS                              AS ID_CARGA_ARCHIVO,
        a.APRTFCRG                              AS FECHA_REGISTRO,
        a.APRTGLSA                              AS GLOSA,
        a.ENTDCDGO                              AS ID_ENTIDAD,
        a.TPAPCDGO                              AS ID_TIPO_APORTE
FROM    SIN_HUECOS s
JOIN    DUP_MES d    ON d.ENTDCDGO = s.ENTDCDGO AND d.TPAPCDGO = s.TPAPCDGO
JOIN    CRD.APRT a   ON a.ENTDCDGO = s.ENTDCDGO
                    AND a.TPAPCDGO = s.TPAPCDGO
                    AND TRUNC(a.APRTFCTR) = d.DIA
                    AND a.APRTUSRG = 'SAA_AH'
JOIN    CRD.ENTD e   ON e.ENTDCDGO = a.ENTDCDGO
JOIN    CRD.TPAP t   ON t.TPAPCDGO = a.TPAPCDGO
ORDER BY NUMERO_IDENTIFICACION, TIPO_APORTE, d.DIA, a.APRTCDGO;


-- =============================================================================
-- VARIANTES
-- =============================================================================
--
-- (a) MOVER EL RANGO
--     Hay que cambiar DOS cosas a la vez en cada consulta:
--         CONNECT BY LEVEL <= MONTHS_BETWEEN(DATE '<primer dia del ultimo mes>',
--                                            DATE '2025-06-01') + 1
--         a.APRTFCTR <  DATE '<primer dia del mes SIGUIENTE al ultimo>'
--     Hoy: ultimo mes 2026-07  ->  DATE '2026-07-01'  y  DATE '2026-08-01'.
--
-- (b) SOLO LOS HUECOS INTERNOS (el caso "no aporto solo nov-2025")
--     CONSULTA 1: cambiar el HAVING por
--         HAVING SUM(CASE WHEN m.APORTO = 0
--                          AND m.MES > m.PRIMER_MES
--                          AND m.MES < m.ULTIMO_MES
--                         THEN 1 ELSE 0 END) > 0
--     CONSULTA 2: agregar al WHERE
--         AND m.PRIMER_MES IS NOT NULL
--         AND m.MES > m.PRIMER_MES AND m.MES < m.ULTIMO_MES
--
-- (c) SOLO DUPLICADOS DUROS (mismo mes procesado por dos cargas distintas)
--     CONSULTA 4: agregar al final del SELECT
--         HAVING SUM(CASE WHEN d.CARGAS > 1 THEN 1 ELSE 0 END) > 0
--     CONSULTA 5: agregar al WHERE
--         WHERE d.CARGAS > 1
--     Asi se descarta la pareja legitima aporte + excedente del mes siguiente.
--
-- (d) SOLO PARTICIPES ACTIVOS (descarta huecos por cesantia/desafiliacion)
--     Agregar al WHERE de la consulta que corresponda:
--         AND e.ENTDIDST IN (1, 6, 8)   -- ACTIVO, JUBILADO APORTANTE, ACTIVO EN MORA
--     (codigos alternos ESPRCDEX; ver MIGRACION-ESTADO-PARTICIPE.md)
--
-- (e) UN SOLO TIPO DE APORTE
--     Cambiar  IN (9, 11)  por  = 9  (JUBILACION)  o  = 11  (CESANTIA)
--     en TODOS los CTE de la consulta, no solo en uno.
-- =============================================================================

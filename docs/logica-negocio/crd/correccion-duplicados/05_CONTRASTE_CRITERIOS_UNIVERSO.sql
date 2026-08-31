-- =============================================================================
-- 05 — CONTRASTE DE LOS CRITERIOS PARA DEFINIR "FILA DE CARGA"
-- =============================================================================
-- FECHA: 2026-08-31 · Equipo omen-saa-2
--
-- ⛔ SOLO LECTURA. Sin DML, sin bloque de reverso.
--
-- -----------------------------------------------------------------------------
-- POR QUE EXISTE
-- -----------------------------------------------------------------------------
--   Definir mal el universo ya invalido DOS analisis completos de este frente:
--     - el 61 filtro por APRTUSRG = 'SAA_AH' y no vio 2.635 filas de junio 2025;
--     - el 69 filtro por APRTIDAS IS NOT NULL y se trajo 393.869 filas migradas.
--   Las dos veces el error fue silencioso: la consulta corrio, devolvio numeros
--   y nadie tenia como saber que faltaban o sobraban filas.
--
--   Este script NO elige el criterio: los mide TODOS a la vez y muestra en que
--   se diferencian, para decidir con datos. Se corre una vez y se archiva el
--   resultado en el README.
--
-- -----------------------------------------------------------------------------
-- LOS CUATRO CRITERIOS QUE SE CONTRASTAN
-- -----------------------------------------------------------------------------
--   A  APRTIDAS existe en CRD.CRAR   (criterio propuesto por el usuario: desde
--                                     junio 2025 el idas ES un id de carga)
--   B  APRTUSRG = 'SAA_AH'           (criterio propuesto por el usuario: la carga
--                                     siempre graba ese usuario)
--   C  La glosa calza alguno de los tres patrones conocidos
--   D  CRARCDGO IS NOT NULL          (columna gobernada, solo la escribe la carga)
--
--   Mas el piso de fecha de caja >= 2025-06-01, que acota los cuatro.
--
-- -----------------------------------------------------------------------------
-- LA PREGUNTA QUE TIENE QUE RESPONDER
-- -----------------------------------------------------------------------------
--   ¿Hay filas que un criterio ve y otro no? ¿Cuantas, de que fecha, por cuanta
--   plata? Toda combinacion con FILAS > 0 y criterios en desacuerdo es una
--   decision a tomar, no un detalle.
--
--   Medicion previa que motiva esto (01 §4, corrido el 2026-08-31): las 2.637
--   filas de junio 2025 (carga 352, $160.532,74, 2.002 participes) tienen
--   APRTIDAS NULL **y** APRTUSRG NULL en 2.635 de ellas. O sea que los criterios
--   A y B, los dos, las dejan afuera. Son las mismas filas que el 74 acaba de
--   restaurar.
--
-- INDICE
--   1  Tabla de contraste: una fila por combinacion de criterios
--   2  Solo los desacuerdos, con ejemplos concretos
--   3  Que usuarios aparecen, por criterio
--   4  Control: el criterio A contra el piso de fecha (¿ids viejos que chocan?)
-- =============================================================================


-- =============================================================================
-- 1. TABLA DE CONTRASTE — una fila por combinacion de criterios
-- =============================================================================
-- Leer asi: cada fila es un grupo de aportes que responde IGUAL a los cuatro
-- criterios. Si un grupo tiene S/N mezclados y muchas filas, ahi esta el
-- desacuerdo que hay que resolver.
-- =============================================================================
SELECT  CASE WHEN EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
             THEN 'S' ELSE 'N' END                                  AS A_IDAS_ES_CARGA,
        CASE WHEN a.APRTUSRG = 'SAA_AH' THEN 'S' ELSE 'N' END       AS B_USUARIO_SAA_AH,
        CASE WHEN a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                  OR a.APRTGLSA LIKE 'Abono al aporte%'
             THEN 'S' ELSE 'N' END                                  AS C_GLOSA,
        CASE WHEN a.CRARCDGO IS NOT NULL THEN 'S' ELSE 'N' END      AS D_CRARCDGO,
        CASE WHEN a.APRTFCTR >= DATE '2025-06-01' THEN 'S' ELSE 'N' END AS PISO_JUN2025,
        COUNT(*)                                                    AS FILAS,
        COUNT(DISTINCT a.ENTDCDGO)                                  AS PARTICIPES,
        ROUND(SUM(a.APRTVLRR), 2)                                   AS VALOR,
        MIN(a.APRTFCTR)                                             AS MIN_FECHA_CAJA,
        MAX(a.APRTFCTR)                                             AS MAX_FECHA_CAJA,
        SUM(CASE WHEN a.APRTUSRG IS NULL THEN 1 ELSE 0 END)         AS SIN_USUARIO,
        SUM(CASE WHEN a.APRTPRDV IS NULL THEN 1 ELSE 0 END)         AS SIN_DEVENGO
FROM    CRD.APRT a
WHERE   a.TPAPCDGO IN (9, 11)
AND     a.APRTVLRR > 0
GROUP BY CASE WHEN EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS) THEN 'S' ELSE 'N' END,
         CASE WHEN a.APRTUSRG = 'SAA_AH' THEN 'S' ELSE 'N' END,
         CASE WHEN a.APRTGLSA LIKE 'Aporte %CargaArchivo: %' OR a.APRTGLSA LIKE 'Abono al aporte%' THEN 'S' ELSE 'N' END,
         CASE WHEN a.CRARCDGO IS NOT NULL THEN 'S' ELSE 'N' END,
         CASE WHEN a.APRTFCTR >= DATE '2025-06-01' THEN 'S' ELSE 'N' END
ORDER BY 6 DESC;


-- =============================================================================
-- 2. SOLO LOS DESACUERDOS — con tres ejemplos concretos de cada uno
-- =============================================================================
-- Un desacuerdo es una fila donde el criterio A (idas) y el criterio B (usuario)
-- NO dicen lo mismo que el criterio C (glosa), estando dentro del piso de fecha.
-- Son las filas que se ganan o se pierden segun cual se elija.
-- =============================================================================
WITH MARCADAS AS (
        SELECT  a.APRTCDGO, a.ENTDCDGO, a.TPAPCDGO, a.APRTVLRR, a.APRTGLSA,
                a.APRTUSRG, a.APRTIDAS, a.CRARCDGO, a.APRTFCTR, a.APRTPRDV,
                CASE WHEN EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS) THEN 1 ELSE 0 END AS A,
                CASE WHEN a.APRTUSRG = 'SAA_AH' THEN 1 ELSE 0 END AS B,
                CASE WHEN a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                          OR a.APRTGLSA LIKE 'Abono al aporte%' THEN 1 ELSE 0 END AS C,
                CASE WHEN a.CRARCDGO IS NOT NULL THEN 1 ELSE 0 END AS D
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (9, 11)
        AND     a.APRTVLRR > 0
        AND     a.APRTFCTR >= DATE '2025-06-01'
),
CLASIFICADAS AS (
        SELECT  m.*,
                CASE
                    WHEN C = 1 AND A = 0 AND B = 0 AND D = 0
                         THEN '1. La ve SOLO la glosa — la pierden idas Y usuario'
                    WHEN C = 1 AND A = 0 AND B = 1
                         THEN '2. Glosa y usuario si, idas no'
                    WHEN C = 1 AND A = 1 AND B = 0
                         THEN '3. Glosa e idas si, usuario no'
                    WHEN C = 0 AND (A = 1 OR B = 1 OR D = 1)
                         THEN '4. La ven idas/usuario/crarcdgo pero NO la glosa'
                    WHEN C = 1 AND A = 1 AND B = 1
                         THEN '5. Los tres de acuerdo (sin conflicto)'
                    ELSE     '6. Ningun criterio la ve'
                END AS SITUACION,
                ROW_NUMBER() OVER (PARTITION BY
                    CASE
                        WHEN C = 1 AND A = 0 AND B = 0 AND D = 0 THEN 1
                        WHEN C = 1 AND A = 0 AND B = 1 THEN 2
                        WHEN C = 1 AND A = 1 AND B = 0 THEN 3
                        WHEN C = 0 AND (A = 1 OR B = 1 OR D = 1) THEN 4
                        WHEN C = 1 AND A = 1 AND B = 1 THEN 5
                        ELSE 6 END
                    ORDER BY m.APRTCDGO) AS EJEMPLO
        FROM    MARCADAS m
)
SELECT  SITUACION,
        EJEMPLO,
        APRTCDGO                AS ID_APORTE,
        TPAPCDGO                AS TIPO,
        ROUND(APRTVLRR, 2)      AS VALOR,
        APRTFCTR                AS FECHA_CAJA,
        APRTPRDV                AS DEVENGO,
        NVL(APRTUSRG, '(null)') AS USUARIO,
        APRTIDAS,
        CRARCDGO,
        SUBSTR(APRTGLSA, 1, 90) AS GLOSA
FROM    CLASIFICADAS
WHERE   EJEMPLO <= 3
AND     SITUACION NOT LIKE '5.%'
ORDER BY SITUACION, EJEMPLO;


-- =============================================================================
-- 3. QUE USUARIOS APARECEN, POR CRITERIO
-- =============================================================================
-- Si 'SAA_AH' fuera universal en las filas de carga, la columna SIN_USUARIO y
-- OTRO_USUARIO tendrian que dar 0 en las filas que los otros criterios marcan
-- como de carga. Lo que salga distinto de 0 es lo que el filtro por usuario
-- perderia.
-- =============================================================================
SELECT  NVL(a.APRTUSRG, '(null)')                                   AS USUARIO,
        COUNT(*)                                                    AS FILAS,
        COUNT(DISTINCT a.ENTDCDGO)                                  AS PARTICIPES,
        ROUND(SUM(a.APRTVLRR), 2)                                   AS VALOR,
        MIN(a.APRTFCTR)                                             AS MIN_FECHA_CAJA,
        MAX(a.APRTFCTR)                                             AS MAX_FECHA_CAJA,
        SUM(CASE WHEN a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                      OR a.APRTGLSA LIKE 'Abono al aporte%' THEN 1 ELSE 0 END) AS CON_GLOSA_DE_CARGA,
        SUM(CASE WHEN EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
                 THEN 1 ELSE 0 END)                                 AS CON_IDAS_DE_CARGA
FROM    CRD.APRT a
WHERE   a.TPAPCDGO IN (9, 11)
AND     a.APRTVLRR > 0
AND     a.APRTFCTR >= DATE '2025-06-01'
GROUP BY NVL(a.APRTUSRG, '(null)')
ORDER BY 2 DESC;


-- =============================================================================
-- 4. CONTROL DEL CRITERIO A — ¿hay ids viejos que chocan con ids de carga?
-- =============================================================================
-- El criterio "APRTIDAS existe en CRD.CRAR" es solido SOLO si ningun id de la
-- migracion cae por casualidad dentro del rango de codigos de CRD.CRAR. Como los
-- codigos de carga llegan a ~448, cualquier fila ANTERIOR a junio 2025 cuyo
-- APRTIDAS este en ese rango es una colision: el criterio la tomaria por fila de
-- carga sin serlo. Por eso el piso de fecha no es opcional.
--
-- Si FILAS_ANTES_DE_JUN2025 > 0, el criterio A NECESITA el piso de fecha.
--
-- ⚠ CORREGIDO EL 2026-08-31: la version anterior mezclaba dos subconsultas
--   escalares con funciones de grupo en el mismo SELECT sin GROUP BY, y Oracle
--   la rechaza con ORA-00937 ("la funcion de grupo no es de grupo unico").
--   Se parte en dos consultas independientes, que ademas se leen mejor.
-- =============================================================================

-- 4a. Rango de codigos de carga que existe en la base
SELECT  MIN(cr.CRARCDGO)    AS MIN_ID_CARGA,
        MAX(cr.CRARCDGO)    AS MAX_ID_CARGA,
        COUNT(*)            AS CARGAS
FROM    CRD.CRAR cr;

-- 4b. Las colisiones, FILA POR FILA (no agregadas: se espera que sean poquisimas
--     y lo que interesa es mirarlas, no contarlas).
--     Cada fila de aca es un aporte ANTERIOR a junio 2025 cuyo APRTIDAS coincide
--     por casualidad con un codigo de CRD.CRAR. El criterio "idas es una carga"
--     las tomaria por filas de carga sin serlo: es la razon de ser del piso.
SELECT  a.APRTCDGO          AS ID_APORTE,
        a.ENTDCDGO          AS ID_ENTIDAD,
        a.TPAPCDGO          AS TIPO,
        ROUND(a.APRTVLRR, 2) AS VALOR,
        a.APRTFCTR          AS FECHA_CAJA,
        a.APRTPRDV          AS DEVENGO,
        NVL(a.APRTUSRG, '(null)') AS USUARIO,
        a.APRTIDAS,
        a.CRARCDGO,
        SUBSTR(a.APRTGLSA, 1, 80) AS GLOSA
FROM    CRD.APRT a
WHERE   a.TPAPCDGO IN (9, 11)
AND     a.APRTVLRR > 0
AND     a.APRTFCTR < DATE '2025-06-01'
AND     EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
ORDER BY a.APRTFCTR, a.APRTCDGO;


-- =============================================================================
-- FIN. Nada de este script modifica datos.
-- El resultado del bloque 1 se archiva en el README §10 y fija el universo
-- definitivo del frente. Hasta entonces, el 03 usa el criterio compuesto:
--   CRARCDGO no nulo
--   OR (fecha de caja >= 2025-06 AND (idas es una carga OR glosa de carga))
-- =============================================================================

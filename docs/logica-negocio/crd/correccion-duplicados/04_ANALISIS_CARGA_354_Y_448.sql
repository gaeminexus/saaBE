-- =============================================================================
-- 04 — LAS DOS CARGAS ANOMALAS: 354 (agosto 2025) y 448 (julio 2026)
-- =============================================================================
-- FECHA: 2026-08-31 · Equipo omen-saa-2
-- Nace de los resultados del 01, bloque 4b. Ver README §9.
--
-- ⛔ SOLO LECTURA. Sin DML, sin bloque de reverso: no hay nada que revertir.
--
-- -----------------------------------------------------------------------------
-- POR QUE ESTAS DOS Y NO LAS CATORCE
-- -----------------------------------------------------------------------------
--   El 01 §4b midio, carga por carga, lo descontado por Petro contra lo
--   registrado en CRD.APRT. Doce de las catorce cierran con una diferencia
--   NEGATIVA chica (entre -387 y -4.530, o sea: falta registrar poco). Dos se
--   salen de la escala, y en direcciones opuestas:
--
--     CARGA 354 (2025-08)  descontado 143.084,04   registrado 184.145,57
--                          filas 3.107 (las demas rondan 2.100-2.300)
--                          >>> SOBRAN 41.061,53 y ~1.000 filas <<<
--                          Procesada el 2026-04-08 14:30, o sea el dia ANTES del
--                          cambio de generador del 2026-04-09. Es el perfil exacto
--                          del mecanismo M3 (filas V1 que quedaron conviviendo con
--                          filas V3 de la misma carga) o M1 (doble ejecucion).
--
--     CARGA 448 (2026-07)  descontado 120.657,06   registrado  86.299,57
--                          >>> FALTAN 34.357,49 <<<
--                          Es la ULTIMA carga procesada (2026-08-04). Aca no sobra
--                          plata: falta registrarla. Es un problema distinto y
--                          posiblemente mas urgente, porque es dinero descontado a
--                          gente que no figura en su cuenta.
--
--   El resto del frente (12 cargas, -11.112 en total) es goteo: roles del archivo
--   que no resuelven a ninguna entidad y casos sueltos. Ver el 75.
--
-- -----------------------------------------------------------------------------
-- INDICE
-- -----------------------------------------------------------------------------
--   A.1  354 — como se reparten sus filas por version del generador
--   A.2  354 — participes con mas filas que meses, y cuanto les sobra
--   A.3  354 — ¿hubo dos ejecuciones? huecos entre fechas de registro
--   B.1  448 — participes con descuento y SIN fila de aporte
--   B.2  448 — participes con fila por menos de lo descontado
--   B.3  448 — ¿el faltante fue a otro producto (prestamos) o se perdio?
-- =============================================================================


-- =============================================================================
-- A.1  CARGA 354 — reparto de sus filas por version del generador
-- =============================================================================
-- Si aparecen filas de DOS versiones distintas para la misma carga, el mecanismo
-- es M3: la carga se proceso con el generador viejo y se volvio a procesar con el
-- nuevo, y las filas viejas nunca se retiraron.
-- Universo por GLOSA / CRARCDGO — NUNCA por APRTIDAS (ver cabecera del 03).
-- =============================================================================
SELECT  CASE
            WHEN a.APRTGLSA LIKE 'Abono al aporte%'                          THEN 'V2 EXCEDENTE'
            WHEN a.APRTGLSA LIKE 'Aporte % - Mes %/% - CargaArchivo: %'      THEN 'V3 VIGENTE'
            WHEN a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'                   THEN 'V1 (sin - Mes -)'
            ELSE                                                                  'SIN PATRON'
        END                                             AS VERSION,
        a.TPAPCDGO                                      AS TIPO,
        COUNT(*)                                        AS FILAS,
        COUNT(DISTINCT a.ENTDCDGO)                      AS PARTICIPES,
        ROUND(SUM(a.APRTVLRR), 2)                       AS VALOR,
        MIN(a.APRTFCTR)                                 AS MIN_FECHA_CAJA,
        MAX(a.APRTFCTR)                                 AS MAX_FECHA_CAJA,
        MIN(a.APRTFCRG)                                 AS MIN_FECHA_REGISTRO,
        MAX(a.APRTFCRG)                                 AS MAX_FECHA_REGISTRO,
        SUM(CASE WHEN a.APRTPRDV IS NULL THEN 1 ELSE 0 END) AS SIN_DEVENGO
FROM    CRD.APRT a
WHERE   a.APRTVLRR > 0
AND     a.TPAPCDGO IN (9, 11)
AND     TO_NUMBER(REGEXP_SUBSTR(a.APRTGLSA, 'CargaArchivo: ([0-9]+)', 1, 1, NULL, 1)) = 354
GROUP BY CASE
            WHEN a.APRTGLSA LIKE 'Abono al aporte%'                          THEN 'V2 EXCEDENTE'
            WHEN a.APRTGLSA LIKE 'Aporte % - Mes %/% - CargaArchivo: %'      THEN 'V3 VIGENTE'
            WHEN a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'                   THEN 'V1 (sin - Mes -)'
            ELSE                                                                  'SIN PATRON'
         END, a.TPAPCDGO
ORDER BY 1, 2;


-- =============================================================================
-- A.2  CARGA 354 — participes a los que les sobra, y cuanto
-- =============================================================================
-- Por participe: lo que Petro le descontó en ESA carga contra lo que quedó
-- registrado de ESA carga. La diferencia positiva es el exceso atribuible a 354.
-- =============================================================================
WITH DESCONTADO_354 AS (
        SELECT  e.ENTDCDGO, SUM(NVL(x.PXCADSDO, 0)) AS DESCONTADO
        FROM    CRD.DTCA d
        JOIN    CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
        JOIN    CRD.ENTD e ON e.ENTDRLPC = x.PXCACDPT
        WHERE   d.CRARCDGO = 354
        AND     d.DTCACDPP = 'AH'
        GROUP BY e.ENTDCDGO
),
FILAS_354 AS (
        SELECT  a.ENTDCDGO,
                COUNT(*)                                            AS FILAS,
                SUM(a.APRTVLRR)                                     AS VALOR,
                SUM(CASE WHEN a.APRTGLSA LIKE '%- Mes %' THEN 1 ELSE 0 END) AS FILAS_V3,
                SUM(CASE WHEN a.APRTGLSA NOT LIKE '%- Mes %' THEN 1 ELSE 0 END) AS FILAS_V1
        FROM    CRD.APRT a
        WHERE   a.APRTVLRR > 0
        AND     a.TPAPCDGO IN (9, 11)
        AND     TO_NUMBER(REGEXP_SUBSTR(a.APRTGLSA, 'CargaArchivo: ([0-9]+)', 1, 1, NULL, 1)) = 354
        GROUP BY a.ENTDCDGO
)
SELECT  e.ENTDNMID                                      AS IDENTIFICACION,
        e.ENTDRZNS                                      AS PARTICIPE,
        f.FILAS,
        f.FILAS_V1,
        f.FILAS_V3,
        ROUND(NVL(d.DESCONTADO, 0), 2)                  AS DESCONTADO_EN_354,
        ROUND(f.VALOR, 2)                               AS REGISTRADO_DE_354,
        ROUND(f.VALOR - NVL(d.DESCONTADO, 0), 2)        AS EXCESO,
        CASE WHEN f.FILAS_V1 > 0 AND f.FILAS_V3 > 0
             THEN 'M3 — conviven V1 y V3 de la misma carga'
             WHEN f.FILAS > 2
             THEN 'Revisar — mas de una fila por tipo'
             ELSE 'Sin patron claro' END                AS DIAGNOSTICO
FROM    FILAS_354 f
JOIN    CRD.ENTD e ON e.ENTDCDGO = f.ENTDCDGO
LEFT    JOIN DESCONTADO_354 d ON d.ENTDCDGO = f.ENTDCDGO
WHERE   f.VALOR - NVL(d.DESCONTADO, 0) > 0.02
ORDER BY 8 DESC;


-- =============================================================================
-- A.3  CARGA 354 — ¿una ejecucion o dos?
-- =============================================================================
-- Agrupa las filas por hora de registro. Dos bloques separados por horas o dias
-- es la huella de M1 (la fase 3 corrida dos veces). Un solo bloque descarta M1 y
-- deja a M3 como explicacion.
-- Las filas sin APRTFCRG no participan del calculo: se cuentan aparte.
-- =============================================================================
SELECT  TO_CHAR(a.APRTFCRG, 'YYYY-MM-DD HH24')          AS HORA_REGISTRO,
        COUNT(*)                                        AS FILAS,
        COUNT(DISTINCT a.ENTDCDGO)                      AS PARTICIPES,
        ROUND(SUM(a.APRTVLRR), 2)                       AS VALOR
FROM    CRD.APRT a
WHERE   a.APRTVLRR > 0
AND     a.TPAPCDGO IN (9, 11)
AND     TO_NUMBER(REGEXP_SUBSTR(a.APRTGLSA, 'CargaArchivo: ([0-9]+)', 1, 1, NULL, 1)) = 354
GROUP BY TO_CHAR(a.APRTFCRG, 'YYYY-MM-DD HH24')
ORDER BY 1;


-- =============================================================================
-- B.1  CARGA 448 — participes con descuento y SIN ninguna fila de aporte
-- =============================================================================
-- Dinero que Petro descontó en julio 2026 y que no figura en la cuenta de nadie.
-- =============================================================================
SELECT  e.ENTDNMID                                      AS IDENTIFICACION,
        e.ENTDRZNS                                      AS PARTICIPE,
        x.PXCACDPT                                      AS ROL_EN_ARCHIVO,
        ROUND(NVL(x.PXCADSDO, 0), 2)                    AS DESCONTADO,
        NVL(esp.ESPRNMBR, TO_CHAR(e.ENTDIDST))          AS ESTADO_PARTICIPE
FROM    CRD.DTCA d
JOIN    CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
LEFT    JOIN CRD.ENTD e ON e.ENTDRLPC = x.PXCACDPT
LEFT    JOIN CRD.ESPR esp ON esp.ESPRCDEX = e.ENTDIDST
WHERE   d.CRARCDGO = 448
AND     d.DTCACDPP = 'AH'
AND     NVL(x.PXCADSDO, 0) > 0.01
AND     NOT EXISTS (
            SELECT 1 FROM CRD.APRT a
            WHERE  a.ENTDCDGO = e.ENTDCDGO
            AND    a.TPAPCDGO IN (9, 11)
            AND    a.APRTVLRR > 0
            AND    TO_NUMBER(REGEXP_SUBSTR(a.APRTGLSA, 'CargaArchivo: ([0-9]+)', 1, 1, NULL, 1)) = 448 )
ORDER BY 4 DESC;


-- =============================================================================
-- B.2  CARGA 448 — participes con fila, pero por menos de lo descontado
-- =============================================================================
WITH DESCONTADO_448 AS (
        SELECT  e.ENTDCDGO, SUM(NVL(x.PXCADSDO, 0)) AS DESCONTADO
        FROM    CRD.DTCA d
        JOIN    CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
        JOIN    CRD.ENTD e ON e.ENTDRLPC = x.PXCACDPT
        WHERE   d.CRARCDGO = 448 AND d.DTCACDPP = 'AH'
        GROUP BY e.ENTDCDGO
),
FILAS_448 AS (
        SELECT  a.ENTDCDGO, COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS VALOR
        FROM    CRD.APRT a
        WHERE   a.APRTVLRR > 0 AND a.TPAPCDGO IN (9, 11)
        AND     TO_NUMBER(REGEXP_SUBSTR(a.APRTGLSA, 'CargaArchivo: ([0-9]+)', 1, 1, NULL, 1)) = 448
        GROUP BY a.ENTDCDGO
)
SELECT  e.ENTDNMID                                      AS IDENTIFICACION,
        e.ENTDRZNS                                      AS PARTICIPE,
        f.FILAS,
        ROUND(d.DESCONTADO, 2)                          AS DESCONTADO,
        ROUND(f.VALOR, 2)                               AS REGISTRADO,
        ROUND(f.VALOR - d.DESCONTADO, 2)                AS DIFERENCIA
FROM    DESCONTADO_448 d
JOIN    FILAS_448 f ON f.ENTDCDGO = d.ENTDCDGO
JOIN    CRD.ENTD e  ON e.ENTDCDGO = d.ENTDCDGO
WHERE   ABS(f.VALOR - d.DESCONTADO) > 0.02
ORDER BY 6;


-- =============================================================================
-- B.3  CARGA 448 — ¿a que se aplico todo lo que trajo el archivo?
-- =============================================================================
-- Compara, por PRODUCTO del archivo, lo descontado contra lo aplicado. Si el
-- faltante de 'AH' aparece como sobrante en otro producto, el dinero se aplico
-- mal, no se perdio. Si no aparece en ninguno, quedo sin aplicar.
-- =============================================================================
SELECT  d.DTCACDPP                                      AS PRODUCTO,
        d.DTCANMPP                                      AS NOMBRE_PRODUCTO,
        COUNT(*)                                        AS LINEAS,
        SUM(CASE WHEN NVL(x.PXCADSDO, 0) > 0.01 THEN 1 ELSE 0 END) AS LINEAS_CON_DESCUENTO,
        ROUND(SUM(NVL(x.PXCADSDO, 0)), 2)               AS DESCONTADO
FROM    CRD.DTCA d
JOIN    CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
WHERE   d.CRARCDGO = 448
GROUP BY d.DTCACDPP, d.DTCANMPP
ORDER BY 5 DESC;


-- =============================================================================
-- FIN. Nada de este script modifica datos.
-- =============================================================================

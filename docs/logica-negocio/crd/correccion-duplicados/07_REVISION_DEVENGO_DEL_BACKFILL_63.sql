-- =============================================================================
-- 07 — QUE DEJO EL BACKFILL 63 EN APRTPRDV, Y DONDE SE PASO
-- =============================================================================
-- FECHA: 2026-08-31 · Equipo omen-saa-2
--
-- ⛔ SOLO LECTURA. Sin DML, sin bloque de reverso.
-- ⛔ Todas las consultas devuelven POCAS FILAS a proposito.
--
-- -----------------------------------------------------------------------------
-- POR QUE EXISTE — el 63 ya hizo una reubicacion, sin grilla
-- -----------------------------------------------------------------------------
--   El usuario confirmo el 2026-08-31 que NINGUNA carga se proceso todavia con
--   el devengo nuevo. O sea que TODO lo que hay hoy en APRTPRDV lo escribio
--   63_BACKFILL_DEVENGO_APORTES.sql, no la carga.
--
--   Y al leer el 63 aparece que su REGLA 2 (el MERGE, lineas 196-246) ya hace
--   una version de lo que este frente iba a hacer: cuando encuentra VARIAS filas
--   en el mismo mes de caja, les reparte meses HACIA ATRAS, uno por fila, y la
--   fila mas antigua del grupo se lleva el mes mas lejano.
--
--   La idea es la misma. Le faltan tres cosas, y las tres son medibles:
--
--     (1) NO TIENE GRILLA. meses_atras genera 24 meses hacia atras con un
--         CONNECT BY, sin preguntarle a CRD.VGCN si en ese mes se esperaba un
--         aporte. Puede haber puesto devengo en meses en que el participe todavia
--         no aportaba.
--     (2) NO TIENE PISO. ADD_MONTHS(MES_CAJA, -23) desde 2025-06 llega a 2023-07.
--         Pero ALCANCE_MINIMO_DEVENGO (CargaArchivoPetroServiceImpl:3518) es
--         2025-06-01, y sumValorPorEntidadTipoYRangoDevengo filtra
--         "HAVING periodo BETWEEN :desde AND :hasta". Una fila con devengo
--         anterior al piso QUEDA INVISIBLE para el calculo del faltante: su mes
--         se ve impago y se puede volver a cobrar.
--     (3) SOLO MIRA HACIA ATRAS. Nunca asigna un mes posterior al de caja, asi
--         que no puede cerrar un hueco que quedo adelante.
--
--   Este script mide las tres. Segun lo que de, la reubicacion del 03 es una
--   SEGUNDA PASADA sobre lo del 63 — no un trabajo desde cero — y puede tener que
--   corregir filas que el 63 dejo en un mes imposible.
--
-- ⚠ URGENCIA: esto se resuelve ANTES de que corra la primera carga con devengo.
--   Desde esa carga, distribuirAportePorDevengo empieza a consumir los huecos con
--   plata nueva y el reparto historico pasa a ser un blanco movil.
--
-- INDICE
--   1  Devengo vs mes de caja: cuanto movio el 63
--   2  ⛔ Filas con devengo ANTERIOR al piso 2025-06 — invisibles al faltante
--   3  Filas cuyo devengo cae FUERA de la vigencia del participe
--   4  Los 15 desplazamientos mas grandes, con nombre
-- =============================================================================


-- =============================================================================
-- 1. DEVENGO VS MES DE CAJA — cuanto movio el 63
-- =============================================================================
-- DESPLAZAMIENTO = meses entre el devengo y el mes de caja. 0 = la Regla 1 (la
-- gran mayoria). Negativo = la Regla 2 lo mando hacia atras. Positivo NO deberia
-- existir: el 63 nunca asigna un mes posterior.
-- =============================================================================
SELECT  MONTHS_BETWEEN(a.APRTPRDV, TRUNC(a.APRTFCTR, 'MM'))     AS DESPLAZAMIENTO_MESES,
        COUNT(*)                                                AS FILAS,
        COUNT(DISTINCT a.ENTDCDGO)                              AS PARTICIPES,
        ROUND(SUM(a.APRTVLRR), 2)                               AS VALOR,
        MIN(a.APRTPRDV)                                         AS MIN_DEVENGO,
        MAX(a.APRTPRDV)                                         AS MAX_DEVENGO
FROM    CRD.APRT a
WHERE   a.TPAPCDGO IN (9, 11)
AND     a.APRTVLRR > 0
AND     a.APRTPRDV IS NOT NULL
AND     a.APRTFCTR >= DATE '2025-06-01'
GROUP BY MONTHS_BETWEEN(a.APRTPRDV, TRUNC(a.APRTFCTR, 'MM'))
ORDER BY 1;


-- =============================================================================
-- 2. ⛔ FILAS CON DEVENGO ANTERIOR AL PISO — las que el faltante no ve
-- =============================================================================
-- Si esto devuelve algo, hay dinero registrado cuyo mes de devengo cae fuera del
-- rango que mira distribuirAportePorDevengo. Ese mes se ve impago aunque este
-- pagado, y la proxima carga con devengo lo cobra de nuevo.
-- Esperado ideal: 0 filas.
-- =============================================================================
SELECT  COUNT(*)                                AS FILAS,
        COUNT(DISTINCT a.ENTDCDGO)              AS PARTICIPES,
        ROUND(SUM(a.APRTVLRR), 2)               AS VALOR,
        MIN(a.APRTPRDV)                         AS DEVENGO_MAS_ANTIGUO,
        MAX(a.APRTPRDV)                         AS DEVENGO_MAS_RECIENTE
FROM    CRD.APRT a
WHERE   a.TPAPCDGO IN (9, 11)
AND     a.APRTVLRR > 0
AND     a.APRTPRDV IS NOT NULL
AND     a.APRTPRDV < DATE '2025-06-01';


-- =============================================================================
-- 3. FILAS CUYO DEVENGO CAE FUERA DE LA VIGENCIA DEL PARTICIPE
-- =============================================================================
-- El 63 repartio meses hacia atras sin consultar CRD.VGCN. Aca se comprueba si
-- alguno de esos meses cae antes del inicio de la vigencia (o despues de su fin),
-- o sea en un mes en que ese participe no debia aportar.
-- Los participes SIN contrato ACTIVO se cuentan aparte: para ellos no hay
-- vigencia contra la cual comparar, y ya estan contados en el 06.
-- =============================================================================
WITH CONTRATO_ACTIVO AS (
        SELECT c.ENTDCDGO, MAX(c.CNTRCDGO) AS CNTRCDGO
        FROM   CRD.CNTR c WHERE c.CNTRESTD = 1 GROUP BY c.ENTDCDGO
)
SELECT  CASE WHEN ca.ENTDCDGO IS NULL              THEN '1. sin contrato ACTIVO — no evaluable'
             WHEN v.VGCNCDGO IS NULL               THEN '2. devengo FUERA de toda vigencia — REVISAR'
             ELSE                                       '3. devengo dentro de la vigencia'
        END                                         AS SITUACION,
        a.TPAPCDGO                                  AS TIPO,
        COUNT(*)                                    AS FILAS,
        COUNT(DISTINCT a.ENTDCDGO)                  AS PARTICIPES,
        ROUND(SUM(a.APRTVLRR), 2)                   AS VALOR,
        MIN(a.APRTPRDV)                             AS MIN_DEVENGO
FROM    CRD.APRT a
LEFT    JOIN CONTRATO_ACTIVO ca ON ca.ENTDCDGO = a.ENTDCDGO
LEFT    JOIN CRD.VGCN v
        ON  v.CNTRCDGO = ca.CNTRCDGO
        AND v.TPAPCDGO = a.TPAPCDGO
        AND v.VGCNIDST = 1
        AND v.VGCNFCIN <= LAST_DAY(a.APRTPRDV)
        AND (v.VGCNFCFN IS NULL OR v.VGCNFCFN >= LAST_DAY(a.APRTPRDV))
WHERE   a.TPAPCDGO IN (9, 11)
AND     a.APRTVLRR > 0
AND     a.APRTPRDV IS NOT NULL
AND     a.APRTFCTR >= DATE '2025-06-01'
GROUP BY CASE WHEN ca.ENTDCDGO IS NULL              THEN '1. sin contrato ACTIVO — no evaluable'
              WHEN v.VGCNCDGO IS NULL               THEN '2. devengo FUERA de toda vigencia — REVISAR'
              ELSE                                       '3. devengo dentro de la vigencia'
         END, a.TPAPCDGO
ORDER BY 1, 2;


-- =============================================================================
-- 4. LOS 15 DESPLAZAMIENTOS MAS GRANDES, CON NOMBRE
-- =============================================================================
-- Para mirar de a uno los casos donde el 63 mando una fila mas lejos hacia atras.
-- =============================================================================
SELECT * FROM (
    SELECT  e.ENTDNMID                                              AS IDENTIFICACION,
            SUBSTR(e.ENTDRZNS, 1, 35)                               AS PARTICIPE,
            a.TPAPCDGO                                              AS TIPO,
            a.APRTCDGO                                              AS ID_APORTE,
            ROUND(a.APRTVLRR, 2)                                    AS VALOR,
            TO_CHAR(TRUNC(a.APRTFCTR, 'MM'), 'MM/YYYY')             AS MES_CAJA,
            TO_CHAR(a.APRTPRDV, 'MM/YYYY')                          AS DEVENGO,
            ROUND(MONTHS_BETWEEN(a.APRTPRDV, TRUNC(a.APRTFCTR, 'MM'))) AS DESPLAZAMIENTO,
            SUBSTR(a.APRTGLSA, 1, 60)                               AS GLOSA
    FROM    CRD.APRT a
    JOIN    CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
    WHERE   a.TPAPCDGO IN (9, 11)
    AND     a.APRTVLRR > 0
    AND     a.APRTPRDV IS NOT NULL
    AND     a.APRTFCTR >= DATE '2025-06-01'
    ORDER BY MONTHS_BETWEEN(a.APRTPRDV, TRUNC(a.APRTFCTR, 'MM')) ASC, a.APRTCDGO
) WHERE ROWNUM <= 15;


-- =============================================================================
-- FIN. Nada de este script modifica datos.
-- =============================================================================

-- =============================================================================
-- 09 — PREVUELO DEL 08, DESPUES DE LOS 373 CONTRATOS DEL SCRIPT 98
-- =============================================================================
-- FECHA: 2026-08-31 · Equipo omen-saa-2
--
-- ⛔ SOLO LECTURA. Sin DML, sin bloque de reverso. Pocas filas a proposito.
--
-- -----------------------------------------------------------------------------
-- PARA QUE
-- -----------------------------------------------------------------------------
--   El equipo A corrio 98_CONTRATOS_FALTANTES_404.sql: 373 contratos y 486
--   vigencias, todas desde 2025-06-01 y abiertas. La grilla cambio, asi que el
--   alcance del 08 cambio con ella.
--
--   Este script se corre JUSTO ANTES del 08 y responde tres cosas:
--     A. Cuanta gente alcanza ahora la reubicacion (era 1.640 de 2.044).
--     B. Los 31 que el criterio del equipo A no vio — su pregunta explicita.
--     C. Si quedan pares (entidad,tipo) sin cupos, que son los que el 08 no
--        podra tocar y que ademas, con el abort nuevo, detendrian la carga.
--
-- -----------------------------------------------------------------------------
-- POR QUE SON 373 Y NO 404 — el punto que hay que medir
-- -----------------------------------------------------------------------------
--   Los dos universos NO son el mismo:
--     equipo A : CRARCDGO IS NOT NULL  OR  APRTIDAS existe en CRD.CRAR
--     este     : lo anterior  OR  APRTGLSA LIKE 'Aporte %CargaArchivo: %'
--                                 OR  APRTGLSA LIKE 'Abono al aporte%'
--
--   La rama de glosa es la que ve las 2.635 filas de junio 2025 (carga 352), que
--   no tienen APRTIDAS ni CRARCDGO ni usuario. Si algun participe existe SOLO por
--   esa rama, no entro en el 98 y sigue sin contrato.
--
--   El bloque B los cuenta y los lista. Si sale > 0, hay que pasarle la lista al
--   equipo A: son suyos, CRD.CNTR es su tabla.
-- =============================================================================


-- =============================================================================
-- A. COBERTURA DESPUES DEL 98 — ¿a cuanta gente alcanza ahora el 08?
-- =============================================================================
-- Mismo conteo que el 06 §1. ANTES del 98 daba: 2.044 / 1.640 / 1.640 / 1.640.
-- Esperado ahora: CON_CONTRATO_ACTIVO cerca de 2.013 (1.640 + 373).
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
SELECT  (SELECT COUNT(*) FROM MOVILES)                                      AS CON_FILAS_MOVILES,
        (SELECT COUNT(*) FROM MOVILES m
          WHERE EXISTS (SELECT 1 FROM CRD.CNTR c
                        WHERE c.ENTDCDGO = m.ENTDCDGO AND c.CNTRESTD = 1))  AS CON_CONTRATO_ACTIVO,
        (SELECT COUNT(*) FROM MOVILES m
          WHERE EXISTS (SELECT 1 FROM CRD.CNTR c
                        JOIN CRD.VGCN v ON v.CNTRCDGO = c.CNTRCDGO
                        WHERE c.ENTDCDGO = m.ENTDCDGO AND c.CNTRESTD = 1
                        AND   v.TPAPCDGO IN (9, 11) AND v.VGCNIDST = 1
                        AND   NVL(v.VGCNMNTO, 0) > 0))                      AS CON_VIGENCIA_UTIL,
        (SELECT COUNT(*) FROM MOVILES m
          WHERE NOT EXISTS (SELECT 1 FROM CRD.CNTR c
                            WHERE c.ENTDCDGO = m.ENTDCDGO AND c.CNTRESTD = 1)) AS TODAVIA_SIN_CONTRATO
FROM    DUAL;


-- =============================================================================
-- B. ⛔ LOS QUE SOLO VE EL CRITERIO DE GLOSA — respuesta al equipo A
-- =============================================================================
-- Participes que este equipo identifica como "de carga" y el criterio del equipo A
-- NO, y que ademas siguen sin contrato ACTIVO. Son los que el 98 no pudo alcanzar.
-- Con el abort nuevo, cada uno de estos detiene la carga del mes.
-- Si devuelve 0 filas, los 373 cubrieron todo y no hay nada que pedirles.
-- =============================================================================
SELECT  e.ENTDNMID                              AS IDENTIFICACION,
        SUBSTR(e.ENTDRZNS, 1, 40)               AS PARTICIPE,
        e.ENTDRLPC                              AS ROL_PETRO,
        COUNT(*)                                AS FILAS,
        ROUND(SUM(a.APRTVLRR), 2)               AS VALOR,
        MIN(a.APRTFCTR)                         AS DESDE,
        MAX(a.APRTFCTR)                         AS HASTA
FROM    CRD.APRT a
JOIN    CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
WHERE   a.TPAPCDGO IN (9, 11)
AND     a.APRTVLRR > 0
AND     a.APRTFCTR >= DATE '2025-06-01'
-- lo ve este equipo...
AND     (   a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
         OR a.APRTGLSA LIKE 'Abono al aporte%')
-- ...y NO lo ve el criterio del equipo A
AND     a.CRARCDGO IS NULL
AND     NOT EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
-- ...y sigue sin contrato
AND     NOT EXISTS (SELECT 1 FROM CRD.CNTR c
                    WHERE c.ENTDCDGO = a.ENTDCDGO AND c.CNTRESTD = 1)
GROUP BY e.ENTDNMID, e.ENTDRZNS, e.ENTDRLPC
ORDER BY 5 DESC;


-- =============================================================================
-- C. PARES (ENTIDAD, TIPO) QUE SIGUEN SIN CUPOS
-- =============================================================================
-- Un participe puede tener contrato y vigencia de UN tipo y no del otro (el equipo
-- A midio 207 sin jubilacion y 53 sin cesantia, porque su HSTR tiene ese monto en
-- cero). Si aporta el tipo que no tiene vigencia, ese par sigue sin grilla: el 08
-- no lo toca, y con el abort nuevo detiene la carga.
-- =============================================================================
WITH APORTES AS (
        SELECT  a.ENTDCDGO, a.TPAPCDGO, a.APRTVLRR
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
        AND     a.APRTFCTR >= DATE '2025-06-01'
        AND     (   a.CRARCDGO IS NOT NULL
                 OR EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
                 OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                 OR a.APRTGLSA LIKE 'Abono al aporte%')
)
SELECT  CASE WHEN NOT EXISTS (SELECT 1 FROM CRD.CNTR c
                              WHERE c.ENTDCDGO = ap.ENTDCDGO AND c.CNTRESTD = 1)
             THEN '1. sin contrato ACTIVO'
             ELSE '2. con contrato, SIN vigencia de ESE tipo' END  AS SITUACION,
        ap.TPAPCDGO                             AS TIPO,
        COUNT(DISTINCT ap.ENTDCDGO)             AS PARTICIPES,
        COUNT(*)                                AS FILAS,
        ROUND(SUM(ap.APRTVLRR), 2)              AS VALOR
FROM    APORTES ap
WHERE   NOT EXISTS (
            SELECT 1 FROM CRD.CNTR c
            JOIN   CRD.VGCN v ON v.CNTRCDGO = c.CNTRCDGO
            WHERE  c.ENTDCDGO = ap.ENTDCDGO AND c.CNTRESTD = 1
            AND    v.TPAPCDGO = ap.TPAPCDGO AND v.VGCNIDST = 1
            AND    NVL(v.VGCNMNTO, 0) > 0)
GROUP BY CASE WHEN NOT EXISTS (SELECT 1 FROM CRD.CNTR c
                               WHERE c.ENTDCDGO = ap.ENTDCDGO AND c.CNTRESTD = 1)
              THEN '1. sin contrato ACTIVO'
              ELSE '2. con contrato, SIN vigencia de ESE tipo' END, ap.TPAPCDGO
ORDER BY 1, 2;


-- =============================================================================
-- D. EL CAMPO MAL DOCUMENTADO, APLICADO A CRD.VGCN
-- =============================================================================
-- El equipo A encontro que HistorialSueldo.HSTRPRJB, documentado como "Porcentaje
-- Jubilacion", contiene un PERIODO (202606). Lo agarro porque Oracle rechazo el
-- valor por precision; en una columna NUMBER a secas habria entrado sin error.
--
-- Este bloque busca la misma clase de dato en VGCNPRCN: un porcentaje deberia
-- estar entre 0 y 100. Cualquier cosa por encima es un valor que no es un
-- porcentaje. Esperado: 0 filas.
--
-- Nota: los scripts de este frente NO leen HSTR ni sus porcentajes — usan VGCN y
-- solo como prueba de "monto > 0" —, asi que el hallazgo no los afecta. Este
-- control existe para descartar que el mismo dato haya llegado a VGCN por la
-- migracion.
-- =============================================================================
SELECT  COUNT(*)                        AS VIGENCIAS_CON_PORCENTAJE_IMPOSIBLE,
        MIN(v.VGCNPRCN)                 AS MIN_VALOR,
        MAX(v.VGCNPRCN)                 AS MAX_VALOR
FROM    CRD.VGCN v
WHERE   v.TPAPCDGO IN (9, 11)
AND     NVL(v.VGCNPRCN, 0) > 100;


-- =============================================================================
-- FIN. Nada de este script modifica datos.
-- Si A y C dan lo esperado y B da 0, el 08 puede correr.
-- =============================================================================

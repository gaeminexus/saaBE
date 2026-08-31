-- =============================================================================
-- 03 — PROPUESTA DE REUBICACION DE APORTES A LOS MESES HUECOS  (DRY RUN)
-- =============================================================================
-- FECHA: 2026-08-31 · Equipo omen-saa-2
-- Implementa el algoritmo de 02_ALGORITMO_REUBICACION_MESES.md. LEER ESE
-- DOCUMENTO ANTES: acá está el "como", allá el "por que" y las tres validaciones
-- que corrigen el enunciado original.
--
-- ⛔ SOLO LECTURA. NINGUN UPDATE, NINGUN DML. Sin bloque de reverso porque no hay
--    nada que revertir. Es seguro correrlo de corrido.
--
-- -----------------------------------------------------------------------------
-- QUE HACE
-- -----------------------------------------------------------------------------
--   Calcula, fila por fila, QUE APORTE SE MOVERIA DE QUE MES A QUE MES, y deja
--   escrita la glosa que quedaria. Esa salida es lo que se revisa y aprueba; el
--   UPDATE se escribe despues, en 04_, tomando exactamente esta lista.
--
-- -----------------------------------------------------------------------------
-- LAS TRES REGLAS QUE HACEN QUE ESTO SEA CORRECTO (detalle en el 02)
-- -----------------------------------------------------------------------------
--   1. SE MUEVE APRTPRDV (periodo de devengo), NUNCA APRTFCTR (fecha de caja).
--      Todo el sistema decide el mes de un aporte con PeriodoEfectivoAporteSql:
--         CASE WHEN APRTPRDV IS NOT NULL THEN APRTPRDV
--              WHEN APRTVLRR > 0         THEN TRUNC(APRTFCTR,'MM') ELSE NULL END
--      APRTPRDV gana siempre: en una fila que ya tiene devengo, cambiar APRTFCTR
--      no mueve nada de mes y ademas falsea la fecha contable.
--
--   2. SOLO PARTICIPES CUYO DINERO CUADRA. Mover no altera SUM(APRTVLRR): si al
--      participe le sobra saldo, reubicar convierte un saldo inflado en una
--      historia falsificada (meses impagos que quedan como pagados). Primero se
--      depura el exceso (R1-R6 del README), despues se reubica.
--
--   3. UN MES VACIO NO ES UN HUECO SI NO SE ESPERABA APORTE. El piso real de cada
--      participe es su vigencia de contrato, no junio 2025.
--
-- -----------------------------------------------------------------------------
-- ⛔ CORREGIDO EL 2026-08-31, DESPUES DE MEDIR: "APRTIDAS IS NOT NULL" NO SIRVE
--    PARA IDENTIFICAR UNA FILA DE CARGA. LEER ANTES DE COMPARAR CON EL 69.
-- -----------------------------------------------------------------------------
--   El bloque 4 del 01 devolvio 393.869 filas de tipos 9/11 con APRTIDAS lleno y
--   fechas desde 1990-01-28. La primera carga Petro afecta a 2025-06: esas filas
--   NO pueden venir de una carga. APRTIDAS (Aporte.idAsoprep) fue rellenado por la
--   MIGRACION con el id del aporte en el sistema viejo, y despues el codigo reuso
--   la misma columna para el id de CargaArchivo. Dos significados, una columna.
--
--   Verificado en el codigo: el UNICO punto que le escribe un valor es
--   CargaArchivoPetroServiceImpl.crearNuevoAporte (:3751); todos los demas
--   (AporteServiceImpl, DevolucionAporteServiceImpl, ProcesoPagoPrestamoServiceImpl,
--   PagoPensionComplementariaServiceImpl) le ponen NULL explicito. O sea: lo que
--   hay de 1990 a 2025 no lo escribio esta aplicacion.
--
--   Verificado en los datos: filas con glosa V3 = 29.674; filas con CRARCDGO
--   lleno = 29.677. Coinciden. Las 393.869 sobran por completo.
--
--   POR ESO el universo de este script YA NO usa APRTIDAS. Una fila es de la carga
--   por su GLOSA (los tres patrones conocidos) o por CRARCDGO, que es la columna
--   gobernada y solo la escribe la carga. Sin esto, la reubicacion habria tratado
--   como movibles 393.869 filas historicas.
--
--   ⚠ EL 69 SIGUE USANDO APRTIDAS EN SU UNIVERSO. Sus cifras de exceso por
--     participe (§3) estan infladas por esas filas. Corregirlo antes de usarlas.
--
-- -----------------------------------------------------------------------------
-- INDICE
-- -----------------------------------------------------------------------------
--   0  Parametros efectivos (piso y techo del rango)
--   1  Participes ELEGIBLES vs EXCLUIDOS por la regla del dinero
--   2  ▶ LA PROPUESTA: cada movimiento, con su glosa nueva
--   3  Resumen por participe
--   4  SOBRANTES — mas filas moviles que cupos (revision individual)
--   5  Huecos que quedarian sin cubrir despues de reubicar
-- =============================================================================


-- =============================================================================
-- 0. PARAMETROS EFECTIVOS
-- =============================================================================
-- PISO  2025-06-01 — es ALCANCE_MINIMO_DEVENGO (CargaArchivoPetroServiceImpl:3518).
--       Obligatorio: por debajo, el devengo es NULL a proposito y TODOS los meses
--       se verian incompletos.
-- TECHO el mes de la ULTIMA CARGA PROCESADA (CRARESTD = 3), no SYSDATE: el mes en
--       curso todavia no se cobro, asi que no es un hueco.
-- Si TECHO sale NULL, no hay ninguna carga procesada y el resto no devuelve nada.
-- =============================================================================
SELECT  DATE '2025-06-01'                                       AS PISO,
        (SELECT MAX(TO_DATE(TO_CHAR(c.CRARANAF) || LPAD(TO_CHAR(c.CRARMSAF), 2, '0'), 'YYYYMM'))
           FROM CRD.CRAR c WHERE c.CRARESTD = 3)                AS TECHO,
        (SELECT COUNT(*) FROM CRD.CRAR c WHERE c.CRARESTD = 3)  AS CARGAS_PROCESADAS
FROM    DUAL;


-- =============================================================================
-- 1. PARTICIPES ELEGIBLES VS EXCLUIDOS — la regla del dinero (validacion 2)
-- =============================================================================
-- ELEGIBLE  : |valor de las filas de carga - descontado real| <= 0.02
--             Es el mecanismo M6: estaba en mora, pago varios meses de golpe y el
--             generador les puso a todas el mes de la carga. Plata real, mes mal
--             puesto. Reubicar es la correccion correcta.
-- EXCLUIDO  : hay diferencia. NO se reubica: primero se depura el exceso.
-- =============================================================================
WITH DESCONTADO AS (
        SELECT  e.ENTDCDGO, SUM(NVL(x.PXCADSDO, 0)) AS DESCONTADO
        FROM    CRD.DTCA d
        JOIN    CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
        JOIN    CRD.CRAR c ON c.CRARCDGO = d.CRARCDGO
        JOIN    CRD.ENTD e ON e.ENTDRLPC = x.PXCACDPT
        WHERE   d.DTCACDPP = 'AH'
        AND     c.CRARESTD = 3
        GROUP BY e.ENTDCDGO
),
VALOR_CARGA AS (
        SELECT  a.ENTDCDGO, SUM(a.APRTVLRR) AS VALOR, COUNT(*) AS FILAS
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (9, 11)
        AND     a.APRTVLRR > 0
        AND     (   a.CRARCDGO IS NOT NULL
                 OR (    a.APRTFCTR >= DATE '2025-06-01'
                     AND (   EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
                          OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                          OR a.APRTGLSA LIKE 'Abono al aporte%')))
        GROUP BY a.ENTDCDGO
)
SELECT  CASE WHEN ABS(v.VALOR - NVL(d.DESCONTADO, 0)) <= 0.02
             THEN 'ELEGIBLE — el dinero cuadra (M6: mora cobrada de golpe)'
             WHEN v.VALOR - NVL(d.DESCONTADO, 0) > 0.02
             THEN 'EXCLUIDO — saldo INFLADO: depurar el exceso primero (R1-R6)'
             ELSE 'EXCLUIDO — registrado MENOS que lo descontado: falta registrar, caso aparte'
        END                                             AS CLASIFICACION,
        COUNT(*)                                        AS PARTICIPES,
        SUM(v.FILAS)                                    AS FILAS_DE_CARGA,
        ROUND(SUM(v.VALOR), 2)                          AS VALOR_REGISTRADO,
        ROUND(SUM(NVL(d.DESCONTADO, 0)), 2)             AS DESCONTADO_REAL,
        ROUND(SUM(v.VALOR - NVL(d.DESCONTADO, 0)), 2)   AS DIFERENCIA
FROM    VALOR_CARGA v
LEFT    JOIN DESCONTADO d ON d.ENTDCDGO = v.ENTDCDGO
GROUP BY CASE WHEN ABS(v.VALOR - NVL(d.DESCONTADO, 0)) <= 0.02
             THEN 'ELEGIBLE — el dinero cuadra (M6: mora cobrada de golpe)'
             WHEN v.VALOR - NVL(d.DESCONTADO, 0) > 0.02
             THEN 'EXCLUIDO — saldo INFLADO: depurar el exceso primero (R1-R6)'
             ELSE 'EXCLUIDO — registrado MENOS que lo descontado: falta registrar, caso aparte'
        END
ORDER BY 1;


-- =============================================================================
-- 2. ▶ LA PROPUESTA — cada movimiento, con la glosa que quedaria
-- =============================================================================
-- Una fila por aporte a mover. Si esta consulta devuelve 0 filas, no hay nada que
-- reubicar y el frente se resuelve entero por la via de la depuracion del exceso.
--
-- ⚠ La columna GLOSA_NUEVA mete la traza ANTES de " - CargaArchivo:", NUNCA al
--   final. El script 69 extrae el id de carga con un regex ANCLADO al final
--   ('CargaArchivo: ([0-9]+)\s*$'): cualquier texto despues del numero saca la
--   fila de todos los analisis por carga, en silencio.
--   El marcador "(reubicado desde" es ademas lo que hace IDEMPOTENTE al 04: una
--   fila ya movida se reconoce y no se vuelve a mover.
-- =============================================================================
WITH PARAM AS (
        SELECT  DATE '2025-06-01' AS PISO,
                (SELECT MAX(TO_DATE(TO_CHAR(c.CRARANAF) || LPAD(TO_CHAR(c.CRARMSAF), 2, '0'), 'YYYYMM'))
                   FROM CRD.CRAR c WHERE c.CRARESTD = 3) AS TECHO
        FROM DUAL
),
MESES AS (
        SELECT  ADD_MONTHS(p.PISO, LEVEL - 1) AS MES
        FROM    PARAM p
        CONNECT BY LEVEL <= MONTHS_BETWEEN(p.TECHO, p.PISO) + 1
),
-- Universo de aportes positivos de los tipos 9/11, con su periodo efectivo y su
-- clase. La clase se decide POR LA FORMA de la fila, nunca por APRTUSRG: las 2.635
-- filas de junio 2025 tienen usuario NULL y ese filtro es el que las escondio.
APORTES AS (
        SELECT  a.APRTCDGO, a.ENTDCDGO, a.TPAPCDGO, a.APRTVLRR, a.APRTGLSA,
                a.APRTPRDV, a.APRTFCTR, a.APRTIDAS, a.APRTTPMV,
                CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                     ELSE TRUNC(a.APRTFCTR, 'MM') END               AS PERIODO_EFECTIVO,
                CASE WHEN (   a.CRARCDGO IS NOT NULL
                           OR (    a.APRTFCTR >= DATE '2025-06-01'
                               AND (   EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
                                    OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                                    OR a.APRTGLSA LIKE 'Abono al aporte%')))
                     THEN 'MOVIL' ELSE 'FIJA' END                    AS CLASE
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (9, 11)
        AND     a.APRTVLRR > 0
        AND     NVL(a.APRTTPMV, 1) <> 8                 -- 8 = EXCEDENTE_PETRO: no cubre ningun mes
        AND     a.APRTGLSA NOT LIKE 'REVERSO%'
        AND     a.APRTGLSA NOT LIKE 'PAGO PRESTAMO%'
        AND     a.APRTGLSA NOT LIKE 'DEVOLUCION%'
),
-- Elegibilidad por dinero (ver bloque 1)
DESCONTADO AS (
        SELECT  e.ENTDCDGO, SUM(NVL(x.PXCADSDO, 0)) AS DESCONTADO
        FROM    CRD.DTCA d
        JOIN    CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
        JOIN    CRD.CRAR c ON c.CRARCDGO = d.CRARCDGO
        JOIN    CRD.ENTD e ON e.ENTDRLPC = x.PXCACDPT
        WHERE   d.DTCACDPP = 'AH'
        AND     c.CRARESTD = 3
        GROUP BY e.ENTDCDGO
),
ELEGIBLE AS (
        SELECT  a.ENTDCDGO
        FROM    APORTES a
        LEFT    JOIN DESCONTADO d ON d.ENTDCDGO = a.ENTDCDGO
        WHERE   a.CLASE = 'MOVIL'
        GROUP BY a.ENTDCDGO, d.DESCONTADO
        HAVING  ABS(SUM(a.APRTVLRR) - NVL(MAX(d.DESCONTADO), 0)) <= 0.02
),
-- Contrato ACTIVO (CNTRESTD = 1). Se toma el de mayor codigo, igual que
-- ContratoDaoServiceImpl.selectActivoPorEntidad (:34-46, order by codigo desc).
CONTRATO_ACTIVO AS (
        SELECT  c.ENTDCDGO, MAX(c.CNTRCDGO) AS CNTRCDGO
        FROM    CRD.CNTR c
        WHERE   c.CNTRESTD = 1
        GROUP BY c.ENTDCDGO
),
-- Meses en los que ese participe SI debia aportar ese tipo. Misma regla que
-- VigenciaContratoDaoServiceImpl.selectVigenteEnFecha (:83-89), evaluada contra el
-- ultimo dia del mes como hace esperadoEnLotePorFilial.
ESPERADO AS (
        SELECT  DISTINCT ca.ENTDCDGO, v.TPAPCDGO, m.MES
        FROM    MESES m
        CROSS   JOIN CONTRATO_ACTIVO ca
        JOIN    CRD.VGCN v ON v.CNTRCDGO = ca.CNTRCDGO
        WHERE   v.TPAPCDGO IN (9, 11)
        AND     v.VGCNIDST = 1
        AND     NVL(v.VGCNMNTO, 0) > 0
        AND     v.VGCNFCIN <= LAST_DAY(m.MES)
        AND     (v.VGCNFCFN IS NULL OR v.VGCNFCFN >= LAST_DAY(m.MES))
),
-- Un mes ocupado por una fila FIJA no es cupo: esa fila no se puede mover.
OCUPADO_FIJO AS (
        SELECT  DISTINCT a.ENTDCDGO, a.TPAPCDGO, a.PERIODO_EFECTIVO AS MES
        FROM    APORTES a
        WHERE   a.CLASE = 'FIJA'
        AND     a.PERIODO_EFECTIVO IS NOT NULL
),
CUPOS AS (
        SELECT  e.ENTDCDGO, e.TPAPCDGO, e.MES,
                ROW_NUMBER() OVER (PARTITION BY e.ENTDCDGO, e.TPAPCDGO ORDER BY e.MES) AS RN
        FROM    ESPERADO e
        WHERE   NOT EXISTS ( SELECT 1 FROM OCUPADO_FIJO o
                             WHERE o.ENTDCDGO = e.ENTDCDGO
                             AND   o.TPAPCDGO = e.TPAPCDGO
                             AND   o.MES      = e.MES )
),
MOVILES AS (
        SELECT  a.*,
                ROW_NUMBER() OVER (PARTITION BY a.ENTDCDGO, a.TPAPCDGO
                                   ORDER BY a.PERIODO_EFECTIVO, a.APRTCDGO) AS RN
        FROM    APORTES a
        JOIN    ELEGIBLE el ON el.ENTDCDGO = a.ENTDCDGO
        CROSS   JOIN PARAM p
        WHERE   a.CLASE = 'MOVIL'
        AND     a.PERIODO_EFECTIVO BETWEEN p.PISO AND p.TECHO
        AND     a.APRTGLSA NOT LIKE '%(reubicado desde%'      -- idempotencia
)
SELECT  e.ENTDNMID                                              AS IDENTIFICACION,
        e.ENTDRZNS                                              AS PARTICIPE,
        m.TPAPCDGO                                              AS TIPO,
        tp.TPAPNMBR                                             AS TIPO_NOMBRE,
        m.APRTCDGO                                              AS ID_APORTE,
        ROUND(m.APRTVLRR, 2)                                    AS VALOR,
        TO_CHAR(m.PERIODO_EFECTIVO, 'MM/YYYY')                  AS MES_ACTUAL,
        TO_CHAR(c.MES, 'MM/YYYY')                               AS MES_DESTINO,
        CASE WHEN m.APRTPRDV IS NULL
             THEN 'ESCRIBE devengo (hoy NULL: cae por fecha de caja)'
             ELSE 'MUEVE devengo' END                           AS ACCION,
        m.APRTPRDV                                              AS APRTPRDV_ACTUAL,
        c.MES                                                   AS APRTPRDV_NUEVO,
        m.APRTFCTR                                              AS APRTFCTR_SIN_CAMBIO,
        m.APRTGLSA                                              AS GLOSA_ACTUAL,
        CASE
            WHEN m.APRTGLSA LIKE 'Aporte % - Mes %/% - CargaArchivo: %'
            THEN REGEXP_REPLACE(m.APRTGLSA,
                    ' - Mes [0-9]{1,2}/[0-9]{4} - CargaArchivo: ',
                    ' - Mes ' || TO_CHAR(EXTRACT(MONTH FROM c.MES)) || '/' || TO_CHAR(EXTRACT(YEAR FROM c.MES))
                    || ' (reubicado desde ' || TO_CHAR(EXTRACT(MONTH FROM m.PERIODO_EFECTIVO))
                    || '/' || TO_CHAR(EXTRACT(YEAR FROM m.PERIODO_EFECTIVO)) || ')'
                    || ' - CargaArchivo: ')
            ELSE REGEXP_REPLACE(m.APRTGLSA,
                    ' - CargaArchivo: ',
                    ' (reubicado desde ' || TO_CHAR(EXTRACT(MONTH FROM m.PERIODO_EFECTIVO))
                    || '/' || TO_CHAR(EXTRACT(YEAR FROM m.PERIODO_EFECTIVO)) || ')'
                    || ' - CargaArchivo: ')
        END                                                     AS GLOSA_NUEVA
FROM    MOVILES m
JOIN    CUPOS c   ON c.ENTDCDGO = m.ENTDCDGO AND c.TPAPCDGO = m.TPAPCDGO AND c.RN = m.RN
JOIN    CRD.ENTD e ON e.ENTDCDGO = m.ENTDCDGO
JOIN    CRD.TPAP tp ON tp.TPAPCDGO = m.TPAPCDGO
WHERE   c.MES <> m.PERIODO_EFECTIVO          -- solo lo que de verdad cambia
ORDER BY e.ENTDNMID, m.TPAPCDGO, c.MES;


-- =============================================================================
-- 3. RESUMEN POR PARTICIPE — para dimensionar antes de leer el detalle
-- =============================================================================
-- Repite el calculo del bloque 2 y lo agrega. Si el bloque 2 devolvio muchas
-- filas, mirar este primero.
-- =============================================================================
WITH PARAM AS (
        SELECT  DATE '2025-06-01' AS PISO,
                (SELECT MAX(TO_DATE(TO_CHAR(c.CRARANAF) || LPAD(TO_CHAR(c.CRARMSAF), 2, '0'), 'YYYYMM'))
                   FROM CRD.CRAR c WHERE c.CRARESTD = 3) AS TECHO
        FROM DUAL
),
MESES AS (
        SELECT  ADD_MONTHS(p.PISO, LEVEL - 1) AS MES
        FROM    PARAM p
        CONNECT BY LEVEL <= MONTHS_BETWEEN(p.TECHO, p.PISO) + 1
),
APORTES AS (
        SELECT  a.APRTCDGO, a.ENTDCDGO, a.TPAPCDGO, a.APRTVLRR, a.APRTGLSA, a.APRTPRDV,
                CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                     ELSE TRUNC(a.APRTFCTR, 'MM') END               AS PERIODO_EFECTIVO,
                CASE WHEN (   a.CRARCDGO IS NOT NULL
                           OR (    a.APRTFCTR >= DATE '2025-06-01'
                               AND (   EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
                                    OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                                    OR a.APRTGLSA LIKE 'Abono al aporte%')))
                     THEN 'MOVIL' ELSE 'FIJA' END                    AS CLASE
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (9, 11)
        AND     a.APRTVLRR > 0
        AND     NVL(a.APRTTPMV, 1) <> 8
        AND     a.APRTGLSA NOT LIKE 'REVERSO%'
        AND     a.APRTGLSA NOT LIKE 'PAGO PRESTAMO%'
        AND     a.APRTGLSA NOT LIKE 'DEVOLUCION%'
),
DESCONTADO AS (
        SELECT  e.ENTDCDGO, SUM(NVL(x.PXCADSDO, 0)) AS DESCONTADO
        FROM    CRD.DTCA d
        JOIN    CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
        JOIN    CRD.CRAR c ON c.CRARCDGO = d.CRARCDGO
        JOIN    CRD.ENTD e ON e.ENTDRLPC = x.PXCACDPT
        WHERE   d.DTCACDPP = 'AH'
        AND     c.CRARESTD = 3
        GROUP BY e.ENTDCDGO
),
ELEGIBLE AS (
        SELECT  a.ENTDCDGO
        FROM    APORTES a
        LEFT    JOIN DESCONTADO d ON d.ENTDCDGO = a.ENTDCDGO
        WHERE   a.CLASE = 'MOVIL'
        GROUP BY a.ENTDCDGO, d.DESCONTADO
        HAVING  ABS(SUM(a.APRTVLRR) - NVL(MAX(d.DESCONTADO), 0)) <= 0.02
),
CONTRATO_ACTIVO AS (
        SELECT c.ENTDCDGO, MAX(c.CNTRCDGO) AS CNTRCDGO FROM CRD.CNTR c WHERE c.CNTRESTD = 1 GROUP BY c.ENTDCDGO
),
ESPERADO AS (
        SELECT  DISTINCT ca.ENTDCDGO, v.TPAPCDGO, m.MES
        FROM    MESES m
        CROSS   JOIN CONTRATO_ACTIVO ca
        JOIN    CRD.VGCN v ON v.CNTRCDGO = ca.CNTRCDGO
        WHERE   v.TPAPCDGO IN (9, 11)
        AND     v.VGCNIDST = 1
        AND     NVL(v.VGCNMNTO, 0) > 0
        AND     v.VGCNFCIN <= LAST_DAY(m.MES)
        AND     (v.VGCNFCFN IS NULL OR v.VGCNFCFN >= LAST_DAY(m.MES))
),
OCUPADO_FIJO AS (
        SELECT DISTINCT a.ENTDCDGO, a.TPAPCDGO, a.PERIODO_EFECTIVO AS MES
        FROM   APORTES a WHERE a.CLASE = 'FIJA' AND a.PERIODO_EFECTIVO IS NOT NULL
),
CUPOS AS (
        SELECT  e.ENTDCDGO, e.TPAPCDGO, e.MES,
                ROW_NUMBER() OVER (PARTITION BY e.ENTDCDGO, e.TPAPCDGO ORDER BY e.MES) AS RN
        FROM    ESPERADO e
        WHERE   NOT EXISTS ( SELECT 1 FROM OCUPADO_FIJO o
                             WHERE o.ENTDCDGO = e.ENTDCDGO AND o.TPAPCDGO = e.TPAPCDGO AND o.MES = e.MES )
),
MOVILES AS (
        SELECT  a.ENTDCDGO, a.TPAPCDGO, a.APRTCDGO, a.APRTVLRR, a.PERIODO_EFECTIVO,
                ROW_NUMBER() OVER (PARTITION BY a.ENTDCDGO, a.TPAPCDGO
                                   ORDER BY a.PERIODO_EFECTIVO, a.APRTCDGO) AS RN
        FROM    APORTES a
        JOIN    ELEGIBLE el ON el.ENTDCDGO = a.ENTDCDGO
        CROSS   JOIN PARAM p
        WHERE   a.CLASE = 'MOVIL'
        AND     a.PERIODO_EFECTIVO BETWEEN p.PISO AND p.TECHO
        AND     a.APRTGLSA NOT LIKE '%(reubicado desde%'
)
SELECT  e.ENTDNMID                                              AS IDENTIFICACION,
        e.ENTDRZNS                                              AS PARTICIPE,
        m.TPAPCDGO                                              AS TIPO,
        COUNT(*)                                                AS FILAS_MOVILES,
        SUM(CASE WHEN c.MES <> m.PERIODO_EFECTIVO THEN 1 ELSE 0 END) AS FILAS_A_MOVER,
        MIN(TO_CHAR(m.PERIODO_EFECTIVO, 'MM/YYYY'))             AS PRIMER_MES_ACTUAL,
        MIN(TO_CHAR(c.MES, 'MM/YYYY'))                          AS PRIMER_MES_DESTINO,
        ROUND(SUM(m.APRTVLRR), 2)                               AS VALOR_INVOLUCRADO
FROM    MOVILES m
JOIN    CUPOS c   ON c.ENTDCDGO = m.ENTDCDGO AND c.TPAPCDGO = m.TPAPCDGO AND c.RN = m.RN
JOIN    CRD.ENTD e ON e.ENTDCDGO = m.ENTDCDGO
GROUP BY e.ENTDNMID, e.ENTDRZNS, m.TPAPCDGO
HAVING  SUM(CASE WHEN c.MES <> m.PERIODO_EFECTIVO THEN 1 ELSE 0 END) > 0
ORDER BY 5 DESC, 1;


-- =============================================================================
-- 4. SOBRANTES — mas filas moviles que cupos disponibles
-- =============================================================================
-- Un participe con mas aportes que meses esperados. Despues de depurar el exceso
-- esto NO deberia aparecer; si aparece, va a REVISION INDIVIDUAL y no al lote.
-- Se listan los participes, no las filas: son casos a mirar de a uno.
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
        SELECT  a.APRTCDGO, a.ENTDCDGO, a.TPAPCDGO, a.APRTVLRR,
                CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                     ELSE TRUNC(a.APRTFCTR, 'MM') END           AS PERIODO_EFECTIVO,
                CASE WHEN (   a.CRARCDGO IS NOT NULL
                           OR (    a.APRTFCTR >= DATE '2025-06-01'
                               AND (   EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
                                    OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                                    OR a.APRTGLSA LIKE 'Abono al aporte%')))
                     THEN 'MOVIL' ELSE 'FIJA' END                AS CLASE
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
        AND     NVL(a.APRTTPMV, 1) <> 8
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
        FROM    APORTES a
        CROSS   JOIN PARAM p
        WHERE   a.CLASE = 'MOVIL' AND a.PERIODO_EFECTIVO BETWEEN p.PISO AND p.TECHO
        GROUP BY a.ENTDCDGO, a.TPAPCDGO
)
SELECT  e.ENTDNMID                          AS IDENTIFICACION,
        e.ENTDRZNS                          AS PARTICIPE,
        mn.TPAPCDGO                         AS TIPO,
        mn.FILAS                            AS FILAS_MOVILES,
        NVL(cn.CUPOS, 0)                    AS CUPOS_DISPONIBLES,
        mn.FILAS - NVL(cn.CUPOS, 0)         AS SOBRANTES,
        ROUND(mn.VALOR, 2)                  AS VALOR_TOTAL
FROM    MOVILES_N mn
LEFT    JOIN CUPOS_N cn ON cn.ENTDCDGO = mn.ENTDCDGO AND cn.TPAPCDGO = mn.TPAPCDGO
JOIN    CRD.ENTD e ON e.ENTDCDGO = mn.ENTDCDGO
WHERE   mn.FILAS > NVL(cn.CUPOS, 0)
ORDER BY 6 DESC, 1;


-- =============================================================================
-- 5. HUECOS QUE QUEDARIAN SIN CUBRIR DESPUES DE REUBICAR
-- =============================================================================
-- Meses esperados sin ninguna fila, una vez aplicada la propuesta. Son deuda real
-- del participe: NO se inventan filas para taparlos — los cobra la generacion por
-- el camino del faltante. Este bloque existe para poder decir cuanta deuda queda,
-- no para corregirla.
-- Se cuenta a nivel agregado: cupos totales menos filas moviles.
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
                     ELSE TRUNC(a.APRTFCTR, 'MM') END           AS PERIODO_EFECTIVO
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
)
SELECT  esp.TPAPCDGO                                        AS TIPO,
        COUNT(*)                                            AS MESES_ESPERADOS,
        SUM(CASE WHEN ap.FILAS > 0 THEN 1 ELSE 0 END)       AS MESES_CON_APORTE_HOY,
        SUM(CASE WHEN NVL(ap.FILAS, 0) = 0 THEN 1 ELSE 0 END) AS MESES_HUECOS_HOY,
        COUNT(DISTINCT esp.ENTDCDGO)                        AS PARTICIPES
FROM    ESPERADO esp
LEFT    JOIN ( SELECT ENTDCDGO, TPAPCDGO, PERIODO_EFECTIVO, COUNT(*) AS FILAS
               FROM   APORTES GROUP BY ENTDCDGO, TPAPCDGO, PERIODO_EFECTIVO ) ap
        ON  ap.ENTDCDGO = esp.ENTDCDGO AND ap.TPAPCDGO = esp.TPAPCDGO AND ap.PERIODO_EFECTIVO = esp.MES
GROUP BY esp.TPAPCDGO
ORDER BY 1;


-- =============================================================================
-- FIN. Nada de este script modifica datos.
-- El UPDATE (04_) se escribe con la salida del bloque 2 aprobada, y lleva tabla
-- de respaldo, controles antes/despues y bloque de reverso comentado.
-- =============================================================================

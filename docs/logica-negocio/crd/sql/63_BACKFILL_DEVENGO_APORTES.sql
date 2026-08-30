-- =============================================================================
-- 63 - BACKFILL DE DEVENGO DE APORTES (CRD.APRT.APRTPRDV / APRTTPMV)
-- =============================================================================
-- Fase 2 del plan de devengo de aportes
-- (docs/logica-negocio/crd/PLAN-APORTES-DEVENGO-CONTRATOS.md). Leer ese documento y
-- docs/logica-negocio/petro/REGLAS-CARGA-PETRO.md §3.6 antes de correr esto.
--
-- ⚠ ORDEN OBLIGATORIO: ejecutar DESPUÉS de 65_RECONSTRUCCION_APORTES_JUNIO_2025.sql. Si
-- este script (63) se corre ANTES, la Regla 2 (bloque 3, reconstrucción hacia atrás) asigna
-- filas hacia atrás hasta junio 2025 -- que en ese momento todavía estaría vacío -- y
-- despues el 65 volvería a insertar ahí: junio quedaría contado dos veces.
--
-- ALCANCE (D11): filas con APRTFCTR >= 2025-06-01. LO ANTERIOR QUEDA CON DEVENGO NULL A
-- PROPOSITO: las consultas de cartera leen SIEMPRE NVL(APRTPRDV, TRUNC(APRTFCTR,'MM')), así
-- que ese histórico sigue respondiendo exactamente igual que hoy.
--
-- REGLA 1 (DIRECTA) — dato confiable:
--   Filas de la carga Petro (producto AH: APRTUSRG = 'SAA_AH' o glosa
--   'Aporte %CargaArchivo: %') de tipos 9 (jubilación) y 11 (cesantía) donde la entidad
--   tiene UNA SOLA fila de ese tipo en ese mes de caja -> devengo = TRUNC(APRTFCTR,'MM').
--   Un solo pago ese mes solo puede ser el aporte de ese mes.
--
-- REGLA 2 (RECONSTRUCCIÓN POR REGLA — NO ES UN DATO RECUPERADO):
--   Cuando hay VARIAS filas del mismo tipo en el mismo mes de caja (cobro de meses
--   atrasados en una sola carga), se asignan HACIA ATRÁS, a los meses consecutivos
--   anteriores SIN devengo de ese tipo, empezando por el más antiguo: la fila más antigua
--   del grupo (por fecha de registro) va al mes disponible más lejano, la más nueva al mes
--   disponible más cercano (típicamente el propio mes de caja).
--   ESTO ES UNA RECONSTRUCCIÓN POR REGLA, ASUMIENDO que el atraso se cobró completo y en
--   orden cronológico estricto — no es un hecho verificado fila por fila. La glosa de TODAS
--   las filas del grupo dice el mes de la CARGA (no el mes cobrado) y CRD.CXPG guarda el
--   monto ya multiplicado por los meses atrasados: no existe ninguna fuente que enumere qué
--   meses fueron. Si un grupo tiene más filas que meses disponibles sin devengo en la
--   ventana de búsqueda (24 meses), las filas sobrantes del grupo QUEDAN SIN ASIGNAR
--   (APRTPRDV sigue NULL) en vez de adivinar: revisarlas a mano (ver control 0.4).
--
-- REGLA 3 (TIPO DE MOVIMIENTO) — aplica a TODAS las filas del alcance, no solo a la carga:
--   glosa 'Aporte %CargaArchivo: %'      y valor > 0  -> APORTE_MENSUAL (1)
--   glosa 'REGISTRO APORTE %'            y valor > 0  -> AJUSTE_MANUAL (2)
--   glosa 'DEVOLUCION APORTES %'         y valor < 0  -> DEVOLUCION (3)
--   glosa 'PAGO PRESTAMO %'              y valor < 0  -> PAGO_PRESTAMO (4)
--   glosa 'REVERSO %'                    (cualquier signo) -> REVERSO (5)
--   cualquier otra cosa                                -> MIGRADO (6)
--
-- NO SE EJECUTA AUTOMÁTICAMENTE. SQL puro (sin SET/DEFINE/WHENEVER). El usuario corre cada
-- bloque a mano en un cliente JDBC, revisando el resultado del bloque anterior antes de
-- seguir.
--
-- ÍNDICE
--   0. Controles PREVIOS (leer antes de correr nada)
--   1. Respaldo — CRD.BKP_APRT_DEVENGO_<fecha>
--   2. Regla 1 — UPDATE directo
--   3. Regla 2 — MERGE de reconstrucción hacia atrás
--   4. Regla 3 — UPDATE de tipoMovimiento
--   5. SELECT de verificación (después)
--   6. Reverso desde el respaldo
-- =============================================================================


-- =============================================================================
-- 0. CONTROLES PREVIOS
-- =============================================================================

-- 0.1 Volumen total del alcance y cuánto ya tiene devengo (debe ser 0 antes de correr esto
--     por primera vez; si no es 0, ya se corrió parcialmente — revisar antes de repetir).
SELECT  COUNT(*)                                                    AS FILAS_EN_ALCANCE,
        SUM(CASE WHEN a.APRTPRDV IS NOT NULL THEN 1 ELSE 0 END)     AS YA_CON_DEVENGO,
        SUM(CASE WHEN a.APRTTPMV IS NOT NULL THEN 1 ELSE 0 END)     AS YA_CON_TIPO_MOVIMIENTO
FROM    CRD.APRT a
WHERE   a.APRTFCTR >= DATE '2025-06-01';

-- 0.2 Regla 1: cuántas filas de carga son grupo de UNA sola fila (se resuelven directo).
SELECT  COUNT(*) AS FILAS_REGLA_1
FROM  ( SELECT  a.APRTCDGO,
                COUNT(*) OVER (PARTITION BY a.ENTDCDGO, a.TPAPCDGO, TRUNC(a.APRTFCTR,'MM')) AS FILAS_EN_GRUPO
        FROM    CRD.APRT a
        WHERE   a.APRTFCTR >= DATE '2025-06-01'
        AND     a.APRTPRDV IS NULL
        AND     a.TPAPCDGO IN (9, 11)
        AND     (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %') ) x
WHERE   x.FILAS_EN_GRUPO = 1;

-- 0.3 Regla 2: grupos con más de una fila (candidatos a reconstrucción), y cuántas filas
--     suman en total.
SELECT  COUNT(*)         AS GRUPOS_MULTIPLES,
        SUM(FILAS_EN_GRUPO) AS FILAS_EN_GRUPOS_MULTIPLES
FROM  ( SELECT  a.ENTDCDGO, a.TPAPCDGO, TRUNC(a.APRTFCTR,'MM') AS MES_CAJA,
                COUNT(*) AS FILAS_EN_GRUPO
        FROM    CRD.APRT a
        WHERE   a.APRTFCTR >= DATE '2025-06-01'
        AND     a.APRTPRDV IS NULL
        AND     a.TPAPCDGO IN (9, 11)
        AND     (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %')
        GROUP BY a.ENTDCDGO, a.TPAPCDGO, TRUNC(a.APRTFCTR,'MM')
        HAVING  COUNT(*) > 1 ) x;

-- 0.4 Regla 2: de esas filas, cuántas la reconstrucción NO logra resolver (el grupo tiene
--     más filas que meses sin devengo disponibles en la ventana de 24 meses hacia atrás).
--     Estas quedan con APRTPRDV NULL después del bloque 3 — revisarlas a mano.
--     (Misma lógica que el bloque 3; ver ahí los comentarios de cada CTE.)
WITH grupos_multiples AS (
        SELECT  a.APRTCDGO, a.ENTDCDGO, a.TPAPCDGO, TRUNC(a.APRTFCTR,'MM') AS MES_CAJA,
                ROW_NUMBER() OVER (PARTITION BY a.ENTDCDGO, a.TPAPCDGO, TRUNC(a.APRTFCTR,'MM')
                                   ORDER BY a.APRTFCRG, a.APRTCDGO) AS ORDEN_EN_GRUPO,
                COUNT(*) OVER (PARTITION BY a.ENTDCDGO, a.TPAPCDGO, TRUNC(a.APRTFCTR,'MM')) AS FILAS_EN_GRUPO
        FROM    CRD.APRT a
        WHERE   a.APRTFCTR >= DATE '2025-06-01'
        AND     a.APRTPRDV IS NULL
        AND     a.TPAPCDGO IN (9, 11)
        AND     (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %')
),
solo_multiples AS (
        SELECT * FROM grupos_multiples WHERE FILAS_EN_GRUPO > 1
),
meses_cubiertos AS (
        SELECT DISTINCT a.ENTDCDGO, a.TPAPCDGO, a.APRTPRDV AS MES
        FROM    CRD.APRT a
        WHERE   a.APRTPRDV IS NOT NULL
),
meses_atras AS (
        SELECT DISTINCT g.ENTDCDGO, g.TPAPCDGO, g.MES_CAJA,
                ADD_MONTHS(g.MES_CAJA, -(NIVEL.N)) AS MES_CANDIDATO, NIVEL.N AS DESPLAZAMIENTO
        FROM  ( SELECT DISTINCT ENTDCDGO, TPAPCDGO, MES_CAJA FROM solo_multiples ) g
        CROSS JOIN ( SELECT LEVEL - 1 AS N FROM DUAL CONNECT BY LEVEL <= 24 ) NIVEL
),
meses_disponibles AS (
        SELECT  ma.ENTDCDGO, ma.TPAPCDGO, ma.MES_CAJA, ma.MES_CANDIDATO,
                ROW_NUMBER() OVER (PARTITION BY ma.ENTDCDGO, ma.TPAPCDGO, ma.MES_CAJA
                                   ORDER BY ma.DESPLAZAMIENTO) AS ORDEN_DISPONIBLE
        FROM    meses_atras ma
        WHERE   NOT EXISTS ( SELECT 1 FROM meses_cubiertos mc
                             WHERE mc.ENTDCDGO = ma.ENTDCDGO AND mc.TPAPCDGO = ma.TPAPCDGO
                             AND   mc.MES = ma.MES_CANDIDATO )
)
SELECT  e.ENTDNMID AS NUMERO_IDENTIFICACION, e.ENTDRZNS AS RAZON_SOCIAL,
        sm.TPAPCDGO, sm.MES_CAJA, sm.FILAS_EN_GRUPO, sm.APRTCDGO, sm.ORDEN_EN_GRUPO
FROM    solo_multiples sm
JOIN    CRD.ENTD e ON e.ENTDCDGO = sm.ENTDCDGO
WHERE   NOT EXISTS ( SELECT 1 FROM meses_disponibles md
                     WHERE md.ENTDCDGO = sm.ENTDCDGO AND md.TPAPCDGO = sm.TPAPCDGO
                     AND   md.MES_CAJA = sm.MES_CAJA
                     AND   md.ORDEN_DISPONIBLE = (sm.FILAS_EN_GRUPO - sm.ORDEN_EN_GRUPO + 1) )
ORDER BY 1, 2, 4;

-- 0.5 Regla 3: distribución de glosas que NO calzan con ningún patrón conocido (van a
--     MIGRADO). Revisar que la lista tenga sentido antes de aceptar el default.
SELECT  SUBSTR(a.APRTGLSA, 1, 40) AS GLOSA_MUESTRA, COUNT(*) AS FILAS
FROM    CRD.APRT a
WHERE   a.APRTFCTR >= DATE '2025-06-01'
AND     a.APRTGLSA NOT LIKE 'Aporte %CargaArchivo: %'
AND     a.APRTGLSA NOT LIKE 'REGISTRO APORTE %'
AND     a.APRTGLSA NOT LIKE 'DEVOLUCION APORTES %'
AND     a.APRTGLSA NOT LIKE 'PAGO PRESTAMO %'
AND     a.APRTGLSA NOT LIKE 'REVERSO %'
GROUP BY SUBSTR(a.APRTGLSA, 1, 40)
ORDER BY COUNT(*) DESC;


-- =============================================================================
-- 1. RESPALDO — antes de tocar nada
-- =============================================================================
-- Cambiar <fecha> por la fecha de ejecución real (ej. BKP_APRT_DEVENGO_20260827).
-- =============================================================================
CREATE TABLE CRD.BKP_APRT_DEVENGO_20260827 AS
SELECT  a.*
FROM    CRD.APRT a
WHERE   a.APRTFCTR >= DATE '2025-06-01';

SELECT COUNT(*) AS FILAS_RESPALDADAS FROM CRD.BKP_APRT_DEVENGO_20260827;


-- =============================================================================
-- 2. REGLA 1 — UPDATE DIRECTO
-- =============================================================================
UPDATE  CRD.APRT a
SET     a.APRTPRDV = TRUNC(a.APRTFCTR, 'MM')
WHERE   a.APRTFCTR >= DATE '2025-06-01'
AND     a.APRTPRDV IS NULL
AND     a.TPAPCDGO IN (9, 11)
AND     (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %')
AND     1 = ( SELECT COUNT(*) FROM CRD.APRT b
              WHERE b.ENTDCDGO = a.ENTDCDGO AND b.TPAPCDGO = a.TPAPCDGO
              AND   TRUNC(b.APRTFCTR,'MM') = TRUNC(a.APRTFCTR,'MM')
              AND   b.APRTFCTR >= DATE '2025-06-01' AND b.APRTPRDV IS NULL
              AND   (b.APRTUSRG = 'SAA_AH' OR b.APRTGLSA LIKE 'Aporte %CargaArchivo: %') );

COMMIT;

-- Verificación: debe coincidir con el control 0.2.
SELECT COUNT(*) AS FILAS_CON_DEVENGO_REGLA_1
FROM   CRD.APRT a
WHERE  a.APRTFCTR >= DATE '2025-06-01' AND a.APRTPRDV = TRUNC(a.APRTFCTR,'MM')
AND    a.TPAPCDGO IN (9, 11) AND (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %');


-- =============================================================================
-- 3. REGLA 2 — RECONSTRUCCIÓN HACIA ATRÁS (MERGE)
-- =============================================================================
-- Correr DESPUÉS del bloque 2: usa meses_cubiertos, que ya incluye lo que dejó la Regla 1.
-- =============================================================================
MERGE INTO CRD.APRT a
USING (
    WITH grupos_multiples AS (
            SELECT  a.APRTCDGO, a.ENTDCDGO, a.TPAPCDGO, TRUNC(a.APRTFCTR,'MM') AS MES_CAJA,
                    ROW_NUMBER() OVER (PARTITION BY a.ENTDCDGO, a.TPAPCDGO, TRUNC(a.APRTFCTR,'MM')
                                       ORDER BY a.APRTFCRG, a.APRTCDGO) AS ORDEN_EN_GRUPO,
                    COUNT(*) OVER (PARTITION BY a.ENTDCDGO, a.TPAPCDGO, TRUNC(a.APRTFCTR,'MM')) AS FILAS_EN_GRUPO
            FROM    CRD.APRT a
            WHERE   a.APRTFCTR >= DATE '2025-06-01'
            AND     a.APRTPRDV IS NULL
            AND     a.TPAPCDGO IN (9, 11)
            AND     (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %')
    ),
    solo_multiples AS (
            SELECT * FROM grupos_multiples WHERE FILAS_EN_GRUPO > 1
    ),
    meses_cubiertos AS (
            SELECT DISTINCT a.ENTDCDGO, a.TPAPCDGO, a.APRTPRDV AS MES
            FROM    CRD.APRT a
            WHERE   a.APRTPRDV IS NOT NULL
    ),
    meses_atras AS (
            SELECT DISTINCT g.ENTDCDGO, g.TPAPCDGO, g.MES_CAJA,
                    ADD_MONTHS(g.MES_CAJA, -(NIVEL.N)) AS MES_CANDIDATO, NIVEL.N AS DESPLAZAMIENTO
            FROM  ( SELECT DISTINCT ENTDCDGO, TPAPCDGO, MES_CAJA FROM solo_multiples ) g
            CROSS JOIN ( SELECT LEVEL - 1 AS N FROM DUAL CONNECT BY LEVEL <= 24 ) NIVEL
    ),
    meses_disponibles AS (
            SELECT  ma.ENTDCDGO, ma.TPAPCDGO, ma.MES_CAJA, ma.MES_CANDIDATO,
                    ROW_NUMBER() OVER (PARTITION BY ma.ENTDCDGO, ma.TPAPCDGO, ma.MES_CAJA
                                       ORDER BY ma.DESPLAZAMIENTO) AS ORDEN_DISPONIBLE
            FROM    meses_atras ma
            WHERE   NOT EXISTS ( SELECT 1 FROM meses_cubiertos mc
                                 WHERE mc.ENTDCDGO = ma.ENTDCDGO AND mc.TPAPCDGO = ma.TPAPCDGO
                                 AND   mc.MES = ma.MES_CANDIDATO )
    )
    -- La fila MÁS ANTIGUA del grupo (ORDEN_EN_GRUPO=1) recibe el mes disponible MÁS LEJANO;
    -- la MÁS NUEVA (ORDEN_EN_GRUPO=FILAS_EN_GRUPO) recibe el más cercano (ORDEN_DISPONIBLE=1).
    SELECT  sm.APRTCDGO, md.MES_CANDIDATO AS MES_ASIGNADO
    FROM    solo_multiples sm
    JOIN    meses_disponibles md
      ON    md.ENTDCDGO = sm.ENTDCDGO AND md.TPAPCDGO = sm.TPAPCDGO AND md.MES_CAJA = sm.MES_CAJA
     AND    md.ORDEN_DISPONIBLE = (sm.FILAS_EN_GRUPO - sm.ORDEN_EN_GRUPO + 1)
) x
ON (a.APRTCDGO = x.APRTCDGO)
WHEN MATCHED THEN UPDATE SET a.APRTPRDV = x.MES_ASIGNADO;

COMMIT;

-- Verificación: filas de grupos múltiples que SIGUEN sin devengo (esperado: igual al 0.4).
SELECT  COUNT(*) AS FILAS_SIN_RESOLVER_REGLA_2
FROM  ( SELECT  a.APRTCDGO,
                COUNT(*) OVER (PARTITION BY a.ENTDCDGO, a.TPAPCDGO, TRUNC(a.APRTFCTR,'MM')) AS FILAS_EN_GRUPO
        FROM    CRD.APRT a
        WHERE   a.APRTFCTR >= DATE '2025-06-01'
        AND     a.APRTPRDV IS NULL
        AND     a.TPAPCDGO IN (9, 11)
        AND     (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %') ) x;


-- =============================================================================
-- 4. REGLA 3 — TIPO DE MOVIMIENTO (APRTTPMV, rubro 235)
-- =============================================================================
-- Aplica a TODAS las filas del alcance, no solo a las de la carga. Se corre en orden de
-- más específico a más general; MIGRADO es el default final para lo que no calzó.
-- =============================================================================
UPDATE CRD.APRT a SET a.APRTTPMV = 1  -- APORTE_MENSUAL
WHERE a.APRTFCTR >= DATE '2025-06-01' AND a.APRTTPMV IS NULL
AND   a.APRTGLSA LIKE 'Aporte %CargaArchivo: %' AND a.APRTVLRR > 0;

UPDATE CRD.APRT a SET a.APRTTPMV = 2  -- AJUSTE_MANUAL
WHERE a.APRTFCTR >= DATE '2025-06-01' AND a.APRTTPMV IS NULL
AND   a.APRTGLSA LIKE 'REGISTRO APORTE %' AND a.APRTVLRR > 0;

UPDATE CRD.APRT a SET a.APRTTPMV = 3  -- DEVOLUCION
WHERE a.APRTFCTR >= DATE '2025-06-01' AND a.APRTTPMV IS NULL
AND   a.APRTGLSA LIKE 'DEVOLUCION APORTES %' AND a.APRTVLRR < 0;

UPDATE CRD.APRT a SET a.APRTTPMV = 4  -- PAGO_PRESTAMO
WHERE a.APRTFCTR >= DATE '2025-06-01' AND a.APRTTPMV IS NULL
AND   a.APRTGLSA LIKE 'PAGO PRESTAMO %' AND a.APRTVLRR < 0;

UPDATE CRD.APRT a SET a.APRTTPMV = 5  -- REVERSO
WHERE a.APRTFCTR >= DATE '2025-06-01' AND a.APRTTPMV IS NULL
AND   a.APRTGLSA LIKE 'REVERSO %';

UPDATE CRD.APRT a SET a.APRTTPMV = 6  -- MIGRADO (default: nada de lo anterior calzó)
WHERE a.APRTFCTR >= DATE '2025-06-01' AND a.APRTTPMV IS NULL;

COMMIT;


-- =============================================================================
-- 5. SELECT DE VERIFICACIÓN (DESPUÉS)
-- =============================================================================

-- 5.1 Todas las filas del alcance deben tener tipoMovimiento. Esperado: 0.
SELECT COUNT(*) AS FILAS_SIN_TIPO_MOVIMIENTO
FROM   CRD.APRT a WHERE a.APRTFCTR >= DATE '2025-06-01' AND a.APRTTPMV IS NULL;

-- 5.2 Resumen de devengo y tipoMovimiento tras el backfill.
SELECT  CASE WHEN a.APRTPRDV IS NULL THEN 'SIN DEVENGO' ELSE 'CON DEVENGO' END AS ESTADO_DEVENGO,
        a.APRTTPMV, COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS VALOR
FROM    CRD.APRT a
WHERE   a.APRTFCTR >= DATE '2025-06-01'
GROUP BY CASE WHEN a.APRTPRDV IS NULL THEN 'SIN DEVENGO' ELSE 'CON DEVENGO' END, a.APRTTPMV
ORDER BY 1, 2;

-- 5.3 Guardarraíl del CHECK CK_APRT_PRDV_MES: ningún devengo asignado quedó fuera del
--     primer día del mes. Esperado: 0 (si esto fallara, el CHECK del DDL ya lo habría
--     bloqueado, pero sirve como doble verificación explícita).
SELECT COUNT(*) AS DEVENGOS_MAL_TRUNCADOS
FROM   CRD.APRT a
WHERE  a.APRTPRDV IS NOT NULL AND a.APRTPRDV <> TRUNC(a.APRTPRDV, 'MM');


-- =============================================================================
-- 6. REVERSO DESDE EL RESPALDO
-- =============================================================================
-- ⛔ COMENTADO A PROPOSITO. Corre SOLO si hay que deshacer el backfill.
--    Si el script se ejecuta de corrido con esto activo, revierte en silencio todo lo
--    que acaba de hacer y el resultado parece correcto.
--
-- UPDATE  CRD.APRT a
-- SET     (a.APRTPRDV, a.APRTTPMV) = (
--             SELECT  b.APRTPRDV, b.APRTTPMV
--             FROM    CRD.BKP_APRT_DEVENGO_20260827 b
--             WHERE   b.APRTCDGO = a.APRTCDGO
--         )
-- WHERE   a.APRTCDGO IN (SELECT b.APRTCDGO FROM CRD.BKP_APRT_DEVENGO_20260827 b);

COMMIT;

SELECT  COUNT(*) AS FILAS_QUE_NO_COINCIDEN_CON_RESPALDO
FROM    CRD.APRT a
JOIN    CRD.BKP_APRT_DEVENGO_20260827 b ON b.APRTCDGO = a.APRTCDGO
WHERE   NVL(TO_CHAR(a.APRTPRDV,'YYYYMMDD'), 'X') <> NVL(TO_CHAR(b.APRTPRDV,'YYYYMMDD'), 'X')
OR      NVL(a.APRTTPMV, -1) <> NVL(b.APRTTPMV, -1);

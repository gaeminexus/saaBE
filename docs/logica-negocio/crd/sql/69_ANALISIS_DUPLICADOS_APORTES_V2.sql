-- =============================================================================
-- 69 - ANALISIS DE APORTES DUPLICADOS GENERADOS POR LA CARGA PETRO (V2)
-- =============================================================================
-- FECHA: 2026-08-27
-- Reemplaza a 61_ANALISIS_APORTES_DUPLICADOS_PETRO.sql para el pedido 7 del cliente.
-- NO lo sustituye como documento de referencia: docs/logica-negocio/crd/
-- ANALISIS-APORTES-DUPLICADOS-PETRO.md (versiones del generador, mecanismos M1/M2/M3,
-- reglas R1-R6) sigue vigente. Lo que cambia aquí es el UNIVERSO de filas que las
-- consultas miran, no las reglas de negocio.
--
-- ⚠ POR QUÉ HIZO FALTA UNA V2 — EL 61 TENÍA EL UNIVERSO INCOMPLETO
--   El 61 construye su universo con `a.APRTUSRG = 'SAA_AH'` (o, en algunas consultas,
--   con el OR `a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'`, que ayuda pero no alcanza).
--   En producción aparecieron 2.635 filas de junio 2025 con:
--     - USUARIO NULL (no 'SAA_AH')
--     - glosa vieja 'Aporte jubilacion - CargaArchivo: 352' (SIN ' - Mes ')
--     - APRTFCTR = 2025-06-30 00:00:00 (medianoche, no 23:59:59 como el generador actual)
--     - SIN APRTIDAS
--     - SIN APRTFCRG (fecha de registro)
--   Las consultas A0 y A3b del 61 filtraban SOLO por `APRTUSRG = 'SAA_AH'` (sin el OR de
--   glosa) y nunca vieron estas filas. Y para colmo, A0/A2/A6 se corrieron DESPUÉS de que
--   62_CORRECCION_VALOR_APORTES_CARGA.sql (en su primera versión, ya corregida — ver su
--   encabezado) las puso en APRTVLRR = 0 con un NVL que confundió "no sé" con "cero": el
--   filtro `APRTVLRR > 0` que llevan casi todas las consultas del 61 las excluyó una
--   SEGUNDA vez, por una razón totalmente distinta a la del universo. Doble exclusión,
--   dos causas independientes: el pedido 7 se cerró sin haber visto nunca estas filas.
--
--   Conclusión: el universo se redefine SIN usar APRTUSRG para nada. Una fila se
--   considera "creada por la carga" únicamente por su FORMA (APRTIDAS, o el patrón de su
--   glosa), nunca por quién dice el campo usuario que la creó — ese campo puede faltar.
--
-- ⚠ PRECONDICIÓN OBLIGATORIA — LEER ANTES DE CORRER
--   Este script debe correrse en PRODUCCIÓN DESPUÉS de que se haya ejecutado
--   74_RESTAURACION_VALOR_APORTES_ANULADOS.sql (o en un ambiente que ya tenga ese
--   restauro aplicado). Mientras las 2.635 filas de junio 2025 sigan en APRTVLRR = 0, el
--   análisis vuelve a mentir exactamente por la razón que motivó esta V2: con valor 0,
--   el filtro `APRTVLRR > 0` las descarta y todas las cifras de abajo salen incompletas,
--   otra vez sin decirlo. Si el bloque 0.0 de control no muestra el aviso "OK, restaurado"
--   NO seguir leyendo el resto del script como definitivo.
--
-- IDENTIFICACIÓN DE "FILA CREADA POR LA CARGA" QUE USA ESTE SCRIPT (sin usuario, en
-- ningún punto):
--   APRTIDAS IS NOT NULL                                    (generador vigente, con FK)
--   OR APRTGLSA LIKE 'Aporte %CargaArchivo: %'               (V1 y V3, con o sin ' - Mes ')
--   OR APRTGLSA LIKE 'Abono al aporte%'                       (V2 excedente)
--
-- SOLO LECTURA. Ningún DML. No crea tablas.
--
-- ÍNDICE
--   0.0  Control de precondición — ¿ya se restauró el daño del 62?
--   0.1  Clasificación por versión del generador, SIN filtro de usuario (reemplaza A2 del
--        61), con la variante nueva (junio 2025, sin fecha de registro)
--   0.2  Usuarios que aparecen por versión — alerta temprana de variantes no previstas
--   1    Ejecuciones por carga: cuándo corrió cada una y cuántas veces (reemplaza A0)
--   2    Ratio ARCHIVO vs APLICADO vs FILAS, por carga (reemplaza A4)
--   3    Exceso total acumulado por partícipe y tipo — LA CIFRA A CORREGIR (reemplaza A6)
-- =============================================================================


-- =============================================================================
-- 0.0 CONTROL DE PRECONDICIÓN — ¿SE RESTAURÓ EL DAÑO DEL 62?
-- =============================================================================
-- Si RESTAURADO = 'NO' o FILAS_EN_CERO > 0, PARAR: correr primero
-- 74_RESTAURACION_VALOR_APORTES_ANULADOS.sql (o confirmar que ya corrió) antes de leer
-- cualquier otro resultado de este script.
-- =============================================================================
SELECT  CASE WHEN COUNT(*) = 0 THEN 'OK, RESTAURADO' ELSE 'NO — CORRER 74 ANTES DE SEGUIR' END AS ESTADO,
        COUNT(*) AS FILAS_TODAVIA_EN_CERO,
        SUM(b.APRTVLRR) AS VALOR_PENDIENTE_DE_RESTAURAR
FROM    CRD.APRT a
JOIN    CRD.BKP_APRT_VALOR_20260827 b ON b.APRTCDGO = a.APRTCDGO
WHERE   NVL(a.APRTVLRR, 0) = 0
AND     b.APRTVLRR > 0;
-- Nota: si CRD.BKP_APRT_VALOR_20260827 ya no existe (el usuario la eliminó tras validar
-- la restauración), esta consulta falla con "tabla no existe" — eso también es una señal
-- de que la restauración ya se hizo; seguir con el resto del script en ese caso.


-- =============================================================================
-- 0.1 CLASIFICACION POR VERSION DEL GENERADOR — SIN FILTRO DE USUARIO
-- =============================================================================
-- Universo: TODAS las filas positivas de APRT tipo 9/11, sin condición alguna sobre
-- APRTUSRG. Reemplaza a A2 del 61.
--   V3 VIGENTE            glosa 'Aporte X - Mes m/aaaa - CargaArchivo: N' (incluye los
--                         "excedente" del script 66, que llevan ' - Mes ' también)
--   V2 EXCEDENTE (vieja)  glosa 'Abono al aporte...' (versión 2026-04-09..11, anterior al
--                         formato "excedente" del 66)
--   V1 CON REGISTRO       glosa 'Aporte %CargaArchivo: %' sin ' - Mes ', sin APRTIDAS,
--                         CON fecha de registro (APRTFCRG) — la V1 ya conocida por el 61.
--   V1 SIN FECHA DE REGISTRO   la variante que el 61 nunca vio: mismo patrón de glosa,
--                         sin APRTIDAS, sin usuario, SIN APRTFCRG, y APRTFCTR a las
--                         00:00:00 en vez de 23:59:59. Es la firma exacta de las 2.635
--                         filas de junio 2025.
--   OTRA CON CARGA        tiene APRTIDAS pero no calza ningún patrón de glosa conocido.
--   MANUAL / MIGRADA      todo lo demás (no tiene forma de fila de carga).
-- =============================================================================
SELECT  CASE
            WHEN a.APRTGLSA LIKE 'Abono al aporte%'                              THEN 'V2 EXCEDENTE (vieja)'
            WHEN a.APRTGLSA LIKE 'Aporte % - Mes %/% - CargaArchivo: %'         THEN 'V3 VIGENTE'
            WHEN a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                 AND a.APRTGLSA NOT LIKE '%- Mes %'
                 AND a.APRTIDAS IS NULL
                 AND a.APRTFCRG IS NULL
                 AND a.APRTFCTR = TRUNC(a.APRTFCTR)                              THEN 'V1 SIN FECHA DE REGISTRO (hallada en produccion)'
            WHEN a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                 AND a.APRTGLSA NOT LIKE '%- Mes %'
                 AND a.APRTIDAS IS NULL                                          THEN 'V1 CON REGISTRO'
            WHEN a.APRTIDAS IS NOT NULL                                          THEN 'OTRA CON CARGA'
            ELSE                                                                      'MANUAL / MIGRADA'
        END                                          AS VERSION,
        COUNT(*)                                     AS FILAS,
        COUNT(DISTINCT a.ENTDCDGO)                   AS PARTICIPES,
        MIN(a.APRTFCTR)                              AS MIN_FECHA_TRANSACCION,
        MAX(a.APRTFCTR)                              AS MAX_FECHA_TRANSACCION,
        SUM(a.APRTVLRR)                              AS VALOR,
        SUM(NVL(a.APRTVLPG, 0))                      AS VALOR_PAGADO,
        SUM(NVL(a.APRTSLDO, 0))                      AS SALDO_FIFO,
        SUM(CASE WHEN a.APRTUSRG IS NULL THEN 1 ELSE 0 END) AS FILAS_SIN_USUARIO
FROM    CRD.APRT a
WHERE   a.APRTVLRR > 0
AND     a.TPAPCDGO IN (9, 11)
GROUP BY CASE
            WHEN a.APRTGLSA LIKE 'Abono al aporte%'                              THEN 'V2 EXCEDENTE (vieja)'
            WHEN a.APRTGLSA LIKE 'Aporte % - Mes %/% - CargaArchivo: %'         THEN 'V3 VIGENTE'
            WHEN a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                 AND a.APRTGLSA NOT LIKE '%- Mes %'
                 AND a.APRTIDAS IS NULL
                 AND a.APRTFCRG IS NULL
                 AND a.APRTFCTR = TRUNC(a.APRTFCTR)                              THEN 'V1 SIN FECHA DE REGISTRO (hallada en produccion)'
            WHEN a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                 AND a.APRTGLSA NOT LIKE '%- Mes %'
                 AND a.APRTIDAS IS NULL                                          THEN 'V1 CON REGISTRO'
            WHEN a.APRTIDAS IS NOT NULL                                          THEN 'OTRA CON CARGA'
            ELSE                                                                      'MANUAL / MIGRADA'
         END
ORDER BY 1;


-- =============================================================================
-- 0.2 USUARIOS QUE APARECEN POR VERSION — alerta temprana de variantes no previstas
-- =============================================================================
-- Solo sobre filas con forma de carga (APRTIDAS o glosa conocida). Si aparece un usuario
-- distinto de 'SAA_AH' o NULL en V1/V3, es una variante que este script todavía no
-- clasifica: revisar antes de confiar en los totales de abajo.
-- =============================================================================
SELECT  CASE
            WHEN a.APRTGLSA LIKE 'Abono al aporte%'                       THEN 'V2 EXCEDENTE (vieja)'
            WHEN a.APRTGLSA LIKE 'Aporte % - Mes %/% - CargaArchivo: %'  THEN 'V3 VIGENTE'
            WHEN a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'                THEN 'V1 (alguna variante)'
            ELSE                                                               'OTRA CON CARGA'
        END                                          AS VERSION,
        NVL(a.APRTUSRG, '(null)')                    AS USUARIO,
        COUNT(*)                                      AS FILAS
FROM    CRD.APRT a
WHERE   a.APRTVLRR > 0
AND     a.TPAPCDGO IN (9, 11)
AND     (a.APRTIDAS IS NOT NULL
         OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
         OR a.APRTGLSA LIKE 'Abono al aporte%')
GROUP BY CASE
            WHEN a.APRTGLSA LIKE 'Abono al aporte%'                       THEN 'V2 EXCEDENTE (vieja)'
            WHEN a.APRTGLSA LIKE 'Aporte % - Mes %/% - CargaArchivo: %'  THEN 'V3 VIGENTE'
            WHEN a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'                THEN 'V1 (alguna variante)'
            ELSE                                                               'OTRA CON CARGA'
         END,
         NVL(a.APRTUSRG, '(null)')
ORDER BY 1, 3 DESC;


-- =============================================================================
-- 1. EJECUCIONES POR CARGA — cuándo corrió cada una y cuántas veces
-- =============================================================================
-- Reemplaza a A0 del 61. Misma lógica de detección de "inicio de ejecución" (huecos
-- > 30 min entre fechas de registro), pero el universo de filas ya no exige
-- APRTUSRG = 'SAA_AH': exige APRTIDAS IS NOT NULL (generador vigente) o el patrón de
-- glosa (para las cargas viejas, sin APRTIDAS, agrupadas por el id extraído de la glosa).
-- Las filas SIN fecha de registro (la variante hallada en producción) no tienen cómo
-- participar en el cálculo de huecos entre ejecuciones: se cuentan aparte, en
-- FILAS_SIN_FECHA_REGISTRO, para que no se pierdan silenciosamente del conteo.
-- =============================================================================
WITH BASE AS (
        SELECT  a.APRTCDGO, a.APRTFCRG, a.APRTVLRR, a.APRTVLPG,
                NVL(a.APRTIDAS, TO_NUMBER(REGEXP_SUBSTR(a.APRTGLSA, 'CargaArchivo: ([0-9]+)', 1, 1, NULL, 1))) AS ID_CARGA
        FROM    CRD.APRT a
        WHERE   a.APRTVLRR > 0
        AND     a.TPAPCDGO IN (9, 11)
        AND     (a.APRTIDAS IS NOT NULL
                 OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                 OR a.APRTGLSA LIKE 'Abono al aporte%')
),
CON_REGISTRO AS (
        SELECT  x.ID_CARGA,
                SUM(x.INICIO)   AS EJECUCIONES,
                MIN(x.APRTFCRG) AS PRIMERA_EJECUCION,
                MAX(x.APRTFCRG) AS ULTIMA_EJECUCION,
                COUNT(*)        AS FILAS_CON_REGISTRO,
                SUM(x.APRTVLRR) AS VALOR_CON_REGISTRO
        FROM  ( SELECT  b.ID_CARGA, b.APRTFCRG, b.APRTVLRR,
                        CASE WHEN LAG(b.APRTFCRG) OVER (PARTITION BY b.ID_CARGA
                                                        ORDER BY b.APRTFCRG, b.APRTCDGO) IS NULL
                             THEN 1
                             WHEN CAST(b.APRTFCRG AS DATE)
                                - CAST(LAG(b.APRTFCRG) OVER (PARTITION BY b.ID_CARGA
                                                             ORDER BY b.APRTFCRG, b.APRTCDGO) AS DATE)
                                > 30/1440
                             THEN 1 ELSE 0 END AS INICIO
                FROM    BASE b
                WHERE   b.APRTFCRG IS NOT NULL ) x
        GROUP BY x.ID_CARGA
),
SIN_REGISTRO AS (
        SELECT  b.ID_CARGA, COUNT(*) AS FILAS_SIN_FECHA_REGISTRO, SUM(b.APRTVLRR) AS VALOR_SIN_FECHA_REGISTRO
        FROM    BASE b
        WHERE   b.APRTFCRG IS NULL
        GROUP BY b.ID_CARGA
),
PGAP_CARGA AS (
        SELECT  TO_NUMBER(REGEXP_SUBSTR(p.PGAPCNCP, 'CargaArchivo: ([0-9]+)\s*$', 1, 1, NULL, 1)) AS ID_CARGA,
                COUNT(*)        AS PAGOS,
                SUM(p.PGAPVLRR) AS APLICADO
        FROM    CRD.PGAP p
        WHERE   p.PGAPUSRG = 'SISTEMA'
        AND     p.PGAPCNCP LIKE 'Pago aporte mes %CargaArchivo: %'
        GROUP BY TO_NUMBER(REGEXP_SUBSTR(p.PGAPCNCP, 'CargaArchivo: ([0-9]+)\s*$', 1, 1, NULL, 1))
),
ARCHIVO AS (
        SELECT  d.CRARCDGO AS ID_CARGA,
                COUNT(*)                     AS LINEAS_AH,
                SUM(CASE WHEN NVL(x.PXCADSDO,0) > 0.01 THEN 1 ELSE 0 END) AS LINEAS_CON_DESCUENTO,
                SUM(NVL(x.PXCADSDO, 0))      AS DESCONTADO_AH
        FROM    CRD.DTCA d
        JOIN    CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
        WHERE   d.DTCACDPP = 'AH'
        GROUP BY d.CRARCDGO
)
SELECT  c.CRARCDGO                                   AS ID_CARGA,
        c.CRARANAF || '-' || LPAD(c.CRARMSAF, 2, '0') AS PERIODO,
        c.FLLLCDGO                                   AS FILIAL,
        c.CRARESTD                                   AS ESTADO_CARGA,
        r.EJECUCIONES,
        r.PRIMERA_EJECUCION,
        r.ULTIMA_EJECUCION,
        ROUND((CAST(r.ULTIMA_EJECUCION AS DATE) - CAST(r.PRIMERA_EJECUCION AS DATE)) * 24, 1) AS HORAS_ENTRE_EJECUCIONES,
        NVL(r.FILAS_CON_REGISTRO, 0)                 AS FILAS_CON_REGISTRO,
        NVL(sr.FILAS_SIN_FECHA_REGISTRO, 0)          AS FILAS_SIN_FECHA_REGISTRO,
        NVL(r.VALOR_CON_REGISTRO, 0) + NVL(sr.VALOR_SIN_FECHA_REGISTRO, 0) AS VALOR_APRT_TOTAL,
        ar.LINEAS_AH,
        ar.LINEAS_CON_DESCUENTO,
        ar.DESCONTADO_AH                             AS DESCONTADO_ARCHIVO,
        pg.PAGOS                                     AS PAGOS_PGAP,
        pg.APLICADO                                  AS APLICADO_PGAP,
        CASE WHEN NVL(ar.DESCONTADO_AH, 0) > 0
             THEN ROUND(NVL(pg.APLICADO, 0) / ar.DESCONTADO_AH, 3) END AS RATIO_PGAP_ARCHIVO,
        ROUND(NVL(r.VALOR_CON_REGISTRO, 0) + NVL(sr.VALOR_SIN_FECHA_REGISTRO, 0)
              - NVL(ar.DESCONTADO_AH, 0), 2)          AS EXCESO_VALOR_VS_ARCHIVO
FROM    CRD.CRAR c
LEFT    JOIN CON_REGISTRO r  ON r.ID_CARGA = c.CRARCDGO
LEFT    JOIN SIN_REGISTRO sr ON sr.ID_CARGA = c.CRARCDGO
LEFT    JOIN PGAP_CARGA   pg ON pg.ID_CARGA = c.CRARCDGO
LEFT    JOIN ARCHIVO      ar ON ar.ID_CARGA = c.CRARCDGO
WHERE   r.ID_CARGA IS NOT NULL OR sr.ID_CARGA IS NOT NULL
ORDER BY c.CRARANAF, c.CRARMSAF, c.FLLLCDGO, c.CRARCDGO;


-- =============================================================================
-- 2. RATIO ARCHIVO vs APLICADO vs FILAS, POR CARGA — SOLO PARTÍCIPES CON APORTE
-- =============================================================================
-- Reemplaza a A4 del 61. Igual que allá, pero FILAS ya no exige APRTUSRG = 'SAA_AH':
-- exige únicamente APRTIDAS IS NOT NULL. Las filas viejas sin APRTIDAS (V1) no tienen
-- ROL confiable en esta vista sin repetir el join por ENTDRLPC vía la glosa; quedan
-- cubiertas igual en la sección 3 (exceso por partícipe), que sí las suma todas.
-- =============================================================================
WITH LINEAS AS (
        SELECT  d.CRARCDGO AS ID_CARGA, x.PXCACDPT AS ROL, NVL(x.PXCADSDO, 0) AS DESCONTADO
        FROM    CRD.DTCA d
        JOIN    CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
        WHERE   d.DTCACDPP = 'AH'
        AND     NVL(x.PXCADSDO, 0) > 0.01
),
FILAS AS (
        SELECT  a.APRTIDAS AS ID_CARGA, e.ENTDRLPC AS ROL,
                COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS VALOR, SUM(NVL(a.APRTVLPG,0)) AS PAGADO
        FROM    CRD.APRT a
        JOIN    CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
        WHERE   a.APRTIDAS IS NOT NULL
        AND     a.APRTVLRR > 0
        AND     a.TPAPCDGO IN (9, 11)
        GROUP BY a.APRTIDAS, e.ENTDRLPC
),
PAGOS AS (
        SELECT  TO_NUMBER(REGEXP_SUBSTR(p.PGAPCNCP, 'CargaArchivo: ([0-9]+)\s*$', 1, 1, NULL, 1)) AS ID_CARGA,
                e.ENTDRLPC AS ROL,
                SUM(p.PGAPVLRR) AS APLICADO
        FROM    CRD.PGAP p
        JOIN    CRD.APRT a ON a.APRTCDGO = p.APRTCDGO
        JOIN    CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
        WHERE   p.PGAPUSRG = 'SISTEMA'
        AND     p.PGAPCNCP LIKE 'Pago aporte mes %CargaArchivo: %'
        GROUP BY TO_NUMBER(REGEXP_SUBSTR(p.PGAPCNCP, 'CargaArchivo: ([0-9]+)\s*$', 1, 1, NULL, 1)), e.ENTDRLPC
)
SELECT  c.CRARCDGO AS ID_CARGA,
        c.CRARANAF || '-' || LPAD(c.CRARMSAF, 2, '0') AS PERIODO,
        COUNT(l.ROL)                                        AS LINEAS_CON_DESCUENTO,
        SUM(CASE WHEN f.ROL IS NOT NULL THEN 1 ELSE 0 END)  AS LINEAS_CON_FILAS,
        SUM(CASE WHEN f.ROL IS NULL THEN l.DESCONTADO ELSE 0 END) AS DESCONTADO_SIN_FILAS,
        SUM(CASE WHEN f.ROL IS NOT NULL THEN l.DESCONTADO ELSE 0 END) AS DESCONTADO_CON_FILAS,
        SUM(NVL(pg.APLICADO, 0))                            AS APLICADO_PGAP,
        SUM(NVL(f.VALOR, 0))                                AS VALOR_APRT,
        SUM(NVL(f.PAGADO, 0))                               AS PAGADO_APRT,
        CASE WHEN SUM(CASE WHEN f.ROL IS NOT NULL THEN l.DESCONTADO ELSE 0 END) > 0
             THEN ROUND(SUM(NVL(pg.APLICADO,0)) /
                        SUM(CASE WHEN f.ROL IS NOT NULL THEN l.DESCONTADO ELSE 0 END), 3) END AS RATIO_PGAP_ARCHIVO,
        ROUND(SUM(NVL(f.VALOR,0)) - SUM(CASE WHEN f.ROL IS NOT NULL THEN l.DESCONTADO ELSE 0 END), 2) AS EXCESO_VALOR,
        SUM(CASE WHEN ABS(NVL(pg.APLICADO,0) - l.DESCONTADO) > 0.02 AND f.ROL IS NOT NULL THEN 1 ELSE 0 END) AS PARTICIPES_DESCUADRADOS
FROM    CRD.CRAR c
JOIN    LINEAS l  ON l.ID_CARGA = c.CRARCDGO
LEFT    JOIN FILAS f  ON f.ID_CARGA = l.ID_CARGA AND f.ROL = l.ROL
LEFT    JOIN PAGOS pg ON pg.ID_CARGA = l.ID_CARGA AND pg.ROL = l.ROL
GROUP BY c.CRARCDGO, c.CRARANAF, c.CRARMSAF
ORDER BY c.CRARANAF, c.CRARMSAF, c.CRARCDGO;


-- =============================================================================
-- 3. EXCESO TOTAL ACUMULADO POR PARTÍCIPE Y TIPO — LA CIFRA A CORREGIR
-- =============================================================================
-- Reemplaza a A6 del 61. Esta es la consulta que decide el pedido 7. La diferencia con
-- el 61 es exclusivamente el universo de FILAS: ya no exige
-- `APRTUSRG = 'SAA_AH' OR glosa`, exige la identificación por forma (APRTIDAS o
-- cualquiera de los tres patrones de glosa conocidos), que ya cubre la variante sin
-- fecha de registro sin necesitar una rama aparte.
--
--   DESCONTADO_TOTAL  dinero real (PXCA, todas las cargas)         <- por partícipe,
--                                                                     no por tipo
--   APLICADO_PGAP     lo que el sistema aplicó (PGAP SISTEMA)      <- por tipo
--   VALOR_CARGA       SUM(valor) de las filas de carga             <- por tipo
--   EXCESO_DINERO     APLICADO - DESCONTADO (solo en la fila TOTAL): plata contada
--                     dos veces. Si > 0 hubo M1 o M2.
--   SALDO_FIFO        VALOR - APLICADO: filas con valor > pagado.
--   EXCESO_TOTAL      VALOR - DESCONTADO = EXCESO_DINERO + SALDO_FIFO.
--
-- Solo partícipes con |EXCESO_TOTAL| > 0.02 en la fila TOTAL.
-- =============================================================================
WITH DESC_TOTAL AS (
        SELECT  e.ENTDCDGO, SUM(NVL(x.PXCADSDO, 0)) AS DESCONTADO
        FROM    CRD.DTCA d
        JOIN    CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
        JOIN    CRD.CRAR c ON c.CRARCDGO = d.CRARCDGO
        JOIN    CRD.ENTD e ON e.ENTDRLPC = x.PXCACDPT
        WHERE   d.DTCACDPP = 'AH'
        AND     c.CRARESTD = 3
        GROUP BY e.ENTDCDGO
),
FILAS AS (
        SELECT  a.ENTDCDGO, a.TPAPCDGO,
                COUNT(*) AS FILAS,
                SUM(a.APRTVLRR) AS VALOR,
                SUM(NVL(a.APRTVLPG, 0)) AS PAGADO,
                SUM(NVL(a.APRTSLDO, 0)) AS SALDO_FIFO,
                SUM(CASE WHEN a.APRTIDST = 6 THEN 1 ELSE 0 END) AS FILAS_PARCIAL,
                SUM(CASE WHEN a.APRTIDAS IS NULL THEN 1 ELSE 0 END) AS FILAS_SIN_APRTIDAS,
                SUM(CASE WHEN a.APRTFCRG IS NULL THEN 1 ELSE 0 END) AS FILAS_SIN_FECHA_REGISTRO
        FROM    CRD.APRT a
        WHERE   a.APRTVLRR > 0
        AND     (a.APRTIDAS IS NOT NULL
                 OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                 OR a.APRTGLSA LIKE 'Abono al aporte%')
        GROUP BY a.ENTDCDGO, a.TPAPCDGO
),
PAGOS AS (
        SELECT  a.ENTDCDGO, a.TPAPCDGO, SUM(p.PGAPVLRR) AS APLICADO, COUNT(*) AS PAGOS
        FROM    CRD.PGAP p
        JOIN    CRD.APRT a ON a.APRTCDGO = p.APRTCDGO
        WHERE   p.PGAPUSRG = 'SISTEMA'
        AND     p.PGAPCNCP LIKE 'Pago aporte mes %CargaArchivo: %'
        GROUP BY a.ENTDCDGO, a.TPAPCDGO
),
POR_TIPO AS (
        SELECT  f.ENTDCDGO, f.TPAPCDGO,
                f.FILAS, f.FILAS_PARCIAL, f.FILAS_SIN_APRTIDAS, f.FILAS_SIN_FECHA_REGISTRO,
                f.VALOR, f.PAGADO, f.SALDO_FIFO,
                NVL(pg.APLICADO, 0) AS APLICADO, NVL(pg.PAGOS, 0) AS PAGOS
        FROM    FILAS f
        LEFT    JOIN PAGOS pg ON pg.ENTDCDGO = f.ENTDCDGO AND pg.TPAPCDGO = f.TPAPCDGO
),
TOTAL AS (
        SELECT  t.ENTDCDGO,
                SUM(t.FILAS) AS FILAS, SUM(t.FILAS_PARCIAL) AS FILAS_PARCIAL,
                SUM(t.FILAS_SIN_APRTIDAS) AS FILAS_SIN_APRTIDAS,
                SUM(t.FILAS_SIN_FECHA_REGISTRO) AS FILAS_SIN_FECHA_REGISTRO,
                SUM(t.VALOR) AS VALOR, SUM(t.PAGADO) AS PAGADO, SUM(t.SALDO_FIFO) AS SALDO_FIFO,
                SUM(t.APLICADO) AS APLICADO, SUM(t.PAGOS) AS PAGOS
        FROM    POR_TIPO t
        GROUP BY t.ENTDCDGO
),
CON_EXCESO AS (
        SELECT  t.ENTDCDGO
        FROM    TOTAL t
        LEFT    JOIN DESC_TOTAL dt ON dt.ENTDCDGO = t.ENTDCDGO
        WHERE   ABS(t.VALOR - NVL(dt.DESCONTADO, 0)) > 0.02
)
SELECT  e.ENTDNMID                          AS NUMERO_IDENTIFICACION,
        e.ENTDRZNS                          AS RAZON_SOCIAL,
        e.ENTDRLPC                          AS ROL,
        NVL(esp.ESPRNMBR, TO_CHAR(e.ENTDIDST)) AS ESTADO_PARTICIPE,
        'TOTAL'                             AS TIPO,
        t.FILAS, t.FILAS_PARCIAL, t.FILAS_SIN_APRTIDAS, t.FILAS_SIN_FECHA_REGISTRO,
        NVL(dt.DESCONTADO, 0)               AS DESCONTADO_TOTAL,
        t.APLICADO                          AS APLICADO_PGAP,
        t.VALOR                             AS VALOR_CARGA,
        t.PAGADO                            AS PAGADO_CARGA,
        ROUND(t.APLICADO - NVL(dt.DESCONTADO, 0), 2) AS EXCESO_DINERO,
        ROUND(t.VALOR - t.APLICADO, 2)      AS SALDO_FIFO,
        ROUND(t.VALOR - NVL(dt.DESCONTADO, 0), 2)    AS EXCESO_TOTAL,
        e.ENTDCDGO AS ID_ENTIDAD, NULL AS ID_TIPO,
        0 AS ORDEN
FROM    TOTAL t
JOIN    CON_EXCESO ce ON ce.ENTDCDGO = t.ENTDCDGO
JOIN    CRD.ENTD e ON e.ENTDCDGO = t.ENTDCDGO
LEFT    JOIN DESC_TOTAL dt ON dt.ENTDCDGO = t.ENTDCDGO
LEFT    JOIN CRD.ESPR esp ON esp.ESPRCDEX = e.ENTDIDST
UNION ALL
SELECT  e.ENTDNMID, e.ENTDRZNS, e.ENTDRLPC,
        NVL(esp.ESPRNMBR, TO_CHAR(e.ENTDIDST)),
        tp.TPAPNMBR,
        pt.FILAS, pt.FILAS_PARCIAL, pt.FILAS_SIN_APRTIDAS, pt.FILAS_SIN_FECHA_REGISTRO,
        NULL,
        pt.APLICADO, pt.VALOR, pt.PAGADO,
        NULL,
        ROUND(pt.VALOR - pt.APLICADO, 2),
        NULL,
        e.ENTDCDGO, pt.TPAPCDGO,
        1
FROM    POR_TIPO pt
JOIN    CON_EXCESO ce ON ce.ENTDCDGO = pt.ENTDCDGO
JOIN    CRD.ENTD e ON e.ENTDCDGO = pt.ENTDCDGO
JOIN    CRD.TPAP tp ON tp.TPAPCDGO = pt.TPAPCDGO
LEFT    JOIN CRD.ESPR esp ON esp.ESPRCDEX = e.ENTDIDST
ORDER BY 1, 19, 5;

-- =============================================================================
-- 61 - ANALISIS DE APORTES DUPLICADOS GENERADOS POR LA CARGA PETRO
-- =============================================================================
-- Acompaña a docs/logica-negocio/crd/ANALISIS-APORTES-DUPLICADOS-PETRO.md.
-- Leer el documento antes de interpretar cualquier resultado: ahí están la
-- línea de tiempo de las versiones del generador de aportes, los mecanismos
-- de duplicación y las reglas para decidir qué fila sobra.
--
-- SOLO LECTURA. Ningún DML. No crea tablas.
--
-- CADENA DE VERDAD que usan estas consultas (por partícipe y carga):
--   CXPG  lo que se PIDIÓ cobrar   (generación del archivo, tipos 9/11)
--   PXCA  lo que la empresa DESCONTÓ (archivo de respuesta, producto AH)  <- dinero
--   PGAP  lo que el sistema APLICÓ   (un registro por cada aplicación)
--   APRT  las filas que quedaron     (SUM(valor) = saldo en el modelo nuevo)
--
-- CÓMO SE ENLAZA CADA TABLA CON LA CARGA
--   APRT.APRTIDAS = CRAR.CRARCDGO            (solo filas creadas desde 2026-04-09)
--   APRT.APRTGLSA '...CargaArchivo: N'       (filas de la versión 2026-04-02..09,
--                                             que no llevan APRTIDAS)
--   PGAP.PGAPCNCP '...CargaArchivo: N'       (PGAP no tiene FK a CRAR: se extrae
--                                             el id del concepto con REGEXP)
--   PXCA.PXCACDPT = ENTD.ENTDRLPC            (rol Petrocomercial)
--   DTCA.DTCACDPP = 'AH'                     (producto aportes)
--
-- USUARIOS
--   APRT.APRTUSRG = 'SAA_AH'   fila creada por la carga (desde 2026-04-09)
--   APRT.APRTUSRG IS NULL      fila creada por la carga (2026-04-02..09) o dato migrado
--   PGAP.PGAPUSRG = 'SISTEMA'  pago registrado por la carga
--
-- ÍNDICE
--   A0  Línea de tiempo de las cargas: cuándo se ejecutó cada una y cuántas veces
--   A1  Periodos con más de una carga
--   A2  Filas de APRT por versión del generador
--   A3  Filas huérfanas (carga borrada) y filas de carga sin PGAP
--   A4  Por carga: dinero del archivo vs dinero aplicado vs filas creadas
--   A5  Por partícipe y carga: la conciliación (dónde está el exceso)
--   A6  Por partícipe y tipo: exceso total acumulado (la cifra a corregir)
--   A7  Fila a fila, con número de ejecución: la lista de candidatas
--   A8  Impacto: saldos que quedarían negativos si se retira el exceso
-- =============================================================================


-- =============================================================================
-- A0 - LÍNEA DE TIEMPO DE LAS CARGAS
-- =============================================================================
-- Una fila por carga. Lo que hay que mirar:
--   EJECUCIONES        > 1  -> la fase 3 corrió más de una vez sobre esa carga.
--                              Es el mecanismo M1 del documento. Se detecta por
--                              huecos > 30 min entre fechas de registro de APRT.
--   RATIO_PGAP_ARCHIVO ~ 2  -> el dinero del archivo se aplicó dos veces.
--   PRIMERA_EJECUCION  < 2026-04-09  -> esa carga corrió con el generador viejo
--                              (filas sin APRTIDAS, sin SAA_AH; ver A2).
-- El orden es por periodo, así se ve la secuencia real de procesamiento.
-- =============================================================================
WITH RUNS AS (
        SELECT  x.APRTIDAS,
                SUM(x.INICIO) AS EJECUCIONES,
                MIN(x.APRTFCRG) AS PRIMERA_EJECUCION,
                MAX(x.APRTFCRG) AS ULTIMA_EJECUCION,
                COUNT(*)        AS FILAS_APRT,
                SUM(x.APRTVLRR) AS VALOR_APRT,
                SUM(NVL(x.APRTVLPG, 0)) AS PAGADO_APRT
        FROM  ( SELECT  a.APRTIDAS, a.APRTFCRG, a.APRTVLRR, a.APRTVLPG,
                        CASE WHEN LAG(a.APRTFCRG) OVER (PARTITION BY a.APRTIDAS
                                                        ORDER BY a.APRTFCRG, a.APRTCDGO) IS NULL
                             THEN 1
                             WHEN CAST(a.APRTFCRG AS DATE)
                                - CAST(LAG(a.APRTFCRG) OVER (PARTITION BY a.APRTIDAS
                                                             ORDER BY a.APRTFCRG, a.APRTCDGO) AS DATE)
                                > 30/1440
                             THEN 1 ELSE 0 END AS INICIO
                FROM    CRD.APRT a
                WHERE   a.APRTIDAS IS NOT NULL
                AND     a.APRTUSRG = 'SAA_AH' ) x
        GROUP BY x.APRTIDAS
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
        c.CRARFCCR                                   AS FECHA_CARGA,
        r.EJECUCIONES,
        r.PRIMERA_EJECUCION,
        r.ULTIMA_EJECUCION,
        ROUND((CAST(r.ULTIMA_EJECUCION AS DATE) - CAST(r.PRIMERA_EJECUCION AS DATE)) * 24, 1) AS HORAS_ENTRE_EJECUCIONES,
        ar.LINEAS_AH,
        ar.LINEAS_CON_DESCUENTO,
        ar.DESCONTADO_AH                             AS DESCONTADO_ARCHIVO,
        pg.PAGOS                                     AS PAGOS_PGAP,
        pg.APLICADO                                  AS APLICADO_PGAP,
        CASE WHEN NVL(ar.DESCONTADO_AH, 0) > 0
             THEN ROUND(NVL(pg.APLICADO, 0) / ar.DESCONTADO_AH, 3) END AS RATIO_PGAP_ARCHIVO,
        r.FILAS_APRT,
        r.VALOR_APRT,
        r.PAGADO_APRT,
        ROUND(NVL(r.VALOR_APRT, 0) - NVL(ar.DESCONTADO_AH, 0), 2) AS EXCESO_VALOR_VS_ARCHIVO
FROM    CRD.CRAR c
LEFT    JOIN RUNS       r  ON r.APRTIDAS = c.CRARCDGO
LEFT    JOIN PGAP_CARGA pg ON pg.ID_CARGA = c.CRARCDGO
LEFT    JOIN ARCHIVO    ar ON ar.ID_CARGA = c.CRARCDGO
ORDER BY c.CRARANAF, c.CRARMSAF, c.FLLLCDGO, c.CRARCDGO;


-- =============================================================================
-- A1 - PERIODOS CON MÁS DE UNA CARGA (mecanismo M2)
-- =============================================================================
-- Esperado: 0 filas. Si un periodo tiene dos cargas y las dos tienen filas en
-- APRT, los aportes de ese mes están dos veces con APRTIDAS distinto.
-- =============================================================================
SELECT  c.CRARANAF || '-' || LPAD(c.CRARMSAF, 2, '0') AS PERIODO,
        c.FLLLCDGO                                   AS FILIAL,
        COUNT(*)                                     AS CARGAS,
        LISTAGG(c.CRARCDGO || ' (est ' || c.CRARESTD || ', ' ||
                (SELECT COUNT(*) FROM CRD.APRT a WHERE a.APRTIDAS = c.CRARCDGO) || ' aprt)', ' | ')
            WITHIN GROUP (ORDER BY c.CRARCDGO)       AS DETALLE
FROM    CRD.CRAR c
GROUP BY c.CRARANAF, c.CRARMSAF, c.FLLLCDGO
HAVING  COUNT(*) > 1
ORDER BY 1, 2;


-- =============================================================================
-- A2 - FILAS DE APRT POR VERSIÓN DEL GENERADOR
-- =============================================================================
-- Clasifica TODAS las filas positivas de APRT según quién las escribió. Las
-- versiones son las del documento (§2):
--   V1  2026-04-02..09  glosa 'Aporte jubilación - CargaArchivo: N', sin
--                       APRTIDAS, sin usuario, valor = HistorialSueldo (NO el
--                       dinero descontado), fecha de transacción = fecha de
--                       proceso.
--   V2  2026-04-09..11  glosa 'Abono al aporte ...' (excedente al mes
--                       siguiente), valor = esperado, pagado = excedente.
--   V3  2026-04-11..hoy glosa 'Aporte X - Mes m/aaaa - CargaArchivo: N'.
--   MANUAL / OTRA       todo lo demás.
-- Si V1 tiene filas y las mismas cargas también tienen filas V3 -> la carga
-- se procesó con las dos versiones (mecanismo M3): las V1 sobran.
-- =============================================================================
SELECT  CASE
            WHEN a.APRTGLSA LIKE 'Abono al aporte%'                       THEN 'V2 EXCEDENTE'
            WHEN a.APRTGLSA LIKE 'Aporte % - Mes %/% - CargaArchivo: %'  THEN 'V3 VIGENTE'
            WHEN a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'                THEN 'V1 ABRIL 2-9'
            WHEN a.APRTIDAS IS NOT NULL                                   THEN 'OTRA CON CARGA'
            WHEN a.APRTUSRG = 'SAA_AH'                                    THEN 'OTRA SAA_AH'
            ELSE                                                               'MANUAL / MIGRADA'
        END                                          AS VERSION,
        NVL(a.APRTUSRG, '(null)')                    AS USUARIO,
        CASE WHEN a.APRTIDAS IS NULL THEN 'SIN' ELSE 'CON' END AS ID_ASOPREP,
        COUNT(*)                                     AS FILAS,
        COUNT(DISTINCT a.ENTDCDGO)                   AS PARTICIPES,
        COUNT(DISTINCT NVL(a.APRTIDAS,
              TO_NUMBER(REGEXP_SUBSTR(a.APRTGLSA, 'CargaArchivo: ([0-9]+)', 1, 1, NULL, 1)))) AS CARGAS,
        MIN(a.APRTFCTR)                              AS MIN_FECHA_TRANSACCION,
        MAX(a.APRTFCTR)                              AS MAX_FECHA_TRANSACCION,
        MIN(a.APRTFCRG)                              AS MIN_FECHA_REGISTRO,
        MAX(a.APRTFCRG)                              AS MAX_FECHA_REGISTRO,
        SUM(a.APRTVLRR)                              AS VALOR,
        SUM(NVL(a.APRTVLPG, 0))                      AS VALOR_PAGADO,
        SUM(NVL(a.APRTSLDO, 0))                      AS SALDO_FIFO,
        SUM(CASE WHEN a.APRTIDST = 6 THEN 1 ELSE 0 END) AS FILAS_PARCIAL
FROM    CRD.APRT a
WHERE   a.APRTVLRR > 0
AND     a.TPAPCDGO IN (9, 11)
GROUP BY CASE
            WHEN a.APRTGLSA LIKE 'Abono al aporte%'                       THEN 'V2 EXCEDENTE'
            WHEN a.APRTGLSA LIKE 'Aporte % - Mes %/% - CargaArchivo: %'  THEN 'V3 VIGENTE'
            WHEN a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'                THEN 'V1 ABRIL 2-9'
            WHEN a.APRTIDAS IS NOT NULL                                   THEN 'OTRA CON CARGA'
            WHEN a.APRTUSRG = 'SAA_AH'                                    THEN 'OTRA SAA_AH'
            ELSE                                                               'MANUAL / MIGRADA'
         END,
         NVL(a.APRTUSRG, '(null)'),
         CASE WHEN a.APRTIDAS IS NULL THEN 'SIN' ELSE 'CON' END
ORDER BY 1, 2;


-- -----------------------------------------------------------------------------
-- A2b - Cargas que tienen filas V1 Y filas V3 a la vez (mecanismo M3)
-- -----------------------------------------------------------------------------
-- Esperado: 0 filas. Cada carga que salga fue procesada con el generador viejo
-- y luego con el nuevo: las filas V1 de esa carga son las que sobran.
-- -----------------------------------------------------------------------------
WITH V1 AS (
        SELECT  TO_NUMBER(REGEXP_SUBSTR(a.APRTGLSA, 'CargaArchivo: ([0-9]+)', 1, 1, NULL, 1)) AS ID_CARGA,
                COUNT(*) AS FILAS_V1, SUM(a.APRTVLRR) AS VALOR_V1,
                COUNT(DISTINCT a.ENTDCDGO) AS PARTICIPES_V1
        FROM    CRD.APRT a
        WHERE   a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
        AND     a.APRTGLSA NOT LIKE '% - Mes %'
        AND     a.APRTIDAS IS NULL
        GROUP BY TO_NUMBER(REGEXP_SUBSTR(a.APRTGLSA, 'CargaArchivo: ([0-9]+)', 1, 1, NULL, 1))
),
V3 AS (
        SELECT  a.APRTIDAS AS ID_CARGA,
                COUNT(*) AS FILAS_V3, SUM(a.APRTVLRR) AS VALOR_V3,
                COUNT(DISTINCT a.ENTDCDGO) AS PARTICIPES_V3
        FROM    CRD.APRT a
        WHERE   a.APRTIDAS IS NOT NULL
        GROUP BY a.APRTIDAS
)
SELECT  v1.ID_CARGA,
        c.CRARANAF || '-' || LPAD(c.CRARMSAF, 2, '0') AS PERIODO,
        v1.FILAS_V1, v1.PARTICIPES_V1, v1.VALOR_V1,
        v3.FILAS_V3, v3.PARTICIPES_V3, v3.VALOR_V3
FROM    V1 v1
JOIN    V3 v3 ON v3.ID_CARGA = v1.ID_CARGA
LEFT    JOIN CRD.CRAR c ON c.CRARCDGO = v1.ID_CARGA
ORDER BY c.CRARANAF, c.CRARMSAF;


-- =============================================================================
-- A3 - HUÉRFANAS Y SIN PAGO
-- =============================================================================

-- -----------------------------------------------------------------------------
-- A3a - Filas cuya carga ya no existe (la carga se borró y se volvió a cargar)
-- -----------------------------------------------------------------------------
-- Esperado: 0 filas. APRTIDAS no tiene FK: borrar la CRAR deja las filas.
-- -----------------------------------------------------------------------------
SELECT  NVL(a.APRTIDAS, TO_NUMBER(REGEXP_SUBSTR(a.APRTGLSA, 'CargaArchivo: ([0-9]+)', 1, 1, NULL, 1))) AS ID_CARGA_REFERIDA,
        COUNT(*)                   AS FILAS,
        COUNT(DISTINCT a.ENTDCDGO) AS PARTICIPES,
        SUM(a.APRTVLRR)            AS VALOR,
        MIN(a.APRTFCTR)            AS MIN_FECHA_TRANSACCION,
        MIN(a.APRTFCRG)            AS MIN_FECHA_REGISTRO
FROM    CRD.APRT a
WHERE   (a.APRTIDAS IS NOT NULL OR a.APRTGLSA LIKE '%CargaArchivo: %')
AND     NOT EXISTS (SELECT 1 FROM CRD.CRAR c
                    WHERE c.CRARCDGO = NVL(a.APRTIDAS,
                          TO_NUMBER(REGEXP_SUBSTR(a.APRTGLSA, 'CargaArchivo: ([0-9]+)', 1, 1, NULL, 1))))
GROUP BY NVL(a.APRTIDAS, TO_NUMBER(REGEXP_SUBSTR(a.APRTGLSA, 'CargaArchivo: ([0-9]+)', 1, 1, NULL, 1)))
ORDER BY 1;


-- -----------------------------------------------------------------------------
-- A3b - Filas de carga (V3) sin ningún PGAP: valor positivo que nadie pagó
-- -----------------------------------------------------------------------------
-- En el generador vigente cada fila nace y se paga en la misma transacción,
-- así que una fila SAA_AH sin PGAP no debería existir. Si existe, es un
-- fantasma: suma al saldo sin dinero detrás. Candidata directa a retirar.
-- -----------------------------------------------------------------------------
SELECT  a.APRTCDGO AS ID_APORTE, a.APRTIDAS AS ID_CARGA,
        e.ENTDNMID AS NUMERO_IDENTIFICACION, e.ENTDRZNS AS RAZON_SOCIAL,
        a.TPAPCDGO AS ID_TIPO, a.APRTVLRR AS VALOR, a.APRTVLPG AS VALOR_PAGADO,
        a.APRTSLDO AS SALDO, a.APRTIDST AS ESTADO, a.APRTFCTR AS FECHA_TRANSACCION,
        a.APRTFCRG AS FECHA_REGISTRO, a.APRTGLSA AS GLOSA
FROM    CRD.APRT a
JOIN    CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
WHERE   a.APRTUSRG = 'SAA_AH'
AND     a.APRTVLRR > 0
AND     NOT EXISTS (SELECT 1 FROM CRD.PGAP p WHERE p.APRTCDGO = a.APRTCDGO)
ORDER BY a.APRTIDAS, e.ENTDNMID, a.APRTCDGO;


-- =============================================================================
-- A4 - POR CARGA: ARCHIVO vs APLICADO vs FILAS, SOLO PARTÍCIPES CON APORTE
-- =============================================================================
-- Igual que A0 pero restringido a las líneas AH que sí generaron filas (las
-- que no tienen HistorialSueldo activo descuentan en el archivo y no crean
-- nada; A0 las incluye en DESCONTADO_ARCHIVO y eso ensucia el ratio).
-- Aquí el ratio debe ser exactamente 1.000 en una carga sana.
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
        WHERE   a.APRTIDAS IS NOT NULL AND a.APRTUSRG = 'SAA_AH'
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
-- A5 - POR PARTÍCIPE Y CARGA: LA CONCILIACIÓN
-- =============================================================================
-- Una fila por (partícipe, carga) donde algo no cuadra. DIAGNOSTICO:
--   DINERO APLICADO > 1 VEZ  APLICADO_PGAP > DESCONTADO: la misma plata entró
--                            dos veces. Mecanismo M1 (fase 3 repetida). Las
--                            filas y PGAP de la segunda ejecución sobran.
--   SOBREVALORADO            APLICADO ~ DESCONTADO pero VALOR_APRT > DESCONTADO:
--                            hay filas con valor > pagado (PARCIAL del FIFO, o
--                            V2 excedente). No es dinero duplicado; es la
--                            fila "esperada" que el modelo nuevo cuenta entera.
--   SUBVALORADO              VALOR_APRT < DESCONTADO: parte del dinero se aplicó
--                            por FIFO a filas de OTRA carga (APRTIDAS distinto).
--                            Normal; se compensa en A6 al sumar por partícipe.
--   SIN FILAS                descontó pero no se creó nada (sin HistorialSueldo,
--                            o jubilación y cesantía en 0). Fuera de alcance.
--   SIN DESCUENTO CON FILAS  hay filas de la carga pero el archivo no descontó.
--                            Raro; revisar a mano.
-- Ojo con FIFO entre cargas: por eso esta vista es orientativa y la cifra
-- definitiva es la de A6.
-- =============================================================================
WITH LINEAS AS (
        SELECT  d.CRARCDGO AS ID_CARGA, x.PXCACDPT AS ROL, x.PXCANMBR AS NOMBRE_ARCHIVO,
                NVL(x.PXCADSDO, 0) AS DESCONTADO
        FROM    CRD.DTCA d
        JOIN    CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
        WHERE   d.DTCACDPP = 'AH'
),
FILAS AS (
        SELECT  a.APRTIDAS AS ID_CARGA, a.ENTDCDGO,
                COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS VALOR, SUM(NVL(a.APRTVLPG,0)) AS PAGADO,
                SUM(NVL(a.APRTSLDO,0)) AS SALDO_FIFO,
                MIN(a.APRTFCRG) AS PRIMER_REGISTRO, MAX(a.APRTFCRG) AS ULTIMO_REGISTRO
        FROM    CRD.APRT a
        WHERE   a.APRTIDAS IS NOT NULL AND a.APRTUSRG = 'SAA_AH'
        GROUP BY a.APRTIDAS, a.ENTDCDGO
),
PAGOS AS (
        SELECT  TO_NUMBER(REGEXP_SUBSTR(p.PGAPCNCP, 'CargaArchivo: ([0-9]+)\s*$', 1, 1, NULL, 1)) AS ID_CARGA,
                a.ENTDCDGO,
                COUNT(*) AS PAGOS, SUM(p.PGAPVLRR) AS APLICADO
        FROM    CRD.PGAP p
        JOIN    CRD.APRT a ON a.APRTCDGO = p.APRTCDGO
        WHERE   p.PGAPUSRG = 'SISTEMA'
        AND     p.PGAPCNCP LIKE 'Pago aporte mes %CargaArchivo: %'
        GROUP BY TO_NUMBER(REGEXP_SUBSTR(p.PGAPCNCP, 'CargaArchivo: ([0-9]+)\s*$', 1, 1, NULL, 1)), a.ENTDCDGO
),
BASE AS (
        SELECT  c.CRARCDGO AS ID_CARGA,
                c.CRARANAF || '-' || LPAD(c.CRARMSAF, 2, '0') AS PERIODO,
                e.ENTDCDGO, e.ENTDNMID, e.ENTDRZNS, e.ENTDRLPC,
                NVL(l.DESCONTADO, 0) AS DESCONTADO,
                NVL(pg.APLICADO, 0)  AS APLICADO,
                NVL(pg.PAGOS, 0)     AS PAGOS,
                NVL(f.FILAS, 0)      AS FILAS,
                NVL(f.VALOR, 0)      AS VALOR,
                NVL(f.PAGADO, 0)     AS PAGADO,
                NVL(f.SALDO_FIFO, 0) AS SALDO_FIFO,
                f.PRIMER_REGISTRO, f.ULTIMO_REGISTRO
        FROM    CRD.CRAR c
        JOIN    CRD.ENTD e ON 1 = 1
        LEFT    JOIN LINEAS l  ON l.ID_CARGA = c.CRARCDGO AND l.ROL = e.ENTDRLPC
        LEFT    JOIN FILAS  f  ON f.ID_CARGA = c.CRARCDGO AND f.ENTDCDGO = e.ENTDCDGO
        LEFT    JOIN PAGOS  pg ON pg.ID_CARGA = c.CRARCDGO AND pg.ENTDCDGO = e.ENTDCDGO
        WHERE   (l.ROL IS NOT NULL OR f.ENTDCDGO IS NOT NULL OR pg.ENTDCDGO IS NOT NULL)
)
SELECT  b.ID_CARGA, b.PERIODO, b.ENTDRLPC AS ROL,
        b.ENTDNMID AS NUMERO_IDENTIFICACION, b.ENTDRZNS AS RAZON_SOCIAL,
        b.DESCONTADO AS DESCONTADO_ARCHIVO,
        b.APLICADO   AS APLICADO_PGAP,
        b.PAGOS      AS PAGOS_PGAP,
        b.FILAS      AS FILAS_APRT,
        b.VALOR      AS VALOR_APRT,
        b.PAGADO     AS PAGADO_APRT,
        b.SALDO_FIFO,
        ROUND(b.APLICADO - b.DESCONTADO, 2) AS EXCESO_DINERO,
        ROUND(b.VALOR - b.DESCONTADO, 2)    AS EXCESO_VALOR,
        CASE
            WHEN b.DESCONTADO <= 0.01 AND b.FILAS > 0          THEN 'SIN DESCUENTO CON FILAS'
            WHEN b.DESCONTADO >  0.01 AND b.FILAS = 0
                                       AND b.APLICADO <= 0.01  THEN 'SIN FILAS'
            WHEN b.APLICADO > b.DESCONTADO + 0.02              THEN 'DINERO APLICADO > 1 VEZ'
            WHEN b.VALOR    > b.DESCONTADO + 0.02              THEN 'SOBREVALORADO'
            WHEN b.VALOR    < b.DESCONTADO - 0.02              THEN 'SUBVALORADO'
            ELSE                                                    'OK'
        END AS DIAGNOSTICO,
        b.PRIMER_REGISTRO, b.ULTIMO_REGISTRO,
        b.ENTDCDGO AS ID_ENTIDAD
FROM    BASE b
WHERE   NOT ( ABS(b.APLICADO - b.DESCONTADO) <= 0.02
          AND ABS(b.VALOR    - b.DESCONTADO) <= 0.02 )
ORDER BY CASE
            WHEN b.APLICADO > b.DESCONTADO + 0.02 THEN 1
            WHEN b.VALOR    > b.DESCONTADO + 0.02 THEN 2
            ELSE 3 END,
         b.PERIODO, b.ENTDNMID;


-- =============================================================================
-- A6 - POR PARTÍCIPE Y TIPO: EXCESO TOTAL ACUMULADO
-- =============================================================================
-- Esta es la cifra a corregir. Suma TODO lo que la carga escribió para el
-- partícipe (todas las versiones, todas las cargas) y lo compara con TODO lo
-- que el archivo descontó. El FIFO entre cargas ya no molesta porque se suma
-- por partícipe.
--
--   DESCONTADO_TOTAL  dinero real (PXCA, todas las cargas)         <- por partícipe,
--                                                                     no por tipo
--   APLICADO_PGAP     lo que el sistema aplicó (PGAP SISTEMA)      <- por tipo
--   VALOR_CARGA       SUM(valor) de las filas de carga             <- por tipo
--   EXCESO_DINERO     APLICADO - DESCONTADO (solo en la fila TOTAL): plata contada
--                     dos veces. Si > 0 hubo M1 o M2.
--   SALDO_FIFO        VALOR - APLICADO: filas con valor > pagado. Es lo que el
--                     modelo nuevo cuenta sin que haya entrado dinero.
--   EXCESO_TOTAL      VALOR - DESCONTADO = EXCESO_DINERO + SALDO_FIFO. Lo que
--                     sobra en el saldo del partícipe por culpa de la carga.
--
-- Trae una fila por tipo y una fila TOTAL por partícipe (TIPO = 'TOTAL'),
-- porque el descuento del archivo no viene separado por tipo.
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
                SUM(CASE WHEN a.APRTIDAS IS NULL THEN 1 ELSE 0 END) AS FILAS_V1
        FROM    CRD.APRT a
        WHERE   a.APRTVLRR > 0
        AND     (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %')
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
                f.FILAS, f.FILAS_PARCIAL, f.FILAS_V1,
                f.VALOR, f.PAGADO, f.SALDO_FIFO,
                NVL(pg.APLICADO, 0) AS APLICADO, NVL(pg.PAGOS, 0) AS PAGOS
        FROM    FILAS f
        LEFT    JOIN PAGOS pg ON pg.ENTDCDGO = f.ENTDCDGO AND pg.TPAPCDGO = f.TPAPCDGO
),
TOTAL AS (
        SELECT  t.ENTDCDGO,
                SUM(t.FILAS) AS FILAS, SUM(t.FILAS_PARCIAL) AS FILAS_PARCIAL, SUM(t.FILAS_V1) AS FILAS_V1,
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
        t.FILAS, t.FILAS_PARCIAL, t.FILAS_V1,
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
        pt.FILAS, pt.FILAS_PARCIAL, pt.FILAS_V1,
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
ORDER BY 1, 18, 5;


-- =============================================================================
-- A7 - FILA A FILA: LAS CANDIDATAS, CON NÚMERO DE EJECUCIÓN
-- =============================================================================
-- Todas las filas de carga de los partícipes que salen en A6, clasificadas.
-- NRO_EJECUCION se calcula por carga: una ejecución nueva empieza cuando entre
-- dos registros consecutivos de la misma carga hay más de 30 minutos.
--
-- CLASIFICACION (ver reglas R1-R6 del documento):
--   EJECUCION REPETIDA   NRO_EJECUCION > 1. La carga corrió otra vez. Sobra la
--                        fila Y sus PGAP. Además, los PGAP de esta ejecución que
--                        cayeron en filas de la ejecución 1 (por FIFO) también
--                        sobran: ver A7b.
--   V1 REEMPLAZADA       fila del generador viejo cuya carga también tiene filas
--                        V3. Sobra.
--   V1 UNICA             fila del generador viejo sin reemplazo. Conservar, pero
--                        su valor es el esperado de HSTR, no el dinero: verificar
--                        contra DESCONTADO en A5.
--   SIN PAGO             fila positiva sin PGAP. Sobra.
--   PARCIAL CON SALDO    valor > pagado, estado 6. No es duplicado: es la deuda
--                        del FIFO. Decisión R4 del documento.
--   PAGADA COMPLETA      valor = pagado. Se queda.
-- =============================================================================
WITH DESC_TOTAL AS (
        SELECT  e.ENTDCDGO, SUM(NVL(x.PXCADSDO, 0)) AS DESCONTADO
        FROM    CRD.DTCA d
        JOIN    CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
        JOIN    CRD.CRAR c ON c.CRARCDGO = d.CRARCDGO
        JOIN    CRD.ENTD e ON e.ENTDRLPC = x.PXCACDPT
        WHERE   d.DTCACDPP = 'AH' AND c.CRARESTD = 3
        GROUP BY e.ENTDCDGO
),
VALOR_CARGA AS (
        SELECT  a.ENTDCDGO, SUM(a.APRTVLRR) AS VALOR
        FROM    CRD.APRT a
        WHERE   a.APRTVLRR > 0
        AND     (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %')
        GROUP BY a.ENTDCDGO
),
CON_EXCESO AS (
        SELECT  v.ENTDCDGO
        FROM    VALOR_CARGA v
        LEFT    JOIN DESC_TOTAL dt ON dt.ENTDCDGO = v.ENTDCDGO
        WHERE   ABS(v.VALOR - NVL(dt.DESCONTADO, 0)) > 0.02
),
CARGAS_V3 AS (
        SELECT DISTINCT a.APRTIDAS AS ID_CARGA FROM CRD.APRT a WHERE a.APRTIDAS IS NOT NULL
),
EJEC AS (
        SELECT  x.*,
                SUM(x.INICIO) OVER (PARTITION BY x.ID_CARGA
                                    ORDER BY x.APRTFCRG, x.APRTCDGO
                                    ROWS UNBOUNDED PRECEDING) AS NRO_EJECUCION
        FROM  ( SELECT  a.APRTCDGO, a.ENTDCDGO, a.TPAPCDGO, a.APRTFCTR, a.APRTFCRG,
                        a.APRTVLRR, a.APRTVLPG, a.APRTSLDO, a.APRTIDST, a.APRTUSRG, a.APRTGLSA,
                        NVL(a.APRTIDAS, TO_NUMBER(REGEXP_SUBSTR(a.APRTGLSA, 'CargaArchivo: ([0-9]+)', 1, 1, NULL, 1))) AS ID_CARGA,
                        CASE WHEN a.APRTIDAS IS NULL THEN 'V1'
                             WHEN a.APRTGLSA LIKE 'Abono al aporte%' THEN 'V2'
                             ELSE 'V3' END AS VERSION,
                        CASE WHEN LAG(a.APRTFCRG) OVER (PARTITION BY a.APRTIDAS
                                                        ORDER BY a.APRTFCRG, a.APRTCDGO) IS NULL THEN 1
                             WHEN CAST(a.APRTFCRG AS DATE)
                                - CAST(LAG(a.APRTFCRG) OVER (PARTITION BY a.APRTIDAS
                                                             ORDER BY a.APRTFCRG, a.APRTCDGO) AS DATE)
                                > 30/1440 THEN 1 ELSE 0 END AS INICIO
                FROM    CRD.APRT a
                WHERE   a.APRTVLRR > 0
                AND     (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %') ) x
),
PGAP_FILA AS (
        SELECT  p.APRTCDGO,
                COUNT(*) AS PAGOS,
                SUM(p.PGAPVLRR) AS APLICADO,
                LISTAGG(TO_NUMBER(REGEXP_SUBSTR(p.PGAPCNCP, 'CargaArchivo: ([0-9]+)\s*$', 1, 1, NULL, 1))
                        || ':' || p.PGAPVLRR, ' | ')
                    WITHIN GROUP (ORDER BY p.PGAPCDGO) AS DETALLE_PGAP
        FROM    CRD.PGAP p
        WHERE   p.PGAPUSRG = 'SISTEMA'
        GROUP BY p.APRTCDGO
)
SELECT  e.ENTDNMID AS NUMERO_IDENTIFICACION,
        e.ENTDRZNS AS RAZON_SOCIAL,
        tp.TPAPNMBR AS TIPO_APORTE,
        x.ID_CARGA,
        c.CRARANAF || '-' || LPAD(c.CRARMSAF, 2, '0') AS PERIODO_CARGA,
        TO_CHAR(x.APRTFCTR, 'YYYY-MM-DD') AS FECHA_TRANSACCION,
        x.APRTFCRG AS FECHA_REGISTRO,
        x.VERSION,
        x.NRO_EJECUCION,
        x.APRTCDGO AS ID_APORTE,
        x.APRTVLRR AS VALOR,
        NVL(x.APRTVLPG, 0) AS VALOR_PAGADO,
        NVL(x.APRTSLDO, 0) AS SALDO_FIFO,
        x.APRTIDST AS ESTADO,
        NVL(pf.PAGOS, 0) AS PAGOS_PGAP,
        NVL(pf.APLICADO, 0) AS APLICADO_PGAP,
        pf.DETALLE_PGAP AS PGAP_CARGA_VALOR,
        CASE
            WHEN x.VERSION = 'V3' AND x.NRO_EJECUCION > 1            THEN 'EJECUCION REPETIDA'
            WHEN x.VERSION = 'V1' AND cv.ID_CARGA IS NOT NULL         THEN 'V1 REEMPLAZADA'
            WHEN x.VERSION = 'V1'                                     THEN 'V1 UNICA'
            WHEN NVL(pf.APLICADO, 0) <= 0.01                          THEN 'SIN PAGO'
            WHEN NVL(x.APRTSLDO, 0) > 0.01                            THEN 'PARCIAL CON SALDO'
            WHEN ABS(x.APRTVLRR - NVL(pf.APLICADO, 0)) <= 0.02        THEN 'PAGADA COMPLETA'
            ELSE                                                           'REVISAR'
        END AS CLASIFICACION,
        x.APRTGLSA AS GLOSA,
        x.ENTDCDGO AS ID_ENTIDAD
FROM    EJEC x
JOIN    CON_EXCESO ce ON ce.ENTDCDGO = x.ENTDCDGO
JOIN    CRD.ENTD e  ON e.ENTDCDGO = x.ENTDCDGO
JOIN    CRD.TPAP tp ON tp.TPAPCDGO = x.TPAPCDGO
LEFT    JOIN CRD.CRAR c ON c.CRARCDGO = x.ID_CARGA
LEFT    JOIN CARGAS_V3 cv ON cv.ID_CARGA = x.ID_CARGA AND x.VERSION = 'V1'
LEFT    JOIN PGAP_FILA pf ON pf.APRTCDGO = x.APRTCDGO
ORDER BY e.ENTDNMID, tp.TPAPNMBR, x.APRTFCTR, x.APRTFCRG, x.APRTCDGO;


-- -----------------------------------------------------------------------------
-- A7b - PGAP de una ejecución repetida que cayeron en filas de la ejecución 1
-- -----------------------------------------------------------------------------
-- Cuando la fase 3 corre por segunda vez, el FIFO aplica primero el dinero a
-- las filas PARCIAL que dejó la primera ejecución. Esos PGAP tienen concepto de
-- la misma carga y fecha de la segunda ejecución, pero están enlazados a filas
-- de la primera. Al retirar la segunda ejecución hay que retirar también estos
-- pagos y recalcular valorPagado/saldo/estado de la fila desde los PGAP que
-- queden. Esta consulta los lista.
-- Criterio: PGAP de carga N registrado > 30 min después del primer registro de
-- APRT de esa carga, sobre una fila cuyo NRO_EJECUCION = 1.
-- -----------------------------------------------------------------------------
WITH PRIMERA AS (
        SELECT  a.APRTIDAS AS ID_CARGA, MIN(a.APRTFCRG) AS INICIO_EJEC_1
        FROM    CRD.APRT a
        WHERE   a.APRTIDAS IS NOT NULL AND a.APRTUSRG = 'SAA_AH'
        GROUP BY a.APRTIDAS
),
RUNS AS (
        SELECT  x.ID_CARGA, x.INICIO_EJEC_1,
                MIN(CASE WHEN x.INICIO = 1 AND x.APRTFCRG > x.INICIO_EJEC_1 THEN x.APRTFCRG END) AS INICIO_EJEC_2
        FROM  ( SELECT  a.APRTIDAS AS ID_CARGA, a.APRTFCRG, p.INICIO_EJEC_1,
                        CASE WHEN CAST(a.APRTFCRG AS DATE)
                                - CAST(LAG(a.APRTFCRG) OVER (PARTITION BY a.APRTIDAS
                                                             ORDER BY a.APRTFCRG, a.APRTCDGO) AS DATE)
                                > 30/1440 THEN 1 ELSE 0 END AS INICIO
                FROM    CRD.APRT a
                JOIN    PRIMERA p ON p.ID_CARGA = a.APRTIDAS
                WHERE   a.APRTIDAS IS NOT NULL AND a.APRTUSRG = 'SAA_AH' ) x
        GROUP BY x.ID_CARGA, x.INICIO_EJEC_1
        HAVING  MIN(CASE WHEN x.INICIO = 1 AND x.APRTFCRG > x.INICIO_EJEC_1 THEN x.APRTFCRG END) IS NOT NULL
)
SELECT  r.ID_CARGA,
        c.CRARANAF || '-' || LPAD(c.CRARMSAF, 2, '0') AS PERIODO,
        r.INICIO_EJEC_1, r.INICIO_EJEC_2,
        p.PGAPCDGO AS ID_PGAP, p.PGAPFCRG AS FECHA_PGAP, p.PGAPVLRR AS VALOR_PGAP,
        a.APRTCDGO AS ID_APORTE, a.APRTFCRG AS FECHA_APORTE, a.APRTIDAS AS CARGA_DEL_APORTE,
        a.APRTVLRR AS VALOR_APORTE, a.APRTVLPG AS PAGADO_APORTE, a.APRTSLDO AS SALDO_APORTE, a.APRTIDST AS ESTADO_APORTE,
        e.ENTDNMID AS NUMERO_IDENTIFICACION, e.ENTDRZNS AS RAZON_SOCIAL,
        p.PGAPCNCP AS CONCEPTO
FROM    RUNS r
JOIN    CRD.PGAP p ON TO_NUMBER(REGEXP_SUBSTR(p.PGAPCNCP, 'CargaArchivo: ([0-9]+)\s*$', 1, 1, NULL, 1)) = r.ID_CARGA
                  AND p.PGAPUSRG = 'SISTEMA'
                  AND p.PGAPFCRG >= r.INICIO_EJEC_2
JOIN    CRD.APRT a ON a.APRTCDGO = p.APRTCDGO
                  AND a.APRTFCRG < r.INICIO_EJEC_2
JOIN    CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
LEFT    JOIN CRD.CRAR c ON c.CRARCDGO = r.ID_CARGA
ORDER BY r.ID_CARGA, e.ENTDNMID, p.PGAPCDGO;


-- =============================================================================
-- A8 - IMPACTO: SALDOS QUE QUEDARÍAN NEGATIVOS AL RETIRAR EL EXCESO
-- =============================================================================
-- Desde el 2026-08-14 hay pagos con aportes y devoluciones (filas negativas)
-- que se validaron contra SUM(valor). Si ese saldo estaba inflado, el partícipe
-- pudo usar plata que no tenía. Al retirar el exceso el saldo quedaría negativo.
-- Esos casos NO se corrigen con el resto: van a revisión individual.
--   SALDO_ACTUAL     SUM(valor) de todas las filas del tipo (positivas y negativas)
--   EXCESO           VALOR_CARGA - APLICADO_PGAP por tipo (la parte por tipo del
--                    exceso; el EXCESO_DINERO por M1 se reparte según A7)
--   SALDO_CORREGIDO  SALDO_ACTUAL - EXCESO
-- =============================================================================
WITH FILAS AS (
        SELECT  a.ENTDCDGO, a.TPAPCDGO, SUM(a.APRTVLRR) AS VALOR
        FROM    CRD.APRT a
        WHERE   a.APRTVLRR > 0
        AND     (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %')
        GROUP BY a.ENTDCDGO, a.TPAPCDGO
),
PAGOS AS (
        SELECT  a.ENTDCDGO, a.TPAPCDGO, SUM(p.PGAPVLRR) AS APLICADO
        FROM    CRD.PGAP p
        JOIN    CRD.APRT a ON a.APRTCDGO = p.APRTCDGO
        WHERE   p.PGAPUSRG = 'SISTEMA'
        AND     p.PGAPCNCP LIKE 'Pago aporte mes %CargaArchivo: %'
        GROUP BY a.ENTDCDGO, a.TPAPCDGO
),
SALDO AS (
        SELECT  a.ENTDCDGO, a.TPAPCDGO,
                SUM(a.APRTVLRR) AS SALDO_ACTUAL,
                SUM(CASE WHEN a.APRTVLRR < 0 THEN a.APRTVLRR ELSE 0 END) AS NEGATIVOS,
                SUM(CASE WHEN a.APRTVLRR < 0 THEN 1 ELSE 0 END) AS FILAS_NEGATIVAS
        FROM    CRD.APRT a
        GROUP BY a.ENTDCDGO, a.TPAPCDGO
)
SELECT  e.ENTDNMID AS NUMERO_IDENTIFICACION, e.ENTDRZNS AS RAZON_SOCIAL,
        tp.TPAPNMBR AS TIPO_APORTE,
        s.SALDO_ACTUAL,
        s.FILAS_NEGATIVAS, s.NEGATIVOS,
        f.VALOR AS VALOR_CARGA, NVL(pg.APLICADO, 0) AS APLICADO_PGAP,
        ROUND(f.VALOR - NVL(pg.APLICADO, 0), 2) AS EXCESO,
        ROUND(s.SALDO_ACTUAL - (f.VALOR - NVL(pg.APLICADO, 0)), 2) AS SALDO_CORREGIDO,
        e.ENTDCDGO AS ID_ENTIDAD, f.TPAPCDGO AS ID_TIPO
FROM    FILAS f
LEFT    JOIN PAGOS pg ON pg.ENTDCDGO = f.ENTDCDGO AND pg.TPAPCDGO = f.TPAPCDGO
JOIN    SALDO s  ON s.ENTDCDGO = f.ENTDCDGO AND s.TPAPCDGO = f.TPAPCDGO
JOIN    CRD.ENTD e  ON e.ENTDCDGO = f.ENTDCDGO
JOIN    CRD.TPAP tp ON tp.TPAPCDGO = f.TPAPCDGO
WHERE   f.VALOR - NVL(pg.APLICADO, 0) > 0.02
AND     s.SALDO_ACTUAL - (f.VALOR - NVL(pg.APLICADO, 0)) < -0.01
ORDER BY SALDO_CORREGIDO, e.ENTDNMID;

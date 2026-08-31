-- =============================================================================
-- 01 — DIAGNOSTICO DE PRECONDICIONES DEL FRENTE DE CORRECCION DE DUPLICADOS
-- =============================================================================
-- FECHA: 2026-08-31 · Equipo omen-saa-2
-- Acompaña a README.md de esta carpeta. Leerlo antes de interpretar cualquier
-- resultado de acá.
--
-- ⛔ SOLO LECTURA. NINGUN DML, NINGUN DDL, NINGUNA TABLA NUEVA.
--    Por eso NO lleva bloque de reverso: no hay nada que revertir. Es seguro
--    correrlo de corrido, en produccion, en horario de trabajo.
--
-- -----------------------------------------------------------------------------
-- QUE RESPONDE, Y POR QUE CADA PREGUNTA BLOQUEA
-- -----------------------------------------------------------------------------
--   1  ¿Existe CRD.APRT.CRARCDGO en la base?
--      Aporte.java:85 la mapea como @JoinColumn. Hibernate incluye toda columna
--      mapeada en el SELECT que genera: si la columna NO existe y el WAR ya se
--      desplego, TODA lectura de aportes revienta con ORA-00904. No se ve al
--      compilar; aparece cuando un usuario abre la pantalla.
--
--   2  ¿Corrio 74_RESTAURACION_VALOR_APORTES_ANULADOS.sql?
--      Mientras las 2.635 filas de junio 2025 sigan en APRTVLRR = 0, el analisis
--      del 69 vuelve a mentir por la misma razon que motivo su V2: el filtro
--      APRTVLRR > 0 las descarta y todas las cifras salen incompletas, otra vez
--      sin decirlo.
--
--   3  ¿Esta apagado el flag contable de CRD (rubro 237)?
--      Con el flag en 0 no hay asientos emitidos por estos aportes: corregir es
--      corregir datos. Con el flag en 1 hay que reversar asientos ademas.
--
--   4  ¿Que filas de carga hay, por version del generador?
--      Confirma que las filas de junio 2025 estan visibles con el universo
--      correcto (por FORMA, nunca por APRTUSRG — ver README §2).
--
--   5  ¿Cuantos participes JUBILADO COMPLEMENTARIO tienen exceso?
--      Es la decision D4 y hoy tiene un proceso vivo detras: si se jubila a
--      alguien con saldo inflado, se le entrega dinero que no aporto y eso ya
--      no lo arregla una correccion de datos.
--
--   6  ¿Cuanta exposicion hay en CRD.PGPR (pagos de prestamo de la carga)?
--      Es la decision D5 y es lo que le llega al equipo B.
--
-- -----------------------------------------------------------------------------
-- COMO LEERLO
-- -----------------------------------------------------------------------------
--   Los bloques 1, 2 y 3 son SEMAFOROS: cada uno devuelve una sola fila con una
--   columna que dice OK o PARAR. Si alguno dice PARAR, los bloques 4 a 6 se
--   pueden mirar igual, pero sus cifras NO son definitivas.
-- =============================================================================


-- =============================================================================
-- 1. ¿EXISTE CRD.APRT.CRARCDGO?   (riesgo ORA-00904 en toda lectura de aportes)
-- =============================================================================
SELECT  CASE WHEN COUNT(*) = 1
             THEN 'OK — la columna existe'
             ELSE 'PARAR — CRD.APRT.CRARCDGO NO EXISTE y Aporte.java la mapea: si el WAR esta desplegado, toda lectura de aportes falla con ORA-00904. Correr DDL-TRAZABILIDAD-CARGA-PETRO.sql'
        END                                     AS SEMAFORO_CRARCDGO,
        COUNT(*)                                AS COLUMNAS_ENCONTRADAS
FROM    ALL_TAB_COLUMNS
WHERE   OWNER       = 'CRD'
AND     TABLE_NAME  = 'APRT'
AND     COLUMN_NAME = 'CRARCDGO';


-- =============================================================================
-- 1b. DE PASO: ¿cuantas filas ya tienen CRARCDGO?  (mide si corrio el 78)
-- =============================================================================
-- Si el bloque 1 dijo PARAR, esta consulta falla con ORA-00904. Es esperado:
-- saltearla y seguir en el bloque 2.
-- =============================================================================
SELECT  COUNT(*)                                                   AS FILAS_CON_APRTIDAS,
        SUM(CASE WHEN CRARCDGO IS NOT NULL THEN 1 ELSE 0 END)      AS FILAS_CON_CRARCDGO,
        SUM(CASE WHEN CRARCDGO IS NULL     THEN 1 ELSE 0 END)      AS FILAS_PENDIENTES_DE_BACKFILL
FROM    CRD.APRT
WHERE   APRTIDAS IS NOT NULL;


-- =============================================================================
-- 2. ¿CORRIO EL 74?   (¿siguen filas de carga en valor 0?)
-- =============================================================================
-- La tabla de respaldo CRD.BKP_APRT_VALOR_20260827 la crea el 62. Si NO existe,
-- puede ser que el 62 nunca corrio en esta base, o que el usuario la elimino tras
-- validar la restauracion. Las dos consultas de abajo distinguen los casos.
-- =============================================================================
SELECT  CASE WHEN COUNT(*) = 1
             THEN 'La tabla de respaldo EXISTE — seguir con 2b'
             ELSE 'La tabla de respaldo NO existe — el 62 no corrio aca, o ya se elimino tras validar. Seguir con 2c'
        END                                     AS ESTADO_RESPALDO
FROM    ALL_TABLES
WHERE   OWNER      = 'CRD'
AND     TABLE_NAME = 'BKP_APRT_VALOR_20260827';


-- =============================================================================
-- 2b. SOLO SI 2 DIJO "EXISTE" — filas que el 62 puso en cero y siguen en cero
-- =============================================================================
SELECT  CASE WHEN COUNT(*) = 0
             THEN 'OK, RESTAURADO — el 74 ya corrio (o no hizo falta)'
             ELSE 'PARAR — CORRER 74_RESTAURACION_VALOR_APORTES_ANULADOS.sql ANTES DE SEGUIR'
        END                                     AS SEMAFORO_RESTAURACION,
        COUNT(*)                                AS FILAS_TODAVIA_EN_CERO,
        ROUND(SUM(b.APRTVLRR), 2)               AS VALOR_PENDIENTE_DE_RESTAURAR
FROM    CRD.APRT a
JOIN    CRD.BKP_APRT_VALOR_20260827 b ON b.APRTCDGO = a.APRTCDGO
WHERE   NVL(a.APRTVLRR, 0) = 0
AND     b.APRTVLRR > 0;


-- =============================================================================
-- 2c. CONTROL INDEPENDIENTE DEL RESPALDO — filas de carga en valor 0
-- =============================================================================
-- No depende de la tabla de respaldo: busca la firma de las filas danadas
-- directamente. Una fila creada por la carga NUNCA deberia valer 0.
-- Universo por FORMA, sin usar APRTUSRG (README §2).
-- =============================================================================
SELECT  CASE WHEN COUNT(*) = 0
             THEN 'OK — ninguna fila de carga esta en valor 0'
             ELSE 'REVISAR — hay filas de carga en valor 0; ver detalle abajo'
        END                                     AS SEMAFORO_VALOR_CERO,
        COUNT(*)                                AS FILAS_EN_CERO,
        COUNT(DISTINCT a.ENTDCDGO)              AS PARTICIPES,
        MIN(a.APRTFCTR)                         AS DESDE,
        MAX(a.APRTFCTR)                         AS HASTA
FROM    CRD.APRT a
WHERE   NVL(a.APRTVLRR, 0) = 0
AND     a.TPAPCDGO IN (9, 11)
AND     (   a.APRTIDAS IS NOT NULL
         OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
         OR a.APRTGLSA LIKE 'Abono al aporte%');


-- =============================================================================
-- 3. FLAG CONTABLE DE CRD — rubro 237, detalle alterno 1 (CONTABILIDAD_ACTIVA)
-- =============================================================================
-- El valor vive en PDTRVLRN (0/1) segun CrdParametroContabilidad.java. Se traen
-- tambien PDTRVLRV y PDTRESTD para no depender de esa suposicion.
-- =============================================================================
SELECT  d.PDTRCDGO,
        d.PDTRALTR                              AS ALTERNO,
        d.PDTRDSCR                              AS DESCRIPCION,
        d.PDTRVLRN                              AS VALOR_NUMERICO,
        d.PDTRVLRV                              AS VALOR_TEXTO,
        d.PDTRESTD                              AS ESTADO,
        CASE WHEN NVL(d.PDTRVLRN, 0) = 0
             THEN 'OK — contabilidad APAGADA: corregir ahora es solo corregir datos'
             ELSE 'ATENCION — contabilidad ENCENDIDA: la correccion tambien tiene que reversar asientos'
        END                                     AS SEMAFORO_FLAG
FROM    SCP.PDTR d
WHERE   d.PRBRCDGO = 237
ORDER BY d.PDTRALTR;


-- =============================================================================
-- 4. UNIVERSO DE FILAS DE CARGA, POR VERSION DEL GENERADOR
-- =============================================================================
-- Mismo criterio que 69 §0.1: la version se deduce de la FORMA de la fila, nunca
-- de APRTUSRG. Se agrega FILAS_SIN_USUARIO para ver de un vistazo por que el
-- filtro viejo (APRTUSRG = 'SAA_AH') no las veia.
--
-- Nota: a diferencia del 69, aca NO se filtra APRTVLRR > 0. Si el 74 no corrio,
-- las filas danadas valen 0 y con ese filtro desaparecerian — que es exactamente
-- el error que este bloque tiene que poder mostrar.
-- =============================================================================
WITH CLASIFICADAS AS (
        SELECT  a.*,
                CASE
                    WHEN a.APRTGLSA LIKE 'Abono al aporte%'                     THEN '4. V2 EXCEDENTE (vieja)'
                    WHEN a.APRTGLSA LIKE 'Aporte % - Mes %/% - CargaArchivo: %' THEN '5. V3 VIGENTE'
                    WHEN a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                         AND a.APRTGLSA NOT LIKE '%- Mes %'
                         AND a.APRTIDAS IS NULL
                         AND a.APRTFCRG IS NULL
                         AND a.APRTFCTR = TRUNC(a.APRTFCTR)                     THEN '2. V1 SIN FECHA DE REGISTRO (junio 2025)'
                    WHEN a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                         AND a.APRTGLSA NOT LIKE '%- Mes %'
                         AND a.APRTIDAS IS NULL                                 THEN '3. V1 CON REGISTRO'
                    WHEN a.APRTIDAS IS NOT NULL                                 THEN '6. OTRA CON CARGA'
                    ELSE                                                             '1. MANUAL / MIGRADA'
                END AS VERSION
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (9, 11)
        AND     NVL(a.APRTVLRR, 0) >= 0          -- se excluyen los negativos: pagos, devoluciones, jubilacion
)
SELECT  VERSION,
        COUNT(*)                                            AS FILAS,
        COUNT(DISTINCT ENTDCDGO)                            AS PARTICIPES,
        MIN(APRTFCTR)                                       AS MIN_FECHA_TRANSACCION,
        MAX(APRTFCTR)                                       AS MAX_FECHA_TRANSACCION,
        ROUND(SUM(NVL(APRTVLRR, 0)), 2)                     AS VALOR,
        ROUND(SUM(NVL(APRTVLPG, 0)), 2)                     AS VALOR_PAGADO,
        ROUND(SUM(NVL(APRTSLDO, 0)), 2)                     AS SALDO_FIFO,
        SUM(CASE WHEN APRTUSRG IS NULL          THEN 1 ELSE 0 END) AS FILAS_SIN_USUARIO,
        SUM(CASE WHEN NVL(APRTVLRR, 0) = 0      THEN 1 ELSE 0 END) AS FILAS_EN_CERO,
        SUM(CASE WHEN APRTIDST = 6              THEN 1 ELSE 0 END) AS FILAS_PARCIAL
FROM    CLASIFICADAS
GROUP BY VERSION
ORDER BY VERSION;


-- =============================================================================
-- 4b. LAS CARGAS DEL PERIODO, DE JUNIO 2025 EN ADELANTE
-- =============================================================================
-- Contexto para decidir D3 (alcance temporal). CRARESTD = 3 es "procesada".
-- =============================================================================
SELECT  c.CRARCDGO                                          AS ID_CARGA,
        c.CRARANAF || '-' || LPAD(c.CRARMSAF, 2, '0')       AS PERIODO,
        c.FLLLCDGO                                          AS FILIAL,
        c.CRARESTD                                          AS ESTADO_CARGA,
        c.CRARFCCR                                          AS FECHA_CREACION,
        ROUND(NVL(ar.DESCONTADO_AH, 0), 2)                  AS DESCONTADO_AH,
        NVL(ap.FILAS_APRT, 0)                               AS FILAS_APRT,
        ROUND(NVL(ap.VALOR_APRT, 0), 2)                     AS VALOR_APRT,
        ROUND(NVL(ap.VALOR_APRT, 0) - NVL(ar.DESCONTADO_AH, 0), 2) AS DIFERENCIA
FROM    CRD.CRAR c
LEFT    JOIN (  SELECT  d.CRARCDGO, SUM(NVL(x.PXCADSDO, 0)) AS DESCONTADO_AH
                FROM    CRD.DTCA d
                JOIN    CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
                WHERE   d.DTCACDPP = 'AH'
                GROUP BY d.CRARCDGO ) ar ON ar.CRARCDGO = c.CRARCDGO
LEFT    JOIN (  SELECT  NVL(a.APRTIDAS,
                            TO_NUMBER(REGEXP_SUBSTR(a.APRTGLSA, 'CargaArchivo: ([0-9]+)', 1, 1, NULL, 1))) AS ID_CARGA,
                        COUNT(*) AS FILAS_APRT, SUM(NVL(a.APRTVLRR, 0)) AS VALOR_APRT
                FROM    CRD.APRT a
                WHERE   a.TPAPCDGO IN (9, 11)
                AND     NVL(a.APRTVLRR, 0) >= 0
                AND     (   a.APRTIDAS IS NOT NULL
                         OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                         OR a.APRTGLSA LIKE 'Abono al aporte%')
                GROUP BY NVL(a.APRTIDAS,
                             TO_NUMBER(REGEXP_SUBSTR(a.APRTGLSA, 'CargaArchivo: ([0-9]+)', 1, 1, NULL, 1)))
             ) ap ON ap.ID_CARGA = c.CRARCDGO
WHERE   (c.CRARANAF > 2025 OR (c.CRARANAF = 2025 AND c.CRARMSAF >= 6))
ORDER BY c.CRARANAF, c.CRARMSAF, c.FLLLCDGO;


-- =============================================================================
-- 5. ⛔ EXPOSICION DE LOS JUBILADOS — decision D4
-- =============================================================================
-- Participes en estado JUBILADO COMPLEMENTARIO que ademas tienen filas de carga.
-- Si su EXCESO_TOTAL es positivo, su saldo esta inflado y el proceso de jubilacion
-- puede estarles pagando dinero que no aportaron.
--
-- OJO CON EL ESTADO: CRD.ENTD.ENTDIDST guarda el CODIGO ALTERNO del catalogo
-- (ESPR.ESPRCDEX), NO el PK (ESPRCDGO). JUBILADO_COMPLEMENTARIO = 3 en alterno
-- (PK 30). Es la trampa documentada en MIGRACION-ESTADO-PARTICIPE.md; filtrar por
-- el PK devuelve cero filas sin error.
-- Verificado: EntidadDaoServiceImpl.selectByIdEstado (:305-311) filtra por
-- Entidad.idEstado = ENTDIDST, y PagoPensionComplementariaServiceImpl:181 le pasa
-- EstadoParticipeEntidad.JUBILADO_COMPLEMENTARIO = 3.
--
-- ⚠ LIMITACION DELIBERADA DE ESTE BLOQUE: SALDO_ACTUAL agrega TODOS los tipos de
--   aporte del participe (incluido el 23, pension complementaria), mientras que el
--   exceso se mide solo sobre los tipos 9 y 11. Alcanza para saber A QUIEN mirar,
--   NO para decidir su caso: la jubilacion paga desde el tipo 23, asi que un
--   participe puede salir "no queda negativo" aca y estarlo en el tipo que se le
--   paga. Para el caso a caso, usar el desglose por tipo del 69 §3.
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
FILAS_CARGA AS (
        SELECT  a.ENTDCDGO,
                COUNT(*)                        AS FILAS,
                SUM(NVL(a.APRTVLRR, 0))         AS VALOR
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (9, 11)
        AND     NVL(a.APRTVLRR, 0) > 0
        AND     (   a.APRTIDAS IS NOT NULL
                 OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                 OR a.APRTGLSA LIKE 'Abono al aporte%')
        GROUP BY a.ENTDCDGO
),
SALDO_TOTAL AS (
        -- El saldo real del participe, modelo vigente: SUM(APRTVLRR) sin filtro,
        -- incluyendo los negativos (pagos con aportes, devoluciones, jubilacion).
        SELECT  a.ENTDCDGO, SUM(NVL(a.APRTVLRR, 0)) AS SALDO
        FROM    CRD.APRT a
        GROUP BY a.ENTDCDGO
)
SELECT  e.ENTDNMID                                          AS IDENTIFICACION,
        e.ENTDRZNS                                          AS PARTICIPE,
        e.ENTDRLPC                                          AS ROL_PETRO,
        NVL(esp.ESPRNMBR, TO_CHAR(e.ENTDIDST))              AS ESTADO_PARTICIPE,
        f.FILAS                                             AS FILAS_DE_CARGA,
        ROUND(NVL(d.DESCONTADO, 0), 2)                      AS DESCONTADO_REAL,
        ROUND(f.VALOR, 2)                                   AS VALOR_REGISTRADO,
        ROUND(f.VALOR - NVL(d.DESCONTADO, 0), 2)            AS EXCESO_TOTAL,
        ROUND(NVL(s.SALDO, 0), 2)                           AS SALDO_ACTUAL,
        ROUND(NVL(s.SALDO, 0) - (f.VALOR - NVL(d.DESCONTADO, 0)), 2) AS SALDO_SI_SE_CORRIGE,
        CASE WHEN NVL(s.SALDO, 0) - (f.VALOR - NVL(d.DESCONTADO, 0)) < 0
             THEN '⛔ QUEDA NEGATIVO — decision D4, caso individual'
             WHEN f.VALOR - NVL(d.DESCONTADO, 0) > 0.02
             THEN 'Saldo inflado — corregible sin dejarlo en negativo'
             ELSE 'Sin exceso'
        END                                                 AS DIAGNOSTICO
FROM    FILAS_CARGA f
JOIN    CRD.ENTD e ON e.ENTDCDGO = f.ENTDCDGO
LEFT    JOIN DESCONTADO  d ON d.ENTDCDGO = f.ENTDCDGO
LEFT    JOIN SALDO_TOTAL s ON s.ENTDCDGO = f.ENTDCDGO
LEFT    JOIN CRD.ESPR esp ON esp.ESPRCDEX = e.ENTDIDST
WHERE   e.ENTDIDST = 3                       -- JUBILADO COMPLEMENTARIO (alterno, no PK)
AND     ABS(f.VALOR - NVL(d.DESCONTADO, 0)) > 0.02
ORDER BY (f.VALOR - NVL(d.DESCONTADO, 0)) DESC;


-- =============================================================================
-- 5b. RESUMEN DE JUBILADOS — para decidir si hace falta frenar el proceso
-- =============================================================================
SELECT  COUNT(*)                                                        AS JUBILADOS_COMPLEMENTARIOS,
        SUM(CASE WHEN NVL(f.VALOR, 0) > 0 THEN 1 ELSE 0 END)            AS CON_FILAS_DE_CARGA
FROM    CRD.ENTD e
LEFT    JOIN (  SELECT  a.ENTDCDGO, SUM(NVL(a.APRTVLRR, 0)) AS VALOR
                FROM    CRD.APRT a
                WHERE   a.TPAPCDGO IN (9, 11)
                AND     NVL(a.APRTVLRR, 0) > 0
                AND     (   a.APRTIDAS IS NOT NULL
                         OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                         OR a.APRTGLSA LIKE 'Abono al aporte%')
                GROUP BY a.ENTDCDGO ) f ON f.ENTDCDGO = e.ENTDCDGO
WHERE   e.ENTDIDST = 3;


-- =============================================================================
-- 6. EXPOSICION EN CRD.PGPR — pagos de prestamo escritos por la carga (D5)
-- =============================================================================
-- ⛔ TRAMPA: el texto de PGPR NO termina igual que el de PGAP.
--      PGAP.PGAPCNCP  ... - CargaArchivo: 352
--      PGPR.PGPROBSR  ... [CargaArchivo: 352]     <- corchete de cierre
--   El 69 usa REGEXP_SUBSTR(..., 'CargaArchivo: ([0-9]+)\s*$', ...) ANCLADO al
--   final. Copiado tal cual a PGPR devuelve CERO filas y no avisa. Aca el patron
--   va SIN ancla, a proposito.
--   Verificado en CargaArchivoPetroServiceImpl:3214.
-- =============================================================================
SELECT  c.CRARCDGO                                          AS ID_CARGA,
        c.CRARANAF || '-' || LPAD(c.CRARMSAF, 2, '0')       AS PERIODO,
        c.CRARESTD                                          AS ESTADO_CARGA,
        COUNT(*)                                            AS PAGOS_PRESTAMO,
        COUNT(DISTINCT p.PGPRNMCT)                          AS CUOTAS_DISTINTAS,
        ROUND(SUM(NVL(p.PGPRVLRR, 0)), 2)                   AS VALOR_APLICADO,
        MIN(p.PGPRFCRG)                                     AS PRIMER_REGISTRO,
        MAX(p.PGPRFCRG)                                     AS ULTIMO_REGISTRO,
        ROUND((CAST(MAX(p.PGPRFCRG) AS DATE) - CAST(MIN(p.PGPRFCRG) AS DATE)) * 24, 1) AS HORAS_ENTRE_EXTREMOS
FROM    CRD.PGPR p
JOIN    CRD.CRAR c
        ON c.CRARCDGO = TO_NUMBER(REGEXP_SUBSTR(p.PGPROBSR, 'CargaArchivo: ([0-9]+)', 1, 1, NULL, 1))
WHERE   p.PGPROBSR LIKE '%CargaArchivo: %'
GROUP BY c.CRARCDGO, c.CRARANAF, c.CRARMSAF, c.CRARESTD
ORDER BY c.CRARANAF, c.CRARMSAF, c.CRARCDGO;


-- =============================================================================
-- 6b. CONTROL DE LA TRAMPA — cuantas filas ve cada patron
-- =============================================================================
-- Si PATRON_ANCLADO da 0 y PATRON_CORRECTO da miles, queda demostrado en esta
-- misma base por que no se puede copiar el regex del 69 a PGPR.
-- =============================================================================
SELECT  COUNT(*)                                                                   AS TOTAL_CON_TEXTO_DE_CARGA,
        SUM(CASE WHEN REGEXP_SUBSTR(p.PGPROBSR, 'CargaArchivo: ([0-9]+)\s*$', 1, 1, NULL, 1) IS NOT NULL
                 THEN 1 ELSE 0 END)                                                AS PATRON_ANCLADO_69,
        SUM(CASE WHEN REGEXP_SUBSTR(p.PGPROBSR, 'CargaArchivo: ([0-9]+)', 1, 1, NULL, 1) IS NOT NULL
                 THEN 1 ELSE 0 END)                                                AS PATRON_CORRECTO
FROM    CRD.PGPR p
WHERE   p.PGPROBSR LIKE '%CargaArchivo: %';


-- =============================================================================
-- FIN. Nada de este script modifica datos.
-- Con los resultados en mano se decide D3 (alcance temporal), D4 (jubilados en
-- negativo) y D5 (¿se toca PGPR?), y recien ahi se escribe 02_CORRECCION_*.sql.
-- =============================================================================

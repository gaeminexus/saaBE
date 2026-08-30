-- =============================================================================
-- 76 - AUDITORIA DE CORRIMIENTO UTC EN COLUMNAS DE FECHA MAPEADAS COMO STRING
-- =============================================================================
-- FECHA: 2026-08-27
--
-- QUE SE ESTA BUSCANDO
--   El frontend documentó y corrigió un defecto sistémico (docs/patrones/
--   FECHA-SOLO-DIA-CORRIMIENTO-UTC.md en el repo saaFE — no existe en este repo, es
--   documentación de frontend): `new Date("yyyy-MM-dd")` se interpreta como medianoche
--   UTC, y con Ecuador en UTC-5 cada ciclo guardar→leer→guardar corre la fecha un día
--   hacia atrás. Los campos mapeados como LocalDate en este backend quedan protegidos
--   por ese arreglo (el backend nunca vuelve a serializar mal lo que ya llegó bien). El
--   riesgo PERSISTENTE está en columnas Oracle VARCHAR2 mapeadas como String: si un valor
--   pasó alguna vez por esa pantalla, el corrimiento quedó escrito en la base y ningún
--   arreglo de frontend lo revierte solo. Este script es SOLO DIAGNÓSTICO: mide si hay
--   rastro y de qué tamaño, sin corregir nada.
--
-- ALCANCE: LOS DOS CAMPOS QUE PIDIÓ EL USUARIO
--   CRD.ENTD.ENTDFCIN            (Entidad.fechaIngreso)
--   ~20 columnas de fecha en RPR.HM40, HM41, HM43, HM44, HM46, HM47, HM48, HM49, HM50,
--   HM51, HMJB, HMCP (Historico{G40,G41,G43-G51,CJBM,CCPM})
--
-- QUE SE ENCONTRÓ AL RASTREAR EL ORIGEN DE CADA COLUMNA (trazado contra el código de
-- GeneracionGxxServiceImpl, 2026-08-27) — informa cómo leer los resultados de abajo:
--
--   SEGURAS (se descartan de este diagnóstico; el valor se copia en cada corrida desde un
--   LocalDate/LocalDateTime real, o se calcula fresco con LocalDate.of(), sin round-trip
--   por el navegador):
--     HM41.HM41FNDP (fechaNacimiento)              <- CRD.EXTR.EXTRFCNC (LocalDateTime)
--     HM41.HM41FIDP (fechaIngreso)                 <- Participe.fechaIngresoFondo (LocalDate/Time)
--     HM45.HM45FDNC (fechaNacimiento)               <- misma familia de fuente segura
--     HM43.HM43FTRL, HM43.HM43FCLQ                  <- calculadas con LocalDate.of(anio,mes,...)
--     HM49.HM49FCCN (fechaCancelacion)              <- calculada con LocalDate.of(...)
--     HM47.HM47FDNR (fechaNovacion)                 <- Prestamo.fecha (LocalDateTime)
--     HM46.HM46FCCN, HM46.HM46FCVN                  <- Prestamo.fecha / Prestamo.fechaFin
--     HM51.HM51FDAV, HM51.HM51FDLC                  <- hoy siempre NULL (no las llena nada
--                                                       en el código actual) — bloque 5
--
--   CON FUENTE INDEPENDIENTE PARA COMPARAR (diagnóstico fuerte, igual que hizo
--   72_CORRECCION_FECHA_NACIMIENTO_ENTD.sql con CRD.EXTR):
--     HM48.HM48FDEC (fechaExigibilidadCuota)        <-> RPR.CG48.CG48FDEC (LocalDate),
--                                                        misma cuota por HM48NMOP=CG48NMOP
--     HMCP.HMCPFDEC (fechaExigibilidadCuota)        <-> RPR.CCPM.CCPMFDEC (LocalDate),
--                                                        misma cuota por HMCPNMOP=CCPMNMOP
--     — bloque 2. CG48/CCPM son las tablas que de verdad alimentan el reporte con tipo
--     LocalDate; HM48/HMCP son históricos en paralelo, de escritor no identificado en este
--     barrido. Si difieren con la firma del defecto, HM48/HMCP son las corridas.
--
--   SIN FUENTE INDEPENDIENTE CONOCIDA (diagnóstico débil: solo formato y plausibilidad,
--   NO concluyente por sí solo; antes de decidir cualquier corrección hay que ubicar qué
--   proceso escribe cada una — no se identificó en este barrido):
--     CRD.ENTD.ENTDFCIN (fechaIngreso)              — bloque 1. CRD.EXTR NO tiene columna
--                                                       de fecha de ingreso (solo
--                                                       EXTRFCNC/EXTRFCDF), así que no hay
--                                                       cruce posible como el de ENTDFCNC.
--     HM44.HM44FCJB, HMJB.HMJBFCJB (fechaJubilacion) — bloque 3. El código las llena
--                                                       parseando un string de otra fuente
--                                                       (no un LocalDate ya sano): hereda
--                                                       el riesgo si esa fuente lo tenía.
--     HM40.HM40FCRS/HM40FCTR/HM40FRCE                — bloque 4. Escritor no localizado.
--     HM50.HM50FDEG (fechaEliminacionGarante)         — bloque 4. Escritor no localizado.
--
-- METODOLOGÍA (igual que 72_CORRECCION_FECHA_NACIMIENTO_ENTD.sql, bloque 0):
--   Primero se mide el FORMATO real de cada columna VARCHAR2 (puede traer cualquier cosa),
--   porque sin eso no se puede ni siquiera parsear la fecha para comparar. TO_DATE con un
--   formato equivocado sobre una columna así de sucia rompe la consulta o, peor, parsea mal
--   en silencio: no asumir 'YYYY-MM-DD' sin haberlo confirmado primero.
--
-- SOLO LECTURA. Ningún DML. Ninguna corrección: primero hay que ver si hay daño y de qué
-- tamaño, y decidir con eso — igual que se hizo con ENTDFCNC en el 72.
-- SQL PURO: sin SET / DEFINE / WHENEVER.
--
-- ÍNDICE
--   1  ENTD.ENTDFCIN            — formato + heurística (sin fuente independiente)
--   2  HM48/CG48 y HMCP/CCPM    — comparación directa contra la fuente LocalDate
--   3  HM44/HMJB fechaJubilacion — formato + plausibilidad (sin fuente independiente)
--   4  HM40 y HM50               — formato + ¿siquiera están en uso?
--   5  HM51                      — confirmar que sigue en NULL (sin riesgo actual)
--   6  Resumen ejecutivo
-- =============================================================================


-- =============================================================================
-- 1. CRD.ENTD.ENTDFCIN (fecha de ingreso) — SIN FUENTE INDEPENDIENTE
-- =============================================================================

-- 1.1 Formato real de la columna (igual criterio que el bloque 0 del 72).
SELECT  CASE
            WHEN e.ENTDFCIN IS NULL                             THEN '(nulo)'
            WHEN REGEXP_LIKE(e.ENTDFCIN, '^\d{4}-\d{2}-\d{2}$')  THEN 'YYYY-MM-DD'
            WHEN REGEXP_LIKE(e.ENTDFCIN, '^\d{4}-\d{2}-\d{2} ')  THEN 'YYYY-MM-DD con hora'
            WHEN REGEXP_LIKE(e.ENTDFCIN, '^\d{2}/\d{2}/\d{4}$')  THEN 'DD/MM/YYYY'
            WHEN REGEXP_LIKE(e.ENTDFCIN, '^\d{4}/\d{2}/\d{2}$')  THEN 'YYYY/MM/DD'
            ELSE                                                      'OTRO'
        END                                   AS FORMATO,
        COUNT(*)                              AS FILAS,
        MIN(e.ENTDFCIN)                       AS EJEMPLO_MIN,
        MAX(e.ENTDFCIN)                       AS EJEMPLO_MAX
FROM    CRD.ENTD e
GROUP BY CASE
            WHEN e.ENTDFCIN IS NULL                             THEN '(nulo)'
            WHEN REGEXP_LIKE(e.ENTDFCIN, '^\d{4}-\d{2}-\d{2}$')  THEN 'YYYY-MM-DD'
            WHEN REGEXP_LIKE(e.ENTDFCIN, '^\d{4}-\d{2}-\d{2} ')  THEN 'YYYY-MM-DD con hora'
            WHEN REGEXP_LIKE(e.ENTDFCIN, '^\d{2}/\d{2}/\d{4}$')  THEN 'DD/MM/YYYY'
            WHEN REGEXP_LIKE(e.ENTDFCIN, '^\d{4}/\d{2}/\d{2}$')  THEN 'YYYY/MM/DD'
            ELSE                                                      'OTRO'
         END
ORDER BY 2 DESC;

-- 1.2 Heurística DÉBIL: concentración en el ÚLTIMO día del mes. El defecto SIEMPRE resta
--     un día; una fecha que en realidad era el día 1 de un mes queda escrita como el
--     último día del mes anterior. Una proporción de "último día del mes" muy por encima
--     de lo esperable (~3.3% si las fechas fueran uniformes) es un indicio, NO una prueba:
--     hay procesos (ingresos de fin de mes, cierres) que producen esto sin ningún defecto.
--     No corregir nada solo con esto.
SELECT  COUNT(*)                                                              AS TOTAL_FILAS,
        SUM(CASE WHEN TO_DATE(SUBSTR(e.ENTDFCIN,1,10),'YYYY-MM-DD')
                      = LAST_DAY(TO_DATE(SUBSTR(e.ENTDFCIN,1,10),'YYYY-MM-DD'))
                 THEN 1 ELSE 0 END)                                           AS EN_ULTIMO_DIA_MES,
        ROUND(100 * SUM(CASE WHEN TO_DATE(SUBSTR(e.ENTDFCIN,1,10),'YYYY-MM-DD')
                                  = LAST_DAY(TO_DATE(SUBSTR(e.ENTDFCIN,1,10),'YYYY-MM-DD'))
                             THEN 1 ELSE 0 END) / COUNT(*), 2)                AS PORCENTAJE
FROM    CRD.ENTD e
WHERE   e.ENTDFCIN IS NOT NULL
AND     REGEXP_LIKE(e.ENTDFCIN, '^\d{4}-\d{2}-\d{2}');

-- 1.3 Distribución por día del mes (1..31). Buscar si el día 1 está anormalmente hundido
--     frente a los días 2-5 (indicio de que ingresos del día 1 migraron al 28-31 anterior).
SELECT  TO_NUMBER(SUBSTR(e.ENTDFCIN, 9, 2)) AS DIA_DEL_MES, COUNT(*) AS FILAS
FROM    CRD.ENTD e
WHERE   e.ENTDFCIN IS NOT NULL
AND     REGEXP_LIKE(e.ENTDFCIN, '^\d{4}-\d{2}-\d{2}')
GROUP BY TO_NUMBER(SUBSTR(e.ENTDFCIN, 9, 2))
ORDER BY 1;


-- =============================================================================
-- 2. HM48/CG48 Y HMCP/CCPM — COMPARACIÓN DIRECTA CONTRA LA FUENTE LocalDate
-- =============================================================================
-- Mismo principio que el 72 con CRD.EXTR: CG48 y CCPM tienen la MISMA fecha en columna
-- LocalDate real (nunca pasó por el navegador como texto editable). Si HM48/HMCP
-- difieren, la firma del defecto es DIAS = 1 (HM48/HMCP atrasada exactamente un día,
-- posiblemente más de una vez si el registro se re-guardó varias veces).

-- 2.1 Formato real de HM48FDEC y HMCPFDEC (antes de parsear nada).
SELECT  'HM48' AS TABLA,
        CASE WHEN h.HM48FDEC IS NULL THEN '(nulo)'
             WHEN REGEXP_LIKE(h.HM48FDEC, '^\d{4}-\d{2}-\d{2}$') THEN 'YYYY-MM-DD'
             ELSE 'OTRO' END AS FORMATO,
        COUNT(*) AS FILAS
FROM    RPR.HM48 h
GROUP BY CASE WHEN h.HM48FDEC IS NULL THEN '(nulo)'
              WHEN REGEXP_LIKE(h.HM48FDEC, '^\d{4}-\d{2}-\d{2}$') THEN 'YYYY-MM-DD'
              ELSE 'OTRO' END
UNION ALL
SELECT  'HMCP' AS TABLA,
        CASE WHEN h.HMCPFDEC IS NULL THEN '(nulo)'
             WHEN REGEXP_LIKE(h.HMCPFDEC, '^\d{4}-\d{2}-\d{2}$') THEN 'YYYY-MM-DD'
             ELSE 'OTRO' END AS FORMATO,
        COUNT(*) AS FILAS
FROM    RPR.HMCP h
GROUP BY CASE WHEN h.HMCPFDEC IS NULL THEN '(nulo)'
              WHEN REGEXP_LIKE(h.HMCPFDEC, '^\d{4}-\d{2}-\d{2}$') THEN 'YYYY-MM-DD'
              ELSE 'OTRO' END
ORDER BY 1, 2;

-- 2.2 HM48 vs CG48. DIAS = CG48 (fuente segura) - HM48 (texto). POSITIVO significa que
--     HM48 quedó ATRASADA, que es exactamente la firma del defecto.
SELECT  g.CG48FDEC - TO_DATE(h.HM48FDEC,'YYYY-MM-DD') AS DIAS,
        COUNT(*) AS FILAS
FROM    RPR.HM48 h
JOIN    RPR.CG48 g ON g.CG48NMOP = h.HM48NMOP
WHERE   h.HM48FDEC IS NOT NULL AND g.CG48FDEC IS NOT NULL
AND     REGEXP_LIKE(h.HM48FDEC, '^\d{4}-\d{2}-\d{2}$')
GROUP BY g.CG48FDEC - TO_DATE(h.HM48FDEC,'YYYY-MM-DD')
ORDER BY 1;

-- 2.3 HMCP vs CCPM. Misma lógica.
SELECT  c.CCPMFDEC - TO_DATE(h.HMCPFDEC,'YYYY-MM-DD') AS DIAS,
        COUNT(*) AS FILAS
FROM    RPR.HMCP h
JOIN    RPR.CCPM c ON c.CCPMNMOP = h.HMCPNMOP
WHERE   h.HMCPFDEC IS NOT NULL AND c.CCPMFDEC IS NOT NULL
AND     REGEXP_LIKE(h.HMCPFDEC, '^\d{4}-\d{2}-\d{2}$')
GROUP BY c.CCPMFDEC - TO_DATE(h.HMCPFDEC,'YYYY-MM-DD')
ORDER BY 1;

-- 2.4 Detalle fila a fila SOLO de las que muestran la firma exacta (DIAS = 1), para tener
--     ejemplos concretos que citar si hay que escalar esto.
SELECT  'HM48' AS TABLA, h.HM48NMOP AS NUMERO_OPERACION, h.HM48IDSJ AS IDENTIFICACION,
        h.HM48FDEC AS FECHA_HISTORICA, TO_CHAR(g.CG48FDEC,'YYYY-MM-DD') AS FECHA_FUENTE_SEGURA
FROM    RPR.HM48 h
JOIN    RPR.CG48 g ON g.CG48NMOP = h.HM48NMOP
WHERE   h.HM48FDEC IS NOT NULL AND g.CG48FDEC IS NOT NULL
AND     REGEXP_LIKE(h.HM48FDEC, '^\d{4}-\d{2}-\d{2}$')
AND     g.CG48FDEC - TO_DATE(h.HM48FDEC,'YYYY-MM-DD') = 1
UNION ALL
SELECT  'HMCP', h.HMCPNMOP, h.HMCPIDSJ,
        h.HMCPFDEC, TO_CHAR(c.CCPMFDEC,'YYYY-MM-DD')
FROM    RPR.HMCP h
JOIN    RPR.CCPM c ON c.CCPMNMOP = h.HMCPNMOP
WHERE   h.HMCPFDEC IS NOT NULL AND c.CCPMFDEC IS NOT NULL
AND     REGEXP_LIKE(h.HMCPFDEC, '^\d{4}-\d{2}-\d{2}$')
AND     c.CCPMFDEC - TO_DATE(h.HMCPFDEC,'YYYY-MM-DD') = 1
ORDER BY 1, 2;


-- =============================================================================
-- 3. HM44 / HMJB — fechaJubilacion — SIN FUENTE INDEPENDIENTE
-- =============================================================================
-- El código las llena parseando un string de otra fuente (no un LocalDate ya sano) —
-- hereda el riesgo si esa fuente alguna vez pasó por la pantalla del defecto. No se
-- localizó en este barrido cuál es esa fuente exacta: por eso el diagnóstico aquí es
-- solo de formato y plausibilidad, no una comparación con un valor de verdad.

-- 3.1 Formato real de ambas columnas.
SELECT  'HM44' AS TABLA,
        CASE WHEN h.HM44FCJB IS NULL THEN '(nulo)'
             WHEN REGEXP_LIKE(h.HM44FCJB, '^\d{4}-\d{2}-\d{2}$') THEN 'YYYY-MM-DD'
             WHEN REGEXP_LIKE(h.HM44FCJB, '^\d{2}/\d{2}/\d{4}$') THEN 'DD/MM/YYYY'
             ELSE 'OTRO' END AS FORMATO,
        COUNT(*) AS FILAS, MIN(h.HM44FCJB) AS EJEMPLO_MIN, MAX(h.HM44FCJB) AS EJEMPLO_MAX
FROM    RPR.HM44 h
GROUP BY CASE WHEN h.HM44FCJB IS NULL THEN '(nulo)'
              WHEN REGEXP_LIKE(h.HM44FCJB, '^\d{4}-\d{2}-\d{2}$') THEN 'YYYY-MM-DD'
              WHEN REGEXP_LIKE(h.HM44FCJB, '^\d{2}/\d{2}/\d{4}$') THEN 'DD/MM/YYYY'
              ELSE 'OTRO' END
UNION ALL
SELECT  'HMJB' AS TABLA,
        CASE WHEN j.HMJBFCJB IS NULL THEN '(nulo)'
             WHEN REGEXP_LIKE(j.HMJBFCJB, '^\d{4}-\d{2}-\d{2}$') THEN 'YYYY-MM-DD'
             WHEN REGEXP_LIKE(j.HMJBFCJB, '^\d{2}/\d{2}/\d{4}$') THEN 'DD/MM/YYYY'
             ELSE 'OTRO' END AS FORMATO,
        COUNT(*) AS FILAS, MIN(j.HMJBFCJB) AS EJEMPLO_MIN, MAX(j.HMJBFCJB) AS EJEMPLO_MAX
FROM    RPR.HMJB j
GROUP BY CASE WHEN j.HMJBFCJB IS NULL THEN '(nulo)'
              WHEN REGEXP_LIKE(j.HMJBFCJB, '^\d{4}-\d{2}-\d{2}$') THEN 'YYYY-MM-DD'
              WHEN REGEXP_LIKE(j.HMJBFCJB, '^\d{2}/\d{2}/\d{4}$') THEN 'DD/MM/YYYY'
              ELSE 'OTRO' END
ORDER BY 1, 2;

-- 3.2 HM44 vs HMJB para el MISMO identificador (HM44IDJB = HMJBIDJB: las dos tablas son
--     estructuralmente espejo, PK identificacion con el mismo significado — HM44 para el
--     reporte G44, HMJB para CJBM). Si comparten identificacion deberían decir lo mismo:
--     son dos snapshots del mismo dato de jubilación. Esperado si todo está sano: 0 filas.
SELECT  h.HM44IDJB AS IDENTIFICACION, h.HM44FCJB AS FECHA_HM44, j.HMJBFCJB AS FECHA_HMJB,
        CASE WHEN REGEXP_LIKE(h.HM44FCJB,'^\d{4}-\d{2}-\d{2}$')
                  AND REGEXP_LIKE(j.HMJBFCJB,'^\d{4}-\d{2}-\d{2}$')
             THEN TO_DATE(h.HM44FCJB,'YYYY-MM-DD') - TO_DATE(j.HMJBFCJB,'YYYY-MM-DD') END AS DIAS
FROM    RPR.HM44 h
JOIN    RPR.HMJB j ON j.HMJBIDJB = h.HM44IDJB
WHERE   h.HM44FCJB IS NOT NULL AND j.HMJBFCJB IS NOT NULL
AND     h.HM44FCJB <> j.HMJBFCJB
FETCH FIRST 200 ROWS ONLY;


-- =============================================================================
-- 4. HM40 Y HM50 — ¿SIQUIERA ESTÁN EN USO? (escritor no localizado en este barrido)
-- =============================================================================

-- 4.1 HM40: formato + última vez que se ve actividad (no hay columna de fecha de registro
--     propia; se usa la única PK-like disponible para saber si la tabla se sigue llenando).
SELECT  COUNT(*) AS FILAS_TOTALES,
        SUM(CASE WHEN h.HM40FCRS IS NOT NULL THEN 1 ELSE 0 END) AS CON_FECHA_RESOLUCION,
        SUM(CASE WHEN h.HM40FCTR IS NOT NULL THEN 1 ELSE 0 END) AS CON_FECHA_TRASPASO,
        SUM(CASE WHEN h.HM40FRCE IS NOT NULL THEN 1 ELSE 0 END) AS CON_FECHA_RESOL_CAMBIO_ESTATUTO
FROM    RPR.HM40 h;

SELECT  'HM40FCRS' AS COLUMNA,
        CASE WHEN h.HM40FCRS IS NULL THEN '(nulo)'
             WHEN REGEXP_LIKE(h.HM40FCRS, '^\d{4}-\d{2}-\d{2}$') THEN 'YYYY-MM-DD'
             ELSE 'OTRO' END AS FORMATO, COUNT(*) AS FILAS
FROM    RPR.HM40 h
GROUP BY CASE WHEN h.HM40FCRS IS NULL THEN '(nulo)'
              WHEN REGEXP_LIKE(h.HM40FCRS, '^\d{4}-\d{2}-\d{2}$') THEN 'YYYY-MM-DD'
              ELSE 'OTRO' END
UNION ALL
SELECT  'HM40FCTR',
        CASE WHEN h.HM40FCTR IS NULL THEN '(nulo)'
             WHEN REGEXP_LIKE(h.HM40FCTR, '^\d{4}-\d{2}-\d{2}$') THEN 'YYYY-MM-DD'
             ELSE 'OTRO' END, COUNT(*)
FROM    RPR.HM40 h
GROUP BY CASE WHEN h.HM40FCTR IS NULL THEN '(nulo)'
              WHEN REGEXP_LIKE(h.HM40FCTR, '^\d{4}-\d{2}-\d{2}$') THEN 'YYYY-MM-DD'
              ELSE 'OTRO' END
UNION ALL
SELECT  'HM40FRCE',
        CASE WHEN h.HM40FRCE IS NULL THEN '(nulo)'
             WHEN REGEXP_LIKE(h.HM40FRCE, '^\d{4}-\d{2}-\d{2}$') THEN 'YYYY-MM-DD'
             ELSE 'OTRO' END, COUNT(*)
FROM    RPR.HM40 h
GROUP BY CASE WHEN h.HM40FRCE IS NULL THEN '(nulo)'
              WHEN REGEXP_LIKE(h.HM40FRCE, '^\d{4}-\d{2}-\d{2}$') THEN 'YYYY-MM-DD'
              ELSE 'OTRO' END
ORDER BY 1, 2;

-- 4.2 HM50 (fechaEliminacionGarante): formato + cuántas filas la tienen llena. Si la
--     mayoría está NULL, el riesgo actual es bajo aunque el mecanismo no esté ubicado.
SELECT  CASE WHEN h.HM50FDEG IS NULL THEN '(nulo)'
             WHEN REGEXP_LIKE(h.HM50FDEG, '^\d{4}-\d{2}-\d{2}$') THEN 'YYYY-MM-DD'
             ELSE 'OTRO' END AS FORMATO,
        COUNT(*) AS FILAS
FROM    RPR.HM50 h
GROUP BY CASE WHEN h.HM50FDEG IS NULL THEN '(nulo)'
              WHEN REGEXP_LIKE(h.HM50FDEG, '^\d{4}-\d{2}-\d{2}$') THEN 'YYYY-MM-DD'
              ELSE 'OTRO' END
ORDER BY 2 DESC;


-- =============================================================================
-- 5. HM51 — CONFIRMAR QUE SIGUE SIN USARSE (sin riesgo actual, según el código trazado)
-- =============================================================================
-- Esperado: TOTAL_FILAS = CON_AVALUO_NULL = CON_CONTABILIZACION_NULL, o la tabla vacía.
-- Si aparece cualquier fila con estas columnas llenas, el código que las llena cambió
-- desde el trazado de este script y hay que volver a ubicar la fuente antes de asumir
-- que siguen seguras.
SELECT  COUNT(*)                                                 AS TOTAL_FILAS,
        SUM(CASE WHEN h.HM51FDAV IS NULL THEN 1 ELSE 0 END)     AS CON_AVALUO_NULL,
        SUM(CASE WHEN h.HM51FDLC IS NULL THEN 1 ELSE 0 END)     AS CON_CONTABILIZACION_NULL
FROM    RPR.HM51 h;


-- =============================================================================
-- 6. RESUMEN EJECUTIVO
-- =============================================================================
-- Una fila por columna auditada con datos, con el conteo de filas con la firma exacta del
-- defecto (diferencia de 1 día contra la fuente independiente) donde hay fuente, o NULL en
-- esa columna donde no la hay (que no significa "sin riesgo", significa "no medible con lo
-- que hay hoy").
SELECT  'CRD.ENTD.ENTDFCIN'  AS COLUMNA, 'sin fuente independiente' AS FUENTE_COMPARADA,
        NULL AS FILAS_CON_FIRMA_EXACTA, '(ver bloque 1: heurística débil, no concluyente)' AS NOTA
FROM DUAL
UNION ALL
SELECT  'RPR.HM48.HM48FDEC', 'RPR.CG48.CG48FDEC (LocalDate)',
        (SELECT COUNT(*) FROM RPR.HM48 h JOIN RPR.CG48 g ON g.CG48NMOP = h.HM48NMOP
         WHERE h.HM48FDEC IS NOT NULL AND g.CG48FDEC IS NOT NULL
         AND REGEXP_LIKE(h.HM48FDEC,'^\d{4}-\d{2}-\d{2}$')
         AND g.CG48FDEC - TO_DATE(h.HM48FDEC,'YYYY-MM-DD') = 1),
        'diagnóstico fuerte, ver bloque 2'
FROM DUAL
UNION ALL
SELECT  'RPR.HMCP.HMCPFDEC', 'RPR.CCPM.CCPMFDEC (LocalDate)',
        (SELECT COUNT(*) FROM RPR.HMCP h JOIN RPR.CCPM c ON c.CCPMNMOP = h.HMCPNMOP
         WHERE h.HMCPFDEC IS NOT NULL AND c.CCPMFDEC IS NOT NULL
         AND REGEXP_LIKE(h.HMCPFDEC,'^\d{4}-\d{2}-\d{2}$')
         AND c.CCPMFDEC - TO_DATE(h.HMCPFDEC,'YYYY-MM-DD') = 1),
        'diagnóstico fuerte, ver bloque 2'
FROM DUAL
UNION ALL
SELECT  'RPR.HM44.HM44FCJB / RPR.HMJB.HMJBFCJB', 'sin fuente independiente',
        NULL, '(ver bloque 3: solo formato/plausibilidad)'
FROM DUAL
UNION ALL
SELECT  'RPR.HM40 (3 columnas) / RPR.HM50', 'sin fuente independiente, escritor no localizado',
        NULL, '(ver bloque 4)'
FROM DUAL;

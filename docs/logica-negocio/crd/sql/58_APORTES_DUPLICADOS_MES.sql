-- ==================================================================================
-- DETECCION DE APORTES DUPLICADOS EN UN MISMO MES
-- Alcance: fechas de FIN DE MES, usuario de registro SAA_AH
-- Fecha: 2026-08-24
--
-- Objetivo: ubicar participes con mas de un aporte del mismo tipo en la misma fecha
--           de carga, desde el 31/05/2025 en adelante, considerando UNICAMENTE los
--           aportes fechados el ultimo dia del mes (30-jun-2025, 31-jul-2025, ...)
--           y creados por el proceso automatico (APRTUSRG = 'SAA_AH').
--           Solo consulta, no modifica.
--
-- Criterio de duplicado: misma ENTIDAD (participe) + mismo TIPO DE APORTE
--                        + misma FECHA DE TRANSACCION (a nivel de dia),
--                        siendo esa fecha el ultimo dia del mes.
--
-- Por que APRTFCTR y no APRTFCRG:
--   APRTFCTR (fechaTransaccion) es el sello del periodo. La carga Petro lo fija en
--   el ULTIMO DIA DEL MES de afectacion a las 23:59:59, asi que todos los aportes de
--   un mismo mes comparten exactamente ese valor: es la "fecha de carga" del mes.
--   APRTFCRG (fechaRegistro) es LocalDateTime.now() del instante de ejecucion; nunca
--   coincide entre dos filas, ni siquiera dentro de la misma carga. No sirve de clave.
--
-- Quien escribe 'SAA_AH':
--   Solo dos rutas, ambas en CargaArchivoPetroServiceImpl:
--     linea 3666  crearNuevoAporte              (aporte del mes de carga)
--     linea 3794  crearAporteExcedenteMesSiguiente
--   Todo lo demas que crea aportes graba el usuario real de la sesion
--   (AporteServiceImpl, DevolucionAporteServiceImpl, ProcesoPagoPrestamoServiceImpl),
--   asi que este filtro deja solo lo generado por el proceso automatico.
--   Ninguna ruta REESCRIBE el usuario de un aporte ya existente: la devolucion
--   inserta una fila NEGATIVA nueva, no toca la original. Por eso el filtro no
--   esconde aportes de carga que despues hayan sido devueltos.
--
-- ==================================================================================
-- OJO AL LEER EL RESULTADO: hay un par LEGITIMO que cae en este criterio
-- ==================================================================================
--   Una sola corrida de la carga puede crear DOS aportes del mismo participe, mismo
--   tipo y MISMA FECHA, ambos con SAA_AH y el mismo APRTIDAS:
--
--     1. crearNuevoAporte                 -> aporte del mes de carga
--        estado PENDIENTE (1), valorPagado = 0
--        glosa "Aporte <tipo> - Mes m/a - CargaArchivo: n"
--
--     2. crearAporteExcedenteMesSiguiente -> aporte del MES SIGUIENTE, pero fechado
--        deliberadamente con el ultimo dia del MES DE CARGA (ver comentario del
--        metodo: "usar el ultimo dia del mes del periodo de CARGA, no del mes
--        siguiente"), estado PARCIAL (6), valorPagado > 0
--        glosa "Abono al aporte <tipo> del mes m/a generado con CargaArchivo n"
--
--   Son dos meses distintos compartiendo fecha, no un duplicado. La columna ORIGEN
--   los separa, y la variante (b) del pie los excluye.
--
--   El duplicado REAL se reconoce por CARGAS_DISTINTAS > 1 en la consulta 2:
--   el mismo mes cargado mas de una vez, con APRTIDAS diferente en cada fila.
-- ==================================================================================


-- ==================================================================================
-- CONSULTA 1 (principal): detalle de cada aporte que forma parte de un duplicado
-- ==================================================================================
SELECT  d.VECES                          AS APORTES_EN_LA_FECHA,
        d.DIA_CARGA,
        d.PERIODO,
        d.NUMERO_IDENTIFICACION,
        d.RAZON_SOCIAL,
        d.TIPO_APORTE,
        d.ORIGEN,
        d.APRTCDGO                       AS ID_APORTE,
        d.VALOR,
        d.VALOR_PAGADO,
        d.SALDO,
        d.ESTADO,
        d.ID_CARGA_ARCHIVO,
        d.FECHA_REGISTRO,
        d.GLOSA
FROM  ( SELECT  a.APRTCDGO,
                TRUNC(a.APRTFCTR)                    AS DIA_CARGA,
                TO_CHAR(a.APRTFCTR, 'YYYY-MM')       AS PERIODO,
                e.ENTDNMID                           AS NUMERO_IDENTIFICACION,
                e.ENTDRZNS                           AS RAZON_SOCIAL,
                t.TPAPNMBR                           AS TIPO_APORTE,
                CASE WHEN a.APRTGLSA LIKE 'Abono al aporte%'
                     THEN 'EXCEDENTE MES SIGUIENTE'
                     ELSE 'APORTE DEL MES'
                END                                  AS ORIGEN,
                a.APRTVLRR                           AS VALOR,
                a.APRTVLPG                           AS VALOR_PAGADO,
                a.APRTSLDO                           AS SALDO,
                CASE a.APRTIDST
                     WHEN 1 THEN 'PENDIENTE'
                     WHEN 2 THEN 'ACTIVA'
                     WHEN 3 THEN 'EMITIDA'
                     WHEN 4 THEN 'PAGADA'
                     WHEN 5 THEN 'EN MORA'
                     WHEN 6 THEN 'PARCIAL'
                     WHEN 7 THEN 'CANCELADA ANTICIPADA'
                     WHEN 8 THEN 'VENCIDA'
                     ELSE TO_CHAR(a.APRTIDST)
                END                                  AS ESTADO,
                a.APRTIDAS                           AS ID_CARGA_ARCHIVO,
                a.APRTFCRG                           AS FECHA_REGISTRO,
                a.APRTGLSA                           AS GLOSA,
                COUNT(*) OVER (PARTITION BY a.ENTDCDGO,
                                            a.TPAPCDGO,
                                            TRUNC(a.APRTFCTR))  AS VECES
        FROM    CRD.APRT a
        JOIN    CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
        JOIN    CRD.TPAP t ON t.TPAPCDGO = a.TPAPCDGO
        WHERE   a.APRTFCTR >= DATE '2025-05-31'
        AND     TRUNC(a.APRTFCTR) = LAST_DAY(TRUNC(a.APRTFCTR))
        AND     a.APRTUSRG = 'SAA_AH' ) d
WHERE   d.VECES > 1
ORDER BY d.DIA_CARGA,
         d.NUMERO_IDENTIFICACION,
         d.TIPO_APORTE,
         d.APRTCDGO;


-- ==================================================================================
-- CONSULTA 2 (resumen): un renglon por grupo duplicado
-- CARGAS_DISTINTAS > 1  -> el mes se cargo mas de una vez  = DUPLICADO REAL
-- CARGAS_DISTINTAS = 1  -> revisar ORIGEN: suele ser el par aporte + excedente
-- ==================================================================================
SELECT  TRUNC(a.APRTFCTR)                  AS DIA_CARGA,
        TO_CHAR(a.APRTFCTR, 'YYYY-MM')     AS PERIODO,
        e.ENTDNMID                         AS NUMERO_IDENTIFICACION,
        e.ENTDRZNS                         AS RAZON_SOCIAL,
        t.TPAPNMBR                         AS TIPO_APORTE,
        COUNT(*)                           AS APORTES,
        COUNT(DISTINCT a.APRTIDAS)         AS CARGAS_DISTINTAS,
        SUM(CASE WHEN a.APRTGLSA LIKE 'Abono al aporte%' THEN 1 ELSE 0 END)
                                           AS FILAS_EXCEDENTE,
        SUM(a.APRTVLRR)                    AS VALOR_TOTAL,
        SUM(a.APRTSLDO)                    AS SALDO_TOTAL,
        LISTAGG(a.APRTCDGO, ', ')
          WITHIN GROUP (ORDER BY a.APRTCDGO)  AS IDS_APORTE,
        LISTAGG(a.APRTIDAS, ', ')
          WITHIN GROUP (ORDER BY a.APRTCDGO)  AS IDS_CARGA
FROM    CRD.APRT a
JOIN    CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
JOIN    CRD.TPAP t ON t.TPAPCDGO = a.TPAPCDGO
WHERE   a.APRTFCTR >= DATE '2025-05-31'
AND     TRUNC(a.APRTFCTR) = LAST_DAY(TRUNC(a.APRTFCTR))
AND     a.APRTUSRG = 'SAA_AH'
GROUP BY TRUNC(a.APRTFCTR),
         TO_CHAR(a.APRTFCTR, 'YYYY-MM'),
         a.ENTDCDGO,
         e.ENTDNMID,
         e.ENTDRZNS,
         a.TPAPCDGO,
         t.TPAPNMBR
HAVING  COUNT(*) > 1
ORDER BY CARGAS_DISTINTAS DESC, DIA_CARGA, NUMERO_IDENTIFICACION, TIPO_APORTE;


-- ==================================================================================
-- CONSULTA 3 (conteo por mes): cuantos grupos duplicados hay en cada periodo,
-- separando los que vienen de mas de una carga (duplicado real) de los demas.
-- ==================================================================================
SELECT  g.PERIODO,
        g.DIA_CARGA,
        COUNT(*)                                                   AS GRUPOS_DUPLICADOS,
        SUM(CASE WHEN g.CARGAS > 1 THEN 1 ELSE 0 END)              AS GRUPOS_MULTICARGA,
        SUM(g.APORTES)                                             AS FILAS_INVOLUCRADAS,
        SUM(g.APORTES - 1)                                         AS FILAS_SOBRANTES,
        SUM(g.VALOR_TOTAL)                                         AS VALOR_TOTAL
FROM  ( SELECT  TO_CHAR(a.APRTFCTR, 'YYYY-MM') AS PERIODO,
                TRUNC(a.APRTFCTR)              AS DIA_CARGA,
                COUNT(*)                       AS APORTES,
                COUNT(DISTINCT a.APRTIDAS)     AS CARGAS,
                SUM(a.APRTVLRR)                AS VALOR_TOTAL
        FROM    CRD.APRT a
        WHERE   a.APRTFCTR >= DATE '2025-05-31'
        AND     TRUNC(a.APRTFCTR) = LAST_DAY(TRUNC(a.APRTFCTR))
        AND     a.APRTUSRG = 'SAA_AH'
        GROUP BY TO_CHAR(a.APRTFCTR, 'YYYY-MM'),
                 TRUNC(a.APRTFCTR),
                 a.ENTDCDGO,
                 a.TPAPCDGO
        HAVING  COUNT(*) > 1 ) g
GROUP BY g.PERIODO, g.DIA_CARGA
ORDER BY g.PERIODO;


-- ==================================================================================
-- CONSULTA 4 (control): que fechas de fin de mes existen y cuantos aportes SAA_AH
-- tiene cada una. Correr ANTES que las demas: confirma que el filtro esta agarrando
-- los meses esperados y que CARGAS_DISTINTAS delata los meses recargados.
-- ==================================================================================
SELECT  TRUNC(a.APRTFCTR)          AS DIA_CARGA,
        COUNT(*)                   AS TOTAL_APORTES,
        COUNT(DISTINCT a.ENTDCDGO) AS PARTICIPES,
        COUNT(DISTINCT a.APRTIDAS) AS CARGAS_DISTINTAS,
        SUM(a.APRTVLRR)            AS VALOR_TOTAL
FROM    CRD.APRT a
WHERE   a.APRTFCTR >= DATE '2025-05-31'
AND     TRUNC(a.APRTFCTR) = LAST_DAY(TRUNC(a.APRTFCTR))
AND     a.APRTUSRG = 'SAA_AH'
GROUP BY TRUNC(a.APRTFCTR)
ORDER BY DIA_CARGA;


-- ==================================================================================
-- VARIANTES (ajustar segun lo que se quiera medir)
--
-- a) Excluir mayo/2025 y arrancar en junio:
--       cambiar     a.APRTFCTR >= DATE '2025-05-31'
--       por         a.APRTFCTR >= DATE '2025-06-01'
--    (31-may-2025 tambien es fin de mes, por eso hoy entra en el resultado)
--
-- b) Dejar fuera el par legitimo aporte + excedente y quedarse solo con lo que
--    huele a mes recargado:
--       agregar     AND a.APRTGLSA NOT LIKE 'Abono al aporte%'
--
-- c) Quedarse SOLO con los grupos que vienen de mas de una carga, que es el
--    duplicado duro: en la consulta 2 cambiar el HAVING por
--       HAVING COUNT(*) > 1 AND COUNT(DISTINCT a.APRTIDAS) > 1
--
-- d) Un solo tipo de aporte:
--       agregar     AND t.TPAPNMBR = 'APORTE PERSONAL'
-- ==================================================================================

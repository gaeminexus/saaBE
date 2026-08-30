-- =============================================================================
-- 62 - CORRECCION DEL VALOR DE APORTES ESCRITOS POR LA CARGA PETRO (fila PARCIAL)
-- =============================================================================
-- Acompaña a la Fase 1 de docs/logica-negocio/crd/PLAN-APORTES-DEVENGO-CONTRATOS.md
-- (decisión D1). Leer ese documento y docs/logica-negocio/petro/REGLAS-CARGA-PETRO.md
-- §3.6 antes de correr esto.
--
-- ⚠ DEFECTO YA CAUSÓ DAÑO EN PRODUCCIÓN, CORREGIDO EN ESTA VERSIÓN (2026-08-27) — LEER
-- ANTES DE VOLVER A CORRER NADA DE ESTE SCRIPT:
--   La versión anterior hacía `SET a.APRTVLRR = NVL(a.APRTVLPG, 0)`. Toda fila creada por
--   la carga que NUNCA registró un pago (APRTVLPG en NULL, no en 0 — no hay ningún
--   APRTVLPG) quedó en valor 0: puso en CERO 2.635 aportes de junio 2025 por un total de
--   $160.350,81. La intención de D1 era "valor = lo recibido", no "valor = 0 cuando no
--   sabemos cuánto se recibió": son cosas distintas. Un NVL sobre una columna que puede ser
--   NULL convirtió "no sé" en "cero" y destruyó datos. Es la clase de error que se repite
--   si no queda dicho: por eso el UPDATE (bloque 3) ahora exige explícitamente
--   `APRTVLPG IS NOT NULL AND APRTVLPG > 0`, y las filas con `APRTVLPG` NULL o 0 se listan
--   aparte (bloque 0) para que el usuario decida qué hacer con ellas — no se les aplica
--   ninguna regla automática. El usuario ya tiene un script de restauración para el daño ya
--   causado en producción; este archivo sólo corrige la fuente para que no se repita.
--
-- QUE CORRIGE Y QUE NO:
--   Corrige la inflacion de la fila PARCIAL: la carga escribia valor = esperado
--   (HistorialSueldo) y valorPagado = lo realmente descontado, dejando saldo > 0.
--   Como el saldo del participe es SUM(APRTVLRR), esa diferencia (valor - valorPagado)
--   quedaba contada de mas en el saldo Y en el "registrado" que lee el cierre de
--   cartera (CierreCarteraDaoServiceImpl.selectAportesRegistrados, tipos 9/11).
--
--   NO corrige la carga procesada DOS VECES (dos juegos de filas completos, cada uno
--   con valor = valorPagado). Esa es la inflacion "carga duplicada" de
--   docs/logica-negocio/crd/PLAN-APORTES-DEVENGO-CONTRATOS.md §7.1, y sigue esperando
--   los resultados de 61_ANALISIS_APORTES_DUPLICADOS_PETRO.sql (A0, A2, A6). Este script
--   no la toca: un UPDATE valor = valorPagado no distingue una fila duplicada de una
--   fila sana, porque en la duplicada valor YA es igual a valorPagado.
--
--   NO toca las filas con APRTVLPG NULL o 0 — ver bloque 0. Sin un valorPagado real no hay
--   forma de saber cuánto se recibió: no es que el UPDATE "no aplique", es que no hay dato.
--
-- ALCANCE: filas escritas por la carga (APRTUSRG = 'SAA_AH' o glosa que empiece con
-- 'Aporte %CargaArchivo: %', que cubre tambien las filas de la version del generador
-- que no llenaba APRTUSRG — ver REGLAS-CARGA-PETRO.md / 61_ANALISIS...sql, clasificacion
-- V1/V3) con valor > 0, valor <> valorPagado, Y valorPagado NO NULO y > 0.
--
-- NO SE EJECUTA AUTOMATICAMENTE. El usuario corre cada bloque a mano, en un cliente SQL
-- (plugin JDBC de VS Code), revisando el resultado del bloque anterior antes de seguir.
-- SQL puro: sin SET/DEFINE/WHENEVER.
--
-- INDICE
--   0. Filas EXCLUIDAS          valorPagado NULL o 0 — solo se listan, NO se tocan
--   1. SELECT de control (antes)      cuantas filas y cuanto monto cambia, por periodo
--   2. Respaldo                       CRD.BKP_APRT_VALOR_<fecha> con las filas afectadas
--   3. UPDATE                         valor = valorPagado, saldo = 0, estado = 4
--   4. SELECT de verificacion (despues)
--   5. Impacto contable               "registrado" del cierre de cartera, antes/despues,
--                                      por mes, tipos 9/11 (misma consulta de
--                                      selectAportesRegistrados)
--   6. Reverso                        UPDATE de vuelta desde el respaldo, si hace falta
-- =============================================================================


-- =============================================================================
-- 0. FILAS EXCLUIDAS — valorPagado (APRTVLPG) NULL o 0. SOLO SE LISTAN, NO SE TOCAN.
-- =============================================================================
-- Estas son las filas que la versión anterior del script ponía en valor = 0 por el NVL.
-- Necesitan una decisión del usuario (¿se sabe por otra fuente cuánto se recibió realmente?
-- ¿son filas fantasma que no debieron crearse? ¿se dejan tal cual?), no una regla automática.
-- El bloque 3 de este script las deja fuera a propósito.
-- =============================================================================

-- 0.1 Resumen por periodo de las filas excluidas.
SELECT  TO_CHAR(TRUNC(a.APRTFCTR, 'MM'), 'YYYY-MM')  AS PERIODO,
        a.TPAPCDGO                                    AS ID_TIPO_APORTE,
        tp.TPAPNMBR                                    AS TIPO_APORTE,
        SUM(CASE WHEN a.APRTVLPG IS NULL THEN 1 ELSE 0 END) AS FILAS_VALORPAGADO_NULL,
        SUM(CASE WHEN a.APRTVLPG = 0     THEN 1 ELSE 0 END) AS FILAS_VALORPAGADO_CERO,
        COUNT(*)                                       AS TOTAL_FILAS_EXCLUIDAS,
        SUM(a.APRTVLRR)                                AS VALOR_ACTUAL_SIN_TOCAR
FROM    CRD.APRT a
JOIN    CRD.TPAP tp ON tp.TPAPCDGO = a.TPAPCDGO
WHERE   (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %')
AND     a.APRTVLRR > 0
AND     a.APRTVLRR <> NVL(a.APRTVLPG, 0)
AND     (a.APRTVLPG IS NULL OR a.APRTVLPG = 0)
GROUP BY TRUNC(a.APRTFCTR, 'MM'), a.TPAPCDGO, tp.TPAPNMBR
ORDER BY 1, 2;

-- 0.2 Totales generales de lo excluido (para citar en el reporte al usuario).
SELECT  SUM(CASE WHEN a.APRTVLPG IS NULL THEN 1 ELSE 0 END) AS FILAS_VALORPAGADO_NULL,
        SUM(CASE WHEN a.APRTVLPG = 0     THEN 1 ELSE 0 END) AS FILAS_VALORPAGADO_CERO,
        COUNT(*)                                       AS TOTAL_FILAS_EXCLUIDAS,
        SUM(a.APRTVLRR)                                AS VALOR_ACTUAL_SIN_TOCAR,
        COUNT(DISTINCT a.ENTDCDGO)                     AS PARTICIPES_AFECTADOS
FROM    CRD.APRT a
WHERE   (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %')
AND     a.APRTVLRR > 0
AND     a.APRTVLRR <> NVL(a.APRTVLPG, 0)
AND     (a.APRTVLPG IS NULL OR a.APRTVLPG = 0);

-- 0.3 Detalle fila a fila de lo excluido, para revisión manual.
SELECT  a.APRTCDGO, a.ENTDCDGO, e.ENTDNMID AS NUMERO_IDENTIFICACION, e.ENTDRZNS AS RAZON_SOCIAL,
        a.TPAPCDGO, a.APRTVLRR AS VALOR_ACTUAL, a.APRTVLPG AS VALOR_PAGADO,
        a.APRTIDST AS ESTADO, a.APRTFCTR AS FECHA_TRANSACCION, a.APRTGLSA AS GLOSA
FROM    CRD.APRT a
JOIN    CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
WHERE   (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %')
AND     a.APRTVLRR > 0
AND     a.APRTVLRR <> NVL(a.APRTVLPG, 0)
AND     (a.APRTVLPG IS NULL OR a.APRTVLPG = 0)
ORDER BY a.APRTFCTR, e.ENTDNMID;


-- =============================================================================
-- 1. SELECT DE CONTROL (ANTES) — cuantas filas y cuanto monto cambia, por periodo
-- =============================================================================
-- PERIODO es el mes de CAJA (APRTFCTR), que es el que agrupa hoy el "registrado"
-- contable — la fecha de devengo (APRTPRDV) todavia no se llena en esta fase.
-- DIFERENCIA = SUM(valor - valorPagado): lo que baja el "registrado" de ese mes.
-- Solo filas con valorPagado real (NOT NULL y > 0) — ver bloque 0 para lo excluido.
-- =============================================================================
SELECT  TO_CHAR(TRUNC(a.APRTFCTR, 'MM'), 'YYYY-MM')  AS PERIODO,
        a.TPAPCDGO                                    AS ID_TIPO_APORTE,
        tp.TPAPNMBR                                    AS TIPO_APORTE,
        COUNT(*)                                       AS FILAS_A_CORREGIR,
        SUM(a.APRTVLRR)                                AS VALOR_ANTES,
        SUM(a.APRTVLPG)                                AS VALOR_PAGADO,
        SUM(a.APRTVLRR - a.APRTVLPG)                   AS DIFERENCIA_A_RESTAR
FROM    CRD.APRT a
JOIN    CRD.TPAP tp ON tp.TPAPCDGO = a.TPAPCDGO
WHERE   (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %')
AND     a.APRTVLRR > 0
AND     a.APRTVLPG IS NOT NULL AND a.APRTVLPG > 0
AND     a.APRTVLRR <> a.APRTVLPG
GROUP BY TRUNC(a.APRTFCTR, 'MM'), a.TPAPCDGO, tp.TPAPNMBR
ORDER BY 1, 2;

-- 1b. Totales generales (una sola fila) — la cifra a citar en el reporte al usuario.
SELECT  COUNT(*)                                     AS FILAS_A_CORREGIR,
        SUM(a.APRTVLRR)                              AS VALOR_ANTES,
        SUM(a.APRTVLPG)                              AS VALOR_PAGADO,
        SUM(a.APRTVLRR - a.APRTVLPG)                 AS DIFERENCIA_A_RESTAR,
        COUNT(DISTINCT a.ENTDCDGO)                   AS PARTICIPES_AFECTADOS,
        SUM(CASE WHEN a.APRTIDST <> 4 THEN 1 ELSE 0 END) AS FILAS_CON_ESTADO_DISTINTO_DE_PAGADA
FROM    CRD.APRT a
WHERE   (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %')
AND     a.APRTVLRR > 0
AND     a.APRTVLPG IS NOT NULL AND a.APRTVLPG > 0
AND     a.APRTVLRR <> a.APRTVLPG;


-- =============================================================================
-- 2. RESPALDO — CREATE TABLE con las filas afectadas, ANTES de tocarlas
-- =============================================================================
-- Cambiar <fecha> por la fecha de ejecucion real (ej. BKP_APRT_VALOR_20260827).
-- Guarda la fila COMPLETA (SELECT *) para poder reversar cualquier columna, no solo
-- las tres que toca el UPDATE. Mismo filtro EXACTO que el bloque 1 (incluye el filtro de
-- valorPagado real: ver el defecto corregido en el encabezado).
-- =============================================================================
CREATE TABLE CRD.BKP_APRT_VALOR_20260827 AS
SELECT  a.*
FROM    CRD.APRT a
WHERE   (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %')
AND     a.APRTVLRR > 0
AND     a.APRTVLPG IS NOT NULL AND a.APRTVLPG > 0
AND     a.APRTVLRR <> a.APRTVLPG;

-- Verificar que el respaldo tiene EXACTAMENTE las mismas filas que el 1b.
SELECT COUNT(*) AS FILAS_RESPALDADAS FROM CRD.BKP_APRT_VALOR_20260827;


-- =============================================================================
-- 3. UPDATE — valor = valorPagado, saldo = 0, estado = 4 (PAGADA)
-- =============================================================================
-- Alinea la fila con el modelo D1: valor pasa a ser lo efectivamente recibido.
-- Mismo filtro EXACTO que el respaldo del bloque 2: si se corre este bloque sin
-- haber corrido el 2 sobre el mismo filtro, no hay como reversar.
--
-- ⚠ APRTVLPG IS NOT NULL AND APRTVLPG > 0 es OBLIGATORIO — sin este filtro, un NVL
-- convierte "no sé cuánto se recibió" (NULL) en "se recibió cero" y pone en 0 filas que
-- nunca debieron tocarse (ver el defecto ya corregido, en el encabezado de este archivo).
-- =============================================================================
UPDATE  CRD.APRT a
SET     a.APRTVLRR = a.APRTVLPG,
        a.APRTSLDO = 0,
        a.APRTIDST = 4
WHERE   (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %')
AND     a.APRTVLRR > 0
AND     a.APRTVLPG IS NOT NULL AND a.APRTVLPG > 0
AND     a.APRTVLRR <> a.APRTVLPG;

COMMIT;


-- =============================================================================
-- 4. SELECT DE VERIFICACION (DESPUES) — esperado: igual al total del bloque 0.2
--    (lo único que debe seguir "descuadrado" es lo excluido a propósito)
-- =============================================================================
SELECT  COUNT(*) AS FILAS_TODAVIA_DESCUADRADAS
FROM    CRD.APRT a
WHERE   (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %')
AND     a.APRTVLRR > 0
AND     a.APRTVLRR <> NVL(a.APRTVLPG, 0);

-- Debe coincidir con el TOTAL_FILAS_EXCLUIDAS del bloque 0.2 — son las mismas filas,
-- todavía sin tocar a propósito.
SELECT  COUNT(*) AS FILAS_EXCLUIDAS_SIGUEN_IGUAL
FROM    CRD.APRT a
WHERE   (a.APRTUSRG = 'SAA_AH' OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %')
AND     a.APRTVLRR > 0
AND     (a.APRTVLPG IS NULL OR a.APRTVLPG = 0);

-- Las filas corregidas deben quedar todas con saldo 0 y estado 4.
SELECT  COUNT(*) AS FILAS_CORREGIDAS_OK
FROM    CRD.APRT a
JOIN    CRD.BKP_APRT_VALOR_20260827 b ON b.APRTCDGO = a.APRTCDGO
WHERE   a.APRTVLRR = b.APRTVLPG
AND     NVL(a.APRTSLDO, 0) = 0
AND     a.APRTIDST = 4;

SELECT  COUNT(*) AS TOTAL_RESPALDADAS FROM CRD.BKP_APRT_VALOR_20260827;
-- FILAS_CORREGIDAS_OK debe ser IGUAL a TOTAL_RESPALDADAS.


-- =============================================================================
-- 5. IMPACTO CONTABLE — "registrado" del cierre de cartera, ANTES vs DESPUES
-- =============================================================================
-- Misma consulta que CierreCarteraDaoServiceImpl.selectAportesRegistrados (tipos 9/11,
-- APRTVLRR > 0, agrupado por mes de APRTFCTR), para cuantificar cuanto baja el
-- "registrado" de cada mes de cierre ya corrido.
--
-- DESPUES de correr el bloque 3, esta consulta ya muestra el valor NUEVO. Para el
-- valor ANTES, correr esta misma consulta ANTES del bloque 3 (o reconstruirlo con
-- SUM(BKP.APRTVLRR) sobre el respaldo, que es lo que hace la segunda consulta).
-- =============================================================================

-- 5a. DESPUES de la correccion (correr tras el commit del bloque 3).
SELECT  TO_CHAR(TRUNC(a.APRTFCTR, 'MM'), 'YYYY-MM') AS PERIODO,
        NVL(SUM(CASE WHEN a.TPAPCDGO = 9  THEN a.APRTVLRR ELSE 0 END), 0) AS JUBILACION_DESPUES,
        NVL(SUM(CASE WHEN a.TPAPCDGO = 11 THEN a.APRTVLRR ELSE 0 END), 0) AS CESANTIA_DESPUES
FROM    CRD.APRT a
WHERE   a.TPAPCDGO IN (9, 11)
AND     a.APRTVLRR > 0
GROUP BY TRUNC(a.APRTFCTR, 'MM')
ORDER BY 1;

-- 5b. ANTES de la correccion, reconstruido desde el respaldo (valores originales) más
--     lo que NO cambió (filas fuera del alcance del bloque 3, que ya estaban bien).
--     Compara contra 5a mes a mes: la diferencia es exactamente lo que este script
--     bajó del "registrado".
WITH SIN_TOCAR AS (
        SELECT  a.APRTFCTR, a.TPAPCDGO, a.APRTVLRR
        FROM    CRD.APRT a
        WHERE   a.TPAPCDGO IN (9, 11)
        AND     a.APRTVLRR > 0
        AND     a.APRTCDGO NOT IN (SELECT b.APRTCDGO FROM CRD.BKP_APRT_VALOR_20260827 b)
),
ORIGINAL_CORREGIDAS AS (
        SELECT  b.APRTFCTR, b.TPAPCDGO, b.APRTVLRR
        FROM    CRD.BKP_APRT_VALOR_20260827 b
        WHERE   b.TPAPCDGO IN (9, 11)
        AND     b.APRTVLRR > 0
),
TODO AS (
        SELECT * FROM SIN_TOCAR
        UNION ALL
        SELECT * FROM ORIGINAL_CORREGIDAS
)
SELECT  TO_CHAR(TRUNC(t.APRTFCTR, 'MM'), 'YYYY-MM') AS PERIODO,
        NVL(SUM(CASE WHEN t.TPAPCDGO = 9  THEN t.APRTVLRR ELSE 0 END), 0) AS JUBILACION_ANTES,
        NVL(SUM(CASE WHEN t.TPAPCDGO = 11 THEN t.APRTVLRR ELSE 0 END), 0) AS CESANTIA_ANTES
FROM    TODO t
GROUP BY TRUNC(t.APRTFCTR, 'MM')
ORDER BY 1;


-- =============================================================================
-- 6. REVERSO — UPDATE de vuelta desde el respaldo
-- =============================================================================
-- Solo si hace falta deshacer la correccion. Restaura las TRES columnas que tocó
-- el bloque 3 desde el respaldo del bloque 2. No borra CRD.BKP_APRT_VALOR_20260827:
-- el usuario la elimina a mano cuando ya no la necesite.
-- =============================================================================
-- ⛔ COMENTADO A PROPOSITO. Corre SOLO si hay que deshacer la correccion.
--    Si el script se ejecuta de corrido con esto activo, revierte en silencio todo lo
--    que acaba de hacer y el resultado parece correcto.
--
-- UPDATE  CRD.APRT a
-- SET     (a.APRTVLRR, a.APRTSLDO, a.APRTIDST) = (
--             SELECT  b.APRTVLRR, b.APRTSLDO, b.APRTIDST
--             FROM    CRD.BKP_APRT_VALOR_20260827 b
--             WHERE   b.APRTCDGO = a.APRTCDGO
--         )
-- WHERE   a.APRTCDGO IN (SELECT b.APRTCDGO FROM CRD.BKP_APRT_VALOR_20260827 b);

COMMIT;

-- Verificacion del reverso: esperado 0 filas (todo volvió a coincidir con el respaldo).
SELECT  COUNT(*) AS FILAS_QUE_NO_COINCIDEN_CON_RESPALDO
FROM    CRD.APRT a
JOIN    CRD.BKP_APRT_VALOR_20260827 b ON b.APRTCDGO = a.APRTCDGO
WHERE   NVL(a.APRTVLRR, -1) <> NVL(b.APRTVLRR, -1)
OR      NVL(a.APRTSLDO, -1) <> NVL(b.APRTSLDO, -1)
OR      NVL(a.APRTIDST, -1) <> NVL(b.APRTIDST, -1);

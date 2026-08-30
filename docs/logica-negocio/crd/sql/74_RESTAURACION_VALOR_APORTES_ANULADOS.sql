-- =====================================================================================
-- 74 — RESTAURACION DE LOS APORTES QUE EL SCRIPT 62 PUSO EN CERO
-- =====================================================================================
-- FECHA: 2026-08-27
--
-- QUE PASO
--   La primera version de 62_CORRECCION_VALOR_APORTES_CARGA.sql hacia:
--       SET a.APRTVLRR = NVL(a.APRTVLPG, 0)
--   Toda fila creada por la carga cuyo valorPagado era NULL quedo con valor 0. Un NVL
--   sobre una columna que puede ser NULL convirtio "no se" en "cero" y destruyo datos.
--   En junio de 2025 eso barrio 2.635 filas por $160.350,81.
--
--   Esas filas llevaban ahi desde antes de todo este trabajo: glosa
--   'Aporte jubilacion - CargaArchivo: 352', APRTFCTR = 2025-06-30 00:00:00, sin usuario
--   y sin APRTIDAS. Ningun control de los scripts 61, 65 ni 66 las vio, porque todos
--   filtran por APRTUSRG = 'SAA_AH' o por glosa con ' - Mes m/aaaa'. Por eso el analisis
--   concluyo, equivocadamente, que junio 2025 nunca habia generado aportes.
--
--   El script 66 SI las vio (su guardarrail exige APRTVLRR > 0 y en ese momento todavia
--   valian) y por eso solo inserto 3 filas, $174,08, para tres participes que de verdad
--   no tenian nada. El 66 hizo lo correcto.
--
-- QUE HACE ESTE SCRIPT
--   Devuelve a esas filas su valor original desde CRD.BKP_APRT_VALOR_20260827, y de paso
--   las alinea al modelo nuevo (valorPagado = valor, saldo = 0, estado = 4).
--
-- ⚠ EL BLOQUE 3 ESTA ACOTADO A JUNIO 2025 A PROPOSITO.
--   Para junio sabemos que el dinero SI se recibio: Petro descontó $162.004,30 ese mes y
--   estas filas suman $160.350,81, el 99%. Poner valorPagado = valor es afirmar algo
--   cierto.
--   Para OTROS meses no lo sabemos: una fila con valorPagado NULL puede ser un aporte que
--   nunca se cobro, y restaurarlo inflaria el saldo del participe con dinero que no entro.
--   Si el control 1.2 muestra otros meses afectados, NO los restaures con este script:
--   avisa y se decide aparte.
--
-- ORDEN: este script va ANTES de desplegar el WAR. No hace falta volver a correr el 62,
-- el 63, el 64 ni el 66: los tres primeros quedaron bien y el 66 hizo lo que debia.
-- SQL PURO: sin SET / DEFINE / WHENEVER.
-- =====================================================================================


-- =====================================================================================
-- 1. ALCANCE DEL DAÑO — correr y leer ANTES de tocar nada
-- =====================================================================================

-- 1.1  Junio 2025. Esperado: 2.635 filas, $160.350,81.
SELECT  COUNT(*)                    AS FILAS_EN_CERO,
        COUNT(DISTINCT a.ENTDCDGO)  AS PARTICIPES,
        SUM(b.APRTVLRR)             AS VALOR_A_RESTAURAR,
        SUM(NVL(b.APRTVLPG,0))      AS PAGADO_QUE_TENIAN
FROM    CRD.APRT a
JOIN    CRD.BKP_APRT_VALOR_20260827 b ON b.APRTCDGO = a.APRTCDGO
WHERE   NVL(a.APRTVLRR,0) = 0
AND     b.APRTVLRR > 0
AND     TRUNC(a.APRTFCTR,'MM') = DATE '2025-06-01';

-- 1.2  ⚠ EL CONTROL QUE DECIDE. Todos los meses afectados, no solo junio.
--      Si aparece CUALQUIER mes distinto de 2025-06, PARAR despues del bloque 3 y avisar:
--      para esos meses no sabemos si el dinero se recibio, y restaurarlos a ciegas
--      inflaria el saldo del participe.
SELECT  TO_CHAR(a.APRTFCTR,'YYYY-MM')  AS MES_CAJA,
        NVL(a.APRTUSRG,'(null)')       AS USUARIO,
        a.TPAPCDGO                     AS ID_TIPO,
        COUNT(*)                       AS FILAS_EN_CERO,
        SUM(b.APRTVLRR)                AS VALOR_QUE_TENIAN,
        SUM(NVL(b.APRTVLPG,0))         AS PAGADO_QUE_TENIAN
FROM    CRD.APRT a
JOIN    CRD.BKP_APRT_VALOR_20260827 b ON b.APRTCDGO = a.APRTCDGO
WHERE   NVL(a.APRTVLRR,0) = 0
AND     b.APRTVLRR > 0
GROUP BY TO_CHAR(a.APRTFCTR,'YYYY-MM'), NVL(a.APRTUSRG,'(null)'), a.TPAPCDGO
ORDER BY 1, 2, 3;


-- =====================================================================================
-- 2. RESPALDO DEL ESTADO ACTUAL
-- =====================================================================================
-- Por si esta restauracion tampoco fuera lo correcto. No sustituye a
-- BKP_APRT_VALOR_20260827: ese guarda el estado ANTES del 62, este el estado DESPUES.
CREATE TABLE CRD.BKP_APRT_CERO_20260827 AS
SELECT  a.*
FROM    CRD.APRT a
JOIN    CRD.BKP_APRT_VALOR_20260827 b ON b.APRTCDGO = a.APRTCDGO
WHERE   NVL(a.APRTVLRR,0) = 0
AND     b.APRTVLRR > 0;

SELECT COUNT(*) AS FILAS_RESPALDADAS, SUM(APRTVLRR) AS VALOR_ACTUAL
FROM   CRD.BKP_APRT_CERO_20260827;


-- =====================================================================================
-- 3. LA RESTAURACION — SOLO JUNIO 2025
-- =====================================================================================
-- El filtro de mes es deliberado: ver la advertencia del encabezado. Para otros meses,
-- decidir aparte con el resultado del control 1.2.
UPDATE  CRD.APRT a
SET     a.APRTVLRR = (SELECT b.APRTVLRR FROM CRD.BKP_APRT_VALOR_20260827 b
                      WHERE b.APRTCDGO = a.APRTCDGO),
        a.APRTVLPG = (SELECT b.APRTVLRR FROM CRD.BKP_APRT_VALOR_20260827 b
                      WHERE b.APRTCDGO = a.APRTCDGO),
        a.APRTSLDO = 0,
        a.APRTIDST = 4
WHERE   NVL(a.APRTVLRR,0) = 0
AND     TRUNC(a.APRTFCTR,'MM') = DATE '2025-06-01'
AND     EXISTS (SELECT 1 FROM CRD.BKP_APRT_VALOR_20260827 b
                WHERE b.APRTCDGO = a.APRTCDGO AND b.APRTVLRR > 0);

COMMIT;


-- =====================================================================================
-- 4. VERIFICACION
-- =====================================================================================

-- 4.1  Junio 2025 completo. Esperado: ~$160.525 en total ($160.350,81 restaurados +
--      $174,08 de las 3 filas del script 66 + $2.691 de las manuales), y CERO filas en
--      cero con usuario nulo.
SELECT  NVL(a.APRTUSRG,'(null)') AS USUARIO,
        COUNT(*)                 AS FILAS,
        SUM(CASE WHEN NVL(a.APRTVLRR,0) = 0 THEN 1 ELSE 0 END) AS EN_CERO,
        SUM(a.APRTVLRR)          AS VALOR
FROM    CRD.APRT a
WHERE   a.TPAPCDGO IN (9,11)
AND     TRUNC(a.APRTFCTR,'MM') = DATE '2025-06-01'
GROUP BY NVL(a.APRTUSRG,'(null)')
ORDER BY 1;

-- 4.2  No debe quedar ninguna fila de JUNIO en cero que el respaldo tuviera con valor.
--      Esperado: 0.
SELECT  COUNT(*) AS TODAVIA_EN_CERO_JUNIO
FROM    CRD.APRT a
JOIN    CRD.BKP_APRT_VALOR_20260827 b ON b.APRTCDGO = a.APRTCDGO
WHERE   NVL(a.APRTVLRR,0) = 0 AND b.APRTVLRR > 0
AND     TRUNC(a.APRTFCTR,'MM') = DATE '2025-06-01';

-- 4.3  Lo que sigue en cero en OTROS meses, si el control 1.2 mostro alguno.
--      Este script NO los toco a proposito.
SELECT  TO_CHAR(a.APRTFCTR,'YYYY-MM') AS MES_CAJA, COUNT(*) AS FILAS_EN_CERO,
        SUM(b.APRTVLRR) AS VALOR_SIN_RESTAURAR
FROM    CRD.APRT a
JOIN    CRD.BKP_APRT_VALOR_20260827 b ON b.APRTCDGO = a.APRTCDGO
WHERE   NVL(a.APRTVLRR,0) = 0 AND b.APRTVLRR > 0
GROUP BY TO_CHAR(a.APRTFCTR,'YYYY-MM')
ORDER BY 1;

-- 4.4  Contraste contra lo que Petro descontó en junio 2025. La diferencia esperada es
--      pequeña (los 7 roles sin entidad, $746,74, y el remanente).
SELECT  (SELECT ROUND(SUM(NVL(x.PXCADSDO,0)),2)
         FROM   CRD.DTCA d JOIN CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
         WHERE  d.CRARCDGO = 352 AND d.DTCACDPP = 'AH')            AS DESCONTADO_ARCHIVO,
        (SELECT ROUND(SUM(a.APRTVLRR),2) FROM CRD.APRT a
         WHERE  a.TPAPCDGO IN (9,11)
         AND    TRUNC(a.APRTFCTR,'MM') = DATE '2025-06-01')        AS REGISTRADO_HOY
FROM    DUAL;


-- =====================================================================================
-- 5. REVERSO
-- =====================================================================================
-- ⛔ COMENTADO A PROPOSITO. Corre SOLO si hay que deshacer esta restauracion.
--    Si el script se ejecuta de corrido con esto activo, revierte en silencio todo lo que
--    acaba de hacer y el resultado parece correcto.
--
-- UPDATE CRD.APRT a
-- SET   (a.APRTVLRR, a.APRTVLPG, a.APRTSLDO, a.APRTIDST) =
--       (SELECT c.APRTVLRR, c.APRTVLPG, c.APRTSLDO, c.APRTIDST
--        FROM   CRD.BKP_APRT_CERO_20260827 c WHERE c.APRTCDGO = a.APRTCDGO)
-- WHERE EXISTS (SELECT 1 FROM CRD.BKP_APRT_CERO_20260827 c WHERE c.APRTCDGO = a.APRTCDGO);
-- COMMIT;

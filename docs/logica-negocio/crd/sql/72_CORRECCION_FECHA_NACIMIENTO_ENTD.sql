-- =====================================================================================
-- 72 — CORRECCION DE LA FECHA DE NACIMIENTO DE ENTIDADES, DESDE CRD.EXTR
-- =====================================================================================
-- FECHA: 2026-08-27
--
-- QUE PASO
--   La pantalla de actualizacion de datos del participe corria la fecha de nacimiento UN
--   DIA HACIA ATRAS en cada guardado. Es el defecto de zona horaria que ya documenta
--   CLAUDE.md, aplicado a este campo: ENTD.ENTDFCNC es VARCHAR2, el frontend la convierte
--   a Date para el selector y la vuelve a serializar; Ecuador es UTC-5, asi que la
--   medianoche UTC renderizada en local cae el dia anterior.
--   El efecto es ACUMULATIVO: diez guardados, diez dias menos. Nadie lo nota hasta que la
--   fecha de alguien esta anios corrida.
--
-- POR QUE SE PUEDE CORREGIR
--   Hay una fuente independiente: CRD.EXTR guarda la misma fecha (EXTRFCNC, y es un
--   TIMESTAMP de verdad, no texto), cruzada por cedula contra ENTD.ENTDNMID. EXTR no pasa
--   por esa pantalla, asi que no sufrio el corrimiento.
--
-- ⛔ PRECONDICION BLOQUEANTE
--   NO EJECUTAR EL BLOQUE 3 HASTA QUE EL ARREGLO DEL FRONTEND ESTE DESPLEGADO. Si se
--   corrige la base con el defecto todavia vivo, las fichas vuelven a correrse en el
--   siguiente guardado y el trabajo se pierde sin dejar rastro.
--   Los bloques 0 a 2 son de lectura y respaldo: esos si se pueden correr ya.
--
-- ALCANCE DE LA CORRECCION
--   Solo las filas cuya diferencia tiene la FIRMA DEL DEFECTO: ENTD por DETRAS de EXTR,
--   entre 1 y 30 dias. Todo lo demas —diferencias negativas, o mayores— NO se toca: son
--   divergencias entre las dos fuentes que vienen de la migracion, y ahi no sabemos cual
--   de las dos tiene razon. Salen listadas en el control 1.3 para revisarlas aparte.
--
-- SQL PURO: sin SET / DEFINE / WHENEVER.
-- =====================================================================================


-- =====================================================================================
-- 0. FORMATO REAL DE ENTDFCNC — leer antes que nada
-- =====================================================================================
-- ENTDFCNC es VARCHAR2, asi que puede traer cualquier cosa. El bloque 3 escribe con el
-- MISMO formato que ya predomina, para no introducir una segunda variante en la columna.
-- Si aqui aparece un formato mayoritario distinto de YYYY-MM-DD, hay que ajustar el
-- TO_CHAR del bloque 3 antes de ejecutarlo.
SELECT  CASE
            WHEN e.ENTDFCNC IS NULL                                          THEN '(nulo)'
            WHEN REGEXP_LIKE(e.ENTDFCNC, '^\d{4}-\d{2}-\d{2}$')              THEN 'YYYY-MM-DD'
            WHEN REGEXP_LIKE(e.ENTDFCNC, '^\d{4}-\d{2}-\d{2} ')              THEN 'YYYY-MM-DD con hora'
            WHEN REGEXP_LIKE(e.ENTDFCNC, '^\d{2}/\d{2}/\d{4}$')              THEN 'DD/MM/YYYY'
            WHEN REGEXP_LIKE(e.ENTDFCNC, '^\d{4}/\d{2}/\d{2}$')              THEN 'YYYY/MM/DD'
            ELSE                                                                  'OTRO'
        END                                   AS FORMATO,
        COUNT(*)                              AS FILAS,
        MIN(e.ENTDFCNC)                       AS EJEMPLO_MIN,
        MAX(e.ENTDFCNC)                       AS EJEMPLO_MAX
FROM    CRD.ENTD e
GROUP BY CASE
            WHEN e.ENTDFCNC IS NULL                                          THEN '(nulo)'
            WHEN REGEXP_LIKE(e.ENTDFCNC, '^\d{4}-\d{2}-\d{2}$')              THEN 'YYYY-MM-DD'
            WHEN REGEXP_LIKE(e.ENTDFCNC, '^\d{4}-\d{2}-\d{2} ')              THEN 'YYYY-MM-DD con hora'
            WHEN REGEXP_LIKE(e.ENTDFCNC, '^\d{2}/\d{2}/\d{4}$')              THEN 'DD/MM/YYYY'
            WHEN REGEXP_LIKE(e.ENTDFCNC, '^\d{4}/\d{2}/\d{2}$')              THEN 'YYYY/MM/DD'
            ELSE                                                                  'OTRO'
         END
ORDER BY 2 DESC;


-- =====================================================================================
-- 1. LA FOTO DEL DAÑO
-- =====================================================================================

-- 1.1  Distribucion de las diferencias. DIAS = EXTR − ENTD, o sea que POSITIVO significa
--      que ENTD quedo ATRASADA, que es exactamente lo que hace el defecto.
--      Si el grueso cae en "1 a 5 dias" y "6 a 30 dias" con valores enteros pequeños, ese
--      es el rastro. Diferencias negativas o de anios son otra cosa.
WITH PAR AS (
        SELECT  e.ENTDCDGO,
                TRUNC(x.EXTRFCNC) - TO_DATE(SUBSTR(e.ENTDFCNC, 1, 10), 'YYYY-MM-DD') AS DIAS
        FROM    CRD.ENTD e
        JOIN    CRD.EXTR x ON x.EXTRCDLA = e.ENTDNMID
        WHERE   e.ENTDFCNC IS NOT NULL
        AND     x.EXTRFCNC IS NOT NULL
        AND     REGEXP_LIKE(SUBSTR(e.ENTDFCNC, 1, 10), '^\d{4}-\d{2}-\d{2}$'))
SELECT  CASE WHEN DIAS = 0            THEN 'A. iguales'
             WHEN DIAS BETWEEN 1 AND 5   THEN 'B. ENTD atrasada 1 a 5 dias   <-- firma del defecto'
             WHEN DIAS BETWEEN 6 AND 30  THEN 'C. ENTD atrasada 6 a 30 dias  <-- firma del defecto'
             WHEN DIAS > 30           THEN 'D. ENTD atrasada mas de 30 dias (revisar)'
             ELSE                          'E. ENTD ADELANTADA (revisar, el defecto no puede causarlo)'
        END                    AS TRAMO,
        COUNT(*)               AS ENTIDADES,
        MIN(DIAS)              AS DIAS_MIN,
        MAX(DIAS)              AS DIAS_MAX
FROM    PAR
GROUP BY CASE WHEN DIAS = 0            THEN 'A. iguales'
              WHEN DIAS BETWEEN 1 AND 5   THEN 'B. ENTD atrasada 1 a 5 dias   <-- firma del defecto'
              WHEN DIAS BETWEEN 6 AND 30  THEN 'C. ENTD atrasada 6 a 30 dias  <-- firma del defecto'
              WHEN DIAS > 30           THEN 'D. ENTD atrasada mas de 30 dias (revisar)'
              ELSE                          'E. ENTD ADELANTADA (revisar, el defecto no puede causarlo)'
         END
ORDER BY 1;

-- 1.2  Cuantas entidades quedan FUERA del cruce y por que. No se pueden corregir: no hay
--      contra que compararlas.
SELECT  SUM(CASE WHEN e.ENTDFCNC IS NULL THEN 1 ELSE 0 END)                    AS SIN_FECHA_EN_ENTD,
        SUM(CASE WHEN e.ENTDFCNC IS NOT NULL
                  AND NOT REGEXP_LIKE(SUBSTR(e.ENTDFCNC,1,10), '^\d{4}-\d{2}-\d{2}$')
                 THEN 1 ELSE 0 END)                                            AS FORMATO_NO_RECONOCIDO,
        SUM(CASE WHEN e.ENTDFCNC IS NOT NULL
                  AND NOT EXISTS (SELECT 1 FROM CRD.EXTR x
                                  WHERE x.EXTRCDLA = e.ENTDNMID AND x.EXTRFCNC IS NOT NULL)
                 THEN 1 ELSE 0 END)                                            AS SIN_PAR_EN_EXTR,
        COUNT(*)                                                               AS TOTAL_ENTIDADES
FROM    CRD.ENTD e;

-- 1.3  Los casos que NO se van a corregir, para revisarlos a mano.
--      Diferencias grandes o en sentido contrario: ahi no sabemos cual fuente tiene razon.
SELECT  e.ENTDNMID AS CEDULA, e.ENTDRZNS AS NOMBRE,
        e.ENTDFCNC AS FECHA_EN_ENTD,
        TO_CHAR(x.EXTRFCNC, 'YYYY-MM-DD') AS FECHA_EN_EXTR,
        TRUNC(x.EXTRFCNC) - TO_DATE(SUBSTR(e.ENTDFCNC,1,10), 'YYYY-MM-DD') AS DIAS
FROM    CRD.ENTD e
JOIN    CRD.EXTR x ON x.EXTRCDLA = e.ENTDNMID
WHERE   e.ENTDFCNC IS NOT NULL AND x.EXTRFCNC IS NOT NULL
AND     REGEXP_LIKE(SUBSTR(e.ENTDFCNC,1,10), '^\d{4}-\d{2}-\d{2}$')
AND     (   TRUNC(x.EXTRFCNC) - TO_DATE(SUBSTR(e.ENTDFCNC,1,10), 'YYYY-MM-DD') < 0
         OR TRUNC(x.EXTRFCNC) - TO_DATE(SUBSTR(e.ENTDFCNC,1,10), 'YYYY-MM-DD') > 30 )
ORDER BY ABS(TRUNC(x.EXTRFCNC) - TO_DATE(SUBSTR(e.ENTDFCNC,1,10), 'YYYY-MM-DD')) DESC;


-- =====================================================================================
-- 2. RESPALDO
-- =====================================================================================
CREATE TABLE CRD.BKP_ENTD_FCNC_20260827 AS
SELECT  e.ENTDCDGO, e.ENTDNMID, e.ENTDRZNS, e.ENTDFCNC
FROM    CRD.ENTD e
WHERE   e.ENTDFCNC IS NOT NULL;

SELECT COUNT(*) AS FILAS_RESPALDADAS FROM CRD.BKP_ENTD_FCNC_20260827;


-- =====================================================================================
-- 3. LA CORRECCION
-- =====================================================================================
-- ⛔ NO EJECUTAR HASTA QUE EL ARREGLO DEL FRONTEND ESTE DESPLEGADO. Con el defecto vivo,
--    las fichas se vuelven a correr en el siguiente guardado.
--
-- EL 30 ES LA PERILLA: es el maximo de dias de atraso que se considera "firma del
-- defecto". Ajustalo despues de mirar el control 1.1. Si el tramo D esta vacio, el 30
-- sobra y no cambia nada; si tiene volumen, decidi conscientemente si entran o no.
--
-- El TO_CHAR usa YYYY-MM-DD porque es el formato que espera el control 0. Si ahi
-- predomino otro, cambialo aqui ANTES de ejecutar.

UPDATE  CRD.ENTD e
SET     e.ENTDFCNC = ( SELECT TO_CHAR(TRUNC(x.EXTRFCNC), 'YYYY-MM-DD')
                       FROM   CRD.EXTR x WHERE x.EXTRCDLA = e.ENTDNMID )
WHERE   e.ENTDFCNC IS NOT NULL
AND     REGEXP_LIKE(SUBSTR(e.ENTDFCNC,1,10), '^\d{4}-\d{2}-\d{2}$')
AND     EXISTS ( SELECT 1 FROM CRD.EXTR x
                 WHERE x.EXTRCDLA = e.ENTDNMID AND x.EXTRFCNC IS NOT NULL )
AND     ( SELECT TRUNC(x.EXTRFCNC) FROM CRD.EXTR x WHERE x.EXTRCDLA = e.ENTDNMID )
        - TO_DATE(SUBSTR(e.ENTDFCNC,1,10), 'YYYY-MM-DD') BETWEEN 1 AND 30;

COMMIT;


-- =====================================================================================
-- 4. VERIFICACION
-- =====================================================================================

-- 4.1  Los tramos B y C deben quedar en cero. El A crece por la misma cantidad.
--      Los tramos D y E no se movieron: siguen igual que en el control 1.1.
WITH PAR AS (
        SELECT  TRUNC(x.EXTRFCNC) - TO_DATE(SUBSTR(e.ENTDFCNC, 1, 10), 'YYYY-MM-DD') AS DIAS
        FROM    CRD.ENTD e
        JOIN    CRD.EXTR x ON x.EXTRCDLA = e.ENTDNMID
        WHERE   e.ENTDFCNC IS NOT NULL AND x.EXTRFCNC IS NOT NULL
        AND     REGEXP_LIKE(SUBSTR(e.ENTDFCNC, 1, 10), '^\d{4}-\d{2}-\d{2}$'))
SELECT  CASE WHEN DIAS = 0                  THEN 'A. iguales'
             WHEN DIAS BETWEEN 1 AND 30     THEN 'B+C. deberia estar en CERO'
             WHEN DIAS > 30                 THEN 'D. sin tocar'
             ELSE                                'E. sin tocar'
        END AS TRAMO, COUNT(*) AS ENTIDADES
FROM    PAR
GROUP BY CASE WHEN DIAS = 0                  THEN 'A. iguales'
              WHEN DIAS BETWEEN 1 AND 30     THEN 'B+C. deberia estar en CERO'
              WHEN DIAS > 30                 THEN 'D. sin tocar'
              ELSE                                'E. sin tocar' END
ORDER BY 1;

-- 4.2  Cuantas filas cambiaron realmente, contra el respaldo.
SELECT  COUNT(*) AS FILAS_CORREGIDAS
FROM    CRD.ENTD e
JOIN    CRD.BKP_ENTD_FCNC_20260827 b ON b.ENTDCDGO = e.ENTDCDGO
WHERE   NVL(e.ENTDFCNC,'x') <> NVL(b.ENTDFCNC,'x');

-- 4.3  Muestra de lo corregido, para ojear que las fechas son plausibles.
SELECT  e.ENTDNMID AS CEDULA, e.ENTDRZNS AS NOMBRE,
        b.ENTDFCNC AS ANTES, e.ENTDFCNC AS DESPUES
FROM    CRD.ENTD e
JOIN    CRD.BKP_ENTD_FCNC_20260827 b ON b.ENTDCDGO = e.ENTDCDGO
WHERE   NVL(e.ENTDFCNC,'x') <> NVL(b.ENTDFCNC,'x')
ORDER BY e.ENTDNMID
FETCH FIRST 30 ROWS ONLY;


-- =====================================================================================
-- 5. REVERSO
-- =====================================================================================
-- ⛔ COMENTADO A PROPOSITO. Corre SOLO si hay que deshacer la correccion.
--    Si el script se ejecuta de corrido con esto activo, revierte en silencio todo lo que
--    acaba de hacer y el resultado parece correcto.
--
-- UPDATE  CRD.ENTD e
-- SET     e.ENTDFCNC = ( SELECT b.ENTDFCNC FROM CRD.BKP_ENTD_FCNC_20260827 b
--                        WHERE b.ENTDCDGO = e.ENTDCDGO )
-- WHERE   EXISTS ( SELECT 1 FROM CRD.BKP_ENTD_FCNC_20260827 b
--                  WHERE b.ENTDCDGO = e.ENTDCDGO );
-- COMMIT;
--
-- Limpieza del respaldo, solo cuando la correccion este validada:
-- DROP TABLE CRD.BKP_ENTD_FCNC_20260827 PURGE;

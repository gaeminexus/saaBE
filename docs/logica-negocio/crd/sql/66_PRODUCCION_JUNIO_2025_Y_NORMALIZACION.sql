-- =====================================================================================
-- 66 — PRODUCCION: RECONSTRUCCION DE JUNIO 2025 + NORMALIZACION DE FILAS MIGRADAS
-- =====================================================================================
-- FECHA: 2026-08-27
-- Reemplaza al 65 para producción. NO correr el 65 allá: esta versión evita el problema
-- que el 65 destapó en local (ver "QUE CAMBIA" más abajo).
--
-- ORDEN OBLIGATORIO EN PRODUCCION
--   1. 62_CORRECCION_VALOR_APORTES_CARGA.sql   ← todavía NO se ejecutó en producción
--   2. ESTE SCRIPT
--   3. 63_BACKFILL_DEVENGO_APORTES.sql          ← después, nunca antes (ver su encabezado)
--   4. Desplegar el WAR
--
-- Sacar copia de la base antes. Además, cada bloque crea su propio respaldo: eso permite
-- revertir SOLO lo que tocó el script, sin restaurar toda la base.
--
-- =====================================================================================
-- QUE CAMBIA RESPECTO DEL 65 QUE SE CORRIO EN LOCAL
-- =====================================================================================
--   a) El INSERT lleva un NOT EXISTS que EXCLUYE a los partícipes que YA tengan un aporte
--      de junio 2025. En local, 17 partícipes ya lo tenían registrado a mano (usuario
--      SAA_UC, desde el 2025-06-25) y el script se los duplicó: hubo que insertar y
--      después borrar $2.041,04. Con el guardarraíl no se crean nunca.
--   b) Se agrega el bloque A.4b: registra el REMANENTE (lo descontado por encima de lo
--      esperado en HSTR). Decisión del usuario del 2026-08-27: es su dinero y debe
--      quedar registrado, para que se cumpla "lo registrado = lo descontado".
--
-- =====================================================================================
-- VALORES ESPERADOS, MEDIDOS EN LOCAL (copia exacta de producción, 2026-08-27)
-- =====================================================================================
--   Líneas AH de la carga 352 con descuento .................... 2.021
--   ... de ellas, con rol que NO resuelve a ninguna entidad .....     7   ($746,74)
--   ... con entidad ............................................ 2.014   ($161.257,56)
--   Partícipes sin HSTR estado 99 ..............................     0
--   Partícipes que YA tienen junio 2025 (se excluyen) ..........    17   ($2.041,04)
--   ---------------------------------------------------------------------------------
--   Partícipes que este script debe insertar ................... 1.997
--   Monto a registrar sin remanente ............................ $159.026,47
--   Remanente total (bloque A.4b) .............................. hasta $190,05
--
--   Si producción se movió respecto de la copia, los controles lo muestran ANTES de
--   insertar. Cualquier desvío grande: PARAR y avisar.
-- =====================================================================================


-- =====================================================================================
-- A.1  CONTROL BLOQUEANTE
-- =====================================================================================
-- Esperado: FILAS_APRT = 0, FILAS_PGAP = 0, PARTICIPES_JUN_2025 = 17.
-- Si FILAS_APRT > 0, este script YA CORRIO: parar.
SELECT  (SELECT COUNT(*) FROM CRD.APRT a WHERE a.APRTIDAS = 352)          AS FILAS_APRT,
        (SELECT COUNT(*) FROM CRD.PGAP p
         WHERE p.PGAPCNCP LIKE '%CargaArchivo: 352')                      AS FILAS_PGAP,
        (SELECT COUNT(DISTINCT a.ENTDCDGO) FROM CRD.APRT a
         WHERE a.TPAPCDGO IN (9,11) AND a.APRTVLRR > 0
         AND   TRUNC(a.APRTFCTR,'MM') = DATE '2025-06-01')                AS PARTICIPES_JUN_2025
FROM    DUAL;

-- La carga sigue siendo la que creemos: periodo 6/2025, filial 1, estado 3.
SELECT c.CRARCDGO, c.CRARMSAF AS MES, c.CRARANAF AS ANIO, c.FLLLCDGO AS FILIAL,
       c.CRARESTD AS ESTADO, c.CRARFCCR AS FECHA_CARGA
FROM   CRD.CRAR c WHERE c.CRARCDGO = 352;


-- =====================================================================================
-- A.2  RESUMEN — comparar contra los valores esperados del encabezado
-- =====================================================================================
-- El reparto replica la prelación del proceso: JUBILACION (9) primero hasta su monto
-- esperado, luego CESANTIA (11) hasta el suyo. Misma regla que la Fase 2 del plan.
WITH LINEAS AS (
        SELECT x.PXCACDPT AS ROL, NVL(x.PXCADSDO,0) AS DESCONTADO
        FROM   CRD.DTCA d JOIN CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
        WHERE  d.CRARCDGO = 352 AND d.DTCACDPP = 'AH' AND NVL(x.PXCADSDO,0) > 0.01),
HS AS ( SELECT h.ENTDCDGO, NVL(h.HSTRMNAJ,0) AS MONTO_JUB, NVL(h.HSTRMNAC,0) AS MONTO_CES
        FROM ( SELECT h.*, ROW_NUMBER() OVER (PARTITION BY h.ENTDCDGO
                      ORDER BY h.HSTRFCIN DESC, h.HSTRCDGO DESC) rn
               FROM CRD.HSTR h WHERE h.HSTRESTD = 99 ) h WHERE h.rn = 1),
BASE AS (
        SELECT  e.ENTDCDGO, l.DESCONTADO, hs.MONTO_JUB, hs.MONTO_CES,
                ROUND(LEAST(l.DESCONTADO, NVL(hs.MONTO_JUB,0)), 2) AS ASIG_JUB,
                CASE WHEN hs.ENTDCDGO IS NULL THEN 1 ELSE 0 END    AS SIN_HSTR,
                CASE WHEN EXISTS (SELECT 1 FROM CRD.APRT a
                                  WHERE a.ENTDCDGO = e.ENTDCDGO
                                  AND   a.TPAPCDGO IN (9,11) AND a.APRTVLRR > 0
                                  AND   TRUNC(a.APRTFCTR,'MM') = DATE '2025-06-01')
                     THEN 1 ELSE 0 END                             AS YA_TIENE_JUNIO
        FROM    LINEAS l JOIN CRD.ENTD e ON e.ENTDRLPC = l.ROL
                LEFT JOIN HS hs ON hs.ENTDCDGO = e.ENTDCDGO)
SELECT  COUNT(*)                                                  AS LINEAS_CON_ENTIDAD,
        SUM(b.SIN_HSTR)                                           AS SIN_HSTR_99,
        SUM(b.YA_TIENE_JUNIO)                                     AS YA_TIENEN_JUNIO,
        ROUND(SUM(b.DESCONTADO), 2)                               AS TOTAL_DESCONTADO,
        COUNT(*) - SUM(b.SIN_HSTR) - SUM(b.YA_TIENE_JUNIO)        AS PARTICIPES_A_INSERTAR,
        ROUND(SUM(CASE WHEN b.SIN_HSTR = 0 AND b.YA_TIENE_JUNIO = 0
                       THEN b.ASIG_JUB ELSE 0 END), 2)            AS TOTAL_JUBILACION,
        ROUND(SUM(CASE WHEN b.SIN_HSTR = 0 AND b.YA_TIENE_JUNIO = 0
                       THEN LEAST(b.DESCONTADO - b.ASIG_JUB, NVL(b.MONTO_CES,0))
                       ELSE 0 END), 2)                            AS TOTAL_CESANTIA,
        ROUND(SUM(CASE WHEN b.SIN_HSTR = 0 AND b.YA_TIENE_JUNIO = 0
                       THEN b.DESCONTADO - b.ASIG_JUB
                            - LEAST(b.DESCONTADO - b.ASIG_JUB, NVL(b.MONTO_CES,0))
                       ELSE 0 END), 2)                            AS TOTAL_REMANENTE
FROM    BASE b;

-- Roles del archivo que no resuelven a ninguna entidad. Esperado: 7 filas, $746,74.
-- NO se insertan: quedan para resolver a mano.
SELECT  x.PXCACDPT AS ROL, x.PXCANMBR AS NOMBRE_ARCHIVO, NVL(x.PXCADSDO,0) AS DESCONTADO
FROM    CRD.DTCA d JOIN CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
WHERE   d.CRARCDGO = 352 AND d.DTCACDPP = 'AH' AND NVL(x.PXCADSDO,0) > 0.01
AND     NOT EXISTS (SELECT 1 FROM CRD.ENTD e WHERE e.ENTDRLPC = x.PXCACDPT)
ORDER BY DESCONTADO DESC;

-- Roles que resuelven a MAS DE UNA entidad: duplicarían el aporte. Esperado: 0 filas.
SELECT  e.ENTDRLPC AS ROL, COUNT(*) AS ENTIDADES,
        LISTAGG(e.ENTDCDGO || '=' || e.ENTDNMID, ' | ')
            WITHIN GROUP (ORDER BY e.ENTDCDGO) AS DETALLE
FROM    CRD.ENTD e
WHERE   e.ENTDRLPC IN (SELECT x.PXCACDPT FROM CRD.DTCA d
                       JOIN CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
                       WHERE d.CRARCDGO = 352 AND d.DTCACDPP = 'AH')
GROUP BY e.ENTDRLPC HAVING COUNT(*) > 1;

-- Los que se EXCLUYEN por tener junio ya registrado. Esperado: 17 filas, $2.041,04.
SELECT  e.ENTDNMID AS CEDULA, e.ENTDRZNS AS NOMBRE,
        NVL(x.PXCADSDO,0) AS DESCONTADO_ARCHIVO,
        (SELECT SUM(a.APRTVLRR) FROM CRD.APRT a
         WHERE a.ENTDCDGO = e.ENTDCDGO AND a.TPAPCDGO IN (9,11) AND a.APRTVLRR > 0
         AND   TRUNC(a.APRTFCTR,'MM') = DATE '2025-06-01')      AS YA_REGISTRADO,
        (SELECT MAX(a.APRTUSRG) FROM CRD.APRT a
         WHERE a.ENTDCDGO = e.ENTDCDGO AND a.TPAPCDGO IN (9,11)
         AND   TRUNC(a.APRTFCTR,'MM') = DATE '2025-06-01')      AS USUARIO_ORIGEN
FROM    CRD.DTCA d
JOIN    CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
JOIN    CRD.ENTD e ON e.ENTDRLPC = x.PXCACDPT
WHERE   d.CRARCDGO = 352 AND d.DTCACDPP = 'AH' AND NVL(x.PXCADSDO,0) > 0.01
AND     EXISTS (SELECT 1 FROM CRD.APRT a
                WHERE a.ENTDCDGO = e.ENTDCDGO
                AND   a.TPAPCDGO IN (9,11) AND a.APRTVLRR > 0
                AND   TRUNC(a.APRTFCTR,'MM') = DATE '2025-06-01')
ORDER BY YA_REGISTRADO DESC;


-- =====================================================================================
-- A.3  INSERCION DE LOS APORTES
-- =====================================================================================
-- Nacen YA EN EL MODELO NUEVO: valor = valorPagado = lo recibido, saldo = 0, estado = 4.
-- Y con las columnas nuevas llenas: APRTPRDV = 2025-06-01, APRTTPMV = 1 (APORTE_MENSUAL).
-- Por eso NO necesitan pasar por el backfill del script 63.
--
-- APRTFCTR = 30-jun-2025 23:59:59: el último día del mes de afectación a las 23:59:59,
-- el mismo instante exacto que graba crearNuevoAporte. Es la FECHA DE CAJA, la que lee
-- contabilidad, y es correcta porque ese es el periodo en que entró el dinero.
--
-- APRTCDGO no se especifica: la PK de CRD.APRT es IDENTITY.

INSERT INTO CRD.APRT
       (ENTDCDGO, FLLLCDGO, TPAPCDGO, APRTVLRR, APRTVLPG, APRTSLDO, APRTIDST,
        APRTIDAS, APRTFCTR, APRTPRDV, APRTTPMV, APRTGLSA, APRTUSRG, APRTFCRG)
WITH LINEAS AS (
        SELECT x.PXCACDPT AS ROL, NVL(x.PXCADSDO,0) AS DESCONTADO
        FROM   CRD.DTCA d JOIN CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
        WHERE  d.CRARCDGO = 352 AND d.DTCACDPP = 'AH' AND NVL(x.PXCADSDO,0) > 0.01),
HS AS ( SELECT h.ENTDCDGO, NVL(h.HSTRMNAJ,0) AS MONTO_JUB, NVL(h.HSTRMNAC,0) AS MONTO_CES
        FROM ( SELECT h.*, ROW_NUMBER() OVER (PARTITION BY h.ENTDCDGO
                      ORDER BY h.HSTRFCIN DESC, h.HSTRCDGO DESC) rn
               FROM CRD.HSTR h WHERE h.HSTRESTD = 99 ) h WHERE h.rn = 1),
BASE AS (
        SELECT  e.ENTDCDGO, e.FLLLCDGO,
                ROUND(LEAST(l.DESCONTADO, hs.MONTO_JUB), 2) AS ASIG_JUB,
                ROUND(LEAST(l.DESCONTADO - LEAST(l.DESCONTADO, hs.MONTO_JUB),
                            hs.MONTO_CES), 2)               AS ASIG_CES
        FROM    LINEAS l
        JOIN    CRD.ENTD e ON e.ENTDRLPC = l.ROL
        JOIN    HS hs      ON hs.ENTDCDGO = e.ENTDCDGO      -- sin HSTR no se inserta
        WHERE   NOT EXISTS (SELECT 1 FROM CRD.APRT a        -- GUARDARRAIL ANTI-DUPLICADO
                            WHERE a.ENTDCDGO = e.ENTDCDGO
                            AND   a.TPAPCDGO IN (9,11) AND a.APRTVLRR > 0
                            AND   TRUNC(a.APRTFCTR,'MM') = DATE '2025-06-01'))
SELECT  b.ENTDCDGO, b.FLLLCDGO, 9, b.ASIG_JUB, b.ASIG_JUB, 0, 4, 352,
        TO_TIMESTAMP('2025-06-30 23:59:59', 'YYYY-MM-DD HH24:MI:SS'),
        DATE '2025-06-01', 1,
        'Aporte Jubilación - Mes 6/2025 - CargaArchivo: 352', 'SAA_AH', SYSTIMESTAMP
FROM    BASE b WHERE b.ASIG_JUB > 0.01
UNION ALL
SELECT  b.ENTDCDGO, b.FLLLCDGO, 11, b.ASIG_CES, b.ASIG_CES, 0, 4, 352,
        TO_TIMESTAMP('2025-06-30 23:59:59', 'YYYY-MM-DD HH24:MI:SS'),
        DATE '2025-06-01', 1,
        'Aporte Cesantía - Mes 6/2025 - CargaArchivo: 352', 'SAA_AH', SYSTIMESTAMP
FROM    BASE b WHERE b.ASIG_CES > 0.01;

COMMIT;


-- =====================================================================================
-- A.4  EL REMANENTE
-- =====================================================================================
-- Lo descontado POR ENCIMA de lo esperado en HSTR. Decisión del usuario (2026-08-27):
-- se registra, porque es su dinero y así se cumple "lo registrado = lo descontado".
--
-- Va como una fila más del MISMO mes, del tipo que el partícipe tenga (cesantía si la
-- tiene, jubilación si no). NO se anticipa al mes siguiente: julio 2025 ya tiene sus
-- propios aportes de la carga 353 y anticiparlo lo dejaría sobre-cubierto.
-- La glosa dice "excedente" para que sea rastreable.
--
-- OJO: este bloque debe correr DESPUES del A.3 y usa el mismo guardarraíl, pero además
-- exige que el partícipe tenga ya sus filas de esta carga — así no le crea un excedente
-- suelto a alguien que fue excluido.

INSERT INTO CRD.APRT
       (ENTDCDGO, FLLLCDGO, TPAPCDGO, APRTVLRR, APRTVLPG, APRTSLDO, APRTIDST,
        APRTIDAS, APRTFCTR, APRTPRDV, APRTTPMV, APRTGLSA, APRTUSRG, APRTFCRG)
WITH LINEAS AS (
        SELECT x.PXCACDPT AS ROL, NVL(x.PXCADSDO,0) AS DESCONTADO
        FROM   CRD.DTCA d JOIN CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
        WHERE  d.CRARCDGO = 352 AND d.DTCACDPP = 'AH' AND NVL(x.PXCADSDO,0) > 0.01),
HS AS ( SELECT h.ENTDCDGO, NVL(h.HSTRMNAJ,0) AS MONTO_JUB, NVL(h.HSTRMNAC,0) AS MONTO_CES
        FROM ( SELECT h.*, ROW_NUMBER() OVER (PARTITION BY h.ENTDCDGO
                      ORDER BY h.HSTRFCIN DESC, h.HSTRCDGO DESC) rn
               FROM CRD.HSTR h WHERE h.HSTRESTD = 99 ) h WHERE h.rn = 1)
SELECT  e.ENTDCDGO, e.FLLLCDGO,
        CASE WHEN hs.MONTO_CES > 0 THEN 11 ELSE 9 END,
        r.REMANENTE, r.REMANENTE, 0, 4, 352,
        TO_TIMESTAMP('2025-06-30 23:59:59', 'YYYY-MM-DD HH24:MI:SS'),
        DATE '2025-06-01', 1,
        'Aporte ' || CASE WHEN hs.MONTO_CES > 0 THEN 'Cesantía' ELSE 'Jubilación' END
            || ' excedente - Mes 6/2025 - CargaArchivo: 352',
        'SAA_AH', SYSTIMESTAMP
FROM    LINEAS l
JOIN    CRD.ENTD e ON e.ENTDRLPC = l.ROL
JOIN    HS hs      ON hs.ENTDCDGO = e.ENTDCDGO
CROSS   JOIN LATERAL (
        SELECT ROUND(l.DESCONTADO
                     - LEAST(l.DESCONTADO, hs.MONTO_JUB)
                     - LEAST(l.DESCONTADO - LEAST(l.DESCONTADO, hs.MONTO_JUB),
                             hs.MONTO_CES), 2) AS REMANENTE
        FROM DUAL) r
WHERE   r.REMANENTE > 0.01
AND     EXISTS (SELECT 1 FROM CRD.APRT a
                WHERE a.ENTDCDGO = e.ENTDCDGO AND a.APRTIDAS = 352);

COMMIT;


-- =====================================================================================
-- A.5  LOS PAGOS (CRD.PGAP)
-- =====================================================================================
-- Un PGAP por aporte, con el formato de concepto de crearRegistroPagoAporte, para que las
-- consultas del script 61 (que extraen el id de carga del concepto con REGEXP) cuadren.
-- Sin esto la carga 352 quedaría con N aportes y 0 pagos: justo la anomalía que el 61
-- marca como "fila sin pago". PGAPCDGO también es IDENTITY.

INSERT INTO CRD.PGAP
       (APRTCDGO, FLLLCDGO, PGAPVLRR, PGAPFCCN, PGAPCNCP, PGAPFCRG, PGAPUSRG, PGAPIDST)
SELECT  a.APRTCDGO, a.FLLLCDGO, a.APRTVLRR,
        TO_TIMESTAMP('2025-06-30 23:59:59', 'YYYY-MM-DD HH24:MI:SS'),
        'Pago aporte mes 6/2025 - Partícipe: ' || e.ENTDRLPC
            || ' (' || SUBSTR(e.ENTDRZNS, 1, 60) || ') - CargaArchivo: 352',
        SYSTIMESTAMP, 'SISTEMA', 1
FROM    CRD.APRT a
JOIN    CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
WHERE   a.APRTIDAS = 352
AND     NOT EXISTS (SELECT 1 FROM CRD.PGAP p WHERE p.APRTCDGO = a.APRTCDGO);

COMMIT;


-- =====================================================================================
-- A.6  VERIFICACION
-- =====================================================================================
-- Esperado, medido en local:
--   FILAS_CREADAS ..... 2.628 aprox (2.648 del local menos las de los 17 excluidos,
--                       más las del remanente)
--   PARTICIPES ........ 1.997
--   TOTAL_REGISTRADO .. $159.026,47 + remanente
--   PERIODOS_DEVENGO .. 1
--   FILAS_MAL_FORMADAS  0
SELECT  COUNT(*)                                            AS FILAS_CREADAS,
        COUNT(DISTINCT a.ENTDCDGO)                          AS PARTICIPES,
        SUM(CASE WHEN a.TPAPCDGO = 9  THEN a.APRTVLRR ELSE 0 END) AS TOTAL_JUBILACION,
        SUM(CASE WHEN a.TPAPCDGO = 11 THEN a.APRTVLRR ELSE 0 END) AS TOTAL_CESANTIA,
        SUM(CASE WHEN a.APRTGLSA LIKE '%excedente%' THEN a.APRTVLRR ELSE 0 END) AS TOTAL_REMANENTE,
        SUM(a.APRTVLRR)                                     AS TOTAL_REGISTRADO,
        COUNT(DISTINCT a.APRTPRDV)                          AS PERIODOS_DEVENGO,
        SUM(CASE WHEN a.APRTVLPG <> a.APRTVLRR OR NVL(a.APRTSLDO,0) <> 0
                  OR a.APRTIDST <> 4 OR a.APRTTPMV <> 1 THEN 1 ELSE 0 END) AS FILAS_MAL_FORMADAS
FROM    CRD.APRT a WHERE a.APRTIDAS = 352;

-- Un pago por aporte. Esperado: FILAS_SIN_PAGO = 0 y DIFERENCIA = 0.
SELECT  (SELECT COUNT(*) FROM CRD.APRT a WHERE a.APRTIDAS = 352
         AND NOT EXISTS (SELECT 1 FROM CRD.PGAP p WHERE p.APRTCDGO = a.APRTCDGO)) AS FILAS_SIN_PAGO,
        (SELECT ROUND(SUM(a.APRTVLRR),2) FROM CRD.APRT a WHERE a.APRTIDAS = 352)
      - (SELECT ROUND(SUM(p.PGAPVLRR),2) FROM CRD.PGAP p
         JOIN CRD.APRT a ON a.APRTCDGO = p.APRTCDGO WHERE a.APRTIDAS = 352) AS DIFERENCIA
FROM    DUAL;

-- ⚠ EL CONTROL QUE IMPORTA: nadie con junio por encima de lo que le descontaron.
-- Esperado en local: UNA sola fila, FIALLOS PACHECO (0603715772), con exceso 105,69 —
-- un ajuste manual anterior a este trabajo, no algo que provoque el script.
-- Si aparece cualquier otro, el guardarraíl no funcionó: PARAR y avisar.
WITH DESCONTADO AS (
        SELECT e.ENTDCDGO, SUM(NVL(x.PXCADSDO,0)) AS DESCONTADO
        FROM   CRD.DTCA d JOIN CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
        JOIN   CRD.ENTD e ON e.ENTDRLPC = x.PXCACDPT
        WHERE  d.CRARCDGO = 352 AND d.DTCACDPP = 'AH' AND NVL(x.PXCADSDO,0) > 0.01
        GROUP  BY e.ENTDCDGO)
SELECT  e.ENTDNMID AS CEDULA, e.ENTDRZNS AS NOMBRE, d.DESCONTADO,
        SUM(a.APRTVLRR) AS REGISTRADO_JUN,
        ROUND(SUM(a.APRTVLRR) - d.DESCONTADO, 2) AS EXCESO
FROM    CRD.APRT a
JOIN    CRD.ENTD e   ON e.ENTDCDGO = a.ENTDCDGO
JOIN    DESCONTADO d ON d.ENTDCDGO = a.ENTDCDGO
WHERE   a.TPAPCDGO IN (9,11) AND a.APRTVLRR > 0
AND     TRUNC(a.APRTFCTR,'MM') = DATE '2025-06-01'
GROUP BY e.ENTDNMID, e.ENTDRZNS, d.DESCONTADO
HAVING  SUM(a.APRTVLRR) - d.DESCONTADO > 0.01
ORDER BY 5 DESC;


-- =====================================================================================
-- A.7  REVERSO — no hace falta respaldo: nada de esto existía antes
-- =====================================================================================
-- DELETE FROM CRD.PGAP WHERE APRTCDGO IN (SELECT APRTCDGO FROM CRD.APRT WHERE APRTIDAS = 352);
-- DELETE FROM CRD.APRT WHERE APRTIDAS = 352;
-- COMMIT;


-- =====================================================================================
-- B  NORMALIZAR LAS FILAS MIGRADAS A MANO
-- =====================================================================================
-- Confirmado por el usuario: las filas de SAA_JUL_FIN son registros migrados a mano, con
-- el VALOR CORRECTO, a los que no se les llenó valorPagado.
-- Este bloque NO TOCA APRTVLRR: sólo alinea las columnas muertas al modelo nuevo, para
-- que nadie las lea como deuda pendiente. SUM(valor) no cambia ni un centavo: cero
-- impacto en saldos, reportes y contabilidad.
-- Para SAA_UC y SAA_UI el valorPagado traía un parcial; alinearlo borra ese rastro, que
-- queda preservado en el respaldo.

-- B.1  Qué se va a tocar. Esperado en local: SAA_JUL_FIN 84 filas / $36.036,31,
--      SAA_UC 138 / $7.879,75, SAA_UI 166 / $11.771,91.
SELECT  NVL(a.APRTUSRG,'(null)') AS USUARIO, COUNT(*) AS FILAS,
        SUM(a.APRTVLRR) AS VALOR, SUM(NVL(a.APRTVLPG,0)) AS PAGADO_ACTUAL,
        SUM(NVL(a.APRTSLDO,0)) AS SALDO_ACTUAL,
        MIN(a.APRTFCTR) AS DESDE, MAX(a.APRTFCTR) AS HASTA
FROM    CRD.APRT a
WHERE   NVL(a.APRTSLDO,0) > 0.01 AND a.APRTVLRR > 0
AND     NVL(a.APRTUSRG,'x') <> 'BATCH'
GROUP BY NVL(a.APRTUSRG,'(null)')
ORDER BY 3 DESC;

-- B.2  Respaldo
CREATE TABLE CRD.BKP_APRT_MIGRADAS_20260827 AS
SELECT a.* FROM CRD.APRT a
WHERE  NVL(a.APRTSLDO,0) > 0.01 AND a.APRTVLRR > 0
AND    a.APRTUSRG IN ('SAA_JUL_FIN', 'SAA_UC', 'SAA_UI');

SELECT COUNT(*) AS FILAS_RESPALDADAS FROM CRD.BKP_APRT_MIGRADAS_20260827;

-- B.3  La normalización. APRTVLRR NO SE TOCA.
UPDATE  CRD.APRT a
SET     a.APRTVLPG = a.APRTVLRR, a.APRTSLDO = 0, a.APRTIDST = 4
WHERE   NVL(a.APRTSLDO,0) > 0.01 AND a.APRTVLRR > 0
AND     a.APRTUSRG IN ('SAA_JUL_FIN', 'SAA_UC', 'SAA_UI');

COMMIT;

-- B.4  Verificación. Esperado: FILAS_CON_SALDO = 0 y VALOR_ANTES = VALOR_DESPUES.
SELECT  (SELECT COUNT(*) FROM CRD.APRT a
         WHERE NVL(a.APRTSLDO,0) > 0.01
         AND   a.APRTUSRG IN ('SAA_JUL_FIN','SAA_UC','SAA_UI'))              AS FILAS_CON_SALDO,
        (SELECT ROUND(SUM(b.APRTVLRR),2) FROM CRD.BKP_APRT_MIGRADAS_20260827 b) AS VALOR_ANTES,
        (SELECT ROUND(SUM(a.APRTVLRR),2) FROM CRD.APRT a
         JOIN  CRD.BKP_APRT_MIGRADAS_20260827 b ON b.APRTCDGO = a.APRTCDGO)  AS VALOR_DESPUES
FROM    DUAL;

-- B.5  Reverso
-- UPDATE CRD.APRT a
-- SET   (a.APRTVLPG, a.APRTSLDO, a.APRTIDST) =
--       (SELECT b.APRTVLPG, b.APRTSLDO, b.APRTIDST
--        FROM CRD.BKP_APRT_MIGRADAS_20260827 b WHERE b.APRTCDGO = a.APRTCDGO)
-- WHERE EXISTS (SELECT 1 FROM CRD.BKP_APRT_MIGRADAS_20260827 b WHERE b.APRTCDGO = a.APRTCDGO);
-- COMMIT;

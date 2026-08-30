-- =====================================================================================
-- 65 — RECONSTRUCCION DE LOS APORTES DE JUNIO 2025 (carga 352)
--      + normalizacion de las filas migradas a mano
-- =====================================================================================
-- FECHA: 2026-08-27
-- Contexto: docs/logica-negocio/crd/ANALISIS-APORTES-DUPLICADOS-PETRO.md y los
--           resultados de 61_ANALISIS_APORTES_DUPLICADOS_PETRO.sql (A0 y A2).
--
-- POR QUE EXISTE ESTE SCRIPT
--   La carga 352 (periodo 2025-06, filial 1) descontó $162.004,30 a 2.021 partícipes y
--   NO generó ni una sola fila en CRD.APRT ni en CRD.PGAP. La razón está en la línea de
--   tiempo: la carga se procesó el 2026-03-30 y el generador de aportes de la carga
--   Petro apareció el 2026-04-02 (commit 60b8258). El código todavía no sabía crear
--   aportes.
--   Es decir: el dinero se le descontó a la gente, ASOPREP lo recibió, y no existe como
--   aporte. Esos partícipes tienen un mes menos de ahorro registrado del que pagaron.
--
-- ⚠ POR QUE ES URGENTE, ADEMAS DE JUSTO
--   Con la Fase 4 del plan (la generación cobra el FALTANTE por mes de devengo), junio
--   2025 se ve como un mes incompleto y SE LE VOLVERIA A COBRAR a los 2.021 partícipes.
--   Este script tiene que correr ANTES de habilitar ese camino.
--
-- QUE HACE
--   PARTE A — reconstruye los aportes de junio 2025 desde CRD.PXCA de la carga 352.
--   PARTE B — normaliza al modelo nuevo las filas migradas a mano que quedaron con
--             valorPagado = 0 / saldo > 0. NO TOCA valor: el valor migrado es correcto.
--
-- SOLO EL USUARIO LO EJECUTA. Los pasos 1 y 2 son de lectura.
-- SQL PURO: sin SET / DEFINE / WHENEVER.
-- =====================================================================================


-- =====================================================================================
-- PARTE A — RECONSTRUCCION DE JUNIO 2025
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- A.1  CONTROL BLOQUEANTE: la carga 352 NO debe tener aportes todavía
-- -------------------------------------------------------------------------------------
-- Esperado: 0 filas y 0 pagos. Si sale cualquier otra cosa, el script YA CORRIO o
-- alguien creó aportes a mano: PARAR y revisar antes de insertar nada.
SELECT  (SELECT COUNT(*) FROM CRD.APRT a WHERE a.APRTIDAS = 352)            AS FILAS_APRT,
        (SELECT COUNT(*) FROM CRD.PGAP p
         WHERE p.PGAPCNCP LIKE '%CargaArchivo: 352')                         AS FILAS_PGAP,
        (SELECT COUNT(*) FROM CRD.APRT a
         WHERE a.TPAPCDGO IN (9,11) AND a.APRTVLRR > 0
         AND   TRUNC(a.APRTFCTR,'MM') = DATE '2025-06-01')                   AS APORTES_JUN_2025
FROM    DUAL;

-- Y la carga debe seguir siendo la que creemos: periodo 6/2025, filial 1, estado 3.
SELECT c.CRARCDGO, c.CRARMSAF AS MES, c.CRARANAF AS ANIO, c.FLLLCDGO AS FILIAL,
       c.CRARESTD AS ESTADO, c.CRARFCCR AS FECHA_CARGA
FROM   CRD.CRAR c WHERE c.CRARCDGO = 352;


-- -------------------------------------------------------------------------------------
-- A.2  LO QUE SE VA A INSERTAR — revisar ANTES de ejecutar A.4
-- -------------------------------------------------------------------------------------
-- El reparto replica la prelación del proceso: JUBILACION (9) primero hasta su monto
-- esperado, luego CESANTIA (11) hasta el suyo. Es la misma regla que la Fase 2 del plan.
--
-- Columnas a mirar:
--   DESCONTADO      lo que dice el archivo para ese partícipe
--   MONTO_JUB / MONTO_CES   lo esperado según HSTR estado 99
--   ASIG_JUB / ASIG_CES     lo que se va a registrar
--   REMANENTE       descontado − asignado. Debería ser ~0. Si es > 0 es un excedente
--                   que este script NO reparte: quedaría sin registrar. Revisar cuántos
--                   casos hay y cuánto suman antes de decidir.
WITH LINEAS AS (
        SELECT  x.PXCACDPT               AS ROL,
                x.PXCANMBR               AS NOMBRE_ARCHIVO,
                NVL(x.PXCADSDO, 0)       AS DESCONTADO
        FROM    CRD.DTCA d
        JOIN    CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
        WHERE   d.CRARCDGO = 352
        AND     d.DTCACDPP = 'AH'
        AND     NVL(x.PXCADSDO, 0) > 0.01
),
HS AS (
        SELECT  h.ENTDCDGO,
                NVL(h.HSTRMNAJ,0) AS MONTO_JUB,
                NVL(h.HSTRMNAC,0) AS MONTO_CES
        FROM  ( SELECT h.*, ROW_NUMBER() OVER (PARTITION BY h.ENTDCDGO
                        ORDER BY h.HSTRFCIN DESC, h.HSTRCDGO DESC) rn
                FROM   CRD.HSTR h WHERE h.HSTRESTD = 99 ) h
        WHERE   h.rn = 1
),
BASE AS (
        SELECT  e.ENTDCDGO, e.ENTDNMID, e.ENTDRZNS, e.FLLLCDGO, e.ENTDIDST,
                l.ROL, l.NOMBRE_ARCHIVO, l.DESCONTADO,
                hs.MONTO_JUB, hs.MONTO_CES,
                LEAST(l.DESCONTADO, NVL(hs.MONTO_JUB,0)) AS ASIG_JUB
        FROM    LINEAS l
        JOIN    CRD.ENTD e  ON e.ENTDRLPC = l.ROL
        LEFT    JOIN HS hs  ON hs.ENTDCDGO = e.ENTDCDGO
)
SELECT  b.ENTDNMID AS CEDULA, b.ENTDRZNS AS NOMBRE, b.ROL, b.ENTDIDST AS ESTADO_PART,
        b.DESCONTADO, b.MONTO_JUB, b.MONTO_CES,
        ROUND(b.ASIG_JUB, 2) AS ASIG_JUB,
        ROUND(LEAST(b.DESCONTADO - b.ASIG_JUB, NVL(b.MONTO_CES,0)), 2) AS ASIG_CES,
        ROUND(b.DESCONTADO - b.ASIG_JUB
              - LEAST(b.DESCONTADO - b.ASIG_JUB, NVL(b.MONTO_CES,0)), 2) AS REMANENTE
FROM    BASE b
ORDER BY REMANENTE DESC, b.ENTDNMID;


-- -------------------------------------------------------------------------------------
-- A.3  RESUMEN DEL CONTROL ANTERIOR + LOS CASOS QUE NO SE PUEDEN RECONSTRUIR
-- -------------------------------------------------------------------------------------
-- TOTAL_DESCONTADO debe dar 162.004,30 (lo que reportó A0 del script 61).
-- TOTAL_A_REGISTRAR es lo que este script va a insertar.
-- La diferencia entre ambos son los remanentes + los partícipes sin HSTR.
WITH LINEAS AS (
        SELECT x.PXCACDPT AS ROL, NVL(x.PXCADSDO,0) AS DESCONTADO
        FROM   CRD.DTCA d JOIN CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
        WHERE  d.CRARCDGO = 352 AND d.DTCACDPP = 'AH' AND NVL(x.PXCADSDO,0) > 0.01),
HS AS (SELECT h.ENTDCDGO, NVL(h.HSTRMNAJ,0) AS MONTO_JUB, NVL(h.HSTRMNAC,0) AS MONTO_CES
       FROM ( SELECT h.*, ROW_NUMBER() OVER (PARTITION BY h.ENTDCDGO
                     ORDER BY h.HSTRFCIN DESC, h.HSTRCDGO DESC) rn
              FROM CRD.HSTR h WHERE h.HSTRESTD = 99 ) h WHERE h.rn = 1),
BASE AS (SELECT e.ENTDCDGO, l.DESCONTADO, hs.MONTO_JUB, hs.MONTO_CES,
                LEAST(l.DESCONTADO, NVL(hs.MONTO_JUB,0)) AS ASIG_JUB,
                CASE WHEN hs.ENTDCDGO IS NULL THEN 1 ELSE 0 END AS SIN_HSTR
         FROM LINEAS l JOIN CRD.ENTD e ON e.ENTDRLPC = l.ROL
              LEFT JOIN HS hs ON hs.ENTDCDGO = e.ENTDCDGO)
SELECT  COUNT(*)                                          AS LINEAS_CON_DESCUENTO,
        SUM(b.SIN_HSTR)                                   AS SIN_HSTR_99,
        ROUND(SUM(b.DESCONTADO), 2)                       AS TOTAL_DESCONTADO,
        ROUND(SUM(b.ASIG_JUB), 2)                         AS TOTAL_JUBILACION,
        ROUND(SUM(LEAST(b.DESCONTADO - b.ASIG_JUB, NVL(b.MONTO_CES,0))), 2) AS TOTAL_CESANTIA,
        ROUND(SUM(b.ASIG_JUB + LEAST(b.DESCONTADO - b.ASIG_JUB, NVL(b.MONTO_CES,0))), 2)
                                                          AS TOTAL_A_REGISTRAR,
        ROUND(SUM(b.DESCONTADO - b.ASIG_JUB
              - LEAST(b.DESCONTADO - b.ASIG_JUB, NVL(b.MONTO_CES,0))), 2) AS TOTAL_REMANENTE
FROM    BASE b;

-- Los partícipes que NO tienen HSTR estado 99: no se puede repartir su descuento entre
-- jubilación y cesantía, así que este script NO los inserta. Si la lista es corta,
-- resolverlos a mano; si es larga, decidir una regla antes de continuar.
SELECT  e.ENTDNMID AS CEDULA, e.ENTDRZNS AS NOMBRE, e.ENTDRLPC AS ROL,
        e.ENTDIDST AS ESTADO_PART, NVL(x.PXCADSDO,0) AS DESCONTADO
FROM    CRD.DTCA d
JOIN    CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
JOIN    CRD.ENTD e ON e.ENTDRLPC = x.PXCACDPT
WHERE   d.CRARCDGO = 352 AND d.DTCACDPP = 'AH' AND NVL(x.PXCADSDO,0) > 0.01
AND     NOT EXISTS (SELECT 1 FROM CRD.HSTR h
                    WHERE h.ENTDCDGO = e.ENTDCDGO AND h.HSTRESTD = 99)
ORDER BY DESCONTADO DESC;

-- Roles del archivo que no resuelven a ninguna entidad. Esperado: 0 filas.
SELECT  x.PXCACDPT AS ROL, x.PXCANMBR AS NOMBRE_ARCHIVO, NVL(x.PXCADSDO,0) AS DESCONTADO
FROM    CRD.DTCA d
JOIN    CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
WHERE   d.CRARCDGO = 352 AND d.DTCACDPP = 'AH' AND NVL(x.PXCADSDO,0) > 0.01
AND     NOT EXISTS (SELECT 1 FROM CRD.ENTD e WHERE e.ENTDRLPC = x.PXCACDPT);

-- Roles duplicados: dos entidades con el mismo rolPetroComercial duplicarían el aporte.
-- Esperado: 0 filas. Si sale alguno, PARAR.
SELECT  e.ENTDRLPC AS ROL, COUNT(*) AS ENTIDADES,
        LISTAGG(e.ENTDCDGO || '=' || e.ENTDNMID, ' | ')
            WITHIN GROUP (ORDER BY e.ENTDCDGO) AS DETALLE
FROM    CRD.ENTD e
WHERE   e.ENTDRLPC IN (SELECT x.PXCACDPT FROM CRD.DTCA d
                       JOIN CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
                       WHERE d.CRARCDGO = 352 AND d.DTCACDPP = 'AH')
GROUP BY e.ENTDRLPC HAVING COUNT(*) > 1;


-- -------------------------------------------------------------------------------------
-- A.4  LA INSERCION
-- -------------------------------------------------------------------------------------
-- Las filas nacen YA EN EL MODELO NUEVO: valor = valorPagado = lo recibido, saldo = 0,
-- estado = 4 (PAGADA). Y aprovechan las columnas nuevas: APRTPRDV = 2025-06-01 (mes de
-- devengo) y APRTTPMV = 1 (APORTE_MENSUAL), así que NO necesitan pasar por el backfill
-- del script 63.
--
-- APRTFCTR = 30-jun-2025 23:59:59 — el último día del mes de afectación a las 23:59:59,
-- exactamente el mismo instante que graba crearNuevoAporte. Es la FECHA DE CAJA: es la
-- que lee contabilidad, y es correcta porque ese es el periodo en que entró el dinero.
--
-- La glosa replica el formato de crearNuevoAporte para que estas filas sean
-- indistinguibles de las que habría creado el proceso.
--
-- APRTCDGO no se especifica: la PK de CRD.APRT es IDENTITY.

INSERT INTO CRD.APRT
       (ENTDCDGO, FLLLCDGO, TPAPCDGO, APRTVLRR, APRTVLPG, APRTSLDO, APRTIDST,
        APRTIDAS, APRTFCTR, APRTPRDV, APRTTPMV, APRTGLSA, APRTUSRG, APRTFCRG)
WITH LINEAS AS (
        SELECT x.PXCACDPT AS ROL, NVL(x.PXCADSDO,0) AS DESCONTADO
        FROM   CRD.DTCA d JOIN CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
        WHERE  d.CRARCDGO = 352 AND d.DTCACDPP = 'AH' AND NVL(x.PXCADSDO,0) > 0.01),
HS AS (SELECT h.ENTDCDGO, NVL(h.HSTRMNAJ,0) AS MONTO_JUB, NVL(h.HSTRMNAC,0) AS MONTO_CES
       FROM ( SELECT h.*, ROW_NUMBER() OVER (PARTITION BY h.ENTDCDGO
                     ORDER BY h.HSTRFCIN DESC, h.HSTRCDGO DESC) rn
              FROM CRD.HSTR h WHERE h.HSTRESTD = 99 ) h WHERE h.rn = 1),
BASE AS (
        SELECT  e.ENTDCDGO, e.FLLLCDGO, l.DESCONTADO,
                ROUND(LEAST(l.DESCONTADO, hs.MONTO_JUB), 2) AS ASIG_JUB,
                ROUND(LEAST(l.DESCONTADO - LEAST(l.DESCONTADO, hs.MONTO_JUB),
                            hs.MONTO_CES), 2)               AS ASIG_CES
        FROM    LINEAS l
        JOIN    CRD.ENTD e ON e.ENTDRLPC = l.ROL
        JOIN    HS hs      ON hs.ENTDCDGO = e.ENTDCDGO)     -- JOIN, no LEFT: sin HSTR no se inserta
SELECT  b.ENTDCDGO, b.FLLLCDGO, 9, b.ASIG_JUB, b.ASIG_JUB, 0, 4,
        352,
        TO_TIMESTAMP('2025-06-30 23:59:59', 'YYYY-MM-DD HH24:MI:SS'),
        DATE '2025-06-01',
        1,
        'Aporte Jubilación - Mes 6/2025 - CargaArchivo: 352',
        'SAA_AH',
        SYSTIMESTAMP
FROM    BASE b
WHERE   b.ASIG_JUB > 0.01
UNION ALL
SELECT  b.ENTDCDGO, b.FLLLCDGO, 11, b.ASIG_CES, b.ASIG_CES, 0, 4,
        352,
        TO_TIMESTAMP('2025-06-30 23:59:59', 'YYYY-MM-DD HH24:MI:SS'),
        DATE '2025-06-01',
        1,
        'Aporte Cesantía - Mes 6/2025 - CargaArchivo: 352',
        'SAA_AH',
        SYSTIMESTAMP
FROM    BASE b
WHERE   b.ASIG_CES > 0.01;

COMMIT;


-- -------------------------------------------------------------------------------------
-- A.5  LOS PAGOS (CRD.PGAP)
-- -------------------------------------------------------------------------------------
-- Un PGAP por aporte, con el mismo formato de concepto que crearRegistroPagoAporte, para
-- que las consultas del script 61 (que extraen el id de carga del concepto con REGEXP)
-- sigan cuadrando. Sin esto, la carga 352 quedaría con N filas de APRT y 0 pagos, que es
-- justo la anomalía que el script 61 marca como "fila sin pago".
-- PGAPCDGO tampoco se especifica: también es IDENTITY.

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


-- -------------------------------------------------------------------------------------
-- A.6  VERIFICACION
-- -------------------------------------------------------------------------------------
-- El total registrado debe coincidir con TOTAL_A_REGISTRAR del control A.3.
SELECT  COUNT(*)                                        AS FILAS_CREADAS,
        COUNT(DISTINCT a.ENTDCDGO)                      AS PARTICIPES,
        SUM(CASE WHEN a.TPAPCDGO = 9  THEN a.APRTVLRR ELSE 0 END) AS TOTAL_JUBILACION,
        SUM(CASE WHEN a.TPAPCDGO = 11 THEN a.APRTVLRR ELSE 0 END) AS TOTAL_CESANTIA,
        SUM(a.APRTVLRR)                                 AS TOTAL_REGISTRADO,
        MIN(a.APRTFCTR)                                 AS MIN_FECHA_CAJA,
        MAX(a.APRTFCTR)                                 AS MAX_FECHA_CAJA,
        COUNT(DISTINCT a.APRTPRDV)                      AS PERIODOS_DEVENGO,
        SUM(CASE WHEN a.APRTVLPG <> a.APRTVLRR OR NVL(a.APRTSLDO,0) <> 0
                  OR a.APRTIDST <> 4 THEN 1 ELSE 0 END) AS FILAS_MAL_FORMADAS
FROM    CRD.APRT a WHERE a.APRTIDAS = 352;

-- Un pago por aporte. Esperado: FILAS_SIN_PAGO = 0 y DIFERENCIA = 0.
SELECT  (SELECT COUNT(*) FROM CRD.APRT a WHERE a.APRTIDAS = 352
         AND NOT EXISTS (SELECT 1 FROM CRD.PGAP p WHERE p.APRTCDGO = a.APRTCDGO)) AS FILAS_SIN_PAGO,
        (SELECT ROUND(SUM(a.APRTVLRR),2) FROM CRD.APRT a WHERE a.APRTIDAS = 352)
      - (SELECT ROUND(SUM(p.PGAPVLRR),2) FROM CRD.PGAP p
         JOIN CRD.APRT a ON a.APRTCDGO = p.APRTCDGO WHERE a.APRTIDAS = 352) AS DIFERENCIA
FROM    DUAL;

-- Junio 2025 ya cuenta como mes aportado para el padrón.
SELECT  COUNT(DISTINCT a.ENTDCDGO) AS PARTICIPES_CON_APORTE_JUN_2025
FROM    CRD.APRT a
WHERE   a.TPAPCDGO IN (9,11) AND a.APRTVLRR > 0
AND     TRUNC(a.APRTFCTR,'MM') = DATE '2025-06-01';


-- -------------------------------------------------------------------------------------
-- A.7  REVERSO
-- -------------------------------------------------------------------------------------
-- No hace falta respaldo: nada existía antes, así que revertir es borrar lo insertado.
-- DELETE FROM CRD.PGAP WHERE APRTCDGO IN (SELECT APRTCDGO FROM CRD.APRT WHERE APRTIDAS = 352);
-- DELETE FROM CRD.APRT WHERE APRTIDAS = 352;
-- COMMIT;


-- =====================================================================================
-- PARTE B — NORMALIZAR LAS FILAS MIGRADAS A MANO
-- =====================================================================================
-- Confirmado por el usuario el 2026-08-27: las filas de SAA_JUL_FIN son registros
-- migrados a mano, con el VALOR CORRECTO, a los que simplemente no se les llenó
-- valorPagado.
--
-- Por eso este bloque NO TOCA APRTVLRR. Solo alinea las columnas muertas al modelo
-- nuevo (valorPagado = valor, saldo = 0, estado = PAGADA) para que nadie las lea como
-- deuda pendiente. SUM(valor) no cambia en un solo centavo: cero impacto en saldos,
-- reportes y contabilidad.
--
-- ⚠ Para SAA_UC y SAA_UI el valorPagado SÍ traía un valor parcial. Alinearlo borra el
--   único rastro de ese parcial, que queda preservado en el respaldo. Si prefieres
--   dejarlas como están, quita esos dos usuarios del WHERE del UPDATE.

-- B.1  Qué se va a tocar, por usuario. Revisar antes de ejecutar B.3.
SELECT  NVL(a.APRTUSRG,'(null)') AS USUARIO,
        COUNT(*)                 AS FILAS,
        SUM(a.APRTVLRR)          AS VALOR,
        SUM(NVL(a.APRTVLPG,0))   AS PAGADO_ACTUAL,
        SUM(NVL(a.APRTSLDO,0))   AS SALDO_ACTUAL,
        MIN(a.APRTFCTR)          AS DESDE,
        MAX(a.APRTFCTR)          AS HASTA
FROM    CRD.APRT a
WHERE   NVL(a.APRTSLDO,0) > 0.01
AND     a.APRTVLRR > 0
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
SET     a.APRTVLPG = a.APRTVLRR,
        a.APRTSLDO = 0,
        a.APRTIDST = 4
WHERE   NVL(a.APRTSLDO,0) > 0.01
AND     a.APRTVLRR > 0
AND     a.APRTUSRG IN ('SAA_JUL_FIN', 'SAA_UC', 'SAA_UI');

COMMIT;

-- B.4  Verificación. Esperado: 0 filas con saldo en esos usuarios, y el TOTAL de valor
--      idéntico al del respaldo.
SELECT  (SELECT COUNT(*) FROM CRD.APRT a
         WHERE NVL(a.APRTSLDO,0) > 0.01
         AND   a.APRTUSRG IN ('SAA_JUL_FIN','SAA_UC','SAA_UI'))        AS FILAS_CON_SALDO,
        (SELECT ROUND(SUM(b.APRTVLRR),2) FROM CRD.BKP_APRT_MIGRADAS_20260827 b) AS VALOR_ANTES,
        (SELECT ROUND(SUM(a.APRTVLRR),2) FROM CRD.APRT a
         JOIN  CRD.BKP_APRT_MIGRADAS_20260827 b ON b.APRTCDGO = a.APRTCDGO) AS VALOR_DESPUES
FROM    DUAL;

-- B.5  Reverso
-- UPDATE CRD.APRT a
-- SET   (a.APRTVLPG, a.APRTSLDO, a.APRTIDST) =
--       (SELECT b.APRTVLPG, b.APRTSLDO, b.APRTIDST
--        FROM CRD.BKP_APRT_MIGRADAS_20260827 b WHERE b.APRTCDGO = a.APRTCDGO)
-- WHERE EXISTS (SELECT 1 FROM CRD.BKP_APRT_MIGRADAS_20260827 b WHERE b.APRTCDGO = a.APRTCDGO);
-- COMMIT;

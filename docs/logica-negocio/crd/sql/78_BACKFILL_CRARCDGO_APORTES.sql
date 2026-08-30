-- =====================================================================================
-- BACKFILL — CRD.APRT.CRARCDGO desde APRTIDAS
-- Traslada la trazabilidad de carga historica a la columna gobernada
-- FECHA: 2026-08-29
--
-- =====================================================================================
-- ESTADO: NO EJECUTADO. Se corre DESPUES de DDL-TRAZABILIDAD-CARGA-PETRO.sql
--         (la columna CRARCDGO tiene que existir) y DESPUES del despliegue del WAR
--         (para que las cargas nuevas ya nazcan con las dos columnas llenas).
-- =====================================================================================
--
-- POR QUE EXISTE
--
-- CRD.APRT ya tenia trazabilidad a la carga ANTES de este trabajo: la columna APRTIDAS
-- (Aporte.idAsoprep) guarda el codigo de la CargaArchivo desde crearNuevoAporte, y se
-- deja explicitamente en NULL para los aportes que NO vienen de una carga (ajuste manual,
-- devolucion, reverso). Es la misma semantica que la columna nueva.
--
-- Se decidio (usuario, 2026-08-28) convivir con las dos durante la transicion en vez de
-- elegir una de golpe:
--
--   1. CRARCDGO es la columna GOBERNADA (FK real a CRD.CRAR + indice). Se empieza a
--      llenar en las cargas nuevas.
--   2. APRTIDAS SIGUE SIENDO LA QUE LEE EL ASIENTO por ahora. No se cambia el lector
--      hasta que CRARCDGO este completa y verificada — invertir ese orden dejaria el
--      asiento leyendo una columna a medio llenar.
--   3. Este script traslada lo historico, para que las dos columnas digan lo mismo.
--
-- ⚠ ES UNA COPIA, NO UN "MOVER": APRTIDAS NO SE VACIA.
--
--    Dos consultas VIVAS dependen de APRTIDAS y se romperian si se anulara:
--      - AporteDaoServiceImpl.selectByEntidadTipoYCarga  (b.idAsoprep = :idAsoprep)
--      - AporteDaoServiceImpl.selectAporteAdelantado     (b.idAsoprep <> :idAsoprep)
--    Las dos las usa la carga Petro para decidir si un aporte ya existe o si es un
--    adelanto de otra carga. Vaciar APRTIDAS rompe la carga entera, en silencio.
--
-- ⚠ TRAMPA DE NOMBRES, no confundir:
--    Aporte.idAsoprep    (APRTIDAS) = codigo de la CARGA. Es lo que este script traslada.
--    Prestamo.idAsoprep  (PRSTIDAS) = numero de OPERACION del prestamo en ASOPREP, que
--                                     usan G46, G47, G48, G49 y CCPM, y se valida unico.
--    Mismo nombre de campo, significados sin relacion. Este script NO toca CRD.PRST.
--
-- IDEMPOTENTE: el UPDATE filtra por CRARCDGO IS NULL, asi que una segunda corrida
-- actualiza 0 filas. Seguro de repetir.
--
-- Contenido:
--   0. Controles PREVIOS (leer antes de ejecutar nada)
--   1. Respaldo
--   2. El UPDATE
--   3. Controles POSTERIORES
--   4. Reverso — COMENTADO, no ejecutar
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS — ejecutar y leer ANTES de correr el resto
-- =====================================================================================

-- 0.1 La columna destino existe. Esperado: 1 fila, NULLABLE = 'Y'.
--     Si no aparece, falta correr DDL-TRAZABILIDAD-CARGA-PETRO.sql.
SELECT c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'APRT' AND c.COLUMN_NAME = 'CRARCDGO';

-- 0.2 Cuanto se va a trasladar, y cuanto queda fuera a proposito.
SELECT COUNT(*)                                                             AS APORTES_TOTAL,
       SUM(CASE WHEN a.APRTIDAS IS NOT NULL THEN 1 ELSE 0 END)              AS CON_APRTIDAS,
       SUM(CASE WHEN a.APRTIDAS IS NULL     THEN 1 ELSE 0 END)              AS SIN_APRTIDAS,
       SUM(CASE WHEN a.CRARCDGO IS NOT NULL THEN 1 ELSE 0 END)              AS YA_CON_CRARCDGO,
       SUM(CASE WHEN a.APRTIDAS IS NOT NULL AND a.CRARCDGO IS NULL
                THEN 1 ELSE 0 END)                                          AS A_TRASLADAR
FROM   CRD.APRT a;

-- 0.3 ⚠ CONTROL CRITICO — huerfanos. Esperado: 0 filas.
--     CRARCDGO tiene FK a CRD.CRAR; APRTIDAS no la tiene nunca tuvo. Si alguna fila
--     apunta a una carga que ya no existe, el UPDATE del bloque 2 FALLA ENTERO con
--     ORA-02291. Hay que resolver estas filas ANTES: decidir si se dejan en NULL
--     (perdiendo su trazabilidad) o si la carga se recupera.
SELECT a.APRTIDAS AS CARGA_INEXISTENTE, COUNT(*) AS FILAS,
       MIN(a.APRTFCTR) AS DESDE, MAX(a.APRTFCTR) AS HASTA
FROM   CRD.APRT a
WHERE  a.APRTIDAS IS NOT NULL
AND    NOT EXISTS (SELECT 1 FROM CRD.CRAR c WHERE c.CRARCDGO = a.APRTIDAS)
GROUP  BY a.APRTIDAS ORDER BY FILAS DESC;

-- 0.4 Coherencia: donde las dos columnas ya tienen valor, deben coincidir.
--     Esperado: 0 filas. Si sale algo, alguien esta escribiendo cosas distintas en cada
--     una y hay que entender por que ANTES de seguir.
SELECT a.APRTCDGO, a.APRTIDAS, a.CRARCDGO
FROM   CRD.APRT a
WHERE  a.APRTIDAS IS NOT NULL AND a.CRARCDGO IS NOT NULL
AND    a.APRTIDAS <> a.CRARCDGO;

-- 0.5 Distribucion por carga, informativo: cuantos aportes trae cada archivo.
SELECT a.APRTIDAS AS ID_CARGA,
       c.CRARANAF || '-' || LPAD(c.CRARMSAF, 2, '0') AS PERIODO,
       COUNT(*) AS APORTES, SUM(NVL(a.APRTVLRR, 0)) AS VALOR
FROM   CRD.APRT a
JOIN   CRD.CRAR c ON c.CRARCDGO = a.APRTIDAS
WHERE  a.APRTIDAS IS NOT NULL
GROUP  BY a.APRTIDAS, c.CRARANAF, c.CRARMSAF
ORDER  BY c.CRARANAF, c.CRARMSAF;


-- =====================================================================================
-- 1. RESPALDO
-- =====================================================================================
-- Solo las dos columnas de trazabilidad mas la PK: es lo unico que este script toca, y
-- alcanza para reconstruir el estado previo fila por fila.

CREATE TABLE CRD.BKP_APRT_CRARCDGO_20260829 AS
SELECT a.APRTCDGO, a.APRTIDAS, a.CRARCDGO
FROM   CRD.APRT a
WHERE  a.APRTIDAS IS NOT NULL AND a.CRARCDGO IS NULL;

-- Debe coincidir con A_TRASLADAR del control 0.2.
SELECT COUNT(*) AS FILAS_RESPALDADAS FROM CRD.BKP_APRT_CRARCDGO_20260829;


-- =====================================================================================
-- 2. EL UPDATE — copia APRTIDAS a CRARCDGO
-- =====================================================================================
-- NO se toca APRTIDAS: sigue siendo la columna que lee el asiento y de la que dependen
-- las dos consultas vivas de la carga (ver el encabezado).
--
-- El filtro CRARCDGO IS NULL hace el script idempotente y ademas evita pisar cualquier
-- valor que el codigo nuevo ya haya escrito.

UPDATE CRD.APRT a
SET    a.CRARCDGO = a.APRTIDAS
WHERE  a.APRTIDAS IS NOT NULL
AND    a.CRARCDGO IS NULL
AND    EXISTS (SELECT 1 FROM CRD.CRAR c WHERE c.CRARCDGO = a.APRTIDAS);

-- Debe coincidir con A_TRASLADAR del control 0.2 (menos los huerfanos del 0.3, si se
-- decidio dejarlos fuera).


-- =====================================================================================
-- 3. CONTROLES POSTERIORES — revisar ANTES del COMMIT
-- =====================================================================================

-- 3.1 Ninguna fila con APRTIDAS quedo sin CRARCDGO. Esperado: 0 filas.
--     Si sale algo, son exactamente los huerfanos del control 0.3.
SELECT COUNT(*) AS SIN_TRASLADAR
FROM   CRD.APRT a
WHERE  a.APRTIDAS IS NOT NULL AND a.CRARCDGO IS NULL;

-- 3.2 Las dos columnas dicen lo mismo donde ambas tienen valor. Esperado: 0 filas.
SELECT COUNT(*) AS DISCREPANTES
FROM   CRD.APRT a
WHERE  a.APRTIDAS IS NOT NULL AND a.CRARCDGO IS NOT NULL
AND    a.APRTIDAS <> a.CRARCDGO;

-- 3.3 ⚠ APRTIDAS NO se vacio. Este numero debe ser IGUAL al CON_APRTIDAS del control 0.2.
--     Si bajo, algo vacio la columna y las dos consultas vivas de la carga
--     (selectByEntidadTipoYCarga / selectAporteAdelantado) van a fallar en silencio.
SELECT COUNT(*) AS CON_APRTIDAS_DESPUES
FROM   CRD.APRT a WHERE a.APRTIDAS IS NOT NULL;

-- 3.4 Los aportes que NO vienen de carga siguen intactos en las dos columnas.
--     Esperado: 0 filas (un aporte sin APRTIDAS no puede haber ganado CRARCDGO aca).
SELECT COUNT(*) AS INVENTADOS
FROM   CRD.APRT a
WHERE  a.APRTIDAS IS NULL AND a.CRARCDGO IS NOT NULL
AND    NOT EXISTS (SELECT 1 FROM CRD.BKP_APRT_CRARCDGO_20260829 b
                   WHERE b.APRTCDGO = a.APRTCDGO);

-- Si los cuatro controles pasan:  COMMIT;


-- =====================================================================================
-- 4. ⛔ REVERSO — NO EJECUTAR salvo que haya que deshacer el traslado
-- =====================================================================================
-- Descomentar SOLO si los controles del bloque 3 fallan o si hay que volver atras. Deja
-- CRARCDGO como estaba (NULL) en las filas que este script toco, sin afectar las que ya
-- venian pobladas por el codigo nuevo.
--
-- UPDATE CRD.APRT a
-- SET    a.CRARCDGO = NULL
-- WHERE  EXISTS (SELECT 1 FROM CRD.BKP_APRT_CRARCDGO_20260829 b
--                WHERE b.APRTCDGO = a.APRTCDGO);
-- COMMIT;
--
-- El respaldo se conserva hasta que el traslado este verificado en produccion. Para
-- eliminarlo despues:
-- DROP TABLE CRD.BKP_APRT_CRARCDGO_20260829;

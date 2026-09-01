-- =====================================================================================
-- ⚠️ URGENTE — ¿existe CBCRASRP en ESTA base? + controles 4 y 5 del 102 corregidos
-- FECHA: 2026-08-31 · Equipo A de crd
--
-- ⛔ SOLO LECTURA.
--
-- POR QUE ES URGENTE. El control 5 del script 102 fallo con:
--     ORA-00904: "C"."CBCRASRP": identificador no valido
--
-- **CBCRASRP es el nombre correcto** — esta mapeado en la entidad CobroCredito:181
-- (@JoinColumn(name = "CBCRASRP")). Que Oracle no lo reconozca significa que la columna
-- NO EXISTE en la base contra la que se corrio esa consulta.
--
-- Hay dos explicaciones y hay que saber cual es, YA:
--
--   (A) La sesion del cliente SQL apunta a otra base que aquella donde se corrio el
--       script 100 (local vs produccion). Molesto, pero inofensivo.
--
--   (B) La base donde corre el WAR desplegado NO tiene la columna. **Esto rompe la
--       pantalla de cobros entera**, no solo el asiento nuevo: Hibernate incluye toda
--       columna mapeada en el SELECT que genera, asi que CUALQUIER lectura de CRD.CBCR
--       falla con ORA-00904. Sin relacion aparente con el asiento de reparto.
--
-- ⚠️ CORRER ESTE SCRIPT EN LA MISMA CONEXION DONDE CORRE EL WAR.
-- =====================================================================================


-- 0. ¿En que base y con que usuario estoy? Para no volver a confundir dos conexiones.
SELECT SYS_CONTEXT('USERENV','DB_NAME')     AS BASE,
       SYS_CONTEXT('USERENV','CON_NAME')    AS PDB,
       SYS_CONTEXT('USERENV','SESSION_USER') AS USUARIO
FROM   DUAL;


-- 1. LA CONSULTA QUE DECIDE. Esperado: 1 fila.
--    Si devuelve 0 filas, la columna NO esta y hay que correr el script 100 acá.
SELECT c.OWNER, c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'CBCR'
AND    c.COLUMN_NAME = 'CBCRASRP';


-- 2. Las tres columnas de asiento juntas, para ver cuales si estan. Esperado: 3 filas.
SELECT c.COLUMN_NAME, c.DATA_TYPE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'CBCR'
AND    c.COLUMN_NAME IN ('CBCRASN1','CBCRASN2','CBCRASRP')
ORDER  BY c.COLUMN_NAME;


-- =====================================================================================
-- CONTROLES 4 Y 5 DEL 102, CORREGIDOS
-- Los nombres de columna de CNT.DTAS estaban mal en el 102 (invencion del arbitro).
-- Los reales, leidos de com.saa.model.cnt.DetalleAsiento: DTASDBEE (debe), DTASHBRR
-- (haber), DTASCNTA (numero de cuenta, guardado como texto en el propio detalle).
-- =====================================================================================

-- 3. LA TRANSITORIA TIENE QUE CERRAR EN CERO.
--    Es la prueba de que el asiento de reparto descarga lo que el transitorio cargo.
--    Si da distinto de cero y no hay cobros REGISTRADOS pendientes de procesar, hay un
--    cobro cuyo reparto no se genero o se genero por otro monto.
SELECT SUM(NVL(d.DTASDBEE,0)) - SUM(NVL(d.DTASHBRR,0)) AS SALDO_TRANSITORIA,
       SUM(NVL(d.DTASDBEE,0))                          AS TOTAL_DEBE,
       SUM(NVL(d.DTASHBRR,0))                          AS TOTAL_HABER,
       COUNT(*)                                        AS CUANTAS_LINEAS
FROM   CNT.DTAS d
WHERE  d.DTASCNTA = '2.3.01.15.01';


-- 4. Los cobros y sus tres asientos. Corre SOLO si el control 1 devolvio la columna.
--    Un cobro PROCESADO con CBCRASRP nulo es uno que no genero su asiento de reparto.
SELECT c.CBCRCDGO, c.CBCRESTD, c.CBCRVLOR,
       c.CBCRASN1 AS ASIENTO_TRANSITORIO,
       c.CBCRASRP AS ASIENTO_REPARTO,
       c.CBCRASN2 AS ASIENTO_DEFINITIVO
FROM   CRD.CBCR c
ORDER  BY c.CBCRCDGO DESC
FETCH  FIRST 30 ROWS ONLY;

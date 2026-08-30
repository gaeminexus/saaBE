-- =====================================================================================
-- DDL — TRAZABILIDAD: qué pagos y aportes salieron de cada carga Petro
-- Habilita el asiento de APLICACION (paso 2b) de la Fase 3a
-- FECHA: 2026-08-28
--
-- =====================================================================================
-- ESTADO: NO EJECUTADO. Escrito por el orquestador, pendiente de correr en local.
-- =====================================================================================
--
-- QUE RESUELVE
--
-- El asiento de aplicacion de Petro necesita saber, para la carga que se acaba de
-- procesar: cuanto capital por banda y producto, cuanto interes ordinario, cuanta mora,
-- cuanto seguro de desgravamen, y cuanto de aportes separado en cesantia y jubilacion.
--
-- Todo eso vive en CRD.PGPR (pagos de cuota) y CRD.APRT (aportes) — pero NINGUNA de las
-- dos tablas tiene forma de decir de que carga salio cada fila. Verificado el 2026-08-28
-- sobre las entidades:
--
--   CRD.PGPR  ->  FK a PRST (prestamo), DTPR (cuota) y EVPR (evento). Ninguna a CRAR.
--   CRD.APRT  ->  FK a FLLL, ENTD, CNTR y TPAP. Ninguna a CRAR.
--   CRD.PGAP  ->  liga APRT con PGPR, pero tampoco conoce la carga.
--   CRD.PXCA  ->  cuelga de DTCA (que si pertenece a la carga), pero nada la liga a
--                 PGPR ni a APRT.
--
-- No hay camino indirecto: el link simplemente no existe.
--
-- POR QUE ESTA SOLUCION Y NO ACUMULAR EN EL BUCLE
--
-- La alternativa era hacer que el bucle de aplicarPagosArchivoPetro fuera acumulando los
-- montos por concepto mientras aplica, y armar el asiento al final con esos acumuladores.
-- Se descarto por tres razones:
--
--   1. Obliga a hilvanar acumuladores a traves del metodo mas grande y mas fragil del
--      proyecto — el que CLAUDE.md marca como delicado y el que acaba de dar el
--      STATUS_MARKED_ROLLBACK del 2026-08-28.
--   2. Es fragil hacia adelante: el dia que alguien agregue una rama nueva al bucle y se
--      olvide de acumular, el asiento sale corto Y NO AVISA. Un asiento que cuadra D=H
--      pero por menos plata de la que se movio es el peor error posible aca.
--   3. No se puede recalcular. Con acumuladores, el asiento solo se puede armar DURANTE
--      la carga; si hay que reversarlo y volver a generarlo, o auditar por que dio un
--      numero, hay que reprocesar todo el archivo. Con la columna, es un SELECT.
--
-- BENEFICIO QUE VA MAS ALLA DEL ASIENTO — y es la razon mas fuerte
--
-- Esta es exactamente la columna que falto cuando hubo que analizar los aportes
-- duplicados de Petro (sql/61_ANALISIS_APORTES_DUPLICADOS_PETRO.sql): la pregunta
-- "que aportes entraron con esta carga" hubo que RECONSTRUIRLA por glosa, usuario y
-- fecha, porque el dato no existia. Con CRARCDGO poblado, esa clase entera de analisis
-- pasa a ser una consulta trivial, y una carga reprocesada se detecta al instante en vez
-- de inferirse.
--
-- ALCANCE Y NULOS
--
-- La columna es NULLABLE y se queda NULL en:
--   - todas las filas historicas (no se hace backfill: no hay dato de donde sacarlo, y
--     reconstruirlo por glosa seria inventar);
--   - los pagos y aportes que NO vienen de una carga Petro (pago manual, abono a capital,
--     cruce de valores, ajuste manual, devolucion). Ahi NULL es la respuesta correcta,
--     no un dato faltante.
--
-- Consecuencia: el asiento de aplicacion solo se puede generar para cargas procesadas
-- DESPUES de este cambio. Es lo correcto — las cargas viejas ya se contabilizaron (o no)
-- con el esquema anterior y no se re-contabilizan retroactivamente.
--
-- COSTO EN ORACLE
--
-- ALTER TABLE ... ADD de una columna NULLABLE sin DEFAULT es una operacion de solo
-- metadatos desde Oracle 11g: NO reescribe la tabla y es instantanea, sin importar el
-- volumen de PGPR ni de APRT. No hay ventana de mantenimiento que planificar.
--
-- ORDEN RESPECTO DEL WAR: este DDL va ANTES del despliegue. Las entidades JPA mapean
-- estas columnas y el arranque de WildFly falla si no existen.
--
-- Contenido:
--   0. Controles PREVIOS
--   1. ALTER CRD.PGPR — trazabilidad a la carga
--   2. ALTER CRD.APRT — trazabilidad a la carga
--   3. Controles POSTERIORES
--   4. Consultas que este cambio habilita (informativo)
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS — ejecutar y leer ANTES de correr el resto
-- =====================================================================================

-- 0.1 La columna no debe existir todavia en ninguna de las dos. Esperado: 0 filas.
SELECT c.TABLE_NAME, c.COLUMN_NAME
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME IN ('PGPR', 'APRT')
AND    c.COLUMN_NAME = 'CRARCDGO';

-- 0.2 Volumen de las dos tablas, solo para saber que se esta tocando.
--     (El ALTER es de metadatos, no depende del volumen; esto es informativo.)
SELECT 'PGPR' AS TABLA, COUNT(*) AS FILAS FROM CRD.PGPR
UNION ALL
SELECT 'APRT', COUNT(*) FROM CRD.APRT;

-- 0.3 CRD.CRAR existe y su PK se llama CRARCDGO. Esperado: 1 fila.
SELECT c.TABLE_NAME, c.CONSTRAINT_NAME, cc.COLUMN_NAME
FROM   ALL_CONSTRAINTS c
JOIN   ALL_CONS_COLUMNS cc ON cc.OWNER = c.OWNER AND cc.CONSTRAINT_NAME = c.CONSTRAINT_NAME
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'CRAR' AND c.CONSTRAINT_TYPE = 'P';

-- 0.4 Cargas existentes, para dimensionar desde cuando habra trazabilidad.
--     Todas las cargas ANTERIORES a este cambio quedan sin poder contabilizar la
--     aplicacion, por diseño (ver el encabezado).
SELECT c.CRARESTD, COUNT(*) AS CARGAS,
       MAX(c.CRARANAF || '-' || LPAD(c.CRARMSAF, 2, '0')) AS ULTIMO_PERIODO
FROM   CRD.CRAR c GROUP BY c.CRARESTD ORDER BY 1;


-- =====================================================================================
-- 1. ALTER TABLE: CRD.PGPR — de que carga salio este pago de cuota
-- =====================================================================================
-- Se llama CRARCDGO, igual que la PK de destino: es la convencion de FK del proyecto
-- (CRD.APRT usa ENTDCDGO, FLLLCDGO, TPAPCDGO; CRD.PGPR usa PRSTCDGO, DTPRCDGO, EVPRCDGO).
-- =====================================================================================

ALTER TABLE CRD.PGPR ADD (CRARCDGO NUMBER);

ALTER TABLE CRD.PGPR ADD CONSTRAINT FK_PGPR_CRAR
    FOREIGN KEY (CRARCDGO) REFERENCES CRD.CRAR(CRARCDGO);

-- La consulta del asiento es "todos los pagos de ESTA carga"; el indice la sostiene y
-- ademas evita el bloqueo de tabla padre al borrar una carga.
CREATE INDEX CRD.IDX_PGPR_CARGA ON CRD.PGPR (CRARCDGO);

COMMENT ON COLUMN CRD.PGPR.CRARCDGO IS
    'Carga Petro que genero este pago. NULL = no vino de una carga (pago manual, abono a capital, cruce) o es anterior al 2026-08-28. NO se hizo backfill.';


-- =====================================================================================
-- 2. ALTER TABLE: CRD.APRT — de que carga salio este aporte
-- =====================================================================================
-- OJO: NULL aca NO significa "aporte huerfano". Un aporte de ajuste manual, una fila
-- negativa de devolucion o un contra-movimiento de reverso nacen sin carga y es correcto.
-- Para saber la NATURALEZA del movimiento esta APRTTPMV (rubro 235), que es otra pregunta
-- distinta y ya existe: esta columna responde "de que archivo salio", no "que tipo es".
-- =====================================================================================

ALTER TABLE CRD.APRT ADD (CRARCDGO NUMBER);

ALTER TABLE CRD.APRT ADD CONSTRAINT FK_APRT_CRAR
    FOREIGN KEY (CRARCDGO) REFERENCES CRD.CRAR(CRARCDGO);

CREATE INDEX CRD.IDX_APRT_CARGA ON CRD.APRT (CRARCDGO);

COMMENT ON COLUMN CRD.APRT.CRARCDGO IS
    'Carga Petro que genero este aporte. NULL = no vino de una carga (ajuste manual, devolucion, reverso) o es anterior al 2026-08-28. Distinto de APRTTPMV, que dice la naturaleza del movimiento.';


-- =====================================================================================
-- 3. CONTROLES POSTERIORES
-- =====================================================================================

-- 3.1 Las dos columnas existen y son NUMBER nullable. Esperado: 2 filas, NULLABLE = 'Y'.
SELECT c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME IN ('PGPR', 'APRT')
AND    c.COLUMN_NAME = 'CRARCDGO'
ORDER  BY c.TABLE_NAME;

-- 3.2 Las dos FK existen y estan habilitadas. Esperado: 2 filas, STATUS = 'ENABLED'.
SELECT c.TABLE_NAME, c.CONSTRAINT_NAME, c.STATUS, c.R_CONSTRAINT_NAME
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.CONSTRAINT_NAME IN ('FK_PGPR_CRAR', 'FK_APRT_CRAR');

-- 3.3 Los dos indices quedaron en el schema CRD (no en el del usuario de la sesion).
SELECT i.OWNER, i.TABLE_NAME, i.INDEX_NAME
FROM   ALL_INDEXES i
WHERE  i.TABLE_OWNER = 'CRD' AND i.INDEX_NAME IN ('IDX_PGPR_CARGA', 'IDX_APRT_CARGA');

-- 3.4 Todo arranca en NULL: nadie escribio nada todavia. Esperado: 0 en las dos.
SELECT 'PGPR' AS TABLA, COUNT(*) AS CON_CARGA FROM CRD.PGPR WHERE CRARCDGO IS NOT NULL
UNION ALL
SELECT 'APRT', COUNT(*) FROM CRD.APRT WHERE CRARCDGO IS NOT NULL;


-- =====================================================================================
-- 4. LO QUE ESTE CAMBIO HABILITA — informativo, correr despues de la primera carga nueva
-- =====================================================================================
-- No forman parte del DDL. Quedan escritas aca porque son la razon de ser de la columna
-- y sirven de control cuando pase la primera carga con el codigo nuevo.

-- 4.1 Desglose de una carga por concepto — es la base del asiento de aplicacion.
-- SELECT COUNT(*)                     AS PAGOS,
--        SUM(NVL(p.PGPRCPPG, 0))      AS CAPITAL,
--        SUM(NVL(p.PGPRINPG, 0))      AS INTERES_ORDINARIO,
--        SUM(NVL(p.PGPRMRPG, 0))      AS INTERES_MORA,
--        SUM(NVL(p.PGPRDSGR, 0))      AS SEGURO_DESGRAVAMEN
-- FROM   CRD.PGPR p
-- WHERE  p.CRARCDGO = :idCarga AND NVL(p.PGPRANUL, 0) = 0;

-- 4.2 Aportes de una carga, separados por tipo (9 jubilacion / 11 cesantia).
-- SELECT a.TPAPCDGO, COUNT(*) AS FILAS, SUM(NVL(a.APRTVLRR, 0)) AS VALOR
-- FROM   CRD.APRT a
-- WHERE  a.CRARCDGO = :idCarga
-- GROUP  BY a.TPAPCDGO ORDER BY a.TPAPCDGO;

-- 4.3 Deteccion de carga reprocesada — el analisis que sql/61 tuvo que reconstruir
--     por glosa, usuario y fecha porque este dato no existia.
-- SELECT a.ENTDCDGO, a.TPAPCDGO, a.APRTPRDV, COUNT(DISTINCT a.CRARCDGO) AS CARGAS_DISTINTAS,
--        COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS VALOR
-- FROM   CRD.APRT a
-- WHERE  a.CRARCDGO IS NOT NULL
-- GROUP  BY a.ENTDCDGO, a.TPAPCDGO, a.APRTPRDV
-- HAVING COUNT(*) > 1
-- ORDER  BY VALOR DESC;

-- =====================================================================================
-- CRD.CBCR — columna del ASIENTO DE REPARTO (el tercer asiento del cobro)
-- FECHA: 2026-08-31 · Equipo A de crd
--
-- ⛔ NO CORRER DE CORRIDO. El bloque 0 es solo lectura.
--
-- QUE RESUELVE. Decision del usuario (2026-08-31): **cada cobro genera TRES asientos**, no
-- dos. Hoy genera dos y falta el del medio.
--
--   1. Al REGISTRAR   Banco -> transitoria 2.3.01.15.01        CBCRASN1  (ya existe)
--   2. Al PROCESAR    transitoria -> activo 1.4.05.05/.10      CBCRASRP  (FALTA, es esta)
--   3. Al PROCESAR    pasivo -> desglose de valores            CBCRASN2  (ya existe)
--
-- El asiento 2 es el que hace que **la cuenta transitoria cierre en cero**: el 1 la carga y
-- el 2 la descarga, por el MISMO monto (`cobro.getValor()`, o sea el deposito). Sin el, la
-- transitoria acumula sin techo — que es exactamente el riesgo que el plan de cierre
-- contable marca como bloqueante antes de encender el flag (rubro 237, §5.1: "la cuenta
-- transitoria queda en cero por cada cobro").
--
-- ⚠️ POR QUE UNA COLUMNA NUEVA Y NO REUSAR NINGUNA.
-- `CBCRASN1` guarda el transitorio y `CBCRASN2` el definitivo; los dos se usan y se
-- reversan. Guardar el tercero en cualquiera de las dos perderia la referencia del otro y
-- ese asiento no se podria reversar nunca — el mismo defecto que ya se corrigio en
-- CRD.DVAP con DVAPNMRC.
--
-- ⚠️ EL NOMBRE ES `CBCRASRP`, NO `CBCRASN3`.
-- Dice QUE ES (asiento reparto) en vez de un ordinal — y el ordinal ademas contradice el
-- orden cronologico real, porque el reparto ocurre ANTES que el definitivo.
--
-- ⚠️ ES ASNTCDGO (la PK), NO ASNTNMRO (el correlativo por empresa/periodo).
-- Misma convencion que CBCRASN1/CBCRASN2, que DVAP.DVAPNMRC y que EVPR.EVPRNMAS:
-- AsientoService.anulaAsiento recibe el ID, no el numero.
-- =====================================================================================


-- =====================================================================================
-- BLOQUE 0 — CONTROLES PREVIOS
-- =====================================================================================

-- 0.1 La tabla existe. Esperado: 1 fila.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME = 'CBCR';

-- 0.2 La columna nueva NO debe existir todavia. Esperado: 0 filas.
SELECT c.COLUMN_NAME FROM ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'CBCR' AND c.COLUMN_NAME = 'CBCRASRP';

-- 0.3 Las dos que ya existen, para copiar su forma exacta. Esperado: 2 filas.
SELECT c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'CBCR'
AND    c.COLUMN_NAME IN ('CBCRASN1','CBCRASN2')
ORDER  BY c.COLUMN_NAME;

-- 0.4 Como estan declaradas sus FK, para replicarla igual. Esperado: 2 filas.
SELECT c.CONSTRAINT_NAME, c.STATUS, cc.COLUMN_NAME
FROM   ALL_CONSTRAINTS c
JOIN   ALL_CONS_COLUMNS cc ON cc.OWNER = c.OWNER AND cc.CONSTRAINT_NAME = c.CONSTRAINT_NAME
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'CBCR' AND c.CONSTRAINT_TYPE = 'R'
AND    cc.COLUMN_NAME IN ('CBCRASN1','CBCRASN2');

-- 0.5 Cuantos cobros hay ya, por estado. Los existentes quedan con CBCRASRP en NULL: son
--     anteriores al tercer asiento, NO es un dato faltante. Informativo.
SELECT c.CBCRESTD, COUNT(*) AS CUANTOS FROM CRD.CBCR c
GROUP BY c.CBCRESTD ORDER BY c.CBCRESTD;


-- =====================================================================================
-- 1. LA COLUMNA
-- =====================================================================================

ALTER TABLE CRD.CBCR ADD (CBCRASRP NUMBER);

ALTER TABLE CRD.CBCR ADD CONSTRAINT FK_CBCR_ASRP
    FOREIGN KEY (CBCRASRP) REFERENCES CNT.ASNT(ASNTCDGO);

CREATE INDEX CRD.IDX_CBCR_ASRP ON CRD.CBCR (CBCRASRP);

COMMENT ON COLUMN CRD.CBCR.CBCRASRP IS
    'Asiento de REPARTO del cobro (el 2do de los tres): D cuenta transitoria 2.3.01.15.01 -> H activo 1.4.05.05 aportes / 1.4.05.10 prestamos, por el monto que paso por la transitoria (el deposito). Es el que hace que la transitoria cierre en cero: CBCRASN1 la carga y este la descarga. Es ASNTCDGO (PK), no ASNTNMRO. NULL en los cobros anteriores al 2026-08-31 y en los procesados con el flag de contabilidad apagado.';


-- =====================================================================================
-- 2. CONTROLES POSTERIORES
-- =====================================================================================

-- 2.1 La columna quedo. Esperado: 1 fila, NUMBER, NULLABLE Y.
SELECT c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'CBCR' AND c.COLUMN_NAME = 'CBCRASRP';

-- 2.2 Las TRES columnas de asiento conviven. Esperado: 3 filas.
SELECT c.COLUMN_NAME, c.DATA_TYPE FROM ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'CBCR'
AND    c.COLUMN_NAME IN ('CBCRASN1','CBCRASN2','CBCRASRP')
ORDER  BY c.COLUMN_NAME;

-- 2.3 La FK quedo ENABLED y VALIDATED. Esperado: 1 fila.
SELECT c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.STATUS, c.VALIDATED
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.CONSTRAINT_NAME = 'FK_CBCR_ASRP';

-- 2.4 El indice quedo en CRD, no en el schema de la sesion. Esperado: OWNER = 'CRD'.
--     Un CREATE INDEX sin prefijo queda en el schema del usuario conectado y la tabla se
--     queda sin indice, sin ningun error.
SELECT i.OWNER, i.INDEX_NAME, i.TABLE_NAME, i.STATUS
FROM   ALL_INDEXES i
WHERE  i.TABLE_OWNER = 'CRD' AND i.INDEX_NAME = 'IDX_CBCR_ASRP';


-- =====================================================================================
-- 3. QUE MIRAR DESPUES, CUANDO EL CODIGO ESTE ARRIBA
-- =====================================================================================
-- El control que de verdad prueba que el tercer asiento funciona no es que la columna
-- exista, es que **la transitoria cierre en cero**. Con el flag de contabilidad encendido y
-- unos cuantos cobros procesados:
--
--   SELECT SUM(d.DTASVLDB) - SUM(d.DTASVLHB) AS SALDO_TRANSITORIA
--   FROM   CNT.DTAS d JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
--   WHERE  n.PLNNCNTA = '2.3.01.15.01';
--
-- (verificar los nombres de columna de CNT.DTAS contra la entidad DetalleAsiento antes de
--  correrla — no estan comprobados en este script)
--
-- Si da distinto de cero y no hay cobros REGISTRADOS pendientes de procesar, hay un cobro
-- cuyo asiento de reparto no se genero o se genero por otro monto.


-- =====================================================================================
-- 4. REVERSO — comentado a proposito.
-- =====================================================================================
-- Solo si el WAR con el tercer asiento NO se desplego. Si ya se generaron asientos de
-- reparto, borrar la columna pierde su referencia y quedan sin forma de reversarse.
--
-- DROP INDEX CRD.IDX_CBCR_ASRP;
-- ALTER TABLE CRD.CBCR DROP CONSTRAINT FK_CBCR_ASRP;
-- ALTER TABLE CRD.CBCR DROP COLUMN CBCRASRP;
-- =====================================================================================

-- =====================================================================================
-- DEVOLUCION DE APORTES — columna del asiento de reclasificacion (OPCION C)
-- FECHA: 2026-08-31
--
-- DECISION DEL USUARIO (2026-08-31): opcion C — cada uno contabiliza una mitad.
--
--   CRD, al registrar la devolucion (asiento de RECLASIFICACION, NO toca Banco):
--       D 2.1.01.05.01 / 2.1.02.05.01   (baja el aporte del socio)
--     H   2.3.01.05.01 / 2.3.01.10.01   (nace la obligacion de pagarle)
--
--   CXP, al confirmarse el pago (asiento de PAGO):
--       D 2.3.01.05.01 / 2.3.01.10.01   (se cancela la obligacion)
--     H   Banco
--
-- POR QUE HACE FALTA ESTA COLUMNA, y no alcanza con DVAPNMAS:
-- `DevolucionAporteServiceImpl.aplicarPagado` (linea ~1003) SOBREESCRIBE DVAPNMAS con el
-- asiento que genera CXP. Si el asiento de reclasificacion se guardara ahi, se perderia la
-- referencia en cuanto el pago se confirme — y no se podria reversar NUNCA.
--
-- Son dos asientos distintos de dos modulos distintos: necesitan dos columnas.
--   DVAPNMAS -> asiento de PAGO, lo escribe CXP.        (ya existe, no se toca)
--   DVAPNMRC -> asiento de RECLASIFICACION, lo escribe CRD.  (esta columna)
--
-- ⚠️ ES ASNTCDGO (la PK del asiento), NO ASNTNMRO (el correlativo por empresa/periodo).
-- Misma convencion que DVAPNMAS y que EVPR.EVPRNMAS — decision del arbitro del 2026-08-31:
-- AsientoService.anulaAsiento recibe el ID, no el numero, y toda la mecanica de reverso del
-- sistema esta construida sobre la PK.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS
-- =====================================================================================

-- 0.1 La tabla existe. Esperado: 1 fila.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME = 'DVAP';

-- 0.2 La columna nueva NO debe existir todavia. Esperado: 0 filas.
SELECT c.COLUMN_NAME FROM ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'DVAP' AND c.COLUMN_NAME = 'DVAPNMRC';

-- 0.3 DVAPNMAS si existe y se queda como esta. Esperado: 1 fila.
SELECT c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE FROM ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'DVAP' AND c.COLUMN_NAME = 'DVAPNMAS';

-- 0.4 Cuantas devoluciones hay ya, y en que estados. Las existentes quedan con DVAPNMRC
--     en NULL: son anteriores a que existiera el asiento, no es un dato faltante.
--     Es informativo, no bloquea.
SELECT d.DVAPESTD, COUNT(*) AS CUANTAS
FROM   CRD.DVAP d
GROUP  BY d.DVAPESTD
ORDER  BY d.DVAPESTD;


-- =====================================================================================
-- 1. LA COLUMNA
-- =====================================================================================

ALTER TABLE CRD.DVAP ADD (DVAPNMRC NUMBER);

COMMENT ON COLUMN CRD.DVAP.DVAPNMRC IS
    'Asiento de RECLASIFICACION que genera CRD al registrar la devolucion (D 2.1.01.05.01/2.1.02.05.01 -> H 2.3.01.05.01/2.3.01.10.01). Es ASNTCDGO, la PK de CNT.ASNT, no el correlativo ASNTNMRO. Separada de DVAPNMAS a proposito: esa guarda el asiento de PAGO que escribe CXP y aplicarPagado la sobreescribe. NULL en las devoluciones anteriores al 2026-08-31 y en toda devolucion registrada con la contabilidad de CRD apagada.';

CREATE INDEX CRD.IDX_DVAP_ASNTRECL ON CRD.DVAP (DVAPNMRC);


-- =====================================================================================
-- 2. CONTROLES POSTERIORES
-- =====================================================================================

-- 2.1 La columna quedo. Esperado: 1 fila, DATA_TYPE NUMBER, NULLABLE Y.
SELECT c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'DVAP' AND c.COLUMN_NAME = 'DVAPNMRC';

-- 2.2 El indice quedo en CRD, no en el schema de la sesion. Esperado: 1 fila con
--     OWNER = 'CRD' y STATUS = 'VALID'.
--     (Un CREATE INDEX sin prefijo de schema queda en el schema del usuario conectado y la
--     tabla se queda sin el indice, sin ningun error. Por eso el prefijo CRD. de arriba.)
SELECT i.OWNER, i.INDEX_NAME, i.TABLE_NAME, i.STATUS
FROM   ALL_INDEXES i
WHERE  i.TABLE_OWNER = 'CRD' AND i.INDEX_NAME = 'IDX_DVAP_ASNTRECL';

-- 2.3 Las dos columnas de asiento conviven. Esperado: 2 filas.
SELECT c.COLUMN_NAME, c.DATA_TYPE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'DVAP'
AND    c.COLUMN_NAME IN ('DVAPNMAS','DVAPNMRC')
ORDER  BY c.COLUMN_NAME;


-- =====================================================================================
-- 3. REVERSO — comentado a proposito. Leer antes de descomentar.
-- =====================================================================================
-- Solo tiene sentido si el WAR con el codigo de la reclasificacion NO se desplego. Si ya
-- corrio con contabilidad activa, borrar la columna PIERDE la referencia de los asientos
-- ya generados y quedan imposibles de reversar.
--
-- DROP INDEX CRD.IDX_DVAP_ASNTRECL;
-- ALTER TABLE CRD.DVAP DROP COLUMN DVAPNMRC;
-- =====================================================================================

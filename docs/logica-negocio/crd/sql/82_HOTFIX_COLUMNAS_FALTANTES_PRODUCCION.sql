-- =====================================================================================
-- HOTFIX — columnas mapeadas que faltan en PRODUCCION
-- FECHA: 2026-08-30
--
-- SINTOMA (produccion, 2026-08-30 10:57):
--   ORA-00904: "A1_0"."APRTIDDV": identificador no valido
--   al abrir participe-dash → selectByCriteria de APORTE
--
-- ⚠️ NO ROMPE SOLO ESA PANTALLA. Hibernate incluye TODA columna @Column basica en el
-- SELECT que genera, asi que CUALQUIER lectura de la entidad Aporte falla: participe-dash,
-- consultas de aportes, devoluciones, reportes que pasen por la entidad. Todo lo que lea
-- CRD.APRT por JPA esta caido.
--
-- CAUSA: el WAR desplegado mapea columnas que el DDL no creo. El bloque 3 de
-- ALTER-COBROS-ANULACION-Y-PAGO-DEVOLUCION.sql no llego a ejecutarse en produccion.
--
-- ESTE SCRIPT ES ACUMULATIVO Y SEGURO: el bloque 1 dice exactamente que falta, y solo se
-- ejecuta lo que el bloque 1 marque como ausente.
-- =====================================================================================


-- =====================================================================================
-- 1. DIAGNOSTICO — correr esto PRIMERO y leer la salida
-- =====================================================================================
-- Esperado si todo estuviera bien: 3 filas (APRTIDDV, PGAPFCPG, PGAPRFPG).
-- Cada una que NO aparezca es una que hay que crear en el bloque 2.

SELECT  c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM    ALL_TAB_COLUMNS c
WHERE   c.OWNER = 'CRD'
AND     (   (c.TABLE_NAME = 'APRT' AND c.COLUMN_NAME = 'APRTIDDV')
         OR (c.TABLE_NAME = 'PGAP' AND c.COLUMN_NAME IN ('PGAPFCPG', 'PGAPRFPG')) )
ORDER   BY c.TABLE_NAME, c.COLUMN_NAME;

-- 1.1 Y el barrido completo, por si falta alguna mas que todavia no dio la cara.
--     Es la CONSULTA B de VERIFICACION-ENTIDADES-VS-ESQUEMA-CRD.sql: correla entera.
--     ⚠️ Esa consulta ya habria detectado esto. Al correr el script de verificacion hay
--     que mirar LAS DOS consultas, no solo la primera: la A lista tablas ausentes, la B
--     lista COLUMNAS ausentes — y es la B la que encuentra este tipo de fallo.


-- =====================================================================================
-- 2. LAS COLUMNAS — ejecutar SOLO las que el bloque 1 no haya devuelto
-- =====================================================================================

-- 2.1 CRD.APRT — de que devolucion salio este aporte negativo.
--     ESTA ES LA QUE ESTA ROMPIENDO PRODUCCION AHORA.
ALTER TABLE CRD.APRT ADD (APRTIDDV NUMBER);

ALTER TABLE CRD.APRT ADD CONSTRAINT FK_APRT_DVAP
    FOREIGN KEY (APRTIDDV) REFERENCES CRD.DVAP(DVAPCDGO);

CREATE INDEX CRD.IDX_APRT_DEVOLUCION ON CRD.APRT (APRTIDDV);

COMMENT ON COLUMN CRD.APRT.APRTIDDV IS
    'FK a CRD.DVAP: de que devolucion salio este aporte negativo. NULL en los aportes que no vienen de una devolucion.';


-- 2.2 CRD.PGAP — fecha y referencia reales del pago de la devolucion.
--     Correr solo si el bloque 1 no las devolvio.
ALTER TABLE CRD.PGAP ADD (
    PGAPFCPG DATE,
    PGAPRFPG VARCHAR2(100)
);

COMMENT ON COLUMN CRD.PGAP.PGAPFCPG IS
    'Fecha real del pago, leida de PagoProgramado.fechaRespuesta cuando contabilidad confirma.';
COMMENT ON COLUMN CRD.PGAP.PGAPRFPG IS
    'Referencia bancaria del pago, leida de PagoProgramado.referenciaBanco.';

COMMIT;


-- =====================================================================================
-- 3. CONTROL POSTERIOR
-- =====================================================================================

-- 3.1 Las tres columnas existen y son NULLABLE. Esperado: 3 filas, NULLABLE = 'Y'.
SELECT  c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.NULLABLE
FROM    ALL_TAB_COLUMNS c
WHERE   c.OWNER = 'CRD'
AND     (   (c.TABLE_NAME = 'APRT' AND c.COLUMN_NAME = 'APRTIDDV')
         OR (c.TABLE_NAME = 'PGAP' AND c.COLUMN_NAME IN ('PGAPFCPG', 'PGAPRFPG')) )
ORDER   BY c.TABLE_NAME, c.COLUMN_NAME;

-- 3.2 La FK y el indice. Esperado: 1 fila cada uno, ENABLED / VALID.
SELECT c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.STATUS
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.CONSTRAINT_NAME = 'FK_APRT_DVAP';

SELECT i.INDEX_NAME, i.STATUS
FROM   ALL_INDEXES i
WHERE  i.OWNER = 'CRD' AND i.INDEX_NAME = 'IDX_APRT_DEVOLUCION';

-- 3.3 Lectura real de la entidad, que es la prueba que importa. No debe dar ORA-00904.
SELECT COUNT(*) AS FILAS FROM CRD.APRT a WHERE a.ENTDCDGO = 6394;

-- 3.4 Y volver a correr ENTERO VERIFICACION-ENTIDADES-VS-ESQUEMA-CRD.sql, las DOS
--     consultas, para confirmar que no queda ninguna otra columna mapeada sin crear.

-- =====================================================================================
-- 229 — Qué columnas de CRD.VPPC son obligatorias (NOT NULL)
-- FECHA: 2026-09-16   EQUIPO: omen-saa-1
--
-- ⚠️ ESTE SCRIPT NO ESCRIBE NADA. Dos SELECT, sin parámetros. Se corre de corrido.
--
-- POR QUÉ: jubilar un partícipe reventó con
--   ORA-01400: no se puede realizar una inserción NULL en ("CRD"."VPPC"."VPPCVLSR")
-- porque el formulario manda el valor del seguro vacío como nulo. Ya se corrigió `VPPCVLSR`
-- (el service lo normaliza a 0). Pero **las dos pantallas mandan también `VPPCNMCT` (número de
-- cuotas) en nulo cuando el campo queda vacío**, así que si esa columna fuera NOT NULL el mismo
-- error vuelve con otro nombre. El mapeo JPA no sirve para contestarlo: ninguna de las tres
-- columnas declara `nullable=false` y `VPPCVLSR` sí lo es en producción.
--
-- Diseño: docs/logica-negocio/crd/CORRECCION-VPPC-SEGURO-NULO.md §4
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 200


-- =====================================================================================
-- BLOQUE 1 — Todas las columnas de CRD.VPPC, con su obligatoriedad y su valor por defecto
--
-- NULLABLE = 'N' es obligatoria. Interesan sobre todo VPPCNMCT y VPPCTNPR.
-- =====================================================================================
SELECT  c.COLUMN_ID         AS orden,
        c.COLUMN_NAME       AS columna,
        c.DATA_TYPE         AS tipo,
        c.DATA_LENGTH       AS longitud,
        c.DATA_PRECISION    AS precision_,
        c.DATA_SCALE        AS escala,
        c.NULLABLE          AS admite_nulo,
        c.DATA_DEFAULT      AS valor_por_defecto
FROM    ALL_TAB_COLUMNS c
WHERE   c.OWNER = 'CRD'
AND     c.TABLE_NAME = 'VPPC'
ORDER BY c.COLUMN_ID;


-- =====================================================================================
-- BLOQUE 2 — Cuántas filas tienen hoy nulo en las columnas en discusión
--
-- Si VPPCNMCT admite nulo y además ya hay filas con nulo, el nulo es un valor legítimo
-- («pago mensual fijo, sin número de cuotas») y NO hay que normalizarlo a cero: cambiarlo
-- alteraría el significado del dato.
-- =====================================================================================
SELECT  COUNT(*)                                                   AS filas_totales,
        SUM(CASE WHEN v.VPPCNMCT IS NULL THEN 1 ELSE 0 END)        AS numero_cuotas_nulo,
        SUM(CASE WHEN v.VPPCTNPR IS NULL THEN 1 ELSE 0 END)        AS tiene_prestamo_nulo,
        SUM(CASE WHEN v.VPPCVLSR IS NULL THEN 1 ELSE 0 END)        AS valor_seguro_nulo,
        SUM(CASE WHEN v.VPPCVLSR = 0     THEN 1 ELSE 0 END)        AS valor_seguro_cero
FROM    CRD.VPPC v;

-- ============================================================================
-- Backfill de codSustento (FCTCCSUS) en facturas de compra ya registradas
-- ============================================================================
-- Contexto: docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md #4.2 y #6.
-- Solo SELECTs: este script NO actualiza nada. Sirve para saber a que codigo
-- resolveria cada factura ya registrada. El UPDATE real de backfill se
-- escribe aparte, a mano, a partir de estos numeros -- no lo genera este script.
--
-- REGLA (corregida 2026-08-27; la version anterior de este script usaba una
-- regla equivocada -el sustento por defecto del grupo de producto como caso
-- general- que daba 131 de 131 facturas SIN RESOLVER, porque un mismo grupo
-- mezcla lineas con y sin IVA: "Servicios Basicos" trae 56 lineas con IVA y
-- 96 sin -luz y agua van al 0%-, y ningun codigo por grupo puede representar
-- eso). La regla correcta, verificada contra las 131 facturas activas:
--
--   1) EXCEPCION (se revisa primero): si el grupo de producto con mayor base
--      imponible acumulada en la factura tiene configurado uno de los TRES
--      codigos de excepcion -activo fijo (03/04), inventario (06/07) o
--      reembolso de gasto (08)- en PGS.GRPP.GRPPCSUS, ese codigo gana. Hoy
--      NINGUN grupo tiene excepcion configurada (ver consulta 2 de todas
--      formas, por si cambia), asi que esta rama no aporta nada todavia.
--   2) REGLA BASE (si no hay excepcion): PGS.FCTC.VIVA > 0 -> '01' (credito
--      tributario IVA); si no -> '02' (costo/gasto IR). Mira la FACTURA
--      -documento-, no el grupo -linea-, que es justo lo que el ATS exige
--      (un solo codSustento por comprobante).
--
-- Con esta regla, verificado el 2026-08-27: 103 facturas -> 01, 28 -> 02,
-- CERO sin resolver (131 de 131).
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 1) Cuantas facturas resuelven por cada codSustento con la regla correcta.
-- ----------------------------------------------------------------------------
-- Resultado verificado el 2026-08-27: 103 -> '01', 28 -> '02', 0 sin resolver.
WITH excepcion_por_grupo AS (
    SELECT
        df.factura                      AS id_factura,
        g.grppcsus                      AS sustento_grupo,
        SUM(df.baseimponible)           AS base_acumulada
    FROM PGS.dfcc df
    JOIN PGS.prdp p ON p.id = df.producto
    JOIN PGS.grpp g ON g.grppcdgo = p.grupoproducto
    WHERE g.grppcsus IN ('03','04','06','07','08')  -- solo los 3 casos de excepcion
    GROUP BY df.factura, g.grppcdgo, g.grppcsus
),
excepcion_ganadora AS (
    SELECT id_factura, sustento_grupo,
           ROW_NUMBER() OVER (PARTITION BY id_factura ORDER BY base_acumulada DESC) AS orden
    FROM excepcion_por_grupo
)
SELECT
    NVL(eg.sustento_grupo, CASE WHEN NVL(f.viva, 0) > 0 THEN '01' ELSE '02' END) AS sustento_resuelto,
    COUNT(*)                                                                     AS num_facturas
FROM PGS.fctc f
LEFT JOIN excepcion_ganadora eg ON eg.id_factura = f.id AND eg.orden = 1
WHERE f.estado = 1  -- Estado.ACTIVO
GROUP BY NVL(eg.sustento_grupo, CASE WHEN NVL(f.viva, 0) > 0 THEN '01' ELSE '02' END)
ORDER BY 1;


-- ----------------------------------------------------------------------------
-- 2) Grupos de producto que HOY tienen un codigo de EXCEPCION configurado
--    (03/04/06/07/08), y cuantas facturas/base imponible mueven. Sirve para
--    verificar que la excepcion se está aplicando donde corresponde -activos
--    fijos, inventario, reembolsos- y no se coló en un grupo que en realidad
--    es un gasto corriente (que ya resuelve bien solo con la regla del IVA).
-- ----------------------------------------------------------------------------
-- Verificado el 2026-08-27: 0 filas. Ningun grupo tiene excepcion configurada
-- todavia -coherente con que la regla base ya resuelva el 100%-. Si en el
-- futuro se parametriza alguno (ej. un grupo "Activo Fijo" con GRPPCSUS=03),
-- esta consulta es la forma de verificar el efecto antes de confiar en el
-- backfill.
SELECT
    g.grppcdgo                        AS id_grupo,
    g.grppnmbr                        AS nombre_grupo,
    g.grppcsus                        AS sustento_excepcion,
    COUNT(DISTINCT df.factura)        AS num_facturas_afectadas,
    SUM(df.baseimponible)             AS base_imponible_total
FROM PGS.dfcc df
JOIN PGS.prdp p ON p.id = df.producto
JOIN PGS.grpp g ON g.grppcdgo = p.grupoproducto
JOIN PGS.fctc f ON f.id = df.factura AND f.estado = 1
WHERE g.grppcsus IN ('03','04','06','07','08')
GROUP BY g.grppcdgo, g.grppnmbr, g.grppcsus
ORDER BY num_facturas_afectadas DESC;


-- ----------------------------------------------------------------------------
-- 3) Detalle completo: para cada factura activa, el codSustento que resolveria
--    hoy y por que rama de la regla (EXCEPCION vs BASE-IVA). Es la base para
--    escribir el UPDATE de backfill real.
-- ----------------------------------------------------------------------------
-- Verificado el 2026-08-27: 131 filas, ninguna en blanco. 103 en rama
-- BASE-IVA con VIVA>0 (-> 01), 28 en rama BASE-IVA con VIVA=0 (-> 02), 0 en
-- rama EXCEPCION (no hay grupos con excepcion configurada todavia).
WITH excepcion_por_grupo AS (
    SELECT
        df.factura                      AS id_factura,
        g.grppcsus                      AS sustento_grupo,
        SUM(df.baseimponible)           AS base_acumulada
    FROM PGS.dfcc df
    JOIN PGS.prdp p ON p.id = df.producto
    JOIN PGS.grpp g ON g.grppcdgo = p.grupoproducto
    WHERE g.grppcsus IN ('03','04','06','07','08')
    GROUP BY df.factura, g.grppcdgo, g.grppcsus
),
excepcion_ganadora AS (
    SELECT id_factura, sustento_grupo,
           ROW_NUMBER() OVER (PARTITION BY id_factura ORDER BY base_acumulada DESC) AS orden
    FROM excepcion_por_grupo
)
SELECT
    f.id                                                                        AS id_factura,
    f.numero,
    f.fecha,
    t.ttlrnmbr                                                                  AS proveedor,
    f.viva,
    NVL(eg.sustento_grupo, CASE WHEN NVL(f.viva, 0) > 0 THEN '01' ELSE '02' END) AS sustento_resuelto,
    CASE WHEN eg.sustento_grupo IS NOT NULL THEN 'EXCEPCION_GRUPO' ELSE 'BASE_IVA' END AS rama_regla
FROM PGS.fctc f
JOIN TSR.ttlr t ON t.ttlrcdgo = f.titular
LEFT JOIN excepcion_ganadora eg ON eg.id_factura = f.id AND eg.orden = 1
WHERE f.estado = 1
ORDER BY f.fecha DESC;


-- ----------------------------------------------------------------------------
-- 4) HALLAZGO APARTE (no relacionado con la regla de resolucion; ver el
--    segundo tema de esta ronda): lineas de DetalleFacturaCompra (DFCC) que
--    apuntan a un ProductoPago que ya no existe en PGS.PRDP. Con la regla
--    corregida esto YA NO bloquea la resolucion del sustento (la regla base
--    mira FCTC.VIVA, no las lineas) -verificado arriba: las 5 facturas de
--    este hallazgo (122,159,189,190,191) resuelven igual que cualquier
--    otra-, pero sigue siendo un problema de integridad de datos real,
--    independiente del ATS.
-- ----------------------------------------------------------------------------
SELECT
    df.factura                                    AS id_factura,
    f.numero,
    f.fecha,
    t.ttlrnmbr                                     AS proveedor,
    t.ttlridnt                                     AS identificacion,
    df.producto                                    AS producto_inexistente,
    df.descripcion                                 AS descripcion_linea,
    COUNT(*) OVER (PARTITION BY df.factura)        AS lineas_huerfanas_en_la_factura
FROM PGS.dfcc df
JOIN PGS.fctc f ON f.id = df.factura AND f.estado = 1
JOIN TSR.ttlr t ON t.ttlrcdgo = f.titular
WHERE NOT EXISTS (SELECT 1 FROM PGS.prdp p WHERE p.id = df.producto)
ORDER BY df.factura;

-- =====================================================================================
-- ⛔ EL ASIENTO DE APLICACION REPARTIO $112,30 MAS DE LO QUE ENTRO — carga 449
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ NO ESCRIBE NADA. Solo SELECT.
--
-- LO QUE SE VE EN LOS ASIENTOS (reportado por el usuario):
--
--   REPARTO      (CRE-2026-08-0143)   354.491,37
--     aportes                          116.804,80
--     prestamos                        237.686,57
--
--   APLICACION                         354.603,67   -> $112,30 DE MAS
--     aportes cesantia                  77.688,46
--     aportes jubilacion                39.168,60   (suman 116.857,06 -> +52,26)
--     resto (prestamos y seguros)      237.746,61                    (-> +60,04)
--
--   El asiento de aplicacion CUADRA consigo mismo (debe = haber), pero DEBITA de la
--   transitoria mas de lo que el reparto le acredito. Contablemente eso deja la cuenta
--   transitoria en negativo por $112,30.
--
-- ⛔ SON DOS DEFECTOS DISTINTOS, no uno, y por eso hay que medirlos por separado:
--
--   (A) APORTES, +52,26. Los dos asientos NO cuentan lo mismo:
--       - El REPARTO suma CRD.DTCA.totalDescontado del producto AH — lo que el ARCHIVO dijo.
--       - La APLICACION usa sumValorPorTipoAporteByCarga, que filtra por **APRTIDAS**, NO
--         por CRARCDGO. Esta documentado como TRANSITORIO en AporteDaoServiceImpl:986 hasta
--         que corra el backfill 78_BACKFILL_CRARCDGO_APORTES.sql. Si algun aporte de OTRA
--         carga comparte idAsoprep, o si hay aportes creados por otra via con ese mismo
--         valor, entran en la suma sin pertenecer a esta carga.
--
--   (B) PRESTAMOS, +60,04. Los pagos aplicados superan lo que el archivo descontó para
--       prestamos. Un pago no puede inventar plata, asi que o el archivo trae menos de lo
--       que se aplico (dinero de aportes redirigido a prestamo por afectacion manual), o
--       hay pagos de la carga que no salieron de un descuento de esta carga.
--
-- ⛔ NO TOCAR NADA HASTA TENER ESTOS NUMEROS. Ya se perdio un ciclo hoy corrigiendo el
--    seguro por deduccion y salio peor. Se mide primero.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los cinco bloques.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 240

DEFINE CARGA = 449


-- =====================================================================================
-- BLOQUE 1 — Las dos mitades, lado a lado. Confirma la aritmetica de arriba.
-- =====================================================================================
SELECT  r.APORTES_ARCHIVO,
        a.APORTES_APLICADOS,
        ROUND(a.APORTES_APLICADOS - r.APORTES_ARCHIVO, 2)       AS DIFERENCIA_APORTES,
        r.PRESTAMOS_ARCHIVO,
        p.PRESTAMOS_APLICADOS,
        ROUND(p.PRESTAMOS_APLICADOS - r.PRESTAMOS_ARCHIVO, 2)   AS DIFERENCIA_PRESTAMOS
FROM (
    SELECT  ROUND(NVL(SUM(CASE WHEN d.DTCACDPP = 'AH' THEN d.DTCATTDS END), 0), 2) AS APORTES_ARCHIVO,
            ROUND(NVL(SUM(CASE WHEN d.DTCACDPP <> 'AH' THEN d.DTCATTDS END), 0), 2) AS PRESTAMOS_ARCHIVO
    FROM    CRD.DTCA d
    WHERE   d.CRARCDGO = &CARGA
) r,
(
    SELECT  ROUND(NVL(SUM(NVL(ar.APRTVLRR,0)), 0), 2)           AS APORTES_APLICADOS
    FROM    CRD.APRT ar
    WHERE   ar.APRTIDAS = &CARGA
) a,
(
    SELECT  ROUND(NVL(SUM(NVL(g.PGPRVLRR,0)), 0), 2)            AS PRESTAMOS_APLICADOS
    FROM    CRD.PGPR g
    WHERE   g.CRARCDGO = &CARGA
    AND     NVL(g.PGPRANUL, 0) = 0
) p;


-- =====================================================================================
-- BLOQUE 2 — ⛔ (A) EL FILTRO FLOJO DE APORTES: los dos criterios, comparados
--
-- Si APRTIDAS trae mas que CRARCDGO, ahi esta el +52,26: son aportes que la aplicacion
-- cuenta y no pertenecen a esta carga (o que CRARCDGO todavia no tiene poblado).
--
-- Como leerlo:
--   POR_IDASOPREP > POR_CRARCDGO -> el filtro flojo suma de mas. Es la causa (A).
--   Iguales                      -> el filtro NO es la causa y hay que mirar otra cosa.
--   POR_CRARCDGO menor y con NULOS -> CRARCDGO esta a medio poblar; entonces el filtro
--                                     flojo es correcto HOY y la causa es otra.
-- =====================================================================================
SELECT  ROUND(NVL(SUM(CASE WHEN ar.APRTIDAS = &CARGA THEN NVL(ar.APRTVLRR,0) END), 0), 2) AS POR_IDASOPREP,
        ROUND(NVL(SUM(CASE WHEN ar.CRARCDGO = &CARGA THEN NVL(ar.APRTVLRR,0) END), 0), 2) AS POR_CRARCDGO,
        SUM(CASE WHEN ar.APRTIDAS = &CARGA AND ar.CRARCDGO IS NULL THEN 1 ELSE 0 END)     AS IDAS_SIN_CRAR,
        SUM(CASE WHEN ar.APRTIDAS = &CARGA AND NVL(ar.CRARCDGO,-1) <> &CARGA THEN 1 ELSE 0 END) AS IDAS_CON_OTRA_CARGA
FROM    CRD.APRT ar
WHERE   ar.APRTIDAS = &CARGA
OR      ar.CRARCDGO = &CARGA;


-- =====================================================================================
-- BLOQUE 3 — (A) El detalle: aportes que entran por idAsoprep y NO por carga
--
-- Cada fila es un aporte que el asiento de aplicacion cuenta y que, segun la columna
-- gobernada, no es de esta carga.
-- =====================================================================================
SELECT  ar.APRTCDGO                                             AS ID_APORTE,
        ar.ENTDCDGO                                             AS ID_ENTIDAD,
        ar.TPAPCDGO                                             AS TIPO_APORTE,
        ar.APRTVLRR                                             AS VALOR,
        ar.APRTIDAS                                             AS IDASOPREP,
        ar.CRARCDGO                                             AS CARGA_GOBERNADA,
        ar.APRTTPMV                                             AS TIPO_MOVIMIENTO,
        TO_CHAR(ar.APRTFCRG, 'YYYY-MM-DD')                      AS FECHA_REGISTRO
FROM    CRD.APRT ar
WHERE   ar.APRTIDAS = &CARGA
AND     NVL(ar.CRARCDGO, -1) <> &CARGA
ORDER   BY ar.APRTVLRR DESC
FETCH FIRST 40 ROWS ONLY;


-- =====================================================================================
-- BLOQUE 4 — ⛔ (B) PRESTAMOS: de donde salieron los $60,04 de mas
--
-- Compara, por participe, lo que el archivo descontó para prestamos contra lo que se
-- aplico. Una diferencia positiva es plata aplicada que el archivo no trajo para prestamos.
--
-- Como leerlo:
--   Pocos participes con diferencias grandes -> casos puntuales, mirables uno a uno
--     (tipicamente afectacion manual que mando a prestamo dinero que vino como aporte).
--   Muchos con centavos -> redondeo acumulado, otra conversacion.
-- =====================================================================================
SELECT  x.ROL,
        x.PARTICIPE,
        x.DESCONTADO_PRESTAMOS,
        NVL(pr.APLICADO, 0)                                     AS APLICADO_PRESTAMOS,
        ROUND(NVL(pr.APLICADO,0) - x.DESCONTADO_PRESTAMOS, 2)   AS DIFERENCIA
FROM (
    SELECT  e.ENTDCDGO                                          AS ID_ENTIDAD,
            p.PXCACDPT                                          AS ROL,
            MIN(SUBSTR(p.PXCANMBR,1,30))                        AS PARTICIPE,
            ROUND(SUM(NVL(p.PXCADSDO,0)), 2)                    AS DESCONTADO_PRESTAMOS
    FROM    CRD.PXCA p
    JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
    JOIN    CRD.ENTD e ON e.ENTDRLPC = p.PXCACDPT
    WHERE   d.CRARCDGO = &CARGA
    AND     d.DTCACDPP <> 'AH'
    GROUP   BY e.ENTDCDGO, p.PXCACDPT
) x
LEFT JOIN (
    SELECT  pr2.ENTDCDGO                                        AS ID_ENTIDAD,
            ROUND(SUM(NVL(g.PGPRVLRR,0)), 2)                    AS APLICADO
    FROM    CRD.PGPR g
    JOIN    CRD.PRST pr2 ON pr2.PRSTCDGO = g.PRSTCDGO
    WHERE   g.CRARCDGO = &CARGA AND NVL(g.PGPRANUL,0) = 0
    GROUP   BY pr2.ENTDCDGO
) pr ON pr.ID_ENTIDAD = x.ID_ENTIDAD
WHERE   ABS(NVL(pr.APLICADO,0) - x.DESCONTADO_PRESTAMOS) > 0.01
ORDER   BY DIFERENCIA DESC
FETCH FIRST 40 ROWS ONLY;


-- =====================================================================================
-- BLOQUE 5 — La brecha de PGPR, para confirmar que la invariante SI se cumplio
--
-- Es el bloque 1 del sql/171. Deberia dar BRECHA = 0 en las dos filas: la migracion de la
-- fase 3 impuso que los componentes sumen el valor grabado. Si da 0, el descuadre de HOY
-- NO es el de ayer — es otro, y los bloques 2 y 4 dicen cual.
-- =====================================================================================
SELECT  CASE
            WHEN g.PGPROBSR LIKE 'Afectaci%n manual AVPC%' THEN 'AFECTACION_MANUAL'
            WHEN g.PGPROBSR LIKE 'Pago cuota%'             THEN 'PAGO_NORMAL'
            ELSE 'OTRO'
        END                                                     AS ORIGEN,
        COUNT(*)                                                AS PAGOS,
        ROUND(SUM(NVL(g.PGPRVLRR,0)), 2)                        AS TOTAL_REGISTRADO,
        ROUND(SUM(NVL(g.PGPRMRPG,0)), 2)                        AS MORA_COBRADA,
        ROUND(SUM(NVL(g.PGPRVLRR,0))
              - SUM(NVL(g.PGPRCPPG,0)) - SUM(NVL(g.PGPRINPG,0))
              - SUM(NVL(g.PGPRMRPG,0)) - SUM(NVL(g.PGPRINVP,0))
              - SUM(NVL(g.PGPRDSGR,0)) - SUM(NVL(g.PGPRVLSI,0))
              - SUM(NVL(g.PGPRSLOT,0)), 2)                      AS BRECHA
FROM    CRD.PGPR g
WHERE   g.CRARCDGO = &CARGA
AND     NVL(g.PGPRANUL, 0) = 0
GROUP   BY CASE
            WHEN g.PGPROBSR LIKE 'Afectaci%n manual AVPC%' THEN 'AFECTACION_MANUAL'
            WHEN g.PGPROBSR LIKE 'Pago cuota%'             THEN 'PAGO_NORMAL'
            ELSE 'OTRO'
        END
ORDER   BY BRECHA DESC;


-- =====================================================================================
-- FIN. Pegar la salida de los cinco bloques.
-- =====================================================================================

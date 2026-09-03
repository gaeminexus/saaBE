-- =====================================================================================
-- ⛔ LOS $79,44 QUE QUEDAN: quién recibió más de lo que se le descontó — carga 449
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ NO ESCRIBE NADA. Solo SELECT.
--
-- DÓNDE ESTAMOS:
--   Reparto     354.491,37   (lo que el archivo descontó, DTCA)
--   Aplicación  354.570,81   (lo que se aplicó: PGPR + APRT)
--   Diferencia      79,44
--
--   Venía de 112,30. La correccion de SANCHEZ (-32,86) bajo exactamente eso: 112,30 - 32,86
--   = 79,44. O sea que esa correccion fue correcta y lo que queda es OTRA COSA.
--
-- ⛔ LA HIPOTESIS, y sale de la aritmetica del propio SANCHEZ que no se leyo completa:
--
--   La validacion del tope (commit 438257f) compara AFECTACIONES MANUALES contra lo
--   descontado. Pero el flujo AUTOMATICO aplica ADEMAS, por los productos que no tienen
--   novedad bloqueante.
--
--   SANCHEZ: descontado 406,73. Manuales 439,59 sobre el prestamo 6782. Y ADEMAS 57,79
--   aplicados al prestamo 6786 por el camino automatico. Total aplicado 497,38.
--   497,38 - 439,59 = 57,79 = exactamente su PE. Estaba a la vista y no lo lei.
--
--   Ahora que las manuales bajaron al tope (406,73), el automatico SIGUE sumando sus 57,79
--   encima. Por eso la diferencia no se fue del todo.
--
--   La regla que escribi estaba incompleta: "manual <= descontado". El invariante real es
--   "MANUAL + AUTOMATICO <= descontado".
--
-- ⛔ PERO NO SE TOCA NADA HASTA VER ESTE RESULTADO. Ya hubo cinco diagnosticos mios
--    equivocados en esta jornada, y los dos ultimos aciertos salieron de medir, no de
--    deducir. Si el bloque 1 no suma ~79,44 entre sus filas, la hipotesis esta mal.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los tres bloques.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 240

DEFINE CARGA = 449


-- =====================================================================================
-- BLOQUE 1 — ⛔ EL QUE DECIDE: por participe, descontado vs aplicado (TODO)
--
-- Descontado = todo lo que el archivo le quito, todos los productos.
-- Aplicado   = pagos a prestamos + aportes creados, que es todo lo que el proceso hizo
--              con su plata.
--
-- Como leerlo: la suma de la columna DIFERENCIA de las filas positivas menos las negativas
-- tiene que dar ~79,44. Si da eso, el universo esta completo y estos son todos los casos.
-- =====================================================================================
SELECT  x.ROL,
        x.PARTICIPE,
        x.DESCONTADO,
        NVL(pr.APLICADO_PRESTAMOS, 0)                           AS APLICADO_PRESTAMOS,
        NVL(ap.APLICADO_APORTES, 0)                             AS APLICADO_APORTES,
        ROUND(NVL(pr.APLICADO_PRESTAMOS,0) + NVL(ap.APLICADO_APORTES,0), 2) AS APLICADO_TOTAL,
        ROUND(NVL(pr.APLICADO_PRESTAMOS,0) + NVL(ap.APLICADO_APORTES,0)
              - x.DESCONTADO, 2)                                AS DIFERENCIA
FROM (
    SELECT  e.ENTDCDGO                                          AS ID_ENTIDAD,
            p.PXCACDPT                                          AS ROL,
            MIN(SUBSTR(p.PXCANMBR,1,30))                        AS PARTICIPE,
            ROUND(SUM(NVL(p.PXCADSDO,0)), 2)                    AS DESCONTADO
    FROM    CRD.PXCA p
    JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
    JOIN    CRD.ENTD e ON e.ENTDRLPC = p.PXCACDPT
    WHERE   d.CRARCDGO = &CARGA
    GROUP   BY e.ENTDCDGO, p.PXCACDPT
) x
LEFT JOIN (
    SELECT  pr2.ENTDCDGO                                        AS ID_ENTIDAD,
            ROUND(SUM(NVL(g.PGPRVLRR,0)), 2)                    AS APLICADO_PRESTAMOS
    FROM    CRD.PGPR g
    JOIN    CRD.PRST pr2 ON pr2.PRSTCDGO = g.PRSTCDGO
    WHERE   g.CRARCDGO = &CARGA AND NVL(g.PGPRANUL,0) = 0
    GROUP   BY pr2.ENTDCDGO
) pr ON pr.ID_ENTIDAD = x.ID_ENTIDAD
LEFT JOIN (
    SELECT  ar.ENTDCDGO                                         AS ID_ENTIDAD,
            ROUND(SUM(NVL(ar.APRTVLRR,0)), 2)                   AS APLICADO_APORTES
    FROM    CRD.APRT ar
    WHERE   ar.APRTIDAS = &CARGA
    GROUP   BY ar.ENTDCDGO
) ap ON ap.ID_ENTIDAD = x.ID_ENTIDAD
WHERE   ABS(NVL(pr.APLICADO_PRESTAMOS,0) + NVL(ap.APLICADO_APORTES,0) - x.DESCONTADO) > 0.01
ORDER   BY DIFERENCIA DESC;


-- =====================================================================================
-- BLOQUE 2 — El total, para confirmar que el bloque 1 cubre TODO el descuadre
--
-- SUMA_DIFERENCIAS tiene que dar ~79,44. Si da otra cosa, hay casos que el bloque 1 no
-- esta viendo (participes sin fila PXCA, o aportes sin entidad que empareje) y hay que
-- buscarlos antes de concluir.
-- =====================================================================================
SELECT  COUNT(*)                                                AS PARTICIPES_CON_DIFERENCIA,
        SUM(CASE WHEN t.DIFERENCIA > 0 THEN 1 ELSE 0 END)       AS RECIBIERON_DE_MAS,
        SUM(CASE WHEN t.DIFERENCIA < 0 THEN 1 ELSE 0 END)       AS RECIBIERON_DE_MENOS,
        ROUND(SUM(t.DIFERENCIA), 2)                             AS SUMA_DIFERENCIAS
FROM (
    SELECT  ROUND(NVL(pr.AP,0) + NVL(ap.AA,0) - x.DESCONTADO, 2) AS DIFERENCIA
    FROM (
        SELECT  e.ENTDCDGO AS ID_ENTIDAD, ROUND(SUM(NVL(p.PXCADSDO,0)), 2) AS DESCONTADO
        FROM    CRD.PXCA p
        JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
        JOIN    CRD.ENTD e ON e.ENTDRLPC = p.PXCACDPT
        WHERE   d.CRARCDGO = &CARGA
        GROUP   BY e.ENTDCDGO
    ) x
    LEFT JOIN (
        SELECT  pr2.ENTDCDGO AS ID_ENTIDAD, ROUND(SUM(NVL(g.PGPRVLRR,0)), 2) AS AP
        FROM    CRD.PGPR g
        JOIN    CRD.PRST pr2 ON pr2.PRSTCDGO = g.PRSTCDGO
        WHERE   g.CRARCDGO = &CARGA AND NVL(g.PGPRANUL,0) = 0
        GROUP   BY pr2.ENTDCDGO
    ) pr ON pr.ID_ENTIDAD = x.ID_ENTIDAD
    LEFT JOIN (
        SELECT  ar.ENTDCDGO AS ID_ENTIDAD, ROUND(SUM(NVL(ar.APRTVLRR,0)), 2) AS AA
        FROM    CRD.APRT ar
        WHERE   ar.APRTIDAS = &CARGA
        GROUP   BY ar.ENTDCDGO
    ) ap ON ap.ID_ENTIDAD = x.ID_ENTIDAD
    WHERE   ABS(NVL(pr.AP,0) + NVL(ap.AA,0) - x.DESCONTADO) > 0.01
) t;


-- =====================================================================================
-- BLOQUE 3 — ⛔ LA PRUEBA DE LA HIPOTESIS: manual + automatico en los que recibieron de mas
--
-- Para cada participe con diferencia POSITIVA, cuanto vino de afectacion manual y cuanto
-- del camino automatico. El origen se distingue por el prefijo de la observacion, que el
-- commit e7b76c8 dejo estable justamente para esto.
--
-- Como leerlo:
--   AUTOMATICO > 0 junto a MANUAL cercano al descontado -> hipotesis CONFIRMADA: la
--     validacion topea lo manual pero el automatico aplica encima.
--   Todo MANUAL -> la hipotesis esta mal y el tope no se esta respetando; avisar.
-- =====================================================================================
SELECT  e.ENTDRLPC                                              AS ROL,
        SUBSTR(e.ENTDRZNS,1,30)                                 AS PARTICIPE,
        ROUND(SUM(CASE WHEN g.PGPROBSR LIKE '%AFECTACION_MANUAL%'
                         OR g.PGPROBSR LIKE 'Afectaci%n manual%'
                       THEN NVL(g.PGPRVLRR,0) END), 2)          AS MANUAL,
        ROUND(SUM(CASE WHEN g.PGPROBSR NOT LIKE '%AFECTACION_MANUAL%'
                        AND g.PGPROBSR NOT LIKE 'Afectaci%n manual%'
                       THEN NVL(g.PGPRVLRR,0) END), 2)          AS AUTOMATICO,
        ROUND(SUM(NVL(g.PGPRVLRR,0)), 2)                        AS TOTAL_PRESTAMOS
FROM    CRD.PGPR g
JOIN    CRD.PRST p ON p.PRSTCDGO = g.PRSTCDGO
JOIN    CRD.ENTD e ON e.ENTDCDGO = p.ENTDCDGO
WHERE   g.CRARCDGO = &CARGA
AND     NVL(g.PGPRANUL,0) = 0
AND     e.ENTDRLPC IN (
            SELECT  x.ROL FROM (
                SELECT  e2.ENTDCDGO AS ID_ENTIDAD, p2.PXCACDPT AS ROL,
                        ROUND(SUM(NVL(p2.PXCADSDO,0)), 2) AS DESCONTADO
                FROM    CRD.PXCA p2
                JOIN    CRD.DTCA d2 ON d2.DTCACDGO = p2.DTCACDGO
                JOIN    CRD.ENTD e2 ON e2.ENTDRLPC = p2.PXCACDPT
                WHERE   d2.CRARCDGO = &CARGA
                GROUP   BY e2.ENTDCDGO, p2.PXCACDPT
            ) x
            LEFT JOIN (
                SELECT  pr3.ENTDCDGO AS ID_ENTIDAD, ROUND(SUM(NVL(g3.PGPRVLRR,0)), 2) AS AP
                FROM    CRD.PGPR g3
                JOIN    CRD.PRST pr3 ON pr3.PRSTCDGO = g3.PRSTCDGO
                WHERE   g3.CRARCDGO = &CARGA AND NVL(g3.PGPRANUL,0) = 0
                GROUP   BY pr3.ENTDCDGO
            ) pr3b ON pr3b.ID_ENTIDAD = x.ID_ENTIDAD
            LEFT JOIN (
                SELECT  ar3.ENTDCDGO AS ID_ENTIDAD, ROUND(SUM(NVL(ar3.APRTVLRR,0)), 2) AS AA
                FROM    CRD.APRT ar3
                WHERE   ar3.APRTIDAS = &CARGA
                GROUP   BY ar3.ENTDCDGO
            ) ap3 ON ap3.ID_ENTIDAD = x.ID_ENTIDAD
            WHERE   NVL(pr3b.AP,0) + NVL(ap3.AA,0) - x.DESCONTADO > 0.01
        )
GROUP   BY e.ENTDRLPC, e.ENTDRZNS
ORDER   BY TOTAL_PRESTAMOS DESC;


-- =====================================================================================
-- FIN. Pegar la salida de los tres bloques.
-- =====================================================================================

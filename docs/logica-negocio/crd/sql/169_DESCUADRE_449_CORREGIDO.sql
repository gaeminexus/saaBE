-- =====================================================================================
-- EL DESCUADRE DE LA CARGA 449 — reemplaza los bloques 3 y 4 del script 168
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ NO ESCRIBE NADA. Solo SELECT.
--
-- POR QUE ESTE SCRIPT: dos bloques del 168 estaban mal escritos y son MIOS los dos:
--   * Bloque 3 -> ORA-00937. Mezclaba SUM() con subconsultas escalares sin GROUP BY.
--     Se reescribe con subconsultas en el FROM, que es la forma correcta.
--   * Bloque 4 -> daba "aplicado > descontado" en muchas filas, y no era un hallazgo:
--     comparaba el descuento de UN registro PXCA (un solo producto, p.ej. PH) contra
--     los pagos de TODOS los prestamos de la entidad. Mal planteado. Se compara por
--     ENTIDAD, sumando todos sus productos.
--
-- LO QUE YA QUEDO RESUELTO CON EL 168, y no hace falta volver a mirar:
--   * El asiento de TRANSITORIO **SI existe**: asiento 36, 2026-09-01, $354.491,37.
--     La hipotesis del flag apagado era INCORRECTA — el flag estaba encendido y la
--     carga se autorizo el 2026-09-01 12:25.
--   * Las transferencias suman exactamente $354.491,37: el transitorio cuadra.
--   * El saldo acreedor de ~$2.974.015 de la cuenta transitoria es HISTORICO y anterior
--     a esta carga (ya estaba en -$2.973.328 el 2026-08-31). Esta carga no lo movio: el
--     transitorio la acredita y el reparto la debita por el mismo monto.
--
-- LO QUE QUEDA POR EXPLICAR, y es lo unico:
--
--     REPARTO      354.491,37
--     APLICACION   351.927,95
--     DIFERENCIA     2.563,42
--
--   Pista del script 167: los componentes de los pagos suman 235.070,89
--   (capital 150.939,84 + interes 73.740,69 + mora 0 + desgravamen 9.266,08
--    + incendio 1.124,28) pero PGPRVLRR total da 237.746,62 — hay 2.675,73 en
--   componentes que el pago registra y el asiento no tiene linea para contabilizar.
--   Los candidatos son PGPRSLOT (saldoOtros) y PGPRINVP (interes vencido pagado).
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los cuatro bloques.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 230

DEFINE CARGA = 449


-- =====================================================================================
-- BLOQUE 1 — ⛔ EL DESGLOSE COMPLETO DE LOS PAGOS: donde estan los 2.675,73
--
-- Como leerlo: SIN_DESGLOSAR es la parte del pago que NO esta en ninguno de los
-- componentes que el asiento contabiliza. Si SALDO_OTROS o INTERES_VENCIDO explican esa
-- cifra, ahi esta el descuadre: son componentes que se cobran y no se contabilizan.
-- =====================================================================================
SELECT  COUNT(*)                                            AS PAGOS,
        ROUND(SUM(NVL(g.PGPRVLRR,0)), 2)                    AS TOTAL_REGISTRADO,
        ROUND(SUM(NVL(g.PGPRCPPG,0)), 2)                    AS CAPITAL,
        ROUND(SUM(NVL(g.PGPRINPG,0)), 2)                    AS INTERES,
        ROUND(SUM(NVL(g.PGPRMRPG,0)), 2)                    AS MORA,
        ROUND(SUM(NVL(g.PGPRINVP,0)), 2)                    AS INTERES_VENCIDO,
        ROUND(SUM(NVL(g.PGPRDSGR,0)), 2)                    AS DESGRAVAMEN,
        ROUND(SUM(NVL(g.PGPRVLSI,0)), 2)                    AS SEGURO_INCENDIO,
        ROUND(SUM(NVL(g.PGPRSLOT,0)), 2)                    AS SALDO_OTROS,
        ROUND(SUM(NVL(g.PGPRVLRR,0))
              - SUM(NVL(g.PGPRCPPG,0)) - SUM(NVL(g.PGPRINPG,0))
              - SUM(NVL(g.PGPRMRPG,0)) - SUM(NVL(g.PGPRINVP,0))
              - SUM(NVL(g.PGPRDSGR,0)) - SUM(NVL(g.PGPRVLSI,0))
              - SUM(NVL(g.PGPRSLOT,0)), 2)                  AS SIN_DESGLOSAR
FROM    CRD.PGPR g
WHERE   g.CRARCDGO = &CARGA
AND     NVL(g.PGPRANUL, 0) = 0;


-- =====================================================================================
-- BLOQUE 2 — Recibido vs aplicado, a nivel de TODA la carga (bloque 3 del 168, corregido)
--
-- Escrito con subconsultas en el FROM: sin mezclar agregados con escalares, que es lo
-- que provocaba el ORA-00937.
--
-- Como leerlo: SIN_APLICAR deberia ser 0. Si no lo es, ese es dinero recibido que no
-- llego a ningun destino.
-- =====================================================================================
SELECT  a.TOTAL_ARCHIVO,
        p.TOTAL_PRESTAMOS,
        ap.TOTAL_APORTES,
        ROUND(a.TOTAL_ARCHIVO - p.TOTAL_PRESTAMOS - ap.TOTAL_APORTES, 2) AS SIN_APLICAR
FROM   (SELECT ROUND(NVL(SUM(x.PXCADSDO),0), 2) AS TOTAL_ARCHIVO
          FROM CRD.PXCA x
          JOIN CRD.DTCA d ON d.DTCACDGO = x.DTCACDGO
         WHERE d.CRARCDGO = &CARGA) a,
       (SELECT ROUND(NVL(SUM(g.PGPRVLRR),0), 2) AS TOTAL_PRESTAMOS
          FROM CRD.PGPR g
         WHERE g.CRARCDGO = &CARGA
           AND NVL(g.PGPRANUL,0) = 0) p,
       (SELECT ROUND(NVL(SUM(ar.APRTVLRR),0), 2) AS TOTAL_APORTES
          FROM CRD.APRT ar
         WHERE ar.CRARCDGO = &CARGA) ap;


-- =====================================================================================
-- BLOQUE 3 — Por PARTICIPE, sumando TODOS sus productos (bloque 4 del 168, corregido)
--
-- El del 168 comparaba el descuento de un solo registro PXCA contra los pagos de todos
-- los prestamos de la entidad, y por eso daba "aplicado > descontado". Aca se agrupa por
-- rol Petro y se suman todos los registros del participe.
--
-- Como leerlo: DIFERENCIA > 1 es dinero de ese participe que no llego a destino.
-- Si son pocos participes con diferencias grandes -> casos puntuales, mirables uno a uno.
-- Si son muchos con centavos -> redondeo acumulado, otra conversacion.
-- =====================================================================================
SELECT  x.ROL,
        x.PARTICIPE,
        x.DESCONTADO,
        NVL(pr.APLICADO_PRESTAMOS, 0)                       AS APLICADO_PRESTAMOS,
        NVL(ap.APLICADO_APORTES, 0)                         AS APLICADO_APORTES,
        ROUND(x.DESCONTADO - NVL(pr.APLICADO_PRESTAMOS,0)
                           - NVL(ap.APLICADO_APORTES,0), 2) AS DIFERENCIA
FROM (
    SELECT  e.ENTDCDGO                                      AS ID_ENTIDAD,
            p.PXCACDPT                                      AS ROL,
            MIN(SUBSTR(p.PXCANMBR,1,30))                    AS PARTICIPE,
            ROUND(SUM(NVL(p.PXCADSDO,0)), 2)                AS DESCONTADO
    FROM    CRD.PXCA p
    JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
    JOIN    CRD.ENTD e ON e.ENTDRLPC = p.PXCACDPT
    WHERE   d.CRARCDGO = &CARGA
    GROUP   BY e.ENTDCDGO, p.PXCACDPT
) x
LEFT JOIN (
    SELECT  pr2.ENTDCDGO                                    AS ID_ENTIDAD,
            ROUND(SUM(NVL(g.PGPRVLRR,0)), 2)                AS APLICADO_PRESTAMOS
    FROM    CRD.PGPR g
    JOIN    CRD.PRST pr2 ON pr2.PRSTCDGO = g.PRSTCDGO
    WHERE   g.CRARCDGO = &CARGA AND NVL(g.PGPRANUL,0) = 0
    GROUP   BY pr2.ENTDCDGO
) pr ON pr.ID_ENTIDAD = x.ID_ENTIDAD
LEFT JOIN (
    SELECT  ar.ENTDCDGO                                     AS ID_ENTIDAD,
            ROUND(SUM(NVL(ar.APRTVLRR,0)), 2)               AS APLICADO_APORTES
    FROM    CRD.APRT ar
    WHERE   ar.CRARCDGO = &CARGA
    GROUP   BY ar.ENTDCDGO
) ap ON ap.ID_ENTIDAD = x.ID_ENTIDAD
WHERE   ABS(x.DESCONTADO - NVL(pr.APLICADO_PRESTAMOS,0)
                         - NVL(ap.APLICADO_APORTES,0)) > 1
ORDER   BY DIFERENCIA DESC
FETCH FIRST 40 ROWS ONLY;


-- =====================================================================================
-- BLOQUE 4 — Las 279 cuotas PARCIAL: cuanto les falta en total
--
-- Es la contracara del descuadre: si el dinero no alcanzo para agotar las cuotas, lo que
-- falta aca deberia relacionarse con lo que no se aplico.
-- =====================================================================================
SELECT  COUNT(*)                                            AS CUOTAS_PARCIALES,
        ROUND(SUM(NVL(d.DTPRTTLL,0)), 2)                    AS TOTAL_DE_ESAS_CUOTAS,
        ROUND(SUM(NVL(d.DTPRMRAA,0)), 2)                    AS MORA_INCLUIDA,
        ROUND(SUM(NVL(d.DTPRTTLL,0) - NVL(d.DTPRMRAA,0) - NVL(d.DTPRINVN,0)), 2) AS BASE_SIN_MORA
FROM    CRD.DTPR d
WHERE   d.DTPRCDGO IN (SELECT DISTINCT g.DTPRCDGO FROM CRD.PGPR g
                        WHERE g.CRARCDGO = &CARGA AND NVL(g.PGPRANUL,0) = 0)
AND     d.DTPRESTD = 6;


-- =====================================================================================
-- FIN. Pegar la salida de los cuatro bloques.
-- =====================================================================================

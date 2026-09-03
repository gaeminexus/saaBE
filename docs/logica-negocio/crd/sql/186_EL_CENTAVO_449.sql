-- =====================================================================================
-- ⛔ EL CENTAVO: quien lo tiene y de donde sale — carga 449
-- FECHA: 2026-09-03   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ NO ESCRIBE NADA. Solo SELECT.
--
-- DONDE ESTAMOS:
--   2.906,52  ->  79,44  ->  0,01 EN MENOS (se aplico un centavo menos de lo descontado).
--
-- ⛔ POR QUE sql/184 YA NO LO MUESTRA, y no es que este cuadrado:
--   su filtro es ABS(diferencia) > 0.01, que EXCLUYE justamente el 0.01. Este script usa
--   > 0.001. Es la unica diferencia relevante entre los dos en el bloque 1.
--
-- LA HIPOTESIS (MotorPagoPrestamoServiceImpl:152-165), a confirmar con el bloque 3:
--
--   Los seis saldos de una cuota -desgravamen, mora, interes vencido, interes, capital,
--   seguro- se redondean CADA UNO por separado y recien despues se suman para obtener
--   `totalPendiente`. Sumar seis valores ya redondeados no es lo mismo que redondear la
--   suma: el total puede quedar un centavo corto del pendiente real.
--
--   Y ese total es el techo de lo que se aplica:
--       montoAplicar = min(valorDisponible, saldos.getTotalPendiente())   (linea 374)
--
--   Techo un centavo corto -> se aplica un centavo menos de lo descontado. Da "EN MENOS",
--   que es exactamente el signo observado.
--
--   El `redondear` en si esta bien (BigDecimal HALF_UP, no el Math.round sobre double que
--   rompe en los .005). No es un error de implementacion: es el ORDEN de las operaciones.
--
-- ⛔ PERO ES UNA HIPOTESIS. En esta jornada hubo cinco diagnosticos equivocados por
--    deducir, y los aciertos salieron de medir. Con un centavo sobre 354 mil hay UNA sola
--    fila: el bloque 3 la mira de cerca y decide. Si el residuo de la cuota no es 0,01,
--    la hipotesis esta mal y hay que buscar en otro lado (candidatos en el bloque 4).
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los cuatro bloques.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 240

DEFINE CARGA = 449


-- =====================================================================================
-- BLOQUE 1 — QUIEN. Por participe, descontado vs aplicado, con umbral FINO (> 0.001).
--
-- Esperado: UNA fila (o muy pocas) con DIFERENCIA = -0,01.
-- Anotar el ROL: los bloques 3 y 4 se corren sobre ese.
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
WHERE   ABS(NVL(pr.APLICADO_PRESTAMOS,0) + NVL(ap.APLICADO_APORTES,0) - x.DESCONTADO) > 0.001
ORDER   BY DIFERENCIA;


-- =====================================================================================
-- BLOQUE 2 — Que el bloque 1 cubra TODO el descuadre y no haya casos escondidos.
--
-- SUMA_DIFERENCIAS tiene que dar -0,01. Si da otra cosa, hay compensaciones (uno de mas
-- y otro de menos) y el centavo neto esconde dos errores, no uno.
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
    WHERE   ABS(NVL(pr.AP,0) + NVL(ap.AA,0) - x.DESCONTADO) > 0.001
) t;


-- =====================================================================================
-- BLOQUE 3 — ⛔ EL QUE DECIDE. La cuota del participe del bloque 1, componente por
--            componente, con el residuo de redondeo calculado a la vista.
--
-- ⚠️ PONER ACA EL ROL QUE DEVOLVIO EL BLOQUE 1 antes de correr este bloque.
--
-- COMO LEERLO — la columna RESIDUO_REDONDEO es la prueba:
--
--   RESIDUO = (suma de los seis componentes redondeados) - (redondeo de la suma sin redondear)
--
--   RESIDUO = -0,01  -> HIPOTESIS CONFIRMADA. El techo de la cuota quedo un centavo corto
--                       porque se redondeo seis veces antes de sumar. El arreglo es que el
--                       ULTIMO componente absorba el residuo (residuo = total - suma(otros)),
--                       en vez de redondear cada uno por separado.
--   RESIDUO = 0      -> HIPOTESIS DESCARTADA. El redondeo de la cuota esta limpio y el
--                       centavo se pierde en otro lado: mirar el bloque 4.
-- =====================================================================================
DEFINE ROL_DEL_BLOQUE_1 = 0    -- <<< REEMPLAZAR por el ROL que devolvio el bloque 1

SELECT  d.DTPRCDGO                                              AS CUOTA,
        d.DTPRNMCT                                              AS NRO_CUOTA,
        p.PRSTCDGO                                              AS PRESTAMO,
        ROUND(NVL(d.DTPRSLCP,0), 2)                             AS SALDO_CAPITAL,
        ROUND(NVL(d.DTPRSLIN,0), 2)                             AS SALDO_INTERES,
        ROUND(NVL(d.DTPRSLMR,0), 2)                             AS SALDO_MORA,
        ROUND(NVL(d.DTPRSLIV,0), 2)                             AS SALDO_INT_VENC,
        ROUND(NVL(d.DTPRDSGR,0), 2)                             AS DESGRAVAMEN,
        ROUND(NVL(d.DTPRSLDO,0), 2)                             AS SALDO_TOTAL,
        -- Suma de los seis YA redondeados: lo que hace el motor hoy.
        ROUND(NVL(d.DTPRSLCP,0),2) + ROUND(NVL(d.DTPRSLIN,0),2)
          + ROUND(NVL(d.DTPRSLMR,0),2) + ROUND(NVL(d.DTPRSLIV,0),2)
          + ROUND(NVL(d.DTPRDSGR,0),2)                          AS SUMA_REDONDEADOS,
        -- Redondeo de la suma sin redondear: lo que deberia dar.
        ROUND(NVL(d.DTPRSLCP,0) + NVL(d.DTPRSLIN,0) + NVL(d.DTPRSLMR,0)
              + NVL(d.DTPRSLIV,0) + NVL(d.DTPRDSGR,0), 2)       AS REDONDEO_DE_LA_SUMA,
        -- ⛔ LA COLUMNA QUE DECIDE.
        ROUND(
            (ROUND(NVL(d.DTPRSLCP,0),2) + ROUND(NVL(d.DTPRSLIN,0),2)
             + ROUND(NVL(d.DTPRSLMR,0),2) + ROUND(NVL(d.DTPRSLIV,0),2)
             + ROUND(NVL(d.DTPRDSGR,0),2))
            - ROUND(NVL(d.DTPRSLCP,0) + NVL(d.DTPRSLIN,0) + NVL(d.DTPRSLMR,0)
                    + NVL(d.DTPRSLIV,0) + NVL(d.DTPRDSGR,0), 2)
        , 4)                                                    AS RESIDUO_REDONDEO
FROM    CRD.DTPR d
JOIN    CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
JOIN    CRD.ENTD e ON e.ENTDCDGO = p.ENTDCDGO
WHERE   e.ENTDRLPC = &ROL_DEL_BLOQUE_1
AND     EXISTS (
            SELECT  1 FROM CRD.PGPR g
            WHERE   g.DTPRCDGO = d.DTPRCDGO
            AND     g.CRARCDGO = &CARGA
            AND     NVL(g.PGPRANUL,0) = 0
        )
ORDER   BY p.PRSTCDGO, d.DTPRNMCT;


-- =====================================================================================
-- BLOQUE 4 — LOS OTROS CANDIDATOS, por si el bloque 3 descarta la hipotesis.
--
-- Los pagos del participe, componente por componente, contra el valor total del pago.
-- Si DIFERENCIA_COMPONENTES no es 0, el centavo se pierde al REPARTIR el pago entre los
-- seis destinos (prelacion), no al calcular el techo de la cuota — es otro arreglo, en
-- otro lugar del motor (aplicarPrelacion, no calcularSaldosRealesCuota).
-- =====================================================================================
SELECT  g.PGPRCDGO                                              AS PAGO,
        g.PRSTCDGO                                              AS PRESTAMO,
        g.DTPRCDGO                                              AS CUOTA,
        ROUND(NVL(g.PGPRVLRR,0), 2)                             AS VALOR_PAGO,
        ROUND(NVL(g.PGPRCPPG,0), 2)                             AS CAPITAL,
        ROUND(NVL(g.PGPRINPG,0), 2)                             AS INTERES,
        ROUND(NVL(g.PGPRMRPG,0), 2)                             AS MORA,
        ROUND(NVL(g.PGPRINVP,0), 2)                             AS INT_VENCIDO,
        ROUND(NVL(g.PGPRDSGR,0), 2)                             AS DESGRAVAMEN,
        ROUND(NVL(g.PGPRVLSI,0), 2)                             AS SEGURO_INCENDIO,
        -- ⛔ Tiene que dar 0. Si no, el reparto entre componentes pierde (o inventa) plata.
        ROUND(NVL(g.PGPRVLRR,0)
              - (NVL(g.PGPRCPPG,0) + NVL(g.PGPRINPG,0) + NVL(g.PGPRMRPG,0)
                 + NVL(g.PGPRINVP,0) + NVL(g.PGPRDSGR,0) + NVL(g.PGPRVLSI,0))
        , 4)                                                    AS DIFERENCIA_COMPONENTES
FROM    CRD.PGPR g
JOIN    CRD.PRST p ON p.PRSTCDGO = g.PRSTCDGO
JOIN    CRD.ENTD e ON e.ENTDCDGO = p.ENTDCDGO
WHERE   g.CRARCDGO = &CARGA
AND     NVL(g.PGPRANUL,0) = 0
AND     e.ENTDRLPC = &ROL_DEL_BLOQUE_1
ORDER   BY g.PGPRCDGO;


-- =====================================================================================
-- BLOQUE 4.b — La misma pregunta del 4, pero sobre TODA la carga.
--
-- Barato y vale la pena: si el reparto entre componentes pierde centavos, esto lo muestra
-- de una vez para las 449 en vez de un participe a la vez. Esperado: 0 filas.
-- =====================================================================================
SELECT  COUNT(*)                                                AS PAGOS_DESCUADRADOS,
        ROUND(SUM(NVL(g.PGPRVLRR,0)
              - (NVL(g.PGPRCPPG,0) + NVL(g.PGPRINPG,0) + NVL(g.PGPRMRPG,0)
                 + NVL(g.PGPRINVP,0) + NVL(g.PGPRDSGR,0) + NVL(g.PGPRVLSI,0))), 2)
                                                                AS SUMA_DIFERENCIAS
FROM    CRD.PGPR g
WHERE   g.CRARCDGO = &CARGA
AND     NVL(g.PGPRANUL,0) = 0
AND     ABS(NVL(g.PGPRVLRR,0)
            - (NVL(g.PGPRCPPG,0) + NVL(g.PGPRINPG,0) + NVL(g.PGPRMRPG,0)
               + NVL(g.PGPRINVP,0) + NVL(g.PGPRDSGR,0) + NVL(g.PGPRVLSI,0))) > 0.001;


-- =====================================================================================
-- FIN. Pegar la salida de los bloques 1, 2, 3, 4 y 4.b.
--
-- ORDEN DE LECTURA:
--   1 -> quien tiene el centavo (anotar el ROL y ponerlo en ROL_DEL_BLOQUE_1)
--   2 -> que sea UN error y no dos que se compensan
--   3 -> RESIDUO_REDONDEO = -0,01 confirma la hipotesis del techo de la cuota
--   4 / 4.b -> si el 3 da 0, el centavo se pierde repartiendo el pago entre componentes
-- =====================================================================================

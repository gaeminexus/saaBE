-- =====================================================================================
-- ⛔ EL CENTAVO DE QUEZADA SARANGO (rol 4454): de que lado se pierde
-- FECHA: 2026-09-03   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ NO ESCRIBE NADA. Solo SELECT.
--
-- ⚠️⚠️ LO UNICO QUE HAY QUE TOCAR ES LA LINEA `DEFINE CARGA` DE ABAJO.
--      El rol ya va adentro (4454) — en sql/186 quedo un DEFINE sin reemplazar y por eso
--      los bloques 3 y 4 salieron vacios. Acá no hay ningun paso manual mas.
--
-- LO QUE YA SABEMOS, de sql/186 sobre la carga regenerada:
--
--   Bloque 1  4454 QUEZADA SARANGO ARTURO RENE
--             descontado 472,94 | prestamos 351,93 | aportes 121,00 | total 472,93 | -0,01
--   Bloque 2  UN solo participe, de menos. No hay dos errores que se compensen.
--   Bloque 4b 0 pagos descuadrados en TODA la carga -> el reparto del pago entre los seis
--             componentes (prelacion) esta limpio. El centavo NO se pierde ahi.
--
--   Queda entonces: se aplico un centavo menos de lo que habia para aplicar. La pregunta
--   es de que lado — prestamo o aporte — y por que.
--
--   El aporte es 121,00 EXACTO. Si es un valor fijo, al prestamo le tocaban 351,94 y
--   recibio 351,93.
--
-- HIPOTESIS VIGENTE (MotorPagoPrestamoServiceImpl:152-165 + 374): el techo de la cuota
--   (`totalPendiente`) se arma sumando seis saldos YA redondeados por separado, y puede
--   quedar un centavo corto del pendiente real. Como `montoAplicar = min(disponible,
--   totalPendiente)`, un techo corto aplica de menos.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los cuatro bloques.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 240

-- ⚠️ REEMPLAZAR por el codigo de la carga REGENERADA (ya no es 449).
DEFINE CARGA = 0


-- =====================================================================================
-- BLOQUE 1 — De que lado esta el centavo: que le descontaron, producto por producto.
--
-- Como leerlo: sumar los productos de PRESTAMO y compararlos con los 351,93 aplicados;
-- lo mismo con los de aporte contra los 121,00. El lado que no cierre es el lado del
-- centavo. Total de la columna DESCONTADO = 472,94.
-- =====================================================================================
SELECT  p.PXCACDPT                                              AS ROL,
        SUBSTR(p.PXCANMBR,1,30)                                 AS PARTICIPE,
        p.PXCACDPR                                              AS PRODUCTO,
        ROUND(NVL(p.PXCADSDO,0), 2)                             AS DESCONTADO
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     p.PXCACDPT = 4454
ORDER   BY p.PXCACDPR;


-- =====================================================================================
-- BLOQUE 2 — Los pagos que recibio, con el desglose por componente.
--
-- DIFERENCIA_COMPONENTES tiene que dar 0 en todas (el 4b de sql/186 ya lo dijo para toda
-- la carga; esto lo confirma fila por fila para el).
-- El dato que importa es SUMA de VALOR_PAGO = 351,93.
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
        ROUND(NVL(g.PGPRVLRR,0)
              - (NVL(g.PGPRCPPG,0) + NVL(g.PGPRINPG,0) + NVL(g.PGPRMRPG,0)
                 + NVL(g.PGPRINVP,0) + NVL(g.PGPRDSGR,0) + NVL(g.PGPRVLSI,0))
        , 4)                                                    AS DIFERENCIA_COMPONENTES
FROM    CRD.PGPR g
JOIN    CRD.PRST p ON p.PRSTCDGO = g.PRSTCDGO
JOIN    CRD.ENTD e ON e.ENTDCDGO = p.ENTDCDGO
WHERE   g.CRARCDGO = &CARGA
AND     NVL(g.PGPRANUL,0) = 0
AND     e.ENTDRLPC = 4454
ORDER   BY g.PGPRCDGO;


-- =====================================================================================
-- BLOQUE 3 — ⛔ EL QUE DECIDE. Las cuotas que recibieron pago, con el residuo de
--            redondeo calculado a la vista.
--
-- RESIDUO_REDONDEO = (suma de los componentes YA redondeados)
--                  - (redondeo de la suma sin redondear)
--
--   -0,01  -> HIPOTESIS CONFIRMADA. El techo de la cuota quedo corto por redondear cinco
--             veces antes de sumar. Se arregla en calcularSaldosRealesCuota: que el ultimo
--             componente absorba el residuo, en vez de redondear cada uno por separado.
--    0     -> HIPOTESIS DESCARTADA. El redondeo de la cuota esta limpio y el centavo se
--             pierde antes de llegar al motor — mirar el bloque 4.
-- =====================================================================================
SELECT  d.DTPRCDGO                                              AS CUOTA,
        d.DTPRNMCT                                              AS NRO_CUOTA,
        p.PRSTCDGO                                              AS PRESTAMO,
        ROUND(NVL(d.DTPRSLCP,0), 2)                             AS SALDO_CAPITAL,
        ROUND(NVL(d.DTPRSLIN,0), 2)                             AS SALDO_INTERES,
        ROUND(NVL(d.DTPRSLMR,0), 2)                             AS SALDO_MORA,
        ROUND(NVL(d.DTPRSLIV,0), 2)                             AS SALDO_INT_VENC,
        ROUND(NVL(d.DTPRDSGR,0), 2)                             AS DESGRAVAMEN,
        ROUND(NVL(d.DTPRSLDO,0), 2)                             AS SALDO_TOTAL,
        ROUND(NVL(d.DTPRSLCP,0),2) + ROUND(NVL(d.DTPRSLIN,0),2)
          + ROUND(NVL(d.DTPRSLMR,0),2) + ROUND(NVL(d.DTPRSLIV,0),2)
          + ROUND(NVL(d.DTPRDSGR,0),2)                          AS SUMA_REDONDEADOS,
        ROUND(NVL(d.DTPRSLCP,0) + NVL(d.DTPRSLIN,0) + NVL(d.DTPRSLMR,0)
              + NVL(d.DTPRSLIV,0) + NVL(d.DTPRDSGR,0), 2)       AS REDONDEO_DE_LA_SUMA,
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
WHERE   e.ENTDRLPC = 4454
AND     EXISTS (
            SELECT  1 FROM CRD.PGPR g
            WHERE   g.DTPRCDGO = d.DTPRCDGO
            AND     g.CRARCDGO = &CARGA
            AND     NVL(g.PGPRANUL,0) = 0
        )
ORDER   BY p.PRSTCDGO, d.DTPRNMCT;


-- =====================================================================================
-- BLOQUE 4 — El otro lado: los aportes que se le crearon.
--
-- Si el bloque 3 da RESIDUO 0, el centavo no se perdio en el prestamo — se perdio al
-- decidir cuanto iba a aporte. VALOR debe sumar 121,00; si en el bloque 1 el descontado
-- del producto de aporte fuera 121,01, el centavo esta acá y no en el motor de pagos.
-- =====================================================================================
SELECT  a.APRTCDGO                                              AS APORTE,
        a.TPAPCDGO                                              AS TIPO_APORTE,
        ROUND(NVL(a.APRTVLRR,0), 2)                             AS VALOR,
        a.APRTFCRG                                              AS FECHA_REGISTRO,
        a.APRTPRDV                                              AS PERIODO_DEVENGO
FROM    CRD.APRT a
JOIN    CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
WHERE   a.APRTIDAS = &CARGA
AND     e.ENTDRLPC = 4454
ORDER   BY a.APRTCDGO;


-- =====================================================================================
-- FIN. Pegar los cuatro bloques.
--
-- ORDEN DE LECTURA:
--   1 -> de que lado falta el centavo (prestamo vs aporte)
--   2 -> confirma 351,93 y que los componentes cierran
--   3 -> RESIDUO_REDONDEO decide si es el techo de la cuota
--   4 -> si el 3 da 0, mirar acá: el centavo se fue al decidir cuanto va a aporte
-- =====================================================================================

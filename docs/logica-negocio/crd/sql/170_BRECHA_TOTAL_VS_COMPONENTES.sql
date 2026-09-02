-- =====================================================================================
-- ⛔ LA BRECHA: el total de la cuota vs la suma de sus componentes — carga 449
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ NO ESCRIBE NADA. Un solo SELECT, una sola fila.
--
-- QUE RESPONDE: de que esta hecho el descuadre de $2.906,52 entre lo que se registra
-- como PAGADO y la suma de los componentes de ese pago.
--
-- LA CAUSA ENCONTRADA EN EL CODIGO (calcularSaldosRealesCuota) — dos ramas que calculan
-- el total de forma DISTINTA:
--
--   cuota SIN pagos previos:  totalPendiente = totalBaseCuota(cuota)
--                                            = DTPRTTLL - mora - interes vencido
--   cuota CON pagos previos:  totalPendiente = saldoDesgravamen + saldoInteres
--                                            + saldoCapital + saldoSeguroIncendio
--
-- La primera toma el total de la TABLA; la segunda SUMA los componentes. Si esos dos
-- numeros no coinciden, el pago se graba con el total de la tabla pero los componentes
-- por separado — y ahi nace el descuadre. En la cartera migrada no tienen por que
-- coincidir: DTPRTTLL vino del Excel de migracion, no de sumar sus partes.
--
-- ⛔ POR QUE ESTE DATO DECIDE EL ARREGLO, y no se puede saltear: hay tres formas de
--    cerrar la brecha y COBRAN DISTINTO.
--
--   * Si OTROS_SEGUROS explica la brecha -> hay un componente REAL que el pago no
--     registra. El socio debe ese dinero y hay que agregarlo al pago.
--   * Si la brecha aparece SIN que otros seguros la explique -> DTPRTTLL de la cartera
--     migrada simplemente no cuadra con sus componentes, y hay que decidir QUE MANDA:
--     el total de la tabla o la suma de las partes. Eso cambia cuanto se le cobra a
--     cada socio.
--
--   Elegir mal aca no descuadra un asiento: cobra de mas o de menos.
--
-- COMO LEER EL RESULTADO:
--   BRECHA cerca de 2.906  -> es esto, no hay mas que buscar.
--   BRECHA cerca de 0      -> la causa es otra y hay que seguir mirando. AVISAR.
--   OTROS_SEGUROS > 0      -> ese es el componente que falta registrar.
-- =====================================================================================

SET PAGESIZE 100
SET LINESIZE 240

SELECT  COUNT(*)                                                    AS CUOTAS,
        ROUND(SUM(NVL(d.DTPRTTLL,0)), 2)                            AS TOTAL_DTPRTTLL,
        ROUND(SUM(NVL(d.DTPRMRAA,0)), 2)                            AS MORA,
        ROUND(SUM(NVL(d.DTPRINVN,0)), 2)                            AS INTERES_VENCIDO,
        ROUND(SUM(NVL(d.DTPRCPTL,0)), 2)                            AS CAPITAL,
        ROUND(SUM(NVL(d.DTPRINTR,0)), 2)                            AS INTERES,
        ROUND(SUM(NVL(d.DTPRDSGR,0)), 2)                            AS DESGRAVAMEN,
        ROUND(SUM(NVL(d.DTPRVLSI,0)), 2)                            AS SEGURO_INCENDIO,
        ROUND(SUM(NVL(d.DTPROTSG,0)), 2)                            AS OTROS_SEGUROS,
        ROUND(SUM(NVL(d.DTPRTTLL,0) - NVL(d.DTPRMRAA,0) - NVL(d.DTPRINVN,0))
            - SUM(NVL(d.DTPRCPTL,0) + NVL(d.DTPRINTR,0)
                + NVL(d.DTPRDSGR,0) + NVL(d.DTPRVLSI,0)), 2)        AS BRECHA
FROM    CRD.DTPR d
WHERE   d.DTPRCDGO IN (SELECT DISTINCT g.DTPRCDGO FROM CRD.PGPR g
                        WHERE g.CRARCDGO = 449 AND NVL(g.PGPRANUL,0) = 0);


-- =====================================================================================
-- SI LA BRECHA APARECE, ESTO DICE EN QUE CUOTAS ESTA (opcional, para mirar casos)
-- =====================================================================================
SELECT  d.PRSTCDGO                                                  AS PRESTAMO,
        d.DTPRNMCT                                                  AS NRO_CUOTA,
        d.DTPRTTLL                                                  AS TOTAL,
        NVL(d.DTPRMRAA,0)                                           AS MORA,
        NVL(d.DTPRCPTL,0)                                           AS CAPITAL,
        NVL(d.DTPRINTR,0)                                           AS INTERES,
        NVL(d.DTPRDSGR,0)                                           AS DESGRAVAMEN,
        NVL(d.DTPRVLSI,0)                                           AS SEGURO,
        NVL(d.DTPROTSG,0)                                           AS OTROS_SEGUROS,
        ROUND(NVL(d.DTPRTTLL,0) - NVL(d.DTPRMRAA,0) - NVL(d.DTPRINVN,0)
              - NVL(d.DTPRCPTL,0) - NVL(d.DTPRINTR,0)
              - NVL(d.DTPRDSGR,0) - NVL(d.DTPRVLSI,0), 2)           AS BRECHA_CUOTA
FROM    CRD.DTPR d
WHERE   d.DTPRCDGO IN (SELECT DISTINCT g.DTPRCDGO FROM CRD.PGPR g
                        WHERE g.CRARCDGO = 449 AND NVL(g.PGPRANUL,0) = 0)
AND     ABS(NVL(d.DTPRTTLL,0) - NVL(d.DTPRMRAA,0) - NVL(d.DTPRINVN,0)
            - NVL(d.DTPRCPTL,0) - NVL(d.DTPRINTR,0)
            - NVL(d.DTPRDSGR,0) - NVL(d.DTPRVLSI,0)) > 0.01
ORDER   BY BRECHA_CUOTA DESC
FETCH FIRST 30 ROWS ONLY;

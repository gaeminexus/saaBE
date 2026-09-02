-- =====================================================================================
-- ASIENTOS DE CARGA PETRO CON FECHA DESFASADA — cuantos quedaron mal y por cuanto
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ NO ESCRIBE NADA. Solo SELECT. Se puede correr en cualquier momento.
--
-- EL DEFECTO, reportado por el usuario el 2026-09-02 y corregido en el commit f94532b
-- (SIN DESPLEGAR todavia):
--
--   Los asientos de REPARTO y APLICACION de la carga Petro se generaban con
--   `LocalDate.now()` — la fecha en que se corria el proceso — en vez de la
--   FECHA DE AUTORIZACION DE CONTABILIDAD de la carga (CRD.CRAR.CRARFCAC).
--
--   Como la carga se autoriza un dia y se procesa otro, el asiento podia caer en un
--   PERIODO CONTABLE distinto del que le corresponde. Y con un procesamiento que tarda
--   ~22 minutos, incluso podia cruzar la medianoche.
--
-- QUE RESPONDE ESTE SCRIPT: cuantos asientos ya generados tienen la fecha equivocada,
-- de que cargas, y con cuantos dias de desfase. Es el insumo para decidir si hay que
-- corregirlos — decision del usuario y de contabilidad, NO de este equipo.
--
-- ⚠️ EL ASIENTO DE TRANSITORIO (subproceso 1) NO ENTRA EN ESTO. Ese usa la fecha mas
--    reciente de las transferencias registradas —cuando llego el dinero al banco— y eso
--    es correcto por diseño: transferencia, autorizacion y procesamiento son tres
--    momentos distintos. Se lista aparte, solo para tenerlo a la vista.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los cuatro bloques.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 230


-- =====================================================================================
-- BLOQUE 1 — LA MEDICION: asientos de REPARTO y APLICACION contra la autorizacion
--
-- Como leerlo: DIAS_DESFASE es la diferencia entre la fecha con la que quedo el asiento
-- y la fecha de autorizacion de contabilidad de su carga.
--   * 0            -> quedo bien (se proceso el mismo dia que se autorizo).
--   * > 0          -> el asiento quedo DESPUES de lo que corresponde, por esos dias.
--   * MES_DISTINTO -> lo que de verdad importa: el asiento cayo en OTRO MES contable.
--                     Esos son los que hay que decidir si se corrigen.
-- =====================================================================================
SELECT  a.ANCPCDGO                                          AS ID_REGISTRO,
        c.CRARCDGO                                          AS CARGA,
        CASE a.ANCPTPOO
            WHEN 1 THEN 'TRANSITORIO'
            WHEN 2 THEN 'REPARTO'
            WHEN 3 THEN 'APLICACION'
            ELSE 'OTRO (' || TO_CHAR(a.ANCPTPOO) || ')'
        END                                                 AS SUBPROCESO,
        a.ANCPNMAS                                          AS NRO_ASIENTO,
        TO_CHAR(a.ANCPFCHA, 'YYYY-MM-DD')                   AS FECHA_ASIENTO,
        TO_CHAR(c.CRARFCAC, 'YYYY-MM-DD')                   AS FECHA_AUTORIZACION,
        TRUNC(a.ANCPFCHA) - TRUNC(c.CRARFCAC)               AS DIAS_DESFASE,
        CASE WHEN TO_CHAR(a.ANCPFCHA,'YYYYMM') <> TO_CHAR(c.CRARFCAC,'YYYYMM')
             THEN 'MES DISTINTO' ELSE 'mismo mes' END       AS PERIODO,
        a.ANCPVLRR                                          AS VALOR,
        a.ANCPIDST                                          AS ESTADO
FROM    CRD.ANCP a
JOIN    CRD.CRAR c ON c.CRARCDGO = a.CRARCDGO
WHERE   a.ANCPTPOO IN (2, 3)
AND     c.CRARFCAC IS NOT NULL
ORDER   BY PERIODO DESC, DIAS_DESFASE DESC, a.ANCPCDGO;


-- =====================================================================================
-- BLOQUE 2 — El resumen de una linea: cuanto hay que decidir
--
-- Como leerlo: DESFASADOS_MES es el numero que importa — asientos que quedaron en un
-- periodo contable distinto del que les corresponde, y su monto.
-- =====================================================================================
SELECT  COUNT(*)                                            AS ASIENTOS_TOTALES,
        SUM(CASE WHEN TRUNC(a.ANCPFCHA) = TRUNC(c.CRARFCAC)
                 THEN 1 ELSE 0 END)                         AS CORRECTOS,
        SUM(CASE WHEN TRUNC(a.ANCPFCHA) <> TRUNC(c.CRARFCAC)
                 THEN 1 ELSE 0 END)                         AS DESFASADOS_DIA,
        SUM(CASE WHEN TO_CHAR(a.ANCPFCHA,'YYYYMM') <> TO_CHAR(c.CRARFCAC,'YYYYMM')
                 THEN 1 ELSE 0 END)                         AS DESFASADOS_MES,
        ROUND(SUM(CASE WHEN TO_CHAR(a.ANCPFCHA,'YYYYMM') <> TO_CHAR(c.CRARFCAC,'YYYYMM')
                       THEN NVL(a.ANCPVLRR,0) ELSE 0 END), 2) AS MONTO_DESFASADO_MES
FROM    CRD.ANCP a
JOIN    CRD.CRAR c ON c.CRARCDGO = a.CRARCDGO
WHERE   a.ANCPTPOO IN (2, 3)
AND     c.CRARFCAC IS NOT NULL;


-- =====================================================================================
-- BLOQUE 3 — El asiento de TRANSITORIO, solo para tenerlo a la vista
--
-- Este NO se corrigio y probablemente NO haya que corregirlo: usa la fecha mas reciente
-- de las transferencias (cuando llego el dinero al banco), que es un momento distinto
-- de la autorizacion. Se lista para que el usuario confirme que ese criterio es el que
-- quiere, ahora que los otros dos cambian.
-- =====================================================================================
SELECT  a.ANCPCDGO                                          AS ID_REGISTRO,
        c.CRARCDGO                                          AS CARGA,
        a.ANCPNMAS                                          AS NRO_ASIENTO,
        TO_CHAR(a.ANCPFCHA, 'YYYY-MM-DD')                   AS FECHA_ASIENTO,
        TO_CHAR(c.CRARFCAC, 'YYYY-MM-DD')                   AS FECHA_AUTORIZACION,
        TRUNC(a.ANCPFCHA) - TRUNC(c.CRARFCAC)               AS DIAS_VS_AUTORIZACION,
        a.ANCPVLRR                                          AS VALOR
FROM    CRD.ANCP a
JOIN    CRD.CRAR c ON c.CRARCDGO = a.CRARCDGO
WHERE   a.ANCPTPOO = 1
ORDER   BY a.ANCPCDGO DESC
FETCH FIRST 20 ROWS ONLY;


-- =====================================================================================
-- BLOQUE 4 — El detalle del asiento en contabilidad, para los desfasados de MES
--
-- Antes de decidir si se corrigen hay que saber en que estado estan del lado contable:
-- un asiento ya mayorizado o en un periodo cerrado NO se re-fecha, se corrige con otro
-- asiento. Este bloque da el dato para esa conversacion.
--
-- Como leerlo: si ESTADO_ASIENTO indica mayorizado/cerrado, la correccion NO es cambiar
-- la fecha: hay que hacerlo con un asiento de ajuste. AVISAR a contabilidad.
-- =====================================================================================
SELECT  a.ANCPCDGO                                          AS ID_REGISTRO,
        c.CRARCDGO                                          AS CARGA,
        a.ANCPTPOO                                          AS SUBPROCESO,
        asn.ASNTCDGO                                        AS ID_ASIENTO,
        asn.ASNTNMRO                                        AS NUMERO,
        TO_CHAR(asn.ASNTFCHA, 'YYYY-MM-DD')                 AS FECHA_EN_CNT,
        asn.ASNTESTD                                        AS ESTADO_ASIENTO
FROM    CRD.ANCP a
JOIN    CRD.CRAR c   ON c.CRARCDGO = a.CRARCDGO
JOIN    CNT.ASNT asn ON asn.ASNTCDGO = a.ANCPASNT
WHERE   a.ANCPTPOO IN (2, 3)
AND     c.CRARFCAC IS NOT NULL
AND     TO_CHAR(a.ANCPFCHA,'YYYYMM') <> TO_CHAR(c.CRARFCAC,'YYYYMM')
ORDER   BY a.ANCPCDGO;


-- =====================================================================================
-- FIN. Pegar la salida de los cuatro bloques.
--
-- ⚠️ ESTE SCRIPT NO CORRIGE NADA, Y NO SE DEBE CORREGIR SIN CONTABILIDAD. Re-fechar un
--    asiento ya mayorizado o de un periodo cerrado no es una correccion de datos: es
--    mover plata de periodo. Si el bloque 4 muestra asientos en ese estado, lo que
--    corresponde es un asiento de ajuste, no un UPDATE.
-- =====================================================================================

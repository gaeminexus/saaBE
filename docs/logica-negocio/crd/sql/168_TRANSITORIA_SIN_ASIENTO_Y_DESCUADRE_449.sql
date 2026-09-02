-- =====================================================================================
-- ⛔ DOS PROBLEMAS CONTABLES DE LA CARGA 449
--    (1) falta el asiento de transitoria contra bancos  (2) descuadre de ~$2.563
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ NO ESCRIBE NADA. Solo SELECT.
--
-- =====================================================================================
-- PROBLEMA 1 — NO SE GENERO EL ASIENTO DE TRANSITORIA CONTRA BANCOS
--
-- CUANDO DEBE GENERARSE, verificado en el codigo: en `confirmarRecepcion`
-- (CobroPetroContableServiceImpl:275) — o sea CUANDO CONTABILIDAD APRUEBA, en el mismo
-- paso que sella CRARFCAC. Es el asiento D bancos / H transitoria: registra que el
-- dinero llego. Sub-proceso 1 (TRANSITORIO) en CRD.ANCP.
--
-- POR QUE PUDO NO GENERARSE, y esta explicito en el codigo (:331-343):
--
--     if (!configuracionContabilidadService.contabilidadActiva()) {
--         // "la confirmacion IGUAL ocurre (ya sello arriba) pero sin asiento.
--         //  No es un error"
--         return resultado;
--     }
--
--   El flag es el rubro 237 (CRD_PARAMETROS_CONTABILIDAD), detalle alterno 1
--   (CONTABILIDAD_ACTIVA), valor numerico 1 = activa.
--
--   ⚠️ Si la recepcion se confirmo con el flag APAGADO, la carga quedo confirmada y
--      SIN asiento de transitoria — silenciosamente, por diseño. Y despues el reparto
--      (que si se genero hoy, con el flag ya encendido) ACREDITA la transitoria que
--      nunca fue debitada. La transitoria queda descuadrada por el total de la carga.
--
--   El flag se encendio el 2026-08-31 (ORDEN-EJECUCION-DDL-PENDIENTE.md §5). Si la
--   carga se confirmo antes de esa fecha, esa es la explicacion completa.
--
-- =====================================================================================
-- PROBLEMA 2 — SE REPARTIO MENOS DE LO RECIBIDO (~$2.563 de diferencia)
--
--   Asiento de REPARTO      (#CRE-2026-09-0006):  $354.491,37
--   Asiento de APLICACION   (el de arriba)     :  $351.927,95
--   DIFERENCIA                                 :  $  2.563,42
--
--   El reparto DEBITA la transitoria por el total recibido y la reparte entre aportes y
--   prestamos. La aplicacion registra lo que de verdad se aplico a cuotas y aportes. Si
--   la aplicacion es menor, ese dinero quedo en la transitoria SIN destino: la
--   contabilidad no cuadra, y el usuario ya fijo la regla — TODO el dinero recibido se
--   debe repartir.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los seis bloques.
-- =====================================================================================

SET PAGESIZE 300
SET LINESIZE 240

DEFINE CARGA = 449


-- =====================================================================================
-- BLOQUE 1 — El flag de contabilidad: ¿estaba encendido, y desde cuando?
--
-- Esperado: una fila con VALOR = 1. Si no existe la fila o VALOR <> 1, la contabilidad
-- de CRD esta apagada AHORA. Ojo: esto dice el estado de HOY, no el del dia en que se
-- confirmo la recepcion — eso lo cruza el bloque 2.
-- =====================================================================================
SELECT  d.PDTRCDGO                                          AS ID_DETALLE,
        d.PRBRCDGO                                          AS RUBRO,
        d.PDTRALTR                                          AS ALTERNO,
        d.PDTRDSCR                                          AS NOMBRE,
        d.PDTRVLRN                                          AS VALOR,
        d.PDTRESTD                                          AS ESTADO
FROM    SCP.PDTR d
WHERE   d.PRBRCDGO = 237
ORDER   BY d.PDTRALTR;


-- =====================================================================================
-- BLOQUE 2 — La carga 449: cuando se confirmo y que asientos tiene
--
-- Como leerlo:
--   * Si FECHA_AUTORIZACION es ANTERIOR al 2026-08-31 (cuando se encendio el flag), el
--     asiento de transitoria NO se genero por eso — explicacion completa, sin misterio.
--   * ASIENTOS_TRANSITORIO debe ser 1. Si es 0, falta el asiento.
--   * ASIENTOS_REPARTO y ASIENTOS_APLICACION deberian ser 1 cada uno.
-- =====================================================================================
SELECT  c.CRARCDGO                                          AS CARGA,
        TO_CHAR(c.CRARFCAC, 'YYYY-MM-DD HH24:MI')           AS FECHA_AUTORIZACION,
        c.CRARESTD                                          AS ESTADO_CARGA,
        (SELECT COUNT(*) FROM CRD.ANCP a
          WHERE a.CRARCDGO = c.CRARCDGO AND a.ANCPTPOO = 1
            AND a.ANCPIDST = 1)                               AS TRANSITORIO_ACTIVO,
        (SELECT COUNT(*) FROM CRD.ANCP a
          WHERE a.CRARCDGO = c.CRARCDGO AND a.ANCPTPOO = 1
            AND NVL(a.ANCPIDST,0) <> 1)                       AS TRANSITORIO_ANULADO,
        (SELECT COUNT(*) FROM CRD.ANCP a
          WHERE a.CRARCDGO = c.CRARCDGO AND a.ANCPTPOO = 2) AS ASIENTOS_REPARTO,
        (SELECT COUNT(*) FROM CRD.ANCP a
          WHERE a.CRARCDGO = c.CRARCDGO AND a.ANCPTPOO = 3) AS ASIENTOS_APLICACION
FROM    CRD.CRAR c
WHERE   c.CRARCDGO = &CARGA;

-- 2.b Los asientos que SI existen, con su valor. Para ver el descuadre de una mirada.
SELECT  a.ANCPTPOO                                          AS SUBPROCESO,
        CASE a.ANCPTPOO WHEN 1 THEN 'TRANSITORIO' WHEN 2 THEN 'REPARTO'
                        WHEN 3 THEN 'APLICACION' ELSE 'OTRO' END AS NOMBRE,
        a.ANCPNMAS                                          AS NRO_ASIENTO,
        TO_CHAR(a.ANCPFCHA, 'YYYY-MM-DD')                   AS FECHA,
        a.ANCPVLRR                                          AS VALOR
FROM    CRD.ANCP a
WHERE   a.CRARCDGO = &CARGA
ORDER   BY a.ANCPTPOO;


-- =====================================================================================
-- BLOQUE 3 — ⛔ EL DESCUADRE: recibido vs repartido vs aplicado
--
-- Como leerlo — las tres cifras deberian ser iguales:
--   TOTAL_ARCHIVO   lo que Petro dice que descontó (suma de PXCA.PXCADSDO)
--   TOTAL_APLICADO  lo que de verdad se aplico a cuotas de prestamo
--   TOTAL_APORTES   lo que se aplico a aportes
--   SIN_APLICAR     la diferencia. DEBE SER 0. Si no, ese dinero quedo en la
--                   transitoria sin destino.
-- =====================================================================================
SELECT  ROUND(SUM(p.PXCADSDO), 2)                           AS TOTAL_ARCHIVO,
        ROUND(NVL((SELECT SUM(g.PGPRVLRR) FROM CRD.PGPR g
                    WHERE g.CRARCDGO = &CARGA
                      AND NVL(g.PGPRANUL,0) = 0), 0), 2)    AS TOTAL_APLICADO_PRESTAMOS,
        ROUND(NVL((SELECT SUM(ap.APRTVLRR) FROM CRD.APRT ap
                    WHERE ap.CRARCDGO = &CARGA
                      AND NVL(ap.APRTIDST,1) <> 0), 0), 2)  AS TOTAL_APORTES,
        ROUND(SUM(p.PXCADSDO)
              - NVL((SELECT SUM(g.PGPRVLRR) FROM CRD.PGPR g
                      WHERE g.CRARCDGO = &CARGA
                        AND NVL(g.PGPRANUL,0) = 0), 0)
              - NVL((SELECT SUM(ap.APRTVLRR) FROM CRD.APRT ap
                      WHERE ap.CRARCDGO = &CARGA
                        AND NVL(ap.APRTIDST,1) <> 0), 0), 2) AS SIN_APLICAR
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA;


-- =====================================================================================
-- BLOQUE 4 — DE QUIEN es el dinero que no se aplico
--
-- Participe por participe: lo que Petro le descontó contra lo que se le aplico.
-- Cada fila con DIFERENCIA > 0.01 es plata recibida que no llego a ningun destino.
--
-- Como leerlo: si las diferencias se concentran en unos pocos participes, es un caso
-- puntual. Si estan repartidas en muchos por centavos, es redondeo acumulado.
-- =====================================================================================
SELECT  p.PXCACDPT                                          AS ROL,
        SUBSTR(p.PXCANMBR, 1, 30)                           AS PARTICIPE,
        d.DTCACDPP                                          AS PRODUCTO,
        p.PXCADSDO                                          AS DESCONTADO,
        ROUND(NVL((SELECT SUM(g.PGPRVLRR) FROM CRD.PGPR g
                    WHERE g.CRARCDGO = &CARGA
                      AND NVL(g.PGPRANUL,0) = 0
                      AND g.PRSTCDGO IN (SELECT pr.PRSTCDGO FROM CRD.PRST pr
                                          WHERE pr.ENTDCDGO = (SELECT MIN(e.ENTDCDGO)
                                                                FROM CRD.ENTD e
                                                                WHERE e.ENTDRLPC = p.PXCACDPT))
                  ), 0), 2)                                 AS APLICADO_PRESTAMOS,
        ROUND(NVL((SELECT SUM(ap.APRTVLRR) FROM CRD.APRT ap
                    WHERE ap.CRARCDGO = &CARGA
                      AND NVL(ap.APRTIDST,1) <> 0
                      AND ap.ENTDCDGO = (SELECT MIN(e.ENTDCDGO) FROM CRD.ENTD e
                                          WHERE e.ENTDRLPC = p.PXCACDPT)
                  ), 0), 2)                                 AS APLICADO_APORTES
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     NVL(p.PXCADSDO, 0) > 0.01
ORDER   BY p.PXCADSDO DESC
FETCH FIRST 40 ROWS ONLY;


-- =====================================================================================
-- BLOQUE 5 — El saldo real de la cuenta transitoria
--
-- Si falta el asiento de transitoria (problema 1), esta cuenta quedo acreditada por el
-- reparto sin haber sido debitada nunca. Este bloque muestra su movimiento.
--
-- Como leerlo: un saldo acreedor creciente es exactamente el sintoma. La cuenta es
-- 2.3.01.15.01 CUENTA TRANSITORIA segun el asiento de reparto de la carga.
-- =====================================================================================
SELECT  c.PLNNCNTA                                          AS CUENTA,
        c.PLNNNMBR                                          AS NOMBRE,
        COUNT(*)                                            AS LINEAS,
        ROUND(SUM(NVL(dt.DTASDBEE,0)), 2)                   AS TOTAL_DEBE,
        ROUND(SUM(NVL(dt.DTASHBRR,0)), 2)                   AS TOTAL_HABER,
        ROUND(SUM(NVL(dt.DTASDBEE,0)) - SUM(NVL(dt.DTASHBRR,0)), 2) AS SALDO
FROM    CNT.DTAS dt
JOIN    CNT.PLNN c ON c.PLNNCDGO = dt.PLNNCDGO
WHERE   c.PLNNCNTA = '2.3.01.15.01'
GROUP   BY c.PLNNCNTA, c.PLNNNMBR;


-- =====================================================================================
-- BLOQUE 6 — Las transferencias registradas de la carga
--
-- Son la contrapartida del asiento que falta (D bancos / H transitoria). Sirven para
-- saber por cuanto habria que generarlo y contra que banco.
-- =====================================================================================
SELECT  t.TRCRCDGO                                          AS ID_TRANSFERENCIA,
        TO_CHAR(t.TRCRFCHA, 'YYYY-MM-DD')                   AS FECHA,
        t.TRCRVLRR                                          AS VALOR,
        t.TRCRIDST                                          AS ESTADO
FROM    CRD.TRCR t
WHERE   t.CRARCDGO = &CARGA
ORDER   BY t.TRCRFCHA;


-- =====================================================================================
-- FIN. Pegar la salida de los seis bloques.
--
-- ⚠️ NO generar el asiento faltante a mano sin contabilidad. Si el flag estaba apagado,
--    puede haber MAS cargas en la misma situacion — el bloque 2 se puede repetir para
--    otras cargas cambiando el DEFINE.
-- =====================================================================================


-- =====================================================================================
-- BLOQUE 7 — DESCARTAR LAS OTRAS EXPLICACIONES (agregado 2026-09-02, a pedido del usuario)
--
-- El usuario pidio no dar por sentado que fue el flag. Se reviso el codigo y solo hay
-- TRES caminos por los que una carga puede quedar confirmada sin asiento de transitoria:
--
--   A) El flag de contabilidad estaba APAGADO al confirmar.
--      Unico `return` silencioso del metodo (:331-343).
--
--   B) Se confirmo, se REVERSO y se volvio a confirmar.
--      reversarRecepcion (:430) anula el asiento y marca el registro ANCP como anulado,
--      pero NO BORRA la fila. Si despues se reconfirmo con el flag apagado, queda una
--      fila anulada y ninguna activa.
--
--   C) ⛔ DESCARTADO POR EL MECANISMO, no por suposicion: cualquier OTRO fallo despues
--      del sello (plantilla COBRO_TRANSITORIO_PETRO inexistente, linea de transitoria
--      ausente, transferencia con cuenta bancaria sin cuenta contable, empresa no
--      resoluble) lanza IncomeException — que es @ApplicationException(rollback = true)
--      y extiende RuntimeException. Eso REVIERTE la transaccion entera, incluido el
--      sello de CRARFCAC. Una carga que fallo por ahi NO quedaria confirmada.
--      Por lo tanto: si hay CRARFCAC y no hay asiento activo, es (A) o (B), nunca (C).
--
-- Este bloque distingue (A) de (B) y busca otras cargas en la misma situacion.
--
-- Como leerlo:
--   * TRANSITORIO_ANULADO > 0 y TRANSITORIO_ACTIVO = 0 -> caso (B): hubo una reversion.
--   * Los dos en 0 -> caso (A): el flag estaba apagado. No hubo nunca asiento.
-- =====================================================================================
SELECT  c.CRARCDGO                                          AS CARGA,
        TO_CHAR(c.CRARFCAC, 'YYYY-MM-DD HH24:MI')           AS FECHA_AUTORIZACION,
        c.CRARESTD                                          AS ESTADO_CARGA,
        (SELECT COUNT(*) FROM CRD.ANCP a
          WHERE a.CRARCDGO = c.CRARCDGO AND a.ANCPTPOO = 1
            AND a.ANCPIDST = 1)                             AS TRANSITORIO_ACTIVO,
        (SELECT COUNT(*) FROM CRD.ANCP a
          WHERE a.CRARCDGO = c.CRARCDGO AND a.ANCPTPOO = 1
            AND NVL(a.ANCPIDST,0) <> 1)                     AS TRANSITORIO_ANULADO,
        (SELECT COUNT(*) FROM CRD.ANCP a
          WHERE a.CRARCDGO = c.CRARCDGO AND a.ANCPTPOO IN (2,3)
            AND a.ANCPIDST = 1)                             AS REPARTO_Y_APLICACION,
        CASE
            WHEN c.CRARFCAC IS NULL THEN 'sin confirmar'
            WHEN (SELECT COUNT(*) FROM CRD.ANCP a
                   WHERE a.CRARCDGO = c.CRARCDGO AND a.ANCPTPOO = 1
                     AND a.ANCPIDST = 1) > 0 THEN 'OK'
            WHEN (SELECT COUNT(*) FROM CRD.ANCP a
                   WHERE a.CRARCDGO = c.CRARCDGO AND a.ANCPTPOO = 1) > 0
                 THEN '(B) reversado y reconfirmado'
            ELSE '(A) confirmado con el flag apagado'
        END                                                 AS DIAGNOSTICO
FROM    CRD.CRAR c
WHERE   c.CRARFCAC IS NOT NULL
ORDER   BY c.CRARFCAC DESC;

-- ⚠️ ESTE BLOQUE MIRA **TODAS** LAS CARGAS CONFIRMADAS, no solo la 449. Cada fila con
--    DIAGNOSTICO distinto de 'OK' es una carga cuya transitoria quedo acreditada sin
--    haber sido debitada. Si son varias, el descuadre de la cuenta 2.3.01.15.01 es la
--    suma de todas — que es lo que deberia explicar el saldo del bloque 5.

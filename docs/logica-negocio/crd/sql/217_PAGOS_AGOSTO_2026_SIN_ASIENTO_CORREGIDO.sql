-- =====================================================================================
-- ⭐ PAGOS A PRESTAMOS DE AGOSTO 2026 SIN ASIENTO CONTABLE — VERSION CORREGIDA
-- FECHA: 2026-09-08   EQUIPO: omen-saa-1 (omen1)   SCRIPT: 217 (rango 200-249)
--
-- ⚠️ NO ESCRIBE NADA. Los cuatro bloques son SELECT. Se puede correr en horario laboral.
--
-- ⛔ ESTE SCRIPT REEMPLAZA AL sql/216. **EL 216 ESTABA MAL** y hay que ignorar su salida.
--    Tambien corrige el mapa del sql/215, que tenia el mismo error de fondo.
--
-- =====================================================================================
-- QUE ESTABA MAL EN EL 216, dicho sin adornos
--
--   El 216 daba por sentado que un pago hecho por "pagar cuota" dejaba el asiento estampado en
--   CRD.PGPR.PGPRASNT, porque en el codigo se ve que pagarCuota llama a aplicarAsiento():
--
--       ProcesoPagoPrestamoServiceImpl:216
--           Long numeroAsiento = contabilidadPrestamoService.contabilizarPagoCuota(resultado, ctx);
--           aplicarAsiento(evento, resultado, numeroAsiento);
--
--   Se leyo la LLAMADA y no lo que la llamada DEVUELVE. Y lo que devuelve es esto:
--
--       ContabilidadPrestamoServiceImpl:443
--           public Long contabilizarPagoCuota(...) {
--               ...
--               return null;          <-- SIEMPRE. A proposito.
--           }
--
--   ⇒ contabilizarPagoCuota devuelve null SIEMPRE, asi que aplicarAsiento estampa null, asi que
--     **NINGUN pago de cuota tiene jamas PGPRASNT**. Y esta bien que sea asi: el asiento de esa
--     plata es CBCRASN2, el asiento definitivo DEL COBRO, y encender el hook por pago lo
--     duplicaria. Lo dice el propio comentario del codigo, tanto en ContabilidadPrestamoServiceImpl:436
--     como en CobroCreditoServiceImpl:1895.
--
--   Lo mismo pasa con contabilizarPagoConAportes y contabilizarPrecancelacion cuando la
--   operacion nacio de un cobro: si ContextoPago.idCobroCredito viene con valor, devuelven null
--   sin tocar nada, porque procesarCobro ya genero CBCRASN2 por la misma plata.
--
--   ⇒ TODO pago hecho por la pantalla de cobros —que en agosto es la mayoria— sale sin PGPRASNT
--     y sin EVPRNMAS, y el 216 los conto a todos como "sin asiento". Por eso viste tantos.
--
-- =====================================================================================
-- DONDE VIVE DE VERDAD EL ASIENTO DE CADA PAGO (esta vez, la cadena completa)
--
--   1. CRD.PGPR.PGPRASNT                        abono a capital (asiento de RECLASIFICACION de
--                                               bandas), y pago con aportes / precancelacion
--                                               cuando fueron llamada DIRECTA, sin cobro.
--   2. CRD.EVPR.EVPRNMAS                        el asiento del evento (condonaciones).
--   3. ⭐ CRD.CBCR.CBCRASN2  via  CRD.DCBC      **EL QUE FALTABA.** El asiento definitivo del
--        PGPR.EVPRCDGO = DCBC.EVPRCDGO         COBRO. Es donde esta la plata de casi todo agosto.
--        DCBC.CBCRCDGO  = CBCR.CBCRCDGO         El enlace lo escribe CobroCreditoServiceImpl.enlazarEvento:1150.
--   4. CRD.ANCP (subproceso 3, APLICACION)      Petro / descuento nomina: asiento por CARGA.
--   5. nada, y esta bien                        MIGRACION: saldo historico, no lleva asiento.
--
-- ⚠️ EL RANGO: PGPRFCHA es TIMESTAMP. Se usa >= 01-ago y < 01-sep, NO un BETWEEN hasta el 31,
--    que perderia todo el 31 de agosto menos la medianoche.
--
-- COMO DEVOLVER EL RESULTADO: pegar los cuatro bloques. El BLOQUE 1 contesta la pregunta.
-- =====================================================================================

SET PAGESIZE 400
SET LINESIZE 300
SET FEEDBACK ON


-- =====================================================================================
-- BLOQUE 0 — CONTROL DE QUE LA CORRECCION ES LA CORRECTA.
--
-- Muestra, para los pagos de agosto, en cual de los cinco lugares aparecio el asiento. Si el
-- diagnostico de arriba es cierto, la gran mayoria tiene que caer en "3 cobro CBCRASN2" — que
-- es justo el que el 216 no miraba. **Si este bloque no muestra eso, avisame antes de seguir:
-- querria decir que la correccion tampoco es la buena.**
-- =====================================================================================

SELECT t.DONDE_APARECIO,
       COUNT(*)                            AS PAGOS,
       ROUND(SUM(NVL(t.VALOR, 0)), 2)      AS VALOR
  FROM (SELECT p.PGPRVLRR AS VALOR,
               CASE WHEN NVL(p.PGPRTPOO, 'x') = 'MIGRACION'      THEN '5 migracion (no lleva)'
                    WHEN p.PGPRASNT IS NOT NULL                  THEN '1 pago PGPRASNT'
                    WHEN e.EVPRNMAS IS NOT NULL                  THEN '2 evento EVPRNMAS'
                    WHEN b.CBCRASN2 IS NOT NULL                  THEN '3 cobro CBCRASN2'
                    WHEN p.CRARCDGO IS NOT NULL
                         AND EXISTS (SELECT 1 FROM CRD.ANCP n
                                      WHERE n.CRARCDGO = p.CRARCDGO
                                        AND n.ANCPTPOO = 3)      THEN '4 Petro ANCP aplicacion'
                    ELSE '⛔ 0 SIN ASIENTO EN NINGUN LADO' END AS DONDE_APARECIO
          FROM CRD.PGPR p
          LEFT JOIN CRD.EVPR e ON e.EVPRCDGO = p.EVPRCDGO
          LEFT JOIN (SELECT x.EVPRCDGO, MAX(x.CBCRCDGO) AS CBCRCDGO
                       FROM CRD.DCBC x GROUP BY x.EVPRCDGO) d ON d.EVPRCDGO = p.EVPRCDGO
          LEFT JOIN CRD.CBCR b ON b.CBCRCDGO = d.CBCRCDGO
         WHERE p.PGPRFCHA >= TIMESTAMP '2026-08-01 00:00:00'
           AND p.PGPRFCHA <  TIMESTAMP '2026-09-01 00:00:00'
           AND NVL(p.PGPRANUL, 0) = 0) t
 GROUP BY t.DONDE_APARECIO
 ORDER BY 1;


-- =====================================================================================
-- BLOQUE 1 — ⭐ LA RESPUESTA: pagos de agosto 2026 por origen, con y sin respaldo contable.
-- Ahora la columna SIN_RESPALDO cuenta solo los que no aparecen en NINGUNO de los cinco lugares.
-- =====================================================================================

SELECT t.ORIGEN,
       COUNT(*)                                                        AS PAGOS_AGOSTO,
       ROUND(SUM(NVL(t.VALOR, 0)), 2)                                  AS VALOR_TOTAL,
       SUM(CASE WHEN t.TIENE_ASIENTO = 1 THEN 1 ELSE 0 END)            AS CON_RESPALDO,
       SUM(CASE WHEN t.TIENE_ASIENTO = 0 THEN 1 ELSE 0 END)            AS SIN_RESPALDO,
       ROUND(SUM(CASE WHEN t.TIENE_ASIENTO = 0
                      THEN NVL(t.VALOR, 0) ELSE 0 END), 2)             AS VALOR_SIN_RESPALDO
  FROM (SELECT NVL(p.PGPRTPOO, '(sin tipo)') AS ORIGEN,
               p.PGPRVLRR                    AS VALOR,
               CASE WHEN NVL(p.PGPRTPOO, 'x') = 'MIGRACION' THEN 1
                    WHEN p.PGPRASNT IS NOT NULL             THEN 1
                    WHEN e.EVPRNMAS IS NOT NULL             THEN 1
                    WHEN b.CBCRASN2 IS NOT NULL             THEN 1
                    WHEN p.CRARCDGO IS NOT NULL
                         AND EXISTS (SELECT 1 FROM CRD.ANCP n
                                      WHERE n.CRARCDGO = p.CRARCDGO
                                        AND n.ANCPTPOO = 3)  THEN 1
                    ELSE 0 END               AS TIENE_ASIENTO
          FROM CRD.PGPR p
          LEFT JOIN CRD.EVPR e ON e.EVPRCDGO = p.EVPRCDGO
          LEFT JOIN (SELECT x.EVPRCDGO, MAX(x.CBCRCDGO) AS CBCRCDGO
                       FROM CRD.DCBC x GROUP BY x.EVPRCDGO) d ON d.EVPRCDGO = p.EVPRCDGO
          LEFT JOIN CRD.CBCR b ON b.CBCRCDGO = d.CBCRCDGO
         WHERE p.PGPRFCHA >= TIMESTAMP '2026-08-01 00:00:00'
           AND p.PGPRFCHA <  TIMESTAMP '2026-09-01 00:00:00'
           AND NVL(p.PGPRANUL, 0) = 0) t
 GROUP BY t.ORIGEN
 ORDER BY 5 DESC, 2 DESC;


-- =====================================================================================
-- BLOQUE 2 — ⛔ EL DETALLE: los pagos de agosto que no tienen asiento por NINGUNO de los
--            cinco caminos. Esperado: 0 filas.
-- =====================================================================================

SELECT p.PGPRCDGO                                AS PAGO,
       p.PRSTCDGO                                AS PRESTAMO,
       p.DTPRCDGO                                AS CUOTA,
       p.EVPRCDGO                                AS EVENTO,
       d.CBCRCDGO                                AS COBRO,
       p.CRARCDGO                                AS CARGA_PETRO,
       NVL(p.PGPRTPOO, '(sin tipo)')             AS ORIGEN,
       TO_CHAR(p.PGPRFCHA, 'YYYY-MM-DD HH24:MI') AS FECHA_PAGO,
       p.PGPRVLRR                                AS VALOR,
       p.PGPRCPPG                                AS CAPITAL,
       p.PGPRINPG                                AS INTERES,
       p.PGPRMRPG                                AS MORA,
       e.EVPRTPOO                                AS TIPO_EVENTO,
       b.CBCRESTD                                AS ESTADO_COBRO,
       b.CBCRASN1                                AS COBRO_ASN_TRANSITORIO,
       CASE WHEN p.CRARCDGO IS NOT NULL
                 THEN 'Petro: la carga ' || p.CRARCDGO || ' no tiene asiento de APLICACION'
            WHEN d.CBCRCDGO IS NOT NULL AND b.CBCRASN1 IS NOT NULL
                 THEN 'cobro ' || d.CBCRCDGO || ' con transitorio pero SIN definitivo (CBCRASN2)'
            WHEN d.CBCRCDGO IS NOT NULL
                 THEN 'cobro ' || d.CBCRCDGO || ' sin ningun asiento'
            WHEN p.EVPRCDGO IS NULL
                 THEN 'el pago no tiene evento, ni cobro, ni asiento'
            ELSE 'evento sin asiento y sin cobro asociado' END AS PROBLEMA
  FROM CRD.PGPR p
  LEFT JOIN CRD.EVPR e ON e.EVPRCDGO = p.EVPRCDGO
  LEFT JOIN (SELECT x.EVPRCDGO, MAX(x.CBCRCDGO) AS CBCRCDGO
               FROM CRD.DCBC x GROUP BY x.EVPRCDGO) d ON d.EVPRCDGO = p.EVPRCDGO
  LEFT JOIN CRD.CBCR b ON b.CBCRCDGO = d.CBCRCDGO
 WHERE p.PGPRFCHA >= TIMESTAMP '2026-08-01 00:00:00'
   AND p.PGPRFCHA <  TIMESTAMP '2026-09-01 00:00:00'
   AND NVL(p.PGPRANUL, 0) = 0
   AND NVL(p.PGPRTPOO, 'x') <> 'MIGRACION'
   AND p.PGPRASNT IS NULL
   AND e.EVPRNMAS IS NULL
   AND b.CBCRASN2 IS NULL
   AND (p.CRARCDGO IS NULL
        OR NOT EXISTS (SELECT 1 FROM CRD.ANCP n
                        WHERE n.CRARCDGO = p.CRARCDGO AND n.ANCPTPOO = 3))
 ORDER BY p.PGPRFCHA, p.PGPRCDGO;


-- =====================================================================================
-- BLOQUE 3 — ⛔⛔ EL QUE NO SE VE A OJO: el pago SI tiene asiento, pero ese asiento no existe,
--            esta ANULADO / REVERSADO / INCOMPLETO, o quedo fechado FUERA de agosto.
--
-- Para cuadrar el mes pesa igual que no tenerlo: cartera lo cuenta y contabilidad no.
-- Mira el asiento que efectivamente le corresponde a cada pago, venga de donde venga.
-- Estados: 1 ACTIVO · 2 ANULADO · 3 REVERSADO · 4 INCOMPLETO.
-- Esperado: 0 filas.
-- =====================================================================================

SELECT t.PAGO, t.PRESTAMO, t.ORIGEN, t.FECHA_PAGO, t.VALOR,
       t.FUENTE, t.ASIENTO,
       a.ASNTNMRO                        AS NUMERO,
       a.ASNTESTD                        AS ESTADO,
       TO_CHAR(a.ASNTFCHA, 'YYYY-MM-DD') AS FECHA_ASIENTO,
       CASE WHEN a.ASNTCDGO IS NULL  THEN 'EL ASIENTO NO EXISTE'
            WHEN a.ASNTESTD = 2      THEN 'asiento ANULADO'
            WHEN a.ASNTESTD = 3      THEN 'asiento REVERSADO'
            WHEN a.ASNTESTD = 4      THEN 'asiento INCOMPLETO'
            WHEN a.ASNTESTD <> 1     THEN 'otro estado — mirar'
            ELSE 'activo pero FECHADO FUERA DE AGOSTO' END AS PROBLEMA
  FROM (SELECT p.PGPRCDGO AS PAGO, p.PRSTCDGO AS PRESTAMO,
               NVL(p.PGPRTPOO, '(sin tipo)') AS ORIGEN,
               TO_CHAR(p.PGPRFCHA, 'YYYY-MM-DD') AS FECHA_PAGO,
               p.PGPRVLRR AS VALOR,
               CASE WHEN p.PGPRASNT IS NOT NULL THEN 'pago'
                    WHEN e.EVPRNMAS IS NOT NULL THEN 'evento'
                    WHEN b.CBCRASN2 IS NOT NULL THEN 'cobro' END AS FUENTE,
               COALESCE(p.PGPRASNT, e.EVPRNMAS, b.CBCRASN2) AS ASIENTO
          FROM CRD.PGPR p
          LEFT JOIN CRD.EVPR e ON e.EVPRCDGO = p.EVPRCDGO
          LEFT JOIN (SELECT x.EVPRCDGO, MAX(x.CBCRCDGO) AS CBCRCDGO
                       FROM CRD.DCBC x GROUP BY x.EVPRCDGO) d ON d.EVPRCDGO = p.EVPRCDGO
          LEFT JOIN CRD.CBCR b ON b.CBCRCDGO = d.CBCRCDGO
         WHERE p.PGPRFCHA >= TIMESTAMP '2026-08-01 00:00:00'
           AND p.PGPRFCHA <  TIMESTAMP '2026-09-01 00:00:00'
           AND NVL(p.PGPRANUL, 0) = 0
           AND NVL(p.PGPRTPOO, 'x') <> 'MIGRACION'
           AND COALESCE(p.PGPRASNT, e.EVPRNMAS, b.CBCRASN2) IS NOT NULL) t
  LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = t.ASIENTO
 WHERE a.ASNTCDGO IS NULL
    OR a.ASNTESTD <> 1
    OR a.ASNTFCHA <  DATE '2026-08-01'
    OR a.ASNTFCHA >= DATE '2026-09-01'
 ORDER BY t.FECHA_PAGO, t.PAGO;


-- =====================================================================================
-- BLOQUE 4 — CONTROL DE MAGNITUD: un renglon para poner al lado de la diferencia contra
-- contabilidad.
-- =====================================================================================

SELECT COUNT(*)                                                   AS PAGOS_AGOSTO,
       ROUND(SUM(NVL(t.VALOR, 0)), 2)                             AS VALOR_TOTAL_AGOSTO,
       SUM(CASE WHEN t.TIENE_ASIENTO = 0 THEN 1 ELSE 0 END)       AS PAGOS_SIN_ASIENTO,
       ROUND(SUM(CASE WHEN t.TIENE_ASIENTO = 0
                      THEN NVL(t.VALOR, 0) ELSE 0 END), 2)        AS VALOR_SIN_ASIENTO,
       ROUND(SUM(CASE WHEN t.ORIGEN = 'MIGRACION'
                      THEN NVL(t.VALOR, 0) ELSE 0 END), 2)        AS VALOR_MIGRACION_IGNORADO
  FROM (SELECT NVL(p.PGPRTPOO, '(sin tipo)') AS ORIGEN,
               p.PGPRVLRR                    AS VALOR,
               CASE WHEN NVL(p.PGPRTPOO, 'x') = 'MIGRACION' THEN 1
                    WHEN p.PGPRASNT IS NOT NULL             THEN 1
                    WHEN e.EVPRNMAS IS NOT NULL             THEN 1
                    WHEN b.CBCRASN2 IS NOT NULL             THEN 1
                    WHEN p.CRARCDGO IS NOT NULL
                         AND EXISTS (SELECT 1 FROM CRD.ANCP n
                                      WHERE n.CRARCDGO = p.CRARCDGO
                                        AND n.ANCPTPOO = 3)  THEN 1
                    ELSE 0 END               AS TIENE_ASIENTO
          FROM CRD.PGPR p
          LEFT JOIN CRD.EVPR e ON e.EVPRCDGO = p.EVPRCDGO
          LEFT JOIN (SELECT x.EVPRCDGO, MAX(x.CBCRCDGO) AS CBCRCDGO
                       FROM CRD.DCBC x GROUP BY x.EVPRCDGO) d ON d.EVPRCDGO = p.EVPRCDGO
          LEFT JOIN CRD.CBCR b ON b.CBCRCDGO = d.CBCRCDGO
         WHERE p.PGPRFCHA >= TIMESTAMP '2026-08-01 00:00:00'
           AND p.PGPRFCHA <  TIMESTAMP '2026-09-01 00:00:00'
           AND NVL(p.PGPRANUL, 0) = 0) t;

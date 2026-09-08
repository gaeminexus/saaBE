-- =====================================================================================
-- ⭐ PAGOS A PRESTAMOS DE AGOSTO 2026 SIN ASIENTO CONTABLE
-- FECHA: 2026-09-08   EQUIPO: omen-saa-1 (omen1)   SCRIPT: 216 (rango 200-249)
--
-- ⚠️ NO ESCRIBE NADA. Los cuatro bloques son SELECT. Se puede correr en horario laboral.
--
-- Version acotada del sql/215 a un solo mes: 01-ago-2026 a 31-ago-2026.
--
-- =====================================================================================
-- ⚠️ EL RANGO DE FECHAS, Y POR QUE NO ES UN BETWEEN
--
--   CRD.PGPR.PGPRFCHA es TIMESTAMP, no DATE. Un
--       BETWEEN DATE '2026-08-01' AND DATE '2026-08-31'
--   corta en las 00:00:00 del 31 y **pierde todo el 31 de agosto menos la medianoche**.
--   Este script usa rango medio abierto:
--       >= 01-ago-2026 00:00:00   y   < 01-sep-2026 00:00:00
--   Es el mismo defecto de borde que ya costo dos diagnosticos equivocados en el cierre de
--   cartera y en los informes mensuales (H54). No cambiarlo por un BETWEEN.
--
-- =====================================================================================
-- ⚠️ Y POR QUE NO ALCANZA CON "PGPRASNT IS NULL"
--
--   PGPRASNT no es una bandera de "contabilizado". Hay tres origenes que NO la escriben y que
--   igual tienen asiento, colgado de otra tabla (medido contra el codigo el 2026-09-08):
--
--     - DESCUENTO_NOMINA (Petro) -> el asiento es POR CARGA y vive en CRD.ANCP
--     - ACUERDO_CONDONACION      -> el asiento vive en el evento, CRD.EVPR.EVPRNMAS
--     - MIGRACION                -> saldo historico, no tiene ni debe tener asiento
--
--   Este script resuelve el asiento de CADA pago **por su origen**, no por una sola columna.
--   Detalle completo del mapa en el sql/215.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los cuatro bloques. El BLOQUE 1 contesta la
-- pregunta; el BLOQUE 2 dice cuales son.
-- =====================================================================================

SET PAGESIZE 400
SET LINESIZE 260
SET FEEDBACK ON


-- =====================================================================================
-- BLOQUE 1 — ⭐ LA RESPUESTA: pagos de agosto 2026, con y sin respaldo contable, por origen.
--
-- SIN_RESPALDO es la columna que contesta. Si da 0 en todas las filas, no hay pagos de agosto
-- sin asiento y el descuadre viene de otro lado.
-- =====================================================================================

SELECT t.ORIGEN,
       COUNT(*)                                                       AS PAGOS_AGOSTO,
       ROUND(SUM(NVL(t.VALOR, 0)), 2)                                 AS VALOR_TOTAL,
       SUM(CASE WHEN t.ASIENTO_EFECTIVO IS NOT NULL THEN 1 ELSE 0 END) AS CON_RESPALDO,
       SUM(CASE WHEN t.ASIENTO_EFECTIVO IS NULL     THEN 1 ELSE 0 END) AS SIN_RESPALDO,
       ROUND(SUM(CASE WHEN t.ASIENTO_EFECTIVO IS NULL
                      THEN NVL(t.VALOR, 0) ELSE 0 END), 2)            AS VALOR_SIN_RESPALDO,
       CASE WHEN t.ORIGEN = 'MIGRACION'
            THEN 'migracion: no lleva asiento por diseno, ignorar'
            ELSE '' END                                               AS NOTA
  FROM (SELECT NVL(p.PGPRTPOO, '(sin tipo)') AS ORIGEN,
               p.PGPRVLRR                    AS VALOR,
               CASE WHEN p.CRARCDGO IS NOT NULL
                         THEN (SELECT MAX(n.ANCPASNT) FROM CRD.ANCP n
                                WHERE n.CRARCDGO = p.CRARCDGO AND n.ANCPTPOO = 3)
                    WHEN p.PGPRASNT IS NOT NULL THEN p.PGPRASNT
                    ELSE e.EVPRNMAS
               END                           AS ASIENTO_EFECTIVO
          FROM CRD.PGPR p
          LEFT JOIN CRD.EVPR e ON e.EVPRCDGO = p.EVPRCDGO
         WHERE p.PGPRFCHA >= TIMESTAMP '2026-08-01 00:00:00'
           AND p.PGPRFCHA <  TIMESTAMP '2026-09-01 00:00:00'
           AND NVL(p.PGPRANUL, 0) = 0) t
 GROUP BY t.ORIGEN
 ORDER BY 5 DESC, 2 DESC;


-- =====================================================================================
-- BLOQUE 2 — ⛔ EL DETALLE: uno por uno, los pagos de agosto SIN asiento por ningun lado.
--
-- Se excluye MIGRACION, que por diseno no lleva asiento. Todo lo demas que salga aca es plata
-- aplicada a cartera en agosto sin ningun asiento detras.
--
-- Esperado: 0 filas.
-- =====================================================================================

SELECT p.PGPRCDGO                              AS PAGO,
       p.PRSTCDGO                              AS PRESTAMO,
       p.DTPRCDGO                              AS CUOTA,
       p.EVPRCDGO                              AS EVENTO,
       p.CRARCDGO                              AS CARGA_PETRO,
       NVL(p.PGPRTPOO, '(sin tipo)')           AS ORIGEN,
       TO_CHAR(p.PGPRFCHA, 'YYYY-MM-DD HH24:MI') AS FECHA_PAGO,
       p.PGPRVLRR                              AS VALOR,
       p.PGPRCPPG                              AS CAPITAL,
       p.PGPRINPG                              AS INTERES,
       p.PGPRMRPG                              AS MORA,
       e.EVPRTPOO                              AS TIPO_EVENTO,
       e.EVPRNMAS                              AS ASIENTO_DEL_EVENTO,
       CASE WHEN p.CRARCDGO IS NOT NULL
            THEN 'Petro: la carga ' || p.CRARCDGO || ' no tiene asiento de APLICACION en ANCP'
            WHEN p.EVPRCDGO IS NULL
            THEN 'el pago no tiene evento ni asiento'
            ELSE 'ni el pago ni su evento tienen asiento' END AS PROBLEMA
  FROM CRD.PGPR p
  LEFT JOIN CRD.EVPR e ON e.EVPRCDGO = p.EVPRCDGO
 WHERE p.PGPRFCHA >= TIMESTAMP '2026-08-01 00:00:00'
   AND p.PGPRFCHA <  TIMESTAMP '2026-09-01 00:00:00'
   AND NVL(p.PGPRANUL, 0) = 0
   AND NVL(p.PGPRTPOO, 'x') <> 'MIGRACION'
   AND p.PGPRASNT IS NULL
   AND e.EVPRNMAS IS NULL
   AND (p.CRARCDGO IS NULL
        OR NOT EXISTS (SELECT 1 FROM CRD.ANCP n
                        WHERE n.CRARCDGO = p.CRARCDGO AND n.ANCPTPOO = 3))
 ORDER BY p.PGPRFCHA, p.PGPRCDGO;


-- =====================================================================================
-- BLOQUE 3 — ⛔⛔ EL QUE NO SE VE A OJO: el pago de agosto SI tiene asiento, pero ese asiento
--            no existe, o esta ANULADO / REVERSADO / INCOMPLETO en contabilidad.
--
-- Para el cuadre pesa igual que no tenerlo: cartera lo cuenta como pagado y contabilidad no.
-- PGPRASNT NO se limpia cuando alguien anula el asiento del lado de CNT.
--
-- Estados: 1 ACTIVO · 2 ANULADO · 3 REVERSADO · 4 INCOMPLETO.
-- Esperado: 0 filas.
-- =====================================================================================

SELECT p.PGPRCDGO                              AS PAGO,
       p.PRSTCDGO                              AS PRESTAMO,
       NVL(p.PGPRTPOO, '(sin tipo)')           AS ORIGEN,
       TO_CHAR(p.PGPRFCHA, 'YYYY-MM-DD')       AS FECHA_PAGO,
       p.PGPRVLRR                              AS VALOR,
       p.PGPRASNT                              AS ASIENTO,
       a.ASNTNMRO                              AS NUMERO,
       a.ASNTESTD                              AS ESTADO,
       TO_CHAR(a.ASNTFCHA, 'YYYY-MM-DD')       AS FECHA_ASIENTO,
       CASE WHEN a.ASNTCDGO IS NULL       THEN 'EL ASIENTO NO EXISTE'
            WHEN a.ASNTESTD = 2           THEN 'asiento ANULADO'
            WHEN a.ASNTESTD = 3           THEN 'asiento REVERSADO'
            WHEN a.ASNTESTD = 4           THEN 'asiento INCOMPLETO'
            WHEN a.ASNTESTD <> 1          THEN 'otro estado — mirar'
            ELSE 'activo pero fuera de agosto' END AS PROBLEMA
  FROM CRD.PGPR p
  LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = p.PGPRASNT
 WHERE p.PGPRFCHA >= TIMESTAMP '2026-08-01 00:00:00'
   AND p.PGPRFCHA <  TIMESTAMP '2026-09-01 00:00:00'
   AND NVL(p.PGPRANUL, 0) = 0
   AND p.PGPRASNT IS NOT NULL
   AND (a.ASNTCDGO IS NULL
        OR a.ASNTESTD <> 1
        OR a.ASNTFCHA <  DATE '2026-08-01'
        OR a.ASNTFCHA >= DATE '2026-09-01')
 ORDER BY p.PGPRFCHA, p.PGPRCDGO;


-- =====================================================================================
-- BLOQUE 4 — CONTROL DE MAGNITUD, para poner al lado de la diferencia contra contabilidad.
-- Un solo renglon con el total de agosto y cuanto de eso queda sin respaldo.
-- =====================================================================================

SELECT COUNT(*)                                                      AS PAGOS_AGOSTO,
       ROUND(SUM(NVL(p.PGPRVLRR, 0)), 2)                             AS VALOR_TOTAL_AGOSTO,
       SUM(CASE WHEN p.PGPRASNT IS NULL AND e.EVPRNMAS IS NULL
                     AND NVL(p.PGPRTPOO,'x') <> 'MIGRACION'
                     AND (p.CRARCDGO IS NULL
                          OR NOT EXISTS (SELECT 1 FROM CRD.ANCP n
                                          WHERE n.CRARCDGO = p.CRARCDGO AND n.ANCPTPOO = 3))
                THEN 1 ELSE 0 END)                                   AS PAGOS_SIN_ASIENTO,
       ROUND(SUM(CASE WHEN p.PGPRASNT IS NULL AND e.EVPRNMAS IS NULL
                           AND NVL(p.PGPRTPOO,'x') <> 'MIGRACION'
                           AND (p.CRARCDGO IS NULL
                                OR NOT EXISTS (SELECT 1 FROM CRD.ANCP n
                                                WHERE n.CRARCDGO = p.CRARCDGO AND n.ANCPTPOO = 3))
                      THEN NVL(p.PGPRVLRR, 0) ELSE 0 END), 2)        AS VALOR_SIN_ASIENTO,
       ROUND(SUM(CASE WHEN NVL(p.PGPRTPOO,'x') = 'MIGRACION'
                      THEN NVL(p.PGPRVLRR, 0) ELSE 0 END), 2)        AS VALOR_MIGRACION_IGNORADO
  FROM CRD.PGPR p
  LEFT JOIN CRD.EVPR e ON e.EVPRCDGO = p.EVPRCDGO
 WHERE p.PGPRFCHA >= TIMESTAMP '2026-08-01 00:00:00'
   AND p.PGPRFCHA <  TIMESTAMP '2026-09-01 00:00:00'
   AND NVL(p.PGPRANUL, 0) = 0;

-- =====================================================================================
-- PAGOS A PRESTAMOS SIN RESPALDO CONTABLE — para cuadrar el cierre de cartera con contabilidad
-- FECHA: 2026-09-08   EQUIPO: omen-saa-1 (omen1)   SCRIPT: 215 (rango 200-249)
--
-- ⚠️ NO ESCRIBE NADA. Los seis bloques son SELECT. Se puede correr en horario laboral.
--
-- =====================================================================================
-- ⛔ LEER ESTO ANTES DE MIRAR NINGUN NUMERO
--
--   La pregunta fue "que pagos a prestamos no tienen asiento contable". La columna que parece
--   contestarla es CRD.PGPR.PGPRASNT (id del asiento, = CNT.ASNT.ASNTCDGO). Pero:
--
--   ⭐ PGPRASNT **NO** ES UNA BANDERA DE "ESTE PAGO ESTA CONTABILIZADO".
--      Se estampa en unos caminos y NO se estampa en otros, y en esos otros el asiento SI
--      EXISTE, solo que colgado de otra tabla. Medido contra el codigo, 2026-09-08:
--
--   SE ESTAMPA (PGPRASNT queda con el id del asiento):
--     - pagarCuota           ProcesoPagoPrestamoServiceImpl:217  -> aplicarAsiento (todos los PGPR)
--     - pagarConAportes      ProcesoPagoPrestamoServiceImpl:637  -> aplicarAsiento (todos los PGPR)
--     - abono a capital      AbonoCapitalPrestamoServiceImpl:287
--     - precancelacion       ProcesoPagoPrestamoServiceImpl:1175 -> ⚠️ SOLO la fila de capital futuro
--
--   NO SE ESTAMPA NUNCA, Y ESTA BIEN QUE ASI SEA:
--     - DESCUENTO_NOMINA (Petro)  ProcesoCargaPetroServiceImpl:421 crea el PGPR y no le pone asiento.
--                                 El asiento de Petro es AGREGADO POR CARGA y vive en CRD.ANCP
--                                 (tres por carga: TRANSITORIO, REPARTO, APLICACION). Un PGPR de
--                                 Petro con PGPRASNT nulo es lo NORMAL, no un hueco. Son la MAYORIA
--                                 de las filas de PGPR.
--     - ACUERDO_CONDONACION       AcuerdoCondonacionServiceImpl:547 crea el PGPR y no le pone
--                                 asiento. El asiento queda en el EVENTO (CRD.EVPR.EVPRNMAS).
--     - MIGRACION                 PrestamoServiceImpl:942. Son saldos historicos cargados, no
--                                 movimientos del periodo: no tienen ni deben tener asiento.
--
--   ⇒ Un "SELECT * FROM CRD.PGPR WHERE PGPRASNT IS NULL" devuelve MILES de filas y **casi todas
--     son correctas**. Ese numero no sirve para cuadrar: asusta y no dice nada.
--
--   Por eso este script no da un numero: da SEIS, uno por origen, y cada uno mirado donde su
--   asiento realmente vive. El BLOQUE 0 es el mapa; el 1 y el 2 son los que pueden doler.
--
-- =====================================================================================
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los seis bloques. Si alguno sale muy largo,
-- el BLOQUE 0, el 1 y el 2 son los que hay que ver si o si.
-- =====================================================================================

SET PAGESIZE 400
SET LINESIZE 260
SET FEEDBACK ON


-- =====================================================================================
-- BLOQUE 0 — ⭐ EL MAPA. No asume nada: deja que el dato diga que origen estampa y cual no.
--
-- Como leerlo: si un tipo tiene SIN_ASIENTO = 0, ese camino estampa siempre y cualquier hueco
-- futuro saltaria. Si tiene CON_ASIENTO = 0, ese camino nunca estampa (es de los de arriba) y
-- hay que buscarle el asiento en otra tabla. **Si un tipo tiene LAS DOS COLUMNAS CON VALOR, ahi
-- hay algo que mirar**: el mismo proceso a veces estampo y a veces no.
-- =====================================================================================

SELECT NVL(p.PGPRTPOO, '(sin tipo)')                                  AS TIPO_PAGO,
       COUNT(*)                                                       AS PAGOS,
       SUM(CASE WHEN p.PGPRASNT IS NOT NULL THEN 1 ELSE 0 END)        AS CON_ASIENTO,
       SUM(CASE WHEN p.PGPRASNT IS NULL     THEN 1 ELSE 0 END)        AS SIN_ASIENTO,
       ROUND(SUM(CASE WHEN p.PGPRASNT IS NULL THEN NVL(p.PGPRVLRR,0) ELSE 0 END), 2)
                                                                      AS VALOR_SIN_ASIENTO,
       SUM(CASE WHEN p.CRARCDGO IS NOT NULL THEN 1 ELSE 0 END)        AS VIENEN_DE_CARGA_PETRO,
       SUM(CASE WHEN NVL(p.PGPRANUL,0) <> 0 THEN 1 ELSE 0 END)        AS ANULADOS,
       TO_CHAR(MIN(p.PGPRFCHA), 'YYYY-MM-DD')                         AS DESDE,
       TO_CHAR(MAX(p.PGPRFCHA), 'YYYY-MM-DD')                         AS HASTA
  FROM CRD.PGPR p
 GROUP BY NVL(p.PGPRTPOO, '(sin tipo)')
 ORDER BY 4 DESC;


-- =====================================================================================
-- BLOQUE 1 — ⛔ EL HUECO REAL: pagos de un camino QUE SI ESTAMPA y que quedaron sin asiento.
--
-- Se excluye a proposito lo que por diseno no estampa (Petro por carga, condonacion por evento,
-- migracion), y los pagos anulados. Lo que salga aca es plata movida en cartera **sin ningun
-- asiento detras**, y es exactamente lo que descuadra contra contabilidad.
--
-- Esperado: 0 filas. Si devuelve algo, cada fila es un pago a investigar uno por uno.
--
-- ⚠️ La PRECANCELACION aparece aca a proposito aunque solo estampe la fila de capital futuro:
--    si sale, hay que mirarla con el ojo puesto en eso y NO tratarla como hueco automaticamente.
--    Se la marca en la columna OJO.
-- =====================================================================================

SELECT p.PGPRCDGO                              AS PAGO,
       p.PRSTCDGO                              AS PRESTAMO,
       p.DTPRCDGO                              AS CUOTA,
       p.EVPRCDGO                              AS EVENTO,
       p.PGPRTPOO                              AS TIPO,
       TO_CHAR(p.PGPRFCHA, 'YYYY-MM-DD')       AS FECHA_PAGO,
       p.PGPRVLRR                              AS VALOR,
       e.EVPRNMAS                              AS ASIENTO_DEL_EVENTO,
       CASE WHEN p.PGPRTPOO = 'PRECANCELACION'
            THEN 'precancelacion: solo estampa capital futuro — revisar a mano'
            WHEN e.EVPRNMAS IS NOT NULL
            THEN 'el EVENTO si tiene asiento — se perdio el estampado en el pago'
            ELSE 'ni el pago ni el evento tienen asiento' END AS OJO
  FROM CRD.PGPR p
  LEFT JOIN CRD.EVPR e ON e.EVPRCDGO = p.EVPRCDGO
 WHERE p.PGPRASNT IS NULL
   AND NVL(p.PGPRANUL, 0) = 0
   AND p.CRARCDGO IS NULL
   AND NVL(p.PGPRTPOO, 'x') NOT IN ('DESCUENTO_NOMINA', 'MIGRACION', 'ACUERDO_CONDONACION')
 ORDER BY p.PGPRFCHA DESC, p.PGPRCDGO DESC;


-- =====================================================================================
-- BLOQUE 2 — ⛔⛔ EL PEOR CASO Y EL MAS DIFICIL DE VER A OJO:
--            el pago SI tiene asiento estampado, pero ese asiento **no existe**, o existe y
--            esta ANULADO / REVERSADO / INCOMPLETO en contabilidad.
--
-- Esto descuadra sin dejar rastro: la cartera lo cuenta como pagado y contabilidad no lo tiene.
-- El estampado en PGPRASNT NO se limpia cuando alguien anula el asiento del lado de CNT.
--
-- Estados (com.saa.rubros.EstadoAsiento): 1 ACTIVO · 2 ANULADO · 3 REVERSADO · 4 INCOMPLETO.
-- Esperado: 0 filas.
-- =====================================================================================

SELECT p.PGPRCDGO                          AS PAGO,
       p.PRSTCDGO                          AS PRESTAMO,
       p.PGPRTPOO                          AS TIPO,
       TO_CHAR(p.PGPRFCHA, 'YYYY-MM-DD')   AS FECHA_PAGO,
       p.PGPRVLRR                          AS VALOR,
       p.PGPRASNT                          AS ASIENTO_ESTAMPADO,
       a.ASNTNMRO                          AS NUMERO_ASIENTO,
       a.ASNTESTD                          AS ESTADO_ASIENTO,
       CASE WHEN a.ASNTCDGO IS NULL THEN 'EL ASIENTO NO EXISTE'
            WHEN a.ASNTESTD = 2 THEN 'asiento ANULADO'
            WHEN a.ASNTESTD = 3 THEN 'asiento REVERSADO'
            WHEN a.ASNTESTD = 4 THEN 'asiento INCOMPLETO'
            ELSE 'otro estado — mirar' END  AS PROBLEMA,
       TO_CHAR(a.ASNTFCHA, 'YYYY-MM-DD')   AS FECHA_ASIENTO
  FROM CRD.PGPR p
  LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = p.PGPRASNT
 WHERE p.PGPRASNT IS NOT NULL
   AND NVL(p.PGPRANUL, 0) = 0
   AND (a.ASNTCDGO IS NULL OR a.ASNTESTD <> 1)
 ORDER BY p.PGPRFCHA DESC, p.PGPRCDGO DESC;


-- =====================================================================================
-- BLOQUE 3 — PETRO: cargas con pagos aplicados a las que les falta alguno de sus tres asientos.
--
-- El asiento de Petro es por CARGA, no por pago: CRD.ANCP, un registro por subproceso.
-- Una carga con pagos y sin sus tres filas es un descuadre agregado — y del tamano de una carga
-- entera, no de un pago.
--
-- Esperado: la columna FALTAN vacia en todas las filas.
--
-- ⚠️ Una carga del mes EN CURSO puede tener el transitorio y todavia no el reparto ni la
--    aplicacion: eso no es un defecto, es que el ciclo no termino. Mirar la fecha.
-- =====================================================================================

SELECT p.CRARCDGO                                AS CARGA,
       COUNT(p.PGPRCDGO)                         AS PAGOS_APLICADOS,
       ROUND(SUM(NVL(p.PGPRVLRR, 0)), 2)         AS VALOR_APLICADO,
       TO_CHAR(MIN(p.PGPRFCHA), 'YYYY-MM-DD')    AS DESDE,
       MAX(CASE WHEN n.ANCPTPOO = 1 THEN n.ANCPASNT END) AS ASNT_TRANSITORIO,
       MAX(CASE WHEN n.ANCPTPOO = 2 THEN n.ANCPASNT END) AS ASNT_REPARTO,
       MAX(CASE WHEN n.ANCPTPOO = 3 THEN n.ANCPASNT END) AS ASNT_APLICACION,
       TRIM(CASE WHEN MAX(CASE WHEN n.ANCPTPOO = 1 THEN 1 ELSE 0 END) = 0 THEN 'TRANSITORIO ' END ||
            CASE WHEN MAX(CASE WHEN n.ANCPTPOO = 2 THEN 1 ELSE 0 END) = 0 THEN 'REPARTO '     END ||
            CASE WHEN MAX(CASE WHEN n.ANCPTPOO = 3 THEN 1 ELSE 0 END) = 0 THEN 'APLICACION '  END)
                                                 AS FALTAN
  FROM CRD.PGPR p
  LEFT JOIN CRD.ANCP n ON n.CRARCDGO = p.CRARCDGO
 WHERE p.CRARCDGO IS NOT NULL
   AND NVL(p.PGPRANUL, 0) = 0
 GROUP BY p.CRARCDGO
 ORDER BY p.CRARCDGO;


-- =====================================================================================
-- BLOQUE 4 — CONDONACIONES: el asiento vive en el EVENTO, no en el pago.
-- Esperado: 0 filas. Una fila aca es una condonacion aplicada a cartera sin asiento.
-- =====================================================================================

SELECT p.PGPRCDGO                          AS PAGO,
       p.PRSTCDGO                          AS PRESTAMO,
       p.EVPRCDGO                          AS EVENTO,
       TO_CHAR(p.PGPRFCHA, 'YYYY-MM-DD')   AS FECHA_PAGO,
       p.PGPRVLRR                          AS VALOR,
       e.EVPRTPOO                          AS TIPO_EVENTO,
       e.EVPRNMAS                          AS ASIENTO_DEL_EVENTO,
       CASE WHEN p.EVPRCDGO IS NULL THEN 'el pago no tiene evento'
            WHEN e.EVPRNMAS IS NULL  THEN 'el evento no tiene asiento'
            ELSE 'ok' END                  AS PROBLEMA
  FROM CRD.PGPR p
  LEFT JOIN CRD.EVPR e ON e.EVPRCDGO = p.EVPRCDGO
 WHERE p.PGPRTPOO = 'ACUERDO_CONDONACION'
   AND NVL(p.PGPRANUL, 0) = 0
   AND (p.EVPRCDGO IS NULL OR e.EVPRNMAS IS NULL)
 ORDER BY p.PGPRFCHA DESC;


-- =====================================================================================
-- BLOQUE 5 — COBROS DE CREDITO procesados sin su asiento definitivo.
-- Otro angulo del mismo cuadre: el cobro se dio por procesado y no dejo asiento.
-- CBCRASN1 transitorio · CBCRASN2 definitivo · CBCRASRP reparto.
-- Esperado: 0 filas.
-- =====================================================================================

SELECT b.CBCRCDGO                          AS COBRO,
       b.CBCRTPOO                          AS TIPO_OPERACION,
       b.CBCRESTD                          AS ESTADO,
       TO_CHAR(b.CBCRFCHA, 'YYYY-MM-DD')   AS FECHA,
       b.CBCRVLRR                          AS VALOR,
       b.CBCRASN1                          AS ASNT_TRANSITORIO,
       b.CBCRASN2                          AS ASNT_DEFINITIVO,
       b.CBCRASRP                          AS ASNT_REPARTO,
       b.CBCRUSPR                          AS USUARIO_PROCESO,
       SUBSTR(b.CBCROBSR, 1, 60)           AS OBSERVACION
  FROM CRD.CBCR b
 WHERE b.CBCRFCPR IS NOT NULL
   AND b.CBCRFCAN IS NULL
   AND b.CBCRASN2 IS NULL
 ORDER BY b.CBCRFCHA DESC, b.CBCRCDGO DESC;


-- =====================================================================================
-- BLOQUE 6 — ⭐ EL RESUMEN PARA EL CUADRE: cuanta plata queda sin respaldo, mes por mes.
--
-- Junta el hueco del BLOQUE 1 (pago sin asiento donde deberia haberlo) con el del BLOQUE 2
-- (asiento estampado que no existe o esta anulado). Es el numero que hay que poner al lado de
-- la diferencia contra contabilidad para ver si la explica.
--
-- Esperado: 0 filas. Si sale un mes, ese es el mes a atacar.
-- =====================================================================================

SELECT TO_CHAR(t.PGPRFCHA, 'YYYY-MM')                AS PERIODO,
       t.MOTIVO,
       COUNT(*)                                      AS PAGOS,
       ROUND(SUM(NVL(t.PGPRVLRR, 0)), 2)             AS VALOR_SIN_RESPALDO
  FROM (
        SELECT p.PGPRFCHA, p.PGPRVLRR, 'pago sin asiento' AS MOTIVO
          FROM CRD.PGPR p
         WHERE p.PGPRASNT IS NULL
           AND NVL(p.PGPRANUL, 0) = 0
           AND p.CRARCDGO IS NULL
           AND NVL(p.PGPRTPOO, 'x') NOT IN ('DESCUENTO_NOMINA', 'MIGRACION', 'ACUERDO_CONDONACION')
        UNION ALL
        SELECT p.PGPRFCHA, p.PGPRVLRR, 'asiento inexistente o anulado' AS MOTIVO
          FROM CRD.PGPR p
          LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = p.PGPRASNT
         WHERE p.PGPRASNT IS NOT NULL
           AND NVL(p.PGPRANUL, 0) = 0
           AND (a.ASNTCDGO IS NULL OR a.ASNTESTD <> 1)
       ) t
 GROUP BY TO_CHAR(t.PGPRFCHA, 'YYYY-MM'), t.MOTIVO
 ORDER BY 1 DESC, 2;

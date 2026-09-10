-- =====================================================================================
-- DIAGNOSTICO — "Capital Pagado" mal calculado en prestamos con CUOTA 0
-- FECHA: 2026-09-10   EQUIPO: omen-saa-1 (omen1)   SCRIPT: 221 (rango 200-249)
--
-- ⚠️ NO ESCRIBE NADA. Los cuatro bloques son SELECT. Se puede correr en horario laboral.
--
-- =====================================================================================
-- QUE PASA Y POR QUE ESTE SCRIPT
--
--   La columna "Capital Pagado" de la consulta de prestamos ahora sale de
--   SUM(PGPR.PGPRCPPG) sobre los pagos VIGENTES del prestamo (PGPRANUL nulo o 0).
--   El usuario reporto que en algunos prestamos CON CUOTA 0 el valor no cuadra, y
--   esta revisando a mano si pasa en otros.
--
--   El BLOQUE 3 hace ese barrido automaticamente: no hace falta revisar prestamo por
--   prestamo.
--
--   QUE ES LA CUOTA 0: una fila con DTPRNMCT = 0 que la calculadora agrega cuando el
--   prestamo se genera con "tiene cuota cero". Cubre el periodo inicial: paga interes y
--   seguros, y su CAPITAL ES 0 (CalculadoraAmortizacionServiceImpl:200). Por eso NO
--   deberia aportar nada al capital pagado. Si aporta, ahi esta el defecto.
--
--   LAS DOS HIPOTESIS QUE ESTE SCRIPT SEPARA:
--     A) La cuota 0 tiene un pago con PGPRCPPG > 0 -> el capital pagado sale de MAS.
--     B) Faltan filas en PGPR (cartera migrada sin pagos registrados) -> sale de MENOS,
--        o en 0, aunque las cuotas figuren pagadas.
--   No son la misma causa y no se arreglan igual: leer los bloques antes de decidir.
--
-- COMO USARLO
--   1. Si ya tenes un prestamo concreto que se ve mal, poné su codigo en el BLOQUE 0
--      y corré los bloques 1 y 2.
--   2. Corré el BLOQUE 3 (barrido) para saber a cuantos prestamos les pasa.
--   3. Pasame las salidas. Con eso se decide el arreglo; sin eso es adivinar.
--
-- =====================================================================================


-- =====================================================================================
-- BLOQUE 0 — El prestamo a examinar. CAMBIAR ESTE NUMERO.
-- =====================================================================================
DEFINE PRESTAMO = 0;


-- =====================================================================================
-- BLOQUE 1 — Radiografia del prestamo: que dice la tabla de amortizacion y que dice PGPR
--
--   Una fila por cuota, con el capital de la cuota, su estado, y lo que suman sus pagos
--   vigentes. La cuota 0 aparece primero.
--
--   QUE MIRAR:
--     - Fila DTPRNMCT = 0: CAPITAL_CUOTA deberia ser 0 y CAPITAL_PAGADO_PGPR deberia
--       ser 0. Si CAPITAL_PAGADO_PGPR > 0 -> HIPOTESIS A confirmada.
--     - Cuotas con estado 4 (PAGADA) o 7 (CANCELADA_ANTICIPADA) y PAGOS_VIGENTES = 0
--       -> HIPOTESIS B: la cuota figura pagada pero no hay pago registrado.
-- =====================================================================================
SELECT d.DTPRNMCT                                   AS num_cuota,
       d.DTPRCDGO                                   AS cod_cuota,
       d.DTPRESTD                                   AS estado_cuota,
       d.DTPRCPTL                                   AS capital_cuota,
       d.DTPRCPPG                                   AS capital_pagado_en_la_cuota,
       NVL(g.pagos_vigentes, 0)                     AS pagos_vigentes,
       NVL(g.capital_pagado_pgpr, 0)                AS capital_pagado_pgpr,
       NVL(g.pagos_anulados, 0)                     AS pagos_anulados
  FROM CRD.DTPR d
  LEFT JOIN (SELECT p.DTPRCDGO,
                    SUM(CASE WHEN NVL(p.PGPRANUL, 0) = 0 THEN 1 ELSE 0 END)              AS pagos_vigentes,
                    SUM(CASE WHEN NVL(p.PGPRANUL, 0) = 0 THEN NVL(p.PGPRCPPG, 0) ELSE 0 END) AS capital_pagado_pgpr,
                    SUM(CASE WHEN NVL(p.PGPRANUL, 0) <> 0 THEN 1 ELSE 0 END)             AS pagos_anulados
               FROM CRD.PGPR p
              GROUP BY p.DTPRCDGO) g
    ON g.DTPRCDGO = d.DTPRCDGO
 WHERE d.PRSTCDGO = &PRESTAMO
 ORDER BY d.DTPRNMCT;


-- =====================================================================================
-- BLOQUE 2 — El numero que muestra la pantalla, contra las alternativas
--
--   CAPITAL_PAGADO_PANTALLA es exactamente lo que calcula hoy POST /prst/saldos.
--   Las otras columnas son las candidatas con las que se lo puede contrastar.
--
--   QUE MIRAR: si PANTALLA y SEGUN_CUOTAS_LIQUIDADAS difieren mucho, el problema es de
--   ORIGEN DE DATOS (hipotesis B), no de la formula. Si difieren solo en el aporte de
--   la cuota 0, es la hipotesis A.
-- =====================================================================================
SELECT p.PRSTCDGO                                                   AS prestamo,
       p.PRSTIDST                                                   AS estado_prestamo,
       (SELECT NVL(SUM(g.PGPRCPPG), 0)
          FROM CRD.PGPR g
          JOIN CRD.DTPR dd ON dd.DTPRCDGO = g.DTPRCDGO
         WHERE dd.PRSTCDGO = p.PRSTCDGO
           AND NVL(g.PGPRANUL, 0) = 0)                              AS capital_pagado_pantalla,
       (SELECT NVL(SUM(g.PGPRCPPG), 0)
          FROM CRD.PGPR g
          JOIN CRD.DTPR dd ON dd.DTPRCDGO = g.DTPRCDGO
         WHERE dd.PRSTCDGO = p.PRSTCDGO
           AND NVL(g.PGPRANUL, 0) = 0
           AND dd.DTPRNMCT > 0)                                     AS pantalla_sin_cuota_cero,
       (SELECT NVL(SUM(dd.DTPRCPTL), 0)
          FROM CRD.DTPR dd
         WHERE dd.PRSTCDGO = p.PRSTCDGO
           AND dd.DTPRESTD IN (4, 7))                               AS segun_cuotas_liquidadas,
       (SELECT NVL(SUM(dd.DTPRCPPG), 0)
          FROM CRD.DTPR dd
         WHERE dd.PRSTCDGO = p.PRSTCDGO)                            AS segun_capital_pagado_de_cuotas,
       (SELECT COUNT(*)
          FROM CRD.DTPR dd
         WHERE dd.PRSTCDGO = p.PRSTCDGO
           AND dd.DTPRNMCT = 0)                                     AS tiene_cuota_cero
  FROM CRD.PRST p
 WHERE p.PRSTCDGO = &PRESTAMO;


-- =====================================================================================
-- BLOQUE 3 — ⭐ BARRIDO: ¿a cuantos prestamos con cuota 0 les pasa?
--
--   Esto es lo que el usuario esta haciendo a mano. Lista SOLO los prestamos que tienen
--   cuota 0 Y en los que el capital pagado de la pantalla NO coincide con el capital de
--   sus cuotas liquidadas, ordenados por el tamaño de la diferencia.
--
--   La tolerancia de 0,01 evita listar diferencias de redondeo.
--
--   QUE MIRAR:
--     - APORTE_CUOTA_CERO > 0 en muchas filas -> HIPOTESIS A, y es un defecto de datos
--       o del proceso que registro ese pago.
--     - APORTE_CUOTA_CERO = 0 y DIFERENCIA negativa grande -> HIPOTESIS B: faltan pagos.
--     - Si la lista sale VACIA, el problema no es sistematico: es un caso puntual y hay
--       que mirarlo con los bloques 1 y 2.
-- =====================================================================================
SELECT prestamo,
       estado_prestamo,
       capital_pagado_pantalla,
       segun_cuotas_liquidadas,
       ROUND(capital_pagado_pantalla - segun_cuotas_liquidadas, 2) AS diferencia,
       aporte_cuota_cero,
       cuotas_liquidadas_sin_pago
  FROM (SELECT p.PRSTCDGO       AS prestamo,
               p.PRSTIDST       AS estado_prestamo,
               (SELECT NVL(SUM(g.PGPRCPPG), 0)
                  FROM CRD.PGPR g
                  JOIN CRD.DTPR dd ON dd.DTPRCDGO = g.DTPRCDGO
                 WHERE dd.PRSTCDGO = p.PRSTCDGO
                   AND NVL(g.PGPRANUL, 0) = 0)              AS capital_pagado_pantalla,
               (SELECT NVL(SUM(dd.DTPRCPTL), 0)
                  FROM CRD.DTPR dd
                 WHERE dd.PRSTCDGO = p.PRSTCDGO
                   AND dd.DTPRESTD IN (4, 7))               AS segun_cuotas_liquidadas,
               (SELECT NVL(SUM(g.PGPRCPPG), 0)
                  FROM CRD.PGPR g
                  JOIN CRD.DTPR dd ON dd.DTPRCDGO = g.DTPRCDGO
                 WHERE dd.PRSTCDGO = p.PRSTCDGO
                   AND NVL(g.PGPRANUL, 0) = 0
                   AND dd.DTPRNMCT = 0)                     AS aporte_cuota_cero,
               (SELECT COUNT(*)
                  FROM CRD.DTPR dd
                 WHERE dd.PRSTCDGO = p.PRSTCDGO
                   AND dd.DTPRESTD IN (4, 7)
                   AND NOT EXISTS (SELECT 1
                                     FROM CRD.PGPR g
                                    WHERE g.DTPRCDGO = dd.DTPRCDGO
                                      AND NVL(g.PGPRANUL, 0) = 0)) AS cuotas_liquidadas_sin_pago
          FROM CRD.PRST p
         WHERE EXISTS (SELECT 1
                         FROM CRD.DTPR dd
                        WHERE dd.PRSTCDGO = p.PRSTCDGO
                          AND dd.DTPRNMCT = 0))
 WHERE ABS(capital_pagado_pantalla - segun_cuotas_liquidadas) > 0.01
 ORDER BY ABS(capital_pagado_pantalla - segun_cuotas_liquidadas) DESC
 FETCH FIRST 100 ROWS ONLY;


-- =====================================================================================
-- BLOQUE 4 — CONTROL DE ALCANCE: ¿es exclusivo de la cuota 0?
--
--   El usuario sospecha de los prestamos con cuota 0, pero conviene saber si el desajuste
--   tambien ocurre SIN cuota 0. Si los dos grupos descuadran en proporcion parecida, la
--   cuota 0 es una coincidencia y la causa es otra.
--
--   Esperado si la sospecha es correcta: PCT_DESCUADRADOS mucho mas alto en CON_CUOTA_0.
-- =====================================================================================
SELECT grupo,
       COUNT(*)                                                         AS prestamos,
       SUM(CASE WHEN descuadra = 1 THEN 1 ELSE 0 END)                   AS descuadrados,
       ROUND(100 * SUM(CASE WHEN descuadra = 1 THEN 1 ELSE 0 END) / COUNT(*), 1) AS pct_descuadrados
  FROM (SELECT CASE WHEN EXISTS (SELECT 1 FROM CRD.DTPR dd
                                  WHERE dd.PRSTCDGO = p.PRSTCDGO AND dd.DTPRNMCT = 0)
                    THEN 'CON_CUOTA_0' ELSE 'SIN_CUOTA_0' END AS grupo,
               CASE WHEN ABS(
                      (SELECT NVL(SUM(g.PGPRCPPG), 0)
                         FROM CRD.PGPR g
                         JOIN CRD.DTPR dd ON dd.DTPRCDGO = g.DTPRCDGO
                        WHERE dd.PRSTCDGO = p.PRSTCDGO AND NVL(g.PGPRANUL, 0) = 0)
                    - (SELECT NVL(SUM(dd.DTPRCPTL), 0)
                         FROM CRD.DTPR dd
                        WHERE dd.PRSTCDGO = p.PRSTCDGO AND dd.DTPRESTD IN (4, 7))
                    ) > 0.01 THEN 1 ELSE 0 END AS descuadra
          FROM CRD.PRST p
         WHERE p.PRSTIDST IN (2, 11))
 GROUP BY grupo
 ORDER BY grupo;

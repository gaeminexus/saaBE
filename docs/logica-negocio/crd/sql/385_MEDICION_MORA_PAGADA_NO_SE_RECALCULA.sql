-- =====================================================================================
-- 385 — MEDICIÓN de la mora ya pagada (lote 3, punto 4.1, DS-06) — SOLO LECTURA
-- Equipo omen-saa-1 · 2026-11-24 · No modifica nada.
--
-- Regla C (decidida por el árbitro) de ProcesoMoraPrestamoServiceImpl.moraDeCuota, para cuotas vencidas con mora pagada
-- (DTPRMRPG > 0):
--     mora = DTPRMRPG + capital × tasa × días(desde max(vencimiento, fecha del último PGPR vigente con mora pagada) hasta la fecha)
--   con la misma base y tasa de siempre (capital = DTPRCPTL de la cuota; tasa = PRSTINNM/36000, 9 si falta). Sin capital
--   pendiente en la cuota (DTPRCPTL − DTPRCPPG <= 0,004) la mora queda en lo pagado. Con DTPRMRPG = 0, el cálculo es el de siempre.
--   Esto aplica con saa.cartera.moraSobreCapitalPendiente apagada (el defecto); si se enciende, la base es la función por tramos.
--
-- Cómo: cambiar las fechas de PARAMS y correr cada bloque. FECHA1 y FECHA2 son dos corridas consecutivas del timer.
-- =====================================================================================


-- CASO 1 — Mora pagada y SIN capital pendiente en la cuota.
-- Esperado: mora_esperada = DTPRMRPG en las dos fechas, o sea que dos corridas seguidas NO cambian DTPRMRAA
-- (la columna cambia_entre_fechas debe ser 0). Si DTPRMRAA hoy es distinto de DTPRMRPG, es la mora que el timer viejo
-- ya recargó: el primer recálculo con el código nuevo la deja en lo pagado.
WITH params AS (SELECT DATE '2026-11-24' AS fecha1, DATE '2026-11-25' AS fecha2 FROM DUAL)
SELECT p.PRSTIDAS AS operacion, d.DTPRNMCT AS cuota, d.DTPRESTD AS estado_cuota, TRUNC(d.DTPRFCVN) AS vence,
       d.DTPRCPTL AS capital, d.DTPRCPPG AS capital_pagado, d.DTPRMRPG AS mora_pagada, d.DTPRMRAA AS mora_hoy_en_tabla,
       d.DTPRMRPG AS mora_esperada_fecha1, d.DTPRMRPG AS mora_esperada_fecha2, 0 AS cambia_entre_fechas
  FROM params pa
  JOIN CRD.DTPR d ON NVL(d.DTPRESTD,0) NOT IN (4, 7, 9) AND NVL(d.DTPRMRPG,0) > 0 AND TRUNC(d.DTPRFCVN) < pa.fecha1
                 AND NVL(d.DTPRCPTL,0) - NVL(d.DTPRCPPG,0) <= 0.004
  JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO AND p.PRSTIDST IN (2, 8, 11)
 WHERE ROWNUM <= 30
 ORDER BY 1, 2;


-- CASO 2 — Mora pagada CON capital pendiente.
-- Esperado: DTPRMRAA sube solo lo de los días nuevos sobre ese capital (desde el último cobro de mora), nunca lo ya
-- pagado: mora_esperada_fecha2 − mora_esperada_fecha1 = capital × tasa × (fecha2 − fecha1) = sube_esperado, y la mora
-- esperada nunca baja de DTPRMRPG.
WITH params AS (SELECT DATE '2026-11-24' AS fecha1, DATE '2026-11-25' AS fecha2 FROM DUAL),
base AS (
    SELECT p.PRSTIDAS, d.DTPRCDGO, d.DTPRNMCT, d.DTPRESTD, TRUNC(d.DTPRFCVN) AS vence, d.DTPRCPTL, d.DTPRCPPG,
           d.DTPRMRPG, d.DTPRMRAA,
           CASE WHEN NVL(p.PRSTINNM,0) > 0 THEN p.PRSTINNM ELSE 9 END / 36000 AS tasa_diaria,
           (SELECT TRUNC(MAX(g.PGPRFCHA)) FROM CRD.PGPR g
             WHERE g.DTPRCDGO = d.DTPRCDGO AND NVL(g.PGPRANUL,0) = 0 AND NVL(g.PGPRMRPG,0) > 0) AS ultimo_cobro_mora
      FROM params pa
      JOIN CRD.DTPR d ON NVL(d.DTPRESTD,0) NOT IN (4, 7, 9) AND NVL(d.DTPRMRPG,0) > 0 AND TRUNC(d.DTPRFCVN) < pa.fecha1
                     AND NVL(d.DTPRCPTL,0) - NVL(d.DTPRCPPG,0) > 0.004
      JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO AND p.PRSTIDST IN (2, 8, 11)
)
SELECT b.PRSTIDAS AS operacion, b.DTPRNMCT AS cuota, b.vence, b.DTPRCPTL AS capital, b.DTPRCPPG AS capital_pagado,
       b.DTPRMRPG AS mora_pagada, b.DTPRMRAA AS mora_hoy_en_tabla, b.ultimo_cobro_mora,
       ROUND(b.DTPRMRPG + b.DTPRCPTL * b.tasa_diaria * GREATEST(pa.fecha1 - GREATEST(b.vence, NVL(b.ultimo_cobro_mora, b.vence)), 0), 2) AS mora_esperada_fecha1,
       ROUND(b.DTPRMRPG + b.DTPRCPTL * b.tasa_diaria * GREATEST(pa.fecha2 - GREATEST(b.vence, NVL(b.ultimo_cobro_mora, b.vence)), 0), 2) AS mora_esperada_fecha2,
       ROUND(b.DTPRCPTL * b.tasa_diaria * (pa.fecha2 - pa.fecha1), 2) AS sube_esperado,
       CASE WHEN b.ultimo_cobro_mora IS NULL THEN 'SIN PGPR CON FECHA: el código usa la fórmula de siempre' ELSE 'ok' END AS nota
  FROM params pa, base b
 WHERE ROWNUM <= 30
 ORDER BY 1, 2;

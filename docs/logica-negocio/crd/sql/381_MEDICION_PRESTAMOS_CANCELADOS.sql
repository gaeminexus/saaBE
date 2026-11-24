-- =====================================================================================
-- 381 — MEDICIÓN de préstamos cancelados por rango (SOLO LECTURA)
-- Equipo omen-saa-1 · 2026-11-24 · Contrato: docs/logica-negocio/crd/API-REPORTE-PRESTAMOS-CANCELADOS.md
--
-- Para qué: cuadrar el reporte nuevo (POST /rest/prcn/consultar) contra la base. Aplica la MISMA regla de
-- fecha de cancelación del §3 del contrato. No modifica nada: se puede correr en producción cuando se quiera.
--
-- Cómo: cambiar las dos fechas del bloque PARAMS (aparecen en los tres bloques) y correr bloque por bloque.
-- El total del BLOQUE 1 (sin filtros) tiene que ser igual al "cantidad" del reporte con los cuatro estados y
-- sin producto ni filial; el BLOQUE 3 al "canceladosSinFecha".
-- =====================================================================================


-- BLOQUE 1 — Cancelados en el rango, por estado y por origen de la fecha.
WITH params AS (
    SELECT DATE '2026-10-01' AS desde, DATE '2026-10-31' AS hasta FROM DUAL
),
base AS (
    SELECT p.PRSTCDGO, p.PRSTIDST,
           (SELECT MAX(e.EVPRFCHA) FROM CRD.EVPR e
             WHERE e.PRSTCDGO = p.PRSTCDGO AND e.EVPRESTD = 1
               AND e.EVPRTPOO IN ('NOVACION', 'REESTRUCTURACION'))                         AS f_sust,
           (SELECT MAX(e.EVPRFCHA) FROM CRD.EVPR e
             WHERE e.PRSTCDGO = p.PRSTCDGO AND e.EVPRESTD = 1
               AND e.EVPRTPOO NOT IN ('NOVACION', 'REESTRUCTURACION'))                     AS f_evento,
           (SELECT MAX(g.PGPRFCHA) FROM CRD.PGPR g
             WHERE g.PRSTCDGO = p.PRSTCDGO AND g.EVPRCDGO IS NULL AND NVL(g.PGPRANUL, 0) = 0) AS f_pago,
           (SELECT MAX(d.DTPRFCPG) FROM CRD.DTPR d
             WHERE d.PRSTCDGO = p.PRSTCDGO AND d.DTPRESTD IN (4, 7))                       AS f_cuota
      FROM CRD.PRST p
     WHERE p.PRSTIDST IN (3, 4, 5, 13)
),
derivada AS (
    SELECT b.PRSTCDGO, b.PRSTIDST,
           CASE
             WHEN b.PRSTIDST IN (5, 13) AND b.f_sust IS NOT NULL THEN TRUNC(b.f_sust)
             WHEN b.PRSTIDST IN (3, 4) AND (b.f_evento IS NOT NULL OR b.f_pago IS NOT NULL)
                  THEN TRUNC(GREATEST(NVL(b.f_evento, DATE '1900-01-01'), NVL(b.f_pago, DATE '1900-01-01')))
             ELSE TRUNC(b.f_cuota)
           END AS fecha_cancelacion,
           CASE
             WHEN b.PRSTIDST IN (5, 13) AND b.f_sust IS NOT NULL THEN 'SUSTITUCION'
             WHEN b.PRSTIDST IN (3, 4) AND b.f_evento IS NOT NULL
                  AND (b.f_pago IS NULL OR b.f_evento >= b.f_pago) THEN 'EVENTO'
             WHEN b.PRSTIDST IN (3, 4) AND b.f_pago IS NOT NULL THEN 'PAGO_CARGA_O_MIGRACION'
             WHEN b.f_cuota IS NOT NULL THEN 'CUOTA'
             ELSE 'SIN_FECHA'
           END AS origen_fecha
      FROM base b
)
SELECT d.PRSTIDST AS estado, d.origen_fecha, COUNT(*) AS cantidad
  FROM derivada d, params pr
 WHERE d.fecha_cancelacion BETWEEN pr.desde AND pr.hasta
 GROUP BY ROLLUP (d.PRSTIDST, d.origen_fecha)
 ORDER BY d.PRSTIDST, d.origen_fecha;
-- La fila con estado y origen nulos es el TOTAL: debe ser igual a "cantidad" del reporte.
-- Nota: el reporte separa PAGO_CARGA de MIGRACION por el tipo del pago; aquí van juntos.


-- BLOQUE 2 — Las filas, para contrastar préstamo por préstamo con la pantalla (mismas fechas que el bloque 1).
WITH params AS (
    SELECT DATE '2026-10-01' AS desde, DATE '2026-10-31' AS hasta FROM DUAL
),
base AS (
    SELECT p.PRSTCDGO, p.PRSTIDAS, p.PRSTIDST, p.PRSTMNSL,
           (SELECT MAX(e.EVPRFCHA) FROM CRD.EVPR e WHERE e.PRSTCDGO = p.PRSTCDGO AND e.EVPRESTD = 1
               AND e.EVPRTPOO IN ('NOVACION', 'REESTRUCTURACION'))                         AS f_sust,
           (SELECT MAX(e.EVPRFCHA) FROM CRD.EVPR e WHERE e.PRSTCDGO = p.PRSTCDGO AND e.EVPRESTD = 1
               AND e.EVPRTPOO NOT IN ('NOVACION', 'REESTRUCTURACION'))                     AS f_evento,
           (SELECT MAX(g.PGPRFCHA) FROM CRD.PGPR g WHERE g.PRSTCDGO = p.PRSTCDGO
               AND g.EVPRCDGO IS NULL AND NVL(g.PGPRANUL, 0) = 0)                          AS f_pago,
           (SELECT MAX(d.DTPRFCPG) FROM CRD.DTPR d WHERE d.PRSTCDGO = p.PRSTCDGO
               AND d.DTPRESTD IN (4, 7))                                                   AS f_cuota,
           (SELECT ROUND(NVL(SUM(g.PGPRVLRR), 0), 2) FROM CRD.PGPR g WHERE g.PRSTCDGO = p.PRSTCDGO
               AND NVL(g.PGPRANUL, 0) = 0)                                                 AS total_pagado
      FROM CRD.PRST p
     WHERE p.PRSTIDST IN (3, 4, 5, 13)
)
SELECT b.PRSTCDGO, b.PRSTIDAS, b.PRSTIDST, b.PRSTMNSL, b.total_pagado,
       CASE
         WHEN b.PRSTIDST IN (5, 13) AND b.f_sust IS NOT NULL THEN TRUNC(b.f_sust)
         WHEN b.PRSTIDST IN (3, 4) AND (b.f_evento IS NOT NULL OR b.f_pago IS NOT NULL)
              THEN TRUNC(GREATEST(NVL(b.f_evento, DATE '1900-01-01'), NVL(b.f_pago, DATE '1900-01-01')))
         ELSE TRUNC(b.f_cuota)
       END AS fecha_cancelacion
  FROM base b, params pr
 WHERE CASE
         WHEN b.PRSTIDST IN (5, 13) AND b.f_sust IS NOT NULL THEN TRUNC(b.f_sust)
         WHEN b.PRSTIDST IN (3, 4) AND (b.f_evento IS NOT NULL OR b.f_pago IS NOT NULL)
              THEN TRUNC(GREATEST(NVL(b.f_evento, DATE '1900-01-01'), NVL(b.f_pago, DATE '1900-01-01')))
         ELSE TRUNC(b.f_cuota)
       END BETWEEN pr.desde AND pr.hasta
 ORDER BY fecha_cancelacion, b.PRSTIDAS;


-- BLOQUE 3 — Cancelados SIN ninguna fecha derivable (no entran a ningún rango del reporte).
SELECT p.PRSTIDST AS estado, COUNT(*) AS cantidad
  FROM CRD.PRST p
 WHERE p.PRSTIDST IN (3, 4, 5, 13)
   AND NOT EXISTS (SELECT 1 FROM CRD.EVPR e WHERE e.PRSTCDGO = p.PRSTCDGO AND e.EVPRESTD = 1)
   AND NOT EXISTS (SELECT 1 FROM CRD.PGPR g WHERE g.PRSTCDGO = p.PRSTCDGO
                      AND g.EVPRCDGO IS NULL AND NVL(g.PGPRANUL, 0) = 0)
   AND NOT EXISTS (SELECT 1 FROM CRD.DTPR d WHERE d.PRSTCDGO = p.PRSTCDGO
                      AND d.DTPRESTD IN (4, 7) AND d.DTPRFCPG IS NOT NULL)
 GROUP BY ROLLUP (p.PRSTIDST)
 ORDER BY p.PRSTIDST;
-- Aproximación por estado: para 5/13 cuenta como "con fecha" un EVPR de cualquier tipo, igual que el reporte
-- solo si es de sustitución. Si el bloque 3 y el reporte difieren en 5/13, revisar a mano esos préstamos.

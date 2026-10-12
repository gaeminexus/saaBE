-- =====================================================================================
-- 321 — MEDICIÓN: precancelaciones mixtas (depósito + aportes) y el capital futuro
-- Equipo omen-saa-1 · 2026-10-12 · SOLO LECTURA (ningún bloque escribe)
--
-- Por qué: el ítem 0 del frente «abono a capital con aportes»
-- (DISENO-ABONO-CAPITAL-CON-APORTES.md §4) midió en el código que el asiento de reparto
-- de un cobro CBCR debita la transitoria por (depósito − capital futuro), y el capital
-- futuro incluye lo pagado con aportes. En una precancelación mixta con aportes grandes
-- eso da negativo, y el cuadre revienta («El asiento de reparto del cobro N no cuadra»)
-- y el cobro no se puede procesar. Como esa excepción hace rollback, el cobro queda
-- APROBADO (2) sin procesar. Esto mide si ya pasó en producción.
--
-- Correr de corrido. Devolver al árbitro la salida de los bloques 1, 2 y 3.
-- =====================================================================================

-- BLOQUE 1 — Todas las precancelaciones con aportes consumidos (CRD.DAPR), por estado.
--   Estados de CBCRESTD: 1 REGISTRADO · 2 APROBADO · 3 PROCESADO · 4 RECHAZADO · 5 ANULADO
SELECT c.CBCRCDGO                         AS cobro,
       c.CBCRESTD                         AS estado,
       c.CBCRFCHA                         AS fecha,
       c.CBCRVLRR                         AS deposito,
       (SELECT NVL(SUM(a.DAPRVLOR), 0)
          FROM CRD.DAPR a JOIN CRD.DCBC d ON d.DCBCCDGO = a.DCBCCDGO
         WHERE d.CBCRCDGO = c.CBCRCDGO)   AS aportes,
       c.CBCRASN1                         AS asiento_transitorio,
       c.CBCRASRP                         AS asiento_reparto,
       c.CBCRASN2                         AS asiento_definitivo,
       c.CBCRUSRC                         AS rechazado_por,
       SUBSTR(c.CBCRMTRC, 1, 200)         AS motivo_rechazo
  FROM CRD.CBCR c
 WHERE c.CBCRTPOO = 'PRECANCELACION'
   AND EXISTS (SELECT 1 FROM CRD.DAPR a JOIN CRD.DCBC d ON d.DCBCCDGO = a.DCBCCDGO
                WHERE d.CBCRCDGO = c.CBCRCDGO)
 ORDER BY c.CBCRFCHA, c.CBCRCDGO;

-- BLOQUE 2 — Las que están APROBADAS y no se procesaron (posibles atascadas por el defecto).
SELECT c.CBCRCDGO AS cobro, c.CBCRFCHA AS fecha, c.CBCRFCAP AS aprobado_el,
       c.CBCRVLRR AS deposito, c.CBCRUSAP AS aprobado_por
  FROM CRD.CBCR c
 WHERE c.CBCRTPOO = 'PRECANCELACION'
   AND c.CBCRESTD = 2
   AND EXISTS (SELECT 1 FROM CRD.DAPR a JOIN CRD.DCBC d ON d.DCBCCDGO = a.DCBCCDGO
                WHERE d.CBCRCDGO = c.CBCRCDGO)
 ORDER BY c.CBCRFCHA;

-- BLOQUE 3 — De las PROCESADAS: los totales de sus asientos de reparto y definitivo.
--   Si alguna línea de reparto tiene DEBE negativo, o un asiento no cuadra, se marca.
SELECT c.CBCRCDGO AS cobro,
       'REPARTO'  AS asiento,
       x.ASNTNMRO AS numero,
       SUM(NVL(t.DTASDBEE, 0)) AS debe,
       SUM(NVL(t.DTASHBRR, 0)) AS haber,
       MIN(NVL(t.DTASDBEE, 0)) AS debe_minimo_linea
  FROM CRD.CBCR c
  JOIN CNT.ASNT x ON x.ASNTCDGO = c.CBCRASRP
  JOIN CNT.DTAS t ON t.ASNTCDGO = x.ASNTCDGO
 WHERE c.CBCRTPOO = 'PRECANCELACION' AND c.CBCRESTD = 3
   AND EXISTS (SELECT 1 FROM CRD.DAPR a JOIN CRD.DCBC d ON d.DCBCCDGO = a.DCBCCDGO
                WHERE d.CBCRCDGO = c.CBCRCDGO)
 GROUP BY c.CBCRCDGO, x.ASNTNMRO
UNION ALL
SELECT c.CBCRCDGO, 'DEFINITIVO', x.ASNTNMRO,
       SUM(NVL(t.DTASDBEE, 0)), SUM(NVL(t.DTASHBRR, 0)), MIN(NVL(t.DTASDBEE, 0))
  FROM CRD.CBCR c
  JOIN CNT.ASNT x ON x.ASNTCDGO = c.CBCRASN2
  JOIN CNT.DTAS t ON t.ASNTCDGO = x.ASNTCDGO
 WHERE c.CBCRTPOO = 'PRECANCELACION' AND c.CBCRESTD = 3
   AND EXISTS (SELECT 1 FROM CRD.DAPR a JOIN CRD.DCBC d ON d.DCBCCDGO = a.DCBCCDGO
                WHERE d.CBCRCDGO = c.CBCRCDGO)
 GROUP BY c.CBCRCDGO, x.ASNTNMRO
 ORDER BY 1, 2;

-- Lectura esperada:
--  · Bloque 1 vacío → nunca hubo precancelación mixta; el defecto no tocó producción.
--  · Bloque 2 con filas → cobros atascados: no reintentar hasta que salga la corrección
--    (frente abono con aportes, ítem 1). Avisar al árbitro.
--  · Bloque 3: un cobro sin asiento de reparto es normal si se procesó con la contabilidad
--    apagada. debe_minimo_linea < 0 no debería verse (el cuadre lo impide).

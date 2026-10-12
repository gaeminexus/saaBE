-- =====================================================================================
-- e2-101 — Retenciones de septiembre con código 332 al 0%, que el validador del ATS rechaza. SOLO LECTURA.
-- Equipo omen-saa-2 · 2026-10-12.
--
-- El validador del SRI, sobre el ATS de 2026/09: «Para el CÓDIGO RETENCIÓN [332] El PORCENTAJE DE RETENCIÓN EN
-- LA FUENTE DE IMPUESTO A LA RENTA reportado [0.00] no es el esperado para el periodo declarado [1.00]».
--
-- El generador NO inventa ese 0%: copia la línea de renta de la retención emitida (CBR.DRV2, CODIMPUESTO '1':
-- CODRETENCION, PORCENTAJERETEN; GeneradorAtsServiceImpl ~l.1576). O sea que el 0% está en las retenciones
-- que ya se emitieron y autorizaron en el SRI. Corregirlo SOLO en el ATS dejaría el anexo distinto de los
-- comprobantes de retención. Qué código y qué porcentaje correspondían lo decide contabilidad, no el sistema.
-- Este script da la lista completa para esa decisión. NO corrige nada.
-- Columnas copiadas de RetencionV2 (CBR.RTV2) y DetalleRetencionV2 (CBR.DRV2).
-- =====================================================================================

-- BLOQUE 1 — Todas las líneas de renta con código 332 en retenciones de septiembre. ESPERADO: las ~13 del
-- validador (notas de venta y algunas facturas). Si salen más, el validador cortó la lista.
SELECT 'BLOQUE 1 - 332 de septiembre' AS bloque, r.ID AS id_retencion, r.NUMERO AS retencion, TRUNC(r.FECHA) AS fecha,
       r.ESTADO, r.ESTADOEMISION, d.NUMDOCRETEN AS documento_sustento, d.CODRETENCION, d.BASEIMPONIBLE,
       d.PORCENTAJERETEN, d.VALORRETEN
  FROM CBR.DRV2 d
  JOIN CBR.RTV2 r ON r.ID = d.RETENCIONV2
 WHERE d.CODIMPUESTO = '1'
   AND TRIM(d.CODRETENCION) = '332'
   AND r.FECHA >= DATE '2026-09-01' AND r.FECHA < DATE '2026-10-01'
 ORDER BY r.FECHA, r.ID;

-- BLOQUE 2 — Qué códigos de renta se usaron en septiembre y con qué porcentaje. ESPERADO: ver si hay otros
-- códigos con un porcentaje que no corresponda a la tabla vigente de 2026 (el validador solo listó el 332).
SELECT 'BLOQUE 2 - codigos de renta' AS bloque, TRIM(d.CODRETENCION) AS codigo, d.PORCENTAJERETEN, COUNT(*) AS lineas,
       SUM(d.BASEIMPONIBLE) AS base, SUM(d.VALORRETEN) AS retenido
  FROM CBR.DRV2 d
  JOIN CBR.RTV2 r ON r.ID = d.RETENCIONV2
 WHERE d.CODIMPUESTO = '1'
   AND r.FECHA >= DATE '2026-09-01' AND r.FECHA < DATE '2026-10-01'
 GROUP BY TRIM(d.CODRETENCION), d.PORCENTAJERETEN
 ORDER BY codigo, d.PORCENTAJERETEN;

-- =====================================================================================================
-- 366 — DIAGNÓSTICO (SOLO LECTURA): cancelados con cuotas PAGADAS sin fecha de pago
-- Consulta del agente del cuadre (C23 / D-052), guardada por omen-saa-1-arb · 2026-11-05
--
-- Para decidir la variante NVL(DTPRFCPG, DTPRFCVN) de CANCELADO_TERMINADO_AL_CORTE_SQL (reproceso):
--   * si «TODAS pagadas sin fecha (riesgo)» no existe o es solo «antes de jun-2025» → la variante va tal cual;
--   * si existe con fechas posteriores → la variante solo para cancelados con canceladas anticipadas, y los de
--     «todas pagadas sin fecha» van al aviso para depurarlos.
-- No toca producción mientras saa.cartera.alCorte = nunca. No modifica nada.
-- =====================================================================================================
WITH c AS (
  SELECT p.PRSTCDGO, p.PRSTIDST,
         SUM(CASE WHEN d.DTPRESTD = 4 AND d.DTPRFCPG IS NULL THEN 1 ELSE 0 END) pagadas_sin_fecha,
         SUM(CASE WHEN d.DTPRESTD = 4 AND d.DTPRFCPG IS NOT NULL THEN 1 ELSE 0 END) pagadas_con_fecha,
         SUM(CASE WHEN d.DTPRESTD = 7 THEN 1 ELSE 0 END) canceladas_anticipadas,
         SUM(CASE WHEN NVL(d.DTPRESTD,0) NOT IN (4, 7, 9) THEN 1 ELSE 0 END) otras,
         MAX(CASE WHEN d.DTPRESTD = 4 THEN NVL(d.DTPRFCPG, d.DTPRFCVN) END) ultima_pagada_variante
    FROM CRD.PRST p JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO
   WHERE NVL(p.PRSTIDST,0) IN (3, 4, 5, 9, 13)
   GROUP BY p.PRSTCDGO, p.PRSTIDST),
k AS (
  SELECT PRSTIDST estado,
         CASE WHEN otras > 0 THEN 'con cuotas en otro estado (aviso)'
              WHEN pagadas_sin_fecha > 0 AND canceladas_anticipadas = 0 AND pagadas_con_fecha = 0 THEN 'TODAS pagadas sin fecha (riesgo)'
              WHEN pagadas_sin_fecha > 0 THEN 'pagadas sin fecha + canceladas anticipadas o con fecha'
              ELSE 'sin pagadas sin fecha' END patron,
         CASE WHEN ultima_pagada_variante IS NULL THEN 'sin cuota pagada'
              WHEN ultima_pagada_variante < DATE '2025-06-01' THEN 'antes de jun-2025'
              WHEN ultima_pagada_variante <= DATE '2026-10-31' THEN 'jun-2025 a oct-2026'
              ELSE 'despues de oct-2026' END ultima_pagada
    FROM c)
SELECT estado, patron, ultima_pagada, COUNT(*) prestamos FROM k GROUP BY estado, patron, ultima_pagada ORDER BY 1, 2, 3;

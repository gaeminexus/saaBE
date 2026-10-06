-- =====================================================================================
-- 320 — MEDICION (solo lectura): cuanto se condono de mas y cuanto pago de mas el participe en los 8
--       acuerdos aplicados con cuotas futuras (sql/319)
-- FECHA: 2026-10-06 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- El adeudado de cada concepto (DACCVLAD) incluyo lo de las cuotas futuras (F). Lo realmente exigible es
-- E = DACCVLAD - F. Con P = pagado y C = condonado del concepto (P + C = E + F):
--   * condonacion correcta = max(0, E - P)
--   * condonado de mas (gasto de condonacion inflado)  = C - condonacion correcta
--   * pagado de mas por el participe                    = max(0, P - E)
-- Desgravamen y seguro no son condonables: se pagaron al 100%, incluida la parte futura (pagado de mas).
-- Las cuotas futuras = las CANCELADAS ANTICIPADAS (7) que vencian despues de ACCNFCHA.
-- =====================================================================================

WITH fut AS (
    SELECT a.ACCNCDGO,
           SUM(NVL(d.DTPRCPTL, 0))           AS CAPITAL_F,
           SUM(NVL(d.DTPRINTR, 0))           AS INTERES_F,
           SUM(NVL(d.DTPRDSGR, 0))           AS DESGRAVAMEN_F,
           SUM(NVL(d.DTPRVLSI, 0))           AS SEGURO_F
      FROM CRD.ACCN a JOIN CRD.DTPR d ON d.PRSTCDGO = a.PRSTCDGO
     WHERE a.ACCNESTD = 2 AND d.DTPRESTD = 7 AND d.DTPRFCVN > a.ACCNFCHA
     GROUP BY a.ACCNCDGO
),
con AS (
    SELECT x.ACCNCDGO, x.DACCCPTO AS CONCEPTO, NVL(x.DACCVLAD,0) AS ADEUDADO, NVL(x.DACCVLPG,0) AS PAGADO,
           NVL(x.DACCVLCN,0) AS CONDONADO
      FROM CRD.DACC x
)
SELECT c.ACCNCDGO AS ACUERDO, NVL(p.PRSTIDAS, p.PRSTCDGO) AS PRESTAMO, a.ACCNFCHA, e.EVPRNMAS AS ASIENTO_EVENTO,
       c.CONCEPTO,               -- 1 capital, 2 interes, 3 mora, 4 desgravamen, 5 seguro
       c.ADEUDADO, c.PAGADO, c.CONDONADO,
       ROUND(CASE c.CONCEPTO WHEN 2 THEN f.INTERES_F WHEN 4 THEN f.DESGRAVAMEN_F WHEN 5 THEN f.SEGURO_F ELSE 0 END, 2) AS FUTURO,
       ROUND(c.ADEUDADO - CASE c.CONCEPTO WHEN 2 THEN f.INTERES_F WHEN 4 THEN f.DESGRAVAMEN_F
                                          WHEN 5 THEN f.SEGURO_F ELSE 0 END, 2)                     AS EXIGIBLE,
       ROUND(GREATEST(0, (c.ADEUDADO - CASE c.CONCEPTO WHEN 2 THEN f.INTERES_F WHEN 4 THEN f.DESGRAVAMEN_F
                                          WHEN 5 THEN f.SEGURO_F ELSE 0 END) - c.PAGADO), 2)        AS CONDONACION_CORRECTA,
       ROUND(c.CONDONADO - GREATEST(0, (c.ADEUDADO - CASE c.CONCEPTO WHEN 2 THEN f.INTERES_F WHEN 4 THEN f.DESGRAVAMEN_F
                                          WHEN 5 THEN f.SEGURO_F ELSE 0 END) - c.PAGADO), 2)        AS CONDONADO_DE_MAS,
       ROUND(GREATEST(0, c.PAGADO - (c.ADEUDADO - CASE c.CONCEPTO WHEN 2 THEN f.INTERES_F WHEN 4 THEN f.DESGRAVAMEN_F
                                          WHEN 5 THEN f.SEGURO_F ELSE 0 END)), 2)                   AS PAGADO_DE_MAS
  FROM con c
  JOIN fut f ON f.ACCNCDGO = c.ACCNCDGO
  JOIN CRD.ACCN a ON a.ACCNCDGO = c.ACCNCDGO
  JOIN CRD.PRST p ON p.PRSTCDGO = a.PRSTCDGO
  LEFT JOIN CRD.EVPR e ON e.EVPRCDGO = a.EVPRCDGO
 ORDER BY c.ACCNCDGO, c.CONCEPTO;

-- =====================================================================================
-- 305 — MEDICION: por que la precancelacion del prestamo 7912 (cobro 149) no cuadra por 1,76
-- FECHA: 2026-09-30 · EQUIPO: omen-saa-1 (CRD · EQUIPO B) · solo lectura
--
-- ERROR: «el prestamo 7912 tiene 7 cuota(s) en estado CANCELADA_ANTICIPADA(7) que suman $372.54 de
-- capital, pero no coincide con el capital futuro del pago ($370.78)».
-- Causa sospechada (ContabilizacionIndividualCreditoServiceImpl:352-368): el control suma el
-- capital COMPLETO de las cuotas en 7; el capital futuro se calculo con lo que FALTABA pagar
-- (capital − capital pagado). Este script muestra cuota por cuota cual tenia capital ya pagado.
-- ⚠️ 7912 es PRSTCDGO (el codigo interno que muestra el mensaje), no el numero ASOPREP.
-- =====================================================================================
WITH pg AS (
    SELECT x.DTPRCDGO,
           SUM(NVL(x.PGPRCPPG, 0)) AS CAP_PAG,
           SUM(NVL(x.PGPRSLOT, 0)) AS EXTRA,
           COUNT(*) AS PAGOS
      FROM CRD.PGPR x WHERE NVL(x.PGPRANUL, 0) = 0 GROUP BY x.DTPRCDGO
)
SELECT d.DTPRNMCT AS CUOTA, d.DTPRESTD AS ESTADO, d.DTPRFCVN AS VENCE,
       ROUND(NVL(d.DTPRCPTL,0), 2)                 AS CAPITAL,
       ROUND(NVL(g.CAP_PAG,0), 2)                  AS CAPITAL_PAGADO,
       ROUND(NVL(d.DTPRCPTL,0) - NVL(g.CAP_PAG,0), 2) AS CAPITAL_PENDIENTE,
       ROUND(NVL(g.EXTRA,0), 2)                    AS PAGO_EXTRA,
       NVL(g.PAGOS, 0)                             AS PAGOS,
       ROUND(NVL(d.DTPRSLOT,0), 2)                 AS DTPR_SALDO_OTROS
  FROM CRD.DTPR d
  LEFT JOIN pg g ON g.DTPRCDGO = d.DTPRCDGO
 WHERE d.PRSTCDGO = 7912
 ORDER BY d.DTPRNMCT;

-- Lectura: la suma de CAPITAL_PENDIENTE de las cuotas en estado 7 (mas la cuota ancla, si el
-- capital futuro se registro en una cuota que era futura) tiene que dar 370,78. La diferencia de
-- 1,76 aparece como CAPITAL_PAGADO > 0 en alguna cuota en estado 7.

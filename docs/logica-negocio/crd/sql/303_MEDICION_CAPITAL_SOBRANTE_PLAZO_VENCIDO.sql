-- =====================================================================================
-- 303 — MEDICION: prestamos donde cobrado + saldo de capital da MAS que el monto (solo lectura)
-- FECHA: 2026-09-30 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- EL CASO: la pantalla de Plazo Vencido deshabilita 61538, 65991, 64561, 63392 y 62890 con
-- «saldo de capital ≠ monto − capital cobrado», pero al reves que el 62439: aca SOBRA
-- (154,39 · 1.629,27 · 84,39 · 37,92 · 44,35). Dos hipotesis que este script separa:
--   H1. Una o mas cuotas recibieron MAS capital pagado que su capital (sobrepago). El calculo
--       pone el saldo de esa cuota en 0 (piso), asi que el exceso no descuenta y "sobra".
--       → el bloque 1 muestra esas cuotas: SOBREPAGO > 0.
--   H2. La tabla de amortizacion suma MAS que el monto (tabla rehecha / cargada mal).
--       → el bloque 2 compara Σ capital de la tabla contra el monto.
-- Con la salida se decide la regla. Nada se cambia antes.
--
-- SOLO LECTURA. SQL PURO. Correr los dos bloques y pasar la salida completa al arbitro.
-- =====================================================================================


-- =====================================================================================
-- 1. CUOTAS CON CAPITAL SOBREPAGADO (H1) — una fila por cuota con pagado > capital
-- =====================================================================================
WITH pg AS (
    SELECT x.DTPRCDGO,
           SUM(NVL(x.PGPRCPPG, 0)) AS CAP_PAG,
           SUM(NVL(x.PGPRSLOT, 0)) AS EXTRA,
           COUNT(*)                AS PAGOS
      FROM CRD.PGPR x
     WHERE NVL(x.PGPRANUL, 0) = 0
     GROUP BY x.DTPRCDGO
)
SELECT NVL(p.PRSTIDAS, p.PRSTCDGO) AS PRESTAMO,
       d.DTPRNMCT                  AS CUOTA,
       d.DTPRESTD                  AS ESTADO_CUOTA,
       d.DTPRFCVN                  AS VENCE,
       ROUND(NVL(d.DTPRCPTL, 0), 2) AS CAPITAL_CUOTA,
       ROUND(NVL(g.CAP_PAG, 0), 2)  AS CAPITAL_PAGADO,
       ROUND(NVL(g.CAP_PAG, 0) - NVL(d.DTPRCPTL, 0), 2) AS SOBREPAGO,
       ROUND(NVL(g.EXTRA, 0), 2)    AS PAGO_EXTRA,
       g.PAGOS
  FROM CRD.PRST p
  JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO
  JOIN pg g       ON g.DTPRCDGO = d.DTPRCDGO
 WHERE NVL(p.PRSTIDAS, p.PRSTCDGO) IN (61538, 65991, 64561, 63392, 62890)
   AND NVL(d.DTPRESTD, 0) <> 7
   AND NVL(g.CAP_PAG, 0) - NVL(d.DTPRCPTL, 0) > 0.01
 ORDER BY 1, 2;


-- =====================================================================================
-- 2. LA TABLA CONTRA EL MONTO (H2) — una fila por prestamo
-- =====================================================================================
WITH pg AS (
    SELECT x.DTPRCDGO,
           SUM(NVL(x.PGPRCPPG, 0)) AS CAP_PAG,
           SUM(NVL(x.PGPRSLOT, 0)) AS EXTRA
      FROM CRD.PGPR x
     WHERE NVL(x.PGPRANUL, 0) = 0
     GROUP BY x.DTPRCDGO
)
SELECT NVL(p.PRSTIDAS, p.PRSTCDGO)                     AS PRESTAMO,
       ROUND(NVL(p.PRSTMNSL, 0), 2)                    AS MONTO,
       COUNT(*)                                        AS CUOTAS,
       ROUND(SUM(CASE WHEN NVL(d.DTPRESTD,0) <> 7 THEN NVL(d.DTPRCPTL,0) ELSE 0 END), 2) AS CAPITAL_TABLA,
       ROUND(SUM(CASE WHEN d.DTPRESTD = 7 THEN NVL(d.DTPRCPTL,0) ELSE 0 END), 2)        AS CAPITAL_EN_CANCELADAS,
       ROUND(SUM(CASE WHEN NVL(d.DTPRESTD,0) <> 7 THEN NVL(g.CAP_PAG,0) ELSE 0 END), 2)  AS CAPITAL_PAGADO,
       ROUND(SUM(CASE WHEN NVL(d.DTPRESTD,0) <> 7 THEN NVL(g.EXTRA,0) ELSE 0 END), 2)    AS PAGO_EXTRA,
       ROUND(SUM(CASE WHEN d.DTPRESTD = 7 THEN NVL(g.CAP_PAG,0) + NVL(g.EXTRA,0) ELSE 0 END), 2)
                                                       AS PAGADO_EN_CANCELADAS,
       ROUND(SUM(CASE WHEN NVL(d.DTPRESTD,0) <> 7 THEN NVL(d.DTPRCPTL,0) ELSE 0 END)
             - NVL(p.PRSTMNSL, 0), 2)                  AS TABLA_MENOS_MONTO,
       ROUND(SUM(CASE WHEN NVL(d.DTPRESTD,0) <> 7
                      THEN GREATEST(0, NVL(d.DTPRCPTL,0) - NVL(g.CAP_PAG,0)) ELSE 0 END), 2) AS SALDO_CON_PISO,
       ROUND(SUM(CASE WHEN NVL(d.DTPRESTD,0) <> 7
                      THEN NVL(d.DTPRCPTL,0) - NVL(g.CAP_PAG,0) ELSE 0 END), 2)              AS SALDO_SIN_PISO
  FROM CRD.PRST p
  JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO
  LEFT JOIN pg g  ON g.DTPRCDGO = d.DTPRCDGO
 WHERE NVL(p.PRSTIDAS, p.PRSTCDGO) IN (61538, 65991, 64561, 63392, 62890)
 GROUP BY NVL(p.PRSTIDAS, p.PRSTCDGO), p.PRSTMNSL
 ORDER BY 1;

-- LECTURA:
--   * Si el bloque 1 trae filas y SALDO_SIN_PISO del bloque 2 cuadra con monto − cobrado → H1:
--     el capital sobrepagado de una cuota debe descontar del saldo del prestamo (sin piso por
--     cuota para el CAPITAL). Es un ajuste de regla, no de datos.
--   * Si TABLA_MENOS_MONTO ≠ 0 → H2: la tabla no suma el monto. Hay que revisar esa tabla.
--   * PAGADO_EN_CANCELADAS > 0 → pagos sobre cuotas CANCELADA_ANTICIPADA que el calculo hoy no
--     cuenta. Tercera causa posible.

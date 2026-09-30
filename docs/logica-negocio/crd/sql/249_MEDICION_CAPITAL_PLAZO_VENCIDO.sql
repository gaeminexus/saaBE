-- =====================================================================================
-- 249 — MEDICION: por que los prestamos en mora fallan la invariante de capital (solo lectura)
-- FECHA: 2026-09-30 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- EL CASO: la pantalla de Plazo Vencido deshabilita el prestamo 62439 (JARA MARTINEZ FAUSTO
-- HERNAN, hipotecario, 2017) con:
--     devengado 100.020,83 ≠ cobrado + saldo 95.784,03
--     saldo de capital 88.592,22 ≠ monto − capital cobrado 92.829,02
-- Con esas cifras, lo cobrado por PGPR es 7.191,81 y a la tabla le faltan 4.236,80 de capital
-- para sumar el monto. Hipotesis que este script mide, sin elegir de antemano:
--   H1. La TABLA no suma el monto: cuotas CANCELADA_ANTICIPADA (7) con capital, un abono
--       que rehizo la tabla, o una tabla migrada que arranca en un saldo y no en el monto.
--   H2. Lo COBRADO no esta completo en PGPR: pagos historicos (migrados, anteriores al SAA)
--       que solo quedaron en las columnas *PG de DTPR, o cuotas PAGADAS sin fila en PGPR.
-- De la respuesta depende la regla. Por eso no se cambia el codigo antes de correr esto.
--
-- ✅ ACTUALIZADO 2026-09-30, con la tabla de amortizacion del 62439 que paso el usuario: la
--    causa de ESE caso es un PAGO EXTRA (abono a capital) de 4.236,80 en la cuota 38, que se
--    graba en PGPRSLOT / DTPRSLOT y NO en capital pagado. La regla se corrige para sumarlo. Los
--    bloques 2 y 3 ya lo suman: lo que sigan mostrando como fallas son OTRAS causas.
--
-- SOLO LECTURA. SQL PURO. Correr los tres bloques y pasar la salida al arbitro.
-- ⚠️ ULTIMO NUMERO del rango 200-249 de omen-saa-1: el proximo script necesita rango nuevo.
-- =====================================================================================


-- =====================================================================================
-- 1. EL PRESTAMO 62439, CUOTA POR CUOTA AGRUPADA POR ESTADO
--    Por cada estado de cuota: cuantas, capital de la tabla, capital pagado segun DTPR
--    (DTPRCPPG) y segun PGPR, y cuantas cuotas no tienen NINGUN pago en PGPR.
-- =====================================================================================
SELECT d.DTPRESTD                                   AS ESTADO_CUOTA,
       COUNT(*)                                     AS CUOTAS,
       MIN(d.DTPRNMCT)                              AS DESDE_CUOTA,
       MAX(d.DTPRNMCT)                              AS HASTA_CUOTA,
       ROUND(SUM(NVL(d.DTPRCPTL, 0)), 2)            AS CAPITAL_TABLA,
       ROUND(SUM(NVL(d.DTPRCPPG, 0)), 2)            AS CAPITAL_PAGADO_DTPR,
       ROUND(SUM(NVL(g.CAP, 0)), 2)                 AS CAPITAL_PAGADO_PGPR,
       SUM(CASE WHEN g.DTPRCDGO IS NULL THEN 1 ELSE 0 END) AS CUOTAS_SIN_PGPR
  FROM CRD.PRST p
  JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO
  LEFT JOIN (SELECT x.DTPRCDGO, SUM(NVL(x.PGPRCPPG, 0)) AS CAP
               FROM CRD.PGPR x WHERE NVL(x.PGPRANUL, 0) = 0 GROUP BY x.DTPRCDGO) g
         ON g.DTPRCDGO = d.DTPRCDGO
 WHERE p.PRSTIDAS = 62439 OR (p.PRSTIDAS IS NULL AND p.PRSTCDGO = 62439)
 GROUP BY d.DTPRESTD
 ORDER BY d.DTPRESTD;

-- 1b. La cabecera del mismo prestamo: monto, plazo y cuantas cuotas tiene la tabla.
SELECT p.PRSTCDGO, p.PRSTIDAS, p.PRSTMNSL AS MONTO, p.PRSTPLZO AS PLAZO, p.PRSTIDST,
       p.PRSTFCIN AS FECHA_INICIO,
       (SELECT COUNT(*) FROM CRD.DTPR d WHERE d.PRSTCDGO = p.PRSTCDGO) AS CUOTAS_EN_TABLA,
       (SELECT MIN(d.DTPRNMCT) FROM CRD.DTPR d WHERE d.PRSTCDGO = p.PRSTCDGO) AS PRIMERA_CUOTA
  FROM CRD.PRST p
 WHERE p.PRSTIDAS = 62439 OR (p.PRSTIDAS IS NULL AND p.PRSTCDGO = 62439);


-- =====================================================================================
-- 2. TODOS LOS PRESTAMOS EN MORA (PRSTIDST = 11) — el tamano del problema
--    Una fila con los conteos. Es lo que decide si la regla se ajusta o si son casos aislados.
-- =====================================================================================
WITH pg AS (
    SELECT x.DTPRCDGO, SUM(NVL(x.PGPRCPPG, 0) + NVL(x.PGPRSLOT, 0)) AS CAP, SUM(NVL(x.PGPRSLOT, 0)) AS EXTRA
      FROM CRD.PGPR x WHERE NVL(x.PGPRANUL, 0) = 0 GROUP BY x.DTPRCDGO
),
por_prestamo AS (
    SELECT p.PRSTCDGO,
           NVL(p.PRSTMNSL, 0) AS MONTO,
           SUM(CASE WHEN NVL(d.DTPRESTD,0) <> 7 THEN NVL(d.DTPRCPTL,0) ELSE 0 END) AS CAP_TABLA,
           SUM(CASE WHEN d.DTPRESTD = 7        THEN NVL(d.DTPRCPTL,0) ELSE 0 END) AS CAP_EN_CANCELADAS,
           SUM(CASE WHEN NVL(d.DTPRESTD,0) <> 7 THEN NVL(g.CAP,0)      ELSE 0 END) AS COBRADO_PGPR,
           SUM(CASE WHEN NVL(d.DTPRESTD,0) <> 7 THEN NVL(d.DTPRCPPG,0) + NVL(d.DTPRSLOT,0) ELSE 0 END) AS COBRADO_DTPR,
           SUM(CASE WHEN NVL(d.DTPRESTD,0) <> 7
                    THEN GREATEST(0, NVL(d.DTPRCPTL,0) - (NVL(g.CAP,0) - NVL(g.EXTRA,0))) ELSE 0 END)  AS SALDO_REGLA_ACTUAL,
           SUM(CASE WHEN d.DTPRESTD = 4 AND g.DTPRCDGO IS NULL THEN 1 ELSE 0 END)  AS PAGADAS_SIN_PGPR
      FROM CRD.PRST p
      JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO
      LEFT JOIN pg g ON g.DTPRCDGO = d.DTPRCDGO
     WHERE p.PRSTIDST = 11
     GROUP BY p.PRSTCDGO, p.PRSTMNSL
)
SELECT COUNT(*)                                                                AS PRESTAMOS_EN_MORA,
       SUM(CASE WHEN ABS(SALDO_REGLA_ACTUAL - (MONTO - COBRADO_PGPR)) > 0.01 THEN 1 ELSE 0 END)
                                                                               AS FALLAN_HOY,
       SUM(CASE WHEN ABS(CAP_TABLA - MONTO) > 0.01 THEN 1 ELSE 0 END)          AS TABLA_NO_SUMA_MONTO_SIN_EXTRA,
       SUM(CASE WHEN ABS(CAP_TABLA + CAP_EN_CANCELADAS - MONTO) > 0.01 THEN 1 ELSE 0 END)
                                                                               AS NO_SUMA_NI_CON_CANCELADAS,
       SUM(CASE WHEN PAGADAS_SIN_PGPR > 0 THEN 1 ELSE 0 END)                   AS CON_PAGADAS_SIN_PGPR,
       SUM(CASE WHEN ABS(COBRADO_DTPR - COBRADO_PGPR) > 0.01 THEN 1 ELSE 0 END) AS PGPR_DISTINTO_DE_DTPR
  FROM por_prestamo;


-- =====================================================================================
-- 3. EL DETALLE DE LOS QUE FALLAN — las 40 diferencias mas grandes
-- =====================================================================================
WITH pg AS (
    SELECT x.DTPRCDGO, SUM(NVL(x.PGPRCPPG, 0) + NVL(x.PGPRSLOT, 0)) AS CAP, SUM(NVL(x.PGPRSLOT, 0)) AS EXTRA
      FROM CRD.PGPR x WHERE NVL(x.PGPRANUL, 0) = 0 GROUP BY x.DTPRCDGO
),
por_prestamo AS (
    SELECT p.PRSTCDGO, p.PRSTIDAS, p.PRSTFCIN,
           NVL(p.PRSTMNSL, 0) AS MONTO,
           COUNT(*)           AS CUOTAS,
           SUM(CASE WHEN NVL(d.DTPRESTD,0) <> 7 THEN NVL(d.DTPRCPTL,0) ELSE 0 END) AS CAP_TABLA,
           SUM(CASE WHEN d.DTPRESTD = 7        THEN NVL(d.DTPRCPTL,0) ELSE 0 END) AS CAP_EN_CANCELADAS,
           SUM(CASE WHEN NVL(d.DTPRESTD,0) <> 7 THEN NVL(g.CAP,0)      ELSE 0 END) AS COBRADO_PGPR,
           SUM(CASE WHEN NVL(d.DTPRESTD,0) <> 7 THEN NVL(d.DTPRCPPG,0) + NVL(d.DTPRSLOT,0) ELSE 0 END) AS COBRADO_DTPR,
           SUM(CASE WHEN NVL(d.DTPRESTD,0) <> 7
                    THEN GREATEST(0, NVL(d.DTPRCPTL,0) - (NVL(g.CAP,0) - NVL(g.EXTRA,0))) ELSE 0 END)  AS SALDO_REGLA_ACTUAL,
           SUM(CASE WHEN d.DTPRESTD = 4 AND g.DTPRCDGO IS NULL THEN 1 ELSE 0 END)  AS PAGADAS_SIN_PGPR,
           SUM(CASE WHEN d.DTPRESTD = 4 THEN 1 ELSE 0 END)                         AS PAGADAS
      FROM CRD.PRST p
      JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO
      LEFT JOIN pg g ON g.DTPRCDGO = d.DTPRCDGO
     WHERE p.PRSTIDST = 11
     GROUP BY p.PRSTCDGO, p.PRSTIDAS, p.PRSTFCIN, p.PRSTMNSL
)
SELECT * FROM (
    SELECT NVL(PRSTIDAS, PRSTCDGO) AS PRESTAMO, PRSTFCIN, MONTO, CUOTAS, PAGADAS,
           ROUND(CAP_TABLA, 2)          AS CAP_TABLA,
           ROUND(CAP_EN_CANCELADAS, 2)  AS CAP_CANCELADAS,
           ROUND(COBRADO_PGPR, 2)       AS COBRADO_PGPR,
           ROUND(COBRADO_DTPR, 2)       AS COBRADO_DTPR,
           PAGADAS_SIN_PGPR,
           ROUND(SALDO_REGLA_ACTUAL, 2) AS SALDO_ACTUAL,
           ROUND(MONTO - COBRADO_PGPR, 2) AS MONTO_MENOS_COBRADO,
           ROUND(SALDO_REGLA_ACTUAL - (MONTO - COBRADO_PGPR), 2) AS DIFERENCIA
      FROM por_prestamo
     WHERE ABS(SALDO_REGLA_ACTUAL - (MONTO - COBRADO_PGPR)) > 0.01
     ORDER BY ABS(SALDO_REGLA_ACTUAL - (MONTO - COBRADO_PGPR)) DESC
) WHERE ROWNUM <= 40;

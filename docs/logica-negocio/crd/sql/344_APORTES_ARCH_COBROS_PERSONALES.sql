-- =====================================================================================
-- 344 — SOLO LECTURA. omen-saa-1, 2026-10-31.
-- Los 7 partícipes de ARCH (filial 2): sus aportes desde 2025-06, para ver cómo quedaron
-- registrados los meses de junio a septiembre 2026 que, según el usuario, se cargaron A MANO
-- por COBROS PERSONALES. Con el 343, el aporte de octubre de la carga 510 de todos cayó en
-- JUNIO 2026: el reparto ve junio-septiembre incompletos por «período efectivo»
-- (APRTPRDV, o si es nulo el mes de APRTFCTR). Si los cobros personales grabaron esos meses sin
-- APRTPRDV y con fecha de caja de otro mes, no cuentan para junio-septiembre.
-- Pasar al árbitro la salida de los 2 bloques.
-- =====================================================================================

-- 1. Todos los aportes 9/11 de ARCH desde 2025-06 (por fecha de caja o devengo)
SELECT e.ENTDRLPC AS ROL, e.ENTDRZNS, a.APRTCDGO, a.TPAPCDGO, a.APRTVLRR, a.APRTPRDV,
       a.APRTFCTR, a.APRTTPMV, a.APRTIDAS, a.CRARCDGO, a.APRTUSRG, a.APRTFCRG, a.APRTIDST,
       SUBSTR(a.APRTGLSA, 1, 120) AS GLOSA,
       (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
             WHEN a.APRTVLRR > 0 THEN TRUNC(a.APRTFCTR, 'MM') END) AS PERIODO_EFECTIVO
  FROM CRD.APRT a
  JOIN CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
 WHERE e.FLLLCDGO = 2
   AND a.TPAPCDGO IN (9, 11)
   AND (a.APRTPRDV >= DATE '2025-06-01' OR a.APRTFCTR >= DATE '2025-06-01')
 ORDER BY e.ENTDRLPC, a.TPAPCDGO, NVL(a.APRTPRDV, TRUNC(a.APRTFCTR, 'MM')), a.APRTCDGO;

-- 2. Resumen por partícipe, tipo y período efectivo (lo que ve el reparto)
SELECT e.ENTDRLPC AS ROL, a.TPAPCDGO,
       TO_CHAR(CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                    WHEN a.APRTVLRR > 0 THEN TRUNC(a.APRTFCTR, 'MM') END, 'YYYY-MM') AS PERIODO,
       COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS TOTAL
  FROM CRD.APRT a
  JOIN CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
 WHERE e.FLLLCDGO = 2
   AND a.TPAPCDGO IN (9, 11)
   AND (a.APRTPRDV >= DATE '2025-06-01' OR a.APRTFCTR >= DATE '2025-06-01')
 GROUP BY e.ENTDRLPC, a.TPAPCDGO,
          TO_CHAR(CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                       WHEN a.APRTVLRR > 0 THEN TRUNC(a.APRTFCTR, 'MM') END, 'YYYY-MM')
 ORDER BY 1, 2, 3;

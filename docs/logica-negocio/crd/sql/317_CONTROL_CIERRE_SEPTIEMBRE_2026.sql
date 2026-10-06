-- =====================================================================================
-- 317 — CONTROL (solo lectura): el cierre de septiembre 2026 / apertura de octubre
-- FECHA: 2026-10-05 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
-- Primer cierre con provision de intereses (paso 7), libro MVIC y plazo vencido incluido (D27).
-- =====================================================================================

-- 1. La corrida. Esperado: 1 fila, estado 2 (EJECUTADA).
SELECT c.CRCTCDGO, c.CRCTANOO, c.CRCTMESS, c.CRCTFCCR, c.CRCTFCPR, c.CRCTIDST, c.CRCTFCRG
  FROM CRD.CRCT c WHERE c.CRCTANOO = 2026 AND c.CRCTMESS = 9;

-- 2. Sus asientos, uno por sub-proceso. Esperado: hasta 7 filas, incluida la 7 (provision).
SELECT a.ANCCTPOO AS SUBPROCESO, a.ANCCASNT AS ASIENTO, a.ANCCNMAS AS NUMERO, a.ANCCVLRR AS VALOR, a.ANCCIDST, a.ANCCFCHA
  FROM CRD.ANCC a JOIN CRD.CRCT c ON c.CRCTCDGO = a.CRCTCDGO
 WHERE c.CRCTANOO = 2026 AND c.CRCTMESS = 9
 ORDER BY a.ANCCTPOO;

-- 3. El libro MVIC de la corrida: tipo 1 (provision) y tipo 5 (mora devengada), por componente.
SELECT m.MVICTPMV AS TIPO, m.MVICCMPN AS COMPONENTE, COUNT(*) AS FILAS, ROUND(SUM(m.MVICVLOR), 2) AS TOTAL,
       MIN(m.MVICFCCT) AS FECHA
  FROM CRD.MVIC m JOIN CRD.CRCT c ON c.CRCTCDGO = m.CRCTCDGO
 WHERE c.CRCTANOO = 2026 AND c.CRCTMESS = 9 AND m.MVICANUL = 0
 GROUP BY m.MVICTPMV, m.MVICCMPN
 ORDER BY 1, 2;

-- 4. Provision por tipo de prestamo y estado del prestamo (para ver cuanto aportan los de plazo vencido).
SELECT pr.TPPRCDGO AS TIPO_PRESTAMO, p.PRSTIDST AS ESTADO, m.MVICCMPN AS COMPONENTE,
       ROUND(SUM(m.MVICVLOR), 2) AS PROVISIONADO
  FROM CRD.MVIC m
  JOIN CRD.CRCT c ON c.CRCTCDGO = m.CRCTCDGO
  JOIN CRD.PRST p ON p.PRSTCDGO = m.PRSTCDGO
  JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
 WHERE c.CRCTANOO = 2026 AND c.CRCTMESS = 9 AND m.MVICANUL = 0 AND m.MVICTPMV = 1
 GROUP BY pr.TPPRCDGO, p.PRSTIDST, m.MVICCMPN
 ORDER BY 1, 2, 3;

-- =====================================================================================
-- c20-01 — SOLO LECTURA. omen-saa-1, 2026-11-01. Desde P1+P2, los controles de cierre leen:
--   - los grupos de la ecuación contable del rubro 250 (detalles 8 a 12);
--   - las 4 cuentas puente de la plantilla alterno 1 (aux1 1 a 4).
-- Si falta algo, el control queda en ADVERTENCIA «No se pudo medir» (no se cae, pero no controla).
-- ESPERADO: bloque 1 = 5 filas (8=5, 9=4, 10=1, 11=2, 12=3); bloque 2 = 4 filas
-- (1=1.4.05.05, 2=1.4.05.10, 3=2.3.02.05, 4=2.3.02.10).
-- =====================================================================================

-- 1. Rubro 250: grupos de nivel 1
SELECT d.PDTRALTR AS DETALLE, d.PDTRDSCR, d.PDTRVLRV AS GRUPO
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 250 AND d.PDTRESTD = 1 AND d.PDTRALTR BETWEEN 8 AND 12
 ORDER BY d.PDTRALTR;

-- 2. Plantilla alterno 1 (apertura de la planilla mensual), líneas 1 a 4, empresa 1236
SELECT d.DTPLAXL1 AS AUX1, n.PLNNCNTA AS CUENTA, d.DTPLDSCR, d.DTPLESTD
  FROM CNT.PLNS p JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
  LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PLNSCDAL = 1 AND p.PJRQCDGO = 1236 AND d.DTPLAXL1 BETWEEN 1 AND 4
 ORDER BY d.DTPLAXL1;

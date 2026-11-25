-- 404 - MEDICIÓN (solo lectura): cuánto cambió el reparto por COMPONENTE la re-imputación C33 del 2026-11-25, por tipo de préstamo y por préstamo.
-- Uso: (1) saber si la diferencia del ⑨ (H y Q) viene del reparto y (2) base del asiento de reclasificación (un asiento por corrida, por plantilla).
-- Definición: componente de los pagos NUEVOS (marca «C33-REIMPUTADO de P» en la observación, vigentes) menos el de los ANULADOS por la re-imputación
-- (motivo C33-REIMPUTADO, anulados el 25-nov). La suma total debe ser 0: el dinero no cambia, cambia el reparto. Excluir el préstamo 3492 si ya se revirtió (402).
DEFINE dia = '2026-11-25'

-- A. Por componente (total de la corrida). Esperado según el JSON de la corrida: interés +1976,37; capital -1590,33; mora -308,53; seguro -182,40; desgravamen +104,89.
WITH nuevos AS (
  SELECT d.PRSTCDGO, g.PGPRVLSI seguro, g.PGPRDSGR desgravamen, g.PGPRMRPG mora, g.PGPRINVP iv, g.PGPRINPG interes, g.PGPRCPPG capital
    FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
   WHERE NVL(g.PGPRANUL,0) = 0 AND g.PGPROBSR LIKE '%C33-REIMPUTADO de P%' AND g.PGPRFCRG >= TO_DATE('&dia','YYYY-MM-DD')),
anulados AS (
  SELECT d.PRSTCDGO, g.PGPRVLSI seguro, g.PGPRDSGR desgravamen, g.PGPRMRPG mora, g.PGPRINVP iv, g.PGPRINPG interes, g.PGPRCPPG capital
    FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
   WHERE NVL(g.PGPRANUL,0) = 1 AND g.PGPRMTAN LIKE '%C33-REIMPUTADO%' AND g.PGPRFCAN >= TO_DATE('&dia','YYYY-MM-DD'))
SELECT 'seguro' componente, ROUND(NVL((SELECT SUM(seguro) FROM nuevos),0) - NVL((SELECT SUM(seguro) FROM anulados),0),2) delta FROM DUAL UNION ALL
SELECT 'desgravamen', ROUND(NVL((SELECT SUM(desgravamen) FROM nuevos),0) - NVL((SELECT SUM(desgravamen) FROM anulados),0),2) FROM DUAL UNION ALL
SELECT 'mora', ROUND(NVL((SELECT SUM(mora) FROM nuevos),0) - NVL((SELECT SUM(mora) FROM anulados),0),2) FROM DUAL UNION ALL
SELECT 'interes vencido', ROUND(NVL((SELECT SUM(iv) FROM nuevos),0) - NVL((SELECT SUM(iv) FROM anulados),0),2) FROM DUAL UNION ALL
SELECT 'interes', ROUND(NVL((SELECT SUM(interes) FROM nuevos),0) - NVL((SELECT SUM(interes) FROM anulados),0),2) FROM DUAL UNION ALL
SELECT 'capital', ROUND(NVL((SELECT SUM(capital) FROM nuevos),0) - NVL((SELECT SUM(capital) FROM anulados),0),2) FROM DUAL;

-- B. Por TIPO DE PRÉSTAMO (producto): base del asiento por plantilla (las cuentas son por tipo de préstamo). Una fila por tipo.
-- (Si PRDC.PRDCCDGO / PRDC.TPPRCDGO no son los nombres de la base, ajustar: es el enlace préstamo -> producto -> tipo de préstamo.)
WITH mov AS (
  SELECT d.PRSTCDGO, +1 signo, g.PGPRVLSI seguro, g.PGPRDSGR desgravamen, g.PGPRMRPG mora, g.PGPRINVP iv, g.PGPRINPG interes, g.PGPRCPPG capital
    FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
   WHERE NVL(g.PGPRANUL,0) = 0 AND g.PGPROBSR LIKE '%C33-REIMPUTADO de P%' AND g.PGPRFCRG >= TO_DATE('&dia','YYYY-MM-DD')
  UNION ALL
  SELECT d.PRSTCDGO, -1, g.PGPRVLSI, g.PGPRDSGR, g.PGPRMRPG, g.PGPRINVP, g.PGPRINPG, g.PGPRCPPG
    FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
   WHERE NVL(g.PGPRANUL,0) = 1 AND g.PGPRMTAN LIKE '%C33-REIMPUTADO%' AND g.PGPRFCAN >= TO_DATE('&dia','YYYY-MM-DD'))
SELECT pr.TPPRCDGO AS tipo_prestamo, COUNT(DISTINCT m.PRSTCDGO) AS prestamos,
       ROUND(SUM(m.signo * NVL(m.seguro,0)),2) seguro, ROUND(SUM(m.signo * NVL(m.desgravamen,0)),2) desgravamen, ROUND(SUM(m.signo * NVL(m.mora,0)),2) mora,
       ROUND(SUM(m.signo * NVL(m.iv,0)),2) interes_vencido, ROUND(SUM(m.signo * NVL(m.interes,0)),2) interes, ROUND(SUM(m.signo * NVL(m.capital,0)),2) capital
  FROM mov m JOIN CRD.PRST p ON p.PRSTCDGO = m.PRSTCDGO JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
 GROUP BY pr.TPPRCDGO ORDER BY 1;

-- C. Los 30 préstamos con mayor cambio de reparto (para que el cuadre los mire): Σ|delta de componente|.
WITH mov AS (
  SELECT d.PRSTCDGO, +1 signo, g.PGPRVLSI seguro, g.PGPRDSGR desgravamen, g.PGPRMRPG mora, g.PGPRINPG interes, g.PGPRCPPG capital
    FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
   WHERE NVL(g.PGPRANUL,0) = 0 AND g.PGPROBSR LIKE '%C33-REIMPUTADO de P%' AND g.PGPRFCRG >= TO_DATE('&dia','YYYY-MM-DD')
  UNION ALL
  SELECT d.PRSTCDGO, -1, g.PGPRVLSI, g.PGPRDSGR, g.PGPRMRPG, g.PGPRINPG, g.PGPRCPPG
    FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
   WHERE NVL(g.PGPRANUL,0) = 1 AND g.PGPRMTAN LIKE '%C33-REIMPUTADO%' AND g.PGPRFCAN >= TO_DATE('&dia','YYYY-MM-DD'))
SELECT * FROM (
  SELECT m.PRSTCDGO, ROUND(SUM(m.signo*NVL(m.seguro,0)),2) seguro, ROUND(SUM(m.signo*NVL(m.desgravamen,0)),2) desgravamen, ROUND(SUM(m.signo*NVL(m.mora,0)),2) mora,
         ROUND(SUM(m.signo*NVL(m.interes,0)),2) interes, ROUND(SUM(m.signo*NVL(m.capital,0)),2) capital,
         ROUND(ABS(SUM(m.signo*NVL(m.seguro,0)))+ABS(SUM(m.signo*NVL(m.desgravamen,0)))+ABS(SUM(m.signo*NVL(m.mora,0)))+ABS(SUM(m.signo*NVL(m.interes,0)))+ABS(SUM(m.signo*NVL(m.capital,0))),2) magnitud
    FROM mov m GROUP BY m.PRSTCDGO ORDER BY magnitud DESC) WHERE ROWNUM <= 30;

-- =====================================================================================
-- 359 — SOLO LECTURA. omen-saa-1, 2026-11-01. Plan cuentas a plantilla, P4 (alcance de cartera del cierre).
-- Hoy el cierre de cartera y sus controles usan «1.3.», «1.3.99» y los rangos 01-04 = Q, 05-08 = P,
-- 09-12 = H escritos en el código. La propuesta deriva:
--   B) la provisión, de las cuentas de la línea 83 de la plantilla 37;
--   C) el grupo de cada cuenta de banda, del tipo de préstamo de su producto (BNDP → CBPR → PRDC).
-- B y C SOLO se implementan si estas consultas confirman que el resultado no cambia.
-- Corte: el 2026-10-01 corresponde al cierre de septiembre (último cerrado); cambiarlo si hace falta.
-- Consultas propuestas por omen-saa-1-be; columnas tomadas de las entidades.
-- =====================================================================================

-- 1) ¿La agrupación derivada da los MISMOS grupos que los rangos? ESPERADO: todas las filas IGUAL.
WITH cuentas AS (
  SELECT n.PLNNCNTA AS CUENTA, TO_NUMBER(SUBSTR(n.PLNNCNTA, 5, 2)) AS NN, p.TPPRCDGO AS TIPO
    FROM CRD.BNDP b
    JOIN CRD.CBPR c ON c.CBPRCDGO = b.CBPRCDGO
    JOIN CRD.PRDC p ON p.PRDCCDGO = c.PRDCCDGO
    JOIN CNT.PLNN n ON n.PLNNCDGO = b.PLNNCDGO
   WHERE b.BNDPESTD = 1 AND c.CBPRESTD = 1 AND n.PJRQCDGO = 1236
     AND n.PLNNCNTA LIKE '1.3.__.%' AND n.PLNNCNTA NOT LIKE '1.3.99%')
SELECT CUENTA, NN,
       CASE WHEN NN BETWEEN 1 AND 4 THEN 'Q' WHEN NN BETWEEN 5 AND 8 THEN 'P' WHEN NN BETWEEN 9 AND 12 THEN 'H' ELSE 'OTRA' END AS GRUPO_POR_RANGO,
       LISTAGG(DISTINCT TIPO, ',') WITHIN GROUP (ORDER BY TIPO) AS TIPOS_DE_PRESTAMO,
       CASE WHEN MIN(TIPO) = MAX(TIPO) THEN CASE MIN(TIPO) WHEN 1 THEN 'Q' WHEN 3 THEN 'P' WHEN 2 THEN 'H' ELSE '?' END ELSE 'MIXTO' END AS GRUPO_DERIVADO,
       CASE WHEN MIN(TIPO) = MAX(TIPO)
             AND CASE MIN(TIPO) WHEN 1 THEN 'Q' WHEN 3 THEN 'P' WHEN 2 THEN 'H' ELSE '?' END
               = CASE WHEN NN BETWEEN 1 AND 4 THEN 'Q' WHEN NN BETWEEN 5 AND 8 THEN 'P' WHEN NN BETWEEN 9 AND 12 THEN 'H' ELSE 'OTRA' END
            THEN 'IGUAL' ELSE 'DISTINTO' END AS RESULTADO
  FROM cuentas
 GROUP BY CUENTA, NN
 ORDER BY CUENTA;

-- 2) Cuentas 1.3.xx (no 1.3.99) con movimiento que NO están en ninguna banda activa. ESPERADO: 0 filas.
SELECT d.DTASCNTA AS CUENTA, SUM(NVL(d.DTASDBEE, 0)) - SUM(NVL(d.DTASHBRR, 0)) AS SALDO_DEUDOR, COUNT(*) AS MOVIMIENTOS
  FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
 WHERE a.PJRQCDGO = 1236 AND a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < DATE '2026-10-01'
   AND d.DTASCNTA LIKE '1.3.%' AND d.DTASCNTA NOT LIKE '1.3.99%'
   AND d.DTASCNTA NOT IN (SELECT n.PLNNCNTA FROM CRD.BNDP b JOIN CNT.PLNN n ON n.PLNNCDGO = b.PLNNCDGO
                           WHERE b.BNDPESTD = 1 AND n.PJRQCDGO = 1236)
 GROUP BY d.DTASCNTA
 ORDER BY 1;

-- 3) Cuentas 1.3.99% con movimiento frente a las de la línea 83 de la plantilla 37.
--    ESPERADO: todas EN_PLANTILLA.
SELECT d.DTASCNTA AS CUENTA, SUM(NVL(d.DTASHBRR, 0)) - SUM(NVL(d.DTASDBEE, 0)) AS SALDO_ACREEDOR,
       CASE WHEN d.DTASCNTA IN (SELECT n.PLNNCNTA FROM CNT.PLNS p JOIN CNT.DTPL l ON l.PLNSCDGO = p.PLNSCDGO
                                  JOIN CNT.PLNN n ON n.PLNNCDGO = l.PLNNCDGO
                                 WHERE p.PLNSCDAL = 37 AND p.PJRQCDGO = 1236 AND l.DTPLAXL1 = 83)
            THEN 'EN_PLANTILLA' ELSE 'FUERA' END AS RESULTADO
  FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
 WHERE a.PJRQCDGO = 1236 AND a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < DATE '2026-10-01' AND d.DTASCNTA LIKE '1.3.99%'
 GROUP BY d.DTASCNTA ORDER BY 1;

-- 4) Productos con banda vigente sin tipo de préstamo. ESPERADO: 0 filas.
SELECT p.PRDCCDGO, p.TPPRCDGO FROM CRD.PRDC p
 WHERE p.TPPRCDGO IS NULL AND p.PRDCCDGO IN (SELECT c.PRDCCDGO FROM CRD.CBPR c WHERE c.CBPRESTD = 1);

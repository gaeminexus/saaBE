-- Control mensual: cuentas de movimiento del SAA con saldo contrario a su naturaleza al corte (24-nov-2026).
-- El control del B17 (cuadre-integral-mes.js, D-150) revisa los renglones de la Super, que suman varias cuentas del SAA:
-- una subcuenta negativa puede quedar escondida en un renglón positivo. Así pasó con 2.5.15 LIQUIDACION DE HABERES POR
-- PAGAR (−9.871,49 dentro del 2501 de septiembre 2025), que se vio en los estados financieros.
-- Regla: activo (1.x) con saldo acreedor, salvo correctoras (…99…); pasivo (2.x) o patrimonio (3.x) con saldo deudor.
-- Esperado: 0 filas antes de cerrar el mes. Cada fila se corrige o se explica con su novedad.
-- Cambiar la fecha de corte (DATE '2025-09-30') por la del mes.
WITH S AS (
  SELECT N.PLNNCNTA CUENTA, N.PLNNNMBR NOMBRE, ROUND(SUM(NVL(D.DTASDBEE, 0) - NVL(D.DTASHBRR, 0)), 2) SALDO_DEUDOR
  FROM CNT.DTAS D
  JOIN CNT.ASNT A ON A.ASNTCDGO = D.ASNTCDGO
  JOIN CNT.PLNN N ON N.PLNNCDGO = D.PLNNCDGO
  WHERE A.ASNTFCHA <= DATE '2025-09-30' AND A.ASNTESTD <> 2 AND N.PLNNTPOO = 2 AND N.PJRQCDGO = 1236
  GROUP BY N.PLNNCNTA, N.PLNNNMBR)
SELECT CUENTA, NOMBRE, SALDO_DEUDOR,
       CASE WHEN CUENTA LIKE '1.%' THEN 'ACTIVO CON SALDO ACREEDOR' ELSE 'PASIVO O PATRIMONIO CON SALDO DEUDOR' END PROBLEMA
FROM S
WHERE (CUENTA LIKE '1.%' AND CUENTA NOT LIKE '%.99%' AND SALDO_DEUDOR < -0.004)
   OR ((CUENTA LIKE '2.%' OR CUENTA LIKE '3.%') AND SALDO_DEUDOR > 0.004)
ORDER BY CUENTA;

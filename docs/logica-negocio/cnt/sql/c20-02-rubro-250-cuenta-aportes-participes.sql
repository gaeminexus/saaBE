-- =====================================================================================
-- c20-02 — SEMILLA de DOS filas (detalles 16 y 17 del rubro 250). omen-saa-1, 2026-11-01. Plan cuentas a plantilla, P8.
-- El control de cierre B17_21_VS_G42 tenía el literal "2.1." (cuenta 21 «aportes de los partícipes» del
-- B17, contra la G42). Ahora lee el detalle 16 del rubro 250 (CntParametroEstadoFinanciero
-- .CUENTA_APORTES_PARTICIPES). Se siembra con el MISMO valor de hoy, '2.1': el control mide lo mismo.
--
-- Sin esta fila, con el EAR que trae P8, el control queda en ADVERTENCIA «No se pudo medir» (no se cae).
-- Va antes del EAR (o justo después; no rompe nada, pero hasta sembrarla el cierre no mide B17 contra G42).
--
-- El rubro se busca por su ALTERNO (PRBRALTR = 250), que es como lo lee el código: en producción el
-- PRBRCDGO de un rubro puede no coincidir con su alterno (pasó con el 249 → PRBR 312 en el pase).
-- PDTRCDGO: no hay secuencia; siguiente libre (MAX + 1), como C19-04 y c12-01b.
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES
-- 0.1 El rubro 250 existe. ESPERADO: 1 fila. (!) Si sale 0, PARAR: falta el C19-04 en esta base.
SELECT r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR FROM SCP.PRBR r WHERE r.PRBRALTR = 250;
-- 0.2 Detalles actuales del rubro 250. El 16 y el 17 NO deben existir.
SELECT d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRV, d.PDTRESTD
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 250 ORDER BY d.PDTRALTR;
-- 0.3 La cuenta 2.1 existe en el plan de la empresa. ESPERADO: 1 fila.
SELECT n.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR FROM CNT.PLNN n WHERE n.PJRQCDGO = 1236 AND n.PLNNCNTA = '2.1';


-- BLOQUE 1 — (ESCRIBE) El NOT EXISTS impide duplicarla.
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
SELECT (SELECT NVL(MAX(PDTRCDGO), 0) + 1 FROM SCP.PDTR), r.PRBRCDGO,
       'CUENTA NIVEL 2 DE LOS APORTES DE LOS PARTICIPES (B17 21)', NULL, '2.1', 16, 1
  FROM SCP.PRBR r
 WHERE r.PRBRALTR = 250
   AND NOT EXISTS (SELECT 1 FROM SCP.PDTR d WHERE d.PRBRCDGO = r.PRBRCDGO AND d.PDTRALTR = 16);
-- (!) "1 fila insertada".

-- P4-A (agregado 2026-11-01): raíz de la cartera de créditos (B17 cuenta 13), detalle 17, valor '1.3'
-- (CntParametroEstadoFinanciero.CUENTA_CARTERA_CREDITOS). Mismo valor que hoy está en el código.
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
SELECT (SELECT NVL(MAX(PDTRCDGO), 0) + 1 FROM SCP.PDTR), r.PRBRCDGO,
       'CUENTA NIVEL 2 DE LA CARTERA DE CREDITOS (B17 13)', NULL, '1.3', 17, 1
  FROM SCP.PRBR r
 WHERE r.PRBRALTR = 250
   AND NOT EXISTS (SELECT 1 FROM SCP.PDTR d WHERE d.PRBRCDGO = r.PRBRCDGO AND d.PDTRALTR = 17);
-- (!) "1 fila insertada".
COMMIT;


-- BLOQUE 2 — CONTROL DESPUÉS. ESPERADO: 2 filas: 16 = '2.1' y 17 = '1.3', las dos con PDTRESTD = 1.
SELECT d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRV, d.PDTRESTD
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 250 AND d.PDTRALTR IN (16, 17);


-- REVERSO (comentado):
-- DELETE FROM SCP.PDTR d WHERE d.PDTRALTR IN (16, 17)
--    AND d.PRBRCDGO = (SELECT r.PRBRCDGO FROM SCP.PRBR r WHERE r.PRBRALTR = 250);
-- COMMIT;

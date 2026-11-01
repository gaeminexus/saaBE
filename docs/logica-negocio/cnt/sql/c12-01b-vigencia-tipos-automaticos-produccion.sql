-- =====================================================================================
-- c12-01b — Vigencia de la regla IN-A/EG-A/DI-A = 2025-06-01 para la empresa 1236, versión PARA
-- PRODUCCIÓN del c12-01 del cuadre. omen-saa-1, 2026-11-01 (pase de la versión fusionada, paso 5.6).
--
-- Por qué una versión aparte: el c12-01 inserta el detalle del rubro 249 con PDTRCDGO FIJO = 1518. En
-- producción el C19-04 (paso 5.5) ya tomó 1518..1532 para el rubro 250 → el INSERT del c12-01 fallaría
-- por clave duplicada. El código NO busca el detalle por PDTRCDGO sino por el rubro (alterno 249) y la
-- empresa (PDTRALTR = 1236): AsientoContableServiceImpl.fechaVigenciaTiposAutomaticos. Así que el
-- detalle toma el siguiente código libre, igual que hizo el C19-04 (MAX + 1).
-- El PRBR 249 sí va fijo: el código lo lee por alterno 249 y el c12-01 usa PRBRCDGO = PRBRALTR = 249.
--
-- Decisión del usuario (2026-11-01): la regla rige desde 2025-06-01; la contabilidad de producción sale
-- desde el pase con la numeración nueva y después se reprocesa hasta noviembre 2026.
-- COMMIT a mano después del CONTROL DESPUÉS.
-- =====================================================================================

-- CONTROL ANTES
-- 1. El rubro 249 no existe (ni como código ni como alterno). ESPERADO: 0 filas.
SELECT r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR FROM SCP.PRBR r WHERE r.PRBRCDGO = 249 OR r.PRBRALTR = 249;
-- 2. Los tipos de asiento 7, 8 y 9 (IN-A, EG-A, DI-A) de la 1236 existen y son de sistema
--    (PLNTSSTM = 1). ESPERADO: 3 filas con PLNTSSTM = 1. Si no, PARAR: todo asiento automático fallaría.
SELECT t.PLNTCDAL AS alterno, t.PLNTCDGO, t.PLNTNMBR, t.PLNTSSTM, t.PLNTESTD
  FROM CNT.PLNT t WHERE t.PJRQCDGO = 1236 AND t.PLNTCDAL IN (7, 8, 9) ORDER BY t.PLNTCDAL;
-- 3. Siguiente código libre de detalle (informativo).
SELECT NVL(MAX(PDTRCDGO), 0) + 1 AS siguiente_pdtr FROM SCP.PDTR;

-- CAMBIO
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (249, 'CNT PARAMETROS ASIENTOS AUTOMATICOS (vigencia IN-A/EG-A/DI-A por empresa)', SYSDATE, 249, 1);

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
SELECT NVL(MAX(PDTRCDGO), 0) + 1, 249,
       'VIGENCIA TIPOS AUTOMATICOS EMPRESA 1236 (yyyy-MM-dd en valor alfanumerico)', NULL, '2025-06-01', 1236, 1
  FROM SCP.PDTR;

-- CONTROL DESPUÉS
-- ESPERADO: 1 fila, rubro 249, empresa 1236, vigencia 2025-06-01, fecha válida, estado 1.
SELECT r.PRBRCDGO, r.PRBRALTR, d.PDTRCDGO, d.PDTRALTR AS empresa, d.PDTRVLRV AS vigencia,
       TO_DATE(d.PDTRVLRV, 'YYYY-MM-DD') AS fecha_valida, d.PDTRESTD
  FROM SCP.PRBR r JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
 WHERE r.PRBRALTR = 249;

-- COMMIT;   <- darlo a mano si el control está bien

-- REVERSO (comentado; sin el rubro rige otra vez 2026-11-01)
-- DELETE FROM SCP.PDTR WHERE PRBRCDGO = 249;
-- DELETE FROM SCP.PRBR WHERE PRBRCDGO = 249;
-- COMMIT;

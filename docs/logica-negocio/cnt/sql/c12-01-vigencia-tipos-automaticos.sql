-- =====================================================================================
-- c12-01 — Vigencia parametrizable de la regla de tipos de asiento automaticos IN-A / EG-A / DI-A
--          (modulo origen y numeracion mensual por modulo) para la empresa 1236: 2025-06-01.
-- Cuadre Superintendencia, tarea C12 · 2026-10-28 · Decision D-006 del lider del proyecto
-- (cuadreSuper/01-seguimiento/DECISIONES-Y-PARTIDAS.md) · Diseño: DISENO-TIPOS-ASIENTO-AUTOMATICOS.md §7.
--
-- QUE HACE: crea el rubro 249 (Rubros.CNT_PARAMETROS_ASIENTOS_AUTOMATICOS) y UN detalle por empresa:
--   PDTRALTR = PJRQCDGO de la empresa (1236), PDTRVLRV = fecha de vigencia 'yyyy-MM-dd', PDTRESTD = 1.
--   AsientoContableServiceImpl.fechaVigenciaTiposAutomaticos(idEmpresa) lee el detalle de la empresa; si no hay,
--   el detalle 0 (todas las empresas); si no hay ninguno, usa TipoAsientoAutomatico.FECHA_VIGENCIA_DEFECTO
--   (2026-11-01). Sin este script el sistema se comporta exactamente igual que antes.
--
-- SQL PURO (sin SET/DEFINE/WHENEVER) y SIN COMMIT: lo revisa y lo confirma quien lo corre, en la base de trabajo
-- del proyecto (saa-oracle-23ai). NO SE HA EJECUTADO. En produccion NO se corre hasta que el lider lo decida:
-- cambiaria el tipo de los asientos nuevos con fecha anterior al 2026-11-01 (hoy ninguno: la operacion va al dia).
--
-- ⛔ Va DESPUES del e2-111 (columnas BNCOREAL / ASNTMDOR / ASNTNMMD / ASNTNAMD) y CON el WAR que lee el rubro 249.
--    Con el WAR anterior la fila no hace daño: nadie la lee.
--
-- PK explicitos, mismo patron que 235..248 (PRBRCDGO = PRBRALTR; sin secuencia SQ_PRBRCDGO/SQ_PDTRCDGO en esta
-- base, verificado en ALL_SEQUENCES el 2026-10-28: SCP no tiene ninguna de las dos).
-- Verificado el 2026-10-28 en la base de trabajo: MAX(PRBRCDGO) = 311 (245-248 crd, 310-311 rhh; 330-349
-- reservados por lap-saa-1 en Rubros.java), 249 libre; MAX(PDTRCDGO) = 1517, 1518 libre.
-- =====================================================================================

-- BLOQUE 1 — CONTROL ANTES
-- 1.1 El rubro 249 no debe existir (ni por PK ni por alterno). ESPERADO: 0 filas.
SELECT 'PRBR' AS tabla, r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR
  FROM SCP.PRBR r WHERE r.PRBRCDGO = 249 OR r.PRBRALTR = 249;

-- 1.2 El PK 1518 del detalle debe estar libre. ESPERADO: 0 filas.
SELECT 'PDTR' AS tabla, d.PDTRCDGO, d.PRBRCDGO, d.PDTRDSCR FROM SCP.PDTR d WHERE d.PDTRCDGO = 1518;

-- 1.3 Maximos actuales (ESPERADO: 311 y 1517; si son mayores, ajustar los PK de abajo al siguiente libre).
SELECT (SELECT MAX(PRBRCDGO) FROM SCP.PRBR) AS max_prbrcdgo,
       (SELECT MAX(PRBRALTR) FROM SCP.PRBR) AS max_prbraltr,
       (SELECT MAX(PDTRCDGO) FROM SCP.PDTR) AS max_pdtrcdgo
  FROM DUAL;

-- 1.4 Lo que la regla necesita en la 1236 para fechas de 2025 (solo informativo; se verifica aparte en C12):
--     tipos 7/8/9 del sistema activos (ESPERADO: 3 filas, una por alterno) ...
SELECT t.PLNTCDAL AS alterno, t.PLNTCDGO, t.PLNTNMBR, t.PLNTSSTM, t.PLNTESTD
  FROM CNT.PLNT t WHERE t.PJRQCDGO = 1236 AND t.PLNTCDAL IN (7, 8, 9) ORDER BY t.PLNTCDAL;
--     ... periodos jun-2025 a nov-2026 (ESPERADO: 18 filas, 4524..4530 y 4511..4521) ...
SELECT p.PRDOCDGO, p.PRDOANNN AS anio, p.PRDOMSSS AS mes, p.PRDOESTD AS estado
  FROM CNT.PRDO p
 WHERE p.PJRQCDGO = 1236 AND ((p.PRDOANNN = 2025 AND p.PRDOMSSS >= 6) OR (p.PRDOANNN = 2026 AND p.PRDOMSSS <= 11))
 ORDER BY p.PRDOANNN, p.PRDOMSSS;
--     ... y asientos ya existentes con fecha >= 2025-06-01 y < 2026-11-01 en la 1236 (quedan como estan: la regla
--     solo toca asientos que se GENEREN desde ahora; el re-proceso los anula/regenera por su propio camino).
SELECT a.ASNTANOO AS anio, COUNT(*) AS asientos, SUM(CASE WHEN a.ASNTMDOR IS NULL THEN 1 ELSE 0 END) AS sin_modulo
  FROM CNT.ASNT a
 WHERE a.PJRQCDGO = 1236 AND a.ASNTFCHA >= DATE '2025-06-01' AND a.ASNTFCHA < DATE '2026-11-01'
 GROUP BY a.ASNTANOO ORDER BY a.ASNTANOO;

-- BLOQUE 2 — CATALOGO
-- 2.1 Rubro 249 (PRBRTPOO = 1, como 237/242/243).
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (249, 'CNT PARAMETROS ASIENTOS AUTOMATICOS (vigencia IN-A/EG-A/DI-A por empresa)', SYSDATE, 249, 1);

-- 2.2 Detalle de la empresa 1236: vigencia 2025-06-01 (D-006). PDTRALTR = codigo de la empresa.
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (1518, 249, 'VIGENCIA TIPOS AUTOMATICOS EMPRESA 1236 (yyyy-MM-dd en valor alfanumerico)', NULL, '2025-06-01', 1236, 1);

-- 2.3 (OPCIONAL, comentado) Detalle 0 = todas las empresas sin detalle propio. No hace falta: sin el, rige la
--     constante FECHA_VIGENCIA_DEFECTO (2026-11-01), que es lo mismo que poner aqui.
-- INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
-- VALUES (1519, 249, 'VIGENCIA TIPOS AUTOMATICOS TODAS LAS EMPRESAS', NULL, '2026-11-01', 0, 1);

-- (sin COMMIT: lo da quien lo corre despues de revisar el BLOQUE 3)

-- BLOQUE 3 — CONTROL DESPUES
-- 3.1 ESPERADO: 1 fila, rubro 249 / detalle alterno 1236 / valor '2025-06-01' / estado 1.
SELECT r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR, d.PDTRCDGO, d.PDTRALTR AS empresa, d.PDTRVLRV AS vigencia, d.PDTRESTD
  FROM SCP.PRBR r JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
 WHERE r.PRBRALTR = 249 ORDER BY d.PDTRALTR;

-- 3.2 El valor tiene que ser una fecha yyyy-MM-dd valida (si no, el codigo la ignora y usa 2026-11-01 sin fallar).
--     ESPERADO: 1 fila con fecha_valida = 2025-06-01.
SELECT d.PDTRALTR AS empresa, d.PDTRVLRV, TO_DATE(d.PDTRVLRV, 'YYYY-MM-DD') AS fecha_valida
  FROM SCP.PDTR d WHERE d.PRBRCDGO = 249 AND d.PDTRESTD = 1;

-- REVERSO (si hay que volver atras; sin COMMIT)
-- DELETE FROM SCP.PDTR WHERE PRBRCDGO = 249;
-- DELETE FROM SCP.PRBR WHERE PRBRCDGO = 249;
-- Para volver al comportamiento anterior SIN borrar, basta desactivar el detalle: UPDATE SCP.PDTR SET PDTRESTD = 0
-- WHERE PRBRCDGO = 249 AND PDTRALTR = 1236;  (el codigo solo lee detalles con PDTRESTD = 1).

-- =====================================================================================
-- 380 — CRD.HDTP.EVPRCDGO pasa a admitir NULL (cambio C35, DTPRSLOT = lo respaldado por los pagos)
-- 2026-11-18 · expediente cuadreSuper, C35-ABONO-CRUCE-CUOTAS-EN-MORA.md
--
-- Por que: C35 deja en CRD.HDTP la cuota tal como estaba cuando le quita a DTPRSLOT el valor sin pagos
--   que lo respalden (motivos SLOT_SIN_RESPALDO_*: reapertura, recalculo desde pagos, precancelacion y
--   abono en re-proceso, script C35-02). Esas filas van SIN evento a proposito: la anulacion del abono,
--   su asiento (re-bandeo) y la cartera al corte (ModoCorteCartera.abonosPosterioresSql) leen HDTP por
--   EVPRCDGO; atadas al evento del abono se restaurarian como cuotas o cambiarian la G48.
--   La FK FK_HDTP_EVPR se conserva (un NULL no la viola). Las filas existentes no cambian.
--
-- ORDEN: ANTES del WAR con C35 (saaBE rama cuadre/c35-saldo-otros). Sin este DDL, la primera
--   reapertura o precancelacion con residuo falla con ORA-01400 y revierte toda la operacion.
-- Script puro (sin WHENEVER/SET/DEFINE). Correr bloque por bloque.
-- =====================================================================================


-- BLOQUE 1 — Antes. Esperado: NULLABLE = 'N'. Si ya dice 'Y', el DDL ya esta aplicado: saltar el bloque 2.
SELECT column_name, nullable FROM all_tab_columns
 WHERE owner = 'CRD' AND table_name = 'HDTP' AND column_name = 'EVPRCDGO';


-- BLOQUE 2 — DDL.
ALTER TABLE CRD.HDTP MODIFY (EVPRCDGO NULL);


-- BLOQUE 3 — Despues. Esperado: NULLABLE = 'Y'; FK_HDTP_EVPR sigue ENABLED.
SELECT column_name, nullable FROM all_tab_columns
 WHERE owner = 'CRD' AND table_name = 'HDTP' AND column_name = 'EVPRCDGO';
SELECT constraint_name, constraint_type, status FROM all_constraints
 WHERE owner = 'CRD' AND table_name = 'HDTP' AND constraint_name = 'FK_HDTP_EVPR';

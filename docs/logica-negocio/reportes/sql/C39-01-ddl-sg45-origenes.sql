-- =====================================================================================
-- C39 (23-nov-2026): RPR.SG45 admite dos orígenes nuevos (ver SujetoReportadoG45):
--   CONFIRMADO  sujeto que la Superintendencia ya tiene en su base, con evidencia (acuse que lo validó);
--   PENDIENTE   sujeto que faltó en una G45 ya presentada y se reporta en la G45 del período SG45PRDO.
-- Va ANTES del WAR de C39. SQL PURO (cliente JDBC). DDL: confirma solo.
-- =====================================================================================

-- 1. Antes: la restricción vigente.
SELECT constraint_name, search_condition_vc FROM all_constraints
 WHERE owner = 'RPR' AND table_name = 'SG45' AND constraint_name = 'CK_SG45_ORGN';

-- 2. Cambio.
ALTER TABLE RPR.SG45 DROP CONSTRAINT CK_SG45_ORGN;
ALTER TABLE RPR.SG45 ADD CONSTRAINT CK_SG45_ORGN CHECK (SG45ORGN IN ('SEMILLA','HM45','EJECUCION','CONFIRMADO','PENDIENTE'));
COMMENT ON COLUMN RPR.SG45.SG45ORGN IS 'SEMILLA (supuesto sin confirmar, no cuenta como reportado desde C39) | HM45 (G45 presentada mayo 2025) | EJECUCION (G45 generada por el SAA) | CONFIRMADO (la Super lo tiene, con acuse) | PENDIENTE (se reporta en la G45 de SG45PRDO)';

-- 3. Después: la restricción nueva.
SELECT constraint_name, search_condition_vc FROM all_constraints
 WHERE owner = 'RPR' AND table_name = 'SG45' AND constraint_name = 'CK_SG45_ORGN';

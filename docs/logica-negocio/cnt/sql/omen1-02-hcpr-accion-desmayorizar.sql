-- =====================================================================================
-- omen1-02 — Amplía CK_HCPR_ACCION para la acción DESMAYORIZAR (6.5 del cuadre, DS-21)
-- Equipo omen-saa-1 · 2026-11-24 · REVISABLE: NO está ejecutado en ninguna base.
--
-- POST /prdo/desmayorizar deja una fila en CNT.HCPR con HCPRACCN = 'DESMAYORIZAR' (12 caracteres: cabe en VARCHAR2(12)).
-- El CHECK actual solo admite ('CIERRE', 'REAPERTURA'): sin este script el INSERT de la bitácora falla con ORA-02290 y
-- la desmayorización se revierte entera. Va ANTES del EAR.
-- No cambia datos ni la estructura de la tabla.
-- =====================================================================================

-- BLOQUE 0 — Control ANTES. Esperado: HCPRACCN IN ('CIERRE', 'REAPERTURA'), ENABLED, VALIDATED.
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION_VC, c.STATUS, c.VALIDATED
  FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CNT' AND c.TABLE_NAME = 'HCPR' AND c.CONSTRAINT_NAME = 'CK_HCPR_ACCION';

-- Filas existentes por acción. Esperado: solo CIERRE y REAPERTURA.
SELECT HCPRACCN, COUNT(*) AS filas FROM CNT.HCPR GROUP BY HCPRACCN ORDER BY 1;

-- BLOQUE A — (ESCRIBE) amplía el CHECK.
ALTER TABLE CNT.HCPR DROP CONSTRAINT CK_HCPR_ACCION;
ALTER TABLE CNT.HCPR ADD CONSTRAINT CK_HCPR_ACCION CHECK (HCPRACCN IN ('CIERRE', 'REAPERTURA', 'DESMAYORIZAR'));

-- BLOQUE 1 — Control DESPUÉS. Esperado: HCPRACCN IN ('CIERRE', 'REAPERTURA', 'DESMAYORIZAR'), ENABLED, VALIDATED.
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION_VC, c.STATUS, c.VALIDATED
  FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CNT' AND c.TABLE_NAME = 'HCPR' AND c.CONSTRAINT_NAME = 'CK_HCPR_ACCION';

-- REVERSO (solo si hay que deshacer y NO existe ninguna fila con HCPRACCN = 'DESMAYORIZAR'; confirmarlo con el SELECT de arriba):
-- ALTER TABLE CNT.HCPR DROP CONSTRAINT CK_HCPR_ACCION;
-- ALTER TABLE CNT.HCPR ADD CONSTRAINT CK_HCPR_ACCION CHECK (HCPRACCN IN ('CIERRE', 'REAPERTURA'));

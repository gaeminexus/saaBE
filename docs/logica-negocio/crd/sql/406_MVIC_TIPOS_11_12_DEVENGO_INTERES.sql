-- =====================================================================================
-- 406 — MVIC tipos 11 «DEVENGO_INTERES» y 12 «REVERSO_DEVENGO_INTERES» (lote 19, nivel 1.4): amplía CK_MVIC_TIPO a 1..12
-- Equipo omen-saa-1 · 2026-11-25 · REVISABLE: NO está ejecutado en ninguna base.
--
-- El libro por cuota del devengo de INTERÉS (componente 1) usa dos tipos nuevos; los 5 y 6 siguen siendo solo de mora. Sin este script el
-- INSERT de un tipo 11 o 12 falla con ORA-02290. No cambia datos ni estructura. Va ANTES del EAR que escriba esos tipos (el lote 19 todavía
-- no los escribe: solo la constante y este script).
-- =====================================================================================

-- BLOQUE 0 — Controles previos (solo lectura). Esperado: BETWEEN 1 AND 10 (o 1 AND 9 si el 392 no corrió), ENABLED, VALIDATED.
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION_VC, c.STATUS, c.VALIDATED
  FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'MVIC' AND c.CONSTRAINT_NAME = 'CK_MVIC_TIPO';
SELECT MVICTPMV, COUNT(*) AS filas FROM CRD.MVIC GROUP BY MVICTPMV ORDER BY 1;

-- BLOQUE A — (ESCRIBE).
ALTER TABLE CRD.MVIC DROP CONSTRAINT CK_MVIC_TIPO;
ALTER TABLE CRD.MVIC ADD CONSTRAINT CK_MVIC_TIPO CHECK (MVICTPMV BETWEEN 1 AND 12);
COMMENT ON COLUMN CRD.MVIC.MVICTPMV IS '1 PROVISION (cierre 7) · 2 REVERSO PROVISION POR COBRO · 3 REVERSO PROVISION POR COBRO TARDIO · 4 RE-PROVISION (se deshizo el cobro) · 5 DEVENGO DE MORA (cierre 4) · 6 REVERSO DEVENGO POR COBRO TARDIO · 7 CLASIFICACION DE CAPITAL EN EL CIERRE · 8 RECLASIFICACION POR COBRO TARDIO · 9 REVERSO PROVISION POR CONDONACION · 10 AJUSTE TOPE PROVISION DE MORA (D-186, lote 7) · 11 DEVENGO DE INTERES (componente 1, lote 19) · 12 REVERSO DEVENGO DE INTERES (componente 1, lote 19)';

-- BLOQUE 1 — Controles posteriores. Esperado: BETWEEN 1 AND 12, ENABLED, VALIDATED.
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION_VC, c.STATUS, c.VALIDATED
  FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'MVIC' AND c.CONSTRAINT_NAME = 'CK_MVIC_TIPO';

-- REVERSO (solo si NO hay filas con MVICTPMV = 11 o 12: confirmarlo con el 0b):
-- ALTER TABLE CRD.MVIC DROP CONSTRAINT CK_MVIC_TIPO;
-- ALTER TABLE CRD.MVIC ADD CONSTRAINT CK_MVIC_TIPO CHECK (MVICTPMV BETWEEN 1 AND 10);

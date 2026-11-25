-- =====================================================================================
-- 392 — MVIC tipo 10 «AJUSTE_TOPE_PROVISION» (lote 7, D-186): amplía CK_MVIC_TIPO a 1..10 y el comentario del catálogo
-- Equipo omen-saa-1 · 2026-11-25 · REVISABLE: NO está ejecutado en ninguna base.
--
-- AjusteTopeProvisionService graba, por cuota, un movimiento de tipo 10 (componente MORA, valor positivo, sin corrida) contra el asiento que
-- el cuadre contabilizó por el exceso de provisión de mora. Sin este script el INSERT falla con ORA-02290 (CK_MVIC_TIPO solo admite 1..9).
-- No cambia datos ni estructura. El catálogo de tipos de MVIC no tiene rubro: son constantes de TipoMovimientoInteresCuota (Java) y el
-- comentario de la columna. Va ANTES del EAR.
-- =====================================================================================

-- BLOQUE 0 — Controles previos (solo lectura).
-- 0a. El CHECK actual. Esperado: MVICTPMV BETWEEN 1 AND 9, ENABLED, VALIDATED.
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION_VC, c.STATUS, c.VALIDATED
  FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'MVIC' AND c.CONSTRAINT_NAME = 'CK_MVIC_TIPO';
-- 0b. Filas por tipo. Esperado: solo tipos 1..9 (ninguna con 10).
SELECT MVICTPMV, COUNT(*) AS filas FROM CRD.MVIC GROUP BY MVICTPMV ORDER BY 1;

-- BLOQUE A — (ESCRIBE).
ALTER TABLE CRD.MVIC DROP CONSTRAINT CK_MVIC_TIPO;
ALTER TABLE CRD.MVIC ADD CONSTRAINT CK_MVIC_TIPO CHECK (MVICTPMV BETWEEN 1 AND 10);
COMMENT ON COLUMN CRD.MVIC.MVICTPMV IS '1 PROVISION (cierre 7) · 2 REVERSO PROVISION POR COBRO · 3 REVERSO PROVISION POR COBRO TARDIO · 4 RE-PROVISION (se deshizo el cobro) · 5 DEVENGO DE MORA (cierre 4) · 6 REVERSO DEVENGO POR COBRO TARDIO · 7 CLASIFICACION DE CAPITAL EN EL CIERRE · 8 RECLASIFICACION POR COBRO TARDIO · 9 REVERSO PROVISION POR CONDONACION · 10 AJUSTE TOPE PROVISION DE MORA (D-186, lote 7)';

-- BLOQUE 1 — Controles posteriores. Esperado: BETWEEN 1 AND 10, ENABLED, VALIDATED; el comentario termina en «10 AJUSTE TOPE …».
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION_VC, c.STATUS, c.VALIDATED
  FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'MVIC' AND c.CONSTRAINT_NAME = 'CK_MVIC_TIPO';
SELECT m.COMMENTS FROM ALL_COL_COMMENTS m WHERE m.OWNER = 'CRD' AND m.TABLE_NAME = 'MVIC' AND m.COLUMN_NAME = 'MVICTPMV';

-- REVERSO (solo si hay que deshacer y NO hay filas con MVICTPMV = 10: confirmarlo con el 0b):
-- ALTER TABLE CRD.MVIC DROP CONSTRAINT CK_MVIC_TIPO;
-- ALTER TABLE CRD.MVIC ADD CONSTRAINT CK_MVIC_TIPO CHECK (MVICTPMV BETWEEN 1 AND 9);
-- COMMENT ON COLUMN CRD.MVIC.MVICTPMV IS '1 PROVISION (cierre 7) · 2 REVERSO PROVISION POR COBRO · 3 REVERSO PROVISION POR COBRO TARDIO · 4 RE-PROVISION (se deshizo el cobro) · 5 DEVENGO DE MORA (cierre 4) · 6 REVERSO DEVENGO POR COBRO TARDIO · 7 CLASIFICACION DE CAPITAL EN EL CIERRE · 8 RECLASIFICACION POR COBRO TARDIO · 9 REVERSO PROVISION POR CONDONACION';

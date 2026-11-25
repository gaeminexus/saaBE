-- =====================================================================================
-- 410 — ANCC subproceso 13 «AJUSTE_ULTIMO_RECURSO» (lote 22, nivel 2.3): amplía CK_ANCC_TPOO a 1..13
-- Equipo omen-saa-1 · 2026-11-25 · REVISABLE: NO está ejecutado en ninguna base.
--
-- El cierre de cartera, si queda una diferencia entre la contabilidad de la 1.4.02 o de la 1.4.99.05 y el libro por cuota, asienta el ajuste de último recurso
-- y lo registra en CRD.ANCC con el subproceso 13. Sin este script el INSERT falla con ORA-02290. Solo hace falta si se enciende la propiedad
-- saa.cartera.ajusteUltimoRecurso (apagada por defecto). Va DESPUÉS del 383 (que lo dejó en 1..12) y ANTES del EAR.
-- No cambia datos ni estructura.
-- =====================================================================================

-- BLOQUE 0 — Controles previos (solo lectura). Esperado: ANCCTPOO IN (1, ..., 12), ENABLED, VALIDATED.
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION_VC, c.STATUS, c.VALIDATED
  FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'ANCC' AND c.CONSTRAINT_NAME = 'CK_ANCC_TPOO';
SELECT ANCCTPOO, COUNT(*) AS filas FROM CRD.ANCC GROUP BY ANCCTPOO ORDER BY 1;

-- BLOQUE A — (ESCRIBE).
ALTER TABLE CRD.ANCC DROP CONSTRAINT CK_ANCC_TPOO;
ALTER TABLE CRD.ANCC ADD CONSTRAINT CK_ANCC_TPOO CHECK (ANCCTPOO IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13));

-- BLOQUE 1 — Controles posteriores. Esperado: ... 12, 13), ENABLED, VALIDATED.
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION_VC, c.STATUS, c.VALIDATED
  FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'ANCC' AND c.CONSTRAINT_NAME = 'CK_ANCC_TPOO';

-- REVERSO (solo si NO hay filas con ANCCTPOO = 13: confirmarlo con el 0):
-- ALTER TABLE CRD.ANCC DROP CONSTRAINT CK_ANCC_TPOO;
-- ALTER TABLE CRD.ANCC ADD CONSTRAINT CK_ANCC_TPOO CHECK (ANCCTPOO IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));

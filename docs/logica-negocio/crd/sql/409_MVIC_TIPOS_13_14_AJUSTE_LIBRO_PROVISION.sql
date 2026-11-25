-- =====================================================================================
-- 409 — MVIC tipos 13 «AJUSTE_LIBRO_PROVISION_BAJA» y 14 «AJUSTE_LIBRO_PROVISION_ALTA» (lote 21, A1): amplía CK_MVIC_TIPO a 1..14
-- Equipo omen-saa-1 · 2026-11-25 · REVISABLE: NO está ejecutado en ninguna base.
--
-- El ajuste del libro de provisión por cuota (POST /rest/prov/ajusteLibroProvision) graba un movimiento por cuota y componente: el 13 BAJA el saldo
-- provisionado (resta, como el 2, 3, 9 y 10) y el 14 lo SUBE (suma, como el 1 y el 4). Son dos tipos porque CK_MVIC_VALOR obliga MVICVLOR >= 0 y el signo
-- lo da el tipo. Sin este script el INSERT falla con ORA-02290. Va DESPUÉS del 392 y del 406 (o junto con ellos) y ANTES del EAR que escriba esos tipos.
-- No cambia datos ni estructura.
-- =====================================================================================

-- BLOQUE 0 — Controles previos (solo lectura). Esperado: BETWEEN 1 AND 12 (si corrieron 392 y 406), ENABLED, VALIDATED.
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION_VC, c.STATUS, c.VALIDATED
  FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'MVIC' AND c.CONSTRAINT_NAME = 'CK_MVIC_TIPO';
SELECT MVICTPMV, COUNT(*) AS filas FROM CRD.MVIC GROUP BY MVICTPMV ORDER BY 1;

-- BLOQUE A — (ESCRIBE).
ALTER TABLE CRD.MVIC DROP CONSTRAINT CK_MVIC_TIPO;
ALTER TABLE CRD.MVIC ADD CONSTRAINT CK_MVIC_TIPO CHECK (MVICTPMV BETWEEN 1 AND 14);
COMMENT ON COLUMN CRD.MVIC.MVICTPMV IS '1 PROVISION (cierre 7) · 2 REVERSO PROVISION POR COBRO · 3 REVERSO PROVISION POR COBRO TARDIO · 4 RE-PROVISION (se deshizo el cobro) · 5 DEVENGO DE MORA (cierre 4) · 6 REVERSO DEVENGO POR COBRO TARDIO · 7 CLASIFICACION DE CAPITAL EN EL CIERRE · 8 RECLASIFICACION POR COBRO TARDIO · 9 REVERSO PROVISION POR CONDONACION · 10 AJUSTE TOPE PROVISION DE MORA (D-186, lote 7) · 11 DEVENGO DE INTERES (componente 1, lote 19) · 12 REVERSO DEVENGO DE INTERES (componente 1, lote 19) · 13 AJUSTE LIBRO PROVISION BAJA (lote 21, A1) · 14 AJUSTE LIBRO PROVISION ALTA (lote 21, A1)';

-- BLOQUE 1 — Controles posteriores. Esperado: BETWEEN 1 AND 14, ENABLED, VALIDATED.
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION_VC, c.STATUS, c.VALIDATED
  FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'MVIC' AND c.CONSTRAINT_NAME = 'CK_MVIC_TIPO';
SELECT m.COMMENTS FROM ALL_COL_COMMENTS m WHERE m.OWNER = 'CRD' AND m.TABLE_NAME = 'MVIC' AND m.COLUMN_NAME = 'MVICTPMV';

-- REVERSO (solo si NO hay filas con MVICTPMV = 13 o 14: confirmarlo con el 0b; dejarlo en 1..12 si corrió el 406):
-- ALTER TABLE CRD.MVIC DROP CONSTRAINT CK_MVIC_TIPO;
-- ALTER TABLE CRD.MVIC ADD CONSTRAINT CK_MVIC_TIPO CHECK (MVICTPMV BETWEEN 1 AND 12);

-- =====================================================================================
-- D043-01 — Amplia CNT.ASNH.ASNHOBSR de 200 a 2000 (igual que CNT.ASNT.ASNTOBSR).
-- Cuadre Superintendencia, decision D-043 · 2026-10-29 · docs/logica-negocio/cnt/CIERRES-BLOQUEANTES.md §8
-- Sin esto la desmayorizacion de un periodo con cierre contable falla con ORA-12899 al copiar un asiento
-- con observacion de mas de 200 caracteres. Va a produccion ANTES del WAR (HistAsiento.java ya mapea 2000).
-- Ya aplicado en la base de trabajo (saa-oracle-23ai) el 29-oct-2026.
-- SQL PURO. Ampliar un VARCHAR2 no toca datos.
-- =====================================================================================

-- 1. Antes. ESPERADO en produccion: ASNHOBSR = 200, ASNTOBSR = 2000.
SELECT table_name, column_name, data_length
  FROM all_tab_columns
 WHERE owner = 'CNT' AND table_name IN ('ASNH', 'ASNT') AND column_name IN ('ASNHOBSR', 'ASNTOBSR');

-- 2. DDL.
ALTER TABLE CNT.ASNH MODIFY (ASNHOBSR VARCHAR2(2000));

-- 3. Despues. ESPERADO: las dos columnas en 2000.
SELECT table_name, column_name, data_length
  FROM all_tab_columns
 WHERE owner = 'CNT' AND table_name IN ('ASNH', 'ASNT') AND column_name IN ('ASNHOBSR', 'ASNTOBSR');

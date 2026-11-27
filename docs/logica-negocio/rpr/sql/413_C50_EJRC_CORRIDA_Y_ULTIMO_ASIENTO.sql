-- =====================================================================================================================================
-- 413 - C50: cada ejecución de las G40-G51 (RPR.EJRC) queda atada a la corrida de cartera vigente y al último asiento de cartera y aportes.
-- Equipo omen-saa-1 - 2026-11-27 - REVISABLE: NO está ejecutado en ninguna base. Va ANTES del EAR que lleve el C50.
--
-- Columnas nuevas (todas nullable; las ejecuciones viejas quedan NULL = «sin dato»: ni vigentes ni desactualizadas):
--   EJRCCRCT NUMBER      corrida de cartera (CRD.CRCT) vigente del mes al generar.
--   EJRCFCUA TIMESTAMP   fecha de ingreso (ASNTFCPR) del último asiento vigente del mes en 1.3, 1.4 y 2.1 al generar.
--   EJRCFCHG TIMESTAMP   instante de generación (EJRCFCGN solo trae la fecha).
-- Sin este script el guardado de RPR.EJRC falla con ORA-00904 (la entidad EjecucionReporte ya mapea las tres).
-- Solo agrega columnas: no cambia datos. El DDL de Oracle confirma por sí mismo.
-- =====================================================================================================================================

-- BLOQUE 0 - Controles previos (solo lectura). Esperado: 0 filas de columnas; N ejecuciones existentes.
SELECT COLUMN_NAME, DATA_TYPE, NULLABLE FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'RPR' AND TABLE_NAME = 'EJRC' AND COLUMN_NAME IN ('EJRCCRCT', 'EJRCFCUA', 'EJRCFCHG');
SELECT COUNT(*) AS ejecuciones, MIN(EJRCANOO * 100 + EJRCMESS) AS desde, MAX(EJRCANOO * 100 + EJRCMESS) AS hasta FROM RPR.EJRC;

-- BLOQUE A - (ESCRIBE).
ALTER TABLE RPR.EJRC ADD (EJRCCRCT NUMBER, EJRCFCUA TIMESTAMP, EJRCFCHG TIMESTAMP);
COMMENT ON COLUMN RPR.EJRC.EJRCCRCT IS 'C50: corrida de cartera (CRD.CRCT) vigente del mes al generar las G; NULL = generada antes del C50 (sin dato)';
COMMENT ON COLUMN RPR.EJRC.EJRCFCUA IS 'C50: fecha de ingreso (ASNTFCPR) del ultimo asiento vigente del mes en 1.3, 1.4 y 2.1 al generar';
COMMENT ON COLUMN RPR.EJRC.EJRCFCHG IS 'C50: instante de generacion de la ejecucion';

-- BLOQUE 1 - Controles posteriores. Esperado: 3 columnas TIMESTAMP/NUMBER nullable; todas las ejecuciones existentes con las tres en NULL.
SELECT COLUMN_NAME, DATA_TYPE, NULLABLE FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'RPR' AND TABLE_NAME = 'EJRC' AND COLUMN_NAME IN ('EJRCCRCT', 'EJRCFCUA', 'EJRCFCHG') ORDER BY COLUMN_NAME;
SELECT COUNT(*) AS con_corrida_grabada FROM RPR.EJRC WHERE EJRCCRCT IS NOT NULL;

-- REVERSO (solo si ninguna ejecución nueva dejó datos que se quieran conservar; confirmarlo con el último SELECT):
-- ALTER TABLE RPR.EJRC DROP (EJRCCRCT, EJRCFCUA, EJRCFCHG);

-- =====================================================================================
-- 411 — CRD.CRAR.CRARALCD «alerta de cuadre» (C46): columna nueva, nullable, VARCHAR2(2000)
-- Equipo omen-saa-1 · 2026-11-26 · REVISABLE: NO está ejecutado en ninguna base.
--
-- La carga del retorno de Petro compara lo cargado contra los TOTAL que imprime el propio reporte (por sección de producto y el TOTAL ==> final). Si hay
-- diferencia, la carga NO falla (marzo 2026 es un descuadre del propio Petro): queda en esta columna, persistente y visible, y la fase 3 la repite al
-- empezar su resumen. NULL = cuadra (o la carga es anterior a esta columna, o es de ARCH). Sin este script el INSERT/UPDATE de CRD.CRAR falla con ORA-00904
-- (la entidad CargaArchivo ya mapea CRARALCD). Va ANTES del EAR que lleve el código del C46.
-- Solo agrega una columna: no cambia datos, no tiene COMMIT automático (el DDL de Oracle confirma por sí mismo).
-- =====================================================================================

-- BLOQUE 0 — Controles previos (solo lectura). Esperado: 0 filas (la columna no existe todavía).
SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'CRD' AND TABLE_NAME = 'CRAR' AND COLUMN_NAME = 'CRARALCD';
SELECT COUNT(*) AS cargas FROM CRD.CRAR;

-- BLOQUE A — (ESCRIBE).
ALTER TABLE CRD.CRAR ADD (CRARALCD VARCHAR2(2000));
COMMENT ON COLUMN CRD.CRAR.CRARALCD IS 'C46: alerta de cuadre de la carga contra los TOTAL del reporte de Petro (por seccion y TOTAL ==>); NULL si cuadra. No bloquea la carga ni la fase 3.';

-- BLOQUE 1 — Controles posteriores. Esperado: VARCHAR2 / 2000 / Y; y todas las cargas existentes con CRARALCD nulo.
SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'CRD' AND TABLE_NAME = 'CRAR' AND COLUMN_NAME = 'CRARALCD';
SELECT COUNT(*) AS cargas_con_alerta FROM CRD.CRAR WHERE CRARALCD IS NOT NULL;

-- REVERSO (solo si ninguna carga nueva guardó una alerta que se quiera conservar; confirmarlo con el último SELECT):
-- ALTER TABLE CRD.CRAR DROP COLUMN CRARALCD;

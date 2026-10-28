-- C5 (cuadre Superintendencia): mover el tipo de asiento APERTURA de la empresa 1236 del codigo alterno 8 al 10.
-- Motivo: EG-A (EGRESO AUTOMATICO, vigente desde 2026-11-01) usa el alterno 8 con PLNTSSTM = 1; en una empresa con
-- los dos, TipoAsientoDaoService.selectByAlternoSistema(8) devuelve dos filas y tipoAutomatico() falla.
-- PROPUESTA, NO EJECUTADA. Sin COMMIT. SQL puro para el cliente JDBC de VS Code. Al aplicarlo, cambiar
-- TipoAsientos.APERTURA a 10 en el mismo despliegue.

-- 1 Control previo: quien tiene el alterno 8 y el 10 en 1236 (se espera APERTURA en el 8 y nadie en el 10)
SELECT PLNTCDGO, PLNTNMBR, PLNTCDAL, PLNTESTD, PLNTSSTM FROM CNT.PLNT WHERE PJRQCDGO = 1236 AND PLNTCDAL IN (8, 10);

-- 2 Cambio (solo si el control previo muestra que el 10 esta libre)
UPDATE CNT.PLNT SET PLNTCDAL = 10
WHERE PJRQCDGO = 1236 AND PLNTNMBR = 'APERTURA' AND PLNTCDAL = 8
  AND NOT EXISTS (SELECT 1 FROM CNT.PLNT WHERE PJRQCDGO = 1236 AND PLNTCDAL = 10);

-- 3 Control posterior: APERTURA en el 10, ninguna otra fila de 1236 con el 10
SELECT PLNTCDGO, PLNTNMBR, PLNTCDAL, PLNTESTD, PLNTSSTM FROM CNT.PLNT WHERE PJRQCDGO = 1236 AND PLNTCDAL IN (8, 10);
-- Los asientos ya registrados no cambian: CNT.ASNT referencia PLNTCDGO (1257), no el alterno.
SELECT COUNT(*) asientos_apertura FROM CNT.ASNT WHERE PJRQCDGO = 1236 AND PLNTCDGO = (SELECT PLNTCDGO FROM CNT.PLNT WHERE PJRQCDGO = 1236 AND PLNTNMBR = 'APERTURA');

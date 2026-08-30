-- =====================================================================
-- Anulacion de documentos de compra: fecha, motivo y usuario
-- Esquema: PGS  |  Tablas: FCTC, LQCC, NTCC, NTDC
-- Fecha:  2026-08-28
-- Autor:  arbitro
--
-- PARA QUE
--   docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md §10.3: el generador
--   del ATS no puede armar <anulados> del lado compra porque ninguna de las
--   cuatro tablas de <compras> tiene forma de saber CUANDO ni POR QUE se
--   anulo un documento -- solo existe ESTADOEMISION (Long), sin auditoria.
--   El mecanismo de estado YA EXISTE (las 4 tablas ya tienen ESTADOEMISION,
--   verificado en el codigo, mismo campo que ya usa CBR.FCTR con valor 3 =
--   ANULADA para la factura de venta electronica) -- lo que falta es solo
--   la auditoria de la anulacion, no el estado en si.
--
-- QUE HACE
--   Agrega a las cuatro tablas, mismo patron que ya usa CBR.FCTR
--   (MOTIVOANULACION/FECHAANULACION/USUARIOANULACION) pero con la
--   convencion de 8 caracteres que ya sigue el resto de esta ola (ver
--   TTLRPREL, FCTCFCRG, ANTCIDPG en los DDL anteriores):
--     <TABLA>MTAN  VARCHAR2(1000)  -- motivo de la anulacion
--     <TABLA>FCAN  TIMESTAMP       -- fecha/hora de la anulacion
--     <TABLA>USAN  VARCHAR2(200)   -- usuario que anulo
--
-- POR QUE NO ROMPE NADA
--   Columnas nuevas, todas nullable, sin CHECK ni FK. Nula = nunca se
--   anulo (o se anulo antes de este cambio, sin auditoria -- dato
--   historico perdido, no hay de donde reconstruirlo).
--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 0: control previo -- las 12 columnas no deben existir todavia
-- ---------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'PGS' AND TABLE_NAME IN ('FCTC','LQCC','NTCC','NTDC')
   AND COLUMN_NAME IN ('FCTCMTAN','FCTCFCAN','FCTCUSAN',
                        'LQCCMTAN','LQCCFCAN','LQCCUSAN',
                        'NTCCMTAN','NTCCFCAN','NTCCUSAN',
                        'NTDCMTAN','NTDCFCAN','NTDCUSAN');
-- Debe devolver 0 filas.

-- Recordatorio: confirmar que ESTADOEMISION=3 significa ANULADA en las
-- cuatro tablas igual que en CBR.FCTR, antes de que el backend lo use.
-- Si alguna tabla usa otro valor para "anulada", el agente lo reporta
-- -- no lo asume.
SELECT 'FCTC' AS TABLA, ESTADOEMISION, COUNT(*) FROM PGS.FCTC GROUP BY ESTADOEMISION
UNION ALL
SELECT 'LQCC', ESTADOEMISION, COUNT(*) FROM PGS.LQCC GROUP BY ESTADOEMISION
UNION ALL
SELECT 'NTCC', ESTADOEMISION, COUNT(*) FROM PGS.NTCC GROUP BY ESTADOEMISION
UNION ALL
SELECT 'NTDC', ESTADOEMISION, COUNT(*) FROM PGS.NTDC GROUP BY ESTADOEMISION
ORDER BY 1, 2;

-- ---------------------------------------------------------------------
-- BLOQUE 1: columnas de auditoria de anulacion
-- ---------------------------------------------------------------------
ALTER TABLE PGS.FCTC ADD (FCTCMTAN VARCHAR2(1000), FCTCFCAN TIMESTAMP, FCTCUSAN VARCHAR2(200));
ALTER TABLE PGS.LQCC ADD (LQCCMTAN VARCHAR2(1000), LQCCFCAN TIMESTAMP, LQCCUSAN VARCHAR2(200));
ALTER TABLE PGS.NTCC ADD (NTCCMTAN VARCHAR2(1000), NTCCFCAN TIMESTAMP, NTCCUSAN VARCHAR2(200));
ALTER TABLE PGS.NTDC ADD (NTDCMTAN VARCHAR2(1000), NTDCFCAN TIMESTAMP, NTDCUSAN VARCHAR2(200));

COMMENT ON COLUMN PGS.FCTC.FCTCMTAN IS 'Motivo de anulacion de la factura de compra. Nulo = nunca anulada (o anulada antes de este cambio, sin auditoria)';
COMMENT ON COLUMN PGS.FCTC.FCTCFCAN IS 'Fecha/hora de anulacion de la factura de compra';
COMMENT ON COLUMN PGS.FCTC.FCTCUSAN IS 'Usuario que anulo la factura de compra';
COMMENT ON COLUMN PGS.LQCC.LQCCMTAN IS 'Motivo de anulacion de la liquidacion de compra. Nulo = nunca anulada';
COMMENT ON COLUMN PGS.LQCC.LQCCFCAN IS 'Fecha/hora de anulacion de la liquidacion de compra';
COMMENT ON COLUMN PGS.LQCC.LQCCUSAN IS 'Usuario que anulo la liquidacion de compra';
COMMENT ON COLUMN PGS.NTCC.NTCCMTAN IS 'Motivo de anulacion de la nota de credito de compra. Nulo = nunca anulada';
COMMENT ON COLUMN PGS.NTCC.NTCCFCAN IS 'Fecha/hora de anulacion de la nota de credito de compra';
COMMENT ON COLUMN PGS.NTCC.NTCCUSAN IS 'Usuario que anulo la nota de credito de compra';
COMMENT ON COLUMN PGS.NTDC.NTDCMTAN IS 'Motivo de anulacion de la nota de debito de compra. Nulo = nunca anulada';
COMMENT ON COLUMN PGS.NTDC.NTDCFCAN IS 'Fecha/hora de anulacion de la nota de debito de compra';
COMMENT ON COLUMN PGS.NTDC.NTDCUSAN IS 'Usuario que anulo la nota de debito de compra';

-- ---------------------------------------------------------------------
-- BLOQUE 2: control final
-- ---------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'PGS' AND TABLE_NAME IN ('FCTC','LQCC','NTCC','NTDC')
   AND COLUMN_NAME IN ('FCTCMTAN','FCTCFCAN','FCTCUSAN',
                        'LQCCMTAN','LQCCFCAN','LQCCUSAN',
                        'NTCCMTAN','NTCCFCAN','NTCCUSAN',
                        'NTDCMTAN','NTDCFCAN','NTDCUSAN')
 ORDER BY TABLE_NAME, COLUMN_NAME;
-- Deben ser 12 filas, todas NULLABLE='Y'.

COMMIT;

-- ---------------------------------------------------------------------
-- BLOQUE 3: reverso (comentado a proposito)
-- ---------------------------------------------------------------------
-- ALTER TABLE PGS.FCTC DROP COLUMN FCTCMTAN; ALTER TABLE PGS.FCTC DROP COLUMN FCTCFCAN; ALTER TABLE PGS.FCTC DROP COLUMN FCTCUSAN;
-- ALTER TABLE PGS.LQCC DROP COLUMN LQCCMTAN; ALTER TABLE PGS.LQCC DROP COLUMN LQCCFCAN; ALTER TABLE PGS.LQCC DROP COLUMN LQCCUSAN;
-- ALTER TABLE PGS.NTCC DROP COLUMN NTCCMTAN; ALTER TABLE PGS.NTCC DROP COLUMN NTCCFCAN; ALTER TABLE PGS.NTCC DROP COLUMN NTCCUSAN;
-- ALTER TABLE PGS.NTDC DROP COLUMN NTDCMTAN; ALTER TABLE PGS.NTDC DROP COLUMN NTDCFCAN; ALTER TABLE PGS.NTDC DROP COLUMN NTDCUSAN;
-- COMMIT;

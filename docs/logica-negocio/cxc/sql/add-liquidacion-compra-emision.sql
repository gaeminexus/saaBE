-- =====================================================================
-- LIQUIDACIONES DE COMPRA EMITIDAS (CXC): datos y enlace con CXP
-- Fecha: 2026-08-26 — orquestador
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- BLOQUE 1: numeracion del punto de emision para tipo 03 (liquidacion de compra)
--   Sin esta fila, LiquidacionCompraServiceImpl.obtenerSecuencial falla.
--   En BD local existen 01, 04, 05, 07 para PTOEMISION=1. Verificar el
--   punto de emision correcto en produccion antes de insertar.
SELECT ID, PTOEMISION, TIPODOC, NUMACTUAL FROM CBR.NXPE ORDER BY PTOEMISION, TIPODOC;
INSERT INTO CBR.NXPE (PTOEMISION, TIPODOC, NUMACTUAL) VALUES (1, '03', 0);
SELECT ID, PTOEMISION, TIPODOC, NUMACTUAL FROM CBR.NXPE WHERE TIPODOC = '03';
COMMIT;

-- BLOQUE 2: enlace liquidacion emitida (CBR.LQCS) -> documento CXP (PGS.LQCC)
--   La cuenta por pagar y el asiento viven en CXP; CXC guarda el puntero.
ALTER TABLE CBR.LQCS ADD (LQCSLQCC NUMBER);
-- Oracle no considera los privilegios de ROL al crear constraints: hace
-- falta el GRANT REFERENCES directo. PGS.LQCC no lo tiene concedido a
-- PUBLIC (verificado 2026-08-27), asi que sin esta linea el ALTER falla.
-- Ejecutar conectado como PGS (o como DBA).
GRANT REFERENCES ON PGS.LQCC TO CBR;
ALTER TABLE CBR.LQCS ADD CONSTRAINT FK_LQCS_LQCC FOREIGN KEY (LQCSLQCC) REFERENCES PGS.LQCC(ID);
CREATE INDEX CBR.IDX_LQCS_LQCC ON CBR.LQCS(LQCSLQCC);
COMMENT ON COLUMN CBR.LQCS.LQCSLQCC IS 'Documento CXP (PGS.LQCC) creado al autorizar la liquidacion; ahi viven la cuenta por pagar y el asiento';
SELECT COUNT(*) FROM CBR.LQCS;
COMMIT;

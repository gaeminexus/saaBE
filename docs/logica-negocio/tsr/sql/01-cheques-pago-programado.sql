-- =====================================================================
-- CHEQUES: habilitar pago con cheque en el circuito moderno PGS.PGTR
-- Módulo: TSR / CXP
-- Fecha:  2026-08-26
-- Autor:  orquestador (verificado contra BD local saa-oracle-23ai)
--
-- Las tablas TSR.CHQR (Chequera) y TSR.DTCH (Cheque) YA EXISTEN con sus
-- secuencias TSR.SQ_CHQRCDGO / TSR.SQ_DTCHCDGO y FKs (FK_DTCH_CHQR,
-- FK_DTCH_ASNT, FK_DTCH_PRSN, FK_CHQR_CNBC). No se tocan.
-- TSR.PGSS y TSR.MVCB ya tienen la FK DTCHCDGO. No se tocan.
--
-- Ejecutar por bloques revisando los SELECT de control.
-- SQL puro (sin directivas SQL*Plus).
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 1: TSR.CNBC — la cuenta bancaria maneja chequera (0/1)
-- ---------------------------------------------------------------------
ALTER TABLE TSR.CNBC ADD (CNBCCHQR NUMBER DEFAULT 0);
COMMENT ON COLUMN TSR.CNBC.CNBCCHQR IS 'Maneja chequera: 0=No, 1=Si. Habilita la forma de pago CHEQUE en los pagos que salen de esta cuenta (no excluye transferencia ni debito automatico)';
UPDATE TSR.CNBC SET CNBCCHQR = 0 WHERE CNBCCHQR IS NULL;
-- Control: todas las cuentas con 0
SELECT CNBCCDGO, CNBCNMRO, CNBCCHQR FROM TSR.CNBC ORDER BY CNBCCDGO;
COMMIT;

-- ---------------------------------------------------------------------
-- BLOQUE 2: PGS.PGTR — forma de pago explícita + cheque asignado
--   PGTRFPAG: 1=Efectivo, 2=Transferencia, 3=Cheque, 4=Debito automatico
--             (mismo catalogo que PGS.APLP.APLPFPAG y el enum del frontend)
--   PGTRDTCH: FK al cheque girado cuando PGTRFPAG = 3
-- ---------------------------------------------------------------------
ALTER TABLE PGS.PGTR ADD (PGTRFPAG NUMBER DEFAULT 2, PGTRDTCH NUMBER);
COMMENT ON COLUMN PGS.PGTR.PGTRFPAG IS 'Forma de pago: 1=Efectivo, 2=Transferencia, 3=Cheque, 4=Debito automatico';
COMMENT ON COLUMN PGS.PGTR.PGTRDTCH IS 'Cheque girado para este pago (TSR.DTCH). Solo cuando PGTRFPAG=3';
ALTER TABLE PGS.PGTR ADD CONSTRAINT FK_PGTR_DTCH FOREIGN KEY (PGTRDTCH) REFERENCES TSR.DTCH(DTCHCDGO);
-- Indice UNICO (no simplemente indexado): ademas de servir a la FK, impide
-- que dos pagos tomen el mismo cheque si fallara el lock del backend.
-- Oracle admite multiples NULL en indice unico, asi que los pagos sin cheque
-- (transferencia, debito automatico) no se ven afectados.
-- OJO: prefijar el schema. Sin prefijo el indice queda en el schema de la
-- SESION, ocupa la columna igual y luego da ORA-01408.
CREATE UNIQUE INDEX PGS.UQ_PGTR_DTCH ON PGS.PGTR(PGTRDTCH);
-- Backfill: los pagos existentes fueron transferencia (2) o debito automatico (4)
UPDATE PGS.PGTR SET PGTRFPAG = 4 WHERE PGTRDBAT = 1;
UPDATE PGS.PGTR SET PGTRFPAG = 2 WHERE PGTRFPAG IS NULL;
-- Control: debe cuadrar PGTRDBAT=1 <-> PGTRFPAG=4, resto 2, ninguno NULL
SELECT PGTRDBAT, PGTRFPAG, COUNT(*) FROM PGS.PGTR GROUP BY PGTRDBAT, PGTRFPAG ORDER BY 1, 2;
COMMIT;

-- ---------------------------------------------------------------------
-- BLOQUE 3: rubro 38 (MOTIVO DE ANULACION DE CHEQUES) — completar detalles
--   En BD solo existen 1=ERROR DE TIPEO y 2=ERROR DE USUARIO.
--   El Java (MotivoAnulacionCheque) ya declara CHEQUERA_ANULADA=3.
--   Se agrega 4=PAGO REVERSADO para la anulacion automatica por reverso.
--   PDTRCDGO libres desde 1116 (MAX en BD local = 1115).
-- ---------------------------------------------------------------------
-- Control previo: confirmar que 1116 y 1117 estan libres y que el rubro 38 existe
SELECT MAX(PDTRCDGO) FROM SCP.PDTR;
SELECT PDTRCDGO, PDTRDSCR, PDTRALTR FROM SCP.PDTR WHERE PRBRCDGO = 38 ORDER BY PDTRALTR;
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (1116, 38, 'CHEQUERA ANULADA', 3, 'CHEQUERA_ANULADA', 3, 1);
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (1117, 38, 'PAGO REVERSADO', 4, 'PAGO_REVERSADO', 4, 1);
-- Control: 4 detalles con alternos 1..4
SELECT PDTRCDGO, PDTRDSCR, PDTRALTR FROM SCP.PDTR WHERE PRBRCDGO = 38 ORDER BY PDTRALTR;
COMMIT;

-- ---------------------------------------------------------------------
-- BLOQUE 4 (control final): estado de las tablas de cheques
-- ---------------------------------------------------------------------
SELECT COUNT(*) AS CHEQUERAS FROM TSR.CHQR;
SELECT COUNT(*) AS CHEQUES FROM TSR.DTCH;

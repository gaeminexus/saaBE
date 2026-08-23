-- ============================================================
-- Migración: Débito automático en pagos a proveedores (PGTR)
-- Módulo:    CXP - Cuentas por Pagar
-- Schema:    PGS
-- Fecha:     2026-08-12
--
-- Orden de ejecución: 6
-- Anterior: 05-insert-rubros-proceso-carga.sql
--
-- Propósito: Marcar los pagos que el banco debita automáticamente por
--            convenio con el proveedor. Esos pagos no se aprueban ni se
--            incluyen en el archivo enviado al banco: al registrarlos ya
--            nacen CONFIRMADOS (PGTRESTD=3) y en ese mismo momento abonan
--            la factura, generan el asiento contable y el movimiento
--            bancario de egreso.
-- ============================================================

-- 1. Marca de débito automático
--    0 = No (transferencia normal, recorre lote y archivo del banco)
--    1 = Sí (el banco ya debitó la cuenta; se contabiliza al registrarlo)
ALTER TABLE PGS.PGTR
    ADD PGTRDBAT NUMBER(1) DEFAULT 0;

COMMENT ON COLUMN PGS.PGTR.PGTRDBAT
    IS '0=Transferencia normal (lote + archivo al banco), 1=Débito automático ya ejecutado por el banco.';


-- 2. Los pagos existentes son todos transferencias normales
UPDATE PGS.PGTR
   SET PGTRDBAT = 0
 WHERE PGTRDBAT IS NULL;

COMMIT;


-- ============================================================
-- Verificación
-- ============================================================

-- Columna creada
-- SELECT COLUMN_NAME, DATA_TYPE, DATA_DEFAULT, NULLABLE
--   FROM ALL_TAB_COLUMNS
--  WHERE OWNER = 'PGS' AND TABLE_NAME = 'PGTR' AND COLUMN_NAME = 'PGTRDBAT';

-- Ningún pago quedó sin marca
-- SELECT PGTRDBAT, COUNT(*) FROM PGS.PGTR GROUP BY PGTRDBAT;

-- Pagos por débito automático y su aplicación / asiento
-- SELECT p.PGTRCDGO, p.PGTRVLOR, p.PGTRESTD, p.PGTRRFBN, p.PGTRFRSP,
--        a.APLPCDGO, a.APLPFPAG, a.APLPASNT
--   FROM PGS.PGTR p
--   LEFT JOIN PGS.APLP a ON a.APLPCDGO = p.PGTRAPLP
--  WHERE p.PGTRDBAT = 1
--  ORDER BY p.PGTRCDGO DESC;

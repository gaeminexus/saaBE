-- ============================================================
-- Migración: Débito automático en egresos de tesorería (EGRS)
-- Módulo:    TSR - Tesorería
-- Schema:    TSR
-- Fecha:     2026-08-18
--
-- Anterior: sql-ingresos-egresos-tesoreria.sql (creación de TSR.EGRS)
--
-- Propósito: Registrar en el propio egreso si se pagó por débito
--            automático, igual que PGS.PGTR.PGTRDBAT en los pagos a
--            facturas. El proceso ya existía (el egreso con débito
--            automático nace pagado: su PGTR se crea CONFIRMADO y en ese
--            mismo momento genera el asiento y el movimiento bancario),
--            pero la modalidad solo quedaba en el pago: para listar o
--            filtrar los egresos por débito automático había que cruzar
--            con PGS.PGTR.
--
--            La marca guarda la modalidad con la que se REGISTRÓ el
--            egreso: si después se reversa el pago (el egreso vuelve a
--            Pendiente y el pago queda Anulado) la marca no cambia.
--
-- Nota:      sql-ingresos-egresos-tesoreria.sql ya incluye EGRSDBAT en el
--            CREATE TABLE. En una base creada desde cero con ese script el
--            paso 1 falla con ORA-01430 (columna ya existente): saltarlo y
--            ejecutar solo los controles.
-- ============================================================

-- ============================================================
-- Control ANTES (ejecutar y guardar el resultado)
-- ============================================================

-- La columna no debe existir todavía
-- SELECT COLUMN_NAME FROM ALL_TAB_COLUMNS
--  WHERE OWNER = 'TSR' AND TABLE_NAME = 'EGRS' AND COLUMN_NAME = 'EGRSDBAT';

-- Cuántos egresos hay y cuántos se pagaron por débito automático según su pago
-- SELECT NVL(p.PGTRDBAT, 0) AS DEBITO_AUTOMATICO_EN_EL_PAGO, COUNT(*) AS EGRESOS
--   FROM TSR.EGRS e
--   LEFT JOIN PGS.PGTR p ON p.PGTREGRS = e.EGRSCDGO
--  GROUP BY NVL(p.PGTRDBAT, 0);


-- ============================================================
-- 1. Marca de débito automático
--    0 = No (transferencia: beneficiario + cuenta destino, lote y archivo al banco)
--    1 = Sí (el banco ya debitó la cuenta; se contabiliza al registrarlo)
-- ============================================================

ALTER TABLE TSR.EGRS
    ADD EGRSDBAT NUMBER(1) DEFAULT 0;

COMMENT ON COLUMN TSR.EGRS.EGRSDBAT
    IS '0=Transferencia (pago por lote + archivo al banco), 1=Débito automático ya ejecutado por el banco. Espejo de PGS.PGTR.PGTRDBAT del pago del egreso.';


-- ============================================================
-- 2. Egresos existentes: se toma la modalidad de su pago
--    (sin pago o sin marca en el pago => transferencia)
-- ============================================================

UPDATE TSR.EGRS e
   SET e.EGRSDBAT = NVL((SELECT MAX(p.PGTRDBAT)
                           FROM PGS.PGTR p
                          WHERE p.PGTREGRS = e.EGRSCDGO), 0)
 WHERE e.EGRSDBAT IS NULL;

COMMIT;


-- ============================================================
-- Control DESPUÉS
-- ============================================================

-- Columna creada
-- SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, DATA_DEFAULT, NULLABLE
--   FROM ALL_TAB_COLUMNS
--  WHERE OWNER = 'TSR' AND TABLE_NAME = 'EGRS' AND COLUMN_NAME = 'EGRSDBAT';

-- Ningún egreso quedó sin marca (no debe haber filas con NULL)
-- SELECT EGRSDBAT, COUNT(*) FROM TSR.EGRS GROUP BY EGRSDBAT;

-- La marca del egreso coincide con la de su pago (no debe devolver filas)
-- SELECT e.EGRSCDGO, e.EGRSDBAT, p.PGTRCDGO, p.PGTRDBAT
--   FROM TSR.EGRS e
--   JOIN PGS.PGTR p ON p.PGTREGRS = e.EGRSCDGO
--  WHERE NVL(e.EGRSDBAT, -1) <> NVL(p.PGTRDBAT, -1);

-- Egresos por débito automático con su pago, asiento y estado
-- SELECT e.EGRSCDGO, e.EGRSDSCR, e.EGRSVLOR, e.EGRSFCHA, e.EGRSESTD, e.EGRSASNT,
--        p.PGTRCDGO, p.PGTRESTD, p.PGTRRFBN
--   FROM TSR.EGRS e
--   LEFT JOIN PGS.PGTR p ON p.PGTREGRS = e.EGRSCDGO
--  WHERE e.EGRSDBAT = 1
--  ORDER BY e.EGRSCDGO DESC;

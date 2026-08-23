-- ============================================================
-- Migración: Anticipos a proveedor por el circuito de pagos
-- Módulo:    CXP (+ ALTER en PGS.PGTR)
-- Schema:    PGS
-- Fecha:     2026-08-14
--
-- Propósito: Los anticipos a proveedor (PGS.ANTP) dejan de
--            contabilizarse al registrarse y pasan por el mismo
--            circuito de pagos que los egresos de tesorería y las
--            facturas de compra: registrar el anticipo crea un
--            PGS.PGTR con FK al anticipo, que aparece en el listado
--            de pagos a realizar y sigue lote → archivo →
--            confirmación. Recién cuando el banco confirma el pago
--            se genera el asiento de ANTICIPO (DEBE cuenta de
--            anticipos del proveedor / HABER banco,
--            TipoAsientos.ANTICIPOS_PROVEEDOR=9 — no el asiento de
--            egreso), el MovimientoBanco, y se acredita el saldo de
--            anticipos del proveedor (TSR.PRCC).
--
--            El débito automático (PGTRDBAT=1) también aplica: el
--            pago nace confirmado y contabiliza al registrarse.
--
--            Estados de PGS.ANTP.ANTPESTD (sin cambio de valores):
--            1=Ingresado (pago pendiente), 2=Confirmado (con
--            asiento), 3=Anulado.
-- ============================================================

-- Requiere que ya exista el ALTER de egresos
-- (docs/scripts/sql-ingresos-egresos-tesoreria.sql, bloque 3:
-- PGTRFCTC nullable + PGTREGRS).


-- ============================================================
-- 1. ALTER PGS.PGTR — el pago puede apuntar a un anticipo
-- ============================================================

-- El pago paga UNA de tres cosas: una factura de compra, un egreso
-- de tesorería o un anticipo a proveedor (lo valida el backend).
ALTER TABLE PGS.PGTR ADD PGTRANTP NUMBER(19) NULL;

ALTER TABLE PGS.PGTR
    ADD CONSTRAINT FK_PGTR_ANTICIPO
        FOREIGN KEY (PGTRANTP)
        REFERENCES PGS.ANTP (ANTPCDGO);

COMMENT ON COLUMN PGS.PGTR.PGTRANTP
    IS 'FK a PGS.ANTP. Anticipo a proveedor que se paga. Excluyente con PGTRFCTC y PGTREGRS: el pago referencia una factura O un egreso O un anticipo, nunca más de uno (lo valida el backend).';

CREATE INDEX IX_PGTR_ANTICIPO ON PGS.PGTR (PGTRANTP);

COMMIT;


-- ============================================================
-- Verificación
-- ============================================================

-- Columna nueva
-- SELECT COLUMN_NAME, NULLABLE FROM ALL_TAB_COLUMNS
--  WHERE OWNER = 'PGS' AND TABLE_NAME = 'PGTR'
--    AND COLUMN_NAME IN ('PGTRFCTC','PGTREGRS','PGTRANTP');

-- Anticipos con su pago y estado
-- SELECT a.ANTPCDGO, a.ANTPVLOR, a.ANTPESTD, a.ANTPASNT,
--        p.PGTRCDGO, p.PGTRESTD, p.PGTRDBAT
--   FROM PGS.ANTP a
--   LEFT JOIN PGS.PGTR p ON p.PGTRANTP = a.ANTPCDGO
--  ORDER BY a.ANTPCDGO DESC;

-- Ningún pago debe apuntar a más de un origen
-- SELECT PGTRCDGO FROM PGS.PGTR
--  WHERE (CASE WHEN PGTRFCTC IS NOT NULL THEN 1 ELSE 0 END
--       + CASE WHEN PGTREGRS IS NOT NULL THEN 1 ELSE 0 END
--       + CASE WHEN PGTRANTP IS NOT NULL THEN 1 ELSE 0 END) > 1;

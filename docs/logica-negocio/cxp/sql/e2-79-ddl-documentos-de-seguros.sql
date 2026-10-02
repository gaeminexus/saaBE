-- =====================================================================================
-- e2-79 — DDL: marca de «documento de seguros» y enlace a la póliza de crédito en las facturas,
--          notas de débito y notas de crédito de compra, y en la bandeja de carga (PGS.DCXP).
-- Equipo omen-saa-2 · 2026-10-02 · Diseño: docs/logica-negocio/cxp/API-DOCUMENTOS-SEGUROS-CXP.md
--
-- ⛔⛔ VA ANTES DEL WAR. Las cuatro entidades (FacturaCompra, NotaDebitoCompra, NotaCreditoCompra, DocumentoCxp)
--    mapean las columnas nuevas. Hibernate mete toda @Column en el SELECT, así que un WAR sin este
--    script rompe CUALQUIER lectura de facturas, ND, NC de compra y de la bandeja de carga (ORA-00904):
--    consulta de documentos, gestión de documentos, pagos, ATS, cartera… No es solo la función nueva.
--
-- Columnas (las mismas en FCTC, NTDC, NTCC; en DCXP solo la marca 0/1 y el enlace):
--   xxxxESSG NUMBER(1) DEFAULT 0 NOT NULL  0 = no es de seguros · 1 = de seguros, BLOQUEADO hasta que
--                                           crédito lo libere · 2 = de seguros, LIBERADO
--   xxxxPOSG NUMBER                         CRD.POSG.POSGCDGO del documento de seguro de crédito.
--                                           Sin FK: otro esquema (exigiría GRANT REFERENCES, §27)
-- ADD con DEFAULT + NOT NULL: en Oracle 12c+ es una operación de diccionario, no reescribe la tabla.
-- Correr de corrido: cada ALTER se salta solo si la columna ya existe (idempotente).
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES (ESPERADO: CERO filas; si aparecen, el script las respeta y no las duplica)
SELECT 'BLOQUE 0 - columnas que ya existen' AS bloque, OWNER, TABLE_NAME, COLUMN_NAME
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'PGS'
   AND COLUMN_NAME IN ('FCTCESSG', 'FCTCPOSG', 'NTDCESSG', 'NTDCPOSG', 'NTCCESSG', 'NTCCPOSG', 'DCXPESSG', 'DCXPPOSG');

-- BLOQUE 1 — DDL
DECLARE
    PROCEDURE agrega(p_tabla VARCHAR2, p_columna VARCHAR2, p_definicion VARCHAR2) IS
        v_existe NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_existe FROM ALL_TAB_COLUMNS
         WHERE OWNER = 'PGS' AND TABLE_NAME = p_tabla AND COLUMN_NAME = p_columna;
        IF v_existe = 0 THEN
            EXECUTE IMMEDIATE 'ALTER TABLE PGS.' || p_tabla || ' ADD (' || p_columna || ' ' || p_definicion || ')';
        END IF;
    END;
BEGIN
    agrega('FCTC', 'FCTCESSG', 'NUMBER(1) DEFAULT 0 NOT NULL');
    agrega('FCTC', 'FCTCPOSG', 'NUMBER');
    agrega('NTDC', 'NTDCESSG', 'NUMBER(1) DEFAULT 0 NOT NULL');
    agrega('NTDC', 'NTDCPOSG', 'NUMBER');
    agrega('NTCC', 'NTCCESSG', 'NUMBER(1) DEFAULT 0 NOT NULL');
    agrega('NTCC', 'NTCCPOSG', 'NUMBER');
    agrega('DCXP', 'DCXPESSG', 'NUMBER(1) DEFAULT 0 NOT NULL');
    agrega('DCXP', 'DCXPPOSG', 'NUMBER');

    EXECUTE IMMEDIATE q'[COMMENT ON COLUMN PGS.FCTC.FCTCESSG IS '0 no es de seguros, 1 de seguros BLOQUEADO para pago hasta que credito lo libere, 2 de seguros LIBERADO']';
    EXECUTE IMMEDIATE q'[COMMENT ON COLUMN PGS.NTDC.NTDCESSG IS '0 no es de seguros, 1 de seguros PENDIENTE (no aplicada a la factura), 2 de seguros LIBERADA (aplicada)']';
    EXECUTE IMMEDIATE q'[COMMENT ON COLUMN PGS.NTCC.NTCCESSG IS '0 no es de seguros, 1 de seguros PENDIENTE de liberar en credito, 2 LIBERADA. La NC se aplica a la factura al registrarse, como cualquier NC']';
    EXECUTE IMMEDIATE q'[COMMENT ON COLUMN PGS.DCXP.DCXPESSG IS '1 = el documento de la bandeja es de seguros: el registro lo marca aunque el check no venga (lo pone registrarDesdeXml o el registro)']';
    EXECUTE IMMEDIATE q'[COMMENT ON COLUMN PGS.FCTC.FCTCPOSG IS 'CRD.POSG.POSGCDGO: documento de seguro de credito enlazado (sin FK: otro esquema)']';
    EXECUTE IMMEDIATE q'[COMMENT ON COLUMN PGS.NTDC.NTDCPOSG IS 'CRD.POSG.POSGCDGO: documento de seguro de credito enlazado (sin FK: otro esquema)']';
    EXECUTE IMMEDIATE q'[COMMENT ON COLUMN PGS.NTCC.NTCCPOSG IS 'CRD.POSG.POSGCDGO: documento de seguro de credito enlazado (sin FK: otro esquema)']';
END;
/

-- BLOQUE 2 — CONTROL DESPUÉS
-- ESPERADO: 8 filas. Las cuatro ESSG con NULLABLE = 'N' y DATA_DEFAULT = 0; las cuatro POSG nullable.
SELECT 'BLOQUE 2 - columnas' AS bloque, TABLE_NAME, COLUMN_NAME, DATA_TYPE, NULLABLE, DATA_DEFAULT
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'PGS'
   AND COLUMN_NAME IN ('FCTCESSG', 'FCTCPOSG', 'NTDCESSG', 'NTDCPOSG', 'NTCCESSG', 'NTCCPOSG', 'DCXPESSG', 'DCXPPOSG')
 ORDER BY TABLE_NAME, COLUMN_NAME;

-- ESPERADO: ninguna fila marcada todavía (todas en 0).
SELECT 'BLOQUE 2 - marcados' AS bloque, 'FCTC' AS tabla, COUNT(*) AS marcados FROM PGS.FCTC WHERE FCTCESSG <> 0
UNION ALL SELECT 'BLOQUE 2 - marcados', 'NTDC', COUNT(*) FROM PGS.NTDC WHERE NTDCESSG <> 0
UNION ALL SELECT 'BLOQUE 2 - marcados', 'NTCC', COUNT(*) FROM PGS.NTCC WHERE NTCCESSG <> 0;

-- REVERSO (comentado; sólo ANTES de desplegar un WAR que mapee estas columnas)
-- ALTER TABLE PGS.FCTC DROP (FCTCESSG, FCTCPOSG);
-- ALTER TABLE PGS.NTDC DROP (NTDCESSG, NTDCPOSG);
-- ALTER TABLE PGS.NTCC DROP (NTCCESSG, NTCCPOSG);
-- ALTER TABLE PGS.DCXP DROP (DCXPESSG, DCXPPOSG);

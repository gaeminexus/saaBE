-- =====================================================================================
-- e2-109 — DDL: la devolución de saldo a favor del proveedor (PGS.DVPR/DDPR) acepta también el saldo
--          de una NOTA DE CRÉDITO de compra (PGS.NTCC), y registra si el proveedor devolvió por
--          TRANSFERENCIA o con CHEQUE.
-- Equipo omen-saa-2 · 2026-10-27 · Diseño: docs/logica-negocio/cxp/NC-EXCEDE-SALDO-FACTURA.md (§5)
--
-- Cambios:
--   PGS.DDPR.ANTPCDGO      pasa a aceptar NULL (una línea es de un anticipo O de una NC)
--   PGS.DDPR.DDPRNTCC      NUEVA, NUMBER, FK a PGS.NTCC(ID)
--   CK_DDPR_ORIGEN         exactamente uno de ANTPCDGO / DDPRNTCC
--   UK_DDPR_DVPR_ANTP      se REEMPLAZA por UK_DDPR_DVPR_ORIG (DVPRCDGO, ANTPCDGO, DDPRNTCC): con
--                          ANTPCDGO NULL, la UK vieja de 2 columnas rechazaría DOS notas de crédito en
--                          la misma devolución (Oracle indexa (5, NULL) y lo trata como duplicado)
--   PGS.DVPR.DVPRFPAG      NUEVA, NUMBER DEFAULT 1 NOT NULL, CHECK IN (1, 2): 1 TRANSFERENCIA, 2 CHEQUE.
--                          Las devoluciones existentes quedan en 1 (fueron depósitos/transferencias)
--
-- ⛔ VA ANTES DEL WAR que trae DetalleDevolucionAnticipo.notaCredito y DevolucionAnticipoProveedor.formaPago.
--    Con el WAR primero, CUALQUIER lectura de DVPR/DDPR revienta con ORA-00904 — y anularAnticipo
--    consulta DDPR, así que también la anulación de cualquier anticipo de proveedor.
-- Sin INSERT, sin secuencias. Correr de corrido: cada paso verifica si ya está hecho (idempotente).
-- Columnas copiadas de las entidades: DetalleDevolucionAnticipo (DDPRCDGO, DVPRCDGO, ANTPCDGO, DDPRVLOR),
-- DevolucionAnticipoProveedor (DVPR*), NotaCreditoCompra (@Table NTCC, schema PGS, PK "ID").
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES
-- ESPERADO: ANTPCDGO con NULLABLE = 'N'; no existen DDPRNTCC ni DVPRFPAG; existe UK_DDPR_DVPR_ANTP.
SELECT 'BLOQUE 0 - columnas' AS bloque, TABLE_NAME, COLUMN_NAME, DATA_TYPE, NULLABLE, DATA_DEFAULT
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'PGS' AND TABLE_NAME IN ('DDPR', 'DVPR')
   AND COLUMN_NAME IN ('ANTPCDGO', 'DDPRNTCC', 'DVPRFPAG')
 ORDER BY TABLE_NAME, COLUMN_NAME;
SELECT 'BLOQUE 0 - restricciones' AS bloque, TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE
  FROM ALL_CONSTRAINTS
 WHERE OWNER = 'PGS' AND TABLE_NAME IN ('DDPR', 'DVPR') AND CONSTRAINT_NAME NOT LIKE 'SYS_%'
 ORDER BY TABLE_NAME, CONSTRAINT_NAME;
SELECT 'BLOQUE 0 - filas actuales' AS bloque,
       (SELECT COUNT(*) FROM PGS.DVPR) AS devoluciones,
       (SELECT COUNT(*) FROM PGS.DDPR) AS detalles
  FROM DUAL;

-- BLOQUE 1 — DDL (idempotente)
DECLARE
    v_n NUMBER;
BEGIN
    -- 1. DDPR.ANTPCDGO acepta NULL
    SELECT COUNT(*) INTO v_n FROM ALL_TAB_COLUMNS
     WHERE OWNER = 'PGS' AND TABLE_NAME = 'DDPR' AND COLUMN_NAME = 'ANTPCDGO' AND NULLABLE = 'N';
    IF v_n = 1 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE PGS.DDPR MODIFY (ANTPCDGO NULL)';
    END IF;

    -- 2. DDPR.DDPRNTCC
    SELECT COUNT(*) INTO v_n FROM ALL_TAB_COLUMNS
     WHERE OWNER = 'PGS' AND TABLE_NAME = 'DDPR' AND COLUMN_NAME = 'DDPRNTCC';
    IF v_n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE PGS.DDPR ADD (DDPRNTCC NUMBER)';
    END IF;

    SELECT COUNT(*) INTO v_n FROM ALL_CONSTRAINTS
     WHERE OWNER = 'PGS' AND TABLE_NAME = 'DDPR' AND CONSTRAINT_NAME = 'FK_DDPR_NTCC';
    IF v_n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE PGS.DDPR ADD CONSTRAINT FK_DDPR_NTCC '
            || 'FOREIGN KEY (DDPRNTCC) REFERENCES PGS.NTCC (ID)';
    END IF;

    SELECT COUNT(*) INTO v_n FROM ALL_INDEXES
     WHERE OWNER = 'PGS' AND INDEX_NAME = 'IX_DDPR_NTCC';
    IF v_n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX PGS.IX_DDPR_NTCC ON PGS.DDPR (DDPRNTCC)';
    END IF;

    -- 3. Exactamente un origen por línea
    SELECT COUNT(*) INTO v_n FROM ALL_CONSTRAINTS
     WHERE OWNER = 'PGS' AND TABLE_NAME = 'DDPR' AND CONSTRAINT_NAME = 'CK_DDPR_ORIGEN';
    IF v_n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE PGS.DDPR ADD CONSTRAINT CK_DDPR_ORIGEN CHECK ('
            || '(ANTPCDGO IS NOT NULL AND DDPRNTCC IS NULL) OR (ANTPCDGO IS NULL AND DDPRNTCC IS NOT NULL))';
    END IF;

    -- 4. UK de 2 columnas -> UK de 3 columnas
    SELECT COUNT(*) INTO v_n FROM ALL_CONSTRAINTS
     WHERE OWNER = 'PGS' AND TABLE_NAME = 'DDPR' AND CONSTRAINT_NAME = 'UK_DDPR_DVPR_ANTP';
    IF v_n = 1 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE PGS.DDPR DROP CONSTRAINT UK_DDPR_DVPR_ANTP DROP INDEX';
    END IF;

    SELECT COUNT(*) INTO v_n FROM ALL_CONSTRAINTS
     WHERE OWNER = 'PGS' AND TABLE_NAME = 'DDPR' AND CONSTRAINT_NAME = 'UK_DDPR_DVPR_ORIG';
    IF v_n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE PGS.DDPR ADD CONSTRAINT UK_DDPR_DVPR_ORIG '
            || 'UNIQUE (DVPRCDGO, ANTPCDGO, DDPRNTCC)';
    END IF;

    -- 5. DVPR.DVPRFPAG
    SELECT COUNT(*) INTO v_n FROM ALL_TAB_COLUMNS
     WHERE OWNER = 'PGS' AND TABLE_NAME = 'DVPR' AND COLUMN_NAME = 'DVPRFPAG';
    IF v_n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE PGS.DVPR ADD (DVPRFPAG NUMBER DEFAULT 1 NOT NULL)';
    END IF;

    SELECT COUNT(*) INTO v_n FROM ALL_CONSTRAINTS
     WHERE OWNER = 'PGS' AND TABLE_NAME = 'DVPR' AND CONSTRAINT_NAME = 'CK_DVPRFPAG';
    IF v_n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE PGS.DVPR ADD CONSTRAINT CK_DVPRFPAG CHECK (DVPRFPAG IN (1, 2))';
    END IF;

    EXECUTE IMMEDIATE q'[COMMENT ON COLUMN PGS.DDPR.DDPRNTCC IS 'Nota de credito de compra (PGS.NTCC) cuyo saldo a favor se devuelve. Excluyente con ANTPCDGO']';
    EXECUTE IMMEDIATE q'[COMMENT ON COLUMN PGS.DVPR.DVPRFPAG IS 'Forma en que el proveedor devolvio: 1 TRANSFERENCIA, 2 CHEQUE']';
END;
/

-- BLOQUE 2 — CONTROL DESPUÉS
-- ESPERADO:
--   columnas: DDPR.ANTPCDGO NULLABLE='Y'; DDPR.DDPRNTCC NUMBER 'Y'; DVPR.DVPRFPAG NUMBER 'N' default 1
--   restricciones DDPR: CK_DDPRVLOR, CK_DDPR_ORIGEN, FK_DDPR_ANTP, FK_DDPR_DVPR, FK_DDPR_NTCC, PK_DDPR,
--                       UK_DDPR_DVPR_ORIG   (y YA NO UK_DDPR_DVPR_ANTP)
--   restricciones DVPR: CK_DVPRESTD, CK_DVPRFPAG, CK_DVPRVLOR, PK_DVPR
--   filas: las mismas del BLOQUE 0, y todas las devoluciones con DVPRFPAG = 1
SELECT 'BLOQUE 2 - columnas' AS bloque, TABLE_NAME, COLUMN_NAME, DATA_TYPE, NULLABLE, DATA_DEFAULT
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'PGS' AND TABLE_NAME IN ('DDPR', 'DVPR')
   AND COLUMN_NAME IN ('ANTPCDGO', 'DDPRNTCC', 'DVPRFPAG')
 ORDER BY TABLE_NAME, COLUMN_NAME;
SELECT 'BLOQUE 2 - restricciones' AS bloque, TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE, STATUS
  FROM ALL_CONSTRAINTS
 WHERE OWNER = 'PGS' AND TABLE_NAME IN ('DDPR', 'DVPR') AND CONSTRAINT_NAME NOT LIKE 'SYS_%'
 ORDER BY TABLE_NAME, CONSTRAINT_NAME;
SELECT 'BLOQUE 2 - filas' AS bloque,
       (SELECT COUNT(*) FROM PGS.DVPR) AS devoluciones,
       (SELECT COUNT(*) FROM PGS.DVPR WHERE DVPRFPAG = 1) AS devoluciones_transferencia,
       (SELECT COUNT(*) FROM PGS.DDPR) AS detalles,
       (SELECT COUNT(*) FROM PGS.DDPR WHERE ANTPCDGO IS NULL) AS detalles_sin_anticipo
  FROM DUAL;
-- detalles_sin_anticipo tiene que ser 0 al terminar este script (todavía no hay devoluciones de NC).

-- REVERSO (comentado; sólo si NINGUNA fila de DDPR tiene DDPRNTCC y hay que deshacerlo)
-- ALTER TABLE PGS.DVPR DROP CONSTRAINT CK_DVPRFPAG;
-- ALTER TABLE PGS.DVPR DROP COLUMN DVPRFPAG;
-- ALTER TABLE PGS.DDPR DROP CONSTRAINT UK_DDPR_DVPR_ORIG DROP INDEX;
-- ALTER TABLE PGS.DDPR ADD CONSTRAINT UK_DDPR_DVPR_ANTP UNIQUE (DVPRCDGO, ANTPCDGO);
-- ALTER TABLE PGS.DDPR DROP CONSTRAINT CK_DDPR_ORIGEN;
-- ALTER TABLE PGS.DDPR DROP CONSTRAINT FK_DDPR_NTCC;
-- DROP INDEX PGS.IX_DDPR_NTCC;
-- ALTER TABLE PGS.DDPR DROP COLUMN DDPRNTCC;
-- ALTER TABLE PGS.DDPR MODIFY (ANTPCDGO NOT NULL);

-- =====================================================================================
-- e2-111 — Tipos de asiento automáticos IN-A / EG-A / DI-A (desde el 01-11-2026):
--          check «banco real» en TSR.BNCO y módulo origen con numeración mensual propia en CNT.ASNT.
-- Equipo omen-saa-2 · 2026-10-27 · Diseño: docs/logica-negocio/cnt/DISENO-TIPOS-ASIENTO-AUTOMATICOS.md
--
-- BLOQUE 0  SOLO LECTURA — pasar la salida al árbitro: decide qué bancos NO son reales y si el alterno 7/8/9 choca.
-- BLOQUE 1  DDL idempotente (columnas nuevas; todos los bancos quedan «reales» por defecto).
-- BLOQUE 2  OPCIONAL, comentado: marcar como NO reales los bancos que decida el usuario.
-- BLOQUE 3  control después.
--
-- ⛔ VA ANTES DEL WAR que mapea Banco.bancoReal y Asiento.moduloOrigen/numeroMesModulo/numeroAlternoModulo:
--    con el WAR primero, toda lectura de bancos y de asientos revienta con ORA-00904.
-- Columnas copiadas de las entidades: TipoAsiento (CNT.PLNT: PLNTCDGO, PLNTNMBR, PLNTCDAL, PLNTESTD, PJRQCDGO,
-- PLNTSSTM), Asiento (CNT.ASNT: ASNTCDGO, PJRQCDGO, PLNTCDGO, ASNTPRDO mes, ASNTANOO año, ASNTNMAL, ASNTNMMS),
-- Banco (TSR.BNCO: BNCOCDGO, BNCONMBR, BNCOESTD, PJRQCDGO), CuentaBancaria (TSR.CNBC: CNBCCDGO, BNCOCDGO,
-- PLNNCDGO, CNBCESTD), PlanCuenta (CNT.PLNN: PLNNCDGO, PLNNCNTA, PLNNNMBR), DetalleAsiento (CNT.DTAS: ASNTCDGO, PLNNCDGO, DTASDBEE, DTASHBRR).
-- Sin INSERT, sin secuencias.
-- =====================================================================================

-- BLOQUE 0a — Tipos de asiento con alterno 1..9 por empresa. generarAsiento busca el tipo SOLO por empresa + alterno
-- (TipoAsientoDaoService.selectByAlterno no filtra PLNTSSTM). ESPERADO: por empresa, UNA sola fila con alterno 7, 8 y 9
-- (IN-A, EG-A, DI-A). Si aparece otra fila con alterno 7 (p. ej. la vieja TRANSFERENCIAS), hay choque: pásame la salida.
SELECT 'BLOQUE 0a - tipos' AS bloque, t.PJRQCDGO AS empresa, t.PLNTCDAL AS alterno, t.PLNTCDGO, t.PLNTNMBR,
       t.PLNTSSTM AS sistema, t.PLNTESTD AS estado
  FROM CNT.PLNT t
 WHERE t.PLNTCDAL BETWEEN 1 AND 9
 ORDER BY t.PJRQCDGO, t.PLNTCDAL, t.PLNTCDGO;

-- BLOQUE 0b — Bancos y sus cuentas bancarias con la cuenta contable. El usuario decide cuáles NO son bancos reales
-- (caja, transitorias, cuentas puente, inversiones…). Hoy TODOS cuentan como reales cuando corra el BLOQUE 1.
SELECT 'BLOQUE 0b - bancos' AS bloque, b.PJRQCDGO AS empresa, b.BNCOCDGO, b.BNCONMBR, b.BNCOESTD AS banco_estado,
       c.CNBCCDGO, c.CNBCESTD AS cuenta_estado, p.PLNNCDGO, p.PLNNCNTA AS cuenta_contable, p.PLNNNMBR AS nombre_cuenta
  FROM TSR.BNCO b
  LEFT JOIN TSR.CNBC c ON c.BNCOCDGO = b.BNCOCDGO
  LEFT JOIN CNT.PLNN p ON p.PLNNCDGO = c.PLNNCDGO
 ORDER BY b.PJRQCDGO, b.BNCONMBR, c.CNBCCDGO;

-- BLOQUE 0c — ¿Ya hay asientos con fecha >= 01-11-2026? ESPERADO: 0 (si hay, se generaron con la regla vieja).
SELECT 'BLOQUE 0c - asientos noviembre' AS bloque, COUNT(*) AS asientos
  FROM CNT.ASNT a
 WHERE (a.ASNTANOO = 2026 AND a.ASNTPRDO >= 11) OR a.ASNTANOO > 2026;

-- BLOQUE 1 — DDL (idempotente)
DECLARE
    v_n NUMBER;
BEGIN
    -- TSR.BNCO: banco real (1 sí, 0 no). Default 1: todos los existentes quedan reales hasta que el usuario diga.
    SELECT COUNT(*) INTO v_n FROM ALL_TAB_COLUMNS WHERE OWNER = 'TSR' AND TABLE_NAME = 'BNCO' AND COLUMN_NAME = 'BNCOREAL';
    IF v_n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE TSR.BNCO ADD (BNCOREAL NUMBER(1) DEFAULT 1 NOT NULL)';
        EXECUTE IMMEDIATE 'ALTER TABLE TSR.BNCO ADD CONSTRAINT CK_BNCOREAL CHECK (BNCOREAL IN (0, 1))';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN TSR.BNCO.BNCOREAL IS 'Banco real (1) o no (0). Un asiento automatico desde 01-11-2026 con una cuenta contable de una cuenta bancaria de un banco real es IN-A o EG-A; sin ella, DI-A']';
    END IF;

    -- CNT.ASNT: módulo origen, consecutivo mensual por módulo y número alterno por módulo
    SELECT COUNT(*) INTO v_n FROM ALL_TAB_COLUMNS WHERE OWNER = 'CNT' AND TABLE_NAME = 'ASNT' AND COLUMN_NAME = 'ASNTMDOR';
    IF v_n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE CNT.ASNT ADD (ASNTMDOR VARCHAR2(3))';
        EXECUTE IMMEDIATE q'[ALTER TABLE CNT.ASNT ADD CONSTRAINT CK_ASNTMDOR CHECK (ASNTMDOR IS NULL OR ASNTMDOR IN ('CRE','CXC','CXP','TSR','RRH','CNT'))]';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN CNT.ASNT.ASNTMDOR IS 'Modulo origen del asiento automatico (CRE, CXC, CXP, TSR, RRH, CNT). Nulo en manuales y en los anteriores al 01-11-2026']';
    END IF;

    SELECT COUNT(*) INTO v_n FROM ALL_TAB_COLUMNS WHERE OWNER = 'CNT' AND TABLE_NAME = 'ASNT' AND COLUMN_NAME = 'ASNTNMMD';
    IF v_n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE CNT.ASNT ADD (ASNTNMMD NUMBER)';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN CNT.ASNT.ASNTNMMD IS 'Consecutivo mensual por empresa + modulo origen + anio + mes']';
    END IF;

    SELECT COUNT(*) INTO v_n FROM ALL_TAB_COLUMNS WHERE OWNER = 'CNT' AND TABLE_NAME = 'ASNT' AND COLUMN_NAME = 'ASNTNAMD';
    IF v_n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE CNT.ASNT ADD (ASNTNAMD VARCHAR2(30))';
        EXECUTE IMMEDIATE q'[COMMENT ON COLUMN CNT.ASNT.ASNTNAMD IS 'Numero alterno por modulo, p. ej. CXP-2026-11-0001 (doble control con ASNTNMAL)']';
    END IF;

    -- Índice para el MAX+1 del consecutivo por módulo
    SELECT COUNT(*) INTO v_n FROM ALL_INDEXES WHERE OWNER = 'CNT' AND INDEX_NAME = 'IX_ASNT_MDOR';
    IF v_n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX CNT.IX_ASNT_MDOR ON CNT.ASNT (PJRQCDGO, ASNTMDOR, ASNTANOO, ASNTPRDO)';
    END IF;
END;
/

-- BLOQUE 2 — Marcar como NO reales los 6 que decidió el usuario el 2026-10-28:
--   425 CAJA CHICA OFICINAS, 427 CAJA CHICA PENDIENTE LEGAL, 432 CUENTA TRANSITORIA,
--   433 ARRIENDO RECIBIDOS POR ANTICIPADO, 435 ANTICIPO A PROVEEDORES, 438 DEPOSITOS POR CONFIRMAR.
-- Solo pesa en asientos con fecha >= 2026-11-01 (antes, el tipo no se clasifica por banco). Todo o nada.
DECLARE
    v_n NUMBER;
BEGIN
    UPDATE TSR.BNCO SET BNCOREAL = 0
     WHERE BNCOCDGO IN (425, 427, 432, 433, 435, 438) AND BNCOREAL = 1;
    v_n := SQL%ROWCOUNT;
    IF v_n <> 6 THEN
        ROLLBACK;
        RAISE_APPLICATION_ERROR(-20001, 'Se esperaban 6 bancos y se actualizaron ' || v_n || '. No cambia nada.');
    END IF;
    COMMIT;
END;
/
-- Control del BLOQUE 2. ESPERADO: BNCOREAL 0 -> 6, BNCOREAL 1 -> 12.
SELECT 'BLOQUE 2 - control' AS bloque, BNCOREAL, COUNT(*) AS bancos FROM TSR.BNCO GROUP BY BNCOREAL ORDER BY BNCOREAL;
-- Reverso del BLOQUE 2 (comentado):
-- UPDATE TSR.BNCO SET BNCOREAL = 1 WHERE BNCOCDGO IN (425, 427, 432, 433, 435, 438); COMMIT;

-- BLOQUE 3 — CONTROL DESPUÉS
-- ESPERADO: BNCOREAL NUMBER 'N' default 1; ASNTMDOR VARCHAR2(3), ASNTNMMD NUMBER, ASNTNAMD VARCHAR2(30), todas 'Y';
-- restricciones CK_BNCOREAL y CK_ASNTMDOR habilitadas; índice IX_ASNT_MDOR; bancos con su BNCOREAL.
SELECT 'BLOQUE 3 - columnas' AS bloque, OWNER, TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE, DATA_DEFAULT
  FROM ALL_TAB_COLUMNS
 WHERE (OWNER = 'TSR' AND TABLE_NAME = 'BNCO' AND COLUMN_NAME = 'BNCOREAL')
    OR (OWNER = 'CNT' AND TABLE_NAME = 'ASNT' AND COLUMN_NAME IN ('ASNTMDOR', 'ASNTNMMD', 'ASNTNAMD'))
 ORDER BY OWNER, TABLE_NAME, COLUMN_NAME;
SELECT 'BLOQUE 3 - restricciones' AS bloque, OWNER, TABLE_NAME, CONSTRAINT_NAME, STATUS
  FROM ALL_CONSTRAINTS WHERE CONSTRAINT_NAME IN ('CK_BNCOREAL', 'CK_ASNTMDOR');
SELECT 'BLOQUE 3 - indice' AS bloque, OWNER, INDEX_NAME, TABLE_NAME FROM ALL_INDEXES WHERE INDEX_NAME = 'IX_ASNT_MDOR';
SELECT 'BLOQUE 3 - bancos' AS bloque, BNCOCDGO, BNCONMBR, BNCOREAL FROM TSR.BNCO ORDER BY BNCONMBR;

-- REVERSO (comentado; solo si ningún asiento tiene ASNTMDOR)
-- DROP INDEX CNT.IX_ASNT_MDOR;
-- ALTER TABLE CNT.ASNT DROP CONSTRAINT CK_ASNTMDOR;
-- ALTER TABLE CNT.ASNT DROP (ASNTMDOR, ASNTNMMD, ASNTNAMD);
-- ALTER TABLE TSR.BNCO DROP CONSTRAINT CK_BNCOREAL;
-- ALTER TABLE TSR.BNCO DROP (BNCOREAL);

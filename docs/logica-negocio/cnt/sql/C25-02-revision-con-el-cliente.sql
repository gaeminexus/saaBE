-- =====================================================================================
-- C25-02 — Seguimiento de novedades: lo que muestra el HTML de revisión con el cliente (pedido del líder,
--          11-nov-2026). Incremental sobre C25-01.
-- Cuadre Superintendencia, tarea C25 · 2026-11-11 · Diseño: expediente cuadreSuper,
-- 03-trabajo/cambios-saa/c25-seguimiento-novedades/C25-SEGUIMIENTO-NOVEDADES.md §8 (Implementación, ampliación C25b).
--
-- QUE HACE:
--   CNT.NVDD  + 9 columnas: por qué importa, qué se registró, monto y su nota, nota de la revisión, estado según la
--             revisión, pedido general, cifras (JSON) y asientos de cuentas por regularizar repartidos a la novedad (JSON).
--             Salen de 03-trabajo/revision/datos-AAAA-MM.json (textos curados) a través del novedades-AAAA-MM.json
--             (formato C25-NOVEDADES-2).
--   CNT.RQNV  + RQNVORGN: de dónde sale el requerimiento (TXT = parser de los .txt; REVISION = pedido extra de la
--             revisión con el cliente; AJUSTE = ajuste manual del expediente; PANTALLA = registrado en el SAA).
--   CNT.DCSN  + título, resumen y razones de la decisión (03-trabajo/revision/decisiones-resumen.json), para el
--             listado de decisiones de la pantalla.
--   CNT.RVNM  tabla nueva: revisión del mes (cabecera del HTML): estado y cierre del mes, flujo del mes, cuadratura
--             B17-G con sus notas y cuentas puente, bancos, observaciones, cuentas por regularizar y el reparto de
--             sus asientos por novedad. Una fila por empresa + mes + versión; la de la versión anterior queda
--             HISTORICA (RVNMESTD = 2), no se borra ni se duplica.
--
-- ⛔ VA ANTES DEL WAR de la rama cuadre/c25b-navegacion: Hibernate valida las columnas nuevas. Con el WAR de C25-01
--    las columnas nuevas no hacen daño (nadie las lee). Requiere C25-01.
--
-- SQL PURO (sin SET/DEFINE/WHENEVER). Solo DDL (commit implícito). Idempotente por control: si el CONTROL 1.1
-- devuelve filas, NO correr (ya está aplicado). Sin datos: se llenan con la recarga del mes
-- (03-generar-sql-carga.js AAAA-MM o POST /nvdd/cargaLote) y de decisiones (03-generar-sql-carga.js decisiones).
-- =====================================================================================

-- BLOQUE 1 — CONTROLES ANTES
-- 1.1 Nada de C25-02 existe todavía. ESPERADO: 0 filas.
SELECT TABLE_NAME, COLUMN_NAME
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'CNT'
   AND ((TABLE_NAME = 'NVDD' AND COLUMN_NAME IN ('NVDDPRQE', 'NVDDRGST', 'NVDDMNTO', 'NVDDMNNT', 'NVDDNTRV', 'NVDDESRV', 'NVDDPDGN', 'NVDDCFRS', 'NVDDRGLR'))
     OR (TABLE_NAME = 'RQNV' AND COLUMN_NAME = 'RQNVORGN')
     OR (TABLE_NAME = 'DCSN' AND COLUMN_NAME IN ('DCSNTTLO', 'DCSNRSMN', 'DCSNRZNS'))
     OR TABLE_NAME = 'RVNM');

-- 1.2 C25-01 está aplicado. ESPERADO: 7 filas (DCSN, NVDD, RQNV, RSNV, ADNV, VNNV, HSNV).
SELECT TABLE_NAME FROM ALL_TABLES
 WHERE OWNER = 'CNT' AND TABLE_NAME IN ('DCSN', 'NVDD', 'RQNV', 'RSNV', 'ADNV', 'VNNV', 'HSNV')
 ORDER BY TABLE_NAME;

-- 1.3 Ninguna restricción ni índice con los nombres nuevos. ESPERADO: 0 filas.
SELECT CONSTRAINT_NAME AS NOMBRE FROM ALL_CONSTRAINTS
 WHERE OWNER = 'CNT' AND CONSTRAINT_NAME IN ('CK_NVDD_ESRV', 'CK_NVDD_CFRS', 'CK_NVDD_RGLR', 'CK_RQNV_ORGN', 'PK_RVNM', 'FK_RVNM_PJRQ',
       'FK_RVNM_PRDO', 'UK_RVNM_MES', 'CK_RVNM_MSSS', 'CK_RVNM_ESTD', 'CK_RVNM_FLJO', 'CK_RVNM_CDRT', 'CK_RVNM_BNCS', 'CK_RVNM_OBSR',
       'CK_RVNM_CNTS', 'CK_RVNM_RGLR')
UNION ALL
SELECT INDEX_NAME FROM ALL_INDEXES WHERE OWNER = 'CNT' AND INDEX_NAME IN ('UK_RVNM_VIGENTE', 'IDX_RVNM_PRDO');


-- =====================================================
-- 2. CNT.NVDD — textos curados de la revisión con el cliente
-- =====================================================
ALTER TABLE CNT.NVDD ADD (
    NVDDPRQE CLOB,
    NVDDRGST CLOB,
    NVDDMNTO NUMBER(18,2),
    NVDDMNNT VARCHAR2(2000),
    NVDDNTRV VARCHAR2(4000),
    NVDDESRV VARCHAR2(25),
    NVDDPDGN VARCHAR2(2000),
    NVDDCFRS CLOB,
    NVDDRGLR CLOB
);

ALTER TABLE CNT.NVDD ADD CONSTRAINT CK_NVDD_ESRV CHECK (NVDDESRV IS NULL OR NVDDESRV IN ('RESUELTO', 'PENDIENTE_DOCUMENTO', 'PENDIENTE_DECISION'));
ALTER TABLE CNT.NVDD ADD CONSTRAINT CK_NVDD_CFRS CHECK (NVDDCFRS IS JSON);
ALTER TABLE CNT.NVDD ADD CONSTRAINT CK_NVDD_RGLR CHECK (NVDDRGLR IS JSON);

COMMENT ON COLUMN CNT.NVDD.NVDDPRQE IS 'Por qué importa (texto curado de la revisión con el cliente, datos-AAAA-MM.json porQue)';
COMMENT ON COLUMN CNT.NVDD.NVDDRGST IS 'Qué se registró en el SAA (texto curado, datos-AAAA-MM.json registro)';
COMMENT ON COLUMN CNT.NVDD.NVDDMNTO IS 'Monto de la novedad según la revisión (datos-AAAA-MM.json monto); NVDDVLOR es el del parser';
COMMENT ON COLUMN CNT.NVDD.NVDDMNNT IS 'Nota que explica el monto (montoNota)';
COMMENT ON COLUMN CNT.NVDD.NVDDNTRV IS 'Nota de la revisión (relación con otras novedades, actualización posterior)';
COMMENT ON COLUMN CNT.NVDD.NVDDESRV IS 'Estado según la revisión con el cliente: RESUELTO, PENDIENTE_DOCUMENTO, PENDIENTE_DECISION (no es el estado de seguimiento NVDDESTD)';
COMMENT ON COLUMN CNT.NVDD.NVDDPDGN IS 'Pedido general de la novedad (p. ej. TABLAS-59), además de sus requerimientos';
COMMENT ON COLUMN CNT.NVDD.NVDDCFRS IS 'Cifras de la novedad, JSON [[concepto, valor, detalle], ...]';
COMMENT ON COLUMN CNT.NVDD.NVDDRGLR IS 'Asientos a cuentas por regularizar repartidos a esta novedad, JSON [{asnt, numero, fecha, cuenta, valor, lote}]';


-- =====================================================
-- 3. CNT.RQNV — origen del requerimiento
-- =====================================================
ALTER TABLE CNT.RQNV ADD (RQNVORGN VARCHAR2(20) DEFAULT 'TXT' NOT NULL);
ALTER TABLE CNT.RQNV ADD CONSTRAINT CK_RQNV_ORGN CHECK (RQNVORGN IN ('TXT', 'REVISION', 'AJUSTE', 'PANTALLA'));
COMMENT ON COLUMN CNT.RQNV.RQNVORGN IS 'Origen: TXT (parser de los .txt), REVISION (pedido extra de la revisión con el cliente), AJUSTE (ajuste manual del expediente), PANTALLA';


-- =====================================================
-- 4. CNT.DCSN — ficha de la decisión para el listado
-- =====================================================
ALTER TABLE CNT.DCSN ADD (
    DCSNTTLO VARCHAR2(300),
    DCSNRSMN VARCHAR2(2000),
    DCSNRZNS VARCHAR2(2000)
);
COMMENT ON COLUMN CNT.DCSN.DCSNTTLO IS 'Título corto de la decisión (decisiones-resumen.json titulo)';
COMMENT ON COLUMN CNT.DCSN.DCSNRSMN IS 'Resumen en una frase (decisiones-resumen.json resumen)';
COMMENT ON COLUMN CNT.DCSN.DCSNRZNS IS 'Razones: por qué se decidió así (decisiones-resumen.json porque)';


-- =====================================================
-- 5. CNT.RVNM (RevisionMesNovedad) — cabecera del HTML de revisión del mes
-- =====================================================
CREATE TABLE CNT.RVNM (
    RVNMCDGO NUMBER GENERATED BY DEFAULT AS IDENTITY (START WITH 1 INCREMENT BY 1) NOT NULL,
    PJRQCDGO NUMBER NOT NULL,
    PRDOCDGO NUMBER,
    RVNMANNN NUMBER(4) NOT NULL,
    RVNMMSSS NUMBER(2) NOT NULL,
    RVNMVRSN NUMBER DEFAULT 1 NOT NULL,
    RVNMNMBR VARCHAR2(50),
    RVNMCRTE VARCHAR2(30),
    RVNMESMS VARCHAR2(100),
    RVNMFCCR VARCHAR2(30),
    RVNMVRCR VARCHAR2(300),
    RVNMEJRC NUMBER,
    RVNMFLJO CLOB,
    RVNMCDRT CLOB,
    RVNMBNCS CLOB,
    RVNMOBSR CLOB,
    RVNMCNTS CLOB,
    RVNMRGLR CLOB,
    RVNMFNTE VARCHAR2(2000),
    RVNMLTCR VARCHAR2(50),
    RVNMESTD NUMBER DEFAULT 1 NOT NULL,
    RVNMFCRG TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    RVNMUSAR VARCHAR2(50) NOT NULL,
    RVNMFCMD TIMESTAMP,
    RVNMUSMD VARCHAR2(50),
    CONSTRAINT PK_RVNM PRIMARY KEY (RVNMCDGO),
    CONSTRAINT FK_RVNM_PJRQ FOREIGN KEY (PJRQCDGO) REFERENCES SCP.PJRQ(PJRQCDGO),
    CONSTRAINT FK_RVNM_PRDO FOREIGN KEY (PRDOCDGO) REFERENCES CNT.PRDO(PRDOCDGO),
    CONSTRAINT UK_RVNM_MES UNIQUE (PJRQCDGO, RVNMANNN, RVNMMSSS, RVNMVRSN),
    CONSTRAINT CK_RVNM_MSSS CHECK (RVNMMSSS BETWEEN 1 AND 12),
    CONSTRAINT CK_RVNM_ESTD CHECK (RVNMESTD IN (1, 2)),
    CONSTRAINT CK_RVNM_FLJO CHECK (RVNMFLJO IS JSON),
    CONSTRAINT CK_RVNM_CDRT CHECK (RVNMCDRT IS JSON),
    CONSTRAINT CK_RVNM_BNCS CHECK (RVNMBNCS IS JSON),
    CONSTRAINT CK_RVNM_OBSR CHECK (RVNMOBSR IS JSON),
    CONSTRAINT CK_RVNM_CNTS CHECK (RVNMCNTS IS JSON),
    CONSTRAINT CK_RVNM_RGLR CHECK (RVNMRGLR IS JSON)
);

-- Una sola revisión VIGENTE por empresa y mes (las de versiones anteriores quedan HISTORICAS).
CREATE UNIQUE INDEX UK_RVNM_VIGENTE ON CNT.RVNM (CASE WHEN RVNMESTD = 1 THEN PJRQCDGO END, CASE WHEN RVNMESTD = 1 THEN RVNMANNN END,
    CASE WHEN RVNMESTD = 1 THEN RVNMMSSS END);
CREATE INDEX IDX_RVNM_PRDO ON CNT.RVNM(PRDOCDGO);

COMMENT ON TABLE CNT.RVNM IS 'C25: revisión del mes con el cliente (cabecera del HTML de revisión): flujo, cuadratura, bancos, observaciones, cuentas por regularizar y reparto por novedad';
COMMENT ON COLUMN CNT.RVNM.RVNMCDGO IS 'Código (PK)';
COMMENT ON COLUMN CNT.RVNM.PJRQCDGO IS 'Empresa (SCP.PJRQ)';
COMMENT ON COLUMN CNT.RVNM.PRDOCDGO IS 'Período contable (CNT.PRDO) del mes';
COMMENT ON COLUMN CNT.RVNM.RVNMANNN IS 'Año';
COMMENT ON COLUMN CNT.RVNM.RVNMMSSS IS 'Mes (1..12)';
COMMENT ON COLUMN CNT.RVNM.RVNMVRSN IS 'Versión del lote de novedades del mes (la misma de CNT.NVDD.NVDDVRSN)';
COMMENT ON COLUMN CNT.RVNM.RVNMNMBR IS 'Nombre del mes (junio 2025)';
COMMENT ON COLUMN CNT.RVNM.RVNMCRTE IS 'Fecha de corte como texto (30-jun-2025)';
COMMENT ON COLUMN CNT.RVNM.RVNMESMS IS 'Estado del mes (Cerrado definitivo)';
COMMENT ON COLUMN CNT.RVNM.RVNMFCCR IS 'Fecha del cierre como texto (29-oct-2026)';
COMMENT ON COLUMN CNT.RVNM.RVNMVRCR IS 'Versión del cierre (qué quedó definitivo)';
COMMENT ON COLUMN CNT.RVNM.RVNMEJRC IS 'Ejecución de las estructuras G del mes (RPR.EJRC.EJRCCDGO)';
COMMENT ON COLUMN CNT.RVNM.RVNMFLJO IS 'Flujo del mes, JSON [[paso, texto], ...]';
COMMENT ON COLUMN CNT.RVNM.RVNMCDRT IS 'Cuadratura B17-G, JSON {filas:[{clave, control, b17, g, diferencia, registros}], notas:{clave: texto}, puentes:[{cuenta, saldo}]}';
COMMENT ON COLUMN CNT.RVNM.RVNMBNCS IS 'Bancos, JSON {estado, texto, transito:[texto]}';
COMMENT ON COLUMN CNT.RVNM.RVNMOBSR IS 'Observaciones del mes, JSON [texto]';
COMMENT ON COLUMN CNT.RVNM.RVNMCNTS IS 'Cuentas, JSON {nombres:{cuenta: nombre}, saldos:[{cuenta, inicial, movimiento, final}]}';
COMMENT ON COLUMN CNT.RVNM.RVNMRGLR IS 'Reparto de los asientos de cuentas por regularizar por novedad, JSON [{codigo, cuenta, valor, filas}] (codigo SIN-NOVEDAD si no tiene regla)';
COMMENT ON COLUMN CNT.RVNM.RVNMFNTE IS 'Archivos de trabajo del expediente de donde sale (solo GAEMII)';
COMMENT ON COLUMN CNT.RVNM.RVNMLTCR IS 'Lote de carga (C25-AAAA-MM-vN)';
COMMENT ON COLUMN CNT.RVNM.RVNMESTD IS '1 VIGENTE, 2 HISTORICA (reemplazada por una versión nueva del mes)';
COMMENT ON COLUMN CNT.RVNM.RVNMFCRG IS 'Fecha de registro';
COMMENT ON COLUMN CNT.RVNM.RVNMUSAR IS 'Usuario que registró';
COMMENT ON COLUMN CNT.RVNM.RVNMFCMD IS 'Fecha de la última recarga';
COMMENT ON COLUMN CNT.RVNM.RVNMUSMD IS 'Usuario de la última recarga';


-- BLOQUE 6 — CONTROLES DESPUÉS
-- 6.1 (DECISIVO) Columnas nuevas. ESPERADO: DCSN 3, NVDD 9, RQNV 1, RVNM 25.
SELECT TABLE_NAME, COUNT(*) AS COLUMNAS
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'CNT'
   AND ((TABLE_NAME = 'NVDD' AND COLUMN_NAME IN ('NVDDPRQE', 'NVDDRGST', 'NVDDMNTO', 'NVDDMNNT', 'NVDDNTRV', 'NVDDESRV', 'NVDDPDGN', 'NVDDCFRS', 'NVDDRGLR'))
     OR (TABLE_NAME = 'RQNV' AND COLUMN_NAME = 'RQNVORGN')
     OR (TABLE_NAME = 'DCSN' AND COLUMN_NAME IN ('DCSNTTLO', 'DCSNRSMN', 'DCSNRZNS'))
     OR TABLE_NAME = 'RVNM')
 GROUP BY TABLE_NAME
 ORDER BY TABLE_NAME;

-- 6.2 Restricciones nuevas. ESPERADO: 16 filas, todas ENABLED.
SELECT CONSTRAINT_NAME, STATUS FROM ALL_CONSTRAINTS
 WHERE OWNER = 'CNT' AND CONSTRAINT_NAME IN ('CK_NVDD_ESRV', 'CK_NVDD_CFRS', 'CK_NVDD_RGLR', 'CK_RQNV_ORGN', 'PK_RVNM', 'FK_RVNM_PJRQ',
       'FK_RVNM_PRDO', 'UK_RVNM_MES', 'CK_RVNM_MSSS', 'CK_RVNM_ESTD', 'CK_RVNM_FLJO', 'CK_RVNM_CDRT', 'CK_RVNM_BNCS', 'CK_RVNM_OBSR',
       'CK_RVNM_CNTS', 'CK_RVNM_RGLR')
 ORDER BY CONSTRAINT_NAME;

-- 6.3 Los requerimientos que ya existían quedaron con origen TXT. ESPERADO: una fila TXT con todos los requerimientos.
SELECT RQNVORGN, COUNT(*) AS CANTIDAD FROM CNT.RQNV GROUP BY RQNVORGN;


-- REVERSO (comentado; borra también los datos de la revisión):
-- DROP TABLE CNT.RVNM;
-- ALTER TABLE CNT.DCSN DROP (DCSNTTLO, DCSNRSMN, DCSNRZNS);
-- ALTER TABLE CNT.RQNV DROP CONSTRAINT CK_RQNV_ORGN;
-- ALTER TABLE CNT.RQNV DROP (RQNVORGN);
-- ALTER TABLE CNT.NVDD DROP CONSTRAINT CK_NVDD_ESRV;
-- ALTER TABLE CNT.NVDD DROP CONSTRAINT CK_NVDD_CFRS;
-- ALTER TABLE CNT.NVDD DROP CONSTRAINT CK_NVDD_RGLR;
-- ALTER TABLE CNT.NVDD DROP (NVDDPRQE, NVDDRGST, NVDDMNTO, NVDDMNNT, NVDDNTRV, NVDDESRV, NVDDPDGN, NVDDCFRS, NVDDRGLR);

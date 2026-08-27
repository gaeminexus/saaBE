-- =====================================================================
-- CONCILIACION: partidas en transito (punto 12)
-- Modulo: TSR
-- Fecha:  2026-08-27
-- Autor:  orquestador (extraido con DBMS_METADATA de la base local y
--         verificado columna por columna; no transcrito a mano)
--
-- PARA QUE
--   Hoy una sola partida en transito impide cerrar el mes para siempre:
--   selectPendientes filtra por periodo y verificar exige cero pendientes,
--   asi que un deposito registrado el 30/abr y acreditado por el banco el
--   02/may no puede conciliarse nunca. Con esta tabla el mes cierra
--   declarando esas partidas, y ellas se arrastran al mes siguiente.
--   Ver docs/logica-negocio/tsr/DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md
--
-- OJO CON EL ORIGEN DE LAS PARTIDAS DE LIBROS
--   La primera version del diseno colgaba las partidas de tipo 1 y 2 de
--   TSR.MVCB. Estaba mal y se corrigio (§7bis): solo 122 de 1.448 detalles
--   de asiento sobre cuentas bancarias tienen MovimientoBanco -- el 8% --
--   porque solo lo crean pagos, cheques y depositos. Un asiento hecho
--   desde contabilidad toca el banco sin generarlo.
--   Por eso cuelgan de CNT.DTAS (DTCNDTAS), que siempre existe, y
--   MVCBCDGO quedo como dato adicional opcional.
--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 0: control previo
--   (a) TSR.DTCN no debe existir; TSR.CNCL y CNT.DTAS si
--   (b) las 4 columnas nuevas de CNCL no deben existir todavia
--   (c) CNT.DTAS debe tener REFERENCES concedido: en la base local lo
--       tiene PUBLIC, por eso la FK cross-schema funciona sin GRANT
--       propio. Si en produccion NO aparece, ejecutar el GRANT del
--       bloque 1 antes de crear la tabla; si aparece, saltarlo.
-- ---------------------------------------------------------------------
SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER = 'TSR' AND TABLE_NAME IN ('DTCN','CNCL','DEXB','MVCB');

SELECT COLUMN_NAME FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'TSR' AND TABLE_NAME = 'CNCL'
   AND COLUMN_NAME IN ('CNCLESTD','CNCLFCCR','CNCLUSCR','CNCLMTAN');

SELECT GRANTEE, TABLE_SCHEMA, TABLE_NAME, PRIVILEGE FROM ALL_TAB_PRIVS
 WHERE PRIVILEGE = 'REFERENCES' AND TABLE_SCHEMA = 'CNT' AND TABLE_NAME = 'DTAS';

-- ---------------------------------------------------------------------
-- BLOQUE 1: privilegio REFERENCES, SOLO si el control (c) salio vacio
--   Oracle no considera los privilegios heredados por ROL al crear un
--   constraint, ni con rol DBA. Ejecutar como DBA o como CNT.
--   Si PUBLIC ya lo tiene, esta linea sobra: NO ejecutarla.
-- ---------------------------------------------------------------------
-- GRANT REFERENCES ON CNT.DTAS TO TSR;

-- ---------------------------------------------------------------------
-- BLOQUE 2: secuencia del PK
--   DTCNCDGO no es identity: la entidad usa @SequenceGenerator sobre
--   TSR.SQ_DTCNCDGO. Sin esta secuencia, insertar falla.
-- ---------------------------------------------------------------------
CREATE SEQUENCE TSR.SQ_DTCNCDGO START WITH 1 INCREMENT BY 1 NOCACHE;

-- ---------------------------------------------------------------------
-- BLOQUE 3: TSR.DTCN — detalle de partidas en transito
--   Los dos CHECK de origen son el corazon del modelo:
--     CK_DTCN_ORIGEN      una partida es de libros O del banco, nunca las dos
--     CK_DTCN_TIPO_ORIGEN  tipos 1 y 2 exigen DTCNDTAS (libros)
--                          tipos 3 y 4 exigen DTCNIDEX (extracto)
-- ---------------------------------------------------------------------
CREATE TABLE TSR.DTCN (
    DTCNCDGO NUMBER NOT NULL,
    CNCLCDGO NUMBER NOT NULL,
    MVCBCDGO NUMBER,
    DTCNIDEX NUMBER,
    DTCNTPOO NUMBER NOT NULL,
    DTCNVLOR NUMBER(18,2) NOT NULL,
    DTCNESTD NUMBER DEFAULT 1 NOT NULL,
    DTCNCNSL NUMBER,
    DTCNOBSR VARCHAR2(1000),
    DTCNFCRG TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    DTCNDTAS NUMBER,
    CONSTRAINT PK_DTCN PRIMARY KEY (DTCNCDGO),
    CONSTRAINT CK_DTCN_TPOO CHECK (DTCNTPOO IN (1,2,3,4)),
    CONSTRAINT CK_DTCN_ESTD CHECK (DTCNESTD IN (1,2)),
    CONSTRAINT CK_DTCN_VLOR CHECK (DTCNVLOR > 0),
    CONSTRAINT CK_DTCN_ORIGEN CHECK ((DTCNDTAS IS NULL) <> (DTCNIDEX IS NULL)),
    CONSTRAINT CK_DTCN_TIPO_ORIGEN CHECK (
        (DTCNTPOO IN (1,2) AND DTCNDTAS IS NOT NULL AND DTCNIDEX IS NULL)
        OR
        (DTCNTPOO IN (3,4) AND DTCNIDEX IS NOT NULL AND DTCNDTAS IS NULL)
    ),
    CONSTRAINT FK_DTCN_CNCL FOREIGN KEY (CNCLCDGO) REFERENCES TSR.CNCL(CNCLCDGO),
    CONSTRAINT FK_DTCN_CNSL FOREIGN KEY (DTCNCNSL) REFERENCES TSR.CNCL(CNCLCDGO),
    CONSTRAINT FK_DTCN_MVCB FOREIGN KEY (MVCBCDGO) REFERENCES TSR.MVCB(MVCBCDGO),
    CONSTRAINT FK_DTCN_DEXB FOREIGN KEY (DTCNIDEX) REFERENCES TSR.DEXB(DEXBCDGO),
    CONSTRAINT FK_DTCN_DTAS FOREIGN KEY (DTCNDTAS) REFERENCES CNT.DTAS(DTASCDGO)
);

-- Indices de las FK. Oracle NO los crea solo, y sin ellos borrar o
-- actualizar un CNCL, un MVCB o un asiento obliga a un full scan de DTCN.
-- Prefijar el schema: sin prefijo el indice queda en el schema de la
-- SESION, ocupa la columna igual y luego da ORA-01408.
CREATE INDEX TSR.IDX_DTCN_CNCL ON TSR.DTCN(CNCLCDGO);
CREATE INDEX TSR.IDX_DTCN_CNSL ON TSR.DTCN(DTCNCNSL);
CREATE INDEX TSR.IDX_DTCN_DTAS ON TSR.DTCN(DTCNDTAS);
CREATE INDEX TSR.IDX_DTCN_DEXB ON TSR.DTCN(DTCNIDEX);
CREATE INDEX TSR.IDX_DTCN_ESTD ON TSR.DTCN(DTCNESTD);

COMMENT ON TABLE TSR.DTCN IS 'Partidas en transito declaradas al cerrar una conciliacion: que partida quedo sin conciliar, de que tipo, que cierre la declaro y cual la saldo';
COMMENT ON COLUMN TSR.DTCN.DTCNCDGO IS 'PK, desde TSR.SQ_DTCNCDGO';
COMMENT ON COLUMN TSR.DTCN.CNCLCDGO IS 'Cierre que DECLARO la partida (TSR.CNCL)';
COMMENT ON COLUMN TSR.DTCN.MVCBCDGO IS 'Movimiento bancario, si existe. OPCIONAL: solo el 8% de los asientos bancarios tiene uno';
COMMENT ON COLUMN TSR.DTCN.DTCNIDEX IS 'Linea del extracto no registrada en libros (TSR.DEXB), para tipos 3 y 4';
COMMENT ON COLUMN TSR.DTCN.DTCNDTAS IS 'Detalle de asiento no acreditado por el banco (CNT.DTAS), para tipos 1 y 2. Es el origen real: siempre existe';
COMMENT ON COLUMN TSR.DTCN.DTCNTPOO IS '1 Deposito en transito, 2 Cheque girado no cobrado, 3 NC del banco no registrada, 4 ND del banco no registrada';
COMMENT ON COLUMN TSR.DTCN.DTCNVLOR IS 'Valor, siempre positivo; el tipo decide el signo en la ecuacion del cierre';
COMMENT ON COLUMN TSR.DTCN.DTCNESTD IS '1 Pendiente, 2 Saldada';
COMMENT ON COLUMN TSR.DTCN.DTCNCNSL IS 'Cierre en el que SE SALDO. Queda nulo si se salda por la conciliacion N:M ordinaria, que no crea ningun cierre';
COMMENT ON COLUMN TSR.DTCN.DTCNOBSR IS 'Por que quedo en transito';

-- ---------------------------------------------------------------------
-- BLOQUE 4: TSR.CNCL — el cierre gana estado, fecha, usuario y motivo
--   CNCL ya modelaba el cuadre clasico completo pero no tenia ciclo de
--   vida. Verificado: la tabla esta VACIA, asi que ampliarla no arriesga
--   ningun dato.
-- ---------------------------------------------------------------------
ALTER TABLE TSR.CNCL ADD (
    CNCLESTD NUMBER,
    CNCLFCCR TIMESTAMP(6),
    CNCLUSCR VARCHAR2(60),
    CNCLMTAN VARCHAR2(500)
);

COMMENT ON COLUMN TSR.CNCL.CNCLESTD IS 'Estado del cierre: 1 Borrador, 2 Cerrado, 3 Anulado. No confundir con CNCLRZZA, que es el estado del mecanismo viejo insertaConciliacion, nunca usado en produccion';
COMMENT ON COLUMN TSR.CNCL.CNCLFCCR IS 'Fecha del cierre';
COMMENT ON COLUMN TSR.CNCL.CNCLUSCR IS 'Usuario que cerro';
COMMENT ON COLUMN TSR.CNCL.CNCLMTAN IS 'Motivo de anulacion, con el nombre de quien anulo anexado al final';

-- ---------------------------------------------------------------------
-- BLOQUE 5: control final
--   Esperado: 11 columnas en DTCN, 5 FK, 5 CHECK propios, 6 indices
--   (PK + los 5 creados), la secuencia, y 4 columnas nuevas en CNCL.
-- ---------------------------------------------------------------------
SELECT COUNT(*) AS COLUMNAS_DTCN FROM ALL_TAB_COLUMNS WHERE OWNER = 'TSR' AND TABLE_NAME = 'DTCN';

SELECT CONSTRAINT_TYPE, COUNT(*) AS N FROM ALL_CONSTRAINTS
 WHERE OWNER = 'TSR' AND TABLE_NAME = 'DTCN' AND CONSTRAINT_NAME NOT LIKE 'SYS_%'
 GROUP BY CONSTRAINT_TYPE ORDER BY 1;

SELECT INDEX_NAME FROM ALL_INDEXES WHERE TABLE_OWNER = 'TSR' AND TABLE_NAME = 'DTCN' ORDER BY 1;

SELECT SEQUENCE_NAME FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER = 'TSR' AND SEQUENCE_NAME = 'SQ_DTCNCDGO';

SELECT COLUMN_NAME, DATA_TYPE FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'TSR' AND TABLE_NAME = 'CNCL'
   AND COLUMN_NAME IN ('CNCLESTD','CNCLFCCR','CNCLUSCR','CNCLMTAN') ORDER BY COLUMN_ID;

COMMIT;

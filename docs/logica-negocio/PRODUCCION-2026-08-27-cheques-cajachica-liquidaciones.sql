-- #####################################################################
-- #  SCRIPT UNICO DE PRODUCCION — 2026-08-27
-- #
-- #  Cambios que cubre:
-- #    A. Pago con cheque (TSR / CXP)
-- #    B. Caja chica (TSR)
-- #    C. Emision de liquidaciones de compra (CXC -> CXP)
-- #
-- #  Estado: los tres procesos fueron PROBADOS de punta a punta en la
-- #  base local (copia de produccion) el 2026-08-27, con la contabilidad
-- #  verificada asiento por asiento. Ver los manuales en
-- #  docs/logica-negocio/tsr/manuales/.
-- #
-- #  COMO EJECUTARLO
-- #  ---------------
-- #  * SQL puro: sirve en el plugin de VS Code (JDBC) y en SQL*Plus.
-- #    No lleva WHENEVER, SET, DEFINE ni ningun comando de SQL*Plus.
-- #  * Ejecutar POR BLOQUES, de arriba abajo, revisando el SELECT de
-- #    control de cada uno antes de seguir. No lanzarlo entero de una.
-- #  * Los GRANT del bloque B1 requieren conectarse como DBA (o cada uno
-- #    como el owner de la tabla). El resto va con el usuario de la
-- #    aplicacion.
-- #  * VA ANTES DE DESPLEGAR EL WAR. El codigo nuevo asume estas columnas.
-- #
-- #  DOS TRAMPAS DE ORACLE QUE YA COSTARON TIEMPO AQUI
-- #  --------------------------------------------------
-- #  1. Una FK hacia otro schema exige GRANT REFERENCES directo: Oracle
-- #     NO considera los privilegios heredados por ROL al crear un
-- #     constraint, ni con rol DBA. Por eso los GRANT van primero.
-- #  2. CREATE INDEX sin prefijo de schema deja el indice en el schema de
-- #     la SESION, no en el de la tabla. Todos van prefijados. Y para
-- #     verificarlos hay que filtrar por TABLE_OWNER, no por OWNER.
-- #
-- #  SI ALGO FALLA A MITAD
-- #  ---------------------
-- #  Cada bloque termina en COMMIT. Un bloque que falle se puede repetir
-- #  tras corregir la causa: los DDL no son idempotentes, asi que si un
-- #  ALTER o CREATE ya paso, saltarlo en el reintento.
-- #####################################################################


-- #####################################################################
-- #  PARTE A — PAGO CON CHEQUE
-- #####################################################################

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


-- --- A.2: la FK del beneficiario del cheque apunta a la tabla equivocada ---

--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 1: control previo
--   (a) FKs actuales de DTCH: debe verse FK_DTCH_PRSN -> TSR.PRSN
--   (b) filas de DTCH con beneficiario grabado: debe ser 0
-- ---------------------------------------------------------------------
SELECT C.CONSTRAINT_NAME, C.R_OWNER || '.' || RC.TABLE_NAME AS TABLA_REFERENCIADA, CC.COLUMN_NAME
  FROM ALL_CONSTRAINTS C
  JOIN ALL_CONS_COLUMNS CC ON CC.CONSTRAINT_NAME = C.CONSTRAINT_NAME AND CC.OWNER = C.OWNER
  JOIN ALL_CONSTRAINTS RC ON RC.CONSTRAINT_NAME = C.R_CONSTRAINT_NAME AND RC.OWNER = C.R_OWNER
 WHERE C.OWNER = 'TSR' AND C.TABLE_NAME = 'DTCH' AND C.CONSTRAINT_TYPE = 'R'
 ORDER BY C.CONSTRAINT_NAME;

SELECT COUNT(*) AS CHEQUES_CON_BENEFICIARIO
  FROM TSR.DTCH
 WHERE PRSNCDGO IS NOT NULL OR DTCHIDBN IS NOT NULL;

-- Si el conteo anterior NO es 0, detenerse: hay que revisar esos cheques
-- antes de repuntar la FK (sus codigos podrian existir en PRSN y no en TTLR).

-- ---------------------------------------------------------------------
-- BLOQUE 2: repuntar la FK del beneficiario al Titular vigente
-- ---------------------------------------------------------------------
ALTER TABLE TSR.DTCH DROP CONSTRAINT FK_DTCH_PRSN;

ALTER TABLE TSR.DTCH ADD CONSTRAINT FK_DTCH_TTLR
    FOREIGN KEY (PRSNCDGO) REFERENCES TSR.TTLR(TTLRCDGO);

COMMENT ON COLUMN TSR.DTCH.PRSNCDGO IS 'Titular beneficiario del cheque (TSR.TTLR). El nombre de la columna es herencia del modelo antiguo, cuando apuntaba a TSR.PRSN';

-- ---------------------------------------------------------------------
-- BLOQUE 3: FK que faltaba para el beneficiario alterno
-- ---------------------------------------------------------------------
ALTER TABLE TSR.DTCH ADD CONSTRAINT FK_DTCH_IDBN
    FOREIGN KEY (DTCHIDBN) REFERENCES TSR.TTLR(TTLRCDGO);

CREATE INDEX TSR.IDX_DTCH_PRSN ON TSR.DTCH(PRSNCDGO);
CREATE INDEX TSR.IDX_DTCH_IDBN ON TSR.DTCH(DTCHIDBN);

-- ---------------------------------------------------------------------
-- BLOQUE 4: control final — las tres FKs deben apuntar a TTLR, CHQR y ASNT
-- ---------------------------------------------------------------------
SELECT C.CONSTRAINT_NAME, C.R_OWNER || '.' || RC.TABLE_NAME AS TABLA_REFERENCIADA,
       CC.COLUMN_NAME, C.STATUS, C.VALIDATED
  FROM ALL_CONSTRAINTS C
  JOIN ALL_CONS_COLUMNS CC ON CC.CONSTRAINT_NAME = C.CONSTRAINT_NAME AND CC.OWNER = C.OWNER
  JOIN ALL_CONSTRAINTS RC ON RC.CONSTRAINT_NAME = C.R_CONSTRAINT_NAME AND RC.OWNER = C.R_OWNER
 WHERE C.OWNER = 'TSR' AND C.TABLE_NAME = 'DTCH' AND C.CONSTRAINT_TYPE = 'R'
 ORDER BY C.CONSTRAINT_NAME;


-- #####################################################################
-- #  PARTE B — CAJA CHICA
-- #####################################################################

--   asi que este camino nunca se ejercito. La devolucion de aportes de
--   CRD (el otro origen externo) fallaria igual en su primer uso real.
--
--   Verificado: 0 filas con PGTRTTLR nulo, asi que relajar la restriccion
--   no toca ningun dato existente y no rompe los pagos con titular.
--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 1: control previo
--   (a) PGTRTTLR debe figurar como NOT NULL ('N')
--   (b) no debe haber ninguna fila con titular nulo (deberia dar 0)
-- ---------------------------------------------------------------------
SELECT COLUMN_NAME, NULLABLE
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'PGS' AND TABLE_NAME = 'PGTR' AND COLUMN_NAME = 'PGTRTTLR';

SELECT COUNT(*) AS PAGOS_SIN_TITULAR FROM PGS.PGTR WHERE PGTRTTLR IS NULL;

-- ---------------------------------------------------------------------
-- BLOQUE 2: relajar la restriccion
--   La FK a TSR.TTLR se mantiene: si viene titular, tiene que existir.
-- ---------------------------------------------------------------------
ALTER TABLE PGS.PGTR MODIFY (PGTRTTLR NULL);

COMMENT ON COLUMN PGS.PGTR.PGTRTTLR IS 'Titular beneficiario del pago. Opcional: los pagos de origen externo (caja chica) pueden no tener un tercero detras; en ese caso el beneficiario va en PGTRBFNM';

-- ---------------------------------------------------------------------
-- BLOQUE 3: control final — NULLABLE debe pasar a 'Y', y la FK seguir viva
-- ---------------------------------------------------------------------
SELECT COLUMN_NAME, NULLABLE
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'PGS' AND TABLE_NAME = 'PGTR' AND COLUMN_NAME = 'PGTRTTLR';

SELECT CONSTRAINT_NAME, STATUS, VALIDATED
  FROM ALL_CONSTRAINTS
 WHERE OWNER = 'PGS' AND TABLE_NAME = 'PGTR' AND CONSTRAINT_TYPE = 'R'
   AND CONSTRAINT_NAME LIKE '%TTLR%';


-- ---------------------------------------------------------------------
-- ---------------------------------------------------------------------
-- BLOQUE 0: privilegios REFERENCES para las FK cross-schema
--   Oracle NO considera los privilegios heredados por ROL al crear un
--   constraint: hace falta el GRANT REFERENCES directo, aunque TSR tenga
--   rol DBA. SCP.PJRQ, CNT.PLNN y CNT.ASNT ya lo tienen concedido a
--   PUBLIC, por eso esas FK no fallan; PGS.PGTR, PGS.PRDP y RHH.MPLD no.
--   VA PRIMERO: los CREATE TABLE de abajo dependen de estos permisos.
--   Ejecutar conectado como DBA (o cada GRANT como el owner de la tabla).
-- ---------------------------------------------------------------------
GRANT REFERENCES ON PGS.PGTR TO TSR;
GRANT REFERENCES ON PGS.PRDP TO TSR;
GRANT REFERENCES ON RHH.MPLD TO TSR;
-- Control: deben aparecer las tres filas
SELECT GRANTEE, TABLE_SCHEMA, TABLE_NAME, PRIVILEGE FROM ALL_TAB_PRIVS
 WHERE PRIVILEGE = 'REFERENCES' AND GRANTEE = 'TSR';

-- BLOQUE 1: TSR.CJCH — Caja chica (cabecera / fondo)
-- ---------------------------------------------------------------------
CREATE TABLE TSR.CJCH (
    CJCHCDGO NUMBER GENERATED BY DEFAULT AS IDENTITY (START WITH 1 INCREMENT BY 1) NOT NULL,
    PJRQCDGO NUMBER NOT NULL,
    CJCHNMBR VARCHAR2(200) NOT NULL,
    PLNNCDGO NUMBER NOT NULL,
    CJCHMNTO NUMBER(18,2) NOT NULL,
    CJCHMXGS NUMBER(18,2),
    CJCHPRAL NUMBER(5,2) DEFAULT 20,
    CJCHRSPN VARCHAR2(200),
    CJCHUSCS NUMBER,
    CJCHOBSR VARCHAR2(1000),
    CJCHESTD NUMBER DEFAULT 1,
    CJCHFCRG TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CJCHUSAR NUMBER,
    CONSTRAINT PK_CJCH PRIMARY KEY (CJCHCDGO),
    CONSTRAINT FK_CJCH_PJRQ FOREIGN KEY (PJRQCDGO) REFERENCES SCP.PJRQ(PJRQCDGO),
    CONSTRAINT FK_CJCH_PLNN FOREIGN KEY (PLNNCDGO) REFERENCES CNT.PLNN(PLNNCDGO),
    CONSTRAINT FK_CJCH_MPLD FOREIGN KEY (CJCHUSCS) REFERENCES RHH.MPLD(MPLDCDGO)
);
CREATE INDEX TSR.IDX_CJCH_PJRQ ON TSR.CJCH(PJRQCDGO);
CREATE INDEX TSR.IDX_CJCH_PLNN ON TSR.CJCH(PLNNCDGO);
COMMENT ON TABLE TSR.CJCH IS 'Caja chica: fondo fijo con limite, cuenta contable propia y reposicion';
COMMENT ON COLUMN TSR.CJCH.CJCHCDGO IS 'PK';
COMMENT ON COLUMN TSR.CJCH.PJRQCDGO IS 'Empresa (SCP.PJRQ)';
COMMENT ON COLUMN TSR.CJCH.CJCHNMBR IS 'Nombre de la caja chica';
COMMENT ON COLUMN TSR.CJCH.PLNNCDGO IS 'Cuenta contable de la caja (CNT.PLNN)';
COMMENT ON COLUMN TSR.CJCH.CJCHMNTO IS 'Monto del fondo fijo (limite de la caja)';
COMMENT ON COLUMN TSR.CJCH.CJCHMXGS IS 'Monto maximo permitido por gasto individual; NULL = sin tope';
COMMENT ON COLUMN TSR.CJCH.CJCHPRAL IS 'Porcentaje del fondo bajo el cual se alerta que hay que reponer (default 20)';
COMMENT ON COLUMN TSR.CJCH.CJCHRSPN IS 'Nombre del responsable o custodio';
COMMENT ON COLUMN TSR.CJCH.CJCHUSCS IS 'Colaborador custodio de la caja (RHH.MPLD), opcional';
COMMENT ON COLUMN TSR.CJCH.CJCHOBSR IS 'Observaciones';
COMMENT ON COLUMN TSR.CJCH.CJCHESTD IS 'Estado: 1=Activa, 2=Inactiva';
COMMENT ON COLUMN TSR.CJCH.CJCHFCRG IS 'Fecha de registro';
COMMENT ON COLUMN TSR.CJCH.CJCHUSAR IS 'Usuario que registra';

-- ---------------------------------------------------------------------
-- BLOQUE 2: TSR.CRCH — Cierre / arqueo de caja chica
--   (se crea antes que MVCH porque MVCH la referencia)
-- ---------------------------------------------------------------------
CREATE TABLE TSR.CRCH (
    CRCHCDGO NUMBER GENERATED BY DEFAULT AS IDENTITY (START WITH 1 INCREMENT BY 1) NOT NULL,
    CJCHCDGO NUMBER NOT NULL,
    CRCHFCHA DATE NOT NULL,
    CRCHFCIN DATE,
    CRCHFCFN DATE,
    CRCHSLIN NUMBER(18,2) DEFAULT 0,
    CRCHTGST NUMBER(18,2) DEFAULT 0,
    CRCHTRPS NUMBER(18,2) DEFAULT 0,
    CRCHTAJS NUMBER(18,2) DEFAULT 0,
    CRCHSLDO NUMBER(18,2) DEFAULT 0,
    CRCHSLFS NUMBER(18,2),
    CRCHDFRN NUMBER(18,2),
    CRCHOBSR VARCHAR2(2000),
    CRCHESTD NUMBER DEFAULT 1,
    ASNTCDGO NUMBER,
    CRCHFCRG TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CRCHUSAR NUMBER,
    CONSTRAINT PK_CRCH PRIMARY KEY (CRCHCDGO),
    CONSTRAINT FK_CRCH_CJCH FOREIGN KEY (CJCHCDGO) REFERENCES TSR.CJCH(CJCHCDGO),
    CONSTRAINT FK_CRCH_ASNT FOREIGN KEY (ASNTCDGO) REFERENCES CNT.ASNT(ASNTCDGO)
);
CREATE INDEX TSR.IDX_CRCH_CJCH ON TSR.CRCH(CJCHCDGO);
COMMENT ON TABLE TSR.CRCH IS 'Cierre / arqueo de caja chica: saldo libros vs saldo fisico';
COMMENT ON COLUMN TSR.CRCH.CRCHCDGO IS 'PK';
COMMENT ON COLUMN TSR.CRCH.CJCHCDGO IS 'Caja chica (TSR.CJCH)';
COMMENT ON COLUMN TSR.CRCH.CRCHFCHA IS 'Fecha del cierre';
COMMENT ON COLUMN TSR.CRCH.CRCHFCIN IS 'Inicio del periodo cerrado';
COMMENT ON COLUMN TSR.CRCH.CRCHFCFN IS 'Fin del periodo cerrado';
COMMENT ON COLUMN TSR.CRCH.CRCHSLIN IS 'Saldo inicial del periodo';
COMMENT ON COLUMN TSR.CRCH.CRCHTGST IS 'Total gastos del periodo';
COMMENT ON COLUMN TSR.CRCH.CRCHTRPS IS 'Total reposiciones y aperturas del periodo';
COMMENT ON COLUMN TSR.CRCH.CRCHTAJS IS 'Total ajustes del periodo (positivos menos negativos)';
COMMENT ON COLUMN TSR.CRCH.CRCHSLDO IS 'Saldo segun libros al cierre';
COMMENT ON COLUMN TSR.CRCH.CRCHSLFS IS 'Saldo fisico contado';
COMMENT ON COLUMN TSR.CRCH.CRCHDFRN IS 'Diferencia = saldo fisico - saldo libros';
COMMENT ON COLUMN TSR.CRCH.CRCHOBSR IS 'Observaciones del arqueo';
COMMENT ON COLUMN TSR.CRCH.CRCHESTD IS 'Estado (rubro 233): 1=Borrador, 2=Cerrado, 3=Anulado';
COMMENT ON COLUMN TSR.CRCH.ASNTCDGO IS 'Asiento de ajuste por diferencia (CNT.ASNT), opcional';
COMMENT ON COLUMN TSR.CRCH.CRCHFCRG IS 'Fecha de registro';
COMMENT ON COLUMN TSR.CRCH.CRCHUSAR IS 'Usuario que registra';

-- ---------------------------------------------------------------------
-- ---------------------------------------------------------------------
-- BLOQUE 3: TSR.MVCH — Movimiento de caja chica
-- ---------------------------------------------------------------------
CREATE TABLE TSR.MVCH (
    MVCHCDGO NUMBER GENERATED BY DEFAULT AS IDENTITY (START WITH 1 INCREMENT BY 1) NOT NULL,
    CJCHCDGO NUMBER NOT NULL,
    MVCHTPOO NUMBER NOT NULL,
    MVCHFCHA DATE NOT NULL,
    MVCHVLOR NUMBER(18,2) NOT NULL,
    MVCHDSCR VARCHAR2(500) NOT NULL,
    MVCHOBSR VARCHAR2(2000),
    MVCHPRDP NUMBER,
    TTLRCDGO NUMBER,
    MVCHNDOC VARCHAR2(50),
    ASNTCDGO NUMBER,
    PGTRCDGO NUMBER,
    CRCHCDGO NUMBER,
    MVCHESTD NUMBER DEFAULT 1,
    MVCHMTAN VARCHAR2(500),
    MVCHFCRG TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    MVCHUSAR NUMBER,
    CONSTRAINT PK_MVCH PRIMARY KEY (MVCHCDGO),
    CONSTRAINT FK_MVCH_CJCH FOREIGN KEY (CJCHCDGO) REFERENCES TSR.CJCH(CJCHCDGO),
    CONSTRAINT FK_MVCH_PRDP FOREIGN KEY (MVCHPRDP) REFERENCES PGS.PRDP(ID),
    CONSTRAINT FK_MVCH_TTLR FOREIGN KEY (TTLRCDGO) REFERENCES TSR.TTLR(TTLRCDGO),
    CONSTRAINT FK_MVCH_ASNT FOREIGN KEY (ASNTCDGO) REFERENCES CNT.ASNT(ASNTCDGO),
    CONSTRAINT FK_MVCH_PGTR FOREIGN KEY (PGTRCDGO) REFERENCES PGS.PGTR(PGTRCDGO),
    CONSTRAINT FK_MVCH_CRCH FOREIGN KEY (CRCHCDGO) REFERENCES TSR.CRCH(CRCHCDGO),
    CONSTRAINT CK_MVCH_TPOO CHECK (MVCHTPOO IN (1,2,3,4,5)),
    CONSTRAINT CK_MVCH_VLOR CHECK (MVCHVLOR > 0)
);
CREATE INDEX TSR.IDX_MVCH_CJCH ON TSR.MVCH(CJCHCDGO);
CREATE INDEX TSR.IDX_MVCH_FCHA ON TSR.MVCH(CJCHCDGO, MVCHFCHA);
CREATE INDEX TSR.IDX_MVCH_CRCH ON TSR.MVCH(CRCHCDGO);
CREATE INDEX TSR.IDX_MVCH_PGTR ON TSR.MVCH(PGTRCDGO);
COMMENT ON TABLE TSR.MVCH IS 'Movimientos de caja chica: apertura, gastos, reposiciones y ajustes';
COMMENT ON COLUMN TSR.MVCH.MVCHCDGO IS 'PK';
COMMENT ON COLUMN TSR.MVCH.CJCHCDGO IS 'Caja chica (TSR.CJCH)';
COMMENT ON COLUMN TSR.MVCH.MVCHTPOO IS 'Tipo (rubro 232): 1=Apertura, 2=Gasto, 3=Reposicion, 4=Ajuste positivo, 5=Ajuste negativo';
COMMENT ON COLUMN TSR.MVCH.MVCHFCHA IS 'Fecha del movimiento';
COMMENT ON COLUMN TSR.MVCH.MVCHVLOR IS 'Valor positivo; el tipo determina si suma o resta al saldo';
COMMENT ON COLUMN TSR.MVCH.MVCHDSCR IS 'Concepto';
COMMENT ON COLUMN TSR.MVCH.MVCHOBSR IS 'Observacion; obligatoria en gastos (se valida en el servicio)';
COMMENT ON COLUMN TSR.MVCH.MVCHPRDP IS 'Producto de pago (PGS.PRDP) que clasifica el gasto y da la cuenta contable via su grupo';
COMMENT ON COLUMN TSR.MVCH.TTLRCDGO IS 'Beneficiario o proveedor (TSR.TTLR), opcional';
COMMENT ON COLUMN TSR.MVCH.MVCHNDOC IS 'Numero del comprobante pagado (factura, recibo, vale)';
COMMENT ON COLUMN TSR.MVCH.ASNTCDGO IS 'Asiento contable generado (CNT.ASNT)';
COMMENT ON COLUMN TSR.MVCH.PGTRCDGO IS 'Pago programado (PGS.PGTR) con el que se pago la apertura o reposicion desde el banco';
COMMENT ON COLUMN TSR.MVCH.CRCHCDGO IS 'Cierre de caja chica (TSR.CRCH) en el que quedo incluido';
COMMENT ON COLUMN TSR.MVCH.MVCHESTD IS 'Estado: 1=Activo, 2=Anulado';
COMMENT ON COLUMN TSR.MVCH.MVCHMTAN IS 'Motivo de anulacion';
COMMENT ON COLUMN TSR.MVCH.MVCHFCRG IS 'Fecha de registro';
COMMENT ON COLUMN TSR.MVCH.MVCHUSAR IS 'Usuario que registra';

-- ---------------------------------------------------------------------
-- BLOQUE 4: TSR.PTCH — Documento digitalizado del movimiento
-- ---------------------------------------------------------------------
CREATE TABLE TSR.PTCH (
    PTCHCDGO NUMBER GENERATED BY DEFAULT AS IDENTITY (START WITH 1 INCREMENT BY 1) NOT NULL,
    MVCHCDGO NUMBER NOT NULL,
    PTCHPATH VARCHAR2(1000) NOT NULL,
    PTCHNMDC VARCHAR2(500),
    PTCHTPDC VARCHAR2(50),
    PTCHFCRG TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PTCHUSAR NUMBER,
    CONSTRAINT PK_PTCH PRIMARY KEY (PTCHCDGO),
    CONSTRAINT FK_PTCH_MVCH FOREIGN KEY (MVCHCDGO) REFERENCES TSR.MVCH(MVCHCDGO)
);
CREATE INDEX TSR.IDX_PTCH_MVCH ON TSR.PTCH(MVCHCDGO);
COMMENT ON TABLE TSR.PTCH IS 'Archivos digitalizados (comprobantes) de movimientos de caja chica';
COMMENT ON COLUMN TSR.PTCH.PTCHCDGO IS 'PK';
COMMENT ON COLUMN TSR.PTCH.MVCHCDGO IS 'Movimiento de caja chica (TSR.MVCH)';
COMMENT ON COLUMN TSR.PTCH.PTCHPATH IS 'Ruta del archivo devuelta por FileService';
COMMENT ON COLUMN TSR.PTCH.PTCHNMDC IS 'Nombre original del documento';
COMMENT ON COLUMN TSR.PTCH.PTCHTPDC IS 'Tipo de documento: FACTURA, RECIBO, VALE, OTRO';
COMMENT ON COLUMN TSR.PTCH.PTCHFCRG IS 'Fecha de registro';
COMMENT ON COLUMN TSR.PTCH.PTCHUSAR IS 'Usuario que registra';

-- Control: 4 tablas creadas
SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER = 'TSR' AND TABLE_NAME IN ('CJCH','MVCH','CRCH','PTCH') ORDER BY 1;

-- ---------------------------------------------------------------------
-- BLOQUE 5: rubros 232 y 233
-- ---------------------------------------------------------------------
-- Control previo: 232/233 libres en PRBRCDGO y PRBRALTR; PDTRCDGO >= 1118 libre
SELECT MAX(PRBRCDGO) AS MAX_CDGO, MAX(PRBRALTR) AS MAX_ALTR FROM SCP.PRBR;
SELECT MAX(PDTRCDGO) FROM SCP.PDTR;
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (232, 'TSR - Tipo de movimiento de caja chica (MVCH)', SYSDATE, 232, 0);
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD) VALUES (1118, 232, 'APERTURA: fondo inicial entregado a la caja', 1, 'APERTURA', 1, 1);
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD) VALUES (1119, 232, 'GASTO: pago realizado con dinero de la caja', 2, 'GASTO', 2, 1);
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD) VALUES (1120, 232, 'REPOSICION: reembolso del fondo desde una cuenta bancaria', 3, 'REPOSICION', 3, 1);
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD) VALUES (1121, 232, 'AJUSTE POSITIVO: sobrante detectado en arqueo', 4, 'AJUSTE_POSITIVO', 4, 1);
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD) VALUES (1122, 232, 'AJUSTE NEGATIVO: faltante detectado en arqueo', 5, 'AJUSTE_NEGATIVO', 5, 1);
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (233, 'TSR - Estado del cierre de caja chica (CRCH)', SYSDATE, 233, 0);
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD) VALUES (1123, 233, 'BORRADOR: arqueo en preparacion', 1, 'BORRADOR', 1, 1);
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD) VALUES (1124, 233, 'CERRADO: arqueo confirmado, movimientos bloqueados', 2, 'CERRADO', 2, 1);
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD) VALUES (1125, 233, 'ANULADO: arqueo anulado', 3, 'ANULADO', 3, 1);
-- Control
SELECT P.PRBRCDGO, P.PRBRDSCR, D.PDTRALTR, D.PDTRVLRV FROM SCP.PRBR P JOIN SCP.PDTR D ON D.PRBRCDGO = P.PRBRCDGO WHERE P.PRBRCDGO IN (232, 233) ORDER BY 1, 3;
COMMIT;

-- ---------------------------------------------------------------------
-- BLOQUE 6 (NO EJECUTAR TODAVIA): retiro de las cuentas bancarias "CAJA CHICA"
--   Hoy la caja chica vive como bancos TSR.BNCO 425/427 y cuentas TSR.CNBC
--   428 (plan cuenta 10029) y 429 (plan cuenta 10033). Se ejecuta SOLO
--   despues de crear las cajas chicas en la pantalla nueva con su saldo
--   inicial migrado (movimiento tipo 1 APERTURA sin asiento) y de verificar
--   que no queda ningun pago pendiente sobre esas cuentas.
-- ---------------------------------------------------------------------
-- Control: pagos programados no confirmados/anulados sobre esas cuentas (debe ser 0)
SELECT PGTRCDGO, PGTRESTD, PGTRVLOR FROM PGS.PGTR WHERE PGTRCNBC IN (428, 429) AND PGTRESTD IN (1, 2);
-- Control: cuentas y su plan de cuenta (la caja chica nueva debe usar el mismo PLNNCDGO)
SELECT CNBCCDGO, CNBCNMRO, PLNNCDGO, CNBCESTD FROM TSR.CNBC WHERE CNBCCDGO IN (428, 429);
-- UPDATE TSR.CNBC SET CNBCESTD = 2, CNBCFCDS = SYSDATE WHERE CNBCCDGO IN (428, 429);
-- COMMIT;


-- #####################################################################
-- #  PARTE C — LIQUIDACIONES DE COMPRA EMITIDAS
-- #####################################################################


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

-- =====================================================================================
-- DDL — DEVOLUCIÓN DE APORTES A PARTÍCIPES
-- Implementa la sección 4 de docs/logica-negocio/crd/PLAN-DEVOLUCION-APORTES.md
-- FECHA: 2026-08-24
-- FASE: 0 (Cimientos)
--
-- Contenido:
--   1. ALTER PGS.PGTR  — origen externo genérico + beneficiario ocasional + asiento
--   2. PGS.DPGT        — desglose contable del pago de origen externo
--   3. CRD.DVAP        — DevolucionAporte (documento de origen)
--   4. CRD.DDVA        — DetalleDevolucionAporte (por tipo de aporte)
--   5. ALTER CRD.TPAP  — producto de pago por tipo de aporte
--   6. Grants
--   7. Controles posteriores a la ejecución
--
-- EJECUCION MANUAL. Correr el bloque 1-2 como owner del esquema PGS y el bloque 3-5
--    como owner del esquema CRD (o como un usuario con privilegios sobre ambos).
-- El DDL va ANTES del despliegue del WAR: las entidades JPA nuevas ya mapean estas
--    columnas y el arranque falla si no existen.
--
-- NOTA DE DISEÑO (§1 del plan): el sistema se comercializa después SIN el módulo crd.
-- Por eso NINGUNA columna de PGS.* apunta a CRD.*: PGTRORGN es una etiqueta de texto
-- opaca y PGTRIDOR un número sin FK. Y en el otro sentido, DVAPIDPG / DVAPNMAS / TPAPPRDP
-- tampoco llevan FK: si se arranca uno de los dos esquemas no queda rastro de integridad
-- referencial hacia el otro. La consistencia la garantiza el reconciliador, no la base.
-- =====================================================================================


-- =====================================================================================
-- 1. ALTER TABLE: PGS.PGTR (PagoProgramado)
-- DESCRIPCIÓN: El pago programado gana tres capacidades genéricas de CXP:
--              a) ORIGEN EXTERNO: etiqueta + id del documento que originó el pago en
--                 OTRO módulo del sistema. CXP guarda el par y lo devuelve, pero nunca
--                 lo resuelve: para CXP es un dato opaco.
--              b) ASIENTO propio: los otros orígenes (factura, egreso, anticipo) cuelgan
--                 el asiento de su documento; el de origen externo no tiene documento CXP
--                 donde colgarlo, así que se guarda aquí.
--              c) BENEFICIARIO OCASIONAL: pagarle a alguien que NO está en el maestro de
--                 titulares (TSR.TTLR) ni tiene cuenta en TSR.CTBN. Se usa cuando
--                 PGTRCTBN es NULL.
-- =====================================================================================

ALTER TABLE PGS.PGTR ADD (
    PGTRORGN VARCHAR2(30),     -- etiqueta del proceso origen; NULL en los pagos propios de CXP
    PGTRIDOR NUMBER,           -- id del documento en el modulo origen. SIN FK, a proposito
    PGTRASNT NUMBER,           -- asiento generado (solo origen externo)
    PGTRBFNM VARCHAR2(2000),   -- beneficiario ocasional: nombre
    PGTRBFID VARCHAR2(20),     -- beneficiario ocasional: identificacion
    PGTRBFBC NUMBER,           -- beneficiario ocasional: banco externo (FK TSR.BEXT)
    PGTRBFTP NUMBER,           -- beneficiario ocasional: tipo de cuenta
    PGTRBFCT VARCHAR2(50)      -- beneficiario ocasional: numero de cuenta
);

-- FK verificada contra la entidad com.saa.model.cnt.Asiento
-- (@Table(name="ASNT", schema="CNT"), PK @Column(name="ASNTCDGO")).
-- La entidad es la autoridad, no el documento.
ALTER TABLE PGS.PGTR ADD CONSTRAINT FK_PGTR_ASNT
    FOREIGN KEY (PGTRASNT) REFERENCES CNT.ASNT(ASNTCDGO);

-- FK verificada contra la entidad com.saa.model.tsr.BancoExterno
-- (@Table(name="BEXT", schema="TSR"), PK @Column(name="BEXTCDGO")).
ALTER TABLE PGS.PGTR ADD CONSTRAINT FK_PGTR_BEXT
    FOREIGN KEY (PGTRBFBC) REFERENCES TSR.BEXT(BEXTCDGO);

CREATE INDEX IDX_PGTR_ORIGEN ON PGS.PGTR(PGTRORGN, PGTRIDOR);

COMMENT ON COLUMN PGS.PGTR.PGTRORGN IS
  'Etiqueta del proceso externo que origino el pago (ej. CRD_DEVOLUCION_APORTE). NULL en los pagos propios de CXP. CXP la guarda y la devuelve, nunca la interpreta.';
COMMENT ON COLUMN PGS.PGTR.PGTRIDOR IS
  'Id del documento en el modulo que origino el pago. SIN FK a proposito: CXP no puede depender de otros modulos.';
COMMENT ON COLUMN PGS.PGTR.PGTRASNT IS
  'Asiento contable generado al confirmarse un pago de origen externo. Los demas origenes cuelgan el asiento de su propio documento.';
COMMENT ON COLUMN PGS.PGTR.PGTRBFNM IS
  'Beneficiario ocasional: nombre. Se usa cuando PGTRCTBN es NULL (el beneficiario no esta en el maestro de titulares).';
COMMENT ON COLUMN PGS.PGTR.PGTRBFID IS
  'Beneficiario ocasional: numero de identificacion (cedula o RUC).';
COMMENT ON COLUMN PGS.PGTR.PGTRBFBC IS
  'Beneficiario ocasional: banco externo al que se transfiere. FK a TSR.BEXT.';
COMMENT ON COLUMN PGS.PGTR.PGTRBFTP IS
  'Beneficiario ocasional: tipo de cuenta (codigoAlterno del DetalleRubro de tipo de cuenta bancaria).';
COMMENT ON COLUMN PGS.PGTR.PGTRBFCT IS
  'Beneficiario ocasional: numero de cuenta destino.';


-- =====================================================================================
-- 2. TABLA: PGS.DPGT (DetallePagoOrigenExterno)
-- DESCRIPCIÓN: Desglose contable de un pago de origen externo, como pares
--              (producto de pago, valor). Al confirmarse el pago, CXP arma el asiento con
--              UNA línea DEBE por producto (cuenta del GrupoProductoPago.planCuenta) y
--              UNA línea HABER a la cuenta contable del banco por el total.
--              Existe porque un solo documento origen puede cubrir varios conceptos, cada
--              uno con su cuenta contable, sin necesidad de emitir N transferencias.
-- =====================================================================================

CREATE TABLE PGS.DPGT (
    -- PK Autoincrementable (Oracle 12c+)
    DPGTCDGO NUMBER GENERATED BY DEFAULT AS IDENTITY (START WITH 1 INCREMENT BY 1) NOT NULL,

    -- FK al pago programado
    PGTRCDGO NUMBER NOT NULL,

    -- FK al producto de pago. OJO: la PK de PGS.PRDP se llama ID, no PRDPCDGO
    -- (verificado en com.saa.model.cxp.ProductoPago, @Id @Column(name = "ID")).
    DPGTPRDP NUMBER NOT NULL,

    -- DATOS DE LA LINEA
    DPGTVLRR NUMBER(18,2) NOT NULL,            -- valor imputado a este producto
    DPGTCNCP VARCHAR2(500),                    -- concepto de la linea del asiento

    -- CONSTRAINTS
    CONSTRAINT PK_DPGT PRIMARY KEY (DPGTCDGO),
    CONSTRAINT FK_DPGT_PGTR FOREIGN KEY (PGTRCDGO) REFERENCES PGS.PGTR(PGTRCDGO),
    CONSTRAINT FK_DPGT_PRDP FOREIGN KEY (DPGTPRDP) REFERENCES PGS.PRDP(ID)
);

CREATE INDEX IDX_DPGT_PGTR ON PGS.DPGT(PGTRCDGO);

COMMENT ON TABLE  PGS.DPGT IS
  'Desglose contable de un pago de origen externo: una fila por producto de pago. Genera una linea DEBE del asiento al confirmarse el pago.';
COMMENT ON COLUMN PGS.DPGT.DPGTCDGO IS 'Codigo del detalle (PK autoincremental).';
COMMENT ON COLUMN PGS.DPGT.PGTRCDGO IS 'Pago programado al que pertenece el detalle. FK a PGS.PGTR.';
COMMENT ON COLUMN PGS.DPGT.DPGTPRDP IS 'Producto de pago que clasifica contablemente la linea. FK a PGS.PRDP(ID).';
COMMENT ON COLUMN PGS.DPGT.DPGTVLRR IS 'Valor imputado a este producto. La suma de las lineas debe igualar PGTRVLOR.';
COMMENT ON COLUMN PGS.DPGT.DPGTCNCP IS 'Concepto que se escribe en la descripcion de la linea del asiento.';


-- =====================================================================================
-- 3. TABLA: CRD.DVAP (DevolucionAporte)
-- DESCRIPCIÓN: Documento de origen de la devolución de dinero de los aportes de un
--              partícipe. Al registrarse genera las filas NEGATIVAS de CRD.APRT y dispara
--              una orden de pago en CXP (PGS.PGTR). Cuando el pago queda confirmado, el
--              reconciliador de CRD marca la devolución como PAGADA.
-- =====================================================================================

CREATE TABLE CRD.DVAP (
    -- PK Autoincrementable (Oracle 12c+)
    DVAPCDGO NUMBER GENERATED BY DEFAULT AS IDENTITY (START WITH 1 INCREMENT BY 1) NOT NULL,

    -- FK al partícipe y a su filial
    ENTDCDGO NUMBER NOT NULL,                  -- FK CRD.ENTD, el participe
    FLLLCDGO NUMBER,                           -- FK CRD.FLLL, filial del participe
    CNBPCDGO NUMBER,                           -- FK CRD.CNBP, cuenta del participe destino

    -- DATOS DE LA DEVOLUCION
    DVAPVLRR NUMBER(18,2) NOT NULL,            -- valor total devuelto
    DVAPFCHA DATE NOT NULL,                    -- fecha de negocio de la devolucion
    DVAPMTVO VARCHAR2(2000),                   -- motivo / observacion del usuario
    DVAPESTD NUMBER DEFAULT 1 NOT NULL,        -- 1 REGISTRADA 2 EN_PAGO 3 PAGADA 4 RECHAZADA 5 ANULADA

    -- ENLACE CON LA ORDEN DE PAGO (SIN FK, a proposito)
    DVAPIDPG NUMBER,                           -- PGS.PGTR.PGTRCDGO. SIN FK
    DVAPNMAS NUMBER,                           -- codigo del asiento, copiado al confirmarse. SIN FK
    DVAPFCPG DATE,                             -- fecha en que el pago quedo confirmado
    DVAPIDEM NUMBER,                           -- empresa contable con la que se genero la orden. SIN FK

    -- AUDITORIA
    DVAPUSRG VARCHAR2(50) NOT NULL,
    DVAPFCRG TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- ANULACION
    DVAPUSAN VARCHAR2(50),
    DVAPFCAN TIMESTAMP,
    DVAPMTAN VARCHAR2(500),

    -- CONSTRAINTS
    CONSTRAINT PK_DVAP PRIMARY KEY (DVAPCDGO),
    CONSTRAINT FK_DVAP_ENTD FOREIGN KEY (ENTDCDGO) REFERENCES CRD.ENTD(ENTDCDGO),
    CONSTRAINT FK_DVAP_FLLL FOREIGN KEY (FLLLCDGO) REFERENCES CRD.FLLL(FLLLCDGO),
    CONSTRAINT FK_DVAP_CNBP FOREIGN KEY (CNBPCDGO) REFERENCES CRD.CNBP(CNBPCDGO),
    CONSTRAINT CK_DVAP_ESTD CHECK (DVAPESTD IN (1,2,3,4,5))
);

CREATE INDEX IDX_DVAP_ENTD ON CRD.DVAP(ENTDCDGO);
CREATE INDEX IDX_DVAP_ESTD ON CRD.DVAP(DVAPESTD);
CREATE INDEX IDX_DVAP_PAGO ON CRD.DVAP(DVAPIDPG);

COMMENT ON TABLE  CRD.DVAP IS
  'Devolucion de dinero de los aportes de un participe. Genera los aportes negativos y dispara una orden de pago en CXP.';
COMMENT ON COLUMN CRD.DVAP.DVAPCDGO IS 'Codigo de la devolucion (PK autoincremental).';
COMMENT ON COLUMN CRD.DVAP.ENTDCDGO IS 'Participe al que se devuelve el dinero. FK a CRD.ENTD.';
COMMENT ON COLUMN CRD.DVAP.FLLLCDGO IS 'Filial del participe al momento de la devolucion. FK a CRD.FLLL.';
COMMENT ON COLUMN CRD.DVAP.CNBPCDGO IS 'Cuenta bancaria del participe a la que se transfiere. FK a CRD.CNBP.';
COMMENT ON COLUMN CRD.DVAP.DVAPVLRR IS 'Valor total devuelto. Debe igualar la suma de CRD.DDVA.DDVAVLRR.';
COMMENT ON COLUMN CRD.DVAP.DVAPFCHA IS 'Fecha de negocio de la devolucion. No puede ser futura.';
COMMENT ON COLUMN CRD.DVAP.DVAPMTVO IS 'Motivo u observacion que escribe el usuario.';
COMMENT ON COLUMN CRD.DVAP.DVAPESTD IS
  'Estado: 1 REGISTRADA, 2 EN_PAGO, 3 PAGADA, 4 RECHAZADA, 5 ANULADA. Ver com.saa.rubros.EstadoDevolucionAporte.';
COMMENT ON COLUMN CRD.DVAP.DVAPIDPG IS
  'Orden de pago generada en CXP (PGS.PGTR.PGTRCDGO). SIN FK a proposito: CRD no ata el esquema PGS.';
COMMENT ON COLUMN CRD.DVAP.DVAPNMAS IS
  'Codigo del asiento contable del pago, copiado por el reconciliador al confirmarse. SIN FK a proposito.';
COMMENT ON COLUMN CRD.DVAP.DVAPFCPG IS 'Fecha en que el banco confirmo el pago (PGTRFRSP).';
COMMENT ON COLUMN CRD.DVAP.DVAPIDEM IS 'Empresa contable con la que se genero la orden de pago. SIN FK a proposito.';
COMMENT ON COLUMN CRD.DVAP.DVAPUSRG IS 'Usuario que registro la devolucion.';
COMMENT ON COLUMN CRD.DVAP.DVAPFCRG IS 'Fecha y hora de registro en el sistema.';
COMMENT ON COLUMN CRD.DVAP.DVAPUSAN IS 'Usuario que anulo la devolucion.';
COMMENT ON COLUMN CRD.DVAP.DVAPFCAN IS 'Fecha y hora de la anulacion.';
COMMENT ON COLUMN CRD.DVAP.DVAPMTAN IS 'Motivo de la anulacion.';


-- =====================================================================================
-- 4. TABLA: CRD.DDVA (DetalleDevolucionAporte)
-- DESCRIPCIÓN: Detalle de la devolución por tipo de aporte. Cada fila deja registrado el
--              aporte NEGATIVO y el PagoAporte que generó, y —si el pago se rechaza o se
--              reversa— el aporte POSITIVO de contra-movimiento.
--              CRD.APRT es append-only para los reportes (G42, G43, G44, CJBM, CPRM/CCPM,
--              dashboard, padron): un reverso NUNCA borra ni edita la fila negativa.
-- =====================================================================================

CREATE TABLE CRD.DDVA (
    -- PK Autoincrementable (Oracle 12c+)
    DDVACDGO NUMBER GENERATED BY DEFAULT AS IDENTITY (START WITH 1 INCREMENT BY 1) NOT NULL,

    -- FKs
    DVAPCDGO NUMBER NOT NULL,                  -- FK CRD.DVAP
    TPAPCDGO NUMBER NOT NULL,                  -- FK CRD.TPAP

    -- DATOS DE LA LINEA
    DDVAVLRR NUMBER(18,2) NOT NULL,            -- valor devuelto de ese tipo

    -- TRAZA DE LO GENERADO (sin FK: son ids de filas append-only de APRT/PGAP)
    DDVAAPRT NUMBER,                           -- CRD.APRT.APRTCDGO de la fila NEGATIVA
    DDVAPGAP NUMBER,                           -- CRD.PGAP.PGAPCDGO generado
    DDVAAPRV NUMBER,                           -- CRD.APRT de la fila POSITIVA de reverso

    -- CONSTRAINTS
    CONSTRAINT PK_DDVA PRIMARY KEY (DDVACDGO),
    CONSTRAINT FK_DDVA_DVAP FOREIGN KEY (DVAPCDGO) REFERENCES CRD.DVAP(DVAPCDGO),
    CONSTRAINT FK_DDVA_TPAP FOREIGN KEY (TPAPCDGO) REFERENCES CRD.TPAP(TPAPCDGO)
);

CREATE INDEX IDX_DDVA_DVAP ON CRD.DDVA(DVAPCDGO);

COMMENT ON TABLE  CRD.DDVA IS
  'Detalle de una devolucion de aportes por tipo de aporte, con la traza de las filas de CRD.APRT y CRD.PGAP generadas.';
COMMENT ON COLUMN CRD.DDVA.DDVACDGO IS 'Codigo del detalle (PK autoincremental).';
COMMENT ON COLUMN CRD.DDVA.DVAPCDGO IS 'Devolucion a la que pertenece el detalle. FK a CRD.DVAP.';
COMMENT ON COLUMN CRD.DDVA.TPAPCDGO IS 'Tipo de aporte devuelto. FK a CRD.TPAP.';
COMMENT ON COLUMN CRD.DDVA.DDVAVLRR IS 'Valor devuelto de este tipo de aporte, en positivo.';
COMMENT ON COLUMN CRD.DDVA.DDVAAPRT IS 'Fila NEGATIVA generada en CRD.APRT al registrar la devolucion.';
COMMENT ON COLUMN CRD.DDVA.DDVAPGAP IS 'Fila generada en CRD.PGAP asociada al aporte negativo.';
COMMENT ON COLUMN CRD.DDVA.DDVAAPRV IS
  'Fila POSITIVA de contra-movimiento en CRD.APRT, generada si el pago se rechaza o se reversa. NULL mientras no ocurra.';


-- =====================================================================================
-- 5. ALTER TABLE: CRD.TPAP (TipoAporte)
-- DESCRIPCIÓN: Mapeo tipo de aporte -> producto de pago de CXP. Es el dato que permite
--              clasificar contablemente la devolución de ese tipo.
--              Es PARAMETRIZACIÓN PREVIA que carga el usuario: sin ella la devolución de
--              ese tipo no se puede registrar (error TIPO_APORTE_SIN_PRODUCTO).
-- =====================================================================================

ALTER TABLE CRD.TPAP ADD (TPAPPRDP NUMBER);

COMMENT ON COLUMN CRD.TPAP.TPAPPRDP IS
  'PGS.PRDP.ID que clasifica contablemente la devolucion de este tipo de aporte. Sin FK: CRD no ata el esquema PGS.';


-- =====================================================================================
-- 6. GRANTS
-- Verificar el nombre real de los roles en la instancia antes de ejecutar
--    (SELECT * FROM DBA_ROLES WHERE ROLE LIKE '%CRD%' OR ROLE LIKE '%PGS%';).
--    Si el esquema no usa roles, omitir este bloque.
-- =====================================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON PGS.DPGT TO ROLE_PGS;
GRANT SELECT, INSERT, UPDATE, DELETE ON CRD.DVAP TO ROLE_CRD;
GRANT SELECT, INSERT, UPDATE, DELETE ON CRD.DDVA TO ROLE_CRD;

-- El proceso de devolucion vive en CRD y necesita escribir la orden de pago en PGS.
GRANT SELECT, INSERT, UPDATE ON PGS.PGTR TO ROLE_CRD;
GRANT SELECT, INSERT, UPDATE ON PGS.DPGT TO ROLE_CRD;


-- =====================================================================================
-- 7. CONTROLES POSTERIORES A LA EJECUCIÓN
-- =====================================================================================

-- 7.1 Las tres tablas nuevas existen y estan vacias
SELECT 'DPGT' AS TABLA, COUNT(*) AS FILAS FROM PGS.DPGT
UNION ALL
SELECT 'DVAP', COUNT(*) FROM CRD.DVAP
UNION ALL
SELECT 'DDVA', COUNT(*) FROM CRD.DDVA;

-- 7.2 Las columnas nuevas de PGS.PGTR existen (deben salir 8 filas)
SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE
FROM   ALL_TAB_COLUMNS
WHERE  OWNER = 'PGS' AND TABLE_NAME = 'PGTR'
AND    COLUMN_NAME IN ('PGTRORGN','PGTRIDOR','PGTRASNT','PGTRBFNM','PGTRBFID','PGTRBFBC','PGTRBFTP','PGTRBFCT')
ORDER BY COLUMN_NAME;

-- 7.3 La columna nueva de CRD.TPAP existe (debe salir 1 fila)
SELECT COLUMN_NAME, DATA_TYPE, NULLABLE
FROM   ALL_TAB_COLUMNS
WHERE  OWNER = 'CRD' AND TABLE_NAME = 'TPAP' AND COLUMN_NAME = 'TPAPPRDP';

-- 7.4 Las constraints y los indices nuevos quedaron habilitados
SELECT CONSTRAINT_NAME, TABLE_NAME, CONSTRAINT_TYPE, STATUS
FROM   ALL_CONSTRAINTS
WHERE  OWNER IN ('PGS','CRD')
AND    CONSTRAINT_NAME IN ('FK_PGTR_ASNT','FK_PGTR_BEXT','PK_DPGT','FK_DPGT_PGTR','FK_DPGT_PRDP',
                           'PK_DVAP','FK_DVAP_ENTD','FK_DVAP_FLLL','FK_DVAP_CNBP','CK_DVAP_ESTD',
                           'PK_DDVA','FK_DDVA_DVAP','FK_DDVA_TPAP')
ORDER BY TABLE_NAME, CONSTRAINT_NAME;

SELECT INDEX_NAME, TABLE_NAME, STATUS
FROM   ALL_INDEXES
WHERE  OWNER IN ('PGS','CRD')
AND    INDEX_NAME IN ('IDX_PGTR_ORIGEN','IDX_DPGT_PGTR','IDX_DVAP_ENTD','IDX_DVAP_ESTD',
                      'IDX_DVAP_PAGO','IDX_DDVA_DVAP')
ORDER BY TABLE_NAME, INDEX_NAME;

-- 7.5 Los pagos existentes de CXP NO quedaron con origen externo (debe devolver 0)
SELECT COUNT(*) AS PAGOS_CON_ORIGEN_INDEBIDO
FROM   PGS.PGTR
WHERE  PGTRORGN IS NOT NULL;

-- 7.6 PARAMETRIZACION PENDIENTE: tipos de aporte vigentes SIN producto de pago.
--     Cada fila que devuelva esta consulta es un tipo cuya devolucion fallara con
--     TIPO_APORTE_SIN_PRODUCTO hasta que el usuario cargue TPAPPRDP.
SELECT TPAPCDGO, TPAPNMBR
FROM   CRD.TPAP
WHERE  TPAPIDST = 1
AND    TPAPPRDP IS NULL
ORDER BY TPAPCDGO;

-- 7.7 Control de integridad "blanda" del enlace CRD -> PGS (sin FK).
--     Devuelve las devoluciones cuyo DVAPIDPG no existe en PGS.PGTR: debe devolver 0 filas.
--     Es la consulta que reemplaza a la FK que deliberadamente NO se creo.
SELECT D.DVAPCDGO, D.DVAPIDPG
FROM   CRD.DVAP D
WHERE  D.DVAPIDPG IS NOT NULL
AND    NOT EXISTS (SELECT 1 FROM PGS.PGTR P WHERE P.PGTRCDGO = D.DVAPIDPG);

-- 7.8 Control de cuadre del desglose contable: la suma de PGS.DPGT debe igualar el valor
--     del pago, con tolerancia de 0.01. Debe devolver 0 filas.
SELECT P.PGTRCDGO, P.PGTRVLOR, SUM(D.DPGTVLRR) AS SUMA_DESGLOSE
FROM   PGS.PGTR P
JOIN   PGS.DPGT D ON D.PGTRCDGO = P.PGTRCDGO
GROUP  BY P.PGTRCDGO, P.PGTRVLOR
HAVING ABS(P.PGTRVLOR - SUM(D.DPGTVLRR)) > 0.01;

-- 7.9 Control de cuadre CRD: la suma de los detalles debe igualar el total de la
--     devolucion, con tolerancia de 0.01. Debe devolver 0 filas.
SELECT V.DVAPCDGO, V.DVAPVLRR, SUM(A.DDVAVLRR) AS SUMA_DETALLE
FROM   CRD.DVAP V
JOIN   CRD.DDVA A ON A.DVAPCDGO = V.DVAPCDGO
GROUP  BY V.DVAPCDGO, V.DVAPVLRR
HAVING ABS(V.DVAPVLRR - SUM(A.DDVAVLRR)) > 0.01;


-- =====================================================================================
-- 8. CONTROLES DE REGULARIZACION CONTABLE
--
-- Agregados el 2026-08-24 por la decision de la seccion 6.5.b del plan: la contabilidad de
-- la devolucion de aportes es OPCIONAL. Sin producto de pago parametrizado
-- (CRD.TPAP.TPAPPRDP) no se manda desglose a CXP, y sin desglose el pago se confirma SIN
-- asiento y SIN movimiento bancario: los dos caen juntos porque
-- creaMovimientoPorTransferencia recibe el Asiento como parametro.
--
-- CONSECUENCIA: esos pagos salieron del banco y no quedan registrados en ningun lado mas
-- que en PGS.PGTR. Son INVISIBLES PARA LA CONCILIACION BANCARIA hasta que se regularicen.
-- Es aceptable en una etapa de revision de pantallas. NO lo es en produccion con dinero
-- real.
--
-- No hace falta ninguna columna nueva para encontrarlos: PGTRASNT IS NULL en un pago
-- CONFIRMADO de origen externo ES la marca.
--
-- Correr estos dos controles periodicamente mientras TPAPPRDP siga sin cargarse.
-- =====================================================================================

-- 8.1 Pagos de origen externo CONFIRMADOS que quedaron sin asiento ni movimiento bancario.
--     Cada fila es plata que salio del banco y no esta en contabilidad.
SELECT P.PGTRCDGO,
       P.PGTRORGN,
       P.PGTRIDOR,
       P.PGTRVLOR,
       P.PGTRFRSP,
       P.PGTRBFNM
FROM   PGS.PGTR P
WHERE  P.PGTRESTD = 3
AND    P.PGTRORGN IS NOT NULL
AND    P.PGTRASNT IS NULL
ORDER  BY P.PGTRFRSP, P.PGTRCDGO;

-- 8.2 Su equivalente del lado de CRD: devoluciones PAGADAS sin asiento.
--     DVAPNMAS nulo en una devolucion PAGADA NO es un error del reconciliador: es
--     exactamente este caso.
SELECT DVAPCDGO,
       DVAPVLRR,
       DVAPFCPG,
       DVAPIDPG
FROM   CRD.DVAP
WHERE  DVAPESTD = 3
AND    DVAPNMAS IS NULL
ORDER  BY DVAPFCPG;

-- 8.3 Total de dinero devuelto que todavia no llego a contabilidad.
--     Es el numero que hay que poder explicar antes de cerrar un periodo.
SELECT COUNT(*)          AS PAGOS_SIN_CONTABILIDAD,
       SUM(P.PGTRVLOR)   AS VALOR_TOTAL
FROM   PGS.PGTR P
WHERE  P.PGTRESTD = 3
AND    P.PGTRORGN IS NOT NULL
AND    P.PGTRASNT IS NULL;

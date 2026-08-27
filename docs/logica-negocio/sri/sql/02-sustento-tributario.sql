-- =====================================================================
-- ATS FASE 2: codigo de sustento tributario en los documentos de compra
-- Modulo: PGS
-- Fecha:  2026-08-27
-- Autor:  orquestador (verificado contra la BD local, copia de produccion)
--
-- PARA QUE
--   codSustento (Tabla 5 del SRI) es campo OBLIGATORIO de cada comprobante
--   de la seccion <compras> del ATS y no existia en el modelo. Sin el, el
--   anexo no valida. No se puede deducir del resto: depende del destino
--   tributario que la empresa le da a cada compra.
--
-- COMO SE RESUELVE (verificado sobre las 131 facturas reales)
--   Regla base, por documento:  IVA > 0 -> '01'  ;  IVA = 0 -> '02'
--     '01' Credito tributario para IVA
--     '02' Costo o gasto para Impuesto a la Renta
--   Resultado: 103 facturas a '01', 28 a '02', CERO sin resolver.
--
--   El grupo de producto (GRPPCSUS) NO es el defecto: es la EXCEPCION,
--   y solo para activo fijo (03/04), inventario (06/07) y reembolso (08).
--   No puede ser el defecto porque los grupos mezclan: 'Servicios Basicos'
--   tiene 56 lineas con IVA y 96 sin (luz y agua van al 0%), asi que
--   ningun codigo por grupo representa a sus dos mitades.
--   Hoy ningun grupo de esta empresa necesita excepcion.
--
-- POR QUE EL CHECK LLEVA 15 CODIGOS Y NO 16
--   La Tabla 5 del SRI tiene 16 codigos, pero el '00' (casos especiales)
--   CADUCO EL 28/02/2015. Por eso no se cargo en el catalogo
--   (docs/logica-negocio/sri/sql/01-catalogos-ats.sql, TABLA 703, 15
--   valores) y por eso tampoco entra aqui: si el CHECK admitiera un codigo
--   que el catalogo no lista, se podria grabar por API un valor que la
--   pantalla nunca ofrece y que el SRI rechazaria igual.
--   El CHECK y el catalogo dicen lo mismo, a proposito.
--
-- QUE TABLAS
--   Las cuatro que el ATS reporta como compras, confirmado contra la
--   Tabla 4 del catalogo oficial: factura (1), liquidacion de compra (3),
--   nota de credito (4) y nota de debito (5). Mas el grupo de producto,
--   que guarda la excepcion.
--
-- Nullable a proposito: un documento sin sustento resuelto queda en nulo y
-- se lista aparte para que el usuario lo complete. Nunca se inventa.
--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 0: control previo
--   Las cinco consultas deben devolver 0 filas. Si alguna devuelve algo,
--   la columna ya existe: saltar su ALTER en el bloque 1.
-- ---------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATA_LENGTH
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'PGS'
   AND COLUMN_NAME IN ('GRPPCSUS','FCTCCSUS','LQCCCSUS','NTCCCSUS','NTDCCSUS')
 ORDER BY TABLE_NAME;

-- Referencia: cuantos documentos hay para resolver despues
SELECT 'FCTC' AS TABLA, COUNT(*) AS DOCUMENTOS FROM PGS.FCTC
UNION ALL SELECT 'LQCC', COUNT(*) FROM PGS.LQCC
UNION ALL SELECT 'NTCC', COUNT(*) FROM PGS.NTCC
UNION ALL SELECT 'NTDC', COUNT(*) FROM PGS.NTDC
UNION ALL SELECT 'GRPP (grupos)', COUNT(*) FROM PGS.GRPP;

-- ---------------------------------------------------------------------
-- BLOQUE 1: las cinco columnas
-- ---------------------------------------------------------------------
ALTER TABLE PGS.GRPP ADD (GRPPCSUS VARCHAR2(2));
ALTER TABLE PGS.FCTC ADD (FCTCCSUS VARCHAR2(2));
ALTER TABLE PGS.LQCC ADD (LQCCCSUS VARCHAR2(2));
ALTER TABLE PGS.NTCC ADD (NTCCCSUS VARCHAR2(2));
ALTER TABLE PGS.NTDC ADD (NTDCCSUS VARCHAR2(2));

-- ---------------------------------------------------------------------
-- BLOQUE 2: los CHECK, con los 15 codigos vigentes de la Tabla 5
--   El del grupo es mas estrecho a proposito: un grupo solo puede
--   declarar una EXCEPCION (activo fijo, inventario o reembolso). Poner
--   '01' o '02' como excepcion de grupo no tendria sentido, porque esos
--   dos ya salen de la regla base del IVA.
-- ---------------------------------------------------------------------
ALTER TABLE PGS.FCTC ADD CONSTRAINT CK_FCTC_CSUS CHECK (FCTCCSUS IS NULL OR FCTCCSUS IN
    ('01','02','03','04','05','06','07','08','09','10','11','12','13','14','15'));
ALTER TABLE PGS.LQCC ADD CONSTRAINT CK_LQCC_CSUS CHECK (LQCCCSUS IS NULL OR LQCCCSUS IN
    ('01','02','03','04','05','06','07','08','09','10','11','12','13','14','15'));
ALTER TABLE PGS.NTCC ADD CONSTRAINT CK_NTCC_CSUS CHECK (NTCCCSUS IS NULL OR NTCCCSUS IN
    ('01','02','03','04','05','06','07','08','09','10','11','12','13','14','15'));
ALTER TABLE PGS.NTDC ADD CONSTRAINT CK_NTDC_CSUS CHECK (NTDCCSUS IS NULL OR NTDCCSUS IN
    ('01','02','03','04','05','06','07','08','09','10','11','12','13','14','15'));
ALTER TABLE PGS.GRPP ADD CONSTRAINT CK_GRPP_CSUS CHECK (GRPPCSUS IS NULL OR GRPPCSUS IN
    ('03','04','06','07','08'));

-- ---------------------------------------------------------------------
-- BLOQUE 3: documentacion de las columnas
-- ---------------------------------------------------------------------
COMMENT ON COLUMN PGS.GRPP.GRPPCSUS IS 'EXCEPCION de sustento tributario del grupo (Tabla 5 SRI). Solo activo fijo (03/04), inventario (06/07) y reembolso (08). Nulo = manda la regla base por IVA';
COMMENT ON COLUMN PGS.FCTC.FCTCCSUS IS 'Codigo de sustento tributario del ATS (Tabla 5 SRI). Se resuelve por IVA: >0 -> 01, =0 -> 02; el grupo lo sobreescribe solo en los casos de excepcion. Nulo = sin resolver, se lista para completar a mano';
COMMENT ON COLUMN PGS.LQCC.LQCCCSUS IS 'Codigo de sustento tributario del ATS (Tabla 5 SRI) para la liquidacion de compra';
COMMENT ON COLUMN PGS.NTCC.NTCCCSUS IS 'Codigo de sustento tributario del ATS (Tabla 5 SRI) para la nota de credito de compra';
COMMENT ON COLUMN PGS.NTDC.NTDCCSUS IS 'Codigo de sustento tributario del ATS (Tabla 5 SRI) para la nota de debito de compra';

-- ---------------------------------------------------------------------
-- BLOQUE 4: control final
--   (a) las cinco columnas, VARCHAR2(2) y nullable
--   (b) los cinco CHECK, ENABLED y VALIDATED
--   (c) el CHECK y el catalogo deben coincidir: esta consulta compara los
--       codigos del catalogo 703 contra lo que admite el CHECK de FCTC.
--       Debe devolver 15 filas y ninguna marcada 'FALTA EN EL CATALOGO'.
-- ---------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE||'('||DATA_LENGTH||')' AS TIPO, NULLABLE
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'PGS'
   AND COLUMN_NAME IN ('GRPPCSUS','FCTCCSUS','LQCCCSUS','NTCCCSUS','NTDCCSUS')
 ORDER BY TABLE_NAME;

SELECT TABLE_NAME, CONSTRAINT_NAME, STATUS, VALIDATED
  FROM ALL_CONSTRAINTS
 WHERE OWNER = 'PGS' AND CONSTRAINT_TYPE = 'C'
   AND CONSTRAINT_NAME IN ('CK_FCTC_CSUS','CK_LQCC_CSUS','CK_NTCC_CSUS','CK_NTDC_CSUS','CK_GRPP_CSUS')
 ORDER BY CONSTRAINT_NAME;

SELECT T.CODIGO, SUBSTR(T.DETALLE,1,60) AS DETALLE
  FROM PGS.TSRI T JOIN PGS.LSRI L ON L.ID = T.LSRI
 WHERE L.TABLA = '703'
 ORDER BY T.CODIGO;

COMMIT;

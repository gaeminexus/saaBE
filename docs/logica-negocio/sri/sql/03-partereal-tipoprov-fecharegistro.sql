-- =====================================================================
-- ATS FASE 3: parte relacionada + tipo de proveedor (en el TITULAR) y
--             fecha de registro contable (en cada documento de compra)
-- Modulos: TSR (titular) + PGS (documentos de compra)
-- Fecha:  2026-08-28
-- Autor:  agente BACKEND -- MODELO PROPUESTO, sin ejecutar. Verificar y
--         correr primero en local; el script de produccion lo escribe
--         y corre el orquestador/usuario, como el resto del proyecto.
--
-- PARA QUE
--   docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md S3.3 y S4.2/S4.3
--   (fase 3): tres campos que faltan en el modelo para que <compras> y
--   <ventas> del ATS puedan armarse completos.
--
--   parteRel  ("SI"/"NO"): si el titular es una PARTE RELACIONADA del
--     contribuyente. Va UNA VEZ POR TITULAR, no por documento -- a
--     diferencia de codSustento (fase 2), que es por comprobante.
--   tipoProv  (Tabla 14 del catalogo SRI: "01" Persona natural,
--     "02" Sociedad): tambien una vez por titular. El catalogo YA esta
--     cargado -- PGS.LSRI.TABLA='706' ("Cat ATS - T14 - Tipo de
--     identificacion del proveedor", 01-catalogos-ats.sql linea 214-216).
--     No es lo mismo que TTLRPRVD (que solo dice "es proveedor si/no").
--   fechaRegistro (fecha de REGISTRO CONTABLE): por documento, en las
--     cuatro tablas de <compras> ya identificadas en la fase 2
--     (FCTC, LQCC, NTCC, NTDC) -- distinta de la fecha de EMISION del
--     comprobante, que cada tabla ya tiene (columna FECHA).
--
-- LO QUE NO SE RESUELVE AQUI
--   Este script solo agrega columnas. NO hay regla automatica para
--   completar parteRel/tipoProv/fechaRegistro -- a diferencia de
--   codSustento (que sí tenia una regla derivable del IVA), estos tres
--   son datos que alguien tiene que capturar a mano (parteRel/tipoProv
--   al dar de alta o editar el titular; fechaRegistro al contabilizar
--   cada documento). Nacen en NULL. No hay backfill: no hay de donde
--   inferir retroactivamente si un proveedor ya existente es parte
--   relacionada o si es persona natural o sociedad.
--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 0: control previo
--   Las dos consultas deben devolver 0 filas. Si alguna devuelve algo,
--   la columna ya existe: saltar su ALTER en el bloque 1/3.
-- ---------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATA_LENGTH
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'TSR' AND TABLE_NAME = 'TTLR'
   AND COLUMN_NAME IN ('TTLRPREL','TTLRTPAT');

SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATA_LENGTH
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'PGS'
   AND COLUMN_NAME IN ('FCTCFCRG','LQCCFCRG','NTCCFCRG','NTDCFCRG');

-- Confirmar que el catalogo de tipoProv (Tabla 14) sigue cargado como se
-- documento en la fase 1 -- debe devolver 2 filas: 01 PERSONA NATURAL,
-- 02 SOCIEDAD.
SELECT T.CODIGO, T.DETALLE FROM PGS.TSRI T JOIN PGS.LSRI L ON L.ID = T.LSRI
 WHERE L.TABLA = '706' ORDER BY T.CODIGO;

-- ---------------------------------------------------------------------
-- BLOQUE 1: TSR.TTLR -- parte relacionada y tipo de proveedor ATS
-- ---------------------------------------------------------------------
ALTER TABLE TSR.TTLR ADD (
    TTLRPREL VARCHAR2(2),
    TTLRTPAT VARCHAR2(2)
);

ALTER TABLE TSR.TTLR ADD CONSTRAINT CK_TTLR_PREL CHECK (TTLRPREL IS NULL OR TTLRPREL IN ('SI','NO'));
ALTER TABLE TSR.TTLR ADD CONSTRAINT CK_TTLR_TPAT CHECK (TTLRTPAT IS NULL OR TTLRTPAT IN ('01','02'));

COMMENT ON COLUMN TSR.TTLR.TTLRPREL IS 'ATS: si el titular es parte relacionada ("SI"/"NO"). Una vez por titular, no por documento. Nulo = sin capturar';
COMMENT ON COLUMN TSR.TTLR.TTLRTPAT IS 'ATS Tabla 14 (PGS.LSRI.TABLA=706): tipo de proveedor -- "01" Persona natural, "02" Sociedad. Distinto de TTLRPRVD (flag "es proveedor"). Nulo = sin capturar';

-- ---------------------------------------------------------------------
-- BLOQUE 2: PGS -- fecha de registro contable en los cuatro documentos
--   de <compras> (mismas cuatro tablas que la fase 2 de codSustento).
--   DATE, nullable, sin CHECK -- es una fecha libre, no un codigo de
--   catalogo.
-- ---------------------------------------------------------------------
ALTER TABLE PGS.FCTC ADD (FCTCFCRG DATE);
ALTER TABLE PGS.LQCC ADD (LQCCFCRG DATE);
ALTER TABLE PGS.NTCC ADD (NTCCFCRG DATE);
ALTER TABLE PGS.NTDC ADD (NTDCFCRG DATE);

COMMENT ON COLUMN PGS.FCTC.FCTCFCRG IS 'ATS: fecha de REGISTRO CONTABLE de la factura de compra, distinta de FECHA (fecha de emision del comprobante). Nula = sin capturar';
COMMENT ON COLUMN PGS.LQCC.LQCCFCRG IS 'ATS: fecha de REGISTRO CONTABLE de la liquidacion de compra, distinta de FECHA (fecha de emision). Nula = sin capturar';
COMMENT ON COLUMN PGS.NTCC.NTCCFCRG IS 'ATS: fecha de REGISTRO CONTABLE de la nota de credito de compra, distinta de FECHA (fecha de emision). Nula = sin capturar';
COMMENT ON COLUMN PGS.NTDC.NTDCFCRG IS 'ATS: fecha de REGISTRO CONTABLE de la nota de debito de compra, distinta de FECHA (fecha de emision). Nula = sin capturar';

-- ---------------------------------------------------------------------
-- BLOQUE 3: control final
-- ---------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE||'('||DATA_LENGTH||')' AS TIPO, NULLABLE
  FROM ALL_TAB_COLUMNS
 WHERE (OWNER = 'TSR' AND TABLE_NAME = 'TTLR' AND COLUMN_NAME IN ('TTLRPREL','TTLRTPAT'))
    OR (OWNER = 'PGS' AND COLUMN_NAME IN ('FCTCFCRG','LQCCFCRG','NTCCFCRG','NTDCFCRG'))
 ORDER BY TABLE_NAME, COLUMN_NAME;

SELECT TABLE_NAME, CONSTRAINT_NAME, STATUS, VALIDATED
  FROM ALL_CONSTRAINTS
 WHERE OWNER = 'TSR' AND CONSTRAINT_TYPE = 'C'
   AND CONSTRAINT_NAME IN ('CK_TTLR_PREL','CK_TTLR_TPAT');

COMMIT;

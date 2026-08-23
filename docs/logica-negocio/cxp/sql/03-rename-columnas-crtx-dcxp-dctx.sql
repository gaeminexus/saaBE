-- ============================================================
-- RENAME COLUMNAS: Nomenclatura estándar SAA
-- Tablas: PGS.DCXP, PGS.CRTX, PGS.DCTX
-- Módulo: CXP - Cuentas por Pagar
-- Fecha:  2026-07-25
--
-- Orden de ejecución: 3 de 5
-- Anterior: 02-alter-periodo-contable-cxp.sql
-- Siguiente: 04-create-tables-negociacion-proveedor.sql
--
-- ESTÁNDAR APLICADO:
--   Nombre columna = [TABLA 4 chars][CAMPO 4 chars descriptivos]
--   Excepción FK única: el campo toma el nombre del PK de la tabla
--   referenciada (ej: FK única a PRDO → PRDOCDGO, a CRTX → CRTXCDGO).
--   Cuando hay múltiples FK a la misma tabla en una entidad, se usa
--   nombre descriptivo (ej: DCXPPJRQ, DCXPUCXM, DCXPURBD, DCXPURVS).
--
-- NOTA: En Oracle 12c+ el RENAME COLUMN conserva automáticamente
--   constraints e índices que referencien la columna renombrada.
--   Verificar vistas y triggers si los hubiera.
-- ============================================================


-- ============================================================
-- 1. TABLA PGS.DCXP  (DocumentoCxp)
-- ============================================================

ALTER TABLE PGS.DCXP RENAME COLUMN ID                        TO DCXPCDGO;
ALTER TABLE PGS.DCXP RENAME COLUMN EMPRESA                   TO DCXPPJRQ;
ALTER TABLE PGS.DCXP RENAME COLUMN RUCEMISOR                 TO DCXPRCEM;
ALTER TABLE PGS.DCXP RENAME COLUMN RAZONSOCIALEMISOR         TO DCXPRSEM;
ALTER TABLE PGS.DCXP RENAME COLUMN TIPOCOMPROBANTE           TO DCXPTPCM;
ALTER TABLE PGS.DCXP RENAME COLUMN SERIECOMPROBANTE          TO DCXPSRCM;
ALTER TABLE PGS.DCXP RENAME COLUMN CLAVEACCESO               TO DCXPCLAC;
ALTER TABLE PGS.DCXP RENAME COLUMN FECHAAUTORIZACION         TO DCXPFAUT;
ALTER TABLE PGS.DCXP RENAME COLUMN FECHAEMISION              TO DCXPFEMS;
ALTER TABLE PGS.DCXP RENAME COLUMN IDENTIFICACIONRECEPTOR    TO DCXPIDRC;
ALTER TABLE PGS.DCXP RENAME COLUMN VALORSINIMPUESTOS         TO DCXPVSIM;
ALTER TABLE PGS.DCXP RENAME COLUMN IVA                       TO DCXPIVAA; -- IVA = 3 chars → última letra repetida hasta completar 4: IVAA
ALTER TABLE PGS.DCXP RENAME COLUMN IMPORTETOTAL              TO DCXPIMTT;
ALTER TABLE PGS.DCXP RENAME COLUMN NUMERODOCUMENTOMODIFICADO TO DCXPNDMD;
ALTER TABLE PGS.DCXP RENAME COLUMN ESTADODOCUMENTO           TO DCXPESTD;
ALTER TABLE PGS.DCXP RENAME COLUMN PATHXML                   TO DCXPPXML;
ALTER TABLE PGS.DCXP RENAME COLUMN FECHACARGAXML             TO DCXPFCXM;
ALTER TABLE PGS.DCXP RENAME COLUMN USUARIOCARGAXML           TO DCXPUCXM;  -- FK a PJRQ (múltiple → descriptivo)
ALTER TABLE PGS.DCXP RENAME COLUMN IDDOCUMENTOBD             TO DCXPIDBD;
ALTER TABLE PGS.DCXP RENAME COLUMN TIPOTABLADESTINO          TO DCXPTBTD;
ALTER TABLE PGS.DCXP RENAME COLUMN FECHAREGISTROBD           TO DCXPFRBD;
ALTER TABLE PGS.DCXP RENAME COLUMN USUARIOREGISTROBD         TO DCXPURBD;  -- FK a PJRQ (múltiple → descriptivo)
ALTER TABLE PGS.DCXP RENAME COLUMN FECHAREVERSION            TO DCXPFRVS;
ALTER TABLE PGS.DCXP RENAME COLUMN USUARIOREVERSION          TO DCXPURVS;  -- FK a PJRQ (múltiple → descriptivo)
ALTER TABLE PGS.DCXP RENAME COLUMN NOVEDAD                   TO DCXPNVDD;
ALTER TABLE PGS.DCXP RENAME COLUMN ESTADONOVEDAD             TO DCXPENOV;
ALTER TABLE PGS.DCXP RENAME COLUMN OBSERVACION               TO DCXPOBSR;
ALTER TABLE PGS.DCXP RENAME COLUMN PERIODOCONTABLE           TO PRDOCDGO;  -- FK única a CNT.PRDO

COMMENT ON TABLE  PGS.DCXP          IS 'Documento único CXP por clave de acceso SRI. Un registro por comprobante recibido, con su ciclo de vida completo.';
COMMENT ON COLUMN PGS.DCXP.DCXPCDGO IS 'PK autoincremental del documento único CXP';
COMMENT ON COLUMN PGS.DCXP.DCXPPJRQ IS 'FK a SCP.PJRQ — empresa receptora. DCXP tiene cuatro referencias a PJRQ; se usa nombre descriptivo.';
COMMENT ON COLUMN PGS.DCXP.DCXPRCEM IS 'RUC del emisor (proveedor)';
COMMENT ON COLUMN PGS.DCXP.DCXPRSEM IS 'Razón social del emisor (proveedor)';
COMMENT ON COLUMN PGS.DCXP.DCXPTPCM IS 'Tipo de comprobante: Factura, Nota de Crédito, Nota de Débito, Liquidación de compra, Comprobante de Retención';
COMMENT ON COLUMN PGS.DCXP.DCXPSRCM IS 'Serie del comprobante (ej: 001-001-000000123)';
COMMENT ON COLUMN PGS.DCXP.DCXPCLAC IS 'Clave de acceso SRI (49 dígitos). Única por empresa.';
COMMENT ON COLUMN PGS.DCXP.DCXPFAUT IS 'Fecha y hora de autorización otorgada por el SRI';
COMMENT ON COLUMN PGS.DCXP.DCXPFEMS IS 'Fecha de emisión del comprobante';
COMMENT ON COLUMN PGS.DCXP.DCXPIDRC IS 'Identificación (RUC/cédula) del receptor tal como aparece en el comprobante';
COMMENT ON COLUMN PGS.DCXP.DCXPVSIM IS 'Valor sin impuestos (subtotal del comprobante)';
COMMENT ON COLUMN PGS.DCXP.DCXPIVAA IS 'Valor total del IVA. (IVA = 3 chars, última letra repetida: IVAA)';
COMMENT ON COLUMN PGS.DCXP.DCXPIMTT IS 'Importe total del comprobante (subtotal + IVA + otros impuestos)';
COMMENT ON COLUMN PGS.DCXP.DCXPNDMD IS 'Número del documento modificado; aplica a Notas de Crédito y Notas de Débito';
COMMENT ON COLUMN PGS.DCXP.DCXPESTD IS 'Estado del documento: 1=LEIDO 2=XML_CARGADO 3=REGISTRADO_BD 4=ERROR 5=NOVEDAD 6=REVERTIDO';
COMMENT ON COLUMN PGS.DCXP.DCXPPXML IS 'Path físico del archivo XML en el servidor';
COMMENT ON COLUMN PGS.DCXP.DCXPFCXM IS 'Fecha y hora en que se cargó el XML al servidor';
COMMENT ON COLUMN PGS.DCXP.DCXPUCXM IS 'FK a SCP.PJRQ — usuario que cargó el XML. DCXP tiene cuatro referencias a PJRQ; se usa nombre descriptivo.';
COMMENT ON COLUMN PGS.DCXP.DCXPIDBD IS 'ID del registro creado en la tabla destino (FacturaCompra, NotaCreditoCompra, etc.)';
COMMENT ON COLUMN PGS.DCXP.DCXPTBTD IS 'Tabla destino: FACTURA_COMPRA | NOTA_CREDITO_COMPRA | NOTA_DEBITO_COMPRA | LIQUIDACION_COMPRA_COMPRA | RETENCION_COMPRA | RETENCION_COMPRA_V2';
COMMENT ON COLUMN PGS.DCXP.DCXPFRBD IS 'Fecha y hora en que se registró en las tablas CXP destino';
COMMENT ON COLUMN PGS.DCXP.DCXPURBD IS 'FK a SCP.PJRQ — usuario que registró en BD. DCXP tiene cuatro referencias a PJRQ; se usa nombre descriptivo.';
COMMENT ON COLUMN PGS.DCXP.DCXPFRVS IS 'Fecha y hora en que se revirtió el documento';
COMMENT ON COLUMN PGS.DCXP.DCXPURVS IS 'FK a SCP.PJRQ — usuario que ejecutó la reversión. DCXP tiene cuatro referencias a PJRQ; se usa nombre descriptivo.';
COMMENT ON COLUMN PGS.DCXP.DCXPNVDD IS 'Descripción de las diferencias detectadas respecto a la carga anterior';
COMMENT ON COLUMN PGS.DCXP.DCXPENOV IS 'Estado de la novedad: 1=PENDIENTE 2=REEMPLAZADO 3=MANTENIDO';
COMMENT ON COLUMN PGS.DCXP.DCXPOBSR IS 'Observaciones internas: errores técnicos, mensajes de proceso, etc.';
COMMENT ON COLUMN PGS.DCXP.PRDOCDGO IS 'FK única a CNT.PRDO — período contable del documento, determinado por su fecha de emisión';


-- ============================================================
-- 2. TABLA PGS.CRTX  (CargaArchivoTxt)
-- ============================================================

ALTER TABLE PGS.CRTX RENAME COLUMN ID                  TO CRTXCDGO;
ALTER TABLE PGS.CRTX RENAME COLUMN EMPRESA             TO CRTXPJRQ;  -- FK a PJRQ (múltiple → descriptivo)
ALTER TABLE PGS.CRTX RENAME COLUMN USUARIO             TO CRTXUSAR;  -- FK a PJRQ (múltiple → descriptivo)
ALTER TABLE PGS.CRTX RENAME COLUMN FECHACARGA          TO CRTXFCGA;
ALTER TABLE PGS.CRTX RENAME COLUMN NOMBREARCHIVO       TO CRTXNARV;
ALTER TABLE PGS.CRTX RENAME COLUMN TOTALREGISTROS      TO CRTXTTLR;
ALTER TABLE PGS.CRTX RENAME COLUMN REGISTROSNUEVOS     TO CRTXRGNV;
ALTER TABLE PGS.CRTX RENAME COLUMN REGISTROSDUPLICADOS TO CRTXRGDP;
ALTER TABLE PGS.CRTX RENAME COLUMN REGISTROSNOVEDAD    TO CRTXRGND;
ALTER TABLE PGS.CRTX RENAME COLUMN ESTADO              TO CRTXESTD;
ALTER TABLE PGS.CRTX RENAME COLUMN OBSERVACION         TO CRTXOBSR;
ALTER TABLE PGS.CRTX RENAME COLUMN PERIODOCONTABLE     TO PRDOCDGO;  -- FK única a CNT.PRDO

COMMENT ON TABLE  PGS.CRTX          IS 'Cabecera de cada archivo TXT de documentos recibidos del SRI cargado al sistema';
COMMENT ON COLUMN PGS.CRTX.CRTXCDGO IS 'PK autoincremental de la carga';
COMMENT ON COLUMN PGS.CRTX.CRTXPJRQ IS 'FK a SCP.PJRQ — empresa que recibe los documentos. CRTX tiene dos referencias a PJRQ; se usa nombre descriptivo.';
COMMENT ON COLUMN PGS.CRTX.CRTXUSAR IS 'FK a SCP.PJRQ — usuario que realizó la carga. CRTX tiene dos referencias a PJRQ; se usa nombre descriptivo.';
COMMENT ON COLUMN PGS.CRTX.CRTXFCGA IS 'Fecha y hora en que se procesó el archivo TXT';
COMMENT ON COLUMN PGS.CRTX.CRTXNARV IS 'Nombre original del archivo TXT (ej: 1793228946001_Recibidos.txt)';
COMMENT ON COLUMN PGS.CRTX.CRTXTTLR IS 'Total de líneas procesadas (excluye encabezado e ignorados)';
COMMENT ON COLUMN PGS.CRTX.CRTXRGNV IS 'Cantidad de documentos nuevos detectados en esta carga';
COMMENT ON COLUMN PGS.CRTX.CRTXRGDP IS 'Cantidad de documentos duplicados (ya existían sin diferencias de valores)';
COMMENT ON COLUMN PGS.CRTX.CRTXRGND IS 'Cantidad de documentos con novedad: diferencias de valores + documentos desaparecidos del período';
COMMENT ON COLUMN PGS.CRTX.CRTXESTD IS 'Estado de la carga: 1=PROCESADO 2=ERROR_PARCIAL';
COMMENT ON COLUMN PGS.CRTX.CRTXOBSR IS 'Observaciones generales del proceso de carga';
COMMENT ON COLUMN PGS.CRTX.PRDOCDGO IS 'FK única a CNT.PRDO — período contable de esta carga, usado para detectar documentos desaparecidos entre cargas del mismo período';


-- ============================================================
-- 3. TABLA PGS.DCTX  (DetalleCargaTxt)
-- ============================================================

ALTER TABLE PGS.DCTX RENAME COLUMN ID                      TO DCTXCDGO;
ALTER TABLE PGS.DCTX RENAME COLUMN CARGATXT                TO CRTXCDGO;  -- FK única a PGS.CRTX
ALTER TABLE PGS.DCTX RENAME COLUMN DOCUMENTO               TO DCXPCDGO;  -- FK única a PGS.DCXP
ALTER TABLE PGS.DCTX RENAME COLUMN VALORSINIMPUESTOS_CARGA TO DCTXVSIM;
ALTER TABLE PGS.DCTX RENAME COLUMN IVA_CARGA               TO DCTXIVAA;  -- IVA = 3 chars → última letra repetida: IVAA
ALTER TABLE PGS.DCTX RENAME COLUMN IMPORTETOTAL_CARGA      TO DCTXIMTT;
ALTER TABLE PGS.DCTX RENAME COLUMN FECHAAUTORIZACION_CARGA TO DCTXFAUT;
ALTER TABLE PGS.DCTX RENAME COLUMN FECHAEMISION_CARGA      TO DCTXFEMS;
ALTER TABLE PGS.DCTX RENAME COLUMN RESULTADO               TO DCTXRSLT;
ALTER TABLE PGS.DCTX RENAME COLUMN OBSERVACION             TO DCTXOBSR;

-- DCTXRSLT era VARCHAR2(20) con valores texto. Ahora almacena el código
-- numérico del Rubro 174 (CXP_RESULTADO_CARGA_TXT).
-- 1=NUEVO  2=DUPLICADO  3=NOVEDAD  4=IGNORADO  5=DESAPARECIDO
ALTER TABLE PGS.DCTX MODIFY (DCTXRSLT NUMBER(2));

COMMENT ON TABLE  PGS.DCTX          IS 'Detalle de líneas por carga TXT — una fila cada vez que un documento aparece en un archivo. Un mismo documento puede tener N filas en esta tabla.';
COMMENT ON COLUMN PGS.DCTX.DCTXCDGO IS 'PK autoincremental de la línea de detalle';
COMMENT ON COLUMN PGS.DCTX.CRTXCDGO IS 'FK única a PGS.CRTX — cabecera de la carga a la que pertenece esta línea';
COMMENT ON COLUMN PGS.DCTX.DCXPCDGO IS 'FK única a PGS.DCXP — documento único al que apunta esta línea';
COMMENT ON COLUMN PGS.DCTX.DCTXVSIM IS 'Snapshot: valor sin impuestos tal como venía en esta carga';
COMMENT ON COLUMN PGS.DCTX.DCTXIVAA IS 'Snapshot: IVA tal como venía en esta carga. (IVA = 3 chars, última letra repetida: IVAA)';
COMMENT ON COLUMN PGS.DCTX.DCTXIMTT IS 'Snapshot: importe total tal como venía en esta carga';
COMMENT ON COLUMN PGS.DCTX.DCTXFAUT IS 'Snapshot: fecha de autorización tal como venía en esta carga';
COMMENT ON COLUMN PGS.DCTX.DCTXFEMS IS 'Snapshot: fecha de emisión tal como venía en esta carga';
COMMENT ON COLUMN PGS.DCTX.DCTXRSLT IS 'Rubro 174 CXP_RESULTADO_CARGA_TXT: 1=NUEVO 2=DUPLICADO 3=NOVEDAD 4=IGNORADO 5=DESAPARECIDO';
COMMENT ON COLUMN PGS.DCTX.DCTXOBSR IS 'Observaciones y detalle adicional del resultado de esta línea';


-- ============================================================
-- Verificación post-rename
-- ============================================================
-- SELECT COLUMN_NAME, DATA_TYPE, NULLABLE
-- FROM ALL_TAB_COLUMNS WHERE TABLE_NAME = 'DCXP' AND OWNER = 'PGS' ORDER BY COLUMN_ID;
--
-- SELECT COLUMN_NAME, DATA_TYPE, NULLABLE
-- FROM ALL_TAB_COLUMNS WHERE TABLE_NAME = 'CRTX' AND OWNER = 'PGS' ORDER BY COLUMN_ID;
--
-- SELECT COLUMN_NAME, DATA_TYPE, NULLABLE
-- FROM ALL_TAB_COLUMNS WHERE TABLE_NAME = 'DCTX' AND OWNER = 'PGS' ORDER BY COLUMN_ID;

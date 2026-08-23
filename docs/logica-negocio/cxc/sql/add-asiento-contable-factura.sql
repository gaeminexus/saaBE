-- =============================================================================
-- SCRIPT: Cambios de base de datos para asiento contable automático en factura
-- Módulo: CXC - Facturación Electrónica + CNT - Contabilidad
-- Fecha:  2026-07-14
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. FACTURADOR (CBR.FCDR): campos ya creados en base de datos
--    (incluidos aquí solo como referencia, NO ejecutar de nuevo)
-- -----------------------------------------------------------------------------
-- ALTER TABLE CBR.FCDR ADD EMPRESA    NUMBER(38,0) NULL;
-- ALTER TABLE CBR.FCDR ADD AMBIENTE   NUMBER(38,0) NULL;
-- ALTER TABLE CBR.FCDR ADD GENERACONTA NUMBER(38,0) NULL;
-- COMMENT ON COLUMN CBR.FCDR.EMPRESA     IS 'Empresa a la que pertenece el facturador';
-- COMMENT ON COLUMN CBR.FCDR.AMBIENTE    IS '1=Pruebas SRI, 2=Produccion SRI';
-- COMMENT ON COLUMN CBR.FCDR.GENERACONTA IS '1=Genera asiento contable al facturar, 0/nulo=No genera';

-- FK al plan de empresa contable (ejecutar si no existe)
ALTER TABLE CBR.FCDR
    ADD CONSTRAINT FK_FCDR_EMPRESA_PJRQ
    FOREIGN KEY (EMPRESA) REFERENCES SCP.PJRQ(PJRQCDGO);

-- -----------------------------------------------------------------------------
-- 2. FACTURA (CBR.FCTR): agregar campo ASIENTO (FK a CNT.ASNT)
-- -----------------------------------------------------------------------------
ALTER TABLE CBR.FCTR ADD (
    ASIENTO NUMBER NULL   -- FK a CNT.ASNT: asiento contable generado al autorizar
);

ALTER TABLE CBR.FCTR
    ADD CONSTRAINT FK_FCTR_ASNT
    FOREIGN KEY (ASIENTO) REFERENCES CNT.ASNT(ASNTCDGO);

COMMENT ON COLUMN CBR.FCTR.ASIENTO IS 'Asiento contable generado al autorizar la factura';

-- -----------------------------------------------------------------------------
-- 3. TIPO DE ASIENTO (CNT.PLNT): asegurar que exista el tipo para Facturas Venta
--    codigoAlterno = 2, sistema = 1
--    (Ejecutar sólo si NO existe ya; ajustar PJRQCDGO con el ID real de la empresa)
-- -----------------------------------------------------------------------------
-- NOTA: Reemplazar :ID_EMPRESA con el código real de la empresa en SCP.PJRQ
/*
INSERT INTO CNT.PLNT (PLNTCDGO, PLNTNMBR, PLNTCDAL, PLNTESTD, PJRQCDGO, PLNTOBSR, PLNTSSTM)
SELECT CNT.SQ_PLNTCDGO.NEXTVAL,
       'FACTURAS VENTA CXC',
       2,       -- codigoAlterno = 2 (TipoAsientos.FACTURAS_VENTA)
       1,       -- estado = 1 (activo)
       :ID_EMPRESA,
       'Asientos generados automaticamente por emision de facturas electronicas',
       1        -- sistema = 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM CNT.PLNT
    WHERE PLNTCDAL = 2
    AND   PJRQCDGO = :ID_EMPRESA
    AND   PLNTSSTM = 1
);
COMMIT;
*/

-- -----------------------------------------------------------------------------
-- 4. PLANTILLA (CNT.PLNS): registrar la plantilla del proceso de factura
--    codigoAlterno = 1001 (reservado para Factura Venta - ajustar según catálogo)
--    sistema = 1  →  es plantilla del sistema (no editable por el usuario)
-- -----------------------------------------------------------------------------
-- NOTA: Este INSERT es referencial. Las cuentas reales se insertan en DetallePlantilla.
--       Reemplazar :ID_EMPRESA con el código real de la empresa.
/*
INSERT INTO CNT.PLNS (PLNSCDGO, PLNSNMBR, PLNSCDAL, PLNSESTD, PJRQCDGO, PLNSOBSR, PLNSSSTM)
SELECT CNT.SQ_PLNSCDGO.NEXTVAL,
       'FACTURA VENTA ELECTRONICA',
       1001,    -- codigoAlterno para este proceso (documentar)
       1,       -- activo
       :ID_EMPRESA,
       'Plantilla automatica para asiento de emision de factura electronica',
       1        -- sistema = 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM CNT.PLNS
    WHERE PLNSCDAL = 1001
    AND   PJRQCDGO = :ID_EMPRESA
    AND   PLNSSSTM = 1
);
COMMIT;
*/

-- -----------------------------------------------------------------------------
-- 5. TSRI (CBR.TSRI): verificar que los registros de IVA tengan cuenta contable
--    La cuenta contable (PLNNCDGO) debe estar configurada en cada tipo de IVA
--    del LSRI tabla = '17'.
--    Verificar con esta consulta:
-- -----------------------------------------------------------------------------
SELECT t.ID,
       t.CODIGO,
       t.DETALLE,
       t.PORCENTAJE,
       t.PLNNCDGO,
       p.PLNNCNTA,
       p.PLNNNMBR
FROM   CBR.TSRI  t
JOIN   CBR.LSRI  l ON l.TABLA = t.LSRI
LEFT JOIN CNT.PLNN p ON p.PLNNCDGO = t.PLNNCDGO
WHERE  l.TABLA = '17'
ORDER BY t.CODIGO;
-- Si PLNNCDGO es NULL para algún tipo de IVA activo, configurar la cuenta
-- con un UPDATE:
-- UPDATE CBR.TSRI SET PLNNCDGO = :ID_CUENTA_IVA WHERE ID = :ID_TSRI;

-- -----------------------------------------------------------------------------
-- 6. GRUPO PRODUCTO (CBR.GRPC): verificar que todos los grupos tengan cuenta
-- -----------------------------------------------------------------------------
SELECT g.GRPCCDGO, g.GRPCNMBR, g.PLNNCDGO, p.PLNNCNTA, p.PLNNNMBR
FROM   CBR.GRPC g
LEFT JOIN CNT.PLNN p ON p.PLNNCDGO = g.PLNNCDGO
WHERE  g.GRPCESTD = 1
ORDER BY g.GRPCNMBR;
-- Si PLNNCDGO es NULL, configurar:
-- UPDATE CBR.GRPC SET PLNNCDGO = :ID_CUENTA WHERE GRPCCDGO = :ID_GRUPO;

-- -----------------------------------------------------------------------------
-- 7. PERSONA CUENTA CONTABLE (TSR.PRCC): verificar que los clientes tengan
--    cuenta contable de tipo factura (PRCCTPOO=1, PRCCCLPR=1)
-- -----------------------------------------------------------------------------
-- Clientes SIN cuenta contable de facturas configurada:
SELECT tt.TTLRCDGO,
       tt.TTLRNMBR,
       tt.TTLRIDNT
FROM   TSR.TTLR tt
JOIN   TSR.PRRL pr ON pr.PRSNCDGO = tt.TTLRCDGO
WHERE  pr.PRRLESTD = 1
AND    NOT EXISTS (
    SELECT 1 FROM TSR.PRCC pcc
    WHERE pcc.PRRLCDGO = pr.PRRLCDGO
    AND   pcc.PRCCTPOO = 1   -- tipo factura
    AND   pcc.PRCCCLPR = 1   -- rol cliente
)
ORDER BY tt.TTLRNMBR;

-- =============================================================================
-- FIN DEL SCRIPT
-- =============================================================================
COMMIT;

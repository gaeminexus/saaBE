-- =====================================================================================
-- CRD.POSG + CRD.PSPR + CRD.PSCT — POLIZAS DE SEGURO DE PRESTAMOS (desgravamen, incendio, prendario)
-- FECHA: 2026-10-02 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- PARA QUE: registrar la factura anual de la aseguradora (y sus notas de debito/credito),
-- prorratear su valor total en las cuotas de los prestamos incluidos y liberarla a pago por CXP.
--
--   CRD.POSG — UN DOCUMENTO DE SEGURO: factura, nota de debito o nota de credito, con su ciclo
--              (listado enviado → documento registrado → distribuido → liberado a pago / anulado).
--   CRD.PSPR — UN PRESTAMO DENTRO DE UN DOCUMENTO: la base enviada (saldo de capital o suma
--              asegurada), el peso y el valor que le toco.
--   CRD.PSCT — UNA CUOTA AFECTADA: el valor anterior y el nuevo del seguro, para poder reversar.
--
-- AUTORIZADAS POR EL USUARIO el 2026-10-02 («arranca con diseño y construcción... completo»).
-- DISENO:   crd/DISENO-POLIZAS-SEGURO-PRESTAMOS.md §5
-- CONTRATO: crd/API-POLIZAS-SEGURO.md
--
-- ⛔ VA ANTES DEL WAR que mapee estas entidades (H58, H75, 246: sin la tabla, Hibernate revienta).
-- SQL PURO. Correr por bloques. SOLO EN EL BACKEND: no se espeja a saaFE.
-- =====================================================================================

-- 0. CONTROLES PREVIOS ------------------------------------------------------------------
-- 0.1 Los tres codigos libres en toda la base. Esperado: 0 filas.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t WHERE t.TABLE_NAME IN ('POSG', 'PSPR', 'PSCT');
-- 0.2 Las secuencias libres. Esperado: 0 filas.
SELECT s.SEQUENCE_OWNER, s.SEQUENCE_NAME FROM ALL_SEQUENCES s
 WHERE s.SEQUENCE_NAME IN ('SQ_POSGCDGO', 'SQ_PSPRCDGO', 'SQ_PSCTCDGO');
-- 0.3 Dependencias. Esperado: 2 filas (PRST, DTPR).
SELECT t.TABLE_NAME FROM ALL_TABLES t WHERE t.OWNER = 'CRD' AND t.TABLE_NAME IN ('PRST', 'DTPR');


-- 1. CRD.POSG — el documento de seguro --------------------------------------------------
CREATE TABLE CRD.POSG (
  POSGCDGO  NUMBER          NOT NULL,   -- PK
  POSGTPSG  NUMBER          NOT NULL,   -- tipo de seguro: 1 DESGRAVAMEN · 2 INCENDIO · 3 PRENDARIO
  POSGCLSE  NUMBER          NOT NULL,   -- clase: 1 FACTURA · 2 NOTA_DEBITO · 3 NOTA_CREDITO
  POSGPADR  NUMBER,                     -- la FACTURA madre (obligatoria para ND/NC, nula para factura)
  POSGESTD  NUMBER          NOT NULL,   -- 1 LISTADO_ENVIADO · 2 DOCUMENTO_REGISTRADO · 3 DISTRIBUIDO · 4 LIBERADO_A_PAGO · 5 ANULADO
  POSGFCCT  DATE            NOT NULL,   -- fecha de corte del listado (la base se tomo a esta fecha)

  -- Documento de la aseguradora (se llenan en el paso 2)
  POSGASGR  VARCHAR2(300),              -- aseguradora o broker
  POSGRUCA  VARCHAR2(20),               -- RUC
  POSGNMPL  VARCHAR2(60),               -- numero de poliza
  POSGNMDC  VARCHAR2(60),               -- numero del documento (factura / ND / NC)
  POSGCLAC  VARCHAR2(60),               -- clave de acceso del SRI (unica)
  POSGFCEM  DATE,                       -- fecha de emision
  POSGFCIN  DATE,                       -- inicio de vigencia
  POSGFCFN  DATE,                       -- fin de vigencia
  POSGTASA  NUMBER(14,8),               -- tasa aplicada por la aseguradora (informativa; el reparto usa el valor total)
  POSGVLTT  NUMBER(18,2),               -- VALOR TOTAL con impuestos (lo que se reparte)
  POSGDCXP  NUMBER,                     -- id del documento en CXP una vez enlazado (sin FK: otro esquema/equipo)
  POSGOBSR  VARCHAR2(1000),

  -- Auditoria por paso
  POSGUSRG  VARCHAR2(50),  POSGFCRG  TIMESTAMP,   -- listado generado
  POSGUSDC  VARCHAR2(50),  POSGFCDC  TIMESTAMP,   -- documento registrado
  POSGUSDS  VARCHAR2(50),  POSGFCDS  TIMESTAMP,   -- distribuido
  POSGUSLB  VARCHAR2(50),  POSGFCLB  TIMESTAMP,   -- liberado a pago
  POSGUSAN  VARCHAR2(50),  POSGFCAN  TIMESTAMP,   -- anulado
  POSGMTAN  VARCHAR2(500)                          -- motivo de anulacion
);
COMMENT ON TABLE  CRD.POSG          IS 'Documento de seguro de prestamos (factura anual, nota de debito de inclusion, nota de credito de exclusion) y su ciclo hasta liberarlo a pago por CXP.';
COMMENT ON COLUMN CRD.POSG.POSGVLTT IS 'Valor TOTAL del documento con impuestos. Es lo que se prorratea en las cuotas (S1-S12 del diseno).';
COMMENT ON COLUMN CRD.POSG.POSGCLAC IS 'Clave de acceso del SRI. Llave del enlace con DocumentoCxp (omen-saa-2). Unica.';


-- 2. CRD.PSPR — un prestamo dentro de un documento ---------------------------------------
CREATE TABLE CRD.PSPR (
  PSPRCDGO  NUMBER          NOT NULL,   -- PK
  POSGCDGO  NUMBER          NOT NULL,   -- FK documento
  PRSTCDGO  NUMBER          NOT NULL,   -- FK prestamo
  PSPRNVDD  NUMBER          NOT NULL,   -- 1 ORIGINAL (listado) · 2 INCLUSION (ND) · 3 EXCLUSION (NC)
  PSPRBASE  NUMBER(18,2)    NOT NULL,   -- base enviada: saldo de capital (desgravamen) o suma asegurada (incendio/prendario)
  PSPRMSCB  NUMBER,                     -- cuotas/meses cubiertos dentro de la vigencia
  PSPRPESO  NUMBER(24,8),               -- peso = base × mesesCubiertos / mesesVigencia
  PSPRVLAS  NUMBER(18,2),               -- valor asignado del documento a este prestamo
  PSPRNCTS  NUMBER                      -- cuotas en que se repartio
);
COMMENT ON TABLE CRD.PSPR IS 'Prestamo incluido en un documento de seguro: la foto de la base enviada a la aseguradora y lo que le toco del valor total.';


-- 3. CRD.PSCT — una cuota afectada ------------------------------------------------------
CREATE TABLE CRD.PSCT (
  PSCTCDGO  NUMBER          NOT NULL,   -- PK
  POSGCDGO  NUMBER          NOT NULL,   -- FK documento
  PSPRCDGO  NUMBER          NOT NULL,   -- FK prestamo dentro del documento
  DTPRCDGO  NUMBER          NOT NULL,   -- FK cuota
  PSCTCMPO  NUMBER          NOT NULL,   -- campo tocado: 1 DTPRDSGR (desgravamen) · 2 DTPRVLSI (incendio/prendario)
  PSCTSICP  NUMBER(18,2),               -- saldo inicial de capital de la cuota usado como peso (DTPRSICP)
  PSCTVLAN  NUMBER(18,2)    NOT NULL,   -- valor del seguro ANTES
  PSCTVLNV  NUMBER(18,2)    NOT NULL,   -- valor del seguro DESPUES
  PSCTFCRV  TIMESTAMP                   -- cuando se reverso (anulacion). Nulo = vigente
);
COMMENT ON TABLE CRD.PSCT IS 'Cuota cuyo seguro escribio un documento de seguro. Guarda el valor anterior para reversar. No se borra.';


-- 4. SECUENCIAS (tablas nuevas, arrancan en 1) -----------------------------------------
CREATE SEQUENCE CRD.SQ_POSGCDGO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE CRD.SQ_PSPRCDGO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE CRD.SQ_PSCTCDGO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;


-- 5. RESTRICCIONES ----------------------------------------------------------------------
ALTER TABLE CRD.POSG ADD CONSTRAINT PK_POSG PRIMARY KEY (POSGCDGO);
ALTER TABLE CRD.PSPR ADD CONSTRAINT PK_PSPR PRIMARY KEY (PSPRCDGO);
ALTER TABLE CRD.PSCT ADD CONSTRAINT PK_PSCT PRIMARY KEY (PSCTCDGO);

ALTER TABLE CRD.POSG ADD CONSTRAINT FK_POSG_PADRE FOREIGN KEY (POSGPADR) REFERENCES CRD.POSG (POSGCDGO);
ALTER TABLE CRD.PSPR ADD CONSTRAINT FK_PSPR_POSG  FOREIGN KEY (POSGCDGO) REFERENCES CRD.POSG (POSGCDGO);
ALTER TABLE CRD.PSPR ADD CONSTRAINT FK_PSPR_PRST  FOREIGN KEY (PRSTCDGO) REFERENCES CRD.PRST (PRSTCDGO);
ALTER TABLE CRD.PSCT ADD CONSTRAINT FK_PSCT_POSG  FOREIGN KEY (POSGCDGO) REFERENCES CRD.POSG (POSGCDGO);
ALTER TABLE CRD.PSCT ADD CONSTRAINT FK_PSCT_PSPR  FOREIGN KEY (PSPRCDGO) REFERENCES CRD.PSPR (PSPRCDGO);
ALTER TABLE CRD.PSCT ADD CONSTRAINT FK_PSCT_DTPR  FOREIGN KEY (DTPRCDGO) REFERENCES CRD.DTPR (DTPRCDGO);

ALTER TABLE CRD.POSG ADD CONSTRAINT CK_POSG_TIPO   CHECK (POSGTPSG IN (1, 2, 3));
ALTER TABLE CRD.POSG ADD CONSTRAINT CK_POSG_CLASE  CHECK (POSGCLSE IN (1, 2, 3));
ALTER TABLE CRD.POSG ADD CONSTRAINT CK_POSG_ESTADO CHECK (POSGESTD IN (1, 2, 3, 4, 5));
ALTER TABLE CRD.POSG ADD CONSTRAINT CK_POSG_MADRE  CHECK ((POSGCLSE = 1 AND POSGPADR IS NULL) OR (POSGCLSE IN (2, 3) AND POSGPADR IS NOT NULL));
ALTER TABLE CRD.POSG ADD CONSTRAINT CK_POSG_VIGENCIA CHECK (POSGFCIN IS NULL OR POSGFCFN IS NULL OR POSGFCFN >= POSGFCIN);
ALTER TABLE CRD.PSPR ADD CONSTRAINT CK_PSPR_NOVEDAD CHECK (PSPRNVDD IN (1, 2, 3));
ALTER TABLE CRD.PSCT ADD CONSTRAINT CK_PSCT_CAMPO  CHECK (PSCTCMPO IN (1, 2));

-- Clave de acceso unica (la red de la carrera; el backend valida antes con mensaje claro).
CREATE UNIQUE INDEX CRD.UX_POSG_CLAVE_ACCESO ON CRD.POSG (UPPER(TRIM(POSGCLAC)));
-- Un prestamo una vez por documento, una cuota una vez por documento.
CREATE UNIQUE INDEX CRD.UX_PSPR_DOC_PRESTAMO ON CRD.PSPR (POSGCDGO, PRSTCDGO);
CREATE UNIQUE INDEX CRD.UX_PSCT_DOC_CUOTA    ON CRD.PSCT (POSGCDGO, DTPRCDGO);
CREATE INDEX CRD.IX_PSPR_PRST ON CRD.PSPR (PRSTCDGO);
CREATE INDEX CRD.IX_PSCT_DTPR ON CRD.PSCT (DTPRCDGO);


-- 6. CONTROLES POSTERIORES --------------------------------------------------------------
-- 6.1 Columnas. Esperado: POSG 29, PSPR 9, PSCT 9.
SELECT c.TABLE_NAME, COUNT(*) AS COLUMNAS FROM ALL_TAB_COLUMNS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME IN ('POSG', 'PSPR', 'PSCT') GROUP BY c.TABLE_NAME ORDER BY 1;
-- 6.2 Secuencias. Esperado: 3 filas, LAST_NUMBER = 1.
SELECT s.SEQUENCE_NAME, s.LAST_NUMBER FROM ALL_SEQUENCES s
 WHERE s.SEQUENCE_OWNER = 'CRD' AND s.SEQUENCE_NAME IN ('SQ_POSGCDGO', 'SQ_PSPRCDGO', 'SQ_PSCTCDGO');
-- 6.3 Restricciones con nombre, todas ENABLED. Esperado: 3 PK, 6 FK, 7 CHECK.
SELECT c.TABLE_NAME, c.CONSTRAINT_TYPE, COUNT(*) AS CUANTAS FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME IN ('POSG', 'PSPR', 'PSCT') AND c.CONSTRAINT_NAME NOT LIKE 'SYS_%'
 GROUP BY c.TABLE_NAME, c.CONSTRAINT_TYPE ORDER BY 1, 2;
-- 6.4 Vacias. Esperado: 0, 0, 0.
SELECT (SELECT COUNT(*) FROM CRD.POSG) POSG, (SELECT COUNT(*) FROM CRD.PSPR) PSPR, (SELECT COUNT(*) FROM CRD.PSCT) PSCT FROM DUAL;


-- 7. REVERSO — COMENTADO. ⛔ Si ya hay documentos distribuidos, PSCT es lo UNICO que permite
--    devolver el seguro anterior a las cuotas. Exportar antes.
-- DROP TABLE CRD.PSCT CASCADE CONSTRAINTS;
-- DROP TABLE CRD.PSPR CASCADE CONSTRAINTS;
-- DROP TABLE CRD.POSG CASCADE CONSTRAINTS;
-- DROP SEQUENCE CRD.SQ_PSCTCDGO; DROP SEQUENCE CRD.SQ_PSPRCDGO; DROP SEQUENCE CRD.SQ_POSGCDGO;

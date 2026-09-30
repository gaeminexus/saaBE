-- =====================================================================================
-- CRD.PLVN + CRD.DPLV — DECLARACION DE PLAZO VENCIDO y SEGURO ANULADO POR CUOTA
-- FECHA: 2026-09-30 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- PARA QUE: pasar prestamos EN_MORA (PRSTIDST 11) a DE_PLAZO_VENCIDO (8) en lote, con un
-- memorando de orden de cobro (Credito) y una liquidacion contable (Contabilidad), y poder
-- revertirlo. Hoy ningun codigo escribe el estado 8.
--
-- CRD.PLVN — UNA FILA POR PRESTAMO DECLARADO. Guarda la FOTO de los valores a la fecha de
--   corte. No es una optimizacion: la mora sigue corriendo despues de declarar (D6), asi que
--   si el memorando se regenerara leyendo las cuotas en vivo, reimprimirlo un mes despues
--   daria otro total CON LA MISMA FECHA IMPRESA. Los documentos se imprimen desde aca.
--
-- CRD.DPLV — UNA FILA POR CUOTA A LA QUE SE LE ANULO EL SEGURO. Al declarar, el desgravamen
--   y el seguro de incendio de las cuotas con vencimiento POSTERIOR a la fecha de corte se
--   ponen en cero (D13, D19, D22). Al revertir, vuelven (D23). El valor original tiene que
--   estar guardado en algun lado, y DTPRDSOR (desgravamenOriginal) NO sirve: ya lo escribe
--   el abono a capital (AbonoCapitalPrestamoServiceImpl:763, PrestamoServiceImpl:535, :732).
--
-- AUTORIZADAS POR EL USUARIO el 2026-09-30 (PLVN en D12, DPLV en D23). Ver el recuadro §3
-- de REGISTRO-RESERVAS-EQUIPOS.md: crear una tabla en CRD lo decide el usuario de este equipo.
--
-- DISENO:   crd/DISENO-PASE-A-PLAZO-VENCIDO.md
-- CONTRATO: crd/API-PASE-A-PLAZO-VENCIDO.md
--
-- ⛔ VA ANTES DEL WAR que mapee estas entidades: Hibernate pide toda columna mapeada en el
--    SELECT y, sin la tabla, cualquier lectura revienta (H58, H75, y el 246 del 2026-09-30).
--
-- NO EJECUTAR SIN REVISAR. Correr por bloques y revisar la salida de cada uno antes de
-- seguir. SQL PURO: sin comandos SQL*Plus (WHENEVER / SET / DEFINE / PROMPT).
--
-- SOLO EN EL BACKEND: este archivo NO se espeja a saaFE.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS — si alguno no da lo esperado, PARAR
-- =====================================================================================

-- 0.1 Los dos codigos de 4 letras tienen que estar LIBRES en toda la base, no solo en CRD.
--     Esperado: 0 filas.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t WHERE t.TABLE_NAME IN ('PLVN', 'DPLV');

-- 0.2 Las secuencias tambien. Esperado: 0 filas.
SELECT s.SEQUENCE_OWNER, s.SEQUENCE_NAME FROM ALL_SEQUENCES s
WHERE  s.SEQUENCE_NAME IN ('SQ_PLVNCDGO', 'SQ_DPLVCDGO');

-- 0.3 Las tablas de las que dependen. Esperado: 2 filas (CRD.PRST, CRD.DTPR).
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME IN ('PRST', 'DTPR');

-- 0.4 Las columnas de CRD.DTPR que DPLV respalda tienen que existir con esos nombres.
--     Esperado: 4 filas (DTPRDSGR, DTPRVLSI, DTPRTTLL, DTPRTTCS).
--     Si el ejecutor BE mide que al anular el seguro cambia OTRA columna de la cuota, este
--     script se corrige ANTES de correrlo, no despues.
SELECT c.COLUMN_NAME, c.DATA_TYPE FROM ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'DTPR'
AND    c.COLUMN_NAME IN ('DTPRDSGR', 'DTPRVLSI', 'DTPRTTLL', 'DTPRTTCS');

-- 0.5 Foto del universo: cuantos prestamos hay hoy en cada estado que importa.
--     Informativo. 11 = EN_MORA (los que la pantalla va a listar), 8 = DE_PLAZO_VENCIDO (los
--     historicos, que empiezan a generar mora con el WAR de este frente: D10).
SELECT p.PRSTIDST, COUNT(*) AS PRESTAMOS
FROM   CRD.PRST p
WHERE  p.PRSTIDST IN (8, 11)
GROUP  BY p.PRSTIDST
ORDER  BY p.PRSTIDST;


-- =====================================================================================
-- 1. CRD.PLVN — la declaracion, con la foto de los valores
-- =====================================================================================

CREATE TABLE CRD.PLVN (
  PLVNCDGO  NUMBER          NOT NULL,   -- PK
  PRSTCDGO  NUMBER          NOT NULL,   -- FK al prestamo (CRD.PRST)
  PLVNESTD  NUMBER          NOT NULL,   -- 1 DECLARADA · 2 LIQUIDADA · 3 REVERTIDA
  PLVNESAN  NUMBER          NOT NULL,   -- PRSTIDST antes de declarar (registro; el reverso vuelve SIEMPRE a 11, D14)

  -- Memorando (paso 1, Credito)
  PLVNNMMM  VARCHAR2(60)    NOT NULL,   -- numero de memorando, ingresado (D9); unico (D16)
  PLVNFCCR  DATE            NOT NULL,   -- fecha de corte = fecha del memorando (D4)
  PLVNPRNM  VARCHAR2(200),              -- PARA: nombre (D21)
  PLVNPRCR  VARCHAR2(200),              -- PARA: cargo
  PLVNCCNM  VARCHAR2(200),              -- CC: nombre
  PLVNCCCR  VARCHAR2(200),              -- CC: cargo

  -- Foto de identificacion (no se lee en vivo: un nombre puede corregirse despues)
  PLVNNMPR  VARCHAR2(300),              -- nombre del participe
  PLVNIDPR  VARCHAR2(20),               -- cedula
  PLVNTPCR  VARCHAR2(200),              -- tipo de credito (nombre del producto)
  PLVNFCIN  DATE,                       -- fecha inicial del prestamo
  PLVNFCFN  DATE,                       -- fecha final del prestamo
  PLVNFCUC  DATE,                       -- ultima fecha de cobro (nula si nunca pago)
  PLVNFCIM  DATE,                       -- inicio de la mora (vencimiento mas antiguo impago)

  -- Foto del cuadro del memorando, a PLVNFCCR (devengado / cobrado / saldo)
  PLVNMNTO  NUMBER(18,2)    NOT NULL,   -- monto del prestamo
  PLVNCPCB  NUMBER(18,2)    NOT NULL,   -- capital cobrado
  PLVNSLCP  NUMBER(18,2)    NOT NULL,   -- saldo capital vencido (acelerado, D11)
  PLVNINDV  NUMBER(18,2)    NOT NULL,   -- interes devengado (TODAS las cuotas, D11)
  PLVNINCB  NUMBER(18,2)    NOT NULL,   -- interes cobrado
  PLVNSLIN  NUMBER(18,2)    NOT NULL,   -- saldo de interes
  PLVNDSDV  NUMBER(18,2)    NOT NULL,   -- desgravamen devengado (vencimiento <= corte, D22)
  PLVNDSCB  NUMBER(18,2)    NOT NULL,   -- desgravamen cobrado
  PLVNSLDS  NUMBER(18,2)    NOT NULL,   -- saldo desgravamen
  PLVNSGDV  NUMBER(18,2)    NOT NULL,   -- seguro incendio devengado (vencimiento <= corte, D22)
  PLVNSGCB  NUMBER(18,2)    NOT NULL,   -- seguro incendio cobrado
  PLVNSLSG  NUMBER(18,2)    NOT NULL,   -- saldo seguro incendio
  PLVNMRDV  NUMBER(18,2)    NOT NULL,   -- mora devengada AL CORTE (formula pura, no la persistida)
  PLVNMRCB  NUMBER(18,2)    NOT NULL,   -- mora cobrada
  PLVNSLMR  NUMBER(18,2)    NOT NULL,   -- saldo de mora
  PLVNTTCB  NUMBER(18,2)    NOT NULL,   -- total cobrado
  PLVNTTPC  NUMBER(18,2)    NOT NULL,   -- total por cobrar al corte
  PLVNDVMN  NUMBER(18,2),               -- dividendo mensual
  PLVNCTPL  NUMBER          NOT NULL,   -- plazo (numero de cuotas)
  PLVNCTCB  NUMBER          NOT NULL,   -- cuotas cobradas
  PLVNCTPN  NUMBER          NOT NULL,   -- cuotas pendientes
  PLVNCTXV  NUMBER          NOT NULL,   -- cuotas pendientes por vencer (vencimiento > corte)

  -- Auditoria del paso 1
  PLVNUSDC  VARCHAR2(50),               -- usuario que declaro
  PLVNFCDC  TIMESTAMP,                  -- cuando

  -- Liquidacion (paso 2, Contabilidad) — su propia fecha de corte y su propia foto
  PLVNFCLQ  DATE,                       -- fecha de corte de la liquidacion
  PLVNLQCP  NUMBER(18,2),               -- saldo capital vencido
  PLVNLQIN  NUMBER(18,2),               -- interes vencido
  PLVNLQDS  NUMBER(18,2),               -- seguro desgravamen vencido
  PLVNLQSG  NUMBER(18,2),               -- seguro incendio vencido
  PLVNLQMR  NUMBER(18,2),               -- interes de mora
  PLVNLQTT  NUMBER(18,2),               -- total adeudado
  PLVNLQCI  NUMBER,                     -- cuotas impagas (el texto las dice en numero y letras)
  PLVNUSLQ  VARCHAR2(50),               -- usuario que liquido
  PLVNFRLQ  TIMESTAMP,                  -- cuando

  -- Reverso (D14: Jefe de Credito, vuelve a 11)
  PLVNUSRV  VARCHAR2(50),
  PLVNFCRV  TIMESTAMP,
  PLVNMTRV  VARCHAR2(500)               -- motivo, obligatorio para revertir (lo valida el backend)
);

-- Los NOT NULL de la foto son a proposito: una declaracion sin sus valores no se puede
-- imprimir, y un 0 que el codigo no escribio seria un 0 falso en un documento que va a legal.
-- Las columnas de auditoria y las del paso 2 y del reverso son NULLABLE porque se llenan
-- despues. Sin DEFAULT en ninguna: H77 — Hibernate manda NULL explicito y el DEFAULT no gana.

COMMENT ON TABLE  CRD.PLVN           IS 'Declaracion de plazo vencido de un prestamo: foto de valores del memorando (Credito) y de la liquidacion (Contabilidad). Los documentos se imprimen desde aca, no desde las cuotas.';
COMMENT ON COLUMN CRD.PLVN.PLVNESTD  IS '1 DECLARADA, 2 LIQUIDADA, 3 REVERTIDA. Constantes planas, sin rubro. Una REVERTIDA nunca se borra.';
COMMENT ON COLUMN CRD.PLVN.PLVNESAN  IS 'PRSTIDST antes de declarar. Solo registro: el reverso vuelve siempre a 11 EN_MORA.';
COMMENT ON COLUMN CRD.PLVN.PLVNNMMM  IS 'Numero de memorando ingresado por el usuario. Unico en toda la tabla, incluidas las revertidas: un documento emitido no se renumera.';
COMMENT ON COLUMN CRD.PLVN.PLVNFCCR  IS 'Fecha de corte del memorando. Desde el dia siguiente, las cuotas quedan sin seguro (CRD.DPLV).';
COMMENT ON COLUMN CRD.PLVN.PLVNMRDV  IS 'Mora calculada AL CORTE con la formula de ProcesoMoraPrestamoService.calcularMoraCuota, no la persistida en DTPR.';


-- =====================================================================================
-- 2. CRD.DPLV — el seguro original de cada cuota a la que se le anulo
-- =====================================================================================

CREATE TABLE CRD.DPLV (
  DPLVCDGO  NUMBER          NOT NULL,   -- PK
  PLVNCDGO  NUMBER          NOT NULL,   -- FK a la declaracion
  DTPRCDGO  NUMBER          NOT NULL,   -- FK a la cuota
  DPLVDSGR  NUMBER(18,2)    NOT NULL,   -- DTPRDSGR (desgravamen) antes de anular
  DPLVVLSI  NUMBER(18,2)    NOT NULL,   -- DTPRVLSI (seguro de incendio) antes de anular
  DPLVTTLL  NUMBER(18,2),               -- DTPRTTLL (total) antes de anular
  DPLVTTCS  NUMBER(18,2),               -- DTPRTTCS (total con seguro) antes de anular
  DPLVFCRS  TIMESTAMP                   -- cuando se restituyo (reverso). Nulo = sigue anulado
);

COMMENT ON TABLE  CRD.DPLV           IS 'Seguro original (desgravamen e incendio) de cada cuota posterior al corte a la que se le anulo al declarar plazo vencido. El reverso lo restituye desde aca.';
COMMENT ON COLUMN CRD.DPLV.DPLVFCRS  IS 'Fecha de restitucion en el reverso. La fila no se borra: queda como rastro de lo que se anulo y se devolvio.';


-- =====================================================================================
-- 3. SECUENCIAS — arrancan en 1: las tablas nacen vacias, no hay PK explicitas
-- =====================================================================================
CREATE SEQUENCE CRD.SQ_PLVNCDGO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE CRD.SQ_DPLVCDGO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;


-- =====================================================================================
-- 4. RESTRICCIONES
-- =====================================================================================

-- 4.1 PKs
ALTER TABLE CRD.PLVN ADD CONSTRAINT PK_PLVN PRIMARY KEY (PLVNCDGO);
ALTER TABLE CRD.DPLV ADD CONSTRAINT PK_DPLV PRIMARY KEY (DPLVCDGO);

-- 4.2 FKs
ALTER TABLE CRD.PLVN ADD CONSTRAINT FK_PLVN_PRST
  FOREIGN KEY (PRSTCDGO) REFERENCES CRD.PRST (PRSTCDGO);
ALTER TABLE CRD.DPLV ADD CONSTRAINT FK_DPLV_PLVN
  FOREIGN KEY (PLVNCDGO) REFERENCES CRD.PLVN (PLVNCDGO);
ALTER TABLE CRD.DPLV ADD CONSTRAINT FK_DPLV_DTPR
  FOREIGN KEY (DTPRCDGO) REFERENCES CRD.DTPR (DTPRCDGO);

-- 4.3 D16: el numero de memorando no se repite. Normalizado: '033-2026' y ' 033-2026 ' o
--     'gr-033' y 'GR-033' son el mismo documento. El backend valida antes con un mensaje
--     claro; este indice es la red para la carrera de dos usuarios a la vez.
CREATE UNIQUE INDEX CRD.UX_PLVN_MEMORANDO ON CRD.PLVN (UPPER(TRIM(PLVNNMMM)));

-- 4.4 Un prestamo tiene a lo sumo UNA declaracion viva (DECLARADA o LIQUIDADA). Las
--     REVERTIDAS no cuentan: un prestamo revertido puede volver a declararse. Indice unico
--     por funcion: las filas en 3 dan NULL y Oracle no indexa claves totalmente nulas.
CREATE UNIQUE INDEX CRD.UX_PLVN_PRESTAMO_VIVO ON CRD.PLVN (
  CASE WHEN PLVNESTD IN (1, 2) THEN PRSTCDGO END
);

-- 4.5 Para listar las declaraciones de un prestamo
CREATE INDEX CRD.IX_PLVN_PRST ON CRD.PLVN (PRSTCDGO);

-- 4.6 Una cuota aparece una sola vez por declaracion
CREATE UNIQUE INDEX CRD.UX_DPLV_DECLARACION_CUOTA ON CRD.DPLV (PLVNCDGO, DTPRCDGO);

-- 4.7 Estados acotados
ALTER TABLE CRD.PLVN ADD CONSTRAINT CK_PLVN_ESTADO CHECK (PLVNESTD IN (1, 2, 3));

-- 4.8 Ningun saldo negativo en la foto (invariante 5 del diseno §4.4bis). El backend lo
--     valida antes; este CHECK es la red.
ALTER TABLE CRD.PLVN ADD CONSTRAINT CK_PLVN_SALDOS CHECK (
      PLVNSLCP >= 0 AND PLVNSLIN >= 0 AND PLVNSLDS >= 0
  AND PLVNSLSG >= 0 AND PLVNSLMR >= 0 AND PLVNTTPC >= 0
);


-- =====================================================================================
-- 5. CONTROLES POSTERIORES — leer la salida, no asumir
-- =====================================================================================

-- 5.1 Columnas de PLVN. Esperado: 54 filas. Revisar NULLABLE contra el bloque 1: el mapeo
--     JPA no dice si una columna es obligatoria (H68); lo dice solo esta consulta.
SELECT c.COLUMN_ID, c.COLUMN_NAME, c.DATA_TYPE, c.DATA_PRECISION, c.DATA_SCALE, c.NULLABLE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'PLVN'
ORDER  BY c.COLUMN_ID;

-- 5.2 Columnas de DPLV. Esperado: 8 filas.
SELECT c.COLUMN_ID, c.COLUMN_NAME, c.DATA_TYPE, c.DATA_PRECISION, c.DATA_SCALE, c.NULLABLE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'DPLV'
ORDER  BY c.COLUMN_ID;

-- 5.3 Secuencias. Esperado: 2 filas, LAST_NUMBER = 1.
SELECT s.SEQUENCE_NAME, s.LAST_NUMBER
FROM   ALL_SEQUENCES s
WHERE  s.SEQUENCE_OWNER = 'CRD' AND s.SEQUENCE_NAME IN ('SQ_PLVNCDGO', 'SQ_DPLVCDGO');

-- 5.4 Indices. Esperado: PK_PLVN, UX_PLVN_MEMORANDO, UX_PLVN_PRESTAMO_VIVO, IX_PLVN_PRST
--     en PLVN; PK_DPLV, UX_DPLV_DECLARACION_CUOTA en DPLV.
SELECT i.TABLE_NAME, i.INDEX_NAME, i.UNIQUENESS, i.INDEX_TYPE
FROM   ALL_INDEXES i
WHERE  i.OWNER = 'CRD' AND i.TABLE_NAME IN ('PLVN', 'DPLV')
ORDER  BY i.TABLE_NAME, i.INDEX_NAME;

-- 5.5 Restricciones con nombre. Esperado: PK_PLVN, FK_PLVN_PRST, CK_PLVN_ESTADO,
--     CK_PLVN_SALDOS, PK_DPLV, FK_DPLV_PLVN, FK_DPLV_DTPR — todas ENABLED.
SELECT c.TABLE_NAME, c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.STATUS
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME IN ('PLVN', 'DPLV')
AND    c.CONSTRAINT_NAME NOT LIKE 'SYS_%'
ORDER  BY c.TABLE_NAME, c.CONSTRAINT_TYPE, c.CONSTRAINT_NAME;

-- 5.6 Las dos tablas existen y estan vacias. Esperado: 0 y 0.
SELECT (SELECT COUNT(*) FROM CRD.PLVN) AS PLVN, (SELECT COUNT(*) FROM CRD.DPLV) AS DPLV FROM DUAL;


-- =====================================================================================
-- 6. REVERSO — COMENTADO A PROPOSITO. Descomentar SOLO si hay que volver atras.
-- =====================================================================================
-- ⛔ Si ya hay declaraciones, esto BORRA el rastro de que prestamos se pasaron a plazo
--    vencido, con que memorando, y el seguro original de sus cuotas — sin el cual el
--    reverso de una declaracion ya no puede devolver el seguro. Exportar las dos antes.
--
-- DROP TABLE CRD.DPLV CASCADE CONSTRAINTS;
-- DROP TABLE CRD.PLVN CASCADE CONSTRAINTS;
-- DROP SEQUENCE CRD.SQ_DPLVCDGO;
-- DROP SEQUENCE CRD.SQ_PLVNCDGO;

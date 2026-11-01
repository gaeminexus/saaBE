-- =====================================================================================
-- C19-01 — Extension generica del motor de reportes contables (CNT.RPRT / CNT.DTRP):
--          "estructuras de renglones" (B17, flujo de caja y cualquier reporte cuyo renglon NO es una
--          cuenta del plan sino un codigo propio alimentado por cuentas del plan).
-- Cuadre Superintendencia, tarea C19 · 2026-10-28 · Diseño: docs/logica-negocio/rpr/ESTRUCTURAS-DE-RENGLONES.md
--
-- QUE HACE
--   1. CNT.RPRT gana columnas OPCIONALES. Con todas en NULL el reporte se comporta exactamente igual que hoy
--      (RPRTTPRP NULL/1 = balance sobre el plan de cuentas por rangos de DTRP, el motor de siempre).
--   2. CNT.RNRP (renglon de estructura) y CNT.CRRP (regla cuenta del plan -> renglon), con vigencia por fecha de
--      corte para que el catalogo de la Superintendencia pueda cambiar sin perder los meses anteriores.
--   3. Secuencias CNT.SQ_RNRPCDGO y CNT.SQ_CRRPCDGO.
--
-- SQL PURO (sin SET/DEFINE/WHENEVER). El DDL de Oracle hace COMMIT implicito: correr primero el BLOQUE 1.
-- NO SE HA EJECUTADO. Orden de despliegue: este script -> C19-02 (B17) -> C19-03 (flujo) -> C19-04 (estados) -> WAR.
-- Con el WAR anterior las columnas y tablas nuevas no hacen daño: nadie las lee.
-- Con el WAR nuevo y SIN este script: /esrp y /esfo responden error claro; /dtmt/generarBalance sigue igual.
-- =====================================================================================

-- BLOQUE 1 — CONTROL ANTES
-- 1.1 Columnas actuales de CNT.RPRT. ESPERADO: 5 filas (RPRTCDGO, PJRQCDGO, RPRTNMBR, RPRTESTD, RPRTALTR).
SELECT column_name, data_type FROM all_tab_columns WHERE owner = 'CNT' AND table_name = 'RPRT' ORDER BY column_id;
-- 1.2 Las tablas nuevas no deben existir. ESPERADO: 0 filas.
SELECT owner, table_name FROM all_tables WHERE owner = 'CNT' AND table_name IN ('RNRP', 'CRRP');
-- 1.3 Las secuencias nuevas no deben existir. ESPERADO: 0 filas.
SELECT sequence_name FROM all_sequences WHERE sequence_owner = 'CNT' AND sequence_name IN ('SQ_RNRPCDGO', 'SQ_CRRPCDGO');

-- BLOQUE 2 — CABECERA DEL REPORTE (CNT.RPRT)
ALTER TABLE CNT.RPRT ADD (
  RPRTTPRP NUMBER(1),          -- tipo: NULL/1 = plan de cuentas por rangos (motor actual) · 2 = estructura de renglones
  RPRTCDES VARCHAR2(10),       -- codigo de la estructura ante el ente de control (B17, FLC...)
  RPRTPRDC VARCHAR2(1),        -- periodicidad del archivo (M mensual, Q quincenal, S semestral...)
  RPRTCDEN VARCHAR2(10),       -- codigo de la entidad ante el ente de control (ASOPREP = 3968)
  RPRTBSVL NUMBER(1),          -- base del valor: 1 saldo a la fecha de corte · 2 movimiento del periodo · 3 saldo al inicio
  RPRTMPAU NUMBER(1),          -- 1 = cuenta sin regla -> renglon cuyo codigo es el prefijo mas largo de la cuenta sin separadores
  RPRTACPF NUMBER(1),          -- 1 = un renglon suma tambien lo de los renglones cuyo codigo empieza por el suyo (catalogo jerarquico)
  RPRTFRAR NUMBER(1),          -- formato de archivo: NULL sin archivo · 1 estructura de la Superintendencia (.txt TAB/CRLF/latin1)
  RPRTSMCT NUMBER(1),          -- 1 = la cabecera del archivo lleva la suma de control de los valores
  RPRTTTLO VARCHAR2(200)       -- titulo oficial impreso (estados financieros); NULL = RPRTNMBR
);

-- BLOQUE 3 — RENGLONES (CNT.RNRP)
CREATE TABLE CNT.RNRP (
  RNRPCDGO NUMBER        NOT NULL,
  RPRTCDGO NUMBER        NOT NULL,
  RNRPCDRN VARCHAR2(30)  NOT NULL,  -- codigo del renglon tal como se reporta (110105, 7, FLC-11...)
  RNRPNMBR VARCHAR2(200),           -- descripcion
  RNRPNVLL NUMBER(2),               -- nivel (presentacion)
  RNRPORDN NUMBER(6)     NOT NULL,  -- orden en el reporte y en el archivo
  RNRPTPCL NUMBER(1)     NOT NULL,  -- calculo: 1 suma de cuentas asignadas · 2 neto deudor (debe - haber) de sus cuentas · 3 formula
  RNRPBSVL NUMBER(1),               -- base del valor propia del renglon (NULL = la del reporte)
  RNRPFRML VARCHAR2(1000),          -- formula (tipo 3): terminos [+|-]codigo separados por espacio, ej. "+FLC-10 +FLC-22 -FLC-32"
  RNRPRPAR NUMBER(1),               -- 1 = va en el archivo (default); 0 = solo de presentacion/calculo
  RNRPFCDS DATE,                    -- vigencia desde (fecha de corte); NULL = siempre
  RNRPFCHS DATE,                    -- vigencia hasta (fecha de corte); NULL = abierta
  RNRPESTD NUMBER(1)     NOT NULL,  -- 1 activo · 2 inactivo
  CONSTRAINT PK_RNRP PRIMARY KEY (RNRPCDGO),
  CONSTRAINT FK_RNRP_RPRT FOREIGN KEY (RPRTCDGO) REFERENCES CNT.RPRT (RPRTCDGO)
);
CREATE INDEX CNT.IX_RNRP_RPRT ON CNT.RNRP (RPRTCDGO, RNRPORDN);

-- BLOQUE 4 — REGLAS CUENTA DEL PLAN -> RENGLON (CNT.CRRP)
CREATE TABLE CNT.CRRP (
  CRRPCDGO NUMBER        NOT NULL,
  RPRTCDGO NUMBER        NOT NULL,
  CRRPCNTA VARCHAR2(50)  NOT NULL,  -- cuenta del plan (PLNNCNTA, con sus puntos)
  CRRPALCN NUMBER(1)     NOT NULL,  -- alcance: 1 la cuenta y sus subcuentas · 2 solo la cuenta exacta
  CRRPCDRN VARCHAR2(30),            -- renglon destino; NULL = el que da el mapeo automatico (RPRTMPAU)
  CRRPMDSG NUMBER(1)     NOT NULL,  -- signo: 1 segun naturaleza · 2 deudor (D-H) · 3 acreedor (H-D) · 4 valor absoluto por cuenta
  CRRPRPRT NUMBER(1)     NOT NULL,  -- 1 se reporta · 0 NO se reporta (excluida a proposito, queda en las advertencias)
  CRRPFCDS DATE,                    -- vigencia desde (fecha de corte); NULL = siempre
  CRRPFCHS DATE,                    -- vigencia hasta (fecha de corte); NULL = abierta
  CRRPOBSR VARCHAR2(500),           -- sustento de la regla (decision, criterio)
  CRRPESTD NUMBER(1)     NOT NULL,  -- 1 activo · 2 inactivo
  CONSTRAINT PK_CRRP PRIMARY KEY (CRRPCDGO),
  CONSTRAINT FK_CRRP_RPRT FOREIGN KEY (RPRTCDGO) REFERENCES CNT.RPRT (RPRTCDGO)
);
CREATE INDEX CNT.IX_CRRP_RPRT ON CNT.CRRP (RPRTCDGO, CRRPCNTA);

-- BLOQUE 5 — SECUENCIAS
CREATE SEQUENCE CNT.SQ_RNRPCDGO START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE CNT.SQ_CRRPCDGO START WITH 1 INCREMENT BY 1 NOCACHE;

-- BLOQUE 6 — PERMISOS (mismo usuario de la aplicacion que lee CNT.RPRT; ajustar si en esta base es otro)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON CNT.RNRP TO <usuario_app>;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON CNT.CRRP TO <usuario_app>;
-- GRANT SELECT ON CNT.SQ_RNRPCDGO TO <usuario_app>;
-- GRANT SELECT ON CNT.SQ_CRRPCDGO TO <usuario_app>;

-- BLOQUE 7 — CONTROL DESPUES
-- 7.1 ESPERADO: 15 filas (las 5 de antes + 10 nuevas, todas NULL en los reportes existentes).
SELECT column_name, data_type FROM all_tab_columns WHERE owner = 'CNT' AND table_name = 'RPRT' ORDER BY column_id;
-- 7.2 Los 5 reportes existentes siguen siendo del motor de siempre. ESPERADO: 5 filas con tipo NULL.
SELECT RPRTCDGO, RPRTNMBR, RPRTALTR, RPRTTPRP FROM CNT.RPRT ORDER BY RPRTALTR;
-- 7.3 ESPERADO: 2 tablas y 2 secuencias.
SELECT owner, table_name FROM all_tables WHERE owner = 'CNT' AND table_name IN ('RNRP', 'CRRP');
SELECT sequence_name FROM all_sequences WHERE sequence_owner = 'CNT' AND sequence_name IN ('SQ_RNRPCDGO', 'SQ_CRRPCDGO');

-- REVERSO
-- DROP SEQUENCE CNT.SQ_CRRPCDGO;
-- DROP SEQUENCE CNT.SQ_RNRPCDGO;
-- DROP TABLE CNT.CRRP;
-- DROP TABLE CNT.RNRP;
-- ALTER TABLE CNT.RPRT DROP (RPRTTPRP, RPRTCDES, RPRTPRDC, RPRTCDEN, RPRTBSVL, RPRTMPAU, RPRTACPF, RPRTFRAR, RPRTSMCT, RPRTTTLO);

-- =====================================================================================
-- C19-01 — Motor de balances (CNT.RPRT / CNT.DTRP -> CNT.DTMT) robustecido para TODO tipo de balance contable:
--          reportes de RENGLONES PROPIOS (B17, flujo de caja), salida en archivo de la Superintendencia, base de
--          valor, mapeo por prefijo, vigencias, nivel de profundidad. Decision D-031 (2026-10-29): sin motor paralelo.
-- Cuadre Superintendencia, tarea C19b · Diseño: docs/logica-negocio/rpr/ESTRUCTURAS-DE-RENGLONES.md
--
-- PARA UN AMBIENTE LIMPIO (produccion). En la base de trabajo, donde ya corrio la version anterior (con CNT.CRRP),
-- se corre C19-05 en su lugar.
--
-- QUE HACE
--   1. CNT.RPRT (cabecera del reporte del motor) gana 11 columnas OPCIONALES: con todas en NULL el reporte es el
--      balance sobre el plan de cuentas de siempre.
--   2. CNT.DTRP (detalle del reporte) gana 7 columnas OPCIONALES: en un reporte de renglones propios cada fila es
--      una REGLA cuenta del plan -> renglon; en los balances de siempre quedan NULL.
--   3. CNT.RNRP: renglones del reporte (detalle de CNT.RPRT). Es la unica tabla nueva y es estrictamente necesaria:
--      los renglones del B17 (534 codigos 110105...) no son cuentas del plan, no caben en DTRP (que son reglas
--      sobre cuentas) ni en PLNN.
--
-- ⛔ ORDEN: este script ANTES de desplegar el WAR de C19b. Las entidades ReporteContable y DetalleReporteContable
--    mapean las columnas nuevas: sin ellas el motor de balances completo falla al leer CNT.RPRT/CNT.DTRP.
-- SQL PURO (sin SET/DEFINE/WHENEVER). El DDL de Oracle hace COMMIT implicito. NO SE HA EJECUTADO.
-- =====================================================================================

-- BLOQUE 1 — CONTROL ANTES
-- 1.1 ESPERADO: CNT.RPRT con 5 columnas (RPRTCDGO, PJRQCDGO, RPRTNMBR, RPRTESTD, RPRTALTR) y CNT.DTRP con 9.
SELECT table_name, COUNT(*) columnas FROM all_tab_columns WHERE owner = 'CNT' AND table_name IN ('RPRT', 'DTRP') GROUP BY table_name;
-- 1.2 ESPERADO: 0 filas (ni RNRP ni su secuencia).
SELECT owner, table_name FROM all_tables WHERE owner = 'CNT' AND table_name = 'RNRP';
SELECT sequence_name FROM all_sequences WHERE sequence_owner = 'CNT' AND sequence_name = 'SQ_RNRPCDGO';

-- BLOQUE 2 — CABECERA DEL REPORTE (CNT.RPRT)
ALTER TABLE CNT.RPRT ADD (
  RPRTTPRP NUMBER(1),          -- tipo: NULL/1 = balance sobre el plan de cuentas (rangos de DTRP) · 2 = renglones propios (RNRP)
  RPRTCDES VARCHAR2(10),       -- codigo de la estructura ante el ente de control (B17, FLC...)
  RPRTPRDC VARCHAR2(1),        -- periodicidad del archivo (M mensual, Q quincenal, S semestral...)
  RPRTCDEN VARCHAR2(10),       -- codigo de la entidad ante el ente de control (ASOPREP = 3968)
  RPRTBSVL NUMBER(1),          -- base del valor de los renglones: 1 saldo al corte · 2 movimiento del periodo · 3 saldo al inicio
  RPRTMPAU NUMBER(1),          -- 1 = cuenta sin regla -> renglon cuyo codigo es el prefijo mas largo de la cuenta sin separadores
  RPRTACPF NUMBER(1),          -- 1 = un renglon suma lo de los renglones cuyo codigo empieza por el suyo (catalogo jerarquico)
  RPRTFRAR NUMBER(1),          -- tipo de salida en archivo: NULL sin archivo · 1 TXT de la Superintendencia (TAB/CRLF/latin1)
  RPRTSMCT NUMBER(1),          -- 1 = la cabecera del archivo lleva la suma de control
  RPRTTTLO VARCHAR2(200),      -- titulo oficial impreso (estados financieros); NULL = RPRTNMBR
  RPRTNVMX NUMBER(2)           -- nivel de profundidad: digitos maximos del codigo en el resultado; NULL = todos (como siempre)
);

-- BLOQUE 3 — DETALLE DEL REPORTE (CNT.DTRP): reglas cuenta -> renglon de los reportes de renglones propios
ALTER TABLE CNT.DTRP ADD (
  DTRPCDRN VARCHAR2(30),       -- renglon destino (CNT.RNRP.RNRPCDRN); NULL = el que da el mapeo automatico del reporte
  DTRPALCN NUMBER(1),          -- alcance: 1 la cuenta NUMCTADS y sus subcuentas · 2 solo la cuenta exacta
  DTRPMDSG NUMBER(1),          -- signo: 1 segun naturaleza · 2 deudor (D-H) · 3 acreedor (H-D) · 4 valor absoluto por cuenta
  DTRPRPRT NUMBER(1),          -- 1 se reporta · 0 NO se reporta (excluida a proposito, sale en las advertencias)
  DTRPFCDS DATE,               -- vigencia desde (fecha de corte); NULL = siempre
  DTRPFCHS DATE,               -- vigencia hasta (fecha de corte); NULL = abierta
  DTRPOBSR VARCHAR2(500)       -- sustento de la regla (decision, criterio)
);

-- BLOQUE 4 — RENGLONES DEL REPORTE (CNT.RNRP)
CREATE TABLE CNT.RNRP (
  RNRPCDGO NUMBER        NOT NULL,
  RPRTCDGO NUMBER        NOT NULL,
  RNRPCDRN VARCHAR2(30)  NOT NULL,  -- codigo del renglon tal como se reporta (110105, 7, FC18...)
  RNRPNMBR VARCHAR2(200),           -- descripcion
  RNRPNVLL NUMBER(2),               -- nivel (presentacion)
  RNRPORDN NUMBER(6)     NOT NULL,  -- orden en el reporte y en el archivo
  RNRPTPCL NUMBER(1)     NOT NULL,  -- calculo: 1 suma · 2 neto deudor (debe - haber) de sus cuentas · 3 formula
  RNRPBSVL NUMBER(1),               -- base del valor propia del renglon (NULL = la del reporte)
  RNRPFRML VARCHAR2(1000),          -- formula (tipo 3): terminos [+|-]codigo separados por espacio, ej. "+FC10 +FC22 -FC32"
  RNRPRPAR NUMBER(1),               -- 1 = va en el archivo (default); 0 = solo presentacion/control
  RNRPFCDS DATE,                    -- vigencia desde (fecha de corte); NULL = siempre
  RNRPFCHS DATE,                    -- vigencia hasta (fecha de corte); NULL = abierta
  RNRPESTD NUMBER(1)     NOT NULL,  -- 1 activo · 2 inactivo
  CONSTRAINT PK_RNRP PRIMARY KEY (RNRPCDGO),
  CONSTRAINT FK_RNRP_RPRT FOREIGN KEY (RPRTCDGO) REFERENCES CNT.RPRT (RPRTCDGO)
);
CREATE INDEX CNT.IX_RNRP_RPRT ON CNT.RNRP (RPRTCDGO, RNRPORDN);
CREATE SEQUENCE CNT.SQ_RNRPCDGO START WITH 1 INCREMENT BY 1 NOCACHE;

-- BLOQUE 5 — PERMISOS (mismo usuario de la aplicacion que lee CNT.RPRT; ajustar si en esta base es otro)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON CNT.RNRP TO <usuario_app>;
-- GRANT SELECT ON CNT.SQ_RNRPCDGO TO <usuario_app>;

-- BLOQUE 6 — CONTROL DESPUES
-- 6.1 ESPERADO: RPRT 16 columnas, DTRP 16 columnas.
SELECT table_name, COUNT(*) columnas FROM all_tab_columns WHERE owner = 'CNT' AND table_name IN ('RPRT', 'DTRP') GROUP BY table_name;
-- 6.2 Los reportes existentes siguen siendo balances sobre el plan. ESPERADO: todos con tipo NULL y sus DTRP con DTRPCDRN NULL.
SELECT r.RPRTCDGO, r.RPRTNMBR, r.RPRTALTR, r.RPRTTPRP, COUNT(d.DTRPCDGO) rangos, COUNT(d.DTRPCDRN) reglas_renglon
  FROM CNT.RPRT r LEFT JOIN CNT.DTRP d ON d.RPRTCDGO = r.RPRTCDGO GROUP BY r.RPRTCDGO, r.RPRTNMBR, r.RPRTALTR, r.RPRTTPRP ORDER BY r.RPRTALTR;
-- 6.3 ESPERADO: 1 tabla y 1 secuencia.
SELECT owner, table_name FROM all_tables WHERE owner = 'CNT' AND table_name = 'RNRP';
SELECT sequence_name FROM all_sequences WHERE sequence_owner = 'CNT' AND sequence_name = 'SQ_RNRPCDGO';

-- REVERSO (despues de quitar el WAR de C19b)
-- DROP SEQUENCE CNT.SQ_RNRPCDGO;
-- DROP TABLE CNT.RNRP;
-- ALTER TABLE CNT.DTRP DROP (DTRPCDRN, DTRPALCN, DTRPMDSG, DTRPRPRT, DTRPFCDS, DTRPFCHS, DTRPOBSR);
-- ALTER TABLE CNT.RPRT DROP (RPRTTPRP, RPRTCDES, RPRTPRDC, RPRTCDEN, RPRTBSVL, RPRTMPAU, RPRTACPF, RPRTFRAR, RPRTSMCT, RPRTTTLO, RPRTNVMX);

-- =====================================================================================
-- CRD.CTAP — cuentas contables por tipo de aporte (y empresa)
-- FECHA: 2026-08-31
--
-- PARA QUE: el asiento de RECLASIFICACION de la devolucion de aportes (opcion C) necesita,
-- por cada tipo de aporte, su cuenta de PASIVO (el DEBE) y su cuenta de LIQUIDACION (el
-- HABER). El usuario confirmo que se devuelve CUALQUIER tipo, no solo cesantia y jubilacion.
--
-- POR QUE UNA TABLA Y NO LINEAS EN LA PLANTILLA 27:
-- serian ~22 lineas con auxiliares posicionales sin significado propio, y la plantilla no
-- tiene dimension de tipo de aporte: habria que traducir "tipo 12" a "auxiliar 17" en un if
-- de Java. Es la misma fragilidad que produjo el bug de la condonacion (aux1=10, que en la
-- plantilla 25 era una banda posicional).
-- Es ademas el patron que la casa ya usa para el mismo problema: las bandas de cartera
-- resuelven su cuenta desde CRD.BNDP, no desde una plantilla.
--
-- LLEVA EMPRESA a proposito: las cuentas son por empresa (la base tiene 1236, 280 y 300; las
-- de aportes son todas de la 1236). Sin esa columna una segunda instalacion no se puede
-- configurar — mismo criterio que CRD.BNDP y CRD.CRCT.
--
-- ⚠️ LA EMPRESA ES UN NODO DE JERARQUIA: la columna se llama PJRQCDGO y apunta a SCP.PJRQ,
-- igual que en CRD.CRCT y CRD.CBPR. No es "EMPRCDGO".
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS — si alguno no da lo esperado, PARAR
-- =====================================================================================

-- 0.1 El codigo de 4 letras CTAP tiene que estar LIBRE en toda la base, no solo en CRD.
--     Esperado: 0 filas. (Ya se verifico que no existe en src/main/java/com/saa/model/.)
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t WHERE t.TABLE_NAME = 'CTAP';

-- 0.2 Las tablas y secuencia de las que depende. Esperado: 2 filas (CRD.TPAP y SCP.PJRQ).
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  (t.OWNER = 'CRD' AND t.TABLE_NAME = 'TPAP')
OR     (t.OWNER = 'SCP' AND t.TABLE_NAME = 'PJRQ')
ORDER  BY t.OWNER;

-- 0.3 Las 11 cuentas del mapeo existen y son de la empresa 1236. Esperado: 11 filas.
--     Si falta alguna, PARAR: el INSERT de la seccion 3 la dejaria sin mapear en silencio.
SELECT n.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR
FROM   CNT.PLNN n
WHERE  n.PJRQCDGO = 1236
AND    n.PLNNCDGO IN (10349,10350,10351,10352,10353,10354,10355,10356,10357,10358,10359,
                      10360,10361,10362,10363,10364)
ORDER  BY n.PLNNCNTA;

-- 0.4 Los 11 tipos de aporte a mapear existen. Esperado: 11 filas.
SELECT t.TPAPCDGO, t.TPAPNMBR, t.TPAPCSBC, t.TPAPIDST
FROM   CRD.TPAP t
WHERE  t.TPAPCDGO IN (9,11,12,13,14,15,16,21,22,23,24)
ORDER  BY t.TPAPCDGO;


-- =====================================================================================
-- 1. LA TABLA
-- =====================================================================================

CREATE TABLE CRD.CTAP (
    CTAPCDGO NUMBER          NOT NULL,
    TPAPCDGO NUMBER          NOT NULL,
    PJRQCDGO NUMBER          NOT NULL,
    CTAPPLNP NUMBER          NOT NULL,
    CTAPPLNL NUMBER          NOT NULL,
    CTAPESTD NUMBER  DEFAULT 1 NOT NULL
);

ALTER TABLE CRD.CTAP ADD CONSTRAINT PK_CTAP PRIMARY KEY (CTAPCDGO);

ALTER TABLE CRD.CTAP ADD CONSTRAINT FK_CTAP_TPAP
    FOREIGN KEY (TPAPCDGO) REFERENCES CRD.TPAP(TPAPCDGO);
ALTER TABLE CRD.CTAP ADD CONSTRAINT FK_CTAP_PJRQ
    FOREIGN KEY (PJRQCDGO) REFERENCES SCP.PJRQ(PJRQCDGO);
ALTER TABLE CRD.CTAP ADD CONSTRAINT FK_CTAP_PLNP
    FOREIGN KEY (CTAPPLNP) REFERENCES CNT.PLNN(PLNNCDGO);
ALTER TABLE CRD.CTAP ADD CONSTRAINT FK_CTAP_PLNL
    FOREIGN KEY (CTAPPLNL) REFERENCES CNT.PLNN(PLNNCDGO);

-- Un tipo de aporte tiene UNA configuracion por empresa. Sin esto, dos filas para el mismo
-- tipo hacen que el asiento salga por la que devuelva primero la consulta — y cuadra igual.
ALTER TABLE CRD.CTAP ADD CONSTRAINT UK_CTAP_TPAP_PJRQ UNIQUE (TPAPCDGO, PJRQCDGO);

CREATE INDEX CRD.IDX_CTAP_EMPRESA ON CRD.CTAP (PJRQCDGO);

CREATE SEQUENCE CRD.SQ_CTAPCDGO START WITH 1 INCREMENT BY 1 NOCACHE;

COMMENT ON TABLE  CRD.CTAP           IS 'Cuentas contables por tipo de aporte y empresa. Fuente UNICA de las cuentas del asiento de reclasificacion de la devolucion de aportes (opcion C, decision del usuario 2026-08-31). Un tipo sin fila aca NO se puede devolver contablemente: el proceso falla con mensaje claro, nunca adivina una cuenta.';
COMMENT ON COLUMN CRD.CTAP.TPAPCDGO  IS 'Tipo de aporte (CRD.TPAP).';
COMMENT ON COLUMN CRD.CTAP.PJRQCDGO  IS 'Empresa (nodo de jerarquia SCP.PJRQ). Las cuentas son por empresa; sin esta columna una segunda instalacion no se puede configurar.';
COMMENT ON COLUMN CRD.CTAP.CTAPPLNP  IS 'Cuenta de PASIVO del aporte (2.1.01.xx / 2.1.02.xx). Es el DEBE de la reclasificacion: baja lo que el fondo le debe al socio.';
COMMENT ON COLUMN CRD.CTAP.CTAPPLNL  IS 'Cuenta de LIQUIDACION por pagar (2.3.01.xx). Es el HABER de la reclasificacion: nace la obligacion de pagarle. Es tambien la cuenta que CXP debita al pagar, via el grupo del producto de pago (CRD.TPAP.TPAPPRDP).';


-- =====================================================================================
-- 2. LA CARGA — mapeo confirmado por el usuario el 2026-08-31
-- =====================================================================================
--
-- CONFIRMADO POR EL USUARIO:
--   - Las RESERVAS (tipos 17, 18, 19, 20) NO SE DEVUELVEN -> no se mapean.
--   - El tipo 22 (JUBILACION RETIRO VOLUNTARIO) es SIN RELACION LABORAL -> cuelga de
--     2.1.02.25, y le corresponde 2.1.02.25.05 CTA INDIVIDUAL PASIVOS JUBILACION. Es el
--     espejo exacto del tipo 21 (cesantia retiro voluntario -> 2.1.01.20.05).
--
-- NO SE MAPEAN, y es correcto:
--   17, 18, 19, 20  reservas, no se devuelven (decision del usuario)
--   2, 3, 4, 5, 10, 25  cero movimientos en toda la historia
--   1  APORTE PERSONALES — 5 movimientos, $140.000. QUEDA PENDIENTE de definicion.
--
-- ⚠️ Un tipo sin fila aca hace que la devolucion que lo incluya ABORTE con mensaje claro.
-- Es el comportamiento correcto —mejor que contabilizar contra una cuenta inventada— pero
-- es una limitacion operativa real: si un participe tiene saldo en el tipo 1, su devolucion
-- no se va a poder procesar hasta que se defina su cuenta.

INSERT INTO CRD.CTAP (CTAPCDGO, TPAPCDGO, PJRQCDGO, CTAPPLNP, CTAPPLNL, CTAPESTD)
SELECT CRD.SQ_CTAPCDGO.NEXTVAL, m.TPAPCDGO, 1236, m.PLNP, m.PLNL, 1
FROM (
    -- tipo                                        pasivo (DEBE)  liquidacion (HABER)
    SELECT 11 AS TPAPCDGO, 10349 AS PLNP, 10360 AS PLNL FROM DUAL UNION ALL -- CESANTIA PERSONAL
    SELECT  9,             10354,         10362         FROM DUAL UNION ALL -- JUBILACION PERSONAL
    SELECT 24,             10355,         10363         FROM DUAL UNION ALL -- REND. JUBILACION PERSONAL
    SELECT 12,             10350,         10361         FROM DUAL UNION ALL -- REND. CESANTIA PERSONAL
    SELECT 15,             10357,         10363         FROM DUAL UNION ALL -- REND. JUBILACION PATRONAL
    SELECT 23,             10358,         10364         FROM DUAL UNION ALL -- PENSION COMPLEMENTARIA
    SELECT 22,             10359,         10362         FROM DUAL UNION ALL -- JUBILACION RETIRO VOLUNTARIO
    SELECT 13,             10356,         10362         FROM DUAL UNION ALL -- JUBILACION PATRONAL
    SELECT 21,             10353,         10360         FROM DUAL UNION ALL -- CESANTIA RETIRO VOLUNTARIO
    SELECT 16,             10352,         10361         FROM DUAL UNION ALL -- REND. CESANTIA PATRONAL
    SELECT 14,             10351,         10360         FROM DUAL            -- CESANTIA PATRONAL
) m
WHERE NOT EXISTS (
    SELECT 1 FROM CRD.CTAP c
    WHERE  c.TPAPCDGO = m.TPAPCDGO AND c.PJRQCDGO = 1236
);

COMMIT;


-- =====================================================================================
-- 3. CONTROLES POSTERIORES
-- =====================================================================================

-- 3.1 Esperado: 11 filas, con las cuentas legibles. Revisar UNA POR UNA contra
--     docs/logica-negocio/crd/MAPEO-CUENTAS-TIPO-APORTE.md §3.
--     Un tipo apuntado a la cuenta equivocada produce un asiento que CUADRA IGUAL.
SELECT c.CTAPCDGO, c.TPAPCDGO, t.TPAPNMBR AS TIPO_APORTE,
       np.PLNNCNTA AS CUENTA_PASIVO,      np.PLNNNMBR AS NOMBRE_PASIVO,
       nl.PLNNCNTA AS CUENTA_LIQUIDACION, nl.PLNNNMBR AS NOMBRE_LIQUIDACION
FROM   CRD.CTAP c
JOIN   CRD.TPAP t  ON t.TPAPCDGO  = c.TPAPCDGO
JOIN   CNT.PLNN np ON np.PLNNCDGO = c.CTAPPLNP
JOIN   CNT.PLNN nl ON nl.PLNNCDGO = c.CTAPPLNL
WHERE  c.PJRQCDGO = 1236
ORDER  BY t.TPAPNMBR;

-- 3.2 Tipos CON SALDO que quedaron SIN mapear. Esperado: 17, 18, 19, 20 (reservas, por
--     decision) y 1 (pendiente de definicion). Cualquier otro es un olvido.
SELECT a.TPAPCDGO, t.TPAPNMBR, SUM(a.APRTVLRR) AS SALDO
FROM   CRD.APRT a
JOIN   CRD.TPAP t ON t.TPAPCDGO = a.TPAPCDGO
WHERE  NOT EXISTS (SELECT 1 FROM CRD.CTAP c
                   WHERE c.TPAPCDGO = a.TPAPCDGO AND c.PJRQCDGO = 1236)
GROUP  BY a.TPAPCDGO, t.TPAPNMBR
HAVING SUM(a.APRTVLRR) <> 0
ORDER  BY SUM(a.APRTVLRR) DESC;

-- 3.3 Las cuentas de LIQUIDACION distintas. Esperado: 5.
--     Es EXACTAMENTE la cantidad de productos de pago que hay que crear en CXP para el
--     lado de CXP (uno por cuenta, NO uno por tipo de aporte).
SELECT DISTINCT nl.PLNNCDGO, nl.PLNNCNTA, nl.PLNNNMBR
FROM   CRD.CTAP c
JOIN   CNT.PLNN nl ON nl.PLNNCDGO = c.CTAPPLNL
WHERE  c.PJRQCDGO = 1236
ORDER  BY nl.PLNNCNTA;

-- 3.4 El indice quedo en CRD, no en el schema de la sesion. Esperado: 1 fila OWNER='CRD'.
SELECT i.OWNER, i.INDEX_NAME, i.TABLE_NAME, i.STATUS
FROM   ALL_INDEXES i
WHERE  i.TABLE_OWNER = 'CRD' AND i.INDEX_NAME = 'IDX_CTAP_EMPRESA';


-- =====================================================================================
-- 4. REVERSO — comentado a proposito. Leer antes de descomentar.
-- =====================================================================================
-- Solo si el WAR con la resolucion por CTAP no se desplego. Si ya corrio con contabilidad
-- activa, borrar la tabla deja los asientos generados sin forma de explicar de donde salio
-- cada cuenta.
--
-- DROP SEQUENCE CRD.SQ_CTAPCDGO;
-- DROP TABLE CRD.CTAP CASCADE CONSTRAINTS;
-- =====================================================================================

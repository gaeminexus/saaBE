-- =====================================================================================
-- CRD.RVSG — RECEPCION DE VALORES DE SEGURO (sepelio y afines)
-- FECHA: 2026-09-21 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- PARA QUE: cuando un participe fallece, la aseguradora entrega AL FONDO un valor de
-- sepelio que el fondo despues reparte entre los familiares. Hoy no hay donde registrar
-- esa entrada: el dinero llega al banco y no queda ni asentado ni asignado al participe.
-- Esta tabla es el registro de esa recepcion, con el mismo ciclo que un cobro:
-- se registra -> contabilidad aprueba -> recien ahi se genera el asiento y el valor
-- entra al saldo del participe.
--
-- LOS DOS ASIENTOS DEL CICLO COMPLETO (correo de contabilidad, 2026-09-21):
--   RECEPCION: D 1102XX Banco            / H 23909011 Indemnizaciones por pagar a benef.
--   ENTREGA:   D 23909011                / H 1102XX Banco
-- Esta tabla cubre el PRIMERO. El segundo es el pago a los beneficiarios (fase 2).
--
-- SE CALCA DE CRD.CBCR (CobroCredito), que ya tiene el ciclo registro -> aprobacion ->
-- asiento probado en produccion: mismos nombres de campo por etapa (USRG/FCRG, USAP/FCAP,
-- USRC/FCRC/MTRC, USAN/FCAN/MTAN) y el respaldo como RUTA, no como adjunto de CRD.ADJN.
--
-- DISENO: crd/DISENO-BENEFICIARIOS-Y-VALORES-DE-SEGURO.md
-- CONTRATO: crd/API-RECEPCION-VALORES-SEGURO.md
--
-- ⚠️ LA EMPRESA NO ES COLUMNA: se deriva de la cuenta bancaria
-- (CNBC -> PLNN -> PJRQ), igual que derivarEmpresaCobro de CobroCreditoServiceImpl.
-- Guardarla seria una segunda fuente de verdad que puede discrepar en silencio.
--
-- NO EJECUTAR SIN REVISAR. Correr por bloques y revisar la salida de cada uno antes de
-- seguir. SQL PURO: sin comandos SQL*Plus (WHENEVER / SET / DEFINE).
--
-- SOLO EN EL BACKEND: este archivo NO se espeja a saaFE.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS — si alguno no da lo esperado, PARAR
-- =====================================================================================

-- 0.1 El codigo de 4 letras RVSG tiene que estar LIBRE en toda la base, no solo en CRD.
--     Esperado: 0 filas. Si devuelve algo, PARAR: el nombre ya esta tomado por otro
--     equipo o por una tabla que no esta mapeada en Java.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t WHERE t.TABLE_NAME = 'RVSG';

-- 0.2 La secuencia tambien tiene que estar libre. Esperado: 0 filas.
SELECT s.SEQUENCE_OWNER, s.SEQUENCE_NAME FROM ALL_SEQUENCES s
WHERE  s.SEQUENCE_NAME = 'SQ_RVSGCDGO';

-- 0.3 Las tablas de las que depende. Esperado: 5 filas.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  (t.OWNER = 'CRD' AND t.TABLE_NAME IN ('ENTD','TPAP','APRT'))
OR     (t.OWNER = 'TSR' AND t.TABLE_NAME = 'CNBC')
OR     (t.OWNER = 'CNT' AND t.TABLE_NAME = 'ASNT')
ORDER  BY t.OWNER, t.TABLE_NAME;

-- 0.4 ¿El usuario CRD puede referenciar TSR.CNBC y CNT.ASNT? Las FK entre esquemas
--     necesitan REFERENCES concedido, y en esta base ya hubo FK que quedaron comentadas
--     por falta de GRANT (ver el caso TSR.DTCN -> CNT.DTAS en CLAUDE.md).
--     Esperado: 2 filas con PRIVILEGE = 'REFERENCES'.
--     SI FALTAN: no es motivo para parar. Correr igual el bloque 1 y dejar COMENTADAS
--     las dos FK del bloque 1.3 — la tabla funciona sin ellas y el codigo valida las
--     referencias antes de grabar. Pedir el GRANT despues y agregarlas.
SELECT p.GRANTOR, p.TABLE_NAME, p.PRIVILEGE
FROM   ALL_TAB_PRIVS p
WHERE  p.GRANTEE = 'CRD'
AND    p.PRIVILEGE = 'REFERENCES'
AND    ((p.GRANTOR = 'TSR' AND p.TABLE_NAME = 'CNBC')
     OR (p.GRANTOR = 'CNT' AND p.TABLE_NAME = 'ASNT'));


-- =====================================================================================
-- 1. LA TABLA
-- =====================================================================================

CREATE TABLE CRD.RVSG (
    RVSGCDGO NUMBER            NOT NULL,
    ENTDCDGO NUMBER            NOT NULL,
    TPAPCDGO NUMBER            NOT NULL,
    RVSGESTD NUMBER  DEFAULT 1 NOT NULL,
    CNBCCDGO NUMBER            NOT NULL,
    RVSGVLRR NUMBER(18,2)      NOT NULL,
    RVSGFCHA DATE              NOT NULL,
    RVSGRFRN VARCHAR2(100),
    RVSGRTRS VARCHAR2(2000),
    RVSGOBSR VARCHAR2(2000),
    ASNTCDGO NUMBER,
    APRTCDGO NUMBER,
    RVSGUSRG VARCHAR2(50)      NOT NULL,
    RVSGFCRG TIMESTAMP         NOT NULL,
    RVSGUSAP VARCHAR2(50),
    RVSGFCAP TIMESTAMP,
    RVSGUSRC VARCHAR2(50),
    RVSGFCRC TIMESTAMP,
    RVSGMTRC VARCHAR2(2000),
    RVSGUSAN VARCHAR2(50),
    RVSGFCAN TIMESTAMP,
    RVSGMTAN VARCHAR2(2000)
);

-- 1.2 PK y secuencia
ALTER TABLE CRD.RVSG ADD CONSTRAINT PK_RVSG PRIMARY KEY (RVSGCDGO);

CREATE SEQUENCE CRD.SQ_RVSGCDGO START WITH 1 INCREMENT BY 1 NOCACHE;

-- 1.3 Claves foraneas
ALTER TABLE CRD.RVSG ADD CONSTRAINT FK_RVSG_ENTD
    FOREIGN KEY (ENTDCDGO) REFERENCES CRD.ENTD(ENTDCDGO);
ALTER TABLE CRD.RVSG ADD CONSTRAINT FK_RVSG_TPAP
    FOREIGN KEY (TPAPCDGO) REFERENCES CRD.TPAP(TPAPCDGO);
ALTER TABLE CRD.RVSG ADD CONSTRAINT FK_RVSG_APRT
    FOREIGN KEY (APRTCDGO) REFERENCES CRD.APRT(APRTCDGO);

-- ⚠️ ESTAS DOS CRUZAN DE ESQUEMA. Si el control 0.4 no devolvio sus dos filas,
--    COMENTARLAS y seguir: la tabla queda funcional igual.
ALTER TABLE CRD.RVSG ADD CONSTRAINT FK_RVSG_CNBC
    FOREIGN KEY (CNBCCDGO) REFERENCES TSR.CNBC(CNBCCDGO);
ALTER TABLE CRD.RVSG ADD CONSTRAINT FK_RVSG_ASNT
    FOREIGN KEY (ASNTCDGO) REFERENCES CNT.ASNT(ASNTCDGO);

-- 1.4 Indices de consulta: las dos preguntas que hace la pantalla.
CREATE INDEX CRD.IDX_RVSG_ENTIDAD ON CRD.RVSG (ENTDCDGO);
CREATE INDEX CRD.IDX_RVSG_ESTADO  ON CRD.RVSG (RVSGESTD);

-- 1.5 Comentarios
COMMENT ON TABLE  CRD.RVSG           IS 'Recepcion de valores de seguro que el fondo recibe de la aseguradora para entregar a los beneficiarios de un participe fallecido (sepelio, gastos funerarios, seguro de vida, indemnizaciones). Ciclo calcado de CRD.CBCR: se registra, contabilidad aprueba, y RECIEN AHI se genera el asiento D banco / H 2.3.90.90.11 y el valor entra al saldo del participe. El pago a los beneficiarios es un segundo momento (fase 2).';
COMMENT ON COLUMN CRD.RVSG.ENTDCDGO  IS 'Participe fallecido a cuya cuenta entra el valor. El dinero es de sus beneficiarios; el participe es solo el eje por el que se lo administra.';
COMMENT ON COLUMN CRD.RVSG.TPAPCDGO  IS 'Tipo de aporte por el que entra el valor. Su cuenta contable sale de CRD.CTAP (cuenta de pasivo 2.3.90.90.11). NUNCA se quema el codigo del tipo en Java.';
COMMENT ON COLUMN CRD.RVSG.RVSGESTD  IS 'Estado: 1 REGISTRADO (sin asiento y sin tocar el saldo) - 2 APROBADO por contabilidad (con asiento y con el aporte hecho) - 3 RECHAZADO - 4 ANULADO.';
COMMENT ON COLUMN CRD.RVSG.CNBCCDGO  IS 'Cuenta bancaria de ASOPREP donde entro el dinero (TSR.CNBC). De aca se deriva ademas la EMPRESA contable de la operacion, via PLNN -> PJRQ: no hay columna de empresa a proposito, para no tener dos fuentes de verdad que puedan discrepar.';
COMMENT ON COLUMN CRD.RVSG.RVSGRTRS  IS 'RUTA del respaldo digitalizado, igual que CBCRRTRS. NO es un adjunto de CRD.ADJN.';
COMMENT ON COLUMN CRD.RVSG.ASNTCDGO  IS 'Asiento generado al aprobar. NULL mientras esta en estado 1.';
COMMENT ON COLUMN CRD.RVSG.APRTCDGO  IS 'Aporte positivo generado al aprobar. NULL mientras esta en estado 1. Sin esta columna, anular no tendria a que aporte reversar: CRD.APRT es append-only y buscarlo por glosa/fecha/valor seria adivinar.';


-- =====================================================================================
-- 2. CONTROL POSTERIOR — la tabla quedo como se esperaba
-- =====================================================================================

-- 2.1 Las 22 columnas, con sus tipos y su obligatoriedad. Esperado: 22 filas.
--     Revisar que NULLABLE sea 'N' solo en: RVSGCDGO, ENTDCDGO, TPAPCDGO, RVSGESTD,
--     CNBCCDGO, RVSGVLRR, RVSGFCHA, RVSGUSRG, RVSGFCRG.
SELECT c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.DATA_PRECISION, c.DATA_SCALE, c.NULLABLE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'RVSG'
ORDER  BY c.COLUMN_ID;

-- 2.2 Constraints creadas. Esperado: PK_RVSG y las FK que NO se hayan comentado.
SELECT c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.STATUS
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'RVSG'
ORDER  BY c.CONSTRAINT_TYPE, c.CONSTRAINT_NAME;

-- 2.3 La secuencia existe y arranca en 1. Esperado: 1 fila, LAST_NUMBER = 1.
--     No hay PK explicitas insertadas por este script, asi que no hay nada que
--     sincronizar: la aplicacion es la unica que va a escribir en esta tabla.
SELECT s.SEQUENCE_NAME, s.LAST_NUMBER, s.INCREMENT_BY
FROM   ALL_SEQUENCES s
WHERE  s.SEQUENCE_OWNER = 'CRD' AND s.SEQUENCE_NAME = 'SQ_RVSGCDGO';

-- 2.4 La tabla nace vacia. Esperado: 0.
SELECT COUNT(*) AS FILAS_RVSG FROM CRD.RVSG;


-- =====================================================================================
-- 3. REVERSO — COMENTADO A PROPOSITO. Descomentar solo para deshacer este script.
-- =====================================================================================
--
-- ⛔ Borra la tabla y todo lo que tenga dentro. Si ya se registro alguna recepcion
--    aprobada, sus asientos y sus aportes NO se van con esto: hay que anularlos antes
--    desde la aplicacion (POST /rest/rvsg/{id}/anular), o quedan huerfanos.
--
-- DROP TABLE CRD.RVSG CASCADE CONSTRAINTS;
-- DROP SEQUENCE CRD.SQ_RVSGCDGO;

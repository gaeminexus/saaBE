-- =====================================================================================
-- CRD.CBBP — CUENTA BANCARIA DE BENEFICIARIO DEL PARTICIPE (sepelio, fase 2a)
-- FECHA: 2026-09-22 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- PARA QUE: cuando un participe fallece, la aseguradora entrega AL FONDO un valor de
-- sepelio (ya se recibe: CRD.RVSG, script 230) que el fondo debe repartir ENTRE LOS
-- FAMILIARES, segun un porcentaje, a la cuenta bancaria de cada uno y con su certificado
-- bancario digitalizado. Hoy no hay donde guardar eso.
--
-- POR QUE NO SIRVE CRD.CNBP: esa es la cuenta bancaria DEL PROPIO PARTICIPE. No tiene
-- nombre ni cedula, porque el titular es el; y validarCuentaBancariaParticipe exige que
-- la cuenta le pertenezca y este activa. La de un fallecido no sirve.
--
-- UNA FILA = UN BENEFICIARIO CON SU CUENTA. No hacen falta dos tablas: un beneficiario
-- cobra a una sola cuenta. El nombre CBBP (Cuenta Bancaria Beneficiario Participe) queda
-- en paralelo directo con CRD.CNBP (Cuenta Bancaria Participe), que es su equivalente
-- para el titular.
--
-- EL CERTIFICADO BANCARIO NO ES COLUMNA: va en CRD.ADJN con el tipo CRD.TPDJ
-- "CERTIFICADO BANCARIO", igual que CuentaBancariaParticipeServiceImpl.crearConCertificado.
--
-- AUTORIZADO POR EL USUARIO el 2026-09-22. Ver el recuadro §3 de
-- REGISTRO-RESERVAS-EQUIPOS.md: crear una tabla en CRD lo decide el usuario de este equipo.
--
-- DISENO:   crd/DISENO-BENEFICIARIOS-Y-VALORES-DE-SEGURO.md
-- CONTRATO: crd/API-BENEFICIARIOS-PARTICIPE.md
--
-- ALCANCE DE ESTE SCRIPT: SOLO la tabla de beneficiarios (fase 2a). El PAGO a los
-- beneficiarios (fase 2b) NO va en este script: depende de un producto de pago de CXP
-- contra 2.3.90.90.11, que es de otro equipo, y de una decision sobre el asiento de
-- reclasificacion que todavia se esta midiendo. Ver el contrato seccion 7.
--
-- NO EJECUTAR SIN REVISAR. Correr por bloques y revisar la salida de cada uno antes de
-- seguir. SQL PURO: sin comandos SQL*Plus (WHENEVER / SET / DEFINE).
--
-- SOLO EN EL BACKEND: este archivo NO se espeja a saaFE.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS — si alguno no da lo esperado, PARAR
-- =====================================================================================

-- 0.1 El codigo de 4 letras CBBP tiene que estar LIBRE en toda la base, no solo en CRD.
--     Esperado: 0 filas. Si devuelve algo, PARAR: el nombre ya esta tomado por otro
--     equipo o por una tabla que no esta mapeada en Java.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t WHERE t.TABLE_NAME = 'CBBP';

-- 0.2 La secuencia tambien tiene que estar libre. Esperado: 0 filas.
SELECT s.SEQUENCE_OWNER, s.SEQUENCE_NAME FROM ALL_SEQUENCES s
WHERE  s.SEQUENCE_NAME = 'SQ_CBBPCDGO';

-- 0.3 Las tablas de las que depende. Esperado: 3 filas (CRD.ENTD, CRD.ADJN, TSR.BEXT).
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  (t.OWNER = 'CRD' AND t.TABLE_NAME IN ('ENTD','ADJN'))
   OR  (t.OWNER = 'TSR' AND t.TABLE_NAME = 'BEXT');

-- 0.4 Puede CRD referenciar TSR.BEXT? Una FK entre esquemas necesita REFERENCES
--     concedido. En esta base ya hubo FK que quedaron comentadas por falta de GRANT
--     (el caso TSR.DTCN -> CNT.DTAS), PERO el script 232 probo que CRD.RVSG SI pudo
--     crear FK contra TSR.CNBC y CNT.ASNT. O sea que el permiso existe.
--     Esperado: al menos 1 fila. Si devuelve 0, crear la tabla igual y dejar la FK de
--     BEXT comentada (bloque 3.3), anotandolo en el contrato.
SELECT p.GRANTEE, p.OWNER, p.TABLE_NAME, p.PRIVILEGE
FROM   ALL_TAB_PRIVS p
WHERE  p.OWNER = 'TSR' AND p.TABLE_NAME = 'BEXT' AND p.PRIVILEGE = 'REFERENCES';

-- 0.5 EL TIPO DE ADJUNTO "CERTIFICADO BANCARIO" TIENE QUE EXISTIR.
--     Sin esta fila el endpoint que sube el certificado responde
--     TIPO_ADJUNTO_CERTIFICADO_NO_CONFIGURADO en CUALQUIER intento, y la pantalla no va a
--     decir que lo que falta es una fila de catalogo.
--
--     ⚠️ CORREGIDO 2026-09-22, y la correccion importa mas que el control:
--     la primera version de este bloque buscaba con LIKE '%CERTIFICADO%BANCARIO%'. ESTA MAL.
--     El codigo real (TipoAdjuntoDaoServiceImpl.selectByNombre:25-31) hace
--       UPPER(t.nombre) = UPPER(:nombre)  AND  t.estado = Estado.ACTIVO (1)
--     o sea IGUALDAD EXACTA y ademas filtra por estado. Un LIKE habria dado "1 fila, todo
--     bien" con un nombre como 'CERTIFICADO BANCARIO DIGITALIZADO', o con la fila INACTIVA,
--     y el certificado habria fallado igual el primer dia. Un control mas laxo que el codigo
--     que pretende controlar no controla nada: da tranquilidad falsa.
--     Lo levanto el ejecutor BE al programar contra el codigo en vez de contra mi prosa.
--
--     Esperado: EXACTAMENTE 1 fila, con TPDJNMBR = 'CERTIFICADO BANCARIO' (sin sufijos) y
--     TPDJIDST = 1. Si devuelve 0 filas, o el nombre tiene algo mas, o el estado no es 1,
--     PARAR: hay que corregir la fila de catalogo antes de usar la pantalla.
SELECT d.TPDJCDGO, d.TPDJNMBR, d.TPDJIDST
FROM   CRD.TPDJ d
WHERE  UPPER(d.TPDJNMBR) = UPPER('CERTIFICADO BANCARIO')
AND    d.TPDJIDST = 1;

-- 0.5b Diagnostico, para el caso de que el 0.5 devuelva 0 filas: muestra TODO lo que se
--      parezca, con su estado, para ver si el problema es el nombre o el estado.
SELECT d.TPDJCDGO, d.TPDJNMBR, d.TPDJIDST
FROM   CRD.TPDJ d
WHERE  UPPER(d.TPDJNMBR) LIKE '%CERTIFICAD%';

-- 0.6 Foto del antes: la tabla todavia no debe existir, asi que descomentar esta linea
--     tiene que fallar con ORA-00942. Es el control, no un error.
-- SELECT COUNT(*) AS CBBP_ANTES FROM CRD.CBBP;


-- =====================================================================================
-- 1. LA TABLA
-- =====================================================================================

CREATE TABLE CRD.CBBP (
  CBBPCDGO  NUMBER          NOT NULL,   -- PK
  ENTDCDGO  NUMBER          NOT NULL,   -- FK al participe fallecido (CRD.ENTD)
  CBBPNMBR  VARCHAR2(200)   NOT NULL,   -- nombre completo del beneficiario
  CBBPIDNT  VARCHAR2(20)    NOT NULL,   -- cedula / identificacion
  BEXTCDGO  NUMBER          NOT NULL,   -- FK al banco externo (TSR.BEXT)
  CBBPTPCN  NUMBER          NOT NULL,   -- tipo de cuenta: mismo catalogo que CNBPTPCN
  CBBPNMRO  VARCHAR2(100)   NOT NULL,   -- numero de cuenta
  CBBPPRCN  NUMBER(5,2)     NOT NULL,   -- porcentaje que le corresponde (0,01 a 100,00)
  CBBPIDST  NUMBER          NOT NULL,   -- estado: 1 activo, 2 inactivo
  CBBPUSRG  VARCHAR2(50),               -- usuario que lo registro
  CBBPFCRG  DATE                        -- fecha de registro
);

-- CBBPUSRG y CBBPFCRG NO estan en la tabla del diseno: los agrega el arbitro.
-- Esta tabla decide A QUIEN SE LE PAGA PLATA de un fallecido. Saber quien cargo cada
-- beneficiario y cuando no es un lujo: es lo primero que se va a preguntar si un
-- familiar reclama. Son NULLABLE para no romper una carga por SQL si hiciera falta.

COMMENT ON TABLE  CRD.CBBP           IS 'Beneficiarios del participe y la cuenta bancaria a la que cobran. Se usa para repartir el valor de seguro (sepelio) recibido en CRD.RVSG.';
COMMENT ON COLUMN CRD.CBBP.CBBPCDGO  IS 'PK. Secuencia CRD.SQ_CBBPCDGO.';
COMMENT ON COLUMN CRD.CBBP.ENTDCDGO  IS 'Participe (CRD.ENTD) del que este beneficiario cobra.';
COMMENT ON COLUMN CRD.CBBP.CBBPIDNT  IS 'Identificacion del beneficiario. UNICA POR PARTICIPE, no en toda la tabla: la misma persona puede ser beneficiaria de varios participes (un hijo, de su padre y de su madre).';
COMMENT ON COLUMN CRD.CBBP.CBBPPRCN  IS 'Porcentaje del valor que le corresponde. La suma 100 se valida AL PAGAR, no al guardar.';
COMMENT ON COLUMN CRD.CBBP.CBBPIDST  IS 'Estado: 1 activo, 2 inactivo. Un beneficiario inactivo no entra en el reparto.';


-- =====================================================================================
-- 2. SECUENCIA
-- =====================================================================================
-- Arranca en 1: la tabla nace vacia. No hay PK explicitas que sincronizar.
CREATE SEQUENCE CRD.SQ_CBBPCDGO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;


-- =====================================================================================
-- 3. RESTRICCIONES
-- =====================================================================================

-- 3.1 PK
ALTER TABLE CRD.CBBP ADD CONSTRAINT PK_CBBP PRIMARY KEY (CBBPCDGO);

-- 3.2 FK al participe
ALTER TABLE CRD.CBBP ADD CONSTRAINT FK_CBBP_ENTD
  FOREIGN KEY (ENTDCDGO) REFERENCES CRD.ENTD (ENTDCDGO);

-- 3.3 FK al banco externo (cruza de esquema: ver control 0.4)
--     Si el 0.4 devolvio 0 filas, COMENTAR esta sentencia y anotarlo en el contrato.
ALTER TABLE CRD.CBBP ADD CONSTRAINT FK_CBBP_BEXT
  FOREIGN KEY (BEXTCDGO) REFERENCES TSR.BEXT (BEXTCDGO);

-- 3.4 UNICO POR (PARTICIPE, IDENTIFICACION) — NO por identificacion sola.
--     DECISION DEL USUARIO (2026-09-21): la misma cedula puede ser beneficiaria de
--     VARIOS participes. Un hijo es beneficiario del padre y de la madre: caso real y
--     frecuente. Un UNIQUE sobre CBBPIDNT solo seria un defecto que aparece recien
--     cuando muere el segundo progenitor — es decir, en el peor momento posible.
CREATE UNIQUE INDEX CRD.UX_CBBP_PARTICIPE_IDENT ON CRD.CBBP (ENTDCDGO, CBBPIDNT);

-- 3.5 El porcentaje tiene que ser un porcentaje
ALTER TABLE CRD.CBBP ADD CONSTRAINT CK_CBBP_PORCENTAJE
  CHECK (CBBPPRCN > 0 AND CBBPPRCN <= 100);

-- 3.6 Estado acotado
ALTER TABLE CRD.CBBP ADD CONSTRAINT CK_CBBP_ESTADO
  CHECK (CBBPIDST IN (1, 2));


-- =====================================================================================
-- 4. CONTROLES POSTERIORES — leer la salida, no asumir
-- =====================================================================================

-- 4.1 Las 11 columnas, con su tipo y su obligatoriedad. Esperado: 11 filas.
--     El mapeo JPA NO dice si una columna es obligatoria (leccion de H68, VPPCVLSR).
--     La obligatoriedad real la contesta SOLO esta consulta.
SELECT c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.DATA_PRECISION, c.DATA_SCALE, c.NULLABLE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'CBBP'
ORDER  BY c.COLUMN_ID;

-- 4.2 La secuencia. Esperado: 1 fila, LAST_NUMBER = 1.
SELECT s.SEQUENCE_NAME, s.LAST_NUMBER, s.INCREMENT_BY
FROM   ALL_SEQUENCES s
WHERE  s.SEQUENCE_OWNER = 'CRD' AND s.SEQUENCE_NAME = 'SQ_CBBPCDGO';

-- 4.3 El indice unico. Esperado: 2 filas (una por columna), UNIQUENESS = UNIQUE.
SELECT i.INDEX_NAME, i.UNIQUENESS, c.COLUMN_NAME, c.COLUMN_POSITION
FROM   ALL_INDEXES i
JOIN   ALL_IND_COLUMNS c ON c.INDEX_OWNER = i.OWNER AND c.INDEX_NAME = i.INDEX_NAME
WHERE  i.OWNER = 'CRD' AND i.TABLE_NAME = 'CBBP'
ORDER  BY c.COLUMN_POSITION;

-- 4.4 Las restricciones. Esperado: PK_CBBP (P), FK_CBBP_ENTD (R), FK_CBBP_BEXT (R),
--     CK_CBBP_PORCENTAJE (C), CK_CBBP_ESTADO (C), mas los SYS_C de los NOT NULL,
--     que tienen que ser 9: las 9 columnas obligatorias del diseno.
SELECT c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.SEARCH_CONDITION, c.STATUS
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'CBBP'
ORDER  BY c.CONSTRAINT_TYPE, c.CONSTRAINT_NAME;

-- 4.5 La tabla existe y esta vacia. Esperado: 0.
SELECT COUNT(*) AS CBBP_DESPUES FROM CRD.CBBP;


-- =====================================================================================
-- 5. REVERSO — COMENTADO A PROPOSITO. Descomentar SOLO si hay que volver atras.
-- =====================================================================================
-- Si ya se cargaron beneficiarios, esto los BORRA. Exportarlos antes.
--
-- DROP INDEX CRD.UX_CBBP_PARTICIPE_IDENT;
-- DROP TABLE CRD.CBBP CASCADE CONSTRAINTS;
-- DROP SEQUENCE CRD.SQ_CBBPCDGO;

-- =====================================================================================
-- DDL — CERTIFICADOS DE CREDITO (CRD.CRTF) + rubros de parametrizacion
-- FECHA: 2026-08-29
--
-- =====================================================================================
-- ESTADO: NO EJECUTADO. Escrito por el orquestador sobre el modelo propuesto por el
--         agente de certificados y verificado contra la base.
--         Va ANTES del despliegue del WAR: la entidad JPA mapea esta tabla y el arranque
--         de WildFly falla si no existe.
-- =====================================================================================
--
-- QUE RESUELVE
--
-- Seis certificados que hoy se emiten a mano en Word, con numeracion llevada por fuera
-- del sistema. Pasan a emitirse desde la pantalla del participe.
--
-- LA NUMERACION ES EL PUNTO DELICADO, y por eso existe esta tabla.
--
--   El numero impreso es "ASOPREP-FCPC-PARTICIPE-099-2026": una SERIE UNICA POR AÑO,
--   COMPARTIDA entre los seis tipos. Se comprobo en los ejemplos reales: los numeros
--   067, 075, 084, 099, 111 y 118 de 2026 estan intercalados entre tipos distintos, no
--   hay una serie por tipo.
--
--   NO SE USA UNA SECUENCIA DE ORACLE. Dos razones, las dos verificadas, no de estilo:
--     a) Una secuencia no se reinicia por año, y la serie si.
--     b) Una secuencia NO PARTICIPA DEL ROLLBACK: si el PDF falla despues de pedir el
--        NEXTVAL, ese numero queda quemado y la serie muestra un hueco que nadie puede
--        explicar. En un documento firmado y numerado eso no es aceptable.
--
--   El numero se calcula como MAX(CRTFNMRO)+1 dentro del año, bajo LOCK TABLE explicito,
--   en la MISMA transaccion que genera el PDF y hace el INSERT. Si algo falla, la
--   transaccion revierte y el numero nunca existio. UK_CRTF_ANIO_NMRO es la red por si
--   dos usuarios coinciden: uno falla y reintenta, en vez de duplicar un numero.
--
--   ⚠ CONSECUENCIA PARA QUIEN IMPLEMENTE: el .jrxml NO PUEDE CONSULTAR CRD.CRTF. El
--   llenado del reporte usa una conexion JDBC CRUDA (ver la seccion Reportes de
--   CLAUDE.md), que no ve la transaccion abierta — leeria una tabla sin la fila que se
--   acaba de insertar. El numero y todo lo variable viajan como PARAMETROS al reporte.
--
-- POR QUE SE GUARDA EL PDF (CRTFPDFF)
--
-- Para que una reimpresion devuelva EL MISMO documento, bit a bit. La alternativa era
-- regenerarlo desde el snapshot, pero entonces un cambio en la plantilla cambiaria un
-- certificado ya emitido y firmado. El snapshot (CRTFDTOS) se guarda igual, para poder
-- auditar QUE se afirmo y de donde salio cada valor.
--
-- POR QUE HAY CAPTURA MANUAL, y por que se registra cual campo fue capturado
--
-- Medido sobre la base el 2026-08-29: buena parte de lo que estos certificados afirman
-- NO EXISTE EN S.A.A., viene del sistema anterior (DELTA21).
--   - CRD.HPCS (liquidaciones) solo tiene datos desde 2024.
--   - De 3.351 participes cesantes, solo 338 tienen registrados los aportes de cesantia
--     patronal que el certificado 6 afirma.
--   - La fecha de corte de la cuenta de pension complementaria no existe en ninguna
--     columna.
-- Decision del usuario (2026-08-29): el operador captura lo que falte, con precarga de
-- lo que si exista. Por eso CRTFDTOS guarda, campo por campo, si el valor lo puso el
-- sistema o una persona: es la unica forma de saber despues que se afirmo con respaldo
-- y que se afirmo por criterio de quien firmo.
--
-- EJECUCION MANUAL, como owner del esquema CRD (bloques 1-2) y SCP (bloque 3).
-- Todo intra-esquema salvo los rubros: no hace falta ningun GRANT REFERENCES.
--
-- IDEMPOTENCIA: los CREATE y los INSERT fallan si ya se ejecutaron. Es deliberado.
--
-- Contenido:
--   0. Controles PREVIOS
--   1. CREATE CRD.CRTF
--   2. Grants de CRD
--   3. Rubros 243 y 244 (esquema SCP)
--   4. Controles POSTERIORES
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS — ejecutar y leer ANTES de correr el resto
-- =====================================================================================

-- 0.1 La tabla no debe existir todavia. Esperado: 0 filas.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME = 'CRTF';

-- 0.2 Las tablas referenciadas existen y sus PK se llaman como espera la FK.
--     Esperado: 2 filas (ENTD/ENTDCDGO y PRST/PRSTCDGO).
SELECT c.TABLE_NAME, c.CONSTRAINT_NAME, cc.COLUMN_NAME
FROM   ALL_CONSTRAINTS c
JOIN   ALL_CONS_COLUMNS cc ON cc.OWNER = c.OWNER AND cc.CONSTRAINT_NAME = c.CONSTRAINT_NAME
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME IN ('ENTD', 'PRST') AND c.CONSTRAINT_TYPE = 'P'
ORDER  BY c.TABLE_NAME;

-- 0.3 ⚠ LOS ALTERNOS 243 Y 244 DEBEN ESTAR LIBRES. Esperado: 0 filas.
--     Si alguno esta ocupado, PARAR: hay que renumerar aqui Y en com.saa.rubros.Rubros.
SELECT r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR
FROM   SCP.PRBR r WHERE r.PRBRALTR IN (243, 244);

--     ⚠ Y ESTA CONSULTA NO ALCANZA POR SI SOLA. Rubros.java del working tree ya RESERVA
--     alternos que todavia NO tienen fila en SCP.PRBR (238 SUSTENTO_TRIBUTARIO_SRI, 239
--     a 241 de partidas en transito, 242 CRD_GENERACION_POR_FALTANTE), y hay un 249
--     mencionado en el .sql de otro modulo. La base los da por libres y NO lo estan.
--     Verificado el 2026-08-29: 243 y 244 estan libres en los DOS lados.
--     La leccion, en las dos direcciones: la BD no sabe lo que el codigo reservo, y
--     Rubros.java no sabe lo que otro modulo ya inserto. Mirar siempre ambos.

-- 0.4 Los PK explicitos del bloque 3 deben estar libres. Esperado: 0 filas en ambas.
SELECT 'PRBR' AS TABLA, r.PRBRCDGO AS CODIGO_OCUPADO, r.PRBRDSCR AS DESCRIPCION
FROM   SCP.PRBR r WHERE r.PRBRCDGO IN (243, 244)
UNION ALL
SELECT 'PDTR', d.PDTRCDGO, d.PDTRDSCR
FROM   SCP.PDTR d WHERE d.PDTRCDGO BETWEEN 1142 AND 1150;

-- 0.4b ⚠ ORDEN RESPECTO DE 70_CATALOGO_RUBRO_GENERACION_POR_FALTANTE.sql
--      Ese script inserta el rubro 242 con PDTRCDGO = 1141. ESTE script arranca en 1142
--      justamente para no chocar con el. Si por cualquier razon el 70 NO se corrio y no
--      se va a correr, 1141 queda libre y este script funciona igual (deja un hueco en
--      la numeracion de PDTR, que es inofensivo).
--      Lo que NO puede pasar es correr los dos con el mismo codigo: la PK de PDTR lo
--      rechazaria. Esta consulta lo confirma. Esperado: 242 -> 0 o 1 fila, sin error.
SELECT r.PRBRALTR, d.PDTRCDGO, d.PDTRDSCR
FROM   SCP.PRBR r LEFT JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
WHERE  r.PRBRALTR = 242;

-- 0.5 Estado de las secuencias de rubros, para el paso 3.3.
SELECT  'PRBR' AS TABLA,
        (SELECT MAX(p.PRBRCDGO) FROM SCP.PRBR p) AS MAX_PK,
        (SELECT s.LAST_NUMBER FROM ALL_SEQUENCES s
         WHERE s.SEQUENCE_OWNER = 'SCP' AND s.SEQUENCE_NAME = 'SQ_PRBRCDGO') AS SIGUIENTE
FROM    DUAL
UNION ALL
SELECT  'PDTR',
        (SELECT MAX(d.PDTRCDGO) FROM SCP.PDTR d),
        (SELECT s.LAST_NUMBER FROM ALL_SEQUENCES s
         WHERE s.SEQUENCE_OWNER = 'SCP' AND s.SEQUENCE_NAME = 'SQ_PDTRCDGO')
FROM    DUAL;


-- =====================================================================================
-- 1. CREATE TABLE: CRD.CRTF — certificados emitidos
-- =====================================================================================

CREATE TABLE CRD.CRTF (
    CRTFCDGO NUMBER GENERATED BY DEFAULT AS IDENTITY (START WITH 1 INCREMENT BY 1) NOT NULL,
    CRTFANIO NUMBER          NOT NULL,   -- año de la serie, tomado de la fecha de emision
    CRTFNMRO NUMBER          NOT NULL,   -- secuencial DENTRO del año
    CRTFNMAL VARCHAR2(60)    NOT NULL,   -- numero impreso: ASOPREP-FCPC-PARTICIPE-099-2026
    CRTFTPCR NUMBER          NOT NULL,   -- tipo de certificado: alterno del rubro 244
    ENTDCDGO NUMBER          NOT NULL,   -- FK CRD.ENTD, el participe
    PRSTCDGO NUMBER,                     -- FK CRD.PRST; solo el certificado por credito
    CRTFCLDD NUMBER          NOT NULL,   -- calidad IMPRESA (ESPRCDEX). Puede diferir de ENTDIDST
    CRTFFCEM DATE            NOT NULL,   -- fecha de emision impresa en el documento
    CRTFUSEM VARCHAR2(50)    NOT NULL,   -- usuario que emitio
    CRTFDTOS CLOB,                       -- snapshot JSON de lo impreso, con el origen de cada campo
    CRTFPDFF BLOB            NOT NULL,   -- el PDF tal como se emitio
    CRTFESTD NUMBER          DEFAULT 1 NOT NULL,  -- 1 EMITIDO, 2 ANULADO
    CRTFUSAN VARCHAR2(50),               -- usuario de anulacion
    CRTFFCAN TIMESTAMP,                  -- fecha de anulacion
    CRTFMTAN VARCHAR2(500),              -- motivo de anulacion
    CRTFFCRG TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE CRD.CRTF ADD CONSTRAINT PK_CRTF PRIMARY KEY (CRTFCDGO);

-- ⚠ ESTA ES LA GARANTIA DE QUE NO SE REPITE UN NUMERO. El calculo MAX+1 bajo lock evita
--   la carrera; esta constraint la ATRAPA si el lock se omitiera por error en algun
--   camino nuevo. No quitarla aunque "el codigo ya lo controla".
ALTER TABLE CRD.CRTF ADD CONSTRAINT UK_CRTF_ANIO_NMRO UNIQUE (CRTFANIO, CRTFNMRO);

ALTER TABLE CRD.CRTF ADD CONSTRAINT FK_CRTF_ENTD
    FOREIGN KEY (ENTDCDGO) REFERENCES CRD.ENTD(ENTDCDGO);
ALTER TABLE CRD.CRTF ADD CONSTRAINT FK_CRTF_PRST
    FOREIGN KEY (PRSTCDGO) REFERENCES CRD.PRST(PRSTCDGO);

ALTER TABLE CRD.CRTF ADD CONSTRAINT CK_CRTF_ESTD CHECK (CRTFESTD IN (1, 2));
ALTER TABLE CRD.CRTF ADD CONSTRAINT CK_CRTF_ANIO CHECK (CRTFANIO BETWEEN 2000 AND 2999);
ALTER TABLE CRD.CRTF ADD CONSTRAINT CK_CRTF_NMRO CHECK (CRTFNMRO > 0);

-- NOTA: a proposito NO hay un CHECK sobre el rango de CRTFTPCR. El catalogo de tipos es
-- el rubro 244 y VA A CRECER (el usuario ya anticipo condiciones de habilitacion por
-- tipo). Un CHECK con el rango cableado obligaria a un DDL cada vez que se agregue un
-- certificado — el catalogo vive en SCP.PDTR, no en una constraint.

-- El acceso natural es "los certificados de este participe", para la reimpresion.
CREATE INDEX CRD.IDX_CRTF_ENTD ON CRD.CRTF (ENTDCDGO, CRTFESTD);
-- Y el calculo del siguiente numero, que recorre un año.
CREATE INDEX CRD.IDX_CRTF_ANIO ON CRD.CRTF (CRTFANIO, CRTFNMRO);

COMMENT ON TABLE  CRD.CRTF IS
    'Certificados emitidos a participes. Serie unica por año compartida entre todos los tipos. Guarda el PDF para reimprimir el mismo documento.';
COMMENT ON COLUMN CRD.CRTF.CRTFNMRO IS
    'Secuencial dentro del año. Se calcula MAX+1 bajo LOCK TABLE en la misma transaccion que genera el PDF: si el PDF falla, el numero nunca existio. NO usar una secuencia de Oracle: no reinicia por año y no participa del rollback.';
COMMENT ON COLUMN CRD.CRTF.CRTFCLDD IS
    'Calidad del participe EFECTIVAMENTE IMPRESA (ESPRCDEX). Se precarga de ENTD.ENTDIDST pero el operador puede corregirla, y lo que vale para el documento es esto.';
COMMENT ON COLUMN CRD.CRTF.CRTFDTOS IS
    'Snapshot JSON de todo lo impreso, incluyendo POR CAMPO si el valor lo resolvio el sistema o lo capturo el operador. Buena parte de estos datos no existe en S.A.A. (vienen de DELTA21) y esa distincion es la trazabilidad de que se afirmo con respaldo.';
COMMENT ON COLUMN CRD.CRTF.CRTFPDFF IS
    'PDF tal como se emitio. La reimpresion devuelve este binario, no regenera: un cambio de plantilla no puede alterar un documento ya firmado.';


-- =====================================================================================
-- 2. GRANTS
-- =====================================================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON CRD.CRTF TO ROLE_CRD;


-- =====================================================================================
-- 3. RUBROS 243 y 244 — ejecutar CONECTADO AL ESQUEMA SCP
-- =====================================================================================
-- Codigos explicitos, no secuencia, mismo criterio que los rubros 235-237 de la ola de
-- devengo: PRBRCDGO y PRBRALTR COINCIDEN a proposito. La aplicacion resuelve siempre por
-- ALTERNO, nunca por PK.
-- PRBRTPOO se copia del rubro 169, para no inventar un valor.

-- 3.1 Rubro 243 — parametros de los certificados (lo que se puede cambiar sin recompilar)
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (243, 'CRD PARAMETROS CERTIFICADOS', SYSDATE, 243,
        NVL((SELECT MAX(r.PRBRTPOO) FROM SCP.PRBR r WHERE r.PRBRALTR = 169), 1));

-- 3.2 Rubro 244 — catalogo de tipos de certificado
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (244, 'CRD TIPO CERTIFICADO', SYSDATE, 244,
        NVL((SELECT MAX(r.PRBRTPOO) FROM SCP.PRBR r WHERE r.PRBRALTR = 169), 1));

-- 3.3 Detalles. PDTR 1142 a 1150 (el 1141 lo toma el rubro 242, ver el control 0.4b).
--     Los tres del 243 llevan el valor en PDTRVLRV (alfanumerico): son textos que se
--     imprimen. Cambiar de jefe de credito es un UPDATE aqui, NO tocar los reportes ni
--     recompilar ningun .jasper.
INSERT ALL
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1142, 243, 'FIRMANTE DEL CERTIFICADO', NULL, 'Lic. Gabriel Patricio Robayo Rueda', 1, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1143, 243, 'CARGO DEL FIRMANTE', NULL, 'Jefe de Credito', 2, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1144, 243, 'CIUDAD DE EMISION', NULL, 'Quito', 3, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1145, 244, 'AL DIA EN SUS OBLIGACIONES', 1, NULL, 1, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1146, 244, 'HABER RECIBIDO APORTES', 2, NULL, 2, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1147, 244, 'NO ADEUDAR - CREDITO', 3, NULL, 3, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1148, 244, 'NO ADEUDAR - GLOBAL', 4, NULL, 4, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1149, 244, 'LICITUD DE FONDOS DEPOSITADOS', 5, NULL, 5, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1150, 244, 'APORTES PATRONALES SIN JUBILACION MENSUAL', 6, NULL, 6, 1)
SELECT * FROM DUAL;

COMMIT;

-- 3.4 ⚠ SINCRONIZAR LAS SECUENCIAS — obligatorio despues de insertar PK explicitos.
--     Sin esto, el proximo rubro o detalle que cree la aplicacion pedira un NEXTVAL ya
--     ocupado y fallara con ORA-00001. Correr PRIMERO la consulta y ejecutar SOLO la
--     linea cuya secuencia haya quedado en o por debajo del PK usado. Si ya esta
--     adelantada, NO TOCARLA.
SELECT  'SQ_PRBRCDGO' AS SECUENCIA, 244 AS PK_USADO,
        (SELECT s.LAST_NUMBER FROM ALL_SEQUENCES s
         WHERE s.SEQUENCE_OWNER = 'SCP' AND s.SEQUENCE_NAME = 'SQ_PRBRCDGO') AS SIGUIENTE
FROM    DUAL
UNION ALL
SELECT  'SQ_PDTRCDGO', 1150,
        (SELECT s.LAST_NUMBER FROM ALL_SEQUENCES s
         WHERE s.SEQUENCE_OWNER = 'SCP' AND s.SEQUENCE_NAME = 'SQ_PDTRCDGO')
FROM    DUAL;

-- ALTER SEQUENCE SCP.SQ_PRBRCDGO RESTART START WITH 245;
-- ALTER SEQUENCE SCP.SQ_PDTRCDGO RESTART START WITH 1151;


-- =====================================================================================
-- 4. CONTROLES POSTERIORES
-- =====================================================================================

-- 4.1 La tabla existe con sus 17 columnas.
SELECT c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.NULLABLE, c.DATA_DEFAULT
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'CRTF' ORDER BY c.COLUMN_ID;

-- 4.2 Constraints: 1 PK, 1 UNIQUE, 2 FK, 3 CHECK propios (mas los de NOT NULL).
SELECT c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.SEARCH_CONDITION, c.STATUS
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'CRTF'
ORDER  BY c.CONSTRAINT_TYPE, c.CONSTRAINT_NAME;

-- 4.3 Los dos indices, en el schema CRD (no en el del usuario de la sesion).
SELECT i.OWNER, i.INDEX_NAME, i.UNIQUENESS FROM ALL_INDEXES i
WHERE  i.TABLE_OWNER = 'CRD' AND i.TABLE_NAME = 'CRTF' ORDER BY i.INDEX_NAME;

-- 4.4 Los dos rubros y sus detalles. Esperado: 243 -> 3 filas, 244 -> 6 filas.
SELECT  r.PRBRALTR AS RUBRO, r.PRBRDSCR, d.PDTRALTR AS DETALLE, d.PDTRDSCR,
        d.PDTRVLRN, d.PDTRVLRV, d.PDTRESTD
FROM    SCP.PRBR r
LEFT    JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
WHERE   r.PRBRALTR IN (243, 244)
ORDER   BY r.PRBRALTR, d.PDTRALTR;

-- 4.5 La tabla arranca vacia. Esperado: 0.
SELECT COUNT(*) AS CERTIFICADOS_EMITIDOS FROM CRD.CRTF;

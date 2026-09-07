-- =====================================================================================
-- CRD.USAP - credenciales de acceso a la app movil de ASOPREP
-- FECHA: 2026-09-03
--
-- PARA QUE: la app "Asoprep Contigo" deja de usar los servicios de Azure y pasa a
-- consultar el SAA a traves de un WAR de borde (SaaMovilBE) expuesto a internet. SaaBE
-- queda en la intranet y NO se expone. Lo unico que le faltaba a SaaBE para eso es un
-- almacen de credenciales del participe: esta tabla.
--
-- QUE NO ES: no es la tabla de usuarios del sistema (esa es la de intranet, con su propio
-- login). USAP es exclusivamente "este participe puede entrar a la app movil". Un participe
-- sin fila aca simplemente no tiene app; su condicion de participe no cambia en nada.
--
-- RELACION 1:1 CON EL PARTICIPE: ENTDCDGO es UNIQUE, no solo FK. Un participe tiene como
-- maximo una credencial de app. La identificacion tambien es UNIQUE: es el "usuario" del
-- login, no un dato descriptivo.
--
-- LA CLAVE NUNCA SE GUARDA EN CLARO. USAPCLVE guarda un hash PBKDF2WithHmacSHA256 con el
-- formato iteraciones$saltBase64$hashBase64 (100.000 iteraciones, salt de 16 bytes).
-- El formato lleva las iteraciones adelante A PROPOSITO: el dia que haya que subirlas, los
-- hashes viejos se siguen validando con su propio numero y se re-hashean al proximo login.
-- Por eso 256 caracteres y no menos.
--
-- (!) NINGUN ENDPOINT DEVUELVE ESTA COLUMNA. Se verifico dos veces durante el desarrollo:
-- ni por getAll/getId genericos (deliberadamente NO se publicaron para esta entidad, contra
-- el patron estandar de la casa), ni en el body de crear/resetearClave/reactivar (devuelven
-- un DTO sin el hash, no la entidad). Si alguien agrega un endpoint nuevo sobre esta tabla,
-- ESO es lo primero que hay que revisar.
--
-- POR QUE VARCHAR2(20) EN LA IDENTIFICACION Y NO 13: la columna origen CRD.ENTD.ENTDNMID es
-- VARCHAR2(50). Con 13 (cedula + RUC) un participe con pasaporte, o con un dato historico
-- mas largo, no se puede enrolar - y el error aparecia recien en produccion, el dia que
-- alguien lo intentara. 20 cubre cedula (10), RUC (13) y pasaporte sin copiar los 50 de una
-- columna historica ancha. El control 0.4 verifica esto contra los datos REALES antes de
-- crear la tabla.
--
-- POR QUE SECUENCIA Y NO IDENTITY: ESTANDARES-CREACION-TABLAS-ORACLE.md dice IDENTITY, y 87
-- entidades de crd la usan. Pero las tablas nuevas del modulo (CBCR, ACCN, CTAP, VGCN...)
-- usan CRD.SQ_XXXXCDGO con @SequenceGenerator, y ese es el patron vigente. El doc quedo
-- atras de la practica.
-- (!) La entidad DEBE declarar allocationSize = 1. El default de Hibernate es 50 y contra
-- una secuencia INCREMENT BY 1 produce colisiones de PK. Verificado contra CobroCredito.java:41.
--
-- USAPUSAR (usuario de oficina): NO es adorno de auditoria. crear y resetearClave los
-- ejecuta personal de oficina desde la intranet, y sin esta columna un reseteo de la clave
-- de un participe no deja rastro de quien lo hizo. resetearClave y reactivar la
-- SOBREESCRIBEN: el rastro que importa es el del ultimo cambio de credencial, no el del
-- enrolamiento original.
--
-- ESTADOS - por que constantes planas y no catalogo Rubro/DetalleRubro: es estado tecnico de
-- cuenta (activo / bloqueado por intentos / eliminado por el propio participe), no parametria
-- que la oficina vaya a configurar. Van en com.saa.rubros.EstadoUsuarioApp. Ventaja lateral:
-- al no ser FK a catalogo, esta tabla no puede caer en la trampa *IDST vs FK que documenta
-- CLAUDE.md para CRD.ENTD y CRD.PRST.
--
-- ELIMINADO (3) ES REVERSIBLE, PERO SOLO A PROPOSITO: el borrado de cuenta desde la app es
-- requisito de Google Play y App Store. Un participe que borro su cuenta y meses despues
-- vuelve a la oficina se recupera con usap/reactivar, que es un endpoint APARTE.
-- resetearClave (el desbloqueo de rutina) RECHAZA a un usuario eliminado a proposito: asi
-- revertir un borrado es un acto deliberado y auditado en USAPUSAR, y no el efecto lateral
-- invisible de un reseteo cualquiera.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS - si alguno no da lo esperado, PARAR
-- =====================================================================================

-- 0.1 El codigo de 4 letras USAP tiene que estar LIBRE en TODA la base, no solo en CRD.
--     Esperado: 0 filas.
--     (En el repositorio ya se verifico que no existe ninguna entidad ni tabla USAP, pero
--      un grep sobre el codigo NO prueba nada sobre la base de produccion: puede haber una
--      tabla creada a mano que el codigo no mapea. Este control es el que vale.)
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t WHERE t.TABLE_NAME = 'USAP';

-- 0.2 El nombre de la secuencia tiene que estar libre. Esperado: 0 filas.
SELECT s.SEQUENCE_OWNER, s.SEQUENCE_NAME FROM ALL_SEQUENCES s
WHERE  s.SEQUENCE_NAME = 'SQ_USAPCDGO';

-- 0.3 La tabla de la que depende existe. Esperado: 1 fila (CRD.ENTD).
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME = 'ENTD';

-- 0.4 (!) EL CONTROL QUE DECIDE SI VARCHAR2(20) ALCANZA.
--     Mide la identificacion mas larga que existe REALMENTE entre los participes.
--     Esperado: MAX_LARGO <= 20 y EXCEDEN_20 = 0.
--     SI DA MAS DE 20: PARAR. No crear la tabla. Subir USAPIDNT al largo que haga falta
--     (y el @Column(length) de la entidad UsuarioApp con el), o confirmar con el usuario
--     que esos participes no van a usar la app. Crear la tabla corta y descubrirlo despues
--     significa un ALTER sobre una tabla con credenciales ya en uso.
SELECT MAX(LENGTH(TRIM(e.ENTDNMID))) AS MAX_LARGO,
       COUNT(*)                      AS TOTAL_ENTIDADES,
       SUM(CASE WHEN LENGTH(TRIM(e.ENTDNMID)) > 20 THEN 1 ELSE 0 END) AS EXCEDEN_20
FROM   CRD.ENTD e
WHERE  NVL(TRIM(e.ENTDNMID), '0') <> '0';

-- 0.5 Si 0.4 dio EXCEDEN_20 > 0, mirar QUIENES son antes de decidir. Esperado: 0 filas.
SELECT e.ENTDCDGO, e.ENTDNMID, LENGTH(TRIM(e.ENTDNMID)) AS LARGO, e.ENTDRZNS
FROM   CRD.ENTD e
WHERE  LENGTH(TRIM(e.ENTDNMID)) > 20
ORDER  BY LARGO DESC;

-- 0.6 Identificaciones DUPLICADAS entre participes. Esperado: 0 filas.
--     USAPIDNT es UNIQUE. Si en ENTD hay dos participes con la misma identificacion, el
--     segundo que se intente enrolar va a fallar con un error de constraint que nadie va a
--     saber interpretar desde la oficina. Mejor saberlo ahora.
SELECT TRIM(e.ENTDNMID) AS IDENTIFICACION, COUNT(*) AS VECES
FROM   CRD.ENTD e
WHERE  NVL(TRIM(e.ENTDNMID), '0') <> '0'
GROUP  BY TRIM(e.ENTDNMID)
HAVING COUNT(*) > 1
ORDER  BY COUNT(*) DESC;


-- =====================================================================================
-- 1. LA TABLA
-- =====================================================================================

CREATE SEQUENCE CRD.SQ_USAPCDGO START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE CRD.USAP (
    -- PK
    USAPCDGO NUMBER NOT NULL,

    -- FK al participe - 1:1, un participe una credencial
    ENTDCDGO NUMBER NOT NULL,

    -- CREDENCIALES
    USAPIDNT VARCHAR2(20)  NOT NULL,          -- usuario del login (ver control 0.4)
    USAPCLVE VARCHAR2(256) NOT NULL,          -- hash PBKDF2, NUNCA la clave en claro

    -- ESTADO Y BLOQUEO POR INTENTOS
    USAPESTD NUMBER DEFAULT 1 NOT NULL,
    USAPINTF NUMBER DEFAULT 0 NOT NULL,
    USAPBLHS TIMESTAMP,
    USAPDCCL NUMBER DEFAULT 1 NOT NULL,       -- arranca en 1: la clave inicial es temporal

    -- AUDITORIA
    USAPFCRG TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    USAPFCUA TIMESTAMP,
    USAPUSAR VARCHAR2(50),

    -- CONSTRAINTS
    CONSTRAINT PK_USAP      PRIMARY KEY (USAPCDGO),
    CONSTRAINT FK_USAP_ENTD FOREIGN KEY (ENTDCDGO) REFERENCES CRD.ENTD(ENTDCDGO),
    CONSTRAINT UQ_USAP_ENTD UNIQUE (ENTDCDGO),
    CONSTRAINT UQ_USAP_IDNT UNIQUE (USAPIDNT),
    CONSTRAINT CK_USAP_ESTD CHECK (USAPESTD IN (1,2,3)),
    CONSTRAINT CK_USAP_DCCL CHECK (USAPDCCL IN (0,1))
);

-- SIN "CREATE INDEX IDX_USAP_ENTD" A PROPOSITO: el estandar pide indice para toda FK, pero
-- esa regla asume una FK sin UNIQUE. Aca UQ_USAP_ENTD ya crea su indice unico sobre
-- ENTDCDGO; un CREATE INDEX aparte dejaria dos indices sobre la misma columna. Lo mismo
-- vale para USAPIDNT, cubierta por UQ_USAP_IDNT - que es ademas el indice que usa el login.

COMMENT ON TABLE  CRD.USAP          IS 'Credenciales de acceso a la app movil ASOPREP por participe (login = identificacion). NO es la tabla de usuarios del sistema.';
COMMENT ON COLUMN CRD.USAP.USAPCDGO IS 'Codigo (PK)';
COMMENT ON COLUMN CRD.USAP.ENTDCDGO IS 'FK a CRD.ENTD - participe dueno de la credencial (1:1)';
COMMENT ON COLUMN CRD.USAP.USAPIDNT IS 'Identificacion (cedula/RUC/pasaporte) - usuario de login de la app';
COMMENT ON COLUMN CRD.USAP.USAPCLVE IS 'Hash de clave, formato iteraciones$saltBase64$hashBase64 (PBKDF2WithHmacSHA256). NUNCA se expone por REST.';
COMMENT ON COLUMN CRD.USAP.USAPESTD IS 'Estado: 1=Activo 2=Bloqueado(intentos fallidos) 3=Eliminado(por el participe desde la app)';
COMMENT ON COLUMN CRD.USAP.USAPINTF IS 'Intentos fallidos consecutivos de login (se resetea al login exitoso)';
COMMENT ON COLUMN CRD.USAP.USAPBLHS IS 'Fecha/hora hasta la cual esta bloqueado por intentos fallidos (5 fallos = 15 minutos)';
COMMENT ON COLUMN CRD.USAP.USAPDCCL IS 'Flag: 1 = debe cambiar clave en el proximo ingreso, 0 = no';
COMMENT ON COLUMN CRD.USAP.USAPFCRG IS 'Fecha de creacion del registro';
COMMENT ON COLUMN CRD.USAP.USAPFCUA IS 'Fecha/hora del ultimo acceso exitoso';
COMMENT ON COLUMN CRD.USAP.USAPUSAR IS 'Usuario de oficina del ultimo cambio de credencial (crear / resetearClave / reactivar)';


-- =====================================================================================
-- 2. GRANTS
-- =====================================================================================
-- Todo intra-esquema (la FK apunta a CRD.ENTD), asi que no hace falta GRANT REFERENCES.

GRANT SELECT, INSERT, UPDATE, DELETE ON CRD.USAP        TO ROLE_CRD;
GRANT SELECT                         ON CRD.SQ_USAPCDGO TO ROLE_CRD;


-- =====================================================================================
-- 3. CONTROLES POSTERIORES
-- =====================================================================================

-- 3.1 La tabla y la secuencia quedaron en CRD, no en el schema de la sesion.
--     Esperado: 2 filas, las dos con OWNER = 'CRD'.
SELECT 'TABLA'     AS OBJETO, t.OWNER AS PROPIETARIO, t.TABLE_NAME    AS NOMBRE
FROM   ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME = 'USAP'
UNION ALL
SELECT 'SECUENCIA' AS OBJETO, s.SEQUENCE_OWNER, s.SEQUENCE_NAME
FROM   ALL_SEQUENCES s
WHERE  s.SEQUENCE_OWNER = 'CRD' AND s.SEQUENCE_NAME = 'SQ_USAPCDGO';

-- 3.2 Las 6 constraints existen y estan habilitadas. Esperado: 6 filas, todas ENABLED.
--     P=primary key, R=foreign key, U=unique, C=check.
SELECT c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.STATUS, c.SEARCH_CONDITION
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'USAP'
AND    c.CONSTRAINT_NAME IN ('PK_USAP','FK_USAP_ENTD','UQ_USAP_ENTD','UQ_USAP_IDNT',
                             'CK_USAP_ESTD','CK_USAP_DCCL')
ORDER  BY c.CONSTRAINT_TYPE, c.CONSTRAINT_NAME;

-- 3.3 Los largos y NOT NULL quedaron como se esperaba.
--     Esperado: USAPIDNT VARCHAR2(20) N, USAPCLVE VARCHAR2(256) N, USAPUSAR VARCHAR2(50) Y.
SELECT c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.NULLABLE, c.DATA_DEFAULT
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'USAP'
ORDER  BY c.COLUMN_ID;

-- 3.4 (!) EL CONTROL DE SEGURIDAD. Ninguna clave en claro, nunca.
--     Correr DESPUES de que la oficina enrole a los primeros participes.
--     Todo hash valido empieza con las iteraciones y tiene DOS separadores. Esperado: 0 filas.
--     Una fila aca significa que algo escribio una clave sin hashear: PARAR, avisar, y
--     forzar reseteo de todas las credenciales afectadas.
SELECT u.USAPCDGO, u.USAPIDNT, LENGTH(u.USAPCLVE) AS LARGO_HASH
FROM   CRD.USAP u
WHERE  REGEXP_COUNT(u.USAPCLVE, '\$') <> 2
OR     NOT REGEXP_LIKE(u.USAPCLVE, '^[0-9]+\$');

-- 3.5 Distribucion de estados. Sirve de foto para el dia a dia.
--     Muchos en 2 (bloqueado) puede ser gente que olvido la clave... o un ataque de fuerza
--     bruta contra el login. Vale mirarlo de vez en cuando junto con USAPINTF.
SELECT u.USAPESTD AS ESTADO,
       CASE u.USAPESTD WHEN 1 THEN 'Activo' WHEN 2 THEN 'Bloqueado' WHEN 3 THEN 'Eliminado'
            ELSE 'DESCONOCIDO - REVISAR' END AS DESCRIPCION,
       COUNT(*) AS CANTIDAD
FROM   CRD.USAP u
GROUP  BY u.USAPESTD
ORDER  BY u.USAPESTD;


-- =====================================================================================
-- 4. NO HAY CARGA INICIAL - Y ESO ES UN PENDIENTE, NO UN OLVIDO
-- =====================================================================================
-- Esta tabla nace VACIA a proposito: no se pueden inventar claves para los participes.
-- Cada credencial se crea de a una por POST /rest/usap/crear, que ejecuta el personal de
-- oficina desde la intranet, entregandole al participe una clave temporal.
--
-- (!) CONSECUENCIA OPERATIVA: el dia que la app salga a las tiendas, NADIE va a poder entrar
-- hasta que la oficina enrole. Y a la fecha de este script NO EXISTE la pantalla de intranet
-- que llama a crear/resetearClave/reactivar: los endpoints estan, la pantalla no.
-- Eso hay que resolverlo ANTES del lanzamiento, no despues.
--
-- NO cargar credenciales por INSERT directo: el hash lo genera el Service en Java. Un INSERT
-- a mano con una clave en claro la deja permanentemente invalida (nunca va a validar) y
-- ademas viola lo que verifica el control 3.4.


-- =====================================================================================
-- 5. REVERSO - comentado a proposito. Leer antes de descomentar.
-- =====================================================================================
-- Solo si el WAR con los endpoints usap NO se desplego todavia. Si ya hay participes
-- enrolados usando la app, borrar la tabla los deja sin acceso y sin forma de recuperarlo:
-- las claves no se pueden regenerar (son hashes), habria que re-enrolar a todos de a uno.
--
-- DROP SEQUENCE CRD.SQ_USAPCDGO;
-- DROP TABLE CRD.USAP CASCADE CONSTRAINTS;
-- =====================================================================================

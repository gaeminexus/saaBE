-- =====================================================================================
-- DDL — COBROS CON APROBACION DE CONTABILIDAD (CRD.CBCR / CRD.DCBC) + rubros
-- FECHA: 2026-08-29
--
-- =====================================================================================
-- ESTADO: EJECUTADO EN LOCAL el 2026-08-29 (tablas 1-3, grants con ORA-01917 esperado por
--         ROLE_CRD inexistente en local, rubros 245/246 con el rango PDTR corregido —
--         ver control 0.5 mas abajo). Pendiente SOLO en produccion. Va ANTES del
--         despliegue del WAR: las entidades JPA mapean estas tablas.
-- =====================================================================================
--
-- QUE RESUELVE
--
-- Hoy un cobro se aplica EN EL ACTO: el operador registra el pago y el sistema toca los
-- prestamos y los aportes en la misma transaccion. Contabilidad se entera despues, si se
-- entera.
--
-- Decision del usuario (2026-08-29): TODOS los cobros pasan por autorizacion de
-- contabilidad, con el mismo comportamiento que ya tiene la carga del archivo Petro.
-- Tres pasos:
--
--   1. REGISTRO (credito)      Se registra el cobro con su respaldo digitalizado.
--                              Asiento contra la CUENTA TRANSITORIA (2.3.01.15.01).
--                              NO se toca ni un prestamo ni un aporte todavia.
--   2. APROBACION (contabilidad)  Revisa el comprobante y aprueba, o rechaza con motivo.
--   3. PROCESO (credito)       Recien aca se afectan prestamos y aportes, y el asiento
--                              pasa de la transitoria a las cuentas definitivas.
--
-- ALCANCE (decision del usuario): pago de cuota individual y multiple, abono a capital,
-- precancelacion, y registro manual de aportes.
--   ⛔ EL CRUCE DE VALORES / PAGO CON APORTES QUEDA FUERA, y es correcto: ahi no entra
--      plata de afuera, se usa el saldo del propio socio. No hay deposito que conciliar
--      ni comprobante que digitalizar.
--
-- LA IDEA QUE HACE QUE ESTO SEA BARATO: el motor de pago NO SE TOCA.
--
--   El REGISTRO guarda la INTENCION (que prestamo, que monto, que respaldo) en estas dos
--   tablas. El PROCESO reconstruye la Solicitud* correspondiente y llama al metodo que ya
--   existe (pagarCuota, aplicar abono, precancelar, registrar aporte) exactamente como se
--   llama hoy. Todo lo que ya funciona sigue funcionando; lo unico que cambia es CUANDO
--   se invoca.
--
-- ⚠️ POR QUE NO SE LLAMAN CBRO Y DCBR
--
--   El nombre natural, CRD.CBRO, YA ESTA OCUPADO: TSR.CBRO es la entidad Cobro de
--   tesoreria (cobro en ventanilla con cierre de caja). Oracle permitiria las dos por
--   estar en esquemas distintos, pero en este proyecto los codigos de 4 letras se leen
--   como identificadores globales — dos CBRO serian una fuente permanente de confusion.
--   Se usan CBCR (CoBro de CRedito) y DCBC (Detalle de CoBro de Credito).
--
-- LA CUENTA BANCARIA, Y POR QUE NO HAY CAJA
--
--   Verificado con el usuario el 2026-08-29: TODOS los cobros son depositos o
--   transferencias a una cuenta bancaria de la institucion, y la pantalla YA le pide al
--   operador a cual. No hay cobro en efectivo por ventanilla que contabilizar contra caja.
--   Por eso CNBCCDGO es NOT NULL y no existe ninguna columna de caja ni de forma de cobro.
--
--   ⚠️ Si algun dia hiciera falta cobrar en efectivo: TSR.CJAA (CajaFisica) y TSR.CJCN
--      (CajaLogica) estan modeladas con su PlanCuenta y su Empresa, pero NINGUN codigo del
--      sistema genera un asiento de cobro en efectivo — no existe ningun
--      generarAsientoCobroEfectivo. Seria camino nuevo, no algo que se copia. Y en la base
--      hay UNA sola fila en CJAA, "CAJA GAEMISOFT", que por el nombre es dato de fabrica
--      del proveedor, no una caja real configurada.
--
-- IDEMPRESA: no es columna. Se resuelve al vuelo desde CNBCCDGO -> PlanCuenta -> Empresa
-- al armar cada asiento, el mismo camino que ya usa CobroPetroContableService. No hay
-- ningun problema generico de "resolver la empresa desde crd" — ver la correccion del
-- 2026-08-29 en ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §9.3.
--
-- EJECUCION MANUAL, como owner del esquema CRD (bloques 1-3) y SCP (bloque 4).
-- IDEMPOTENCIA: los CREATE y los INSERT fallan si ya se ejecutaron. Es deliberado.
--
-- Contenido:
--   0. Controles PREVIOS
--   1. CREATE CRD.CBCR — cabecera del cobro
--   2. CREATE CRD.DCBC — detalle por prestamo
--   3. Grants
--   4. Rubros 245 y 246 (esquema SCP)
--   5. Controles POSTERIORES
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS — ejecutar y leer ANTES de correr el resto
-- =====================================================================================

-- 0.1 Las tablas nuevas no deben existir. Esperado: 0 filas.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME IN ('CBCR', 'DCBC');

SELECT s.SEQUENCE_OWNER, s.SEQUENCE_NAME FROM ALL_SEQUENCES s
WHERE  s.SEQUENCE_OWNER = 'CRD' AND s.SEQUENCE_NAME IN ('SQ_CBCRCDGO', 'SQ_DCBCCDGO');

-- 0.2 Las tablas referenciadas existen y sus PK se llaman como esperan las FK.
--     Esperado: 5 filas (CNBC/CNBCCDGO en TSR; ENTD, PRST, EVPR, PGAP en CRD).
SELECT c.OWNER, c.TABLE_NAME, cc.COLUMN_NAME
FROM   ALL_CONSTRAINTS c
JOIN   ALL_CONS_COLUMNS cc ON cc.OWNER = c.OWNER AND cc.CONSTRAINT_NAME = c.CONSTRAINT_NAME
WHERE  c.CONSTRAINT_TYPE = 'P'
AND  ( (c.OWNER = 'TSR' AND c.TABLE_NAME = 'CNBC')
    OR (c.OWNER = 'CRD' AND c.TABLE_NAME IN ('ENTD', 'PRST', 'EVPR', 'PGAP')) )
ORDER  BY c.OWNER, c.TABLE_NAME;

-- 0.3 ⚠ GRANT REFERENCES CROSS-SCHEMA — lo corre el OWNER DE TSR, no CRD.
--     CBCR referencia TSR.CNBC. El rol DBA NO habilita REFERENCES: sin este grant la FK
--     del bloque 1 falla con ORA-01031. Ejecutar CONECTADO COMO TSR y volver a CRD.
--
--     GRANT REFERENCES ON TSR.CNBC TO CRD;
--
--     Verificacion (esperado: 1 fila con PRIVILEGE = 'REFERENCES'):
SELECT p.TABLE_NAME, p.PRIVILEGE, p.GRANTEE
FROM   ALL_TAB_PRIVS p
WHERE  p.TABLE_SCHEMA = 'TSR' AND p.TABLE_NAME = 'CNBC'
AND    p.PRIVILEGE = 'REFERENCES' AND p.GRANTEE = 'CRD';

-- 0.4 Los alternos 245 y 246 deben estar libres. Esperado: 0 filas.
--     ⚠ Y ESTA CONSULTA NO ALCANZA POR SI SOLA. Rubros.java reserva alternos que todavia
--     NO tienen fila en SCP.PRBR: 238 (SRI), 239-241 (partidas en transito), 242
--     (generacion por faltante), 243-244 (certificados). La BD los da por libres y NO lo
--     estan. Verificado el 2026-08-29: 245 y 246 estan libres en los DOS lados.
SELECT r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR
FROM   SCP.PRBR r WHERE r.PRBRALTR IN (245, 246);

-- 0.5 Los PK explicitos del bloque 4 deben estar libres. Esperado: 0 filas en ambas.
--     ⚠ EL RANGO DE PDTR ARRANCA EN 1160, CONFIRMADO POR EL USUARIO CONTRA LA BASE REAL
--     el 2026-08-29 — NO en 1151, que era lo que este script asumia contando solo los
--     rubros de crd (1141 del rubro 242, 1142-1150 de certificados). Entre medio hay
--     detalles de OTROS modulos que no estan en ningun script de crd.
--     La leccion, otra vez y en la tercera direccion: para PDTR no alcanza ni mirar
--     Rubros.java ni contar los scripts propios. Preguntar el MAX real de la base.
SELECT 'PRBR' AS TABLA, r.PRBRCDGO AS CODIGO_OCUPADO, r.PRBRDSCR AS DESCRIPCION
FROM   SCP.PRBR r WHERE r.PRBRCDGO IN (245, 246)
UNION ALL
SELECT 'PDTR', d.PDTRCDGO, d.PDTRDSCR
FROM   SCP.PDTR d WHERE d.PDTRCDGO BETWEEN 1160 AND 1168;

-- 0.6 Estado de las secuencias de rubros, para el paso 4.4.
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
-- 1. CREATE TABLE: CRD.CBCR — cabecera del cobro
-- =====================================================================================
-- Un registro por OPERACION, no por prestamo: el cobro multiple es UN CBCR con N filas
-- en DCBC. Una sola aprobacion, un solo proceso, un solo comprobante.
-- =====================================================================================

CREATE SEQUENCE CRD.SQ_CBCRCDGO START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE CRD.CBCR (
    CBCRCDGO NUMBER          NOT NULL,   -- PK
    ENTDCDGO NUMBER          NOT NULL,   -- FK CRD.ENTD, el participe que paga
    CBCRTPOO VARCHAR2(30)    NOT NULL,   -- tipo de operacion; ver el COMMENT
    CBCRESTD NUMBER          NOT NULL,   -- estado; rubro 246
    CNBCCDGO NUMBER          NOT NULL,   -- FK TSR.CNBC, cuenta donde entro el dinero
    CBCRRFRN VARCHAR2(100),              -- referencia de la transferencia o deposito
    CBCRRTRS VARCHAR2(2000)  NOT NULL,   -- ruta del respaldo digitalizado. OBLIGATORIO
    CBCRVLRR NUMBER(18,2)    NOT NULL,   -- valor total del cobro
    CBCRFCHA DATE            NOT NULL,   -- fecha del cobro (la del deposito, no la de captura)
    CBCROBSR VARCHAR2(2000),
    CBCRUSRG VARCHAR2(50)    NOT NULL,   -- usuario que registro
    CBCRFCRG TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CBCRUSAP VARCHAR2(50),               -- usuario de contabilidad que aprobo
    CBCRFCAP TIMESTAMP,                  -- fecha de aprobacion
    CBCRUSRC VARCHAR2(50),               -- usuario del ULTIMO rechazo
    CBCRFCRC TIMESTAMP,                  -- fecha del ultimo rechazo
    CBCRMTRC VARCHAR2(2000),             -- motivo del ultimo rechazo
    CBCRUSPR VARCHAR2(50),               -- usuario que proceso
    CBCRFCPR TIMESTAMP,                  -- fecha de proceso
    CBCRUSAN VARCHAR2(50),               -- usuario de credito que ANULO
    CBCRFCAN TIMESTAMP,                  -- fecha de anulacion
    CBCRMTAN VARCHAR2(2000),             -- motivo de anulacion. Obligatorio al anular, ver CK_CBCR_MTAN
    CBCRASN1 NUMBER,                     -- FK CNT.ASNT: asiento TRANSITORIO (registro)
    CBCRASN2 NUMBER                      -- FK CNT.ASNT: asiento DEFINITIVO (proceso)
);

ALTER TABLE CRD.CBCR ADD CONSTRAINT PK_CBCR PRIMARY KEY (CBCRCDGO);

ALTER TABLE CRD.CBCR ADD CONSTRAINT FK_CBCR_ENTD
    FOREIGN KEY (ENTDCDGO) REFERENCES CRD.ENTD(ENTDCDGO);
ALTER TABLE CRD.CBCR ADD CONSTRAINT FK_CBCR_CNBC
    FOREIGN KEY (CNBCCDGO) REFERENCES TSR.CNBC(CNBCCDGO);
ALTER TABLE CRD.CBCR ADD CONSTRAINT FK_CBCR_ASN1
    FOREIGN KEY (CBCRASN1) REFERENCES CNT.ASNT(ASNTCDGO);
ALTER TABLE CRD.CBCR ADD CONSTRAINT FK_CBCR_ASN2
    FOREIGN KEY (CBCRASN2) REFERENCES CNT.ASNT(ASNTCDGO);

ALTER TABLE CRD.CBCR ADD CONSTRAINT CK_CBCR_ESTD CHECK (CBCRESTD IN (1, 2, 3, 4, 5));
ALTER TABLE CRD.CBCR ADD CONSTRAINT CK_CBCR_VLRR CHECK (CBCRVLRR > 0);

-- ⚠ EL MOTIVO DE ANULACION ES OBLIGATORIO CUANDO EL COBRO ESTA ANULADO, y lo garantiza la
--   base, no solo la validacion de Java. Anular un cobro significa afirmar que el dinero
--   NUNCA ENTRO y reversar su asiento: sin el motivo escrito, nadie puede reconstruir
--   despues por que se dio de baja plata que el sistema decia haber recibido.
--   Mismo criterio que CK_CBCR_RTRS: el TRIM cubre la cadena vacia, que no es NULL y
--   pasaria igual con un simple NOT NULL.
ALTER TABLE CRD.CBCR ADD CONSTRAINT CK_CBCR_MTAN
    CHECK (CBCRESTD <> 5 OR TRIM(CBCRMTAN) IS NOT NULL);

-- ⚠ EL RESPALDO ES OBLIGATORIO Y NO PUEDE SER UNA CADENA VACIA. El NOT NULL solo no
--   alcanza: '' distinto de NULL, y un campo en blanco pasaria igual. Decision del
--   usuario: sin respaldo NO se registra el cobro.
ALTER TABLE CRD.CBCR ADD CONSTRAINT CK_CBCR_RTRS CHECK (TRIM(CBCRRTRS) IS NOT NULL);

-- La bandeja de contabilidad lista por estado; la de credito, por estado APROBADO.
CREATE INDEX CRD.IDX_CBCR_ESTADO ON CRD.CBCR (CBCRESTD, CBCRFCHA);
-- "los cobros de este participe", para la ficha.
CREATE INDEX CRD.IDX_CBCR_ENTIDAD ON CRD.CBCR (ENTDCDGO, CBCRESTD);

COMMENT ON TABLE  CRD.CBCR IS
    'Cobro recibido, pendiente de aprobacion de contabilidad. Guarda la INTENCION: al procesar se reconstruye la Solicitud y se llama al motor de pago existente.';
COMMENT ON COLUMN CRD.CBCR.CBCRTPOO IS
    'Tipo de operacion: PAGO_CUOTA, PAGO_MULTIPLE, ABONO_CAPITAL, PRECANCELACION, REGISTRO_APORTE. Decide que metodo del motor se invoca al procesar.';
COMMENT ON COLUMN CRD.CBCR.CBCRESTD IS
    'Rubro 246: 1 REGISTRADO, 2 APROBADO, 3 PROCESADO, 4 RECHAZADO. RECHAZADO vuelve a REGISTRADO cuando credito corrige y reenvia (mismo registro, no uno nuevo).';
COMMENT ON COLUMN CRD.CBCR.CNBCCDGO IS
    'Cuenta bancaria donde entro el dinero. NOT NULL: todos los cobros son deposito o transferencia (no hay cobro en efectivo). De aqui sale el DEBE del asiento transitorio y tambien la empresa, via PlanCuenta.';
COMMENT ON COLUMN CRD.CBCR.CBCRRTRS IS
    'Ruta del comprobante digitalizado (PDF o imagen), subido con FileService. Obligatorio: sin respaldo contabilidad no puede aprobar.';
COMMENT ON COLUMN CRD.CBCR.CBCRMTRC IS
    'Motivo del ULTIMO rechazo. Se sobreescribe si se rechaza de nuevo: no hay historial de rechazos sucesivos, es deliberado. Si algun dia hace falta, es una tabla aparte que no rompe esto.';
COMMENT ON COLUMN CRD.CBCR.CBCRASN1 IS
    'Asiento del REGISTRO: D cuenta contable del banco -> H 2.3.01.15.01 transitoria. Plantilla alterno 19, la misma de Petro. NULL significa SIEMPRE "se registro con la contabilidad apagada": el registro y el asiento van en la MISMA transaccion, asi que un fallo del asiento revierte tambien el cobro y nunca deja una fila con CBCRASN1 nulo. Verificado el 2026-08-29 (toda la cadena es REQUIRED; el unico REQUIRES_NEW es la lectura del flag, que no escribe nada). Si alguien rompiera esa atomicidad, este NULL pasaria a tener dos significados indistinguibles.';
COMMENT ON COLUMN CRD.CBCR.CBCRASN2 IS
    'Asiento del PROCESO: D 2.3.01.15.01 transitoria -> H cuentas definitivas.';


-- =====================================================================================
-- 2. CREATE TABLE: CRD.DCBC — detalle por prestamo
-- =====================================================================================
-- Una fila por prestamo dentro del cobro. Para REGISTRO_APORTE va UNA fila con
-- PRSTCDGO NULL: el aporte es de la entidad, no de un prestamo.
--
-- Las tres columnas especificas por tipo (modalidad, tipo de aporte, periodo de devengo)
-- son nullable A PROPOSITO: se llenan solo cuando el tipo de operacion las necesita.
-- Se eligieron COLUMNAS EXPLICITAS y no un JSON serializado: esta tabla es el rastro de
-- la plata que entro, y alguien la va a consultar a mano justo cuando algo no cuadre —
-- un CLOB con JSON adentro es ilegible en ese momento.
-- =====================================================================================

CREATE SEQUENCE CRD.SQ_DCBCCDGO START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE CRD.DCBC (
    DCBCCDGO NUMBER          NOT NULL,   -- PK
    CBCRCDGO NUMBER          NOT NULL,   -- FK CRD.CBCR
    PRSTCDGO NUMBER,                     -- FK CRD.PRST; NULL solo en REGISTRO_APORTE
    DCBCVLRR NUMBER(18,2)    NOT NULL,   -- monto de ESTA linea
    DCBCMDLD NUMBER,                     -- modalidad; solo ABONO_CAPITAL (1 o 2)
    TPAPCDGO NUMBER,                     -- FK CRD.TPAP; solo REGISTRO_APORTE
    DCBCPRDV DATE,                       -- periodo de devengo; solo REGISTRO_APORTE
    EVPRCDGO NUMBER,                     -- FK CRD.EVPR: evento generado AL PROCESAR
    PGAPCDGO NUMBER,                     -- FK CRD.PGAP: pago de aporte generado al procesar
    DCBCOBSR VARCHAR2(2000)
);

ALTER TABLE CRD.DCBC ADD CONSTRAINT PK_DCBC PRIMARY KEY (DCBCCDGO);

ALTER TABLE CRD.DCBC ADD CONSTRAINT FK_DCBC_CBCR
    FOREIGN KEY (CBCRCDGO) REFERENCES CRD.CBCR(CBCRCDGO);
ALTER TABLE CRD.DCBC ADD CONSTRAINT FK_DCBC_PRST
    FOREIGN KEY (PRSTCDGO) REFERENCES CRD.PRST(PRSTCDGO);
ALTER TABLE CRD.DCBC ADD CONSTRAINT FK_DCBC_TPAP
    FOREIGN KEY (TPAPCDGO) REFERENCES CRD.TPAP(TPAPCDGO);
ALTER TABLE CRD.DCBC ADD CONSTRAINT FK_DCBC_EVPR
    FOREIGN KEY (EVPRCDGO) REFERENCES CRD.EVPR(EVPRCDGO);
ALTER TABLE CRD.DCBC ADD CONSTRAINT FK_DCBC_PGAP
    FOREIGN KEY (PGAPCDGO) REFERENCES CRD.PGAP(PGAPCDGO);

ALTER TABLE CRD.DCBC ADD CONSTRAINT CK_DCBC_VLRR CHECK (DCBCVLRR > 0);
ALTER TABLE CRD.DCBC ADD CONSTRAINT CK_DCBC_MDLD CHECK (DCBCMDLD IS NULL OR DCBCMDLD IN (1, 2));

-- El acceso natural es "las lineas de este cobro".
CREATE INDEX CRD.IDX_DCBC_COBRO ON CRD.DCBC (CBCRCDGO);
-- Y "que cobros tocaron este prestamo", para la ficha del credito.
CREATE INDEX CRD.IDX_DCBC_PRESTAMO ON CRD.DCBC (PRSTCDGO);

COMMENT ON TABLE  CRD.DCBC IS
    'Detalle del cobro: una fila por prestamo. El cobro multiple es UN CBCR con N filas aca. REGISTRO_APORTE lleva una sola fila con PRSTCDGO nulo.';
COMMENT ON COLUMN CRD.DCBC.EVPRCDGO IS
    'EventoPrestamo generado al PROCESAR esta linea. NULL mientras el cobro no se haya procesado. Es el enlace para auditar y para reversar por la via existente (anularOperacion).';
COMMENT ON COLUMN CRD.DCBC.DCBCMDLD IS
    'Modalidad del abono a capital (1 o 2). NULL en los demas tipos de operacion, a proposito.';


-- =====================================================================================
-- 3. GRANTS
-- =====================================================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON CRD.CBCR TO ROLE_CRD;
GRANT SELECT, INSERT, UPDATE, DELETE ON CRD.DCBC TO ROLE_CRD;
GRANT SELECT ON CRD.SQ_CBCRCDGO TO ROLE_CRD;
GRANT SELECT ON CRD.SQ_DCBCCDGO TO ROLE_CRD;


-- =====================================================================================
-- 4. RUBROS 245 y 246 — ejecutar CONECTADO AL ESQUEMA SCP
-- =====================================================================================
-- Codigos explicitos, PRBRCDGO = PRBRALTR, mismo criterio que 235-237 y 243-244.
-- La aplicacion resuelve siempre por ALTERNO.

-- 4.1 Rubro 245 — tipo de operacion del cobro
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (245, 'CRD TIPO OPERACION COBRO', SYSDATE, 245,
        NVL((SELECT MAX(r.PRBRTPOO) FROM SCP.PRBR r WHERE r.PRBRALTR = 169), 1));

-- 4.2 Rubro 246 — estado del cobro
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (246, 'CRD ESTADO COBRO', SYSDATE, 246,
        NVL((SELECT MAX(r.PRBRTPOO) FROM SCP.PRBR r WHERE r.PRBRALTR = 169), 1));

-- 4.3 Detalles. PDTR 1160 a 1168 (confirmado por el usuario: el siguiente libre es 1160).
INSERT ALL
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1160, 245, 'PAGO DE CUOTA',        1, 'PAGO_CUOTA',      1, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1161, 245, 'PAGO MULTIPLE',        2, 'PAGO_MULTIPLE',   2, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1162, 245, 'ABONO A CAPITAL',      3, 'ABONO_CAPITAL',   3, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1163, 245, 'PRECANCELACION',       4, 'PRECANCELACION',  4, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1164, 245, 'REGISTRO DE APORTE',   5, 'REGISTRO_APORTE', 5, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1165, 246, 'REGISTRADO',           1, NULL, 1, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1166, 246, 'APROBADO',             2, NULL, 2, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1167, 246, 'PROCESADO',            3, NULL, 3, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1168, 246, 'RECHAZADO',            4, NULL, 4, 1)
SELECT * FROM DUAL;

COMMIT;

-- 4.4 ⚠ SINCRONIZAR LAS SECUENCIAS — obligatorio despues de insertar PK explicitos.
--     Correr PRIMERO la consulta y ejecutar SOLO la linea cuya secuencia haya quedado en
--     o por debajo del PK usado. Si ya esta adelantada, NO TOCARLA.
SELECT  'SQ_PRBRCDGO' AS SECUENCIA, 246 AS PK_USADO,
        (SELECT s.LAST_NUMBER FROM ALL_SEQUENCES s
         WHERE s.SEQUENCE_OWNER = 'SCP' AND s.SEQUENCE_NAME = 'SQ_PRBRCDGO') AS SIGUIENTE
FROM    DUAL
UNION ALL
SELECT  'SQ_PDTRCDGO', 1168,
        (SELECT s.LAST_NUMBER FROM ALL_SEQUENCES s
         WHERE s.SEQUENCE_OWNER = 'SCP' AND s.SEQUENCE_NAME = 'SQ_PDTRCDGO')
FROM    DUAL;

-- ALTER SEQUENCE SCP.SQ_PRBRCDGO RESTART START WITH 247;
-- ALTER SEQUENCE SCP.SQ_PDTRCDGO RESTART START WITH 1169;


-- =====================================================================================
-- 5. CONTROLES POSTERIORES
-- =====================================================================================

-- 5.1 Las dos tablas existen.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME IN ('CBCR', 'DCBC') ORDER BY t.TABLE_NAME;

-- 5.2 Constraints. CBCR: 1 PK + 4 FK + 3 CHECK propios. DCBC: 1 PK + 5 FK + 2 CHECK.
SELECT c.TABLE_NAME, c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.SEARCH_CONDITION, c.STATUS
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME IN ('CBCR', 'DCBC')
ORDER  BY c.TABLE_NAME, c.CONSTRAINT_TYPE, c.CONSTRAINT_NAME;

-- 5.3 Los cuatro indices, en el schema CRD (no en el del usuario de la sesion).
SELECT i.OWNER, i.TABLE_NAME, i.INDEX_NAME FROM ALL_INDEXES i
WHERE  i.TABLE_OWNER = 'CRD' AND i.TABLE_NAME IN ('CBCR', 'DCBC')
ORDER  BY i.TABLE_NAME, i.INDEX_NAME;

-- 5.4 Los dos rubros y sus detalles. Esperado: 245 -> 5 filas, 246 -> 4 filas.
SELECT  r.PRBRALTR AS RUBRO, r.PRBRDSCR, d.PDTRALTR AS DETALLE, d.PDTRDSCR,
        d.PDTRVLRN, d.PDTRVLRV, d.PDTRESTD
FROM    SCP.PRBR r
LEFT    JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
WHERE   r.PRBRALTR IN (245, 246)
ORDER   BY r.PRBRALTR, d.PDTRALTR;

-- 5.5 Las tablas arrancan vacias. Esperado: 0 en ambas.
SELECT 'CBCR' AS TABLA, COUNT(*) AS FILAS FROM CRD.CBCR
UNION ALL
SELECT 'DCBC', COUNT(*) FROM CRD.DCBC;

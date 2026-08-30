-- =====================================================================================
-- DDL — ACUERDOS DE PAGO CON CONDONACION (CRD.ACCN / CRD.DACC) + integracion con CBCR + rubros
-- FECHA: 2026-08-29
--
-- ESTADO: EJECUTADO EN LOCAL el 2026-08-29 por el agente de backend. Pendiente SOLO en
--         produccion. Ver docs/logica-negocio/crd/PLAN-ACUERDOS-PAGO-CONDONACION.md para el
--         diseno completo (decisiones K1-K10, seccion 5 de integracion con CBCR).
--
-- Nombres de tabla NO estaban decididos en el plan (seccion 6) — elegidos por el agente de
-- backend, verificados sin colision en TODO el proyecto (Java + BD, los 8 esquemas), no solo
-- crd: ACCN (ACuerdo de CoNdonacion) y DACC (Detalle de ACuerdo de Condonacion, mismo patron
-- que CBCR/DCBC: D + 3 primeras letras de la cabecera).
--
-- ⚠️ LA CUENTA DE GASTO DE LA PLANTILLA 25 NO ESTA EN ESTE SCRIPT A PROPOSITO — decision del
-- usuario (plan seccion 6.2): "ninguna cuenta provisional entra en un script". El flag de
-- contabilidad de CRD (rubro 237) esta en 0, asi que el flujo funciona sin ella. El dia que
-- el usuario defina la cuenta, hace falta un INSERT en CNT.DTPL para la plantilla alterno 25
-- ANTES de encender el flag — si no, los acuerdos van a fallar al contabilizar.
--
-- =====================================================================================
-- ⛔ PRERREQUISITO DURO — ORDEN DE EJECUCION
-- =====================================================================================
-- ESTE SCRIPT NO CORRE SOLO. Necesita que YA se haya corrido, en la MISMA base:
--
--     docs/logica-negocio/crd/sql/DDL-COBROS-APROBACION-CONTABILIDAD.sql
--
-- Porque referencia CRD.CBCR (FK_ACCN_CBCR, bloque 1), altera CRD.DCBC (bloque 3) y agrega
-- un detalle al rubro 245 (bloque 5.3) — las tres cosas las crea ese script.
--
-- En LOCAL ya esta corrido. En PRODUCCION, al 2026-08-29, NO. Si se corre este primero,
-- falla a mitad de camino DEJANDO CREADAS la secuencia SQ_ACCNCDGO y la tabla ACCN: hay que
-- borrarlas a mano antes de reintentar. Correr el control 0.0 y no seguir si da 0 filas.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS
-- =====================================================================================

-- 0.0 ⛔ PRERREQUISITO. Esperado: 2 filas (CBCR y DCBC). Si da 0 o 1, PARAR: falta correr
--     DDL-COBROS-APROBACION-CONTABILIDAD.sql en esta base.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME IN ('CBCR', 'DCBC');

-- 0.1 Las tablas nuevas no deben existir.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  t.TABLE_NAME IN ('ACCN', 'DACC');

-- 0.3 El CHECK de EVPR que el bloque 4 va a DROPEAR y recrear. Mirar la condicion ANTES de
--     tocarla: si ya trae valores que este script no repite, el ADD posterior los perderia
--     (falla al validar si hay filas con ese valor, pero si no hay filas se pierde en
--     silencio y la operacion deja de poder registrarse).
--     Esperado: los 4 valores PAGO_MANUAL / PAGO_APORTES / ABONO_CAPITAL / PRECANCELACION.
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION, c.STATUS
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'EVPR' AND c.CONSTRAINT_TYPE = 'C';

-- 0.4 Y que valores hay REALMENTE en la tabla, que es lo que el ADD tiene que aceptar.
SELECT e.EVPRTPOO, COUNT(*) AS FILAS FROM CRD.EVPR e GROUP BY e.EVPRTPOO ORDER BY 1;

-- 0.2 PDTRCDGO/PRBRCDGO libres — VOLVER A CORRER esto mismo (MAX real) justo antes de
-- ejecutar el bloque 4: el rango pudo cambiar entre la escritura de este script y su
-- ejecucion (ya paso una vez con el rango de 245/246, ver DDL-COBROS-APROBACION-CONTABILIDAD.sql).
SELECT MAX(PRBRCDGO) AS MAX_PRBR FROM SCP.PRBR;
SELECT MAX(PDTRCDGO) AS MAX_PDTR FROM SCP.PDTR;


-- =====================================================================================
-- 1. CREATE TABLE: CRD.ACCN — cabecera del acuerdo
-- =====================================================================================
-- La cabecera de la OPERACION APLICADA (pago + condonacion + CANCELADO) reusa EventoPrestamo
-- (K8) — EVPRCDGO se llena recien al PROCESAR, vease seccion 5 del plan. Esta tabla es el
-- ciclo de vida PREVIO: registro del acuerdo y aprobacion/rechazo de la condonacion, que
-- ocurre ANTES de que exista ningun EventoPrestamo.
-- =====================================================================================

CREATE SEQUENCE CRD.SQ_ACCNCDGO START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE CRD.ACCN (
    ACCNCDGO NUMBER          NOT NULL,   -- PK
    ENTDCDGO NUMBER          NOT NULL,   -- FK CRD.ENTD, el participe
    PRSTCDGO NUMBER          NOT NULL,   -- FK CRD.PRST, el prestamo (EN_MORA u DE_PLAZO_VENCIDO, K7)
    ACCNESTD NUMBER          NOT NULL,   -- estado; rubro 247 (1 VIGENTE, 2 APLICADO, 3 ANULADO)
    ACCNVLPG NUMBER(18,2)    NOT NULL,   -- valor total a pagar (la parte NO condonada, el "piso")
    ACCNVLCN NUMBER(18,2)    NOT NULL,   -- valor total a condonar
    ACCNFCHA DATE            NOT NULL,   -- fecha de negocio del acuerdo
    ACCNOBSR VARCHAR2(2000),
    ACCNUSRG VARCHAR2(50)    NOT NULL,   -- usuario que registra
    ACCNFCRG TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ACCNUSAP VARCHAR2(50),               -- usuario (segundo, K4) que aprueba la condonacion
    ACCNFCAP TIMESTAMP,
    -- Los tres de abajo eran del RECHAZO de la condonacion (K10). Al derogarse K4/K10 el
    -- 2026-08-30 pasaron a ser los de la ANULACION, que hoy llega SIEMPRE en cascada desde la
    -- anulacion del CBCR. Se copian de ahi, no se piden aparte.
    ACCNUSRC VARCHAR2(50),               -- usuario que anula (copiado de CBCRUSAN)
    ACCNFCRC TIMESTAMP,                  -- fecha de anulacion (copiada de CBCRFCAN)
    ACCNMTRC VARCHAR2(2000),             -- motivo de la anulacion (copiado de CBCRMTAN)
    EVPRCDGO NUMBER,                     -- FK CRD.EVPR: se llena SOLO al procesar via CBCR (K8)
    CBCRCDGO NUMBER                      -- FK CRD.CBCR: el cobro de la parte pagada (seccion 5)
);

ALTER TABLE CRD.ACCN ADD CONSTRAINT PK_ACCN PRIMARY KEY (ACCNCDGO);
ALTER TABLE CRD.ACCN ADD CONSTRAINT FK_ACCN_ENTD FOREIGN KEY (ENTDCDGO) REFERENCES CRD.ENTD(ENTDCDGO);
ALTER TABLE CRD.ACCN ADD CONSTRAINT FK_ACCN_PRST FOREIGN KEY (PRSTCDGO) REFERENCES CRD.PRST(PRSTCDGO);
ALTER TABLE CRD.ACCN ADD CONSTRAINT FK_ACCN_EVPR FOREIGN KEY (EVPRCDGO) REFERENCES CRD.EVPR(EVPRCDGO);
-- FK_ACCN_CBCR se agrega en el bloque 3, DESPUES de que exista la columna en DCBC/CBCR ya
-- corrido (CBCR ya existe desde el proyecto de cobros, esto es solo referencial).
ALTER TABLE CRD.ACCN ADD CONSTRAINT FK_ACCN_CBCR FOREIGN KEY (CBCRCDGO) REFERENCES CRD.CBCR(CBCRCDGO);

ALTER TABLE CRD.ACCN ADD CONSTRAINT CK_ACCN_ESTD CHECK (ACCNESTD IN (1, 2, 3));
ALTER TABLE CRD.ACCN ADD CONSTRAINT CK_ACCN_VLPG CHECK (ACCNVLPG >= 0);
ALTER TABLE CRD.ACCN ADD CONSTRAINT CK_ACCN_VLCN CHECK (ACCNVLCN >= 0);
-- Un acuerdo ANULADO conserva su registro, y sin motivo ese registro no informa nada. Mismo
-- criterio que CK_CBCR_MTAN en DDL-COBROS-APROBACION-CONTABILIDAD.sql.
--
-- ⚠️ ESTE CHECK EXIGE QUE LA CASCADA COPIE EL MOTIVO. La anulacion del acuerdo llega SIEMPRE
-- desde la anulacion del CBCR, que ya trae usuario/fecha/motivo obligatorios. Si la cascada
-- pasa el acuerdo a ANULADO(3) sin copiarlos, este CHECK la RECHAZA y la anulacion del cobro
-- entero falla — con un ORA-02290 crudo, en una operacion que no tiene nada que ver a simple
-- vista. Redactado asi a proposito: es preferible que falle a que queden acuerdos anulados sin
-- constancia de quien y por que.
ALTER TABLE CRD.ACCN ADD CONSTRAINT CK_ACCN_MTRC
    CHECK (ACCNESTD <> 3 OR TRIM(ACCNMTRC) IS NOT NULL);

CREATE INDEX CRD.IDX_ACCN_PRESTAMO ON CRD.ACCN (PRSTCDGO);
CREATE INDEX CRD.IDX_ACCN_ENTIDAD ON CRD.ACCN (ENTDCDGO, ACCNESTD);
-- La tabla es LA UNICA FUENTE de "cuanto se condono" (consecuencia de K6, ver el plan): el
-- indice por estado+fecha es el que sostiene un reporte consultable de verdad.
CREATE INDEX CRD.IDX_ACCN_ESTADO ON CRD.ACCN (ACCNESTD, ACCNFCHA);

COMMENT ON TABLE  CRD.ACCN IS
    'Acuerdo de pago con condonacion: ciclo de registro y aprobacion de condonacion, previo a aplicarse. Unica fuente consultable de cuanto se condono, a quien y quien lo autorizo (K6: el prestamo queda CANCELADO, indistinguible de uno pagado normal).';
COMMENT ON COLUMN CRD.ACCN.ACCNESTD IS
    'Rubro 247: 1 VIGENTE, 2 APLICADO, 3 ANULADO. El acuerdo nace VIGENTE y ya decidido (K4 derogada el 2026-08-30: no hay aprobacion, la previsualizacion en pantalla es el control). Pasa a APLICADO al PROCESAR su cobro en CBCR (K11), no al confirmarlo. Un ANULADO conserva su registro.';
COMMENT ON COLUMN CRD.ACCN.ACCNVLPG IS
    'Parte NO condonada que paga el socio (capital pagado + seguros al 100%, K3). Es el monto que luego se registra como cobro en CBCR, ya fijo porque el acuerdo esta aprobado (seccion 5 del plan).';
COMMENT ON COLUMN CRD.ACCN.EVPRCDGO IS
    'EventoPrestamo de la aplicacion real (pago + condonacion + CANCELADO). NULL hasta el paso PROCESO del cobro en CBCR (K8).';
COMMENT ON COLUMN CRD.ACCN.CBCRCDGO IS
    'Cobro en CRD.CBCR por la parte pagada. Se llena en el paso 3 del flujo (seccion 5 del plan), despues de que el acuerdo ya este APROBADO.';


-- =====================================================================================
-- 2. CREATE TABLE: CRD.DACC — detalle por concepto (5 filas por acuerdo)
-- =====================================================================================
-- Los CINCO conceptos del prestamo (no una clasificacion pagado/condonado) — seccion 2 del
-- plan: Capital, Interes, Mora, Desgravamen, Seguro de incendio. Los dos ultimos SIEMPRE con
-- DACCVLCN = 0 (K3: nunca se condonan).
-- =====================================================================================

CREATE SEQUENCE CRD.SQ_DACCCDGO START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE CRD.DACC (
    DACCCDGO NUMBER       NOT NULL,   -- PK
    ACCNCDGO NUMBER       NOT NULL,   -- FK CRD.ACCN
    DACCCPTO NUMBER       NOT NULL,   -- rubro 248: 1 CAPITAL, 2 INTERES, 3 MORA, 4 DESGRAVAMEN, 5 SEGURO_INCENDIO
    DACCVLAD NUMBER(18,2) NOT NULL,   -- monto adeudado de este concepto
    DACCVLPG NUMBER(18,2) NOT NULL,   -- monto pagado de este concepto
    DACCVLCN NUMBER(18,2) NOT NULL    -- monto condonado de este concepto
);

ALTER TABLE CRD.DACC ADD CONSTRAINT PK_DACC PRIMARY KEY (DACCCDGO);
ALTER TABLE CRD.DACC ADD CONSTRAINT FK_DACC_ACCN FOREIGN KEY (ACCNCDGO) REFERENCES CRD.ACCN(ACCNCDGO);
ALTER TABLE CRD.DACC ADD CONSTRAINT CK_DACC_CPTO CHECK (DACCCPTO BETWEEN 1 AND 5);
-- Una sola fila por concepto en el mismo acuerdo — nunca dos filas de "Capital".
-- (UNIQUE, no CHECK: el nombre lleva UK_ para que diga lo que es.)
ALTER TABLE CRD.DACC ADD CONSTRAINT UK_DACC_ACCN_CPTO UNIQUE (ACCNCDGO, DACCCPTO);

-- ⚠️ K3 EN LA BASE, no solo en la aplicacion. Es la regla mas fuerte del frente entero — los
-- seguros se pagan al 100% y NUNCA se condonan — y hasta esta linea el unico que la sostenia
-- era el codigo. Una condonacion de seguro no se nota al ocurrir: se nota meses despues,
-- cuando la aseguradora reclama una prima que el sistema perdono.
ALTER TABLE CRD.DACC ADD CONSTRAINT CK_DACC_SEGUROS
    CHECK (DACCCPTO NOT IN (4, 5) OR DACCVLCN = 0);
ALTER TABLE CRD.DACC ADD CONSTRAINT CK_DACC_VALORES
    CHECK (DACCVLAD >= 0 AND DACCVLPG >= 0 AND DACCVLCN >= 0);

-- Sin indice propio por ACCNCDGO: UK_DACC_ACCN_CPTO ya crea uno con ACCNCDGO como columna
-- lider, y sirve igual para el acceso por acuerdo.

COMMENT ON TABLE  CRD.DACC IS
    'Detalle por concepto de un acuerdo de condonacion: exactamente 5 filas (Capital/Interes/Mora/Desgravamen/Seguro de incendio), cada una con adeudado/pagado/condonado.';
COMMENT ON COLUMN CRD.DACC.DACCVLCN IS
    'Monto condonado de este concepto. SIEMPRE 0 para Desgravamen(4) y Seguro de incendio(5) — K3, garantizado por CK_DACC_SEGUROS y ademas validado en la aplicacion.';


-- =====================================================================================
-- 3. CRD.DCBC — columna nueva ACCNCDGO (opcional, seccion 5 del plan)
-- =====================================================================================
-- Mismo patron que PRSTCDGO/TPAPCDGO: nullable, solo se llena cuando CBCRTPOO = ACUERDO_CONDONACION.

ALTER TABLE CRD.DCBC ADD ACCNCDGO NUMBER;
ALTER TABLE CRD.DCBC ADD CONSTRAINT FK_DCBC_ACCN FOREIGN KEY (ACCNCDGO) REFERENCES CRD.ACCN(ACCNCDGO);
CREATE INDEX CRD.IDX_DCBC_ACUERDO ON CRD.DCBC (ACCNCDGO);

COMMENT ON COLUMN CRD.DCBC.ACCNCDGO IS
    'FK al acuerdo de condonacion cuando CBCR.CBCRTPOO = ACUERDO_CONDONACION. NULL en los demas tipos de operacion.';


-- =====================================================================================
-- 4. CRD.EVPR — expandir CK_EVPR_TIPO con el nuevo tipo de operacion (K8)
-- =====================================================================================

ALTER TABLE CRD.EVPR DROP CONSTRAINT CK_EVPR_TIPO;
ALTER TABLE CRD.EVPR ADD CONSTRAINT CK_EVPR_TIPO
    CHECK (EVPRTPOO IN ('PAGO_MANUAL', 'PAGO_APORTES', 'ABONO_CAPITAL', 'PRECANCELACION',
                         'ACUERDO_CONDONACION'));


-- =====================================================================================
-- 5. RUBROS 247 y 248, y un detalle nuevo en el rubro 245 — esquema SCP
-- =====================================================================================
-- ⚠️ VOLVER A CORRER el control 0.2 (MAX real) INMEDIATAMENTE ANTES de este bloque. Los
-- PDTRCDGO de abajo (1169-1177) fueron el rango libre verificado el 2026-08-29 a la hora en
-- que se escribio este script — puede haber cambiado.

-- 5.1 Rubro 247 — estado del acuerdo de condonacion
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (247, 'CRD ESTADO ACUERDO CONDONACION', SYSDATE, 247,
        NVL((SELECT MAX(r.PRBRTPOO) FROM SCP.PRBR r WHERE r.PRBRALTR = 169), 1));

-- 5.2 Rubro 248 — concepto del prestamo (los 5 de la seccion 2 del plan)
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (248, 'CRD CONCEPTO PRESTAMO', SYSDATE, 248,
        NVL((SELECT MAX(r.PRBRTPOO) FROM SCP.PRBR r WHERE r.PRBRALTR = 169), 1));

-- 5.3 Detalles. PDTR 1169-1177.
INSERT ALL
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1169, 247, 'VIGENTE',  1, NULL, 1, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1170, 247, 'APLICADO', 2, NULL, 2, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1171, 247, 'ANULADO',  3, NULL, 3, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1172, 248, 'CAPITAL',            1, 'CAPITAL',         1, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1173, 248, 'INTERES',            2, 'INTERES',         2, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1174, 248, 'MORA',               3, 'MORA',            3, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1175, 248, 'DESGRAVAMEN',        4, 'DESGRAVAMEN',     4, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1176, 248, 'SEGURO DE INCENDIO', 5, 'SEGURO_INCENDIO', 5, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1177, 245, 'ACUERDO CONDONACION', 6, 'ACUERDO_CONDONACION', 6, 1)
SELECT * FROM DUAL;

COMMIT;


-- 5.4 ⚠ SINCRONIZAR LAS SECUENCIAS — obligatorio despues de insertar PK explicitos, y es el
--     paso que se olvida. Si SQ_PRBRCDGO / SQ_PDTRCDGO quedan por debajo de lo que acabamos
--     de insertar a mano, la proxima vez que ALGUIEN cree un rubro DESDE LA APLICACION la
--     secuencia entrega 247 (o 1169) y el INSERT muere por PK duplicada — en una pantalla
--     que no tiene nada que ver con acuerdos, asi que nadie va a relacionar las dos cosas.
--     Mismo bloque 4.4 de DDL-COBROS-APROBACION-CONTABILIDAD.sql.
--
--     Correr PRIMERO la consulta y ejecutar SOLO la linea cuya secuencia haya quedado en o
--     por debajo del PK usado. Si ya esta adelantada, NO TOCARLA.
SELECT  'SQ_PRBRCDGO' AS SECUENCIA, 248 AS PK_USADO,
        (SELECT s.LAST_NUMBER FROM ALL_SEQUENCES s
         WHERE s.SEQUENCE_OWNER = 'SCP' AND s.SEQUENCE_NAME = 'SQ_PRBRCDGO') AS SIGUIENTE
FROM    DUAL
UNION ALL
SELECT  'SQ_PDTRCDGO', 1177,
        (SELECT s.LAST_NUMBER FROM ALL_SEQUENCES s
         WHERE s.SEQUENCE_OWNER = 'SCP' AND s.SEQUENCE_NAME = 'SQ_PDTRCDGO')
FROM    DUAL;

-- ALTER SEQUENCE SCP.SQ_PRBRCDGO RESTART START WITH 249;
-- ALTER SEQUENCE SCP.SQ_PDTRCDGO RESTART START WITH 1178;


-- =====================================================================================
-- 6. CONTROLES POSTERIORES
-- =====================================================================================

SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME IN ('ACCN', 'DACC') ORDER BY t.TABLE_NAME;

SELECT c.TABLE_NAME, c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.STATUS
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME IN ('ACCN', 'DACC')
ORDER  BY c.TABLE_NAME, c.CONSTRAINT_TYPE;

SELECT SEARCH_CONDITION FROM ALL_CONSTRAINTS WHERE OWNER='CRD' AND CONSTRAINT_NAME='CK_EVPR_TIPO';

SELECT  r.PRBRALTR AS RUBRO, r.PRBRDSCR, d.PDTRALTR AS DETALLE, d.PDTRDSCR, d.PDTRVLRV
FROM    SCP.PRBR r
LEFT    JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
WHERE   r.PRBRALTR IN (247, 248)
ORDER   BY r.PRBRALTR, d.PDTRALTR;

SELECT 'ACCN' AS TABLA, COUNT(*) AS FILAS FROM CRD.ACCN
UNION ALL
SELECT 'DACC', COUNT(*) FROM CRD.DACC;

-- 6.5 Los CHECK que sostienen K3 y K10 tienen que estar los dos, y ENABLED/VALIDATED.
--     Esperado: 3 filas — CK_ACCN_MTRC, CK_DACC_SEGUROS, CK_DACC_VALORES.
SELECT c.TABLE_NAME, c.CONSTRAINT_NAME, c.STATUS, c.VALIDATED, c.SEARCH_CONDITION
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD'
AND    c.CONSTRAINT_NAME IN ('CK_ACCN_MTRC', 'CK_DACC_SEGUROS', 'CK_DACC_VALORES');


-- =====================================================================================
-- 7. DELTA PARA LA BASE LOCAL — ejecutar SOLO en local
-- =====================================================================================
-- La base local ya corrio la version ANTERIOR de este script (el agente de backend, el
-- 2026-08-29), que no traia los tres CHECK nuevos y nombraba el UNIQUE como si fuera un
-- CHECK. En PRODUCCION no corras nada de esta seccion: el script de arriba ya la incluye.
--
-- Descomentar y correr en local. Si alguna linea falla con ORA-02264/ORA-00955 (ya existe),
-- esa parte ya estaba aplicada — seguir con las demas.

-- ALTER TABLE CRD.DACC DROP CONSTRAINT CK_DACC_UNIQ;
-- DROP INDEX CRD.IDX_DACC_ACUERDO;
-- ALTER TABLE CRD.DACC ADD CONSTRAINT UK_DACC_ACCN_CPTO UNIQUE (ACCNCDGO, DACCCPTO);
-- ALTER TABLE CRD.DACC ADD CONSTRAINT CK_DACC_SEGUROS
--     CHECK (DACCCPTO NOT IN (4, 5) OR DACCVLCN = 0);
-- ALTER TABLE CRD.DACC ADD CONSTRAINT CK_DACC_VALORES
--     CHECK (DACCVLAD >= 0 AND DACCVLPG >= 0 AND DACCVLCN >= 0);
-- ALTER TABLE CRD.ACCN ADD CONSTRAINT CK_ACCN_MTRC
--     CHECK (ACCNESTD <> 3 OR TRIM(ACCNMTRC) IS NOT NULL);

-- Y la sincronizacion de secuencias del 5.4, que tampoco estaba: correr la consulta y solo
-- entonces la linea que corresponda.

-- =====================================================================================
-- ALTER — estado ANULADO en CRD.CBCR + fecha y referencia del pago en CRD.PGAP
-- FECHA: 2026-08-29
--
-- =====================================================================================
-- ESTADO: NO EJECUTADO.
--
-- ⚠️ ESTE SCRIPT ES SOLO PARA AMBIENTES DONDE **YA SE CORRIO**
--    DDL-COBROS-APROBACION-CONTABILIDAD.sql EN SU VERSION VIEJA (sin el estado 5).
--    Hoy eso es unicamente el LOCAL de desarrollo.
--
--    EN PRUEBAS Y PRODUCCION **NO SE CORRE ESTE ARCHIVO** para la parte de CBCR: el
--    script principal ya quedo actualizado con el estado 5, las tres columnas de
--    anulacion y su CHECK, asi que una sola corrida de aquel deja todo bien.
--    ⚠️ PERO EL BLOQUE 2 (CRD.PGAP) SI VA EN TODOS LOS AMBIENTES: esa tabla existe desde
--    siempre y las dos columnas nuevas no estan en ningun otro script.
-- =====================================================================================
--
-- QUE RESUELVE, y son dos cosas independientes:
--
-- BLOQUE 1 — Estado ANULADO (5) en CRD.CBCR.
--   Al construir el circuito aparecio un caso que la maquina de estados original no
--   cubria. Los motivos reales por los que contabilidad rechaza un cobro son cuatro
--   (confirmados por el usuario el 2026-08-29): el respaldo no sirve, el monto no coincide
--   con el deposito, la cuenta bancaria es la equivocada... y **el deposito no aparece en
--   el banco**.
--   Los tres primeros se corrigen y se reenvian. EL CUARTO NO SE CORRIGE: la plata no
--   esta, y el asiento contra la cuenta transitoria NUNCA DEBIO EXISTIR. Sin un estado de
--   anulacion, ese cobro quedaria RECHAZADO para siempre con un asiento vivo por dinero
--   que no entro, falseando la transitoria hasta que alguien lo note conciliando.
--   Por eso ANULADO reversa el asiento, a diferencia del rechazo simple: no es que el
--   cobro este mal registrado, es que NO HUBO COBRO.
--   Quien anula: CREDITO (decision del usuario). Contabilidad detecta que el deposito no
--   llego y lo dice en el motivo del rechazo; credito ejecuta la anulacion sobre esa base.
--   El control cruzado se mantiene porque queda por escrito quien dijo que no estaba.
--
-- BLOQUE 2 — Fecha y referencia del pago en CRD.PGAP.
--   Cuando tesoreria confirma el pago de una devolucion de aportes, el usuario quiere ver
--   en el PagoAporte la fecha y la referencia con que se pago. El dato existe del lado de
--   CXP (PagoProgramado) y CRD lo LEE — nunca al reves: la direccion cxp -> crd esta
--   prohibida (el sistema se comercializa sin crd), y ya existe el mecanismo de
--   reconciliacion "CRD consulta, CXP no avisa".
--
--   ⚠️ POR QUE PGAPRFPG ES NULLABLE Y NO LLEVA CHECK. Verificado en
--      PagoProgramadoServiceImpl el 2026-08-29: `fechaRespuesta` se llena SIEMPRE que un
--      pago pasa a CONFIRMADO, por los dos caminos que existen. Pero `referenciaBanco`
--      solo es confiable en el camino del ARCHIVO DE RESPUESTA DEL BANCO; en la
--      CONFIRMACION MANUAL es un parametro OPCIONAL y puede quedar sin dato.
--      O sea: una referencia vacia aca es un estado LEGITIMO, no un error de carga.
--      No rellenar con ningun texto de relleno — la pantalla decide como mostrar el vacio;
--      el dato se guarda como lo que es.
--
-- EJECUCION MANUAL, como owner del esquema CRD.
-- IDEMPOTENCIA: los ALTER ADD fallan si ya se ejecutaron. Es deliberado.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS
-- =====================================================================================

-- 0.1 ¿En que ambiente estoy? Si CBCR NO existe, este ambiente todavia no corrio el script
--     principal: NO corras el bloque 1 aca — corre DDL-COBROS-APROBACION-CONTABILIDAD.sql,
--     que ya trae todo. El bloque 2 (PGAP) SI se corre igual.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME IN ('CBCR', 'PGAP') ORDER BY t.TABLE_NAME;

-- 0.2 Las columnas nuevas no deben existir todavia. Esperado: 0 filas.
SELECT c.TABLE_NAME, c.COLUMN_NAME
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD'
AND  ( (c.TABLE_NAME = 'CBCR' AND c.COLUMN_NAME IN ('CBCRUSAN','CBCRFCAN','CBCRMTAN'))
    OR (c.TABLE_NAME = 'PGAP' AND c.COLUMN_NAME IN ('PGAPFCPG','PGAPRFPG')) );

-- 0.3 El CHECK viejo de estado, el que hay que reemplazar. Esperado: la condicion con
--     IN (1,2,3,4). Si ya dice (1,2,3,4,5), el bloque 1 ya se corrio: saltealo.
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'CBCR' AND c.CONSTRAINT_NAME = 'CK_CBCR_ESTD';

-- 0.4 ¿Hay cobros ya cargados? Si CBCR tiene filas, el DROP/ADD del CHECK las revalida:
--     ninguna deberia tener un estado fuera de 1-4, pero mejor saberlo antes.
SELECT CBCRESTD, COUNT(*) FROM CRD.CBCR GROUP BY CBCRESTD ORDER BY 1;

-- 0.5 Volumen de PGAP, solo informativo (el ALTER ADD de columna nullable es
--     metadata-only en Oracle: instantaneo, sin reescribir la tabla).
SELECT COUNT(*) AS FILAS_PGAP FROM CRD.PGAP;


-- =====================================================================================
-- 1. CRD.CBCR — estado ANULADO (5) y columnas de anulacion
--    ⛔ SALTAR ESTE BLOQUE en ambientes que todavia no corrieron el script principal.
-- =====================================================================================

ALTER TABLE CRD.CBCR ADD (
    CBCRUSAN VARCHAR2(50),      -- usuario de credito que anulo
    CBCRFCAN TIMESTAMP,         -- fecha de anulacion
    CBCRMTAN VARCHAR2(2000)     -- motivo de anulacion
);

-- Oracle NO permite modificar la condicion de un CHECK existente: hay que dropearlo y
-- volver a crearlo. El DROP deja la tabla un instante sin esa validacion; como el ADD va
-- inmediatamente despues y en la misma sesion, la ventana es irrelevante — pero no
-- separes estas dos sentencias ni las corras en momentos distintos.
ALTER TABLE CRD.CBCR DROP CONSTRAINT CK_CBCR_ESTD;
ALTER TABLE CRD.CBCR ADD CONSTRAINT CK_CBCR_ESTD CHECK (CBCRESTD IN (1, 2, 3, 4, 5));

-- ⚠ EL MOTIVO DE ANULACION ES OBLIGATORIO CUANDO EL COBRO ESTA ANULADO, garantizado por
--   la base y no solo por Java. Anular significa afirmar que el dinero NUNCA ENTRO y
--   reversar su asiento: sin el motivo escrito, nadie puede reconstruir despues por que se
--   dio de baja plata que el sistema decia haber recibido.
--   El TRIM cubre la cadena vacia, que no es NULL y pasaria con un simple NOT NULL.
ALTER TABLE CRD.CBCR ADD CONSTRAINT CK_CBCR_MTAN
    CHECK (CBCRESTD <> 5 OR TRIM(CBCRMTAN) IS NOT NULL);

COMMENT ON COLUMN CRD.CBCR.CBCRMTAN IS
    'Motivo de la anulacion. Obligatorio cuando CBCRESTD = 5, garantizado por CK_CBCR_MTAN. Distinto de CBCRMTRC (motivo del rechazo): el rechazo se corrige y se reenvia, la anulacion reversa el asiento porque no hubo cobro.';
COMMENT ON COLUMN CRD.CBCR.CBCRUSAN IS
    'Usuario de CREDITO que anulo (decision del usuario 2026-08-29). Contabilidad detecta que el deposito no llego y lo escribe en el motivo del rechazo; credito ejecuta la anulacion sobre esa base.';


-- =====================================================================================
-- 2. CRD.PGAP — fecha y referencia del pago de la devolucion
--    ✅ ESTE BLOQUE VA EN TODOS LOS AMBIENTES, incluido produccion.
-- =====================================================================================

ALTER TABLE CRD.PGAP ADD (
    PGAPFCPG DATE,               -- fecha real del pago; de PagoProgramado.fechaRespuesta
    PGAPRFPG VARCHAR2(100)       -- referencia bancaria; de PagoProgramado.referenciaBanco
);

COMMENT ON COLUMN CRD.PGAP.PGAPFCPG IS
    'Fecha en que tesoreria pago efectivamente la devolucion. Se copia de CXP.PagoProgramado.fechaRespuesta al reconciliar. Confiable: se llena siempre que el pago pasa a CONFIRMADO, por los dos caminos.';
COMMENT ON COLUMN CRD.PGAP.PGAPRFPG IS
    'Referencia bancaria del pago, copiada de CXP.PagoProgramado.referenciaBanco. NULLABLE A PROPOSITO: solo es confiable cuando el pago se confirma por el archivo de respuesta del banco; en la confirmacion MANUAL es opcional y puede quedar vacia legitimamente. Un vacio aca NO es un error de carga.';


-- =====================================================================================
-- 3. CRD.APRT — de que devolucion salio este aporte negativo
--    ✅ ESTE BLOQUE VA EN TODOS LOS AMBIENTES, incluido produccion.
-- =====================================================================================
--
-- POR QUE HACE FALTA, y no es una comodidad
--
-- Una devolucion que consume aportes de VARIOS periodos de devengo genera VARIAS filas de
-- CRD.APRT (una por periodo) para el mismo tipo de aporte. Pero CRD.DDVA solo tiene UN
-- DDVAAPRT/DDVAPGAP por (devolucion, tipo): apunta a la PRIMERA fila y las demas quedan
-- sin referencia. Esta limitacion ya estaba documentada en el comentario de
-- DevolucionAporteServiceImpl.crearFilaNegativaDevolucion.
--
-- Consecuencia: `SELECT ... FROM DDVA WHERE DDVAAPRT = :idAporte` devuelve CERO FILAS para
-- cualquier aporte que no sea el primero de su grupo — aunque pertenezca genuinamente a una
-- devolucion. No es que la relacion sea muchos-a-muchos: es que simplemente no esta.
--
-- Sin esta columna, la unica forma de correlacionar las demas filas es por
-- entidad + tipo de aporte + el instante exacto que comparten todas las filas del mismo
-- registro. Funciona, pero es una muleta: depende de que el timestamp sea identico al
-- milisegundo en todas las filas y de que no haya otra devolucion del mismo socio y tipo
-- en ese mismo instante. Con APRTIDDV pasa a ser un JOIN directo.
--
-- Es exactamente el mismo patron que ya se aplico el 2026-08-28 con CRARCDGO para la
-- trazabilidad de la carga Petro, por la misma razon: cuando la pregunta es "de que
-- operacion salio esta fila", la respuesta va en la fila, no se reconstruye.
--
-- ⚠️ NO SE HACE BACKFILL de lo historico, y es deliberado: al 2026-08-29 no hay ninguna
--    devolucion registrada (DVAP y DDVA estan vacias), asi que no hay nada que rellenar.
--    Si en algun momento hubiera filas previas, reconstruirlas por la correlacion de
--    instante seria justamente la muleta que esta columna viene a eliminar.

ALTER TABLE CRD.APRT ADD (APRTIDDV NUMBER);

ALTER TABLE CRD.APRT ADD CONSTRAINT FK_APRT_DVAP
    FOREIGN KEY (APRTIDDV) REFERENCES CRD.DVAP(DVAPCDGO);

CREATE INDEX CRD.IDX_APRT_DEVOLUCION ON CRD.APRT (APRTIDDV);

COMMENT ON COLUMN CRD.APRT.APRTIDDV IS
    'Devolucion de aportes que genero esta fila negativa. NULL en todo aporte que no venga de una devolucion (que es la enorme mayoria). Se setea en TODAS las filas que crea crearFilaNegativaDevolucion, no solo en la primera: CRD.DDVA solo referencia una por (devolucion, tipo) y las demas quedaban sin rastro.';


-- =====================================================================================
-- 4. CONTROLES POSTERIORES
-- =====================================================================================

-- 3.1 Las cinco columnas nuevas existen. Esperado: 5 filas, NULLABLE = 'Y' en todas.
SELECT c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.NULLABLE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD'
AND  ( (c.TABLE_NAME = 'CBCR' AND c.COLUMN_NAME IN ('CBCRUSAN','CBCRFCAN','CBCRMTAN'))
    OR (c.TABLE_NAME = 'PGAP' AND c.COLUMN_NAME IN ('PGAPFCPG','PGAPRFPG')) )
ORDER  BY c.TABLE_NAME, c.COLUMN_NAME;

-- 3.2 El CHECK de estado admite el 5, y existe el del motivo. Esperado: 2 filas, ENABLED.
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION, c.STATUS
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'CBCR'
AND    c.CONSTRAINT_NAME IN ('CK_CBCR_ESTD', 'CK_CBCR_MTAN');

-- 3.3 Prueba del CHECK del motivo, para confirmar que de verdad protege. NO deberia
--     insertar nada: se espera ORA-02290 (check constraint violated).
--     Descomentar solo si se quiere verificar; ROLLBACK obligatorio despues.
--
-- INSERT INTO CRD.CBCR (CBCRCDGO, ENTDCDGO, CBCRTPOO, CBCRESTD, CNBCCDGO, CBCRRTRS,
--                       CBCRVLRR, CBCRFCHA, CBCRUSRG)
-- VALUES (-1, (SELECT MIN(ENTDCDGO) FROM CRD.ENTD), 'PAGO_CUOTA', 5,
--         (SELECT MIN(CNBCCDGO) FROM TSR.CNBC), 'x', 1, SYSDATE, 'PRUEBA');
-- ROLLBACK;

-- 3.4 Las columnas de PGAP arrancan vacias. Esperado: 0 en las dos.
SELECT SUM(CASE WHEN PGAPFCPG IS NOT NULL THEN 1 ELSE 0 END) AS CON_FECHA_PAGO,
       SUM(CASE WHEN PGAPRFPG IS NOT NULL THEN 1 ELSE 0 END) AS CON_REFERENCIA
FROM   CRD.PGAP;

-- =====================================================================
-- PGS.APLP: un gasto de caja chica puede ser el origen de un pago
-- Modulo: TSR/CXP  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-03
--
-- QUE HACE
--   Agrega PGS.APLP.APLPMVCH -> FK a TSR.MVCH, para que una aplicacion de
--   pago pueda venir de un gasto de caja chica, igual que hoy puede venir de
--   un anticipo (APLPANTP).
--
--   Diseno: docs/logica-negocio/tsr/PLAN-GASTO-CAJA-CHICA-PAGA-FACTURA.md
--
-- POR QUE VA DE ESTE LADO Y NO EN TSR.MVCH
--   APLP ya es la tabla que modela «que documento se afecta» (APLPFCTC,
--   APLPLQCC, ...) y «con que se paga» (APLPANTP). Caja chica es otro origen
--   de pago, exactamente como el anticipo. Ponerlo aca reusa el reverso, el
--   estado de pago y el calculo de saldo que ya funcionan, y no toca MVCH,
--   que es una tabla en uso.
--
-- ⚠️ EL CONTROL 0.2 NO ES DECORATIVO
--   Lista el NULLABLE de TODAS las FK de documento de esta tabla, no solo de
--   la nueva. El 2026-09-03 APLPFCTC era NOT NULL —de cuando toda aplicacion
--   era contra una factura— y rompio el cruce contra liquidaciones con
--   ORA-01400 (ver cxp/sql/e2-05). La leccion: cuando una columna pasa de ser
--   OBLIGATORIA a ser UNA DE VARIAS ALTERNATIVAS, el trabajo no es agregar la
--   nueva sino relajar la vieja. Si el control devuelve alguna en 'N', PARAR.
--
-- ES SEGURO CORRERLO CON EL SISTEMA ARRIBA
--   Agregar una columna nullable no reescribe filas ni invalida nada. Ninguna
--   fila existente la usa todavia.
--
-- ORDEN RESPECTO DEL WAR
--   ESTE SCRIPT VA ANTES del WAR que mapee APLPMVCH. Si el WAR sube primero,
--   AplicacionPagoCxp mapea una columna que no existe: ORA-00904 en TODA
--   lectura de esa entidad, que es de las mas usadas del modulo.
--   ⛔ Y no se mergea a main el mapeo hasta que esto este corrido — seccion 7 «No mergear a main un mapeo cuya columna no esta
--      en la base» del registro de reservas (hay DOS secciones 7: citar por titulo).
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 -- CONTROLES ANTES. Correr y LEER.
-- =====================================================================

-- 0.1 La columna NO debe existir todavia. ESPERADO: 0 filas.
SELECT column_name FROM all_tab_columns
 WHERE owner = 'PGS' AND table_name = 'APLP' AND column_name = 'APLPMVCH';

-- 0.2 ⚠️ EL CONTROL QUE IMPORTA. Todas las FK de documento y de origen de esta
--     tabla deben ser NULLABLE = 'Y', porque son EXCLUYENTES entre si: una
--     aplicacion afecta UN documento y viene de UN origen.
--     ESPERADO: las siete en 'Y'.
--     Si alguna sale en 'N', PARAR Y AVISAR: el mismo ORA-01400 del e2-05 se
--     va a repetir con el documento u origen que la use.
SELECT column_name, nullable
  FROM all_tab_columns
 WHERE owner = 'PGS' AND table_name = 'APLP'
   AND column_name IN ('APLPFCTC','APLPLQCC','APLPNTCC','APLPNTDC',
                       'APLPRTNC','APLPRTV2','APLPANTP')
 ORDER BY nullable, column_name;

-- 0.3 Linea base: cuantas aplicaciones hay y de que origen.
--     Sirve para comprobar despues que ninguna cambio.
SELECT COUNT(*)                                              AS total,
       SUM(CASE WHEN APLPANTP IS NOT NULL THEN 1 ELSE 0 END) AS desde_anticipo
  FROM PGS.APLP;

-- 0.4 La tabla destino existe y su PK es la que se va a referenciar.
--     ESPERADO: 1 fila, MVCHCDGO.
SELECT column_name FROM all_tab_columns
 WHERE owner = 'TSR' AND table_name = 'MVCH' AND column_name = 'MVCHCDGO';


-- =====================================================================
-- BLOQUE 1 -- LA COLUMNA
-- =====================================================================
-- NULLABLE a proposito, y no es un detalle: la enorme mayoria de las
-- aplicaciones NO vienen de caja chica y esta columna queda vacia. Una
-- columna DEFAULT n NOT NULL romperia todo INSERT existente.

ALTER TABLE PGS.APLP ADD (APLPMVCH NUMBER NULL);

COMMENT ON COLUMN PGS.APLP.APLPMVCH IS
    'Gasto de caja chica que origino este pago (TSR.MVCH). Excluyente con APLPANTP y con los demas origenes: una aplicacion viene de UN origen';


-- =====================================================================
-- BLOQUE 1b -- ⚠️ EL GRANT. VA ANTES DE LA FK Y LO CORRE OTRO USUARIO.
-- =====================================================================
-- Esto fallo el 2026-09-03 y la culpa es de este script, que lo tenia como un
-- comentario suelto en vez de un paso.
--
--   ORA-01031: insufficient privileges
--
-- POR QUE
--   MVCH vive en TSR y APLP en PGS. Una FK cross-schema exige el privilegio
--   REFERENCES sobre la tabla apuntada, y ese privilegio lo da EL DUEÑO DE TSR.
--
-- ⛔ EL ROL DBA NO ALCANZA. REFERENCES no viene con DBA: hay que otorgarlo
--    explicitamente. Ya se aprendio en este repo — ver el bloque 0.4 de
--    docs/logica-negocio/crd/sql/DDL-COBRO-PETRO-DOS-PASOS.sql, que lo dice
--    con todas las letras. Esa leccion estaba escrita y este script no la
--    aplico.
--
-- CONECTADO COMO TSR (no como PGS, no como el ejecutor del resto):

GRANT REFERENCES ON TSR.MVCH TO PGS;

-- Control: confirmar que quedo otorgado antes de seguir. ESPERADO: 1 fila.
SELECT grantee, privilege, table_name
  FROM all_tab_privs
 WHERE table_schema = 'TSR' AND table_name = 'MVCH'
   AND privilege = 'REFERENCES' AND grantee = 'PGS';


-- =====================================================================
-- BLOQUE 2 -- LA FK  (volver al usuario del resto del script)
-- =====================================================================
-- Si el bloque 1 ya corrio y solo fallo esto, NO hace falta rehacer nada:
-- la columna APLPMVCH ya existe y este bloque se reanuda tal cual.

ALTER TABLE PGS.APLP ADD CONSTRAINT FK_APLP_MVCH
    FOREIGN KEY (APLPMVCH) REFERENCES TSR.MVCH (MVCHCDGO);

-- El prefijo de schema va tambien en el INDICE, no solo en la tabla. Sin el,
-- el indice queda en el schema del ejecutor — la trampa que costo el e2-04.
CREATE INDEX PGS.IX_APLP_MVCH ON PGS.APLP (APLPMVCH);


-- =====================================================================
-- BLOQUE 3 -- CONTROLES DESPUES. Correr y LEER.
-- =====================================================================

-- 3.1 La columna existe y es nullable. ESPERADO: 1 fila, NUMBER, 'Y'.
SELECT column_name, data_type, nullable
  FROM all_tab_columns
 WHERE owner = 'PGS' AND table_name = 'APLP' AND column_name = 'APLPMVCH';

-- 3.2 La FK apunta a MVCH y esta habilitada. ESPERADO: FK_APLP_MVCH / ENABLED.
SELECT c.constraint_name, c.status, r.table_name AS referencia
  FROM all_constraints c
  LEFT JOIN all_constraints r
         ON r.owner = c.r_owner AND r.constraint_name = c.r_constraint_name
 WHERE c.owner = 'PGS' AND c.table_name = 'APLP'
   AND c.constraint_name = 'FK_APLP_MVCH';

-- 3.3 El indice quedo en PGS y no en el schema del ejecutor. ESPERADO: PGS.
SELECT owner, index_name FROM all_indexes
 WHERE table_name = 'APLP' AND index_name = 'IX_APLP_MVCH';

-- 3.4 Ninguna aplicacion cambio. ESPERADO: los mismos numeros que 0.3.
SELECT COUNT(*)                                              AS total,
       SUM(CASE WHEN APLPANTP IS NOT NULL THEN 1 ELSE 0 END) AS desde_anticipo,
       SUM(CASE WHEN APLPMVCH IS NOT NULL THEN 1 ELSE 0 END) AS desde_caja_chica
  FROM PGS.APLP;


-- =====================================================================
-- BLOQUE 4 -- REVERSO. COMENTADO A PROPOSITO.
-- Solo se puede revertir si ninguna aplicacion vino de caja chica todavia.
-- =====================================================================
--
-- SELECT COUNT(*) FROM PGS.APLP WHERE APLPMVCH IS NOT NULL;  -- debe dar 0
--
-- DROP INDEX PGS.IX_APLP_MVCH;
-- ALTER TABLE PGS.APLP DROP CONSTRAINT FK_APLP_MVCH;
-- ALTER TABLE PGS.APLP DROP COLUMN APLPMVCH;
-- =====================================================================

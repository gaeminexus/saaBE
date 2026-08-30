-- ============================================================================
-- 08-rubros-partidas-transito.sql
-- Modulo TSR - Conciliacion bancaria: partidas en transito (frente N)
-- Escrito por el arbitro del equipo cxp/cxc/pagos/tsr/rhh/sri el 2026-08-29
-- ============================================================================
--
-- QUE HACE: crea en SCP.PRBR/SCP.PDTR los tres rubros del frente N, que hoy
-- existen SOLO como constantes Java y no tienen fila en la base:
--
--   239  TipoPartidaTransito       (TSR.DTCN.DTCNTPOO)  -> 4 detalles
--   240  EstadoPartidaTransito     (TSR.DTCN.DTCNESTD)  -> 2 detalles
--   241  EstadoCierreConciliacion  (TSR.CNCL.CNCLESTD)  -> 3 detalles
--
-- POR QUE NO ES URGENTE, Y AUN ASI HAY QUE CORRERLO
-- Verificado el 2026-08-29 sobre el codigo: NADA consulta estos rubros contra
-- la base. Las constantes se usan directas desde las interfaces Java
-- (EstadoPartidaTransito.PENDIENTE, etc.) en DetalleTransitoDaoServiceImpl,
-- GrupoConciliacionAsientoDaoServiceImpl, GrupoConciliacionExtractoDaoServiceImpl,
-- ConciliacionCierreServiceImpl, ConciliacionContableMatchServiceImpl y
-- ConciliacionDaoServiceImpl; y `Rubros.TIPO_PARTIDA_TRANSITO` /
-- `Rubros.ESTADO_PARTIDA_TRANSITO` / `Rubros.ESTADO_CIERRE_CONCILIACION`
-- no aparecen referenciadas en ninguna parte. El frontend tampoco los pide:
-- las pantallas de conciliacion no usan detalle-rubro.service.
--
-- Lo que se corrige aqui es una DEUDA DE REGISTRO, no una falla funcional:
-- los numeros 239/240/241 estan tomados en Java pero libres en la base, asi
-- que cualquier otro equipo puede reutilizarlos sin enterarse. Ya paso una vez:
-- crd/sql/70_CATALOGO_RUBRO_GENERACION_POR_FALTANTE.sql tuvo que saltar a mano
-- del 237 al 242 anotando en un comentario que 238-241 estaban "reservados
-- aunque no tengan fila". Esa reserva vive hoy en un comentario, no en la base.
--
-- ORDEN: independiente del resto de scripts de tsr/sql. No toca tablas de datos.
-- Se puede correr antes o despues de los de crd (usa PKs que no chocan con los
-- de ellos, ver BLOQUE 0).
-- ============================================================================


-- ============================================================================
-- BLOQUE 0 - CONTROL PREVIO. Correr y revisar ANTES de insertar nada.
-- ============================================================================

-- 0.1 Los tres rubros NO deben existir todavia, ni por PK ni por alterno.
--     Esperado: 0 filas.
SELECT PRBRCDGO, PRBRALTR, PRBRDSCR
FROM   SCP.PRBR
WHERE  PRBRCDGO IN (239, 240, 241) OR PRBRALTR IN (239, 240, 241);

-- 0.2 Los nueve detalles (1151-1159) deben estar libres. Esperado: 0 filas.
SELECT PDTRCDGO, PRBRCDGO, PDTRDSCR
FROM   SCP.PDTR
WHERE  PDTRCDGO BETWEEN 1151 AND 1159;

-- 0.3 Ancho real de las columnas de descripcion. Las entidades JPA no declaran
--     `length`, asi que este SELECT es la unica forma de saberlo antes de que
--     Oracle lo diga con un ORA-12899 (le paso al script 04 del modulo sri).
--     PRBRDSCR = 100. La descripcion mas larga de ESTE script mide 73 y va en
--     PDTRDSCR, que es mas ancha: ya hay descripciones de 105 cargadas.
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATA_LENGTH
FROM   ALL_TAB_COLUMNS
WHERE  OWNER = 'SCP'
AND    COLUMN_NAME IN ('PRBRDSCR', 'PDTRDSCR')
ORDER  BY TABLE_NAME;

-- 0.4 Maximos actuales. Sirve para confirmar el hueco elegido.
--     Se eligio 1151-1159 a proposito: los scripts pendientes de crd ocupan
--     1141 (rubro 242) y 1142-1150 (rubros 243/244), asi que arrancar en 1151
--     deja correr los dos juegos en cualquier orden sin colision.
SELECT MAX(PRBRCDGO) AS MAX_PRBR_PK,
       MAX(PRBRALTR) AS MAX_PRBR_ALT
FROM   SCP.PRBR;

SELECT MAX(PDTRCDGO) AS MAX_PDTR_PK FROM SCP.PDTR;


-- ============================================================================
-- BLOQUE 1 - LOS TRES RUBROS Y SUS DETALLES
-- Ejecutar CONECTADO AL ESQUEMA SCP (o con prefijo, como esta escrito).
--
-- Convencion seguida, la misma de tsr/sql/02-caja-chica.sql (rubros 232/233):
--   PRBRCDGO = PRBRALTR  (el PK y el alterno son el mismo numero)
--   PRBRTPOO = 0
--   PDTRVLRN = PDTRALTR  = el valor de la constante Java
--   PDTRVLRV = el nombre de la constante Java
--   PDTRESTD = 1 (activo)
--
-- OJO: el codigo resuelve por ALTERNO, no por PK
-- (DetalleRubroDaoService.selectValorStringByRubAltDetAlt). Por eso PDTRALTR
-- tiene que coincidir exactamente con el int de la interfaz Java.
-- ============================================================================

-- 1.1 Rubro 239 - Tipo de partida en transito (TSR.DTCN.DTCNTPOO)
--     Valores tomados de com/saa/rubros/TipoPartidaTransito.java
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (239, 'TSR - Tipo de partida en transito (DTCN)', SYSDATE, 239, 0);

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (1151, 239, 'DEPOSITO EN TRANSITO: esta en libros (MVCB), no en el banco', 1, 'DEPOSITO_EN_TRANSITO', 1, 1);

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (1152, 239, 'CHEQUE GIRADO NO COBRADO: esta en libros (MVCB), no en el banco', 2, 'CHEQUE_GIRADO_NO_COBRADO', 2, 1);

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (1153, 239, 'NC DEL BANCO NO REGISTRADA: esta en el extracto (DEXB), no en libros', 3, 'NC_BANCO_NO_REGISTRADA', 3, 1);

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (1154, 239, 'ND DEL BANCO NO REGISTRADA: comisiones del extracto (DEXB) no registradas', 4, 'ND_BANCO_NO_REGISTRADA', 4, 1);

-- 1.2 Rubro 240 - Estado de la partida en transito (TSR.DTCN.DTCNESTD)
--     Valores tomados de com/saa/rubros/EstadoPartidaTransito.java
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (240, 'TSR - Estado de la partida en transito (DTCN)', SYSDATE, 240, 0);

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (1155, 240, 'PENDIENTE: declarada en transito, no se conoce su contraparte', 1, 'PENDIENTE', 1, 1);

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (1156, 240, 'SALDADA: se concilio con su contraparte (ver DTCNCNSL)', 2, 'SALDADA', 2, 1);

-- 1.3 Rubro 241 - Estado del cierre de conciliacion (TSR.CNCL.CNCLESTD)
--     Valores tomados de com/saa/rubros/EstadoCierreConciliacion.java
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (241, 'TSR - Estado del cierre de conciliacion bancaria (CNCL)', SYSDATE, 241, 0);

-- BORRADOR no lo usa el flujo actual (cerrar() crea el CNCL ya en CERRADO).
-- Se inserta igual para que el catalogo refleje la interfaz Java completa.
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (1157, 241, 'BORRADOR: reservado, el flujo actual no lo usa', 1, 'BORRADOR', 1, 1);

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (1158, 241, 'CERRADO: el mes quedo cerrado con esta declaracion de partidas', 2, 'CERRADO', 2, 1);

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (1159, 241, 'ANULADO: se deshizo con anularCierre, liberando sus partidas', 3, 'ANULADO', 3, 1);

COMMIT;


-- ============================================================================
-- BLOQUE 2 - LAS SECUENCIAS
-- Los PK se insertaron explicitos, asi que las secuencias no avanzaron. Si
-- alguien crea un rubro despues por secuencia, chocaria con estos numeros.
-- ============================================================================

-- 2.1 Comparar el proximo valor de cada secuencia contra el PK mas alto usado.
--     Si SIGUIENTE_VALOR ya es MAYOR que PK_USADO, no hay nada que hacer.
SELECT 'SQ_PRBRCDGO' AS SECUENCIA, 241 AS PK_USADO, LAST_NUMBER AS SIGUIENTE_VALOR
FROM   ALL_SEQUENCES
WHERE  SEQUENCE_OWNER = 'SCP' AND SEQUENCE_NAME = 'SQ_PRBRCDGO';

SELECT 'SQ_PDTRCDGO' AS SECUENCIA, 1159 AS PK_USADO, LAST_NUMBER AS SIGUIENTE_VALOR
FROM   ALL_SEQUENCES
WHERE  SEQUENCE_OWNER = 'SCP' AND SEQUENCE_NAME = 'SQ_PDTRCDGO';

-- 2.2 Solo si 2.1 mostro que la secuencia se quedo atras, descomentar la que
--     corresponda. OJO: si los scripts de crd (242/243/244 y 1141-1150) se
--     corren DESPUES, hay que rehacer este paso con los numeros de ellos.
-- ALTER SEQUENCE SCP.SQ_PRBRCDGO RESTART START WITH 245;
-- ALTER SEQUENCE SCP.SQ_PDTRCDGO RESTART START WITH 1160;


-- ============================================================================
-- BLOQUE 3 - VERIFICACION POSTERIOR
-- ============================================================================

-- 3.1 Los tres rubros con su conteo de detalles.
--     Esperado: 239 -> 4 filas, 240 -> 2 filas, 241 -> 3 filas.
SELECT r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR, COUNT(d.PDTRCDGO) AS DETALLES
FROM   SCP.PRBR r
       LEFT JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
WHERE  r.PRBRALTR IN (239, 240, 241)
GROUP  BY r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR
ORDER  BY r.PRBRALTR;

-- 3.2 El detalle completo, para contrastar PDTRALTR contra las interfaces Java.
SELECT r.PRBRALTR AS RUBRO, d.PDTRALTR AS VALOR, d.PDTRVLRV AS CONSTANTE, d.PDTRDSCR
FROM   SCP.PRBR r
       JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
WHERE  r.PRBRALTR IN (239, 240, 241)
ORDER  BY r.PRBRALTR, d.PDTRALTR;

-- 3.3 Contraste contra los datos que YA existen en TSR.DTCN y TSR.CNCL: todo
--     valor usado en las tablas debe tener ahora su fila de catalogo.
--     Esperado: 0 filas. Si devuelve algo, hay un valor escrito por el codigo
--     que este script no contempla y hay que revisarlo antes de darlo por bueno.
SELECT DISTINCT 'DTCNTPOO' AS COLUMNA, t.DTCNTPOO AS VALOR_HUERFANO
FROM   TSR.DTCN t
WHERE  t.DTCNTPOO IS NOT NULL
AND    NOT EXISTS (SELECT 1 FROM SCP.PRBR r JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
                   WHERE r.PRBRALTR = 239 AND d.PDTRALTR = t.DTCNTPOO)
UNION ALL
SELECT DISTINCT 'DTCNESTD', t.DTCNESTD
FROM   TSR.DTCN t
WHERE  t.DTCNESTD IS NOT NULL
AND    NOT EXISTS (SELECT 1 FROM SCP.PRBR r JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
                   WHERE r.PRBRALTR = 240 AND d.PDTRALTR = t.DTCNESTD)
UNION ALL
SELECT DISTINCT 'CNCLESTD', c.CNCLESTD
FROM   TSR.CNCL c
WHERE  c.CNCLESTD IS NOT NULL
AND    NOT EXISTS (SELECT 1 FROM SCP.PRBR r JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
                   WHERE r.PRBRALTR = 241 AND d.PDTRALTR = c.CNCLESTD);


-- ============================================================================
-- REVERSO - DELIBERADAMENTE COMENTADO
-- Descomentar SOLO si hay que deshacer este script. Borra los tres rubros y
-- sus detalles. No toca ninguna fila de TSR.DTCN ni TSR.CNCL.
-- ============================================================================
-- DELETE FROM SCP.PDTR
-- WHERE  PDTRCDGO BETWEEN 1151 AND 1159
-- AND    PRBRCDGO IN (SELECT PRBRCDGO FROM SCP.PRBR WHERE PRBRALTR IN (239, 240, 241));
--
-- DELETE FROM SCP.PRBR WHERE PRBRALTR IN (239, 240, 241);
--
-- COMMIT;

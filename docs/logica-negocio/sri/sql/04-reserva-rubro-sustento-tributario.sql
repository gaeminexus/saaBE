-- ============================================================================
-- 04-reserva-rubro-sustento-tributario.sql
-- Modulo SRI - Reserva del rubro 238 (SUSTENTO_TRIBUTARIO_SRI)
-- Escrito por el arbitro del equipo cxp/cxc/pagos/tsr/rhh/sri el 2026-08-29
-- ============================================================================
--
-- QUE HACE: inserta UNA fila de cabecera en SCP.PRBR para el numero 238, que
-- `com.saa.rubros.Rubros.SUSTENTO_TRIBUTARIO_SRI` ya declara pero que no tiene
-- fila en la base. NO inserta detalles en SCP.PDTR: no consume ningun PDTRCDGO.
--
-- POR QUE NO LLEVA DETALLES, Y ESO ES CORRECTO
-- El catalogo real de sustentos tributarios NO vive en el sistema de rubros.
-- Vive en PGS.LSRI / PGS.TSRI (LSRI.TABLA = '703'), que crea
-- sri/sql/01-catalogos-ats.sql, y sus codigos son STRINGS de dos digitos
-- ('01', '02', ...), no enteros de un rubro. Asi lo dice el javadoc de
-- com/saa/rubros/SustentoTributarioSri.java y asi lo usan
-- SustentoTributarioServiceImpl y ReporteCuadreSriServiceImpl.
--
-- Es decir: el 238 es un NUMERO RESERVADO, no un catalogo. Se inserta la
-- cabecera para que quede tomado en la base y nadie lo reutilice, con una
-- descripcion que apunta a donde esta el catalogo de verdad.
--
-- SI PREFIERES NO TENER UN RUBRO SIN DETALLES EN EL CATALOGO, la alternativa
-- es NO correr este script y en su lugar cambiar
-- Rubros.SUSTENTO_TRIBUTARIO_SRI de 238 a otro numero, o eliminar esa
-- constante (hoy no la referencia nadie). Es una decision tuya; las dos
-- opciones son validas. Lo que NO conviene es dejarlo como esta: declarado en
-- Java, libre en la base.
--
-- ORDEN: correr ANTES que tsr/sql/08-rubros-partidas-transito.sql para que la
-- numeracion quede contigua (238 aqui, 239-241 alla). Si se corre despues no
-- pasa nada: son independientes.
-- ============================================================================


-- ============================================================================
-- BLOQUE 0 - CONTROL PREVIO
-- ============================================================================

-- 0.1 El 238 no debe existir todavia, ni por PK ni por alterno. Esperado: 0 filas.
SELECT PRBRCDGO, PRBRALTR, PRBRDSCR
FROM   SCP.PRBR
WHERE  PRBRCDGO = 238 OR PRBRALTR = 238;

-- 0.2 Ancho real de las columnas de descripcion. Las entidades JPA no declaran
--     `length`, asi que este SELECT es la unica forma de saberlo antes de que
--     Oracle lo diga con un ORA-12899. Esperado: PRBRDSCR = 100.
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATA_LENGTH
FROM   ALL_TAB_COLUMNS
WHERE  OWNER = 'SCP'
AND    COLUMN_NAME IN ('PRBRDSCR', 'PDTRDSCR')
ORDER  BY TABLE_NAME;

-- 0.3 El catalogo real del SRI debe existir ya (lo crea 01-catalogos-ats.sql).
--     Esperado: varias filas. Si da 0, corre primero ese script: sin el, el
--     generador del ATS no tiene de donde leer los sustentos.
--     (PGS.LSRI y PGS.TSRI no siguen la nomenclatura de 8 caracteres de la
--     casa: sus columnas son ID/TABLA/DETALLE/ESTADO y ID/LSRI/CODIGO/
--     DETALLE/PORCENTAJE/ESTADO. Verificado contra 01-catalogos-ats.sql.)
SELECT COUNT(*) AS SUSTENTOS_EN_CATALOGO_REAL
FROM   PGS.TSRI t
       JOIN PGS.LSRI l ON l.ID = t.LSRI
WHERE  l.TABLA = '703';


-- ============================================================================
-- BLOQUE 1 - LA CABECERA
-- Ejecutar CONECTADO AL ESQUEMA SCP (o con prefijo, como esta escrito).
-- Misma convencion que los rubros 232/233 de tsr/sql/02-caja-chica.sql:
-- PRBRCDGO = PRBRALTR, PRBRTPOO = 0.
-- ============================================================================

-- OJO: SCP.PRBR.PRBRDSCR es VARCHAR2(100). La entidad JPA
-- com/saa/model/scp/Rubro.java declara `@Column(name = "PRBRDSCR")` SIN
-- atributo `length`, asi que ni Hibernate ni el codigo avisan del limite: el
-- unico que lo dice es Oracle, con ORA-12899, al insertar. La primera version
-- de esta linea media 102 caracteres y fallo por eso. Esta mide 87.
-- Contexto: PDTRDSCR es mas ancha (hay descripciones de 105 ya cargadas).
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (238, 'SRI - Sustento tributario ATS (reservado: catalogo real en PGS.LSRI/PGS.TSRI tabla 703)', SYSDATE, 238, 0);

COMMIT;


-- ============================================================================
-- BLOQUE 2 - LA SECUENCIA
-- El PK se inserto explicito, la secuencia no avanzo.
-- ============================================================================

-- 2.1 Si SIGUIENTE_VALOR ya es MAYOR que 238, no hay nada que hacer.
SELECT 'SQ_PRBRCDGO' AS SECUENCIA, 238 AS PK_USADO, LAST_NUMBER AS SIGUIENTE_VALOR
FROM   ALL_SEQUENCES
WHERE  SEQUENCE_OWNER = 'SCP' AND SEQUENCE_NAME = 'SQ_PRBRCDGO';

-- 2.2 No la reinicies aqui: si vas a correr tambien
--     tsr/sql/08-rubros-partidas-transito.sql (239-241) y los de crd
--     (242/243/244), ajusta la secuencia UNA sola vez al final, con el numero
--     mas alto que haya quedado usado.


-- ============================================================================
-- BLOQUE 3 - VERIFICACION POSTERIOR
-- ============================================================================

-- 3.1 La cabecera, sin detalles. Esperado: 1 fila, DETALLES = 0.
SELECT r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR, COUNT(d.PDTRCDGO) AS DETALLES
FROM   SCP.PRBR r
       LEFT JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
WHERE  r.PRBRALTR = 238
GROUP  BY r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR;


-- ============================================================================
-- REVERSO - DELIBERADAMENTE COMENTADO
-- ============================================================================
-- DELETE FROM SCP.PRBR WHERE PRBRALTR = 238;
--
-- COMMIT;

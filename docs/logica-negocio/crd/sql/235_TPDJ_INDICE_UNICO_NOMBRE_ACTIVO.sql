-- =====================================================================================
-- 235 - CRD.TPDJ: que un nombre de tipo de adjunto NO PUEDA duplicarse nunca mas
-- FECHA: 2026-09-22 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- Sin comandos de SQL*Plus: no usa PROMPT, DEFINE, SET ni &variables.
--
-- ⛔ CORRER DESPUES del sql/234 (el UPDATE que desactiva el 38). Con dos filas activas
-- llamadas 'CERTIFICADO BANCARIO' el indice NO se puede crear, y el error de Oracle
-- (ORA-01452) seria justamente la prueba de que el 234 todavia no corrio.
--
-- =====================================================================================
-- POR QUE ESTE SCRIPT EXISTE: porque arreglar el script de carga NO ALCANZA
-- =====================================================================================
-- El 2026-09-22 se corrigio CARGA-TIPO-ADJUNTO-CERTIFICADO-BANCARIO.sql para que su
-- INSERT lleve WHERE NOT EXISTS. Eso cierra UN camino. Quedan abiertos otros:
--
--   1. ⛔ LA PANTALLA. Existe POST /rest/tpdj (TipoAdjuntoRest:84, CRUD generico, sin
--      ninguna validacion de nombre repetido) y la pantalla que lo usa
--      (saaFE crd/forms/parametrizacion/tipos-crd). Cualquier operador puede crear otro
--      'CERTIFICADO BANCARIO' desde Parametrizacion, hoy mismo, sin que nada se lo impida.
--   2. Cualquier INSERT manual, o un script de otro equipo, o una migracion.
--
-- Y el efecto de un duplicado no es un dato feo en un catalogo: el backend resuelve ese
-- tipo POR NOMBRE y exige EXACTAMENTE una fila activa, asi que con dos
--   - TODOS los certificados ya cargados desaparecen de la pantalla (410 adjuntos en
--     273 cuentas, medido el 2026-09-22),
--   - no se puede cargar ningun certificado nuevo, y
--   - se bloquea el pago a los jubilados que dependen del certificado.
--
-- ⇒ Paso DOS VECES en 18 dias (2026-09-04 -> id 37, ver sql/193; 2026-09-22 -> id 38,
--   ver sql/234). Las dos veces habia un comentario que decia que no se hiciera. La
--   conclusion no es "leer mejor": es que MIENTRAS LA BASE LO PERMITA, VA A VOLVER A
--   PASAR. Una regla que solo vive en un comentario no es una regla.
--
-- Es el mismo criterio que este equipo ya aplico en UX_RVSG_REFERENCIA y en el indice de
-- CBCR: el chequeo previo informa, el indice IMPIDE.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS — si alguno no da lo esperado, PARAR
-- =====================================================================================

-- 0.1 ¿Queda UN SOLO 'CERTIFICADO BANCARIO' activo? Esperado: 1.
--     Si da 2, el 234 no corrio todavia: correrlo primero.
SELECT COUNT(*) AS ACTIVOS_CERTIFICADO_BANCARIO
  FROM CRD.TPDJ t
 WHERE UPPER(TRIM(t.TPDJNMBR)) = 'CERTIFICADO BANCARIO'
   AND t.TPDJIDST = 1;

-- 0.2 ⭐ ¿Hay CUALQUIER OTRO nombre repetido entre las filas ACTIVAS? Esperado: 0 filas.
--     Esto mira TODA la tabla, no solo el certificado bancario: si otro catalogo ya
--     tiene homonimos activos, el CREATE INDEX de abajo va a fallar con ORA-01452 y hay
--     que decidir que hacer con ESOS antes (no se resuelven a ciegas: pueden ser de otro
--     frente y tener adjuntos colgando, igual que el 4).
SELECT UPPER(TRIM(t.TPDJNMBR)) AS NOMBRE_NORMALIZADO,
       COUNT(*)                AS CUANTAS_ACTIVAS,
       LISTAGG(t.TPDJCDGO, ', ') WITHIN GROUP (ORDER BY t.TPDJCDGO) AS IDS
  FROM CRD.TPDJ t
 WHERE t.TPDJIDST = 1
 GROUP BY UPPER(TRIM(t.TPDJNMBR))
HAVING COUNT(*) > 1;

-- 0.3 ¿El indice ya existe? Esperado: 0 filas.
SELECT i.INDEX_NAME, i.UNIQUENESS
  FROM ALL_INDEXES i
 WHERE i.OWNER = 'CRD' AND i.INDEX_NAME = 'UX_TPDJ_NOMBRE_ACTIVO';


-- =====================================================================================
-- 1. EL INDICE
-- =====================================================================================
-- ⭐ Es un indice unico FUNCIONAL Y PARCIAL, y las dos cosas son deliberadas:
--
--   - UPPER(TRIM(...)) normaliza: 'certificado bancario', 'CERTIFICADO BANCARIO ' y
--     'Certificado Bancario' colisionan entre si. Sin el TRIM, un espacio al final
--     burlaria el indice y el backend igual no encontraria la fila (su igualdad es
--     exacta), que es el peor de los dos mundos: parece cargado y no funciona.
--
--   - El CASE deja fuera a las INACTIVAS. La expresion da NULL cuando TPDJIDST <> 1, y
--     Oracle NO indexa las filas cuya clave es enteramente NULL. Por eso pueden convivir
--     el 37 y el 38 (los dos duplicados desactivados) con el 4 activo, sin conflicto.
--     Un UNIQUE sobre el nombre a secas obligaria a BORRAR los duplicados historicos, y
--     borrar un tipo con adjuntos colgando es justo lo que no se quiere hacer.

CREATE UNIQUE INDEX CRD.UX_TPDJ_NOMBRE_ACTIVO
    ON CRD.TPDJ (CASE WHEN TPDJIDST = 1 THEN UPPER(TRIM(TPDJNMBR)) END);


-- =====================================================================================
-- 2. CONTROLES POSTERIORES
-- =====================================================================================

-- 2.1 El indice quedo. Esperado: 1 fila, UNIQUENESS = UNIQUE.
SELECT i.INDEX_NAME, i.UNIQUENESS, i.STATUS
  FROM ALL_INDEXES i
 WHERE i.OWNER = 'CRD' AND i.INDEX_NAME = 'UX_TPDJ_NOMBRE_ACTIVO';

-- 2.2 ⭐ LA PRUEBA DE VERDAD: intentar duplicar y que la base lo RECHACE.
--     Esperado: ORA-00001 (restriccion unica violada). ESE ERROR ES EL EXITO.
--     Va con ROLLBACK a proposito: no deja nada.
--
-- INSERT INTO CRD.TPDJ (TPDJNMBR, TPDJIDST) VALUES ('certificado bancario  ', 1);
-- ROLLBACK;
--
-- ⚠️ Si esa linea INSERTA en vez de fallar, el indice no esta cumpliendo su funcion:
--    PARAR y avisar. Un control que no se prueba no es un control — es la misma leccion
--    del 234, donde el control del script de carga estaba escrito y no impedia nada.

-- 2.3 Y que el estado siga siendo el bueno: exactamente 1 activo. Esperado: 1.
SELECT COUNT(*) AS ACTIVOS_CERTIFICADO_BANCARIO
  FROM CRD.TPDJ t
 WHERE UPPER(TRIM(t.TPDJNMBR)) = 'CERTIFICADO BANCARIO'
   AND t.TPDJIDST = 1;


-- =====================================================================================
-- 3. QUE CAMBIA PARA EL USUARIO DESPUES DE ESTO
-- =====================================================================================
-- En la pantalla de Parametrizacion -> Tipos (CRD), crear un tipo con un nombre que ya
-- existe activo va a devolver un error del servidor (500, ORA-00001) en vez de crearlo
-- en silencio. Es feo pero es SEGURO: hoy lo crea sin avisar y rompe los certificados
-- de 273 cuentas.
--
-- ⇒ PENDIENTE DE CODIGO, no de datos (anotado en el tablero): TipoAdjuntoRest deberia
--   chequear el nombre repetido y responder 409 con un mensaje claro, como ya hace
--   /rest/cbbp con el beneficiario duplicado. El indice es la red que NO depende de que
--   ese codigo se escriba; el 409 es la cortesia que hace entendible el rechazo.


-- =====================================================================================
-- 4. REVERSO — comentado
-- =====================================================================================
-- DROP INDEX CRD.UX_TPDJ_NOMBRE_ACTIVO;
--
-- No toca ni una fila: es solo un indice.

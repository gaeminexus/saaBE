-- =====================================================================================
-- 243 - URGENTE: CK_PGPC_VLRR exige > 0 y el diseño del seguro fija 0 a proposito
-- FECHA: 2026-09-22 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- Sin comandos de SQL*Plus. Comentarios ARRIBA, nunca intercalados.
-- ⛔ ESTE SCRIPT ESCRIBE, pero SOLO en el bloque 2, que esta COMENTADO.
-- ⛔ VA ANTES DEL WAR que trae el fix de PGPCVLRR. Si el WAR sube primero, el proceso
--    de seguro vuelve a fallar — con otro error, pero para los mismos jubilados.
--
-- =====================================================================================
-- LA CONTRADICCION, MEDIDA EN LAS DOS PUNTAS
-- =====================================================================================
-- LA BASE (crd/sql/97 linea 93):
--     ALTER TABLE CRD.PGPC ADD CONSTRAINT CK_PGPC_VLRR CHECK (PGPCVLRR > 0);
--   Estrictamente mayor a cero. No admite una fila con total 0.
--
-- EL CODIGO (PagoPensionComplementariaServiceImpl, comentario que ya estaba ahi):
--     "La fila se graba SIEMPRE, aunque seguroFijado sea 0.0 (§11.1): asi la corrida la
--      reconoce como fijada y no la trata como SIN_SEGURO_DEL_PERIODO."
--   Y seguroFijado = 0.0 es un resultado LEGITIMO en tres ramas documentadas del propio
--   metodo: SIN_ANCLA, AL_DIA, y el tope por saldo cuando el saldo llega a cero.
--
-- ⇒ Las dos reglas no pueden ser ciertas a la vez. Hoy la contradiccion estaba dormida
--   porque el metodo NUNCA completaba PGPCVLRR (lo dejaba null) y moria antes, con el
--   ORA-01400. Al corregir eso, la fila llega al CHECK y muere con ORA-02290.
--
-- ⭐ CUANTO IMPORTA, con los numeros de hoy: de 182 jubilados del padron, solo OCHO
--   tienen seguro configurado mayor a cero (sql/237 bloque 5). Los otros 174 fijarian
--   seguro 0 ⇒ **el proceso seguiria fallando para 174 de 182**, sin este script.
--
-- Lo encontro el ejecutor BE leyendo el DDL entero en vez de sólo la columna que yo le
-- habia señalado. Mi despacho no lo mencionaba.
--
-- =====================================================================================
-- LA DECISION, Y POR QUE NO ES LA OTRA
-- =====================================================================================
-- Se RELAJA el CHECK a >= 0. Las alternativas, y por que se descartan:
--
--   a) No persistir la fila cuando el total da 0.
--      ⛔ Rompe la idempotencia que el propio codigo busca: sin fila, el reintento vuelve
--      a procesar a ese jubilado y generarMesesRetroactivos lo trata como
--      SIN_SEGURO_DEL_PERIODO. Es el comportamiento que el comentario de §11.1 puso ahi
--      a proposito.
--
--   b) Dejar el CHECK y que el seguro 0 no se grabe nunca.
--      ⛔ Es (a) con otro nombre, y ademas pierde el dato de "a este jubilado se lo
--      evaluo y no le correspondia cobrar", que es informacion, no ausencia de ella.
--
-- ⇒ El CHECK viene del script 97, de cuando CRD.PGPC guardaba SOLO pagos de pension, que
--   siempre son mayores a cero. El seguro fijado en 0 es semantica NUEVA (§11.1 / H60,
--   septiembre 2026): "evaluado, no corresponde cobrar". **La restriccion quedo vieja
--   respecto del diseño, no al reves.**
--
-- ⚠️ Lo que NO se relaja: sigue prohibido el negativo. Un total menor a cero seria un
--    error real y el CHECK lo tiene que seguir atajando.
-- =====================================================================================


-- =====================================================================================
-- 1. CONTROLES PREVIOS
-- =====================================================================================

-- 1.1 ¿El CHECK existe de verdad en la base? El DDL del repo no prueba que se corrio.
--     Esperado: 1 fila, con SEARCH_CONDITION "PGPCVLRR > 0" y STATUS ENABLED.
--     Si devuelve 0 filas, PARAR: el CHECK no existe, no hay nada que relajar, y el
--     ORA-02290 no puede pasar — avisar, porque entonces el diagnostico es otro.
SELECT c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.SEARCH_CONDITION, c.STATUS
  FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'PGPC' AND c.CONSTRAINT_TYPE = 'C'
   AND c.CONSTRAINT_NAME = 'CK_PGPC_VLRR';

-- 1.2 ¿Hay filas que ya violarian el CHECK nuevo (negativas)? Esperado: 0.
SELECT COUNT(*) AS FILAS_NEGATIVAS
  FROM CRD.PGPC p
 WHERE p.PGPCVLRR < 0;

-- 1.3 Foto del reparto actual de valores, para saber que hay hoy.
SELECT SUM(CASE WHEN p.PGPCVLRR = 0 THEN 1 ELSE 0 END)                AS EN_CERO,
       SUM(CASE WHEN p.PGPCVLRR > 0 THEN 1 ELSE 0 END)                AS MAYORES_A_CERO,
       COUNT(*)                                                       AS TOTAL
  FROM CRD.PGPC p;


-- =====================================================================================
-- 2. EL CAMBIO — COMENTADO. Descomentar despues de leer el bloque 1.
-- =====================================================================================
-- Se borra y se vuelve a crear con la condicion relajada. Oracle no permite modificar la
-- condicion de un CHECK en su lugar.
--
-- ⚠️ Entre el DROP y el ADD la tabla queda un instante sin la proteccion. Correr los dos
--    juntos, no uno hoy y otro mañana.
--
-- ALTER TABLE CRD.PGPC DROP CONSTRAINT CK_PGPC_VLRR;
--
-- ALTER TABLE CRD.PGPC ADD CONSTRAINT CK_PGPC_VLRR CHECK (PGPCVLRR >= 0);
--
-- (no lleva COMMIT: el DDL en Oracle confirma solo)


-- =====================================================================================
-- 3. CONTROLES POSTERIORES
-- =====================================================================================

-- 3.1 Esperado: 1 fila, SEARCH_CONDITION "PGPCVLRR >= 0", STATUS ENABLED.
SELECT c.CONSTRAINT_NAME, c.SEARCH_CONDITION, c.STATUS
  FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'PGPC'
   AND c.CONSTRAINT_NAME = 'CK_PGPC_VLRR';

-- 3.2 La prueba de que sigue atajando lo que importa. Esperado: ORA-02290.
--     ESE ERROR ES EL EXITO. Va con ROLLBACK: no deja nada.
--     ⚠️ Un control que no se prueba no es un control (leccion del sql/240).
--
-- INSERT INTO CRD.PGPC (ENTDCDGO, PGPCANNO, PGPCMESS, PGPCVLRR, PGPCESTD)
-- VALUES ((SELECT MIN(ENTDCDGO) FROM CRD.ENTD), 1900, 1, -1, 1);
-- ROLLBACK;


-- =====================================================================================
-- 4. REVERSO — comentado
-- =====================================================================================
-- ⛔ Volver a > 0 SOLO si antes no quedo ninguna fila en cero, o el ADD falla.
--
-- ALTER TABLE CRD.PGPC DROP CONSTRAINT CK_PGPC_VLRR;
-- ALTER TABLE CRD.PGPC ADD CONSTRAINT CK_PGPC_VLRR CHECK (PGPCVLRR > 0);


-- =====================================================================================
-- 5. ORDEN DE TODO LO DE HOY PARA EL SEGURO MEDICO
-- =====================================================================================
--   1. ESTE script (243) — relajar el CHECK.
--   2. WAR con el fix de PGPCVLRR.
--   3. sql/242 — reabrir la corrida de 9/2026, que quedo cerrada sin pagar nada.
--   4. Reintentar la generacion del seguro desde la pantalla.
--
-- ⚠️ Saltarse el 1 hace que el 4 falle para 174 de 182 jubilados. Saltarse el 3 hace que
--    el 4 ni siquiera arranque ("ya se genero").
-- =====================================================================================


-- =====================================================================================
-- ✅ CONTROLES PREVIOS CORRIDOS EN PRODUCCION — 2026-09-22
-- =====================================================================================
-- 1.1 -> CK_PGPC_VLRR | C | "PGPCVLRR > 0" | ENABLED
--        El CHECK existe de verdad y esta activo: la contradiccion es real, no del repo.
--
-- 1.2 -> 0 filas negativas. Nada que arreglar antes de relajar.
--
-- 1.3 -> EN_CERO 0 · MAYORES_A_CERO 225 · TOTAL 225
--        ⭐ Ninguna fila existente queda afectada por el cambio, y el dato confirma el
--        razonamiento de este script: las 225 filas son de corridas anteriores, todas
--        pagos de pension REALES y por eso todas mayores a cero. El CHECK nunca estorbo
--        porque hasta hoy CRD.PGPC solo guardaba eso. El seguro fijado en 0 —"evaluado,
--        no corresponde cobrar"— es la semantica nueva que la restriccion no contemplaba.
--
-- ⇒ Las tres condiciones del bloque 2 se cumplen. El cambio es seguro.
-- =====================================================================================

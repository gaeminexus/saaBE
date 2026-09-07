-- =====================================================================
-- INSERTA los 3 comandos de busqueda que faltan en SCP.PDTR (rubro alt. 71)
-- Modulo: transversal (SCP)  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- DE DONDE SALE ESTO
--   El e2-08 corrido en produccion devolvio que faltan los alternos
--   12 (IS_NULL), 13 (ABRE_PARENTESIS) y 14 (CIERRA_PARENTESIS).
--   Esa es la causa del WFLYEJB0034 / NoResultException de
--   selectValorStringByRubAltDetAlt que revienta selectByCriteria.
--
-- ⭐ LOS VALORES NO SE INVENTARON. Salen de leer el unico consumidor,
--   com.saa.basico.utilImpl.EntityDaoImpl.selectByCriteria:
--
--     :161-163  CIERRA_PARENTESIS -> se concatena a la consulta tal cual  -> ")"
--     :170-173  ABRE_PARENTESIS   -> idem, antes de la condicion          -> "("
--
-- ⚠️ Y UN HALLAZGO QUE CAMBIA EL ALCANCE: el 12 (IS_NULL) NO SE LEE.
--   EntityDaoImpl:184-186 tiene el texto HARDCODEADO en Java:
--
--       if (TipoComandosBusqueda.IS_NULL == aBuscar.getTipoComparacion()) {
--           strQuery = strQuery + " IS NULL ";
--
--   Nunca llama al catalogo para ese caso: la rama que consulta
--   selectValorStringByRubAltDetAlt es la del ELSE. Asi que la fila 12 NO es la
--   que estaba causando el error, y el sistema funcionaria sin ella.
--
--   SE INSERTA IGUAL, por dos razones: deja el catalogo completo respecto de
--   com.saa.rubros.TipoComandosBusqueda (los 15 alternos), y si alguna vez
--   alguien mueve ese IS NULL hardcodeado al catalogo, la fila ya esta. Es una
--   fila inerte hoy, y esta dicho aca para que nadie la crea activa.
--
--   ⇒ LAS QUE ARREGLAN EL ERROR SON LA 13 Y LA 14. Son las que usa cualquier
--     pantalla que arme criterios con parentesis.
--
-- ⛔ ANTES DE CORRER: SCP.PRBR / SCP.PDTR es CATALOGO COMPARTIDO por todos los
--   equipos (docs/logica-negocio/REGISTRO-RESERVAS-EQUIPOS.md). Este script NO
--   crea rubros nuevos ni toma PRBRCDGO/PDTRCDGO fijos: agrega detalles a un
--   rubro que YA EXISTE (el 71) y toma el PK de la secuencia. No consume ningun
--   rango reservado y no puede colisionar con otro equipo.
--
-- Columnas verificadas contra la entidad DetalleRubro (no supuestas):
--   PDTRCDGO PK · PRBRCDGO FK al rubro · PDTRDSCR descripcion
--   PDTRVLRN valor numerico · PDTRVLRV valor alfanumerico
--   PDTRALTR codigo alterno · PDTRESTD estado
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 -- CONTROLES ANTES. Solo lectura. Correr y LEER.
-- =====================================================================

-- 0.1 El rubro 71 existe y su PK. ESPERADO: 1 fila. Anotar el PRBRCDGO.
SELECT PRBRCDGO, PRBRALTR, PRBRDSCR
  FROM SCP.PRBR
 WHERE PRBRALTR = 71;

-- 0.2 ⭐ COMO SON LAS 12 FILAS QUE SI EXISTEN. Correr y MIRAR antes de insertar.
--     Sirve para copiar el patron real de PDTRVLRN y PDTRDSCR en vez de
--     suponerlo. Si las existentes usan otra convencion que la de este script,
--     PARAR y ajustar los INSERT de abajo para que queden homogeneas.
SELECT d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRV, d.PDTRVLRN, d.PDTRESTD
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 71
 ORDER BY d.PDTRALTR;

-- 0.3 Confirmar que 12, 13 y 14 NO estan. ESPERADO: 0 filas.
--     Si devuelve alguna, ese INSERT se saltea: ya existe.
SELECT d.PDTRALTR, d.PDTRVLRV
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 71 AND d.PDTRALTR IN (12, 13, 14);


-- =====================================================================
-- BLOQUE 1 -- LOS TRES INSERT
--   El PRBRCDGO se resuelve con un subselect por PRBRALTR = 71, asi no hay que
--   pegar a mano el numero que devolvio el 0.1 ni equivocarse al copiarlo.
-- =====================================================================

-- 13 -- ABRE_PARENTESIS -> "("   ⭐ una de las dos que arreglan el error
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRALTR, PDTRVLRN, PDTRVLRV, PDTRDSCR, PDTRESTD)
VALUES (SCP.SQ_PDTRCDGO.NEXTVAL,
        (SELECT PRBRCDGO FROM SCP.PRBR WHERE PRBRALTR = 71),
        13, 13, '(', 'Abre parentesis', 1);

-- 14 -- CIERRA_PARENTESIS -> ")"   ⭐ la otra
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRALTR, PDTRVLRN, PDTRVLRV, PDTRDSCR, PDTRESTD)
VALUES (SCP.SQ_PDTRCDGO.NEXTVAL,
        (SELECT PRBRCDGO FROM SCP.PRBR WHERE PRBRALTR = 71),
        14, 14, ')', 'Cierra parentesis', 1);

-- 12 -- IS_NULL -> "IS NULL"
--   ⚠️ HOY ESTA FILA ES INERTE: EntityDaoImpl:184-186 tiene el "IS NULL"
--   hardcodeado y nunca consulta el catalogo para este caso. Se inserta para
--   dejar el catalogo completo respecto de TipoComandosBusqueda, no porque
--   haga falta para que algo funcione.
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRALTR, PDTRVLRN, PDTRVLRV, PDTRDSCR, PDTRESTD)
VALUES (SCP.SQ_PDTRCDGO.NEXTVAL,
        (SELECT PRBRCDGO FROM SCP.PRBR WHERE PRBRALTR = 71),
        12, 12, 'IS NULL', 'Es nulo', 1);

COMMIT;


-- =====================================================================
-- BLOQUE 2 -- SINCRONIZAR LA SECUENCIA
--   Este script usa NEXTVAL, asi que la secuencia avanza sola y NO hace falta
--   sincronizarla. El control esta igual porque la regla de la casa lo pide y
--   porque si alguna vez alguien mete filas con PK explicita, este es el lugar
--   donde se nota.
--   ESPERADO: LAST_NUMBER de la secuencia > MAX(PDTRCDGO).
-- =====================================================================
SELECT (SELECT MAX(PDTRCDGO) FROM SCP.PDTR)              AS MAX_PDTRCDGO,
       (SELECT last_number FROM all_sequences
         WHERE sequence_owner = 'SCP' AND sequence_name = 'SQ_PDTRCDGO') AS SECUENCIA
  FROM DUAL;


-- =====================================================================
-- BLOQUE 3 -- CONTROLES DESPUES. Correr y LEER.
-- =====================================================================

-- 3.1 ⭐ LOS 15 COMPLETOS. ESPERADO: 15 filas, todas con DIAGNOSTICO = 'OK'.
--     Es el mismo bloque 2 del e2-08: si ahora sale limpio, el problema esta
--     resuelto.
WITH ESPERADOS AS (
    SELECT  0 AS ALT, 'RAIZ'               AS NOMBRE FROM DUAL UNION ALL
    SELECT  1, 'IGUAL'                                FROM DUAL UNION ALL
    SELECT  2, 'DIFERENTE'                            FROM DUAL UNION ALL
    SELECT  3, 'MAYOR'                                FROM DUAL UNION ALL
    SELECT  4, 'MAYOR_IGUAL'                          FROM DUAL UNION ALL
    SELECT  5, 'MENOR'                                FROM DUAL UNION ALL
    SELECT  6, 'MENOR_IGUAL'                          FROM DUAL UNION ALL
    SELECT  7, 'BETWEEN'                              FROM DUAL UNION ALL
    SELECT  8, 'TRUNCADO'                             FROM DUAL UNION ALL
    SELECT  9, 'LIKE'                                 FROM DUAL UNION ALL
    SELECT 10, 'AND'                                  FROM DUAL UNION ALL
    SELECT 11, 'OR'                                   FROM DUAL UNION ALL
    SELECT 12, 'IS_NULL'                              FROM DUAL UNION ALL
    SELECT 13, 'ABRE_PARENTESIS'                      FROM DUAL UNION ALL
    SELECT 14, 'CIERRA_PARENTESIS'                    FROM DUAL
)
SELECT  e.ALT AS CODIGO_ALTERNO, e.NOMBRE AS CONSTANTE_JAVA,
        d.PDTRVLRV AS VALOR_EN_BASE, d.PDTRESTD AS ESTADO_FILA,
        CASE WHEN d.PDTRCDGO IS NULL THEN '*** FALTA ***'
             WHEN d.PDTRVLRV IS NULL THEN '*** SIN VALOR ***'
             ELSE 'OK' END AS DIAGNOSTICO
  FROM ESPERADOS e
  LEFT JOIN SCP.PRBR r ON r.PRBRALTR = 71
  LEFT JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO AND d.PDTRALTR = e.ALT
 ORDER BY e.ALT;

-- 3.2 Prueba funcional: la pantalla que fallaba (CuentaBancariaParticipe, crd)
--     tiene que dejar de dar WFLYEJB0034. Eso se prueba en la aplicacion, no
--     aca. Avisar al equipo de crd cuando este corrido.


-- =====================================================================
-- REVERSO -- comentado a proposito.
-- =====================================================================
-- DELETE FROM SCP.PDTR
--  WHERE PDTRALTR IN (12, 13, 14)
--    AND PRBRCDGO = (SELECT PRBRCDGO FROM SCP.PRBR WHERE PRBRALTR = 71);
-- COMMIT;

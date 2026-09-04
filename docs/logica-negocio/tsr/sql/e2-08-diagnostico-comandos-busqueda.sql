-- =====================================================================
-- DIAGNOSTICO: falta una fila del catalogo de COMANDOS DE BUSQUEDA
-- Modulo: transversal (SCP)  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-04
--
-- EL SINTOMA, reportado por el usuario en produccion
--   WFLYEJB0034 ... DetalleRubroDaoService.selectValorStringByRubAltDetAlt(int,int)
--   jakarta.ejb.EJBTransactionRolledbackException:
--     No result found for query [ select t.valorAlfanumerico from DetalleRubro t
--       where t.rubro.codigoAlterno = :codigoAlternoRubro
--         and t.codigoAlterno = :codigoAlternoDetalle ]
--
-- LA CAUSA, y esta documentada en CLAUDE.md
--   selectByCriteria de EntityDaoImpl NO tiene los operadores JPQL en el codigo:
--   los LEE DE LA BASE. Cada "and", "like", "between", "(" sale de una fila de
--   SCP.PDTR bajo el rubro de codigo alterno 71 (Rubros.TIPO_COMANDOS_BUSQUEDA).
--   Si falta UNA fila, selectByCriteria revienta con NoResultException — no
--   devuelve vacio, tumba la transaccion entera.
--
--   Por eso el error NO nombra la pantalla: nombra DetalleRubroDaoServiceImpl,
--   que es el ultimo eslabon. La pantalla que lo disparo esta en las lineas de
--   log inmediatamente anteriores ("selectByCriteria de CNBP" en el reporte del
--   usuario = CuentaBancariaParticipe, del modulo crd).
--
-- QUE HACE ESTE SCRIPT
--   NADA. Es 100% SOLO LECTURA. Ningun INSERT, UPDATE, DELETE ni DDL.
--   Seguro de correr de corrido, en local y en produccion.
-- =====================================================================


-- =====================================================================
-- 1 -- ¿Existe el rubro 71?
--      ESPERADO: exactamente 1 fila. Si devuelve 0, el problema no es una fila
--      suelta: falta el catalogo entero y NINGUNA busqueda por criterios del
--      sistema funciona.
-- =====================================================================
SELECT PRBRCDGO, PRBRALTR, PRBRNMBR
  FROM SCP.PRBR
 WHERE PRBRALTR = 71;


-- =====================================================================
-- 2 -- ⭐ EL CONTROL QUE RESPONDE LA PREGUNTA: cual de los 15 falta.
--      Los 15 codigos salen de com.saa.rubros.TipoComandosBusqueda.
--      ESPERADO: la columna ESTADO dice 'OK' en las 15 filas.
--      Cualquier 'FALTA' es la causa del error.
-- =====================================================================
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
SELECT  e.ALT                        AS CODIGO_ALTERNO,
        e.NOMBRE                     AS CONSTANTE_JAVA,
        d.PDTRVLAL                   AS VALOR_EN_BASE,
        CASE WHEN d.PDTRCDGO IS NULL THEN '*** FALTA ***'
             WHEN d.PDTRVLAL IS NULL THEN '*** EXISTE PERO SIN VALOR ***'
             ELSE 'OK' END           AS ESTADO
  FROM ESPERADOS e
  LEFT JOIN SCP.PRBR r ON r.PRBRALTR = 71
  LEFT JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO AND d.PDTRALTR = e.ALT
 ORDER BY e.ALT;


-- =====================================================================
-- 3 -- Lo que SI hay hoy bajo el rubro 71, tal cual.
--      Sirve para comparar contra local: las dos bases arrancan iguales, asi
--      que una diferencia es una fila que se cargo de un lado y no del otro.
-- =====================================================================
SELECT d.PDTRCDGO, d.PDTRALTR, d.PDTRNMBR, d.PDTRVLAL, d.PDTRESTD
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 71
 ORDER BY d.PDTRALTR;


-- =====================================================================
-- 4 -- ¿Hay filas INACTIVAS? Una fila que existe pero esta dada de baja
--      puede comportarse igual que una ausente segun como filtre la consulta.
--      ESPERADO: 0 filas.
-- =====================================================================
SELECT d.PDTRALTR, d.PDTRNMBR, d.PDTRESTD
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 71
   AND (d.PDTRESTD IS NULL OR d.PDTRESTD <> 1);


-- =====================================================================
-- QUE HACER CON EL RESULTADO
--
--   El bloque 2 dice exactamente que fila falta. La correccion es un INSERT en
--   SCP.PDTR, PERO:
--
--   ⛔ SCP.PRBR / SCP.PDTR son CATALOGO COMPARTIDO entre todos los equipos y
--      estan gobernados por docs/logica-negocio/REGISTRO-RESERVAS-EQUIPOS.md.
--      El INSERT no se escribe hasta saber que fila es, y se anota en el
--      registro antes de correrlo.
--
--   ⚠️ Y el valor NO se inventa: se copia textual del de la base LOCAL, que es
--      copia de produccion y donde la busqueda si funciona. Un operador JPQL
--      mal escrito (un 'and' con espacios de mas, un 'like' en mayusculas) no
--      da error de catalogo: da una consulta JPQL invalida mas adelante, que es
--      mas dificil de rastrear que la fila ausente.
-- =====================================================================

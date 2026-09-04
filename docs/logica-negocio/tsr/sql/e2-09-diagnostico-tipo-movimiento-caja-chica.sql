-- =====================================================================
-- DIAGNOSTICO: la columna Tipo de movimientos de caja chica sale vacia
-- Modulo: TSR  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-04
--
-- POR QUE EXISTE
--   El usuario ve el chip "Tipo —" en TODAS las filas de la pantalla de gastos,
--   y por eso el boton de anular no aparece (puedeAnular exige esGasto).
--
--   CAUSA YA CONFIRMADA: el frontend leia rubroTipoMovimientoH/P, campos que el
--   backend nunca manda (manda "tipo"). Ver ESTADO-EQUIPO-OMEN-2.md 17.
--   El arreglo del frontend esta hecho.
--
--   ESTE SCRIPT DESCARTA LA OTRA MITAD: que ademas MVCHTIPO este NULL o con un
--   valor fuera del catalogo en la base. Si eso pasara, arreglar el frontend es
--   necesario pero NO suficiente, y la pantalla seguiria mostrando "Tipo —"
--   despues de desplegar. Vale la pena saberlo ANTES del proximo despliegue.
--
-- QUE HACE
--   NADA. Es 100% SOLO LECTURA. Ningun INSERT, UPDATE, DELETE ni DDL.
--   Seguro de correr de corrido, en local y en produccion.
-- =====================================================================


-- =====================================================================
-- 1 -- ⭐ EL CONTROL QUE DECIDE. Distribucion de MVCHTIPO.
--      Los valores validos salen de com.saa.rubros.TipoMovimientoCajaChica:
--        1=APERTURA  2=GASTO  3=REPOSICION  4=AJUSTE_MAS  5=AJUSTE_MENOS
--
--      ESPERADO: todas las filas con DIAGNOSTICO = 'OK'.
--      Si aparece 'NULL' o 'FUERA DE CATALOGO', el problema NO se arregla solo
--      con el frontend: PARAR Y AVISAR AL ARBITRO antes de desplegar.
-- =====================================================================
SELECT  m.MVCHTIPO                       AS TIPO,
        CASE m.MVCHTIPO
             WHEN 1 THEN 'APERTURA'
             WHEN 2 THEN 'GASTO'
             WHEN 3 THEN 'REPOSICION'
             WHEN 4 THEN 'AJUSTE_MAS'
             WHEN 5 THEN 'AJUSTE_MENOS'
             ELSE NULL END               AS NOMBRE,
        COUNT(*)                         AS FILAS,
        CASE WHEN m.MVCHTIPO IS NULL           THEN '*** NULL ***'
             WHEN m.MVCHTIPO NOT IN (1,2,3,4,5) THEN '*** FUERA DE CATALOGO ***'
             ELSE 'OK' END               AS DIAGNOSTICO
  FROM TSR.MVCH m
 GROUP BY m.MVCHTIPO
 ORDER BY m.MVCHTIPO;


-- =====================================================================
-- 2 -- Lo mismo pero solo de la caja que el usuario esta mirando (id 1),
--      que es la de la captura. Por si el problema fuera de una caja sola.
-- =====================================================================
SELECT  m.MVCHTIPO AS TIPO, m.MVCHESTD AS ESTADO, COUNT(*) AS FILAS
  FROM TSR.MVCH m
 WHERE m.CJCHCDGO = 1
 GROUP BY m.MVCHTIPO, m.MVCHESTD
 ORDER BY m.MVCHTIPO, m.MVCHESTD;


-- =====================================================================
-- 3 -- Las filas concretas de la captura, para cruzarlas una a una.
--      ESPERADO: MVCHTIPO = 2 (GASTO) y MVCHESTD = 1 (activo) en todas.
--      Si TIPO=2 y ESTADO=1, entonces el backend manda bien el dato y el unico
--      problema era el frontend: desplegarlo alcanza.
-- =====================================================================
SELECT  m.MVCHCDGO      AS ID,
        m.MVCHFCHA      AS FECHA,
        m.MVCHTIPO      AS TIPO,
        m.MVCHESTD      AS ESTADO,
        m.MVCHVLOR      AS VALOR,
        SUBSTR(m.MVCHDSCR, 1, 60) AS CONCEPTO
  FROM TSR.MVCH m
 WHERE m.CJCHCDGO = 1
   AND m.MVCHFCHA >= DATE '2026-08-01'
 ORDER BY m.MVCHFCHA, m.MVCHCDGO;


-- =====================================================================
-- 4 -- El catalogo de tipos existe? (rubro 232, creado por tsr/sql/02)
--      Es informativo: el backend NO lee este catalogo para resolver el tipo
--      —usa la constante Java— asi que su ausencia no explicaria el sintoma.
--      Sirve para saber si el combo "Tipo" del filtro tiene con que llenarse.
-- =====================================================================
SELECT d.PDTRALTR, d.PDTRNMBR, d.PDTRESTD
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 232
 ORDER BY d.PDTRALTR;


-- =====================================================================
-- COMO LEER EL RESULTADO
--
--   Bloque 1 y 3 en 'OK' con TIPO=2  -> el dato esta bien en la base. El unico
--   problema era el frontend leyendo el nombre equivocado; desplegar el build
--   nuevo de saaFE alcanza y el boton de anular aparece.
--
--   Cualquier NULL o valor fuera de 1..5 -> hay ademas un problema de datos.
--   NO desplegar y avisar: haria falta un UPDATE de correccion, que se escribe
--   aparte y no va en este script.
-- =====================================================================

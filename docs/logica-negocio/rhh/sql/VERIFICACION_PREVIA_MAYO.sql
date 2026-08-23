-- =====================================================
-- MODULO: RHH - MAYO: EL RANGO DEL PERIODO Y LA BASE DE ASIENTOS
-- DESCRIPCION: Solo lectura. Se corre con el periodo de mayo recien creado
--              y ANTES de registrar la primera de las ocho novedades.
-- FECHA: 2026-08-23
-- PARAMETRO: ninguno
-- =====================================================
-- Con un rango que no sea el mes, calcularPeriodo NO revienta: calcula, y
-- prorratea a todo el mundo por los dias del rango. Por eso va antes.
-- =====================================================


-- =====================================================
-- CONTROL 1: LOS CINCO PERIODOS DE 2026.
-- Mayo tiene que salir OK, en estado 1 ABIERTO, y con su PRDNCDGO, que es
-- el dato que el frontend necesita para las consultas de su §3.
-- Los cuatro anteriores en estado 7.
-- .
-- LOS CUATRO VEREDICTOS NO SE ARREGLAN IGUAL:
--   RANGO MALO -> borrar el periodo y rehacerlo.
--   MODO       -> corregir en sitio. calcularPeriodo no lee el modo.
--   TIPO       -> corregir en sitio. NADIE lee PRDNTPNM, asi que un tipo
--                 equivocado no rompe nada y por eso no lo caza ningun
--                 control posterior: se quedaria mal para siempre.
-- =====================================================
SELECT PRDNCDGO, PRDNANOO AS ANIO, PRDNMSEE AS MES, PRDNFCHI, PRDNFCHF,
       PRDNMODO AS MODO, PRDNTPNM AS TIPO, PRDNESTD AS ESTADO,
       CASE WHEN PRDNMODO IS NULL THEN '*** MODO NULO: CORREGIR ANTES DE CERRAR, NO BORRAR ***'
            WHEN PRDNMODO <> 1    THEN '*** MODO ' || PRDNMODO || ', NO ES HISTORICO: CORREGIR EN SITIO ***'
            WHEN PRDNTPNM <> 1    THEN '*** TIPO ' || PRDNTPNM || ', NO ES MENSUAL: CORREGIR EN SITIO ***'
            WHEN EXTRACT(MONTH FROM PRDNFCHI) = PRDNMSEE
             AND EXTRACT(MONTH FROM PRDNFCHF) = PRDNMSEE
             AND EXTRACT(YEAR  FROM PRDNFCHI) = PRDNANOO
             AND EXTRACT(YEAR  FROM PRDNFCHF) = PRDNANOO
            THEN 'OK'
            ELSE '*** RANGO MALO: BORRAR EL PERIODO Y REHACERLO ***' END AS VEREDICTO
  FROM RHH.PRDN
 WHERE PRDNANOO = 2026
 ORDER BY PRDNMSEE;


-- =====================================================
-- CONTROL 2: LA BASE DE ASIENTOS DE MAYO.
-- El 8179 era la de abril y ya no sirve. Se anota AHORA, no al final, y el
-- censo del cierre va acotado a mayor que este numero, NUNCA total: otros
-- modulos escriben en CNT.ASNT en paralelo.
-- =====================================================
SELECT MAX(ASNTCDGO) AS BASE_ASIENTOS_MAYO FROM CNT.ASNT;


-- =====================================================
-- CONTROL 3: CTRL_PARAM SE QUEDA EN 4. Debe decir 2026 / 4.
-- Se mueve a 5 en el paso 1 del §4 del guion, al ir a contrastar, y no
-- antes. Adelantarlo vacia los bloques; dejarlo atras contrasta ABRIL otra
-- vez y sale verde al centimo, que es peor. Lo delata PERIODO_LEIDO.
-- =====================================================
SELECT ANIO, MES,
       CASE WHEN ANIO = 2026 AND MES = 4 THEN 'OK, se queda asi'
            ELSE '*** REVISAR ***' END AS VEREDICTO
  FROM RHH.CTRL_PARAM;

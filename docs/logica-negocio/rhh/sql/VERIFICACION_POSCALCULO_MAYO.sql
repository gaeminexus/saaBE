-- =====================================================
-- MODULO: RHH - MAYO RECIEN CALCULADO, ANTES DE CONTRASTAR
-- DESCRIPCION: Solo lectura. Son las seis comprobaciones del §4 del guion
--              mas las dos que el guion no traia. El periodo 41 debe estar
--              en estado 3 CALCULADO y SIN aprobar.
-- FECHA: 2026-08-23
-- PARAMETRO: ninguno
-- =====================================================
-- ESTO VA ANTES DEL CONTRASTE, Y EL CONTRASTE ANTES DE APROBAR.
-- Con el periodo en 3 un fallo se arregla recalculando. Con el periodo
-- cerrado habria que reabrirlo, que es el punto 6 y reabrirPeriodo no avisa.
-- =====================================================


-- =====================================================
-- CONTROL 1: CABECERA. Estado 3, modo 1, 20 empleados, y los totales.
-- Neto esperado 16.035,21. Cliente 16.035,21. DIFERENCIA CERO.
-- OJO: el 16.035,21 es PREDICCION, no un numero visto: la unica corrida de
-- mayo que existe dio 16.015,04 con el motor viejo. La misma derivacion se
-- valido en abril, asi que gana credito, no certeza. El discriminador de
-- verdad es el bloque 2 del contraste --TRES filas--, nunca este total.
-- =====================================================
SELECT PRDNCDGO, PRDNESTD AS ESTADO, PRDNMODO AS MODO, PRDNNMEM AS EMPLEADOS,
       PRDNTTIN AS INGRESOS, PRDNTTDS AS DESCUENTOS, PRDNTTNT AS NETO,
       PRDNTTPT AS PATRONAL, PRDNASNT AS ASIENTO_ROL,
       CASE WHEN PRDNESTD = 3 THEN 'OK' ELSE '*** NO ESTA EN 3 CALCULADO ***' END AS VEREDICTO
  FROM RHH.PRDN
 WHERE PRDNANOO = 2026 AND PRDNMSEE = 5;


-- =====================================================
-- CONTROL 2: CABECERA CONTRA LA SUMA DE NMNA.
-- Es el punto 9: la cabecera se acumula EN MEMORIA sobre los contratos
-- procesados, no desde NMNA. Si divergen hay nominas huerfanas y la
-- cabecera NO lo delata sola. Las cuatro cifras tienen que coincidir.
-- =====================================================
SELECT p.PRDNNMEM AS CAB_EMPLEADOS, COUNT(n.NMNACDGO) AS DET_NOMINAS,
       p.PRDNTTIN AS CAB_INGRESOS,  SUM(n.NMNATING) AS DET_INGRESOS,
       p.PRDNTTDS AS CAB_DESCUENTOS,SUM(n.NMNATDSC) AS DET_DESCUENTOS,
       p.PRDNTTNT AS CAB_NETO,      SUM(n.NMNANETO) AS DET_NETO,
       CASE WHEN p.PRDNNMEM = COUNT(n.NMNACDGO)
             AND p.PRDNTTIN = SUM(n.NMNATING)
             AND p.PRDNTTDS = SUM(n.NMNATDSC)
             AND p.PRDNTTNT = SUM(n.NMNANETO)
            THEN 'OK' ELSE '*** CABECERA Y DETALLE DIVERGEN: PUNTO 9 ***' END AS VEREDICTO
  FROM RHH.PRDN p
  LEFT JOIN RHH.NMNA n ON n.PRDNCDGO = p.PRDNCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 5
 GROUP BY p.PRDNCDGO, p.PRDNNMEM, p.PRDNTTIN, p.PRDNTTDS, p.PRDNTTNT;


-- =====================================================
-- CONTROL 3: LOS DIAS. Es la prueba de la correccion 1, y no la prueba
-- el total: la prueban los dias. En abril NADIE tiene dias distintos de 30,
-- Mendez incluida, porque ya va a mes completo. Tiene que salir VACIO.
-- Si aparece un 16,4516 o cualquier decimal, el WAR no es el que toca.
-- =====================================================
SELECT m.MPLDIDNT, m.MPLDAPLL, n.NMNADITR AS DIAS
  FROM RHH.NMNA n
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 5
   AND n.NMNADITR <> 30;


-- =====================================================
-- CONTROL 4: NI UN RENGLON DE IMPUESTO A LA RENTA. Tiene que salir VACIO.
-- Se busca por ROL 8, no por alterno: el rol es lo que el motor consulta.
-- Si sale Robayo, o el WAR no lleva CNTENRIR o el 'S' no esta puesto.
-- =====================================================
SELECT m.MPLDIDNT, m.MPLDAPLL, c.CPNMNMBR AS CONCEPTO, r.RNGLVLRO AS VALOR
  FROM RHH.RNGL r
  JOIN RHH.NMNA n ON n.NMNACDGO = r.NMNACDGO
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
  JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 5
   AND c.CPNMROLM = 8;


-- =====================================================
-- CONTROL 5: MENDEZ TORRES. 30 dias, 482,00 / 45,55 / 436,45.
-- Es la fila que prueba que el sql/49 llego al calculo y no solo a la ficha.
-- =====================================================
SELECT m.MPLDIDNT, n.NMNADITR AS DIAS, n.NMNATING AS INGRESOS,
       n.NMNATDSC AS DESCUENTOS, n.NMNANETO AS NETO,
       CASE WHEN n.NMNADITR = 30 AND n.NMNATING = 482 AND n.NMNANETO = 436.45
            THEN 'OK' ELSE '*** REVISAR: SIGUE EN MEDIA JORNADA? ***' END AS VEREDICTO
  FROM RHH.NMNA n
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 5 AND m.MPLDIDNT = '1004350904';


-- =====================================================
-- CONTROL 6: LAS DIEZ NOVEDADES SE COBRARON DE VERDAD.
-- Que entren al filtro no prueba que generaran renglon. Se comparan los
-- subtotales del rol contra lo registrado.
-- Esperado: rol 12 -> 171,25  ·  rol 13 -> 1015,14  ·  rol 14 -> 1869,81
-- =====================================================
SELECT c.CPNMROLM AS ROL, c.CPNMNMBR AS CONCEPTO, COUNT(*) AS RENGLONES,
       SUM(r.RNGLVLRO) AS TOTAL_COBRADO
  FROM RHH.RNGL r
  JOIN RHH.NMNA n ON n.NMNACDGO = r.NMNACDGO
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 5
   AND c.CPNMROLM IN (12, 13, 14)
 GROUP BY c.CPNMROLM, c.CPNMNMBR
 ORDER BY c.CPNMROLM;


-- =====================================================
-- CONTROL 7: NINGUNA NOMINA HUERFANA NI REPETIDA. VACIO las dos.
-- =====================================================
SELECT 'REPETIDA' AS PROBLEMA, m.MPLDIDNT, m.MPLDAPLL, COUNT(*) AS VECES
  FROM RHH.NMNA n
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 5
 GROUP BY m.MPLDIDNT, m.MPLDAPLL
HAVING COUNT(*) > 1
UNION ALL
SELECT 'CESANTE CON NOMINA', m.MPLDIDNT, m.MPLDAPLL, 1
  FROM RHH.NMNA n
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 5 AND m.MPLDESTD = 4;


-- =====================================================
-- CONTROL 8: NO NACIO NINGUN ASIENTO. La base se anoto en 8179.
-- El censo total de CNT.ASNT no vale: otros modulos escriben en paralelo.
-- Lo que sale de aqui no puede ser de RRHH.
-- =====================================================
SELECT ASNTCDGO, ASNTFCHA, ASNTNMRO, ASNTUSRO, SUBSTR(ASNTOBSR, 1, 80) AS OBSERVACION
  FROM CNT.ASNT WHERE ASNTCDGO > 8179 ORDER BY ASNTCDGO;

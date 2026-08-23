-- =====================================================
-- MODULO: RHH - LAS OCHO NOVEDADES DE MAYO, ANTES DE CALCULAR
-- DESCRIPCION: Solo lectura. Se corre con las diez ya registradas y el
--              periodo 41 todavia sin calcular.
-- FECHA: 2026-08-23
-- PARAMETRO: :EMPRESA -- 1236
-- =====================================================
-- POR QUE NO BASTA CONTAR DIEZ FILAS.
--   - El motor exige DOS condiciones, no una (NovedadNominaDaoServiceImpl:58):
--     aprobada = 'S' AND estado = 1. Una novedad que falle cualquiera de las
--     dos se descarta EN SILENCIO y en la pantalla se ve igual que una buena.
--   - NVNMAPRB lleva DEFAULT 'N' en el DDL y NVNMESTD lleva DEFAULT 1, pero
--     el default de columna NO se dispara: JPA manda el valor explicito. Asi
--     que los dos valores peligrosos son los que la pantalla pone sola.
--   - El valor por defecto de Aprobada es justo el que hace invisible la fila.
--     Un combo que no toma el valor no deja rastro: deja el 'N'.
-- =====================================================


-- =====================================================
-- CONTROL 1: EL VEREDICTO, FILA A FILA. OCHO filas, todas ENTRA.
-- Cualquier otra cosa: parar y no calcular.
-- =====================================================
SELECT n.NVNMCDGO, c.CPNMALTR AS ALTERNO, c.CPNMNMBR AS CONCEPTO,
       m.MPLDIDNT, m.MPLDAPLL, n.NVNMVLRR AS VALOR,
       n.NVNMAPRB AS APROBADA, n.NVNMESTD AS ESTADO,
       CASE WHEN n.NVNMAPRB = 'S' AND n.NVNMESTD = 1 THEN 'ENTRA'
            WHEN n.NVNMAPRB IS NULL OR n.NVNMAPRB <> 'S'
                 THEN '*** APROBADA NO ES S: LA IGNORA. PARAR ***'
            ELSE '*** ESTADO NO ES 1: LA IGNORA. PARAR ***' END AS VEREDICTO
  FROM RHH.NVNM n
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
  LEFT JOIN RHH.CPNM c ON c.CPNMCDGO = n.CPNMCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 5
 ORDER BY c.CPNMALTR, m.MPLDAPLL;


-- =====================================================
-- CONTROL 2: LOS SUBTOTALES, PERO SOLO DE LAS QUE ENTRAN.
-- Sumar las diez daria el numero bueno aunque una no entrase: el subtotal
-- tiene que calcularse sobre el MISMO filtro que usa el motor.
-- Esperado: 23 -> 171,25  ·  24 -> 1015,14  ·  25 -> 1869,81
-- =====================================================
SELECT c.CPNMALTR AS ALTERNO, COUNT(*) AS FILAS, SUM(n.NVNMVLRR) AS SUBTOTAL
  FROM RHH.NVNM n
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.CPNM c ON c.CPNMCDGO = n.CPNMCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 5
   AND n.NVNMAPRB = 'S' AND n.NVNMESTD = 1
 GROUP BY c.CPNMALTR
 ORDER BY c.CPNMALTR;


-- =====================================================
-- CONTROL 3: NINGUNA A UN CESANTE (D18) Y NINGUNA REPETIDA.
-- Las dos tienen que devolver CERO filas.
-- Manosalvas y Calderon salen dos veces cada uno, pero con CONCEPTO
-- DISTINTO, por eso la clave del duplicado lleva el concepto.
-- =====================================================
SELECT 'CESANTE' AS PROBLEMA, m.MPLDIDNT, m.MPLDAPLL, n.NVNMVLRR
  FROM RHH.NVNM n
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 5
   AND m.MPLDESTD = 4
UNION ALL
SELECT 'REPETIDA', m.MPLDIDNT, m.MPLDAPLL, NULL
  FROM RHH.NVNM n
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 5
 GROUP BY m.MPLDIDNT, m.MPLDAPLL, n.CPNMCDGO
HAVING COUNT(*) > 1;


-- =====================================================
-- CONTROL 4: EL CATALOGO SIGUE LIMPIO.
-- Va por el tropiezo de la quinta fila, donde un importe se escribio DENTRO
-- del campo de concepto. El dialogo se cancelo y no deberia haber tocado
-- nada, pero un nombre de concepto con digitos pegados es barato de
-- descartar y caro de descubrir en agosto.
-- Los tres nombres tienen que salir limpios, sin cifras dentro.
-- =====================================================
SELECT CPNMALTR AS ALTERNO, CPNMCDGO, CPNMNMBR AS NOMBRE, CPNMROLM AS ROL,
       CASE WHEN REGEXP_LIKE(CPNMNMBR, '[0-9]')
            THEN '*** TIENE CIFRAS DENTRO: MIRAR ***' ELSE 'OK' END AS VEREDICTO
  FROM RHH.CPNM
 WHERE PJRQCDGO = :EMPRESA AND CPNMALTR IN (23, 24, 25)
 ORDER BY CPNMALTR;


-- =====================================================
-- CONTROL 5: EL FILO DE CALDERON. Es propio de mayo y no lo trae el guion.
-- Anadido el 2026-08-23 tras verificar recortaDescuentos en fuente.
-- =====================================================
-- CALDERON PARRAGA ATERRIZA EN NETO CERO EXACTO:
--   700,00 - 66,15 (aporte 9,45%) - 14,04 (quirografario) - 619,81 (anticipo) = 0,00
-- .
-- El anticipo de 619,81 esta puesto para agotar el neto al centimo, asi que
-- su fila esta EN EL BORDE de la proteccion de neto negativo. Pasos 13 y 14
-- de calcularPeriodo: si el neto sale < 0, recortaDescuentos RECORTA un
-- descuento --el EGRESO recortable de mayor CPNMORDN-- y sigue adelante.
-- Imprime un System.out y NO falla.
-- .
-- CONSECUENCIA SI UN SOLO CENTIMO SE MUEVE: el neto seria -0,01, se
-- recortaria un centimo de un descuento, y el subtotal de anticipos bajaria
-- de 1.869,81 a 1.869,80. Eso lo caza el control 6 del poscalculo, pero
-- conviene saber POR QUE en mayo ese subtotal importa mas que en abril.
-- .
-- QUE MIRAR AQUI: cuales de los tres conceptos son recortables. Si el
-- anticipo lo es, el recorte le tocaria a el. Si NINGUNO lo es, el metodo
-- no encuentra nada que recortar y LANZA EXCEPCION: el calculo del mes
-- entero se cae con un mensaje sobre Calderon. Los dos finales son
-- informativos, pero son finales distintos y conviene reconocerlos.
-- =====================================================
SELECT CPNMALTR AS ALTERNO, CPNMNMBR AS CONCEPTO, CPNMORDN AS ORDEN,
       CPNMRCRT AS RECORTABLE, CPNMTPCN AS TIPO
  FROM RHH.CPNM
 WHERE PJRQCDGO = :EMPRESA AND CPNMALTR IN (20, 23, 24, 25)
 ORDER BY CPNMORDN;

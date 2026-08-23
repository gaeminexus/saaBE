-- =====================================================
-- MODULO: RHH - LAS DIEZ NOVEDADES DE ABRIL, ANTES DE CALCULAR
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
-- CONTROL 1: EL VEREDICTO, FILA A FILA. Diez filas, todas ENTRA.
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
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 4
 ORDER BY c.CPNMALTR, m.MPLDAPLL;


-- =====================================================
-- CONTROL 2: LOS SUBTOTALES, PERO SOLO DE LAS QUE ENTRAN.
-- Sumar las diez daria el numero bueno aunque una no entrase: el subtotal
-- tiene que calcularse sobre el MISMO filtro que usa el motor.
-- Esperado: 23 -> 687,05  ·  24 -> 1015,14  ·  25 -> 1300,00
-- =====================================================
SELECT c.CPNMALTR AS ALTERNO, COUNT(*) AS FILAS, SUM(n.NVNMVLRR) AS SUBTOTAL
  FROM RHH.NVNM n
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.CPNM c ON c.CPNMCDGO = n.CPNMCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 4
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
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 4
   AND m.MPLDESTD = 4
UNION ALL
SELECT 'REPETIDA', m.MPLDIDNT, m.MPLDAPLL, NULL
  FROM RHH.NVNM n
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 4
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

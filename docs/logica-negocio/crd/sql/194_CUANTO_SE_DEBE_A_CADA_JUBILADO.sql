-- =====================================================================================
-- 194 - Cuanto se le PUEDE pagar a cada jubilado si el pago pasa a ser ACUMULADO
-- FECHA: 2026-09-04 · EQUIPO: CRD / Equipo B (eqB, omen-saa-1)
-- CORREGIDO: 2026-09-04, misma tarde — ver "Correccion" mas abajo.
--
-- SOLO SELECT. No modifica una sola fila.
-- Sin comandos de SQL*Plus: no usa PROMPT, DEFINE, SET ni &variables.
--
-- =====================================================================================
-- LA REGLA, cerrada por el usuario el 2026-09-04
-- =====================================================================================
-- El pago mensual ACUMULA los meses no pagados: se busca el ULTIMO MOVIMIENTO NEGATIVO
-- del aporte 23 y se paga desde ahi hasta el periodo en curso. Si el ultimo fue enero,
-- agosto paga enero..agosto.
--
-- CON DOS CONDICIONES, las dos del usuario:
--
--   1. El participe tiene que estar en estado JUBILADO COMPLEMENTARIO (ENTDIDST = 3).
--   2. (!) EL SALDO DE SU CUENTA ES EL TECHO. No se le puede pagar mas de lo que su
--      saldo del aporte 23 permite. El acumulado se PAGA HASTA DONDE ALCANZA.
--
-- =====================================================================================
-- CORRECCION - por que este script cambio
-- =====================================================================================
-- La primera version media la deuda TEORICA (meses x valor mensual) sin toparla contra
-- el saldo, y mostraba un TOTAL_SI_SE_ACUMULA que nadie va a pagar nunca. El usuario lo
-- senalo: "no se le puede pagar mas de lo que el saldo de su cuenta le permite".
--
-- El numero que importa es el PAGABLE = MIN(deuda acumulada, saldo disponible), y eso es
-- lo que mide ahora el bloque 1. La diferencia entre deuda y pagable tambien se muestra:
-- es lo que queda debiendo y NO se va a pagar en esta corrida.
--
-- =====================================================================================
-- QUE ANCLA CADA CASO, verificado contra el codigo el 2026-09-04
-- =====================================================================================
--   Solo DOS procesos escriben en el aporte 23:
--     AporteServiceImpl:495                 -> POSITIVO, tipoMovimiento JUBILACION (7)
--     PagoPensionComplementariaServiceImpl  -> NEGATIVO, tipoMovimiento PAGO_PENSION (9)
--
--   Asi que "ultimo movimiento negativo del 23" = ultimo pago de pension, limpio. Y si no
--   hay ninguno, el ancla es el movimiento de JUBILACION - que es exactamente el momento
--   en que el participe paso a JUBILADO COMPLEMENTARIO, o sea que la condicion 1 del
--   usuario ya queda cubierta por el propio ancla.
--
-- (!!) CRD.PGPC esta VACIA: el proceso nunca corrio. Por lo tanto NADIE tiene movimiento
-- negativo, y para TODOS el punto de partida es su jubilacion. La primera corrida
-- acumulada no paga un mes: paga todo lo retroactivo que el saldo permita.
-- =====================================================================================

-- ==========================================================================
-- BLOQUE 1 - (!) EL NUMERO QUE IMPORTA: deuda, saldo y lo realmente pagable
-- ==========================================================================

SELECT COUNT(*)                                                    AS JUBILADOS,
       ROUND(AVG(x.MESES_ADEUDADOS), 1)                            AS MESES_PROMEDIO,
       MAX(x.MESES_ADEUDADOS)                                      AS MESES_MAXIMO,
       ROUND(SUM(x.DEUDA), 2)                                      AS DEUDA_TEORICA,
       ROUND(SUM(LEAST(x.DEUDA, x.SALDO)), 2)                      AS PAGABLE_REAL,
       ROUND(SUM(GREATEST(x.DEUDA - x.SALDO, 0)), 2)               AS QUEDA_DEBIENDO,
       ROUND(SUM(x.VALOR_MENSUAL), 2)                              AS SI_FUERA_UN_SOLO_MES
  FROM (SELECT e.ENTDCDGO,
               NVL(v.VPPCVLRR, 0)                                  AS VALOR_MENSUAL,
               NVL((SELECT SUM(a3.APRTVLRR) FROM CRD.APRT a3
                     WHERE a3.ENTDCDGO = e.ENTDCDGO
                       AND a3.TPAPCDGO = 23), 0)                   AS SALDO,
               NVL(v.VPPCVLRR, 0) * NVL(MONTHS_BETWEEN(
                   TRUNC(SYSDATE, 'MM'),
                   TRUNC(NVL((SELECT MAX(a.APRTFCTR) FROM CRD.APRT a
                               WHERE a.ENTDCDGO = e.ENTDCDGO
                                 AND a.TPAPCDGO = 23
                                 AND a.APRTVLRR < 0),
                             (SELECT MIN(a2.APRTFCTR) FROM CRD.APRT a2
                               WHERE a2.ENTDCDGO = e.ENTDCDGO
                                 AND a2.TPAPCDGO = 23
                                 AND a2.APRTVLRR > 0)), 'MM')), 0) AS DEUDA,
               NVL(MONTHS_BETWEEN(
                   TRUNC(SYSDATE, 'MM'),
                   TRUNC(NVL((SELECT MAX(a.APRTFCTR) FROM CRD.APRT a
                               WHERE a.ENTDCDGO = e.ENTDCDGO
                                 AND a.TPAPCDGO = 23
                                 AND a.APRTVLRR < 0),
                             (SELECT MIN(a2.APRTFCTR) FROM CRD.APRT a2
                               WHERE a2.ENTDCDGO = e.ENTDCDGO
                                 AND a2.TPAPCDGO = 23
                                 AND a2.APRTVLRR > 0)), 'MM')), 0) AS MESES_ADEUDADOS
          FROM CRD.ENTD e
          JOIN CRD.VPPC v ON v.ENTDCDGO = e.ENTDCDGO AND v.VPPCIDST = 1
         WHERE e.ENTDIDST = 3) x            -- JUBILADO COMPLEMENTARIO, condicion 1
 WHERE x.SALDO > 0;

--
-- (!) PAGABLE_REAL es lo que de verdad saldria en ordenes de pago. Es el numero a mirar,
--     no DEUDA_TEORICA.
-- (!) QUEDA_DEBIENDO es lo que el saldo no alcanza a cubrir. No desaparece: sigue
--     debiendose, pero no se paga en esta corrida.
-- (!) Comparar PAGABLE_REAL contra SI_FUERA_UN_SOLO_MES: esa es, en un numero, la
--     diferencia entre lo que el proceso hace hoy y lo que haria con la regla nueva.
--

-- ==========================================================================
-- BLOQUE 2 - A cuantos les alcanza el saldo y a cuantos no
-- ==========================================================================

SELECT SUM(CASE WHEN x.SALDO + 0.005 >= x.DEUDA THEN 1 ELSE 0 END) AS COBRAN_TODO,
       SUM(CASE WHEN x.SALDO + 0.005 <  x.DEUDA
                 AND x.SALDO > 0.005          THEN 1 ELSE 0 END)   AS COBRAN_PARCIAL,
       SUM(CASE WHEN x.SALDO <= 0.005         THEN 1 ELSE 0 END)   AS SIN_SALDO_NO_COBRAN,
       ROUND(SUM(CASE WHEN x.SALDO + 0.005 < x.DEUDA
                      THEN x.SALDO ELSE 0 END), 2)                 AS MONTO_DE_LOS_PARCIALES
  FROM (SELECT e.ENTDCDGO,
               NVL((SELECT SUM(a3.APRTVLRR) FROM CRD.APRT a3
                     WHERE a3.ENTDCDGO = e.ENTDCDGO
                       AND a3.TPAPCDGO = 23), 0)                   AS SALDO,
               NVL(v.VPPCVLRR, 0) * NVL(MONTHS_BETWEEN(
                   TRUNC(SYSDATE, 'MM'),
                   TRUNC(NVL((SELECT MAX(a.APRTFCTR) FROM CRD.APRT a
                               WHERE a.ENTDCDGO = e.ENTDCDGO
                                 AND a.TPAPCDGO = 23
                                 AND a.APRTVLRR < 0),
                             (SELECT MIN(a2.APRTFCTR) FROM CRD.APRT a2
                               WHERE a2.ENTDCDGO = e.ENTDCDGO
                                 AND a2.TPAPCDGO = 23
                                 AND a2.APRTVLRR > 0)), 'MM')), 0) AS DEUDA
          FROM CRD.ENTD e
          JOIN CRD.VPPC v ON v.ENTDCDGO = e.ENTDCDGO AND v.VPPCIDST = 1
         WHERE e.ENTDIDST = 3) x;

--
-- (!) COBRAN_PARCIAL es el grupo nuevo que crea esta regla: hoy el proceso los rechaza
--     enteros con SALDO_INSUFICIENTE; con el tope los paga hasta donde alcanza.
-- (!) SIN_SALDO_NO_COBRAN: saldo cero o negativo. A estos no se les paga nada, y hay que
--     decidir si salen como ERROR o como una categoria propia ("saldo agotado"), que no
--     es lo mismo: un saldo agotado es el final normal de una pension, no una falla.
--

-- ==========================================================================
-- BLOQUE 3 - (!) Los que NO tienen de donde partir
-- ==========================================================================

SELECT COUNT(*)                                                    AS SIN_ANCLA
  FROM CRD.ENTD e
  JOIN CRD.VPPC v ON v.ENTDCDGO = e.ENTDCDGO AND v.VPPCIDST = 1
 WHERE e.ENTDIDST = 3
   AND NOT EXISTS (SELECT 1 FROM CRD.APRT a
                    WHERE a.ENTDCDGO = e.ENTDCDGO AND a.TPAPCDGO = 23);

--
-- (!) Sin ningun movimiento del aporte 23 no hay fecha de la cual partir NI saldo que
--     pagar. Vienen de migracion, no del proceso. Si el numero es alto, la regla necesita
--     una tercera fuente de fecha antes de implementarse. Decision del usuario.
--

-- ==========================================================================
-- BLOQUE 4 - Los 20 casos mas grandes, para revisarlos uno por uno
-- ==========================================================================

SELECT * FROM (
  SELECT e.ENTDNMID                                                AS CEDULA,
         SUBSTR(e.ENTDRZNS,1,35)                                   AS NOMBRE,
         v.VPPCVLRR                                                AS VALOR_MENSUAL,
         NVL(MONTHS_BETWEEN(TRUNC(SYSDATE,'MM'),
             TRUNC(NVL((SELECT MAX(a.APRTFCTR) FROM CRD.APRT a
                         WHERE a.ENTDCDGO = e.ENTDCDGO AND a.TPAPCDGO = 23
                           AND a.APRTVLRR < 0),
                       (SELECT MIN(a2.APRTFCTR) FROM CRD.APRT a2
                         WHERE a2.ENTDCDGO = e.ENTDCDGO AND a2.TPAPCDGO = 23
                           AND a2.APRTVLRR > 0)),'MM')),0)         AS MESES,
         NVL((SELECT SUM(a3.APRTVLRR) FROM CRD.APRT a3
               WHERE a3.ENTDCDGO = e.ENTDCDGO AND a3.TPAPCDGO = 23),0) AS SALDO
    FROM CRD.ENTD e
    JOIN CRD.VPPC v ON v.ENTDCDGO = e.ENTDCDGO AND v.VPPCIDST = 1
   WHERE e.ENTDIDST = 3
   ORDER BY 4 DESC
) WHERE ROWNUM <= 20;

--
-- (!) Mirar si los MESES mas altos tienen sentido. Un numero absurdo (cientos de meses)
--     delata una fecha de movimiento mala, no una deuda real, y con el tope del saldo
--     igual se pagaria de mas si el saldo es grande.
--

-- =====================================================================================
-- No hay bloque de reverso: este script no escribe nada.
-- =====================================================================================

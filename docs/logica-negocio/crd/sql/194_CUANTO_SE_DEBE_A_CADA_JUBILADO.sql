-- =====================================================================================
-- 194 - Cuanto se le debe a cada jubilado si el pago pasa a ser ACUMULADO
-- FECHA: 2026-09-04 · EQUIPO: CRD / Equipo B (eqB, omen-saa-1)
--
-- SOLO SELECT. No modifica una sola fila.
--
-- Sin comandos de SQL*Plus: no usa PROMPT, DEFINE, SET ni &variables.
--
-- =====================================================================================
-- POR QUE ESTE SCRIPT VA ANTES DE IMPLEMENTAR NADA
-- =====================================================================================
-- Pedido del usuario (2026-09-04): el pago mensual debe ACUMULAR los meses no pagados.
-- Se busca el ULTIMO MOVIMIENTO NEGATIVO del aporte 23 y se paga desde ahi hasta el
-- periodo en curso. Si el ultimo fue enero, agosto paga enero..agosto.
--
-- (!!) Y ACA ESTA EL PUNTO QUE HAY QUE MEDIR ANTES DE CODIFICAR:
--
-- CRD.PGPC esta VACIA (medido con sql/189 el 2026-09-04): el proceso NUNCA corrio. Por
-- lo tanto NINGUN jubilado tiene un movimiento negativo de tipo 23 por pago de pension.
-- Con la regla de acumulacion, eso significa que para TODOS el punto de partida es su
-- JUBILACION, no el mes pasado.
--
-- Es decir: la PRIMERA corrida acumulada no paga un mes. Paga TODO lo retroactivo desde
-- que cada uno se jubilo. Si alguien se jubilo hace tres anios, son 36 mensualidades en
-- un solo pago.
--
-- Eso puede ser exactamente lo que el usuario quiere - o puede ser una consecuencia que
-- no estaba mirando. Este script pone el numero sobre la mesa ANTES, no despues de haber
-- generado 191 ordenes de pago sin reverso.
--
-- QUE ANCLA CADA CASO, verificado contra el codigo el 2026-09-04:
--   - Solo DOS procesos escriben en el aporte 23:
--       AporteServiceImpl:495              -> POSITIVO, tipoMovimiento JUBILACION (7)
--       PagoPensionComplementariaServiceImpl -> NEGATIVO, tipoMovimiento PAGO_PENSION (9)
--   - Asi que "ultimo movimiento negativo del 23" = ultimo pago de pension. Limpio.
--   - Y si no hay ninguno, el ancla natural es el movimiento de JUBILACION.
-- =====================================================================================

-- ==========================================================================
-- BLOQUE 1 - El panorama: cuantos meses debe cada uno y cuanto suma
-- ==========================================================================

SELECT COUNT(*)                                                    AS JUBILADOS,
       SUM(x.MESES_ADEUDADOS)                                      AS MESES_TOTALES,
       ROUND(AVG(x.MESES_ADEUDADOS), 1)                            AS MESES_PROMEDIO,
       MAX(x.MESES_ADEUDADOS)                                      AS MESES_MAXIMO,
       ROUND(SUM(x.MESES_ADEUDADOS * x.VALOR_MENSUAL), 2)          AS TOTAL_SI_SE_ACUMULA,
       ROUND(SUM(x.VALOR_MENSUAL), 2)                              AS TOTAL_SI_ES_UN_MES
  FROM (SELECT e.ENTDCDGO,
               NVL(v.VPPCVLRR, 0)                                  AS VALOR_MENSUAL,
               MONTHS_BETWEEN(
                   TRUNC(SYSDATE, 'MM'),
                   TRUNC(NVL((SELECT MAX(a.APRTFCTR) FROM CRD.APRT a
                               WHERE a.ENTDCDGO = e.ENTDCDGO
                                 AND a.TPAPCDGO = 23
                                 AND a.APRTVLRR < 0),
                             (SELECT MIN(a2.APRTFCTR) FROM CRD.APRT a2
                               WHERE a2.ENTDCDGO = e.ENTDCDGO
                                 AND a2.TPAPCDGO = 23
                                 AND a2.APRTVLRR > 0)), 'MM'))     AS MESES_ADEUDADOS
          FROM CRD.ENTD e
          JOIN CRD.VPPC v ON v.ENTDCDGO = e.ENTDCDGO AND v.VPPCIDST = 1
         WHERE e.ENTDIDST = 3) x
 WHERE x.MESES_ADEUDADOS IS NOT NULL;

--
-- (!) COMPARAR TOTAL_SI_SE_ACUMULA CONTRA TOTAL_SI_ES_UN_MES. Esa es, en un numero, la
--     diferencia entre lo que el proceso hace hoy y lo que haria con la regla nueva.
-- (!) MESES_MAXIMO dice cuan viejo es el caso mas retrasado.
-- (!) MESES_ADEUDADOS NULL = ese jubilado no tiene NINGUN movimiento del aporte 23, ni
--     positivo ni negativo. No se jubilo por el proceso (viene de migracion) y no hay
--     fecha de la cual partir. El bloque 3 los lista: son los que NO se pueden acumular
--     sin una decision aparte.
--

-- ==========================================================================
-- BLOQUE 2 - La distribucion, para ver si es un problema de pocos o de todos
-- ==========================================================================

SELECT CASE WHEN x.MESES_ADEUDADOS <= 1  THEN '0-1 mes (al dia)'
            WHEN x.MESES_ADEUDADOS <= 3  THEN '2-3 meses'
            WHEN x.MESES_ADEUDADOS <= 6  THEN '4-6 meses'
            WHEN x.MESES_ADEUDADOS <= 12 THEN '7-12 meses'
            WHEN x.MESES_ADEUDADOS <= 24 THEN '1-2 anios'
            ELSE 'mas de 2 anios'
       END                                                         AS TRAMO,
       COUNT(*)                                                    AS JUBILADOS,
       ROUND(SUM(x.MESES_ADEUDADOS * x.VALOR_MENSUAL), 2)          AS MONTO_DEL_TRAMO
  FROM (SELECT e.ENTDCDGO,
               NVL(v.VPPCVLRR, 0)                                  AS VALOR_MENSUAL,
               MONTHS_BETWEEN(
                   TRUNC(SYSDATE, 'MM'),
                   TRUNC(NVL((SELECT MAX(a.APRTFCTR) FROM CRD.APRT a
                               WHERE a.ENTDCDGO = e.ENTDCDGO
                                 AND a.TPAPCDGO = 23
                                 AND a.APRTVLRR < 0),
                             (SELECT MIN(a2.APRTFCTR) FROM CRD.APRT a2
                               WHERE a2.ENTDCDGO = e.ENTDCDGO
                                 AND a2.TPAPCDGO = 23
                                 AND a2.APRTVLRR > 0)), 'MM'))     AS MESES_ADEUDADOS
          FROM CRD.ENTD e
          JOIN CRD.VPPC v ON v.ENTDCDGO = e.ENTDCDGO AND v.VPPCIDST = 1
         WHERE e.ENTDIDST = 3) x
 WHERE x.MESES_ADEUDADOS IS NOT NULL
 GROUP BY CASE WHEN x.MESES_ADEUDADOS <= 1  THEN '0-1 mes (al dia)'
               WHEN x.MESES_ADEUDADOS <= 3  THEN '2-3 meses'
               WHEN x.MESES_ADEUDADOS <= 6  THEN '4-6 meses'
               WHEN x.MESES_ADEUDADOS <= 12 THEN '7-12 meses'
               WHEN x.MESES_ADEUDADOS <= 24 THEN '1-2 anios'
               ELSE 'mas de 2 anios'
          END
 ORDER BY 1;

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
-- (!) Estos jubilados no tienen NINGUN movimiento del aporte 23: ni el traslado de la
--     jubilacion ni un pago. Vienen de la migracion, no del proceso. Para ellos la regla
--     "desde el ultimo movimiento negativo" no tiene punto de partida, y tampoco lo tiene
--     la variante "desde la jubilacion".
-- (!) Si este numero es alto, la regla de acumulacion necesita una tercera fuente de
--     fecha (una fecha de jubilacion en ENTD, o un mes de arranque fijado a mano) ANTES
--     de poder implementarse. Es una decision del usuario, no del arbitro.
--

-- ==========================================================================
-- BLOQUE 4 - Alcanza el saldo del aporte 23 para pagar lo acumulado?
-- ==========================================================================

SELECT SUM(CASE WHEN x.SALDO + 0.005 >= x.DEUDA THEN 1 ELSE 0 END) AS LES_ALCANZA,
       SUM(CASE WHEN x.SALDO + 0.005 <  x.DEUDA THEN 1 ELSE 0 END) AS NO_LES_ALCANZA,
       ROUND(SUM(CASE WHEN x.SALDO < x.DEUDA
                      THEN x.DEUDA - x.SALDO ELSE 0 END), 2)       AS FALTANTE_TOTAL
  FROM (SELECT e.ENTDCDGO,
               NVL((SELECT SUM(a3.APRTVLRR) FROM CRD.APRT a3
                     WHERE a3.ENTDCDGO = e.ENTDCDGO AND a3.TPAPCDGO = 23), 0) AS SALDO,
               NVL(v.VPPCVLRR, 0) *
               NVL(MONTHS_BETWEEN(
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
-- (!) Hoy el proceso falla con SALDO_INSUFICIENTE si el saldo no cubre el pago. Con
--     acumulacion, NO_LES_ALCANZA dice a cuantos les va a pasar. Si es un numero grande,
--     hace falta decidir que se hace: pagar hasta donde alcance, o rechazar el caso.
--     Tambien es decision del usuario.
--

-- =====================================================================================
-- No hay bloque de reverso: este script no escribe nada.
-- =====================================================================================

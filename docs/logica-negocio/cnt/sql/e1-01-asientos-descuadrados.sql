-- =====================================================================================
-- ⛔ ASIENTOS DESCUADRADOS EN LA BASE — cuantos hay y de que modulos
-- FECHA: 2026-09-07   EQUIPO: omen-saa-1 (omen1)   PREFIJO: e1- (fuera de crd/sql, §2b)
--
-- ⚠️ NO ESCRIBE NADA. Los cuatro bloques son SELECT.
--
-- =====================================================================================
-- EL DEFECTO, medido contra el codigo:
--
--   DetalleAsientoServiceImpl.validaDebeHaber:246
--
--       if (Math.abs(debe - haber) > 0.01) { igual = false; }
--
--   Es la UNICA guarda que impide grabar un asiento descuadrado, y TOLERA hasta un
--   centavo. Un asiento con debe 1.487,03 y haber 1.487,02 se da por cuadrado y se graba.
--
--   ⛔ Y hay algo peor que la tolerancia: la comparacion esta JUSTO EN EL BORDE, sobre
--      doubles. En punto flotante:
--          171,86 - 171,85  = 0,010000000000005...  ->  > 0.01  ->  RECHAZA
--          1487,03 - 1487,02 = 0,009999999999991... ->  no      ->  ACEPTA
--      El MISMO descuadre de un centavo pasa o no segun la magnitud de los numeros.
--      No es una tolerancia: es un resultado que depende de la representacion binaria.
--
--   Caso real que lo destapo: asiento CRE-2026-08-0085 (cobro credito 29), grabado con
--   debe 1.487,03 y haber 1.487,02.
--
-- =====================================================================================
-- PARA QUE ESTE SCRIPT: el usuario pidio que el sistema JAMAS permita un asiento
-- descuadrado. Corregir la guarda es una linea, pero esa guarda la usan TODOS los modulos
-- (cnt, crd, tsr, cxp, cxc, rhh). Pasar de "tolera un centavo" a "exacto" puede empezar a
-- RECHAZAR asientos que hoy algun proceso genera de rutina con un centavo de diferencia.
--
-- ⇒ Antes de apretar la guarda hay que saber CUANTOS asientos descuadrados existen y de
--   QUE procesos vienen. Si son cuatro, se corrigen. Si son miles, apretar la guarda frena
--   la operacion de varios modulos y hay que planificarlo.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los cuatro bloques.
-- =====================================================================================

SET PAGESIZE 300
SET LINESIZE 260
SET FEEDBACK ON


-- =====================================================================================
-- BLOQUE 1 — ⛔ EL NUMERO QUE DECIDE TODO: cuantos asientos no cuadran
-- Se usa ROUND(...,2) para comparar en centavos y no arrastrar el error de punto flotante
-- que es justamente parte del defecto.
-- =====================================================================================

SELECT COUNT(*) AS ASIENTOS_DESCUADRADOS,
       ROUND(SUM(ABS(t.DIFERENCIA)), 2) AS SUMA_DE_DIFERENCIAS,
       ROUND(MAX(ABS(t.DIFERENCIA)), 2) AS PEOR_DIFERENCIA
  FROM (SELECT l.ASNTCDGO,
               ROUND(SUM(NVL(l.DTASDBEE,0)) - SUM(NVL(l.DTASHBRR,0)), 2) AS DIFERENCIA
          FROM CNT.DTAS l
         GROUP BY l.ASNTCDGO) t
 WHERE t.DIFERENCIA <> 0;


-- =====================================================================================
-- BLOQUE 2 — Repartidos por tamano del descuadre
-- Lo esperable si el defecto es SOLO la tolerancia de un centavo: casi todo en la fila
-- de "1 centavo". Si aparecen descuadres grandes, hay OTRO defecto ademas de este.
-- =====================================================================================

SELECT CASE WHEN ABS(t.DIFERENCIA) <= 0.01 THEN '1 centavo'
            WHEN ABS(t.DIFERENCIA) <= 0.10 THEN 'hasta 10 centavos'
            WHEN ABS(t.DIFERENCIA) <= 1    THEN 'hasta 1 dolar'
            ELSE 'MAS DE 1 DOLAR — mirar aparte' END AS RANGO,
       COUNT(*) AS ASIENTOS,
       ROUND(SUM(ABS(t.DIFERENCIA)), 2) AS SUMA
  FROM (SELECT l.ASNTCDGO,
               ROUND(SUM(NVL(l.DTASDBEE,0)) - SUM(NVL(l.DTASHBRR,0)), 2) AS DIFERENCIA
          FROM CNT.DTAS l
         GROUP BY l.ASNTCDGO) t
 WHERE t.DIFERENCIA <> 0
 GROUP BY CASE WHEN ABS(t.DIFERENCIA) <= 0.01 THEN '1 centavo'
               WHEN ABS(t.DIFERENCIA) <= 0.10 THEN 'hasta 10 centavos'
               WHEN ABS(t.DIFERENCIA) <= 1    THEN 'hasta 1 dolar'
               ELSE 'MAS DE 1 DOLAR — mirar aparte' END
 ORDER BY 2 DESC;


-- =====================================================================================
-- BLOQUE 3 — ⭐ DE QUE PROCESOS VIENEN. Este es el que dice a quien afecta apretar la guarda.
-- Se agrupa por el tipo de asiento y por el prefijo del numero alterno, que es lo que
-- identifica al proceso que lo genero (CRE-, T-E-, etc.).
-- =====================================================================================

SELECT NVL(SUBSTR(a.ASNTNMAL, 1, INSTR(a.ASNTNMAL || '-', '-') - 1), '(sin alterno)') AS PREFIJO,
       a.ASNTPRDO AS PERIODO,
       COUNT(*) AS ASIENTOS_DESCUADRADOS,
       MIN(a.ASNTFCHA) AS DESDE,
       MAX(a.ASNTFCHA) AS HASTA
  FROM CNT.ASNT a
  JOIN (SELECT l.ASNTCDGO,
               ROUND(SUM(NVL(l.DTASDBEE,0)) - SUM(NVL(l.DTASHBRR,0)), 2) AS DIFERENCIA
          FROM CNT.DTAS l
         GROUP BY l.ASNTCDGO) t ON t.ASNTCDGO = a.ASNTCDGO
 WHERE t.DIFERENCIA <> 0
 GROUP BY NVL(SUBSTR(a.ASNTNMAL, 1, INSTR(a.ASNTNMAL || '-', '-') - 1), '(sin alterno)'),
          a.ASNTPRDO
 ORDER BY 3 DESC;


-- =====================================================================================
-- BLOQUE 4 — El detalle de los peores, para mirarlos con ojos de contador
-- =====================================================================================

SELECT a.ASNTCDGO AS ASIENTO, a.ASNTNMAL AS NUMERO, a.ASNTFCHA AS FECHA,
       a.PJRQCDGO AS EMPRESA, a.ASNTESTD AS ESTADO,
       t.TOTAL_DEBE, t.TOTAL_HABER, t.DIFERENCIA,
       SUBSTR(a.ASNTOBSR, 1, 90) AS OBSERVACION
  FROM CNT.ASNT a
  JOIN (SELECT l.ASNTCDGO,
               ROUND(SUM(NVL(l.DTASDBEE,0)), 2) AS TOTAL_DEBE,
               ROUND(SUM(NVL(l.DTASHBRR,0)), 2) AS TOTAL_HABER,
               ROUND(SUM(NVL(l.DTASDBEE,0)) - SUM(NVL(l.DTASHBRR,0)), 2) AS DIFERENCIA
          FROM CNT.DTAS l
         GROUP BY l.ASNTCDGO) t ON t.ASNTCDGO = a.ASNTCDGO
 WHERE t.DIFERENCIA <> 0
 ORDER BY ABS(t.DIFERENCIA) DESC, a.ASNTCDGO DESC
 FETCH FIRST 40 ROWS ONLY;

-- =====================================================================================
-- 238 - Seguro medico 9/2026: los OCHO que tienen seguro configurado, y por que dan $0
-- FECHA: 2026-09-22 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- Sin comandos de SQL*Plus. ⭐ NO ESCRIBE NADA. Comentarios ARRIBA, nunca intercalados.
--
-- =====================================================================================
-- CORRIGE AL 237, Y EL ERROR FUE MIO
-- =====================================================================================
-- Los bloques 3 y 4 del 237 reventaron con ORA-00904: invente los nombres de columna
-- APRTFCMV y APRTVLMV. Los reales, leidos de com.saa.model.crd.Aporte, son:
--     APRTVLRR = valor del movimiento   (NO APRTVLMV)
--     APRTFCTR = fecha de transaccion   (NO APRTFCMV)
-- Es la segunda vez en esta serie que escribo un nombre de memoria en vez de leerlo del
-- modelo (la primera fue SCP.RUBR por SCP.PRBR, en el 232). Todas las columnas de este
-- script estan verificadas una por una contra las entidades JPA.
--
-- =====================================================================================
-- LO QUE YA SE SABE, DE LAS SALIDAS DEL 237
-- =====================================================================================
--   Bloque 1: CERO filas en CRD.PGPC para 9/2026.
--             ⇒ El proceso NO fijo NADA, ni siquiera ceros. Eso DESCARTA la hipotesis
--               H46 del 237 (no hubo escritura previa que envenenara el ancla) y
--               descarta tambien "se fijaron en 0".
--   Bloque 5: 190 configuraciones VPPC activas, suma nominal $450,40,
--             y solo OCHO con seguro configurado (VPPCVLSR > 0).
--             ⇒ ⭐ Los $450,40 de la pantalla NO son de 139 jubilados: son de OCHO.
--               Los otros 182 tienen el seguro en 0 y no aportan nada al total.
--
-- ⇒ LA PREGUNTA QUE QUEDA, Y ES LA UNICA: que pasa con esos OCHO. El proceso recorre
--   el padron de estado JUBILADO_COMPLEMENTARIO (codigo alterno 3 en ENTDIDST) y fija
--   min(nominal, saldo del aporte tipo 23). Si los ocho no estan en ese padron, o no
--   tienen saldo, el total da $0 sin un solo error.
-- =====================================================================================


-- =====================================================================================
-- 1. ⭐ LOS OCHO, UNO POR UNO: quienes son, en que estado estan y cuanto saldo tienen
-- =====================================================================================
-- Esta consulta sola contesta casi todo. Columnas que decide cada una:
--   EN_EL_PADRON = NO   -> el proceso NUNCA los mira. Causa encontrada.
--   SALDO_TIPO_23 <= 0  -> el proceso los mira pero topa el seguro en 0. Causa encontrada.
--   ULTIMO_MOVIMIENTO en 9/2026 o despues -> el ancla los da por "al dia" -> 0.

SELECT v.ENTDCDGO,
       v.VPPCVLSR                                                     AS SEGURO_NOMINAL,
       e.ENTDIDST                                                     AS ESTADO_PARTICIPE,
       CASE WHEN e.ENTDIDST = 3 THEN 'SI' ELSE 'NO' END               AS EN_EL_PADRON,
       NVL((SELECT SUM(a.APRTVLRR) FROM CRD.APRT a
             WHERE a.ENTDCDGO = v.ENTDCDGO AND a.TPAPCDGO = 23), 0)   AS SALDO_TIPO_23,
       (SELECT MAX(a.APRTFCTR) FROM CRD.APRT a
         WHERE a.ENTDCDGO = v.ENTDCDGO AND a.TPAPCDGO = 23)           AS ULTIMO_MOVIMIENTO,
       (SELECT COUNT(*) FROM CRD.APRT a
         WHERE a.ENTDCDGO = v.ENTDCDGO AND a.TPAPCDGO = 23)           AS MOVIMIENTOS
  FROM CRD.VPPC v
  JOIN CRD.ENTD e ON e.ENTDCDGO = v.ENTDCDGO
 WHERE v.VPPCIDST = 1
   AND NVL(v.VPPCVLSR, 0) > 0
 ORDER BY v.ENTDCDGO;


-- =====================================================================================
-- 2. El resumen de lo mismo, para leerlo de un vistazo
-- =====================================================================================

SELECT COUNT(*)                                                       AS CON_SEGURO_CONFIGURADO,
       SUM(CASE WHEN e.ENTDIDST = 3 THEN 1 ELSE 0 END)                AS EN_EL_PADRON,
       SUM(CASE WHEN e.ENTDIDST <> 3 THEN 1 ELSE 0 END)               AS FUERA_DEL_PADRON,
       SUM(v.VPPCVLSR)                                                AS NOMINAL_TOTAL,
       SUM(CASE WHEN e.ENTDIDST = 3 THEN v.VPPCVLSR ELSE 0 END)       AS NOMINAL_DE_LOS_DEL_PADRON
  FROM CRD.VPPC v
  JOIN CRD.ENTD e ON e.ENTDCDGO = v.ENTDCDGO
 WHERE v.VPPCIDST = 1
   AND NVL(v.VPPCVLSR, 0) > 0;


-- =====================================================================================
-- 3. Cuantos jubilados hay en el padron que recorre el proceso
-- =====================================================================================
-- El log decia "Jubilados JUBILADO_COMPLEMENTARIO a evaluar: N" y la pantalla mostro
-- 182 evaluados. Esperado: que este numero coincida con 182.

SELECT COUNT(*) AS JUBILADOS_EN_EL_PADRON
  FROM CRD.ENTD e
 WHERE e.ENTDIDST = 3;


-- =====================================================================================
-- 4. Control: el padron completo por tramo de saldo del aporte tipo 23
-- =====================================================================================
-- Sirve para saber si el problema es general (nadie tiene saldo) o solo de los ocho.

SELECT CASE WHEN NVL(s.SALDO, 0) <= 0 THEN 'SIN SALDO (<= 0)'
            WHEN s.SALDO < 10         THEN 'SALDO < 10'
            ELSE                           'CON SALDO'
       END                                                            AS TRAMO,
       COUNT(*)                                                       AS JUBILADOS
  FROM (SELECT e.ENTDCDGO,
               (SELECT SUM(a.APRTVLRR) FROM CRD.APRT a
                 WHERE a.ENTDCDGO = e.ENTDCDGO AND a.TPAPCDGO = 23)   AS SALDO
          FROM CRD.ENTD e
         WHERE e.ENTDIDST = 3) s
 GROUP BY CASE WHEN NVL(s.SALDO, 0) <= 0 THEN 'SIN SALDO (<= 0)'
               WHEN s.SALDO < 10         THEN 'SALDO < 10'
               ELSE                           'CON SALDO'
          END
 ORDER BY 1;


-- =====================================================================================
-- 5. COMO SE LEE EL RESULTADO
-- =====================================================================================
-- A) Los ocho con EN_EL_PADRON = NO
--    -> El proceso no los mira nunca, y el total $0 es CORRECTO para lo que el proceso
--       sabe. El defecto esta en la PREVISUALIZACION, que los cuenta igual: promete
--       $450,40 que nadie va a cobrar. Se corrige la pantalla, no el proceso.
--
-- B) Los ocho EN_EL_PADRON = SI y SALDO_TIPO_23 <= 0
--    -> El proceso los mira y topa el seguro en 0 por falta de saldo (regla H60/§11.1:
--       el seguro se cobra del saldo del jubilado). Tampoco es un defecto del proceso:
--       la pantalla vuelve a prometer un nominal incobrable. Decision de negocio: o se
--       acepta que no se cobra, o se cambia de donde sale la plata.
--
-- C) Los ocho EN_EL_PADRON = SI y SALDO_TIPO_23 > 0
--    -> ⛔ ENTONCES SI HAY UN DEFECTO y el proceso deberia haber cobrado. Con esta
--       salida se vuelve al codigo: habria que mirar el ancla (ULTIMO_MOVIMIENTO) y,
--       si tampoco explica nada, el log completo de generarSeguroIndividual para esos
--       ocho, que imprime por que fija 0. AVISAR con la salida del bloque 1 pegada.
--
-- D) MOVIMIENTOS = 0 en los ocho
--    -> No tienen NINGUN movimiento de aporte tipo 23: no hay ancla y el seguro se fija
--       en 0 por la primera rama. Es el caso B con otro nombre, y explica de paso por
--       que no se grabo ninguna fila en PGPC.
-- =====================================================================================

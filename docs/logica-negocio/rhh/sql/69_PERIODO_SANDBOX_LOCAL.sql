-- ============================================================================
-- 69 - PERIODO DE PRUEBAS AISLADO, SOLO EN LOCAL
-- ==
-- ############ NO CORRER ESTE SCRIPT EN PRODUCCION. ############
-- Crea un periodo cuyo unico proposito es que los agentes de frontend puedan
-- dar de alta, editar y borrar novedades sin tocar nada que importe.
-- ==
-- POR QUE EXISTE. El local NO es "una base de pruebas": es el BANCO DE
-- REGRESION de las correcciones del motor. Los siete meses de 2026 estan
-- calculados y contrastados alli, y cada correccion se valida recalculandolos y
-- comprobando que dan lo mismo -cinco en cero, junio en -44,60, julio en
-- +31,43-.
-- ==
-- Una sola novedad de prueba metida en cualquiera de esos siete meses cambia su
-- recalculo. Y no se notaria hoy: se notaria dentro de tres correcciones,
-- culpando a la correccion equivocada. El agente de frontend levanto este
-- riesgo por su cuenta y paro antes de tocar nada, que es exactamente lo que
-- habia que hacer.
-- ==
-- POR QUE DICIEMBRE Y POR QUE MODO 1:
--   diciembre  -> fuera de los siete meses del banco de regresion, y RHH.CTRL
--                 no tiene ni una fila de ese mes, asi que el instrumento de
--                 contraste lo ignora por completo.
--   modo 1     -> HISTORICO_SIN_CONTABILIZAR. Aunque alguien lo calculara por
--                 accidente, NO PUEDE generar un solo asiento contable.
-- ============================================================================


-- ============================================================================
-- BLOQUE 0 - LA GUARDA. Comprueba que esto es LOCAL y no produccion.
-- ==
-- En LOCAL los siete meses son PRDN 1, 2, 21, 41, 42, 61 y 62 -los mismos
-- codigos que produccion por casualidad de la carga-, asi que el codigo no
-- distingue. Lo que SI distingue es que produccion tiene el periodo de AGOSTO
-- creado -PRDN 81, modo 2- y local NO lo tiene.
-- ==
-- ESPERADO EN LOCAL: cero filas.
-- SI DEVUELVE UNA FILA, ESTAS EN PRODUCCION: no sigas.
-- ============================================================================
SELECT PRDNCDGO, PRDNANOO, PRDNMSEE, PRDNMODO
  FROM RHH.PRDN
 WHERE PRDNANOO = 2026 AND PRDNMSEE = 8 AND PRDNMODO = 2;


-- ============================================================================
-- BLOQUE 1 - CONTROL ANTES. ESPERADO: cero filas.
-- ============================================================================
SELECT PRDNCDGO, PRDNANOO, PRDNMSEE, PRDNESTD, PRDNMODO
  FROM RHH.PRDN
 WHERE PRDNANOO = 2026 AND PRDNMSEE = 12;


-- ============================================================================
-- BLOQUE 2 - EL CAJON DE ARENA.
-- Idempotente: correrlo dos veces no crea un segundo diciembre.
-- ============================================================================
INSERT INTO RHH.PRDN (PRDNANOO, PRDNMSEE, PRDNFCHI, PRDNFCHF,
                      PRDNESTD, PRDNMODO, PRDNTPNM, PJRQCDGO,
                      PRDNFCHR, PRDNUSRR, PRDNOBSR)
SELECT 2026, 12,
       DATE '2026-12-01', DATE '2026-12-31',
       1, 1, 1, 1236,
       SYSDATE, 'SANDBOX',
       'PERIODO DE PRUEBAS - LOCAL. No calcular, no aprobar, no cerrar. Existe solo para que los agentes den de alta novedades sin tocar el banco de regresion.'
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM RHH.PRDN
                    WHERE PRDNANOO = 2026 AND PRDNMSEE = 12);

COMMIT;


-- ============================================================================
-- BLOQUE 3 - CONTROL DESPUES.
-- ESPERADO: una fila, estado 1, MODO 1, del 01-12 al 31-12, con el texto de
-- aviso en la observacion.
-- ============================================================================
SELECT PRDNCDGO AS PRDN, PRDNMSEE AS MES, PRDNESTD AS ESTADO, PRDNMODO AS MODO,
       PRDNFCHI AS DESDE, PRDNFCHF AS HASTA, PRDNOBSR AS OBSERVACION
  FROM RHH.PRDN
 WHERE PRDNANOO = 2026 AND PRDNMSEE = 12;


-- ============================================================================
-- BLOQUE 4 - Y QUE EL BANCO DE REGRESION SIGA INTACTO.
-- Se mira ANTES de dejar al agente trabajar, y se vuelve a mirar DESPUES.
-- ==
-- ESPERADO: los siete meses con sus netos de siempre. Diciembre en cero, que
-- es lo que debe: nadie lo calcula.
--   1 -> 16.476,92   2 -> 17.525,11   3 -> 17.591,12   4 -> 15.914,22
--   5 -> 16.035,21   6 -> 15.772,84   7 -> 16.328,30
-- ==
-- NOTA: si local aun no tiene el ajuste de julio aplicado, el mes 7 dara
-- 16.315,13 en vez de 16.328,30. Eso es correcto: el ajuste se corrio en
-- produccion, no en local.
-- ============================================================================
SELECT PRDNMSEE AS MES, PRDNESTD AS ESTADO, PRDNTTNT AS NETO,
       (SELECT COUNT(*) FROM RHH.NVNM n WHERE n.PRDNCDGO = p.PRDNCDGO) AS NOVEDADES
  FROM RHH.PRDN p
 WHERE PRDNANOO = 2026
 ORDER BY PRDNMSEE;


-- ============================================================================
-- LIMPIEZA, cuando el agente termine.
-- Borra las novedades de prueba y el periodo. NO se corre ahora.
-- ==
-- DELETE FROM RHH.NVNM WHERE PRDNCDGO = (SELECT PRDNCDGO FROM RHH.PRDN
--                                         WHERE PRDNANOO = 2026 AND PRDNMSEE = 12);
-- DELETE FROM RHH.PRDN WHERE PRDNANOO = 2026 AND PRDNMSEE = 12;
-- COMMIT;
-- ============================================================================

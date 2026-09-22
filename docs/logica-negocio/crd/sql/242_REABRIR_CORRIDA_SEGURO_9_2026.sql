-- =====================================================================================
-- 242 - URGENTE: la corrida de seguro de 9/2026 quedo CERRADA sin haber pagado nada
-- FECHA: 2026-09-22 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- Sin comandos de SQL*Plus. Comentarios ARRIBA, nunca intercalados.
-- ⛔ ESTE SCRIPT ESCRIBE, pero SOLO en el bloque 4, que esta COMENTADO.
--
-- =====================================================================================
-- QUE PASO, Y POR QUE EL SISTEMA DICE "YA ESTA GENERADO"
-- =====================================================================================
-- El proceso de seguro medico fallo para CADA jubilado con
--   ORA-01400 sobre CRD.PGPC.PGPCVLRR
-- porque generarSeguroIndividual:1077-1082 crea la fila del pago con el valor del
-- seguro pero NUNCA setea el valor total (PGPCVLRR, campo "valor", NOT NULL). El
-- proceso de pensiones si lo hace (:2109); el de seguro nunca lo hizo. Es la primera
-- vez que se ejecuta de verdad. Se corrige en el codigo, no aca.
--
-- ⛔ PERO EL PROBLEMA QUE BLOQUEA HOY ES OTRO, Y ES DE DISEÑO:
-- el bucle de generarSeguroDelMes ATRAPA la excepcion de cada jubilado, la suma a
-- conError y SIGUE. Al terminar, cierra la cabecera igual: CRJBESSG = 1. Entonces
-- la guarda de :1130-1134 dice "El seguro medico de 9/2026 ya se genero... No se puede
-- generar dos veces" y NO SE PUEDE REINTENTAR, aunque no se haya pagado un centavo.
--
-- ⇒ Es la misma familia que H46 y que el H78 de hoy: un error por jubilado que termina
--   en un contador que nadie mira, y un proceso que informa exito despues de fallar
--   para todos. La corrida quedo cerrada SIN haber generado nada.
-- =====================================================================================


-- =====================================================================================
-- 1. LA CABECERA: confirmar que esta cerrada y que su total es CERO
-- =====================================================================================
-- Esperado: 1 fila, CRJBESSG = 1 (cerrada) y CRJBVLSG = 0 o null (no pago nada).
-- Si CRJBVLSG tiene un valor > 0, PARAR y avisar: entonces SI se genero algo y
-- reabrir la corrida podria duplicar pagos.

SELECT c.CRJBCDGO, c.PJRQCDGO, c.CRJBANNO, c.CRJBMESS,
       c.CRJBESSG AS ESTADO_SEGURO, c.CRJBVLSG AS TOTAL_SEGURO,
       c.CRJBCTSG AS CANT_JUBILADOS, c.CRJBIDSG AS ID_ORDEN_PAGO,
       c.CRJBFCSG AS FECHA_CIERRE, c.CRJBUSSG AS USUARIO,
       c.CRJBESPN AS ESTADO_PENSIONES
  FROM CRD.CRJB c
 WHERE c.CRJBANNO = 2026 AND c.CRJBMESS = 9;


-- =====================================================================================
-- 2. ⭐ LO QUE DECIDE SI SE PUEDE REABRIR SIN RIESGO: que quedo en CRD.PGPC
-- =====================================================================================
-- Esperado: 0 filas. Si el ORA-01400 reviento en CADA jubilado, no se grabo ninguna.
--
-- COMO SE LEE:
--   0 filas          -> limpio. Reabrir la cabecera es todo lo que hace falta (bloque 4).
--   N filas en 0     -> se fijaron seguros en cero: mirar el bloque 3 antes de decidir.
--   N filas con valor-> ⛔ PARAR Y AVISAR. Se pago algo: reabrir podria duplicar.

SELECT COUNT(*)                                                       AS FILAS,
       SUM(CASE WHEN p.PGPCVLSG IS NULL THEN 1 ELSE 0 END)            AS SEGURO_NULO,
       SUM(CASE WHEN p.PGPCVLSG = 0 THEN 1 ELSE 0 END)                AS SEGURO_EN_CERO,
       SUM(CASE WHEN p.PGPCVLSG > 0 THEN 1 ELSE 0 END)                AS SEGURO_CON_VALOR,
       SUM(NVL(p.PGPCVLSG, 0))                                        AS TOTAL_FIJADO
  FROM CRD.PGPC p
 WHERE p.PGPCANNO = 2026 AND p.PGPCMESS = 9;


-- =====================================================================================
-- 3. EL ANCLA: ¿se movio algun aporte del tipo 23 en septiembre?
-- =====================================================================================
-- Esto es lo que H46 llama "el ancla envenenada": un movimiento NEGATIVO de tipo 23 en
-- 9/2026 haria que el proximo intento considere a ese jubilado "al dia" y le fije el
-- seguro en 0, sin error.
--
-- Esperado: 0 filas. El ORA-01400 revirtio la transaccion de cada jubilado
-- (generarSeguroIndividual corre REQUIRES_NEW y el fallo deshace TODO lo suyo,
-- incluido cualquier movimiento de aporte), asi que no deberia haber quedado ninguno.
--
-- ⛔ Si devuelve filas, PARAR Y AVISAR: reabrir la corrida no alcanza y hay que decidir
--    que hacer con esos movimientos antes de reintentar.

SELECT COUNT(*)                                                       AS MOVIMIENTOS_SEPT,
       SUM(CASE WHEN a.APRTVLRR < 0 THEN 1 ELSE 0 END)                AS NEGATIVOS,
       MIN(a.APRTFCTR)                                                AS DESDE,
       MAX(a.APRTFCTR)                                                AS HASTA
  FROM CRD.APRT a
 WHERE a.TPAPCDGO = 23
   AND a.APRTFCTR >= TO_DATE('2026-09-01', 'YYYY-MM-DD')
   AND a.APRTFCTR <  TO_DATE('2026-10-01', 'YYYY-MM-DD');


-- =====================================================================================
-- 4. LA REAPERTURA — COMENTADA. Solo si el bloque 1 dio total 0, el 2 dio 0 filas y el
--    3 dio 0 negativos.
-- =====================================================================================
-- Devuelve la cabecera al estado "seguro pendiente" para poder reintentar. NO borra
-- nada: limpia tambien los datos de cierre para que no quede una fecha y un usuario de
-- un cierre que no pago nada.
--
-- ⚠️ NO correr esto antes de desplegar el WAR con la correccion de PGPCVLRR: sin esa
--    correccion el reintento vuelve a fallar igual y vuelve a cerrar la corrida.
--
-- UPDATE CRD.CRJB
--    SET CRJBESSG = 0,
--        CRJBFCSG = NULL,
--        CRJBUSSG = NULL,
--        CRJBVLSG = NULL,
--        CRJBIDSG = NULL,
--        CRJBCTSG = NULL
--  WHERE CRJBANNO = 2026
--    AND CRJBMESS = 9
--    AND CRJBESSG = 1
--    AND NVL(CRJBVLSG, 0) = 0;
--
-- -- Esperado: 1 fila. Si dice 0, es que CRJBVLSG NO era cero: PARAR, el filtro protege
-- -- justamente de reabrir una corrida que si pago.
-- COMMIT;


-- =====================================================================================
-- 5. CONTROL POSTERIOR
-- =====================================================================================
-- Esperado: ESTADO_SEGURO = 0 y todo lo demas en null. El estado de PENSIONES no se
-- toca: si estaba en 0 sigue en 0.

SELECT c.CRJBANNO, c.CRJBMESS, c.CRJBESSG AS ESTADO_SEGURO, c.CRJBVLSG AS TOTAL_SEGURO,
       c.CRJBFCSG, c.CRJBUSSG, c.CRJBIDSG, c.CRJBESPN AS ESTADO_PENSIONES
  FROM CRD.CRJB c
 WHERE c.CRJBANNO = 2026 AND c.CRJBMESS = 9;


-- =====================================================================================
-- 6. Y LO QUE HAY QUE ARREGLAR EN EL CODIGO, QUE ESTE SCRIPT NO RESUELVE
-- =====================================================================================
-- 1. PGPCVLRR: ya despachado. Sin eso, reintentar vuelve a fallar.
--
-- 2. ⛔ EL DISEÑO DEL BUCLE, que es lo que convirtio un error corregible en un bloqueo:
--    generarSeguroDelMes atrapa la excepcion de cada jubilado, suma a conError y sigue,
--    y despues CIERRA la cabecera igual. Deberia NO cerrar la corrida si conError > 0,
--    o al menos no cerrarla si NO se genero ni un solo seguro. Hoy un fallo total se
--    presenta como exito con total $0 y deja el periodo trabado.
--    ⇒ Es la misma familia de H78 (ocho jubilados sin cobrar sin que nadie se entere) y
--      de H46 (una corrida que falla a medias no se puede reintentar).
--    ⇒ PENDIENTE DE DECISION DEL USUARIO, sin despachar.
-- =====================================================================================


-- =====================================================================================
-- ✅ CONFIRMADO CON LA PANTALLA — 2026-09-22. Y CORRIGE EL DIAGNOSTICO DEL H78.
-- =====================================================================================
-- El mensaje que devolvio el proceso, textual:
--
--   "Seguro medico 9/2026 - 0 jubilados nuevos y 0 que ya tenian el seguro fijado,
--    $0.0 en total hacia el proveedor (sin orden, total $0), 182 CON ERROR, DE 182
--    EVALUADOS. 0 jubilado(s) con el seguro topado por saldo, sin ancla o al dia."
--
-- Y despues: "El seguro medico de 9/2026 ya se genero el 2026-09-22T15:23:47 por
-- GROBAYO. No se puede generar dos veces."
--
-- ⇒ 182 CON ERROR DE 182. Fallaron TODOS, por el ORA-01400 de PGPCVLRR. El bucle los
--   conto, siguio, y cerro la cabecera igual a las 15:23:47 con total $0.
--
-- ⭐ Y ESTO CORRIGE EL DIAGNOSTICO DEL H78, que hay que actualizar en el tablero:
--   el $0 de los intentos anteriores NO venia del ancla envenenada ni de las VPPC
--   duplicadas. Venia de que TODOS los jubilados fallaban al insertar su fila de PGPC.
--   Lo prueba la propia pantalla por dos lados:
--     - "0 jubilado(s) con el seguro topado por saldo, SIN ANCLA O AL DIA" -> el ancla
--       no descarto a nadie.
--     - "182 con error" -> el total $0 se explica entero por los errores.
--
--   ⇒ El sql/239 (VPPC duplicadas) deja de ser la hipotesis principal. La resta
--     190 - 182 = 8 sigue siendo rara y vale medirla, pero NO es la causa del $0.
--     Correrlo pasa de urgente a informativo.
-- =====================================================================================

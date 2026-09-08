-- =====================================================================
-- e2-28 — Reabrir a mano el periodo de nomina 81 (8/2026)
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-08
--
-- ⚠️ NO ES SOLO LECTURA. Borra acumulados y cambia el estado del periodo.
--    Correr el BLOQUE 0 primero, LEERLO, y recien despues seguir.
--
-- 🔴 POR QUE NO ALCANZA CON UN UPDATE DEL ESTADO
--   El usuario pidio "el update para cambiar de estado y reprocesar". Cambiar
--   solo PRDNESTD deja el periodo reabierto pero SUCIO, y el recalculo sale mal
--   de dos formas distintas, ninguna de las cuales da error:
--
--   1. LOS ACUMULADOS QUEDAN. El cierre escribio las filas de RHH.ACMN del
--      periodo (dias trabajados, bases de IESS, decimos, fondos...). Si no se
--      retiran, el recalculo las SUMA otra vez. Lo dice el propio codigo de
--      reabrirPeriodo (ProcesoNominaServiceImpl:578): "El cierre escribio
--      acumulados: hay que retirarlos o el recalculo los duplica".
--      Y el decimo cuarto se calcula sumando esos dias: un acumulado duplicado
--      infla el beneficio de todo el año.
--
--   2. LA REFERENCIA AL ASIENTO QUEDA. Mientras PRDNASNT tenga valor, la
--      pantalla NO habilita "Reabrir" —es exactamente la condicion que lo tiene
--      bloqueado hoy— y el periodo sigue diciendo que su rol ya se contabilizo.
--
--   Este script hace lo mismo que hace el boton, mas la limpieza de la
--   referencia al asiento que el boton no necesita hacer porque nunca se le
--   habilita en este caso.
--
-- 🔴 EL SUPUESTO QUE HAY QUE CONFIRMAR ANTES
--   El usuario afirma que la contabilidad de este periodo NO se genero. El
--   BLOQUE 0 lo verifica contra CNT.ASNT. Si resultara que el asiento SI existe,
--   ESTE SCRIPT NO SE CORRE: habria que reversar ese asiento primero, porque
--   recontabilizar generaria un SEGUNDO asiento por el mismo mes y la nomina
--   quedaria contabilizada dos veces.
--
-- Estados (RhhEstadoPeriodoNomina): 1 ABIERTO, 2 EN_CALCULO, 3 CALCULADO,
-- 4 APROBADO, 5 CONTABILIZADO, 6 PAGADO, 7 CERRADO, 8 ANULADO.
-- Se deja en 3 (CALCULADO), que es a donde lo deja reabrirPeriodo.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 — CONTROLES ANTES. Correr esto solo, LEER, y pegar el resultado.
-- =====================================================================

-- 0.1 El periodo y sus referencias a asientos.
--     🔴 ESTO DECIDE SI EL SCRIPT SE PUEDE CORRER:
--        - existe_rol = 0 y existe_provisiones = 0  -> la contabilidad NO se
--          genero, como dice el usuario. Se puede seguir.
--        - alguno en 1 -> EL ASIENTO EXISTE. PARAR Y AVISAR: hay que reversarlo
--          antes, no borrar la referencia y dejarlo huerfano en el mayor.
SELECT '0.1 - el periodo' AS control,
       p.PRDNCDGO AS periodo, p.PRDNMSEE AS mes, p.PRDNANOO AS anio,
       p.PRDNESTD AS estado, p.PRDNFCCR AS fecha_cierre, p.PRDNUSCR AS cerrado_por,
       p.PRDNASNT AS asiento_rol, p.PRDNASPR AS asiento_provisiones,
       (SELECT COUNT(*) FROM CNT.ASNT a WHERE a.ASNTCDGO = p.PRDNASNT) AS existe_rol,
       (SELECT COUNT(*) FROM CNT.ASNT a WHERE a.ASNTCDGO = p.PRDNASPR) AS existe_provisiones
  FROM RHH.PRDN p
 WHERE p.PRDNCDGO = 81;

-- 0.2 Cuantos acumulados va a borrar el BLOQUE 1, por tipo.
--     Es lo que el recalculo va a volver a escribir. Anotar los numeros: sirven
--     para contrastar despues de reprocesar.
SELECT '0.2 - acumulados que se borran' AS control,
       a.ACMNTPAC AS tipo, COUNT(*) AS filas,
       SUM(NVL(a.ACMNVLOR,0)) AS suma_valor, SUM(NVL(a.ACMNDIAS,0)) AS suma_dias
  FROM RHH.ACMN a
 WHERE a.PRDNCDGO = 81
 GROUP BY a.ACMNTPAC
 ORDER BY a.ACMNTPAC;

-- 0.3 ⚠️ Que NO haya nada colgando de este periodo que el reproceso no rehaga.
--     ESPERADO: 0 en las dos. Si hay una orden de pago de nomina emitida, o
--     valores no pagados que ya se retuvieron contra este periodo, PARAR:
--     reprocesar por debajo de eso deja los importes desalineados.
SELECT '0.3 - dependencias del periodo' AS control,
       (SELECT COUNT(*) FROM RHH.RDPG o WHERE o.PRDNCDGO = 81)                    AS ordenes_de_pago,
       (SELECT COUNT(*) FROM RHH.VNPG v WHERE v.VNPGPRNM = 81 AND v.VNPGESTD = 2) AS valores_no_pagados_retenidos
  FROM DUAL;


-- =====================================================================
-- BLOQUE 1 — REABRIR. ⛔ CONDICIONES OBLIGATORIAS
-- =====================================================================
--   ✅ El 0.1 devolvio existe_rol = 0 Y existe_provisiones = 0
--   ✅ El 0.3 devolvio 0 en las dos
--   ❌ Si alguna no se cumple, NO CORRER. Avisar.

-- 1.1 Retirar los acumulados del periodo. Es lo mismo que hace
--     reabrirPeriodo; sin esto el recalculo los duplica.
DELETE FROM RHH.ACMN WHERE PRDNCDGO = 81;

-- 1.2 Devolver el periodo a CALCULADO y limpiar el rastro del cierre y de la
--     contabilizacion que nunca llego a generar asiento.
UPDATE RHH.PRDN
   SET PRDNESTD = 3,
       PRDNFCCR = NULL,
       PRDNUSCR = NULL,
       PRDNASNT = NULL,
       PRDNASPR = NULL,
       PRDNOBSR = 'Reabierto a mano (e2-28, 2026-09-08): quedo CERRADO con referencia'
                  || ' a asiento inexistente. Se verifico contra CNT.ASNT antes de limpiar.'
 WHERE PRDNCDGO = 81;

COMMIT;


-- =====================================================================
-- BLOQUE 2 — CONTROLES DESPUES. Correrlos SIEMPRE.
-- =====================================================================

-- 2.1 ESPERADO: estado 3, las cuatro columnas del cierre y los asientos en NULL.
SELECT '2.1 - el periodo reabierto' AS control,
       p.PRDNCDGO, p.PRDNESTD AS estado_debe_ser_3,
       p.PRDNFCCR AS fecha_cierre, p.PRDNUSCR AS cerrado_por,
       p.PRDNASNT AS asiento_rol, p.PRDNASPR AS asiento_provisiones
  FROM RHH.PRDN p WHERE p.PRDNCDGO = 81;

-- 2.2 ESPERADO: 0.
SELECT '2.2 - acumulados restantes' AS control, COUNT(*) AS debe_ser_cero
  FROM RHH.ACMN WHERE PRDNCDGO = 81;


-- =====================================================================
-- QUE SIGUE
-- =====================================================================
-- 1. En la pantalla del periodo, "Reabrir" ya no hace falta: el periodo queda
--    en CALCULADO y con los botones de Validar / Calcular / Aprobar
--    habilitados.
-- 2. Ingresar el valor que faltaba.
-- 3. Recalcular, aprobar, contabilizar rol, contabilizar provisiones y cerrar.
-- 4. Contrastar contra los numeros del 0.2: los acumulados que se borraron
--    tienen que volver a escribirse, y las sumas por tipo deberian coincidir
--    salvo por el valor que se agrego.
--
-- ⚠️ NO recalcular decimos ni fondos de reserva entre el BLOQUE 1 y el
--    reproceso: entre esos dos momentos los acumulados de 8/2026 no existen, y
--    el decimo tercero —cuya ventana va de diciembre a noviembre— los suma.
--    Si se calcula ahi en el medio, sale de menos.
-- =====================================================================

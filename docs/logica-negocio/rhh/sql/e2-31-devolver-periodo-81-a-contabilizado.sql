-- =====================================================================
-- e2-31 — Devolver el periodo 81 (8/2026) de CERRADO a CONTABILIZADO
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-08
--
-- ⚠️ NO ES SOLO LECTURA. Cambia el estado del periodo.
--    Correr el BLOQUE 0 primero, LEERLO, y recien despues seguir.
--
-- 🔴 POR QUE EXISTE
--   El usuario contabilizo el rol y las provisiones de 8/2026 (asientos
--   REC-2026-08-0002 y -0003) y despues CERRO el periodo. Al ir a generar la
--   orden de pago, el boton esta deshabilitado sin explicacion.
--
--   Causa, leida en el codigo: la orden de pago exige APROBADO(4),
--   CONTABILIZADO(5) o PAGADO(6) -GeneracionOrdenPagoServiceImpl:1060 y
--   ESTADOS_GENERA_ORDEN_PAGO en el FE-. CERRADO(7) no esta. Y cerrarPeriodo
--   ACEPTA cerrar desde CONTABILIZADO, o sea antes de pagar: el sistema deja
--   producir un estado desde el cual despues se niega a pagar. Ademas, el
--   arbitro le dio al usuario el orden "aprobar -> contabilizar -> cerrar", que
--   es exactamente el camino a esta trampa. El orden correcto es:
--   aprobar -> contabilizar -> GENERAR ORDEN -> CONFIRMAR ACREDITACION -> cerrar.
--
-- POR QUE NO SIRVE "REABRIR"
--   reabrirPeriodo borra los acumulados y deja el periodo en CALCULADO: obliga
--   a recalcular, aprobar y CONTABILIZAR OTRA VEZ, con los asientos ya emitidos.
--   Eso duplica la contabilidad. Y la pantalla ni lo ofrece mientras PRDNASNT
--   tenga valor. No es el camino.
--
-- QUE HACE ESTE SCRIPT
--   Deshace SOLO lo que hizo el cierre en la cabecera: estado 7 -> 5, y limpia
--   fecha y usuario de cierre. NO toca los acumulados (RHH.ACMN): cerrarPeriodo
--   los borra y los reescribe cada vez ("para que cerrar dos veces no
--   duplique"), asi que dejarlos no molesta y borrarlos no hace falta. NO toca
--   los asientos ni sus referencias.
--
-- Estados (RhhEstadoPeriodoNomina): 1 ABIERTO, 2 EN_CALCULO, 3 CALCULADO,
-- 4 APROBADO, 5 CONTABILIZADO, 6 PAGADO, 7 CERRADO, 8 ANULADO.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 — CONTROLES ANTES. Correr esto solo, LEER, y pegar el resultado.
-- =====================================================================

-- 0.1 El periodo. ESPERADO: estado 7, los dos asientos con valor y existentes.
--     🔴 Si estado NO es 7, PARAR: este script es solo para deshacer un cierre.
SELECT '0.1 - el periodo' AS control,
       p.PRDNCDGO, p.PRDNMSEE AS mes, p.PRDNANOO AS anio,
       p.PRDNESTD AS estado, p.PRDNFCCR AS fecha_cierre, p.PRDNUSCR AS cerrado_por,
       p.PRDNASNT AS asiento_rol, p.PRDNASPR AS asiento_provisiones,
       (SELECT COUNT(*) FROM CNT.ASNT a WHERE a.ASNTCDGO = p.PRDNASNT) AS existe_rol,
       (SELECT COUNT(*) FROM CNT.ASNT a WHERE a.ASNTCDGO = p.PRDNASPR) AS existe_prov
  FROM RHH.PRDN p
 WHERE p.PRDNCDGO = 81;

-- 0.2 Que no haya ya una orden de pago del periodo. ESPERADO: 0.
SELECT '0.2 - ordenes de pago existentes' AS control, COUNT(*) AS debe_ser_cero
  FROM RHH.RDPG o WHERE o.PRDNCDGO = 81;

-- 0.3 Los acumulados que dejo el cierre. Se quedan como estan; es solo para
--     contrastar despues del cierre definitivo (deberian reescribirse iguales).
SELECT '0.3 - acumulados del cierre' AS control,
       a.ACMNTPAC AS tipo, COUNT(*) AS filas, SUM(NVL(a.ACMNVLOR,0)) AS suma_valor
  FROM RHH.ACMN a WHERE a.PRDNCDGO = 81
 GROUP BY a.ACMNTPAC ORDER BY a.ACMNTPAC;


-- =====================================================================
-- BLOQUE 1 — DESHACER EL CIERRE. Solo si el 0.1 dio estado 7.
-- =====================================================================
UPDATE RHH.PRDN
   SET PRDNESTD = 5,
       PRDNFCCR = NULL,
       PRDNUSCR = NULL,
       PRDNOBSR = 'Cierre deshecho (e2-31, 2026-09-08): se cerro antes de generar la'
                  || ' orden de pago. Asientos intactos; se vuelve a cerrar despues de pagar.'
 WHERE PRDNCDGO = 81
   AND PRDNESTD = 7;

COMMIT;


-- =====================================================================
-- BLOQUE 2 — CONTROL DESPUES. ESPERADO: estado 5, cierre en NULL, asientos igual.
-- =====================================================================
SELECT '2.1 - el periodo' AS control,
       p.PRDNCDGO, p.PRDNESTD AS estado_debe_ser_5,
       p.PRDNFCCR AS fecha_cierre, p.PRDNUSCR AS cerrado_por,
       p.PRDNASNT AS asiento_rol, p.PRDNASPR AS asiento_provisiones
  FROM RHH.PRDN p WHERE p.PRDNCDGO = 81;


-- =====================================================================
-- QUE SIGUE
-- =====================================================================
-- 1. Refrescar la pantalla de ordenes de pago: "Generar orden" se habilita.
-- 2. Generar la orden, descargar el archivo, confirmar la acreditacion.
-- 3. RECIEN AHI cerrar el periodo. El cierre reescribe los acumulados.
--
-- ⚠️ LO QUE ESTE SCRIPT NO ARREGLA (pendiente de decision del usuario)
-- - El sistema sigue permitiendo cerrar desde CONTABILIZADO sin pagar, y la
--   pantalla de ordenes no explica por que el boton esta gris. Esto se va a
--   repetir. Opciones: (a) que la orden de pago admita CERRADO, o (b) que
--   cerrar exija PAGADO -pero eso rompe el flujo historico, que cierra sin
--   pagar por el sistema-. Como minimo, (c) que la pantalla diga el motivo.
-- =====================================================================

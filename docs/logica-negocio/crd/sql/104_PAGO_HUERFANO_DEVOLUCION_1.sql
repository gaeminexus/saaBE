-- =====================================================================================
-- Pago 130 huerfano — apunta a una devolucion de aportes que NO EXISTE
-- FECHA: 2026-08-31 · Equipo A de crd
--
-- ⛔ SOLO LECTURA. Ni un UPDATE, ni una anulacion. Este script junta los hechos para que
--    el usuario decida; la decision es de negocio, no tecnica.
--
-- LO QUE YA SABEMOS (verificado en produccion, misma base que el WAR):
--   · CRD.DVAP no tiene ninguna fila con DVAPCDGO = 1.
--   · PGS.PGTR tiene el pago 130: origen CRD_DEVOLUCION_APORTE, idOrigen 1,
--     estado 3 (CONFIRMADO), $5.564,27, programado 2025-08-25, creado 2026-08-28 16:45.
--   · El asiento 8380 del log NO quedo grabado: la transaccion revirtio bien.
--
-- POR QUE BLOQUEA. Al registrar una devolucion nueva, el sistema pregunta si ese documento
-- ya tiene un pago vigente (estados REGISTRADO/EN_ARCHIVO/CONFIRMADO). El pago 130 lo
-- tiene, asi que corta. Si CRD.DVAP esta vacia, la devolucion nueva vuelve a tomar el
-- codigo 1 y choca con este pago huerfano una y otra vez.
--
-- LA PREGUNTA QUE HAY QUE CONTESTAR ANTES DE TOCAR NADA:
-- **¿Los $5.564,27 se pagaron de verdad?** El pago esta CONFIRMADO, que es el estado que
-- pone tesoreria cuando el dinero salio. Si salio, anularlo para "destrabar" la pantalla
-- borraria el rastro de una transferencia real. Si nunca salio, el pago es basura de un
-- intento viejo y hay que revertirlo.
-- =====================================================================================


-- 1. ¿CRD.DVAP esta vacia, o solo falta la 1? Cambia por completo la lectura:
--    vacia = nunca se registro ninguna devolucion (o se borraron todas).
--    con filas = la 1 se borro especificamente.
SELECT COUNT(*) AS CUANTAS_DEVOLUCIONES,
       MIN(d.DVAPCDGO) AS CODIGO_MINIMO,
       MAX(d.DVAPCDGO) AS CODIGO_MAXIMO
FROM   CRD.DVAP d;


-- 2. El pago 130 completo: quien es el beneficiario, cuando se confirmo, con que
--    referencia bancaria y con que observacion. La referencia bancaria (PGTRRFBN) es el
--    dato que dice si el dinero salio de verdad.
SELECT p.PGTRCDGO, p.PGTRORGN, p.PGTRIDOR, p.PGTRESTD, p.PGTRVLOR,
       p.PGTRBFNM AS BENEFICIARIO, p.PGTRBFID AS IDENTIFICACION,
       p.PGTRBFCT AS CUENTA_BENEFICIARIO,
       p.PGTRFPRG AS FECHA_PROGRAMADA, p.PGTRFCRG AS FECHA_CREACION,
       p.PGTRFRSP AS FECHA_RESPUESTA, p.PGTRRFBN AS REFERENCIA_BANCARIA,
       p.PGTRMTVO AS MOTIVO, p.PGTROBSR AS OBSERVACION
FROM   PGS.PGTR p
WHERE  p.PGTRCDGO = 130;


-- 3. ¿Hay otros pagos huerfanos del mismo origen? Si el 130 no es el unico, no es un
--    accidente aislado y hay que mirar el proceso, no solo esta fila.
SELECT p.PGTRCDGO, p.PGTRIDOR, p.PGTRESTD, p.PGTRVLOR, p.PGTRFCRG,
       CASE WHEN EXISTS (SELECT 1 FROM CRD.DVAP d WHERE d.DVAPCDGO = p.PGTRIDOR)
            THEN 'TIENE DEVOLUCION' ELSE '*** HUERFANO ***' END AS SITUACION
FROM   PGS.PGTR p
WHERE  p.PGTRORGN = 'CRD_DEVOLUCION_APORTE'
ORDER  BY p.PGTRCDGO;


-- 4. ¿Que asientos genero ese pago? Si esta CONFIRMADO deberia tener contabilidad detras.
--    Un pago confirmado SIN asiento es distinto de uno con asiento: el segundo movio
--    saldos que habria que revertir.
SELECT a.ASNTCDGO, a.ASNTESTD, a.ASNTFCHA, a.ASNTOBSR
FROM   CNT.ASNT a
WHERE  a.ASNTOBSR LIKE '%CRD_DEVOLUCION_APORTE%'
OR     a.ASNTOBSR LIKE '%evolucion%aporte%'
ORDER  BY a.ASNTCDGO DESC
FETCH  FIRST 20 ROWS ONLY;


-- =====================================================================================
-- LAS OPCIONES — ninguna se ejecuta sin decision del usuario
-- =====================================================================================
-- (A) El dinero SALIO. Entonces el pago es legitimo y lo que falta es la devolucion que
--     lo justifica: hay que reconstruir la fila de CRD.DVAP, no borrar el pago. Anular un
--     pago confirmado de $5.564,27 dejaria la contabilidad sin el respaldo de una
--     transferencia que si ocurrio.
--
-- (B) El dinero NO salio (se confirmo por error, o el proceso quedo a medias el 28-08).
--     Entonces se revierte con el mecanismo que ya existe, NUNCA con un UPDATE:
--         POST /rest/pgtr/revertirConfirmado/130   body: { "motivo": "...", "idUsuario": N }
--     Ese endpoint genera el reverso contable que corresponda; un UPDATE del estado no.
--
-- ⚠️ En los dos casos, el arreglo de fondo es el mismo y ya esta encargado: que el control
-- de "ya tiene pago vigente" corra AL PRINCIPIO y diga cual es el pago y en que estado,
-- en vez de fallar despues de anunciar en el log que el asiento se genero.
-- =====================================================================================

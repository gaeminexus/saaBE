-- =====================================================================================
-- DIAGNOSTICO — por que la Devolucion 1 no se puede registrar
-- FECHA: 2026-08-31 · Equipo A de crd
--
-- ⛔ SOLO LECTURA. Este script no modifica nada. No hay bloque de UPDATE a proposito.
--
-- EL SINTOMA. Al registrar la devolucion de aportes, WildFly:
--   ✅ Asiento de reclasificacion generado - Devolucion 1 - Asiento 8380 - $5544.0
--   ERROR_ORDEN_PAGO: ... El documento 1 de CRD_DEVOLUCION_APORTE ya tiene un pago
--   vigente. Anulelo o reviertalo antes de registrar otro.
--
-- LA HIPOTESIS QUE SE DESCARTO. Se penso que un intento anterior habia dejado un pago
-- colgado tras un rollback. **El codigo dice que no.** Toda la cadena
-- (registrarDevolucion -> generarAsientoReclasificacion -> generarAsiento ->
-- registrarPagoDeOrigenExterno) corre en la MISMA transaccion REQUIRED: un fallo
-- revierte todo junto, no deja mitades.
--
-- LA HIPOTESIS VIGENTE, y por que. El pago de una devolucion nace POR_APROBAR (estado 0,
-- sin cuenta bancaria, a proposito). Pero el control que bloquea
-- (PagoProgramadoDaoServiceImpl.selectVigentesByOrigen:158) solo cuenta como "vigente"
-- los estados REGISTRADO / EN_ARCHIVO / CONFIRMADO. **Un pago recien creado NO alcanza a
-- bloquear.** Para que este bloqueando hoy, ese pago tuvo que ser APROBADO por tesoreria.
--
-- O sea: lo mas probable es que la Devolucion 1 **ya se proceso una vez, completa y bien**,
-- y este intento sea un SEGUNDO registro sobre el mismo documento. Estas consultas lo
-- confirman o lo desmienten con datos.
-- =====================================================================================


-- 1. Estado actual de la devolucion. Si DVAPIDPG y DVAPNMAS ya vienen llenos, la
--    devolucion YA tiene pago y asiento: se proceso antes.
SELECT d.DVAPCDGO, d.DVAPESTD, d.DVAPIDPG, d.DVAPNMAS, d.DVAPNMRC,
       d.DVAPFCRG, d.DVAPUSRG
FROM   CRD.DVAP d
WHERE  d.DVAPCDGO = 1;


-- 2. TODOS los pagos que alguna vez apuntaron a esta devolucion — no solo los vigentes.
--    Es la consulta que decide: si hay UNA fila en estado 1/2/3, esa es la que bloquea.
--    Estados: 0=POR_APROBAR (no bloquea) · 1=REGISTRADO · 2=EN_ARCHIVO · 3=CONFIRMADO
SELECT p.PGTRCDGO, p.PGTRORGN, p.PGTRIDOR, p.PGTRESTD, p.PGTRVLOR,
       p.PGTRFPRG, p.PGTRFCRG
FROM   PGS.PGTR p
WHERE  p.PGTRORGN = 'CRD_DEVOLUCION_APORTE'
AND    p.PGTRIDOR = 1
ORDER  BY p.PGTRCDGO;


-- 3. El asiento 8380 del log: ¿quedo grabado, o se revirtio con la transaccion?
--    Si NO devuelve filas, la transaccion revirtio bien y el log solo mostro un paso
--    que despues se deshizo — que es lo esperado.
SELECT a.ASNTCDGO, a.ASNTESTD, a.ASNTFCHA, a.ASNTOBSR
FROM   CNT.ASNT a
WHERE  a.ASNTCDGO = 8380;


-- 4. Los asientos que la devolucion 1 sí dejo grabados, si los hay.
SELECT a.ASNTCDGO, a.ASNTESTD, a.ASNTFCHA, a.ASNTOBSR
FROM   CNT.ASNT a
WHERE  a.ASNTCDGO IN (SELECT d.DVAPNMAS FROM CRD.DVAP d WHERE d.DVAPCDGO = 1
                      UNION ALL
                      SELECT d.DVAPNMRC FROM CRD.DVAP d WHERE d.DVAPCDGO = 1);


-- =====================================================================================
-- COMO SE DESBLOQUEA — y por que NO con un UPDATE
-- =====================================================================================
-- Si la consulta 2 devuelve un pago en estado 1, 2 o 3, ESE es el que bloquea. **No se
-- toca con UPDATE.** Un UPDATE del estado dejaria sin reverso cualquier asiento que ese
-- pago haya generado — un descuadre contable silencioso.
--
-- El mecanismo seguro ya existe y es el que el propio mensaje de error sugiere:
--
--   estado 1 (REGISTRADO) o 2 (EN_ARCHIVO)  ->  POST /rest/pgtr/anular/{idPago}
--   estado 3 (CONFIRMADO)                   ->  POST /rest/pgtr/revertirConfirmado/{idPago}
--
--   body: { "motivo": "...", "idUsuario": N }
--
-- ⚠️ Pero antes de anular nada: si la consulta 1 muestra que la devolucion ya tiene pago
-- y asiento, **la pregunta no es como desbloquear, es si esta devolucion debe registrarse
-- de nuevo.** Anular un pago que tesoreria ya aprobo para volver a registrar el mismo
-- documento puede ser exactamente lo que NO hay que hacer.
-- =====================================================================================

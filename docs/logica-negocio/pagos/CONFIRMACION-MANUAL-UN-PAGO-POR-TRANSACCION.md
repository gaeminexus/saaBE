# Confirmación manual de pagos: cada pago en su propia transacción

**Equipo:** `omen-saa-2` · **Escrito:** 2026-10-15 por `omen-saa-2-arb`, antes de despachar.
**Origen:** producción, 15-10-2026. Al confirmar pagos en Tesorería → Pagos → Confirmación, la pantalla
muestra *«Error al confirmar los pagos: org.hibernate.exception.GenericJDBCException: could not prepare
statement [IJ031070: Transaction cannot proceed: STATUS_MARKED_ROLLBACK] [update PGS.PGTR set …]»* y no se
confirma ninguno.

## Causa (verificada en el código)

`PagoProgramadoServiceImpl.confirmarPagosManual` (~l.2092) recorre los pagos dentro de UNA sola transacción
(la del método, REQUIRED por defecto) y atrapa el error de cada pago con `try/catch (Throwable)` para seguir
con los demás. Eso no sirve cuando el error es una excepción de sistema que viene de otro EJB
(`aplicacionPagoCxpService`, `movimientoBancoService`, `asientoService`… vía `contabilizarSegunOrigen`):
el contenedor ya marcó la transacción entera como *rollback-only*. El `catch` anota el error en `errores`,
pero el siguiente `save`/`em.flush()` revienta con `STATUS_MARKED_ROLLBACK` y el REST devuelve 500.

Consecuencias:
- Un solo pago con problema tumba toda la confirmación: no se confirma ninguno.
- El usuario ve un mensaje de Hibernate que no dice nada. El motivo real solo queda en el log del
  servidor (`⚠ Error confirmando manualmente el pago N: …`).

## La regla nueva

Cada pago se confirma en **su propia transacción** (`REQUIRES_NEW`): el asiento, el movimiento bancario, la
aplicación y el cambio de estado de ESE pago se graban juntos o no se graba nada de ese pago. El error de un
pago no afecta a los demás, y su motivo real llega a la pantalla en `errores`.

## BACKEND

1. Método nuevo en `PagoProgramadoService` (interfaz @Local) e implementación con
   `@TransactionAttribute(REQUIRES_NEW)`: confirma UN pago. Mueve ahí, sin cambiar la lógica, el cuerpo de
   hoy del bucle: `em.find`, el control de estado (REGISTRADO / EN_ARCHIVO; si no, `IncomeException` con el
   mismo texto de hoy), referencia, fecha, observación, `contabilizarSegunOrigen`, estado CONFIRMADO,
   `save`. Devuelve lo que el bucle necesita: si generó asiento o no (para la advertencia de «confirmado sin
   contabilidad») y el id del lote.
2. `confirmarPagosManual` lo llama por la vista de negocio del propio bean
   (`sessionContext.getBusinessObject(PagoProgramadoService.class)`, mismo idiom que
   `FacturaServiceImpl.self()`), NO con `this.` (con `this.` la anotación no aplica).
3. En el `catch` de cada pago, el mensaje que va a `errores` es el de la **causa raíz** (bajar por
   `getCause()` hasta la última con mensaje), no el de `EJBException`/`TransactionRolledbackLocalException`.
   Además, `printStackTrace` al log del servidor.
4. El cierre de lotes (`cierraLoteSiNoQuedanPendientes`) queda después del bucle, igual que hoy. El
   `em.flush()` del final ya no es necesario para los pagos; dejarlo solo si el cierre de lotes lo requiere.
5. El contrato del REST no cambia: mismo request; la respuesta sigue siendo
   `{exito, mensaje, confirmados, errores?, advertencias?}`. Solo cambia que ahora llega 200 con `errores`
   en vez de 500.

## Lo que NO se hace en este cambio

- No se toca `contabilizarSegunOrigen` ni los servicios que llama: el defecto concreto del pago que falló en
  producción se ve en el log o, ya desplegado esto, en la pantalla, y se arregla aparte.
- `aprobar` y `cargarRespuesta` tienen el mismo patrón de lote en una sola transacción (pendiente §58.1).
  No entran aquí.

# Corrección — «Cancelar el crédito» de un préstamo con todas las cuotas vencidas (2026-11-18)

**Equipo:** `omen-saa-3` · **Urgente, reportado en producción.**

## Síntoma
Préstamo en plazo vencido con **todas** sus cuotas vencidas. En Cobros personales se usa «Cancelar el crédito»
(precancelación). El cobro se registra y Contabilidad lo aprueba, pero al procesarlo en Crédito falla:
`Error al procesar el cobro 286: SIN_CUOTAS_FUTURAS: No hay cuotas futuras que precancelar; use pagarCuota`.

## Causa (verificada en el código)
`ProcesoPagoPrestamoServiceImpl.precancelar()` (~línea 1284) lanza `SIN_CUOTAS_FUTURAS` cuando
`calculo.futuras` está vacío, aunque haya deuda exigible. Pero `simularPrecancelacion()` sí da un total (todo
exigible) y la pantalla deja registrar el cobro: el registro acepta lo que el proceso rechaza. Un préstamo vencido
completo **siempre** cae en este caso. El fallo es una `IncomeException` (rollback), así que el cobro 286 queda como
estaba (aprobado, sin procesar) y se puede volver a procesar después del despliegue.

## Decisión
`precancelar()` acepta el caso «sin cuotas futuras y con deuda exigible»: es un pago total de lo exigible.
- Paga las exigibles igual que hoy (paso 2).
- **Se saltan** los pasos que existen solo por el capital futuro: la ancla y `DTPRSLOT` (3-4), el `PGPR` del capital
  futuro (5) y el cierre de futuras a CANCELADA_ANTICIPADA (3). No se crea un `PGPR` en 0.
- Estado final del préstamo: **CANCELADO (3)**, no CANCELADO_ANTICIPADO (4): no se anticipó nada; es el mismo
  estado que pone el motor cuando se paga la última cuota. La garantía de cuentas de orden se sincroniza igual (C31).
- Sigue fallando, con un error claro, si **no hay ni exigibles ni futuras** (no hay deuda).
- Los aportes y la referencia de `PagoAporte` usan el primer PGPR exigible (que existe en este caso).
- El evento sigue siendo de tipo precancelación; el asiento del circuito de cobros (CBCRASN2) no cambia.

## Por verificar en la implementación
- Todo consumidor de `ResultadoPrecancelacion.idCuotaConSaldoOtros` / `idPagoPrestamoCapitalFuturo` tolera `null`.
- `contabilizarPrecancelacion` (vía directa `/prst/precancelar`, sin cobro) con capital futuro 0.
- El reverso/anulación de una precancelación no exige el PGPR de capital futuro ni el SLOT.

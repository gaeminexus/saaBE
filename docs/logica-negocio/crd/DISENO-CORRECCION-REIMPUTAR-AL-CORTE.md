# Diseño — corrección de `reimputarAlCorte` (C33), solo diseño, sin código

*2026-11-25 · omen-saa-1 · incidente de la re-imputación del 2026-11-25. Esto NO está programado; no se vuelve a correr en producción hasta probarlo.*

## Causa (ver `sql/394` y `sql/396`)
La herramienta lee los pagos por `PGPR.PRSTCDGO` (`PagoPrestamoDaoService.selectVigentesByPrestamo`); el control del cierre los lee por `PGPR.DTPRCDGO`
(`DetallePrestamoDaoServiceImpl.selectControlPrelacionAlCorte`). Un PGPR sin `PRSTCDGO` lo ve el control y no la herramienta; la herramienta reparte
dinero sobre cuotas que ya estaban pagadas, y su verificación posterior (`roturaResidual`) es ciega a lo mismo.

## Cambios propuestos

1. **Leer los pagos por cuota.** En `PrelacionCuotasServiceImpl.pagosAlCortePorCuota`, traer los PGPR vigentes de las cuotas del préstamo
   (`p.detallePrestamo.prestamo.codigo = :idPrestamo`, o `DTPRCDGO IN (cuotas del préstamo)`), como ya hacen `selectCapitalAbonadoEnMoraOParcialByPrestamos` y
   vecinos. Un método nuevo del DAO (`selectVigentesByPrestamoPorCuota`) y se deja de usar `selectVigentesByPrestamo` en C33. Los otros dos llamadores de
   `selectVigentesByPrestamo` (`PrestamoServiceImpl:764` revertir desembolso y `:1163` anular préstamo) tienen el mismo defecto: se cambian aparte, como lote chico.
2. **Una sola fuente de criterios.** Hoy la regla vive dos veces: SQL nativo en `selectControlPrelacionAlCorte` y Java en `PrelacionCuotas.detectarRotura`.
   Se unifican en una sola: el control del cierre se vuelve la fuente y la herramienta lo consulta **para un préstamo** (`selectControlPrelacionAlCorte(desde, corte, idPrestamo)`)
   en vez de recalcular la rotura con su propia lectura. El plan de re-imputación (`planificar`) sigue siendo Java, pero parte de las cuotas y pagos leídos
   por el punto 1, con el mismo universo (mismos estados de cartera, `PRSTFCHA ≤ corte`, mismos exentos con la comparación exacta del tipo).
3. **Validar antes de aplicar, y dentro de la misma transacción.** Después de grabar los PGPR nuevos y recalcular las cuotas, volver a ejecutar el control de
   ese préstamo (el mismo SQL del control). Si el préstamo sigue roto, o aparece una `SOBREIMPUTADA`, se hace `rollback` de ese préstamo (ya corre en
   `REQUIRES_NEW`) y se reporta como «no aplicado: <rotura>». Hoy `roturaResidual` solo informa; pasa a ser condición de aplicar.
   Además, el plan se rechaza antes de grabar si algún pago del préstamo tiene `PRSTCDGO` nulo o distinto (se reporta el préstamo para corregir el dato primero).
4. **Simular = ejecutar.** La simulación usa la misma lectura y la misma validación que ejecutar, para que el resultado de simular sea el de ejecutar.
5. **Mora a la fecha real, nunca futura.** `recalcularMoraCuotaALaFecha(c, dia)` y `recalcularCuotaDesdePagos(c, dia)` se llaman con `min(corte, hoy)`:
   un corte futuro (2026-11-30 ejecutado el 25) ya no deja mora de cinco días que no corrieron. El corte sigue siendo el que filtra los pagos.
6. **Resultado por préstamo con rastro.** Devolver (y escribir en el log) por préstamo: PGPR anulados, PGPR nuevos y la rotura antes y después según el control,
   para poder cuadrar contra el JSON y volver atrás con exactitud.
7. **Asientos de provisión.** Hoy se generan dos asientos por préstamo (re-provisión y reverso). Propuesta: un asiento por corrida y tipo de préstamo con idOrigen = la
   corrida, o dejar el asiento para el cierre; es decisión contable, no del lote.

## Pruebas antes de volver a correrla
- Caso 2904 y 3492 sobre una copia del respaldo; resultado esperado: 2904 queda sin `SOBREIMPUTADA` y 3492 se detecta como rota.
- Barrido: el control del cierre sobre una copia después de re-imputar todo; el conjunto de roturas tiene que bajar, nunca subir ni cambiar de tipo.
- Un préstamo con `PGPR.PRSTCDGO` nulo debe verse igual en herramienta y control.

## Datos mal grabados (`sql/396`)
`PGPR.PRSTCDGO` nulo o distinto también esconde pagos a `PrestamoServiceImpl` (revertir desembolso, anular préstamo), al cuadre de cargas Petro de
`CierreCarteraDaoServiceImpl` y a las consultas de préstamos cancelados (`PrestamoCanceladoDaoServiceImpl`). El SQL 396 mide cuántos son y de dónde vienen.
Recomendación: corregir el dato con un UPDATE autorizado `PGPR.PRSTCDGO = DTPR.PRSTCDGO` (con respaldo) después de medir, aparte de la corrección de la herramienta.

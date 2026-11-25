# Regularización de asientos de condonación que quedaron vivos (lote 1c)

**Estado: PROPUESTA, sin ejecutar.** La aprueba el usuario antes de tocar nada en producción. Sin `UPDATE`.

## Qué pasó

Hasta el lote 1c, `AcuerdoCondonacionServiceImpl.generarAsientoCondonacion` no guardaba su asiento en el evento del
acuerdo (`EVPR.EVPRNMAS`). Al reversar el cobro de un acuerdo (o anular la operación de un acuerdo 100 % aportes) se
restauraban las cuotas y se anulaba el pago, pero el asiento D gasto por condonación / H capital e interés quedaba
**ACTIVO**. En el acuerdo 100 % aportes pasaba lo mismo con el asiento del cruce de aportes, y además el acuerdo quedaba
APLICADO aunque su evento estuviera anulado.

Desde el lote 1c el asiento se guarda en el evento y el reverso lo anula por la vía de siempre. **Lo que ya existía antes
no se corrige solo.**

## Qué hay que regularizar

Lo mide `sql/387_MEDICION_ASIENTOS_CONDONACION_VIVOS.sql` (solo lectura):

| Bloque | Qué lista | Qué hacer |
|---|---|---|
| 1 | Asientos de condonación activos de acuerdos que ya no están APLICADOS o con el evento anulado | Anular cada asiento (abajo) |
| 2 | Acuerdos con más de un asiento de condonación activo (reabierto y vuelto a aplicar) | Anular el **más viejo** de cada acuerdo |
| 3 | Asientos de cruce de aportes activos del acuerdo 100 % aportes reversado | Anular cada asiento (abajo) |
| 4 | Totales del bloque 1 por mes | Para calcular el efecto por período |
| 5 | Acuerdos 100 % aportes con el evento anulado que siguen APLICADOS | Reabrirlos (ver «El acuerdo 100 % aportes») |

Limitación: el asiento no está ligado al acuerdo por una columna; los bloques lo reconocen por su observación
(«Condonación del acuerdo N - préstamo M», «Cruce de aportes - acuerdo N»). Un asiento editado a mano no aparece.

## Cómo anular cada asiento (reversión estándar, sin UPDATE)

`POST /rest/asnt/anular/{idAsiento}?usuario=...&motivo=...&fechaReverso=AAAA-MM-DD`
(`AsientoRest` → `asientoService.anulaAsiento(id, usuario, motivo, fecha)`):

- **Período contable abierto:** se anula el asiento original.
- **Período cerrado o mayorizado:** el original no se toca; se genera un asiento de reverso en `fechaReverso` (que tiene que
  caer en un período abierto) y el original queda REVERSADO, con su rastro. Los dos quedan enlazados por `ASNTASRV`.
- El `motivo` debe nombrar el lote: p. ej. «Regularización lote 1c: asiento de condonación del acuerdo N que quedó vivo al
  reversar el cobro».

## Fecha del reverso: la decide el contador

Opciones:

1. **La fecha del reverso del cobro** (el día en que se anuló el cobro). Deja el efecto contable en el mismo período en que
   el cobro dejó de valer. Solo sirve si ese período sigue abierto.
2. **El primer día del mes abierto más antiguo.** Concentra el ajuste en un solo período y evita tocar períodos cerrados.

Para el **bloque 2 (duplicados)**, la misma regla de fecha; se anula el más viejo de cada acuerdo.

## Orden sugerido

1. El usuario aprueba este procedimiento y la fecha.
2. Correr el 387 y guardar la salida de los bloques 1, 2, 3 y 5 como respaldo.
3. Anular los asientos de los bloques 1 y 3, y el más viejo de cada acuerdo del bloque 2, uno por uno (no hay endpoint de lote).
4. Correr el 387 otra vez: los bloques 1, 2 y 3 deben salir vacíos.
5. Verificar contra la cuenta de gasto por condonación y las cuentas de cartera afectadas que el saldo bajó por el total del bloque 4.

## El acuerdo 100 % aportes (bloque 5)

Desde el lote 1c, anular la operación de un acuerdo sin cobro lo vuelve a VIGENTE (`reabrirAcuerdoPorReverso`) en la misma
transacción. Los acuerdos del bloque 5 ya quedaron APLICADOS con evento anulado: hay que reabrirlos por la misma vía de
negocio que el reverso del cobro (no con `UPDATE`); el equipo de desarrollo propone el mecanismo cuando el usuario apruebe
la lista.

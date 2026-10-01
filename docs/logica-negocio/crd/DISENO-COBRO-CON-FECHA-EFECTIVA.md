# DISEÑO — Cobro registrado tarde: aplicar a la fecha efectiva de pago y reversar lo que se contabilizó de más

**Equipo:** `omen-saa-1` (CRD · equipo B) · **Abierto:** 2026-09-30 · **Estado:** ⛔ DIAGNÓSTICO, NO DESPACHADO.
Faltan las decisiones del §5.

⚠️ **Alcance compartido.** Esto toca el motor de pagos (`MotorPagoPrestamoServiceImpl`,
`ProcesoPagoPrestamoServiceImpl`), el circuito CBCR (`CobroCreditoServiceImpl`) y el cierre de cartera.
Los tres los usa **todo** cobro de crédito. En el registro (§4), `CobroCreditoServiceImpl` y
`ProcesoPagoPrestamoServiceImpl` figuraban como del equipo A, que hoy no está activo. Cualquier aviso lo
autoriza el usuario.

---

## 1. El requerimiento, como lo dio el usuario (2026-09-30)

> Los partícipes depositan y no lo notifican, o pagan a las 9 de la noche y lo notifican (o se hace
> efectivo en el banco) al día siguiente, a veces hasta **3 meses después**. Mientras el pago no se
> procesa, el sistema sigue generando mora, **y eso está bien** (el pago puede rebotar). Pero al
> registrar el pago en crédito:
> 1. Se pide la **fecha de pago**, que puede ser muy anterior a hoy. Hay **dos fechas**: la **efectiva
>    de pago** y la de **registro** en el sistema.
> 2. **Toda la contabilidad sale en el mes de registro**, no en el de pago.
> 3. El sistema **elimina la mora** generada desde la fecha de pago, porque el pago estuvo dentro del
>    vencimiento.
> 4. Si esa mora, o el pase de esos valores a vencido o entre bandas, **ya se contabilizó**, en el mes de
>    registro se **REVERSA**: siempre reversar, nunca anular. Y sólo por los valores entre la fecha de
>    pago y la contabilización.
> 5. Caso de hoy: 11 pagos registrados el 30/09 que contabilidad aprueba el 01/10.

---

## 2. Lo que hay hoy — medido contra el código (HEAD `3a083249`, verificado por el árbitro)

### 2.1 La fecha efectiva YA existe y YA viaja
- `CobroCredito.fecha` está documentada como *«la del depósito, NO la de captura»* y es obligatoria. La de
  registro es `fechaRegistro`.
- `procesarCobro` la pasa a todos los motores: la fecha del `EventoPrestamo`, `PagoPrestamo.fecha`
  (PGPRFCHA), `cuota.fechaPagado`, `Aporte.fechaTransaccion`, `PagoAporte.fechaContable` y `DSBN`.

### 2.2 ⛔ La contabilidad sale en el mes de PAGO, no en el de registro (contrario al punto 2)
- **Los tres asientos del cobro se fechan con `cobro.getFecha()`**: transitorio
  `CobroCreditoServiceImpl:1672`, reparto `:1932` y definitivo `:2112` (`fechaCorte = cobro.getFecha()`,
  `:1966`).
- `AsientoServiceImpl.saveSingle` asigna el período por la fecha del asiento. Un cobro de hace 3 meses
  **se contabiliza en un período pasado**: si está CERRADO, falla; si está MAYORIZADO, por lo leído no
  parece bloquearse (`:741` queda en una rama que no se alcanza). **No probado.**
- El asiento definitivo clasifica la banda del capital pagado con `tipoCarteraYDias(vencimiento,
  cobro.fecha)`: lo saca de la banda que correspondía en la fecha de pago, **no de la banda donde lo
  dejó el cierre de cartera**.

### 2.3 ⛔ H42 sigue vivo: la mora NO se recalcula a la fecha del pago
- `aplicarPagoACuota` → `calcularSaldosRealesCuota` → `calcularSaldosCuota` lee la mora **persistida**
  (`DTPRMRAA`, `MotorPagoPrestamoServiceImpl:123`), calculada por la corrida de las 02:00 a la fecha de
  **esa** noche. No recibe la fecha del pago.
- `calcularTotalPendientePrestamo` (la validación «el pago no supera la deuda») también usa la mora
  persistida.
- `recalcularMoraALaFecha` / `calcularMoraCuota(cuota, tasa, fecha)` (una función pura) existen, pero
  **sólo las usan la precancelación (en la SIMULACIÓN) y la condonación**.
- **La precancelación tiene su propio desfase:** simula con la mora a la fecha (`:844`), pero en el bucle
  real paga la persistida (`:1067-1073`). Inferido del código, no probado.
- **Al pagar, el motor nunca reescribe** `mora`/`moraCalculada`/`diasMora`/`total` de la cuota. Sólo
  acumula los `*Pagado`.
- **Consecuencia:** un pago registrado tarde **cobra la mora hasta la última noche**. La prelación cobra
  mora antes que interés y capital: si el depósito alcanzaba justo para la cuota a la fecha de pago, la
  cuota queda **PARCIAL** y el excedente de mora se lleva capital. Si alcanzaba, queda PAGADA con la mora
  inflada **para siempre**, porque el proceso nocturno ya no toca cuotas PAGADAS.
- El proceso nocturno (`ProcesoMoraPrestamoTimer`, 02:00, **activo desde el 2026-09-09**) recalcula
  **desde el vencimiento** cada noche, y a una PARCIAL le calcula la mora sobre el **capital original
  completo**.

### 2.4 Qué se contabiliza de la mora y de las bandas
- **El proceso nocturno de mora no genera ningún asiento.** Sólo escribe en `DTPR`.
- **El cierre de cartera mensual** (`CierreCarteraServiceImpl`) es el que mueve bandas y vencidos:
  - ① vencidos;
  - ② y ①.1 cambio de bandas;
  - ③ apertura y ⑥ neteo, que suman capital + interés + **mora persistida** + seguros de lo pendiente;
  - ④ devengo, que en la práctica no devenga mora de cuotas vencidas: sólo las que vencen en el mes que
    se abre.
- ⛔ **El cierre clasifica con el estado de HOY, no con el de la fecha de corte** (`selectCapitalPorProductoYVencimiento`,
  `CierreCarteraDaoServiceImpl:69-87`): toma las cuotas no PAGADAS y Σ **todos** los pagos, **sin**
  `PGPRFCHA <= corte`. Sólo la antigüedad usa la fecha de corte. Es el mismo defecto que H53 en el CCPM.
- ⛔ **El cierre NO guarda detalle por préstamo ni por cuota.** BDCC es agregado por
  producto/tipo/banda, ANCC es un asiento por subproceso, y DSBN no lo escribe el cierre. ⇒ Lo que el
  cierre contabilizó para UN préstamo **no se puede leer, hay que recalcularlo**. Es determinístico
  (vencimiento, capital y las tres fechas de la corrida), pero hay que recalcularlo.

### 2.5 Reversos
- `AsientoServiceImpl.reversionAsiento(id, fecha)` hace un **espejo COMPLETO**: un asiento nuevo con
  debe/haber invertidos, en el período de `fecha`, y el original marcado REVERSADO. **No existe reverso
  parcial ni por líneas.**
- **CRD usa `anulaAsiento`**: ANULA si el período no está mayorizado, y sólo reversa si lo está. Lo usan
  `anularCobro`, `reversarProceso` del CBCR y el «reversar corrida» del cierre de cartera. ⇒ Hoy CRD
  **anula** casi siempre. El usuario pide **siempre reversar**.
- Contradicción comentario/código: `reversarProceso:666-669` dice que el transitorio rehecho lleva la
  fecha de HOY, y el código usa `cobro.getFecha()`.

---

## 3. Lo que habría que cambiar (hechos, todavía no es el diseño)

| # | Pieza | Hoy | Lo pedido |
|---|---|---|---|
| A | Mora al aplicar el pago | la persistida (H42) | **calculada a la fecha efectiva** (`calcularMoraCuota`), en el motor y en la validación de deuda |
| B | La cuota después del pago | `mora`/`total` inflados para siempre | `mora`, `moraCalculada`, `diasMora` y `total` **reescritos** a la fecha efectiva |
| C | Fecha de los asientos del cobro | `cobro.fecha` (mes de pago) | **mes de registro/proceso**. Hay que decidir cuál, §5 |
| D | Banda del capital pagado en el definitivo | la que tocaba a la fecha de pago | **la banda donde lo dejó el último cierre**. Si no, la cuenta de vencido nunca se descarga |
| E | Reverso de lo que el cierre ya contabilizó | no existe | asiento de **reverso** en el mes de registro, por préstamo: vencido → por vencer, y bandas, sólo del tramo [fecha de pago, último corte] |
| F | Reversos en CRD | anular | **reversar** (`reversionAsiento`), siempre |

**Lo delicado es D + E juntos.** Si el cierre de agosto movió una cuota a vencido 1-30 y la pagaron el
31/08, al registrar el pago en septiembre hay que: (i) reversar ese pase a vencido en septiembre y (ii)
cobrar el capital desde por vencer. O lo que es igual: cobrarla desde la banda donde está **y** no
reversar nada. **Las dos cosas a la vez la cuentan dos veces.** Hay que elegir una (§5, pregunta P4).

---

## 4. El caso urgente — los 11 pagos del 30/09 que se aprueban el 01/10

- Esta noche (01/10, 02:00) la corrida de mora le pone **1 día de mora** a toda cuota vencida el 30/09
  que siga impaga. Mañana el motor cobraría **esa mora** (H42).
- **Si el cierre de septiembre todavía NO corrió**, no hay asiento de bandas que reversar. Los tres
  asientos del cobro salen fechados el 30/09, en septiembre. Y como el cierre lee el estado de hoy, si se
  procesan ANTES de correr el cierre, esas cuotas ya figuran pagadas. ⇒ **Para estos 11 el único daño
  real es la mora de 1 día.**
- **Mitigación posible sin código**, si el usuario la elige: un `.sql` que, para los préstamos de los
  CBCR en estado 1 o 2 con `fecha` anterior a hoy, recalcule la mora de sus cuotas impagas **a la fecha
  del cobro** con la misma fórmula del proceso nocturno, y que corra **después de las 02:00 y antes de
  procesar**. Así el motor cobra la mora correcta. No cubre el caso general: es puntual.

---

## 4bis. Decisiones del usuario — 2026-09-30. NO re-litigar

| # | Pregunta | Decisión |
|---|---|---|
| C1 (P1) | ¿Corrió el cierre de septiembre? | **No, todavía no.** |
| C2 (P2) | Los 11 pagos de mañana | **Esperan al cambio completo.** No hay mitigación puntual. |
| C3 (P3) | Límite de antigüedad de la fecha efectiva | **Sin límite.** |
| C4 (P6) | Asiento de reverso | **Uno separado por cobro.** |
| C5 (P4) | ¿Reversar el pase a vencido/bandas del cierre, o cobrar desde la banda actual? | **Reversar**, dicho por el usuario en el requerimiento (§1, punto 4). ⇒ El definitivo cobra el capital desde la banda que tocaba **a la fecha de pago**, que es lo que **ya hace hoy** (§2.2), y un asiento SEPARADO en el mes de registro reversa lo que el cierre movió de esa cuota entre la fecha de pago y el último corte. |

⚠️ **Consecuencia operativa de C1 + C2, que hay que decirle al usuario:** si los 11 cobros esperan sin
procesarse y **el cierre de septiembre corre antes**, el cierre los va a ver impagos (lee el estado de
hoy) y los va a pasar a vencido. Eso no rompe nada: es justo el caso que este cambio reversa. Pero
**genera trabajo de reverso** que se evita procesándolos antes del cierre, una vez desplegado el
cambio. Si el cambio no llega antes del cierre, se procesan después y el reverso hace su trabajo.

## 5. ⛔ Decisiones que faltan

| # | Pregunta |
|---|---|
| P1 | ¿Ya corrió el **cierre de cartera de septiembre**? |
| P2 | Los 11 pagos de mañana: ¿mitigación puntual con `.sql` (§4) o esperan al cambio completo? |
| P3 | ¿**Límite de antigüedad** de la fecha efectiva? ¿O se acepta cualquiera anterior a hoy? |
| P4 | **D vs E:** ¿se reversa en el mes de registro el pase a vencido/bandas que hizo el cierre y se cobra desde por vencer, o se cobra desde la banda actual sin reversar? |
| P5 | **Fecha de los asientos del cobro:** ¿la de **registro** (el transitorio, al registrar) o la de **proceso** (reparto y definitivo, al procesar)? Si se registra el 30/09 y se procesa el 01/10, ¿septiembre u octubre? |
| P6 | ¿Un asiento de reverso **por cobro** o uno por lote? |
| P7 | **Anular → reversar en todo CRD** (cobros, cierre de cartera, operaciones): ¿también los que no son de este frente? |

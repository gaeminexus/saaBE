# Pago retroactivo de pensión a jubilados — diseño

**Fecha:** 2026-09-04 · **Equipo:** CRD / Equipo B (`eqB`, `omen-saa-1`)
**Estado:** diseño escrito, **dos decisiones pendientes del usuario** — no implementar hasta cerrarlas.

> **Pedido del usuario, 2026-09-04**, sobre el jubilado con préstamo al que no se le ha descontado
> desde hace meses:
>
> *«Debe tomar el pago de cada mes que no se le pagó e ir aplicando el pago como un pago de cuota,
> hasta llegar a la del día, o hasta que el préstamo quede como pagado, o hasta que ya no tenga
> saldo en su cuenta de pensión complementaria. […] Toma el valor de su pensión de enero y lo
> aplica como pago a cuota del préstamo, y supongamos que con su pensión paga 1 cuota y un poco
> más, entonces con su pensión de febrero se paga el parcial y se abona a la siguiente, y así
> sucesivamente. Y obviamente esto con su contabilidad correspondiente.»*

---

## 1. Lo que NO hay que construir

**La cascada entre cuotas ya está implementada y en producción.** Verificado el 2026-09-04:
`ProcesoPagoPrestamoServiceImpl.pagarConAportes:616` delega en
`motorPagoPrestamoService.aplicarPago(idPrestamo, valorTotal, ctx)`, y el motor es el que reparte
sobre las cuotas pendientes: paga la más antigua, y el excedente sigue a la siguiente.

O sea que *«paga 1 cuota y un poco más, y con la de febrero se paga el parcial y se abona a la
siguiente»* **no se programa: se obtiene llamando al motor una vez por mes con el valor de ese mes.**

Y la contabilidad también: `contabilizarPagoConAportes(resultado, movimientos, ctx)` sale de la
misma llamada (`:635`). No hay que escribir asientos nuevos.

**Reimplementar el reparto sería un cuarto motor de pagos** — el error que este módulo ya evitó dos
veces.

---

## 2. La forma correcta: un `PGPC` por mes, no un acumulado

⭐ **El modelo ya lo pedía y nadie lo había leído así.** `CRD.PGPC` tiene
`UNIQUE (ENTDCDGO, PGPCANNO, PGPCMESS)`. Generar los meses faltantes **uno por uno** —enero, febrero,
…— encaja exacto:

- Cada mes tiene su propia fila con su período real.
- La idempotencia sigue funcionando: volver a correr no duplica ninguno.
- `porPeriodo` muestra cada mes en su lugar.
- Cada mes lleva su propia fecha de hecho y su propio asiento.

**Un solo `PGPC` acumulado en agosto por ocho meses sería peor:** perdería a qué meses corresponde,
rompería la lectura por período, y contabilizaría en un solo asiento lo que son ocho hechos.

---

## 3. El algoritmo

Para cada jubilado, desde el mes siguiente al **ancla** hasta el mes en curso:

```
ancla = fecha del último movimiento NEGATIVO del aporte 23
        (si no hay ninguno: fecha del movimiento de JUBILACION, positivo)

para cada mes M desde ancla+1 hasta el mes en curso:
    si saldo(aporte 23) <= 0            -> CORTAR: "saldo agotado"
    si el prestamo no tiene cuotas pendientes -> CORTAR: "prestamo cancelado"

    valorMes = min(VPPC.valorPagar, saldo restante del aporte 23)
    fechaM   = min(ultimo dia de M, hoy)          <- regla del §6bis del contrato

    pagarConAportes(prestamo, valorMes, fechaM)   <- el motor reparte en cascada
    crear PGPC(entidad, anio(M), mes(M), fechaM)
```

### Las tres condiciones de corte, que son del usuario

| Corte | Cómo se detecta |
|---|---|
| Llegó al mes en curso | fin natural del bucle |
| El préstamo quedó pagado | `aplicarPago` lanza `SIN_CUOTAS_PENDIENTES` — hay que tratarlo como **corte normal, no como error** |
| Se acabó el saldo del aporte 23 | el saldo llega a cero. **Final normal de una pensión, no una falla** |

⛔ **Las tres son finales normales.** Ninguna puede salir como renglón `ERROR` junto a los defectos
reales: si salen así, el operador no puede distinguir «terminó bien» de «se rompió». Van con estado
propio en el detalle.

---

## 4. Por qué mes a mes y no en un solo pago

No es una preferencia de estilo. Un pago único de ocho mensualidades fechado hoy y ocho pagos
fechados en su mes **dan resultados distintos en el préstamo**: el motor calcula interés y mora
contra la fecha del pago. Aplicar la cuota de enero con fecha de enero es lo que corresponde al
hecho; aplicarla con fecha de septiembre le carga al jubilado ocho meses de mora que no le
corresponden.

Es la misma lección de **H21** (la fecha de la carga Petro), que ya costó mandar casi toda la
cartera a vencidos por fechar pagos en el mes equivocado.

---

## 5. ⛔ DECISIÓN PENDIENTE 1 — la fecha contable de los meses retroactivos

Por el §6bis del contrato, el `PGPC` de enero se fecha `min(31-ene, hoy)` = **31 de enero**, y su
asiento también.

**Eso postea asientos en meses contables ya cerrados.** Verificado: **el sistema NO lo impide** — no
existe ninguna validación de período cerrado en `cnt`. O sea que va a entrar sin protestar, y el
problema aparecería en contabilidad, no acá.

| Opción | Qué implica |
|---|---|
| **(a) Cada mes con su fecha** | Fiel al hecho. El interés del préstamo se calcula bien. **Mete asientos en meses cerrados** |
| **(b) Todos con la fecha del período en curso** | No toca meses cerrados. Pero el motor calcularía mora como si todo se pagara hoy, y **eso perjudica al jubilado** |
| **(c) Mixto** | El pago al préstamo con la fecha de su mes (para que el interés salga bien) y el asiento contable en el período abierto |

**No lo decide el árbitro.** Es una decisión contable y hay que consultarla con quien cierra los
períodos.

---

## 6. ⛔ DECISIÓN PENDIENTE 2 — ¿a quién se le aplica?

El usuario lo planteó sobre el caso *«jubilado sin certificado bancario y con préstamo»*. Pero la
regla del retroactivo mes a mes tiene sentido para **cualquier** jubilado con meses adeudados.

| Alcance | Consecuencia |
|---|---|
| **Solo sin certificado y con préstamo** | Literal a lo pedido. Deja sin retroactivo a un jubilado que sí tiene certificado y también está atrasado |
| **Todo jubilado con meses adeudados** | Coherente. Al que tiene certificado y no tiene deuda, el retroactivo le sale **al banco** — y eso es plata saliendo, no una compensación interna |

⚠️ **La diferencia no es menor:** con préstamo, el retroactivo se queda dentro de la asociación
(cancela deuda). Sin préstamo, **sale al banco**. Medirlo antes está en `sql/194`.

---

## 7. Qué NO se toca

- `MotorPagoPrestamoService` y `pagarConAportes`: están en producción y los usan otros procesos.
- El cierre de cartera, la carga Petro y la devolución de aportes.
- `cxp` / `tsr`.

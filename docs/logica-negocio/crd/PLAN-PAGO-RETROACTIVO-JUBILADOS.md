# Pago retroactivo de pensión a jubilados — diseño

**Fecha:** 2026-09-04 · **Equipo:** CRD / Equipo B (`eqB`, `omen-saa-1`)
**Estado:** diseño CERRADO — las dos decisiones se resolvieron el 2026-09-04 (§4bis). Listo para implementar.

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

    pagarConAportes(prestamo, valorMes, fechaCorrida)  <- el motor reparte en cascada
    crear PGPC(entidad, anio(M), mes(M), fechaCorrida)
```

⛔ **`fechaCorrida` es UNA sola para todo el bucle: la del mes de la corrida** (D1), no la del mes
`M`. Se calcula una vez, fuera del bucle, con la regla del §6bis del contrato —
`min(último día del mes de la corrida, hoy)` — y **no cambia entre iteraciones**.

**Lo que sí cambia por iteración es el período del `PGPC`** (`anio(M)`, `mes(M)`): eso es lo que
dice a qué mes corresponde cada pago, y es lo que hace que la `UNIQUE` funcione como idempotencia.

> Una versión anterior de este algoritmo decía `fechaM = min(último día de M, hoy)`. **Quedó
> obsoleta con D1** y se corrige acá para que nadie la implemente por leer solo el §3.

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

## 3bis. ⛔ D3 — Hasta las cuotas EXIGIBLES a la fecha de corrida, no más

> *«Que se aplique el saldo hasta que el préstamo quede con sus cuotas pagadas hasta la fecha de
> corrida, es decir agosto 2026; el resto que se lo deje en su cuenta de pensión complementaria.
> Lo que sí debemos asegurarnos es que se vayan cruzando los aportes cronológicamente: si el aporte
> cubre más de lo que se debe en una cuota, antes de usar valores de aporte del siguiente mes se
> debe cruzar el saldo del mes anterior, y luego sí usar saldo del siguiente mes.»*

### ⛔ El motor NO respeta esto solo. Hay que toparlo desde afuera

**Verificado el 2026-09-04**, y es el hallazgo que decide la implementación:
`MotorPagoPrestamoServiceImpl.buscarSiguienteCuotaConSaldo(idPrestamo)` (`:333`) **no recibe fecha
y no filtra por vencimiento**. Devuelve la siguiente cuota pendiente con saldo, **sea exigible o
futura**, y `aplicarPago` cascadea hasta agotar el dinero o quedarse sin cuotas.

**Si se le entrega el acumulado completo, el motor va a PRE-PAGAR cuotas futuras** — exactamente lo
que el usuario no quiere. No es un defecto del motor: es correcto para un abono voluntario, y este
caso no lo es.

### El tope, calculado antes de llamar

```
finCorrida = ultimo dia del mes de la corrida (2026-08-31)

deudaExigible = SUMA del saldo pendiente de las cuotas del prestamo
                cuya FECHA DE VENCIMIENTO <= finCorrida

valorAAplicar = min( pension del mes M ,
                     deudaExigible restante ,
                     saldo restante del aporte 23 )
```

`deudaExigible` se **descuenta en cada iteración** por lo que el motor haya aplicado. Cuando llega a
cero, **se corta el bucle**: el préstamo ya está al día a agosto y no se toca una cuota más.

⛔ **Lo que sobra NO se consume.** Ni se descuenta del aporte 23 ni se manda al banco: queda en la
cuenta de pensión complementaria del jubilado. *«El resto que se lo deje en su cuenta.»*

### La cronología, que sale sola si el bucle es secuencial

El requisito de *«antes de usar el aporte del siguiente mes, cruzar el saldo del mes anterior»* se
cumple **por construcción** si se llama al motor **una vez por mes, en orden**: cada llamada
cascadea hasta agotar el dinero de ESE mes antes de que exista la siguiente.

⛔ **Lo que lo rompería es sumar todos los meses y hacer una sola llamada.** Ahí se pierde de qué mes
salió cada peso, y con él la cronología que el usuario pide. **Un `pagarConAportes` por mes, en
orden ascendente. Nunca un acumulado.**

---

## 4bis. ✅ DECISIONES DEL USUARIO — 2026-09-04, cerradas

> **1.** *«Fecha de mes de corrida, en este caso agosto.»*
> **2.** *«A todos los que tengan préstamo. Y solo para los que tengan certificado bancario sale
> pago, de lo contrario no.»*

### D1 — Todas las fechas son las del mes de la corrida

Los `PGPC` retroactivos conservan **su propio período** (`PGPCANNO`/`PGPCMESS` = enero, febrero…),
que es lo que dice **a qué mes corresponde** cada uno. Pero la **fecha del hecho** —la del pago al
préstamo y la del asiento— es **la del mes de la corrida**: agosto.

Esto anula la opción (a) del §5: **no se postea nada en meses cerrados.**

⚠️ **La consecuencia, dicha en voz alta:** el motor calcula interés y mora **contra la fecha del
pago**. Con las ocho mensualidades fechadas en agosto, el motor las ve como ocho pagos del mismo
día, y **la mora de enero a agosto se le cobra al jubilado**. Si se hubieran fechado en su mes, esa
mora no existiría.

Es una decisión legítima —la deuda estuvo impaga— pero conviene saber que **el retraso lo causó el
sistema, no el jubilado**: el proceso nunca corrió. Si más adelante se decide condonar esa mora, es
un ajuste aparte y este documento deja constancia de dónde se originó.

### ⛔ D4 — AMPLIACIÓN DEL 2026-09-04: el retroactivo es para TODOS, con o sin préstamo

> *«Para tu ejemplo, se le debe pagar los 8 meses, no solo un mes.»*
> (Sobre un jubilado **sin préstamo**, con certificado y 8 meses adeudados.)

**Esto reemplaza la lectura literal de D2.** Cuando el usuario dijo *«a todos los que tengan
préstamo»* estaba contestando sobre **el cruce**, no sobre la acumulación. El retroactivo mes a mes
aplica a **todo jubilado con meses adeudados**.

| Caso | Qué pasa |
|---|---|
| Con préstamo | Cruce hasta la deuda exigible; el remanente al banco **si tiene certificado** |
| **Sin préstamo, con certificado** | **Todo el acumulado sale al banco** |
| Sin préstamo, sin certificado | `BLOQUEADO`: no hay cruce posible y no puede salir dinero |

**El tope sin préstamo** es `min(meses × pensión, saldo del aporte 23)`. No hay `deudaExigible` que
lo limite, así que **el saldo es el único techo** — la regla que el usuario ya había fijado.

**Las condiciones de corte sin préstamo** son dos, no tres: llegar al mes de la corrida, o agotar el
saldo. «Préstamo al día» no aplica.

#### ⚠️ Lo que esto cambia en plata, y hay que medirlo antes de correr

Con el alcance anterior, el retroactivo era mayormente **interno**: cancelaba deuda y casi no salía
dinero de la asociación. Con D4, **un jubilado sin préstamo y con meses adeudados cobra todo lo
atrasado en efectivo, de una vez.**

⛔ **El total que sale al banco puede multiplicarse.** `sql/194` es el que lo mide, y
`previsualizarCorrida` lo muestra separado en «Total a dinero» — que ahora es el número a mirar
antes de apretar Ejecutar.

---

### D2 — El certificado gobierna la SALIDA, no el cruce

| | Con certificado | Sin certificado |
|---|---|---|
| **Cruce contra el préstamo** | ✅ sí | ✅ **sí, igual** |
| **Remanente al banco** | ✅ sí | ⛔ **no sale pago** |

El certificado bancario valida **la cuenta de destino**. Si no hay salida de dinero al banco, no hay
cuenta que validar — por eso el cruce contra el préstamo procede sin él.

Esto **confirma y generaliza** la excepción que el agente de backend había propuesto para el caso
100 % cruzado: no era una excepción, era la regla.

#### ⚠️ Supuesto del árbitro sobre el remanente sin certificado

Si a un jubilado **sin** certificado la deuda le consume **solo parte** de la pensión del mes, el
remanente **no se toca**: no se descuenta de su aporte 23 y le queda a favor.

**Motivo:** no se le puede sacar dinero de la cuenta si no hay forma de entregárselo. Descontarlo y
no pagarlo sería quitarle saldo sin contrapartida.

**Si el usuario quiere lo contrario** —consumir la pensión completa y dejar el remanente como deuda
de la asociación hacia el jubilado— hay que decirlo, porque **no hay dónde registrar esa deuda**:
no existe una tabla de «pensión devengada y no pagada».

---

## 5. ~~DECISIÓN PENDIENTE 1~~ — RESUELTA por D1. Se conserva el análisis

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

## 6. ~~DECISIÓN PENDIENTE 2~~ — RESUELTA por D2: a todos los que tengan préstamo

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

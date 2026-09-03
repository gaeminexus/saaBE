# Pago mensual a jubilados — lo que falta para cerrarlo

**Fecha:** 2026-09-02 · **Equipo:** CRD / Equipo B · **Estado:** plan aprobado, pendiente de implementar

> **Pedido del usuario, 2026-09-02:** *«el pago a jubilados y la jubilación de partícipes. Con más
> urgencia el pago a jubilados. Ver que al procesar se crucen los préstamos con los valores
> indicados, se genere el valor de pago a tsr y se dé de baja del aporte pensión complementaria. Y
> que toda la contabilidad se alimente como se indicaba en el levantamiento.»*

---

## 1. No se diseña nada: el levantamiento ya lo tiene resuelto

Nada de este frente se decide acá. Todo está levantado y **cerrado**:

| Qué | Dónde |
|---|---|
| Decisiones J1–J7 del usuario | `LEVANTAMIENTO-TRES-FRENTES-2026-08-30.md` §4.b |
| Flujo completo de la jubilación | ídem, §«RESUELTO — el flujo completo» |
| Los asientos, con cuentas resueltas contra `CNT.PLNN` | `LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md` §3.1 |

**No volver a preguntar lo que ahí está contestado.** El levantamiento lo dice con esas palabras.

De lo levantado, lo que aplica al **pago mensual** (no a la jubilación) es:

- **J5:** las pensiones mensuales se descuentan de la **pensión complementaria** (tipo de aporte **23**).
- **§3.1**, pago mensual por jubilado, con el ejemplo de pensión 300 = **280 pensión + 20 seguro salud**,
  cada uno a su cuenta.

---

## 2. Qué está construido, medido contra el código

`PagoPensionComplementariaServiceImpl` son **603 líneas reales**, no un cascarón. De los cuatro
requisitos del usuario, **dos ya están y funcionan**:

| Requisito | Estado |
|---|---|
| Baja del aporte de pensión complementaria | ✅ `APRT` negativo con `tipoMovimiento = PAGO_PENSION (9)`, distinto de `JUBILACION (7)` a propósito |
| Salida hacia tesorería | ✅ `OrigenPagoExterno.CRD_PAGO_PENSION_COMPLEMENTARIA`, mismo circuito que la devolución de aportes, con `PGTRIDOR` llevando el `PGPC` de vuelta y un reconciliador asíncrono (`sincronizarPagos`) |

**Lo que falta es exactamente lo que el usuario pidió mirar.**

---

## 3. Falta 1 — El cruce contra préstamos no existe

En las 603 líneas **no hay una sola referencia** a `pagarConAportes` ni al motor de pagos. La pensión
del mes se paga íntegra al jubilado aunque tenga deuda vigente.

**Y el enganche ya estaba previsto en el modelo:** `VPPC.VPPCTNPR` (`tienePrestamo`) existe como
columna en la tabla de parametrización y **el proceso no lo lee nunca**. Alguien pensó el caso,
parametrizó el campo, y el proceso quedó sin usarlo. Se ve en el propio código: de `VPPC` sólo se leen
`getValorPagar()` y `getValorSeguro()` (`:246-247`).

### Cómo se resuelve — no se construye, se orquesta

`ProcesoPagoPrestamoServiceImpl.pagarConAportes` **ya está en producción**: es el cruce de valores. Y
desde hoy además cobra mora, porque pasa por `MotorPagoPrestamoService`. Reimplementar el cruce dentro
del pago de pensiones sería un tercer motor de pago — exactamente el error que la migración de la
fase 3 acaba de deshacer.

### Orden del proceso, por jubilado

1. Resolver el valor del mes desde `VPPC`: **pensión** (`VPPCVLRR`) y **seguro de salud** (`VPPCVLSR`).
2. **Si el jubilado tiene préstamo vigente** → cruzar contra la deuda con `pagarConAportes`, consumiendo
   del aporte tipo **23**.
3. **Sólo el remanente** sale como orden de pago hacia tesorería.
4. La baja del aporte (`PAGO_PENSION`) debe reflejar **el total consumido** — cruce + pago —, no sólo
   lo que salió al banco.

> ⚠️ **Supuesto declarado, decidible pendiente.** El plan asume que el cruce se aplica **cuando el
> jubilado tiene préstamo vigente**, que es lo que sugiere `VPPCTNPR` siendo una marca por jubilado.
> Si el usuario quiere que sea una decisión del operador **pago por pago**, cambia la pantalla y el
> contrato — avisar antes de que el frontend arranque, no después. Lo que **no** cambia en ningún
> escenario es el orden: primero la deuda, después el banco.

⛔ **El cruce puede consumir todo el valor del mes.** Entonces la orden de pago es **cero** y **no debe
generarse**: una orden de pago en cero es una orden que tesorería va a procesar y devolver. El pago del
mes existe igual —quedó registrado y contabilizado— pero sin salida de dinero.

---

## 4. Falta 2 — El asiento contable no se genera

Hoy: `pago.setNumeroAsiento(pagoProgramado.getAsiento() != null ? ... : null)` — **toma prestado** el
asiento de la orden de pago externa. El asiento de CRD nunca se crea. El propio modelo lo admite en
`PagoPensionComplementaria.java:123`: *«ítem 5 de jubilados, pendiente»*.

### El precedente que fija dónde va la frontera

`DevolucionAporteServiceImpl` —mismo circuito, ya desplegado— genera en **CRD** el asiento de
**reclasificación**, y deja que **CXP** genere el movimiento bancario con el suyo. La misma división
aplica acá:

| Asiento | Quién lo genera |
|---|---|
| **Devengo** de la pensión y del seguro de salud | **CRD** — es lo que falta |
| **Pago** contra banco | CXP/TSR, con la orden de pago. Ya funciona |

### El devengo, según §3.1 del levantamiento

| Concepto | D | H |
|---|---|---|
| Pensión | `2.1.02.25.01` cuenta del jubilado | `2.3.01.10.03` pensiones complementarias por pagar |
| Seguro de salud | `2.1.02.25.01` cuenta del jubilado | `2.3.90.90.06` seguro de salud por pagar |

**Cuando hay cruce contra préstamo, ese tramo NO va por estas cuentas:** lo contabiliza
`contabilizarPagoConAportes`, que es el asiento del cruce de valores y ya existe. El devengo cubre
únicamente lo que se devenga como pensión y seguro.

⚠️ **Lección de la devolución de aportes, y hay que repetirla acá:** el orden se invirtió a propósito
el 2026-08-31 — **primero la orden de pago, después el asiento**. Si el asiento va primero y la orden
falla, queda un «✅ Asiento contable generado» impreso en un log de una operación que no ocurrió. Y
si el desglose es parcial (unos tipos con producto de pago parametrizado y otros no), **el caso mezclado
tiene que fallar**: un asiento descuadrado es peor que no tener asiento.

---

## 5. ⛔ Falta 3 — La plantilla contable no existe. Prerrequisito duro

**No hay plantilla de pago mensual de pensiones.** La **29** es la de *jubilación* (el traslado a
pensión complementaria), no la del pago. El propio levantamiento contable lo tiene anotado como
pendiente en su §605:

> *«Crear plantillas faltantes … pensiones de jubilados (`21022501`, `23011003`, `23909006`)»*

Sin la plantilla, el asiento del §4 **no tiene de dónde salir**. Es la misma trampa que la línea de
mora del asiento de Petro: compila, pasa revisión, y revienta la primera vez que un usuario lo corre.

**El DDL va en `sql/173`, y se corre ANTES de desplegar el WAR.** Alterno libre: **35** (el 34 es
entrega de préstamo quirografario, creado el 2026-09-01).

Auxiliares **posicionales**, como en la 29 y la 34 — **no** del catálogo semántico `CrdLineaAsiento`:

| aux1 | Mov | Cuenta | |
|---|---|---|---|
| 1 | DEBE | `2.1.02.25.01` | cuenta del jubilado — tramo pensión |
| 2 | HABER | `2.3.01.10.03` | pensiones complementarias por pagar |
| 3 | DEBE | `2.1.02.25.01` | cuenta del jubilado — tramo seguro de salud |
| 4 | HABER | `2.3.90.90.06` | seguro de salud por pagar |

---

## 6. Qué NO se toca

- **CXP y TSR.** La orden de pago vive en CXP y ese módulo **no es de este equipo**. La parte CRD
  (generar la orden, leer su estado) ya está construida y esa sí se toca. Si aparece algo que exige
  cambiar CXP o TSR por dentro, **se reporta al árbitro y se detiene ahí** — no se toca.
- **`ProcesoPagoPrestamoServiceImpl.pagarConAportes`.** Se llama, no se modifica.
- **El reconciliador `sincronizarPagos`.** Ya funciona; sólo hay que verificar que el reverso también
  deshaga el cruce cuando lo hubo (ver §7).
- **La jubilación de partícipes.** Es el otro frente y arranca cuando éste cierre.

---

## 7. El reverso, que es donde esto se puede romper feo

`sincronizarPago` ya revierte el movimiento de aporte cuando CXP rechaza o reversa el pago. **Con el
cruce agregado, ese reverso deja de ser suficiente:** si la pensión del mes canceló cuotas de un
préstamo y después el pago se rechaza, hay que decidir qué pasa con esas cuotas.

**Criterio:** el cruce y el pago son **dos hechos distintos**. El cruce ya consumió aporte y liquidó
deuda; el rechazo de tesorería afecta **sólo al tramo que salía al banco**. Revertir cuotas ya pagadas
por un rechazo bancario sería peor que el problema.

**Pero hay que verificarlo contra la regla LIFO de anulación de eventos** (`ProcesoPagoPrestamoServiceImpl`:
no se puede anular un evento si hay operaciones posteriores vigentes sobre el préstamo). Si el reverso
intentara deshacer el cruce, chocaría con esa regla y fallaría de una forma difícil de leer.

---

## 8. Verificación antes de dar por entregado

1. `mvn -q compile`.
2. `sql/173` corrido y su bloque de control final mostrando las 4 líneas activas.
3. Un jubilado **sin** préstamo: orden de pago por el total, devengo de 2 asientos, `APRT` negativo
   por el total.
4. Un jubilado **con** préstamo cuya deuda es menor que la pensión: cuotas liquidadas, orden de pago
   por el remanente, `APRT` negativo por el **total consumido**.
5. Un jubilado **con** préstamo cuya deuda se lleva todo: **ninguna orden de pago generada**, y el
   `PGPC` igualmente registrado y contabilizado.
6. Saldo insuficiente del aporte 23: `ERR_SALDO_INSUFICIENTE`, sin efectos parciales.

---

## 9. Documentación obligatoria en el mismo cambio

- `docs/logica-negocio/crd/API-PAGO-PENSION-COMPLEMENTARIA.md` — el contrato, espejado a
  `saaFE/docs/crd/`.
- `LEVANTAMIENTO-TRES-FRENTES-2026-08-30.md` §1 — bajar el pago mensual de «lo que hay que construir»
  a construido, dejando la jubilación como lo que sigue abierto.

---

## ✅ VERIFICADO EL 2026-09-02 — el prerrequisito del §5 está cumplido

El usuario corrió `sql/173`. Control D.1, **4 filas**, y D.2 con el cuadre estructural exacto
(`LINEAS=4, MIN_AUX1=1, MAX_AUX1=4, DEBES=2, HABERES=2`):

| aux1 | Mov | Cuenta | Nombre en el plan |
|---|---|---|---|
| 1 | DEBE | `2.1.02.25.01` | CTA INDIVIDUAL DE PENSIONES COMPLEMENTARIAS |
| 2 | HABER | `2.3.01.10.03` | PENSIONES COMPLEMENTARIAS POR PAGAR |
| 3 | DEBE | `2.1.02.25.01` | CTA INDIVIDUAL DE PENSIONES COMPLEMENTARIAS (seguro de salud) |
| 4 | HABER | `2.3.90.90.06` | SEGURO POR PAGAR JUBILADOS |

Las tres cuentas existían y están activas, y los nombres del plan coinciden con lo que el
levantamiento contable §3.1 anticipaba. **La plantilla alterno 35 está lista: el asiento de devengo
del §4 ya tiene de dónde salir.**

# Plan — cerrar la contabilidad de TODOS los procesos de crédito

**Fecha:** 2026-08-31 · **Módulo:** CRD · Escrito por el árbitro `saabe-4b`
**Decisión del usuario (2026-08-31):** *"Debemos cerrar todo con asientos. Ningún proceso puede
quedar sin asientos."*

---

## 0. Por qué esto es un proyecto y no dos parches

Veníamos cerrando agujeros de a uno, según aparecían. El usuario lo convirtió en un objetivo
explícito: **ningún proceso sin asiento.** Eso cambia el método — hace falta un **inventario
cerrado**, no una lista de pendientes que crece cuando alguien tropieza con algo.

**Y hay una fecha límite implícita: el día que se encienda el flag** de contabilidad de CRD
(rubro 237, hoy en 0). Todo lo que quede fuera para entonces empieza a producir contabilidad
incompleta en silencio.

---

## 1. Inventario — verificado en código el 2026-08-31

### ✅ Ya contabilizan

| Proceso | Dónde |
|---|---|
| Cierre / apertura de cartera | `CierreCarteraServiceImpl` — plantillas 1, 17, 33 |
| Cobro Petro (transitorio, reparto, aplicación) | `CobroPetroContableServiceImpl` — plantillas 19, 20, 21 |
| Cobros por el circuito CBCR (`ASN1` + `ASN2`) | `CobroCreditoServiceImpl` |
| Condonación (lo condonado y lo pagado, con y sin depósito) | `AcuerdoCondonacionServiceImpl` |
| *(helper compartido de líneas)* | `ContabilizacionIndividualCreditoServiceImpl` |

### ❌ NO contabilizan — lo que hay que cerrar

| # | Proceso | Servicio | Uso real | Asiento levantado |
|---|---|---|---|---|
| **1** | **Cruce de valores / pago con aportes** | `ProcesoPagoPrestamoServiceImpl.pagarConAportes` | **diario** | §3.5 |
| **2** | **Devolución de aportes** | `DevolucionAporteServiceImpl` | frecuente, **sale dinero** | §3.7 + plantillas 27/28, cuenta `2.3.01.15.04` |
| **3** | Precancelación 100% aportes (endpoint directo) | `ProcesoPagoPrestamoServiceImpl.precancelar` | poco | §3.5 (es un cruce) |
| **4** | Re-bandeo tras abono a capital | `AbonoCapitalPrestamoServiceImpl` | con cada abono | §3.6 asiento 2 |
| **5** | Cobro en exceso → devolución al partícipe | carga Petro / novedades | ocasional | §3.7 opción ① |

**Los cinco tienen su asiento levantado con contabilidad.** No hay que diseñar ninguno: hay que
implementarlos.

### ⚠️ La causa común de 1, 3 y 4

`ContabilidadPrestamoNoOpImpl` — **los cinco hooks del motor de pagos devuelven `null`**, sin
condición, desde el 2026-08-14. `contabilizarPagoCuota`, `contabilizarPagoConAportes`,
`contabilizarAbonoCapital`, `contabilizarPrecancelacion`, `contabilizarReverso`.

**La costura está bien puesta y nunca se llenó.**

---

## 2. ⛔ La trampa que hace peligroso el camino obvio

El camino obvio es implementar `ContabilidadPrestamoServiceImpl` y llenar los cinco hooks. **No lo
hagas sin resolver esto primero:**

Después del cutover, **todo cobro con dinero entrante pasa por `CBCR`**, y `procesarCobro` llama
por dentro a `pagarCuota()` / `precancelar()` / `abonarCapital()` — **que a su vez llaman a los
hooks**. Si los hooks generan asiento, cada cobro procesado por CBCR va a producir **dos asientos
por la misma operación**.

**Y los dos van a cuadrar**, así que no habría ningún error. Se descubriría conciliando.

**La regla:** el hook y `CBCRASN2` son **alternativas, no complementos**. Quien implemente los
hooks tiene que **excluir las operaciones que vengan de `procesarCobro`**. Ya está anotado en el
javadoc de los dos lados.

**Por eso el orden de abajo empieza por lo que NO pasa por CBCR.**

---

## 3. Orden de trabajo

### Fase 1 — los dos cruces (no pasan por CBCR, no hay riesgo de duplicar)

**#1 cruce de valores** y **#3 precancelación 100% aportes**. Los dos son el asiento del §3.5:
**D cuentas de aporte del socio, diferenciadas por tipo → H bandas de capital, intereses y
seguros**.

**El método ya existe**: `ContabilizacionIndividualCreditoService.lineasCruceAportesConsumidos`,
escrito para `CBCRASN2` y **devolviendo líneas en vez de guardarlas**, justamente para poder
reusarse acá. **Es enchufar dos llamadas**, no escribir un asiento.

Empezar por el **#1**: es el que se usa a diario.

### Fase 2 — devolución de aportes (#2)

Sale dinero y hoy no queda registro contable. Asiento propio, ya decidido por el usuario el
2026-08-30. §3.7, plantillas **27** y **28**, cuenta `2.3.01.15.04`.

⚠️ Su reverso y su circuito de aprobación de tesorería **ya existen**: el asiento se cuelga de lo
que hay, no se rehace el flujo.

### Fase 3 — re-bandeo del abono (#4)

Diferido explícitamente al construir `CBCRASN2`, y anotado en su javadoc. Diferencias **netas por
banda**, no bruto contra bruto (§3.6, decisión C2).

**Su omisión es menos grave que las otras y conviene saber por qué:** el descuadre de bandas es
**transitorio** — lo corrige el cierre mensual. Las demás omisiones son permanentes.

### Fase 4 — cobro en exceso devuelto (#5)

§3.7 opción ①. Es hermano del excedente a aportes (opción ③) que ya está en construcción:
conviene hacerlo **después**, reusando lo que ese deje montado.

---

## 4. Reglas que aplican a las cuatro fases

1. **Ningún asiento se diseña: están todos levantados** en
   `LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md`, con las cuentas resueltas contra `CNT.PLNN`.
2. **Las cuentas salen de la plantilla 21** por su `aux1` semántico — es la única renumerada y
   probada. **Nunca escritas en el código.**
3. **Un solo lugar por cuenta.** Si dos procesos necesitan la misma línea, la resuelve el mismo
   helper. Se aplicó ya tres veces —transitoria, interés, empresa— y las tres veces evitó una
   divergencia futura.
4. **Gate de `contabilidadActiva()`** en todos. Apagado: el proceso corre igual, sin asiento, y lo
   informa.
5. **Si falta una línea en la plantilla, fallar con `IncomeException` clara.** Nunca un asiento
   incompleto.
6. **Verificar el cuadre contra el monto de la operación**, no solo que D=H. **Un asiento mal
   clasificado también cuadra** — es la lección de `CBCRASN2` §7.
7. **Cada asiento nuevo necesita su reverso.** Si el proceso se anula, el asiento se reversa.

---

## 5. Lo que hay que verificar antes de encender el flag

Cuando las cuatro fases estén, **antes de poner el rubro 237 en 1**:

1. **La cuenta transitoria queda en cero** por cada cobro: `ASN2` cierra exactamente lo que abrió
   `ASN1`.
2. **La línea de gasto de condonación existe** en la plantilla 25 (cuenta 9743, ya corrida).
3. **La plantilla 21 tiene todas las líneas** que necesitan los procesos nuevos, por empresa.
4. **Ningún proceso de la tabla §1 quedó fuera.**
5. **Encenderlo un día de baja actividad**, no un viernes: la primera corrida real con
   contabilidad activa es cuando aparecen los defectos que ninguna prueba encontró.

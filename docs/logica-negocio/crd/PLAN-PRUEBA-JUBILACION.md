# Plan de prueba — jubilación de partícipes

**Fecha:** 2026-09-03 · **Equipo:** CRD / Equipo B · **Estado:** listo para ejecutar

> **Por qué existe este documento.** El proceso está construido —backend (`POST /rest/aprt/procesarJubilacion`),
> frontend orquestando los tres pasos, y el defecto de elegibilidad por `PRSTSLTT` ya corregido— pero
> **nunca corrió con datos reales**. Es el único frente grande del módulo en esa situación, y a
> diferencia de la carga Petro no tiene una corrida diaria que lo ponga a prueba solo: se ejecuta
> partícipe por partícipe, y el primero que se procese mal es un partícipe real con plata real.

---

## 1. Qué se prueba, y qué NO

`procesarJubilacion` hace **tres cosas y solo tres**:

1. Traslada el remanente de **cesantía** (tipo 1) y **jubilación** (tipo 2) a **pensión
   complementaria** (tipo 23), como movimientos en `CRD.APRT`.
2. Genera el asiento de reclasificación con la **plantilla alterno 29**, usando **solo los aux1 1, 2 y 5**.
3. Cambia el estado del partícipe a **JUBILADO COMPLEMENTARIO**.

⛔ **NO orquesta el cruce contra préstamos ni la devolución en efectivo.** Esas son decisiones
previas y opcionales que la pantalla ejecuta ANTES, llamando por separado a
`POST /rest/prst/pagarConAportes` y `POST /rest/dvap/registrar`. Cada una genera **su propio
asiento por su propio proceso**.

**Esa separación es la fuente del riesgo contable más serio de este flujo**, y por eso el caso C7
existe: los aux1 **3 y 4** de la plantilla 29 (liquidación diferenciada de cesantía y jubilación)
están definidos en el catálogo pero **deliberadamente sin consumidor** — si este método también los
asentara, el cruce/la devolución y esta reclasificación contabilizarían **el mismo dinero dos veces**.

**Tampoco entran los rendimientos** (tipos 12 y 24). Decisión cerrada del usuario el 2026-08-31.

---

## 2. Antes de empezar

| # | Verificar | Cómo | Si falla |
|---|---|---|---|
| P1 | La plantilla alterno **29** existe para la empresa contable con la que se va a probar | `sql/188` bloque 1 | **Parar.** Sin ella todo el plan falla en C1 por la misma causa y no se prueba nada más |
| P2 | La plantilla 29 tiene las líneas **aux1 1, 2 y 5** | `sql/188` bloque 1 | Parar: falta una línea → `IncomeException` |
| P3 | Los tipos de aporte 1, 2 y **23** existen y están vigentes | `sql/188` bloque 2 | Parar |
| P4 | Hay partícipes candidatos en **ACTIVO** o **ACTIVO EN MORA** con saldos distintos | `sql/188` bloque 3 | Sin candidatos no hay prueba |
| P5 | **Respaldo de la base**, o un ambiente donde se pueda restaurar | — | ⛔ Ver §5 |

⛔ **Esto se prueba primero en un ambiente restaurable.** No hay reverso de jubilación: no existe
un `revertirJubilacion`. Deshacer un caso mal procesado significa borrar movimientos de `APRT`,
revertir el asiento y devolver el estado del partícipe **a mano**, y eso sobre un partícipe real es
peor que el problema que se quería probar.

---

## 3. Los casos, en orden de riesgo

Cada uno indica **qué preparar**, **qué correr** y **qué tiene que pasar**. El bloque de
verificación correspondiente está en `sql/188`.

### C1 — El caso normal: saldo en las dos cuentas

**Preparar:** un partícipe ACTIVO con saldo > 0 en cesantía **y** en jubilación.
**Correr:** `POST /rest/aprt/procesarJubilacion` con `{idEntidad, usuario, fecha, idEmpresa}`.

**Tiene que pasar:**

- **201** con `valorTotalTrasladado` = cesantía + jubilación.
- **Tres movimientos** en `CRD.APRT` con `tipoMovimiento = JUBILACION`: dos **negativos**
  (cesantía, jubilación) y uno **positivo** (tipo 23), todos por el mismo total.
- Saldo de cesantía y de jubilación **en cero**; el de pensión complementaria subió por el total.
- **Un asiento** con `numeroAsiento` no nulo, **tres líneas**: dos al DEBE (aux1 1 y 2) y una al
  HABER (aux1 5), y **debe = haber al centavo**.
- Estado del partícipe = **JUBILADO COMPLEMENTARIO**.

⛔ **Y lo que NO tiene que pasar:** ninguna línea con aux1 **3 o 4**. Si aparecen, el §1 se rompió
y hay doble contabilización con el cruce y la devolución.

### C2 — Saldo en una sola cuenta

**Preparar:** un partícipe con saldo en cesantía y **cero** en jubilación (o al revés).

**Tiene que pasar:** dos movimientos (uno negativo, uno positivo) y un asiento de **dos líneas** —
la del aux1 de la cuenta vacía **no se emite**. Sigue cuadrando.

**Por qué importa:** el código decide línea por línea con `> 0.01`. Emitir una línea en cero, o
peor, omitirla y no ajustar el haber, descuadra el asiento.

### C3 — ⛔ Sin saldo en ninguna: el caso silencioso

**Preparar:** un partícipe cuyo saldo ya se cruzó/devolvió por completo en los pasos previos —
cesantía y jubilación **en cero**.

**Tiene que pasar:**

- **201**, `valorTotalTrasladado` = 0, `numeroAsiento` = **null**.
- **Ningún** movimiento de `APRT`, **ningún** asiento.
- ⛔ **Pero el estado SÍ cambia a JUBILADO COMPLEMENTARIO.**

**Por qué es el más delicado:** `generarAsientoJubilacion` devuelve `null` cuando el total es
≤ 0,01, y eso es **correcto** (no hay nada que contabilizar porque ya se contabilizó por las otras
dos patas). Pero un `numeroAsiento` nulo se parece mucho a un fallo. **Si la pantalla lo muestra
como error, el operador va a reintentar** — y el reintento va a fallar por estado no elegible (C8),
dejándolo convencido de que el proceso se rompió cuando en realidad salió bien las dos veces.
**Verificar qué muestra la pantalla en este caso, no solo qué devuelve el backend.**

### C4 — Estado no elegible

**Preparar:** un partícipe ya JUBILADO, o CESANTE, o NUEVO.

**Tiene que pasar:** **422**, mensaje nombrando el estado actual, y **nada cambia**: sin
movimientos, sin asiento, sin cambio de estado.

### C5 — Fecha futura

**Tiene que pasar:** rechazo por fecha inválida, sin efectos.

### C6 — ⛔ Plantilla ausente: la prueba del rollback

**Preparar:** llamar con un `idEmpresa` **para el que la plantilla 29 no exista**.

**Tiene que pasar:** error indicando que falta la plantilla — **y, críticamente, que el traslado NO
haya quedado hecho.**

**Por qué es la prueba más importante del plan:** el traslado de `APRT` ocurre **antes** de generar
el asiento. Si la transacción no revierte, el partícipe queda con la plata movida a pensión
complementaria y **sin ningún asiento que lo respalde** — descuadre contable silencioso, del tipo
que aparece meses después. `IncomeException` es `@ApplicationException(rollback = true)`, así que
**debería** revertir. **Esto se verifica, no se asume.**

Verificación: después del error, los saldos de cesantía y jubilación tienen que estar **como
antes**, sin movimientos nuevos y con el estado sin tocar (`sql/188` bloque 4).

### C7 — El flujo completo de la pantalla, sin doble contabilización

**Preparar:** un partícipe con saldo **y** un préstamo vigente.
**Correr, en este orden, desde la pantalla:** cruce contra el préstamo → devolución en efectivo del
excedente → `procesarJubilacion`.

**Tiene que pasar:** **tres asientos distintos**, uno por paso, y la suma de lo contabilizado en los
tres = el saldo total que tenía el partícipe. **Ni un centavo dos veces.**

**Cómo se detecta el defecto que este caso busca:** si `procesarJubilacion` emitiera aux1 3/4, el
monto del cruce y de la devolución aparecería contabilizado también en el tercer asiento, y el total
daría **más** que el saldo original.

### C8 — Idempotencia: llamar dos veces

**Correr:** repetir C1 sobre el mismo partícipe.

**Tiene que pasar:** la segunda llamada **falla por estado no elegible** (ya está
JUBILADO COMPLEMENTARIO) y no duplica nada. Es la única defensa contra un doble click o un
reintento, y hay que confirmarla en la pantalla, no solo por API.

---

## 4. Orden de ejecución

**C4 → C5 → C6 → C3 → C2 → C1 → C8 → C7.**

Los que **no deben cambiar nada** van primero: si alguno deja efectos, se descubre con la base
todavía limpia. C7 va último porque es el más caro de preparar y el que más ensucia.

---

## 5. Si algo sale mal

⛔ **No hay `revertirJubilacion`.** Deshacer significa, a mano y en este orden:

1. Borrar los movimientos de `CRD.APRT` con `tipoMovimiento = JUBILACION` de ese partícipe.
2. Revertir el asiento por el proceso normal de contabilidad — **no borrarlo**.
3. Devolver `ENTD.ENTDIDST` al estado anterior.

**Anotar el estado anterior ANTES de correr cada caso** (`sql/188` bloque 3 lo lista). Si no se
anotó, no se sabe si el partícipe estaba en ACTIVO o en ACTIVO EN MORA, y son estados distintos.

---

## 6. Qué queda fuera de este plan

- **El asiento del pago mensual de la pensión** (devengo pensión/seguro, §3.1 del levantamiento).
  Es un proceso aparte, todavía sin encargo.
- **`PagoPensionComplementaria`** y la orden de pago en CXP que dispara — circuito propio.
- El **pago a jubilados** como proceso mensual, que es el frente que el usuario marcó como más
  urgente y que se prueba por separado.

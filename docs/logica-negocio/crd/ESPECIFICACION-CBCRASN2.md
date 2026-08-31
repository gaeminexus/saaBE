# Especificación — `CBCRASN2`, el asiento definitivo de los cobros

**Fecha:** 2026-08-30 · **Módulo:** CRD · Escrita por el árbitro `saabe-4b`
**Para:** el agente de backend que construya `CobroCreditoServiceImpl.procesarCobro`
**Estado:** especificación. **No construido.**

---

## 0. El problema que resuelve

`procesarCobro` **no genera el asiento definitivo.** El transitorio sí se genera al registrar.

**Consecuencia el día que se encienda el flag de contabilidad (rubro 237, hoy en 0): cada cobro
deja un asiento transitorio que nunca se cierra.** La cuenta transitoria acumula sin techo, y eso
no se nota al ocurrir — se nota meses después, cuando alguien concilia y encuentra un saldo que no
debería existir.

**Es un defecto de algo que ya está en producción**, no una función nueva.

---

## 1. La forma del par, y de dónde sale

`generarAsientoTransitorio` (`CobroCreditoServiceImpl:1108`) produce hoy, **al registrar**:

| | Cuenta | Valor |
|---|---|---|
| **D** | La cuenta contable del **banco** donde entró el dinero (`cobro.cuentaBancaria`) | valor del cobro |
| **H** | La **cuenta transitoria** — plantilla alterno **19**, línea `aux1 = 1` (`2.3.01.15.01`) | valor del cobro |

**Entonces `CBCRASN2`, al procesar, tiene que cerrarla desde el otro lado:**

| | Cuenta | Valor |
|---|---|---|
| **D** | **La MISMA cuenta transitoria**, resuelta por la misma plantilla y la misma línea | valor del cobro |
| **H** | Las cuentas por cobrar / pasivos que la operación efectivamente liquidó | desglose, §3 |

⚠️ **La cuenta transitoria del debe NO se elige: se resuelve igual que en `ASN1`.** Si se toma de
otro lado y difiere aunque sea una vez, la transitoria queda abierta por ese cobro y el saldo
sobrevive a cualquier conciliación posterior. **Extraé la resolución a un método compartido y
usalo en los dos asientos** — que no puedan divergir es más importante que que sean correctos hoy.

---

## 2. ⛔ Las tres trampas — cada una produce un asiento MAL en silencio

**No se puede copiar la clasificación de `CobroPetroContableServiceImpl.contabilizarAplicacion`.**
Estas son las razones, verificadas:

### 2.1 El abono a capital NO graba en `capitalPagado`

Graba en **`saldoOtros`** (`PGPRSLOT`). Una clasificación que lea `capitalPagado` lo contabilizaría
en **$0**, sin ningún error: el asiento cuadraría, la transitoria se cerraría, y el capital abonado
no aparecería en ninguna cuenta.

### 2.2 La precancelación tampoco, para el capital futuro

Mismo campo, mismo riesgo. El **capital futuro** de una precancelación —la parte de la deuda que
aún no vencía— se acumula en `saldoOtros` de la cuota ancla, con `capitalPagado = 0`. Es la parte
más grande de una precancelación típica.

### 2.3 Solo reconoce aportes de tipo 9 y 11

Jubilación y cesantía. **Cualquier otro tipo contabilizable queda fuera del asiento.** Hoy la
plantilla 21 tiene cuenta para el 9 (`aux1=51`), el 11 (`aux1=50`) y el **2, aporte adicional**
(`aux1=52`) — y `validar()` ya rechaza en el registro los tipos sin cuenta. **Cubrí los tres**, y
resolvelos por el `aux1` de la plantilla, no por una lista de tipos escrita en el código: el día
que agreguen un tipo con cuenta, no debería haber que tocar Java.

---

## 3. El haber, por tipo de operación

Los asientos están **levantados con contabilidad** en
`LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md`, con las cuentas ya resueltas contra `CNT.PLNN`.
**No los inventes: implementalos.**

| Tipo de operación | Haber | Referencia |
|---|---|---|
| `PAGO_CUOTA`, `PAGO_MULTIPLE` | Bandas de capital (`1.3.xx` por banda), interés ordinario, interés de mora, seguro de desgravamen, seguro de incendio | §3.4 |
| `ABONO_CAPITAL` | Bandas de capital, **leyendo `saldoOtros`** (§2.1). Más el **re-bandeo**, §4 | §3.6 |
| `PRECANCELACION` | Bandas + intereses + seguros de lo exigible, **más el capital futuro desde `saldoOtros`** (§2.2) | §3.6 / §3.8 |
| `REGISTRO_APORTE` | Cuentas de aporte por tipo: cesantía `2.1.01.05.01`, jubilación `2.1.02.05.01`, adicional — por `aux1` de la plantilla 21 (§2.3) | §3.4 |
| `COBRO_MIXTO` | Las dos cosas: las líneas de préstamo como `PAGO_MULTIPLE`, las de aporte como `REGISTRO_APORTE`, **en un solo asiento** | §3.4 |
| `ACUERDO_CONDONACION` | **Solo la parte PAGADA.** Ver §5 | — |

**Cada familia de producto usa su propio juego completo de cuentas** — bandas, intereses y seguros
de quirografario, prendario e hipotecario son distintas. Eso ya lo resuelve
`ClasificadorBandaService` con el modelo dinámico de bandas; **usalo, no escribas cuentas**.

**La mora comparte cuenta con el interés ordinario** (decisión **D3**), pero **la descripción de la
línea del asiento debe decir explícitamente si es mora o interés ordinario**. Sin eso, el detalle
del asiento no se puede leer.

---

## 4. El re-bandeo del abono a capital — segundo asiento

Un abono a capital **redistribuye el saldo restante entre bandas** (nueva tabla de amortización).
Eso es un asiento aparte del cobro, con **diferencias netas por banda** — no bruto contra bruto
(§3.6, y la decisión C2 del §9.1).

**Matiz del usuario, ya decidido:** durante el mes alcanza con mover los saldos de las bandas **de
ese préstamo**; el cierre/apertura de fin de mes es el que garantiza los saldos globales.

⚠️ **Si esto no cabe en esta entrega, decilo y difierelo explícitamente** — pero entonces
**anotalo en el javadoc igual que estaba anotado `CBCRASN2`**, que es lo que hizo que lo
encontráramos. Un pendiente escrito se resuelve; uno omitido se descubre conciliando.

---

## 5. Las operaciones con dos fuentes de dinero

`ACUERDO_CONDONACION` y `PRECANCELACION` mixta se cubren en parte con **depósito** y en parte
**consumiendo aportes del socio**. El `ASN1` solo cubrió el depósito — es lo único que entró al
banco.

**Por lo tanto `CBCRASN2` tiene dos mitades, en un solo asiento cuadrado:**

| | Debe | Haber |
|---|---|---|
| Parte del **depósito** | Cuenta transitoria | Cuentas por cobrar de esa parte |
| Parte de los **aportes consumidos** | Cuentas de aporte del socio (cesantía `2.1.01.05.01`, jubilación `2.1.02.05.01`, …) | Cuentas por cobrar de esa parte |

**La segunda mitad es exactamente el asiento del cruce de valores del §3.5** — donde los aportes
van al debe, diferenciados por tipo. No es un caso nuevo: es un asiento que ya estaba levantado.

⚠️ **El acuerdo de condonación YA tiene su propio asiento** (`generarAsientoCondonacion`), que
cubre **lo condonado**: D gasto de la plantilla 25 → H cuentas por cobrar dadas de baja. Es
**autónomo y no toca la transitoria**. `CBCRASN2` para ese tipo cubre **solo lo pagado**. Si
clasificara también lo condonado, estaría duplicando lo que ese asiento ya hace por otro lado.

⚠️ Y el caso **100% aportes** de esos dos tipos **no genera `CBCR`**, así que no pasa por acá. Su
asiento —el del cruce— hay que generarlo en el motor que los aplica. **Verificalo: si hoy no se
genera, es el mismo agujero que este documento resuelve, en otro lugar.**

---

## 6. Reglas transversales

- **Gate de `contabilidadActiva()`** (rubro 237). Apagado: se procesa igual, sin asiento, y se
  informa — mismo criterio que `registrarCobro` y `CierreCarteraServiceImpl`.
- **`idEmpresa` viaja por parámetro**, como en el cierre de cartera.
- **El asiento se guarda en `CBCRASN2`** del cobro, igual que el transitorio en `CBCRASN1`.
- **Si la plantilla no tiene alguna línea que hace falta, fallá con `IncomeException` clara** — no
  generes un asiento incompleto. Mismo criterio que la línea de gasto de condonación.
- **Al ANULAR un cobro procesado**, el `ASN2` tiene que reversarse. Verificá qué hace hoy
  `anularCobro` con el transitorio y seguí ese patrón.

---

## 7. Cómo verificar que quedó bien

No alcanza con que el asiento cuadre: **un asiento mal clasificado también cuadra.**

1. **La transitoria queda en cero** para el cobro: el debe del `ASN2` es exactamente el haber del
   `ASN1`. Consultá los dos asientos de un mismo cobro y restá.
2. **Un abono a capital no contabiliza $0.** Es la trampa 2.1, y es la que más probablemente pase
   inadvertida porque el asiento cuadra igual.
3. **Un cobro de aportes de tipo 2** (adicional) aparece en el asiento. Es la trampa 2.3.
4. **Una precancelación contabiliza el capital futuro.** Es la trampa 2.2.
5. **Un cobro mixto** deja las líneas de préstamo y las de aporte en cuentas distintas, y la suma
   del haber es el total del cobro.

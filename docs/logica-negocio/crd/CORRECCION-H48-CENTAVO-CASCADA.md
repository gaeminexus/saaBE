# Corrección definitiva de H48 — el centavo que la cascada abandona

**Fecha:** 2026-09-07 · **Equipo:** `omen-saa-1` (`omen1`) · **Estado:** diseñado, pendiente de implementar.

Verificado contra el código el 2026-09-07. Caso que lo destapó: cobro 54, préstamo 4524.

---

## 1. La causa raíz: `TOLERANCIA` significa dos cosas incompatibles

`MotorPagoPrestamoServiceImpl:54` declara una sola constante:

```java
private static final double TOLERANCIA = 0.01;
```

y la usa para **dos conceptos distintos que casualmente valen lo mismo**:

| Uso | Líneas | Qué significa | Veredicto |
|---|---|---|---|
| **Umbral de cuota saldada** | 180, 343, 369, 415, 426, 464, 517, 741 | «una cuota con saldo ≤ 1 ctvo se considera pagada; no vale la pena perseguir el residuo» | ✅ **Correcto, no se toca** |
| **Guarda del bucle de dinero** | **287**, **299** | «si queda ≤ 1 ctvo por aplicar, dejá de aplicar» | ⛔ **Falso.** Un centavo es plata real y tiene dueño |

**El defecto no es el valor 0,01 — es que las dos ideas comparten la constante.** Cualquier
"arreglo" que cambie el valor rompe el primer uso mientras intenta arreglar el segundo.

### Cómo se manifiesta

```
while (valorRestante > TOLERANCIA && iteraciones < MAX_ITERACIONES)   // :287
```

Pagada la cuota #18 con 171,85 de un cobro de 171,86, queda `valorRestante = 0.01` y
`0.01 > 0.01` es **falso**. El bucle sale. **Había 43 cuotas pendientes por delante**: el centavo
tenía perfectamente adónde ir. Lo frena la comparación, no la falta de cuotas.

### ⛔ Y hay un SEGUNDO off-by-one que hace que el arreglo ingenuo empeore las cosas

```java
if (detalle.getTotalAplicado() <= TOLERANCIA) {   // :299
    // Blindaje: si una cuota no absorbe nada, no tiene sentido reintentar sobre ella
    break;
}
```

El comentario dice **«no absorbe nada»**, pero la condición dice **«absorbe un centavo o menos»**.
Y el `break` ocurre **después** de agregar el detalle a `cuotasAfectadas` y **antes** de
`valorRestante -= detalle.getTotalAplicado()`.

**Consecuencia si alguien solo cambia `:287` a `>=`:** el bucle entra con 0,01, `aplicarPagoACuota`
**graba el `PagoPrestamo` del centavo**, el blindaje corta sin restar, y el resultado informa
`excedenteNoAplicado = 0,01` **mientras el centavo YA se aplicó**. Los `PGPR` sumarían 171,86 y el
`ResultadoAplicacionPago` diría 171,85. Peor que ahora, porque el número que se reporta deja de
coincidir con el que se grabó.

> ⭐ **Los dos se arreglan juntos o no se arregla ninguno.**

---

## 2. La corrección

### 2.1 Separar las dos constantes (`MotorPagoPrestamoServiceImpl`)

```java
/** Una cuota con saldo pendiente menor o igual a esto se considera saldada. */
private static final double TOLERANCIA_CUOTA = 0.01;

/**
 * Medio centavo. Frontera entre "no queda plata" y "queda plata": todos los importes pasan
 * por redondear() a 2 decimales, así que un residuo real es 0,00 o >= 0,01 y nunca algo
 * intermedio. Se usa medio centavo en vez de comparar contra 0,01 para que la comparación no
 * dependa de la representación binaria del double.
 */
private static final double MEDIO_CENTAVO = 0.005;
```

- **Las 8 líneas del umbral de cuota** (180, 343, 369, 415, 426, 464, 517, 741) pasan a
  `TOLERANCIA_CUOTA`. **Su comportamiento no cambia en nada** — es un renombre.
- **`:287`** → `while (valorRestante > MEDIO_CENTAVO && iteraciones < MAX_ITERACIONES)`
- **`:299`** → `if (detalle.getTotalAplicado() <= MEDIO_CENTAVO)` y **corregir el comentario**:
  ahora la condición sí dice lo que el comentario siempre quiso decir, «no absorbió nada».

**Por qué esto no cicla:** `buscarSiguienteCuotaConSaldo` solo devuelve cuotas con
`totalPendiente > TOLERANCIA_CUOTA`, así que una cuota que no puede absorber nada nunca se
devuelve dos veces; y si aun así una devolviera 0 aplicado, el blindaje de `:299` sigue cortando.
`MAX_ITERACIONES` queda como tercera red.

### 2.2 El invariante que el asiento asume, hecho explícito (`CobroCreditoServiceImpl`)

Aunque la cascada deje de abandonar plata, **queda un caso legítimo de excedente**: que el préstamo
ya no tenga ninguna cuota con saldo (`buscarSiguienteCuotaConSaldo` devuelve `null`). Ahí sobra
dinero de verdad y no hay dónde ponerlo.

Hoy ese caso produce **exactamente el mismo error contable opaco**: el asiento definitivo arma el
DEBE desde `CRD.DCBC` y el HABER desde los `PGPR`, y descuadra por el excedente.

**Corrección:** en `procesarCobro`, **justo después del bloque de tipos de operación y ANTES del
`if (configuracionContabilidadService.contabilidadActiva())` que genera los asientos 2 y 3**
(hoy alrededor de la línea 1014, después de `cobro.setFechaProceso(...)`), verificar el invariante:

- **Cobrado:** suma de `DCBC.valor` de las líneas **con préstamo** (`getPrestamo() != null`).
- **Aplicado:** suma de los `PagoPrestamo` **vigentes** de los `EventoPrestamo` enlazados a esas
  mismas líneas (`pagoPrestamoDaoService.selectByEvento`, que `procesarCobro` ya usa para las bandas).
- Si `|cobrado − aplicado| > MEDIO_CENTAVO` → `IncomeException` con un mensaje **operativo**:

> `El cobro {id} registra $X sobre préstamos, pero solo se pudieron aplicar $Y: sobran $Z que el préstamo no puede absorber (ya está cancelado, o el valor excede la deuda). Corrija el valor del cobro y vuelva a procesarlo.`

**Solo se comparan las líneas de préstamo.** Las de aporte no pasan por la cascada y no tienen este
problema; meterlas en la cuenta introduciría falsos positivos.

La `IncomeException` es `@ApplicationException(rollback = true)`: **la transacción entera se
revierte y no queda nada a medias** — mismo comportamiento que hoy, pero con un mensaje que el
operador puede accionar en vez de un volcado de débitos y créditos.

---

## 3. Qué NO se hace, y por qué

- **No se toca `AsientoContableServiceImpl:529`.** Su validación debe-vs-haber es la última red y
  está bien: el problema era que se llegaba hasta ahí, no que exista. Además vive en `cnt`.
- **No se decide dónde va un excedente legítimo.** Si un socio deposita de más sobre un préstamo
  cancelado, ese dinero necesita un destino contable (saldo a favor, aporte, devolución) y **eso es
  una decisión de negocio, no del motor.** Por ahora se rechaza el proceso con un mensaje claro.
- **No se cambia la prelación** ni ningún otro comportamiento del motor.

---

## 4. ⚠️ Radio de impacto — leer antes de tocar

`MotorPagoPrestamoServiceImpl` **no lo usa solo el cobro individual**. Lo usan la carga Petro, el
pago de jubilados, la precancelación, el abono a capital y los pagos manuales.

**El cambio de `:287` altera el comportamiento de todos ellos en un solo escenario:** cuando quedaba
exactamente un residuo de 0,01 y había cuotas con saldo por delante. Antes ese centavo se descartaba
en silencio; ahora se aplica. **Es lo correcto y es lo que se busca**, pero significa que un proceso
que antes dejaba un centavo sin aplicar ahora lo aplica — los totales pueden moverse en centavos
respecto de corridas anteriores.

Precedente que confirma que esto importa: **H19 — el proceso de carga Petro descarta dinero en
silencio.** Ese frente ya estaba anotado como el hallazgo más grave de su día. Esta corrección va en
la dirección contraria a ese descarte, que es la correcta.

---

## 5. Verificación de aceptación

**Automática:** `mvn -q compile` exit 0. No hay tests en el proyecto.

**Manual, con datos reales, después de desplegar:**

1. **El caso que lo destapó.** Un cobro cuyo valor supere en 0,01 el `DTPRTTLL` de la primera cuota
   abierta, con más cuotas pendientes por delante → **debe procesarse sin error**, aplicando el
   centavo a la cuota siguiente. Los `PGPR` del evento deben sumar exactamente el valor del cobro.
2. **El excedente legítimo.** Un cobro por más de lo que debe un préstamo con una sola cuota abierta
   → **debe fallar con el mensaje operativo del §2.2**, no con el volcado contable, y no debe quedar
   ningún `PGPR`, asiento ni cambio de estado (rollback completo).
3. **No regresión del umbral de cuota.** Una cuota con saldo de 0,01 tiene que seguir tratándose
   como saldada: no debe aparecer en `buscarSiguienteCuotaConSaldo` ni recibir pagos.
4. **`ResultadoAplicacionPago` coherente:** en todos los casos, `valorAplicado` debe ser igual a la
   suma de los `PGPR` realmente grabados. Es el invariante que el segundo off-by-one rompía.

# El cobro cierra la apertura sólo por lo que estaba abierto

**Fecha:** 2026-09-04 · **Escrito por:** `lap-saa-1-arb` · **Pedido urgente del usuario.**

> ## ⛔ REEMPLAZA a `DISENO-APERTURA-EXTRAORDINARIA-CAPITAL-FUTURO.md`
>
> Ese documento describía la solución **inversa y equivocada**: abrir el capital futuro con un
> asiento extraordinario. Se implementó, se detectó que el requisito era el opuesto, y **se revirtió
> entero** (`0b665f2d`, 418 líneas). Queda en el repositorio como registro de por qué no se hace así.
>
> **Lo correcto no es abrir de más: es cerrar de menos.**

---

## 1. El problema, en una frase

**La apertura mensual abre lo que se espera cobrar en el mes. El asiento del cobro cierra esa cuenta
por el TOTAL del cobro — capital futuro incluido. En un abono a capital o una precancelación, cierra
más de lo que se abrió.**

## 2. Las cuentas reales — consultadas en producción el 2026-09-04

| Papel | `aux1` | Mov | Cuenta |
|---|---|---|---|
| Apertura · préstamos **por cobrar** | 2 | DEBE | `1.4.05.10` |
| Apertura · préstamos **por aplicar** | 4 | HABER | **`2.3.02.10`** |
| Transitoria del cobro (plantilla alterno 19) | 1 | HABER | `2.3.01.15.01` |

**`2.3.02.10` es "la cuenta de apertura" del enunciado del usuario.** Es distinta de la transitoria —
eso estaba en duda y quedó resuelto con datos, no por deducción.

## 3. El circuito hoy, y dónde se rompe

```
Apertura ③   D 1.4.05.10      →  H 2.3.02.10        lo esperado del mes
Asiento 1    ...              →  H 2.3.01.15.01     el dinero que entró (al registrar)
Asiento 2    D 2.3.01.15.01   →  H 2.3.02.10        reparto (al procesar)
Asiento 3    D 2.3.02.10      →  H bandas           definitivo, por el TOTAL del cobro
```

`generarAsientoReparto` y `generarAsientoDefinitivo` usan **los dos** el mismo
`totalesAportesPrestamos(detalles)`, que suma `detalle.getValor()` sin distinguir qué tramo del pago
era exigible y cuál era capital futuro.

## 4. La corrección — confirmada por el usuario el 2026-09-04

```
Asiento 2    D 2.3.01.15.01 (vencido+actual)  →  H 2.3.02.10 (vencido+actual)
Asiento 3    D 2.3.02.10    (vencido+actual)
           + D 2.3.01.15.01 (futuro)          →  H bandas (TOTAL)
```

**Por qué cierra:** la transitoria queda en cero igual que hoy —el asiento 2 se lleva una parte y el
3 la otra—; las bandas siguen recibiendo el total, porque el capital **sí se cobró**; y
**`2.3.02.10` sólo se mueve por lo que estaba abierto.**

⚠️ **Compatibilidad con una corrección previa que parece contradecir esto.** El comentario de
`generarAsientoDefinitivo` (2026-08-31) advierte que volver a debitar la transitoria en el asiento 3
la dejaba en `-cobro.getValor()` en vez de cero. **Eso pasaba porque el asiento 2 la cerraba
entera.** Con el tramo futuro fuera del asiento 2, deja de pasar. **No se está deshaciendo esa
corrección: se está quitando la causa que la hizo necesaria.**

## 5. El corte — decisión del usuario

**«Vencido + cuota actual» = las cuotas con `DTPRFCVN <= fechaCorteApertura`** de la corrida de
cierre viva. La «cuota actual» es **la que vence dentro del mes abierto**, no la próxima a vencer.

> ⭐ **Es exactamente lo que la apertura abrió**: `selectCobrablePrestamosHasta(fechaCorteApertura)`
> usa `TRUNC(d.DTPRFCVN) <= :hasta`. **Lo que cierra y lo que se abrió son la misma definición**, y
> por eso el neteo de fin de mes cuadra.

**Tramo futuro = capital de cuotas con `DTPRFCVN > fechaCorteApertura`.** Sólo capital: intereses,
mora y seguros no se prepagan.

**Da igual si se paga con dinero o con cruce de valores** (decisión del usuario). Lo que decide no es
cómo se paga, sino **qué** se paga.

### 5.1 Si no hay corrida de cierre viva

**No se parte nada: se comporta exactamente como hoy**, con el total en el asiento 2 y en el 3. Sin
período abierto no hay apertura que cerrar de más, y cambiar el comportamiento ahí introduciría un
riesgo sin resolver ningún problema. Traza y seguir; **nunca bloquear el cobro.**

## 6. Dónde se toca

| Archivo | Qué |
|---|---|
| `CobroCreditoServiceImpl.generarAsientoReparto` | Usar (total − futuro) en vez de total |
| `CobroCreditoServiceImpl.generarAsientoDefinitivo` | Usar (total − futuro) en el Debe «por aplicar», **más** una línea nueva de Debe a la transitoria por el futuro |
| `ContabilizacionIndividualCreditoService` | Método que calcula el tramo futuro del cobro |

**El cálculo del tramo futuro es el mismo que tenía el `capitalFuturoPosteriorACorte` revertido** —
la lógica era correcta, lo que estaba mal era qué se hacía con el resultado. Se puede recuperar de
`96a492b8`, con estas dos salvedades ya conocidas:

- Guard `saldoOtros > 0` para quedarse con el capital fuera del cronograma normal.
- ⛔ **`PAGO_APORTES` no tiene rama a propósito** — un cruce de valores solo no prepaga capital
  futuro. *(Una precancelación o un abono **pagados** con aportes sí, porque el tipo del evento es
  `PRECANCELACION`/`ABONO_CAPITAL`.)*

## 7. Verificación

1. **Pago de cuota normal**: nada cambia. Asiento 2 y 3 por el total; `2.3.02.10` igual que hoy.
2. **Abono a capital**: `2.3.02.10` se mueve sólo por el tramo hasta el corte; el futuro va D
   transitoria en el asiento 3. Transitoria en cero al final.
3. **Precancelación con cuotas vencidas + futuro**: las vencidas cierran la apertura, el futuro no.
4. **Abono cuyo capital cae íntegro dentro del mes abierto**: se comporta como un pago normal.
5. **Sin corrida viva**: como hoy, sin partir.
6. **Cierre del mes siguiente: el neteo cuadra.** Es la prueba que valida todo lo demás.

## 8. ⚠️ Hallazgo lateral, sin resolver

La consulta de plantillas devolvió **dos filas con alterno 1**: la de partícipes y una
`CIERRE PERIODOS` con cuenta `1.1.1.1.01 CAJA CHICA`. **`plantillaService.codigoByAlterno(1, idEmpresa)`
resuelve por alterno**: si las dos son de la misma empresa, la resolución es ambigua y podría tomar la
equivocada — un asiento contra caja chica sin que nada avise. **Confirmar con `PJRQCDGO` antes de
darlo por inofensivo.**

---

## 9. El camino DIRECTO — decisión del usuario, 2026-09-05

**Lo de arriba (§1 a §8) cubre `procesarCobro`. NO cubre el camino por el que el usuario precancela
de verdad.** Probado con dos logs de producción —eventos 481 y 444— que muestran
`✅ contabilizarPrecancelacion OK` y **ninguna** traza de `procesarCobro`.

### 9.1 Qué caminos son

| Método | Operación | Cierra la apertura hoy |
|---|---|---|
| `ContabilidadPrestamoServiceImpl.contabilizarPrecancelacion` | Precancelación directa | ❌ **nunca** |
| `ContabilidadPrestamoServiceImpl.contabilizarPagoConAportes` | Cruce de valores (incluye el de jubilados) | ❌ **nunca** |

Los dos arman `D cuentas de aporte consumido → H bandas`. **No hay depósito ni cuenta transitoria:**
la plata sale de los aportes del socio.

### 9.2 La solución — opción (A), confirmada por el usuario

**Dos líneas más EN EL MISMO ASIENTO**, que reversan la apertura por lo que sí estaba abierto:

```
D 2.3.02.10 (PRESTAMOS_POR_APLICAR)  →  H 1.4.05.10 (PRESTAMOS_POR_COBRAR)
```

**Cuadra por construcción:** agrega un Debe y un Haber por el mismo monto, así que el asiento sigue
cerrando sin tocar el movimiento de dinero.

⛔ **Los lados van INVERTIDOS respecto de la plantilla, y es deliberado.** La plantilla declara
`PRESTAMOS_POR_APLICAR` como **HABER** y `PRESTAMOS_POR_COBRAR` como **DEBE**, porque así es como
*abre* la apertura. Acá se está **deshaciendo** esa apertura, así que los dos lados se invierten por
definición. **Leer el `getMovimiento()` de cada línea, invertirlo explícitamente, y dejarlo dicho en
un comentario** — si no, el próximo lo lee como el defecto del Debe/Haber que ya cometimos una vez.

### 9.3 El monto — sólo lo vencido y la cuota actual

**`total pagado a préstamos en esta operación − capital futuro posterior al corte`.**

Es la misma resta del §4 y el mismo `capitalFuturoPosteriorACorte` que ya existe. **El tramo futuro
no entra: nunca se abrió, así que no hay nada que cerrar por él.**

- **Sin corrida de cierre viva** → no se agregan las dos líneas. Nada estaba abierto.
- **Monto cero** → no se agregan. Un par de líneas en cero es ruido.

### 9.4 ⚠️ Sólo el lado préstamos — supuesto declarado

La apertura también abre aportes (`1.4.05.05` / `2.3.02.05`), y **esas líneas NO se tocan.** Lo que
abre la apertura del lado aportes es **el aporte mensual que se espera cobrar**; una precancelación o
un cruce **consumen el saldo acumulado del socio**, que es otra cosa. Cerrar la apertura de aportes
por un consumo de saldo sería cerrar algo que nunca se abrió por ese concepto.

**Es un supuesto del árbitro, no una instrucción del usuario. Si está mal, cambia una línea.**

### 9.5 La descripción

Tiene que decir qué es, no sólo de dónde viene. Del estilo:
`"Cierre de apertura - vencido y cuota del mes - precancelación evento N"`.
El usuario lo pidió explícitamente: **«con la descripción correcta»**.

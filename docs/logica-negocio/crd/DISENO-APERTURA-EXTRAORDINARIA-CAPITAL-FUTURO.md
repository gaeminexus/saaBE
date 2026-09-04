# Apertura extraordinaria por capital futuro prepagado

**Fecha:** 2026-09-04 · **Escrito por:** `lap-saa-1-arb` · **Pedido urgente del usuario, 2026-09-04.**

> **Implementa:** por definir. `omen-saa-1` (`eqB`) es dueño de hecho de los tres archivos que
> toca; al 2026-09-04 **no dio el ok** para que otro equipo escriba en ellos, con dos razones
> concretas: el despliegue del pago a jubilados está en curso sin haberse corrido en producción, y
> el checkout compartido ya produjo dos choques ese mismo día. **La especificación se escribe igual
> porque no pisa nada y vale para quien la implemente.**

---

## 1. El problema, en una frase

**La apertura mensual abre lo que se espera cobrar en el mes. Un abono a capital o una
precancelación cobran capital que vence DESPUÉS de ese mes — capital que nunca se abrió. El asiento
del pago lo acredita contra una contrapartida que no existe.**

## 2. Cómo funciona hoy la apertura, verificado contra el código

`CierreCarteraServiceImpl.armaApertura` (`:783`), sub-proceso ③ del cierre:

| Línea | Catálogo `CrdLineaAsiento` | Monto |
|---|---|---|
| Aportes por cobrar | `APORTES_POR_COBRAR` (1) | esperado de aportes del mes |
| **Préstamos por cobrar** | **`PRESTAMOS_POR_COBRAR` (2)** | `selectCobrablePrestamosHasta(fechaCorteApertura)` |
| Aportes por aplicar | `APORTES_POR_APLICAR` (3) | ídem aportes |
| **Préstamos por aplicar** | **`PRESTAMOS_POR_APLICAR` (4)** | ídem préstamos |

Plantilla: `PlantillasCredito.APERTURA_PLANILLA_MENSUAL`. Las cuentas se resuelven con
`detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla, codigoLinea)` — el mismo
mecanismo que usa toda la contabilidad individual de crédito.

**El corte que define qué está abierto** (`CierreCarteraDaoServiceImpl.selectCobrablePrestamosHasta`):

```sql
AND TRUNC(d.DTPRFCVN) <= :hasta      -- cuotas que vencen hasta el corte del período abierto
```

netea lo ya pagado con `GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0)`.

> ⭐ **De acá sale toda la regla: lo que vence DESPUÉS de `fechaCorteApertura` no está abierto.**

## 3. Qué se genera, y cuánto

**Un asiento de apertura extraordinario, por el capital de cuotas con vencimiento POSTERIOR al
corte del período abierto que este pago está cancelando.**

**Sólo capital.** Ni intereses, ni mora, ni desgravamen, ni incendio: esos conceptos no se prepagan
— un abono a capital y el tramo futuro de una precancelación cancelan capital, y los intereses
futuros directamente dejan de devengarse.

### 3.0 Decisiones del usuario — 2026-09-04, NO re-preguntar

1. **Sólo el capital de cuotas con vencimiento POSTERIOR al corte del período abierto** — opción (A)
   del §3.1. No todo el capital prepagado.
2. **Aplica a los DOS casos: abono a capital y precancelación.**

La primera coincide con la lectura independiente del árbitro de `omen-saa-1`, que llegó a ella por
el mismo argumento del neteo. La segunda importa más de lo que parece: **una precancelación puede
llevar además cuotas ya vencidas, que sí estaban abiertas** — por eso el §3.2 exige el cruce por
fecha y no alcanza con `saldoOtros > 0`.

### 3.1 Por qué NO es "todo el capital prepagado"

**Decisión (A) del usuario, coincidida con el árbitro de `omen-saa-1` el 2026-09-04.** La parte del prepago que
cae en cuotas del mes en curso **ya la abrió la apertura mensual**. Abrirla de nuevo la contaría dos
veces y **el neteo de fin de mes no cerraría** — un descuadre que aparece un mes después, lejos de
la operación que lo causó.

### 3.2 Cómo se identifica el capital futuro — la pieza ya existe

`ContabilizacionIndividualCreditoServiceImpl.haberDesdePagos` documenta la regla:

> *«el abono a capital y el capital futuro de una precancelación graban en `saldoOtros` con
> `capitalPagado = 0` — leer solo `capitalPagado` los contabilizaría en $0, sin ningún error»*

Entonces, por cada `PagoPrestamo` vigente del evento:

1. `saldoOtros > 0` → es capital fuera del cronograma normal (candidato).
2. Cruzar contra la fecha de vencimiento de la cuota que cancela.
3. **Suma sólo lo que vence después de `fechaCorteApertura` del período abierto.**

Para el **abono a capital**, el reparto por cuota ya está resuelto: `lineasReclasificacionAbonoCapital`
calcula `repartirProporcionalPorCapital(historizadas, capitalAbono)` — `partes.get(idx)` es cuánto
del abono le tocó a cada cuota historizada, y cada una trae su `fechaVencimiento`. **Ese mismo
reparto es el insumo, no hay que recalcularlo.**

⛔ **Y por eso no alcanza con `saldoOtros > 0` a secas:** el mismo javadoc advierte que las cuotas
EXIGIBLES de una precancelación comparten `TIPO_PRECANCELACION` con el capital futuro. Sin el cruce
por fecha, una precancelación abriría también lo que ya estaba abierto.

## 4. Dónde va, exactamente

**En `CobroCreditoServiceImpl.procesarCobro`, inmediatamente ANTES de
`generarAsientoDefinitivo` (`:898`)**, dentro del mismo `if (contabilidadActiva())`.

```
procesarCobro
  ├── ... aplicación de la operación ...
  ├── if (contabilidadActiva()) {
  │      ├── ⭐ NUEVO: generarAsientoAperturaExtraordinaria(cobro, detalles)   ← acá
  │      └── generarAsientoDefinitivo(cobro, detalles)      // CBCRASN2, :898
  │   }
```

**El orden importa y no es cosmético:** el asiento del pago da de baja contra lo abierto. Si la
apertura extraordinaria fuera después, el asiento del pago daría de baja algo que todavía no
existía.

**Si el monto da cero** —el pago no tocó capital futuro— **no se genera asiento.** Un asiento en
cero es ruido en el mayor.

### 4.1 Qué período se consulta

El corte es el del **período abierto vigente**, no el del mes del cobro. Hay que leerlo de la
corrida de cierre vigente (`CorridaCierreCartera`), no recalcularlo: dos formas de derivar el mismo
corte es exactamente el patrón que este proyecto viene pagando caro toda la semana.

⛔ **Si no hay período abierto, el pago NO se bloquea.** Se registra el pago sin apertura
extraordinaria y se deja traza. Bloquear un cobro por un asiento de apertura sería peor que el
problema.

## 5. Plantilla

**Reusar `APERTURA_PLANILLA_MENSUAL` con las dos líneas de préstamos** — `PRESTAMOS_POR_COBRAR` (2)
y `PRESTAMOS_POR_APLICAR` (4) — para que el extraordinario y el mensual golpeen **las mismas
cuentas**. Es lo que permite que el neteo de fin de mes los vea como un solo saldo.

La alternativa —plantilla propia— separa los dos hechos en los reportes pero exige crearla, y
**romper la identidad de cuentas con el mensual sería el error**.

✅ **CONFIRMADO por el usuario el 2026-09-04: se usa la plantilla de apertura mensual.** No se crea
una propia. No re-preguntar.

La descripción de cada línea debe decir que es extraordinaria y por qué, p. ej.
`"Apertura extraordinaria - capital futuro prepagado - cobro 1234"`.

## 6. Segundo pedido: la observación no identifica a nadie

**Verificado el 2026-09-04:**

| Camino | Observación de hoy |
|---|---|
| `contabilizarPrecancelacion` (`ContabilidadPrestamoServiceImpl:415`) | `"Precancelación - evento N"` — **nada más** |
| CBCRASN2 (`observacionEnriquecida`) | Cédula ✅ · Nombre ✅ · **un solo préstamo** ⚠️ |
| `contabilizarPagoConAportes` (`:243`) | razón social ✅ |

**El defecto de `observacionEnriquecida`, y es el que el usuario está viendo:**

```java
for (DetalleCobroCredito detalle : lista) {
    if (detalle.getPrestamo() != null && detalle.getPrestamo().getIdAsoprep() != null) {
        obs.append(" | idAsoprep: ").append(detalle.getPrestamo().getIdAsoprep());
        break;                                  // ⛔ corta en el primero
    }
}
```

Dos fallas: **corta con `break`** —una precancelación o un cobro múltiple con varios préstamos
muestra uno solo— y **depende de `idAsoprep`**, que es `null` en todo préstamo que no venga de
Petro, en cuyo caso **no aparece ninguno**.

**Qué debe quedar:**
1. **Todos** los préstamos del cobro, no el primero.
2. El **código de préstamo** siempre, y el `idAsoprep` **además** cuando exista.
3. El mismo enriquecimiento en el camino directo de `contabilizarPrecancelacion`, que hoy no tiene
   ninguno.
4. Respetar el tope de 2000 caracteres de `ASNTOBSR` que `observacionEnriquecida` ya controla —
   con varios préstamos se llega mucho antes.

## 7. Verificación antes de dar por entregado

1. Abono a capital con período abierto: **dos asientos antes del de pago no; uno** — la apertura
   extraordinaria — y después CBCRASN2. La apertura por **sólo** el capital de cuotas posteriores al
   corte.
2. Abono cuyo capital cae **íntegro dentro del mes abierto**: **no se genera** apertura extraordinaria.
3. Precancelación con cuotas vencidas + capital futuro: la apertura extraordinaria lleva **sólo** el
   tramo futuro, nunca las vencidas.
4. Sin período abierto: el cobro se procesa igual, sin apertura extraordinaria, con traza.
5. Observación: nombre, cédula y **todos** los préstamos, en los dos caminos.
6. Cierre de mes posterior: **el neteo cuadra.** Es la prueba que de verdad valida el §3.1.

## 8. ⚠️ Aviso de coordinación — levantado por `omen-saa-1-arb`

**La apertura y el cierre de cartera son la contrapartida de lo que `eqB` está generando ahora
mismo** con el pago a jubilados: asientos de devengo y cruces contra préstamos, mes a mes
retroactivo. **Si el criterio de apertura cambia mientras se escriben cientos de asientos de pago,
el descuadre lo va a ver el usuario de este equipo, no el de ellos.**

**Regla operativa: avisar a `omen-saa-1-arb` antes de que esto corra en producción**, no después.

# Reglas de clasificación de cartera — para que los reportes cuadren con los mayores

**Fecha:** 2026-09-05 · **Escrito por:** `lap-saa-1-arb` · **Destinatario principal:** el equipo que
genera los reportes financieros de `crd` (hoy `omen-saa-1` / jubilados).

> ## Por qué existe este documento
>
> **Un reporte financiero que clasifica la cartera con su propio criterio no cuadra con el mayor
> contable, aunque los dos estén "bien".** Los asientos se generan con las reglas de abajo; si el
> reporte las reimplementa distinto —o replica una versión vieja— la diferencia aparece al conciliar,
> semanas después, y nadie sabe cuál de los dos está mal.
>
> **Pedido del usuario, 2026-09-05:** *«esos valores deben cuadrar exactamente con los mayores
> contables, por lo que debe utilizar el mismo algoritmo»*.
>
> Todas estas reglas se corrigieron entre el 2026-09-04 y el 2026-09-05. **Un reporte escrito antes
> de esa fecha usa los criterios viejos.**

---

## 1. ⭐ La regla que reemplaza a todas: no reimplementar, llamar

```java
long[] tipoYDias = contabilizacionIndividualCreditoService.tipoCarteraYDias(fechaVencimiento, fechaCorte);
// tipoYDias[0] = TipoCarteraBanda.VENCIDO | POR_VENCER
// tipoYDias[1] = días
```

**`ContabilizacionIndividualCreditoServiceImpl.tipoCarteraYDias` es la ÚNICA definición.** Es
público en la interfaz, no tiene ciclos de dependencia, y ya delegan en él
`CierreCarteraServiceImpl.distribuye` y `AcuerdoCondonacionServiceImpl`.

**Este frente empezó porque el mismo cálculo estaba copiado en cinco lugares y tres divergían.** Un
reporte que copie la fórmula se convierte en la sexta copia y en la próxima divergencia.

## 2. El día del vencimiento es POR VENCER

**Regla de negocio confirmada por el usuario el 2026-09-04.** Una cuota que vence **el mismo día del
corte** está **por vencer**. Pasa a vencida **al día siguiente**.

| Vence vs corte | Tipo | Días |
|---|---|---|
| Posterior | `POR_VENCER` | días hasta el vencimiento |
| **Igual** | **`POR_VENCER`** | **1** |
| Un día antes | `VENCIDO` | **1** |
| 30 días antes | `VENCIDO` | **30** |

⛔ **Y no hay `+1` en la cuenta de días vencidos.** El `+1` que tenía el código compensaba que el día
del vencimiento contara como vencido; al corregir esa premisa, **pasó a correr un día todas las
cuotas vencidas y a mover la plata de banda en los bordes 30 / 90 / 180 / 360.** Un reporte que
conserve el `+1` va a diferir del mayor exactamente en las cuotas de borde.

## 3. ⛔ La corrida que ABRE un mes está registrada bajo el mes ANTERIOR

**Es el error más caro de este frente y el menos evidente.**

`CierreCarteraServiceImpl` graba `corrida.anio/mes` con el período que **cierra**, mientras que la
apertura que genera es la del mes **siguiente**:

```java
corrida.setAnio(solicitud.getAnio());   // JULIO
corrida.setMes(solicitud.getMes());     // JULIO
LocalDate fechaProceso       = fechaCorte.plusDays(1);                              // 01/08
LocalDate fechaCorteApertura = fechaProceso.withDayOfMonth(fechaProceso.lengthOfMonth()); // 31/08
```

**Para saber qué se abrió en agosto hay que buscar la corrida de JULIO.**

```java
CorridaCierreCartera corrida =
        corridaCierreCarteraDaoService.selectUltimaEjecutadaAntesDe(idEmpresa, anio, mes);
LocalDate fechaCorteApertura = corrida.getFechaProceso()
        .withDayOfMonth(corrida.getFechaProceso().lengthOfMonth());
```

- `selectUltimaEjecutadaAntesDe` **ya resuelve el cruce de año** (compara `anio*100+mes`). No
  reimplementarlo.
- ⛔ **El corte sale de `getFechaProceso()`, NUNCA de `getFechaCorte()`.** `getFechaCorte()` es el
  corte del **cierre** del mes anterior (31/07): usarlo parte por una fecha un mes anterior y **da
  números plausibles pero equivocados**, que es peor que no dar nada.
- **Buscar `selectVivaByPeriodo` con el mes del reporte no encuentra nada.** No existe una corrida
  registrada bajo el mes abierto.

## 4. Qué cierra la apertura y qué no

La apertura mensual abre en `2.3.02.10` (préstamos por aplicar) contra `1.4.05.10` (por cobrar) lo
que se espera cobrar del mes: `selectCobrablePrestamosHasta(fechaCorteApertura)`, o sea las cuotas
con **`DTPRFCVN <= fechaCorteApertura`**, con capital, interés, mora y seguros.

| Tramo del pago | ¿Toca las cuentas de apertura? |
|---|---|
| Vencido y cuota del mes (`DTPRFCVN <= corte`) | **Sí — cierra**, es lo que estaba abierto |
| **Capital futuro** (`DTPRFCVN > corte`) | **No. Ni abre ni cierra** — nunca se abrió |

**Da igual si se pagó con dinero o con cruce de valores.** Lo que decide es **qué** se pagó.

⭐ **Lo que cierra y lo que se abrió usan la MISMA definición de corte**, y por eso el neteo de fin de
mes cuadra. Un reporte que use otro corte no va a cuadrar aunque su lógica sea internamente
coherente.

## 5. Cada cuota se clasifica por su propia banda

**Nunca forzar `VENCIDO` para un conjunto.** `AcuerdoCondonacionServiceImpl` lo hacía y escondía el
error: con `Math.max(1, DAYS.between(vencimiento, fecha))`, una cuota **futura** da negativo y el
`max` la deja en **1** — banda 1 de vencidos, **sin error, sin traza, sin nada raro a la vista**.

**Si un reporte agrupa cuotas y clasifica el grupo, tiene el mismo defecto.**

## 6. Dónde mirar para contrastar

| Qué | Dónde |
|---|---|
| La definición única | `ContabilizacionIndividualCreditoServiceImpl.tipoCarteraYDias` |
| Qué abre la apertura | `CierreCarteraServiceImpl.armaApertura` + `selectCobrablePrestamosHasta` |
| Qué reversa el cierre | `CierreCarteraServiceImpl.armaNeteo` |
| El cierre parcial en el cobro | `CobroCreditoServiceImpl.generarAsientoDefinitivo` |
| El cierre parcial en el camino directo | `ContabilidadPrestamoServiceImpl.agregaCierreAperturaCaminoDirecto` |
| El diseño completo | `crd/DISENO-CIERRE-APERTURA-SOLO-LO-ABIERTO.md` |

## 7. La prueba que vale

**No es que el reporte cuadre consigo mismo: es que cuadre contra el mayor.**

Tomar un período cerrado y comparar, por banda y por cuenta contable, el total del reporte contra el
saldo del mayor de esa cuenta. **Si difieren, la diferencia casi siempre está en una de las cinco
reglas de arriba** — y en la 3 más que en ninguna, porque produce números plausibles.

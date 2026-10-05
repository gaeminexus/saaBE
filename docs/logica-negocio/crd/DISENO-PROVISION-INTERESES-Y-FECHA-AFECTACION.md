# DISEÑO — Provisión de intereses no cobrados, fecha de afectación del cobro y reverso por cobro tardío

**Equipo:** `omen-saa-1` · **Abierto:** 2026-10-05 · **Estado:** DISEÑO CERRADO salvo las cuentas por tipo de préstamo (§7bis). DDL: `sql/309`.
**Reemplaza y cierra** la decisión C6 (en pausa) de `crd/DISENO-COBRO-CON-FECHA-EFECTIVA.md`, y le da forma
a su Fase 2.
**Origen:** correo de contabilidad (Ing. Steven Cevallos) del 2026-09-17, *«Control de Intereses Por Cobrar
Préstamos (Inversiones Privativas) según Catálogo de la Super»*, y las decisiones del usuario del 2026-10-05.

---

## 1. Requerimiento

### 1.1 El asiento de la Superintendencia (correo de contabilidad)
| Cuándo | Asiento |
|---|---|
| **A fin de cada mes**, por los intereses no cobrados | **D `470510` Intereses inversiones privativas / H `149905` (Provisiones intereses inversiones privativas)** |
| **Cuando se cobra un valor ya provisionado**, en cada cobro atrasado | **D `149905` / H `470510`** (el reverso) |

### 1.2 Dos fechas en el cobro
- **Fecha de pago real:** cuándo pagó el partícipe. Con ella se graban los pagos de crédito (PGPR, EVPR, aportes)
  y a ella se recalcula la mora. Es `CBCR.CBCRFCHA`, que ya existe; la Fase 1 ya recalcula la mora.
- **Fecha de afectación:** con ella se hace **toda la contabilidad** del cobro. **Nunca menor** que la fecha de
  pago. Sirve para cobros que se identifican cuando el mes del pago ya se cerró.
- Aplica a **todos** los tipos de Cobros Personales que mueven un banco (todo CBCR).

### 1.3 Cobro tardío
Si el pago real es anterior a un cierre ya corrido, en la fecha de afectación se:
- **elimina** la mora generada después del pago (ya lo hace la Fase 1);
- **reversa la mora contabilizada** de ese tramo;
- **reversa las provisiones** de ese tramo.

---

## 2. Decisiones del usuario — 2026-10-05. NO re-litigar

| # | Decisión |
|---|---|
| P1 | Se provisiona **todo** el interés no cobrado: el ordinario **y la mora**. |
| P2 | **Una sola vez.** Lo provisionado no se vuelve a provisionar. De la mora, cada mes **sólo lo nuevo** (el delta desde la última provisión). |
| P3 | Las cuentas `470510` y `149905` **ya existen** en el plan de cuentas. |
| P4 | La provisión es un **paso más del cierre de cartera**. |
| P5 | **Arranca con el cierre de septiembre / apertura de octubre 2026.** El primer asiento provisiona todo lo vencido y no cobrado al 30-09. |
| P6 | La fecha de afectación **la pone crédito**; contabilidad **ve las dos fechas claramente**. |
| P7 | El reverso de la provisión aplica a **todos los cobros** de valores provisionados: Cobros Personales (CBCR), Petro, cruce de valores, jubilados y precancelación. |
| P8 | Los préstamos **DE PLAZO VENCIDO (8)** **también** se provisionan. |
| P9 | La mora a provisionar se calcula **a la fecha de corte**, con `calcularMoraCuota(cuota, tasa, corte)`, no con la persistida al ejecutar. |
| ex-C6 | Los asientos del cobro van con la **fecha de afectación**. |

---

## 3. Pieza central: el libro de intereses por cuota — `CRD.MVIC` (nueva, a autorizar)

Para provisionar «una sola vez» y reversar exacto hay que saber **cuánto se provisionó (y se devengó) de cada
cuota**. El cierre hoy sólo guarda totales (hallazgo del inventario, diseño de cobro §4ter). Esto **es** la
tabla de detalle autorizada en C7, aplicada a intereses.

Una fila por **movimiento** (nunca se actualiza ni se borra):

| Columna | Qué |
|---|---|
| `MVICCDGO` | PK |
| `PRSTCDGO` | préstamo (FK) |
| `DTPRCDGO` | cuota (Long, **sin FK**: H82, el abono y el reverso borran cuotas) |
| `MVICTPMV` | tipo: **1** PROVISIÓN (cierre ⑦) · **2** REVERSO POR COBRO · **3** REVERSO POR COBRO TARDÍO (mora eliminada) · **4** RE-PROVISIÓN (se anuló o reversó el cobro que la había reversado) · **5** DEVENGO DE MORA (cierre ④) · **6** REVERSO DE DEVENGO POR COBRO TARDÍO |
| `MVICCMPN` | componente: **1** INTERÉS (ordinario + vencido) · **2** MORA |
| `MVICVLOR` | valor, **siempre positivo**; el signo lo da el tipo |
| `MVICFCCT` | fecha contable del movimiento |
| `CRCTCDGO` | corrida del cierre (tipos 1 y 5) |
| `PGPRCDGO` | pago que lo originó (tipos 2, 3 y 4) |
| `MVICORGN` / `MVICIDOR` | origen (`CBCR` / `PETRO` / `CRUCE` / `PRECANCELACION` / …) y su id |
| `ASNTCDGO` | asiento contable que lo registró |
| `MVICREVR` | para el tipo 4: el movimiento que deshace |
| auditoría | usuario y fecha |

**Saldo provisionado de una cuota y componente:** Σ tipo 1 + Σ tipo 4 − Σ tipo 2 − Σ tipo 3.

---

## 4. Paso ⑦ del cierre de cartera — la provisión

- **Universo:** cuotas de préstamos `PRSTIDST IN (2, 8, 11)` (P8), con vencimiento ≤ corte, ni PAGADAS ni
  CANCELADAS ANTICIPADAMENTE. Las PARCIALES entran.
- **Por cuota:**
  - pendiente de interés = `interes + interesVencido − (interesPagado + IV pagado)`, desde PGPR vigente;
  - pendiente de mora = `calcularMoraCuota(cuota, tasa, corte) − mora pagada (PGPR)`, con piso en 0 (P9);
  - **a provisionar** = `max(0, pendiente − saldo provisionado)` por componente (P2).
- **Asiento:** uno por corrida, **D `470510` / H `149905`** por el total, fechado en la **fecha de corte** (fin
  de mes: «al final de cada mes»). Se registra en `ANCC` como subproceso 7. Un `MVIC` tipo 1 por cuota y
  componente con valor > 0.
- **Primera corrida (septiembre, P5):** el saldo provisionado es 0 en todas las cuotas, así que provisiona todo
  lo vencido y no cobrado al 30-09.
- **Reversar una corrida** (`CierreCarteraServiceImpl.reversar`, que ya existe): sus `MVIC` tipo 1 y 5 se dan de
  baja junto con sus asientos. Es el único caso en que un `MVIC` deja de contar. Se marca, no se borra.
- **Cuentas:** dos papeles nuevos en `CrdLineaAsiento` (por ejemplo `PROVISION_INTERESES_GASTO` → 4.7.05.10 y
  `PROVISION_INTERESES` → 1.4.99.05), en una **plantilla nueva, alterno 36** «CRD PROVISION INTERESES»
  (se reserva en el registro §2c, que dice 1–33 ocupados; el 34 y el 35 existen). La configura un `.sql`.

---

## 5. Reverso de la provisión al cobrar — todos los canales (P7)

Un **servicio único**, `ProvisionInteresService.reversarPorPagos(List<PagoPrestamo> pagos, LocalDate fecha,
String origen, Long idOrigen)`. Por cada pago y cuota:
- interés reversado = `min(interés pagado en ese pago, saldo provisionado de interés)`;
- mora reversada = `min(mora pagada en ese pago, saldo provisionado de mora)`.

Genera **un asiento separado** **D `149905` / H `470510`** por el total de la operación, más los `MVIC` tipo 2.
**Un solo lugar** lo llaman todos los cobros, después de su propio asiento y con su misma fecha contable:

| Canal | Dónde engancha | Fecha |
|---|---|---|
| CBCR (todos los tipos) | `CobroCreditoServiceImpl.procesarCobro`, junto a reparto y definitivo | **fecha de afectación** |
| Petro | `CobroPetroContableServiceImpl.contabilizarAplicacion` | la del asiento de aplicación |
| Cruce de valores / jubilados | `ContabilidadPrestamoServiceImpl.contabilizarPagoConAportes` | la del cruce |
| Precancelación directa | `contabilizarPrecancelacion` | la del evento |

**Deshacer un cobro** (`anularCobro`, `reversarProceso`, `anularOperacion` y el reverso de Petro): los `MVIC`
tipo 2 de esos pagos se compensan con tipo 4 (re-provisión) y su asiento espejo. **Sin esto, anular un cobro
dejaría la provisión reversada de un interés que vuelve a estar impago.**

---

## 6. Fecha de afectación y cobro tardío

### 6.1 Fecha de afectación en CBCR
- **DDL:** `CBCR.CBCRFCAF DATE`. Las filas existentes se rellenan con `CBCRFCHA`, que es con lo que se
  contabilizaron.
- **Registro (crédito):** campo obligatorio, por defecto la fecha de hoy. Validaciones:
  - `fechaAfectacion ≥ fecha` (fecha de pago) → si no, 400;
  - no futura;
  - período contable **abierto** en esa fecha (`/prdo/verificaPeriodoAbierto`).
- **Corrección:** rechazar y reenviar permite cambiarla, y el transitorio se rehace si cambia.
- **Asientos ①, ② y ③ del cobro y el reverso de provisión (§5):** fecha de afectación. Los pagos de crédito
  (PGPR, EVPR, aportes) **siguen con la fecha de pago real**. La banda del capital en el ③ se sigue
  clasificando a la **fecha de pago real** (C5).
- **Pantallas:** Cobros Personales pide las dos. La bandeja de contabilidad, Proceso de crédito y Seguimiento
  muestran **las dos, rotuladas** «Fecha de pago» y «Fecha de afectación contable» (P6).

### 6.2 Cobro tardío (Fase 2 del diseño de cobro)
Al procesar un CBCR, después del recálculo de mora de la Fase 1, por cada cuota del préstamo:
- si su **saldo provisionado de mora > mora pendiente recalculada** → se reversa el exceso con `MVIC` tipo 3, en
  el mismo asiento separado del §5;
- si el **devengo de mora del cierre ④** (`MVIC` tipo 5) supera la mora recalculada → se reversa el exceso con
  tipo 6, **D ingreso mora / H mora por cobrar**.

**Sólo hay qué reversar desde el cierre de septiembre**, que es el primero con libro. Antes no había
provisiones, y el devengo de mora de los cierres viejos no tiene detalle por cuota (§7, pregunta R3).

---

## 7. ⛔ Preguntas que faltan

| # | Pregunta | Por qué |
|---|---|---|
| R1 | **Bandas (C5):** dijiste antes que el cobro tardío también reversa el pase a vencido y entre bandas que hizo el cierre. ¿Sigue en el alcance? | Es otra pieza grande; se puede dejar para después de septiembre |
| R2 | **Condonación:** el interés y la mora **condonados** de una cuota provisionada, ¿también reversan la provisión? | No es un cobro, pero deja de estar por cobrar |
| R3 | **Cierres anteriores a septiembre:** su devengo de mora no tiene detalle por cuota. ¿Se acepta que el reverso por cobro tardío sólo actúe sobre lo contabilizado desde septiembre? | Si no, hay que recalcular corridas viejas |
| R4 | **El asiento de provisión del cierre:** ¿uno solo por el total, o separado por tipo de préstamo (como las líneas de interés)? | Define si la plantilla usa la dimensión tipo de préstamo |

---

## 7bis. Respuestas del usuario — 2026-10-05

| # | Decisión |
|---|---|
| R1 | **Bandas: SÍ siguen en el alcance.** El cobro tardío reversa también el pase a vencido y entre bandas que hizo el cierre. |
| R2 | **Condonación: SÍ reversa la provisión** del interés y la mora condonados (`MVIC` tipo 9, origen CONDONACION). |
| R3 | **Aceptado:** el reverso por cobro tardío sólo actúa sobre lo contabilizado **desde el cierre de septiembre 2026**, que es el primero con libro. |
| R4 | **El asiento de provisión va separado por tipo de préstamo** (quirografario / hipotecario / prendario): líneas con `DTPLAXL2 = TPPRCDGO`, como las de interés. |
| — | **`CRD.MVIC` AUTORIZADA** por el usuario. La clasificación de capital por banda (R1) va **en la misma tabla** (componente 3, tipos 7 y 8): no se pide otra. DDL: `sql/309`. |

### Bandas por cobro tardío (R1), con el libro

- **El cierre registra la clasificación** (`MVIC` tipo 7, componente 3): tipo de cartera, banda y capital pendiente
  a la fecha de corte, para cada cuota con **vencimiento ≤ fin del mes que se abre**. Cubre las vencidas y las
  que vencen el mes siguiente. Las de vencimiento más lejano sólo se mueven entre bandas por vencer, no se
  registran y el reverso no las toca (§7ter).
- **El cobro tardío:** si una cuota pagada tiene una clasificación del último cierre posterior a la fecha de pago
  en una banda **distinta** de la banda que tenía a la fecha de pago (la que usa el ③), se genera:
  - **D banda a la fecha de pago / H banda del último cierre**, por el capital pagado de esa cuota (con tope en el
    capital clasificado);
  - un `MVIC` tipo 8;
  - todo en el asiento separado del cobro tardío, con fecha de afectación.

  Así, el ③ descarga la banda de la fecha de pago y este asiento devuelve el capital desde donde el cierre lo
  había dejado.

### 7ter. Lo que queda fuera, a propósito
- Cuotas con vencimiento posterior al mes que se abre, pagadas por adelantado y reportadas tarde: sólo
  cambiaron de banda dentro de «por vencer». No se reclasifican.
- Cierres anteriores a septiembre 2026 (R3).

### ⛔ Falta un dato para escribir la plantilla 36
**El número de cuenta de cada tipo de préstamo** para `470510` y `149905`. ¿Hay subcuentas por tipo (por
ejemplo `4.7.05.10.01` quirografario…) o es la **misma cuenta** para los tres, separada sólo por el auxiliar?
Hasta saberlo, el `.sql` de la plantilla no se escribe. El código no depende de esto: usa los papeles.

## 8. Orden de construcción

1. DDL: `CBCRFCAF` + `MVIC` + plantilla 36 y sus dos papeles.
2. Fecha de afectación de punta a punta (BE + FE).
3. `ProvisionInteresService`: el paso ⑦ del cierre + el reverso por cobro (CBCR primero, después Petro, cruce y
   precancelación) + la re-provisión al deshacer.
4. Cobro tardío (6.2).

**El cierre de septiembre espera a 1–3.**

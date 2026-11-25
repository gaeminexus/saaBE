# DISEÑO — Provisión de intereses no cobrados, fecha de afectación del cobro y reverso por cobro tardío

**Equipo:** `omen-saa-1` · **Abierto:** 2026-10-05 · **Estado:** DISEÑO CERRADO. DDL: `sql/309`; plantilla: `sql/310`.
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
- **Reversar una corrida** (`CierreCarteraServiceImpl.reversar`, que ya existe): sus `MVIC` tipo **1, 5 y 7** se dan
  de baja (`MVICANUL = 1`) junto con sus asientos. Es el único caso en que un `MVIC` deja de contar. Se marca, no se borra.
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

**Fecha de la re-provisión en CBCR (D-043, 29-oct-2026).** `anularCobro` y `reversarProceso` la registran con la
**fecha de afectación del cobro** si su período contable sigue abierto (ACTIVO o DESMAYORIZADO, sin cierre C17:
`verificaPeriodoAbierto`), que es la misma fecha del reverso de provisión que compensa; en ese caso los asientos
del cobro se anulan en su sitio y el mes queda como si el cobro no se hubiera procesado. Si ese período ya está
mayorizado o cerrado, va con la fecha de hoy (los asientos del cobro se reversan en el período abierto), como
antes. Antes iba siempre con la fecha de hoy: reversar en re-proceso un cobro de un mes pasado dejaba la
re-provisión en el mes en curso (cobro 227, secuencia 44 del expediente). El hook de `anularOperacion` fuera de
CBCR (`ContabilidadPrestamoServiceImpl.contabilizarReverso`, CASO A) y el re-proceso Petro no cambian: el
primero sigue con hoy, el segundo ya recibe la fecha contable del mes de la carga.

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

### Plantilla 36 — decidido por el usuario (2026-10-05)
**Hoy la MISMA cuenta para los tres tipos de préstamo** (4.7.05.10 y 1.4.99.05). Las líneas igual quedan una por
tipo (`DTPLAXL2 = TPPRCDGO`), así que si mañana contabilidad abre subcuentas por tipo **se cambia el
`PLNNCDGO` de esa línea** sin tocar código. Script: **`crd/sql/310`**.

## 8. Orden de construcción

1. DDL: `CBCRFCAF` + `MVIC` + plantilla 36 y sus dos papeles.
2. Fecha de afectación de punta a punta (BE + FE).
3. `ProvisionInteresService`: el paso ⑦ del cierre + el reverso por cobro (CBCR primero, después Petro, cruce y
   precancelación) + la re-provisión al deshacer.
4. Cobro tardío (6.2).

**El cierre de septiembre espera a 1–3.**

## 9. Precisiones del ítem 0 del backend (2026-10-05, aprobadas por el árbitro)
- **Condonación (R2):** el acuerdo cierra TODAS las cuotas pendientes. Se reversa el saldo provisionado completo
  de cada cuota que cierra, con tope en ese saldo. Orden: primero el tipo 2 por la parte pagada del acuerdo y
  después el tipo 9 sobre lo que quede. Σ tipo 9 puede no coincidir con `interesCondonado`, porque el acuerdo
  guarda el condonado agregado por concepto, no por cuota.
- **Petro no tiene reverso** de una carga ya procesada (`reversarRecepcion` lo rechaza), así que no hay
  re-provisión en ese canal. Si se construye, va ahí.
- La re-provisión por deshacer un cobro CBCR va en `ProcesoPagoPrestamoServiceImpl.anularOperacion`, el único
  punto que anula PGPR para `anularCobro` y `reversarProceso`.
- **MVIC tipo 7 queda RESERVADO y SIN USO** (2026-10-05). La banda de una cuota en el último cierre se
  **reconstruye** en el cobro tardío, sin foto. Se calcula con `tipoCarteraYDias(fechaVencimiento, corte de la última
  corrida no reversada)`, que es pura, y los rangos de `BandaProductoDetalle` vigentes a la `fechaProceso` de esa
  corrida. Es el mismo resolvedor que usa `distribuye()`, extraído a un método común.
  La banda a la fecha de pago es la misma que ya usó el ③ (`haberDesdeEvento`), y el capital, el que el ③ acreditó.
  Una foto por cuota en cada cierre habría sido enorme y no agrega información.
- **MVIC tipo 5** se graba por cuota en el ④, con la misma consulta y la misma fuente (`DTPRMRAA` persistida) que el
  total del asiento. Si la suma por tipo de préstamo no coincide con el asiento, el asiento manda y la diferencia
  queda en el log.

## Lote 17 — todo cobro de interés o mora provisionados reversa su provisión, cuota por cuota

### Matriz por camino (verificada en el código, 2026-11-25)
| Camino | ¿Reversa la provisión por cuota? | Método |
|---|---|---|
| Ventanilla (cobro de crédito) | Sí | `CobroCreditoServiceImpl.procesarCobro` → `reversarPorPagos` con los PGPR de los eventos del cobro (origen CBCR) |
| Petro | Sí | `CobroPetroContableServiceImpl` → `reversarPorPagos` (origen PETRO) |
| Cruce de pensión | Sí | `ContabilidadPrestamoServiceImpl.contabilizarPagoConAportes` → `reversarPorPagos` (origen CRUCE) |
| Débito | Sí | No tiene camino propio: entra por el cobro de crédito o por Petro |
| Acuerdo con depósito | **No (corregido)** | El acuerdo graba UN PGPR anclado en la última cuota pagada con el interés y la mora de todas las cuotas que cierra; `reversarPorPagos` (por la cuota del pago) acotaba contra la provisión de esa cuota, no de las vencidas. Ahora `aplicarAcuerdo` llama a `reversarCobradoEnCuotas` |
| Acuerdo sin depósito (100% aportes) | **No (corregido)** | No llamaba a ningún reverso de lo cobrado. Mismo arreglo |
| Precancelación | Sí | `contabilizarPrecancelacion` / `...ConPrestamo` → `reversarPorPagos` (CASO A) o el cobro (CASO B): un PGPR por cuota vencida que cierra |
| Abono a capital | Sí, en lo que cobra | El interés de las cuotas vencidas que paga va por el cobro. El abono no toca la provisión de las cuotas que reamortiza (futuras, aún sin provisionar) |

### Corrección
- `ProvisionInteresService.reversarCobradoEnCuotas(cuotas, interesCobrado, moraCobrada, idPago, ...)`: reparte lo cobrado entre las cuotas que el acuerdo cierra, la más antigua primero, consume el saldo provisionado de cada una **sin pasar de él** ni de lo cobrado, y graba MVIC tipo 2 por cuota y componente con el `idPago` del acuerdo, más el asiento D 1.4.99.05 / H 4.7.05.10 por tipo de préstamo (cuentas de la plantilla 36; no hay línea nueva ni semilla).
- `AcuerdoCondonacionServiceImpl.aplicarAcuerdo`: lo llama antes de `condonarProvision` (la condonación consume lo que queda).
- `CobroCreditoServiceImpl.procesarCobro`: los pagos de tipo ACUERDO_CONDONACION se excluyen de `reversarPorPagos` para no contarlos otra vez contra la cuota ancla.
- Anulación: el MVIC lleva el `idPago`, así que `reProvisionarPorAnulacion` lo encuentra al anular el cobro o el evento; ahora toma el tipo de préstamo del propio MVIC cuando su cuota no es la del pago.
- Tope: `reversarPorPagos`, `condonarProvision` y `reversarCobradoEnCuotas` acotan contra el saldo provisionado de cada cuota. Las provisiones negativas históricas (−1.787,26 al 30-nov) no salen de este camino: se regularizan con el ajuste por cuota del cuadre (A1).

### Nivel 1.3 — condonación, castigo y anulación (solo descrito)
- **Condonación** (solo el acuerdo): mueve la 1.4.02 por el interés y la mora condonados y la provisión de las mismas cuotas por el mismo valor, con un tope por cuota (MVIC tipo 9). Si la provisión de una cuota es menor que lo condonado, la diferencia queda sin provisión que bajar: es lo aprobado en 7bis.
- **Anulación del acuerdo**: `reProvisionarPorAnulacion` solo compensa MVIC tipo 2 y 3 ligados a un pago. El tipo 9 de la condonación no lleva `idPago` (origen CONDONACION, `idOrigen` = acuerdo), así que **al anular un acuerdo la provisión condonada no se restituye**. Pide un diseño: ligar el tipo 9 al evento o buscarlo por origen e `idOrigen`.
- **Castigo**: no hay un camino de castigo en `crd` que toque la provisión por cuota; no se corrige.
- Pendiente de verificar en una copia: acuerdo con depósito y sin depósito (provisión de las cuotas 49 y 50 del caso del cuadre a cero), anulación del cobro y re-provisión del reverso.

## Lote 18 — el ⑦ arma el asiento y el libro desde el MISMO cálculo

### Causa (confirmada en el orden real de los pasos)
`CierreCarteraServiceImpl.calcula` arma las líneas del ⑦ con `calcularProvisionPorTipoPrestamo` ANTES de grabar nada. Después `ejecutar` recorre los sub-procesos en orden y, en `generaAsiento`, el ⑩ (devengo de mora, MVIC tipo 5 con fecha contable al corte) va ANTES que el ⑦. Cuando llega el ⑦, `registrarProvisionCierre` recalcula todo, y el tope de la mora (D-186: mora contabilizada pendiente por préstamo, tipo 5 − 6 con fecha ≤ corte) ya incluye lo que acaba de grabar el ⑩. El libro sale mayor que el asiento y la diferencia está toda en la mora. Es la sospecha del cuadre, confirmada.

### Cambio
- Un solo cálculo por cuota: `ProvisionInteresService.calcularProvisionPorCuota(corte, alCorte, devengoMoraDelCierre)` devuelve la lista (`ProvisionCuotaCalculada`: cuota, préstamo, tipo, interés, mora). El asiento por tipo de préstamo es la suma de esa lista, al centavo. `registrarProvisionCierre(..., lista)` graba el libro (MVIC tipo 1) de ESA lista, sin recalcular. La lista viaja en el sub-proceso (campo transitorio, fuera del JSON).
- El tope ve el ⑩ del mismo cierre: antes de calcular, `armaProvisionIntereses` obtiene el devengo del ⑩ por préstamo (la misma consulta que graba el ⑩) y `selectMoraContabilizadaPendientePorPrestamo(corte, devengoAdicional)` lo suma al devengo ANTES del piso en cero, como si ya estuviera en el libro. Así la mora provisionada incluye la que devenga el mismo cierre (que es lo que tiene la 1.4.02) y no depende del orden.
- Previsualización: usa el mismo cálculo y el mismo devengo adicional, así que muestra los valores del asiento ejecutado (mientras la base no cambie entre las dos llamadas).
- Los callers viejos (`calcularProvisionPorTipoPrestamo`, `registrarProvisionCierre` con recálculo) siguen, sin uso en el cierre.

### Nivel 2.2 (mora = mora contabilizada pendiente del préstamo) — descrito, NO cambiado
Hoy el tope del préstamo se reparte entre sus cuotas «sin pasar de la mora calculada pendiente de cada una» (`TopeProvisionMora.repartir`), y la herramienta `/prov/ajusteTopeMora` usa la MISMA función para llevar cada cuota a su tope (tipo 10 = max(0, saldo − tope de la cuota)). Quitar el límite por cuota en el ⑦ choca con el ajuste: el ⑦ subiría la provisión de una cuota por encima de su tope de la función y la siguiente corrida del ajuste la volvería a bajar, o el tope sin límite cargaría todo el préstamo en la cuota más vieja y el ajuste vaciaría las demás, que es justo lo que dejó cuadrado. Cambiarlo exige cambiar `repartir` y el ajuste a la vez (y rehacer lo ya ajustado), así que queda para decidirlo con el cuadre. Los 1.843,89 por debajo al 30-nov son préstamos con mora contabilizada mayor que la calculada por cuota.

### Anular un acuerdo restituye la provisión condonada
`AcuerdoCondonacionServiceImpl.reabrirAcuerdoPorReverso` (lo usan la anulación del cobro y la del evento) llama a `ProvisionInteresService.reProvisionarCondonacion(idAcuerdo, ...)`: busca por origen CONDONACION e `idOrigen` los MVIC tipo 9 vigentes, los compensa con un MVIC tipo 4 por cuota y por el mismo valor, y genera D 4.7.05.10 / H 1.4.99.05 por tipo de préstamo con la plantilla 36. Idempotente. Va con la fecha del reverso; si su período está cerrado, la anulación del acuerdo falla con el mensaje del asiento.

### Lo ya acumulado en oct y nov 2025
Recomendación: un asiento manual por la diferencia, ligado al ajuste A1, y no tocar el libro. El libro (mayor) es el que refleja la mora contabilizada (la 1.4.02), y el asiento del ⑦ de esos meses quedó por debajo; el asiento manual lleva la 1.4.99.05 al libro, por tipo de préstamo y por componente (D 4.7.05.10 / H 1.4.99.05), y A1 deja el saldo por cuota igual a su devengado no cobrado. Ajustar el libro hacia abajo borraría provisión de mora que sí tiene su devengo en 1.4.02.

## Lote 21 — arranque de los libros por cuota a un corte cualquiera: A2 (semilla del devengo de interés) y A1 (ajuste del libro de provisión)

Dos herramientas, sin asiento contable propio, ligadas al asiento manual del cuadre que ya lleva la contabilidad a esos valores. **Nada fijo:** el corte y el asiento de referencia son parámetros, para usarlas en el arranque que toque (el cuadre resuelve octubre y noviembre de 2025 en el reproceso). Requieren `sql/406` (tipos 11 y 12) y `sql/409` (tipos 13 y 14) corridos ANTES del EAR.

### Definición común: «devengado y no cobrado al corte»
Por cuota, de las que ve el ⑦ al corte (vencidas impagas y la que vence el día del corte y no se pagó, con la regla de préstamos cancelados del ⑦), **sin** las cuotas cerradas (estado 7) ni las anuladas:
- **Interés** = interés + interés vencido de la cuota − interés pagado − interés vencido pagado, con los pagos de fecha efectiva ≤ corte, piso 0 y 0 si el interés está condonado por plazo vencido. Es la misma cuenta del ⑦ (`pendienteInteres`), a propósito: así el ⑦ siguiente ve lo mismo.
- **Mora** (solo A1) = lo que el ⑦ quiere provisionar: la mora contabilizada pendiente del préstamo repartida entre sus cuotas (`TopeProvisionMora`) si el tope (D-186) está activo al corte, y la mora calculada pendiente si no.
- Se usa el estado ACTUAL de la cuota (7 o anulada excluye aunque se haya cerrado después del corte).

### A2 — `POST /rest/prov/semillaDevengoInteres`
Cuerpo `{idEmpresa, corte, idAsientoReferencia, grabar, usuario}`. Graba un MVIC tipo 11 por cuota con el interés devengado y no cobrado, fecha contable = corte, origen `SEMILLA_DEVENGO_INTERES`, `idOrigen` = `MVICASNT` = la referencia. Sin asiento propio.
- Idempotente por referencia: si hay filas vigentes con ese origen e `idOrigen`, no graba y devuelve lo ya grabado (`yaGrabado = true`).
- No siembra cuotas que ya tienen un MVIC 11 o 12 vigente con fecha ≤ corte (las alimentó el ④ nuevo): las cuenta aparte (`cuotasYaAlimentadas`).
- `grabar = false` devuelve la suma por tipo de préstamo y el número de cuotas. El asiento de referencia debe existir y estar ACTIVO (no se exige que su fecha sea el corte: el del cuadre puede ir fechado el día siguiente).
- `POST /rest/prov/semillaDevengoInteres/anular` anula (MVICANUL = 1) la semilla de una referencia.

### A1 — `POST /rest/prov/ajusteLibroProvision`
Cuerpo `{idEmpresa, corte, idAsientoReferencia, grabar, usuario}`. Deja el saldo provisionado de cada cuota (Σ tipos 1, 4, 14 − Σ tipos 2, 3, 9, 10, 13, vigentes, fecha ≤ corte) igual a su interés y su mora devengados y no cobrados:
- diferencia = objetivo − saldo, por cuota y componente. Si es negativa graba un MVIC **13** (baja, resta); si es positiva, un **14** (alta, suma). Son dos tipos porque `CK_MVIC_VALOR` obliga a valor ≥ 0 y el signo lo da el tipo (el pedido hablaba de un 13; hizo falta un 14). Origen `AJUSTE_LIBRO_PROVISION`, `idOrigen` = `MVICASNT` = la referencia, fecha contable = corte.
- Cuotas cerradas (estado 7, pagadas o fuera del universo del ⑦): su objetivo es 0, así que su provisión baja a 0. Provisiones negativas: suben hasta 0 (o hasta su objetivo).
- Respeta el tipo 10 (D-186): el saldo ya lo descuenta, así que si el corte coincide con uno ya ajustado por D-186, A1 solo mueve lo que falta.
- `grabar = false` devuelve, por tipo de préstamo, baja, alta y neto (alta − baja) de interés y de mora, y aparte `deCuotasCerradas` (lo que baja por cuotas cerradas) y `deNegativas` (lo que sube por provisiones negativas), más `totalBaja` y `totalAlta`: el cuadre los cruza al centavo con su asiento manual (neto positivo = sube la 1.4.99.05).
- Idempotente por referencia; `POST /rest/prov/ajusteLibroProvision/anular` anula los ajustes de una referencia. Al devolver lo ya grabado, `deCuotasCerradas` y `deNegativas` no se reconstruyen.

### Por qué el ⑦ siguiente no genera otra diferencia
El ⑦ provisiona por cuota `max(0, pendiente − saldo provisionado)`, y ese saldo (`saldoProvisionadoPorCuotas`, también al corte) ya suma los tipos 14 y resta los 13. Después de A1 el saldo de cada cuota es su objetivo, que es el mismo `pendiente` que usa el ⑦ (misma cuenta de interés, y la mora con el mismo tope): la diferencia es 0 salvo lo que se devengue o se cobre de verdad después del corte. Sin A1, el ⑦ vería la provisión vieja (cuotas cerradas, negativas, mora de más) frente a su objetivo y generaría otro asiento por lo mismo que el cuadre ya contabilizó. A2 hace lo propio con el libro de devengo: sin la semilla, el libro de interés empezaría en cero y la regla de oro (Σ(11 − 12) = 1.4.02) no cerraría.

### Orden de uso
1. El cuadre carga su asiento manual (D 1.4.99.05 / H 4.7.05.10 y D 1.4.02 / H 5.1.02 por tipo y componente) y da su código: es el `idAsientoReferencia`.
2. A1 y A2 con `grabar = false`, con el mismo `corte`.
3. Cruce: el neto por tipo y componente de A1 contra las líneas de 1.4.99.05 del asiento, y la suma por tipo de A2 contra la 1.4.02 (`deCuotasCerradas` y `deNegativas` explican la parte «cuotas cerradas» y «negativas» del asiento). Si no cruza, no se graba.
4. A1 y A2 con `grabar = true`. Repetir la llamada no duplica. Para deshacer: los `/anular` correspondientes.

## Lote 22 — control 1.4.02 = 1.4.99.05 en el cierre, ajuste de último recurso, bloqueo de /prdo/cerrar y control del día 1

Todo rige solo con el devengo completo activo al corte (`saa.cartera.devengoCompletoDesde`). Las cuentas salen de las plantillas, nunca del código: la 1.4.02 de la línea `INTERES_ORDINARIO_POR_COBRAR` de la plantilla 17 y la 1.4.99.05 de la línea `PROVISION_INTERESES` de la 36, por tipo de préstamo; los saldos con `selectSaldoContablePorCuenta` (cuadratura C14: asientos activos o mayorizados con fecha ≤ corte). Tolerancia 0,01 por tipo de préstamo y por diferencia.

### 2.3 — el control (`ControlProvisionInteresService.calcular`)
Por tipo de préstamo se calculan, al corte:
- (a) **contable contra contable**: 1.4.02 − 1.4.99.05 (la regla; debe dar 0).
- (b) **cada cuenta contra su libro por cuota**: 1.4.02 − (Σ(11−12) + Σ(5−6)) y 1.4.99.05 − saldo provisionado del libro (Σ 1, 4, 14 − Σ 2, 3, 9, 10, 13).
- por **componente** (solo libro): devengo de interés − provisión de interés, y devengo de mora − provisión de mora. Las dos cuentas comparten cuenta contable entre interés y mora (se distinguen por la descripción), así que el componente solo se puede comparar en el libro.
En la **previsualización** es una proyección: suma a lo asentado hoy lo que este cierre todavía va a asentar con libro (el ⑦, el ⑩ y el ④b); sale en `controlProvision` y, si no cuadra, como advertencia. No incluye el ⑪ (interés anticipado), que mueve una cuenta 1.4.02 sin libro por cuota: si la 1.4.02 de la plantilla 17 es la misma cuenta, aparecerá como diferencia hasta decidir su tratamiento.

### 2.3 — el ajuste de último recurso (`controlarYAjustar`, después de los pasos y antes de marcar la corrida EJECUTADA)
Si queda diferencia, siempre se **registra la excepción** (por tipo: saldos contables y de libro, las diferencias y la **causa probable**) en `CRCTOBSR` de la corrida (hasta 1.500 caracteres) y en las advertencias de la respuesta. El **asiento** solo se genera con la propiedad `saa.cartera.ajusteUltimoRecurso = true` (apagada por defecto, para no tocar la contabilidad de producción hasta que el líder lo pida). Con ella encendida:
1. **El libro primero.** Se calcula por cuota lo que debería decir (el mismo cálculo de A2 y A1: interés devengado y no cobrado de las cuotas del ⑦ y, para la provisión, también la mora con la regla del ⑦; las cuotas cerradas valen 0). Lo que falta se graba con origen `AJUSTE_ULTIMO_RECURSO`, `idOrigen` = la corrida: MVIC 11 o 12 para el devengo de interés de la 1.4.02 y 13 o 14 para la provisión. Con eso el mes siguiente no repite la diferencia.
2. **La contabilidad al libro.** Un asiento (subproceso 13, `sql/410`) por tipo de préstamo lleva la 1.4.02 contra el ingreso (líneas `INTERES_ORDINARIO_POR_COBRAR` e `INGRESO_INTERES_ORDINARIO` de la plantilla 17, 5.1.02) y la 1.4.99.05 contra el gasto (`PROVISION_INTERESES` y `PROVISION_INTERESES_GASTO` de la 36, 4.7.05.10) por lo que falte para igualar el libro ya corregido.
**Cómo se distinguen los casos:** si el libro por cuota ya dice lo que debe (no hay deltas de A1/A2), el ajuste es **solo contable** (un asiento, ningún MVIC): la contabilidad se movió sin libro (asiento manual, ⑪, un reverso sin MVIC 12). Si el libro está mal (provisión de cuotas cerradas o negativas, devengo sin semilla), se corrige **el libro por cuota** y la contabilidad sigue al libro corregido (MVIC y asiento). Si el libro y la contabilidad coinciden y solo la regla 1.4.02 ≠ 1.4.99.05 no cierra (por ejemplo la mora provisionada bajo la devengada por el tope del 2.2), no hay nada que asentar automáticamente: queda como excepción. La anulación del cierre anula estos MVIC y el asiento (`anularByCorrida` incluye ahora los tipos 11 a 14 con corrida).

### 2.4 — bloqueo de `/prdo/cerrar`
`CierrePeriodoContableService.cerrar` consulta el control del mes (lo asentado, `controlParaCierreContable`) y rechaza con `CIERRE_PROVISION_DESCUADRADA` si hay diferencia y **no hay un ajuste de último recurso registrado** (MVIC vigentes con origen `AJUSTE_ULTIMO_RECURSO` y fecha contable = corte). Solo rige con el devengo completo activo al corte **y** con la propiedad de sistema **`saa.cartera.bloqueoCierreProvision = true`**, apagada por defecto.

### 3.1 — control del día 1 (informativo)
Después del ④, `controlDia1` agrega a `controlProvision.dia1`, por tipo de préstamo: (1.4.02 − 1.4.99.05 al primer día del mes que abre), el devengo del asiento del ④ (MVIC 5 y 11 de ese asiento) y la diferencia entre los dos (debe ser 0). No bloquea ni asienta nada.

### Propiedades y scripts
- `saa.cartera.ajusteUltimoRecurso` (asienta el ajuste; apagada) y `saa.cartera.bloqueoCierreProvision` (bloquea `/prdo/cerrar`; apagada).
- `sql/410` (CK_ANCC_TPOO hasta 13): antes del EAR si se enciende el ajuste. Con `sql/406` y `sql/409` para los tipos 11 a 14. Sin ejecutar.
- Antes de encender el ajuste en un ambiente, correr A2 y A1 al corte de arranque: sin ellos el primer cierre vería todo el libro vacío como diferencia.

## Lote 23 — el reparto del tope de la mora coloca SIEMPRE todo el tope del préstamo (nivel 2.2)

**Defecto (cuadre, mensaje 14, punto 2.2).** `TopeProvisionMora.repartir` asignaba a cada cuota `min(mora calculada pendiente, lo que queda del tope)` y lo que sobraba del préstamo se perdía: al 30-nov-2025, en la previsualización de A1, la mora de la 1.4.02 era 66.064,49 y la provisión de mora 65.889,90; los 174,59 de diferencia eran sobrante de préstamos que no caía en ninguna cuota. **El tope de D-186 no cambia** (mora contabilizada pendiente por préstamo); solo cambia cómo se reparte.

**Regla nueva.** Después del reparto de siempre (de la cuota más vieja a la más nueva, sin pasar de la mora calculada de cada una), el sobrante de cada préstamo (tope − Σ repartido) se suma a la **cuota candidata más reciente** del préstamo (la última por vencimiento y código), aunque pase de su mora calculada. Si el préstamo no tiene ninguna candidata, va a su **cuota de respaldo**: la cuota con saldo de provisión de mora mayor que cero y el vencimiento más reciente (`selectCuotaRespaldoMoraPorPrestamo`). Si tampoco hay, el sobrante queda en `sinRepartir` y el llamador lo reporta (advertencia de `/prov/ajusteTopeMora`; aviso en el log del ⑦ y de A1), nunca en silencio. Invariante: Σ repartido del préstamo = su tope, al centavo (el redondeo se ajusta en la cuota que recibe el sobrante).

**Los tres llamadores usan el mismo reparto** (`repartir(tope, candidatas, respaldo, sinRepartir)`): el ⑦ (`ProvisionInteresServiceImpl.calcularItems`), `/prov/ajusteTopeMora` (`AjusteTopeProvisionServiceImpl`) y A1 y las deltas del ajuste de último recurso (`LibroInteresArranqueServiceImpl.universoAlCorte`). Se revisó que ninguno anule el efecto filtrando por mora calculada: el **⑦ sí lo hacía** (`pendienteMora = min(mora calculada, tope repartido)`) y ahora provisiona el tope repartido tal cual; el ajuste del tope y A1 ya usaban el reparto directo. Cuando la cuota de respaldo no está entre las que recorre cada llamador (por ejemplo una cuota cerrada que conserva su provisión): en A1 el objetivo de esa cuota es lo que le toca del tope (`moraFuera`); en el ajuste del tope su tope sube y por eso el ajuste le quita menos; el ⑦ no la recorre y no le provisiona nada (ya tiene su provisión).

**Idempotencia de `/prov/ajusteTopeMora` con un corte ya ajustado.** El ajuste solo BAJA: por cuota graba `max(0, saldo provisionado − tope de la cuota)`, y el saldo ya descuenta los tipo 10 grabados. Con el reparto nuevo, los topes de todas las cuotas que no son la última candidata del préstamo no cambian, y el de la última sube (recibe el sobrante): una segunda corrida del ajuste con el mismo corte da 0 en las cuotas que ya habían quedado en su tope viejo, así que **no mueve el sobrante ni repite lo ya grabado**; mueve solo la diferencia nueva (cuotas cuyo saldo siga por encima del tope nuevo). Los tipo 10 ya grabados siguen válidos: no hay que anularlos ni regrabarlos. El sobrante no lo sube el ajuste del tope; lo sube el ⑦ siguiente (que ahora provisiona hasta el tope repartido de la última cuota) o A1 con un MVIC 14 hasta el objetivo.

**Prueba pedida por el cuadre (no ejecutada).** Con este código desplegado en la copia de trabajo: (1) correr el `391` v2 al 30-nov-2025, donde para cada préstamo la mora del libro (tope repartido, Σ por préstamo) debe ser igual a la mora contabilizada pendiente del préstamo, al centavo; (2) `POST /prov/ajusteLibroProvision` con `grabar=false` al 30-nov-2025: la mora del libro (la provisión de mora objetivo) debe igualar la mora de la 1.4.02 (66.064,49) y los 174,59 deben desaparecer; (3) comprobar que `sinRepartir` no trae préstamos (si trae, es mora contabilizada de un préstamo sin ninguna cuota donde colocarla) y que `/prov/ajusteTopeMora` con `grabar=false` sobre un corte ya ajustado devuelve 0 de ajuste.

## Lote 24 — el control por CUENTA contable y A2 en modo DIFERENCIA

Los dos salen de probar los lotes 16 a 22 en el reproceso (cierre de diciembre de 2025, corrida 165); la semilla A2 y el ajuste A1 al 30-nov salieron correctos.

### 1. El control compara por cuenta, no por tipo (`ControlProvisionInteresService`)
**Defecto.** El control del lote 22 comparaba la 1.4.99.05 de CADA tipo contra el saldo de su cuenta; en ASOPREP la provisión es UNA sola cuenta para Q, P y H, así que cada tipo recibía el saldo total y salían diferencias de un millón por tipo (bloqueaba encender `saa.cartera.ajusteUltimoRecurso`). Lo mismo pasaría con la 1.4.02 si dos tipos comparten cuenta.

**Ahora** (`ResultadoControlProvision`):
- **Por cuenta (`porCuenta`)**: cada cuenta contable de la 1.4.02 (línea `INTERES_ORDINARIO_POR_COBRAR` de la plantilla 17) y de la 1.4.99.05 (línea `PROVISION_INTERESES` de la 36) se lee UNA vez, y su saldo se compara contra Σ del libro de los tipos que la usan (1.4.02: Σ(11−12) + Σ(5−6); 1.4.99.05: saldo provisionado). Lleva los tipos que la usan.
- **Por grupo de cuentas (`porGrupo`)** y **al total** (`totalContable1402`, `totalContable149905`, `difTotal`): la regla del contador, Σ 1.4.02 = Σ 1.4.99.05. Un grupo es el conjunto de tipos que comparten alguna cuenta (una cuenta para los tres tipos es un grupo; tres cuentas distintas son tres grupos).
- **Por tipo (`porTipo`)**: solo el libro (devengo − provisión por interés y por mora), que sí es por tipo.
- La previsualización proyecta lo que el cierre aún va a asentar con la misma agrupación (el Δ de cada tipo se suma a la cuenta que usa).
- El bloqueo de `/prdo/cerrar` y el control del día 1 usan lo mismo: el bloqueo reporta las excepciones por cuenta, grupo y total; el día 1 compara, por grupo de cuentas, (1.4.02 − 1.4.99.05 al día 1) contra el devengo del ④ de los tipos del grupo.

**El ajuste de último recurso asienta por cuenta.** Primero se corrige el libro por cuota (igual que antes). Después, para cada cuenta, el movimiento contable que falta es `Σ libro de sus tipos (ya corregido) − saldo contable`:
- cuenta de un solo tipo: se asienta todo lo que falta (como en el lote 22);
- cuenta COMPARTIDA: se asienta por tipo SOLO lo que el libro de cada tipo explica (lo que corrige el ajuste del libro, repartido según la corrección de cada tipo con las líneas de la plantilla de ese tipo). Lo demás es una diferencia solo contable, sin libro que la explique: NO se asienta y queda registrada como excepción por cuenta (cuenta, rol, tipos y valor), en la observación de la corrida y en las advertencias.

### 2. A2 en modo DIFERENCIA — `POST /rest/prov/semillaDevengoInteres` con `"modo": "DIFERENCIA"`
El default sigue siendo `"SEMILLA"` (sin cambios). En DIFERENCIA graba, con referencia a un asiento y sin asiento propio, **solo la diferencia por cuota** entre el interés devengado y no cobrado al corte (el objetivo) y el libro de devengo (Σ MVIC 11 − 12): un MVIC **11** si falta y un **12** si sobra. Origen `AJUSTE_LIBRO_DEVENGO_INTERES`, `idOrigen` = `MVICASNT` = la referencia, fecha contable = corte.
- Usa el MISMO cálculo que el ajuste de último recurso (`deltasLibroDevengoInteresAlCorte`): una sola función para las dos.
- Idempotente por referencia (si hay filas vigentes con ese origen, no graba y devuelve lo grabado con `yaGrabado = true`), previsualizable con `grabar = false` y anulable con `POST /rest/prov/semillaDevengoInteres/anular` (que ahora anula también este origen).
- La respuesta trae, por tipo de préstamo, `valorAlta` (lo que falta, MVIC 11), `valorBaja` (lo que sobra, MVIC 12), `valor` (neto) y `cuotas`; y `totalAlta`, `totalBaja` y `total` (neto). El asiento de referencia debe existir y estar ACTIVO. No filtra las cuotas «ya alimentadas»: la diferencia es contra el libro entero.

### Prueba (no ejecutada)
1. Con el código desplegado en la copia, sobre la corrida 165 (cierre de diciembre de 2025) con `saa.cartera.ajusteUltimoRecurso` apagada: el control debe traer **una sola cuenta** de la 1.4.99.05 con sus tres tipos, sin diferencias de un millón por tipo.
2. A2 en modo DIFERENCIA al 31-dic-2025 con referencia al asiento D207-2025-12-001 y `grabar = false`: el libro de devengo de interés es 1.131.164,59 y el objetivo 1.155.056,00 (lo que provisionó el ⑦), así que `total` debe ser **+23.891,41** (cuando hay cuotas que sobran, `totalAlta − totalBaja` da ese neto). Con `grabar = true`, repetir la llamada no duplica.
3. Después de grabar: el control de diciembre (corrida 165) **por cuenta en 0** (cuenta 1.4.02, cuenta 1.4.99.05, el grupo y el total), y el libro de interés al 31-dic = **1.155.056,00**.

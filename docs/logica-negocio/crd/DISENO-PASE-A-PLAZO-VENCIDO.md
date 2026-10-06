# DISEÑO — Pase de préstamos EN MORA a DE PLAZO VENCIDO, con orden de cobro y liquidación

**Equipo:** `omen-saa-1` (CRD · equipo B) · **Abierto:** 2026-09-30 · **Estado:** ⛔ DISEÑO, NO DESPACHADO — decisiones D1–D23 tomadas. FASE 1 LISTA PARA DESPACHAR (DDL `sql/247`, contrato `API-PASE-A-PLAZO-VENCIDO.md`). Fase 2 (reporte a la aseguradora) espera validación del usuario.
Faltan las decisiones del §6 antes de escribir el contrato de API y despachar.

---

## 1. El requerimiento, como lo dio el usuario

> *«Se requiere una pantalla para enviar los préstamos en mora a DE PLAZO VENCIDO, esa pantalla debe
> mostrar los préstamos en mora y cambiarles el estado a de plazo vencido revisando los valores y
> generando documentos.»*

Formatos entregados por el usuario (ejemplo real, crédito hipotecario 60123):

1. **Memorando — Orden de cobro** (firma el Jefe de Crédito, dirigido al Representante Legal con copia
   al Contador General). Número `ASOPREP-FCPC-CREDITO-GR-033-2026`. Cuadro de valores + la frase
   *«se declara en estado de plazo vencido por incumplimiento de pago»*. `Adj. Tabla de Amortización`.
2. **Liquidación contable** (firma el Jefe de Contabilidad). Cita el número y la fecha del memorando,
   las cuotas impagas (en número y en letras), el mes de inicio de la mora, y un cuadro de 6 filas con
   corte a una fecha.

### Campos de cada documento

| Campo | Memorando | Liquidación |
|---|---|---|
| Número de memorando, fecha | ✅ | ✅ (citado) |
| Tipo de crédito (Hipotecario…), número de préstamo | ✅ | ✅ |
| Nombre y cédula del partícipe | ✅ | ✅ |
| Fecha inicial / fecha final del préstamo | ✅ | ✅ (otorgado / vencimiento) |
| Última fecha de cobro | ✅ | — |
| Monto del préstamo | ✅ | ✅ |
| Capital cobrado · **saldo capital vencido** | ✅ | ✅ (sólo el saldo) |
| Interés devengado · cobrado · **saldo** | ✅ | ✅ (sólo el saldo, «interés vencido») |
| Desgravamen devengado · cobrado · **saldo** | ✅ | ✅ (sólo el saldo) |
| Seguro (incendio) devengado · cobrado · **saldo** | ✅ | ✅ (sólo el saldo) |
| Interés de mora devengado · cobrado · **saldo** | ✅ | ✅ (sólo el saldo) |
| Total cobrado · **total por cobrar a la fecha de corte** | ✅ | ✅ (total adeudado) |
| Plazo · cuotas cobradas · cuotas pendientes · cuotas pendientes hasta el vencimiento | ✅ | ✅ (sólo las pendientes, con letras) |
| Mes de inicio de la mora | — | ✅ |
| Dividendo mensual | ✅ | — |

### ⚠️ El ejemplo NO se puede tomar como especificación numérica

Verificado sumando sus propias cifras:

- **Cuadra:** 49.786,36 + 14.535,62 + 18.624,53 + 1.591,38 + 0,00 = **84.537,89** ✓.
- **No cuadra:** 66 cuotas cobradas + 88 pendientes = **154**, pero el plazo dice **155**.
- **Incoherente:** «interés mora devengado $0,00» y «cobrado $0,00», pero el **saldo es $14.535,62**.
- **Incoherente:** «TOTAL COBRADO $0,00», cuando el propio cuadro dice que se cobraron $40.232,39 de
  capital y $39.841,92 de interés.

⇒ El sistema calcula **devengado = cobrado + saldo** en cada fila, y el total cobrado como la suma de
los cobrados. Las filas del ejemplo se llenaron a mano. **No se copian.**

### Lectura del caso

La declaración es del **08-06-2026** y el préstamo vencía el **30-06-2026**, es decir, **antes** del fin
natural. «Plazo vencido» aquí es **declarar vencida toda la obligación por incumplimiento**, no «se
acabó el plazo». En el ejemplo quedaba una sola cuota por vencer, así que no se nota si esa cuota
futura entró al saldo vencido o no. **Ver §6, pregunta B.**

---

## 2. Lo que YA existe y lo que NO (verificado contra el código, 2026-09-30)

- **No existe ninguna pantalla de ejemplo** para esto. Se buscó en el menú de créditos, en
  `app.routes.ts`, en las carpetas `crd/forms`, en los textos «plazo vencido» y «en mora», y en el
  historial de git. Lo más parecido y que **no** sirve de base: «Repote Valores Insolutos» (un reporte
  CSV/PDF), el diálogo «Procesos varios» (corre la mora) y «Acuerdo de condonación» (un partícipe, para
  condonar).
- **Ningún código del backend pasa un préstamo a `PRSTIDST = 8`.** Los que hoy están en 8 se pusieron a
  mano o por migración.
- **`EstadoPrestamo.DE_PLAZO_VENCIDO = 8`**, `EN_MORA = 11` (`com.saa.rubros.EstadoPrestamo`).
  ⚠️ El JavaDoc de `PrestamoService`/`PrestamoDaoService` («mora (8), plazo vencido (11)») tiene las
  etiquetas **al revés**. Manda el código de la interfaz de rubros.
- **Cuotas:** en el backend `EstadoCuotaPrestamo` llega hasta `VENCIDA = 8`. **No existe** un estado de
  cuota «de plazo vencido». El frontend (`model/estado-cuota-prestamo.ts:40`) declara
  `DE_PLAZO_VENCIDO = 9` para cuotas, y **ese código no existe en el backend**: desajuste de contrato
  que ya estaba y que este frente no debe agrandar.
- **La fórmula de mora ya es pura y reusable a una fecha elegida:**
  `ProcesoMoraPrestamoService.calcularMoraCuota(cuota, tasaDiaria, fecha)` y
  `tasaDiariaDelPrestamo(prestamo, …)`. Se extrajeron el 2026-08-28 para la precancelación. La
  liquidación a una fecha de corte elegida la usa, **sin persistir**.
- **`ReporteServiceImpl.exportarReporte` sólo soporta PDF, EXCEL y HTML.** Un formato desconocido cae a
  PDF con un WARNING. **No hay DOCX.** JasperReports 7 trae `JRDocxExporter` en el jar principal, y
  `poi-ooxml` ya está en el `pom.xml`.
- **Contabilidad:** la reclasificación de por vencer a vencido ya la hace el **cierre de cartera**
  mensual por bandas (`REGLAS-ASIENTOS-CONTABLES-CRD.md` filas 9 y 11). Decisión D7: no hay asiento
  propio.

### ⛔ El choque con la decisión D6 (la mora)

Hoy **un préstamo en 8 NO genera mora**, y es a propósito:

- `DetallePrestamoDaoServiceImpl.selectPrestamosConCuotasVencidas` limita el universo a
  `idEstado IN (2, 11)`.
- `ProcesoMoraPrestamoServiceImpl.calcularMoraPrestamo:197-213` sale temprano si `PRSTIDST = 8`, sin
  calcular y sin tocar estados.

Viene del incidente del **2026-08-24** (`PROCESO-DIARIO-INTERES-MORA.md` §11): el proceso reclasificó
**todos** los 8 a 11. El arreglo sacó a los 8 **de las dos cosas a la vez**: del cambio de estado **y**
del cálculo de mora. Y `LIMPIEZA-MORA-PLAZO-VENCIDO.md` + `sql/77` se escribieron con la premisa
*«un préstamo de plazo vencido nunca debió tener mora»*. **No hay constancia de que el `77` se haya
corrido.**

**D6 dice lo contrario:** el préstamo en 8 **sigue generando mora**. La lectura que concilia las dos
cosas: el defecto del 24-08 era **el cambio de estado**, no la mora. Lo que hay que hacer es separar lo
que el arreglo juntó: **calcular la mora de los 8, pero nunca tocar su estado**, ni a 11 ni a 2 por
regularización.

⚠️ **Y la consecuencia que no se ve:** la mora se recalcula **desde el vencimiento de cada cuota** (no
es incremental, `calcularMoraPrestamo:228-247`). Si se abre el universo a **todos** los préstamos en 8,
la primera corrida de las 02:00 les pone de golpe la mora completa a los que ya estaban en 8 antes de
esta pantalla, incluida la que el `sql/77` iba a limpiar. Esa mora entra al **archivo Petro**
(`GeneracionArchivoPetroServiceImpl.calcularSaldoCuota` la suma) y a **cualquier cobro manual**. Es
plata cobrada al partícipe. **Ver §6, pregunta A.**

---

## 3. Decisiones del usuario — 2026-09-30. NO re-litigar

| # | Pregunta | Decisión |
|---|---|---|
| D1 | Qué préstamos aparecen en la lista | **Todos** los `EN_MORA` (`PRSTIDST = 11`) |
| D2 | Uno o varios | **Varios a la vez** |
| D3 | Quién lo hace | **Dos pasos, dos personas** (Crédito declara · Contabilidad liquida) |
| D4 | Fecha de corte | **Se puede escoger** |
| D5 | Formato de salida | **Ambos:** PDF y Word |
| D6 | Mora después de pasar a 8 | **Sigue generando mora.** Cuando se vaya a cobrar por la vía legal, se revisa el nuevo valor |
| D7 | Asiento contable propio | **No.** Basta el cierre de cartera |
| D8 | Reversible | **Sí** |
| D9 | Número de memorando | **Lo ingresa el usuario** (no hay secuencia del sistema) |

### Segunda ronda — 2026-09-30 (respuestas a las preguntas A–F del §6)

| # | Pregunta | Decisión |
|---|---|---|
| D10 (A) | Mora de los que **ya estaban** en 8 | **Les cae toda desde su vencimiento.** La primera corrida de las 02:00 después del WAR les carga la mora histórica completa, a sabiendas. ⇒ **El `sql/77` (limpieza de mora de los 8) queda OBSOLETO y NO se corre** |
| D11 (B) | Aceleración | **Se acelera toda la deuda:** el saldo de capital incluye las cuotas no vencidas, **y el interés de esas cuotas también entra** |
| D12 (C) | Tabla `CRD.PLVN` | **Autorizada** por el usuario |
| D13 (C) | Seguro | **Un préstamo de plazo vencido ya no debe tener seguro.** El seguro de esas deudas se reporta a la aseguradora para que emita una **nota de crédito** |
| D14 (D) | Reverso | Vuelve **siempre a `EN_MORA` (11)**. Lo hace el **Jefe de Crédito**. Si Contabilidad ya liquidó, **se reversan los asientos generados** |
| D15 (E) | Firmantes | **Líneas de firma en el reporte** (firman después el PDF). Nada quemado |
| D16 (F) | Memorando | **Uno por préstamo.** El sistema **valida que el número no se repita** |

### Tercera ronda — 2026-09-30

| # | Pregunta | Decisión |
|---|---|---|
| D17 (G) | ¿La liquidación genera asiento? | **NO.** El usuario corrige su D14: *«tienes razón, fue mi error»*. D7 queda firme. ⇒ **El reverso no reversa ningún asiento**: una declaración LIQUIDADA se revierte igual que una DECLARADA (préstamo a 11, declaración a REVERTIDA) |
| D18 (H) | Aceleración | **Sólo en los documentos y en la foto de `PLVN`.** La tabla de amortización **no se toca**: la mora sigue corriendo cuota a cuota con su vencimiento original |
| D19 (I) | Qué seguro deja de cobrarse | **Los dos:** desgravamen e incendio |

### Cuarta ronda — 2026-09-30

| # | Pregunta | Decisión |
|---|---|---|
| D20 (J) | Formato del reporte a la aseguradora | **No existe.** Se sugiere uno (§4.6); el usuario lo valida con la aseguradora antes de congelarlo |
| D21 (K) | PARA / CC del memorando | **Campos en la pantalla del paso 1**, con el último valor usado como sugerencia, y guardados en la foto de `PLVN` para que la reimpresión sea idéntica |

### Quinta ronda — 2026-09-30

| # | Pregunta | Decisión |
|---|---|---|
| D22 (L) | Desde cuándo deja de cobrarse el seguro | **Desde la fecha de corte de la declaración.** Las cuotas con vencimiento **≤ corte** conservan y deben su seguro; las **posteriores** quedan en **0**. Confirma la lectura del árbitro |
| D23 (M) | ¿El seguro vuelve al revertir? | **Sí.** ⇒ **Se autoriza `CRD.DPLV`** para guardar el original por cuota |

| **D25** | **El interés, ¿todo o hasta la fecha?** | **Sólo hasta la fecha de corte** (usuario, 2026-09-30, en producción: *«el valor de intereses sólo lo debe tomar en cuenta hasta la fecha en que se va a mandar de plazo vencido»*). **Corrige la mitad de D11:** el CAPITAL se sigue acelerando completo; el INTERÉS sólo de las cuotas con vencimiento ≤ corte, completas, sin prorrata del período en curso y sin interés futuro. Es la regla de `ProcesoPagoPrestamoServiceImpl.calcularPrecancelacion` (exigibles = vencimiento ≤ fin del día del corte; interés de las futuras, condonado). Árbitro: se eligió «sin prorrata» por ese precedente |
| **D26** | **Formato del número de memorando** | El usuario escribe **sólo el número** (p. ej. `46`); el sistema lo compone como **`ASOPREP-FCPC-CREDITO-GR-046-2026`**: el número con ceros a la izquierda hasta 3 dígitos (uno de 4 o más dígitos va tal cual) y el **año en curso al declarar**. La unicidad (D16) se valida sobre el número COMPUESTO: el 46 de 2026 y el de 2027 son memorandos distintos. Las declaraciones grabadas antes con el número pelado se corrigen con `crd/sql/300`. (usuario, 2026-09-30) |
| **D27** | **¿Los de plazo vencido entran al cierre de cartera?** | **SÍ** (usuario, 2026-09-30), tras el hallazgo H80: el cierre sólo toma `PRSTIDST IN (2, 11)` y el G48 a la Superintendencia sí incluye el 8, así que contabilidad y G48 no cuadraban. Esto **corrige D7**, que suponía que «basta el cierre». ⚠️ La **transición** pide decisión del contador: los declarados con la pantalla (grupo A) ya se abrieron como 11 en la apertura de septiembre, y los históricos en 8 (grupo B) nunca entraron al cierre. Medición: `sql/304` |
| D24 | ¿Sale sin permisos por rol? | **Sí** (2026-09-30). Sin `idPermiso`; la separación Crédito/Contabilidad queda como deuda del frente de seguridad |

⇒ **Diseño cerrado para la fase 1.** DDL: `sql/247`. Contrato: `API-PASE-A-PLAZO-VENCIDO.md`.

### ⚠️ La tensión que abre D19 frente a D18

D18 dice que la tabla no se toca, pero para que el seguro **deje de cobrarse** (D13/D19) hay que
escribir **cero** en `desgravamen` y `valorSeguroIncendio` de las cuotas futuras. Si no, el archivo
Petro y cualquier cobro lo siguen sumando. Es la **única** escritura sobre las cuotas que hace este
frente, y es sobre el seguro, no sobre las fechas ni el capital: D18 se sigue cumpliendo en lo que
protege (el vencimiento y la mora).

El reverso (D14) tiene que poder **devolver** esos valores, y **no hay dónde guardarlos**: `DTPRDSOR`
(`desgravamenOriginal`) ya tiene dueño (`AbonoCapitalPrestamoServiceImpl:763`, `PrestamoServiceImpl:535`,
`:732`) y reusarlo rompería el abono a capital. ⇒ Hace falta una **tabla de detalle** con el seguro
original de cada cuota tocada: nombre propuesto **`CRD.DPLV`** (libre en el modelo, en `docs/` y en el
registro, verificado el 2026-09-30). **Ver §6.1, preguntas L y M.**

---

## 4. Diseño propuesto (sujeto al §6)

### 4.1 Flujo

```
EN_MORA (11) ──[Paso 1: Crédito declara, lote]──▶ DE_PLAZO_VENCIDO (8)   + declaración DECLARADA
                                                         │                   + Memorando (PDF/DOCX)
                                                         ▼
                                      [Paso 2: Contabilidad liquida]  → declaración LIQUIDADA
                                                                          + Liquidación (PDF/DOCX)
DE_PLAZO_VENCIDO (8) ──[Reverso]──▶ EN_MORA (11)   + declaración REVERTIDA (no se borra)
```

- **Paso 1 (Jefe de Crédito):** lista de préstamos en 11 con sus valores calculados a la fecha de
  corte elegida, selección múltiple, número de memorando ingresado, confirmación. Por cada préstamo:
  `PRSTIDST` 11 → 8, y se graba una **declaración** con la **foto** de los valores a esa fecha.
- **Paso 2 (Contabilidad):** bandeja de declaraciones DECLARADAS. Confirma (con su propia fecha de
  corte, que por defecto es la del memorando) y emite la liquidación.
- **La foto es obligatoria, no una optimización.** Con D6 la mora sigue creciendo: si el documento se
  regenerara leyendo las cuotas en vivo, reimprimir el memorando del 8 de junio en agosto daría otro
  total **con la misma fecha impresa**. El documento se imprime desde la declaración, no desde las
  cuotas.
- **Todo o nada por lote**, en una sola transacción. Mismo criterio que la devolución a beneficiarios
  (`51584844`): un lote a medias con un contador de errores es lo que dejó a ocho jubilados sin su
  seguro (H78). Si un préstamo del lote ya no está en 11 al confirmar (lo tocó el proceso de mora o un
  cobro), se rechaza el lote entero con la lista de los que cambiaron.

### 4.2 Tabla nueva — `CRD.PLVN` (declaración de plazo vencido)

⛔ **Nombre propuesto, NO autorizado.** Libre en `src/main/java/com/saa/model`, en `docs/` y en el
registro (verificado 2026-09-30). Falta el control contra `ALL_TABLES` (bloque 0 del DDL) y **la
autorización del usuario** para crear la tabla (registro §3: reservar ≠ autorizar).

Una fila por préstamo declarado. Columnas (8 caracteres, `PLVN` + 4):

| Columna | Tipo | Qué |
|---|---|---|
| `PLVNCDGO` | NUMBER PK | secuencia `CRD.SQ_PLVNCDGO` |
| `PRSTCDGO` | NUMBER FK | préstamo |
| `PLVNNMMM` | VARCHAR2(60) | número de memorando (D9, ingresado) |
| `PLVNFCMM` | DATE | fecha del memorando = fecha de corte del paso 1 |
| `PLVNESTD` | NUMBER | 1 DECLARADA · 2 LIQUIDADA · 3 REVERTIDA (constantes planas, sin rubro) |
| `PLVNESAN` | NUMBER | estado del préstamo **antes** (para el reverso) |
| `PLVNMNTO`, `PLVNCPCB`, `PLVNSLCP`, `PLVNINDV`, `PLVNINCB`, `PLVNSLIN`, `PLVNDSDV`, `PLVNDSCB`, `PLVNSLDS`, `PLVNSGDV`, `PLVNSGCB`, `PLVNSLSG`, `PLVNMRDV`, `PLVNMRCB`, `PLVNSLMR`, `PLVNTTCB`, `PLVNTTPC` | NUMBER(18,2) | la foto del cuadro (monto, capital cobrado/saldo, interés devengado/cobrado/saldo, desgravamen, seguro, mora, totales) |
| `PLVNCTPL`, `PLVNCTCB`, `PLVNCTPN`, `PLVNCTXV` | NUMBER | plazo, cuotas cobradas, pendientes, por vencer |
| `PLVNFUCB`, `PLVNFIMR` | DATE | última fecha de cobro, inicio de la mora |
| `PLVNDVMN` | NUMBER(18,2) | dividendo mensual |
| `PLVNUSDC`, `PLVNFCDC` | VARCHAR2 / TIMESTAMP | quién y cuándo declaró |
| `PLVNFCLQ`, `PLVNSLMQ`, `PLVNTTLQ`, `PLVNUSLQ`, `PLVNFCRL` | DATE / NUMBER / VARCHAR2 / TIMESTAMP | liquidación: corte, mora y total a ese corte, usuario, fecha de registro |
| `PLVNUSRV`, `PLVNFCRV`, `PLVNMTRV` | VARCHAR2 / TIMESTAMP / VARCHAR2(500) | reverso: quién, cuándo, motivo |

Los nombres exactos de columna se fijan en el DDL. Esta tabla es la **intención**, no el script.

### 4.3 Mora (D6) — el cambio en el proceso diario

- `selectPrestamosConCuotasVencidas`: el universo vuelve a incluir el 8 → `IN (2, 8, 11)`.
- `calcularMoraPrestamo`: la guarda del 8 **deja de salir temprano**. Calcula y persiste la mora de las
  cuotas, pero **no toca el estado del préstamo** (ni a 11 ni la regularización a 2).
- **El estado de las cuotas** de un préstamo en 8 también tiene que decidirse: hoy la cuota vencida pasa
  a `EN_MORA(5)`. Propuesta: se mantiene igual, porque no hay estado de cuota «plazo vencido» en el
  backend y crearlo es otro frente.
- La **prueba 8** de `PROCESO-DIARIO-INTERES-MORA.md` §10 (el 8 no se reclasifica) sigue siendo
  obligatoria. Se agrega su par: el 8 **sí** acumula mora.
- ⚠️ `PROCESO-DIARIO-INTERES-MORA.md` §11 y `LIMPIEZA-MORA-PLAZO-VENCIDO.md` quedan **superados en la
  parte de la mora**, no en la del estado. Se anotan en el mismo cambio.

### 4.4 Documentos (D5)

- Dos `.jrxml` nuevos en `rep/crd/`, en **sintaxis compacta**, con `.jasper` compilado **y fill
  verificado** (`CLAUDE.md`, sección de reportes). Se llenan con los parámetros de la declaración, no
  con una query sobre las cuotas.
- **DOCX:** agregar `case "DOCX"` con `JRDocxExporter` en `ReporteServiceImpl.exportarReporte`. Es
  aditivo (PDF, EXCEL y HTML no cambian), pero **`ReporteServiceImpl` es transversal a todos los
  módulos**: se avisa a los otros árbitros, con autorización del usuario.
- Tabla de amortización adjunta: reusar un reporte de tabla existente de `rep/crd/` (a elegir al
  despachar).
- Firmantes, cargos y destinatarios: **no van quemados**. Ver §6, pregunta E.

### 4.4bis ⛔ Reglas de cálculo — exigencia del usuario: *«el sistema debe realizar bien los cálculos»*

Nada de este cuadro se deduce al implementar: cada fila tiene su fórmula escrita acá, y el ejecutor
**mide antes de usar un campo** (regla de la casa: ante un campo ambiguo, medir antes que deducir).
Universo: **todas las cuotas del préstamo** (`CRD.DTPR`), incluidas las futuras (D11), excluidas las
`CANCELADA_ANTICIPADA (7)`. `corte` = la fecha elegida (D4).

| Fila | Devengado | Cobrado | Saldo |
|---|---|---|---|
| Capital | `montoSolicitado` del préstamo ⚠️ confirmar que es «monto del préstamo» y no `montoLiquidacion` | Σ `capitalPagado` | Σ (`capital` − `capitalPagado`) |
| Interés | Σ `interes` (**todas**, D11) | Σ `interesPagado` | devengado − cobrado |
| Desgravamen | Σ `desgravamen` de las cuotas **con vencimiento ≤ corte** (pendiente de la pregunta I) | Σ `desgravamenPagado` | devengado − cobrado |
| Seguro incendio | Σ `valorSeguroIncendio` (mismo criterio que desgravamen) | ⚠️ **no existe campo «pagado» en `DTPR`**: medir de dónde sale antes de implementar (ver H22, el seguro de incendio que se pierde en las cascadas) | devengado − cobrado |
| Mora | Σ `calcularMoraCuota(cuota, tasaDiaria, corte)` sobre las cuotas vencidas e impagas **a la fecha de corte** — ⛔ **NUNCA** el `mora` persistido, que está calculado a la última corrida nocturna, no al corte | Σ `moraPagado` | devengado − cobrado |
| **Totales** | — | Σ de los cobrados | Σ de los saldos = **total por cobrar** |

| Dato | Regla |
|---|---|
| Plazo | `Prestamo.plazo` ⚠️ contrastar con el número de cuotas en `DTPR`; si difieren, se informa, no se elige en silencio |
| Cuotas cobradas | cuotas en `PAGADA (4)` |
| Cuotas pendientes | el resto (una `PARCIAL` es pendiente) |
| Cuotas por vencer | pendientes con `fechaVencimiento > corte` |
| Inicio de la mora | la `fechaVencimiento` más antigua entre las cuotas vencidas e impagas |
| Última fecha de cobro | máx. `fechaPagado` |
| Dividendo mensual | `Prestamo.valorCuota` ⚠️ contrastar con la cuota típica de la tabla |

⛔ **Corrección 2026-09-30, antes de despachar:** la columna «Cobrado» de la tabla de arriba NO sale de
las columnas `*Pagado` de `DTPR`, sino de los **pagos válidos de `CRD.PGPR`**: la misma fuente que
`MotorPagoPrestamoServiceImpl.calcularSaldosCuota` y el frontend. En créditos migrados las columnas de
`DTPR` no son confiables, y el seguro de incendio pagado sólo existe en PGPR. Manda el contrato §2.

**Invariantes que el backend verifica ANTES de declarar un préstamo.** Si alguna no se cumple, ese
préstamo **no se declara** y el lote entero se rechaza con el motivo de cada uno. No se emite un
documento con números que no cuadran:

1. En cada fila: devengado = cobrado + saldo (al centavo).
2. Σ saldos = total por cobrar; Σ cobrados = total cobrado.
3. Saldo de capital = monto − capital cobrado (si falla, la tabla del préstamo tiene un problema previo,
   y eso hay que verlo antes de mandarlo a legal).
4. Cuotas cobradas + pendientes = número de cuotas de la tabla.
5. Ningún saldo negativo.

**Verificación independiente (regla 11 del árbitro):** el árbitro escribe un `.sql` de control que
recalcula el cuadro **directamente desde `CRD.DTPR`**, sin pasar por el código, y lo compara fila por
fila contra la foto de `CRD.PLVN`. La mora del SQL se contrasta aparte, porque la fórmula vive en Java.
El frente no se da por bueno hasta que ese contraste salga en cero diferencias sobre préstamos reales.

### 4.6 Reporte a la aseguradora (D13, D20) — FORMATO SUGERIDO, sin validar

**Fase 2**: no bloquea el paso 1 ni el 2. Se despacha después de que el usuario lo valide con la
aseguradora. En el sistema **no existe ninguna entidad «aseguradora»** (el frente de seguros por póliza
tiene cero construido), así que el reporte se emite **por tipo de seguro**: desgravamen e incendio
pueden ser de aseguradoras distintas, y cada una recibe sólo lo suyo.

**Parámetros:** tipo de seguro (desgravamen | incendio) · rango de fechas de declaración.
**Universo:** declaraciones de `PLVN` en ese rango, en estado DECLARADA o LIQUIDADA (las REVERTIDAS no
entran; si una se revierte después de reportada, sale en el reporte siguiente con signo contrario,
según lo que se decida en la pregunta M).

| Columna | Origen |
|---|---|
| Nro. | secuencial del reporte |
| Asegurado — apellidos y nombres | partícipe |
| Cédula | partícipe |
| Nro. de préstamo · tipo de crédito | préstamo / producto |
| Fecha de otorgamiento · monto original | préstamo |
| Nro. de memorando · fecha de declaración | `PLVN` |
| Saldo de capital a la declaración | foto de `PLVN` (capital asegurado) |
| Cuotas con seguro anulado | cantidad y rango (primera a última fecha de vencimiento) |
| **Prima anulada** | Σ del seguro original de esas cuotas (desgravamen o incendio, según el parámetro) |

**Pie:** total de préstamos, total de capital y **total de prima a acreditar**. Líneas de firma
(Jefe de Crédito, Contador), como D15. PDF y Word, como D5.

⚠️ **Lo que sólo la aseguradora puede decir, y hay que preguntarle:** si la nota de crédito se calcula
sobre la **prima por cuota** que el sistema anula (lo que muestra este formato) o sobre la **prima que el
fondo ya le pagó** por esos asegurados desde la declaración. Si es lo segundo, el valor sale de lo que
tesorería pagó, no de las cuotas, y el reporte cambia.

### 4.5 Reverso (D8)

`PRSTIDST` 8 → **siempre `EN_MORA` (11)** (D14; `PLVNESAN` queda sólo como registro). Lo hace el Jefe de Crédito. No hay asiento que reversar (D17), aunque la declaración ya esté LIQUIDADA. La declaración queda
REVERTIDA con usuario, fecha y motivo; **nunca se borra**. El proceso de mora decide después, como con
cualquier préstamo en 11.

---

## 5. Hallazgos al levantar esto

0. **El «No. 60123» no es `PRSTCDGO`:** es `Prestamo.idAsoprep` (`PRSTIDAS`), nullable, con respaldo en `codigo` — la convención de todas las pantallas. Lo midió el ejecutor BE antes de programar (ítem 0.a). Se agregó `PLVNNMPS` al `sql/247` (55 columnas), antes de correrlo.

1. **No había pantalla de ejemplo**, y ningún código escribe el estado 8.
2. **El arreglo del 24-08 juntó dos cosas en una:** sacó a los 8 del cambio de estado (correcto) y del
   cálculo de mora (que D6 ahora contradice). Desde afuera parecía una sola regla.
3. **Estado de cuota 9 «de plazo vencido»** existe en el frontend y no en el backend.
4. **El JavaDoc de `PrestamoService`/`PrestamoDaoService` tiene 8 y 11 al revés.**
5. **El formato de ejemplo tiene tres incoherencias numéricas** (§1): no se copian.
6. **El reporte no exporta Word.**

---

## 6. ⛔ Lo que falta decidir antes de despachar

### 6.0 Abiertas después de la tercera ronda (G, H e I ya contestadas: D17–D19)

| # | Pregunta | Por qué importa |
|---|---|---|
| **L** | **Desde cuándo deja de cobrarse el seguro.** Lectura del árbitro, que el usuario no ha confirmado explícitamente: el seguro de las cuotas **con vencimiento ≤ fecha de corte** se sigue debiendo (el ejemplo lo cobra: «saldo desgravamen 1.591,38»), y el de las cuotas **posteriores** se pone en cero | Es una escritura en la tabla de cuotas que afecta el archivo Petro y los cobros |
| **M** | **Al revertir, ¿el seguro de las cuotas futuras vuelve?** Si vuelve, se autoriza la tabla de detalle `CRD.DPLV` para guardar el original. Si no vuelve (ya se pidió la nota de crédito a la aseguradora), no hace falta la tabla, pero el préstamo revertido queda sin seguro | Define si hace falta una segunda tabla nueva |
| ~~J~~ | CONTESTADA (D20): se sugiere el formato del §4.6 | — |
| ~~K~~ | CONTESTADA (D21) | — |

### 6.1 Abiertas después de la segunda ronda — G, H e I CONTESTADAS (D17–D19); J y K pasan al §6.0

| # | Pregunta | Por qué importa |
|---|---|---|
| **G** | **D14 contra D7.** D7 dice que la liquidación **no** genera asiento propio (basta el cierre de cartera). D14 dice que al revertir una declaración liquidada **se reversan los asientos generados**. ¿Qué asientos? Si la liquidación no genera ninguno, no hay nada que reversar; los del cierre de cartera son mensuales por bandas y no por préstamo. **¿La liquidación SÍ debe generar un asiento** (y cuál: reclasificación a cartera vencida, cuentas de orden, otro)? | Cambia el paso 2 entero y pide una plantilla contable, que se reserva en `CNT.PLNS` |
| **H** | **Aceleración: ¿sólo en los documentos o también en la tabla de amortización?** Recomendación del árbitro: **sólo en la foto y los documentos**, sin tocar las cuotas. Tocarlas (vencer todas a la fecha de corte) hace que la mora corra sobre el capital futuro desde la declaración, pero destruye la tabla original y hace el reverso (D14) casi imposible de dejar exacto | Con la tabla intacta, la mora sigue corriendo cuota a cuota según su vencimiento original |
| **I** | **Seguro (D13): ¿cuál y desde cuándo?** ¿Desgravamen, incendio o los dos? ¿Deja de cobrarse **desde la fecha de declaración** (las cuotas futuras quedan sin seguro) o desde que empezó la mora? Lectura del árbitro: el seguro de las cuotas **ya vencidas** e impagas se sigue debiendo (el ejemplo lo cobra: «saldo desgravamen 1.591,38»), y el de las **futuras** se anula | Define el valor de «seguro» en la foto y qué se escribe en las cuotas futuras. El desgravamen entra al archivo Petro y a los cobros |
| **J** | **El reporte a la aseguradora para la nota de crédito:** ¿qué formato, qué columnas, por qué período? ¿Hay un ejemplo? | Es un tercer documento. Sin formato no se despacha |
| **K** | **Encabezado del memorando** (PARA: Representante Legal, CC: Contador General): ¿los nombres se escriben en la pantalla del paso 1 y se guardan en la foto? Propuesta: campos editables con el último valor usado; las **firmas** van en líneas, como dice D15 | Los nombres cambian cuando cambian las personas; no pueden ir quemados |

### 6.2 Primera ronda — CONTESTADAS (ver D10–D16), se conserva el texto

| # | Pregunta | Por qué importa |
|---|---|---|
| **A** | La mora de los préstamos que **ya estaban en 8** antes de esta pantalla: ¿empiezan a generar mora desde su vencimiento (de golpe en la primera corrida), desde la fecha en que se active el cambio, o sólo los que pasen por la pantalla nueva? ¿Y el `sql/77` se corrió? | Es plata que entra al archivo Petro y a los cobros. La fórmula recalcula desde el vencimiento: sin una regla, la primera noche les carga toda la mora histórica |
| **B** | Al declarar plazo vencido, ¿el **saldo capital vencido** incluye el capital de las cuotas que todavía no vencen (se acelera la deuda)? ¿Y el interés de esas cuotas futuras entra o no? | Cambia el total del memorando en cualquier préstamo con cuotas futuras. El ejemplo no lo resuelve (sólo quedaba una) |
| **C** | ¿Autorizas crear la tabla `CRD.PLVN`? | Sin ella no hay foto, y los documentos no son reproducibles (§4.1) |
| **D** | El reverso: ¿vuelve siempre a `EN_MORA`, o al estado que tenía? ¿Quién puede revertir: Crédito, Contabilidad o los dos? ¿Se puede revertir después de liquidado? | Define las guardas del endpoint |
| **E** | Firmantes: ¿de dónde salen los nombres y cargos (Representante Legal, Contador/Jefe de Contabilidad, Jefe de Crédito)? ¿Del usuario que ejecuta cada paso, de un parámetro, o se escriben en la pantalla? | No pueden quedar quemados en la plantilla |
| **F** | El número de memorando: ¿se valida que no se repita? ¿Todo el lote lleva un solo memorando o uno por préstamo? | En el ejemplo es uno por préstamo |

## D27 — decisión del usuario (2026-10-05)
- El capital de los préstamos DE PLAZO VENCIDO (8), grupo A y grupo B, **está en las mismas cuentas de capital**
  que el resto de la cartera.
- **Para la contabilidad, el 8 se trata IGUAL que EN MORA (11):** entra a todos los subprocesos del cierre, con la
  misma clasificación por banda según los días vencidos.
- Se aplica desde el cierre de septiembre / apertura de octubre de 2026. La transición se mide antes de programar:
  el ③ de la apertura de septiembre incluyó al grupo A como 11 y no incluyó al grupo B, así que el ⑥ y el ② de este
  cierre no deben dejar residuo ni duplicar capital.
- **Interés futuro de los 105 históricos sin declaración (grupo B), decisión del usuario del 2026-10-05: opción a).**
  Se tratan igual que mora: el interés de sus cuotas se devenga y se provisiona a medida que vencen. La
  exclusión de D25 aplica **solo** a los préstamos con una declaración viva en `CRD.PLVN` (grupo A, 152).
  Medición en `sql/315`: el grupo B tiene 351.699,55 de interés futuro, de los cuales 5.963,61 vencen en
  octubre de 2026.

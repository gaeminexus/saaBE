# DISEÑO — Pase de préstamos EN MORA a DE PLAZO VENCIDO, con orden de cobro y liquidación

**Equipo:** `omen-saa-1` (CRD · equipo B) · **Abierto:** 2026-09-30 · **Estado:** ⛔ DISEÑO, NO DESPACHADO.
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

### 4.5 Reverso (D8)

`PRSTIDST` 8 → el estado anterior guardado (`PLVNESAN`, normalmente 11). La declaración queda
REVERTIDA con usuario, fecha y motivo; **nunca se borra**. El proceso de mora decide después, como con
cualquier préstamo en 11.

---

## 5. Hallazgos al levantar esto

1. **No había pantalla de ejemplo**, y ningún código escribe el estado 8.
2. **El arreglo del 24-08 juntó dos cosas en una:** sacó a los 8 del cambio de estado (correcto) y del
   cálculo de mora (que D6 ahora contradice). Desde afuera parecía una sola regla.
3. **Estado de cuota 9 «de plazo vencido»** existe en el frontend y no en el backend.
4. **El JavaDoc de `PrestamoService`/`PrestamoDaoService` tiene 8 y 11 al revés.**
5. **El formato de ejemplo tiene tres incoherencias numéricas** (§1): no se copian.
6. **El reporte no exporta Word.**

---

## 6. ⛔ Lo que falta decidir antes de despachar

| # | Pregunta | Por qué importa |
|---|---|---|
| **A** | La mora de los préstamos que **ya estaban en 8** antes de esta pantalla: ¿empiezan a generar mora desde su vencimiento (de golpe en la primera corrida), desde la fecha en que se active el cambio, o sólo los que pasen por la pantalla nueva? ¿Y el `sql/77` se corrió? | Es plata que entra al archivo Petro y a los cobros. La fórmula recalcula desde el vencimiento: sin una regla, la primera noche les carga toda la mora histórica |
| **B** | Al declarar plazo vencido, ¿el **saldo capital vencido** incluye el capital de las cuotas que todavía no vencen (se acelera la deuda)? ¿Y el interés de esas cuotas futuras entra o no? | Cambia el total del memorando en cualquier préstamo con cuotas futuras. El ejemplo no lo resuelve (sólo quedaba una) |
| **C** | ¿Autorizas crear la tabla `CRD.PLVN`? | Sin ella no hay foto, y los documentos no son reproducibles (§4.1) |
| **D** | El reverso: ¿vuelve siempre a `EN_MORA`, o al estado que tenía? ¿Quién puede revertir: Crédito, Contabilidad o los dos? ¿Se puede revertir después de liquidado? | Define las guardas del endpoint |
| **E** | Firmantes: ¿de dónde salen los nombres y cargos (Representante Legal, Contador/Jefe de Contabilidad, Jefe de Crédito)? ¿Del usuario que ejecuta cada paso, de un parámetro, o se escriben en la pantalla? | No pueden quedar quemados en la plantilla |
| **F** | El número de memorando: ¿se valida que no se repita? ¿Todo el lote lleva un solo memorando o uno por préstamo? | En el ejemplo es uno por préstamo |

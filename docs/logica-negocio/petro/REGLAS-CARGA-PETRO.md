# REGLAS DE CARGA — ARCHIVO DE DESCUENTOS PETROCOMERCIAL

**Documento consolidado y verificado contra el código fuente al 2026-08-13.**
Contexto general, catálogos y endpoints: ver [REGLAS-GENERALES-PETRO.md](REGLAS-GENERALES-PETRO.md).
Fuente de verdad: `com.saa.ejb.asoprep.serviceImpl.CargaArchivoPetroServiceImpl` (`@Stateful`).

El proceso tiene tres fases:

```
FASE 1  validarArchivoPetro / procesarArchivoPetro   parseo + validación de partícipes + persistencia
FASE 2  ejecutarValidacionesFase2 (misma llamada)    validaciones de producto/préstamo/cuota → NVPC
        (el usuario revisa novedades y registra afectaciones manuales AVPC si hace falta)
FASE 3  aplicarPagosArchivoPetro/{idCarga}           aplicación de pagos a préstamos y aportes
```

---

## 1. FASE 1 — Parseo y validación inicial

### 1.1 Validaciones del archivo
1. Extensión `.txt` obligatoria.
2. Archivo no vacío.
3. Lectura en **ISO-8859-1**.
4. El contenido debe **empezar con `EP`** (formato Petrocomercial).
5. Debe producir al menos un registro parseado y al menos un producto agrupado.

### 1.2 Estructura del TXT de respuesta (parser `procesarContenido`)
- El archivo son **bloques por producto**: línea que empieza con `EP` → se saltan 8 líneas →
  línea de producto (`substring(0,4)` = código, resto = descripción) → se saltan 2 líneas →
  registros de partícipes hasta el próximo `EP`.
- Cada línea de registro (mínimo 50 caracteres; el parser rellena con espacios hasta 198):

| Posición (0-based) | Campo → `ParticipeXCargaArchivo` |
|---|---|
| 0–7 | `codigoPetro` (ROL; si viene vacío la línea se descarta) |
| 7–44 | `nombre` (35 útiles) |
| 44–50 | `plazoInicial` |
| 50–61 | `saldoActual` |
| 61–65 | `mesesPlazo` |
| 65–70 | `interesAnual` |
| 70–80 | `valorSeguro` |
| 80–95 | `montoDescontar` |
| 95–110 | `capitalDescontado` |
| 110–125 | `interesDescontado` |
| 125–140 | `seguroDescontado` (**= desgravamen pagado**) |
| 140–155 | `totalDescontado` |
| 155–170 | `capitalNoDescontado` |
| 170–184 | `interesNoDescontado` |
| 184–198 | `desgravamenNoDescontado` |

- Se agrupa por código de producto en `DetalleCargaArchivo` acumulando todos los totales, y se
  calculan los totales generales de `CargaArchivo`.

### 1.3 Persistencia (transaccional; el archivo físico va al final)
1. `CargaArchivo` (exige `filial` y `usuarioCarga` del frontend; `fechaCarga = now`).
2. Un `DetalleCargaArchivo` por producto.
3. Un `ParticipeXCargaArchivo` por línea. **Todo o nada (§3.1b, corregido 2026-08-29):** si el
   INSERT de una línea falla, se aborta toda la carga. Antes se registraba el error y se
   continuaba con la siguiente — ese partícipe quedaba invisible para el resto del proceso, sin
   dejar ningún rastro (ver §3.1b, es el caso más grave de todo el barrido).
4. Al final, el TXT se sube vía `FileService` a `aportes/{añoAfectacion}/{mesAfectacion}/` y se
   guarda `rutaArchivo`.

### 1.4 Validación de partícipe (solo préstamos; `AH` y `HS` se marcan `OK` directo)
```
Entidad por rolPetroComercial = codigoPetro:
  >1  → CODIGO_ROL_DUPLICADO (2)
  0   → buscar por nombre (primeros 35 chars de razonSocial):
          0  → PARTICIPE_NO_ENCONTRADO (1)
          >1 → NOMBRE_ENTIDAD_DUPLICADO (3)
          1  → se ACTUALIZA Entidad.rolPetroComercial = codigoPetro y queda OK (0)
  1   → si razonSocial(35) ≠ nombre → CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE (4); si no → OK (0)
```
- Si quedó OK y **todos** los valores financieros son cero → `VALORES_CERO (8)`.
- Novedad financiera (`novedadesFinancieras`, solo préstamos): si hay algún `*NoDescontado > 0`,
  entonces `SIN_DESCUENTOS (5)` si `totalDescontado == 0`, o `DESCUENTOS_INCOMPLETOS (6)` si > 0.

Estas novedades de fase 1 van en los campos `PXCA.novedadesCarga` / `PXCA.novedadesFinancieras`.

## 2. FASE 2 — Validaciones avanzadas (después de insertar TODO)

Se ejecuta al final de la persistencia, para que al validar un PH/PP el registro HS ya exista en BD.
Se excluye únicamente el producto `HS`. Las novedades de esta fase se registran como filas en
**`CRD.NVPC`** (N por partícipe), con producto/préstamo relacionados, `montoEsperado`,
`montoRecibido` y `montoDiferencia = recibido − esperado` (con signo).

### 2.1 Producto `AH` → `validarAporteAH`
1. Entidad por rol; si no existe → `HISTORIAL_SUELDO_NO_ENCONTRADO (18)`.
2. `HistorialSueldo` activo (**estado 99**) vía `selectByEntidadYEstadoActivo`; si no hay → (18).
3. `montoEsperado = montoJubilacion + montoCesantia`.
4. `montoArchivo == 0` → `APORTE_VALORES_CERO (21)`.
5. `|esperado − archivo| > $1` → `APORTE_MONTO_INCONSISTENTE (22)`.
6. `0.01 < dif ≤ $1` → `APORTE_DIFERENCIA_MENOR_UN_DOLAR (23)`.

### 2.2 Préstamos
1. Productos por `codigoPetro`; ninguno → `PRODUCTO_NO_MAPEADO (9)`.
2. Entidad por rol; nada → `PRESTAMO_NO_ENCONTRADO (10)`.
3. Préstamos activos por entidad+producto (todos los productos que compartan el código);
   ninguno → `PRESTAMO_NO_ENCONTRADO (10)`.
4. **PH/PP**: se busca el registro `HS` del mismo partícipe en la misma carga y
   `montoAValidar = monto PH/PP + monto HS`.
5. `montoAValidar == 0` → si hay cuota pendiente del mes → `SIN_DESCUENTOS (5)` ("pasará a mora");
   si no se encontró → también `SIN_DESCUENTOS` genérica.
6. Búsqueda de cuota por préstamo: primero la cuota **del mes/año de afectación** en estado
   ≠ PAGADA/CANCELADA_ANTICIPADA; si no hay y el préstamo está en GENERADO, VIGENTE,
   DE_PLAZO_VENCIDO o EN_MORA, se toma la **mínima cuota pendiente** y se marca
   `CUOTA_FECHA_DIFERENTE (16)`.
7. Comparación de montos (`cuota.total` vs `montoAValidar`):
   - dif ≤ $1 → cuota aceptada; si `0.01 < dif ≤ $1` además `DIFERENCIA_MENOR_UN_DOLAR (17)`.
   - ninguna cuota coincide → `CUOTA_NO_ENCONTRADA (12)` si no se halló cuota alguna, o
     `MONTO_INCONSISTENTE (13)` con esperado/recibido si la suma de cuotas difiere > $1.

## 3. FASE 3 — Aplicación de pagos (`aplicarPagosArchivoPetro`)

### 3.1 Precondiciones (cualquiera falla → no se toca NADA)
0. **No reproceso** (agregado 2026-08-27, Fase 1 del plan de devengo de aportes): si
   `cargaArchivo.getEstado() == 3` (ya procesada) → `IncomeException` inmediata, antes de
   cualquier otra validación. Antes de este control, la ÚLTIMA carga procesada se podía
   volver a correr sin problema: `validarOrdenProcesamiento` (`:2951`) excluye
   explícitamente a la propia carga de la comparación de orden ("no validar contra sí
   misma"), así que el orden cronológico por sí solo no detectaba el reproceso. Correrla de
   nuevo duplicaba aportes y pagos de préstamos con el mismo `PGAP`/transferencia real.
0b. **Cobro confirmado por contabilidad** (agregado 2026-08-28, regla 11 de §5 de
   `LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md`): si
   `cargaArchivo.getFechaAutorizacionContabilidad() == null` (`CRARFCAC`) →
   `IncomeException` inmediata. El cobro de Petro se contabiliza en dos pasos — paso 1,
   contabilidad confirma a mano que el dinero entró al banco (asiento TRANSITORIO,
   `CobroPetroContableService.confirmarRecepcion`); paso 2, este método reparte esa
   transitoria y aplica a las cuentas reales. Sin el paso 1 hecho, el asiento de reparto
   saldría antes del que lo alimenta y la transitoria quedaría en negativo. Ver §6.
1. **Orden cronológico**: solo se puede procesar la carga del **mes consecutivo siguiente** a la
   última carga con estado 3 (`validarOrdenProcesamiento`). La primera carga no tiene restricción.
2. **Todo valor descontado debe tener destino** (`validarValoresConDestino`): un registro bloquea si
   - tiene `totalDescontado > $0.01`, y
   - tiene alguna novedad de la lista `NOVEDADES_REQUIEREN_AFECTACION_MANUAL`
     (1,2,3,4,7,9,10,11,12,13,18,19,20,22 — las que impiden saber dónde aplicar el dinero), y
   - las afectaciones AVPC registradas (solo las que tienen cuota asociada) no cubren el total
     descontado (faltante > $1).
   Si hay bloqueados se lanza `IncomeException` listando hasta 20 y **no se procesa nada** de la
   carga. Preview sin procesar: `GET /rest/asgn/valoresSinDestino/{idCarga}`.
   NO bloquean: SIN_DESCUENTOS, VALORES_CERO, APORTE_VALORES_CERO, DESCUENTOS_INCOMPLETOS,
   DIFERENCIA_MENOR_UN_DOLAR (y su par de aportes), CUOTA_FECHA_DIFERENTE, ni las de resultado.
3. `tieneNovedadesBloqueantes()` retorna **siempre false**: las novedades son informativas; el
   control real es el punto 2.

### 3.1b TODO O NADA (decisión del usuario, 2026-08-29) — ausencia de dato vs. fallo real

Hasta el 2026-08-28, un error al procesar UN partícipe se atrapaba, se contaba en
`totalErrores` y el bucle seguía con el siguiente — pero la transacción del contenedor ya
había quedado `STATUS_MARKED_ROLLBACK`: el resultado no era "todo" ni "nada", sino un commit
final que fallaba con un error indescifrable apuntando a una consulta inocente muy posterior a
la causa real. Peor: en varios puntos el catch no solo tragaba el error, sino que dejaba
escrituras a medias (una cuota ya marcada PAGADA sin su `PagoPrestamo`/`PagoAporte` detrás, una
afectación manual aplicada dos veces, una cuota re-cobrada porque el cálculo de saldos cayó a
los valores originales). El detalle completo de cada punto corregido está en el historial de
`CargaArchivoPetroServiceImpl` (comentarios `TODO O NADA (2026-08-29)` en cada sitio).

**El caso más grave de los diez encontrados no es de negocio, es de existencia.** Los nueve
puntos de `aplicarPagosArchivoPetro` son sobre negocio YA EN LA BASE — algo se calcula mal, se
cobra dos veces, queda un estado inconsistente — y todos dejan rastro: una cuota, un préstamo,
un pago con un valor equivocado que alguien puede auditar. El INSERT de `ParticipeXCargaArchivo`
en Fase 1 (`almacenaRegistros`) es distinto: si falla, ese partícipe simplemente **NO EXISTE**
para la carga. No genera error, no genera novedad, no aparece en ningún resumen, y las Fases 2 y
3 ni siquiera saben que debía estar — el archivo se procesa "sin errores" con gente adentro que
se perdió en el camino, y nadie se entera salvo que alguien note meses después que a un
partícipe no le descontaron. Es la forma más extrema del patrón "el dato no viene de donde
parece" que se persiguió toda esta semana: acá no es que el dato venga de otro lado, es que el
dato **no está y nadie lo va a preguntar**. También aborta toda la carga, con el código Petro y
el nombre del partícipe en el mensaje — no hay número de línea del archivo disponible en ese
punto del pipeline (se pierde entre `procesarContenido`, que sí lo rastrea para su propio abort,
y `almacenaRegistros`, que ya opera sobre la lista de partícipes parseados sin esa referencia); si
hiciera falta agregarlo habría que sumar un campo a `ParticipeXCargaArchivo`, cambio deliberadamente
fuera de este arreglo.

**La regla, para no volver a "simplificar" esto en seis meses poniendo un catch de vuelta:**

- **La ausencia de un dato NO es un error.** Un registro `HS` que no existe, un partícipe sin
  entidad, un préstamo sin `HistorialSueldo` activo, una AVPC sin cuota asociada: son
  condiciones de negocio normales, y se manejan con un `if` explícito que decide qué hacer (casi
  siempre: omitir esa parte y seguir), nunca con un `catch` que interprete la ausencia como
  falla.
- **Lo que debe abortar TODA la carga es que una operación FALLE**: la base no responde, un
  `saveSingle` revienta, una regla de negocio se viola. Ahí ya no hay `catch`-y-sigue: la
  excepción sube tal cual (con contexto agregado en cada capa — partícipe, código Petro,
  préstamo o cuota según corresponda — y la causa original nunca se pierde) hasta el método
  público, que la deja propagar sin atraparla. El contenedor hace rollback de TODO — nada de
  esta carga queda grabado.
- Esto **no** cambia la convención de los DAO (`selectXxx` que absorben su propio error y
  devuelven lista vacía, ver `CORRECCION_MANEJO_EXCEPCIONES_DAO.md`): eso sigue vigente, es a
  nivel de consulta. Lo que cambió es cómo reacciona el PROCESO (`procesarArchivoPetro`,
  `aplicarPagosArchivoPetro` y todo lo que llaman) cuando una operación de negocio falla de
  verdad.
- El parseo del archivo crudo (`parseDouble`/`parseLongSimple`, Fase 1) sigue la misma
  distinción: un campo vacío/ausente sigue siendo `0.0`/`0L` legítimo; un campo PRESENTE pero
  mal formado ahora propaga (antes se convertía en `$0` en silencio, lo que podía marcar
  EN_MORA a un partícipe que en realidad sí había pagado). El abort por línea ya existía en
  `procesarContenido` — este fix solo hace que se dispare.

### 3.2 Bucle principal
Por cada `DetalleCargaArchivo` de la carga (el producto `HS` se **omite**, se consume desde PH/PP),
y por cada partícipe del detalle:
- Producto `AH` → §3.6 (aportes).
- Otro producto → `aplicarPagoParticipe` (§3.3).
**Todo o nada (§3.1b):** si el procesamiento de un partícipe falla de verdad, se aborta toda la
carga — nada queda grabado. Antes (hasta 2026-08-28) los errores por partícipe se contaban y no
abortaban el resto; ese comportamiento se eliminó. Al final, sin errores: `CargaArchivo.estado = 3`.

### 3.3 `aplicarPagoParticipe` (préstamos)
1. **Monto $0** → `marcarCuotasEnMoraPorFaltaDePago`: las cuotas del mes de afectación que no estén
   PAGADA ni CANCELADA_ANTICIPADA pasan a **EN_MORA (5)** (con `codigoExterno = idCarga`). Fin.
   Marcar la mora ES el propósito completo de esta rama: si falla a mitad de camino, aborta
   toda la carga (§3.1b) — antes se tragaba el error y el partícipe quedaba sin pago Y sin mora
   bien marcada, en silencio.
2. **AVPC tiene prioridad máxima**: si el partícipe tiene afectaciones manuales (vía sus novedades
   NVPC), se aplican **solo** esas (§3.5) y se termina. Si falla a mitad de aplicar varias (algunas
   ya persistidas), aborta toda la carga — antes se tragaba el error y se seguía con el flujo
   normal de pago ENCIMA de lo ya aplicado manualmente, con riesgo real de pagar la misma cuota
   dos veces.
3. Se resuelven una sola vez: entidad (primera por rol), productos (`selectAllByCodigoPetro`),
   préstamos activos de todos esos productos. Si algo falta (ausencia de dato legítima) → se omite
   el partícipe (con log), sin abortar la carga.
4. **PH/PP + HS**: se busca la cuota a pagar y, si `cuota.valorSeguroIncendio > 0.01`, se busca el
   registro `HS` del partícipe en la carga:
   - HS encontrado: se compara `montoHS` vs `valorSeguroIncendio` esperado (±0.01). Coincida o no,
     **`montoArchivo += montoHS`** (si no coincide, la cuota terminará PARCIAL y queda logueado).
   - HS no encontrado y la cuota requiere seguro (ausencia de dato legítima, no un error): se
     procesa solo el monto PH/PP → PARCIAL.
   - La cuota no requiere seguro (`valorSeguroIncendio ≤ 0.01`): proceso normal sin HS.
   - Si la consulta/validación del HS FALLA de verdad (no "no existe": la excepción de la
     consulta), aborta toda la carga — antes se tragaba el error y la cuota quedaba PARCIAL
     igual, un resultado equivocado disfrazado de degradación legítima (§3.1b).
5. `buscarCuotaAPagar` (§3.4) y `procesarPagoCuota` (§3.5).
6. **Red de seguridad**: al final se ejecuta `verificarYActualizarEstadoPrestamos(prestamos)`
   (dedup por código) porque cualquier ruta pudo liquidar la última cuota.

### 3.4 Búsqueda de cuota y saldos reales
- `buscarCuotaAPagar`: iterativo (máx. 100). Trae la **mínima cuota no pagada** del préstamo
  (`selectMinCuotaNoPagadaByPrestamo`), calcula sus saldos reales y:
  - si quedó PAGADA por el recálculo → busca la siguiente;
  - si `totalPendiente > 0.01` → esa es la cuota a pagar;
  - si el saldo es insignificante (≤ 0.01) → la marca PAGADA y sigue.
  El mes/año del archivo NO importa aquí: **los préstamos se pagan secuencialmente por la menor
  cuota pendiente**.
- `calcularSaldosRealesCuota` (fuente de verdad = `PGPR`):
  - suma capital/interés/desgravamen/seguroIncendio pagados de TODOS los `PagoPrestamo` de la cuota;
  - `saldoX = max(0, cuota.X − pagadoX)`; `totalPendiente = Σ` (incluye seguro de incendio);
  - **autocorrección**: si `totalPendiente ≤ 0.01` y la cuota no está PAGADA → la pasa a PAGADA,
    sincroniza los `*Pagado`, `saldoCapital = max(0, saldoInicialCapital − capitalPagado)`,
    `saldoInteres = 0`, recalcula `saldo` global, **respeta `fechaPagado` existente** y persiste.
  - **Aborta toda la carga si el `saveSingle` de la autocorrección falla** (§3.1b, corregido
    2026-08-29): antes, cualquier fallo acá (incluida una falla de guardado) caía a devolver los
    valores ORIGINALES de la cuota como si nunca se hubiera pagado nada — si la cuota ya tenía
    pagos parciales reales en `PGPR`, la trataba como si debiera el 100% de nuevo (riesgo de
    re-cobro).

### 3.5 `procesarPagoCuota` — determinación de estado y desglose
`totalPendiente` = saldos reales (desgravamen + interés + capital + seguro de incendio).
La comparación del desglose archivo-vs-cuota (`validarDesgloseCuotaSinTolerancia`, ±0.01 por
componente) es **solo informativa**: lo que decide el estado es si el monto cubre el saldo.

| Caso | Resultado |
|---|---|
| cuota ya PAGADA según PGPR | todo el monto pasa a excedente (§ excedentes) + verificación de préstamo |
| `\|monto − totalPendiente\| ≤ 0.01` | **PAGADA**: `fechaPagado = now`, `capitalPagado = capital` (etc. — totales, no acumulativo porque es estado final), `saldoInteres = 0`, `saldoCapital = max(0, saldoInicialCapital − capitalPagado)`, `saldo` global recalculado |
| `monto > totalPendiente` | **PAGADA** igual que arriba + `crearRegistroPago(totalPendiente, …)` + excedente a la siguiente cuota + verificación de préstamo |
| `monto < totalPendiente` | **PARCIAL**: distribución en orden **Desgravamen → Interés → Capital → Seguro de Incendio** con `min(restante, pendiente)`; los `*Pagado` se **ACUMULAN** sobre lo previo |

- Siempre: `cuota.codigoExterno = idCarga`, `saveSingle(cuota)`, `crearRegistroPago(...)` con el
  monto aplicado en ESTA operación (incluye `valorSeguroIncendio` y `idEstado = 1L` obligatorio,
  observación `"Pago cuota #N - Mes m/aaaa - Carga X [CargaArchivo: X]"`), y
  `verificarYActualizarEstadoPrestamo(prestamo)`. Si `crearRegistroPago` falla, aborta toda la
  carga (§3.1b, corregido 2026-08-29): antes se tragaba el error y la cuota quedaba
  PAGADA/PARCIAL sin ningún `PagoPrestamo` detrás — rompía el invariante "PGPR es la fuente de
  verdad" del que depende `calcularSaldosRealesCuota` y el resto del sistema. Mismo criterio
  para `crearRegistroPagoAporte` del lado de aportes (§3.6): un `Aporte` marcado PAGADA sin su
  `PagoAporte` es el mismo problema.

**Excedentes** (`procesarExcedenteASiguienteCuota`): cualquier excedente > 0 se aplica a la
siguiente cuota pendiente con número MAYOR y saldo real > 0.01 (iterativo, máx. 100; las cuotas con
saldo insignificante se van marcando PAGADA). El excedente se procesa recursivamente vía
`procesarPagoCuota` con seguro de incendio 0 (ya se aplicó en la cuota original). Si no hay más
cuotas, el remanente queda sin aplicar (solo log).

**Afectación manual** (`aplicarAfectacionManualConRegistroPago`): se aplican las AVPC ordenadas por
número de cuota. Si la AVPC no tiene desglose (capital/interés/desgravamen en 0) pero sí
`valorAfectar`, se distribuye automáticamente con el mismo orden Desgravamen → Interés → Capital →
Seguro de Incendio contra los saldos reales. Los valores se **acumulan**; estado PAGADA si
`|totalPagadoAcumulado − totalEsperado| ≤ 0.01`, si no PARCIAL. AVPC sin cuota asociada se omite.
La tabla AVPC no tiene campo para seguro de incendio (limitación conocida, queda advertencia en log).

**Estado del préstamo** (`verificarYActualizarEstadoPrestamo`):
- Se escribe en `idEstado` (PRSTIDST). Sale temprano si ya está en estado terminal (3/4/5).
- Exige `contarCuotasByPrestamo > 0` (un préstamo sin amortización NO se cancela).
- Cancela (**CANCELADO = 3**) solo si `contarCuotasPendientesByPrestamo == 0`, donde pendiente es
  `estado IS NULL OR estado NOT IN (4,7)`.
- **No toca `fechaFin`** (es el fin de plazo); sella `fechaModificacion = now`.
- **Aborta toda la carga si falla** (§3.1b, corregido 2026-08-29): antes atrapaba `Throwable` y
  seguía — la cuota que lo invocó YA había quedado guardada como PAGADA, así que un fallo aquí
  dejaba la última cuota liquidada con el préstamo VIGENTE para siempre (o sin volver de EN_MORA
  a VIGENTE).
- Debe invocarse al final de TODA ruta que pueda marcar una cuota como PAGADA (pago exacto,
  excedente, afectación manual, recálculo por PGPR) — hoy: 2 puntos en `procesarPagoCuota`, tras
  las AVPC, y la red de seguridad de `aplicarPagoParticipe`.

### 3.6 Producto `AH` — aportes (`aplicarAporteAH`)

**Cambio del 2026-08-27 (Fase 1 del plan de devengo de aportes, D1).** `valor` de `CRD.APRT` pasa a
significar **lo efectivamente recibido**, no lo esperado. Toda fila nueva nace **pagada por
construcción**: `valorPagado = valor`, `saldo = 0`, `estado = PAGADA (4)`. Desapareció el FIFO
(`buscarAporteConSaldoPendiente` / `AporteDaoService.selectMinAporteConSaldo`, marcado
`@Deprecated`): ya no existe el concepto de "abonar" a un aporte existente, cada tramo recibido crea
su propia fila. La razón: el saldo del partícipe es `SUM(APRTVLRR)` (ver
`SaldoAporteServiceImpl`), así que una fila con `valor = esperado` y `valorPagado < valor` inflaba
ese saldo y el "registrado" que lee `CierreCarteraDaoServiceImpl.selectAportesRegistrados` por la
diferencia — exactamente en la suma de los `saldo` de las filas PARCIAL. Los datos históricos con
esa inflación se corrigen aparte, con
`docs/logica-negocio/crd/sql/62_CORRECCION_VALOR_APORTES_CARGA.sql` (documento revisable, no
ejecutado por el agente).

**Cambio del 2026-08-27 (Fase 2 del plan de devengo de aportes).** El reparto ya no es único-tipo
vs alternado: `distribuirAportePorDevengo` reemplaza `procesarAporteUnicoTipo` y
`procesarAportesAlternados` por la prelación de §2.3 del plan — "busca el mes de devengo incompleto
más antiguo":

```
para cada mes m, desde el primer mes incompleto hasta el mes de la carga (y más allá si sobra):
    para tipo en (JUBILACION 9, CESANTIA 11):        // jubilación siempre primero
        faltante = esperado(m, tipo) − aportado(m, tipo)
        if faltante > 0 y disponible > 0:
            crear fila: valor = min(disponible, faltante), devengo = m, tipo, APORTE_MENSUAL
```

- `esperado(m, tipo)`: punto de extensión único, método privado `esperadoMensual`.
  **Cambio del 2026-08-27 (Fase 3):** ya no sale de `HistorialSueldo`; delega en
  `VigenciaContratoService.esperadoPorEntidad`, que resuelve el contrato activo de la
  entidad y devuelve el monto de la vigencia de `CRD.VGCN` vigente al **último día de `m`**
  (0.0 si el contrato no tiene ninguna vigencia abierta ese mes). Ahora sí varía con `m`.
  `HistorialSueldo` estado 99 queda sólo como fuente de la migración a `CRD.VGCN`
  (`docs/logica-negocio/crd/sql/64_MIGRACION_CONTRATOS_VIGENCIAS.sql`), no del cobro
  corriente.
- `aportado(m, tipo)` = `SUM(valor)` de esa entidad y tipo con **devengo exacto** `= m`
  (`AporteDaoService.sumValorPorEntidadTipoYRangoDevengo`, una sola consulta para todo el rango).
- Si el recibido excede lo que falta hasta el mes de la carga, el sobrante se **anticipa** a los
  meses siguientes (D4), sin más tope que el de seguridad (60 meses).
- Cada fila creada (`crearNuevoAporte`) sigue registrando un **`PagoAporte`** por el mismo monto, y
  sigue naciendo pagada por construcción (Fase 1, D1): no hay abono posterior.
- Aporte nuevo: `filial` de la entidad, `fechaTransaccion` = último día del mes de la CARGA 23:59:59
  (es la fecha de caja, `APRTFCTR`, y **no cambia de significado** aunque el devengo sea un mes
  distinto por atraso o anticipo — D2, contabilidad la sigue leyendo igual), `periodoDevengo` = el
  mes `m` que cubre esa fila, `tipoMovimiento = APORTE_MENSUAL`, `idAsoprep = idCarga`, glosa con
  mes/año **de la carga**/carga (no del devengo), usuario `SAA_AH`.

**⚠️ PRECONDICIÓN OPERATIVA para desplegar esta fase:** la prelación lee `aportado(m,tipo)` por
devengo **exacto** (`a.periodoDevengo = m`), no por `NVL(APRTFCTR)` — eso es a propósito, ver el
JavaDoc de `sumValorPorEntidadTipoYDevengo`. Si esta carga corre **antes** de ejecutar
`docs/logica-negocio/crd/sql/63_BACKFILL_DEVENGO_APORTES.sql`, cualquier entidad con historial
anterior a esta fase aparecerá con **todos** los meses desde 2025-06 incompletos (las filas viejas
tienen devengo `NULL`) y la carga intentaría repartir el pago recibido hacia atrás sobre esos meses
ya cobrados. **Correr el backfill antes de la primera carga que use este código.**

**Mora del partícipe** (efectos secundarios; un fallo aquí no aborta la carga):
- `montoRecibido ≤ 0.01` → `evaluarMoraPorFaltaDeAporte`: si la entidad está en **ACTIVO (1)**, el
  periodo anterior fue cargado con producto AH, y en ese periodo la entidad NO registró aportes
  positivos (tipos 9/11 en `CRD.APRT` — la misma base del padrón), entonces son dos periodos
  consecutivos sin aportar → la entidad pasa a **ACTIVO_EN_MORA (8)**. Si el periodo anterior no se
  cargó o la consulta falla, no se marca.
- `montoRecibido > 0.01` → `restaurarActivoPorPago`: si la entidad estaba en ACTIVO_EN_MORA vuelve a
  **ACTIVO** (basta que llegue un pago; la generación ya cobra la deuda acumulada completa).

## 4. Vía alterna: `ProcesoCargaPetroServiceImpl` (crd) — parcialmente implementada

`POST /rest/asgn/procesarCargaPetro/{idCarga}`. Procesa solo registros con `novedadesCarga = OK`,
con control de re-proceso vía `CRD.PRCA` (`yaFueProcesado`). Diferencias con el flujo vigente:
solo maneja el caso "una cuota del mes", los aportes están en `TODO` (`esAporte = false` fijo),
usa tolerancia $1 para decidir PAGADA, y guarda el resultado/novedad/saldos en PRCA.
Sí respeta los estados terminales del préstamo y escribe en `PRSTIDST`.
**No es el flujo productivo**; ante discrepancias manda `aplicarPagosArchivoPetro`.

## 4b. `DTPRTTLL` ya no es el monto a cobrar: usar siempre `totalBaseCuota(cuota)`

**Cambio del 2026-08-14.** Desde que existe el proceso diario de interés de mora
(`com.saa.ejb.crd.service.ProcesoMoraPrestamoService`, ver
`docs/logica-negocio/crd/PROCESO-DIARIO-INTERES-MORA.md`), la columna `DTPRTTLL` de una **cuota
vencida** incluye la mora acumulada, que crece todos los días a las 02:00.

Este proceso **no debe verla nunca**. `CargaArchivoPetroServiceImpl` ya no lee
`cuota.getTotal()` directamente: usa el helper privado

```java
private Double totalBaseCuota(DetallePrestamo cuota) {
    return nullSafe(cuota.getTotal()) - nullSafe(cuota.getMora()) - nullSafe(cuota.getInteresVencido());
}
```

que devuelve exactamente el valor que esta clase leía antes de que el proceso diario existiera.
**El comportamiento del módulo no cambió**; el helper solo lo preserva.

Por qué era obligatorio:

- **Fase 2**: compara `DTPRTTLL` contra el monto del archivo con tolerancia de $1. Con la mora
  adentro, **toda cuota vencida** generaría `MONTO_INCONSISTENTE (13)` — que está en
  `NOVEDADES_REQUIEREN_AFECTACION_MANUAL` y **bloquearía la fase 3 completa** de la carga mensual.
- **Fase 3**: `calcularSaldosRealesCuota` usa `DTPRTTLL` como `totalPendiente` cuando la cuota no
  tiene pagos previos. La prelación de este proceso solo reparte entre **desgravamen, interés,
  capital y seguro de incendio** — no tiene componente de mora, así que jamás podría agotar un
  pendiente que la incluya y toda cuota vencida quedaría **PARCIAL en vez de PAGADA**.

⚠️ **Regla permanente**: cualquier lectura nueva de `DTPRTTLL` en este servicio debe pasar por
`totalBaseCuota(...)`. La mora de las cuotas vencidas la cobra el motor de pagos de préstamos
(`MotorPagoPrestamoService`), que sí tiene el componente en su prelación de 6.

Nota: `GeneracionArchivoPetroServiceImpl` tampoco lee `DTPRTTLL` — arma el monto en
`calcularSaldoCuota` sumando los componentes uno a uno (`capital + interés + mora +
interés vencido + desgravamen`, menos lo ya pagado). Por eso el archivo enviado a la empresa
**sí cobra la mora** en cuanto el proceso diario empieza a alimentar `DTPRMRAA`, sin ningún
cambio de código. Ver `REGLAS-GENERACION-PETRO.md`.

## 5. Reglas de implementación

- Prohibido `selectAll()` en este servicio: usar los métodos específicos del DAO
  (`selectByCargaArchivo`, `selectByDetalleCargaArchivo`, `selectMinCuotaNoPagadaByPrestamo`,
  `selectByIdDetallePrestamo`, `selectByNovedad`, `selectByParticipe`,
  `selectByCodigoPetroYProductoEnCarga`, `selectByEntidadYEstadoActivo`, `selectMinAporteConSaldo`,
  `contarCuotasByPrestamo`, `contarCuotasPendientesByPrestamo`, `selectUltimaCargaProcesada`, …).
- Las constantes de rubros son `int` y los campos de entidad `Long`: castear `(long)` explícito.
- Los DAOs de este módulo absorben errores de BD y devuelven listas vacías/null para que una fila
  mala no aborte el lote (ver `docs/general/CORRECCION_MANEJO_EXCEPCIONES_DAO.md`) — preservar.
- Timeouts WildFly ampliados a 15 min para EJB stateful y transacciones (cargas grandes).

## 6. Cobro de Petro en dos pasos (Fase 3a, 2026-08-28)

Diseño autoritativo: regla 11 de §5 y §3.3 de
`docs/logica-negocio/crd/LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md`. Contrato de API
CONGELADO con el frontend: `docs/logica-negocio/crd/API-COBRO-PETRO-DOS-PASOS.md` — no
cambiarlo sin acordarlo con el árbitro.

**El flujo, en dos pasos:**

1. **PASO 1 — Contabilidad confirma que el dinero llegó al banco** (acto explícito, no
   automático). Registra las N transferencias con las que Petro pagó (`CRD.TRCR` — "Petro
   puede pagar con más de 1 transferencia", pizarra), y al confirmar genera el asiento
   **TRANSITORIO**: D Banco(s) (cuenta contable de cada `TRCR.cuentaBancaria`) → H
   `2.3.01.15.01`, plantilla alterno **19** (`COBRO_TRANSITORIO_PETRO`). Sella
   `CargaArchivo.usuarioContabilidadConfirma`/`fechaAutorizacionContabilidad` (`CRARUSCC`/
   `CRARFCAC` — existían mapeados y sin usar desde antes de esta fase) y mueve `CRARESTD` a
   `CrdEstadoCargaArchivo.CONFIRMADO_CONTABILIDAD` (2).
2. **PASO 2 — Se procesa el archivo** (`aplicarPagosArchivoPetro`, precondición 0b de §3.1:
   exige el paso 1 hecho). Asiento de **REPARTO** (D `2.3.01.15.01` → H `1.4.05.05`/
   `1.4.05.10`, plantilla alterno **20**) y de **APLICACION** a las cuentas reales (plantilla
   alterno **21** + bandas de capital desde `CRD.BNDP` vía `ClasificadorBandaService` — **NO
   implementado todavía**, ver pendientes abajo).

**Servicio:** `com.saa.ejb.crd.service.CobroPetroContableService` /
`serviceImpl.CobroPetroContableServiceImpl`. Endpoints en
`com.saa.ws.rest.asoprep.AsoprepGenerales` (`@Path("asgn")`, junto a `procesarArchivoPetro`/
`procesarCargaPetro`/`aplicarPagosArchivoPetro`): `GET/POST/DELETE /transferencias`,
`POST /confirmarRecepcion/{idCarga}`, `POST /reversarRecepcion/{idCarga}`,
`GET /estadoContable/{idCarga}`.

**Tablas nuevas** (DDL `docs/logica-negocio/crd/sql/DDL-COBRO-PETRO-DOS-PASOS.sql`, **ejecutado
en la BD local de desarrollo el 2026-08-28**; pruebas y producción los corre el usuario):
- `CRD.TRCR` (`TransferenciaCargaPetro`): una fila por transferencia, FK a `TSR.CNBC`/`BNCO`/
  `BEXT` (dirección `crd → tsr`, la única permitida).
- `CRD.ANCP` (`AsientoCargaPetro`): un asiento por sub-proceso (`ANCPTPOO`:
  `com.saa.rubros.SubProcesoCobroPetro` — 1 TRANSITORIO, 2 REPARTO, 3 APLICACION), espejo
  literal de `CRD.ANCC` del cierre de cartera. Un solo estado (`ANCPIDST`: 1 vigente, 0
  reversado) — más simple que `ANCC`, que arrastra dos.

**⚠️ Trampa de qué columna dice "confirmada" — ya nos mordió tres veces en esta ola.**
`CRARESTD` es **transitorio**: avanza de `CONFIRMADO_CONTABILIDAD`(2) a `PROCESADO`(3) en
cuanto corre el paso 2, así que leerlo para decidir "¿ya se confirmó el paso 1?" da un falso
negativo sobre cualquier carga ya procesada. El marcador DURADERO es
`CargaArchivo.fechaAutorizacionContabilidad != null` (con `usuarioContabilidadConfirma`) —
así lo calcula `resumenTransferencias`, así lo valida `confirmarRecepcion`, y así lo limpia
`reversarRecepcion`. **No leer nunca `CRARESTD` para esto.**

**`idEmpresa` no viaja en el contrato.** `SolicitudConfirmarRecepcion` no lo trae, y
`CargaArchivo`/`Filial` no tienen FK a `Empresa` (verificado: `CRD.FLLL` tiene 4 columnas).
Se deriva de la cuenta bancaria de la primera transferencia vigente
(`TRCR.cuentaBancaria.planCuenta.empresa`) — toda transferencia exige una `CuentaBancaria`, y
esta siempre resuelve un `PlanCuenta` con empresa (verificado contra `TSR.CNBC`/`CNT.PLNN`).

**Pendiente — Paso 2 (asientos de REPARTO y APLICACION), no implementado todavía:**
- El reparto (plantilla 20) necesita el desglose del total del archivo entre aportes
  (`1.4.05.05`) y préstamos (`1.4.05.10`) — hoy `CargaArchivo.totalDescontado` es un total
  agregado, hace falta sumar por producto desde `DetalleCargaArchivo`.
- La aplicación (plantilla 21) necesita el saneamiento de auxiliares de
  `docs/logica-negocio/crd/ACTUALIZACION-PLANTILLA-21-PETRO-APLICACION.md` **ejecutado
  primero** (sin ejecutar al 2026-08-28), más el motor de clasificación de bandas
  (`ClasificadorBandaService.clasificarEnBandas`) aplicado a cada cuota pagada de la carga.
- El reverso de `precondición 0b` cuando se reversa el paso 2 (hoy `reversarRecepcion`
  rechaza si `CRARESTD == PROCESADO`; falta el reverso simétrico del propio paso 2).

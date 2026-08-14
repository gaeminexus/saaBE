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
3. Un `ParticipeXCargaArchivo` por línea; si un INSERT falla se registra el error y se **continúa**
   con el siguiente (no aborta la carga).
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

### 3.2 Bucle principal
Por cada `DetalleCargaArchivo` de la carga (el producto `HS` se **omite**, se consume desde PH/PP),
y por cada partícipe del detalle:
- Producto `AH` → §3.6 (aportes).
- Otro producto → `aplicarPagoParticipe` (§3.3).
Los errores por partícipe se cuentan y no abortan el resto. Al final: `CargaArchivo.estado = 3`.

### 3.3 `aplicarPagoParticipe` (préstamos)
1. **Monto $0** → `marcarCuotasEnMoraPorFaltaDePago`: las cuotas del mes de afectación que no estén
   PAGADA ni CANCELADA_ANTICIPADA pasan a **EN_MORA (5)** (con `codigoExterno = idCarga`). Fin.
2. **AVPC tiene prioridad máxima**: si el partícipe tiene afectaciones manuales (vía sus novedades
   NVPC), se aplican **solo** esas (§3.5) y se termina.
3. Se resuelven una sola vez: entidad (primera por rol), productos (`selectAllByCodigoPetro`),
   préstamos activos de todos esos productos. Si algo falta → se omite el partícipe (con log).
4. **PH/PP + HS**: se busca la cuota a pagar y, si `cuota.valorSeguroIncendio > 0.01`, se busca el
   registro `HS` del partícipe en la carga:
   - HS encontrado: se compara `montoHS` vs `valorSeguroIncendio` esperado (±0.01). Coincida o no,
     **`montoArchivo += montoHS`** (si no coincide, la cuota terminará PARCIAL y queda logueado).
   - HS no encontrado y la cuota requiere seguro: se procesa solo el monto PH/PP → PARCIAL.
   - La cuota no requiere seguro (`valorSeguroIncendio ≤ 0.01`): proceso normal sin HS.
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
  `verificarYActualizarEstadoPrestamo(prestamo)`.

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
- Atrapa `Throwable`: un fallo aquí no aborta la carga.
- Debe invocarse al final de TODA ruta que pueda marcar una cuota como PAGADA (pago exacto,
  excedente, afectación manual, recálculo por PGPR) — hoy: 2 puntos en `procesarPagoCuota`, tras
  las AVPC, y la red de seguridad de `aplicarPagoParticipe`.

### 3.6 Producto `AH` — aportes (`aplicarAporteAH`)

**Mora del partícipe** (efectos secundarios; un fallo aquí no aborta la carga):
- `montoRecibido ≤ 0.01` → `evaluarMoraPorFaltaDeAporte`: si la entidad está en **ACTIVO (1)**, el
  periodo anterior fue cargado con producto AH, y en ese periodo la entidad NO registró aportes
  positivos (tipos 9/11 en `CRD.APRT` — la misma base del padrón), entonces son dos periodos
  consecutivos sin aportar → la entidad pasa a **ACTIVO_EN_MORA (8)**. Si el periodo anterior no se
  cargó o la consulta falla, no se marca.
- `montoRecibido > 0.01` → `restaurarActivoPorPago`: si la entidad estaba en ACTIVO_EN_MORA vuelve a
  **ACTIVO** (basta que llegue un pago; la generación ya cobra la deuda acumulada completa).

**Generación/pago de aportes** (requiere `HistorialSueldo` estado 99; sin él no se procesa):
- Esperados: `montoJubilacion` y `montoCesantia` del historial.
- Solo uno tiene valor → todo el monto se aplica a ese tipo (`procesarAporteUnicoTipo`).
- Ambos tienen valor → `procesarAportesAlternados`:
  1. **FIFO**: primero se completan aportes anteriores con saldo (`selectMinAporteConSaldo`: el más
     antiguo con `saldo > 0.01`, estado PENDIENTE o PARCIAL, de cualquier usuario) — jubilación y
     luego cesantía.
  2. Con el remanente se **alterna** creando nuevos aportes: Jubilación → Cesantía → Jubilación …,
     cada uno por `min(disponible, montoEsperado)`; en cada turno se re-verifica si el otro tipo
     quedó con saldo pendiente y se salta a pagarlo primero.
- Cada aplicación actualiza `valorPagado`, `saldo = max(0, valor − valorPagado)` y estado
  (PAGADA si saldo ≤ 0.01, PARCIAL si hay pago, PENDIENTE si no — códigos de `EstadoCuotaPrestamo`),
  y registra un **`PagoAporte`** (concepto con mes/año, partícipe y carga; usuario `SISTEMA`).
- Aporte nuevo: `filial` de la entidad, `fechaTransaccion` = último día del mes de la carga
  23:59:59, `idAsoprep = idCarga` (enlace con la carga), glosa con mes/año/carga, estado PENDIENTE,
  usuario `SAA_AH`.

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

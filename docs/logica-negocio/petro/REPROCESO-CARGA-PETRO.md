# REPROCESO DE UNA CARGA PETRO YA APLICADA

**Cuadre Superintendencia — cambio C4** (`cuadreSuper/01-seguimiento/CAMBIOS-SAA.md`). Escrito el
2026-10-28 contra la base del proyecto (`saa-oracle-cuadre`, copia de producción del 1-oct-2026 con
la apertura de mayo 2025 cargada). Forma parte de la documentación vigente del módulo junto con
[REGLAS-GENERALES-PETRO.md](REGLAS-GENERALES-PETRO.md) y [REGLAS-CARGA-PETRO.md](REGLAS-CARGA-PETRO.md)
(§7 de este último remite acá).

Código: `com.saa.ejb.asoprep.service.ReprocesoCargaPetroService` /
`serviceImpl.ReprocesoCargaPetroServiceImpl` (`@Stateless`), el método
`CargaArchivoPetroService.aplicarPagosArchivoPetroReproceso` y el endpoint
`POST /SaaBE/rest/crar/reprocesar/{idCarga}` (`CargaArchivoRest`).

---

## 1. Por qué existe

Las 16 cargas Petro de jun-2025 a sep-2026 están en el SAA con su detalle por partícipe
(`CRD.CRAR`/`DTCA`/`PXCA`). Las de jun-2025 a jul-2026 se **aplicaron con una versión anterior del
proceso**: pagos fechados el día en que se corrió el lote (abril 2026), sin enlace a la carga
(`CRARCDGO` nulo), aportes sin `APRTIDAS`, y sin ningún asiento. El proceso vigente ya fecha pagos,
aportes y asientos al **último día del mes de la carga** y deja todo enlazado, así que no hay que
reescribir el motor: **hay que poder volver a correrlo** sobre una carga que ya está en estado 3,
deshaciendo antes lo que la versión vieja dejó.

## 2. Cómo quedó la carga 352 (junio 2025) antes del reproceso — medido el 2026-10-28

Es lo que la anulación tiene que encontrar. Consultas reproducibles en
[sql/reproceso-carga-352-controles.sql](sql/reproceso-carga-352-controles.sql), bloque A.

| Qué | Cómo está | Cómo lo identifica el reproceso |
|---|---|---|
| `CRD.CRAR` 352 | estado 3 (PROCESADO), mes/año 6/2025, descontado 535.477,39, a descontar 708.813,71, creada 30-mar-2026. **Sin confirmación de contabilidad** (`CRARFCAC` nulo), **sin transferencias** (`CRD.TRCR`), **sin asientos** (`CRD.ANCP`) | — |
| Detalle | 6 productos: AH 2.068 filas / 162.004,30 · PE 923 / 172.117,72 · PH 342 / 174.722,21 · PQ 45 / 20.933,40 · PP 4 / 2.281,03 · HS 254 / 3.418,73 | — |
| Pagos de préstamo | **1.313** filas en `CRD.PGPR`, **319.328,38**, todas con fecha **06-abr-2026**, `CRARCDGO` NULO, `PRSTCDGO` NULO, `PGPRASNT` NULO, sin tipo ni usuario, observación `"Pago cuota #N - Mes 6/2025 - Carga 352 [CargaArchivo: 352]"`. Componentes: capital 185.863,59, interés 115.411,50, desgravamen 15.366,03, seguro incendio 2.745,56 (mora e interés vencido 0). Cuotas tocadas: 1.306 hoy PAGADA, 3 PARCIAL, 4 CANCELADA_ANTICIPADA; 1.118 vencen en jun-2025, 119 antes, 76 después. 148 de esas cuotas tienen además otros pagos vigentes (158 filas, 32.608,08) | `selectVigentesLegadoPorObservacionCarga(352)`: `CRARCDGO IS NULL AND PGPROBSR LIKE '%[CargaArchivo: 352]%'` |
| Pagos enlazados | 0 con `CRARCDGO = 352` | `selectVigentesByCargaArchivo(352)` |
| Aportes del esquema viejo | **2.635** filas en `CRD.APRT`, **160.350,81** (cesantía 1.725 / 98.166,91; jubilación 910 / 62.183,90), fecha de caja 30-jun-2025 00:00, sin usuario, `APRTIDAS` y `CRARCDGO` NULOS, estado 4, tipo de movimiento 1, glosa `"Aporte cesantia - CargaArchivo: 352"` / `"Aporte jubilacion - CargaArchivo: 352"` (9 de ellas `"... (reubicado desde 6/2025) - CargaArchivo: 352"`). Devengo poblado por el backfill: 2.626 en jun-2025, 14 en jul-2025. 2.005 partícipes distintos. **Sin `CRD.PGAP`** | `selectVigentesLegadoPorGlosaCarga(352)`: `APRTIDAS IS NULL AND CRARCDGO IS NULL AND APRTGLSA LIKE '%CargaArchivo: 352'` |
| Aportes enlazados | **3** filas con `APRTIDAS = 352` y `CRARCDGO = 352` (174,08; usuario `SAA_AH`, registradas el 27-ago-2026, con su `PGAP`): una prueba del proceso nuevo sobre esta carga | `selectByCarga(352)` (filtra `APRTIDAS`) |
| Cuotas con mora marcada por la carga | 1.227 cuotas con `DTPRCDEX = 352` (1.210 hoy PAGADA, 11 EN_MORA, 4 CANCELADA_ANTICIPADA, 2 PARCIAL) | No se tocan (ver §4.5) |
| Novedades `CRD.NVPC` | tipo 5: 30 · tipo 10: 77 · tipo 13: 120 (41.708,35) · tipo 17: 2. **Ninguna AVPC** | — |
| Roles sin partícipe | **14 filas** de `PXCA` cuyo rol no existe en `CRD.ENTD.ENTDRLPC`: AH 7 (746,74), PH 4 (3.775,62), PE 1 (330,57), HS 2 (19,11) = **4.872,04**. Hoy con `PXCANVCA = 0` (la fase 1 vieja no los detectó) | Modo reproceso, §4.4 |
| Pagos posteriores sobre los mismos préstamos | 0 pagos vigentes con fecha posterior al 30-jun-2025 sobre los préstamos de la 352 (en esta base; la 353 en adelante se reprocesan después) | — |

Lo que falta para que la 352 pueda reprocesarse hoy: **el paso 1 del cobro en dos pasos** (registrar
la o las transferencias con que Petro pagó junio 2025 — suma = 535.477,39 ± 0,01 — y confirmar la
recepción). Sin eso no hay empresa contable que resolver ni asiento transitorio que repartir, y el
reproceso se niega con el mensaje exacto de qué falta (§4.1). Es la misma precondición 0b de la fase
3 normal; no se saltea. Como la 352 está en estado 3, la confirmación se pide con
`"paraReproceso": true` (§9, agregado el 2026-10-28): sin eso `confirmarRecepcion` exige estado
CARGADO y la rechaza.

## 3. Diseño en una frase

**Anular con trazabilidad lo que la carga generó (marcar, nunca borrar), devolver la carga a
CONFIRMADO_CONTABILIDAD y volver a correr la fase 3 vigente en "modo reproceso", todo en una sola
transacción.** Cada corrida lleva una etiqueta estándar
`REPROCESO carga <id> corrida <N>` que queda en cada fila anulada; correr el reproceso dos veces
deja el mismo resultado neto (la corrida 2 anula lo que dejó la 1 y vuelve a aplicar).

```
POST /rest/crar/reprocesar/352   {usuario, ip, motivo}
   │
   ├─ 0. validar: carga existe · mes/año · CRARFCAC != null (paso 1 hecho) · transferencias vigentes → idEmpresa
   ├─ 1. corrida N = 1 + max(corridas previas en PGPRMTAN / APRTGLSA)   etiqueta = "REPROCESO carga 352 corrida N"
   ├─ 2. asientos: ANCP vigentes REPARTO(2) y APLICACION(3) → AsientoService.anulaAsiento(id, usuario, etiqueta+motivo), ANCPIDST=0
   │              (el TRANSITORIO(1) del paso 1 se conserva)
   ├─ 3. pagos:    PGPR enlazados (CRARCDGO) ∪ esquema viejo (observación "[CargaArchivo: 352]")
   │              → re-provisión de intereses (ProvisionInteresService.reProvisionarPorAnulacion, vacía si no hay MVIC)
   │              → PGPRANUL=1, PGPRUSAN, PGPRFCAN, PGPRMTAN = etiqueta + motivo
   │              → cada cuota tocada: MotorPagoPrestamoService.recalcularCuotaDesdePagos (desde los pagos vigentes)
   │              → préstamo CANCELADO(3) con cuotas pendientes → VIGENTE(2)
   ├─ 4. aportes:  APRT enlazados (APRTIDAS) ∪ esquema viejo (glosa termina en "CargaArchivo: 352"), sin REVERSO ni ya reversados
   │              → contra-movimiento: fila nueva tipo REVERSO(5), valor −original, misma fecha de caja, mismo devengo,
   │                misma APRTIDAS/CRARCDGO, glosa = etiqueta + motivo + " - reverso APRT <id>"
   │              → original: APRTIDST = 0 (marca "reversado por reproceso"; su valor se conserva)
   │              → sus PGAP: PGAPIDST = 0
   ├─ 5. CRAR.CRARESTD = 2 (CONFIRMADO_CONTABILIDAD); CRARFCAC/CRARUSCC intactos
   ├─ 6. CargaArchivoPetroService.aplicarPagosArchivoPetroReproceso(352, etiqueta)   ← la fase 3 vigente, ver §4.4
   │              pagos (motor, fecha 30-jun-2025 23:59:59, CRARCDGO=352) · aportes (devengo, APRTFCTR 30-jun-2025, APRTIDAS=352)
   │              · DSBN (bandas) reescrito · asiento REPARTO · asiento APLICACION (fecha 30-jun-2025) · reverso de provisión · CRARESTD=3
   └─ 7. medir lo aplicado y devolver ResultadoReprocesoCargaPetro
```

## 4. Detalle por tabla

### 4.1 Precondiciones (no tocan datos)

| Falla | Mensaje / efecto |
|---|---|
| carga inexistente, sin mes/año | `IncomeException` |
| `usuario` o `motivo` vacíos | `IncomeException` — el motivo se estampa en todo lo anulado, no es opcional |
| `CRARFCAC` nulo (paso 1 no hecho) | `IncomeException` con las dos llamadas que faltan (`POST /rest/asgn/transferencias`, `POST /rest/asgn/confirmarRecepcion/{id}`) |
| confirmada pero sin transferencias vigentes | `IncomeException` (no puede pasar por diseño; se verifica igual) |

### 4.2 `CNT.ASNT` / `CRD.ANCP` — asientos

Solo los sub-procesos **REPARTO** y **APLICACION** vigentes de la carga. Se anulan por
`AsientoService.anulaAsiento(idAsiento, usuario, motivo)`, que es el estándar del módulo: anula
(`ASNTESTD` = ANULADO, `ASNTUSAN/FCAN/MTAN`) si el período está abierto, y **reversa** (asiento de
contrapartida + original en REVERSADO) si el período está mayorizado. La fila `ANCP` pasa a
`ANCPIDST = 0` con `"REVERSADO: <etiqueta> - <motivo>"` en la observación — exactamente como
`reversarRecepcion` trata el TRANSITORIO. El **TRANSITORIO no se toca**: pertenece al paso 1
(contabilidad confirmó que la plata entró al banco) y la re-aplicación lo necesita para repartirlo.

Los asientos secundarios de la aplicación anterior se deshacen por su propio mecanismo:
- **Reverso de provisión de intereses** (`ProvisionInteresService.reversarPorPagos`, MVIC tipo 2):
  se compensa con `reProvisionarPorAnulacion(pagos, idEmpresa, fechaContable, "REPROCESO_PETRO", idCarga, usuario)`
  — MVIC tipo 4 + asiento espejo fechado al último día del mes de la carga. Si ningún pago tiene un
  tipo 2 vigente (caso de la 352: pagos del esquema viejo, sin provisión), no genera nada.
- **`CRD.DSBN`** (distribución en bandas) no se anula: `registrarDistribucionCargaPetro` borra y
  reescribe por `(CARGA_PETRO, idCarga)` en la re-aplicación. Es la idempotencia que ya tenía.

### 4.3 `CRD.PGPR` / `CRD.DTPR` / `CRD.PRST` — pagos de préstamo

- **Qué se anula:** la unión de `selectVigentesByCargaArchivo(idCarga)` (enlazados) y
  `selectVigentesLegadoPorObservacionCarga(idCarga)` (esquema viejo: `CRARCDGO IS NULL AND
  PGPROBSR LIKE '%[CargaArchivo: N]%'`, con corchetes para que la 35 no arrastre a la 352). Solo
  vigentes (`PGPRANUL` nulo o 0).
- **Cómo:** `PGPRANUL = 1`, `PGPRUSAN = usuario`, `PGPRFCAN = ahora`, `PGPRMTAN = etiqueta + " - " +
  motivo` (truncado a 500). El valor y los componentes quedan intactos. En los pagos del esquema
  viejo `PRSTCDGO` venía nulo: se completa desde la cuota (es el valor correcto y lo necesita la
  re-provisión).
- **Cuotas:** cada cuota tocada pasa por `MotorPagoPrestamoService.recalcularCuotaDesdePagos` —
  reconstruye `*Pagado`, saldos, `DTPRSLDO`, estado (PAGADA / PARCIAL / PENDIENTE o EN_MORA según
  la fecha de vencimiento contra hoy) y `fechaPagado` desde los pagos **vigentes** que le quedan.
  Es el mismo camino que usa el reverso de un evento de pago (`ProcesoPagoPrestamoServiceImpl.anularOperacion`).
- **Préstamos:** si quedó en `PRSTIDST = 3 (CANCELADO)` y ahora tiene cuotas pendientes, vuelve a
  `2 (VIGENTE)` con `fechaModificacion = ahora`. La cancelación automática solo la produce la
  última cuota pagada, y acaba de dejar de estarlo; los demás terminales (4, 5, 12, 13) no los
  escribe la carga y no se tocan. EN_MORA/VIGENTE los regula el motor al re-aplicar
  (`regularizarPrestamoSiSinMora`).

### 4.4 `CRD.APRT` / `CRD.PGAP` — aportes

- **Qué se reversa:** la unión de `selectByCarga(idCarga)` (`APRTIDAS = N`) y
  `selectVigentesLegadoPorGlosaCarga(idCarga)` (`APRTIDAS IS NULL AND CRARCDGO IS NULL AND
  APRTGLSA LIKE '%CargaArchivo: N'`, anclado al final), **excluyendo** las filas tipo REVERSO (5) y
  las ya reversadas (`APRTIDST = 0`).
- **Cómo — contra-movimiento, no borrado ni cambio de valor.** `APRT` no tiene columna de
  anulación y el saldo del partícipe es `SUM(APRTVLRR)` sin filtro de estado
  (`SaldoAporteServiceImpl`), así que el estándar de la casa es la fila negativa
  (`DevolucionAporteServiceImpl.reversar…`, rubro 235 alterno 5 `REVERSO`). La fila nueva lleva:
  misma entidad/filial/contrato/tipo, `valor = −original`, `valorPagado = 0`, `saldo = 0`, estado
  PAGADA (4), **misma `APRTFCTR`** (fecha de caja: el saldo a cualquier corte queda neto en cero),
  **mismo periodo efectivo** (`APRTPRDV` del original, o `TRUNC(APRTFCTR,'MM')` si era nulo con
  valor positivo — un negativo sin devengo no cuenta para ningún mes en `PERIODO_EFECTIVO_SQL`,
  así que se fija explícito para que `aportado(m, tipo)` de la prelación vuelva a cero), **misma
  `APRTIDAS` y `CRARCDGO`** (así la suma por carga del asiento de aplicación y
  `validarRecibidoIgualAplicado` neutralizan al original en vez de contarlo), tipo de movimiento
  REVERSO, glosa `etiqueta + " - " + motivo + " - reverso APRT <id> (<glosa original>)"`, usuario del
  reproceso.
- **Original:** `APRTIDST = 0` (`Estado.INACTIVO`). Es solo la marca "reversado por reproceso" para
  que la próxima corrida no lo tome dos veces: ninguna suma del módulo filtra por ese estado, y los
  dos únicos métodos que sí lo hacen (`selectByEntidadTipoYCarga`, `selectAporteAdelantado`) no
  tienen llamadores desde la Fase 1 del plan de devengo.
- **`PGAP`** del original → `PGAPIDST = 0` (como la devolución). Los aportes del esquema viejo no
  tienen `PGAP`.

### 4.5 Lo que NO se toca, a propósito

- `DTPRCDEX` (cuota marcada EN_MORA por la carga cuando el partícipe no pagó) y `DTPRESTD = 5` de
  cuotas sin pagos: la re-aplicación vuelve a marcarlas por el mismo camino
  (`marcarCuotasEnMoraPorFaltaDePago`); re-marcar una cuota ya EN_MORA es un no-op.
- `CRD.ENTD.ENTDIDST` (estado del partícipe ACTIVO/ACTIVO_EN_MORA): la fase 3 normal lo mueve en
  `evaluarMoraPorFaltaDeAporte`/`restaurarActivoPorPago`; en modo reproceso **no** (el estado de
  hoy no se decide con el aporte de un mes pasado; el historial de estados es el dato N11 del
  proyecto, se reconstruye aparte).
- `CRD.NVPC`/`AVPC`: las novedades existentes se conservan; la re-aplicación agrega las suyas por el
  registrador idempotente (una por (fila, tipo)). Las AVPC vigentes se respetan y se aplican como en
  la fase 3 normal.
- El asiento TRANSITORIO y las transferencias (`TRCR`).
- `CRARFCAC`/`CRARUSCC` (el paso 1 sigue hecho).

### 4.6 La re-aplicación: `aplicarPagosArchivoPetroReproceso` (modo reproceso)

Es **el mismo** `aplicarPagosArchivoPetro` — misma prelación de aportes por devengo, mismo motor de
pagos, misma fecha de efecto (último día del mes de la carga: `crearContextoPagoCarga`,
`crearNuevoAporte`, `crearRegistroPagoAporte`, y los tres asientos en `CobroPetroContableServiceImpl`,
verificado línea por línea el 2026-10-28 — no hubo que agregar ninguna fecha contable), mismos
asientos de REPARTO y APLICACION, mismo `CRARESTD = 3` al final. El modo (un `boolean` de instancia
del bean `@Stateful`, encendido solo dentro de ese método y apagado en `finally`) cambia **cuatro**
cosas, cada una con su `if (modoReproceso)`:

1. **No corre `validarOrdenProcesamiento`.** La última carga en estado 3 es la 450 (sep-2026); una
   carga pasada nunca sería "el mes siguiente". El orden lo garantiza el operador: se reprocesa de la
   más antigua a la más nueva (ver §6).
2. **Un rol Petro sin partícipe en el SAA no aborta ni se omite en silencio.** En la fase 3 normal
   un AH sin entidad aborta toda la carga ("no se sabe a quién pertenece el dinero") y un préstamo
   sin entidad se omite con un log. En modo reproceso los dos caminos — y el HS huérfano —
   llaman a `registrarSinParticipeReproceso`: novedad `PARTICIPE_NO_ENCONTRADO` en transacción
   propia (idempotente por fila y tipo; `montoEsperado = 0`, `montoRecibido = monto`, convención de
   §1.4.3), una línea en el resumen y el monto acumulado. Para una fila PH/PP el monto incluye el HS
   del mismo partícipe (`montoConHsMergeado`), porque ese HS tampoco se aplica por ningún otro
   camino.
3. **`validarValoresConDestino` no bloquea por esa novedad** (sí por cualquier otra bloqueante). Sin
   esto, la corrida 2 se frenaría con las novedades tipo 1 que dejó la corrida 1.
4. **`validarRecibidoIgualAplicado` compara contra el recibido neto**: transferencias − monto sin
   partícipe. El mensaje de éxito y el de error dicen cuánto se descontó y por cuántas filas.

Más el punto de §4.5 sobre el estado del partícipe, y lo agregado el 2026-10-28 (§4.7): la lista de
excedentes devueltos (D-010), que el `MONTO_INCONSISTENTE` heredado de la validación original no
bloquee, y que una fila sin partícipe no bloquee `validarValoresConDestino` aunque traiga otra
novedad (en la 352 hay una con tipo 10). Todo lo demás — recepción confirmada, tope de
afectación manual, novedades bloqueantes con AVPC, todo-o-nada — sigue exactamente igual.

### 4.7 Excedentes: cascada por defecto, aporte si se devolvió (D-010, 2026-10-28)

**Regla (decisión D-010 del líder del proyecto):** al reprocesar se compara con la documentación de
crédito; cuando Petro descontó **de más** y ASOPREP **devolvió** el excedente al partícipe, ese
excedente **no** se aplica a cuotas siguientes: va a **aportes** y después se registra la
devolución. Cuando no hubo devolución, el excedente sigue el camino del motor (es lo que de verdad
pasó: el dinero quedó en el fondo y pagó cuotas).

| Caso | Qué hace el modo reproceso |
|---|---|
| Excedente **declarado como devuelto** en la solicitud (`excedentesDevueltos`) | Se registra un aporte `EXCEDENTE_PETRO` (rubro 235 alterno 8) por ese monto: tipo cesantía (11) si el partícipe tiene vigencia de cesantía en el mes de la carga, si no jubilación (9), o el `idTipoAporte` de la línea; fecha de caja = último día del mes de la carga (`crearNuevoAporte`), **sin devengo** (no cubre ningún mes), `APRTIDAS` = carga, glosa `Excedente Petro devuelto al participe (D-010) - <producto> - <referencia> - <etiqueta> - Mes M/AAAA - CargaArchivo: N`, con su `PGAP`. Lo que queda de la fila (descontado − excedente) entra al motor / a la prelación como siempre. Si todo lo descontado era excedente (p. ej. préstamo ya cancelado), la fila termina ahí. |
| Excedente **no** declarado, préstamo (PE/PH/PQ/PP) | Cascada del motor: mora y cuotas vencidas primero, luego la cuota del mes y las siguientes. La novedad `MONTO_INCONSISTENTE` que dejó la validación de la carga original (calculada contra la tabla del SAA de cuando se subió, no contra el ancla) **no bloquea** en modo reproceso. Si el motor no encuentra cuota para todo, registra su propio `MONTO_INCONSISTENTE` y el invariante recibido == aplicado frena la corrida (nada queda grabado). |
| Excedente **no** declarado, aporte (AH) | Prelación por devengo: se anticipa a los meses siguientes (D4 del plan de devengo). |

**Contabilidad:** ningún asiento propio. El aporte entra en `sumValorPorTipoAporteByCarga`
(`APRTIDAS`), o sea al HABER de cesantía/jubilación del asiento de APLICACION (plantilla 21, aux1
50/51), y al invariante recibido == aplicado. Es el mismo camino que la afectación manual a aporte
(`aplicarAfectacionAAporte`).

**Después, la devolución:** se registra aparte, con la fecha **real** del pago al partícipe, por el
motor de devolución de aportes (`POST /rest/dvap/registrar` con `fecha`, ver
`cnt/FECHA-CONTABLE-REPROCESO.md` §3.4), por el mismo tipo de aporte y monto. El saldo del partícipe
queda: +excedente (mes de la carga) − devolución (mes en que se pagó).

**Contrato (cuerpo de `POST /rest/crar/reprocesar/{idCarga}`):**

```json
{ "usuario": "USUARIO 1", "ip": "…", "motivo": "…",
  "excedentesDevueltos": [
    { "idEntidad": 5324, "codigoProducto": "PE", "monto": 87.99,
      "idTipoAporte": 11, "referencia": "D-0xx: comprobante de devolución …" } ] }
```

`idEntidad` (código interno del partícipe) o `codigoPetro` (rol); `codigoProducto` como viene en el
archivo (`PE`, `PH`, `PQ`, `PP`, `AH`; el HS va dentro de su PH/PP); `idTipoAporte` opcional;
`referencia` obligatoria. **Se valida todo antes de aplicar nada** (`validarExcedentesDevueltosReproceso`):
la fila (rol + producto) existe en la carga; monto > 0 y ≤ lo descontado en la fila (PH/PP con su
HS); el rol tiene partícipe (sin partícipe la fila ya va como novedad, §4.4); la fila no tiene además
AVPC (serían dos destinos); el tipo de aporte se puede resolver; sin líneas repetidas. Y al final,
ningún excedente de la lista puede quedar sin aplicar. Cualquier falla → 409, nada grabado. La
respuesta trae `excedentesDevueltos`, `montoExcedentesDevueltos` y `detalleExcedentesDevueltos`.

**Reversibilidad:** el aporte `EXCEDENTE_PETRO` tiene `APRTIDAS` = carga, así que la corrida N+1 lo
reversa como cualquier otro aporte de la carga (§4.4).

**Sin confirmación de devolución** (decisión del líder, 2026-10-28): el excedente real contra el
ancla igual va a aportes por defecto, con referencia `"D-010 pendiente de confirmar"` (caso de
junio: entidad 5324, §10).

### 4.8 La operación se busca por su estado a la fecha de corte (D-019, 2026-10-28)

**Regla (decisión D-019 del líder):** el reproceso **no** toma en cuenta el estado actual del
préstamo. Una operación que estaba vigente en el mes de la carga recibe la cuota de ese mes como
ocurrió cronológicamente, aunque hoy esté cancelada anticipadamente o liquidada; esa cancelación se
vuelve a aplicar después, en su propio mes, con el saldo correcto.

Qué hace el modo reproceso (`CargaArchivoPetroServiceImpl`):

1. **Búsqueda** (`PrestamoDaoService.selectByEntidadYProductoVigentesAlCorte`, corte = último
   instante del mes de la carga) en vez de `selectByEntidadYProductoActivosById`: entra la operación
   activa hoy (1, 2, 8, 10, 11) y la terminal hoy (3, 4, 5, 9, 13) que empezó a más tardar en el
   corte, tiene alguna cuota vencida a más tardar en el corte y alguna cuota **no saldada al corte**
   (estado distinto de PAGADA/CANCELADA_ANTICIPADA, o saldada sin fecha de pago o con fecha
   posterior). Orden por código. `buscarValoresSinDestino` usa la misma búsqueda: un
   `PRESTAMO_NO_ENCONTRADO` heredado deja de bloquear si existe una operación vigente al corte.
2. **Reapertura** (`reabrirPrestamoVigenteAlCorte`) de cada operación terminal encontrada: sus
   cuotas saldadas sin fecha o después del corte se reconstruyen desde sus pagos vigentes
   (`recalcularCuotaDesdePagos`; en las migradas no hay ningún PGPR, así que vuelven a pendiente) y
   su mora se calcula al corte (`ProcesoMoraPrestamoService.recalcularMoraCuotaALaFecha`); el
   préstamo pasa a VIGENTE (el motor no admite pagos en un estado terminal). Las cuotas saldadas
   hasta el corte no se tocan.
3. **Marca** `"<etiqueta>: cancelación posterior pendiente de re-aplicar (estado anterior N)"` en
   `PRSTOBSR` y en la observación de cada evento `EVPR` no anulado con fecha posterior al corte (no
   se anula ninguno). La respuesta trae `cancelacionesPosterioresPendientes`, una línea por operación.
4. El pago del mes entra por el motor como siempre; la cancelación/liquidación **no** se re-aplica
   acá: se re-aplica al reprocesar su mes, con el saldo que dejó la cronología.

Lo mismo para las cuotas que deja libres la anulación de los pagos viejos (§4.3): su mora se calcula
al último día del mes de la carga en la misma transacción, porque el proceso de mora corrido antes
las veía pagadas.

**Consulta que lista lo pendiente de re-aplicar** (bloque 07 de
`cuadreSuper/03-trabajo/meses/2025-06/sql/08-excedentes-d010-junio.sql`):

```sql
SELECT p.PRSTCDGO, p.ENTDCDGO, p.PRSTIDST,
       REGEXP_SUBSTR(p.PRSTOBSR, 'REPROCESO carga [0-9]+ corrida [0-9]+: cancelaci.n posterior pendiente de re-aplicar \(estado anterior [0-9]+\)') marca,
       TO_CHAR(p.PRSTFCMD,'YYYY-MM-DD') modificado,
       (SELECT LISTAGG(e.EVPRCDGO||' '||e.EVPRTPOO||' '||TO_CHAR(e.EVPRFCHA,'YYYY-MM-DD'), '; ') WITHIN GROUP (ORDER BY e.EVPRFCHA)
          FROM CRD.EVPR e WHERE e.PRSTCDGO = p.PRSTCDGO AND e.EVPROBSR LIKE '%pendiente de re-aplicar%') eventos_marcados
FROM CRD.PRST p
WHERE p.PRSTOBSR LIKE '%cancelaci_n posterior pendiente de re-aplicar%'
ORDER BY p.PRSTCDGO;
```

En la 352 (simulado sobre la base del 28-oct): **318 operaciones** cumplen el criterio con una fila
de la carga — 121 hoy CANCELADO (3) y 194 CANCELADO_ANTICIPADO (4) que estaban en la G48 de mayo,
más 3 desembolsadas en junio 2025 y canceladas después. Entre ellas, las 73 de las filas tipo 10. La
mayoría de las canceladas de la G48 fueron migradas desde Delta con todas sus cuotas desde junio
marcadas PAGADA/CANCELADA_ANTICIPADA, sin pagos ni eventos en el SAA (`PRSTFCMD` jul-2025): su
"cancelación posterior" no tiene evento que re-aplicar; se re-registra al reprocesar ese mes con
la liquidación documentada. ⚠ Una fila Petro se aplica a UNA operación (la primera con cuota
pendiente, por código): si el partícipe tenía dos del mismo producto, revisar el reparto.

### 4.9 Cuota del mes saldada sin pago = pendiente al corte (D-019 ampliada, 2026-10-28)

**Por qué:** el primer intento de reprocesar la 352 se frenó en `validarValoresConDestino` con
*"hay 13 registro(s) con valores descontados sin destino definido … Novedad: CUOTA NO ENCONTRADA"*.
No eran excedentes: en los 13 la cuota de junio 2025 existe y su total es exactamente lo que Petro
descontó, pero la migración desde Delta 21 la dejó en PAGADA (4) / CANCELADA_ANTICIPADA (7) **sin
ninguna fila en `CRD.PGPR`** y con `DTPRFCPG` nula o posterior al corte. La validación 12
(`validarNovedadesFase2`) y `buscarCuotaAPagar` solo miran cuotas con estado distinto de 4 y 7, así
que no la encontraban. Tres situaciones:

| Caso | Préstamo hoy | Cuota de junio | Ejemplos (préstamo / cuota) | Antes de este cambio |
|---|---|---|---|---|
| 1 | VIGENTE (2) o DE PLAZO VENCIDO (8) | estado 4, `DTPRFCPG` nula, sin PGPR | 7578 / 2, 7584 / 2, 7690 / 1 | D-019 no los tocaba (no son terminales) |
| 2 | CANCELADO ANTICIPADO (4) | estado 4, `DTPRFCPG` nula, sin PGPR | 7591 / 2 | D-019 lo reabría recién al aplicar; la validación bloqueaba antes |
| 3 | CANCELADO (3) | estado 4, `DTPRFCPG` 2026-04-06 (> corte), sin PGPR | 3959 / 65, 3170 / 60, 4672 / 48, 5285 / 70, 6804 / 72, 6739 / 41, 6801 / 24, 3120 / 84, 3416 / 84 | igual que el caso 2 |

En la base del 28-oct hay 1.442 cuotas con vencimiento en jun-2025 saldadas sin PGPR vigente y con
fecha de pago nula o posterior al 30-jun: es la regla de la migración, no una excepción.

**Regla (solo modo reproceso):** en toda operación vigente al corte (§4.8) de una fila de la carga
con descuento, la cuota **con vencimiento dentro del mes de la carga** que esté en estado 4/7, con
`DTPRFCPG` nula o posterior al corte y **sin ningún pago vigente** (`PGPRANUL` nulo o 0, mismo
criterio que `recalcularCuotaDesdePagos`) se reconstruye con `recalcularCuotaDesdePagos` (sin pagos
vuelve a pendiente / en mora) y su mora se calcula al corte (`recalcularMoraCuotaALaFecha`). Si
tiene algún pago vigente no se toca. Método: `reabrirCuotasSaldadasSinPagoAlCorte`; lo usan las
operaciones no terminales y, como primer paso, `reabrirPrestamoVigenteAlCorte` (el cambio del
préstamo a VIGENTE y la marca de eventos siguen siendo solo para las terminales, §4.8).

**Criterio conservador del alcance:**
- Solo la cuota **del mes de la carga**. Las de meses anteriores quedan como están (las fija el
  ancla de mayo 2025 o el reproceso de su propio mes: reabrir una de mayo haría que el pago de junio
  se fuera a mayo, porque el motor paga la menor cuota pendiente). Las de meses siguientes las
  reabre el reproceso de su mes. (Para las operaciones **terminales**, D-019 sigue reconstruyendo
  además todas las saldadas sin fecha o después del corte, como antes.)
- Solo las operaciones de los partícipes con fila en la carga con descuento &gt; 0 para ese producto
  (no AH ni HS; se excluyen las filas con afectación manual — su destino lo define el AVPC — y las
  que son íntegramente excedente devuelto, D-010). El resto de la cartera lo trata el cierre de
  cartera (C15) y los meses siguientes.

**Orden nuevo en `aplicarPagosArchivoPetro` (modo reproceso):**
1. Validación de la lista de excedentes devueltos (D-010).
2. **`prepararOperacionesVigentesAlCorteReproceso`**: por cada fila con descuento, operaciones
   vigentes al corte (`prestamosVigentesAlCorteReproceso`) → regla de esta sección y, si es
   terminal, reapertura D-019. Cada préstamo se prepara una sola vez por corrida.
3. `validarValoresConDestino`: además de lo de §4.7/§4.8, un `CUOTA_NO_ENCONTRADA` heredado de la
   validación original **no bloquea** si alguna operación vigente al corte de la fila tiene ahora
   una cuota no saldada (`existeCuotaPendienteAlCorte`, mismo criterio que la validación 12). Si
   no la tiene, sigue bloqueando. El monto lo cuida el invariante recibido == aplicado.
4. Tope de afectación manual, fase 3 (el préstamo ya preparado no se vuelve a reabrir), invariante,
   asientos.

Todo en la misma transacción del reproceso: si cualquier paso posterior lanza, la reapertura
revierte junto con la anulación y la re-aplicación (§5, errores). Nada se borra.

**Trazabilidad:** el log tiene una línea por cuota (`D-019 ampliada: Préstamo N cuota M (préstamo
hoy estado E): estado 4 -> 5, fecha de pago anterior nula, sin pagos vigentes`); el resumen trae el
bloque "Cuotas del mes saldadas sin pago reabiertas al corte" y la respuesta
`cuotasSaldadasSinPagoReabiertas` (cantidad) y `detalleCuotasSaldadasSinPagoReabiertas` (las líneas).

**Consultas de control** (SQL puro; para otra carga cambiar 352 y el rango del mes).

Antes de reprocesar — las cuotas que la regla va a reabrir (las de pagos vigentes de la propia carga
no entran: esas las reconstruye la anulación, §4.3):

```sql
SELECT d.PRSTCDGO, p.PRSTIDST, d.DTPRNMCT, d.DTPRESTD,
       TO_CHAR(d.DTPRFCVN,'YYYY-MM-DD') vence, TO_CHAR(d.DTPRFCPG,'YYYY-MM-DD') fecha_pago,
       d.DTPRCPTL, d.DTPRINTR, d.DTPRDSGR, d.DTPRVLSI
FROM CRD.DTPR d
JOIN CRD.PRST p  ON p.PRSTCDGO = d.PRSTCDGO
JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
JOIN CRD.ENTD e  ON e.ENTDCDGO = p.ENTDCDGO
WHERE d.DTPRESTD IN (4, 7)
  AND d.DTPRFCVN >= DATE '2025-06-01' AND d.DTPRFCVN < DATE '2025-07-01'
  AND (d.DTPRFCPG IS NULL OR d.DTPRFCPG > TIMESTAMP '2025-06-30 23:59:59')
  AND NOT EXISTS (SELECT 1 FROM CRD.PGPR g WHERE g.DTPRCDGO = d.DTPRCDGO
                  AND (g.PGPRANUL IS NULL OR g.PGPRANUL = 0))
  AND EXISTS (SELECT 1 FROM CRD.PXCA x JOIN CRD.DTCA t ON t.DTCACDGO = x.DTCACDGO
              WHERE t.CRARCDGO = 352 AND x.PXCACDPT = e.ENTDRLPC AND x.PXCADSDO > 0.01
                AND TRIM(t.DTCACDPP) = TRIM(pr.PRDCCDPT)
                AND TRIM(t.DTCACDPP) NOT IN ('AH', 'HS'))
ORDER BY d.PRSTCDGO, d.DTPRNMCT;
```

Después de reprocesar — las cuotas del mes de esas operaciones que no tienen ningún pago fuera de
esta carga ni anulado (o sea, que antes no tenían ninguno): las que pagó el reproceso y las que
quedaron pendientes. Aproximado; la lista exacta es `detalleCuotasSaldadasSinPagoReabiertas`.

```sql
SELECT d.PRSTCDGO, p.PRSTIDST, d.DTPRNMCT, d.DTPRESTD, TO_CHAR(d.DTPRFCPG,'YYYY-MM-DD') fecha_pago,
       (SELECT COUNT(*) FROM CRD.PGPR g WHERE g.DTPRCDGO = d.DTPRCDGO AND g.CRARCDGO = 352
          AND (g.PGPRANUL IS NULL OR g.PGPRANUL = 0)) pagos_352
FROM CRD.DTPR d
JOIN CRD.PRST p  ON p.PRSTCDGO = d.PRSTCDGO
JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
JOIN CRD.ENTD e  ON e.ENTDCDGO = p.ENTDCDGO
WHERE d.DTPRFCVN >= DATE '2025-06-01' AND d.DTPRFCVN < DATE '2025-07-01'
  AND NOT EXISTS (SELECT 1 FROM CRD.PGPR g WHERE g.DTPRCDGO = d.DTPRCDGO
                  AND (g.CRARCDGO IS NULL OR g.CRARCDGO <> 352 OR g.PGPRANUL = 1))
  AND (d.DTPRESTD NOT IN (4, 7)
       OR EXISTS (SELECT 1 FROM CRD.PGPR g WHERE g.DTPRCDGO = d.DTPRCDGO AND g.CRARCDGO = 352
                  AND (g.PGPRANUL IS NULL OR g.PGPRANUL = 0)))
  AND EXISTS (SELECT 1 FROM CRD.PXCA x JOIN CRD.DTCA t ON t.DTCACDGO = x.DTCACDGO
              WHERE t.CRARCDGO = 352 AND x.PXCACDPT = e.ENTDRLPC AND x.PXCADSDO > 0.01
                AND TRIM(t.DTCACDPP) = TRIM(pr.PRDCCDPT)
                AND TRIM(t.DTCACDPP) NOT IN ('AH', 'HS'))
ORDER BY d.PRSTCDGO, d.DTPRNMCT;
```

## 5. Respuesta (`ResultadoReprocesoCargaPetro`)

```json
{
  "idCarga": 352, "mesAfectacion": 6, "anioAfectacion": 2025, "fechaContable": "2025-06-30",
  "corrida": 1, "etiqueta": "REPROCESO carga 352 corrida 1",
  "pagosAnulados": 1313, "montoPagosAnulados": 319328.38, "pagosAnuladosSinEnlace": 1313,
  "aportesReversados": 2638, "montoAportesReversados": 160524.89, "aportesReversadosSinEnlace": 2635,
  "pagosAporteInactivados": 3, "cuotasRecalculadas": 1313, "prestamosReabiertos": 0,
  "asientosAnulados": [], "asientoReProvision": null,
  "pagosAplicados": 0, "montoPagosAplicados": 0.0, "aportesAplicados": 0, "montoAportesAplicados": 0.0,
  "asientosGenerados": [], "resumenAplicacion": "=== RESUMEN APLICACIÓN DE PAGOS === ...",
  "filasSinParticipe": 14, "montoSinParticipe": 4872.04, "detalleSinParticipe": ["Rol … (AH): $… sin partícipe en el SAA", "…"],
  "cancelacionesPosterioresPendientes": ["Préstamo … (estado anterior 4): … cuota(s) reabiertas, … evento(s) posteriores marcados"],
  "cuotasSaldadasSinPagoReabiertas": 0, "detalleCuotasSaldadasSinPagoReabiertas": ["Préstamo … cuota … (préstamo hoy estado 2): estado 4 -> 5, fecha de pago anterior nula, sin pagos vigentes"],
  "mensaje": "Carga 352 reprocesada (REPROCESO carga 352 corrida 1): …"
}
```
(Los valores "anulado" de arriba son los medidos en §2; los "aplicado" se llenan con la corrida real
en WildFly.) `aportesAplicados`/`montoAportesAplicados` se miden con el mismo filtro del asiento de
aplicación (`APRTIDAS = idCarga`): el neto de originales reversados y sus contra-movimientos es cero,
así que lo que queda es lo de esta corrida. `asientosGenerados` son los `ANCP` vigentes de
REPARTO/APLICACION después de la corrida (vacío si la contabilidad de CRD está apagada — no es error).

Errores: 409 (`IncomeException`, precondición de negocio: nada quedó grabado) o 500. En los dos casos
la transacción revierte completa: no existe el estado "anulado pero no re-aplicado".

## 6. Reglas de uso

1. **Orden cronológico, de la más antigua a la más nueva**, y sólo después de C3 (volver al ancla
   de mayo 2025). La prelación de aportes reparte "desde el primer mes incompleto": si la 353 (julio)
   todavía tiene sus aportes viejos cuando se reprocesa la 352, un excedente de junio se anticiparía
   más allá de julio. Y el motor de pagos paga "la menor cuota pendiente": los pagos viejos de las
   cargas posteriores tienen que estar anulados (C3) antes de re-aplicar junio.
2. **Paso 1 primero.** Registrar las transferencias reales con que Petro pagó ese mes y confirmar la
   recepción. El asiento TRANSITORIO (D banco / H 2.3.01.15.01) se fecha solo al último día del mes
   de la carga.
3. **Período contable abierto** para el mes de la carga (los de 2025 existen desde C1). Si está
   mayorizado, los asientos nuevos fallan y la transacción revierte.
4. **Rubro de contabilidad de CRD** (`ConfiguracionContabilidadService.contabilidadActiva`): apagado,
   el reproceso igual aplica pagos y aportes y reescribe bandas, pero no genera asientos. Para el
   proyecto tiene que estar encendido.
5. **Tiempo:** una carga de ~3.600 partícipes más la anulación de ~4.000 filas corre en una sola
   transacción; el timeout de transacciones de WildFly del proyecto está en 15 min (REGLAS-CARGA-PETRO
   §5). Medir la primera corrida de la 352 y subirlo si hace falta antes de pasar a producción. La
   primera corrida (28-oct) no terminó en 35 min por un costo cuadrático del contexto de
   persistencia, corregido en §11; ahí están los tiempos de referencia y cómo leer la medición.
6. **Reversibilidad:** todo lo anulado por una corrida se encuentra con la etiqueta
   (`PGPRMTAN LIKE 'REPROCESO carga 352 corrida 1%'`, `APRTGLSA LIKE 'REPROCESO carga 352 corrida 1%'`,
   `ASNTMTAN` ídem). No hay "des-reproceso" automático: si una corrida dejó algo mal, se corrige el
   dato de origen y se vuelve a reprocesar (corrida N+1), que anula lo de la N.

## 7. Criterio de aceptación (carga 352)

Consultas antes/después en [sql/reproceso-carga-352-controles.sql](sql/reproceso-carga-352-controles.sql),
bloques B y C. Después de reprocesar:

| # | Control | Esperado |
|---|---|---|
| 1 | Pagos vigentes con `CRARCDGO = 352` + aportes netos con `APRTIDAS = 352` (jubilación + cesantía) + seguro de incendio (incluido en los pagos) | **535.477,39 − 4.872,04 = 530.605,35** (± 0,01; si algún otro partícipe quedara sin destino, el proceso aborta antes y lo dice) |
| 2 | Pagos vigentes de la 352: fecha | todos **30-jun-2025** (`TRUNC(PGPRFCHA) = DATE '2025-06-30'`), todos con `CRARCDGO = 352` y `PRSTCDGO` no nulo |
| 3 | Pagos del esquema viejo | los 1.313 con `PGPRANUL = 1` y `PGPRMTAN` = etiqueta; **cero** vigentes con `[CargaArchivo: 352]` y `CRARCDGO` nulo |
| 4 | Aporte de junio por partícipe | para cada rol del producto AH con entidad: `SUM(APRTVLRR)` con `APRTIDAS = 352` y `TPAPCDGO IN (9,11)` = `PXCADSDO` de su fila AH; total **161.257,56 en 2.061 partícipes** (los 7 roles AH sin partícipe, 746,74, quedan en novedad) |
| 5 | Aportes del esquema viejo | 2.635 con `APRTIDST = 0` y 2.635 contra-movimientos REVERSO con la etiqueta, misma fecha de caja y devengo; `SUM` neto por entidad/tipo/devengo de (original + reverso) = 0 |
| 6 | Asientos | `ANCP` vigentes REPARTO y APLICACION de la carga con `ASNTFCHA = 30-jun-2025` y `PRDOCDGO` = período 6/2025 (4524); el de APLICACION cuadra D = H y su HABER de aportes = control 4 |
| 7 | Novedades | 14 filas `NVPC` tipo 1 (una por fila PXCA sin partícipe), `montoRecibido` suma 4.872,04 |
| 8 | Idempotencia | una segunda corrida deja los controles 1, 2, 4, 6 y 7 idénticos (con corrida 2 en las etiquetas, y los aportes/pagos de la corrida 1 anulados) |

> Nota sobre el control 4: la prelación reparte lo recibido por devengo. Si para algún partícipe el
> esperado de junio 2025 según `CRD.VGCN` es menor que lo que Petro descontó, el excedente se
> anticipa a julio (`APRTPRDV = 01-jul-2025`) y el `SUM` por partícipe **con `APRTIDAS = 352`** sigue
> cuadrando con el retorno (control 4 compara por carga, no por devengo). El detalle por devengo se
> lista aparte en el bloque C para revisarlo contra la regla del proyecto.

## 8. Archivos

| Archivo | Qué |
|---|---|
| `ejb/asoprep/service/ReprocesoCargaPetroService.java`, `serviceImpl/ReprocesoCargaPetroServiceImpl.java` | orquestación: anulación + re-aplicación |
| `ejb/asoprep/service/CargaArchivoPetroService.java`, `serviceImpl/CargaArchivoPetroServiceImpl.java` | `aplicarPagosArchivoPetroReproceso`, campo `modoReproceso`, `registrarSinParticipeReproceso`, los cuatro `if (modoReproceso)` |
| `ejb/crd/dao/PagoPrestamoDaoService(+Impl)` | `selectVigentesLegadoPorObservacionCarga`, `selectMaxCorridaReproceso` |
| `ejb/crd/dao/AporteDaoService(+Impl)` | `selectVigentesLegadoPorGlosaCarga`, `selectMaxCorridaReproceso` |
| `ejb/crd/service/dto/SolicitudReprocesoCargaPetro`, `ResultadoReprocesoCargaPetro` | contrato REST |
| `ws/rest/crd/CargaArchivoRest.java` | `POST /rest/crar/reprocesar/{idCarga}` |
| `docs/logica-negocio/petro/sql/reproceso-carga-352-controles.sql` | controles antes/después |
| `ejb/crd/service/dto/ExcedenteDevueltoReproceso` | línea de la lista D-010 (§4.7) |
| `ejb/crd/service/dto/SolicitudConfirmarRecepcion` + `CobroPetroContableServiceImpl.confirmarRecepcion` | `paraReproceso`: paso 1 sobre una carga ya en estado 3 (§9) |
| `ejb/crd/serviceImpl/ProcesoMoraPrestamoServiceImpl` + `DetallePrestamoDaoService(+Impl)` | mora con fecha de negocio pasada retira la mora de cuotas aún no vencidas (paso 0, §9); `recalcularMoraCuotaALaFecha` para las cuotas que reabre el reproceso |
| `ejb/crd/dao/PrestamoDaoService(+Impl)` | `selectByEntidadYProductoVigentesAlCorte` (D-019, §4.8) |
| `serviceImpl/CargaArchivoPetroServiceImpl.java` | D-019 ampliada (§4.9): `prepararOperacionesVigentesAlCorteReproceso`, `reabrirCuotasSaldadasSinPagoAlCorte`, `existeCuotaPendienteAlCorte`; respuesta `cuotasSaldadasSinPagoReabiertas` / `detalleCuotasSaldadasSinPagoReabiertas` en `ResultadoReprocesoCargaPetro` |
| `basico/ejb/ContextoPersistenciaService` + `basico/ejbImpl/ContextoPersistenciaServiceImpl` | rendimiento (§11): tamaño e histograma del contexto de persistencia, flush + detach por clase, auto-flush suspendido en fases de solo lectura |

## 9. Secuencia de junio 2025 (carga 352) — lista para correr

Base de trabajo (`saa-oracle-23ai`, WildFly del líder en 8080). Usuario `USUARIO 1` (`SCP.PJRQ`
código 38: los servicios reciben el **nombre**, que `confirmarRecepcion` resuelve con
`selectByNombre`; un nombre inexistente revienta con `NoResultException`). Antes: C1, C2 y C3
aplicados, este código desplegado, período 6/2025 (4524) abierto y la contabilidad de CRD activa.

**Paso 0 — mora al 31-may-2025.** El proceso de mora ya aceptaba fecha de negocio
(`POST /rest/prst/calcularMora?fecha=…&usuario=…`, `ProcesoMoraPrestamoServiceImpl.calcularMoraDiaria`);
lo que no hacía era retirar la mora que una corrida posterior dejó en cuotas que a esa fecha no
vencían — en la base del 28-oct hay **146 cuotas con vencimiento 31-may/jun-2025 (4.830,04)** y
**2.695 con vencimiento desde jul-2025 (34.987,23)** con mora calculada al 28-oct-2026 por el
timer de producción; sin el cambio, la cascada de junio cobraría esa mora. Cambio mínimo hecho:
con fecha pasada, esas cuotas quedan con mora 0 y EN_MORA → PENDIENTE (`PROCESO-DIARIO-INTERES-MORA.md` §8).
Las cuotas vencen el 30/31 de cada mes y el corte es el inicio del día: al 31-may la cuota de
mayo todavía no está vencida.

Decisión del líder (2026-10-28): el paso 0 al **31-may** fija el ancla, y **justo antes del
reproceso** la mora se calcula a la fecha del cobro, **30-jun-2025** (paso 1b). Las cuotas que el
reproceso reabre (pagos anulados, D-019) calculan su mora al 30-jun dentro del propio reproceso.

```http
POST /SaaBE/rest/prst/calcularMora?fecha=2025-05-31&usuario=USUARIO%201
```

⚠ El timer de las 02:00 (`ProcesoMoraPrestamoTimer`) recalcula a la fecha del día y deshace los
pasos 0 y 1b: correr todo en la misma sesión; si pasa una noche, repetir 0 y 1b.

**Paso 1 — el dinero de Petro entró al banco** (Pacífico ahorros 1048142735, SAA cuenta bancaria
418, plan `1.1.02.05.65`; dos abonos SPI, `cuadreSuper/03-trabajo/bancos/PETRO-VS-BANCO.md`):

```http
POST /SaaBE/rest/asgn/transferencias
{ "idCarga": 352, "idCuentaBancaria": 418, "valor": 77975.32, "fecha": "2025-06-26",
  "numero": "1899332", "cuentaOrigen": "Petroecuador SPI 53*Ocp*083390621000045",
  "observacion": "Retorno Petro junio 2025 - abono 1 de 2 (extracto Pacifico ahorros)",
  "usuario": "USUARIO 1" }

POST /SaaBE/rest/asgn/transferencias
{ "idCarga": 352, "idCuentaBancaria": 418, "valor": 457502.07, "fecha": "2025-06-30",
  "numero": "3815029", "cuentaOrigen": "Petroecuador SPI 53*Ocp*083508076000007",
  "observacion": "Retorno Petro junio 2025 - abono 2 de 2 (extracto Pacifico ahorros)",
  "usuario": "USUARIO 1" }

GET  /SaaBE/rest/asgn/transferencias/352          → totalTransferencias 535477.39, cuadra true

POST /SaaBE/rest/asgn/confirmarRecepcion/352
{ "usuario": "USUARIO 1", "ip": "127.0.0.1", "paraReproceso": true,
  "observacion": "Cuadre Superintendencia C4 - paso 1 de la carga de junio 2025 antes del reproceso" }
```

Fechas, verificadas en el código el 2026-10-28: la transferencia guarda la fecha real del abono
(`TRCRFCHA`); el asiento TRANSITORIO (D 1.1.02.05.65 por cada abono / H 2.3.01.15.01 535.477,39)
se fecha `ultimoDiaMesAnioLocal(6, 2025)` = **30-jun-2025** (período 4524, tipo IN-A por tocar un
banco real, C12) y cada línea del DEBE lleva "recibida 2025-06-26/30" en su descripción. Solo
`CRARFCAC` y las fechas de registro de la transferencia y de la fila `ANCP` llevan la fecha de
hoy: son sellos de auditoría (cuándo se registró), no fechas contables. No hizo falta fecha contable opcional (patrón C6).

**Paso 1b — mora a la fecha del cobro:**

```http
POST /SaaBE/rest/prst/calcularMora?fecha=2025-06-30&usuario=USUARIO%201
```

**Paso 2 — reproceso:**

```http
POST /SaaBE/rest/crar/reprocesar/352
{ "usuario": "USUARIO 1", "ip": "127.0.0.1",
  "motivo": "Cuadre Superintendencia C4: re-aplicar junio 2025 sobre el ancla de mayo (D-003, D-010, D-019)",
  "excedentesDevueltos": [
    { "idEntidad": 5324, "codigoProducto": "PE", "monto": 87.99,
      "referencia": "D-010 pendiente de confirmar" } ] }
```

La línea de la entidad 5324 es la decisión del líder del 28-oct (§10). Sin `idTipoAporte`: cesantía
si tiene vigencia de cesantía en junio 2025, si no jubilación. Pagos, aportes, REPARTO y APLICACION
salen al 30-jun-2025 (§4.6); las operaciones reabiertas por D-019 vienen en
`cancelacionesPosterioresPendientes` (§4.8).

**Paso 3 — controles:** bloques B y C de `sql/reproceso-carga-352-controles.sql` (§7).

**Después del mes (no es parte del reproceso):** cada devolución D-010 por `POST /rest/dvap/registrar`
con la fecha real del pago; cierre de cartera de junio (`POST /rest/cierrecartera/ejecutar`, provisión e
intereses al 30-jun); listar las cancelaciones posteriores pendientes (§4.8) para re-aplicarlas en
su mes. La carga de julio (353) repite la secuencia con mora al 30-jun (ancla del mes) y al 31-jul
(cobro) y sus dos abonos (25-jul 75.855,40 y 28-jul 438.107,13).

## 10. Junio 2025 frente a la documentación de crédito (D-010) — medido el 2026-10-28

Cruce de las filas de préstamo de la 352 con la deuda de cada operación en el ancla (`RPR.HM48`:
`VPVN + VLVN + INRD + ISMR`) y con la documentación del cliente (`08-Cartera/`: retornos y enviados
Petro, `ABONOS Y LIQ/`, `LIQUIDACIONES - PLAZO VENCIDO/`, `DEVOLUCIONES CESANTIAS Y CRUCE DE
CUENTAS/PAGOS REALIZADOS … .xlsx`). Consultas en `cuadreSuper/03-trabajo/meses/2025-06/`.

- **En ninguna fila Petro descontó más de lo que ASOPREP pidió** (`PXCADSDO <= PXCADSCT` en las
  3.636 filas). El "de más" solo existe contra el SAA.
- **Contra la deuda del ancla**, solo hay excedente real en 5 filas: 4 de roles **sin partícipe** en
  el SAA (van como novedad, §4.4) y **1** con partícipe (entidad 5324, PE, descontado 204,79 contra
  una deuda de 116,80 en la única operación que tenía en la G48 → **87,99**).
- **Devoluciones documentadas de junio 2025: ninguna.** El libro de devoluciones empieza en
  oct-2025 y no incluye a la entidad 5324 ni a los 11 partícipes cuyo aporte de junio se anticipó a
  julio (14 filas, 763,13: siguen activos y aportando, el anticipo es legítimo). No hay en el SAA
  aportes de devolución ni pagos que citen un sobrante de junio de un rol Petro (el `PAGO_MANUAL`
  de 174,58 de jul-2026, "sobrante de junio 2025 a abril 2026", es de un partícipe **sin rol
  Petro** que paga por transferencia: no pertenece a la carga 352 y se revisa en jul-2026).
- **Devoluciones documentadas: 0.** Por decisión del líder (28-oct), el excedente real de la
  entidad 5324 (PE, **87,99**) va igual a aportes por defecto (D-010), con referencia
  `"D-010 pendiente de confirmar"`: **lista de junio = 1 línea, 87,99** (§9, paso 2;
  `cuadreSuper/03-trabajo/meses/2025-06/EXCEDENTES-D010-2025-06.json`).

**Novedades heredadas de la validación de marzo-2026** y cómo las trata el modo reproceso:

| Novedad heredada | Filas | Descontado | Qué es | Tratamiento |
|---|---|---|---|---|
| tipo 10 PRESTAMO_NO_ENCONTRADO, con partícipe | 74 | 20.668,15 | La operación **estaba en la G48 de mayo con deuda** (73 hoy en CANCELADO_ANTICIPADO 4, migradas así desde Delta en jul-2025, con la cuota de junio marcada PAGADA/CANCELADA_ANTICIPADA sin ningún pago; 1 en DE_PLAZO_VENCIDO 8); la 352 vieja nunca les aplicó nada | D-019 (§4.8): se buscan por su estado al 30-jun-2025, se reabren, reciben la cuota de junio y su cancelación queda marcada para re-aplicarla en su mes |
| tipo 13 MONTO_INCONSISTENTE positiva | 105 | 39.017,30 (14.675,35 sobre la cuota del SAA) | Petro descontó la cuota de Delta, mayor que la del SAA o con vencidos; contra el ancla solo la 5324 supera la deuda | §4.7: va a la cascada (la 5324: 87,99 a aporte) |
| sin partícipe con tipo 10/13 | 2 | 1.980,07 | — | Van como sin partícipe (§4.4) |

## 11. Rendimiento (2026-10-28)

### 11.1 Qué pasó en la primera corrida de la 352

`POST /crar/reprocesar/352` corrió más de 35 minutos sin llegar a la fase 3. Tiempos medidos por
fase, todo en la misma transacción JTA:

| Fase | Volumen | Tiempo medido |
|---|---|---|
| anulación (pagos + aportes) | 1.313 pagos, 2.640 aportes | 1 min |
| `prepararOperacionesVigentesAlCorteReproceso` | 1.277 filas, 1.304 operaciones, 200 terminales reabiertas (~7.000 cuotas recalculadas), 102 cuotas del mes | **16 min** |
| `validarValoresConDestino` | 3.184 consultas de novedades por partícipe | **7 min** |
| `validarTopeAfectacionManualPorParticipe` | ídem | más de 10 min, sin terminar |
| fase 3 (aplicar 1.314 filas de préstamo + AH) | — | no empezó |

Síntomas: la latencia de la misma llamada crecía con el tiempo (39 ms a las 17:56 → 115 → 184 →
422 ms a las 18:27), la JVM ocupaba exactamente un núcleo y Oracle estaba ocioso. El `jstack` del
hilo `default task-2` lo mostraba en
`DefaultFlushEntityEventListener.onFlushEntity ← AbstractFlushingEventListener.flushEntities ←
DefaultAutoFlushEventListener.onAutoFlush ← SessionImpl.autoFlushIfRequired ←
ConcreteSqmSelectQueryPlan.performList ← DetallePrestamoDaoServiceImpl.contarCuotasByPrestamo`.

### 11.2 Causa

Con `FlushMode.AUTO` (el de siempre), **antes de cada consulta JPQL Hibernate recorre todas las
entidades administradas** para ver si hay cambios que volcar (dirty-check), aunque no haya ninguno.
Los DAO comparten el contexto de persistencia de la transacción (`SaaPU`, `@PersistenceContext`
transaccional en cada `*DaoServiceImpl`, `save` = `persist`/`merge` de `EntityDaoImpl`), y en el
reproceso todo es una transacción: cada cuota leída con `selectByPrestamo`, cada pago, aporte,
novedad, préstamo y entidad (todas las `@ManyToOne` son EAGER) se queda en el contexto hasta el
commit. Costo por consulta proporcional al tamaño del contexto × miles de consultas = cuadrático.
No es Oracle ni el SQL: es CPU de la JVM.

### 11.3 Qué se cambió (sin cambiar ningún resultado)

**1. Contexto acotado por unidad de trabajo — flush + detach** (`ContextoPersistenciaService.volcarYDesprender`).
Primero `em.flush()` (todo lo pendiente va a la base, como iba a ir de todos modos en la próxima
consulta o en el commit) y después `detach` de las instancias administradas de las clases de vida
corta (`CargaArchivoPetroServiceImpl.CLASES_DESPRENDIBLES_REPROCESO`): `DetallePrestamo`,
`PagoPrestamo`, `Prestamo`, `EventoPrestamo`, `Aporte`, `PagoAporte`, `NovedadParticipeCarga`,
`AfectacionValoresParticipeCarga`, `HistorialSueldo`, `Contrato`, `VigenciaContrato`, `Entidad`,
`ParticipeXCargaArchivo`. **No** se desprenden `CargaArchivo`, `DetalleCargaArchivo`, `Producto`
ni los catálogos. No se usa `em.clear()`: desprendería también lo de larga vida.

Puntos de flush/detach (todos solo en modo reproceso):

| Dónde | Cuándo | Qué cruza el borde (y por qué es seguro) |
|---|---|---|
| `ReprocesoCargaPetroServiceImpl.reprocesar`, tras `anularPagosPrestamo` | siempre (borde de fase) | solo `carga` (no se desprende) y `transferenciasVigentes` (no se desprende ni se vuelve a usar). Los pagos, cuotas y préstamos del método eran variables locales |
| ídem, tras `reversarAportes` | siempre | ídem |
| `prepararOperacionesVigentesAlCorteReproceso`, al terminar cada fila | si el contexto pasa de `UMBRAL_CONTEXTO_REPROCESO` (2.000) | `detalles` y `productosCache` (no se desprenden), `participes` (se desprenden pero solo se leen). Los préstamos ya preparados se recuerdan por **id** (`prestamosPreparadosReproceso`), nunca por instancia. Nunca se desprende a mitad de una fila: dentro de `reabrirPrestamoVigenteAlCorte` todas las cuotas son las mismas instancias |
| `aplicarPagosArchivoPetro`, tras preparar | siempre | `cargaArchivo` (no se desprende) |
| ídem, tras `validarTopeAfectacionManualPorParticipe` | siempre | ídem |
| fase 3, al terminar cada partícipe (después de su `try`) | si el contexto pasa de 2.000 | `cargaArchivo`, `detallesCarga`, `productosCache` (no se desprenden), `participesDetalle` (se desprenden; en modo reproceso nada en la fase 3 los modifica: verificado, los únicos `participe.set…` del archivo son de la fase 1 y de `actualizaCodigoPetroEntidad`), `esperadoEnLoteAportes`/`entidadesCubiertasAportes` (no son entidades). Nunca a mitad de un partícipe: ahí el motor, `buscarCuotaAPagar` y `verificarYActualizarEstadoPrestamos` comparten las mismas instancias de préstamo y cuota |
| ídem, al terminar la fase 3 | siempre | `cargaArchivo` |

Por qué no puede cambiar un resultado:
- **Ninguna consulta ve algo distinto.** Antes de desprender se vuelca, así que la base tiene el
  mismo estado que tenía la memoria; una consulta posterior devuelve instancias nuevas con ese
  estado. Volcar antes de tiempo solo adelanta SQL que el auto-flush o el commit iban a ejecutar
  (las consultas JPQL ya volcaban lo de sus tablas; las nativas, todo).
- **Ninguna escritura se pierde.** Lo único que podría perderse es una modificación hecha *después*
  del borde sobre una instancia desprendida sin volver a guardarla. En los bordes elegidos ninguna
  variable que cruce modifica una entidad desprendible (tabla de arriba); además todo el módulo
  escribe por `save` (= `merge`), que sobre una instancia desprendida copia su estado a la
  administrada. Los campos de instancia del bean `@Stateful` son listas de texto, ids y DTO, no
  entidades.
- **Sin cascadas ni colecciones**: el modelo de `crd` no declara `cascade` ni `@OneToMany`, así que
  `detach` afecta solo a esa instancia, y una entidad nueva que apunta a una desprendida se inserta
  igual (usa su id; todas las PK son IDENTITY/SEQUENCE, Hibernate la reconoce como desprendida sin
  ir a la base). Ninguna clase conservada apunta a una desprendible.
- La transacción sigue siendo una sola: el todo-o-nada (§5) no cambia.

**2. Auto-flush suspendido en las dos validaciones de solo lectura** (`suspenderAutoFlush` /
`restaurarAutoFlush`, `FlushMode.MANUAL` acotado). `validarValoresConDestino` (con
`existePrestamoVigenteAlCorte`, `existeCuotaPendienteAlCorte`, `novedadesQueRequierenAfectacion`,
`totalAfectadoManualmente`) y `validarTopeAfectacionManualPorParticipe` (con
`calcularTopeAfectacionManual`, `disponibleParaTope`, `cedulaPorRolPetro`,
`codigosAfectacionManual`) **solo consultan**: ninguna escribe (verificado método por método). Al
entrar se vuelca todo y se pone MANUAL; al salir (en `finally`) se restaura el modo y se comprueba
`Session.isDirty()`. Como no hay nada pendiente y la fase no escribe, una consulta con AUTO no
tendría nada que volcar: el resultado es idéntico, solo se ahorra el recorrido. Si algún día una
de esas validaciones empezara a escribir, `restaurarAutoFlush` lo vuelca y deja
`ADVERTENCIA [contexto de persistencia]: la fase de solo lectura '…' dejó cambios pendientes` en el
log (revisar el supuesto). **No** se usa en `prepararOperaciones…` ni en la fase 3: ahí hay
consultas que dependen de lo escrito en la misma fase (`buscarSiguienteCuotaConSaldo` después de
pagar una cuota, `contarCuotasPendientesByPrestamo` después de saldarla, `selectVigentesByIdDetallePrestamo`
después de anular).

**3. Consultas repetidas (N+1) memoizadas, sin cambio de resultado:**
- `reabrirPrestamoVigenteAlCorte` leía dos veces `selectByPrestamo` del mismo préstamo (una en
  `reabrirCuotasSaldadasSinPagoAlCorte` y otra para su segunda pasada). Ahora la lee una vez y pasa
  la lista: son las mismas instancias administradas y el mismo orden que devolvía la segunda
  consulta (la consulta no filtra por estado; el contexto devuelve la instancia que ya tiene).
- `verificarYActualizarEstadoPrestamo` (fase 3) memoiza `contarCuotasByPrestamo` por préstamo
  dentro de la corrida (`totalCuotasPorPrestamoReproceso`, reset al entrar). Es `COUNT(*)` de
  `CRD.DTPR` por préstamo y el reproceso nunca inserta ni borra cuotas. El
  `contarCuotasPendientesByPrestamo` (que sí cambia) se sigue consultando siempre.

Nada de esto toca el flujo normal de una carga nueva: todo está detrás de `modoReproceso`.

### 11.4 Cómo leer la próxima corrida

Cada fase deja dos líneas en `server.log`:

```
[REPROCESO-RENDIMIENTO] REPROCESO carga 352 corrida N | <fase> | INICIO | contexto <n> entidades
[REPROCESO-RENDIMIENTO] REPROCESO carga 352 corrida N | <fase> | FIN | <s> s | contexto <n> entidades (<top 6 clases>) | ...
```

Fases: `anular asientos REPARTO/APLICACION`, `anular pagos de préstamo`, `reversar aportes`,
`re-aplicación (aplicarPagosArchivoPetroReproceso)` y, dentro de ella, `preparar operaciones
vigentes al corte`, `validarValoresConDestino`, `validarTopeAfectacionManualPorParticipe`,
`fase 3: aplicar filas`, `validarRecibidoIgualAplicado`, `asientos REPARTO y APLICACION`; al final
`medir lo aplicado` y `TOTAL`. Para seguirla en vivo:
`Select-String -Path <wildfly>\standalone\log\server.log -Pattern 'REPROCESO-RENDIMIENTO'`.

Lo esperado: el contexto no pasa de unos pocos miles de entidades al INICIO de cada fase y la
latencia por llamada ya no crece con el tiempo. Referencia (estimación, a confirmar con la
medición): preparar 1–3 min, cada validación menos de 1 min, fase 3 entre 3 y 8 min, invariante
y asientos 1–2 min; la corrida completa del orden de 10–15 min. **Sigue estando cerca del timeout
de 15 min de WildFly** (§6, regla 5): subir el timeout de transacciones antes de la corrida.

### 11.5 Cómo se confirma que el resultado no cambió

Los mismos controles de §7 (bloques B y C de
[sql/reproceso-carga-352-controles.sql](sql/reproceso-carga-352-controles.sql)) y los de §4.9: el
cambio es solo de cuándo se vuelca y qué queda en memoria, así que los esperados no cambian —
530.605,35 aplicado (control 1), pagos al 30-jun con `CRARCDGO = 352` (2), 1.313 pagos viejos
anulados (3), 161.257,56 de aportes en 2.061 partícipes (4), reversos netos en cero (5), asientos
con D = H (6), 14 novedades sin partícipe por 4.872,04 (7). En la respuesta,
`cuotasSaldadasSinPagoReabiertas`, `cancelacionesPosterioresPendientes` y `detalleSinParticipe`
tienen que ser los de una corrida sin este cambio. Y en el log no debe aparecer ninguna
`ADVERTENCIA [contexto de persistencia]`.

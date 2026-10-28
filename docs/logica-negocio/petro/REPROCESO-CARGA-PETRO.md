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
3 normal; no se saltea.

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

Más el punto de §4.5 sobre el estado del partícipe. Todo lo demás — recepción confirmada, tope de
afectación manual, novedades bloqueantes con AVPC, todo-o-nada — sigue exactamente igual.

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
   §5). Medir la primera corrida de la 352 y subirlo si hace falta antes de pasar a producción.
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

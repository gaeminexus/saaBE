# Caja chica — fondo fijo con gastos, reposición, adjuntos, alerta y cierre

> Fecha: 2026-08-27. Backend: `com.saa.ws.rest.tsr.CajaChicaRest` (`/cjch`),
> `com.saa.ws.rest.tsr.MovimientoCajaChicaRest` (`/mvch`), `com.saa.ws.rest.tsr.CierreCajaChicaRest` (`/crch`),
> `com.saa.ws.rest.tsr.PathCajaChicaRest` (`/ptch`).
> DDL: `docs/logica-negocio/tsr/sql/02-caja-chica.sql` (bloques 1-5 ejecutados; el bloque 6 —
> retiro de las cuentas bancarias legadas 428/429 — se ejecuta después, ver §7).
> Requiere el prompt 02 de cheques terminado: la reposición se paga por `PagoProgramado` y
> puede ir con cheque (ver `docs/logica-negocio/tsr/CHEQUES.md`).

## 1. Modelo

- **CajaChica** (`TSR.CJCH`): empresa, nombre (único por empresa), cuenta contable propia
  (`planCuenta`), `montoFondo` (límite del fondo fijo), `montoMaximoGasto` (tope por gasto
  individual, opcional), `porcentajeAlerta` (default 20), responsable, custodio, estado
  (1=Activa, 2=Inactiva — flag simple, no catalogado en SCP.PRBR).
- **MovimientoCajaChica** (`TSR.MVCH`): caja, tipo (rubro 232), fecha, valor (siempre
  positivo), descripción, observación, producto CXP (sólo gastos), titular/beneficiario
  (opcional), número de documento, asiento, pago programado (sólo apertura/reposición desde
  banco), cierre, estado (1=Activo, 2=Anulado), motivo de anulación.
- **CierreCajaChica** (`TSR.CRCH`): caja, fecha, periodo (`fechaInicio`/`fechaFin`), saldo
  inicial, totales del periodo, saldo según libros, saldo físico, diferencia, observación,
  estado (rubro 233), asiento de ajuste.
- **PathCajaChica** (`TSR.PTCH`): adjuntos (comprobantes digitalizados) de un movimiento.

### El saldo NUNCA se guarda

El saldo de una caja se calcula siempre en el momento, sumando/restando los movimientos
**ACTIVOS**:

```
saldo = Σ(APERTURA + REPOSICION + AJUSTE_POSITIVO) − Σ(GASTO + AJUSTE_NEGATIVO)
```

`GET /cjch/saldo/{id}` devuelve el saldo, el porcentaje respecto al fondo, si está en alerta
(`saldo <= fondo * porcentajeAlerta / 100`), el monto sugerido de reposición
(`max(0, fondo − saldo)`) y la fecha del último cierre.

### Tipos de movimiento (rubro 232)

| Código | Tipo | Efecto en el saldo | Genera asiento |
|---|---|---|---|
| 1 | APERTURA | Suma | Sólo si se paga desde banco (no en la apertura migrada) |
| 2 | GASTO | Resta | Sí, en el acto |
| 3 | REPOSICION | Suma | Sí, cuando el pago se confirma |
| 4 | AJUSTE_POSITIVO (sobrante) | Suma | Sí, al confirmar el cierre |
| 5 | AJUSTE_NEGATIVO (faltante) | Resta | Sí, al confirmar el cierre |

### Estados del cierre (rubro 233)

| Código | Estado | Significado |
|---|---|---|
| 1 | BORRADOR | En preparación; el usuario todavía no ingresó el saldo físico |
| 2 | CERRADO | Confirmado; sus movimientos quedan bloqueados para anular |
| 3 | ANULADO | El cierre se deshizo (sólo se puede anular el último CERRADO) |

## 2. Gasto de caja chica

`POST /mvch/gasto` valida, en este orden: caja activa; `observacion` no vacía;
`valor > 0`; `descripcion` no vacía; producto obligatorio con grupo y cuenta contable
configurada; `valor <= saldo` disponible; `valor <= montoMaximoGasto` si está definido;
`fecha` posterior al `fechaFin` del último cierre CERRADO (si no hay cierres, cualquier
fecha vale).

Contabiliza en el acto — `AsientoContableServiceImpl.generarAsientoGastoCajaChica`:

- **DEBE**: cuenta del grupo del producto CXP — descripción `"Gasto caja chica: {descripcion}"`.
- **HABER**: cuenta contable de la caja — descripción `"Caja chica {nombreCaja}: {descripcion}"`.
- Tipo de asiento: `TipoAsientos.EGRESO_TESORERIA` (codigoAlterno 5, T-EGRESOS), módulo TESORERIA.
- Observación de cabecera: `"Gasto caja chica {nombre} | {descripcion} | Doc: {numeroDocumento} | Valor: $x"`.

`POST /mvch/anular/{id}` anula el asiento y marca el movimiento ANULADO — sólo si es tipo
GASTO, está ACTIVO y no quedó incluido en un cierre CERRADO. Las aperturas y reposiciones no
se anulan aquí: se reversa su `PagoProgramado` (`POST /pgtr/revertirConfirmado/{id}`), que
anula el movimiento automáticamente (ver §3).

## 3. Apertura y reposición desde banco

`POST /mvch/reposicion` y `POST /mvch/apertura` (misma forma, distinto tipo de movimiento)
crean el `MovimientoCajaChica` (sin asiento todavía) y lo envían al circuito de pagos como
**origen externo**: `PagoProgramadoService.registrarPagoDeOrigenExterno` con
`origen = OrigenPagoExterno.TSR_CAJA_CHICA` e `idOrigen = MVCHCDGO`. El beneficiario
denormalizado es la propia caja (`nombre` + identificación sintética `CAJACHICA-{id}`).

- **Reposición**: rechaza si `valor > fondo − saldo` (no se puede reponer por encima del fondo).
- **Apertura**: rechaza si la caja ya tiene saldo (`saldo > 0`) — para eso está la apertura
  MIGRADA de `CajaChicaService.registrar` (ver §7), que no pasa por el banco.

El pago se confirma igual que cualquier otro (cheque y débito automático nacen CONFIRMADOS,
por eso son las únicas formas admitidas — ver nota más abajo). Al confirmarse,
`PagoProgramadoServiceImpl.contabilizarPagoCajaChica` (rama especial de
`contabilizarPagoOrigenExterno`, **no usa el desglose de PGS.DPGT**) genera el asiento con
`AsientoContableServiceImpl.generarAsientoReposicionCajaChica`:

- **DEBE**: cuenta contable de la caja.
- **HABER**: cuenta contable de la cuenta bancaria de origen.
- Observación: `"{Apertura|Reposición} caja chica {nombre} | {descripcion} | Ref: {referencia} | Valor: $x"`
  (+ `" | Cheque N° n Cta c"` si se pagó con cheque, igual que el resto de pagos con cheque).
- El movimiento bancario (`TSR.MVCB`) se genera igual que en los demás orígenes: tipo
  `CHEQUES_GIRADOS_Y_NO_COBRADOS` si hay cheque, `TRANSFERENCIAS_DEBITOS_EN_TRANSITO` si no.
- El asiento se guarda tanto en `pago.asiento` como en `movimiento.asiento`.

**Reversión / cancelación**: el `MovimientoCajaChica` de una apertura/reposición se anula
(estado ANULADO, `asiento = null`) en los tres puntos donde el `PagoProgramado` asociado puede
dejar de ser válido — vía el método privado
`PagoProgramadoServiceImpl.anularMovimientoCajaChicaSiAplica` (idempotente, no hace nada si ya
estaba ANULADO):

- `POST /pgtr/revertirConfirmado/{id}` (pago ya CONFIRMADO): `motivoAnulacion = "PAGO REVERSADO: {motivo}"`. Si falla, propaga.
- `POST /pgtr/anular/{id}` (pago aún no confirmado, p.ej. REGISTRADO): `motivoAnulacion = "PAGO ANULADO: {motivo}"`. Si falla, propaga.
- Rechazo del banco al procesar el lote/archivo: `motivoAnulacion = "PAGO RECHAZADO POR EL BANCO: {motivo}"`. Si falla, se captura y sólo se registra en log — no aborta el resto del archivo.

Las dos últimas vías son **defensivas, hoy inalcanzables en la práctica**: el pago de caja
chica nace CONFIRMADO (sólo admite cheque o débito automático — ver nota más abajo), así que
nunca llega a `anularPago` ni al rechazo de un lote. Se dejan por si esa restricción cambia a
futuro.

El método rechaza (`IncomeException`) si el movimiento ya quedó incluido en un cierre
confirmado (`movimiento.getCierre() != null`): anularlo alteraría en silencio un periodo ya
cerrado; primero hay que anular ese cierre (`POST /crch/anular/{id}`).

**Nota sobre formas de pago**: `registrarPagoBanco` valida al inicio que `formaPago` sea
**3 (Cheque) o 4 (Débito automático)** — si no, rechaza con
`"La reposición de caja chica debe pagarse con cheque o débito automático: la caja no tiene
cuenta bancaria de destino."` Transferencia (`formaPago=2`) y efectivo (`formaPago=1`) se
descartan de forma explícita y temprana, no como efecto indirecto del circuito de pagos: una
caja chica no tiene cuenta bancaria externa de destino que una transferencia pudiera usar, y
de paso cheque/débito automático contabilizan de inmediato (nacen CONFIRMADOS), así que el
saldo nunca sube antes de que el dinero realmente entre a la caja.

## 4. Adjuntos

El archivo físico se sube primero con `POST /file/upload/custom?fileName=...&uploadPath=...`
(`FileRest`, `uploadPath` sugerido: `"caja-chica/{idCaja}/{idMovimiento}"`), y el `path` que
devuelve se graba con `POST /ptch`. `DELETE /ptch/{id}` borra el registro **y** el archivo
físico (`FileService.deleteFile`).

## 5. Cierre (arqueo)

**Preparar** (`POST /crch/preparar`, estado BORRADOR): el periodo va desde el día siguiente al
`fechaFin` del último cierre CERRADO (o desde el primer movimiento de la caja si no tiene
ninguno) hasta la fecha indicada. Calcula `saldoInicial` (saldo hasta el día anterior al
inicio del periodo), `totalGastos`, `totalReposiciones` (apertura + reposición) y
`totalAjustes` (positivos − negativos) del periodo, y `saldoLibros` (saldo hasta la fecha de
corte). Rechaza si la caja ya tiene un BORRADOR pendiente.

**Confirmar** (`POST /crch/confirmar/{id}`): recibe el saldo físico contado. Antes de comparar,
**recalcula** `saldoLibros`, `totalGastos`, `totalReposiciones` y `totalAjustes` desde los
movimientos vigentes a la fecha de corte (no usa los valores congelados en `preparar`) y los
regraba en el cierre — un gasto puede haberse registrado o anulado entre `preparar` y
`confirmar`, y usar el valor congelado habría comparado el saldo físico contra un `saldoLibros`
desactualizado. `diferencia = saldoFisico − saldoLibros` (con el valor recalculado). Si
`|diferencia| > 0.01`:

- Exige `idPlanCuentaDiferencia` (la cuenta de faltantes/sobrantes la elige el usuario en la
  pantalla del cierre; no se parametriza en la caja).
- Crea un `MovimientoCajaChica` tipo AJUSTE_POSITIVO (sobrante) o AJUSTE_NEGATIVO (faltante),
  `descripcion = "AJUSTE POR ARQUEO {fechaFin}"`, fecha = `fechaFin` del cierre.
- Genera el asiento con `generarAsientoAjusteCajaChica`: sobrante → DEBE caja / HABER cuenta de
  diferencia; faltante → DEBE cuenta de diferencia / HABER caja. Tipo EGRESO_TESORERIA, módulo
  TESORERIA.
- El asiento se guarda en el movimiento de ajuste y en el propio cierre (`CRCH.ASNTCDGO`).

Marca con el cierre (`CRCHCDGO`) todos los movimientos ACTIVOS del periodo, incluido el ajuste
recién creado (su fecha cae dentro del periodo). Estado pasa a CERRADO.

**Anular** (`POST /crch/anular/{id}`): sólo el **último** cierre CERRADO de la caja. Desmarca
el cierre de todos sus movimientos; si hubo ajuste, anula su asiento y el propio movimiento de
ajuste (queda ANULADO). El cierre pasa a ANULADO con el motivo anexado a su observación.

**Mientras hay un BORRADOR pendiente**: registrar un gasto (`POST /mvch/gasto`), anular un
movimiento (`POST /mvch/anular/{id}`) o registrar una apertura/reposición
(`POST /mvch/apertura`, `POST /mvch/reposicion`) con fecha dentro del rango
`[fechaInicio, fechaFin]` del BORRADOR se rechaza — de lo contrario el `saldoLibros` congelado
en `preparar` quedaría desfasado frente a lo que el usuario ve en pantalla mientras cuenta el
efectivo. El bloqueo se libera al confirmar o anular el BORRADOR.

**Regla derivada**: mientras un movimiento tenga `cierre != null`, no se puede anular
(`POST /mvch/anular/{id}` lo rechaza) — hay que anular el cierre primero.

## 6. Endpoints

### `/cjch` (cajas)

| Método | Path | Body / Query | Devuelve |
|---|---|---|---|
| POST | `/cjch/registrar` | `{idEmpresa, nombre, idPlanCuenta, montoFondo, montoMaximoGasto?, porcentajeAlerta?, responsable?, idCustodio?, observacion?, saldoInicialMigrado?, idUsuario}` | Caja creada |
| GET | `/cjch/saldo/{id}` | — | `{idCaja, nombre, fondo, saldo, porcentaje, alerta, montoSugeridoReposicion, ultimoCierre}` |
| GET | `/cjch/saldos/{idEmpresa}` | — | Lista del shape anterior, una por caja activa |
| GET | `/cjch/activas/{idEmpresa}` | — | Cajas activas (sin saldo) |
| GET/POST/PUT/DELETE | CRUD estándar | — | — |

Ejemplo `registrar`:
```json
POST /SaaBE/rest/cjch/registrar
{
  "idEmpresa": 1, "nombre": "Caja Matriz", "idPlanCuenta": 10029,
  "montoFondo": 500.00, "montoMaximoGasto": 50.00, "porcentajeAlerta": 20,
  "responsable": "María Pérez", "saldoInicialMigrado": 350.00, "idUsuario": 5
}
```

### `/mvch` (movimientos)

| Método | Path | Body / Query | Devuelve |
|---|---|---|---|
| POST | `/mvch/gasto` | `{idCaja, fecha, valor, descripcion, observacion, idProducto, idTitular?, numeroDocumento?, idUsuario}` | Movimiento con su asiento |
| POST | `/mvch/reposicion` | `{idCaja, valor, idCuentaBancariaOrigen, formaPago, debitoAutomatico, referencia?, fecha, descripcion?, idUsuario}` | `{idMovimiento, idPago, estadoPago, numeroCheque}` |
| POST | `/mvch/apertura` | (igual forma que reposición) | (igual respuesta) |
| POST | `/mvch/anular/{id}` | `{motivo, idUsuario}` | `{mensaje}` |
| GET | `/mvch/listar` | `?idCaja&desde&hasta&tipo&estado` | Movimientos de la caja |
| GET/POST/PUT/DELETE | CRUD estándar | — | — |

Ejemplo `gasto`:
```json
POST /SaaBE/rest/mvch/gasto
{
  "idCaja": 1, "fecha": "2026-08-27", "valor": 12.50,
  "descripcion": "Compra de suministros de oficina",
  "observacion": "Proveedor no emite factura, recibo interno adjunto",
  "idProducto": 7, "numeroDocumento": "REC-0042", "idUsuario": 5
}
```

### `/crch` (cierres)

| Método | Path | Body / Query | Devuelve |
|---|---|---|---|
| POST | `/crch/preparar` | `{idCaja, fecha, idUsuario}` | `{cierre, movimientos}` |
| POST | `/crch/confirmar/{id}` | `{saldoFisico, observacion?, idPlanCuentaDiferencia?, idUsuario}` | Cierre CERRADO |
| POST | `/crch/anular/{id}` | `{motivo, idUsuario}` | `{mensaje}` |
| GET | `/crch/listar/{idCaja}` | — | Cierres de la caja |
| GET | `/crch/movimientos/{idCierre}` | — | Movimientos incluidos en el cierre |
| GET/POST/PUT/DELETE | CRUD estándar | — | — |

### `/ptch` (adjuntos)

| Método | Path | Body / Query | Devuelve |
|---|---|---|---|
| GET | `/ptch/porMovimiento/{idMovimiento}` | — | Adjuntos del movimiento |
| DELETE | `/ptch/{id}` | — | Borra el registro y el archivo físico |
| GET/POST/PUT | CRUD estándar | — | — |

## 7. Migración desde las cuentas bancarias 428/429

Hoy la caja chica vive como bancos ficticios `TSR.BNCO` 425/427 y cuentas `TSR.CNBC` 428
(plan cuenta 10029) y 429 (plan cuenta 10033). Procedimiento:

1. Para cada cuenta legada, crear la caja chica nueva con `POST /cjch/registrar` usando el
   **mismo `idPlanCuenta`** que la cuenta bancaria (10029 o 10033) y
   `saldoInicialMigrado` = saldo contable actual de esa cuenta.
2. `CajaChicaService.registrar` graba la caja y, cuando `saldoInicialMigrado > 0`, crea un
   `MovimientoCajaChica` tipo APERTURA con `descripcion = "SALDO INICIAL MIGRADO"` **sin
   asiento**: el saldo ya está en la cuenta contable 10029/10033, no hay nada que contabilizar
   de nuevo — sólo se dota a la caja de su saldo inicial para que el cálculo de saldo cuadre.
3. Verificar que no quede ningún pago programado pendiente sobre las cuentas 428/429:
   ```sql
   SELECT PGTRCDGO, PGTRESTD, PGTRVLOR FROM PGS.PGTR WHERE PGTRCNBC IN (428, 429) AND PGTRESTD IN (1, 2);
   ```
   Debe devolver cero filas.
4. Recién entonces ejecutar el **bloque 6** de `sql/02-caja-chica.sql` (comentado a propósito):
   inactiva las cuentas 428/429 (`CNBCESTD = 2`).

No se automatiza el paso 4: es una decisión operativa del usuario, después de confirmar que
las cajas nuevas están funcionando y que no quedó nada pendiente en las cuentas viejas.

## 8. Pendiente (fuera de esta fase)

- Pantallas de frontend: fuera de alcance de este prompt (agente BACKEND).
- Reportes de arqueo / kárdex de caja chica en Jasper: no se pidieron en esta fase.

# Anticipos a trabajadores

**Fecha:** 2026-08-28 (PROMPT 08). **Estado:** T1, T2, T3, T5, T6 implementados. **T4 (cierre del ciclo en el rol) NO implementado — ver §6, es la pieza que falta para que el ciclo cierre solo.**

## 1. Qué existía antes de este cambio

Sólo el **descuento**: una fila de `RHH.DSRC` (`DescuentoRecurrente`) creada a mano, con `tipoDescuento = ANTICIPO_DE_SUELDO (3)`. Ningún rastro de que el dinero salió: ni documento, ni pago, ni asiento de la entrega.

## 2. Decisiones de diseño (tomadas en el prompt, no re-abrir sin revisar el impacto)

1. **Cuenta contable compartida.** La entrega usa la MISMA cuenta que ya usa el descuento del rol — `RhhLineaAsiento.CUENTAS_POR_COBRAR_EMPLEADOS` (línea 14 del rubro 214), resuelta contra la plantilla de **ROL** de `RHH.CFNM` (`ConfiguracionNomina.plantillaRol`), el mismo mecanismo que usa `ContabilizacionNominaServiceImpl` (plantilla → línea por auxiliar1 → cuenta real, rechazando la cuenta marcadora). Así el ciclo cuadra solo:
   - **Entrega**: DEBE Cuentas por Cobrar Empleados / HABER Banco.
   - **Descuento en el rol**: DEBE Sueldos y Salarios (u otro concepto) / **HABER** Cuentas por Cobrar Empleados (vía `ContabilizacionNominaServiceImpl.acumulaRenglon` → `lineaDeDescuento`, que ya usaba esta cuenta como su `default` para "anticipos, préstamos internos y cualquier otro descuento").
   No se creó ninguna línea nueva en el rubro 214.
2. **Beneficiario desde `Empleado`, sin `CuentaBancariaEmpleado`.** `Empleado` no tiene FK a `Titular`, así que el pago usa un `BeneficiarioOcasional` armado con `identificacion` y `nombres` del empleado — igual que caja chica hizo con su propio nombre sintético. **No** se referenció `CuentaBancariaEmpleado` (que sí existe y ya usa la nómina para depósito directo): la cuenta destino del pago se resuelve a mano al aprobar. Consecuencia directa: sin datos bancarios del empleado en este circuito, **transferencia es estructuralmente imposible** — `aprobar()` sólo admite `formaPago` **3 (Cheque) o 4 (Débito automático)**, exactamente la misma restricción que se le puso a caja chica y por la misma razón.
3. **El `DescuentoRecurrente` se crea al confirmarse el pago, no al aprobar.** Si el pago se revierte, no queda un descuento cobrando un dinero que nunca se entregó.

## 3. El ciclo completo

```
POST /ante/solicitar
  Empleado activo, valor > 0, numeroCuotas >= 1, sin otro anticipo vivo.
  valorCuota = round(valor / numeroCuotas, 2); saldo = valor. Estado SOLICITADO.
        │
        ▼
POST /ante/aprobar/{id}     (sólo desde SOLICITADO)
  usuarioAprueba, fechaAprobacion, estado APROBADO (transitorio: ver más abajo).
  pagoProgramadoService.registrarPagoDeOrigenExterno(RHH_ANTICIPO_EMPLEADO, ...)
  formaPago debe ser CHEQUE o DÉBITO_AUTOMÁTICO → el pago nace CONFIRMADO
  en la MISMA llamada (igual que caja chica), así que "APROBADO" es un
  estado que en la práctica dura lo que tarda la llamada: al volver de
  aprobar(), el anticipo YA quedó en PAGADO → EN_DESCUENTO (ver abajo).
        │  (síncrono, misma transacción)
        ▼
PagoProgramadoServiceImpl.contabilizarPagoAnticipoEmpleado
  Asiento: DEBE Cuentas por Cobrar Empleados / HABER Banco.
  Movimiento bancario TSR.MVCB (cheque girado / transferencia-débito en tránsito).
  Estado PAGADO → crea DescuentoRecurrente + calendario CuotaDescuento (RHH.CTDS)
  → estado EN_DESCUENTO.
        │
        ▼
   (cada rol cobra una cuota — HOY NO CIERRA SOLO, ver §6)
        │
        ▼
   Saldo en 0 → CANCELADO
```

`POST /ante/anular/{id}`: sólo SOLICITADO, o APROBADO con pago aún no confirmado (defensivo — hoy inalcanzable, igual razón que caja chica: el pago nace CONFIRMADO). Si ya está PAGADO/EN_DESCUENTO, pide revertir el pago primero (`POST /pgtr/revertirConfirmado/{id}`), que dispara la reversión simétrica (§5).

## 4. Asiento de la entrega

Generado por `AsientoContableService.generarAsientoAnticipoEmpleado`, tipo `TipoAsientos.EGRESO_TESORERIA`, módulo `ModuloSistema.TESORERIA`:

| Línea | Cuenta | Valor |
|---|---|---|
| DEBE | Cuentas por Cobrar Empleados (resuelta por plantilla de ROL) | Valor del anticipo |
| HABER | Cuenta contable de la cuenta bancaria de origen | Valor del anticipo |

Observación: `"Anticipo a colaborador {nombre} | {N} cuotas | Ref: {ref} | Valor: $x"` (+ nota de cheque si aplica, mismo formato que el resto de pagos con cheque).

## 5. Reversión

`PagoProgramadoServiceImpl.anularAnticipoEmpleadoSiAplica` (llamado desde `revertirContabilidadOrigenExterno`, simétrico a `anularMovimientoCajaChicaSiAplica`):

- Anula el asiento y el movimiento bancario (igual que cualquier origen externo).
- Si el `DescuentoRecurrente` ya existe: **rechaza si ya cobró alguna cuota** (`cuotasPagadas > 0`) — no se puede deshacer una entrega cuyo descuento ya corrió sin dejar el saldo del empleado inconsistente; hay que resolverlo a mano. Si no cobró ninguna, borra las `CuotaDescuento` pendientes, marca el descuento ANULADO y limpia la FK del anticipo.
- Devuelve el anticipo a **APROBADO** (no a SOLICITADO) con el motivo en `ANTEMTAN` — reutilizando ese campo también para "por qué se deshizo el pago", no sólo para una anulación definitiva del anticipo en sí; no hay otro campo libre para esto en el DDL.

## 6. T4 NO implementado — el cierre del ciclo en el rol

**Diagnóstico verificado:** el motor de rol (`ProcesoNominaServiceImpl`, paso 12) ya **lee** `CuotaDescuento` vía `cuotaDescuentoDaoService.selectPendientesPorVencer(idEmpleado, desde, hasta)` para armar el renglón de descuento de cada nómina — es el mecanismo real de consumo, no `DescuentoRecurrente.saldo` directamente (por eso este cambio SÍ genera el calendario de `CuotaDescuento` al crear el descuento, en `PagoProgramadoServiceImpl.generaCuotasAnticipo`, aunque el prompt no lo pedía explícitamente: sin esas filas el rol nunca encontraría nada que cobrar).

Pero **nada, fuera de la migración de saldos de apertura, marca una `CuotaDescuento` como cobrada** ni decrementa `DescuentoRecurrente.saldo`/`cuotasPagadas` — grep completo del paquete `com.saa.ejb.rhh` lo confirma. El cálculo de la nómina (`calculaContrato`) sólo arma renglones de *previsualización*; nunca escribe de vuelta.

**Por qué no se forzó (permitido explícitamente por el prompt):** el punto correcto para marcar cuotas como cobradas es `ContabilizacionNominaServiceImpl.contabilizarPago(idOrdenPago, fechaAcreditacion, usuario)` — es el único lugar donde el período pasa a `PAGADO`, el único estado desde el que `reabrirPeriodo` **ya no permite** deshacer el rol (`"Un periodo PAGADO no se puede reabrir"`). Marcar cuotas en `aprobarPeriodo` (CALCULADO→APROBADO) sería más temprano pero **reversible**: `reabrirPeriodo` puede deshacer un período APROBADO, y habría que descontar simétricamente las cuotas marcadas — una máquina de estados más grande sobre un método de pago de nómina que hoy funciona y no tiene forma de probarse aquí sin ejecutar un rol real contra la base.

**Diseño propuesto para cuando se decida implementarlo** (no hecho, sólo dejado documentado):
1. En `ContabilizacionNominaServiceImpl.contabilizarPago`, después de generar el asiento de pago y marcar `periodo.setEstado(PAGADO)`: consultar los `ReglonNomina` del período con `origenTabla = "RHH.CTDS"` (el mismo string literal que ya escribe `ProcesoNominaServiceImpl` al crear el renglón, junto al id de la `CuotaDescuento` en `origenId`).
2. Por cada renglón: cargar la `CuotaDescuento`, marcarla `DESCONTADA` (rubro `RhhEstadoCuotaDescuento`), incrementar `DescuentoRecurrente.cuotasPagadas`, decrementar `DescuentoRecurrente.saldo` por el valor de la cuota.
3. `AnticipoEmpleadoDaoService.selectByDescuentoRecurrente(idDescuentoRecurrente)` (ya implementado en este cambio, sin usar todavía) resuelve si ese descuento viene de un anticipo; si sí, decrementar `AnticipoEmpleado.saldo` en el mismo valor y, si llega a 0, pasar el anticipo a `CANCELADO`.
4. Nada de esto se revierte con `reabrirPeriodo` (que ya rechaza períodos PAGADO) — coherente con que, a partir de ese punto, el pago de nómina tampoco se puede deshacer.

**Mientras tanto:** `AnticipoEmpleado.saldo` queda **fijo en el valor total** desde que se crea — no baja solo. Es un hueco real y visible: cualquier pantalla que muestre "saldo pendiente" de un anticipo estará mostrando el valor íntegro hasta que se implemente lo anterior. `AnticipoEmpleado.estado` tampoco pasará nunca de `EN_DESCUENTO` a `CANCELADO` automáticamente.

## 7. Endpoints

| Endpoint | Body / Query | Uso |
|---|---|---|
| `POST /ante/solicitar` | `{idEmpleado, valor, numeroCuotas, fechaInicioDescuento?, motivo?, observacion?, idUsuario}` | Crea el anticipo en SOLICITADO |
| `POST /ante/aprobar/{id}` | `{idCuentaBancariaOrigen, formaPago, debitoAutomatico, referencia?, idUsuario}` | Aprueba y paga (formaPago 3 o 4 solamente) |
| `POST /ante/anular/{id}` | `{motivo, idUsuario}` | Anula (sólo SOLICITADO/APROBADO sin pago confirmado) |
| `GET /ante/listar?idEmpresa&idEmpleado&estado` | — | Listado con filtros opcionales |
| `GET /ante/vigente/{idEmpleado}` | — | Anticipo vivo del empleado (404 si no tiene) |
| CRUD estándar | `/ante/getAll`, `/ante/getId/{id}`, `PUT`/`POST /ante`, `DELETE /ante/{id}`, `POST /ante/selectByCriteria` | — |

**Ejemplo `POST /ante/aprobar/12`:**
```json
{
  "idCuentaBancariaOrigen": 4,
  "formaPago": 3,
  "debitoAutomatico": false,
  "referencia": "CHQ-001234",
  "idUsuario": 5
}
```
Respuesta:
```json
{
  "idAnticipo": 12,
  "idPago": 231,
  "estadoPago": 3,
  "numeroCheque": 1234
}
```
`estadoPago = 3` (`EstadoPagoProgramado.CONFIRMADO`) confirma que, al volver de `aprobar()`, el anticipo ya pasó por PAGADO y quedó en EN_DESCUENTO — no hace falta un paso adicional de confirmación.

## 8. Archivos

- `model/rhh/AnticipoEmpleado.java`, `rubros/EstadoAnticipoEmpleado.java`, `rubros/Rubros.java` (+`RHH_ESTADO_ANTICIPO_EMPLEADO=234`), `rubros/OrigenPagoExterno.java` (+`RHH_ANTICIPO_EMPLEADO`), `model/rhh/NombreEntidadesRhh.java` (+`ANTICIPO_EMPLEADO`)
- `ejb/rhh/dao/AnticipoEmpleadoDaoService.java` + `daoImpl/AnticipoEmpleadoDaoServiceImpl.java`
- `ejb/rhh/service/AnticipoEmpleadoService.java` + `serviceImpl/AnticipoEmpleadoServiceImpl.java`
- `ws/rest/rhh/AnticipoEmpleadoRest.java`
- `ejb/cxp/serviceImpl/PagoProgramadoServiceImpl.java`: rama `RHH_ANTICIPO_EMPLEADO` en `contabilizarPagoOrigenExterno` (→ `contabilizarPagoAnticipoEmpleado`, `generaCuotasAnticipo`), y en `revertirContabilidadOrigenExterno` (→ `anularAnticipoEmpleadoSiAplica`)
- `ejb/cnt/service/AsientoContableService.java` + `serviceImpl/AsientoContableServiceImpl.java`: `generarAsientoAnticipoEmpleado` + `obtenerCuentaCuentasPorCobrarEmpleados`

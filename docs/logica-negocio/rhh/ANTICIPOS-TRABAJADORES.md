# Anticipos a trabajadores

**Fecha:** 2026-08-28 (PROMPT 08); T4 agregado 2026-08-27. **Estado:** T1 a T6 implementados. El ciclo cierra solo — ver §6.

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

## 6. T4 — el cierre del ciclo en el rol

**Diagnóstico que motivó el cambio:** el motor de rol (`ProcesoNominaServiceImpl`, paso 12) ya **lee** `CuotaDescuento` vía `cuotaDescuentoDaoService.selectPendientesPorVencer(idEmpleado, desde, hasta)` para armar el renglón de descuento de cada nómina — es el mecanismo real de consumo, no `DescuentoRecurrente.saldo` directamente (por eso T3 genera el calendario de `CuotaDescuento` al crear el descuento, en `PagoProgramadoServiceImpl.generaCuotasAnticipo`: sin esas filas el rol nunca encontraría nada que cobrar). Pero, fuera de la migración de saldos de apertura, nada marcaba una `CuotaDescuento` como cobrada ni decrementaba `DescuentoRecurrente.saldo`/`cuotasPagadas`: el cálculo de la nómina (`calculaContrato`) sólo arma renglones de *previsualización*, nunca escribe de vuelta.

**Implementado como un EJB aparte, `CierreCuotasDescuentoService`/`CierreCuotasDescuentoServiceImpl`**, invocado desde `ContabilizacionNominaServiceImpl.contabilizarPago` inmediatamente después de generar el asiento de pago y marcar `periodo.setEstado(PAGADO)` — es el único lugar donde el período pasa a ese estado, el único desde el que `reabrirPeriodo` **ya no permite** deshacer el rol (`"Un periodo PAGADO no se puede reabrir"`); marcar cuotas más temprano, en `aprobarPeriodo`, habría exigido descontarlas simétricamente si el período se reabre, una máquina de estados mayor sobre un flujo que ya funciona.

### Por qué es un EJB aparte y no un método privado (corregido el 2026-08-27)

La primera versión de este cambio metía el cierre como dos métodos `private` dentro de `ContabilizacionNominaServiceImpl`, envueltos en `try/catch`. **Estaba mal**, y no por una omisión menor: un método privado corre en la misma transacción JTA que el resto de `contabilizarPago`. Si algo dentro de ese método falla, el contenedor marca la transacción **`rollback-only`** — y ese marcado **no lo deshace un `try/catch`**: el `catch` evita que la excepción se propague como código Java, pero la transacción ya quedó condenada. Al llegar al final del método, el `commit` implícito falla con `RollbackException` y se pierde **todo**: el asiento que se acababa de generar y el estado `PAGADO` del período, exactamente lo que este diseño existe para evitar. Y anotar el método privado con `REQUIRES_NEW` no lo arregla: los interceptores de EJB que interpretan `@TransactionAttribute` sólo actúan sobre llamadas que pasan por el **proxy** del bean — nunca sobre `this.metodo()`.

La corrección: el cierre vive en su propio `@Stateless` (`CierreCuotasDescuentoServiceImpl`), inyectado con `@EJB` en `ContabilizacionNominaServiceImpl` y llamado a través de su interfaz `@Local` (`CierreCuotasDescuentoService.descuentaCuotasDelPeriodo`), anotado `@TransactionAttribute(REQUIRES_NEW)`. Al pasar por el proxy, el contenedor sí suspende la transacción del pago y abre una nueva, independiente, para el cierre. Si algo falla ahí adentro, es esa transacción nueva la que se pierde — la del pago, que ya comiteó el asiento y el período `PAGADO` antes de llegar a esta llamada, no se entera.

**No volver a "simplificar" esto a un método privado.** Es exactamente el error que motivó la corrección.

1. `descuentaCuotasDelPeriodo` (en el bean nuevo) recarga el `PeriodoNomina` por id — recibe `idPeriodoNomina`, no la entidad, porque una entidad cargada en la transacción del pago llegaría detached a la transacción nueva del cierre — y recorre todas las `Nomina` del período y, dentro de cada una, sus `ReglonNomina`, filtrando los que tienen `tablaReferencia = "RHH.CTDS"` (el mismo literal que `ProcesoNominaServiceImpl` escribe al crear el renglón, junto al id de la `CuotaDescuento` en `idReferencia`).
2. `marcaCuotaDescontada`, por cada renglón: carga la `CuotaDescuento`, la marca `DESCONTADA` — o `PARCIAL` si el valor del renglón quedó por debajo del total de la cuota por la protección de neto negativo (`recortaDescuentos`, paso 13-14 del cálculo) —, incrementa `DescuentoRecurrente.cuotasPagadas` y decrementa `DescuentoRecurrente.saldo` por el valor efectivamente descontado (no por el total nominal de la cuota, para no desfasar el saldo cuando hubo recorte). Si el saldo llega a 0, el descuento pasa a `CANCELADO` (rubro `RhhEstadoDescuentoRecurrente`).
3. `AnticipoEmpleadoDaoService.selectByDescuentoRecurrente(idDescuentoRecurrente)` resuelve si ese descuento viene de un anticipo; si sí, decrementa `AnticipoEmpleado.saldo` en el mismo valor y, si llega a 0, pasa el anticipo a `CANCELADO`.
4. Nada de esto se revierte con `reabrirPeriodo` (que ya rechaza períodos PAGADO) — coherente con que, a partir de ese punto, el pago de nómina tampoco se puede deshacer.

**Protección explícita (decisión del 2026-08-27):** el punto de fondo — *"el anticipo desactualizado es un problema de reporte; una nómina que no se paga es un problema de gente"* — sigue intacto, ahora sobre la base correcta (transacción propia, no sólo `try/catch`):

- `CierreCuotasDescuentoServiceImpl.descuentaCuotasDelPeriodo` corre en `REQUIRES_NEW` y además está envuelto en su propio `try/catch` que sólo registra en el log, nunca relanza — doble protección: aunque nunca debería escapar una excepción de un método `REQUIRES_NEW` bien atrapado, si algo lo lograra, sería esa transacción nueva la que se pierde, no la del pago.
- Cada renglón se procesa de forma aislada (`marcaCuotaDescontada` atrapa sus propias excepciones): si una cuota falla, las demás igual se marcan.
- Dentro de `marcaCuotaDescontada`, la cascada a `AnticipoEmpleado.saldo` tiene **su propio** `try/catch`, separado de la actualización de `CuotaDescuento`/`DescuentoRecurrente`: si la cuota y el descuento se marcaron bien pero el anticipo falla, el rol de todas formas no vuelve a cobrar esa cuota (el circuito de cobro real ya cerró) y sólo queda desactualizado un dato de reporte.
- `ContabilizacionNominaServiceImpl.contabilizarPago` además envuelve la propia llamada a `cierreCuotasDescuentoService.descuentaCuotasDelPeriodo(...)` en un `try/catch`, como última red por si algo fallara antes de entrar siquiera a la transacción nueva (por ejemplo, al arrancarla).

No forzar la prueba en vivo (no hay forma de correr un rol real contra la base en esta sesión) fue una razón válida para no implementar T4 antes, pero no para dejarlo sin esta protección — ni, como quedó demostrado, para conformarse con un `try/catch` que parecía protección y no lo era.

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
- `ejb/rhh/service/CierreCuotasDescuentoService.java` + `serviceImpl/CierreCuotasDescuentoServiceImpl.java` (nuevo, T4): `descuentaCuotasDelPeriodo` (`REQUIRES_NEW`) + `marcaCuotaDescontada`
- `ejb/rhh/serviceImpl/ContabilizacionNominaServiceImpl.java`: inyecta `CierreCuotasDescuentoService` y lo llama al final de `contabilizarPago`, envuelto en `try/catch`

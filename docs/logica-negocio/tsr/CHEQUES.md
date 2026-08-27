# Pago con cheque — chequeras y cheques

> Fecha: 2026-08-26. Backend: `com.saa.ws.rest.tsr.ChequeraRest` (`/chqr`),
> `com.saa.ws.rest.tsr.ChequeRest` (`/dtch`), `com.saa.ejb.tsr.serviceImpl.ChequeraServiceImpl`,
> `com.saa.ejb.tsr.serviceImpl.ChequeServiceImpl`. Integración con pagos:
> `com.saa.ejb.cxp.serviceImpl.PagoProgramadoServiceImpl` (formaPago=CHEQUE).
> DDL: `docs/logica-negocio/tsr/sql/01-cheques-pago-programado.sql`.
>
> El circuito legado `TSR.PGSS` / `ChequeServiceImpl.procesoImpresionCheques` **no se usa**
> ni se toca: los métodos de esta fase son nuevos, con nombres nuevos.

## 1. Modelo

- **Chequera** (`TSR.CHQR`): cuenta bancaria, rango (`comienza`/`finaliza`), número de cheques,
  fecha de solicitud/entrega, estado (rubro 25 `EstadoChequera`).
- **Cheque** (`TSR.DTCH`): chequera, número, valor, titular, beneficiario (texto), fechas de
  uso/impresión/entrega/anulación, asiento, estado (rubro 26 `EstadoCheque`), motivo de
  anulación (rubro 38 `MotivoAnulacionCheque`).
- **CuentaBancaria.manejaChequera** (`TSR.CNBC.CNBCCHQR`, 0/1): habilita la forma de pago
  CHEQUE en los pagos que salen de esa cuenta. No excluye transferencia ni débito automático.
- **PagoProgramado.formaPago** (`PGS.PGTR.PGTRFPAG`): 1=Efectivo, 2=Transferencia, 3=Cheque,
  4=Débito automático (`com.saa.rubros.FormaPagoProgramado`).
- **PagoProgramado.cheque** (`PGS.PGTR.PGTRDTCH`): cheque girado cuando `formaPago=3`.

### Estados de la chequera (rubro 25)

| Código | Estado | Significado |
|---|---|---|
| 1 | ACTIVA | Tiene cheques ACTIVO disponibles |
| 2 | INACTIVA | (sin uso en esta fase) |
| 3 | SOLICITADA | (sin uso en esta fase) |
| 4 | TERMINADA | Se agotaron sus cheques ACTIVO (automático) |
| 5 | PERDIDA | (sin uso en esta fase) |
| 6 | ANULADA | Anulada por el usuario (`POST /chqr/anular/{id}`) |

### Estados del cheque (rubro 26)

| Código | Estado | Significado |
|---|---|---|
| 1 | ACTIVO | Disponible para asignar a un pago |
| 2 | ANULADO | Ya no se usa (motivo en rubro 38) |
| 3 | GENERADO | Asignado a un pago, con valor y beneficiario |
| 4 | IMPRESO | Impresión física registrada |
| 5 | DANIADO | (sin uso en esta fase) |
| 6 | ENTREGADO | Entregado al beneficiario |

### Motivo de anulación (rubro 38)

| Código | Motivo |
|---|---|
| 1 | ERROR_DE_TIPEO |
| 2 | ERROR_DE_USUARIO |
| 3 | CHEQUERA_ANULADA (anulación en cascada al anular la chequera) |
| 4 | PAGO_REVERSADO (anulación automática al reversar el pago) |

## 2. Ciclo de vida

```
Chequera: ACTIVA ──(sin cheques ACTIVO)──▶ TERMINADA
   │
   └──(anular)──▶ ANULADA  (rechaza si tiene cheques GENERADO/IMPRESO/ENTREGADO;
                             sus cheques ACTIVO pasan a ANULADO motivo 3)

Cheque:   ACTIVO ──(se asigna a un pago)──▶ GENERADO ──(imprimir)──▶ IMPRESO ──(entregar)──▶ ENTREGADO
            │                                  │
            └──(anular suelto)──▶ ANULADO      └──(reversar el pago)──▶ ANULADO (motivo 4)
```

- Un cheque **ACTIVO** sin pago asociado se anula directo (`POST /dtch/anular/{id}`).
- Un cheque **GENERADO/IMPRESO/ENTREGADO** (con pago) sólo se anula **reversando el pago**
  (`POST /pgtr/revertirConfirmado/{id}`) — `anularChequeSuelto` lo rechaza explícitamente
  indicando el pago asociado.
- La chequera pasa a **TERMINADA** automáticamente (`cerrarSiTerminada`) cada vez que se
  consume o se anula un cheque y no queda ninguno ACTIVO. No hay endpoint para esto: es
  efecto colateral de `asignarAPago` y `anularChequeSuelto`/`anularChequera`.

## 3. Recepción de una chequera

`POST /chqr/registrarRecepcion` con `{ idCuentaBancaria, comienza, finaliza, fechaEntrega, idUsuario }`:

1. Valida que la cuenta exista y tenga `manejaChequera=1`.
2. Valida `comienza >= 1`, `finaliza >= comienza`.
3. Rechaza si el rango se solapa con otra chequera **no anulada** de la misma cuenta.
4. Graba la chequera en estado ACTIVA y genera **un `Cheque` ACTIVO por cada número** del
   rango (reutiliza `ChequeService.crearChequesDeChequera`, ya existente).

`GET /chqr/sugerirInicio/{idCuenta}` sugiere el número inicial: `max(finaliza)+1` de las
chequeras no anuladas de la cuenta, o `1` si no tiene ninguna.

## 4. Pago con cheque (integración con `/pgtr`)

Cuando un pago se registra con `formaPago=3` (en `/pgtr`, `/egrs/procesar` o `/antp/procesar`):

1. Se valida que la cuenta de origen tenga `manejaChequera=1` (`PagoProgramadoServiceImpl.validarFormaPago`).
2. `ChequeService.asignarAPago` toma el cheque **ACTIVO de menor número** entre las chequeras
   **ACTIVAS** de la cuenta, lo deja **GENERADO** con `valor`, `titular`, `beneficiario` y
   `fechaUso`, y cierra la chequera si era el último disponible.
3. El pago nace **CONFIRMADO** de inmediato (igual que el débito automático) y se contabiliza
   en la misma llamada — el cheque girado ya reduce el banco en libros, y la conciliación
   bancaria ya conoce "cheques girados y no cobrados" como categoría en tránsito. No se exige
   cuenta de destino del beneficiario (el cheque no se transfiere).
4. El beneficiario del cheque es: factura → titular de la factura; egreso →
   `egreso.getTitular()` si existe, si no el `beneficiarioNombre` del pago; anticipo → titular
   del anticipo; origen externo → `beneficiarioNombre` (sin titular en el maestro).
5. El movimiento bancario (`TSR.MVCB`) lleva `cheque`, `numeroCheque` (`MVCBCHQN`) y tipo
   **CHEQUES_GIRADOS_Y_NO_COBRADOS (2)** en vez de transferencias en tránsito.
6. La glosa del asiento anexa ` | Cheque N° {numero} Cta {numeroCuenta}` a la observación de
   cabecera, y ` | Cheque N° {numero}` a la descripción de la línea HABER (banco).
7. Un pago con cheque **no entra** en `POST /pgtr/lote` (misma exclusión que el débito
   automático: el dinero ya salió, no hay nada que enviar al banco).
8. **Reversión** (`POST /pgtr/revertirConfirmado/{id}`): además de reversar la contabilidad
   del proceso de origen, anula el cheque con motivo `PAGO_REVERSADO (4)` y dobla el pago a
   **ANULADO** (no a Rechazado — un cheque anulado no se reprograma). El cheque queda
   histórico, sin desasociarse del pago.
9. **Anulación simple** (`POST /pgtr/anular/{id}`): un pago con cheque nunca llega a este
   endpoint en un estado anulable, porque nace CONFIRMADO — el endpoint lo rechaza con un
   mensaje explícito que remite a la reversión.

## 5. Endpoints

### `/chqr` (chequeras)

| Método | Path | Body / Query | Devuelve |
|---|---|---|---|
| GET | `/chqr/sugerirInicio/{idCuenta}` | — | `{ "siguiente": 1051 }` |
| POST | `/chqr/registrarRecepcion` | `{ idCuentaBancaria, comienza, finaliza, fechaEntrega, idUsuario }` | Chequera creada |
| GET | `/chqr/resumen/{idChequera}` | — | `{ comienza, finaliza, total, disponibles, generados, impresos, entregados, anulados, siguiente }` |
| POST | `/chqr/anular/{id}` | `{ motivo, idUsuario }` | `{ mensaje }` |
| GET | `/chqr/porCuenta/{idCuenta}` | — | Chequeras de la cuenta |
| GET/POST/PUT/DELETE | CRUD estándar | — | — |

Ejemplo `registrarRecepcion`:
```json
POST /SaaBE/rest/chqr/registrarRecepcion
{
  "idCuentaBancaria": 4,
  "comienza": 1001,
  "finaliza": 1050,
  "fechaEntrega": "2026-08-26T09:00:00",
  "idUsuario": 5
}
```

### `/dtch` (cheques)

| Método | Path | Body / Query | Devuelve |
|---|---|---|---|
| GET | `/dtch/siguiente/{idCuenta}` | — | `{ idCheque, numero }` (404 `{ mensaje }` si no hay disponibles) |
| GET | `/dtch/listar` | `?idEmpresa&idCuenta&estado&desde&hasta` (todos opcionales) | Lista con `idCheque, numero, estado, valor, beneficiario, fechaUso, fechaImpresion, fechaEntrega, idPago, tipoPago, referenciaPago, idDocumento, numeroCuenta, banco` (+ `origenExterno`, `idOrigen` cuando `tipoPago="EXTERNO"`) |
| POST | `/dtch/anular/{id}` | `{ motivo, idUsuario }` (motivo: código del rubro 38) | `{ mensaje }` |
| POST | `/dtch/imprimir` | `{ ids: [...], idUsuario }` | `{ mensaje }` |
| POST | `/dtch/entregar` | `{ ids: [...], idUsuario }` | `{ mensaje }` |
| GET/POST/PUT/DELETE | CRUD estándar | — | — |

`tipoPago` en `/dtch/listar` es uno de `FACTURA`, `EGRESO`, `ANTICIPO`, `EXTERNO`, o `null` si
el cheque todavía no se asignó a ningún pago. `idDocumento` es el id del documento CXP que
originó el pago (id de la factura, del egreso o del anticipo, según `tipoPago`) — el frontend
lo usa para navegar del cheque al documento. Es `null` cuando `tipoPago` es `EXTERNO` (no hay
un documento CXP: la fila trae en su lugar `origenExterno` e `idOrigen`, la pareja opaca que
identifica el documento en el módulo que originó el pago) o cuando el cheque todavía no tiene
pago asociado. El filtro `idEmpresa` sólo aplica a los cheques que ya tienen pago asociado
(`TSR.CNBC` no tiene empresa, así que un cheque sin pago no se puede filtrar por empresa y
siempre aparece).

Ejemplo `imprimir`:
```json
POST /SaaBE/rest/dtch/imprimir
{ "ids": [1012, 1013], "idUsuario": 5 }
```
Si alguno de los ids no está en el estado esperado (Generado para imprimir, Impreso para
entregar), la operación completa se aborta con un mensaje que indica el número de cheque.

## 6. Normalización, concurrencia y filtros — detalles verificados el 2026-08-27

- **`formaPago` vs. `debitoAutomatico` se normalizan, no se rechazan.** Son la misma
  información contada dos veces en los endpoints de `/pgtr`, `/egrs/procesar` y
  `/antp/procesar`. Si llegan en desacuerdo, `PagoProgramadoServiceImpl.validarFormaPago`
  ajusta en vez de lanzar error:
  - `debitoAutomatico=true` con cualquier `formaPago` que no sea 4 → se normaliza a
    `formaPago=4` (queda una traza en el log del servidor).
  - `formaPago=4` con `debitoAutomatico=false` → se trata igual como débito automático.
  - `formaPago=3` (Cheque) con `debitoAutomatico=true` **sí es un error real** y se rechaza:
    un pago no puede a la vez requerir chequera y no transferir nada porque el banco ya lo
    debitó por convenio.
  - `formaPago=1` (Efectivo) se rechaza siempre: todavía no está soportado.
  - El pago, la respuesta del endpoint (`debitoAutomatico`, `formaPago`) y la contabilidad
    generada siguen **siempre** la forma de pago ya normalizada, nunca el valor crudo que
    mandó el cliente.

- **Condición de carrera al tomar el siguiente cheque.** Dos pagos simultáneos desde la misma
  cuenta bancaria podrían competir por el mismo cheque ACTIVO. Se resuelve en dos capas:
  1. `ChequeDaoServiceImpl.selectMinChequeActivoPorCuenta` bloquea la fila candidata con
     `LockModeType.PESSIMISTIC_WRITE`: el segundo registro simultáneo espera a que el primero
     confirme o revierta su transacción antes de leer cuál es "el siguiente disponible".
  2. Como red de seguridad, `PagoProgramadoServiceImpl.guardaPagoConCheque` hace `flush()`
     inmediatamente después de grabar el pago con el cheque asignado; si el índice único
     `UQ_PGTR_DTCH` (`PGS.PGTR.PGTRDTCH`, a cargo del usuario — no viene en el DDL de esta
     fase) salta por una violación de constraint, el error se traduce a
     *"El cheque N° X fue tomado por otro usuario, intente nuevamente"* en vez de una
     excepción de persistencia cruda.

- **El filtro de fechas de `/dtch/listar` no sirve para ver cheques disponibles.** `desde`/
  `hasta` comparan contra `Cheque.fechaUso`, que es `null` mientras el cheque está ACTIVO (la
  fecha de uso recién se graba cuando se asigna a un pago). Un frontend que ponga un rango de
  fechas por defecto en la pantalla de consulta **hará desaparecer los cheques disponibles**
  del listado. Si se quiere ver "qué cheques hay para usar", consultar sin `desde`/`hasta` (o
  filtrar por `estado=1`) en vez de por rango de fechas.

## 7. Pendiente (fuera de esta fase)

- Impresión física del cheque (reporte Jasper con la plantilla del banco).
- Pantallas de frontend (proceso 03-FRONTEND-cheques.md): las 8 pantallas de
  `saaFE/src/app/modules/tsr/forms/pagos/cheques/**` y `pagos/procesos/**` siguen en maqueta
  con datos hardcodeados; conectarlas a estos endpoints es tarea del agente FRONTEND.

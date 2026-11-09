# API — Pensión retenida de jubilados sin cuenta bancaria

2026-11-09 · Diseño: `DISENO-PENSION-RETENIDA-SIN-CUENTA.md` · DDL: `sql/374_DDL_PGPC_PENSION_RETENIDA.sql` (va ANTES del EAR).
Reglas de la corrida: `API-DOS-PROCESOS-MENSUALES-JUBILADOS.md` §12bis.

## Qué cambia para el usuario

Un jubilado sin cuenta activa única con certificado ya no genera un «por pagar» fantasma: su pensión del mes queda **retenida**
(PGPC `REGISTRADA` con `valorRetenido` > 0), sin orden ni devengo. Cuando registra la cuenta se le paga todo lo acumulado: una
orden de pago por mes, automáticamente en la siguiente corrida de pensiones, o antes con el botón «Pagar pensiones retenidas».

## Endpoints (`/SaaBE/rest/pgpc/...`)

### GET `/retenidos/{idEntidad}` — solo lectura (para mostrar el botón)
```json
{
  "idEntidad": 123,
  "tieneSalida": true,
  "motivoSinSalida": null,
  "pendientes": [ { "idPago": 9001, "anio": 2026, "mes": 9, "valor": 250.00, "historicoYaDevengado": false } ],
  "totalPendiente": 250.00,
  "pagados": [], "noPagados": [], "totalPagado": 0, "totalNoPagado": 0, "mensaje": null
}
```
Mostrar el botón cuando `pendientes` no está vacío y `tieneSalida` es `true`. Si `tieneSalida` es `false`, mostrar `motivoSinSalida`.

### POST `/pagarRetenidos?idEntidad=&idEmpresa=&usuario=`
Sin cuerpo. Una transacción propia. Errores HTTP: 400 si falta un parámetro o el usuario no es válido; el resto
(mes en curso cerrado, partícipe inexistente, falla de CXP) por el manejo de errores de siempre.
```json
{
  "exito": true,
  "mensaje": "2 mes(es) retenido(s) pagado(s) por $500.0, 1 sin pagar por $250.0.",
  "resultado": {
    "idEntidad": 123, "tieneSalida": true, "motivoSinSalida": null,
    "pagados":   [ { "idPago": 9001, "anio": 2026, "mes": 9,  "valor": 250.0, "idOrdenPago": 777, "idAsientoDevengo": 5555, "historicoYaDevengado": false } ],
    "noPagados": [ { "idPago": 9003, "anio": 2026, "mes": 11, "valor": 250.0, "motivo": "Saldo de pensión complementaria insuficiente ..." } ],
    "pendientes": [], "totalPagado": 500.0, "totalNoPagado": 250.0
  }
}
```
- Lo no pagado **no es error HTTP**: va en `noPagados` con su `motivo` (sin cuenta/certificado, o saldo insuficiente).
- Se paga en orden de período; si un mes no alcanza de saldo, ese y los siguientes quedan sin pagar.
- `idAsientoDevengo` es `null` en los históricos ya devengados (`historicoYaDevengado: true`): su «por pagar» ya existía.
- La orden nace con la fecha de hoy; el negativo del aporte 23 y el devengo también. El mes en curso debe estar abierto.

## Campos nuevos en lo existente

| Dónde | Campo | Significado |
|---|---|---|
| `PagoPensionComplementaria` (PGPC) | `valorRetenido`, `devengadoRetenido` | Pensión retenida del mes; 1 = histórico ya devengado |
| `DetallePagoPension` (corrida, por jubilado) | `valorRetenido` | Pensión de los meses de esta corrida que quedó retenida |
| | `retenidoAcumuladoPagado`, `mesesRetenidosPagados` | Retenido de meses anteriores que la corrida pagó |
| `ResultadoGeneracionPagosPension` | `totalRetenido`, `totalRetenidoAcumuladoPagado` | Totales; el pagado ya está incluido en `totalOrdenesGeneradas` |
| `DetallePrevisualizacionJubilado` (prevuelo) | `montoRetenido`, `retenidoAcumuladoAPagar` | Lo que la corrida retendría / pagaría de acumulado |
| `ResultadoPrevisualizacionCorrida` | `totalRetenido`, `totalRetenidoAcumuladoAPagar` | Totales del prevuelo |

`participacion = "SOLO_CRUCE"` sigue significando que algún mes quedó con la pensión retenida.

## Reglas que el FE debe conocer
- Ancla: pagar un retenido no cuenta como «último movimiento» del jubilado; no cambia sus meses adeudados.
- **Un pago rechazado queda pendiente** (§3.4): cualquier pensión de jubilado cuyo pago termina rechazado (rechazo del banco, orden anulada o reversada y sincronizada) vuelve a `REGISTRADA` sin orden con `valorRetenido` = lo devuelto y `devengadoRetenido` = 1. Aparece de nuevo en `GET /retenidos` y se paga con `pagarRetenidos` / `procesarPendientes`; no se devenga otra vez. La orden rechazada sigue visible en tesorería.
- Fuera de alcance: re-proceso de períodos pasados (C20).

## Lectura en bloque (para el botón por fila)

### GET `/retenidos?idEmpresa=`
Una fila por jubilado con pendientes; evita un GET por jubilado. Solo lectura.
```json
[ { "idEntidad": 123, "nombre": "…", "identificacion": "…", "totalPendiente": 750.0, "meses": 3,
    "tieneSalida": true, "motivoSinSalida": null } ]
```
Botón «Pagar pendientes» por fila: visible si hay fila; habilitado si `tieneSalida`; usa `POST /pagarRetenidos?idEntidad&idEmpresa&usuario`. Sin `tieneSalida`, mostrar `motivoSinSalida`.

## Proceso de pendientes

### POST `/procesarPendientes?idEmpresa=&anio=&mes=&usuario=`
Paga, a todo jubilado con pendientes y salida válida, sus meses pendientes (una orden por mes, una transacción por jubilado). No recorre el padrón y no toca seguro, cruce ni la pensión del mes. Se puede repetir, también con las pensiones de ese mes ya generadas. El mes (anio, mes) debe estar abierto; las órdenes y devengos se fechan `min(fin de ese mes, hoy)`. 400 si falta un parámetro o el usuario no es válido.
```json
{ "exito": true, "mensaje": "Pendientes 11/2026 - 5 mes(es) pagado(s) a 2 jubilado(s) por $1250.0, 1 con algo sin pagar, 0 con error, de 3 evaluados.",
  "resultado": { "anio": 2026, "mes": 11, "evaluados": 3, "conPagos": 2, "conPendientesSinPagar": 1, "conError": 0,
                 "mesesPagados": 5, "totalPagado": 1250.0, "totalNoPagado": 250.0,
                 "detalle": [ /* un ResultadoPagoRetenidos por jubilado: pagados[], noPagados[] con motivo */ ],
                 "errores": [] } }
```
Un jubilado con error no detiene al resto: va en `errores` y `conError`.

# PROMPT 05 — AGENTE FRONTEND — Caja chica: parametrización, gastos con adjunto, reposición, saldo/alerta y cierre

**Agente:** FRONTEND (`C:\work\saaFE\v1\saaFE`). **No tocar el backend.**
**Prerrequisito:** backend del prompt 04 desplegado. Leer `docs/logica-negocio/tsr/CAJA-CHICA.md` del backend (lo escribe el agente BE) para los payloads exactos; lo de abajo es el contrato acordado.

| Método | URL | Body / respuesta |
|---|---|---|
| POST | `/cjch/registrar` | `{cajaChica:{empresa:{codigo}, nombre, planCuenta:{codigo}, montoFondo, montoMaximoGasto, porcentajeAlerta, responsable, custodio:{codigo}?, observacion}, saldoInicialMigrado, idUsuario}` |
| GET | `/cjch/activas/{idEmpresa}` · `/cjch/saldo/{id}` · `/cjch/saldos/{idEmpresa}` | saldo → `{idCaja, nombre, fondo, saldo, porcentaje, alerta, montoSugeridoReposicion, ultimoCierre}` |
| POST | `/mvch/gasto` | `{idCaja, fecha:"yyyy-MM-dd", valor, descripcion, observacion, idProducto, idTitular?, numeroDocumento?, idUsuario}` → MovimientoCajaChica |
| POST | `/mvch/reposicion` · `/mvch/apertura` | `{idCaja, valor, idCuentaBancariaOrigen, formaPago (2/3/4), debitoAutomatico, referencia, fecha, descripcion, idUsuario}` → `{idMovimiento, idPago, estadoPago, numeroCheque}` |
| POST | `/mvch/anular/{id}` | `{motivo, idUsuario}` |
| GET | `/mvch/listar?idCaja&desde&hasta&tipo&estado` | MovimientoCajaChica[] |
| POST | `/file/upload/custom?fileName=&uploadPath=caja-chica/{idCaja}/{idMovimiento}` | (octet-stream) → `{filePath}` |
| POST/DELETE | `/ptch` · `/ptch/{id}` · GET `/ptch/porMovimiento/{id}` | `{movimiento:{codigo}, path, nombreDoc, tipoDoc, usuario}` |
| POST | `/crch/preparar` | `{idCaja, fecha, idUsuario}` → `{cierre, movimientos}` |
| POST | `/crch/confirmar/{id}` | `{saldoFisico, observacion, idPlanCuentaDiferencia?, idUsuario}` |
| POST | `/crch/anular/{id}` · GET `/crch/listar/{idCaja}` · GET `/crch/movimientos/{idCierre}` | |

Rubros: tipo de movimiento padre **232** (1 Apertura, 2 Gasto, 3 Reposición, 4 Ajuste+, 5 Ajuste−); estado de cierre padre **233** (1 Borrador, 2 Cerrado, 3 Anulado).
Fechas: `LocalDate` → `yyyy-MM-dd`; leer con `convertirFechaDesdeBackend()`.

## Tareas

### T1. Modelos y servicios
`modules/tsr/model/{caja-chica,movimiento-caja-chica,cierre-caja-chica,path-caja-chica,saldo-caja-chica}.ts` espejo de las entidades; `modules/tsr/service/{caja-chica,movimiento-caja-chica,cierre-caja-chica,path-caja-chica}.service.ts` con los métodos del contrato; constantes `RS_CJCH`, `RS_MVCH`, `RS_CRCH`, `RS_PTCH` en `ws-tsr.ts`.

### T2. Parametrización — `modules/tsr/forms/caja-chica/parametrizacion/cajas-chicas.component.ts`
Ruta `menutesoreria/parametrizacion/caja-chica`, menú "Cajas chicas" bajo Parametrización. Listado de cajas de la empresa con saldo y semáforo de alerta (`/cjch/saldos/{idEmpresa}`). Formulario: nombre, cuenta contable (`shared/components/plan-cuenta-selector-dialog`), monto del fondo, tope por gasto (opcional), % de alerta (default 20), responsable, custodio (opcional), observación, y **solo al crear** "Saldo inicial migrado" con ayuda: "Úselo solo para cajas que ya existían como cuenta bancaria; el saldo ya está contabilizado". Guardar → `/cjch/registrar`. Editar → `PUT /cjch` (sin tocar el saldo).

### T3. Gastos — `modules/tsr/forms/caja-chica/gastos/gastos-caja-chica.component.ts`
Ruta `menutesoreria/procesos/caja-chica/gastos`, menú "Caja chica → Gastos". Cabecera: selector de caja (activas) con tarjeta de saldo (`fondo`, `saldo`, `porcentaje`, barra; banner de alerta si `alerta`). Formulario: fecha (default hoy), valor, concepto, **observación (obligatoria)**, producto (usar `shared/components/grupo-producto-selector-dialog` como en registro-egreso), beneficiario (opcional, `shared/components/titular-selector-dialog`), nº comprobante, y zona de adjunto (opcional, `FileService.uploadFileCustomPath` tras crear el gasto, luego `POST /ptch`; patrón de `modules/cxp/forms/negociaciones/detalle-negociacion/detalle-negociacion.component.ts` ~l.297-326). Validaciones en cliente: valor ≤ saldo, valor ≤ tope si existe. Listado de movimientos de la caja (`/mvch/listar`) con filtros de fecha y tipo, iconos por tipo, botón "Anular" (solo gastos activos sin cierre; pide motivo), botón "Adjuntos" (ver/descargar/eliminar, `FileService.downloadFile`).

### T4. Reposición / apertura — `modules/tsr/forms/caja-chica/reposicion/reposicion-caja-chica.component.ts`
Ruta `menutesoreria/procesos/caja-chica/reposicion`. Selector de caja; muestra saldo, fondo y **monto sugerido** (`montoSugeridoReposicion`) precargado y editable (máximo = sugerido). Cuenta bancaria origen (`CuentaBancariaService.getAll()`), **forma de pago** con la misma lógica del prompt 03 (Cheque solo si la cuenta `manejaChequera === 1`, con "Se girará el cheque N° X"), referencia, fecha, descripción. Botón "Reponer" → `/mvch/reposicion`; si el saldo es 0 y la caja no tiene movimientos, el botón se llama "Aperturar" → `/mvch/apertura`. Mostrar el resultado (`estadoPago`, `numeroCheque`) y recordar que si la forma de pago es transferencia el pago queda REGISTRADO hasta confirmarlo en pagos-transferencia.

### T5. Cierre / arqueo — `modules/tsr/forms/caja-chica/cierre/cierre-caja-chica.component.ts`
Ruta `menutesoreria/procesos/caja-chica/cierre`. Selector de caja; histórico de cierres (`/crch/listar/{idCaja}`). "Nuevo cierre": fecha → `/crch/preparar` → muestra periodo, saldo inicial, totales, saldo libros y la tabla de movimientos. Campo "Saldo físico contado" → diferencia calculada en vivo; si ≠ 0 exigir cuenta contable de diferencia (`shared/components/plan-cuenta-selector-dialog`) y mostrar el texto "Se generará un ajuste por $X". "Confirmar cierre" → `/crch/confirmar/{id}`. "Anular cierre" (solo el último cerrado) → `/crch/anular/{id}` con motivo. Ver movimientos de un cierre → `/crch/movimientos/{id}`.

### T6. Alerta global
En `modules/tsr/menu/menutesoreria/menutesoreria.component.ts` (o el dashboard de tesorería si existe): al cargar, llamar `/cjch/saldos/{idEmpresa}` y mostrar un chip/banner "Caja chica X al N% — reponer" por cada caja con `alerta = true`, con enlace a Reposición.

### T7. Menú y rutas
`app.routes.ts` con `canActivate:[authGuard]`; menú Tesorería: Parametrización → "Cajas chicas"; Procesos → "Caja chica" → Gastos / Reposición / Cierre.

## Restricciones
- No inventar endpoints; anotar faltantes como pendientes para el backend.
- Reutilizar selectores existentes (plan de cuentas, titular, grupo/producto, cuenta bancaria); no crear nuevos si ya hay uno.
- Entregar: archivos, rutas/menús, pendientes, y descripción de una prueba manual completa (crear caja → aperturar por transferencia → registrar gasto con adjunto → alerta → reposición con cheque → cierre con diferencia).

# PROMPT 03 — AGENTE FRONTEND — Pago con cheque: chequeras, selector de forma de pago y procesos de cheques

**Agente:** FRONTEND (`C:\work\saaFE\v1\saaFE`, Angular 20 standalone, signals, Material). **No tocar el backend.**
**Prerrequisito:** backend del prompt 02 desplegado. Endpoints disponibles (base `/SaaBE/rest`):

| Método | URL | Body / respuesta |
|---|---|---|
| GET | `/chqr/sugerirInicio/{idCuenta}` | → `{"siguiente": 1051}` (leer `.siguiente`, no el número pelado) |
| POST | `/chqr/registrarRecepcion` | `{idCuentaBancaria, comienza, finaliza, fechaEntrega:"yyyy-MM-ddTHH:mm:ss", idUsuario}` → Chequera |
| GET | `/chqr/resumen/{idChequera}` | → `{comienza, finaliza, total, disponibles, generados, impresos, entregados, anulados, siguiente}` |
| GET | `/chqr/porCuenta/{idCuenta}` | → Chequera[] |
| POST | `/chqr/anular/{id}` | `{motivo, idUsuario}` |
| GET | `/dtch/siguiente/{idCuenta}` | → `{idCheque, numero}`; 404 `{mensaje}` si no hay |
| GET | `/dtch/listar?idEmpresa&idCuenta&estado&desde&hasta` | → filas `{idCheque, numero, estado, valor, beneficiario, fechaUso, fechaImpresion, fechaEntrega, idPago, tipoPago, referenciaPago, numeroCuenta, banco}` |
| POST | `/dtch/anular/{id}` | `{motivo, idUsuario}` (solo cheques ACTIVO) |
| POST | `/dtch/imprimir` · `/dtch/entregar` | `{ids:[...], idUsuario}` |
| POST | `/pgtr`, `/egrs/procesar`, `/antp/procesar` | ahora aceptan `formaPago` (2 Transferencia, 3 Cheque, 4 Débito automático); respuesta incluye `numeroCheque` |

Rubros: estado de cheque = padre **26** (1 ACTIVO, 2 ANULADO, 3 GENERADO, 4 IMPRESO, 5 DAÑADO, 6 ENTREGADO); estado de chequera = padre **25**; motivo anulación cheque = padre **38** (1 error de tipeo, 2 error de usuario, 3 chequera anulada, 4 pago reversado). Usar `DetalleRubroService.getDetallesByParent()`.
Fechas: enviar `LocalDateTime` como ISO local sin zona (`yyyy-MM-ddTHH:mm:ss`), `LocalDate` como `yyyy-MM-dd`; nunca un `Date` crudo ni `Z`. Leer con `FuncionesDatosService.convertirFechaDesdeBackend()`.
Patrón del proyecto: `CLAUDE.md` de saaFE; rutas en `src/app/app.routes.ts`; menú en `modules/tsr/menu/menutesoreria/menutesoreria.component.ts`; servicios en `modules/tsr/service/*.service.ts` con endpoints en `ws-tsr.ts` (`RS_CHQR`, `RS_DTCH`, `RS_CNBC` ya existen).

## Tareas

### T1. Cuentas bancarias — `modules/tsr/forms/cuentas-bancarias/`
- Agregar al modelo `CuentaBancaria` (`modules/tsr/model/`) `manejaChequera?: number`.
- En el formulario, un toggle "Maneja chequera" (0/1). Mostrar la columna en el listado.

### T2. Recepción de chequera — `modules/tsr/forms/chequeras/recepcion-chequera/`
- Al elegir la cuenta (solo cuentas con `manejaChequera === 1`): llamar `GET /chqr/sugerirInicio/{idCuenta}` y precargar "Comienza en". Campo "Termina en" y, alternativamente, "Cantidad de cheques" (al escribir cantidad calcular fin = inicio + cantidad − 1 y viceversa). Fecha de entrega.
- Botón "Registrar recepción" → `POST /chqr/registrarRecepcion`. Eliminar la llamada comentada `getMaxNumeroCheque` (línea ~241) y cualquier grabación directa con `ChequeraService.add` para este flujo.
- Tras registrar, mostrar el resumen (`/chqr/resumen/{id}`).
- Mantener `solicitud-chequera` como está (solo registra la solicitud, estado 3).

### T3. Chequera / cheques — `modules/tsr/forms/chequeras/chequera/chequera.component.ts`
- Listado de chequeras por cuenta (`/chqr/porCuenta/{idCuenta}`) con su resumen; al seleccionar una, listar sus cheques con `POST /dtch/selectByCriteria` (criterio `chequera.codigo`) mostrando número, estado (label del rubro 26), valor, beneficiario, fechas.
- Acciones: "Anular cheque" (solo estado 1) → diálogo con motivo (rubro 38, opciones 1 y 2) → `POST /dtch/anular/{id}`. "Anular chequera" → `POST /chqr/anular/{id}`.

### T4. Selector de forma de pago en los tres registros de salida de dinero
Aplicar el mismo bloque en:
- `modules/cxp/forms/pagos/pagos-transferencia/pagos-transferencia.component.ts` (+ html),
- `modules/tsr/forms/registrar/registro-egreso/registro-egreso.component.ts` (+ html),
- `modules/tsr/forms/anticipos/anticipos-proveedores/anticipos-proveedores.component.ts` (+ html).

Comportamiento:
1. Reemplazar el toggle "débito automático" por un `mat-radio-group`/`mat-select` **Forma de pago** con opciones: Transferencia (2), Débito automático (4) y, **solo si la cuenta origen seleccionada tiene `manejaChequera === 1`**, Cheque (3). Al cambiar de cuenta, si la forma era Cheque y la nueva cuenta no maneja chequera, volver a Transferencia.
2. Al enviar: `formaPago` en el payload y `debitoAutomatico = formaPago === 4 ? 1 : 0` (mantener el campo por compatibilidad).
3. Si Cheque: ocultar/desactivar el selector de cuenta destino del beneficiario (no es obligatorio); consultar `GET /dtch/siguiente/{idCuenta}` y mostrar "Se girará el cheque N° X"; si devuelve 404 mostrar el `mensaje` y bloquear el botón de guardar.
4. Tras guardar, si la respuesta trae `numeroCheque`, mostrarlo en el snackbar/diálogo de confirmación.
5. En los listados de pagos de esas pantallas, mostrar la forma de pago (labels de `FormaPagoAplicacion` en `shared/model/pagos-cobros/catalogos-aplicacion-pago.ts`) y el número de cheque (`pago.cheque?.numero`) cuando exista. Los pagos con cheque no se pueden incluir en "Generar lote": excluirlos de la selección igual que los de débito automático.

### T5. Reemplazar las maquetas de procesos de cheques por pantallas reales
Estas pantallas hoy tienen datos hardcodeados; conectarlas a `GET /dtch/listar` con filtros (empresa del `AppStateService`, cuenta, estado, rango de fechas):
- `modules/tsr/forms/pagos/procesos/generados/cheques-generados.component.ts` → estado 3; selección múltiple + botón "Marcar impresos" → `POST /dtch/imprimir`.
- `modules/tsr/forms/pagos/procesos/impresos/cheques-impresos-proc.component.ts` → estado 4; botón "Marcar entregados" → `POST /dtch/entregar`.
- `modules/tsr/forms/pagos/procesos/entregados/cheques-entregados-proc.component.ts` → estado 6; solo consulta.
- `modules/tsr/forms/pagos/consultas/cheques/consultas-cheques.component.ts` → todos los estados, con filtros.
- `modules/tsr/forms/pagos/cheques/impresion/cheques-impresion.component.ts` y `.../entrega/cheques-entrega.component.ts`: **eliminar** sus rutas y entradas de menú (duplican Generados/Impresos) o redirigirlas a las anteriores. `pagos/ingresar/pagos-ingresar.component.ts` y `pagos/consultas/pagos/consultas-pagos.component.ts` (circuito legado PGSS): quitar del menú, no borrar archivos.
- Cada fila: número, cuenta/banco, beneficiario, valor, fecha de giro, tipo de pago (FACTURA/EGRESO/ANTICIPO/EXTERNO), referencia, estado. Acción "Ver pago" que navegue a la pantalla del pago cuando `idPago` exista.
- Crear `modules/tsr/model/cheque-listado.ts` para el DTO de `/dtch/listar` y agregar en `cheque.service.ts` los métodos `siguiente`, `listar`, `anular`, `imprimir`, `entregar`; en `chequera.service.ts` los métodos `sugerirInicio`, `registrarRecepcion`, `resumen`, `porCuenta`, `anular`.

### T6. Verificación (describir en el informe)
Flujo completo en local: marcar una cuenta con chequera → recibir chequera 1..50 → pagar una factura CXP con cheque → ver que el pago queda CONFIRMADO con cheque 1 → en Generados marcar impreso → en Impresos marcar entregado → reversar el pago desde pagos-transferencia y ver el cheque en estado ANULADO con motivo "pago reversado".

## Restricciones
- No cambiar el comportamiento actual para transferencia y débito automático.
- No inventar endpoints: si falta alguno, anotarlo en el informe final como pendiente para el backend.
- Entregar: archivos creados/modificados, rutas y menús tocados, pendientes.

## Contrato verificado contra el código del backend (2026-08-27)

Confirmado leyendo `ChequeraRest`, `ChequeRest` y sus ServiceImpl. Úsalo tal cual:

- `GET /chqr/sugerirInicio/{idCuenta}` → `{"siguiente": N}`.
- `POST /chqr/registrarRecepcion` body `{idCuentaBancaria, comienza, finaliza, fechaEntrega, idUsuario}`; `fechaEntrega` es **String ISO local sin zona** (`"2026-08-26T09:00:00"`).
- `GET /chqr/resumen/{idChequera}` → `{comienza, finaliza, total, disponibles, generados, impresos, entregados, anulados, siguiente}`.
- `POST /chqr/anular/{id}` body `{motivo, idUsuario}` — aqui `motivo` es **texto libre**.
- `GET /dtch/siguiente/{idCuenta}` → `{idCheque, numero}`; 404 cuando no hay disponibles.
- `GET /dtch/listar` query params `idEmpresa, idCuenta, estado, desde, hasta`; `desde`/`hasta` en formato **`yyyy-MM-dd`** (el backend hace `LocalDate.parse` directo: cualquier otro formato da error 500).
  Respuesta: lista de `{idCheque, numero, estado, valor, beneficiario, fechaUso, fechaImpresion, fechaEntrega, numeroCuenta, banco, idPago, tipoPago, referenciaPago}`. `estado` es el **codigo numerico** del rubro 26; `tipoPago` es `"FACTURA" | "EGRESO" | "ANTICIPO" | "EXTERNO"` y `referenciaPago` trae el numero de factura, la descripcion del egreso o el numero del anticipo segun el caso.
- `POST /dtch/anular/{id}` body `{motivo, idUsuario}` — aqui `motivo` es **numerico** (rubro 38: 1, 2 o 3). Ojo: distinto del de chequera, que es texto.
- `POST /dtch/imprimir` y `POST /dtch/entregar` body `{ids: [...], idUsuario}`.
- **Filtro de fechas:** va contra `fechaUso`, que es null en los cheques ACTIVO. Si pones un rango por defecto desaparecen los disponibles — deja el rango vacio salvo que el usuario lo llene.
- **Errores:** el backend responde texto y el filtro `MensajeErrorJsonFilter` lo envuelve como `{"mensaje": "..."}`. Leer siempre `error.error?.mensaje ?? error.message`.

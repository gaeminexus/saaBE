# PROMPT 07 — AGENTE FRONTEND — Emisión de liquidaciones de compra: conectar al flujo real y clasificar por producto

**Agente:** FRONTEND (`C:\work\saaFE\v1\saaFE`). **No tocar el backend.**
**Prerrequisito:** backend del prompt 06 desplegado. Leer `docs/logica-negocio/cxc/LIQUIDACION-COMPRA-EMISION.md` (lo escribe el agente BE) para el payload exacto.

## Diagnóstico ya verificado
`modules/cxc/forms/emitir/liquidaciones/liquidaciones.component.ts` llama a `POST /lqcs/grabarLiquidacion` (`service/emitir/liquidacion-emitir.service.ts:29`) y a `/dtlc/*` (`detalle-liquidacion-emitir.service.ts:17,35`): el primero no existe; el segundo existe recién desde el prompt 06. Nunca llama a `/lqcs/procesarCompleta`. Manda `tipoDoc: '04'` (debe ser `'03'`), `detalleLiquidacion` y `formaPagosFactura` con nombres que el backend no consume. La pantalla de facturas (`modules/cxc/forms/emitir/facturas-ingreso/facturas-ingreso.component.ts`, ~l.726) es la referencia correcta.

## Tareas
1. **Payload y endpoint**: en `liquidacion-emitir.service.ts` reemplazar `grabarLiquidacion` por `procesarCompleta(payload)` → `POST /lqcs/procesarCompleta` con `{liquidacionCompra:{...}, detalles:[...], formasPago:[...]}` (mismos nombres de campo que usa `facturas-ingreso` para `procesarCompleta` de factura, adaptados a `LiquidacionCompra`/`DetalleLiquidacionCompra`/`FormaPagoLiquidacion`). `tipoDoc: '03'`. Fechas `LocalDate` → `yyyy-MM-dd`.
2. **Producto por línea**: cada detalle debe llevar `producto: {id}` (ProductoPago de CXP). Agregar en la grilla de detalle un selector de producto usando `shared/components/grupo-producto-selector-dialog` (el mismo de registro-egreso); mostrar grupo/producto en la fila. Sin producto no se puede emitir (validación en cliente con mensaje "Clasifique todas las líneas").
3. **Formas de pago**: capturar al menos una (formaPago SRI, total, plazo, unidadTiempo) igual que en facturas; enviar en `formasPago`. Cambiar el endpoint del servicio de formas de pago a `/fplc`.
4. **Resultado**: tratar la respuesta como en facturas (`estado` COMPLETADO / COMPLETADO_CON_PENDIENTES / errores por etapa); mostrar número, clave de acceso, autorización, y las advertencias (`advertenciaAsiento`, `PRODUCTOS_SIN_CLASIFICAR`).
5. **Acciones sobre liquidaciones emitidas** (en el listado/consulta de la misma pantalla o en `consultas` de CXC si existe una para facturas): "Reintentar autorización" → `POST /lqcs/reintentarAutorizacion/{id}`; "Consultar estado SRI" → `GET /lqcs/consultarYActualizarEstado/{id}`; "Reenviar email" → `POST /lqcs/reenviarEmail/{id}`; "Anular" → `POST /lqcs/anular/{id}` (con confirmación); "Crear documento CXP" → `POST /lqcs/crearDocumentoCxp/{id}` visible solo cuando está autorizada y `documentoCxp == null`; "Descargar RIDE/XML" con `PathLiquidacionCompra` (`/ptlc/selectByCriteria` por `liquidacion.id`) + `FileService.downloadFile`.
6. **Retención**: botón "Emitir retención" en una liquidación autorizada que navegue a la pantalla de Retención V2 (`modules/cxc/forms/emitir/retencionesv2` (carpeta verificada; confirmar la ruta en `app.routes.ts`)) con query params `codDocSustento=03`, `numDocSustento={numero completo}`, `fechaEmisionDocSustento`, `idProveedor`, y que esa pantalla precargue esos valores si vienen en la URL (cambio mínimo en el componente de retención).
7. Verificar en `app.routes.ts` que la ruta `emitir/liquidaciones` sigue apuntando al componente y que el menú la muestra.

## Restricciones
- No inventar endpoints; los no listados se anotan como pendientes para el backend.
- No cambiar el flujo de facturas.
- Entregar: archivos modificados, payload final de ejemplo, pendientes, y descripción de una prueba manual (emitir una liquidación con dos líneas clasificadas → ver autorización → ver que aparece en CXP consulta-documentos/proposición de pago → emitir retención desde el botón).

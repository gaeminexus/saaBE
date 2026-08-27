# Liquidación de compra — emisión (CXC), cuenta por pagar (CXP), clasificación contable por producto

**Fecha:** 2026-08-27 (PROMPT 06). **Estado:** implementado, RIDE pendiente de compilar `.jasper`.

## 1. Qué es una liquidación de compra

Documento electrónico tipo `03` que la propia empresa emite en nombre de un
proveedor que no puede facturar (persona natural no obligada, informal, etc.).
La empresa es a la vez **emisora ante el SRI** (CXC, `CBR.LQCS`) y **deudora**
del proveedor (CXP): al autorizarse, la liquidación crea automáticamente su
propia cuenta por pagar.

## 2. Decisión de diseño (no cambiar sin revisar el impacto)

La cuenta por pagar y el asiento contable de una liquidación **emitida** viven
en **CXP**, no en CXC:

- `CBR.LQCS` (CXC) es sólo el trámite de emisión ante el SRI: clave de acceso,
  XML, firma, autorización. **No genera asiento ni cuenta por pagar propios.**
- Al autorizarse por el SRI, `LiquidacionCompraServiceImpl.crearDocumentoCxp`
  crea `PGS.LQCC` (el mismo documento que usa la carga automática de
  liquidaciones recibidas del SRI, `LiquidacionCompraCompra`) con sus detalles
  y path, y lo contabiliza con
  `AsientoContableServiceImpl.generarAsientoLiquidacionCompraCompra`
  (`TipoAsientos.LIQUIDACIONES_COMPRA_RECIBIDAS`, codigoAlterno=3) — **exactamente
  como si el SRI hubiera "recibido" la liquidación del lado de compras.**
- `CBR.LQCS.LQCSLQCC` guarda el puntero al `PGS.LQCC` creado (columna agregada
  por `docs/logica-negocio/cxc/sql/add-liquidacion-compra-emision.sql`,
  prerrequisito de este cambio, ya ejecutado).
- **La retención no se genera automáticamente.** El usuario la emite aparte,
  con **Retención V2**, sustento `03` (liquidación de compra) — el circuito de
  retenciones ya soporta ese sustento; no hay wiring adicional que hacer aquí.

Por qué así y no un asiento propio en CXC: evita duplicar toda la lógica de
clasificación por producto, IVA crédito tributario y cuenta CxP que ya existe
y está probada para la recepción de liquidaciones — y mantiene una sola fuente
de verdad para "cuánto le debo a este proveedor", sea que la liquidación haya
llegado por el SRI (de un tercero) o la haya emitido esta misma empresa.

## 3. Flujo completo

```
POST /lqcs/procesarCompleta
 │
 ├─ PASO 0  validarContabilidadLiquidacion         (sin BD, bloqueante)
 │           AsientoContableService.validarCuentasContablesLiquidacion
 │             1. Cuenta CxP del proveedor/prestador (obtenerCuentaProveedor)
 │             2. Cuenta IVA crédito tributario por cada % usado (obtenerCuentaIVACxp)
 │             3. Cuenta del grupo de CADA producto de los detalles
 │                → PRODUCTOS_SIN_CLASIFICAR si algún detalle no tiene producto
 │
 ├─ emitirLiquidacionAnteSRI                        @TransactionAttribute(REQUIRES_NEW)
 │   PASO 1  Preparar campos en memoria (secuencial, número, clave de acceso)
 │   PASO 2  Generar XML (detalles reales + pagos reales — antes iban vacíos)
 │   PASO 3  Firmar XML
 │   PASO 4a WS1 Recepción → si RECIBIDA, grabar LQCS + DetalleLiquidacionCompra
 │                            + FormaPagoLiquidacion (antes NUNCA se persistían)
 │   PASO 4d WS2 Autorización → si AUTORIZADO: estado=5, autorizacion, RIDE (PDF)
 │
 ├─ crearDocumentoCxp(idLiquidacion)                 @TransactionAttribute(REQUIRES_NEW)
 │   Idempotente (liquidacion.documentoCxp != null → no repite).
 │   Copia cabecera (incl. pathGen) + detalles (con producto) a PGS.LQCC/DLCM,
 │   copia las formas de pago a PGS.FPLM (las lee la pantalla CxP → Consulta
 │   de documentos) y el path del XML autorizado a PGS.PTLC, genera el asiento
 │   (LIQUIDACIONES_COMPRA_RECIBIDAS) y enlaza LQCS.LQCSLQCC → LQCC.ID.
 │   Si falla: no revierte la emisión — resultado queda COMPLETADO_CON_PENDIENTES
 │   con advertenciaAsiento, y POST /lqcs/crearDocumentoCxp/{id} reintenta solo.
 │
 └─ Email (RIDE + XML autorizado) al proveedor      (sin transacción, no crítico)
```

Igual que Factura: la emisión ante el SRI es irreversible, así que corre en su
propia transacción (`REQUIRES_NEW`); un fallo tardío en `crearDocumentoCxp` o en
el email **nunca** reversa una liquidación ya autorizada.

## 4. Payload de `POST /lqcs/procesarCompleta`

```json
{
  "liquidacionCompra": {
    "ptoEmision": { "id": 1 },
    "facturador": { "id": 1 },
    "titular": { "codigo": 45 },
    "fecha": "2026-08-27T09:00:00",
    "subtotal": 100.00,
    "subcero": 0.0,
    "pIVA": 15.0,
    "vIVA": 15.00,
    "descuento": 0.0,
    "total": 115.00,
    "observacion": "Compra de reciclaje a proveedor no obligado a facturar"
  },
  "detalles": [
    {
      "descripcion": "Cartón reciclado",
      "cantidad": 100.0,
      "valor": 1.0,
      "subTotal": 100.0,
      "porcentajeIVA": 15,
      "valorIVA": 15.0,
      "descuento": 0.0,
      "total": 115.0,
      "producto": { "id": 12, "codigo": "REC-001", "grupoProducto": { "codigo": 3, "planCuenta": { ... } } }
    }
  ],
  "formasPago": [
    { "formaPago": "01", "valor": 115.0, "plazo": 0, "unidadTiempo": "dias" }
  ]
}
```

- `detalles` es **obligatorio**, al menos uno — igual que Factura.
- `producto` de cada detalle debe venir con `grupoProducto.planCuenta` ya
  resuelto (el mismo contrato que usa Factura para `producto.grupoProducto`):
  el backend no hace un `SELECT` adicional para completarlo antes de validar.
- `formasPago` es **opcional**. Si no viene, se usa `"01"` (Sin utilización del
  sistema financiero) por el total de la liquidación — igual criterio que usa
  el XML por defecto cuando no hay formas de pago explícitas.

Respuesta (éxito, sin pendientes):

```json
{
  "exito": true,
  "estado": "AUTORIZADO",
  "etapa": "COMPLETADO",
  "clave": "27082026...",
  "idLiquidacion": 501,
  "autorizacion": "AUTORIZADO",
  "documentoCxp": 234,
  "asiento": "AS-000123",
  "emailEnviado": true
}
```

Con pendientes (`etapa: "COMPLETADO_CON_PENDIENTES"`): la liquidación **sí**
quedó autorizada por el SRI (`exito: true`); revisar `advertenciaAsiento` /
`advertenciaEmail` y usar los endpoints de recuperación del §6.

## 5. Asiento contable (glosas)

Generado por `generarAsientoLiquidacionCompraCompra` sobre el documento CXP
(`PGS.LQCC`), agrupando el DEBE por `producto.grupoProducto.planCuenta` — igual
patrón que `generarAsientoFacturaCompra`:

| Línea | Cuenta | Glosa | Valor |
|---|---|---|---|
| DEBE | `GrupoProductoPago.planCuenta` (una por grupo) | `"Gasto liquidación compra: {nombreGrupo}"` | Suma de `subTotal` por grupo |
| DEBE (sólo si hay detalles sin producto) | Cuenta de gasto default CXP | `"Gasto liquidación compra (SIN CLASIFICAR): {numero}"` | Suma de `subTotal` de esos detalles |
| DEBE | IVA crédito tributario (`obtenerCuentaIVACxp`) | `"IVA crédito tributario liquidación compra código SRI: {codigo}"` | `LQCC.vIVA` repartido por código IVA de los detalles |
| HABER | Cuenta CxP del proveedor/prestador | `"CxP Prestador: {nombre}"` | Suma de las líneas DEBE (garantiza cuadratura) |

Observación de cabecera del asiento:
`"Liquidación de Compra N° {numero} | Proveedor: {nombre} | Aut: {autorizacion}"`.

En la emisión propia, la línea "SIN CLASIFICAR" no debería aparecer nunca —
`validarCuentasContablesLiquidacion` la bloquea antes de emitir. Es una válvula
de seguridad para no dejar un documento ya autorizado por el SRI sin
contabilizar si, por lo que sea, un detalle llega sin producto.

## 6. Endpoints de recuperación

| Endpoint | Uso |
|---|---|
| `POST /lqcs/reintentarAutorizacion/{id}` | La liquidación quedó RECIBIDA/ENVIADA pero sin respuesta de autorización (WS2) — reintenta sólo esa consulta; si autoriza, también crea el documento CXP. |
| `GET /lqcs/consultarYActualizarEstado/{id}` | Punto de recuperación general: consulta el SRI, actualiza el estado si falta, crea el documento CXP si falta, reenvía el email si falta. Idempotente, seguro de llamar varias veces. |
| `POST /lqcs/crearDocumentoCxp/{id}` | Reintenta sólo la creación del documento CXP / asiento (p.ej. cuando la emisión SRI salió bien pero esta etapa falló). Idempotente. |
| `POST /lqcs/reenviarEmail` | `{ "idLiquidacion": id, "destinatarios": "a@x.com;b@x.com" }` — reenvía RIDE + XML a uno o más correos; regenera el PDF si no está en disco. |
| `POST /lqcs/anular` | `{ "idLiquidacion": id, "motivo": "...", "usuario": "..." }` — ver §7. |

## 7. Anulación — pendiente conocido

`POST /lqcs/anular` anula el asiento del documento CXP (si existe), pasa
`PGS.LQCC.ESTADO = 0` y `CBR.LQCS.ESTADO = 0` (`Estado.INACTIVO`, el mismo que
usan las facturas de CXC anuladas) + `LQCS.ESTADOEMISION = 3`.

**No verifica que el documento CXP no esté pagado**, a diferencia de lo que se
planteó en el pedido original. Investigado y confirmado: **hoy no existe ningún
mecanismo para pagar/aplicar contra un `LiquidacionCompraCompra`** —
`AplicacionPagoCxp` sólo tiene FK a `FacturaCompra` (`facturaCompra`, tipada a
esa entidad), y `PagoProgramado` tampoco tiene FK a `LiquidacionCompraCompra`.
Es un vacío preexistente en el módulo de pagos CXP, no algo que este cambio
haya introducido. Si en el futuro se implementa el pago de liquidaciones
recibidas, agregar el bloqueo real en `LiquidacionCompraServiceImpl.anularLiquidacion`
antes de anular.

## 8. Retención de la liquidación

**No se genera automáticamente.** El flujo de emisión no crea ninguna
retención. El usuario debe emitirla aparte con **Retención V2**
(`RetencionV2ServiceImpl`), indicando como sustento el tipo `03` (liquidación
de compra) y el número/clave de la liquidación recién emitida. El circuito de
Retención V2 ya soporta ese sustento sin cambios adicionales.

## 9. RIDE (PDF)

`RPRT_RIDE_LIQUIDACION.jrxml` (copiado de `RPRT_RIDE_FACTURA.jrxml`, mismo
layout, mismo parámetro único `P_ID_LIQUIDACION` en vez de `P_ID_FACTURA`,
título cambiado a "LIQUIDACIÓN DE COMPRA", consulta SQL reescrita sobre
`CBR.LQCS`/`CBR.DTLC`/`CBR.FPLC` con `JOIN PGS.PRDP` para el producto).

**⚠️ Falta compilar `RPRT_RIDE_LIQUIDACION.jasper` con Jaspersoft Studio 7.0.3**
y comitearlo junto al `.jrxml` (ver CLAUDE.md § Reportes: no hay compilación en
tiempo de ejecución en JasperReports 7.0.3). Mientras no exista el `.jasper`,
`generarPDFLiquidacion` captura el error, deja la liquidación **sin RIDE** y
**no aborta la emisión** — el XML autorizado y el asiento sí quedan completos.

## 10. Mapeo de código de IVA en el XML

`DetalleLiquidacionCompra` no guarda el código SRI de IVA (a diferencia de
`DetalleFactura.codigoIVASRI`), sólo el porcentaje (`porcentajeIVA`, ej. 15,
12, 5, 8, 0). El XML se genera mapeando ese porcentaje al código SRI con la
misma tabla que usa `AsientoContableServiceImpl.mapPorcentajeIVAaCodigo`
(duplicada a propósito como `codigoPorcentajeIVA` en `LiquidacionCompraServiceImpl`
— métodos privados de otra clase no son reutilizables): `0→"0"`, `5→"5"`,
`8→"8"`, `12→"2"`, `14→"3"`, `15→"4"`, cualquier otro valor se emite tal cual.

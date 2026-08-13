# Proceso de Carga de Documentos CXP

> **Archivo de referencia principal.**  
> Última revisión: 2026-08-13 — verificado contra
> `ProcesoCargaDocumentosServiceImpl`, `ProcesoCargaDocumentosRest`,
> `AplicacionPagoCxp*` (CXP) y `AplicacionPagoCxc*` (CXC).

**Módulo:** CXP - Cuentas por Pagar  
**Stack:** Jakarta EE · WildFly · Oracle DB · Schema PGS

---

## 1. Arquitectura

### Tablas de control de la carga

| Tabla | Entidad Java | Propósito |
|---|---|---|
| `PGS.CRTX` | `CargaArchivoTxt` | Cabecera de cada archivo TXT cargado |
| `PGS.DCXP` | `DocumentoCxp` | **Un solo registro por documento** (por `claveAcceso`). Ciclo de vida completo |
| `PGS.DCTX` | `DetalleCargaTxt` | Una línea por aparición en un TXT. FK a DCXP |

### Tablas de aplicación de pago

| Tabla | Entidad Java | Propósito |
|---|---|---|
| `PGS.APLP` | `AplicacionPagoCxp` | Abonos/cargos sobre una **factura de compra** (lo que nosotros debemos) |
| `CBR.APLC` | `AplicacionPagoCxc` | Abonos/cargos sobre una **factura de venta** (lo que nos deben) |

> Toda retención que un proveedor/cliente nos emite abona una factura de **venta**,
> así que se registra en `AplicacionPagoCxc`, no en `AplicacionPagoCxp`. Ver §7.

### Archivos Java clave

| Archivo | Paquete | Rol |
|---|---|---|
| `ProcesoCargaDocumentosServiceImpl.java` | `com.saa.ejb.cxp.serviceImpl` | Implementación completa de las fases |
| `ProcesoCargaDocumentosRest.java` | `com.saa.ws.rest.cxp` | Endpoints REST del proceso |
| `AsientoContableServiceImpl.java` | `com.saa.ejb.cnt.serviceImpl` | Generación de asientos contables CXP |
| `AplicacionPagoCxpServiceImpl.java` | `com.saa.ejb.cxp.serviceImpl` | Aplicaciones sobre facturas de compra |
| `AplicacionPagoCxcServiceImpl.java` | `com.saa.ejb.cxc.serviceImpl` | Aplicaciones sobre facturas de venta |
| `ResultadoCargaTxt.java` | `com.saa.rubros` | Constantes de resultado por línea (rubro 174) |
| `EstadoDocumentoCxp.java` | `com.saa.rubros` | Constantes de estado del documento (rubro 175) |
| `EstadoNovedad.java` | `com.saa.rubros` | PENDIENTE=1, REEMPLAZADO=2, MANTENIDO=3 |
| `TipoGrupoProductos.java` | `com.saa.rubros` | BIEN=1, SERVICIO=2, POR_CLASIFICAR=3 |
| `TipoAsientos.java` | `com.saa.rubros` | `codigoAlterno` de tipos de asiento CXP |
| `TipoDocPagoAplicacion.java` | `com.saa.rubros` | Tipo de documento que paga (1..5) |
| `EstadoPagoFactura.java` | `com.saa.rubros` | PENDIENTE=1, PAGADA_PARCIAL=2, PAGADA_TOTAL=3 |
| `EstadoAplicacionPago.java` | `com.saa.rubros` | ACTIVO=1, REVERSADO=2 |

---

## 2. Estados del DocumentoCxp (rubro 175)

| Valor | Nombre | Descripción | Botón frontend |
|---|---|---|---|
| `1` | LEIDO | Leído del TXT, pendiente de XML | "Cargar XML y Registrar" |
| `2` | XML_CARGADO | Transitorio interno — **también es el estado en que queda un documento detenido por bloqueantes** | "Reintentar" |
| `3` | REGISTRADO_BD | Registrado en tablas CXP + asiento contable + aplicación de pago | "Revertir" |
| `4` | ERROR | Falló algún paso. Ver campo `observacion` | "Reintentar" |
| `5` | NOVEDAD | Diferencias detectadas o desaparecido en recarga | "Resolver novedad" |
| `6` | REVERTIDO | BD revertida y asiento anulado | "Cargar XML y Registrar" |

> ⚠️ En el flujo normal `/procesarXml/{id}` pasa de `1 → 3`. El estado `2` sí es
> visible cuando el registro se detiene por bloqueantes (§5 Paso 3): el XML ya
> quedó guardado y `observacion` explica qué falta. Al resolverlo se puede
> reintentar con `/procesarXml` o con `/registrarBD/{id}` (que exige estado `2`).

---

## 3. Tablas destino por tipo de comprobante

| `tipoComprobante` (del TXT) | `tipoTablaDestino` | Tablas que se llenan | Estado |
|---|---|---|---|
| `Factura` | `FACTURA_COMPRA` | `FacturaCompra` + `DetalleFacturaCompra` + `FormaPagoFacturaCompra` + `PathFacturaCompra` | ✅ Completo |
| `Nota de Crédito` | `NOTA_CREDITO_COMPRA` | `NotaCreditoCompra` + `DetalleNotaCreditoCompra` + `PathNotaCreditoCompra` | ✅ Registro + asiento + aplicación · ⚠️ sin bloqueantes estructurados (§5) |
| `Nota de Débito` | `NOTA_DEBITO_COMPRA` | `NotaDebitoCompra` + `DetalleNotaDebitoCompra` + `PathNotaDebitoCompra` | ✅ Registro + asiento + aplicación · ⚠️ sin bloqueantes estructurados (§5) |
| `Liquidación de compra` | `LIQUIDACION_COMPRA_COMPRA` | `LiquidacionCompraCompra` + `DetalleLiquidacionCompraCompra` + `PathLiquidacionCompraCompra` | ⚠️ Registro + asiento, sin validaciones bloqueantes ni aplicación de pago |
| `Comprobante de Retención` | `RETENCION_COMPRA_V2` | `RetencionCompraV2` (`PGS.RCV2`) + `DetalleRetencionCompraV2` (`PGS.DRC2`) *(sin path)* | ✅ Completo |
| `Comprobante de Retención electrónica versión 2.0` | `RETENCION_COMPRA_V2` | Ídem | ✅ Completo |

> ⚠️ **Desde el 2026-08-11 las dos versiones del comprobante de retención se
> registran en `RetencionCompraV2` (`PGS.RCV2` + `PGS.DRC2`).** El despacho de
> `cargarXmlYRegistrar` / `registrarDocumentoBD` llama a
> `registrarRetencionCompraV2()` para ambos tipos: el parser tolera los dos
> esquemas del SRI (`obtenerDetallesRetencion` / `getValorDocSustento`).
>
> `registrarRetencionCompra()` (V1, `PGS.RTCM`) **ya no se invoca desde ningún
> despacho** — queda como código muerto de referencia. `RetencionCompra` sirve
> solo para consultar lo cargado antes de ese cambio;
> `docs/scripts/sql-migrar-retenciones-v1-a-v2.sql` lo mueve a las tablas v2.
> El tipo `RETENCION_COMPRA` sigue vivo en reversión, contabilización y
> aplicación de pago para no romper los documentos históricos.

### Trampa: el total de una retención no está en `importeTotal`

El comprobante de retención del SRI **no trae `<importeTotal>`**, y la columna
IMPORTE_TOTAL del TXT llega en `0.00`. El total de una retención es la **suma de
los `<valorRetenido>` de sus detalles**, que se calcula con
`calculaTotalRetenido(retenciones, doc)` — el mismo valor que
`generarAsientoRetencionCompra(V2)` usa para el lado HABER del asiento.

Antes se hacía `rc.setTotal(doc.getImporteTotal())`, así que `RCV2.TOTAL` quedaba
en cero y la aplicación de pago del Paso 4 moría con
*"El monto a aplicar no puede ser cero"* (`nuevaAplicacion` rechaza monto null o
cero) → rollback de todo el registro, incluido el asiento que ya se había
generado. Corregido el 2026-08-13 en las dos versiones.

El respaldo a `doc.getImporteTotal()` solo aplica si el XML no trae ningún
`<valorRetenido>`. El valor se redondea a 2 decimales para que coincida con la
columna `NUMBER(18,2)` y con lo que valida `validaMontoContraSaldo`.

### Nombre del atributo padre en el detalle V2

`DetalleRetencionCompraV2` referencia a su cabecera con el campo
**`retencionCompraV2`** (columna `DRC2.RETENCIONV2`), **no** `retencion` como en
`DetalleRetencionCompra` (V1). Toda consulta JPQL nueva debe usar
`d.retencionCompraV2.id`; escribir `d.retencion.id` produce
`UnknownPathException` en tiempo de ejecución (fue el bug corregido el
2026-08-13 en `AsientoContableServiceImpl.generarAsientoRetencionCompraV2`).

---

## 4. FASE 1 — Carga del TXT

**Endpoint:** `POST /rest/carga-documentos/cargarTxt`

**Body:**
```json
{
  "contenidoTxt": "...",
  "nombreArchivo": "facturas-julio-2026.txt",
  "idEmpresa": 1236,
  "idUsuario": 5,
  "idPeriodo": 123
}
```

### Lógica por línea del TXT

```
Para cada línea:
  Si RUC receptor ≠ empresa → IGNORADO (no cuenta)

  Buscar DocumentoCxp por claveAcceso:

  NO existe → crear (estado=LEIDO) → resultado=NUEVO (1)

  Existe, sin diferencias → resultado=DUPLICADO (2)

  Existe, con diferencias, estado=3 (REGISTRADO_BD):
    → resultado=REGISTRADO_CON_DIFERENCIAS (6)   ← NO se toca el documento

  Existe, con diferencias, estado=1/2/6 (LEIDO/XML_CARGADO/REVERTIDO):
    → actualizar valores, estado=LEIDO → resultado=NOVEDAD (3)

  Existe, con diferencias, estado=5 (NOVEDAD):
    → actualizar campo novedad → resultado=NOVEDAD (3)

  Siempre registrar una línea en DCTX
```

> **Regla fundamental:** Los documentos en estado `3 (REGISTRADO_BD)` **NUNCA se modifican** durante una recarga, ni si cambian los valores ni si desaparecen. Solo queda trazabilidad en `DCTX`.

### Detección de documentos desaparecidos

Se ejecuta solo si se envía `idPeriodo`. Filtra por los **tipos de comprobante presentes en esta carga** (si el TXT es de facturas, solo revisa facturas del período — no toca retenciones ni notas de crédito).

| Estado del documento | No aparece en la carga | Resultado |
|---|---|---|
| `1, 2, 4, 5` (pendiente de procesar) | → marca NOVEDAD/DESAPARECIDO, cambia estado | `DESAPARECIDO (5)` — requiere acción |
| `3` (ya registrado con asiento) | → solo registra en DCTX, NO modifica el documento | `REGISTRADO_DESAPARECIDO (7)` — solo informativo |

### Respuesta del TXT

```json
{
  "idCargaTxt": 45,
  "nombreArchivo": "facturas-julio-2026.txt",
  "totalRegistros": 20,
  "nuevos": 5,
  "duplicados": 10,
  "novedades": 2,
  "registradosConDiferencias": 1,
  "desaparecidos": 1,
  "detalles": [ ... ],
  "desaparecidosDetalle": [ ... ]
}
```

---

## 5. FASE 2+3 Unificada — Procesar XML

**Endpoint recomendado:** `POST /rest/carga-documentos/procesarXml/{idDocumentoCxp}`

**Body:**
```json
{ "contenidoXml": "<?xml ...", "idEmpresa": 1236, "idUsuario": 5, "pathDestino": "opcional" }
```

Si no se envía `pathDestino`, el REST sube el XML con `FileService` a
`docs/xml/cxp/{claveAcceso}.xml` antes de llamar al servicio.

### Paso 1 — Validación XML vs documento esperado

Compara: `claveAcceso`, `rucEmisor`, `razonSocialEmisor`, `serieComprobante`, `valorSinImpuestos`, `importeTotal`, `iva` (tolerancia ±0.01).

Diferencias → **HTTP 422:**
```json
{
  "valido": false,
  "errores": [
    { "campo": "rucEmisor", "esperado": "0913128088001", "enXml": "1705431771001" },
    { "campo": "importeTotal", "esperado": "2875.0", "enXml": "437.0" }
  ]
}
```

### Paso 2 — Acciones automáticas (siempre se ejecutan, no bloquean)

| Acción | Condición |
|---|---|
| Crear `Titular` con rol Proveedor | Si no existe en TSR por RUC |
| Asignar rol Proveedor al `Titular` | Si existe pero `tipoProveedor ≠ 1` |
| Crear grupo `POR CLASIFICAR` | Si no existe para la empresa |
| Crear `ProductoPago` en POR CLASIFICAR | Si no existe por nombre en la empresa |
| Asignar grupo POR CLASIFICAR al producto | Si existe pero `grupoProducto = null` |

Logs esperados:
```
ℹ Titular ya tiene rol de Proveedor: 0913128088001 | id=39 | nombre=...
✓ Rol de Proveedor asignado a Titular existente: ... | id=...
Auto-creando Titular-Proveedor para RUC: ...
⚠ Producto 'X' (ID=3) no tenía grupo → asignado a POR CLASIFICAR automáticamente.
```

### Paso 3 — Validaciones bloqueantes (HTTP 422 si alguna falla, nada se graba)

**No todos los tipos validan lo mismo.** Solo Factura y Retención tienen
bloqueantes implementados:

| Bloqueante | Factura | Retención V2 | Retención V1 *(muerto)* | NC / ND / Liquidación |
|---|---|---|---|---|
| `PROVEEDOR_SIN_CUENTA` — proveedor sin cuenta contable CxP (`PersonaCuentaContable`, `tipoCuenta=1`) | ✅ | ✅ | ✅ | ✗ |
| `TIPO_ASIENTO_NO_CONFIGURADO` — no existe `TipoAsiento` con el `codigoAlterno` del tipo, para la empresa | ✅ | ✅ | ✅ | ✗ |
| `PRODUCTOS_SIN_CLASIFICAR` — algún producto está en grupo POR CLASIFICAR | ✅ | — | — | ✗ |
| `GRUPOS_SIN_CUENTA_CONTABLE` — el grupo del producto no tiene `planCuenta` | ✅ | — | — | ✗ |
| `CODIGOS_RETENCION_SIN_CUENTA` — un `codigoRetencion` del XML no tiene cuenta en `PGS.TSRI` | — | ✅ | ✅ | — |
| `FACTURA_VENTA_NO_ENCONTRADA` — no se resuelve la factura de venta del documento sustento | — | ✅ | ✅ | — |
| `RETENCION_MULTIDOCUMENTO` — el XML trae más de un `numDocSustento` distinto | — | ✅ | — | — |

### Resolución del documento sustento (factura de venta)

La retención recibida abona una factura de **venta**, así que esa factura debe
existir antes de registrar la retención. El bloqueante llama al **mismo**
resolutor que después usará la aplicación de pago del Paso 4:

```java
aplicacionPagoCxcService.resolverFacturaPorNumero(numDocSustento, null, idEmpresa)
    → AplicacionPagoCxcDaoService.selectFacturaByNumero(...)
    → " where FUNCTION('replace', f.numero, '-', '') = :numero "   // numero también sin guiones
```

**El número se compara SIN GUIONES en ambos lados.** El SRI manda
`001001000000784` y en `CBR.FCTR` suele estar como `001-001-000000784`; antes el
Paso 2d hacía su propio `COUNT` con `f.numero = :val` tal cual, así que reportaba
"documento sustento no encontrado" aunque la factura sí existiera. Reutilizar el
resolutor garantiza que el bloqueante y la aplicación de pago no puedan discrepar.

Detalles del comportamiento:

- Se valida **solo si `Facturador.generaConta = 1`**. Con `generaConta = 0` no se
  genera asiento ni aplicación de pago, así que la factura de venta no hace falta.
- Si el XML trae varios `<retencion>` con `numDocSustento` distintos, se corta con
  `RETENCION_MULTIDOCUMENTO`: el Paso 4 (`obtenerNumeroDocSustento`) solo soporta
  un documento sustento por retención. La comparación es del valor tal cual, sin
  normalizar, porque así los distingue el `select distinct d.numDocReten` del
  Paso 4.
- `numAutDocSustento` ya no se usa para buscar la factura (solo aparece en el
  mensaje de error): la búsqueda por autorización podía encontrar la factura pero
  el Paso 4 resuelve por número, así que validar por autorización daba un OK
  falso.
- ⚠️ `selectFacturaByNumero` filtra por empresa pero **no** por `f.estado`, así
  que una factura anulada también resuelve. Es el comportamiento del resolutor
  de CXC (y del equivalente de CXP), no algo propio de este proceso.

**NC y ND sí abortan si falta la factura de compra afectada**, pero no como
bloqueante estructurado: `registrarNotaCreditoCompra` /
`registrarNotaDebitoCompra` llaman a
`AplicacionPagoCxpService.resolverFacturaCompraPorNumero(numDocModificado, idTitular, idEmpresa)`
**antes de grabar**, y ese método lanza excepción si no hay coincidencia o si hay
más de una. El resultado para el frontend es un `500` con mensaje de texto (y el
documento en estado `ERROR`), no un `422` con la lista `bloqueantes`.

**Cuenta del código de retención (`PGS.TSRI`):** el `lsri.tabla` depende del
impuesto del XML — `codigo=1` (Renta) → `608`; `codigo=2` (IVA) → `20`.
Cualquier otro valor se reporta como *"Tipo de impuesto desconocido"*.

**HTTP 422 con bloqueantes:**
```json
{
  "pendienteClasificacion": true,
  "bloqueantes": [
    { "tipo": "PRODUCTOS_SIN_CLASIFICAR", "detalle": "...", "productos": ["SERVICIOS PROFESIONALES"] },
    { "tipo": "PROVEEDOR_SIN_CUENTA", "detalle": "..." }
  ],
  "productosPendientes": ["SERVICIOS PROFESIONALES"],
  "mensaje": "No se puede registrar la factura. Hay 2 condición(es) bloqueante(s)..."
}
```

> **Corregido el 2026-08-13.** Antes, `registrarRetencionCompraV2` trataba el
> documento sustento no encontrado como advertencia (`advertenciaDocSustento`) y
> seguía adelante, pero el Paso 4 lanzaba excepción al no resolver la factura →
> rollback completo y documento en `ERROR` con un mensaje poco claro. Además la
> comparación del número no ignoraba los guiones, así que la advertencia salía
> incluso cuando la factura existía. Ahora es bloqueante y usa el mismo resolutor
> que el Paso 4. La clave `advertenciaDocSustento` de la respuesta **ya no
> existe** (aplicaba a V1 y V2).

### Paso 4 — Registro en BD, asiento y aplicación de pago (solo si todo OK)

Todo dentro de la misma transacción:

1. Graba cabecera + detalles (+ formas de pago + path según el tipo)
2. Actualiza `DocumentoCxp`: `idDocumentoBD`, `tipoTablaDestino`,
   `fechaRegistroBD`, `usuarioRegistroBD`, estado=`3`, `observacion=null`
3. Genera el asiento contable (`generarAsientoCxp`), **solo si el `Facturador`
   de la empresa tiene `generaConta = 1`**; si no, se omite el asiento en
   silencio y con él la aplicación de pago
4. Graba la FK `ASIENTO` de vuelta en la tabla del documento
   (`grabarAsientoEnDocumento`) — si esto falla, no revierte: devuelve
   `advertenciaAsientoFK`
5. Registra la aplicación de pago (`registrarAplicacionPagoCxp`, §7) — si esto
   falla **sí** revierte todo

**HTTP 200 éxito:**
```json
{
  "valido": true,
  "idDocumentoBD": 11,
  "tipoTablaDestino": "FACTURA_COMPRA",
  "mensaje": "FacturaCompra registrada correctamente con id=11.",
  "productosPendientes": [],
  "pendienteClasificacion": false,
  "asiento": "CXP-2026-07-0002",
  "aplicacionPago": "Nota de crédito aplicada a la factura afectada."
}
```

---

## 6. Generación del asiento contable

**Prerequisito:** `Facturador.generaConta = 1` para la empresa.

| `tipoTablaDestino` | Método de `AsientoContableService` | Constante `TipoAsientos` | `codigoAlterno` |
|---|---|---|---|
| `FACTURA_COMPRA` | `generarAsientoFacturaCompra` | `FACTURAS_COMPRA` | 3 |
| `NOTA_CREDITO_COMPRA` | `generarAsientoNotaCreditoCompra` | `NOTAS_CREDITO_COMPRA` | 10 ⚠️ TODO verificar en BD |
| `NOTA_DEBITO_COMPRA` | `generarAsientoNotaDebitoCompra` | `NOTAS_DEBITO_COMPRA` | 11 ⚠️ TODO verificar en BD |
| `LIQUIDACION_COMPRA_COMPRA` | `generarAsientoLiquidacionCompraCompra` | `LIQUIDACIONES_COMPRA_RECIBIDAS` | 12 ⚠️ TODO verificar en BD |
| `RETENCION_COMPRA` | `generarAsientoRetencionCompra` | `RETENCIONES_RECIBIDAS` | 3 |
| `RETENCION_COMPRA_V2` | `generarAsientoRetencionCompraV2` | `RETENCIONES_RECIBIDAS_V2` | 3 |

> ⚠️ `FACTURAS_COMPRA`, `RETENCIONES_RECIBIDAS` y `RETENCIONES_RECIBIDAS_V2`
> valen **todas 3**, así que hoy los tres tipos de documento se contabilizan con
> el mismo `TipoAsiento`. Está pendiente definir los `codigoAlterno` propios en
> `CNT.TPAS`.

### Fecha contable

Siempre la **fecha de emisión del documento** (`obtenerFechaDocumento`, leída de
la tabla destino, que a su vez viene del `<fechaEmision>` del XML). Respaldo:
`DocumentoCxp.fechaEmision` (del TXT). Si no hay ninguna de las dos, **no se
genera el asiento y se lanza excepción** — antes caía a la fecha de hoy en
silencio y contabilizaba en el período equivocado.

### Estructura — Factura de Compra

| Lado | Cuenta | Valor |
|---|---|---|
| **DEBE** | `GrupoProductoPago.planCuenta` | Suma de `subTotal` por grupo — una línea por grupo distinto |
| **DEBE** | IVA crédito tributario (`PGS.TSRI` donde `lsri.tabla='17'` y `codigo=codigoIVASRI`) | Suma de `valorIVA` por código IVA |
| **HABER** | Cuenta CxP proveedor (`PersonaCuentaContable`, `tipoCuenta=1`) | `factura.total` |

### Estructura — Retención recibida (V1 y V2)

| Lado | Cuenta | Valor |
|---|---|---|
| **DEBE** | Cuenta de retención recibida por código SRI (`PGS.TSRI`) | `valorReten` de cada detalle — una línea por detalle |
| **HABER** | Cuenta CxP del proveedor | Total retenido |

### Observación del asiento

```
{tipo}: {serie} | {Cliente|Proveedor}: {razón social emisor} [| Factura: {número}]
```

Para **retenciones recibidas** la contraparte se rotula **`Cliente:`**, no
`Proveedor:`: el documento entra por la carga de CXP, pero quien nos retuvo es el
cliente de una factura de **venta**. Además se agrega `| Factura: {número}` con
la factura de venta afectada, para poder rastrear el asiento hasta ella:

```
Retención compra V2: 001-001-000000123 | Cliente: COOPERATIVA DE AHORRO Y CREDITO CREDIMAS | Factura: 001-001-000000784
```

El número lo resuelve `obtenerFacturaAfectadaRetencion(tipo, idDocBD, idEmpresa)`:
lee el `numDocReten` de los detalles y lo pasa por
`AplicacionPagoCxcService.resolverFacturaPorNumero`, así que se muestra **con
guiones**, como está en `CBR.FCTR` (el sustento del XML llega sin ellos). Si no se
puede resolver, cae al número crudo del XML y, si tampoco hay sustento, el
segmento se omite — la observación es informativa y nunca hace fallar el asiento.
Con varios sustentos distintos (solo datos históricos: el bloqueante
`RETENCION_MULTIDOCUMENTO` lo impide) se listan los números separados por coma.

Los demás tipos conservan `Proveedor:`. `ASNT.ASNTOBSR` admite 2000 caracteres.

### Lectura del IVA desde el XML del SRI

```xml
<impuesto>
  <codigo>2</codigo>               <!-- 2=IVA. Solo se procesa si codigo=2 -->
  <codigoPorcentaje>4</codigoPorcentaje>  <!-- este es el código a buscar en PGS.TSRI -->
  <tarifa>15</tarifa>
  <valor>375.00</valor>
</impuesto>
```

- Solo se genera línea de IVA si `<codigo> = "2"`
- El campo `DetalleFacturaCompra.codigoIVASRI` guarda el valor de `<codigoPorcentaje>`
- Búsqueda en `PGS.TSRI`: `WHERE lsri.tabla = '17' AND codigo = :codigoIVASRI AND estado = 1`

### Reglas del asiento

- Si `DEBE ≠ HABER` → **excepción + rollback completo** (ni documento ni asiento quedan en BD) → HTTP 422
- Si el `TipoAsiento` no existe → detectado en validaciones bloqueantes (Paso 3) para Factura y Retención; para NC/ND/Liquidación revienta recién en el asiento
- Si el método del asiento aún es un stub (`UnsupportedOperationException`) → **no bloquea**: se devuelve `advertenciaAsiento` y el documento queda registrado sin asiento

---

## 7. Aplicación de pago automática

Al registrar el documento, en la **misma transacción del asiento**, se registra
el abono o cargo que ese documento produce sobre la factura afectada
(`registrarAplicacionPagoCxp`). Si la aplicación falla, se revierte todo.

| `tipoTablaDestino` | Servicio | Tabla | Factura afectada | Efecto |
|---|---|---|---|---|
| `NOTA_CREDITO_COMPRA` | `AplicacionPagoCxpService.aplicarNotaCredito` | `PGS.APLP` | Factura de **compra** (por `numDocModificado`) | Abona (monto positivo) |
| `NOTA_DEBITO_COMPRA` | `AplicacionPagoCxpService.aplicarNotaDebito` | `PGS.APLP` | Factura de **compra** | Aumenta el saldo (monto **negativo**) |
| `RETENCION_COMPRA` | `AplicacionPagoCxcService.aplicarRetencionRecibida` | `CBR.APLC` | Factura de **venta** (por el `numDocSustento` del detalle) | Abona |
| `RETENCION_COMPRA_V2` | `AplicacionPagoCxcService.aplicarRetencionRecibidaV2` | `CBR.APLC` | Factura de **venta** | Abona |
| `FACTURA_COMPRA` / `LIQUIDACION_COMPRA_COMPRA` | — | — | — | No genera aplicación (son el documento que se paga) |

`AplicacionPagoCxp.tipoDocPago` (`TipoDocPagoAplicacion`):
`1`=Pago directo · `2`=Nota de Crédito · `3`=Retención · `4`=Anticipo ·
`5`=Nota de Débito (monto negativo).
Formas de pago directo (`formaPago`, solo con `tipoDocPago=1`):
`1`=Efectivo · `2`=Transferencia · `3`=Cheque · `4`=Débito automático.

Cada vez que se crea o reversa una aplicación se recalcula y graba
`FacturaCompra.estadoPago` (`EstadoPagoFactura`: `1`=Pendiente,
`2`=Pagada parcial, `3`=Pagada total).

### Endpoints de aplicaciones — CXP (`/rest/aplp`)

| Método | Path | Uso |
|---|---|---|
| GET | `/aplp/getAll` | Todas las aplicaciones |
| GET | `/aplp/getId/{id}` | Una aplicación |
| GET | `/aplp/factura/{idFactura}?soloActivas=true` | Historial de una factura de compra |
| GET | `/aplp/saldo/{idFactura}` | `total`, `totalAplicado`, `saldoPendiente`, `estadoPago` |
| POST | `/aplp/anticipo` | Cruza saldo de anticipos del proveedor contra la factura |
| POST | `/aplp/revertir/{id}` | Reversa una aplicación (body: `motivo`, `idUsuario`) |
| POST | `/aplp/selectByCriteria` | Búsqueda por criterios |

Body de `/aplp/anticipo`:
```json
{
  "idFacturaCompra": 123,
  "valor": 225.00,
  "fechaAplicacion": "2026-08-07",
  "idEmpresa": 1236,
  "idUsuario": 5,
  "observacion": "Cruce parcial"
}
```

El cruce de anticipos es **por valor** contra el saldo global del proveedor
(`PersonaCuentaContable` con `tipoCuenta=2`): no se seleccionan anticipos
individuales, y la FK a `PGS.ANTP` queda nula.

Además, `AplicacionPagoCxpService.aplicarPagoTransferencia(pago, idUsuario)` se
invoca desde el flujo de pagos programados (transferencia confirmada por el
banco y débito automático `PGTRDBAT=1`); genera el asiento y el movimiento
bancario.

### Endpoints de aplicaciones — CXC (`/rest/aplc`)

Mismo juego para facturas de venta, más `POST /aplc/cobroTransferencia`.

---

## 8. Almacenamiento del XML

Usa `FileService` centralizado. Directorio base (por prioridad):

1. Propiedad: `-Dsaa.upload.dir=...`
2. Variable de entorno: `SAA_UPLOAD_DIR`
3. Default Linux: `/opt/saa-uploads/`
4. Default Windows: `{userHome}/saa-uploads/`

Subdirectorio: `docs/xml/cxp/`  
Nombre: `{claveAcceso}.xml`  
Extensión `.xml` está incluida en `FileService.EXTENSIONES_PERMITIDAS`.

---

## 9. Reversión

**Endpoint:** `POST /rest/carga-documentos/revertir/{idDocumentoCxp}`  
Solo aplica a documentos en estado `3 (REGISTRADO_BD)`.

**Pasos (`revertirRegistrosBD`):**

1. **Eliminar las aplicaciones de pago** que generó el documento — antes de
   borrar cualquier fila, para no chocar con las FK:
   - `NOTA_CREDITO_COMPRA` → `AplicacionPagoCxpService.eliminarAplicacionesDeDocumento("NOTA_CREDITO", id)`
   - `NOTA_DEBITO_COMPRA` → ídem con `"NOTA_DEBITO"`
   - `RETENCION_COMPRA` → `AplicacionPagoCxcService.eliminarAplicacionesDeDocumento("RETENCION", id)`
   - `RETENCION_COMPRA_V2` → ídem con `"RETENCION_V2"`
   - `FACTURA_COMPRA` → **no se borra nada**: si la factura tiene aplicaciones
     activas se lanza `IncomeException` y hay que reversar primero esos pagos
2. Anular el asiento vinculado → estado `ANULADO (2)` en `CNT.ASNT`
   (`anularAsientoDeDocumento`; si falla solo advierte, no aborta)
3. Eliminar los registros CXP (detalles → paths → cabecera).
   En V2: `DetalleRetencionCompraV2` (FK `DRC2.RETENCIONV2`) y luego
   `RetencionCompraV2`. No hay `PathRetencionCompraV2`: la ruta del XML queda en
   `DocumentoCxp.pathXml`
4. `DocumentoCxp.estadoDocumento = 6 (REVERTIDO)`

---

## 10. Endpoints REST del proceso

Application path JAX-RS: `/rest` · Base: `/SaaBE/rest/carga-documentos`

| Método | Path | HTTP éxito | HTTP error |
|---|---|---|---|
| POST | `/cargarTxt` | 201 | 500 |
| POST | `/procesarXml/{id}` | 200 | 422 / 400 / 404 / 500 |
| POST | `/cargarXml/{id}` *(legacy)* | 200 | 422 |
| POST | `/registrarBD/{id}` *(legacy — exige estado 2)* | 200 | 422 |
| POST | `/crearProductosYRegistrar/{id}` | 200 | 422 / 500 |
| POST | `/resolverNovedad/{id}` | 200 | 500 |
| POST | `/revertir/{id}` | 200 | 500 |
| GET | `/resumen/{idCargaTxt}` | 200 | 500 |
| GET | `/documento/{id}` | 200 | 404/500 |
| GET | `/novedades/{idEmpresa}` | 200 | 500 |
| GET | `/productosPendientes/{idFacturaCompra}` | 200 | 500 |
| GET | `/gruposProducto` | 200 | 500 |

---

## 11. Pendientes

| Tema | Pendiente |
|---|---|
| Marcado de `ERROR` que no persiste | `cargarXmlYRegistrar` y `registrarDocumentoBD` hacen `doc.setEstadoDocumento(ERROR)` + `save` en el `catch` y luego re-lanzan. El bean es `@Stateless` sin `@TransactionAttribute`, así que la excepción rueda atrás **toda** la transacción, incluido ese marcado: el documento **nunca queda en estado 4** ni guarda la `observacion` del error. Se necesita un método aparte con `@TransactionAttribute(REQUIRES_NEW)` para estampar el error |
| Retención V2 · doc sustento | ✅ Resuelto el 2026-08-13: `FACTURA_VENTA_NO_ENCONTRADA` es bloqueante y el número se compara sin guiones (§5) |
| Retención · total | ✅ Resuelto el 2026-08-13: el total sale de la suma de `<valorRetenido>`, no de `importeTotal` (§3) |
| Retención · multidocumento | Soportar retenciones con varios documentos sustento: hoy se bloquean con `RETENCION_MULTIDOCUMENTO` porque la aplicación de pago solo sabe abonar a una factura |
| Retención V2 · path | Falta la entidad `PathRetencionCompraV2`; el path solo queda en `DocumentoCxp.pathXml` |
| `TipoAsientos` | Definir `codigoAlterno` propios en `CNT.TPAS`: hoy Factura de compra y las dos retenciones comparten el `3`, y NC/ND/Liquidación (`10/11/12`) están marcados como *TODO verificar en BD* |
| `Nota de Crédito` / `Nota de Débito` | Agregar validaciones bloqueantes estructuradas (cuenta proveedor, tipo asiento, cuenta del grupo) y convertir el fallo de `resolverFacturaCompraPorNumero` en un `422` con `bloqueantes` en vez de un `500` |
| `Liquidación de Compra` | Ídem, más la aplicación de pago sobre el documento afectado |
| `RetencionCompra` (V1) | Código muerto: `registrarRetencionCompra` ya no se invoca. Decidir si se elimina tras migrar los históricos |
| Rubros | Script SQL para insertar rubro 174 con los códigos 6 y 7 en `SCP.PDTR` |

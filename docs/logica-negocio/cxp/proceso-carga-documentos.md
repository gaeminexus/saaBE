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

**Todos los tipos validan ya con bloqueantes estructurados.** NC, ND y
Liquidación se sumaron el 2026-08-23 (§9 defecto 3 del plan de carga
automática): antes reventaban con una excepción y el frontend recibía un `500`
con texto plano, inservible dentro de un lote de 50 documentos.

| Bloqueante | Factura | Retención V2 | Retención V1 *(muerto)* | NC | ND | Liquidación |
|---|---|---|---|---|---|---|
| `PROVEEDOR_SIN_CUENTA` — proveedor sin cuenta contable CxP (`PersonaCuentaContable`, `tipoCuenta=1`) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `TIPO_ASIENTO_NO_CONFIGURADO` — no existe `TipoAsiento` con el `codigoAlterno` del tipo, para la empresa | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `PRODUCTOS_SIN_CLASIFICAR` — algún producto está en grupo POR CLASIFICAR | ✅ | — | — | — | — | — |
| `GRUPOS_SIN_CUENTA_CONTABLE` — el grupo del producto no tiene `planCuenta` | ✅ | — | — | — | — | — |
| `CODIGOS_RETENCION_SIN_CUENTA` — un `codigoRetencion` del XML no tiene cuenta en `PGS.TSRI` | — | ✅ | ✅ | — | — | — |
| `FACTURA_VENTA_NO_ENCONTRADA` — no se resuelve la factura de venta del documento sustento | — | ✅ | ✅ | — | — | — |
| `RETENCION_MULTIDOCUMENTO` — el XML trae más de un `numDocSustento` distinto | — | ✅ | — | — | — | — |
| `FACTURA_COMPRA_NO_ENCONTRADA` — no se resuelve la factura de compra que el documento modifica | — | — | — | ✅ | ✅ | — |

NC, ND y Liquidación no llevan `PRODUCTOS_SIN_CLASIFICAR` ni
`GRUPOS_SIN_CUENTA_CONTABLE` porque su detalle no pasa por `ProductoPago`: se
graba como descripción libre en `DNCC` / `DNDC` / `DLCC`.

Los dos bloqueantes comunes viven en un solo sitio,
`agregarBloqueantesComunesCompra(...)`, para que los tres tipos no puedan
divergir. El `codigoAlterno` que se exige es el mismo que después usará
`generarAsientoCxp`: `NOTAS_CREDITO_COMPRA`, `NOTAS_DEBITO_COMPRA` y
`LIQUIDACIONES_COMPRA_RECIBIDAS`, que desde el 2026-08-23 valen **3** — el de la
factura de compra (§6). Con el 3 configurado en `CNT.TPAS`, que es el caso en
ASOPREP, este bloqueante no se dispara nunca; solo protege a una empresa a la que
le falte la plantilla.

### Resolución del documento sustento (factura de venta)

La retención recibida abona una factura de **venta**, así que esa factura debe
existir antes de registrar la retención. El bloqueante usa la **misma consulta**
que después usará la aplicación de pago del Paso 4:

```java
aplicacionPagoCxcDaoService.selectFacturaByNumero(numDocSustento, null, idEmpresa)
    → " where FUNCTION('replace', f.numero, '-', '') = :numero "   // numero también sin guiones
```

**El número se compara SIN GUIONES en ambos lados.** El SRI manda
`001001000000784` y en `CBR.FCTR` suele estar como `001-001-000000784`; antes el
Paso 2d hacía su propio `COUNT` con `f.numero = :val` tal cual, así que reportaba
"documento sustento no encontrado" aunque la factura sí existiera. Compartir la
consulta garantiza que el bloqueante y la aplicación de pago no puedan discrepar.

#### ⚠️ Corregido el 2026-08-23: entre el 13 y el 23 de agosto este bloqueante no funcionó

La corrección del **2026-08-13** hizo que el Paso 2d llamara a
`AplicacionPagoCxcService.resolverFacturaPorNumero(...)`, el resolutor **a nivel
de servicio**, dentro de un `try/catch` que armaba el mapa de bloqueantes. Nunca
surtió efecto, y el motivo es de EJB, no de negocio:

1. `resolverFacturaPorNumero` comunica el fallo lanzando `IncomeException`.
2. `IncomeException` está anotada `@ApplicationException(rollback = true)`
   (`basico/util/IncomeException.java:16`).
3. `AplicacionPagoCxcService` se invoca por `@EJB` y es `REQUIRED`, así que corre
   **en la transacción del registro**. Al cruzar la frontera del EJB, el
   contenedor marca esa transacción para rollback **antes** de entregar la
   excepción — y **atraparla no la desmarca**.
4. El método retornaba su `422` con `bloqueantes`, el contenedor encontraba la
   marca al hacer commit, lanzaba `EJBTransactionRolledbackException`, y el REST
   devolvía un **500 opaco**.

O sea: el síntoma que la corrección venía a resolver —"documento sustento no
encontrado que revienta con un 500 poco claro"— siguió igual diez días, solo que
por otro motivo. Se arregló llamando al **DAO**, que devuelve lista y no lanza.

> **Regla general (§11 decisión 18 del plan de carga automática).** Dentro de una
> transacción que debe sobrevivir, **nunca invoques otro EJB que comunique el
> fallo con `IncomeException` si piensas atrapar el error y continuar**. Usa el
> DAO, o un método que devuelva vacío en lugar de lanzar. Atrapar no basta. Vale
> igual para cualquier excepción de sistema (`RuntimeException`) que cruce una
> frontera de EJB: esas también marcan la transacción para rollback.

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

### Resolución de la factura de compra afectada (NC y ND)

Desde el 2026-08-23 esto **sí** es un bloqueante estructurado,
`FACTURA_COMPRA_NO_ENCONTRADA`, con las mismas tres condiciones que aplicaba el
resolutor: sin `numDocModificado`, sin coincidencia, o más de una.

Se resuelve con `AplicacionPagoCxpDaoService.selectFacturaByNumero(...)`, que es
**la consulta que usa después la aplicación de pago**, así que el bloqueante y el
Paso 4 no pueden discrepar. Se llama al DAO y no a
`AplicacionPagoCxpService.resolverFacturaCompraPorNumero(...)` por una razón
concreta: ese comunica el fallo con `IncomeException`, anotada
`@ApplicationException(rollback = true)`. Como el servicio es `REQUIRED` y se une
a la transacción del registro, atrapar esa excepción para devolver un bloqueante
dejaría la transacción marcada para rollback, y con ella se irían el retorno
estructurado, la observación que graba `registrarDocumentoBD` y el proveedor
recién autocreado.

Las retenciones tenían el mismo problema y se corrigieron igual el 2026-08-23
— ver el recuadro de *Resolución del documento sustento*, más abajo.

⚠️ **Queda un caso vivo del mismo patrón**: `TIPO_ASIENTO_NO_CONFIGURADO`.
`TipoAsientoService.codigoByAlterno` **lanza `IncomeException`** cuando no
encuentra la plantilla (`TipoAsientoServiceImpl:120`), nunca devuelve `null`, así
que el `if (idTipoAsiento == null)` de todos los bloqueantes es código muerto y
el que se ejecuta es el `catch` — con la transacción ya condenada. Afecta a
Factura, a las dos Retenciones y a NC/ND/Liquidación. Pendiente de decisión.

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
| `NOTA_CREDITO_COMPRA` | `generarAsientoNotaCreditoCompra` | `NOTAS_CREDITO_COMPRA` | 3 |
| `NOTA_DEBITO_COMPRA` | `generarAsientoNotaDebitoCompra` | `NOTAS_DEBITO_COMPRA` | 3 |
| `LIQUIDACION_COMPRA_COMPRA` | `generarAsientoLiquidacionCompraCompra` | `LIQUIDACIONES_COMPRA_RECIBIDAS` | 3 |
| `RETENCION_COMPRA` | `generarAsientoRetencionCompra` | `RETENCIONES_RECIBIDAS` | 3 |
| `RETENCION_COMPRA_V2` | `generarAsientoRetencionCompraV2` | `RETENCIONES_RECIBIDAS_V2` | 3 |

> **Los seis valen 3, y es deliberado.** Decisión del usuario del 2026-08-23:
> todos los comprobantes de la carga CXP se contabilizan con el **mismo tipo de
> asiento que la factura de compra**, tal como ya hacían las dos retenciones. No
> se crean tipos 10/11/12 y no queda nada pendiente de definir en `CNT.TPAS`
> para este flujo. Lo que distingue el asiento de cada tipo no es la plantilla
> sino el método de `AsientoContableService` que lo arma.
>
> Antes de esta decisión, NC, ND y Liquidación apuntaban a `10`, `11` y `12`, que
> **no existen** en `CNT.TPAS`: los `codigoAlterno` configurados en ASOPREP son
> 0, 1, 2, 3, 4, 5 y 6. No es casualidad que `NTCC`, `NTDC` y `LQCC` tengan cero
> filas mientras `FCTC` tiene 134 y `RCV2` tiene 8 — los únicos tipos que nunca
> llegaron a registrarse eran justo los que no tenían tipo de asiento.

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
| **DEBE** | IVA crédito tributario (`PGS.TSRI` donde `lsri.tabla='17'` y `codigo=codigoIVASRI`) | **`FacturaCompra.vIVA` (IVA de la cabecera del XML)**, repartido entre los códigos IVA de los detalles |
| **HABER** | Cuenta CxP proveedor (`PersonaCuentaContable`, `tipoCuenta=1`) | Suma de las líneas DEBE |

### Estructura — Nota de Crédito de Compra (inverso de la factura)

| Lado | Cuenta | Valor |
|---|---|---|
| **DEBE** | Cuenta CxP proveedor | Suma de las líneas HABER |
| **HABER** | Cuenta de gasto default CXP (la NC no trae `GrupoProductoPago`) | Suma de `subTotal` de los detalles |
| **HABER** | IVA crédito tributario (reverso) | **`NotaCreditoCompra.vIVA` (IVA de la cabecera del XML)**, repartido entre los códigos IVA de los detalles |

### Estructura — Nota de Débito de Compra

| Lado | Cuenta | Valor |
|---|---|---|
| **DEBE** | Cuenta de gasto default CXP | `total − vIVA` |
| **DEBE** | IVA crédito tributario (código SRI deducido de `pIVA`) | **`NotaDebitoCompra.vIVA` (IVA de la cabecera del XML)** — solo si `> 0` |
| **HABER** | Cuenta CxP proveedor | `total` |

Los `<motivo>` de la ND vienen **sin impuesto**, así que la cabecera es la única
fuente del IVA. Antes el total entraba completo a la cuenta de gasto y el
crédito tributario se perdía.

### Estructura — Liquidación de Compra

| Lado | Cuenta | Valor |
|---|---|---|
| **DEBE** | Cuenta de gasto default CXP (la liquidación no trae `GrupoProductoPago`) | Suma de `subTotal` de los detalles |
| **DEBE** | IVA crédito tributario | **`LiquidacionCompraCompra.vIVA` (IVA de la cabecera del XML)**, repartido entre los códigos IVA de los detalles |
| **HABER** | Cuenta CxP prestador | Suma de las líneas DEBE |

### Estructura — Retención recibida (V1 y V2)

| Lado | Cuenta | Valor |
|---|---|---|
| **DEBE** | Cuenta de retención recibida por código SRI (`PGS.TSRI`) | `valorReten` de cada detalle — una línea por detalle |
| **HABER** | Cuenta CxP del proveedor | Total retenido |

La retención no lleva IVA, así que no le aplica la regla de la cabecera.

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

#### El VALOR del IVA sale de la cabecera, no de los detalles

Aplica a **factura, NC, ND y liquidación** de compra. El asiento contabiliza el
IVA declarado en la cabecera del XML, que es lo que el SRI autorizó y lo que
suma el total que se le paga al proveedor. Los detalles solo definen **qué
cuentas** intervienen. Cuando el emisor redondea el impuesto línea por línea, la
sumatoria de los detalles difiere en centavos de la cabecera y la CxP del asiento
no cuadraba con el total del documento.

**Al registrar** — `ProcesoCargaDocumentosServiceImpl.leerIvaCabecera(xmlDoc, tag)`
llena `vIVA` / `pIVA` de la cabecera en los cuatro documentos:

| Documento | Bloque de impuesto en la cabecera |
|---|---|
| Factura, NC, Liquidación | `<totalConImpuestos><totalImpuesto>` |
| Nota de Débito | `<impuestos><impuesto>` |

- `vIVA` = **suma de todos** los bloques con `<codigo>2</codigo>` — el XML trae
  uno por tarifa y antes se leía solo el primero. ICE (`3`) e IRBPNR (`5`) se
  excluyen; si el bloque no trae `<codigo>` (esquemas antiguos) se asume IVA.
- `pIVA` = tarifa del bloque de mayor valor. Los esquemas del SRI anteriores a
  la 1.1.0 no llevan `<tarifa>` en la cabecera, así que se deduce del
  `<codigoPorcentaje>` (`tarifaDesdeCodigoPorcentaje`: `4`→15, `2`→12, `3`→14,
  `5`→5, `8`→8, `0`/`6`/`7`→0).

**Al contabilizar** — `AsientoContableServiceImpl.distribuirIvaCabecera(...)`,
compartido por los cuatro asientos:

| Caso | Qué se registra |
|---|---|
| `vIVA` nulo (documento previo al cambio, o XML sin bloque de IVA) | Fallback: la sumatoria de los detalles |
| `vIVA = 0.00` | **Ninguna** línea de IVA, aunque los detalles traigan valor |
| Una sola tarifa en los detalles | Todo el `vIVA` a esa cuenta |
| Varias tarifas | Prorrateo del `vIVA` proporcional al IVA de cada código; el residuo del redondeo se carga al código de mayor valor |
| `vIVA > 0` y ningún detalle con IVA | Se deduce el código SRI de `pIVA` (`mapPorcentajeIVAaCodigo`) |

La ND no pasa por `distribuirIvaCabecera` porque sus `<motivo>` no traen
impuesto: usa `vIVA` directo y solo separa el IVA si `0 < vIVA < total`.

La cuenta se busca por código SRI: la factura usa `codigoIVASRI` del detalle
(`obtenerCuentaIVACxpPorCodigo`); NC, ND y liquidación derivan el código de la
tarifa con `mapPorcentajeIVAaCodigo` (`obtenerCuentaIVACxp`, con fallback a la
cuenta de IVA de CXC).

> **Config nueva para la ND.** Una ND con IVA ahora exige la cuenta de IVA
> crédito tributario configurada en `PGS.TSRI`; antes no la necesitaba porque
> mandaba todo a gasto. Si falta, el asiento falla con mensaje explícito.

#### El total de NC y ND no está en `importeTotal`

`<importeTotal>` es un tag del esquema de **factura**. Se leía en los cuatro
cargadores, así que NC y ND quedaban con `total = 0.00` y su asiento salía en
cero. El tag correcto por comprobante:

| Documento | Tag del total |
|---|---|
| Factura, Liquidación | `<importeTotal>` |
| Nota de Crédito | `<valorModificacion>` |
| Nota de Débito | `<valorTotal>` |

Se conserva `<importeTotal>` como respaldo por si algún emisor lo incluye.

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
| POST | `/marcarReembolso/{idDocumentoCxp}` | 200 | 422 / 500 |
| POST | `/contabilizarReembolso/{idFacturaCompra}` | 200 | 422 / 500 |
| POST | `/recalcularTotalesReembolso/{idFacturaCompra}` | 200 | 500 |
| POST | `/crearProductoPorClasificar` | 200 | 500 |

Y para el CRUD de la tabla nueva `PGS.RMBF` (`/rest/rmbf`):

| Método | Path | HTTP éxito |
|---|---|---|
| GET | `/rmbf/getAll` | 200 |
| GET | `/rmbf/getId/{id}` | 200 |
| GET | `/rmbf/getByFactura/{idFactura}` | 200 (solo activos, ordenados por id) |
| POST | `/rmbf` | 201 |
| PUT | `/rmbf` | 200 |
| DELETE | `/rmbf/{id}` | 200 |
| POST | `/rmbf/selectByCriteria` | 200 |

---

## 12. Facturas de reembolso de gastos (2026-08-19)

> Spec completa: `docs/logica-negocio/cxp/CAMBIO-REEMBOLSO-GASTOS-BACKEND.md`

Los proveedores intermediarios emiten **facturas de reembolso de gastos** (SRI ANEXO 5). Además del detalle normal pueden traer un bloque `<reembolsos>` con N `<reembolsoDetalle>`, uno por documento sustento del gasto del tercero.

### Tabla nueva `PGS.RMBF`

| Columna | Campo Java | Descripción |
|---|---|---|
| `RMBFCDGO` | `id` | PK identity |
| `RMBFFCTC` | `factura` | FK → `PGS.FCTC.ID` |
| `RMBFTIPR` | `tipoIdentificacionProveedor` | Tipo ident. del proveedor del gasto (tabla 6 SRI) |
| `RMBFIDPR` | `identificacionProveedor` | Identificación del proveedor del gasto |
| `RMBFCDDC` | `codDoc` | Tipo de documento sustento (tabla 3 SRI) |
| `RMBFESTB/PTEM/SCNL` | `establecimiento/puntoEmision/secuencial` | Identificación del doc sustento |
| `RMBFFEMS` | `fechaEmision` | Fecha del doc sustento |
| `RMBFNAUT` | `numeroAutorizacion` | Clave de acceso / autorización del sustento |
| `RMBFBSCR/BSGR` | `baseImponibleCero/Gravada` | Bases del sustento |
| `RMBFTRIV/VLIV/VLIC` | `tarifaIva/valorIva/valorIce` | Impuestos del sustento |
| `RMBFTTAL` | `total` | Total del sustento |
| `RMBFPRDC` | `producto` | ID de `PGS.PRDP` (sin FK JPA) para contabilización por grupo |
| `RMBFORGN` | `origen` | `1`=XML `2`=Manual (`OrigenReembolso`) |
| `RMBFESTD` | `estado` | `1`=Activo `0`=Anulado |

DDL: `docs/logica-negocio/cxp/sql/07-reembolso-gastos.sql` (ya ejecutado en BD).

### Campos nuevos en tablas existentes

| Tabla | Columna | Campo Java | Descripción |
|---|---|---|---|
| `PGS.FCTC` | `FCTCESRM` | `esReembolso` | `0`=No `1`=Sí |
| `PGS.FCTC` | `FCTCCDRM` | `codDocReembolso` | código doc (normalmente `41`) |
| `PGS.FCTC` | `FCTCTCRM/TBRM/TIRM` | `totalComprobantes/BaseImponible/ImpuestoReembolso` | totales recalculados desde RMBF |
| `PGS.DCXP` | `DCXPESRM` | `esReembolso` | flag de bandeja (`0`/`1`) |

Ambos campos están en `obtieneCampos()` de sus respectivos `DaoServiceImpl`.

### Detección automática

Al ejecutar `cargarXmlYRegistrar` / `procesarXml`, el servicio detecta reembolso si:
- `doc.getEsReembolso() == 1` (marcado por el usuario previamente), **o**
- el XML contiene `<reembolsoDetalle>`, **o**
- `<infoFactura>` tiene `<codDocReembolso>` no vacío.

> ⚠️ `<codDocReembolso>` también aparece **dentro** de cada `<reembolsoDetalle>`. Se lee **desde `<infoFactura>` específicamente** (buscando el primer hijo del nodo `infoFactura`), nunca con `getXmlValue(xmlDoc, "codDocReembolso")` que devuelve la primera ocurrencia del documento completo.

### Productos de los sustentos

Por cada `<reembolsoDetalle>`, el servicio busca un `ProductoPago` con `codigo = identificacionProveedorReembolso`. Si no existe, lo crea con `nombre = "REEMBOLSO {identificacion}"` en el grupo **POR CLASIFICAR**. Este mecanismo reutiliza `obtenerOAutoCrearProducto`.

**Regla de bloqueantes cuando `esReembolso=1`:**
- Los productos de los **sustentos** (`RMBF`) **sí bloquean** con `PRODUCTOS_SIN_CLASIFICAR`.
- Los productos de los `<detalle>` normales **no bloquean** (no participan del asiento de reembolso).

`obtenerProductosPendientesDeClasificar(idFactura)` también respeta esta regla: si la factura es reembolso, busca pendientes desde `RMBF`, no desde `DFCC`.

### Contabilización

El asiento de una factura de reembolso se genera igual que el de una factura normal, **excepto que las líneas de DEBE salen de los sustentos (`RMBF`), no del detalle (`DFCC`)**:

| Lado | Cuenta | Valor |
|---|---|---|
| **DEBE** | `GrupoProductoPago.planCuenta` del producto de cada sustento | `sum(baseImponibleCero + baseImponibleGravada + valorIce)` por grupo |
| **DEBE** | IVA crédito tributario | `sum(RMBF.valorIva)` |
| **HABER** | Cuenta CxP proveedor | Suma exacta de los DEBE |

Implementado en `AsientoContableServiceImpl.generarAsientoFacturaCompraReembolso` (método privado, invocado desde `generarAsientoFacturaCompra` cuando `fc.esReembolso == 1`).

#### Precondiciones para generar el asiento (validadas en `generarAsientoCxp` y en `contabilizarReembolso`)

1. Existe al menos un `RMBF` activo.
2. Todos los productos de los `RMBF` activos están clasificados (grupo ≠ POR_CLASIFICAR) y su grupo tiene cuenta contable.
3. `|sum(RMBF.total) − factura.total| ≤ 0.01`.

Si alguna falla: **NO se aborta el registro**. La factura y sus `RMBF` quedan grabados, pero el asiento no se genera. `DocumentoCxp` queda en **estado 2** con `observacion` descriptiva y la respuesta lleva `contabilizacionPendiente: true` + `motivoContabilizacionPendiente`.

Excepción: si los productos están en POR_CLASIFICAR, se usa el mecanismo habitual de bloqueantes `422` + `crearProductosYRegistrar`.

**Con `Facturador.generaConta = 0`:** no se genera asiento; la factura pasa a estado 3 directamente.

### Flujo XML bien formado (tiene `<reembolsos>`)

```
procesarXml → detecta esReembolso=true
           → PASO 1: crea productos de sustentos (en POR CLASIFICAR si son nuevos)
           → PASO 2: bloqueantes sobre productos de sustentos (no sobre detalle normal)
           → Si hay pendientes → HTTP 422 con PRODUCTOS_SIN_CLASIFICAR
           → PASO 3: graba FacturaCompra (esReembolso=1) + DFCC + RMBF
           → generarAsientoCxp: verifica precondiciones
             → Si OK → asiento desde RMBF → estado 3
             → Si NO → estado 2, observacion, contabilizacionPendiente=true en respuesta
```

### Flujo XML mal formado (sin `<reembolsos>`, usuario marcó como reembolso)

```
procesarXml → detecta esReembolso=true (por DCXPESRM=1)
           → reembolsosXml.getLength() == 0
           → PASO 3: graba FacturaCompra (esReembolso=1) sin RMBF
           → generarAsientoCxp: sin RMBF activos → estado 2
           → respuesta: reembolsoManualPendiente=true + advertenciaReembolso
```

### Reversión

`revertirRegistrosBD` borra los `RMBF` **antes** del detalle y la cabecera de `FCTC` para no violar la FK `FK_RMBF_FACTURA`:

```java
em.createQuery("delete from ReembolsoFacturaCompra r where r.factura.id = :id")
        .setParameter("id", idDocBD).executeUpdate();
// ...luego DFCC, FMPC, PFCC, FCTC
```

### Endpoints de negocio nuevos

#### `POST /carga-documentos/marcarReembolso/{idDocumentoCxp}`
Body: `{esReembolso: 0|1, idUsuario}`. Marca/desmarca el flag en `DCXP` (y en `FCTC` si ya está registrada). Si hay pagos aplicados → 422. Si al desmarcar hay RMBF activos → 422.

#### `POST /carga-documentos/contabilizarReembolso/{idFacturaCompra}`
Body: `{idEmpresa, idUsuario}`. Valida las 3 precondiciones y genera el asiento desde RMBF. Pasa el `DocumentoCxp` a estado 3. 422 con mensaje descriptivo si falla.

#### `POST /carga-documentos/recalcularTotalesReembolso/{idFacturaCompra}`
Sin body obligatorio. Suma los campos de los RMBF activos, persiste los 3 totales en `FCTC` y devuelve `{cantidadReembolsos, totalComprobantesReembolso, totalBaseImponibleReembolso, totalImpuestoReembolso, importeTotalFactura, diferencia, cuadra}`.

#### `POST /carga-documentos/crearProductoPorClasificar`
Body: `{nombre, codigo?, idEmpresa}`. Si ya existe un producto con ese código lo devuelve sin crear. Útil para el alta manual de sustentos desde la pantalla.

---

## 11. Pendientes

| Tema | Pendiente |
|---|---|
| Marcado de `ERROR` que no persiste | `cargarXmlYRegistrar` y `registrarDocumentoBD` hacen `doc.setEstadoDocumento(ERROR)` + `save` en el `catch` y luego re-lanzan. El bean es `@Stateless` sin `@TransactionAttribute`, así que la excepción rueda atrás **toda** la transacción, incluido ese marcado: el documento **nunca queda en estado 4** ni guarda la `observacion` del error. Se necesita un método aparte con `@TransactionAttribute(REQUIRES_NEW)` para estampar el error |
| Retención V2 · doc sustento | ✅ Resuelto el 2026-08-13: `FACTURA_VENTA_NO_ENCONTRADA` es bloqueante y el número se compara sin guiones (§5) |
| Retención · total | ✅ Resuelto el 2026-08-13: el total sale de la suma de `<valorRetenido>`, no de `importeTotal` (§3) |
| Retención · multidocumento | Soportar retenciones con varios documentos sustento: hoy se bloquean con `RETENCION_MULTIDOCUMENTO` porque la aplicación de pago solo sabe abonar a una factura |
| Retención V2 · path | Falta la entidad `PathRetencionCompraV2`; el path solo queda en `DocumentoCxp.pathXml` |
| `TipoAsientos` | ✅ Cerrado el 2026-08-23: los seis tipos de la carga CXP comparten `codigoAlterno = 3`, el de la factura de compra, por decisión del usuario. No se crean tipos propios (§6) |
| `Nota de Crédito` / `Nota de Débito` | ✅ Resuelto el 2026-08-23: bloqueantes estructurados `PROVEEDOR_SIN_CUENTA`, `TIPO_ASIENTO_NO_CONFIGURADO` y `FACTURA_COMPRA_NO_ENCONTRADA`, con `422` en vez de `500` (§5) |
| `Liquidación de Compra` | ✅ Bloqueantes resueltos el 2026-08-23 (§5). Queda pendiente la aplicación de pago sobre el documento afectado |
| `RetencionCompra` (V1) | Código muerto: `registrarRetencionCompra` ya no se invoca. Decidir si se elimina tras migrar los históricos |
| Rubros | Script SQL para insertar rubro 174 con los códigos 6 y 7 en `SCP.PDTR` |
| Reembolso · marcar al cargar XML | `procesarXml` acepta `esReembolso` en el body pero delega en `marcarReembolso` (que a su vez verifica pagos aplicados). Para el flujo normal (XML subido antes de marcar) esto puede causar un 422 innecesario; evaluar si el flag se debe propagar directamente al documento sin validar pagos en ese punto |
| Reembolso · facturas mixtas | Las facturas con fee del intermediario + reembolso en el mismo documento quedan fuera de alcance. Si aparece un caso real, decidir si el fee va al detalle normal y el reembolso a RMBF, o si se crea un tipo nuevo |

# Proceso de Carga de Documentos CXP

> **Archivo de referencia principal.**  
> Última revisión: 2026-07-27

**Módulo:** CXP - Cuentas por Pagar  
**Stack:** Jakarta EE · WildFly · Oracle DB · Schema PGS

---

## 1. Arquitectura — tres tablas

| Tabla | Entidad Java | Propósito |
|---|---|---|
| `PGS.CRTX` | `CargaArchivoTxt` | Cabecera de cada archivo TXT cargado |
| `PGS.DCXP` | `DocumentoCxp` | **Un solo registro por documento** (por `claveAcceso`). Ciclo de vida completo |
| `PGS.DCTX` | `DetalleCargaTxt` | Una línea por aparición en un TXT. FK a DCXP |

### Archivos Java clave

| Archivo | Paquete | Rol |
|---|---|---|
| `ProcesoCargaDocumentosServiceImpl.java` | `com.saa.ejb.cxp.serviceImpl` | Implementación completa de las fases |
| `ProcesoCargaDocumentosRest.java` | `com.saa.ws.rest.cxp` | Endpoints REST |
| `AsientoContableServiceImpl.java` | `com.saa.ejb.cnt.serviceImpl` | Generación de asientos contables CXP |
| `ResultadoCargaTxt.java` | `com.saa.rubros` | Constantes de resultado por línea (rubro 174) |
| `EstadoDocumentoCxp.java` | `com.saa.rubros` | Constantes de estado del documento (rubro 175) |
| `TipoGrupoProductos.java` | `com.saa.rubros` | BIEN=1, SERVICIO=2, POR_CLASIFICAR=3 |
| `TipoAsientos.java` | `com.saa.rubros` | codigoAlterno de tipos de asiento CXP |

---

## 2. Estados del DocumentoCxp (rubro 175)

| Valor | Nombre | Descripción | Botón frontend |
|---|---|---|---|
| `1` | LEIDO | Leído del TXT, pendiente de XML | "Cargar XML y Registrar" |
| `2` | XML_CARGADO | Transitorio interno — rara vez visible | — |
| `3` | REGISTRADO_BD | Registrado en tablas CXP + asiento contable generado | "Revertir" |
| `4` | ERROR | Falló algún paso. Ver campo `observacion` | "Reintentar" |
| `5` | NOVEDAD | Diferencias detectadas o desaparecido en recarga | "Resolver novedad" |
| `6` | REVERTIDO | BD revertida y asiento anulado | "Cargar XML y Registrar" |

> ⚠️ El estado `2` es transitorio. El endpoint `/procesarXml/{id}` pasa directamente de `1 → 3`.

---

## 3. Tablas destino por tipo de comprobante

| `tipoComprobante` | `tipoTablaDestino` | Tablas que se llenan | Validaciones bloqueantes |
|---|---|---|---|
| `Factura` | `FACTURA_COMPRA` | `FacturaCompra` + `DetalleFacturaCompra` + `FormaPagoFacturaCompra` + `PathFacturaCompra` | ✅ Implementadas |
| `Nota de Crédito` | `NOTA_CREDITO_COMPRA` | `NotaCreditoCompra` + `DetalleNotaCreditoCompra` + `PathNotaCreditoCompra` | ⚠️ Pendiente de pulir |
| `Nota de Débito` | `NOTA_DEBITO_COMPRA` | `NotaDebitoCompra` + `DetalleNotaDebitoCompra` + `PathNotaDebitoCompra` | ⚠️ Pendiente de pulir |
| `Liquidación de compra` | `LIQUIDACION_COMPRA_COMPRA` | `LiquidacionCompraCompra` + `DetalleLiquidacionCompraCompra` + `PathLiquidacionCompraCompra` | ⚠️ Pendiente de pulir |
| `Comprobante de Retención` | `RETENCION_COMPRA` | `RetencionCompra` + `DetalleRetencionCompra` + `PathRetencionCompra` | ⚠️ Pendiente de pulir |
| `Comprobante de Retención electrónica versión 2.0` | `RETENCION_COMPRA_V2` | `RetencionCompraV2` *(sin path aún)* | ⚠️ Pendiente de pulir |

---

## 4. FASE 1 — Carga del TXT

**Endpoint:** `POST /carga-documentos/cargarTxt`

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

**Endpoint recomendado:** `POST /carga-documentos/procesarXml/{idDocumentoCxp}`

**Body:**
```json
{ "contenidoXml": "<?xml ...", "idEmpresa": 1236, "idUsuario": 5 }
```

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

| Tipo | Causa | Solución |
|---|---|---|
| `PROVEEDOR_SIN_CUENTA` | Proveedor sin cuenta contable CxP (`PersonaCuentaContable`, `tipoCuenta=1`) | Contabilidad → Cuentas por Titular |
| `TIPO_ASIENTO_NO_CONFIGURADO` | No existe `TipoAsiento` con `codigoAlterno=3` para la empresa | Contabilidad → Tipos de Asiento |
| `PRODUCTOS_SIN_CLASIFICAR` | Algún producto está en grupo POR CLASIFICAR | Clasificar productos |
| `GRUPOS_SIN_CUENTA_CONTABLE` | El grupo del producto no tiene `planCuenta` | Contabilidad → Grupos de Producto |

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

### Paso 4 — Registro en BD y asiento (solo si todo OK)

1. Graba cabecera + detalles + formas de pago + path
2. Actualiza `DocumentoCxp`: estado=`3`, `observacion=null`
3. Genera asiento contable

**HTTP 200 éxito:**
```json
{
  "valido": true,
  "idDocumentoBD": 11,
  "tipoTablaDestino": "FACTURA_COMPRA",
  "mensaje": "FacturaCompra registrada correctamente con id=11.",
  "productosPendientes": [],
  "pendienteClasificacion": false,
  "asiento": "CXP-2026-07-0002"
}
```

---

## 6. Generación del asiento contable — Factura de Compra

**Prerequisito:** `Facturador.generaConta = 1` para la empresa.  
**TipoAsiento:** `codigoAlterno = 3` (`TipoAsientos.FACTURAS_COMPRA`) en `CNT.TPAS`.

### Estructura del asiento

| Lado | Cuenta | Valor |
|---|---|---|
| **DEBE** | `GrupoProductoPago.planCuenta` | Suma de `subTotal` por grupo — una línea por grupo distinto |
| **DEBE** | IVA crédito tributario (`PGS.TSRI` donde `lsri.tabla='17'` y `codigo=codigoIVASRI`) | Suma de `valorIVA` por código IVA |
| **HABER** | Cuenta CxP proveedor (`PersonaCuentaContable`, `tipoCuenta=1`) | `factura.total` |

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

- Si `DEBE ≠ HABER` → **excepción + rollback completo** (ni factura ni asiento quedan en BD) → HTTP 422
- Si el TipoAsiento no existe → detectado en validaciones bloqueantes (Paso 3), nunca llega al asiento

---

## 7. Almacenamiento del XML

Usa `FileService` centralizado. Directorio base (por prioridad):

1. Propiedad: `-Dsaa.upload.dir=...`
2. Variable de entorno: `SAA_UPLOAD_DIR`
3. Default Linux: `/opt/saa-uploads/`
4. Default Windows: `{userHome}/saa-uploads/`

Subdirectorio: `docs/xml/cxp/`  
Nombre: `{claveAcceso}.xml`  
Extensión `.xml` está incluida en `FileService.EXTENSIONES_PERMITIDAS`.

---

## 8. Reversión

**Endpoint:** `POST /carga-documentos/revertir/{idDocumentoCxp}`  
Solo aplica a documentos en estado `3 (REGISTRADO_BD)`.

**Pasos:**
1. Recuperar asiento vinculado → cambiar estado a `ANULADO (2)` en `CNT.ASNT`
2. Eliminar registros CXP (detalles → paths → cabecera)
3. `DocumentoCxp.estadoDocumento = 6 (REVERTIDO)`

---

## 9. Endpoints REST

| Método | Path | HTTP éxito | HTTP error |
|---|---|---|---|
| POST | `/cargarTxt` | 201 | 500 |
| POST | `/procesarXml/{id}` | 200 | 422 |
| POST | `/cargarXml/{id}` *(legacy)* | 200 | 422 |
| POST | `/registrarBD/{id}` *(legacy)* | 200 | 422 |
| POST | `/resolverNovedad/{id}` | 200 | 500 |
| POST | `/revertir/{id}` | 200 | 500 |
| GET | `/resumen/{idCargaTxt}` | 200 | 500 |
| GET | `/documento/{id}` | 200 | 404/500 |
| GET | `/novedades/{idEmpresa}` | 200 | 500 |
| GET | `/productosPendientes/{idFacturaCompra}` | 200 | 500 |

---

## 10. Pendientes de pulir

| Tipo de comprobante | Pendiente |
|---|---|
| `Nota de Crédito` | Aplicar validaciones bloqueantes (cuenta proveedor, tipo asiento, cuenta grupo) + pulir asiento |
| `Nota de Débito` | Ídem |
| `Liquidación de Compra` | Ídem |
| `Retención V1 y V2` | Revisar estructura XML, mapeo de campos, generación del asiento |
| Todos | Script SQL para insertar rubros 174 con los nuevos códigos 6 y 7 en `SCP.PDTR` |

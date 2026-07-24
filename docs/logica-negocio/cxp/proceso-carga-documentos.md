# Proceso de Carga de Documentos CXP

> **Archivo de referencia principal** — reemplaza a `ACTUALIZACION_CARGA_DOCUMENTOS_CXP.md`.  
> Actualizar aquí cada vez que se afina el proceso.  
> Última revisión: 2026-07-23

**Módulo:** CXP - Cuentas por Pagar  
**Stack:** Jakarta EE · WildFly · Oracle DB · Schema PGS

---

## 1. Contexto y arquitectura

### Problema original
La tabla `DCTX` mezclaba dos responsabilidades: registrar líneas del TXT y hacer seguimiento del ciclo de vida del documento. Esto provocaba múltiples registros para un mismo documento cuando aparecía en varias cargas.

### Solución implementada
Tres tablas con responsabilidades separadas:

| Tabla | Entidad Java | Propósito |
|---|---|---|
| `PGS.CRTX` | `CargaArchivoTxt` | Cabecera de cada archivo TXT cargado |
| `PGS.DCXP` | `DocumentoCxp` | **Un solo registro por documento** (por `claveAcceso`). Ciclo de vida completo |
| `PGS.DCTX` | `DetalleCargaTxt` | Una línea por aparición en un TXT. N líneas posibles para el mismo documento. FK a DCXP |

```
CRTX (CargaArchivoTxt)
  └── DCTX (DetalleCargaTxt) ──FK──► DCXP (DocumentoCxp)
         línea del archivo              documento único por empresa
         resultado: NUEVO|DUPLICADO     estadoDocumento: 1..6
                    NOVEDAD|IGNORADO    pathXml, idDocumentoBD, etc.
```

### Archivos Java clave

| Archivo | Paquete | Rol |
|---|---|---|
| `ProcesoCargaDocumentosService.java` | `com.saa.ejb.cxp.service` | Interfaz del servicio principal |
| `ProcesoCargaDocumentosServiceImpl.java` | `com.saa.ejb.cxp.serviceImpl` | Implementación completa de las 5 fases + flujo unificado |
| `ProcesoCargaDocumentosRest.java` | `com.saa.ws.rest.cxp` | Endpoints REST del proceso |
| `GrupoProductoPagoRest.java` | `com.saa.ws.rest.cxp` | CRUD de grupos de producto (con protección del tipo POR_CLASIFICAR) |
| `DocumentoCxp.java` | `com.saa.model.cxp` | Entidad principal del ciclo de vida |
| `TipoGrupoProductos.java` | `com.saa.rubros` | Constantes de tipo de grupo: BIEN=1, SERVICIO=2, POR_CLASIFICAR=3 |

---

## 2. Modelo de datos

### `DCXP` — DocumentoCxp

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | NUMBER | PK |
| `empresa` | NUMBER(11) | FK a SCP.PJRQ — empresa receptora |
| `claveAcceso` | VARCHAR2(100) | UNIQUE — clave SRI del documento |
| `rucEmisor` | VARCHAR2(20) | RUC del proveedor |
| `razonSocialEmisor` | VARCHAR2(500) | Razón social del proveedor |
| `tipoComprobante` | VARCHAR2(100) | Factura / Nota de Crédito / etc. |
| `serieComprobante` | VARCHAR2(50) | Ej: `001-001-000000123` |
| `fechaAutorizacion` | TIMESTAMP | Fecha de autorización SRI |
| `fechaEmision` | DATE | Fecha de emisión del documento |
| `identificacionReceptor` | VARCHAR2(20) | RUC receptor (empresa) |
| `valorSinImpuestos` | NUMBER(14,2) | Subtotal |
| `iva` | NUMBER(12,2) | IVA |
| `importeTotal` | NUMBER(14,2) | Total |
| `estadoDocumento` | NUMBER(2) | Ver tabla de estados |
| `pathXml` | VARCHAR2(2000) | Path físico del XML en el servidor |
| `idDocumentoBD` | NUMBER(11) | ID del registro en la tabla destino (ej: FacturaCompra.id) |
| `tipoTablaDestino` | VARCHAR2(50) | `FACTURA_COMPRA` / `NOTA_CREDITO_COMPRA` / etc. |
| `novedad` | VARCHAR2(2000) | Descripción de diferencias detectadas entre cargas |
| `estadoNovedad` | NUMBER(2) | 1=PENDIENTE 2=REEMPLAZADO 3=MANTENIDO |
| `fechaCargaXml` | TIMESTAMP | Cuándo se subió el XML |
| `usuarioCargaXml` | NUMBER(11) | FK a SCP.PJRQ |
| `fechaRegistroBD` | TIMESTAMP | Cuándo se registró en tablas CXP |
| `usuarioRegistroBD` | NUMBER(11) | FK a SCP.PJRQ |
| `fechaReversion` | TIMESTAMP | Cuándo se revirtió |
| `usuarioReversion` | NUMBER(11) | FK a SCP.PJRQ |
| `observacion` | VARCHAR2(2000) | Errores o notas adicionales |

### `DCTX` — DetalleCargaTxt

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | NUMBER | PK |
| `cargaTxt.id` | NUMBER(11) | FK a CRTX |
| `documento.id` | NUMBER(11) | FK a DCXP |
| `valorSinImpuestosCarga` | NUMBER(14,2) | Snapshot del valor en esta carga |
| `ivaCarga` | NUMBER(12,2) | Snapshot del IVA en esta carga |
| `importeTotalCarga` | NUMBER(14,2) | Snapshot del total en esta carga |
| `fechaAutorizacionCarga` | TIMESTAMP | Snapshot fecha autorización |
| `fechaEmisionCarga` | DATE | Snapshot fecha emisión |
| `resultado` | VARCHAR2(20) | `NUEVO` / `DUPLICADO` / `NOVEDAD` / `IGNORADO` |
| `observacion` | VARCHAR2(2000) | Detalle adicional |

### `CRTX` — CargaArchivoTxt

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | NUMBER | PK |
| `empresa` | NUMBER(11) | FK a SCP.PJRQ |
| `usuario` | NUMBER(11) | FK a SCP.PJRQ |
| `fechaCarga` | TIMESTAMP | Fecha/hora de la carga |
| `nombreArchivo` | VARCHAR2(500) | Nombre original del archivo TXT |
| `totalRegistros` | NUMBER(11) | Total de líneas procesadas |
| `registrosNuevos` | NUMBER(11) | Documentos nuevos |
| `registrosDuplicados` | NUMBER(11) | Documentos ya existentes sin diferencias |
| `registrosNovedad` | NUMBER(11) | Documentos con diferencias de valores |
| `estado` | NUMBER(2) | 1=PROCESADO 2=ERROR_PARCIAL |

---

## 3. Estados del DocumentoCxp

### `estadoDocumento`

| Valor | Nombre | Descripción | Acción disponible en frontend |
|---|---|---|---|
| `1` | LEIDO | Leído del TXT, esperando XML | Botón "Cargar XML y Registrar" |
| `2` | XML_CARGADO | XML validado y guardado *(transitorio interno)* | — |
| `3` | REGISTRADO_BD | Registrado en tablas CXP | Botón "Revertir" |
| `4` | ERROR | Falló algún paso (ver campo `observacion`) | Botón "Reintentar" |
| `5` | NOVEDAD | Valores distintos detectados respecto a carga anterior | Botón "Resolver novedad" |
| `6` | REVERTIDO | Registros de BD eliminados | Botón "Cargar XML y Registrar" nuevamente |

> ⚠️ El estado `2 (XML_CARGADO)` es transitorio. Con el endpoint unificado `/procesarXml` el documento pasa directamente de `1` a `3`. Solo queda en `2` si se usan los endpoints legacy separados.

### `estadoNovedad`

| Valor | Descripción |
|---|---|
| `1` | Pendiente de resolución |
| `2` | Reemplazado |
| `3` | Mantenido (usuario decidió no cambiar) |

---

## 4. Tablas destino por tipo de comprobante

| `tipoComprobante` (TXT/XML) | `tipoTablaDestino` | Tablas que se llenan | Maneja POR_CLASIFICAR |
|---|---|---|---|
| `Factura` | `FACTURA_COMPRA` | `FacturaCompra` + `DetalleFacturaCompra` + `FormaPagoFacturaCompra` + `PathFacturaCompra` | ✅ Sí |
| `Nota de Crédito` | `NOTA_CREDITO_COMPRA` | `NotaCreditoCompra` + `DetalleNotaCreditoCompra` + `PathNotaCreditoCompra` | ❌ No |
| `Nota de Débito` | `NOTA_DEBITO_COMPRA` | `NotaDebitoCompra` + `DetalleNotaDebitoCompra` + `PathNotaDebitoCompra` | ❌ No |
| `Liquidación de compra` | `LIQUIDACION_COMPRA_COMPRA` | `LiquidacionCompraCompra` + `DetalleLiquidacionCompraCompra` + `PathLiquidacionCompraCompra` | ❌ No* |
| `Comprobante de Retención` | `RETENCION_COMPRA` | `RetencionCompra` + `DetalleRetencionCompra` + `PathRetencionCompra` | ❌ No |
| `Comprobante de Retención electrónica versión 2.0` | `RETENCION_COMPRA_V2` | `RetencionCompraV2` *(sin path — ver DT-1)* | ❌ No |

> (*) La liquidación de compra tiene líneas de detalle pero no usa el modelo de `ProductoPago`, por lo que no requiere clasificación.

---

## 5. Grupo de productos "POR CLASIFICAR"

### Concepto
Cuando se procesa un XML de factura de compra con productos que no existen en el sistema, en lugar de interrumpir el proceso, el backend los **crea automáticamente** en un grupo especial identificado por `rubroTipoGrupoH = 3 (POR_CLASIFICAR)`.

Esto permite que:
- El documento quede registrado en BD sin interrupciones, incluso con muchos productos nuevos.
- El usuario clasifique los productos después, desde una pantalla dedicada.
- La contabilización quede **bloqueada** hasta que todos los productos tengan un grupo definitivo.

### Constantes (`TipoGrupoProductos.java`)
```java
public interface TipoGrupoProductos {
    int BIEN           = 1;  // Grupo de bienes (creado por usuario)
    int SERVICIO       = 2;  // Grupo de servicios (creado por usuario)
    int POR_CLASIFICAR = 3;  // Uso exclusivo del backend — protegido en el REST
}
```

El identificador es `rubroTipoGrupoH = 3` en la tabla `PGS.GRPP`. **No se identifica por nombre**, por lo que puede renombrarse libremente en BD sin afectar la lógica.

### Reglas del grupo POR_CLASIFICAR

| Operación | Frontend | Backend |
|---|---|---|
| **Crear** grupo con `rubroTipoGrupoH = 3` | ❌ HTTP 403 | ✅ Solo al procesar XML |
| **Modificar** grupo con `rubroTipoGrupoH = 3` | ❌ HTTP 403 | — |
| **Eliminar** grupo con `rubroTipoGrupoH = 3` | ❌ HTTP 403 | — |
| **Consultar** grupo (GET) | ✅ Libre | ✅ |
| **Mover producto** a otro grupo (PUT en ProductoPago) | ✅ Permitido | — |

> El backend crea el grupo automáticamente la primera vez que se necesita para esa empresa. Si ya existe uno con `rubroTipoGrupoH = 3`, lo reutiliza.

---

## 6. Auto-creación de entidades

### Titular-Proveedor (TSR)
- Se busca por `identificacion = rucEmisor`.
- Si no existe → se crea con datos del XML (`razonSocial`, `telefono`, `correoElectronico`, `dirEstablecimiento`/`dirMatriz`).
- Tipo de identificación: 13 dígitos → RUC (2), 10 dígitos → Cédula (1), otro → Pasaporte (3).
- Si existe pero no tiene `tipoProveedor=1` → se le asigna automáticamente.

### Producto (`ProductoPago`)
- Se busca por nombre (case-insensitive) dentro de la empresa.
- Si no existe → se crea en el grupo **"POR CLASIFICAR"** (`rubroTipoGrupoH = 3`).
- Si el grupo "POR CLASIFICAR" no existe para esa empresa → se crea automáticamente.

---

## 7. Servicios REST — BASE URL: `/saaBE/rest/`

### 7.1 Proceso principal — `/carga-documentos`

#### FASE 1 — Cargar archivo TXT
**`POST /carga-documentos/cargarTxt`**

```json
// Request
{
  "contenidoTxt": "<contenido completo del archivo TXT>",
  "nombreArchivo": "1793228946001_Recibidos.txt",
  "idEmpresa": 1,
  "idUsuario": 5
}

// Response 201
{
  "idCargaTxt": 10,
  "nombreArchivo": "1793228946001_Recibidos.txt",
  "totalRegistros": 45,
  "nuevos": 38,
  "duplicados": 5,
  "novedades": 2,
  "detalles": [
    { "linea": 1, "serie": "001-001-000000123", "claveAcceso": "2406202401...", "resultado": "NUEVO",     "idDocumentoCxp": 101 },
    { "linea": 2, "serie": "001-001-000000050", "claveAcceso": "1506202401...", "resultado": "DUPLICADO", "idDocumentoCxp": 55  },
    { "linea": 3, "serie": "001-001-000000099", "claveAcceso": "0106202401...", "resultado": "NOVEDAD",   "idDocumentoCxp": 72, "diferencias": "importeTotal: previo=100.00 nuevo=115.00" }
  ]
}
```

> El `idDocumentoCxp` de cada línea es el **ID permanente del documento**. Se usa en todas las fases siguientes. El `idCargaTxt` solo sirve para consultar el resumen de esa carga.

**Lógica interna:**
1. Obtiene el RUC receptor de la empresa via `Facturador`.
2. Crea una cabecera `CargaArchivoTxt`.
3. Itera líneas del TXT (tab-delimitado, 11+ columnas, salta encabezado `RUC_EMISOR`).
4. Valida que `identificacionReceptor` coincida con el RUC de la empresa; si no → `IGNORADO`.
5. Por línea: **NUEVO** / **DUPLICADO** / **NOVEDAD** según si el `DocumentoCxp` existe y si hay diferencias en montos/fechas.
6. Si es NOVEDAD y el doc estaba en `REGISTRADO_BD` → pasa a `NOVEDAD (5)`. Si estaba en `LEIDO/XML_CARGADO/REVERTIDO` → actualiza valores, vuelve a `LEIDO (1)`.
7. Siempre registra `DetalleCargaTxt` con snapshot de valores y resultado.

---

#### FASE 2+3 UNIFICADA ⭐ — Procesar XML *(endpoint principal recomendado)*
**`POST /carga-documentos/procesarXml/{idDocumentoCxp}`**

Realiza en un solo paso: valida XML → guarda en disco → registra en tablas CXP → auto-crea proveedor/productos si faltan.

```json
// Request
{
  "contenidoXml": "<?xml version=\"1.0\"...>",
  "idEmpresa": 1,
  "idUsuario": 5
  // "pathDestino": "/docs/xml/cxp/clave.xml"  ← opcional, el backend lo calcula como /docs/xml/cxp/{claveAcceso}.xml
}

// Response 422 → El XML no coincide con el documento esperado (nada se guarda)
{
  "valido": false,
  "errores": [
    "rucEmisor: esperado=1790016919001 | en XML=0999999999001",
    "importeTotal: esperado=100.00 | en XML=115.50",
    "serieComprobante: esperada=001-001-000000123 | en XML=001-001-000000124"
  ],
  "documento": { "id": 101, "estadoDocumento": 1, "claveAcceso": "...", "..." : "..." }
}

// Response 200 → Proveedor no encontrado en TSR (documento pasa a estadoDocumento=4)
{
  "valido": true,
  "error": "TITULAR_NO_ENCONTRADO",
  "mensaje": "El emisor con RUC 1790016919001 no existe en TSR. Créelo como Proveedor.",
  "rucEmisor": "1790016919001"
}

// Response 200 → Éxito con productos auto-creados en POR_CLASIFICAR
{
  "valido": true,
  "idDocumentoBD": 234,
  "tipoTablaDestino": "FACTURA_COMPRA",
  "mensaje": "FacturaCompra registrada con id=234. 3 producto(s) creados en grupo POR CLASIFICAR.",
  "productosPendientes": ["Milhojas", "Pan de yema", "Empanada de viento"]
}

// Response 200 → Éxito total, todos los productos ya existían
{
  "valido": true,
  "idDocumentoBD": 234,
  "tipoTablaDestino": "FACTURA_COMPRA",
  "mensaje": "FacturaCompra registrada con id=234.",
  "productosPendientes": []
}
```

**Campos que valida el XML contra el DocumentoCxp:**

| Campo | Nodo XML buscado | Tolerancia |
|---|---|---|
| `claveAcceso` | `claveAccesoConsultada` o `claveAcceso` | Exacta |
| `rucEmisor` | `ruc` o `rucEmisor` | Exacta |
| `razonSocialEmisor` | `razonSocial` | Case-insensitive |
| `serieComprobante` | `estab`-`ptoEmi`-`secuencial` | Exacta |
| `valorSinImpuestos` | `totalSinImpuestos` | ±0.01 |
| `importeTotal` | `importeTotal` | ±0.01 |
| `iva` | `totalImpuesto[codigo=2].valor` (suma) | ±0.01 |

**Lógica del frontend al recibir la respuesta:**
```
if (httpStatus == 422):
    → Mostrar lista de errores: response.errores[]
    → El usuario debe subir el XML correcto

if (httpStatus == 200):
    if (response.error == "TITULAR_NO_ENCONTRADO"):
        → "El proveedor {rucEmisor} no existe. Créelo en Tesorería > Titulares."

    if (response.productosPendientes.length > 0):
        → Mostrar aviso: "{N} productos sin clasificar. No se puede contabilizar aún."
        → Botón: "Ir a clasificar productos"
    else:
        → "Documento registrado correctamente. Puede contabilizarse."
```

---

#### FASE 4 — Resolver novedad
**`POST /carga-documentos/resolverNovedad/{idDocumentoCxp}`**
*(Solo cuando `estadoDocumento = 5`)*

```json
// Request — mantener el documento previo sin cambios
{ "accion": "MANTENER", "idUsuario": 5 }

// Request — reemplazar con nuevo XML
{ "accion": "REEMPLAZAR", "contenidoXml": "<?xml...>", "idUsuario": 5 }

// Response MANTENER
{ "accion": "MANTENIDO", "mensaje": "Se mantiene el documento sin cambios." }

// Response REEMPLAZAR → revierte registros previos y vuelve a registrar
{ "accion": "REEMPLAZADO", "idDocumentoBD": 235, "tipoTablaDestino": "FACTURA_COMPRA", "mensaje": "FacturaCompra registrada con id=235" }
```

| Acción | Comportamiento |
|---|---|
| `REEMPLAZAR` | Si `idDocumentoBD != null` → revierte registros BD previos → carga nuevo XML → llama a `registrarDocumentoBD` |
| `MANTENER` | Marca `estadoNovedad=3`, conserva registros sin cambios |

---

#### FASE 5 — Revertir documento
**`POST /carga-documentos/revertir/{idDocumentoCxp}`**
*(Solo cuando `estadoDocumento = 3`)*

```json
// Request
{ "idUsuario": 5 }

// Response 200 → documento pasa a estadoDocumento=6 (REVERTIDO)
{
  "mensaje": "Documento revertido correctamente.",
  "idDocumentoCxp": 101,
  "idDocumentoBD": 234,
  "tipoTablaDestino": "FACTURA_COMPRA"
}
```

Elimina en cascada: detalles, formas de pago, paths y cabecera de la tabla destino.

---

#### Verificar si una factura puede contabilizarse
**`GET /carga-documentos/productosPendientes/{idFacturaCompra}`**

Debe llamarse **antes de generar el asiento contable** de cualquier `FacturaCompra`.

```json
// Con productos sin clasificar → NO contabilizar
{ "idFacturaCompra": 234, "pendientes": ["Milhojas", "Pan de yema"], "puedeContabilizar": false }

// Todos clasificados → SÍ puede contabilizarse
{ "idFacturaCompra": 234, "pendientes": [], "puedeContabilizar": true }
```

---

#### Consultas generales del proceso

| Método | URL | Descripción |
|---|---|---|
| `GET` | `/carga-documentos/resumen/{idCargaTxt}` | Cabecera + líneas de una carga con `DocumentoCxp` embebido |
| `GET` | `/carga-documentos/documento/{idDocumentoCxp}` | Un `DocumentoCxp` por su ID |
| `GET` | `/carga-documentos/novedades/{idEmpresa}` | Documentos con `estadoDocumento=5` y `estadoNovedad=1` |

---

#### Endpoints legacy (no usar en nuevas pantallas)

| Método | URL | Descripción |
|---|---|---|
| `POST` | `/carga-documentos/cargarXml/{id}` | Solo valida y guarda el XML sin registrar en BD |
| `POST` | `/carga-documentos/registrarBD/{id}` | Solo registra en BD, requiere XML ya guardado en disco |
| `POST` | `/carga-documentos/crearProductosYRegistrar/{id}` | Flujo antiguo de productos — ya no necesario con el flujo unificado |

---

### 7.2 Cargas (CRTX) — `/crtx`

| Método | URL | Descripción |
|---|---|---|
| `GET` | `/crtx/getAll` | Todas las cargas |
| `GET` | `/crtx/getId/{id}` | Carga por ID |
| `GET` | `/crtx/getByEmpresa/{idEmpresa}` | Cargas de una empresa, ordenadas por fecha desc |
| `POST` | `/crtx/selectByCriteria` | Búsqueda por criterios |
| `POST` | `/crtx` | Crear |
| `PUT` | `/crtx` | Actualizar |
| `DELETE` | `/crtx/{id}` | Eliminar |

---

### 7.3 Líneas de carga (DCTX) — `/dctx`

| Método | URL | Descripción |
|---|---|---|
| `GET` | `/dctx/getAll` | Todas las líneas |
| `GET` | `/dctx/getId/{id}` | Línea por ID |
| `GET` | `/dctx/getByCarga/{idCarga}` | Líneas de una carga (con `DocumentoCxp` embebido) |
| `GET` | `/dctx/getByDocumento/{idDocumentoCxp}` | Historial de cargas en las que apareció un documento |
| `POST` | `/dctx/selectByCriteria` | Búsqueda por criterios |
| `POST` | `/dctx` | Crear |
| `PUT` | `/dctx` | Actualizar |
| `DELETE` | `/dctx/{id}` | Eliminar |

---

### 7.4 Documentos únicos (DCXP) — `/dcxp`

| Método | URL | Descripción |
|---|---|---|
| `GET` | `/dcxp/getAll` | Todos los documentos |
| `GET` | `/dcxp/getId/{id}` | Documento por ID |
| `GET` | `/dcxp/getByEmpresa/{idEmpresa}` | Todos los documentos de una empresa, ordenados por id desc |
| `GET` | `/dcxp/getByEmpresaEstado/{idEmpresa}/{estado}` | Documentos filtrados por estado |
| `GET` | `/dcxp/novedadesPendientes/{idEmpresa}` | Con `estadoDocumento=5` y `estadoNovedad=1` |
| `POST` | `/dcxp/selectByCriteria` | Búsqueda por criterios |
| `POST` | `/dcxp` | Crear |
| `PUT` | `/dcxp` | Actualizar |
| `DELETE` | `/dcxp/{id}` | Eliminar |

---

### 7.5 Grupos de producto (GRPP) — `/grpp`

| Método | URL | Frontend | Descripción |
|---|---|---|---|
| `GET` | `/grpp/getAll` | ✅ Libre | Todos los grupos (incluye POR_CLASIFICAR) |
| `GET` | `/grpp/getId/{id}` | ✅ Libre | Grupo por ID |
| `POST` | `/grpp/selectByCriteria` | ✅ Libre | Búsqueda por criterios |
| `POST` | `/grpp` | ⚠️ Bloqueado si `rubroTipoGrupoH=3` → **HTTP 403** | Crear grupo |
| `PUT` | `/grpp` | ⚠️ Bloqueado si el grupo es o intenta ser `rubroTipoGrupoH=3` → **HTTP 403** | Actualizar grupo |
| `DELETE` | `/grpp/{id}` | ⚠️ Bloqueado si `rubroTipoGrupoH=3` → **HTTP 403** | Eliminar grupo |

---

## 8. Resumen completo de endpoints

| Grupo | Método | URL | Descripción |
|---|---|---|---|
| **Proceso** | `POST` | `/carga-documentos/cargarTxt` | Fase 1: leer TXT |
| **Proceso** ⭐ | `POST` | `/carga-documentos/procesarXml/{id}` | Fase 2+3 unificada: validar XML + registrar en BD |
| **Proceso** | `POST` | `/carga-documentos/resolverNovedad/{id}` | Fase 4: resolver novedad |
| **Proceso** | `POST` | `/carga-documentos/revertir/{id}` | Fase 5: revertir documento |
| **Proceso** | `GET` | `/carga-documentos/resumen/{idCargaTxt}` | Cabecera + líneas de una carga |
| **Proceso** | `GET` | `/carga-documentos/documento/{id}` | Un DocumentoCxp por ID |
| **Proceso** | `GET` | `/carga-documentos/novedades/{idEmpresa}` | Novedades pendientes |
| **Proceso** | `GET` | `/carga-documentos/productosPendientes/{idFacturaCompra}` | Pre-validación contabilización |
| **Legacy** | `POST` | `/carga-documentos/cargarXml/{id}` | Solo valida y guarda XML |
| **Legacy** | `POST` | `/carga-documentos/registrarBD/{id}` | Solo registra en BD |
| **Legacy** | `POST` | `/carga-documentos/crearProductosYRegistrar/{id}` | Ya no necesario |
| **CRTX** | `GET` | `/crtx/getByEmpresa/{idEmpresa}` | Cargas de una empresa |
| **CRTX** | `GET` | `/crtx/getId/{id}` | Carga por ID |
| **DCTX** | `GET` | `/dctx/getByCarga/{idCarga}` | Líneas de una carga |
| **DCTX** | `GET` | `/dctx/getByDocumento/{idDocumentoCxp}` | Historial de cargas de un documento |
| **DCXP** | `GET` | `/dcxp/getByEmpresa/{idEmpresa}` | Todos los documentos de una empresa |
| **DCXP** | `GET` | `/dcxp/getByEmpresaEstado/{idEmpresa}/{estado}` | Documentos por estado |
| **DCXP** | `GET` | `/dcxp/novedadesPendientes/{idEmpresa}` | Novedades pendientes |
| **GRPP** | `GET` | `/grpp/getAll` | Grupos de producto (incluye POR_CLASIFICAR) |
| **GRPP** | `POST/PUT/DELETE` | `/grpp` | Protegido: bloquea `rubroTipoGrupoH=3` → HTTP 403 |

---

## 9. Flujo de pantallas recomendado para el frontend

```
┌─────────────────────────────────────────────────────────────────┐
│ PANTALLA 1: Historial de cargas de la empresa                   │
│  GET /crtx/getByEmpresa/{idEmpresa}                             │
│  Muestra: fecha, archivo, nuevos, duplicados, novedades         │
│  → Click en fila: ir a PANTALLA 2                               │
│  → Botón "Nueva carga": acción Cargar TXT                       │
└─────────────────────────────────────────────────────────────────┘
        ↓ "Nueva carga"                    ↓ click en fila
┌──────────────────────────┐   ┌────────────────────────────────────┐
│ ACCIÓN: Cargar TXT       │   │ PANTALLA 2: Detalle de la carga    │
│  POST /carga-documentos/ │   │  GET /dctx/getByCarga/{idCarga}    │
│       cargarTxt          │   │  Muestra líneas + estado actual    │
│  Mostrar resumen         │   │  del DocumentoCxp embebido         │
└──────────────────────────┘   └────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ PANTALLA 3: Lista de documentos por estado                      │
│  GET /dcxp/getByEmpresaEstado/{idEmpresa}/{estado}              │
│                                                                 │
│  Estado 1 (LEIDO)      → botón "Cargar XML y Registrar"         │
│  Estado 3 (REGISTRADO) → botón "Revertir"                       │
│                          indicador si tiene productos           │
│                          pendientes de clasificar               │
│  Estado 4 (ERROR)      → mostrar campo observacion             │
│  Estado 5 (NOVEDAD)    → botón "Resolver novedad"               │
│  Estado 6 (REVERTIDO)  → botón "Cargar XML y Registrar"         │
└─────────────────────────────────────────────────────────────────┘
        ↓ "Cargar XML y Registrar"
┌─────────────────────────────────────────────────────────────────┐
│ ACCIÓN: Procesar XML (flujo unificado)                          │
│  POST /carga-documentos/procesarXml/{idDocumentoCxp}            │
│  Body: { contenidoXml, idEmpresa, idUsuario }                   │
│                                                                 │
│  HTTP 422 → Mostrar errores de validación                       │
│             El usuario debe subir el XML correcto               │
│                                                                 │
│  HTTP 200 con error "TITULAR_NO_ENCONTRADO":                    │
│    → "El proveedor {rucEmisor} no existe. Créelo en TSR."       │
│                                                                 │
│  HTTP 200 con productosPendientes no vacío:                     │
│    → "X productos sin clasificar. No puede contabilizarse."     │
│    → Botón: ir a PANTALLA Clasificación de Productos            │
│                                                                 │
│  HTTP 200 con productosPendientes vacío:                        │
│    → "Documento registrado. Puede contabilizarse."              │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ PANTALLA: Clasificación de Productos Pendientes                 │
│                                                                 │
│  a. Obtener el grupo POR_CLASIFICAR de la empresa:              │
│     POST /grpp/selectByCriteria → filtrar rubroTipoGrupoH=3     │
│  b. Listar sus productos:                                       │
│     POST /prdp/selectByCriteria → filtrar por id del grupo      │
│  c. Para cada producto: selector de grupo destino               │
│     GET /grpp/getAll → mostrar grupos EXCEPTO rubroTipoGrupoH=3 │
│  d. Guardar el cambio de grupo:                                 │
│     PUT /prdp → actualizar campo grupoProducto                  │
│  e. Verificar si ya puede contabilizarse:                       │
│     GET /carga-documentos/productosPendientes/{idFacturaCompra} │
│     → puedeContabilizar: true → habilitar botón asiento         │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ PANTALLA: Resolver novedad (estadoDocumento = 5)                │
│  POST /carga-documentos/resolverNovedad/{idDocumentoCxp}        │
│                                                                 │
│  Mostrar: campo novedad (diferencias detectadas)                │
│  Opción A → MANTENER: { "accion": "MANTENER", "idUsuario": X } │
│  Opción B → REEMPLAZAR con nuevo XML:                           │
│    { "accion": "REEMPLAZAR", "contenidoXml": "...",             │
│      "idUsuario": X }                                           │
└─────────────────────────────────────────────────────────────────┘
```

---

## 10. Reglas de negocio clave

1. **Un `DocumentoCxp` por `claveAcceso`**: Si el mismo documento aparece en múltiples cargas TXT, siempre apunta al mismo `DocumentoCxp`. Las N apariciones generan N registros en `DCTX`, pero uno solo en `DCXP`.

2. **Validación del XML contra el TXT**: Al usar `/procesarXml`, si el XML no coincide con el proveedor o valores registrados → HTTP 422 con lista de diferencias. El documento **no se modifica**.

3. **Productos auto-creados**: Los productos nuevos encontrados en el XML se crean en el grupo `POR_CLASIFICAR` (`rubroTipoGrupoH = 3`). El documento se registra igualmente. La contabilización queda bloqueada hasta clasificar todos los productos.

4. **Bloqueo de contabilización**: Antes de generar el asiento contable de una `FacturaCompra`, llamar a `GET /carga-documentos/productosPendientes/{idFacturaCompra}`. Si `puedeContabilizar = false` → no permitir el asiento.

5. **Identificación del grupo POR_CLASIFICAR**: Se identifica por `rubroTipoGrupoH = 3`, **no por nombre**. Puede renombrarse en BD sin afectar la lógica del backend.

6. **Novedades**: Si entre una carga TXT y la siguiente cambian los valores de un documento ya `REGISTRADO_BD`, el estado pasa a `NOVEDAD (5)`. El usuario decide MANTENER o REEMPLAZAR.

7. **Proveedor auto-creado**: Si el RUC emisor del XML no existe en `Titular` (TSR), se crea automáticamente con rol de Proveedor usando los datos disponibles en el XML/TXT. El documento continúa el registro normalmente. (Comportamiento anterior era marcar ERROR — fue corregido.)

8. **Asiento contable**: Se intenta generar automáticamente al pasar a `REGISTRADO_BD (3)` si `Facturador.generaConta = 1`. Si falla, se registra como advertencia (`advertenciaAsiento`) sin revertir el registro del documento.

---

## 11. Asiento contable automático

- Se ejecuta al final de la Fase 3 si `Facturador.generaConta = 1` para la empresa.
- Llama a `AsientoContableService.generarAsiento*` según el tipo de documento.
- Si falla (incluyendo `UnsupportedOperationException` de stubs no configurados) → **NO revierte** el registro; devuelve `advertenciaAsiento` en la respuesta.
- Pendiente configurar las plantillas en BD para cada tipo de asiento (ver DT-2).

---

## 12. Bugs corregidos

| # | Método | Descripción | Estado |
|---|---|---|---|
| 1 | `registrarFacturaCompra` | `PathFacturaCompra` se persistía **dos veces** con los mismos datos (doble `save`). | ✅ Corregido 2026-07-23 |
| 2 | `registrarRetencionCompraV2` | `PathRetencionCompra` se guardaba **sin FK** a `RetencionCompraV2` porque el modelo solo tiene FK a `RetencionCompra` (V1). Se eliminó el save incorrecto. | ✅ Corregido 2026-07-23 |
| 3 | `revertirRegistrosBD` / `RETENCION_COMPRA_V2` | No existía path que eliminar (consecuencia del bug 2). Se documentó el TODO para cuando se cree `PathRetencionCompraV2`. | ✅ Corregido 2026-07-23 |
| 4 | `resolverNovedad` / `REEMPLAZAR` | La condición usaba `doc.getEstadoDocumento() != null` (siempre `true`); corregida a `doc.getIdDocumentoBD() != null`. | ✅ Corregido 2026-07-23 |
| 5 | Asiento contable automático | Los stubs de `AsientoContableService` aún no están configurados. Se tratan como advertencia, no bloquean el registro. | ⏳ Pendiente |

---

## 13. Deuda técnica

| # | Descripción |
|---|---|
| DT-1 | Crear entidad `PathRetencionCompraV2` con FK a `RetencionCompraV2`, su DAO, y agregar el `save` en `registrarRetencionCompraV2` y el `delete` en `revertirRegistrosBD`. |
| DT-2 | Configurar plantillas de asiento contable en BD para cada tipo de comprobante CXP y activar la generación automática en `AsientoContableService`. |
| DT-3 | Crear endpoint `GET /prdp/getByGrupoPorClasificar/{idEmpresa}` para simplificar la carga de la pantalla de clasificación de productos (mientras tanto usar `POST /prdp/selectByCriteria`). |

---

## 14. Historial de cambios

| Fecha | Descripción |
|---|---|
| 2026-07-20 | Documento `ACTUALIZACION_CARGA_DOCUMENTOS_CXP.md` creado con arquitectura, modelo de datos, endpoints y flujo de pantallas |
| 2026-07-23 | Documento `proceso-carga-documentos.md` creado con análisis de bugs y lógica del servicio |
| 2026-07-23 | Ambos documentos fusionados en este archivo. `ACTUALIZACION_CARGA_DOCUMENTOS_CXP.md` queda obsoleto |
| 2026-07-23 | Bugs #1 al #4 corregidos en `ProcesoCargaDocumentosServiceImpl` |

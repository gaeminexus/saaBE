# Actualización: Carga de Documentos CXP desde TXT del SRI

**Fecha:** 2026-07-19  
**Módulo:** CXP - Cuentas por Pagar  
**Impacto:** Cambio conceptual en la estructura de tablas y en el flujo de endpoints

---

## 1. Contexto del cambio

### Problema anterior
La tabla `DCTX` (DetalleCargaTxt) mezclaba dos responsabilidades:
- Registrar cada línea de un archivo TXT cargado
- Hacer el seguimiento del ciclo de vida del documento (XML, registro en BD, reversión, novedades)

Esto causaba que un mismo documento (misma `claveAcceso`) tuviese múltiples registros en `DCTX` cuando se incluía en varias cargas, lo que impedía tener un único punto de seguimiento por documento.

### Solución implementada
Se separaron las responsabilidades en **dos tablas** y el frontend ahora trabaja con **dos IDs distintos**:

| Tabla | Entidad Java | Propósito |
|---|---|---|
| `PGS.DCXP` | `DocumentoCxp` | **UN solo registro por documento** (por `claveAcceso`). Aquí vive el ciclo de vida completo. |
| `PGS.DCTX` | `DetalleCargaTxt` | **Una línea por aparición** en un archivo TXT. Puede haber N líneas para un mismo documento. |
| `PGS.CRTX` | `CargaArchivoTxt` | Sin cambios. Cabecera del archivo cargado. |

---

## 2. Modelo de datos nuevo

```
CRTX (CargaArchivoTxt)
  └── DCTX (DetalleCargaTxt) ──FK──► DCXP (DocumentoCxp)
         línea del archivo              documento único
         resultado: NUEVO|DUPLICADO     estadoDocumento: 1..6
                    NOVEDAD|IGNORADO    pathXml, idDocumentoBD, etc.
```

### Campos de `DCXP` (DocumentoCxp)

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | NUMBER | PK - ID del documento único |
| `claveAcceso` | VARCHAR2(100) | **UNIQUE** - clave SRI del documento |
| `rucEmisor` | VARCHAR2(20) | RUC del proveedor |
| `razonSocialEmisor` | VARCHAR2(500) | Razón social del proveedor |
| `tipoComprobante` | VARCHAR2(100) | Factura / Nota de Crédito / etc. |
| `serieComprobante` | VARCHAR2(50) | Ej: 001-001-000000123 |
| `fechaAutorizacion` | TIMESTAMP | Fecha de autorización SRI |
| `fechaEmision` | DATE | Fecha de emisión del documento |
| `valorSinImpuestos` | NUMBER(14,2) | Subtotal actual del documento |
| `iva` | NUMBER(12,2) | IVA actual |
| `importeTotal` | NUMBER(14,2) | Total actual |
| `estadoDocumento` | NUMBER(2) | Ver tabla de estados |
| `pathXml` | VARCHAR2(2000) | Path del XML en el servidor |
| `idDocumentoBD` | NUMBER(11) | ID del registro en tabla destino |
| `tipoTablaDestino` | VARCHAR2(50) | FACTURA_COMPRA / NOTA_CREDITO_COMPRA / etc. |
| `novedad` | VARCHAR2(2000) | Descripción de diferencias detectadas |
| `estadoNovedad` | NUMBER(2) | 1=PENDIENTE 2=REEMPLAZADO 3=MANTENIDO |

### Estados del documento (`estadoDocumento`)

| Valor | Nombre | Descripción | Acción disponible |
|---|---|---|---|
| `1` | LEIDO | Documento leído del TXT, esperando XML | Cargar XML |
| `2` | XML_CARGADO | XML subido, esperando registro | Registrar en BD |
| `3` | REGISTRADO_BD | Registrado en tablas CXP | Revertir |
| `4` | ERROR | Falló algún paso | Reintentar |
| `5` | NOVEDAD | Valores distintos detectados | Resolver novedad |
| `6` | REVERTIDO | Registros de BD eliminados | Cargar XML nuevamente |

### Campos de `DCTX` (DetalleCargaTxt) - estructura nueva

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | NUMBER | PK |
| `cargaTxt.id` | NUMBER | FK a CRTX |
| `documento.id` | NUMBER | **FK a DCXP** |
| `valorSinImpuestosCarga` | NUMBER(14,2) | Valor en **esta** carga (snapshot) |
| `ivaCarga` | NUMBER(12,2) | IVA en esta carga (snapshot) |
| `importeTotalCarga` | NUMBER(14,2) | Total en esta carga (snapshot) |
| `resultado` | VARCHAR2(20) | NUEVO / DUPLICADO / NOVEDAD / IGNORADO |
| `observacion` | VARCHAR2(2000) | Detalle adicional |

---

## 3. Flujo completo de los endpoints

### BASE URL
```
POST/GET  /saaBE/rest/carga-documentos/...
```

---

### FASE 1 — Cargar archivo TXT
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
    {
      "linea": 1,
      "serie": "001-001-000000123",
      "claveAcceso": "2406202401...",
      "resultado": "NUEVO",
      "idDocumentoCxp": 101
    },
    {
      "linea": 2,
      "serie": "001-001-000000050",
      "claveAcceso": "1506202401...",
      "resultado": "DUPLICADO",
      "idDocumentoCxp": 55
    },
    {
      "linea": 3,
      "serie": "001-001-000000099",
      "claveAcceso": "0106202401...",
      "resultado": "NOVEDAD",
      "idDocumentoCxp": 72,
      "diferencias": "importeTotal: previo=100.00 nuevo=115.00"
    }
  ]
}
```

> ⚠️ **Cambio importante:** El campo `idDocumentoCxp` en cada detalle es el ID del `DocumentoCxp`. Este es el ID que se usará en **todas las fases siguientes** (2, 3, 4 y 5).  
> El `idCargaTxt` solo sirve para consultar el resumen de esa carga.

---

### FASE 2 — Subir XML de un documento
**`POST /carga-documentos/cargarXml/{idDocumentoCxp}`**

> **Antes:** usaba `{idDetalle}` (ID de DCTX)  
> **Ahora:** usa `{idDocumentoCxp}` (ID de DCXP)

```json
// Request
{
  "contenidoXml": "<?xml version=\"1.0\"...>",
  "idUsuario": 5
  // "pathDestino": "/docs/xml/cxp/clave.xml"  ← opcional, el backend lo calcula
}

// Response 200 → objeto DocumentoCxp actualizado
{
  "id": 101,
  "claveAcceso": "2406202401...",
  "estadoDocumento": 2,
  "pathXml": "/docs/xml/cxp/2406202401....xml",
  "fechaCargaXml": "2026-07-19T10:30:00",
  ...
}
```

---

### FASE 3 — Registrar en tablas CXP
**`POST /carga-documentos/registrarBD/{idDocumentoCxp}`**

> **Antes:** usaba `{idDetalle}`  
> **Ahora:** usa `{idDocumentoCxp}`

```json
// Request
{
  "idEmpresa": 1,
  "idUsuario": 5
}

// Response 200 - éxito
{
  "idDocumentoBD": 234,
  "tipoTablaDestino": "FACTURA_COMPRA",
  "mensaje": "FacturaCompra registrada con id=234",
  "requiereProductos": false
}

// Response 200 - requiere asignar productos (solo Facturas)
{
  "requiereProductos": true,
  "mensaje": "Productos del XML no existen. Asigne un grupo y llame /crearProductosYRegistrar",
  "idDocumentoCxp": 101,
  "productosNuevos": [
    {
      "nombre": "Milhojas",
      "codigo": "4001",
      "codigoAux": "",
      "precioUnitario": 3.04
    }
  ]
}

// Response 200 - proveedor no encontrado
{
  "error": "TITULAR_NO_ENCONTRADO",
  "mensaje": "El emisor con RUC 1234567890 no existe en TSR. Créelo como Proveedor.",
  "rucEmisor": "1234567890001"
}
```

---

### FASE 3b — Crear productos faltantes y registrar
**`POST /carga-documentos/crearProductosYRegistrar/{idDocumentoCxp}`**

> **Antes:** usaba `{idDetalle}`  
> **Ahora:** usa `{idDocumentoCxp}`

```json
// Request
{
  "idEmpresa": 1,
  "idUsuario": 5,
  "productosConGrupo": [
    {
      "nombre": "Milhojas",
      "codigo": "4001",
      "codigoAux": "",
      "precioUnitario": 3.04,
      "idGrupo": 12
    }
  ]
}

// Response 200 → igual que registrarBD exitoso
```

---

### FASE 4 — Resolver novedad
**`POST /carga-documentos/resolverNovedad/{idDocumentoCxp}`**

> **Antes:** usaba `{idDetalle}`  
> **Ahora:** usa `{idDocumentoCxp}` (solo disponible cuando `estadoDocumento = 5`)

```json
// Request - mantener documento previo sin cambios
{
  "accion": "MANTENER",
  "idUsuario": 5
}

// Request - reemplazar con nuevos valores y nuevo XML
{
  "accion": "REEMPLAZAR",
  "contenidoXml": "<?xml version=\"1.0\"...>",
  "idUsuario": 5
  // "pathDestino": "..."  ← opcional
}

// Response 200 - MANTENER
{
  "accion": "MANTENIDO",
  "mensaje": "Se mantiene el documento sin cambios."
}

// Response 200 - REEMPLAZAR
{
  "accion": "REEMPLAZADO",
  "idDocumentoBD": 235,
  "tipoTablaDestino": "FACTURA_COMPRA",
  "mensaje": "FacturaCompra registrada con id=235"
}
```

---

### FASE 5 — Revertir documento
**`POST /carga-documentos/revertir/{idDocumentoCxp}`**

> **Antes:** usaba `{idDetalle}`  
> **Ahora:** usa `{idDocumentoCxp}` (solo disponible cuando `estadoDocumento = 3`)

```json
// Request
{
  "idUsuario": 5
}

// Response 200
{
  "mensaje": "Documento revertido correctamente.",
  "idDocumentoCxp": 101,
  "idDocumentoBD": 234,
  "tipoTablaDestino": "FACTURA_COMPRA"
}
```

---

### CONSULTAS

#### Resumen de una carga (sin cambios en URL)
**`GET /carga-documentos/resumen/{idCargaTxt}`**

```json
// Response 200
{
  "cabecera": {
    "id": 10,
    "nombreArchivo": "1793228946001_Recibidos.txt",
    "totalRegistros": 45,
    "registrosNuevos": 38,
    "registrosDuplicados": 5,
    "registrosNovedad": 2
  },
  "lineas": [
    {
      "id": 201,
      "cargaTxt": { "id": 10 },
      "documento": { "id": 101, "claveAcceso": "...", "estadoDocumento": 1, ... },
      "valorSinImpuestosCarga": 89.29,
      "ivaCarga": 10.71,
      "importeTotalCarga": 100.00,
      "resultado": "NUEVO"
    }
  ]
}
```

#### Consultar un documento específico *(endpoint nuevo)*
**`GET /carga-documentos/documento/{idDocumentoCxp}`**

```json
// Response 200 → objeto DocumentoCxp completo
{
  "id": 101,
  "claveAcceso": "2406202401...",
  "rucEmisor": "1790016919001",
  "razonSocialEmisor": "PROVEEDOR SA",
  "tipoComprobante": "Factura",
  "serieComprobante": "001-001-000000123",
  "estadoDocumento": 3,
  "pathXml": "/docs/xml/cxp/2406202401....xml",
  "idDocumentoBD": 234,
  "tipoTablaDestino": "FACTURA_COMPRA",
  "fechaRegistroBD": "2026-07-19T11:00:00"
}
```

#### Novedades pendientes de una empresa
**`GET /carga-documentos/novedades/{idEmpresa}`**

> **Antes:** retornaba `List<DetalleCargaTxt>`  
> **Ahora:** retorna `List<DocumentoCxp>`

```json
// Response 200 → lista de DocumentoCxp con estadoDocumento=5 y estadoNovedad=1
[
  {
    "id": 72,
    "claveAcceso": "...",
    "estadoDocumento": 5,
    "estadoNovedad": 1,
    "novedad": "importeTotal: previo=100.00 nuevo=115.00",
    ...
  }
]
```

#### Grupos de productos disponibles (sin cambios)
**`GET /carga-documentos/gruposProducto`**

---

## 4. Resumen de cambios en URLs

| Fase | URL anterior | URL nueva | Cambio |
|---|---|---|---|
| 1 - Cargar TXT | `POST /cargarTxt` | `POST /cargarTxt` | Sin cambio |
| 2 - Cargar XML | `POST /cargarXml/{idDetalle}` | `POST /cargarXml/{idDocumentoCxp}` | ID cambia |
| 3 - Registrar BD | `POST /registrarBD/{idDetalle}` | `POST /registrarBD/{idDocumentoCxp}` | ID cambia |
| 3b - Crear productos | `POST /crearProductosYRegistrar/{idDetalle}` | `POST /crearProductosYRegistrar/{idDocumentoCxp}` | ID cambia |
| 4 - Resolver novedad | `POST /resolverNovedad/{idDetalle}` | `POST /resolverNovedad/{idDocumentoCxp}` | ID cambia |
| 5 - Revertir | `POST /revertir/{idDetalle}` | `POST /revertir/{idDocumentoCxp}` | ID cambia |
| Resumen carga | `GET /resumen/{idCargaTxt}` | `GET /resumen/{idCargaTxt}` | Sin cambio |
| Novedades | `GET /novedades/{idEmpresa}` | `GET /novedades/{idEmpresa}` | Tipo de respuesta cambia |
| Documento | — | `GET /documento/{idDocumentoCxp}` | **Endpoint nuevo** |

---

## 5. Flujo de pantallas recomendado para el frontend

```
┌─────────────────────────────────────────────────────────────────┐
│ PANTALLA 1: Cargar archivo TXT                                  │
│  → POST /cargarTxt                                              │
│  → Mostrar tabla de resultados con:                             │
│     - serie, claveAcceso, resultado (NUEVO/DUPLICADO/NOVEDAD)   │
│     - Para cada fila: guardar idDocumentoCxp                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ PANTALLA 2: Lista de documentos (filtrar por estadoDocumento)   │
│  → GET /resumen/{idCargaTxt}  para ver líneas de una carga      │
│  → GET /novedades/{idEmpresa} para ver novedades pendientes     │
│                                                                 │
│  Estado 1 (LEIDO)        → botón "Subir XML"                   │
│  Estado 2 (XML_CARGADO)  → botón "Registrar en BD"             │
│  Estado 3 (REGISTRADO)   → botón "Revertir"                    │
│  Estado 4 (ERROR)        → mostrar mensaje de error            │
│  Estado 5 (NOVEDAD)      → botón "Resolver novedad"            │
│  Estado 6 (REVERTIDO)    → botón "Subir XML" nuevamente        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ PANTALLA 3: Subir XML                                           │
│  → POST /cargarXml/{idDocumentoCxp}                             │
│  → Selector de archivo .xml                                     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ PANTALLA 4: Registrar en BD                                     │
│  → POST /registrarBD/{idDocumentoCxp}                           │
│  → Si responde requiereProductos=true:                          │
│     - Mostrar lista de productos sin grupo                      │
│     - GET /gruposProducto → selector por cada producto          │
│     - POST /crearProductosYRegistrar/{idDocumentoCxp}           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ PANTALLA 5: Resolver novedad (estadoDocumento=5)                │
│  → Mostrar: novedad (diferencias detectadas)                    │
│  → Opción A: MANTENER → POST /resolverNovedad/{id}             │
│              { "accion": "MANTENER", "idUsuario": X }           │
│  → Opción B: REEMPLAZAR → subir nuevo XML +                     │
│              POST /resolverNovedad/{id}                         │
│              { "accion": "REEMPLAZAR", "contenidoXml": "...",   │
│                "idUsuario": X }                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. Punto clave para el frontend

> **El `idDocumentoCxp` que retorna la Fase 1 en el array `detalles[].idDocumentoCxp` es el ID permanente del documento.** Debe almacenarse y usarse para todas las acciones posteriores sobre ese documento.
>
> Un mismo documento puede aparecer en múltiples cargas con el **mismo** `idDocumentoCxp`. Esto es correcto y esperado.
>
> Si el mismo documento aparece dos veces en cargas distintas sin diferencias, `resultado = DUPLICADO` pero el `idDocumentoCxp` apunta al mismo registro existente.

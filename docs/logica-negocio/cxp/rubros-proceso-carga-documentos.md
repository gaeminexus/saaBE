# Rubros — Proceso de Carga de Documentos CXP

> **Documento para el frontend.**  
> Describe los rubros que maneja el proceso de carga de documentos SRI en el módulo CXP.  
> Última revisión: 2026-07-25

---

## Resumen de rubros

| Código alterno | Nombre constante Java         | Propósito |
|---|---|---|
| **174** | `Rubros.CXP_RESULTADO_CARGA_TXT`  | Resultado de cada línea al procesar el TXT |
| **175** | `Rubros.CXP_ESTADO_DOCUMENTO_CXP` | Ciclo de vida del documento (campo de solo lectura) |
| **176** | `Rubros.CXP_ESTADO_NOVEDAD`       | Estado de resolución de una novedad (campo de solo lectura) |
| **177** | `Rubros.CXP_ACCION_NOVEDAD`       | Acción que **envía el frontend** para resolver una novedad |

---

## Rubro 174 — `CXP_RESULTADO_CARGA_TXT`

**¿Dónde aparece?** En cada elemento del array `detalles[]` y `desaparecidosDetalle[]` de la respuesta de `POST /carga-documentos/cargarTxt`.  
**Tipo en BD:** `VARCHAR2(20)` — campo `DCTX.DCTXRSLT`.  
**El frontend lo recibe, nunca lo envía.**

| `PDTRVLRN` | `PDTRVLRV` (String en JSON) | Significado |
|---|---|---|
| 1 | `"NUEVO"` | Primera vez que el documento aparece en el sistema. Queda en estado **LEIDO**, esperando XML. |
| 2 | `"DUPLICADO"` | El documento ya existía y los valores (montos, fechas) son idénticos. No se modifica nada. |
| 3 | `"NOVEDAD"` | El documento ya existía pero con diferencias en montos o fechas. Ver campo `diferencias` en la respuesta. |
| 4 | `"IGNORADO"` | El RUC receptor de la línea no coincide con el RUC de la empresa. La línea se descarta. |
| 5 | `"DESAPARECIDO"` | Documento activo del período que **no apareció** en esta nueva carga. Solo se genera si se envía `idPeriodo`. |

**Ejemplo en la respuesta del TXT:**
```json
{
  "detalles": [
    { "linea": 1, "serie": "001-001-000000123", "resultado": "NUEVO",      "idDocumentoCxp": 101 },
    { "linea": 2, "serie": "001-001-000000050", "resultado": "DUPLICADO",  "idDocumentoCxp": 55  },
    { "linea": 3, "serie": "001-001-000000099", "resultado": "NOVEDAD",    "idDocumentoCxp": 72,
      "diferencias": "importeTotal: previo=100.00 nuevo=115.00" }
  ],
  "desaparecidosDetalle": [
    { "serie": "001-001-000000080", "resultado": "DESAPARECIDO", "idDocumentoCxp": 60 }
  ]
}
```

---

## Rubro 175 — `CXP_ESTADO_DOCUMENTO_CXP`

**¿Dónde aparece?** En el campo `estadoDocumento` del objeto `DocumentoCxp` (tabla `DCXP`).  
**Tipo en BD:** `NUMBER(2)` — campo `DCXP.DCXPESTD`.  
**El frontend lo recibe como número. Nunca lo envía.**

| Valor numérico | Nombre | Descripción | Botón que muestra el frontend |
|---|---|---|---|
| `1` | `LEIDO` | Leído del TXT. Pendiente de cargar XML. | **"Cargar XML"** |
| `2` | `XML_CARGADO` | XML guardado. Estado transitorio — con el flujo unificado no debería verse. | — |
| `3` | `REGISTRADO_BD` | Registrado en las tablas CXP. Puede tener productos pendientes de clasificar. | **"Revertir"** |
| `4` | `ERROR` | Falló algún paso. Ver campo `observacion`. | **"Reintentar"** |
| `5` | `NOVEDAD` | Valores distintos detectados o el documento desapareció de la nueva carga. | **"Resolver novedad"** |
| `6` | `REVERTIDO` | Registros de BD eliminados. | **"Cargar XML"** nuevamente |

> ⚠️ El estado `2 (XML_CARGADO)` es transitorio. Usando el endpoint recomendado `POST /procesarXml/{id}` el documento pasa directamente de `1 → 3`. Solo queda en `2` si se usan los endpoints legacy separados.

**Lógica de presentación sugerida:**
```
estadoDocumento == 1 || estadoDocumento == 6  →  mostrar botón "Cargar XML"
estadoDocumento == 3                          →  mostrar botón "Revertir" + verificar productosPendientes
estadoDocumento == 4                          →  mostrar mensaje de error + botón "Reintentar"
estadoDocumento == 5                          →  mostrar botón "Resolver novedad"
```

---

## Rubro 176 — `CXP_ESTADO_NOVEDAD`

**¿Dónde aparece?** En el campo `estadoNovedad` del objeto `DocumentoCxp`.  
**Tipo en BD:** `NUMBER(2)` — campo `DCXP.DCXPENVD`.  
**Solo tiene valor cuando `estadoDocumento = 5 (NOVEDAD)`. El frontend lo recibe, nunca lo envía.**

| Valor numérico | Nombre | Descripción |
|---|---|---|
| `1` | `PENDIENTE` | Novedad detectada, sin resolución aún. El usuario debe actuar. |
| `2` | `REEMPLAZADO` | El usuario eligió subir un nuevo XML y el documento fue re-registrado. |
| `3` | `MANTENIDO` | El usuario eligió conservar el registro anterior sin cambios. |

> El frontend solo debe preocuparse por `estadoNovedad == 1` para mostrar la alerta al usuario. Los valores `2` y `3` son estados finales informativos.

---

## Rubro 177 — `CXP_ACCION_NOVEDAD`

**¿Dónde se usa?** En el body JSON de `POST /carga-documentos/resolverNovedad/{idDocumentoCxp}`.  
**El frontend lo envía. El backend lo recibe y ejecuta la acción.**

| `PDTRVLRV` (String a enviar) | Descripción | Campos adicionales requeridos en el body |
|---|---|---|
| `"MANTENER"` | Conservar el documento previo sin ningún cambio. | Ninguno |
| `"REEMPLAZAR"` | Revertir el registro anterior y procesar el nuevo XML enviado. | `"contenidoXml"` (obligatorio) |

**Body JSON — acción MANTENER:**
```json
{
  "accion": "MANTENER",
  "idUsuario": 5
}
```

**Body JSON — acción REEMPLAZAR:**
```json
{
  "accion": "REEMPLAZAR",
  "contenidoXml": "<?xml version=\"1.0\"...>",
  "idUsuario": 5
}
```

**Respuesta — MANTENER:**
```json
{
  "accion": "MANTENER",
  "mensaje": "Se mantiene el documento sin cambios."
}
```

**Respuesta — REEMPLAZAR (éxito):**
```json
{
  "accion": "REEMPLAZAR",
  "valido": true,
  "idDocumentoBD": 235,
  "tipoTablaDestino": "FACTURA_COMPRA",
  "mensaje": "FacturaCompra registrada con id=235.",
  "productosPendientes": []
}
```

> El valor del campo `"accion"` es **case-insensitive** en el backend (`MANTENER`, `mantener`, `Mantener` son equivalentes), pero se recomienda enviarlo siempre en **MAYÚSCULAS** tal como se define aquí.

---

## Consulta de rubros desde el backend

Para cargar los rubros en el frontend (listas desplegables, etiquetas de estado, etc.) se puede usar el endpoint genérico de rubros:

```
GET /pdtr/getByRubro/{codigoAlterno}
```

| Llamada | Devuelve |
|---|---|
| `GET /pdtr/getByRubro/174` | Resultados posibles de la carga TXT |
| `GET /pdtr/getByRubro/175` | Estados del documento CXP |
| `GET /pdtr/getByRubro/176` | Estados de resolución de novedad |
| `GET /pdtr/getByRubro/177` | Acciones disponibles para resolver novedad |

Cada elemento retorna:
```json
{
  "codigo": 301,
  "descripcion": "Documento nuevo: primera vez que aparece en el sistema",
  "valorNumerico": 1,
  "valorAlfanumerico": "NUEVO",
  "codigoAlterno": 1,
  "estado": 1
}
```

---

## Cambios en los archivos Java

### Nuevas interfaces en `com.saa.rubros`

| Archivo | Rubro | Uso |
|---|---|---|
| `ResultadoCargaTxt.java` | 174 | Constantes `String` para el campo `resultado` de `DetalleCargaTxt` |
| `EstadoDocumentoCxp.java` | 175 | Constantes `long` para `estadoDocumento` de `DocumentoCxp` |
| `EstadoNovedad.java` | 176 | Constantes `long` para `estadoNovedad` de `DocumentoCxp` |
| `AccionNovedad.java` | 177 | Constantes `String` para la acción en `resolverNovedad` |

### `Rubros.java` — constantes añadidas
```java
int CXP_RESULTADO_CARGA_TXT  = 174;
int CXP_ESTADO_DOCUMENTO_CXP = 175;
int CXP_ESTADO_NOVEDAD       = 176;
int CXP_ACCION_NOVEDAD       = 177;
```

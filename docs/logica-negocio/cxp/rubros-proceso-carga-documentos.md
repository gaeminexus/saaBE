# Rubros — Proceso de Carga de Documentos CXP

> **Documento para el frontend.**  
> Última revisión: 2026-07-27

---

## Resumen de rubros

| Código alterno | Nombre constante Java | Propósito |
|---|---|---|
| **174** | `Rubros.CXP_RESULTADO_CARGA_TXT` | Resultado de cada línea al procesar el TXT |
| **175** | `Rubros.CXP_ESTADO_DOCUMENTO_CXP` | Ciclo de vida del documento (solo lectura) |
| **176** | `Rubros.CXP_ESTADO_NOVEDAD` | Estado de resolución de una novedad (solo lectura) |
| **177** | `Rubros.CXP_ACCION_NOVEDAD` | Acción que **envía el frontend** para resolver una novedad |

---

## Rubro 174 — `CXP_RESULTADO_CARGA_TXT`

**¿Dónde aparece?** En cada elemento del array `detalles[]` y `desaparecidosDetalle[]` de la respuesta de `POST /carga-documentos/cargarTxt`.  
**Tipo en BD:** `NUMBER` — campo `DCTX.DCTXRSLT`.  
**El frontend lo recibe, nunca lo envía.**

| Valor numérico | Nombre | Descripción | Requiere acción del usuario |
|---|---|---|---|
| `1` | `NUEVO` | Primera vez que el documento aparece. Queda en estado LEIDO, esperando XML. | Sí — cargar XML |
| `2` | `DUPLICADO` | El documento ya existía con valores idénticos. No se modifica nada. | No |
| `3` | `NOVEDAD` | El documento ya existía pero con diferencias en montos o fechas. Ver campo `diferencias`. | Sí — resolver novedad |
| `4` | `IGNORADO` | El RUC receptor no coincide con la empresa. Línea descartada. | No |
| `5` | `DESAPARECIDO` | Documento pendiente de procesar que **no apareció** en esta nueva carga. | Sí — resolver novedad |
| `6` | `REGISTRADO_CON_DIFERENCIAS` | Ya registrado en BD con asiento contable, pero el SRI reporta valores diferentes. **Solo informativo.** | No — solo revisar |
| `7` | `REGISTRADO_DESAPARECIDO` | Ya registrado en BD con asiento contable, pero no apareció en esta carga. **Solo informativo.** | No — solo revisar |

> **Distinción clave para el frontend:**
> - Códigos **1–5** → el documento **aún no está completamente procesado** o requiere atención
> - Códigos **6–7** → el documento **ya tiene registro en BD y asiento contable**, no se toca, son solo informativos

**Ejemplo en la respuesta del TXT:**
```json
{
  "nuevos": 5,
  "duplicados": 8,
  "novedades": 2,
  "registradosConDiferencias": 1,
  "desaparecidos": 1,
  "detalles": [
    { "linea": 1, "serie": "001-001-000000123", "resultado": 1, "idDocumentoCxp": 101 },
    { "linea": 2, "serie": "001-001-000000050", "resultado": 2, "idDocumentoCxp": 55  },
    { "linea": 3, "serie": "001-001-000000099", "resultado": 3, "idDocumentoCxp": 72,
      "diferencias": "importeTotal: previo=100.00 nuevo=115.00" },
    { "linea": 4, "serie": "001-001-000000088", "resultado": 6, "idDocumentoCxp": 88,
      "diferencias": "valorSinImpuestos: previo=2500.00 nuevo=2600.00" }
  ],
  "desaparecidosDetalle": [
    { "serie": "001-001-000000080", "resultado": 5, "idDocumentoCxp": 60, "novedad": "DESAPARECIDO_EN_CARGA..." },
    { "serie": "001-001-000000070", "resultado": 7, "idDocumentoCxp": 55, "novedad": "REGISTRADO_DESAPARECIDO..." }
  ]
}
```

---

## Rubro 175 — `CXP_ESTADO_DOCUMENTO_CXP`

**¿Dónde aparece?** En el campo `estadoDocumento` del objeto `DocumentoCxp`.  
**Tipo en BD:** `NUMBER(2)` — campo `DCXP.DCXPESTD`.  
**El frontend lo recibe como número. Nunca lo envía.**

| Valor | Nombre | Descripción | Botón que muestra el frontend |
|---|---|---|---|
| `1` | `LEIDO` | Leído del TXT. Pendiente de cargar XML. | **"Cargar XML"** |
| `2` | `XML_CARGADO` | XML guardado. Estado transitorio interno. | — |
| `3` | `REGISTRADO_BD` | Registrado en tablas CXP + asiento contable generado. | **"Revertir"** |
| `4` | `ERROR` | Falló algún paso. Ver campo `observacion`. | **"Reintentar"** |
| `5` | `NOVEDAD` | Valores distintos o documento desaparecido en nueva carga. | **"Resolver novedad"** |
| `6` | `REVERTIDO` | BD revertida y asiento anulado. | **"Cargar XML"** nuevamente |

**Lógica de presentación sugerida:**
```
estadoDocumento == 1 || estadoDocumento == 6  →  botón "Cargar XML"
estadoDocumento == 3                          →  botón "Revertir"
estadoDocumento == 4                          →  mostrar observacion + botón "Reintentar"
estadoDocumento == 5                          →  botón "Resolver novedad"
```

---

## Rubro 176 — `CXP_ESTADO_NOVEDAD`

**¿Dónde aparece?** En el campo `estadoNovedad` del objeto `DocumentoCxp`. Solo tiene valor cuando `estadoDocumento = 5`.  
**El frontend lo recibe, nunca lo envía.**

| Valor | Nombre | Descripción |
|---|---|---|
| `1` | `PENDIENTE` | Novedad detectada, sin resolución. El usuario debe actuar. |
| `2` | `REEMPLAZADO` | El usuario eligió subir un nuevo XML. |
| `3` | `MANTENIDO` | El usuario conservó el registro anterior. |

---

## Rubro 177 — `CXP_ACCION_NOVEDAD`

**¿Dónde se usa?** En el body de `POST /carga-documentos/resolverNovedad/{id}`.  
**El frontend lo envía como número entero.**

| Valor numérico | Nombre | Descripción | Campos adicionales requeridos |
|---|---|---|---|
| `1` | `MANTENER` | Conservar el documento previo sin cambios. | Ninguno |
| `2` | `REEMPLAZAR` | Revertir el registro anterior y procesar el nuevo XML. | `"contenidoXml"` (obligatorio) |

**Body JSON — MANTENER:**
```json
{ "accion": 1, "idUsuario": 5 }
```

**Body JSON — REEMPLAZAR:**
```json
{ "accion": 2, "contenidoXml": "<?xml ...", "idUsuario": 5 }
```

---

## Script SQL — Insertar rubros 174 en `SCP.PDTR`

> ⚠️ Ejecutar este script para agregar los nuevos códigos **6** y **7** al rubro 174.  
> Ajustar el valor de `PDTRRBRR` al `PRBRCDGO` real del rubro 174 en tu base de datos.

```sql
-- =============================================================================
-- SCRIPT: Agregar valores 6 y 7 al Rubro 174 — CXP_RESULTADO_CARGA_TXT
-- Tabla:  SCP.PDTR
-- Fecha:  2026-07-27
-- Nota:   Reemplazar :ID_RUBRO_174 con el PRBRCDGO real del rubro 174
--         Reemplazar :NEXT_ID con el siguiente valor de la secuencia de PDTR
-- =============================================================================

-- Verificar el PRBRCDGO del rubro 174 antes de ejecutar:
-- SELECT PRBRCDGO, PRBRDSCR, PRBRALTR FROM SCP.PRBR WHERE PRBRALTR = 174;

-- Código 6: REGISTRADO_CON_DIFERENCIAS
INSERT INTO SCP.PDTR (PDTRCDGO, PDTRRBRR, PDTRVLRN, PDTRVLRV, PDTRNMBR)
VALUES (SCP.SQ_PDTRCDGO.NEXTVAL, :ID_RUBRO_174, 6, 'REGISTRADO_CON_DIFERENCIAS',
        'Registrado - Diferencias');

-- Código 7: REGISTRADO_DESAPARECIDO
INSERT INTO SCP.PDTR (PDTRCDGO, PDTRRBRR, PDTRVLRN, PDTRVLRV, PDTRNMBR)
VALUES (SCP.SQ_PDTRCDGO.NEXTVAL, :ID_RUBRO_174, 7, 'REGISTRADO_DESAPARECIDO',
        'Registrado - No Aparece');

COMMIT;
```

> Si la tabla `SCP.PDTR` no tiene secuencia o usa otro mecanismo de PK, ajustar según corresponda. Si desconoces los nombres exactos de columnas, ejecuta primero:
> ```sql
> SELECT COLUMN_NAME, DATA_TYPE FROM ALL_TAB_COLUMNS WHERE TABLE_NAME = 'PDTR' AND OWNER = 'SCP' ORDER BY COLUMN_ID;
> ```
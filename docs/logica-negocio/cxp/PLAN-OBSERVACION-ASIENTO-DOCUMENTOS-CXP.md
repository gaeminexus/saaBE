# PLAN — Observación del asiento: nota de venta y observación adicional al registrar un XML

**Equipo:** `omen-saa-2` · **Creado:** 2026-09-15 · **Estado:** diseño en disco, despachable tras el `e2-48`
**Contrato:** `cxp/API-OBSERVACION-ADICIONAL-REGISTRO-CXP.md` (espejado a `saaFE/docs/cxp/`)
**DDL:** `cxp/sql/e2-48-observacion-adicional-documento-cxp.sql`

---

## 0. Los pedidos, textuales (2026-09-15)

> **A.** *«Al generar notas de venta, la observación del asiento no está incluyendo la misma descripción que
> cuando se emiten facturas. Incluir la misma información que cuando se emiten facturas.»*
>
> **B.** *«Luego de cargar el xml de un documento desde cxp, antes de procesarlo, se desea que exista un campo
> de observación que se habilitará bajo demanda, puede ser con un check antes de registrar que diga incluir
> observación adicional, y que esta se almacene en el documento y se incluya en la observación del asiento.
> Esta observación debe tener hasta 500 caracteres.»*

---

## 1. Medido contra el código

### 1.1 A — qué dice hoy cada asiento

| Documento | Dónde | Observación que arma |
|---|---|---|
| Factura de compra cargada por XML | `ProcesoCargaDocumentosServiceImpl.generarAsientoCxp` `:4531` y `:4579` | `Factura compra: {serie} \| Proveedor: {razón social del emisor}` |
| NC / ND / liquidación cargadas | mismo método, `:4589/4599/4609` | `NC compra: …` / `ND compra: …` / `Liquidación compra: …` + la misma base |
| Factura de reembolso | `contabilizarReembolso` `:2068` | `Factura reembolso: {número} \| Proveedor: {nombre}` |
| Factura recontabilizada | `recontabilizarDocumento` `:1085` | `Factura compra (recontabilizada): {número}` |
| **Nota de venta manual** | `FacturaCompraServiceImpl` `:514` | **`Nota de venta compra: {número}`** — sin proveedor |

La nota de venta es una compra y usa el **mismo generador** que la factura de compra
(`generarAsientoFacturaCompra`), así que «la misma información» es la de la factura de compra: el
**proveedor**. (La factura de *venta* de cxc arma `Factura N° … | Cliente: … | {observación}`,
`FacturaServiceImpl:888`: otro documento, otro rol.)

**Decisión técnica del árbitro, anotada:** la nota de venta lleva además su propia `observacion` si la
tiene. Es el mismo principio que pide el pedido B (lo que el usuario escribe llega al asiento), y la
columna ya existe y se llena desde el formulario manual.

### 1.2 B — el camino del registro

- **Pantalla:** `cxp/forms/procesos/gestion-documentos`, botón Registrar → `RegistrarDocumentoDialogComponent`
  (ya tiene una casilla bajo demanda: «Factura de intermediario», `registrar-documento-dialog.component.html:8-10`).
  **Ese diálogo es el lugar y ese patrón es el que se copia.**
- **Endpoint:** `POST /rest/carga-documentos/registrarBD/{idDocumentoCxp}` lee el cuerpo como `Map`
  (`ProcesoCargaDocumentosRest:279-317`) → `registrarDocumentoBD(id, empresa, usuario, esIntermediario, idProducto)`.
- **El asiento NO siempre se genera en esa llamada.** Si faltan productos por clasificar, el registro se
  corta y se completa después por `crearProductosYRegistrar` (`:1238`, llama a la versión de 3 parámetros);
  una factura de reembolso se contabiliza aparte (`contabilizarReembolso`); y una contabilidad anulada se
  regenera por `recontabilizarDocumento`. **Por eso la observación tiene que quedar guardada al registrar y
  leerse al contabilizar**, no viajar como parámetro hasta el asiento.

### 1.3 B — dónde se guarda: `PGS.DCXP`, columna nueva

| Opción | Por qué no / por qué sí |
|---|---|
| `FCTC/NTCC/NTDC/LQCC.OBSERVACION` (2000, ya existen) | ❌ En NC y ND la carga guarda ahí el `motivo` del XML (`:2712`, `:2868`). Y cuando el registro se corta por productos pendientes **el documento de destino todavía no existe**: no hay dónde guardarla |
| `PGS.DCXP.DCXPOBSR` (ya existe) | ❌ Es la observación **del sistema**: la pisa y la borra el propio proceso (`:628-652`, `:860`, `:1037`, `:1102`, `:1133`…) |
| **`PGS.DCXP.DCXPOBAD VARCHAR2(500 CHAR)`, nueva** | ✅ Existe desde que se cargó el XML, es la misma fila para los seis tipos (factura, NC, ND, liquidación, retenciones), y todos los caminos de contabilización ya tienen el `DocumentoCxp` en la mano o lo buscan |

`500 CHAR` y no `500`: con semántica de bytes, 500 letras con tildes no entran.

---

## 2. Reglas del comportamiento

1. **Sólo `POST /registrarBD` escribe la columna**, y escribe exactamente lo que llega: `trim`; vacío o
   ausente → `NULL`; más de 500 caracteres → rechazo con mensaje (no se trunca en silencio).
2. Se escribe **al principio** del registro, después de validar el estado del documento y **antes** de
   cualquier bloqueante. Así, si el registro se corta por productos pendientes, la observación ya quedó
   para cuando se complete.
3. **Ningún otro camino la escribe ni la borra**: las versiones de 3 y 5 parámetros de `registrarDocumentoBD`,
   `crearProductosYRegistrar`, el lote, `contabilizarReembolso`, `recontabilizarDocumento`, `revertir`.
4. **Todo asiento de un documento CXP** agrega ` | {observación adicional}` al final de su observación
   cuando la columna no está vacía: `generarAsientoCxp` (los seis tipos), `contabilizarReembolso` y
   `recontabilizarDocumento`. Una sola función privada para los tres.
5. La observación del asiento (`CNT.ASNT.ASNTOBSR`) es de 2000: si el texto armado pasa de 2000, se corta a
   2000.
6. **Decisión del usuario, 2026-09-15: sólo en Gestión de documentos.** la pantalla `bandeja-electronica` registra con un `confirm()` nativo y sin diálogo
   (`bandeja-electronica.component.ts:231`); **no** tendrá la casilla. Tampoco el registro por lote.

## 3. Reparto

| Ítem | Quién | Qué |
|---|---|---|
| **BE-7** | `omen-saa-2-be` | A: observación del asiento de la nota de venta |
| **BE-8** | `omen-saa-2-be` | B: entidad + REST + servicio + asientos (reglas del §2) |
| **FE-4** | `omen-saa-2-fe` | B: casilla y textarea en `RegistrarDocumentoDialogComponent`, y el campo en el payload |

⚠️ `ProcesoCargaDocumentosServiceImpl` también lo tocó `lap-saa-1` (factura de intermediario). Hoy sin
cambios sin commitear en el árbol; el ejecutor revisa `git status` sobre el archivo antes de editar.

## 4. Despliegue — ORDEN ESTRICTO

**`e2-48` → WAR → FE.** El WAR mapea `DCXPOBAD`: sin la columna, **toda lectura de `DocumentoCxp`** (la
bandeja entera, el registro, la carga) muere con `ORA-00904`. El FE nuevo con WAR viejo es inofensivo: el
REST lee un `Map` e ignora la clave de más.

## 5. Criterio de aceptación

1. Nota de venta nueva con observación «PRUEBA NV»: el asiento dice
   `Nota de venta compra: 001-001-000000123 | Proveedor: {nombre} | PRUEBA NV`.
2. Factura por XML registrada con la casilla y «PRUEBA XML»: `PGS.DCXP.DCXPOBAD = 'PRUEBA XML'` y el asiento
   dice `Factura compra: {serie} | Proveedor: {emisor} | PRUEBA XML`.
3. La misma, sin la casilla: `DCXPOBAD` nulo y el asiento sin el ` | ` final.
4. 501 caracteres: la pantalla no deja escribir el 501; forzado por API, `422 {error}` y nada grabado.

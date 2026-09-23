# PLAN — Seguimiento de cobros: lo que falta, sobre lo que ya existe

**Equipo:** `omen-saa-2` · **Creado:** 2026-09-23 · **Módulo:** `cxc`

**Pedido del usuario, textual:**

> *«Se requiere una pantalla de seguimiento de cobros. Poder consultarlos, ver en qué estado están,
> un reporte del cobro en el que salgan sus observaciones y el asiento contable relacionado con cada
> cobro. Que tenga filtros de búsqueda x fecha, cliente, periodo. O ya existe una opción así?»*

---

## 0. La respuesta a su pregunta: SÍ existe, y cubre más de la mitad

**`cxc/forms/cobros/consulta-cobros`** (CxC → Cobros → Consulta de Cobros), 263 líneas, ya tiene:

| Lo que el usuario pide | ¿Está? |
|---|:--:|
| Consultar los cobros | ✅ |
| Ver en qué estado están | ✅ columna `estado` (Activo / Reversado) |
| Filtro por cliente | ✅ selector de titular |
| Filtro por fecha | ✅ `desde` / `hasta` |
| El asiento contable de cada cobro | ✅ columna `asiento` (número alterno) |
| **Las observaciones** | ❌ **el dato existe y no se muestra** |
| **Un reporte del cobro** | ❌ no hay ninguna plantilla de cobro |
| **Filtro por período** | ❌ hay rango de fechas, no período contable |

Además ya tiene exportar a CSV y anular un cobro con motivo.

> **Por eso este frente AMPLÍA esa pantalla y no crea una nueva** — decisión del usuario, 2026-09-23.
> Este equipo ya dejó un huérfano por duplicar (`titulares` y `titulares-v2`, §43) y nadie supo
> cuál era la vigente.

### 0.1 Dos primos que NO hay que tocar ni confundir

- **`crd/forms/cobros/seguimiento-cobros`** — es de **créditos**, sobre `/rest/cbcr/seguimiento`.
  ⛔ `crd` está fuera del alcance de este equipo.
- **`tsr` → Seguimiento de Pagos** (`API-SEGUIMIENTO-PAGOS.md`, entregado el 2026-09-15) — es el
  **espejo de esto del lado CxP**. No se toca, pero **se mira**: cualquier decisión de forma que ya
  esté tomada allá se copia acá en vez de inventar otra.

## 1. Lo que hay que agregar, y por qué cada cosa es chica

### 1.1 Las observaciones — el dato ya está

`AplicacionPagoCxc.observacion` (`APLCOBSR`, `VARCHAR2(2000)`) existe y se graba. Lo que pasa es que
el listado **no la trae**: `AplicacionPagoCxcServiceImpl.listar` arma cada fila desde una proyección
`Object[]` de 16 columnas (`:1215-1265`) y la observación no está entre ellas.

**No es un problema de la pantalla: es una columna que falta en la proyección.** Se agrega como
columna 16 y se expone en el item. Es el mismo hallazgo que la ficha de documentos de CXP (§49.2):
*el dato está, no se pinta* — con la diferencia de que acá ni siquiera viaja.

> ⚠️ **Y que siga siendo una proyección, no la entidad.** Ese listado **no** devuelve
> `AplicacionPagoCxc` entera, y eso es deliberado: el modelo no tiene un solo `LAZY` y
> `AplicacionPagoCxc` arrastra **12 relaciones**. Devolver la entidad convertiría una pantalla de
> consulta en el problema de rendimiento del §52. **No se cambia la proyección por la entidad.**

### 1.2 El filtro por período

Hoy hay `desde`/`hasta`. El período contable (`CNT.PRDO`) es otra cosa: el usuario piensa en
«el período de agosto», no en dos fechas.

**Se resuelve en el frontend, sin tocar el backend:** un combo de períodos que, al elegir uno,
**rellena `desde` y `hasta`** con sus fechas. El endpoint sigue recibiendo el rango que ya recibe.
Elegir un período y después mover una fecha a mano es válido: gana lo último que se tocó.

### 1.3 El comprobante del cobro (PDF)

**Es lo único realmente nuevo.** En `rep/cxc/` sólo hay RIDE de documentos electrónicos (factura,
liquidación, NC, ND, retención): **no existe ninguna plantilla de cobro**.

**Decisión del usuario:** es un **comprobante de UN cobro**, para imprimir o mandarle al cliente —
no un listado del período.

Qué lleva, que es exactamente lo que el usuario enumeró más lo que hace falta para que se entienda
solo:

- **Encabezado:** empresa, título («Comprobante de cobro»), número del cobro y fecha.
- **Cliente:** identificación y razón social.
- **El cobro:** forma de pago, valor, estado (y si está **Reversado**, que lo diga en la cara).
- **Documento al que se aplicó:** tipo (factura o liquidación) y número.
- **Observaciones** — el campo del §1.1, completo.
- **Asiento contable:** número alterno y fecha.

## 2. Cómo se genera el PDF — el patrón ya existe, se copia

`FacturaServiceImpl:2215` genera el RIDE así:

```java
byte[] pdfBytes = reporteService.generarReporte("cxc", "RPRT_RIDE_FACTURA", p, "PDF");
```

donde `p` es un `Map` de parámetros. **Todo por parámetros, sin query dentro del `.jrxml`.**

⭐ **El comprobante de cobro se hace igual, y eso además esquiva una trampa conocida:** un `.jrxml`
con `SELECT *` se rompe solo el día que alguien hace un `ALTER TABLE` en cualquier tabla del `FROM`
(CLAUDE.md, caso `RPRT_MVMN_APXT`). **Sin query no hay `COLUMN_n` que se desalinee.**

### 2.1 ⛔ Lo que NO se puede olvidar con el `.jasper`

Está en `CLAUDE.md` y este equipo ya lo pagó dos veces:

1. **El `.jasper` no es opcional.** `ReporteServiceImpl:110` busca el `.jasper`; el respaldo al
   `.jrxml` **está muerto** dentro de WildFly. Un reporte con sólo `.jrxml` compila en el IDE, pasa
   la revisión y revienta la primera vez que un usuario lo abre.
2. **Compilar no alcanza:** `compilar-jasper.bat <ruta>` **y** `verificar-fill-jasper.bat <ruta>`,
   los dos, antes de commitear. El compilador **no valida referencias de estilo** — así se cayeron
   `RPRT_CNCL_CNTA` y `RPRT_CNCL_GNRL` en producción.
3. **Sintaxis compacta**, la del `<element kind="...">`. Un `.jrxml` clásico no se puede recompilar
   con lo que trae este repo.

## 3. El reparto

| Ítem | Qué | Dónde |
|---|---|---|
| **BE-1** | `observacion` en la proyección del listado | `AplicacionPagoCxcServiceImpl.listar` |
| **BE-2** | `GET /aplc/comprobante/{id}` → PDF | `AplicacionPagoCxcRest` + service |
| **REP-1** | `rep/cxc/RPRT_COBRO.jrxml` + `.jasper` | compilado **y** con fill verificado |
| **FE-1** | Observaciones en la grilla, filtro por período, botón de imprimir | `consulta-cobros` |

## 4. Criterio de aceptación

1. Buscar cobros de un cliente en un rango y ver, **sin consultar la base**, su estado, su asiento y
   sus observaciones.
2. Elegir un período del combo y que las fechas se completen solas.
3. Imprimir el comprobante de un cobro y que salgan las observaciones y el asiento.
4. Un cobro **reversado** imprime igual, y el PDF **dice que está reversado**.
5. Un cobro **sin** observaciones imprime sin un bloque vacío colgando.
6. El listado sigue respondiendo igual de rápido: **la proyección sigue siendo proyección.**

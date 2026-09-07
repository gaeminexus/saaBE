# API — Administración de la calificación de riesgo (contrato para el frontend)

**Fecha:** 2026-09-07 · **Equipo:** CRD / Equipo B (`eqB`, `omen-saa-1`) · **P22 del tablero (`a470472`)**

> El path de JAX-RS es `/rest`, así que las URLs reales son `/SaaBE/rest/cfcr/...` y
> `/SaaBE/rest/escr/...`.

Plan de fondo: `PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md` (ya implementado: entidades, DAOs y
`CalificacionRiesgoService` existían antes de P22, y `GeneracionG48ServiceImpl` ya los consume).
**Este documento cubre solo lo nuevo de P22: la capa REST de administración** — antes de P22 la
única forma de tocar `CRD.CFCR`/`CRD.ESCR` era correr `sql/177` a mano contra producción.

---

## 0. Lo primero: NO es la misma clasificación que las bandas contables

| | Bandas contables (`/rest/cbpr` + `/rest/bndp`) | Calificación de riesgo (`/rest/cfcr` + `/rest/escr`) |
|---|---|---|
| Para qué | a qué **cuenta contable** va el saldo | cuánta **provisión** regulatoria se constituye (G48) |
| Valores | 1-30, 31-90, 91-180, 181-360, +360 | A1, A2, A3, B1, B2, C1, C2, D, E |
| Varía por | producto, empresa (FK real) Y tipo de cartera (por vencer/vencido) | producto y, opcionalmente, empresa — **sin** tipo de cartera |
| Tabla | `CRD.CBPR` + `CRD.BNDP` | `CRD.CFCR` + `CRD.ESCR` |

**Ningún corte coincide entre las dos.** El molde de REST es el mismo (`cbpr`/`bndp` copiados a
`cfcr`/`escr`) porque resuelven el mismo tipo de problema —una parametrización con vigencia que
antes estaba cableada o en un script—, pero son dominios distintos. No mezclar sus pantallas ni
su lectura.

---

## 0bis. ⛔ La diferencia deliberada con el molde de bandas: `/escr` es SOLO LECTURA

`/rest/bndp` (bandas) expone `POST`/`PUT`/`DELETE` de una banda suelta, con la advertencia de que
la pantalla no debe usarlos para grabar. **`/rest/escr` (calificación de riesgo) NO expone esos
tres verbos, directamente.**

**Por qué la diferencia:** una banda de `BNDP` es una estructura DERIVADA (se acumula por
`periodos`) que no puede quedar en un estado sin sentido con solo agregar o quitar una fila. **Una
calificación de `ESCR` sí puede quedar inválida con un solo cambio** — un `DELETE` de la línea
B1, o un `POST` con un rango que no empalma, deja un hueco entre dos días y una cuota que caiga
ahí no califica, sin ningún error visible hasta que el G48 sale mal. Exponer esos verbos por REST
reintroduciría, con la bendición de un endpoint publicado, exactamente el problema que P22 vino a
resolver (hoy vía `sql/177` a mano). Decisión del árbitro `omen-saa-1-arb`, 2026-09-07.

**Toda escritura de una escala pasa por `POST /cfcr/guardarConfiguracion` (o `/cerrarVigencia`)**,
que valida el conjunto completo antes de grabar una sola fila. Si en el futuro aparece un caso
real que necesite tocar una línea sola, se agrega con la validación del conjunto adentro — no se
abre `/escr` "por las dudas".

---

## 1. `idEmpresa` es OPCIONAL, y es a propósito — distinto de bandas

`CRD.CFCR.PJRQCDGO` es un número de trazabilidad **sin FK** (a diferencia de `CBPR.PJRQCDGO`, que
sí es FK real a `SCP.PJRQ`). **`idEmpresa` ausente/null en cualquier endpoint de este contrato
significa "aplica a cualquier empresa"** — es como está cargada hoy toda la parametrización
(`sql/177`), y es la semántica que usa `GeneracionG48ServiceImpl` al llamar
`CalificacionRiesgoService.resolverEscala(idProducto, null, fecha)`.

⚠️ **La pantalla tiene que poder guardar una configuración SIN empresa**, y mostrar claramente qué
significa (probablemente "todas las empresas" o similar, a definir con UX) — no forzar a elegir
una si el usuario quiere una configuración universal.

**Unicidad, con la misma semántica null-tolerante**: no puede haber dos configuraciones vigentes
del mismo producto si sus vigencias se solapan Y (cualquiera de las dos es universal, o las dos
son de la misma empresa específica). Guardar una configuración universal cuando ya hay una
específica vigente para ese producto (o viceversa) es tratado como conflicto — `selectSolapadas`
en el DAO. Esto cierra hacia adelante una ambigüedad que el propio código de
`selectVigentePorProducto` tenía anotada como pendiente para cuando existiera más de una
configuración por producto.

---

## 2. Sin columnas de auditoría de modificación — no es un olvido

`CRD.CFCR`/`CRD.ESCR` sólo tienen `fechaRegistro`/`usuarioRegistro` (a diferencia de
`CBPR`/`BNDP`, que también tienen `ipRegistro`/`fechaModificacion`/`usuarioModificacion`/
`ipModificacion`). **No se agregó DDL para igualarlas** — decisión del árbitro, 2026-09-07: con el
modelo de vigencia, una configuración no se edita después de empezar a regir — se cierra y se abre
otra, y **cada una nace con su propio `fechaRegistro`/`usuarioRegistro`**. La trazabilidad real la
da la cadena de vigencias completa, no una columna de "última modificación": las columnas de
modificación de bandas sólo cubrirían la ventana en que la vigencia todavía no arrancó, que es
poco.

---

## 3. Endpoints CRUD estándar — `/rest/cfcr`

| Método | Ruta | Devuelve |
|---|---|---|
| GET | `/getAll` | Todas las configuraciones, entidad cruda |
| GET | `/getId/{id}` | Una configuración; 404 si no existe |
| POST | *(raíz)* | Crea SOLO la cabecera — el alta real es `/guardarConfiguracion` |
| PUT | *(raíz)* | Actualiza SOLO la cabecera |
| DELETE | `/{id}` | Elimina; falla si todavía tiene calificaciones (FK) |
| POST | `/selectByCriteria` | Búsqueda dinámica (`List<DatosBusqueda>`) |

## 4. Endpoints CRUD estándar (solo lectura) — `/rest/escr`

| Método | Ruta | Devuelve |
|---|---|---|
| GET | `/getAll` | Todas las calificaciones, entidad cruda |
| GET | `/getId/{id}` | Una calificación; 404 si no existe |
| GET | `/getByConfiguracion/{idConfiguracion}` | Calificaciones de una configuración, con etiqueta del rango |
| POST | `/selectByCriteria` | Búsqueda dinámica |

**Sin POST/PUT/DELETE — ver §0bis.**

---

## 5. `GET /rest/cfcr/vigente`

Configuración vigente de un producto, con su escala completa.

```
GET /SaaBE/rest/cfcr/vigente?idProducto=2&idEmpresa=1236&fecha=2026-09-01
```

| Param | Tipo | Obligatorio |
|---|---|---|
| `idProducto` | number | sí |
| `idEmpresa` | number | no — ausente = trae la configuración universal si existe |
| `fecha` | `yyyy-MM-dd` | no — ausente = hoy |

**Response 200:**

```json
{
  "idConfiguracion": 1,
  "idProducto": 2,
  "nombreProducto": "CREDIPLUS",
  "idEmpresa": null,
  "nombre": "GENERAL",
  "fechaDesde": [1900, 1, 1],
  "fechaHasta": null,
  "editable": false,
  "estado": 1,
  "escalas": [
    { "idEscala": 10, "calificacion": "A1", "diaDesde": 0, "diaHasta": 0, "etiqueta": "0", "porcentajeProvision": 0.0099 },
    { "idEscala": 11, "calificacion": "A2", "diaDesde": 1, "diaHasta": 15, "etiqueta": "1 - 15", "porcentajeProvision": 0.0199 },
    { "idEscala": 18, "calificacion": "E",  "diaDesde": 271, "diaHasta": null, "etiqueta": "mas de 270 (resto)", "porcentajeProvision": 1.0 }
  ]
}
```

**Error** si no hay configuración vigente (`500`, mensaje de negocio): *"No hay configuracion de
calificacion de riesgo vigente al ... para el producto ..."*.

---

## 6. `GET /rest/cfcr/listado`

Todos los productos de crédito (activos e inactivos) con su configuración vigente. Los productos
sin configuración salen igual, con `configuracion: null` — ese hueco es lo que la pantalla tiene
que mostrar (mismo hueco que hoy detecta
`CalificacionRiesgoService.productosSinConfiguracion` para el G48).

```
GET /SaaBE/rest/cfcr/listado?idEmpresa=1236&fecha=2026-09-01
```

`idEmpresa` es opcional (ver §1). `fecha` opcional, ausente = hoy.

**Response 200:** arreglo de

```json
{
  "idProducto": 2, "nombreProducto": "CREDIPLUS", "estadoProducto": 1,
  "configuracion": { "...": "mismo shape de /vigente, o null" }
}
```

---

## 7. `GET /rest/cfcr/historial`

Historial de un producto, vigentes y cerradas, de la más reciente a la más antigua, cada una con
su escala.

```
GET /SaaBE/rest/cfcr/historial?idProducto=2&idEmpresa=1236
```

`idEmpresa` opcional (ver §1). **Response 200:** arreglo del mismo shape de `/vigente`.

---

## 8. `POST /rest/cfcr/guardarConfiguracion`

Graba una configuración COMPLETA — cabecera más escala — en una transacción. Alta si
`idConfiguracion` viene nulo; edición en el lugar si viene, **y solo si la vigencia todavía no
empezó** (si ya empezó: usar `/cerrarVigencia`).

```json
{
  "idConfiguracion": null,
  "idProducto": 2,
  "idEmpresa": null,
  "nombre": "GENERAL",
  "fechaDesde": "2026-09-07",
  "fechaHasta": null,
  "usuario": "jperez",
  "escalas": [
    { "calificacion": "A1", "diaHasta": 0,   "porcentajeProvision": 0.0099 },
    { "calificacion": "A2", "diaHasta": 15,  "porcentajeProvision": 0.0199 },
    { "calificacion": "A3", "diaHasta": 30,  "porcentajeProvision": 0.02 },
    { "calificacion": "B1", "diaHasta": 60,  "porcentajeProvision": 0.05 },
    { "calificacion": "B2", "diaHasta": 90,  "porcentajeProvision": 0.10 },
    { "calificacion": "C1", "diaHasta": 120, "porcentajeProvision": 0.20 },
    { "calificacion": "C2", "diaHasta": 180, "porcentajeProvision": 0.40 },
    { "calificacion": "D",  "diaHasta": 270, "porcentajeProvision": 0.60 },
    { "calificacion": "E",  "diaHasta": null,"porcentajeProvision": 1.0 }
  ]
}
```

### ⛔⛔ `diaDesde` y `orden` NO se envían — el servidor los deriva

**El orden de la lista `escalas` ES el orden de evaluación** (de ahí sale `ESCRORDN`), y
**`diaDesde` de cada línea es siempre `diaHasta` de la anterior más uno, empezando en 0 la
primera.** Decisión del árbitro, 2026-09-07: un dato que el cliente podría mandar desalineado de
`diaHasta` (la fuente de verdad real) no se recibe — así un hueco o un solape no se puede ni
expresar en la solicitud, no hace falta detectarlo después.

⚠️ **Por qué empieza en 0, y es importante**: `GeneracionG48ServiceImpl:257` pone piso en 0 al
calcular días de morosidad — **una cuota al día llega con 0 días** y tiene que caer en la primera
calificación (A1). Si la validación exigiera que la escala empiece en 1, las cuotas sanas se
quedarían sin calificar.

### Validación de la escala — el porqué de P22

Todas las reglas fallan con `IncomeException`, **antes de grabar nada** (el criterio es el mismo
que ya usaba el control D.3 de `sql/177`, aplicado ahora por construcción, no verificado después):

| Regla | Mensaje si falla |
|---|---|
| Al menos una calificación | *"La escala debe tener al menos una calificacion"* |
| Calificación no vacía | *"La calificacion de la linea N es obligatoria"* |
| Sin calificaciones repetidas | *"La calificacion X esta repetida en la escala"* |
| `porcentajeProvision` obligatorio, entre 0 y 1 (tanto por uno) | *"...debe estar entre 0 y 1... revise si se quiso decir 0.XX"* — protege contra tipear `99` en vez de `0.99` |
| `diaHasta` nulo SOLO en la última línea | *"Solo la ULTIMA calificacion... puede quedar sin tope superior"* |
| `diaHasta` de una línea no puede ser menor a su `diaDesde` derivado | *"El dia hasta de X... no puede ser menor al dia desde..."* |
| Vigencia: `fechaDesde` obligatoria, `fechaHasta` no anterior a `fechaDesde` | *"La fecha desde de la vigencia es obligatoria"* / *"...no puede ser anterior..."* |
| Una sola configuración vigente por producto (con la semántica null-tolerante de empresa, §1) | *"Ya existe la configuracion N vigente desde... para [la empresa N \| CUALQUIER empresa] de ese producto"* |

**Response 200:** la configuración grabada, mismo shape que `/vigente`.

---

## 9. `POST /rest/cfcr/cerrarVigencia`

Cambio normativo: cierra la vigencia de la configuración actual (`fechaHasta = fechaDesdeNueva - 1
día`) y abre una nueva desde `fechaDesdeNueva`, con la escala que traiga la solicitud. Producto,
empresa y nombre se heredan de la configuración que se cierra — no se reenvían.

```json
{
  "idConfiguracionVigente": 1,
  "fechaDesdeNueva": "2026-10-01",
  "usuario": "jperez",
  "escalas": [ "... mismo shape que en guardarConfiguracion, sin diaDesde ni orden ..." ]
}
```

**Errores de negocio**: la configuración ya está cerrada; `fechaDesdeNueva` no es posterior a la
`fechaDesde` de la que se cierra; la escala nueva no pasa la validación de §8; quedaría una
tercera configuración solapada (defensivo — no debería poder pasar en el uso normal).

**Response 200:** la configuración NUEVA, mismo shape que `/vigente`.

---

## 10. `GET /rest/cfcr/probar` — endpoint de verificación

Pedido del FE, 2026-09-07: la forma más barata de que el usuario detecte un hueco en la escala es
meter unos días y ver si no cae en ninguna calificación.

```
GET /SaaBE/rest/cfcr/probar?idProducto=2&idEmpresa=1236&dias=31&fecha=2026-09-01
```

**⛔ Usa `CalificacionRiesgoService.calificar` — el MISMO camino que consume
`GeneracionG48ServiceImpl`.** Si este endpoint calificara distinto que el reporte regulatorio real,
sería peor que no tenerlo. No es un endpoint de proceso: ningún proceso contable lo consume, es
para que QA y el frontend verifiquen la parametrización cargada — mismo espíritu que
`GET /cbpr/clasificar` en bandas.

**Response 200:**

```json
{
  "idConfiguracion": 1,
  "idEscala": 12,
  "calificacion": "A3",
  "porcentajeProvision": 0.02,
  "diaDesde": 16,
  "diaHasta": 30
}
```

**Error** si `dias` no cae en ninguna calificación (el hueco que el probador existe para detectar):
*"Ninguna calificación de la escala del producto ... cubre N días de morosidad — la escala tiene un
hueco. Revise CRD.ESCR (sql/177 control D.3)."*

---

## 11. Permisos

**Ninguno del lado del backend — a propósito.** El usuario eligió el mismo control que bandas:
`authGuard` + `usuarioUnoGuard` del lado del frontend (solo el usuario código 38, "USUARIO 1").
**No existe esquema de permisos en el backend** (verificado: no hay entidad de menú, perfil, rol
ni permiso en `com.saa.model`), así que estos endpoints van sin anotaciones de seguridad, igual
que `/rest/cbpr`. El control lo pone exclusivamente el FE.

---

## 12. Fechas

Mismas reglas que el resto del sistema (ver CLAUDE.md §Serialización y `API-BANDAS-PRODUCTO.md`
§0.1): en query params, `yyyy-MM-dd`; en el cuerpo de una solicitud POST, `LocalDate` como
`yyyy-MM-dd`; en toda respuesta, Jackson las emite como arreglo (`[2026,9,7]`).

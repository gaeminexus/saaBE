# PLAN — clasificar las compras por tarifa: 0%, no objeto, exento, 5%, 8%, 15%

**Equipo:** `omen-saa-2` · **Creado:** 2026-09-21 · **Estado:** diseño en disco, **esperando la
decisión de alcance del usuario** (§4)
**Pedido del usuario, textual (2026-09-21):** *«Se había solicitado que en la reportería de facturas
compras esté bien clasificado las compras tarifa 0%, no objeto de IVA, exentas de IVA, tarifa 5%,
tarifa 8%, y tarifa 15%; en la actualidad no está bien la clasificación.»*

---

## 1. Por qué no está bien — y no es que se haya roto

Medido el 2026-09-21. **La clasificación por tarifa nunca se construyó entera.** Lo que hay:

| Bucket | Columna en `PGS.FCTC` | Quién la llena hoy |
|---|---|---|
| Total sin impuestos | `SUBTOTAL` | la carga, desde el XML |
| Tarifa 0% | `SUBCERO` | la carga, desde el detalle (`774dc0e0`, 11-09) |
| **No objeto** | `SUBNOOBJ` | **la crea el `e2-55` de hoy**; antes no existía |
| **Exento** | **no existe** | — |
| **5%** | `SUBTOTAL5` | **nadie**. La columna existe y está vacía |
| **8%** | `SUBTOTAL8` | **nadie**. La columna existe y está vacía |
| 15% (gravada) | no hay columna propia | se **deduce**: `SUBTOTAL − SUBCERO − SUBNOOBJ` |

Y la reportería lo refleja: el dashboard de CXP
(`saaFE/src/app/modules/cxp/forms/reportes/dashboard-cxp/`) tiene exactamente seis columnas —
`subtotal`, `subcero` (rotulada *«Subtotal 0%»*), `vIVA`, `vICE`, `descuento`, `total`— y lee
`d.subcero` directo de la entidad. **No hay ninguna columna por tarifa.**

**El dato sí existe**, y está donde siempre estuvo: `PGS.DFCC.CODIGOIVASRI`, línea por línea, que la
carga graba desde el XML. Lo que falta es **subirlo a la cabecera** y mostrarlo.

> **Por qué esto se venía dando por cerrado:** el 11-09 se arregló el reparto del **0%** y el frente
> se reportó como resuelto. Cubría un bucket de seis. Es el mismo patrón del §46.3 del estado: *lo
> que cerró un caso se anotó como si cerrara la familia.*

## 2. Tabla 17 del SRI, que es el mapa

| `codigoPorcentaje` | Qué es | A dónde va |
|---|---|---|
| `0` | 0% | `SUBCERO` |
| `6` | No objeto de impuesto | `SUBNOOBJ` |
| `7` | Exento de IVA | **columna nueva** |
| `5` | 5% | `SUBTOTAL5` |
| `8` | 8% | `SUBTOTAL8` |
| `4` | 15% | gravada |
| `2` / `3` / `10` | 12% / 14% / 13% (históricos) | gravada |

## 3. Las cinco piezas

| # | Qué | Dónde |
|---|---|---|
| **1** | Medir qué tarifas existen de verdad en los datos | `cxp/sql/e2-59` — **solo lectura, correr primero** |
| **2** | DDL: la columna de exento (y confirmar `SUBTOTAL5`/`SUBTOTAL8`) | un `e2-*` nuevo, después de leer el `e2-59` |
| **3** | La carga reparte **todos** los buckets desde el detalle, no sólo el 0% | `ProcesoCargaDocumentosServiceImpl`, la zona de `base0Detalle` (`:1697` factura, `:2848` NC, `:2979` ND, `:3117` LQCC) |
| **4** | El ATS emite `baseImponible`, `baseNoGraIva`, `baseImpExe` y `baseImpGrav` desde los buckets | `GeneradorAtsServiceImpl` — `baseNoGraIva` ya salió hoy (`efe8003d`); falta `baseImpExe`, hoy en `"0.00"` fijo |
| **5** | La reportería muestra las columnas | `dashboard-cxp` en `saaFE` |

Y aparte, **recalcular lo ya cargado**: los buckets de la cabecera se pueden reconstruir desde el
detalle con exactitud, sin inventar nada. Eso cubre de paso **las facturas mixtas**, que son el mismo
defecto: hoy su `SUBCERO` no tiene la parte al 0% y el ATS declara todo como gravado.

## 4. ⛔ La decisión que falta, y por eso esto no está despachado

**Lo mínimo para el anexo de agosto** y **el frente completo** no son lo mismo:

| | Mínimo para agosto | Completo |
|---|---|---|
| Alcance | recalcular `SUBCERO` y `SUBNOOBJ` de las compras de **agosto** desde el detalle | los seis buckets, en la carga, el ATS, la reportería y **todo el histórico** |
| Toca | datos (un `.sql`) | DDL + carga + ATS + frontend + recálculo histórico |
| Riesgo | bajo y acotado a un mes | cambia números de reportes de meses ya declarados |
| Cuándo | hoy | varios días |

**El mínimo alcanza para declarar agosto bien.** El completo es lo que el usuario pidió, y toca
períodos ya declarados — por eso la decisión es suya, no nuestra.

## 5. Dos cosas que hay que tener a la vista al hacerlo

1. **⚠️ `ReporteCuadreSriServiceImpl` usa `subcero` como «base 0%» del cuadre 104** (levantado por
   `omen-saa-2-be` al entregar el BE-4). Cuando el bomberos pase a `SUBNOOBJ`, esa columna **baja**.
   Si el cuadre compara contra el ATS o contra el formulario, la casilla de no objeto queda sin
   reflejar. **Es otra decisión: ¿el 104 pide el no objeto aparte?**
2. **⚠️ `PGS.FCTC` es territorio compartido con `lap-saa-1`** (ellos agregaron `FCTCESIN`). Las
   columnas nuevas son aditivas, pero **el cambio de significado de `SUBCERO` afecta cualquier
   reporte suyo que lo lea**. Pendiente de autorización del usuario para avisarle a su árbitro.

# El talón del ATS: retenciones en cero y bases mal repartidas

**Equipo:** `omen-saa-2` · **Fecha:** 2026-09-11 · **Encargo:** urgente, del contador de ASOPREP
vía el usuario. Período **08/2026**.

El anexo ya **valida sin errores**. Lo que sigue no lo detecta el validador del SRI: son valores
**mal repartidos o en cero** que sólo se ven contrastando el talón resumen contra las facturas
físicas. Cinco síntomas, **tres causas**.

---

## 1. 🔴 Las retenciones del talón salen en CERO — el ATS lee la tabla equivocada

**Síntoma.** En `RETENCION EN LA FUENTE DE IVA` del talón resumen, las siete filas (10%, 20%, 30%,
50%, 70%, 100%, NC) dan **0.00**, y el `TOTAL` también.

**Causa, medida.** `GeneradorAtsServiceImpl:1032` consulta **`DetalleRetencionCompraV2`**
(`PGS.DRC2`). Las retenciones que ASOPREP **realmente emite** viven en **`CBR.RTV2`/`CBR.DRV2`**
(`RetencionV2`).

Esto ya se había medido el 2026-09-10 al construir el cuadre del 104, y quedó escrito: los dos
`ServiceImpl` no son comparables.

| | `CBR.RTV2` / `DRV2` | `PGS.RCV2` / `DRC2` |
|---|---|---|
| Servicio | `RetencionV2ServiceImpl` — genera el XML 2.0.0, lo firma, lo somete al SRI por SOAP y sigue su estado 1-6 | `RetencionCompraV2ServiceImpl` — **45 líneas**, CRUD genérico (`saveSingle`/`selectAll`/`selectByCriteria`) |
| Ciclo de vida ante el SRI | sí | **ninguno** |

**Sólo el primero representa comprobantes efectivamente emitidos y autorizados**, que es lo único
que un anexo puede declarar.

> **Y hay una contradicción interna que lo confirma:** el **cuadre del 104** (commit `f68d7edd`) suma
> desde `CBR.DRV2` y da **4.642,52** de retención de IVA para el período. El **ATS**, leyendo
> `PGS.DRC2`, declara **0,00** del mismo mes. **Dos reportes del mismo sistema, sobre el mismo
> período, que no pueden ser ciertos a la vez.**

**Era el `e2-38`, escrito el 2026-09-09 y nunca corrido.** Ese script existía justamente para
resolver qué juego de tablas es el vigente. La respuesta llegó igual, por el camino caro.

---

## 2. 🔴 Todas las bases de compra vienen infladas por el monto 0% — doble conteo

**Síntoma.** El reporte de compras y el ATS no coinciden con las facturas físicas de EMPRESA
ELÉCTRICA QUITO. **Medido, las ocho facturas del proveedor `1790053881001`:**

| Física | ATS | Diferencia |
|---|---|---|
| 451,03 | 458,26 | **+7,23** |
| 221,33 | 228,56 | **+7,23** |
| 204,61 | 211,84 | **+7,23** |
| 17,55 | 24,78 | **+7,23** |
| 17,40 | 24,63 | **+7,23** |
| 10,48 | 17,71 | **+7,23** |
| 2,76 | 9,99 | **+7,23** |
| 2,06 | 9,29 | **+7,23** |

**Las ocho, exactamente +7,23** — que es el valor de la columna `Base IVA 0%` de esas mismas filas.

**Causa, medida en el código.** `GeneradorAtsServiceImpl:232` arma la línea de compra así:

```java
nvl(f.getSubtotal(), 0.0),   // → baseImpGrav  (base gravada)
nvl(f.getSubcero(),  0.0),   // → baseImponible (base 0%)
```

Pero `SUBTOTAL` **no es la base gravada**: es `<totalSinImpuestos>` del XML del SRI
(`ProcesoCargaDocumentosServiceImpl:1546`), o sea **el total sin impuestos, que ya incluye la parte
al 0%**. Y para las facturas de servicios básicos el cargador **suma los valores de terceros a las
dos columnas a la vez** (líneas 1674-1675), que es correcto para `subtotal` y es el origen del 7,23:

```java
factura.setSubtotal(nvlDouble(factura.getSubtotal()) + totalTerceros);
factura.setSubcero(nvlDouble(factura.getSubcero())  + totalTerceros);
```

**Entonces el ATS declara `subtotal` como gravada y `subcero` aparte: el 0% se cuenta dos veces.**

**La base gravada correcta es `subtotal − subcero`.**

⚠️ **Alcance: NO es sólo la eléctrica.** Toda compra con cualquier porción al 0% está inflada por
esa porción. Afecta también al reporte de compras y explica el caso de la factura `000133636`, que
el usuario reporta con tarifa 0% y 15% mezcladas.

---

## 3. 🟠 `SUBCERO` sólo se llena con los valores de terceros, nunca con la base 0% real

**Síntoma.** Servicios básicos que **no gravan IVA** aparecen en `BI tarifa diferente 0%` con
`Monto IVA = 0.00` — una base declarada como gravada con impuesto cero, que es una contradicción.

**Causa.** `grep` sobre todo `ProcesoCargaDocumentosServiceImpl`: **el único `setSubcero` de la
factura de compra es el de la línea 1675**, el de bomberos/basura. **La base al 0% que viene en el
propio XML del comprobante nunca se separa**: entra entera en `subtotal` y el ATS la reporta como
gravada.

El dato existe en el XML — `<totalConImpuestos><totalImpuesto>` trae `codigoPorcentaje` por tarifa,
y el cargador ya lo lee en la línea 1601 para resolver el código de IVA. **Lo que falta es repartir
las bases por tarifa, no leer un dato nuevo.**

⛔ **Esto toca la CARGA, no el ATS.** Cambiarlo altera cómo se graban las compras de aquí en
adelante y **no corrige las ya cargadas**: haría falta un recálculo de lo existente. Es la decisión
que el usuario tiene que tomar (§6).

---

## 4. 🟠 La nota de venta se declara como tarifa distinta de 0%

**Síntoma.** En el talón, `NOTA DE VENTA` (1 registro) aparece con **80,00 en `BI tarifa diferente
0%`**. Una nota de venta es de régimen simplificado y **no traslada IVA**: su base va en
`BI tarifa 0%`.

Probablemente es el mismo mecanismo del §3 —la base entra sin repartir— pero **no está medido**, y
un comprobante de otro tipo puede tener otra causa. **Verificar antes de tocar.**

---

## 5. 🟠 El filtro de Consulta de Documentos no permite ver las notas de venta

**Síntoma.** El combo `Tabla de Destino` lista: Todas · Factura Compra · Nota Crédito Compra · Nota
Débito Compra · Liquidación Compra · Retención Compra. **No incluye Nota de Venta.**

El sistema tiene una pantalla *Nota de Venta (Manual)* que las registra, y después **no hay forma de
consultarlas desde el filtro**. Es el patrón que este equipo ya registró varias veces en
septiembre: **la función existe, le falta la puerta.** Frontend, y es el arreglo más barato de esta
lista.

---

## 6. Lo que decide el usuario, no nosotros

| # | Decisión | Por qué no la tomamos |
|---|---|---|
| A | **¿Se recalculan las compras ya cargadas** cuando se arregle el reparto de bases (§3)? | Toca datos históricos ya contabilizados y ya declarados en períodos anteriores |
| B | **¿El ATS de agosto se presenta con los valores corregidos** o se sustituye después? | Depende de si ya se presentó y del criterio del contador |

**Lo que NO es decisión y se corrige igual:** la tabla de retenciones (§1) y el doble conteo (§2).
Los dos son defectos con una única lectura correcta.

---

# ANEXO A — El §3, medido sobre TODO el repositorio (2026-09-11)

El §3 decía que `SUBCERO` sólo se llena con los valores de terceros. Se midió sobre el repositorio
completo y **es peor de lo que decía**: no hay, en ningún servicio, un camino que reparta la base
al 0% leyendo la tarifa del XML.

## A.1 Los tres únicos `setSubcero` del sistema

`grep -rn "\.setSubcero(" src/main/java` → **3 resultados en todo el backend**:

| Archivo:línea | Qué hace | ¿Reparte la base 0% real? |
|---|---|---|
| `ProcesoCargaDocumentosServiceImpl:1675` | `+ totalTerceros` (bomberos/basura) | ❌ sólo terceros |
| `FacturaCompraServiceImpl:444` | Nota de Venta manual: toma `solicitud.getSubcero()` **tal cual del payload** | ❌ sin cómputo: depende de lo que mande el frontend |
| `LiquidacionCompraServiceImpl:814` | `crearDocumentoCxp` copia `subcero` de `cxc.LiquidacionCompra` a `cxp.LiquidacionCompraCompra` | ❌ propagación, no cálculo |

**Ninguno lee `<totalConImpuestos><totalImpuesto codigoPorcentaje=…>`.** El dato está en el XML y el
cargador ya lo lee (línea 1601) para resolver el código de IVA — **lo que falta no es obtener un
dato nuevo, es repartir las bases por tarifa con uno que ya se tiene.**

## A.2 Nota de crédito, nota de débito y liquidación de compra: peor que la factura

`setSubcero` **no aparece ni una vez** para `NotaCreditoCompra`, `NotaDebitoCompra` ni
`LiquidacionCompraCompra`. Tras la carga automática su `subcero` queda **siempre en 0**.

Consecuencia doble:
- El arreglo del doble conteo (`subtotal − subcero`) es **inocuo** para las tres: resta cero. Queda
  puesto igual, correcto para el día en que el campo se llene.
- Pero **toda su base al 0% se declara como gravada**, sin siquiera el atenuante parcial que la
  factura tiene por los terceros.

## A.3 Y el lado ventas no se puede medir desde el backend

`FacturaServiceImpl`: **cero** `setSubtotal`/`setSubcero`. `Factura.subtotal` y `Factura.subcero`
**llegan tal cual del payload del frontend** al emitir. El mecanismo de doble conteo del lado
compra no está replicado en el backend de ventas.

O sea: **si el problema existe en ventas, está en qué manda `saaFE` al crear la factura**, y eso se
mide del otro lado. Para agosto no se nota — la base 0% de ventas del talón es `0,00`.

> **Lo que este anexo cambia respecto del §3:** deja de ser "un detalle de los servicios básicos" y
> pasa a ser **el reparto de bases por tarifa, que no existe en ninguna parte del sistema**. Sigue
> siendo decisión del usuario (§6-A) porque tocar la carga no corrige lo ya cargado.

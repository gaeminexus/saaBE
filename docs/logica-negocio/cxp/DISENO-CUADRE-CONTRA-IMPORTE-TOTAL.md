# El asiento de compra cuadra contra el `importeTotal` de la factura

**Equipo:** `lap-saa-1` · **2026-09-02** · Módulo `cxp` (toca `cnt`, compartido)
**Estado: diseñado.** Requerimiento del usuario, urgente.

---

## 1. El requerimiento, en una línea

> Validar siempre la suma final de las líneas contra el **total con impuestos** de la factura, y
> mandar la diferencia —**en más o en menos**— a la cuenta **`4.8.90.90.35 – Diferencia por
> redondeo SRI`**.

---

## 2. Qué hace hoy, verificado archivo:línea

`AsientoContableServiceImpl.generarAsientoFacturaCompra:2299-2416`:

| Línea | Valor | Fuente |
|---|---|---|
| DEBE gasto | `Σ DetalleFacturaCompra.subTotal`, agrupado por `GrupoProductoPago` | **el detalle** |
| DEBE IVA | `FacturaCompra.vIVA` (cabecera), repartido entre los códigos del detalle por `distribuirIvaCabecera` | la cabecera |
| HABER CxP | **`Σ de las líneas DEBE`**, redondeado (`:2406-2412`) | ninguna de las dos |

**El `importeTotal` del XML no interviene en ningún punto del asiento.** Se guarda en
`FacturaCompra.total` y nadie lo usa para contabilizar.

> **Por qué nadie lo notó, y es la parte que vale registrar:** como el HABER se calcula **sumando el
> DEBE**, el asiento **siempre cuadra**. No falla nunca. Simplemente registra una cuenta por pagar
> que no es la que dice la factura. Es la familia ya conocida acá — *un asiento mal clasificado y
> cuadrado igual no se detecta solo*.

**El precedente de la solución ya existe en el mismo método:** para el IVA, `distribuirIvaCabecera`
toma el valor de **cabecera** y lo reparte entre los códigos que aparecen en el detalle, cargando el
residuo del redondeo al código de mayor valor. **Al subtotal nunca se le hizo lo mismo.**

---

## 3. Qué pasa a hacer

**La cuenta por pagar pasa a ser `importeTotal`** —que es lo que el proveedor realmente cobra— y una
línea de ajuste absorbe la diferencia:

```
totalDebe   = Σ (DEBE gasto por grupo) + Σ (DEBE IVA)
importeTotal = FacturaCompra.total          // <importeTotal> del XML
diferencia  = redondear(importeTotal - totalDebe, 2)

si |diferencia| >= 0.01:
    diferencia > 0  ->  línea DEBE  de  diferencia   a 4.8.90.90.35
    diferencia < 0  ->  línea HABER de |diferencia|  a 4.8.90.90.35

HABER CxP proveedor = importeTotal        // ya no la suma del DEBE
```

**Comprobación de cuadre, en los dos sentidos:**

| Caso | DEBE | HABER | ¿Cuadra? |
|---|---|---|---|
| `importeTotal > totalDebe` | `totalDebe + diferencia` | `importeTotal` | ✅ |
| `importeTotal < totalDebe` | `totalDebe` | `importeTotal + |diferencia|` | ✅ |
| Sin diferencia | `totalDebe` | `importeTotal` (= totalDebe) | ✅ |

### 3.1 Un efecto colateral que conviene entender, porque es bueno

Este mecanismo **cubre de una sola vez los dos descuadres distintos** que existen hoy:

1. `Σ detalles ≠ totalSinImpuestos` de cabecera — el emisor redondeó distinto línea por línea.
2. `totalSinImpuestos + impuestos ≠ importeTotal` — el propio XML no cuadra consigo mismo.

No hace falta tratarlos por separado: **cualquier hueco entre lo que se contabiliza y lo que la
factura dice que se debe termina en la línea de ajuste.** Anclar contra `importeTotal` es lo que lo
hace posible; anclar contra el subtotal habría dejado el segundo abierto.

---

## 4. ⚠️ La tolerancia, y por qué el proceso tiene que parar cuando se supera

**El `importeTotal` del SRI puede incluir legítimamente `ICE` y `propina`**, que no están ni en el
subtotal ni en el IVA. Si esa diferencia se manda a «diferencia por redondeo», el sistema estaría
**escondiendo un error de clasificación detrás de una cuenta de ajuste** — y quedaría cuadrado, o sea
invisible, que es exactamente el defecto que este cambio viene a corregir.

**Regla adoptada:**

| `|diferencia|` | Qué pasa |
|---|---|
| `< 0.01` | No se agrega línea. No hay diferencia |
| `0.01` a `0.50` | Línea de ajuste a `4.8.90.90.35`. Es el caso de redondeo |
| `> 0.50` | ⛔ **`IncomeException`**, nombrando la factura, el total, la suma calculada y la diferencia |

**Umbral de 0,50 y no de 0,05**: el usuario reportó centavos, pero una factura con muchas líneas
puede acumular más de cinco centavos de redondeo legítimo. Medio dólar deja pasar el redondeo real y
frena el ICE o la propina, que nunca son de esa magnitud.

> Es una **decisión revisable con datos**: `cxp/sql/lap1-04-diagnostico-descuadre-centavos-fctc.sql`
> mide la distribución real de las diferencias. Si aparecen redondeos legítimos por encima de 0,50,
> se sube el umbral; **la constante está en un solo lugar para que sea un cambio de un número.**

---

## 5. La cuenta `4.8.90.90.35` — cómo se resuelve

**Este asiento no usa plantilla.** Las líneas se construyen en código; las plantillas (`CNT.PLNS`) se
usan en otros flujos, como el rol de nómina. Así que la cuenta no «se agrega a una plantilla»: el
código tiene que resolverla.

**Se resuelve por su código contable** en `CNT.PLNN` (`PLNNCNTA = '4.8.90.90.35'`), con el código en
una constante única. **Si no existe o está inactiva, `IncomeException` que la nombre** — nunca un
silencioso «no ajusto nada».

**No hace falta DDL** si la cuenta ya está en el plan (lo confirma el BLOQUE 1 del diagnóstico).

> **Deuda declarada:** lo correcto a futuro es una fila de configuración por empresa, como se resuelve
> la cuenta de IVA crédito tributario (`PGS.TSRI`). Se hace por constante **por urgencia**, y queda
> anotado acá para no descubrirlo como sorpresa: **una empresa nueva con otro plan de cuentas va a
> fallar en el alta hasta que exista esa cuenta con ese código exacto.**

---

## 6. Alcance

| Método | Entra | Por qué |
|---|---|---|
| `generarAsientoFacturaCompra` | ✅ | Es el del requerimiento |
| `generarAsientoFacturaCompraReembolso` | ✅ | Misma clase, mismo patrón de HABER = Σ DEBE, misma factura de origen. Dejarlo afuera dejaría la mitad del módulo con el defecto |
| Liquidación de compra, NC y ND de compra | ⏸️ **pendiente de decisión del usuario** | Tienen el mismo patrón; no se tocan sin confirmarlo |

---

## 7. Riesgos

1. **Cambia el valor de la cuenta por pagar** de facturas nuevas. Es el objetivo, pero es un cambio
   de comportamiento contable: a partir del despliegue, la CxP registrada es `importeTotal`.
2. **No corrige lo ya contabilizado.** Las facturas cargadas antes conservan su CxP anterior. Si hay
   que corregirlas, es un trabajo aparte y con su propio script.
3. **`com.saa.ejb.cnt` es territorio compartido** con `omen-saa-2`. Verificado sin cambios ajenos
   desde el 2026-08-27 (`5594f8a`); se avisa antes de escribir.

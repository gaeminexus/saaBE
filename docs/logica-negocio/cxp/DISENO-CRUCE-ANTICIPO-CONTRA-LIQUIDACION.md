# Cruzar anticipos de proveedor contra liquidaciones de compra — diseño

**Equipo:** `lap-saa-1` · **2026-09-02** · Módulo `cxp`
**Requerimiento del usuario.**

---

## 1. Qué se pide

En la pantalla de **cruce de anticipos de CXP**, además de las facturas, deben aparecer las
**liquidaciones de compra emitidas**, para poder cruzar contra ellas el anticipo del proveedor.

---

## 2. Por qué no es un cambio de pantalla

**La tabla de aplicaciones no puede referenciar una liquidación.** `PGS.APLP` tiene una FK por cada
documento que participa, y **ninguna apunta a `LQCC`**:

```
APLPFCTC → PGS.FCTC   factura de compra    (el documento AFECTADO)
APLPNTCC → PGS.NTCC   nota de crédito      \
APLPRTNC → CBR.RTNC   retención             |  el documento que PAGA,
APLPRTV2 → CBR.RTV2   retención V2          |  discriminado por APLPTDPG
APLPNTDC → PGS.NTDC   nota de débito        |
APLPANTP / APLPANTO → anticipo            /
```

Y esto **explica hacia atrás** un hallazgo de esta misma semana: al escribir el contrato de
anulación se verificó que `LQCC` es el único de los nueve documentos **sin movimientos que
cascadear**, y al agregar la liquidación al estado de cuenta se concluyó que **no debe consultar
saldo**. Las dos cosas eran ciertas por la misma razón: *la liquidación de compra nunca pudo tener
aplicaciones*. Este cambio es el que las vuelve falsas — a propósito.

### 2.1 El discriminador `APLPTDPG` NO cambia

`TipoDocPagoAplicacion` (1 cobro directo, 2 nota de crédito, 3 retención, 4 anticipo, 5 nota de
débito) describe **quién paga**, no qué se paga. Un anticipo cruzado contra una liquidación sigue
siendo `ANTICIPO = 4`.

⛔ **No agregar un valor nuevo a ese rubro.** Lo que cambia es el documento **afectado**, que se
identifica por cuál de las dos columnas viene poblada.

---

## 3. Modelo

### 3.1 DDL

| Tabla | Columna | Para qué |
|---|---|---|
| `PGS.APLP` | `APLPLQCC` `NUMBER` nullable, FK a `PGS.LQCC(ID)` | la liquidación afectada |
| `PGS.LQCC` | `LQCCEPAG` `NUMBER(1)` nullable, `DEFAULT 1` | estado de pago, espejo de `FCTC.FCTCEPAG` |

**`LQCCEPAG` hace falta y no es opcional:** hoy la liquidación **no tiene estado de pago** —lo
verifiqué columna por columna—, así que sin él no hay dónde registrar que quedó parcial o pagada, y
la pantalla no podría distinguir una liquidación ya cruzada de una pendiente.

⚠️ **Las dos van NULLABLE**, por la lección de `ANTCAPLC`: una columna `DEFAULT n NOT NULL` rompe
**todo `INSERT`** de la entidad, porque Hibernate siempre la nombra y el `DEFAULT` de Oracle sólo
actúa cuando el `INSERT` la omite. La integridad la da el inicializador en Java.

Script: `cxp/sql/lap1-10-cruce-anticipo-liquidacion.sql`. **Va ANTES del WAR.**

### 3.2 Entidades

`AplicacionPagoCxp` — una FK más, en el estilo de las que ya tiene:
```java
/** Liquidación de compra afectada. FK a PGS.LQCC. Excluyente con `facturaCompra`. */
@ManyToOne @JoinColumn(name = "APLPLQCC", referencedColumnName = "ID")
private LiquidacionCompraCompra liquidacionCompra;
```

`LiquidacionCompraCompra` — `private Long estadoPago = 1L;` sobre `LQCCEPAG`, **inicializado**.

> **Invariante nuevo, y hay que escribirlo en el javadoc:** una aplicación afecta **una factura o
> una liquidación, nunca las dos ni ninguna**. Es el mismo tipo de FK excluyente que ya tiene
> `PagoProgramado` entre factura, egreso y anticipo — y que, como está documentado en el bloqueo de
> la reversión, **es justamente lo que impide poner esas FK en null a la ligera**.

---

## 4. Contrato de API

### 4.1 `POST /rest/aplp/anticipo` — aditivo

```json
{ "idFacturaCompra": 12, "valor": 100.00, "idEmpresa": 1, "idUsuario": 1 }
{ "idLiquidacionCompra": 7, "valor": 100.00, "idEmpresa": 1, "idUsuario": 1 }
```

| Campo | Nota |
|---|---|
| `idFacturaCompra` | como hoy |
| `idLiquidacionCompra` | **alternativo**, no adicional |

**Exactamente uno de los dos.** Ninguno → **400**. Los dos → **400**, no «gana el primero»: un
cliente que mande los dos está confundido, y elegir por él escondería el error.

Mismo criterio en `POST /rest/aplp/anticipos` (el de lote).

### 4.2 `GET /rest/aplp/saldo/{id}` — ⛔ NO se toca

Ese endpoint hace `em.find(FacturaCompra.class, id)` con el id que reciba. **`FCTC` y `LQCC` tienen
numeraciones IDENTITY independientes**, así que pasarle un id de liquidación devolvería los datos de
una factura ajena que coincida en número, **sin ningún error**.

Es el defecto que ya se evitó esta semana en el estado de cuenta. **La liquidación necesita su
propio endpoint de saldo**, no reusar éste:

```
GET /rest/aplp/saldoLiquidacion/{idLiquidacionCompra}
```

Misma forma de respuesta que `/saldo/{id}`, para que el frontend no tenga que tratarlos distinto más
allá de la ruta.

### 4.3 Documentos cruzables del proveedor

La pantalla necesita la lista. Si el endpoint que hoy alimenta esa grilla devuelve sólo facturas,
**se extiende para incluir liquidaciones con saldo**, con un campo que diga cuál es cuál — el
frontend tiene que saber a qué ruta mandar el cruce.

---

## 5. Lo que hay que tocar del lado servidor

| # | Qué | Nota |
|---|---|---|
| 1 | `aplicarAnticipo` / `aplicarAnticipos` | aceptar liquidación como documento afectado |
| 2 | `sumaAplicadoByFactura` | equivalente por liquidación |
| 3 | `recalcularEstadoPagoFactura` | equivalente que escriba `LQCCEPAG` |
| 4 | `revertirAplicacion` | ya reversa por aplicación; verificar que el camino de vuelta actualice el estado de pago **de la liquidación** y no asuma factura |
| 5 | Anulación de liquidación de compra | **hoy no cascadea porque no tenía movimientos.** Al existir aplicaciones, `LQCC` deja de ser el caso sin cascada del contrato de anulación |

⛔ **El punto 5 es el que se olvida.** `cxc/API-ANULACION-DOCUMENTOS.md` dice que `lqcc` no tiene
`movimientosRelacionados`, no acepta cascada y nunca devuelve 409 — **y eso deja de ser cierto con
este cambio**. Si no se actualiza, anular una liquidación con anticipos cruzados los dejaría
huérfanos en silencio. **El contrato se actualiza en el mismo cambio.**

---

## 6. Frontend

Pantalla de cruce de anticipos de CXP: la grilla de documentos afectables lista **facturas y
liquidaciones**, distinguidas visiblemente (columna o etiqueta de tipo), y el cruce se manda a la
ruta que corresponda según el tipo.

**Que se distingan importa:** una factura y una liquidación del mismo proveedor pueden tener números
parecidos, y quien cruza tiene que saber contra qué está aplicando la plata.

---

## 7. Alcance

| | |
|---|---|
| Entra | Cruce de **anticipo de proveedor** contra **liquidación de compra** |
| No entra | Cruzar retenciones, notas de crédito o notas de débito contra liquidaciones. El modelo queda preparado —la FK sirve para cualquiera— pero **no se activa sin pedido**: cada uno tiene sus propias reglas de recálculo |
| No entra | El lado `cxc` (`AplicacionPagoCxc`), que ya tiene su propia FK a liquidación de venta (`APLCLQCS`) |

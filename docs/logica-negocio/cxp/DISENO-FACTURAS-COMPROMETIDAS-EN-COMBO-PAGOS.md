# Una factura con pago registrado no debe ofrecerse para pagar de nuevo

**Equipo:** `lap-saa-1` · **2026-09-02** · Módulo `cxp`

---

## 1. Qué se pide

Si ya se registró un pago que cubre **la totalidad** de una factura, esa factura **no debe aparecer**
en el combo de registrar pagos.

---

## 2. Por qué pasa hoy — dos causas, no una

### 2.1 El combo filtra por el campo equivocado

`factura-compra-selector-dialog.component.ts:137-140` filtra con
`estadoPago !== EstadoPagoFactura.PAGADA`.

**`FCTCEPAG` refleja pagos APLICADOS** — aplicaciones en `PGS.APLP` —, no pagos **registrados**. Un
pago recién registrado todavía no aplicó nada, así que la factura sigue diciendo `PENDIENTE` y
vuelve a ofrecerse. Se puede registrar dos veces el pago de la misma factura sin ningún aviso.

### 2.2 ⛔ Y la consulta que serviría está desactualizada desde el rediseño de la bandeja

`PagoProgramadoDaoServiceImpl.selectVigentesByFactura` considera vigentes
`REGISTRADO(1)`, `EN_ARCHIVO(2)` y `CONFIRMADO(3)` — **y no `POR_APROBAR(0)`**.

Desde el frente S (bandeja universal de aprobación), **un pago nace `POR_APROBAR`** cuando no se
manda cuenta bancaria de origen, que es el flujo normal desde entonces
(`PagoProgramadoServiceImpl:957`). O sea: **la consulta no ve los pagos que hoy se crean.**

> Es el patrón que este equipo ya tiene registrado dos veces esta semana: *al mover una decisión de
> una capa a otra, lo que dependía de ella no viaja solo.* Primero fue la guarda del egreso, que se
> quedó exigiendo la cuenta; ahora es esta consulta, que se quedó sin el estado nuevo.

**Y no dio la cara antes porque `selectVigentesByFactura` no tiene un solo llamador.** Es código
muerto desde que se escribió. Ninguna pantalla lo usa, así que su omisión nunca se notó.

---

## 3. La regla — «la totalidad», no «algún pago»

El usuario dijo **totalidad**, y la distinción importa: un pago parcial **no** debe sacar la factura
del combo, porque todavía se le puede registrar el resto.

```
saldoPendiente = total − aplicado            (lo que ya reconoce el sistema hoy)
comprometido   = Σ valor de los pagos VIGENTES de esa factura
disponible     = saldoPendiente − comprometido

se OCULTA del combo  ⟺  disponible <= 0
```

**Vigente = `POR_APROBAR(0)`, `REGISTRADO(1)`, `EN_ARCHIVO(2)`, `CONFIRMADO(3)`.**
`RECHAZADO(4)` y `ANULADO(5)` **no** cuentan: un pago rechazado libera la factura, que es
exactamente el caso de la factura que hoy no se puede revertir por el pago 137.

---

## 4. Dónde vive la regla — en el servidor

**El backend decide, el frontend sólo excluye.** El servidor tiene las tres cifras —total, aplicado
y pagos vigentes—; el combo sólo tiene `total` y `estadoPago`. Calcularlo en el cliente obligaría a
una llamada por factura y dejaría la regla escrita en una pantalla.

### `GET /rest/pgtr/facturasComprometidas/{idTitular}`

Devuelve los **ids de las facturas de ese proveedor cuyo saldo pendiente ya está íntegramente
comprometido** por pagos vigentes.

```json
{ "idTitular": 45, "idsFacturas": [12, 87, 103] }
```

El frontend hace la llamada junto a la de facturas y excluye esos ids **cuando `soloPendientes`
está activo**. Sin ese flag, el selector sigue mostrando todo — hay pantallas que necesitan elegir
una factura ya pagada.

---

## 5. Lo que hay que tocar

| # | Dónde | Qué |
|---|---|---|
| 1 | `PagoProgramadoDaoServiceImpl.selectVigentesByFactura` | **agregar `POR_APROBAR(0)`** a los estados vigentes |
| 2 | `PagoProgramadoService` / `Impl` | método nuevo que aplique la regla del §3 para un titular |
| 3 | `PagoProgramadoRest` | el endpoint del §4 |
| 4 | `factura-compra-selector-dialog` | excluir esos ids cuando `soloPendientes` |

⚠️ **El punto 1 cambia el comportamiento de un método que hoy nadie llama, así que no rompe nada
—pero deja de ser código muerto.** Antes de darlo por seguro hay que confirmar que sigue sin
llamadores en el momento de tocarlo.

⚠️ **El punto 4 toca `factura-compra-selector-dialog`, que es de `eq3`** (`af312c0`, 2026-08-31) y lo
usan varias pantallas. `git status` + `git log -3` antes, y el cambio va **detrás de
`soloPendientes`** para que ninguna otra pantalla cambie de comportamiento.

---

## 6. Fuera de alcance

- **Liquidaciones de compra.** Podrían tener el mismo problema, pero `PagoProgramado` **no tiene FK a
  `LQCC`**, así que hoy no se les registra un pago por esta vía. No se toca sin pedido.
- **El lado `cxc`** (cobros).

# PLAN — Pagar liquidaciones de compra y notas de venta desde la Solicitud de pago

**Equipo:** `omen-saa-2` · **Escrito:** 2026-09-14 · **Urgente:** el usuario tiene que pagar 3 liquidaciones de compra.
**Contrato:** `pagos/API-PAGO-LIQUIDACION-Y-NOTA-VENTA.md` (espejado a `saaFE/docs/pagos/`).
**DDL:** `cxp/sql/e2-41-pago-programado-a-liquidacion-de-compra.sql`. **Medición previa:** `cxp/sql/e2-40`.

---

## 1. El defecto, medido en el código

*«En CXP → Solicitud de pago, elijo el proveedor y solo salen facturas.»*

**No es un filtro: el pago de una liquidación no existe.**

| Pieza | Qué hace hoy |
|---|---|
| `solicitud-pago.component.ts` | Abre `FacturaCompraSelectorDialog` sin `tipoDocumento` → sólo `FacturaCompra` |
| `PagoProgramadoRest.registrar` (`:179`) | Exige `idFacturaCompra` (400 si falta) |
| `PagoProgramadoServiceImpl.registrarPago` (`:218`) | `em.find(FacturaCompra.class, …)` |
| `PGS.PGTR` | FK a factura (`PGTRFCTC`), egreso, anticipo, origen externo. **Ninguna a liquidación** |
| `contabilizarSegunOrigen` | Ramas: origen externo → anticipo → egreso → **`else` = factura** (`aplicarPagoTransferencia`, que revienta si no hay factura) |

## 2. Lo que YA existe y se reusa

- **La cuenta por pagar de la liquidación es `PGS.LQCC`** (`LiquidacionCompraCompra`). La crea `LiquidacionCompraServiceImpl.crearDocumentoCxp` al autorizarse la liquidación emitida (`CBR.LQCS`), con su asiento, y la enlaza en `LQCS.LQCSLQCC`.
  ⚠️ **No confundir con `CBR.LQCS`**, que es sólo el trámite de emisión ante el SRI y **no** tiene cuenta por pagar. El selector del FE, en su modo `LIQUIDACION`, consulta `LQCS` — ese modo es para retenciones y **no se toca**.
- `PGS.APLP.APLPLQCC` y en `AplicacionPagoCxpServiceImpl`: `nuevaAplicacionLiquidacion`, `validaMontoContraSaldoLiquidacion`, `saldoLiquidacion`, `recalcularEstadoPagoLiquidacion`, `consultarPorLiquidacion`. Hoy los usan el cruce de anticipos y caja chica.
- `LQCC.LQCCEPAG` (1 pendiente, 2 parcial, 3 pagada), espejo de `FCTC.FCTCEPAG`.
- REST: `POST /lqcc/selectByCriteria`, `GET /aplp/saldoLiquidacion/{id}`, `GET /aplp/liquidacion/{id}`.

## 3. La nota de venta no necesita backend nuevo

`FacturaCompraServiceImpl.registrarNotaVentaManual` graba una **`FacturaCompra` con `TIPOCOMPROBANTE = '02'`**. Se paga por `PGTRFCTC` como cualquier factura. **Hoy ya aparece en el listado, mezclada con las facturas y sin distinguirse.** Lo que falta es sólo que la pantalla la separe por tipo.

⚠️ Su `FCTCEPAG` nace NULL (la nota manual no lo setea). El selector conserva las filas con estado de pago nulo, así que no se oculta; el saldo sale de `total − aplicado`, no de esa columna.

## 4. Diseño

### 4.1 Base
`PGS.PGTR.PGTRLQCC NUMBER NULL` → FK `PGS.LQCC(ID)`. **Excluyente** con `PGTRFCTC`, `PGTREGRS`, `PGTRANTP` y el origen externo. Script `e2-41`. **Va antes del WAR.**

### 4.2 Backend
1. `PagoProgramado.liquidacionCompra` (`@ManyToOne @JoinColumn(name="PGTRLQCC")`).
2. `OrigenPagoCxp.LIQUIDACION_COMPRA = "LIQUIDACION_COMPRA"`.
3. **Registro:** `POST /pgtr` acepta `idLiquidacionCompra` (id de `LQCC`) como alternativa excluyente a `idFacturaCompra`. Mismas reglas que la factura: titular obligatorio, documento activo, cuenta de destino del mismo titular, valor ≤ saldo − comprometido en pagos vigentes no confirmados de **la misma liquidación**. Mismo flujo por cuenta nula (`POR_APROBAR`), cheque, transferencia y débito automático.
4. **Contabilización:** rama nueva en `contabilizarSegunOrigen`, **antes del `else`**: `if (pago.getLiquidacionCompra() != null)` → `aplicacionPagoCxpService.aplicarPagoTransferenciaLiquidacion(...)`. Copia de `aplicarPagoTransferencia` cambiando **sólo** el documento: mismo asiento `generarAsientoPagoTransferenciaCxp(idProveedor, …)` (DEBE CxP proveedor / HABER banco), aplicación con `nuevaAplicacionLiquidacion`, `recalcularEstadoPagoLiquidacion`, mismo movimiento bancario.
5. **Todo lugar que pregunta «¿es de factura?»** debe conocer la liquidación: `origenDe`, `conceptoDe` (`"Liquidación {numero}"`), los dos filtros por origen de `PagoProgramadoDaoServiceImpl` (`:107`, `:345`), guardas de `aprobar`, archivo del banco, anular, revertirConfirmado, textos de asiento y movimiento. Inventario completo: reporte del ítem 1 de `omen-saa-2-be`.
6. `GET /pgtr/facturasComprometidas/{idTitular}` agrega `idsLiquidaciones` (aditivo).

### 4.3 Frontend — `cxp/forms/pagos/solicitud-pago`
Pedido del usuario, textual: *«un combo que indique el documento que se va a pagar —factura, liquidación en compra y nota de venta— y luego otro combo que despliegue el listado de esos documentos»*.

1. Combo **Tipo de documento**: Factura (default) / Liquidación de compra / Nota de venta.
2. Elegido proveedor y tipo → combo **Documento** con los pendientes de ese proveedor:
   - Factura: `/fctc/selectByCriteria` por titular, `tipoComprobante !== '02'`.
   - Nota de venta: la misma consulta, `tipoComprobante === '02'`.
   - Liquidación: `/lqcc/selectByCriteria` por titular.
   - En los tres: descartar `estado` inactivo, `estadoPago === 3` y los ids comprometidos.
3. Saldo: `/aplp/saldo/{id}` (factura y nota de venta) o `/aplp/saldoLiquidacion/{id}` (liquidación).
4. Registrar manda `idFacturaCompra` **o** `idLiquidacionCompra`, nunca los dos.

## 5. Orden de despliegue

1. **`e2-41`** (el usuario). Sin esto, el WAR rompe **todo** el circuito de pagos con `ORA-00904`.
2. **WAR.** Con FE viejo es inofensivo: sigue mandando `idFacturaCompra`.
3. **FE.**

⛔ El mapeo de `PGTRLQCC` no se commitea a `main` hasta que el usuario confirme el `e2-41` corrido (registro de reservas §7).

## 6. Trampas

| # | Trampa | Defensa |
|---|---|---|
| 1 | Un pago sin factura cae hoy en el `else` de factura, y los filtros por origen desconocido caen en la rama de origen externo. Un pago de liquidación mal enganchado **no revienta: se contabiliza o se esconde por otro camino** | La rama nueva va ANTES del `else`, y cada «¿es de factura?» del inventario tiene su caso de liquidación |
| 2 | Confundir `CBR.LQCS` (emisión SRI) con `PGS.LQCC` (cuenta por pagar) | El pago referencia **`LQCC`**. El modo `LIQUIDACION` del selector (que lee `LQCS`) no se toca |
| 3 | Liquidación autorizada sin documento CXP todavía | No aparece en `LQCC`, así que no se ofrece. La pantalla avisa: *«¿No ve la liquidación? Debe estar autorizada y con "Generar documento CXP" ejecutado»* |
| 4 | `saldoLiquidacion` responde `liquidacionId`/`numeroLiquidacion`, no `facturaId`/`numeroFactura` | El contrato lo dice; el FE mapea por tipo |
| 5 | `merge` desnudo: un PUT parcial graba NULL | No aplica: todo va por `POST /pgtr`, no por PUT |

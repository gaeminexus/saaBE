# Un gasto de caja chica puede pagar una factura o liquidación de compra

**Equipo:** `omen-saa-2` · **Creado:** 2026-09-03 · **Estado (2026-09-04):** los 7 pasos del §7 están IMPLEMENTADOS y compilan. ⛔ **Nada se probó contra un servidor real** — la lista de 8 casos está en `API-GASTO-CAJA-CHICA.md` §4. Ver §16 de `ESTADO-EQUIPO-OMEN-2.md`.

---

## 0. El pedido

**Textual del usuario:** *«Al registrar gastos de caja chica, me permite escoger el beneficiario,
pero no me permite escoger las facturas o liquidaciones de ese proveedor. Debería permitirme escoger
la factura para que se abone o se pague a su totalidad esa factura con caja chica. Y obviamente esto
debería verse reflejado en el estado de cuenta.»*

**Decisiones del usuario, 2026-09-03:**

| # | Decisión |
|---|---|
| **D1** | El gasto puede pagar **parcialmente** una factura, no sólo el total |
| **D2** | **Un gasto por factura.** Si se pagan tres facturas, son tres gastos de caja chica |
| **D3** | Se refleja en **los dos** estados de cuenta: el del proveedor y el de la caja chica |

**D2 es la que simplifica todo el diseño**: sin reparto de un gasto entre varios documentos, no hay
que resolver ni el prorrateo ni el reverso parcial. La relación es 1:1.

---

## 1. Lo que hay hoy, verificado

| Pieza | Estado |
|---|---|
| `TSR.MVCH` (`MovimientoCajaChica`) | Tiene `TTLRCDGO` (el beneficiario que ya se elige), `MVCHPRDP` (producto de pago), `MVCHNDOC` (**número de documento como texto libre**), `ASNTCDGO`, `PGTRCDGO`. **Ninguna FK a factura ni a liquidación** |
| `TipoDocPagoAplicacion` | Cinco orígenes: `COBRO_DIRECTO(1)`, `NOTA_CREDITO(2)`, `RETENCION(3)`, `ANTICIPO(4)`, `NOTA_DEBITO(5)`. **Caja chica no está** |
| `PGS.APLP` (`AplicacionPagoCxp`) | Ya modela «documento afectado» (`APLPFCTC`/`APLPLQCC`) y «origen del pago» (`APLPANTP` para anticipo). **Falta el origen caja chica** |
| `/mvch/gasto` | Registra el movimiento y contabiliza contra la cuenta del **producto de pago**. **No** pasa por la bandeja de tesorería, y eso es correcto: el efectivo ya salió de la caja, no hay nada que aprobar |

---

## 2. 🔴 El punto contable, que es lo que hace que esto no sea «agregar un combo»

**Hoy un gasto de caja chica RECONOCE un gasto.** Se contabiliza contra la cuenta del producto de
pago elegido.

**Pero si el gasto paga una factura ya registrada, ese gasto YA se reconoció** cuando se registró la
factura. Volver a reconocerlo lo **cuenta dos veces** — el mismo defecto que se corrigió el
2026-09-01 en jubilación patronal y desahucio (§4.1bis de
`rhh/PLAN-PAGO-BENEFICIOS-Y-SALIDA-POR-TESORERIA.md`).

**Van a convivir dos gastos de caja chica con contabilidad distinta:**

| Caso | DEBE | HABER |
|---|---|---|
| Gasto suelto — **lo de hoy, no se toca** | cuenta del producto de pago (gasto) | caja chica |
| **Pago de documento — nuevo** | **cuentas por pagar del proveedor** | caja chica |

> **La regla, y es la que hay que verificar al implementar:** si el gasto trae documento afectado,
> **NO se contabiliza contra la cuenta del producto**. El producto sigue viajando —clasifica el
> movimiento para tesorería— pero no decide la cuenta del DEBE.

⚠️ **Al implementar, confirmar de dónde sale la cuenta por pagar del proveedor.** El precedente es
cómo la resuelve `AplicacionPagoCxpServiceImpl` al aplicar un anticipo; **usar ese mismo camino**, no
inventar uno. Si resulta que la cuenta se resuelve de una forma que no aplica acá, **parar y
reportar**.

---

## 3. Modelo — dónde va el vínculo

⛔ **NO se agrega la FK al documento en `TSR.MVCH`.** Se agrega el origen en `PGS.APLP`:

```
PGS.APLP.APLPMVCH  NUMBER NULL  → FK a TSR.MVCH.MVCHCDGO
```

**Por qué de este lado y no del otro:** `APLP` **ya es** la tabla que modela «qué documento se
afecta» y «con qué se paga». Tiene `APLPFCTC`/`APLPLQCC` para lo primero y `APLPANTP` para lo
segundo. Caja chica es **otro origen de pago**, exactamente como el anticipo. Ponerlo ahí:

- reusa el reverso, el estado de pago y el saldo que ya funcionan para anticipos;
- hace que el saldo de la factura salga **solo** — `sumaAplicadoByLiquidacion` y su hermano de
  factura ya suman por documento afectado, sin importar el origen;
- y **no toca `MVCH`**, que es una tabla en uso.

**Saber si un gasto pagó un documento** se consulta por `APLPMVCH`, no se persiste en `MVCH`.

⚠️ **`APLPMVCH` va NULLABLE, y las demás FK de documento de esa tabla también.** El 2026-09-03
`APLPFCTC` era `NOT NULL` y rompió el cruce contra liquidaciones con `ORA-01400` — ver
`cxp/sql/e2-05`. **Al agregar esta columna, correr el mismo control**: listar el `NULLABLE` de las
siete FK de `APLP`, no sólo de la nueva.

**Tipo nuevo:** `CAJA_CHICA = 6` en `com.saa.rubros.TipoDocPagoAplicacion`.

---

## 4. El flujo

```
1. El usuario registra un gasto de caja chica y elige beneficiario  (YA EXISTE)
2. NUEVO: elige una factura o liquidación de ESE proveedor, con saldo pendiente
3. NUEVO: indica el monto — puede ser parcial (D1) o el saldo completo
4. Se registra el movimiento de caja chica                          (YA EXISTE)
5. NUEVO: se crea la AplicacionPagoCxp con tipoDocPago = CAJA_CHICA
          y APLPMVCH apuntando al movimiento
6. NUEVO: el asiento va contra cuentas por pagar, no contra gasto   (§2)
```

**El paso 2 NO necesita ningún endpoint nuevo. Verificado el 2026-09-03.** Existe
`DocumentoCruceSelectorDialogComponent` (`saaFE`, `cxp/dialog/documento-cruce-selector-dialog/`),
que sirve **facturas y liquidaciones de compra en una fila unificada**, filtrando por
`titular.codigo` con `DatosBusqueda` sobre el `selectByCriteria` genérico. Se reusa tal cual.
*Sexta pieza construida y sin puerta encontrada en una semana.*

> ⚠️ **No confundirlo con `FacturaCompraSelectorDialogComponent`**, su hermano de nombre parecido: el
> `'LIQUIDACION'` de ese otro es `CBR.LQCS` —la que ASOPREP **emite**, en cxc— y no `PGS.LQCC`. El
> propio componente lo advierte en su comentario de cabecera.

### ⚠️ Lo comprometido: el filtro de la lista NO es la protección

El `soloPendientes` de ese selector filtra únicamente por `estadoPago !== PAGADA`. **No excluye las
facturas ya comprometidas** — esa lógica vive en el otro componente, y sólo para su tipo `FACTURA`.
*(Este documento afirmó lo contrario durante unas horas del 2026-09-03; lo corrigió el agente de
frontend leyendo el archivo completo en vez de confiar en la cita con archivo y línea que le pasé.)*

Sin una validación de servidor hay **doble pago real**, y el camino es corto: factura de $100 con un
pago programado en `POR_APROBAR` por $100 esperando en la bandeja → se paga con caja chica → se crea
la `APLP` y la factura queda pagada → después aprueban el pago de la bandeja y sale $100 otra vez. El
saldo no lo evita: un pago `POR_APROBAR` todavía no tiene `APLP`, y por eso no figura como aplicado.
**Tercera aparición del mismo hueco esta semana** — §11 del documento de estado del equipo.

**Ya está resuelto en el proyecto:** `PagoProgramadoServiceImpl:2012`,
`validaValorContraSaldo(FacturaCompra, Double, Long idPagoEx)`, que resta lo comprometido excluyendo
los `CONFIRMADO` —ya reflejados en el saldo— y arma un mensaje con saldo y comprometido por separado.
Está `private`: se expone **sin tocarle el cuerpo**, y el gasto la llama con `idPagoEx = null`.

**Sólo aplica a facturas, y eso está verificado, no supuesto:** `PagoProgramado` no tiene FK a
liquidación, y `OrigenPagoCxp` define únicamente `FACTURA_COMPRA`, `EGRESO_TESORERIA` y
`ANTICIPO_PROVEEDOR` — **no existe `LIQUIDACION_COMPRA`**. Una liquidación no puede quedar
comprometida por la bandeja, así que para ese caso alcanza con el saldo. No hace falta ningún
`selectVigentesByLiquidacion`.

> **El principio, que ya lleva varias apariciones:** *una validación sólo protege el camino que pasa
> por ella.* Un filtro de lista es comodidad para el usuario; lo que impide el doble pago es la
> comprobación del servidor, porque es la única que está en todos los caminos.

**Validaciones, con el precedente de cada una:**

| Regla | Precedente a copiar |
|---|---|
| El monto no puede superar el saldo del documento | `validaMontoContraSaldo` / `validaMontoContraSaldoLiquidacion` |
| El monto no puede superar el saldo de la caja chica | ya existe en el gasto actual |
| El documento tiene que ser **del proveedor elegido** | — ⚠️ ver abajo |

⚠️ **La validación del proveedor no es opcional.** Si el combo de documentos se llena por proveedor
pero el servidor no revalida, un cambio de beneficiario después de elegir el documento deja un pago
aplicado a la factura de otro. **Revalidar en el servidor**, no confiar en que la pantalla filtró.

---

## 5. Los dos estados de cuenta (D3)

**El del proveedor** debería salir solo: la aplicación existe y los saldos ya se calculan por
documento afectado. **Verificarlo, no asumirlo** — si el estado de cuenta filtra por tipo de
documento de pago y enumera los cinco actuales, el sexto no aparecería. **Buscar ese enumerado antes
de dar el frente por cerrado.**

**El de la caja chica** ya muestra sus movimientos. Lo que hay que agregar es **que se vea contra
qué documento fue** el gasto, para que no quede como una salida sin destino.

---

## 6. El reverso — pedido explícito del usuario, 2026-09-03

> *«Y también es necesario el proceso de anulación de gastos de caja chica y que se reverse el pago
> de factura o el gasto generado.»*

### 6.1 La anulación YA EXISTE, y está bien construida

**Verificado el 2026-09-03.** `POST /mvch/anular/{id}` →
`MovimientoCajaChicaServiceImpl.anularGasto` (~:257). Hoy hace, en este orden:

1. Exige **motivo** no vacío.
2. Exige que el movimiento sea un **GASTO** (no una apertura ni una reposición).
3. Rechaza si **ya está anulado**.
4. ⭐ **Rechaza si el movimiento ya quedó incluido en un cierre de caja**, nombrando el cierre.
5. **Anula el asiento** contable.
6. Marca el movimiento `ANULADO` con su motivo.

**No hay que construir nada de esto.** El paso 4 es el que más vale: impide deshacer un movimiento
que ya se consolidó en un cierre, y esa regla hay que respetarla también para el caso nuevo.

*Quinta pieza en una semana que se dio por faltante y estaba construida. El reflejo correcto en este
repositorio es buscar antes de estimar.*

### 6.2 Lo único que falta: reversar la aplicación

Cuando el gasto pagó un documento, `anularGasto` tiene que **reversar también la
`AplicacionPagoCxp`** — si no, la factura queda pagada y el dinero de vuelta en la caja.

⚠️ **Y acá hay una trampa en el código actual que NO se debe copiar.** El paso 5 envuelve
`anulaAsiento` en un `try/catch` que sólo imprime un aviso por consola y **sigue de largo**: si el
asiento no se puede anular, el movimiento queda anulado igual.

Eso puede ser tolerable para el asiento —es el criterio que ya eligió quien lo escribió— pero
**para la aplicación NO lo es**: un gasto anulado con su aplicación viva deja la factura pagada por
dinero que volvió a la caja. **Si la reversa de la aplicación falla, la anulación entera tiene que
fallar**, no seguir de largo.

*Es la misma familia que todo lo de esta semana: un paso que no puede fallar deja de avisar cuando
está equivocado.*

### 6.3 El otro sentido, y hay que elegir

Revertir la aplicación desde la pantalla de abonos **no puede** dejar el gasto de caja chica vivo.

**Decidir cuál de los dos caminos manda y bloquear el otro con un mensaje que diga qué hacer.** No
alcanza con impedirlo: el mensaje tiene que nombrar el camino correcto. Es el criterio de
`lap-saa-1` — la pregunta no es qué tan grave es el error, sino **si el usuario puede arreglarlo y
reintentar**.

**Recomendación:** que mande **la anulación del gasto**, y que el reverso desde abonos bloquee
diciendo «este abono vino de un gasto de caja chica: anúlelo desde la caja chica». Razón: el gasto
es el hecho de origen —salió efectivo de una caja— y la aplicación es su consecuencia. Además la
anulación ya tiene la validación del cierre (§6.1 paso 4), que el reverso de abonos no conoce.

⚠️ **Verificar que ese mensaje no mande a un callejón sin salida**, como pasó el 2026-09-02 con la
anulación de anticipos: el bloqueo decía «revierta el pago primero» y el camino de reversión
respondía «anule el anticipo, que lo hace en un solo paso». **Comprobar que anular el gasto es
realmente posible en ese estado antes de sugerirlo.**

*Precedentes de los dos lados: `anularAnticipo` (reversa en cascada) y `revertirPagoConfirmado`
(bloquea y manda al otro camino).*

---

## 7. Orden de ejecución

| Paso | Qué | Depende de |
|---|---|---|
| 1 | DDL: `APLPMVCH` + control del `NULLABLE` de las siete FK | — |
| 2 | `TipoDocPagoAplicacion.CAJA_CHICA = 6` | — |
| 3 | BE: aplicación + contabilidad del §2 | 1, 2 |
| 4 | ~~BE: endpoint de documentos pendientes por proveedor~~ **ELIMINADO** — el selector ya existe | — |
| 4b | BE: exponer `validaValorContraSaldo` y llamarla desde el gasto (**sólo facturas**) | 2 |
| 5 | FE: reusar `DocumentoCruceSelectorDialogComponent` + monto en el gasto; agregar la columna de documento a la tabla de movimientos; **elegir documento obliga a informar beneficiario**, que hoy es opcional y no bloquea guardar | 4b |
| 6 | Extender `anularGasto` para reversar la aplicación, y bloquear el otro sentido (§6) | 3 |
| 7 | Verificar los dos estados de cuenta (§5) | 3 |

**El DDL va antes del WAR**, y **no se mergea a `main` el mapeo hasta que la columna exista** — §7
del registro de reservas.

---

## 8. Lo que este diseño NO decide

- **De dónde sale la cuenta por pagar del proveedor** en el asiento nuevo: se copia del camino del
  anticipo, y si no aplica, se para y se reporta (§2).
- **Cuál de los dos reversos manda** (§6) — hay recomendación, falta confirmar que el camino que se sugiera sea realmente transitable en ese estado.
- **Si el gasto con documento debe seguir exigiendo producto de pago.** Hoy es obligatorio y
  clasifica el movimiento; con documento, la cuenta ya no sale de ahí. Se mantiene por ahora.

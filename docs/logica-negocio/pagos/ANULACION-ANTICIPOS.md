# Anulación de anticipos (clientes y proveedores)

**Fecha:** 2026-08-20
**Estado:** IMPLEMENTADO
**Alcance:** `CBR.ANTC` (anticipos de cliente) y `PGS.ANTP` (anticipos a proveedor)

Documenta el proceso de anulación de un anticipo desde la pantalla
*Tesorería → Anticipos → (Clientes | Proveedores) → Ver Anticipos*, incluida la
validación de cruces con facturas.

---

## 1. Cómo se sabe qué abonos deshacer

Desde el **2026-08-20** el cruce se hace contra un **anticipo específico**: la
aplicación guarda en `PGS.APLP.APLPANTO` / `CBR.APLC.APLCANTO` de qué anticipo
salió el dinero, y hay **una aplicación por anticipo consumido**. Anular un
anticipo es entonces exacto:

```
cruces a reversar = APLP/APLC con APLPANTO/APLCANTO = este anticipo y estado = 1
```

El saldo de cada anticipo vive en `ANTPSALD`/`ANTCSALD` (**saldo disponible de
ese anticipo**, no el global del titular). Ver
`MIGRACION-CRUCES-ANTICIPO.md` para el cambio de modelo y la corrección de los
datos que ya estaban en producción.

### Respaldo para los cruces anteriores a la migración

Los cruces viejos no tienen esa FK: se hacían por valor contra el saldo global
del titular (`TSR.PRCC.PRCCSLIN`) y dejaban una fila negativa en la propia tabla
de anticipos, a la que apuntaba `APLPANTP`/`APLCANTC` — el **movimiento**, no el
anticipo de origen.

Si la migración no llegó a atribuirlos, el servicio completa la diferencia con
la heurística vieja:

```
consumido        = valor del anticipo - ANTPSALD
explicado        = suma de los cruces con APLPANTO = este anticipo
sin atribuir     = consumido - explicado

sin atribuir > 0.01  →  se completan con cruces del titular SIN anticipo de
                        origen, del más reciente al más antiguo (LIFO)
```

Esos cruces estimados se cuentan aparte y la respuesta los marca en el campo
`estimacion`, para que la pantalla avise al usuario de que no son atribuciones
exactas. En cuanto la migración corre, ese camino deja de usarse.

---

## 2. Qué se revierte en cada caso

### 2.1 Anticipo NO cruzado

| Estado del anticipo | Qué hace la anulación |
|---|---|
| **Ingresado (1)** | CXP: anula también su `PagoProgramado` Registrado. CXC: solo cambia de estado. No hay asiento ni saldo que tocar. |
| **Confirmado (2)** | 1. Anula el **movimiento bancario** del asiento (`MovimientoBancoService.actualizaEstadoMovimiento` → ANULADO).<br>2. Anula/reversa el **asiento del anticipo** (`AsientoService.anulaAsiento`, que decide entre anular o generar la reversión según el período).<br>3. **Descuenta** `anticipo.valor` del saldo de anticipos del titular (`PRCCSLIN`).<br>4. CXP: anula el `PagoProgramado` confirmado.<br>5. Anticipo → **Anulado (3)**, `saldo = 0`, motivo en la observación. |

### 2.2 Anticipo YA cruzado con facturas

Igual que 2.1, **precedido** de la reversión de los cruces seleccionados. Cada
reversión (`AplicacionPagoCxpService.revertirAplicacion` /
`AplicacionPagoCxcService.revertirAplicacion`) hace:

- Marca la aplicación **Reversada (2)** con el motivo.
- **Recalcula el estado de pago de la factura**: vuelve a quedar pendiente por
  el monto que el anticipo le había abonado.
- **Devuelve el saldo** al anticipo de origen (`ANTPSALD`/`ANTCSALD`) y al saldo
  global del titular (`PRCCSLIN`).
- **Anula el movimiento negativo** en `ANTP`/`ANTC`, si el cruce era de los viejos
  y tenía uno.
- Anula el **asiento del cruce** y su movimiento bancario, si lo hubiera.

El neto sobre `PRCCSLIN` es: `+ cruces reversados − valor del anticipo`.

### 2.3 Qué bloquea la anulación

| Situación | Mensaje |
|---|---|
| El registro es un **movimiento de cruce** (`valor < 0`) | No es un anticipo: hay que reversar el abono desde la factura. |
| El anticipo ya está **Anulado (3)** | Ya está anulado. |
| CXP: el `PagoProgramado` está **En archivo (2)** | El pago está en poder del banco: procesar la respuesta antes de anular. |

Un pago **Confirmado (3)** ya **no** bloquea: la anulación lo reversa (antes
obligaba a pasar primero por `pgtr/revertirConfirmado`).

---

## 3. Endpoints

Ambos módulos exponen el mismo par de endpoints. El flujo es de **dos pasos**:
consultar primero, anular después con la confirmación del usuario.

### 3.1 Consulta previa (no modifica nada)

```
GET /SaaBE/rest/antp/verificarAnulacion/{id}     (proveedores)
GET /SaaBE/rest/antc/verificarAnulacion/{id}     (clientes)
```

```json
{
  "anticipo": 42,
  "puedeAnular": true,
  "requiereConfirmacion": true,
  "estado": 2,
  "valorAnticipo": 500.00,
  "saldoDisponible": 200.00,          (saldo de ESTE anticipo)
  "saldoGlobalAnticipos": 200.00,     (saldo global del titular, como contexto)
  "montoACruzar": 300.00,
  "crucesEstimados": 0,               (>0 = hay cruces elegidos por LIFO, no por FK)
  "cruces": [
    { "idAplicacion": 9, "idFactura": 12, "numeroFactura": "001-001-000000123",
      "montoAplicado": 300.00, "fechaAplicacion": [2026,8,1], "observacion": "..." }
  ],
  "mensaje": "El anticipo ya fue cruzado con 1 factura(s) por un total de $300.00. ..."
}
```

- `puedeAnular = false` → `mensaje` explica el bloqueo (§2.3).
- `requiereConfirmacion = true` → el anticipo fue cruzado; hay que mostrar
  `cruces` y pedir confirmación explícita.

### 3.2 Anulación

```
POST /SaaBE/rest/antp/anular/{id}
POST /SaaBE/rest/antc/anular/{id}

{ "motivo": "...", "idUsuario": 5, "confirmarReversionCruces": false }
```

- `motivo` es **obligatorio** (400 si falta).
- Si el anticipo fue cruzado y `confirmarReversionCruces = false`, responde
  **200** con `{ "exito": false, "requiereConfirmacion": true, "cruces": [...] }`
  — no es un error, es la pregunta al usuario. Reenviar con `true` para ejecutar.
- Éxito: `{ "exito": true, "crucesReversados": 1, "mensaje": "..." }`.

`DELETE /antp/{id}` y `DELETE /antc/{id}` siguen existiendo: equivalen a un
`anular` con motivo genérico y `confirmarReversionCruces = false`, así que
nunca eliminan abonos por su cuenta.

---

## 4. Frontend

La anulación está en **dos** pantallas:

**a) Tesorería → Anticipos → Clientes / Proveedores → Ver Anticipos.** Columna de
acciones de la tabla, **junto al botón de imprimir**.

**b) Tesorería → Anticipos → Seguimiento.** La pantalla de estado de cuenta
(`seguimiento-anticipos`), que además muestra los cruces y sus asientos.

En ambas:

- El botón de anular solo aparece en filas con `valor > 0` y `estado ≠ 3`: los
  movimientos históricos y los ya anulados no lo muestran.
- Flujo: `verificarAnulacion` → diálogo `AnularAnticipoDialogComponent`
  (`modules/tsr/forms/anticipos/dialogs/anular-anticipo-dialog/`) → `anular`.
  El diálogo muestra el saldo del anticipo, el global del titular, las facturas
  afectadas con su total, y exige un check explícito *"Acepto eliminar estos
  abonos"* además del motivo. Si vienen cruces estimados lo advierte.
- Si el backend responde `requiereConfirmacion` en el POST (aparecieron cruces
  entre la consulta y la anulación), el diálogo se reabre con el detalle nuevo.
- Al terminar se recargan el saldo del titular y el historial.

---

## 5. Archivos tocados

**Backend**

| Archivo | Cambio |
|---|---|
| `ejb/cxp/dao/AplicacionPagoCxpDaoService(+Impl)` | `selectCrucesAnticipoActivos` (heurística legacy) y `selectCrucesByAnticipoOrigen` (exacta) |
| `ejb/cxc/dao/AplicacionPagoCxcDaoService(+Impl)` | idem (facturas de venta y liquidaciones) |
| `ejb/cxp/dao/AnticipoProveedorDaoService(+Impl)` | `selectDisponiblesByTitular`, `selectMovimientosByTitular`, `sumaSaldoDisponible` |
| `ejb/cxc/dao/AnticipoClienteDaoService(+Impl)` | idem |
| `ejb/cxp/service/AnticipoProveedorService(+Impl)` | `verificarAnulacion`, `anularAnticipo(..., confirmaReversionCruces)`, `selectDisponibles`, `seguimiento` |
| `ejb/cxc/service/AnticipoClienteService(+Impl)` | idem |
| `ws/rest/cxp/AnticipoProveedorRest` | `GET /verificarAnulacion/{id}`, `GET /disponibles/...`, `GET /seguimiento/...`, `POST /anular/{id}` |
| `ws/rest/cxc/AnticipoClienteRest` | idem, más `DELETE` delegado |
| `rubros/EstadoAnticipoCliente` (nuevo) y `EstadoAnticipoProveedor` | estado 4 = Migrado |

**Frontend (`saaFE`)**

| Archivo | Cambio |
|---|---|
| `modules/tsr/service/anticipo.service.ts` | `verificarAnulacion*`, `anular*`, `disponibles*`, `seguimiento*` y sus interfaces |
| `modules/tsr/forms/anticipos/dialogs/anular-anticipo-dialog/` | Diálogo de confirmación (ts/html/scss) |
| `modules/tsr/forms/anticipos/seguimiento-anticipos/` | Pantalla de seguimiento (ts/html/scss) |
| `modules/tsr/forms/anticipos/anticipos-clientes/*` | Botón anular + columna Estado |
| `modules/tsr/forms/anticipos/anticipos-proveedores/*` | Botón anular + columna Estado |
| `app.routes.ts`, `modules/tsr/menu/menutesoreria/*` | Ruta y entrada de menú de Seguimiento |


## 6. SQL de control

```sql
-- Anticipos de un proveedor con su saldo DISPONIBLE por anticipo
SELECT a.ANTPCDGO, a.ANTPFANT, a.ANTPVLOR, a.ANTPSALD, a.ANTPESTD, a.ANTPNDOC
  FROM PGS.ANTP a
 WHERE a.ANTPTTLR = :titular AND a.ANTPPJRQ = :empresa
   AND a.ANTPESTD <> 4                -- 4 = movimiento histórico
 ORDER BY a.ANTPFANT DESC, a.ANTPCDGO DESC;

-- Qué abonos deshace exactamente la anulación de UN anticipo
SELECT ap.APLPCDGO, f.NUMERO AS factura, ap.APLPMAPL, ap.APLPFAPL,
       ap.APLPESTD, ap.APLPASNT
  FROM PGS.APLP ap
  JOIN PGS.FCTC f ON f.ID = ap.APLPFCTC
 WHERE ap.APLPANTO = :idAnticipo
   AND ap.APLPTDPG = 4 AND ap.APLPESTD = 1
 ORDER BY ap.APLPFAPL DESC, ap.APLPCDGO DESC;

-- Cuadre: suma de saldos por anticipo vs saldo global de la cuenta contable.
-- Es el mismo que devuelve GET /antp/seguimiento/{titular}/{empresa}.
SELECT NVL(pc.PRCCSLIN, 0)                                   AS saldo_global,
       (SELECT NVL(SUM(a.ANTPSALD), 0) FROM PGS.ANTP a
         WHERE a.ANTPTTLR = :titular AND a.ANTPPJRQ = :empresa
           AND a.ANTPESTD = 2 AND a.ANTPVLOR > 0)            AS suma_por_anticipo
  FROM TSR.PRCC pc
  JOIN TSR.PRRL pr ON pr.PRRLCDGO = pc.PRRLCDGO
 WHERE pc.PRCCTPOO = 2
   AND pc.PJRQCDGO = :empresa
   AND pr.PRSNCDGO = :titular
   AND pr.PRRLRZZA = 2;               -- 2 = Proveedor, 1 = Cliente

-- Cruces del titular que TODAVÍA no declaran su anticipo de origen: si
-- devuelve filas, la migración no corrió o quedó incompleta.
SELECT ap.APLPCDGO, f.NUMERO, ap.APLPMAPL, ap.APLPFAPL
  FROM PGS.APLP ap
  JOIN PGS.FCTC f ON f.ID = ap.APLPFCTC
 WHERE ap.APLPTDPG = 4 AND ap.APLPESTD = 1 AND ap.APLPANTO IS NULL
   AND f.TITULAR = :titular AND ap.APLPPJRQ = :empresa
 ORDER BY ap.APLPFAPL DESC;
```

Para clientes: `CBR.ANTC` (`ID`, `TITULAR`, `EMPRESA`, `VALOR`, `ANTCSALD`,
`ESTADO`) / `CBR.APLC` (`APLCTDPG`, `APLCESTD`, `APLCFCTR`, `APLCANTO`) /
`CBR.FCTR`, y `pr.PRRLRZZA = 1`.

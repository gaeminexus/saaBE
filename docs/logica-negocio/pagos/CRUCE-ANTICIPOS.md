# Cruce de anticipos contra facturas

**Fecha:** 2026-08-20
**Estado:** IMPLEMENTADO
**Alcance:** CXP (`PGS.APLP` ← `PGS.ANTP`) y CXC (`CBR.APLC` ← `CBR.ANTC`)

Para la corrección de los datos que ya estaban en producción ver
`MIGRACION-CRUCES-ANTICIPO.md`. Para deshacer un anticipo cruzado ver
`ANULACION-ANTICIPOS.md`.

---

## 1. El modelo

Un cruce consume **un anticipo concreto**, no un saldo abstracto:

```
PGS.ANTP (anticipo)                    PGS.APLP (aplicación de pago)
  ANTPCDGO  ─────────────────────────►   APLPANTO   (anticipo de origen)
  ANTPVLOR  valor del anticipo           APLPTDPG = 4 (ANTICIPO)
  ANTPSALD  saldo DISPONIBLE             APLPMAPL   monto de ESTE cruce
                                         APLPFCTC   factura abonada
                                         APLPASNT   asiento del cruce
```

Reglas:

1. **Una aplicación por anticipo consumido.** Cruzar una factura contra dos
   anticipos genera **dos** aplicaciones, cada una con su asiento. No existe la
   aplicación que "sale de varios anticipos".
2. **`ANTPSALD`/`ANTCSALD` es el saldo disponible de ese anticipo**: nace igual
   al valor cuando el anticipo se confirma, lo descuentan los cruces y se lo
   devuelve la reversión. No es el saldo global del titular.
3. **`TSR.PRCC.PRCCSLIN` sigue siendo el saldo global** y la cuenta que mueve la
   contabilidad. Se mantiene sincronizado: `PRCCSLIN` debe ser igual a
   `SUM(ANTPSALD)` de los anticipos confirmados del titular. El endpoint de
   seguimiento expone ese cuadre (`cuadra`, `diferencia`, `advertencia`).
4. **Ya no se crean filas negativas** en `ANTP`/`ANTC`. Las históricas quedan en
   estado **4 = Migrado** y ninguna pantalla las lee.

### Por qué una aplicación por anticipo

Porque es lo único que hace exacta la anulación. Antes el cruce descontaba el
saldo global y la FK `APLPANTP` apuntaba al movimiento negativo, no al anticipo:
al anular había que **adivinar** por LIFO qué abonos deshacer. Ahora la consulta
es directa (`APLPANTO = :idAnticipo`).

---

## 2. Endpoints

### 2.1 Anticipos disponibles para cruzar

```
GET /SaaBE/rest/antp/disponibles/{idTitular}/{idEmpresa}     (proveedores)
GET /SaaBE/rest/antc/disponibles/{idTitular}/{idEmpresa}     (clientes)
```

Devuelve los anticipos **Confirmados**, con `valor > 0` y `saldo > 0`, ordenados
del más antiguo al más nuevo (FIFO). Es la lista que alimenta la pantalla de
cruce.

### 2.2 Cruce con anticipos elegidos

```
POST /SaaBE/rest/aplp/anticipos          (CXP)
POST /SaaBE/rest/aplc/anticipos          (CXC)

{
  "idFacturaCompra": 123,          // CXC: "idFactura"
  "anticipos": [ { "idAnticipo": 7, "valor": 300.00 },
                 { "idAnticipo": 9, "valor": 200.00 } ],
  "fechaAplicacion": "2026-08-20",
  "idEmpresa": 1,
  "idUsuario": 5,
  "observacion": "Cruce parcial"
}
```

Respuesta:

```json
{
  "exito": true,
  "mensaje": "Se cruzaron 2 anticipos correctamente.",
  "totalCruzado": 500.00,
  "saldoAnticipos": 1500.00,
  "lineas": [
    { "aplicacion": 41, "idAnticipo": 7, "numeroDocAnticipo": "ANT-001",
      "montoAplicado": 300.00, "saldoAnticipo": 0.00, "asiento": "..." },
    { "aplicacion": 42, "idAnticipo": 9, "numeroDocAnticipo": "ANT-002",
      "montoAplicado": 200.00, "saldoAnticipo": 800.00, "asiento": "..." }
  ],
  "facturaId": 123, "total": 900.00, "totalAplicado": 500.00,
  "saldoPendiente": 400.00, "estadoPago": 2
}
```

Validaciones, con el motivo exacto en el mensaje: el anticipo existe, es del
titular de la factura, es de la misma empresa, está **Confirmado**, tiene saldo
suficiente, no viene repetido en dos líneas, y el total no supera el saldo
pendiente de la factura.

### 2.3 Cruce por monto total (compatibilidad)

```
POST /SaaBE/rest/aplp/anticipo     { "idFacturaCompra": 123, "valor": 500.00, ... }
POST /SaaBE/rest/aplc/anticipo     { "idFactura": 123, "valor": 500.00, ... }
```

Sigue existiendo y ahora **reparte el valor por FIFO** entre los anticipos
disponibles, generando igualmente una aplicación por anticipo. Los clientes que
solo mandan el monto siguen funcionando y quedan con la trazabilidad completa.

### 2.4 Seguimiento

```
GET /SaaBE/rest/antp/seguimiento/{idTitular}/{idEmpresa}
GET /SaaBE/rest/antc/seguimiento/{idTitular}/{idEmpresa}
```

Estado de cuenta: cada anticipo con sus fechas, su documento, su asiento y sus
cruces (activos **y** reversados, para poder seguir las anulaciones), más los
totales y el cuadre contra `PRCCSLIN`.

---

## 3. Contabilidad

Un asiento **por línea de cruce**, con las mismas cuentas que antes:

| | CXP (proveedor) | CXC (cliente) |
|---|---|---|
| Tipo | `TipoAsientos.APLICACION_ANTICIPO_PROVEEDOR` | `TipoAsientos.APLICACION_ANTICIPO_CLIENTE` |
| DEBE | Cuenta CxP del proveedor | Cuenta de anticipos del cliente |
| HABER | Cuenta de anticipos del proveedor | Cuenta CxC del cliente |

La observación del asiento nombra el anticipo consumido, así que el mayor queda
legible sin cruzar tablas.

---

## 4. Frontend

**Pantallas de cruce** — CXP: *Cuentas por Pagar → Pagos → Cruce de anticipo*;
CXC: *Cuentas por Cobrar → Cobros → Cruce de anticipo*.

- Tabla de anticipos disponibles con fecha, documento, valor y **saldo
  disponible**, y un campo de monto por fila.
- **Repartir el saldo pendiente**: llena los montos por FIFO hasta cubrir la
  factura. El usuario puede ajustar cualquier línea después.
- **Usar todo el saldo** por fila, y **Limpiar** para empezar de nuevo.
- El pie muestra el total seleccionado y avisa si supera el saldo de la factura
  o el de algún anticipo. El backend revalida todo igual.
- Al confirmar, el resultado lista una línea por anticipo con su asiento y el
  saldo que le quedó.
- Al entrar desde una factura (`?idFactura=`) la pantalla resuelve sola el
  titular leyendo la factura, para poder listar sus anticipos.

**Pantalla de seguimiento** — *Tesorería → Anticipos → Seguimiento*
(`modules/tsr/forms/anticipos/seguimiento-anticipos/`): selector cliente /
proveedor, tarjetas de totales con el semáforo de cuadre, y la tabla de
anticipos expandible al detalle de cruces (factura, monto, estado, asiento,
usuario, observación). Desde ahí también se anula un anticipo.

---

## 5. Qué mirar si algo no cuadra

| Síntoma | Dónde mirar |
|---|---|
| "El saldo global no coincide con la suma por anticipo" | La migración no corrió o quedó incompleta: `MIGRACION-CRUCES-ANTICIPO.md` §7.1 y §8 |
| Un anticipo confirmado no aparece para cruzar | `ANTPSALD = 0` (agotado) o estado ≠ 2 |
| Al anular avisa "cruces estimados" | Hay cruces sin `APLPANTO`: son anteriores a la migración |
| El cruce dice "no alcanza para cruzar" | El saldo es por anticipo, no global: revisar fila por fila en Seguimiento |

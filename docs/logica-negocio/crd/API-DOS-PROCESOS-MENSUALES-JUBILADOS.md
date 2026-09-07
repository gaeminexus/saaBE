# API — Los dos procesos mensuales de jubilados: seguro al inicio, pensiones al final

**Fecha:** 2026-09-07 · **Equipo:** `omen-saa-1` (`omen1`) · **Estado:** contrato cerrado, sin implementar.

Pedido del usuario: *«el seguro médico se paga a inicios de mes y al final del mes se pagan las
pensiones, cruzándolos con préstamos y generando el pago en el banco. El sistema debe permitir
realizar estos dos procesos dentro del mes y dar seguimiento a los mismos»*.

---

## 1. Lo que hay hoy, verificado contra el código

**Hoy es UN solo proceso.** `POST /rest/pgpc/generarPagosDelMes` →
`PagoPensionComplementariaServiceImpl.generarPagosDelMes:701`:

1. Cinco guards de precondición (cuentas, productos, plantillas).
2. Recorre el padrón de jubilados. Por cada uno calcula **pensión y seguro juntos**, cruza con
   préstamos, y genera **una orden individual de pensión** por el remanente.
3. Acumula `totalSeguroGeneral` y al final, **fuera del bucle**, genera **UNA sola orden agregada
   al proveedor** del seguro (`:858` → `generarOrdenPagoProveedorSeguro`).

**`CRD.PGPC` es una fila por jubilado y por período** — con `PGPCVLPN` (pensión), `PGPCVLSG`
(seguro), `PGPCESTD` y las FK a las órdenes. **No existe ninguna cabecera de corrida:** no hay
dónde decir «el seguro de agosto ya se corrió, las pensiones todavía no».

### ⛔ Lo que hace que esto NO sea partir un método en dos

Decisión del usuario del 2026-09-04, textual: *«el seguro médico es un valor que debe bajar también
de la pensión […] pero ese valor no debe ir incluido en el valor a pagar al partícipe, sino debe
salir como un pago aparte al TITULAR con un RUC»*.

O sea: **el seguro se descuenta de la pensión del jubilado, pero se le paga al proveedor.** Separar
los procesos en el tiempo **crea una dependencia**: la corrida de pensiones de fin de mes tiene que
saber qué seguro se pagó al inicio para descontarlo, y descontar **exactamente** eso.

---

## 2. Las cuatro decisiones del usuario (2026-09-07)

| # | Decisión | Consecuencia |
|---|---|---|
| **D1** | **El valor del seguro se fija al inicio** y la corrida de pensiones lo descuenta **tal cual, sin recalcular** | Es lo único que garantiza que lo descontado al jubilado y lo pagado al proveedor sean el **mismo número** |
| **D2** | Correr pensiones sin el seguro del mes **se bloquea** con un mensaje claro | Evita pagar una pensión sin descontar el seguro — plata que después no se recupera |
| **D3** | El seguimiento vive en una **cabecera de corrida por período**, tabla nueva | Es lo que permite bloquear (D2), reintentar y auditar quién corrió qué y cuándo |
| **D4** | **Cada proceso usa el padrón de su propia fecha** | Puede haber alguien con seguro pagado y sin pensión, o al revés. Es esperado, y hay que **mostrarlo**, no esconderlo |

---

## 3. La tabla nueva — `CRD.CRJB`

**Código reservado el 2026-09-07.** Verificado libre contra las más de 400 tablas del modelo y
contra el §3 del `REGISTRO-RESERVAS-EQUIPOS.md`. Una fila por **empresa + año + mes**.

| Columna | Tipo | Qué guarda |
|---|---|---|
| `CRJBCDGO` | `NUMBER` PK, IDENTITY | Código |
| `PJRQCDGO` | `NUMBER` | Empresa (jerarquía). FK a la que ya usan `ASNT` y `PLNN` |
| `CRJBANNO` / `CRJBMESS` | `NUMBER` | Período |
| `CRJBESSG` | `NUMBER` | Estado del proceso de **seguro**: 0 pendiente · 1 generado |
| `CRJBFCSG` | `TIMESTAMP` | Cuándo se generó el seguro |
| `CRJBUSSG` | `VARCHAR2(50)` | Quién |
| `CRJBVLSG` | `NUMBER(18,2)` | Total pagado al proveedor |
| `CRJBIDSG` | `NUMBER` | Id de la orden de pago al proveedor |
| `CRJBCTSG` | `NUMBER` | Cuántos jubilados entraron en el seguro |
| `CRJBESPN` | `NUMBER` | Estado del proceso de **pensiones**: 0 pendiente · 1 generado |
| `CRJBFCPN` / `CRJBUSPN` | `TIMESTAMP` / `VARCHAR2(50)` | Cuándo y quién |
| `CRJBVLPN` | `NUMBER(18,2)` | Total de órdenes de pensión generadas |
| `CRJBVLCR` | `NUMBER(18,2)` | Total cruzado a préstamos |
| `CRJBCTPN` | `NUMBER` | Cuántos jubilados entraron en la pensión |

**Índice único por `(PJRQCDGO, CRJBANNO, CRJBMESS)`** — un período, una corrida. Es lo que hace
que el proceso sea idempotente por construcción y no por convención.

> ⛔ **El DDL va ANTES del WAR.** Toda columna `@Column` entra en el `SELECT` que genera Hibernate:
> si la entidad sube sin la tabla, revienta cualquier lectura. Es el incidente de `CBCRASRP` del
> 2026-08-31.

---

## 4. Los dos endpoints

### 4.1 `POST /rest/pgpc/seguro/generar` — inicio de mes

Cuerpo: `{ idEmpresa, anio, mes, usuario, idUsuario }` (el mismo que hoy recibe `generarPagosDelMes`).

1. Guards de precondición **del seguro solamente**: `verificarCuentaProductoPagoSeguroMedico`,
   el proveedor y su cuenta bancaria. **No** los de pensión.
2. Si `CRJB` de ese período ya tiene `CRJBESSG = 1` → `IncomeException`:
   `El seguro médico de {mes}/{anio} ya se generó el {fecha} por {usuario}. No se puede generar dos veces.`
3. Recorre el **padrón vigente a la fecha de ejecución** (D4). Por cada jubilado calcula su seguro
   y **crea o completa su fila de `CRD.PGPC`** del período escribiendo `PGPCVLSG`.
4. Genera **UNA orden agregada al proveedor** por el total — reusar
   `generarOrdenPagoProveedorSeguro` tal cual, **no reescribirla**.
5. Sella la cabecera: `CRJBESSG = 1`, fecha, usuario, total, id de la orden, conteo.

### 4.2 `POST /rest/pgpc/pensiones/generar` — fin de mes

Mismo cuerpo. Es el `generarPagosDelMes` de hoy **menos** el seguro, **más** la guarda.

1. **⛔ Guard D2, lo primero de todo:** si `CRJB` del período no existe o tiene `CRJBESSG = 0` →
   `IncomeException`:
   `No se puede generar las pensiones de {mes}/{anio}: el seguro médico de ese mes todavía no se ha generado. Ejecute primero el proceso de seguro médico.`
2. Si `CRJBESPN = 1` → `IncomeException` con el mismo formato que 4.1.
3. Los guards de precondición de pensión que ya existen, tal cual.
4. Recorre el **padrón vigente a fin de mes** (D4). Por cada jubilado:
   - **NO recalcula el seguro.** Lee `PGPCVLSG` de la fila del período (D1) y lo descuenta.
   - **Si el jubilado no tiene fila de seguro** (entró después del inicio de mes): su pensión se
     genera **sin descuento de seguro**, y entra en el resumen como
     `SIN_SEGURO_DEL_PERIODO` — **nunca se le inventa un seguro ni se le bloquea el pago**.
   - Cruza con préstamos y genera la orden individual, como hoy.
5. **NO genera ninguna orden al proveedor.** Eso ya lo hizo el proceso 4.1.
6. Sella la cabecera: `CRJBESPN = 1`, fecha, usuario, totales, conteo.

### 4.3 `GET /rest/pgpc/corrida/{anio}/{mes}?idEmpresa=` — seguimiento

Devuelve la cabecera del período, o los dos estados en `0` si no existe todavía. **200 con estados
en cero, nunca 404**: «este mes no se corrió nada» es una respuesta válida.

```json
{
  "anio": 2026, "mes": 9, "idEmpresa": 1236,
  "seguro":   { "estado": 1, "nombreEstado": "GENERADO", "fecha": "2026-09-01T08:12:04",
                "usuario": "mlopez", "total": 4820.00, "jubilados": 178, "idOrdenPago": 9931 },
  "pensiones":{ "estado": 0, "nombreEstado": "PENDIENTE", "fecha": null,
                "usuario": null, "total": null, "cruzadoAPrestamos": null, "jubilados": null },
  "puedeGenerarSeguro": false,
  "puedeGenerarPensiones": true,
  "conSeguroSinPension": 0,
  "conPensionSinSeguro": 0
}
```

- `nombreEstado` lo resuelve el **backend**. El frontend no traduce números.
- `puedeGenerarSeguro` / `puedeGenerarPensiones` los calcula el backend con **la misma regla** que
  aplican los endpoints. Si la pantalla la reimplementa, se van a desincronizar.
- `conSeguroSinPension` / `conPensionSinSeguro` son la consecuencia visible de D4. Son
  **información, no error**.

---

## 5. Lo que NO cambia

- **`generarOrdenPagoProveedorSeguro` no se toca.** El proceso de seguro la llama tal cual.
- **El cálculo de la pensión, el cruce con préstamos y la contabilidad no cambian.** Se mueve
  *cuándo* corre cada cosa, no *qué* calcula.
- **`/previsualizarCorrida` se mantiene**, y debería poder previsualizar cada proceso por separado.
- **`CRD.PGPC` no cambia de estructura**: `PGPCVLSG` ya existe. Lo que cambia es **quién la escribe
  primero** — antes la fila nacía completa en la corrida única, ahora nace en el proceso de seguro
  y se completa en el de pensiones.

---

## 6. ⛔ Lo que hay que mirar dos veces al implementar

1. **`generarPagosDelMes` no se borra todavía.** Los dos endpoints nuevos salen de él, pero el
   viejo se deja marcado como `@Deprecated` con un javadoc que diga cuál lo reemplaza. Borrarlo en
   el mismo cambio deja sin salida a cualquiera que lo esté llamando.
2. **El aislamiento por jubilado se conserva.** Hoy cada jubilado corre en su propia transacción
   (`REQUIRES_NEW`) y un error suyo no aborta el lote. Los dos procesos nuevos mantienen eso: es lo
   que evitó que una corrida entera se cayera por un dato malo.
3. **H46 sigue vivo.** El movimiento negativo de `CRD.APRT` sobrevive a cualquier reverso: si una
   corrida falla a mitad, el reintento informa «al día» y no le paga a nadie. **Respaldo antes de
   correr cada mes**, y ahora son dos corridas, no una.
4. **El `(PGPC n)` de la referencia bancaria** sigue como está (`API-PAGO-PENSION-COMPLEMENTARIA.md`).

---

## 7. Verificación de aceptación

1. Correr **solo** el seguro → `CRJBESSG = 1`, una orden al proveedor, `PGPCVLSG` escrito en cada
   jubilado, y `puedeGenerarPensiones = true`.
2. Correr el seguro **dos veces** → la segunda falla nombrando fecha y usuario de la primera.
3. Correr pensiones **sin** seguro → falla con el mensaje de D2, **sin generar ni una orden**.
4. Correr pensiones **después** del seguro → las órdenes descuentan **exactamente** el `PGPCVLSG`
   ya pagado, no un recálculo.
5. Un jubilado que entra **después** del proceso de seguro → cobra pensión sin descuento y aparece
   en `conPensionSinSeguro`.
6. Un jubilado que sale **antes** de fin de mes → tiene seguro pagado y no tiene pensión, y aparece
   en `conSeguroSinPension`.
7. `GET /corrida/{anio}/{mes}` de un mes sin correr → **200** con los dos estados en `0`.

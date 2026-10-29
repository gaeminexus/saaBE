# API — Abono a capital con aportes del partícipe

**Estado:** CONTRATO CONGELADO, sin implementar (2026-10-12) · **Dueño:** `omen-saa-1`
**Diseño y decisiones:** `docs/logica-negocio/crd/DISENO-ABONO-CAPITAL-CON-APORTES.md` (AA1–AA8, solo en saaBE).
**Espejo:** `saaFE/docs/crd/API-ABONO-CAPITAL-CON-APORTES.md`. Si los dos difieren, manda el de saaBE.

Base: `/SaaBE/rest`. Las fechas viajan como `yyyy-MM-dd`, nunca como `Date` crudo ni con `Z`.

---

## 0. Qué cambia, en una tabla

| Cómo paga el socio | Camino | ¿Aprueba contabilidad? | Endpoint |
|---|---|---|---|
| 100% depósito | CBCR `ABONO_CAPITAL` | Sí | `POST /cbcr/registrar` — **sin cambios** |
| Depósito + aportes | CBCR `ABONO_CAPITAL` con `detalles[0].aportes` | Sí | `POST /cbcr/registrar` — **NUEVO: ahora admite `aportes`** |
| 100% aportes | Directo, se aplica en el acto | No | `POST /prst/abonarCapitalConAportes` — **NUEVO** |

- `POST /prst/abonarCapital` **sigue respondiendo 409.** No se reabre.
- La simulación no cambia. Se simula con el **total** del abono (depósito + aportes): `GET /prst/simularAbonoCapital/{idPrestamo}?valor={total}&modalidad={1|2}`.
- Los tipos de aporte y su saldo salen de `GET /aprt/saldosPorEntidad/{idEntidad}`, igual que en la precancelación.

---

## 1. Abono mixto por CBCR — `POST /cbcr/registrar`

Es el mismo cuerpo que el abono de hoy, más `aportes` en la única línea:

```json
{
  "idEntidad": 1234,
  "tipoOperacion": "ABONO_CAPITAL",
  "idCuentaBancaria": 7,
  "referencia": "DEP-998877",
  "rutaRespaldo": "crd/cobros/....pdf",
  "valor": 300.00,
  "fecha": "2026-10-12",
  "fechaAfectacion": "2026-10-12",
  "observacion": null,
  "usuario": "jperez",
  "detalles": [{
    "idPrestamo": 5555,
    "valor": 300.00,
    "modalidad": 1,
    "aportes": [ { "idTipoAporte": 11, "valor": 150.00 },
                 { "idTipoAporte": 9,  "valor": 50.00 } ]
  }]
}
```

**Reglas:**
- **`valor` (cabecera y línea) es SOLO el depósito**, igual que en la precancelación mixta. El abono total es `valor + Σ aportes`; en el ejemplo, 500,00.
- El depósito sigue siendo **obligatorio y > 0**, con cuenta bancaria, referencia y respaldo. Si el socio paga todo con aportes, el abono **no va por aquí**: va por §2.
- `aportes` se valida al registrar con las mismas reglas que la precancelación:
  - lista no vacía;
  - cada `valor` > 0;
  - sin tipos repetidos;
  - tipo vigente;
  - saldo suficiente, con tolerancia de 0,01.
- `aportes` **sigue rechazándose** en `PAGO_CUOTA`, `PAGO_MULTIPLE`, `COBRO_MIXTO`, `REGISTRO_APORTE` y los demás tipos. Ahora se admite **solo en `PRECANCELACION` y `ABONO_CAPITAL`**.
- **El saldo de aportes NO se reserva al registrar.** Al **procesar** se valida otra vez el saldo y se vuelve a simular el abono con el total. Si alguna de las dos cosas falla, el cobro pasa a **RECHAZADO** con usuario `SISTEMA` y el motivo, y `procesar` responde `procesado=false`. Es el mismo comportamiento que la precancelación mixta.
- Al procesar se consume el aporte: se escribe un APRT negativo en estado CONSUMIDO y un `PagoAporte` enlazado al pago del abono.
- Si se **anula o reversa** el cobro, el aporte **se restituye** con un APRT positivo de tipo REVERSO.
- **Respuestas:** las mismas de `/cbcr/registrar` de hoy (`API-COBROS-APROBACION-CONTABILIDAD.md`). Un desglose inválido da 422 con el motivo en `mensaje`.

### 1.1 La bandeja ve los aportes — `GET /cbcr/getId/{id}`

La respuesta suma un campo nuevo. Los dos que ya tenía no cambian:

```json
{
  "cabecera": { ... },
  "detalle": [ ... ],
  "aportesConsumidos": [
    { "idDetalleCobro": 901, "idTipoAporte": 11, "nombreTipoAporte": "CESANTIA", "valor": 150.00 },
    { "idDetalleCobro": 901, "idTipoAporte": 9,  "nombreTipoAporte": "JUBILACION", "valor": 50.00 }
  ]
}
```

- Un cobro sin aportes devuelve `"aportesConsumidos": []`, nunca `null`.
- El campo también aparece en las precancelaciones mixtas, que hasta hoy tampoco mostraban sus aportes.

### 1.2 Editar y reenviar un cobro rechazado — `POST /cbcr/{id}/reenviar`

Al corregir un abono o una precancelación mixta rechazada, se borran los aportes anteriores de la línea y se graban los que trae la corrección. Hoy se pierden, o el reenvío revienta por la FK.

---

## 2. Abono 100% aportes — `POST /prst/abonarCapitalConAportes`

```json
{
  "idEmpresa": 1,
  "idPrestamo": 5555,
  "modalidad": 1,
  "aportes": [ { "idTipoAporte": 11, "valor": 500.00 } ],
  "usuario": "jperez",
  "observacion": "Abono con cesantía",
  "fecha": "2026-10-12",
  "fechaAfectacion": "2026-10-12",
  "rutaDocumentoRespaldo": null
}
```

**Reglas:**
- **No hay campo `valor`.** El abono es `Σ aportes`; mandarlo aparte solo abriría la puerta a que no cuadre.
- `idEmpresa`, `idPrestamo`, `modalidad` (1 o 2), `aportes` (no vacío) y `usuario` son obligatorios.
- `fechaAfectacion` sigue las reglas de la precancelación directa (`API-FECHA-AFECTACION-COBRO.md` §2bis): obligatoria, no menor a `fecha`, no futura, y en un período abierto.
- `rutaDocumentoRespaldo` es **opcional**. No entra dinero de afuera, así que no hay comprobante bancario.
- Las validaciones del abono son las mismas de la simulación: préstamo al día, abono menor al capital pendiente, y la cuota nueva tiene que cubrir el interés.
- `reproceso` (opcional, D-048): con `true`, "al día" se evalúa a la fecha del abono (`fecha`; si falta, `fechaAfectacion`) y no con el estado de hoy. Ver `ABONO-CAPITAL-AL-DIA-A-FECHA.md`.
- Se aplica en el acto. Genera **un solo asiento**: Debe las cuentas de aporte; Haber el capital por banda; más el re-bandeo del saldo que queda.
- El abono se anula con `POST /prst/anularOperacion` `{ idEvento, usuario, motivo }`, igual que un pago con aportes. La anulación restaura la tabla, restituye el aporte y anula el asiento.

**200 OK:**
```json
{
  "exito": true,
  "etapa": "APLICACION",
  "mensaje": "Abono a capital con aportes aplicado por $500.00. Plazo: 48 → 41 cuotas.",
  "resultado": { /* ResultadoAbonoCapital: el mismo objeto que hoy devuelve el abono procesado por CBCR */
    "idPrestamo": 5555, "idEvento": 7777, "idPagoPrestamo": 8888, "valorAbono": 500.00,
    "modalidad": 1, "plazoAnterior": 48, "plazoNuevo": 41, "cuotaAnterior": 210.33, "cuotaNueva": 210.33,
    "cuotasHistorizadas": 41, "cuotasGeneradas": 41, "...": "..." },
  "movimientosAporte": [ { "idTipoAporte": 11, "valor": -500.00, "...": "..." } ]
}
```
`movimientosAporte` tiene la misma forma que en `POST /prst/pagarConAportes`.

**Errores.** Todos con `{ exito:false, etapa, mensaje, error }`:

| HTTP | `error` | Cuándo | Qué hace la pantalla |
|---|---|---|---|
| 400 | `PARAMETRO_INVALIDO` o el mensaje | Falta un campo obligatorio, modalidad fuera de 1/2, o `aportes` vacío | Muestra el mensaje |
| 400 | `FECHA_AFECTACION_OBLIGATORIA` / `FECHA_AFECTACION_MENOR_A_PAGO` / `FECHA_AFECTACION_FUTURA` / `PERIODO_CERRADO` | Fecha de afectación inválida | Muestra el mensaje |
| 404 | `PRESTAMO_NO_ENCONTRADO` | — | Muestra el mensaje |
| 422 | `SALDO_APORTES_INSUFICIENTE` / `TIPO_APORTE_NO_VIGENTE` | El desglose no se puede financiar | Recarga los saldos y muestra el mensaje |
| 422 | `PRESTAMO_NO_AL_DIA` | Hay una cuota vencida o parcial | Ofrece «Pagar cuotas» (`ir-a-pagar`), igual que hoy |
| 422 | `ABONO_CUBRE_CAPITAL` | El abono cubre todo el capital | Ofrece «Precancelar» (`ir-a-precancelar`), igual que hoy |
| 422 | `CUOTA_NO_CUBRE_INTERES`, `SIN_CUOTAS_PENDIENTES`, `POLIZA_SIN_CUOTAS_EN_VIGENCIA` | Las mismas reglas del abono de hoy | Muestra el mensaje |
| 500 | — | Imprevisto | Muestra el mensaje |

---

## 2bis. Abono sobre una tabla que ya lo refleja — `regenerarTabla: false` (C16b, 2026-10-29)

**Para qué:** registrar por el motor un abono a capital cuando la tabla vigente **ya está
re-amortizada** con ese abono (tablas migradas de Delta). El abono de siempre historizaría las
pendientes y generaría una tabla nueva, que rebajaría el capital **dos veces**. Origen: D-030 del
cuadre Superintendencia, préstamo 8052 (`cuadreSuper/03-trabajo/meses/2025-06/sql/21b-propuesta-destino-8052.md`).

**Qué hace:**
- No toca ninguna cuota ni el plazo ni el valor de la cuota.
- Crea el evento `ABONO_CAPITAL` con `modalidad = 0`, suma el valor a `DTPRSLOT` de la cuota
  indicada y crea el `PagoPrestamo` con `PGPRSLOT = valor` y `PGPRCPPG = 0`, igual que el abono de
  siempre. La observación lleva `[C16b abono sin regenerar tabla, cuota #N]`.
- **Cuota:** `numeroCuotaAbono` (CBCR) o `numeroCuota` (directo). Sin ella, la cuota **vigente a la
  fecha del abono**: la de mayor vencimiento `<=` fecha (no depende del estado de hoy de las cuotas).
- **Validación de cuadre:** después de aplicarlo, `Σ DTPRCPTL` (todas las cuotas) `+ Σ DTPRSLOT` tiene
  que ser igual a `PRSTMNSL`, con tolerancia de 0,01. Si no cuadra, se rechaza con
  `ABONO_NO_CUADRA_CON_TABLA` y un mensaje con el capital de la tabla, los abonos ya registrados, el
  abono y cuánto falta o sobra. En CBCR se valida al **registrar** y otra vez al **procesar**.
- **Asiento (CBCR):** CBCRASN2 de siempre, D cuenta del banco / H capital por banda. No hay cuotas
  historizadas, así que el capital se reparte, proporcional al capital, entre las cuotas que **vencen
  después de la fecha del abono** (`haberDesdePagos`). Es el capital futuro que la tabla re-amortizada
  ya no cobra. El cierre de apertura (`capitalFuturoPosteriorACorte`) usa el mismo reparto. No hay
  asiento de re-bandeo, porque la tabla no cambió.
- **Camino directo 100% aportes** (`POST /prst/abonarCapitalConAportes` con `"regenerarTabla": false`,
  `numeroCuota` opcional y sin `modalidad`): un solo asiento, D aportes / H capital por banda, con el
  cierre de apertura y **sin re-bandeo**.
- **Anulación** (`POST /prst/anularOperacion`): anula el pago y resta el `DTPRSLOT`. No hay cuotas
  que restaurar, y el plazo y la cuota quedan iguales.
- **Sin DDL:** en `CRD.DCBC.DCBCMDLD` se guarda `0` = sin regenerar en la cuota vigente y `-N` = sin
  regenerar en la cuota N. Las modalidades 1 y 2 siguen siendo positivas.
- Por defecto (`regenerarTabla` ausente o `true`) el comportamiento no cambia.
- **Cartera al corte (C14b, 2026-10-29):** el `PGPRSLOT` del pago `ABONO_CAPITAL` **no** cuenta como capital
  pagado de la cuota donde se registra (cierre, G48 y CCPM al corte); la cuota solo descuenta su
  `PGPRCPPG`. El abono ya está en el capital de la tabla, como reducción del saldo por vencer. En 8052 la cuota 42
  queda con sus 445,72 vencidos. Detalle: `CIERRE-CARTERA-A-FECHA-DE-CORTE.md` §1.1.1.

**Ejemplo, préstamo 8052 (D-030):** tabla 43–83 re-amortizada por Delta desde ago-2025; el abono de
1.289,54 se registra en la cuota 42 con el dinero del 13-jun-2025 (Pacífico ahorros, cuenta 418, NUT
68607). Antes hay que borrar el pago migrado 243479 con su `DTPRSLOT` de 1.289,54 (D-026). Si no se
borra, el abono quedaría contado dos veces y el cuadre lo rechaza.

```json
{
  "idEntidad": 7401,
  "tipoOperacion": "ABONO_CAPITAL",
  "idCuentaBancaria": 418,
  "referencia": "68607",
  "rutaRespaldo": "crd/cobros/2025-06/8052-abono-13jun2025.pdf",
  "valor": 1289.54,
  "fecha": "2025-06-13",
  "fechaAfectacion": "2025-06-13",
  "observacion": "D-030: abono de la tabla re-amortizada por Delta (lectura 2 de 18c), dinero del 12/13-jun-2025",
  "usuario": "USUARIO 1",
  "detalles": [{
    "idPrestamo": 8052,
    "valor": 1289.54,
    "regenerarTabla": false,
    "numeroCuotaAbono": 42
  }]
}
```

Con la base del 29-oct-2026 este cobro se **rechaza al registrar**, y el rechazo es correcto. Con el
pago migrado borrado (`DTPRSLOT` = 0), queda capital 36.491,74 + abono 1.289,54 = 37.781,28 contra un
monto de 38.230,70: **faltan 449,42**. Es la parte de la re-amortización que ningún abono registra
(`21b` §2). Mientras D-030 no lleve esos 449,42 a la tabla, el abono no cuadra. D-030 los deja como
préstamo vencido hasta el kardex de crédito.

**SELECT de control** (antes y después; sin datos personales):

```sql
SELECT p.PRSTCDGO, p.PRSTMNSL AS MONTO,
       SUM(d.DTPRCPTL) AS CAPITAL_TABLA, SUM(NVL(d.DTPRSLOT, 0)) AS ABONOS_DTPRSLOT,
       ROUND(p.PRSTMNSL - SUM(d.DTPRCPTL) - SUM(NVL(d.DTPRSLOT, 0)), 2) AS DIFERENCIA,   -- después: 0
       (SELECT LISTAGG(g.PGPRCDGO || ':' || g.PGPRSLOT || ':' || NVL(g.PGPRANUL, 0) || ':' || g.EVPRCDGO, ', ')
                 WITHIN GROUP (ORDER BY g.PGPRCDGO)
          FROM CRD.PGPR g WHERE g.PRSTCDGO = p.PRSTCDGO AND NVL(g.PGPRSLOT, 0) <> 0) AS PAGOS_CON_SLOT,
       (SELECT COUNT(*) FROM CRD.EVPR e WHERE e.PRSTCDGO = p.PRSTCDGO AND e.EVPRTPOO = 'ABONO_CAPITAL'
           AND e.EVPRMDLD = 0 AND e.EVPRESTD = 1) AS ABONOS_SIN_REGENERAR
FROM   CRD.PRST p JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO
WHERE  p.PRSTCDGO = 8052
GROUP  BY p.PRSTCDGO, p.PRSTMNSL;
-- La tabla no cambia: mismo COUNT(*) y SUM(DTPRCPTL) antes y después; en HDTP no aparece nada del evento.
```

---

## 3. Trampas

- **`aportes` en `ABONO_CAPITAL` CONSUME saldo del socio.** Lo mismo pasa en `PRECANCELACION`. Es lo **opuesto** a una línea de aporte en `COBRO_MIXTO`, donde el socio entrega plata y su saldo sube (`API-COBROS-APROBACION-CONTABILIDAD.md`).
- **En CBCR, `valor` es solo el depósito.** Si el FE manda el total, el banco recibe un asiento por plata que nunca entró.
- **La simulación se hace con el total**, no con el depósito. Simular con el depósito muestra una tabla que no es la que se va a aplicar.
- El saldo de aportes que se ve al registrar puede cambiar antes de que contabilidad procese. Un rechazo por `SISTEMA` en ese momento **no es un error**: es la guarda.

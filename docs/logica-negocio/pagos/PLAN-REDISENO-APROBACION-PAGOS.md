# Rediseño del circuito de pagos: solicitud sin cuenta, aprobación con cuenta

**Punto 14 del listado del 2026-08-27** · Tamaño XL · **Fecha:** 2026-08-27
**Rol de este documento:** diseño y plan de fases, escrito por el orquestador. El DDL y los prompts salen de aquí.

---

## 1. Lo que pide el negocio

> *"Pueden llegar solicitudes de pago de CxP, RRHH y créditos. La idea es que desde cualquier módulo se pueda receptar la solicitud de pago y en la pantalla de aprobaciones se escoja una cuenta bancaria y todos los pagos que se van a realizar con esa cuenta; el sistema debe validar si alcanza el saldo. Que se aprueben ahí los pagos y de ahí se generen los archivos para pagar o que se pueda procesar manualmente. También ahí se indicaría si se paga con cheque o transferencia, y en caso de cheque que se genere el cheque. Y que permita seleccionar varios pagos a incluir en una sola transferencia."*

Cuatro cambios, en orden de dependencia:

1. La **cuenta bancaria deja de elegirse al registrar** y se elige **al aprobar**.
2. La aprobación **valida el saldo** de esa cuenta contra el total seleccionado.
3. La **forma de pago se decide al aprobar**, y con cheque se gira el cheque ahí.
4. Varios pagos al **mismo beneficiario y cuenta destino** pueden ir en **una sola transferencia** del archivo del banco.

---

## 2. Lo que ya existe (verificado en el código)

| Pieza | Estado |
|---|---|
| `PagoProgramado` (`PGS.PGTR`) como documento único de salida de dinero | ✅ lo usan CxP (facturas), tesorería (egresos), anticipos a proveedor, caja chica y CRD (devolución de aportes) |
| Origen externo (`OrigenPagoExterno`) para que otros módulos entreguen pagos sin que CxP los conozca | ✅ probado con caja chica |
| Pago con cheque, con giro y anulación por reverso | ✅ probado de punta a punta |
| Lote y archivo del banco (`LotePago`, `generarLote`, `procesarRespuestaBanco`) | ✅ funciona, con un formateador único |
| Saldo de una cuenta a una fecha (`CuentaBancariaServiceImpl.obtieneSaldoFecha`) | ✅ existe: cierre de `SaldoBanco` + movimientos del período |
| Estados: `REGISTRADO(1) · EN_ARCHIVO(2) · CONFIRMADO(3) · RECHAZADO(4) · ANULADO(5)` | ✅ |

**Lo que falta:** un estado anterior a `REGISTRADO` (la solicitud sin cuenta), que `PGTRCNBC` admita nulo, el endpoint de aprobación masiva, la validación de saldo y la agrupación por beneficiario.

**Entidades legadas que NO se usan** (verificado: sin llamadores fuera de su propio ServiceImpl): `AprobacionXProposicionPago`, `ProposicionPagoXCuota`, `AprobacionXMonto`, `MontoAprobacion`, `UsuarioXAprobacion`. **No reutilizarlas**: modelan un circuito de aprobación por montos y usuarios que nadie mantiene. Si más adelante se quiere aprobación por monto y perfil, se rediseña aparte.

---

## 3. Decisiones de diseño

### 3.1 Un estado nuevo: `POR_APROBAR = 0`
La solicitud nace **sin cuenta bancaria y sin forma de pago**. Se usa `0` y no `6` para que el orden numérico siga el ciclo de vida y las consultas por `estado >= 1` (pagos ya con cuenta) sigan valiendo.

Ciclo nuevo:
```
POR_APROBAR(0) --aprobar--> REGISTRADO(1) --lote--> EN_ARCHIVO(2) --respuesta--> CONFIRMADO(3)
                       \--(cheque o débito automático)--> CONFIRMADO(3)
```

### 3.2 Los cheques se confirman al APROBAR
Decisión del usuario. Cambia lo probado en la fase A: hoy el cheque se gira y contabiliza al **registrar**; pasará a girarse y contabilizarse al **aprobar**. Es coherente: el cheque se gira cuando tesorería decide pagarlo, no cuando el área lo solicita.
**Consecuencia:** las tres pantallas de origen (pagos CxP, egresos, anticipos) dejan de mostrar el selector de forma de pago y el aviso "se girará el cheque N° X". Todo eso se muda a la pantalla de aprobación.

### 3.3 Validación de saldo: informativa y bloqueante
Al elegir la cuenta, la pantalla muestra: **saldo disponible** (`obtieneSaldoFecha` a hoy), **comprometido** (suma de pagos ya aprobados de esa cuenta que aún no se confirman) y **disponible real** = saldo − comprometido. Si el total seleccionado lo supera, **la aprobación se rechaza** con el detalle de los tres números.
El comprometido es imprescindible: sin él, dos aprobaciones seguidas pasan la validación cada una por su lado y juntas sobregiran la cuenta.

### 3.4 Agrupación en una sola transferencia
Al generar el lote, los pagos con **el mismo beneficiario y la misma cuenta destino** se emiten como **una línea** del archivo, sumando los valores. El `LotePago` sigue teniendo N pagos; lo que se agrupa es la línea del archivo. Cada pago conserva su asiento y su aplicación: **no se fusionan documentos**, solo la instrucción al banco.
Requiere que el formateador reciba las líneas ya agrupadas y que la conciliación posterior sepa repartir un débito bancario entre N pagos — por eso va en la última fase.

### 3.5 Qué NO cambia
- El asiento y el movimiento bancario de cada pago siguen generándose igual, por pago.
- El reverso sigue siendo por pago.
- Los orígenes externos siguen entregando pagos con `registrarPagoDeOrigenExterno`; solo dejan de pasar cuenta.

---

## 4. Fases

### Fase 1 — DDL + backend del ciclo nuevo (M)
- `PGTRCNBC` pasa a nullable (columna y entidad).
- Estado `POR_APROBAR = 0` en `EstadoPagoProgramado` y en el rubro correspondiente.
- Los cuatro `registrar*` aceptan cuenta nula → nace `POR_APROBAR`. Las firmas actuales (con cuenta) siguen funcionando y nacen `REGISTRADO`, para no romper nada mientras el frontend migra.
- `POST /pgtr/aprobar` con `{idsPagos, idCuentaBancaria, formaPago, fechaPago, idUsuario}`: valida que todos estén `POR_APROBAR`, valida saldo, asigna cuenta y forma de pago, gira cheque si corresponde, y deja cada pago en `REGISTRADO` (transferencia) o `CONFIRMADO` (cheque o débito automático, contabilizando en el acto).
- `GET /pgtr/porAprobar?idEmpresa&origen&desde&hasta`: la bandeja, con origen (CxP, RRHH, CRD, caja chica), beneficiario, concepto, valor y fecha solicitada.
- `GET /pgtr/disponibilidad/{idCuenta}?fecha`: saldo, comprometido y disponible real.

### Fase 2 — Pantalla de aprobación (M)
Bandeja única con filtros por origen y fecha, selección múltiple, total seleccionado en vivo, selector de cuenta con los tres números de disponibilidad, selector de forma de pago (cheque solo si la cuenta maneja chequera) y botón aprobar. Al aprobar con cheque, muestra los números girados.
Las pantallas de origen dejan de pedir cuenta y forma de pago.

### Fase 3 — Agrupación en el archivo (S)
`generarLote` agrupa por beneficiario + cuenta destino al construir el archivo. Ajustar el formateador y documentar cómo se concilia un débito agrupado.

### Fase 4 — Orígenes nuevos (S cada uno)
RRHH entrega al circuito: nómina y anticipos a trabajadores (este último ya diseñado, ver `rhh/prompts/08`). CRD ya entrega devolución de aportes.

---

## 5. Riesgos

1. **Toca el archivo más caliente del sistema.** `PagoProgramadoServiceImpl` concentra cheques, caja chica, anticipos y liquidaciones. **No empezar hasta que la tanda actual esté en producción y estable.**
2. **Cambia un flujo recién probado** (cheque al registrar → al aprobar). Hay que volver a probar la fase A completa después.
3. **Pagos en vuelo durante el despliegue**: los que estén en `REGISTRADO` con cuenta ya asignada siguen su curso normal; el estado nuevo solo aplica a los que nazcan después. No hace falta migrar datos.
4. **La validación de saldo depende de `MovimientoBanco`**, que hoy acumula movimientos en tránsito que nadie cierra. **Conviene cerrar antes el trabajo de conciliación** (conectar el cierre de movimientos), o el "disponible" será optimista.

---

## 6. Dependencia con conciliación

El punto 12 (conciliación) y este comparten `MovimientoBanco`. El orden correcto es:

```
conciliación: conectar el cierre de movimientos  →  saldo confiable
                     ↓
rediseño de pagos: validación de disponibilidad
```

Hacerlo al revés significa validar saldo contra una cifra que arrastra tránsitos de meses.

**Actualizado el 2026-08-27:** el diagnóstico de arriba ya no es "arrastra tránsitos de meses" —
es más grave. `DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md` §7bis midió que `MovimientoBanco`
cubre entre el 1% y el 5% del movimiento real sobre cuentas bancarias (no un porcentaje alto con
algunos tránsitos sin cerrar). Cerrar la conciliación por sí sola no basta para que
`obtieneSaldoFecha` sea confiable: hace falta decidir si el saldo sale de ahí o de la contabilidad
(`PlanCuentaService.saldoCuentaFechaEmpresa`, que ya existe y ya se usa para el mayor auxiliar).
Ver §7bis para el detalle completo y la recomendación.

## 7. Estado de implementación

> **Actualizado el 2026-08-28 — el frente está CERRADO.** Fases 1 y 2 completas, más lo que este
> apartado listaba como "no implementado". **Fase 3 descartada por el usuario** (ver §7ter). El DDL
> `sql/01-aprobacion-pagos.sql` **ya corrió en producción** — resolvió un bloqueo real: el WAR
> desplegado ya no pedía cuenta al registrar, pero `PGTRCNBC` seguía `NOT NULL`, y cada registro de
> pago moría con `ORA-01400`. Lo de abajo se conserva como registro de la Fase 1; el estado
> consolidado del frente está en `../ESTADO-CXP-CXC-TSR-RHH-SRI.md`.

### 7.0 Fase 1 (2026-08-27)

**Backend hecho, en local.** DDL ya ejecutado
(`docs/logica-negocio/pagos/sql/01-aprobacion-pagos.sql`, relajó `PGTRCNBC`; sin cambios de
esquema adicionales, `PGTRESTD` no tiene CHECK ni FK).

- `EstadoPagoProgramado.POR_APROBAR = 0` agregado.
- Los cuatro `registrar*` (`registrarPago`, `registrarPagoDeEgreso`, `registrarPagoDeAnticipo`,
  `registrarPagoDeOrigenExterno`) aceptan `idCuentaBancariaOrigen = null` → el pago nace
  `POR_APROBAR`, sin cuenta ni forma de pago. Las llamadas con cuenta siguen naciendo
  `REGISTRADO` (o `CONFIRMADO` con cheque/débito automático) exactamente como antes — cero
  cambio de comportamiento para los llamadores actuales. `POST /pgtr` (REST) ya no exige
  `idCuentaBancariaOrigen`; las otras tres solo se invocan hoy internamente (egreso, anticipo,
  orígenes externos), sin endpoint REST propio, así que no hay otro punto de entrada que tocar.
- Cuando se conoce de una vez la cuenta destino del beneficiario (`idCuentaDestinoTitular`), se
  guarda igual aunque la cuenta de origen venga nula — no depende de ella.
- `GET /pgtr/porAprobar` y `POST /pgtr/aprobar` implementados — contrato exacto en §7.1 y §7.2.
- `validaDisponibilidad`, aislada en `PagoProgramadoServiceImpl`, conectada desde `aprobar` pero
  **no implementada a propósito**: activarla es fase 3, después de decidir de dónde sale el saldo
  bancario confiable (ver `docs/logica-negocio/tsr/DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md`
  §7bis — `obtieneSaldoFecha` hoy cubre 1-5% del movimiento real).

**No implementado en esta fase** (fuera de alcance, ver §4): `GET /pgtr/disponibilidad/{idCuenta}`
de la fase 1 original del plan (depende de la misma decisión de saldo que `validaDisponibilidad`),
la pantalla (fase 2), la agrupación por beneficiario en el archivo (fase 3).

### 7ter. Cierre del frente — 2026-08-28

Todo lo que §7.0 dejaba abierto quedó resuelto, salvo la Fase 3, que se descartó.

| Pieza | Estado |
|---|---|
| `validaDisponibilidad` real | ✅ Implementada. Lee el **saldo contable** vía `PlanCuentaService.saldoCuentaFechaEmpresa`, menos lo comprometido en `REGISTRADO`/`EN_ARCHIVO`. **No** usa `MovimientoBanco` |
| `GET /pgtr/disponibilidad/{idCuenta}` | ✅ Implementado y consumido por la pantalla (tres números: saldo, comprometido, disponible real) |
| Fase 2 — pantalla de aprobación | ✅ Cerrada. `tsr/forms/procesos/aprobacion-pagos/` — **movida de CxP a Tesorería** por decisión del usuario: quien aprueba, elige banco y gira cheque es tesorería |
| Origen `CXC_DEVOLUCION_CLIENTE` | ✅ Nuevo, no estaba en el plan original. `AnticipoClienteService.solicitarDevolucion` + `POST /antc/solicitarDevolucion`, con reconciliación de saldo (`sincronizarDevolucion`) y DDL `cxc/sql/add-anticipo-cliente-devolucion.sql` |
| Pantalla de respuesta del banco | ✅ Cerrada — la pestaña existía construida pero deshabilitada |
| **Fase 3 — agrupación por beneficiario** | ⛔ **DESCARTADA por el usuario (2026-08-28):** *"no debemos agrupar, está bien que salga una línea por cada pago, no debemos fusionar nada"*. No se implementa, y §3.4 queda como diseño no adoptado |

**La decisión de saldo de §6 ya está tomada y aplicada:** gana `saldoCuentaFechaEmpresa`
(contabilidad); `obtieneSaldoFecha` se renombró a `saldoSegunMovimientosBanco` y queda solo para
el circuito de conciliación. Ver `../tsr/DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md` §7bis.

**Riesgo cruzado con el equipo `crd`, avisado:** `CRD_DEVOLUCION_APORTE` puede registrar con
`idCuentaBancariaOrigen` nulo; esas devoluciones ahora quedan sujetas a la validación real de saldo
al aprobarse. No es regresión, es el efecto esperado — pero `crd` usa `PagoProgramadoServiceImpl`
sin tocarlo y necesita saberlo.

**Limitación heredada, no resuelta aquí:** el lector de la respuesta del banco
(`LectorRespuestaBancoExcelImpl`) sigue siendo **provisional** — espera un Excel de 4 columnas
armado a mano (id de pago, resultado, referencia, motivo), no el formato nativo del banco, que
todavía no se entregó. Confirmado con el usuario el 2026-08-28: la pantalla de confirmación manual
es el camino principal, no algo temporal.

### 7.1 `GET /pgtr/porAprobar`

```
GET /rest/pgtr/porAprobar?idEmpresa=1&origen=FACTURA_COMPRA&desde=2026-08-01&hasta=2026-08-31
```
`idEmpresa` obligatorio. `origen`, `desde`, `hasta` opcionales. `origen` acepta:
`FACTURA_COMPRA` · `EGRESO_TESORERIA` · `ANTICIPO_PROVEEDOR` (documentos propios de CXP) o
`CRD_DEVOLUCION_APORTE` · `TSR_CAJA_CHICA` · `RHH_ANTICIPO_EMPLEADO` (documentos de otro módulo,
ver `OrigenPagoExterno`) — se omite para traer todos los orígenes.

Respuesta 200 — proyección `PagoPorAprobar`, no la entidad:
```json
[
  {
    "id": 812,
    "origen": "FACTURA_COMPRA",
    "beneficiario": "DISTRIBUIDORA ACME S.A.",
    "concepto": "Factura 001-002-000456",
    "valor": 3500.00,
    "fechaSolicitada": "2026-08-20"
  },
  {
    "id": 815,
    "origen": "TSR_CAJA_CHICA",
    "beneficiario": "CAJA CHICA MATRIZ",
    "concepto": "Reposición caja chica agosto",
    "valor": 250.00,
    "fechaSolicitada": "2026-08-22"
  }
]
```
Respuesta 400 si falta `idEmpresa`. Lista vacía (200) si no hay pagos `POR_APROBAR`.

### 7.2 `POST /pgtr/aprobar`

Solicitud:
```json
{
  "idsPagos": [812, 815, 820],
  "idCuentaBancaria": 4,
  "formaPago": 2,
  "fechaPago": "2026-08-27",
  "idUsuario": 5
}
```
`formaPago`: `2` Transferencia, `3` Cheque, `4` Débito automático (`1` Efectivo no soportado —
mismo mensaje de error que en `registrar*`). Todos los pagos de `idsPagos` deben estar
`POR_APROBAR`: si uno solo no lo está, **no se aprueba ninguno** (falla completo, con el detalle
de cuáles). `fechaPago` opcional (vacío = hoy). Con `formaPago=3` la cuenta debe manejar
chequera, mismo `IncomeException` accionable que ya usan los `registrar*`.

Respuesta 200 (formaPago=2, transferencia — quedan `REGISTRADO`):
```json
{
  "exito": true,
  "idCuentaBancaria": 4,
  "formaPago": 2,
  "totalAprobado": 3750.00,
  "pagosAprobados": 3,
  "registrados": [812, 815, 820],
  "confirmados": [],
  "mensaje": "Se aprobaron 3 pago(s) por un total de $3750.00."
}
```
Respuesta 200 (formaPago=3, cheque — quedan `CONFIRMADO`, cheque girado y contabilizado por
cada pago):
```json
{
  "exito": true,
  "idCuentaBancaria": 4,
  "formaPago": 3,
  "totalAprobado": 3750.00,
  "pagosAprobados": 3,
  "registrados": [],
  "confirmados": [812, 815, 820],
  "cheques": [
    { "pago": 812, "numeroCheque": "001234", "asiento": "AS-2026-04512" },
    { "pago": 815, "numeroCheque": "001235", "asiento": "AS-2026-04513" },
    { "pago": 820, "numeroCheque": "001236", "asiento": "AS-2026-04514" }
  ],
  "mensaje": "Se aprobaron 3 pago(s) por un total de $3750.00."
}
```
`formaPago=4` (débito automático) responde igual que cheque pero sin el bloque `cheques`.

Respuesta 400 — texto plano (mismo estilo que el resto del módulo), casos típicos: algún pago no
está `POR_APROBAR`, `idsPagos`/`idCuentaBancaria`/`formaPago` faltantes, cuenta sin chequera con
`formaPago=3`, cheque ya tomado por otro usuario (misma protección de índice único que
`registrar*`).


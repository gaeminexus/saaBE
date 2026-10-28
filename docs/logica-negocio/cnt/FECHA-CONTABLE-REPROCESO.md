# Fecha contable explícita para re-procesar meses pasados (C6)

**Proyecto:** cuadre Superintendencia ASOPREP (`cuadreSuper/01-seguimiento/CAMBIOS-SAA.md`, cambio C6).
**Rama:** `cuadre/c6-fecha-contable`. **Fecha:** 2026-10-28. **Estado:** código compilado
(`mvn -q compile` OK), pendiente de prueba en WildFly contra la base del proyecto.

## 1. Problema y patrón adoptado

La contabilidad ya acepta una fecha: `AsientoContableService.generarAsiento(idEmpresa, tipo,
LocalDate fechaAsiento, …)` recibe la fecha del asiento y `AsientoServiceImpl.saveSingle` deriva de
ella mes, año y período (`CNT.PRDO`), validando que el período exista y no esté mayorizado. El
problema estaba en los **motores que llaman con la fecha del sistema** y en los **movimientos
operativos** fechados con `now()`.

La auditoría (grep de `LocalDate.now()`, `LocalDateTime.now()`, `new Date()`, `SYSDATE` sobre
`src/main/java/com/saa/ejb`: 513 ocurrencias en 135 archivos) clasifica cada uso en:

- **Fecha de negocio** (fecha del pago, del asiento, de la orden, del movimiento bancario): tiene
  que poder venir de afuera. Es lo que se parametriza acá.
- **Auditoría / registro** (`fechaRegistro`, `fechaIngreso`, `fechaModificacion`, `fechaAnulacion`,
  `fechaAprobacion`, `fechaCierre`, `fechaSeguro`, `fechaPensiones`, sellos de corrida): **se deja
  como está**, es la hora en que de verdad se ejecutó.
- **Validaciones "no futura"** (`isAfter(LocalDate.now())`): se dejan, son correctas también en
  re-proceso (una fecha de 2025 nunca es futura).

**Patrón:** cada proceso tiene **una sola fecha de negocio** y esa fecha es la contable. Donde el
proceso ya la recibía (`fecha`, `fechaPago`, `fechaAfectacion`, `fechaAcreditacion`,
`PRDNFCCN`), se usa esa y no se agrega otra. Donde no la recibía, se agrega un parámetro
**opcional** `fechaContable` (`LocalDate`, `yyyy-MM-dd`) en el REST y en el método de servicio
(sobrecarga; la firma vieja delega con `null`): **sin el parámetro, el comportamiento es el de
siempre (hoy)**; con él, se propaga a la fecha del movimiento operativo y a `generarAsiento`.
Se valida "no futura" en el motor; que el período esté abierto lo valida `saveSingle`.

## 2. Auditoría por motor

Leyenda de la columna *Estado*: **HECHO** = cambio en esta rama · **YA** = ya recibía la fecha de
negocio antes de C6, verificado en código, sin cambio · **NO APLICA** = no genera contabilidad
alcanzable o la fecha es de auditoría.

| # | Motor | Clase / método | Dónde se fijaba la fecha (antes) | Fecha que usa en re-proceso | Estado |
|---|---|---|---|---|---|
| 1 | **Pensión complementaria** (jubilados) | `PagoPensionComplementariaServiceImpl.generarPagoParaJubilado` / `generarSeguroIndividual` | `fecha = min(fin de mes del período, hoy)` → `PGPC.PGPCFCHA`, `PGPR.PGPRFCHA` (cruce), `APRT`, devengo | La del período (`fechaDelPeriodo(anio, mes)`); para jun-2025 es **30-jun-2025** | YA |
| 1 | | `generarAsientoDevengoPension` | `pago.getFecha()` | ídem | YA |
| 1 | | `generarAsientoSeguroMedico` | **`LocalDate.now()`** (línea 2838) — único asiento del motor fechado hoy | `fechaDelPeriodo(anio, mes)` | **HECHO** |
| 1 | | `generarOrdenPagoPension`, `generarOrdenPagoProveedorSeguro` | `LocalDate.now()` como `fechaProgramada` de la orden en CXP (`PGTR.PGTRFPRG`) | `PGPCFCHA` / `fechaDelPeriodo` (= hoy si el período es el mes en curso, así §6bis no cambia) | **HECHO** |
| 1 | | `corrida.setFechaSeguro/FechaPensiones`, `fechaRegistro` | `now()` | auditoría | NO APLICA |
| 2 | **Pago manual de cuotas** | `ProcesoPagoPrestamoServiceImpl.pagarCuota` / `pagarMultiplesCuotas` | `solicitud.fechaPago` → `validarFechaNoFutura` → `EVPR.EVPRFCHA`, `PGPR.PGPRFCHA`, `ctx.fechaPago` | `fechaPago` (asiento: `ContabilidadPrestamoServiceImpl.contabilizarPagoCuota` usa `ctx.getFechaPago()`) | YA |
| 2 | **Cobro con depósito** (bandeja) | `CobroCreditoServiceImpl` | `CBCR.CBCRFCHA` (fecha de pago) + `CBCRFCAF` (fecha de afectación, obligatoria) — `API-FECHA-AFECTACION-COBRO.md` | `fechaAfectacion` para todos los asientos | YA |
| 2 | **Pago con aportes** | `ProcesoPagoPrestamoServiceImpl.pagarConAportes` | `solicitud.fechaPago` → `PGPR`, `APRT`, `PGAP.fechaContable` | `fechaPago` | YA |
| 3 | **Precancelación** | `ProcesoPagoPrestamoServiceImpl.precancelar` | `solicitud.fecha` (pago) + `solicitud.fechaAfectacion` (opcional, = `fecha`) | `fechaAfectacion` para el asiento, `fecha` para PGPR/mora | YA |
| 3 | **Abono a capital** | `AbonoCapitalPrestamoServiceImpl.abonar*` | `solicitud.fecha` + `solicitud.fechaAfectacion` | ídem | YA |
| 2-3 | **Motor de pagos** (compartido) | `MotorPagoPrestamoServiceImpl.aplicarPago` | `ctx.getFechaPago()` → `DTPR.DTPRFCPG` | fecha del contexto | YA |
| 2-3 | | `calcularSaldosRealesCuota` (autocorrección) y `recalcularCuotaDesdePagos` (reverso) | **`LocalDateTime.now()`** (líneas 229 y 765) cuando la cuota queda PAGADA sin `DTPRFCPG` | Mayor `PGPRFCHA` de los pagos vigentes (`fechaUltimoPago`); `now()` sólo si ningún pago trae fecha | **HECHO** |
| 2-3 | | `recalcularCuotaDesdePagos` línea 774: EN_MORA vs PENDIENTE según `fechaVencimiento < hoy` | estado operativo a hoy, no fecha contable | NO APLICA |
| 2-3 | | `pago.setFechaRegistro`, `evento.setFechaRegistro`, `prestamo.setFechaModificacion` | auditoría | — | NO APLICA |
| 4 | **Devolución de aportes** | `DevolucionAporteServiceImpl.registrarDevolucion` / `registrarParaBeneficiarios` | `solicitud.fecha` → `resolverFecha` (no futura) → `DVAP.DVAPFCHA`, `APRT`, `PGAP.fechaContable`, orden CXP (`fecha.toString()`), asiento de reclasificación (`devolucion.getFecha()`) | `fecha` | YA |
| 4 | | `regenerarOrden…` (línea 1414) | `LocalDate.now()` para la orden re-emitida | fecha de re-emisión (hecho nuevo) | NO APLICA |
| 5 | **Desembolso** (aprobar préstamo) | `ContabilidadPrestamoServiceImpl.contabilizarEntrega` | `prestamo.getFechaInicio()` (`PRST.PRSTFCIN`) | `PRSTFCIN` | YA |
| 5 | | `PrestamoServiceImpl.aprobar` → orden de pago en CXP | **`LocalDate.now()`** como `fechaProgramada` (línea 460) | `SolicitudDecisionPrestamo.fechaContable` (opcional, no futura); sin ella, hoy | **HECHO** |
| 5 | | `prestamo.setFechaAprobacion` | auditoría | — | NO APLICA |
| 5 | | `anular` → `reversionAsiento(id, LocalDate.now())` | reverso fechado hoy | hecho nuevo (la anulación ocurre hoy) | NO APLICA |
| 6 | **Nómina — rol y provisiones** | `ContabilizacionNominaServiceImpl.contabilizarRol` / `contabilizarProvisiones` | `fechaContable(periodo)` = `PRDN.PRDNFCCN`, si no `PRDNFCHF` (fin del período), si no hoy | `PRDNFCCN` del período (se fija en el período, `PUT /prdn`) | YA |
| 6 | **Nómina — pago** | `ContabilizacionNominaServiceImpl.contabilizarPago` (vía `GeneracionOrdenPagoServiceImpl.confirmar`) | `fechaAcreditacion` (si null, hoy) → asiento y `RDPG.RDPGFCAC` | `fechaAcreditacion` | YA |
| 6 | **Nómina — cierre del período** | `ProcesoNominaServiceImpl` 558/763: `fechaAprobacion`, `fechaCierre = LocalDate.now()`; 857/1165/1783/1816 `fechaRegistro` | auditoría; el cierre no genera asiento | — | NO APLICA |
| 7 | **CxP — documentos** | `ProcesoCargaDocumentosServiceImpl` (`registrarBD`, recontabilizar, reembolso), `FacturaCompraServiceImpl` | `fc.getFecha()` (fecha del documento) | fecha del documento | YA |
| 7 | **CxP — aplicación de NC**, caja chica | `AplicacionPagoCxpServiceImpl` 445 (`fecha` del body), 1178 (`movimiento.getFecha()`) | ídem | YA |
| 7 | **CxP — pagos: aprobar** | `PagoProgramadoServiceImpl.aprobar` | `fechaPago` del body → `PGTRFPRG`; con cheque/débito `PGTRFRSP = fechaPago` y contabiliza en el acto | `fechaPago` | YA |
| 7 | **CxP — pagos: confirmar manual** | `confirmarPagosManual` / `confirmarUnPagoManual` | `fechaPago` del body → `PGTRFRSP` → asiento (`contabilizarSegunOrigen`) y `MVCB` | `fechaPago` | YA |
| 7 | **CxP — pagos: respuesta del banco** | `procesarRespuestaBanco` | **`LocalDate.now()`** → `PGTRFRSP` (línea 2036) → asiento y `MVCB` de cada pago confirmado | Sobrecarga `procesarRespuestaBanco(idLote, archivo, idUsuario, LocalDate fechaRespuesta)`; query param `fechaContable`; sin él, hoy | **HECHO** |
| 7 | **CxP — anticipos** | `AnticipoProveedorServiceImpl` 386 | `fechaPago` (si null, hoy) | `fechaPago` | YA |
| 7 | | `pago.setFechaRegistro`, `lote.setFechaGeneracion/FechaRegistro` | auditoría | — | NO APLICA |
| 8 | **CxC — documentos** | `FacturaServiceImpl`, `NotaCreditoServiceImpl`, `NotaDebitoServiceImpl`, `RetencionServiceImpl`, `RetencionV2ServiceImpl`, `LiquidacionCompraServiceImpl` | `documento.getFecha().toLocalDate()` | fecha del documento | YA |
| 8 | **CxC — cobros y anticipos** | `AplicacionPagoCxcServiceImpl.registrarCobroTransferencia` (644, `fechaCobro`), aplicación de anticipo (502, `fechaAplicacion`), NC/ND/retención (`fechaDe(doc.getFecha())`) | fecha del body / del documento | YA |
| 8 | | `AnticipoClienteServiceImpl` 1069: devolución al cliente, orden CXP con `LocalDate.now()` | se confirma en CXP con `fechaPago` | NO APLICA (ver §5) |
| 9 | **Tesorería — ingresos** | `IngresoServiceImpl.procesarIngreso` | `fecha` del body (`parseFecha`, hoy si falta) → asiento, `INGR.INGRFCHA`, `MVCB` | `fecha` | YA |
| 9 | **Tesorería — egresos** | `EgresoServiceImpl.procesarEgreso` (+ con anticipo) | `fecha` del body → `EGRS.EGRSFCHA`, `PGTRFPRG`; el asiento sale al confirmar el pago (CxP, fila 7) o en el acto con `fechaEgreso` (con anticipo / débito automático) | `fecha` + `fechaPago` de la confirmación | YA |
| 9 | **Tesorería — movimientos bancarios** (conciliación) | `MovimientoBancoServiceImpl.crearMovimientoPorCobro`, `creaMovimientoPorDeposito`, `creaMovimientoPorTransferencia` (ingresos, egresos, pagos CXP, pensiones), `creaMovimientoPorCheque` | **`LocalDate.now()`** → `MVCB.MVCBFRGS`, aunque período/mes/año ya salían del asiento. La conciliación consulta `MVCB` por esa columna (`BETWEEN`, `<= :fecha`) | `asiento.getFechaAsiento()` (`fechaDelAsiento`); hoy si el asiento no trae fecha | **HECHO** |
| 9 | **Tesorería — transferencias entre cuentas, débitos/créditos bancarios, cobros de caja, depósitos, cierre de caja** | `TransferenciaServiceImpl`, `DebitoCreditoServiceImpl`, `CobroServiceImpl`, `DepositoServiceImpl`, `CierreCajaServiceImpl`, `ChequeServiceImpl`, `PagoServiceImpl` → `AsientoServiceImpl.insertarCabeceraAsiento` / `generaCabeceraTransferencia` (`LocalDate.now()` + `Calendar.getInstance()`) | Circuito legado: sus REST (`/trns`, `/dbcr`, `/cbro`, `/dpst`, `/crcj`) sólo exponen CRUD; ningún REST llama a estos métodos de proceso | NO APLICA (sin punto de entrada) |
| 9 | **Conciliación — cierre en tránsito** | `ConciliacionCierreServiceImpl.cerrar` | `cierre.setFecha(now)`, `fechaCierre`, `partida.fechaRegistro` | el período viene por `idPeriodo`; no genera asientos | NO APLICA |
| 10 | **Cierre mensual de cartera** | `CierreCarteraServiceImpl.ejecutar` → `calcular` → `generaAsiento` | `sub.getFecha()` derivada de `solicitud.anio/mes` (`fechaCorte` = último día del mes, `fechaProceso` = primer día del siguiente); `ahora` sólo para `fechaRegistro`/`fechaModificacion` | la del período de la solicitud | YA (verificado) |
| — | **Petro** (`asoprep/**`, `ProcesoCargaPetroServiceImpl`, `CobroPetroContableServiceImpl`) | — | fecha al último día del mes de la carga | — | fuera de C6 (C4) |
| — | **Mora diaria** (`ProcesoMoraPrestamoServiceImpl`), **provisión de intereses** (`ProvisionInteresServiceImpl`) | `corteDelDia(LocalDate)`, `fecha` por parámetro | ya reciben fecha; se corren desde el cierre de cartera | YA |

## 3. Cómo se invoca cada proceso con fecha contable (30-jun-2025 como ejemplo)

Todas las fechas van como `yyyy-MM-dd` (regla del repo: `LocalDate` sin hora ni zona). Base
`/SaaBE/rest`.

### 3.1 Pensión complementaria — la fecha sale del período, no hay parámetro nuevo

```http
POST /pgpc/seguro/generar
{ "idEmpresa": 1236, "anio": 2025, "mes": 6, "usuario": "cuadre" }

POST /pgpc/pensiones/generar
{ "idEmpresa": 1236, "anio": 2025, "mes": 6, "usuario": "cuadre" }
```

Con `anio=2025, mes=6`, `fechaDelPeriodo` = 30-jun-2025: `PGPCFCHA`, el devengo, el asiento del
seguro, los pagos del cruce (`PGPR`), los aportes (`APRT`) y la `fechaProgramada` de las órdenes
en CXP quedan en junio 2025. Las órdenes se confirman después en CXP con `fechaPago` (3.6).

### 3.2 Pago manual de cuota / múltiple / con aportes

```http
POST /prst/pagarCuota
{ "idPrestamo": 12345, "valor": 150.00, "fechaPago": "2025-06-30",
  "idEmpresa": 1236, "usuario": "cuadre", "observacion": "re-proceso jun-2025" }
```

`fechaPago` fecha `EVPR`, `PGPR`, `DTPRFCPG` y el asiento. Mismo campo en
`/prst/pagarMultiplesCuotas` (por renglón) y `/prst/pagarConAportes`.

### 3.3 Cobro con depósito (bandeja), precancelación y abono a capital

```http
POST /cbcr/registrar          { ..., "fecha": "2025-06-30", "fechaAfectacion": "2025-06-30" }
POST /prst/precancelar        { "idPrestamo": 12345, "fecha": "2025-06-30",
                                "fechaAfectacion": "2025-06-30", "idEmpresa": 1236, "usuario": "cuadre", ... }
POST /prst/abonarCapital      { "idPrestamo": 12345, "valor": 500.00, "modalidad": 1, "fecha": "2025-06-30",
                                "fechaAfectacion": "2025-06-30", "idEmpresa": 1236, "usuario": "cuadre" }
POST /prst/abonarCapitalConAportes   (mismos campos de fecha + "aportes": [...])
```

`fecha` = fecha de pago (PGPR, mora, bandas); `fechaAfectacion` = fecha de todos los asientos
(obligatoria en CBCR, opcional y por defecto `fecha` en precancelación y abono con aportes).
Detalle en `crd/API-FECHA-AFECTACION-COBRO.md`.

### 3.4 Devolución de aportes

```http
POST /dvap/registrar
{ "idEntidad": 777, "idEmpresa": 1236, "idUsuario": 5, "usuario": "cuadre",
  "fecha": "2025-06-30", "motivo": "...", "tipos": [...], "valores": [...] }
```

### 3.5 Desembolso (aprobar préstamo) — **nuevo** `fechaContable`

```http
POST /prst/aprobar/12345
{ "usuario": "cuadre", "idEmpresa": 1236, "idUsuario": 5,
  "observacion": "re-proceso jun-2025", "fechaContable": "2025-06-30" }
```

`fechaContable` (opcional) fecha la orden de pago del desembolso (`PGTRFPRG`). El asiento de
entrega va con `PRSTFCIN`: para que caiga en junio 2025 el préstamo tiene que tener esa fecha de
inicio. La orden se confirma luego en CXP con `fechaPago` (3.6).

### 3.6 CxP — confirmación de pagos (donde de verdad se contabiliza el egreso)

```http
POST /pgtr/aprobar
{ "idsPagos": [9001, 9002], "idCuentaBancaria": 4, "formaPago": 2,
  "fechaPago": "2025-06-30", "idUsuario": 5 }

POST /pgtr/confirmarManual
{ "idsPagos": [9001, 9002], "referencia": "REPROCESO-JUN25",
  "fechaPago": "2025-06-30", "idUsuario": 5 }

POST /pgtr/lote/{idLote}/respuesta?idUsuario=5&fechaContable=2025-06-30     (**nuevo** query param)
  Content-Type: application/octet-stream  (archivo de respuesta del banco)
```

`fechaPago` / `fechaContable` → `PGTRFRSP` → fecha del asiento del pago y de su `MVCB`.
Con `formaPago` cheque (3) o débito automático (4) la aprobación contabiliza en el acto con
`fechaPago`.

### 3.7 Nómina

```http
PUT  /prdn                      (período con "fechaContable": "2025-06-30"  → PRDNFCCN)
POST /prdn/contabilizar/{idPeriodo}?usuario=cuadre
POST /prdn/contabilizarProvisiones/{idPeriodo}?usuario=cuadre
POST /rdpg/confirmar/{idOrden}  { "fechaAcreditacion": "2025-06-30", "usuario": "cuadre", "idUsuario": 5 }
```

Si `PRDNFCCN` es nulo se usa `PRDNFCHF` (fin del período); un período de junio 2025 cae en
junio sin tocar nada.

### 3.8 CxC, tesorería (ingresos/egresos) y cierre de cartera

```http
POST /aplc/cobroTransferencia   { "idFactura": 1, "valor": 10.0, "fechaCobro": "2025-06-30", "idCuentaBancaria": 4, "idEmpresa": 1236, "idUsuario": 5 }
POST /ingr/procesar             { "idEmpresa": 1236, "idProductoCobro": 3, "valor": 45.10, "fecha": "2025-06-30", "idCuentaBancaria": 4, "idUsuario": 5, "descripcion": "..." }
POST /egrs/procesar             { "idEmpresa": 1236, "idProductoPago": 7, "valor": 80.00, "fecha": "2025-06-30", "idCuentaBancariaOrigen": 4, "debitoAutomatico": true, "idUsuario": 5, "descripcion": "..." }
POST /cierrecartera/ejecutar    { "idEmpresa": 1236, "anio": 2025, "mes": 6, "usuario": "cuadre", ... }
```

Facturas, NC, ND y retenciones (CxC y CxP) se contabilizan con la **fecha del documento**.

## 4. SELECT de control por motor (criterio de aceptación: filas y asiento en junio 2025)

Base del proyecto `saa-oracle-cuadre`, sólo `SELECT`. El asiento se verifica siempre igual:

```sql
-- Asiento en el período de junio 2025
SELECT A.ASNTCDGO, A.ASNTFCHA, A.ASNTPRDO AS MES, P.PRDOANNN, P.PRDOMSSS, A.ASNTESTD
  FROM CNT.ASNT A JOIN CNT.PRDO P ON P.PRDOCDGO = A.PRDOCDGO
 WHERE A.ASNTCDGO = :idAsiento;          -- esperado: ASNTFCHA = 30/06/2025, PRDOANNN 2025, PRDOMSSS 6
```

| Motor | Filas a verificar |
|---|---|
| Pensión complementaria | `SELECT PGPCCDGO, PGPCFCHA, PGPCANNO, PGPCMESS, PGPCNMDV, PGPCIDPG FROM CRD.PGPC WHERE PGPCANNO=2025 AND PGPCMESS=6` → `PGPCFCHA` = 30/06/2025; `PGPCNMDV` = asiento de junio. Seguro: `SELECT CRJBASSG FROM CRD.CRJB WHERE CRJBANNO=2025 AND CRJBMESS=6` → asiento de junio. Órdenes: `SELECT PGTRCDGO, PGTRFPRG, PGTRFRSP, PGTRASNT FROM PGS.PGTR WHERE PGTRIDOR IN (...)` → `PGTRFPRG` = 30/06/2025 |
| Pago manual / precancelación / abono | `SELECT EVPRCDGO, EVPRTPOO, EVPRFCHA, EVPRNMAS FROM CRD.EVPR WHERE PRSTCDGO=:p ORDER BY EVPRCDGO DESC`; `SELECT PGPRCDGO, PGPRFCHA FROM CRD.PGPR WHERE EVPRCDGO=:e`; `SELECT DTPRNMCT, DTPRESTD, DTPRFCPG FROM CRD.DTPR WHERE PRSTCDGO=:p` → `DTPRFCPG` = 30/06/2025 en las cuotas pagadas (ya no la fecha de la corrida) |
| Cobro con depósito | `SELECT CBCRCDGO, CBCRFCHA, CBCRFCAF FROM CRD.CBCR WHERE CBCRCDGO=:c` y sus asientos (columnas `CBCRASN*`) en junio |
| Devolución de aportes | `SELECT DVAPCDGO, DVAPFCHA, DVAPNMAS, DVAPNMRC FROM CRD.DVAP WHERE DVAPCDGO=:d` → `DVAPFCHA` = 30/06/2025, `DVAPNMRC` (reclasificación) asiento de junio; orden en `PGS.PGTR` con `PGTRFPRG` = 30/06/2025 |
| Desembolso | `SELECT PRSTCDGO, PRSTFCIN, PRSTIDST, PRSTIDPG FROM CRD.PRST WHERE PRSTCDGO=:p` (asiento de entrega en la columna `idAsientoEntrega` del préstamo); `SELECT PGTRFPRG FROM PGS.PGTR WHERE PGTRCDGO=:PRSTIDPG` → 30/06/2025 |
| CxP pagos | `SELECT PGTRCDGO, PGTRFPRG, PGTRFRSP, PGTRASNT, PGTRESTD FROM PGS.PGTR WHERE PGTRCDGO IN (...)` → `PGTRFRSP` = 30/06/2025, `PGTRASNT` asiento de junio; `SELECT APLPFAPL, APLPASNT FROM PGS.APLP WHERE PGTRCDGO=:pg` |
| CxP documentos | asiento de la factura (`PGS.FCCM`/documento) con `ASNTFCHA` = fecha del documento |
| CxC | `SELECT APLCFAPL, APLCASNT FROM CBR.APLC WHERE ...` → `APLCFAPL` = 30/06/2025 y asiento de junio |
| Nómina | `SELECT PRDNCDGO, PRDNFCCN, PRDNFCHF, PRDNASNT, PRDNASPR, PRDNASPG FROM RHH.PRDN WHERE PRDNCDGO=:p`; `SELECT RDPGFCAC, ASNTCDGO FROM RHH.RDPG WHERE RDPGCDGO=:o` → los tres asientos en junio |
| Tesorería ingresos / egresos | `SELECT INGRFCHA, INGRASNT FROM TSR.INGR WHERE INGRCDGO=:i`; `SELECT EGRSFCHA, EGRSASNT FROM TSR.EGRS WHERE EGRSCDGO=:e` |
| Movimientos bancarios | `SELECT MVCBCDGO, MVCBFRGS, ASNTCDGO, PRDOCDGO FROM TSR.MVCB WHERE ASNTCDGO=:a` → `MVCBFRGS` = `ASNTFCHA` del asiento (30/06/2025), mismo período |
| Cierre de cartera | `SELECT CRCTCDGO, CRCTANOO, CRCTMESS, CRCTFCCR FROM CRD.CRCT WHERE CRCTANOO=2025 AND CRCTMESS=6`; `SELECT ANCCTPOO, ANCCFCHA, ANCCASNT FROM CRD.ANCC WHERE CRCTCDGO=:c` → `ANCCFCHA` en junio (apertura: 01/07/2025 por diseño) |

## 5. Lo que quedó sin cambiar y por qué

1. **Sellos de auditoría** (`fechaRegistro`, `fechaIngreso`, `fechaModificacion`, `fechaAnulacion`,
   `fechaAprobacion`, `fechaCierre`, `fechaSeguro`, `fechaPensiones`, `fechaGeneracion` del lote):
   dicen cuándo se corrió el proceso. Cambiarlos a la fecha contable destruiría la trazabilidad
   del re-proceso.
2. **Reversos y anulaciones fechados hoy** (`reversionAsiento(id, LocalDate.now())` al anular un
   préstamo, `regenerarOrden` de devolución, `revertirPagoConfirmado` sin `fechaReverso`): son
   hechos nuevos; además `revertirConfirmado` ya acepta `fechaReverso` cuando hace falta.
3. **Estado EN_MORA/PENDIENTE a hoy** al reconstruir una cuota (motor, línea 774): es el estado
   operativo vigente; la fecha contable no lo cambia.
4. **Circuito legado de tesorería** (transferencias entre cuentas, débitos/créditos bancarios,
   cobros de caja, depósitos, cierre de caja): fecha el asiento con `LocalDate.now()` y el período
   con `Calendar.getInstance()` dentro de `AsientoServiceImpl.insertarCabeceraAsiento` /
   `generaCabeceraTransferencia`, **pero ningún REST invoca esos métodos** (sus recursos sólo
   exponen CRUD). Si alguna vez se necesitara para el cuadre, el cambio es agregar una
   sobrecarga con `LocalDate` a esos dos métodos de `AsientoServiceImpl` (`cnt`, fuera del
   alcance acordado para C6, que no toca `cnt` salvo lo imprescindible).
5. **Órdenes de pago de origen externo que siguen naciendo con `fechaProgramada = hoy`**:
   devolución de anticipo a cliente (`AnticipoClienteServiceImpl` 1069) y re-emisión de orden de
   devolución de aportes. La contabilidad del pago sale al confirmarlo en CXP con `fechaPago`, así
   que no afecta el cuadre; se dejan para no tocar flujos que el re-proceso no usa.
6. **Petro** (C4, otro agente) y **AsientoRest / AsientoContableServiceImpl** (C5): no se tocaron.
   No hizo falta nada de ellos.
7. **`PagoPensionComplementariaServiceImpl.generarPagosDelMes`** (proceso único, deprecado): hereda
   los cambios de las órdenes porque comparte `generarOrdenPagoProveedorSeguro` y
   `generarOrdenPagoPension`.

## 6. Decisiones que conviene confirmar

- **`MVCB.MVCBFRGS` = fecha del asiento** (antes: fecha del sistema). Es lo que hace que la
  conciliación de un mes re-procesado encuentre sus movimientos; en operación normal el asiento se
  fecha con la fecha de negocio que eligió el usuario (hoy casi siempre), así que el efecto es
  nulo o mínimo. Si tesorería prefiere conservar "fecha en que se registró" habría que agregar
  una columna nueva para la fecha del movimiento y migrar las consultas del DAO.
- **Órdenes de pago de pensión/seguro fechadas con la fecha del período** cuando el período es
  pasado (antes siempre hoy, §6bis de 2026-09-04). Para el mes en curso no cambia nada.
- **Desembolso:** el asiento de entrega sigue con `PRSTFCIN`; `fechaContable` sólo gobierna la
  orden de pago. Si se quisiera que `fechaContable` también mande sobre el asiento, es un cambio
  de una línea en `contabilizarEntrega` (hoy recibe el préstamo, no la fecha).

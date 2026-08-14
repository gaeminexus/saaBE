# Plan Técnico — Sistema de Pagos/Cobros de Facturas (CXP y CXC)

> ⚠️ **Nota:** este documento es una **fotografía del plan al 2026-08-07**, generada
> durante la sesión de análisis. Los números de línea y estados descritos corresponden
> al código de ese día — **verificar contra el código fuente antes de confiar en un
> número de línea o en un "ya existe / no existe"**. El requerimiento de negocio está
> en `REQUERIMIENTO-PAGOS-COBROS.md` (misma carpeta).
> Los puntos sin definir están marcados como **PENDIENTE**.

## Contexto

Las facturas de compra (PGS.FCTC) y de venta (CBR.FCTR) generan contabilidad al
autorizarse/registrarse, pero los abonos que reducen su saldo (retenciones, NC/ND,
anticipos, transferencias) solo se reflejan en las cuentas contables — **no en la vida
de la factura**. Hay que registrar cada abono como "aplicación de pago" para que el
saldo del módulo y el contable nunca difieran.

**Ya existe en BD y modelo** (script `docs/scripts/sql-pagos-facturas.sql`, YA aplicado
en BD): entidades `AplicacionPagoCxc` (CBR.APLC) y `AplicacionPagoCxp` (PGS.APLP) con
tipoDocPago (1=directo, 2=NC, 3=retención, 4=anticipo), montoAplicado, FKs cruzadas,
estado (1=Activo/2=Reversado); campos `estadoPago` en ambas facturas (FCTREPAG/FCTCEPAG,
recalculados por triggers de BD); anticipos ANTC/ANTP completos con REST que suman a
`PersonaCuentaContable.saldoInicial` (PRCC tipoCuenta=2). **Falta**: DAO/Service/REST de
las aplicaciones, hooks en la generación de asientos, cruce de anticipos, transferencias
+ archivo bancario, y huecos de modelo (sin FK para ND ni para RetenciónV2 /
RetencionCompraV2).

### Decisiones del usuario (fijas)
- **D1**: la aplicación se crea en la MISMA transacción que el asiento del documento
  (atómico: sin aplicación no hay asiento).
- **D2**: Nota de Débito = tipoDocPago **5** con montoAplicado **negativo** (aumenta el saldo).
- **D3**: factura referenciada inexistente ⇒ **bloquear todo** (CXC: validar antes de
  firmar/enviar al SRI; CXP carga: validación bloqueante 422).
- **D4**: cruce de anticipos **por valor** contra el saldo global `PRCC.saldoInicial`
  (pasa a ser "saldo actual"); aplicación tipo 4 con FK anticipo NULL; ANTC/ANTP son solo
  historial de entradas; crear registro documental "SALDO-INICIAL-MIGRADO" sin asiento.
- **D5**: pantallas de tesorería solo ofrecen transferencia y cruce de anticipo.
- **D6**: pagos CXP por transferencia: pago programado → lote → archivo TXT al banco
  (formato **PENDIENTE**, servicio formateador aislado) → respuesta del banco →
  confirmados generan aplicación+asiento+MovimientoBanco; rechazados quedan en seguimiento.
- **D7**: cobros CXC por transferencia: pantalla registra valor/fecha/nro
  transferencia/cuenta receptora → aplicación tipo 1 + asiento + MovimientoBanco ingreso.
  Pagos parciales múltiples.
- **D8**: anular un documento revierte sus aplicaciones (estado=2; triggers recalculan
  estadoPago).
- **D9**: implementar FASE CXP primero, pero TODO el DDL en un solo script.
- **D10 (arquitectura)**: la lógica de negocio vive **solo en los servicios del backend**.
  La base de datos únicamente almacena: **sin triggers ni procedimientos**. El script v2
  elimina los 4 triggers que había creado el v1 (`TRG_APLC_ESTADOPAGO`,
  `TRG_APLP_ESTADOPAGO`, `TRG_ANTC_INIT_SALDO`, `TRG_ANTP_INIT_SALDO`) y esa lógica pasa a
  `AplicacionPagoCxpService.recalcularEstadoPago(...)` y a los servicios de anticipos.
- **D11 (2026-08-11; SUPERADA para proveedores por D14 el 2026-08-14)**: los anticipos
  (a proveedores y a clientes) **NO** se conectaban al circuito de lote/archivo/respuesta
  del banco de `PagoProgramado`; generaban su asiento contable de forma síncrona al
  registrarse. Sigue vigente **solo para anticipos de clientes**
  (`AnticipoClienteServiceImpl.procesarAnticipo`), que son cobros (dinero que entra) y no
  tienen nada que enviar al banco. Para los anticipos a proveedores rige D14. En cambio, para los **pagos/cobros de
  facturas por transferencia** (`PagoProgramado`, §4) el usuario indicó que debe existir
  un asiento al **generar** el pago y otro al **confirmarlo** — hoy solo existe el de
  confirmación; ese cambio queda **PENDIENTE de definir** (ver ítem 5 en §7: qué cuentas
  usaría el asiento "al generar" y en qué paso exacto del flujo se dispara).
- **D12 (2026-08-12, confirmado)**: existen pagos a proveedores que el banco **debita
  automáticamente** por convenio. Esos pagos ya se ejecutaron antes de llegar al sistema:
  no se aprueban ni se seleccionan para el archivo del banco. Se marcan con
  `PGS.PGTR.PGTRDBAT=1` y al registrarlos, en la misma transacción, abonan la factura
  (APLP tipo 1 con `formaPago=4`), generan el asiento (mismas cuentas que la
  transferencia: DEBE CxP proveedor / HABER banco) y el MovimientoBanco de egreso
  (`TRANSFERENCIAS_DEBITOS_EN_TRANSITO` / origen `PAGOS`, igual que los pagos
  confirmados, para no partir el criterio de conciliación). Nacen en estado
  CONFIRMADO; al revertirse quedan ANULADOS, porque un débito ya ejecutado no se
  reprograma.
- **D13 (2026-08-12, confirmado)**: ingresos y egresos de tesorería **sin documento
  físico** (administración de cuentas, comisiones, intereses) en tablas nuevas
  `TSR.EGRS` / `TSR.INGR` (script `docs/scripts/sql-ingresos-egresos-tesoreria.sql`) —
  las legadas `TSR.DBCR`/`PGSS`/`CBRO` quedan como histórico (sus métodos de negocio no
  están expuestos por REST). La contrapartida contable sale del **grupo del producto**
  CXP/CXC elegido (`GrupoProducto*.planCuenta`), no se configura por registro. Los
  **egresos SÍ pasan por el circuito de pagos**: `PGS.PGTR.PGTRFCTC` pasa a nullable y
  se agrega `PGTREGRS` (FK excluyente con la factura); registrar el egreso crea su pago
  (`EgresoServiceImpl.procesarEgreso` → `registrarPagoDeEgreso`), y al confirmarse
  (respuesta del banco o débito automático D12) se genera el asiento
  (DEBE grupo del producto / HABER banco, `TipoAsientos.EGRESO_TESORERIA`=5), el
  MovimientoBanco de egreso y el egreso queda PAGADO; la reversión lo devuelve a
  PENDIENTE_PAGO. Los pagos de egreso no crean `AplicacionPagoCxp` (no hay factura).
  Los **ingresos se registran ya recibidos** en un paso
  (`IngresoServiceImpl.procesarIngreso`: asiento DEBE banco / HABER grupo,
  `TipoAsientos.INGRESO_TESORERIA`=4, movimiento créditos en tránsito / origen Cobros).
  REST: `/egrs`, `/ingr`. Doc frontend: `INGRESOS-EGRESOS-TESORERIA.md`.
- **D14 (2026-08-14, confirmado; reemplaza a D11 para proveedores)**: los **anticipos a
  proveedor SÍ pasan por el circuito de pagos**, igual que los egresos de tesorería:
  se agrega `PGS.PGTR.PGTRANTP` (FK a `PGS.ANTP`, excluyente con `PGTRFCTC` y `PGTREGRS`;
  script `docs/scripts/sql-pago-anticipo-proveedor.sql`). Registrar el anticipo
  (`AnticipoProveedorServiceImpl.procesarAnticipo` → `registrarPagoDeAnticipo`) lo deja
  Ingresado con su pago Registrado; **la contabilidad se genera recién al confirmarse el
  pago** (respuesta del banco, confirmación manual o débito automático D12), y el circuito
  despacha el asiento **según el proceso que lo originó**: anticipo → asiento de ANTICIPO
  (DEBE cuenta de anticipos del proveedor PRCC tipo 2 / HABER banco,
  `TipoAsientos.ANTICIPOS_PROVEEDOR`=9, con la **fecha real del pago**), egreso → asiento
  de egreso, factura → aplicación de pago. Al confirmar también se genera el
  MovimientoBanco (`TRANSFERENCIAS_DEBITOS_EN_TRANSITO` / origen `PAGOS` — antes el
  anticipo no creaba movimiento bancario) y se acredita el saldo de anticipos del
  proveedor (`TSR.PRCC.saldoInicial`), que hasta entonces **no** está disponible para
  cruces. Estados `ANTPESTD`: 1=Ingresado, 2=Confirmado, 3=Anulado
  (`rubros/EstadoAnticipoProveedor`); la reversión del pago
  (`pgtr/revertirConfirmado`) anula asiento y movimiento, descuenta el PRCC y devuelve el
  anticipo a Ingresado. Anulación: `POST /antp/anular/{id}` (anula el pago Registrado
  junto con el anticipo; se bloquea con pago En archivo o Confirmado).
- **D15 (2026-08-14, corrección)**: el **cruce de anticipo con una factura**
  (`AplicacionPagoCxpServiceImpl.aplicarAnticipo` y su gemelo CXC) descontaba el saldo
  global (`TSR.PRCC.PRCCSLIN`) pero no dejaba rastro en la tabla de anticipos: el listado
  de movimientos no mostraba la resta. Ahora cada cruce inserta un **movimiento negativo**
  en `PGS.ANTP` (valor = -cruce, saldo = acumulado tras el cruce, estado Confirmado,
  asiento del cruce) y lo enlaza a la aplicación por `APLPANTP` (FK que existía y nunca se
  usaba). La reversión del cruce anula ese movimiento (estado 3) además de devolver el
  saldo global; las aplicaciones antiguas sin FK siguen reversando solo contra el PRCC.
  Backfill de los cruces previos al fix:
  `CORRECCION-MOVIMIENTO-CRUCE-ANTICIPO.md` (pendiente de ejecutar).
- **D16 (2026-08-14, confirmado)**: en la **confirmación manual** de pagos
  (`pgtr/confirmarManual`) el asiento se genera con la **fecha de respuesta** del pago:
  el parámetro `fechaPago` del request (la fecha real en que el banco ejecutó el pago), o
  la fecha actual si viene vacío. Se evaluó usar la fecha programada (`PGTRFPRG`) y se
  descartó el mismo día: la fecha programada es una intención, no la ejecución real.

---

## 1. Script SQL incremental — `docs/scripts/sql-pagos-facturas-v2.sql` (NUEVO)

Solo se genera; el usuario lo revisa y ejecuta en DBeaver. Bloques:

1. **ALTERs APLC/APLP**: `APLCNTDB`→CBR.NTDB, `APLCRCV2`→PGS.RCV2; `APLPNTDC`→PGS.NTDC,
   `APLPRTV2`→CBR.RTV2 (+FKs, +COMMENT tipo 5).
2. **Índices únicos Oracle** (los del script v1, líneas 197/265, usan `WHERE` de
   PostgreSQL → fallaron y no existen): recrear function-based:
   `CREATE UNIQUE INDEX UIX_APLC_RETENCION ON CBR.APLC (CASE WHEN APLCESTD=1 THEN APLCRTCM END);`
   + análogos APLCRCV2, APLPRTNC, APLPRTV2.
3. **CBR.LQCS**: `ADD LQCSASNT NUMBER(19)` + FK a CNT.ASNT (columna ESTADOPAGO ya existe en BD).
4. **Tablas nuevas** (códigos PGTR y LTPG verificados libres en PGS):
   - `PGS.LTPG` LotePago: LTPGCDGO PK (seq), empresa, cuenta origen CNBC, fecha
     generación, nombre archivo, path, total, cantidad, estado (1=GENERADO
     2=RESPUESTA_PROCESADA 3=ANULADO), observación, usuario, fechaRegistro.
   - `PGS.PGTR` PagoProgramado: PGTRCDGO PK (seq), empresa, facturaCompra NOT NULL,
     titular, cuentaBancaria origen (CNBC), cuentaDestino (CTBN), valor, fechaProgramada,
     lote (FK LTPG, null), estado (1=REGISTRADO 2=EN_ARCHIVO 3=CONFIRMADO 4=RECHAZADO
     5=ANULADO), referenciaBanco, fechaRespuesta, motivo, aplicacion (FK APLP),
     observación, usuario, fechaRegistro. Índices por factura/estado/lote/titular.
5. **Triggers: se ELIMINAN** (D10). El v1 creó 4 triggers; el v2 los borra. Además de ir
   contra la arquitectura, eran incorrectos: al ser `FOR EACH ROW` consultando con
   `SELECT SUM` la misma tabla que se modifica, fallaban con **ORA-04091 (mutating table)**
   en el UPDATE del estado — justo el camino de reversión. Su lógica pasa al backend:
   `recalcularEstadoPago` en el service de aplicaciones, e inicialización del saldo en
   `AnticipoClienteServiceImpl` / `AnticipoProveedorServiceImpl.saveSingle`.
6. **Saldo inicial migrado (D4)**: PL/SQL idempotente — por cada PRCC tipoCuenta=2 con
   saldoInicial>0, INSERT en PGS.ANTP (proveedor) o CBR.ANTC (cliente) con
   numeroDoc='SALDO-INICIAL-MIGRADO', sin asiento, `WHERE NOT EXISTS`.
7. **(PENDIENTE, bloque comentado)** `TSR.BEXT ADD BEXTCDIF VARCHAR2(10)` — código de
   institución financiera, casi seguro necesario para el TXT del banco (confirmar con el
   formato).

## 2. Constantes (rubros)

- `rubros/TipoDocPagoAplicacion.java` (NUEVO): COBRO_DIRECTO=1, NOTA_CREDITO=2,
  RETENCION=3, ANTICIPO=4, NOTA_DEBITO=5.
- `rubros/EstadoAplicacionPago.java` (NUEVO): ACTIVO=1, REVERSADO=2.
- `rubros/EstadoPagoProgramado.java` (NUEVO): REGISTRADO=1, EN_ARCHIVO=2, CONFIRMADO=3,
  RECHAZADO=4, ANULADO=5; y `EstadoLotePago`: GENERADO=1, RESPUESTA_PROCESADA=2, ANULADO=3.
- `rubros/TipoAsientos.java` (MOD): 4 constantes nuevas, ya con su codigoAlterno definido —
  salidas de dinero a proveedores (`APLICACION_ANTICIPO_PROVEEDOR`, `PAGO_TRANSFERENCIA_CXP`)
  usan **codigoAlterno 5 (TEGRESO)**; entradas de dinero de clientes
  (`APLICACION_ANTICIPO_CLIENTE`, `COBRO_TRANSFERENCIA_CXC`) usan **codigoAlterno 4 (TINGRESO)**.
  Son los mismos tipos que ya usan los anticipos de proveedor y de cliente.
- `model/cxp/NombreEntidadesCompra.java` (MOD): PAGO_PROGRAMADO, LOTE_PAGO.

## 3. Modelos JPA

- MOD `model/cxp/AplicacionPagoCxp.java`: +notaDebito (APLPNTDC→NotaDebitoCompra),
  +retencionV2 (APLPRTV2→com.saa.model.cxc.RetencionV2), NamedQueries por documento.
- MOD `model/cxc/AplicacionPagoCxc.java`: +notaDebito (APLCNTDB→NotaDebito),
  +retencionCompraV2 (APLCRCV2→com.saa.model.cxp.RetencionCompraV2).
- MOD `model/cxc/LiquidacionCompra.java`: +estadoPago (ESTADOPAGO), +asiento (LQCSASNT).
- NUEVOS `model/cxp/PagoProgramado.java` (PGS.PGTR) y `model/cxp/LotePago.java`
  (PGS.LTPG) según bloque 4 del script.

## 4. Servicios nuevos

### AsientoContableService/Impl (MOD) — 4 asientos nuevos + 1 overload
- `generarAsientoAplicacionAnticipoProveedor(idTitular, valor, idEmpresa, tipoAsiento, fecha, obs, usuario)`:
  DEBE PRCC proveedor tipo1 (CxP) / HABER PRCC proveedor tipo2 (Anticipos).
- `generarAsientoPagoTransferenciaCxp(idTitular, valor, idCuentaBancaria, ...)`:
  DEBE PRCC proveedor tipo1 / HABER CuentaBancaria.planCuenta.
  (Plantilla: `generarAsientoAnticipoProveedor`, impl línea ~659.)
- `generarAsientoAplicacionAnticipoCliente(...)`: DEBE PRCC cliente tipo2 / HABER PRCC cliente tipo1.
- `generarAsientoCobroTransferenciaCxc(...)`: DEBE CuentaBancaria.planCuenta / HABER PRCC cliente tipo1.
- Overload `generarAsiento(..., Long moduloSistemaH)` — el actual (línea ~433) hardcodea
  módulo CUENTAS_POR_COBRAR (~466-470).
- Reutiliza helpers privados existentes `obtenerCuentaPorTipo` (~737) y
  `obtenerCuentaProveedorPorTipo` (~821).

### AplicacionPagoCxpService/Impl + DAO (NUEVOS, patrón 5 capas)
Métodos clave:
- `aplicarRetencionEmitida(RetencionV2, Asiento, idEmpresa, usuario)` — resuelve
  FacturaCompra por detalles.numDocReten normalizado + proveedor + empresa; APLP tipo 3
  (retencionV2), monto=total. IncomeException si falla → el caller anula el asiento (D1).
- `aplicarNotaCredito(NotaCreditoCompra, Asiento, ...)` tipo 2 por numDocModificado;
  `aplicarNotaDebito(...)` tipo 5 monto negativo.
- `aplicarAnticipo(idFacturaCompra, valor, fecha, idEmpresa, idUsuario, obs)` — valida
  saldo factura y PRCC.saldoInicial ≥ valor; asiento aplicación-anticipo; APLP tipo 4 FK
  null; `PRCC.saldoInicial -= valor`. Map exito/mensaje.
- `aplicarPagoTransferencia(PagoProgramado, idUsuario)` — asiento pago-transferencia +
  APLP tipo 1 (formaPago=2, referencia=referenciaBanco) + MovimientoBanco egreso
  (patrón `TransferenciaServiceImpl.tranferenciaCuentaBancaria`, impl ~136; MVCB no tiene
  campo referencia: va en descripcion, idMovimiento=id aplicación).
- `revertirAplicacion(id, motivo, idUsuario)` — estado=2 (trigger recalcula); asiento vía
  `AsientoServiceImpl.anulaAsiento` (decide anular vs reversar según período mayorizado);
  tipo 4 devuelve saldo a PRCC; tipo 1 anula MovimientoBanco.
- `revertirAplicacionesDeDocumento(tipoDoc, idDoc, motivo, idUsuario)` y
  `eliminarAplicacionesDeDocumento(tipoDoc, idDoc)` (para el borrado físico de
  revertirRegistrosBD: update estado=2 → delete → anular asiento propio).
- Consultas: `consultarPorFactura`, `saldoFactura` (vista PGS.V_SALDO_FACTURA_COMPRA),
  `estadoCuentaProveedor`.
- `resolverFacturaCompraPorNumero(numDoc, idTitular, idEmpresa)` público (lo usa la
  validación pre-SRI). Normalización: comparar sin guiones y con secuencial LPAD 9;
  >1 match ⇒ bloquear.

### PagoProgramadoService/Impl + LotePago DAO (NUEVOS)
- `registrarPago(idFacturaCompra, idCuentaOrigen, idCuentaDestinoTitular, valor, fecha, idEmpresa, idUsuario, obs, debitoAutomatico, referencia)`
  — valida saldo pendiente ≥ valor + PGTR activos de la misma factura.
  Con `debitoAutomatico=true` (2026-08-12, ver D12) el pago nace CONFIRMADO y
  llama a `aplicarPagoTransferencia` en la misma transacción; `generarLote` lo
  rechaza explícitamente y `revertirPagoConfirmado` lo deja ANULADO (no RECHAZADO).
- `listar(idEmpresa, estado, idTitular)`.
- `generarLote(idsPagos, idCuentaOrigen, idEmpresa, idUsuario)` — valida estado=1 y misma
  cuenta origen (releer estado dentro de la TX, evita doble lote); crea LTPG,
  pagos→EN_ARCHIVO; contenido vía formateador; guarda archivo (FileService) y devuelve
  para descarga.
- `procesarRespuestaBanco(idLote, respuestas[{idPago, resultado, referencia, motivo}], idUsuario)`
  — CONFIRMADO → `aplicarPagoTransferencia` + estado 3 + FK aplicación; RECHAZADO →
  estado 4 + motivo.
- `anularPago(id, motivo, idUsuario)` (estados 1/2/4→5); `revertirPagoConfirmado(id, motivo, idUsuario)`
  → revertirAplicacion + estado 4.
- `FormateadorArchivoBanco` (interfaz simple): `generarContenido(lote, pagos)`,
  `nombreArchivo(lote)`; impl provisional `FormateadorArchivoBancoPlanoImpl` marcada
  **PENDIENTE: formato oficial del banco no entregado — impl provisional NO producción**.

### AplicacionPagoCxcService/Impl + DAO (FASE CXC, espejo)
- `aplicarRetencionRecibida(RetencionCompra | RetencionCompraV2, Asiento, ...)` →
  resuelve Factura venta por numDocReten; APLC tipo 3.
- `aplicarNotaCredito(NotaCredito, ...)` usa FK dura nc.getFactura();
  `aplicarNotaDebito` tipo 5 negativo.
- `aplicarAnticipo(idFactura | idLiquidacion XOR, valor, ...)`;
  `aplicarCobroTransferencia(idFactura, valor, fecha, numTransferencia, idCuentaReceptora, ...)`
  (D7) + MovimientoBanco ingreso.
- Reversiones y consultas espejo (vista CBR.V_SALDO_FACTURA).

### REST (NUEVOS)
- `ws/rest/cxp/AplicacionPagoCxpRest` @Path("aplp"): GET /factura/{id},
  GET /estadoCuenta/{idTitular}/{idEmpresa}, POST /anticipo, POST /revertir/{id}.
- `ws/rest/cxp/PagoProgramadoRest` @Path("pgtr"): POST /, GET /listar, POST /lote,
  GET /lote/{id}/archivo, POST /lote/{id}/respuesta, POST /anular/{id},
  POST /revertirConfirmado/{id}.
- `ws/rest/cxc/AplicacionPagoCxcRest` @Path("aplc"): POST /cobroTransferencia,
  POST /anticipo, POST /revertir/{id}, GET /factura/{id}, GET /estadoCuenta.

## 5. Hooks en código existente

| Archivo | Cambio |
|---|---|
| `ejb/cxc/serviceImpl/RetencionV2ServiceImpl.java` | (a) PASO 0 (~1050): validación pre-SRI D3 — cada numDocReten debe resolver FacturaCompra, si no → return con mensaje ANTES de firmar. (b) PASO 5 (~1249) y consultarYActualizar (~1488): tras vincular asiento → `aplicarRetencionEmitida`; en el catch, si el asiento ya se creó y falló la aplicación → anular asiento (D1). (c) `anularRetencionV2` (~1316): revertir aplicaciones. |
| `ejb/cxp/serviceImpl/ProcesoCargaDocumentosServiceImpl.java` | (a) Bloqueantes D3: en registrarNotaCreditoCompra (~1178) / registrarNotaDebitoCompra (~1252) numDocModificado debe resolver FacturaCompra; en registrarRetencionCompra (~1383) / V2 (~1573) convertir la advertencia de doc sustento (~1665) en BLOQUEANTE contra Factura venta. (b) `generarAsientoCxp` (~2303, tras grabarAsientoEnDocumento ~2411): NC→aplicarNotaCredito, ND→aplicarNotaDebito (APLP); retenciones→aplicarRetencionRecibida (APLC, fase CXC). Excepciones propagan → rollback total. (c) `revertirRegistrosBD` (~1763): `eliminarAplicacionesDeDocumento` antes de los deletes; FACTURA_COMPRA con APLP activas de terceros → excepción (revertir pagos primero). |
| `ejb/cxc/serviceImpl/NotaCreditoServiceImpl.java` | Hooks en PASO 5 (~1168) y consultarYActualizar (~1865): aplicarNotaCredito (APLC); anularNotaCredito (~1213) revierte. Pre-SRI: factura FK not-null. |
| `ejb/cxc/serviceImpl/NotaDebitoServiceImpl.java` | Ídem (~1165/~1887/~1329) con tipo 5 negativo. |
| `ejb/cxc/serviceImpl/FacturaServiceImpl.java` | `anularFactura` (~2355): revertir todas las APLC activas de la factura. |

## 6. Orden de implementación (compilable en cada paso)

**FASE 0 — Base**: 1) script SQL v2 → usuario lo ejecuta; 2) constantes; 3) modelos
(MOD APLC/APLP/LQCS, NUEVOS PGTR/LTPG); 4) asientos nuevos en AsientoContableService.
**FASE 1 — CXP**: 5) DAO+Service AplicacionPagoCxp; 6) hooks RetencionV2 (pre-SRI +
aplicación + anulación); 7) hooks carga CXP (bloqueantes NC/ND + aplicaciones NC/ND +
revertir); 8) cruce anticipo proveedor + AplicacionPagoCxpRest; 9) PagoProgramado/
LotePago + formateador + aplicarPagoTransferencia + PagoProgramadoRest.
**FASE 2 — CXC**: 10) DAO+Service+REST AplicacionPagoCxc; 11) hook retenciones cargadas +
su bloqueante; 12) hooks NC/ND venta + anulaciones + anularFactura; 13) anticipo cliente
+ cobro transferencia; 14) opcional retención v1 y liquidaciones.

## 7. PENDIENTES (decisiones abiertas para el usuario)

| # | Pendiente | Impacto |
|---|---|---|
| 1 | ~~codigoAlterno de los 4 TipoAsientos nuevos~~ **RESUELTO (2026-08-07)**: proveedores (pago y cruce de anticipo) → codigoAlterno **5** (TEGRESO); clientes (cobro y cruce de anticipo) → codigoAlterno **4** (TINGRESO) | — |
| 2 | **Formato oficial del TXT del banco** y del archivo/mecanismo de **respuesta** (la interfaz FormateadorArchivoBanco los aísla; la impl provisional NO es de producción) | Pagos CXP por transferencia |
| 3 | **¿Agregar BEXTCDIF (código IFI) a TSR.BEXT?** — bloque comentado en el script; casi seguro el archivo bancario lo exige | Archivo TXT bancario |
| 4 | **Rubros de MovimientoBanco** para "pago proveedores" / "cobro clientes" (detalles de rubro a configurar en catálogo) | Clasificación de movimientos de tesorería |
| 5 | **Segundo asiento "al generar el pago"** para pagos/cobros de facturas por transferencia (D11, 2026-08-11): el usuario pidió que exista un asiento al generar el pago y otro al confirmarlo — falta definir en qué paso exacto (¿registrar el pago? ¿generar el lote/archivo?) y qué cuentas usa (¿una cuenta puente de "transferencias en tránsito"?) | `PagoProgramadoServiceImpl` (generarLote / procesarRespuestaBanco), `AsientoContableService` |

## Verificación
- Usuario ejecuta script v2 en DBeaver (con SELECTs de verificación incluidos) y
  compila/despliega desde Eclipse.
- Prueba CXP: emitir RetenciónV2 sobre factura de compra cargada → verificar APLP tipo 3
  + FCTCEPAG=2/3 + asiento; cargar NC/ND compra → aplicaciones 2/5; cruce anticipo
  (validar saldo PRCC); registrar pago → lote → TXT → respuesta simulada → aplicación
  tipo 1 + MovimientoBanco; reversiones (saldos y asientos restaurados).
- Prueba bloqueos D3: retención V2 con numDocReten inexistente no debe firmar; carga CXP
  con NC sin factura → 422.

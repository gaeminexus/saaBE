# PLAN DE TRABAJO — Cheques, Caja chica, Liquidaciones de compra, Estado de cuenta

**Fecha:** 2026-08-26 · **Rol de este documento:** análisis de impacto, decisiones de diseño, fases y avance.
**Esquema de trabajo:** orquestador (analiza, escribe DDL y prompts) → agente BACKEND (`saaBE`) → agente FRONTEND (`saaFE`) → usuario (ejecuta DDL, compila, despliega).
Los prompts están en `docs/logica-negocio/tsr/prompts/`. Los scripts en `docs/logica-negocio/tsr/sql/` y `docs/logica-negocio/cxc/sql/`.

Todo lo que sigue fue **verificado contra el código y la BD local** (`saa-oracle-23ai`) el 2026-08-26; lo que es sospecha está marcado.

---

## 0. Resumen ejecutivo

| # | Pedido | Estado real hoy | Tamaño | Fase |
|---|---|---|---|---|
| 4 | Estado de cuenta de titular no carga facturas CXC | **Defecto de frontend**: filtro `estado !== 1` descarta facturas SRI autorizadas (estado 5). Backend correcto. | XS (FE) | **D** — primero, 1 prompt |
| 1 | Pago con cheque | Modelo (`TSR.CHQR`, `TSR.DTCH`), rubros 25/26/38, servicio legado `ChequeServiceImpl` y 8 pantallas FE en maqueta **ya existen**; nada conectado al circuito moderno `PGS.PGTR`. | L | **A** — 2 prompts (BE, FE) |
| 2 | Caja chica | **No existe**. Hoy son bancos ficticios 425/427 + cuentas 428/429. Dos diseños en papel contradictorios (BE quiere cuenta bancaria; FE quiere tablas propias). | L | **B** — 2 prompts (BE, FE) |
| 3 | Liquidaciones de compra | **Recepción CXP funciona** (con clasificación contable gruesa). **Emisión CXC no es operativa**: FE llama a endpoints inexistentes, XML sin detalles, asiento es un stub que lanza excepción, sin CxP ni retención. | M | **C** — 2 prompts (BE, FE) |

Orden recomendado por agente (cada agente trabaja en serie):
- **FRONTEND:** D → A → B → C
- **BACKEND:** A → B → C (D no necesita backend)
- Dependencias: FE-A necesita BE-A desplegado; FE-B necesita BE-B; FE-C necesita BE-C. BE-A necesita DDL 01; BE-B necesita DDL 02; BE-C necesita DDL cxc.

---

## 1. FASE D — Estado de cuenta de titular (solo FRONTEND)

### Diagnóstico (verificado)
- No hay endpoint de estado de cuenta en el backend; la pantalla `saaFE/src/app/modules/tsr/forms/estado-cuenta-titular/` arma todo con N `POST {tabla}/selectByCriteria` por `titular.codigo` y pide saldos a `aplc/saldo/{id}` / `aplp/saldo/{id}`.
- `estado-cuenta-titular.component.ts:239` y `:316-318`: `esAnulado(d) = d.estado != null && Number(d.estado) !== 1`. En CXC `ESTADO` es el ciclo SRI (1 ingresada, 3 firmada, 4 enviada, **5 autorizada**, 6 devuelta; anulada = 2). En CXP todo se graba con 1. Por eso proveedor carga y cliente no.
- BD local confirma: `CBR.FCTR` estados 0/4/5 (ninguna en 1), `CBR.RTV2` 0/5, `PGS.FCTC` todo 1.
- Daño colateral del mismo filtro: NC/ND CXC autorizadas (5), retenciones emitidas RTV2 (5) en el estado de proveedor, anticipos confirmados (estado 2) en ambos.
- Falta además en el rol CLIENTE la fuente **retenciones recibidas** (`/rest/rcv2`, campo `proveedor`), que la regla de negocio exige.
- El filtro entro en saaFE commit `f019941` (2026-08-21).
- **Corregido el 2026-08-27:** anulado en CXC/CXP es `estado = 0` (`Estado.INACTIVO`), **no 2** — verificado en `FacturaServiceImpl:2640`, `NotaCreditoServiceImpl:1530`, `NotaDebitoServiceImpl:1646`, `RetencionV2ServiceImpl:1721` y contra la BD (las 11 `CBR.FCTR` en estado 0 tienen `MOTIVOANULACION` y `ESTADOEMISION=3`). En CXP ningun flujo anula documentos. `estado = 6` (no autorizada por el SRI) tampoco cuenta. Anticipos: anulado = 3. La primera version del prompt 01 llevaba `[2]`; se corrige con el prompt `01b`.

### Corrección
Prompt `01-FRONTEND-estado-cuenta-titular.md`. Sin cambios de backend (opcional: `FacturaDaoServiceImpl.obtieneCampos()` lista `"comprador"` en vez de `titular`; sin efecto en `selectByCriteria`; se corrige dentro del prompt BE-A como limpieza).

---

## 2. FASE A — Pago con cheque

### 2.1 Lo que ya existe (no reinventar)
- `TSR.CHQR` (Chequera: cuenta, comienza, finaliza, numeroCheques, fechas, rubro estado 25) y `TSR.DTCH` (Cheque: chequera, numero, valor, beneficiario, titular, asiento, fechas uso/impresión/entrega/anulación, rubro estado 26, rubro motivo anulación 38). Vacías en BD local. Secuencias `TSR.SQ_CHQRCDGO` / `TSR.SQ_DTCHCDGO`.
- `TSR.MVCB` (MovimientoBanco) ya tiene `DTCHCDGO` y `MVCBCHQN` (número de cheque). `TSR.PGSS` (Pago legado) tiene `DTCHCDGO`.
- Rubros: `EstadoCheque` (26): ACTIVO=1, ANULADO=2, GENERADO=3, IMPRESO=4, DANIADO=5, ENTREGADO=6. `EstadoChequera` (25): ACTIVA=1, INACTIVA=2, SOLICITADA=3, TERMINADA=4, PERDIDA=5, ANULADA=6. `MotivoAnulacionCheque` (38): en BD solo 1 y 2; el Java declara 3.
- `ejb/tsr/serviceImpl/ChequeServiceImpl.java` (417 líneas, legado Income): `crearChequesDeChequera`, `recuperaSiguienteCheque`, `procesoImpresionCheques`, `impresionFisicaCheque`, `actualizaChequeEntregado`, `reversar*`. **Nadie lo invoca**; `ChequeRest`/`ChequeraRest` son CRUD puro. Bugs: `obtieneCampos()` declara `"persona"` (campo real `titular`); `generaAsientoImpresion` usa `TipoAsientos.INGRESOS` para un egreso.
- `TipoMovimientoConciliacion.CHEQUES_GIRADOS_Y_NO_COBRADOS = 2` y `CHEQUE_COBRADO = 4` ya existen para conciliación.
- FE: `solicitud-chequera`, `recepcion-chequera`, `chequera` (reales, contra `/chqr` y `/dtch`); 8 pantallas de proceso de cheques en **maqueta con datos hardcodeados** (`modules/tsr/forms/pagos/cheques/**`, `pagos/procesos/**`, `pagos/consultas/**`).
- Circuito moderno de salida de dinero: **todo pasa por `PGS.PGTR` (`PagoProgramado`)** — pago de factura CXP, egreso de tesorería, anticipo a proveedor, pago de origen externo. El switch contable está en `PagoProgramadoServiceImpl` (l. 886-902 y 995-1007). **Es el único punto de extensión necesario.**
- Forma de pago: el catálogo efectivo es `1=Efectivo, 2=Transferencia, 3=Cheque, 4=Débito automático` (`PGS.APLP.APLPFPAG`, enum FE `FormaPagoAplicacion`, doc `ENDPOINTS-FRONTEND-PAGOS-COBROS.md:103`). El rubro Java `TipoFormaPago` (CHEQUE=2) **no se usa** y el rubro 101 en BD es otra cosa ("CVAL VALORIZACION ANUAL"). No usarlo.

### 2.2 Decisiones de diseño (tomadas)
1. **Cuenta bancaria**: nueva columna `CNBCCHQR` (0/1) "maneja chequera". No cambia nada más de la cuenta.
2. **Chequera**: al **registrar la recepción** de una chequera (cuenta, comienza, finaliza, fecha entrega) el backend genera los `DTCH` en estado ACTIVO, uno por número. Sugerencia de inicio = `max(finaliza)+1` de las chequeras no anuladas de esa cuenta. Se rechazan rangos solapados. Chequera pasa a TERMINADA automáticamente cuando no quedan cheques ACTIVO. Anular chequera anula sus cheques ACTIVO con motivo 3.
3. **Pago con cheque en PGTR**: nuevas columnas `PGTRFPAG` (forma de pago explícita, backfill 2/4) y `PGTRDTCH` (FK al cheque). `formaPago=3` exige `cuenta.manejaChequera=1`, `debitoAutomatico=0`, y un cheque ACTIVO disponible; no exige cuenta destino del beneficiario. El sistema asigna **el menor número ACTIVO** de la cuenta, lo pasa a GENERADO con valor, beneficiario, titular y fechaUso.
4. **Contabilización inmediata** al registrar el pago con cheque (igual que débito automático): el pago nace CONFIRMADO y pasa por el mismo switch contable existente. Razón: el cheque girado reduce el banco en libros; la conciliación ya conoce "cheques girados y no cobrados". El movimiento bancario lleva `cheque`, `numeroCheque` y tipo `CHEQUES_GIRADOS_Y_NO_COBRADOS (2)`.
5. **Glosa del asiento**: se anexa a la observación de cabecera ya existente de cada proceso ` | Cheque N° {numero} Cta {numeroCuenta}` y en la línea HABER ` | Cheque N° {numero}`. No se cambia ninguna glosa existente.
6. **Lote/archivo banco**: los pagos con cheque **no entran** en `generarLote` (misma exclusión que débito automático).
7. **Anulación**: `revertirPagoConfirmado` (ya existe) además pasa el cheque a ANULADO con motivo 4 "PAGO REVERSADO" y `fechaAnulacion`; el pago queda ANULADO. Un cheque anulado no se reutiliza. Anular un cheque suelto (ACTIVO) es un endpoint propio; un cheque GENERADO/IMPRESO/ENTREGADO solo se anula reversando su pago.
8. **Impresión/entrega**: cambios de estado GENERADO→IMPRESO→ENTREGADO por endpoints simples (con listas de ids). **La impresión física (Jasper) queda fuera de esta fase.**
9. **Circuito legado** `TSR.PGSS`/`ChequeServiceImpl.procesoImpresionCheques`: no se toca ni se reutiliza; los métodos nuevos van en `ChequeServiceImpl`/`ChequeraServiceImpl` con nombres nuevos. Se corrige el `obtieneCampos` de `ChequeDaoServiceImpl`.

### 2.3 DDL — `docs/logica-negocio/tsr/sql/01-cheques-pago-programado.sql`
`TSR.CNBC.CNBCCHQR`, `PGS.PGTR.PGTRFPAG` + `PGTRDTCH` (FK `FK_PGTR_DTCH`, índice), backfill, detalles 3 y 4 del rubro 38 (PDTR 1116/1117).

### 2.4 Prompts
`02-BACKEND-cheques.md`, `03-FRONTEND-cheques.md`.

---

## 3. FASE B — Caja chica

### 3.1 Situación
- Cero código. `docs/pendientes/PLAN_IMPLEMENTACION.md` (prioridad 6) propone caja chica = cuenta bancaria ficticia + tabla `TSR.ARCJ`; `saaFE/docs/transversal/BACKEND-REQUIREMENTS-TSR-CXP-CXC.md` §2 propone `CJCH/MVCH/RCCH/DRCH` con rubros 83/84 (**83 y 84 ya están ocupados**: `TIPO_PAGOS`, `DIAS_VENCIMIENTO_PAGO`).
- Las tablas `TSR.CJAA/CJCN/CJIN/USXC/CRCJ/DTCR` que el usuario recuerda **no son caja chica**: son el circuito de cobros en ventanilla (cajero), marcado para deprecar. No sirven de base.
- En producción hoy: bancos `TSR.BNCO` 425 "CAJA CHICA OFICINAS" y 427 "CAJA CHICA PENDIENTE LEGAL", cuentas `TSR.CNBC` 428 (plan cuenta 10029) y 429 (plan cuenta 10033, estado 3). En BD local no tienen movimientos ni pagos.
- `Egreso` (TSR.EGRS) + `PagoProgramado` ya contabilizan exactamente "DEBE cuenta del grupo del producto / HABER cuenta contable de la cuenta bancaria" (`AsientoContableServiceImpl.generarAsientoEgresoTesoreria`, l. 895). Ese asiento es el de un gasto de caja chica si la "cuenta bancaria" es la caja.

### 3.2 Decisión: modelo propio, no cuenta bancaria ficticia
Razón: el pedido exige límite del fondo, varias cajas por nombre, tope por gasto, alerta por saldo, observación obligatoria, adjunto, cierre/cuadre. Una cuenta bancaria no modela nada de eso y contamina conciliación bancaria y saldos de bancos. Se descarta también el diseño FE (RCCH/DRCH con aprobación): la reposición se modela como **un pago normal desde el banco**, que así hereda transferencia, débito automático **y cheque** sin código extra.

Modelo (DDL `02-caja-chica.sql`):
- `TSR.CJCH` CajaChica: empresa, nombre, `PLNNCDGO` cuenta contable de la caja, `CJCHMNTO` fondo (límite), `CJCHMXGS` tope por gasto, `CJCHPRAL` % de alerta (default 20), responsable, custodio, estado.
- `TSR.MVCH` MovimientoCajaChica: tipo (rubro 232: 1 Apertura, 2 Gasto, 3 Reposición, 4 Ajuste+, 5 Ajuste−), fecha, valor, concepto, observación, producto de pago (`PGS.PRDP` → grupo → cuenta de gasto), beneficiario, nº comprobante, asiento, pago programado, cierre, estado, motivo anulación.
- `TSR.CRCH` CierreCajaChica: periodo, saldo inicial, totales, saldo libros, saldo físico, diferencia, estado (rubro 233), asiento de ajuste.
- `TSR.PTCH` PathCajaChica: adjuntos del movimiento (patrón `PathNegociacion` + `FileService`).

Reglas:
- **Saldo** = Σ(apertura + reposición + ajuste+) − Σ(gasto + ajuste−) de movimientos activos. Nunca se guarda; se calcula. Alerta cuando `saldo <= fondo * pral/100`.
- **Gasto**: observación obligatoria; `valor <= saldo`; `valor <= CJCHMXGS` si está definido; producto obligatorio. Contabiliza en el acto: DEBE cuenta del grupo del producto / HABER `CJCH.PLNNCDGO`, tipo de asiento T-EGRESOS (código alterno 5, sistema 1 — el mismo que usa `EGRESO_TESORERIA`), módulo TESORERIA. Glosa: `Gasto caja chica {nombre} | {concepto} | Doc: {ndoc} | Valor: $x`. Adjunto opcional vía `PTCH`.
- **Reposición y apertura desde banco**: se registran como `PagoProgramado` de **origen externo** `OrigenPagoExterno.TSR_CAJA_CHICA` con `idOrigen = MVCHCDGO`; el asiento lo genera un método nuevo `generarAsientoReposicionCajaChica`: DEBE `CJCH.PLNNCDGO` / HABER cuenta contable del banco; el movimiento `MVCH` guarda `PGTRCDGO` y `ASNTCDGO`. Monto sugerido de reposición = fondo − saldo. Reverso del pago → anula el movimiento.
- **Apertura migrada** (para las cajas que hoy son cuentas bancarias): movimiento tipo 1 con `sinAsiento=true` — el saldo ya está en la cuenta contable 10029/10033; solo se registra el saldo inicial. Después el usuario inactiva las cuentas 428/429 (bloque 6 del DDL).
- **Ajustes** (solo desde un cierre con diferencia): DEBE/HABER entre `CJCH.PLNNCDGO` y la cuenta de "faltantes/sobrantes de caja" que el usuario elige en la pantalla del cierre.
- **Cierre**: BORRADOR calcula totales y saldo libros hasta la fecha; el usuario ingresa saldo físico; CERRADO marca los movimientos (`CRCHCDGO`) y bloquea anulaciones de esos movimientos; si hay diferencia y el usuario confirma, genera el movimiento de ajuste y su asiento. No se puede registrar un gasto con fecha ≤ último cierre CERRADO.

### 3.3 Prompts
`04-BACKEND-caja-chica.md`, `05-FRONTEND-caja-chica.md`.

---

## 4. FASE C — Liquidaciones de compra

### 4.1 Hallazgos (verificados, salvo donde se indica)
Recepción CXP (`ProcesoCargaDocumentosServiceImpl.registrarLiquidacionCompraCompra` + `AsientoContableServiceImpl.generarAsientoLiquidacionCompraCompra` l. 2497): funciona; sentido D/H correcto (DEBE gasto + IVA crédito / HABER CxP). Debilidad: el DEBE va **al primer `GrupoProductoPago` por código** (`obtenerCuentaGastoDefaultCxp`), no al producto del detalle. Nunca se ejercitó en producción con un XML real (sospecha razonable: el lote de carga tuvo cero liquidaciones).

Emisión CXC (`LiquidacionCompraServiceImpl`, `LiquidacionCompraRest`):
1. **Bloqueante** — `AsientoContableServiceImpl.generarAsientoLiquidacionCompra` (l. 1542) es un stub que lanza `UnsupportedOperationException`; toda emisión termina `COMPLETADO_CON_PENDIENTES`.
2. **Bloqueante** — XML SRI con `<detalles/>` vacío (`writeDetalles`, l. 833-840). Rechazo del SRI: sospecha, no probado.
3. **Bloqueante** — FE llama `POST /lqcs/grabarLiquidacion` y `/dtlc/*`, que **no existen**; no llama `/lqcs/procesarCompleta`. No hay `DetalleLiquidacionCompraRest` ni `PathLiquidacionCompraRest`.
4. **Bloqueante** — `LiquidacionCompraRest.procesarLiquidacionCompleta` pasa `detalles = null`.
5. **Importante** — `TipoAsientos.LIQUIDACIONES_COMPRA_EMITIDAS = 5` colisiona con `CREDITO_BANCARIO=5` y con la plantilla T-EGRESOS (código alterno 5). Las plantillas reales en `CNT.PLNT`: CREDITOS=1, CXC=2, CXP=3, T-INGRESOS=4, T-EGRESOS=5, RRHH=6.
6. **Importante** — no genera retención ni cuenta por pagar.
7. **Importante** — sin RIDE (no hay `RPRT_RIDE_LIQUIDACION` en `rep/cxc`), sin email, sin persistir formas de pago (`<pagos>` hardcodeado a 01).
8. **Importante** — sin `anular`, `reintentarAutorizacion`, `consultarYActualizarEstado`.
9. Menores — `FormaPagoLiquidacionRest` usa `@Path("/formas-pago-liquidacion")`; FE manda `tipoDoc:'04'` en vez de `'03'`; `CBR.NXPE` **no tiene fila para tipo 03** (verificado en BD local: solo 01, 04, 05, 07).

### 4.2 Decisiones de diseño (tomadas; ver §6 si el usuario quiere cambiarlas)
- **La cuenta por pagar y el asiento viven en CXP.** Al quedar AUTORIZADA, la emisión crea `PGS.LQCC` + `DLCM` + `PLCC` (con el XML autorizado y el RIDE) y llama a la contabilización existente de CXP (`generarAsientoLiquidacionCompraCompra`, tipo CXP alterno 3). `CBR.LQCS` guarda el puntero `LQCSLQCC`. Así la liquidación aparece en proposición de pago, historial y estado de cuenta de proveedor como cualquier otra, y se paga por `PGTR` (transferencia, débito o **cheque**). El stub `generarAsientoLiquidacionCompra` se elimina; la constante `LIQUIDACIONES_COMPRA_EMITIDAS` se elimina.
- **Clasificación contable por producto** en las dos vías: `DetalleLiquidacionCompra.producto` y `DetalleLiquidacionCompraCompra.producto` pasan a `@ManyToOne ProductoPago` (columna `PRODUCTO` ya es NUMBER; guarda `PGS.PRDP.ID`). `generarAsientoLiquidacionCompraCompra` agrupa el DEBE por `producto.grupo.planCuenta` como hace `generarAsientoFacturaCompra` (l. 1994-2000), con fallback al default actual solo cuando el detalle no trae producto (carga desde XML del SRI).
- **Retención**: no se genera automáticamente. Tras autorizar, el FE ofrece "Emitir retención" que abre la pantalla de Retención V2 con `codDocSustento=03` y el número de la liquidación (ya soportado por `RetencionV2ServiceImpl`).
- RIDE: se crea `RPRT_RIDE_LIQUIDACION.jrxml` **+ `.jasper` compilado con Jaspersoft Studio 7.0.3** (regla del CLAUDE.md). El agente BE lo diseña copiando el de factura; **el `.jasper` lo genera el usuario** (ver pendientes).

### 4.3 DDL — `docs/logica-negocio/cxc/sql/add-liquidacion-compra-emision.sql`
Fila `CBR.NXPE` para tipo `03` y columna `CBR.LQCS.LQCSLQCC` (FK a `PGS.LQCC`).

### 4.4 Prompts
`06-BACKEND-liquidaciones.md`, `07-FRONTEND-liquidaciones.md`.

---

## 5. Avance

| Fase | DDL | BACKEND | FRONTEND | Verificado por usuario |
|---|---|---|---|---|
| D estado de cuenta | n/a | n/a | HECHO: prompt 01 + 01b (revisados en codigo por el orquestador) | pendiente prueba en navegador |
| A cheques | 01 + 03 ejecutados en local (UQ_PGTR_DTCH OK) | 02 y 02b hechos y revisados; PENDIENTE 02c (3 importantes) | prompt 03 en curso | pendiente |
| B caja chica | 02 EJECUTADO en local 2026-08-27 (4 tablas + 12 FK + rubros 232/233) | prompt 04 listo para lanzar | prompt 05 listo | pendiente |
| C liquidaciones | ⬜ cxc entregado | ⬜ prompt 06 entregado | ⬜ prompt 07 entregado | ⬜ |

---

## 6. Pendientes del usuario

**Bloqueantes (sin esto los agentes no pueden avanzar):**
1. Ejecutar en **local** `tsr/sql/01-cheques-pago-programado.sql` antes de lanzar el prompt 02 (BE cheques).
2. Ejecutar en **local** `tsr/sql/02-caja-chica.sql` (bloques 1-5; el 6 NO) antes del prompt 04.
3. Ejecutar en **local** `cxc/sql/add-liquidacion-compra-emision.sql` antes del prompt 06. En producción, confirmar antes cuál es el `PTOEMISION` correcto para la fila `03`.

**Decidibles (hay una recomendación tomada; si no se objeta, se sigue):**
4. Caja chica contabiliza **cada gasto en el acto** (no al reponer). Es lo que ya ocurre hoy con las cuentas 428/429 y mantiene saldo contable = saldo de caja.
5. La cuenta por pagar de la liquidación emitida nace en **CXP** (no en CXC). Alternativa descartada: contabilizar y pagar desde CXC sin documento CXP.
6. Pago con cheque contabiliza **al girar** (no al entregar). Alternativa: contabilizar en la entrega — implicaría que un cheque impreso no esté en libros.
7. Cuenta contable de "faltantes/sobrantes de caja" para los ajustes de cierre: la elige el usuario en la pantalla del cierre; no se parametriza en la caja.

**Opcionales / posteriores:**
8. Generar el `.jasper` de `RPRT_RIDE_LIQUIDACION` con Jaspersoft Studio 7.0.3 cuando el agente BE entregue el `.jrxml` (sin él, la emisión queda sin RIDE pero el resto funciona).
9. Impresión física de cheques (reporte Jasper con la plantilla del banco): fase posterior.
10. Ejecutar el bloque 6 del DDL de caja chica (inactivar cuentas 428/429) solo después de migrar el saldo inicial desde la pantalla nueva.
11. Probar en el sandbox del SRI una liquidación real (fase C) antes de usarla en producción.

## 7. Orden de paso a PRODUCCION

Regla general: **el DDL va siempre ANTES del WAR**, y una fase solo pasa a produccion cuando esta probada en local (BE + FE + prueba en navegador). Los DDL de este plan son aditivos (columnas nuevas nullable con default, tablas nuevas), asi que conviven sin problema con el WAR viejo mientras tanto.

| Orden | Fase | Que ejecutar en produccion | Depende de |
|---|---|---|---|
| 1 | D estado de cuenta | **nada de DDL** (es solo frontend) | probar en navegador |
| 2 | A cheques | `tsr/sql/01-cheques-pago-programado.sql` (consolidado: ya crea el indice UNICO). **NO ejecutar el 03**, que es historico de la base local | fase A probada en local |
| 3 | B caja chica | `tsr/sql/02-caja-chica.sql` bloques 1 a 5, incluido el 2b de GRANT REFERENCES. El **bloque 6 va aparte**, solo tras migrar el saldo de las cajas | fase B probada en local |
| 4 | C liquidaciones | `cxc/sql/add-liquidacion-compra-emision.sql`, incluido el GRANT REFERENCES sobre `PGS.LQCC`. Verificar antes el `PTOEMISION` correcto para la fila tipo `03` | fase C probada en local |

Antes de cada ejecucion en produccion, correr los SELECT de control de cada bloque y comparar contra lo que devolvieron en local: las dos bases **no estan identicas** (ver el episodio del indice `SCP.IDX_PGTR_DTCH` del 2026-08-27).

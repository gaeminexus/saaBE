# Inventario: usuario obligatorio en todo proceso que graba (U-1)

**Estado: inventario, solo lectura. Sin cambios de código.** omen-saa-1-be, 2026-11-01.
Diseño: `DISENO-USUARIO-OBLIGATORIO.md`. Punto de partida: `B6-LLAMADORES-USUARIO-DEL-ASIENTO.md`.
Regla: todo proceso que graba valida el usuario **primero**; sin usuario válido no se ejecuta; ningún relleno a «SISTEMA».

**Cómo se hizo.** Cuatro lecturas en paralelo (cxc+sri · cxp+pagos+histórico · tsr+rhh · crd+cnt+asoprep), cada
una contra el REST, el servicio y el servicio Angular de saaFE. Las líneas son del código de hoy. Lo que no se
pudo verificar está en §9. **Nada de esto se probó contra base de datos.**

## 1. Hallazgos que cambian el trabajo

1. **No hay un solo timer que cree asientos.** Los únicos `@Schedule` vivos son `ProcesoMoraPrestamoTimer`
   (02:00, calcula mora, no crea asientos) y, comentado, `ProcesoDevolucionAporteTimer`. cxc, cxp, tsr, rhh y sri
   no tienen `@Schedule`, `@Timeout` ni `@Startup`. Todo lo demás tiene una persona detrás o es post-proceso
   de una persona (§6).
2. **El relleno central sigue vivo y lo alimentan los «usuario puede llegar nulo»:**
   `AsientoContableServiceImpl:719-720` (`usuario != null ? usuario : "SISTEMA"`; un `""` pasa tal cual),
   `AsientoServiceImpl:688` y `:711-712` (anulaciones). U-5 (defensa central) no puede activarse mientras
   queden llamadores que lleguen con nulo (§7).
3. **Las anulaciones de un argumento terminan en «SISTEMA»:** `anulaAsiento(idAsiento)` delega con `(null, null)`
   (`AsientoServiceImpl:614`). Hay 5 vivas en tsr/rhh y varias en cxc (`AnticipoClienteServiceImpl:607`).
4. **Un helper de cxp rellena para tsr y rhh:** `PagoProgramadoServiceImpl.usuarioNombre:3912-3917`.
   Un `idUsuario` inexistente o `0` sigue produciendo asientos «SISTEMA» aunque tsr/rhh no rellenen. Es del equipo 2.
5. **El FE tiene dos formas de leer la sesión, y una rellena:**
   - Sana: `shared/utils/usuario-sesion.util.ts` / `shared/services/usuario-sesion.ts` (`idUsuarioSesion()`,
     `usuarioSesion()`): devuelve `null` o lanza `SesionPerdidaError` y **la pantalla no envía**.
   - Mala: `appState.getIdUsuario()` (`shared/services/app-state.service.ts:313-314`) devuelve **`|| 0`**.
     Con `0`, `em.find(Usuario, 0)` da `null` y el BE graba «SISTEMA» o `usuario = null` sin protestar.
   - Otros rellenos del FE: `consulta-facturas.component.ts:52-54` y `consulta-documentos-electronicos.component.ts:102-104`
     (`'sistema'`, viaja al BE como usuario de la anulación); `reembolsos-factura.component.ts:27` (`@Input() idUsuario = 1`);
     `conciliacion-contable.component.ts:241,486,538,581,691,712` (`?.nombre || ''`); afectaciones manuales
     (`|| ''`, `afectacion-participe-dialog.component.ts:1216,1238`, `detalle-consulta-carga.component.ts:3077,3105`);
     `recibo-operacion-dialog.component.ts:176`.
6. **Ya hay modelos que copiar:** `ProcesoCargaDocumentosServiceImpl.nombreUsuarioObligatorio` (cxp, :1460),
   `ConciliacionCierreServiceImpl.usuarioNombre` (tsr, :655-666), `DocumentoSeguroCxp` (resolverUsuario) y, ya en
   este repo, `UsuarioValidacionService` (§8).
7. **B6 está viejo en un punto:** `CobroPetroContableServiceImpl` ya no rellena (B6-a/B6-b); lo que queda en
   el camino de la carga Petro es el reproceso (§5).
8. **Código muerto de tsr:** los llamadores antiguos de Cheque, CierreCaja, Cobro, DebitoCredito, Deposito,
   DetalleDeposito, Pago y Transferencia no tienen ningún llamador en el backend (los REST solo exponen CRUD
   genérico, sin asiento). Se limpian de pasada, no se migran.
9. **Un GET puede crear un asiento:** `GET /rest/lqex/getAll` y `POST /rest/lqex/selectByCriteria` (rhh) llaman a
   `sincronizarPago` → `generaAsientoYPasaAPagada`, a nombre de `liquidacion.getUsuarioRegistro()`.

## 2. cxc y sri

**sri: 0 procesos que graban.** `AtsRest` (`/rest/ats/*`), `CuadreSriRest` (`/rest/cuadresri/*`) y `ConsultaSRIRest`
(`/rest/consultasri/*`) son de solo lectura. 0 literales «SISTEMA» en `ejb/sri` y `ws/rest/sri`.

Prefijos REST: factura `fctr`, NC `ntcr`, ND `ntdb`, retención v1 `rtnc`, retención v2 `rtv2`, liquidación de
compra `lqcs`, anticipo `antc`, aplicación de pago `aplc`.

Patrón de los 6 documentos electrónicos: el usuario **no viaja como parámetro**; sale de `documento.usuario`
(`@ManyToOne Usuario`, en el body de `procesarCompleta`). El servicio relee el documento y usa
`getUsuario().getNombre()` con relleno «SISTEMA» si es nulo. **Ningún `procesar*Completa` valida que exista.**
`contabilizar`, `reintentarAutorizacion` y `consultarYActualizarEstado` no reciben usuario ni el FE lo manda: reusan
`documento.getUsuario()`.

| Proceso | REST | Usuario hoy | ¿FE lo manda? | Relleno a quitar |
|---|---|---|---|---|
| Emitir factura `FacturaServiceImpl.procesarFacturaCompleta` | POST `/rest/fctr/procesarCompleta` | `factura.usuario` (cliente, sin validar) | Sí (`facturas-ingreso.component.ts:625,702`) | `FacturaServiceImpl:896-897` |
| Contabilizar factura | POST `/rest/fctr/contabilizar/{id}` | `factura.getUsuario()` | No | `:897` |
| Reintentar autorización | POST `/rest/fctr/reintentarAutorizacion` | **literal fijo**, ignora `factura.usuario` | No | **`FacturaServiceImpl:2482`** |
| Consultar y actualizar estado | POST `/rest/fctr/consultarYActualizarEstado` | `factura.getUsuario()` | No (`{idFactura}`) | `:897` |
| Anular factura `anularFactura` | POST `/rest/fctr/anular` | `usuario` (String libre, no se contrasta) + `idUsuario` (opcional) | Sí; el String cae a `'sistema'` en el FE | `FacturaServiceImpl:2705`; cascada `AplicacionPagoCxcServiceImpl:1279/1282` |
| Emitir NC | POST `/rest/ntcr/procesarCompleta` | `notaCredito.usuario` | Sí (`notas-credito.component.ts:326,350`) | `NotaCreditoServiceImpl:1345-1346`, cruce `:1469-1470` |
| Contabilizar NC / consultar estado | POST `/rest/ntcr/contabilizar/{id}`, `/consultarYActualizarEstado` | `getUsuario()` | No | los mismos |
| Anular NC | POST `/rest/ntcr/anular` | `usuario` obligatorio + `idUsuario` | Sí | `NotaCreditoServiceImpl:1612` |
| Emitir ND | POST `/rest/ntdb/procesarCompleta` | `notaDebito.usuario` | Sí (`notas-debito.component.ts:269,286`) | `NotaDebitoServiceImpl:1336-1337`, cruce `:1460-1461` |
| Contabilizar ND / consultar estado | POST `/rest/ntdb/contabilizar/{id}`, `/consultarYActualizarEstado` | `getUsuario()` | No | los mismos |
| Anular ND | POST `/rest/ntdb/anular` | `usuario` + `idUsuario` | Sí | `NotaDebitoServiceImpl:1722` |
| Emitir retención v1 | POST `/rest/rtnc/procesarCompleta` | `retencion.usuario` | Sí (`retenciones.component.ts:190,206`) | `RetencionServiceImpl:1204-1205` |
| Anular retención v1 | POST `/rest/rtnc/anular` | `usuario` obligatorio; **no existe `idUsuario`** | sin llamador hallado (la pantalla usa v2) | `RetencionServiceImpl:1345` |
| Emitir retención v2 (+ cruce CXP) | POST `/rest/rtv2/procesarCompleta` | `retencion.usuario` | Sí (`retencionesv2.component.ts:470,484`) | `RetencionV2ServiceImpl:762-763`, cruce `:843-844` |
| Contabilizar / reenviarSRI / consultar estado v2 | POST `/rest/rtv2/contabilizar/{id}`, `/reenviarSRI/{id}`, `/consultarYActualizarEstado` | `getUsuario()` | No | los mismos |
| Anular retención v2 | POST `/rest/rtv2/anular` | `usuario` + `idUsuario`; **el REST no exige `usuario`** (:375 lo deja nulo) | Sí | `RetencionV2ServiceImpl:2179` |
| Emitir liquidación de compra | POST `/rest/lqcs/procesarCompleta` | `liquidacion.usuario` | Sí (`liquidaciones.component.ts:625,682`) | `LiquidacionCompraServiceImpl:915-916` |
| Crear doc. CXP / contabilizar / reintentar / consultar | POST `/rest/lqcs/crearDocumentoCxp/{id}`, `/contabilizar/{id}`, `/reintentarAutorizacion/{id}`; GET `/consultarYActualizarEstado/{id}` | `getUsuario()` | No | `:916` |
| Anular liquidación | POST `/rest/lqcs/anular` | `usuario` + `idUsuario` | Sí | `LiquidacionCompraServiceImpl:2203` |
| Registrar anticipo | POST `/rest/antc/procesar` | `idUsuario` **obligatorio y contrastado** (`:354`, `:373`) | Sí (`idUsuarioSesion()`) | `AnticipoClienteServiceImpl:436` (cae al **código** del usuario, no a «SISTEMA»; revisar) |
| **Confirmar anticipo** (crea asiento) | POST `/rest/antc/confirmar` | `usuario` String opcional, sin contrastar | **No** (`anticipo.component.ts:203` manda solo `{idAnticipo}`) | **`AnticipoClienteServiceImpl:287`**: el que más se ejecuta en un flujo real de cxc |
| Anular anticipo | POST `/rest/antc/anular/{id}` | `idUsuario` (opcional); el asiento se anula con `anulaAsiento(idAsiento)` **sin usuario** (`:607`) | Sí | indirecto (cnt) |
| Solicitar devolución de anticipo | POST `/rest/antc/solicitarDevolucion` | `idUsuario` opcional | Sí (`seguimiento-anticipos.component.ts:267-277`) | — |
| Sincronizar devoluciones | POST `/rest/antc/sincronizar/{id}`, `/sincronizarDevoluciones` | sin usuario (reconciliación, sin persona) | no | — |
| Cobro por transferencia `aplicarCobroTransferencia` | POST `/rest/aplc/cobroTransferencia` | `idUsuario` opcional | Sí (`registrar-cobro.component.ts:181-196`) | `AplicacionPagoCxcServiceImpl:1279,1282` (`usuarioNombre`) |
| Cruce de anticipo(s) | POST `/rest/aplc/anticipo`, `/anticipos` | `idUsuario` opcional | Sí | los mismos |
| Revertir aplicación | POST `/rest/aplc/revertir/{id}` | `idUsuario` opcional | Sí; `consulta-cobros.component.ts:251` no valida null | los mismos |
| Aplicaciones automáticas NC/ND/retención | sin REST | `String usuario` que pasa el emisor | — | vienen de los emisores |

## 3. cxp, pagos e histórico

**Ya correctos (la plantilla a copiar):** `ProcesoCargaDocumentosServiceImpl` valida con
`nombreUsuarioObligatorio` (:1460) en `cargarXmlYRegistrar` (:591), `registrarDocumentoBD` (:873),
`recontabilizarDocumento` (:1216) y `contabilizarReembolso` (:2290); `crearProductosYRegistrar` delega en
`registrarDocumentoBD`; `DocumentoSeguroCxp` (`registrarDesdeXml`, `enlazar`, `liberar`, `desenlazar`) valida; `AnticipoProveedorServiceImpl.procesarAnticipo` (:228, :253) y
`FacturaCompraServiceImpl.registrarNotaVentaManual` (:278, :317) exigen el id.

| Proceso | REST | Usuario hoy | ¿FE? | Pendiente / relleno |
|---|---|---|---|---|
| Anular factura de compra | POST `/rest/fctc/anular/{id}` | `usuario` String libre + `idUsuario` | Sí (`consulta-documentos.component.ts:604-618`; `idUsuario` por `getIdUsuario()`, cae a 0) | `FacturaCompraServiceImpl:166` |
| Anular NC de compra | POST `/rest/ntcc/anular/{id}` | igual | igual | `NotaCreditoCompraServiceImpl:147` |
| Anular ND de compra | POST `/rest/ntdc/anular/{id}` | igual | igual | `NotaDebitoCompraServiceImpl:109` |
| Anular liquidación de compra | POST `/rest/lqcc/anular/{id}` | el REST lee `usuario` e `idUsuario`; **el FE solo manda `usuario`** | parcial | `LiquidacionCompraCompraServiceImpl:121` |
| Nota de venta manual | POST `/rest/fctc/manual` | `idUsuario` **validado** | Sí (`getIdUsuario()`, 0 si no hay sesión; el BE lo rechaza) | `FacturaCompraServiceImpl:592` (nombre nulo de un usuario existente) |
| Carga: cargarTxt, cargarXml, resolverNovedad, revertir, anularContabilidad, anularDocumento, marcarReembolso | `carga-documentos/…` (`ProcesoCargaDocumentosRest`) | `idUsuario` del cliente; `em.find` **sin validar** (o ni se usa) | Sí (`idUsuarioSesion()`) | **validar al inicio**; no hay literales |
| Lote: descargarXmlLote / registrarLote | POST `carga-documentos/descargarXmlLote/{id}`, `/registrarLote/{id}` (`@Asynchronous`) | `idUsuario` del request | Sí | validar **antes de lanzar el hilo** (hoy el fallo se pierde en el hilo) |
| Reembolso: contabilizar | POST `carga-documentos/contabilizarReembolso/{id}` | validado | `reembolsos-factura.component.ts:27` default `1` | quitar el default del FE |
| Registrar anticipo a proveedor | POST `/rest/antp/procesar` | `idUsuario` validado | Sí | — |
| Contabilizar anticipo al confirmar pago | sin REST (`PagoProgramadoServiceImpl:3848`) | `idUsuario` del pago, puede ser nulo | — | `AnticipoProveedorServiceImpl:389-390` |
| Anular anticipo | POST `/rest/antp/anular/{id}` | `idUsuario`, sin validar | Sí | — |
| Devolución de anticipo: registrar / anular | POST `/rest/dvpr/registrar`, `/anular/{id}` | `idUsuario` sin validar; el anular ni lo usa | Sí | `DevolucionAnticipoProveedorServiceImpl:359-361` |
| Cruces: anticipo(s), NC | POST `/rest/aplp/anticipo`, `/anticipos`, `/cruzarNotaCredito` | `idUsuario`, `em.find` sin validar | Sí | `AplicacionPagoCxpServiceImpl:446, 2082, 2085` (`usuarioNombre`, usada en 10 puntos) |
| Revertir aplicación | POST `/rest/aplp/revertir/{id}` | `idUsuario` ni se usa | Sí | indirecto |
| Pago programado: registrar | POST `/rest/pgtr` | `idUsuario`, `pago.setUsuario(em.find)` sin validar | Sí (`idUsuarioSesion()`) | `PagoProgramadoServiceImpl:3914,3917` |
| Pago: aprobar / lote / respuesta banco / confirmar manual / anular / cuenta destino / revertir | POST `/rest/pgtr/aprobar`, `/lote`, `/lote/{id}/respuesta`, `/confirmarManual`, `/anular/{id}`, `/cuentaDestino/{id}`, `/revertirConfirmado/{id}` | `idUsuario` del cliente | Sí; **`aprobacion-pagos` y `anular` mandan `getIdUsuario()` (0)** | el mismo helper; usado en :3016, :3283, :3368, :3452, :3505 |
| Orden de pago de origen externo | sin REST (`registrarPagoDeOrigenExterno`: crd `DevolucionAporteServiceImpl`, `PagoPensionComplementariaServiceImpl`, `PrestamoServiceImpl`; cxc `AnticipoClienteServiceImpl:1067`; rhh, tsr) | `Long idUsuario` del llamador, sin validar | — | validar dentro del método |
| Carga histórica CxC/CxP | POST `/rest/historico/cargarHistoricoCxcCxp`, `/validarHistoricoCxcCxp`, `/reasignarSaldoInicial`, `/cargarHistoricoNomina`, `/validarHistoricoNomina` | `idUsuario` del lote; **si falta, se inyecta 38** | sin FE (script externo) | **no se toca** (§6) |

## 4. tsr y rhh

**tsr.** Ningún BE valida que el usuario exista, salvo `ConciliacionCierreServiceImpl.usuarioNombre:655-666`.

| Proceso | REST | Usuario hoy | ¿FE? | Relleno |
|---|---|---|---|---|
| Registrar ingreso | POST `/rest/ingr/procesar` | `idUsuario`; `em.find` | Sí, con guarda (`idUsuarioSesion()`) | `IngresoServiceImpl:280,283` |
| Anular ingreso | POST `/rest/ingr/anular/{id}` | `idUsuario` recibido pero **no usado**; `anulaAsiento(idAsiento)` (:240) | Sí | indirecto (:240) |
| Registrar egreso | POST `/rest/egrs/procesar` | `idUsuario`; con anticipo arma `nombreUsuario` | Sí, con guarda | `EgresoServiceImpl:307` |
| Anular egreso | POST `/rest/egrs/anular/{id}` | `idUsuario`; con anticipo no se usa, `anulaAsiento(idAsiento)` (:469) | Sí | indirecto (:469) |
| Gasto de caja chica | POST `/rest/mvch/gasto` | `idUsuario` (Long, sin validar) | Sí, **sin guarda** (`getIdUsuario()`) | `MovimientoCajaChicaServiceImpl:731,734` |
| Reposición / apertura | POST `/rest/mvch/reposicion`, `/apertura` | `idUsuario`; asiento por cxp | Sí, sin guarda | los mismos + cxp |
| Anular movimiento | POST `/rest/mvch/anular/{id}` | `idUsuario`; `anulaAsiento(idAsiento)` (:411) | Sí, sin guarda | indirecto (:411) |
| Cierre de caja chica: preparar / confirmar / anular | POST `/rest/crch/preparar`, `/confirmar/{id}`, `/anular/{id}` | `idUsuario`; el anular no lo usa (:305, error tragado) | Sí, sin guarda | `CierreCajaChicaServiceImpl:410,413`; indirecto :305 |
| Caja chica: registrar / baja / activar | POST `/rest/cjch/registrar`, `/anular/{id}`, `/activar/{id}` | `idUsuario` sin validar | Sí, sin guarda | — |
| Cheques / chequera | POST `/rest/dtch/anular/{id}`, `/imprimir`, `/entregar`; `/rest/chqr/registrarRecepcion`, `/anular/{id}` | `idUsuario` **ignorado** | Sí, sin guarda | — (no graban el usuario: decidir si deben) |
| Conciliación contable | POST `/rest/cnct/conciliar`, `/deshacer/{id}`, `/verificar/{id}`, `/cerrarMes/{e}/{p}` | `usuario` String, nadie lo valida | `?.nombre \|\| ''` | cadena vacía en el FE |
| Tránsito: cerrar / anular | POST `/rest/cnct/transito/cerrar`, `/anular/{id}` | `idUsuario` **validado** | Sí | — (modelo) |
| Llamadores antiguos (Cheque, CierreCaja, Cobro, DebitoCredito, Deposito, DetalleDeposito, Pago, Transferencia) | sin REST | parámetro / `getNombreUsuario()` | — | **muertos**: `ChequeServiceImpl:269,378`, `CierreCajaServiceImpl:155,263`, `CobroServiceImpl:358,372,620`, `DebitoCreditoServiceImpl:219,253`, `DepositoServiceImpl:283,391,396`, `DetalleDepositoServiceImpl:187`, `PagoServiceImpl:224,287`, `TransferenciaServiceImpl:109,225` |

**rhh.** Casi todo usa `String usuarioRegistro` (nombre de login), por query o body, sin contrastar con la tabla. El
`Long idUsuario` es para tesorería. En el FE de rhh `usuarioSesion()` lanza si no hay sesión.

| Proceso | REST | Usuario hoy | ¿FE? | Relleno |
|---|---|---|---|---|
| Asiento del rol / provisiones | POST `/rest/prdn/contabilizar/{id}`, `/contabilizarProvisiones/{id}` | query `usuarioRegistro`, puede llegar nulo (cnt rellena) | Sí | rechazar nulo/vacío |
| Descontabilizar período | POST `/rest/prdn/descontabilizar/{id}` | body `usuarioRegistro`; nulo → cnt «SISTEMA» (`AsientoServiceImpl:711-712`) | Sí | indirecto |
| Pago de nómina | POST `/rest/rdpg/generar`, `/confirmar/{id}`, `/sincronizarPagos/{id}` | `usuarioRegistro` + `idUsuario` (`exigeIdUsuario` solo comprueba no-nulo) | Sí, con guarda de 0 | `sincronizarPagos` usa `orden.getUsuarioRegistro()` |
| Liquidación de haberes | POST `/rest/lqdc/contabilizar/{id}`, `/calcular`, `/aprobar/{id}`, `/ejecutarSalida/{id}` | query `usuarioRegistro` | Sí | — |
| Orden de beneficio social | POST `/rest/odbs/generar`, `/confirmarPago/{id}`, `/enviarATesoreria/{id}`, `/anular/{id}`, `/revertirPago/{id}` | `usuario` String / `idUsuario`; `revertirPago` usa `anulaAsiento(idAsiento)` (:599) | Sí (revertirPago no verificado) | **`OrdenBeneficioSocialServiceImpl:882,885`**; indirecto :599 |
| Liquidación externa | POST `/rest/lqex/registrar`, `/enviarATesoreria/{id}`, `/sincronizarPago/{id}`, `/anular/{id}`; **GET `/getAll`, POST `/selectByCriteria`** | entidad `usuarioRegistro`; `idUsuario` | Sí | **`LiquidacionExternaServiceImpl:647-648`** |
| Anticipo a empleado | POST `/rest/ante/solicitar`, `/aprobar/{id}`, `/anular/{id}` | `idUsuario` sin validar | Sí, **sin guarda** | **`AnticipoEmpleadoServiceImpl:342,345`** |
| Planilla IESS | POST `/rest/plis/registrar`, `/conciliar/{id}`, `/anular/{id}`, `/pagar/{id}`, `/reversarPago/{id}` | `idUsuario` sin chequeo de nulo | Sí, sin guarda | indirecto (cxp) |
| Nómina: calcular / aprobar / reabrir / cerrar / excluir / recalcular | POST `/rest/prdn/…` | `usuarioRegistro` String, sin validar | Sí | — |
| Retención de renta en línea | indirecto de `prdn/calcular` | **no se pasa el usuario** | n/a | **`RetencionRentaServiceImpl:155`** `"SISTEMA"`; propagar el de `calcularPeriodo` |

## 5. crd, cnt y asoprep

**crd: lo que ya exige y valida el usuario en servidor** (no tocar): pago de cuota, pago múltiple, pago con aportes,
abono a capital (con y sin aportes), precancelación, `anularOperacion`, `anular` préstamo, aportes
(`registrarAporte`, `procesarJubilacion`), devolución de aportes (registrar, para beneficiarios, anular, reemitir),
pensión complementaria (4 endpoints), plazo vencido (declarar, liquidar, revertir), acuerdo de condonación,
recepción de valor de seguro (4), CBCR aprobar/rechazar/reenviar/anular/reversar/procesar, reproceso
(`ReprocesoCargaPetroServiceImpl:142`), aplicar pagos de la carga (B6-b), cerrar/reabrir período contable, cargador de asientos manuales.

**crd: lo que NO valida y puede llegar nulo**

| Proceso | REST | Usuario hoy | ¿FE? | Pendiente / relleno |
|---|---|---|---|---|
| Aprobar y desembolsar préstamo | POST `/rest/prst/aprobar/{id}` | `solicitud.usuario` e `idUsuario`; solo valida `idEmpresa` e `idUsuario` | Sí (`usuarioSesion()`) | validar `usuario` |
| Rechazar préstamo | POST `/rest/prst/rechazar/{id}` | `solicitud.usuario`, sin validar | Sí | validar |
| Contabilizar desembolso(s) | POST `/rest/prst/contabilizarDesembolso`, `/contabilizarDesembolsos` | `item.usuario` con respaldo `lote.usuario`, sin validar | sin llamador hallado | validar; **`PrestamoServiceImpl:739`** `usuario != null ? usuario : "CUADRE"` |
| Cargar tabla de amortización de Excel | POST `/rest/prst/cargarTablaExcel/{id}` | no hay usuario | — | **`PrestamoServiceImpl:1658`** `setUsuarioRegistro("SISTEMA")` |
| Registrar cobro CBCR | POST `/rest/cbcr/registrar` | `solicitud.usuario` no validado en servicio (:247) → asiento transitorio | Sí (`usuarioSesion()`) | validar |
| Cierre de cartera | POST `/rest/cierrecartera/ejecutar` | `SolicitudCierreCartera.usuario`, no validado | **puede mandar nulo** (`cierre-cartera.component:197`, `?? sessionStorage ?? null`) | validar; **`CierreCarteraServiceImpl:262`** |
| Reversar / reabrir cierre | POST `/rest/cierrecartera/reversar/{id}`, `/reabrir` | `@QueryParam usuario`, no validado | **condicional** (`cierre-cartera.service.ts:57,76`, solo si existe) | validar; el nulo llega a cnt |
| Provisión de intereses | sin REST | hereda | — | **`ContabilizacionIndividualCreditoServiceImpl:838`** `"SISTEMA"` |
| Reverso contable de operación | dentro de `anularOperacion` | `eventoAnulado.getUsuarioAnulacion()` | — | **`ContabilidadPrestamoServiceImpl:1080`** (inalcanzable con nulo, pero el relleno sigue) |
| Rechazo automático de cobros | dentro de `procesar` | sin persona | — | **`CobroCreditoServiceImpl:1136,1264,1350`** `setUsuarioRechazo("SISTEMA")` |
| Mora manual | POST `/rest/prst/calcularMora`, `/calcularMora/{id}` | `@QueryParam usuario` opcional → `"SAA_MORA"` | Sí | **`PrestamoRest:1755-1756,1816-1817`** |
| Recalcular distribución de banda | POST `/rest/dsbn/recalcularDistribucion` | `@QueryParam usuario` opcional | condicional | **`DistribucionBandaRest:152`** `"sistema"` (el servicio ya exige en :392) |
| Generación de archivo Petro | POST `/rest/gnap/crearCabecera` | `@QueryParam usuario`, no validado | Sí | validar |
| Afectaciones manuales | POST `/rest/avpc/batch` | no revisé el servicio | puede mandar `''` | revisar |
| Transferencias / confirmar / reversar recepción Petro | POST `/rest/asgn/transferencias`, DELETE `/transferencias/{id}`, POST `/confirmarRecepcion/{id}`, `/reversarRecepcion/{id}` | `solicitud.usuario`, **no validado** (`CobroPetroContableServiceImpl:267,361,469,481,528`); nulo → cnt «SISTEMA» | Sí (`usuarioSesion()`) | validar (el valor lo pone la cobranza de la carga) |

**cnt**

| Proceso | REST | Usuario hoy | ¿FE? | Relleno |
|---|---|---|---|---|
| Asiento manual (crear/editar) | PUT/POST `/rest/asnt` | `Asiento.nombreUsuario` del cliente, sin validar | Sí (`usuarioSesion()`) | si llega nulo queda nulo |
| Anular asiento | POST `/rest/asnt/anular/{id}` | `@QueryParam usuario`; el REST exige `motivo`, **no `usuario`** | Sí | **`AsientoServiceImpl:712`** y javadoc `AsientoService:277` |
| Anular asiento de documento | interno | `asiento.getUsuarioAnulacion()` | — | **`AsientoServiceImpl:688`** |
| Crear asiento por plantilla `generarAsiento` | interno (único punto de creación) | el que reciba | — | **`AsientoContableServiceImpl:719-720`** (central) |
| Reversión / copia de asiento | GET `/rest/asnt/generaReversion/{id}`, `/generaCopia/{id}` | **no reciben usuario**; copian el del original | no | atribuir a quien lo pide |
| Cerrar / reabrir período | POST `/rest/prdo/cerrar`, `/reabrir` | validado | Sí | **`CierrePeriodoContableServiceImpl:212`** (alcanzable por `CierreCarteraServiceImpl:298,411`) |
| Mayorización / desmayorización | GET `/rest/myrz/mayorizacion/…`, `/desmayorizacion/…` | no hay usuario en el modelo | no | decidir si debe haberlo (no crea asientos) |

**asoprep**

| Proceso | REST | Usuario hoy | Relleno |
|---|---|---|---|
| Subir / validar archivo Petro/ARCH | POST `/rest/asgn/procesarArchivoPetro`, `/validarArchivoPetro` | `usuarioCarga` dentro del JSON de la carga | sin validar |
| Fase 2: procesar carga (legacy `ProcesoCargaPetroServiceImpl`) | POST `/rest/asgn/procesarCargaPetro/{id}` | **no hay usuario** | **`:184`** `"SISTEMA"` y **`:523`** `"SISTEMA_PETRO"` |
| Revalidar carga | POST `/rest/asgn/revalidarCarga/{id}` | no hay usuario | — |
| Fase 3: aplicar pagos | POST `/rest/asgn/aplicarPagosArchivoPetro/{id}?usuario=` | **validado primero (B6-b)** | — |
| Aportes y pagos de aporte creados por la carga | interno | — | **`CargaArchivoPetroServiceImpl:7969`** `"SAA_AH"` y **`:8022`** `"SISTEMA"` |

## 6. Procesos sin persona

**(a) El reproceso (decisión del usuario: no se toca; los cargadores históricos se eliminan al cierre del proyecto).**

| Qué | Dónde | Con qué usuario graba hoy |
|---|---|---|
| Asientos de reparto y aplicación en modo reproceso | `CargaArchivoPetroServiceImpl:2380` | `modoReproceso ? "SISTEMA" : usuarioDeLaCarga(...)` |
| Usuario de la corrida | `:2426` y `:7761` | `solicitud.usuario` o `"SAA_REPROCESO"` |
| Carga histórica CxC/CxP y nómina | `CargaHistoricaCxcCxpServiceImpl:30,45,126`, `CargaHistoricaNominaServiceImpl:30,45`, `CargaHistoricaDocumentoServiceImpl:155`, `CargaHistoricaNominaPasoServiceImpl:132` | usuario 38 si el lote no trae `idUsuario`; `"SISTEMA"` si el nombre es nulo |
| Cadena que llaman | `contabilizarRol/Provisiones/Pago` con `ctx.usuario` (`CargaHistoricaNominaPasoServiceImpl:414,433,502,597`); `aplicarNotaCredito/Debito/RetencionEmitida` de cxp (:626, :1114) | el de arriba |

Los cargadores históricos quedan **«proceso sin persona, del reproceso»**: no se proponen cambios. Lo que **sí** hay que
resolver antes de activar U-5 es el camino del reproceso de la carga Petro (`:2380`, `:2426`, `:7761`) y lo que llama
(cnt `generarAsiento`, que recibe `"SISTEMA"` explícito): la defensa central rechazaría esos asientos. Ver §7.

**(b) Otros sin persona**
- `ProcesoMoraPrestamoTimer.ejecutarCalculoDiario` (02:00): `"SAA_MORA"` (`ProcesoMoraPrestamoService:65`). No crea asientos.
- `ProcesoDevolucionAporteTimer`: `@Schedule` comentado; `DevolucionAporteService.USUARIO_PROCESO = "SAA_DEVOLUCION"`.
- Rechazo automático de cobros CBCR: «SISTEMA» (§5).
- Aportes y pagos de aporte de la carga: `"SAA_AH"` / `"SISTEMA"`; fase 2 legacy: `"SISTEMA"` / `"SISTEMA_PETRO"`.
- **Post-proceso del SRI, sin timer:** el SRI autoriza de forma asíncrona. `reintentarAutorizacion`, `consultarYActualizarEstado` y
  `contabilizar` crean el asiento o el cruce **después** de la emisión, en otra llamada de una persona, y usan
  `documento.getUsuario()` o, el peor caso, el literal de `FacturaServiceImpl:2482`. No hay recepción automática
  del SRI que grabe sin que alguien pulse (`FacturaServiceImpl:2479-2482` es `reintentarAutorizacion`).
- `GET /rest/lqex/getAll` y `selectByCriteria` (rhh), `sincronizarPagos` (rhh): efecto lateral a nombre de `usuarioRegistro` de la entidad.
- Mayorización: no tiene usuario en el modelo.
- Lotes de cxp (`@Asynchronous`): los lanza una persona; el usuario viaja en el request.

## 7. Lo que hay que resolver antes de activar U-5 (defensa central)

U-5 hace que `generarAsiento` y `anulaAsiento` rechacen un usuario nulo/vacío/inexistente. Hoy lo violan:
1. **El reproceso**: `"SISTEMA"` explícito y `"SAA_REPROCESO"` (§6a). Decisión pendiente del arbitro: ¿el reproceso recibe el usuario de quien lo ejecuta (`solicitud.usuario`, que el servicio ya valida)? Sin eso, U-5 no puede ir.
2. Los **«usuario puede llegar nulo»** de §5: CBCR registrar, cierre de cartera (ejecutar/reversar/reabrir), confirmar/reversar recepción Petro, `anulaAsiento(idAsiento)` de un argumento (cxc, tsr, rhh, cxp).
3. Los **rellenos explícitos** de los otros módulos (cxc 19 líneas, cxp 14, tsr 7, rhh 7, crd/cnt/asoprep 28 expresiones).
4. Las **anulaciones sin usuario** (reversión/copia, las de un argumento): hay que atribuirlas a alguien.
5. Los **timers**: «SAA_MORA» y «SAA_DEVOLUCION» no crean asientos; si U-5 se limita a asientos, no los toca.

## 8. El helper (ya construido en B6-b)

`com.saa.basico.ejb.UsuarioValidacionService.validar(String nombre)` (`@Stateless`, `basico/ejbImpl/UsuarioValidacionServiceImpl`):
- busca por `Usuario.nombre` (SCP.PJRQ), igual que el login; nulo o vacío → «falta el usuario»; ausente → «el usuario 'x' no existe»;
- lanza `UsuarioInvalidoException` (extiende `IncomeException`, `@ApplicationException(rollback)`); el REST responde **400**;
- devuelve el nombre ya normalizado para usarlo en el asiento.
- **Limitación:** «activo» se reduce a «existe»: la entidad `Usuario` (SCP.PJRQ) no mapea ninguna columna de estado.

**Dónde vive el estado del usuario (pista, no verificada contra la base).** `UsuarioDaoServiceImpl.selectUsuarios` (`:75-83`) lista los usuarios
activos con `PJRQ.PGSPCDGO = 9 AND (SELECT PDCNVLRR FROM SCP.PDCN WHERE PCXNCDGO = 19 AND PJRQCDGO = d.PJRQCDGO) = 1`:
el «activo» sería **`SCP.PDCN` con `PCXNCDGO = 19` y valor 1**, no una columna de `PJRQ`. El login
(`validaUsuario`, `UsuarioDaoServiceImpl:86-97`) delega en el procedimiento `scp.pc_prmt_gnrl.pr_vlda_usro_clve`,
cuyo cuerpo no está en el repo; si ese procedimiento mira otra cosa, el estado vive allí. Para exigir «activo» en el helper
alcanza con repetir esa consulta; **queda por confirmar con el usuario o con el DBA** (no corrí SQL).

## 9. No se pudo verificar

- Todo contra base de datos (usuarios inexistentes, `PDCN` 19, `pr_vlda_usro_clve`).
- Endpoints `/autorizar` y `/reenviarEmail` de cxc (¿generan asiento?); `EmailFacturaServiceImpl`.
- FE de `plis/conciliar`, `odbs/revertirPago`, `cxp-seguros`, `DetalleOrdenPagoNominaRest.reenviar`, `ProyeccionImpuestoRentaRest`.
- Callers de FE para `prst/contabilizarDesembolso(s)`, `asnt/cargarManuales*` y `crar/reprocesar`: no los hallé.
- `avpc/batch`, `pgpc/reprocesarPeriodo` y `pgpc/sincronizarPagos`: no leí si llevan usuario.
- REST CRUD genérico de cada entidad (`PUT/POST /rest/<tabla>`): quién pone el usuario en la entidad.
- Cuerpos completos de `revertirDocumento`, `anularContabilidadDocumento`, `marcarDocumentoAnulado` (cxp).
- Qué hace `getIdUsuario()` exactamente fuera de lo leído: se vio `|| 0`.
- No se compiló nada (solo lectura).

## 10. Resumen en cifras
- Procesos que graban, contados por fila de las tablas: cxc 33, cxp+pagos+histórico 34, tsr ≈21 (+9 muertos), rhh ≈32, crd 33, cnt 8, asoprep 5. ≈166 filas (≈145 con REST vivo). sri: 0.
- Validan el usuario en servidor: una minoría clara (§3 y §5 lo detallan). El resto acepta nulo o un id inexistente.
- Rellenos a quitar: cxc 19 + cxp 14 + tsr 7 + rhh 7 + crd/cnt/asoprep 28 (+ 11 anulaciones de un argumento, 5 vivas) ≈ 75 expresiones; usuario fijo 38 (histórico, no se toca); FE: `getIdUsuario()` a 0, `'sistema'`, `|| ''`, `idUsuario = 1`.
- El FE ya manda usuario en casi todo; los puntos donde **no** (o donde manda `0`/`''`) están marcados en las tablas.

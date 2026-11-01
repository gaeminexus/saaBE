# B6 paso 1: quién crea asientos y con qué usuario (rastreo de solo lectura, 2026-11-01)

Rastreo hecho por omen-saa-1-be. Se registra aquí porque es lo que necesita el equipo 2 al retomar.

**Hallazgos clave:**
1. **«SISTEMA» se escribe en cnt en estos puntos:**
   - `AsientoContableServiceImpl.grabarAsiento:719`, solo si el usuario es null; un `""` pasa tal cual.
     Es el único punto de creación.
   - `AsientoServiceImpl:688/712` (anulación).
   - `CierrePeriodoContableServiceImpl:212`.
2. **crd, del equipo 1: la mayor fuente de asientos «SISTEMA».** `CobroPetroContableServiceImpl:678`
   (reparto), `:739` (complemento D-024) y `:1262` (aplicación) pasan null, porque
   `contabilizarReparto`/`contabilizarAplicacion` no reciben usuario. Cada carga genera 2 o 3.
   Además, `crearContextoPagoCarga` no setea `ContextoPago.usuario`, así que los PGPR de la carga
   quedan sin usuario.
3. **cxc, cxp, tsr y rhh ya rellenan «SISTEMA» por su cuenta antes de llamar a cnt.** Un rechazo
   central en cnt no los alcanza: hay que corregirlos en cada módulo. **Esto es del equipo 2**
   (cxc/cxp/tsr) **y del equipo 3** (rhh).
4. **Los timers de crd/cnt no crean asientos.**

**Decisión pendiente del usuario:** el rechazo central de cnt sin usuario. Hoy alcanzaría a crd y
cnt, pero no a los módulos que rellenan por su cuenta. **El equipo 2 está abajo**: esto queda como
aviso en `docs/equipos/saa-1.md` y aquí.

## Lista completa (salida del rastreo)
```
ejb/asoprep/serviceImpl/ReprocesoCargaPetroServiceImpl.java:354 | asientoService.anulaAsiento | registro.getAsiento(), usuario, motivoCompleto
ejb/cnt/serviceImpl/CargaAsientosManualesServiceImpl.java:231 | asientoContableService.generarAsientoManual | idEmpresa, tipoAsiento, asiento.fecha, observacion(nombreArchivo, asiento), solicitud.getUsuario().trim(), construirLineas(asiento)
ejb/cnt/serviceImpl/MayorizacionServiceImpl.java:152 | asientoService.actualizaMayorizacionByPeriodo | periodo, mayorizacionIngresada
ejb/cnt/serviceImpl/MayorizacionServiceImpl.java:251 | asientoService.eliminaIdMayorizacion | idProcesoMayorizacion
ejb/cnt/serviceImpl/MayorizacionServiceImpl.java:339 | asientoService.eliminaIdMayorizacion | codigoMayorizacion
ejb/crd/serviceImpl/AcuerdoCondonacionServiceImpl.java:920 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fechaAsiento, observacionCondonacion, acuerdo.getUsuarioRegistro(), lineasFinal, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/AcuerdoCondonacionServiceImpl.java:1004 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fechaAsiento, observacionCruce, acuerdo.getUsuarioRegistro(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/AporteServiceImpl.java:730 | asientoContableService.generarAsiento | idEmpresa, com.saa.rubros.TipoAsientos.CREDITOS, fecha, prefijo + " - reclasificación por jubilación", usuario, lineas, Long.valueOf(com.saa.rubros.ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/CierreCarteraServiceImpl.java:377 | asientoService.anulaAsiento | registro.getAsiento(), usuario, "Reverso de la corrida de cierre de cartera " + idCorrida + (motivo != null && !motivo.trim().isEmpty() ? ": " + motivo.trim() : "")
ejb/crd/serviceImpl/CierreCarteraServiceImpl.java:2201 | asientoContableService.generarAsiento | solicitud.getIdEmpresa(), TipoAsientos.CREDITOS, sub.getFecha(), sub.getGlosa(), solicitud.getUsuario(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/CobroCreditoServiceImpl.java:438 | asientoService.anulaAsiento | cobro.getAsientoTransitorio().getCodigo(), usuario, "Corrección del cobro " + idCobro + ": se " + (cambioMonto && cambioCuenta ? "corrigió el monto y la cuenta bancaria" : cambioMonto ? "corrigió el monto" : "corrigió la cuenta bancaria") + ". El asiento se rehace con los datos corregidos."
ejb/crd/serviceImpl/CobroCreditoServiceImpl.java:580 | asientoService.anulaAsiento | cobro.getAsientoDefinitivo().getCodigo(), usuario, "Anulación del cobro " + idCobro + ": " + motivo.trim()
ejb/crd/serviceImpl/CobroCreditoServiceImpl.java:584 | asientoService.anulaAsiento | cobro.getAsientoReparto().getCodigo(), usuario, "Anulación del cobro " + idCobro + ": " + motivo.trim()
ejb/crd/serviceImpl/CobroCreditoServiceImpl.java:588 | asientoService.anulaAsiento | cobro.getAsientoTransitorio().getCodigo(), usuario, "Anulación del cobro " + idCobro + ": " + motivo.trim()
ejb/crd/serviceImpl/CobroCreditoServiceImpl.java:759 | asientoService.anulaAsiento | cobro.getAsientoDefinitivo().getCodigo(), usuario, motivoLinea
ejb/crd/serviceImpl/CobroCreditoServiceImpl.java:763 | asientoService.anulaAsiento | cobro.getAsientoReparto().getCodigo(), usuario, motivoLinea
ejb/crd/serviceImpl/CobroCreditoServiceImpl.java:775 | asientoService.anulaAsiento | cobro.getAsientoTransitorio().getCodigo(), usuario, "Reverso del cobro " + idCobro + ": " + motivoTrim + ". El asiento se rehace."
ejb/crd/serviceImpl/CobroCreditoServiceImpl.java:2272 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, cobro.getFechaAfectacion(), observacionEnriquecida(cobro, null, "Cobro crédito " + cobro.getCodigo() + " - registro pendiente de aprobación" + (cobro.getObservacion() != null ? ": " + cobro.getObservacion() : "")), cobro.getUsuarioRegistro(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/CobroCreditoServiceImpl.java:2622 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, cobro.getFechaAfectacion(), observacionEnriquecida(cobro, detalles, "Cobro crédito " + cobro.getCodigo() + " - reparto" + (cobro.getObservacion() != null ? ": " + cobro.getObservacion() : "")), cobro.getUsuarioProceso(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/CobroCreditoServiceImpl.java:2826 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, cobro.getFechaAfectacion(), observacionEnriquecida(cobro, detalles, "Cobro crédito " + cobro.getCodigo() + " - asiento definitivo" + (cobro.getObservacion() != null ? ": " + cobro.getObservacion() : "")), cobro.getUsuarioProceso(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/CobroPetroContableServiceImpl.java:464 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fechaAsiento, "Cobro Petro/ARCH carga " + idCarga + " - recepción confirmada" + textoFechaRecepcion + (solicitud != null && solicitud.getObservacion() != null ? ": " + solicitud.getObservacion() : ""), solicitud != null ? solicitud.getUsuario() : null, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/CobroPetroContableServiceImpl.java:528 | asientoService.anulaAsiento | registro.getAsiento(), solicitud.getUsuario(), "Reverso de la confirmación de recepción de la carga " + idCarga + ": " + solicitud.getMotivo().trim()
ejb/crd/serviceImpl/CobroPetroContableServiceImpl.java:678 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fechaAsiento, "Reparto Petro/ARCH carga " + idCarga, null, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/CobroPetroContableServiceImpl.java:739 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fechaAsiento, "Complemento de apertura carga " + idCarga + " (D-024): " + detalle, null, complemento.getLineas(), Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/CobroPetroContableServiceImpl.java:1262 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fechaAsiento, "Aplicación Petro/ARCH carga " + idCarga, null, lineasFinal, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/ContabilidadPrestamoServiceImpl.java:228 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fechaCorte, prefijo + (ctx.getObservacion() != null ? ": " + ctx.getObservacion() : ""), ctx.getUsuario(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/ContabilidadPrestamoServiceImpl.java:602 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fechaCorte, prefijo, evento.getUsuario(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/ContabilidadPrestamoServiceImpl.java:723 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fechaAsiento, observacion, evento.getUsuario(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/ContabilidadPrestamoServiceImpl.java:844 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fechaAsiento, observacion, ctx.getUsuario(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/ContabilidadPrestamoServiceImpl.java:937 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fechaAsiento, observacion, ctx.getUsuario(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/ContabilidadPrestamoServiceImpl.java:988 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fecha, prefijo, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/ContabilidadPrestamoServiceImpl.java:1081 | asientoService.anulaAsiento | eventoAnulado.getNumeroAsiento(), usuario, "Reverso de la operación " + eventoAnulado.getCodigo() + " (" + eventoAnulado.getTipoOperacion() + ")" + (eventoAnulado.getMotivoAnulacion() != null ? ": " + eventoAnulado.getMotivoAnulacion() : "")
ejb/crd/serviceImpl/ContabilidadPrestamoServiceImpl.java:1093 | asientoService.selectById | eventoAnulado.getNumeroAsiento()
ejb/crd/serviceImpl/ContabilidadPrestamoServiceImpl.java:1328 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fechaAsiento != null ? fechaAsiento : fechaInicio, prefijo, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/ContabilidadPrestamoServiceImpl.java:1700 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, hoy, prefijo, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/DevolucionAporteServiceImpl.java:1746 | asientoContableService.generarAsiento | idEmpresa, com.saa.rubros.TipoAsientos.CREDITOS, devolucion.getFecha(), observacionReclasificacion, devolucion.getUsuarioRegistro(), lineas, Long.valueOf(com.saa.rubros.ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/DevolucionAporteServiceImpl.java:1903 | asientoService.anulaAsiento | devolucion.getNumeroAsientoReclasificacion(), usuario, "REVERSO DEVOLUCION " + devolucion.getCodigo() + " - " + causa
ejb/crd/serviceImpl/PagoPensionComplementariaServiceImpl.java:2870 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fechaAsiento, glosa, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/PagoPensionComplementariaServiceImpl.java:2960 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, pago.getFecha(), prefijo, pago.getUsuarioRegistro(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/PrestamoServiceImpl.java:977 | asientoService.selectById | prestamo.getIdAsientoEntrega()
ejb/crd/serviceImpl/PrestamoServiceImpl.java:1096 | asientoService.reversionAsiento | prestamo.getIdAsientoEntrega(), LocalDate.now()
ejb/crd/serviceImpl/ProvisionInteresServiceImpl.java:415 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fecha, "Reverso de provisión de intereses por cobro - " + origen, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/ProvisionInteresServiceImpl.java:513 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fecha, "Re-provisión de intereses por anulación - " + origen, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/ProvisionInteresServiceImpl.java:586 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fecha, "Reverso de provisión de intereses por condonación - " + origen, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/ProvisionInteresServiceImpl.java:677 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fecha, "Reverso de provisión de intereses por cobro tardío - " + origen, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/ProvisionInteresServiceImpl.java:821 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fecha, "Reverso de devengo de mora por cobro tardío - " + origen, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/ProvisionInteresServiceImpl.java:895 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, fecha, "Reclasificación de banda por cobro tardío - " + origen, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/RecepcionValorSeguroServiceImpl.java:261 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.CREDITOS, recepcion.getFecha(), observacion, usuario.trim(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR)
ejb/crd/serviceImpl/RecepcionValorSeguroServiceImpl.java:334 | asientoService.anulaAsiento | recepcion.getAsiento().getCodigo(), usuario.trim(), "Anulación de la recepción de valor de seguro " + idRecepcion + ": " + motivo.trim()
ejb/cxc/serviceImpl/AnticipoClienteServiceImpl.java:285 | asientoContableService.generarAsientoAnticipo | anticipo, TipoAsientos.ANTICIPOS_CLIENTE, usuario != null ? usuario : "SISTEMA"
ejb/cxc/serviceImpl/AnticipoClienteServiceImpl.java:436 | asientoContableService.generarAsientoAnticipoCliente | anticipo, idCuentaBancaria, TipoAsientos.ANTICIPOS_CLIENTE, usuario.getNombre() != null ? usuario.getNombre() : usuario.getCodigo().toString()
ejb/cxc/serviceImpl/AnticipoClienteServiceImpl.java:607 | asientoService.anulaAsiento | idAsiento
ejb/cxc/serviceImpl/AplicacionPagoCxcServiceImpl.java:516 | asientoContableService.generarAsientoAplicacionAnticipoCliente | idCliente, monto, idEmpresa, TipoAsientos.APLICACION_ANTICIPO_CLIENTE, fecha, observacionAsiento, usuarioNombre(idUsuario)
ejb/cxc/serviceImpl/AplicacionPagoCxcServiceImpl.java:652 | asientoContableService.generarAsientoCobroTransferenciaCxc | idCliente, valor, idCuentaBancaria, idEmpresa, TipoAsientos.COBRO_TRANSFERENCIA_CXC, fecha, observacionAsiento, usuarioNombre(idUsuario)
ejb/cxc/serviceImpl/AplicacionPagoCxcServiceImpl.java:1199 | asientoService.anulaAsiento | idAsiento, usuario, motivo
ejb/cxc/serviceImpl/FacturaServiceImpl.java:354 | asientoContableService.validarCuentasContables | factura.getTitular(), detalles, idEmpresa
ejb/cxc/serviceImpl/FacturaServiceImpl.java:900 | asientoContableService.generarAsientoFactura | factura.getId(), idEmpresa, com.saa.rubros.TipoAsientos.FACTURAS_VENTA, factura.getFecha(), obsAsiento, usuarioAsiento
ejb/cxc/serviceImpl/FacturaServiceImpl.java:965 | asientoContableService.validarCuentasContables | factura.getTitular(), detalles, idEmpresa
ejb/cxc/serviceImpl/FacturaServiceImpl.java:2479 | asientoContableService.generarAsientoFactura | factura.getId(), idEmpresa, com.saa.rubros.TipoAsientos.FACTURAS_VENTA, factura.getFecha(), obsAsiento, "SISTEMA"
ejb/cxc/serviceImpl/FacturaServiceImpl.java:2744 | asientoService.anulaAsientoDeDocumento | asiento
ejb/cxc/serviceImpl/LiquidacionCompraServiceImpl.java:243 | asientoContableService.validarCuentasContablesLiquidacion | liquidacion, detalles, idEmpresa
ejb/cxc/serviceImpl/LiquidacionCompraServiceImpl.java:919 | asientoContableService.generarAsientoLiquidacionCompraCompra | lqcc.getId(), idEmpresaConta, com.saa.rubros.TipoAsientos.LIQUIDACIONES_COMPRA_RECIBIDAS, fechaAsiento, obsAsiento, usuarioAsiento
ejb/cxc/serviceImpl/LiquidacionCompraServiceImpl.java:2317 | asientoService.anulaAsientoDeDocumento | asiento
ejb/cxc/serviceImpl/NotaCreditoServiceImpl.java:956 | asientoContableService.validarCuentasContablesNC | notaCredito.getTitular(), detalles, idEmpresaVal
ejb/cxc/serviceImpl/NotaCreditoServiceImpl.java:1349 | asientoContableService.generarAsientoNotaCredito | notaCredito.getId(), idEmpresa, com.saa.rubros.TipoAsientos.FACTURAS_VENTA, fechaAsiento, obsAsiento, usuarioAsiento
ejb/cxc/serviceImpl/NotaCreditoServiceImpl.java:1533 | asientoContableService.validarCuentasContablesNC | notaCredito.getTitular(), detalles, idEmpresa
ejb/cxc/serviceImpl/NotaCreditoServiceImpl.java:1649 | asientoService.anulaAsientoDeDocumento | asiento
ejb/cxc/serviceImpl/NotaDebitoServiceImpl.java:941 | asientoContableService.validarCuentasContablesND | notaDebito, idEmpresaVal
ejb/cxc/serviceImpl/NotaDebitoServiceImpl.java:1340 | asientoContableService.generarAsientoNotaDebito | notaDebito.getId(), idEmpresaConta, com.saa.rubros.TipoAsientos.FACTURAS_VENTA, fechaAsiento, obsAsiento, usuarioAsiento
ejb/cxc/serviceImpl/NotaDebitoServiceImpl.java:1519 | asientoContableService.validarCuentasContablesND | notaDebito, idEmpresa
ejb/cxc/serviceImpl/NotaDebitoServiceImpl.java:1759 | asientoService.anulaAsientoDeDocumento | asiento
ejb/cxc/serviceImpl/RetencionServiceImpl.java:742 | asientoContableService.validarCuentasContablesRetencion | retencion, detalles, idEmpresa
ejb/cxc/serviceImpl/RetencionServiceImpl.java:1208 | asientoContableService.generarAsientoRetencion | retencion.getId(), idEmpresa, TipoAsientos.RETENCIONES_EMITIDAS, fechaAsiento, obsAsiento, usuarioAsiento
ejb/cxc/serviceImpl/RetencionServiceImpl.java:1358 | asientoService.anulaAsientoDeDocumento | asiento
ejb/cxc/serviceImpl/RetencionV2ServiceImpl.java:770 | asientoContableService.generarAsientoRetencionV2 | retencion.getId(), idEmpresa, TipoAsientos.RETENCIONES_EMITIDAS_V2, fechaAsiento, obsAsiento, usuarioAsiento
ejb/cxc/serviceImpl/RetencionV2ServiceImpl.java:1507 | asientoContableService.validarCuentasContablesRetencion | retencionDummy, detallesParaValidar, idEmpresa
ejb/cxc/serviceImpl/RetencionV2ServiceImpl.java:1839 | asientoContableService.validarCuentasContablesRetencion | retencionDummy, detalesParaValidar, idEmpresa
ejb/cxc/serviceImpl/RetencionV2ServiceImpl.java:2223 | asientoService.anulaAsientoDeDocumento | asiento
ejb/cxp/serviceImpl/AnticipoProveedorServiceImpl.java:394 | asientoContableService.generarAsientoAnticipoProveedor | anticipo, idCuentaBancaria, TipoAsientos.ANTICIPOS_PROVEEDOR, fecha, nombreUsuario, observaciones
ejb/cxp/serviceImpl/AnticipoProveedorServiceImpl.java:460 | asientoService.reversionAsiento | idAsiento, fechaReverso
ejb/cxp/serviceImpl/AnticipoProveedorServiceImpl.java:461 | asientoService.reversionAsiento | idAsiento
ejb/cxp/serviceImpl/AnticipoProveedorServiceImpl.java:484 | asientoService.anulaAsiento | idAsiento
ejb/cxp/serviceImpl/AplicacionPagoCxpServiceImpl.java:839 | asientoContableService.generarAsientoAplicacionAnticipoProveedor | idTitularFactura, idTitularAnticipo, monto, idEmpresa, TipoAsientos.APLICACION_ANTICIPO_PROVEEDOR, fecha, observacionAsiento, usuarioNombre(idUsuario)
ejb/cxp/serviceImpl/AplicacionPagoCxpServiceImpl.java:998 | asientoContableService.generarAsientoPagoTransferenciaCxp | idProveedor, pago.getValor(), idCuentaBancaria, idEmpresa, TipoAsientos.PAGO_TRANSFERENCIA_CXP, fecha, observacionAsiento, usuarioNombre(idUsuario)
ejb/cxp/serviceImpl/AplicacionPagoCxpServiceImpl.java:1080 | asientoContableService.generarAsientoPagoTransferenciaCxp | idProveedor, pago.getValor(), idCuentaBancaria, idEmpresa, TipoAsientos.PAGO_TRANSFERENCIA_CXP, fecha, observacionAsiento, usuarioNombre(idUsuario)
ejb/cxp/serviceImpl/AplicacionPagoCxpServiceImpl.java:1194 | asientoContableService.generarAsientoAplicacionCajaChica | idTitularGasto, monto, idPlanCuentaCaja, idEmpresa, fecha, observacionAsiento, usuarioNombre(idUsuario)
ejb/cxp/serviceImpl/AplicacionPagoCxpServiceImpl.java:1937 | asientoService.reversionAsiento | idAsiento, fechaReverso
ejb/cxp/serviceImpl/AplicacionPagoCxpServiceImpl.java:1938 | asientoService.reversionAsiento | idAsiento
ejb/cxp/serviceImpl/AplicacionPagoCxpServiceImpl.java:1958 | asientoService.anulaAsiento | idAsiento
ejb/cxp/serviceImpl/DevolucionAnticipoProveedorServiceImpl.java:372 | asientoContableService.generarAsientoDevolucionAnticipoProveedor | idTitular, idCuentaBancaria, redondea(total), redondea(totalAnticipos), redondea(totalNotasCredito), idEmpresa, fechaDeposito, observacionAsiento, nombreUsuario
ejb/cxp/serviceImpl/DevolucionAnticipoProveedorServiceImpl.java:509 | asientoService.anulaAsiento | idAsiento
ejb/cxp/serviceImpl/FacturaCompraServiceImpl.java:225 | asientoService.anulaAsientoDeDocumento | asiento
ejb/cxp/serviceImpl/FacturaCompraServiceImpl.java:587 | asientoContableService.generarAsientoFacturaCompra | factura.getId(), solicitud.getIdEmpresa(), com.saa.rubros.TipoAsientos.FACTURAS_COMPRA, factura.getFecha() != null ? factura.getFecha().toLocalDate() : java.time.LocalDate.now(), obsAsiento, usuario.getNombre() != null ? usuario.getNombre() : "SISTEMA"
ejb/cxp/serviceImpl/FacturaCompraServiceImpl.java:621 | asientoContableService.existeCuentaConRolEstricto | codigoTitular, idEmpresa, 1L, RolPersona.PROVEEDOR
ejb/cxp/serviceImpl/LiquidacionCompraCompraServiceImpl.java:163 | asientoService.anulaAsientoDeDocumento | asiento
ejb/cxp/serviceImpl/NotaCreditoCompraServiceImpl.java:197 | asientoService.anulaAsientoDeDocumento | asiento
ejb/cxp/serviceImpl/NotaDebitoCompraServiceImpl.java:148 | asientoService.anulaAsientoDeDocumento | asiento
ejb/cxp/serviceImpl/PagoProgramadoServiceImpl.java:3013 | asientoContableService.generarAsientoEgresoTesoreria | egreso.getProducto().getId(), egreso.getDescripcion(), pago.getValor(), idCuentaBancaria, idEmpresa, TipoAsientos.EGRESO_TESORERIA, fecha, observacionAsiento, usuarioNombre(idUsuario)
ejb/cxp/serviceImpl/PagoProgramadoServiceImpl.java:3088 | asientoService.reversionAsiento | idAsiento, fechaReverso
ejb/cxp/serviceImpl/PagoProgramadoServiceImpl.java:3089 | asientoService.reversionAsiento | idAsiento
ejb/cxp/serviceImpl/PagoProgramadoServiceImpl.java:3110 | asientoService.anulaAsiento | idAsiento
ejb/cxp/serviceImpl/PagoProgramadoServiceImpl.java:3281 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.PAGO_ORIGEN_EXTERNO, fecha, observacionAsiento, usuarioNombre(idUsuario), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_PAGAR)
ejb/cxp/serviceImpl/PagoProgramadoServiceImpl.java:3366 | asientoContableService.generarAsientoReposicionCajaChica | caja.getPlanCuenta().getCodigo(), pago.getCuentaBancaria().getCodigo(), pago.getValor(), idEmpresa, fecha, observacionAsiento, usuarioNombre(idUsuario)
ejb/cxp/serviceImpl/PagoProgramadoServiceImpl.java:3450 | asientoContableService.generarAsientoAnticipoEmpleado | empleado.getCodigo(), pago.getValor(), pago.getCuentaBancaria().getCodigo(), idEmpresa, fecha, observacionAsiento, usuarioNombre(idUsuario)
ejb/cxp/serviceImpl/ProcesoCargaDocumentosServiceImpl.java:1246 | asientoContableService.generarAsientoFacturaCompra | idDocBD, idEmpresa, com.saa.rubros.TipoAsientos.FACTURAS_COMPRA, // El asiento lleva LocalDate y la factura guarda LocalDateTime. // Mismo recorte que hace el registro original (:1933). fc.getFecha() != null ? fc.getFecha().toLocalDate() : java.time.LocalDate.now(), conObservacionAdicional("Factura compra (recontabilizada): " + nvlStr(fc.getNumero(), String.valueOf(idDocBD)), doc), usuario
ejb/cxp/serviceImpl/ProcesoCargaDocumentosServiceImpl.java:2396 | asientoContableService.generarAsientoFacturaCompra | idFacturaCompra, idEmpresa, com.saa.rubros.TipoAsientos.FACTURAS_COMPRA, fechaDoc, obs, usuario
ejb/cxp/serviceImpl/ProcesoCargaDocumentosServiceImpl.java:2644 | asientoContableService.existeCuentaConRolEstricto | codigoTitular, idEmpresa, 1L, RolPersona.PROVEEDOR
ejb/cxp/serviceImpl/ProcesoCargaDocumentosServiceImpl.java:2665 | asientoContableService.existeCuentaConRolEstricto | codigoTitular, idEmpresa, 1L, RolPersona.CLIENTE
ejb/cxp/serviceImpl/ProcesoCargaDocumentosServiceImpl.java:4008 | asientoService.anulaAsientoDeDocumento | asiento
ejb/cxp/serviceImpl/ProcesoCargaDocumentosServiceImpl.java:5073 | asientoContableService.generarAsientoFacturaCompra | idDocBD, idEmpresa, com.saa.rubros.TipoAsientos.FACTURAS_COMPRA, fechaDoc, "Factura compra: " + obsBase, usuario
ejb/cxp/serviceImpl/ProcesoCargaDocumentosServiceImpl.java:5083 | asientoContableService.generarAsientoNotaCreditoCompra | idDocBD, idEmpresa, com.saa.rubros.TipoAsientos.NOTAS_CREDITO_COMPRA, fechaDoc, "NC compra: " + obsBase, usuario
ejb/cxp/serviceImpl/ProcesoCargaDocumentosServiceImpl.java:5093 | asientoContableService.generarAsientoNotaDebitoCompra | idDocBD, idEmpresa, com.saa.rubros.TipoAsientos.NOTAS_DEBITO_COMPRA, fechaDoc, "ND compra: " + obsBase, usuario
ejb/cxp/serviceImpl/ProcesoCargaDocumentosServiceImpl.java:5103 | asientoContableService.generarAsientoLiquidacionCompraCompra | idDocBD, idEmpresa, com.saa.rubros.TipoAsientos.LIQUIDACIONES_COMPRA_RECIBIDAS, fechaDoc, "Liquidación compra: " + obsBase, usuario
ejb/cxp/serviceImpl/ProcesoCargaDocumentosServiceImpl.java:5113 | asientoContableService.generarAsientoRetencionCompra | idDocBD, idEmpresa, com.saa.rubros.TipoAsientos.RETENCIONES_RECIBIDAS, fechaDoc, "Retención compra: " + obsBase, usuario
ejb/cxp/serviceImpl/ProcesoCargaDocumentosServiceImpl.java:5123 | asientoContableService.generarAsientoRetencionCompraV2 | idDocBD, idEmpresa, com.saa.rubros.TipoAsientos.RETENCIONES_RECIBIDAS_V2, fechaDoc, "Retención compra V2: " + obsBase, usuario
ejb/historico/serviceImpl/CargaHistoricaDocumentoServiceImpl.java:361 | asientoContableService.existeCuentaConRolEstricto | proveedor.getCodigo(), ctx.idEmpresa, 1L, RolPersona.PROVEEDOR
ejb/historico/serviceImpl/CargaHistoricaDocumentoServiceImpl.java:461 | asientoContableService.generarAsientoFacturaCompra | fc.getId(), ctx.idEmpresa, TipoAsientos.FACTURAS_COMPRA, fe, obsAsiento, ctx.usuario
ejb/historico/serviceImpl/CargaHistoricaDocumentoServiceImpl.java:621 | asientoContableService.generarAsientoRetencionV2 | ret.getId(), ctx.idEmpresa, TipoAsientos.RETENCIONES_EMITIDAS_V2, fe, obs, ctx.usuario
ejb/historico/serviceImpl/CargaHistoricaDocumentoServiceImpl.java:745 | asientoContableService.existeCuentaConRolEstricto | cliente.getCodigo(), ctx.idEmpresa, 1L, RolPersona.CLIENTE
ejb/historico/serviceImpl/CargaHistoricaDocumentoServiceImpl.java:828 | asientoContableService.generarAsientoFactura | f.getId(), ctx.idEmpresa, TipoAsientos.FACTURAS_VENTA, fe, obs, ctx.usuario
ejb/historico/serviceImpl/CargaHistoricaDocumentoServiceImpl.java:984 | asientoContableService.generarAsientoRetencionCompraV2 | ret.getId(), ctx.idEmpresa, TipoAsientos.RETENCIONES_RECIBIDAS_V2, fe, obs, ctx.usuario
ejb/historico/serviceImpl/CargaHistoricaDocumentoServiceImpl.java:1105 | asientoContableService.generarAsientoNotaCreditoCompra | nc.getId(), ctx.idEmpresa, TipoAsientos.NOTAS_CREDITO_COMPRA, fe, obs, ctx.usuario
ejb/historico/serviceImpl/CargaHistoricaDocumentoServiceImpl.java:1234 | asientoContableService.generarAsientoNotaCredito | nc.getId(), ctx.idEmpresa, TipoAsientos.FACTURAS_VENTA, fe, obs, ctx.usuario
ejb/historico/serviceImpl/CargaHistoricaDocumentoServiceImpl.java:1333 | asientoContableService.existeCuentaConRolEstricto | titular.getCodigo(), ctx.idEmpresa, 1L, proveedor ? RolPersona.PROVEEDOR : RolPersona.CLIENTE
ejb/historico/serviceImpl/CargaHistoricaNominaPasoServiceImpl.java:597 | asientoContableService.generarAsiento | ctx.idEmpresa, TipoAsientos.RECURSOS_HUMANOS, fp, recortar(traza + " comprobante " + pl.numeroComprobante, 1900), ctx.usuario, lineas, Long.valueOf(ModuloSistema.TESORERIA), "RRH"
ejb/rhh/service/ContabilizacionNominaService.java:123 | asientoService.anulaAsiento | idAsiento, usuario, motivo
ejb/rhh/serviceImpl/ContabilizacionNominaServiceImpl.java:259 | asientoContableService.generarAsiento | periodo.getEmpresa().getCodigo(), TipoAsientos.RECURSOS_HUMANOS, fechaContable(periodo), "Rol de pagos " + periodo.getMes() + "/" + periodo.getAnio(), usuario, lineas, Long.valueOf(ModuloSistema.RECURSOS_HUMANOS)
ejb/rhh/serviceImpl/ContabilizacionNominaServiceImpl.java:315 | asientoContableService.generarAsiento | periodo.getEmpresa().getCodigo(), TipoAsientos.RECURSOS_HUMANOS, fechaContable(periodo), "Provisiones de nomina " + periodo.getMes() + "/" + periodo.getAnio(), usuario, lineas, Long.valueOf(ModuloSistema.RECURSOS_HUMANOS)
ejb/rhh/serviceImpl/ContabilizacionNominaServiceImpl.java:368 | asientoService.anulaAsiento | periodo.getAsientoRol(), usuario, motivo
ejb/rhh/serviceImpl/ContabilizacionNominaServiceImpl.java:371 | asientoService.anulaAsiento | periodo.getAsientoProvisiones(), usuario, motivo
ejb/rhh/serviceImpl/ContabilizacionNominaServiceImpl.java:404 | asientoService.selectById | idAsiento
ejb/rhh/serviceImpl/ContabilizacionNominaServiceImpl.java:502 | asientoContableService.generarAsiento | periodo.getEmpresa().getCodigo(), TipoAsientos.RECURSOS_HUMANOS, fecha, "Pago de nomina " + periodo.getMes() + "/" + periodo.getAnio() + " orden " + (orden.getNumero() != null ? orden.getNumero() : idOrdenPago), usuario, lineas, Long.valueOf(ModuloSistema.TESORERIA)
ejb/rhh/serviceImpl/ContabilizacionNominaServiceImpl.java:689 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.RECURSOS_HUMANOS, liquidacion.getFechaSalida(), "Liquidacion de haberes de " + empleado.getApellidos() + " " + empleado.getNombres(), usuario, lineas, Long.valueOf(ModuloSistema.RECURSOS_HUMANOS)
ejb/rhh/serviceImpl/ContabilizacionNominaServiceImpl.java:1011 | asientoContableService.generarAsiento | idEmpresa, TipoAsientos.RECURSOS_HUMANOS, fecha, descripcion, usuario, lineas, Long.valueOf(ModuloSistema.TESORERIA)
ejb/rhh/serviceImpl/LiquidacionExternaServiceImpl.java:650 | asientoContableService.generarAsiento | liquidacion.getEmpresa().getCodigo(), TipoAsientos.RECURSOS_HUMANOS, fechaRespuesta, observacion, usuario, lineas, Long.valueOf(ModuloSistema.RECURSOS_HUMANOS)
ejb/rhh/serviceImpl/OrdenBeneficioSocialServiceImpl.java:599 | asientoService.anulaAsiento | idAsiento
ejb/tsr/serviceImpl/ChequeServiceImpl.java:224 | asientoService.selectById | datosAsiento[0]
ejb/tsr/serviceImpl/ChequeServiceImpl.java:269 | asientoService.insertarCabeceraAsiento | idEmpresa, nombreUsuario, observacion, TipoAsientos.INGRESOS
ejb/tsr/serviceImpl/ChequeServiceImpl.java:304 | asientoService.selectById | idAsiento
ejb/tsr/serviceImpl/ChequeServiceImpl.java:318 | asientoService.selectById | idAsiento
ejb/tsr/serviceImpl/ChequeServiceImpl.java:378 | asientoService.anulaAsiento | asiento.getCodigo()
ejb/tsr/serviceImpl/ChequeServiceImpl.java:414 | asientoService.verificaAnulacionReversion | asiento, ProcesosAsiento.ANULAR
ejb/tsr/serviceImpl/CierreCajaChicaServiceImpl.java:246 | asientoContableService.generarAsientoAjusteCajaChica | caja.getPlanCuenta().getCodigo(), idPlanCuentaDiferencia, valorAjuste, sobrante, caja.getEmpresa().getCodigo(), cierre.getFechaFin(), observacionAjuste, usuarioNombre(idUsuario)
ejb/tsr/serviceImpl/CierreCajaChicaServiceImpl.java:305 | asientoService.anulaAsiento | idAsiento
ejb/tsr/serviceImpl/CierreCajaServiceImpl.java:155 | asientoService.anulaAsiento | cierreCaja.getAsiento().getCodigo()
ejb/tsr/serviceImpl/CierreCajaServiceImpl.java:205 | asientoService.selectById | datosAsiento[0]
ejb/tsr/serviceImpl/CierreCajaServiceImpl.java:263 | asientoService.insertarCabeceraAsiento | idEmpresa, nombreUsuario, descripcion, TipoAsientos.ASIENTO_CIERRE
ejb/tsr/serviceImpl/CierreCajaServiceImpl.java:279 | asientoService.selectById | idAsiento
ejb/tsr/serviceImpl/CierreCajaServiceImpl.java:292 | asientoService.selectById | idAsiento
ejb/tsr/serviceImpl/CobroServiceImpl.java:358 | asientoService.insertarCabeceraAsiento | cobro.getEmpresa().getCodigo(), cobro.getNombreUsuario(), cobro.getDescripcion(), TipoAsientos.INGRESOS
ejb/tsr/serviceImpl/CobroServiceImpl.java:372 | asientoService.insertarCabeceraAsiento | cobro.getEmpresa().getCodigo(), cobro.getNombreUsuario(), descripcion, TipoAsientos.INGRESOS
ejb/tsr/serviceImpl/CobroServiceImpl.java:390 | asientoService.selectById | idAsiento
ejb/tsr/serviceImpl/CobroServiceImpl.java:400 | asientoService.selectById | idAsiento
ejb/tsr/serviceImpl/CobroServiceImpl.java:461 | asientoService.insertarCobroDetalleAsientoDebe | planCuenta, descripcion, valor, idAsiento
ejb/tsr/serviceImpl/CobroServiceImpl.java:474 | asientoService.insertarCobroDetalleAsientoDebe | planCuenta, descripcion, cobroTarjeta.getValor(), idAsiento
ejb/tsr/serviceImpl/CobroServiceImpl.java:487 | asientoService.insertarCobroDetalleAsientoDebe | planCuenta, descripcion, cobroRetencion.getValor(), idAsiento
ejb/tsr/serviceImpl/CobroServiceImpl.java:506 | asientoService.insertarCobroDetalleAsientoDebe | planCuenta, descripcion, cobroTransferencia.getValor(), idAsiento
ejb/tsr/serviceImpl/CobroServiceImpl.java:521 | asientoService.selectById | idAsiento
ejb/tsr/serviceImpl/CobroServiceImpl.java:547 | asientoService.selectById | idAsiento
ejb/tsr/serviceImpl/CobroServiceImpl.java:620 | asientoService.anulaAsiento | cobro.getAsiento().getCodigo()
ejb/tsr/serviceImpl/DebitoCreditoServiceImpl.java:219 | asientoService.insertarCabeceraAsiento | empresa, usuario, descripcion, TipoAsientos.CREDITO_BANCARIO
ejb/tsr/serviceImpl/DebitoCreditoServiceImpl.java:220 | asientoService.selectById | datosAsiento[0]
ejb/tsr/serviceImpl/DebitoCreditoServiceImpl.java:253 | asientoService.insertarCabeceraAsiento | empresa, usuario, descripcion, TipoAsientos.DEBITO_BANCARIO
ejb/tsr/serviceImpl/DebitoCreditoServiceImpl.java:254 | asientoService.selectById | datosAsiento[0]
ejb/tsr/serviceImpl/DebitoCreditoServiceImpl.java:306 | asientoService.selectById | idAsiento
ejb/tsr/serviceImpl/DepositoServiceImpl.java:283 | asientoService.insertarCabeceraAsiento | idEmpresa, deposito.getNombreUsuario(), observacion, TipoAsientos.INGRESOS
ejb/tsr/serviceImpl/DepositoServiceImpl.java:284 | asientoService.selectById | datosAsiento[0]
ejb/tsr/serviceImpl/DepositoServiceImpl.java:391 | asientoService.anulaAsiento | detalleDeposito.getAsiento().getCodigo()
ejb/tsr/serviceImpl/DepositoServiceImpl.java:396 | asientoService.anulaAsiento | deposito.getAsiento().getCodigo()
ejb/tsr/serviceImpl/DetalleDepositoServiceImpl.java:171 | asientoService.selectById | datosAsiento[0]
ejb/tsr/serviceImpl/DetalleDepositoServiceImpl.java:187 | asientoService.insertarCabeceraAsiento | idEmpresa, detalleDeposito.getNombreUsuario(), observacion, TipoAsientos.INGRESOS
ejb/tsr/serviceImpl/DetalleDepositoServiceImpl.java:188 | asientoService.selectById | datosAsiento[0]
ejb/tsr/serviceImpl/EgresoServiceImpl.java:318 | asientoContableService.generarAsientoEgresoContraAnticipo | idProductoPago, idTitular, valor, idEmpresa, fechaEgreso, observacionAsiento, nombreUsuario
ejb/tsr/serviceImpl/EgresoServiceImpl.java:469 | asientoService.anulaAsiento | idAsiento
ejb/tsr/serviceImpl/IngresoServiceImpl.java:163 | asientoContableService.generarAsientoIngresoTesoreria | idProductoCobro, concepto, valor, idCuentaBancaria, idEmpresa, TipoAsientos.INGRESO_TESORERIA, fechaIngreso, observacionAsiento, usuarioNombre(idUsuario)
ejb/tsr/serviceImpl/IngresoServiceImpl.java:240 | asientoService.anulaAsiento | idAsiento
ejb/tsr/serviceImpl/MovimientoCajaChicaServiceImpl.java:248 | asientoContableService.generarAsientoGastoCajaChica | idProducto, caja.getNombre(), descripcion.trim(), valor, caja.getPlanCuenta().getCodigo(), caja.getEmpresa().getCodigo(), fechaGasto, observacionAsiento, usuarioNombre(idUsuario)
ejb/tsr/serviceImpl/MovimientoCajaChicaServiceImpl.java:411 | asientoService.anulaAsiento | idAsiento
ejb/tsr/serviceImpl/PagoServiceImpl.java:224 | asientoService.insertarCabeceraAsiento | pago.getEmpresa().getCodigo(), pago.getNombreUsuario(), "PAGO Nro "+pago.getCodigo()+ " A PROVEEDOR "+pago.getProveedor()+ ". DESCRIPCION: "+pago.getDescripcion(), TipoAsientos.EGRESOS
ejb/tsr/serviceImpl/PagoServiceImpl.java:231 | asientoService.selectById | datosAsiento[0]
ejb/tsr/serviceImpl/PagoServiceImpl.java:287 | asientoService.anulaAsiento | asiento.getCodigo()
ejb/tsr/serviceImpl/TransferenciaServiceImpl.java:109 | asientoService.generaCabeceraTransferencia | empresa, usuario, concepto
ejb/tsr/serviceImpl/TransferenciaServiceImpl.java:142 | asientoService.selectById | datosAsiento[0]
ejb/tsr/serviceImpl/TransferenciaServiceImpl.java:203 | asientoService.selectById | datosAsiento[0]
ejb/tsr/serviceImpl/TransferenciaServiceImpl.java:225 | asientoService.generaCabeceraTransferencia | empresa, usuario, concepto
ws/rest/cnt/AsientoRest.java:115 | asientoService.reversionAsiento | idAsiento
ws/rest/cnt/AsientoRest.java:144 | asientoService.anulaAsiento | idAsiento, usuario, motivo, fecha
ws/rest/cnt/AsientoRest.java:158 | asientoService.copiaAsiento | idAsiento
ws/rest/cnt/AsientoRest.java:170 | asientoService.saveSingle | registro
ws/rest/cnt/AsientoRest.java:183 | asientoService.saveSingle | registro
ws/rest/cnt/AsientoRest.java:198 | asientoService.selectByCriteria | registros```

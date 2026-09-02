package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.ejb.UsuarioDaoService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.ejb.cnt.dao.PlanCuentaDaoService;
import com.saa.ejb.crd.dao.AporteDaoService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.service.CargaArchivoService;
import com.saa.ejb.crd.service.ClasificadorBandaService;
import com.saa.ejb.crd.service.CobroPetroContableService;
import com.saa.ejb.crd.service.ConfiguracionContabilidadService;
import com.saa.ejb.crd.dao.AsientoCargaPetroDaoService;
import com.saa.ejb.crd.dao.DetalleCargaArchivoDaoService;
import com.saa.ejb.crd.dao.TransferenciaCargaPetroDaoService;
import com.saa.ejb.crd.service.dto.AsientoPetroDTO;
import com.saa.ejb.crd.service.dto.BandaProductoDetalle;
import com.saa.ejb.crd.service.dto.EstadoContablePetro;
import com.saa.ejb.crd.service.dto.ResultadoClasificacionBanda;
import com.saa.ejb.crd.service.dto.ResultadoConfirmarRecepcion;
import com.saa.ejb.crd.service.dto.ResultadoReversarRecepcion;
import com.saa.ejb.crd.service.dto.ResumenTransferenciasCarga;
import com.saa.ejb.crd.service.dto.SolicitudConfirmarRecepcion;
import com.saa.ejb.crd.service.dto.SolicitudReversarRecepcion;
import com.saa.ejb.crd.service.dto.SolicitudTransferenciaCargaPetro;
import com.saa.ejb.crd.service.dto.TransferenciaCargaPetroDTO;
import com.saa.ejb.tsr.dao.BancoDaoService;
import com.saa.ejb.tsr.dao.BancoExternoDaoService;
import com.saa.ejb.tsr.dao.CuentaBancariaDaoService;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.crd.AsientoCargaPetro;
import com.saa.model.crd.CargaArchivo;
import com.saa.model.crd.DetalleCargaArchivo;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.Producto;
import com.saa.model.crd.TransferenciaCargaPetro;
import com.saa.model.tsr.Banco;
import com.saa.model.tsr.BancoExterno;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.rubros.CrdEstadoCargaArchivo;
import com.saa.rubros.CrdLineaAsiento;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.PlantillasCredito;
import com.saa.rubros.SubProcesoCobroPetro;
import com.saa.rubros.TipoAsientos;
import com.saa.rubros.TipoCarteraBanda;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @see CobroPetroContableService
 *
 * Cobro de Petro en dos pasos. CONTRATO CONGELADO con el frontend — ver
 * {@code docs/logica-negocio/crd/API-COBRO-PETRO-DOS-PASOS.md}. No cambiar la forma de los
 * DTO ni el orden de las validaciones sin acordarlo con el árbitro.
 *
 * @author Sistema SAA
 * @since 2026-08-28
 */
@Stateless
public class CobroPetroContableServiceImpl implements CobroPetroContableService {

    private static final double TOLERANCIA_CUADRE = 0.01;

    /** TRCRIDST / ANCPIDST: 1 vigente, 0 anulado/reversado. */
    private static final long VIGENTE = 1L;
    private static final long ANULADO = 0L;

    /** CRD.TPAP.TPAPCDGO — literales usados en todo el módulo (no hay rubro dedicado). */
    private static final long TIPO_APORTE_JUBILACION = 9L;
    private static final long TIPO_APORTE_CESANTIA = 11L;

    /** CRD.TPPR.TPPRCDGO — verificado en la BD local: 1 quirografario, 2 hipotecario, 3 prendario. */
    private static final long TIPO_PRESTAMO_HIPOTECARIO = 2L;
    private static final long TIPO_PRESTAMO_PRENDARIO = 3L;

    /**
     * Líneas de seguro de préstamo en la plantilla 21, RAW (no en CrdLineaAsiento: fuera del
     * alcance de Petro por diseño — §3.3 del levantamiento no las necesita, salvo que un pago
     * de la carga sí traiga seguro de incendio, en cuyo caso hace falta la cuenta igual).
     */
    private static final int AUX1_SEGURO_HIPOTECARIO = 42;
    private static final int AUX1_SEGURO_PRENDARIO = 43;

    @EJB
    private CargaArchivoService cargaArchivoService;

    @EJB
    private TransferenciaCargaPetroDaoService transferenciaCargaPetroDaoService;

    @EJB
    private AsientoCargaPetroDaoService asientoCargaPetroDaoService;

    @EJB
    private DetalleCargaArchivoDaoService detalleCargaArchivoDaoService;

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private AporteDaoService aporteDaoService;

    @EJB
    private ClasificadorBandaService clasificadorBandaService;

    @EJB
    private PlanCuentaDaoService planCuentaDaoService;

    @EJB
    private CuentaBancariaDaoService cuentaBancariaDaoService;

    @EJB
    private BancoDaoService bancoDaoService;

    /** Líneas de aportes/préstamos del reparto (2026-08-31) — extraídas de este archivo para
     * que CBCR use la misma implementación. Ver su javadoc. */
    @EJB
    private com.saa.ejb.crd.service.ContabilizacionIndividualCreditoService contabilizacionIndividualCreditoService;

    @EJB
    private BancoExternoDaoService bancoExternoDaoService;

    @EJB
    private UsuarioDaoService usuarioDaoService;

    @EJB
    private ConfiguracionContabilidadService configuracionContabilidadService;

    @EJB
    private AsientoContableService asientoContableService;

    @EJB
    private AsientoService asientoService;

    @EJB
    private PlantillaService plantillaService;

    @EJB
    private DetallePlantillaDaoService detallePlantillaDaoService;

    // =====================================================================
    // Transferencias (§2.1 del contrato)
    // =====================================================================

    @Override
    public ResumenTransferenciasCarga resumenTransferencias(Long idCarga) throws Throwable {
        System.out.println("CobroPetroContableService.resumenTransferencias - Carga: " + idCarga);
        CargaArchivo carga = buscarCarga(idCarga);

        List<TransferenciaCargaPetro> filas = transferenciaCargaPetroDaoService.selectByCarga(idCarga);
        List<TransferenciaCargaPetroDTO> dtos = new ArrayList<>();
        double totalTransferencias = 0.0;
        for (TransferenciaCargaPetro fila : filas) {
            dtos.add(aDto(fila));
            if (fila.getIdEstado() != null && fila.getIdEstado() == VIGENTE) {
                totalTransferencias += nvl(fila.getValor());
            }
        }
        totalTransferencias = redondear(totalTransferencias);

        double totalArchivo = redondear(nvl(carga.getTotalDescontado()));
        double diferencia = redondear(totalArchivo - totalTransferencias);
        boolean confirmada = carga.getFechaAutorizacionContabilidad() != null;

        ResumenTransferenciasCarga resumen = new ResumenTransferenciasCarga();
        resumen.setIdCarga(idCarga);
        resumen.setPeriodo(periodo(carga));
        resumen.setNombreFilial(carga.getFilial() != null ? carga.getFilial().getNombre() : null);
        resumen.setTotalArchivo(totalArchivo);
        resumen.setTotalTransferencias(totalTransferencias);
        resumen.setDiferencia(diferencia);
        resumen.setCuadra(Math.abs(diferencia) <= TOLERANCIA_CUADRE);
        resumen.setConfirmada(confirmada);
        resumen.setUsuarioConfirma(confirmada && carga.getUsuarioContabilidadConfirma() != null
                ? carga.getUsuarioContabilidadConfirma().getNombre() : null);
        resumen.setFechaConfirmacion(carga.getFechaAutorizacionContabilidad());
        resumen.setTransferencias(dtos);
        return resumen;
    }

    @Override
    public TransferenciaCargaPetroDTO registrarTransferencia(SolicitudTransferenciaCargaPetro solicitud)
            throws Throwable {
        System.out.println("CobroPetroContableService.registrarTransferencia - Carga: "
                + (solicitud != null ? solicitud.getIdCarga() : null));
        if (solicitud == null || solicitud.getIdCarga() == null) {
            throw new IncomeException("idCarga es obligatorio");
        }
        CargaArchivo carga = buscarCarga(solicitud.getIdCarga());
        exigeNoConfirmada(carga, "registrar una transferencia");

        if (solicitud.getIdCuentaBancaria() == null) {
            throw new IncomeException("idCuentaBancaria es obligatorio");
        }
        if (solicitud.getValor() == null || solicitud.getValor() <= 0.0) {
            throw new IncomeException("El valor de la transferencia debe ser mayor a cero");
        }
        if (solicitud.getFecha() == null) {
            throw new IncomeException("La fecha de la transferencia es obligatoria");
        }

        CuentaBancaria cuentaBancaria = cuentaBancariaDaoService.selectById(
                solicitud.getIdCuentaBancaria(), NombreEntidadesTesoreria.CUENTA_BANCARIA);
        if (cuentaBancaria == null) {
            throw new IncomeException("No existe la cuenta bancaria " + solicitud.getIdCuentaBancaria());
        }

        TransferenciaCargaPetro transferencia = new TransferenciaCargaPetro();
        transferencia.setCargaArchivo(carga);
        transferencia.setCuentaBancaria(cuentaBancaria);
        if (solicitud.getIdBanco() != null) {
            transferencia.setBanco(bancoDaoService.selectById(solicitud.getIdBanco(),
                    NombreEntidadesTesoreria.BANCO));
        }
        if (solicitud.getIdBancoExterno() != null) {
            transferencia.setBancoExterno(bancoExternoDaoService.selectById(solicitud.getIdBancoExterno(),
                    NombreEntidadesTesoreria.BANCO_EXTERNO));
        }
        transferencia.setCuentaOrigen(solicitud.getCuentaOrigen());
        transferencia.setNumero(solicitud.getNumero());
        transferencia.setValor(redondear(solicitud.getValor()));
        transferencia.setFecha(solicitud.getFecha());
        transferencia.setObservacion(solicitud.getObservacion());
        transferencia.setIdEstado(VIGENTE);
        transferencia.setUsuarioRegistro(solicitud.getUsuario());
        transferencia.setFechaRegistro(LocalDateTime.now());

        transferencia = transferenciaCargaPetroDaoService.save(transferencia, null);
        return aDto(transferencia);
    }

    @Override
    public void anularTransferencia(Long idTransferencia, String usuario) throws Throwable {
        System.out.println("CobroPetroContableService.anularTransferencia - Transferencia: "
                + idTransferencia);
        if (idTransferencia == null) {
            throw new IncomeException("idTransferencia es obligatorio");
        }
        TransferenciaCargaPetro transferencia = transferenciaCargaPetroDaoService.selectById(
                idTransferencia, NombreEntidadesCredito.TRANSFERENCIA_CARGA_PETRO);
        if (transferencia == null) {
            throw new IncomeException("No existe la transferencia " + idTransferencia);
        }
        exigeNoConfirmada(transferencia.getCargaArchivo(), "anular una transferencia");

        transferencia.setIdEstado(ANULADO);
        transferenciaCargaPetroDaoService.save(transferencia, transferencia.getCodigo());
    }

    // =====================================================================
    // Paso 1 — confirmar / reversar (§2.2, §2.3 del contrato)
    // =====================================================================

    @Override
    public ResultadoConfirmarRecepcion confirmarRecepcion(Long idCarga,
            SolicitudConfirmarRecepcion solicitud) throws Throwable {
        System.out.println("CobroPetroContableService.confirmarRecepcion - Carga: " + idCarga);

        // Validación 1: la carga existe.
        CargaArchivo carga = buscarCarga(idCarga);

        // Validación 2: no está confirmada ya. NUNCA se lee CRARESTD para esto — es
        // transitorio y avanza a PROCESADO en cuanto se ejecuta el paso 2 (ver
        // CrdEstadoCargaArchivo.CONFIRMADO_CONTABILIDAD). El marcador duradero es
        // fechaAutorizacionContabilidad.
        if (carga.getFechaAutorizacionContabilidad() != null) {
            throw new IncomeException("La carga " + idCarga + " ya fue confirmada el "
                    + carga.getFechaAutorizacionContabilidad() + " por "
                    + (carga.getUsuarioContabilidadConfirma() != null
                            ? carga.getUsuarioContabilidadConfirma().getNombre() : "?")
                    + "; reverse la confirmación antes de volver a confirmar.");
        }

        // Validación 3: al menos una transferencia vigente.
        List<TransferenciaCargaPetro> vigentes =
                transferenciaCargaPetroDaoService.selectVigentesByCarga(idCarga);
        if (vigentes == null || vigentes.isEmpty()) {
            throw new IncomeException("La carga " + idCarga + " no tiene ninguna transferencia"
                    + " registrada; registre al menos una antes de confirmar la recepción.");
        }

        // Validación 4: la suma cuadra con el total del archivo (tolerancia 0.01). La base no
        // puede garantizarlo con un CHECK porque involucra otra tabla — lo valida el servicio.
        double totalTransferencias = transferenciaCargaPetroDaoService.sumaValorVigentesByCarga(idCarga);
        double totalArchivo = redondear(nvl(carga.getTotalDescontado()));
        double diferencia = redondear(totalArchivo - totalTransferencias);
        if (Math.abs(diferencia) > TOLERANCIA_CUADRE) {
            throw new IncomeException("Las transferencias de la carga " + idCarga + " suman $"
                    + totalTransferencias + " pero el archivo dice que se descontó $" + totalArchivo
                    + " (diferencia $" + diferencia + "); revise las transferencias antes de confirmar.");
        }

        // Validación 5: la carga está en un estado que admite la confirmación.
        if (carga.getEstado() == null || carga.getEstado() != CrdEstadoCargaArchivo.CARGADO) {
            throw new IncomeException("La carga " + idCarga + " está en estado "
                    + carga.getEstado() + "; solo se puede confirmar la recepción de una carga"
                    + " en estado CARGADO (" + CrdEstadoCargaArchivo.CARGADO + ").");
        }

        // Efecto: sella el "visto de contabilidad" — CRARUSCC/CRARFCAC son el rastro
        // DURADERO; CRARESTD es solo transitorio (avanza a PROCESADO en el paso 2).
        LocalDateTime ahora = LocalDateTime.now();
        carga.setUsuarioContabilidadConfirma(usuarioDaoService.selectByNombre(
                solicitud != null ? solicitud.getUsuario() : null));
        carga.setFechaAutorizacionContabilidad(ahora);
        carga.setEstado(Long.valueOf(CrdEstadoCargaArchivo.CONFIRMADO_CONTABILIDAD));
        carga = cargaArchivoService.saveSingle(carga);

        ResultadoConfirmarRecepcion resultado = new ResultadoConfirmarRecepcion();
        resultado.setIdCarga(idCarga);
        resultado.setConfirmada(Boolean.TRUE);

        // Flag global de contabilidad de CRD: apagado, la confirmación IGUAL ocurre (ya
        // selló arriba) pero sin asiento. No es un error — mismo criterio que
        // CierreCarteraServiceImpl.generaAsiento.
        if (!configuracionContabilidadService.contabilidadActiva()) {
            System.out.println("CobroPetroContableService.confirmarRecepcion - contabilidad de CRD"
                    + " INACTIVA: carga " + idCarga + " confirmada sin generar asiento.");
            resultado.setContabilidadActiva(Boolean.FALSE);
            resultado.setMensaje("Recepción confirmada. La contabilidad de CRD está desactivada:"
                    + " no se generó asiento.");
            return resultado;
        }

        // idEmpresa no viaja en el contrato (SolicitudConfirmarRecepcion no lo trae, y
        // CargaArchivo/Filial no tienen FK a Empresa — verificado en CRD.FLLL, 4 columnas).
        // Se deriva de la cuenta bancaria de la primera transferencia vigente: toda
        // transferencia exige una CuentaBancaria, y esta siempre resuelve un PlanCuenta con
        // empresa (verificado contra TSR.CNBC/CNT.PLNN).
        Long idEmpresa = resolverEmpresa(vigentes);

        Long idPlantilla = plantillaService.codigoByAlterno(PlantillasCredito.COBRO_TRANSITORIO_PETRO,
                idEmpresa);
        if (idPlantilla == null) {
            throw new IncomeException("No existe la plantilla contable alterno "
                    + PlantillasCredito.COBRO_TRANSITORIO_PETRO
                    + " (COBRO PETROECUADOR/ARCH CORRELACIONADO) para la empresa " + idEmpresa + ".");
        }
        DetallePlantilla lineaTransitoria = detallePlantillaDaoService.selectByPlantillaYAuxiliar(
                idPlantilla, 1);
        if (lineaTransitoria == null || lineaTransitoria.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno "
                    + PlantillasCredito.COBRO_TRANSITORIO_PETRO
                    + " no tiene la línea de la cuenta transitoria (2.3.01.15.01).");
        }

        List<DetalleAsiento> lineas = new ArrayList<>();
        for (TransferenciaCargaPetro transferencia : vigentes) {
            if (transferencia.getCuentaBancaria() == null
                    || transferencia.getCuentaBancaria().getPlanCuenta() == null) {
                throw new IncomeException("La transferencia " + transferencia.getCodigo()
                        + " tiene una cuenta bancaria sin cuenta contable asignada.");
            }
            PlanCuenta cuenta = transferencia.getCuentaBancaria().getPlanCuenta();
            DetalleAsiento debe = new DetalleAsiento();
            debe.setPlanCuenta(cuenta);
            debe.setNumeroCuenta(cuenta.getCuentaContable());
            debe.setNombreCuenta(cuenta.getNombre());
            debe.setDescripcion("Transferencia " + (transferencia.getNumero() != null
                    ? transferencia.getNumero() : transferencia.getCodigo())
                    + (transferencia.getBancoExterno() != null
                            ? " - " + transferencia.getBancoExterno().getNombre() : ""));
            debe.setValorDebe(transferencia.getValor());
            debe.setValorHaber(0.0);
            lineas.add(debe);
        }

        DetalleAsiento haber = new DetalleAsiento();
        haber.setPlanCuenta(lineaTransitoria.getPlanCuenta());
        haber.setNumeroCuenta(lineaTransitoria.getPlanCuenta().getCuentaContable());
        haber.setNombreCuenta(lineaTransitoria.getPlanCuenta().getNombre());
        haber.setDescripcion("Cobro Petro/ARCH carga " + idCarga + " - cuenta transitoria");
        haber.setValorDebe(0.0);
        haber.setValorHaber(totalTransferencias);
        lineas.add(haber);

        LocalDate fechaAsiento = fechaMasReciente(vigentes);
        com.saa.model.cnt.Asiento asiento = asientoContableService.generarAsiento(idEmpresa,
                TipoAsientos.CREDITOS, fechaAsiento,
                "Cobro Petro/ARCH carga " + idCarga + " - recepción confirmada"
                        + (solicitud != null && solicitud.getObservacion() != null
                                ? ": " + solicitud.getObservacion() : ""),
                solicitud != null ? solicitud.getUsuario() : null, lineas,
                Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        AsientoCargaPetro registro = new AsientoCargaPetro();
        registro.setCargaArchivo(carga);
        registro.setSubProceso(Long.valueOf(SubProcesoCobroPetro.TRANSITORIO));
        registro.setAsiento(asiento.getCodigo());
        registro.setNumeroAsiento(asiento.getNumero() != null ? asiento.getNumero().toString() : null);
        registro.setFecha(fechaAsiento);
        registro.setValor(totalTransferencias);
        registro.setCantidad(Long.valueOf(lineas.size()));
        registro.setIdEstado(VIGENTE);
        registro.setUsuarioRegistro(solicitud != null ? solicitud.getUsuario() : null);
        registro.setFechaRegistro(ahora);
        registro.setIpRegistro(solicitud != null ? solicitud.getIp() : null);
        asientoCargaPetroDaoService.save(registro, null);

        resultado.setIdAsiento(asiento.getCodigo());
        resultado.setNumeroAsiento(registro.getNumeroAsiento());
        resultado.setFechaAsiento(fechaAsiento);
        resultado.setValorAsiento(totalTransferencias);
        resultado.setContabilidadActiva(Boolean.TRUE);
        resultado.setMensaje("Recepción confirmada y asiento transitorio generado.");
        return resultado;
    }

    @Override
    public ResultadoReversarRecepcion reversarRecepcion(Long idCarga,
            SolicitudReversarRecepcion solicitud) throws Throwable {
        System.out.println("CobroPetroContableService.reversarRecepcion - Carga: " + idCarga);
        CargaArchivo carga = buscarCarga(idCarga);

        if (carga.getFechaAutorizacionContabilidad() == null) {
            throw new IncomeException("La carga " + idCarga + " no está confirmada; no hay nada"
                    + " que reversar.");
        }
        // El paso 2 no puede ejecutarse sin el paso 1 (ver el guard nuevo en
        // aplicarPagosArchivoPetro): si la carga ya llegó a PROCESADO, el paso 2 ya corrió y
        // hay que reversarlo primero.
        if (carga.getEstado() != null && carga.getEstado() == CrdEstadoCargaArchivo.PROCESADO) {
            throw new IncomeException("La carga " + idCarga + " ya fue procesada (paso 2 hecho);"
                    + " reverse primero el procesamiento del archivo antes de reversar la"
                    + " recepción.");
        }
        if (solicitud == null || solicitud.getMotivo() == null || solicitud.getMotivo().trim().isEmpty()) {
            throw new IncomeException("El motivo del reverso es obligatorio.");
        }

        Long idAsientoAnulado = null;
        AsientoCargaPetro registro = asientoCargaPetroDaoService.selectVigenteByCargaYSubProceso(
                idCarga, SubProcesoCobroPetro.TRANSITORIO);
        if (registro != null) {
            if (registro.getAsiento() != null) {
                asientoService.anulaAsiento(registro.getAsiento(), solicitud.getUsuario(),
                        "Reverso de la confirmación de recepción de la carga " + idCarga + ": "
                                + solicitud.getMotivo().trim());
                idAsientoAnulado = registro.getAsiento();
            }
            registro.setIdEstado(ANULADO);
            registro.setObservacion(concatena(registro.getObservacion(),
                    "REVERSADO: " + solicitud.getMotivo().trim()));
            asientoCargaPetroDaoService.save(registro, registro.getCodigo());
        }

        carga.setUsuarioContabilidadConfirma(null);
        carga.setFechaAutorizacionContabilidad(null);
        carga.setEstado(Long.valueOf(CrdEstadoCargaArchivo.CARGADO));
        cargaArchivoService.saveSingle(carga);

        ResultadoReversarRecepcion resultado = new ResultadoReversarRecepcion();
        resultado.setIdCarga(idCarga);
        resultado.setConfirmada(Boolean.FALSE);
        resultado.setIdAsientoAnulado(idAsientoAnulado);
        resultado.setMensaje("Confirmación de recepción reversada.");
        return resultado;
    }

    // =====================================================================
    // Estado contable (§2.4 del contrato)
    // =====================================================================

    @Override
    public EstadoContablePetro estadoContable(Long idCarga) throws Throwable {
        System.out.println("CobroPetroContableService.estadoContable - Carga: " + idCarga);
        buscarCarga(idCarga);

        EstadoContablePetro estado = new EstadoContablePetro();
        estado.setIdCarga(idCarga);
        estado.setContabilidadActiva(configuracionContabilidadService.contabilidadActiva());

        List<AsientoPetroDTO> asientos = new ArrayList<>();
        for (AsientoCargaPetro registro : asientoCargaPetroDaoService.selectByCarga(idCarga)) {
            AsientoPetroDTO dto = new AsientoPetroDTO();
            dto.setTipo(registro.getSubProceso());
            dto.setTipoTexto(textoSubProceso(registro.getSubProceso()));
            dto.setIdAsiento(registro.getAsiento());
            dto.setNumeroAsiento(registro.getNumeroAsiento());
            dto.setFecha(registro.getFecha());
            dto.setValor(registro.getValor());
            dto.setLineas(registro.getCantidad());
            dto.setEstado(registro.getIdEstado());
            dto.setUsuarioRegistro(registro.getUsuarioRegistro());
            dto.setFechaRegistro(registro.getFechaRegistro());
            asientos.add(dto);
        }
        estado.setAsientos(asientos);
        return estado;
    }

    // =====================================================================
    // Paso 2a — reparto (§3.3 del levantamiento; lo llama aplicarPagosArchivoPetro)
    // =====================================================================

    @Override
    public void contabilizarReparto(Long idCarga) throws Throwable {
        System.out.println("CobroPetroContableService.contabilizarReparto - Carga: " + idCarga);
        CargaArchivo carga = buscarCarga(idCarga);

        if (!configuracionContabilidadService.contabilidadActiva()) {
            System.out.println("CobroPetroContableService.contabilizarReparto - contabilidad de"
                    + " CRD INACTIVA: no se genera el asiento de reparto de la carga " + idCarga);
            return;
        }

        // Desglose aportes/préstamos: DTCATTDO por producto. AH = aportes; el resto
        // (PE/PH/PQ/PP/HS) = préstamos — "TODO" incluido el seguro, §2.2 del levantamiento.
        // No hace falta traceability hacia CRD.PGPR/CRD.APRT para este asiento: el total ya
        // está en CRD.DTCA, que SÍ pertenece a esta carga sin ambigüedad.
        List<DetalleCargaArchivo> detalles = detalleCargaArchivoDaoService.selectByCargaArchivo(idCarga);
        double totalAportes = 0.0;
        double totalPrestamos = 0.0;
        if (detalles != null) {
            for (DetalleCargaArchivo detalle : detalles) {
                double totalDetalle = nvl(detalle.getTotalDescontado());
                if ("AH".equals(detalle.getCodigoPetroProducto())) {
                    totalAportes += totalDetalle;
                } else {
                    totalPrestamos += totalDetalle;
                }
            }
        }
        totalAportes = redondear(totalAportes);
        totalPrestamos = redondear(totalPrestamos);
        double total = redondear(totalAportes + totalPrestamos);
        if (total <= 0.0) {
            System.out.println("CobroPetroContableService.contabilizarReparto - carga " + idCarga
                    + " sin monto descontado; no se genera asiento.");
            return;
        }

        // idEmpresa: mismo criterio que confirmarRecepcion — se deriva de la cuenta bancaria
        // de una transferencia vigente. Existe al menos una porque el paso 1 ya se confirmó
        // (exigeConfirmacionContabilidad lo garantiza antes de llegar acá) y una transferencia
        // no se puede anular una vez confirmada la carga (exigeNoConfirmada).
        List<TransferenciaCargaPetro> vigentes = transferenciaCargaPetroDaoService.selectVigentesByCarga(idCarga);
        Long idEmpresa = resolverEmpresa(vigentes);

        Long idPlantilla = plantillaService.codigoByAlterno(PlantillasCredito.REPARTO_TRANSITORIA, idEmpresa);
        if (idPlantilla == null) {
            throw new IncomeException("No existe la plantilla contable alterno "
                    + PlantillasCredito.REPARTO_TRANSITORIA
                    + " (COBRO PETROECUADOR/ARCH CORRELACIONADO (1)) para la empresa " + idEmpresa + ".");
        }
        // Transitoria: SIN CAMBIOS, sigue resolviéndose acá mismo (no se movió al servicio
        // compartido — ver el javadoc de ContabilizacionIndividualCreditoService#lineasReparto
        // sobre por qué cada circuito mantiene su propio camino para esta línea).
        DetallePlantilla lineaTransitoria = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla, 1);
        if (lineaTransitoria == null) {
            throw new IncomeException("La plantilla alterno " + PlantillasCredito.REPARTO_TRANSITORIA
                    + " no tiene la línea de la cuenta transitoria (aux1=1).");
        }

        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.add(lineaDesdePlantilla(lineaTransitoria, total,
                "Reparto Petro/ARCH carga " + idCarga + " - cuenta transitoria"));
        // Aportes/préstamos (aux1 2/3): 2026-08-31, extraído a ContabilizacionIndividualCreditoService
        // para que CBCR use la misma implementación — mismo resultado, misma plantilla, mismo
        // idPlantilla ya resuelto arriba. Refactor puro: el asiento sale idéntico.
        lineas.addAll(contabilizacionIndividualCreditoService.lineasReparto(idPlantilla, totalAportes,
                totalPrestamos, "Reparto Petro/ARCH carga " + idCarga));

        // 2026-09-02, pedido del usuario: el asiento va con la fecha de AUTORIZACIÓN de
        // contabilidad, no con la fecha en que corre este proceso — la carga se autoriza un
        // día y se procesa otro (este proceso tarda ~22 minutos, puede cruzar la medianoche),
        // y con LocalDate.now() el asiento caía en el período contable equivocado. Sin
        // fallback a now(): si viniera null es una ruta que nadie previó (en la práctica no
        // puede pasar — exigeConfirmacionContabilidad ya lo valida antes de llegar acá) y
        // tiene que gritar, no esconder el mismo defecto detrás de un if.
        if (carga.getFechaAutorizacionContabilidad() == null) {
            throw new IncomeException("No se puede generar el asiento de reparto de la carga "
                    + idCarga + ": falta la fecha de autorización de contabilidad.");
        }
        LocalDate fechaAsiento = carga.getFechaAutorizacionContabilidad().toLocalDate();
        com.saa.model.cnt.Asiento asiento = asientoContableService.generarAsiento(idEmpresa,
                TipoAsientos.CREDITOS, fechaAsiento,
                "Reparto Petro/ARCH carga " + idCarga, null, lineas,
                Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        AsientoCargaPetro registro = new AsientoCargaPetro();
        registro.setCargaArchivo(carga);
        registro.setSubProceso(Long.valueOf(SubProcesoCobroPetro.REPARTO));
        registro.setAsiento(asiento.getCodigo());
        registro.setNumeroAsiento(asiento.getNumero() != null ? asiento.getNumero().toString() : null);
        registro.setFecha(fechaAsiento);
        registro.setValor(total);
        registro.setCantidad(Long.valueOf(lineas.size()));
        registro.setIdEstado(VIGENTE);
        registro.setUsuarioRegistro("SISTEMA");
        registro.setFechaRegistro(LocalDateTime.now());
        asientoCargaPetroDaoService.save(registro, null);
    }

    // =====================================================================
    // Paso 2b — aplicación (§3.3 del levantamiento; lo llama aplicarPagosArchivoPetro
    // después de contabilizarReparto)
    // =====================================================================

    /** Acumulador de capital por (producto, tipo de cartera, banda) hasta armar la línea. */
    private static class LineaBandaAcumulada {
        private final BandaProductoDetalle banda;
        private final String nombreProducto;
        private double valor;

        LineaBandaAcumulada(BandaProductoDetalle banda, String nombreProducto) {
            this.banda = banda;
            this.nombreProducto = nombreProducto;
        }
    }

    @Override
    public void contabilizarAplicacion(Long idCarga) throws Throwable {
        System.out.println("CobroPetroContableService.contabilizarAplicacion - Carga: " + idCarga);
        CargaArchivo carga = buscarCarga(idCarga);

        if (!configuracionContabilidadService.contabilidadActiva()) {
            System.out.println("CobroPetroContableService.contabilizarAplicacion - contabilidad de"
                    + " CRD INACTIVA: no se genera el asiento de aplicación de la carga " + idCarga);
            return;
        }

        // Pagos: filtran por CRARCDGO (única fuente, sin equivalente previo).
        List<PagoPrestamo> pagos = pagoPrestamoDaoService.selectVigentesByCargaArchivo(idCarga);
        // Aportes: sumValorPorTipoAporteByCarga filtra HOY por APRTIDAS, no por CRARCDGO
        // (transitorio hasta el backfill — ver su javadoc y el de Aporte.idAsoprep).
        List<Object[]> aportesPorTipo = aporteDaoService.sumValorPorTipoAporteByCarga(idCarga);
        boolean sinPagos = pagos == null || pagos.isEmpty();
        boolean sinAportes = aportesPorTipo == null || aportesPorTipo.isEmpty();
        if (sinPagos && sinAportes) {
            // Esperado para cargas anteriores al 2026-08-28 (CRARCDGO sin backfill) o para una
            // carga que solo tuvo valores sin destino. No es un error: no hay nada que aplicar.
            System.out.println("CobroPetroContableService.contabilizarAplicacion - la carga "
                    + idCarga + " no tiene pagos ni aportes con trazabilidad (CRARCDGO); no se"
                    + " genera asiento de aplicación.");
            return;
        }

        // idEmpresa: mismo criterio que confirmarRecepcion/contabilizarReparto.
        List<TransferenciaCargaPetro> vigentes = transferenciaCargaPetroDaoService.selectVigentesByCarga(idCarga);
        Long idEmpresa = resolverEmpresa(vigentes);

        // --- Aportes: cesantía / jubilación --------------------------------------------
        double totalCesantia = 0.0;
        double totalJubilacion = 0.0;
        if (aportesPorTipo != null) {
            for (Object[] fila : aportesPorTipo) {
                Long idTipo = fila[0] != null ? ((Number) fila[0]).longValue() : null;
                double suma = fila[1] != null ? ((Number) fila[1]).doubleValue() : 0.0;
                if (idTipo != null && idTipo == TIPO_APORTE_JUBILACION) {
                    totalJubilacion += suma;
                } else if (idTipo != null && idTipo == TIPO_APORTE_CESANTIA) {
                    totalCesantia += suma;
                } else {
                    System.out.println("  ADVERTENCIA: aporte de tipo " + idTipo + " en la carga "
                            + idCarga + " no es cesantía(11) ni jubilación(9); no se contabiliza"
                            + " en el asiento de aplicación.");
                }
            }
        }
        totalCesantia = redondear(totalCesantia);
        totalJubilacion = redondear(totalJubilacion);

        // --- Préstamos: capital por banda, interés/mora por tipo, seguros ---------------
        Map<String, LineaBandaAcumulada> bandas = new LinkedHashMap<>();
        Map<Long, Double> interesPorTipo = new LinkedHashMap<>();
        Map<Long, Double> moraPorTipo = new LinkedHashMap<>();
        Map<Long, Double> seguroIncendioPorTipo = new LinkedHashMap<>();
        double totalSeguroDesgravamen = 0.0;

        if (pagos != null) {
            for (PagoPrestamo pago : pagos) {
                Prestamo prestamo = pago.getPrestamo();
                Producto producto = prestamo != null ? prestamo.getProducto() : null;
                if (producto == null) {
                    throw new IncomeException("El pago " + pago.getCodigo() + " de la carga "
                            + idCarga + " no tiene producto asignado; no se puede clasificar su"
                            + " capital por banda para el asiento de aplicación.");
                }
                Long idProducto = producto.getCodigo();
                Long idTipoPrestamo = producto.getTipoPrestamo() != null
                        ? producto.getTipoPrestamo().getCodigo() : null;

                double capital = nvl(pago.getCapitalPagado());
                double interes = nvl(pago.getInteresPagado());
                double mora = nvl(pago.getMoraPagada());
                double desgravamen = nvl(pago.getDesgravamen());
                double seguroIncendio = nvl(pago.getValorSeguroIncendio());

                if (capital > 0.0) {
                    DetallePrestamo cuota = pago.getDetallePrestamo();
                    LocalDate fechaVencimiento = (cuota != null && cuota.getFechaVencimiento() != null)
                            ? cuota.getFechaVencimiento().toLocalDate() : null;
                    if (fechaVencimiento == null) {
                        throw new IncomeException("El pago " + pago.getCodigo() + " de la carga "
                                + idCarga + " no tiene fecha de vencimiento de cuota; no se puede"
                                + " clasificar su capital por banda.");
                    }
                    LocalDate fechaPago = pago.getFecha() != null
                            ? pago.getFecha().toLocalDate() : LocalDate.now();
                    long tipoCartera;
                    long dias;
                    if (!fechaPago.isAfter(fechaVencimiento)) {
                        tipoCartera = TipoCarteraBanda.POR_VENCER;
                        dias = Math.max(1, ChronoUnit.DAYS.between(fechaPago, fechaVencimiento));
                    } else {
                        tipoCartera = TipoCarteraBanda.VENCIDO;
                        dias = ChronoUnit.DAYS.between(fechaVencimiento, fechaPago);
                    }
                    ResultadoClasificacionBanda resultado = clasificadorBandaService.clasificar(
                            idProducto, idEmpresa, tipoCartera, dias, fechaPago);
                    BandaProductoDetalle banda = resultado.getBanda();
                    String clave = idProducto + "|" + tipoCartera + "|" + banda.getNumero();
                    LineaBandaAcumulada acumulada = bandas.get(clave);
                    if (acumulada == null) {
                        acumulada = new LineaBandaAcumulada(banda, producto.getNombre());
                        bandas.put(clave, acumulada);
                    }
                    acumulada.valor += capital;
                }

                if ((interes > 0.0 || mora > 0.0 || seguroIncendio > 0.0) && idTipoPrestamo == null) {
                    throw new IncomeException("El pago " + pago.getCodigo() + " de la carga "
                            + idCarga + " tiene interés, mora o seguro de incendio pero su"
                            + " producto no tiene tipo de préstamo asignado; no se puede resolver"
                            + " la cuenta contable.");
                }
                if (interes > 0.0) {
                    interesPorTipo.merge(idTipoPrestamo, interes, Double::sum);
                }
                if (mora > 0.0) {
                    moraPorTipo.merge(idTipoPrestamo, mora, Double::sum);
                }
                if (seguroIncendio > 0.0) {
                    seguroIncendioPorTipo.merge(idTipoPrestamo, seguroIncendio, Double::sum);
                }
                totalSeguroDesgravamen += desgravamen;
            }
        }
        totalSeguroDesgravamen = redondear(totalSeguroDesgravamen);

        Long idPlantilla = plantillaService.codigoByAlterno(PlantillasCredito.APLICACION_PETRO, idEmpresa);
        if (idPlantilla == null) {
            throw new IncomeException("No existe la plantilla contable alterno "
                    + PlantillasCredito.APLICACION_PETRO
                    + " (COBRO PETRO ASIENTO CONTABLE CORRELACIONADO CIERRE CARTERA) para la"
                    + " empresa " + idEmpresa + ".");
        }

        List<DetalleAsiento> lineas = new ArrayList<>();
        double totalHaber = 0.0;

        // H: bandas de capital (redondeadas antes de sumar, para que el D de préstamos
        // cuadre exacto contra la suma de las líneas de Haber que lo componen).
        for (LineaBandaAcumulada acumulada : bandas.values()) {
            double valor = redondear(acumulada.valor);
            if (valor <= 0.0) {
                continue;
            }
            if (acumulada.banda.getIdPlanCuenta() == null) {
                throw new IncomeException("La banda " + acumulada.banda.getNumero() + " de "
                        + acumulada.nombreProducto + " no tiene cuenta contable asignada en"
                        + " CRD.BNDP; no se puede armar el asiento de aplicación.");
            }
            DetalleAsiento linea = new DetalleAsiento();
            linea.setPlanCuenta(planCuentaDaoService.selectById(acumulada.banda.getIdPlanCuenta(),
                    NombreEntidadesContabilidad.PLAN_CUENTA));
            linea.setNumeroCuenta(acumulada.banda.getCuentaContable());
            linea.setNombreCuenta(acumulada.banda.getNombreCuenta());
            linea.setDescripcion("Aplicación Petro/ARCH carga " + idCarga + " - " + acumulada.nombreProducto
                    + " banda " + acumulada.banda.getNumero());
            linea.setValorDebe(0.0);
            linea.setValorHaber(valor);
            lineas.add(linea);
            totalHaber += valor;
        }

        // H: interés ordinario y mora, por tipo de préstamo (dimensión DTPLAXL2)
        for (Map.Entry<Long, Double> entrada : interesPorTipo.entrySet()) {
            double valor = redondear(entrada.getValue());
            if (valor <= 0.0) {
                continue;
            }
            DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliares(
                    idPlantilla, CrdLineaAsiento.INTERES_ORDINARIO_POR_COBRAR, entrada.getKey().intValue());
            if (linea == null) {
                throw new IncomeException("La plantilla alterno " + PlantillasCredito.APLICACION_PETRO
                        + " no tiene la línea de interés ordinario para el tipo de préstamo "
                        + entrada.getKey() + ".");
            }
            lineas.add(lineaDesdePlantilla(linea, valor,
                    "Aplicación Petro/ARCH carga " + idCarga + " - interés ordinario"));
            totalHaber += valor;
        }
        for (Map.Entry<Long, Double> entrada : moraPorTipo.entrySet()) {
            double valor = redondear(entrada.getValue());
            if (valor <= 0.0) {
                continue;
            }
            DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliares(
                    idPlantilla, CrdLineaAsiento.INTERES_MORA_POR_COBRAR, entrada.getKey().intValue());
            if (linea == null) {
                throw new IncomeException("La plantilla alterno " + PlantillasCredito.APLICACION_PETRO
                        + " no tiene la línea de interés de mora para el tipo de préstamo "
                        + entrada.getKey() + ".");
            }
            lineas.add(lineaDesdePlantilla(linea, valor,
                    "Aplicación Petro/ARCH carga " + idCarga + " - interés de mora"));
            totalHaber += valor;
        }

        // H: seguro de incendio (hipotecario/prendario), líneas RAW de la plantilla —
        // fuera del catálogo semántico a propósito, ver AUX1_SEGURO_*.
        for (Map.Entry<Long, Double> entrada : seguroIncendioPorTipo.entrySet()) {
            double valor = redondear(entrada.getValue());
            if (valor <= 0.0) {
                continue;
            }
            int aux1;
            if (entrada.getKey() == TIPO_PRESTAMO_HIPOTECARIO) {
                aux1 = AUX1_SEGURO_HIPOTECARIO;
            } else if (entrada.getKey() == TIPO_PRESTAMO_PRENDARIO) {
                aux1 = AUX1_SEGURO_PRENDARIO;
            } else {
                throw new IncomeException("El pago con seguro de incendio de la carga " + idCarga
                        + " es de tipo de préstamo " + entrada.getKey()
                        + ", que no tiene cuenta de seguro de incendio definida (solo hipotecario"
                        + " y prendario).");
            }
            DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla, aux1);
            if (linea == null) {
                throw new IncomeException("La plantilla alterno " + PlantillasCredito.APLICACION_PETRO
                        + " no tiene la línea de seguro de incendio (aux1=" + aux1 + ").");
            }
            lineas.add(lineaDesdePlantilla(linea, valor,
                    "Aplicación Petro/ARCH carga " + idCarga + " - seguro de incendio"));
            totalHaber += valor;
        }

        // H: seguro de desgravamen
        if (totalSeguroDesgravamen > 0.0) {
            DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(
                    idPlantilla, CrdLineaAsiento.SEGURO_DESGRAVAMEN);
            if (linea == null) {
                throw new IncomeException("La plantilla alterno " + PlantillasCredito.APLICACION_PETRO
                        + " no tiene la línea de seguro de desgravamen.");
            }
            lineas.add(lineaDesdePlantilla(linea, totalSeguroDesgravamen,
                    "Aplicación Petro/ARCH carga " + idCarga + " - seguro de desgravamen"));
            totalHaber += totalSeguroDesgravamen;
        }

        // H: aportes cesantía / jubilación
        if (totalCesantia > 0.0) {
            DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(
                    idPlantilla, CrdLineaAsiento.APORTES_CESANTIA);
            if (linea == null) {
                throw new IncomeException("La plantilla alterno " + PlantillasCredito.APLICACION_PETRO
                        + " no tiene la línea de aportes cesantía.");
            }
            lineas.add(lineaDesdePlantilla(linea, totalCesantia,
                    "Aplicación Petro/ARCH carga " + idCarga + " - aportes cesantía"));
            totalHaber += totalCesantia;
        }
        if (totalJubilacion > 0.0) {
            DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(
                    idPlantilla, CrdLineaAsiento.APORTES_JUBILACION);
            if (linea == null) {
                throw new IncomeException("La plantilla alterno " + PlantillasCredito.APLICACION_PETRO
                        + " no tiene la línea de aportes jubilación.");
            }
            lineas.add(lineaDesdePlantilla(linea, totalJubilacion,
                    "Aplicación Petro/ARCH carga " + idCarga + " - aportes jubilación"));
            totalHaber += totalJubilacion;
        }

        if (lineas.isEmpty()) {
            System.out.println("CobroPetroContableService.contabilizarAplicacion - carga " + idCarga
                    + " sin montos que aplicar; no se genera asiento.");
            return;
        }

        // D: por aplicar — se arman DESPUÉS de sumar el Haber real, así el asiento cuadra
        // por construcción (D = suma de las líneas de H ya redondeadas), sin necesitar un
        // ajuste de centavos.
        double totalPrestamos = redondear(totalHaber - totalCesantia - totalJubilacion);
        double totalAportes = redondear(totalCesantia + totalJubilacion);

        List<DetalleAsiento> lineasFinal = new ArrayList<>();
        if (totalAportes > 0.0) {
            DetallePlantilla lineaAportes = detallePlantillaDaoService.selectByPlantillaYAuxiliar(
                    idPlantilla, CrdLineaAsiento.APORTES_POR_APLICAR);
            if (lineaAportes == null) {
                throw new IncomeException("La plantilla alterno " + PlantillasCredito.APLICACION_PETRO
                        + " no tiene la línea de aportes por aplicar.");
            }
            lineasFinal.add(lineaDesdePlantilla(lineaAportes, totalAportes,
                    "Aplicación Petro/ARCH carga " + idCarga + " - aportes por aplicar"));
        }
        if (totalPrestamos > 0.0) {
            DetallePlantilla lineaPrestamos = detallePlantillaDaoService.selectByPlantillaYAuxiliar(
                    idPlantilla, CrdLineaAsiento.PRESTAMOS_POR_APLICAR);
            if (lineaPrestamos == null) {
                throw new IncomeException("La plantilla alterno " + PlantillasCredito.APLICACION_PETRO
                        + " no tiene la línea de préstamos por aplicar.");
            }
            lineasFinal.add(lineaDesdePlantilla(lineaPrestamos, totalPrestamos,
                    "Aplicación Petro/ARCH carga " + idCarga + " - préstamos por aplicar"));
        }
        lineasFinal.addAll(lineas);

        double totalAsiento = redondear(totalAportes + totalPrestamos);
        // 2026-09-02, mismo pedido del usuario que contabilizarReparto: fecha de AUTORIZACIÓN
        // de contabilidad, no la de hoy — ver el comentario de allá para el porqué completo.
        // Sin fallback a now(): si viniera null, grita en vez de esconder el mismo defecto.
        if (carga.getFechaAutorizacionContabilidad() == null) {
            throw new IncomeException("No se puede generar el asiento de aplicación de la carga "
                    + idCarga + ": falta la fecha de autorización de contabilidad.");
        }
        LocalDate fechaAsiento = carga.getFechaAutorizacionContabilidad().toLocalDate();
        com.saa.model.cnt.Asiento asiento = asientoContableService.generarAsiento(idEmpresa,
                TipoAsientos.CREDITOS, fechaAsiento,
                "Aplicación Petro/ARCH carga " + idCarga, null, lineasFinal,
                Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        AsientoCargaPetro registro = new AsientoCargaPetro();
        registro.setCargaArchivo(carga);
        registro.setSubProceso(Long.valueOf(SubProcesoCobroPetro.APLICACION));
        registro.setAsiento(asiento.getCodigo());
        registro.setNumeroAsiento(asiento.getNumero() != null ? asiento.getNumero().toString() : null);
        registro.setFecha(fechaAsiento);
        registro.setValor(totalAsiento);
        registro.setCantidad(Long.valueOf(lineasFinal.size()));
        registro.setIdEstado(VIGENTE);
        registro.setUsuarioRegistro("SISTEMA");
        registro.setFechaRegistro(LocalDateTime.now());
        asientoCargaPetroDaoService.save(registro, null);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private CargaArchivo buscarCarga(Long idCarga) throws Throwable {
        if (idCarga == null) {
            throw new IncomeException("idCarga es obligatorio");
        }
        CargaArchivo carga = cargaArchivoService.selectById(idCarga);
        if (carga == null) {
            throw new IncomeException("No existe la carga Petro " + idCarga);
        }
        return carga;
    }

    private void exigeNoConfirmada(CargaArchivo carga, String accion) throws Throwable {
        if (carga != null && carga.getFechaAutorizacionContabilidad() != null) {
            throw new IncomeException("La carga " + carga.getCodigo() + " ya está confirmada"
                    + " (paso 1 hecho); no se puede " + accion + ". Reverse la confirmación primero.");
        }
    }

    /**
     * idEmpresa no viaja en ningún DTO del contrato: se deriva de la cuenta bancaria de la
     * primera transferencia vigente (toda CuentaBancaria resuelve un PlanCuenta con empresa).
     */
    private Long resolverEmpresa(List<TransferenciaCargaPetro> vigentes) throws Throwable {
        for (TransferenciaCargaPetro transferencia : vigentes) {
            if (transferencia.getCuentaBancaria() != null
                    && transferencia.getCuentaBancaria().getPlanCuenta() != null
                    && transferencia.getCuentaBancaria().getPlanCuenta().getEmpresa() != null) {
                return transferencia.getCuentaBancaria().getPlanCuenta().getEmpresa().getCodigo();
            }
        }
        throw new IncomeException("No se pudo determinar la empresa contable: ninguna de las"
                + " transferencias tiene una cuenta bancaria con cuenta contable y empresa"
                + " asignadas.");
    }

    /** Construye una línea de asiento a partir de su definición en la plantilla (respeta Debe/Haber). */
    private DetalleAsiento lineaDesdePlantilla(DetallePlantilla plantilla, double valor, String descripcion)
            throws Throwable {
        PlanCuenta cuenta = plantilla.getPlanCuenta();
        if (cuenta == null) {
            throw new IncomeException("La línea de la plantilla no tiene cuenta contable asignada.");
        }
        boolean debe = plantilla.getMovimiento() != null && plantilla.getMovimiento().longValue() == 1L;
        DetalleAsiento linea = new DetalleAsiento();
        linea.setPlanCuenta(cuenta);
        linea.setNumeroCuenta(cuenta.getCuentaContable());
        linea.setNombreCuenta(cuenta.getNombre());
        linea.setDescripcion(descripcion);
        linea.setValorDebe(debe ? valor : 0.0);
        linea.setValorHaber(debe ? 0.0 : valor);
        return linea;
    }

    private LocalDate fechaMasReciente(List<TransferenciaCargaPetro> transferencias) {
        LocalDate mayor = null;
        for (TransferenciaCargaPetro transferencia : transferencias) {
            if (transferencia.getFecha() != null
                    && (mayor == null || transferencia.getFecha().isAfter(mayor))) {
                mayor = transferencia.getFecha();
            }
        }
        return mayor != null ? mayor : LocalDate.now();
    }

    private String periodo(CargaArchivo carga) {
        if (carga.getAnioAfectacion() == null || carga.getMesAfectacion() == null) {
            return null;
        }
        return YearMonth.of(carga.getAnioAfectacion().intValue(), carga.getMesAfectacion().intValue())
                .toString();
    }

    private String textoSubProceso(Long tipo) {
        if (tipo == null) {
            return null;
        }
        if (tipo == SubProcesoCobroPetro.TRANSITORIO) {
            return "TRANSITORIO";
        }
        if (tipo == SubProcesoCobroPetro.REPARTO) {
            return "REPARTO";
        }
        if (tipo == SubProcesoCobroPetro.APLICACION) {
            return "APLICACION";
        }
        return String.valueOf(tipo);
    }

    private TransferenciaCargaPetroDTO aDto(TransferenciaCargaPetro fila) {
        TransferenciaCargaPetroDTO dto = new TransferenciaCargaPetroDTO();
        dto.setIdTransferencia(fila.getCodigo());
        dto.setIdCarga(fila.getCargaArchivo() != null ? fila.getCargaArchivo().getCodigo() : null);
        CuentaBancaria cuenta = fila.getCuentaBancaria();
        dto.setIdCuentaBancaria(cuenta != null ? cuenta.getCodigo() : null);
        dto.setCuentaBancaria(cuenta != null ? cuenta.getNumeroCuenta() : null);
        Banco banco = fila.getBanco();
        dto.setIdBanco(banco != null ? banco.getCodigo() : null);
        dto.setNombreBanco(banco != null ? banco.getNombre() : null);
        BancoExterno bancoExterno = fila.getBancoExterno();
        dto.setIdBancoExterno(bancoExterno != null ? bancoExterno.getCodigo() : null);
        dto.setNombreBancoExterno(bancoExterno != null ? bancoExterno.getNombre() : null);
        dto.setCuentaOrigen(fila.getCuentaOrigen());
        dto.setNumero(fila.getNumero());
        dto.setValor(fila.getValor());
        dto.setFecha(fila.getFecha());
        dto.setObservacion(fila.getObservacion());
        dto.setEstado(fila.getIdEstado());
        dto.setUsuarioRegistro(fila.getUsuarioRegistro());
        dto.setFechaRegistro(fila.getFechaRegistro());
        return dto;
    }

    private String concatena(String actual, String nuevo) {
        if (nuevo == null || nuevo.trim().isEmpty()) {
            return actual;
        }
        return (actual != null && !actual.trim().isEmpty() ? actual + " | " : "") + nuevo;
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}

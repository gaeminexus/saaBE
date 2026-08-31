package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.ejb.crd.dao.AcuerdoCondonacionDaoService;
import com.saa.ejb.crd.dao.CargaArchivoDaoService;
import com.saa.ejb.crd.dao.CobroCreditoDaoService;
import com.saa.ejb.crd.dao.DetalleAportePrecancelacionDaoService;
import com.saa.ejb.crd.dao.DetalleCobroCreditoDaoService;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.EventoPrestamoDaoService;
import com.saa.ejb.crd.dao.PagoAporteDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.dao.TipoAporteDaoService;
import com.saa.ejb.crd.service.AbonoCapitalPrestamoService;
import com.saa.ejb.crd.service.AcuerdoCondonacionService;
import com.saa.ejb.crd.service.AporteService;
import com.saa.ejb.crd.service.CobroCreditoService;
import com.saa.ejb.crd.service.ConfiguracionContabilidadService;
import com.saa.ejb.crd.service.ProcesoPagoPrestamoService;
import com.saa.ejb.crd.service.dto.DesgloseAporte;
import com.saa.ejb.crd.service.dto.DetalleRegistroCobroDTO;
import com.saa.ejb.crd.service.dto.FilaBandejaAprobacion;
import com.saa.ejb.crd.service.dto.ResultadoAbonoCapital;
import com.saa.ejb.crd.service.dto.ResultadoAnulacion;
import com.saa.ejb.crd.service.dto.ResultadoAplicacionAcuerdo;
import com.saa.ejb.crd.service.dto.ResultadoAplicacionPago;
import com.saa.ejb.crd.service.dto.ResultadoPagoMultiple;
import com.saa.ejb.crd.service.dto.ResultadoPrecancelacion;
import com.saa.ejb.crd.service.dto.ResultadoProcesoCobro;
import com.saa.ejb.crd.service.dto.ResultadoRegistroAporte;
import com.saa.ejb.crd.service.dto.ResultadoRegistroCobro;
import com.saa.ejb.crd.service.dto.SimulacionPrecancelacion;
import com.saa.ejb.crd.service.dto.SolicitudAbonoCapital;
import com.saa.ejb.crd.service.dto.SolicitudAnulacion;
import com.saa.ejb.crd.service.dto.SolicitudEdicionCobro;
import com.saa.ejb.crd.service.dto.SolicitudPagoCuota;
import com.saa.ejb.crd.service.dto.SolicitudPagoMultiple;
import com.saa.ejb.crd.service.dto.SolicitudPrecancelacion;
import com.saa.ejb.crd.service.dto.SolicitudRegistroAporte;
import com.saa.ejb.crd.service.dto.SolicitudRegistroCobro;
import com.saa.ejb.tsr.dao.CuentaBancariaDaoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.crd.AcuerdoCondonacion;
import com.saa.model.crd.CargaArchivo;
import com.saa.model.crd.CobroCredito;
import com.saa.model.crd.DetalleAportePrecancelacion;
import com.saa.model.crd.DetalleCobroCredito;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.EventoPrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PagoAporte;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.TipoAporte;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.rubros.CrdEstadoAcuerdoCondonacion;
import com.saa.rubros.CrdEstadoCargaArchivo;
import com.saa.rubros.CrdEstadoCobro;
import com.saa.rubros.CrdTipoOperacionCobro;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.PlantillasCredito;
import com.saa.rubros.TipoAsientos;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;

/**
 * @see CobroCreditoService
 * @author Sistema SAA
 * @since 2026-08-29
 */
@Stateless
public class CobroCreditoServiceImpl implements CobroCreditoService {

    private static final double TOLERANCIA_CUADRE = 0.01;

    /**
     * CRD.TPAP.TPAPCDGO — únicos tipos de aporte con cuenta contable en la plantilla 21
     * (aux1 51/50/52 respectivamente). Confirmado con el usuario 2026-08-29: son los únicos
     * que se registran manualmente en la práctica. Cualquier otro tipo se rechaza en el
     * registro (ver {@link #esTipoAporteContabilizable}).
     */
    private static final long TIPO_APORTE_JUBILACION = 9L;
    private static final long TIPO_APORTE_CESANTIA = 11L;
    private static final long TIPO_APORTE_ADICIONAL = 2L;

    @EJB
    private CobroCreditoDaoService cobroCreditoDaoService;

    @EJB
    private DetalleCobroCreditoDaoService detalleCobroCreditoDaoService;

    @EJB
    private DetalleAportePrecancelacionDaoService detalleAportePrecancelacionDaoService;

    @EJB
    private EntidadDaoService entidadDaoService;

    @EJB
    private CuentaBancariaDaoService cuentaBancariaDaoService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private TipoAporteDaoService tipoAporteDaoService;

    @EJB
    private ConfiguracionContabilidadService configuracionContabilidadService;

    @EJB
    private AsientoContableService asientoContableService;

    @EJB
    private PlantillaService plantillaService;

    @EJB
    private DetallePlantillaDaoService detallePlantillaDaoService;

    @EJB
    private CargaArchivoDaoService cargaArchivoDaoService;

    @EJB
    private EventoPrestamoDaoService eventoPrestamoDaoService;

    @EJB
    private PagoAporteDaoService pagoAporteDaoService;

    @EJB
    private ProcesoPagoPrestamoService procesoPagoPrestamoService;

    @EJB
    private AbonoCapitalPrestamoService abonoCapitalPrestamoService;

    @EJB
    private AporteService aporteService;

    @EJB
    private AcuerdoCondonacionService acuerdoCondonacionService;

    @EJB
    private AcuerdoCondonacionDaoService acuerdoCondonacionDaoService;

    @EJB
    private AsientoService asientoService;

    @Override
    public ResultadoRegistroCobro registrarCobro(SolicitudRegistroCobro solicitud) throws Throwable {
        System.out.println("CobroCreditoService.registrarCobro - entidad: "
                + (solicitud != null ? solicitud.getIdEntidad() : null));

        Entidad entidad = validar(solicitud);

        CobroCredito cobro = new CobroCredito();
        cobro.setEntidad(entidad);
        cobro.setTipoOperacion(solicitud.getTipoOperacion());
        cobro.setEstado(Long.valueOf(CrdEstadoCobro.REGISTRADO));
        cobro.setCuentaBancaria(cuentaBancariaDaoService.selectById(solicitud.getIdCuentaBancaria(),
                NombreEntidadesTesoreria.CUENTA_BANCARIA));
        cobro.setReferencia(solicitud.getReferencia());
        cobro.setRutaRespaldo(solicitud.getRutaRespaldo().trim());
        cobro.setValor(redondear(solicitud.getValor()));
        cobro.setFecha(solicitud.getFecha());
        cobro.setObservacion(solicitud.getObservacion());
        cobro.setUsuarioRegistro(solicitud.getUsuario());
        cobro.setFechaRegistro(LocalDateTime.now());
        cobro = cobroCreditoDaoService.save(cobro, null);

        for (DetalleRegistroCobroDTO lineaSolicitud : solicitud.getDetalles()) {
            DetalleCobroCredito linea = new DetalleCobroCredito();
            linea.setCobroCredito(cobro);
            if (lineaSolicitud.getIdPrestamo() != null) {
                linea.setPrestamo(buscarPrestamo(lineaSolicitud.getIdPrestamo()));
            }
            linea.setValor(redondear(lineaSolicitud.getValor()));
            linea.setModalidad(lineaSolicitud.getModalidad());
            if (lineaSolicitud.getIdTipoAporte() != null) {
                linea.setTipoAporte(buscarTipoAporte(lineaSolicitud.getIdTipoAporte()));
            }
            linea.setPeriodoDevengo(lineaSolicitud.getPeriodoDevengo());
            linea.setObservacion(lineaSolicitud.getObservacion());
            if (lineaSolicitud.getIdAcuerdo() != null) {
                linea.setAcuerdoCondonacion(acuerdoCondonacionDaoService.selectById(lineaSolicitud.getIdAcuerdo(),
                        NombreEntidadesCredito.ACUERDO_CONDONACION));
            }
            linea = detalleCobroCreditoDaoService.save(linea, null);

            // Precancelación mixta (2026-08-30): el desglose de aportes CONSUMIDOS se persiste
            // ya, aunque recién se consuma al procesar — entre el registro y la aprobación de
            // contabilidad no puede perderse de qué cuentas sale el dinero.
            if (lineaSolicitud.getAportes() != null && !lineaSolicitud.getAportes().isEmpty()) {
                for (DesgloseAporte renglon : lineaSolicitud.getAportes()) {
                    DetalleAportePrecancelacion lineaAporte = new DetalleAportePrecancelacion();
                    lineaAporte.setDetalleCobroCredito(linea);
                    lineaAporte.setTipoAporte(buscarTipoAporte(renglon.getIdTipoAporte()));
                    lineaAporte.setValor(redondear(renglon.getValor()));
                    detalleAportePrecancelacionDaoService.save(lineaAporte, null);
                }
            }
        }

        ResultadoRegistroCobro resultado = new ResultadoRegistroCobro();
        resultado.setIdCobro(cobro.getCodigo());
        resultado.setEstado(cobro.getEstado());
        resultado.setValor(cobro.getValor());

        // Flag global de contabilidad de CRD: apagado, el registro IGUAL ocurre (ya se
        // guardó arriba) pero sin asiento. No es un error — mismo criterio que
        // CobroPetroContableServiceImpl/CierreCarteraServiceImpl.
        if (!configuracionContabilidadService.contabilidadActiva()) {
            System.out.println("CobroCreditoService.registrarCobro - contabilidad de CRD INACTIVA:"
                    + " cobro " + cobro.getCodigo() + " registrado sin generar asiento.");
            resultado.setContabilidadActiva(Boolean.FALSE);
            resultado.setMensaje("Cobro registrado. La contabilidad de CRD está desactivada:"
                    + " no se generó asiento.");
            return resultado;
        }

        Asiento asientoTransitorio = generarAsientoTransitorio(cobro);
        cobro.setAsientoTransitorio(asientoTransitorio);
        cobroCreditoDaoService.save(cobro, cobro.getCodigo());

        resultado.setContabilidadActiva(Boolean.TRUE);
        resultado.setIdAsientoTransitorio(asientoTransitorio.getCodigo());
        resultado.setNumeroAsientoTransitorio(asientoTransitorio.getNumero() != null
                ? asientoTransitorio.getNumero().toString() : null);
        resultado.setMensaje("Cobro registrado y asiento transitorio generado.");
        return resultado;
    }

    @Override
    public CobroCredito aprobarCobro(Long idCobro, String usuario) throws Throwable {
        System.out.println("CobroCreditoService.aprobarCobro - cobro: " + idCobro);
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IncomeException("usuario es obligatorio");
        }
        CobroCredito cobro = buscarCobro(idCobro);
        if (cobro.getEstado() == null || cobro.getEstado() != CrdEstadoCobro.REGISTRADO) {
            throw new IncomeException("El cobro " + idCobro + " está en estado "
                    + textoEstado(cobro.getEstado()) + "; solo se puede aprobar un cobro en"
                    + " estado REGISTRADO");
        }
        cobro.setEstado(Long.valueOf(CrdEstadoCobro.APROBADO));
        cobro.setUsuarioAprobacion(usuario);
        cobro.setFechaAprobacion(LocalDateTime.now());
        return cobroCreditoDaoService.save(cobro, cobro.getCodigo());
    }

    @Override
    public CobroCredito rechazarCobro(Long idCobro, String usuario, String motivo) throws Throwable {
        System.out.println("CobroCreditoService.rechazarCobro - cobro: " + idCobro);
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IncomeException("usuario es obligatorio");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IncomeException("El motivo del rechazo es obligatorio");
        }
        // La comprobación del estado ocurre ACÁ, dentro de la misma transacción que el
        // cambio (@Stateless sin @TransactionAttribute -> REQUIRED por default): no hay
        // ventana entre leer el estado y escribirlo donde un PROCESO simultáneo pueda
        // colarse sin que esta transacción lo vea.
        CobroCredito cobro = buscarCobro(idCobro);
        long estado = cobro.getEstado() != null ? cobro.getEstado() : -1L;
        if (estado != CrdEstadoCobro.REGISTRADO && estado != CrdEstadoCobro.APROBADO) {
            throw new IncomeException("El cobro " + idCobro + " está en estado "
                    + textoEstado(cobro.getEstado()) + "; solo se puede rechazar un cobro"
                    + " REGISTRADO o APROBADO (nunca uno ya PROCESADO)");
        }
        cobro.setEstado(Long.valueOf(CrdEstadoCobro.RECHAZADO));
        cobro.setUsuarioRechazo(usuario);
        cobro.setFechaRechazo(LocalDateTime.now());
        cobro.setMotivoRechazo(motivo.trim());
        return cobroCreditoDaoService.save(cobro, cobro.getCodigo());
    }

    @Override
    public CobroCredito editarYReenviarCobro(Long idCobro, String usuario,
            SolicitudEdicionCobro correccion) throws Throwable {
        System.out.println("CobroCreditoService.editarYReenviarCobro - cobro: " + idCobro);
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IncomeException("usuario es obligatorio");
        }
        if (correccion == null) {
            throw new IncomeException("La corrección es obligatoria");
        }
        CobroCredito cobro = buscarCobro(idCobro);
        if (cobro.getEstado() == null || cobro.getEstado() != CrdEstadoCobro.RECHAZADO) {
            throw new IncomeException("El cobro " + idCobro + " está en estado "
                    + textoEstado(cobro.getEstado()) + "; solo se puede corregir y reenviar un"
                    + " cobro RECHAZADO");
        }

        // Reusa EXACTAMENTE las mismas validaciones que un registro nuevo — entidad y tipo de
        // operación son los del cobro existente, no se pueden cambiar acá.
        SolicitudRegistroCobro solicitudValidacion = new SolicitudRegistroCobro();
        solicitudValidacion.setIdEntidad(cobro.getEntidad().getCodigo());
        solicitudValidacion.setTipoOperacion(cobro.getTipoOperacion());
        solicitudValidacion.setIdCuentaBancaria(correccion.getIdCuentaBancaria());
        solicitudValidacion.setReferencia(correccion.getReferencia());
        solicitudValidacion.setRutaRespaldo(correccion.getRutaRespaldo());
        solicitudValidacion.setValor(correccion.getValor());
        solicitudValidacion.setFecha(correccion.getFecha());
        solicitudValidacion.setObservacion(correccion.getObservacion());
        solicitudValidacion.setUsuario(usuario);
        solicitudValidacion.setDetalles(correccion.getDetalles());
        validar(solicitudValidacion);

        // ¿Hace falta rehacer el asiento? Solo si cambia el monto o la cuenta bancaria — el
        // DEBE del asiento transitorio depende de las dos. Referencia/observación/respaldo no
        // afectan ninguna cuenta contable.
        boolean cambioMonto = Math.abs(redondear(correccion.getValor()) - redondear(nvl(cobro.getValor()))) > TOLERANCIA_CUADRE;
        boolean cambioCuenta = cobro.getCuentaBancaria() == null
                || !cobro.getCuentaBancaria().getCodigo().equals(correccion.getIdCuentaBancaria());
        boolean rehacerAsiento = cambioMonto || cambioCuenta;

        if (rehacerAsiento && cobro.getAsientoTransitorio() != null) {
            asientoService.anulaAsiento(cobro.getAsientoTransitorio().getCodigo(), usuario,
                    "Corrección del cobro " + idCobro + ": se " + (cambioMonto && cambioCuenta
                            ? "corrigió el monto y la cuenta bancaria" : cambioMonto
                                    ? "corrigió el monto" : "corrigió la cuenta bancaria")
                            + ". El asiento se rehace con los datos corregidos.");
            cobro.setAsientoTransitorio(null);
        }

        // El detalle se reemplaza entero: las líneas viejas nunca tuvieron
        // EventoPrestamo/PagoAporte porque el cobro nunca se procesó (solo se llega acá desde
        // RECHAZADO), así que no hay nada que preservar.
        for (DetalleCobroCredito lineaVieja : detalleCobroCreditoDaoService.selectByCobro(idCobro)) {
            detalleCobroCreditoDaoService.remove(new DetalleCobroCredito(), lineaVieja.getCodigo());
        }

        cobro.setCuentaBancaria(cuentaBancariaDaoService.selectById(correccion.getIdCuentaBancaria(),
                NombreEntidadesTesoreria.CUENTA_BANCARIA));
        cobro.setReferencia(correccion.getReferencia());
        cobro.setRutaRespaldo(correccion.getRutaRespaldo().trim());
        cobro.setValor(redondear(correccion.getValor()));
        cobro.setFecha(correccion.getFecha());
        cobro.setObservacion(correccion.getObservacion());
        cobro.setEstado(Long.valueOf(CrdEstadoCobro.REGISTRADO));
        cobro.setUsuarioRegistro(usuario);
        cobro.setFechaRegistro(LocalDateTime.now());
        cobro = cobroCreditoDaoService.save(cobro, cobro.getCodigo());

        for (DetalleRegistroCobroDTO lineaSolicitud : correccion.getDetalles()) {
            DetalleCobroCredito linea = new DetalleCobroCredito();
            linea.setCobroCredito(cobro);
            if (lineaSolicitud.getIdPrestamo() != null) {
                linea.setPrestamo(buscarPrestamo(lineaSolicitud.getIdPrestamo()));
            }
            linea.setValor(redondear(lineaSolicitud.getValor()));
            linea.setModalidad(lineaSolicitud.getModalidad());
            if (lineaSolicitud.getIdTipoAporte() != null) {
                linea.setTipoAporte(buscarTipoAporte(lineaSolicitud.getIdTipoAporte()));
            }
            linea.setPeriodoDevengo(lineaSolicitud.getPeriodoDevengo());
            linea.setObservacion(lineaSolicitud.getObservacion());
            detalleCobroCreditoDaoService.save(linea, null);
        }

        if (rehacerAsiento && configuracionContabilidadService.contabilidadActiva()) {
            Asiento nuevoAsiento = generarAsientoTransitorio(cobro);
            cobro.setAsientoTransitorio(nuevoAsiento);
            cobro = cobroCreditoDaoService.save(cobro, cobro.getCodigo());
        }

        return cobro;
    }

    @Override
    public CobroCredito anularCobro(Long idCobro, String usuario, String motivo) throws Throwable {
        System.out.println("CobroCreditoService.anularCobro - cobro: " + idCobro);
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IncomeException("usuario es obligatorio");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IncomeException("El motivo de la anulación es obligatorio");
        }
        CobroCredito cobro = buscarCobro(idCobro);
        long estado = cobro.getEstado() != null ? cobro.getEstado() : -1L;
        // Criterio (2026-08-30, extendido tras revisión del árbitro): el reverso lo maneja
        // ESTE método cuando el tipo de operación no tiene una única herramienta externa de
        // un-solo-evento que le sirva — PAGO_MULTIPLE y COBRO_MIXTO porque generan VARIOS
        // EventoPrestamo (anularOperacion solo toma uno), REGISTRO_APORTE porque NO genera
        // ningún EventoPrestamo — no hay redirección posible, el único reverso de un aporte es
        // el que se construye acá (ver AporteService#reversarAporte). Se delega al reverso por
        // préstamo (anularOperacion) solo cuando la operación es de una sola línea Y esa línea
        // es de préstamo: PAGO_CUOTA, ABONO_CAPITAL, PRECANCELACION — ahí un evento, un
        // préstamo, es exactamente lo correcto y no hay nada que arreglar.
        boolean reversoPorLineas = CrdTipoOperacionCobro.COBRO_MIXTO.equals(cobro.getTipoOperacion())
                || CrdTipoOperacionCobro.PAGO_MULTIPLE.equals(cobro.getTipoOperacion())
                || CrdTipoOperacionCobro.REGISTRO_APORTE.equals(cobro.getTipoOperacion());
        if (estado == CrdEstadoCobro.PROCESADO && !reversoPorLineas) {
            throw new IncomeException("El cobro " + idCobro + " ya fue PROCESADO; para"
                    + " deshacerlo use la anulación de la operación sobre el préstamo/aporte"
                    + " correspondiente (anularOperacion), no la anulación del cobro");
        }
        if (estado == CrdEstadoCobro.ANULADO) {
            throw new IncomeException("El cobro " + idCobro + " ya está ANULADO");
        }

        // Cobro PROCESADO de un tipo con reverso por líneas: "un depósito = un cobro = una
        // aprobación = un reverso" (defecto de producción del 2026-08-30 — un cobro múltiple
        // bypaseó la bandeja y, al anularlo por fuera con un solo idEvento, revirtió un
        // préstamo y dejó el otro y los aportes intactos). Acá se revierten TODAS las líneas
        // del detalle, en la MISMA transacción que este método: si cualquier línea falla, el
        // contenedor revierte todo y ninguna queda a medias.
        if (estado == CrdEstadoCobro.PROCESADO) {
            List<DetalleCobroCredito> detalles = detalleCobroCreditoDaoService.selectByCobro(idCobro);
            String motivoLinea = "Anulación del cobro " + idCobro + ": " + motivo.trim();
            for (DetalleCobroCredito linea : detalles) {
                if (linea.getPagoAporte() != null) {
                    Long idAporte = linea.getPagoAporte().getAporte() != null
                            ? linea.getPagoAporte().getAporte().getCodigo() : null;
                    if (idAporte != null) {
                        try {
                            aporteService.reversarAporte(idAporte, usuario, motivoLinea);
                        } catch (IncomeException e) {
                            throw new IncomeException("No se pudo anular el cobro " + idCobro
                                    + ": el aporte " + idAporte + " (línea " + linea.getCodigo()
                                    + ") lo rechazó: " + e.getMessage());
                        }
                    }
                } else if (linea.getEventoPrestamo() != null) {
                    SolicitudAnulacion solicitudAnulacion = new SolicitudAnulacion();
                    solicitudAnulacion.setIdEvento(linea.getEventoPrestamo().getCodigo());
                    solicitudAnulacion.setUsuario(usuario);
                    solicitudAnulacion.setMotivo(motivoLinea);
                    Long idPrestamoLinea = linea.getPrestamo() != null
                            ? linea.getPrestamo().getCodigo() : null;
                    ResultadoAnulacion resultadoAnulacion;
                    try {
                        resultadoAnulacion = procesoPagoPrestamoService.anularOperacion(solicitudAnulacion);
                    } catch (IncomeException e) {
                        // Todo-o-nada es correcto (si un préstamo no se puede reversar, no se
                        // reversa nada) — pero sin este contexto, un cobro de 3+ líneas deja al
                        // usuario con un error genérico sin saber cuál de los préstamos lo
                        // bloqueó ni por qué (típicamente ERR_EVENTO_POSTERIOR_VIGENTE: hay una
                        // operación más nueva sobre ESE préstamo específico que hay que anular
                        // primero).
                        throw new IncomeException("No se pudo anular el cobro " + idCobro
                                + ": el préstamo " + idPrestamoLinea + " (línea " + linea.getCodigo()
                                + ", evento " + linea.getEventoPrestamo().getCodigo() + ") lo rechazó: "
                                + e.getMessage());
                    }
                    System.out.println("  ↩️ Cobro " + idCobro + " - préstamo "
                            + resultadoAnulacion.getIdPrestamo() + " revertido (evento "
                            + resultadoAnulacion.getIdEvento() + ")");
                }
            }
        }

        // No hubo cobro: el DEBE al banco nunca debió registrarse, a diferencia de un rechazo
        // simple (que sí corresponde a un cobro real, mal registrado). Por eso ACÁ SÍ se
        // reversa el asiento transitorio.
        if (cobro.getAsientoTransitorio() != null) {
            asientoService.anulaAsiento(cobro.getAsientoTransitorio().getCodigo(), usuario,
                    "Anulación del cobro " + idCobro + ": " + motivo.trim());
        }

        cobro.setEstado(Long.valueOf(CrdEstadoCobro.ANULADO));
        cobro.setUsuarioAnulacion(usuario);
        cobro.setFechaAnulacion(LocalDateTime.now());
        cobro.setMotivoAnulacion(motivo.trim());
        cobro = cobroCreditoDaoService.save(cobro, cobro.getCodigo());

        // Cascada a ACCN (§5 del plan): "anulado" en el ciclo de vida del acuerdo significa
        // exactamente esto — se anuló su CBCR antes de procesarlo. No es una acción que el
        // usuario dispare sobre el acuerdo directamente.
        if (CrdTipoOperacionCobro.ACUERDO_CONDONACION.equals(cobro.getTipoOperacion())) {
            AcuerdoCondonacion acuerdo = acuerdoCondonacionDaoService.selectByCobroCredito(idCobro);
            if (acuerdo != null) {
                acuerdoCondonacionService.anularAcuerdoPorCobro(acuerdo.getCodigo(),
                        cobro.getUsuarioAnulacion(), cobro.getFechaAnulacion(), cobro.getMotivoAnulacion());
            }
        }

        return cobro;
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    @Override
    public List<FilaBandejaAprobacion> bandejaAprobacion() throws Throwable {
        System.out.println("CobroCreditoService.bandejaAprobacion");
        List<FilaBandejaAprobacion> filas = new ArrayList<>();

        List<CobroCredito> cobros = cobroCreditoDaoService.selectByEstado(
                Long.valueOf(CrdEstadoCobro.REGISTRADO));
        for (CobroCredito cobro : cobros) {
            FilaBandejaAprobacion fila = new FilaBandejaAprobacion();
            fila.setTipo("COBRO_CREDITO");
            fila.setId(cobro.getCodigo());
            fila.setDescripcion(cobro.getEntidad() != null ? cobro.getEntidad().getRazonSocial() : null);
            fila.setValor(cobro.getValor());
            fila.setUsuarioRegistro(cobro.getUsuarioRegistro());
            fila.setFechaRegistro(cobro.getFechaRegistro());
            filas.add(fila);
        }

        // Cargas Petro pendientes del paso 1 (confirmación de recepción): estado CARGADO,
        // sin fechaAutorizacionContabilidad todavía. Ver CobroPetroContableServiceImpl.
        List<CargaArchivo> cargas = cargaArchivoDaoService.selectByEstado(
                Long.valueOf(CrdEstadoCargaArchivo.CARGADO));
        for (CargaArchivo carga : cargas) {
            FilaBandejaAprobacion fila = new FilaBandejaAprobacion();
            fila.setTipo("CARGA_PETRO");
            fila.setId(carga.getCodigo());
            fila.setDescripcion(carga.getFilial() != null ? carga.getFilial().getNombre() : null);
            fila.setValor(carga.getTotalDescontado());
            fila.setUsuarioRegistro(carga.getUsuarioCarga() != null
                    ? carga.getUsuarioCarga().getNombre() : null);
            fila.setFechaRegistro(carga.getFechaCarga());
            filas.add(fila);
        }

        filas.sort(Comparator.comparing(FilaBandejaAprobacion::getFechaRegistro,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return filas;
    }

    // =====================================================================
    // PROCESO (paso 3): reconstruye la Solicitud y llama al motor existente. El motor de
    // pago NO SE TOCA — ver DDL-COBROS-APROBACION-CONTABILIDAD.sql.
    //
    // ⚠️ PENDIENTE: el asiento definitivo (CBCRASN2) NO se genera todavía en este método.
    // Encontré, al revisar los DTO de resultado de cada operación, que la clasificación
    // contable que ya existe (CobroPetroContableServiceImpl.contabilizarAplicacion) NO se
    // puede reusar tal cual: el abono a capital graba su monto en PagoPrestamo.saldoOtros,
    // NO en capitalPagado (esa clasificación lee capitalPagado, así que se comería el abono
    // entero en silencio), y esa clasificación solo reconoce aportes de tipo jubilación(9) o
    // cesantía(11) — cualquier otro tipo de aporte quedaría fuera del asiento. Reusar esa
    // lógica a ciegas produciría un asiento contable INCORRECTO, así que no lo hice. Falta
    // una decisión del árbitro sobre qué plantilla/líneas usar antes de construirlo.
    //
    // ⚠️ MISMO RIESGO EN PRECANCELACIÓN (verificado 2026-08-30, pedido del usuario): el
    // capital futuro de una precancelación (ProcesoPagoPrestamoServiceImpl.precancelar, línea
    // ~897 en adelante) YA se registra en DetallePrestamo.saldoOtros de la ÚLTIMA CUOTA
    // PAGADA (detallePrestamoDaoService.selectUltimaCuotaPagada — mayor numeroCuota con
    // estado PAGADA(4); si no hay ninguna, la primera cuota futura) Y en un PagoPrestamo
    // propio (pagoCapitalFuturo) con saldoOtros = capitalFuturo, capitalPagado = 0 — mismo
    // patrón exacto que el abono a capital, ya implementado, no hace falta tocarlo. Pero por
    // eso mismo hereda el mismo hueco: cualquier CBCRASN2 que lea capitalPagado se comerá en
    // silencio el capital futuro de toda precancelación, igual que se comería el abono.
    // =====================================================================

    private static final double TOLERANCIA_STALENESS_PRECANCELACION = 0.01;

    @Override
    public ResultadoProcesoCobro procesarCobro(Long idCobro, String usuario) throws Throwable {
        System.out.println("CobroCreditoService.procesarCobro - cobro: " + idCobro);
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IncomeException("usuario es obligatorio");
        }

        // Comprobación de estado DENTRO de la misma transacción que el cambio — mismo
        // razonamiento que aprobarCobro/rechazarCobro.
        CobroCredito cobro = buscarCobro(idCobro);
        if (cobro.getEstado() == null || cobro.getEstado() != CrdEstadoCobro.APROBADO) {
            throw new IncomeException("El cobro " + idCobro + " está en estado "
                    + textoEstado(cobro.getEstado()) + "; solo se puede procesar un cobro en"
                    + " estado APROBADO");
        }

        List<DetalleCobroCredito> detalles = detalleCobroCreditoDaoService.selectByCobro(idCobro);
        if (detalles == null || detalles.isEmpty()) {
            throw new IncomeException("El cobro " + idCobro + " no tiene líneas de detalle");
        }

        String tipoOperacion = cobro.getTipoOperacion();
        LocalDateTime fechaNegocio = cobro.getFecha() != null
                ? cobro.getFecha().atStartOfDay() : LocalDateTime.now();

        if (CrdTipoOperacionCobro.PRECANCELACION.equals(tipoOperacion)) {
            DetalleCobroCredito linea = detalles.get(0);

            // Precancelación mixta (2026-08-30): el desglose de aportes CONSUMIDOS, si lo hay
            // — linea.getValor() es SOLO la parte de depósito, nunca el total.
            List<DetalleAportePrecancelacion> desgloseAportesGuardado =
                    detalleAportePrecancelacionDaoService.selectByDetalleCobro(linea.getCodigo());
            double totalAportesGuardado = 0.0;
            List<DesgloseAporte> aportesParaPrecancelar = new ArrayList<>();
            for (DetalleAportePrecancelacion lineaAporte : desgloseAportesGuardado) {
                totalAportesGuardado += nvl(lineaAporte.getValor());
                DesgloseAporte renglon = new DesgloseAporte();
                renglon.setIdTipoAporte(lineaAporte.getTipoAporte().getCodigo());
                renglon.setValor(lineaAporte.getValor());
                aportesParaPrecancelar.add(renglon);
            }
            totalAportesGuardado = redondear(totalAportesGuardado);

            // Pre-chequeos de SOLO LECTURA, ANTES de llamar al motor: si se llamara a
            // precancelar() directo y este (o consumirAportes dentro de él) lanzara, el
            // contenedor marca la transacción rollback-only apenas se lanza la IncomeException
            // (@ApplicationException(rollback=true)) — atraparla acá NO alcanza para salvar la
            // transacción (mismo defecto que documenta el comentario de
            // ConfiguracionContabilidadServiceImpl.contabilidadActiva). Por eso el rechazo
            // automático se decide ANTES de tocar nada mutable, nunca atrapando el fallo del
            // motor después. Dos cosas pueden haber cambiado desde el registro: el préstamo
            // (staleness de siempre) y el saldo de aportes (puede haberse gastado en el medio,
            // sobre todo si hubo parte de depósito y pasaron días hasta la aprobación).
            SimulacionPrecancelacion simulacion = procesoPagoPrestamoService.simularPrecancelacion(
                    linea.getPrestamo().getCodigo(), cobro.getFecha());
            double valorActual = simulacion.getValorTotalPrecancelacion() != null
                    ? simulacion.getValorTotalPrecancelacion() : 0.0;
            double valorEnviado = redondear(nvl(linea.getValor()) + totalAportesGuardado);
            double diferencia = Math.abs(valorActual - valorEnviado);
            String motivoRechazo = null;
            if (diferencia > TOLERANCIA_STALENESS_PRECANCELACION) {
                motivoRechazo = "Rechazado automáticamente por el sistema: el depósito ($"
                        + linea.getValor() + ") más los aportes ($" + totalAportesGuardado
                        + ") suman $" + valorEnviado + ", que ya no coincide con el valor de"
                        + " precancelación recalculado al procesar ($" + valorActual
                        + "); el préstamo cambió entre el registro y el proceso. Verifique y"
                        + " vuelva a registrar.";
            } else if (!aportesParaPrecancelar.isEmpty()) {
                try {
                    procesoPagoPrestamoService.validarDesgloseAportes(aportesParaPrecancelar,
                            cobro.getEntidad());
                } catch (IncomeException e) {
                    motivoRechazo = "Rechazado automáticamente por el sistema: el saldo de aportes"
                            + " cambió entre el registro y el proceso — " + e.getMessage();
                }
            }
            if (motivoRechazo != null) {
                cobro.setEstado(Long.valueOf(CrdEstadoCobro.RECHAZADO));
                cobro.setUsuarioRechazo("SISTEMA");
                cobro.setFechaRechazo(LocalDateTime.now());
                cobro.setMotivoRechazo(motivoRechazo);
                cobroCreditoDaoService.save(cobro, cobro.getCodigo());

                ResultadoProcesoCobro resultado = new ResultadoProcesoCobro();
                resultado.setIdCobro(idCobro);
                resultado.setEstado(cobro.getEstado());
                resultado.setProcesado(false);
                resultado.setMensaje(motivoRechazo);
                return resultado;
            }

            SolicitudPrecancelacion solicitud = new SolicitudPrecancelacion();
            solicitud.setIdPrestamo(linea.getPrestamo().getCodigo());
            solicitud.setValorEfectivo(linea.getValor());
            solicitud.setAportes(aportesParaPrecancelar.isEmpty() ? null : aportesParaPrecancelar);
            solicitud.setUsuario(usuario);
            solicitud.setObservacion(observacionLinea(cobro, linea));
            solicitud.setFecha(cobro.getFecha());
            solicitud.setRutaDocumentoRespaldo(cobro.getRutaRespaldo());
            // precancelar() no se toca: ya sabía sumar valorEfectivo + aportes y consumirlos
            // con consumirAportes desde antes de este cambio.
            ResultadoPrecancelacion resultado = procesoPagoPrestamoService.precancelar(solicitud);
            enlazarEvento(linea, resultado.getIdEvento());

        } else if (CrdTipoOperacionCobro.PAGO_CUOTA.equals(tipoOperacion)) {
            DetalleCobroCredito linea = detalles.get(0);
            ResultadoAplicacionPago resultado = procesoPagoPrestamoService.pagarCuota(
                    aSolicitudPagoCuota(cobro, linea, usuario));
            enlazarEvento(linea, resultado.getIdEvento());

        } else if (CrdTipoOperacionCobro.PAGO_MULTIPLE.equals(tipoOperacion)) {
            SolicitudPagoMultiple solicitud = new SolicitudPagoMultiple();
            List<SolicitudPagoCuota> pagos = new ArrayList<>();
            for (DetalleCobroCredito linea : detalles) {
                pagos.add(aSolicitudPagoCuota(cobro, linea, usuario));
            }
            solicitud.setPagos(pagos);
            ResultadoPagoMultiple resultado = procesoPagoPrestamoService.pagarMultiplesCuotas(solicitud);
            // Mismo orden que 'detalles': ResultadoPagoMultiple.resultados respeta el orden de
            // 'pagos', que se armó en el mismo orden que 'detalles'.
            List<ResultadoAplicacionPago> resultados = resultado.getResultados();
            for (int i = 0; i < detalles.size(); i++) {
                enlazarEvento(detalles.get(i), resultados.get(i).getIdEvento());
            }

        } else if (CrdTipoOperacionCobro.ABONO_CAPITAL.equals(tipoOperacion)) {
            DetalleCobroCredito linea = detalles.get(0);
            SolicitudAbonoCapital solicitud = new SolicitudAbonoCapital();
            solicitud.setIdPrestamo(linea.getPrestamo().getCodigo());
            solicitud.setValor(linea.getValor());
            solicitud.setModalidad(linea.getModalidad() != null ? linea.getModalidad().intValue() : null);
            solicitud.setUsuario(usuario);
            solicitud.setObservacion(observacionLinea(cobro, linea));
            solicitud.setFecha(cobro.getFecha());
            solicitud.setRutaDocumentoRespaldo(cobro.getRutaRespaldo());
            ResultadoAbonoCapital resultado = abonoCapitalPrestamoService.aplicar(solicitud);
            enlazarEvento(linea, resultado.getIdEvento());

        } else if (CrdTipoOperacionCobro.REGISTRO_APORTE.equals(tipoOperacion)) {
            DetalleCobroCredito linea = detalles.get(0);
            SolicitudRegistroAporte solicitud = new SolicitudRegistroAporte();
            solicitud.setIdEntidad(cobro.getEntidad().getCodigo());
            solicitud.setIdTipoAporte(linea.getTipoAporte().getCodigo());
            solicitud.setValor(linea.getValor());
            solicitud.setUsuario(usuario);
            solicitud.setObservacion(observacionLinea(cobro, linea));
            solicitud.setFechaTransaccion(cobro.getFecha());
            solicitud.setRutaDocumentoRespaldo(cobro.getRutaRespaldo());
            solicitud.setPeriodoDevengo(linea.getPeriodoDevengo());
            ResultadoRegistroAporte resultado = aporteService.registrarAporte(solicitud);
            if (resultado.getIdPagoAporte() != null) {
                PagoAporte pagoAporte = pagoAporteDaoService.selectById(resultado.getIdPagoAporte(),
                        NombreEntidadesCredito.PAGO_APORTE);
                linea.setPagoAporte(pagoAporte);
                detalleCobroCreditoDaoService.save(linea, linea.getCodigo());
            }

        } else if (CrdTipoOperacionCobro.ACUERDO_CONDONACION.equals(tipoOperacion)) {
            DetalleCobroCredito linea = detalles.get(0);
            Long idAcuerdo = linea.getAcuerdoCondonacion().getCodigo();

            // Staleness (§3 del plan, reubicado al proceso el 2026-08-30): mismo motivo que
            // precancelación — pre-chequeo de SOLO LECTURA antes de llamar al motor, para no
            // dejar la transacción marcada rollback-only si el acuerdo ya no coincide.
            String motivoStaleness = acuerdoCondonacionService.verificarVigencia(idAcuerdo);
            if (motivoStaleness != null) {
                cobro.setEstado(Long.valueOf(CrdEstadoCobro.RECHAZADO));
                cobro.setUsuarioRechazo("SISTEMA");
                cobro.setFechaRechazo(LocalDateTime.now());
                cobro.setMotivoRechazo(motivoStaleness);
                cobroCreditoDaoService.save(cobro, cobro.getCodigo());

                ResultadoProcesoCobro resultado = new ResultadoProcesoCobro();
                resultado.setIdCobro(idCobro);
                resultado.setEstado(cobro.getEstado());
                resultado.setProcesado(false);
                resultado.setMensaje(motivoStaleness);
                return resultado;
            }

            ResultadoAplicacionAcuerdo resultado = acuerdoCondonacionService.aplicarAcuerdo(idAcuerdo, usuario);
            enlazarEvento(linea, resultado.getIdEvento());

        } else if (CrdTipoOperacionCobro.COBRO_MIXTO.equals(tipoOperacion)) {
            // Un depósito = un cobro = una aprobación = un reverso (defecto de producción del
            // 2026-08-30): las líneas de préstamo y de aporte se aplican TODAS acá, en la
            // misma transacción que este método — si cualquiera falla, el contenedor revierte
            // TODO (ninguna línea queda aplicada a medias). Cada línea de préstamo se paga
            // individualmente (como PAGO_CUOTA) en vez de agruparlas en pagarMultiplesCuotas:
            // así cada línea guarda su propio EventoPrestamo sin depender del orden de un
            // resultado agregado.
            for (DetalleCobroCredito linea : detalles) {
                if (linea.getTipoAporte() != null) {
                    SolicitudRegistroAporte solicitud = new SolicitudRegistroAporte();
                    solicitud.setIdEntidad(cobro.getEntidad().getCodigo());
                    solicitud.setIdTipoAporte(linea.getTipoAporte().getCodigo());
                    solicitud.setValor(linea.getValor());
                    solicitud.setUsuario(usuario);
                    solicitud.setObservacion(observacionLinea(cobro, linea));
                    solicitud.setFechaTransaccion(cobro.getFecha());
                    solicitud.setRutaDocumentoRespaldo(cobro.getRutaRespaldo());
                    solicitud.setPeriodoDevengo(linea.getPeriodoDevengo());
                    ResultadoRegistroAporte resultadoAporte = aporteService.registrarAporte(solicitud);
                    if (resultadoAporte.getIdPagoAporte() != null) {
                        PagoAporte pagoAporte = pagoAporteDaoService.selectById(
                                resultadoAporte.getIdPagoAporte(), NombreEntidadesCredito.PAGO_APORTE);
                        linea.setPagoAporte(pagoAporte);
                        detalleCobroCreditoDaoService.save(linea, linea.getCodigo());
                    }
                } else {
                    ResultadoAplicacionPago resultadoPago = procesoPagoPrestamoService.pagarCuota(
                            aSolicitudPagoCuota(cobro, linea, usuario));
                    enlazarEvento(linea, resultadoPago.getIdEvento());
                }
            }

        } else {
            throw new IncomeException("Tipo de operación desconocido: " + tipoOperacion);
        }

        cobro.setEstado(Long.valueOf(CrdEstadoCobro.PROCESADO));
        cobro.setUsuarioProceso(usuario);
        cobro.setFechaProceso(LocalDateTime.now());
        cobroCreditoDaoService.save(cobro, cobro.getCodigo());

        ResultadoProcesoCobro resultado = new ResultadoProcesoCobro();
        resultado.setIdCobro(idCobro);
        resultado.setEstado(cobro.getEstado());
        resultado.setProcesado(true);
        resultado.setMensaje("Cobro procesado.");
        return resultado;
    }

    private SolicitudPagoCuota aSolicitudPagoCuota(CobroCredito cobro, DetalleCobroCredito linea,
            String usuario) {
        SolicitudPagoCuota solicitud = new SolicitudPagoCuota();
        solicitud.setIdPrestamo(linea.getPrestamo().getCodigo());
        solicitud.setValor(linea.getValor());
        solicitud.setUsuario(usuario);
        solicitud.setObservacion(observacionLinea(cobro, linea));
        solicitud.setFechaPago(cobro.getFecha());
        solicitud.setRutaDocumentoRespaldo(cobro.getRutaRespaldo());
        return solicitud;
    }

    private String observacionLinea(CobroCredito cobro, DetalleCobroCredito linea) {
        if (linea.getObservacion() != null && !linea.getObservacion().trim().isEmpty()) {
            return linea.getObservacion();
        }
        return cobro.getObservacion();
    }

    private void enlazarEvento(DetalleCobroCredito linea, Long idEvento) throws Throwable {
        if (idEvento == null) {
            return;
        }
        EventoPrestamo evento = eventoPrestamoDaoService.selectById(idEvento,
                NombreEntidadesCredito.EVENTO_PRESTAMO);
        linea.setEventoPrestamo(evento);
        detalleCobroCreditoDaoService.save(linea, linea.getCodigo());
    }

    private CobroCredito buscarCobro(Long idCobro) throws Throwable {
        if (idCobro == null) {
            throw new IncomeException("idCobro es obligatorio");
        }
        try {
            return cobroCreditoDaoService.selectById(idCobro, NombreEntidadesCredito.COBRO_CREDITO);
        } catch (NoResultException e) {
            throw new IncomeException("No existe el cobro " + idCobro);
        }
    }

    private String textoEstado(Long estado) {
        if (estado == null) {
            return "desconocido";
        }
        if (estado == CrdEstadoCobro.REGISTRADO) {
            return "REGISTRADO";
        }
        if (estado == CrdEstadoCobro.APROBADO) {
            return "APROBADO";
        }
        if (estado == CrdEstadoCobro.PROCESADO) {
            return "PROCESADO";
        }
        if (estado == CrdEstadoCobro.RECHAZADO) {
            return "RECHAZADO";
        }
        return String.valueOf(estado);
    }

    // =====================================================================
    // Validaciones
    // =====================================================================

    private Entidad validar(SolicitudRegistroCobro solicitud) throws Throwable {
        if (solicitud == null) {
            throw new IncomeException("La solicitud de registro de cobro es obligatoria");
        }
        if (solicitud.getIdEntidad() == null) {
            throw new IncomeException("idEntidad es obligatorio");
        }
        Entidad entidad;
        try {
            entidad = entidadDaoService.selectById(solicitud.getIdEntidad(), NombreEntidadesCredito.ENTIDAD);
        } catch (NoResultException e) {
            throw new IncomeException("No existe la entidad " + solicitud.getIdEntidad());
        }

        if (!esTipoOperacionValido(solicitud.getTipoOperacion())) {
            throw new IncomeException("tipoOperacion inválido: " + solicitud.getTipoOperacion()
                    + "; debe ser uno de PAGO_CUOTA, PAGO_MULTIPLE, ABONO_CAPITAL, PRECANCELACION,"
                    + " REGISTRO_APORTE, ACUERDO_CONDONACION, COBRO_MIXTO");
        }

        if (solicitud.getIdCuentaBancaria() == null) {
            throw new IncomeException("idCuentaBancaria es obligatorio: todo cobro es depósito o"
                    + " transferencia a una cuenta bancaria de la institución");
        }
        try {
            cuentaBancariaDaoService.selectById(solicitud.getIdCuentaBancaria(),
                    NombreEntidadesTesoreria.CUENTA_BANCARIA);
        } catch (NoResultException e) {
            throw new IncomeException("No existe la cuenta bancaria " + solicitud.getIdCuentaBancaria());
        }

        if (solicitud.getRutaRespaldo() == null || solicitud.getRutaRespaldo().trim().isEmpty()) {
            throw new IncomeException("El respaldo digitalizado es obligatorio: suba el comprobante"
                    + " antes de registrar el cobro");
        }

        if (solicitud.getValor() == null || solicitud.getValor() <= 0.0) {
            throw new IncomeException("El valor del cobro debe ser mayor a cero");
        }

        if (solicitud.getFecha() == null) {
            throw new IncomeException("La fecha del cobro es obligatoria");
        }

        if (solicitud.getDetalles() == null || solicitud.getDetalles().isEmpty()) {
            throw new IncomeException("El cobro debe tener al menos una línea de detalle");
        }
        boolean esMultiple = CrdTipoOperacionCobro.PAGO_MULTIPLE.equals(solicitud.getTipoOperacion());
        boolean esMixto = CrdTipoOperacionCobro.COBRO_MIXTO.equals(solicitud.getTipoOperacion());
        if (!esMultiple && !esMixto && solicitud.getDetalles().size() != 1) {
            throw new IncomeException("El tipo de operación " + solicitud.getTipoOperacion()
                    + " admite exactamente una línea de detalle; use PAGO_MULTIPLE para varios"
                    + " préstamos o COBRO_MIXTO para préstamos y aportes en un mismo cobro");
        }

        boolean esAporte = CrdTipoOperacionCobro.REGISTRO_APORTE.equals(solicitud.getTipoOperacion());
        boolean esAbono = CrdTipoOperacionCobro.ABONO_CAPITAL.equals(solicitud.getTipoOperacion());
        boolean esAcuerdo = CrdTipoOperacionCobro.ACUERDO_CONDONACION.equals(solicitud.getTipoOperacion());
        boolean esPrecancelacion = CrdTipoOperacionCobro.PRECANCELACION.equals(solicitud.getTipoOperacion());
        double sumaDetalles = 0.0;
        for (DetalleRegistroCobroDTO linea : solicitud.getDetalles()) {
            if (linea.getValor() == null || linea.getValor() <= 0.0) {
                throw new IncomeException("El valor de cada línea del detalle debe ser mayor a cero");
            }
            sumaDetalles += linea.getValor();

            // COBRO_MIXTO: cada línea decide su propia clase (préstamo o aporte) — a
            // diferencia de los demás tipos, donde la clase la fija el tipoOperacion entero.
            if (esMixto) {
                boolean traeAporte = linea.getIdTipoAporte() != null;
                boolean traePrestamo = linea.getIdPrestamo() != null;
                if (traeAporte == traePrestamo) {
                    throw new IncomeException("En COBRO_MIXTO cada línea debe traer exactamente"
                            + " uno de idPrestamo o idTipoAporte, nunca ambos ni ninguno");
                }
                if (linea.getIdAcuerdo() != null) {
                    throw new IncomeException("idAcuerdo solo aplica a ACUERDO_CONDONACION;"
                            + " no aplica dentro de COBRO_MIXTO");
                }
                if (linea.getModalidad() != null) {
                    throw new IncomeException("modalidad solo aplica a ABONO_CAPITAL;"
                            + " COBRO_MIXTO no admite abono a capital");
                }
            }
            boolean lineaEsAporte = esAporte || (esMixto && linea.getIdTipoAporte() != null);

            if (lineaEsAporte) {
                if (linea.getIdPrestamo() != null) {
                    throw new IncomeException("REGISTRO_APORTE no lleva préstamo: el aporte es de"
                            + " la entidad, no de un préstamo");
                }
                if (linea.getIdTipoAporte() == null) {
                    throw new IncomeException("idTipoAporte es obligatorio en REGISTRO_APORTE");
                }
                TipoAporte tipoAporte;
                try {
                    tipoAporte = tipoAporteDaoService.selectById(linea.getIdTipoAporte(),
                            NombreEntidadesCredito.TIPO_APORTE);
                } catch (NoResultException e) {
                    throw new IncomeException("No existe el tipo de aporte " + linea.getIdTipoAporte());
                }
                // Solo 9 (jubilación), 11 (cesantía) y 2 (aporte adicional) tienen cuenta
                // contable asignada en la plantilla 21 (aux1 51/50/52) — verificado contra
                // CNT.PLNS/DTPL, no todos los tipos vigentes de CRD.TPAP tienen una. Rechazar
                // acá, no al procesar: mejor que falle en el registro que dejarlo pasar y
                // reventar después de aprobado, con el dinero ya adentro.
                if (!esTipoAporteContabilizable(linea.getIdTipoAporte())) {
                    throw new IncomeException("El tipo de aporte " + linea.getIdTipoAporte() + " ("
                            + tipoAporte.getNombre() + ") no tiene cuenta contable asignada en la"
                            + " plantilla 21; no se puede contabilizar este cobro");
                }
                if (linea.getPeriodoDevengo() == null) {
                    throw new IncomeException("El período de devengo es obligatorio en REGISTRO_APORTE");
                }
                if (linea.getIdAcuerdo() != null) {
                    throw new IncomeException("idAcuerdo solo aplica a ACUERDO_CONDONACION");
                }
            } else {
                if (linea.getIdPrestamo() == null) {
                    throw new IncomeException("idPrestamo es obligatorio para " + solicitud.getTipoOperacion());
                }
                Prestamo prestamo = buscarPrestamo(linea.getIdPrestamo());
                if (prestamo.getEntidad() == null
                        || !prestamo.getEntidad().getCodigo().equals(solicitud.getIdEntidad())) {
                    throw new IncomeException("El préstamo " + linea.getIdPrestamo()
                            + " no pertenece a la entidad " + solicitud.getIdEntidad());
                }
                if (linea.getIdTipoAporte() != null || linea.getPeriodoDevengo() != null) {
                    throw new IncomeException("idTipoAporte/periodoDevengo solo aplican a"
                            + " REGISTRO_APORTE");
                }

                if (esAcuerdo) {
                    if (linea.getIdAcuerdo() == null) {
                        throw new IncomeException("idAcuerdo es obligatorio para ACUERDO_CONDONACION");
                    }
                    AcuerdoCondonacion acuerdo;
                    try {
                        acuerdo = acuerdoCondonacionDaoService.selectById(linea.getIdAcuerdo(),
                                NombreEntidadesCredito.ACUERDO_CONDONACION);
                    } catch (NoResultException e) {
                        throw new IncomeException("No existe el acuerdo " + linea.getIdAcuerdo());
                    }
                    if (acuerdo.getEstado() == null || acuerdo.getEstado() != CrdEstadoAcuerdoCondonacion.VIGENTE) {
                        throw new IncomeException("El acuerdo " + linea.getIdAcuerdo()
                                + " no está VIGENTE; no se puede registrar su cobro");
                    }
                    if (acuerdo.getPrestamo() == null
                            || !acuerdo.getPrestamo().getCodigo().equals(linea.getIdPrestamo())) {
                        throw new IncomeException("El acuerdo " + linea.getIdAcuerdo()
                                + " no corresponde al préstamo " + linea.getIdPrestamo());
                    }
                    if (acuerdo.getEntidad() == null
                            || !acuerdo.getEntidad().getCodigo().equals(solicitud.getIdEntidad())) {
                        throw new IncomeException("El acuerdo " + linea.getIdAcuerdo()
                                + " no corresponde a la entidad " + solicitud.getIdEntidad());
                    }
                    // El monto nace fijo del acuerdo (§5 del plan): el cobro no puede pedir un
                    // valor distinto del que el acuerdo ya decidió. ⚠️ Contra valorPagarDeposito,
                    // NO valorPagar (2026-08-30, cruce con aportes): el CBCR solo cubre la parte
                    // de depósito — la de aportes ni siquiera genera este cobro.
                    double diferencia = Math.abs(nvl(acuerdo.getValorPagarDeposito()) - linea.getValor());
                    if (diferencia > TOLERANCIA_CUADRE) {
                        throw new IncomeException("El valor de la línea ($" + linea.getValor()
                                + ") no coincide con el valor a cubrir con depósito del acuerdo "
                                + linea.getIdAcuerdo() + " ($" + acuerdo.getValorPagarDeposito() + ")");
                    }
                } else if (linea.getIdAcuerdo() != null) {
                    throw new IncomeException("idAcuerdo solo aplica a ACUERDO_CONDONACION");
                }
            }

            if (esAbono) {
                if (linea.getModalidad() == null || (linea.getModalidad() != 1L && linea.getModalidad() != 2L)) {
                    throw new IncomeException("modalidad es obligatoria en ABONO_CAPITAL y debe ser"
                            + " 1 (reduce plazo) o 2 (reduce cuota)");
                }
            } else if (linea.getModalidad() != null) {
                throw new IncomeException("modalidad solo aplica a ABONO_CAPITAL");
            }

            // Consumo de aportes (precancelación mixta, 2026-08-30): SOLO en PRECANCELACION —
            // en cualquier otro tipo se rechaza, sobre todo COBRO_MIXTO, donde una línea con
            // aportes significa exactamente lo OPUESTO (el socio ENTREGA plata, su saldo SUBE;
            // acá se CONSUME saldo del socio, su saldo BAJA). Reusar el mismo campo para los
            // dos sentidos del dinero es la clase de ambigüedad que corrompe saldos sin dar
            // ningún error.
            if (esPrecancelacion) {
                if (linea.getAportes() != null && !linea.getAportes().isEmpty()) {
                    // Solo valida que el desglose sea sano y financiable AHORA (tipo vigente +
                    // saldo suficiente) — no se compara contra el total de la precancelación:
                    // ese total nunca se guarda, se recalcula fresco con simularPrecancelacion
                    // recién al procesar (mismo criterio que ya rige linea.getValor() hoy, que
                    // tampoco se valida contra el total al registrar).
                    procesoPagoPrestamoService.validarDesgloseAportes(linea.getAportes(), entidad);
                }
            } else if (linea.getAportes() != null && !linea.getAportes().isEmpty()) {
                throw new IncomeException("aportes solo aplica a PRECANCELACION; en "
                        + solicitud.getTipoOperacion() + " no se admite");
            }
        }

        double diferencia = redondear(solicitud.getValor() - sumaDetalles);
        if (Math.abs(diferencia) > TOLERANCIA_CUADRE) {
            throw new IncomeException("El valor total del cobro ($" + solicitud.getValor()
                    + ") no cuadra con la suma de sus líneas de detalle ($" + redondear(sumaDetalles)
                    + "); revise los montos");
        }

        return entidad;
    }

    private boolean esTipoAporteContabilizable(Long idTipoAporte) {
        return idTipoAporte != null && (idTipoAporte == TIPO_APORTE_JUBILACION
                || idTipoAporte == TIPO_APORTE_CESANTIA || idTipoAporte == TIPO_APORTE_ADICIONAL);
    }

    private boolean esTipoOperacionValido(String tipoOperacion) {
        return CrdTipoOperacionCobro.PAGO_CUOTA.equals(tipoOperacion)
                || CrdTipoOperacionCobro.PAGO_MULTIPLE.equals(tipoOperacion)
                || CrdTipoOperacionCobro.ABONO_CAPITAL.equals(tipoOperacion)
                || CrdTipoOperacionCobro.PRECANCELACION.equals(tipoOperacion)
                || CrdTipoOperacionCobro.REGISTRO_APORTE.equals(tipoOperacion)
                || CrdTipoOperacionCobro.ACUERDO_CONDONACION.equals(tipoOperacion)
                || CrdTipoOperacionCobro.COBRO_MIXTO.equals(tipoOperacion);
    }

    private Prestamo buscarPrestamo(Long idPrestamo) throws Throwable {
        try {
            return prestamoDaoService.selectById(idPrestamo, NombreEntidadesCredito.PRESTAMO);
        } catch (NoResultException e) {
            throw new IncomeException("No existe el préstamo " + idPrestamo);
        }
    }

    private TipoAporte buscarTipoAporte(Long idTipoAporte) throws Throwable {
        try {
            return tipoAporteDaoService.selectById(idTipoAporte, NombreEntidadesCredito.TIPO_APORTE);
        } catch (NoResultException e) {
            throw new IncomeException("No existe el tipo de aporte " + idTipoAporte);
        }
    }

    // =====================================================================
    // Asiento transitorio (paso 1): D cuenta contable de la cuenta bancaria elegida
    // -> H 2.3.01.15.01, plantilla alterno 19 (la misma de Petro, reutilizada a propósito
    // — ver DDL-COBROS-APROBACION-CONTABILIDAD.sql).
    // =====================================================================

    private Asiento generarAsientoTransitorio(CobroCredito cobro) throws Throwable {
        CuentaBancaria cuentaBancaria = cobro.getCuentaBancaria();
        if (cuentaBancaria.getPlanCuenta() == null) {
            throw new IncomeException("La cuenta bancaria " + cuentaBancaria.getCodigo()
                    + " no tiene cuenta contable asignada; no se puede generar el asiento"
                    + " transitorio del cobro " + cobro.getCodigo());
        }
        PlanCuenta cuentaBanco = cuentaBancaria.getPlanCuenta();
        if (cuentaBanco.getEmpresa() == null) {
            throw new IncomeException("La cuenta contable de la cuenta bancaria "
                    + cuentaBancaria.getCodigo() + " no tiene empresa asignada; no se puede"
                    + " determinar la empresa contable del cobro " + cobro.getCodigo());
        }
        Long idEmpresa = cuentaBanco.getEmpresa().getCodigo();

        Long idPlantilla = plantillaService.codigoByAlterno(PlantillasCredito.COBRO_TRANSITORIO_PETRO,
                idEmpresa);
        if (idPlantilla == null) {
            throw new IncomeException("No existe la plantilla contable alterno "
                    + PlantillasCredito.COBRO_TRANSITORIO_PETRO + " para la empresa " + idEmpresa + ".");
        }
        DetallePlantilla lineaTransitoria = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla, 1);
        if (lineaTransitoria == null || lineaTransitoria.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno " + PlantillasCredito.COBRO_TRANSITORIO_PETRO
                    + " no tiene la línea de la cuenta transitoria (2.3.01.15.01).");
        }

        List<DetalleAsiento> lineas = new ArrayList<>();

        DetalleAsiento debe = new DetalleAsiento();
        debe.setPlanCuenta(cuentaBanco);
        debe.setNumeroCuenta(cuentaBanco.getCuentaContable());
        debe.setNombreCuenta(cuentaBanco.getNombre());
        debe.setDescripcion("Cobro crédito " + cobro.getCodigo() + " - " + cobro.getTipoOperacion());
        debe.setValorDebe(cobro.getValor());
        debe.setValorHaber(0.0);
        lineas.add(debe);

        DetalleAsiento haber = new DetalleAsiento();
        haber.setPlanCuenta(lineaTransitoria.getPlanCuenta());
        haber.setNumeroCuenta(lineaTransitoria.getPlanCuenta().getCuentaContable());
        haber.setNombreCuenta(lineaTransitoria.getPlanCuenta().getNombre());
        haber.setDescripcion("Cobro crédito " + cobro.getCodigo() + " - cuenta transitoria");
        haber.setValorDebe(0.0);
        haber.setValorHaber(cobro.getValor());
        lineas.add(haber);

        return asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS, cobro.getFecha(),
                "Cobro crédito " + cobro.getCodigo() + " - registro pendiente de aprobación"
                        + (cobro.getObservacion() != null ? ": " + cobro.getObservacion() : ""),
                cobro.getUsuarioRegistro(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}

package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
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
import com.saa.ejb.crd.dao.CorridaCierreCarteraDaoService;
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
import com.saa.ejb.crd.service.dto.BandaProductoDetalle;
import com.saa.ejb.crd.service.dto.DesgloseAporte;
import com.saa.ejb.crd.service.dto.ResultadoClasificacionBanda;
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
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.crd.AcuerdoCondonacion;
import com.saa.model.crd.CargaArchivo;
import com.saa.model.crd.CobroCredito;
import com.saa.model.crd.CorridaCierreCartera;
import com.saa.model.crd.DetalleAporteAcuerdoCondonacion;
import com.saa.model.crd.DetalleAportePrecancelacion;
import com.saa.model.crd.DetalleCobroCredito;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.EventoPrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PagoAporte;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.Producto;
import com.saa.model.crd.TipoAporte;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.rubros.CrdEstadoAcuerdoCondonacion;
import com.saa.rubros.CrdEstadoCargaArchivo;
import com.saa.rubros.CrdEstadoCobro;
import com.saa.rubros.CrdLineaAsiento;
import com.saa.rubros.CrdTipoOperacionCobro;
import com.saa.rubros.DsbnOrigen;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.PlantillasCredito;
import com.saa.rubros.TipoAsientos;
import com.saa.rubros.TipoCarteraBanda;

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
    private CorridaCierreCarteraDaoService corridaCierreCarteraDaoService;

    @EJB
    private DetalleCobroCreditoDaoService detalleCobroCreditoDaoService;

    @EJB
    private DetalleAportePrecancelacionDaoService detalleAportePrecancelacionDaoService;

    @EJB
    private com.saa.ejb.crd.dao.DetalleAporteAcuerdoCondonacionDaoService detalleAporteAcuerdoCondonacionDaoService;

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

    @EJB
    private com.saa.ejb.crd.dao.PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private com.saa.ejb.cnt.dao.PlanCuentaDaoService planCuentaDaoService;

    @EJB
    private com.saa.ejb.crd.service.ClasificadorBandaService clasificadorBandaService;

    @EJB
    private com.saa.ejb.crd.service.ContabilizacionIndividualCreditoService contabilizacionIndividualCreditoService;

    @EJB
    private com.saa.ejb.crd.service.DistribucionBandaService distribucionBandaService;

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
        // referenciaOriginal = la que YA tiene guardada este cobro (recortada) — si la corrección
        // no la cambia, validar() se salta el chequeo de unicidad. Necesario porque puede existir
        // un duplicado histórico (p.ej. otro cobro ANULADO o pre-índice) contra el que este mismo
        // cobro ya choca sin que el usuario esté tocando la referencia.
        String referenciaOriginal = cobro.getReferencia() != null ? cobro.getReferencia().trim() : null;
        validar(solicitudValidacion, idCobro, referenciaOriginal);
        // validar() normaliza (trim) la referencia mutando solicitudValidacion — pero el WRITE
        // de más abajo (:372) lee de `correccion`, un objeto DISTINTO. Sin este propagado, la
        // referencia se guardaría sin recortar y divergiría del valor ya validado como único.
        correccion.setReferencia(solicitudValidacion.getReferencia());

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
        // el que se construye acá (ver AporteService#reversarAporte). Y desde 2026-08-31,
        // REGISTRO_APORTE puede tener VARIAS líneas (varios tipos de aporte en el mismo
        // cobro) — el bucle de abajo ya reversaba por línea antes de ese cambio, así que
        // sigue cubriendo N aportes sin tocarlo: cada línea con pagoAporte se reversa
        // independiente. Se delega al reverso por préstamo (anularOperacion) solo cuando la
        // operación es de una sola línea Y esa línea es de préstamo: PAGO_CUOTA,
        // ABONO_CAPITAL, PRECANCELACION — ahí un evento, un préstamo, es exactamente lo
        // correcto y no hay nada que arreglar.
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
                    // idEmpresa lo pone CBCR con la empresa derivada de la cuenta bancaria del
                    // cobro, NUNCA la que vino del cliente (contrato
                    // API-EMPRESA-CONTABLE-CRD.md §2).
                    solicitudAnulacion.setIdEmpresa(derivarEmpresaCobro(cobro));
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
        // reversan los tres asientos (2026-08-31: antes eran dos, ahora con el de reparto).
        //
        // Orden 3, 2, 1 — inverso al de generación (1 al registrar; 2 y 3 al procesar, 2
        // antes que 3). Contablemente da igual: cada Asiento es autocontenido (D=H propio) y
        // asientoService.anulaAsiento no depende de que otro ya esté anulado. Es solo
        // legibilidad — quien lea el log de una anulación entiende el orden inverso sin
        // pensarlo — no hay ninguna dependencia funcional entre los tres que exija este
        // orden hoy.
        if (cobro.getAsientoDefinitivo() != null) {
            asientoService.anulaAsiento(cobro.getAsientoDefinitivo().getCodigo(), usuario,
                    "Anulación del cobro " + idCobro + ": " + motivo.trim());
        }
        if (cobro.getAsientoReparto() != null) {
            asientoService.anulaAsiento(cobro.getAsientoReparto().getCodigo(), usuario,
                    "Anulación del cobro " + idCobro + ": " + motivo.trim());
        }
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
    // El asiento definitivo (CBCRASN2, ver generarAsientoDefinitivo más abajo) SÍ se genera
    // acá desde 2026-08-30 — docs/logica-negocio/crd/ESPECIFICACION-CBCRASN2.md. Las dos
    // trampas que bloquearon la primera versión de este comentario (abono a capital y capital
    // futuro de precancelación grabando en saldoOtros, no en capitalPagado; aportes de tipo
    // distinto a 9/11 quedando fuera) están resueltas: haberDesdeEvento lee saldoOtros si es
    // mayor a 0 y si no capitalPagado (una sola regla para las dos trampas), y las cuentas de
    // aporte se resuelven contra la plantilla 21 por aux1 (50/51/52), no por un if/else de
    // tipos — cualquier tipo sin cuenta falla fuerte en vez de quedar afuera en silencio.
    //
    // ⚠️ PENDIENTE, explícitamente diferido (§4 de la especificación, decisión de no meterlo
    // en esta entrega): el RE-BANDEO de un abono a capital — la redistribución del saldo
    // restante entre bandas tras el abono (nueva tabla de amortización) — es un asiento
    // APARTE del cobro, con diferencias netas por banda (mismo criterio que el cambio de
    // bandas mensual de CierreCarteraServiceImpl, no bruto contra bruto). CBCRASN2 hoy
    // contabiliza el COBRO del abono (baja las bandas por el monto abonado) pero NO genera
    // ese segundo asiento de re-bandeo. Sin él, las cuentas de banda de ese préstamo quedan
    // con la distribución VIEJA hasta el próximo cierre mensual de cartera, que sí recalcula
    // todas las bandas de toda la cartera — así que el descuadre es transitorio, no permanente,
    // pero existe entre un abono y el siguiente cierre.
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

        // Idempotente por (origen, idOrigen) — PLAN-AUDITORIA-BANDAS.md §9, mismo criterio que
        // Petro (§5.1 punto 2). UNA sola vez acá arriba: este método puede llamar a
        // registrarDistribucionBandaEvento() más de una vez más abajo (PAGO_MULTIPLE, COBRO_MIXTO
        // con varias líneas de préstamo), y cada una de esas llamadas NO borra por su cuenta —
        // ver el javadoc de DistribucionBandaService#registrarDistribucionPorPagos.
        distribucionBandaService.eliminarDistribucion(DsbnOrigen.COBRO_INDIVIDUAL, idCobro);

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
            // idEmpresa lo pone CBCR con la empresa derivada de la cuenta bancaria del cobro,
            // NUNCA la que vino del cliente (contrato API-EMPRESA-CONTABLE-CRD.md §2).
            solicitud.setIdEmpresa(derivarEmpresaCobro(cobro));
            // idCobroCredito: discriminador de origen (2026-08-31, circuito de cobros con
            // aportes) — nunca lo manda el cliente. Con esto seteado, contabilizarPrecancelacion
            // sabe que ESTE método ya genera el asiento (CBCRASN2) y no debe generar el suyo.
            solicitud.setIdCobroCredito(idCobro);
            // precancelar() no se toca: ya sabía sumar valorEfectivo + aportes y consumirlos
            // con consumirAportes desde antes de este cambio.
            ResultadoPrecancelacion resultado = procesoPagoPrestamoService.precancelar(solicitud);
            enlazarEvento(linea, resultado.getIdEvento());
            registrarDistribucionBandaEvento(idCobro, derivarEmpresaCobro(cobro), resultado.getIdEvento(), usuario);

        } else if (CrdTipoOperacionCobro.PAGO_CUOTA.equals(tipoOperacion)) {
            DetalleCobroCredito linea = detalles.get(0);
            ResultadoAplicacionPago resultado = procesoPagoPrestamoService.pagarCuota(
                    aSolicitudPagoCuota(cobro, linea, usuario));
            enlazarEvento(linea, resultado.getIdEvento());
            registrarDistribucionBandaEvento(idCobro, derivarEmpresaCobro(cobro), resultado.getIdEvento(), usuario);

        } else if (CrdTipoOperacionCobro.PAGO_MULTIPLE.equals(tipoOperacion)) {
            SolicitudPagoMultiple solicitud = new SolicitudPagoMultiple();
            List<SolicitudPagoCuota> pagos = new ArrayList<>();
            for (DetalleCobroCredito linea : detalles) {
                pagos.add(aSolicitudPagoCuota(cobro, linea, usuario));
            }
            solicitud.setPagos(pagos);
            // idEmpresa lo pone CBCR con la empresa derivada de la cuenta bancaria del cobro,
            // NUNCA la que vino del cliente (contrato API-EMPRESA-CONTABLE-CRD.md §2).
            solicitud.setIdEmpresa(derivarEmpresaCobro(cobro));
            ResultadoPagoMultiple resultado = procesoPagoPrestamoService.pagarMultiplesCuotas(solicitud);
            // Mismo orden que 'detalles': ResultadoPagoMultiple.resultados respeta el orden de
            // 'pagos', que se armó en el mismo orden que 'detalles'.
            List<ResultadoAplicacionPago> resultados = resultado.getResultados();
            for (int i = 0; i < detalles.size(); i++) {
                enlazarEvento(detalles.get(i), resultados.get(i).getIdEvento());
                registrarDistribucionBandaEvento(idCobro, derivarEmpresaCobro(cobro),
                    resultados.get(i).getIdEvento(), usuario);
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
            // idEmpresa lo pone CBCR con la empresa derivada de la cuenta bancaria del cobro,
            // NUNCA la que vino del cliente (contrato API-EMPRESA-CONTABLE-CRD.md §2).
            solicitud.setIdEmpresa(derivarEmpresaCobro(cobro));
            ResultadoAbonoCapital resultado = abonoCapitalPrestamoService.aplicar(solicitud);
            enlazarEvento(linea, resultado.getIdEvento());
            registrarDistribucionBandaEvento(idCobro, derivarEmpresaCobro(cobro), resultado.getIdEvento(), usuario);

        } else if (CrdTipoOperacionCobro.REGISTRO_APORTE.equals(tipoOperacion)) {
            // Varias líneas desde 2026-08-31 (un partícipe puede aportar cesantía Y
            // jubilación en el mismo cobro) — UNA llamada a registrarAporte POR LÍNEA. Antes
            // solo tomaba detalles.get(0): con una sola línea no se notaba, pero con dos o
            // más, la segunda se ignoraba en silencio y esa plata quedaba sin aplicar sin
            // ningún error — el mismo defecto que se cerró en otros lugares hoy.
            for (DetalleCobroCredito linea : detalles) {
                SolicitudRegistroAporte solicitud = new SolicitudRegistroAporte();
                solicitud.setIdEntidad(cobro.getEntidad().getCodigo());
                solicitud.setIdTipoAporte(linea.getTipoAporte().getCodigo());
                solicitud.setValor(linea.getValor());
                solicitud.setUsuario(usuario);
                solicitud.setObservacion(observacionLinea(cobro, linea));
                solicitud.setFechaTransaccion(cobro.getFecha());
                solicitud.setRutaDocumentoRespaldo(cobro.getRutaRespaldo());
                solicitud.setPeriodoDevengo(linea.getPeriodoDevengo());
                // idEmpresa lo pone CBCR con la empresa derivada de la cuenta bancaria del
                // cobro, NUNCA la que vino del cliente (contrato API-EMPRESA-CONTABLE-CRD.md §2).
                solicitud.setIdEmpresa(derivarEmpresaCobro(cobro));
                ResultadoRegistroAporte resultado = aporteService.registrarAporte(solicitud);
                if (resultado.getIdPagoAporte() != null) {
                    PagoAporte pagoAporte = pagoAporteDaoService.selectById(resultado.getIdPagoAporte(),
                            NombreEntidadesCredito.PAGO_APORTE);
                    linea.setPagoAporte(pagoAporte);
                    detalleCobroCreditoDaoService.save(linea, linea.getCodigo());
                }
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
            registrarDistribucionBandaEvento(idCobro, derivarEmpresaCobro(cobro), resultado.getIdEvento(), usuario);

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
                    // idEmpresa lo pone CBCR con la empresa derivada de la cuenta bancaria del
                    // cobro, NUNCA la que vino del cliente (contrato
                    // API-EMPRESA-CONTABLE-CRD.md §2).
                    solicitud.setIdEmpresa(derivarEmpresaCobro(cobro));
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
                    registrarDistribucionBandaEvento(idCobro, derivarEmpresaCobro(cobro),
                        resultadoPago.getIdEvento(), usuario);
                }
            }

        } else {
            throw new IncomeException("Tipo de operación desconocido: " + tipoOperacion);
        }

        cobro.setUsuarioProceso(usuario);
        cobro.setFechaProceso(LocalDateTime.now());

        // Tres asientos por cobro (2026-08-31, decisión del usuario): 1=transitorio (ya
        // generado al registrar), 2=REPARTO (CBCRASRP, nuevo), 3=definitivo (CBCRASN2, sin
        // cambios). Los dos de acá abajo, detrás del mismo gate y con el mismo criterio que
        // registrarCobro con el transitorio: apagado, se procesa igual y se informa, no es un
        // error. Se generan DESPUÉS de todo lo de arriba porque necesitan los EventoPrestamo/
        // PagoAporte que ese bloque acaba de crear.
        if (configuracionContabilidadService.contabilidadActiva()) {
            List<DetalleCobroCredito> detallesActualizados = detalleCobroCreditoDaoService.selectByCobro(idCobro);
            // Capital futuro (2026-09-05): calculado UNA sola vez acá y compartido entre los
            // asientos 2 y 3, para que las dos restas contra la cuenta de apertura salgan del
            // mismo número. Ver el javadoc de calcularCapitalFuturoDelCobro.
            double capitalFuturo = calcularCapitalFuturoDelCobro(cobro, detallesActualizados);
            Asiento asientoReparto = generarAsientoReparto(cobro, detallesActualizados, capitalFuturo);
            cobro.setAsientoReparto(asientoReparto);
            Asiento asientoDefinitivo = generarAsientoDefinitivo(cobro, detallesActualizados);
            cobro.setAsientoDefinitivo(asientoDefinitivo);
        } else {
            System.out.println("CobroCreditoService.procesarCobro - contabilidad de CRD INACTIVA:"
                    + " cobro " + idCobro + " procesado sin generar asientos de reparto/definitivo.");
        }

        cobro.setEstado(Long.valueOf(CrdEstadoCobro.PROCESADO));
        cobroCreditoDaoService.save(cobro, cobro.getCodigo());

        ResultadoProcesoCobro resultado = new ResultadoProcesoCobro();
        resultado.setIdCobro(idCobro);
        resultado.setEstado(cobro.getEstado());
        resultado.setProcesado(true);
        resultado.setMensaje("Cobro procesado.");
        return resultado;
    }

    private SolicitudPagoCuota aSolicitudPagoCuota(CobroCredito cobro, DetalleCobroCredito linea,
            String usuario) throws Throwable {
        SolicitudPagoCuota solicitud = new SolicitudPagoCuota();
        solicitud.setIdPrestamo(linea.getPrestamo().getCodigo());
        solicitud.setValor(linea.getValor());
        solicitud.setUsuario(usuario);
        solicitud.setObservacion(observacionLinea(cobro, linea));
        solicitud.setFechaPago(cobro.getFecha());
        solicitud.setRutaDocumentoRespaldo(cobro.getRutaRespaldo());
        // idEmpresa lo pone CBCR con la empresa derivada de la cuenta bancaria del cobro,
        // NUNCA la que vino del cliente (contrato API-EMPRESA-CONTABLE-CRD.md §2).
        solicitud.setIdEmpresa(derivarEmpresaCobro(cobro));
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

    /** Registro nuevo: nada que excluir, y no hay una referencia previa que pueda ser "la misma". */
    private Entidad validar(SolicitudRegistroCobro solicitud) throws Throwable {
        return validar(solicitud, null, null);
    }

    /**
     * @param idCobroExcluido    Solo lo manda {@code editarYReenviarCobro}: el cobro que se
     *                           está corrigiendo no debe chocar contra su propia referencia.
     * @param referenciaOriginal La referencia YA GUARDADA del cobro que se corrige (recortada
     *                           por el llamador), o {@code null} en un registro nuevo. Si la
     *                           nueva referencia es IGUAL a esta, se salta la unicidad —
     *                           2026-09-01: hay referencias históricas duplicadas de antes de
     *                           esta regla (verificado por el usuario: '31072026' se repite en
     *                           tres cobros), y volver a guardar la MISMA referencia de un
     *                           cobro no crea un conflicto nuevo, aunque otro cobro histórico
     *                           también la tenga. Sin este salto, esos cobros quedarían sin
     *                           poder corregirse nunca más.
     */
    private Entidad validar(SolicitudRegistroCobro solicitud, Long idCobroExcluido, String referenciaOriginal)
            throws Throwable {
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

        // Referencia única (2026-09-01, pedido del usuario): obligatoria — el '9'/'09' existen
        // justamente para los casos sin referencia real, así que dejarla en blanco no tiene
        // sentido (decisión confirmada con el árbitro). Se normaliza acá, UNA vez, mutando la
        // propia `solicitud` — registrarCobro y editarYReenviarCobro leen `solicitud
        // .getReferencia()` después de este método, así que el valor recortado les llega solo.
        if (solicitud.getReferencia() == null || solicitud.getReferencia().trim().isEmpty()) {
            throw new IncomeException("La referencia es obligatoria: use '9' o '09' si el cobro no"
                    + " tiene un número de referencia real");
        }
        String referenciaTrim = solicitud.getReferencia().trim();
        solicitud.setReferencia(referenciaTrim);

        // Valores exentos de la unicidad — MISMA regla que el índice CRD.UX_CBCR_REFERENCIA
        // (CASE WHEN TRIM(CBCRRFRN) IN ('9','09') OR NVL(CBCRESTD,0) = 5 THEN NULL...):
        // '9'/'09' exactos tras el trim, nada más — ningún otro valor que empiece o contenga
        // un 9 exime.
        boolean exento = "9".equals(referenciaTrim) || "09".equals(referenciaTrim);
        // Sin cambio real: si el cobro ya tenía ESTA MISMA referencia, no es un conflicto
        // nuevo — hay históricos duplicados de antes de esta regla (verificado en producción:
        // '31072026' se repite en tres cobros) y hay que poder seguir corrigiendo esos cobros
        // sin la referencia como excusa, mientras no la cambien a otra cosa.
        boolean sinCambio = referenciaOriginal != null && referenciaOriginal.equals(referenciaTrim);
        if (!exento && !sinCambio) {
            // Excluye ANULADO (5) — CrdEstadoCobro.ANULADO, nunca el literal (ver el javadoc
            // de selectByReferencia): un cobro anulado libera su referencia, misma regla que
            // el índice de la base.
            List<CobroCredito> enConflicto = cobroCreditoDaoService.selectByReferencia(referenciaTrim,
                    idCobroExcluido);
            if (enConflicto != null && !enConflicto.isEmpty()) {
                CobroCredito existente = enConflicto.get(0);
                throw new IncomeException("La referencia '" + referenciaTrim + "' ya está en uso por"
                        + " el cobro " + existente.getCodigo() + " (estado " + textoEstado(existente.getEstado())
                        + "); cada referencia debe ser única. Use '9' o '09' si el cobro no tiene un"
                        + " número de referencia real.");
            }
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
        boolean esAporte = CrdTipoOperacionCobro.REGISTRO_APORTE.equals(solicitud.getTipoOperacion());
        // REGISTRO_APORTE admite varias líneas desde 2026-08-31 (decisión del usuario, ejemplo
        // textual: un partícipe que aporta 30 de cesantía y 40 de jubilación en el mismo
        // cobro son DOS líneas del mismo REGISTRO_APORTE, no un COBRO_MIXTO). El resto de las
        // reglas de "una sola línea" no cambia.
        if (!esMultiple && !esMixto && !esAporte && solicitud.getDetalles().size() != 1) {
            throw new IncomeException("El tipo de operación " + solicitud.getTipoOperacion()
                    + " admite exactamente una línea de detalle; use PAGO_MULTIPLE para varios"
                    + " préstamos, COBRO_MIXTO para préstamos y aportes en un mismo cobro, o"
                    + " REGISTRO_APORTE con varias líneas para varios tipos de aporte");
        }

        boolean esAbono = CrdTipoOperacionCobro.ABONO_CAPITAL.equals(solicitud.getTipoOperacion());
        boolean esAcuerdo = CrdTipoOperacionCobro.ACUERDO_CONDONACION.equals(solicitud.getTipoOperacion());
        boolean esPrecancelacion = CrdTipoOperacionCobro.PRECANCELACION.equals(solicitud.getTipoOperacion());
        // REGISTRO_APORTE (o las líneas de aporte de un COBRO_MIXTO) con varias líneas: cada
        // tipo de aporte una sola vez — dos líneas del mismo tipo son un error de la pantalla
        // (producirían dos aportes del mismo tipo en el mismo cobro, y el asiento 3 saldría con
        // dos líneas de pasivo contra la misma cuenta), no un caso legítimo de "aportar el
        // doble". En COBRO_MIXTO el Set solo se llena con las líneas de aporte (lineaEsAporte
        // más abajo); las líneas de préstamo no participan, y dos préstamos distintos son
        // válidos (2026-08-31).
        java.util.Set<Long> tiposAporteVistos = new java.util.HashSet<>();
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
                // Mensajes con el tipo de operación REAL (2026-08-31): esta rama también la
                // pisa una línea de aporte de COBRO_MIXTO, y un mensaje que dice
                // "REGISTRO_APORTE" ahí confunde al operador aunque el rechazo sea correcto.
                if (linea.getIdPrestamo() != null) {
                    throw new IncomeException(solicitud.getTipoOperacion() + " no lleva préstamo"
                            + " en una línea de aporte: el aporte es de la entidad, no de un"
                            + " préstamo");
                }
                if (linea.getIdTipoAporte() == null) {
                    throw new IncomeException("idTipoAporte es obligatorio en una línea de aporte de "
                            + solicitud.getTipoOperacion());
                }
                // Sin tipos repetidos DENTRO de un REGISTRO_APORTE de varias líneas, ni entre
                // las líneas de aporte de un COBRO_MIXTO (2026-08-31) — dos líneas del mismo
                // tipo son un error de la pantalla; lineaEsAporte ya filtra las líneas de
                // préstamo de COBRO_MIXTO, así que el Set nunca las ve.
                if (!tiposAporteVistos.add(linea.getIdTipoAporte())) {
                    throw new IncomeException("El tipo de aporte " + linea.getIdTipoAporte()
                            + " está repetido en más de una línea del cobro; cada tipo de aporte"
                            + " va en una sola línea");
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
    // Empresa contable del cobro — derivada UNA SOLA VEZ de la cuenta bancaria elegida, nunca
    // de la que mande el cliente en la solicitud (contrato API-EMPRESA-CONTABLE-CRD.md §2).
    // Único punto de derivación: lo usan ASN1 (generarAsientoTransitorio), ASN2
    // (generarAsientoDefinitivo), aSolicitudPagoCuota y las llamadas internas a
    // precancelar/abonarCapital/registrarAporte/anularOperacion dentro de procesarCobro y
    // anularCobro. Antes estaba repetida entre ASN1 y ASN2 sin chequear que cobro tuviera
    // cuenta bancaria asignada (NPE si no la tenía); acá se chequea una sola vez.
    // =====================================================================

    /**
     * Distribución en bandas de UN evento de préstamo — PLAN-AUDITORIA-BANDAS.md §9. Se llama
     * justo después de CADA aplicación de pago dentro de {@link #procesarCobro} (precancelación,
     * pago de cuota, pago múltiple, abono a capital, cobro mixto), nunca detrás del guardarraíl
     * de {@code contabilidadActiva} — la banda es un dato de cartera, no contable.
     *
     * <b>No comparte la clasificación con el asiento</b> (DEUDA anotada en el plan, decisión del
     * árbitro 2026-09-02): {@code ContabilizacionIndividualCreditoService#haberDesdePagos} va a
     * volver a clasificar los mismos pagos cuando arme el asiento definitivo. Se acepta a
     * propósito porque son operaciones individuales de un puñado de cuotas —la regla de "no
     * clasificar dos veces" nació del LOTE de Petro (miles de filas)— y porque compartir
     * requeriría cambiar la firma de {@code haberDesdePagos}, que tiene otros 3 llamadores, en
     * medio de una carga en producción. Si el cobro individual pasa a procesarse en lote, esta
     * duplicación deja de ser barata.
     *
     * Reconsulta los pagos por evento ({@code pagoPrestamoDaoService.selectByEvento}) en vez de
     * reconstruirlos desde {@code ResultadoAplicacionPago.cuotasAfectadas}: es la MISMA consulta
     * que ya usa {@code haberDesdeEvento} para lo mismo, así que la lista que ve esta escritura
     * es exactamente la que verá el asiento.
     *
     * @param idEvento null-safe: los flujos sin EventoPrestamo (registro de aporte) no llaman
     *                 a este método
     *
     * <p>⚠️ Confirmado por el árbitro (2026-09-02): incluye {@code ACUERDO_CONDONACION}, que
     * también corre dentro de {@code procesarCobro} y pasa por {@code haberDesdePagos}. PERO una
     * condonación NO es dinero recibido — es deuda que se perdona. Mueve saldos fuera de las
     * bandas igual que un pago (por eso contabilidad la quiere ver acá), pero nadie la pagó. Hoy
     * no rompe nada porque el cuadre de {@code COBRO_INDIVIDUAL} es {@code null} (sin fuente
     * independiente de "recibido"). El día que se conecte una fuente de recibido para este
     * origen, la condonación tiene que quedar EXCLUIDA de ese contraste — si no, va a parecer
     * plata que apareció de la nada.</p>
     */
    private void registrarDistribucionBandaEvento(Long idCobro, Long idEmpresa, Long idEvento, String usuario)
            throws Throwable {
        if (idEvento == null) {
            return;
        }
        List<PagoPrestamo> pagos = pagoPrestamoDaoService.selectByEvento(idEvento);
        distribucionBandaService.registrarDistribucionPorPagos(
            DsbnOrigen.COBRO_INDIVIDUAL, idCobro, idEmpresa, pagos, usuario);
    }

    private Long derivarEmpresaCobro(CobroCredito cobro) throws Throwable {
        CuentaBancaria cuentaBancaria = cobro.getCuentaBancaria();
        if (cuentaBancaria == null) {
            throw new IncomeException("El cobro " + cobro.getCodigo() + " no tiene cuenta"
                    + " bancaria asignada; no se puede determinar la empresa contable de la"
                    + " operación.");
        }
        if (cuentaBancaria.getPlanCuenta() == null || cuentaBancaria.getPlanCuenta().getEmpresa() == null) {
            throw new IncomeException("La cuenta bancaria " + cuentaBancaria.getCodigo()
                    + " no tiene cuenta contable/empresa asignada; no se puede determinar la"
                    + " empresa contable del cobro " + cobro.getCodigo() + ".");
        }
        return cuentaBancaria.getPlanCuenta().getEmpresa().getCodigo();
    }

    // =====================================================================
    // Asiento transitorio (paso 1): D cuenta contable de la cuenta bancaria elegida
    // -> H 2.3.01.15.01, plantilla alterno 19 (la misma de Petro, reutilizada a propósito
    // — ver DDL-COBROS-APROBACION-CONTABILIDAD.sql).
    // =====================================================================

    private Asiento generarAsientoTransitorio(CobroCredito cobro) throws Throwable {
        Long idEmpresa = derivarEmpresaCobro(cobro);
        PlanCuenta cuentaBanco = cobro.getCuentaBancaria().getPlanCuenta();
        DetallePlantilla lineaTransitoria = resolverLineaTransitoria(idEmpresa);

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
                observacionEnriquecida(cobro, null, "Cobro crédito " + cobro.getCodigo()
                        + " - registro pendiente de aprobación"
                        + (cobro.getObservacion() != null ? ": " + cobro.getObservacion() : "")),
                cobro.getUsuarioRegistro(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));
    }

    /**
     * Resuelve la línea de la cuenta TRANSITORIA (2.3.01.15.01) — plantilla alterno
     * {@link PlantillasCredito#COBRO_TRANSITORIO_PETRO} (19), auxiliar1 = 1.
     *
     * ⚠️ EXTRAÍDO A PROPÓSITO (2026-08-30, especificación CBCRASN2 §1; ampliado 2026-08-31
     * con el asiento de reparto): lo usan LOS TRES asientos de un cobro —
     * {@code generarAsientoTransitorio} (ASN1, al registrar), {@code generarAsientoReparto}
     * (CBCRASRP, al procesar) y {@code generarAsientoDefinitivo} (ASN2, al procesar) — el
     * MISMO método, nunca una resolución independiente por asiento. Si la cuenta se
     * resolviera por caminos distintos y difiriera aunque sea una vez, la transitoria
     * quedaría abierta por ese cobro para siempre, sin que nada lo detecte (el asiento igual
     * cuadraría). Que no puedan divergir importa más que que estén bien hoy. Nunca resolverla
     * con la plantilla de reparto (alterno 20, aux1=1) — ver el javadoc de
     * {@code ContabilizacionIndividualCreditoService#lineasReparto}.
     */
    private DetallePlantilla resolverLineaTransitoria(Long idEmpresa) throws Throwable {
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
        return lineaTransitoria;
    }

    // =====================================================================
    // Asiento de REPARTO (paso 2, CBCRASRP, 2026-08-31) — tres asientos por cobro, decisión
    // del usuario. Entre el transitorio (paso 1) y el definitivo (paso 3, sin cambios):
    // D la MISMA cuenta transitoria (resolverLineaTransitoria, compartida con ASN1/ASN2) →
    // H las cuentas de activo que abrió el asiento ③ de apertura (1.4.05.05 aportes,
    // 1.4.05.10 préstamos) — mismo patrón que ya usa Petro
    // (CobroPetroContableServiceImpl.contabilizarReparto), MISMA implementación de las
    // líneas de activo vía ContabilizacionIndividualCreditoService#lineasReparto (no una
    // copia: dos asientos de reparto con código distinto divergen tarde o temprano).
    //
    // ⚠️ El monto es cobro.getValor() — lo que EFECTIVAMENTE pasó por la transitoria (el
    // depósito), NUNCA "el total cobrado" de la operación completa. La parte pagada con
    // APORTES CONSUMIDOS (precancelación/acuerdo mixtos) no toca la transitoria — nunca
    // entró por el banco — y ya tiene su propio cruce en el asiento 3
    // (lineasCruceAportesConsumidos, sin cambios). El invariante que esto preserva: LA
    // TRANSITORIA CIERRA EXACTAMENTE LO QUE ABRIÓ (§5.1 del plan de cierre contable). Si el
    // asiento 2 debitara más de lo que el asiento 1 acreditó, no neteraría — quedaría
    // sobre-debitada para siempre, y eso solo se descubre conciliando.
    // =====================================================================

    /**
     * Total depositado por aportes y por préstamos, SOLO lo depositado (nunca lo pagado con
     * aportes consumidos, que vive aparte) — compartido entre {@code generarAsientoReparto}
     * (2, su Haber) y {@code generarAsientoDefinitivo} (3, su Debe, desde 2026-08-31): las dos
     * mitades de la misma cuenta "por aplicar" tienen que salir de la MISMA suma, nunca de dos
     * cálculos independientes que puedan desalinearse.
     *
     * @return {@code [totalAportes, totalPrestamos]}
     */
    private double[] totalesAportesPrestamos(List<DetalleCobroCredito> detalles) {
        double totalAportes = 0.0;
        double totalPrestamos = 0.0;
        for (DetalleCobroCredito detalle : detalles) {
            double valor = nvl(detalle.getValor());
            if (detalle.getPrestamo() != null) {
                totalPrestamos += valor;
            } else if (detalle.getTipoAporte() != null) {
                totalAportes += valor;
            }
        }
        return new double[]{redondear(totalAportes), redondear(totalPrestamos)};
    }

    /**
     * Agrega cédula, nombre del partícipe y los préstamos del cobro a la observación de un
     * asiento — mismo formato en los tres asientos de un cobro, un solo lugar (2026-08-31,
     * pedido del usuario, aplicado primero al circuito de CBCR — condonación/devolución/Petro/
     * cierre de cartera arman su propia observación por separado y quedan afuera de este
     * cambio).
     *
     * <p><b>Consumido por CBCRASN2 y por TODA la ruta de cobros</b> (transitorio, reparto y
     * definitivo) — no solo por precancelación. Un cambio acá se ve en cualquier asiento nuevo
     * de cobro individual, incluidos los que generan otros procesos en paralelo sobre el mismo
     * préstamo (carga Petro, retroactivo de jubilados): tocar este método con eso corriendo es
     * a propósito no invasivo (agrega texto, no cambia montos ni cuentas), pero es la razón por
     * la que el alcance real de este cambio es más grande de lo que el nombre del método
     * sugiere.</p>
     *
     * <p><b>2026-09-04, corregido:</b> antes cortaba en el primer préstamo con {@code idAsoprep}
     * no nulo (un cobro múltiple o una precancelación con varios préstamos mostraba uno solo) y
     * dependía de {@code idAsoprep}, que es {@code null} en todo préstamo que no venga de Petro
     * (en ese caso no mostraba ninguno). Ahora lista TODOS los préstamos distintos del cobro,
     * con su código siempre, y el {@code idAsoprep} además cuando existe.</p>
     *
     * @param detalles Si viene {@code null} (como en {@code generarAsientoTransitorio}, que no
     *                 recibe la lista), se leen de la base con {@code selectByCobro} — un costo
     *                 aceptable porque el registro de un cobro no es una ruta de alto volumen.
     */
    private String observacionEnriquecida(CobroCredito cobro, List<DetalleCobroCredito> detalles, String base)
            throws Throwable {
        List<DetalleCobroCredito> lista = detalles != null
                ? detalles : detalleCobroCreditoDaoService.selectByCobro(cobro.getCodigo());

        StringBuilder obs = new StringBuilder(base);
        Entidad entidad = cobro.getEntidad();
        if (entidad != null) {
            obs.append(" | Cédula: ").append(entidad.getNumeroIdentificacion() != null
                    ? entidad.getNumeroIdentificacion() : "-");
            obs.append(" | Nombre: ").append(entidad.getRazonSocial() != null
                    ? entidad.getRazonSocial() : "-");
        }
        if (lista != null) {
            List<Long> prestamosListados = new ArrayList<>();
            for (DetalleCobroCredito detalle : lista) {
                Prestamo prestamo = detalle.getPrestamo();
                if (prestamo == null || prestamo.getCodigo() == null
                        || prestamosListados.contains(prestamo.getCodigo())) {
                    continue;
                }
                prestamosListados.add(prestamo.getCodigo());
                obs.append(" | Préstamo: ").append(prestamo.getCodigo());
                if (prestamo.getIdAsoprep() != null) {
                    obs.append(" (idAsoprep: ").append(prestamo.getIdAsoprep()).append(")");
                }
            }
        }

        String resultado = obs.toString();
        // ASNTOBSR admite 2000 caracteres (verificado en com.saa.model.cnt.Asiento) — tope
        // defensivo: nunca asumir que la observación base más esto no va a crecer más que eso.
        // Con varios préstamos se llega mucho antes que antes de este cambio (2026-09-04) — el
        // recorte sigue siendo la última línea de defensa, no algo que dejó de hacer falta.
        if (resultado.length() > 2000) {
            resultado = resultado.substring(0, 1997) + "...";
        }
        return resultado;
    }

    /**
     * Capital de cuotas posteriores al corte del período abierto que este cobro cancela — el
     * tramo que la apertura mensual NUNCA abrió, y que por eso los asientos 2 y 3 NO deben
     * cerrar contra la cuenta de apertura (2.3.02.10). Compartido entre
     * {@code generarAsientoReparto} y {@code generarAsientoDefinitivo}: las dos restas tienen
     * que salir del MISMO número, calculado UNA sola vez acá, nunca de dos cálculos
     * independientes que puedan desalinearse (2026-09-05, ver
     * {@code docs/logica-negocio/crd/DISENO-CIERRE-APERTURA-SOLO-LO-ABIERTO.md}).
     *
     * <p>Da igual si el tramo futuro se pagó con dinero o con cruce de valores — decisión del
     * usuario: lo que decide es QUÉ se pagó, no CÓMO. Por eso este método mira
     * {@code capitalFuturoPosteriorACorte} sobre los {@code PagoPrestamo} de cada préstamo del
     * cobro, sin importar el tipo de operación del cobro en sí.</p>
     *
     * <p><b>Sin corrida de cierre viva para el período del cobro: 0.0, con traza.</b> No se
     * bloquea el cobro — los dos asientos se comportan exactamente como hoy, con el total
     * completo, porque sin período abierto no hay apertura que cerrar de más (§5.1 de la
     * especificación).</p>
     */
    private double calcularCapitalFuturoDelCobro(CobroCredito cobro, List<DetalleCobroCredito> detalles)
            throws Throwable {
        Long idEmpresa = derivarEmpresaCobro(cobro);
        Long anio = Long.valueOf(cobro.getFecha().getYear());
        Long mes = Long.valueOf(cobro.getFecha().getMonthValue());
        CorridaCierreCartera corridaViva = corridaCierreCarteraDaoService.selectVivaByPeriodo(idEmpresa, anio, mes);
        if (corridaViva == null || corridaViva.getFechaCorte() == null) {
            System.out.println("CobroCreditoService.calcularCapitalFuturoDelCobro - cobro " + cobro.getCodigo()
                    + ": no hay corrida de cierre de cartera viva para " + mes + "/" + anio + " en empresa "
                    + idEmpresa + "; los asientos 2 y 3 cierran la apertura por el total, sin partir.");
            return 0.0;
        }
        LocalDate fechaCorteApertura = corridaViva.getFechaCorte();

        double capitalFuturo = 0.0;
        for (DetalleCobroCredito detalle : detalles) {
            if (detalle.getPrestamo() == null || detalle.getEventoPrestamo() == null) {
                continue;
            }
            List<PagoPrestamo> pagos = pagoPrestamoDaoService.selectByEvento(detalle.getEventoPrestamo().getCodigo());
            capitalFuturo += contabilizacionIndividualCreditoService.capitalFuturoPosteriorACorte(
                    pagos, fechaCorteApertura, "Cobro " + cobro.getCodigo());
        }
        return redondear(capitalFuturo);
    }

    private Asiento generarAsientoReparto(CobroCredito cobro, List<DetalleCobroCredito> detalles,
            double capitalFuturo) throws Throwable {
        Long idEmpresa = derivarEmpresaCobro(cobro);
        DetallePlantilla lineaTransitoria = resolverLineaTransitoria(idEmpresa);

        // Reparto entre aportes/préstamos SOLO de lo depositado — cada línea de detalle es
        // préstamo (depósito a un préstamo) o aporte (depósito a la cuenta de aportes,
        // REGISTRO_APORTE/COBRO_MIXTO); linea.getValor() nunca incluye lo pagado con aportes
        // consumidos (eso vive aparte, en DetalleAportePrecancelacion/
        // DetalleAporteAcuerdoCondonacion, y no entra acá).
        //
        // capitalFuturo (2026-09-05) se resta SOLO del lado préstamos, nunca de aportes: es
        // capital que la apertura mensual nunca abrió (vence después del corte del período
        // abierto), así que este asiento no debe cerrar la cuenta de apertura (2.3.02.10) por
        // ese tramo — se resta acá y también del Debe a la transitoria, para que las dos
        // patas de este asiento sigan cuadrando entre sí. El asiento 3 (generarAsientoDefinitivo)
        // se encarga de dejar ese tramo pendiente en la transitoria. Ver
        // docs/logica-negocio/crd/DISENO-CIERRE-APERTURA-SOLO-LO-ABIERTO.md §4.
        double[] totales = totalesAportesPrestamos(detalles);
        double totalAportes = totales[0];
        double totalPrestamosCerrado = redondear(totales[1] - capitalFuturo);

        Long idPlantillaReparto = contabilizacionIndividualCreditoService.resolverPlantillaReparto(idEmpresa);

        List<DetalleAsiento> lineas = new ArrayList<>();
        DetalleAsiento debe = new DetalleAsiento();
        debe.setPlanCuenta(lineaTransitoria.getPlanCuenta());
        debe.setNumeroCuenta(lineaTransitoria.getPlanCuenta().getCuentaContable());
        debe.setNombreCuenta(lineaTransitoria.getPlanCuenta().getNombre());
        debe.setDescripcion("Cobro crédito " + cobro.getCodigo() + " - reparto - cuenta transitoria");
        debe.setValorDebe(redondear(cobro.getValor() - capitalFuturo));
        debe.setValorHaber(0.0);
        lineas.add(debe);
        lineas.addAll(contabilizacionIndividualCreditoService.lineasReparto(idPlantillaReparto, totalAportes,
                totalPrestamosCerrado, "Cobro crédito " + cobro.getCodigo() + " - reparto"));

        // Cuadre explícito, no asumido: si la plantilla de reparto tuviera el movimiento
        // configurado al revés de lo que espera Petro, esto lo revienta acá en vez de dejar
        // pasar un asiento invertido.
        double totalDebe = 0.0;
        double totalHaber = 0.0;
        for (DetalleAsiento linea : lineas) {
            totalDebe += nvl(linea.getValorDebe());
            totalHaber += nvl(linea.getValorHaber());
        }
        if (Math.abs(redondear(totalDebe - totalHaber)) > TOLERANCIA_CUADRE) {
            throw new IncomeException("El asiento de reparto del cobro " + cobro.getCodigo()
                    + " no cuadra: DEBE $" + redondear(totalDebe) + " vs HABER $" + redondear(totalHaber)
                    + ". No se genera un asiento desbalanceado.");
        }

        return asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS, cobro.getFecha(),
                observacionEnriquecida(cobro, detalles, "Cobro crédito " + cobro.getCodigo() + " - reparto"
                        + (cobro.getObservacion() != null ? ": " + cobro.getObservacion() : "")),
                cobro.getUsuarioProceso(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    // =====================================================================
    // Asiento DEFINITIVO (paso 3, CBCRASN2) — docs/logica-negocio/crd/ESPECIFICACION-CBCRASN2.md
    //
    // Cierra la transitoria que abrió generarAsientoTransitorio (ASN1): D la MISMA cuenta
    // transitoria (resuelta por resolverLineaTransitoria, compartido con ASN1 — nunca dos
    // resoluciones independientes) → H las cuentas que la operación efectivamente liquidó.
    //
    // ⚠️ ALTERNATIVA, NO COMPLEMENTO, a los hooks de ContabilidadPrestamoService.
    //
    // Actualizado 2026-08-31 (circuito de cobros con aportes, decisión del usuario): ya existe
    // el discriminador. contabilizarPagoConAportes y contabilizarPrecancelacion generan asiento
    // SOLO cuando ContextoPago.idCobroCredito viene null (llamada directa, sin depósito) — con
    // valor, la llamada nació de este procesarCobro, que ya genera CBCRASN2 por la misma plata,
    // y esos dos hooks devuelven null sin tocar nada. precancelar() setea idCobroCredito desde
    // SolicitudPrecancelacion.idCobroCredito, que este método pone explícitamente más abajo
    // (nunca lo manda el cliente). contabilizarPagoCuota y contabilizarAbonoCapital TODAVÍA no
    // tienen ese discriminador — siguen devolviendo null a propósito; llenarlos sin agregarles
    // el mismo idCobroCredito duplicaría el asiento exactamente como describía este comentario.
    // =====================================================================

    private Asiento generarAsientoDefinitivo(CobroCredito cobro, List<DetalleCobroCredito> detalles)
            throws Throwable {
        Long idEmpresa = derivarEmpresaCobro(cobro);
        Long idPlantillaAplicacion = contabilizacionIndividualCreditoService.resolverPlantillaAplicacion(idEmpresa);
        LocalDate fechaCorte = cobro.getFecha();
        String tipoOperacion = cobro.getTipoOperacion();

        List<DetalleAsiento> haber = new ArrayList<>();
        for (DetalleCobroCredito detalle : detalles) {
            if (detalle.getPrestamo() != null && detalle.getEventoPrestamo() != null) {
                haber.addAll(haberDesdeEvento(detalle.getEventoPrestamo().getCodigo(), idEmpresa,
                        idPlantillaAplicacion, fechaCorte, "Cobro " + cobro.getCodigo()));
            } else if (detalle.getTipoAporte() != null) {
                double valorAporte = detalle.getPagoAporte() != null && detalle.getPagoAporte().getValor() != null
                        ? detalle.getPagoAporte().getValor() : nvl(detalle.getValor());
                if (valorAporte > 0.0) {
                    haber.add(contabilizacionIndividualCreditoService.lineaAporteRegistrado(idPlantillaAplicacion,
                            detalle.getTipoAporte().getCodigo(), valorAporte,
                            "Cobro " + cobro.getCodigo()));
                }
            }
            // La mitad de aportes CONSUMIDOS (precancelación mixta / acuerdo mixto, §5 de la
            // especificación) se agrega más abajo, por línea, para no perder la referencia a
            // linea.getCodigo()/getAcuerdoCondonacion() en DAPR/DAAP.
            if (CrdTipoOperacionCobro.PRECANCELACION.equals(tipoOperacion)) {
                List<DetalleAportePrecancelacion> consumidos =
                        detalleAportePrecancelacionDaoService.selectByDetalleCobro(detalle.getCodigo());
                if (!consumidos.isEmpty()) {
                    List<DesgloseAporte> desglose = new ArrayList<>();
                    for (DetalleAportePrecancelacion consumido : consumidos) {
                        DesgloseAporte renglon = new DesgloseAporte();
                        renglon.setIdTipoAporte(consumido.getTipoAporte().getCodigo());
                        renglon.setValor(consumido.getValor());
                        desglose.add(renglon);
                    }
                    haber.addAll(contabilizacionIndividualCreditoService.lineasCruceAportesConsumidos(
                            idPlantillaAplicacion, desglose, "Cobro " + cobro.getCodigo()));
                }
            } else if (CrdTipoOperacionCobro.ACUERDO_CONDONACION.equals(tipoOperacion)
                    && detalle.getAcuerdoCondonacion() != null) {
                // Acuerdo MIXTO (depósito + aportes): el 100%-aportes nunca llega acá — no
                // genera CBCR, su cruce lo genera aplicarAcuerdo directamente (ver su
                // javadoc). Acá solo el caso con parte de depósito, donde SÍ hay CBCRASN2.
                List<DetalleAporteAcuerdoCondonacion> consumidos = detalleAporteAcuerdoCondonacionDaoService
                        .selectByAcuerdo(detalle.getAcuerdoCondonacion().getCodigo());
                if (!consumidos.isEmpty()) {
                    List<DesgloseAporte> desglose = new ArrayList<>();
                    for (DetalleAporteAcuerdoCondonacion consumido : consumidos) {
                        DesgloseAporte renglon = new DesgloseAporte();
                        renglon.setIdTipoAporte(consumido.getTipoAporte().getCodigo());
                        renglon.setValor(consumido.getValor());
                        desglose.add(renglon);
                    }
                    haber.addAll(contabilizacionIndividualCreditoService.lineasCruceAportesConsumidos(
                            idPlantillaAplicacion, desglose, "Cobro " + cobro.getCodigo()));
                }
            }
        }

        if (haber.isEmpty()) {
            throw new IncomeException("El cobro " + cobro.getCodigo() + " no generó ninguna línea"
                    + " de haber para el asiento definitivo; no se puede contabilizar.");
        }

        double totalHaber = 0.0;
        for (DetalleAsiento linea : haber) {
            totalHaber += nvl(linea.getValorHaber()) - nvl(linea.getValorDebe());
        }
        totalHaber = redondear(totalHaber);

        // Debe: "aportes/préstamos por aplicar" — 2026-08-31, corrección: antes volvía a
        // debitar la cuenta transitoria (resolverLineaTransitoria), pero el asiento de reparto
        // (2) ya la cerró (D transitoria → H por-aplicar) — debitarla OTRA VEZ acá la dejaba
        // en -cobro.getValor() en vez de en cero. El asiento 3 tiene que cerrar lo que el 2
        // dejó abierto: debita las MISMAS cuentas "por aplicar" que apertura (③, cierre de
        // cartera) y la aplicación de Petro ya usan (CrdLineaAsiento.APORTES_POR_APLICAR/
        // PRESTAMOS_POR_APLICAR, catálogo semántico — NO la plantilla posicional del reparto).
        // Mismos totales que el Haber del asiento 2 (totalesAportesPrestamos), nunca un cálculo
        // aparte que pueda desalinearse de lo que ese asiento ya cerró.
        double[] totales = totalesAportesPrestamos(detalles);
        List<DetalleAsiento> debe = contabilizacionIndividualCreditoService.lineasAplicacionPorAplicar(
                idPlantillaAplicacion, totales[0], totales[1], "Cobro crédito " + cobro.getCodigo());
        if (debe.isEmpty()) {
            throw new IncomeException("El cobro " + cobro.getCodigo() + " no generó ninguna línea de"
                    + " aportes/préstamos por aplicar para el Debe del asiento definitivo; no se puede"
                    + " contabilizar.");
        }

        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.addAll(debe);
        lineas.addAll(haber);

        // Cuadre por construcción: D = suma de "por aplicar" (totales[0]+totales[1], lo mismo
        // que acreditó el asiento de reparto), H = suma de las líneas armadas arriba. Si no
        // cuadran, algo quedó mal clasificado — fallar acá es más seguro que ajustar la
        // diferencia con una línea de cuadre, porque "cuadra" no prueba que la clasificación
        // sea correcta (§7 de la especificación): un ajuste de centavos escondería
        // precisamente ese tipo de error.
        double totalDebe = 0.0;
        for (DetalleAsiento linea : debe) {
            totalDebe += nvl(linea.getValorDebe()) - nvl(linea.getValorHaber());
        }
        totalDebe = redondear(totalDebe);
        double diferencia = redondear(totalDebe - totalHaber);
        if (Math.abs(diferencia) > TOLERANCIA_CUADRE) {
            throw new IncomeException("El asiento definitivo del cobro " + cobro.getCodigo()
                    + " no cuadra: por aplicar $" + totalDebe + " vs. clasificado $" + totalHaber
                    + " (diferencia $" + diferencia + "). No se genera un asiento desbalanceado.");
        }

        return asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS, fechaCorte,
                observacionEnriquecida(cobro, detalles, "Cobro crédito " + cobro.getCodigo()
                        + " - asiento definitivo"
                        + (cobro.getObservacion() != null ? ": " + cobro.getObservacion() : "")),
                cobro.getUsuarioProceso(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));
    }

    /**
     * Todas las líneas de haber que salen de un EventoPrestamo — cubre PAGO_CUOTA,
     * PAGO_MULTIPLE, ABONO_CAPITAL, PRECANCELACION, COBRO_MIXTO (líneas de préstamo) y
     * ACUERDO_CONDONACION (K9: el único PagoPrestamo que genera lleva SOLO lo pagado, nunca lo
     * condonado — este método nunca ve lo condonado, así que "solo lo pagado" sale gratis, sin
     * ninguna rama especial) CON UNA SOLA REGLA, EN VEZ DE UNA POR TIPO:
     *
     * <p>Por cada {@code PagoPrestamo} vigente del evento, el capital a bandear es
     * {@code saldoOtros} si es &gt; 0, si no {@code capitalPagado}. Es la regla que evita las
     * trampas 2.1 (abono a capital) y 2.2 (capital futuro de precancelación) — las dos escriben
     * en {@code saldoOtros} con {@code capitalPagado = 0}, y un pago normal es al revés; nunca
     * los dos a la vez, por construcción de todo el motor de pagos. Para un pago normal, la
     * banda se resuelve con la fecha de vencimiento de la cuota real que se pagó; para el
     * capital futuro de una precancelación, sigue siendo la de la cuota ancla. Para un abono a
     * capital YA NO: se reparte proporcional entre las cuotas que el abono historizó en
     * CRD.HDTP, no contra la ancla (2026-08-31, ver el javadoc de {@code
     * ContabilizacionIndividualCreditoService#haberDesdePagos}).
     */
    private List<DetalleAsiento> haberDesdeEvento(Long idEvento, Long idEmpresa, Long idPlantillaAplicacion,
            LocalDate fechaCorte, String prefijoDescripcion) throws Throwable {
        // Delegado a ContabilizacionIndividualCreditoService.haberDesdePagos (2026-08-31,
        // PLAN-CIERRE-CONTABLE-TOTAL): la MISMA regla la necesitaba también el cruce de
        // valores/precancelación directa — un tercer lugar con esta lógica copiada era
        // exactamente el riesgo de divergencia que este servicio existe para evitar.
        List<PagoPrestamo> pagos = pagoPrestamoDaoService.selectByEvento(idEvento);
        return contabilizacionIndividualCreditoService.haberDesdePagos(pagos, idEmpresa, idPlantillaAplicacion,
                fechaCorte, prefijoDescripcion);
    }
}

package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.ejb.crd.dao.AcuerdoCondonacionDaoService;
import com.saa.ejb.crd.dao.CobroCreditoDaoService;
import com.saa.ejb.crd.dao.DetalleAcuerdoCondonacionDaoService;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.AcuerdoCondonacionService;
import com.saa.ejb.crd.service.ClasificadorBandaService;
import com.saa.ejb.crd.service.CobroCreditoService;
import com.saa.ejb.crd.service.ConfiguracionContabilidadService;
import com.saa.ejb.crd.service.DetallePrestamoService;
import com.saa.ejb.crd.service.EventoPrestamoService;
import com.saa.ejb.crd.service.PagoPrestamoService;

import com.saa.ejb.crd.service.ProcesoPagoPrestamoService;
import com.saa.ejb.crd.service.dto.BandaProductoDetalle;
import com.saa.ejb.crd.service.dto.DesgloseConceptosPrestamo;
import com.saa.ejb.crd.service.dto.DetalleConceptoAcuerdoDTO;
import com.saa.ejb.crd.service.dto.DetalleRegistroCobroDTO;
import com.saa.ejb.crd.service.dto.ResultadoAplicacionAcuerdo;
import com.saa.ejb.crd.service.dto.ResultadoClasificacionBanda;
import com.saa.ejb.crd.service.dto.ResultadoRegistroCobro;
import com.saa.ejb.crd.service.dto.SolicitudRegistroAcuerdo;
import com.saa.ejb.crd.service.dto.SolicitudRegistroCobro;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.crd.AcuerdoCondonacion;
import com.saa.model.crd.CobroCredito;
import com.saa.model.crd.DetalleAcuerdoCondonacion;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.EventoPrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.Producto;
import com.saa.rubros.CrdConceptoPrestamo;
import com.saa.rubros.CrdEstadoAcuerdoCondonacion;
import com.saa.rubros.CrdLineaAsiento;
import com.saa.rubros.CrdTipoOperacionCobro;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoPrestamo;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.PlantillasCredito;
import com.saa.rubros.TipoAsientos;
import com.saa.rubros.TipoCarteraBanda;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;

/**
 * @see AcuerdoCondonacionService
 * @author Sistema SAA
 * @since 2026-08-29
 */
@Stateless
public class AcuerdoCondonacionServiceImpl implements AcuerdoCondonacionService {

    private static final double TOLERANCIA = 0.01;

    @EJB
    private AcuerdoCondonacionDaoService acuerdoCondonacionDaoService;

    @EJB
    private DetalleAcuerdoCondonacionDaoService detalleAcuerdoCondonacionDaoService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private ProcesoPagoPrestamoService procesoPagoPrestamoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private DetallePrestamoService detallePrestamoService;

    @EJB
    private PagoPrestamoService pagoPrestamoService;

    @EJB
    private EventoPrestamoService eventoPrestamoService;

    @EJB
    private ConfiguracionContabilidadService configuracionContabilidadService;

    @EJB
    private AsientoContableService asientoContableService;

    @EJB
    private PlantillaService plantillaService;

    @EJB
    private DetallePlantillaDaoService detallePlantillaDaoService;

    @EJB
    private ClasificadorBandaService clasificadorBandaService;

    @EJB
    private CobroCreditoService cobroCreditoService;

    @EJB
    private CobroCreditoDaoService cobroCreditoDaoService;

    /**
     * Confirma el acuerdo Y registra su cobro en CBCR en el MISMO acto (§5 del plan,
     * rediseñado el 2026-08-30: ya no hay aprobación de condonación previa). El préstamo NO
     * se toca acá — eso ocurre recién al procesar (K11), vía {@link #aplicarAcuerdo}.
     *
     * ⚠️ INVARIANTE: {@code ACCNVLPG}/{@code ACCNVLCN} de la cabecera SIEMPRE se calculan
     * como la suma del detalle — NUNCA son un dato de entrada independiente. No hay ningún
     * camino donde la cabecera diga una cosa y el detalle sume otra, porque la cabecera no
     * existe como dato que alguien pueda enviar por separado. Cualquier código futuro que
     * actualice un acuerdo tiene que sostener esto de la misma forma: producir el detalle y
     * derivar la cabecera de él, nunca dos actualizaciones paralelas.
     */
    @Override
    public AcuerdoCondonacion registrarAcuerdo(SolicitudRegistroAcuerdo solicitud) throws Throwable {
        System.out.println("AcuerdoCondonacionService.registrarAcuerdo - prestamo: "
                + (solicitud != null ? solicitud.getIdPrestamo() : null));

        Prestamo prestamo = validar(solicitud);

        AcuerdoCondonacion acuerdo = new AcuerdoCondonacion();
        acuerdo.setEntidad(prestamo.getEntidad());
        acuerdo.setPrestamo(prestamo);
        acuerdo.setEstado(Long.valueOf(CrdEstadoAcuerdoCondonacion.VIGENTE));
        acuerdo.setFecha(solicitud.getFecha());
        acuerdo.setObservacion(solicitud.getObservacion());
        acuerdo.setUsuarioRegistro(solicitud.getUsuario());
        acuerdo.setFechaRegistro(LocalDateTime.now());

        double totalPagar = 0.0;
        double totalCondonar = 0.0;
        for (DetalleConceptoAcuerdoDTO linea : solicitud.getDetalles()) {
            totalPagar += linea.getValorPagado();
            totalCondonar += linea.getValorCondonado();
        }
        acuerdo.setValorPagar(redondear(totalPagar));
        acuerdo.setValorCondonar(redondear(totalCondonar));
        acuerdo = acuerdoCondonacionDaoService.save(acuerdo, null);

        for (DetalleConceptoAcuerdoDTO lineaSolicitud : solicitud.getDetalles()) {
            DetalleAcuerdoCondonacion linea = new DetalleAcuerdoCondonacion();
            linea.setAcuerdo(acuerdo);
            linea.setConcepto(lineaSolicitud.getConcepto());
            linea.setValorAdeudado(redondear(lineaSolicitud.getValorAdeudado()));
            linea.setValorPagado(redondear(lineaSolicitud.getValorPagado()));
            linea.setValorCondonado(redondear(lineaSolicitud.getValorCondonado()));
            detalleAcuerdoCondonacionDaoService.save(linea, null);
        }

        // El cobro por la parte NO condonada, en el MISMO acto — nace con el monto ya fijo
        // porque el acuerdo ya está decidido (no hay ventana de aprobación de por medio).
        SolicitudRegistroCobro solicitudCobro = new SolicitudRegistroCobro();
        solicitudCobro.setIdEntidad(prestamo.getEntidad().getCodigo());
        solicitudCobro.setTipoOperacion(CrdTipoOperacionCobro.ACUERDO_CONDONACION);
        solicitudCobro.setIdCuentaBancaria(solicitud.getIdCuentaBancaria());
        solicitudCobro.setReferencia(solicitud.getReferencia());
        solicitudCobro.setRutaRespaldo(solicitud.getRutaRespaldo());
        solicitudCobro.setValor(acuerdo.getValorPagar());
        solicitudCobro.setFecha(solicitud.getFecha());
        solicitudCobro.setObservacion(solicitud.getObservacion());
        solicitudCobro.setUsuario(solicitud.getUsuario());
        DetalleRegistroCobroDTO lineaCobro = new DetalleRegistroCobroDTO();
        lineaCobro.setIdPrestamo(prestamo.getCodigo());
        lineaCobro.setIdAcuerdo(acuerdo.getCodigo());
        lineaCobro.setValor(acuerdo.getValorPagar());
        solicitudCobro.setDetalles(Collections.singletonList(lineaCobro));

        ResultadoRegistroCobro resultadoCobro = cobroCreditoService.registrarCobro(solicitudCobro);

        // Enlazar desde el nacimiento — nunca queda una ventana con el acuerdo sin su cobro.
        AcuerdoCondonacion acuerdoConCobro = acuerdoCondonacionDaoService.selectById(acuerdo.getCodigo(),
                NombreEntidadesCredito.ACUERDO_CONDONACION);
        CobroCredito cobro = cobroCreditoDaoService.selectById(resultadoCobro.getIdCobro(),
                NombreEntidadesCredito.COBRO_CREDITO);
        acuerdoConCobro.setCobroCredito(cobro);
        return acuerdoCondonacionDaoService.save(acuerdoConCobro, acuerdoConCobro.getCodigo());
    }

    /**
     * Staleness al PROCESAR (§3 del plan, reubicado el 2026-08-30 tras derogarse K4). Solo
     * lectura — NO persiste nada, compara contra un cálculo efímero. Lo llama
     * {@code CobroCreditoService#procesarCobro} ANTES de invocar {@link #aplicarAcuerdo}, por
     * la misma razón que la precancelación: si se llamara a {@code aplicarAcuerdo} y este
     * lanzara, el contenedor ya marcó la transacción rollback-only y no se podría grabar el
     * rechazo del cobro después.
     *
     * @return {@code null} si el desglose sigue vigente; el motivo del rechazo si no
     */
    @Override
    public String verificarVigencia(Long idAcuerdo) throws Throwable {
        AcuerdoCondonacion acuerdo = buscarAcuerdo(idAcuerdo);
        List<DetalleAcuerdoCondonacion> detalles = detalleAcuerdoCondonacionDaoService.selectByAcuerdo(idAcuerdo);
        String diferencia = verificarAdeudados(acuerdo.getPrestamo().getCodigo(), acuerdo.getFecha(), detalles);
        if (diferencia == null) {
            return null;
        }
        return "Rechazado automáticamente por el sistema: el saldo del préstamo cambió entre el"
                + " registro del acuerdo y el proceso del cobro. " + diferencia;
    }

    @Override
    public void anularAcuerdoPorCobro(Long idAcuerdo, String usuario, LocalDateTime fecha, String motivo)
            throws Throwable {
        System.out.println("AcuerdoCondonacionService.anularAcuerdoPorCobro - acuerdo: " + idAcuerdo);
        // CK_ACCN_MTRC exige motivo para ANULADO(3) — mismo CHECK que antes exigía motivo
        // para RECHAZADO(3), reciclado con el estado. Si estos llegan nulos, Oracle revienta
        // con ORA-02290 y de paso arrastra a la anulación del CBCR completo — validar acá
        // ANTES de tocar nada, con un mensaje legible en vez de ese error crudo.
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IncomeException("usuario es obligatorio para anular el acuerdo " + idAcuerdo);
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IncomeException("El motivo es obligatorio para anular el acuerdo " + idAcuerdo);
        }
        AcuerdoCondonacion acuerdo = buscarAcuerdo(idAcuerdo);
        if (acuerdo.getEstado() == null || acuerdo.getEstado() != CrdEstadoAcuerdoCondonacion.VIGENTE) {
            throw new IncomeException("El acuerdo " + idAcuerdo + " está en estado "
                    + textoEstado(acuerdo.getEstado()) + "; solo se puede anular un acuerdo VIGENTE");
        }
        acuerdo.setEstado(Long.valueOf(CrdEstadoAcuerdoCondonacion.ANULADO));
        // Mismos 3 campos que antes servían para el rechazo (ACCNUSRC/ACCNFCRC/ACCNMTRC) —
        // ahora duplican la huella de anulación del CBCR, a propósito (ver el javadoc de la
        // interfaz: K6 hace de ACCN la única fuente consultable, no depender de un join).
        acuerdo.setUsuarioRechazo(usuario);
        acuerdo.setFechaRechazo(fecha != null ? fecha : LocalDateTime.now());
        acuerdo.setMotivoRechazo(motivo.trim());
        acuerdoCondonacionDaoService.save(acuerdo, acuerdo.getCodigo());
    }

    private double valorPorConcepto(DesgloseConceptosPrestamo desglose, Long concepto) {
        if (concepto == CrdConceptoPrestamo.CAPITAL) {
            return desglose.getCapitalPendiente();
        }
        if (concepto == CrdConceptoPrestamo.INTERES) {
            return desglose.getInteresPendiente();
        }
        if (concepto == CrdConceptoPrestamo.MORA) {
            return desglose.getMoraPendiente();
        }
        if (concepto == CrdConceptoPrestamo.DESGRAVAMEN) {
            return desglose.getDesgravamenPendiente();
        }
        if (concepto == CrdConceptoPrestamo.SEGURO_INCENDIO) {
            return desglose.getSeguroIncendioPendiente();
        }
        return 0.0;
    }

    private String nombreConcepto(Long concepto) {
        if (concepto == CrdConceptoPrestamo.CAPITAL) {
            return "CAPITAL";
        }
        if (concepto == CrdConceptoPrestamo.INTERES) {
            return "INTERES";
        }
        if (concepto == CrdConceptoPrestamo.MORA) {
            return "MORA";
        }
        if (concepto == CrdConceptoPrestamo.DESGRAVAMEN) {
            return "DESGRAVAMEN";
        }
        if (concepto == CrdConceptoPrestamo.SEGURO_INCENDIO) {
            return "SEGURO_INCENDIO";
        }
        return String.valueOf(concepto);
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    // =====================================================================
    // Aplicación del acuerdo (pago + condonación + CANCELADO, K11). La llama
    // CobroCreditoService.procesarCobro DESPUÉS de confirmar con verificarVigencia() que el
    // desglose sigue vigente — este método ya no repite esa comprobación.
    // =====================================================================

    @Override
    public ResultadoAplicacionAcuerdo aplicarAcuerdo(Long idAcuerdo, String usuario) throws Throwable {
        System.out.println("AcuerdoCondonacionService.aplicarAcuerdo - acuerdo: " + idAcuerdo);
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IncomeException("usuario es obligatorio");
        }
        AcuerdoCondonacion acuerdo = buscarAcuerdo(idAcuerdo);
        if (acuerdo.getEstado() == null || acuerdo.getEstado() != CrdEstadoAcuerdoCondonacion.VIGENTE) {
            throw new IncomeException("El acuerdo " + idAcuerdo + " está en estado "
                    + textoEstado(acuerdo.getEstado()) + "; solo se puede aplicar un acuerdo VIGENTE");
        }
        if (acuerdo.getEventoPrestamo() != null) {
            throw new IncomeException("El acuerdo " + idAcuerdo + " ya fue aplicado (evento "
                    + acuerdo.getEventoPrestamo().getCodigo() + ")");
        }

        Prestamo prestamo = acuerdo.getPrestamo();
        List<DetalleAcuerdoCondonacion> detalles = detalleAcuerdoCondonacionDaoService.selectByAcuerdo(idAcuerdo);

        // ⚠️ REGLA INNEGOCIABLE (§3 del plan): la fecha de aplicación es SIEMPRE
        // acuerdo.getFecha() — la misma con la que se comparó (o se comparará) el staleness.
        // NUNCA LocalDate.now(): aplicar con otra fecha cerraría el préstamo con números
        // distintos de los que se aprobaron, sin que nada lo detecte.
        LocalDate fecha = acuerdo.getFecha();
        LocalDateTime fechaHora = fecha.atStartOfDay();

        double capitalPagado = valorConceptoPagado(detalles, CrdConceptoPrestamo.CAPITAL);
        double interesPagado = valorConceptoPagado(detalles, CrdConceptoPrestamo.INTERES);
        double moraPagada = valorConceptoPagado(detalles, CrdConceptoPrestamo.MORA);
        double desgravamenPagado = valorConceptoPagado(detalles, CrdConceptoPrestamo.DESGRAVAMEN);
        double seguroPagado = valorConceptoPagado(detalles, CrdConceptoPrestamo.SEGURO_INCENDIO);
        double capitalCondonado = valorConceptoCondonado(detalles, CrdConceptoPrestamo.CAPITAL);
        double interesCondonado = valorConceptoCondonado(detalles, CrdConceptoPrestamo.INTERES)
                + valorConceptoCondonado(detalles, CrdConceptoPrestamo.MORA);

        // 1. Evento (cabecera, K8) — mismo patrón que ProcesoPagoPrestamoServiceImpl.crearEvento.
        EventoPrestamo evento = new EventoPrestamo();
        evento.setPrestamo(prestamo);
        evento.setTipoOperacion(CrdTipoOperacionCobro.ACUERDO_CONDONACION);
        evento.setValor(redondear(nvl(acuerdo.getValorPagar())));
        evento.setFecha(fechaHora);
        evento.setObservacion(acuerdo.getObservacion());
        evento.setUsuario(usuario);
        evento.setFechaRegistro(LocalDateTime.now());
        evento.setEstado(1L);
        evento = eventoPrestamoService.saveSingle(evento);

        // 2. Cuotas pendientes: TODAS se cierran, aunque no se haya cobrado el 100% — eso ES
        // la condonación (a diferencia de aplicarPagoACuota, que cobra en prelación fija
        // tomando el máximo posible). Se elige una cuota ancla para el único PagoPrestamo
        // (mismo patrón que el "capital futuro" de precancelar), y TODAS —incluida la
        // ancla— pasan a CANCELADA_ANTICIPADA(7), igual que hace precancelar con sus futuras.
        List<DetallePrestamo> pendientes =
                detallePrestamoDaoService.selectCuotasPendientesByPrestamoOrdenadas(prestamo.getCodigo());
        if (pendientes == null || pendientes.isEmpty()) {
            throw new IncomeException("El préstamo " + prestamo.getCodigo()
                    + " no tiene cuotas pendientes que cerrar; no se puede aplicar el acuerdo " + idAcuerdo);
        }
        DetallePrestamo ancla = detallePrestamoDaoService.selectUltimaCuotaPagada(prestamo.getCodigo());
        if (ancla == null) {
            ancla = pendientes.get(0);
        }

        // 3. El ÚNICO PagoPrestamo lleva SOLO lo efectivamente cobrado (K9) — lo condonado
        // NUNCA entra acá. Es lo que hace que anularOperacion revierta el acuerdo completo
        // sin lógica nueva: al reversar este PagoPrestamo se descobra exactamente lo que se
        // cobró, ni un centavo de lo condonado, que nunca estuvo registrado como cobrado.
        PagoPrestamo pago = new PagoPrestamo();
        pago.setPrestamo(prestamo);
        pago.setDetallePrestamo(ancla);
        pago.setNumeroCuota(ancla.getNumeroCuota());
        pago.setFecha(fechaHora);
        pago.setValor(redondear(nvl(acuerdo.getValorPagar())));
        pago.setCapitalPagado(redondear(capitalPagado));
        pago.setInteresPagado(redondear(interesPagado));
        pago.setMoraPagada(redondear(moraPagada));
        pago.setInteresVencidoPagado(0.0);
        pago.setDesgravamen(redondear(desgravamenPagado));
        pago.setValorSeguroIncendio(redondear(seguroPagado));
        pago.setSaldoOtros(0.0);
        pago.setTipo(CrdTipoOperacionCobro.ACUERDO_CONDONACION);
        pago.setObservacion(concatObservacion(acuerdo.getObservacion(),
                "Acuerdo " + idAcuerdo + " - Evento " + evento.getCodigo()));
        pago.setUsuarioRegistro(usuario);
        pago.setFechaRegistro(LocalDateTime.now());
        pago.setEstado(1L);
        pago.setIdEstado(1L);
        pago.setAnulado(0L);
        pago.setEventoPrestamo(evento);
        pago.setRutaDocumentoRespaldo(acuerdo.getCobroCredito() != null
                ? acuerdo.getCobroCredito().getRutaRespaldo() : null);
        pagoPrestamoService.saveSingle(pago);

        int canceladas = 0;
        for (DetallePrestamo cuota : pendientes) {
            cuota.setEstado((long) EstadoCuotaPrestamo.CANCELADA_ANTICIPADA);
            cuota.setIdEstado((long) EstadoCuotaPrestamo.CANCELADA_ANTICIPADA);
            cuota.setFechaPagado(null);
            detallePrestamoService.saveSingle(cuota);
            canceladas++;
        }
        System.out.println("  🚫 Acuerdo " + idAcuerdo + " - cuotas cerradas: " + canceladas);

        // 4. Préstamo → CANCELADO(3), K6. NUNCA CANCELADO_ANTICIPADO(4): ese es el terminal
        // de precancelar, este es otro camino con otra causa.
        prestamo.setIdEstado(Long.valueOf(EstadoPrestamo.CANCELADO));
        prestamo.setFechaModificacion(LocalDateTime.now());
        prestamoDaoService.save(prestamo, prestamo.getCodigo());

        procesoPagoPrestamoService.registrarHuellaPrestamo(prestamo, CrdTipoOperacionCobro.ACUERDO_CONDONACION,
                nvl(acuerdo.getValorPagar()), acuerdo.getObservacion(), fechaHora, usuario);

        // 5. Enlazar el acuerdo con el evento aplicado (K8) y pasarlo a APLICADO.
        acuerdo.setEventoPrestamo(evento);
        acuerdo.setEstado(Long.valueOf(CrdEstadoAcuerdoCondonacion.APLICADO));
        acuerdoCondonacionDaoService.save(acuerdo, acuerdo.getCodigo());

        // 6. Asiento de condonación — plantilla 25, detrás del gate. Con el flag apagado
        // (hoy) esto ni se intenta. Con el flag encendido y la línea de gasto todavía sin
        // definir (§6.2), falla FUERTE con un mensaje claro — no genera un asiento a medias.
        if (configuracionContabilidadService.contabilidadActiva()) {
            generarAsientoCondonacion(acuerdo, prestamo, pendientes, capitalCondonado, interesCondonado, fecha);
        } else {
            System.out.println("  AcuerdoCondonacionService.aplicarAcuerdo - contabilidad de CRD"
                    + " INACTIVA: acuerdo " + idAcuerdo + " aplicado sin generar asiento de condonación.");
        }

        ResultadoAplicacionAcuerdo resultado = new ResultadoAplicacionAcuerdo();
        resultado.setIdAcuerdo(idAcuerdo);
        resultado.setIdEvento(evento.getCodigo());
        resultado.setIdPrestamo(prestamo.getCodigo());
        resultado.setEstadoFinalPrestamo(prestamo.getIdEstado());
        resultado.setValorPagado(acuerdo.getValorPagar());
        resultado.setValorCondonado(acuerdo.getValorCondonar());
        return resultado;
    }

    /**
     * D = línea de gasto nueva de la plantilla 25 (aún sin cuenta asignada, §6.2 del plan —
     * esta llamada falla con IncomeException hasta que exista). H = capital condonado por
     * banda (mismo camino que CobroPetroContableServiceImpl.contabilizarAplicacion: se
     * clasifica CADA cuota vencida contra ClasificadorBandaService y se acumula por banda) +
     * interés condonado (ordinario + mora, una sola línea por tipo de préstamo — mismo
     * criterio que CrdLineaAsiento.INTERES_MORA_POR_COBRAR: "la misma cuenta que el
     * ordinario", la descripción es lo que distingue).
     */
    private void generarAsientoCondonacion(AcuerdoCondonacion acuerdo, Prestamo prestamo,
            List<DetallePrestamo> pendientes, double capitalCondonado, double interesCondonado,
            LocalDate fecha) throws Throwable {
        if (capitalCondonado <= 0.0 && interesCondonado <= 0.0) {
            System.out.println("  AcuerdoCondonacionService.generarAsientoCondonacion - acuerdo "
                    + acuerdo.getCodigo() + " no condonó nada; no se genera asiento.");
            return;
        }
        if (acuerdo.getCobroCredito() == null || acuerdo.getCobroCredito().getCuentaBancaria() == null
                || acuerdo.getCobroCredito().getCuentaBancaria().getPlanCuenta() == null
                || acuerdo.getCobroCredito().getCuentaBancaria().getPlanCuenta().getEmpresa() == null) {
            throw new IncomeException("No se pudo determinar la empresa contable del acuerdo "
                    + acuerdo.getCodigo() + ": falta la cuenta bancaria del cobro asociado.");
        }
        Long idEmpresa = acuerdo.getCobroCredito().getCuentaBancaria().getPlanCuenta().getEmpresa().getCodigo();

        Long idPlantilla = plantillaService.codigoByAlterno(PlantillasCredito.COBRO_INDIVIDUAL_PRESTAMO, idEmpresa);
        if (idPlantilla == null) {
            throw new IncomeException("No existe la plantilla contable alterno "
                    + PlantillasCredito.COBRO_INDIVIDUAL_PRESTAMO + " para la empresa " + idEmpresa + ".");
        }
        DetallePlantilla lineaGasto = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla,
                CrdLineaAsiento.GASTO_CONDONACION_PRESTAMOS);
        if (lineaGasto == null || lineaGasto.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno " + PlantillasCredito.COBRO_INDIVIDUAL_PRESTAMO
                    + " todavía no tiene la línea de gasto por condonación (aux1="
                    + CrdLineaAsiento.GASTO_CONDONACION_PRESTAMOS + "); no se puede contabilizar el"
                    + " acuerdo " + acuerdo.getCodigo() + ". Defina la cuenta antes de activar la"
                    + " contabilidad de CRD para acuerdos de condonación.");
        }

        List<DetalleAsiento> lineas = new ArrayList<>();
        double totalHaber = 0.0;

        // H: capital condonado, por banda — clasificando cada cuota vencida como hace Petro.
        //
        // K12 (decisión del usuario, 2026-08-30): el capital condonado se consume desde la
        // banda de MAYOR mora hacia atrás — se agota el capital pendiente de la cuota más
        // vencida, después la siguiente, y así hasta cubrir el monto condonado. YA NO es un
        // prorrateo proporcional entre todas las cuotas.
        //
        // Por qué: el capital más vencido es el más provisionado. Castigarlo primero hace
        // que la liberación de provisión compense la pérdida — así se da de baja una cartera
        // deteriorada. El prorrateo proporcional tocaba todas las bandas por igual y dejaba
        // castigado capital de bandas tempranas (poco provisionadas) mientras sobrevivía
        // capital de las viejas — exactamente lo contrario de lo que se busca.
        //
        // ⚠️ "ÚLTIMAS BANDAS" = MÁS DÍAS DE MORA, no las primeras de la lista. Las bandas se
        // numeran desde 1 ASCENDIENDO en días (diaFin(k) = 30 * SUM(periodos 1..k), la última
        // es la abierta) — así que "las últimas" son las de más antigüedad, y "consumir desde
        // el final" en términos de CUOTAS significa empezar por la de vencimiento MÁS
        // ANTIGUO, no por el número de banda más alto directamente (eso es una consecuencia,
        // no el criterio de orden). Invertir este sentido no descuadra el asiento — la plata
        // simplemente queda acreditada a las cuentas equivocadas, sin que nadie lo note.
        if (capitalCondonado > 0.0) {
            Producto producto = prestamo.getProducto();
            if (producto == null) {
                throw new IncomeException("El préstamo " + prestamo.getCodigo()
                        + " no tiene producto asignado; no se puede clasificar el capital condonado por banda.");
            }
            Map<String, LineaBandaCondonada> bandas = new LinkedHashMap<>();
            List<DetallePrestamo> porAntiguedad = new ArrayList<>(pendientes);
            // Vencimiento más antiguo primero = más días de mora primero = "última banda" primero.
            porAntiguedad.sort((a, b) -> {
                if (a.getFechaVencimiento() == null) {
                    return 1;
                }
                if (b.getFechaVencimiento() == null) {
                    return -1;
                }
                return a.getFechaVencimiento().compareTo(b.getFechaVencimiento());
            });

            double restante = capitalCondonado;
            for (DetallePrestamo cuota : porAntiguedad) {
                if (restante <= TOLERANCIA) {
                    break;
                }
                double capitalCuota = nvl(cuota.getCapital());
                if (capitalCuota <= 0.0 || cuota.getFechaVencimiento() == null) {
                    continue;
                }
                double consumir = redondear(Math.min(restante, capitalCuota));
                if (consumir <= 0.0) {
                    continue;
                }
                LocalDate vencimiento = cuota.getFechaVencimiento().toLocalDate();
                long dias = Math.max(1, ChronoUnit.DAYS.between(vencimiento, fecha));
                ResultadoClasificacionBanda resultado = clasificadorBandaService.clasificar(
                        producto.getCodigo(), idEmpresa, (long) TipoCarteraBanda.VENCIDO, dias, fecha);
                BandaProductoDetalle banda = resultado.getBanda();
                String clave = producto.getCodigo() + "|" + banda.getNumero();
                LineaBandaCondonada acumulada = bandas.get(clave);
                if (acumulada == null) {
                    acumulada = new LineaBandaCondonada(banda, producto.getNombre());
                    bandas.put(clave, acumulada);
                }
                acumulada.valor += consumir;
                restante = redondear(restante - consumir);
            }
            if (restante > TOLERANCIA) {
                throw new IncomeException("El capital condonado del acuerdo " + acuerdo.getCodigo()
                        + " ($" + capitalCondonado + ") supera el capital pendiente total de las cuotas"
                        + " del préstamo " + prestamo.getCodigo() + " (quedaron $" + restante
                        + " sin poder asignar a ninguna cuota); no se puede armar el asiento de condonación.");
            }
            for (LineaBandaCondonada acumulada : bandas.values()) {
                double valor = redondear(acumulada.valor);
                if (valor <= 0.0) {
                    continue;
                }
                if (acumulada.banda.getIdPlanCuenta() == null) {
                    throw new IncomeException("La banda " + acumulada.banda.getNumero() + " de "
                            + acumulada.nombreProducto + " no tiene cuenta contable asignada en CRD.BNDP;"
                            + " no se puede armar el asiento de condonación del acuerdo " + acuerdo.getCodigo());
                }
                DetalleAsiento linea = new DetalleAsiento();
                linea.setNumeroCuenta(acumulada.banda.getCuentaContable());
                linea.setNombreCuenta(acumulada.banda.getNombreCuenta());
                linea.setDescripcion("Condonación acuerdo " + acuerdo.getCodigo() + " - capital - "
                        + acumulada.nombreProducto + " banda " + acumulada.banda.getNumero());
                linea.setValorDebe(0.0);
                linea.setValorHaber(valor);
                lineas.add(linea);
                totalHaber += valor;
            }
        }

        // H: interés condonado (ordinario + mora), por tipo de préstamo.
        if (interesCondonado > 0.0) {
            Long idTipoPrestamo = prestamo.getProducto() != null && prestamo.getProducto().getTipoPrestamo() != null
                    ? prestamo.getProducto().getTipoPrestamo().getCodigo() : null;
            if (idTipoPrestamo == null) {
                throw new IncomeException("El préstamo " + prestamo.getCodigo() + " no tiene tipo de préstamo"
                        + " asignado; no se puede resolver la cuenta de interés condonado.");
            }
            DetallePlantilla lineaInteres = detallePlantillaDaoService.selectByPlantillaYAuxiliares(
                    idPlantilla, CrdLineaAsiento.INTERES_ORDINARIO_POR_COBRAR, idTipoPrestamo.intValue());
            if (lineaInteres == null || lineaInteres.getPlanCuenta() == null) {
                throw new IncomeException("La plantilla alterno " + PlantillasCredito.COBRO_INDIVIDUAL_PRESTAMO
                        + " no tiene la línea de interés por cobrar para el tipo de préstamo " + idTipoPrestamo);
            }
            double valor = redondear(interesCondonado);
            DetalleAsiento linea = new DetalleAsiento();
            linea.setPlanCuenta(lineaInteres.getPlanCuenta());
            linea.setNumeroCuenta(lineaInteres.getPlanCuenta().getCuentaContable());
            linea.setNombreCuenta(lineaInteres.getPlanCuenta().getNombre());
            linea.setDescripcion("Condonación acuerdo " + acuerdo.getCodigo() + " - interés (ordinario y mora)");
            linea.setValorDebe(0.0);
            linea.setValorHaber(valor);
            lineas.add(linea);
            totalHaber += valor;
        }

        if (lineas.isEmpty()) {
            System.out.println("  AcuerdoCondonacionService.generarAsientoCondonacion - acuerdo "
                    + acuerdo.getCodigo() + " sin líneas de haber armables; no se genera asiento.");
            return;
        }

        // D: la línea de gasto, por el total exacto del haber — el asiento cuadra por
        // construcción (D = suma del H ya redondeado), sin ajuste de centavos.
        DetalleAsiento debe = new DetalleAsiento();
        debe.setPlanCuenta(lineaGasto.getPlanCuenta());
        debe.setNumeroCuenta(lineaGasto.getPlanCuenta().getCuentaContable());
        debe.setNombreCuenta(lineaGasto.getPlanCuenta().getNombre());
        debe.setDescripcion("Condonación acuerdo " + acuerdo.getCodigo() + " - gasto por condonación");
        debe.setValorDebe(redondear(totalHaber));
        debe.setValorHaber(0.0);

        List<DetalleAsiento> lineasFinal = new ArrayList<>();
        lineasFinal.add(debe);
        lineasFinal.addAll(lineas);

        Asiento asiento = asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS, fecha,
                "Condonación del acuerdo " + acuerdo.getCodigo() + " - préstamo " + prestamo.getCodigo(),
                acuerdo.getUsuarioRegistro(), lineasFinal, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        System.out.println("  💰 Asiento de condonación generado: " + asiento.getCodigo()
                + " - Acuerdo: " + acuerdo.getCodigo() + " - Total: $" + redondear(totalHaber));
    }

    /** Acumulador de capital condonado por banda, hasta armar la línea del asiento. */
    private static class LineaBandaCondonada {
        private final BandaProductoDetalle banda;
        private final String nombreProducto;
        private double valor;

        LineaBandaCondonada(BandaProductoDetalle banda, String nombreProducto) {
            this.banda = banda;
            this.nombreProducto = nombreProducto;
        }
    }

    private double valorConceptoPagado(List<DetalleAcuerdoCondonacion> detalles, long concepto) {
        for (DetalleAcuerdoCondonacion linea : detalles) {
            if (linea.getConcepto() != null && linea.getConcepto() == concepto) {
                return nvl(linea.getValorPagado());
            }
        }
        return 0.0;
    }

    private double valorConceptoCondonado(List<DetalleAcuerdoCondonacion> detalles, long concepto) {
        for (DetalleAcuerdoCondonacion linea : detalles) {
            if (linea.getConcepto() != null && linea.getConcepto() == concepto) {
                return nvl(linea.getValorCondonado());
            }
        }
        return 0.0;
    }

    private String concatObservacion(String observacion, String sufijo) {
        return (observacion != null && !observacion.trim().isEmpty() ? observacion + " " : "") + "[" + sufijo + "]";
    }

    // =====================================================================
    // Validaciones
    // =====================================================================

    private Prestamo validar(SolicitudRegistroAcuerdo solicitud) throws Throwable {
        if (solicitud == null) {
            throw new IncomeException("La solicitud de registro del acuerdo es obligatoria");
        }
        if (solicitud.getIdPrestamo() == null) {
            throw new IncomeException("idPrestamo es obligatorio");
        }
        Prestamo prestamo;
        try {
            prestamo = prestamoDaoService.selectById(solicitud.getIdPrestamo(), NombreEntidadesCredito.PRESTAMO);
        } catch (NoResultException e) {
            throw new IncomeException("No existe el préstamo " + solicitud.getIdPrestamo());
        }

        // K7: universo del acuerdo, decidido por PRSTIDST — nunca ESPSCDGO.
        Long idEstado = prestamo.getIdEstado();
        if (idEstado == null || (idEstado != EstadoPrestamo.EN_MORA
                && idEstado != EstadoPrestamo.DE_PLAZO_VENCIDO)) {
            throw new IncomeException("El préstamo " + solicitud.getIdPrestamo() + " está en estado "
                    + idEstado + "; un acuerdo de condonación solo aplica a préstamos EN_MORA ("
                    + EstadoPrestamo.EN_MORA + ") o DE_PLAZO_VENCIDO (" + EstadoPrestamo.DE_PLAZO_VENCIDO + ")");
        }

        if (solicitud.getFecha() == null) {
            throw new IncomeException("La fecha del acuerdo es obligatoria");
        }
        if (solicitud.getUsuario() == null || solicitud.getUsuario().trim().isEmpty()) {
            throw new IncomeException("usuario es obligatorio");
        }
        if (solicitud.getDetalles() == null || solicitud.getDetalles().size() != 5) {
            throw new IncomeException("El acuerdo debe traer exactamente los 5 conceptos del"
                    + " préstamo (Capital, Interés, Mora, Desgravamen, Seguro de incendio)");
        }

        Set<Long> conceptosVistos = new HashSet<>();
        for (DetalleConceptoAcuerdoDTO linea : solicitud.getDetalles()) {
            if (linea.getConcepto() == null || !esConceptoValido(linea.getConcepto())) {
                throw new IncomeException("concepto inválido: " + linea.getConcepto()
                        + "; debe ser uno de CrdConceptoPrestamo (1-5)");
            }
            if (!conceptosVistos.add(linea.getConcepto())) {
                throw new IncomeException("El concepto " + linea.getConcepto()
                        + " está repetido en el detalle del acuerdo");
            }
            if (linea.getValorAdeudado() == null || linea.getValorAdeudado() < 0
                    || linea.getValorPagado() == null || linea.getValorPagado() < 0
                    || linea.getValorCondonado() == null || linea.getValorCondonado() < 0) {
                throw new IncomeException("Los montos del concepto " + linea.getConcepto()
                        + " son obligatorios y no pueden ser negativos");
            }

            boolean esSeguro = linea.getConcepto() == CrdConceptoPrestamo.DESGRAVAMEN
                    || linea.getConcepto() == CrdConceptoPrestamo.SEGURO_INCENDIO;
            if (esSeguro && linea.getValorCondonado() > TOLERANCIA) {
                throw new IncomeException("El concepto " + linea.getConcepto()
                        + " (Desgravamen/Seguro de incendio) nunca se condona: se paga al 100% (K3)");
            }

            // K1: pago único, el préstamo queda liquidado en el acto — pagado + condonado
            // debe cubrir EXACTO lo adeudado de cada concepto, sin remanente.
            double diferencia = Math.abs((linea.getValorPagado() + linea.getValorCondonado())
                    - linea.getValorAdeudado());
            if (diferencia > TOLERANCIA) {
                throw new IncomeException("El concepto " + linea.getConcepto() + ": pagado ($"
                        + linea.getValorPagado() + ") + condonado ($" + linea.getValorCondonado()
                        + ") no cubre el adeudado ($" + linea.getValorAdeudado()
                        + "); el acuerdo debe liquidar el préstamo por completo, sin remanente");
            }
        }
        if (conceptosVistos.size() != 5) {
            throw new IncomeException("El acuerdo debe traer los 5 conceptos exactos"
                    + " (Capital, Interés, Mora, Desgravamen, Seguro de incendio), sin repetir ninguno");
        }

        // ⚠️ valorAdeudado NO es una decisión del operador, es un HECHO del préstamo — igual
        // que valorPagar/valorCondonar de la cabecera no son entrada porque son sumas. Lo
        // único que decide el operador es cómo repartir cada adeudado entre pagado y
        // condonado. Sin este chequeo, un cliente podría registrar un acuerdo con adeudados
        // inventados: pasaría la validación de arriba (coherencia interna) pero moriría
        // recién al procesar, en verificarVigencia — lejos de quien lo creó y sin que
        // entienda por qué. Y desde que se derogó K4 esto importa más que antes: la
        // previsualización ES el control, así que lo que el operador vio en pantalla y lo que
        // se guarda tienen que ser la MISMA fuente, no una copia sin verificar.
        String motivoStaleness = verificarAdeudados(prestamo.getCodigo(), solicitud.getFecha(),
                solicitud.getDetalles());
        if (motivoStaleness != null) {
            throw new IncomeException(motivoStaleness);
        }

        return prestamo;
    }

    /**
     * Compara los adeudados de una lista de líneas (del registro o del detalle ya guardado)
     * contra el desglose FRESCO del préstamo. Compartido por {@code validar} (al registrar) y
     * {@link #verificarVigencia} (al procesar) — misma comparación, dos momentos distintos,
     * nunca duplicada.
     *
     * @return {@code null} si todo coincide; el mensaje de la diferencia si no
     */
    private String verificarAdeudados(Long idPrestamo, LocalDate fecha, List<?> lineas) throws Throwable {
        DesgloseConceptosPrestamo actual = procesoPagoPrestamoService.calcularDesgloseConceptos(idPrestamo, fecha);
        for (Object linea : lineas) {
            Long concepto = linea instanceof DetalleConceptoAcuerdoDTO
                    ? ((DetalleConceptoAcuerdoDTO) linea).getConcepto()
                    : ((DetalleAcuerdoCondonacion) linea).getConcepto();
            Double valorAdeudado = linea instanceof DetalleConceptoAcuerdoDTO
                    ? ((DetalleConceptoAcuerdoDTO) linea).getValorAdeudado()
                    : ((DetalleAcuerdoCondonacion) linea).getValorAdeudado();
            double adeudadoActual = valorPorConcepto(actual, concepto);
            double diferencia = Math.abs(adeudadoActual - nvl(valorAdeudado));
            if (diferencia > TOLERANCIA) {
                return "Concepto " + nombreConcepto(concepto) + " del préstamo " + idPrestamo
                        + ": adeudado informado $" + valorAdeudado + ", adeudado real $" + adeudadoActual
                        + ". Verifique y vuelva a registrar el acuerdo.";
            }
        }
        return null;
    }

    private boolean esConceptoValido(Long concepto) {
        return concepto == CrdConceptoPrestamo.CAPITAL || concepto == CrdConceptoPrestamo.INTERES
                || concepto == CrdConceptoPrestamo.MORA || concepto == CrdConceptoPrestamo.DESGRAVAMEN
                || concepto == CrdConceptoPrestamo.SEGURO_INCENDIO;
    }

    private AcuerdoCondonacion buscarAcuerdo(Long idAcuerdo) throws Throwable {
        if (idAcuerdo == null) {
            throw new IncomeException("idAcuerdo es obligatorio");
        }
        try {
            return acuerdoCondonacionDaoService.selectById(idAcuerdo, NombreEntidadesCredito.ACUERDO_CONDONACION);
        } catch (NoResultException e) {
            throw new IncomeException("No existe el acuerdo " + idAcuerdo);
        }
    }

    private String textoEstado(Long estado) {
        if (estado == null) {
            return "desconocido";
        }
        if (estado == CrdEstadoAcuerdoCondonacion.VIGENTE) {
            return "VIGENTE";
        }
        if (estado == CrdEstadoAcuerdoCondonacion.APLICADO) {
            return "APLICADO";
        }
        if (estado == CrdEstadoAcuerdoCondonacion.ANULADO) {
            return "ANULADO";
        }
        return String.valueOf(estado);
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}

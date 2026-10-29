package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.saa.basico.ejb.FechaService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.HistDetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.AbonoCapitalPrestamoService;
import com.saa.ejb.crd.service.ContabilidadPrestamoService;
import com.saa.ejb.crd.service.DetallePrestamoService;
import com.saa.ejb.crd.service.EventoPrestamoService;
import com.saa.ejb.crd.service.MotorPagoPrestamoService;
import com.saa.ejb.crd.service.PagoPrestamoService;
import com.saa.ejb.crd.service.PrestamoService;
import com.saa.ejb.crd.service.ProcesoMoraPrestamoService;
import com.saa.ejb.crd.service.ProcesoPagoPrestamoService;
import com.saa.ejb.crd.service.dto.CuotaProyectada;
import com.saa.ejb.crd.service.dto.DesgloseAporte;
import com.saa.ejb.crd.service.dto.MovimientoAporte;
import com.saa.ejb.crd.service.dto.ResultadoAbonoCapital;
import com.saa.ejb.crd.service.dto.ResultadoAbonoCapitalConAportes;
import com.saa.ejb.crd.service.dto.SimulacionAbonoCapital;
import com.saa.ejb.crd.service.dto.SolicitudAbonoCapital;
import com.saa.ejb.crd.service.dto.SolicitudAbonoCapitalConAportes;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.EventoPrestamo;
import com.saa.model.crd.HistDetallePrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoPrestamo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Implementación del abono a capital con re-amortización (§7.3).
 *
 * @author Sistema SAA
 * @since 2026-08-14
 */
@Stateless
public class AbonoCapitalPrestamoServiceImpl implements AbonoCapitalPrestamoService {

    private static final double TOLERANCIA = 0.01;

    /** Tope de seguridad al calcular el plazo nuevo de la modalidad 1 */
    private static final int MAX_CUOTAS_NUEVAS = 600;

    @EJB
    private MotorPagoPrestamoService motorPagoPrestamoService;

    /** C17: mes cerrado para cartera / contabilidad (CIERRES-BLOQUEANTES.md). */
    @jakarta.ejb.EJB
    private com.saa.ejb.cnt.service.EstadoCierreService estadoCierreService;

    @EJB
    private ProcesoPagoPrestamoService procesoPagoPrestamoService;

    /** Pedido 10: un abono a capital es un abono manual y también regulariza EN_MORA. */
    @EJB
    private ProcesoMoraPrestamoService procesoMoraPrestamoService;

    @EJB
    private EventoPrestamoService eventoPrestamoService;

    @EJB
    private ContabilidadPrestamoService contabilidadPrestamoService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private PrestamoService prestamoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private DetallePrestamoService detallePrestamoService;

    @EJB
    private HistDetallePrestamoDaoService histDetallePrestamoDaoService;

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private PagoPrestamoService pagoPrestamoService;

    @EJB
    private FechaService fechaService;

    /** S13 — re-reparto del seguro de una póliza viva entre las cuotas nuevas del abono. */
    @EJB
    private com.saa.ejb.crd.dao.CuotaSeguroDaoService cuotaSeguroDaoService;

    @EJB
    private com.saa.ejb.crd.service.DocumentoSeguroService documentoSeguroService;

    // ========================================================================
    // Simulación
    // ========================================================================

    @Override
    public SimulacionAbonoCapital simular(Long idPrestamo, double valor, int modalidad) throws Throwable {
        return simular(idPrestamo, valor, modalidad, null, false);
    }

    @Override
    public SimulacionAbonoCapital simular(Long idPrestamo, double valor, int modalidad, LocalDate fecha,
            boolean reproceso) throws Throwable {
        System.out.println("AbonoCapitalPrestamoService.simular - Préstamo: " + idPrestamo
            + " - Valor: $" + valor + " - Modalidad: " + modalidad
            + (fecha != null ? " - Fecha: " + fecha : "") + (reproceso ? " - RE-PROCESO (D-048)" : ""));

        // Bug corregido 2026-08-28: simular() es de solo lectura y NO debe autocorregir/persistir
        // cuotas (ver javadoc de MotorPagoPrestamoService.calcularSaldosCuota).
        CalculoAbono calculo = calcular(idPrestamo, valor, modalidad, fecha != null ? fecha : LocalDate.now(), true,
            reproceso);

        SimulacionAbonoCapital simulacion = new SimulacionAbonoCapital();
        simulacion.setIdPrestamo(idPrestamo);
        simulacion.setSaldoCapitalActual(calculo.saldoCapitalPendiente);
        simulacion.setValorAbono(calculo.valorAbono);
        simulacion.setModalidad(modalidad);
        simulacion.setTipoAmortizacion(calculo.tipoAmortizacion);
        simulacion.setPlazoActual(calculo.prestamo.getPlazo());
        simulacion.setPlazoNuevo(calculo.plazoNuevo);
        simulacion.setCuotaActual(calculo.cuotaVigente);
        simulacion.setCuotaNueva(calculo.cuotaNueva);
        simulacion.setAhorroIntereses(calculo.ahorroIntereses);
        simulacion.setCuotasAHistorizar(calculo.cuotasAHistorizar.size());
        simulacion.setSeguroIncendioLiberado(calculo.seguroIncendioLiberado);
        simulacion.setTablaProyectada(calculo.tabla);
        return simulacion;
    }

    @Override
    public String motivoAbonoInviable(Long idPrestamo, double valorTotal, int modalidad) {
        return motivoAbonoInviable(idPrestamo, valorTotal, modalidad, null, false);
    }

    @Override
    public String motivoAbonoInviable(Long idPrestamo, double valorTotal, int modalidad, LocalDate fecha,
            boolean reproceso) {
        try {
            // Misma ruta de solo lectura que simular(), por self-call: no cruza el proxy EJB.
            calcular(idPrestamo, redondear(valorTotal), modalidad, fecha != null ? fecha : LocalDate.now(), true,
                reproceso);
            return null;
        } catch (IncomeException e) {
            return e.getMessage();
        } catch (Throwable e) {
            return "No se pudo simular el abono: " + e.getMessage();
        }
    }

    // ========================================================================
    // Aplicación
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoAbonoCapital aplicar(SolicitudAbonoCapital solicitud) throws Throwable {
        return aplicarConMovimientos(solicitud).resultado;
    }

    /** Resultado interno de {@link #aplicarConMovimientos}: el abono + los aportes consumidos. */
    private static final class AplicacionAbono {
        private ResultadoAbonoCapital resultado;
        private List<MovimientoAporte> movimientos = new ArrayList<>();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoAbonoCapitalConAportes abonarConAportes(SolicitudAbonoCapitalConAportes solicitud)
            throws Throwable {
        System.out.println("AbonoCapitalPrestamoService.abonarConAportes - Préstamo: "
            + (solicitud != null ? solicitud.getIdPrestamo() : null));

        if (solicitud == null) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PARAMETRO_INVALIDO
                + ": no se recibió el cuerpo de la solicitud");
        }
        if (solicitud.getIdEmpresa() == null) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PARAMETRO_INVALIDO
                + ": idEmpresa es obligatorio: es la empresa contable sobre la que se genera el asiento");
        }
        if (solicitud.getIdPrestamo() == null) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PARAMETRO_INVALIDO
                + ": idPrestamo es obligatorio");
        }
        boolean sinRegenerar = Boolean.FALSE.equals(solicitud.getRegenerarTabla());
        if (!sinRegenerar
                && (solicitud.getModalidad() == null || (solicitud.getModalidad() != 1 && solicitud.getModalidad() != 2))) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PARAMETRO_INVALIDO
                + ": modalidad es obligatoria y debe ser 1 (reduce plazo) o 2 (reduce cuota)");
        }
        if (solicitud.getUsuario() == null || solicitud.getUsuario().trim().isEmpty()) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PARAMETRO_INVALIDO
                + ": usuario es obligatorio");
        }

        Prestamo prestamo;
        try {
            prestamo = prestamoDaoService.selectById(solicitud.getIdPrestamo(), NombreEntidadesCredito.PRESTAMO);
        } catch (jakarta.persistence.NoResultException e) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PRESTAMO_NO_ENCONTRADO
                + ": no existe el préstamo " + solicitud.getIdPrestamo());
        }
        if (prestamo.getEntidad() == null || prestamo.getEntidad().getCodigo() == null) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PARAMETRO_INVALIDO + ": el préstamo "
                + prestamo.getCodigo() + " no tiene partícipe asociado; no se puede abonar con aportes");
        }

        // El abono ES Σ aportes; validarDesgloseAportes devuelve ese total (no vacío, > 0, sin
        // tipos repetidos, vigentes y con saldo).
        double total = procesoPagoPrestamoService.validarDesgloseAportes(solicitud.getAportes(),
            prestamo.getEntidad());

        LocalDate fecha = solicitud.getFecha() != null ? solicitud.getFecha() : LocalDate.now();
        // Mismas reglas que la precancelación directa (API-FECHA-AFECTACION-COBRO.md §2bis):
        // obligatoria, no menor a la fecha de pago, no futura, período abierto. Se reutiliza la
        // validación compartida, no se reescribe.
        procesoPagoPrestamoService.validarFechaAfectacion(solicitud.getFechaAfectacion(), fecha,
            solicitud.getIdEmpresa());

        SolicitudAbonoCapital interna = new SolicitudAbonoCapital();
        interna.setIdPrestamo(solicitud.getIdPrestamo());
        interna.setValor(total);
        interna.setModalidad(sinRegenerar ? null : solicitud.getModalidad());
        interna.setRegenerarTabla(solicitud.getRegenerarTabla());
        interna.setNumeroCuota(solicitud.getNumeroCuota());
        interna.setUsuario(solicitud.getUsuario());
        interna.setObservacion(solicitud.getObservacion());
        interna.setFecha(fecha);
        interna.setRutaDocumentoRespaldo(solicitud.getRutaDocumentoRespaldo());
        interna.setIdEmpresa(solicitud.getIdEmpresa());
        interna.setAportes(solicitud.getAportes());
        // Llamada directa: sin cobro. Con esto aplicarConMovimientos genera el asiento ÚNICO (AA6).
        interna.setIdCobroCredito(null);
        interna.setFechaAfectacion(solicitud.getFechaAfectacion());
        // D-048: en re-proceso, "al día" a la fecha del abono.
        interna.setReproceso(solicitud.getReproceso());

        AplicacionAbono aplicacion = aplicarConMovimientos(interna);

        ResultadoAbonoCapitalConAportes respuesta = new ResultadoAbonoCapitalConAportes();
        respuesta.setResultado(aplicacion.resultado);
        respuesta.setMovimientosAporte(aplicacion.movimientos);
        return respuesta;
    }

    private AplicacionAbono aplicarConMovimientos(SolicitudAbonoCapital solicitud) throws Throwable {
        System.out.println("AbonoCapitalPrestamoService.aplicar - Préstamo: "
            + (solicitud != null ? solicitud.getIdPrestamo() : null));

        if (solicitud == null) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PARAMETRO_INVALIDO
                + ": no se recibió el cuerpo de la solicitud");
        }
        if (solicitud.getIdPrestamo() == null) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PARAMETRO_INVALIDO
                + ": idPrestamo es obligatorio");
        }
        if (solicitud.getUsuario() == null || solicitud.getUsuario().trim().isEmpty()) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PARAMETRO_INVALIDO
                + ": usuario es obligatorio");
        }
        if (solicitud.getModalidad() == null && !solicitud.esSinRegenerarTabla()) {
            throw new IncomeException(ERR_MODALIDAD_INVALIDA + ": debe indicar la modalidad (1 o 2)");
        }
        if (solicitud.getIdEmpresa() == null) {
            throw new IncomeException("idEmpresa es obligatorio: es la empresa contable sobre la que"
                + " se genera el asiento de la operación.");
        }

        LocalDate fecha = solicitud.getFecha() != null ? solicitud.getFecha() : LocalDate.now();
        if (fecha.isAfter(LocalDate.now())) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_FECHA_INVALIDA
                + ": la fecha " + fecha + " es futura");
        }
        LocalDateTime fechaHora = fecha.isEqual(LocalDate.now()) ? LocalDateTime.now() : fecha.atStartOfDay();

        // C17: el mes de la fecha de afectación (o de la fecha, si no viene) tiene que estar abierto para
        // cartera y para contabilidad. Antes de calcular y de grabar el evento.
        estadoCierreService.validarMovimientoCartera(solicitud.getIdEmpresa(),
                solicitud.getFechaAfectacion() != null ? solicitud.getFechaAfectacion() : fecha,
                "abonos a capital");

        double valor = redondear(solicitud.getValor() != null ? solicitud.getValor() : 0.0);

        // Abono con aportes (2026-10-12, AA1/AA4): con aportes, valor es el abono TOTAL
        // (depósito + Σ aportes) y los aportes no pueden exceder ese total. La validación de
        // fondo (tipo vigente, saldo) la hace consumirAportes dentro de la transacción.
        List<DesgloseAporte> aportes = solicitud.getAportes();
        boolean conAportes = aportes != null && !aportes.isEmpty();
        if (conAportes) {
            double sumaAportes = 0.0;
            for (DesgloseAporte renglon : aportes) {
                sumaAportes += renglon != null && renglon.getValor() != null ? renglon.getValor() : 0.0;
            }
            if (redondear(sumaAportes) > valor + TOLERANCIA) {
                throw new IncomeException(ProcesoPagoPrestamoService.ERR_DESGLOSE_INVALIDO
                    + ": los aportes ($" + redondear(sumaAportes) + ") exceden el valor total del abono ($"
                    + valor + ")");
            }
        }

        // C16b (D-030): la tabla ya refleja el abono — se registra sin re-amortizar.
        if (solicitud.esSinRegenerarTabla()) {
            return aplicarSinRegenerarTabla(solicitud, valor, fecha, fechaHora, aportes, conAportes);
        }

        // D-048: en re-proceso "al día" se evalúa a la fecha del abono (la de pago; si no viene, la de
        // afectación). Fuera de re-proceso, `fecha` de siempre (la del abono o hoy).
        LocalDate fechaAlDia = fecha;
        if (solicitud.esReproceso() && solicitud.getFecha() == null && solicitud.getFechaAfectacion() != null) {
            fechaAlDia = solicitud.getFechaAfectacion();
        }
        CalculoAbono calculo = calcular(solicitud.getIdPrestamo(), valor, solicitud.getModalidad(), fechaAlDia, false,
            solicitud.esReproceso());
        Prestamo prestamo = calculo.prestamo;

        // Ninguna cuota a historizar puede tener PagoPrestamo: el DELETE violaría FK_PGPR_DTPR.
        // D-048: en re-proceso, las cuotas con pagos anticipados (fecha <= abono) no se borran: se
        // actualizan en el lugar (calculo.cuotasReutilizadas), así que no entran en esta regla.
        for (DetallePrestamo cuota : calculo.cuotasAHistorizar) {
            if (esReutilizada(calculo, cuota)) {
                continue;
            }
            List<PagoPrestamo> pagos = pagoPrestamoDaoService.selectByIdDetallePrestamo(cuota.getCodigo());
            if (pagos != null && !pagos.isEmpty()) {
                throw new IncomeException(ERR_PRESTAMO_NO_AL_DIA + ": la cuota #" + cuota.getNumeroCuota()
                    + " tiene pagos registrados (incluso anulados) y no puede re-amortizarse;"
                    + " regularice el préstamo antes de abonar a capital");
            }
        }

        // 1. Evento
        EventoPrestamo evento = new EventoPrestamo();
        evento.setPrestamo(prestamo);
        evento.setTipoOperacion(ProcesoPagoPrestamoService.TIPO_ABONO_CAPITAL);
        evento.setValor(valor);
        evento.setModalidad(Long.valueOf(solicitud.getModalidad().longValue()));
        evento.setPlazoAnterior(prestamo.getPlazo());
        evento.setCuotaAnterior(calculo.cuotaVigente);
        evento.setFecha(fechaHora);
        evento.setObservacion(solicitud.getObservacion());
        evento.setUsuario(solicitud.getUsuario());
        evento.setFechaRegistro(LocalDateTime.now());
        evento.setEstado(1L);
        evento = eventoPrestamoService.saveSingle(evento);
        System.out.println("  💾 EventoPrestamo creado: " + evento.getCodigo() + " (ABONO_CAPITAL)");

        // 2. Historizar las cuotas pendientes en CRD.HDTP y borrarlas de CRD.DTPR
        int historizadas = 0;
        for (DetallePrestamo cuota : calculo.cuotasAHistorizar) {
            HistDetallePrestamo historico = copiarAHistorico(cuota, evento,
                ProcesoPagoPrestamoService.TIPO_ABONO_CAPITAL, solicitud.getUsuario());
            histDetallePrestamoDaoService.save(historico, null);
            historizadas++;
        }
        for (DetallePrestamo cuota : calculo.cuotasAHistorizar) {
            if (esReutilizada(calculo, cuota)) {
                continue;
            }
            detallePrestamoDaoService.remove(cuota, cuota.getCodigo());
        }
        System.out.println("  📦 Cuotas historizadas: " + historizadas + " (borradas de DTPR: "
            + (historizadas - calculo.cuotasReutilizadas.size()) + ", actualizadas en el lugar por D-048: "
            + calculo.cuotasReutilizadas.size() + ")");

        // 3. Generar la nueva tabla
        List<DetallePrestamo> nuevas = new ArrayList<>();
        for (CuotaProyectada proyectada : calculo.tabla) {
            Long numero = Math.round(proyectada.getNumeroCuota());
            DetallePrestamo reutilizada = calculo.cuotasReutilizadas.get(numero);
            DetallePrestamo cuota;
            if (reutilizada != null) {
                cuota = reutilizarCuota(reutilizada, prestamo, proyectada, calculo,
                    calculo.pagosReutilizadas.get(numero));
            } else {
                cuota = construirCuota(prestamo, proyectada, calculo);
                cuota = detallePrestamoService.saveSingle(cuota);
            }
            nuevas.add(cuota);
        }
        System.out.println("  📄 Cuotas nuevas generadas: " + nuevas.size());

        // 3bis. S13 — cerrar los PSCT vigentes de la póliza sobre las cuotas que se acaban de
        // borrar y abrir unos nuevos sobre las cuotas nuevas, con el valor re-repartido. Vacío
        // si aplicarReRepartoPolizaViva (dentro de calcular(), más arriba) no encontró ninguna
        // póliza viva — comportamiento idéntico al de antes de S13.
        if (!calculo.reRepartosAplicados.isEmpty()) {
            Map<Long, DetallePrestamo> nuevasPorNumero = new java.util.HashMap<>();
            for (DetallePrestamo nueva : nuevas) {
                if (nueva.getNumeroCuota() != null) {
                    nuevasPorNumero.put(Math.round(nueva.getNumeroCuota()), nueva);
                }
            }
            for (ReRepartoPoliza r : calculo.reRepartosAplicados) {
                for (com.saa.model.crd.CuotaSeguro psctVieja : cuotaSeguroDaoService.selectByCuotas(r.idsCuotaVieja)) {
                    if (psctVieja.getFechaReverso() == null && psctVieja.getCampo() != null
                            && psctVieja.getCampo() == r.campo) {
                        psctVieja.setFechaReverso(LocalDateTime.now());
                        cuotaSeguroDaoService.save(psctVieja, psctVieja.getCodigo());
                    }
                }
                for (Map.Entry<Long, Double> entrada : r.montoRerepartido.entrySet()) {
                    DetallePrestamo nueva = nuevasPorNumero.get(entrada.getKey());
                    if (nueva == null) {
                        continue;
                    }
                    com.saa.model.crd.CuotaSeguro psctNueva = new com.saa.model.crd.CuotaSeguro();
                    psctNueva.setDocumento(r.documento);
                    psctNueva.setPrestamoSeguro(r.prestamoSeguro);
                    psctNueva.setIdCuota(nueva.getCodigo());
                    psctNueva.setCampo(r.campo);
                    psctNueva.setSaldoInicialCapital(nvl(nueva.getSaldoInicialCapital()));
                    psctNueva.setValorAnterior(r.valorQueElAbonoHabriaPuesto.get(entrada.getKey()));
                    psctNueva.setValorNuevo(entrada.getValue());
                    cuotaSeguroDaoService.save(psctNueva, null);
                }
            }
            System.out.println("  🔁 S13 - PSCT re-repartidos sobre " + calculo.reRepartosAplicados.size()
                + " póliza(s)/campo(s)");
        }

        // 4. Cuota ANCLA donde se acumula el abono en DTPRSLOT
        DetallePrestamo ancla = detallePrestamoDaoService.selectUltimaCuotaPagada(prestamo.getCodigo());
        if (solicitud.esReproceso() && ancla != null && esReutilizada(calculo, ancla)) {
            // D-048: una cuota futura que los pagos anticipados dejaron PAGADA no es el ancla del
            // abono; el ancla es la última cuota pagada que no se re-amortizó.
            ancla = ultimaPagadaNoReutilizada(prestamo.getCodigo(), calculo);
        }
        if (ancla == null) {
            // Préstamo sin cuotas pagadas: el abono se registra en la PRIMERA cuota nueva
            ancla = nuevas.isEmpty() ? null : nuevas.get(0);
        }
        if (ancla == null) {
            throw new IncomeException(ERR_SIN_CUOTAS_PENDIENTES
                + ": no hay ninguna cuota donde registrar el abono");
        }
        // ACUMULATIVO: nunca se pisa un pago extra previo
        ancla.setSaldoOtros(redondear(nvl(ancla.getSaldoOtros()) + valor));
        ancla = detallePrestamoService.saveSingle(ancla);
        System.out.println("  ⚓ Abono acumulado en DTPRSLOT de la cuota #" + ancla.getNumeroCuota()
            + " (" + ancla.getCodigo() + "): $" + ancla.getSaldoOtros());

        // 5. PagoPrestamo del abono: el monto va en saldoOtros, NO en capitalPagado
        PagoPrestamo pago = new PagoPrestamo();
        pago.setPrestamo(prestamo);
        pago.setDetallePrestamo(ancla);
        pago.setNumeroCuota(ancla.getNumeroCuota());
        pago.setFecha(fechaHora);
        pago.setValor(valor);
        pago.setSaldoOtros(valor);
        pago.setCapitalPagado(0.0);
        pago.setInteresPagado(0.0);
        pago.setMoraPagada(0.0);
        pago.setInteresVencidoPagado(0.0);
        pago.setDesgravamen(0.0);
        pago.setValorSeguroIncendio(0.0);
        pago.setTipo(ProcesoPagoPrestamoService.TIPO_ABONO_CAPITAL);
        pago.setObservacion((solicitud.getObservacion() != null ? solicitud.getObservacion() : "")
            + " [Evento: " + evento.getCodigo() + "]");
        pago.setUsuarioRegistro(solicitud.getUsuario());
        pago.setFechaRegistro(LocalDateTime.now());
        pago.setEstado(1L);
        pago.setIdEstado(1L);
        pago.setAnulado(0L);
        pago.setEventoPrestamo(evento);
        pago.setRutaDocumentoRespaldo(solicitud.getRutaDocumentoRespaldo());
        pago = pagoPrestamoService.saveSingle(pago);

        // 5bis. Abono con aportes: consume el saldo del partícipe y enlaza cada PagoAporte a ESTE
        // PagoPrestamo (AA7) — así anularOperacion → revertirAportes lo restituye sin tocar la
        // anulación. Mismo patrón que precancelar (paso 6).
        List<MovimientoAporte> movimientosAporte = new ArrayList<>();
        if (conAportes) {
            if (prestamo.getEntidad() == null || prestamo.getEntidad().getCodigo() == null) {
                throw new IncomeException(ProcesoPagoPrestamoService.ERR_PARAMETRO_INVALIDO + ": el préstamo "
                    + prestamo.getCodigo() + " no tiene partícipe asociado; no se puede abonar con aportes");
            }
            String glosa = "ABONO CAPITAL " + prestamo.getCodigo() + " - Evento " + evento.getCodigo();
            movimientosAporte = procesoPagoPrestamoService.consumirAportes(prestamo.getEntidad(), aportes,
                fechaHora, solicitud.getUsuario(), glosa, pago, solicitud.getRutaDocumentoRespaldo());
        }

        // 6. Actualizar el préstamo
        if (solicitud.getModalidad() == 1) {
            prestamo.setPlazo(calculo.plazoNuevo);
        }
        prestamoDaoService.save(prestamo, prestamo.getCodigo());

        // Recalcula totales/fechaFin/tasas desde la tabla VIVA (paso 7 de §7.3)
        prestamo = prestamoService.actualizarCamposDesdeTabla(prestamo.getCodigo());
        // actualizarCamposPrestamo toma el valorCuota de la PRIMERA cuota de la tabla, que tras
        // la re-amortización es una cuota vieja ya pagada. El valor vigente es el de la tabla nueva.
        prestamo.setValorCuota(calculo.cuotaNueva);
        if (solicitud.getModalidad() == 1) {
            prestamo.setPlazo(calculo.plazoNuevo);
        }

        procesoPagoPrestamoService.registrarHuellaPrestamo(prestamo,
            ProcesoPagoPrestamoService.TIPO_ABONO_CAPITAL, valor, solicitud.getObservacion(),
            fechaHora, solicitud.getUsuario());

        // 7. Cerrar el evento con los valores nuevos
        evento.setPlazoNuevo(calculo.plazoNuevo);
        evento.setCuotaNueva(calculo.cuotaNueva);
        evento = eventoPrestamoService.saveSingle(evento);

        // 8. Hook contable — re-bandeo (2026-08-31, Fase 3)
        // AA6: llamada DIRECTA con aportes (sin cobro CBCR) → UN solo asiento que junta el cruce
        // de aportes, el cierre de apertura y el re-bandeo, EN LUGAR del hook de siempre (dos
        // asientos dejarían uno vivo al anular, que solo reversa EVPR.numeroAsiento). Con cobro,
        // el cruce y el cierre los genera CBCRASN2 y este hook solo hace el re-bandeo, como hoy.
        Long numeroAsiento;
        if (conAportes && solicitud.getIdCobroCredito() == null) {
            numeroAsiento = contabilidadPrestamoService.contabilizarAbonoCapitalConAportes(evento,
                movimientosAporte, solicitud.getIdEmpresa(),
                solicitud.getFechaAfectacion() != null ? solicitud.getFechaAfectacion() : fecha);
        } else {
            numeroAsiento = contabilidadPrestamoService.contabilizarAbonoCapital(evento, solicitud.getIdEmpresa());
        }
        if (numeroAsiento != null) {
            evento.setNumeroAsiento(numeroAsiento);
            eventoPrestamoService.saveSingle(evento);
            pago.setAsiento(numeroAsiento);
            pagoPrestamoService.saveSingle(pago);
        }

        // Pedido 10: si el abono dejó al préstamo sin cuotas vencidas y estaba EN_MORA(11),
        // vuelve a VIGENTE(2) de inmediato. En la práctica esta re-amortización ya exige
        // (validación 3, más arriba) que no existan cuotas vencidas ni PARCIAL para llegar
        // hasta aquí, pero se deja el mismo resguardo que en el resto de rutas de pago.
        procesoMoraPrestamoService.regularizarPrestamoSiSinMora(prestamo.getCodigo());

        ResultadoAbonoCapital resultado = new ResultadoAbonoCapital();
        resultado.setIdPrestamo(prestamo.getCodigo());
        resultado.setIdEvento(evento.getCodigo());
        resultado.setIdPagoPrestamo(pago.getCodigo());
        resultado.setIdCuotaConSaldoOtros(ancla.getCodigo());
        resultado.setValorAbono(valor);
        resultado.setModalidad(solicitud.getModalidad());
        resultado.setPlazoAnterior(evento.getPlazoAnterior());
        resultado.setPlazoNuevo(calculo.plazoNuevo);
        resultado.setCuotaAnterior(calculo.cuotaVigente);
        resultado.setCuotaNueva(calculo.cuotaNueva);
        resultado.setCuotasHistorizadas(historizadas);
        resultado.setCuotasGeneradas(nuevas.size());
        resultado.setSeguroIncendioLiberado(calculo.seguroIncendioLiberado);

        System.out.println("  ✅ Abono a capital aplicado - Evento: " + evento.getCodigo()
            + " - Plazo: " + evento.getPlazoAnterior() + " → " + calculo.plazoNuevo
            + " - Cuota: " + calculo.cuotaVigente + " → " + calculo.cuotaNueva);

        AplicacionAbono aplicacion = new AplicacionAbono();
        aplicacion.resultado = resultado;
        aplicacion.movimientos = movimientosAporte;
        return aplicacion;
    }

    // ========================================================================
    // C16b — abono sobre una tabla que YA lo refleja (sin regenerar), D-030 2026-10-29
    // ========================================================================

    /** Cuota donde se registra el abono sin regenerar tabla y el cuadre de la tabla con él. */
    private static final class CuadreSinRegenerar {
        private Prestamo prestamo;
        private DetallePrestamo cuota;
        private double capitalTabla;
        private double abonosPrevios;
        private double monto;
        private double totalDespues;
    }

    @Override
    public String motivoAbonoSinRegenerarInviable(Long idPrestamo, double valorTotal, Integer numeroCuota) {
        try {
            cuadreSinRegenerar(idPrestamo, redondear(valorTotal), numeroCuota, LocalDate.now());
            return null;
        } catch (IncomeException e) {
            return e.getMessage();
        } catch (Throwable e) {
            return "No se pudo validar el abono sin regenerar tabla: " + e.getMessage();
        }
    }

    /**
     * SOLO LECTURA. Ubica la cuota del abono y valida que, con él, la tabla cuadre con el monto:
     * {@code Σ DTPRCPTL (todas las cuotas) + Σ DTPRSLOT (abonos ya registrados) + valor = PRSTMNSL}.
     * La cuota es {@code numeroCuota} si viene; si no, la "vigente" a la fecha del abono: la de mayor
     * vencimiento {@code <=} fecha (el periodo en curso; no depende del estado de HOY de las cuotas,
     * para que un abono de un mes pasado caiga en su cuota de entonces); sin ninguna, la primera.
     */
    private CuadreSinRegenerar cuadreSinRegenerar(Long idPrestamo, double valor, Integer numeroCuota,
            LocalDate fecha) throws Throwable {
        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
        if (prestamo == null) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PRESTAMO_NO_ENCONTRADO
                + ": no existe el préstamo " + idPrestamo);
        }
        if (esEstadoTerminalPrestamo(prestamo.getIdEstado())) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_ESTADO_NO_PERMITE
                + ": el préstamo " + idPrestamo + " está en estado " + textoEstadoTerminal(prestamo.getIdEstado())
                + " (terminal) y no admite abonos");
        }
        if (valor <= 0.0) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_VALOR_INVALIDO
                + ": el valor del abono debe ser mayor a cero");
        }
        if (prestamo.getMontoSolicitado() == null || prestamo.getMontoSolicitado() <= 0.0) {
            throw new IncomeException(ERR_ABONO_NO_CUADRA_CON_TABLA + ": el préstamo " + idPrestamo
                + " no tiene monto (PRSTMNSL); no se puede validar que la tabla cuadre con el abono");
        }
        List<DetallePrestamo> cuotas = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
        if (cuotas == null || cuotas.isEmpty()) {
            throw new IncomeException(ERR_SIN_CUOTAS_PENDIENTES + ": el préstamo " + idPrestamo
                + " no tiene tabla de amortización");
        }

        CuadreSinRegenerar cuadre = new CuadreSinRegenerar();
        cuadre.prestamo = prestamo;
        DetallePrestamo vigente = null;
        DetallePrestamo primera = null;
        for (DetallePrestamo cuota : cuotas) {
            cuadre.capitalTabla += nvl(cuota.getCapital());
            cuadre.abonosPrevios += nvl(cuota.getSaldoOtros());
            if (cuota.getNumeroCuota() == null) {
                continue;
            }
            if (numeroCuota != null && Math.round(cuota.getNumeroCuota()) == numeroCuota.longValue()) {
                cuadre.cuota = cuota;
            }
            if (primera == null || cuota.getNumeroCuota() < primera.getNumeroCuota()) {
                primera = cuota;
            }
            if (cuota.getFechaVencimiento() != null && !cuota.getFechaVencimiento().toLocalDate().isAfter(fecha)
                    && (vigente == null || cuota.getFechaVencimiento().isAfter(vigente.getFechaVencimiento()))) {
                vigente = cuota;
            }
        }
        if (numeroCuota != null && cuadre.cuota == null) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PARAMETRO_INVALIDO + ": el préstamo "
                + idPrestamo + " no tiene la cuota #" + numeroCuota + " en su tabla (CRD.DTPR)");
        }
        if (cuadre.cuota == null) {
            cuadre.cuota = vigente != null ? vigente : primera;
        }
        cuadre.capitalTabla = redondear(cuadre.capitalTabla);
        cuadre.abonosPrevios = redondear(cuadre.abonosPrevios);
        cuadre.monto = redondear(prestamo.getMontoSolicitado());
        cuadre.totalDespues = redondear(cuadre.capitalTabla + cuadre.abonosPrevios + valor);
        double diferencia = redondear(cuadre.monto - cuadre.totalDespues);
        if (Math.abs(diferencia) > TOLERANCIA) {
            throw new IncomeException(ERR_ABONO_NO_CUADRA_CON_TABLA + ": el abono sin regenerar tabla de $"
                + valor + " no cuadra con la tabla del préstamo " + idPrestamo + ": capital de la tabla $"
                + cuadre.capitalTabla + " + abonos ya registrados (DTPRSLOT) $" + cuadre.abonosPrevios
                + " + este abono $" + valor + " = $" + cuadre.totalDespues + ", y el monto del préstamo es $"
                + cuadre.monto + " (" + (diferencia > 0 ? "faltan $" + diferencia : "sobran $" + (-diferencia))
                + "). La tabla vigente no refleja exactamente este abono: corrija la tabla o el valor,"
                + " o use el abono con re-amortización.");
        }
        return cuadre;
    }

    /**
     * C16b: registra el abono sobre la tabla vigente, SIN historizar ni regenerar cuotas: evento
     * ABONO_CAPITAL con modalidad {@link #MODALIDAD_SIN_REGENERAR_TABLA}, {@code DTPRSLOT} de la cuota
     * (acumulativo) y el PagoPrestamo con {@code PGPRSLOT} = valor, igual que el abono de siempre.
     * Asiento: con cobro CBCR lo arma CBCRASN2 (D banco / H capital por banda, ver
     * {@code ContabilizacionIndividualCreditoService#haberDesdePagos}); acá no hay re-bandeo porque la
     * tabla no cambia. Camino directo con aportes: asiento único sin re-bandeo.
     */
    private AplicacionAbono aplicarSinRegenerarTabla(SolicitudAbonoCapital solicitud, double valor, LocalDate fecha,
            LocalDateTime fechaHora, List<DesgloseAporte> aportes, boolean conAportes) throws Throwable {
        CuadreSinRegenerar cuadre = cuadreSinRegenerar(solicitud.getIdPrestamo(), valor, solicitud.getNumeroCuota(), fecha);
        Prestamo prestamo = cuadre.prestamo;
        DetallePrestamo ancla = cuadre.cuota;
        String marca = "[C16b abono sin regenerar tabla, cuota #" + Math.round(nvl(ancla.getNumeroCuota())) + "]";
        String observacion = (solicitud.getObservacion() != null ? solicitud.getObservacion() + " " : "") + marca;

        EventoPrestamo evento = new EventoPrestamo();
        evento.setPrestamo(prestamo);
        evento.setTipoOperacion(ProcesoPagoPrestamoService.TIPO_ABONO_CAPITAL);
        evento.setValor(valor);
        evento.setModalidad(MODALIDAD_SIN_REGENERAR_TABLA);
        evento.setPlazoAnterior(prestamo.getPlazo());
        evento.setCuotaAnterior(prestamo.getValorCuota());
        evento.setPlazoNuevo(prestamo.getPlazo());
        evento.setCuotaNueva(prestamo.getValorCuota());
        evento.setFecha(fechaHora);
        evento.setObservacion(observacion);
        evento.setUsuario(solicitud.getUsuario());
        evento.setFechaRegistro(LocalDateTime.now());
        evento.setEstado(1L);
        evento = eventoPrestamoService.saveSingle(evento);

        ancla.setSaldoOtros(redondear(nvl(ancla.getSaldoOtros()) + valor));
        ancla = detallePrestamoService.saveSingle(ancla);

        PagoPrestamo pago = new PagoPrestamo();
        pago.setPrestamo(prestamo);
        pago.setDetallePrestamo(ancla);
        pago.setNumeroCuota(ancla.getNumeroCuota());
        pago.setFecha(fechaHora);
        pago.setValor(valor);
        pago.setSaldoOtros(valor);
        pago.setCapitalPagado(0.0);
        pago.setInteresPagado(0.0);
        pago.setMoraPagada(0.0);
        pago.setInteresVencidoPagado(0.0);
        pago.setDesgravamen(0.0);
        pago.setValorSeguroIncendio(0.0);
        pago.setTipo(ProcesoPagoPrestamoService.TIPO_ABONO_CAPITAL);
        pago.setObservacion(observacion + " [Evento: " + evento.getCodigo() + "]");
        pago.setUsuarioRegistro(solicitud.getUsuario());
        pago.setFechaRegistro(LocalDateTime.now());
        pago.setEstado(1L);
        pago.setIdEstado(1L);
        pago.setAnulado(0L);
        pago.setEventoPrestamo(evento);
        pago.setRutaDocumentoRespaldo(solicitud.getRutaDocumentoRespaldo());
        pago = pagoPrestamoService.saveSingle(pago);

        List<MovimientoAporte> movimientosAporte = new ArrayList<>();
        if (conAportes) {
            if (prestamo.getEntidad() == null || prestamo.getEntidad().getCodigo() == null) {
                throw new IncomeException(ProcesoPagoPrestamoService.ERR_PARAMETRO_INVALIDO + ": el préstamo "
                    + prestamo.getCodigo() + " no tiene partícipe asociado; no se puede abonar con aportes");
            }
            String glosa = "ABONO CAPITAL " + prestamo.getCodigo() + " - Evento " + evento.getCodigo();
            movimientosAporte = procesoPagoPrestamoService.consumirAportes(prestamo.getEntidad(), aportes,
                fechaHora, solicitud.getUsuario(), glosa, pago, solicitud.getRutaDocumentoRespaldo());
        }

        procesoPagoPrestamoService.registrarHuellaPrestamo(prestamo,
            ProcesoPagoPrestamoService.TIPO_ABONO_CAPITAL, valor, observacion, fechaHora, solicitud.getUsuario());

        // Asiento: solo el camino directo con aportes genera uno acá (cruce + cierre de apertura, sin
        // re-bandeo). Con cobro, CBCRASN2 lleva la plata; el hook de re-bandeo no aplica (la tabla no cambia).
        if (conAportes && solicitud.getIdCobroCredito() == null) {
            Long numeroAsiento = contabilidadPrestamoService.contabilizarAbonoCapitalConAportes(evento,
                movimientosAporte, solicitud.getIdEmpresa(),
                solicitud.getFechaAfectacion() != null ? solicitud.getFechaAfectacion() : fecha);
            if (numeroAsiento != null) {
                evento.setNumeroAsiento(numeroAsiento);
                eventoPrestamoService.saveSingle(evento);
                pago.setAsiento(numeroAsiento);
                pagoPrestamoService.saveSingle(pago);
            }
        }

        procesoMoraPrestamoService.regularizarPrestamoSiSinMora(prestamo.getCodigo());

        ResultadoAbonoCapital resultado = new ResultadoAbonoCapital();
        resultado.setIdPrestamo(prestamo.getCodigo());
        resultado.setIdEvento(evento.getCodigo());
        resultado.setIdPagoPrestamo(pago.getCodigo());
        resultado.setIdCuotaConSaldoOtros(ancla.getCodigo());
        resultado.setValorAbono(valor);
        resultado.setModalidad((int) MODALIDAD_SIN_REGENERAR_TABLA);
        resultado.setPlazoAnterior(prestamo.getPlazo());
        resultado.setPlazoNuevo(prestamo.getPlazo());
        resultado.setCuotaAnterior(prestamo.getValorCuota());
        resultado.setCuotaNueva(prestamo.getValorCuota());
        resultado.setCuotasHistorizadas(0);
        resultado.setCuotasGeneradas(0);
        resultado.setSeguroIncendioLiberado(0.0);
        resultado.setTablaRegenerada(Boolean.FALSE);
        resultado.setCapitalTablaMasAbonos(cuadre.totalDespues);

        System.out.println("  ✅ Abono a capital SIN regenerar tabla - Evento: " + evento.getCodigo()
            + " - cuota #" + ancla.getNumeroCuota() + " - $" + valor + " - tabla + abonos = $" + cuadre.totalDespues
            + " = monto $" + cuadre.monto);

        AplicacionAbono aplicacion = new AplicacionAbono();
        aplicacion.resultado = resultado;
        aplicacion.movimientos = movimientosAporte;
        return aplicacion;
    }

    // ========================================================================
    // Cálculo compartido por simular() y aplicar()
    // ========================================================================

    /** Datos intermedios del abono; no se expone fuera del servicio. */
    private static class CalculoAbono {
        private Prestamo prestamo;
        private long tipoAmortizacion;
        private double tasaMensual;
        private double valorAbono;
        private double saldoCapitalPendiente;
        private double nuevoCapital;
        private double cuotaVigente;
        private double cuotaNueva;
        private long plazoNuevo;
        private double ahorroIntereses;
        /**
         * Seguro de incendio de cada cuota HISTORIZADA, por número de cuota — NO es un valor
         * único para toda la tabla (bug corregido 2026-08-29). "El valor de la cuota N vieja
         * va a la cuota N nueva": se preserva por posición, no se toma de una sola fila.
         */
        private Map<Long, Double> seguroPorNumeroCuota = new java.util.HashMap<>();
        /**
         * Reserva defensiva SOLO para una cuota nueva sin correspondencia en
         * {@link #seguroPorNumeroCuota} (el plazo nuevo no debería superar la cantidad de
         * cuotas historizadas, pero si pasa, esto evita una NPE o un 0 silencioso). Se usa con
         * un log de advertencia — nunca en silencio.
         */
        private double seguroUltimaHistorizadaFallback;
        /**
         * Seguro de incendio TOTAL de las cuotas que desaparecen al re-amortizar con menos
         * plazo (modalidad 1) — 0 si el plazo no se acorta. Requerimiento futuro (2026-08-29,
         * pendiente de que el usuario lo pida): ese monto es lo que quedaría "liberado" para
         * pedir reembolso a la aseguradora. NO se implementa el proceso de reembolso acá; el
         * dato solo se deja calculado y expuesto en el resultado del abono para que ese
         * proceso, cuando se construya, no tenga que reconstruir este cálculo.
         */
        private double seguroIncendioLiberado;
        private List<DetallePrestamo> cuotasAHistorizar = new ArrayList<>();
        private List<CuotaProyectada> tabla = new ArrayList<>();
        /**
         * S13 — una entrada por (póliza viva, campo) que el abono tocó, para que
         * {@code aplicar()} cierre los PSCT viejos y abra los nuevos DESPUÉS de que las cuotas
         * nuevas tengan código real. Vacía si no había ninguna póliza viva sobre
         * {@code cuotasAHistorizar} — mismo comportamiento que antes de S13.
         */
        private List<ReRepartoPoliza> reRepartosAplicados = new ArrayList<>();
        /**
         * D-048, solo en re-proceso: cuotas pendientes (vencimiento posterior al abono) que ya tienen
         * filas en {@code CRD.PGPR} — pagos anticipados con fecha {@code <=} abono, o pagos anulados.
         * No se pueden borrar (FK_PGPR_DTPR, FK_DSBN_DTPR): se historizan igual que las demás y se
         * actualizan EN EL LUGAR con la cuota nueva del mismo número, conservando su código y sus
         * pagos. Clave: número de cuota (redondeado).
         */
        private Map<Long, DetallePrestamo> cuotasReutilizadas = new java.util.HashMap<>();
        /** D-048: pagos VIGENTES de cada cuota reutilizada (misma clave). */
        private Map<Long, List<PagoPrestamo>> pagosReutilizadas = new java.util.HashMap<>();
        /** D-048: capital ya pagado por anticipado en las cuotas reutilizadas (vuelve a la tabla nueva). */
        private double capitalPrepagado;
    }

    /** S13 — una póliza viva (documento + campo) cuyo seguro se re-repartió al abonar. */
    private static class ReRepartoPoliza {
        private com.saa.model.crd.DocumentoSeguro documento;
        private com.saa.model.crd.PrestamoSeguro prestamoSeguro;
        private long campo;
        private List<Long> idsCuotaVieja = new ArrayList<>();
        /** Clave: numeroCuota (redondeado) de la cuota NUEVA. */
        private Map<Long, Double> valorQueElAbonoHabriaPuesto = new java.util.HashMap<>();
        /** Clave: numeroCuota (redondeado) de la cuota NUEVA. Suma exacta al monto re-repartido. */
        private Map<Long, Double> montoRerepartido = new java.util.HashMap<>();
    }

    /**
     * @param soloLectura {@code true} desde {@code simular()}: NO debe escribir en
     *                    {@code CRD.DTPR}. {@code false} desde {@code aplicar()}: puede
     *                    autocorregir cuotas mal etiquetadas antes de re-amortizar (bug
     *                    corregido 2026-08-28, ver MotorPagoPrestamoService.calcularSaldosCuota).
     * @param reproceso   D-048: {@code true} = el préstamo tiene que estar al día A {@code fecha}
     *                    (ver {@link #validarAlDiaALaFecha}); {@code false} = regla de siempre
     *                    (ninguna cuota PARCIAL ni vencida antes de {@code fecha} en el estado de hoy).
     */
    private CalculoAbono calcular(Long idPrestamo, double valorAbono, int modalidad, LocalDate fecha,
            boolean soloLectura, boolean reproceso) throws Throwable {

        CalculoAbono calculo = new CalculoAbono();

        // --- Validaciones 1 y 2 -------------------------------------------------
        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
        if (prestamo == null) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PRESTAMO_NO_ENCONTRADO
                + ": no existe el préstamo " + idPrestamo);
        }
        if (esEstadoTerminalPrestamo(prestamo.getIdEstado())) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_ESTADO_NO_PERMITE
                + ": el préstamo " + idPrestamo + " está en estado "
                + textoEstadoTerminal(prestamo.getIdEstado())
                + " (terminal) y no admite abonos");
        }
        if (modalidad != 1 && modalidad != 2) {
            throw new IncomeException(ERR_MODALIDAD_INVALIDA
                + ": la modalidad debe ser 1 (reduce plazo) o 2 (reduce cuota)");
        }
        if (valorAbono <= 0.0) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_VALOR_INVALIDO
                + ": el valor del abono debe ser mayor a cero");
        }
        if (prestamo.getTipoAmortizacion() == null) {
            throw new IncomeException("El préstamo " + idPrestamo
                + " no tiene definido el tipo de amortización");
        }
        if (prestamo.getTasa() == null) {
            throw new IncomeException("El préstamo " + idPrestamo + " no tiene definida la tasa");
        }

        calculo.prestamo = prestamo;
        calculo.tipoAmortizacion = prestamo.getTipoAmortizacion();
        calculo.tasaMensual = prestamo.getTasa() / 100.0 / 12.0;
        calculo.valorAbono = valorAbono;

        // --- Validación 3: préstamo al día -------------------------------------
        // Primero se dejan correr los saldos reales: la autocorrección de §6.2 pasa a PAGADA
        // cualquier cuota que ya esté liquidada según PGPR, y así no bloquea el abono.
        // ⚠️ SOLO para aplicar() (soloLectura=false): calcularSaldosRealesCuota PERSISTE la
        // autocorrección. simular() usa la variante pura y NUNCA corre este loop — bug
        // corregido 2026-08-28, ver javadoc de MotorPagoPrestamoService.calcularSaldosCuota.
        List<DetallePrestamo> preliminar =
            detallePrestamoDaoService.selectCuotasPendientesByPrestamoOrdenadas(idPrestamo);
        if (!soloLectura) {
            for (DetallePrestamo cuota : preliminar) {
                motorPagoPrestamoService.calcularSaldosRealesCuota(cuota);
            }
        }

        // En modo lectura no hay nada que la autocorrección haya podido cambiar: reusar
        // `preliminar` evita una segunda consulta idéntica.
        List<DetallePrestamo> pendientes = soloLectura
            ? preliminar
            : detallePrestamoDaoService.selectCuotasPendientesByPrestamoOrdenadas(idPrestamo);
        if (pendientes == null || pendientes.isEmpty()) {
            throw new IncomeException(ERR_SIN_CUOTAS_PENDIENTES
                + ": el préstamo " + idPrestamo + " no tiene cuotas pendientes que re-amortizar");
        }

        if (reproceso) {
            // D-048: al día A LA FECHA DEL ABONO, con la lectura al corte de ModoCorteCartera (D-019,
            // D-044), y las cuotas futuras con pagos anticipados se re-amortizan en el lugar.
            validarAlDiaALaFecha(idPrestamo, fecha);
            prepararCuotasReutilizadas(calculo, pendientes, fecha);
        } else {
            for (DetallePrestamo cuota : pendientes) {
                if (cuota.getEstado() != null && cuota.getEstado() == EstadoCuotaPrestamo.PARCIAL) {
                    throw new IncomeException(ERR_PRESTAMO_NO_AL_DIA
                        + ": El préstamo tiene cuotas vencidas o parciales; regularícelas antes de abonar"
                        + " a capital (cuota #" + cuota.getNumeroCuota() + " está PARCIAL)");
                }
                if (cuota.getFechaVencimiento() != null
                        && cuota.getFechaVencimiento().toLocalDate().isBefore(fecha)) {
                    throw new IncomeException(ERR_PRESTAMO_NO_AL_DIA
                        + ": El préstamo tiene cuotas vencidas o parciales; regularícelas antes de abonar"
                        + " a capital (cuota #" + cuota.getNumeroCuota() + " venció el "
                        + cuota.getFechaVencimiento().toLocalDate() + ")");
                }
            }
        }
        calculo.cuotasAHistorizar = pendientes;

        // --- Validación 4: el abono no puede cubrir todo el capital -------------
        // Bug corregido 2026-08-28: antes se llamaba a calcularSaldosRealesCuota acá también,
        // así que hasta la rama de solo-lectura de simular() terminaba escribiendo en DTPR.
        // El capital usa el mismo cálculo compartido que la reestructuración y la precancelación
        // (pedido 8, segunda ola) — PagoPrestamoService.calcularSaldoCapitalPendiente, que
        // reconstruye desde CRD.PGPR sin persistir. El interés no tiene ese método compartido
        // (solo existe para capital), así que se suma con la variante pura por cuota,
        // MotorPagoPrestamoService.calcularSaldosCuota — mismo cálculo que calcularSaldosRealesCuota,
        // sin el efecto de autocorrección.
        double saldoCapitalPendiente = pagoPrestamoService.calcularSaldoCapitalPendiente(pendientes);
        double interesPendienteActual = 0.0;
        for (DetallePrestamo cuota : pendientes) {
            com.saa.ejb.crd.service.dto.SaldosCuota saldos =
                motorPagoPrestamoService.calcularSaldosCuota(cuota);
            interesPendienteActual += saldos.getSaldoInteres();
        }
        saldoCapitalPendiente = redondear(saldoCapitalPendiente);
        calculo.saldoCapitalPendiente = saldoCapitalPendiente;

        if (!(valorAbono < saldoCapitalPendiente - TOLERANCIA)) {
            throw new IncomeException(ERR_ABONO_CUBRE_CAPITAL
                + ": El abono $" + valorAbono + " cubre todo el capital pendiente $"
                + saldoCapitalPendiente + "; use la precancelación");
        }

        // D-048: el capital que una cuota reutilizada ya tiene pagado por anticipado vuelve a la tabla
        // nueva (su pago se queda en esa cuota); sin cuotas reutilizadas capitalPrepagado = 0.
        calculo.nuevoCapital = redondear(saldoCapitalPendiente + calculo.capitalPrepagado - valorAbono);

        // --- Parámetros de la re-amortización ----------------------------------
        DetallePrestamo primeraPendiente = pendientes.get(0);
        DetallePrestamo ultimaHistorizada = pendientes.get(pendientes.size() - 1);

        double cuotaVigente = nvl(prestamo.getValorCuota());
        if (cuotaVigente <= TOLERANCIA) {
            cuotaVigente = nvl(primeraPendiente.getCuota());
        }
        calculo.cuotaVigente = redondear(cuotaVigente);

        // Bug corregido 2026-08-29: el seguro de incendio es fijo por cuota (confirmado
        // contra el usuario — a diferencia del desgravamen, no depende del saldo) pero se
        // PRESERVA por NÚMERO DE CUOTA al re-amortizar, no se toma de una sola fila y se
        // aplica a todas. El valor de la cuota N vieja va a la cuota N nueva.
        for (DetallePrestamo historizada : pendientes) {
            if (historizada.getNumeroCuota() != null) {
                calculo.seguroPorNumeroCuota.put(Math.round(historizada.getNumeroCuota()),
                    redondear(nvl(historizada.getValorSeguroIncendio())));
            }
        }
        calculo.seguroUltimaHistorizadaFallback = redondear(nvl(ultimaHistorizada.getValorSeguroIncendio()));

        // Numeración: continúa desde la última cuota que QUEDA en DTPR
        double numeroInicial = numeroPrimeraCuotaNueva(idPrestamo, pendientes);

        // Calendario: se conservan los vencimientos originales de las cuotas reemplazadas
        List<LocalDateTime> vencimientos = new ArrayList<>();
        for (DetallePrestamo cuota : pendientes) {
            vencimientos.add(cuota.getFechaVencimiento());
        }

        int n = calcularNumeroDeCuotas(calculo, modalidad, primeraPendiente, pendientes.size());

        // --- Tabla proyectada ---------------------------------------------------
        construirTablaProyectada(calculo, modalidad, n, numeroInicial, vencimientos, primeraPendiente);

        // S13 (diseño §5.6bis): si alguna de las cuotas que se van a borrar (pendientes) tiene un
        // seguro vigente de una póliza, ese valor NO puede perderse con la constante 1,12/1000 ni
        // con el "seguro por número de cuota" de arriba — se re-reparte entre las cuotas NUEVAS
        // que caen dentro de la MISMA vigencia, proporcional a su propio saldo inicial de capital.
        // Sin póliza viva sobre 'pendientes', este método no toca nada (comportamiento de siempre).
        aplicarReRepartoPolizaViva(calculo, pendientes);

        // D-048: cada cuota reutilizada tiene que seguir existiendo en la tabla nueva y sus pagos
        // anticipados tienen que caber en ella, componente por componente.
        validarCuotasReutilizadas(calculo);

        // Requerimiento futuro (2026-08-29, anotado y NO implementado — no se construye el
        // proceso de reembolso): en modalidad 1 (acorta plazo), si la tabla nueva tiene menos
        // cuotas que las historizadas, las historizadas que quedan sin cuota nueva
        // correspondiente (índice >= n) ya tenían seguro de incendio cobrado o previsto. Ese
        // monto queda "liberado" y es el insumo que un futuro proceso de reembolso a la
        // aseguradora necesitaría — se deja calculado y expuesto en el resultado del abono.
        double seguroLiberado = 0.0;
        if (modalidad == 1 && n < pendientes.size()) {
            for (int idx = n; idx < pendientes.size(); idx++) {
                seguroLiberado += nvl(pendientes.get(idx).getValorSeguroIncendio());
            }
        }
        calculo.seguroIncendioLiberado = redondear(seguroLiberado);

        double interesNuevo = 0.0;
        for (CuotaProyectada proyectada : calculo.tabla) {
            interesNuevo += nvl(proyectada.getInteres());
        }
        calculo.ahorroIntereses = redondear(interesPendienteActual - interesNuevo);
        calculo.cuotaNueva = calculo.tabla.isEmpty() ? calculo.cuotaVigente
            : redondear(nvl(calculo.tabla.get(0).getCuota()));

        // Plazo: en modalidad 1 = cuotas que quedan (numeroCuota > 0) + n; en modalidad 2 no cambia
        if (modalidad == 1) {
            calculo.plazoNuevo = contarCuotasQueQuedan(idPrestamo, pendientes) + n;
        } else {
            calculo.plazoNuevo = prestamo.getPlazo() != null ? prestamo.getPlazo() : n;
        }

        System.out.println("  Cálculo del abono - Capital pendiente: $" + saldoCapitalPendiente
            + " - Nuevo capital: $" + calculo.nuevoCapital
            + " - Cuotas nuevas: " + n
            + " - Cuota: " + calculo.cuotaVigente + " → " + calculo.cuotaNueva);

        return calculo;
    }

    /** Cantidad de cuotas nuevas según la modalidad y el tipo de amortización. */
    private int calcularNumeroDeCuotas(CalculoAbono calculo, int modalidad,
            DetallePrestamo primeraPendiente, int cuotasReemplazadas) throws Throwable {

        double i = calculo.tasaMensual;
        double capital = calculo.nuevoCapital;

        if (modalidad == 2) {
            // Mantener el plazo: tantas cuotas como las reemplazadas
            return cuotasReemplazadas;
        }

        // Modalidad 1: mantener la cuota y reducir el plazo
        if (calculo.tipoAmortizacion == 2) {
            // Alemana: el capital por cuota es fijo
            double capitalPorCuota = redondear(nvl(primeraPendiente.getCapital()));
            if (capitalPorCuota <= TOLERANCIA) {
                throw new IncomeException("La cuota vigente no tiene capital definido;"
                    + " no se puede reducir el plazo");
            }
            return acotar((int) Math.ceil(capital / capitalPorCuota));
        }

        // Francesa
        double c = calculo.cuotaVigente;
        if (i <= 0.0) {
            // Sin interés la amortización es lineal
            return acotar((int) Math.ceil(capital / c));
        }
        if (capital * i >= c - TOLERANCIA) {
            throw new IncomeException(ERR_CUOTA_NO_CUBRE_INTERES
                + ": la cuota vigente $" + c + " no cubre el interés mensual $"
                + redondear(capital * i) + " del nuevo capital; use la modalidad 2");
        }
        double n = -Math.log(1 - (capital * i / c)) / Math.log(1 + i);
        return acotar((int) Math.ceil(n));
    }

    /** Construye la tabla proyectada aplicando la amortización que corresponda. */
    private void construirTablaProyectada(CalculoAbono calculo, int modalidad, int n,
            double numeroInicial, List<LocalDateTime> vencimientos, DetallePrestamo primeraPendiente)
            throws Throwable {

        double i = calculo.tasaMensual;
        double saldo = calculo.nuevoCapital;

        // Cuota fija de la modalidad francesa
        double cuotaFija;
        if (calculo.tipoAmortizacion == 2) {
            cuotaFija = 0.0; // alemana: la cuota varía
        } else if (modalidad == 1) {
            cuotaFija = calculo.cuotaVigente;
        } else if (i <= 0.0) {
            cuotaFija = redondear(calculo.nuevoCapital / n);
        } else {
            cuotaFija = redondear(calculo.nuevoCapital * i / (1 - Math.pow(1 + i, -n)));
        }

        // Capital fijo de la modalidad alemana
        double capitalFijo = 0.0;
        if (calculo.tipoAmortizacion == 2) {
            capitalFijo = modalidad == 1
                ? redondear(nvl(primeraPendiente.getCapital()))
                : redondear(calculo.nuevoCapital / n);
        }

        for (int k = 1; k <= n; k++) {
            double interes = redondear(saldo * i);
            double capitalCuota;

            if (calculo.tipoAmortizacion == 2) {
                capitalCuota = capitalFijo;
            } else {
                capitalCuota = redondear(cuotaFija - interes);
            }

            if (k == n) {
                // La última cuota absorbe el residuo exacto
                capitalCuota = redondear(saldo);
            }
            if (capitalCuota > saldo) {
                capitalCuota = redondear(saldo);
            }

            double saldoDespues = redondear(saldo - capitalCuota);
            double cuota = redondear(capitalCuota + interes);

            // Bug corregido 2026-08-29: el desgravamen NO es un valor fijo por cuota (era el
            // defecto del 2026-08-28, corregido a medias). Es saldo de capital × 0.9/1000,
            // igual que CalculadoraAmortizacionServiceImpl.calcularFrancesa/calcularAlemana —
            // MISMA constante compartida, no una copia. CRÍTICO: sobre `saldo` (el saldo ANTES
            // de amortizar esta cuota, la misma variable que ya usa `interes` arriba), NUNCA
            // sobre `saldoDespues`: usar el de después correría todo el cuadro un período, la
            // prima de la cuota k reflejaría la exposición de la k+1.
            double desgravamen = redondear(saldo * CalculadoraAmortizacionServiceImpl.FACTOR_DESGRAVAMEN_SOBRE_SALDO);

            double numeroCuotaActual = numeroInicial + (k - 1);
            // Bug corregido 2026-08-29: el seguro de incendio SÍ es un monto fijo por cuota
            // (confirmado contra el motor: CalculadoraAmortizacionServiceImpl.fila() lo aplica
            // igual en cada fila, sin factor sobre saldo — no tiene el defecto del desgravamen),
            // pero debe PRESERVARSE por número de cuota (la cuota N vieja financia la cuota N
            // nueva), no copiarse de una sola fila histórica hacia todas las nuevas. Criterio
            // defensivo para el caso borde (cuota nueva sin número correspondiente en el mapa,
            // p.ej. si el abono alarga el plazo más allá de lo historizado): usar el valor de
            // la última cuota historizada como respaldo y dejar constancia por log — nunca NPE
            // ni 0 silencioso.
            Long claveSeguro = Math.round(numeroCuotaActual);
            Double seguroCuota = calculo.seguroPorNumeroCuota.get(claveSeguro);
            if (seguroCuota == null) {
                seguroCuota = calculo.seguroUltimaHistorizadaFallback;
                System.out.println("AbonoCapitalPrestamoServiceImpl.construirTablaProyectada: "
                    + "cuota nueva #" + numeroCuotaActual + " sin correspondencia en el mapa de "
                    + "seguro de incendio por número de cuota; se usa el respaldo de la última "
                    + "cuota historizada ($" + seguroCuota + ").");
            }

            CuotaProyectada proyectada = new CuotaProyectada();
            proyectada.setNumeroCuota(numeroCuotaActual);
            proyectada.setFechaVencimiento(vencimientoDeLaCuota(vencimientos, k - 1));
            proyectada.setCapital(capitalCuota);
            proyectada.setInteres(interes);
            proyectada.setCuota(cuota);
            proyectada.setSaldoCapital(saldoDespues);
            proyectada.setDesgravamen(desgravamen);
            proyectada.setSeguroIncendio(seguroCuota);
            proyectada.setTotal(redondear(cuota + desgravamen + seguroCuota));
            calculo.tabla.add(proyectada);

            saldo = saldoDespues;
        }
    }

    /**
     * S13 (diseño §5.6bis): re-reparte el seguro de cada póliza viva que cubría alguna de
     * {@code pendientes} entre las cuotas NUEVAS de {@code calculo.tabla} que caen dentro de su
     * MISMA vigencia, proporcional a su propio saldo inicial de capital — reusa
     * {@link com.saa.ejb.crd.service.DocumentoSeguroService#repartirPorPeso}, la MISMA fórmula
     * que la distribución de pólizas, nunca una copia. Sobrescribe
     * {@code CuotaProyectada.desgravamen}/{@code .seguroIncendio}/{@code .total} de las cuotas
     * afectadas y deja en {@code calculo.reRepartosAplicados} lo que {@code aplicar()} necesita
     * para cerrar los PSCT viejos y abrir los nuevos una vez que las cuotas nuevas tengan código.
     *
     * <p>Agrupa por (documento, campo): un préstamo puede tener una póliza viva de desgravamen Y
     * otra de incendio/prendario a la vez, cada una con su propia vigencia. Sin ninguna póliza
     * viva sobre {@code pendientes}, no hace nada — el abono funciona exactamente como antes de
     * S13.</p>
     *
     * @throws IncomeException {@link AbonoCapitalPrestamoService#ERR_POLIZA_SIN_CUOTAS_EN_VIGENCIA}
     *         si una póliza viva no tiene NINGUNA cuota nueva dentro de su vigencia — no hay
     *         dónde re-repartir su seguro sin perderlo, así que no se aplica el abono.
     */
    private void aplicarReRepartoPolizaViva(CalculoAbono calculo, List<DetallePrestamo> pendientes)
            throws Throwable {
        List<Long> idsCuotaVieja = new ArrayList<>();
        for (DetallePrestamo cuota : pendientes) {
            idsCuotaVieja.add(cuota.getCodigo());
        }
        List<com.saa.model.crd.CuotaSeguro> todas = cuotaSeguroDaoService.selectByCuotas(idsCuotaVieja);

        // Agrupar las VIGENTES por (documento, campo) — las ya reversadas (otra póliza anulada
        // antes de este abono) no tienen nada que re-repartir.
        Map<String, List<com.saa.model.crd.CuotaSeguro>> porDocumentoCampo = new java.util.HashMap<>();
        for (com.saa.model.crd.CuotaSeguro psct : todas) {
            if (psct.getFechaReverso() != null) {
                continue;
            }
            String clave = psct.getDocumento().getCodigo() + "-" + psct.getCampo();
            porDocumentoCampo.computeIfAbsent(clave, k -> new ArrayList<>()).add(psct);
        }
        if (porDocumentoCampo.isEmpty()) {
            return;
        }

        for (List<com.saa.model.crd.CuotaSeguro> grupo : porDocumentoCampo.values()) {
            com.saa.model.crd.CuotaSeguro primero = grupo.get(0);
            com.saa.model.crd.DocumentoSeguro documento = primero.getDocumento();
            long campo = primero.getCampo();

            double montoARerepartir = 0.0;
            List<Long> idsViejas = new ArrayList<>();
            for (com.saa.model.crd.CuotaSeguro psct : grupo) {
                montoARerepartir += nvl(psct.getValorNuevo());
                idsViejas.add(psct.getIdCuota());
            }
            montoARerepartir = redondear(montoARerepartir);
            if (montoARerepartir <= TOLERANCIA) {
                continue;
            }

            LocalDate inicioVigencia = documento.getFechaInicio();
            LocalDate finVigencia = documento.getFechaFin();
            List<CuotaProyectada> enVigencia = new ArrayList<>();
            for (CuotaProyectada proyectada : calculo.tabla) {
                LocalDate vencimiento = proyectada.getFechaVencimiento() != null
                    ? proyectada.getFechaVencimiento().toLocalDate() : null;
                if (vencimiento != null && inicioVigencia != null && finVigencia != null
                        && !vencimiento.isBefore(inicioVigencia) && !vencimiento.isAfter(finVigencia)) {
                    enVigencia.add(proyectada);
                }
            }
            if (enVigencia.isEmpty()) {
                throw new IncomeException(AbonoCapitalPrestamoService.ERR_POLIZA_SIN_CUOTAS_EN_VIGENCIA
                    + ": la póliza (documento " + documento.getCodigo() + ") cubre cuotas que este abono"
                    + " va a reemplazar, pero ninguna cuota de la tabla nueva cae dentro de su vigencia ["
                    + inicioVigencia + ", " + finVigencia + "]; no hay dónde re-repartir su seguro de $"
                    + montoARerepartir + " sin perderlo");
            }

            List<Double> pesos = new ArrayList<>();
            for (CuotaProyectada proyectada : enVigencia) {
                pesos.add(nvl(proyectada.getCapital()) + nvl(proyectada.getSaldoCapital())); // DTPRSICP de la fila nueva
            }
            double[] repartido = documentoSeguroService.repartirPorPeso(pesos, montoARerepartir);

            ReRepartoPoliza registro = new ReRepartoPoliza();
            registro.documento = documento;
            registro.prestamoSeguro = primero.getPrestamoSeguro();
            registro.campo = campo;
            registro.idsCuotaVieja = idsViejas;

            for (int i = 0; i < enVigencia.size(); i++) {
                CuotaProyectada proyectada = enVigencia.get(i);
                Long clave = Math.round(proyectada.getNumeroCuota());
                double nuevoValor = repartido[i];

                if (campo == com.saa.rubros.CampoSeguroCuota.DESGRAVAMEN) {
                    registro.valorQueElAbonoHabriaPuesto.put(clave, nvl(proyectada.getDesgravamen()));
                    double diferencia = redondear(nuevoValor - nvl(proyectada.getDesgravamen()));
                    proyectada.setDesgravamen(nuevoValor);
                    proyectada.setTotal(redondear(nvl(proyectada.getTotal()) + diferencia));
                } else {
                    registro.valorQueElAbonoHabriaPuesto.put(clave, nvl(proyectada.getSeguroIncendio()));
                    double diferencia = redondear(nuevoValor - nvl(proyectada.getSeguroIncendio()));
                    proyectada.setSeguroIncendio(nuevoValor);
                    proyectada.setTotal(redondear(nvl(proyectada.getTotal()) + diferencia));
                }
                registro.montoRerepartido.put(clave, nuevoValor);
            }
            calculo.reRepartosAplicados.add(registro);
            System.out.println("  🔁 S13 - póliza " + documento.getCodigo() + " (campo " + campo
                + "): re-repartido $" + montoARerepartir + " entre " + enVigencia.size() + " cuota(s) nueva(s)");
        }
    }

    /**
     * Vencimiento de la cuota nueva en la posición indicada. Se reutilizan los vencimientos
     * originales de las cuotas reemplazadas (conserva el calendario del préstamo); si hicieran
     * falta más, se agregan meses tomando el último día de cada mes.
     */
    private LocalDateTime vencimientoDeLaCuota(List<LocalDateTime> vencimientos, int indice) throws Throwable {
        if (indice < vencimientos.size()) {
            return vencimientos.get(indice);
        }
        LocalDateTime base = vencimientos.get(vencimientos.size() - 1);
        LocalDateTime resultado = base;
        for (int k = vencimientos.size(); k <= indice; k++) {
            LocalDate temp = resultado.toLocalDate().plusMonths(1);
            LocalDate ultimoDia = fechaService.ultimoDiaMesAnioLocal(
                Long.valueOf(temp.getMonthValue()), Long.valueOf(temp.getYear()));
            resultado = ultimoDia.atTime(base.toLocalTime());
        }
        return resultado;
    }

    /** Número de la primera cuota nueva: (mayor numeroCuota que QUEDA en DTPR) + 1. */
    private double numeroPrimeraCuotaNueva(Long idPrestamo, List<DetallePrestamo> aHistorizar) throws Throwable {
        List<DetallePrestamo> todas = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
        double maximo = -1.0;
        for (DetallePrestamo cuota : todas) {
            if (contiene(aHistorizar, cuota.getCodigo()) || cuota.getNumeroCuota() == null) {
                continue;
            }
            if (cuota.getNumeroCuota() > maximo) {
                maximo = cuota.getNumeroCuota();
            }
        }
        return maximo < 0 ? 1.0 : maximo + 1.0;
    }

    /** Cuotas que quedan en DTPR con numeroCuota &gt; 0 (la cuota 0 de gracia no cuenta al plazo). */
    private long contarCuotasQueQuedan(Long idPrestamo, List<DetallePrestamo> aHistorizar) throws Throwable {
        List<DetallePrestamo> todas = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
        long cuenta = 0;
        for (DetallePrestamo cuota : todas) {
            if (contiene(aHistorizar, cuota.getCodigo())) {
                continue;
            }
            if (cuota.getNumeroCuota() != null && cuota.getNumeroCuota() > 0) {
                cuenta++;
            }
        }
        return cuenta;
    }

    private boolean contiene(List<DetallePrestamo> lista, Long codigo) {
        if (codigo == null) {
            return false;
        }
        for (DetallePrestamo cuota : lista) {
            if (codigo.equals(cuota.getCodigo())) {
                return true;
            }
        }
        return false;
    }

    // ========================================================================
    // D-048 — "al día" a la fecha del abono, en re-proceso
    // ========================================================================

    /**
     * D-048 (29-oct-2026): en re-proceso el préstamo tiene que estar al día A LA FECHA DEL ABONO, no
     * hoy. Misma lectura al corte que {@code CierreCarteraDaoServiceImpl.pendiente} (C15/D-019/D-044):
     * una cuota está liquidada a {@code fecha} si su estado es PAGADA (4) o CANCELADA_ANTICIPADA (7)
     * y su fecha de pago ({@code DTPRFCPG}, que D-044 deja en la del ÚLTIMO pago que la completa) es
     * {@code <=} fecha. Reglas:
     * <ul>
     * <li>Cuota con vencimiento {@code <=} fecha (convención inclusiva de ModoCorteCartera): tiene
     *     que estar liquidada a esa fecha; si no → {@code PRESTAMO_NO_AL_DIA}.</li>
     * <li>Cuota con vencimiento {@code >} fecha liquidada DESPUÉS de la fecha → {@code
     *     PAGOS_POSTERIORES_AL_ABONO}: la operación posterior ya está aplicada y la re-amortización
     *     no la puede mover; el abono se re-procesa antes que ella.</li>
     * </ul>
     * Una cuota futura PARCIAL por un pago anticipado (p. ej. el excedente Petro del mes) no impide
     * el abono: la trata {@link #prepararCuotasReutilizadas}.
     */
    private void validarAlDiaALaFecha(Long idPrestamo, LocalDate fecha) throws Throwable {
        List<DetallePrestamo> todas = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
        if (todas == null) {
            return;
        }
        for (DetallePrestamo cuota : todas) {
            if (cuota.getFechaVencimiento() == null) {
                continue;
            }
            LocalDate vencimiento = cuota.getFechaVencimiento().toLocalDate();
            boolean liquidada = cuota.getEstado() != null
                && (cuota.getEstado() == EstadoCuotaPrestamo.PAGADA
                    || cuota.getEstado() == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA);
            LocalDate fechaPago = cuota.getFechaPagado() != null ? cuota.getFechaPagado().toLocalDate() : null;
            if (!vencimiento.isAfter(fecha)) {
                if (!liquidada || fechaPago == null || fechaPago.isAfter(fecha)) {
                    throw new IncomeException(ERR_PRESTAMO_NO_AL_DIA + ": a la fecha del abono (" + fecha
                        + ", re-proceso D-048) la cuota #" + cuota.getNumeroCuota() + " (vence " + vencimiento
                        + ") no estaba liquidada"
                        + (liquidada ? (fechaPago != null ? " (se liquidó el " + fechaPago + ", después del abono)"
                                                          : " (está liquidada sin fecha de pago)")
                                     : " (estado de hoy " + cuota.getEstado() + ")")
                        + "; regularícela a esa fecha antes de abonar a capital");
                }
            } else if (liquidada && fechaPago != null && fechaPago.isAfter(fecha)) {
                throw new IncomeException(ERR_PAGOS_POSTERIORES_AL_ABONO + ": la cuota #" + cuota.getNumeroCuota()
                    + " (vence " + vencimiento + ") se liquidó el " + fechaPago + ", después del abono del " + fecha
                    + "; la re-amortización no puede mover ese pago. Re-procese el abono antes que él.");
            }
        }
    }

    /**
     * D-048: de las cuotas a re-amortizar (todas con vencimiento posterior al abono, por
     * {@link #validarAlDiaALaFecha}), las que ya tienen filas en {@code CRD.PGPR} no se pueden borrar
     * (FK_PGPR_DTPR; los pagos Petro también dejan FK_DSBN_DTPR). Si sus pagos VIGENTES son todos de
     * fecha {@code <=} abono (pago anticipado: el excedente de la cuota del mes que el motor imputa a
     * la siguiente), la cuota se reutiliza: se historiza y se actualiza en el lugar con la cuota nueva
     * del mismo número. Un pago vigente posterior al abono → {@code PAGOS_POSTERIORES_AL_ABONO}. Los
     * pagos anulados no cuentan, pero también obligan a conservar la fila.
     */
    private void prepararCuotasReutilizadas(CalculoAbono calculo, List<DetallePrestamo> pendientes, LocalDate fecha)
            throws Throwable {
        double capitalPrepagado = 0.0;
        for (DetallePrestamo cuota : pendientes) {
            List<PagoPrestamo> pagos = pagoPrestamoDaoService.selectByIdDetallePrestamo(cuota.getCodigo());
            if (pagos == null || pagos.isEmpty()) {
                continue;
            }
            if (cuota.getNumeroCuota() == null) {
                throw new IncomeException(ERR_PRESTAMO_NO_AL_DIA + ": la cuota " + cuota.getCodigo()
                    + " tiene pagos registrados y no tiene número de cuota; no puede re-amortizarse");
            }
            List<PagoPrestamo> vigentes = new ArrayList<>();
            for (PagoPrestamo pago : pagos) {
                if (pago.getAnulado() != null && pago.getAnulado() != 0L) {
                    continue;
                }
                LocalDate fechaPago = pago.getFecha() != null ? pago.getFecha().toLocalDate() : null;
                if (fechaPago == null || fechaPago.isAfter(fecha)) {
                    throw new IncomeException(ERR_PAGOS_POSTERIORES_AL_ABONO + ": la cuota #" + cuota.getNumeroCuota()
                        + " tiene el pago " + pago.getCodigo() + " del " + fechaPago + ", posterior al abono del "
                        + fecha + "; la re-amortización no puede mover ese pago. Re-procese el abono antes que él.");
                }
                vigentes.add(pago);
                capitalPrepagado += nvl(pago.getCapitalPagado());
            }
            Long numero = Math.round(cuota.getNumeroCuota());
            calculo.cuotasReutilizadas.put(numero, cuota);
            calculo.pagosReutilizadas.put(numero, vigentes);
            System.out.println("  ♻️ D-048 - cuota #" + numero + " (" + cuota.getCodigo() + ") con " + vigentes.size()
                + " pago(s) vigente(s) anticipado(s) y " + (pagos.size() - vigentes.size())
                + " anulado(s): se re-amortiza en el lugar");
        }
        calculo.capitalPrepagado = redondear(capitalPrepagado);
    }

    /**
     * D-048: cada cuota reutilizada tiene una cuota nueva con su número, y sus pagos anticipados caben
     * en ella componente por componente (la cuota nueva no tiene mora ni interés vencido).
     */
    private void validarCuotasReutilizadas(CalculoAbono calculo) throws Throwable {
        for (Map.Entry<Long, DetallePrestamo> entrada : calculo.cuotasReutilizadas.entrySet()) {
            CuotaProyectada nueva = null;
            for (CuotaProyectada proyectada : calculo.tabla) {
                if (proyectada.getNumeroCuota() != null && Math.round(proyectada.getNumeroCuota()) == entrada.getKey()) {
                    nueva = proyectada;
                    break;
                }
            }
            if (nueva == null) {
                throw new IncomeException(ERR_PRESTAMO_NO_AL_DIA + ": la cuota #" + entrada.getKey()
                    + " tiene pagos registrados y la tabla nueva termina antes de ella; no puede re-amortizarse");
            }
            double[] pagado = sumarPagos(calculo.pagosReutilizadas.get(entrada.getKey()));
            String exceso = null;
            if (pagado[0] > nvl(nueva.getCapital()) + TOLERANCIA) {
                exceso = "capital " + redondear(pagado[0]) + " > " + nueva.getCapital();
            } else if (pagado[1] > nvl(nueva.getInteres()) + TOLERANCIA) {
                exceso = "interés " + redondear(pagado[1]) + " > " + nueva.getInteres();
            } else if (pagado[2] > nvl(nueva.getDesgravamen()) + TOLERANCIA) {
                exceso = "desgravamen " + redondear(pagado[2]) + " > " + nueva.getDesgravamen();
            } else if (pagado[3] > TOLERANCIA || pagado[4] > TOLERANCIA) {
                exceso = "mora/interés vencido " + redondear(pagado[3] + pagado[4]) + " > 0";
            } else if (pagado[5] > nvl(nueva.getSeguroIncendio()) + TOLERANCIA) {
                exceso = "seguro de incendio " + redondear(pagado[5]) + " > " + nueva.getSeguroIncendio();
            }
            if (exceso != null) {
                throw new IncomeException(ERR_PRESTAMO_NO_AL_DIA + ": los pagos anticipados de la cuota #"
                    + entrada.getKey() + " no caben en la cuota nueva (" + exceso + ")");
            }
        }
    }

    /** {capital, interés, desgravamen, mora, interés vencido, seguro de incendio} pagados. */
    private double[] sumarPagos(List<PagoPrestamo> pagos) {
        double[] suma = new double[6];
        if (pagos != null) {
            for (PagoPrestamo pago : pagos) {
                suma[0] += nvl(pago.getCapitalPagado());
                suma[1] += nvl(pago.getInteresPagado());
                suma[2] += nvl(pago.getDesgravamen());
                suma[3] += nvl(pago.getMoraPagada());
                suma[4] += nvl(pago.getInteresVencidoPagado());
                suma[5] += nvl(pago.getValorSeguroIncendio());
            }
        }
        return suma;
    }

    private boolean esReutilizada(CalculoAbono calculo, DetallePrestamo cuota) {
        if (cuota == null || cuota.getCodigo() == null) {
            return false;
        }
        for (DetallePrestamo reutilizada : calculo.cuotasReutilizadas.values()) {
            if (cuota.getCodigo().equals(reutilizada.getCodigo())) {
                return true;
            }
        }
        return false;
    }

    /** D-048: última cuota PAGADA (mayor número) que no es una cuota reutilizada. */
    private DetallePrestamo ultimaPagadaNoReutilizada(Long idPrestamo, CalculoAbono calculo) throws Throwable {
        DetallePrestamo ultima = null;
        for (DetallePrestamo cuota : detallePrestamoDaoService.selectByPrestamo(idPrestamo)) {
            if (cuota.getEstado() == null || cuota.getEstado() != EstadoCuotaPrestamo.PAGADA
                    || cuota.getNumeroCuota() == null || esReutilizada(calculo, cuota)) {
                continue;
            }
            if (ultima == null || cuota.getNumeroCuota() > ultima.getNumeroCuota()) {
                ultima = cuota;
            }
        }
        return ultima;
    }

    /**
     * D-048: actualiza EN EL LUGAR una cuota reutilizada con los valores de la cuota nueva del mismo
     * número (mismo cálculo que {@link #construirCuota}) y recompone sus acumulados con sus pagos
     * vigentes, igual que el motor al aplicar un pago parcial (MotorPagoPrestamoServiceImpl): pagado
     * por componente, saldo de capital = saldo inicial − capital pagado, saldo = total − pagado, y
     * estado PARCIAL (o PAGADA, con la fecha del último pago, si los pagos la cubren).
     */
    private DetallePrestamo reutilizarCuota(DetallePrestamo cuota, Prestamo prestamo, CuotaProyectada proyectada,
            CalculoAbono calculo, List<PagoPrestamo> pagos) throws Throwable {
        DetallePrestamo nueva = construirCuota(prestamo, proyectada, calculo);
        cuota.setNumeroCuota(nueva.getNumeroCuota());
        cuota.setFechaVencimiento(nueva.getFechaVencimiento());
        cuota.setCapital(nueva.getCapital());
        cuota.setInteres(nueva.getInteres());
        cuota.setCuota(nueva.getCuota());
        cuota.setSaldoInicialCapital(nueva.getSaldoInicialCapital());
        cuota.setSaldoCapital(nueva.getSaldoCapital());
        cuota.setDesgravamen(nueva.getDesgravamen());
        cuota.setDesgravamenOriginal(nueva.getDesgravamenOriginal());
        cuota.setDesgravamenFirmado(nueva.getDesgravamenFirmado());
        cuota.setDesgravamenDiferido(nueva.getDesgravamenDiferido());
        cuota.setValorDiferido(nueva.getValorDiferido());
        cuota.setValorSeguroIncendio(nueva.getValorSeguroIncendio());
        cuota.setOtrosSeguros(nueva.getOtrosSeguros());
        cuota.setTotal(nueva.getTotal());
        cuota.setTotalConSeguro(nueva.getTotalConSeguro());
        cuota.setSaldo(nueva.getSaldo());
        cuota.setMora(0.0);
        cuota.setInteresVencido(0.0);
        cuota.setSaldoInteres(nueva.getSaldoInteres());
        cuota.setSaldoMora(0.0);
        cuota.setSaldoInteresVencido(0.0);
        cuota.setMoraCalculada(0.0);
        cuota.setDiasMora(0L);
        cuota.setAbono(0.0);
        cuota.setSaldoOtros(0.0);
        cuota.setFechaPagado(null);
        cuota.setEstado((long) EstadoCuotaPrestamo.PENDIENTE);

        double[] pagado = sumarPagos(pagos);
        double totalPagado = redondear(pagado[0] + pagado[1] + pagado[2] + pagado[3] + pagado[4] + pagado[5]);
        cuota.setCapitalPagado(redondear(pagado[0]));
        cuota.setInteresPagado(redondear(pagado[1]));
        cuota.setDesgravamenPagado(redondear(pagado[2]));
        cuota.setMoraPagado(redondear(pagado[3]));
        cuota.setInteresVendidoPagado(redondear(pagado[4]));
        if (totalPagado > TOLERANCIA) {
            cuota.setSaldoCapital(Math.max(0, redondear(nvl(cuota.getSaldoInicialCapital()) - pagado[0])));
            cuota.setSaldoInteres(Math.max(0, redondear(nvl(cuota.getInteres()) - pagado[1])));
            double saldo = Math.max(0, redondear(nvl(cuota.getTotal()) - totalPagado));
            cuota.setSaldo(saldo);
            if (saldo <= TOLERANCIA) {
                cuota.setEstado((long) EstadoCuotaPrestamo.PAGADA);
                LocalDateTime ultimoPago = null;
                for (PagoPrestamo pago : pagos) {
                    if (pago.getFecha() != null && (ultimoPago == null || pago.getFecha().isAfter(ultimoPago))) {
                        ultimoPago = pago.getFecha();
                    }
                }
                cuota.setFechaPagado(ultimoPago);
            } else {
                cuota.setEstado((long) EstadoCuotaPrestamo.PARCIAL);
            }
        }
        cuota.setIdEstado(cuota.getEstado());
        cuota = detallePrestamoService.saveSingle(cuota);
        System.out.println("  ♻️ D-048 - cuota #" + cuota.getNumeroCuota() + " (" + cuota.getCodigo()
            + ") re-amortizada en el lugar: total $" + cuota.getTotal() + ", pagado anticipado $" + totalPagado
            + ", estado " + cuota.getEstado());
        return cuota;
    }

    // ========================================================================
    // Construcción de entidades
    // ========================================================================

    /** Arma una cuota nueva de DTPR con TODOS los campos del invariante llenos. */
    private DetallePrestamo construirCuota(Prestamo prestamo, CuotaProyectada proyectada, CalculoAbono calculo) {
        DetallePrestamo cuota = new DetallePrestamo();
        cuota.setPrestamo(prestamo);
        cuota.setNumeroCuota(proyectada.getNumeroCuota());
        cuota.setFechaVencimiento(proyectada.getFechaVencimiento());
        cuota.setCapital(proyectada.getCapital());
        cuota.setInteres(proyectada.getInteres());
        cuota.setCuota(proyectada.getCuota());

        // Invariante: saldoInicialCapital = capital + saldoCapital + saldoOtros
        cuota.setSaldoInicialCapital(redondear(proyectada.getCapital() + proyectada.getSaldoCapital()));
        cuota.setSaldoCapital(proyectada.getSaldoCapital());

        // Bug corregido 2026-08-29: esto persistía calculo.desgravamenPorCuota (el valor FIJO
        // copiado de la última cuota histórica) en CADA cuota real de DTPR, aunque
        // construirTablaProyectada ya calculaba el desgravamen correcto (variable sobre saldo)
        // por fila desde ayer — la vista previa y lo que se grababa quedaban DESINCRONIZADOS.
        // Debe leer proyectada.getDesgravamen(), la fila que corresponde a ESTA cuota.
        double desgravamenCuota = nvl(proyectada.getDesgravamen());
        cuota.setDesgravamen(desgravamenCuota);
        cuota.setDesgravamenOriginal(desgravamenCuota);
        cuota.setDesgravamenFirmado(desgravamenCuota);
        cuota.setDesgravamenDiferido(0.0);
        cuota.setValorDiferido(0.0);
        // Bug corregido 2026-08-29: mismo defecto que el desgravamen arriba, pero con el
        // seguro de incendio — se persistía calculo.seguroPorCuota (un único valor copiado de
        // la última cuota histórica) en todas las cuotas nuevas. Debe leer
        // proyectada.getSeguroIncendio(), que ya resolvió construirTablaProyectada con el
        // valor de LA cuota histórica correspondiente por número (o el respaldo defensivo).
        double seguroCuota = nvl(proyectada.getSeguroIncendio());
        cuota.setValorSeguroIncendio(seguroCuota);
        cuota.setOtrosSeguros(0.0);

        double total = redondear(proyectada.getCuota() + desgravamenCuota + seguroCuota);
        cuota.setTotal(total);
        cuota.setTotalConSeguro(total);
        cuota.setSaldo(total);

        cuota.setMora(0.0);
        cuota.setInteresVencido(0.0);
        cuota.setSaldoInteres(proyectada.getInteres());
        cuota.setSaldoMora(0.0);
        cuota.setSaldoInteresVencido(0.0);
        cuota.setMoraCalculada(0.0);
        cuota.setDiasMora(0L);

        cuota.setAbono(0.0);
        cuota.setSaldoOtros(0.0);
        cuota.setCapitalPagado(0.0);
        cuota.setInteresPagado(0.0);
        cuota.setDesgravamenPagado(0.0);
        cuota.setMoraPagado(0.0);
        cuota.setInteresVendidoPagado(0.0);
        cuota.setFechaPagado(null);

        cuota.setEstado((long) EstadoCuotaPrestamo.PENDIENTE);
        cuota.setIdEstado((long) EstadoCuotaPrestamo.PENDIENTE);
        cuota.setFechaRegistro(LocalDateTime.now());
        return cuota;
    }

    /** Copia campo a campo una cuota de DTPR a su espejo en HDTP. */
    private HistDetallePrestamo copiarAHistorico(DetallePrestamo cuota, EventoPrestamo evento,
            String motivo, String usuario) {

        HistDetallePrestamo h = new HistDetallePrestamo();
        h.setCodigoOriginal(cuota.getCodigo());
        h.setEventoPrestamo(evento);
        h.setMotivo(motivo);
        h.setFechaRegistroHist(LocalDateTime.now());
        h.setUsuarioHist(usuario);

        h.setPrestamo(cuota.getPrestamo());
        h.setNumeroCuota(cuota.getNumeroCuota());
        h.setFechaVencimiento(cuota.getFechaVencimiento());
        h.setCapital(cuota.getCapital());
        h.setInteres(cuota.getInteres());
        h.setMora(cuota.getMora());
        h.setInteresVencido(cuota.getInteresVencido());
        h.setSaldoCapital(cuota.getSaldoCapital());
        h.setSaldoInteres(cuota.getSaldoInteres());
        h.setSaldoMora(cuota.getSaldoMora());
        h.setSaldoInteresVencido(cuota.getSaldoInteresVencido());
        h.setFechaPagado(cuota.getFechaPagado());
        h.setAbono(cuota.getAbono());
        h.setCapitalPagado(cuota.getCapitalPagado());
        h.setInteresPagado(cuota.getInteresPagado());
        h.setDesgravamen(cuota.getDesgravamen());
        h.setCuota(cuota.getCuota());
        h.setSaldo(cuota.getSaldo());
        h.setSaldoOtros(cuota.getSaldoOtros());
        h.setDesgravamenFirmado(cuota.getDesgravamenFirmado());
        h.setDesgravamenDiferido(cuota.getDesgravamenDiferido());
        h.setDesgravamenOriginal(cuota.getDesgravamenOriginal());
        h.setValorDiferido(cuota.getValorDiferido());
        h.setTotal(cuota.getTotal());
        h.setMoraPagado(cuota.getMoraPagado());
        h.setDesgravamenPagado(cuota.getDesgravamenPagado());
        h.setInteresVendidoPagado(cuota.getInteresVendidoPagado());
        h.setMoraCalculada(cuota.getMoraCalculada());
        h.setDiasMora(cuota.getDiasMora());
        h.setEstado(cuota.getEstado());
        h.setFechaRegistro(cuota.getFechaRegistro());
        h.setUsuarioRegistro(cuota.getUsuarioRegistro());
        h.setIdEstado(cuota.getIdEstado());
        h.setCodigoExterno(cuota.getCodigoExterno());
        h.setOtrosSeguros(cuota.getOtrosSeguros());
        h.setTotalConSeguro(cuota.getTotalConSeguro());
        h.setValorSeguroIncendio(cuota.getValorSeguroIncendio());
        h.setSaldoInicialCapital(cuota.getSaldoInicialCapital());
        return h;
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private int acotar(int n) {
        if (n < 1) {
            return 1;
        }
        return Math.min(n, MAX_CUOTAS_NUEVAS);
    }

    private boolean esEstadoTerminalPrestamo(Long idEstado) {
        return idEstado != null
            && (idEstado == EstadoPrestamo.CANCELADO
             || idEstado == EstadoPrestamo.CANCELADO_ANTICIPADO
             || idEstado == EstadoPrestamo.CANCELADO_POR_NOVACION
             || idEstado == EstadoPrestamo.ANULADO
             || idEstado == EstadoPrestamo.CANCELADO_POR_REESTRUCTURACION);
    }

    /** Texto del estado para el rechazo: el ANULADO y el 13 se nombran, los demás van por su número. */
    private String textoEstadoTerminal(Long idEstado) {
        if (idEstado != null && idEstado == EstadoPrestamo.ANULADO) {
            return "ANULADO (12)";
        }
        if (idEstado != null && idEstado == EstadoPrestamo.CANCELADO_POR_REESTRUCTURACION) {
            return "CANCELADO_POR_REESTRUCTURACION (13)";
        }
        return String.valueOf(idEstado);
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

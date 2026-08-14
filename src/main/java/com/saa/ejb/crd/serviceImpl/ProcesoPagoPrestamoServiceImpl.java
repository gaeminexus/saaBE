package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.AporteDaoService;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.EventoPrestamoDaoService;
import com.saa.ejb.crd.dao.HistDetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PagoAporteDaoService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.dao.TipoAporteDaoService;
import com.saa.ejb.crd.service.ContabilidadPrestamoService;
import com.saa.ejb.crd.service.DetallePrestamoService;
import com.saa.ejb.crd.service.EventoPrestamoService;
import com.saa.ejb.crd.service.MotorPagoPrestamoService;
import com.saa.ejb.crd.service.PagoPrestamoService;
import com.saa.ejb.crd.service.PrestamoService;
import com.saa.ejb.crd.service.ProcesoPagoPrestamoService;
import com.saa.ejb.crd.service.SaldoAporteService;
import com.saa.ejb.crd.service.dto.ContextoPago;
import com.saa.ejb.crd.service.dto.CuotaExigible;
import com.saa.ejb.crd.service.dto.DesgloseAporte;
import com.saa.ejb.crd.service.dto.DetalleAplicacionCuota;
import com.saa.ejb.crd.service.dto.MovimientoAporte;
import com.saa.ejb.crd.service.dto.ResultadoAnulacion;
import com.saa.ejb.crd.service.dto.ResultadoAplicacionPago;
import com.saa.ejb.crd.service.dto.ResultadoPagoConAportes;
import com.saa.ejb.crd.service.dto.ResultadoPrecancelacion;
import com.saa.ejb.crd.service.dto.SaldosCuota;
import com.saa.ejb.crd.service.dto.SimulacionPrecancelacion;
import com.saa.ejb.crd.service.dto.SolicitudAnulacion;
import com.saa.ejb.crd.service.dto.SolicitudPagoConAportes;
import com.saa.ejb.crd.service.dto.SolicitudPagoCuota;
import com.saa.ejb.crd.service.dto.SolicitudPrecancelacion;
import com.saa.model.crd.Aporte;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.EventoPrestamo;
import com.saa.model.crd.HistDetallePrestamo;
import com.saa.model.crd.PagoAporte;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.TipoAporte;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoPrestamo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Implementación de los procesos de pago de préstamos (§7 de
 * ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md).
 *
 * @author Sistema SAA
 * @since 2026-08-14
 */
@Stateless
public class ProcesoPagoPrestamoServiceImpl implements ProcesoPagoPrestamoService {

    /** Tolerancia de UN CENTAVO (§6.1) */
    private static final double TOLERANCIA = 0.01;

    /** Longitud máxima en BYTES de PRST.PRSTOBSR (VARCHAR2(2000)) */
    private static final int MAX_BYTES_OBSERVACION = 2000;

    /** Estado con el que se crean las filas de APRT de un pago con aportes: fuera del FIFO petro */
    private static final long ESTADO_APORTE_CONSUMIDO = EstadoCuotaPrestamo.PAGADA;

    private static final DateTimeFormatter FORMATO_HUELLA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @EJB
    private MotorPagoPrestamoService motorPagoPrestamoService;

    @EJB
    private EventoPrestamoService eventoPrestamoService;

    @EJB
    private EventoPrestamoDaoService eventoPrestamoDaoService;

    @EJB
    private ContabilidadPrestamoService contabilidadPrestamoService;

    @EJB
    private SaldoAporteService saldoAporteService;

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
    private AporteDaoService aporteDaoService;

    @EJB
    private PagoAporteDaoService pagoAporteDaoService;

    @EJB
    private TipoAporteDaoService tipoAporteDaoService;

    // ========================================================================
    // §7.1 — Pago de cuota
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoAplicacionPago pagarCuota(SolicitudPagoCuota solicitud) throws Throwable {
        System.out.println("ProcesoPagoPrestamoService.pagarCuota - Préstamo: "
            + (solicitud != null ? solicitud.getIdPrestamo() : null)
            + " - Valor: " + (solicitud != null ? solicitud.getValor() : null));

        if (solicitud == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": no se recibió el cuerpo de la solicitud");
        }
        Prestamo prestamo = validarPrestamoOperable(solicitud.getIdPrestamo(), solicitud.getUsuario());

        double valor = redondear(solicitud.getValor() != null ? solicitud.getValor() : 0.0);
        if (valor <= 0.0) {
            throw new IncomeException(ERR_VALOR_INVALIDO + ": el valor del pago debe ser mayor a cero");
        }

        LocalDateTime fechaPagoHora = validarFechaNoFutura(solicitud.getFechaPago());

        double deudaTotal = motorPagoPrestamoService.calcularTotalPendientePrestamo(prestamo.getCodigo());
        if (deudaTotal <= TOLERANCIA) {
            throw new IncomeException(ERR_SIN_CUOTAS_PENDIENTES + ": el préstamo " + prestamo.getCodigo()
                + " no tiene cuotas pendientes con saldo");
        }
        if (valor > deudaTotal + TOLERANCIA) {
            throw new IncomeException(ERR_VALOR_EXCEDE_DEUDA + ": El valor $" + formatoMonto(valor)
                + " excede la deuda total $" + formatoMonto(deudaTotal)
                + " del préstamo; use la precancelación");
        }

        EventoPrestamo evento = crearEvento(prestamo, TIPO_PAGO_MANUAL, valor, fechaPagoHora,
            solicitud.getUsuario(), solicitud.getObservacion());

        ContextoPago ctx = crearContexto(TIPO_PAGO_MANUAL, solicitud.getUsuario(),
            solicitud.getObservacion(), fechaPagoHora, evento.getCodigo());

        ResultadoAplicacionPago resultado = motorPagoPrestamoService.aplicarPago(
            prestamo.getCodigo(), valor, ctx);

        if (resultado.getValorAplicado() <= TOLERANCIA) {
            throw new IncomeException(ERR_SIN_CUOTAS_PENDIENTES + ": no se encontró ninguna cuota"
                + " pendiente donde aplicar el pago del préstamo " + prestamo.getCodigo());
        }

        registrarHuellaPrestamo(prestamo, TIPO_PAGO_MANUAL, valor, solicitud.getObservacion(),
            fechaPagoHora, solicitud.getUsuario());

        Long numeroAsiento = contabilidadPrestamoService.contabilizarPagoCuota(resultado, ctx);
        aplicarAsiento(evento, resultado, numeroAsiento);

        System.out.println("  ✅ pagarCuota OK - Evento: " + evento.getCodigo()
            + " - Aplicado: $" + resultado.getValorAplicado()
            + " - Cuotas afectadas: " + resultado.getCuotasAfectadas().size()
            + " - Préstamo cancelado: " + resultado.isPrestamoCancelado());

        return resultado;
    }

    // ========================================================================
    // §7.4 — Pago con aportes
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoPagoConAportes pagarConAportes(SolicitudPagoConAportes solicitud) throws Throwable {
        System.out.println("ProcesoPagoPrestamoService.pagarConAportes - Préstamo: "
            + (solicitud != null ? solicitud.getIdPrestamo() : null));

        if (solicitud == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": no se recibió el cuerpo de la solicitud");
        }
        Prestamo prestamo = validarPrestamoOperable(solicitud.getIdPrestamo(), solicitud.getUsuario());

        Entidad entidad = prestamo.getEntidad();
        if (entidad == null || entidad.getCodigo() == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": el préstamo " + prestamo.getCodigo()
                + " no tiene partícipe asociado; no se puede pagar con aportes");
        }

        double valorTotal = validarDesgloseAportes(solicitud.getAportes(), entidad);
        LocalDateTime fechaPagoHora = validarFechaNoFutura(solicitud.getFechaPago());

        double deudaTotal = motorPagoPrestamoService.calcularTotalPendientePrestamo(prestamo.getCodigo());
        if (deudaTotal <= TOLERANCIA) {
            throw new IncomeException(ERR_SIN_CUOTAS_PENDIENTES + ": el préstamo " + prestamo.getCodigo()
                + " no tiene cuotas pendientes con saldo");
        }
        if (valorTotal > deudaTotal + TOLERANCIA) {
            throw new IncomeException(ERR_VALOR_EXCEDE_DEUDA + ": El valor $" + formatoMonto(valorTotal)
                + " excede la deuda total $" + formatoMonto(deudaTotal)
                + " del préstamo; use la precancelación");
        }

        // 1. Evento
        EventoPrestamo evento = crearEvento(prestamo, TIPO_PAGO_APORTES, valorTotal, fechaPagoHora,
            solicitud.getUsuario(), solicitud.getObservacion());

        ContextoPago ctx = crearContexto(TIPO_PAGO_APORTES, solicitud.getUsuario(),
            solicitud.getObservacion(), fechaPagoHora, evento.getCodigo());

        // 2. Aplicación a cuotas
        ResultadoAplicacionPago resultado = motorPagoPrestamoService.aplicarPago(
            prestamo.getCodigo(), valorTotal, ctx);

        if (resultado.getValorAplicado() <= TOLERANCIA) {
            throw new IncomeException(ERR_SIN_CUOTAS_PENDIENTES + ": no se encontró ninguna cuota"
                + " pendiente donde aplicar el pago del préstamo " + prestamo.getCodigo());
        }

        // 3. Consumo de los aportes
        PagoPrestamo primerPago = primerPagoDelResultado(resultado);
        String glosa = "PAGO PRESTAMO " + prestamo.getCodigo() + " - Evento " + evento.getCodigo();
        List<MovimientoAporte> movimientos = consumirAportes(entidad, solicitud.getAportes(),
            fechaPagoHora, solicitud.getUsuario(), glosa, primerPago);

        // 4. Huella y hook
        registrarHuellaPrestamo(prestamo, TIPO_PAGO_APORTES, valorTotal, solicitud.getObservacion(),
            fechaPagoHora, solicitud.getUsuario());

        Long numeroAsiento = contabilidadPrestamoService.contabilizarPagoConAportes(resultado, movimientos, ctx);
        aplicarAsiento(evento, resultado, numeroAsiento);

        ResultadoPagoConAportes respuesta = new ResultadoPagoConAportes();
        respuesta.setResultado(resultado);
        respuesta.setMovimientosAporte(movimientos);

        System.out.println("  ✅ pagarConAportes OK - Evento: " + evento.getCodigo()
            + " - Aplicado: $" + resultado.getValorAplicado()
            + " - Movimientos de aporte: " + movimientos.size());

        return respuesta;
    }

    /**
     * Valida el desglose de aportes de §7.4 y devuelve el valor total.
     * Reglas: no vacío, cada valor &gt; 0, sin tipos repetidos, cada tipo vigente
     * (TipoAporte.estado = 1) y con saldo suficiente en la entidad.
     */
    private double validarDesgloseAportes(List<DesgloseAporte> aportes, Entidad entidad) throws Throwable {
        if (aportes == null || aportes.isEmpty()) {
            throw new IncomeException(ERR_DESGLOSE_INVALIDO + ": debe indicar al menos un tipo de aporte");
        }

        Set<Long> tiposVistos = new HashSet<>();
        double total = 0.0;

        for (DesgloseAporte renglon : aportes) {
            if (renglon == null || renglon.getIdTipoAporte() == null) {
                throw new IncomeException(ERR_DESGLOSE_INVALIDO + ": hay un renglón sin tipo de aporte");
            }
            double valor = redondear(renglon.getValor() != null ? renglon.getValor() : 0.0);
            if (valor <= 0.0) {
                throw new IncomeException(ERR_DESGLOSE_INVALIDO + ": el valor del tipo de aporte "
                    + renglon.getIdTipoAporte() + " debe ser mayor a cero");
            }
            if (!tiposVistos.add(renglon.getIdTipoAporte())) {
                throw new IncomeException(ERR_DESGLOSE_INVALIDO + ": desglose con tipo duplicado ("
                    + renglon.getIdTipoAporte() + ")");
            }

            TipoAporte tipo = tipoAporteDaoService.find(new TipoAporte(), renglon.getIdTipoAporte());
            if (tipo == null) {
                throw new IncomeException(ERR_TIPO_APORTE_NO_VIGENTE + ": no existe el tipo de aporte "
                    + renglon.getIdTipoAporte());
            }
            if (tipo.getEstado() == null || tipo.getEstado() != 1L) {
                throw new IncomeException(ERR_TIPO_APORTE_NO_VIGENTE + ": el tipo de aporte "
                    + tipo.getCodigo() + " (" + tipo.getNombre() + ") no está vigente");
            }

            double disponible = saldoAporteService.saldoPorEntidadYTipo(entidad.getCodigo(), tipo.getCodigo());
            if (disponible < valor - TOLERANCIA) {
                throw new IncomeException(ERR_SALDO_APORTES_INSUFICIENTE + ": el tipo "
                    + tipo.getCodigo() + " (" + tipo.getNombre() + ") tiene disponible $"
                    + formatoMonto(disponible) + " y se solicitaron $" + formatoMonto(valor));
            }

            total += valor;
        }

        return redondear(total);
    }

    /**
     * Crea la fila NEGATIVA en CRD.APRT y su PagoAporte por cada renglón del desglose (§7.4 3a-3c).
     *
     * La fila negativa nace con {@code saldo = 0}, {@code valorPagado = 0} y estado 4 (PAGADA)
     * para que el FIFO del proceso Petro ({@code selectMinAporteConSaldo}, que filtra
     * {@code saldo > 0.01}) nunca la devuelva.
     */
    private List<MovimientoAporte> consumirAportes(Entidad entidad, List<DesgloseAporte> aportes,
            LocalDateTime fecha, String usuario, String glosa, PagoPrestamo pagoPrestamo) throws Throwable {

        List<MovimientoAporte> movimientos = new ArrayList<>();

        for (DesgloseAporte renglon : aportes) {
            double valor = redondear(renglon.getValor());

            // a. GUARDARRAÍL anti-carrera: se revalida DENTRO de la transacción
            Double disponible = aporteDaoService.sumValorByEntidadYTipo(entidad.getCodigo(),
                renglon.getIdTipoAporte());
            double saldoActual = disponible != null ? disponible : 0.0;
            if (saldoActual < valor - TOLERANCIA) {
                throw new IncomeException(ERR_SALDO_APORTES_INSUFICIENTE + ": el saldo del tipo "
                    + renglon.getIdTipoAporte() + " cambió durante la operación (disponible $"
                    + formatoMonto(saldoActual) + ", solicitado $" + formatoMonto(valor) + ")");
            }

            TipoAporte tipo = tipoAporteDaoService.find(new TipoAporte(), renglon.getIdTipoAporte());

            // b. Fila NEGATIVA en APRT
            Aporte aporte = new Aporte();
            aporte.setEntidad(entidad);
            aporte.setFilial(entidad.getFilial());
            aporte.setTipoAporte(tipo);
            aporte.setValor(redondear(-valor));
            aporte.setValorPagado(0.0);
            aporte.setSaldo(0.0);
            aporte.setEstado(ESTADO_APORTE_CONSUMIDO);
            aporte.setIdAsoprep(null);
            aporte.setFechaTransaccion(fecha);
            aporte.setGlosa(glosa);
            aporte.setUsuarioRegistro(usuario);
            aporte.setFechaRegistro(LocalDateTime.now());
            // save() directo del DAO: AporteService.saveSingle forzaría estado = 1 y la fila
            // volvería a ser visible para el FIFO del proceso Petro.
            aporte = aporteDaoService.save(aporte, null);

            // c. PagoAporte
            PagoAporte pagoAporte = new PagoAporte();
            pagoAporte.setAporte(aporte);
            pagoAporte.setFilial(entidad.getFilial());
            pagoAporte.setValor(valor);
            pagoAporte.setFechaContable(fecha);
            pagoAporte.setNumeroAsiento(null);
            pagoAporte.setConcepto(glosa);
            pagoAporte.setUsuarioRegistro(usuario);
            pagoAporte.setFechaRegistro(LocalDateTime.now());
            pagoAporte.setEstado(1L);
            pagoAporte.setPagoPrestamo(pagoPrestamo);
            pagoAporte = pagoAporteDaoService.save(pagoAporte, null);

            MovimientoAporte movimiento = new MovimientoAporte();
            movimiento.setIdAporte(aporte.getCodigo());
            movimiento.setIdTipoAporte(renglon.getIdTipoAporte());
            movimiento.setValor(redondear(-valor));
            movimiento.setIdPagoAporte(pagoAporte.getCodigo());
            movimientos.add(movimiento);

            System.out.println("    💠 Aporte consumido - Tipo " + renglon.getIdTipoAporte()
                + ": $-" + formatoMonto(valor) + " (APRT " + aporte.getCodigo()
                + ", PGAP " + pagoAporte.getCodigo() + ")");
        }

        return movimientos;
    }

    // ========================================================================
    // §7.5 — Precancelación
    // ========================================================================

    @Override
    public SimulacionPrecancelacion simularPrecancelacion(Long idPrestamo, LocalDate fecha) throws Throwable {
        System.out.println("ProcesoPagoPrestamoService.simularPrecancelacion - Préstamo: " + idPrestamo
            + " - Fecha: " + fecha);

        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
        if (prestamo == null) {
            throw new IncomeException(ERR_PRESTAMO_NO_ENCONTRADO + ": no existe el préstamo " + idPrestamo);
        }
        return calcularPrecancelacion(prestamo, fecha != null ? fecha : LocalDate.now()).simulacion;
    }

    /** Cálculo canónico compartido por la simulación y la validación del POST (§7.5). */
    private static class CalculoPrecancelacion {
        private SimulacionPrecancelacion simulacion = new SimulacionPrecancelacion();
        private List<DetallePrestamo> exigibles = new ArrayList<>();
        private List<DetallePrestamo> futuras = new ArrayList<>();
    }

    private CalculoPrecancelacion calcularPrecancelacion(Prestamo prestamo, LocalDate fecha) throws Throwable {
        CalculoPrecancelacion calculo = new CalculoPrecancelacion();
        LocalDateTime finDelDia = fecha.atTime(23, 59, 59);

        List<DetallePrestamo> exigibles =
            detallePrestamoDaoService.selectCuotasExigibles(prestamo.getCodigo(), finDelDia);
        if (exigibles == null) {
            exigibles = new ArrayList<>();
        }

        double valorExigible = 0.0;
        List<CuotaExigible> detalleExigibles = new ArrayList<>();
        Set<Long> codigosExigibles = new HashSet<>();

        for (DetallePrestamo cuota : exigibles) {
            SaldosCuota saldos = motorPagoPrestamoService.calcularSaldosRealesCuota(cuota);
            if (saldos.getTotalPendiente() <= TOLERANCIA) {
                // La autocorrección la dejó PAGADA: ya no forma parte de la deuda exigible
                continue;
            }
            valorExigible += saldos.getTotalPendiente();
            codigosExigibles.add(cuota.getCodigo());
            calculo.exigibles.add(cuota);

            CuotaExigible dto = new CuotaExigible();
            dto.setIdCuota(cuota.getCodigo());
            dto.setNumeroCuota(cuota.getNumeroCuota());
            dto.setFechaVencimiento(cuota.getFechaVencimiento());
            dto.setPendiente(redondear(saldos.getTotalPendiente()));
            detalleExigibles.add(dto);
        }

        // Futuras = pendientes que no son exigibles a la fecha de corte
        List<DetallePrestamo> pendientes =
            detallePrestamoDaoService.selectCuotasPendientesByPrestamoOrdenadas(prestamo.getCodigo());
        double capitalFuturo = 0.0;
        double interesCondonado = 0.0;

        if (pendientes != null) {
            for (DetallePrestamo cuota : pendientes) {
                if (codigosExigibles.contains(cuota.getCodigo())) {
                    continue;
                }
                SaldosCuota saldos = motorPagoPrestamoService.calcularSaldosRealesCuota(cuota);
                if (saldos.getTotalPendiente() <= TOLERANCIA) {
                    continue;
                }
                calculo.futuras.add(cuota);
                capitalFuturo += saldos.getSaldoCapital();
                interesCondonado += (saldos.getTotalPendiente() - saldos.getSaldoCapital());
            }
        }

        valorExigible = redondear(valorExigible);
        capitalFuturo = redondear(capitalFuturo);

        SimulacionPrecancelacion simulacion = calculo.simulacion;
        simulacion.setIdPrestamo(prestamo.getCodigo());
        simulacion.setFecha(fecha);
        simulacion.setExigibles(detalleExigibles);
        simulacion.setValorExigible(valorExigible);
        simulacion.setCapitalFuturo(capitalFuturo);
        simulacion.setValorTotalPrecancelacion(redondear(valorExigible + capitalFuturo));
        simulacion.setCuotasAAnular(calculo.futuras.size());
        simulacion.setInteresCondonado(redondear(interesCondonado));

        System.out.println("  Precancelación al " + fecha + " - Exigible: $" + valorExigible
            + " - Capital futuro: $" + capitalFuturo
            + " - TOTAL: $" + simulacion.getValorTotalPrecancelacion()
            + " - Cuotas a anular: " + calculo.futuras.size());

        return calculo;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoPrecancelacion precancelar(SolicitudPrecancelacion solicitud) throws Throwable {
        System.out.println("ProcesoPagoPrestamoService.precancelar - Préstamo: "
            + (solicitud != null ? solicitud.getIdPrestamo() : null));

        if (solicitud == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": no se recibió el cuerpo de la solicitud");
        }
        Prestamo prestamo = validarPrestamoOperable(solicitud.getIdPrestamo(), solicitud.getUsuario());

        LocalDate fecha = solicitud.getFecha() != null ? solicitud.getFecha() : LocalDate.now();
        if (fecha.isAfter(LocalDate.now())) {
            throw new IncomeException(ERR_FECHA_INVALIDA + ": la fecha " + fecha + " es futura");
        }
        LocalDateTime fechaHora = fecha.isEqual(LocalDate.now()) ? LocalDateTime.now() : fecha.atStartOfDay();

        CalculoPrecancelacion calculo = calcularPrecancelacion(prestamo, fecha);
        SimulacionPrecancelacion simulacion = calculo.simulacion;

        if (calculo.futuras.isEmpty()) {
            throw new IncomeException(ERR_SIN_CUOTAS_FUTURAS
                + ": No hay cuotas futuras que precancelar; use pagarCuota");
        }

        // Componente de aportes (opcional)
        Entidad entidad = prestamo.getEntidad();
        double valorAportes = 0.0;
        boolean conAportes = solicitud.getAportes() != null && !solicitud.getAportes().isEmpty();
        if (conAportes) {
            if (entidad == null || entidad.getCodigo() == null) {
                throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": el préstamo " + prestamo.getCodigo()
                    + " no tiene partícipe asociado; no se puede precancelar con aportes");
            }
            valorAportes = validarDesgloseAportes(solicitud.getAportes(), entidad);
        }

        double valorEfectivo = redondear(solicitud.getValorEfectivo() != null ? solicitud.getValorEfectivo() : 0.0);
        if (valorEfectivo < 0.0) {
            throw new IncomeException(ERR_VALOR_INVALIDO + ": el valor en efectivo no puede ser negativo");
        }

        double valorEnviado = redondear(valorEfectivo + valorAportes);
        double valorTotal = simulacion.getValorTotalPrecancelacion();

        if (Math.abs(valorEnviado - valorTotal) > TOLERANCIA) {
            throw new IncomeException(ERR_MONTO_NO_COINCIDE + ": El valor enviado $"
                + formatoMonto(valorEnviado) + " no coincide con el valor de precancelación $"
                + formatoMonto(valorTotal));
        }

        // 1. Evento
        EventoPrestamo evento = crearEvento(prestamo, TIPO_PRECANCELACION, valorTotal, fechaHora,
            solicitud.getUsuario(), solicitud.getObservacion());

        ContextoPago ctx = crearContexto(TIPO_PRECANCELACION, solicitud.getUsuario(),
            solicitud.getObservacion(), fechaHora, evento.getCodigo());

        // 2. Pagar la deuda exigible cuota por cuota (sin cascada)
        double valorExigiblePagado = 0.0;
        Long primerPagoExigible = null;
        for (DetallePrestamo cuota : calculo.exigibles) {
            SaldosCuota saldos = motorPagoPrestamoService.calcularSaldosRealesCuota(cuota);
            if (saldos.getTotalPendiente() <= TOLERANCIA) {
                continue;
            }
            DetalleAplicacionCuota detalle = motorPagoPrestamoService.aplicarPagoACuota(
                cuota, saldos.getTotalPendiente(), ctx);
            valorExigiblePagado += detalle.getTotalAplicado();
            if (primerPagoExigible == null && detalle.getIdPagoPrestamo() != null) {
                primerPagoExigible = detalle.getIdPagoPrestamo();
            }
        }
        valorExigiblePagado = redondear(valorExigiblePagado);

        // 3-4. Cuota donde se registra el capital futuro (DTPRSLOT)
        double capitalFuturo = simulacion.getCapitalFuturo();
        DetallePrestamo ancla = detallePrestamoDaoService.selectUltimaCuotaPagada(prestamo.getCodigo());
        if (ancla == null) {
            // Caso extremo: precancelar un préstamo sin ninguna cuota pagada
            ancla = calculo.futuras.get(0);
        }
        ancla.setSaldoOtros(redondear(nvl(ancla.getSaldoOtros()) + capitalFuturo));
        ancla = detallePrestamoService.saveSingle(ancla);
        System.out.println("  ⚓ Capital futuro registrado en DTPRSLOT de la cuota #"
            + ancla.getNumeroCuota() + " (" + ancla.getCodigo() + "): $" + ancla.getSaldoOtros());

        // 5. PagoPrestamo del capital futuro (en saldoOtros, NO en capitalPagado)
        PagoPrestamo pagoCapitalFuturo = new PagoPrestamo();
        pagoCapitalFuturo.setPrestamo(prestamo);
        pagoCapitalFuturo.setDetallePrestamo(ancla);
        pagoCapitalFuturo.setNumeroCuota(ancla.getNumeroCuota());
        pagoCapitalFuturo.setFecha(fechaHora);
        pagoCapitalFuturo.setValor(capitalFuturo);
        pagoCapitalFuturo.setSaldoOtros(capitalFuturo);
        pagoCapitalFuturo.setCapitalPagado(0.0);
        pagoCapitalFuturo.setInteresPagado(0.0);
        pagoCapitalFuturo.setMoraPagada(0.0);
        pagoCapitalFuturo.setInteresVencidoPagado(0.0);
        pagoCapitalFuturo.setDesgravamen(0.0);
        pagoCapitalFuturo.setValorSeguroIncendio(0.0);
        pagoCapitalFuturo.setTipo(TIPO_PRECANCELACION);
        pagoCapitalFuturo.setObservacion((solicitud.getObservacion() != null ? solicitud.getObservacion() : "")
            + " [Evento: " + evento.getCodigo() + "]");
        pagoCapitalFuturo.setUsuarioRegistro(solicitud.getUsuario());
        pagoCapitalFuturo.setFechaRegistro(LocalDateTime.now());
        pagoCapitalFuturo.setEstado(1L);
        pagoCapitalFuturo.setIdEstado(1L);
        pagoCapitalFuturo.setAnulado(0L);
        pagoCapitalFuturo.setEventoPrestamo(evento);
        pagoCapitalFuturo = pagoPrestamoService.saveSingle(pagoCapitalFuturo);

        // 3. Cuotas futuras → CANCELADA_ANTICIPADA (7). NO se borran ni se historizan.
        int canceladas = 0;
        for (DetallePrestamo cuota : calculo.futuras) {
            cuota.setEstado((long) EstadoCuotaPrestamo.CANCELADA_ANTICIPADA);
            cuota.setIdEstado((long) EstadoCuotaPrestamo.CANCELADA_ANTICIPADA);
            cuota.setFechaPagado(null);
            detallePrestamoService.saveSingle(cuota);
            canceladas++;
        }
        System.out.println("  🚫 Cuotas futuras canceladas anticipadamente: " + canceladas);

        // 6. Componente de aportes
        List<MovimientoAporte> movimientos = new ArrayList<>();
        if (conAportes) {
            String glosa = "PAGO PRESTAMO " + prestamo.getCodigo() + " - Evento " + evento.getCodigo();
            // Los PagoAporte se enlazan al primer PGPR de la deuda exigible; si no hubo cuotas
            // exigibles, al PGPR del capital futuro. Los demás se recuperan por el evento.
            PagoPrestamo referencia = primerPagoExigible != null
                ? pagoPrestamoDaoService.find(new PagoPrestamo(), primerPagoExigible)
                : pagoCapitalFuturo;
            movimientos = consumirAportes(entidad, solicitud.getAportes(), fechaHora,
                solicitud.getUsuario(), glosa, referencia);
        }

        // 7. Préstamo → CANCELADO_ANTICIPADO (4). NUNCA se toca ESPSCDGO ni fechaFin.
        prestamo.setIdEstado(Long.valueOf(EstadoPrestamo.CANCELADO_ANTICIPADO));
        prestamo.setFechaModificacion(LocalDateTime.now());
        prestamoDaoService.save(prestamo, prestamo.getCodigo());

        registrarHuellaPrestamo(prestamo, TIPO_PRECANCELACION, valorTotal, solicitud.getObservacion(),
            fechaHora, solicitud.getUsuario());

        // 8. Hook contable
        Long numeroAsiento = contabilidadPrestamoService.contabilizarPrecancelacion(evento);
        if (numeroAsiento != null) {
            evento.setNumeroAsiento(numeroAsiento);
            eventoPrestamoService.saveSingle(evento);
            pagoCapitalFuturo.setAsiento(numeroAsiento);
            pagoPrestamoService.saveSingle(pagoCapitalFuturo);
        }

        ResultadoPrecancelacion resultado = new ResultadoPrecancelacion();
        resultado.setIdPrestamo(prestamo.getCodigo());
        resultado.setIdEvento(evento.getCodigo());
        resultado.setValorExigiblePagado(valorExigiblePagado);
        resultado.setCapitalPrecancelado(capitalFuturo);
        resultado.setValorTotalPrecancelacion(valorTotal);
        resultado.setCuotasCanceladasAnticipadas(canceladas);
        resultado.setEstadoFinalPrestamo(prestamo.getIdEstado());
        resultado.setIdCuotaConSaldoOtros(ancla.getCodigo());
        resultado.setIdPagoPrestamoCapitalFuturo(pagoCapitalFuturo.getCodigo());
        resultado.setMovimientosAporte(movimientos);

        System.out.println("  ✅ Precancelación OK - Evento: " + evento.getCodigo()
            + " - Exigible pagado: $" + valorExigiblePagado
            + " - Capital precancelado: $" + capitalFuturo);

        return resultado;
    }

    // ========================================================================
    // §7.6 — Reverso
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoAnulacion anularOperacion(SolicitudAnulacion solicitud) throws Throwable {
        System.out.println("ProcesoPagoPrestamoService.anularOperacion - Evento: "
            + (solicitud != null ? solicitud.getIdEvento() : null));

        if (solicitud == null || solicitud.getIdEvento() == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": idEvento es obligatorio");
        }
        if (solicitud.getUsuario() == null || solicitud.getUsuario().trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": usuario es obligatorio");
        }
        if (solicitud.getMotivo() == null || solicitud.getMotivo().trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": motivo es obligatorio");
        }

        EventoPrestamo evento = eventoPrestamoDaoService.find(new EventoPrestamo(), solicitud.getIdEvento());
        if (evento == null) {
            throw new IncomeException(ERR_EVENTO_NO_ENCONTRADO + ": no existe el evento "
                + solicitud.getIdEvento());
        }
        if (evento.getEstado() == null || evento.getEstado() != 1L) {
            throw new IncomeException(ERR_EVENTO_YA_ANULADO + ": el evento " + evento.getCodigo()
                + " ya fue anulado");
        }

        Prestamo prestamo = evento.getPrestamo();
        if (prestamo == null) {
            throw new IncomeException(ERR_PRESTAMO_NO_ENCONTRADO + ": el evento " + evento.getCodigo()
                + " no tiene préstamo asociado");
        }

        // El reverso es LIFO: nada posterior vigente sobre el mismo préstamo
        List<EventoPrestamo> posteriores = eventoPrestamoDaoService
            .selectVigentesPosterioresByPrestamo(prestamo.getCodigo(), evento.getCodigo());
        if (posteriores != null && !posteriores.isEmpty()) {
            throw new IncomeException(ERR_EVENTO_POSTERIOR_VIGENTE
                + ": Anule primero las operaciones posteriores (evento " + posteriores.get(0).getCodigo() + ")");
        }

        LocalDateTime ahora = LocalDateTime.now();
        String tipo = evento.getTipoOperacion();

        ResultadoAnulacion resultado = new ResultadoAnulacion();
        resultado.setIdEvento(evento.getCodigo());
        resultado.setIdPrestamo(prestamo.getCodigo());
        resultado.setTipoOperacion(tipo);

        // 1. Anular los PagoPrestamo del evento
        List<PagoPrestamo> pagos = pagoPrestamoDaoService.selectByEvento(evento.getCodigo());
        if (pagos == null) {
            pagos = new ArrayList<>();
        }
        Map<Long, DetallePrestamo> cuotasAfectadas = new LinkedHashMap<>();
        double saldoOtrosARevertir = 0.0;

        for (PagoPrestamo pago : pagos) {
            pago.setAnulado(1L);
            pago.setUsuarioAnulacion(solicitud.getUsuario());
            pago.setFechaAnulacion(ahora);
            pago.setMotivoAnulacion(solicitud.getMotivo());
            pagoPrestamoService.saveSingle(pago);

            if (pago.getDetallePrestamo() != null && pago.getDetallePrestamo().getCodigo() != null) {
                cuotasAfectadas.put(pago.getDetallePrestamo().getCodigo(), pago.getDetallePrestamo());
            }
            saldoOtrosARevertir += nvl(pago.getSaldoOtros());
        }
        resultado.setPagosAnulados(pagos.size());
        System.out.println("  ↩️ PagoPrestamo anulados: " + pagos.size());

        // 2. Revertir el saldoOtros que el evento hubiera acumulado (abono / capital futuro)
        if (saldoOtrosARevertir > TOLERANCIA) {
            for (PagoPrestamo pago : pagos) {
                double aRevertir = nvl(pago.getSaldoOtros());
                if (aRevertir <= TOLERANCIA || pago.getDetallePrestamo() == null) {
                    continue;
                }
                DetallePrestamo cuota = pago.getDetallePrestamo();
                cuota.setSaldoOtros(Math.max(0, redondear(nvl(cuota.getSaldoOtros()) - aRevertir)));
                detallePrestamoService.saveSingle(cuota);
                System.out.println("    ⚓ DTPRSLOT revertido en la cuota #" + cuota.getNumeroCuota()
                    + ": -$" + formatoMonto(aRevertir));
            }
        }

        int cuotasRecalculadas = 0;
        int cuotasRestauradas = 0;
        int cuotasEliminadas = 0;

        if (TIPO_ABONO_CAPITAL.equals(tipo)) {
            // 3. Borrar las cuotas GENERADAS por el abono y restaurar las originales desde HDTP
            Double minNumero = histDetallePrestamoDaoService.selectMinNumeroCuotaByEvento(evento.getCodigo());
            List<HistDetallePrestamo> historicas =
                histDetallePrestamoDaoService.selectByEvento(evento.getCodigo());

            if (minNumero != null) {
                List<DetallePrestamo> vivas = detallePrestamoDaoService.selectByPrestamo(prestamo.getCodigo());
                List<DetallePrestamo> aEliminar = new ArrayList<>();
                for (DetallePrestamo cuota : vivas) {
                    if (cuota.getNumeroCuota() != null && cuota.getNumeroCuota() >= minNumero) {
                        aEliminar.add(cuota);
                    }
                }
                // Ninguna cuota generada puede tener pagos vigentes
                for (DetallePrestamo cuota : aEliminar) {
                    Long vigentes = pagoPrestamoDaoService.contarVigentesByIdDetallePrestamo(cuota.getCodigo());
                    if (vigentes != null && vigentes > 0L) {
                        throw new IncomeException(ERR_PAGOS_SOBRE_TABLA_RECALCULADA
                            + ": hay pagos sobre la tabla recalculada (cuota #" + cuota.getNumeroCuota()
                            + "); anúlelos primero");
                    }
                }
                for (DetallePrestamo cuota : aEliminar) {
                    // Los PGPR anulados que apunten a la cuota impedirían el DELETE por FK:
                    // se re-apuntan a la cuota restaurada más adelante no es posible, así que
                    // se exige que no existan (los del propio abono viven en la cuota ancla).
                    List<PagoPrestamo> pagosCuota = pagoPrestamoDaoService.selectByIdDetallePrestamo(cuota.getCodigo());
                    if (pagosCuota != null && !pagosCuota.isEmpty()) {
                        throw new IncomeException(ERR_PAGOS_SOBRE_TABLA_RECALCULADA
                            + ": la cuota #" + cuota.getNumeroCuota() + " tiene pagos registrados"
                            + " (incluso anulados) y no puede eliminarse al reversar el abono");
                    }
                    detallePrestamoDaoService.remove(cuota, cuota.getCodigo());
                    cuotasEliminadas++;
                }
            }

            // Restaurar desde HDTP. El DTPRCDGO cambia: el original queda en HDTP.DTPRCDGO.
            if (historicas != null) {
                for (HistDetallePrestamo historica : historicas) {
                    DetallePrestamo cuota = restaurarDesdeHistorico(historica, prestamo);
                    // DAO directo: DetallePrestamoService.saveSingle fuerza estado = 1 en las
                    // filas nuevas y perdería el estado original que se está restaurando.
                    detallePrestamoDaoService.save(cuota, null);
                    cuotasRestauradas++;
                }
            }
            System.out.println("  ♻️ Cuotas eliminadas: " + cuotasEliminadas
                + " - Cuotas restauradas desde HDTP: " + cuotasRestauradas);

            // Restaurar plazo y valor de cuota anteriores
            prestamo = prestamoService.actualizarCamposDesdeTabla(prestamo.getCodigo());
            if (evento.getPlazoAnterior() != null) {
                prestamo.setPlazo(evento.getPlazoAnterior());
            }
            if (evento.getCuotaAnterior() != null) {
                prestamo.setValorCuota(evento.getCuotaAnterior());
            }

        } else {
            // PAGO_MANUAL, PAGO_APORTES y PRECANCELACION: reconstruir cuotas desde los pagos
            for (DetallePrestamo cuota : cuotasAfectadas.values()) {
                motorPagoPrestamoService.recalcularCuotaDesdePagos(cuota);
                cuotasRecalculadas++;
            }

            if (TIPO_PRECANCELACION.equals(tipo)) {
                // Las cuotas que quedaron en 7 vuelven a PENDIENTE/EN_MORA según su vencimiento
                List<DetallePrestamo> todas = detallePrestamoDaoService.selectByPrestamo(prestamo.getCodigo());
                for (DetallePrestamo cuota : todas) {
                    if (cuota.getEstado() != null
                            && cuota.getEstado() == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
                        motorPagoPrestamoService.recalcularCuotaDesdePagos(cuota);
                        cuotasRecalculadas++;
                    }
                }
            }
        }

        // 4. Contra-movimientos de aportes (si el evento consumió aportes)
        int movimientosRevertidos = revertirAportes(pagos, evento, solicitud.getUsuario(), ahora);
        resultado.setMovimientosAporteRevertidos(movimientosRevertidos);

        // 5. Estado del préstamo
        Long estadoPrevio = prestamo.getIdEstado();
        Long cuotasPendientes = detallePrestamoDaoService.contarCuotasPendientesByPrestamo(prestamo.getCodigo());

        if (TIPO_PRECANCELACION.equals(tipo)
                && estadoPrevio != null && estadoPrevio == EstadoPrestamo.CANCELADO_ANTICIPADO) {
            prestamo.setIdEstado(Long.valueOf(EstadoPrestamo.VIGENTE));
        } else if (estadoPrevio != null && estadoPrevio == EstadoPrestamo.CANCELADO
                && cuotasPendientes != null && cuotasPendientes > 0L) {
            // Se reabre SOLO desde CANCELADO(3); nunca se reabre automáticamente 4 ni 5
            prestamo.setIdEstado(Long.valueOf(EstadoPrestamo.VIGENTE));
        }
        prestamo.setFechaModificacion(ahora);
        prestamoDaoService.save(prestamo, prestamo.getCodigo());

        // 6. Cerrar el evento y dejar huella
        evento.setEstado(0L);
        evento.setUsuarioAnulacion(solicitud.getUsuario());
        evento.setFechaAnulacion(ahora);
        evento.setMotivoAnulacion(solicitud.getMotivo());
        eventoPrestamoService.saveSingle(evento);

        registrarHuellaPrestamo(prestamo, "REVERSO " + tipo, nvl(evento.getValor()),
            solicitud.getMotivo(), ahora, solicitud.getUsuario());

        contabilidadPrestamoService.contabilizarReverso(evento);

        resultado.setCuotasRecalculadas(cuotasRecalculadas);
        resultado.setCuotasRestauradas(cuotasRestauradas);
        resultado.setCuotasEliminadas(cuotasEliminadas);
        resultado.setEstadoFinalPrestamo(prestamo.getIdEstado());

        System.out.println("  ✅ Evento " + evento.getCodigo() + " (" + tipo + ") anulado"
            + " - Pagos: " + resultado.getPagosAnulados()
            + " - Cuotas recalculadas: " + cuotasRecalculadas
            + " - Estado del préstamo: " + estadoPrevio + " → " + prestamo.getIdEstado());

        return resultado;
    }

    /**
     * Crea el contra-movimiento POSITIVO en CRD.APRT por cada PagoAporte de los pagos anulados y
     * marca el PagoAporte con estado 0. Se inserta un contra-movimiento en lugar de borrar: APRT
     * es append-only para los reportes de aportes.
     */
    private int revertirAportes(List<PagoPrestamo> pagos, EventoPrestamo evento, String usuario,
            LocalDateTime ahora) throws Throwable {

        int revertidos = 0;
        String glosa = "REVERSO EVENTO " + evento.getCodigo();

        for (PagoPrestamo pago : pagos) {
            List<PagoAporte> pagosAporte = pagoAporteDaoService.selectByPagoPrestamo(pago.getCodigo());
            if (pagosAporte == null || pagosAporte.isEmpty()) {
                continue;
            }
            for (PagoAporte pagoAporte : pagosAporte) {
                if (pagoAporte.getEstado() != null && pagoAporte.getEstado() == 0L) {
                    continue; // ya revertido
                }
                Aporte original = pagoAporte.getAporte();
                if (original == null) {
                    continue;
                }

                Aporte contra = new Aporte();
                contra.setEntidad(original.getEntidad());
                contra.setFilial(original.getFilia());
                contra.setTipoAporte(original.getTipoAporte());
                contra.setValor(redondear(Math.abs(nvl(pagoAporte.getValor()))));
                contra.setValorPagado(0.0);
                contra.setSaldo(0.0);
                contra.setEstado(ESTADO_APORTE_CONSUMIDO);
                contra.setIdAsoprep(null);
                contra.setFechaTransaccion(ahora);
                contra.setGlosa(glosa);
                contra.setUsuarioRegistro(usuario);
                contra.setFechaRegistro(ahora);
                contra = aporteDaoService.save(contra, null);

                pagoAporte.setEstado(0L);
                pagoAporteDaoService.save(pagoAporte, pagoAporte.getCodigo());

                revertidos++;
                System.out.println("    💠 Contra-movimiento de aporte creado: APRT " + contra.getCodigo()
                    + " por $" + formatoMonto(contra.getValor()));
            }
        }
        return revertidos;
    }

    /** Reconstruye una cuota de DTPR a partir de su espejo en HDTP (el DTPRCDGO será nuevo). */
    private DetallePrestamo restaurarDesdeHistorico(HistDetallePrestamo h, Prestamo prestamo) {
        DetallePrestamo cuota = new DetallePrestamo();
        cuota.setPrestamo(prestamo);
        cuota.setNumeroCuota(h.getNumeroCuota());
        cuota.setFechaVencimiento(h.getFechaVencimiento());
        cuota.setCapital(h.getCapital());
        cuota.setInteres(h.getInteres());
        cuota.setMora(h.getMora());
        cuota.setInteresVencido(h.getInteresVencido());
        cuota.setSaldoCapital(h.getSaldoCapital());
        cuota.setSaldoInteres(h.getSaldoInteres());
        cuota.setSaldoMora(h.getSaldoMora());
        cuota.setSaldoInteresVencido(h.getSaldoInteresVencido());
        cuota.setFechaPagado(h.getFechaPagado());
        cuota.setAbono(h.getAbono());
        cuota.setCapitalPagado(h.getCapitalPagado());
        cuota.setInteresPagado(h.getInteresPagado());
        cuota.setDesgravamen(h.getDesgravamen());
        cuota.setCuota(h.getCuota());
        cuota.setSaldo(h.getSaldo());
        cuota.setSaldoOtros(h.getSaldoOtros());
        cuota.setDesgravamenFirmado(h.getDesgravamenFirmado());
        cuota.setDesgravamenDiferido(h.getDesgravamenDiferido());
        cuota.setDesgravamenOriginal(h.getDesgravamenOriginal());
        cuota.setValorDiferido(h.getValorDiferido());
        cuota.setTotal(h.getTotal());
        cuota.setMoraPagado(h.getMoraPagado());
        cuota.setDesgravamenPagado(h.getDesgravamenPagado());
        cuota.setInteresVendidoPagado(h.getInteresVendidoPagado());
        cuota.setMoraCalculada(h.getMoraCalculada());
        cuota.setDiasMora(h.getDiasMora());
        cuota.setEstado(h.getEstado());
        cuota.setIdEstado(h.getEstado());
        cuota.setFechaRegistro(h.getFechaRegistro());
        cuota.setUsuarioRegistro(h.getUsuarioRegistro());
        cuota.setCodigoExterno(h.getCodigoExterno());
        cuota.setOtrosSeguros(h.getOtrosSeguros());
        cuota.setTotalConSeguro(h.getTotalConSeguro());
        cuota.setValorSeguroIncendio(h.getValorSeguroIncendio());
        cuota.setSaldoInicialCapital(h.getSaldoInicialCapital());
        return cuota;
    }

    // ========================================================================
    // Helpers compartidos
    // ========================================================================

    /** Valida que el préstamo exista, no esté en estado terminal y que venga el usuario. */
    private Prestamo validarPrestamoOperable(Long idPrestamo, String usuario) throws Throwable {
        if (idPrestamo == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": idPrestamo es obligatorio");
        }
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": usuario es obligatorio");
        }
        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
        if (prestamo == null) {
            throw new IncomeException(ERR_PRESTAMO_NO_ENCONTRADO + ": no existe el préstamo " + idPrestamo);
        }
        if (esEstadoTerminalPrestamo(prestamo.getIdEstado())) {
            throw new IncomeException(ERR_ESTADO_NO_PERMITE + ": el préstamo " + prestamo.getCodigo()
                + " está en estado " + prestamo.getIdEstado() + " (terminal) y no admite la operación");
        }
        return prestamo;
    }

    /** Convierte la fecha de negocio a fecha/hora validando que no sea futura. */
    private LocalDateTime validarFechaNoFutura(LocalDate fecha) throws Throwable {
        LocalDate efectiva = fecha != null ? fecha : LocalDate.now();
        if (efectiva.isAfter(LocalDate.now())) {
            throw new IncomeException(ERR_FECHA_INVALIDA + ": la fecha " + efectiva + " es futura");
        }
        // Si es hoy se conserva la hora del reloj (igual que el proceso petro)
        return efectiva.isEqual(LocalDate.now()) ? LocalDateTime.now() : efectiva.atStartOfDay();
    }

    private ContextoPago crearContexto(String tipoPago, String usuario, String observacion,
            LocalDateTime fechaPago, Long idEvento) {
        ContextoPago ctx = new ContextoPago();
        ctx.setTipoPago(tipoPago);
        ctx.setUsuario(usuario);
        ctx.setObservacion(observacion);
        ctx.setFechaPago(fechaPago);
        ctx.setIdEvento(idEvento);
        return ctx;
    }

    /** Crea el EventoPrestamo cabecera. Cubre todos sus campos NOT NULL. */
    private EventoPrestamo crearEvento(Prestamo prestamo, String tipoOperacion, double valor,
            LocalDateTime fecha, String usuario, String observacion) throws Throwable {

        EventoPrestamo evento = new EventoPrestamo();
        evento.setPrestamo(prestamo);
        evento.setTipoOperacion(tipoOperacion);
        evento.setValor(redondear(valor));
        evento.setFecha(fecha);
        evento.setObservacion(observacion);
        evento.setUsuario(usuario);
        evento.setFechaRegistro(LocalDateTime.now());
        evento.setEstado(1L);

        evento = eventoPrestamoService.saveSingle(evento);
        System.out.println("  💾 EventoPrestamo creado: " + evento.getCodigo() + " (" + tipoOperacion + ")");
        return evento;
    }

    /** Primer PagoPrestamo del resultado, para enlazar los PagoAporte (§7.4 paso 3c). */
    private PagoPrestamo primerPagoDelResultado(ResultadoAplicacionPago resultado) throws Throwable {
        if (resultado == null) {
            return null;
        }
        for (DetalleAplicacionCuota detalle : resultado.getCuotasAfectadas()) {
            if (detalle.getIdPagoPrestamo() != null) {
                return pagoPrestamoDaoService.find(new PagoPrestamo(), detalle.getIdPagoPrestamo());
            }
        }
        return null;
    }

    /** Estampa el número de asiento en el evento y en los PagoPrestamo de la operación. */
    private void aplicarAsiento(EventoPrestamo evento, ResultadoAplicacionPago resultado, Long numeroAsiento)
            throws Throwable {
        if (numeroAsiento == null) {
            return;
        }

        evento.setNumeroAsiento(numeroAsiento);
        eventoPrestamoService.saveSingle(evento);

        if (resultado == null) {
            return;
        }
        for (DetalleAplicacionCuota detalle : resultado.getCuotasAfectadas()) {
            if (detalle.getIdPagoPrestamo() == null) {
                continue;
            }
            PagoPrestamo pago = pagoPrestamoDaoService.find(new PagoPrestamo(), detalle.getIdPagoPrestamo());
            if (pago != null) {
                pago.setAsiento(numeroAsiento);
                pagoPrestamoService.saveSingle(pago);
            }
        }
    }

    @Override
    public void registrarHuellaPrestamo(Prestamo prestamo, String tipoOperacion, double valor,
            String observacion, LocalDateTime fecha, String usuario) throws Throwable {

        if (prestamo == null) {
            return;
        }

        String linea = "\n[" + fecha.format(FORMATO_HUELLA) + " " + usuario + "] "
            + tipoOperacion + " $" + formatoMonto(valor)
            + " - " + (observacion != null ? observacion : "");

        String actual = prestamo.getObservacion() != null ? prestamo.getObservacion() : "";
        prestamo.setObservacion(truncarPorLaIzquierda(actual + linea, MAX_BYTES_OBSERVACION));
        prestamo.setFechaModificacion(LocalDateTime.now());
        prestamoDaoService.save(prestamo, prestamo.getCodigo());
    }

    /**
     * Recorta el texto por la izquierda hasta que su representación UTF-8 quepa en
     * {@code maxBytes}, conservando el final (lo más reciente).
     *
     * PRSTOBSR es VARCHAR2(2000) y con semántica BYTE en Oracle 2000 caracteres acentuados
     * superan los 2000 bytes: daría ORA-12899 y revertiría toda la operación.
     */
    private String truncarPorLaIzquierda(String texto, int maxBytes) {
        if (texto == null) {
            return null;
        }
        if (texto.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
            return texto;
        }
        int desde = 0;
        while (desde < texto.length()
                && texto.substring(desde).getBytes(StandardCharsets.UTF_8).length > maxBytes) {
            desde++;
        }
        return texto.substring(desde);
    }

    /** Estados terminales del préstamo: 3, 4 y 5. */
    private boolean esEstadoTerminalPrestamo(Long idEstado) {
        return idEstado != null
            && (idEstado == EstadoPrestamo.CANCELADO
             || idEstado == EstadoPrestamo.CANCELADO_ANTICIPADO
             || idEstado == EstadoPrestamo.CANCELADO_POR_NOVACION);
    }

    /** Monto con 2 decimales y punto decimal, independiente del locale del servidor. */
    private String formatoMonto(double valor) {
        return String.format(Locale.US, "%.2f", valor);
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

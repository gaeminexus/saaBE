package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.EventoPrestamoDaoService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.DetallePrestamoService;
import com.saa.ejb.crd.service.MotorPagoPrestamoService;
import com.saa.ejb.crd.service.PagoPrestamoService;
import com.saa.ejb.crd.service.dto.ContextoPago;
import com.saa.ejb.crd.service.dto.DetalleAplicacionCuota;
import com.saa.ejb.crd.service.dto.ResultadoAplicacionPago;
import com.saa.ejb.crd.service.dto.SaldosCuota;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.EventoPrestamo;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoPrestamo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación del motor de pagos de préstamos (§6 de
 * docs/logica-negocio/crd/ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md).
 *
 * Código NUEVO: es una copia adaptada del comportamiento de
 * {@code CargaArchivoPetroServiceImpl} (calcularSaldosRealesCuota, procesarPagoCuota,
 * procesarExcedenteASiguienteCuota, verificarYActualizarEstadoPrestamo, crearRegistroPago),
 * extendida con mora e interés vencido y con la prelación de 6 componentes. Aquel servicio
 * NO se modifica; la convergencia de ambos es una fase futura.
 *
 * @author Sistema SAA
 * @since 2026-08-14
 */
@Stateless
public class MotorPagoPrestamoServiceImpl implements MotorPagoPrestamoService {

    /** Tolerancia de UN CENTAVO para toda comparación de aplicación (§6.1) */
    private static final double TOLERANCIA = 0.01;

    /** Tope de seguridad del bucle de cascada */
    private static final int MAX_ITERACIONES = 100;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private DetallePrestamoService detallePrestamoService;

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private PagoPrestamoService pagoPrestamoService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private EventoPrestamoDaoService eventoPrestamoDaoService;

    // ========================================================================
    // §6.2 — Saldos reales de una cuota (con autocorrección)
    // ========================================================================

    @Override
    public SaldosCuota calcularSaldosRealesCuota(DetallePrestamo cuota) throws Throwable {
        System.out.println("MotorPagoPrestamoService.calcularSaldosRealesCuota - Cuota: "
            + (cuota != null ? cuota.getCodigo() : null));

        SaldosCuota saldos = new SaldosCuota();
        if (cuota == null) {
            return saldos;
        }

        // Valores originales de la cuota
        double desgravamenOriginal = nullSafe(cuota.getDesgravamen());
        double moraOriginal        = nullSafe(cuota.getMora());
        double ivOriginal          = nullSafe(cuota.getInteresVencido());
        double interesOriginal     = nullSafe(cuota.getInteres());
        double capitalOriginal     = nullSafe(cuota.getCapital());
        double seguroOriginal      = nullSafe(cuota.getValorSeguroIncendio());

        // ✅ PGPR es la fuente de verdad, y SOLO los pagos vigentes (los anulados no cuentan)
        List<PagoPrestamo> pagos = pagoPrestamoDaoService.selectVigentesByIdDetallePrestamo(cuota.getCodigo());

        if (pagos == null || pagos.isEmpty()) {
            // Sin pagos: los saldos son los valores originales
            saldos.setSaldoDesgravamen(desgravamenOriginal);
            saldos.setSaldoMora(moraOriginal);
            saldos.setSaldoInteresVencido(ivOriginal);
            saldos.setSaldoInteres(interesOriginal);
            saldos.setSaldoCapital(capitalOriginal);
            saldos.setSaldoSeguroIncendio(seguroOriginal);

            // ⚠️ DTPRTTLL YA INCLUYE la mora: la escribe el proceso diario
            // (ProcesoMoraPrestamoService) junto con DTPRMRAA. NO se la puede volver a sumar
            // acá o se cobraría dos veces. Solo se agrega el interés vencido, que hoy ningún
            // proceso alimenta y por eso vale 0.
            if (cuota.getTotal() != null) {
                saldos.setTotalPendiente(redondear(cuota.getTotal() + ivOriginal));
            } else {
                // Dato legacy sin DTPRTTLL: fallback a la suma de los 6 componentes
                System.out.println("    ⚠️ Cuota #" + cuota.getNumeroCuota()
                    + " sin DTPRTTLL - usando la suma de los 6 componentes");
                saldos.setTotalPendiente(redondear(desgravamenOriginal + moraOriginal + ivOriginal
                    + interesOriginal + capitalOriginal + seguroOriginal));
            }
            return saldos;
        }

        // Acumular lo pagado por componente
        double desgravamenPagado = 0.0;
        double moraPagada        = 0.0;
        double ivPagado          = 0.0;
        double interesPagado     = 0.0;
        double capitalPagado     = 0.0;
        double seguroPagado      = 0.0;

        for (PagoPrestamo pago : pagos) {
            desgravamenPagado += nullSafe(pago.getDesgravamen());
            moraPagada        += nullSafe(pago.getMoraPagada());
            ivPagado          += nullSafe(pago.getInteresVencidoPagado());
            interesPagado     += nullSafe(pago.getInteresPagado());
            capitalPagado     += nullSafe(pago.getCapitalPagado());
            seguroPagado      += nullSafe(pago.getValorSeguroIncendio());
        }

        saldos.setSaldoDesgravamen(Math.max(0, redondear(desgravamenOriginal - desgravamenPagado)));
        saldos.setSaldoMora(Math.max(0, redondear(moraOriginal - moraPagada)));
        saldos.setSaldoInteresVencido(Math.max(0, redondear(ivOriginal - ivPagado)));
        saldos.setSaldoInteres(Math.max(0, redondear(interesOriginal - interesPagado)));
        saldos.setSaldoCapital(Math.max(0, redondear(capitalOriginal - capitalPagado)));
        saldos.setSaldoSeguroIncendio(Math.max(0, redondear(seguroOriginal - seguroPagado)));
        saldos.setTotalPendiente(redondear(
              saldos.getSaldoDesgravamen()
            + saldos.getSaldoMora()
            + saldos.getSaldoInteresVencido()
            + saldos.getSaldoInteres()
            + saldos.getSaldoCapital()
            + saldos.getSaldoSeguroIncendio()));

        // ✅ AUTOCORRECCIÓN: la cuota está liquidada según PGPR pero su estado dice otra cosa
        if (saldos.getTotalPendiente() <= TOLERANCIA && !esEstadoLiquidado(cuota.getEstado())) {
            System.out.println("    ⚠️ Cuota #" + cuota.getNumeroCuota()
                + " liquidada según PagoPrestamo - Actualizando estado a PAGADA");

            aplicarEstadoCuota(cuota, (long) EstadoCuotaPrestamo.PAGADA);

            // ✅ Respetar la fecha de pago existente; solo se establece si falta
            if (cuota.getFechaPagado() == null) {
                cuota.setFechaPagado(LocalDateTime.now());
            } else {
                System.out.println("    ℹ️ Respetando fecha de pago existente: " + cuota.getFechaPagado());
            }

            cuota.setCapitalPagado(redondear(capitalPagado));
            cuota.setInteresPagado(redondear(interesPagado));
            cuota.setDesgravamenPagado(redondear(desgravamenPagado));
            cuota.setMoraPagado(redondear(moraPagada));
            cuota.setInteresVendidoPagado(redondear(ivPagado));

            // saldoCapital = saldoInicialCapital − capitalPagado (NO se pone en 0)
            cuota.setSaldoCapital(Math.max(0, redondear(nullSafe(cuota.getSaldoInicialCapital()) - capitalPagado)));
            cuota.setSaldoInteres(0.0);
            cuota.setSaldoMora(0.0);
            cuota.setSaldoInteresVencido(0.0);

            double totalConMoraIV = totalConMoraIV(cuota);
            double totalPagado = capitalPagado + interesPagado + desgravamenPagado
                + moraPagada + ivPagado + seguroPagado;
            cuota.setSaldo(Math.max(0, redondear(totalConMoraIV - totalPagado)));

            detallePrestamoService.saveSingle(cuota);
        }

        return saldos;
    }

    // ========================================================================
    // Deuda total pendiente del préstamo
    // ========================================================================

    @Override
    public double calcularTotalPendientePrestamo(Long idPrestamo) throws Throwable {
        System.out.println("MotorPagoPrestamoService.calcularTotalPendientePrestamo - Préstamo: " + idPrestamo);

        double total = 0.0;
        List<DetallePrestamo> pendientes =
            detallePrestamoDaoService.selectCuotasPendientesByPrestamoOrdenadas(idPrestamo);

        if (pendientes != null) {
            for (DetallePrestamo cuota : pendientes) {
                total += calcularSaldosRealesCuota(cuota).getTotalPendiente();
            }
        }

        total = redondear(total);
        System.out.println("  Deuda total pendiente del préstamo " + idPrestamo + ": $" + total);
        return total;
    }

    // ========================================================================
    // §6.3 — Aplicación en cascada
    // ========================================================================

    @Override
    public ResultadoAplicacionPago aplicarPago(Long idPrestamo, double valor, ContextoPago ctx) throws Throwable {
        System.out.println("MotorPagoPrestamoService.aplicarPago - Préstamo: " + idPrestamo
            + " - Valor: $" + valor + " - Tipo: " + (ctx != null ? ctx.getTipoPago() : null));

        // find() (em.find) devuelve null si no existe; selectById usa getSingleResult y
        // lanzaría NoResultException, que el REST no podría mapear a un 404 limpio.
        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
        if (prestamo == null) {
            throw new IncomeException("PRESTAMO_NO_ENCONTRADO: no existe el préstamo " + idPrestamo);
        }
        if (esEstadoTerminalPrestamo(prestamo.getIdEstado())) {
            throw new IncomeException("El préstamo " + idPrestamo + " está en un estado terminal ("
                + prestamo.getIdEstado() + ") y no admite pagos");
        }

        ResultadoAplicacionPago resultado = new ResultadoAplicacionPago();
        resultado.setIdPrestamo(idPrestamo);
        resultado.setIdEvento(ctx != null ? ctx.getIdEvento() : null);
        resultado.setValorRecibido(redondear(valor));

        double valorRestante = redondear(valor);
        int iteraciones = 0;

        while (valorRestante > TOLERANCIA && iteraciones < MAX_ITERACIONES) {
            iteraciones++;

            DetallePrestamo cuota = buscarSiguienteCuotaConSaldo(idPrestamo);
            if (cuota == null) {
                System.out.println("  ℹ️ No hay más cuotas pendientes - Excedente no aplicado: $" + valorRestante);
                break;
            }

            DetalleAplicacionCuota detalle = aplicarPagoACuota(cuota, valorRestante, ctx);
            resultado.getCuotasAfectadas().add(detalle);

            if (detalle.getTotalAplicado() <= TOLERANCIA) {
                // Blindaje: si una cuota no absorbe nada, no tiene sentido reintentar sobre ella
                System.out.println("  ⚠️ La cuota #" + cuota.getNumeroCuota()
                    + " no absorbió valor; se detiene la cascada");
                break;
            }

            valorRestante = redondear(valorRestante - detalle.getTotalAplicado());
        }

        if (iteraciones >= MAX_ITERACIONES) {
            System.out.println("  ⚠️ Se alcanzó el máximo de " + MAX_ITERACIONES + " iteraciones de cascada");
        }

        resultado.setExcedenteNoAplicado(Math.max(0, redondear(valorRestante)));
        resultado.setValorAplicado(redondear(resultado.getValorRecibido() - resultado.getExcedenteNoAplicado()));

        boolean cancelado = verificarYActualizarEstadoPrestamo(prestamo);
        resultado.setPrestamoCancelado(cancelado);
        resultado.setEstadoFinalPrestamo(prestamo.getIdEstado());

        System.out.println("  ✅ Pago aplicado: $" + resultado.getValorAplicado()
            + " en " + resultado.getCuotasAfectadas().size() + " cuota(s)"
            + " - Excedente no aplicado: $" + resultado.getExcedenteNoAplicado()
            + " - Préstamo cancelado: " + cancelado);

        return resultado;
    }

    /**
     * Primera cuota pendiente (menor numeroCuota) con saldo real &gt; 0.01.
     * Las cuotas de saldo insignificante se van autocorrigiendo a PAGADA dentro de
     * {@code calcularSaldosRealesCuota}, así que dejan de aparecer en la siguiente consulta.
     */
    private DetallePrestamo buscarSiguienteCuotaConSaldo(Long idPrestamo) throws Throwable {
        List<DetallePrestamo> pendientes =
            detallePrestamoDaoService.selectCuotasPendientesByPrestamoOrdenadas(idPrestamo);

        if (pendientes == null || pendientes.isEmpty()) {
            return null;
        }

        for (DetallePrestamo cuota : pendientes) {
            SaldosCuota saldos = calcularSaldosRealesCuota(cuota);
            if (saldos.getTotalPendiente() > TOLERANCIA) {
                return cuota;
            }
        }
        return null;
    }

    @Override
    public DetalleAplicacionCuota aplicarPagoACuota(DetallePrestamo cuota, double valorDisponible, ContextoPago ctx)
            throws Throwable {
        System.out.println("MotorPagoPrestamoService.aplicarPagoACuota - Cuota #"
            + (cuota != null ? cuota.getNumeroCuota() : null) + " - Disponible: $" + valorDisponible);

        DetalleAplicacionCuota detalle = new DetalleAplicacionCuota();
        if (cuota == null) {
            return detalle;
        }

        detalle.setIdCuota(cuota.getCodigo());
        detalle.setNumeroCuota(cuota.getNumeroCuota());
        detalle.setEstadoAnterior(cuota.getEstado());

        SaldosCuota saldos = calcularSaldosRealesCuota(cuota);
        // calcularSaldosRealesCuota pudo autocorregir el estado: se relee para el resultado
        detalle.setEstadoNuevo(cuota.getEstado());

        if (saldos.getTotalPendiente() <= TOLERANCIA) {
            System.out.println("      ℹ️ La cuota ya no tiene saldo pendiente - no se aplica nada");
            return detalle;
        }

        double montoAplicar = redondear(Math.min(valorDisponible, saldos.getTotalPendiente()));

        // ------------------------------------------------------------------
        // PRELACIÓN (imputación secuencial; cada componente toma min(restante, saldo))
        //
        //   1. Seguro de incendio
        //   2. Seguro de desgravamen
        //   3. Interés de mora
        //   4. Interés vencido      (hoy siempre 0: ningún proceso lo alimenta)
        //   5. Interés ordinario
        //   6. Capital
        //
        // Orden confirmado por negocio el 2026-08-14: primero los seguros, después la deuda
        // vieja (mora e interés vencido), después el interés corriente y por último el capital.
        // El interés vencido va junto a la mora por ser también deuda vieja; como vale 0, su
        // posición no altera hoy ningún resultado.
        // Hardcodeada por ahora; parametrizable vía CRD.OAVP en una fase futura.
        // ------------------------------------------------------------------
        double restante = montoAplicar;

        double aplicadoSeguro = Math.min(restante, saldos.getSaldoSeguroIncendio());
        restante = redondear(restante - aplicadoSeguro);

        double aplicadoDesgravamen = Math.min(restante, saldos.getSaldoDesgravamen());
        restante = redondear(restante - aplicadoDesgravamen);

        double aplicadoMora = Math.min(restante, saldos.getSaldoMora());
        restante = redondear(restante - aplicadoMora);

        double aplicadoIV = Math.min(restante, saldos.getSaldoInteresVencido());
        restante = redondear(restante - aplicadoIV);

        double aplicadoInteres = Math.min(restante, saldos.getSaldoInteres());
        restante = redondear(restante - aplicadoInteres);

        double aplicadoCapital = Math.min(restante, saldos.getSaldoCapital());
        restante = redondear(restante - aplicadoCapital);

        double totalAplicado = redondear(aplicadoDesgravamen + aplicadoMora + aplicadoIV
            + aplicadoInteres + aplicadoCapital + aplicadoSeguro);

        if (restante > TOLERANCIA) {
            // Solo ocurre con datos legacy donde DTPRTTLL no cuadra con la suma de los
            // componentes. Se registra el desfase y se imputa SOLO lo que los componentes
            // pudieron absorber, para que PGPR siga cumpliendo "los componentes suman el valor";
            // el sobrante vuelve a la cascada.
            System.out.println("      ⚠️ Desfase de $" + restante + " entre el total pendiente ($"
                + saldos.getTotalPendiente() + ") y la suma de los componentes de la cuota #"
                + cuota.getNumeroCuota() + " - se aplica solo $" + totalAplicado);
            montoAplicar = totalAplicado;
        }

        if (totalAplicado <= TOLERANCIA) {
            System.out.println("      ℹ️ No se pudo imputar valor a ningún componente");
            return detalle;
        }

        // ------------------------------------------------------------------
        // Acumular en la cuota (NUNCA reemplazar)
        // ------------------------------------------------------------------
        double capitalPagadoAcum     = redondear(nullSafe(cuota.getCapitalPagado()) + aplicadoCapital);
        double interesPagadoAcum     = redondear(nullSafe(cuota.getInteresPagado()) + aplicadoInteres);
        double desgravamenPagadoAcum = redondear(nullSafe(cuota.getDesgravamenPagado()) + aplicadoDesgravamen);
        double moraPagadaAcum        = redondear(nullSafe(cuota.getMoraPagado()) + aplicadoMora);
        double ivPagadoAcum          = redondear(nullSafe(cuota.getInteresVendidoPagado()) + aplicadoIV);

        cuota.setCapitalPagado(capitalPagadoAcum);
        cuota.setInteresPagado(interesPagadoAcum);
        cuota.setDesgravamenPagado(desgravamenPagadoAcum);
        cuota.setMoraPagado(moraPagadaAcum);
        cuota.setInteresVendidoPagado(ivPagadoAcum);

        // Saldos persistidos
        cuota.setSaldoCapital(Math.max(0, redondear(nullSafe(cuota.getSaldoInicialCapital()) - capitalPagadoAcum)));
        cuota.setSaldoInteres(Math.max(0, redondear(nullSafe(cuota.getInteres()) - interesPagadoAcum)));
        cuota.setSaldoMora(Math.max(0, redondear(nullSafe(cuota.getMora()) - moraPagadaAcum)));
        cuota.setSaldoInteresVencido(Math.max(0, redondear(nullSafe(cuota.getInteresVencido()) - ivPagadoAcum)));

        // El seguro de incendio no tiene campo "pagado" en la cuota: su acumulado vive en PGPR.
        double seguroPagadoPrevio = Math.max(0,
            redondear(nullSafe(cuota.getValorSeguroIncendio()) - saldos.getSaldoSeguroIncendio()));
        double totalPagadoAcumulado = redondear(capitalPagadoAcum + interesPagadoAcum + desgravamenPagadoAcum
            + moraPagadaAcum + ivPagadoAcum + seguroPagadoPrevio + aplicadoSeguro);
        cuota.setSaldo(Math.max(0, redondear(totalConMoraIV(cuota) - totalPagadoAcumulado)));

        // ------------------------------------------------------------------
        // Estado de la cuota
        // ------------------------------------------------------------------
        LocalDateTime fechaPago = ctx != null && ctx.getFechaPago() != null ? ctx.getFechaPago() : LocalDateTime.now();

        if (Math.abs(montoAplicar - saldos.getTotalPendiente()) <= TOLERANCIA) {
            aplicarEstadoCuota(cuota, (long) EstadoCuotaPrestamo.PAGADA);
            cuota.setFechaPagado(fechaPago);
            System.out.println("      ✅ Cuota #" + cuota.getNumeroCuota() + " PAGADA con $" + totalAplicado);
        } else {
            aplicarEstadoCuota(cuota, (long) EstadoCuotaPrestamo.PARCIAL);
            System.out.println("      ⚠️ Cuota #" + cuota.getNumeroCuota() + " PARCIAL - Aplicado $"
                + totalAplicado + " de $" + saldos.getTotalPendiente() + " pendiente");
        }

        detallePrestamoService.saveSingle(cuota);
        detalle.setEstadoNuevo(cuota.getEstado());

        // ------------------------------------------------------------------
        // PagoPrestamo de esta aplicación
        // ------------------------------------------------------------------
        PagoPrestamo pago = crearRegistroPago(cuota, totalAplicado, aplicadoCapital, aplicadoInteres,
            aplicadoMora, aplicadoIV, aplicadoDesgravamen, aplicadoSeguro, fechaPago, ctx);

        detalle.setAplicadoDesgravamen(aplicadoDesgravamen);
        detalle.setAplicadoMora(aplicadoMora);
        detalle.setAplicadoInteresVencido(aplicadoIV);
        detalle.setAplicadoInteres(aplicadoInteres);
        detalle.setAplicadoCapital(aplicadoCapital);
        detalle.setAplicadoSeguro(aplicadoSeguro);
        detalle.setTotalAplicado(totalAplicado);
        detalle.setIdPagoPrestamo(pago != null ? pago.getCodigo() : null);

        return detalle;
    }

    /**
     * Crea el PagoPrestamo de una aplicación. PGPRIDST es NOT NULL: idEstado SIEMPRE en 1.
     */
    private PagoPrestamo crearRegistroPago(DetallePrestamo cuota,
                                           double montoTotal,
                                           double capitalPagado,
                                           double interesPagado,
                                           double moraPagada,
                                           double interesVencidoPagado,
                                           double desgravamenPagado,
                                           double valorSeguroIncendio,
                                           LocalDateTime fechaPago,
                                           ContextoPago ctx) throws Throwable {

        PagoPrestamo pago = new PagoPrestamo();
        pago.setPrestamo(cuota.getPrestamo());
        pago.setDetallePrestamo(cuota);
        pago.setNumeroCuota(cuota.getNumeroCuota());
        pago.setFecha(fechaPago);
        pago.setValor(redondear(montoTotal));

        pago.setCapitalPagado(redondear(capitalPagado));
        pago.setInteresPagado(redondear(interesPagado));
        pago.setMoraPagada(redondear(moraPagada));
        pago.setInteresVencidoPagado(redondear(interesVencidoPagado));
        pago.setDesgravamen(redondear(desgravamenPagado));
        pago.setValorSeguroIncendio(redondear(valorSeguroIncendio));
        // El abono a capital y la precancelación usan saldoOtros; un pago de cuota no.
        pago.setSaldoOtros(0.0);

        String observacion = ctx != null && ctx.getObservacion() != null ? ctx.getObservacion() : "";
        Long idEvento = ctx != null ? ctx.getIdEvento() : null;
        pago.setObservacion(observacion + " [Evento: " + idEvento + "]");
        pago.setTipo(ctx != null ? ctx.getTipoPago() : null);
        pago.setUsuarioRegistro(ctx != null ? ctx.getUsuario() : null);
        pago.setRutaDocumentoRespaldo(ctx != null ? ctx.getRutaDocumentoRespaldo() : null);
        pago.setFechaRegistro(LocalDateTime.now());

        // ✅ PGPRIDST es NOT NULL (ORA-01400 si falta)
        pago.setEstado(1L);
        pago.setIdEstado(1L);
        pago.setAnulado(0L);
        pago.setEventoPrestamo(buscarEvento(idEvento));

        pago = pagoPrestamoService.saveSingle(pago);
        System.out.println("      💾 PagoPrestamo creado: " + pago.getCodigo() + " - Valor: $" + pago.getValor());
        return pago;
    }

    /**
     * Carga el EventoPrestamo por código sin lanzar si no existe (usa em.find, no NamedQuery).
     */
    private EventoPrestamo buscarEvento(Long idEvento) throws Throwable {
        if (idEvento == null) {
            return null;
        }
        return eventoPrestamoDaoService.find(new EventoPrestamo(), idEvento);
    }

    // ========================================================================
    // §6.4 — Estado del préstamo
    // ========================================================================

    @Override
    public boolean verificarYActualizarEstadoPrestamo(Prestamo prestamo) throws Throwable {
        System.out.println("MotorPagoPrestamoService.verificarYActualizarEstadoPrestamo - Préstamo: "
            + (prestamo != null ? prestamo.getCodigo() : null));

        if (prestamo == null || prestamo.getCodigo() == null) {
            return false;
        }

        try {
            // El estado operativo vive en idEstado (PRSTIDST). ESPSCDGO es FK al catálogo
            // CRD.ESPS y NUNCA se toca desde aquí.
            Long estadoActual = prestamo.getIdEstado();

            if (esEstadoTerminalPrestamo(estadoActual)) {
                return false;
            }

            // Un préstamo sin tabla de amortización daría "0 pendientes" y se cancelaría por error
            Long totalCuotas = detallePrestamoDaoService.contarCuotasByPrestamo(prestamo.getCodigo());
            if (totalCuotas == null || totalCuotas == 0L) {
                System.out.println("  ℹ️ Préstamo #" + prestamo.getCodigo()
                    + " sin cuotas registradas - No se evalúa cancelación");
                return false;
            }

            Long cuotasPendientes = detallePrestamoDaoService.contarCuotasPendientesByPrestamo(prestamo.getCodigo());
            System.out.println("  🔍 Préstamo #" + prestamo.getCodigo()
                + " - Cuotas: " + totalCuotas + " / Pendientes: " + cuotasPendientes);

            if (cuotasPendientes == null || cuotasPendientes > 0L) {
                return false;
            }

            System.out.println("  ✅ TODAS LAS CUOTAS PAGADAS - Actualizando préstamo a CANCELADO"
                + " (estado anterior: " + estadoActual + ")");

            // ⚠️ NO tocar fechaFin: es el fin del plazo, no la fecha de cancelación
            prestamo.setIdEstado(Long.valueOf(EstadoPrestamo.CANCELADO));
            prestamo.setFechaModificacion(LocalDateTime.now());
            prestamoDaoService.save(prestamo, prestamo.getCodigo());

            return true;

        } catch (Throwable e) {
            // Un fallo aquí NO debe abortar el pago
            System.err.println("Error al verificar estado del préstamo: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ========================================================================
    // §6.5 — Reconstrucción de la cuota desde los pagos (reverso)
    // ========================================================================

    @Override
    public void recalcularCuotaDesdePagos(DetallePrestamo cuota) throws Throwable {
        System.out.println("MotorPagoPrestamoService.recalcularCuotaDesdePagos - Cuota: "
            + (cuota != null ? cuota.getCodigo() : null));

        if (cuota == null) {
            return;
        }

        List<PagoPrestamo> pagos = pagoPrestamoDaoService.selectVigentesByIdDetallePrestamo(cuota.getCodigo());

        double desgravamenPagado = 0.0;
        double moraPagada        = 0.0;
        double ivPagado          = 0.0;
        double interesPagado     = 0.0;
        double capitalPagado     = 0.0;
        double seguroPagado      = 0.0;

        if (pagos != null) {
            for (PagoPrestamo pago : pagos) {
                desgravamenPagado += nullSafe(pago.getDesgravamen());
                moraPagada        += nullSafe(pago.getMoraPagada());
                ivPagado          += nullSafe(pago.getInteresVencidoPagado());
                interesPagado     += nullSafe(pago.getInteresPagado());
                capitalPagado     += nullSafe(pago.getCapitalPagado());
                seguroPagado      += nullSafe(pago.getValorSeguroIncendio());
            }
        }

        desgravamenPagado = redondear(desgravamenPagado);
        moraPagada        = redondear(moraPagada);
        ivPagado          = redondear(ivPagado);
        interesPagado     = redondear(interesPagado);
        capitalPagado     = redondear(capitalPagado);
        seguroPagado      = redondear(seguroPagado);

        // Campos *Pagado = las sumas reales
        cuota.setCapitalPagado(capitalPagado);
        cuota.setInteresPagado(interesPagado);
        cuota.setDesgravamenPagado(desgravamenPagado);
        cuota.setMoraPagado(moraPagada);
        cuota.setInteresVendidoPagado(ivPagado);

        // Saldos por componente
        double saldoDesgravamen = Math.max(0, redondear(nullSafe(cuota.getDesgravamen()) - desgravamenPagado));
        double saldoMora        = Math.max(0, redondear(nullSafe(cuota.getMora()) - moraPagada));
        double saldoIV          = Math.max(0, redondear(nullSafe(cuota.getInteresVencido()) - ivPagado));
        double saldoInteres     = Math.max(0, redondear(nullSafe(cuota.getInteres()) - interesPagado));
        double saldoCapitalCuota = Math.max(0, redondear(nullSafe(cuota.getCapital()) - capitalPagado));
        double saldoSeguro      = Math.max(0, redondear(nullSafe(cuota.getValorSeguroIncendio()) - seguroPagado));

        cuota.setSaldoInteres(saldoInteres);
        cuota.setSaldoMora(saldoMora);
        cuota.setSaldoInteresVencido(saldoIV);
        cuota.setSaldoCapital(Math.max(0, redondear(nullSafe(cuota.getSaldoInicialCapital()) - capitalPagado)));

        double totalPagado = redondear(desgravamenPagado + moraPagada + ivPagado
            + interesPagado + capitalPagado + seguroPagado);
        double totalPendiente = redondear(saldoDesgravamen + saldoMora + saldoIV
            + saldoInteres + saldoCapitalCuota + saldoSeguro);

        cuota.setSaldo(Math.max(0, redondear(totalConMoraIV(cuota) - totalPagado)));

        // Estado reconstruido
        if (totalPendiente <= TOLERANCIA) {
            aplicarEstadoCuota(cuota, (long) EstadoCuotaPrestamo.PAGADA);
            if (cuota.getFechaPagado() == null) {
                cuota.setFechaPagado(LocalDateTime.now());
            }
            System.out.println("  → Cuota #" + cuota.getNumeroCuota() + " queda PAGADA");
        } else if (totalPagado > TOLERANCIA) {
            aplicarEstadoCuota(cuota, (long) EstadoCuotaPrestamo.PARCIAL);
            cuota.setFechaPagado(null);
            System.out.println("  → Cuota #" + cuota.getNumeroCuota() + " queda PARCIAL");
        } else {
            boolean vencida = cuota.getFechaVencimiento() != null
                && cuota.getFechaVencimiento().toLocalDate().isBefore(java.time.LocalDate.now());
            long estadoBase = vencida ? EstadoCuotaPrestamo.EN_MORA : EstadoCuotaPrestamo.PENDIENTE;
            aplicarEstadoCuota(cuota, estadoBase);
            cuota.setFechaPagado(null);
            System.out.println("  → Cuota #" + cuota.getNumeroCuota() + " vuelve a "
                + (vencida ? "EN_MORA" : "PENDIENTE"));
        }

        detallePrestamoService.saveSingle(cuota);
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    /** Escribe SIEMPRE los dos campos de estado de la cuota (regla de estados espejo). */
    private void aplicarEstadoCuota(DetallePrestamo cuota, long estado) {
        cuota.setEstado(estado);
        cuota.setIdEstado(estado);
    }

    /** PAGADA(4) o CANCELADA_ANTICIPADA(7): la cuota ya no admite aplicación. */
    private boolean esEstadoLiquidado(Long estado) {
        return estado != null
            && (estado == EstadoCuotaPrestamo.PAGADA || estado == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA);
    }

    /** Estados terminales del préstamo: 3, 4 y 5 (nunca se cambian automáticamente). */
    private boolean esEstadoTerminalPrestamo(Long idEstado) {
        return idEstado != null
            && (idEstado == EstadoPrestamo.CANCELADO
             || idEstado == EstadoPrestamo.CANCELADO_ANTICIPADO
             || idEstado == EstadoPrestamo.CANCELADO_POR_NOVACION);
    }

    /** Deuda bruta de la cuota incluyendo mora e interés vencido (DTPRTTLL no los incluye). */
    private double totalConMoraIV(DetallePrestamo cuota) {
        return redondear(nullSafe(cuota.getCapital())
            + nullSafe(cuota.getInteres())
            + nullSafe(cuota.getDesgravamen())
            + nullSafe(cuota.getValorSeguroIncendio())
            + nullSafe(cuota.getMora())
            + nullSafe(cuota.getInteresVencido()));
    }

    private double nullSafe(Double valor) {
        return valor != null ? valor : 0.0;
    }

    /** Redondeo monetario HALF_UP a 2 decimales. */
    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

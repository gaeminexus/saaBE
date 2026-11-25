package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.EventoPrestamoDaoService;
import com.saa.ejb.crd.dao.HistDetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.CargaArchivoService;
import com.saa.ejb.crd.service.DetallePrestamoService;
import com.saa.ejb.crd.service.MotorPagoPrestamoService;
import com.saa.ejb.crd.service.PagoPrestamoService;
import com.saa.ejb.crd.service.PrelacionCuotas;
import com.saa.ejb.crd.service.ProcesoMoraPrestamoService;
import com.saa.ejb.crd.service.SaldoOtrosRespaldo;
import com.saa.ejb.crd.service.dto.ContextoPago;
import com.saa.ejb.crd.service.dto.DetalleAplicacionCuota;
import com.saa.ejb.crd.service.dto.ResiduoSaldoOtros;
import com.saa.ejb.crd.service.dto.ResultadoAplicacionPago;
import com.saa.ejb.crd.service.dto.SaldosCuota;
import com.saa.model.crd.CargaArchivo;
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
 * extendida con mora e interés vencido y con la prelación de 6 componentes.
 *
 * 2026-09-02 (PLAN-FASE3-MOTOR-PAGOS.md): la convergencia dejó de ser una fase futura —
 * {@code CargaArchivoPetroServiceImpl} (fase 3 de la carga Petro) ya llama a
 * {@link #aplicarPago(Long, double, ContextoPago)} con {@code ContextoPago#getIdCargaArchivo()}
 * seteado, en vez de reimplementar su propia cascada.
 *
 * @author Sistema SAA
 * @since 2026-08-14
 */
@Stateless
public class MotorPagoPrestamoServiceImpl implements MotorPagoPrestamoService {

    /** Una cuota con saldo pendiente menor o igual a esto se considera saldada. */
    private static final double TOLERANCIA_CUOTA = 0.01;

    /**
     * Medio centavo. Frontera entre "no queda plata" y "queda plata": todos los importes pasan
     * por redondear() a 2 decimales, así que un residuo real es 0,00 o >= 0,01 y nunca algo
     * intermedio. Se usa medio centavo en vez de comparar contra 0,01 para que la comparación no
     * dependa de la representación binaria del double.
     */
    private static final double MEDIO_CENTAVO = 0.005;

    /** Tope de seguridad del bucle de cascada */
    private static final int MAX_ITERACIONES = 100;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    // C43 (D-156): reversos de la mora no cobrada.
    @EJB
    private com.saa.ejb.crd.service.ProvisionInteresService provisionInteresService;

    @EJB
    private com.saa.ejb.crd.service.ConfiguracionContabilidadService configuracionContabilidadService;

    @EJB
    private com.saa.ejb.crd.dao.TransferenciaCargaPetroDaoService transferenciaCargaPetroDaoService;

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

    /** C35: rastro en CRD.HDTP del DTPRSLOT sin respaldo que se normaliza. */
    @EJB
    private HistDetallePrestamoDaoService histDetallePrestamoDaoService;

    /** Solo para resolver ctx.getIdCargaArchivo() cuando viene no-null (carga Petro). */
    @EJB
    private CargaArchivoService cargaArchivoService;

    /** Pedido 10: para que un préstamo sin cuotas vencidas vuelva a VIGENTE en cuanto se paga. */
    @EJB
    private ProcesoMoraPrestamoService procesoMoraPrestamoService;

    /** C31: baja (o reingreso) de la garantía en cuentas de orden cuando el préstamo termina (o se reabre). */
    @EJB
    private com.saa.ejb.crd.service.GarantiaOrdenPrestamoService garantiaOrdenPrestamoService;

    // ========================================================================
    // §6.2 — Saldos reales de una cuota (con autocorrección)
    // ========================================================================

    @Override
    public SaldosCuota calcularSaldosCuota(DetallePrestamo cuota) throws Throwable {
        System.out.println("MotorPagoPrestamoService.calcularSaldosCuota (solo lectura) - Cuota: "
            + (cuota != null ? cuota.getCodigo() : null));

        if (cuota == null) {
            return new SaldosCuota();
        }

        // ✅ PGPR es la fuente de verdad, y SOLO los pagos vigentes (los anulados no cuentan)
        List<PagoPrestamo> pagos = pagoPrestamoDaoService.selectVigentesByIdDetallePrestamo(cuota.getCodigo());
        return calcularSaldosCuota(cuota, pagos);
    }

    @Override
    public SaldosCuota calcularSaldosCuota(DetallePrestamo cuota, LocalDateTime corte) throws Throwable {
        if (cuota == null) {
            return new SaldosCuota();
        }
        if (corte == null) {
            return calcularSaldosCuota(cuota);
        }
        // C33 (punto 2): solo los pagos con fecha <= corte (o sin fecha).
        return calcularSaldosCuota(cuota,
            PrelacionCuotas.alCorte(pagoPrestamoDaoService.selectVigentesByIdDetallePrestamo(cuota.getCodigo()), corte));
    }

    @Override
    public SaldosCuota calcularSaldosCuota(DetallePrestamo cuota, List<PagoPrestamo> pagosVigentes) throws Throwable {
        System.out.println("MotorPagoPrestamoService.calcularSaldosCuota (solo lectura, pagos en lote) - Cuota: "
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

        List<PagoPrestamo> pagos = pagosVigentes;

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

        return saldos;
    }

    @Override
    public SaldosCuota calcularSaldosRealesCuota(DetallePrestamo cuota) throws Throwable {
        return calcularSaldosRealesCuota(cuota, (LocalDateTime) null);
    }

    @Override
    public SaldosCuota calcularSaldosRealesCuota(DetallePrestamo cuota, LocalDateTime corte) throws Throwable {
        System.out.println("MotorPagoPrestamoService.calcularSaldosRealesCuota - Cuota: "
            + (cuota != null ? cuota.getCodigo() : null) + (corte != null ? " - al " + corte : ""));

        if (cuota == null) {
            return new SaldosCuota();
        }
        // C33 (punto 2): con corte, solo los pagos con fecha <= corte. Saldada al corte => saldada hoy (más pagos
        // solo cubren más), así que la autocorrección de abajo sigue siendo correcta.
        List<PagoPrestamo> pagosAlCorte = PrelacionCuotas.alCorte(
            pagoPrestamoDaoService.selectVigentesByIdDetallePrestamo(cuota.getCodigo()), corte);
        SaldosCuota saldos = calcularSaldosCuota(cuota, pagosAlCorte);

        // ✅ AUTOCORRECCIÓN: la cuota está liquidada según PGPR pero su estado dice otra cosa
        if (saldos.getTotalPendiente() <= TOLERANCIA_CUOTA && !esEstadoLiquidado(cuota.getEstado())) {
            System.out.println("    ⚠️ Cuota #" + cuota.getNumeroCuota()
                + " liquidada según PagoPrestamo - Actualizando estado a PAGADA");

            // Recompone los acumulados "Pagado" desde PGPR para persistirlos en la cuota (los
            // mismos pagos vigentes que calcularSaldosCuota ya usó arriba para el desglose).
            double desgravamenPagado = 0.0;
            double moraPagada        = 0.0;
            double ivPagado          = 0.0;
            double interesPagado     = 0.0;
            double capitalPagado     = 0.0;
            double seguroPagado      = 0.0;
            List<PagoPrestamo> pagos = corte == null ? pagosAlCorte
                : pagoPrestamoDaoService.selectVigentesByIdDetallePrestamo(cuota.getCodigo());
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

            aplicarEstadoCuota(cuota, (long) EstadoCuotaPrestamo.PAGADA);

            // ✅ Respetar la fecha de pago existente; solo se establece si falta — y en ese
            // caso con la fecha del ÚLTIMO pago vigente (C6, FECHA-CONTABLE-REPROCESO.md), no
            // con la de hoy: en un re-proceso de meses pasados la cuota quedaba pagada "hoy".
            if (cuota.getFechaPagado() == null) {
                cuota.setFechaPagado(fechaUltimoPago(pagos));
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
                + textoEstadoTerminal(prestamo.getIdEstado()) + ") y no admite pagos");
        }
        // C31c: estado antes de esta operación; solo la transición vivo → terminado da de baja la garantía.
        Long estadoAnteriorC31 = prestamo.getIdEstado();

        ResultadoAplicacionPago resultado = new ResultadoAplicacionPago();
        resultado.setIdPrestamo(idPrestamo);
        resultado.setIdEvento(ctx != null ? ctx.getIdEvento() : null);
        resultado.setValorRecibido(redondear(valor));

        double valorRestante = redondear(valor);
        int iteraciones = 0;
        // C33 (punto 2): los saldos se miden con los pagos hasta el fin del día del pago.
        LocalDateTime corte = corteDelPago(ctx);

        while (valorRestante > MEDIO_CENTAVO && iteraciones < MAX_ITERACIONES) {
            iteraciones++;

            DetallePrestamo cuota = buscarSiguienteCuotaConSaldo(idPrestamo, corte);
            if (cuota == null) {
                System.out.println("  ℹ️ No hay más cuotas pendientes - Excedente no aplicado: $" + valorRestante);
                break;
            }

            // La cascada ya eligió la cuota más antigua con saldo: el invariante C33 se cumple por construcción.
            DetalleAplicacionCuota detalle = aplicarPagoACuotaInterno(cuota, valorRestante, ctx, corte);
            resultado.getCuotasAfectadas().add(detalle);

            if (detalle.getTotalAplicado() <= MEDIO_CENTAVO && detalle.getMoraNoCobrada() <= MEDIO_CENTAVO) {
                // C43: una cuota a la que solo le quedaba mora no cobrable queda pagada sin absorber valor; la cascada
                // sigue con la siguiente.
                // Blindaje: si una cuota no absorbió nada, no tiene sentido reintentar sobre ella
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
        if (cancelado) {
            // C31: el pago de la última cuota (ventanilla, CBCR, pago con aportes, Petro/Arch) termina el
            // préstamo: su garantía sale de cuentas de orden por el monto otorgado, a la fecha de afectación.
            // C31c: con el estado anterior y el control cronológico (la fecha de los PGPR de esta operación es
            // ctx.fechaPago, o ahora si no viene — la misma regla de aplicarPagoACuota).
            java.time.LocalDate fechaBaja = ctx == null ? null
                : ctx.getFechaAfectacion() != null ? ctx.getFechaAfectacion()
                : ctx.getFechaPago() != null ? ctx.getFechaPago().toLocalDate() : null;
            LocalDateTime fechaPagos = ctx != null && ctx.getFechaPago() != null ? ctx.getFechaPago() : LocalDateTime.now();
            garantiaOrdenPrestamoService.sincronizar(prestamo, estadoAnteriorC31, ctx != null ? ctx.getIdEmpresa() : null,
                fechaBaja, fechaPagos, ctx != null ? ctx.getUsuario() : null,
                "cancelado por pago" + (ctx != null && ctx.getTipoPago() != null ? " " + ctx.getTipoPago() : ""));
        }
        resultado.setPrestamoCancelado(cancelado);
        resultado.setEstadoFinalPrestamo(prestamo.getIdEstado());

        System.out.println("  ✅ Pago aplicado: $" + resultado.getValorAplicado()
            + " en " + resultado.getCuotasAfectadas().size() + " cuota(s)"
            + " - Excedente no aplicado: $" + resultado.getExcedenteNoAplicado()
            + " - Préstamo cancelado: " + cancelado);

        return resultado;
    }

    /**
     * Primera cuota con saldo real al corte &gt; 0.01, en orden de número (C33: incluye la PAGADA sin pagos que la
     * cubran al corte desde el arranque; ver {@code DetallePrestamoDaoService#selectCuotasConSaldoPosibleOrdenadas}).
     * Las cuotas de saldo insignificante se van autocorrigiendo a PAGADA dentro de
     * {@code calcularSaldosRealesCuota}, así que dejan de aparecer en la siguiente consulta.
     */
    private DetallePrestamo buscarSiguienteCuotaConSaldo(Long idPrestamo, LocalDateTime corte) throws Throwable {
        List<DetallePrestamo> pendientes = detallePrestamoDaoService.selectCuotasConSaldoPosibleOrdenadas(idPrestamo,
            PrelacionCuotas.desde(), corte);

        if (pendientes == null || pendientes.isEmpty()) {
            return null;
        }

        for (DetallePrestamo cuota : pendientes) {
            SaldosCuota saldos = calcularSaldosRealesCuota(cuota, corte);
            if (saldos.getTotalPendiente() > TOLERANCIA_CUOTA) {
                return cuota;
            }
        }
        return null;
    }

    /** C33: fin del día de {@code ctx.fechaPago} (sin fecha, ahora): los pagos de ese día cuentan, los posteriores no. */
    private static LocalDateTime corteDelPago(ContextoPago ctx) {
        LocalDateTime fecha = ctx != null && ctx.getFechaPago() != null ? ctx.getFechaPago() : LocalDateTime.now();
        return fecha.toLocalDate().atTime(java.time.LocalTime.MAX);
    }

    /**
     * C33 (punto 3): primera cuota de número menor que {@code cuota} con saldo real al corte, o {@code null}. Puro (no
     * autocorrige estados).
     */
    private DetallePrestamo cuotaAnteriorConSaldo(DetallePrestamo cuota, LocalDateTime corte, double[] saldoEncontrado)
            throws Throwable {
        if (cuota == null || cuota.getPrestamo() == null || cuota.getPrestamo().getCodigo() == null
                || cuota.getNumeroCuota() == null) {
            return null;
        }
        List<DetallePrestamo> candidatas = detallePrestamoDaoService.selectCuotasConSaldoPosibleOrdenadas(
            cuota.getPrestamo().getCodigo(), PrelacionCuotas.desde(), corte);
        if (candidatas == null) {
            return null;
        }
        for (DetallePrestamo previa : candidatas) {
            if (previa.getNumeroCuota() == null || previa.getNumeroCuota() >= cuota.getNumeroCuota()) {
                break;
            }
            SaldosCuota s = calcularSaldosCuota(previa, corte);
            if (s.getTotalPendiente() > TOLERANCIA_CUOTA) {
                saldoEncontrado[0] = s.getTotalPendiente();
                return previa;
            }
        }
        return null;
    }

    @Override
    public DetalleAplicacionCuota aplicarPagoACuota(DetallePrestamo cuota, double valorDisponible, ContextoPago ctx)
            throws Throwable {
        LocalDateTime corte = corteDelPago(ctx);
        // C33 (punto 3): no se aplica dinero a una cuota si una anterior tiene saldo — salvo el abono a capital, la
        // precancelación y el acuerdo con condonación, que tienen su propia regla.
        if (cuota != null && !(ctx != null && PrelacionCuotas.esTipoExento(ctx.getTipoPago()))) {
            double[] saldo = new double[1];
            DetallePrestamo previa = cuotaAnteriorConSaldo(cuota, corte, saldo);
            if (previa != null) {
                String mensaje = PrelacionCuotas.mensajeInvariante(cuota.getPrestamo().getCodigo(),
                    previa.getNumeroCuota(), saldo[0], cuota.getNumeroCuota(), corte,
                    ctx != null ? ctx.getTipoPago() : null);
                if (PrelacionCuotas.esBloqueante()) {
                    throw new IncomeException(mensaje);
                }
                System.out.println("  ⚠️ C33 (" + PrelacionCuotas.PROPIEDAD_MODO + "=avisar) " + mensaje);
            }
        }
        return aplicarPagoACuotaInterno(cuota, valorDisponible, ctx, corte);
    }

    /** Cuerpo de {@link #aplicarPagoACuota}: sin el invariante (la cascada ya eligió la cuota más antigua con saldo). */
    private DetalleAplicacionCuota aplicarPagoACuotaInterno(DetallePrestamo cuota, double valorDisponible,
            ContextoPago ctx, LocalDateTime corte) throws Throwable {
        System.out.println("MotorPagoPrestamoService.aplicarPagoACuota - Cuota #"
            + (cuota != null ? cuota.getNumeroCuota() : null) + " - Disponible: $" + valorDisponible);

        DetalleAplicacionCuota detalle = new DetalleAplicacionCuota();
        if (cuota == null) {
            return detalle;
        }

        detalle.setIdCuota(cuota.getCodigo());
        detalle.setNumeroCuota(cuota.getNumeroCuota());
        detalle.setEstadoAnterior(cuota.getEstado());

        // C33 (punto 2): saldos al corte del pago.
        SaldosCuota saldos = calcularSaldosRealesCuota(cuota, corte);
        // calcularSaldosRealesCuota pudo autocorregir el estado: se relee para el resultado
        detalle.setEstadoNuevo(cuota.getEstado());

        if (saldos.getTotalPendiente() <= TOLERANCIA_CUOTA) {
            System.out.println("      ℹ️ La cuota ya no tiene saldo pendiente - no se aplica nada");
            return detalle;
        }

        // C43 (D-156 3 y a, 24-nov-2026): antes de la fecha en que ASOPREP empezó a cobrar la mora, el pago no la cobra.
        // Se aplica a la cuota sin la mora; si con eso la cuota queda cubierta, la mora se retira de la cuota y se
        // reversan su devengo y su provisión. Si no la cubre, la mora sigue en la cuota (se cobró después, retroactiva).
        LocalDateTime fechaPagoMora = ctx != null && ctx.getFechaPago() != null ? ctx.getFechaPago() : LocalDateTime.now();
        double moraApartada = com.saa.ejb.crd.service.ModoCorteCartera.moraNoCobrable(fechaPagoMora)
            ? redondear(saldos.getSaldoMora()) : 0.0;
        double pendienteCobrable = redondear(saldos.getTotalPendiente() - moraApartada);
        if (moraApartada > MEDIO_CENTAVO && pendienteCobrable <= TOLERANCIA_CUOTA) {
            // Solo le quedaba la mora: queda pagada sin absorber valor.
            retirarMoraNoCobrada(cuota, moraApartada);
            aplicarEstadoCuota(cuota, (long) EstadoCuotaPrestamo.PAGADA);
            cuota.setFechaPagado(fechaPagoMora);
            cuota.setSaldo(0.0);
            detallePrestamoService.saveSingle(cuota);
            detalle.setEstadoNuevo(cuota.getEstado());
            detalle.setMoraNoCobrada(moraApartada);
            reversarMoraNoCobrada(cuota, moraApartada, ctx, fechaPagoMora);
            System.out.println("      ✅ C43 Cuota #" + cuota.getNumeroCuota() + " PAGADA sin cobrar mora ($"
                + moraApartada + " retirada, D-156)");
            return detalle;
        }

        double montoAplicar = redondear(Math.min(valorDisponible, pendienteCobrable));

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

        double aplicadoMora = moraApartada > MEDIO_CENTAVO ? 0.0 : Math.min(restante, saldos.getSaldoMora());
        restante = redondear(restante - aplicadoMora);

        double aplicadoIV = Math.min(restante, saldos.getSaldoInteresVencido());
        restante = redondear(restante - aplicadoIV);

        double aplicadoInteres = Math.min(restante, saldos.getSaldoInteres());
        restante = redondear(restante - aplicadoInteres);

        double aplicadoCapital = Math.min(restante, saldos.getSaldoCapital());
        restante = redondear(restante - aplicadoCapital);

        double totalAplicado = redondear(aplicadoDesgravamen + aplicadoMora + aplicadoIV
            + aplicadoInteres + aplicadoCapital + aplicadoSeguro);

        if (restante > MEDIO_CENTAVO) {
            // Solo ocurre con datos legacy donde DTPRTTLL no cuadra con la suma de los
            // componentes. Se registra el desfase y se imputa SOLO lo que los componentes
            // pudieron absorber, para que PGPR siga cumpliendo "los componentes suman el valor";
            // el sobrante vuelve a la cascada.
            System.out.println("      ⚠️ Desfase de $" + restante + " entre el total pendiente ($"
                + saldos.getTotalPendiente() + ") y la suma de los componentes de la cuota #"
                + cuota.getNumeroCuota() + " - se aplica solo $" + totalAplicado);
            montoAplicar = totalAplicado;
        }

        if (totalAplicado <= MEDIO_CENTAVO) {
            System.out.println("      ℹ️ No se pudo imputar valor a ningún componente");
            return detalle;
        }

        // D-044 (29-oct-2026): lo que queda por componente después de esta aplicación. El total
        // pendiente de una cuota sin pagos es DTPRTTLL, que en datos migrados puede ser MENOR que la
        // suma de sus componentes; entonces "aplicado = total pendiente" no significa que el capital
        // quedó cubierto (préstamos 6557 y 7140 en junio 2025: 3,45 y 7,19 de capital sin cubrir).
        // C43: sin la mora apartada (no se cobra).
        double pendienteComponentes = redondear(saldos.getSaldoSeguroIncendio() + saldos.getSaldoDesgravamen()
            + saldos.getSaldoMora() + saldos.getSaldoInteresVencido() + saldos.getSaldoInteres()
            + saldos.getSaldoCapital() - totalAplicado - moraApartada);
        boolean cubierta = Math.abs(montoAplicar - pendienteCobrable) <= TOLERANCIA_CUOTA;
        // C43 (D-156): la cuota quedó cubierta sin la mora: la mora se retira de la cuota (no se cobró).
        boolean retiraMora = moraApartada > MEDIO_CENTAVO && cubierta && pendienteComponentes <= TOLERANCIA_CUOTA;
        if (retiraMora) {
            retirarMoraNoCobrada(cuota, moraApartada);
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

        if (cubierta && pendienteComponentes > TOLERANCIA_CUOTA) {
            // D-044: la cuota no quedó saldada: PARCIAL con lo que falta (capital incluido). El
            // reparto del dinero y el pago registrado no cambian; solo el estado.
            aplicarEstadoCuota(cuota, (long) EstadoCuotaPrestamo.PARCIAL);
            cuota.setFechaPagado(null);
            System.out.println("      ⚠️ Cuota #" + cuota.getNumeroCuota() + " PARCIAL (D-044) - Aplicado $"
                + totalAplicado + " = total pendiente, pero quedan $" + pendienteComponentes
                + " por componentes (capital pendiente $" + redondear(saldos.getSaldoCapital() - aplicadoCapital) + ")");
        } else if (cubierta) {
            aplicarEstadoCuota(cuota, (long) EstadoCuotaPrestamo.PAGADA);
            // D-044 (29-oct-2026): la cuota queda saldada en la fecha del ÚLTIMO de sus pagos. En
            // un re-proceso de un mes pasado la cuota puede tener pagos vigentes con fecha
            // POSTERIOR a este cobro (cargas Petro de meses siguientes, pagos registrados después);
            // con la fecha de este cobro quedaba "saldada en junio" aunque su capital lo pagó un
            // pago de julio o de 2026 y ningún pago ni asiento de junio lo acreditaba (35 de las 37
            // operaciones del residuo de junio 2025). Con la fecha del pago que la completa, al
            // corte del mes la cuota sigue pendiente con exactamente el capital que falta, igual que
            // una PARCIAL (CierreCarteraDaoServiceImpl.pendiente/pagos al corte), y hoy sigue
            // saldada: el reparto del dinero, la cascada y los asientos no cambian.
            LocalDateTime ultimoPagoPrevio = ultimaFechaPago(
                pagoPrestamoDaoService.selectVigentesByIdDetallePrestamo(cuota.getCodigo()));
            LocalDateTime fechaSaldada = ultimoPagoPrevio != null && ultimoPagoPrevio.isAfter(fechaPago)
                ? ultimoPagoPrevio : fechaPago;
            cuota.setFechaPagado(fechaSaldada);
            System.out.println("      ✅ Cuota #" + cuota.getNumeroCuota() + " PAGADA con $" + totalAplicado
                + (fechaSaldada != fechaPago ? " - saldada al " + fechaSaldada
                    + " por un pago posterior a este cobro (D-044)" : ""));
        } else {
            aplicarEstadoCuota(cuota, (long) EstadoCuotaPrestamo.PARCIAL);
            System.out.println("      ⚠️ Cuota #" + cuota.getNumeroCuota() + " PARCIAL - Aplicado $"
                + totalAplicado + " de $" + pendienteCobrable + " pendiente"
                + (moraApartada > MEDIO_CENTAVO ? " (sin la mora de $" + moraApartada + ", C43)" : ""));
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
        if (retiraMora) {
            detalle.setMoraNoCobrada(moraApartada);
            reversarMoraNoCobrada(cuota, moraApartada, ctx, fechaPago);
            System.out.println("      ℹ️ C43 Cuota #" + cuota.getNumeroCuota() + " pagada sin mora: $" + moraApartada
                + " retirada (D-156)");
        }

        return detalle;
    }

    /**
     * C43 (D-156): retira de la cuota la mora que no se cobró. Queda solo la mora ya pagada (normalmente 0); el total
     * de la cuota baja en lo mismo. DTPRMRCL (mora calculada) queda como rastro.
     */
    private void retirarMoraNoCobrada(DetallePrestamo cuota, double moraApartada) {
        cuota.setMora(Math.max(0, redondear(nullSafe(cuota.getMora()) - moraApartada)));
        if (cuota.getTotal() != null) {
            cuota.setTotal(Math.max(0, redondear(cuota.getTotal() - moraApartada)));
        }
        if (cuota.getTotalConSeguro() != null) {
            cuota.setTotalConSeguro(Math.max(0, redondear(cuota.getTotalConSeguro() - moraApartada)));
        }
        cuota.setSaldoMora(Math.max(0, redondear(nullSafe(cuota.getMora()) - nullSafe(cuota.getMoraPagado()))));
    }

    /**
     * C43 (D-156): reversa el devengo (D 5.1.02 / H 1.4.02, libro tipo 6) y la provisión (D 1.4.99.05 / H 4.7.05.10,
     * libro tipo 3) de la mora que se retiró de la cuota, con los mismos reversos del cobro tardío. La mora que queda
     * es la ya pagada.
     */
    private void reversarMoraNoCobrada(DetallePrestamo cuota, double moraApartada, ContextoPago ctx,
            LocalDateTime fechaPago) throws Throwable {
        if (!configuracionContabilidadService.contabilidadActiva()) {
            return;
        }
        Long idPrestamo = cuota.getPrestamo() != null ? cuota.getPrestamo().getCodigo() : null;
        Long idEmpresa = ctx != null ? ctx.getIdEmpresa() : null;
        if (idEmpresa == null && ctx != null && ctx.getIdCargaArchivo() != null) {
            idEmpresa = transferenciaCargaPetroDaoService.resolverEmpresaByCarga(ctx.getIdCargaArchivo());
        }
        if (idEmpresa == null) {
            throw new IncomeException("C43: no se pudo determinar la empresa para reversar la mora no cobrada de la"
                + " cuota " + cuota.getCodigo() + " del préstamo " + idPrestamo + " (el contexto del pago no la trae).");
        }
        java.time.LocalDate fecha = ctx != null && ctx.getFechaAfectacion() != null ? ctx.getFechaAfectacion()
            : fechaPago.toLocalDate();
        double moraQueda = redondear(nullSafe(cuota.getMoraPagado()));
        List<Object[]> detalleMora = new ArrayList<Object[]>();
        detalleMora.add(new Object[]{ cuota.getCodigo(), Double.valueOf(redondear(moraQueda + moraApartada)),
            Double.valueOf(moraQueda) });
        Long idOrigen = ctx != null ? (ctx.getIdCargaArchivo() != null ? ctx.getIdCargaArchivo() : ctx.getIdEvento())
            : null;
        String usuario = ctx != null ? ctx.getUsuario() : null;
        provisionInteresService.reversarExcesoProvisionPorCobroTardio(idPrestamo, detalleMora, idEmpresa, fecha,
            ORIGEN_MORA_NO_COBRADA, idOrigen, usuario);
        provisionInteresService.reversarExcesoDevengoPorCobroTardio(idPrestamo, detalleMora, idEmpresa, fecha,
            ORIGEN_MORA_NO_COBRADA, idOrigen, usuario);
    }

    /** C43: origen de los reversos de mora no cobrada en el libro de intereses (CRD.MVIC). */
    private static final String ORIGEN_MORA_NO_COBRADA = "MORA_NO_COBRADA";

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

        // §4.6 PLAN-FASE3-MOTOR-PAGOS.md: "dinero sin destino ES el descuadre" — si el total no
        // cuadra exactamente con la suma de los 6 componentes, PGPR queda con un valor que no se
        // puede reconstruir desde su propio desglose (rompe el invariante del que depende
        // calcularSaldosCuota). Nunca hubo esta validación acá; se agrega ahora.
        double sumaComponentes = redondear(redondear(capitalPagado) + redondear(interesPagado)
            + redondear(moraPagada) + redondear(interesVencidoPagado) + redondear(desgravamenPagado)
            + redondear(valorSeguroIncendio));
        double montoTotalRedondeado = redondear(montoTotal);
        if (Math.abs(montoTotalRedondeado - sumaComponentes) > TOLERANCIA_CUOTA) {
            throw new IncomeException("Descuadre al registrar el pago de la cuota #" + cuota.getNumeroCuota()
                + " (préstamo " + (cuota.getPrestamo() != null ? cuota.getPrestamo().getCodigo() : null)
                + "): el total $" + montoTotalRedondeado + " no coincide con la suma de sus componentes $"
                + sumaComponentes + " (capital " + redondear(capitalPagado) + " + interés " + redondear(interesPagado)
                + " + mora " + redondear(moraPagada) + " + interés vencido " + redondear(interesVencidoPagado)
                + " + desgravamen " + redondear(desgravamenPagado) + " + seguro incendio "
                + redondear(valorSeguroIncendio) + ").");
        }

        PagoPrestamo pago = new PagoPrestamo();
        pago.setPrestamo(cuota.getPrestamo());
        pago.setDetallePrestamo(cuota);
        pago.setNumeroCuota(cuota.getNumeroCuota());
        pago.setFecha(fechaPago);
        pago.setValor(montoTotalRedondeado);

        pago.setCapitalPagado(redondear(capitalPagado));
        pago.setInteresPagado(redondear(interesPagado));
        pago.setMoraPagada(redondear(moraPagada));
        pago.setInteresVencidoPagado(redondear(interesVencidoPagado));
        pago.setDesgravamen(redondear(desgravamenPagado));
        pago.setValorSeguroIncendio(redondear(valorSeguroIncendio));
        // El abono a capital y la precancelación usan saldoOtros; un pago de cuota no.
        pago.setSaldoOtros(0.0);

        // 2026-09-02 (URGENTE, regresión detectada en producción, carga 449): antes se
        // concatenaba "[Evento: " + idEvento + "]" incondicionalmente, y la carga Petro (que
        // no tiene EventoPrestamo: es un lote, no una operación individual) quedaba con
        // "[Evento: null]" visible en pantalla. Solo se anota cuando hay evento real; los
        // demás llamadores (pago manual, aportes, precancelación, abono capital) siempre
        // traen idEvento y quedan exactamente igual que hoy.
        String observacion = ctx != null && ctx.getObservacion() != null ? ctx.getObservacion() : "";
        Long idEvento = ctx != null ? ctx.getIdEvento() : null;
        Long idCargaArchivoCtx = ctx != null ? ctx.getIdCargaArchivo() : null;
        // Detalle de la cuota, solo para la carga Petro: su ContextoPago es compartido por
        // toda la cascada de un mismo aplicarPago (que puede tocar varias cuotas), y sin esto
        // todos sus PagoPrestamo quedarían con la misma observación, sin poder distinguir cuál
        // cuota cubrió cada uno al auditar (sql/171_ORIGEN_DE_LA_BRECHA_449.sql). Los demás
        // llamadores no traen idCargaArchivo y no cambian.
        String observacionCompleta = idCargaArchivoCtx != null
            ? observacion + " - Cuota #" + cuota.getNumeroCuota()
            : observacion;
        if (idEvento != null) {
            observacionCompleta = observacionCompleta + " [Evento: " + idEvento + "]";
        }
        pago.setObservacion(observacionCompleta);
        pago.setTipo(ctx != null ? ctx.getTipoPago() : null);
        pago.setUsuarioRegistro(ctx != null ? ctx.getUsuario() : null);
        pago.setRutaDocumentoRespaldo(ctx != null ? ctx.getRutaDocumentoRespaldo() : null);
        pago.setFechaRegistro(LocalDateTime.now());

        // ✅ PGPRIDST es NOT NULL (ORA-01400 si falta)
        pago.setEstado(1L);
        pago.setIdEstado(1L);
        pago.setAnulado(0L);
        pago.setEventoPrestamo(buscarEvento(idEvento));

        // §4.1 PLAN-FASE3-MOTOR-PAGOS.md: trazabilidad hacia la carga Petro que originó el pago
        // (CobroPetroContableServiceImpl.contabilizarAplicacion agrupa por CRARCDGO). Los demás
        // llamadores del motor (pago manual, aportes, precancelación, reverso) no traen
        // idCargaArchivo en su ContextoPago y quedan exactamente igual que hoy.
        Long idCargaArchivo = ctx != null ? ctx.getIdCargaArchivo() : null;
        if (idCargaArchivo != null) {
            CargaArchivo cargaArchivo = cargaArchivoService.selectById(idCargaArchivo);
            pago.setCargaArchivo(cargaArchivo);
        }

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
            // Ausencia de dato legítima: nada que verificar sin un préstamo.
            return false;
        }

        // TODO O NADA (2026-08-29): mismo defecto que el gemelo conceptual de este método,
        // CargaArchivoPetroServiceImpl.verificarYActualizarEstadoPrestamo (ya corregido esta
        // semana) — antes, si el save final fallaba, se tragaba el error y se devolvía `false`
        // ("un fallo aquí no debe abortar el pago"). El dinero ya había quedado bien aplicado en
        // PagoPrestamo/cuotas ANTES de llegar acá, así que esto no duplica ni pierde plata — pero
        // un préstamo que en los hechos ya no debe nada y se queda VIGENTE en silencio no es solo
        // un dato raro en el comprobante: el padrón, los reportes de cartera y el gate de
        // jubilación lo siguen viendo como préstamo activo. Las ausencias de dato ya están
        // manejadas con `if` explícitos (préstamo sin tabla de amortización, cuotas pendientes >
        // 0) y no cambian; `contarCuotasByPrestamo`/`contarCuotasPendientesByPrestamo` son
        // COUNT — nunca lanzan por ausencia, siempre devuelven una fila (0 incluido) — así que lo
        // único que puede llegar a un catch acá es un fallo real de consulta o de guardado.
        try {
            // El estado operativo vive en idEstado (PRSTIDST). ESPSCDGO es FK al catálogo
            // CRD.ESPS y NUNCA se toca desde aquí.
            Long estadoActual = prestamo.getIdEstado();

            if (esEstadoTerminalPrestamo(estadoActual)) {
                return false;
            }

            // Pedido 10: si este pago dejó al préstamo sin cuotas vencidas y estaba EN_MORA(11),
            // vuelve a VIGENTE(2) de inmediato, en vez de esperar al proceso diario de las 02:00.
            procesoMoraPrestamoService.regularizarPrestamoSiSinMora(prestamo.getCodigo());

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
            throw new RuntimeException("Falló al verificar/actualizar el estado del préstamo "
                + prestamo.getCodigo() + " tras el pago — la cuota pudo haber quedado marcada "
                + "como pagada sin que el préstamo reflejara el cambio de estado. Causa: "
                + e.getMessage(), e);
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

        // C35: DTPRSLOT = lo que respaldan los pagos vigentes (Σ PGPRSLOT). Solo se baja, nunca se sube (los
        // DEP de la migración usan PGPRSLOT para otra cosa); la cuota como estaba queda en CRD.HDTP.
        double slotRespaldado = sumaSaldoOtros(pagos);
        if (SaldoOtrosRespaldo.tieneResiduo(cuota.getSaldoOtros(), slotRespaldado)) {
            historizarResiduo(cuota, slotRespaldado, SaldoOtrosRespaldo.MOTIVO_RECALCULO, null);
        }

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
        if (totalPendiente <= TOLERANCIA_CUOTA) {
            aplicarEstadoCuota(cuota, (long) EstadoCuotaPrestamo.PAGADA);
            if (cuota.getFechaPagado() == null) {
                // C6: la fecha del último pago vigente, no la de hoy (ver calcularSaldosRealesCuota).
                cuota.setFechaPagado(fechaUltimoPago(pagos));
            }
            System.out.println("  → Cuota #" + cuota.getNumeroCuota() + " queda PAGADA");
        } else if (totalPagado > MEDIO_CENTAVO) {
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
    // C35 — DTPRSLOT = lo que respaldan los pagos
    // ========================================================================

    /** Σ PGPRSLOT de los pagos dados, redondeada. */
    private double sumaSaldoOtros(List<PagoPrestamo> pagos) {
        double suma = 0.0;
        if (pagos != null) {
            for (PagoPrestamo pago : pagos) {
                suma += nullSafe(pago.getSaldoOtros());
            }
        }
        return redondear(suma);
    }

    /**
     * Copia la cuota como está a CRD.HDTP (sin evento) y deja DTPRSLOT = respaldado, SIN grabar la cuota (lo
     * hace el llamador). Devuelve el residuo quitado.
     */
    private double historizarResiduo(DetallePrestamo cuota, double respaldado, String motivo, String usuario)
            throws Throwable {
        double residuo = SaldoOtrosRespaldo.residuo(cuota.getSaldoOtros(), respaldado);
        histDetallePrestamoDaoService.save(SaldoOtrosRespaldo.copiarAHistorico(cuota, null, motivo,
            SaldoOtrosRespaldo.usuarioHistorico(usuario)), null);
        System.out.println("  C35: cuota #" + cuota.getNumeroCuota() + " (" + cuota.getCodigo() + ") DTPRSLOT "
            + cuota.getSaldoOtros() + " -> " + respaldado + " (sin respaldo " + residuo + ", " + motivo + ")");
        cuota.setSaldoOtros(respaldado);
        return residuo;
    }

    @Override
    public double saldoOtrosRespaldado(DetallePrestamo cuota) throws Throwable {
        if (cuota == null || cuota.getCodigo() == null) {
            return 0.0;
        }
        return sumaSaldoOtros(pagoPrestamoDaoService.selectVigentesByIdDetallePrestamo(cuota.getCodigo()));
    }

    @Override
    public List<ResiduoSaldoOtros> residuosSaldoOtros(Long idPrestamo) throws Throwable {
        List<ResiduoSaldoOtros> residuos = new ArrayList<>();
        List<DetallePrestamo> cuotas = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
        if (cuotas == null) {
            return residuos;
        }
        for (DetallePrestamo cuota : cuotas) {
            if (nullSafe(cuota.getSaldoOtros()) <= SaldoOtrosRespaldo.TOLERANCIA) {
                continue;
            }
            double respaldado = saldoOtrosRespaldado(cuota);
            if (SaldoOtrosRespaldo.tieneResiduo(cuota.getSaldoOtros(), respaldado)) {
                residuos.add(new ResiduoSaldoOtros(idPrestamo, cuota.getCodigo(),
                    cuota.getNumeroCuota() != null ? Math.round(cuota.getNumeroCuota()) : null,
                    redondear(nullSafe(cuota.getSaldoOtros())), respaldado,
                    SaldoOtrosRespaldo.residuo(cuota.getSaldoOtros(), respaldado)));
            }
        }
        return residuos;
    }

    @Override
    public double normalizarSaldoOtros(DetallePrestamo cuota, String motivo, String usuario) throws Throwable {
        if (cuota == null || nullSafe(cuota.getSaldoOtros()) <= SaldoOtrosRespaldo.TOLERANCIA) {
            return 0.0;
        }
        double respaldado = saldoOtrosRespaldado(cuota);
        if (!SaldoOtrosRespaldo.tieneResiduo(cuota.getSaldoOtros(), respaldado)) {
            return 0.0;
        }
        double residuo = historizarResiduo(cuota, respaldado, motivo, usuario);
        detallePrestamoService.saveSingle(cuota);
        return residuo;
    }

    @Override
    public String controlarSaldoOtrosSinRespaldo(Long idPrestamo, boolean normalizar, String motivo, String usuario,
            String operacion) throws Throwable {
        List<ResiduoSaldoOtros> residuos = residuosSaldoOtros(idPrestamo);
        if (residuos.isEmpty()) {
            return null;
        }
        StringBuilder detalle = new StringBuilder();
        double total = 0.0;
        for (int i = 0; i < residuos.size(); i++) {
            ResiduoSaldoOtros r = residuos.get(i);
            if (i < 10) {
                detalle.append(i == 0 ? "" : "; ").append(r.texto());
            } else if (i == 10) {
                detalle.append("; y ").append(residuos.size() - 10).append(" cuota(s) más");
            }
            total += r.getResiduo();
        }
        total = redondear(total);
        if (!normalizar) {
            throw new IncomeException(SaldoOtrosRespaldo.ERR_SALDO_OTROS_SIN_RESPALDO + ": el préstamo " + idPrestamo
                + " tiene $" + total + " en DTPRSLOT (abono/capital precancelado) sin ningún pago que lo respalde ("
                + detalle + "). Si " + operacion + " se registra así, la tabla mostraría el abono dos veces."
                + " Revise el préstamo con crédito: el valor sin respaldo se quita con el script revisable C35-02"
                + " o, en un re-proceso, enviando reproceso = true (se normaliza y queda en CRD.HDTP).");
        }
        for (ResiduoSaldoOtros r : residuos) {
            DetallePrestamo cuota = detallePrestamoDaoService.find(new DetallePrestamo(), r.getIdCuota());
            normalizarSaldoOtros(cuota, motivo, usuario);
        }
        return "[C35: DTPRSLOT sin respaldo normalizado antes de " + operacion + ", $" + total + " (" + detalle
            + "); valor anterior en CRD.HDTP " + motivo + "]";
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    /**
     * Fecha de pago que le corresponde a una cuota que queda PAGADA a partir de sus pagos
     * vigentes: la mayor {@code PGPRFCHA} de la lista. Si ningún pago trae fecha (datos
     * migrados), se cae a la hora del sistema — el comportamiento que había antes de C6.
     * Así, en un re-proceso de meses pasados la cuota queda pagada en la fecha real del
     * último pago y no en la fecha en que se corrió el proceso.
     */
    private LocalDateTime fechaUltimoPago(List<PagoPrestamo> pagos) {
        LocalDateTime ultima = ultimaFechaPago(pagos);
        return ultima != null ? ultima : LocalDateTime.now();
    }

    /** La mayor {@code PGPRFCHA} de la lista, o null si ningún pago trae fecha. */
    private LocalDateTime ultimaFechaPago(List<PagoPrestamo> pagos) {
        LocalDateTime ultima = null;
        if (pagos != null) {
            for (PagoPrestamo pago : pagos) {
                if (pago.getFecha() != null && (ultima == null || pago.getFecha().isAfter(ultima))) {
                    ultima = pago.getFecha();
                }
            }
        }
        return ultima;
    }

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
             || idEstado == EstadoPrestamo.CANCELADO_POR_NOVACION
             || idEstado == EstadoPrestamo.ANULADO
             || idEstado == EstadoPrestamo.CANCELADO_POR_REESTRUCTURACION
             // Sin desembolsar (1, 6, 7, 14): aún no es un préstamo vivo, no admite pagos ni operaciones.
             || EstadoPrestamo.esNoDesembolsado(idEstado));
    }

    /** Texto del estado para el rechazo: el ANULADO y el 13 se nombran, los demás van por su número. */
    private String textoEstadoTerminal(Long idEstado) {
        if (idEstado != null && idEstado == EstadoPrestamo.ANULADO) {
            return "ANULADO (12)";
        }
        if (idEstado != null && idEstado == EstadoPrestamo.CANCELADO_POR_REESTRUCTURACION) {
            return "CANCELADO_POR_REESTRUCTURACION (13)";
        }
        if (idEstado != null && idEstado == EstadoPrestamo.APROBADO) {
            return "APROBADO (14, pendiente de desembolso)";
        }
        return String.valueOf(idEstado);
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

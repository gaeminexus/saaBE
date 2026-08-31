package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.service.ConfiguracionContabilidadService;
import com.saa.ejb.crd.service.ContabilidadPrestamoService;
import com.saa.ejb.crd.service.ContabilizacionIndividualCreditoService;
import com.saa.ejb.crd.service.dto.ContextoPago;
import com.saa.ejb.crd.service.dto.DesgloseAporte;
import com.saa.ejb.crd.service.dto.MovimientoAporte;
import com.saa.ejb.crd.service.dto.ResultadoAplicacionPago;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.crd.EventoPrestamo;
import com.saa.model.crd.PagoPrestamo;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.TipoAsientos;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Hooks de contabilidad de los procesos de pago de préstamos — implementación real.
 *
 * Reemplaza a {@code ContabilidadPrestamoNoOpImpl} (borrada en este cambio, único {@code @Local}
 * de {@link ContabilidadPrestamoService}: dos beans implementándolo a la vez deja la inyección
 * ambigua y el deployment puede fallar en el arranque de WildFly).
 *
 * <p><b>Fase 1 de PLAN-CIERRE-CONTABLE-TOTAL.md — SOLO {@link #contabilizarPagoConAportes}
 * está lleno.</b> Es el único de los cinco hooks que {@code CobroCreditoServiceImpl} nunca llama
 * por dentro (0 referencias, verificado 2026-08-31): los otros cuatro SÍ se llaman desde
 * {@code procesarCobro}/{@code anularCobro}, que ya generan {@code CBCRASN2} por la misma plata.
 * Encenderlos sin el discriminador de origen que defina el árbitro produce DOS asientos por
 * operación, los dos cuadrados, sin ningún error — ver el comentario de cada uno.
 *
 * @author Sistema SAA
 * @since 2026-08-31
 */
@Stateless
public class ContabilidadPrestamoServiceImpl implements ContabilidadPrestamoService {

    private static final double TOLERANCIA_CUADRE = 0.01;

    @EJB
    private ConfiguracionContabilidadService configuracionContabilidadService;

    @EJB
    private ContabilizacionIndividualCreditoService contabilizacionIndividualCreditoService;

    @EJB
    private AsientoContableService asientoContableService;

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    // =====================================================================
    // Fase 1 — cruce de valores (pagarConAportes). Asiento levantado en
    // LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md §3.5: D cuentas de aporte del socio,
    // diferenciadas por tipo -> H bandas de capital, intereses y seguros.
    // =====================================================================

    @Override
    public Long contabilizarPagoConAportes(ResultadoAplicacionPago resultado, List<MovimientoAporte> movimientos,
            ContextoPago ctx) throws Throwable {
        System.out.println("ContabilidadPrestamoService.contabilizarPagoConAportes - Evento: "
                + (ctx != null ? ctx.getIdEvento() : null));

        if (!configuracionContabilidadService.contabilidadActiva()) {
            System.out.println("  Contabilidad de CRD INACTIVA: pagarConAportes procesado sin"
                    + " generar asiento.");
            return null;
        }
        if (ctx == null || ctx.getIdEvento() == null) {
            throw new IncomeException("No se puede contabilizar el pago con aportes: falta el"
                    + " contexto de la operación (evento).");
        }
        Long idEmpresa = ctx.getIdEmpresa();
        if (idEmpresa == null) {
            // No debería pasar nunca: idEmpresa es obligatorio en SolicitudPagoConAportes desde
            // la Fase 0 (contrato API-EMPRESA-CONTABLE-CRD.md) y crearContexto lo copia siempre.
            throw new IncomeException("No se puede contabilizar el pago con aportes del evento "
                    + ctx.getIdEvento() + ": falta idEmpresa en el contexto de la operación.");
        }

        Long idPlantillaAplicacion = contabilizacionIndividualCreditoService.resolverPlantillaAplicacion(idEmpresa);
        String prefijo = "Pago con aportes - evento " + ctx.getIdEvento();

        // DEBE: cuentas de aporte del socio, diferenciadas por tipo — lo CONSUMIDO.
        // MovimientoAporte.valor viaja NEGATIVO al consumir (ver su javadoc);
        // lineasCruceAportesConsumidos espera valores positivos, el mismo contrato que usa
        // CBCRASN2 con DetalleAportePrecancelacion.
        List<DesgloseAporte> desgloseConsumido = new ArrayList<>();
        double totalDebe = 0.0;
        if (movimientos != null) {
            for (MovimientoAporte movimiento : movimientos) {
                double valor = redondear(Math.abs(nvl(movimiento.getValor())));
                DesgloseAporte renglon = new DesgloseAporte();
                renglon.setIdTipoAporte(movimiento.getIdTipoAporte());
                renglon.setValor(valor);
                desgloseConsumido.add(renglon);
                totalDebe += valor;
            }
        }
        totalDebe = redondear(totalDebe);

        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.addAll(contabilizacionIndividualCreditoService.lineasCruceAportesConsumidos(idPlantillaAplicacion,
                desgloseConsumido, prefijo));

        // HABER: bandas de capital, intereses y seguros que efectivamente liquidó el pago —
        // derivadas de los PagoPrestamo VIGENTES del evento, la MISMA regla que usa CBCRASN2
        // (haberDesdePagos, compartido a propósito — ver su javadoc).
        List<PagoPrestamo> pagos = pagoPrestamoDaoService.selectByEvento(ctx.getIdEvento());
        LocalDate fechaCorte = ctx.getFechaPago() != null ? ctx.getFechaPago().toLocalDate() : LocalDate.now();
        List<DetalleAsiento> haber = contabilizacionIndividualCreditoService.haberDesdePagos(pagos, idEmpresa,
                idPlantillaAplicacion, fechaCorte, prefijo);
        lineas.addAll(haber);

        double totalHaber = 0.0;
        for (DetalleAsiento linea : haber) {
            totalHaber += nvl(linea.getValorHaber()) - nvl(linea.getValorDebe());
        }
        totalHaber = redondear(totalHaber);

        // Cuadre contra el MONTO DE LA OPERACIÓN (regla §4.6 del plan), no solo D=H: el
        // desglose consumido y lo efectivamente liquidado tienen que coincidir los dos con lo
        // que el motor dice que aplicó al préstamo — un asiento mal clasificado también cuadra.
        double valorOperacion = redondear(resultado != null ? resultado.getValorAplicado() : 0.0);
        if (Math.abs(redondear(totalDebe - valorOperacion)) > TOLERANCIA_CUADRE) {
            throw new IncomeException("El desglose de aportes consumidos ($" + totalDebe + ") no"
                    + " coincide con el valor aplicado al préstamo ($" + valorOperacion
                    + ") del evento " + ctx.getIdEvento() + ". No se genera un asiento"
                    + " desbalanceado.");
        }
        if (Math.abs(redondear(totalHaber - valorOperacion)) > TOLERANCIA_CUADRE) {
            throw new IncomeException("Las líneas de haber clasificadas ($" + totalHaber + ") no"
                    + " coinciden con el valor aplicado al préstamo ($" + valorOperacion
                    + ") del evento " + ctx.getIdEvento() + ". No se genera un asiento"
                    + " desbalanceado.");
        }

        Asiento asiento = asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS, fechaCorte,
                prefijo + (ctx.getObservacion() != null ? ": " + ctx.getObservacion() : ""),
                ctx.getUsuario(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        System.out.println("  ✅ contabilizarPagoConAportes OK - Asiento: " + asiento.getCodigo()
                + " - Evento: " + ctx.getIdEvento() + " - Monto: $" + valorOperacion);

        return asiento.getCodigo();
    }

    // =====================================================================
    // Los otros cuatro hooks quedan en null esta fase — todos tienen la misma trampa: el
    // proceso que los llama YA pasa por CobroCreditoServiceImpl.procesarCobro/anularCobro en el
    // circuito normal, que genera CBCRASN2 por la misma plata. Encenderlos sin un discriminador
    // de origen (llamada directa del endpoint vs. llamada interna de CBCR) produce dos asientos
    // por operación, los dos cuadrados — nada lo detecta. Ese discriminador lo define el
    // árbitro (Fase 1bis y siguientes de PLAN-CIERRE-CONTABLE-TOTAL.md §3), no este cambio.
    // =====================================================================

    @Override
    public Long contabilizarPagoCuota(ResultadoAplicacionPago resultado, ContextoPago ctx) throws Throwable {
        System.out.println("ContabilidadPrestamoService.contabilizarPagoCuota - diferido a Fase 1bis"
                + " (PLAN-CIERRE-CONTABLE-TOTAL.md): pagarCuota se llama desde"
                + " CobroCreditoServiceImpl.procesarCobro, que ya genera CBCRASN2 por la misma"
                + " plata; falta el discriminador de origen antes de encender este hook.");
        return null;
    }

    @Override
    public Long contabilizarAbonoCapital(EventoPrestamo evento) throws Throwable {
        System.out.println("ContabilidadPrestamoService.contabilizarAbonoCapital - diferido a Fase 3"
                + " (PLAN-CIERRE-CONTABLE-TOTAL.md, re-bandeo del abono): AbonoCapitalPrestamoServiceImpl"
                + ".aplicar se llama desde CobroCreditoServiceImpl.procesarCobro, que ya genera"
                + " CBCRASN2 por la misma plata; falta el discriminador de origen antes de encender"
                + " este hook.");
        return null;
    }

    @Override
    public Long contabilizarPrecancelacion(EventoPrestamo evento) throws Throwable {
        System.out.println("ContabilidadPrestamoService.contabilizarPrecancelacion - diferido a Fase"
                + " 1bis (PLAN-CIERRE-CONTABLE-TOTAL.md): precancelar se llama desde"
                + " CobroCreditoServiceImpl.procesarCobro (:694 en la Fase 0), que ya genera"
                + " CBCRASN2 por la misma plata; falta el discriminador de origen antes de encender"
                + " este hook.");
        return null;
    }

    @Override
    public Long contabilizarReverso(EventoPrestamo eventoAnulado) throws Throwable {
        System.out.println("ContabilidadPrestamoService.contabilizarReverso - diferido: el reverso"
                + " de un pago con aportes hoy se anula por CobroCreditoServiceImpl.anularCobro"
                + " (reverso por líneas) o por ProcesoPagoPrestamoServiceImpl.anularOperacion, y"
                + " ninguno de los dos pasa por este hook (ver el análisis del ítem 5 del reporte de"
                + " Fase 1: EVPR.EVPRNMAS es el NÚMERO del asiento, no la PK, y es por"
                + " empresa/período — reversar buscando por ese número puede ser circular).");
        return null;
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}

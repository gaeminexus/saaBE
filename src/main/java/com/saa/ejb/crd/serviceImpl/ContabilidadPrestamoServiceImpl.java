package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.HistDetallePrestamoDaoService;
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
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.EventoPrestamo;
import com.saa.model.crd.HistDetallePrestamo;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.PlantillasCredito;
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
    private AsientoService asientoService;

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private HistDetallePrestamoDaoService histDetallePrestamoDaoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private com.saa.ejb.cnt.service.PlantillaService plantillaService;

    @EJB
    private com.saa.ejb.cnt.dao.DetallePlantillaDaoService detallePlantillaDaoService;

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
    // contabilizarPagoCuota sigue en null esta fase: pagarCuota se llama desde
    // CobroCreditoServiceImpl.procesarCobro, que ya genera CBCRASN2 por la MISMA plata que
    // pagarCuota movería — a diferencia de contabilizarAbonoCapital (más abajo), esta sí sería
    // la plata duplicada. Falta el discriminador de origen (idCobroCredito) antes de encender.
    // =====================================================================

    @Override
    public Long contabilizarPagoCuota(ResultadoAplicacionPago resultado, ContextoPago ctx) throws Throwable {
        System.out.println("ContabilidadPrestamoService.contabilizarPagoCuota - diferido a Fase 1bis"
                + " (PLAN-CIERRE-CONTABLE-TOTAL.md): pagarCuota se llama desde"
                + " CobroCreditoServiceImpl.procesarCobro, que ya genera CBCRASN2 por la misma"
                + " plata; falta el discriminador de origen antes de encender este hook.");
        return null;
    }

    // =====================================================================
    // contabilizarAbonoCapital — LLENO 2026-08-31 (Fase 3, re-bandeo del abono, §9.1 C2 del
    // levantamiento). Este comentario existía para avisar que encenderlo sin discriminador
    // duplicaría la plata del abono contra CBCRASN2 — ESA RAZÓN YA NO APLICA ACÁ, y hay que
    // decirlo explícito para que el próximo que lea esto no la reintroduzca:
    //
    // ⚠️ ESTE ASIENTO NO LLEVA LA PLATA DEL ABONO. Lleva solo la RECLASIFICACIÓN entre bandas
    // del capital que sigue vivo después de la re-amortización — el mismo movimiento que hace
    // CierreCarteraServiceImpl.armaCambioBandas en el cierre mensual, pero para un préstamo
    // suelto en el momento del abono. La plata del abono (lo que efectivamente se cobró) la
    // banda CobroCreditoServiceImpl vía ContabilizacionIndividualCreditoService#haberDesdePagos
    // dentro de CBCRASN2 — ESE es el asiento que "cierra" el dinero. Si algún día a alguien le
    // parece que acá "falta" una línea de banco o de cuenta por cobrar, NO la agregue: es
    // exactamente el error que este comentario existe para prevenir — un asiento puramente
    // interno (Debe=Haber, nunca entra ni sale plata de la cartera) que además incluyera el
    // movimiento de caja quedaría contando el abono dos veces.
    //
    // El otro motivo por el que SÍ se puede encender sin idCobroCredito: hoy el ÚNICO camino
    // de aplicación de un abono a capital es CobroCreditoServiceImpl (PrestamoRest#abonarCapital
    // rechaza la aplicación directa — ver su javadoc), así que idEmpresa siempre llega derivado
    // del servidor (derivarEmpresaCobro), nunca elegido por un cliente.
    // =====================================================================

    @Override
    public Long contabilizarAbonoCapital(EventoPrestamo evento, Long idEmpresa) throws Throwable {
        System.out.println("ContabilidadPrestamoService.contabilizarAbonoCapital - Evento: "
                + (evento != null ? evento.getCodigo() : null));

        if (!configuracionContabilidadService.contabilidadActiva()) {
            System.out.println("  Contabilidad de CRD INACTIVA: abono a capital procesado sin"
                    + " generar asiento de re-bandeo.");
            return null;
        }
        if (evento == null || evento.getCodigo() == null) {
            throw new IncomeException("No se puede contabilizar el re-bandeo del abono: falta el evento.");
        }
        if (idEmpresa == null) {
            throw new IncomeException("No se puede contabilizar el re-bandeo del abono " + evento.getCodigo()
                    + ": falta idEmpresa.");
        }
        Prestamo prestamo = evento.getPrestamo();
        if (prestamo == null || prestamo.getProducto() == null) {
            throw new IncomeException("El evento " + evento.getCodigo()
                    + " no tiene préstamo o producto asignado; no se puede clasificar por banda.");
        }
        Long idProducto = prestamo.getProducto().getCodigo();

        List<HistDetallePrestamo> historizadas = histDetallePrestamoDaoService.selectByEvento(evento.getCodigo());
        if (historizadas == null || historizadas.isEmpty()) {
            throw new IncomeException("El evento " + evento.getCodigo()
                    + " no tiene ninguna cuota historizada en CRD.HDTP; no se puede armar el"
                    + " re-bandeo. AbonoCapitalPrestamoServiceImpl.aplicar siempre historiza al"
                    + " menos una cuota — revise si el evento es realmente de tipo ABONO_CAPITAL.");
        }

        // Mismo criterio que el reverso de un abono (ProcesoPagoPrestamoServiceImpl,
        // rama TIPO_ABONO_CAPITAL de anularOperacion): las cuotas VIVAS con numeroCuota >= la
        // primera historizada son exactamente las que generó este abono — no hace falta un FK
        // nuevo en CRD.DTPR, ese código ya prueba que el criterio alcanza.
        Double minNumero = histDetallePrestamoDaoService.selectMinNumeroCuotaByEvento(evento.getCodigo());
        List<DetallePrestamo> nuevas = new ArrayList<>();
        if (minNumero != null) {
            for (DetallePrestamo cuota : detallePrestamoDaoService.selectByPrestamo(prestamo.getCodigo())) {
                if (cuota.getNumeroCuota() != null && cuota.getNumeroCuota() >= minNumero) {
                    nuevas.add(cuota);
                }
            }
        }
        if (nuevas.isEmpty()) {
            throw new IncomeException("El evento " + evento.getCodigo()
                    + " no tiene ninguna cuota viva con numeroCuota >= " + minNumero
                    + "; no se puede armar el re-bandeo. Revise CRD.DTPR del préstamo "
                    + prestamo.getCodigo() + ".");
        }

        LocalDate fechaCorte = evento.getFecha() != null ? evento.getFecha().toLocalDate() : LocalDate.now();
        String prefijo = "Re-bandeo abono a capital - evento " + evento.getCodigo();

        List<DetalleAsiento> lineas = contabilizacionIndividualCreditoService.lineasReclasificacionAbonoCapital(
                idProducto, idEmpresa, historizadas, nvl(evento.getValor()), nuevas, fechaCorte, prefijo);

        if (lineas.isEmpty()) {
            System.out.println("  ContabilidadPrestamoService.contabilizarAbonoCapital - evento "
                    + evento.getCodigo() + ": el abono no movió el capital de ninguna banda; no se"
                    + " genera asiento de re-bandeo.");
            return null;
        }

        Asiento asiento = asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS, fechaCorte,
                prefijo, evento.getUsuario(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        System.out.println("  ✅ contabilizarAbonoCapital OK (re-bandeo) - Asiento: " + asiento.getCodigo()
                + " - Evento: " + evento.getCodigo());

        return asiento.getCodigo();
    }

    // =====================================================================
    // Precancelación con aportes CONSUMIDOS — cruce de valores, mismo asiento que
    // contabilizarPagoConAportes (§3.5), gateado por idCobroCredito (2026-08-31, circuito de
    // cobros con aportes, decisión del usuario). Con idCobroCredito != null la llamada nació
    // de CobroCreditoServiceImpl.procesarCobro, que ya genera su propio asiento (CBCRASN2)
    // por la misma plata — este método devuelve null sin tocar nada.
    // =====================================================================

    @Override
    public Long contabilizarPrecancelacion(EventoPrestamo evento, List<MovimientoAporte> movimientos,
            ContextoPago ctx) throws Throwable {
        System.out.println("ContabilidadPrestamoService.contabilizarPrecancelacion - Evento: "
                + (ctx != null ? ctx.getIdEvento() : null));

        if (!configuracionContabilidadService.contabilidadActiva()) {
            System.out.println("  Contabilidad de CRD INACTIVA: precancelación procesada sin"
                    + " generar asiento.");
            return null;
        }
        if (ctx == null || ctx.getIdEvento() == null) {
            throw new IncomeException("No se puede contabilizar la precancelación: falta el"
                    + " contexto de la operación (evento).");
        }
        if (ctx.getIdCobroCredito() != null) {
            // CASO B: la llamada nació de CBCR (hubo depósito) — ese asiento lo genera
            // CobroCreditoServiceImpl#generarAsientoDefinitivo (CBCRASN2). Generarlo acá
            // también duplicaría la misma plata.
            System.out.println("  Llamada originada en CobroCredito " + ctx.getIdCobroCredito()
                    + ": el asiento lo genera CBCR (CBCRASN2), no este hook.");
            return null;
        }
        if (movimientos == null || movimientos.isEmpty()) {
            // Precancelación 100% efectivo: no hay aportes consumidos, nada que cruzar. La
            // parte de efectivo de una llamada directa no genera asiento en este hook — no
            // hay ninguna cuenta transitoria que cerrar fuera de CBCR (una precancelación
            // directa 100% efectivo no es un caso contemplado hoy: el endpoint directo existe
            // para el caso de aportes).
            return null;
        }
        Long idEmpresa = ctx.getIdEmpresa();
        if (idEmpresa == null) {
            throw new IncomeException("No se puede contabilizar la precancelación del evento "
                    + ctx.getIdEvento() + ": falta idEmpresa en el contexto de la operación.");
        }

        Long idPlantillaAplicacion = contabilizacionIndividualCreditoService.resolverPlantillaAplicacion(idEmpresa);
        String prefijo = "Precancelación - evento " + ctx.getIdEvento();

        // DEBE: cuentas de aporte del socio, diferenciadas por tipo — lo CONSUMIDO. Mismo
        // contrato que contabilizarPagoConAportes: MovimientoAporte.valor viaja NEGATIVO,
        // lineasCruceAportesConsumidos espera valores positivos.
        List<DesgloseAporte> desgloseConsumido = new ArrayList<>();
        double totalDebe = 0.0;
        for (MovimientoAporte movimiento : movimientos) {
            double valor = redondear(Math.abs(nvl(movimiento.getValor())));
            DesgloseAporte renglon = new DesgloseAporte();
            renglon.setIdTipoAporte(movimiento.getIdTipoAporte());
            renglon.setValor(valor);
            desgloseConsumido.add(renglon);
            totalDebe += valor;
        }
        totalDebe = redondear(totalDebe);

        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.addAll(contabilizacionIndividualCreditoService.lineasCruceAportesConsumidos(idPlantillaAplicacion,
                desgloseConsumido, prefijo));

        // HABER: cuentas por cobrar liquidadas por los PagoPrestamo VIGENTES del evento —
        // misma regla que contabilizarPagoConAportes (haberDesdePagos, compartido a
        // propósito). En CASO A (idCobroCredito null) TODO el evento se financió con
        // aportes — no hay depósito — así que esta lista es exactamente lo que financiaron
        // los aportes, sin necesidad de separar por fuente.
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

        // Cuadre D=H directo (no contra un "valor de operación" externo, a diferencia de
        // contabilizarPagoConAportes: acá no hay ResultadoAplicacionPago — el evento ES la
        // fuente de verdad, y en CASO A los aportes son el 100% de lo que pagó el evento).
        if (Math.abs(redondear(totalDebe - totalHaber)) > TOLERANCIA_CUADRE) {
            throw new IncomeException("El cruce de aportes de la precancelación no cuadra:"
                    + " aportes consumidos $" + totalDebe + " vs. liquidado del préstamo $"
                    + totalHaber + " (evento " + ctx.getIdEvento() + "). No se genera un"
                    + " asiento desbalanceado.");
        }

        Asiento asiento = asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS, fechaCorte,
                prefijo + (ctx.getObservacion() != null ? ": " + ctx.getObservacion() : ""),
                ctx.getUsuario(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        System.out.println("  ✅ contabilizarPrecancelacion OK - Asiento: " + asiento.getCodigo()
                + " - Evento: " + ctx.getIdEvento() + " - Monto: $" + totalDebe);

        return asiento.getCodigo();
    }

    // =====================================================================
    // Reverso — SOLO del asiento que generó UN HOOK de contabilidad, nunca el de CBCR.
    // El discriminador es evento.getNumeroAsiento(): verificado (2026-08-31) que los ÚNICOS
    // tres escritores de EventoPrestamo.numeroAsiento en todo el proyecto son hooks de
    // contabilidad (ProcesoPagoPrestamoServiceImpl al aplicar el asiento de
    // contabilizarPagoConAportes/contabilizarPrecancelacion, y
    // AbonoCapitalPrestamoServiceImpl con el mismo patrón) — ninguna otra ruta lo toca. Así
    // que "tiene numeroAsiento" ⟺ "un hook generó un asiento para este evento" se sostiene
    // POR CONSTRUCCIÓN, no por convención: no puede desincronizarse como sí podría un flag
    // aparte que alguien deje en true sin asiento real.
    //
    // Por eso este método CUBRE CUALQUIER HOOK que ponga numeroAsiento, presentes y futuros
    // — no hay que agregarle un caso por cada tipo de operación. El día que se llene
    // contabilizarPagoCuota o contabilizarAbonoCapital (con su propio discriminador
    // idCobroCredito), el reverso de esos ya queda cubierto acá sin tocar este método.
    //
    // En CASO B (idCobroCredito != null) contabilizarPrecancelacion/contabilizarPagoConAportes
    // dejan numeroAsiento en null a propósito: ese asiento es CBCRASN2, vive en
    // CobroCredito.asientoDefinitivo (otro campo, en otra entidad, que este método ni ve) y lo
    // reversa CobroCreditoServiceImpl#anularCobro, no este hook.
    // =====================================================================

    @Override
    public Long contabilizarReverso(EventoPrestamo eventoAnulado) throws Throwable {
        System.out.println("ContabilidadPrestamoService.contabilizarReverso - Evento: "
                + (eventoAnulado != null ? eventoAnulado.getCodigo() : null));

        if (eventoAnulado == null || eventoAnulado.getNumeroAsiento() == null) {
            // Sin asiento propio que reversar: contabilidad inactiva cuando se generó, CASO B
            // (el asiento es de CBCR, no de este hook), o un tipo de operación que este hook
            // no contabiliza.
            return null;
        }
        String usuario = eventoAnulado.getUsuarioAnulacion() != null
                ? eventoAnulado.getUsuarioAnulacion() : "SISTEMA";
        asientoService.anulaAsiento(eventoAnulado.getNumeroAsiento(), usuario,
                "Reverso de la operación " + eventoAnulado.getCodigo() + " ("
                        + eventoAnulado.getTipoOperacion() + ")"
                        + (eventoAnulado.getMotivoAnulacion() != null
                                ? ": " + eventoAnulado.getMotivoAnulacion() : ""));
        System.out.println("  ↩️ Asiento " + eventoAnulado.getNumeroAsiento() + " reversado"
                + " (evento " + eventoAnulado.getCodigo() + ")");
        return eventoAnulado.getNumeroAsiento();
    }

    // =====================================================================
    // Entrega del préstamo (2026-09-01, PLAN-DESEMBOLSO-PRESTAMO.md §5 paso 3).
    // =====================================================================

    /**
     * Posiciones aux1 de una plantilla de entrega — SON de esa plantilla y de ninguna otra
     * (ver el javadoc de {@link PlantillasCredito#ENTREGA_PRENDARIO}). {@code auxBien} es
     * {@code null} para la 34 (quirografario): no tiene línea de "el bien".
     */
    private record MapeoPlantillaEntrega(int auxOrdenCartera, int auxDocumentosGarantia,
            Integer auxBien, int auxSociosPorPagar) {
    }

    @Override
    public Long contabilizarEntrega(Prestamo prestamo, List<DetallePrestamo> cuotas, Long idEmpresa,
            double montoOperacion, String usuario) throws Throwable {
        System.out.println("ContabilidadPrestamoService.contabilizarEntrega - Préstamo: "
                + (prestamo != null ? prestamo.getCodigo() : null));

        if (!configuracionContabilidadService.contabilidadActiva()) {
            System.out.println("  Contabilidad de CRD INACTIVA: préstamo "
                    + (prestamo != null ? prestamo.getCodigo() : null)
                    + " aprobado sin generar el asiento de entrega.");
            return null;
        }
        if (prestamo == null || prestamo.getCodigo() == null) {
            throw new IncomeException("No se puede contabilizar la entrega: falta el préstamo.");
        }
        if (idEmpresa == null) {
            throw new IncomeException("No se puede contabilizar la entrega del préstamo "
                    + prestamo.getCodigo() + ": falta idEmpresa.");
        }
        if (prestamo.getProducto() == null || prestamo.getProducto().getTipoPrestamo() == null) {
            throw new IncomeException("El préstamo " + prestamo.getCodigo()
                    + " no tiene producto o tipo de préstamo asignado; no se puede resolver la"
                    + " plantilla del asiento de entrega.");
        }

        // ⚠️ Mapeo producto → familia SIN precedente verificado en el resto del código (no
        // hay ningún otro lugar en el proyecto que compare Producto.tipoPrestamo.nombre
        // contra literales "PRENDARIO"/"HIPOTECARIO"/"QUIROGRAFARIO" — CARGA-INICIAL-BANDAS-
        // PRODUCTO.md muestra que hay MÁS productos por familia (EMERGENTE, CENAPRO, RESTR.,
        // NOVACION...) que probablemente NO tienen ese nombre literal en CRD.TPPR.TPPRNMBR, y
        // ese mismo documento avisa que su lista de códigos es de prueba y puede no calzar
        // contra producción. Si el préstamo es de uno de esos productos "de la misma
        // familia pero con otro nombre", este método lo va a RECHAZAR (comportamiento
        // seguro por diseño: "cualquier otro → rechazar", §5 del plan) en vez de asumir la
        // plantilla — pero eso puede significar que un producto que SÍ debería tener
        // plantilla quede bloqueado. Reportado al árbitro; confirmar antes de dar por
        // cerrado este ítem.
        String tipoPrestamoNombre = prestamo.getProducto().getTipoPrestamo().getNombre();
        String familia = (tipoPrestamoNombre != null) ? tipoPrestamoNombre.trim().toUpperCase() : "";

        int alterno;
        MapeoPlantillaEntrega mapeo;
        if ("PRENDARIO".equals(familia)) {
            alterno = PlantillasCredito.ENTREGA_PRENDARIO;
            mapeo = new MapeoPlantillaEntrega(6, 7, 8, 9);
        } else if ("HIPOTECARIO".equals(familia)) {
            alterno = PlantillasCredito.ENTREGA_HIPOTECARIO;
            mapeo = new MapeoPlantillaEntrega(6, 7, 8, 9);
        } else if ("QUIROGRAFARIO".equals(familia)) {
            alterno = PlantillasCredito.ENTREGA_QUIROGRAFARIO;
            mapeo = new MapeoPlantillaEntrega(6, 7, null, 8);
        } else {
            throw new IncomeException("El producto " + prestamo.getProducto().getNombre()
                    + " (tipo de préstamo '" + tipoPrestamoNombre + "') no tiene plantilla de"
                    + " asiento de entrega configurada. Solo PRENDARIO (alterno 9), HIPOTECARIO"
                    + " (13) y QUIROGRAFARIO (34) la tienen; no se elige una plantilla por"
                    + " defecto para el préstamo " + prestamo.getCodigo() + ".");
        }

        Long idPlantilla = plantillaService.codigoByAlterno(alterno, idEmpresa);
        if (idPlantilla == null) {
            throw new IncomeException("No existe la plantilla contable alterno " + alterno
                    + " para la empresa " + idEmpresa + ".");
        }
        if (prestamo.getFechaInicio() == null) {
            throw new IncomeException("El préstamo " + prestamo.getCodigo()
                    + " no tiene fecha de inicio; no se puede distribuir el capital en bandas"
                    + " por plazo.");
        }

        String prefijo = "Entrega préstamo " + prestamo.getCodigo();
        LocalDate fechaInicio = prestamo.getFechaInicio().toLocalDate();

        // DEBE: capital distribuido en las 5 bandas por plazo, según los días de la fecha de
        // inicio del préstamo al vencimiento de cada cuota — el mismo rango que describen
        // literalmente las 5 primeras líneas de la plantilla 34 (sql/156: "DE 1 A 30 DIAS" …
        // "DE MAS DE 360 DIAS"). NO es el modelo dinámico de bandas de ClasificadorBandaService
        // (CRD.BNDP): esta plantilla trae las 5 cuentas ya fijas en aux1 1-5, así que se
        // clasifica contra esos rangos literales, no contra la parametrización dinámica.
        double[] montosPorBanda = new double[5];
        for (DetallePrestamo cuota : cuotas) {
            if (cuota.getFechaVencimiento() == null || cuota.getCapital() == null) {
                continue;
            }
            long dias = Math.max(1,
                    ChronoUnit.DAYS.between(fechaInicio, cuota.getFechaVencimiento().toLocalDate()));
            int indice = (dias <= 30) ? 0 : (dias <= 90) ? 1 : (dias <= 180) ? 2 : (dias <= 360) ? 3 : 4;
            montosPorBanda[indice] += cuota.getCapital();
        }

        List<DetalleAsiento> lineas = new ArrayList<>();
        double totalCapital = 0.0;
        for (int i = 0; i < 5; i++) {
            double valorBanda = redondear(montosPorBanda[i]);
            if (valorBanda > TOLERANCIA_CUADRE) {
                lineas.add(lineaEntrega(idPlantilla, i + 1, valorBanda, true, alterno,
                        prefijo + " - banda " + (i + 1)));
                totalCapital += valorBanda;
            }
        }
        totalCapital = redondear(totalCapital);

        if (Math.abs(redondear(totalCapital - montoOperacion)) > TOLERANCIA_CUADRE) {
            throw new IncomeException("El capital distribuido en bandas del préstamo "
                    + prestamo.getCodigo() + " ($" + totalCapital + ") no coincide con el monto"
                    + " de la operación ($" + montoOperacion + "). No se genera un asiento"
                    + " desbalanceado.");
        }

        // "El bien" (aux1=8 en 9/13, ausente en 34) solo lleva valor si la plantilla lo tiene
        // Y el préstamo ya tiene valor asegurado — hoy 0 por política mientras no exista
        // póliza (mismo criterio que PrestamoServiceImpl#generarAmortizacion, seguro de
        // incendio en $0 "no se cobra mientras no exista la póliza que lo respalde").
        double valorBien = (mapeo.auxBien() != null) ? redondear(nvl(prestamo.getValorAsegurado())) : 0.0;

        // DEBE: cuenta de orden "cartera de créditos" — espeja el DEBE real (totalCapital) MÁS
        // el valor del bien, para cuadrar contra las dos líneas HABER de garantía de abajo.
        double totalOrdenDebe = redondear(totalCapital + valorBien);
        lineas.add(lineaEntrega(idPlantilla, mapeo.auxOrdenCartera(), totalOrdenDebe, true, alterno,
                prefijo + " - cartera de créditos (orden)"));

        // HABER: documentos en garantía — el pagaré, común a todo crédito, por el monto real.
        lineas.add(lineaEntrega(idPlantilla, mapeo.auxDocumentosGarantia(), totalCapital, false,
                alterno, prefijo + " - documentos en garantía (orden)"));

        // HABER: el bien — solo si la plantilla lo tiene y hay valor asegurado > 0.
        if (mapeo.auxBien() != null && valorBien > TOLERANCIA_CUADRE) {
            lineas.add(lineaEntrega(idPlantilla, mapeo.auxBien(), valorBien, false, alterno,
                    prefijo + " - bien en garantía (orden)"));
        }

        // HABER: SOCIOS POR PAGAR — la cuenta puente, por el monto real del préstamo.
        lineas.add(lineaEntrega(idPlantilla, mapeo.auxSociosPorPagar(), totalCapital, false, alterno,
                prefijo + " - socios por pagar"));

        double totalDebe = 0.0;
        double totalHaber = 0.0;
        for (DetalleAsiento linea : lineas) {
            totalDebe += nvl(linea.getValorDebe());
            totalHaber += nvl(linea.getValorHaber());
        }
        totalDebe = redondear(totalDebe);
        totalHaber = redondear(totalHaber);
        if (Math.abs(redondear(totalDebe - totalHaber)) > TOLERANCIA_CUADRE) {
            throw new IncomeException("El asiento de entrega del préstamo " + prestamo.getCodigo()
                    + " no cuadra: DEBE $" + totalDebe + ", HABER $" + totalHaber
                    + ". No se genera un asiento desbalanceado.");
        }

        Asiento asiento = asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS,
                fechaInicio, prefijo, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        System.out.println("  ✅ contabilizarEntrega OK - Préstamo: " + prestamo.getCodigo()
                + " - Asiento: " + asiento.getCodigo() + " - Monto: $" + totalCapital);

        return asiento.getCodigo();
    }

    /** Línea de una plantilla de entrega por aux1 explícito — posicional, ver {@link MapeoPlantillaEntrega}. */
    private DetalleAsiento lineaEntrega(Long idPlantilla, int aux1, double valor, boolean debe, int alterno,
            String descripcion) throws Throwable {
        DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla, aux1);
        if (linea == null || linea.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno " + alterno + " no tiene la línea aux1=" + aux1 + ".");
        }
        PlanCuenta cuenta = linea.getPlanCuenta();
        DetalleAsiento detalle = new DetalleAsiento();
        detalle.setPlanCuenta(cuenta);
        detalle.setNumeroCuenta(cuenta.getCuentaContable());
        detalle.setNombreCuenta(cuenta.getNombre());
        detalle.setDescripcion(descripcion);
        detalle.setValorDebe(debe ? redondear(valor) : 0.0);
        detalle.setValorHaber(debe ? 0.0 : redondear(valor));
        return detalle;
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}

package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.crd.dao.CorridaCierreCarteraDaoService;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.HistDetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.ClasificadorBandaService;
import com.saa.ejb.crd.service.ConfiguracionContabilidadService;
import com.saa.ejb.crd.service.ContabilidadPrestamoService;
import com.saa.ejb.crd.service.ContabilizacionIndividualCreditoService;
import com.saa.ejb.crd.service.dto.BandaProductoDetalle;
import com.saa.ejb.crd.service.dto.ContextoPago;
import com.saa.ejb.crd.service.dto.DesgloseAporte;
import com.saa.ejb.crd.service.dto.MovimientoAporte;
import com.saa.ejb.crd.service.dto.ResultadoAplicacionPago;
import com.saa.ejb.crd.service.dto.ResultadoClasificacionBanda;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.crd.CorridaCierreCartera;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.EventoPrestamo;
import com.saa.model.crd.HistDetallePrestamo;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.CrdLineaAsiento;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.MovimientoCuentaPlantilla;
import com.saa.rubros.PlantillasCredito;
import com.saa.rubros.TipoAsientos;
import com.saa.rubros.TipoCarteraBanda;

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

    /**
     * CORRECCIONES-2026-09-02.md §1: DetalleAsiento.descripcion (DTASDSCR) es VARCHAR2(200) y
     * los métodos de {@code ContabilizacionIndividualCreditoService} le APPENDEAN un sufijo
     * (" - banda N", " - consumo aporte <tipo>", …) al prefijo que reciben — dejar margen para
     * eso, no llenar los 200 con la identificación del partícipe.
     */
    private static final int MAX_LARGO_GLOSA_CRUCE = 140;

    @EJB
    private ConfiguracionContabilidadService configuracionContabilidadService;

    @EJB
    private ContabilizacionIndividualCreditoService contabilizacionIndividualCreditoService;

    @EJB
    private CorridaCierreCarteraDaoService corridaCierreCarteraDaoService;

    @EJB
    private AsientoContableService asientoContableService;

    @EJB
    private AsientoService asientoService;

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private HistDetallePrestamoDaoService histDetallePrestamoDaoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private com.saa.ejb.cnt.service.PlantillaService plantillaService;

    @EJB
    private com.saa.ejb.cnt.dao.DetallePlantillaDaoService detallePlantillaDaoService;

    /** PLAN-ENTREGA-BANDAS-DINAMICAS.md: la entrega clasifica el capital con el mismo modelo
     * dinámico que ya usa el cobro (ContabilizacionIndividualCreditoServiceImpl.lineaBandaCapital),
     * en vez de la escalera fija de 5 tramos de antes. */
    @EJB
    private ClasificadorBandaService clasificadorBandaService;

    @EJB
    private com.saa.ejb.cnt.dao.PlanCuentaDaoService planCuentaDaoService;

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
        String prefijo = construirGlosaCrucePorParticipe(resultado != null ? resultado.getIdPrestamo() : null,
                ctx.getIdEvento());

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

        // Cierre de apertura del camino DIRECTO (2026-09-05, §9 del diseño, ÍTEM 3): este
        // método es el otro camino sin depósito — el cruce de valores, incluido el de jubilados
        // de omen-saa-1 (el hueco de $16.231,60 reportado). MISMO helper que
        // contabilizarPrecancelacion, nunca una copia: totalHaber es todo lo que el evento
        // liquidó del préstamo, el helper le resta el capital futuro y agrega, si corresponde,
        // las dos líneas invertidas al MISMO asiento.
        agregaCierreAperturaCaminoDirecto(lineas, idEmpresa, totalHaber, pagos, fechaCorte,
                "cruce de valores evento " + ctx.getIdEvento());

        Asiento asiento = asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS, fechaCorte,
                prefijo + (ctx.getObservacion() != null ? ": " + ctx.getObservacion() : ""),
                ctx.getUsuario(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        System.out.println("  ✅ contabilizarPagoConAportes OK - Asiento: " + asiento.getCodigo()
                + " - Evento: " + ctx.getIdEvento() + " - Monto: $" + valorOperacion);

        return asiento.getCodigo();
    }

    /**
     * CORRECCIONES-2026-09-02.md §1: el asiento del cruce de valores es de UN partícipe, UN
     * préstamo, UNA operación — a diferencia del asiento de aplicación de la carga Petro (1000+
     * personas, no hay nombre que poner), acá contabilidad necesita saber de quién es sin salir
     * del asiento. Construye la glosa con nombre/cédula/código asoprep del titular del préstamo.
     *
     * <p>Tolerante a la ausencia de esos datos (se omite el fragmento que falte, un asiento no
     * se cae por una glosa incompleta) — pero NO a la ausencia del préstamo: sin él no hay de
     * quién hablar, y eso es un fallo real, no un dato ausente legítimo.
     */
    private String construirGlosaCrucePorParticipe(Long idPrestamo, Long idEvento) throws Throwable {
        if (idPrestamo == null) {
            throw new IncomeException("No se puede generar la glosa del cruce de valores: falta"
                    + " el préstamo de la operación (evento " + idEvento + ").");
        }

        // find() (em.find) en vez de selectById: un código inexistente no debe tumbar la
        // glosa con un NoResultException — sin el préstamo simplemente se omiten sus datos.
        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
        Entidad entidad = prestamo != null ? prestamo.getEntidad() : null;

        List<String> datos = new ArrayList<>();
        if (entidad != null && entidad.getNumeroIdentificacion() != null) {
            datos.add("CI " + entidad.getNumeroIdentificacion());
        }
        if (entidad != null && entidad.getRolPetroComercial() != null) {
            datos.add("asoprep " + entidad.getRolPetroComercial());
        }

        StringBuilder glosa = new StringBuilder("Cruce de valores");
        if (entidad != null && entidad.getRazonSocial() != null) {
            glosa.append(" - ").append(entidad.getRazonSocial());
        }
        if (!datos.isEmpty()) {
            glosa.append(" (").append(String.join(", ", datos)).append(")");
        }
        glosa.append(" - préstamo ").append(idPrestamo).append(" - evento ").append(idEvento);

        String resultado = glosa.toString();
        return resultado.length() > MAX_LARGO_GLOSA_CRUCE
                ? resultado.substring(0, MAX_LARGO_GLOSA_CRUCE)
                : resultado;
    }

    // =====================================================================
    // Cierre de apertura en el camino DIRECTO (2026-09-05) — §9 de
    // docs/logica-negocio/crd/DISENO-CIERRE-APERTURA-SOLO-LO-ABIERTO.md.
    //
    // §1-§8 de ese documento (implementados en CobroCreditoServiceImpl.generarAsientoReparto/
    // generarAsientoDefinitivo) cubren SOLO procesarCobro — el camino CON depósito (Petro,
    // cobros con cuenta bancaria). Verificado con dos logs de producción (eventos 481 y 444):
    // una precancelación y un cruce de valores reales pasan por ACÁ
    // (contabilizarPrecancelacion/contabilizarPagoConAportes), nunca por procesarCobro. Sin
    // este bloque, esos dos caminos jamás cierran la apertura — es la causa del hueco de
    // $16.231,60 reportado en la corrida de jubilados de omen-saa-1.
    //
    // Compartido entre los dos métodos (no una copia): agrega, EN EL MISMO ASIENTO que cada
    // método ya arma, dos líneas más que reversan la apertura por lo que esta operación
    // efectivamente pagó del cronograma normal (vencido + cuota del mes) — nunca por el capital
    // futuro, que la apertura nunca abrió.
    // =====================================================================

    /**
     * Agrega, SI CORRESPONDE, las dos líneas que cierran la apertura del lado préstamos por lo
     * que esta operación pagó del cronograma normal — la contraparte del camino directo a lo
     * que {@code CobroCreditoServiceImpl#generarAsientoReparto}/{@code generarAsientoDefinitivo}
     * hacen para el camino con depósito.
     *
     * <p><b>Monto:</b> {@code totalLiquidadoPrestamos} (todo lo que este evento liquidó del
     * préstamo — capital por banda, interés, mora, seguros; el mismo total que ya usa el
     * llamador para su propio cuadre) MENOS {@link ContabilizacionIndividualCreditoService
     * #capitalFuturoPosteriorACorte} — el capital que vence después del corte de la corrida de
     * cierre viva NUNCA se abrió, así que no hay nada que cerrar por él.</p>
     *
     * <p><b>Los dos lados van INVERTIDOS respecto de cómo los declara la plantilla, a
     * propósito.</b> {@code PlantillasCredito#APERTURA_PLANILLA_MENSUAL} declara
     * {@code PRESTAMOS_POR_APLICAR} (aux 4, cuenta 2.3.02.10) en HABER y
     * {@code PRESTAMOS_POR_COBRAR} (aux 2, cuenta 1.4.05.10) en DEBE, porque así es como la
     * apertura mensual los ABRE ({@code CierreCarteraServiceImpl.armaApertura}). Acá se está
     * DESHACIENDO esa apertura, así que los dos lados se invierten por definición: DEBE
     * 2.3.02.10, HABER 1.4.05.10. Se lee {@code getMovimiento()} de cada línea igual, y se
     * ignora a propósito — mismo criterio que la línea de transitoria forzada de
     * {@code CobroCreditoServiceImpl#generarAsientoDefinitivo}.</p>
     *
     * <p><b>Sólo el lado préstamos.</b> La apertura también abre aportes (1.4.05.05/2.3.02.05),
     * pero eso abre el aporte MENSUAL esperado — una precancelación o un cruce de valores
     * consumen el saldo acumulado del socio, que es otra cosa. Esas líneas no se tocan
     * (supuesto declarado del árbitro, §9.4 del diseño; si es incorrecto, es una línea de
     * cambio, no un rediseño).</p>
     *
     * <p><b>2026-09-05, corregido — la corrida que ABRE un mes vive registrada bajo el mes
     * ANTERIOR (el que cerró).</b> {@code CierreCarteraServiceImpl.ejecutar} graba
     * {@code corrida.anio/mes} como el período que CIERRA, pero la apertura que ese mismo
     * proceso genera corresponde al mes SIGUIENTE ({@code fechaProceso}). Por eso el corte se
     * busca con {@code selectUltimaEjecutadaAntesDe(idEmpresa, año, mes de fechaCorte)} —
     * última corrida EJECUTADA antes del período de la operación, ya resuelve el cruce de año —
     * y el corte de esa apertura sale de {@code getFechaProceso()} (el 01 del mes abierto)
     * llevado a su último día, NUNCA de {@code getFechaCorte()} (el corte del CIERRE del mes
     * anterior, un mes antes de lo que corresponde) ni derivado de {@code fechaCorte} de esta
     * operación (dos formas de derivar el mismo dato es la trampa que este proyecto viene
     * pagando caro).</p>
     *
     * <p><b>Guardas — nunca bloquean la operación:</b> sin corrida ejecutada previa al período
     * de {@code fechaCorte}, no se agrega nada (no se encontró qué apertura cerrar, con traza).
     * Monto resultante $0 (o negativo por redondeo), no se agrega nada (un par de líneas en
     * cero es ruido).</p>
     *
     * @param lineas                 la lista de líneas del asiento en construcción — se le
     *                               agregan las dos nuevas al final, si corresponde
     * @param totalLiquidadoPrestamos todo lo que el evento liquidó del préstamo en esta
     *                               operación (capital+interés+mora+seguros), ANTES de restar
     *                               el capital futuro
     * @param pagos                  los {@code PagoPrestamo} vigentes del evento — el mismo
     *                               insumo que ya usa {@code haberDesdePagos}
     * @param descripcionOperacion   p. ej. {@code "precancelación evento 481"} — sin el prefijo
     *                               fijo, que este método agrega
     */
    private void agregaCierreAperturaCaminoDirecto(List<DetalleAsiento> lineas, Long idEmpresa,
            double totalLiquidadoPrestamos, List<PagoPrestamo> pagos, LocalDate fechaCorte,
            String descripcionOperacion) throws Throwable {
        Long anio = Long.valueOf(fechaCorte.getYear());
        Long mes = Long.valueOf(fechaCorte.getMonthValue());
        // La corrida que abrió el mes de esta operación es la que se EJECUTÓ el mes anterior
        // (la que lo cerró) — nunca la "corrida viva del mes de la operación", que no existe.
        CorridaCierreCartera corrida = corridaCierreCarteraDaoService.selectUltimaEjecutadaAntesDe(idEmpresa, anio,
                mes);
        if (corrida == null || corrida.getFechaProceso() == null) {
            System.out.println("ContabilidadPrestamoService.agregaCierreAperturaCaminoDirecto - "
                    + descripcionOperacion + ": no se encontró la corrida de cierre de cartera que abrió " + mes
                    + "/" + anio + " en empresa " + idEmpresa + "; no se cierra la apertura (no se encontró qué"
                    + " apertura cerrar).");
            return;
        }
        // fechaProceso es el 01 del mes que esa corrida abrió; el corte de ESA apertura es su
        // último día — NUNCA corrida.getFechaCorte() (eso es el corte del CIERRE del mes
        // anterior, un mes antes de lo que corresponde).
        LocalDate fechaProceso = corrida.getFechaProceso();
        LocalDate fechaCorteApertura = fechaProceso.withDayOfMonth(fechaProceso.lengthOfMonth());

        double capitalFuturo = contabilizacionIndividualCreditoService.capitalFuturoPosteriorACorte(
                pagos, fechaCorteApertura, descripcionOperacion);
        double totalCierre = redondear(totalLiquidadoPrestamos - capitalFuturo);

        if (totalCierre <= 0.0) {
            System.out.println("ContabilidadPrestamoService.agregaCierreAperturaCaminoDirecto - "
                    + descripcionOperacion + ": el monto a cerrar contra la apertura da $" + totalCierre
                    + " (liquidado $" + redondear(totalLiquidadoPrestamos) + " - futuro $"
                    + redondear(capitalFuturo) + "); no se agregan líneas.");
            return;
        }

        Long idPlantillaApertura = plantillaService.codigoByAlterno(PlantillasCredito.APERTURA_PLANILLA_MENSUAL,
                idEmpresa);
        if (idPlantillaApertura == null) {
            throw new IncomeException("No existe la plantilla contable alterno "
                    + PlantillasCredito.APERTURA_PLANILLA_MENSUAL + " para la empresa " + idEmpresa
                    + "; no se puede cerrar la apertura de " + descripcionOperacion + ".");
        }
        DetallePlantilla lineaPorAplicar = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantillaApertura,
                CrdLineaAsiento.PRESTAMOS_POR_APLICAR);
        if (lineaPorAplicar == null || lineaPorAplicar.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno " + PlantillasCredito.APERTURA_PLANILLA_MENSUAL
                    + " no tiene la línea de préstamos por aplicar (aux1=" + CrdLineaAsiento.PRESTAMOS_POR_APLICAR
                    + "); no se puede cerrar la apertura de " + descripcionOperacion + ".");
        }
        DetallePlantilla lineaPorCobrar = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantillaApertura,
                CrdLineaAsiento.PRESTAMOS_POR_COBRAR);
        if (lineaPorCobrar == null || lineaPorCobrar.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno " + PlantillasCredito.APERTURA_PLANILLA_MENSUAL
                    + " no tiene la línea de préstamos por cobrar (aux1=" + CrdLineaAsiento.PRESTAMOS_POR_COBRAR
                    + "); no se puede cerrar la apertura de " + descripcionOperacion + ".");
        }

        // Se lee getMovimiento() de las dos y se invierte A PROPÓSITO: la plantilla las declara
        // así para ABRIR (por-aplicar en HABER, por-cobrar en DEBE); acá se está DESHACIENDO la
        // apertura, así que los dos van al revés. No es un error si lo de abajo da lo esperado
        // (aplicarEsDebeSegunPlantilla=false, cobrarEsDebeSegunPlantilla=true) — es la razón por
        // la que este método invierte en vez de leer tal cual.
        boolean aplicarEsDebeSegunPlantilla = lineaPorAplicar.getMovimiento() != null
                && lineaPorAplicar.getMovimiento().longValue() == MovimientoCuentaPlantilla.DEBE;
        boolean cobrarEsDebeSegunPlantilla = lineaPorCobrar.getMovimiento() != null
                && lineaPorCobrar.getMovimiento().longValue() == MovimientoCuentaPlantilla.DEBE;
        System.out.println("ContabilidadPrestamoService.agregaCierreAperturaCaminoDirecto - "
                + descripcionOperacion + ": cerrando apertura por $" + totalCierre + " (plantilla declara"
                + " por-aplicar " + (aplicarEsDebeSegunPlantilla ? "DEBE" : "HABER") + " y por-cobrar "
                + (cobrarEsDebeSegunPlantilla ? "DEBE" : "HABER") + "; acá van invertidas a propósito).");

        String descripcionLinea = "Cierre de apertura - vencido y cuota del mes - " + descripcionOperacion;

        DetalleAsiento debePorAplicar = new DetalleAsiento();
        debePorAplicar.setPlanCuenta(lineaPorAplicar.getPlanCuenta());
        debePorAplicar.setNumeroCuenta(lineaPorAplicar.getPlanCuenta().getCuentaContable());
        debePorAplicar.setNombreCuenta(lineaPorAplicar.getPlanCuenta().getNombre());
        debePorAplicar.setDescripcion(descripcionLinea);
        debePorAplicar.setValorDebe(totalCierre);
        debePorAplicar.setValorHaber(0.0);
        lineas.add(debePorAplicar);

        DetalleAsiento haberPorCobrar = new DetalleAsiento();
        haberPorCobrar.setPlanCuenta(lineaPorCobrar.getPlanCuenta());
        haberPorCobrar.setNumeroCuenta(lineaPorCobrar.getPlanCuenta().getCuentaContable());
        haberPorCobrar.setNombreCuenta(lineaPorCobrar.getPlanCuenta().getNombre());
        haberPorCobrar.setDescripcion(descripcionLinea);
        haberPorCobrar.setValorDebe(0.0);
        haberPorCobrar.setValorHaber(totalCierre);
        lineas.add(haberPorCobrar);
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

        // Cierre de apertura del camino DIRECTO (2026-09-05, §9 del diseño): esta llamada nace
        // porque ctx.getIdCobroCredito() es null (verificado en el guard de arriba) — o sea que
        // NADIE más cierra la apertura por esta precancelación. totalHaber es todo lo que el
        // evento liquidó del préstamo (capital por banda + interés + mora + seguros); el helper
        // le resta el capital futuro y agrega, si corresponde, las dos líneas invertidas al
        // MISMO asiento — nunca uno aparte.
        agregaCierreAperturaCaminoDirecto(lineas, idEmpresa, totalHaber, pagos, fechaCorte,
                "precancelación evento " + ctx.getIdEvento());

        String observacionBase = prefijo + (ctx.getObservacion() != null ? ": " + ctx.getObservacion() : "");
        String observacion = observacionConParticipeYPrestamos(evento.getPrestamo(), pagos, observacionBase);

        Asiento asiento = asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS, fechaCorte,
                observacion, ctx.getUsuario(), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        System.out.println("  ✅ contabilizarPrecancelacion OK - Asiento: " + asiento.getCodigo()
                + " - Evento: " + ctx.getIdEvento() + " - Monto: $" + totalDebe);

        return asiento.getCodigo();
    }

    /**
     * Agrega cédula, nombre del partícipe y los préstamos del evento a la observación de un
     * asiento — MISMO formato que {@code CobroCreditoServiceImpl#observacionEnriquecida}
     * (2026-09-04, pedido del usuario: los dos caminos tienen que leerse igual). No se llama a
     * ese método porque vive en otra clase y no se movió de archivo a propósito (otro equipo
     * está tocando esa zona de {@code CobroCreditoServiceImpl} en paralelo); si algún día ese
     * método se expone como utilidad compartida, este helper debería reemplazarse por esa
     * llamada en vez de mantenerse en paralelo.
     *
     * <p>Antes esta observación era solo {@code "Precancelación - evento N"}, sin identificar
     * a nadie.</p>
     *
     * @param prestamoEvento el préstamo del {@code EventoPrestamo} que se está contabilizando
     *                       (de ahí sale el partícipe); tolerante a {@code null}, igual que
     *                       {@code observacionEnriquecida} lo es a una entidad ausente.
     * @param pagos          los {@code PagoPrestamo} del evento — de ahí salen TODOS los
     *                       préstamos distintos a listar, con su código siempre y el
     *                       {@code idAsoprep} además cuando existe.
     */
    private String observacionConParticipeYPrestamos(Prestamo prestamoEvento, List<PagoPrestamo> pagos,
            String base) {
        StringBuilder obs = new StringBuilder(base);
        Entidad entidad = prestamoEvento != null ? prestamoEvento.getEntidad() : null;
        if (entidad != null) {
            obs.append(" | Cédula: ").append(entidad.getNumeroIdentificacion() != null
                    ? entidad.getNumeroIdentificacion() : "-");
            obs.append(" | Nombre: ").append(entidad.getRazonSocial() != null
                    ? entidad.getRazonSocial() : "-");
        }
        if (pagos != null) {
            List<Long> prestamosListados = new ArrayList<>();
            for (PagoPrestamo pago : pagos) {
                Prestamo prestamo = pago.getPrestamo();
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
        // Mismo tope defensivo que observacionEnriquecida: ASNTOBSR admite 2000 caracteres.
        if (resultado.length() > 2000) {
            resultado = resultado.substring(0, 1997) + "...";
        }
        return resultado;
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

    /** Acumulador de capital por banda dinámica al armar el asiento de entrega — una línea
     * por banda, no una por cuota (PLAN-ENTREGA-BANDAS-DINAMICAS.md §5, punto 4 de verificación). */
    private static class LineaBandaCapitalAcumulada {
        private final BandaProductoDetalle banda;
        private double valor;

        LineaBandaCapitalAcumulada(BandaProductoDetalle banda) {
            this.banda = banda;
        }
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

        // DEBE: capital clasificado con el MISMO modelo dinámico de bandas que ya usa el cobro
        // (ClasificadorBandaService / CRD.BNDP), no la escalera fija de 5 tramos de antes
        // (PLAN-ENTREGA-BANDAS-DINAMICAS.md). Es una entrega, no un cobro: TODAS las cuotas
        // están por vencer, y los días se cuentan desde la fecha de inicio del préstamo hasta
        // el vencimiento de cada cuota — por eso se le pasa fechaInicio como "fecha de corte"
        // a POR_VENCER, nunca VENCIDO.
        Map<Long, LineaBandaCapitalAcumulada> bandasEntrega = new LinkedHashMap<>();
        for (DetallePrestamo cuota : cuotas) {
            if (cuota.getFechaVencimiento() == null || cuota.getCapital() == null) {
                continue;
            }
            LocalDate vencimiento = cuota.getFechaVencimiento().toLocalDate();
            long dias = Math.max(1, ChronoUnit.DAYS.between(fechaInicio, vencimiento));
            ResultadoClasificacionBanda resultado = clasificadorBandaService.clasificar(
                    prestamo.getProducto().getCodigo(), idEmpresa,
                    Long.valueOf(TipoCarteraBanda.POR_VENCER), Long.valueOf(dias), fechaInicio);
            BandaProductoDetalle banda = resultado.getBanda();
            LineaBandaCapitalAcumulada acumulada = bandasEntrega.computeIfAbsent(
                    banda.getNumero(), k -> new LineaBandaCapitalAcumulada(banda));
            acumulada.valor += cuota.getCapital();
        }

        List<DetalleAsiento> lineas = new ArrayList<>();
        double totalCapital = 0.0;
        for (LineaBandaCapitalAcumulada acumulada : bandasEntrega.values()) {
            double valorBanda = redondear(acumulada.valor);
            if (valorBanda <= TOLERANCIA_CUADRE) {
                continue;
            }
            // Mismo guardarraíl que el cobro (ContabilizacionIndividualCreditoServiceImpl.
            // lineaBandaCapital): sin cuenta asignada en CRD.BNDP, la entrega no se contabiliza
            // (sql/176 verifica esto ANTES de desplegar — ver PLAN-ENTREGA-BANDAS-DINAMICAS.md §3).
            if (acumulada.banda.getIdPlanCuenta() == null) {
                throw new IncomeException("La banda " + acumulada.banda.getNumero() + " del producto "
                        + prestamo.getProducto().getCodigo() + " no tiene cuenta contable asignada en"
                        + " CRD.BNDP; no se puede armar el asiento de entrega del préstamo "
                        + prestamo.getCodigo() + ".");
            }
            PlanCuenta cuenta = planCuentaDaoService.selectById(acumulada.banda.getIdPlanCuenta(),
                    NombreEntidadesContabilidad.PLAN_CUENTA);
            DetalleAsiento linea = new DetalleAsiento();
            linea.setPlanCuenta(cuenta);
            linea.setNumeroCuenta(acumulada.banda.getCuentaContable());
            linea.setNombreCuenta(acumulada.banda.getNombreCuenta());
            linea.setDescripcion(prefijo + " - banda " + acumulada.banda.getNumero());
            linea.setValorDebe(valorBanda);
            linea.setValorHaber(0.0);
            lineas.add(linea);
            totalCapital += valorBanda;
        }
        totalCapital = redondear(totalCapital);

        if (Math.abs(redondear(totalCapital - montoOperacion)) > TOLERANCIA_CUADRE) {
            throw new IncomeException("El capital distribuido en bandas del préstamo "
                    + prestamo.getCodigo() + " ($" + totalCapital + ") no coincide con el monto"
                    + " de la operación ($" + montoOperacion + "). No se genera un asiento"
                    + " desbalanceado.");
        }

        // ⚠️ "El bien" (aux1=8 en 9/13, ausente en 34): esta línea depende de
        // Prestamo.valorAsegurado (PRSTVLAS), y esa columna NO TIENE ESCRITOR HOY —
        // verificado por el árbitro el 2026-09-01 (ESTADO-EQUIPO-SEGUROS.md §1.3: una de
        // las cuatro columnas de seguro de CRD.PRST mapeadas y presentes en la base que
        // nadie escribe). Mientras eso no cambie, valorBien siempre da 0 y esta línea
        // NUNCA se genera — un prendario o hipotecario queda sin registrar su garantía en
        // cuentas de orden, y el asiento cuadra igual (no avisa: es justamente el modo de
        // falla que este diseño evita en todo lo demás). No es un bug de este método: leer
        // valorAsegurado era lo razonable con el código de hoy. Pendiente de sql/157
        // bloque 6 antes del primer prendario/hipotecario real — no bloquea quirografario
        // (sin línea de bien).
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

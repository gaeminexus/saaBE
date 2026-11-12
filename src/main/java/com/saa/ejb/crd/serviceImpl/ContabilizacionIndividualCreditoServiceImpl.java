package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.ejb.cnt.dao.PlanCuentaDaoService;
import com.saa.ejb.crd.dao.HistDetallePrestamoDaoService;
import com.saa.ejb.crd.dao.TipoAporteDaoService;
import com.saa.ejb.crd.service.ClasificadorBandaService;
import com.saa.ejb.crd.service.ContabilizacionIndividualCreditoService;
import com.saa.ejb.crd.service.ProcesoPagoPrestamoService;
import com.saa.ejb.crd.service.dto.BandaProductoDetalle;
import com.saa.ejb.crd.service.dto.DesgloseAporte;
import com.saa.ejb.crd.service.dto.ResultadoClasificacionBanda;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.HistDetallePrestamo;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.Producto;
import com.saa.model.crd.TipoAporte;
import com.saa.rubros.CrdLineaAsiento;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.PlantillasCredito;
import com.saa.rubros.TipoCarteraBanda;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @see ContabilizacionIndividualCreditoService
 * @author Sistema SAA
 * @since 2026-08-30
 */
@Stateless
public class ContabilizacionIndividualCreditoServiceImpl implements ContabilizacionIndividualCreditoService {

    /** CRD.TPAP.TPAPCDGO de los tipos con cuenta en la plantilla 21 (aux1 50/51/52; 53, 54 y 55 más abajo).
     * Mismos valores que {@code CobroCreditoServiceImpl}/{@code CobroPetroContableServiceImpl}
     * — catálogo de negocio estable, no una resolución que pueda divergir. */
    private static final long TIPO_APORTE_JUBILACION = 9L;
    private static final long TIPO_APORTE_CESANTIA = 11L;
    private static final long TIPO_APORTE_ADICIONAL = 2L;
    /**
     * CRD.TPAP.TPAPCDGO de la pensión complementaria — mismo valor que
     * {@code PagoPensionComplementariaServiceImpl}/{@code AporteServiceImpl}. Agregado
     * 2026-09-05, con autorización expresa de {@code lap-saa-1-arb} (dueño de este archivo):
     * el cruce contra préstamo con este aporte (crd, equipo omen-saa-1) reventaba con
     * "El tipo de aporte 23 no tiene cuenta contable parametrizada" al llegar a
     * {@link #aux1ParaTipoAporte}. Cuenta contable {@code 2.1.02.25.01} — la cuenta INDIVIDUAL
     * del partícipe, distinta a propósito de la {@code 2.3.01.10.03} que se acredita al
     * jubilar (esa es el pasivo "por pagar"; el cruce no paga, da de baja el saldo individual).
     * Ver el JavaDoc completo de {@link CrdLineaAsiento#APORTES_PENSION_COMPLEMENTARIA}.
     */
    private static final long TIPO_APORTE_PENSION_COMPLEMENTARIA = 23L;
    /** CRD.TPAP.TPAPCDGO 22, JUBILACION RETIRO VOLUNTARIO. Pasivo {@code 2.1.02.25.05}, aux1 54 de la
     * plantilla 21 (decisión del usuario 2026-10-16, {@code sql/330}). Solo el cruce; el cobro/registro
     * de aportes 22 es otra decisión. */
    private static final long TIPO_APORTE_JUBILACION_RETIRO_VOLUNTARIO = 22L;
    /** CRD.TPAP.TPAPCDGO 21, CESANTIA RETIRO VOLUNTARIO. Pasivo {@code 2.1.01.20.05}, aux1 55 de la
     * plantilla 21 (decisión del usuario 2026-10-19, {@code sql/331}). */
    private static final long TIPO_APORTE_CESANTIA_RETIRO_VOLUNTARIO = 21L;
    /** CRD.TPAP.TPAPCDGO 13, JUBILACION PATRONAL. Pasivo {@code 2.1.02.10.01}, aux1 58 de la plantilla 21
     * ({@code sql/378}). Solo el cruce con préstamos: el registro/cobro de aportes 13 sigue rechazado en
     * {@code CobroCreditoServiceImpl#esTipoAporteContabilizable}. C29, 11-nov-2026. */
    private static final long TIPO_APORTE_JUBILACION_PATRONAL = 13L;
    /** CRD.TPAP.TPAPCDGO 14, CESANTIA PATRONAL. Pasivo {@code 2.1.01.10.01}, aux1 59 de la plantilla 21
     * ({@code sql/378}). Solo el cruce. C29, 11-nov-2026. */
    private static final long TIPO_APORTE_CESANTIA_PATRONAL = 14L;

    private static final double TOLERANCIA = 0.01;

    @EJB
    private PlantillaService plantillaService;

    @EJB
    private DetallePlantillaDaoService detallePlantillaDaoService;

    @EJB
    private PlanCuentaDaoService planCuentaDaoService;

    @EJB
    private TipoAporteDaoService tipoAporteDaoService;

    @EJB
    private ClasificadorBandaService clasificadorBandaService;

    @EJB
    private HistDetallePrestamoDaoService histDetallePrestamoDaoService;

    @EJB
    private com.saa.ejb.crd.dao.DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private com.saa.ejb.crd.service.MotorPagoPrestamoService motorPagoPrestamoService;

    /** C15 / D-017: separa la mora cobrada devengada (1.4.02) de la no devengada (ingreso). */
    @EJB
    private com.saa.ejb.crd.service.ProvisionInteresService provisionInteresService;

    /** D-024: saldo contable de las cuentas por cobrar del período (misma lectura que C14/C15). */
    @EJB
    private com.saa.ejb.crd.dao.CierreCarteraDaoService cierreCarteraDaoService;

    @Override
    public Long resolverPlantillaAplicacion(Long idEmpresa) throws Throwable {
        Long idPlantilla = plantillaService.codigoByAlterno(PlantillasCredito.APLICACION_PETRO, idEmpresa);
        if (idPlantilla == null) {
            throw new IncomeException("No existe la plantilla contable alterno "
                    + PlantillasCredito.APLICACION_PETRO + " para la empresa " + idEmpresa + ".");
        }
        return idPlantilla;
    }

    @Override
    public DetalleAsiento lineaInteres(Long idPlantillaAplicacion, Long idTipoPrestamo, double valor,
            boolean esMora, String prefijoDescripcion) throws Throwable {
        if (idTipoPrestamo == null) {
            throw new IncomeException(prefijoDescripcion + ": el préstamo no tiene tipo de préstamo"
                    + " asignado; no se puede resolver la cuenta de interés.");
        }
        int aux1 = esMora ? CrdLineaAsiento.INTERES_MORA_POR_COBRAR : CrdLineaAsiento.INTERES_ORDINARIO_POR_COBRAR;
        DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliares(
                idPlantillaAplicacion, aux1, idTipoPrestamo.intValue());
        if (linea == null || linea.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno " + PlantillasCredito.APLICACION_PETRO
                    + " no tiene la línea de interés " + (esMora ? "de mora" : "ordinario")
                    + " (aux1=" + aux1 + ") para el tipo de préstamo " + idTipoPrestamo + ".");
        }
        PlanCuenta cuenta = linea.getPlanCuenta();
        DetalleAsiento detalle = new DetalleAsiento();
        detalle.setPlanCuenta(cuenta);
        detalle.setNumeroCuenta(cuenta.getCuentaContable());
        detalle.setNombreCuenta(cuenta.getNombre());
        // D3: mora y ordinario COMPARTEN cuenta — la descripción es lo único que distingue.
        detalle.setDescripcion(prefijoDescripcion + " - interés " + (esMora ? "de mora" : "ordinario"));
        detalle.setValorDebe(0.0);
        detalle.setValorHaber(redondear(valor));
        return detalle;
    }

    @Override
    public DetalleAsiento lineaIngresoMora(Long idEmpresa, Long idTipoPrestamo, double valor,
            String prefijoDescripcion) throws Throwable {
        if (idTipoPrestamo == null) {
            throw new IncomeException(prefijoDescripcion + ": el préstamo no tiene tipo de préstamo asignado;"
                    + " no se puede resolver la cuenta de ingreso por mora.");
        }
        Long idPlantilla = plantillaService.codigoByAlterno(PlantillasCredito.DEVENGO_INTERESES, idEmpresa);
        DetallePlantilla linea = idPlantilla != null ? detallePlantillaDaoService.selectByPlantillaYAuxiliares(
                idPlantilla, CrdLineaAsiento.INGRESO_INTERES_MORA, idTipoPrestamo.intValue()) : null;
        if (linea == null || linea.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno " + PlantillasCredito.DEVENGO_INTERESES
                    + " no tiene la línea de ingreso por mora (aux1=" + CrdLineaAsiento.INGRESO_INTERES_MORA
                    + ") para el tipo de préstamo " + idTipoPrestamo + " (D-017: la mora no devengada va a ingreso).");
        }
        return lineaHaberDesdePlantilla(linea, valor, prefijoDescripcion + " - interés de mora no devengado (ingreso)");
    }

    @Override
    public DetalleAsiento lineaSeguroIncendio(Long idPlantillaAplicacion, Long idTipoPrestamo, double valor,
            String prefijoDescripcion) throws Throwable {
        int aux1;
        if (idTipoPrestamo != null && idTipoPrestamo == CrdLineaAsiento.TIPO_PRESTAMO_HIPOTECARIO) {
            aux1 = CrdLineaAsiento.SEGURO_INCENDIO_HIPOTECARIO;
        } else if (idTipoPrestamo != null && idTipoPrestamo == CrdLineaAsiento.TIPO_PRESTAMO_PRENDARIO) {
            aux1 = CrdLineaAsiento.SEGURO_INCENDIO_PRENDARIO;
        } else {
            throw new IncomeException(prefijoDescripcion + ": hay seguro de incendio pero el tipo de"
                    + " préstamo " + idTipoPrestamo + " no tiene cuenta de seguro de incendio definida"
                    + " (solo hipotecario y prendario).");
        }
        DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantillaAplicacion, aux1);
        if (linea == null || linea.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno " + PlantillasCredito.APLICACION_PETRO
                    + " no tiene la línea de seguro de incendio (aux1=" + aux1 + ").");
        }
        return lineaHaberDesdePlantilla(linea, valor, prefijoDescripcion + " - seguro de incendio");
    }

    @Override
    public DetalleAsiento lineaSeguroDesgravamen(Long idPlantillaAplicacion, double valor, String prefijoDescripcion)
            throws Throwable {
        DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantillaAplicacion,
                CrdLineaAsiento.SEGURO_DESGRAVAMEN);
        if (linea == null || linea.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno " + PlantillasCredito.APLICACION_PETRO
                    + " no tiene la línea de seguro de desgravamen (aux1="
                    + CrdLineaAsiento.SEGURO_DESGRAVAMEN + ").");
        }
        return lineaHaberDesdePlantilla(linea, valor, prefijoDescripcion + " - seguro de desgravamen");
    }

    @Override
    public DetalleAsiento lineaAporteRegistrado(Long idPlantillaAplicacion, Long idTipoAporte, double valor,
            String prefijoDescripcion) throws Throwable {
        DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantillaAplicacion,
                aux1ParaTipoAporte(idTipoAporte));
        if (linea == null || linea.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno " + PlantillasCredito.APLICACION_PETRO
                    + " no tiene la línea del tipo de aporte " + idTipoAporte + ".");
        }
        return lineaHaberDesdePlantilla(linea, valor, prefijoDescripcion + " - aporte registrado");
    }

    @Override
    public List<DetalleAsiento> lineasCruceAportesConsumidos(Long idPlantillaAplicacion,
            List<DesgloseAporte> aportes, String prefijoDescripcion) throws Throwable {
        List<DetalleAsiento> lineas = new ArrayList<>();
        if (aportes == null) {
            return lineas;
        }
        for (DesgloseAporte renglon : aportes) {
            DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantillaAplicacion,
                    aux1ParaTipoAporte(renglon.getIdTipoAporte()));
            if (linea == null || linea.getPlanCuenta() == null) {
                throw new IncomeException("La plantilla alterno " + PlantillasCredito.APLICACION_PETRO
                        + " no tiene la línea del tipo de aporte " + renglon.getIdTipoAporte()
                        + " para el cruce de valores.");
            }
            PlanCuenta cuenta = linea.getPlanCuenta();
            TipoAporte tipoAporte = tipoAporteDaoService.find(new TipoAporte(), renglon.getIdTipoAporte());
            DetalleAsiento detalle = new DetalleAsiento();
            detalle.setPlanCuenta(cuenta);
            detalle.setNumeroCuenta(cuenta.getCuentaContable());
            detalle.setNombreCuenta(cuenta.getNombre());
            // ⚠️ Al DEBE a propósito, aunque DetallePlantilla.movimiento diga HABER para esta
            // línea: esa plantilla la define para el aporte ENTRANDO (saldo sube). Acá el
            // sentido es el opuesto — se CONSUME saldo que el socio ya tenía (saldo baja) — y
            // forzarlo es la única forma de que la cuenta correcta quede del lado correcto.
            detalle.setDescripcion(prefijoDescripcion + " - consumo aporte "
                    + (tipoAporte != null ? tipoAporte.getNombre() : renglon.getIdTipoAporte()));
            detalle.setValorDebe(redondear(renglon.getValor()));
            detalle.setValorHaber(0.0);
            lineas.add(detalle);
        }
        return lineas;
    }

    @Override
    public DetalleAsiento lineaBandaCapital(Long idProducto, Long idEmpresa, double capital,
            LocalDate fechaVencimiento, LocalDate fechaCorte, String prefijoDescripcion) throws Throwable {
        long[] tipoCarteraYDias = tipoCarteraYDias(fechaVencimiento, fechaCorte);
        ResultadoClasificacionBanda resultado = clasificadorBandaService.clasificar(idProducto, idEmpresa,
                tipoCarteraYDias[0], tipoCarteraYDias[1], fechaCorte);
        BandaProductoDetalle banda = resultado.getBanda();
        if (banda.getIdPlanCuenta() == null) {
            throw new IncomeException(prefijoDescripcion + ": la banda " + banda.getNumero()
                    + " del producto " + idProducto + " no tiene cuenta contable asignada en CRD.BNDP.");
        }
        PlanCuenta cuenta = planCuentaDaoService.selectById(banda.getIdPlanCuenta(),
                NombreEntidadesContabilidad.PLAN_CUENTA);
        DetalleAsiento detalle = new DetalleAsiento();
        detalle.setPlanCuenta(cuenta);
        detalle.setNumeroCuenta(banda.getCuentaContable());
        detalle.setNombreCuenta(banda.getNombreCuenta());
        detalle.setDescripcion(prefijoDescripcion + " - banda " + banda.getNumero());
        detalle.setValorDebe(0.0);
        detalle.setValorHaber(redondear(capital));
        return detalle;
    }

    /**
     * Reparte el capital de un abono a capital entre las bandas de las cuotas que ese abono
     * REALMENTE canceló (2026-08-31, PLAN-CIERRE-CONTABLE-TOTAL, Fase 3 — re-bandeo del abono;
     * ver el javadoc de {@link ContabilizacionIndividualCreditoService#haberDesdePagos} para el
     * porqué: antes de este método, el 100% del abono se bandeaba contra la cuota ANCLA, que no
     * tiene relación con lo que el abono canceló).
     *
     * <p>Se reparte PROPORCIONAL al {@code capital} de cada historizada — misma idea que
     * {@code AcuerdoCondonacionServiceImpl.generarAsientoCondonacion}, pero SIN su regla de
     * "consumir desde la más vencida primero": esa regla existe para castigar primero la
     * cartera más provisionada (condonación = pérdida real), y un abono a capital solo puede
     * aplicarse con el préstamo AL DÍA ({@code AbonoCapitalPrestamoServiceImpl.calcular},
     * validación 3) — nunca hay una cuota vencida que castigar primero, así que no hay
     * antigüedad que ordenar y prorratear por tamaño es lo único con sentido. Aplica igual a
     * las dos modalidades (1 reduce plazo, 2 reduce cuota): las dos historizan EXACTAMENTE las
     * mismas cuotas pendientes en el paso 2 de {@code aplicar}, antes de que la modalidad
     * decida cómo se ve la tabla nueva — este método nunca ve esa diferencia.</p>
     *
     * <p>La última historizada (por número de cuota) absorbe el residuo del redondeo, mismo
     * patrón que {@code construirTablaProyectada}. Si las historizadas no tienen capital
     * (caso atípico, capital total $0) se reparte en partes iguales en vez de fallar.</p>
     *
     * @param historizadas TODAS las {@code HistDetallePrestamo} del evento, sin filtrar — un
     *                      evento es una sola operación por diseño de {@code EventoPrestamo},
     *                      así que no hace falta distinguir de cuál abono es cada una
     */
    private List<DetalleAsiento> lineasBandaCapitalAbono(Long idProducto, Long idEmpresa, double capital,
            List<HistDetallePrestamo> historizadas, LocalDate fechaCorte, String prefijoDescripcion)
            throws Throwable {

        List<HistDetallePrestamo> ordenadas = ordenarPorNumeroCuota(historizadas);
        List<Double> partes = repartirProporcionalPorCapital(ordenadas, capital, prefijoDescripcion);

        List<EntradaBanda> entradas = new ArrayList<>();
        for (int idx = 0; idx < ordenadas.size(); idx++) {
            HistDetallePrestamo h = ordenadas.get(idx);
            LocalDate vencimiento = h.getFechaVencimiento() != null ? h.getFechaVencimiento().toLocalDate() : null;
            entradas.add(new EntradaBanda(vencimiento, partes.get(idx),
                    "la cuota historizada " + h.getCodigo() + " (#" + h.getNumeroCuota() + ")"));
        }
        Map<String, LineaBandaAcumulada> bandas = acumulaPorBanda(idProducto, idEmpresa, fechaCorte, entradas,
                prefijoDescripcion);
        return lineasHaberDesdeBandas(bandas, idProducto, prefijoDescripcion);
    }

    /**
     * Arma las líneas de asiento (H) de un mapa de bandas ya acumulado — extraído de {@link
     * #lineasBandaCapitalAbono} (2026-09-01) para que {@link #lineasBandaCapitalFuturoPrecancelacion}
     * use la MISMA construcción en vez de una copia.
     */
    private List<DetalleAsiento> lineasHaberDesdeBandas(Map<String, LineaBandaAcumulada> bandas, Long idProducto,
            String prefijoDescripcion) throws Throwable {
        List<DetalleAsiento> lineas = new ArrayList<>();
        for (LineaBandaAcumulada acumulada : bandas.values()) {
            if (acumulada.valor <= 0.0) {
                continue;
            }
            if (acumulada.banda.getIdPlanCuenta() == null) {
                throw new IncomeException(prefijoDescripcion + ": la banda " + acumulada.banda.getNumero()
                        + " del producto " + idProducto + " no tiene cuenta contable asignada en"
                        + " CRD.BNDP.");
            }
            PlanCuenta cuenta = planCuentaDaoService.selectById(acumulada.banda.getIdPlanCuenta(),
                    NombreEntidadesContabilidad.PLAN_CUENTA);
            DetalleAsiento detalle = new DetalleAsiento();
            detalle.setPlanCuenta(cuenta);
            detalle.setNumeroCuenta(acumulada.banda.getCuentaContable());
            detalle.setNombreCuenta(acumulada.banda.getNombreCuenta());
            detalle.setDescripcion(prefijoDescripcion + " - banda " + acumulada.banda.getNumero());
            detalle.setValorDebe(0.0);
            detalle.setValorHaber(acumulada.valor);
            lineas.add(detalle);
        }
        return lineas;
    }

    /**
     * Reclasifica entre bandas el capital futuro de una precancelación — la trampa gemela del
     * abono (2026-09-01, pedido del usuario), pero SIN prorrateo: una precancelación cancela
     * SIEMPRE la totalidad de las cuotas futuras (verificado en {@code
     * ProcesoPagoPrestamoServiceImpl.precancelar}: exige que el valor recibido cuadre exacto
     * contra la simulación completa ANTES de tocar nada, nunca deja una cuota a medias), así
     * que cada cuota banda exactamente su propio {@code capital} — no hay nada que repartir
     * proporcionalmente.
     *
     * <p><b>De dónde salen las cuotas:</b> precancelación NO historiza a {@code CRD.HDTP} (a
     * diferencia del abono) — las cuotas futuras se quedan vivas en {@code CRD.DTPR} con
     * {@code estado = CANCELADA_ANTICIPADA(7)}. No hace falta ubicarlas por evento: un préstamo
     * precancelado pasa a {@code CANCELADO_ANTICIPADO}, estado TERMINAL que bloquea una segunda
     * precancelación, y si la operación se revierte las cuotas en 7 se recalculan y salen de
     * ese estado antes de que el préstamo pueda volver a operarse — así que "todas las cuotas
     * en estado 7 de este préstamo" identifica sin ambigüedad las de la precancelación vigente.
     *
     * <p><b>Cuadre explícito</b>: la suma del capital PENDIENTE de esas cuotas tiene que coincidir
     * con {@code capitalFuturo} (el {@code saldoOtros} del pago) — si no, {@code IncomeException}
     * con los cuatro números (préstamo, cuántas cuotas encontró, cuánto suman, contra qué
     * capital futuro no cuadra), nunca un asiento armado a medias.
     *
     * <p>⛔ <b>Corrección 2026-09-30 (producción, préstamo 7912, cobro 149):</b> este método
     * sumaba el capital BRUTO de cada cuota ({@code cuota.getCapital()}), pero
     * {@code ProcesoPagoPrestamoServiceImpl.calcularPrecancelacion} arma {@code capitalFuturo}
     * como Σ capital PENDIENTE (saldo = capital − Σ capitalPagado vigente de PGPR). Si una cuota
     * futura ya tenía capital pagado (un PARCIAL, por ejemplo), el bruto queda por encima del
     * pendiente y el control de cuadre falla con un sobrante que no existe — medido: cuota 18 del
     * 7912, capital 52,03, pagado 1,76, pendiente 50,27; el bruto sumaba 372,54 contra un capital
     * futuro real de 370,78. Ahora cada cuota banda por su PENDIENTE
     * ({@code MotorPagoPrestamoService.calcularSaldosCuota}), la misma fuente que usó
     * {@code calcularPrecancelacion} para construir {@code capitalFuturo}.</p>
     *
     * <p>⚓ <b>El universo no son solo las cuotas en estado 7.</b> Cuando el préstamo no tenía
     * ninguna cuota pagada, {@code precancelar} elige como "ancla" (la que carga el
     * {@code saldoOtros} del capital futuro) una de las propias cuotas futuras, y esa ancla
     * queda en PAGADA(4), no en CANCELADA_ANTICIPADA(7) — ver {@code precancelar}, variable
     * {@code anclaEsFutura}. Si no se la agrega, su capital pendiente desaparece del reparto por
     * banda. Se detecta sin ambigüedad porque {@code ancla} ES el
     * {@code DetallePrestamo} del propio {@code pago} de capital futuro (el llamador lo tiene a
     * mano, nunca hace falta re-adivinarlo): si su capital pendiente es mayor que la tolerancia,
     * es porque nada más lo pagó todavía — era una futura — y entra al reparto por su propio
     * vencimiento. Si ya estaba en 0 (el caso normal: una cuota pagada hace tiempo), no se sostiene.</p>
     */
    /**
     * C16 (D-021) — ¿el capital futuro de una precancelación está FUERA de la tabla? Caso de la
     * cartera migrada de Delta 21: la tabla de algunos préstamos termina en la cuota del mes de la
     * cancelación y el saldo de capital posterior vive solo en {@code DTPRSLOT} de esa cuota (no hay
     * ninguna cuota CANCELADA_ANTICIPADA que lo represente). Se reconoce, y solo entonces, cuando
     * la ancla ya no tiene capital pendiente propio (la pagó el mismo evento) y su
     * {@code DTPRSLOT} cubre el capital futuro. Antes este caso lanzaba en el reparto por banda; en
     * el caso normal (hay cuotas en 7) no se llama.
     */
    private boolean esCapitalFuturoFueraDeTabla(double capitalFuturo, DetallePrestamo ancla) throws Throwable {
        if (ancla == null || ancla.getFechaVencimiento() == null || capitalFuturo <= TOLERANCIA) {
            return false;
        }
        if (nvl(ancla.getSaldoOtros()) + TOLERANCIA < redondear(capitalFuturo)) {
            return false;
        }
        double capitalPendienteAncla = redondear(motorPagoPrestamoService.calcularSaldosCuota(ancla).getSaldoCapital());
        return capitalPendienteAncla <= TOLERANCIA;
    }

    private List<DetalleAsiento> lineasBandaCapitalFuturoPrecancelacion(Long idProducto, Long idEmpresa,
            double capitalFuturo, Prestamo prestamo, DetallePrestamo ancla, LocalDate fechaCorte,
            String prefijoDescripcion) throws Throwable {

        List<DetallePrestamo> todas = detallePrestamoDaoService.selectByPrestamo(prestamo.getCodigo());
        List<DetallePrestamo> canceladasAnticipadamente = new ArrayList<>();
        if (todas != null) {
            for (DetallePrestamo cuota : todas) {
                if (cuota.getEstado() != null && cuota.getEstado() == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
                    canceladasAnticipadamente.add(cuota);
                }
            }
        }
        if (canceladasAnticipadamente.isEmpty() && esCapitalFuturoFueraDeTabla(capitalFuturo, ancla)) {
            // C16 (D-021): tabla migrada de Delta truncada en la cuota del mes — el capital futuro
            // no tiene cuotas en CRD.DTPR, vive solo en DTPRSLOT de la ancla. Se banda como si
            // venciera el mes siguiente a la ancla (la primera cuota que faltaría en la tabla).
            LocalDate vencimientoSiguiente = ancla.getFechaVencimiento().toLocalDate().plusMonths(1);
            vencimientoSiguiente = vencimientoSiguiente.withDayOfMonth(vencimientoSiguiente.lengthOfMonth());
            List<EntradaBanda> entradasFueraDeTabla = new ArrayList<>();
            entradasFueraDeTabla.add(new EntradaBanda(vencimientoSiguiente, redondear(capitalFuturo),
                    "el capital futuro fuera de la tabla (DTPRSLOT de la cuota #" + ancla.getNumeroCuota() + ")"));
            Map<String, LineaBandaAcumulada> bandasFueraDeTabla = acumulaPorBanda(idProducto, idEmpresa, fechaCorte,
                    entradasFueraDeTabla, prefijoDescripcion);
            return lineasHaberDesdeBandas(bandasFueraDeTabla, idProducto, prefijoDescripcion);
        }
        if (canceladasAnticipadamente.isEmpty()) {
            throw new IncomeException(prefijoDescripcion + ": el préstamo " + prestamo.getCodigo()
                    + " no tiene ninguna cuota en estado CANCELADA_ANTICIPADA(7); no se puede"
                    + " repartir el capital futuro ($" + redondear(capitalFuturo) + ") por banda."
                    + " ProcesoPagoPrestamoServiceImpl.precancelar siempre deja al menos una — revise"
                    + " si esto corrió ANTES del paso 3 de precancelar (que las pone en 7) o si el"
                    + " evento es realmente de tipo PRECANCELACION.");
        }

        List<EntradaBanda> entradas = new ArrayList<>();
        double totalCuotas = 0.0;
        for (DetallePrestamo cuota : canceladasAnticipadamente) {
            double capitalPendienteCuota = redondear(motorPagoPrestamoService.calcularSaldosCuota(cuota).getSaldoCapital());
            totalCuotas += capitalPendienteCuota;
            LocalDate vencimiento = cuota.getFechaVencimiento() != null
                    ? cuota.getFechaVencimiento().toLocalDate() : null;
            entradas.add(new EntradaBanda(vencimiento, capitalPendienteCuota,
                    "la cuota #" + cuota.getNumeroCuota() + " (cancelada anticipadamente)"));
        }

        // La ancla "futura" (ver javadoc): su capital pendiente no quedó en ninguna cuota 7.
        if (ancla != null && ancla.getEstado() != null
                && ancla.getEstado() != EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
            double capitalPendienteAncla = redondear(motorPagoPrestamoService.calcularSaldosCuota(ancla).getSaldoCapital());
            if (capitalPendienteAncla > TOLERANCIA) {
                totalCuotas += capitalPendienteAncla;
                LocalDate vencimientoAncla = ancla.getFechaVencimiento() != null
                        ? ancla.getFechaVencimiento().toLocalDate() : null;
                entradas.add(new EntradaBanda(vencimientoAncla, capitalPendienteAncla,
                        "la cuota #" + ancla.getNumeroCuota() + " (ancla del capital futuro, sin pago previo)"));
            }
        }
        totalCuotas = redondear(totalCuotas);

        if (Math.abs(redondear(totalCuotas - capitalFuturo)) > TOLERANCIA) {
            throw new IncomeException(prefijoDescripcion + ": el préstamo " + prestamo.getCodigo()
                    + " tiene " + entradas.size() + " cuota(s) futura(s) cuyo capital PENDIENTE suma $"
                    + totalCuotas + ", pero no coincide con el capital futuro del pago ($"
                    + redondear(capitalFuturo) + "); no se puede armar el reparto por banda de la"
                    + " precancelación.");
        }

        Map<String, LineaBandaAcumulada> bandas = acumulaPorBanda(idProducto, idEmpresa, fechaCorte, entradas,
                prefijoDescripcion);
        return lineasHaberDesdeBandas(bandas, idProducto, prefijoDescripcion);
    }

    /**
     * Reclasifica entre bandas el capital que un abono a capital NO tocó — el saldo que sigue
     * vivo tras la re-amortización, no la plata del abono (esa la banda {@link
     * #lineasBandaCapitalAbono}, asiento aparte). 2026-08-31, PLAN-CIERRE-CONTABLE-TOTAL, Fase
     * 3 — decisión del usuario, §9.1 C2 del levantamiento: "diferencias netas por banda, como
     * el cambio de bandas mensual" — mismo patrón que {@code
     * CierreCarteraServiceImpl.armaCambioBandas}, aplicado acá a UN préstamo en vez de a toda
     * la cartera.
     *
     * <p><b>Por qué NO alcanza con comparar {@code historizadas} contra {@code nuevas} directo
     * (la trampa central de este asiento):</b> el capital de {@code historizadas} incluye lo
     * que el abono canceló, y eso ya lo sacó {@link #lineasBandaCapitalAbono} de esas mismas
     * bandas. Restar {@code nuevas - historizadas} tal cual contaría el abono DOS VECES (una
     * vez implícito acá, otra vez explícito en el asiento del abono). Por eso el "lado viejo"
     * de esta comparación NO es {@code historizadas} — es el REMANENTE de cada historizada
     * DESPUÉS de que el abono se llevó su parte: {@code remanente = capital - parte}, donde
     * {@code parte} sale de la MISMA función de reparto que usa {@link
     * #lineasBandaCapitalAbono} ({@link #repartirProporcionalPorCapital}) — nunca una segunda
     * cuenta que pueda desalinearse de la primera.</p>
     *
     * <p><b>Invariante de control (2026-08-31, pedido explícito):</b> por construcción,
     * {@code Σremanente == Σnuevas.capital} (las dos son "el capital pendiente después del
     * abono", solo que agrupado por la banda vieja vs. la banda nueva) — se verifica antes de
     * armar las líneas, y además se verifica que el asiento resultante sume Debe == Haber. Si
     * cualquiera de las dos no cuadra, no se genera nada: este asiento es puro movimiento
     * interno de bandas del mismo préstamo, nunca puede entrar ni salir plata de la cartera.</p>
     *
     * @param historizadas Las mismas {@code HistDetallePrestamo} del evento que usó el asiento
     *                     del abono — el lado viejo, antes de ajustar por lo que el abono
     *                     canceló.
     * @param capitalAbono El mismo monto del abono que se le pasó a
     *                     {@link #lineasBandaCapitalAbono} — se usa para calcular el
     *                     remanente, nunca para armar una línea de asiento acá.
     * @param nuevas       Las cuotas vivas que quedaron tras rehacer la tabla de amortización
     *                     (numeroCuota &gt;= la primera cuota que el abono generó).
     */
    @Override
    public List<DetalleAsiento> lineasReclasificacionAbonoCapital(Long idProducto, Long idEmpresa,
            List<HistDetallePrestamo> historizadas, double capitalAbono, List<DetallePrestamo> nuevas,
            LocalDate fechaCorte, String prefijoDescripcion) throws Throwable {

        List<HistDetallePrestamo> ordenadas = ordenarPorNumeroCuota(historizadas);
        List<Double> partes = repartirProporcionalPorCapital(ordenadas, capitalAbono, prefijoDescripcion);

        List<EntradaBanda> entradasViejas = new ArrayList<>();
        double totalRemanente = 0.0;
        for (int idx = 0; idx < ordenadas.size(); idx++) {
            HistDetallePrestamo h = ordenadas.get(idx);
            double remanente = redondear(nvl(h.getCapital()) - partes.get(idx));
            totalRemanente += remanente;
            LocalDate vencimiento = h.getFechaVencimiento() != null ? h.getFechaVencimiento().toLocalDate() : null;
            entradasViejas.add(new EntradaBanda(vencimiento, remanente,
                    "la cuota historizada " + h.getCodigo() + " (#" + h.getNumeroCuota() + ")"));
        }
        totalRemanente = redondear(totalRemanente);

        List<EntradaBanda> entradasNuevas = new ArrayList<>();
        double totalNuevas = 0.0;
        for (DetallePrestamo cuota : nuevas) {
            double capitalCuota = redondear(nvl(cuota.getCapital()));
            totalNuevas += capitalCuota;
            LocalDate vencimiento = cuota.getFechaVencimiento() != null
                    ? cuota.getFechaVencimiento().toLocalDate() : null;
            entradasNuevas.add(new EntradaBanda(vencimiento, capitalCuota,
                    "la cuota nueva " + cuota.getCodigo() + " (#" + cuota.getNumeroCuota() + ")"));
        }
        totalNuevas = redondear(totalNuevas);

        if (Math.abs(redondear(totalRemanente - totalNuevas)) > TOLERANCIA) {
            throw new IncomeException(prefijoDescripcion + ": el remanente de las cuotas historizadas"
                    + " ($" + totalRemanente + ") no coincide con el capital de las cuotas nuevas ($"
                    + totalNuevas + "); no se puede armar el asiento de re-bandeo. Revise que"
                    + " 'nuevas' sean exactamente las cuotas que generó este abono.");
        }

        Map<String, LineaBandaAcumulada> bandasViejas = acumulaPorBanda(idProducto, idEmpresa, fechaCorte,
                entradasViejas, prefijoDescripcion);
        Map<String, LineaBandaAcumulada> bandasNuevas = acumulaPorBanda(idProducto, idEmpresa, fechaCorte,
                entradasNuevas, prefijoDescripcion);

        Map<String, BandaProductoDetalle> universo = new LinkedHashMap<>();
        for (Map.Entry<String, LineaBandaAcumulada> e : bandasViejas.entrySet()) {
            universo.put(e.getKey(), e.getValue().banda);
        }
        for (Map.Entry<String, LineaBandaAcumulada> e : bandasNuevas.entrySet()) {
            universo.put(e.getKey(), e.getValue().banda);
        }

        List<DetalleAsiento> lineas = new ArrayList<>();
        double totalDebe = 0.0;
        double totalHaber = 0.0;
        for (Map.Entry<String, BandaProductoDetalle> e : universo.entrySet()) {
            BandaProductoDetalle banda = e.getValue();
            double vieja = bandasViejas.containsKey(e.getKey()) ? bandasViejas.get(e.getKey()).valor : 0.0;
            double nueva = bandasNuevas.containsKey(e.getKey()) ? bandasNuevas.get(e.getKey()).valor : 0.0;
            double diferencia = redondear(nueva - vieja);
            if (Math.abs(diferencia) < 0.01) {
                continue;
            }
            if (banda.getIdPlanCuenta() == null) {
                throw new IncomeException(prefijoDescripcion + ": la banda " + banda.getNumero()
                        + " del producto " + idProducto + " no tiene cuenta contable asignada en CRD.BNDP.");
            }
            PlanCuenta cuenta = planCuentaDaoService.selectById(banda.getIdPlanCuenta(),
                    NombreEntidadesContabilidad.PLAN_CUENTA);
            DetalleAsiento detalle = new DetalleAsiento();
            detalle.setPlanCuenta(cuenta);
            detalle.setNumeroCuenta(banda.getCuentaContable());
            detalle.setNombreCuenta(banda.getNombreCuenta());
            detalle.setDescripcion(prefijoDescripcion + " - re-bandeo - banda " + banda.getNumero());
            // La banda crece -> Debe; decrece -> Haber (mismo criterio que armaCambioBandas).
            detalle.setValorDebe(diferencia > 0 ? diferencia : 0.0);
            detalle.setValorHaber(diferencia < 0 ? -diferencia : 0.0);
            lineas.add(detalle);
            totalDebe += nvl(detalle.getValorDebe());
            totalHaber += nvl(detalle.getValorHaber());
        }

        if (Math.abs(redondear(totalDebe - totalHaber)) > TOLERANCIA) {
            throw new IncomeException(prefijoDescripcion + ": el asiento de re-bandeo no cuadra: Debe $"
                    + redondear(totalDebe) + " vs Haber $" + redondear(totalHaber) + ". No se genera un"
                    + " asiento desbalanceado.");
        }
        return lineas;
    }

    /** Historizadas ordenadas por número de cuota — mismo orden que decide cuál absorbe el
     * residuo del redondeo en {@link #repartirProporcionalPorCapital}; compartido para que
     * {@link #lineasBandaCapitalAbono} y {@link #lineasReclasificacionAbonoCapital} usen
     * SIEMPRE el mismo orden y no puedan repartir el residuo a historizadas distintas. */
    private List<HistDetallePrestamo> ordenarPorNumeroCuota(List<HistDetallePrestamo> historizadas) {
        List<HistDetallePrestamo> ordenadas = new ArrayList<>(historizadas);
        ordenadas.sort((a, b) -> {
            if (a.getNumeroCuota() == null) {
                return -1;
            }
            if (b.getNumeroCuota() == null) {
                return 1;
            }
            return Double.compare(a.getNumeroCuota(), b.getNumeroCuota());
        });
        return ordenadas;
    }

    /**
     * Reparte {@code total} proporcional al {@code capital} de cada historizada de
     * {@code ordenadas} — la ÚNICA función de reparto de un abono a capital (2026-08-31):
     * la usa {@link #lineasBandaCapitalAbono} para saber cuánto de cada cuota canceló el abono,
     * y {@link #lineasReclasificacionAbonoCapital} para saber cuánto le queda de remanente
     * ({@code capital - parte}) — la MISMA función en los dos lugares, para que "lo que el
     * abono canceló" y "lo que le queda a cada cuota" nunca puedan desalinearse entre los dos
     * asientos. La última historizada (por número de cuota, ver {@link
     * #ordenarPorNumeroCuota}) absorbe el residuo del redondeo, mismo patrón que
     * {@code construirTablaProyectada}. Si las historizadas no tienen capital (caso atípico,
     * capital total $0) se reparte en partes iguales en vez de fallar.
     *
     * @return Una parte por historizada, EN EL MISMO ORDEN que {@code ordenadas} — el llamador
     *         es responsable de emparejarlas por índice.
     */
    private List<Double> repartirProporcionalPorCapital(List<HistDetallePrestamo> ordenadas, double total,
            String prefijoDescripcion) throws Throwable {

        double totalCapitalHistorizadas = 0.0;
        for (HistDetallePrestamo h : ordenadas) {
            totalCapitalHistorizadas += nvl(h.getCapital());
        }
        boolean prorateoPorCapital = totalCapitalHistorizadas > 0.0;
        if (!prorateoPorCapital) {
            System.out.println("ContabilizacionIndividualCreditoServiceImpl.repartirProporcionalPorCapital: "
                    + prefijoDescripcion + " - las " + ordenadas.size() + " cuotas historizadas"
                    + " tienen capital $0 (caso atípico); se reparte en partes iguales en vez de"
                    + " proporcional.");
        }

        List<Double> partes = new ArrayList<>();
        double restante = redondear(total);
        for (int idx = 0; idx < ordenadas.size(); idx++) {
            HistDetallePrestamo h = ordenadas.get(idx);
            double parte;
            if (idx == ordenadas.size() - 1) {
                parte = restante; // la última absorbe el residuo del redondeo
            } else if (prorateoPorCapital) {
                parte = redondear(total * nvl(h.getCapital()) / totalCapitalHistorizadas);
            } else {
                parte = redondear(total / ordenadas.size());
            }
            parte = Math.min(parte, restante);
            restante = redondear(restante - parte);
            partes.add(parte);
        }
        if (restante > TOLERANCIA) {
            // No debería poder pasar (parte siempre <= restante), pero mejor fallar fuerte que
            // dejar un residuo sin repartir en silencio.
            throw new IncomeException(prefijoDescripcion + ": no se pudo repartir $" + restante
                    + " entre las cuotas historizadas; revise CRD.HDTP del evento.");
        }
        return partes;
    }

    /**
     * Clasifica por banda una lista de importes con su propia fecha de vencimiento, y los
     * acumula cuando caen en la misma banda (ej. dos cuotas consecutivas por vencer dentro del
     * mismo rango de días) — se suman en una sola línea de asiento en vez de una por cuota.
     * Compartido entre {@link #lineasBandaCapitalAbono} y
     * {@link #lineasReclasificacionAbonoCapital} (2026-08-31).
     *
     * <p>Valida la fecha de vencimiento de TODAS las entradas, incluso las de importe $0 —
     * mismo criterio que tenía {@link #lineasBandaCapitalAbono} antes de esta extracción: mejor
     * fallar por un dato faltante que dejarlo pasar sin clasificar.</p>
     */
    private Map<String, LineaBandaAcumulada> acumulaPorBanda(Long idProducto, Long idEmpresa, LocalDate fechaCorte,
            List<EntradaBanda> entradas, String prefijoDescripcion) throws Throwable {

        Map<String, LineaBandaAcumulada> bandas = new LinkedHashMap<>();
        for (EntradaBanda entrada : entradas) {
            if (entrada.fechaVencimiento == null) {
                throw new IncomeException(prefijoDescripcion + ": " + entrada.descripcion
                        + " no tiene fecha de vencimiento; no se puede clasificar por banda.");
            }
            if (entrada.valor <= 0.0) {
                continue;
            }
            long[] tipoCarteraYDias = tipoCarteraYDias(entrada.fechaVencimiento, fechaCorte);
            ResultadoClasificacionBanda resultado = clasificadorBandaService.clasificar(idProducto, idEmpresa,
                    tipoCarteraYDias[0], tipoCarteraYDias[1], fechaCorte);
            BandaProductoDetalle banda = resultado.getBanda();
            String clave = idProducto + "|" + banda.getNumero();
            LineaBandaAcumulada acumulada = bandas.get(clave);
            if (acumulada == null) {
                acumulada = new LineaBandaAcumulada(banda);
                bandas.put(clave, acumulada);
            }
            acumulada.valor = redondear(acumulada.valor + entrada.valor);
        }
        return bandas;
    }

    /** Un importe con su propia fecha de vencimiento, listo para clasificar por banda — ver
     * {@link #acumulaPorBanda}. {@code descripcion} es solo para mensajes de error. */
    private static class EntradaBanda {
        private final LocalDate fechaVencimiento;
        private final double valor;
        private final String descripcion;

        private EntradaBanda(LocalDate fechaVencimiento, double valor, String descripcion) {
            this.fechaVencimiento = fechaVencimiento;
            this.valor = valor;
            this.descripcion = descripcion;
        }
    }

    /** Acumulador de {@link #acumulaPorBanda}: varias entradas pueden caer en la misma banda
     * — se suman en una sola línea de asiento en vez de una por cuota. */
    private static class LineaBandaAcumulada {
        private final BandaProductoDetalle banda;
        private double valor;

        private LineaBandaAcumulada(BandaProductoDetalle banda) {
            this.banda = banda;
        }
    }

    @Override
    public List<DetalleAsiento> haberDesdePagos(List<PagoPrestamo> pagos, Long idEmpresa,
            Long idPlantillaAplicacion, LocalDate fechaCorte, String prefijoDescripcion, String usuario) throws Throwable {
        List<DetalleAsiento> lineas = new ArrayList<>();
        if (pagos == null) {
            return lineas;
        }

        double totalInteres = 0.0;
        double totalMora = 0.0;
        double totalSeguroDesgravamen = 0.0;
        double totalSeguroIncendio = 0.0;
        Long idTipoPrestamo = null;

        for (PagoPrestamo pago : pagos) {
            if (pago.getAnulado() != null && pago.getAnulado() == 1L) {
                continue;
            }
            DetallePrestamo cuota = pago.getDetallePrestamo();
            Prestamo prestamo = pago.getPrestamo();
            Producto producto = prestamo != null ? prestamo.getProducto() : null;
            if (idTipoPrestamo == null && producto != null && producto.getTipoPrestamo() != null) {
                idTipoPrestamo = producto.getTipoPrestamo().getCodigo();
            }

            // ⚠️ TRAMPAS 2.1/2.2 de la especificación CBCRASN2: el abono a capital y el capital
            // futuro de una precancelación graban en saldoOtros con capitalPagado = 0 — leer
            // solo capitalPagado los contabilizaría en $0, sin ningún error.
            double saldoOtros = nvl(pago.getSaldoOtros());
            double capital = saldoOtros > 0.0 ? saldoOtros : nvl(pago.getCapitalPagado());
            if (capital > 0.0) {
                if (producto == null) {
                    throw new IncomeException(prefijoDescripcion + ": el pago " + pago.getCodigo()
                            + " tiene capital pero no tiene producto; no se puede clasificar por banda.");
                }
                // Las dos ramas de re-bandeo exigen ADEMÁS saldoOtros > 0 (no alcanza con el
                // tipo): las cuotas EXIGIBLES de una precancelación comparten
                // TIPO_PRECANCELACION con el pago de capital futuro, pero pagan con
                // capitalPagado (saldoOtros = 0) — sin este chequeo, un exigible caería acá y
                // se rompería lo único que ya bandeaba bien (2026-09-01).
                if (saldoOtros > 0.0 && ProcesoPagoPrestamoService.TIPO_ABONO_CAPITAL.equals(pago.getTipo())) {
                    // Re-bandeo del abono a capital (2026-08-31, Fase 3).
                    if (pago.getEventoPrestamo() == null) {
                        throw new IncomeException(prefijoDescripcion + ": el abono " + pago.getCodigo()
                                + " no tiene EventoPrestamo asociado; no se puede ubicar en"
                                + " CRD.HDTP qué cuotas canceló.");
                    }
                    List<HistDetallePrestamo> historizadas =
                            histDetallePrestamoDaoService.selectByEvento(pago.getEventoPrestamo().getCodigo());
                    if ((historizadas == null || historizadas.isEmpty()) && esAbonoSinRegenerarTabla(pago)) {
                        // C16b (D-030): abono sobre una tabla que ya lo refleja — no hay historizadas;
                        // el capital se reparte entre las cuotas que vencen después del abono.
                        historizadas = cuotasFuturasAbonoSinTabla(pago, prefijoDescripcion);
                    }
                    if (historizadas == null || historizadas.isEmpty()) {
                        throw new IncomeException(prefijoDescripcion + ": el abono " + pago.getCodigo()
                                + " (evento " + pago.getEventoPrestamo().getCodigo() + ") no tiene"
                                + " ninguna cuota historizada en CRD.HDTP; no se puede repartir el"
                                + " capital por banda. AbonoCapitalPrestamoServiceImpl.aplicar"
                                + " siempre historiza al menos una cuota — revise si el evento es"
                                + " realmente de tipo ABONO_CAPITAL.");
                    }
                    lineas.addAll(lineasBandaCapitalAbono(producto.getCodigo(), idEmpresa, capital,
                            historizadas, fechaCorte, prefijoDescripcion));
                } else if (saldoOtros > 0.0 && ProcesoPagoPrestamoService.TIPO_PRECANCELACION.equals(pago.getTipo())) {
                    // Re-bandeo del capital futuro de una precancelación (2026-09-01, pedido
                    // del usuario — la trampa gemela del abono, ver javadoc de la interfaz).
                    if (prestamo == null || prestamo.getCodigo() == null) {
                        throw new IncomeException(prefijoDescripcion + ": el pago " + pago.getCodigo()
                                + " (capital futuro de precancelación) no tiene préstamo asociado;"
                                + " no se puede ubicar qué cuotas canceló.");
                    }
                    // 'cuota' (= pago.getDetallePrestamo(), ya resuelta arriba) ES la ancla:
                    // ProcesoPagoPrestamoServiceImpl.precancelar graba el PagoPrestamo del
                    // capital futuro siempre contra la cuota ancla — no hace falta re-adivinarla.
                    lineas.addAll(lineasBandaCapitalFuturoPrecancelacion(producto.getCodigo(), idEmpresa,
                            capital, prestamo, cuota, fechaCorte, prefijoDescripcion));
                } else {
                    if (cuota == null || cuota.getFechaVencimiento() == null) {
                        throw new IncomeException(prefijoDescripcion + ": el pago " + pago.getCodigo()
                                + " tiene capital pero no tiene cuota con fecha de vencimiento; no"
                                + " se puede clasificar por banda.");
                    }
                    lineas.add(lineaBandaCapital(producto.getCodigo(), idEmpresa, capital,
                            cuota.getFechaVencimiento().toLocalDate(), fechaCorte, prefijoDescripcion));
                }
            }

            totalInteres += nvl(pago.getInteresPagado()) + nvl(pago.getInteresVencidoPagado());
            totalMora += nvl(pago.getMoraPagada());
            totalSeguroDesgravamen += nvl(pago.getDesgravamen());
            totalSeguroIncendio += nvl(pago.getValorSeguroIncendio());
        }

        // C15 / D-017 (H4): la mora cobrada que no estaba devengada para su cuota (la del mes en
        // curso) va directo a ingreso; solo la ya devengada cancela 1.4.02.xx.
        double totalMoraIngreso = 0.0;
        for (Double excedente : provisionInteresService.separarMoraNoDevengada(pagos, usuario).values()) {
            totalMoraIngreso += nvl(excedente);
        }
        totalMoraIngreso = redondear(totalMoraIngreso);
        totalMora = totalMora - totalMoraIngreso;

        totalInteres = redondear(totalInteres);
        totalMora = redondear(totalMora);
        if (totalMoraIngreso > 0.0) {
            lineas.add(lineaIngresoMora(idEmpresa, idTipoPrestamo, totalMoraIngreso, prefijoDescripcion));
        }
        totalSeguroDesgravamen = redondear(totalSeguroDesgravamen);
        totalSeguroIncendio = redondear(totalSeguroIncendio);

        if (totalInteres > 0.0) {
            lineas.add(lineaInteres(idPlantillaAplicacion, idTipoPrestamo, totalInteres, false, prefijoDescripcion));
        }
        if (totalMora > 0.0) {
            lineas.add(lineaInteres(idPlantillaAplicacion, idTipoPrestamo, totalMora, true, prefijoDescripcion));
        }
        if (totalSeguroDesgravamen > 0.0) {
            lineas.add(lineaSeguroDesgravamen(idPlantillaAplicacion, totalSeguroDesgravamen, prefijoDescripcion));
        }
        if (totalSeguroIncendio > 0.0) {
            lineas.add(lineaSeguroIncendio(idPlantillaAplicacion, idTipoPrestamo, totalSeguroIncendio,
                    prefijoDescripcion));
        }
        return lineas;
    }

    /**
     * @see ContabilizacionIndividualCreditoService#capitalFuturoPosteriorACorte(List, LocalDate, String)
     */
    @Override
    public double capitalFuturoPosteriorACorte(List<PagoPrestamo> pagos, LocalDate fechaCorteApertura,
            String prefijoDescripcion) throws Throwable {
        if (pagos == null || fechaCorteApertura == null) {
            return 0.0;
        }
        double total = 0.0;
        for (PagoPrestamo pago : pagos) {
            if (pago.getAnulado() != null && pago.getAnulado() == 1L) {
                continue;
            }
            double saldoOtros = nvl(pago.getSaldoOtros());
            if (saldoOtros <= 0.0) {
                // Mismo discriminador que haberDesdePagos: sin saldoOtros > 0 no hay nada de
                // capital fuera del cronograma normal que evaluar acá (cuota exigible normal).
                continue;
            }
            if (ProcesoPagoPrestamoService.TIPO_ABONO_CAPITAL.equals(pago.getTipo())) {
                if (pago.getEventoPrestamo() == null) {
                    continue;
                }
                List<HistDetallePrestamo> historizadas =
                        histDetallePrestamoDaoService.selectByEvento(pago.getEventoPrestamo().getCodigo());
                if ((historizadas == null || historizadas.isEmpty()) && esAbonoSinRegenerarTabla(pago)) {
                    // C16b: mismo universo que haberDesdePagos para el abono sin regenerar tabla.
                    historizadas = cuotasFuturasAbonoSinTabla(pago, prefijoDescripcion);
                }
                if (historizadas == null || historizadas.isEmpty()) {
                    continue;
                }
                // MISMO reparto proporcional que la banda del abono (lineasBandaCapitalAbono/
                // lineasReclasificacionAbonoCapital) — nunca un prorrateo nuevo.
                List<HistDetallePrestamo> ordenadas = ordenarPorNumeroCuota(historizadas);
                List<Double> partes = repartirProporcionalPorCapital(ordenadas, saldoOtros, prefijoDescripcion);
                for (int idx = 0; idx < ordenadas.size(); idx++) {
                    HistDetallePrestamo h = ordenadas.get(idx);
                    LocalDate vencimiento = h.getFechaVencimiento() != null
                            ? h.getFechaVencimiento().toLocalDate() : null;
                    if (vencimiento != null && vencimiento.isAfter(fechaCorteApertura)) {
                        total += partes.get(idx);
                    }
                }
            } else if (ProcesoPagoPrestamoService.TIPO_PRECANCELACION.equals(pago.getTipo())) {
                Prestamo prestamo = pago.getPrestamo();
                if (prestamo == null || prestamo.getCodigo() == null) {
                    continue;
                }
                // MISMA fuente que la banda del capital futuro de precancelación
                // (lineasBandaCapitalFuturoPrecancelacion, corrección 2026-09-30): el capital
                // PENDIENTE de las cuotas CANCELADA_ANTICIPADA(7) del préstamo, cada una con su
                // propio vencimiento real — ya no el bruto (cuota.getCapital()), que puede estar
                // por encima del pendiente si la cuota tenía un pago parcial previo. Y la ancla
                // SÍ entra cuando era una futura (pago.getDetallePrestamo(): precancelar siempre
                // graba ahí el PagoPrestamo del capital futuro) — queda en PAGADA, no en 7, pero
                // su capital pendiente es parte del mismo capital futuro.
                List<DetallePrestamo> todas = detallePrestamoDaoService.selectByPrestamo(prestamo.getCodigo());
                boolean hayCanceladasAnticipadas = false;
                if (todas != null) {
                    for (DetallePrestamo cuota : todas) {
                        if (cuota.getEstado() != null
                                && cuota.getEstado() == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
                            hayCanceladasAnticipadas = true;
                        }
                        if (cuota.getEstado() != null
                                && cuota.getEstado() == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA
                                && cuota.getFechaVencimiento() != null
                                && cuota.getFechaVencimiento().toLocalDate().isAfter(fechaCorteApertura)) {
                            total += redondear(motorPagoPrestamoService.calcularSaldosCuota(cuota).getSaldoCapital());
                        }
                    }
                }
                DetallePrestamo anclaFueraDeTabla = pago.getDetallePrestamo();
                if (!hayCanceladasAnticipadas && esCapitalFuturoFueraDeTabla(saldoOtros, anclaFueraDeTabla)) {
                    // C16 (D-021): mismo caso que lineasBandaCapitalFuturoPrecancelacion — capital
                    // futuro fuera de la tabla migrada; vence (a efectos de banda y de apertura) el
                    // mes siguiente a la ancla, así que la apertura del mes de la ancla no lo abrió.
                    LocalDate vencimientoSiguiente = anclaFueraDeTabla.getFechaVencimiento().toLocalDate().plusMonths(1);
                    vencimientoSiguiente = vencimientoSiguiente.withDayOfMonth(vencimientoSiguiente.lengthOfMonth());
                    if (vencimientoSiguiente.isAfter(fechaCorteApertura)) {
                        total += redondear(saldoOtros);
                    }
                    continue;
                }
                DetallePrestamo ancla = pago.getDetallePrestamo();
                if (ancla != null && ancla.getEstado() != null
                        && ancla.getEstado() != EstadoCuotaPrestamo.CANCELADA_ANTICIPADA
                        && ancla.getFechaVencimiento() != null
                        && ancla.getFechaVencimiento().toLocalDate().isAfter(fechaCorteApertura)) {
                    double capitalPendienteAncla =
                            redondear(motorPagoPrestamoService.calcularSaldosCuota(ancla).getSaldoCapital());
                    if (capitalPendienteAncla > TOLERANCIA) {
                        total += capitalPendienteAncla;
                    }
                }
            }
            // ⛔ NO agregar acá una rama para TIPO_PAGO_APORTES ("PAGO_APORTES", el cruce de
            // valores solo). No tiene rama A PROPÓSITO, no es un olvido — decisión del usuario,
            // 2026-09-04, §5 de DISENO-CIERRE-APERTURA-SOLO-LO-ABIERTO.md ("da igual si se paga
            // con dinero o con cruce de valores — lo que decide no es cómo se paga, sino qué se
            // paga"): un cruce liquida deuda EXIGIBLE con saldos del propio socio, nunca capital
            // futuro, así que no debe excluir nada del cierre. Hoy ya queda afuera solo
            // (pagarConAportes nunca llama a setSaldoOtros, así que sus PagoPrestamo tienen
            // saldoOtros = 0 y nunca llegan hasta este if), pero eso es una coincidencia de
            // implementación, no la regla — si ese guard cambiara, esta ausencia de rama es la
            // que de verdad lo protege.
            // (Una precancelación o un abono PAGADOS con aportes sí excluyen su tramo futuro: el
            // tipo del evento sigue siendo PRECANCELACION/ABONO_CAPITAL, y caen en las ramas de
            // arriba. Lo que no excluye nada es el cruce como operación propia — no conviertas
            // esto en "si hay aportes de por medio, no cierra".)
        }
        return redondear(total);
    }

    /** C16b: el pago es de un abono registrado SIN regenerar la tabla (evento con modalidad 0). */
    private boolean esAbonoSinRegenerarTabla(PagoPrestamo pago) {
        return pago.getEventoPrestamo() != null && pago.getEventoPrestamo().getModalidad() != null
                && pago.getEventoPrestamo().getModalidad().longValue()
                        == com.saa.ejb.crd.service.AbonoCapitalPrestamoService.MODALIDAD_SIN_REGENERAR_TABLA;
    }

    /**
     * C16b (D-030, 2026-10-29): para un abono sin regenerar tabla no hay cuotas historizadas. El
     * capital que el abono cancela es capital FUTURO a su fecha — el que la tabla ya re-amortizada
     * dejó de cobrar —, así que se reparte (con la misma {@link #repartirProporcionalPorCapital})
     * entre las cuotas vivas que vencen DESPUÉS de la fecha del abono, proporcional a su capital.
     * Copias transitorias en {@link HistDetallePrestamo} (nunca se persisten) para reusar el reparto
     * y el bandeo del abono de siempre sin duplicarlos.
     */
    private List<HistDetallePrestamo> cuotasFuturasAbonoSinTabla(PagoPrestamo pago, String prefijoDescripcion)
            throws Throwable {
        Prestamo prestamo = pago.getPrestamo();
        if (prestamo == null || prestamo.getCodigo() == null) {
            throw new IncomeException(prefijoDescripcion + ": el abono " + pago.getCodigo()
                    + " no tiene préstamo asociado; no se puede repartir el capital por banda.");
        }
        java.time.LocalDateTime fechaAbono = pago.getFecha() != null ? pago.getFecha()
                : pago.getEventoPrestamo().getFecha();
        if (fechaAbono == null) {
            throw new IncomeException(prefijoDescripcion + ": el abono " + pago.getCodigo()
                    + " no tiene fecha; no se puede saber qué cuotas son futuras.");
        }
        LocalDate dia = fechaAbono.toLocalDate();
        List<HistDetallePrestamo> futuras = new ArrayList<>();
        for (DetallePrestamo cuota : detallePrestamoDaoService.selectByPrestamo(prestamo.getCodigo())) {
            if (cuota.getFechaVencimiento() == null || !cuota.getFechaVencimiento().toLocalDate().isAfter(dia)
                    || nvl(cuota.getCapital()) <= 0.0) {
                continue;
            }
            HistDetallePrestamo copia = new HistDetallePrestamo();
            copia.setCodigo(cuota.getCodigo());
            copia.setNumeroCuota(cuota.getNumeroCuota());
            copia.setCapital(cuota.getCapital());
            copia.setFechaVencimiento(cuota.getFechaVencimiento());
            futuras.add(copia);
        }
        if (futuras.isEmpty()) {
            throw new IncomeException(prefijoDescripcion + ": el abono sin regenerar tabla " + pago.getCodigo()
                    + " no tiene cuotas con vencimiento posterior al " + dia + " en el préstamo "
                    + prestamo.getCodigo() + "; no se puede repartir su capital por banda.");
        }
        return futuras;
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    /**
     * @see ContabilizacionIndividualCreditoService#tipoCarteraYDias(LocalDate, LocalDate)
     */
    @Override
    public long[] tipoCarteraYDias(LocalDate fechaVencimiento, LocalDate fechaCorte) {
        if (fechaVencimiento.isBefore(fechaCorte)) {
            // VENCIDO empieza el día DESPUÉS del vencimiento — el día del vencimiento todavía es
            // POR VENCER (regla de negocio confirmada por el usuario, 2026-09-04). Por eso acá
            // NO hay +1: un +1 en esta rama contaría el día del vencimiento como vencido otra
            // vez, corriendo un día de más TODAS las cuotas vencidas y rompiendo los bordes de
            // banda (30/90/180/360 días). Ver el javadoc de la interfaz para la tabla de verdad.
            long dias = Math.max(1, ChronoUnit.DAYS.between(fechaVencimiento, fechaCorte));
            return new long[]{TipoCarteraBanda.VENCIDO, dias};
        }
        // fechaVencimiento == fechaCorte cae ACÁ (POR_VENCER, 1 día) — no es un descuido, es la
        // regla: el día del vencimiento todavía está por vencer.
        long dias = Math.max(1, ChronoUnit.DAYS.between(fechaCorte, fechaVencimiento));
        return new long[]{TipoCarteraBanda.POR_VENCER, dias};
    }

    /** Un tipo de aporte sin cuenta en la plantilla 21 NUNCA debería llegar acá: el registro ya
     * lo rechaza (ver {@code CobroCreditoServiceImpl#esTipoAporteContabilizable}). Si llega,
     * es un tipo nuevo sin parametrizar — falla fuerte, no se adivina la cuenta. */
    private int aux1ParaTipoAporte(Long idTipoAporte) throws Throwable {
        if (idTipoAporte != null && idTipoAporte == TIPO_APORTE_CESANTIA) {
            return CrdLineaAsiento.APORTES_CESANTIA;
        }
        if (idTipoAporte != null && idTipoAporte == TIPO_APORTE_JUBILACION) {
            return CrdLineaAsiento.APORTES_JUBILACION;
        }
        if (idTipoAporte != null && idTipoAporte == TIPO_APORTE_ADICIONAL) {
            return CrdLineaAsiento.APORTE_ADICIONAL_PERSONAL;
        }
        // Agregado 2026-09-05 (autorización expresa de lap-saa-1-arb) — ver JavaDoc de
        // TIPO_APORTE_PENSION_COMPLEMENTARIA. Requiere que CNT.DTPL tenga la línea
        // DTPLAXL1=53 en la plantilla alterno 21; si no existe todavía, esta rama igual no
        // rompe nada nuevo: selectByPlantillaYAuxiliar devuelve null y el llamador
        // (lineaAporteRegistrado/lineasCruceAportesConsumidos) lanza su propio
        // IncomeException con el aux1 exacto que falta — mismo modo de falla que ya existía,
        // con mejor mensaje.
        if (idTipoAporte != null && idTipoAporte == TIPO_APORTE_PENSION_COMPLEMENTARIA) {
            return CrdLineaAsiento.APORTES_PENSION_COMPLEMENTARIA;
        }
        if (idTipoAporte != null && idTipoAporte == TIPO_APORTE_JUBILACION_RETIRO_VOLUNTARIO) {
            return CrdLineaAsiento.APORTES_JUBILACION_RETIRO_VOLUNTARIO;
        }
        if (idTipoAporte != null && idTipoAporte == TIPO_APORTE_CESANTIA_RETIRO_VOLUNTARIO) {
            return CrdLineaAsiento.APORTES_CESANTIA_RETIRO_VOLUNTARIO;
        }
        if (idTipoAporte != null && idTipoAporte == TIPO_APORTE_JUBILACION_PATRONAL) {
            return CrdLineaAsiento.APORTES_PATRONALES_JUBILACION;
        }
        if (idTipoAporte != null && idTipoAporte == TIPO_APORTE_CESANTIA_PATRONAL) {
            return CrdLineaAsiento.APORTES_PATRONALES_CESANTIA;
        }
        throw new IncomeException("El tipo de aporte " + idTipoAporte + " no tiene cuenta contable"
                + " parametrizada (hoy solo 9 jubilación, 11 cesantía, 2 adicional, 23 pensión"
                + " complementaria, 22 jubilación retiro voluntario, 21 cesantía retiro voluntario,"
                + " 13 jubilación patronal, 14 cesantía patronal); no se puede contabilizar.");
    }

    private DetalleAsiento lineaHaberDesdePlantilla(DetallePlantilla plantilla, double valor, String descripcion) {
        PlanCuenta cuenta = plantilla.getPlanCuenta();
        DetalleAsiento detalle = new DetalleAsiento();
        detalle.setPlanCuenta(cuenta);
        detalle.setNumeroCuenta(cuenta.getCuentaContable());
        detalle.setNombreCuenta(cuenta.getNombre());
        detalle.setDescripcion(descripcion);
        detalle.setValorDebe(0.0);
        detalle.setValorHaber(redondear(valor));
        return detalle;
    }

    // =====================================================================
    // Reparto (2026-08-31) — extraído de CobroPetroContableServiceImpl.contabilizarReparto.
    // =====================================================================

    @Override
    public Long resolverPlantillaReparto(Long idEmpresa) throws Throwable {
        Long idPlantilla = plantillaService.codigoByAlterno(PlantillasCredito.REPARTO_TRANSITORIA, idEmpresa);
        if (idPlantilla == null) {
            throw new IncomeException("No existe la plantilla contable alterno "
                    + PlantillasCredito.REPARTO_TRANSITORIA + " para la empresa " + idEmpresa + ".");
        }
        return idPlantilla;
    }

    @Override
    public List<DetalleAsiento> lineasReparto(Long idPlantilla, double totalAportes, double totalPrestamos,
            String prefijoDescripcion) throws Throwable {
        List<DetalleAsiento> lineas = new ArrayList<>();
        if (totalAportes > 0.0) {
            DetallePlantilla lineaAportes = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla, 2);
            if (lineaAportes == null || lineaAportes.getPlanCuenta() == null) {
                throw new IncomeException("La plantilla de reparto (alterno " + PlantillasCredito.REPARTO_TRANSITORIA
                        + ") no tiene la línea de aportes por cobrar (aux1=2).");
            }
            lineas.add(lineaMovimientoDesdePlantilla(lineaAportes, totalAportes, prefijoDescripcion + " - aportes"));
        }
        if (totalPrestamos > 0.0) {
            DetallePlantilla lineaPrestamos = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla, 3);
            if (lineaPrestamos == null || lineaPrestamos.getPlanCuenta() == null) {
                throw new IncomeException("La plantilla de reparto (alterno " + PlantillasCredito.REPARTO_TRANSITORIA
                        + ") no tiene la línea de préstamos por cobrar (aux1=3).");
            }
            lineas.add(lineaMovimientoDesdePlantilla(lineaPrestamos, totalPrestamos,
                    prefijoDescripcion + " - préstamos"));
        }
        return lineas;
    }

    // =====================================================================
    // D-024 (28-oct-2026) — complemento de apertura
    // =====================================================================

    /** D-032: ¿abrió algún ③ el mes {@code primeroDelMes}? (la última corrida EJECUTADA antes del mes lo abrió). */
    @EJB
    private com.saa.ejb.crd.dao.CorridaCierreCarteraDaoService corridaCierreCarteraDaoService;

    @Override
    public com.saa.ejb.crd.service.dto.ComplementoAperturaCobro complementoApertura(Long idEmpresa, LocalDate fecha,
            List<DetalleAsiento> lineasDelCobro, String referencia) throws Throwable {
        return complementoApertura(idEmpresa, fecha, lineasDelCobro, referencia, null, 0.0, null);
    }

    @Override
    public com.saa.ejb.crd.service.dto.PuentePrestamosMes puentePrestamosMes(Long idEmpresa, String cuenta,
            LocalDate finMes, Long excluirCarga) throws Throwable {
        com.saa.ejb.crd.service.dto.PuentePrestamosMes puente = new com.saa.ejb.crd.service.dto.PuentePrestamosMes();
        LocalDate desde = finMes.withDayOfMonth(1);
        LocalDate corteAnterior = desde.minusDays(1);
        // Lo abierto existe solo si una corrida EJECUTADA abrió el mes (la que cerró M−1, fechaProceso = 1.º de M).
        com.saa.model.crd.CorridaCierreCartera corrida = corridaCierreCarteraDaoService.selectUltimaEjecutadaAntesDe(
                idEmpresa, Long.valueOf(desde.getYear()), Long.valueOf(desde.getMonthValue()));
        boolean abrioElMes = corrida != null && corrida.getFechaProceso() != null
                && !corrida.getFechaProceso().isBefore(desde) && !corrida.getFechaProceso().isAfter(finMes);
        if (abrioElMes) {
            for (Object[] fila : cierreCarteraDaoService.selectAperturaPorPrestamo(finMes, corteAnterior)) {
                if (fila[0] != null && fila[1] != null) {
                    puente.getAbierto().merge((Long) fila[0], (Double) fila[1], Double::sum);
                }
            }
        } else {
            System.out.println("ContabilizacionIndividualCreditoService.puentePrestamosMes - ninguna corrida ejecutada"
                    + " abrió " + desde + " (última: " + (corrida != null ? corrida.getCodigo() : null)
                    + "): abierto por préstamo = 0.");
        }
        for (Object[] fila : cierreCarteraDaoService.selectCobradoPuentePorPrestamo(idEmpresa, cuenta, desde, finMes,
                excluirCarga)) {
            if (fila[0] != null && fila[1] != null) {
                puente.getCobrado().merge((Long) fila[0], (Double) fila[1], Double::sum);
            }
        }
        puente.setNoAtribuible(cierreCarteraDaoService.selectNoAtribuiblePetro(idEmpresa, cuenta, desde, finMes,
                excluirCarga));
        puente.setComplementoRegistrado(cierreCarteraDaoService.selectComplementoRegistrado(idEmpresa, cuenta, desde,
                finMes));
        return puente;
    }

    @Override
    public Map<Long, Double> repartePorLineasDePrestamo(List<com.saa.model.crd.DetalleCobroCredito> detalles,
            double monto) {
        Map<Long, Double> pesos = new LinkedHashMap<>();
        if (detalles != null) {
            for (com.saa.model.crd.DetalleCobroCredito detalle : detalles) {
                if (detalle.getPrestamo() != null && detalle.getPrestamo().getCodigo() != null) {
                    pesos.merge(detalle.getPrestamo().getCodigo(), nvl(detalle.getValor()), Double::sum);
                }
            }
        }
        Map<Long, Double> reparto = new LinkedHashMap<>();
        if (pesos.isEmpty() || monto == 0.0) {
            return reparto;
        }
        double total = 0.0;
        for (Double peso : pesos.values()) {
            total += peso;
        }
        for (Map.Entry<Long, Double> e : pesos.entrySet()) {
            reparto.put(e.getKey(), total != 0.0 ? monto * e.getValue() / total : monto / pesos.size());
        }
        return reparto;
    }

    @Override
    public com.saa.ejb.crd.service.dto.ComplementoAperturaCobro complementoApertura(Long idEmpresa, LocalDate fecha,
            List<DetalleAsiento> lineasDelCobro, String referencia, Map<Long, Double> cobradoPorPrestamo,
            double noAtribuible, Long excluirCarga) throws Throwable {
        com.saa.ejb.crd.service.dto.ComplementoAperturaCobro complemento =
                new com.saa.ejb.crd.service.dto.ComplementoAperturaCobro();
        if (lineasDelCobro == null || lineasDelCobro.isEmpty() || fecha == null) {
            return complemento;
        }
        Long idPlantillaApertura = plantillaService.codigoByAlterno(PlantillasCredito.APERTURA_PLANILLA_MENSUAL,
                idEmpresa);
        if (idPlantillaApertura == null) {
            throw new IncomeException("No existe la plantilla contable alterno "
                    + PlantillasCredito.APERTURA_PLANILLA_MENSUAL + " (apertura del período) para la empresa "
                    + idEmpresa + "; no se puede medir el complemento de apertura de " + referencia + " (D-024).");
        }
        DetallePlantilla aportesPorCobrar = lineaApertura(idPlantillaApertura, CrdLineaAsiento.APORTES_POR_COBRAR, referencia);
        DetallePlantilla aportesPorAplicar = lineaApertura(idPlantillaApertura, CrdLineaAsiento.APORTES_POR_APLICAR, referencia);
        DetallePlantilla prestamosPorCobrar = lineaApertura(idPlantillaApertura, CrdLineaAsiento.PRESTAMOS_POR_COBRAR, referencia);
        DetallePlantilla prestamosPorAplicar = lineaApertura(idPlantillaApertura, CrdLineaAsiento.PRESTAMOS_POR_APLICAR, referencia);

        String cuentaAportes = aportesPorCobrar.getPlanCuenta().getCuentaContable();
        String cuentaPrestamos = prestamosPorCobrar.getPlanCuenta().getCuentaContable();
        complemento.setCuentaAportes(cuentaAportes);
        complemento.setCuentaPrestamos(cuentaPrestamos);

        // Lo que el asiento del cobro acredita (haber − debe) en cada cuenta por cobrar.
        double cobradoAportes = 0.0;
        double cobradoPrestamos = 0.0;
        for (DetalleAsiento linea : lineasDelCobro) {
            String cuenta = linea.getNumeroCuenta() != null ? linea.getNumeroCuenta()
                    : (linea.getPlanCuenta() != null ? linea.getPlanCuenta().getCuentaContable() : null);
            double neto = nvl(linea.getValorHaber()) - nvl(linea.getValorDebe());
            if (cuentaAportes.equals(cuenta)) {
                cobradoAportes += neto;
            } else if (cuentaPrestamos.equals(cuenta)) {
                cobradoPrestamos += neto;
            }
        }
        cobradoAportes = redondear(cobradoAportes);
        cobradoPrestamos = redondear(cobradoPrestamos);
        if (cobradoAportes <= 0.0 && cobradoPrestamos <= 0.0) {
            return complemento;
        }

        LocalDate finDeMes = fecha.withDayOfMonth(fecha.lengthOfMonth());
        String fechaTexto = finDeMes.toString();
        if (cobradoAportes > 0.0) {
            double abierto = saldoDeudor(idEmpresa, cuentaAportes, finDeMes);
            double falta = redondear(cobradoAportes - Math.max(abierto, 0.0));
            complemento.setCobradoAportes(cobradoAportes);
            complemento.setAbiertoAportes(abierto);
            if (falta >= TOLERANCIA) {
                complemento.setComplementoAportes(falta);
                String glosa = "Complemento de apertura " + referencia + " aportes: cobrado " + formato(cobradoAportes)
                        + ", abierto " + formato(abierto) + " al " + fechaTexto;
                complemento.getLineas().add(lineaMovimientoDesdePlantilla(aportesPorCobrar, falta, glosa));
                complemento.getLineas().add(lineaMovimientoDesdePlantilla(aportesPorAplicar, falta, glosa));
            }
        }
        if (cobradoPrestamos > 0.0 && cobradoPorPrestamo != null
                && com.saa.ejb.crd.service.ModoCorteCartera.neteoPorPrestamo(finDeMes, null)) {
            // D-032 (29-oct-2026): por préstamo contra lo que la apertura abrió de cada uno. Objetivo
            // acumulado del mes (contando este cobro) menos lo ya complementado en el mes: lo que un
            // cobro anterior del mes no abrió lo recoge este.
            com.saa.ejb.crd.service.dto.PuentePrestamosMes puente = puentePrestamosMes(idEmpresa, cuentaPrestamos,
                    finDeMes, excluirCarga);
            puente.sumaCobrado(cobradoPorPrestamo);
            puente.setNoAtribuible(puente.getNoAtribuible() + Math.max(noAtribuible, 0.0));
            double objetivo = puente.complementoObjetivo();
            double registrado = redondear(puente.getComplementoRegistrado());
            double falta = redondear(objetivo - registrado);
            complemento.setCobradoPrestamos(cobradoPrestamos);
            complemento.setAbiertoPrestamos(redondear(puente.totalAbierto()));
            complemento.setNotaPrestamos("por prestamo (D-032): cobrado sin abrir en el mes " + formato(puente.cobradoSinAbrir())
                    + " en " + puente.prestamosConComplemento() + " prestamos + no atribuible " + formato(puente.getNoAtribuible())
                    + " - ya complementado " + formato(registrado) + " = " + formato(Math.max(falta, 0.0)));
            if (falta >= TOLERANCIA) {
                complemento.setComplementoPrestamos(falta);
                String glosa = com.saa.ejb.crd.service.dto.ComplementoAperturaCobro.GLOSA_PREFIJO + " " + referencia
                        + " cuotas por prestamo (D-032): sin abrir " + formato(objetivo) + ", ya abierto "
                        + formato(registrado) + " en " + fechaTexto;
                complemento.getLineas().add(lineaMovimientoDesdePlantilla(prestamosPorCobrar, falta, glosa));
                complemento.getLineas().add(lineaMovimientoDesdePlantilla(prestamosPorAplicar, falta, glosa));
            } else if (falta <= -TOLERANCIA) {
                System.out.println("ContabilizacionIndividualCreditoService.complementoApertura - " + referencia
                        + ": el mes ya tiene " + registrado + " de complemento de cuotas y el objetivo es " + objetivo
                        + " (un cobro del mes se reversó con su complemento en otro asiento): no se resta; lo muestra"
                        + " el control del ⑥.");
            }
        } else if (cobradoPrestamos > 0.0) {
            double abierto = saldoDeudor(idEmpresa, cuentaPrestamos, finDeMes);
            double falta = redondear(cobradoPrestamos - Math.max(abierto, 0.0));
            complemento.setCobradoPrestamos(cobradoPrestamos);
            complemento.setAbiertoPrestamos(abierto);
            if (falta >= TOLERANCIA) {
                complemento.setComplementoPrestamos(falta);
                String glosa = "Complemento de apertura " + referencia + " cuotas: cobrado " + formato(cobradoPrestamos)
                        + ", abierto " + formato(abierto) + " al " + fechaTexto;
                complemento.getLineas().add(lineaMovimientoDesdePlantilla(prestamosPorCobrar, falta, glosa));
                complemento.getLineas().add(lineaMovimientoDesdePlantilla(prestamosPorAplicar, falta, glosa));
            }
        }
        System.out.println("ContabilizacionIndividualCreditoService.complementoApertura - " + referencia + " al "
                + fechaTexto + ": aportes cobrado " + cobradoAportes + " / abierto " + complemento.getAbiertoAportes()
                + " -> complemento " + complemento.getComplementoAportes() + "; cuotas cobrado " + cobradoPrestamos
                + " / abierto " + complemento.getAbiertoPrestamos() + " -> complemento "
                + complemento.getComplementoPrestamos());
        return complemento;
    }

    private DetallePlantilla lineaApertura(Long idPlantillaApertura, int codigoLinea, String referencia)
            throws Throwable {
        DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantillaApertura, codigoLinea);
        if (linea == null || linea.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno " + PlantillasCredito.APERTURA_PLANILLA_MENSUAL
                    + " no tiene la línea " + codigoLinea + " del catálogo CRD_LINEA_ASIENTO; no se puede armar el"
                    + " complemento de apertura de " + referencia + " (D-024).");
        }
        return linea;
    }

    /** Saldo deudor (debe − haber) de la cuenta y sus hijas al corte, asientos activos/mayorizados. */
    private double saldoDeudor(Long idEmpresa, String cuenta, LocalDate corte) throws Throwable {
        double saldo = 0.0;
        for (Object[] fila : cierreCarteraDaoService.selectSaldoContablePorCuenta(idEmpresa, cuenta, null, corte)) {
            String cuentaFila = fila[0] != null ? fila[0].toString() : "";
            // LIKE 'cuenta%' también traería, p. ej., 1.4.05.050 si existiera: solo la cuenta y sus hijas.
            if (cuentaFila.equals(cuenta) || cuentaFila.startsWith(cuenta + ".")) {
                saldo += fila[2] != null ? ((Number) fila[2]).doubleValue() : 0.0;
            }
        }
        return redondear(saldo);
    }

    private static String formato(double valor) {
        return String.format("%,.2f", valor);
    }

    /**
     * Respeta {@code DetallePlantilla.movimiento} — a diferencia de
     * {@link #lineaHaberDesdePlantilla}, que fuerza Haber. Réplica exacta de
     * {@code CobroPetroContableServiceImpl.lineaDesdePlantilla} (antes de esta extracción):
     * mismo cálculo, para que el asiento de Petro no cambie ni un signo.
     */
    private DetalleAsiento lineaMovimientoDesdePlantilla(DetallePlantilla plantilla, double valor,
            String descripcion) {
        PlanCuenta cuenta = plantilla.getPlanCuenta();
        boolean debe = plantilla.getMovimiento() != null && plantilla.getMovimiento().longValue() == 1L;
        DetalleAsiento detalle = new DetalleAsiento();
        detalle.setPlanCuenta(cuenta);
        detalle.setNumeroCuenta(cuenta.getCuentaContable());
        detalle.setNombreCuenta(cuenta.getNombre());
        detalle.setDescripcion(descripcion);
        detalle.setValorDebe(debe ? redondear(valor) : 0.0);
        detalle.setValorHaber(debe ? 0.0 : redondear(valor));
        return detalle;
    }

    // =====================================================================
    // Aplicación / "por aplicar" (2026-08-31) — corrige el asiento 3 (CBCRASN2) de
    // CobroCreditoServiceImpl, que volvía a debitar la transitoria después de que el asiento
    // de reparto ya la había cerrado. Ver el javadoc de la interfaz.
    // =====================================================================

    @Override
    public List<DetalleAsiento> lineasAplicacionPorAplicar(Long idPlantillaAplicacion, double totalAportes,
            double totalPrestamos, String prefijoDescripcion) throws Throwable {
        List<DetalleAsiento> lineas = new ArrayList<>();
        if (totalAportes > 0.0) {
            DetallePlantilla lineaAportes = detallePlantillaDaoService.selectByPlantillaYAuxiliar(
                    idPlantillaAplicacion, CrdLineaAsiento.APORTES_POR_APLICAR);
            if (lineaAportes == null || lineaAportes.getPlanCuenta() == null) {
                throw new IncomeException("La plantilla de aplicación (alterno " + PlantillasCredito.APLICACION_PETRO
                        + ") no tiene la línea de aportes por aplicar (CrdLineaAsiento.APORTES_POR_APLICAR).");
            }
            lineas.add(lineaMovimientoDesdePlantilla(lineaAportes, totalAportes,
                    prefijoDescripcion + " - aportes por aplicar"));
        }
        if (totalPrestamos > 0.0) {
            DetallePlantilla lineaPrestamos = detallePlantillaDaoService.selectByPlantillaYAuxiliar(
                    idPlantillaAplicacion, CrdLineaAsiento.PRESTAMOS_POR_APLICAR);
            if (lineaPrestamos == null || lineaPrestamos.getPlanCuenta() == null) {
                throw new IncomeException("La plantilla de aplicación (alterno " + PlantillasCredito.APLICACION_PETRO
                        + ") no tiene la línea de préstamos por aplicar (CrdLineaAsiento.PRESTAMOS_POR_APLICAR).");
            }
            lineas.add(lineaMovimientoDesdePlantilla(lineaPrestamos, totalPrestamos,
                    prefijoDescripcion + " - préstamos por aplicar"));
        }
        return lineas;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}

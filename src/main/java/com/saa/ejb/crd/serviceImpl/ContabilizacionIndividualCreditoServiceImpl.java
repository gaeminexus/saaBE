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

    /** CRD.TPAP.TPAPCDGO de los únicos tres tipos con cuenta en la plantilla 21 (aux1 50/51/52).
     * Mismos valores que {@code CobroCreditoServiceImpl}/{@code CobroPetroContableServiceImpl}
     * — catálogo de negocio estable, no una resolución que pueda divergir. */
    private static final long TIPO_APORTE_JUBILACION = 9L;
    private static final long TIPO_APORTE_CESANTIA = 11L;
    private static final long TIPO_APORTE_ADICIONAL = 2L;

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
     * <p><b>Cuadre explícito</b>: la suma del capital de esas cuotas tiene que coincidir con
     * {@code capitalFuturo} (el {@code saldoOtros} del pago) — si no, {@code IncomeException}
     * con los cuatro números (préstamo, cuántas cuotas encontró, cuánto suman, contra qué
     * capital futuro no cuadra), nunca un asiento armado a medias.
     */
    private List<DetalleAsiento> lineasBandaCapitalFuturoPrecancelacion(Long idProducto, Long idEmpresa,
            double capitalFuturo, Prestamo prestamo, LocalDate fechaCorte, String prefijoDescripcion)
            throws Throwable {

        List<DetallePrestamo> todas = detallePrestamoDaoService.selectByPrestamo(prestamo.getCodigo());
        List<DetallePrestamo> canceladasAnticipadamente = new ArrayList<>();
        if (todas != null) {
            for (DetallePrestamo cuota : todas) {
                if (cuota.getEstado() != null && cuota.getEstado() == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
                    canceladasAnticipadamente.add(cuota);
                }
            }
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
            double capitalCuota = redondear(nvl(cuota.getCapital()));
            totalCuotas += capitalCuota;
            LocalDate vencimiento = cuota.getFechaVencimiento() != null
                    ? cuota.getFechaVencimiento().toLocalDate() : null;
            entradas.add(new EntradaBanda(vencimiento, capitalCuota,
                    "la cuota #" + cuota.getNumeroCuota() + " (cancelada anticipadamente)"));
        }
        totalCuotas = redondear(totalCuotas);

        if (Math.abs(redondear(totalCuotas - capitalFuturo)) > TOLERANCIA) {
            throw new IncomeException(prefijoDescripcion + ": el préstamo " + prestamo.getCodigo()
                    + " tiene " + canceladasAnticipadamente.size() + " cuota(s) en estado"
                    + " CANCELADA_ANTICIPADA(7) que suman $" + totalCuotas + " de capital, pero no"
                    + " coincide con el capital futuro del pago ($" + redondear(capitalFuturo)
                    + "); no se puede armar el reparto por banda de la precancelación.");
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
            Long idPlantillaAplicacion, LocalDate fechaCorte, String prefijoDescripcion) throws Throwable {
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
                    lineas.addAll(lineasBandaCapitalFuturoPrecancelacion(producto.getCodigo(), idEmpresa,
                            capital, prestamo, fechaCorte, prefijoDescripcion));
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

        totalInteres = redondear(totalInteres);
        totalMora = redondear(totalMora);
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

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    @Override
    public long[] tipoCarteraYDias(LocalDate fechaVencimiento, LocalDate fechaCorte) {
        if (!fechaVencimiento.isAfter(fechaCorte)) {
            long dias = Math.max(1, ChronoUnit.DAYS.between(fechaVencimiento, fechaCorte) + 1);
            return new long[]{TipoCarteraBanda.VENCIDO, dias};
        }
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
        throw new IncomeException("El tipo de aporte " + idTipoAporte + " no tiene cuenta contable"
                + " parametrizada (hoy solo 9 jubilación, 11 cesantía, 2 adicional); no se puede"
                + " contabilizar.");
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

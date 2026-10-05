package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.ejb.crd.dao.CierreCarteraDaoService;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.MovimientoInteresCuotaDaoService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.service.ProcesoMoraPrestamoService;
import com.saa.ejb.crd.service.ProvisionInteresService;
import com.saa.ejb.crd.service.dto.BandaProductoDetalle;
import com.saa.ejb.crd.service.dto.ItemReclasificacionBanda;
import com.saa.ejb.crd.service.dto.ResultadoReversoProvision;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.crd.CorridaCierreCartera;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.MovimientoInteresCuota;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.ComponenteMovimientoInteresCuota;
import com.saa.rubros.CrdLineaAsiento;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.PlantillasCredito;
import com.saa.rubros.TipoAsientos;
import com.saa.rubros.TipoMovimientoInteresCuota;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @see ProvisionInteresService
 */
@Stateless
public class ProvisionInteresServiceImpl implements ProvisionInteresService {

    private static final double TOLERANCIA = 0.01;

    @EJB
    private MovimientoInteresCuotaDaoService movimientoInteresCuotaDaoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private ProcesoMoraPrestamoService procesoMoraPrestamoService;

    @EJB
    private CierreCarteraDaoService cierreCarteraDaoService;

    @EJB
    private PlantillaService plantillaService;

    @EJB
    private DetallePlantillaDaoService detallePlantillaDaoService;

    @EJB
    private AsientoContableService asientoContableService;

    @EJB
    private com.saa.ejb.crd.dao.PrestamoDaoService prestamoDaoService;

    @EJB
    private com.saa.ejb.cnt.dao.PlanCuentaDaoService planCuentaDaoService;

    @EJB
    private com.saa.ejb.crd.dao.CorridaCierreCarteraDaoService corridaCierreCarteraDaoService;

    @Override
    public Map<Long, double[]> saldoProvisionadoPorCuotas(List<Long> idsCuota) throws Throwable {
        System.out.println("ProvisionInteresServiceImpl.saldoProvisionadoPorCuotas - cuotas: "
            + (idsCuota != null ? idsCuota.size() : 0));
        Map<Long, double[]> resultado = new HashMap<>();
        if (idsCuota == null || idsCuota.isEmpty()) {
            return resultado;
        }
        for (Object[] fila : movimientoInteresCuotaDaoService.selectSaldoProvisionadoPorCuotas(idsCuota)) {
            Long idCuota = (Long) fila[0];
            long componente = ((Number) fila[1]).longValue();
            double saldo = fila[2] != null ? ((Number) fila[2]).doubleValue() : 0.0;
            double[] acumulado = resultado.computeIfAbsent(idCuota, k -> new double[2]);
            if (componente == ComponenteMovimientoInteresCuota.INTERES) {
                acumulado[0] = saldo;
            } else if (componente == ComponenteMovimientoInteresCuota.MORA) {
                acumulado[1] = saldo;
            }
        }
        return resultado;
    }

    @Override
    public Map<Long, double[]> calcularProvisionPorTipoPrestamo(LocalDate fechaCorte) throws Throwable {
        System.out.println("ProvisionInteresServiceImpl.calcularProvisionPorTipoPrestamo - corte: " + fechaCorte);
        Map<Long, double[]> porTipo = new HashMap<>();
        for (ItemProvisionCuota item : calcularItems(fechaCorte)) {
            double[] acumulado = porTipo.computeIfAbsent(item.idTipoPrestamo, k -> new double[2]);
            acumulado[0] = redondear(acumulado[0] + item.aProvisionarInteres);
            acumulado[1] = redondear(acumulado[1] + item.aProvisionarMora);
        }
        return porTipo;
    }

    @Override
    public void registrarProvisionCierre(Long idCorrida, Long idAsiento, LocalDate fechaCorte, String usuario)
            throws Throwable {
        System.out.println("ProvisionInteresServiceImpl.registrarProvisionCierre - corrida: " + idCorrida
            + " - asiento: " + idAsiento + " - corte: " + fechaCorte);
        if (idCorrida == null) {
            throw new IncomeException("idCorrida es obligatorio para registrar la provisión del cierre");
        }
        LocalDateTime ahora = LocalDateTime.now();
        for (ItemProvisionCuota item : calcularItems(fechaCorte)) {
            if (item.aProvisionarInteres > TOLERANCIA) {
                guardarMovimiento(item.cuota.getCodigo(), item.prestamo, TipoMovimientoInteresCuota.PROVISION,
                    ComponenteMovimientoInteresCuota.INTERES, item.aProvisionarInteres, fechaCorte,
                    idCorrida, null, "CIERRE", idCorrida, idAsiento, usuario, ahora);
            }
            if (item.aProvisionarMora > TOLERANCIA) {
                guardarMovimiento(item.cuota.getCodigo(), item.prestamo, TipoMovimientoInteresCuota.PROVISION,
                    ComponenteMovimientoInteresCuota.MORA, item.aProvisionarMora, fechaCorte,
                    idCorrida, null, "CIERRE", idCorrida, idAsiento, usuario, ahora);
            }
        }
    }

    @Override
    public void registrarDevengoMoraCierre(Long idCorrida, Long idAsiento, LocalDate desde, LocalDate hasta,
            Map<Long, Double> totalMoraPorTipoAsiento, String usuario) throws Throwable {
        System.out.println("ProvisionInteresServiceImpl.registrarDevengoMoraCierre - corrida: " + idCorrida
            + " - rango: " + desde + " a " + hasta);
        if (idCorrida == null) {
            throw new IncomeException("idCorrida es obligatorio para registrar el devengo de mora del cierre");
        }
        LocalDateTime ahora = LocalDateTime.now();
        Map<Long, Double> totalMoraPorTipoMvic = new HashMap<>();

        for (Object[] fila : cierreCarteraDaoService.selectCuotasDevengoMoraEnRango(desde, hasta)) {
            Long idCuota = (Long) fila[0];
            Long idPrestamoFila = (Long) fila[1];
            Long idTipoPrestamo = (Long) fila[2];
            double mora = fila[3] != null ? ((Number) fila[3]).doubleValue() : 0.0;
            if (mora <= TOLERANCIA) {
                continue;
            }
            Prestamo prestamoReferencia = new Prestamo();
            prestamoReferencia.setCodigo(idPrestamoFila);
            guardarMovimiento(idCuota, prestamoReferencia, TipoMovimientoInteresCuota.DEVENGO_MORA,
                ComponenteMovimientoInteresCuota.MORA, mora, desde, idCorrida, null, "CIERRE", idCorrida,
                idAsiento, usuario, ahora);

            totalMoraPorTipoMvic.merge(idTipoPrestamo, mora, Double::sum);
        }

        // Invariante verificada, no exigida (pedido del árbitro 2026-10-05): el asiento manda.
        if (totalMoraPorTipoAsiento != null) {
            for (Map.Entry<Long, Double> entrada : totalMoraPorTipoAsiento.entrySet()) {
                double delAsiento = redondear(nvl(entrada.getValue()));
                double delLibro = redondear(nvl(totalMoraPorTipoMvic.get(entrada.getKey())));
                if (Math.abs(redondear(delAsiento - delLibro)) > TOLERANCIA) {
                    System.out.println("  ⚠️ ProvisionInteresServiceImpl.registrarDevengoMoraCierre - tipo"
                        + " de préstamo " + entrada.getKey() + ": el asiento devengó $" + delAsiento
                        + " pero el detalle por cuota (MVIC tipo 5) suma $" + delLibro
                        + " — diferencia $" + redondear(delAsiento - delLibro) + ". El asiento manda;"
                        + " el libro queda con lo que de verdad encontró, revisar.");
                }
            }
        }
    }

    @Override
    public ResultadoReversoProvision reversarPorPagos(List<PagoPrestamo> pagos, Long idEmpresa, LocalDate fecha,
            String origen, Long idOrigen, String usuario) throws Throwable {
        System.out.println("ProvisionInteresServiceImpl.reversarPorPagos - origen: " + origen
            + " - idOrigen: " + idOrigen + " - pagos: " + (pagos != null ? pagos.size() : 0));
        ResultadoReversoProvision resultado = new ResultadoReversoProvision();
        if (pagos == null || pagos.isEmpty()) {
            return resultado;
        }

        // Agregado SOLO de los pagos de esta operación (no el histórico de la cuota).
        Map<Long, double[]> pagadoPorCuota = new LinkedHashMap<>();
        Map<Long, Long> primerPagoPorCuota = new LinkedHashMap<>();
        Map<Long, Prestamo> prestamoPorCuota = new LinkedHashMap<>();
        Map<Long, Long> tipoPrestamoPorCuota = new LinkedHashMap<>();

        for (PagoPrestamo pago : pagos) {
            if (pago == null || pago.getDetallePrestamo() == null) {
                continue;
            }
            if (pago.getAnulado() != null && pago.getAnulado().longValue() == 1L) {
                continue;
            }
            Long idCuota = pago.getDetallePrestamo().getCodigo();
            double interesPagado = nvl(pago.getInteresPagado()) + nvl(pago.getInteresVencidoPagado());
            double moraPagada = nvl(pago.getMoraPagada());
            double[] acumulado = pagadoPorCuota.computeIfAbsent(idCuota, k -> new double[2]);
            acumulado[0] += interesPagado;
            acumulado[1] += moraPagada;
            primerPagoPorCuota.putIfAbsent(idCuota, pago.getCodigo());
            Prestamo prestamo = pago.getPrestamo();
            prestamoPorCuota.putIfAbsent(idCuota, prestamo);
            if (!tipoPrestamoPorCuota.containsKey(idCuota) && prestamo != null && prestamo.getProducto() != null
                    && prestamo.getProducto().getTipoPrestamo() != null) {
                tipoPrestamoPorCuota.put(idCuota, prestamo.getProducto().getTipoPrestamo().getCodigo());
            }
        }
        if (pagadoPorCuota.isEmpty()) {
            return resultado;
        }

        Map<Long, double[]> saldoProvisionado = saldoProvisionadoPorCuotas(new ArrayList<>(pagadoPorCuota.keySet()));

        // Cuánto se reversa por cuota, acotado al saldo realmente provisionado (nunca más).
        Map<Long, double[]> aReversarPorCuota = new LinkedHashMap<>();
        Map<Long, Double> totalPorTipo = new LinkedHashMap<>();
        double totalGeneral = 0.0;
        for (Map.Entry<Long, double[]> entrada : pagadoPorCuota.entrySet()) {
            Long idCuota = entrada.getKey();
            double[] pagado = entrada.getValue();
            double[] provisionado = saldoProvisionado.getOrDefault(idCuota, new double[2]);
            double interesReversado = Math.max(0.0, redondear(Math.min(pagado[0], provisionado[0])));
            double moraReversada = Math.max(0.0, redondear(Math.min(pagado[1], provisionado[1])));
            if (interesReversado <= TOLERANCIA && moraReversada <= TOLERANCIA) {
                continue;
            }
            aReversarPorCuota.put(idCuota, new double[]{interesReversado, moraReversada});
            Long idTipoPrestamo = tipoPrestamoPorCuota.get(idCuota);
            if (idTipoPrestamo == null) {
                throw new IncomeException("La cuota " + idCuota + " tiene provisión que reversar pero su"
                    + " préstamo no tiene tipo de préstamo asignado; no se puede resolver la cuenta contable"
                    + " del reverso.");
            }
            double totalCuota = interesReversado + moraReversada;
            totalPorTipo.merge(idTipoPrestamo, totalCuota, Double::sum);
            totalGeneral += totalCuota;
        }
        if (aReversarPorCuota.isEmpty()) {
            return resultado;
        }

        Long idPlantilla = resuelvePlantilla36(idEmpresa);
        List<DetalleAsiento> lineas = new ArrayList<>();
        for (Map.Entry<Long, Double> entrada : totalPorTipo.entrySet()) {
            // Invertido respecto de la provisión (R4/sql/310): acá 81 (149905) va al DEBE y
            // 80 (470510) al HABER — es un reverso, no una provisión nueva.
            lineas.add(lineaProvision(idPlantilla, CrdLineaAsiento.PROVISION_INTERESES, entrada.getKey(),
                entrada.getValue(), true, "Reverso de provisión de intereses por cobro - " + origen));
            lineas.add(lineaProvision(idPlantilla, CrdLineaAsiento.PROVISION_INTERESES_GASTO, entrada.getKey(),
                entrada.getValue(), false, "Reverso de provisión de intereses por cobro - " + origen));
        }

        Asiento asiento = asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS, fecha,
            "Reverso de provisión de intereses por cobro - " + origen, usuario, lineas,
            Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        LocalDateTime ahora = LocalDateTime.now();
        for (Map.Entry<Long, double[]> entrada : aReversarPorCuota.entrySet()) {
            Long idCuota = entrada.getKey();
            double[] valores = entrada.getValue();
            Long idPago = primerPagoPorCuota.get(idCuota);
            Prestamo prestamo = prestamoPorCuota.get(idCuota);
            if (valores[0] > TOLERANCIA) {
                guardarMovimiento(idCuota, prestamo, TipoMovimientoInteresCuota.REVERSO_POR_COBRO,
                    ComponenteMovimientoInteresCuota.INTERES, valores[0], fecha, null, idPago, origen, idOrigen,
                    asiento.getCodigo(), usuario, ahora);
            }
            if (valores[1] > TOLERANCIA) {
                guardarMovimiento(idCuota, prestamo, TipoMovimientoInteresCuota.REVERSO_POR_COBRO,
                    ComponenteMovimientoInteresCuota.MORA, valores[1], fecha, null, idPago, origen, idOrigen,
                    asiento.getCodigo(), usuario, ahora);
            }
        }

        resultado.setIdAsiento(asiento.getCodigo());
        resultado.setTotalReversado(redondear(totalGeneral));
        return resultado;
    }

    @Override
    public ResultadoReversoProvision reProvisionarPorAnulacion(List<PagoPrestamo> pagosAnulados, Long idEmpresa,
            LocalDate fecha, String origen, Long idOrigen, String usuario) throws Throwable {
        System.out.println("ProvisionInteresServiceImpl.reProvisionarPorAnulacion - origen: " + origen
            + " - idOrigen: " + idOrigen + " - pagos: " + (pagosAnulados != null ? pagosAnulados.size() : 0));
        ResultadoReversoProvision resultado = new ResultadoReversoProvision();
        if (pagosAnulados == null || pagosAnulados.isEmpty()) {
            return resultado;
        }
        List<Long> idsPago = new ArrayList<>();
        Map<Long, Long> tipoPrestamoPorCuota = new HashMap<>();
        for (PagoPrestamo pago : pagosAnulados) {
            if (pago == null || pago.getCodigo() == null) {
                continue;
            }
            idsPago.add(pago.getCodigo());
            if (pago.getDetallePrestamo() != null && pago.getPrestamo() != null
                    && pago.getPrestamo().getProducto() != null
                    && pago.getPrestamo().getProducto().getTipoPrestamo() != null) {
                tipoPrestamoPorCuota.putIfAbsent(pago.getDetallePrestamo().getCodigo(),
                    pago.getPrestamo().getProducto().getTipoPrestamo().getCodigo());
            }
        }
        if (idsPago.isEmpty()) {
            return resultado;
        }

        List<MovimientoInteresCuota> reversosVigentes = movimientoInteresCuotaDaoService
            .selectVigentesPorPagosYTipos(idsPago,
                java.util.Collections.singletonList(TipoMovimientoInteresCuota.REVERSO_POR_COBRO));
        if (reversosVigentes.isEmpty()) {
            return resultado;
        }

        Map<Long, Double> totalPorTipo = new LinkedHashMap<>();
        double totalGeneral = 0.0;
        for (MovimientoInteresCuota reverso : reversosVigentes) {
            Long idTipoPrestamo = tipoPrestamoPorCuota.get(reverso.getIdCuota());
            if (idTipoPrestamo == null) {
                throw new IncomeException("La cuota " + reverso.getIdCuota() + " tiene un reverso de provisión"
                    + " (MVIC " + reverso.getCodigo() + ") que compensar, pero no se encontró el tipo de"
                    + " préstamo entre los pagos anulados.");
            }
            totalPorTipo.merge(idTipoPrestamo, reverso.getValor(), Double::sum);
            totalGeneral += reverso.getValor();
        }

        Long idPlantilla = resuelvePlantilla36(idEmpresa);
        List<DetalleAsiento> lineas = new ArrayList<>();
        for (Map.Entry<Long, Double> entrada : totalPorTipo.entrySet()) {
            // Espejo de la provisión original (igual que el paso ⑦): 80 (470510) al DEBE y
            // 81 (149905) al HABER.
            lineas.add(lineaProvision(idPlantilla, CrdLineaAsiento.PROVISION_INTERESES_GASTO, entrada.getKey(),
                entrada.getValue(), true, "Re-provisión por anulación - " + origen));
            lineas.add(lineaProvision(idPlantilla, CrdLineaAsiento.PROVISION_INTERESES, entrada.getKey(),
                entrada.getValue(), false, "Re-provisión por anulación - " + origen));
        }

        Asiento asiento = asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS, fecha,
            "Re-provisión de intereses por anulación - " + origen, usuario, lineas,
            Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        LocalDateTime ahora = LocalDateTime.now();
        for (MovimientoInteresCuota reverso : reversosVigentes) {
            guardarReProvision(reverso, asiento.getCodigo(), origen, idOrigen, usuario, ahora, fecha);
        }

        resultado.setIdAsiento(asiento.getCodigo());
        resultado.setTotalReversado(redondear(totalGeneral));
        return resultado;
    }

    @Override
    public ResultadoReversoProvision condonarProvision(List<DetallePrestamo> cuotas, double interesCondonado,
            double moraCondonada, Long idEmpresa, LocalDate fecha, String origen, Long idOrigen, String usuario)
            throws Throwable {
        System.out.println("ProvisionInteresServiceImpl.condonarProvision - origen: " + origen
            + " - idOrigen: " + idOrigen + " - interés: " + interesCondonado + " - mora: " + moraCondonada);
        ResultadoReversoProvision resultado = new ResultadoReversoProvision();
        double interesRestante = Math.max(0.0, redondear(interesCondonado));
        double moraRestante = Math.max(0.0, redondear(moraCondonada));
        if (cuotas == null || cuotas.isEmpty() || (interesRestante <= TOLERANCIA && moraRestante <= TOLERANCIA)) {
            return resultado;
        }

        List<Long> idsCuota = new ArrayList<>();
        for (DetallePrestamo cuota : cuotas) {
            idsCuota.add(cuota.getCodigo());
        }
        Map<Long, double[]> saldoProvisionado = saldoProvisionadoPorCuotas(idsCuota);

        Prestamo prestamo = cuotas.get(0).getPrestamo();
        Long idTipoPrestamo = prestamo != null && prestamo.getProducto() != null
                && prestamo.getProducto().getTipoPrestamo() != null
                ? prestamo.getProducto().getTipoPrestamo().getCodigo() : null;

        LocalDateTime ahora = LocalDateTime.now();
        double totalCondonadoEscrito = 0.0;
        Map<Long, double[]> aCondonarPorCuota = new LinkedHashMap<>();
        for (DetallePrestamo cuota : cuotas) {
            if (interesRestante <= TOLERANCIA && moraRestante <= TOLERANCIA) {
                break;
            }
            double[] provisionado = saldoProvisionado.getOrDefault(cuota.getCodigo(), new double[2]);
            double interesCuota = Math.max(0.0, redondear(Math.min(interesRestante, provisionado[0])));
            double moraCuota = Math.max(0.0, redondear(Math.min(moraRestante, provisionado[1])));
            if (interesCuota <= TOLERANCIA && moraCuota <= TOLERANCIA) {
                continue;
            }
            aCondonarPorCuota.put(cuota.getCodigo(), new double[]{interesCuota, moraCuota});
            interesRestante = redondear(interesRestante - interesCuota);
            moraRestante = redondear(moraRestante - moraCuota);
            totalCondonadoEscrito += interesCuota + moraCuota;
        }
        if (aCondonarPorCuota.isEmpty()) {
            return resultado;
        }
        if (idTipoPrestamo == null) {
            throw new IncomeException("El préstamo " + (prestamo != null ? prestamo.getCodigo() : null)
                + " tiene provisión que condonar pero su producto no tiene tipo de préstamo asignado; no se"
                + " puede resolver la cuenta contable de la condonación.");
        }

        Long idPlantilla = resuelvePlantilla36(idEmpresa);
        double totalRedondeado = redondear(totalCondonadoEscrito);
        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.add(lineaProvision(idPlantilla, CrdLineaAsiento.PROVISION_INTERESES, idTipoPrestamo,
            totalRedondeado, true, "Reverso de provisión por condonación - " + origen));
        lineas.add(lineaProvision(idPlantilla, CrdLineaAsiento.PROVISION_INTERESES_GASTO, idTipoPrestamo,
            totalRedondeado, false, "Reverso de provisión por condonación - " + origen));

        Asiento asiento = asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS, fecha,
            "Reverso de provisión de intereses por condonación - " + origen, usuario, lineas,
            Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        for (Map.Entry<Long, double[]> entrada : aCondonarPorCuota.entrySet()) {
            Long idCuota = entrada.getKey();
            double[] valores = entrada.getValue();
            if (valores[0] > TOLERANCIA) {
                guardarMovimiento(idCuota, prestamo, TipoMovimientoInteresCuota.REVERSO_POR_CONDONACION,
                    ComponenteMovimientoInteresCuota.INTERES, valores[0], fecha, null, null, origen, idOrigen,
                    asiento.getCodigo(), usuario, ahora);
            }
            if (valores[1] > TOLERANCIA) {
                guardarMovimiento(idCuota, prestamo, TipoMovimientoInteresCuota.REVERSO_POR_CONDONACION,
                    ComponenteMovimientoInteresCuota.MORA, valores[1], fecha, null, null, origen, idOrigen,
                    asiento.getCodigo(), usuario, ahora);
            }
        }

        resultado.setIdAsiento(asiento.getCodigo());
        resultado.setTotalReversado(totalRedondeado);
        return resultado;
    }

    @Override
    public ResultadoReversoProvision reversarExcesoProvisionPorCobroTardio(Long idPrestamo,
            List<Object[]> detalleMora, Long idEmpresa, LocalDate fecha, String origen, Long idOrigen,
            String usuario) throws Throwable {
        System.out.println("ProvisionInteresServiceImpl.reversarExcesoProvisionPorCobroTardio - préstamo: "
            + idPrestamo + " - origen: " + origen + " - idOrigen: " + idOrigen);
        ResultadoReversoProvision resultado = new ResultadoReversoProvision();
        if (detalleMora == null || detalleMora.isEmpty()) {
            return resultado;
        }

        Map<Long, Double> moraNuevaPorCuota = new LinkedHashMap<>();
        List<Long> idsCuota = new ArrayList<>();
        for (Object[] fila : detalleMora) {
            Long idCuota = (Long) fila[0];
            double moraNueva = (Double) fila[2];
            moraNuevaPorCuota.put(idCuota, moraNueva);
            idsCuota.add(idCuota);
        }

        // Corrección del árbitro (2026-10-05): el tope no es el exceso (moraAnterior −
        // moraNueva) — eso ignora que moraNueva puede seguir siendo mora LEGÍTIMA, todavía por
        // cobrar. El saldo provisionado tiene que quedar en lo que de verdad se debe a la fecha
        // de pago, ni un centavo menos: pendiente = max(0, moraNueva − moraPagada ANTES de este
        // cobro); lo que sobra del saldo provisionado sobre ese pendiente es lo que se reversa.
        // El tipo 2 de este mismo cobro reversa, aparte, lo que SÍ se cobre de ese pendiente —
        // nunca los dos a la vez sobre el mismo dólar.
        Map<Long, double[]> saldoProvisionado = saldoProvisionadoPorCuotas(idsCuota);
        Map<Long, double[]> pagosPorCuota = cargarPagosPorCuota(idsCuota);
        Map<Long, Double> aReversarPorCuota = new LinkedHashMap<>();
        double totalGeneral = 0.0;
        for (Map.Entry<Long, Double> entrada : moraNuevaPorCuota.entrySet()) {
            Long idCuota = entrada.getKey();
            double moraNueva = entrada.getValue();
            double[] pagos = pagosPorCuota.get(idCuota);
            double moraPagada = pagos != null ? pagos[1] : 0.0;
            double pendiente = Math.max(0.0, redondear(moraNueva - moraPagada));
            double[] provisionado = saldoProvisionado.getOrDefault(idCuota, new double[2]);
            double saldoProvisionadoMora = provisionado[1];
            double aReversar = Math.max(0.0, redondear(saldoProvisionadoMora - pendiente));
            if (aReversar <= TOLERANCIA) {
                continue;
            }
            aReversarPorCuota.put(idCuota, aReversar);
            totalGeneral += aReversar;
        }
        if (aReversarPorCuota.isEmpty()) {
            return resultado;
        }

        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
        Long idTipoPrestamo = prestamo != null && prestamo.getProducto() != null
                && prestamo.getProducto().getTipoPrestamo() != null
                ? prestamo.getProducto().getTipoPrestamo().getCodigo() : null;
        if (idTipoPrestamo == null) {
            throw new IncomeException("El préstamo " + idPrestamo + " tiene exceso de mora provisionada que"
                + " reversar por cobro tardío, pero su producto no tiene tipo de préstamo asignado.");
        }

        Long idPlantilla = resuelvePlantilla36(idEmpresa);
        double totalRedondeado = redondear(totalGeneral);
        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.add(lineaProvision(idPlantilla, CrdLineaAsiento.PROVISION_INTERESES, idTipoPrestamo,
            totalRedondeado, true, "Reverso de provisión por cobro tardío - " + origen));
        lineas.add(lineaProvision(idPlantilla, CrdLineaAsiento.PROVISION_INTERESES_GASTO, idTipoPrestamo,
            totalRedondeado, false, "Reverso de provisión por cobro tardío - " + origen));

        Asiento asiento = asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS, fecha,
            "Reverso de provisión de intereses por cobro tardío - " + origen, usuario, lineas,
            Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        LocalDateTime ahora = LocalDateTime.now();
        for (Map.Entry<Long, Double> entrada : aReversarPorCuota.entrySet()) {
            guardarMovimiento(entrada.getKey(), prestamo, TipoMovimientoInteresCuota.REVERSO_POR_COBRO_TARDIO,
                ComponenteMovimientoInteresCuota.MORA, entrada.getValue(), fecha, null, null, origen, idOrigen,
                asiento.getCodigo(), usuario, ahora);
        }

        resultado.setIdAsiento(asiento.getCodigo());
        resultado.setTotalReversado(totalRedondeado);
        return resultado;
    }

    /**
     * ÍTEM T — tope de transición para una cuota sin ningún {@code MVIC} tipo 5/6 (su devengo,
     * si lo hubo, se hizo antes de que existiera el libro). Reconstruye con la fórmula pura,
     * {@code calcularMoraCuota}, contra la corrida EJECUTADA que abrió el mes del vencimiento de
     * la cuota — SOLO si esa corrida tiene 0 filas de MVIC (si tiene alguna, ya es de la era del
     * libro, y la ausencia de fila para ESTA cuota es un dato real, no una laguna de
     * transición: el tope queda en 0). Sin corrida, el tope es 0.
     *
     * <p>{@code mora pagada} se acota a {@code PGPRFCHA <= corrida.fechaRegistro} (el día REAL
     * en que esa corrida corrió, {@code CRCTFCRG} — NUNCA {@code fechaProceso}, que es solo el
     * día 1 calendario del mes y siempre da 0 días de mora para cualquier cuota del universo de
     * ④; aprobado por el árbitro 2026-10-05 tras descartar esa primera propuesta).</p>
     *
     * <p>Regla vigente SOLO para la transición: desde el cierre de septiembre de 2026 en
     * adelante, toda corrida ya escribe su propio MVIC tipo 5 y este método nunca se alcanza
     * para ellas (siempre habrá entrada en {@code saldoDevengado}).</p>
     */
    private double topeTransicionDevengo(Long idCuota, Long idEmpresa, Prestamo prestamoDelExceso)
            throws Throwable {
        DetallePrestamo cuota = detallePrestamoDaoService.find(new DetallePrestamo(), idCuota);
        if (cuota == null || cuota.getFechaVencimiento() == null) {
            return 0.0;
        }
        LocalDate vencimiento = cuota.getFechaVencimiento().toLocalDate();
        LocalDate fechaProcesoCandidata = vencimiento.withDayOfMonth(1);
        CorridaCierreCartera corrida = corridaCierreCarteraDaoService.selectEjecutadaByFechaProceso(idEmpresa,
            fechaProcesoCandidata);
        if (corrida == null || corrida.getFechaRegistro() == null) {
            return 0.0;
        }
        if (movimientoInteresCuotaDaoService.existeAlgunoByCorrida(corrida.getCodigo())) {
            return 0.0;
        }

        Prestamo prestamo = prestamoDelExceso != null ? prestamoDelExceso : cuota.getPrestamo();
        double tasaDiaria = procesoMoraPrestamoService.tasaDiariaDelPrestamo(prestamo, false);
        LocalDate fechaRealEjecucion = corrida.getFechaRegistro().toLocalDate();
        double moraCalculada = procesoMoraPrestamoService.calcularMoraCuota(cuota, tasaDiaria, fechaRealEjecucion);

        double moraPagadaHastaEsaFecha = 0.0;
        for (PagoPrestamo pago : pagoPrestamoDaoService.selectVigentesByIdDetallePrestamo(idCuota)) {
            if (pago.getFecha() != null && !pago.getFecha().toLocalDate().isAfter(fechaRealEjecucion)) {
                moraPagadaHastaEsaFecha += nvl(pago.getMoraPagada());
            }
        }
        return Math.max(0.0, redondear(moraCalculada - moraPagadaHastaEsaFecha));
    }

    @Override
    public ResultadoReversoProvision reversarExcesoDevengoPorCobroTardio(Long idPrestamo,
            List<Object[]> detalleMora, Long idEmpresa, LocalDate fecha, String origen, Long idOrigen,
            String usuario) throws Throwable {
        System.out.println("ProvisionInteresServiceImpl.reversarExcesoDevengoPorCobroTardio - préstamo: "
            + idPrestamo + " - origen: " + origen + " - idOrigen: " + idOrigen);
        ResultadoReversoProvision resultado = new ResultadoReversoProvision();
        if (detalleMora == null || detalleMora.isEmpty()) {
            return resultado;
        }

        Map<Long, Double> moraNuevaPorCuota = new LinkedHashMap<>();
        List<Long> idsCuota = new ArrayList<>();
        for (Object[] fila : detalleMora) {
            Long idCuota = (Long) fila[0];
            double moraNueva = (Double) fila[2];
            moraNuevaPorCuota.put(idCuota, moraNueva);
            idsCuota.add(idCuota);
        }

        Map<Long, Double> saldoDevengado = movimientoInteresCuotaDaoService.selectSaldoDevengadoPorCuotas(idsCuota);
        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
        Long idTipoPrestamo = prestamo != null && prestamo.getProducto() != null
                && prestamo.getProducto().getTipoPrestamo() != null
                ? prestamo.getProducto().getTipoPrestamo().getCodigo() : null;

        // Corrección del árbitro (2026-10-05): el devengo es INGRESO GANADO, esté cobrado o no
        // — la mora pagada NO se resta acá (eso lo hace el tipo 2 de este mismo cobro, sobre la
        // parte que de verdad se cobra). Lo único que puede exceder el devengo es que la mora
        // recalculada a la fecha de pago (moraNueva) haya quedado por DEBAJO de lo devengado:
        // ese exceso de ingreso ya reconocido es lo único que se reversa.
        Map<Long, Double> aReversarPorCuota = new LinkedHashMap<>();
        double totalGeneral = 0.0;
        for (Map.Entry<Long, Double> entrada : moraNuevaPorCuota.entrySet()) {
            Long idCuota = entrada.getKey();
            double moraNueva = entrada.getValue();
            double devengado;
            if (saldoDevengado.containsKey(idCuota)) {
                devengado = nvl(saldoDevengado.get(idCuota));
            } else {
                // ÍTEM T, transición (aprobado por el árbitro 2026-10-05, caso real 67023/89):
                // esta cuota no tiene NINGÚN MVIC tipo 5/6 — su devengo, si lo hubo, se hizo
                // antes de que existiera el libro. El devengado sale de reconstruir la fórmula
                // pura contra la corrida EJECUTADA que abrió el mes de su vencimiento, siempre
                // que esa corrida tenga 0 filas de MVIC (si tiene alguna, ya es de la era del
                // libro y la ausencia de fila para ESTA cuota es real, no una laguna de transición).
                devengado = topeTransicionDevengo(idCuota, idEmpresa, prestamo);
            }
            double aReversar = Math.max(0.0, redondear(devengado - moraNueva));
            if (aReversar <= TOLERANCIA) {
                continue;
            }
            aReversarPorCuota.put(idCuota, aReversar);
            totalGeneral += aReversar;
        }
        if (aReversarPorCuota.isEmpty()) {
            return resultado;
        }

        if (idTipoPrestamo == null) {
            throw new IncomeException("El préstamo " + idPrestamo + " tiene exceso de mora devengada que"
                + " reversar por cobro tardío, pero su producto no tiene tipo de préstamo asignado.");
        }

        Long idPlantilla17 = plantillaService.codigoByAlterno(PlantillasCredito.DEVENGO_INTERESES, idEmpresa);
        if (idPlantilla17 == null || idPlantilla17.longValue() == 0L) {
            throw new IncomeException("No existe la plantilla contable alterno " + PlantillasCredito.DEVENGO_INTERESES
                + " (devengo de intereses) para la empresa " + idEmpresa + ".");
        }
        double totalRedondeado = redondear(totalGeneral);
        List<DetalleAsiento> lineas = new ArrayList<>();
        // Espejo de armaDevengoIntereses: allá INTERES_MORA_POR_COBRAR es Debe e
        // INGRESO_INTERES_MORA es Haber (devengo); acá, invertido (reverso).
        lineas.add(lineaDesdePlantillaPorTipo(idPlantilla17, CrdLineaAsiento.INGRESO_INTERES_MORA, idTipoPrestamo,
            totalRedondeado, true, "Reverso de devengo de mora por cobro tardío - " + origen,
            PlantillasCredito.DEVENGO_INTERESES));
        lineas.add(lineaDesdePlantillaPorTipo(idPlantilla17, CrdLineaAsiento.INTERES_MORA_POR_COBRAR, idTipoPrestamo,
            totalRedondeado, false, "Reverso de devengo de mora por cobro tardío - " + origen,
            PlantillasCredito.DEVENGO_INTERESES));

        Asiento asiento = asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS, fecha,
            "Reverso de devengo de mora por cobro tardío - " + origen, usuario, lineas,
            Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        LocalDateTime ahora = LocalDateTime.now();
        for (Map.Entry<Long, Double> entrada : aReversarPorCuota.entrySet()) {
            guardarMovimiento(entrada.getKey(), prestamo, TipoMovimientoInteresCuota.REVERSO_DEVENGO_POR_COBRO_TARDIO,
                ComponenteMovimientoInteresCuota.MORA, entrada.getValue(), fecha, null, null, origen, idOrigen,
                asiento.getCodigo(), usuario, ahora);
        }

        resultado.setIdAsiento(asiento.getCodigo());
        resultado.setTotalReversado(totalRedondeado);
        return resultado;
    }

    @Override
    public ResultadoReversoProvision registrarReclasificacionBandaPorCobroTardio(
            List<ItemReclasificacionBanda> items, Long idEmpresa, LocalDate fecha, String origen, Long idOrigen,
            String usuario) throws Throwable {
        System.out.println("ProvisionInteresServiceImpl.registrarReclasificacionBandaPorCobroTardio - origen: "
            + origen + " - idOrigen: " + idOrigen + " - items: " + (items != null ? items.size() : 0));
        ResultadoReversoProvision resultado = new ResultadoReversoProvision();
        if (items == null || items.isEmpty()) {
            return resultado;
        }

        Map<Long, AcumuladoBanda> acumuladoNueva = new LinkedHashMap<>();
        Map<Long, AcumuladoBanda> acumuladoVieja = new LinkedHashMap<>();
        List<ItemReclasificacionBanda> aEscribir = new ArrayList<>();
        double totalGeneral = 0.0;

        for (ItemReclasificacionBanda item : items) {
            if (item == null || item.getBandaUltimoCierre() == null || item.getBandaFechaPago() == null
                    || item.getBandaUltimoCierre().getBanda() == null || item.getBandaFechaPago().getBanda() == null) {
                continue;
            }
            BandaProductoDetalle bandaVieja = item.getBandaUltimoCierre().getBanda();
            BandaProductoDetalle bandaNueva = item.getBandaFechaPago().getBanda();
            if (bandaVieja.getNumero() != null && bandaVieja.getNumero().equals(bandaNueva.getNumero())) {
                continue;
            }
            double capital = redondear(item.getCapital());
            if (capital <= TOLERANCIA) {
                continue;
            }
            if (bandaVieja.getIdPlanCuenta() == null || bandaNueva.getIdPlanCuenta() == null) {
                throw new IncomeException("La cuota " + item.getIdCuota() + " (préstamo " + item.getIdPrestamo()
                    + ") necesita reclasificarse de banda por cobro tardío, pero una de las dos bandas no"
                    + " tiene cuenta contable asignada en CRD.BNDP (vieja=" + bandaVieja.getNumero()
                    + ", nueva=" + bandaNueva.getNumero() + ").");
            }

            acumuladoNueva.computeIfAbsent(bandaNueva.getIdPlanCuenta(), k -> new AcumuladoBanda(bandaNueva))
                .valor += capital;
            acumuladoVieja.computeIfAbsent(bandaVieja.getIdPlanCuenta(), k -> new AcumuladoBanda(bandaVieja))
                .valor += capital;
            aEscribir.add(item);
            totalGeneral += capital;
        }
        if (aEscribir.isEmpty()) {
            return resultado;
        }

        List<DetalleAsiento> lineas = new ArrayList<>();
        for (AcumuladoBanda acumulado : acumuladoNueva.values()) {
            lineas.add(lineaBanda(acumulado, true,
                "Reclasificación de banda por cobro tardío - " + origen));
        }
        for (AcumuladoBanda acumulado : acumuladoVieja.values()) {
            lineas.add(lineaBanda(acumulado, false,
                "Reclasificación de banda por cobro tardío - " + origen));
        }

        Asiento asiento = asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS, fecha,
            "Reclasificación de banda por cobro tardío - " + origen, usuario, lineas,
            Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        LocalDateTime ahora = LocalDateTime.now();
        for (ItemReclasificacionBanda item : aEscribir) {
            Prestamo prestamo = new Prestamo();
            prestamo.setCodigo(item.getIdPrestamo());
            MovimientoInteresCuota mov = new MovimientoInteresCuota();
            mov.setPrestamo(prestamo);
            mov.setIdCuota(item.getIdCuota());
            mov.setTipoMovimiento(TipoMovimientoInteresCuota.RECLASIFICACION_POR_COBRO_TARDIO);
            mov.setComponente(ComponenteMovimientoInteresCuota.CAPITAL);
            mov.setValor(redondear(item.getCapital()));
            mov.setFechaContable(fecha);
            mov.setOrigen(origen);
            mov.setIdOrigen(idOrigen);
            mov.setAsiento(asiento.getCodigo());
            mov.setTipoCartera(item.getBandaFechaPago().getTipoCartera());
            mov.setIdBanda(item.getBandaFechaPago().getBanda().getIdBanda());
            mov.setAnulado(0L);
            mov.setUsuarioRegistro(usuario);
            mov.setFechaRegistro(ahora);
            movimientoInteresCuotaDaoService.save(mov, null);
        }

        resultado.setIdAsiento(asiento.getCodigo());
        resultado.setTotalReversado(redondear(totalGeneral));
        return resultado;
    }

    private DetalleAsiento lineaBanda(AcumuladoBanda acumulado, boolean esDebe, String descripcion) {
        DetalleAsiento detalle = new DetalleAsiento();
        detalle.setPlanCuenta(acumulado.planCuenta);
        detalle.setNumeroCuenta(acumulado.banda.getCuentaContable());
        detalle.setNombreCuenta(acumulado.banda.getNombreCuenta());
        detalle.setDescripcion(descripcion + " - banda " + acumulado.banda.getNumero());
        double valor = redondear(acumulado.valor);
        detalle.setValorDebe(esDebe ? valor : 0.0);
        detalle.setValorHaber(esDebe ? 0.0 : valor);
        return detalle;
    }

    /** Acumulador de capital por cuenta de banda, con la cuenta real resuelta UNA sola vez. */
    private final class AcumuladoBanda {
        private final BandaProductoDetalle banda;
        private final PlanCuenta planCuenta;
        private double valor;

        AcumuladoBanda(BandaProductoDetalle banda) {
            this.banda = banda;
            try {
                this.planCuenta = planCuentaDaoService.selectById(banda.getIdPlanCuenta(),
                    com.saa.model.cnt.NombreEntidadesContabilidad.PLAN_CUENTA);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Long resuelvePlantilla36(Long idEmpresa) throws Throwable {
        Long idPlantilla = plantillaService.codigoByAlterno(PlantillasCredito.PROVISION_INTERESES, idEmpresa);
        if (idPlantilla == null || idPlantilla.longValue() == 0L) {
            throw new IncomeException(ProvisionInteresService.ERR_PLANTILLA_PROVISION_NO_CONFIGURADA
                + ": no existe la plantilla contable alterno " + PlantillasCredito.PROVISION_INTERESES
                + " (provisión de intereses) para la empresa " + idEmpresa + ".");
        }
        return idPlantilla;
    }

    private DetalleAsiento lineaProvision(Long idPlantilla, int papel, Long idTipoPrestamo, double valor,
            boolean esDebe, String descripcion) throws Throwable {
        return lineaDesdePlantillaPorTipo(idPlantilla, papel, idTipoPrestamo, valor, esDebe, descripcion,
            PlantillasCredito.PROVISION_INTERESES);
    }

    /**
     * Igual que {@link #lineaProvision} pero para cualquier plantilla/papel con dimensión de
     * tipo de préstamo — ÍTEM 5 (tipo 6) reutiliza esto contra la plantilla 17 (DEVENGO_INTERESES),
     * papeles {@code INTERES_MORA_POR_COBRAR}/{@code INGRESO_INTERES_MORA}, en vez de la 36.
     */
    private DetalleAsiento lineaDesdePlantillaPorTipo(Long idPlantilla, int papel, Long idTipoPrestamo,
            double valor, boolean esDebe, String descripcion, int alternoParaError) throws Throwable {
        DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliares(idPlantilla, papel,
            idTipoPrestamo.intValue());
        if (linea == null || linea.getPlanCuenta() == null) {
            throw new IncomeException(ProvisionInteresService.ERR_PLANTILLA_PROVISION_NO_CONFIGURADA
                + ": la plantilla alterno " + alternoParaError + " no tiene la línea " + papel
                + " para el tipo de préstamo " + idTipoPrestamo + ".");
        }
        PlanCuenta cuenta = linea.getPlanCuenta();
        DetalleAsiento detalle = new DetalleAsiento();
        detalle.setPlanCuenta(cuenta);
        detalle.setNumeroCuenta(cuenta.getCuentaContable());
        detalle.setNombreCuenta(cuenta.getNombre());
        detalle.setDescripcion(descripcion);
        detalle.setValorDebe(esDebe ? redondear(valor) : 0.0);
        detalle.setValorHaber(esDebe ? 0.0 : redondear(valor));
        return detalle;
    }

    private void guardarReProvision(MovimientoInteresCuota reverso, Long idAsiento, String origen, Long idOrigen,
            String usuario, LocalDateTime ahora, LocalDate fechaContable) throws Throwable {
        MovimientoInteresCuota mov = new MovimientoInteresCuota();
        mov.setPrestamo(reverso.getPrestamo());
        mov.setIdCuota(reverso.getIdCuota());
        mov.setTipoMovimiento(TipoMovimientoInteresCuota.RE_PROVISION);
        mov.setComponente(reverso.getComponente());
        mov.setValor(reverso.getValor());
        mov.setFechaContable(fechaContable);
        mov.setPago(reverso.getPago());
        mov.setOrigen(origen);
        mov.setIdOrigen(idOrigen);
        mov.setAsiento(idAsiento);
        mov.setMovimientoReversado(reverso);
        mov.setAnulado(0L);
        mov.setUsuarioRegistro(usuario);
        mov.setFechaRegistro(ahora);
        movimientoInteresCuotaDaoService.save(mov, null);
    }

    /**
     * El cálculo compartido entre {@code calcularProvisionPorTipoPrestamo} (totales para la
     * línea del asiento) y {@code registrarProvisionCierre} (el detalle por cuota para MVIC) —
     * SIEMPRE se recalcula entero, nunca se confía en un cálculo previo (mismo criterio que el
     * resto del cierre de cartera con "no confía en la vista previa").
     */
    private List<ItemProvisionCuota> calcularItems(LocalDate fechaCorte) throws Throwable {
        if (fechaCorte == null) {
            throw new IncomeException("fechaCorte es obligatoria para el paso 7 (provisión de intereses)");
        }
        List<DetallePrestamo> cuotas = detallePrestamoDaoService.selectCuotasProvisionables(
            fechaCorte.atTime(23, 59, 59));

        List<Long> idsCuota = new ArrayList<>();
        for (DetallePrestamo cuota : cuotas) {
            idsCuota.add(cuota.getCodigo());
        }
        Map<Long, double[]> pagosPorCuota = cargarPagosPorCuota(idsCuota);
        Map<Long, double[]> saldoProvisionado = saldoProvisionadoPorCuotas(idsCuota);

        List<ItemProvisionCuota> items = new ArrayList<>();
        for (DetallePrestamo cuota : cuotas) {
            Prestamo prestamo = cuota.getPrestamo();
            double[] pagos = pagosPorCuota.get(cuota.getCodigo());
            double interesPagado = pagos != null ? pagos[3] : 0.0;
            double ivPagado = pagos != null ? pagos[2] : 0.0;
            double moraPagada = pagos != null ? pagos[1] : 0.0;

            double pendienteInteres = Math.max(0.0, redondear(
                nvl(cuota.getInteres()) + nvl(cuota.getInteresVencido()) - interesPagado - ivPagado));

            double tasaDiaria = procesoMoraPrestamoService.tasaDiariaDelPrestamo(prestamo, false);
            double moraCalculada = procesoMoraPrestamoService.calcularMoraCuota(cuota, tasaDiaria, fechaCorte);
            double pendienteMora = Math.max(0.0, redondear(moraCalculada - moraPagada));

            double[] provisionado = saldoProvisionado.get(cuota.getCodigo());
            double saldoInteres = provisionado != null ? provisionado[0] : 0.0;
            double saldoMora = provisionado != null ? provisionado[1] : 0.0;

            double aProvisionarInteres = Math.max(0.0, redondear(pendienteInteres - saldoInteres));
            double aProvisionarMora = Math.max(0.0, redondear(pendienteMora - saldoMora));

            if (aProvisionarInteres <= TOLERANCIA && aProvisionarMora <= TOLERANCIA) {
                continue;
            }

            Long idTipoPrestamo = prestamo.getProducto() != null && prestamo.getProducto().getTipoPrestamo() != null
                ? prestamo.getProducto().getTipoPrestamo().getCodigo() : null;
            if (idTipoPrestamo == null) {
                throw new IncomeException("El préstamo " + prestamo.getCodigo() + " (cuota " + cuota.getCodigo()
                    + ") tiene intereses pendientes de provisionar pero su producto no tiene tipo de préstamo"
                    + " asignado; no se puede resolver la cuenta contable del paso 7.");
            }

            ItemProvisionCuota item = new ItemProvisionCuota();
            item.cuota = cuota;
            item.prestamo = prestamo;
            item.idTipoPrestamo = idTipoPrestamo;
            item.aProvisionarInteres = aProvisionarInteres;
            item.aProvisionarMora = aProvisionarMora;
            items.add(item);
        }
        return items;
    }

    private void guardarMovimiento(Long idCuota, Prestamo prestamo, long tipoMovimiento, long componente,
            double valor, LocalDate fechaContable, Long idCorrida, Long idPago, String origen, Long idOrigen,
            Long idAsiento, String usuario, LocalDateTime ahora) throws Throwable {
        MovimientoInteresCuota mov = new MovimientoInteresCuota();
        mov.setPrestamo(prestamo);
        mov.setIdCuota(idCuota);
        mov.setTipoMovimiento(tipoMovimiento);
        mov.setComponente(componente);
        mov.setValor(redondear(valor));
        mov.setFechaContable(fechaContable);
        if (idCorrida != null) {
            com.saa.model.crd.CorridaCierreCartera corrida = new com.saa.model.crd.CorridaCierreCartera();
            corrida.setCodigo(idCorrida);
            mov.setCorrida(corrida);
        }
        if (idPago != null) {
            com.saa.model.crd.PagoPrestamo pago = new com.saa.model.crd.PagoPrestamo();
            pago.setCodigo(idPago);
            mov.setPago(pago);
        }
        mov.setOrigen(origen);
        mov.setIdOrigen(idOrigen);
        mov.setAsiento(idAsiento);
        mov.setAnulado(0L);
        mov.setUsuarioRegistro(usuario);
        mov.setFechaRegistro(ahora);
        movimientoInteresCuotaDaoService.save(mov, null);
    }

    /**
     * Índices del arreglo: 0 desgravamen, 1 moraPagada, 2 interesVencidoPagado, 3 interesPagado,
     * 4 capitalPagado, 5 valorSeguroIncendio, 6 saldoOtros ya filtrado por tipo de pago — mismo
     * orden que {@code PagoPrestamoDaoService#selectDatosPagosVigentes} desplazado -1, mismo
     * patrón que {@code DeclaracionPlazoVencidoServiceImpl}/{@code DocumentoSeguroServiceImpl}.
     */
    private Map<Long, double[]> cargarPagosPorCuota(List<Long> idsCuota) throws Throwable {
        Map<Long, double[]> mapa = new HashMap<>();
        if (idsCuota == null || idsCuota.isEmpty()) {
            return mapa;
        }
        List<Object[]> filas = pagoPrestamoDaoService.selectDatosPagosVigentes(idsCuota);
        if (filas == null) {
            return mapa;
        }
        for (Object[] fila : filas) {
            Long idCuota = (Long) fila[0];
            double[] acumulado = mapa.computeIfAbsent(idCuota, k -> new double[7]);
            acumulado[0] += nvl((Double) fila[1]);
            acumulado[1] += nvl((Double) fila[2]);
            acumulado[2] += nvl((Double) fila[3]);
            acumulado[3] += nvl((Double) fila[4]);
            acumulado[4] += nvl((Double) fila[5]);
            acumulado[5] += nvl((Double) fila[6]);
            acumulado[6] += nvl((Double) fila[7]);
        }
        return mapa;
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** Dato intermedio del cálculo del paso ⑦; no se expone fuera del servicio. */
    private static class ItemProvisionCuota {
        DetallePrestamo cuota;
        Prestamo prestamo;
        Long idTipoPrestamo;
        double aProvisionarInteres;
        double aProvisionarMora;
    }
}

package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.ejb.crd.dao.CorridaCierreCarteraDaoService;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.MovimientoInteresCuotaDaoService;
import com.saa.ejb.crd.service.dto.ResultadoReversoDevengo;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.service.DevengoCompletoService;
import com.saa.ejb.crd.service.ModoCorteCartera;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.crd.CorridaCierreCartera;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.HistDetallePrestamo;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.CrdLineaAsiento;
import com.saa.rubros.EstadoCorridaCierreCartera;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.PlantillasCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación de {@link DevengoCompletoService} (C27b, D-172).
 */
@Stateless
public class DevengoCompletoServiceImpl implements DevengoCompletoService {

    private static final double MINIMO = 0.005;

    /** Lote 20: origen de los MVIC 12 de las cuotas canceladas por precancelación o acuerdo (idOrigen = evento) y de los del abono. */
    private static final String ORIGEN_CANCELACION = "CANCELACION_DEVENGO_INTERES";
    private static final String ORIGEN_ABONO = "ABONO_DEVENGO_INTERES";

    @EJB
    private CorridaCierreCarteraDaoService corridaCierreCarteraDaoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private MovimientoInteresCuotaDaoService movimientoInteresCuotaDaoService;

    @EJB
    private PlantillaService plantillaService;

    @EJB
    private DetallePlantillaDaoService detallePlantillaDaoService;

    @Override
    public LocalDate limiteDevengado(Long idEmpresa, LocalDate fechaOperacion) throws Throwable {
        System.out.println("Ingresa al metodo (limiteDevengado) DevengoCompletoService - empresa: " + idEmpresa
                + " - fecha de la operacion: " + fechaOperacion);
        if (idEmpresa == null || fechaOperacion == null || ModoCorteCartera.devengoCompletoDesde() == null) {
            return null;
        }
        LocalDate ultimoCorte = null;
        List<CorridaCierreCartera> corridas = corridaCierreCarteraDaoService.selectByEmpresa(idEmpresa);
        if (corridas != null) {
            for (CorridaCierreCartera corrida : corridas) {
                if (corrida.getIdEstado() != null && corrida.getIdEstado().longValue() == EstadoCorridaCierreCartera.EJECUTADA
                        && corrida.getFechaCorte() != null
                        // Lote 16: solo el cierre que ya existia al momento de la operacion (corte estrictamente anterior).
                        && corrida.getFechaCorte().isBefore(fechaOperacion)
                        && (ultimoCorte == null || corrida.getFechaCorte().isAfter(ultimoCorte))) {
                    ultimoCorte = corrida.getFechaCorte();
                }
            }
        }
        if (ultimoCorte == null || !ModoCorteCartera.devengoCompletoActivo(ultimoCorte)) {
            return null;
        }
        LocalDate mesSiguiente = ultimoCorte.withDayOfMonth(1).plusMonths(1);
        return mesSiguiente.withDayOfMonth(mesSiguiente.lengthOfMonth());
    }

    @Override
    public List<DetalleAsiento> lineasReversoAbono(Long idEmpresa, Prestamo prestamo,
            List<HistDetallePrestamo> historizadas, List<DetallePrestamo> nuevas, LocalDate fechaOperacion, String prefijo)
            throws Throwable {
        return calcularReversoAbono(idEmpresa, prestamo, historizadas, nuevas, fechaOperacion, prefijo, null).getLineas();
    }

    @Override
    public ResultadoReversoDevengo calcularReversoAbono(Long idEmpresa, Prestamo prestamo,
            List<HistDetallePrestamo> historizadas, List<DetallePrestamo> nuevas, LocalDate fechaOperacion, String prefijo,
            Long idEvento) throws Throwable {
        System.out.println("Ingresa al metodo (calcularReversoAbono) DevengoCompletoService - empresa: " + idEmpresa);
        ResultadoReversoDevengo resultado = new ResultadoReversoDevengo(prestamo, ORIGEN_ABONO, idEvento);
        LocalDate limite = limiteDevengado(idEmpresa, fechaOperacion);
        if (limite == null) {
            return resultado;
        }
        LocalDate primerMes = primerMesDevengado();
        double antes = 0.0;
        if (historizadas != null) {
            for (HistDetallePrestamo cuota : historizadas) {
                if (cuota.getFechaVencimiento() != null && !cuota.getFechaVencimiento().toLocalDate().isAfter(limite)
                        && !cuota.getFechaVencimiento().toLocalDate().isBefore(primerMes)) {
                    antes += cuota.getInteres() != null ? cuota.getInteres().doubleValue() : 0.0;
                }
            }
        }
        double despues = 0.0;
        if (nuevas != null) {
            for (DetallePrestamo cuota : nuevas) {
                if (cuota.getFechaVencimiento() != null && !cuota.getFechaVencimiento().toLocalDate().isAfter(limite)
                        && !cuota.getFechaVencimiento().toLocalDate().isBefore(primerMes)) {
                    despues += cuota.getInteres() != null ? cuota.getInteres().doubleValue() : 0.0;
                }
            }
        }
        double reverso = redondear(antes - despues);
        resultado.getLineas().addAll(lineasReverso(idEmpresa, prestamo, reverso, prefijo
                + " - reverso del devengo del interés que el abono eliminó (cuotas de meses ya devengados)"));
        if (resultado.getLineas().isEmpty()) {
            return resultado;
        }
        repartirReversoAbono(resultado, reverso, historizadas, nuevas, primerMes, limite);
        return resultado;
    }

    /**
     * Lote 20: reparte el reverso del abono por cuota. Regla: por cada número de cuota, interés historizado − interés nuevo cuando da
     * positivo (sobre las cuotas de meses ya devengados); lo de las cuotas que desaparecen va a la de número mayor que queda dentro del
     * límite. El total del libro se fuerza a ser el reverso del asiento, al centavo, ajustando la de número mayor.
     */
    private void repartirReversoAbono(ResultadoReversoDevengo resultado, double reverso, List<HistDetallePrestamo> historizadas,
            List<DetallePrestamo> nuevas, LocalDate primerMes, LocalDate limite) {
        java.util.TreeMap<Double, DetallePrestamo> nuevaPorNumero = new java.util.TreeMap<Double, DetallePrestamo>();
        java.util.TreeMap<Double, DetallePrestamo> nuevaDentroPorNumero = new java.util.TreeMap<Double, DetallePrestamo>();
        if (nuevas != null) {
            for (DetallePrestamo n : nuevas) {
                if (n.getNumeroCuota() == null) {
                    continue;
                }
                nuevaPorNumero.put(n.getNumeroCuota(), n);
                if (dentro(n.getFechaVencimiento(), primerMes, limite)) {
                    nuevaDentroPorNumero.put(n.getNumeroCuota(), n);
                }
            }
        }
        java.util.TreeMap<Double, Double> porNumero = new java.util.TreeMap<Double, Double>();
        double residuo = 0.0;
        if (historizadas != null) {
            for (HistDetallePrestamo h : historizadas) {
                if (h.getNumeroCuota() == null || !dentro(h.getFechaVencimiento(), primerMes, limite)) {
                    continue;
                }
                double interesH = h.getInteres() != null ? h.getInteres().doubleValue() : 0.0;
                DetallePrestamo nueva = nuevaPorNumero.get(h.getNumeroCuota());
                if (nueva == null) {
                    residuo += interesH;
                    continue;
                }
                double interesN = nuevaDentroPorNumero.containsKey(h.getNumeroCuota()) && nueva.getInteres() != null
                        ? nueva.getInteres().doubleValue() : 0.0;
                double diferencia = redondear(interesH - interesN);
                if (diferencia > 0.0) {
                    porNumero.merge(h.getNumeroCuota(), diferencia, Double::sum);
                }
            }
        }
        // Destino del residuo y de los ajustes: la cuota de número mayor que queda dentro del límite (si no hay, la de número mayor).
        Double numeroDestino = !nuevaDentroPorNumero.isEmpty() ? nuevaDentroPorNumero.lastKey()
                : (!nuevaPorNumero.isEmpty() ? nuevaPorNumero.lastKey() : null);
        if (numeroDestino == null) {
            System.out.println("  ⚠ DevengoCompletoService: el abono reversó $" + reverso + " del devengo pero no hay cuota nueva donde"
                    + " asentar el libro por cuota (MVIC 12).");
            return;
        }
        if (residuo > 0.0) {
            porNumero.merge(numeroDestino, redondear(residuo), Double::sum);
        }
        double total = 0.0;
        for (double v : porNumero.values()) {
            total += v;
        }
        double ajuste = redondear(reverso - total);
        if (ajuste > 0.0) {
            porNumero.merge(numeroDestino, ajuste, Double::sum);
        } else if (ajuste < 0.0) {
            double porQuitar = -ajuste;
            for (Double numero : new ArrayList<Double>(porNumero.descendingKeySet())) {
                double quita = Math.min(porNumero.get(numero), porQuitar);
                porNumero.put(numero, redondear(porNumero.get(numero) - quita));
                porQuitar = redondear(porQuitar - quita);
                if (porQuitar <= 0.0) {
                    break;
                }
            }
        }
        for (java.util.Map.Entry<Double, Double> e : porNumero.entrySet()) {
            DetallePrestamo cuota = nuevaPorNumero.get(e.getKey());
            if (cuota == null) {
                cuota = nuevaPorNumero.get(numeroDestino);
            }
            if (cuota != null && e.getValue() >= MINIMO) {
                resultado.getCuotas().add(new ResultadoReversoDevengo.Cuota(cuota.getCodigo(), redondear(e.getValue())));
            }
        }
    }

    private boolean dentro(java.time.LocalDateTime vencimiento, LocalDate primerMes, LocalDate limite) {
        if (vencimiento == null) {
            return false;
        }
        LocalDate v = vencimiento.toLocalDate();
        return !v.isAfter(limite) && !v.isBefore(primerMes);
    }

    @Override
    public List<DetalleAsiento> lineasReversoCuotasCanceladas(Long idEmpresa, Prestamo prestamo, LocalDate fechaEvento,
            String prefijo) throws Throwable {
        return calcularReversoCuotasCanceladas(idEmpresa, prestamo, fechaEvento, prefijo, null).getLineas();
    }

    @Override
    public ResultadoReversoDevengo calcularReversoCuotasCanceladas(Long idEmpresa, Prestamo prestamo, LocalDate fechaEvento,
            String prefijo, Long idEvento) throws Throwable {
        System.out.println("Ingresa al metodo (calcularReversoCuotasCanceladas) DevengoCompletoService - empresa: "
                + idEmpresa);
        ResultadoReversoDevengo resultado = new ResultadoReversoDevengo(prestamo, ORIGEN_CANCELACION, idEvento);
        LocalDate limite = limiteDevengado(idEmpresa, fechaEvento);
        if (limite == null || prestamo == null || fechaEvento == null) {
            return resultado;
        }
        LocalDate primerMes = primerMesDevengado();
        double noCobrado = 0.0;
        for (DetallePrestamo cuota : detallePrestamoDaoService.selectByPrestamo(prestamo.getCodigo())) {
            if (cuota.getEstado() == null || cuota.getEstado().longValue() != EstadoCuotaPrestamo.CANCELADA_ANTICIPADA
                    || cuota.getFechaVencimiento() == null) {
                continue;
            }
            LocalDate vence = cuota.getFechaVencimiento().toLocalDate();
            // (a) las que ESTA operación cerró (vencimiento posterior a su fecha, como en precancelar()),
            // (b) de un mes ya devengado y (c) que el ④ nuevo ya devengó (no las de antes de devengoCompletoDesde).
            if (!vence.isAfter(fechaEvento) || vence.isAfter(limite) || vence.isBefore(primerMes)) {
                continue;
            }
            double cobrado = 0.0;
            List<PagoPrestamo> pagos = pagoPrestamoDaoService.selectVigentesByIdDetallePrestamo(cuota.getCodigo());
            if (pagos != null) {
                for (PagoPrestamo pago : pagos) {
                    cobrado += pago.getInteresPagado() != null ? pago.getInteresPagado().doubleValue() : 0.0;
                }
            }
            double interes = cuota.getInteres() != null ? cuota.getInteres().doubleValue() : 0.0;
            // Lote 20: por cuota y redondeado por cuota; el asiento es la suma de lo que se graba en el libro (MVIC 12).
            double aReversar = redondear(Math.max(interes - cobrado, 0.0));
            if (aReversar >= MINIMO) {
                resultado.getCuotas().add(new ResultadoReversoDevengo.Cuota(cuota.getCodigo(), aReversar));
                noCobrado += aReversar;
            }
        }
        resultado.getLineas().addAll(lineasReverso(idEmpresa, prestamo, redondear(noCobrado), prefijo
                + " - reverso del devengo del interés de cuotas canceladas sin cobrar (meses ya devengados)"));
        if (resultado.getLineas().isEmpty()) {
            resultado.getCuotas().clear();
        }
        return resultado;
    }

    @Override
    public void registrarReverso(ResultadoReversoDevengo resultado, Long idAsiento, LocalDate fechaContable, String usuario)
            throws Throwable {
        if (resultado == null || resultado.getCuotas().isEmpty() || resultado.getLineas().isEmpty()) {
            return;
        }
        // Requiere sql/406 (CK_MVIC_TIPO hasta 12). Solo hay cuotas si el devengo completo está activo.
        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
        for (ResultadoReversoDevengo.Cuota cuota : resultado.getCuotas()) {
            com.saa.model.crd.MovimientoInteresCuota mov = new com.saa.model.crd.MovimientoInteresCuota();
            mov.setPrestamo(resultado.getPrestamo());
            mov.setIdCuota(cuota.getIdCuota());
            mov.setTipoMovimiento(com.saa.rubros.TipoMovimientoInteresCuota.REVERSO_DEVENGO_INTERES);
            mov.setComponente(com.saa.rubros.ComponenteMovimientoInteresCuota.INTERES);
            mov.setValor(cuota.getValor());
            mov.setFechaContable(fechaContable);
            mov.setOrigen(resultado.getOrigen());
            mov.setIdOrigen(resultado.getIdOrigen());
            mov.setAsiento(idAsiento);
            mov.setAnulado(0L);
            mov.setUsuarioRegistro(usuario);
            mov.setFechaRegistro(ahora);
            movimientoInteresCuotaDaoService.save(mov, null);
        }
    }

    @Override
    public int anularPorEvento(Long idEvento) throws Throwable {
        if (idEvento == null) {
            return 0;
        }
        return movimientoInteresCuotaDaoService.anularByOrigenYIdOrigen(ORIGEN_CANCELACION, idEvento)
                + movimientoInteresCuotaDaoService.anularByOrigenYIdOrigen(ORIGEN_ABONO, idEvento);
    }

    /**
     * Primer día del primer mes que devengó el ④ nuevo: el mes siguiente al corte {@code devengoCompletoDesde}. Lo anterior
     * lo devengó el ④ de siempre (y el asiento manual C27b): no se reversa.
     */
    private LocalDate primerMesDevengado() throws Throwable {
        return ModoCorteCartera.devengoCompletoDesde().withDayOfMonth(1).plusMonths(1);
    }

    /** D ingreso (papel 30) / H interés por cobrar (papel 10) de la plantilla 17, del tipo de préstamo del préstamo. */
    private List<DetalleAsiento> lineasReverso(Long idEmpresa, Prestamo prestamo, double valor, String descripcion)
            throws Throwable {
        List<DetalleAsiento> lineas = new ArrayList<DetalleAsiento>();
        if (valor < MINIMO) {
            return lineas;
        }
        Long idTipo = prestamo != null && prestamo.getProducto() != null && prestamo.getProducto().getTipoPrestamo() != null
                ? prestamo.getProducto().getTipoPrestamo().getCodigo() : null;
        if (idTipo == null) {
            throw new IncomeException("No se puede reversar el devengo del préstamo "
                    + (prestamo != null ? prestamo.getCodigo() : null) + ": su producto no tiene tipo de préstamo.");
        }
        Long idPlantilla = plantillaService.codigoByAlterno(PlantillasCredito.DEVENGO_INTERESES, idEmpresa);
        if (idPlantilla == null || idPlantilla.longValue() == 0L) {
            throw new IncomeException("No existe la plantilla contable con codigo alterno "
                    + PlantillasCredito.DEVENGO_INTERESES + " (devengo de intereses) para la empresa " + idEmpresa + ".");
        }
        DetallePlantilla ingreso = detallePlantillaDaoService.selectByPlantillaYAuxiliares(idPlantilla,
                CrdLineaAsiento.INGRESO_INTERES_ORDINARIO, idTipo.intValue());
        DetallePlantilla porCobrar = detallePlantillaDaoService.selectByPlantillaYAuxiliares(idPlantilla,
                CrdLineaAsiento.INTERES_ORDINARIO_POR_COBRAR, idTipo.intValue());
        if (ingreso == null || ingreso.getPlanCuenta() == null || porCobrar == null || porCobrar.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla " + PlantillasCredito.DEVENGO_INTERESES + " no define las líneas "
                    + CrdLineaAsiento.INGRESO_INTERES_ORDINARIO + " y " + CrdLineaAsiento.INTERES_ORDINARIO_POR_COBRAR
                    + " para el tipo de préstamo " + idTipo + ": no se puede reversar el devengo.");
        }
        lineas.add(linea(ingreso.getPlanCuenta(), descripcion, valor, true));
        lineas.add(linea(porCobrar.getPlanCuenta(), descripcion, valor, false));
        return lineas;
    }

    private DetalleAsiento linea(PlanCuenta cuenta, String descripcion, double valor, boolean debe) {
        DetalleAsiento detalle = new DetalleAsiento();
        detalle.setPlanCuenta(cuenta);
        detalle.setNumeroCuenta(cuenta.getCuentaContable());
        detalle.setNombreCuenta(cuenta.getNombre());
        detalle.setDescripcion(descripcion);
        detalle.setValorDebe(debe ? valor : 0.0);
        detalle.setValorHaber(debe ? 0.0 : valor);
        return detalle;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}

package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.MovimientoInteresCuotaDaoService;
import com.saa.ejb.crd.service.ModoCorteCartera;
import com.saa.ejb.crd.service.ProvisionInteresService;
import com.saa.ejb.crd.service.ReversoMoraCancelacionService;
import com.saa.ejb.crd.service.dto.ResultadoReversoMora;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.TipoMovimientoInteresCuota;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @see ReversoMoraCancelacionService
 * @author Sistema SAA
 * @since 2026-11-25
 */
@Stateless
public class ReversoMoraCancelacionServiceImpl implements ReversoMoraCancelacionService {

    private static final double TOLERANCIA = 0.005;

    @EJB
    private MovimientoInteresCuotaDaoService movimientoInteresCuotaDaoService;

    @EJB
    private ProvisionInteresService provisionInteresService;

    /** Una cuota con su mora al corte del evento. */
    private static final class FilaCuota {
        final Long idCuota;
        final LocalDate vencimiento;
        final double devengo;
        final double pagada;
        final double provision;
        double neto;
        double reversaDevengo;

        FilaCuota(Long idCuota, LocalDate vencimiento, double devengo, double pagada, double provision) {
            this.idCuota = idCuota;
            this.vencimiento = vencimiento;
            this.devengo = devengo;
            this.pagada = pagada;
            this.provision = provision;
        }
    }

    @Override
    public ResultadoReversoMora calcular(Long idEmpresa, Prestamo prestamo, LocalDate fecha, List<PagoPrestamo> pagosDelEvento,
            double moraCobradaFueraDePagos, String origen, Long idOrigen, String prefijo) throws Throwable {
        System.out.println("ReversoMoraCancelacionService.calcular - préstamo: " + (prestamo != null ? prestamo.getCodigo() : null)
            + " - fecha: " + fecha + " - origen: " + origen + " " + idOrigen);
        ResultadoReversoMora resultado = new ResultadoReversoMora();
        resultado.setPrestamo(prestamo);
        resultado.setFecha(fecha);
        resultado.setOrigen(origen);
        resultado.setIdOrigen(idOrigen);
        if (prestamo == null || prestamo.getCodigo() == null || fecha == null || idEmpresa == null
                || !ModoCorteCartera.topeProvisionMoraActivo(fecha)) {
            return resultado;
        }
        if (idOrigen != null && movimientoInteresCuotaDaoService.existeVigentePorOrigen(origen, idOrigen)) {
            System.out.println("  Ya hay un reverso de mora vigente para " + origen + " " + idOrigen + ": no se repite.");
            return resultado;
        }

        // Mora cobrada por el evento, por cuota (ya grabada en PGPR: está dentro de «pagada»).
        Map<Long, Double> cobradoPorEvento = new HashMap<Long, Double>();
        if (pagosDelEvento != null) {
            for (PagoPrestamo pago : pagosDelEvento) {
                if (pago == null || pago.getDetallePrestamo() == null || pago.getMoraPagada() == null
                        || (pago.getAnulado() != null && pago.getAnulado().longValue() == 1L)) {
                    continue;
                }
                cobradoPorEvento.merge(pago.getDetallePrestamo().getCodigo(), pago.getMoraPagada(), Double::sum);
            }
        }

        List<FilaCuota> filas = new ArrayList<FilaCuota>();
        for (Object[] f : movimientoInteresCuotaDaoService.selectMoraDelPrestamoPorCuota(prestamo.getCodigo(), fecha)) {
            filas.add(new FilaCuota((Long) f[0], (LocalDate) f[1], ((Number) f[2]).doubleValue(),
                ((Number) f[3]).doubleValue(), ((Number) f[4]).doubleValue()));
        }
        filas.sort(Comparator.comparing((FilaCuota f) -> f.vencimiento, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(f -> f.idCuota));

        // Mora contabilizada pendiente por cuota: devengo − lo que el cobro lleva a 1.4.02. Del cobro del evento solo cuenta hasta el devengo de su
        // cuota: el resto lo separa separarMoraNoDevengada a ingreso y no toca 1.4.02.
        double sumaNeto = 0.0;
        for (FilaCuota f : filas) {
            double delEvento = cobradoPorEvento.getOrDefault(f.idCuota, 0.0);
            double previa = Math.max(0.0, f.pagada - delEvento);
            double aCuentas = previa + Math.min(delEvento, Math.max(0.0, f.devengo - previa));
            f.neto = f.devengo - aCuentas;
            sumaNeto += f.neto;
        }
        double condonada = movimientoInteresCuotaDaoService.selectMoraCondonadaDelPrestamo(prestamo.getCodigo(), fecha)
            + Math.max(0.0, moraCobradaFueraDePagos);
        double pendiente = Math.max(0.0, round2(sumaNeto - condonada));

        // Reparto: lo condonado y lo cobrado fuera de pagos se resta de las cuotas más viejas; lo que queda por préstamo se reparte del más viejo al
        // más nuevo sin pasar del neto positivo de cada cuota (como el reparto del tope del lote 7b).
        double aRestar = condonada;
        List<double[]> pendientePorCuota = new ArrayList<double[]>();
        for (FilaCuota f : filas) {
            double positivo = Math.max(0.0, f.neto);
            double resta = Math.min(aRestar, positivo);
            aRestar -= resta;
            pendientePorCuota.add(new double[]{ positivo - resta });
        }
        double restante = pendiente;
        for (int i = 0; i < filas.size(); i++) {
            double asignado = Math.min(pendientePorCuota.get(i)[0], restante);
            filas.get(i).reversaDevengo = round2(Math.max(0.0, asignado));
            restante = round2(restante - filas.get(i).reversaDevengo);
        }

        double totalDevengo = 0.0;
        double totalProvision = 0.0;
        for (FilaCuota f : filas) {
            double provision = f.provision > TOLERANCIA ? round2(f.provision) : 0.0;
            if (f.reversaDevengo > TOLERANCIA || provision > TOLERANCIA) {
                resultado.getCuotas().add(new ResultadoReversoMora.Cuota(f.idCuota, f.reversaDevengo, provision));
                totalDevengo += f.reversaDevengo;
                totalProvision += provision;
            }
        }
        totalDevengo = round2(totalDevengo);
        totalProvision = round2(totalProvision);
        resultado.setTotalDevengo(totalDevengo);
        resultado.setTotalProvision(totalProvision);
        if (totalDevengo <= TOLERANCIA && totalProvision <= TOLERANCIA) {
            resultado.getCuotas().clear();
            return resultado;
        }

        Long idTipoPrestamo = prestamo.getProducto() != null && prestamo.getProducto().getTipoPrestamo() != null
            ? prestamo.getProducto().getTipoPrestamo().getCodigo() : null;
        if (idTipoPrestamo == null) {
            throw new IncomeException("El préstamo " + prestamo.getCodigo() + " tiene mora devengada o provisionada que reversar al cancelarse"
                + " pero su producto no tiene tipo de préstamo: no se puede resolver la cuenta contable.");
        }
        Map<Long, Double> devengoPorTipo = new LinkedHashMap<Long, Double>();
        Map<Long, Double> provisionPorTipo = new LinkedHashMap<Long, Double>();
        devengoPorTipo.put(idTipoPrestamo, totalDevengo);
        provisionPorTipo.put(idTipoPrestamo, totalProvision);
        resultado.getLineas().addAll(provisionInteresService.lineasReversoMoraCancelacion(idEmpresa, devengoPorTipo,
            provisionPorTipo, prefijo + " - reverso de mora no cobrada por cancelación"));
        System.out.println("  Reverso de mora por cancelación: devengo " + totalDevengo + ", provisión " + totalProvision
            + ", cuotas " + resultado.getCuotas().size());
        return resultado;
    }

    @Override
    public void registrar(ResultadoReversoMora resultado, Long idAsiento, LocalDate fechaContable, String usuario) throws Throwable {
        if (resultado == null || resultado.estaVacio()) {
            return;
        }
        for (ResultadoReversoMora.Cuota cuota : resultado.getCuotas()) {
            if (cuota.getDevengo() > TOLERANCIA) {
                provisionInteresService.registrarMovimientoMora(cuota.getIdCuota(), resultado.getPrestamo(),
                    TipoMovimientoInteresCuota.REVERSO_DEVENGO_POR_COBRO_TARDIO, cuota.getDevengo(), fechaContable != null ? fechaContable : resultado.getFecha(),
                    resultado.getOrigen(), resultado.getIdOrigen(), idAsiento, usuario);
            }
            if (cuota.getProvision() > TOLERANCIA) {
                provisionInteresService.registrarMovimientoMora(cuota.getIdCuota(), resultado.getPrestamo(),
                    TipoMovimientoInteresCuota.REVERSO_POR_COBRO_TARDIO, cuota.getProvision(), fechaContable != null ? fechaContable : resultado.getFecha(),
                    resultado.getOrigen(), resultado.getIdOrigen(), idAsiento, usuario);
            }
        }
    }

    @Override
    public int anularPorEvento(Long idEvento) throws Throwable {
        if (idEvento == null) {
            return 0;
        }
        int n = movimientoInteresCuotaDaoService.anularByOrigenYIdOrigen(ORIGEN_EVENTO, idEvento);
        if (n > 0) {
            System.out.println("ReversoMoraCancelacionService.anularPorEvento - evento " + idEvento + ": " + n + " movimiento(s) anulado(s)");
        }
        return n;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}

package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.AbonoCapitalPrestamoService;
import com.saa.ejb.crd.service.CalculadoraAmortizacionService;
import com.saa.ejb.crd.service.ProcesoPagoPrestamoService;
import com.saa.ejb.crd.service.SimulacionPrestamoService;
import com.saa.ejb.crd.service.dto.CuotaProyectada;
import com.saa.ejb.crd.service.dto.ParametrosAmortizacion;
import com.saa.ejb.crd.service.dto.ResultadoSimulacionCreditoNuevo;
import com.saa.ejb.crd.service.dto.ResultadoSimulacionReestructuracion;
import com.saa.ejb.crd.service.dto.SolicitudReestructuracion;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.EstadoPrestamo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación de {@link SimulacionPrestamoService}. Ninguno de los dos métodos escribe nada
 * (decisión 8 del plan): por eso lee las cuotas pendientes TAL CUAL están en {@code CRD.DTPR},
 * sin pasar por {@code MotorPagoPrestamoService.calcularSaldosRealesCuota} — ese método
 * autocorrige cuotas a PAGADA como efecto secundario deliberado (§10.3 del plan), y usarlo acá
 * rompería la garantía de "no escribe nada". Ver PLAN-SIMULADORES-PRESTAMOS.md §11 para el
 * detalle de esta decisión y sus consecuencias.
 *
 * @author Sistema SAA
 * @since 2026-08-25
 */
@Stateless
public class SimulacionPrestamoServiceImpl implements SimulacionPrestamoService {

    @EJB
    private CalculadoraAmortizacionService calculadoraAmortizacionService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @Override
    public ResultadoSimulacionCreditoNuevo simularCreditoNuevo(ParametrosAmortizacion params) throws Throwable {
        System.out.println("SimulacionPrestamoServiceImpl.simularCreditoNuevo");

        List<CuotaProyectada> tabla = calculadoraAmortizacionService.calcular(params);

        double totalCapital = 0.0;
        double totalInteres = 0.0;
        double totalDesgravamen = 0.0;
        double totalSeguro = 0.0;
        double totalAPagar = 0.0;
        Double valorCuota = null;

        for (CuotaProyectada cuota : tabla) {
            totalCapital += nvl(cuota.getCapital());
            totalInteres += nvl(cuota.getInteres());
            totalDesgravamen += nvl(cuota.getDesgravamen());
            totalSeguro += nvl(cuota.getSeguroIncendio());
            totalAPagar += nvl(cuota.getTotal());
            if (valorCuota == null && cuota.getNumeroCuota() != null && cuota.getNumeroCuota() > 0) {
                valorCuota = cuota.getCuota();
            }
        }

        ResultadoSimulacionCreditoNuevo resultado = new ResultadoSimulacionCreditoNuevo();
        resultado.setTablaProyectada(tabla);
        resultado.setTotalCapital(redondear(totalCapital));
        resultado.setTotalInteres(redondear(totalInteres));
        resultado.setTotalDesgravamen(redondear(totalDesgravamen));
        resultado.setTotalSeguro(redondear(totalSeguro));
        resultado.setTotalAPagar(redondear(totalAPagar));
        resultado.setValorCuota(valorCuota);
        return resultado;
    }

    @Override
    public ResultadoSimulacionReestructuracion simularReestructuracion(SolicitudReestructuracion solicitud)
            throws Throwable {
        System.out.println("SimulacionPrestamoServiceImpl.simularReestructuracion - Préstamo: "
            + (solicitud != null ? solicitud.getIdPrestamo() : null));

        if (solicitud == null || solicitud.getIdPrestamo() == null) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PARAMETRO_INVALIDO
                + ": idPrestamo es obligatorio");
        }

        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), solicitud.getIdPrestamo());
        if (prestamo == null) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PRESTAMO_NO_ENCONTRADO
                + ": no existe el préstamo " + solicitud.getIdPrestamo());
        }
        if (esEstadoTerminal(prestamo.getIdEstado())) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_ESTADO_NO_PERMITE
                + ": el préstamo " + solicitud.getIdPrestamo() + " está en estado " + prestamo.getIdEstado()
                + " (terminal) y no admite reestructuración");
        }
        if (prestamo.getTipoAmortizacion() == null) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PARAMETRO_INVALIDO
                + ": el préstamo " + solicitud.getIdPrestamo() + " no tiene definido el tipo de amortización");
        }

        int mesesGracia = solicitud.getMesesGracia() != null ? solicitud.getMesesGracia() : 0;
        if (mesesGracia < 0 || mesesGracia > 1) {
            throw new IncomeException(ERR_GRACIA_NO_SOPORTADA
                + ": la calculadora solo soporta un único período de gracia (0 o 1); se pidió "
                + mesesGracia);
        }

        // Cuotas pendientes tal cual están en DTPR, SIN reconciliar contra PGPR (ver javadoc de
        // la clase: calcularSaldosRealesCuota tiene un efecto de persistencia que rompería la
        // decisión 8 de este plan).
        List<DetallePrestamo> pendientes =
            detallePrestamoDaoService.selectCuotasNoPagadasByPrestamo(solicitud.getIdPrestamo());
        if (pendientes == null || pendientes.isEmpty()) {
            throw new IncomeException(AbonoCapitalPrestamoService.ERR_SIN_CUOTAS_PENDIENTES
                + ": el préstamo " + solicitud.getIdPrestamo() + " no tiene cuotas pendientes que reestructurar");
        }

        double saldoCapitalPendiente = 0.0;
        double moraPendiente = 0.0;
        double interesVencidoPendiente = 0.0;
        double totalAPagarActualSchedule = 0.0;
        for (DetallePrestamo cuota : pendientes) {
            saldoCapitalPendiente += nvl(cuota.getCapital()) - nvl(cuota.getCapitalPagado());
            moraPendiente += nvl(cuota.getSaldoMora());
            interesVencidoPendiente += nvl(cuota.getSaldoInteresVencido());
            totalAPagarActualSchedule += nvl(cuota.getTotal());
        }
        saldoCapitalPendiente = redondear(saldoCapitalPendiente);
        moraPendiente = redondear(moraPendiente);
        interesVencidoPendiente = redondear(interesVencidoPendiente);

        // "Lo que el préstamo debe hoy en total" SIEMPRE incluye mora e interés vencido
        // pendientes, se capitalicen o no: si no se capitalizan, esa deuda no desaparece, solo
        // queda fuera de la tabla nueva (por eso NO se vuelve a sumar en totalAPagarNuevo salvo
        // que capitalizarVencido sea true).
        double totalAPagarActual = redondear(totalAPagarActualSchedule + moraPendiente + interesVencidoPendiente);

        double cuotaActual = nvl(prestamo.getValorCuota());
        if (cuotaActual <= 0.0) {
            cuotaActual = nvl(pendientes.get(0).getCuota());
        }
        cuotaActual = redondear(cuotaActual);

        boolean capitalizarVencido = Boolean.TRUE.equals(solicitud.getCapitalizarVencido());
        double moraCapitalizada = capitalizarVencido ? moraPendiente : 0.0;
        double interesVencidoCapitalizado = capitalizarVencido ? interesVencidoPendiente : 0.0;
        double capitalDeArranque = redondear(saldoCapitalPendiente + moraCapitalizada + interesVencidoCapitalizado);

        if (capitalDeArranque <= 0.0) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_VALOR_INVALIDO
                + ": el capital de arranque de la reestructuración es $" + capitalDeArranque
                + "; revise el saldo pendiente del préstamo " + solicitud.getIdPrestamo());
        }

        int plazoActual = pendientes.size();
        int plazoNuevo = solicitud.getNuevoPlazo() != null ? solicitud.getNuevoPlazo() : plazoActual;
        double tasaActual = nvl(prestamo.getTasa());
        double tasaNueva = solicitud.getNuevaTasaAnual() != null ? solicitud.getNuevaTasaAnual() : tasaActual;

        // Desgravamen/seguro por cuota: se copian de la última cuota pendiente, mismo supuesto
        // que AbonoCapitalPrestamoServiceImpl (§7.3 paso 6 de ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md).
        DetallePrestamo ultimaPendiente = pendientes.get(pendientes.size() - 1);

        ParametrosAmortizacion params = new ParametrosAmortizacion();
        params.setMonto(capitalDeArranque);
        params.setTasaAnual(tasaNueva);
        params.setPlazo(plazoNuevo);
        params.setTipoAmortizacion(prestamo.getTipoAmortizacion());
        params.setFechaInicio(LocalDateTime.now());
        params.setTieneCuotaCero(mesesGracia == 1);
        params.setDesgravamenPorCuota(nvl(ultimaPendiente.getDesgravamen()));
        params.setSeguroIncendioPorCuota(nvl(ultimaPendiente.getValorSeguroIncendio()));

        List<CuotaProyectada> tabla = calculadoraAmortizacionService.calcular(params);

        double totalAPagarNuevoSchedule = 0.0;
        Double cuotaNueva = null;
        for (CuotaProyectada cuota : tabla) {
            totalAPagarNuevoSchedule += nvl(cuota.getTotal());
            if (cuotaNueva == null && cuota.getNumeroCuota() != null && cuota.getNumeroCuota() > 0) {
                cuotaNueva = cuota.getCuota();
            }
        }
        // Si NO se capitalizó, la mora/interés vencido quedan fuera de la tabla nueva y siguen
        // debiéndose: se suman igual acá para que "totalAPagarNuevo" sea "lo que el socio va a
        // terminar pagando en total", no solo lo que cubre la tabla nueva.
        double deudaNoCapitalizada = capitalizarVencido ? 0.0 : (moraPendiente + interesVencidoPendiente);
        double totalAPagarNuevo = redondear(totalAPagarNuevoSchedule + deudaNoCapitalizada);

        ResultadoSimulacionReestructuracion resultado = new ResultadoSimulacionReestructuracion();
        resultado.setIdPrestamo(prestamo.getCodigo());
        resultado.setTipoAmortizacion(prestamo.getTipoAmortizacion());
        resultado.setSaldoCapitalPendiente(saldoCapitalPendiente);
        resultado.setCapitalizarVencido(capitalizarVencido);
        resultado.setMoraCapitalizada(moraCapitalizada);
        resultado.setInteresVencidoCapitalizado(interesVencidoCapitalizado);
        resultado.setCapitalDeArranque(capitalDeArranque);
        resultado.setTasaActual(tasaActual);
        resultado.setTasaNueva(tasaNueva);
        resultado.setPlazoActual((long) plazoActual);
        resultado.setPlazoNuevo((long) plazoNuevo);
        resultado.setCuotaActual(cuotaActual);
        resultado.setCuotaNueva(cuotaNueva != null ? cuotaNueva : 0.0);
        resultado.setMesesGracia(mesesGracia);
        resultado.setTotalAPagarActual(totalAPagarActual);
        resultado.setTotalAPagarNuevo(totalAPagarNuevo);
        resultado.setTablaProyectada(tabla);
        return resultado;
    }

    private boolean esEstadoTerminal(Long idEstado) {
        return idEstado != null
            && (idEstado == EstadoPrestamo.CANCELADO
             || idEstado == EstadoPrestamo.CANCELADO_ANTICIPADO
             || idEstado == EstadoPrestamo.CANCELADO_POR_NOVACION);
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

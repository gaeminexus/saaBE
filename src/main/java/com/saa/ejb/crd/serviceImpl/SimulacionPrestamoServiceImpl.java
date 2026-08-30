package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.AbonoCapitalPrestamoService;
import com.saa.ejb.crd.service.CalculadoraAmortizacionService;
import com.saa.ejb.crd.service.MotorPagoPrestamoService;
import com.saa.ejb.crd.service.PagoPrestamoService;
import com.saa.ejb.crd.service.ProcesoPagoPrestamoService;
import com.saa.ejb.crd.service.SimulacionPrestamoService;
import com.saa.ejb.crd.service.dto.CuotaProyectada;
import com.saa.ejb.crd.service.dto.ParametrosAmortizacion;
import com.saa.ejb.crd.service.dto.ResultadoSimulacionCreditoNuevo;
import com.saa.ejb.crd.service.dto.ResultadoSimulacionReestructuracion;
import com.saa.ejb.crd.service.dto.SaldosCuota;
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

    /** Segunda ola, pedido 8: reconstruir el saldo de capital desde CRD.PGPR, no leer DTPRCPPG/DTPRSICP.
     *  Cálculo compartido con la precancelación — ver PagoPrestamoService.calcularSaldoCapitalPendiente. */
    @EJB
    private PagoPrestamoService pagoPrestamoService;

    /**
     * Corrección urgente 2026-08-29: para reconstruir el interés ordinario pendiente de las
     * cuotas vencidas (ver {@link #simularReestructuracion}). SOLO la variante PURA
     * {@code calcularSaldosCuota} — {@code calcularSaldosRealesCuota} autocorrige y persiste,
     * lo que rompería la garantía "no escribe nada" de esta clase (ver javadoc de arriba).
     */
    @EJB
    private MotorPagoPrestamoService motorPagoPrestamoService;

    @Override
    public ResultadoSimulacionCreditoNuevo simularCreditoNuevo(ParametrosAmortizacion params) throws Throwable {
        System.out.println("SimulacionPrestamoServiceImpl.simularCreditoNuevo");

        // Segunda ola, pedido 2 (2026-08-27): el desgravamen de la simulación de crédito nuevo
        // SIEMPRE sale del saldo de capital de cada cuota (saldo * 1.12 / 1000), no de un valor
        // fijo — se fuerza acá, sin importar lo que traiga el request, porque es un requisito
        // de este simulador puntual (la reestructuración y el generador real NO cambian: siguen
        // con el valor fijo de ParametrosAmortizacion.desgravamenPorCuota).
        params.setCalcularDesgravamenSobreSaldo(true);

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

        // ⚠️ moraPendiente/totalAPagarActualSchedule se siguen sumando sobre TODA la lista
        // `pendientes` tal como viene de selectCuotasNoPagadasByPrestamo. Tienen el mismo
        // problema de origen que el capital (abajo): si esa lista incluye una cuota que en
        // realidad ya está pagada (DTPRESTD no confiable en cartera migrada), sus
        // DTPRSLMR/DTPRTTLL entran igual a la suma. NO se corrige acá — reportado, no arreglado,
        // a pedido explícito.
        //
        // Bug urgente corregido 2026-08-29 (reportado por el usuario probando en producción):
        // interesVencidoPendiente salía SIEMPRE $0.00. Leía `cuota.getSaldoInteresVencido()`
        // (DTPRIVNC), una columna que NINGÚN proceso llena — ver el javadoc de
        // MotorPagoPrestamoServiceImpl.calcularSaldosCuota: "el interés vencido... hoy ningún
        // proceso lo alimenta y por eso vale 0". Y filtrar por DTPRESTD = EN_MORA(5) tampoco
        // sirve: verificado que en la cartera migrada la mora vive en la cabecera del préstamo,
        // casi ninguna cuota individual tiene ese estado puesto.
        //
        // Regla de negocio correcta (usuario, 2026-08-29): el interés vencido es la SUMA DEL
        // INTERÉS ORDINARIO PENDIENTE de las cuotas que están en mora — no un campo propio, se
        // deriva. "En mora" usa el MISMO criterio que el proceso diario de mora
        // (ProcesoMoraPrestamoServiceImpl.calcularMoraPrestamo): selectCuotasVencidasByPrestamo
        // (no pagada/no cancelada anticipada, fechaVencimiento < corte del día — el mismo
        // "corteDelDia" de ese proceso: inicio del día de hoy, una cuota que vence HOY todavía
        // no está vencida). Y "interés ordinario pendiente" es el SALDO neto de lo ya pagado,
        // reconstruido desde CRD.PGPR con motorPagoPrestamoService.calcularSaldosCuota — la
        // variante PURA (no calcularSaldosRealesCuota, que persiste y rompería la garantía de
        // esta clase), la misma que ya usa el punto de mora/interés vencido en
        // AbonoCapitalPrestamoServiceImpl. Leer el bruto de DTPR cobraría de más si la cuota ya
        // tuvo un pago parcial.
        LocalDateTime corteMora = LocalDate.now().atStartOfDay();
        List<DetallePrestamo> vencidas =
            detallePrestamoDaoService.selectCuotasVencidasByPrestamo(solicitud.getIdPrestamo(), corteMora);
        Set<Long> codigosVencidas = new HashSet<>();
        if (vencidas != null) {
            for (DetallePrestamo v : vencidas) {
                codigosVencidas.add(v.getCodigo());
            }
        }

        double moraPendiente = 0.0;
        double interesVencidoPendiente = 0.0;
        double totalAPagarActualSchedule = 0.0;
        for (DetallePrestamo cuota : pendientes) {
            if (codigosVencidas.contains(cuota.getCodigo())) {
                // Doble cuenta evitada (pedido explícito de verificarlo): DTPRTTLL de una cuota
                // vencida YA incluye su propio interés ordinario (y su mora, ver el mismo
                // javadoc de calcularSaldosCuota) — si acá se sumara cuota.getTotal() completo Y
                // ADEMÁS interesVencidoPendiente/moraPendiente por separado, ese interés y esa
                // mora se contarían dos veces y el socio terminaría capitalizando de más. Por
                // eso, para las VENCIDAS, totalAPagarActualSchedule suma el saldo reconstruido
                // SIN el interés ordinario ni la mora (los dos ya van aparte); para las que no
                // están vencidas se sigue sumando su DTPRTTLL completo, sin cambios.
                SaldosCuota saldos = motorPagoPrestamoService.calcularSaldosCuota(cuota);
                interesVencidoPendiente += nvl(saldos.getSaldoInteres());
                moraPendiente += nvl(saldos.getSaldoMora());
                totalAPagarActualSchedule += redondear(nvl(saldos.getSaldoCapital())
                    + nvl(saldos.getSaldoDesgravamen()) + nvl(saldos.getSaldoSeguroIncendio()));
            } else {
                moraPendiente += nvl(cuota.getSaldoMora());
                totalAPagarActualSchedule += nvl(cuota.getTotal());
            }
        }
        moraPendiente = redondear(moraPendiente);
        interesVencidoPendiente = redondear(interesVencidoPendiente);

        // "Lo que el préstamo debe hoy en total" SIEMPRE incluye mora e interés vencido
        // pendientes, se capitalicen o no: si no se capitalizan, esa deuda no desaparece, solo
        // queda fuera de la tabla nueva (por eso NO se vuelve a sumar en totalAPagarNuevo salvo
        // que capitalizarVencido sea true).
        double totalAPagarActual = redondear(totalAPagarActualSchedule + moraPendiente + interesVencidoPendiente);

        // Regla de negocio literal: la primera pendiente es la MENOR cuota (numeroCuota) cuyo
        // estado no sea PAGADA(4) ni CANCELADA_ANTICIPADA(7) — exactamente lo que ya filtra
        // selectCuotasNoPagadasByPrestamo, ordenado ASC. Sin heurística de evidencia de pago:
        // una cuota PARCIAL tiene PagoPrestamo vigente y capitalPagado > 0 y sigue siendo
        // pendiente, así que descartarla por "evidencia de pago" (como hacía la versión anterior
        // de este método) caía en una cuota posterior con un DTPRSICP menor — el saldo
        // subestimado que reportó el usuario. Ver también el fix de estado NULL en
        // DetallePrestamoDaoServiceImpl.selectCuotasNoPagadasByPrestamo.
        DetallePrestamo primeraPendiente = pendientes.get(0);

        // Segunda ola, pedido 8 (2026-08-27) — CORREGIDO: la versión anterior leía DTPRSICP de
        // la mínima cuota pendiente (con fallback a PRSTSLCP), ninguna de las dos reconstruida
        // desde pagos reales. DTPRSICP es una FOTO fija del momento en que se generó la tabla:
        // a diferencia de DTPRSLCP, ningún proceso la vuelve a tocar después (ni siquiera la
        // autocorrección de MotorPagoPrestamoServiceImpl.calcularSaldosRealesCuota), así que en
        // cartera migrada no refleja los pagos que en realidad se aplicaron. Ahora se reconstruye
        // desde CRD.PGPR con pagos VIGENTES (PGPRANUL = 0), igual que
        // MotorPagoPrestamoServiceImpl.calcularSaldosRealesCuota y
        // CierreCarteraDaoServiceImpl.PAGOS_VIGENTES — NUNCA DTPRCPPG (resto de la migración,
        // no confiable: vale igual que DTPRCPTL en 50.853 de 59.147 cuotas, ver
        // PENDIENTES-SEGUNDA-OLA.md §1).
        double saldoCapitalPendiente = pagoPrestamoService.calcularSaldoCapitalPendiente(pendientes);
        if (saldoCapitalPendiente <= 0.0) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_VALOR_INVALIDO
                + ": no se pudo determinar el capital pendiente del préstamo " + solicitud.getIdPrestamo()
                + " — el saldo reconstruido desde CRD.PGPR dio " + saldoCapitalPendiente
                + "; revise los datos antes de reestructurar");
        }

        double cuotaActual = nvl(prestamo.getValorCuota());
        if (cuotaActual <= 0.0) {
            cuotaActual = nvl(primeraPendiente.getCuota());
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

        // Última cuota pendiente: queda como respaldo del seguro de incendio (ver más abajo;
        // el desgravamen fijo que se leía de acá quedó sin uso — ver comentario junto al flag).
        DetallePrestamo ultimaPendiente = pendientes.get(pendientes.size() - 1);

        ParametrosAmortizacion params = new ParametrosAmortizacion();
        params.setMonto(capitalDeArranque);
        params.setTasaAnual(tasaNueva);
        params.setPlazo(plazoNuevo);
        params.setTipoAmortizacion(prestamo.getTipoAmortizacion());
        params.setFechaInicio(LocalDateTime.now());
        params.setTieneCuotaCero(mesesGracia == 1);

        // Bug corregido 2026-08-29: la reestructuración calculaba el desgravamen con un valor
        // FIJO copiado de ultimaPendiente, igual que abono a capital antes de su propio fix.
        // Mismo criterio que simularCreditoNuevo: el desgravamen SIEMPRE sale del saldo de
        // capital de cada cuota (saldo * 1.12/1000), nunca de un valor fijo. Verificado que el
        // flag alcanza TODAS las filas de este motor, incluida la cuota 0 de gracia si la hay
        // (CalculadoraAmortizacionServiceImpl.agregarCuotaCero respeta el mismo flag) — con el
        // flag en true, params.getDesgravamenPorCuota() no se lee en ningún punto del cálculo,
        // así que no se setea (dejarlo seteado sería un valor fijo muerto sin ningún lector).
        params.setCalcularDesgravamenSobreSaldo(true);

        // Bug corregido 2026-08-29: el seguro de incendio SÍ es fijo por cuota (no depende del
        // saldo, a diferencia del desgravamen), pero se copiaba de una SOLA cuota (la última
        // pendiente) hacia TODA la tabla nueva — mismo defecto que tenía
        // AbonoCapitalPrestamoServiceImpl. La corrección ahí preserva por NÚMERO DE CUOTA
        // porque la numeración nueva continúa la vieja; acá NO se puede usar el mismo número:
        // CalculadoraAmortizacionServiceImpl siempre numera sus filas regulares 1..plazoNuevo
        // desde cero (fila(): numeroCuota = i, el índice del bucle, sin ningún offset — no
        // conoce la numeración histórica del préstamo). La correspondencia correcta es por
        // POSICIÓN dentro de `pendientes` (ya ordenada ascendente, la misma lista que se usa
        // para todo lo demás en este método): la cuota nueva #k toma el seguro de
        // pendientes.get(k-1). Si el plazo nuevo pide más cuotas que las historizadas, las que
        // sobran no tienen correspondencia — criterio defensivo: usan el seguro de la última
        // pendiente como respaldo, con log explícito, nunca NPE ni 0 silencioso.
        Map<Long, Double> seguroPorNumeroCuota = new HashMap<>();
        for (int idx = 0; idx < pendientes.size() && idx < plazoNuevo; idx++) {
            seguroPorNumeroCuota.put((long) (idx + 1), redondear(nvl(pendientes.get(idx).getValorSeguroIncendio())));
        }
        if (plazoNuevo > pendientes.size()) {
            System.out.println("SimulacionPrestamoServiceImpl.simularReestructuracion: el plazo nuevo ("
                + plazoNuevo + ") pide más cuotas que las historizadas (" + pendientes.size()
                + "); las cuotas #" + (pendientes.size() + 1) + " a #" + plazoNuevo
                + " usan el seguro de incendio de respaldo (última cuota pendiente, $"
                + redondear(nvl(ultimaPendiente.getValorSeguroIncendio())) + ").");
        }
        params.setSeguroIncendioPorCuota(nvl(ultimaPendiente.getValorSeguroIncendio()));
        params.setSeguroPorNumeroCuota(seguroPorNumeroCuota);

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

package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.saa.basico.ejb.FechaService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.HistDetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.AbonoCapitalPrestamoService;
import com.saa.ejb.crd.service.ContabilidadPrestamoService;
import com.saa.ejb.crd.service.DetallePrestamoService;
import com.saa.ejb.crd.service.EventoPrestamoService;
import com.saa.ejb.crd.service.MotorPagoPrestamoService;
import com.saa.ejb.crd.service.PagoPrestamoService;
import com.saa.ejb.crd.service.PrestamoService;
import com.saa.ejb.crd.service.ProcesoMoraPrestamoService;
import com.saa.ejb.crd.service.ProcesoPagoPrestamoService;
import com.saa.ejb.crd.service.dto.CuotaProyectada;
import com.saa.ejb.crd.service.dto.ResultadoAbonoCapital;
import com.saa.ejb.crd.service.dto.SimulacionAbonoCapital;
import com.saa.ejb.crd.service.dto.SolicitudAbonoCapital;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.EventoPrestamo;
import com.saa.model.crd.HistDetallePrestamo;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoPrestamo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Implementación del abono a capital con re-amortización (§7.3).
 *
 * @author Sistema SAA
 * @since 2026-08-14
 */
@Stateless
public class AbonoCapitalPrestamoServiceImpl implements AbonoCapitalPrestamoService {

    private static final double TOLERANCIA = 0.01;

    /** Tope de seguridad al calcular el plazo nuevo de la modalidad 1 */
    private static final int MAX_CUOTAS_NUEVAS = 600;

    @EJB
    private MotorPagoPrestamoService motorPagoPrestamoService;

    @EJB
    private ProcesoPagoPrestamoService procesoPagoPrestamoService;

    /** Pedido 10: un abono a capital es un abono manual y también regulariza EN_MORA. */
    @EJB
    private ProcesoMoraPrestamoService procesoMoraPrestamoService;

    @EJB
    private EventoPrestamoService eventoPrestamoService;

    @EJB
    private ContabilidadPrestamoService contabilidadPrestamoService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private PrestamoService prestamoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private DetallePrestamoService detallePrestamoService;

    @EJB
    private HistDetallePrestamoDaoService histDetallePrestamoDaoService;

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private PagoPrestamoService pagoPrestamoService;

    @EJB
    private FechaService fechaService;

    // ========================================================================
    // Simulación
    // ========================================================================

    @Override
    public SimulacionAbonoCapital simular(Long idPrestamo, double valor, int modalidad) throws Throwable {
        System.out.println("AbonoCapitalPrestamoService.simular - Préstamo: " + idPrestamo
            + " - Valor: $" + valor + " - Modalidad: " + modalidad);

        // Bug corregido 2026-08-28: simular() es de solo lectura y NO debe autocorregir/persistir
        // cuotas (ver javadoc de MotorPagoPrestamoService.calcularSaldosCuota).
        CalculoAbono calculo = calcular(idPrestamo, valor, modalidad, LocalDate.now(), true);

        SimulacionAbonoCapital simulacion = new SimulacionAbonoCapital();
        simulacion.setIdPrestamo(idPrestamo);
        simulacion.setSaldoCapitalActual(calculo.saldoCapitalPendiente);
        simulacion.setValorAbono(calculo.valorAbono);
        simulacion.setModalidad(modalidad);
        simulacion.setTipoAmortizacion(calculo.tipoAmortizacion);
        simulacion.setPlazoActual(calculo.prestamo.getPlazo());
        simulacion.setPlazoNuevo(calculo.plazoNuevo);
        simulacion.setCuotaActual(calculo.cuotaVigente);
        simulacion.setCuotaNueva(calculo.cuotaNueva);
        simulacion.setAhorroIntereses(calculo.ahorroIntereses);
        simulacion.setCuotasAHistorizar(calculo.cuotasAHistorizar.size());
        simulacion.setSeguroIncendioLiberado(calculo.seguroIncendioLiberado);
        simulacion.setTablaProyectada(calculo.tabla);
        return simulacion;
    }

    // ========================================================================
    // Aplicación
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoAbonoCapital aplicar(SolicitudAbonoCapital solicitud) throws Throwable {
        System.out.println("AbonoCapitalPrestamoService.aplicar - Préstamo: "
            + (solicitud != null ? solicitud.getIdPrestamo() : null));

        if (solicitud == null) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PARAMETRO_INVALIDO
                + ": no se recibió el cuerpo de la solicitud");
        }
        if (solicitud.getIdPrestamo() == null) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PARAMETRO_INVALIDO
                + ": idPrestamo es obligatorio");
        }
        if (solicitud.getUsuario() == null || solicitud.getUsuario().trim().isEmpty()) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PARAMETRO_INVALIDO
                + ": usuario es obligatorio");
        }
        if (solicitud.getModalidad() == null) {
            throw new IncomeException(ERR_MODALIDAD_INVALIDA + ": debe indicar la modalidad (1 o 2)");
        }
        if (solicitud.getIdEmpresa() == null) {
            throw new IncomeException("idEmpresa es obligatorio: es la empresa contable sobre la que"
                + " se genera el asiento de la operación.");
        }

        LocalDate fecha = solicitud.getFecha() != null ? solicitud.getFecha() : LocalDate.now();
        if (fecha.isAfter(LocalDate.now())) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_FECHA_INVALIDA
                + ": la fecha " + fecha + " es futura");
        }
        LocalDateTime fechaHora = fecha.isEqual(LocalDate.now()) ? LocalDateTime.now() : fecha.atStartOfDay();

        double valor = redondear(solicitud.getValor() != null ? solicitud.getValor() : 0.0);
        CalculoAbono calculo = calcular(solicitud.getIdPrestamo(), valor, solicitud.getModalidad(), fecha, false);
        Prestamo prestamo = calculo.prestamo;

        // Ninguna cuota a historizar puede tener PagoPrestamo: el DELETE violaría FK_PGPR_DTPR
        for (DetallePrestamo cuota : calculo.cuotasAHistorizar) {
            List<PagoPrestamo> pagos = pagoPrestamoDaoService.selectByIdDetallePrestamo(cuota.getCodigo());
            if (pagos != null && !pagos.isEmpty()) {
                throw new IncomeException(ERR_PRESTAMO_NO_AL_DIA + ": la cuota #" + cuota.getNumeroCuota()
                    + " tiene pagos registrados (incluso anulados) y no puede re-amortizarse;"
                    + " regularice el préstamo antes de abonar a capital");
            }
        }

        // 1. Evento
        EventoPrestamo evento = new EventoPrestamo();
        evento.setPrestamo(prestamo);
        evento.setTipoOperacion(ProcesoPagoPrestamoService.TIPO_ABONO_CAPITAL);
        evento.setValor(valor);
        evento.setModalidad(Long.valueOf(solicitud.getModalidad().longValue()));
        evento.setPlazoAnterior(prestamo.getPlazo());
        evento.setCuotaAnterior(calculo.cuotaVigente);
        evento.setFecha(fechaHora);
        evento.setObservacion(solicitud.getObservacion());
        evento.setUsuario(solicitud.getUsuario());
        evento.setFechaRegistro(LocalDateTime.now());
        evento.setEstado(1L);
        evento = eventoPrestamoService.saveSingle(evento);
        System.out.println("  💾 EventoPrestamo creado: " + evento.getCodigo() + " (ABONO_CAPITAL)");

        // 2. Historizar las cuotas pendientes en CRD.HDTP y borrarlas de CRD.DTPR
        int historizadas = 0;
        for (DetallePrestamo cuota : calculo.cuotasAHistorizar) {
            HistDetallePrestamo historico = copiarAHistorico(cuota, evento,
                ProcesoPagoPrestamoService.TIPO_ABONO_CAPITAL, solicitud.getUsuario());
            histDetallePrestamoDaoService.save(historico, null);
            historizadas++;
        }
        for (DetallePrestamo cuota : calculo.cuotasAHistorizar) {
            detallePrestamoDaoService.remove(cuota, cuota.getCodigo());
        }
        System.out.println("  📦 Cuotas historizadas y borradas de DTPR: " + historizadas);

        // 3. Generar la nueva tabla
        List<DetallePrestamo> nuevas = new ArrayList<>();
        for (CuotaProyectada proyectada : calculo.tabla) {
            DetallePrestamo cuota = construirCuota(prestamo, proyectada, calculo);
            cuota = detallePrestamoService.saveSingle(cuota);
            nuevas.add(cuota);
        }
        System.out.println("  📄 Cuotas nuevas generadas: " + nuevas.size());

        // 4. Cuota ANCLA donde se acumula el abono en DTPRSLOT
        DetallePrestamo ancla = detallePrestamoDaoService.selectUltimaCuotaPagada(prestamo.getCodigo());
        if (ancla == null) {
            // Préstamo sin cuotas pagadas: el abono se registra en la PRIMERA cuota nueva
            ancla = nuevas.isEmpty() ? null : nuevas.get(0);
        }
        if (ancla == null) {
            throw new IncomeException(ERR_SIN_CUOTAS_PENDIENTES
                + ": no hay ninguna cuota donde registrar el abono");
        }
        // ACUMULATIVO: nunca se pisa un pago extra previo
        ancla.setSaldoOtros(redondear(nvl(ancla.getSaldoOtros()) + valor));
        ancla = detallePrestamoService.saveSingle(ancla);
        System.out.println("  ⚓ Abono acumulado en DTPRSLOT de la cuota #" + ancla.getNumeroCuota()
            + " (" + ancla.getCodigo() + "): $" + ancla.getSaldoOtros());

        // 5. PagoPrestamo del abono: el monto va en saldoOtros, NO en capitalPagado
        PagoPrestamo pago = new PagoPrestamo();
        pago.setPrestamo(prestamo);
        pago.setDetallePrestamo(ancla);
        pago.setNumeroCuota(ancla.getNumeroCuota());
        pago.setFecha(fechaHora);
        pago.setValor(valor);
        pago.setSaldoOtros(valor);
        pago.setCapitalPagado(0.0);
        pago.setInteresPagado(0.0);
        pago.setMoraPagada(0.0);
        pago.setInteresVencidoPagado(0.0);
        pago.setDesgravamen(0.0);
        pago.setValorSeguroIncendio(0.0);
        pago.setTipo(ProcesoPagoPrestamoService.TIPO_ABONO_CAPITAL);
        pago.setObservacion((solicitud.getObservacion() != null ? solicitud.getObservacion() : "")
            + " [Evento: " + evento.getCodigo() + "]");
        pago.setUsuarioRegistro(solicitud.getUsuario());
        pago.setFechaRegistro(LocalDateTime.now());
        pago.setEstado(1L);
        pago.setIdEstado(1L);
        pago.setAnulado(0L);
        pago.setEventoPrestamo(evento);
        pago.setRutaDocumentoRespaldo(solicitud.getRutaDocumentoRespaldo());
        pago = pagoPrestamoService.saveSingle(pago);

        // 6. Actualizar el préstamo
        if (solicitud.getModalidad() == 1) {
            prestamo.setPlazo(calculo.plazoNuevo);
        }
        prestamoDaoService.save(prestamo, prestamo.getCodigo());

        // Recalcula totales/fechaFin/tasas desde la tabla VIVA (paso 7 de §7.3)
        prestamo = prestamoService.actualizarCamposDesdeTabla(prestamo.getCodigo());
        // actualizarCamposPrestamo toma el valorCuota de la PRIMERA cuota de la tabla, que tras
        // la re-amortización es una cuota vieja ya pagada. El valor vigente es el de la tabla nueva.
        prestamo.setValorCuota(calculo.cuotaNueva);
        if (solicitud.getModalidad() == 1) {
            prestamo.setPlazo(calculo.plazoNuevo);
        }

        procesoPagoPrestamoService.registrarHuellaPrestamo(prestamo,
            ProcesoPagoPrestamoService.TIPO_ABONO_CAPITAL, valor, solicitud.getObservacion(),
            fechaHora, solicitud.getUsuario());

        // 7. Cerrar el evento con los valores nuevos
        evento.setPlazoNuevo(calculo.plazoNuevo);
        evento.setCuotaNueva(calculo.cuotaNueva);
        evento = eventoPrestamoService.saveSingle(evento);

        // 8. Hook contable — re-bandeo (2026-08-31, Fase 3)
        Long numeroAsiento = contabilidadPrestamoService.contabilizarAbonoCapital(evento, solicitud.getIdEmpresa());
        if (numeroAsiento != null) {
            evento.setNumeroAsiento(numeroAsiento);
            eventoPrestamoService.saveSingle(evento);
            pago.setAsiento(numeroAsiento);
            pagoPrestamoService.saveSingle(pago);
        }

        // Pedido 10: si el abono dejó al préstamo sin cuotas vencidas y estaba EN_MORA(11),
        // vuelve a VIGENTE(2) de inmediato. En la práctica esta re-amortización ya exige
        // (validación 3, más arriba) que no existan cuotas vencidas ni PARCIAL para llegar
        // hasta aquí, pero se deja el mismo resguardo que en el resto de rutas de pago.
        procesoMoraPrestamoService.regularizarPrestamoSiSinMora(prestamo.getCodigo());

        ResultadoAbonoCapital resultado = new ResultadoAbonoCapital();
        resultado.setIdPrestamo(prestamo.getCodigo());
        resultado.setIdEvento(evento.getCodigo());
        resultado.setIdPagoPrestamo(pago.getCodigo());
        resultado.setIdCuotaConSaldoOtros(ancla.getCodigo());
        resultado.setValorAbono(valor);
        resultado.setModalidad(solicitud.getModalidad());
        resultado.setPlazoAnterior(evento.getPlazoAnterior());
        resultado.setPlazoNuevo(calculo.plazoNuevo);
        resultado.setCuotaAnterior(calculo.cuotaVigente);
        resultado.setCuotaNueva(calculo.cuotaNueva);
        resultado.setCuotasHistorizadas(historizadas);
        resultado.setCuotasGeneradas(nuevas.size());
        resultado.setSeguroIncendioLiberado(calculo.seguroIncendioLiberado);

        System.out.println("  ✅ Abono a capital aplicado - Evento: " + evento.getCodigo()
            + " - Plazo: " + evento.getPlazoAnterior() + " → " + calculo.plazoNuevo
            + " - Cuota: " + calculo.cuotaVigente + " → " + calculo.cuotaNueva);

        return resultado;
    }

    // ========================================================================
    // Cálculo compartido por simular() y aplicar()
    // ========================================================================

    /** Datos intermedios del abono; no se expone fuera del servicio. */
    private static class CalculoAbono {
        private Prestamo prestamo;
        private long tipoAmortizacion;
        private double tasaMensual;
        private double valorAbono;
        private double saldoCapitalPendiente;
        private double nuevoCapital;
        private double cuotaVigente;
        private double cuotaNueva;
        private long plazoNuevo;
        private double ahorroIntereses;
        /**
         * Seguro de incendio de cada cuota HISTORIZADA, por número de cuota — NO es un valor
         * único para toda la tabla (bug corregido 2026-08-29). "El valor de la cuota N vieja
         * va a la cuota N nueva": se preserva por posición, no se toma de una sola fila.
         */
        private Map<Long, Double> seguroPorNumeroCuota = new java.util.HashMap<>();
        /**
         * Reserva defensiva SOLO para una cuota nueva sin correspondencia en
         * {@link #seguroPorNumeroCuota} (el plazo nuevo no debería superar la cantidad de
         * cuotas historizadas, pero si pasa, esto evita una NPE o un 0 silencioso). Se usa con
         * un log de advertencia — nunca en silencio.
         */
        private double seguroUltimaHistorizadaFallback;
        /**
         * Seguro de incendio TOTAL de las cuotas que desaparecen al re-amortizar con menos
         * plazo (modalidad 1) — 0 si el plazo no se acorta. Requerimiento futuro (2026-08-29,
         * pendiente de que el usuario lo pida): ese monto es lo que quedaría "liberado" para
         * pedir reembolso a la aseguradora. NO se implementa el proceso de reembolso acá; el
         * dato solo se deja calculado y expuesto en el resultado del abono para que ese
         * proceso, cuando se construya, no tenga que reconstruir este cálculo.
         */
        private double seguroIncendioLiberado;
        private List<DetallePrestamo> cuotasAHistorizar = new ArrayList<>();
        private List<CuotaProyectada> tabla = new ArrayList<>();
    }

    /**
     * @param soloLectura {@code true} desde {@code simular()}: NO debe escribir en
     *                    {@code CRD.DTPR}. {@code false} desde {@code aplicar()}: puede
     *                    autocorregir cuotas mal etiquetadas antes de re-amortizar (bug
     *                    corregido 2026-08-28, ver MotorPagoPrestamoService.calcularSaldosCuota).
     */
    private CalculoAbono calcular(Long idPrestamo, double valorAbono, int modalidad, LocalDate fecha,
            boolean soloLectura) throws Throwable {

        CalculoAbono calculo = new CalculoAbono();

        // --- Validaciones 1 y 2 -------------------------------------------------
        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
        if (prestamo == null) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_PRESTAMO_NO_ENCONTRADO
                + ": no existe el préstamo " + idPrestamo);
        }
        if (esEstadoTerminalPrestamo(prestamo.getIdEstado())) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_ESTADO_NO_PERMITE
                + ": el préstamo " + idPrestamo + " está en estado " + prestamo.getIdEstado()
                + " (terminal) y no admite abonos");
        }
        if (modalidad != 1 && modalidad != 2) {
            throw new IncomeException(ERR_MODALIDAD_INVALIDA
                + ": la modalidad debe ser 1 (reduce plazo) o 2 (reduce cuota)");
        }
        if (valorAbono <= 0.0) {
            throw new IncomeException(ProcesoPagoPrestamoService.ERR_VALOR_INVALIDO
                + ": el valor del abono debe ser mayor a cero");
        }
        if (prestamo.getTipoAmortizacion() == null) {
            throw new IncomeException("El préstamo " + idPrestamo
                + " no tiene definido el tipo de amortización");
        }
        if (prestamo.getTasa() == null) {
            throw new IncomeException("El préstamo " + idPrestamo + " no tiene definida la tasa");
        }

        calculo.prestamo = prestamo;
        calculo.tipoAmortizacion = prestamo.getTipoAmortizacion();
        calculo.tasaMensual = prestamo.getTasa() / 100.0 / 12.0;
        calculo.valorAbono = valorAbono;

        // --- Validación 3: préstamo al día -------------------------------------
        // Primero se dejan correr los saldos reales: la autocorrección de §6.2 pasa a PAGADA
        // cualquier cuota que ya esté liquidada según PGPR, y así no bloquea el abono.
        // ⚠️ SOLO para aplicar() (soloLectura=false): calcularSaldosRealesCuota PERSISTE la
        // autocorrección. simular() usa la variante pura y NUNCA corre este loop — bug
        // corregido 2026-08-28, ver javadoc de MotorPagoPrestamoService.calcularSaldosCuota.
        List<DetallePrestamo> preliminar =
            detallePrestamoDaoService.selectCuotasPendientesByPrestamoOrdenadas(idPrestamo);
        if (!soloLectura) {
            for (DetallePrestamo cuota : preliminar) {
                motorPagoPrestamoService.calcularSaldosRealesCuota(cuota);
            }
        }

        // En modo lectura no hay nada que la autocorrección haya podido cambiar: reusar
        // `preliminar` evita una segunda consulta idéntica.
        List<DetallePrestamo> pendientes = soloLectura
            ? preliminar
            : detallePrestamoDaoService.selectCuotasPendientesByPrestamoOrdenadas(idPrestamo);
        if (pendientes == null || pendientes.isEmpty()) {
            throw new IncomeException(ERR_SIN_CUOTAS_PENDIENTES
                + ": el préstamo " + idPrestamo + " no tiene cuotas pendientes que re-amortizar");
        }

        for (DetallePrestamo cuota : pendientes) {
            if (cuota.getEstado() != null && cuota.getEstado() == EstadoCuotaPrestamo.PARCIAL) {
                throw new IncomeException(ERR_PRESTAMO_NO_AL_DIA
                    + ": El préstamo tiene cuotas vencidas o parciales; regularícelas antes de abonar"
                    + " a capital (cuota #" + cuota.getNumeroCuota() + " está PARCIAL)");
            }
            if (cuota.getFechaVencimiento() != null
                    && cuota.getFechaVencimiento().toLocalDate().isBefore(fecha)) {
                throw new IncomeException(ERR_PRESTAMO_NO_AL_DIA
                    + ": El préstamo tiene cuotas vencidas o parciales; regularícelas antes de abonar"
                    + " a capital (cuota #" + cuota.getNumeroCuota() + " venció el "
                    + cuota.getFechaVencimiento().toLocalDate() + ")");
            }
        }
        calculo.cuotasAHistorizar = pendientes;

        // --- Validación 4: el abono no puede cubrir todo el capital -------------
        // Bug corregido 2026-08-28: antes se llamaba a calcularSaldosRealesCuota acá también,
        // así que hasta la rama de solo-lectura de simular() terminaba escribiendo en DTPR.
        // El capital usa el mismo cálculo compartido que la reestructuración y la precancelación
        // (pedido 8, segunda ola) — PagoPrestamoService.calcularSaldoCapitalPendiente, que
        // reconstruye desde CRD.PGPR sin persistir. El interés no tiene ese método compartido
        // (solo existe para capital), así que se suma con la variante pura por cuota,
        // MotorPagoPrestamoService.calcularSaldosCuota — mismo cálculo que calcularSaldosRealesCuota,
        // sin el efecto de autocorrección.
        double saldoCapitalPendiente = pagoPrestamoService.calcularSaldoCapitalPendiente(pendientes);
        double interesPendienteActual = 0.0;
        for (DetallePrestamo cuota : pendientes) {
            com.saa.ejb.crd.service.dto.SaldosCuota saldos =
                motorPagoPrestamoService.calcularSaldosCuota(cuota);
            interesPendienteActual += saldos.getSaldoInteres();
        }
        saldoCapitalPendiente = redondear(saldoCapitalPendiente);
        calculo.saldoCapitalPendiente = saldoCapitalPendiente;

        if (!(valorAbono < saldoCapitalPendiente - TOLERANCIA)) {
            throw new IncomeException(ERR_ABONO_CUBRE_CAPITAL
                + ": El abono $" + valorAbono + " cubre todo el capital pendiente $"
                + saldoCapitalPendiente + "; use la precancelación");
        }

        calculo.nuevoCapital = redondear(saldoCapitalPendiente - valorAbono);

        // --- Parámetros de la re-amortización ----------------------------------
        DetallePrestamo primeraPendiente = pendientes.get(0);
        DetallePrestamo ultimaHistorizada = pendientes.get(pendientes.size() - 1);

        double cuotaVigente = nvl(prestamo.getValorCuota());
        if (cuotaVigente <= TOLERANCIA) {
            cuotaVigente = nvl(primeraPendiente.getCuota());
        }
        calculo.cuotaVigente = redondear(cuotaVigente);

        // Bug corregido 2026-08-29: el seguro de incendio es fijo por cuota (confirmado
        // contra el usuario — a diferencia del desgravamen, no depende del saldo) pero se
        // PRESERVA por NÚMERO DE CUOTA al re-amortizar, no se toma de una sola fila y se
        // aplica a todas. El valor de la cuota N vieja va a la cuota N nueva.
        for (DetallePrestamo historizada : pendientes) {
            if (historizada.getNumeroCuota() != null) {
                calculo.seguroPorNumeroCuota.put(Math.round(historizada.getNumeroCuota()),
                    redondear(nvl(historizada.getValorSeguroIncendio())));
            }
        }
        calculo.seguroUltimaHistorizadaFallback = redondear(nvl(ultimaHistorizada.getValorSeguroIncendio()));

        // Numeración: continúa desde la última cuota que QUEDA en DTPR
        double numeroInicial = numeroPrimeraCuotaNueva(idPrestamo, pendientes);

        // Calendario: se conservan los vencimientos originales de las cuotas reemplazadas
        List<LocalDateTime> vencimientos = new ArrayList<>();
        for (DetallePrestamo cuota : pendientes) {
            vencimientos.add(cuota.getFechaVencimiento());
        }

        int n = calcularNumeroDeCuotas(calculo, modalidad, primeraPendiente, pendientes.size());

        // --- Tabla proyectada ---------------------------------------------------
        construirTablaProyectada(calculo, modalidad, n, numeroInicial, vencimientos, primeraPendiente);

        // Requerimiento futuro (2026-08-29, anotado y NO implementado — no se construye el
        // proceso de reembolso): en modalidad 1 (acorta plazo), si la tabla nueva tiene menos
        // cuotas que las historizadas, las historizadas que quedan sin cuota nueva
        // correspondiente (índice >= n) ya tenían seguro de incendio cobrado o previsto. Ese
        // monto queda "liberado" y es el insumo que un futuro proceso de reembolso a la
        // aseguradora necesitaría — se deja calculado y expuesto en el resultado del abono.
        double seguroLiberado = 0.0;
        if (modalidad == 1 && n < pendientes.size()) {
            for (int idx = n; idx < pendientes.size(); idx++) {
                seguroLiberado += nvl(pendientes.get(idx).getValorSeguroIncendio());
            }
        }
        calculo.seguroIncendioLiberado = redondear(seguroLiberado);

        double interesNuevo = 0.0;
        for (CuotaProyectada proyectada : calculo.tabla) {
            interesNuevo += nvl(proyectada.getInteres());
        }
        calculo.ahorroIntereses = redondear(interesPendienteActual - interesNuevo);
        calculo.cuotaNueva = calculo.tabla.isEmpty() ? calculo.cuotaVigente
            : redondear(nvl(calculo.tabla.get(0).getCuota()));

        // Plazo: en modalidad 1 = cuotas que quedan (numeroCuota > 0) + n; en modalidad 2 no cambia
        if (modalidad == 1) {
            calculo.plazoNuevo = contarCuotasQueQuedan(idPrestamo, pendientes) + n;
        } else {
            calculo.plazoNuevo = prestamo.getPlazo() != null ? prestamo.getPlazo() : n;
        }

        System.out.println("  Cálculo del abono - Capital pendiente: $" + saldoCapitalPendiente
            + " - Nuevo capital: $" + calculo.nuevoCapital
            + " - Cuotas nuevas: " + n
            + " - Cuota: " + calculo.cuotaVigente + " → " + calculo.cuotaNueva);

        return calculo;
    }

    /** Cantidad de cuotas nuevas según la modalidad y el tipo de amortización. */
    private int calcularNumeroDeCuotas(CalculoAbono calculo, int modalidad,
            DetallePrestamo primeraPendiente, int cuotasReemplazadas) throws Throwable {

        double i = calculo.tasaMensual;
        double capital = calculo.nuevoCapital;

        if (modalidad == 2) {
            // Mantener el plazo: tantas cuotas como las reemplazadas
            return cuotasReemplazadas;
        }

        // Modalidad 1: mantener la cuota y reducir el plazo
        if (calculo.tipoAmortizacion == 2) {
            // Alemana: el capital por cuota es fijo
            double capitalPorCuota = redondear(nvl(primeraPendiente.getCapital()));
            if (capitalPorCuota <= TOLERANCIA) {
                throw new IncomeException("La cuota vigente no tiene capital definido;"
                    + " no se puede reducir el plazo");
            }
            return acotar((int) Math.ceil(capital / capitalPorCuota));
        }

        // Francesa
        double c = calculo.cuotaVigente;
        if (i <= 0.0) {
            // Sin interés la amortización es lineal
            return acotar((int) Math.ceil(capital / c));
        }
        if (capital * i >= c - TOLERANCIA) {
            throw new IncomeException(ERR_CUOTA_NO_CUBRE_INTERES
                + ": la cuota vigente $" + c + " no cubre el interés mensual $"
                + redondear(capital * i) + " del nuevo capital; use la modalidad 2");
        }
        double n = -Math.log(1 - (capital * i / c)) / Math.log(1 + i);
        return acotar((int) Math.ceil(n));
    }

    /** Construye la tabla proyectada aplicando la amortización que corresponda. */
    private void construirTablaProyectada(CalculoAbono calculo, int modalidad, int n,
            double numeroInicial, List<LocalDateTime> vencimientos, DetallePrestamo primeraPendiente)
            throws Throwable {

        double i = calculo.tasaMensual;
        double saldo = calculo.nuevoCapital;

        // Cuota fija de la modalidad francesa
        double cuotaFija;
        if (calculo.tipoAmortizacion == 2) {
            cuotaFija = 0.0; // alemana: la cuota varía
        } else if (modalidad == 1) {
            cuotaFija = calculo.cuotaVigente;
        } else if (i <= 0.0) {
            cuotaFija = redondear(calculo.nuevoCapital / n);
        } else {
            cuotaFija = redondear(calculo.nuevoCapital * i / (1 - Math.pow(1 + i, -n)));
        }

        // Capital fijo de la modalidad alemana
        double capitalFijo = 0.0;
        if (calculo.tipoAmortizacion == 2) {
            capitalFijo = modalidad == 1
                ? redondear(nvl(primeraPendiente.getCapital()))
                : redondear(calculo.nuevoCapital / n);
        }

        for (int k = 1; k <= n; k++) {
            double interes = redondear(saldo * i);
            double capitalCuota;

            if (calculo.tipoAmortizacion == 2) {
                capitalCuota = capitalFijo;
            } else {
                capitalCuota = redondear(cuotaFija - interes);
            }

            if (k == n) {
                // La última cuota absorbe el residuo exacto
                capitalCuota = redondear(saldo);
            }
            if (capitalCuota > saldo) {
                capitalCuota = redondear(saldo);
            }

            double saldoDespues = redondear(saldo - capitalCuota);
            double cuota = redondear(capitalCuota + interes);

            // Bug corregido 2026-08-29: el desgravamen NO es un valor fijo por cuota (era el
            // defecto del 2026-08-28, corregido a medias). Es saldo de capital × 1.12/1000,
            // igual que CalculadoraAmortizacionServiceImpl.calcularFrancesa/calcularAlemana —
            // MISMA constante compartida, no una copia. CRÍTICO: sobre `saldo` (el saldo ANTES
            // de amortizar esta cuota, la misma variable que ya usa `interes` arriba), NUNCA
            // sobre `saldoDespues`: usar el de después correría todo el cuadro un período, la
            // prima de la cuota k reflejaría la exposición de la k+1.
            double desgravamen = redondear(saldo * CalculadoraAmortizacionServiceImpl.FACTOR_DESGRAVAMEN_SOBRE_SALDO);

            double numeroCuotaActual = numeroInicial + (k - 1);
            // Bug corregido 2026-08-29: el seguro de incendio SÍ es un monto fijo por cuota
            // (confirmado contra el motor: CalculadoraAmortizacionServiceImpl.fila() lo aplica
            // igual en cada fila, sin factor sobre saldo — no tiene el defecto del desgravamen),
            // pero debe PRESERVARSE por número de cuota (la cuota N vieja financia la cuota N
            // nueva), no copiarse de una sola fila histórica hacia todas las nuevas. Criterio
            // defensivo para el caso borde (cuota nueva sin número correspondiente en el mapa,
            // p.ej. si el abono alarga el plazo más allá de lo historizado): usar el valor de
            // la última cuota historizada como respaldo y dejar constancia por log — nunca NPE
            // ni 0 silencioso.
            Long claveSeguro = Math.round(numeroCuotaActual);
            Double seguroCuota = calculo.seguroPorNumeroCuota.get(claveSeguro);
            if (seguroCuota == null) {
                seguroCuota = calculo.seguroUltimaHistorizadaFallback;
                System.out.println("AbonoCapitalPrestamoServiceImpl.construirTablaProyectada: "
                    + "cuota nueva #" + numeroCuotaActual + " sin correspondencia en el mapa de "
                    + "seguro de incendio por número de cuota; se usa el respaldo de la última "
                    + "cuota historizada ($" + seguroCuota + ").");
            }

            CuotaProyectada proyectada = new CuotaProyectada();
            proyectada.setNumeroCuota(numeroCuotaActual);
            proyectada.setFechaVencimiento(vencimientoDeLaCuota(vencimientos, k - 1));
            proyectada.setCapital(capitalCuota);
            proyectada.setInteres(interes);
            proyectada.setCuota(cuota);
            proyectada.setSaldoCapital(saldoDespues);
            proyectada.setDesgravamen(desgravamen);
            proyectada.setSeguroIncendio(seguroCuota);
            proyectada.setTotal(redondear(cuota + desgravamen + seguroCuota));
            calculo.tabla.add(proyectada);

            saldo = saldoDespues;
        }
    }

    /**
     * Vencimiento de la cuota nueva en la posición indicada. Se reutilizan los vencimientos
     * originales de las cuotas reemplazadas (conserva el calendario del préstamo); si hicieran
     * falta más, se agregan meses tomando el último día de cada mes.
     */
    private LocalDateTime vencimientoDeLaCuota(List<LocalDateTime> vencimientos, int indice) throws Throwable {
        if (indice < vencimientos.size()) {
            return vencimientos.get(indice);
        }
        LocalDateTime base = vencimientos.get(vencimientos.size() - 1);
        LocalDateTime resultado = base;
        for (int k = vencimientos.size(); k <= indice; k++) {
            LocalDate temp = resultado.toLocalDate().plusMonths(1);
            LocalDate ultimoDia = fechaService.ultimoDiaMesAnioLocal(
                Long.valueOf(temp.getMonthValue()), Long.valueOf(temp.getYear()));
            resultado = ultimoDia.atTime(base.toLocalTime());
        }
        return resultado;
    }

    /** Número de la primera cuota nueva: (mayor numeroCuota que QUEDA en DTPR) + 1. */
    private double numeroPrimeraCuotaNueva(Long idPrestamo, List<DetallePrestamo> aHistorizar) throws Throwable {
        List<DetallePrestamo> todas = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
        double maximo = -1.0;
        for (DetallePrestamo cuota : todas) {
            if (contiene(aHistorizar, cuota.getCodigo()) || cuota.getNumeroCuota() == null) {
                continue;
            }
            if (cuota.getNumeroCuota() > maximo) {
                maximo = cuota.getNumeroCuota();
            }
        }
        return maximo < 0 ? 1.0 : maximo + 1.0;
    }

    /** Cuotas que quedan en DTPR con numeroCuota &gt; 0 (la cuota 0 de gracia no cuenta al plazo). */
    private long contarCuotasQueQuedan(Long idPrestamo, List<DetallePrestamo> aHistorizar) throws Throwable {
        List<DetallePrestamo> todas = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
        long cuenta = 0;
        for (DetallePrestamo cuota : todas) {
            if (contiene(aHistorizar, cuota.getCodigo())) {
                continue;
            }
            if (cuota.getNumeroCuota() != null && cuota.getNumeroCuota() > 0) {
                cuenta++;
            }
        }
        return cuenta;
    }

    private boolean contiene(List<DetallePrestamo> lista, Long codigo) {
        if (codigo == null) {
            return false;
        }
        for (DetallePrestamo cuota : lista) {
            if (codigo.equals(cuota.getCodigo())) {
                return true;
            }
        }
        return false;
    }

    // ========================================================================
    // Construcción de entidades
    // ========================================================================

    /** Arma una cuota nueva de DTPR con TODOS los campos del invariante llenos. */
    private DetallePrestamo construirCuota(Prestamo prestamo, CuotaProyectada proyectada, CalculoAbono calculo) {
        DetallePrestamo cuota = new DetallePrestamo();
        cuota.setPrestamo(prestamo);
        cuota.setNumeroCuota(proyectada.getNumeroCuota());
        cuota.setFechaVencimiento(proyectada.getFechaVencimiento());
        cuota.setCapital(proyectada.getCapital());
        cuota.setInteres(proyectada.getInteres());
        cuota.setCuota(proyectada.getCuota());

        // Invariante: saldoInicialCapital = capital + saldoCapital + saldoOtros
        cuota.setSaldoInicialCapital(redondear(proyectada.getCapital() + proyectada.getSaldoCapital()));
        cuota.setSaldoCapital(proyectada.getSaldoCapital());

        // Bug corregido 2026-08-29: esto persistía calculo.desgravamenPorCuota (el valor FIJO
        // copiado de la última cuota histórica) en CADA cuota real de DTPR, aunque
        // construirTablaProyectada ya calculaba el desgravamen correcto (variable sobre saldo)
        // por fila desde ayer — la vista previa y lo que se grababa quedaban DESINCRONIZADOS.
        // Debe leer proyectada.getDesgravamen(), la fila que corresponde a ESTA cuota.
        double desgravamenCuota = nvl(proyectada.getDesgravamen());
        cuota.setDesgravamen(desgravamenCuota);
        cuota.setDesgravamenOriginal(desgravamenCuota);
        cuota.setDesgravamenFirmado(desgravamenCuota);
        cuota.setDesgravamenDiferido(0.0);
        cuota.setValorDiferido(0.0);
        // Bug corregido 2026-08-29: mismo defecto que el desgravamen arriba, pero con el
        // seguro de incendio — se persistía calculo.seguroPorCuota (un único valor copiado de
        // la última cuota histórica) en todas las cuotas nuevas. Debe leer
        // proyectada.getSeguroIncendio(), que ya resolvió construirTablaProyectada con el
        // valor de LA cuota histórica correspondiente por número (o el respaldo defensivo).
        double seguroCuota = nvl(proyectada.getSeguroIncendio());
        cuota.setValorSeguroIncendio(seguroCuota);
        cuota.setOtrosSeguros(0.0);

        double total = redondear(proyectada.getCuota() + desgravamenCuota + seguroCuota);
        cuota.setTotal(total);
        cuota.setTotalConSeguro(total);
        cuota.setSaldo(total);

        cuota.setMora(0.0);
        cuota.setInteresVencido(0.0);
        cuota.setSaldoInteres(proyectada.getInteres());
        cuota.setSaldoMora(0.0);
        cuota.setSaldoInteresVencido(0.0);
        cuota.setMoraCalculada(0.0);
        cuota.setDiasMora(0L);

        cuota.setAbono(0.0);
        cuota.setSaldoOtros(0.0);
        cuota.setCapitalPagado(0.0);
        cuota.setInteresPagado(0.0);
        cuota.setDesgravamenPagado(0.0);
        cuota.setMoraPagado(0.0);
        cuota.setInteresVendidoPagado(0.0);
        cuota.setFechaPagado(null);

        cuota.setEstado((long) EstadoCuotaPrestamo.PENDIENTE);
        cuota.setIdEstado((long) EstadoCuotaPrestamo.PENDIENTE);
        cuota.setFechaRegistro(LocalDateTime.now());
        return cuota;
    }

    /** Copia campo a campo una cuota de DTPR a su espejo en HDTP. */
    private HistDetallePrestamo copiarAHistorico(DetallePrestamo cuota, EventoPrestamo evento,
            String motivo, String usuario) {

        HistDetallePrestamo h = new HistDetallePrestamo();
        h.setCodigoOriginal(cuota.getCodigo());
        h.setEventoPrestamo(evento);
        h.setMotivo(motivo);
        h.setFechaRegistroHist(LocalDateTime.now());
        h.setUsuarioHist(usuario);

        h.setPrestamo(cuota.getPrestamo());
        h.setNumeroCuota(cuota.getNumeroCuota());
        h.setFechaVencimiento(cuota.getFechaVencimiento());
        h.setCapital(cuota.getCapital());
        h.setInteres(cuota.getInteres());
        h.setMora(cuota.getMora());
        h.setInteresVencido(cuota.getInteresVencido());
        h.setSaldoCapital(cuota.getSaldoCapital());
        h.setSaldoInteres(cuota.getSaldoInteres());
        h.setSaldoMora(cuota.getSaldoMora());
        h.setSaldoInteresVencido(cuota.getSaldoInteresVencido());
        h.setFechaPagado(cuota.getFechaPagado());
        h.setAbono(cuota.getAbono());
        h.setCapitalPagado(cuota.getCapitalPagado());
        h.setInteresPagado(cuota.getInteresPagado());
        h.setDesgravamen(cuota.getDesgravamen());
        h.setCuota(cuota.getCuota());
        h.setSaldo(cuota.getSaldo());
        h.setSaldoOtros(cuota.getSaldoOtros());
        h.setDesgravamenFirmado(cuota.getDesgravamenFirmado());
        h.setDesgravamenDiferido(cuota.getDesgravamenDiferido());
        h.setDesgravamenOriginal(cuota.getDesgravamenOriginal());
        h.setValorDiferido(cuota.getValorDiferido());
        h.setTotal(cuota.getTotal());
        h.setMoraPagado(cuota.getMoraPagado());
        h.setDesgravamenPagado(cuota.getDesgravamenPagado());
        h.setInteresVendidoPagado(cuota.getInteresVendidoPagado());
        h.setMoraCalculada(cuota.getMoraCalculada());
        h.setDiasMora(cuota.getDiasMora());
        h.setEstado(cuota.getEstado());
        h.setFechaRegistro(cuota.getFechaRegistro());
        h.setUsuarioRegistro(cuota.getUsuarioRegistro());
        h.setIdEstado(cuota.getIdEstado());
        h.setCodigoExterno(cuota.getCodigoExterno());
        h.setOtrosSeguros(cuota.getOtrosSeguros());
        h.setTotalConSeguro(cuota.getTotalConSeguro());
        h.setValorSeguroIncendio(cuota.getValorSeguroIncendio());
        h.setSaldoInicialCapital(cuota.getSaldoInicialCapital());
        return h;
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private int acotar(int n) {
        if (n < 1) {
            return 1;
        }
        return Math.min(n, MAX_CUOTAS_NUEVAS);
    }

    private boolean esEstadoTerminalPrestamo(Long idEstado) {
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

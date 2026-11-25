package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.service.MotorPagoPrestamoService;
import com.saa.ejb.crd.service.PrelacionCuotas;
import com.saa.ejb.crd.service.PrelacionCuotasService;
import com.saa.ejb.crd.service.ProvisionInteresService;
import com.saa.ejb.crd.service.dto.ResultadoReimputacion;
import com.saa.ejb.crd.service.dto.ResultadoReversoProvision;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.PagoPrestamo;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @see PrelacionCuotasService
 * @author Sistema SAA
 * @since 2026-11-21
 */
@Stateless
public class PrelacionCuotasServiceImpl implements PrelacionCuotasService {

    /** Origen de los MVIC de la provisión (re-provisión y reverso) que genera la re-imputación. */
    public static final String ORIGEN_PROVISION = "C33_REIMPUTACION";

    private static final int MAX_MOTIVO = 500;
    private static final int MAX_OBSERVACION = 2000;
    private static final int MAX_USUARIO_ANULACION = 50;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private MotorPagoPrestamoService motorPagoPrestamoService;

    @EJB
    private ProvisionInteresService provisionInteresService;

    @EJB
    private com.saa.ejb.crd.service.ProcesoMoraPrestamoService procesoMoraPrestamoService;

    /** Lote 10: para deshacer la transacción de un préstamo que quedaría roto (rollback por préstamo). */
    @Resource
    private SessionContext ctx;

    /** Auto-inyección: el lote llama a cada préstamo a través del proxy (REQUIRES_NEW). */
    @EJB
    private PrelacionCuotasService self;

    // ========================================================================
    // Punto 4 — re-imputación en orden
    // ========================================================================

    @Override
    public ResultadoReimputacion reimputarPagosEnOrden(Long idPrestamo, LocalDate fecha, String usuario) throws Throwable {
        return reimputarPagosEnOrden(idPrestamo, fecha, usuario, null, false);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ResultadoReimputacion reimputarPagosEnOrdenNuevaTransaccion(Long idPrestamo, LocalDate fecha, String usuario,
            Long idEmpresa, boolean simular) throws Throwable {
        return reimputar(idPrestamo, fecha, usuario, idEmpresa, simular, true);
    }

    @Override
    public ResultadoReimputacion reimputarPagosEnOrden(Long idPrestamo, LocalDate fecha, String usuario, Long idEmpresa,
            boolean simular) throws Throwable {
        return reimputar(idPrestamo, fecha, usuario, idEmpresa, simular, false);
    }

    /**
     * Lote 10 (incidente C33 del 2026-11-25). Lee los pagos POR CUOTA (no por PGPR.PRSTCDGO), decide con el MISMO control del cierre
     * (selectControlPrelacionAlCorte, acotado al préstamo), valida en memoria antes de grabar y, con {@code rollbackSiQuedaRoto}
     * (REQUIRES_NEW por préstamo), deshace el préstamo si después de aplicar el control lo sigue viendo roto.
     */
    private ResultadoReimputacion reimputar(Long idPrestamo, LocalDate fecha, String usuario, Long idEmpresa,
            boolean simular, boolean rollbackSiQuedaRoto) throws Throwable {
        System.out.println("PrelacionCuotasService.reimputarPagosEnOrden - Préstamo: " + idPrestamo + " - al " + fecha
            + (simular ? " (simulación)" : ""));
        if (idPrestamo == null) {
            throw new IncomeException("PARAMETRO_INVALIDO: idPrestamo es obligatorio");
        }
        if (!simular && (usuario == null || usuario.trim().isEmpty())) {
            throw new IncomeException("PARAMETRO_INVALIDO: usuario es obligatorio para re-imputar");
        }
        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        LocalDateTime corte = dia.atTime(java.time.LocalTime.MAX);
        LocalDate desde = PrelacionCuotas.desde();

        ResultadoReimputacion resultado = new ResultadoReimputacion();
        resultado.setIdPrestamo(idPrestamo);
        resultado.setFecha(dia);
        resultado.setSimulacion(simular);

        List<DetallePrestamo> cuotas = cuotasOrdenadas(idPrestamo);
        // Lote 10: los pagos se leen POR CUOTA (selectVigentesByPrestamo navega por la cuota), no por PGPR.PRSTCDGO.
        List<PagoPrestamo> vigentes = pagoPrestamoDaoService.selectVigentesByPrestamo(idPrestamo);
        Map<Long, List<PagoPrestamo>> pagosAlCorte = pagosAlCortePorCuota(cuotas, vigentes, corte);
        PrelacionCuotas.Rotura rotura = PrelacionCuotas.detectarRotura(cuotas, pagosAlCorte, desde);
        // Fuente única de los criterios: el control del cierre, acotado a este préstamo.
        List<Object[]> filasControl = detallePrestamoDaoService.selectControlPrelacionAlCorte(desde, dia, idPrestamo);
        if (rotura == null && filasControl.isEmpty()) {
            resultado.setMensaje("El préstamo " + idPrestamo + " cumple la prelación al " + dia + ": nada que re-imputar.");
            return resultado;
        }
        if (rotura != null) {
            resultado.setRotura(rotura.tipo);
            resultado.setDetalleRotura(rotura.texto());
        }
        int sinPrestamo = contarPagosSinPrestamo(idPrestamo, vigentes);
        if (sinPrestamo > 0) {
            resultado.setMensaje("RECHAZADO: el préstamo " + idPrestamo + " tiene " + sinPrestamo + " PGPR vigente(s) con PRSTCDGO nulo o"
                + " distinto del de su cuota. Corregir el dato (sql 397) antes de re-imputar: no se toca nada.");
            return resultado;
        }
        if (filasControl.isEmpty()) {
            resultado.setMensaje("No se re-imputa: la herramienta ve una rotura (" + rotura.texto() + ") pero el control del cierre no la"
                + " reporta para el préstamo " + idPrestamo + " al " + dia + " (fuera de su universo o criterio distinto). Revisar con crédito.");
            return resultado;
        }
        if (rotura == null) {
            resultado.setMensaje("No se re-imputa: el control del cierre reporta " + textoFilaControl(filasControl.get(0))
                + " en el préstamo " + idPrestamo + " pero la herramienta no la reproduce. Revisar con crédito.");
            return resultado;
        }

        // Pagos que se mueven: no exentos, con capital o interés, desde la cuota donde empieza la rotura. El resto (los
        // exentos, los de solo seguro/desgravamen —el HS de la carga Petro va por cuota— y los de cuotas anteriores)
        // queda fijo y descuenta lo que debe cada cuota.
        int inicio = rotura.indiceInicio;
        List<PrelacionCuotas.CuotaPlan> cuotasPlan = new ArrayList<>();
        List<PrelacionCuotas.PagoPlan> pagosPlan = new ArrayList<>();
        Map<Long, PagoPrestamo> movibles = new LinkedHashMap<>();
        java.util.Set<Long> noMovibles = new java.util.HashSet<>();
        Map<Long, DetallePrestamo> cuotaPorId = new LinkedHashMap<>();
        for (int i = 0; i < cuotas.size(); i++) {
            DetallePrestamo c = cuotas.get(i);
            cuotaPorId.put(c.getCodigo(), c);
            double[] pendiente = PrelacionCuotas.valoresCuota(c);
            List<PagoPrestamo> pagos = pagosAlCorte.get(c.getCodigo());
            if (pagos != null) {
                for (PagoPrestamo p : pagos) {
                    // Nunca se vacía una cuota cerrada por regla (7, o 4 anterior al arranque): quedaría reabierta.
                    boolean mueve = i >= inicio && !PrelacionCuotas.estaCerradaPorRegla(c, desde)
                        && !PrelacionCuotas.esPagoExento(p)
                        && PrelacionCuotas.nvl(p.getCapitalPagado()) + PrelacionCuotas.nvl(p.getInteresPagado())
                            > PrelacionCuotas.TOLERANCIA;
                    if (mueve) {
                        movibles.put(p.getCodigo(), p);
                        pagosPlan.add(new PrelacionCuotas.PagoPlan(p.getCodigo(), c.getCodigo(), p.getFecha(),
                            PrelacionCuotas.componentes(p)));
                    } else {
                        // Lote 15: pago exento o fijo con capital/interés = la cuota no cede ni recibe; el plan no puede pasar por encima.
                        if (PrelacionCuotas.esPagoExento(p) || (i >= inicio
                            && PrelacionCuotas.nvl(p.getCapitalPagado()) + PrelacionCuotas.nvl(p.getInteresPagado()) > PrelacionCuotas.TOLERANCIA)) {
                            noMovibles.add(c.getCodigo());
                        }
                        double[] comp = PrelacionCuotas.componentes(p);
                        for (int k = 0; k < PrelacionCuotas.COMPONENTES; k++) {
                            pendiente[k] = Math.max(0.0, PrelacionCuotas.redondear(pendiente[k] - comp[k]));
                        }
                    }
                }
            }
            boolean cerrada = i < inicio || PrelacionCuotas.estaCerradaPorRegla(c, desde);
            if (i >= inicio && cerrada) {
                noMovibles.add(c.getCodigo());
            }
            cuotasPlan.add(new PrelacionCuotas.CuotaPlan(c.getCodigo(),
                c.getNumeroCuota() != null ? c.getNumeroCuota() : Double.MAX_VALUE, cerrada, pendiente));
        }

        // Lote 15 (ajuste): una cuota PAGADA o CANCELADA_ANTICIPADA que no cede ningún pago movible (sin pagos que la respalden,
        // "marca") es una isla aunque no esté cerrada por regla: planificar() la ve pendiente, pero si la plata se acaba antes
        // de llegar a ella, el dinero de las posteriores pasa por encima y la cuota sigue pagada entre cuotas en mora.
        java.util.Set<Long> conOrigen = new java.util.HashSet<>();
        for (PrelacionCuotas.PagoPlan pp : pagosPlan) {
            conOrigen.add(pp.idCuota);
        }
        for (int i = inicio; i < cuotas.size(); i++) {
            DetallePrestamo c = cuotas.get(i);
            long e = c.getEstado() != null ? c.getEstado().longValue() : -1L;
            if ((e == com.saa.rubros.EstadoCuotaPrestamo.PAGADA || e == com.saa.rubros.EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) && !conOrigen.contains(c.getCodigo())) {
                noMovibles.add(c.getCodigo());
            }
        }

        PrelacionCuotas.Plan plan = PrelacionCuotas.planificar(cuotasPlan, pagosPlan);
        resultado.setPagosSinCambio(plan.sinCambio.size());
        resultado.setSinDestino(plan.sinDestino);
        for (int k = 0; k < PrelacionCuotas.COMPONENTES; k++) {
            if (Math.abs(plan.deltaComponentes[k]) > 0.004) {
                resultado.getDeltaComponentes().put(PrelacionCuotas.NOMBRES[k], plan.deltaComponentes[k]);
            }
        }
        for (Map.Entry<Long, List<PrelacionCuotas.Asignacion>> e : plan.asignaciones.entrySet()) {
            PagoPrestamo original = movibles.get(e.getKey());
            StringBuilder linea = new StringBuilder("PGPR " + e.getKey() + " (cuota #"
                + entero(original.getNumeroCuota()) + ", " + (original.getFecha() != null ? original.getFecha().toLocalDate() : "sin fecha")
                + ", $" + PrelacionCuotas.formato(PrelacionCuotas.nvl(original.getValor())) + ") ->");
            for (PrelacionCuotas.Asignacion a : e.getValue()) {
                DetallePrestamo destino = cuotaPorId.get(a.idCuota);
                linea.append(" cuota #").append(entero(destino != null ? destino.getNumeroCuota() : null)).append(" [")
                    .append(PrelacionCuotas.texto(a.componentes)).append("]");
            }
            resultado.getMovimientos().add(linea.toString());
        }

        if (!plan.tieneCambios()) {
            resultado.setMensaje("Prelación rota al " + dia + " (" + rotura.texto() + "), pero re-aplicar en orden no"
                + " cambia el reparto: no hay pagos que mover (exentos, de solo seguro/desgravamen, o la cuota está"
                + " saldada por un pago posterior al corte). Revisar con crédito.");
            return resultado;
        }
        if (plan.sinDestino > PrelacionCuotas.TOLERANCIA) {
            resultado.setMensaje("No se re-imputa: $" + PrelacionCuotas.formato(plan.sinDestino) + " no encuentran cuota"
                + " con saldo al re-aplicar en orden (dinero sin destino). Revisar con crédito.");
            return resultado;
        }
        // Lote 15 (incidente 2026-11-25, restaurado con el sql 405): si el plan lleva plata a una cuota anterior a una cuota
        // NO movible sacándola de un pago posterior a ella, la dejaría pagada entre cuotas en mora. Se rechaza, sin escribir.
        java.util.SortedSet<Double> saltadas = PrelacionCuotas.cuotasSaltadas(cuotasPlan, pagosPlan, plan, noMovibles);
        if (!saltadas.isEmpty()) {
            StringBuilder nums = new StringBuilder();
            for (Double n : saltadas) {
                nums.append(nums.length() > 0 ? ", " : "").append('#').append(n.longValue());
            }
            resultado.setMensaje("No se re-imputa: re-aplicar en orden saltaría la(s) cuota(s) " + nums + " (cerradas o con pagos exentos)"
                + " y las dejaría pagadas entre cuotas en mora. Revisar con crédito.");
            return resultado;
        }
        // Lote 10: validar ANTES de grabar, en memoria y con las mismas reglas: el estado que dejaría el plan no puede tener rotura.
        Map<Long, List<PagoPrestamo>> hipotetico = new LinkedHashMap<>();
        for (Map.Entry<Long, List<PagoPrestamo>> e : pagosAlCorte.entrySet()) {
            for (PagoPrestamo p : e.getValue()) {
                if (!plan.asignaciones.containsKey(p.getCodigo())) {
                    hipotetico.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(p);
                }
            }
        }
        for (Map.Entry<Long, List<PrelacionCuotas.Asignacion>> e : plan.asignaciones.entrySet()) {
            PagoPrestamo original = movibles.get(e.getKey());
            for (PrelacionCuotas.Asignacion a : e.getValue()) {
                DetallePrestamo destino = cuotaPorId.get(a.idCuota);
                hipotetico.computeIfAbsent(destino.getCodigo(), k -> new ArrayList<>()).add(copiaReimputada(original, destino, a.componentes));
            }
        }
        PrelacionCuotas.Rotura quedaria = PrelacionCuotas.detectarRotura(cuotas, hipotetico, desde);
        if (quedaria != null) {
            resultado.setRoturaResidual(quedaria.tipo + ": " + quedaria.texto());
            resultado.setMensaje("No se re-imputa" + (simular ? " (simulación)" : "") + ": re-aplicar en orden dejaría el préstamo roto ("
                + quedaria.texto() + "). Revisar con crédito.");
            return resultado;
        }
        if (simular) {
            resultado.setMensaje("Simulación: " + plan.asignaciones.size() + " pago(s) cambiarían de cuota ("
                + rotura.texto() + "). Diferencia por componente: " + PrelacionCuotas.texto(plan.deltaComponentes) + ".");
            return resultado;
        }

        // ---------------------------------------------------------------- aplicar
        LocalDateTime ahora = LocalDateTime.now();
        String motivo = truncar(PrelacionCuotas.MARCA_REIMPUTACION + ": re-imputación en orden al " + dia + " ("
            + rotura.texto() + ")", MAX_MOTIVO);
        List<PagoPrestamo> anulados = new ArrayList<>();
        for (Long id : plan.asignaciones.keySet()) {
            anulados.add(movibles.get(id));
        }
        StringBuilder avisos = new StringBuilder();
        // Provisión: primero re-provisionar lo que los anulados habían reversado (como el re-proceso de una carga).
        if (idEmpresa != null) {
            ResultadoReversoProvision rp = provisionInteresService.reProvisionarPorAnulacion(anulados, idEmpresa, dia,
                ORIGEN_PROVISION, idPrestamo, usuario);
            resultado.setAsientoReProvision(rp != null ? rp.getIdAsiento() : null);
        } else {
            avisos.append(" Sin empresa contable: la provisión de intereses no se ajustó (MVIC tipo 2/4).");
        }

        Map<Long, DetallePrestamo> tocadas = new LinkedHashMap<>();
        for (PagoPrestamo p : anulados) {
            p.setAnulado(1L);
            p.setUsuarioAnulacion(truncar(usuario, MAX_USUARIO_ANULACION));
            p.setFechaAnulacion(ahora);
            p.setMotivoAnulacion(motivo);
            pagoPrestamoDaoService.save(p, p.getCodigo());
            resultado.getPagosAnulados().add(p.getCodigo());
            if (p.getDetallePrestamo() != null) {
                tocadas.put(p.getDetallePrestamo().getCodigo(), cuotaPorId.getOrDefault(p.getDetallePrestamo().getCodigo(),
                    p.getDetallePrestamo()));
            }
        }

        List<PagoPrestamo> nuevos = new ArrayList<>();
        for (Map.Entry<Long, List<PrelacionCuotas.Asignacion>> e : plan.asignaciones.entrySet()) {
            PagoPrestamo original = movibles.get(e.getKey());
            for (PrelacionCuotas.Asignacion a : e.getValue()) {
                DetallePrestamo destino = cuotaPorId.get(a.idCuota);
                PagoPrestamo nuevo = copiaReimputada(original, destino, a.componentes);
                nuevo = pagoPrestamoDaoService.save(nuevo, null);
                nuevos.add(nuevo);
                resultado.getPagosCreados().add(nuevo.getCodigo());
                tocadas.put(destino.getCodigo(), destino);
            }
        }

        // Lote 10: el recálculo y la mora se hacen a la fecha real de la ejecución, nunca a un corte futuro.
        LocalDate fechaRecalculo = dia.isAfter(LocalDate.now()) ? LocalDate.now() : dia;
        // Cada cuota tocada se reconstruye desde sus pagos vigentes (todos, no solo al corte), en orden de número.
        List<DetallePrestamo> aRecalcular = new ArrayList<>(tocadas.values());
        aRecalcular.sort(Comparator.comparing(DetallePrestamo::getNumeroCuota, Comparator.nullsLast(Comparator.naturalOrder())));
        for (DetallePrestamo c : aRecalcular) {
            motorPagoPrestamoService.recalcularCuotaDesdePagos(c, fechaRecalculo);
            // Como la reapertura D-019: la cuota que quedó abierta lleva la mora a la fecha del corte (y EN_MORA solo
            // si a esa fecha ya vencía; recalcularCuotaDesdePagos la juzga con la fecha de hoy).
            if (c.getEstado() == null || (c.getEstado() != com.saa.rubros.EstadoCuotaPrestamo.PAGADA
                    && c.getEstado() != com.saa.rubros.EstadoCuotaPrestamo.CANCELADA_ANTICIPADA)) {
                procesoMoraPrestamoService.recalcularMoraCuotaALaFecha(c, fechaRecalculo);
            }
        }

        if (idEmpresa != null && !nuevos.isEmpty()) {
            ResultadoReversoProvision rv = provisionInteresService.reversarPorPagos(nuevos, idEmpresa, dia,
                ORIGEN_PROVISION, idPrestamo, usuario);
            resultado.setAsientoReversoProvision(rv != null ? rv.getIdAsiento() : null);
        }

        // Control: lo que queda después, con el MISMO control del cierre (Lote 10). Si el préstamo sigue roto, en la ejecución por
        // préstamo (REQUIRES_NEW) se deshace todo: no se deja un préstamo peor ni igual de roto.
        List<Object[]> filasDespues = detallePrestamoDaoService.selectControlPrelacionAlCorte(desde, dia, idPrestamo);
        String residual = filasDespues.isEmpty() ? null : textoFilaControl(filasDespues.get(0));
        resultado.setRoturaResidual(residual);
        if (residual != null && rollbackSiQuedaRoto) {
            ctx.setRollbackOnly();
            resultado.setAplicado(false);
            resultado.getPagosAnulados().clear();
            resultado.getPagosCreados().clear();
            resultado.setAsientoReProvision(null);
            resultado.setAsientoReversoProvision(null);
            resultado.setMensaje("NO APLICADO (se deshizo el préstamo): después de re-imputar el control del cierre lo sigue viendo roto ("
                + residual + "). Revisar con crédito.");
            System.out.println("  C33: " + resultado.getMensaje());
            return resultado;
        }
        resultado.setAplicado(true);
        if (!resultado.getDeltaComponentes().isEmpty()) {
            avisos.append(" El reparto por componente cambió (").append(PrelacionCuotas.texto(plan.deltaComponentes))
                .append("): falta el asiento de reclasificación de la aplicación.");
        }
        resultado.setMensaje("Re-imputados " + anulados.size() + " pago(s) en " + nuevos.size() + " registro(s) al " + dia
            + " (" + rotura.texto() + ")." + (residual != null ? " Queda: " + residual + "." : "") + avisos);
        System.out.println("  C33: " + resultado.getMensaje());
        return resultado;
    }

    /** Copia del PGPR original en otra cuota con otros componentes; misma identidad (tipo, usuario, fechas, carga…). */
    private PagoPrestamo copiaReimputada(PagoPrestamo original, DetallePrestamo destino, double[] c) {
        PagoPrestamo n = new PagoPrestamo();
        n.setPrestamo(original.getPrestamo() != null ? original.getPrestamo() : destino.getPrestamo());
        n.setDetallePrestamo(destino);
        n.setNumeroCuota(destino.getNumeroCuota());
        n.setCargaArchivo(original.getCargaArchivo());
        n.setFecha(original.getFecha());
        n.setValorSeguroIncendio(c[PrelacionCuotas.SEGURO]);
        n.setDesgravamen(c[PrelacionCuotas.DESGRAVAMEN]);
        n.setMoraPagada(c[PrelacionCuotas.MORA]);
        n.setInteresVencidoPagado(c[PrelacionCuotas.INTERES_VENCIDO]);
        n.setInteresPagado(c[PrelacionCuotas.INTERES]);
        n.setCapitalPagado(c[PrelacionCuotas.CAPITAL]);
        double total = 0.0;
        for (double v : c) {
            total += v;
        }
        n.setValor(PrelacionCuotas.redondear(total));
        n.setSaldoOtros(0.0);
        n.setObservacion(truncar((original.getObservacion() != null ? original.getObservacion() + " " : "") + "["
            + PrelacionCuotas.MARCA_REIMPUTACION + " de PGPR " + original.getCodigo() + ", cuota #"
            + entero(original.getNumeroCuota()) + "]", MAX_OBSERVACION));
        n.setTipo(original.getTipo());
        n.setEstado(original.getEstado() != null ? original.getEstado() : 1L);
        n.setIdEstado(1L);
        n.setFechaRegistro(original.getFechaRegistro() != null ? original.getFechaRegistro() : LocalDateTime.now());
        n.setUsuarioRegistro(original.getUsuarioRegistro());
        n.setEventoPrestamo(original.getEventoPrestamo());
        n.setAsiento(original.getAsiento());
        n.setAnulado(0L);
        n.setRutaDocumentoRespaldo(original.getRutaDocumentoRespaldo());
        return n;
    }

    // ========================================================================
    // Gancho de reapertura / anulación / re-proceso
    // ========================================================================

    @Override
    public String asegurarPrelacion(Long idPrestamo, LocalDate corte, String usuario, Long idEmpresa, String origen) {
        if (idPrestamo == null) {
            return null;
        }
        try {
            LocalDate dia = corte != null ? corte : LocalDate.now();
            List<DetallePrestamo> cuotas = cuotasOrdenadas(idPrestamo);
            PrelacionCuotas.Rotura rotura = PrelacionCuotas.detectarRotura(cuotas,
                pagosAlCortePorCuota(cuotas, pagoPrestamoDaoService.selectVigentesByPrestamo(idPrestamo), dia.atTime(java.time.LocalTime.MAX)), PrelacionCuotas.desde());
            if (rotura == null) {
                return null;
            }
            if (!PrelacionCuotas.reimputaAutomaticamente()) {
                String texto = "C33 préstamo " + idPrestamo + " tras " + origen + ": prelación rota al " + dia + " ("
                    + rotura.texto() + "); no se re-imputa (" + PrelacionCuotas.PROPIEDAD_REIMPUTAR + "=no). El cierre"
                    + " de cartera lo bloquea: POST /rest/prelacioncuotas/reimputar.";
                System.out.println("  ⚠️ " + texto);
                return texto;
            }
            ResultadoReimputacion r = reimputarPagosEnOrden(idPrestamo, dia,
                usuario != null && !usuario.trim().isEmpty() ? usuario : "SAA_C33", idEmpresa, false);
            String texto = "C33 préstamo " + idPrestamo + " tras " + origen + ": " + r.getMensaje();
            System.out.println("  C33: " + texto);
            return texto;
        } catch (Throwable e) {
            // Nunca bloquea la operación que llama: el control de cierre lo vuelve a ver.
            String texto = "C33 préstamo " + idPrestamo + " tras " + origen + ": no se pudo re-imputar ("
                + e.getMessage() + "). El cierre de cartera lo bloquea.";
            System.err.println("  ❌ " + texto);
            e.printStackTrace();
            return texto;
        }
    }

    // ========================================================================
    // Punto 6 — control al corte y lote
    // ========================================================================

    @Override
    public List<Object[]> controlAlCorte(LocalDate corte) throws Throwable {
        if (corte == null) {
            throw new IncomeException("PARAMETRO_INVALIDO: la fecha de corte es obligatoria");
        }
        return detallePrestamoDaoService.selectControlPrelacionAlCorte(PrelacionCuotas.desde(), corte);
    }

    @Override
    public String textoControlAlCorte(LocalDate corte) throws Throwable {
        List<Object[]> filas = controlAlCorte(corte);
        if (filas == null || filas.isEmpty()) {
            return null;
        }
        StringBuilder detalle = new StringBuilder();
        int prelacion = 0;
        int sobre = 0;
        for (int i = 0; i < filas.size(); i++) {
            Object[] f = filas.get(i);
            boolean esSobre = PrelacionCuotas.Rotura.SOBREIMPUTADA.equals(f[8]);
            if (esSobre) {
                sobre++;
            } else {
                prelacion++;
            }
            if (i < 10) {
                detalle.append(i == 0 ? "" : "; ").append("préstamo ").append(f[0]).append(" (op. ").append(f[1])
                    .append(") cuota ").append(f[3]).append(esSobre ? " pagada de más " : " con saldo ")
                    .append(PrelacionCuotas.formato(f[4] != null ? ((Number) f[4]).doubleValue() : 0.0))
                    .append(esSobre ? "" : ", posterior " + f[5] + " con pagos");
            }
        }
        return filas.size() + " préstamo(s) con la prelación de cuotas rota al " + corte + " (C33: " + prelacion
            + " con una cuota con saldo antes de otra con pagos, " + sobre + " con una cuota pagada de más): " + detalle
            + (filas.size() > 10 ? " y " + (filas.size() - 10) + " más" : "") + ". Re-imputar en orden con POST"
            + " /rest/prelacioncuotas/reimputarAlCorte (simular primero) o revisar con crédito.";
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<ResultadoReimputacion> reimputarAlCorte(LocalDate corte, String usuario, Long idEmpresa, boolean simular)
            throws Throwable {
        List<Object[]> filas = controlAlCorte(corte);
        List<ResultadoReimputacion> resultados = new ArrayList<>();
        java.util.Set<Long> vistos = new java.util.LinkedHashSet<>();
        for (Object[] f : filas) {
            Long idPrestamo = (Long) f[0];
            if (!vistos.add(idPrestamo)) {
                continue;
            }
            try {
                resultados.add(self.reimputarPagosEnOrdenNuevaTransaccion(idPrestamo, corte, usuario, idEmpresa, simular));
            } catch (Throwable e) {
                ResultadoReimputacion r = new ResultadoReimputacion();
                r.setIdPrestamo(idPrestamo);
                r.setFecha(corte);
                r.setSimulacion(simular);
                r.setMensaje("Error: " + e.getMessage());
                resultados.add(r);
                System.err.println("  ❌ C33 préstamo " + idPrestamo + ": " + e.getMessage());
            }
        }
        return resultados;
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private List<DetallePrestamo> cuotasOrdenadas(Long idPrestamo) throws Throwable {
        List<DetallePrestamo> cuotas = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
        List<DetallePrestamo> r = cuotas != null ? new ArrayList<>(cuotas) : new ArrayList<>();
        r.sort(Comparator.comparing(DetallePrestamo::getNumeroCuota, Comparator.nullsLast(Comparator.naturalOrder())));
        return r;
    }

    /** Pagos vigentes del préstamo (leídos por cuota) con fecha {@code <= corte} (o sin fecha), por código de cuota. */
    private Map<Long, List<PagoPrestamo>> pagosAlCortePorCuota(List<DetallePrestamo> cuotas, List<PagoPrestamo> vigentes,
            LocalDateTime corte) {
        Map<Long, List<PagoPrestamo>> r = new LinkedHashMap<>();
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (DetallePrestamo c : cuotas) {
            ids.add(c.getCodigo());
        }
        for (PagoPrestamo p : PrelacionCuotas.alCorte(vigentes, corte)) {
            if (p.getDetallePrestamo() == null || !ids.contains(p.getDetallePrestamo().getCodigo())) {
                continue;
            }
            r.computeIfAbsent(p.getDetallePrestamo().getCodigo(), k -> new ArrayList<>()).add(p);
        }
        return r;
    }

    /** Lote 10: PGPR vigentes cuyo PRSTCDGO es nulo o distinto del préstamo (el dato mal grabado que esconde pagos a otras consultas). */
    private static int contarPagosSinPrestamo(Long idPrestamo, List<PagoPrestamo> vigentes) {
        int n = 0;
        for (PagoPrestamo p : vigentes) {
            if (p.getPrestamo() == null || p.getPrestamo().getCodigo() == null || !p.getPrestamo().getCodigo().equals(idPrestamo)) {
                n++;
            }
        }
        return n;
    }

    /** Texto de una fila del control del cierre: {tipo en [8], cuota en [3], monto en [4], posterior en [5]}. */
    private static String textoFilaControl(Object[] f) {
        boolean sobre = PrelacionCuotas.Rotura.SOBREIMPUTADA.equals(f[8]);
        return "cuota " + f[3] + (sobre ? " pagada de más " : " con saldo ")
            + PrelacionCuotas.formato(f[4] != null ? ((Number) f[4]).doubleValue() : 0.0)
            + (sobre ? "" : ", posterior " + f[5] + " con pagos") + " (" + f[8] + ")";
    }

    private static String truncar(String texto, int max) {
        return texto == null || texto.length() <= max ? texto : texto.substring(0, max);
    }

    private static String entero(Double n) {
        return n == null ? "?" : String.valueOf(n.longValue());
    }
}

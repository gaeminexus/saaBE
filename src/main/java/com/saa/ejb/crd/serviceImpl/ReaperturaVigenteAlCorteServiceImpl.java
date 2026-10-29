package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.EventoPrestamoDaoService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.MotorPagoPrestamoService;
import com.saa.ejb.crd.service.ProcesoMoraPrestamoService;
import com.saa.ejb.crd.service.ReaperturaVigenteAlCorteService;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.EventoPrestamo;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoPrestamo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @see ReaperturaVigenteAlCorteService
 * @author Sistema SAA
 * @since 2026-10-28
 */
@Stateless
public class ReaperturaVigenteAlCorteServiceImpl implements ReaperturaVigenteAlCorteService {

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private EventoPrestamoDaoService eventoPrestamoDaoService;

    @EJB
    private MotorPagoPrestamoService motorPagoPrestamoService;

    @EJB
    private ProcesoMoraPrestamoService procesoMoraPrestamoService;

    /** Último instante del día de corte (inclusive). */
    private static LocalDateTime instanteCorte(LocalDate fechaCorte) {
        return fechaCorte.atTime(23, 59, 59);
    }

    @Override
    public boolean esPrestamoTerminal(Long idEstado) {
        if (idEstado == null) {
            return false;
        }
        int e = idEstado.intValue();
        return e == EstadoPrestamo.CANCELADO || e == EstadoPrestamo.CANCELADO_ANTICIPADO
            || e == EstadoPrestamo.CANCELADO_POR_NOVACION || e == EstadoPrestamo.CANCELADO_POR_REVISAR
            || e == EstadoPrestamo.CANCELADO_POR_REESTRUCTURACION;
    }

    @Override
    public boolean estabaVigenteAlCorte(Prestamo prestamo, LocalDate fechaCorte) throws Throwable {
        if (prestamo == null || fechaCorte == null || prestamo.getEntidad() == null || prestamo.getProducto() == null) {
            return false;
        }
        List<Prestamo> vigentes = prestamoDaoService.selectByEntidadYProductoVigentesAlCorte(
            prestamo.getEntidad().getCodigo(), prestamo.getProducto().getCodigo(), instanteCorte(fechaCorte));
        if (vigentes == null) {
            return false;
        }
        for (Prestamo vigente : vigentes) {
            if (vigente.getCodigo() != null && vigente.getCodigo().equals(prestamo.getCodigo())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int reabrirCuotasSaldadasSinPagoDelMes(Prestamo prestamo, LocalDate fechaCorte, List<DetallePrestamo> cuotas,
            List<String> bitacora) throws Throwable {
        if (cuotas == null) {
            return 0;
        }
        LocalDateTime corte = instanteCorte(fechaCorte);
        LocalDateTime inicioMes = fechaCorte.withDayOfMonth(1).atStartOfDay();
        int reabiertas = 0;
        for (DetallePrestamo cuota : cuotas) {
            Long estado = cuota.getEstado();
            boolean saldada = estado != null && (estado.intValue() == EstadoCuotaPrestamo.PAGADA
                || estado.intValue() == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA);
            if (!saldada || cuota.getFechaVencimiento() == null
                    || cuota.getFechaVencimiento().isBefore(inicioMes) || cuota.getFechaVencimiento().isAfter(corte)) {
                continue;
            }
            // §4.9 ampliada (2026-10-28): la fecha de pago NO decide. Lo único que prueba que la
            // cuota se pagó es un pago vigente; sin él vuelve a pendiente al corte.
            LocalDateTime fechaPagadoAnterior = cuota.getFechaPagado();
            List<PagoPrestamo> pagosVigentes = pagoPrestamoDaoService.selectVigentesByIdDetallePrestamo(cuota.getCodigo());
            if (pagosVigentes != null && !pagosVigentes.isEmpty()) {
                continue;
            }
            motorPagoPrestamoService.recalcularCuotaDesdePagos(cuota);
            procesoMoraPrestamoService.recalcularMoraCuotaALaFecha(cuota, fechaCorte);
            if (cuota.getEstado() == null || cuota.getEstado().longValue() != estado.longValue()) {
                reabiertas++;
                String linea = "Préstamo " + prestamo.getCodigo() + " cuota " + numeroCuotaTexto(cuota)
                    + " (préstamo hoy estado " + prestamo.getIdEstado() + "): estado " + estado + " -> "
                    + cuota.getEstado() + ", fecha de pago anterior "
                    + (fechaPagadoAnterior != null ? fechaPagadoAnterior.toLocalDate() : "nula")
                    + ", sin pagos vigentes";
                if (bitacora != null) {
                    bitacora.add(linea);
                }
                System.out.println("   D-019 ampliada: " + linea);
            }
        }
        return reabiertas;
    }

    @Override
    public String reabrirPrestamoVigenteAlCorte(Prestamo prestamo, LocalDate fechaCorte, String etiqueta,
            List<DetallePrestamo> cuotas, List<String> bitacoraCuotas) throws Throwable {
        LocalDateTime corte = instanteCorte(fechaCorte);
        Long estadoAnterior = prestamo.getIdEstado();
        String marca = etiqueta + ": " + MARCA_CANCELACION_POSTERIOR + " (estado anterior " + estadoAnterior + ")";

        // Primero la regla común (cuota del mes saldada sin pago vigente); después el resto de las
        // cuotas saldadas sin fecha o con fecha posterior al corte (la cancelación posterior).
        int cuotasReabiertas = reabrirCuotasSaldadasSinPagoDelMes(prestamo, fechaCorte, cuotas, bitacoraCuotas);
        if (cuotas != null) {
            for (DetallePrestamo cuota : cuotas) {
                Long estado = cuota.getEstado();
                boolean saldada = estado != null && (estado.intValue() == EstadoCuotaPrestamo.PAGADA
                    || estado.intValue() == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA);
                if (!saldada || (cuota.getFechaPagado() != null && !cuota.getFechaPagado().isAfter(corte))) {
                    continue;
                }
                motorPagoPrestamoService.recalcularCuotaDesdePagos(cuota);
                procesoMoraPrestamoService.recalcularMoraCuotaALaFecha(cuota, fechaCorte);
                if (cuota.getEstado() == null || cuota.getEstado().longValue() != estado.longValue()) {
                    cuotasReabiertas++;
                }
            }
        }

        prestamo.setIdEstado(Long.valueOf(EstadoPrestamo.VIGENTE));
        prestamo.setFechaModificacion(LocalDateTime.now());
        prestamo.setObservacion(truncarTexto(prestamo.getObservacion() == null || prestamo.getObservacion().trim().isEmpty()
            ? marca : prestamo.getObservacion() + " | " + marca, 2000));
        prestamoDaoService.save(prestamo, prestamo.getCodigo());

        int eventosMarcados = 0;
        List<EventoPrestamo> eventos = eventoPrestamoDaoService.selectByPrestamo(prestamo.getCodigo());
        if (eventos != null) {
            for (EventoPrestamo evento : eventos) {
                if (evento.getFechaAnulacion() != null || evento.getFecha() == null || !evento.getFecha().isAfter(corte)) {
                    continue;
                }
                if (evento.getObservacion() != null && evento.getObservacion().contains(marca)) {
                    continue;
                }
                evento.setObservacion(truncarTexto(evento.getObservacion() == null ? marca
                    : evento.getObservacion() + " | " + marca, 2000));
                eventoPrestamoDaoService.save(evento, evento.getCodigo());
                eventosMarcados++;
            }
        }

        System.out.println("   D-019: préstamo " + prestamo.getCodigo() + " vigente al " + fechaCorte + " (hoy estado "
            + estadoAnterior + ") reabierto - " + cuotasReabiertas + " cuotas, " + eventosMarcados + " eventos marcados");
        return "Préstamo " + prestamo.getCodigo() + " (estado anterior " + estadoAnterior + "): " + cuotasReabiertas
            + " cuota(s) reabiertas, " + eventosMarcados + " evento(s) posteriores marcados";
    }

    @Override
    public void prepararPrestamoAlCorte(Prestamo prestamo, LocalDate fechaCorte, String etiqueta, List<String> bitacora)
            throws Throwable {
        if (prestamo == null || fechaCorte == null) {
            throw new IncomeException("PARAMETRO_INVALIDO: préstamo y fecha de corte son obligatorios");
        }
        if (!estabaVigenteAlCorte(prestamo, fechaCorte)) {
            throw new IncomeException("PRESTAMO_NO_VIGENTE_AL_CORTE: el préstamo " + prestamo.getCodigo()
                + " (hoy estado " + prestamo.getIdEstado() + ") no estaba vigente al " + fechaCorte
                + ": se canceló antes del corte o todavía no tenía cuotas vencidas. D-019 no lo reabre.");
        }
        List<DetallePrestamo> cuotas = detallePrestamoDaoService.selectByPrestamo(prestamo.getCodigo());
        if (esPrestamoTerminal(prestamo.getIdEstado())) {
            String resumen = reabrirPrestamoVigenteAlCorte(prestamo, fechaCorte, etiqueta, cuotas, bitacora);
            if (bitacora != null) {
                bitacora.add(resumen);
            }
        } else {
            reabrirCuotasSaldadasSinPagoDelMes(prestamo, fechaCorte, cuotas, bitacora);
        }
    }

    private static String numeroCuotaTexto(DetallePrestamo cuota) {
        return cuota.getNumeroCuota() == null ? "?" : String.valueOf(cuota.getNumeroCuota().longValue());
    }

    private static String truncarTexto(String texto, int max) {
        if (texto == null || texto.length() <= max) {
            return texto;
        }
        return texto.substring(0, max);
    }
}

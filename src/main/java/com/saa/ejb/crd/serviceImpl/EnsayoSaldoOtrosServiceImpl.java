package com.saa.ejb.crd.serviceImpl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.HistDetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.AbonoCapitalPrestamoService;
import com.saa.ejb.crd.service.EnsayoSaldoOtrosService;
import com.saa.ejb.crd.service.MotorPagoPrestamoService;
import com.saa.ejb.crd.service.ProcesoPagoPrestamoService;
import com.saa.ejb.crd.service.ReaperturaVigenteAlCorteService;
import com.saa.ejb.crd.service.SaldoOtrosRespaldo;
import com.saa.ejb.crd.service.dto.ResultadoAbonoCapital;
import com.saa.ejb.crd.service.dto.ResultadoAnulacion;
import com.saa.ejb.crd.service.dto.ResultadoPrecancelacion;
import com.saa.ejb.crd.service.dto.SimulacionPrecancelacion;
import com.saa.ejb.crd.service.dto.SolicitudAbonoCapital;
import com.saa.ejb.crd.service.dto.SolicitudAnulacion;
import com.saa.ejb.crd.service.dto.SolicitudEnsayoSaldoOtros;
import com.saa.ejb.crd.service.dto.SolicitudPrecancelacion;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.HistDetallePrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.EstadoCuotaPrestamo;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @see EnsayoSaldoOtrosService
 * @since 2026-11-18 (C35)
 */
@Stateless
public class EnsayoSaldoOtrosServiceImpl implements EnsayoSaldoOtrosService {

    @Resource
    private SessionContext sessionContext;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private HistDetallePrestamoDaoService histDetallePrestamoDaoService;

    @EJB
    private MotorPagoPrestamoService motorPagoPrestamoService;

    @EJB
    private ReaperturaVigenteAlCorteService reaperturaVigenteAlCorteService;

    @EJB
    private ProcesoPagoPrestamoService procesoPagoPrestamoService;

    @EJB
    private AbonoCapitalPrestamoService abonoCapitalPrestamoService;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Map<String, Object> ensayar(SolicitudEnsayoSaldoOtros solicitud) throws Throwable {
        System.out.println("EnsayoSaldoOtrosService.ensayar (C35, SIN EFECTO) - Préstamo: "
            + (solicitud != null ? solicitud.getIdPrestamo() : null));
        try {
            return ensayarSinConfirmar(solicitud);
        } finally {
            // Pase lo que pase, nada de esta transacción se confirma. Al final (y no al principio) para que la
            // conexión se enliste en una transacción activa.
            sessionContext.setRollbackOnly();
            System.out.println("EnsayoSaldoOtrosService.ensayar - transacción marcada para ROLLBACK (C35)");
        }
    }

    private Map<String, Object> ensayarSinConfirmar(SolicitudEnsayoSaldoOtros solicitud) throws Throwable {
        if (solicitud == null || solicitud.getIdPrestamo() == null) {
            throw new IncomeException("PARAMETRO_INVALIDO: idPrestamo es obligatorio");
        }
        Long idPrestamo = solicitud.getIdPrestamo();
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("idPrestamo", idPrestamo);
        respuesta.put("revertido", Boolean.TRUE);
        respuesta.put("nota", "Ensayo C35: todo se revierte al terminar (setRollbackOnly); la base no cambia.");
        int hdtpAntes = filasHdtpC35(idPrestamo);
        respuesta.put("inicial", foto(idPrestamo));

        List<Map<String, Object>> pasos = new ArrayList<>();
        respuesta.put("pasos", pasos);
        int n = 0;
        for (SolicitudEnsayoSaldoOtros.Paso paso : solicitud.getPasos()) {
            n++;
            Map<String, Object> salida = new LinkedHashMap<>();
            salida.put("n", n);
            salida.put("tipo", paso.getTipo());
            pasos.add(salida);
            try {
                salida.put("resultado", ejecutar(solicitud, paso));
                salida.put("ok", Boolean.TRUE);
            } catch (Throwable e) {
                salida.put("ok", Boolean.FALSE);
                salida.put("error", e.getMessage() != null ? e.getMessage() : e.getClass().getName());
                // Tras un error de negocio la transacción ya está marcada; no se sigue con los pasos siguientes.
                break;
            } finally {
                try {
                    salida.put("foto", foto(idPrestamo));
                    salida.put("filasHdtpC35Nuevas", filasHdtpC35(idPrestamo) - hdtpAntes);
                } catch (Throwable e) {
                    salida.put("foto", "no se pudo leer: " + e.getMessage());
                }
            }
        }
        return respuesta;
    }

    private Object ejecutar(SolicitudEnsayoSaldoOtros solicitud, SolicitudEnsayoSaldoOtros.Paso paso) throws Throwable {
        String tipo = paso.getTipo() != null ? paso.getTipo().trim().toUpperCase() : "";
        Long idPrestamo = solicitud.getIdPrestamo();
        switch (tipo) {
            case "REAPERTURA": {
                Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
                List<String> bitacora = new ArrayList<>();
                reaperturaVigenteAlCorteService.prepararPrestamoAlCorte(prestamo, paso.getFecha(), "ENSAYO C35", bitacora);
                return bitacora;
            }
            case "ANULAR": {
                SolicitudAnulacion s = new SolicitudAnulacion();
                s.setIdEvento(paso.getIdEvento());
                s.setIdEmpresa(solicitud.getIdEmpresa());
                s.setUsuario(solicitud.getUsuario());
                s.setMotivo(paso.getMotivo() != null ? paso.getMotivo() : "ENSAYO C35 (se revierte)");
                ResultadoAnulacion r = procesoPagoPrestamoService.anularOperacion(s);
                return "evento " + r.getIdEvento() + " anulado, pagos anulados " + r.getPagosAnulados();
            }
            case "PRECANCELAR": {
                SimulacionPrecancelacion sim = procesoPagoPrestamoService.simularPrecancelacion(idPrestamo, paso.getFecha());
                SolicitudPrecancelacion s = new SolicitudPrecancelacion();
                s.setIdPrestamo(idPrestamo);
                s.setIdEmpresa(solicitud.getIdEmpresa());
                s.setUsuario(solicitud.getUsuario());
                s.setFecha(paso.getFecha());
                s.setFechaAfectacion(paso.getFecha());
                s.setValorEfectivo(sim.getValorTotalPrecancelacion());
                s.setReproceso(paso.getReproceso());
                s.setObservacion("ENSAYO C35 (se revierte)");
                ResultadoPrecancelacion r = procesoPagoPrestamoService.precancelar(s);
                return "evento " + r.getIdEvento() + ", total " + sim.getValorTotalPrecancelacion() + ", capital futuro "
                    + sim.getCapitalFuturo();
            }
            case "ABONO": {
                SolicitudAbonoCapital s = new SolicitudAbonoCapital();
                s.setIdPrestamo(idPrestamo);
                s.setIdEmpresa(solicitud.getIdEmpresa());
                s.setUsuario(solicitud.getUsuario());
                s.setFecha(paso.getFecha());
                s.setFechaAfectacion(paso.getFecha());
                s.setValor(paso.getValor());
                s.setModalidad(paso.getModalidad());
                s.setRegenerarTabla(paso.getRegenerarTabla());
                s.setNumeroCuota(paso.getNumeroCuota());
                s.setReproceso(paso.getReproceso());
                s.setObservacion("ENSAYO C35 (se revierte)");
                ResultadoAbonoCapital r = abonoCapitalPrestamoService.aplicar(s);
                return "evento " + r.getIdEvento() + ", cuota del abono " + r.getIdCuotaConSaldoOtros();
            }
            case "RECALCULAR": {
                List<DetallePrestamo> cuotas = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
                for (DetallePrestamo cuota : cuotas) {
                    motorPagoPrestamoService.recalcularCuotaDesdePagos(cuota);
                }
                return cuotas.size() + " cuota(s) recalculadas desde sus pagos";
            }
            default:
                throw new IncomeException("PARAMETRO_INVALIDO: tipo de paso desconocido '" + paso.getTipo()
                    + "' (REAPERTURA, ANULAR, PRECANCELAR, ABONO, RECALCULAR)");
        }
    }

    /** Estado del préstamo y cuotas con DTPRSLOT o PGPRSLOT, con el invariante Σ capital (no 7) + Σ DTPRSLOT = monto. */
    private Map<String, Object> foto(Long idPrestamo) throws Throwable {
        Map<String, Object> f = new LinkedHashMap<>();
        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
        f.put("estadoPrestamo", prestamo != null ? prestamo.getIdEstado() : null);
        List<Map<String, Object>> cuotas = new ArrayList<>();
        double capitalNo7 = 0.0;
        double slot = 0.0;
        double respaldadoTotal = 0.0;
        Map<Long, Integer> porEstado = new java.util.TreeMap<>();
        for (DetallePrestamo c : detallePrestamoDaoService.selectByPrestamo(idPrestamo)) {
            long estado = c.getEstado() != null ? c.getEstado() : -1L;
            porEstado.merge(estado, 1, Integer::sum);
            if (estado != EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
                capitalNo7 += c.getCapital() != null ? c.getCapital() : 0.0;
            }
            double s = c.getSaldoOtros() != null ? c.getSaldoOtros() : 0.0;
            double r = motorPagoPrestamoService.saldoOtrosRespaldado(c);
            slot += s;
            respaldadoTotal += r;
            if (Math.abs(s) > 0.001 || Math.abs(r) > 0.001) {
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("cuota", c.getNumeroCuota() != null ? Math.round(c.getNumeroCuota()) : null);
                fila.put("estado", c.getEstado());
                fila.put("DTPRSLOT", redondear(s));
                fila.put("respaldadoPGPRSLOT", r);
                fila.put("sinRespaldo", SaldoOtrosRespaldo.residuo(s, r));
                cuotas.add(fila);
            }
        }
        f.put("cuotasPorEstado", porEstado);
        f.put("cuotasConSaldoOtros", cuotas);
        f.put("sumaDTPRSLOT", redondear(slot));
        f.put("sumaRespaldada", redondear(respaldadoTotal));
        f.put("capitalCuotasNo7", redondear(capitalNo7));
        Double monto = prestamo != null ? prestamo.getMontoSolicitado() : null;
        f.put("monto", monto);
        f.put("capitalNo7MasDTPRSLOTMenosMonto", monto != null ? redondear(capitalNo7 + slot - monto) : null);
        return f;
    }

    private int filasHdtpC35(Long idPrestamo) throws Throwable {
        int n = 0;
        List<HistDetallePrestamo> filas = histDetallePrestamoDaoService.selectByPrestamo(idPrestamo);
        if (filas != null) {
            for (HistDetallePrestamo h : filas) {
                if (h.getMotivo() != null && h.getMotivo().startsWith(SaldoOtrosRespaldo.PREFIJO_MOTIVO)) {
                    n++;
                }
            }
        }
        return n;
    }

    private static double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}

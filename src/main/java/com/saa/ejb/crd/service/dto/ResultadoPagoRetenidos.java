package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Pensión retenida de un jubilado (DISENO-PENSION-RETENIDA-SIN-CUENTA.md): lo pagado y lo pendiente, por mes.
 * Lo devuelven {@code pagarRetenidos} (POST) y {@code consultarRetenidos} (GET, solo lectura).
 */
public class ResultadoPagoRetenidos {

    /** Un mes retenido. */
    public static class Linea {
        private Long idPago;
        private Long anio;
        private Long mes;
        private double valor;
        /** Pagados: orden de pago en CXP. */
        private Long idOrdenPago;
        /** Pagados: asiento de devengo generado al pagar (null en los históricos ya devengados). */
        private Long idAsientoDevengo;
        /** Pagados: true si el retenido era histórico (ya devengado antes del cambio, no se devengó otra vez). */
        private boolean historicoYaDevengado;
        /** No pagados: por qué. */
        private String motivo;

        public Long getIdPago() { return idPago; }
        public void setIdPago(Long idPago) { this.idPago = idPago; }
        public Long getAnio() { return anio; }
        public void setAnio(Long anio) { this.anio = anio; }
        public Long getMes() { return mes; }
        public void setMes(Long mes) { this.mes = mes; }
        public double getValor() { return valor; }
        public void setValor(double valor) { this.valor = valor; }
        public Long getIdOrdenPago() { return idOrdenPago; }
        public void setIdOrdenPago(Long idOrdenPago) { this.idOrdenPago = idOrdenPago; }
        public Long getIdAsientoDevengo() { return idAsientoDevengo; }
        public void setIdAsientoDevengo(Long idAsientoDevengo) { this.idAsientoDevengo = idAsientoDevengo; }
        public boolean isHistoricoYaDevengado() { return historicoYaDevengado; }
        public void setHistoricoYaDevengado(boolean historicoYaDevengado) { this.historicoYaDevengado = historicoYaDevengado; }
        public String getMotivo() { return motivo; }
        public void setMotivo(String motivo) { this.motivo = motivo; }
    }

    private Long idEntidad;
    /** true si hoy el jubilado tiene salida válida (una cuenta activa con certificado). */
    private boolean tieneSalida;
    /** Si no tiene salida, por qué (para mostrarlo en pantalla). */
    private String motivoSinSalida;
    private List<Linea> pagados = new ArrayList<>();
    private List<Linea> noPagados = new ArrayList<>();
    /** Solo en la consulta (GET): los meses retenidos pendientes de pago, en orden de período. */
    private List<Linea> pendientes = new ArrayList<>();
    private double totalPendiente;
    private double totalPagado;
    private double totalNoPagado;
    private String mensaje;

    public Long getIdEntidad() { return idEntidad; }
    public void setIdEntidad(Long idEntidad) { this.idEntidad = idEntidad; }
    public boolean isTieneSalida() { return tieneSalida; }
    public void setTieneSalida(boolean tieneSalida) { this.tieneSalida = tieneSalida; }
    public String getMotivoSinSalida() { return motivoSinSalida; }
    public void setMotivoSinSalida(String motivoSinSalida) { this.motivoSinSalida = motivoSinSalida; }
    public List<Linea> getPagados() { return pagados; }
    public void setPagados(List<Linea> pagados) { this.pagados = pagados; }
    public List<Linea> getNoPagados() { return noPagados; }
    public void setNoPagados(List<Linea> noPagados) { this.noPagados = noPagados; }
    public List<Linea> getPendientes() { return pendientes; }
    public void setPendientes(List<Linea> pendientes) { this.pendientes = pendientes; }
    public double getTotalPendiente() { return totalPendiente; }
    public void setTotalPendiente(double totalPendiente) { this.totalPendiente = totalPendiente; }
    public double getTotalPagado() { return totalPagado; }
    public void setTotalPagado(double totalPagado) { this.totalPagado = totalPagado; }
    public double getTotalNoPagado() { return totalNoPagado; }
    public void setTotalNoPagado(double totalNoPagado) { this.totalNoPagado = totalNoPagado; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}

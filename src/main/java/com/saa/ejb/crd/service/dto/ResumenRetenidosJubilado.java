package com.saa.ejb.crd.service.dto;

/** Una fila de la lectura en bloque de pensiones retenidas pendientes (GET pgpc/retenidos?idEmpresa): un jubilado. */
public class ResumenRetenidosJubilado {
    private Long idEntidad;
    private String nombre;
    private String identificacion;
    /** Suma de los meses retenidos pendientes (incluye los pagos rechazados que volvieron a pendiente). */
    private double totalPendiente;
    /** Cantidad de meses pendientes (una orden por mes al pagarlos). */
    private int meses;
    /** true si hoy tiene salida válida (una cuenta activa con certificado): se puede pagar ya. */
    private boolean tieneSalida;
    private String motivoSinSalida;

    public Long getIdEntidad() { return idEntidad; }
    public void setIdEntidad(Long idEntidad) { this.idEntidad = idEntidad; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }
    public double getTotalPendiente() { return totalPendiente; }
    public void setTotalPendiente(double totalPendiente) { this.totalPendiente = totalPendiente; }
    public int getMeses() { return meses; }
    public void setMeses(int meses) { this.meses = meses; }
    public boolean isTieneSalida() { return tieneSalida; }
    public void setTieneSalida(boolean tieneSalida) { this.tieneSalida = tieneSalida; }
    public String getMotivoSinSalida() { return motivoSinSalida; }
    public void setMotivoSinSalida(String motivoSinSalida) { this.motivoSinSalida = motivoSinSalida; }
}

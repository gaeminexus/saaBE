package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/**
 * C31c: respuesta de {@code POST /rest/prst/sincronizarGarantia} (sincronización explícita de la garantía
 * de un préstamo en cuentas de orden).
 */
public class ResultadoSincronizacionGarantia {

    /** Se generó el asiento de baja. */
    public static final String ACCION_BAJA = "BAJA";
    /** Se anuló (o reversó) la baja vigente de un préstamo vivo. */
    public static final String ACCION_ANULACION = "ANULACION";
    /** No había nada que hacer (ya dada de baja, vivo sin baja u otro estado). */
    public static final String ACCION_SIN_CAMBIO = "SIN_CAMBIO";

    private Long idPrestamo;
    private Long estadoPrestamo;
    private LocalDate fecha;
    private String accion;
    private Long idAsiento;
    /** Si después de la llamada la baja está vigente (neto de las líneas marcadas &gt; 0). */
    private boolean bajaVigente;
    private String mensaje;

    public ResultadoSincronizacionGarantia() {
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Long getEstadoPrestamo() {
        return estadoPrestamo;
    }

    public void setEstadoPrestamo(Long estadoPrestamo) {
        this.estadoPrestamo = estadoPrestamo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public Long getIdAsiento() {
        return idAsiento;
    }

    public void setIdAsiento(Long idAsiento) {
        this.idAsiento = idAsiento;
    }

    public boolean isBajaVigente() {
        return bajaVigente;
    }

    public void setBajaVigente(boolean bajaVigente) {
        this.bajaVigente = bajaVigente;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}

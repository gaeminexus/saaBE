package com.saa.ejb.crd.service.dto;

/**
 * Un renglón del detalle de {@link ResultadoPrevisualizacionCorrida} — la estimación de un
 * jubilado, SIN escribir nada. Ver API-PAGO-PENSION-COMPLEMENTARIA.md §4bis.
 */
public class DetallePrevisualizacionJubilado {

    private Long idEntidad;
    private String nombre;

    /** Cuántos meses adeudados desde el ancla hasta el mes de la corrida (0 si está al día). */
    private int mesesAdeudados;

    /** Estimado: cuánto absorbería la deuda exigible del préstamo. No sale de la asociación. */
    private double montoACruzar;

    /** Estimado: cuánto saldría como orden de pago hacia tesorería (0 si no tiene certificado). */
    private double montoADinero;

    /** {@code montoACruzar + montoADinero}. */
    private double total;

    private boolean tienePrestamo;
    private boolean tieneCertificado;

    /** "COMPLETA" | "SOLO_CRUCE" | "BLOQUEADO" — mismo campo y mismo significado que
     *  {@link DetallePagoPension#getParticipacion()}. */
    private String participacion;

    /** {@code false} si este jubilado no entra en la corrida real. */
    private boolean apto;

    /** Por qué no es apto — {@code null} si {@code apto = true}. */
    private String motivoBloqueo;

    public DetallePrevisualizacionJubilado() {
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getMesesAdeudados() {
        return mesesAdeudados;
    }

    public void setMesesAdeudados(int mesesAdeudados) {
        this.mesesAdeudados = mesesAdeudados;
    }

    public double getMontoACruzar() {
        return montoACruzar;
    }

    public void setMontoACruzar(double montoACruzar) {
        this.montoACruzar = montoACruzar;
    }

    public double getMontoADinero() {
        return montoADinero;
    }

    public void setMontoADinero(double montoADinero) {
        this.montoADinero = montoADinero;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public boolean isTienePrestamo() {
        return tienePrestamo;
    }

    public void setTienePrestamo(boolean tienePrestamo) {
        this.tienePrestamo = tienePrestamo;
    }

    public boolean isTieneCertificado() {
        return tieneCertificado;
    }

    public void setTieneCertificado(boolean tieneCertificado) {
        this.tieneCertificado = tieneCertificado;
    }

    public String getParticipacion() {
        return participacion;
    }

    public void setParticipacion(String participacion) {
        this.participacion = participacion;
    }

    public boolean isApto() {
        return apto;
    }

    public void setApto(boolean apto) {
        this.apto = apto;
    }

    public String getMotivoBloqueo() {
        return motivoBloqueo;
    }

    public void setMotivoBloqueo(String motivoBloqueo) {
        this.motivoBloqueo = motivoBloqueo;
    }
}

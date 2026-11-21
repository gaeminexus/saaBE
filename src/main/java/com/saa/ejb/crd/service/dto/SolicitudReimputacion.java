package com.saa.ejb.crd.service.dto;

/**
 * C33: pedido de {@code POST /rest/prelacioncuotas/reimputar} y {@code /reimputarAlCorte}.
 * {@code fecha} es el corte ({@code AAAA-MM-DD}, inclusive); {@code simular = true} calcula sin grabar.
 */
public class SolicitudReimputacion {

    private Long idPrestamo;
    private String fecha;
    private String usuario;
    private Long idEmpresa;
    private Boolean simular;

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public Long getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(Long idEmpresa) { this.idEmpresa = idEmpresa; }
    public Boolean getSimular() { return simular; }
    public void setSimular(Boolean simular) { this.simular = simular; }

    public boolean esSimulacion() {
        return Boolean.TRUE.equals(simular);
    }
}

package com.saa.ejb.crd.service.dto;

/** Cuerpo de {@code POST /prst/anular/{id}} (API-CICLO-OTORGAMIENTO.md 7.2). */
public class SolicitudAnularPrestamo {

    private String usuario;
    private String motivo;
    private Long idEmpresa;
    private Long idUsuario;

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public Long getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(Long idEmpresa) { this.idEmpresa = idEmpresa; }

    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
}

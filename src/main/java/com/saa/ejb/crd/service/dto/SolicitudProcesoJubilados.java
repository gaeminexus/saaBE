package com.saa.ejb.crd.service.dto;

/**
 * Cuerpo de {@code POST /rest/pgpc/seguro/generar} y {@code POST /rest/pgpc/pensiones/generar}
 * — forma canónica de {@code docs/logica-negocio/crd/API-DOS-PROCESOS-MENSUALES-JUBILADOS.md}
 * §4.1/§4.2: {@code { idEmpresa, anio, mes, usuario, idUsuario }}.
 *
 * {@code PagoPensionComplementariaRest} también acepta los mismos cuatro valores por
 * {@code @QueryParam}, por compatibilidad; el cuerpo tiene precedencia cuando llegan los dos.
 */
public class SolicitudProcesoJubilados {

    private Long idEmpresa;
    private Integer anio;
    private Integer mes;
    private String usuario;
    private Long idUsuario;

    public SolicitudProcesoJubilados() {
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }
}

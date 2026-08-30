package com.saa.ejb.crd.service.dto;

/**
 * Body de {@code POST /rest/asgn/confirmarRecepcion/{idCarga}} — CONTRATO CONGELADO, ver
 * {@code docs/logica-negocio/crd/API-COBRO-PETRO-DOS-PASOS.md} §2.2.
 */
public class SolicitudConfirmarRecepcion {

    private String usuario;
    private String ip;
    private String observacion;

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}

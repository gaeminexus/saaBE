package com.saa.ejb.crd.service.dto;

/**
 * Body de {@code POST /rest/asgn/reversarRecepcion/{idCarga}} — CONTRATO CONGELADO, ver
 * {@code docs/logica-negocio/crd/API-COBRO-PETRO-DOS-PASOS.md} §2.3. {@code motivo} es
 * OBLIGATORIO.
 */
public class SolicitudReversarRecepcion {

    private String usuario;
    private String ip;
    private String motivo;

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

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}

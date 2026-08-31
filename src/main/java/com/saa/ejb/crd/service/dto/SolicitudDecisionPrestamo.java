package com.saa.ejb.crd.service.dto;

/**
 * Body de POST /rest/prst/aprobar/{id} y POST /rest/prst/rechazar/{id}.
 *
 * Ver PLAN-CICLO-OTORGAMIENTO.md §4.
 */
public class SolicitudDecisionPrestamo {

    private String usuario;
    private String observacion;

    public SolicitudDecisionPrestamo() {
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}

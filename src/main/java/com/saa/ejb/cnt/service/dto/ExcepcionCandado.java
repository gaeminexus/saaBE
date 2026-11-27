package com.saa.ejb.cnt.service.dto;

import java.io.Serializable;

/** C51: excepción explícita al candado de {@code POST /prdo/cerrar}: cubre UN control en ERROR, con motivo obligatorio. */
public class ExcepcionCandado implements Serializable {

    private static final long serialVersionUID = 1L;

    private String codigoControl;
    private String motivo;

    public ExcepcionCandado() {
    }

    public ExcepcionCandado(String codigoControl, String motivo) {
        this.codigoControl = codigoControl;
        this.motivo = motivo;
    }

    public String getCodigoControl() { return codigoControl; }
    public void setCodigoControl(String codigoControl) { this.codigoControl = codigoControl; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}

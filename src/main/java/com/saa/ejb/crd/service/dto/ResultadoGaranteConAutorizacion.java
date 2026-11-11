package com.saa.ejb.crd.service.dto;

import com.saa.model.crd.Adjunto;
import com.saa.model.crd.GarantePrestamo;

/**
 * Resultado de {@code GarantePrestamoService.crearConAutorizacion}: el garante y su autorización firmada, ya guardados
 * los dos en la misma transacción.
 */
public class ResultadoGaranteConAutorizacion {

    private GarantePrestamo garante;
    private Adjunto autorizacion;

    public ResultadoGaranteConAutorizacion() {
    }

    public ResultadoGaranteConAutorizacion(GarantePrestamo garante, Adjunto autorizacion) {
        this.garante = garante;
        this.autorizacion = autorizacion;
    }

    public GarantePrestamo getGarante() {
        return garante;
    }

    public void setGarante(GarantePrestamo garante) {
        this.garante = garante;
    }

    public Adjunto getAutorizacion() {
        return autorizacion;
    }

    public void setAutorizacion(Adjunto autorizacion) {
        this.autorizacion = autorizacion;
    }
}

package com.saa.ejb.crd.service.dto;

/**
 * Resultado de {@link com.saa.ejb.crd.service.CobroCreditoService#procesarCobro}.
 *
 * {@code procesado = false} con {@code estado = RECHAZADO} es un resultado VÁLIDO, no un
 * error: es el rechazo automático por staleness de precancelación (el monto registrado ya
 * no coincide con el préstamo al momento de procesar).
 */
public class ResultadoProcesoCobro {

    private Long idCobro;
    private Long estado;
    private boolean procesado;
    private String mensaje;

    public Long getIdCobro() {
        return idCobro;
    }

    public void setIdCobro(Long idCobro) {
        this.idCobro = idCobro;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public boolean isProcesado() {
        return procesado;
    }

    public void setProcesado(boolean procesado) {
        this.procesado = procesado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}

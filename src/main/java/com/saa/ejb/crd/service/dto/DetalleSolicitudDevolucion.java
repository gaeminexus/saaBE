package com.saa.ejb.crd.service.dto;

/**
 * Una línea de la solicitud de devolución: cuánto se devuelve de un tipo de aporte.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class DetalleSolicitudDevolucion {

    /** Código del tipo de aporte — TPAP.TPAPCDGO. Debe estar vigente (estado = 1). */
    private Long idTipoAporte;

    /**
     * Valor a devolver de ese tipo, en POSITIVO. Debe ser mayor a cero y no superar el
     * saldo neto disponible del tipo.
     */
    private Double valor;

    public DetalleSolicitudDevolucion() {
    }

    public Long getIdTipoAporte() {
        return idTipoAporte;
    }

    public void setIdTipoAporte(Long idTipoAporte) {
        this.idTipoAporte = idTipoAporte;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }
}

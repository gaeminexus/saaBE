package com.saa.ejb.crd.service.dto;

/**
 * Un renglón del desglose de aportes con el que se paga un préstamo:
 * cuánto tomar de qué tipo de aporte.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §5.1 y §7.4.
 */
public class DesgloseAporte {

    private Long idTipoAporte;
    private Double valor;

    public DesgloseAporte() {
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

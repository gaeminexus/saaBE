package com.saa.ejb.crd.service.dto;

/**
 * Una línea del listado de devoluciones de un partícipe (GET /rest/dvap/porEntidad).
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class DetalleResumenDevolucion {

    private Long idTipoAporte;

    private String nombreTipoAporte;

    /** Valor devuelto de ese tipo, en POSITIVO. */
    private Double valor;

    public DetalleResumenDevolucion() {
    }

    public Long getIdTipoAporte() {
        return idTipoAporte;
    }

    public void setIdTipoAporte(Long idTipoAporte) {
        this.idTipoAporte = idTipoAporte;
    }

    public String getNombreTipoAporte() {
        return nombreTipoAporte;
    }

    public void setNombreTipoAporte(String nombreTipoAporte) {
        this.nombreTipoAporte = nombreTipoAporte;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }
}

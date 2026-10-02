package com.saa.ejb.crd.service.dto;

/** Cuerpo de {@code PUT /posg/sumaAsegurada} (contrato §2). */
public class SolicitudSumaAsegurada {

    private Long idPrestamo;
    private Double valor;
    private String usuario;

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
}

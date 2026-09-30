package com.saa.ejb.crd.service.dto;

/** Un préstamo del lote de {@code POST /rest/plvn/declarar} (§5 del contrato). */
public class ItemDeclararPlazoVencido {

    private Long idPrestamo;
    private String numeroMemorando;

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }

    public String getNumeroMemorando() { return numeroMemorando; }
    public void setNumeroMemorando(String numeroMemorando) { this.numeroMemorando = numeroMemorando; }
}

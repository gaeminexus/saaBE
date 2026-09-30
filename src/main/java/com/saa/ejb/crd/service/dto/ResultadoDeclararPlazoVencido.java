package com.saa.ejb.crd.service.dto;

/** Una fila de la respuesta 201 de {@code POST /rest/plvn/declarar} (§5 del contrato). */
public class ResultadoDeclararPlazoVencido {

    private Long idDeclaracion;
    private Long idPrestamo;
    private String numeroMemorando;
    private Double totalPorCobrar;
    private Long cuotasSinSeguro;

    public Long getIdDeclaracion() { return idDeclaracion; }
    public void setIdDeclaracion(Long idDeclaracion) { this.idDeclaracion = idDeclaracion; }

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }

    public String getNumeroMemorando() { return numeroMemorando; }
    public void setNumeroMemorando(String numeroMemorando) { this.numeroMemorando = numeroMemorando; }

    public Double getTotalPorCobrar() { return totalPorCobrar; }
    public void setTotalPorCobrar(Double totalPorCobrar) { this.totalPorCobrar = totalPorCobrar; }

    public Long getCuotasSinSeguro() { return cuotasSinSeguro; }
    public void setCuotasSinSeguro(Long cuotasSinSeguro) { this.cuotasSinSeguro = cuotasSinSeguro; }
}

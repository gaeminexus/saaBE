package com.saa.ejb.crd.service.dto;

/** Respuesta 200 de {@code POST /rest/plvn/{id}/revertir} (§7 del contrato). */
public class ResultadoRevertirPlazoVencido {

    private Long idDeclaracion;
    private Long idPrestamo;
    private Long cuotasRestituidas;
    private Long cuotasNoRestituidas;

    public Long getIdDeclaracion() { return idDeclaracion; }
    public void setIdDeclaracion(Long idDeclaracion) { this.idDeclaracion = idDeclaracion; }

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }

    public Long getCuotasRestituidas() { return cuotasRestituidas; }
    public void setCuotasRestituidas(Long cuotasRestituidas) { this.cuotasRestituidas = cuotasRestituidas; }

    public Long getCuotasNoRestituidas() { return cuotasNoRestituidas; }
    public void setCuotasNoRestituidas(Long cuotasNoRestituidas) { this.cuotasNoRestituidas = cuotasNoRestituidas; }
}

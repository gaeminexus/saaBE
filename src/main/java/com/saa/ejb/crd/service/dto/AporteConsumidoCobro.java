package com.saa.ejb.crd.service.dto;

/**
 * Un aporte CONSUMIDO por una línea de cobro (CRD.DAPR), tal como lo devuelve
 * {@code GET /rest/cbcr/getId/{id}} en {@code aportesConsumidos} (API-ABONO-CAPITAL-CON-APORTES.md
 * §1.1). Sirve para que la bandeja de contabilidad vea que el abono/precancelación es mayor que el
 * depósito.
 */
public class AporteConsumidoCobro {

    private Long idDetalleCobro;
    private Long idTipoAporte;
    private String nombreTipoAporte;
    private Double valor;

    public AporteConsumidoCobro() {
    }

    public Long getIdDetalleCobro() {
        return idDetalleCobro;
    }

    public void setIdDetalleCobro(Long idDetalleCobro) {
        this.idDetalleCobro = idDetalleCobro;
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

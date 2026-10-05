package com.saa.ejb.crd.service.dto;

/**
 * Resultado de {@code ProvisionInteresService#reversarPorPagos} (diseño §5) — lo que un canal
 * de cobro necesita para informar al usuario y, en el caso de CBCR, llenar
 * {@code ResultadoProcesoCobro}.
 */
public class ResultadoReversoProvision {

    private Long idAsiento;
    private double totalReversado;

    public Long getIdAsiento() { return idAsiento; }
    public void setIdAsiento(Long idAsiento) { this.idAsiento = idAsiento; }

    public double getTotalReversado() { return totalReversado; }
    public void setTotalReversado(double totalReversado) { this.totalReversado = totalReversado; }
}

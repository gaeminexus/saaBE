package com.saa.ejb.crd.service.dto;

/**
 * Resultado de aplicar un abono a capital con re-amortización.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §7.3.
 */
public class ResultadoAbonoCapital {

    private Long idPrestamo;
    private Long idEvento;

    /** PGPR del abono (lleva el monto en saldoOtros, NO en capitalPagado) */
    private Long idPagoPrestamo;

    /** Cuota donde se acumuló el abono en DTPRSLOT (la última pagada, o la primera nueva) */
    private Long idCuotaConSaldoOtros;

    private Double valorAbono;
    private Integer modalidad;

    private Long plazoAnterior;
    private Long plazoNuevo;

    private Double cuotaAnterior;
    private Double cuotaNueva;

    /** Cuotas copiadas a CRD.HDTP y borradas de CRD.DTPR */
    private Integer cuotasHistorizadas;

    /** Cuotas nuevas insertadas en CRD.DTPR */
    private Integer cuotasGeneradas;

    /**
     * Seguro de incendio TOTAL de las cuotas historizadas que no tuvieron cuota nueva
     * correspondiente (modalidad 1, plazo acortado) — 0 en cualquier otro caso. Ver
     * {@link SimulacionAbonoCapital#getSeguroIncendioLiberado()}: mismo dato, futuro insumo
     * de reembolso a la aseguradora, proceso no implementado.
     */
    private Double seguroIncendioLiberado;

    public ResultadoAbonoCapital() {
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Long getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(Long idEvento) {
        this.idEvento = idEvento;
    }

    public Long getIdPagoPrestamo() {
        return idPagoPrestamo;
    }

    public void setIdPagoPrestamo(Long idPagoPrestamo) {
        this.idPagoPrestamo = idPagoPrestamo;
    }

    public Long getIdCuotaConSaldoOtros() {
        return idCuotaConSaldoOtros;
    }

    public void setIdCuotaConSaldoOtros(Long idCuotaConSaldoOtros) {
        this.idCuotaConSaldoOtros = idCuotaConSaldoOtros;
    }

    public Double getValorAbono() {
        return valorAbono;
    }

    public void setValorAbono(Double valorAbono) {
        this.valorAbono = valorAbono;
    }

    public Integer getModalidad() {
        return modalidad;
    }

    public void setModalidad(Integer modalidad) {
        this.modalidad = modalidad;
    }

    public Long getPlazoAnterior() {
        return plazoAnterior;
    }

    public void setPlazoAnterior(Long plazoAnterior) {
        this.plazoAnterior = plazoAnterior;
    }

    public Long getPlazoNuevo() {
        return plazoNuevo;
    }

    public void setPlazoNuevo(Long plazoNuevo) {
        this.plazoNuevo = plazoNuevo;
    }

    public Double getCuotaAnterior() {
        return cuotaAnterior;
    }

    public void setCuotaAnterior(Double cuotaAnterior) {
        this.cuotaAnterior = cuotaAnterior;
    }

    public Double getCuotaNueva() {
        return cuotaNueva;
    }

    public void setCuotaNueva(Double cuotaNueva) {
        this.cuotaNueva = cuotaNueva;
    }

    public Integer getCuotasHistorizadas() {
        return cuotasHistorizadas;
    }

    public void setCuotasHistorizadas(Integer cuotasHistorizadas) {
        this.cuotasHistorizadas = cuotasHistorizadas;
    }

    public Integer getCuotasGeneradas() {
        return cuotasGeneradas;
    }

    public void setCuotasGeneradas(Integer cuotasGeneradas) {
        this.cuotasGeneradas = cuotasGeneradas;
    }

    public Double getSeguroIncendioLiberado() {
        return seguroIncendioLiberado;
    }

    public void setSeguroIncendioLiberado(Double seguroIncendioLiberado) {
        this.seguroIncendioLiberado = seguroIncendioLiberado;
    }
}

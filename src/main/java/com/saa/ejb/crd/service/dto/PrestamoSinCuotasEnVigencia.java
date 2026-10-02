package com.saa.ejb.crd.service.dto;

/** Un préstamo del documento sin ninguna cuota dentro de la vigencia (contrato §5). */
public class PrestamoSinCuotasEnVigencia {

    private Long idPrestamo;
    private String numeroPrestamo;

    public PrestamoSinCuotasEnVigencia() {
    }

    public PrestamoSinCuotasEnVigencia(Long idPrestamo, String numeroPrestamo) {
        this.idPrestamo = idPrestamo;
        this.numeroPrestamo = numeroPrestamo;
    }

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }

    public String getNumeroPrestamo() { return numeroPrestamo; }
    public void setNumeroPrestamo(String numeroPrestamo) { this.numeroPrestamo = numeroPrestamo; }
}

package com.saa.ejb.crd.service.dto;

/**
 * C35: una cuota con {@code DTPRSLOT} mayor que lo que respaldan sus pagos vigentes ({@code Σ PGPRSLOT}). Ver
 * {@link com.saa.ejb.crd.service.SaldoOtrosRespaldo}.
 */
public class ResiduoSaldoOtros {

    private Long idPrestamo;
    private Long idCuota;
    private Long numeroCuota;
    private double saldoOtros;
    private double respaldado;
    private double residuo;

    public ResiduoSaldoOtros() {
    }

    public ResiduoSaldoOtros(Long idPrestamo, Long idCuota, Long numeroCuota, double saldoOtros, double respaldado,
            double residuo) {
        this.idPrestamo = idPrestamo;
        this.idCuota = idCuota;
        this.numeroCuota = numeroCuota;
        this.saldoOtros = saldoOtros;
        this.respaldado = respaldado;
        this.residuo = residuo;
    }

    /** "cuota #52: DTPRSLOT 61498.46, respaldado 30749.23, sin respaldo 30749.23". */
    public String texto() {
        return "cuota #" + numeroCuota + ": DTPRSLOT " + formato(saldoOtros) + ", respaldado por pagos "
            + formato(respaldado) + ", sin respaldo " + formato(residuo);
    }

    private static String formato(double v) {
        return String.format(java.util.Locale.US, "%.2f", v);
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Long getIdCuota() {
        return idCuota;
    }

    public void setIdCuota(Long idCuota) {
        this.idCuota = idCuota;
    }

    public Long getNumeroCuota() {
        return numeroCuota;
    }

    public void setNumeroCuota(Long numeroCuota) {
        this.numeroCuota = numeroCuota;
    }

    public double getSaldoOtros() {
        return saldoOtros;
    }

    public void setSaldoOtros(double saldoOtros) {
        this.saldoOtros = saldoOtros;
    }

    public double getRespaldado() {
        return respaldado;
    }

    public void setRespaldado(double respaldado) {
        this.respaldado = respaldado;
    }

    public double getResiduo() {
        return residuo;
    }

    public void setResiduo(double residuo) {
        this.residuo = residuo;
    }
}

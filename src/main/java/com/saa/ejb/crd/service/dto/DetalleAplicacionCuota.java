package com.saa.ejb.crd.service.dto;

/**
 * Detalle de lo que un pago aplicó sobre UNA cuota: estados antes/después,
 * el desglose por componente y el PagoPrestamo creado.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §5.1.
 */
public class DetalleAplicacionCuota {

    private Long idCuota;
    private Double numeroCuota;

    /** Estado de la cuota antes del pago (rubro EstadoCuotaPrestamo) */
    private Long estadoAnterior;

    /** Estado de la cuota después del pago (rubro EstadoCuotaPrestamo) */
    private Long estadoNuevo;

    private double aplicadoDesgravamen;
    private double aplicadoMora;
    private double aplicadoInteresVencido;
    private double aplicadoInteres;
    private double aplicadoCapital;
    private double aplicadoSeguro;

    /** Suma de los 6 componentes aplicados en esta operación */
    private double totalAplicado;

    /** PGPR creado para esta cuota (null si no se aplicó nada) */
    private Long idPagoPrestamo;

    public DetalleAplicacionCuota() {
    }

    public Long getIdCuota() {
        return idCuota;
    }

    public void setIdCuota(Long idCuota) {
        this.idCuota = idCuota;
    }

    public Double getNumeroCuota() {
        return numeroCuota;
    }

    public void setNumeroCuota(Double numeroCuota) {
        this.numeroCuota = numeroCuota;
    }

    public Long getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(Long estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public Long getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(Long estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public double getAplicadoDesgravamen() {
        return aplicadoDesgravamen;
    }

    public void setAplicadoDesgravamen(double aplicadoDesgravamen) {
        this.aplicadoDesgravamen = aplicadoDesgravamen;
    }

    public double getAplicadoMora() {
        return aplicadoMora;
    }

    public void setAplicadoMora(double aplicadoMora) {
        this.aplicadoMora = aplicadoMora;
    }

    public double getAplicadoInteresVencido() {
        return aplicadoInteresVencido;
    }

    public void setAplicadoInteresVencido(double aplicadoInteresVencido) {
        this.aplicadoInteresVencido = aplicadoInteresVencido;
    }

    public double getAplicadoInteres() {
        return aplicadoInteres;
    }

    public void setAplicadoInteres(double aplicadoInteres) {
        this.aplicadoInteres = aplicadoInteres;
    }

    public double getAplicadoCapital() {
        return aplicadoCapital;
    }

    public void setAplicadoCapital(double aplicadoCapital) {
        this.aplicadoCapital = aplicadoCapital;
    }

    public double getAplicadoSeguro() {
        return aplicadoSeguro;
    }

    public void setAplicadoSeguro(double aplicadoSeguro) {
        this.aplicadoSeguro = aplicadoSeguro;
    }

    public double getTotalAplicado() {
        return totalAplicado;
    }

    public void setTotalAplicado(double totalAplicado) {
        this.totalAplicado = totalAplicado;
    }

    public Long getIdPagoPrestamo() {
        return idPagoPrestamo;
    }

    public void setIdPagoPrestamo(Long idPagoPrestamo) {
        this.idPagoPrestamo = idPagoPrestamo;
    }
}

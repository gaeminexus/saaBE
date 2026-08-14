package com.saa.ejb.crd.service.dto;

/**
 * Saldos reales de una cuota, reconstruidos desde los PagoPrestamo VIGENTES.
 * Los campos están en el ORDEN DE PRELACIÓN con el que el motor imputa un pago.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §5.1 y §6.2.
 */
public class SaldosCuota {

    /** 1. Desgravamen pendiente */
    private double saldoDesgravamen;

    /** 2. Mora pendiente */
    private double saldoMora;

    /** 3. Interés vencido pendiente */
    private double saldoInteresVencido;

    /** 4. Interés corriente pendiente */
    private double saldoInteres;

    /** 5. Capital pendiente */
    private double saldoCapital;

    /** 6. Seguro de incendio pendiente */
    private double saldoSeguroIncendio;

    /** Suma de los 6 componentes */
    private double totalPendiente;

    public SaldosCuota() {
    }

    public double getSaldoDesgravamen() {
        return saldoDesgravamen;
    }

    public void setSaldoDesgravamen(double saldoDesgravamen) {
        this.saldoDesgravamen = saldoDesgravamen;
    }

    public double getSaldoMora() {
        return saldoMora;
    }

    public void setSaldoMora(double saldoMora) {
        this.saldoMora = saldoMora;
    }

    public double getSaldoInteresVencido() {
        return saldoInteresVencido;
    }

    public void setSaldoInteresVencido(double saldoInteresVencido) {
        this.saldoInteresVencido = saldoInteresVencido;
    }

    public double getSaldoInteres() {
        return saldoInteres;
    }

    public void setSaldoInteres(double saldoInteres) {
        this.saldoInteres = saldoInteres;
    }

    public double getSaldoCapital() {
        return saldoCapital;
    }

    public void setSaldoCapital(double saldoCapital) {
        this.saldoCapital = saldoCapital;
    }

    public double getSaldoSeguroIncendio() {
        return saldoSeguroIncendio;
    }

    public void setSaldoSeguroIncendio(double saldoSeguroIncendio) {
        this.saldoSeguroIncendio = saldoSeguroIncendio;
    }

    public double getTotalPendiente() {
        return totalPendiente;
    }

    public void setTotalPendiente(double totalPendiente) {
        this.totalPendiente = totalPendiente;
    }
}

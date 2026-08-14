package com.saa.ejb.crd.service.dto;

/**
 * Saldos reales de una cuota, reconstruidos desde los PagoPrestamo VIGENTES.
 *
 * El ORDEN DE PRELACIÓN con el que el motor imputa un pago es el que indican los comentarios
 * de cada campo (confirmado por negocio el 2026-08-14):
 * seguro de incendio → desgravamen → mora → interés vencido → interés ordinario → capital.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §5.1 y §6.2.
 */
public class SaldosCuota {

    /** Prelación 2. Seguro de desgravamen pendiente */
    private double saldoDesgravamen;

    /** Prelación 3. Interés de mora pendiente */
    private double saldoMora;

    /** Prelación 4. Interés vencido pendiente (hoy siempre 0) */
    private double saldoInteresVencido;

    /** Prelación 5. Interés ordinario pendiente */
    private double saldoInteres;

    /** Prelación 6. Capital pendiente */
    private double saldoCapital;

    /** Prelación 1. Seguro de incendio pendiente */
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

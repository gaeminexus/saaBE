package com.saa.ejb.crd.service.dto;

/**
 * Saldo vigente de un préstamo, calculado en lote desde sus cuotas pendientes
 * (docs/logica-negocio/crd/API-SALDOS-PRESTAMO.md). Reemplaza la lectura de
 * {@code Prestamo.saldoTotal}/{@code saldoCapital} (PRSTSLTT/PRSTSLCP), columnas que
 * ninguna línea del backend escribe y que no se mueven con los pagos.
 */
public class SaldoPrestamoResumen {

    /** Código del préstamo (PRSTCDGO). */
    private Long idPrestamo;

    /** Suma de SaldosCuota.saldoCapital sobre las cuotas pendientes. Redondeado a 2 decimales. */
    private Double saldoCapital;

    /** Suma de SaldosCuota.totalPendiente sobre las cuotas pendientes. Redondeado a 2 decimales. */
    private Double saldoTotal;

    /**
     * Σ PGPRCPPG (solo el capital pagado, SIN sumar interés/mora/desgravamen/seguros) de
     * TODOS los pagos vigentes del préstamo — acumulado histórico, no sólo de cuotas
     * pendientes. "Capital Pagado" en pantalla. 0.0 si no tiene pagos vigentes, nunca null.
     */
    private Double capitalPagado;

    /** Cuotas pendientes con fechaVencimiento anterior al corte de hoy. */
    private Long cuotasEnMora;

    public SaldoPrestamoResumen() {
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Double getSaldoCapital() {
        return saldoCapital;
    }

    public void setSaldoCapital(Double saldoCapital) {
        this.saldoCapital = saldoCapital;
    }

    public Double getSaldoTotal() {
        return saldoTotal;
    }

    public void setSaldoTotal(Double saldoTotal) {
        this.saldoTotal = saldoTotal;
    }

    public Double getCapitalPagado() {
        return capitalPagado;
    }

    public void setCapitalPagado(Double capitalPagado) {
        this.capitalPagado = capitalPagado;
    }

    public Long getCuotasEnMora() {
        return cuotasEnMora;
    }

    public void setCuotasEnMora(Long cuotasEnMora) {
        this.cuotasEnMora = cuotasEnMora;
    }
}

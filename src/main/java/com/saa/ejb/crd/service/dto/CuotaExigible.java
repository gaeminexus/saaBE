package com.saa.ejb.crd.service.dto;

import java.time.LocalDateTime;

/**
 * Una cuota de la deuda EXIGIBLE a la fecha de corte de una precancelación:
 * pendiente y con vencimiento &lt;= la fecha.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §7.5.
 */
public class CuotaExigible {

    private Long idCuota;
    private Double numeroCuota;
    private LocalDateTime fechaVencimiento;

    /** Total pendiente real de la cuota, incluyendo mora e interés vencido si los hay */
    private Double pendiente;

    public CuotaExigible() {
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

    public LocalDateTime getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDateTime fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public Double getPendiente() {
        return pendiente;
    }

    public void setPendiente(Double pendiente) {
        this.pendiente = pendiente;
    }
}

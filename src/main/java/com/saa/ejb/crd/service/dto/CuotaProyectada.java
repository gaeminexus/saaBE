package com.saa.ejb.crd.service.dto;

import java.time.LocalDateTime;

/**
 * Una cuota de la tabla de amortización PROYECTADA por la simulación de un abono a capital.
 * No se persiste: solo viaja en la respuesta para que el usuario vea cómo quedaría el préstamo.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §7.3.
 */
public class CuotaProyectada {

    private Double numeroCuota;
    private LocalDateTime fechaVencimiento;
    private Double capital;
    private Double interes;

    /** capital + interés */
    private Double cuota;

    /** Capital pendiente DESPUÉS de esta cuota */
    private Double saldoCapital;

    /** Desgravamen de la cuota. Null si quien la generó no lo calcula (p.ej. simularAbonoCapital). */
    private Double desgravamen;

    /** Seguro de incendio de la cuota. Null si quien la generó no lo calcula (p.ej. simularAbonoCapital). */
    private Double seguroIncendio;

    /** cuota + desgravamen + seguroIncendio (mismo invariante que DTPRTTLL). Null si desgravamen/seguroIncendio son null. */
    private Double total;

    public CuotaProyectada() {
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

    public Double getCapital() {
        return capital;
    }

    public void setCapital(Double capital) {
        this.capital = capital;
    }

    public Double getInteres() {
        return interes;
    }

    public void setInteres(Double interes) {
        this.interes = interes;
    }

    public Double getCuota() {
        return cuota;
    }

    public void setCuota(Double cuota) {
        this.cuota = cuota;
    }

    public Double getSaldoCapital() {
        return saldoCapital;
    }

    public void setSaldoCapital(Double saldoCapital) {
        this.saldoCapital = saldoCapital;
    }

    public Double getDesgravamen() {
        return desgravamen;
    }

    public void setDesgravamen(Double desgravamen) {
        this.desgravamen = desgravamen;
    }

    public Double getSeguroIncendio() {
        return seguroIncendio;
    }

    public void setSeguroIncendio(Double seguroIncendio) {
        this.seguroIncendio = seguroIncendio;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }
}

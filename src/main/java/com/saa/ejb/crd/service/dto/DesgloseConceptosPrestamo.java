package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/**
 * Desglose de TODO lo pendiente de un préstamo a una fecha, por los 5 conceptos de
 * {@link com.saa.rubros.CrdConceptoPrestamo} — capital, interés (ordinario + vencido, es una
 * sola línea, ver el javadoc del rubro), mora (recalculada fresca con
 * {@code ProcesoMoraPrestamoService.calcularMoraCuota}), desgravamen y seguro de incendio.
 *
 * A diferencia de {@link SimulacionPrecancelacion} (que separa exigibles de futuras porque
 * condona el interés futuro pero no el capital futuro), este desglose suma TODAS las cuotas
 * pendientes sin distinción: un acuerdo de condonación liquida el préstamo completo en el
 * acto (K1), no hay componente "futuro" que tratar distinto.
 *
 * Es la base para la staleness de {@code AcuerdoCondonacionService#aprobarCondonacion} (§3 de
 * {@code PLAN-ACUERDOS-PAGO-CONDONACION.md}): se recalcula al aprobar y se compara contra lo
 * que quedó registrado.
 */
public class DesgloseConceptosPrestamo {

    private Long idPrestamo;
    private LocalDate fecha;
    private double capitalPendiente;
    private double interesPendiente;
    private double moraPendiente;
    private double desgravamenPendiente;
    private double seguroIncendioPendiente;

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public double getCapitalPendiente() {
        return capitalPendiente;
    }

    public void setCapitalPendiente(double capitalPendiente) {
        this.capitalPendiente = capitalPendiente;
    }

    public double getInteresPendiente() {
        return interesPendiente;
    }

    public void setInteresPendiente(double interesPendiente) {
        this.interesPendiente = interesPendiente;
    }

    public double getMoraPendiente() {
        return moraPendiente;
    }

    public void setMoraPendiente(double moraPendiente) {
        this.moraPendiente = moraPendiente;
    }

    public double getDesgravamenPendiente() {
        return desgravamenPendiente;
    }

    public void setDesgravamenPendiente(double desgravamenPendiente) {
        this.desgravamenPendiente = desgravamenPendiente;
    }

    public double getSeguroIncendioPendiente() {
        return seguroIncendioPendiente;
    }

    public void setSeguroIncendioPendiente(double seguroIncendioPendiente) {
        this.seguroIncendioPendiente = seguroIncendioPendiente;
    }
}

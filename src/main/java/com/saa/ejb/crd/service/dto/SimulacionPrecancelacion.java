package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Cálculo canónico de una precancelación: deuda EXIGIBLE a la fecha (con su mora) más SOLO el
 * capital pendiente de las cuotas futuras. Intereses, desgravamen y seguros futuros se condonan.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §7.5.
 */
public class SimulacionPrecancelacion {

    private Long idPrestamo;

    /** Fecha de corte del cálculo */
    private LocalDate fecha;

    /** Cuotas pendientes con vencimiento &lt;= fecha */
    private List<CuotaExigible> exigibles = new ArrayList<>();

    /** Suma del pendiente real de las cuotas exigibles */
    private Double valorExigible;

    /** Suma del saldo de capital de las cuotas futuras */
    private Double capitalFuturo;

    /** valorExigible + capitalFuturo: lo que hay que pagar para precancelar */
    private Double valorTotalPrecancelacion;

    /** Cantidad de cuotas futuras que pasarían a CANCELADA_ANTICIPADA (7) */
    private Integer cuotasAAnular;

    /** Interés, desgravamen y seguros futuros que se condonan (informativo) */
    private Double interesCondonado;

    public SimulacionPrecancelacion() {
    }

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

    public List<CuotaExigible> getExigibles() {
        return exigibles;
    }

    public void setExigibles(List<CuotaExigible> exigibles) {
        this.exigibles = exigibles;
    }

    public Double getValorExigible() {
        return valorExigible;
    }

    public void setValorExigible(Double valorExigible) {
        this.valorExigible = valorExigible;
    }

    public Double getCapitalFuturo() {
        return capitalFuturo;
    }

    public void setCapitalFuturo(Double capitalFuturo) {
        this.capitalFuturo = capitalFuturo;
    }

    public Double getValorTotalPrecancelacion() {
        return valorTotalPrecancelacion;
    }

    public void setValorTotalPrecancelacion(Double valorTotalPrecancelacion) {
        this.valorTotalPrecancelacion = valorTotalPrecancelacion;
    }

    public Integer getCuotasAAnular() {
        return cuotasAAnular;
    }

    public void setCuotasAAnular(Integer cuotasAAnular) {
        this.cuotasAAnular = cuotasAAnular;
    }

    public Double getInteresCondonado() {
        return interesCondonado;
    }

    public void setInteresCondonado(Double interesCondonado) {
        this.interesCondonado = interesCondonado;
    }
}

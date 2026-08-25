package com.saa.ejb.crd.service.dto;

/**
 * Una línea de banda dentro de {@link SolicitudConfiguracionBanda}.
 *
 * El cliente NO manda rangos en días: manda cuántos períodos de 30 días abarca la banda y
 * el backend deriva el rango. {@code periodos} nulo = banda abierta ("el resto"), y solo
 * la última línea puede traerlo.
 */
public class SolicitudBanda {

    /** Número de banda, 1..N consecutivo. Obligatorio. */
    private Long numero;

    /** Períodos de 30 días. NULO = banda abierta; solo válido en la última. */
    private Long periodos;

    /** Código de la cuenta contable del capital de esta banda (CNT.PLNN.PLNNCDGO). Obligatorio. */
    private Long idPlanCuenta;

    public SolicitudBanda() {
    }

    public Long getNumero() {
        return numero;
    }

    public void setNumero(Long numero) {
        this.numero = numero;
    }

    public Long getPeriodos() {
        return periodos;
    }

    public void setPeriodos(Long periodos) {
        this.periodos = periodos;
    }

    public Long getIdPlanCuenta() {
        return idPlanCuenta;
    }

    public void setIdPlanCuenta(Long idPlanCuenta) {
        this.idPlanCuenta = idPlanCuenta;
    }
}

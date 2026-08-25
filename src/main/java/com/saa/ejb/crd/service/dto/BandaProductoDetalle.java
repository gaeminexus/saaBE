package com.saa.ejb.crd.service.dto;

/**
 * Una banda de una configuración, con su rango en días YA DERIVADO y su cuenta contable
 * resuelta. Es lo que consumen la pantalla de parametrización y los procesos contables:
 * nadie más debe volver a acumular períodos.
 *
 * <pre>
 *   diaInicio(k) = 30 * SUM(periodos 1..k-1) + 1
 *   diaFin(k)    = 30 * SUM(periodos 1..k)
 * </pre>
 *
 * En la banda abierta ({@code periodos} nulo) {@code diaFin} viene nulo y
 * {@code etiqueta} dice "resto".
 */
public class BandaProductoDetalle {

    /** Código de la banda (CRD.BNDP.BNDPCDGO). Nulo en una banda todavía no grabada. */
    private Long idBanda;

    /** Número de banda, 1..N consecutivo dentro de la configuración. */
    private Long numero;

    /** Períodos de 30 días que abarca. Nulo = banda abierta ("el resto"). */
    private Long periodos;

    /** Primer día del rango, inclusive. Siempre 1 en la banda 1. */
    private Long diaInicio;

    /** Último día del rango, inclusive. NULO en la banda abierta. */
    private Long diaFin;

    /** Etiqueta legible del rango: "1 - 30", "31 - 90", "más de 360 (resto)". */
    private String etiqueta;

    /** Código de la cuenta contable (CNT.PLNN.PLNNCDGO). */
    private Long idPlanCuenta;

    /** Cuenta contable con puntos, p.ej. "1.3.01.05". */
    private String cuentaContable;

    /** Nombre de la cuenta contable, p.ej. "DE 1 A 30 DIAS". */
    private String nombreCuenta;

    /** Estado de la banda: 1 = activo. */
    private Long estado;

    public BandaProductoDetalle() {
    }

    public Long getIdBanda() {
        return idBanda;
    }

    public void setIdBanda(Long idBanda) {
        this.idBanda = idBanda;
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

    public Long getDiaInicio() {
        return diaInicio;
    }

    public void setDiaInicio(Long diaInicio) {
        this.diaInicio = diaInicio;
    }

    public Long getDiaFin() {
        return diaFin;
    }

    public void setDiaFin(Long diaFin) {
        this.diaFin = diaFin;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public void setEtiqueta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public Long getIdPlanCuenta() {
        return idPlanCuenta;
    }

    public void setIdPlanCuenta(Long idPlanCuenta) {
        this.idPlanCuenta = idPlanCuenta;
    }

    public String getCuentaContable() {
        return cuentaContable;
    }

    public void setCuentaContable(String cuentaContable) {
        this.cuentaContable = cuentaContable;
    }

    public String getNombreCuenta() {
        return nombreCuenta;
    }

    public void setNombreCuenta(String nombreCuenta) {
        this.nombreCuenta = nombreCuenta;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}

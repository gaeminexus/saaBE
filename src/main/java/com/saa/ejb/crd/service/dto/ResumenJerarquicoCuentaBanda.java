package com.saa.ejb.crd.service.dto;

/**
 * Segundo nivel de {@code resumenJerarquico}: cuenta contable y banda dentro de un
 * {@link ResumenJerarquicoConcepto} — API-AUDITORIA-BANDAS.md "Las DOS vistas".
 *
 * Sin CNT conectado, {@link #cuentaContable}/{@link #nombreCuenta} vienen null y este nivel
 * agrupa solo por banda. En conceptos sin banda (todo salvo CAPITAL, ver
 * {@link com.saa.rubros.DsbnConcepto}) los cuatro campos vienen null: el concepto entero cae en
 * un único renglón de detalle.
 */
public class ResumenJerarquicoCuentaBanda {

    private String cuentaContable;
    private String nombreCuenta;
    private Long idBanda;
    private String banda;
    private double valor;
    private long filas;

    public ResumenJerarquicoCuentaBanda() {
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

    public Long getIdBanda() {
        return idBanda;
    }

    public void setIdBanda(Long idBanda) {
        this.idBanda = idBanda;
    }

    public String getBanda() {
        return banda;
    }

    public void setBanda(String banda) {
        this.banda = banda;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public long getFilas() {
        return filas;
    }

    public void setFilas(long filas) {
        this.filas = filas;
    }
}

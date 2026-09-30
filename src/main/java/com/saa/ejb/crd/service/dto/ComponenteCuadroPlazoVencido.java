package com.saa.ejb.crd.service.dto;

/**
 * Una fila del cuadro de plazo vencido (capital, interés, desgravamen, seguro o mora):
 * devengado, cobrado y saldo. {@code saldo = devengado - cobrado} siempre
 * (API-PASE-A-PLAZO-VENCIDO.md §2/§3).
 */
public class ComponenteCuadroPlazoVencido {

    private Double devengado;
    private Double cobrado;
    private Double saldo;

    public ComponenteCuadroPlazoVencido() {}

    public ComponenteCuadroPlazoVencido(Double devengado, Double cobrado, Double saldo) {
        this.devengado = devengado;
        this.cobrado = cobrado;
        this.saldo = saldo;
    }

    public Double getDevengado() { return devengado; }
    public void setDevengado(Double devengado) { this.devengado = devengado; }

    public Double getCobrado() { return cobrado; }
    public void setCobrado(Double cobrado) { this.cobrado = cobrado; }

    public Double getSaldo() { return saldo; }
    public void setSaldo(Double saldo) { this.saldo = saldo; }
}

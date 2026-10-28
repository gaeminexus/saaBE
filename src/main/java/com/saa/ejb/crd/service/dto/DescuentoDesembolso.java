package com.saa.ejb.crd.service.dto;

/**
 * C16 (D-021) — un descuento del desembolso de un préstamo que NO es la precancelación del préstamo
 * anterior (comisión, retención, aporte...). Se resta del bruto para llegar al neto transferido y se
 * asienta DEBE socios por pagar (la cuenta puente de la entrega) / HABER {@code cuenta}.
 * En junio 2025 no hay ninguno: queda para casos futuros.
 */
public class DescuentoDesembolso {

    private String concepto;
    private Double valor;
    /** Cuenta contable de destino del descuento, como texto del plan (p. ej. "2.5.90.90"). */
    private String cuenta;

    public DescuentoDesembolso() {
    }

    public DescuentoDesembolso(String concepto, Double valor, String cuenta) {
        this.concepto = concepto;
        this.valor = valor;
        this.cuenta = cuenta;
    }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
    public String getCuenta() { return cuenta; }
    public void setCuenta(String cuenta) { this.cuenta = cuenta; }
}

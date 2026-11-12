package com.saa.ejb.rpr.service.dto;

/**
 * C27-E — una línea del Resumen Financiero de Informes Mensuales: el valor del informe (CCPM/CPRM
 * de la ejecución) junto al saldo contable de su cuenta equivalente al corte y la diferencia.
 * Contrato de {@code GET /rest/ejcc/resumenFinanciero/{codigo}}.
 */
public class LineaResumenFinanciero {

    /** Grupo de la línea ("CARTERA POR VENCER", "PROVISIONES", ...). */
    private String grupo;
    /** Concepto (nombre de la cuenta o del rubro). */
    private String concepto;
    /** Cuenta contable equivalente (con puntos); nula si la línea no tiene cuenta. */
    private String cuenta;
    /** Valor del informe, redondeado a 2 decimales. */
    private Double valorInforme;
    /** Saldo contable de la cuenta al corte, en el signo de la naturaleza de la línea; nulo sin cuenta. */
    private Double saldoContable;
    /** valorInforme - saldoContable; nula si la línea no es comparable. */
    private Double diferencia;
    /** true si la línea debe cuadrar con su cuenta; false si es informativa. */
    private boolean comparable;
    /** Operaciones (o partícipes) que suman en la línea. */
    private Long registros;
    /** Aclaración de la línea (definición, por qué no es comparable). */
    private String nota;
    /** true en las líneas de subtotal/total. */
    private boolean total;

    public String getGrupo() { return grupo; }
    public void setGrupo(String grupo) { this.grupo = grupo; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public String getCuenta() { return cuenta; }
    public void setCuenta(String cuenta) { this.cuenta = cuenta; }
    public Double getValorInforme() { return valorInforme; }
    public void setValorInforme(Double valorInforme) { this.valorInforme = valorInforme; }
    public Double getSaldoContable() { return saldoContable; }
    public void setSaldoContable(Double saldoContable) { this.saldoContable = saldoContable; }
    public Double getDiferencia() { return diferencia; }
    public void setDiferencia(Double diferencia) { this.diferencia = diferencia; }
    public boolean isComparable() { return comparable; }
    public void setComparable(boolean comparable) { this.comparable = comparable; }
    public Long getRegistros() { return registros; }
    public void setRegistros(Long registros) { this.registros = registros; }
    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }
    public boolean isTotal() { return total; }
    public void setTotal(boolean total) { this.total = total; }
}

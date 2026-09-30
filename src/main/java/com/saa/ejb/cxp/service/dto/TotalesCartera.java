package com.saa.ejb.cxp.service.dto;

import java.io.Serializable;

/**
 * Totales del reporte de cartera completo: la suma de {@link ResumenTitularCartera} de todos los
 * titulares (API-CARTERA-CXP-CXC.md §4).
 *
 * POJO plano: getters y setters escritos a mano, sin anotaciones de Jackson.
 */
public class TotalesCartera implements Serializable {

    private static final long serialVersionUID = 1L;

    private int titulares;
    private int documentos;
    private double total;
    private double aplicado;
    private double saldo;
    private double porVencer;
    private double d1a30;
    private double d31a60;
    private double d61a90;
    private double mas90;
    private double anticiposDisponibles;
    private double saldoNeto;

    public int getTitulares() { return titulares; }
    public void setTitulares(int titulares) { this.titulares = titulares; }

    public int getDocumentos() { return documentos; }
    public void setDocumentos(int documentos) { this.documentos = documentos; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public double getAplicado() { return aplicado; }
    public void setAplicado(double aplicado) { this.aplicado = aplicado; }

    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }

    public double getPorVencer() { return porVencer; }
    public void setPorVencer(double porVencer) { this.porVencer = porVencer; }

    public double getD1a30() { return d1a30; }
    public void setD1a30(double d1a30) { this.d1a30 = d1a30; }

    public double getD31a60() { return d31a60; }
    public void setD31a60(double d31a60) { this.d31a60 = d31a60; }

    public double getD61a90() { return d61a90; }
    public void setD61a90(double d61a90) { this.d61a90 = d61a90; }

    public double getMas90() { return mas90; }
    public void setMas90(double mas90) { this.mas90 = mas90; }

    public double getAnticiposDisponibles() { return anticiposDisponibles; }
    public void setAnticiposDisponibles(double anticiposDisponibles) { this.anticiposDisponibles = anticiposDisponibles; }

    public double getSaldoNeto() { return saldoNeto; }
    public void setSaldoNeto(double saldoNeto) { this.saldoNeto = saldoNeto; }
}

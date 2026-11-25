package com.saa.ejb.crd.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Lote 21 (A1): respuesta del ajuste del libro de provisión por cuota (MVIC 13 y 14) al corte. */
public class ResultadoAjusteLibroProvision implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Por tipo de préstamo y componente. {@code baja} y {@code alta} son valores positivos; {@code neto} = alta − baja es lo que cambia el saldo
     * provisionado (positivo sube la 1.4.99.05, negativo la baja): lo que el cuadre cruza contra su asiento manual.
     */
    public static class Tipo implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long idTipoPrestamo;
        private double bajaInteres;
        private double altaInteres;
        private double netoInteres;
        private double bajaMora;
        private double altaMora;
        private double netoMora;

        public Long getIdTipoPrestamo() { return idTipoPrestamo; }
        public void setIdTipoPrestamo(Long idTipoPrestamo) { this.idTipoPrestamo = idTipoPrestamo; }
        public double getBajaInteres() { return bajaInteres; }
        public void setBajaInteres(double bajaInteres) { this.bajaInteres = bajaInteres; }
        public double getAltaInteres() { return altaInteres; }
        public void setAltaInteres(double altaInteres) { this.altaInteres = altaInteres; }
        public double getNetoInteres() { return netoInteres; }
        public void setNetoInteres(double netoInteres) { this.netoInteres = netoInteres; }
        public double getBajaMora() { return bajaMora; }
        public void setBajaMora(double bajaMora) { this.bajaMora = bajaMora; }
        public double getAltaMora() { return altaMora; }
        public void setAltaMora(double altaMora) { this.altaMora = altaMora; }
        public double getNetoMora() { return netoMora; }
        public void setNetoMora(double netoMora) { this.netoMora = netoMora; }
    }

    private String corte;
    private Long idAsientoReferencia;
    private boolean grabado;
    /** {@code true} si la referencia ya tiene sus ajustes vigentes: no se repite. */
    private boolean yaGrabado;
    private int cuotasAjustadas;
    private List<Tipo> porTipo = new ArrayList<Tipo>();
    /** Lo que baja por cuotas cerradas (estado 7, pagadas o fuera del universo del ⑦): provisión que ya no tiene deuda. */
    private double deCuotasCerradas;
    /** Lo que sube por provisiones negativas (hasta llevarlas a 0). */
    private double deNegativas;
    /** Total que baja y total que sube, todos los tipos y componentes. */
    private double totalBaja;
    private double totalAlta;
    private List<String> advertencias = new ArrayList<String>();

    public String getCorte() { return corte; }
    public void setCorte(String corte) { this.corte = corte; }
    public Long getIdAsientoReferencia() { return idAsientoReferencia; }
    public void setIdAsientoReferencia(Long idAsientoReferencia) { this.idAsientoReferencia = idAsientoReferencia; }
    public boolean isGrabado() { return grabado; }
    public void setGrabado(boolean grabado) { this.grabado = grabado; }
    public boolean isYaGrabado() { return yaGrabado; }
    public void setYaGrabado(boolean yaGrabado) { this.yaGrabado = yaGrabado; }
    public int getCuotasAjustadas() { return cuotasAjustadas; }
    public void setCuotasAjustadas(int cuotasAjustadas) { this.cuotasAjustadas = cuotasAjustadas; }
    public List<Tipo> getPorTipo() { return porTipo; }
    public void setPorTipo(List<Tipo> porTipo) { this.porTipo = porTipo; }
    public double getDeCuotasCerradas() { return deCuotasCerradas; }
    public void setDeCuotasCerradas(double deCuotasCerradas) { this.deCuotasCerradas = deCuotasCerradas; }
    public double getDeNegativas() { return deNegativas; }
    public void setDeNegativas(double deNegativas) { this.deNegativas = deNegativas; }
    public double getTotalBaja() { return totalBaja; }
    public void setTotalBaja(double totalBaja) { this.totalBaja = totalBaja; }
    public double getTotalAlta() { return totalAlta; }
    public void setTotalAlta(double totalAlta) { this.totalAlta = totalAlta; }
    public List<String> getAdvertencias() { return advertencias; }
    public void setAdvertencias(List<String> advertencias) { this.advertencias = advertencias; }
}

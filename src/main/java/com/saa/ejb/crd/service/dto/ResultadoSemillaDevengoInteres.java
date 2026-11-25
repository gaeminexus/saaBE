package com.saa.ejb.crd.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Lote 21 (A2): respuesta de la semilla del libro de devengo de interés (MVIC 11) al corte. */
public class ResultadoSemillaDevengoInteres implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Suma por tipo de préstamo de lo que se siembra (o ya se sembró). */
    public static class Tipo implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long idTipoPrestamo;
        private int cuotas;
        private double valor;

        public Long getIdTipoPrestamo() { return idTipoPrestamo; }
        public void setIdTipoPrestamo(Long idTipoPrestamo) { this.idTipoPrestamo = idTipoPrestamo; }
        public int getCuotas() { return cuotas; }
        public void setCuotas(int cuotas) { this.cuotas = cuotas; }
        public double getValor() { return valor; }
        public void setValor(double valor) { this.valor = valor; }
    }

    private String corte;
    private Long idAsientoReferencia;
    private boolean grabado;
    /** {@code true} si ya había filas vigentes de esta referencia: no se repite y lo devuelto es lo ya grabado. */
    private boolean yaGrabado;
    private int cuotasSembradas;
    private double total;
    private List<Tipo> porTipo = new ArrayList<Tipo>();
    /** Cuotas que ya tienen un MVIC 11 o 12 vigente con fecha ≤ corte (las que el ④ nuevo ya alimentó): no se siembran. */
    private int cuotasYaAlimentadas;
    private List<String> advertencias = new ArrayList<String>();

    public String getCorte() { return corte; }
    public void setCorte(String corte) { this.corte = corte; }
    public Long getIdAsientoReferencia() { return idAsientoReferencia; }
    public void setIdAsientoReferencia(Long idAsientoReferencia) { this.idAsientoReferencia = idAsientoReferencia; }
    public boolean isGrabado() { return grabado; }
    public void setGrabado(boolean grabado) { this.grabado = grabado; }
    public boolean isYaGrabado() { return yaGrabado; }
    public void setYaGrabado(boolean yaGrabado) { this.yaGrabado = yaGrabado; }
    public int getCuotasSembradas() { return cuotasSembradas; }
    public void setCuotasSembradas(int cuotasSembradas) { this.cuotasSembradas = cuotasSembradas; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public List<Tipo> getPorTipo() { return porTipo; }
    public void setPorTipo(List<Tipo> porTipo) { this.porTipo = porTipo; }
    public int getCuotasYaAlimentadas() { return cuotasYaAlimentadas; }
    public void setCuotasYaAlimentadas(int cuotasYaAlimentadas) { this.cuotasYaAlimentadas = cuotasYaAlimentadas; }
    public List<String> getAdvertencias() { return advertencias; }
    public void setAdvertencias(List<String> advertencias) { this.advertencias = advertencias; }
}

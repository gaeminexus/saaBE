package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/**
 * C15 — una operación de crédito tal como estaba a una fecha de corte, con su calificación y
 * provisión por la regla del SAA (D-015). Es la fila común de la G48 al corte, el CCPM al corte
 * y el paso ⑧ (provisión de cartera) del cierre: los tres salen de este mismo cálculo.
 */
public class OperacionCarteraCorte {

    private Long idPrestamo;
    private Long idProducto;
    private Long idTipoPrestamo;
    private Long idEntidad;
    private LocalDate corte;
    private double porVencer;
    private double vencido;
    private long diasMora;
    private double interesVencido;
    private double mora;
    private double interesCuotasDelMes;
    private Long idCuotaAncla;
    private double cuentaIndividual;
    private double valorSujetoProvision;
    private String calificacion;
    private double porcentajeProvision;
    private double provisionRequerida;
    private double provisionConstituida;

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }
    public Long getIdProducto() { return idProducto; }
    public void setIdProducto(Long idProducto) { this.idProducto = idProducto; }
    public Long getIdTipoPrestamo() { return idTipoPrestamo; }
    public void setIdTipoPrestamo(Long idTipoPrestamo) { this.idTipoPrestamo = idTipoPrestamo; }
    public Long getIdEntidad() { return idEntidad; }
    public void setIdEntidad(Long idEntidad) { this.idEntidad = idEntidad; }
    public LocalDate getCorte() { return corte; }
    public void setCorte(LocalDate corte) { this.corte = corte; }
    public double getPorVencer() { return porVencer; }
    public void setPorVencer(double porVencer) { this.porVencer = porVencer; }
    public double getVencido() { return vencido; }
    public void setVencido(double vencido) { this.vencido = vencido; }
    public long getDiasMora() { return diasMora; }
    public void setDiasMora(long diasMora) { this.diasMora = diasMora; }
    public double getInteresVencido() { return interesVencido; }
    public void setInteresVencido(double interesVencido) { this.interesVencido = interesVencido; }
    public double getMora() { return mora; }
    public void setMora(double mora) { this.mora = mora; }
    /** D-039: capital de las cuotas que vencen en el mes del corte (G48 campo 20, "cuota del crédito"). */
    private double capitalCuotasDelMes;
    public double getCapitalCuotasDelMes() { return capitalCuotasDelMes; }
    public void setCapitalCuotasDelMes(double capitalCuotasDelMes) { this.capitalCuotasDelMes = capitalCuotasDelMes; }
    public double getInteresCuotasDelMes() { return interesCuotasDelMes; }
    public void setInteresCuotasDelMes(double interesCuotasDelMes) { this.interesCuotasDelMes = interesCuotasDelMes; }
    public Long getIdCuotaAncla() { return idCuotaAncla; }
    public void setIdCuotaAncla(Long idCuotaAncla) { this.idCuotaAncla = idCuotaAncla; }
    public double getCuentaIndividual() { return cuentaIndividual; }
    public void setCuentaIndividual(double cuentaIndividual) { this.cuentaIndividual = cuentaIndividual; }
    public double getValorSujetoProvision() { return valorSujetoProvision; }
    public void setValorSujetoProvision(double valorSujetoProvision) { this.valorSujetoProvision = valorSujetoProvision; }
    public String getCalificacion() { return calificacion; }
    public void setCalificacion(String calificacion) { this.calificacion = calificacion; }
    public double getPorcentajeProvision() { return porcentajeProvision; }
    public void setPorcentajeProvision(double porcentajeProvision) { this.porcentajeProvision = porcentajeProvision; }
    public double getProvisionRequerida() { return provisionRequerida; }
    public void setProvisionRequerida(double provisionRequerida) { this.provisionRequerida = provisionRequerida; }
    public double getProvisionConstituida() { return provisionConstituida; }
    public void setProvisionConstituida(double provisionConstituida) { this.provisionConstituida = provisionConstituida; }

    /** Cartera bruta (por vencer + vencido). */
    public double getSaldo() { return porVencer + vencido; }

    /**
     * Interés ordinario que reporta la G48 al corte: el pendiente de las cuotas vencidas si
     * hay; si la operación está al día, el interés de la cuota del mes (convención del
     * generador G48: grupo 2 / grupo 1).
     */
    public double getInteresOrdinarioReporte() {
        return vencido > 0.004 ? interesVencido : interesCuotasDelMes;
    }
}

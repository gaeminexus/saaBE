package com.saa.ejb.crd.service.dto;

/**
 * Un renglón de {@link ResultadoDiferenciaDistribucionBanda} — API-AUDITORIA-BANDAS.md §4
 * ({@code GET /rest/dsbn/diferencia}): "¿de quién es la diferencia?".
 */
public class DiferenciaParticipeDistribucionBanda {

    private Long codigoPetro;
    private String cedula;
    private String participe;
    private double descontado;
    private double aplicadoPrestamos;
    private double aplicadoAportes;
    private double aplicadoTotal;
    private double diferencia;
    private double aplicadoManual;
    private double aplicadoAutomatico;

    public DiferenciaParticipeDistribucionBanda() {
    }

    public Long getCodigoPetro() {
        return codigoPetro;
    }

    public void setCodigoPetro(Long codigoPetro) {
        this.codigoPetro = codigoPetro;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getParticipe() {
        return participe;
    }

    public void setParticipe(String participe) {
        this.participe = participe;
    }

    public double getDescontado() {
        return descontado;
    }

    public void setDescontado(double descontado) {
        this.descontado = descontado;
    }

    public double getAplicadoPrestamos() {
        return aplicadoPrestamos;
    }

    public void setAplicadoPrestamos(double aplicadoPrestamos) {
        this.aplicadoPrestamos = aplicadoPrestamos;
    }

    public double getAplicadoAportes() {
        return aplicadoAportes;
    }

    public void setAplicadoAportes(double aplicadoAportes) {
        this.aplicadoAportes = aplicadoAportes;
    }

    public double getAplicadoTotal() {
        return aplicadoTotal;
    }

    public void setAplicadoTotal(double aplicadoTotal) {
        this.aplicadoTotal = aplicadoTotal;
    }

    public double getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(double diferencia) {
        this.diferencia = diferencia;
    }

    public double getAplicadoManual() {
        return aplicadoManual;
    }

    public void setAplicadoManual(double aplicadoManual) {
        this.aplicadoManual = aplicadoManual;
    }

    public double getAplicadoAutomatico() {
        return aplicadoAutomatico;
    }

    public void setAplicadoAutomatico(double aplicadoAutomatico) {
        this.aplicadoAutomatico = aplicadoAutomatico;
    }
}

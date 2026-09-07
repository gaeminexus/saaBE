package com.saa.ws.movil.dto;

import java.util.ArrayList;
import java.util.List;

/** Espejo movil de {@code ResultadoSimulacionCreditoNuevo} para {@code POST /movil/simulador/simular}. */
public class SimulacionCreditoMovilDTO {

    private List<CuotaProyectadaMovilDTO> tablaProyectada = new ArrayList<>();
    private Double totalCapital;
    private Double totalInteres;
    private Double totalDesgravamen;
    private Double totalSeguro;
    private Double totalAPagar;
    private Double valorCuota;

    public List<CuotaProyectadaMovilDTO> getTablaProyectada() {
        return tablaProyectada;
    }

    public void setTablaProyectada(List<CuotaProyectadaMovilDTO> tablaProyectada) {
        this.tablaProyectada = tablaProyectada;
    }

    public Double getTotalCapital() {
        return totalCapital;
    }

    public void setTotalCapital(Double totalCapital) {
        this.totalCapital = totalCapital;
    }

    public Double getTotalInteres() {
        return totalInteres;
    }

    public void setTotalInteres(Double totalInteres) {
        this.totalInteres = totalInteres;
    }

    public Double getTotalDesgravamen() {
        return totalDesgravamen;
    }

    public void setTotalDesgravamen(Double totalDesgravamen) {
        this.totalDesgravamen = totalDesgravamen;
    }

    public Double getTotalSeguro() {
        return totalSeguro;
    }

    public void setTotalSeguro(Double totalSeguro) {
        this.totalSeguro = totalSeguro;
    }

    public Double getTotalAPagar() {
        return totalAPagar;
    }

    public void setTotalAPagar(Double totalAPagar) {
        this.totalAPagar = totalAPagar;
    }

    public Double getValorCuota() {
        return valorCuota;
    }

    public void setValorCuota(Double valorCuota) {
        this.valorCuota = valorCuota;
    }
}

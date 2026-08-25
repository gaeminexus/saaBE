package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/** Resultado de {@code POST /rest/prst/simularCreditoNuevo}. Nada de esto se persiste. */
public class ResultadoSimulacionCreditoNuevo {

    private List<CuotaProyectada> tablaProyectada = new ArrayList<>();

    private Double totalCapital;
    private Double totalInteres;
    private Double totalDesgravamen;
    private Double totalSeguro;

    /** Suma de CuotaProyectada.total de toda la tabla (incluida la cuota 0 si aplica). */
    private Double totalAPagar;

    /** Cuota de una cuota regular (numeroCuota > 0); en la alemana es la primera y la más alta. */
    private Double valorCuota;

    public ResultadoSimulacionCreditoNuevo() {
    }

    public List<CuotaProyectada> getTablaProyectada() {
        return tablaProyectada;
    }

    public void setTablaProyectada(List<CuotaProyectada> tablaProyectada) {
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

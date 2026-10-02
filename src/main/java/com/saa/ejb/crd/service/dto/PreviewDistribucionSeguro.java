package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/** Resultado de {@code GET /posg/{id}/distribucion/preview} (contrato §5). */
public class PreviewDistribucionSeguro {

    private double valorTotal;
    private double sumaPrestamos;
    private double sumaCuotas;
    private boolean cuadra;
    private List<PrestamoSinCuotasEnVigencia> prestamosSinCuotasEnVigencia = new ArrayList<>();
    private List<PrestamoDistribucionPreview> prestamos = new ArrayList<>();

    public double getValorTotal() { return valorTotal; }
    public void setValorTotal(double valorTotal) { this.valorTotal = valorTotal; }

    public double getSumaPrestamos() { return sumaPrestamos; }
    public void setSumaPrestamos(double sumaPrestamos) { this.sumaPrestamos = sumaPrestamos; }

    public double getSumaCuotas() { return sumaCuotas; }
    public void setSumaCuotas(double sumaCuotas) { this.sumaCuotas = sumaCuotas; }

    public boolean isCuadra() { return cuadra; }
    public void setCuadra(boolean cuadra) { this.cuadra = cuadra; }

    public List<PrestamoSinCuotasEnVigencia> getPrestamosSinCuotasEnVigencia() { return prestamosSinCuotasEnVigencia; }
    public void setPrestamosSinCuotasEnVigencia(List<PrestamoSinCuotasEnVigencia> prestamosSinCuotasEnVigencia) {
        this.prestamosSinCuotasEnVigencia = prestamosSinCuotasEnVigencia;
    }

    public List<PrestamoDistribucionPreview> getPrestamos() { return prestamos; }
    public void setPrestamos(List<PrestamoDistribucionPreview> prestamos) { this.prestamos = prestamos; }
}

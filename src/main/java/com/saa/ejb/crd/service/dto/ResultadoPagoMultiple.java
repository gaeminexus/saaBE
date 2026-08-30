package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de {@code pagarMultiplesCuotas}: el desglose de cada préstamo pagado, en el mismo
 * orden que llegaron en la solicitud, más el total y el partícipe común — insumos para que el
 * consumidor arme la confirmación o el comprobante sin tener que volver a sumar nada.
 */
public class ResultadoPagoMultiple {

    private List<ResultadoAplicacionPago> resultados = new ArrayList<>();
    private double valorTotalAplicado;
    private Long idEntidad;
    private String nombreEntidad;

    public ResultadoPagoMultiple() {
    }

    public List<ResultadoAplicacionPago> getResultados() {
        return resultados;
    }

    public void setResultados(List<ResultadoAplicacionPago> resultados) {
        this.resultados = resultados;
    }

    public double getValorTotalAplicado() {
        return valorTotalAplicado;
    }

    public void setValorTotalAplicado(double valorTotalAplicado) {
        this.valorTotalAplicado = valorTotalAplicado;
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getNombreEntidad() {
        return nombreEntidad;
    }

    public void setNombreEntidad(String nombreEntidad) {
        this.nombreEntidad = nombreEntidad;
    }
}

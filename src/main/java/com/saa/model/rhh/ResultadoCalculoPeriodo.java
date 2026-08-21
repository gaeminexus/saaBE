package com.saa.model.rhh;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado del calculo de un periodo completo de nomina.
 *
 * <p>POJO de transporte, sin @Entity. Sigue el precedente de
 * {@code com.saa.model.cnt.RespuestaBalance}. El frontend lo espeja como interface
 * en {@code modules/rrh/model/}.</p>
 */
@SuppressWarnings("serial")
public class ResultadoCalculoPeriodo implements Serializable {

    private Long idPeriodo;

    private Integer empleadosProcesados;

    private Integer empleadosConError;

    private Double totalIngresos;

    private Double totalDescuentos;

    private Double totalNeto;

    private Double totalPatronal;

    private List<String> errores = new ArrayList<>();

    public Long getIdPeriodo() {
        return idPeriodo;
    }

    public void setIdPeriodo(Long idPeriodo) {
        this.idPeriodo = idPeriodo;
    }

    public Integer getEmpleadosProcesados() {
        return empleadosProcesados;
    }

    public void setEmpleadosProcesados(Integer empleadosProcesados) {
        this.empleadosProcesados = empleadosProcesados;
    }

    public Integer getEmpleadosConError() {
        return empleadosConError;
    }

    public void setEmpleadosConError(Integer empleadosConError) {
        this.empleadosConError = empleadosConError;
    }

    public Double getTotalIngresos() {
        return totalIngresos;
    }

    public void setTotalIngresos(Double totalIngresos) {
        this.totalIngresos = totalIngresos;
    }

    public Double getTotalDescuentos() {
        return totalDescuentos;
    }

    public void setTotalDescuentos(Double totalDescuentos) {
        this.totalDescuentos = totalDescuentos;
    }

    public Double getTotalNeto() {
        return totalNeto;
    }

    public void setTotalNeto(Double totalNeto) {
        this.totalNeto = totalNeto;
    }

    public Double getTotalPatronal() {
        return totalPatronal;
    }

    public void setTotalPatronal(Double totalPatronal) {
        this.totalPatronal = totalPatronal;
    }

    public List<String> getErrores() {
        return errores;
    }

    public void setErrores(List<String> errores) {
        this.errores = errores;
    }
}

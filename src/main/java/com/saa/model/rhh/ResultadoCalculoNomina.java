package com.saa.model.rhh;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado del calculo de la nomina de un empleado en un periodo.
 *
 * <p>POJO de transporte, sin @Entity. Sigue el precedente de
 * {@code com.saa.model.cnt.RespuestaBalance}. El frontend lo espeja como interface
 * en {@code modules/rrh/model/}.</p>
 */
@SuppressWarnings("serial")
public class ResultadoCalculoNomina implements Serializable {

    private Long idEmpleado;

    private String nombreEmpleado;

    private Double diasTrabajados;

    private List<RenglonCalculado> renglones = new ArrayList<>();

    private Double totalIngresos;

    private Double totalDescuentos;

    private Double neto;

    private List<String> advertencias = new ArrayList<>();

    public Long getIdEmpleado() {
        return idEmpleado;
    }

    public void setIdEmpleado(Long idEmpleado) {
        this.idEmpleado = idEmpleado;
    }

    public String getNombreEmpleado() {
        return nombreEmpleado;
    }

    public void setNombreEmpleado(String nombreEmpleado) {
        this.nombreEmpleado = nombreEmpleado;
    }

    public Double getDiasTrabajados() {
        return diasTrabajados;
    }

    public void setDiasTrabajados(Double diasTrabajados) {
        this.diasTrabajados = diasTrabajados;
    }

    public List<RenglonCalculado> getRenglones() {
        return renglones;
    }

    public void setRenglones(List<RenglonCalculado> renglones) {
        this.renglones = renglones;
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

    public Double getNeto() {
        return neto;
    }

    public void setNeto(Double neto) {
        this.neto = neto;
    }

    public List<String> getAdvertencias() {
        return advertencias;
    }

    public void setAdvertencias(List<String> advertencias) {
        this.advertencias = advertencias;
    }
}

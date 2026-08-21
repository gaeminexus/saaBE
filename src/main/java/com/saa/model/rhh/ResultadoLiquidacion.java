package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado del calculo de una liquidacion de haberes.
 *
 * <p>POJO de transporte, sin @Entity. Sigue el precedente de
 * {@code com.saa.model.cnt.RespuestaBalance}. El frontend lo espeja como interface
 * en {@code modules/rrh/model/}.</p>
 */
@SuppressWarnings("serial")
public class ResultadoLiquidacion implements Serializable {

    private Long idEmpleado;

    private LocalDate fechaSalida;

    private String causal;

    private Double aniosServicio;

    private List<RenglonCalculado> rubros = new ArrayList<>();

    private Double totalIngresos;

    private Double totalDescuentos;

    private Double neto;

    public Long getIdEmpleado() {
        return idEmpleado;
    }

    public void setIdEmpleado(Long idEmpleado) {
        this.idEmpleado = idEmpleado;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDate fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public String getCausal() {
        return causal;
    }

    public void setCausal(String causal) {
        this.causal = causal;
    }

    public Double getAniosServicio() {
        return aniosServicio;
    }

    public void setAniosServicio(Double aniosServicio) {
        this.aniosServicio = aniosServicio;
    }

    public List<RenglonCalculado> getRubros() {
        return rubros;
    }

    public void setRubros(List<RenglonCalculado> rubros) {
        this.rubros = rubros;
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
}

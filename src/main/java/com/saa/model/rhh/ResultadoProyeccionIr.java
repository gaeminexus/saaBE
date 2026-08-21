package com.saa.model.rhh;

import java.io.Serializable;

/**
 * Resultado de la proyeccion anual del impuesto a la renta de un empleado.
 *
 * <p>POJO de transporte, sin @Entity. Sigue el precedente de
 * {@code com.saa.model.cnt.RespuestaBalance}. El frontend lo espeja como interface
 * en {@code modules/rrh/model/}.</p>
 */
@SuppressWarnings("serial")
public class ResultadoProyeccionIr implements Serializable {

    private Long idEmpleado;

    private Integer anio;

    private Double ingresosProyectados;

    private Double baseImponible;

    private Double impuestoCausado;

    private Double gastosDeclarados;

    private Double tope;

    private Double rebaja;

    private Double impuestoAPagar;

    private Integer mesesRestantes;

    private Double retencionMensual;

    public Long getIdEmpleado() {
        return idEmpleado;
    }

    public void setIdEmpleado(Long idEmpleado) {
        this.idEmpleado = idEmpleado;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Double getIngresosProyectados() {
        return ingresosProyectados;
    }

    public void setIngresosProyectados(Double ingresosProyectados) {
        this.ingresosProyectados = ingresosProyectados;
    }

    public Double getBaseImponible() {
        return baseImponible;
    }

    public void setBaseImponible(Double baseImponible) {
        this.baseImponible = baseImponible;
    }

    public Double getImpuestoCausado() {
        return impuestoCausado;
    }

    public void setImpuestoCausado(Double impuestoCausado) {
        this.impuestoCausado = impuestoCausado;
    }

    public Double getGastosDeclarados() {
        return gastosDeclarados;
    }

    public void setGastosDeclarados(Double gastosDeclarados) {
        this.gastosDeclarados = gastosDeclarados;
    }

    public Double getTope() {
        return tope;
    }

    public void setTope(Double tope) {
        this.tope = tope;
    }

    public Double getRebaja() {
        return rebaja;
    }

    public void setRebaja(Double rebaja) {
        this.rebaja = rebaja;
    }

    public Double getImpuestoAPagar() {
        return impuestoAPagar;
    }

    public void setImpuestoAPagar(Double impuestoAPagar) {
        this.impuestoAPagar = impuestoAPagar;
    }

    public Integer getMesesRestantes() {
        return mesesRestantes;
    }

    public void setMesesRestantes(Integer mesesRestantes) {
        this.mesesRestantes = mesesRestantes;
    }

    public Double getRetencionMensual() {
        return retencionMensual;
    }

    public void setRetencionMensual(Double retencionMensual) {
        this.retencionMensual = retencionMensual;
    }
}

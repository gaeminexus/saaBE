package com.saa.model.rhh;

import java.io.Serializable;

/**
 * Una fila de la planilla de control del IESS: un afiliado.
 *
 * <p>POJO de transporte, sin @Entity. Reproduce las columnas que imprime la planilla real
 * --<code>RT · CEDULA · NOMBRE · SUELDO · DIAS · OBS · VALOR · TIEMPO PARCIAL</code>-- para
 * poder enfrentarla con la que genere el portal <b>antes de pagar</b>.</p>
 */
@SuppressWarnings("serial")
public class LineaPlanillaControlIess implements Serializable {

    /** Codigo de relacion de trabajo del IESS, dos digitos. */
    private String relacionTrabajo;

    private String identificacion;

    private String nombre;

    /** Base imponible del mes: lo que el IESS llama SUELDO. */
    private Double sueldo;

    /** Dias declarados. */
    private Long dias;

    /** Aporte personal calculado. */
    private Double aportePersonal;

    /** Aporte patronal calculado. */
    private Double aportePatronal;

    /** Suma de personal y patronal: lo que la planilla llama VALOR. */
    private Double totalIess;

    /**
     * Seguro de salud de la jornada parcial, cuando corresponde.
     *
     * <p>No sale del rol de nadie: es una linea del comprobante que paga el patrono sobre
     * la diferencia entre el salario basico y el sueldo real del afiliado a tiempo parcial.
     * Va en la fila para poder cuadrarla persona a persona, y se suma aparte en el total.</p>
     */
    private Double seguroTiempoParcial;

    public String getRelacionTrabajo() {
        return relacionTrabajo;
    }

    public void setRelacionTrabajo(String relacionTrabajo) {
        this.relacionTrabajo = relacionTrabajo;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Double getSueldo() {
        return sueldo;
    }

    public void setSueldo(Double sueldo) {
        this.sueldo = sueldo;
    }

    public Long getDias() {
        return dias;
    }

    public void setDias(Long dias) {
        this.dias = dias;
    }

    public Double getAportePersonal() {
        return aportePersonal;
    }

    public void setAportePersonal(Double aportePersonal) {
        this.aportePersonal = aportePersonal;
    }

    public Double getAportePatronal() {
        return aportePatronal;
    }

    public void setAportePatronal(Double aportePatronal) {
        this.aportePatronal = aportePatronal;
    }

    public Double getTotalIess() {
        return totalIess;
    }

    public void setTotalIess(Double totalIess) {
        this.totalIess = totalIess;
    }

    public Double getSeguroTiempoParcial() {
        return seguroTiempoParcial;
    }

    public void setSeguroTiempoParcial(Double seguroTiempoParcial) {
        this.seguroTiempoParcial = seguroTiempoParcial;
    }

}

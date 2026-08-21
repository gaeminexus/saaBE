package com.saa.model.rhh;

import java.io.Serializable;

/**
 * Una linea del asiento contable de nomina, para previsualizacion antes de contabilizar.
 *
 * <p>POJO de transporte, sin @Entity. Sigue el precedente de
 * {@code com.saa.model.cnt.RespuestaBalance}. El frontend lo espeja como interface
 * en {@code modules/rrh/model/}.</p>
 */
@SuppressWarnings("serial")
public class LineaAsientoNomina implements Serializable {

    private String cuenta;

    private String nombreCuenta;

    private String descripcion;

    private Double debe;

    private Double haber;

    private Long codigoLinea;

    private String centroCosto;

    public String getCuenta() {
        return cuenta;
    }

    public void setCuenta(String cuenta) {
        this.cuenta = cuenta;
    }

    public String getNombreCuenta() {
        return nombreCuenta;
    }

    public void setNombreCuenta(String nombreCuenta) {
        this.nombreCuenta = nombreCuenta;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Double getDebe() {
        return debe;
    }

    public void setDebe(Double debe) {
        this.debe = debe;
    }

    public Double getHaber() {
        return haber;
    }

    public void setHaber(Double haber) {
        this.haber = haber;
    }

    public Long getCodigoLinea() {
        return codigoLinea;
    }

    public void setCodigoLinea(Long codigoLinea) {
        this.codigoLinea = codigoLinea;
    }

    public String getCentroCosto() {
        return centroCosto;
    }

    public void setCentroCosto(String centroCosto) {
        this.centroCosto = centroCosto;
    }
}

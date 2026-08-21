package com.saa.model.rhh;

import java.io.Serializable;

/**
 * Un renglon del rol de pago ya calculado, tal como lo devuelve el motor.
 *
 * <p>POJO de transporte, sin @Entity. Sigue el precedente de
 * {@code com.saa.model.cnt.RespuestaBalance}. El frontend lo espeja como interface
 * en {@code modules/rrh/model/}.</p>
 */
@SuppressWarnings("serial")
public class RenglonCalculado implements Serializable {

    private Long codigoConcepto;

    private String nombreConcepto;

    private Long tipoConcepto;

    private Double cantidad;

    private Double base;

    private Double porcentaje;

    private Double valor;

    private Integer orden;

    public Long getCodigoConcepto() {
        return codigoConcepto;
    }

    public void setCodigoConcepto(Long codigoConcepto) {
        this.codigoConcepto = codigoConcepto;
    }

    public String getNombreConcepto() {
        return nombreConcepto;
    }

    public void setNombreConcepto(String nombreConcepto) {
        this.nombreConcepto = nombreConcepto;
    }

    public Long getTipoConcepto() {
        return tipoConcepto;
    }

    public void setTipoConcepto(Long tipoConcepto) {
        this.tipoConcepto = tipoConcepto;
    }

    public Double getCantidad() {
        return cantidad;
    }

    public void setCantidad(Double cantidad) {
        this.cantidad = cantidad;
    }

    public Double getBase() {
        return base;
    }

    public void setBase(Double base) {
        this.base = base;
    }

    public Double getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(Double porcentaje) {
        this.porcentaje = porcentaje;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }
}

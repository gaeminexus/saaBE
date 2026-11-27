package com.saa.ejb.cnt.service.dto;

import java.io.Serializable;

/** C51: algo que bloquearía el cierre contable: un control del semáforo en ERROR, o una condición de las G40-G51. */
public class FallaCandado implements Serializable {

    private static final long serialVersionUID = 1L;

    /** {@code CONTROL} (semáforo /prdo/controlCierre) o {@code G} (estructuras G40-G51). */
    private String origen;
    private String codigo;
    private String descripcion;
    private Double valor;
    private String detalle;
    /** Las condiciones de las G (completa, sin errores, vigente) no se excusan. */
    private boolean excepcionable;
    /** Cubierta por una excepción con motivo. */
    private boolean cubierta;
    private String motivoExcepcion;

    public FallaCandado() {
    }

    public FallaCandado(String origen, String codigo, String descripcion, Double valor, String detalle, boolean excepcionable) {
        this.origen = origen;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.valor = valor;
        this.detalle = detalle;
        this.excepcionable = excepcionable;
    }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
    public boolean isExcepcionable() { return excepcionable; }
    public void setExcepcionable(boolean excepcionable) { this.excepcionable = excepcionable; }
    public boolean isCubierta() { return cubierta; }
    public void setCubierta(boolean cubierta) { this.cubierta = cubierta; }
    public String getMotivoExcepcion() { return motivoExcepcion; }
    public void setMotivoExcepcion(String motivoExcepcion) { this.motivoExcepcion = motivoExcepcion; }
}

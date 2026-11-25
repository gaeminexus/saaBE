package com.saa.ejb.cnt.service.dto;

import java.io.Serializable;

/**
 * 6.5 del cuadre (DS-21): resultado de {@code POST /prdo/desmayorizar}. Contrato:
 * docs/logica-negocio/cnt/API-DESMAYORIZAR-PERIODO.md.
 */
public class ResultadoDesmayorizacion implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idEmpresa;
    private Long anio;
    private Long mes;
    /** Estado del período antes (rubro 22: 1 activo, 2 mayorizado, 3 desmayorizado). */
    private Long estadoAnterior;
    /** Estado del período después. */
    private Long estadoNuevo;
    /** true si PRDOMYRZ no apuntaba a una mayorización existente y solo se limpió la referencia. */
    private boolean referenciaColgante;
    private String mensaje;

    public Long getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(Long idEmpresa) { this.idEmpresa = idEmpresa; }
    public Long getAnio() { return anio; }
    public void setAnio(Long anio) { this.anio = anio; }
    public Long getMes() { return mes; }
    public void setMes(Long mes) { this.mes = mes; }
    public Long getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(Long estadoAnterior) { this.estadoAnterior = estadoAnterior; }
    public Long getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(Long estadoNuevo) { this.estadoNuevo = estadoNuevo; }
    public boolean isReferenciaColgante() { return referenciaColgante; }
    public void setReferenciaColgante(boolean referenciaColgante) { this.referenciaColgante = referenciaColgante; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}

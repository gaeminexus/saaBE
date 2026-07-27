/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.model.tsr;

import java.io.Serializable;
import java.util.List;

/**
 * @author GaemiSoft
 * <p>DTO del cuerpo JSON para POST /cnct/conciliar. No es una entidad JPA.</p>
 */
public class SolicitudConciliarGrupo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idCuentaBancaria;
    private Long idPeriodo;
    private List<Long> idsDetalleExtracto;
    private List<Long> idsDetalleAsiento;
    private String usuario;

    public Long getIdCuentaBancaria() {
        return idCuentaBancaria;
    }

    public void setIdCuentaBancaria(Long idCuentaBancaria) {
        this.idCuentaBancaria = idCuentaBancaria;
    }

    public Long getIdPeriodo() {
        return idPeriodo;
    }

    public void setIdPeriodo(Long idPeriodo) {
        this.idPeriodo = idPeriodo;
    }

    public List<Long> getIdsDetalleExtracto() {
        return idsDetalleExtracto;
    }

    public void setIdsDetalleExtracto(List<Long> idsDetalleExtracto) {
        this.idsDetalleExtracto = idsDetalleExtracto;
    }

    public List<Long> getIdsDetalleAsiento() {
        return idsDetalleAsiento;
    }

    public void setIdsDetalleAsiento(List<Long> idsDetalleAsiento) {
        this.idsDetalleAsiento = idsDetalleAsiento;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
}

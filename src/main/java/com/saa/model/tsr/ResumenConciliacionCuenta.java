/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author GaemiSoft
 * <p>DTO de una fila del resumen por cuenta bancaria de la pantalla de
 * Conciliación Contable: para un período dado, ¿esta cuenta ya está
 * verificada, tiene diferencias, o todavía no se ha tocado? No es una
 * entidad JPA - se usa solo para la respuesta de /cnct/resumenPorPeriodo.</p>
 * <p>Es de solo lectura por diseño: NO crea la cabecera
 * {@link ConciliacionContable} para cuentas que el usuario nunca abrió -
 * eso solo sucede al entrar de verdad a conciliar una cuenta específica
 * (ver ConciliacionContableService.obtenerOCrear). Por eso
 * idConciliacionContable y estadoRevision pueden venir null: significa que
 * todavía no existe cabecera para esa cuenta/período.</p>
 */
public class ResumenConciliacionCuenta implements Serializable {

    private static final long serialVersionUID = 1L;

    private CuentaBancaria cuentaBancaria;
    private Long idConciliacionContable;
    private Long estadoRevision;
    private Long totalPendientesExtracto;
    private Long totalPendientesAsiento;
    private String usuarioVerifica;
    private LocalDateTime fechaVerificacion;

    public CuentaBancaria getCuentaBancaria() {
        return cuentaBancaria;
    }

    public void setCuentaBancaria(CuentaBancaria cuentaBancaria) {
        this.cuentaBancaria = cuentaBancaria;
    }

    public Long getIdConciliacionContable() {
        return idConciliacionContable;
    }

    public void setIdConciliacionContable(Long idConciliacionContable) {
        this.idConciliacionContable = idConciliacionContable;
    }

    public Long getEstadoRevision() {
        return estadoRevision;
    }

    public void setEstadoRevision(Long estadoRevision) {
        this.estadoRevision = estadoRevision;
    }

    public Long getTotalPendientesExtracto() {
        return totalPendientesExtracto;
    }

    public void setTotalPendientesExtracto(Long totalPendientesExtracto) {
        this.totalPendientesExtracto = totalPendientesExtracto;
    }

    public Long getTotalPendientesAsiento() {
        return totalPendientesAsiento;
    }

    public void setTotalPendientesAsiento(Long totalPendientesAsiento) {
        this.totalPendientesAsiento = totalPendientesAsiento;
    }

    public String getUsuarioVerifica() {
        return usuarioVerifica;
    }

    public void setUsuarioVerifica(String usuarioVerifica) {
        this.usuarioVerifica = usuarioVerifica;
    }

    public LocalDateTime getFechaVerificacion() {
        return fechaVerificacion;
    }

    public void setFechaVerificacion(LocalDateTime fechaVerificacion) {
        this.fechaVerificacion = fechaVerificacion;
    }
}

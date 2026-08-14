package com.saa.ejb.crd.service.dto;

import java.time.LocalDateTime;

/**
 * Contexto de una operación de pago: el "quién, cuándo y por qué" que el motor
 * estampa en cada PagoPrestamo que crea.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §5.1.
 */
public class ContextoPago {

    /** "PAGO_MANUAL" | "PAGO_APORTES" | "PRECANCELACION" | "ABONO_CAPITAL" */
    private String tipoPago;

    /** Usuario que ejecuta la operación (obligatorio) */
    private String usuario;

    /** Observación registrada por el usuario */
    private String observacion;

    /** Fecha de negocio del pago; si es null el motor usa LocalDateTime.now() */
    private LocalDateTime fechaPago;

    /** EVPRCDGO de la operación (siempre presente en los procesos nuevos) */
    private Long idEvento;

    public ContextoPago() {
    }

    public String getTipoPago() {
        return tipoPago;
    }

    public void setTipoPago(String tipoPago) {
        this.tipoPago = tipoPago;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public Long getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(Long idEvento) {
        this.idEvento = idEvento;
    }
}

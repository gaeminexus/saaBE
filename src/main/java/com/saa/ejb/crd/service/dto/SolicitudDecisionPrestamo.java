package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/**
 * Body de POST /rest/prst/aprobar/{id} y POST /rest/prst/rechazar/{id}.
 *
 * Ver PLAN-CICLO-OTORGAMIENTO.md §4.
 */
public class SolicitudDecisionPrestamo {

    private String usuario;
    private String observacion;

    /**
     * Fecha contable del desembolso (C6, cnt/FECHA-CONTABLE-REPROCESO.md), {@code yyyy-MM-dd},
     * OPCIONAL: fecha programada de la orden de pago del desembolso en CXP. Si no viene, hoy
     * (comportamiento de siempre). No puede ser futura. El asiento de entrega NO la usa: va
     * con {@code PRSTFCIN} (fecha de inicio del préstamo), que ya es una fecha de negocio.
     * Sin uso en {@code rechazar}.
     */
    private LocalDate fechaContable;

    /**
     * Empresa contable del asiento de entrega. Obligatorio en {@code aprobar} desde
     * PLAN-DESEMBOLSO-PRESTAMO.md §5; sin uso en {@code rechazar}.
     */
    private Long idEmpresa;

    /**
     * Quien registra la orden de pago del desembolso en CXP. Obligatorio en {@code aprobar}
     * desde PLAN-DESEMBOLSO-PRESTAMO.md §5; sin uso en {@code rechazar}.
     */
    private Long idUsuario;

    public SolicitudDecisionPrestamo() {
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

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDate getFechaContable() {
        return fechaContable;
    }

    public void setFechaContable(LocalDate fechaContable) {
        this.fechaContable = fechaContable;
    }
}

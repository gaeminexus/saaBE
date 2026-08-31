package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/**
 * Body de POST /rest/aprt/procesarJubilacion.
 *
 * Solo el traslado (paso 3) + el cambio de estado (paso 5) del flujo de jubilación — ver
 * LEVANTAMIENTO-TRES-FRENTES-2026-08-30.md §4.b. El cruce contra préstamos y la devolución en
 * efectivo (paso 2, opcionales) son llamadas PREVIAS y separadas a
 * {@code POST /rest/prst/pagarConAportes} y {@code POST /rest/dvap/registrar} — este endpoint
 * no las orquesta.
 */
public class SolicitudProcesarJubilacion {

    /** Código de la entidad (partícipe) a jubilar — ENTD.ENTDCDGO. Obligatorio. */
    private Long idEntidad;

    private String usuario;

    /**
     * Fecha de negocio del traslado. Si es null se asume hoy. No puede ser futura.
     *
     * Viaja como {@code yyyy-MM-dd}: es un {@code LocalDate}. Nunca un Date de JavaScript ni
     * nada terminado en Z — Jackson descarta el offset en vez de convertirlo.
     */
    private LocalDate fecha;

    /**
     * Empresa contable (SCP.PJRQ) sobre la que se genera el asiento de reclasificación.
     * Obligatorio. Lo manda el frontend desde la empresa de la sesión — mismo criterio que los
     * 7 DTOs del motor de pagos desde la Fase 0 (API-EMPRESA-CONTABLE-CRD.md).
     */
    private Long idEmpresa;

    public SolicitudProcesarJubilacion() {
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }
}

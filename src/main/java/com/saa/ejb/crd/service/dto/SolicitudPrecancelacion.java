package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Body de POST /rest/prst/precancelar. Admite pago en efectivo, con aportes o mixto:
 * valorEnviado = valorEfectivo + suma del desglose de aportes.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §5.1 y §7.5.
 */
public class SolicitudPrecancelacion {

    private Long idPrestamo;

    /** Componente en efectivo (puede ser 0 si se precancela solo con aportes) */
    private Double valorEfectivo;

    /** Componente en aportes (puede venir vacío si se precancela solo con efectivo) */
    private List<DesgloseAporte> aportes = new ArrayList<>();

    private String usuario;
    private String observacion;

    /** Fecha de corte de la precancelación; si es null se asume hoy */
    private LocalDate fecha;

    public SolicitudPrecancelacion() {
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Double getValorEfectivo() {
        return valorEfectivo;
    }

    public void setValorEfectivo(Double valorEfectivo) {
        this.valorEfectivo = valorEfectivo;
    }

    public List<DesgloseAporte> getAportes() {
        return aportes;
    }

    public void setAportes(List<DesgloseAporte> aportes) {
        this.aportes = aportes;
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

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }
}

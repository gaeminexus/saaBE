package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Respuesta de {@code ConfiguracionCalificacionRiesgoService} — configuración con su escala ya
 * resuelta. Mismo espíritu que {@code ConfiguracionBandaDetalle}: DTO propio, no la entidad cruda,
 * para no serializar el grafo de FKs.
 */
public class DetalleConfiguracionCalificacionRiesgo {

    private Long idConfiguracion;
    private Long idProducto;
    private String nombreProducto;

    /** {@code null} = aplica a cualquier empresa. */
    private Long idEmpresa;

    private String nombre;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;

    /** {@code true} solo si la vigencia todavía no empezó (editable en el lugar). */
    private Boolean editable;

    private Long estado;
    private List<DetalleEscalaCalificacionRiesgo> escalas;

    public DetalleConfiguracionCalificacionRiesgo() {
    }

    public Long getIdConfiguracion() {
        return idConfiguracion;
    }

    public void setIdConfiguracion(Long idConfiguracion) {
        this.idConfiguracion = idConfiguracion;
    }

    public Long getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Long idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public Boolean getEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public List<DetalleEscalaCalificacionRiesgo> getEscalas() {
        return escalas;
    }

    public void setEscalas(List<DetalleEscalaCalificacionRiesgo> escalas) {
        this.escalas = escalas;
    }
}

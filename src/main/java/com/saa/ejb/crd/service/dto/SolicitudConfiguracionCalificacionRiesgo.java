package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Solicitud de {@code ConfiguracionCalificacionRiesgoService#guardarConfiguracion} — P22,
 * PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md. Mismo patrón que {@code SolicitudConfiguracionBanda}
 * (bandas), con las diferencias deliberadas de este dominio:
 *
 * <ul>
 *   <li>Sin {@code tipoCartera}: la calificación de riesgo no distingue por vencer/vencido.</li>
 *   <li>{@code idEmpresa} es OPCIONAL — {@code null} = aplica a cualquier empresa, que es como
 *       está cargada hoy toda la parametrización (sql/177). Distinto de bandas, que exige empresa
 *       por FK real; acá es a propósito, ver el javadoc de {@code ConfiguracionCalificacionRiesgo}.</li>
 *   <li>Sin {@code ip}: {@code CRD.CFCR}/{@code CRD.ESCR} no tienen columnas de auditoría de
 *       modificación — decisión del árbitro 2026-09-07, la trazabilidad la da la cadena de
 *       vigencias (cada una nace con su propio {@code fechaRegistro}/{@code usuarioRegistro}),
 *       no hace falta duplicarla.</li>
 * </ul>
 */
public class SolicitudConfiguracionCalificacionRiesgo {

    /** {@code null} = alta; con valor = edición en el lugar (solo si la vigencia no empezó). */
    private Long idConfiguracion;

    private Long idProducto;

    /** {@code null} = aplica a cualquier empresa. */
    private Long idEmpresa;

    /** Etiqueta informativa, p.ej. "HIPOTECARIA" / "GENERAL". Opcional. */
    private String nombre;

    private LocalDate fechaDesde;

    /** {@code null} = vigencia abierta. */
    private LocalDate fechaHasta;

    private String usuario;

    private List<SolicitudEscala> escalas;

    public SolicitudConfiguracionCalificacionRiesgo() {
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

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public List<SolicitudEscala> getEscalas() {
        return escalas;
    }

    public void setEscalas(List<SolicitudEscala> escalas) {
        this.escalas = escalas;
    }
}

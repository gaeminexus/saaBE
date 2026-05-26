package com.saa.model.rpr;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Entidad que representa la tabla RPR.EJRC - Cabecera de Ejecución de Reportes.
 * Registra cada ejecución mensual (inicial o corrección) de los reportes G40-G51.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "EJRC", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "EjecucionReporteAll", query = "select e from EjecucionReporte e"),
    @NamedQuery(name = "EjecucionReporteId",  query = "select e from EjecucionReporte e where e.codigo = :id")
})
public class EjecucionReporte implements Serializable {

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "EJRCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Mes de presentación de la información (1-12). */
    @Basic
    @Column(name = "EJRCMESS")
    private Long mes;

    /** Año de presentación de la información. */
    @Basic
    @Column(name = "EJRCANOO")
    private Long anio;

    /** Usuario que generó la ejecución. */
    @Basic
    @Column(name = "EJRCUSRO", length = 50)
    private String usuario;

    /** Fecha en que se generó la ejecución. */
    @Basic
    @Column(name = "EJRCFCGN")
    private LocalDate fechaGeneracion;

    /**
     * Tipo de ejecución.
     * 1 = Inicial, 2 = Corrección.
     */
    @Basic
    @Column(name = "EJRCTPEJ")
    private Long tipoEjecucion;

    /**
     * Estado general de la ejecución.
     * 1 = En proceso, 2 = Con novedades, 3 = Completo.
     */
    @Basic
    @Column(name = "EJRCESTD")
    private Long estado;

    /** Observaciones generales de la ejecución. */
    @Basic
    @Column(name = "EJRCOBSR", length = 500)
    private String observaciones;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Long getMes() { return mes; }
    public void setMes(Long mes) { this.mes = mes; }

    public Long getAnio() { return anio; }
    public void setAnio(Long anio) { this.anio = anio; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public LocalDate getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDate fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }

    public Long getTipoEjecucion() { return tipoEjecucion; }
    public void setTipoEjecucion(Long tipoEjecucion) { this.tipoEjecucion = tipoEjecucion; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}

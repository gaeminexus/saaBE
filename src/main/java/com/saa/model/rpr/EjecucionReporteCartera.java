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
 * Entidad que representa la tabla RPR.EJCC - Control de Ejecución de Reportes de Cartera.
 * Registra cada ejecución mensual de los reportes CPRM, CJBM y CCPM.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "EJCC", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "EjecucionReporteCarteraAll", query = "select e from EjecucionReporteCartera e"),
    @NamedQuery(name = "EjecucionReporteCarteraId",  query = "select e from EjecucionReporteCartera e where e.codigo = :id")
})
public class EjecucionReporteCartera implements Serializable {

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "EJCCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Mes de presentación de la información (1-12). */
    @Basic
    @Column(name = "EJCCMESS")
    private Long mes;

    /** Año de presentación de la información. */
    @Basic
    @Column(name = "EJCCANOO")
    private Long anio;

    /** Usuario que generó la ejecución. */
    @Basic
    @Column(name = "EJCCUSRO", length = 50)
    private String usuario;

    /** Fecha en que se generó la ejecución. */
    @Basic
    @Column(name = "EJCCFCGN")
    private LocalDate fechaGeneracion;

    /** Observaciones generales de la ejecución. */
    @Basic
    @Column(name = "EJCCOBSR", length = 500)
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

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}

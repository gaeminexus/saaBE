package com.saa.model.rpr;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Entidad que representa la tabla RPR.CG41 - G41 (Partícipes activos y voluntarios).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CG41", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "ParticipeActivoG41All", query = "select e from ParticipeActivoG41 e"),
    @NamedQuery(name = "ParticipeActivoG41Id",  query = "select e from ParticipeActivoG41 e where e.codigo = :id")
})
public class ParticipeActivoG41 implements Serializable {

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "CG41CDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Tipo de identificación del partícipe. */
    @Basic
    @Column(name = "CG41TIDP", length = 50)
    private String tipoIdentificacion;

    /** Identificación del partícipe. */
    @Basic
    @Column(name = "CG41IDPR", length = 50)
    private String identificacion;

    /** Genero del partícipe. */
    @Basic
    @Column(name = "CG41GNPR", length = 50)
    private String genero;

    /** Estado civil del partícipe. */
    @Basic
    @Column(name = "CG41ECDP", length = 50)
    private String estadoCivil;

    /** Fecha de nacimiento del partícipe. */
    @Basic
    @Column(name = "CG41FNDP")
    private LocalDate fechaNacimiento;

    /** Fecha de ingreso del partícipe. */
    @Basic
    @Column(name = "CG41FIDP")
    private LocalDate fechaIngreso;

    /** Estado del partícipe. */
    @Basic
    @Column(name = "CG41ESPR", length = 50)
    private String estadoParticipe;

    /** Tipo de sistema. */
    @Basic
    @Column(name = "CG41TPSS", length = 50)
    private String tipoSistema;

    /** Base de cálculo para la aportación. */
    @Basic
    @Column(name = "CG41BCPA", length = 10)
    private String baseCalculoAportacion;

    /** Tipo de relación laboral del partícipe. */
    @Basic
    @Column(name = "CG41TRLP", length = 100)
    private String tipoRelacionLaboral;

    /** Estado del registro. */
    @Basic
    @Column(name = "CG41ESRG", length = 50)
    private String estadoRegistro;

    /** Fecha de actualización del estado. */
    @Basic
    @Column(name = "CG41FADE")
    private LocalDate fechaActualizacionEstado;

    /** FK al detalle de ejecución (RPR.EJRD) al que pertenece este registro. */
    @ManyToOne
    @JoinColumn(name = "CG41EJRD", referencedColumnName = "EJRDCDGO")
    private DetalleEjecucionReporte detalleEjecucion;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String tipoIdentificacion) { this.tipoIdentificacion = tipoIdentificacion; }

    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public String getEstadoCivil() { return estadoCivil; }
    public void setEstadoCivil(String estadoCivil) { this.estadoCivil = estadoCivil; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(LocalDate fechaIngreso) { this.fechaIngreso = fechaIngreso; }

    public String getEstadoParticipe() { return estadoParticipe; }
    public void setEstadoParticipe(String estadoParticipe) { this.estadoParticipe = estadoParticipe; }

    public String getTipoSistema() { return tipoSistema; }
    public void setTipoSistema(String tipoSistema) { this.tipoSistema = tipoSistema; }

    public String getBaseCalculoAportacion() { return baseCalculoAportacion; }
    public void setBaseCalculoAportacion(String baseCalculoAportacion) { this.baseCalculoAportacion = baseCalculoAportacion; }

    public String getTipoRelacionLaboral() { return tipoRelacionLaboral; }
    public void setTipoRelacionLaboral(String tipoRelacionLaboral) { this.tipoRelacionLaboral = tipoRelacionLaboral; }

    public String getEstadoRegistro() { return estadoRegistro; }
    public void setEstadoRegistro(String estadoRegistro) { this.estadoRegistro = estadoRegistro; }

    public LocalDate getFechaActualizacionEstado() { return fechaActualizacionEstado; }
    public void setFechaActualizacionEstado(LocalDate fechaActualizacionEstado) { this.fechaActualizacionEstado = fechaActualizacionEstado; }

    public DetalleEjecucionReporte getDetalleEjecucion() { return detalleEjecucion; }
    public void setDetalleEjecucion(DetalleEjecucionReporte detalleEjecucion) { this.detalleEjecucion = detalleEjecucion; }
}
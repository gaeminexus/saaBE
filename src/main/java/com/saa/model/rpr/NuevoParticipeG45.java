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
 * Entidad que representa la tabla RPR.CG45 - G45 (Nuevos partícipes).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CG45", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "NuevoParticipeG45All", query = "select e from NuevoParticipeG45 e"),
    @NamedQuery(name = "NuevoParticipeG45Id",  query = "select e from NuevoParticipeG45 e where e.codigo = :id")
})
public class NuevoParticipeG45 implements Serializable {

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "CG45CDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Tipo de identificación del sujeto. */
    @Basic
    @Column(name = "CG45TIDS", length = 50)
    private String tipoIdentificacion;

    /** Identificación del sujeto. */
    @Basic
    @Column(name = "CG45IDSJ", length = 50)
    private String identificacion;

    /** Tipo de partícipe. */
    @Basic
    @Column(name = "CG45TPPR", length = 50)
    private String tipoParticipe;

    /** Actividad económica del sujeto. */
    @Basic
    @Column(name = "CG45AEDS", length = 100)
    private String actividadEconomica;

    /** Patrimonio del sujeto. */
    @Basic
    @Column(name = "CG45PTSJ")
    private Double patrimonio;

    /** Provincia. */
    @Basic
    @Column(name = "CG45PRVN", length = 100)
    private String provincia;

    /** Cantón. */
    @Basic
    @Column(name = "CG45CNTN", length = 100)
    private String canton;

    /** Parroquia. */
    @Basic
    @Column(name = "CG45PRRQ", length = 100)
    private String parroquia;

    /** Género. */
    @Basic
    @Column(name = "CG45GNRO", length = 50)
    private String genero;

    /** Estado civil. */
    @Basic
    @Column(name = "CG45ESCV", length = 50)
    private String estadoCivil;

    /** Fecha de nacimiento. */
    @Basic
    @Column(name = "CG45FDNC")
    private LocalDate fechaNacimiento;

    /** Profesión. */
    @Basic
    @Column(name = "CG45PRFS", length = 100)
    private String profesion;

    /** Cargas familiares. */
    @Basic
    @Column(name = "CG45CRFM")
    private Long cargasFamiliares;

    /** Orígen de ingresos. */
    @Basic
    @Column(name = "CG45ODIN", length = 100)
    private String origenIngresos;

    /** FK al detalle de ejecución (RPR.EJRD) al que pertenece este registro. */
    @ManyToOne
    @JoinColumn(name = "CG45EJRD", referencedColumnName = "EJRDCDGO")
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

    public String getTipoParticipe() { return tipoParticipe; }
    public void setTipoParticipe(String tipoParticipe) { this.tipoParticipe = tipoParticipe; }

    public String getActividadEconomica() { return actividadEconomica; }
    public void setActividadEconomica(String actividadEconomica) { this.actividadEconomica = actividadEconomica; }

    public Double getPatrimonio() { return patrimonio; }
    public void setPatrimonio(Double patrimonio) { this.patrimonio = patrimonio; }

    public String getProvincia() { return provincia; }
    public void setProvincia(String provincia) { this.provincia = provincia; }

    public String getCanton() { return canton; }
    public void setCanton(String canton) { this.canton = canton; }

    public String getParroquia() { return parroquia; }
    public void setParroquia(String parroquia) { this.parroquia = parroquia; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public String getEstadoCivil() { return estadoCivil; }
    public void setEstadoCivil(String estadoCivil) { this.estadoCivil = estadoCivil; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getProfesion() { return profesion; }
    public void setProfesion(String profesion) { this.profesion = profesion; }

    public Long getCargasFamiliares() { return cargasFamiliares; }
    public void setCargasFamiliares(Long cargasFamiliares) { this.cargasFamiliares = cargasFamiliares; }

    public String getOrigenIngresos() { return origenIngresos; }
    public void setOrigenIngresos(String origenIngresos) { this.origenIngresos = origenIngresos; }

    public DetalleEjecucionReporte getDetalleEjecucion() { return detalleEjecucion; }
    public void setDetalleEjecucion(DetalleEjecucionReporte detalleEjecucion) { this.detalleEjecucion = detalleEjecucion; }
}
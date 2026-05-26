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
 * Entidad que representa la tabla RPR.CG50 - G50 (Garantes y codeudores).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CG50", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "GaranteG50All", query = "select e from GaranteG50 e"),
    @NamedQuery(name = "GaranteG50Id",  query = "select e from GaranteG50 e where e.codigo = :id")
})
public class GaranteG50 implements Serializable {

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "CG50CDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Tipo de identificación del sujeto. */
    @Basic
    @Column(name = "CG50TIDS", length = 50)
    private String tipoIdentificacion;

    /** Identificación del sujeto. */
    @Basic
    @Column(name = "CG50IDSJ", length = 50)
    private String identificacion;

    /** Número de operación. */
    @Basic
    @Column(name = "CG50NMOP", length = 50)
    private String numeroOperacion;

    /** Tipo de identificación del garante. */
    @Basic
    @Column(name = "CG50TIDG", length = 50)
    private String tipoIdentificacionGarante;

    /** Identificación del garante. */
    @Basic
    @Column(name = "CG50IDGR", length = 50)
    private String identificacionGarante;

    /** Tipo de garante. */
    @Basic
    @Column(name = "CG50TPGR", length = 50)
    private String tipoGarante;

    /** Fecha de eliminación del garante. */
    @Basic
    @Column(name = "CG50FDEG")
    private LocalDate fechaEliminacion;

    /** Causa de eliminación del garante. */
    @Basic
    @Column(name = "CG50CDEG", length = 200)
    private String causaEliminacion;

    /** FK al detalle de ejecución (RPR.EJRD) al que pertenece este registro. */
    @ManyToOne
    @JoinColumn(name = "CG50EJRD", referencedColumnName = "EJRDCDGO")
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

    public String getNumeroOperacion() { return numeroOperacion; }
    public void setNumeroOperacion(String numeroOperacion) { this.numeroOperacion = numeroOperacion; }

    public String getTipoIdentificacionGarante() { return tipoIdentificacionGarante; }
    public void setTipoIdentificacionGarante(String tipoIdentificacionGarante) { this.tipoIdentificacionGarante = tipoIdentificacionGarante; }

    public String getIdentificacionGarante() { return identificacionGarante; }
    public void setIdentificacionGarante(String identificacionGarante) { this.identificacionGarante = identificacionGarante; }

    public String getTipoGarante() { return tipoGarante; }
    public void setTipoGarante(String tipoGarante) { this.tipoGarante = tipoGarante; }

    public LocalDate getFechaEliminacion() { return fechaEliminacion; }
    public void setFechaEliminacion(LocalDate fechaEliminacion) { this.fechaEliminacion = fechaEliminacion; }

    public String getCausaEliminacion() { return causaEliminacion; }
    public void setCausaEliminacion(String causaEliminacion) { this.causaEliminacion = causaEliminacion; }

    public DetalleEjecucionReporte getDetalleEjecucion() { return detalleEjecucion; }
    public void setDetalleEjecucion(DetalleEjecucionReporte detalleEjecucion) { this.detalleEjecucion = detalleEjecucion; }
}
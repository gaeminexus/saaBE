package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.scp.Usuario;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Entity AdendumNegociacion.
 * Registra los adendums o modificaciones acordadas sobre una negociacion con proveedor.
 * Un adendum puede incrementar o disminuir el valor total de la negociacion,
 * y debe registrar la razón del cambio y el valor del ajuste.
 * Tabla: PGS.ADNG
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ADNG", schema = "PGS")
@SequenceGenerator(name = "SQ_ADNGCDGO", sequenceName = "PGS.SQ_ADNGCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "AdendumNegociacionAll", query = "select e from AdendumNegociacion e"),
    @NamedQuery(name = "AdendumNegociacionId", query = "select e from AdendumNegociacion e where e.id = :id")
})
public class AdendumNegociacion implements Serializable {

    /** Id de tabla. */
    @Basic
    @Id
    @Column(name = "ID", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_ADNGCDGO")
    private Long id;

    /** Negociacion sobre la que aplica el adendum. */
    @ManyToOne
    @JoinColumn(name = "NEGOCIACION", referencedColumnName = "ID")
    private NegociacionProveedor negociacion;

    /** Numero o referencia del adendum (ej: "ADENDUM-001"). */
    @Basic
    @Column(name = "NUMADENDUM", length = 200)
    private String numAdendum;

    /** Fecha en la que se firmó o acordó el adendum. */
    @Basic
    @Column(name = "FECHAADENDUM")
    private LocalDate fechaAdendum;

    /** Descripcion o razon del adendum (motivo del cambio de valor). */
    @Basic
    @Column(name = "DESCRIPCION", length = 2000)
    private String descripcion;

    /**
     * Valor del ajuste del adendum.
     * Positivo si incrementa el valor total de la negociacion.
     * Negativo si lo reduce.
     */
    @Basic
    @Column(name = "VALORAJUSTE")
    private Double valorAjuste;

    /**
     * Valor total de la negociacion luego de aplicar este adendum.
     * Se calcula como: valor total anterior + valorAjuste.
     */
    @Basic
    @Column(name = "VALORTOTALRESULTANTE")
    private Double valorTotalResultante;

    /** Observaciones adicionales del adendum. */
    @Basic
    @Column(name = "OBSERVACION", length = 2000)
    private String observacion;

    /**
     * Estado del adendum.
     * 1 = Activo, 0 = Anulado.
     */
    @Basic
    @Column(name = "ESTADO")
    private Long estado;

    /** Usuario que registra el adendum. */
    @ManyToOne
    @JoinColumn(name = "USUARIO", referencedColumnName = "PJRQCDGO")
    private Usuario usuario;

    /** Fecha y hora en la que se registra el adendum en el sistema. */
    @Basic
    @Column(name = "FECHAREGISTRO")
    private LocalDateTime fechaRegistro;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public NegociacionProveedor getNegociacion() { return negociacion; }
    public void setNegociacion(NegociacionProveedor negociacion) { this.negociacion = negociacion; }

    public String getNumAdendum() { return numAdendum; }
    public void setNumAdendum(String numAdendum) { this.numAdendum = numAdendum; }

    public LocalDate getFechaAdendum() { return fechaAdendum; }
    public void setFechaAdendum(LocalDate fechaAdendum) { this.fechaAdendum = fechaAdendum; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Double getValorAjuste() { return valorAjuste; }
    public void setValorAjuste(Double valorAjuste) { this.valorAjuste = valorAjuste; }

    public Double getValorTotalResultante() { return valorTotalResultante; }
    public void setValorTotalResultante(Double valorTotalResultante) { this.valorTotalResultante = valorTotalResultante; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}

package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDateTime;

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
 * Representa la tabla CRD.DPLV (DetallePlazoVencido).
 *
 * Una fila por cada cuota POSTERIOR a la fecha de corte a la que se le anuló el seguro
 * (desgravamen e incendio) al declarar plazo vencido (D13, D19, D22). Guarda los valores
 * originales de la cuota para que el reverso (D23) pueda restituirlos.
 *
 * {@code DTPRDSOR} (desgravamenOriginal) NO sirve para esto: ya tiene dueño (el abono a
 * capital). Ver {@code docs/logica-negocio/crd/sql/247_DDL_DECLARACION_PLAZO_VENCIDO.sql}.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DPLV", schema = "CRD")
@SequenceGenerator(name = "SQ_DPLVCDGO", sequenceName = "CRD.SQ_DPLVCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "DetallePlazoVencidoAll", query = "select e from DetallePlazoVencido e"),
    @NamedQuery(name = "DetallePlazoVencidoId",  query = "select e from DetallePlazoVencido e where e.codigo = :id")
})
public class DetallePlazoVencido implements Serializable {

    /** Código PK. */
    @Id
    @Basic
    @Column(name = "DPLVCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DPLVCDGO")
    private Long codigo;

    /** FK - Declaración que anuló el seguro de esta cuota. */
    @ManyToOne
    @JoinColumn(name = "PLVNCDGO", referencedColumnName = "PLVNCDGO")
    private DeclaracionPlazoVencido declaracion;

    /** FK - Cuota (CRD.DTPR) a la que se le anuló el seguro. */
    @ManyToOne
    @JoinColumn(name = "DTPRCDGO", referencedColumnName = "DTPRCDGO")
    private DetallePrestamo cuota;

    /** DTPRDSGR (desgravamen) de la cuota, antes de anularlo. */
    @Basic
    @Column(name = "DPLVDSGR")
    private Double desgravamenOriginal;

    /** DTPRVLSI (seguro de incendio) de la cuota, antes de anularlo. */
    @Basic
    @Column(name = "DPLVVLSI")
    private Double valorSeguroIncendioOriginal;

    /** DTPRTTLL (total) de la cuota, antes de anular el seguro. */
    @Basic
    @Column(name = "DPLVTTLL")
    private Double totalOriginal;

    /** DTPRTTCS (totalConSeguro) de la cuota, antes de anular el seguro. */
    @Basic
    @Column(name = "DPLVTTCS")
    private Double totalConSeguroOriginal;

    /** Cuándo se restituyó (reverso). Nulo = el seguro de esta cuota sigue anulado. */
    @Basic
    @Column(name = "DPLVFCRS")
    private LocalDateTime fechaRestitucion;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public DeclaracionPlazoVencido getDeclaracion() { return declaracion; }
    public void setDeclaracion(DeclaracionPlazoVencido declaracion) { this.declaracion = declaracion; }

    public DetallePrestamo getCuota() { return cuota; }
    public void setCuota(DetallePrestamo cuota) { this.cuota = cuota; }

    public Double getDesgravamenOriginal() { return desgravamenOriginal; }
    public void setDesgravamenOriginal(Double desgravamenOriginal) { this.desgravamenOriginal = desgravamenOriginal; }

    public Double getValorSeguroIncendioOriginal() { return valorSeguroIncendioOriginal; }
    public void setValorSeguroIncendioOriginal(Double valorSeguroIncendioOriginal) { this.valorSeguroIncendioOriginal = valorSeguroIncendioOriginal; }

    public Double getTotalOriginal() { return totalOriginal; }
    public void setTotalOriginal(Double totalOriginal) { this.totalOriginal = totalOriginal; }

    public Double getTotalConSeguroOriginal() { return totalConSeguroOriginal; }
    public void setTotalConSeguroOriginal(Double totalConSeguroOriginal) { this.totalConSeguroOriginal = totalConSeguroOriginal; }

    public LocalDateTime getFechaRestitucion() { return fechaRestitucion; }
    public void setFechaRestitucion(LocalDateTime fechaRestitucion) { this.fechaRestitucion = fechaRestitucion; }
}

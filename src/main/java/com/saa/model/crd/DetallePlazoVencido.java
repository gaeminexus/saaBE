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

    /**
     * DTPRCDGO de la cuota a la que se le anuló el seguro, SIN relación ni FK a propósito
     * (H82, corregido 2026-10-02, sql/307 — ver {@code docs/logica-negocio/crd/sql/307_DPLV_QUITAR_FK_A_DTPR.sql}):
     * el abono a capital y el reverso de operaciones BORRAN cuotas de DTPR
     * ({@code AbonoCapitalPrestamoServiceImpl:194-203}, {@code ProcesoPagoPrestamoServiceImpl:1336}).
     * Con un {@code @ManyToOne} EAGER (el diseño original), cargar un DPLV cuya cuota ya no
     * existe revienta con {@code EntityNotFoundException} apenas Hibernate intenta hidratarlo —
     * pasó en producción con préstamos declarados en plazo vencido que después recibieron un
     * abono. DPLV es historia: conserva el id aunque la cuota ya no exista. Buscarla aparte, por
     * este id, y tolerar que no exista (ver {@code revertir}, {@code cuotasNoRestituidas}).
     */
    @Basic
    @Column(name = "DTPRCDGO")
    private Long idCuota;

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

    public Long getIdCuota() { return idCuota; }
    public void setIdCuota(Long idCuota) { this.idCuota = idCuota; }

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

package com.saa.model.crd;

import java.io.Serializable;

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
 * Representa la tabla CRD.DACC (DetalleAcuerdoCondonacion).
 *
 * Detalle por concepto de un {@link AcuerdoCondonacion}: exactamente 5 filas, una por cada
 * concepto del préstamo ({@link com.saa.rubros.CrdConceptoPrestamo}), cada una con su monto
 * adeudado, pagado y condonado. Desgravamen y Seguro de incendio SIEMPRE con condonado = 0
 * (K3 — se pagan al 100%, nunca se condonan).
 *
 * Ver {@code docs/logica-negocio/crd/PLAN-ACUERDOS-PAGO-CONDONACION.md} §2.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DACC", schema = "CRD")
@SequenceGenerator(name = "SQ_DACCCDGO", sequenceName = "CRD.SQ_DACCCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "DetalleAcuerdoCondonacionAll", query = "select e from DetalleAcuerdoCondonacion e"),
    @NamedQuery(name = "DetalleAcuerdoCondonacionId",  query = "select e from DetalleAcuerdoCondonacion e where e.codigo = :id")
})
public class DetalleAcuerdoCondonacion implements Serializable {

    /** Código de la línea. PK. */
    @Id
    @Basic
    @Column(name = "DACCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DACCCDGO")
    private Long codigo;

    /** FK - Acuerdo al que pertenece esta línea. */
    @ManyToOne
    @JoinColumn(name = "ACCNCDGO", referencedColumnName = "ACCNCDGO")
    private AcuerdoCondonacion acuerdo;

    /** Concepto: ver {@link com.saa.rubros.CrdConceptoPrestamo} (rubro 248). */
    @Basic
    @Column(name = "DACCCPTO")
    private Long concepto;

    /** Monto adeudado de este concepto. */
    @Basic
    @Column(name = "DACCVLAD")
    private Double valorAdeudado;

    /** Monto pagado de este concepto. */
    @Basic
    @Column(name = "DACCVLPG")
    private Double valorPagado;

    /** Monto condonado de este concepto. Siempre 0 para Desgravamen y Seguro de incendio. */
    @Basic
    @Column(name = "DACCVLCN")
    private Double valorCondonado;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public AcuerdoCondonacion getAcuerdo() {
        return acuerdo;
    }

    public void setAcuerdo(AcuerdoCondonacion acuerdo) {
        this.acuerdo = acuerdo;
    }

    public Long getConcepto() {
        return concepto;
    }

    public void setConcepto(Long concepto) {
        this.concepto = concepto;
    }

    public Double getValorAdeudado() {
        return valorAdeudado;
    }

    public void setValorAdeudado(Double valorAdeudado) {
        this.valorAdeudado = valorAdeudado;
    }

    public Double getValorPagado() {
        return valorPagado;
    }

    public void setValorPagado(Double valorPagado) {
        this.valorPagado = valorPagado;
    }

    public Double getValorCondonado() {
        return valorCondonado;
    }

    public void setValorCondonado(Double valorCondonado) {
        this.valorCondonado = valorCondonado;
    }
}

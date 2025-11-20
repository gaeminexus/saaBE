package com.saa.model.credito;

import java.io.Serializable;
import java.sql.Timestamp;

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
 * Representa la tabla PGAP (PagoAporte).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PGAP", schema = "CRD")
@SequenceGenerator(name = "SQ_PGAPCDGO", sequenceName = "CRD.SQ_PGAPCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "PagoAporteAll", query = "select e from PagoAporte e"),
    @NamedQuery(name = "PagoAporteId", query = "select e from PagoAporte e where e.codigo = :id")
})
public class PagoAporte implements Serializable {

    /**
     * Código del pago.
     */
    @Id
    @Basic
    @Column(name = "PGAPCDGO")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PGAPCDGO")
    private Long codigo;

    /**
     * Filial (empresa).
     */
    @ManyToOne
    @JoinColumn(name = "FLLLCDGO", referencedColumnName = "FLLLCDGO")
    private Filial filial;

    /**
     * Aporte.
     */
    @ManyToOne
    @JoinColumn(name = "APRTCDGO", referencedColumnName = "APRTCDGO")
    private Aporte aporte;

    /**
     * Valor.
     */
    @Basic
    @Column(name = "PGAPVLRR")
    private Double valor;

    /**
     * Fecha contable.
     */
    @Basic
    @Column(name = "PGAPFCCN")
    private Timestamp fechaContable;

    /**
     * Número de asiento.
     */
    @Basic
    @Column(name = "PGAPNMAS")
    private Long numeroAsiento;

    /**
     * Concepto.
     */
    @Basic
    @Column(name = "PGAPCNCP", length = 2000)
    private String concepto;

    /**
     * Fecha registro.
     */
    @Basic
    @Column(name = "PGAPFCRG")
    private Timestamp fechaRegistro;

    /**
     * Usuario registro.
     */
    @Basic
    @Column(name = "PGAPUSRG", length = 200)
    private String usuarioRegistro;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "PGAPIDST")
    private Long estado;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Filial getFilial() {
        return filial;
    }

    public void setFilial(Filial filial) {
        this.filial = filial;
    }

    public Aporte getAporte() {
        return aporte;
    }

    public void setAporte(Aporte aporte) {
        this.aporte = aporte;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Timestamp getFechaContable() {
        return fechaContable;
    }

    public void setFechaContable(Timestamp fechaContable) {
        this.fechaContable = fechaContable;
    }

    public Long getNumeroAsiento() {
        return numeroAsiento;
    }

    public void setNumeroAsiento(Long numeroAsiento) {
        this.numeroAsiento = numeroAsiento;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public Timestamp getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(Timestamp fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}

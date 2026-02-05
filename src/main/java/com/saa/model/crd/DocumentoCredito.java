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
import jakarta.persistence.Table;

/**
 * Representa la tabla DCMN (DocumentoCredito).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DCMN", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "DocumentoCreditoAll", query = "select e from DocumentoCredito e"),
    @NamedQuery(name = "DocumentoCreditoId", query = "select e from DocumentoCredito e where e.codigo = :id")
})
public class DocumentoCredito implements Serializable {

    /**
     * Código.
     */
    @Id
    @Basic
    @Column(name = "DCMNCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * FK - Código Filial.
     */
    @ManyToOne
    @JoinColumn(name = "FLLLCDGO", referencedColumnName = "FLLLCDGO")
    private Filial filial;

    /**
     * FK - Tipo Préstamo.
     */
    @ManyToOne
    @JoinColumn(name = "TPPRCDGO", referencedColumnName = "TPPRCDGO")
    private TipoPrestamo tipoPrestamo;

    /**
     * FK - Tipo Adjunto.
     */
    @ManyToOne
    @JoinColumn(name = "TPDJCDGO", referencedColumnName = "TPDJCDGO")
    private TipoAdjunto tipoAdjunto;

    /**
     * Cantidad.
     */
    @Basic
    @Column(name = "DCMNCNTD")
    private Long cantidad;

    /**
     * Opcional.
     */
    @Basic
    @Column(name = "DCMNOPCN")
    private Long opcional;

    /**
     * Usuario de ingreso.
     */
    @Basic
    @Column(name = "DCMNUSIN", length = 50)
    private String usuarioIngreso;

    /**
     * Fecha de ingreso.
     */
    @Basic
    @Column(name = "DCMNFCIN")
    private LocalDateTime fechaIngreso;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "DCMNIDST")
    private Long estado;

    // ======================
    // Getters y Setters
    // ======================

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

    public TipoPrestamo getTipoPrestamo() {
        return tipoPrestamo;
    }

    public void setTipoPrestamo(TipoPrestamo tipoPrestamo) {
        this.tipoPrestamo = tipoPrestamo;
    }

    public TipoAdjunto getTipoAdjunto() {
        return tipoAdjunto;
    }

    public void setTipoAdjunto(TipoAdjunto tipoAdjunto) {
        this.tipoAdjunto = tipoAdjunto;
    }

    public Long getCantidad() {
        return cantidad;
    }

    public void setCantidad(Long cantidad) {
        this.cantidad = cantidad;
    }

    public Long getOpcional() {
        return opcional;
    }

    public void setOpcional(Long opcional) {
        this.opcional = opcional;
    }

    public String getUsuarioIngreso() {
        return usuarioIngreso;
    }

    public void setUsuarioIngreso(String usuarioIngreso) {
        this.usuarioIngreso = usuarioIngreso;
    }

    public LocalDateTime getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDateTime fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}


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
 * Representa la tabla RLPR (RelacionPrestamo).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RLPR", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "RelacionPrestamoAll", query = "select e from RelacionPrestamo e"),
    @NamedQuery(name = "RelacionPrestamoId", query = "select e from RelacionPrestamo e where e.codigo = :id")
})
public class RelacionPrestamo implements Serializable {

    /** Código */
    @Id
    @Basic
    @Column(name = "RLPRCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - ID Préstamo Hijo */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGH", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamoHijo;

    /** FK - ID Préstamo Padre */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGP", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamoPadre;

    /** Tipo */
    @Basic
    @Column(name = "RLPRTPPR", length = 500)
    private String tipo;

    /** Fecha registro */
    @Basic
    @Column(name = "RLPRFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario registro */
    @Basic
    @Column(name = "RLPRUSRG", length = 200)
    private String usuarioRegistro;

    /** Estado */
    @Basic
    @Column(name = "RLPRIDST")
    private Long estado;

    // ============================
    // Getters y Setters
    // ============================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Prestamo getPrestamoHijo() {
        return prestamoHijo;
    }

    public void setPrestamoHijo(Prestamo prestamoHijo) {
        this.prestamoHijo = prestamoHijo;
    }

    public Prestamo getPrestamoPadre() {
        return prestamoPadre;
    }

    public void setPrestamoPadre(Prestamo prestamoPadre) {
        this.prestamoPadre = prestamoPadre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
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


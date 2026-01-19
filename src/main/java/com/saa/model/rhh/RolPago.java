package com.saa.model.rhh;

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
 * Comprobante individual de pago generado desde la nómina.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RLPG", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "RolPago.findAll",
                query = "select r from RolPago r"),
    @NamedQuery(name = "RolPago.findById",
                query = "select r from RolPago r where r.codigo = :id"),
    @NamedQuery(name = "RolPago.findByNumero",
                query = "select r from RolPago r where r.numero = :numero"),
    @NamedQuery(name = "RolPago.findActivos",
                query = "select r from RolPago r where r.estado = 'A'")
})
public class RolPago implements Serializable {

    /**
     * Código único del rol.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "RLPGCDGO")
    private Long codigo;

    /**
     * Nómina asociada.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "NMNACDGO", nullable = false)
    private Nomina nomina;

    /**
     * Número del rol de pago.
     */
    @Basic
    @Column(name = "RLPGNMRO")
    private String numero;

    /**
     * Fecha de emisión.
     */
    @Basic
    @Column(name = "RLPGFCHA")
    private LocalDate fechaEmision;

    /**
     * Ruta o referencia del PDF del rol.
     */
    @Basic
    @Column(name = "RLPGPDFO")
    private String rutaPdf;

    /**
     * Estado del registro (A=Activo, I=Inactivo).
     */
    @Basic
    @Column(name = "RLPGESTD")
    private String estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "RLPGFCHR")
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "RLPGUSRR")
    private String usuarioRegistro;

    // =============================
    // Getters y Setters
    // =============================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Nomina getNomina() {
        return nomina;
    }

    public void setNomina(Nomina nomina) {
        this.nomina = nomina;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public String getRutaPdf() {
        return rutaPdf;
    }

    public void setRutaPdf(String rutaPdf) {
        this.rutaPdf = rutaPdf;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }
}

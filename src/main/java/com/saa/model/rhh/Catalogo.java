package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Catálogo de tipos de permisos y licencias.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CTLG", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "Catalogo.findAll", query = "select t from Catalogo t"),
    @NamedQuery(name = "Catalogo.findById", query = "select t from Catalogo t where t.codigo = :id"),
    @NamedQuery(name = "Catalogo.findByNombre", query = "select t from Catalogo t where t.nombre = :nombre")
})
public class Catalogo implements Serializable {

    /**
     * Código único del tipo de permiso.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "CTLGCDGO")
    private Long codigo;

    /**
     * Nombre del permiso o licencia.
     */
    @Basic
    @Column(name = "CTLGNMBR", length = 120, nullable = false)
    private String nombre;

    /**
     * Indica si requiere documento (S/N).
     */
    @Basic
    @Column(name = "CTLGRQDC", length = 1, nullable = false)
    private String requiereDocumento;

    /**
     * Indica si es con goce (S/N).
     */
    @Basic
    @Column(name = "CTLGGCEE", length = 1, nullable = false)
    private String conGoce;

    /**
     * Estado del registro (A=Activo, I=Inactivo).
     */
    @Basic
    @Column(name = "CTLGESTD", length = 1, nullable = false)
    private String estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "CTLGFCHR", nullable = false)
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "CTLGUSRR", length = 60)
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

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRequiereDocumento() {
        return requiereDocumento;
    }

    public void setRequiereDocumento(String requiereDocumento) {
        this.requiereDocumento = requiereDocumento;
    }

    public String getConGoce() {
        return conGoce;
    }

    public void setConGoce(String conGoce) {
        this.conGoce = conGoce;
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

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
 * Catálogo de tipos de contrato (plazo fijo, indefinido, etc.).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TPCN", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "TipoContratoId", query = "select e from TipoContrato e where e.codigo=:id"),
    @NamedQuery(name = "TipoContratoAll", query = "select e from TipoContrato e")
})
public class TipoContratoEmpleado implements Serializable {

    /**
     * Código único del tipo de contrato.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "TPCNCDGO")
    private Long codigo;

    /**
     * Nombre del tipo de contrato.
     */
    @Basic
    @Column(name = "TPCNNMBR")
    private String nombre;

    /**
     * Indica si requiere fecha de fin (S/N).
     */
    @Basic
    @Column(name = "TPCNRQRE")
    private String requiereFechaFin;

    /**
     * Estado del registro (A=Activo, I=Inactivo).
     */
    @Basic
    @Column(name = "TPCNESTD")
    private String estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "TPCNFCHR")
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "TPCNUSRR")
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

    public String getRequiereFechaFin() {
        return requiereFechaFin;
    }

    public void setRequiereFechaFin(String requiereFechaFin) {
        this.requiereFechaFin = requiereFechaFin;
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

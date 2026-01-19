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
 * Catálogo de rubros de nómina.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RBRO", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "RubroRhh.findAll",
                query = "select r from Rubro r"),
    @NamedQuery(name = "RubroRhh.findById",
                query = "select r from Rubro r where r.codigo = :id"),
    @NamedQuery(name = "RubroRhh.findActivos",
                query = "select r from Rubro r where r.estado = 'A'"),
    @NamedQuery(name = "RubroRhh.findByTipo",
                query = "select r from Rubro r where r.tipo = :tipo")
})
public class RubroRhh implements Serializable {

    /**
     * Código único del rubro.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "RBROCDGO")
    private Long codigo;

    /**
     * Nombre del rubro.
     */
    @Basic
    @Column(name = "RBRONMBR", length = 120, nullable = false)
    private String nombre;

    /**
     * Tipo de rubro (INGRESO / DESCUENTO / APORTE).
     */
    @Basic
    @Column(name = "RBROTPOO", length = 12, nullable = false)
    private String tipo;

    /**
     * Fórmula del rubro (si aplica).
     */
    @Basic
    @Column(name = "RBROFRML", length = 500)
    private String formula;

    /**
     * Estado del rubro (A=Activo, I=Inactivo).
     */
    @Basic
    @Column(name = "RBROESTD", length = 1, nullable = false)
    private String estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "RBROFCHR", nullable = false)
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "RBROUSRR", length = 60)
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
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

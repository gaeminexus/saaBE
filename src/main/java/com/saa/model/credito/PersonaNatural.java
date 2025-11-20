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
 * Representa la tabla PRSN (PersonaNatural).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PRSN", schema = "CRD")
@SequenceGenerator(name = "SQ_PRSNCDGO", sequenceName = "CRD.SQ_PRSNCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "PersonaNaturalAll", query = "select e from PersonaNatural e"),
    @NamedQuery(name = "PersonaNaturalId", query = "select e from PersonaNatural e where e.codigo = :id")
})
public class PersonaNatural implements Serializable {

    /**
     * Código (PK y FK de Entidad).
     */
    @Id
    @Basic
    @Column(name = "PRSNCDGO")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PRSNCDGO")
    private Long codigo;

    /**
     * Filial.
     */
    @ManyToOne
    @JoinColumn(name = "FLLLCDGO", referencedColumnName = "FLLLCDGO")
    private Filial filial;

    /**
     * Nombres.
     */
    @Basic
    @Column(name = "PRSNNMBR", length = 2000)
    private String nombres;

    /**
     * Apellidos.
     */
    @Basic
    @Column(name = "PRSNAPLL", length = 2000)
    private String apellidos;

    /**
     * Estado civil.
     */
    @Basic
    @Column(name = "PRSNESCV", length = 2000)
    private String estadoCivil;

    /**
     * Género.
     */
    @Basic
    @Column(name = "PRSNGNRO", length = 200)
    private String genero;

    /**
     * Usuario ingreso.
     */
    @Basic
    @Column(name = "PRSNUSIN", length = 50)
    private String usuarioIngreso;

    /**
     * Fecha ingreso.
     */
    @Basic
    @Column(name = "PRSNFCIN")
    private Timestamp fechaIngreso;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "PRSNIDST")
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

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getEstadoCivil() {
        return estadoCivil;
    }

    public void setEstadoCivil(String estadoCivil) {
        this.estadoCivil = estadoCivil;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getUsuarioIngreso() {
        return usuarioIngreso;
    }

    public void setUsuarioIngreso(String usuarioIngreso) {
        this.usuarioIngreso = usuarioIngreso;
    }

    public Timestamp getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(Timestamp fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}

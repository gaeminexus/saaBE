package com.saa.model.credito;

import java.io.Serializable;

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
 * Representa la tabla PSSS (Catálogo de países).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PSSS", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "PaisAll", query = "select e from Pais e"),
    @NamedQuery(name = "PaisId", query = "select e from Pais e where e.codigo = :id")
})
public class Pais implements Serializable {

    /**
     * Código del país.
     */
    @Id
    @Basic
    @Column(name = "PSSSCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Código alterno (INEC).
     */
    @Basic
    @Column(name = "PSSSCDAL", length = 10)
    private String codigoAlterno;

    /**
     * Nombre del país.
     */
    @Basic
    @Column(name = "PSSSNMBR", length = 2000)
    private String nombre;

    /**
     * Nacionalidad.
     */
    @Basic
    @Column(name = "PSSSNCNL", length = 2000)
    private String nacionalidad;

    /**
     * Código de nacionalidad.
     */
    @Basic
    @Column(name = "PSSSCDNC", length = 10)
    private String codigoNacionalidad;

    /**
     * Código externo.
     */
    @Basic
    @Column(name = "PSSSCDEX", length = 50)
    private String codigoExterno;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "PSSSIDST")
    private Long estado;

    // ============================================================
    // Getters y Setters
    // ============================================================

    /**
     * Devuelve codigo.
     * @return codigo.
     */
    public Long getCodigo() {
        return codigo;
    }

    /**
     * Asigna codigo.
     * @param codigo nuevo valor para codigo.
     */
    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    /**
     * Devuelve codigoAlterno.
     * @return codigoAlterno.
     */
    public String getCodigoAlterno() {
        return codigoAlterno;
    }

    /**
     * Asigna codigoAlterno.
     * @param codigoAlterno nuevo valor para codigoAlterno.
     */
    public void setCodigoAlterno(String codigoAlterno) {
        this.codigoAlterno = codigoAlterno;
    }

    /**
     * Devuelve nombre.
     * @return nombre.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Asigna nombre.
     * @param nombre nuevo valor para nombre.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Devuelve nacionalidad.
     * @return nacionalidad.
     */
    public String getNacionalidad() {
        return nacionalidad;
    }

    /**
     * Asigna nacionalidad.
     * @param nacionalidad nuevo valor para nacionalidad.
     */
    public void setNacionalidad(String nacionalidad) {
        this.nacionalidad = nacionalidad;
    }

    /**
     * Devuelve codigoNacionalidad.
     * @return codigoNacionalidad.
     */
    public String getCodigoNacionalidad() {
        return codigoNacionalidad;
    }

    /**
     * Asigna codigoNacionalidad.
     * @param codigoNacionalidad nuevo valor para codigoNacionalidad.
     */
    public void setCodigoNacionalidad(String codigoNacionalidad) {
        this.codigoNacionalidad = codigoNacionalidad;
    }

    /**
     * Devuelve codigoExterno.
     * @return codigoExterno.
     */
    public String getCodigoExterno() {
        return codigoExterno;
    }

    /**
     * Asigna codigoExterno.
     * @param codigoExterno nuevo valor para codigoExterno.
     */
    public void setCodigoExterno(String codigoExterno) {
        this.codigoExterno = codigoExterno;
    }

    /**
     * Devuelve estado.
     * @return estado.
     */
    public Long getEstado() {
        return estado;
    }

    /**
     * Asigna estado.
     * @param estado nuevo valor para estado.
     */
    public void setEstado(Long estado) {
        this.estado = estado;
    }
}

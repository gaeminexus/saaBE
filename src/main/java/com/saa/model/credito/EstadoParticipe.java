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
 * Representa la tabla ESPR (Estado Partícipe).
 * Catálogo de estados del partícipe.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ESPR", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "EstadoParticipeAll", query = "select e from EstadoParticipe e"),
    @NamedQuery(name = "EstadoParticipeId", query = "select e from EstadoParticipe e where e.codigo = :id")
})
public class EstadoParticipe implements Serializable {
    
    /**
     * Código.
     */
    @Id
    @Basic
    @Column(name = "ESPRCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;
    
    /**
     * Nombre.
     */
    @Basic
    @Column(name = "ESPRNMBR", length = 2000, nullable = false)
    private String nombre;
    
    /**
     * Código externo.
     */
    @Basic
    @Column(name = "ESPRCDEX")
    private Long codigoExterno;
    
    /**
     * ID Estado.
     */
    @Basic
    @Column(name = "ESPRIDST", nullable = false)
    private Long idEstado;
    
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
     * Devuelve codigoExterno.
     * @return codigoExterno.
     */
    public Long getCodigoExterno() {
        return codigoExterno;
    }
    
    /**
     * Asigna codigoExterno.
     * @param codigoExterno nuevo valor para codigoExterno.
     */
    public void setCodigoExterno(Long codigoExterno) {
        this.codigoExterno = codigoExterno;
    }
    
    /**
     * Devuelve idEstado.
     * @return idEstado.
     */
    public Long getIdEstado() {
        return idEstado;
    }
    
    /**
     * Asigna idEstado.
     * @param idEstado nuevo valor para idEstado.
     */
    public void setIdEstado(Long idEstado) {
        this.idEstado = idEstado;
    }
}


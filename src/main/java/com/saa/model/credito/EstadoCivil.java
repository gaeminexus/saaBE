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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Representa la tabla ESCV (Estado Civil).
 * Catálogo de estados civiles.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ESCV", schema = "CRD")
@SequenceGenerator(name = "SQ_ESCVCDGO", sequenceName = "CRD.ISEQ$$_77253", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "EstadoCivilAll", query = "select e from EstadoCivil e"),
    @NamedQuery(name = "EstadoCivilId", query = "select e from EstadoCivil e where e.codigo = :id")
})
public class EstadoCivil implements Serializable {
    
    /**
     * Código.
     */
    @Id
    @Basic
    @Column(name = "ESCVCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;
    
    /**
     * Nombre.
     */
    @Basic
    @Column(name = "ESCVNMBR", length = 2000, nullable = false)
    private String nombre;
    
    /**
     * Código auxiliar.
     */
    @Basic
    @Column(name = "ESCVCAUX")
    private Long codigoAuxiliar;
    
    /**
     * ID Estado.
     */
    @Basic
    @Column(name = "ESCVIDST", nullable = false)
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
     * Devuelve codigoAuxiliar.
     * @return codigoAuxiliar.
     */
    public Long getCodigoAuxiliar() {
        return codigoAuxiliar;
    }
    
    /**
     * Asigna codigoAuxiliar.
     * @param codigoAuxiliar nuevo valor para codigoAuxiliar.
     */
    public void setCodigoAuxiliar(Long codigoAuxiliar) {
        this.codigoAuxiliar = codigoAuxiliar;
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
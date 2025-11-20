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
 * Representa la tabla ESCS (Estado Cesantía).
 * Catálogo de estados de cesantía.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ESCS", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "EstadoCesantiaAll", query = "select e from EstadoCesantia e"),
    @NamedQuery(name = "EstadoCesantiaId", query = "select e from EstadoCesantia e where e.codigo = :id")
})
public class EstadoCesantia implements Serializable {
    
    /**
     * Código.
     */
    @Id
    @Basic
    @Column(name = "ESCSCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;
    
    /**
     * Descripción.
     */
    @Basic
    @Column(name = "ESCSDSCR", length = 2000, nullable = false)
    private String descripcion;
    
    /**
     * ID Estado.
     */
    @Basic
    @Column(name = "ESCSIDST", nullable = false)
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
     * Devuelve descripcion.
     * @return descripcion.
     */
    public String getDescripcion() {
        return descripcion;
    }
    
    /**
     * Asigna descripcion.
     * @param descripcion nuevo valor para descripcion.
     */
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
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

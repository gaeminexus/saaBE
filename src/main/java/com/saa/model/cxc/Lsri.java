/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de Compuseg Cía. Ltda. ("Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.model.cxc;

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
 * @author GaemiSoft
 * Pojo mapeo de tabla CBR.LSRI.
 * Entity Lsri.
 * Contiene la lista de categorías de información del SRI.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "LSRI", schema = "CBR")
@SequenceGenerator(name = "SQ_LSRI", sequenceName = "CBR.SQ_LSRI", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "LsriAll", query = "select e from Lsri e"),
    @NamedQuery(name = "LsriId", query = "select e from Lsri e where e.id = :id")
})
public class Lsri implements Serializable {

    /**
     * ID de la entidad.
     */
    @Basic
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_LSRI")
    private Long id;
    
    /**
     * Nombre de la tabla relacionada.
     */
    @Basic
    @Column(name = "TABLA", length = 100)
    private String tabla;
    
    /**
     * Detalle descriptivo de la categoría.
     */
    @Basic
    @Column(name = "DETALLE", length = 500)
    private String detalle;
    
    /**
     * Estado: 1 = activo, 2 = inactivo.
     */
    @Basic
    @Column(name = "ESTADO")
    private Long estado;

    // ============================================================
    // Getters y Setters
    // ============================================================
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTabla() {
        return tabla;
    }
    
    public void setTabla(String tabla) {
        this.tabla = tabla;
    }
    
    public String getDetalle() {
        return detalle;
    }
    
    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
    
    public Long getEstado() {
        return estado;
    }
    
    public void setEstado(Long estado) {
        this.estado = estado;
    }
}

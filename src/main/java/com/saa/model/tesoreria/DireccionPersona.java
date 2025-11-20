/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.model.tesoreria;

import java.io.Serializable;

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
 * @author GaemiSoft
 * <p>Pojo mapeo de tabla TSR.PDRC.
 *  Entity Direccion por persona.
 *  Contiene las distintas direcciones de una persona.
 *  Es detalle de la entidad persona.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PDRC", schema = "TSR")
@SequenceGenerator(name = "SQ_PDRCCDGO", sequenceName = "TSR.SQ_PDRCCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "DireccionPersonaAll", query = "select e from DireccionPersona e"),
	@NamedQuery(name = "DireccionPersonaId", query = "select e from DireccionPersona e where e.codigo = :id")
})
public class DireccionPersona implements Serializable {

    /**
     * Id de tabla.
     */
    @Basic
    @Id
    @Column(name = "PDRCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PDRCCDGO")
    private Long codigo;
    
    /**
     * Persona a la que pertenece la direccion
     */
    @ManyToOne
    @JoinColumn(name = "PRSNCDGO", referencedColumnName = "PRSNCDGO")
    private Persona persona;    

    /**
     * Rubro 30. Indica el tipo de banco.
     */
    @Basic
    @Column(name = "PDRCRYYA")
    private Long rubroTipoDireccionP;
    
    /**
     * Detalle de Rubro 30. Indica el tipo de banco.
     */
    @Basic
    @Column(name = "PDRCRZZA")
    private Long rubroTipoDireccionH;
    
    /**
     * Ubicacion de la direccion. Detalle de calle y numeracion
     */
    @Basic
    @Column(name = "PDRCDRCC", length = 100)
    private String ubicacion;
    
    /**
     * Indica si es la direccion principal. 1 = Principal, 0 = No principal
     */
    @Basic
    @Column(name = "PDRCPRNC")
    private Long principal;    
    
    /**
     * Devuelve codigo
     * @return codigo
     */
    public Long getCodigo() {
        return codigo;
    }

    /**
     * Asigna codigo
     * @param codigo nuevo valor para codigo 
     */
    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }
    
    /**
     * Devuelve persona
     */
    public Persona getPersona() {
        return this.persona;
    }
    
    /**
     * Asigna persona
     */
    public void setPersona(Persona persona) {
        this.persona = persona;
    }

    /**
     * Devuelve rubroTipoDireccionP
     * @return rubroTipoDireccionP
     */
    public Long getRubroTipoDireccionP() {
        return rubroTipoDireccionP;
    }

    /**
     * Asigna rubroTipoDireccionP
     * @param rubroTipoDireccionP nuevo valor para rubroTipoDireccionP 
     */
    public void setRubroTipoDireccionP(Long rubroTipoDireccionP) {
        this.rubroTipoDireccionP = rubroTipoDireccionP;
    }
    
    /**
     * Devuelve rubroTipoDireccionH
     * @return rubroTipoDireccionH
     */
    public Long getRubroTipoDireccionH() {
        return rubroTipoDireccionH;
    }

    /**
     * Asigna rubroTipoDireccionH
     * @param rubroTipoDireccionH nuevo valor para rubroTipoDireccionH 
     */
    public void setRubroTipoDireccionH(Long rubroTipoDireccionH) {
        this.rubroTipoDireccionH = rubroTipoDireccionH;
    }
    
    /**
     * Devuelve ubicacion
     * @return ubicacion
     */
    public String getUbicacion() {
        return ubicacion;
    }

    /**
     * Asigna ubicacion
     * @param ubicacion nuevo valor para ubicacion 
     */
    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }
    
    /**
     * Devuelve principal
     * @return principal
     */
    public Long getPrincipal() {
        return principal;
    }

    /**
     * Asigna principal
     * @param principal nuevo valor para principal 
     */
    public void setPrincipal(Long principal) {
        this.principal = principal;
    }

}

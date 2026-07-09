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

import com.saa.model.cnt.PlanCuenta;

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
 * Pojo mapeo de tabla CBR.TSRI.
 * Entity Tsri.
 * Contiene los tipos de información del SRI relacionados con una categoría.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TSRI", schema = "CBR")
@SequenceGenerator(name = "SQ_TSRI", sequenceName = "CBR.SQ_TSRI", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "TsriAll", query = "select e from Tsri e"),
    @NamedQuery(name = "TsriId", query = "select e from Tsri e where e.id = :id")
})
public class Tsri implements Serializable {

    /**
     * ID de la entidad.
     */
    @Basic
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TSRI")
    private Long id;
    
    /**
     * Relación con la categoría LSRI.
     */
    @ManyToOne
    @JoinColumn(name = "LSRI", referencedColumnName = "ID")
    private Lsri lsri;
    
    /**
     * Código del tipo de información.
     */
    @Basic
    @Column(name = "CODIGO", length = 500)
    private String codigo;
    
    /**
     * Detalle descriptivo del tipo.
     */
    @Basic
    @Column(name = "DETALLE", length = 1000)
    private String detalle;
    
    /**
     * Porcentaje asociado.
     */
    @Basic
    @Column(name = "PORCENTAJE")
    private Double porcentaje;
    
    /**
     * Valor asociado.
     */
    @Basic
    @Column(name = "VALOR")
    private Double valor;
    
    /**
     * Texto descriptivo adicional.
     */
    @Basic
    @Column(name = "TEXTO", length = 1000)
    private String texto;
    
    /**
     * Estado: 1 = activo, 2 = inactivo.
     */
    @Basic
    @Column(name = "ESTADO")
    private Long estado;
    
    /**
     * Plan de cuenta relacionado.
     */
    @ManyToOne
    @JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
    private PlanCuenta planCuenta;

    // ============================================================
    // Getters y Setters
    // ============================================================
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Lsri getLsri() {
        return lsri;
    }
    
    public void setLsri(Lsri lsri) {
        this.lsri = lsri;
    }
    
    public String getCodigo() {
        return codigo;
    }
    
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
    
    public String getDetalle() {
        return detalle;
    }
    
    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
    
    public Double getPorcentaje() {
        return porcentaje;
    }
    
    public void setPorcentaje(Double porcentaje) {
        this.porcentaje = porcentaje;
    }
    
    public Double getValor() {
        return valor;
    }
    
    public void setValor(Double valor) {
        this.valor = valor;
    }
    
    public String getTexto() {
        return texto;
    }
    
    public void setTexto(String texto) {
        this.texto = texto;
    }
    
    public Long getEstado() {
        return estado;
    }
    
    public void setEstado(Long estado) {
        this.estado = estado;
    }
    
    public PlanCuenta getPlanCuenta() {
        return planCuenta;
    }
    
    public void setPlanCuenta(PlanCuenta planCuenta) {
        this.planCuenta = planCuenta;
    }
}

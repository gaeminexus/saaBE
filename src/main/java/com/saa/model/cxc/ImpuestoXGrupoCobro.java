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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * @author GaemiSoft
 * Pojo mapeo de tabla CBR.IXGC.
 * Entity ImpuestoXGrupoCobro.
 * Impuestos por grupo de productos de cobros. 
 * Impuestos que aplica a los productos de los clientes.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "IXGC", schema = "CBR")
@NamedQueries({
    @NamedQuery(name = "ImpuestoXGrupoCobroAll", query = "select e from ImpuestoXGrupoCobro e"),
    @NamedQuery(name = "ImpuestoXGrupoCobroId", query = "select e from ImpuestoXGrupoCobro e where e.codigo = :id")
})
public class ImpuestoXGrupoCobro implements Serializable {

    /**
     * Codigo de la entidad.
     */
    @Id
    @Basic
    @Column(name = "IXGCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_IXGCCDGO")
    @SequenceGenerator(name = "SQ_IXGCCDGO", sequenceName = "CBR.SQ_IXGCCDGO", allocationSize = 1)
    private Long codigo;
    
    /**
     * Grupo de producto al que se aplicará el impuesto.
     */
    @ManyToOne
    @JoinColumn(name = "GRPCCDGO", referencedColumnName = "GRPCCDGO")
    private GrupoProductoCobro grupoProductoCobro;
    
    /**
     * Detalle de impuesto que se aplicará al producto.
     
    @ManyToOne
    @JoinColumn(name = "DTIMCDGO", referencedColumnName = "DTIMCDGO")
    private DetalleImpuesto detalleImpuesto;*/
    
    /**
     * Estado del impuesto por grupo. 1 = activo, 2 = inactivo.
     */
    @Basic
    @Column(name = "IXGCESTD")
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
    
    public GrupoProductoCobro getGrupoProductoCobro() {
        return grupoProductoCobro;
    }
    
    public void setGrupoProductoCobro(GrupoProductoCobro grupoProductoCobro) {
        this.grupoProductoCobro = grupoProductoCobro;
    }
    
    /*
    public DetalleImpuesto getDetalleImpuesto() {
        return detalleImpuesto;
    }
    
    public void setDetalleImpuesto(DetalleImpuesto detalleImpuesto) {
        this.detalleImpuesto = detalleImpuesto;
    }*/
    
    public Long getEstado() {
        return estado;
    }
    
    public void setEstado(Long estado) {
        this.estado = estado;
    }
}
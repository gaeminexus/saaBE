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
import com.saa.model.scp.Empresa;

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
 * Pojo mapeo de tabla CBR.GRPC.
 * Entity GrupoProductoCobro.
 * Contiene el grupo de productos que ofrece la empresa a los clientes.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "GRPC", schema = "CBR")
@SequenceGenerator(name = "SQ_GPCCCDGO", sequenceName = "CBR.SQ_GPCCCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "GrupoProductoCobroAll", query = "select e from GrupoProductoCobro e"),
    @NamedQuery(name = "GrupoProductoCobroId", query = "select e from GrupoProductoCobro e where e.codigo = :id")
})
public class GrupoProductoCobro implements Serializable {

    /**
     * Codigo de la entidad.
     */
	@Basic
    @Id
    @Column(name = "GRPCCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_GPCCCDGO")
    private Long codigo;
    
    /**
     * Nombre del grupo de productos.
     */
    @Basic
    @Column(name = "GRPCNMBR")
    private String nombre;
    
    /**
     * Rubro para tipo de grupo de producto. tomado de rubro 74.
     */
    @Basic
    @Column(name = "GRPCRYYA")
    private Long rubroTipoGrupoP;
    
    /**
     * Detalle de rubro para el tipo de grupo de producto. tomado de rubro 74.
     */
    @Basic
    @Column(name = "GRPCRZZA")
    private Long rubroTipoGrupoH;
    
    /**
     * Cuenta contable asignada al grupo de producto.
     */
    @ManyToOne
    @JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
    private PlanCuenta planCuenta;
    
    /**
     * Estado 1 = activo, 2 = inactivo.
     */
    @Basic
    @Column(name = "GRPCESTD")
    private Long estado;
    
    /**
     * Empresa a la que pertenece el grupo de producto.
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;
    

    // ============================================================
    // Getters y Setters
    // ============================================================
    
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
    
    public Long getRubroTipoGrupoP() {
        return rubroTipoGrupoP;
    }
    
    public void setRubroTipoGrupoP(Long rubroTipoGrupoP) {
        this.rubroTipoGrupoP = rubroTipoGrupoP;
    }
    
    public Long getRubroTipoGrupoH() {
        return rubroTipoGrupoH;
    }
    
    public void setRubroTipoGrupoH(Long rubroTipoGrupoH) {
        this.rubroTipoGrupoH = rubroTipoGrupoH;
    }
    
    public PlanCuenta getPlanCuenta() {
        return planCuenta;
    }
    
    public void setPlanCuenta(PlanCuenta planCuenta) {
        this.planCuenta = planCuenta;
    }
    
    public Long getEstado() {
        return estado;
    }
    
    public void setEstado(Long estado) {
        this.estado = estado;
    }
    
    public Empresa getEmpresa() {
        return empresa;
    }
    
    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }
    
}
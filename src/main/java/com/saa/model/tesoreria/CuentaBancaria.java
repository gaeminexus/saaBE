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
import java.time.LocalDateTime;

import com.saa.model.contabilidad.PlanCuenta;

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
 * <p>Pojo mapeo de tabla TSR.CNBC.
 *  Entity CuentaBancaria.
 *  Almacena las cuentas bancarias de la empresa.
 *  Es detalle de la entidad Banco.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CNBC", schema = "TSR")
@SequenceGenerator(name = "SQ_CNBCCDGO", sequenceName = "TSR.SQ_BNCOCDGO", allocationSize=1)
@NamedQueries({
    @NamedQuery(name = "CuentaBancariaAll", query = "select e from CuentaBancaria e"),
    @NamedQuery(name = "CuentaBancariaId", query = "select e from CuentaBancaria e where e.codigo = :id")
})
public class CuentaBancaria implements Serializable {
        
    @Basic
    @Id
    @Column(name = "CNBCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CNBCCDGO")
    private Long codigo;    
    
    @ManyToOne
    @JoinColumn(name = "BNCOCDGO", referencedColumnName = "BNCOCDGO")
    private Banco banco;
        
    @Basic
    @Column(name = "CNBCNMRO")
    private String numeroCuenta;    
    
    @Basic
    @Column(name = "CNBCRYYA")
    private Long rubroTipoCuentaP;
    
    @Basic
    @Column(name = "CNBCRZZA")
    private Long rubroTipoCuentaH;    
    
    @Basic
    @Column(name = "CNBCSLIN")
    private Double saldoInicial;
    
    @ManyToOne
    @JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
    private PlanCuenta planCuenta;    
        
    @Basic
    @Column(name = "CNBCFCCR")
    private LocalDateTime fechaCreacion;
    
    @Basic
    @Column(name = "CNBCTTLR")
    private String titular;
        
    @Basic
    @Column(name = "CNBCRYYB")
    private Long rubroTipoMonedaP;    
    
    @Basic
    @Column(name = "CNBCRZZB")
    private Long rubroTipoMonedaH;
        
    @Basic
    @Column(name = "CNBCOFCL")
    private String oficialCuenta;
        
    @Basic
    @Column(name = "CNBCTLF1")
    private String telefono1;
        
    @Basic
    @Column(name = "CNBCTLF2")
    private String telefono2;
        
    @Basic
    @Column(name = "CNBCCLLR")
    private String celular;
        
    @Basic
    @Column(name = "CNBCFXXX")
    private String fax;
        
    @Basic
    @Column(name = "CNBCEMLL")
    private String email;
        
    @Basic
    @Column(name = "CNBCDRCC")
    private String direccion;
        
    @Basic
    @Column(name = "CNBCOBSR")
    private String observacion;    
    
    @Basic
    @Column(name = "CNBCESTD")
    private Long estado;
        
    @Basic
    @Column(name = "CNBCFCIN")
    private LocalDateTime fechaIngreso;
        
    @Basic
    @Column(name = "CNBCFCDS")
    private LocalDateTime fechaInactivo;
    
    @ManyToOne
    @JoinColumn(name = "PLNNORGN", referencedColumnName = "PLNNCDGO")
    private PlanCuenta cuentaApertura;
    
    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Banco getBanco() {
        return banco;
    }

    public void setBanco(Banco banco) {
        this.banco = banco;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }

    public Long getRubroTipoCuentaP() {
        return rubroTipoCuentaP;
    }

    public void setRubroTipoCuentaP(Long rubroTipoCuentaP) {
        this.rubroTipoCuentaP = rubroTipoCuentaP;
    }

    public Long getRubroTipoCuentaH() {
        return rubroTipoCuentaH;
    }

    public void setRubroTipoCuentaH(Long rubroTipoCuentaH) {
        this.rubroTipoCuentaH = rubroTipoCuentaH;
    }

    public Double getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(Double saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public PlanCuenta getPlanCuenta() {
        return planCuenta;
    }

    public void setPlanCuenta(PlanCuenta planCuenta) {
        this.planCuenta = planCuenta;
    }    

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public Long getRubroTipoMonedaP() {
        return rubroTipoMonedaP;
    }

    public void setRubroTipoMonedaP(Long rubroTipoMonedaP) {
        this.rubroTipoMonedaP = rubroTipoMonedaP;
    }

    public Long getRubroTipoMonedaH() {
        return rubroTipoMonedaH;
    }

    public void setRubroTipoMonedaH(Long rubroTipoMonedaH) {
        this.rubroTipoMonedaH = rubroTipoMonedaH;
    }

    public String getOficialCuenta() {
        return oficialCuenta;
    }

    public void setOficialCuenta(String oficialCuenta) {
        this.oficialCuenta = oficialCuenta;
    }

    public String getTelefono1() {
        return telefono1;
    }

    public void setTelefono1(String telefono1) {
        this.telefono1 = telefono1;
    }

    public String getTelefono2() {
        return telefono2;
    }

    public void setTelefono2(String telefono2) {
        this.telefono2 = telefono2;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDateTime fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public LocalDateTime getFechaInactivo() {
        return fechaInactivo;
    }

    public void setFechaInactivo(LocalDateTime fechaInactivo) {
        this.fechaInactivo = fechaInactivo;
    }    
    
    public PlanCuenta getCuentaApertura() {
        return cuentaApertura;
    }

    public void setCuentaApertura(PlanCuenta cuentaApertura) {
        this.cuentaApertura = cuentaApertura;
    }

}

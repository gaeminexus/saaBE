/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.model.cnt.Asiento;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;

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
 * <p>Pojo mapeo de tabla TSR.DPST.
 *  Entity Deposito.
 *  Almacena los depositos realizados por un usuario por caja.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DPST", schema = "TSR")
@SequenceGenerator(name = "SQ_DPSTCDGO", sequenceName = "TSR.SQ_DPSTCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "DepositoAll", query = "select e from Deposito e"),
	@NamedQuery(name = "DepositoId", query = "select e from Deposito e where e.codigo = :id")
})
public class Deposito implements Serializable {

    /**
     * Id.
     */
    @Basic
    @Id
    @Column(name = "DPSTCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DPSTCDGO")
    private Long codigo;
    
    /**
     * Total en efectivo del deposito.
     */
    @Basic
    @Column(name = "DPSTEFCT")
    private Double totalEfectivo;
    
    /**
     * Total en cheque del deposito.
     */
    @Basic
    @Column(name = "DPSTCHQS")
    private Double totalCheque;
    
    /**
     * Total del deposito.
     */
    @Basic
    @Column(name = "DPSTTOTL")
    private Double totalDeposito;
    
    /**
     * Nombre del usuario que realiza el deposito.
     */
    @Basic
    @Column(name = "DPSTUSRO", length = 50)
    private String nombreUsuario;
    
    /**
     * Fecha en la que se realiza el deposito.
     */
    @Basic
    @Column(name = "DPSTFCHA")
    private LocalDateTime fechaDeposito;
    
    /**
     * Usuario que realiza el deposito
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDUS", referencedColumnName = "PJRQCDGO")
    private Usuario usuario;    

    /**
     * Empresa a la que pertenece el deposito
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;    

    /**
     * Estado.
     */
    @Basic
    @Column(name = "DPSTESTD")
    private Long estado;
    
    /**
     * Usuario por caja que realiza el deposito
     */
    @ManyToOne
    @JoinColumn(name = "USXCCDGO", referencedColumnName = "USXCCDGO")
    private UsuarioPorCaja usuarioPorCaja;    
    
    /**
     * Asiento contable ligado a la emision del cheque
     */
    @ManyToOne
    @JoinColumn(name = "ASNTRTCD", referencedColumnName = "ASNTCDGO")
    private Asiento asiento;
    
    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }
    
    public Double getTotalEfectivo() {
        return totalEfectivo;
    }

    public void setTotalEfectivo(Double totalEfectivo) {
        this.totalEfectivo = totalEfectivo;
    }
    
    public Double getTotalCheque() {
        return totalCheque;
    }

    public void setTotalCheque(Double totalCheque) {
        this.totalCheque = totalCheque;
    }
    
    public Double getTotalDeposito() {
        return totalDeposito;
    }

    public void setTotalDeposito(Double totalDeposito) {
        this.totalDeposito = totalDeposito;
    }
    
    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }
    
    public LocalDateTime getFechaDeposito() {
        return fechaDeposito;
    }

    public void setFechaDeposito(LocalDateTime fechaDeposito) {
        this.fechaDeposito = fechaDeposito;
    }
    
    public Usuario getUsuario() {
        return this.usuario;
    }
    
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Empresa getEmpresa() {
        return this.empresa;
    }
    
    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
    
    public UsuarioPorCaja getUsuarioPorCaja() {
        return this.usuarioPorCaja;
    }
    
    public void setUsuarioPorCaja(UsuarioPorCaja usuarioPorCaja) {
        this.usuarioPorCaja = usuarioPorCaja;
    }
    
    public Asiento getAsiento() {
        return asiento;
    }

    public void setAsiento(Asiento asiento) {
        this.asiento = asiento;
    }

}

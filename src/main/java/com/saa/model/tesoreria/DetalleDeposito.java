package com.saa.model.tesoreria;

/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.model.cnt.Asiento;
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
 * <p>Pojo mapeo de tabla TSR.DTDP.
 *  Entity Detalle deposito.
 *  Almacena el detalle de deposito.
 *  Contiene los diferentes bancos a los que se envio el deposito y el desglose de los valores.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DTDP", schema = "TSR")
@SequenceGenerator(name = "SQ_DTDPCDGO", sequenceName = "TSR.SQ_DTDPCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "DetalleDepositoAll", query = "select e from DetalleDeposito e"),
	@NamedQuery(name = "DetalleDepositoId", query = "select e from DetalleDeposito e where e.codigo = :id")
})
public class DetalleDeposito implements Serializable {

    /**
     * Id.
     */
    @Basic
    @Id
    @Column(name = "DTDPCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DTDPCDGO")
    private Long codigo;
    
    /**
     * Deposito al que pertenece el detalle.
     */
    @ManyToOne
    @JoinColumn(name = "DPSTCDGO", referencedColumnName = "DPSTCDGO")
    private Deposito deposito;    

    /**
     * Banco al que se envio parte/total del deposito
     */
    @ManyToOne
    @JoinColumn(name = "BNCOCDGO", referencedColumnName = "BNCOCDGO")
    private Banco banco;    

    /**
     * Cuenta Bancaria a la que se envio parte/total del deposito
     */
    @ManyToOne
    @JoinColumn(name = "CNBCCDGO", referencedColumnName = "CNBCCDGO")
    private CuentaBancaria cuentaBancaria;    

    /**
     * Valor total enviado a esta cuenta bancaria.
     */
    @Basic
    @Column(name = "DTDPVLRR")
    private Double valor;
    
    /**
     * Valor en efectivo enviado a esta cuenta bancaria.
     */
    @Basic
    @Column(name = "DTDPVLEF")
    private Double valorEfectivo;
    
    /**
     * Valor en cheque enviado a esta cuenta bancaria.
     */
    @Basic
    @Column(name = "DTDPVLCH")
    private Double valorCheque;
    
    /**
     * Estado. 0 = Enviado  1 = Ratificado 
     */
    @Basic
    @Column(name = "DTDPESTD")
    private Long estado;
    
    /**
     * Fecha de envio del deposito.
     */
    @Basic
    @Column(name = "DTDPFCEN")
    private LocalDateTime fechaEnvio;
    
    /**
     * Fecha de ratificacion del deposito.
     */
    @Basic
    @Column(name = "DTDPFCRT")
    private LocalDateTime fechaRatificacion;
    
    /**
     * Numero de deposito.
     */
    @Basic
    @Column(name = "DTDPNMDP", length = 500)
    private String numeroDeposito;    
    
    /**
     * Asiento contable ligado a la emision del cheque
     */
    @ManyToOne
    @JoinColumn(name = "ASNTRTFC", referencedColumnName = "ASNTCDGO")
    private Asiento asiento;
    
    /**
     * Usuario que realizo la ratificacion.
     */
    @ManyToOne
    @JoinColumn(name = "USRORTFC", referencedColumnName = "PJRQCDGO")
    private Usuario usuario;    

    /**
     * Nombre del usuario que realizo la ratificacion.
     */
    @Basic
    @Column(name = "USRTNMBR", length = 500)
    private String nombreUsuario;
    
    /**
     * Devuelve codigo
     * @return codigo
     */    
    public Long getCodigo() {
        return codigo;
    }

    /**
     * Asigna codigo
     * @param codigo Nuevo valor para codigo 
     */
    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }
    
    /**
     * Devuelve deposito
     */
    public Deposito getDeposito() {
        return this.deposito;
    }
    
    /**
     * Asigna deposito
     */
    public void setDeposito(Deposito deposito) {
        this.deposito = deposito;
    }

    /**
     * Devuelve banco
     */
    public Banco getBanco() {
        return this.banco;
    }
    
    /**
     * Asigna banco
     */
    public void setBanco(Banco banco) {
        this.banco = banco;
    }

    /**
     * Devuelve cuentaBancaria
     */
    public CuentaBancaria getCuentaBancaria() {
        return this.cuentaBancaria;
    }
    
    /**
     * Asigna cuentaBancaria
     */
    public void setCuentaBancaria(CuentaBancaria cuentaBancaria) {
        this.cuentaBancaria = cuentaBancaria;
    }

    /**
     * Devuelve valor
     * @return valor
     */
    public Double getValor() {
        return valor;
    }

    /**
     * Asigna valor
     * @param valor Nuevo valor para valor 
     */
    public void setValor(Double valor) {
        this.valor = valor;
    }
    
    /**
     * Devuelve valorEfectivo
     * @return valorEfectivo
     */
    public Double getValorEfectivo() {
        return valorEfectivo;
    }

    /**
     * Asigna valorEfectivo
     * @param valorEfectivo Nuevo valor para valorEfectivo 
     */
    public void setValorEfectivo(Double valorEfectivo) {
        this.valorEfectivo = valorEfectivo;
    }
    
    /**
     * Devuelve valorCheque
     * @return valorCheque
     */
    public Double getValorCheque() {
        return valorCheque;
    }

    /**
     * Asigna valorCheque
     * @param valorCheque Nuevo valor para valorCheque 
     */
    public void setValorCheque(Double valorCheque) {
        this.valorCheque = valorCheque;
    }
    
    /**
     * Devuelve estado
     * @return estado
     */
    public Long getEstado() {
        return estado;
    }

    /**
     * Asigna estado
     * @param estado Nuevo valor para estado 
     */
    public void setEstado(Long estado) {
        this.estado = estado;
    }
    
    /**
     * Devuelve fechaEnvio
     * @return fechaEnvio
     */
    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    /**
     * Asigna fechaEnvio
     * @param fechaEnvio Nuevo valor para fechaEnvio 
     */
    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }
    
    /**
     * Devuelve fechaRatificacion
     * @return fechaRatificacion
     */
    public LocalDateTime getFechaRatificacion() {
        return fechaRatificacion;
    }

    /**
     * Asigna fechaRatificacion
     * @param fechaRatificacion Nuevo valor para fechaRatificacion 
     */
    public void setFechaRatificacion(LocalDateTime fechaRatificacion) {
        this.fechaRatificacion = fechaRatificacion;
    }
    
    /**
     * Devuelve numeroDeposito
     * @return numeroDeposito
     */
    public String getNumeroDeposito() {
        return numeroDeposito;
    }

    /**
     * Asigna numeroDeposito
     * @param numeroDeposito Nuevo valor para numeroDeposito 
     */
    public void setNumeroDeposito(String numeroDeposito) {
        this.numeroDeposito = numeroDeposito;
    }
    
    /**
     *Devuelve  asiento
     */
    public Asiento getAsiento() {
        return asiento;
    }

    /**
     * Asigna asiento
     */
    public void setAsiento(Asiento asiento) {
        this.asiento = asiento;
    }
    
    /**
     * Devuelve usuario
     */
    public Usuario getUsuario() {
        return this.usuario;
    }
    
    /**
     * Asigna usuario
     */
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    /**
     * Devuelve nombreUsuario
     * @return nombreUsuario
     */
    public String getNombreUsuario() {
        return nombreUsuario;
    }

    /**
     * Asigna nombreUsuario
     * @param nombreUsuario Nuevo valor para nombreUsuario 
     */
    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }
    
}

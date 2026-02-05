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
 * <p>Pojo mapeo de tabla TSR.APDS.
 * Entity Auxiliar para el proceso de deposito (Desglose).
 *Almacena de forma temporal el desglose de cobros para cada cuenta bancaria.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "APDS", schema = "TSR")
@SequenceGenerator(name = "SQ_APDSCDGO", sequenceName = "TSR.SQ_APDSCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "AuxDepositoDesgloseAll", query = "select e from AuxDepositoDesglose e"),
    @NamedQuery(name = "AuxDepositoDesgloseId", query = "select e from AuxDepositoDesglose e where e.codigo = :id")
})
public class AuxDepositoDesglose implements Serializable {

    @Basic
    @Id
    @Column(name = "APDSCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_APDSCDGO")
    private Long codigo;
    
    @Basic
    @Column(name = "APDSTPOO")
    private Long tipo;
    
    @Basic
    @Column(name = "APDSVLRR")
    private Double valor;
    
    @Basic
    @Column(name = "APDSSLCC")
    private Long seleccionado;
    
    @ManyToOne
    @JoinColumn(name = "CBROCDGO", referencedColumnName = "CBROCDGO")
    private Cobro cobro;
    
    @ManyToOne
    @JoinColumn(name = "BNCOCDGO", referencedColumnName = "BNCOCDGO")
    private Banco banco;
    
    @ManyToOne
    @JoinColumn(name = "CNBCCDGO", referencedColumnName = "CNBCCDGO")
    private CuentaBancaria cuentaBancaria;
    
    @ManyToOne
    @JoinColumn(name = "BEXTCDGO", referencedColumnName = "BEXTCDGO")
    private BancoExterno bancoExterno;
    
    @Basic
    @Column(name = "CCHQNMRO")
    private Long numeroCheque;
    
    @ManyToOne
    @JoinColumn(name = "USXCCDGO", referencedColumnName = "USXCCDGO")
    private UsuarioPorCaja usuarioPorCaja;
    
    @ManyToOne
    @JoinColumn(name = "CCHQCDGO", referencedColumnName = "CCHQCDGO")
    private CobroCheque cobroCheque;

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
	 * Devuelve tipo
	 * @return tipo
	 */
	public Long getTipo() {
		return tipo;
	}

	/**
	 * Asigna tipo
	 * @param tipo Nuevo valor para tipo 
	 */
	public void setTipo(Long tipo) {
		this.tipo = tipo;
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
	 * Devuelve seleccionado
	 * @return seleccionado
	 */
	public Long getSeleccionado() {
		return seleccionado;
	}

	/**
	 * Asigna seleccionado
	 * @param seleccionado Nuevo valor para seleccionado 
	 */
	public void setSeleccionado(Long seleccionado) {
		this.seleccionado = seleccionado;
	}
	
	/**
	 * Devuelve cobroCheque
	 */
	public Cobro getCobro() {
		return cobro;
	}

	/**
	 * Asigna cobroCheque
	 * @param cobro Nuevo valor para cobro 
	 */
	public void setCobro(Cobro cobro) {
		this.cobro = cobro;
	}
	
	/**
	 * Devuelve Banco
	 */
	public Banco getBanco() {
		return banco;
	}

	/**
	 * Asigna Banco
	 * @param banco Nuevo valor para banco 
	 */
	public void setBanco(Banco banco) {
		this.banco = banco;
	}
	
	/**
	 * Devuelve Cuenta Bancaria
	 */
	public CuentaBancaria getCuentaBancaria() {
		return cuentaBancaria;
	}

	/**
	 * Asigna Cuenta Bancaria
	 * @param cuentaBancaria Nuevo valor para cuentaBancaria 
	 */
	public void setCuentaBancaria(CuentaBancaria cuentaBancaria) {
		this.cuentaBancaria = cuentaBancaria;
	}
	
	/**
	 * Devuelve Banco Externo
	 */
	public BancoExterno getBancoExterno() {
		return bancoExterno;
	}

	/**
	 * Asigna Banco Externo
	 * @param bancoExterno Nuevo valor para bancoExterno 
	 */
	public void setBancoExterno(BancoExterno bancoExterno) {
		this.bancoExterno = bancoExterno;
	}
	
	/**
	 * Devuelve numeroCheque
	 * @return numeroCheque
	 */
	public Long getNumeroCheque() {
		return numeroCheque;
	}

	/**
	 * Asigna numeroCheque
	 * @param numeroCheque Nuevo valor para numeroCheque 
	 */
	public void setNumeroCheque(Long numeroCheque) {
		this.numeroCheque = numeroCheque;
	}
	
	/**
	 * Devuelve Usuario por caja
	 */
	public UsuarioPorCaja getUsuarioPorCaja() {
		return usuarioPorCaja;
	}

	/**
	 * Asigna Usuario por caja
	 * @param usuarioPorCaja Nuevo valor para usuarioPorCaja 
	 */
	public void setUsuarioPorCaja(UsuarioPorCaja usuarioPorCaja) {
		this.usuarioPorCaja = usuarioPorCaja;
	}
	
	/**
	 * Devuelve cobroCheque
	 */
	public CobroCheque getCobroCheque() {
		return this.cobroCheque;
	}
	
	/**
	 * Asigna cobroCheque
	 */
	public void setCobroCheque(CobroCheque cobroCheque) {
		this.cobroCheque = cobroCheque;
	}
	
}

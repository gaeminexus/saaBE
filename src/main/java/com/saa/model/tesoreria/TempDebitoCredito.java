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

import com.saa.model.contabilidad.DetallePlantilla;
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
 * <p>Pojo mapeo de tabla TSR.TDBC.
 * Entity Temp Debito Credito.
 * Almacena los debitos creditos de manera temporal para el proceso en pantalla.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TDBC", schema = "TSR")
@SequenceGenerator(name = "SQ_TDBCCDGO", sequenceName = "TSR.SQ_TDBCCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TempDebitoCreditoAll", query = "select e from TempDebitoCredito e"),
	@NamedQuery(name = "TempDebitoCreditoId", query = "select e from TempDebitoCredito e where e.codigo = :id")
})
public class TempDebitoCredito implements Serializable {

	/**
	 * Id.
	 */
	@Basic
	@Id
	@Column(name = "TDBCCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TDBCCDGO")
	private Long codigo;
	
	/**
	 * Tipo de movimiento. 1 = Debito, 2 = Credito
	 */
	@Basic
	@Column(name = "TDBCTPOO")
	private Long tipo;
	
	/**
	 * Usuario que realiza el debito o credito
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDUS", referencedColumnName = "PJRQCDGO")
	private Usuario usuario;	

	/**
	 * Detalle de plantilla
	 */
	@ManyToOne
	@JoinColumn(name = "DTPLCDGO", referencedColumnName = "DTPLCDGO")
	private DetallePlantilla detallePlantilla;	

	/**
	 * Descripcion.
	 */
	@Basic
	@Column(name = "TDBCDSCR", length = 500)
	private String descripcion;
	
	/**
	 * Valor.
	 */
	@Basic
	@Column(name = "TDBCVLRR")
	private Double valor;
	
	/**
	 * Empresa en la que se realiza el debito/credito
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;	

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
	 * Devuelve Usuario
	 */
	public Usuario getUsuario() {
		return this.usuario;
	}
	
	/**
	 * Asigna Usuario
	 */
	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	/**
	 * Devuelve detallePlantilla
	 */
	public DetallePlantilla getDetallePlantilla() {
		return this.detallePlantilla;
	}
	
	/**
	 * Asigna detallePlantilla
	 */
	public void setDetallePlantilla(DetallePlantilla detallePlantilla) {
		this.detallePlantilla = detallePlantilla;
	}

	/**
	 * Devuelve descripcion
	 * @return descripcion
	 */
	public String getDescripcion() {
		return descripcion;
	}

	/**
	 * Asigna descripcion
	 * @param descripcion Nuevo valor para descripcion 
	 */
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
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
	 * Devuelve empresa
	 */
	public Empresa getEmpresa() {
		return this.empresa;
	}
	
	/**
	 * Asigna empresa
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}
}

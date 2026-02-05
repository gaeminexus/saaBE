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
 * <p>Pojo mapeo de tabla TSR.BNCO.
 * Entity Banco.
 * Contiene los bancos en los que la empresa tiene cuentas bancarias.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "BNCO", schema = "TSR")
@SequenceGenerator(name = "SQ_BNCOCDGO", sequenceName = "TSR.SQ_BNCOCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "BancoAll", query = "select e from Banco e"),
	@NamedQuery(name = "BancoId", query = "select e from Banco e where e.codigo = :id")
})
public class Banco implements Serializable {
	
	/**
	 * id de la tabla 
	 */
	@Id
	@Basic
	@Column(name = "BNCOCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_BNCOCDGO")
	private Long codigo;
	
	/**
	 * Nombre del banco
	 */
	@Basic
	@Column(name = "BNCONMBR")
	private String nombre;	
	
	/**
	 * Indica si permite conciliar con descuadre. 0 = No, 1 = Si
	 */
	@Basic
	@Column(name = "BNCOCNDS")
	private Long conciliaDescuadre;	
	
	/**
	 * Estado del banco
	 */
	@Basic
	@Column(name = "BNCOESTD")	
	private Long estado;	
	
	/**
	 * Empresa a la que pertenece el banco
	 */
	@ManyToOne() 
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;	
	
	/**
	 * Rubro 24. Indica el tipo de banco 
	 */
	@Basic
	@Column(name = "BNCORYYA")
	private Long rubroTipoBancoP;	
	
	/**
	 * Detalle de rubro 24. Indica el tipo de banco
	 */
	@Basic
	@Column(name = "BNCORZZA")	
	private Long rubroTipoBancoH;	
	
	/**
	 * Fecha de ingreso del banco
	 */
	@Basic
	@Column(name = "BNCOFCIN")
	private LocalDateTime fechaIngreso;	
	
	/**
	 * Fecha de desactivacion del banco
	 */
	@Basic
	@Column(name = "BNCOFCDS")
	private LocalDateTime fechaInactivo;
	

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
	 * Devuelve nombre
	 * @return nombre
	 */
	public String getNombre() {
		return nombre;
	}

	/**
	 * Asigna nombre
	 * @param nombre nuevo valor para nombre
	 */
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	/**
	 * Devuelve conciliaDescuadre
	 * @return conciliaDescuadre
	 */
	public Long getConciliaDescuadre() {
		return conciliaDescuadre;
	}

	/**
	 * Asigna conciliaDescuadre
	 * @param conciliaDescuadre nuevo valor para concilia descuadre
	 */
	public void setConciliaDescuadre(Long conciliaDescuadre) {
		this.conciliaDescuadre = conciliaDescuadre;
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
	 * @param estado nuevo valor para estado
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
	}

	/**
	 * Devuelve empresa
	 */
	public Empresa getEmpresa() {
		return empresa;
	}

	/**
	 * Asigna empresa
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	/**
	 * Devuelve rubroTipoBancoP
	 * @return rubroTipoBancoP
	 */
	public Long getRubroTipoBancoP() {
		return rubroTipoBancoP;
	}

	/**
	 * Asigna rubroTipoBancoP
	 * @param rubroTipoBancoP nuevo valor para rubro
	 */
	public void setRubroTipoBancoP(Long rubroTipoBancoP) {
		this.rubroTipoBancoP = rubroTipoBancoP;
	}

	/**
	 * Devuelve rubroTipoBancoH 
	 * @return rubroTipoBancoH 
	 */
	public Long getRubroTipoBancoH() {
		return rubroTipoBancoH;
	}

	/**
	 * Asigna rubroTipoBancoH 
	 * @param rubroTipoBancoH nuevo valor para detalle de rubro
	 */
	public void setRubroTipoBancoH(Long rubroTipoBancoH) {
		this.rubroTipoBancoH = rubroTipoBancoH;
	}
	
	/**
	 * Devuelve fechaIngreso
	 * @return fechaIngreso
	 */
	public LocalDateTime getFechaIngreso() {
		return fechaIngreso;
	}

	/**
	 * Asigna fechaIngreso
	 * @param fechaIngreso nuevo valor para fecha de ingreso
	 */
	public void setFechaIngreso(LocalDateTime fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}

	/**
	 * Devuelve fechaInactivo
	 * @return fechaInactivo
	 */
	public LocalDateTime getFechaInactivo() {
		return fechaInactivo;
	}

	/**
	 * Asigna fechaInactivo
	 * @param fechaInactivo nuevo valor para fecha Inactivo
	 */
	public void setFechaInactivo(LocalDateTime fechaInactivo) {
		this.fechaInactivo = fechaInactivo;
	}
	
}

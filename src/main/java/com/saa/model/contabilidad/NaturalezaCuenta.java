package com.saa.model.contabilidad;

import java.io.Serializable;
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

@SuppressWarnings("serial")
@Entity
@Table(name = "NTRL",schema = "CNT")
@SequenceGenerator(name = "SQ_NTRLCDGO", sequenceName = "CNT.SQ_NTRLCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "NaturalezaCuentaAll", query = "select e from NaturalezaCuenta e"),
	@NamedQuery(name = "NaturalezaCuentaId", query = "select e from NaturalezaCuenta e where e.codigo = :id")
})
public class NaturalezaCuenta implements Serializable {
	
	/**
	 * id de la tabla 
	 */
	@Basic
	@Id
	@Column(name = "NTRLCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_NTRLCDGO")
	private Long codigo;
	
	/**
	 * Nombre del Grupo de Cuentas
	 */
	@Basic
	@Column(name = "NTRLNMBR", length = 100)
	private String nombre;
	
	/**
	 * Tipo de Grupo 1 = Acreedora, 2 = Deudora
	 */
	@Basic
	@Column(name = "NTRLTPOO")
	private Long tipo;
	
	/**
	 * Número de Grupo de Cuentas 
	 */
	@Basic
	@Column(name = "NTRLNMRO")
	private Long numero;	
	
	/**
	 * Estado 1 = Activo, 2 = Inactivo
	 */
	@Basic
	@Column(name = "NTRLESTD")
	private Long estado;
	
	/**
	 * Empresa asociada
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;
	
	@Basic
	@Column(name = "NTRLCNCS")
	private Long manejaCentroCosto;
	
	/**
	 * Devuelve codigo
	 * @return codigo
	 */
	public Long getCodigo(){
		return codigo;
	}
	
	/**
	 * Asigna codigo
	 * @param codigo Nuevo Valor para codigo
	 */
	public void setCodigo(Long codigo){
		this.codigo = codigo;
	}
	
	/**
	 * Devuelve nombre
	 * @return nombre
	 */
	public String getNombre(){
		return nombre;
	}
	
	/**
	 * Asigna nombre
	 * @param nombre Nuevo Valor Para nombre
	 */
	public void setNombre(String nombre){
		this.nombre = nombre;
	}
	
	/**
	 * Devuelve tipo
	 * @return tipo
	 */
	public Long getTipo(){
		return tipo;
	}	
	
	/**
	 * Asigna tipo
	 * @param tipo nuevo Valor Para tipo
	 */
	public void  setTipo(Long tipo){
		this.tipo = tipo;
	}
	
	/**
	 * Devuelve numero
	 * @return numero
	 */
	public Long getNumero(){
		return numero;
	}

	/**
	 * Asigna numero
	 * @param numero Nuevo valor Para numero
	 */
	public void setNumero(Long numero){
		this.numero = numero;
	}
	
	/**
	 * Devuelve estado 
	 * @return estado
	 */
	public Long getEstado(){
		return estado;
	}
	
	/**
	 * Asigna estado 
	 * @param estado Nuevo Valor Para estado 
	 */
	public void setEstado(Long estado){
		this.estado = estado;
	}
	
	/**
	 * Devuelve empresa
	 * @return empresa
	 */
	public Empresa getEmpresa(){
		return empresa;
	}
	
	/**
	 * Asigna empresa
	 * @param empresa Nuevo Valor Para empresa 
	 */
	public void setEmpresa(Empresa empresa){
		this.empresa = empresa;
	}
	
	/**
	 * Devuelve manejaCentroCosto
	 * @return manejaCentroCosto
	 */
	public Long getManejaCentroCosto(){
		return manejaCentroCosto;
	}
	
	/**
	 * Asigna manejaCentroCosto
	 * @param manejaCentroCosto Nuevo valor Para manejaCentroCosto
	 */
	public void setManejaCentroCosto(Long manejaCentroCosto){
		this.manejaCentroCosto = manejaCentroCosto;
	}
   
}

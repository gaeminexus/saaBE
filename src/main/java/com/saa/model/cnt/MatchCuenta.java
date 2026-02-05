package com.saa.model.cnt;

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
@Table(name = "MTCH", schema = "CNT")
@SequenceGenerator(name = "SQ_MTCHCDGO", sequenceName = "CNT.SQ_MTCHCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "MatchCuentaAll", query = "select e from MatchCuenta e"),
	@NamedQuery(name = "MatchCuentaId", query = "select e from MatchCuenta e where e.codigo = :id")
})
public class MatchCuenta implements Serializable {

	/**
	 * id de la tabla.
	 */
	@Basic
	@Id
	@Column(name = "MTCHCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_MTCHCDGO")
	private Long codigo;
	
	/**
	 * id del nivel de empresa origen tomado de pjrq con pgspcdgo 12.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDOR", referencedColumnName = "PJRQCDGO")
	private Empresa empresaOrigen;
	
	/**
	 * id de la cuenta contable origen.
	 */
	@ManyToOne
	@JoinColumn(name = "PLNNCDOR", referencedColumnName = "PLNNCDGO")
	private PlanCuenta cuentaOrigen;	

	/**
	 * id del nivel de empresa final tomado de pjrq con pgspcdgo 12.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDFN", referencedColumnName = "PJRQCDGO")
	private Empresa empresaDestino;
	
	/**
	 * id de la cuenta contable final.
	 */
	@ManyToOne
	@JoinColumn(name = "PLNNCDFN", referencedColumnName = "PLNNCDGO")
	private PlanCuenta cuentaDestino;	

	/**
	 * estado 1 = activo, 2 = inactivo.
	 */
	@Basic
	@Column(name = "MTCHESTD")
	private Long estado;
	
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
	 * Devuelve empresaOrigen
	 * @return empresaOrigen
	 */
	public Empresa getEmpresaOrigen() {
		return empresaOrigen;
	}

	/**
	 * Asigna empresaOrigen nuevo valor para empresaOrigen 
	 */
	public void setEmpresaOrigen(Empresa empresaOrigen) {
		this.empresaOrigen = empresaOrigen;
	}
	
	/**
	 * Devuelve cuentaOrigen
	 */
	public PlanCuenta getCuentaOrigen() {
		return this.cuentaOrigen;
	}
	
	/**
	 * Asigna cuentaOrigen
	 */
	public void setCuentaOrigen(PlanCuenta cuentaOrigen) {
		this.cuentaOrigen = cuentaOrigen;
	}

	/**
	 * Devuelve empresaDestino
	 * @return empresaDestino
	 */
	public Empresa getEmpresaDestino() {
		return empresaDestino;
	}

	/**
	 * Asigna empresaDestino
	 * @param empresaDestino nuevo valor para empresaDestino 
	 */
	public void setEmpresaDestino(Empresa empresaDestino) {
		this.empresaDestino = empresaDestino;
	}
	
	/**
	 * Devuelve cuentaDestino
	 */
	public PlanCuenta getCuentaDestino() {
		return this.cuentaDestino;
	}
	
	/**
	 * Asigna cuentaDestino
	 */
	public void setCuentaDestino(PlanCuenta cuentaDestino) {
		this.cuentaDestino = cuentaDestino;
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
} 

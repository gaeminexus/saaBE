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

import com.saa.model.contabilidad.Asiento;
import com.saa.model.contabilidad.Periodo;
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
 * <p>Pojo mapeo de tabla TSR.MVCB.
 * Entity Movimiento Bancos.
 * Almacena todos los movimientos realizados sobre una cuenta bancaria.
 * Esta es la base de la conciliacion bancaria.
 * Cada movimiento genera un asiento contable.
 * Cualquier movimiento desde cualquier modulo en una cuenta bancaria se refleja en esta entidad.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "MVCB", schema = "TSR")
@SequenceGenerator(name = "SQ_MVCBCDGO", sequenceName = "TSR.SQ_MVCBCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "MovimientoBancoAll", query = "select e from MovimientoBanco e"),
	@NamedQuery(name = "MovimientoBancoId", query = "select e from MovimientoBanco e where e.codigo = :id")
})
public class MovimientoBanco implements Serializable {

	/**
	 * Id.
	 */
	@Basic
	@Id
	@Column(name = "MVCBCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_MVCBCDGO")
	private Long codigo;
	
	/**
	 * Empresa a la que pertenecen los movimientos
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;	

	/**
	 * Descripcion del movimiento.
	 */
	@Basic
	@Column(name = "MVCBDSCR", length = 200)
	private String descripcion;
	
	/**
	 * Asiento contable relacionado con el movimiento
	 */
	@ManyToOne
	@JoinColumn(name = "ASNTCDGO", referencedColumnName = "ASNTCDGO")
	private Asiento asiento;	

	/**
	 * Valor del movimiento.
	 */
	@Basic
	@Column(name = "MVCBVLRR")
	private Double valor;
	
	/**
	 *  Indica si ha sido o no conciliado este movimiento. 1 = Si, 0 = No.
	 */
	@Basic
	@Column(name = "MVCBCNCL")
	private Long conciliado;
	
	/**
	 * Fecha conciliacion.
	 */
	@Basic
	@Column(name = "MVCBFCCN")
	private LocalDateTime fechaConciliacion;
	
	/**
	 * Numero de cheque.
	 */
	@Basic
	@Column(name = "MVCBCHQN")
	private Long numeroCheque;
	
	/**
	 * Conciliaicion en la que se encuentra el movimiento bancario.
	 */
	@ManyToOne
	@JoinColumn(name = "CNCLCDGO", referencedColumnName = "CNCLCDGO")
	private Conciliacion conciliacion;

	/**
	 * Rubro 37. Tipo de movimiento de conciliacion.
	 */
	@Basic
	@Column(name = "MVCBRYYA")
	private Long rubroTipoMovimientoP;
	
	/**
	 * Detalle de rubro 37. Tipo de movimiento de conciliacion.
	 */
	@Basic
	@Column(name = "MVCBRZZA")
	private Long rubroTipoMovimientoH;
	
	/**
	 * Fecha en la que se registro el movimiento.
	 */
	@Basic
	@Column(name = "MVCBFRGS")
	private LocalDateTime fechaRegistro;
	
	/**
	 * Numero de asiento relacionado con el movimiento.
	 */
	@Basic
	@Column(name = "MVCBNMAS")
	private Long numeroAsiento;
	
	/**
	 * Id del movimiento dependiendo del tipo. Ej: Ingreso, egreso, etc.
	 */
	@Basic
	@Column(name = "MVCBIDMV")
	private Long idMovimiento;
	
	/**
	 * Estado del movimiento. 0 = Anulado, 1 = Activo 
	 */
	@Basic
	@Column(name = "MVCBESTD")
	private Long estado;
	
	/**
	 * Cheque con que se realizo el pago para el caso de egresos
	 */
	@ManyToOne
	@JoinColumn(name = "DTCHCDGO", referencedColumnName = "DTCHCDGO")
	private Cheque cheque;	

	/**
	 * Cuenta bancaria en la que se realiza el movimiento
	 */
	@ManyToOne
	@JoinColumn(name = "CNBCCDGO", referencedColumnName = "CNBCCDGO")
	private CuentaBancaria cuentaBancaria;	

	/**
	 * Detalle del deposito que genero el movimiento. Para el caso de cobros
	 */
	@ManyToOne
	@JoinColumn(name = "DTDPCDGO", referencedColumnName = "DTDPCDGO")
	private DetalleDeposito detalleDeposito;	

	/**
	 * Periodo en el que se realiza el movimiento
	 */
	@ManyToOne
	@JoinColumn(name = "PRDOCDGO", referencedColumnName = "PRDOCDGO")
	private Periodo periodo;	

	/**
	 * Numero de mes en el que se realiza el movimiento.
	 */
	@Basic
	@Column(name = "MVCBMSSS")
	private Long numeroMes;
	
	/**
	 * Numero de año en el que se realiza el movimiento.
	 */
	@Basic
	@Column(name = "MVCBANOO")
	private Long numeroAnio;
	
	/**
	 * Rubro 41. Tipo de movimiento de conciliacion.
	 */
	@Basic
	@Column(name = "MVCBRYYB")
	private Long rubroOrigenP;
	
	/**
	 * Detalle de rubro 41. Tipo de movimiento de conciliacion.
	 */
	@Basic
	@Column(name = "MVCBRZZB")
	private Long rubroOrigenH;
	
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
	 * Devuelve asiento
	 */
	public Asiento getAsiento() {
		return this.asiento;
	}
	
	/**
	 * Asigna asiento
	 */
	public void setAsiento(Asiento asiento) {
		this.asiento = asiento;
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
	 * Devuelve conciliado
	 * @return conciliado
	 */
	public Long getConciliado() {
		return conciliado;
	}

	/**
	 * Asigna conciliado
	 * @param conciliado Nuevo valor para conciliado 
	 */
	public void setConciliado(Long conciliado) {
		this.conciliado = conciliado;
	}
	
	/**
	 * Devuelve fechaConciliacion
	 * @return fechaConciliacion
	 */
	public LocalDateTime getFechaConciliacion() {
		return fechaConciliacion;
	}

	/**
	 * Asigna fechaConciliacion
	 * @param fechaConciliacion Nuevo valor para fechaConciliacion 
	 */
	public void setFechaConciliacion(LocalDateTime fechaConciliacion) {
		this.fechaConciliacion = fechaConciliacion;
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
	 * Devuelve conciliacion
	 */
	public Conciliacion getConciliacion() {
		return conciliacion;
	}

	/**
	 * Asigna conciliacion
	 */
	public void setConciliacion(Conciliacion conciliacion) {
		this.conciliacion = conciliacion;
	}

	/**
	 * Devuelve rubroTipoMovimientoP
	 * @return rubroTipoMovimientoP
	 */
	public Long getRubroTipoMovimientoP() {
		return rubroTipoMovimientoP;
	}

	/**
	 * Asigna rubroTipoMovimientoP
	 * @param rubroTipoMovimientoP Nuevo valor para rubroTipoMovimientoP 
	 */
	public void setRubroTipoMovimientoP(Long rubroTipoMovimientoP) {
		this.rubroTipoMovimientoP = rubroTipoMovimientoP;
	}
	
	/**
	 * Devuelve rubroTipoMovimientoH
	 * @return rubroTipoMovimientoH
	 */
	public Long getRubroTipoMovimientoH() {
		return rubroTipoMovimientoH;
	}

	/**
	 * Asigna rubroTipoMovimientoH
	 * @param rubroTipoMovimientoH Nuevo valor para rubroTipoMovimientoH 
	 */
	public void setRubroTipoMovimientoH(Long rubroTipoMovimientoH) {
		this.rubroTipoMovimientoH = rubroTipoMovimientoH;
	}
	
	/**
	 * Devuelve fechaRegistro
	 * @return fechaRegistro
	 */
	public LocalDateTime getFechaRegistro() {
		return fechaRegistro;
	}

	/**
	 * Asigna fechaRegistro
	 * @param fechaRegistro Nuevo valor para fechaRegistro 
	 */
	public void setFechaRegistro(LocalDateTime fechaRegistro) {
		this.fechaRegistro = fechaRegistro;
	}
	
	/**
	 * Devuelve numeroAsiento
	 * @return numeroAsiento
	 */
	public Long getNumeroAsiento() {
		return numeroAsiento;
	}

	/**
	 * Asigna numeroAsiento
	 * @param numeroAsiento Nuevo valor para numeroAsiento 
	 */
	public void setNumeroAsiento(Long numeroAsiento) {
		this.numeroAsiento = numeroAsiento;
	}
	
	/**
	 * Devuelve idMovimiento
	 * @return idMovimiento
	 */
	public Long getIdMovimiento() {
		return idMovimiento;
	}

	/**
	 * Asigna idMovimiento
	 * @param idMovimiento Nuevo valor para idMovimiento 
	 */
	public void setIdMovimiento(Long idMovimiento) {
		this.idMovimiento = idMovimiento;
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
	 *Devuelve cheque
	 */
	public Cheque getCheque() {
		return this.cheque;
	}
	
	/**
	 * Asigna cheque
	 */
	public void setCheque(Cheque cheque) {
		this.cheque = cheque;
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
	 * Devuelve detalleDeposito
	 */
	public DetalleDeposito getDetalleDeposito() {
		return this.detalleDeposito;
	}
	
	/**
	 * Asigna detalleDeposito
	 */
	public void setDetalleDeposito(DetalleDeposito detalleDeposito) {
		this.detalleDeposito = detalleDeposito;
	}

	/**
	 * Devuelve periodo
	 */
	public Periodo getPeriodo() {
		return this.periodo;
	}
	
	/**
	 * Asigna periodo
	 */
	public void setPeriodo(Periodo periodo) {
		this.periodo = periodo;
	}

	/**
	 * Devuelve numeroMes
	 * @return numeroMes
	 */
	public Long getNumeroMes() {
		return numeroMes;
	}

	/**
	 * Asigna numeroMes
	 * @param numeroMes Nuevo valor para numeroMes 
	 */
	public void setNumeroMes(Long numeroMes) {
		this.numeroMes = numeroMes;
	}
	
	/**
	 * Devuelve numeroAnio
	 * @return numeroAnio
	 */
	public Long getNumeroAnio() {
		return numeroAnio;
	}

	/**
	 * Asigna numeroAnio
	 * @param numeroAnio Nuevo valor para numeroAnio 
	 */
	public void setNumeroAnio(Long numeroAnio) {
		this.numeroAnio = numeroAnio;
	}
	
	/**
	 * Devuelve rubroOrigenP
	 * @return rubroOrigenP
	 */
	public Long getRubroOrigenP() {
		return rubroOrigenP;
	}

	/**
	 * Asigna rubroOrigenP
	 * @param rubroOrigenP Nuevo valor para rubroOrigenP 
	 */
	public void setRubroOrigenP(Long rubroOrigenP) {
		this.rubroOrigenP = rubroOrigenP;
	}
	
	/**
	 * Devuelve rubroOrigenH
	 * @return rubroOrigenH
	 */
	public Long getRubroOrigenH() {
		return rubroOrigenH;
	}

	/**
	 * Asigna rubroOrigenH
	 * @param rubroOrigenH Nuevo valor para rubroOrigenH 
	 */
	public void setRubroOrigenH(Long rubroOrigenH) {
		this.rubroOrigenH = rubroOrigenH;
	}
	
}
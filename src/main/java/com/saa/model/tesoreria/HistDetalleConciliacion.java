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
import java.time.LocalDate;

import com.saa.model.contabilidad.Asiento;
import com.saa.model.contabilidad.Periodo;

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
 * <p>Pojo mapeo de tabla TSR.DTCH.
 * Entity Detalle conciliacion historica.
 * Almacena el detalle de conciliacion de una cuenta bancaria cada vez que se desconcilia.
 * Se genera un grupo de registros por cada desconciliacion en cada cuenta por periodo.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DCHI", schema = "TSR")
@SequenceGenerator(name = "SQ_DCHICDGO", sequenceName = "TSR.SQ_DCHICDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "HistDetalleConciliacionAll", query = "select e from HistDetalleConciliacion e"),
	@NamedQuery(name = "HistDetalleConciliacionId", query = "select e from HistDetalleConciliacion e where e.codigo = :id")
})
public class HistDetalleConciliacion implements Serializable {

	/**
	 * Id.
	 */
	@Basic
	@Id
	@Column(name = "DCHICDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DCHICDGO")
	private Long codigo;
	
	/**
	 * Conciliacio historica a la que pertenece el registro.
	 */
	@ManyToOne
	@JoinColumn(name = "CNCHCDGO", referencedColumnName = "CNCHCDGO")
	private HistConciliacion histConciliacion;	

	/**
	 * Rubro 37. Tipo de movimiento de conciliacion.
	 */
	@Basic
	@Column(name = "DCHIRYYA")
	private Long rubroTipoMovimientoP;
	
	/**
	 * Detalle de rubro 37. Tipo de movimiento de conciliacion.
	 */
	@Basic
	@Column(name = "DCHIRZZA")
	private Long rubroTipoMovimientoH;
	
	/**
	 * Asiento contable relacionado con el movimiento.
	 */
	@ManyToOne
	@JoinColumn(name = "ASNTCDGO", referencedColumnName = "ASNTCDGO")
	private Asiento asiento;	

	/**
	 * Valor.
	 */
	@Basic
	@Column(name = "DCHIVLRR")
	private Double valor;
	
	/**
	 * Indica si ha sido o no conciliado este movimiento. 0 = Si, 1 = No.
	 */
	@Basic
	@Column(name = "DCHICNCL")
	private Long conciliado;
	
	/**
	 * Numero de cheque.
	 */
	@Basic
	@Column(name = "DCHICHQN", length = 50)
	private String numeroCheque;
	
	/**
	 * Rubro 41. Tipo de movimiento de conciliacion.
	 */
	@Basic
	@Column(name = "DCHIRYYB")
	private Long rubroOrigenP;
	
	/**
	 * Detalle de rubro 41. Tipo de movimiento de conciliacion.
	 */
	@Basic
	@Column(name = "DCHIRZZB")
	private Long rubroOrigenH;
	
	/**
	 * Estado del movimiento bancario (o del asiento): 1 = Activo, 2 = Anulado 
	 */
	@Basic
	@Column(name = "DCHIESTD")
	private Long estado;
	
	/**
	 * Numero de asiento.
	 */
	@Basic
	@Column(name = "DCHINMAS")
	private Long numeroAsiento;
	
	/**
	 * Descripcion.
	 */
	@Basic
	@Column(name = "DCHIDSCR", length = 200)
	private String descripcion;
	
	/**
	 * Fecha de registro.
	 */
	@Basic
	@Column(name = "DCHIFRGS")
	private LocalDate fechaRegistro;
	
	/**
	 * Id del movimiento dependiendo del tipo. Ej: Ingreso, egreso, etc.
	 */
	@Basic
	@Column(name = "DCHIIDMV")
	private Long idMovimiento;
	
	/**
	 * Cheque con que se realizo el pago para el caso de egresos.
	 */
	@ManyToOne
	@JoinColumn(name = "DTCHCDGO", referencedColumnName = "DTCHCDGO")
	private Cheque cheque;	

	/**
	 * Detalle del deposito que genero el movimiento. Para el caso de cobros.
	 */
	@ManyToOne
	@JoinColumn(name = "DTDPCDGO", referencedColumnName = "DTDPCDGO")
	private DetalleDeposito detalleDeposito;	

	/**
	 * Periodo en el que se realiza la conciliacion.
	 */
	@ManyToOne
	@JoinColumn(name = "PRDOCDGO", referencedColumnName = "PRDOCDGO")
	private Periodo periodo;	

	/**
	 * Numero de mes en el que se realiza el movimiento.
	 */
	@Basic
	@Column(name = "DCHIMSSS")
	private Long numeroMes;
	
	/**
	 * Numero de año en el que se realiza el movimiento.
	 */
	@Basic
	@Column(name = "DCHIANOO")
	private Long numeroAnio;	
	
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
	 * Devuelve histConciliacion
	 */
	public HistConciliacion getHistConciliacion() {
		return this.histConciliacion;
	}
	
	/**
	 * Asigna histConciliacion
	 */
	public void setHistConciliacion(HistConciliacion histConciliacion) {
		this.histConciliacion = histConciliacion;
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
	 * Devuelve numeroCheque
	 * @return numeroCheque
	 */
	public String getNumeroCheque() {
		return numeroCheque;
	}

	/**
	 * Asigna numeroCheque
	 * @param numeroCheque Nuevo valor para numeroCheque 
	 */
	public void setNumeroCheque(String numeroCheque) {
		this.numeroCheque = numeroCheque;
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
	 * Devuelve fechaRegistro
	 * @return fechaRegistro
	 */
	public LocalDate getFechaRegistro() {
		return fechaRegistro;
	}

	/**
	 * Asigna fechaRegistro
	 * @param fechaRegistro Nuevo valor para fechaRegistro 
	 */
	public void setFechaRegistro(LocalDate fechaRegistro) {
		this.fechaRegistro = fechaRegistro;
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
	 * Devuelve cheque
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
	
}

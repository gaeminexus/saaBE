package com.saa.model.contabilidad;

import java.io.Serializable;
import java.time.LocalDate;
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

@SuppressWarnings("serial")
@Entity
@Table(name = "ASNH", schema = "CNT")
@SequenceGenerator(name = "SQ_ASNHCDGO", sequenceName = "CNT.SQ_ASNHCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "HistAsientoAll", query = "select e from HistAsiento e"),
	@NamedQuery(name = "HistAsientoId", query = "select e from HistAsiento e where e.codigo = :id")
})
public class HistAsiento implements Serializable {

	/**
	 * Id de la tabla. codigo.
	 */
	@Basic
	@Id
	@Column(name = "ASNHCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_ASNHCDGO")	
	private Long codigo;
	
	/**
	 * Id de nivel de empresa.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;
	
	/**
	 * Id de tipo de asiento.
	 */
	@ManyToOne
	@JoinColumn(name = "PLNTCDGO", referencedColumnName = "PLNTCDGO")
	private TipoAsiento tipoAsiento;	

	/**
	 * Fecha del asiento.
	 */
	@Basic
	@Column(name = "ASNHFCHA")
	private LocalDate fechaAsiento;
	
	/**
	 * Número de asiento contable.
	 */
	@Basic
	@Column(name = "ASNHNMRO")
	private Long numero;
	
	/**
	 * Estado del asiento 1 = activo, 2 = anulado, 3 = reversado, 4 = incompleto.
	 */
	@Basic
	@Column(name = "ASNHESTD")
	private Long estado;
	
	/**
	 * Observaciones de asiento contable.
	 */
	@Basic
	@Column(name = "ASNHOBSR", length = 200)
	private String observaciones;
	
	/**
	 * Usuario que genero el asiento.
	 */
	@Basic
	@Column(name = "ASNHUSRO", length = 50)
	private String nombreUsuario;
	
	/**
	 * Id del asiento con el que se reversó el asiento.
	 */
	@Basic
	@Column(name = "ASNHASRV")
	private Long idReversion;
	
	/**
	 * Numero de periodo.
	 */
	@Basic
	@Column(name = "ASNHPRDO")
	private Long numeroMes;
	
	/**
	 * Número de año.
	 */
	@Basic
	@Column(name = "ASNHANOO")
	private Long numeroAnio;
	
	/**
	 * Id de moneda.
	 */
	@Basic
	@Column(name = "ASNHMNDA")
	private Long moneda;
	
	/**
	 * Mayorizacion en la que se incluye el asiento.
	 */
	@ManyToOne
	@JoinColumn(name = "MYRHCDGO", referencedColumnName = "MYRHCDGO")
	private HistMayorizacion histMayorizacion;
	
	/**
	 * Rubro de Modulos del sistema (15).
	 */
	@Basic
	@Column(name = "ASNHRYYA")
	private Long rubroModuloClienteP;
	
	/**
	 * Detalle de rubro de Modulo del sistema.
	 */
	@Basic
	@Column(name = "ASNHRZZA")
	private Long rubroModuloClienteH;
	
	/**
	 * Fecha de proceso.
	 */
	@Basic
	@Column(name = "ASNHFCPR")
	private LocalDateTime fechaIngreso;	
	
	/**
	 * rubro de modulo del sistema para almacenar internamente el modulo desde el que se genera.
	 */
	@Basic
	@Column(name = "ASNHRYYB")
	private Long rubroModuloSistemaP;
	
	/**
	 * detalle de rubro de modulo del sistema para almacenar internamente el modulo desde el que se genera. rubroModuloSistemaH.
	 */
	@Basic
	@Column(name = "ASNHRZZB")
	private Long rubroModuloSistemaH;
	
	/**
	 * Id del asiento que genero el respaldo
	 */
	@Basic
	@Column(name = "ASNHASNT")
	private Long idAsientoOriginal;
	
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
	 * Devuelve empresa
	 * @return empresa
	 */
	public Empresa getEmpresa() {
		return empresa;
	}

	/**
	 * Asigna empresa
	 * @param empresa nuevo valor para empresa 
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}
	
	/**
	 * Devuelve tipoAsiento
	 */
	public TipoAsiento getTipoAsiento() {
		return this.tipoAsiento;
	}
	
	/**
	 * Asigna tipoAsiento
	 */
	public void setTipoAsiento(TipoAsiento tipoAsiento) {
		this.tipoAsiento = tipoAsiento;
	}

	/**
	 * Devuelve fechaAsiento
	 * @return fechaAsiento
	 */
	public LocalDate getFechaAsiento() {
		return fechaAsiento;
	}

	/**
	 * Asigna fechaAsiento
	 * @param fechaAsiento nuevo valor para fechaAsiento 
	 */
	public void setFechaAsiento(LocalDate fechaAsiento) {
		this.fechaAsiento = fechaAsiento;
	}
	
	/**
	 * Devuelve numero
	 * @return numero
	 */
	public Long getNumero() {
		return numero;
	}

	/**
	 * Asigna numero
	 * @param numero nuevo valor para numero 
	 */
	public void setNumero(Long numero) {
		this.numero = numero;
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
	 * Devuelve observaciones
	 * @return observaciones
	 */
	public String getObservaciones() {
		return observaciones;
	}

	/**
	 * Asigna observaciones
	 * @param observaciones nuevo valor para observaciones 
	 */
	public void setObservaciones(String observaciones) {
		this.observaciones = observaciones;
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
	 * @param nombreUsuario nuevo valor para nombreUsuario 
	 */
	public void setNombreUsuario(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario;
	}
	
	/**
	 * Devuelve idReversion
	 * @return idReversion
	 */
	public Long getIdReversion() {
		return idReversion;
	}

	/**
	 * Asigna idReversion
	 * @param idReversion nuevo valor para idReversion 
	 */
	public void setIdReversion(Long idReversion) {
		this.idReversion = idReversion;
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
	 * @param numeroMes nuevo valor para numeroMes 
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
	 * @param numeroAnio nuevo valor para numeroAnio 
	 */
	public void setNumeroAnio(Long numeroAnio) {
		this.numeroAnio = numeroAnio;
	}
	
	/**
	 * Devuelve moneda
	 * @return moneda
	 */
	public Long getMoneda() {
		return moneda;
	}

	/**
	 * Asigna moneda
	 * @param moneda nuevo valor para moneda 
	 */
	public void setMoneda(Long moneda) {
		this.moneda = moneda;
	}
	
	/**
	 * Devuelve mayorizacion
	 * @return mayorizacion
	 */
	public HistMayorizacion getHistMayorizacion() {
		return histMayorizacion;
	}

	/**
	 * Asigna mayorizacion
	 * @param mayorizacion nuevo valor para mayorizacion 
	 */
	public void setHistMayorizacion(HistMayorizacion histMayorizacion) {
		this.histMayorizacion = histMayorizacion;
	}
	
	/**
	 * Devuelve rubroModuloClienteP
	 * @return rubroModuloClienteP
	 */
	public Long getRubroModuloClienteP() {
		return rubroModuloClienteP;
	}

	/**
	 * Asigna rubroModuloClienteP
	 * @param rubroModuloClienteP nuevo valor para rubroModuloClienteP 
	 */
	public void setRubroModuloClienteP(Long rubroModuloClienteP) {
		this.rubroModuloClienteP = rubroModuloClienteP;
	}
	
	/**
	 * Devuelve rubroModuloClienteH
	 * @return rubroModuloClienteH
	 */
	public Long getRubroModuloClienteH() {
		return rubroModuloClienteH;
	}

	/**
	 * Asigna rubroModuloClienteH
	 * @param rubroModuloClienteH nuevo valor para rubroModuloClienteH 
	 */
	public void setRubroModuloClienteH(Long rubroModuloClienteH) {
		this.rubroModuloClienteH = rubroModuloClienteH;
	}
	
	/**
	 * Devuelve fecheIngreso
	 * @return fecheIngreso
	 */
	public LocalDateTime getFechaIngreso() {
		return fechaIngreso;
	}

	/**
	 * Asigna fechaIngreso
	 * @param fechaIngreso nuevo valor para fecheIngreso 
	 */
	public void setFechaIngreso(LocalDateTime fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}
	
	/**
	 * Devuelve rubroModuloSistemaP
	 * @return rubroModuloSistemaP.
	 */
	public Long getRubroModuloSistemaP() {
		return rubroModuloSistemaP;
	}

	/**
	 * Asigna rubroModuloSistemaP
	 * @param rubroModuloSistemaP nuevo valor para rubroModuloSistemaP. 
	 */
	public void setRubroModuloSistemaP(Long rubroModuloSistemaP) {
		this.rubroModuloSistemaP = rubroModuloSistemaP;
	}
	
	/**
	 * Devuelve rubroModuloSistemaH
	 * @return rubroModuloSistemaH.
	 */
	public Long getRubroModuloSistemaH() {
		return rubroModuloSistemaH;
	}

	/**
	 * Asigna rubroModuloSistemaH
	 * @param rubroModuloSistemaH nuevo valor para rubroModuloSistemaH.
	 */
	public void setRubroModuloSistemaH(Long rubroModuloSistemaH) {
		this.rubroModuloSistemaH = rubroModuloSistemaH;
	}
	
	/**
	 * Devuelve idAsientoOriginal
	 * @return idAsientoOriginal
	 */
	public Long getIdAsientoOriginal() {
		return idAsientoOriginal;
	}

	/**
	 * Asigna idAsientoOriginal
	 * @param idAsientoOriginal nuevo valor para idAsientoOriginal
	 */
	public void setIdAsientoOriginal(Long idAsientoOriginal) {
		this.idAsientoOriginal = idAsientoOriginal;
	}

}

package com.saa.model.cnt;

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
@Table(name = "ASNT", schema = "CNT")
@SequenceGenerator(name = "SQ_ASNTCDGO", sequenceName = "CNT.SQ_ASNTCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "AsientoAll", query = "select e from Asiento e"),
	@NamedQuery(name = "AsientoId", query = "select e from Asiento e where e.codigo = :id")
})
public class Asiento implements Serializable {

	/**
	 * id de la tabla.
	 */
	@Basic
	@Id
	@Column(name = "ASNTCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_ASNTCDGO")
	private Long codigo;
	
	/**
	 * id de nivel de empresa.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;
	
	/**
	 * id de tipo de asiento.
	 */
	@ManyToOne
	@JoinColumn(name = "PLNTCDGO", referencedColumnName = "PLNTCDGO")
	 private TipoAsiento tipoAsiento;	

	/**
	 * fecha del asiento.
	 */
	@Basic
	@Column(name = "ASNTFCHA")
	private LocalDate fechaAsiento;
	
	/**
	 * número de asiento contable.
	 */
	@Basic
	@Column(name = "ASNTNMRO")
	private Long numero;
	
	/**
	 * estado del asiento 1 = activo, 2 = anulado, 3 = reversado, 4 = incompleto.
	 */
	@Basic
	@Column(name = "ASNTESTD")
	private Long estado;
	
	/**
	 * observaciones de asiento contable.
	 */
	@Basic
	@Column(name = "ASNTOBSR", length = 200)
	private String observaciones;
	
	/**
	 * usuario que genero el asiento.
	 */
	@Basic
	@Column(name = "ASNTUSRO", length = 50)
	private String nombreUsuario;
	
	/**
	 * id del asiento con el que se reversó el asiento.
	 */
	@Basic
	@Column(name = "ASNTASRV")
	private Long idReversion;
	
	/**
	 * numero de periodo.
	 */
	@Basic
	@Column(name = "ASNTPRDO")
	private Long numeroMes;
	
	/**
	 * número de año.
	 */
	@Basic
	@Column(name = "ASNTANOO")
	private Long numeroAnio;
	
	/**
	 * id de moneda moneda.
	 */
	@Basic
	@Column(name = "ASNTMNDA")
	private Long moneda;
	
	/**
	 * Mayorizacion myrzcdgo.
	 */
	@ManyToOne
	@JoinColumn(name = "MYRZCDGO", referencedColumnName = "MYRZCDGO")
	private Mayorizacion mayorizacion;	 
	
	/**
	 * rubro de modulos del sistema (15).
	 */
	@Basic
	@Column(name = "ASNTRYYA")
	private Long rubroModuloClienteP;
	
	/**
	 * detalle de rubro de modulo del sistema. rubroModuloClienteH.
	 */
	@Basic
	@Column(name = "ASNTRZZA")
	private Long rubroModuloClienteH;
	
	/**
	 * fecha en que se ingreso el registro.
	 */
	@Basic
	@Column(name = "ASNTFCPR")
	private LocalDateTime fechaIngreso;
	
	/**
	 * id del periodo, clave foranea a prdocdgo de la tabla periodo.
	 */
	@ManyToOne
	@JoinColumn(name = "PRDOCDGO", referencedColumnName = "PRDOCDGO")
	private Periodo periodo;	

	/**
	 * rubro de modulo del sistema para almacenar internamente el modulo desde el que se genera.
	 */
	@Basic
	@Column(name = "ASNTRYYB")
	private Long rubroModuloSistemaP;
	
	/**
	 * detalle de rubro de modulo del sistema para almacenar internamente el modulo desde el que se genera. rubroModuloSistemaH.
	 */
	@Basic
	@Column(name = "ASNTRZZB")
	private Long rubroModuloSistemaH;
	
	/**
	 * Devuelve codigo
	 * @return codigo.
	 */
	public Long getCodigo() {
		return codigo;
	}

	/**
	 * Asigna codigo
	 * @param codigo nuevo valor para codigo. 
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}
	
	/**
	 * Devuelve empresa
	 * @return empresa.
	 */
	public Empresa getEmpresa() {
		return empresa;
	}

	/**
	 * Asigna empresa
	 * @param empresa nuevo valor para empresa. 
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}
	
	/**
	 * Devuelve tipoAsiento.
	 */
	public TipoAsiento getTipoAsiento() {
		return this.tipoAsiento;
	}
	
	/**
	 * Asigna tipoAsiento.
	 */
	public void setTipoAsiento(TipoAsiento tipoAsiento) {
		this.tipoAsiento = tipoAsiento;
	}

	/**
	 * Devuelve fechaAsiento
	 * @return fechaAsiento.
	 */
	public LocalDate getFechaAsiento() {
		return fechaAsiento;
	}

	/**
	 * Asigna fechaAsiento
	 * @param fechaAsiento nuevo valor para fechaAsiento. 
	 */
	public void setFechaAsiento(LocalDate fechaAsiento) {
		this.fechaAsiento = fechaAsiento;
	}
	
	/**
	 * Devuelve numero
	 * @return numero.
	 */
	public Long getNumero() {
		return numero;
	}

	/**
	 * Asigna numero
	 * @param numero nuevo valor para numero. 
	 */
	public void setNumero(Long numero) {
		this.numero = numero;
	}
	
	/**
	 * Devuelve estado
	 * @return estado.
	 */
	public Long getEstado() {
		return estado;
	}

	/**
	 * Asigna estado
	 * @param estado nuevo valor para estado. 
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
	}
	
	/**
	 * Devuelve observaciones
	 * @return observaciones.
	 */
	public String getObservaciones() {
		return observaciones;
	}

	/**
	 * Asigna observaciones
	 * @param observaciones nuevo valor para observaciones. 
	 */
	public void setObservaciones(String observaciones) {
		this.observaciones = observaciones;
	}
	
	/**
	 * Devuelve nombreUsuario
	 * @return nombreUsuario.
	 */
	public String getNombreUsuario() {
		return nombreUsuario;
	}

	/**
	 * Asigna nombreUsuario
	 * @param nombreUsuario nuevo valor para nombreUsuario. 
	 */
	public void setNombreUsuario(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario;
	}
	
	/**
	 * Devuelve idReversion
	 * @return idReversion.
	 */
	public Long getIdReversion() {
		return idReversion;
	}

	/**
	 * Asigna idReversion
	 * @param idReversion nuevo valor para idReversion. 
	 */
	public void setIdReversion(Long idReversion) {
		this.idReversion = idReversion;
	}
	
	/**
	 * Devuelve numeroMes
	 * @return numeroMes.
	 */
	@Basic
	@Column(name = "ASNTPRDO")
	public Long getNumeroMes() {
		return numeroMes;
	}

	/**
	 * Asigna numeroMes
	 * @param numeroMes nuevo valor para numeroMes. 
	 */
	public void setNumeroMes(Long numeroMes) {
		this.numeroMes = numeroMes;
	}
	
	/**
	 * Devuelve numeroAnio
	 * @return numeroAnio.
	 */
	public Long getNumeroAnio() {
		return numeroAnio;
	}

	/**
	 * Asigna numeroAnio
	 * @param numeroAnio nuevo valor para numeroAnio. 
	 */
	public void setNumeroAnio(Long numeroAnio) {
		this.numeroAnio = numeroAnio;
	}
	
	/**
	 * Devuelve moneda
	 * @return moneda.
	 */
	public Long getMoneda() {
		return moneda;
	}

	/**
	 * Asigna moneda
	 * @param moneda nuevo valor para moneda. 
	 */
	public void setMoneda(Long moneda) {
		this.moneda = moneda;
	}

	/**
	 * Devuelve Mayorizacion.
	 */
	public Mayorizacion getMayorizacion() {
		return this.mayorizacion;	
	}

	/**
	 * Asigna Mayorizacion.
	 */
	public void setMayorizacion(Mayorizacion mayorizacion) {
		this.mayorizacion = mayorizacion;
	}	
	
	/**
	 * Devuelve rubroModuloClienteP
	 * @return rubroModuloClienteP.
	 */
	public Long getRubroModuloClienteP() {
		return rubroModuloClienteP;
	}

	/**
	 * Asigna rubroModuloClienteP
	 * @param rubroModuloClienteP nuevo valor para rubroModuloClienteP. 
	 */
	public void setRubroModuloClienteP(Long rubroModuloClienteP) {
		this.rubroModuloClienteP = rubroModuloClienteP;
	}
	
	/**
	 * Devuelve rubroModuloClienteH
	 * @return rubroModuloClienteH.
	 */
	public Long getRubroModuloClienteH() {
		return rubroModuloClienteH;
	}

	/**
	 * Asigna rubroModuloClienteH
	 * @param rubroModuloClienteH nuevo valor para rubroModuloClienteH. 
	 */
	public void setRubroModuloClienteH(Long rubroModuloClienteH) {
		this.rubroModuloClienteH = rubroModuloClienteH;
	}
	
	/**
	 * Devuelve fechaIngreso
	 * @return fechaIngreso.
	 */
	@Basic
	@Column(name = "ASNTFCPR")
	public LocalDateTime getFechaIngreso() {
		return fechaIngreso;
	}

	/**
	 * Asigna fechaIngreso
	 * @param fechaIngreso nuevo valor para fecheIngreso. 
	 */
	public void setFechaIngreso(LocalDateTime fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}
	
	/**
	 * Devuelve periodo.
	 */
	public Periodo getPeriodo() {
		return this.periodo;
	}
	
	/**
	 * Asigna periodo.
	 */
	public void setPeriodo(Periodo periodo) {
		this.periodo = periodo;
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

   
}

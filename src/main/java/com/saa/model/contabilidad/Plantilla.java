package com.saa.model.contabilidad;

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

@SuppressWarnings("serial")
@Entity
@Table(name = "PLNS", schema = "CNT")
@SequenceGenerator(name = "SQ_PLNSCDGO", sequenceName = "CNT.SQ_PLNSCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "PlantillaAll", query = "select e from Plantilla e"),
	@NamedQuery(name = "PlantillaId", query = "select e from Plantilla e where e.codigo = :id")
})
public class Plantilla implements Serializable {

	/**
	 * id de la tabla.
	 */
	@Basic
	@Id
	@Column(name = "PLNSCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PLNSCDGO")
	private Long codigo;
	
	/**
	 * nombre de la plantilla.
	 */
	@Basic
	@Column(name = "PLNSNMBR", length = 100)
	private String nombre;
	
	/**
	 * código alterno de la plantilla será numerico y estará documentado.
	 */
	@Basic
	@Column(name = "PLNSCDAL")
	private Long codigoAlterno;
	
	/**
	 * estado 1 = activo,2 = inactivo.
	 */
	@Basic
	@Column(name = "PLNSESTD")
	private Long estado;
	
	/**
	 * código del nivel de empresa tomado de scp.pjrq con pgspcdgo 12.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;
	
	/**
	 * observaciones de plantilla.
	 */
	@Basic
	@Column(name = "PLNSOBSR", length = 500)
	private String observacion;
	
	/**
	 * fecha de desactivación de plantilla.
	 */
	@Basic
	@Column(name = "PLNSFCDS")
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
	 * Devuelve codigoAlterno
	 * @return codigoAlterno
	 */
	public Long getCodigoAlterno() {
		return codigoAlterno;
	}

	/**
	 * Asigna codigoAlterno
	 * @param codigoAlterno nuevo valor para codigoAlterno 
	 */
	public void setCodigoAlterno(Long codigoAlterno) {
		this.codigoAlterno = codigoAlterno;
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
	 * @param empresa nuevo valor para empresa 
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}
	
	/**
	 * Devuelve observacion
	 * @return observacion
	 */
	public String getObservacion() {
		return observacion;
	}

	/**
	 * Asigna observacion
	 * @param observacion nuevo valor para observacion 
	 */
	public void setObservacion(String observacion) {
		this.observacion = observacion;
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
	 * @param fechaInactivo nuevo valor para fechaInactivo 
	 */
	public void setFechaInactivo(LocalDateTime fechaInactivo) {
		this.fechaInactivo = fechaInactivo;
	}

   
}

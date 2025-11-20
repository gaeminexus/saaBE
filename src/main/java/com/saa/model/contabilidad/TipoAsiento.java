package com.saa.model.contabilidad;

import java.io.Serializable;
import java.util.Date;
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
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@SuppressWarnings("serial")
@Entity
@Table(name = "PLNT", schema = "CNT")
@SequenceGenerator(name = "SQ_PLNTCDGO", sequenceName = "CNT.SQ_PLNTCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TipoAsientoAll", query = "select e from TipoAsiento e"),
	@NamedQuery(name = "TipoAsientoId", query = "select e from TipoAsiento e where e.codigo = :id")
})
public class TipoAsiento implements Serializable {

	/**
	 * id de la tabla.
	 */
	@Basic
	@Id
	@Column(name = "PLNTCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PLNTCDGO")
	private Long codigo;
	
	/**
	 * nombre de la plantilla.
	 */
	@Basic
	@Column(name = "PLNTNMBR", length = 100)
	private String nombre;
	
	/**
	 * código alterno de la plantilla será numerico y estará documentado.
	 */
	@Basic
	@Column(name = "PLNTCDAL")
	private Long codigoAlterno;
	
	/**
	 * estado 1 = activo, 2 = inactivo.
	 */
	@Basic
	@Column(name = "PLNTESTD")
	private Long estado;
	
	/**
	 * código del nivel de empresa tomado de scp.pjrq con pgspcdgo 12. empresa.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;
	
	/**
	 * observaciones de plantilla. observacion.
	 */
	@Basic
	@Column(name = "PLNTOBSR", length = 500)
	private String observacion;
	
	/**
	 * fecha de desactivación de plantilla. fechaInactivo.
	 */
	@Basic
	@Column(name = "PLNTFCDS")
	@Temporal(TemporalType.DATE)
	private Date fechaInactivo;

	
	@Basic
	@Column(name = "PLNTSSTM")
	@Temporal(TemporalType.DATE)
	private Long sistema; 

	/**
	 * Metodo que recupera los valores de la variable codigo
	 * @return codigo
	 */
	public Long getSistema() {
		return sistema;
	}
	
	/**
	 * Metodo que recupera los valores de la variable codigo
	 * @param codigo nuevo valor para codigo 
	 */
	public void setSistema(Long sistema) {
		this.sistema = sistema;
	}
	
	
	
	
	/**
	 * Metodo que recupera los valores de la variable codigo
	 * @return codigo
	 */
	public Long getCodigo() {
		return codigo;
	}
	
	/**
	 * Metodo que recupera los valores de la variable codigo
	 * @param codigo nuevo valor para codigo 
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}
	
	/**
	 * Metodo que recupera los valores de la variable Nombre
	 * @return nombre
	 */
	public String getNombre() {
		return nombre;
	}

	/**
	 * Metodo que asigna los valores de la variable nombre
	 * @param nombre nuevo valor para nombre 
	 */
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	
	/**
	 * Metodo que recupera los valores de la variable codigo Alterno
	 * @return codigoAlterno
	 */
	public Long getCodigoAlterno() {
		return codigoAlterno;
	}

	/**
	 * Metodo que asigna los valores de la variable codigo Alterno
	 * @param codigoAlterno nuevo valor para codigoAlterno 
	 */
	public void setCodigoAlterno(Long codigoAlterno) {
		this.codigoAlterno = codigoAlterno;
	}
	
	/**
	 * Metodo que recupera los valores de la variable Estado
	 * @return estado
	 */
	public Long getEstado() {
		return estado;
	}

	/**
	 * Metodo que asigna los valores de la variable Estado
	 * @param estado nuevo valor para estado 
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
	}
	
	/**
	 * Metodo que recupera los valores de la variable empresa
	 * get empresa
	 */
	public Empresa getEmpresa() {
		return empresa;
	}

	/**
	 * Metodo que asigna los valores de la variable empresa
	 * @param empresa nuevo valor para empresa 
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}
	
	/**
	 * Metodo que recupera los valores de la variable observacion
	 * @return observacion
	 */
	public String getObservacion() {
		return observacion;
	}

	/**
	 * Metodo que asigna los valores de la variable observacion
	 * @param observacion nuevo valor para observacion 
	 */
	public void setObservacion(String observacion) {
		this.observacion = observacion;
	}
	
	/**
	 * Metodo que recupera los valores de la variable fechainactivo
	 * @return fechaInactivo
	 */
	public Date getFechaInactivo() {
		return fechaInactivo;
	}

	/**
	 * Metodo que asigna los valores de la variable fechainactivo
	 * @param fechaInactivo nuevo valor para fechaInactivo 
	 */
	public void setFechaInactivo(Date fechaInactivo) {
		this.fechaInactivo = fechaInactivo;
	}

}

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
@Table(name = "RPRT", schema = "CNT")
@SequenceGenerator(name = "SQ_RPRTCDGO", sequenceName = "CNT.SQ_RPRTCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "ReporteContableAll", query = "select e from ReporteContable e"),
	@NamedQuery(name = "ReporteContableId", query = "select e from ReporteContable e where e.codigo = :id")
})
public class ReporteContable implements Serializable {

	/**
	 * Id de la tabla.
	 */
	@Basic
	@Id
	@Column(name = "RPRTCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_RPRTCDGO")	
	private Long codigo;
	
	/**
	 * PJRQCDGO clave foránea a PJRQ.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;	

	/**
	 * Nombre del Reporte Contable.
	 */
	@Basic
	@Column(name = "RPRTNMBR", length = 100)
	private String nombreReporte;
	
	/**
	 * Estado, 1 = Activo, 2 = Inactivo. 
	 */
	@Basic
	@Column(name = "RPRTESTD")
	private Long estado;
	
	/**
	 * Código Alterno.
	 */
	@Basic
	@Column(name = "RPRTALTR")
	private Long codigoAlterno;
	
	/**
	 * Devuelve Codigo
	 * @return codigo.
	 */	
	public Long getCodigo() {
		return codigo;
	}

	/**
	 * Asigna Codigo
	 * @param codigo nuevo valor para codigo. 
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}
	
	/**
	 * Devuelve Empresa
	 */
	public Empresa getEmpresa() {
		return this.empresa;
	}
	
	/**
	 * Asigna Empresa
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	/**
	 * Devuelve NombreReporte
	 * @return nombreReporte.
	 */
	public String getNombreReporte() {
		return nombreReporte;
	}

	/**
	 * Asigna NombreReporte
	 * @param nombreReporte nuevo valor para nombreReporte. 
	 */
	public void setNombreReporte(String nombreReporte) {
		this.nombreReporte = nombreReporte;
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
	 * Devuelve codigoAlterno
	 * @return codigoAlterno.
	 */
	public Long getCodigoAlterno() {
		return codigoAlterno;
	}

	/**
	 * Asigna codigoAlterno
	 * @param codigoAlterno nuevo valor para codigoAlterno. 
	 */
	public void setCodigoAlterno(Long codigoAlterno) {
		this.codigoAlterno = codigoAlterno;
	}

	
}

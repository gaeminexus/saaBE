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

	// ===================================================================================================
	// C19b (D-031): el motor de balances soporta tambien reportes de RENGLONES PROPIOS (B17, flujo de caja):
	// renglones en CNT.RNRP (RenglonReporteContable) alimentados por las reglas de CNT.DTRP. Con todo en NULL
	// el reporte es el balance de siempre (rangos del plan de cuentas). ⛔ Requiere el DDL C19-01 (o C19-05 en
	// la base de trabajo) ANTES de desplegar el WAR. Ver docs/logica-negocio/rpr/ESTRUCTURAS-DE-RENGLONES.md.
	// ===================================================================================================

	/** Tipo de reporte: NULL/1 balance sobre el plan de cuentas · 2 renglones propios (CNT.RNRP). */
	@Basic
	@Column(name = "RPRTTPRP")
	private Long tipoReporte;

	/** Codigo de la estructura ante el ente de control (B17, FLC). */
	@Basic
	@Column(name = "RPRTCDES", length = 10)
	private String codigoEstructura;

	/** Periodicidad del archivo (M mensual...). */
	@Basic
	@Column(name = "RPRTPRDC", length = 1)
	private String periodicidad;

	/** Codigo de la entidad ante el ente de control (ASOPREP = 3968). */
	@Basic
	@Column(name = "RPRTCDEN", length = 10)
	private String codigoEntidad;

	/** Base del valor por defecto de los renglones: 1 saldo al corte · 2 movimiento del periodo · 3 saldo al inicio. */
	@Basic
	@Column(name = "RPRTBSVL")
	private Long baseValor;

	/** 1 = cuenta sin regla -> renglon cuyo codigo es el prefijo mas largo de la cuenta sin separadores. */
	@Basic
	@Column(name = "RPRTMPAU")
	private Long mapeoAutomatico;

	/** 1 = un renglon suma lo de los renglones cuyo codigo empieza por el suyo (catalogo jerarquico). */
	@Basic
	@Column(name = "RPRTACPF")
	private Long acumulaPorPrefijo;

	/** Tipo de salida en archivo: NULL sin archivo · 1 TXT de la Superintendencia (TAB, CRLF, latin1) · 2 formato derivado del B17 (C38, solo /flef). */
	@Basic
	@Column(name = "RPRTFRAR")
	private Long formatoArchivo;

	/** 1 = la cabecera del archivo lleva la suma de control. */
	@Basic
	@Column(name = "RPRTSMCT")
	private Long sumaControl;

	/** Titulo oficial impreso (estados financieros); NULL = nombreReporte. */
	@Basic
	@Column(name = "RPRTTTLO", length = 200)
	private String titulo;

	/** Nivel de profundidad: digitos maximos del codigo de cuenta/renglon en el resultado; NULL = todos. */
	@Basic
	@Column(name = "RPRTNVMX")
	private Long nivelMaximo;

	public Long getTipoReporte() { return tipoReporte; }
	public void setTipoReporte(Long tipoReporte) { this.tipoReporte = tipoReporte; }
	public String getCodigoEstructura() { return codigoEstructura; }
	public void setCodigoEstructura(String codigoEstructura) { this.codigoEstructura = codigoEstructura; }
	public String getPeriodicidad() { return periodicidad; }
	public void setPeriodicidad(String periodicidad) { this.periodicidad = periodicidad; }
	public String getCodigoEntidad() { return codigoEntidad; }
	public void setCodigoEntidad(String codigoEntidad) { this.codigoEntidad = codigoEntidad; }
	public Long getBaseValor() { return baseValor; }
	public void setBaseValor(Long baseValor) { this.baseValor = baseValor; }
	public Long getMapeoAutomatico() { return mapeoAutomatico; }
	public void setMapeoAutomatico(Long mapeoAutomatico) { this.mapeoAutomatico = mapeoAutomatico; }
	public Long getAcumulaPorPrefijo() { return acumulaPorPrefijo; }
	public void setAcumulaPorPrefijo(Long acumulaPorPrefijo) { this.acumulaPorPrefijo = acumulaPorPrefijo; }
	public Long getFormatoArchivo() { return formatoArchivo; }
	public void setFormatoArchivo(Long formatoArchivo) { this.formatoArchivo = formatoArchivo; }
	public Long getSumaControl() { return sumaControl; }
	public void setSumaControl(Long sumaControl) { this.sumaControl = sumaControl; }
	public String getTitulo() { return titulo; }
	public void setTitulo(String titulo) { this.titulo = titulo; }
	public Long getNivelMaximo() { return nivelMaximo; }
	public void setNivelMaximo(Long nivelMaximo) { this.nivelMaximo = nivelMaximo; }

	/** true si es un reporte de renglones propios (RPRTTPRP = 2). */
	public boolean esDeRenglones() {
		return tipoReporte != null && tipoReporte == 2L;
	}
}

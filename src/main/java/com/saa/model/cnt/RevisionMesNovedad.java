package com.saa.model.cnt;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * C25 — Revisión del mes con el cliente (CNT.RVNM): la cabecera del HTML de revisión (flujo del mes, cuadratura B17-G,
 * bancos, observaciones, cuentas por regularizar y reparto de sus asientos por novedad). Una fila por empresa, mes y
 * versión; la de una versión anterior queda HISTORICA (estado 2).
 *
 * <p>DDL: docs/logica-negocio/cnt/sql/C25-02-revision-con-el-cliente.sql. Las columnas JSON se guardan como texto; el
 * servicio las devuelve ya interpretadas en GET /nvdd/resumenMes.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RVNM", schema = "CNT")
@NamedQueries({
	@NamedQuery(name = "RevisionMesNovedadAll", query = "select e from RevisionMesNovedad e"),
	@NamedQuery(name = "RevisionMesNovedadId", query = "select e from RevisionMesNovedad e where e.codigo = :id")
})
public class RevisionMesNovedad implements Serializable {

	/** PK (IDENTITY). */
	@Id
	@Basic
	@Column(name = "RVNMCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long codigo;

	/** Empresa (SCP.PJRQ). */
	@Basic
	@Column(name = "PJRQCDGO")
	private Long idEmpresa;

	/** Período contable (CNT.PRDO). */
	@Basic
	@Column(name = "PRDOCDGO")
	private Long idPeriodo;

	/** Año. */
	@Basic
	@Column(name = "RVNMANNN")
	private Long anio;

	/** Mes (1..12). */
	@Basic
	@Column(name = "RVNMMSSS")
	private Long mes;

	/** Versión del lote del mes. */
	@Basic
	@Column(name = "RVNMVRSN")
	private Long version;

	/** Nombre del mes (junio 2025). */
	@Basic
	@Column(name = "RVNMNMBR", length = 50)
	private String nombreMes;

	/** Fecha de corte como texto. */
	@Basic
	@Column(name = "RVNMCRTE", length = 30)
	private String corte;

	/** Estado del mes (Cerrado definitivo). */
	@Basic
	@Column(name = "RVNMESMS", length = 100)
	private String estadoMes;

	/** Fecha del cierre como texto. */
	@Basic
	@Column(name = "RVNMFCCR", length = 30)
	private String fechaCierre;

	/** Versión del cierre. */
	@Basic
	@Column(name = "RVNMVRCR", length = 300)
	private String versionCierre;

	/** Ejecución de las estructuras G (RPR.EJRC). */
	@Basic
	@Column(name = "RVNMEJRC")
	private Long ejecucionG;

	/** Flujo del mes, JSON. */
	@Lob
	@Column(name = "RVNMFLJO")
	private String flujo;

	/** Cuadratura B17-G con notas y cuentas puente, JSON. */
	@Lob
	@Column(name = "RVNMCDRT")
	private String cuadratura;

	/** Bancos, JSON. */
	@Lob
	@Column(name = "RVNMBNCS")
	private String bancos;

	/** Observaciones, JSON. */
	@Lob
	@Column(name = "RVNMOBSR")
	private String observaciones;

	/** Nombres y saldos de cuentas, JSON. */
	@Lob
	@Column(name = "RVNMCNTS")
	private String cuentas;

	/** Reparto de asientos de cuentas por regularizar por novedad, JSON. */
	@Lob
	@Column(name = "RVNMRGLR")
	private String regularizar;

	/** Archivos de trabajo del expediente (solo GAEMII). */
	@Basic
	@Column(name = "RVNMFNTE", length = 2000)
	private String fuentes;

	/** Lote de carga. */
	@Basic
	@Column(name = "RVNMLTCR", length = 50)
	private String lote;

	/** 1 VIGENTE, 2 HISTORICA. */
	@Basic
	@Column(name = "RVNMESTD")
	private Long estado;

	/** Fecha de registro. */
	@Basic
	@Column(name = "RVNMFCRG")
	private LocalDateTime fechaRegistro;

	/** Usuario que registró. */
	@Basic
	@Column(name = "RVNMUSAR", length = 50)
	private String usuarioRegistro;

	/** Fecha de la última recarga. */
	@Basic
	@Column(name = "RVNMFCMD")
	private LocalDateTime fechaModificacion;

	/** Usuario de la última recarga. */
	@Basic
	@Column(name = "RVNMUSMD", length = 50)
	private String usuarioModificacion;

	public Long getCodigo() { return codigo; }
	public void setCodigo(Long codigo) { this.codigo = codigo; }
	public Long getIdEmpresa() { return idEmpresa; }
	public void setIdEmpresa(Long idEmpresa) { this.idEmpresa = idEmpresa; }
	public Long getIdPeriodo() { return idPeriodo; }
	public void setIdPeriodo(Long idPeriodo) { this.idPeriodo = idPeriodo; }
	public Long getAnio() { return anio; }
	public void setAnio(Long anio) { this.anio = anio; }
	public Long getMes() { return mes; }
	public void setMes(Long mes) { this.mes = mes; }
	public Long getVersion() { return version; }
	public void setVersion(Long version) { this.version = version; }
	public String getNombreMes() { return nombreMes; }
	public void setNombreMes(String nombreMes) { this.nombreMes = nombreMes; }
	public String getCorte() { return corte; }
	public void setCorte(String corte) { this.corte = corte; }
	public String getEstadoMes() { return estadoMes; }
	public void setEstadoMes(String estadoMes) { this.estadoMes = estadoMes; }
	public String getFechaCierre() { return fechaCierre; }
	public void setFechaCierre(String fechaCierre) { this.fechaCierre = fechaCierre; }
	public String getVersionCierre() { return versionCierre; }
	public void setVersionCierre(String versionCierre) { this.versionCierre = versionCierre; }
	public Long getEjecucionG() { return ejecucionG; }
	public void setEjecucionG(Long ejecucionG) { this.ejecucionG = ejecucionG; }
	public String getFlujo() { return flujo; }
	public void setFlujo(String flujo) { this.flujo = flujo; }
	public String getCuadratura() { return cuadratura; }
	public void setCuadratura(String cuadratura) { this.cuadratura = cuadratura; }
	public String getBancos() { return bancos; }
	public void setBancos(String bancos) { this.bancos = bancos; }
	public String getObservaciones() { return observaciones; }
	public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
	public String getCuentas() { return cuentas; }
	public void setCuentas(String cuentas) { this.cuentas = cuentas; }
	public String getRegularizar() { return regularizar; }
	public void setRegularizar(String regularizar) { this.regularizar = regularizar; }
	public String getFuentes() { return fuentes; }
	public void setFuentes(String fuentes) { this.fuentes = fuentes; }
	public String getLote() { return lote; }
	public void setLote(String lote) { this.lote = lote; }
	public Long getEstado() { return estado; }
	public void setEstado(Long estado) { this.estado = estado; }
	public LocalDateTime getFechaRegistro() { return fechaRegistro; }
	public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
	public String getUsuarioRegistro() { return usuarioRegistro; }
	public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
	public LocalDateTime getFechaModificacion() { return fechaModificacion; }
	public void setFechaModificacion(LocalDateTime fechaModificacion) { this.fechaModificacion = fechaModificacion; }
	public String getUsuarioModificacion() { return usuarioModificacion; }
	public void setUsuarioModificacion(String usuarioModificacion) { this.usuarioModificacion = usuarioModificacion; }
}

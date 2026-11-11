package com.saa.model.cnt;

import java.io.Serializable;
import java.time.LocalDate;
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
 * C25 — Novedad de un mes re-procesado (CNT.NVDD): el caso tal como se comunicó a ASOPREP (C-01, B-07, TABLAS-59...), su tipo de respuesta (D-056 (6)) y su estado de seguimiento. El estado lo recalcula SeguimientoNovedadService desde los requerimientos; no se escribe desde el cliente.
 *
 * <p>DDL: docs/logica-negocio/cnt/sql/C25-01-seguimiento-novedades.sql. Las FK se guardan como id (no relación):
 * la entidad se serializa directo a JSON y la pantalla arma el detalle con GET /nvdd/{id}/detalle.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "NVDD", schema = "CNT")
@NamedQueries({
	@NamedQuery(name = "NovedadSeguimientoAll", query = "select e from NovedadSeguimiento e"),
	@NamedQuery(name = "NovedadSeguimientoId", query = "select e from NovedadSeguimiento e where e.codigo = :id")
})
public class NovedadSeguimiento implements Serializable {

	/** PK (IDENTITY). */
	@Id
	@Basic
	@Column(name = "NVDDCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long codigo;

	/** Empresa (SCP.PJRQ). */
	@Basic
	@Column(name = "PJRQCDGO")
	private Long idEmpresa;

	/** Período contable (CNT.PRDO) del mes. */
	@Basic
	@Column(name = "PRDOCDGO")
	private Long idPeriodo;

	/** Año. */
	@Basic
	@Column(name = "NVDDANNN")
	private Long anio;

	/** Mes (1..12). */
	@Basic
	@Column(name = "NVDDMSSS")
	private Long mes;

	/** CREDITO, APORTES, BANCOS, CONTABILIDAD, CXP, CXC, RRHH, ESTRUCTURAS. */
	@Basic
	@Column(name = "NVDDMDLO", length = 20)
	private String modulo;

	/** Código en su archivo del mes (C-01, TABLAS-59). */
	@Basic
	@Column(name = "NVDDCDNV", length = 10)
	private String codigoNovedad;

	/** Título. */
	@Basic
	@Column(name = "NVDDTTLO", length = 300)
	private String titulo;

	/** Qué se encontró (CLOB). */
	@Lob
	@Column(name = "NVDDHLLZ")
	private String hallazgo;

	/** Cómo se resolvió (CLOB). */
	@Lob
	@Column(name = "NVDDRSLC")
	private String resolucion;

	/** INFORMATIVA, DOCUMENTO, DECISION, CONFIRMACION, ELECCION, APROBACION. */
	@Basic
	@Column(name = "NVDDTPRS", length = 20)
	private String tipoRespuesta;

	/** Área principal de ASOPREP (dato del pedido, D-056 (5)). */
	@Basic
	@Column(name = "NVDDARRS", length = 30)
	private String area;

	/** Valor de referencia. */
	@Basic
	@Column(name = "NVDDVLOR")
	private Double valor;

	/** Criterios o decisiones aplicadas. */
	@Basic
	@Column(name = "NVDDCRTR", length = 500)
	private String criterios;

	/** Archivos de trabajo del expediente (solo GAEMII). */
	@Basic
	@Column(name = "NVDDFNTE", length = 2000)
	private String fuentes;

	/** Archivo .txt de origen. */
	@Basic
	@Column(name = "NVDDARCH", length = 100)
	private String archivo;

	/** Versión. */
	@Basic
	@Column(name = "NVDDVRSN")
	private Long version;

	/** Lote de carga. */
	@Basic
	@Column(name = "NVDDLTCR", length = 50)
	private String lote;

	/** 1 BORRADOR, 2 ENVIADA, 3 RESPONDIDA_PARCIAL, 4 RESPONDIDA, 5 OBSERVADA, 6 POR_APLICAR, 7 CERRADA, 8 ANULADA, 9 REEMPLAZADA. */
	@Basic
	@Column(name = "NVDDESTD")
	private Long estado;

	/** Fecha de envío a ASOPREP. */
	@Basic
	@Column(name = "NVDDFCEN")
	private LocalDateTime fechaEnvio;

	/** Usuario que envió. */
	@Basic
	@Column(name = "NVDDUSEN", length = 50)
	private String usuarioEnvio;

	/** Fecha límite de respuesta. */
	@Basic
	@Column(name = "NVDDFCLM")
	private LocalDate fechaLimite;

	/** Fecha de cierre. */
	@Basic
	@Column(name = "NVDDFCCR")
	private LocalDateTime fechaCierre;

	/** Usuario que cerró. */
	@Basic
	@Column(name = "NVDDUSCR", length = 50)
	private String usuarioCierre;

	/** Forma de cierre. */
	@Basic
	@Column(name = "NVDDMTCR", length = 1000)
	private String motivoCierre;

	/** Fecha de registro. */
	@Basic
	@Column(name = "NVDDFCRG")
	private LocalDateTime fechaRegistro;

	/** Usuario que registró. */
	@Basic
	@Column(name = "NVDDUSAR", length = 50)
	private String usuarioRegistro;

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
	public String getModulo() { return modulo; }
	public void setModulo(String modulo) { this.modulo = modulo; }
	public String getCodigoNovedad() { return codigoNovedad; }
	public void setCodigoNovedad(String codigoNovedad) { this.codigoNovedad = codigoNovedad; }
	public String getTitulo() { return titulo; }
	public void setTitulo(String titulo) { this.titulo = titulo; }
	public String getHallazgo() { return hallazgo; }
	public void setHallazgo(String hallazgo) { this.hallazgo = hallazgo; }
	public String getResolucion() { return resolucion; }
	public void setResolucion(String resolucion) { this.resolucion = resolucion; }
	public String getTipoRespuesta() { return tipoRespuesta; }
	public void setTipoRespuesta(String tipoRespuesta) { this.tipoRespuesta = tipoRespuesta; }
	public String getArea() { return area; }
	public void setArea(String area) { this.area = area; }
	public Double getValor() { return valor; }
	public void setValor(Double valor) { this.valor = valor; }
	public String getCriterios() { return criterios; }
	public void setCriterios(String criterios) { this.criterios = criterios; }
	public String getFuentes() { return fuentes; }
	public void setFuentes(String fuentes) { this.fuentes = fuentes; }
	public String getArchivo() { return archivo; }
	public void setArchivo(String archivo) { this.archivo = archivo; }
	public Long getVersion() { return version; }
	public void setVersion(Long version) { this.version = version; }
	public String getLote() { return lote; }
	public void setLote(String lote) { this.lote = lote; }
	public Long getEstado() { return estado; }
	public void setEstado(Long estado) { this.estado = estado; }
	public LocalDateTime getFechaEnvio() { return fechaEnvio; }
	public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }
	public String getUsuarioEnvio() { return usuarioEnvio; }
	public void setUsuarioEnvio(String usuarioEnvio) { this.usuarioEnvio = usuarioEnvio; }
	public LocalDate getFechaLimite() { return fechaLimite; }
	public void setFechaLimite(LocalDate fechaLimite) { this.fechaLimite = fechaLimite; }
	public LocalDateTime getFechaCierre() { return fechaCierre; }
	public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }
	public String getUsuarioCierre() { return usuarioCierre; }
	public void setUsuarioCierre(String usuarioCierre) { this.usuarioCierre = usuarioCierre; }
	public String getMotivoCierre() { return motivoCierre; }
	public void setMotivoCierre(String motivoCierre) { this.motivoCierre = motivoCierre; }
	public LocalDateTime getFechaRegistro() { return fechaRegistro; }
	public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
	public String getUsuarioRegistro() { return usuarioRegistro; }
	public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}

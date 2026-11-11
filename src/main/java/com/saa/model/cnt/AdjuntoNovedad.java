package com.saa.model.cnt;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * C25 — Adjunto (CNT.ADNV): metadatos de un archivo subido con POST /file/upload/custom, de una novedad, un requerimiento, una respuesta o una decisión (constancia).
 *
 * <p>DDL: docs/logica-negocio/cnt/sql/C25-01-seguimiento-novedades.sql. Las FK se guardan como id (no relación):
 * la entidad se serializa directo a JSON y la pantalla arma el detalle con GET /nvdd/{id}/detalle.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ADNV", schema = "CNT")
@NamedQueries({
	@NamedQuery(name = "AdjuntoNovedadAll", query = "select e from AdjuntoNovedad e"),
	@NamedQuery(name = "AdjuntoNovedadId", query = "select e from AdjuntoNovedad e where e.codigo = :id")
})
public class AdjuntoNovedad implements Serializable {

	/** PK (IDENTITY). */
	@Id
	@Basic
	@Column(name = "ADNVCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long codigo;

	/** Novedad. */
	@Basic
	@Column(name = "NVDDCDGO")
	private Long idNovedad;

	/** Requerimiento. */
	@Basic
	@Column(name = "RQNVCDGO")
	private Long idRequerimiento;

	/** Respuesta. */
	@Basic
	@Column(name = "RSNVCDGO")
	private Long idRespuesta;

	/** Decisión. */
	@Basic
	@Column(name = "DCSNCDGO")
	private Long idDecision;

	/** COMPROBANTE, LIQUIDACION, FACTURA, RETENCION, ACTA, CORREO, TABLA_AMORTIZACION, CERTIFICADO, CONTRATO, ROL, EXTRACTO, OTRO. */
	@Basic
	@Column(name = "ADNVTPDC", length = 20)
	private String tipoDocumento;

	/** Nombre original. */
	@Basic
	@Column(name = "ADNVNMAR", length = 500)
	private String nombreArchivo;

	/** Ruta devuelta por FileRest. */
	@Basic
	@Column(name = "ADNVURLA", length = 2000)
	private String url;

	/** Tipo MIME. */
	@Basic
	@Column(name = "ADNVMMTY", length = 200)
	private String mimeType;

	/** Tamaño en bytes. */
	@Basic
	@Column(name = "ADNVTMNO")
	private Long tamanio;

	/** SHA-256. */
	@Basic
	@Column(name = "ADNVHASH", length = 64)
	private String hash;

	/** Observaciones. */
	@Basic
	@Column(name = "ADNVOBSR", length = 1000)
	private String observacion;

	/** Cuándo GAEMII lo copió a 02-informacion-cliente. */
	@Basic
	@Column(name = "ADNVFCEX")
	private LocalDateTime fechaCopiaExpediente;

	/** Fecha de registro. */
	@Basic
	@Column(name = "ADNVFCRG")
	private LocalDateTime fechaRegistro;

	/** Usuario que subió. */
	@Basic
	@Column(name = "ADNVUSAR", length = 50)
	private String usuarioRegistro;

	/** 1 activo, 2 anulado. */
	@Basic
	@Column(name = "ADNVESTD")
	private Long estado;

	public Long getCodigo() { return codigo; }
	public void setCodigo(Long codigo) { this.codigo = codigo; }
	public Long getIdNovedad() { return idNovedad; }
	public void setIdNovedad(Long idNovedad) { this.idNovedad = idNovedad; }
	public Long getIdRequerimiento() { return idRequerimiento; }
	public void setIdRequerimiento(Long idRequerimiento) { this.idRequerimiento = idRequerimiento; }
	public Long getIdRespuesta() { return idRespuesta; }
	public void setIdRespuesta(Long idRespuesta) { this.idRespuesta = idRespuesta; }
	public Long getIdDecision() { return idDecision; }
	public void setIdDecision(Long idDecision) { this.idDecision = idDecision; }
	public String getTipoDocumento() { return tipoDocumento; }
	public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
	public String getNombreArchivo() { return nombreArchivo; }
	public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
	public String getUrl() { return url; }
	public void setUrl(String url) { this.url = url; }
	public String getMimeType() { return mimeType; }
	public void setMimeType(String mimeType) { this.mimeType = mimeType; }
	public Long getTamanio() { return tamanio; }
	public void setTamanio(Long tamanio) { this.tamanio = tamanio; }
	public String getHash() { return hash; }
	public void setHash(String hash) { this.hash = hash; }
	public String getObservacion() { return observacion; }
	public void setObservacion(String observacion) { this.observacion = observacion; }
	public LocalDateTime getFechaCopiaExpediente() { return fechaCopiaExpediente; }
	public void setFechaCopiaExpediente(LocalDateTime fechaCopiaExpediente) { this.fechaCopiaExpediente = fechaCopiaExpediente; }
	public LocalDateTime getFechaRegistro() { return fechaRegistro; }
	public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
	public String getUsuarioRegistro() { return usuarioRegistro; }
	public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
	public Long getEstado() { return estado; }
	public void setEstado(Long estado) { this.estado = estado; }
}

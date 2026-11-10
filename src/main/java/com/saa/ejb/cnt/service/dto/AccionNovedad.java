package com.saa.ejb.cnt.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * C25 — Cuerpo único de las acciones del seguimiento de novedades (responder, revisar, decidir, ajustar,
 * vincular, cerrar...). Cada endpoint usa los campos que le tocan y valida los obligatorios; {@code usuario}
 * es obligatorio en todos (U-2).
 *
 * <p>Fechas: {@code fecha} y {@code fechaLimite} van como texto {@code yyyy-MM-dd} (o {@code yyyy-MM-ddTHH:mm} para
 * {@code fecha}); nunca un instante con zona (ver CLAUDE.md, trampa de LocalDateTime).</p>
 */
@SuppressWarnings("serial")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccionNovedad implements Serializable {

	/** Usuario de la sesión (obligatorio). */
	private String usuario;
	/** Motivo u observación (obligatorio para observar, no aplica, no existe, cerrar, reabrir, anular, desvincular). */
	private String motivo;

	// --- enviar ---
	private List<Long> ids = new ArrayList<Long>();
	private String fechaLimite;

	// --- responder ---
	private String texto;
	private String opcion;
	/** SAA, CORREO, ACTA, REUNION (por defecto SAA). */
	private String canal;
	private String fecha;
	private String area;
	private List<Adjunto> adjuntos = new ArrayList<Adjunto>();

	// --- decisión ---
	private String numero;
	private String tema;
	private String tipoDecide;
	private String cargo;
	private String evidencia;
	private Long idRespuesta;
	private Long idNovedad;
	private Long idRequerimiento;

	// --- ajuste / vínculo ---
	/** Asientos por código interno (ASNTCDGO) o número alterno (ASNTNMAL, p. ej. DIA-2026-11-0005). */
	private List<String> asientos = new ArrayList<String>();
	private String tipoVinculo;
	private Long idAsiento;
	private Long idDetalleAsiento;
	private Long idPartidaC22;
	private Long idDecision;
	private Long idNovedadRelacionada;
	private String tipoDocumentoModulo;
	private Long idDocumentoModulo;
	private Double valor;
	/** Aplicar sin asiento: por qué no hace falta ajuste. */
	private String sinAjusteMotivo;

	/** Metadatos de un archivo ya subido con POST /file/upload/custom. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Adjunto implements Serializable {
		private String tipoDocumento;
		private String nombreArchivo;
		private String url;
		private String mimeType;
		private Long tamanio;
		private String hash;
		private String observacion;

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
	}

	public String getUsuario() { return usuario; }
	public void setUsuario(String usuario) { this.usuario = usuario; }
	public String getMotivo() { return motivo; }
	public void setMotivo(String motivo) { this.motivo = motivo; }
	public List<Long> getIds() { return ids; }
	public void setIds(List<Long> ids) { this.ids = ids; }
	public String getFechaLimite() { return fechaLimite; }
	public void setFechaLimite(String fechaLimite) { this.fechaLimite = fechaLimite; }
	public String getTexto() { return texto; }
	public void setTexto(String texto) { this.texto = texto; }
	public String getOpcion() { return opcion; }
	public void setOpcion(String opcion) { this.opcion = opcion; }
	public String getCanal() { return canal; }
	public void setCanal(String canal) { this.canal = canal; }
	public String getFecha() { return fecha; }
	public void setFecha(String fecha) { this.fecha = fecha; }
	public String getArea() { return area; }
	public void setArea(String area) { this.area = area; }
	public List<Adjunto> getAdjuntos() { return adjuntos; }
	public void setAdjuntos(List<Adjunto> adjuntos) { this.adjuntos = adjuntos; }
	public String getNumero() { return numero; }
	public void setNumero(String numero) { this.numero = numero; }
	public String getTema() { return tema; }
	public void setTema(String tema) { this.tema = tema; }
	public String getTipoDecide() { return tipoDecide; }
	public void setTipoDecide(String tipoDecide) { this.tipoDecide = tipoDecide; }
	public String getCargo() { return cargo; }
	public void setCargo(String cargo) { this.cargo = cargo; }
	public String getEvidencia() { return evidencia; }
	public void setEvidencia(String evidencia) { this.evidencia = evidencia; }
	public Long getIdRespuesta() { return idRespuesta; }
	public void setIdRespuesta(Long idRespuesta) { this.idRespuesta = idRespuesta; }
	public Long getIdNovedad() { return idNovedad; }
	public void setIdNovedad(Long idNovedad) { this.idNovedad = idNovedad; }
	public Long getIdRequerimiento() { return idRequerimiento; }
	public void setIdRequerimiento(Long idRequerimiento) { this.idRequerimiento = idRequerimiento; }
	public List<String> getAsientos() { return asientos; }
	public void setAsientos(List<String> asientos) { this.asientos = asientos; }
	public String getTipoVinculo() { return tipoVinculo; }
	public void setTipoVinculo(String tipoVinculo) { this.tipoVinculo = tipoVinculo; }
	public Long getIdAsiento() { return idAsiento; }
	public void setIdAsiento(Long idAsiento) { this.idAsiento = idAsiento; }
	public Long getIdDetalleAsiento() { return idDetalleAsiento; }
	public void setIdDetalleAsiento(Long idDetalleAsiento) { this.idDetalleAsiento = idDetalleAsiento; }
	public Long getIdPartidaC22() { return idPartidaC22; }
	public void setIdPartidaC22(Long idPartidaC22) { this.idPartidaC22 = idPartidaC22; }
	public Long getIdDecision() { return idDecision; }
	public void setIdDecision(Long idDecision) { this.idDecision = idDecision; }
	public Long getIdNovedadRelacionada() { return idNovedadRelacionada; }
	public void setIdNovedadRelacionada(Long idNovedadRelacionada) { this.idNovedadRelacionada = idNovedadRelacionada; }
	public String getTipoDocumentoModulo() { return tipoDocumentoModulo; }
	public void setTipoDocumentoModulo(String tipoDocumentoModulo) { this.tipoDocumentoModulo = tipoDocumentoModulo; }
	public Long getIdDocumentoModulo() { return idDocumentoModulo; }
	public void setIdDocumentoModulo(Long idDocumentoModulo) { this.idDocumentoModulo = idDocumentoModulo; }
	public Double getValor() { return valor; }
	public void setValor(Double valor) { this.valor = valor; }
	public String getSinAjusteMotivo() { return sinAjusteMotivo; }
	public void setSinAjusteMotivo(String sinAjusteMotivo) { this.sinAjusteMotivo = sinAjusteMotivo; }
}

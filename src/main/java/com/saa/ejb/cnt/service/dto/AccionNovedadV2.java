package com.saa.ejb.cnt.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * C34 — Cuerpo de las acciones de Seguimiento de Novedades v2 (justificar líneas, responder pedidos, cerrar, dar por
 * conocidas, anular). Cada endpoint usa los campos que le tocan; {@code usuario} es siempre obligatorio (U-2).
 *
 * <p>Fechas como texto {@code yyyy-MM-dd}, nunca un instante con zona. Montos con punto decimal.</p>
 */
@SuppressWarnings("serial")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccionNovedadV2 implements Serializable {

	private String usuario;
	/** Comentario (cerrar, dar por conocida) o motivo (anular, reabrir). */
	private String motivo;
	/** Novedades (dar por conocidas en bloque). */
	private List<Long> ids = new ArrayList<Long>();

	// ---- justificar líneas
	private List<LineaMonto> lineas = new ArrayList<LineaMonto>();
	/** RESPALDO, DECISION o CRUCE. */
	private String tipo;
	private Long idLineaCruce;

	// ---- comunes a justificar y responder
	private String fechaHecho;
	private String tipoDocumento;
	private String numeroDocumento;
	private String texto;
	private Long idDecision;
	private List<Reparto> reparto = new ArrayList<Reparto>();
	private List<AccionNovedad.Adjunto> adjuntos = new ArrayList<AccionNovedad.Adjunto>();

	// ---- responder pedido
	private String opcion;
	private Double monto;
	private List<Long> filasDetalle = new ArrayList<Long>();

	/** Línea a justificar y monto (vacío = todo el saldo). */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class LineaMonto implements Serializable {
		private Long idLinea;
		private Double monto;
		public Long getIdLinea() { return idLinea; }
		public void setIdLinea(Long idLinea) { this.idLinea = idLinea; }
		public Double getMonto() { return monto; }
		public void setMonto(Double monto) { this.monto = monto; }
	}

	/** Una cuenta destino del valor. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Reparto implements Serializable {
		private Long idCuenta;
		private Double valor;
		/** D / H; vacío = el sentido del valor en la cuenta por regularizar. */
		private String sentido;
		private Long idPrestamo;
		private Long idCuota;
		private Long idEntidad;
		private String glosa;
		public Long getIdCuenta() { return idCuenta; }
		public void setIdCuenta(Long idCuenta) { this.idCuenta = idCuenta; }
		public Double getValor() { return valor; }
		public void setValor(Double valor) { this.valor = valor; }
		public String getSentido() { return sentido; }
		public void setSentido(String sentido) { this.sentido = sentido; }
		public Long getIdPrestamo() { return idPrestamo; }
		public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }
		public Long getIdCuota() { return idCuota; }
		public void setIdCuota(Long idCuota) { this.idCuota = idCuota; }
		public Long getIdEntidad() { return idEntidad; }
		public void setIdEntidad(Long idEntidad) { this.idEntidad = idEntidad; }
		public String getGlosa() { return glosa; }
		public void setGlosa(String glosa) { this.glosa = glosa; }
	}

	public String getUsuario() { return usuario; }
	public void setUsuario(String usuario) { this.usuario = usuario; }
	public String getMotivo() { return motivo; }
	public void setMotivo(String motivo) { this.motivo = motivo; }
	public List<Long> getIds() { return ids; }
	public void setIds(List<Long> ids) { this.ids = ids == null ? new ArrayList<Long>() : ids; }
	public List<LineaMonto> getLineas() { return lineas; }
	public void setLineas(List<LineaMonto> lineas) { this.lineas = lineas == null ? new ArrayList<LineaMonto>() : lineas; }
	public String getTipo() { return tipo; }
	public void setTipo(String tipo) { this.tipo = tipo; }
	public Long getIdLineaCruce() { return idLineaCruce; }
	public void setIdLineaCruce(Long idLineaCruce) { this.idLineaCruce = idLineaCruce; }
	public String getFechaHecho() { return fechaHecho; }
	public void setFechaHecho(String fechaHecho) { this.fechaHecho = fechaHecho; }
	public String getTipoDocumento() { return tipoDocumento; }
	public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
	public String getNumeroDocumento() { return numeroDocumento; }
	public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
	public String getTexto() { return texto; }
	public void setTexto(String texto) { this.texto = texto; }
	public Long getIdDecision() { return idDecision; }
	public void setIdDecision(Long idDecision) { this.idDecision = idDecision; }
	public List<Reparto> getReparto() { return reparto; }
	public void setReparto(List<Reparto> reparto) { this.reparto = reparto == null ? new ArrayList<Reparto>() : reparto; }
	public List<AccionNovedad.Adjunto> getAdjuntos() { return adjuntos; }
	public void setAdjuntos(List<AccionNovedad.Adjunto> adjuntos) { this.adjuntos = adjuntos == null ? new ArrayList<AccionNovedad.Adjunto>() : adjuntos; }
	public String getOpcion() { return opcion; }
	public void setOpcion(String opcion) { this.opcion = opcion; }
	public Double getMonto() { return monto; }
	public void setMonto(Double monto) { this.monto = monto; }
	public List<Long> getFilasDetalle() { return filasDetalle; }
	public void setFilasDetalle(List<Long> filasDetalle) { this.filasDetalle = filasDetalle == null ? new ArrayList<Long>() : filasDetalle; }
}

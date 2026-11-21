package com.saa.ejb.cnt.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * C34d — Cuerpo para registrar (o anular) un hecho de crédito en Seguimiento de Novedades v2. Fechas como texto
 * {@code yyyy-MM-dd}; montos con punto decimal. {@code usuario} es obligatorio (U-2).
 */
@SuppressWarnings("serial")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HechoCreditoDto implements Serializable {

	private String usuario;
	/** Motivo (anular). */
	private String motivo;
	private Long idEmpresa;
	/** Operación (CRD.PRST.PRSTCDGO). */
	private Long idPrestamo;
	/** ABONO_CAPITAL, PAGO_CUOTAS, PRECANCELACION, REAPERTURA_CUOTAS, PARTICIPE_DEBE. */
	private String tipo;
	private String fecha;
	private Double valor;
	/** Cuota desde la que se recalcula (opcional). */
	private Long idCuota;
	private String texto;
	private String tipoDocumento;
	private String numeroDocumento;
	/** Mes desde (yyyy-MM-dd) de un hecho en serie o de las cuotas pendientes de una precancelación. */
	private String desde;
	/** Qué pasa con la diferencia orígenes - valor: NO_SE_DEBITA, SE_DEVUELVE o PARTICIPE_DEBE. */
	private String tratamientoDiferencia;
	/** Tipo de aporte (CRD.TPAP) con que se cruza la diferencia que falta (CRUCE_APORTES). */
	private Long tipoAporteDiferencia;
	/** Cuotas que paga el hecho (pago de cuotas): su valor y sus conceptos los calcula el SAA a la fecha. */
	private List<Long> cuotas = new ArrayList<Long>();
	/** Conceptos que se condonan del faltante: MORA, INTERES, CAPITAL. */
	private List<String> conceptosCondonados = new ArrayList<String>();
	/** Decisión de ASOPREP que respalda la condonación (obligatoria con CONDONACION). */
	private Long idDecision;
	/** Pedidos que el hecho atiende solo para esta operación (el pedido sigue pendiente). */
	private List<Long> pedidosParciales = new ArrayList<Long>();
	/** Nota de ASOPREP que se convierte en este hecho. */
	private Long idNota;
	private List<Origen> origenes = new ArrayList<Origen>();
	/** Movimientos por regularizar que resuelve (además de los orígenes PENDIENTE, que se resuelven solos). */
	private List<Long> lineas = new ArrayList<Long>();
	/** Pedidos que atiende. */
	private List<Long> pedidos = new ArrayList<Long>();
	private List<AccionNovedad.Adjunto> adjuntos = new ArrayList<AccionNovedad.Adjunto>();

	/** Origen del dinero: EXTRACTO (idFilaExtracto), APORTE (idAporte), PENDIENTE (idLinea) o SIN_DINERO (texto). */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Origen implements Serializable {
		private String tipo;
		private Long idFilaExtracto;
		private Long idAporte;
		private Long idLinea;
		private Double valor;
		/** Qué es en realidad (si falta, lo propone el tipo del hecho). */
		private String conceptoReal;
		private String texto;

		public String getTipo() { return tipo; }
		public void setTipo(String tipo) { this.tipo = tipo; }
		public Long getIdFilaExtracto() { return idFilaExtracto; }
		public void setIdFilaExtracto(Long idFilaExtracto) { this.idFilaExtracto = idFilaExtracto; }
		public Long getIdAporte() { return idAporte; }
		public void setIdAporte(Long idAporte) { this.idAporte = idAporte; }
		public Long getIdLinea() { return idLinea; }
		public void setIdLinea(Long idLinea) { this.idLinea = idLinea; }
		public Double getValor() { return valor; }
		public void setValor(Double valor) { this.valor = valor; }
		public String getConceptoReal() { return conceptoReal; }
		public void setConceptoReal(String conceptoReal) { this.conceptoReal = conceptoReal; }
		public String getTexto() { return texto; }
		public void setTexto(String texto) { this.texto = texto; }
	}

	public String getUsuario() { return usuario; }
	public void setUsuario(String usuario) { this.usuario = usuario; }
	public String getMotivo() { return motivo; }
	public void setMotivo(String motivo) { this.motivo = motivo; }
	public Long getIdEmpresa() { return idEmpresa; }
	public void setIdEmpresa(Long idEmpresa) { this.idEmpresa = idEmpresa; }
	public Long getIdPrestamo() { return idPrestamo; }
	public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }
	public String getTipo() { return tipo; }
	public void setTipo(String tipo) { this.tipo = tipo; }
	public String getFecha() { return fecha; }
	public void setFecha(String fecha) { this.fecha = fecha; }
	public Double getValor() { return valor; }
	public void setValor(Double valor) { this.valor = valor; }
	public Long getIdCuota() { return idCuota; }
	public void setIdCuota(Long idCuota) { this.idCuota = idCuota; }
	public String getTexto() { return texto; }
	public void setTexto(String texto) { this.texto = texto; }
	public String getTipoDocumento() { return tipoDocumento; }
	public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
	public String getNumeroDocumento() { return numeroDocumento; }
	public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
	public List<Origen> getOrigenes() { return origenes; }
	public void setOrigenes(List<Origen> origenes) { this.origenes = origenes; }
	public List<Long> getLineas() { return lineas; }
	public void setLineas(List<Long> lineas) { this.lineas = lineas; }
	public List<Long> getPedidos() { return pedidos; }
	public void setPedidos(List<Long> pedidos) { this.pedidos = pedidos; }
	public List<AccionNovedad.Adjunto> getAdjuntos() { return adjuntos; }
	public void setAdjuntos(List<AccionNovedad.Adjunto> adjuntos) { this.adjuntos = adjuntos; }
	public String getDesde() { return desde; }
	public void setDesde(String desde) { this.desde = desde; }
	public List<Long> getCuotas() { return cuotas; }
	public void setCuotas(List<Long> cuotas) { this.cuotas = cuotas; }
	public List<String> getConceptosCondonados() { return conceptosCondonados; }
	public void setConceptosCondonados(List<String> conceptosCondonados) { this.conceptosCondonados = conceptosCondonados; }
	public Long getIdDecision() { return idDecision; }
	public void setIdDecision(Long idDecision) { this.idDecision = idDecision; }
	public Long getTipoAporteDiferencia() { return tipoAporteDiferencia; }
	public void setTipoAporteDiferencia(Long tipoAporteDiferencia) { this.tipoAporteDiferencia = tipoAporteDiferencia; }
	public String getTratamientoDiferencia() { return tratamientoDiferencia; }
	public void setTratamientoDiferencia(String tratamientoDiferencia) { this.tratamientoDiferencia = tratamientoDiferencia; }
	public List<Long> getPedidosParciales() { return pedidosParciales; }
	public void setPedidosParciales(List<Long> pedidosParciales) { this.pedidosParciales = pedidosParciales; }
	public Long getIdNota() { return idNota; }
	public void setIdNota(Long idNota) { this.idNota = idNota; }
}

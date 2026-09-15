package com.saa.ejb.cxp.seguimiento;

/**
 * ÍTEM 21 (2026-09-15, docs/logica-negocio/tsr/API-SEGUIMIENTO-PAGOS.md §2): forma fija de
 * {@code origen.documento} en la respuesta de {@code GET /pgtr/seguimiento/{idPago}} — la misma
 * para los doce orígenes del contrato, para que cada {@link ResolutorOrigenPago} no invente su
 * propio formato.
 * <p>
 * {@code fecha} es {@code String} ya formateada "yyyy-MM-dd", NO {@code LocalDate}: por defecto
 * este WAR serializa fechas con Jackson como arreglo ({@code [2026,9,7]}), no como texto (ver
 * CLAUDE.md, sección "Serialización") — el contrato exige texto plano, así que se preformatea acá
 * en vez de dejar que Jackson decida.
 */
public class DocumentoOrigenPago {

	private String numero;
	private Long estado;
	private String estadoTexto;
	private String fecha;
	private Double total;

	public DocumentoOrigenPago() {
	}

	public DocumentoOrigenPago(String numero, Long estado, String estadoTexto, String fecha, Double total) {
		this.numero = numero;
		this.estado = estado;
		this.estadoTexto = estadoTexto;
		this.fecha = fecha;
		this.total = total;
	}

	public String getNumero() { return numero; }
	public void setNumero(String numero) { this.numero = numero; }
	public Long getEstado() { return estado; }
	public void setEstado(Long estado) { this.estado = estado; }
	public String getEstadoTexto() { return estadoTexto; }
	public void setEstadoTexto(String estadoTexto) { this.estadoTexto = estadoTexto; }
	public String getFecha() { return fecha; }
	public void setFecha(String fecha) { this.fecha = fecha; }
	public Double getTotal() { return total; }
	public void setTotal(Double total) { this.total = total; }
}

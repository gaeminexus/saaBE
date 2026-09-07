package com.saa.ejb.tsr.formateador;

/**
 * Resultado de generar el archivo de pagos para un banco: los bytes listos
 * para guardar y/o descargar, más los metadatos que exige el contrato de
 * {@code POST /pgtr/lote} (ver
 * docs/logica-negocio/pagos/API-PAGOS-TESORERIA.md §3).
 *
 * {@link #textoPlano} solo viene poblado en los formatos de texto (hoy,
 * Internacional); en los binarios (hoy, Pacífico) queda null. {@link #contenido}
 * siempre trae los bytes reales del archivo, en los dos casos.
 */
public class ArchivoPagosGenerado {

	private byte[] contenido;
	private String nombreArchivo;
	private String mimeType;
	private String formatoBanco;
	private String textoPlano;

	public byte[] getContenido() {
		return contenido;
	}

	public void setContenido(byte[] contenido) {
		this.contenido = contenido;
	}

	public String getNombreArchivo() {
		return nombreArchivo;
	}

	public void setNombreArchivo(String nombreArchivo) {
		this.nombreArchivo = nombreArchivo;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getFormatoBanco() {
		return formatoBanco;
	}

	public void setFormatoBanco(String formatoBanco) {
		this.formatoBanco = formatoBanco;
	}

	public String getTextoPlano() {
		return textoPlano;
	}

	public void setTextoPlano(String textoPlano) {
		this.textoPlano = textoPlano;
	}
}

package com.saa.ejb.cxp.service;

/**
 * Resultado que la entidad financiera devuelve para un pago del lote.
 * Es el dato que produce {@link LectorRespuestaBanco} al leer el archivo de
 * respuesta y que consume el proceso que confirma o rechaza cada pago.
 */
public class RespuestaPagoBanco {

	/** Id del PagoProgramado (PGS.PGTR) al que corresponde la respuesta. */
	private Long idPago;

	/** true = el banco ejecutó la transferencia; false = la rechazó. */
	private boolean confirmado;

	/** Número de transferencia o referencia que devuelve el banco. */
	private String referencia;

	/** Motivo del rechazo, cuando el banco no ejecutó la transferencia. */
	private String motivo;

	public RespuestaPagoBanco() {
	}

	public RespuestaPagoBanco(Long idPago, boolean confirmado, String referencia, String motivo) {
		this.idPago = idPago;
		this.confirmado = confirmado;
		this.referencia = referencia;
		this.motivo = motivo;
	}

	public Long getIdPago() { return idPago; }
	public void setIdPago(Long idPago) { this.idPago = idPago; }

	public boolean isConfirmado() { return confirmado; }
	public void setConfirmado(boolean confirmado) { this.confirmado = confirmado; }

	public String getReferencia() { return referencia; }
	public void setReferencia(String referencia) { this.referencia = referencia; }

	public String getMotivo() { return motivo; }
	public void setMotivo(String motivo) { this.motivo = motivo; }
}

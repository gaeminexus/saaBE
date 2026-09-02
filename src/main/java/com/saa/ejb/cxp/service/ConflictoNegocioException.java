package com.saa.ejb.cxp.service;

import com.saa.basico.util.IncomeException;

/**
 * Excepción de negocio que el REST debe responder con 409 Conflict, no con
 * el 400/500 genérico de las demás {@link IncomeException}. Se distingue por
 * TIPO, no por el texto del mensaje: matchear por prefijo de string se probó
 * fácil de romper en silencio (una redacción distinta del mensaje cambia el
 * status sin que nada avise), así que el vínculo lo sostiene el compilador.
 * <p>
 * Uso actual: validación de beneficiario único al agrupar pagos en un solo
 * cheque, y el rechazo de reverso individual de un pago cuyo cheque respalda
 * a otros (docs/logica-negocio/tsr/DISENO-UN-CHEQUE-VARIOS-PAGOS.md §6). Ver
 * {@code PagoProgramadoServiceImpl.aprobar} y
 * {@code PagoProgramadoServiceImpl.revertirPagoConfirmado}.
 */
public class ConflictoNegocioException extends IncomeException {

	private static final long serialVersionUID = 1L;

	public ConflictoNegocioException(String mensaje) {
		super(mensaje);
	}
}

package com.saa.basico.util;

import jakarta.ejb.ApplicationException;

/**
 * El usuario con el que se intenta grabar no es válido (nulo, vacío o no existe en el sistema). Los REST la
 * traducen a HTTP 400 con {@link #getMotivo()}; ningún proceso que grabe debe haber tocado la base antes de
 * lanzarla (DISENO-USUARIO-OBLIGATORIO.md: el usuario se valida como primera instrucción).
 */
@ApplicationException(rollback = true)
public class UsuarioInvalidoException extends IncomeException {

	private static final long serialVersionUID = 1L;

	private final String motivo;

	public UsuarioInvalidoException(String motivo) {
		super(motivo);
		this.motivo = motivo;
	}

	/** Por qué el usuario no es válido, sin prefijo (cada contrato REST arma su texto con él). */
	public String getMotivo() {
		return motivo;
	}
}

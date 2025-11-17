package com.saa.ejb.contabilidad.service;

import jakarta.ejb.Remote;

@Remote
public interface CuentaService{
	
	
	/**
	 * Método para verificar si se trata de una cuenta de primer nivel
	 * @param cuenta		: Cuenta a verificar
	 * @return				: Variable que indica si es que si o no
	 * @throws Throwable	: Excepcion
	 */
	 Boolean verificaPrimerNivel(String cuenta) throws Throwable;
	
	/**
	 * Reversa el contenido de una cadena de strings
	 * @param cadena 		: Cadena a reversar
	 * @return		 		: Cadena reversada
	 * @throws Throwable	: Excepcion
	 */
	 String reversaCaracteres(String cadena) throws Throwable;
	
	/**
	 * Obtiene el id del padre mediante la cuenta
	 * @param cuenta		: Cuenta para buscar id del padre
	 * @return				: Id de la cuenta padre
	 * @throws Throwable	: Excepcion
	 */
	 String obtieneCuentaPadre(String cuenta) throws Throwable;
	 

}

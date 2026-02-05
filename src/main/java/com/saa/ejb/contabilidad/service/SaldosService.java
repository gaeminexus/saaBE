package com.saa.ejb.contabilidad.service;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.model.cnt.Saldos;

import jakarta.ejb.Local;

@Local
public interface SaldosService{
	
	
	/**
	 * Método para verificar si se trata de una cuenta de primer nivel
	 * @param cuenta		: Saldos a verificar
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
	 * @param cuenta		: Saldos para buscar id del padre
	 * @return				: Id de la cuenta padre
	 * @throws Throwable	: Excepcion
	 */
	 String obtieneSaldosPadre(String cuenta) throws Throwable;

	Saldos saveSingle(Saldos registro);

	Object selectByCriteria(List<DatosBusqueda> registros);

	Object selectByCriteria1(List<DatosBusqueda> registros);
	 

}

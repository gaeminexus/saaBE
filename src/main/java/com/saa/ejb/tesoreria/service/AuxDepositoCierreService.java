package com.saa.ejb.tesoreria.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.AuxDepositoCierre;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad AuxDepositoCierre.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface AuxDepositoCierreService extends EntityService<AuxDepositoCierre>{
	
   
	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	AuxDepositoCierre selectById(Long id) throws Throwable;
	
	/**
	 * Recupera el auxiliar de cierre segun el usuario caja
	 * @param idUsuarioCaja	: Id de usuario caja
	 * @return				: Auxiliar de cierre recuperado
	 * @throws Throwable	: Excepcion
	 */
	List<AuxDepositoCierre> selectByUsuarioCaja(Long idUsuarioCaja) throws Throwable;
	
	/**
	 * Elimina los registros temporales por usuario por caja
	 * @param idUsuarioCaja	: Id de usuario caja
	 * @throws Throwable	: Excepcion
	 */
	void eliminaPorUsuarioCaja(Long idUsuarioCaja) throws Throwable;
	
	/**
	 * Inserta en tabla temporal todos los cierres sin depositar de un usuario por caja
	 * @param idUsuarioCaja	: Id de usuario caja
	 * @throws Throwable	: Excepcion
	 */
	void insertarCierresPendientes(Long idUsuarioCaja)throws Throwable;
}

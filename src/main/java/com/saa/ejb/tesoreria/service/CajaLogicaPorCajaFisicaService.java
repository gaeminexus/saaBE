package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.CajaLogicaPorCajaFisica;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad CajaLogicaPorCajaFisica.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface CajaLogicaPorCajaFisicaService extends EntityService<CajaLogicaPorCajaFisica>{
	
	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  CajaLogicaPorCajaFisica selectById(Long id) throws Throwable;
	
	 /**
	  * Recupera el numero de cajas logicas
	  * @param idCajaFisica: Id de caja Fisica
	  * @return			   : Vector con Numero de cajas logicas y id de caja logica
	  * @throws Throwable  : Excepcion
	  */
	  Long[] recuperaNumeroCajasLogicas(Long idCajaFisica) throws Throwable;
}

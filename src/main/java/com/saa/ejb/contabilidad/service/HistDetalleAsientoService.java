package com.saa.ejb.contabilidad.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.HistAsiento;
import com.saa.model.cnt.HistDetalleAsiento;

import jakarta.ejb.Local;

@Local
public interface HistDetalleAsientoService extends EntityService <HistDetalleAsiento> {

	 
	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  HistDetalleAsiento selectById(Long id) throws Throwable;
	 
	 /**
	  * Elimina los registros por codigo de mayorizacin
	  * @param idHistAsiento: Codigo de asiento histórico
	  * @throws Throwable	: Excepcion
	  */
	  void deleteByHistAsiento(Long idHistAsiento) throws Throwable; 
	 
	 /**
	  * Respalda la detalle de asientos de una mayorizacion en tabla histórica
	  * @param idAsiento		: Codigo del asiento original
	  * @param asientoRespaldo	: Asiento de respaldo
	  * @throws Throwable		: Excepcion
	  */
	  void respaldaDetalleByAsientos(Long idAsiento, HistAsiento asientoRespaldo) throws Throwable;

}

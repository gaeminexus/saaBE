package com.saa.ejb.contabilidad.service;

import com.saa.basico.util.EntityService;
import com.saa.model.contabilidad.HistAsiento;
import com.saa.model.contabilidad.HistDetalleAsiento;

import jakarta.ejb.Remote;

@Remote
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
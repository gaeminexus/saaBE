package com.saa.ejb.contabilidad.service;

import com.saa.basico.util.EntityService;
import com.saa.model.contabilidad.HistAsiento;
import com.saa.model.contabilidad.HistMayorizacion;

import jakarta.ejb.Local;

@Local
public interface HistAsientoService extends EntityService<HistAsiento> {


	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  HistAsiento selectById(Long id) throws Throwable;
	 
	 /**
	  * Elimina los registros por codigo de mayorizacin
	  * @param idDesmayorizacion: Codigo de desmayorizacion
	  * @throws Throwable		: Excepcion
	  */
	  void deleteByDesmayorizacion(Long idDesmayorizacion) throws Throwable; 
	 
	 /**
	  * Respalda la Cabecera de asientos de una mayorizacion en tabla histórica
	  * @param idMayorizacion	: Codigo de la mayorizacion a eliminar
	  * @param desmayorizacion	: Desmayorizacion generada para respaldar
	  * @throws Throwable		: Excepcion
	  */
	  void respaldaAsientosByMayorizacion(Long idMayorizacion, HistMayorizacion desmayorizacion) throws Throwable;	

}

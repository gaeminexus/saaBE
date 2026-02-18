package com.saa.ejb.cnt.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.HistMayorizacion;
import com.saa.model.cnt.Mayorizacion;

import jakarta.ejb.Local;

@Local
public interface HistMayorizacionService extends EntityService <HistMayorizacion> {
	 
	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  HistMayorizacion selectById(Long id) throws Throwable;
	 
	 /**
	  * Elimina los registros por codigo de desmayorizacion
	  * @param idDesmayorizacion: Codigo de desmayorizacion
	  * @throws Throwable		: Excepcion
	  */
	  void deleteByDesmayorizacion(Long idDesmayorizacion) throws Throwable;
	 
	 /**
	  * Almacena la entidad enviando el objeto
	  * @param tipo 			:Tipo de clase a almacenar
	  * @param id			:Id de la clase a almancer
	  * @throws Throwable	:Excepcion en caso de error
	  */
	  void save(HistMayorizacion histMayorizacion, Long id) throws Throwable;	
	 
	 /**
	  * Respalda la Cabecera de mayorizacion en tabla histórica
	  * @param aRespaldar	: Mayorizacion a respaldar
	  * @return				: Codigo de desmayorización generado
	  * @throws Throwable	: Excepcion
	  */
	  HistMayorizacion respaldaCabeceraMayorizacion(Mayorizacion aRespaldar) throws Throwable;

}

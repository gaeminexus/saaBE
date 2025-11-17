package com.saa.ejb.contabilidad.service;

import com.saa.basico.util.EntityService;
import com.saa.model.contabilidad.HistDetalleMayorizacion;

import jakarta.ejb.Remote;

@Remote
public interface HistDetalleMayorizacionService extends EntityService <HistDetalleMayorizacion> {
	
	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  HistDetalleMayorizacion selectById(Long id) throws Throwable;
	 
	 /**
	  * Elimina los registros por codigo de desmayorizacion
	  * @param idDesmayorizacion: Codigo de desmayorizacion
	  * @throws Throwable		: Excepcion
	  */
	  void deleteByDesmayorizacion(Long idDesmayorizacion) throws Throwable;	 
	 
	 /**
	  * Respalda el detalle de una mayorizacion
	 * @param mayorizacionRespaldar	: Id de la mayorizacion a respaldar
	 * @param histMayorizacion		: Id del histórico de mayorizacion generado
	 * @throws Throwable			: Excepcion
	 */
	 void respaldaDetalleMayorizacion(Long mayorizacionRespaldar, Long histMayorizacion) throws Throwable;

}

package com.saa.ejb.contabilidad.service;

import com.saa.basico.util.EntityService;
import com.saa.model.contabilidad.HistMayorizacion;
import com.saa.model.contabilidad.Mayorizacion;

import jakarta.ejb.Local;

@Local
public interface MayorizacionService extends EntityService<Mayorizacion> {

	 
	 /**
	 * Proceso que mayoriza un rango de periodos o un periodo
	 * @param empresa		: Id de la empresa a la que pertenecen los periodos a mayorizar
	 * @param periodoDesde	: Periodo desde el que se va a mayorizar
	 * @param periodoHasta	: Periodo hasta el que se mayoriza
	 * @param proceso		: Entero que indica el proceso a realizar
	 * @throws Throwable	: Excepcion
	 */
	 void mayorizacion(Long empresa, Long periodoDesde, Long periodoHasta, int proceso) throws Throwable;
	
	/**
	 * Crea un nuevo objeto en la entidad Mayorizacion
	 * @param periodo	: Periodo ligado a la mayorizacion
	 * @throws Throwable: Excepcion
	 */
	 void creaMayorizacion(Long periodo) throws Throwable;
	
	/**
	 * Obtiene la mayorizacion que pertenece a un periodo
	 * @param periodo	: Id del periodo a buscar
	 * @return			: Objeto de mayorizacion
	 * @throws Throwable: Excepcion
	 */
	 Mayorizacion obtieneMayorizacionPeriodo(Long periodo) throws Throwable;
	
	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	 Mayorizacion selectById(Long id) throws Throwable;
	
	/**
	 * Proceso que desmayoriza un rango de periodos o un periodo
	 * @param empresa		: Id de la empresa a la que pertenecen los periodos a desmayorizar
	 * @param periodoDesde	: Periodo desde el que se va a desmayorizar
	 * @param periodoHasta	: Periodo hasta el que se desmayoriza
	 * @param proceso		: Entero que indica el proceso a realizar
	 * @throws Throwable	: Excepcion
	 */
	 void desmayorizacion(Long empresa, Long periodoDesde, Long periodoHasta, int proceso) throws Throwable;
	
	/**
	 * Elimina los registros de las tablas de respaldo de la mayorizacion
	 * @param codigoDesmayorizacion	: Codigo de la Desmayorizacion a eliminar
	 * @throws Throwable			: Exception
	 */
	 void eliminaRespaldoMayorizacion(Long codigoDesmayorizacion) throws Throwable;
	
	/**
	 * Respalda todos los datos de mayorizacion en las tablas históricas
	 * @param codigoMayorizacion	: Codigo de la mayorizacion a eliminar
	 * @return						: Codigo de desmayorización generado
	 * @throws Throwable			: Excepcion
	 */
	 Long respaldaDatosMayorizacion(Long codigoMayorizacion) throws Throwable;
	
	/**
	 * Respalda todos los asientos de una mayorizacion en las tablas históricas
	 * @param codigoMayorizacion: Codigo de la mayorizacion a eliminar
	 * @param desmayorizacion	: Desmayorizacion generada para respaldar
	 * @throws Throwable		: Excepcion
	 */
	 void respaldaAsientosMayorizacion(Long codigoMayorizacion, HistMayorizacion desmayorizacion) throws Throwable;
	
	/**
	 * Elimina los registro de mayorizacion
	 * @param mayorizacion	: Id de mayorizacion a eliminal
	 * @throws Throwable	: Excepcion
	 */
	 void eliminaMayorizacionByMayorizacion(Long mayorizacion) throws Throwable;
	
	/**
	 * Elimina los registros por codigo de mayorizacin
	 * @param idMayorizacion	: Codigo de mayorizacion
	 * @throws Throwable		: Excepcion
	 */
	 void deleteByMayorizacion(Long idMayorizacion) throws Throwable;	 
	
}

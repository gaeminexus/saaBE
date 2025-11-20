package com.saa.ejb.contabilidad.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.contabilidad.DetalleMayorizacion;

import jakarta.ejb.Local;

@Local
public interface DetalleMayorizacionDaoService extends EntityDao<DetalleMayorizacion>  {

	/**
	 * Recupera el detalle perteneciente a una mayorizacion y a una cuenta
	 * @param mayorizacion	: Id de mayorizacion
	 * @param cuenta		: Id de cuenta
	 * @return				: Detalle de mayorizacion
	 * @throws Throwable	: Exception
	 */
	DetalleMayorizacion selectByMayorizacionCuenta(Long mayorizacion, Long cuenta) throws Throwable;
	
	/**
	 * Recupera los niveles incluidos en una mayorizacion
	 * @param mayorizacion	: Id de mayorizacion
	 * @return				: Niveles
	 * @throws Throwable	: Excepcion
	 */
	@SuppressWarnings("rawtypes")
	List selectNivelesByMayorizacion(Long mayorizacion) throws Throwable;
	
	/**
	 * Recupera las cuentas padres de un nivel de una mayorizacion
	 * @param mayorizacion	: Id de mayorizacion
	 * @param nivel			: Nivel
	 * @return				: Cuentas padre
	 * @throws Throwable	: Excepcion
	 */
	@SuppressWarnings("rawtypes")
	List selectPadresByNivel(Long mayorizacion, Long nivel) throws Throwable;
	
	/**
	 * Recupera por codigo de padre de una mayorizacion
	 * @param mayorizacion	: Id de mayorizacion
	 * @param nivel			: Nivel
	 * @param padre			: Id de la cuenta padre
	 * @return				: Listado de detalle de mayorizacion
	 * @throws Throwable	: Excepcion
	 */
	List<DetalleMayorizacion> selectByCuentaPadreNivel(Long mayorizacion, Long nivel, Long padre) throws Throwable;	
	
	/**
	 * Recupera los registros para procesar el asiento de cierre de un periodo
	 * @param mayorizacion	: Id de la mayorizacion
	 * @return				: Listado de registros
	 * @throws Throwable	: Excepcion
	 */
	List<DetalleMayorizacion> selectForCierre(Long mayorizacion) throws Throwable;
	
	/**
	 * Recupera datos por codigo de mayorizacion
	 * @param mayorizacion	: Id de mayorizacion
	 * @return				: Listado de registros
	 * @throws Throwable	: Excepcion
	 */
	List<DetalleMayorizacion> selectByCodigoMayorizacion(Long mayorizacion) throws Throwable;
	
	/**
	 * Elimina los registros por id de mayorizacion
	 * @param idMayorizacion	: Id de mayorizacion
	 * @throws Throwable		: Excepcion
	 */
	void deleteByMayorizacion(Long idMayorizacion) throws Throwable;
	
	/**
	 * Recupera datos de movimiento por codigo de mayorizacion
	 * @param mayorizacion	: Id de mayorizacion
	 * @return				: Listado de registros
	 * @throws Throwable	: Excepcion
	 */
	List<DetalleMayorizacion> selectMovimientosByMayorizacion(Long mayorizacion) throws Throwable;
	
	
}

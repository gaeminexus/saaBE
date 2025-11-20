/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.contabilidad.dao;
import java.util.List;
import com.saa.basico.util.EntityDao;
import com.saa.model.contabilidad.DetalleMayorizacionCC;
import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad DetalleMayorizacionCC.
 */
@Local
public interface DetalleMayorizacionCCDaoService extends EntityDao<DetalleMayorizacionCC> {

	/**
	 * Recupera datos por codigo de mayorizacionCC
	 * @param mayorizacionCC	: Id de mayorizacion
	 * @return					: Listado de registros
	 * @throws Throwable		: Excepcion
	 */
	List<DetalleMayorizacionCC> selectByCodigoMayorizacionCC(Long mayorizacionCC) throws Throwable;
	
	/**
	 * Recupera un registro por mayorizacion y centro de costo 
	 * @param mayorizacion	: Id de la mayorizacion
	 * @param cC			: Id del centro de costo
	 * @return				: Registro recuperado
	 * @throws Throwable	: Excepcion
	 */
	DetalleMayorizacionCC selectByMayorizacionAndCC(Long mayorizacion, Long cC) throws Throwable;
	
	/**
	 * Elimina los registros por id de mayorizacionCC
	 * @param idMayorizacion	: Id de mayorizacionCC
	 * @throws Throwable		: Excepcion
	 */
	void deleteByMayorizacionCC(Long idMayorizacionCC) throws Throwable;
	
}

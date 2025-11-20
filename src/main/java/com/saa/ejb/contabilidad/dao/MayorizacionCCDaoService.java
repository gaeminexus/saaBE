/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.contabilidad.dao;
import com.saa.basico.util.EntityDao;
import com.saa.model.contabilidad.MayorizacionCC;
import jakarta.ejb.Local;
/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad MayorizacionCC.
 */
@Local
public interface MayorizacionCCDaoService  extends EntityDao<MayorizacionCC>  {
	
	/**
	 * Elimina los registros por codigo de mayorizacionCC
	 * @param idMayorizacionCC	: Codigo de mayorizacionCC
	 * @throws Throwable		: Excepcion
	 */
	void deleteByMayorizacionCC(Long idMayorizacionCC) throws Throwable;
	
}

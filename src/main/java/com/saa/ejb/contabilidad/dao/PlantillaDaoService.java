/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.contabilidad.dao;
import java.util.List;
import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.Plantilla;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad Plantilla.
 */
@Local
public interface PlantillaDaoService extends EntityDao<Plantilla> {
	
	/**
	 * Recupera una instancia por el codigo alterno y la empresa
	 * @param alterno	: Codigo alterno
	 * @param empresa	: Id de la empresa
	 * @return			: Registro
	 * @throws Throwable: Excepcion
	 */
	List<Plantilla> selectByAlterno(int alterno, Long empresa) throws Throwable;
	
}

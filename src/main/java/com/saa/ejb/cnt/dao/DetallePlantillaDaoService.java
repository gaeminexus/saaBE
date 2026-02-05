/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.cnt.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.PlanCuenta;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad DetallePlantilla.
 */
@Local
public interface DetallePlantillaDaoService extends EntityDao<DetallePlantilla> {

	/**
	 * Recupera el detalle de plantilla para el cierre anual.
	 * @param plantilla	: Id de la plantilla  
	 * @return			: Listado de detalle plantilla
	 * @throws Throwable: Excepcion
	 */
	List<DetallePlantilla> selectByPlantilla(Long plantilla) throws Throwable;

	/**
	 * Recupera la cuenta contable del detalle de plantilla
	 * @param idDetallePlantilla: Id del detalle de plantilla
	 * @return					: Cuenta contable recuperada
	 * @throws Throwable		: Excepcion
	 */
	PlanCuenta recuperaCuentaContable(Long idDetallePlantilla) throws Throwable;
	
	/**
	 * Recupera el detalle de mayorizacion relacionado a una cuenta
	 * @param idPlanCuenta	:Id de la cuenta
	 * @return				:Detalle de mayorizacion
	 * @throws Throwable	:Excepcion
	 */
	List<DetallePlantilla> selectByIdPlanCuenta(Long idPlanCuenta) throws Throwable;

	
}

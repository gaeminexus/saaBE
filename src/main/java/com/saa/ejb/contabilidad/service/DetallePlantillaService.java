package com.saa.ejb.contabilidad.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.PlanCuenta;

import jakarta.ejb.Local;

@Local
public interface DetallePlantillaService extends EntityService<DetallePlantilla> {


	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	DetallePlantilla selectById(Long id) throws Throwable;
	
	/**
	 * Recupera el registro de la cuenta para el cierre de año
	 * @param plantilla	: Id de la plantilla de cierre
	 * @return			: Detalle plantilla
	 * @throws Throwable: Excepcion
	 */
	DetallePlantilla recuperaDetellaForCierre(Long plantilla) throws Throwable;

	/**
	 * Recupera la cuenta contable del detalle de plantilla
	 * @param idDetallePlantilla: Id del detalle de plantilla
	 * @return					: Cuenta contable recuperada
	 * @throws Throwable		: Excepcion
	 */
	PlanCuenta recuperaCuentaContable(Long idDetallePlantilla) throws Throwable;
	
	/**
	 * Almacena el detalle de plantilla a partir de la Entidad 
	 * @param detallePlantilla	: Entidad Detalle de plantilla
	 * @throws Throwable		: Excepcion
	 */
	void save(DetallePlantilla detallePlantilla) throws Throwable;
}

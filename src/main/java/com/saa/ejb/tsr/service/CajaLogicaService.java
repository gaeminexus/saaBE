package com.saa.ejb.tsr.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.tsr.CajaLogica;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad CajaLogica.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface CajaLogicaService extends EntityService<CajaLogica>{


	 /**
	  * Recupera la cuenta contable de una caja
	  * @param idCaja	: Id de la caja
	  * @return			: Cuenta contable recuperada
	  * @throws Throwable: Excepcion
	  */
	  PlanCuenta recuperaCuentaContable(Long idCaja) throws Throwable;
}

package com.saa.ejb.contabilidad.service;

import com.saa.basico.util.EntityService;
import com.saa.model.contabilidad.NaturalezaCuenta;

import jakarta.ejb.Remote;

@Remote
public interface NaturalezaCuentaService extends EntityService<NaturalezaCuenta> {


	 /**
	 * Recupera el id de la naturaleza de cuenta de la cuenta enviada
	 * @param cuenta		: Cuenta de la que se busca
	 * @param empresa		: Id de la empresa
	 * @return				: Id naturaleza cuenta
	 * @throws Throwable	: Excepcion
	 */
	 NaturalezaCuenta recuperaNaturalezaByCuenta(String cuenta, Long empresa) throws Throwable;	
	
	
	/**
	 * Método que valida la existencia de naturalezas de cuentas básicas en una empresa 
	 * @param empresa		: Empresa en la que se valida
	 * @throws Throwable	: Excepcion
	 */
	 String validaExisteNaturalezasBasicas(Long empresa) throws Throwable;
	
	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	 NaturalezaCuenta selectById(Long id) throws Throwable;

}

package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.DebitoCredito;
import com.saa.model.tesoreria.DetalleDebitoCredito;
import com.saa.model.tesoreria.TempDebitoCredito;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad DetalleDebitoCredito.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface DetalleDebitoCreditoService extends EntityService<DetalleDebitoCredito>{
	
	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	DetalleDebitoCredito selectById(Long id) throws Throwable;
	
	/**
	 * Inserta detalle de debito-credito
	 * @param idDebitoCredito	: Debito-Credito
	 * @param tempDebitoCredito	: Temporal  de Detalle de Debito-Credito
	 * @throws Throwable		: Excepcion
	 */
	void insertarDetalleDebitoCredito(DebitoCredito debitoCredito, TempDebitoCredito tempDebitoCredito)throws Throwable;
	
}

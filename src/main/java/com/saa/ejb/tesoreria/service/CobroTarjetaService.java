package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.Cobro;
import com.saa.model.tsr.CobroTarjeta;
import com.saa.model.tsr.TempCobroTarjeta;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad CobroTarjeta.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface CobroTarjetaService extends EntityService<CobroTarjeta>{
	
	/**
	 * Almacena el cobro con tarjeta real de un cobro realizado
	 * @param tempCobroTarjeta	: Entidad TempCobroTarjeta
	 * @param cobro			: Entidad del cobro
	 * @throws Throwable		: Excepcion
	 */
	void saveCobroTarjetaReal(TempCobroTarjeta tempCobroTarjeta, Cobro cobro) throws Throwable;
	  
	/**
	 * Recupera la suma del valor de tarjeta
	 * @param idCobro	: Id de cobro
	 * @return			: Suma de tarjetas cobradas
	 * @throws Throwable: Excepcion
	 */
	Double sumaValorTarjeta(Long idCobro) throws Throwable;
}

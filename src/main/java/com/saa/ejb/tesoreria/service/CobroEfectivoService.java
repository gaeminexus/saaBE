package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.Cobro;
import com.saa.model.tsr.CobroEfectivo;
import com.saa.model.tsr.TempCobroEfectivo;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad CobroEfectivo.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface CobroEfectivoService extends EntityService<CobroEfectivo>{
	
	/**
	 * Almacena el cobro con efectivo real de un cobro realizado
	 * @param tempCobroEfectivo: Entidad TempCobroEfectivo
	 * @param cobro			: Entidad del cobro
	 * @throws Throwable		: Excepcion
	 */
	void saveCobroEfectivoReal(TempCobroEfectivo tempCobroEfectivo, Cobro cobro) throws Throwable;
	
	/**
	 * Recupera la suma del valor de efectivo
	 * @param idCobro	: Id de cobro
	 * @return			: Suma de efectivo cobrados
	 * @throws Throwable: Excepcion
	 */
	Double sumaValorEfectivo(Long idCobro) throws Throwable;
}

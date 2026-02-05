package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.Cobro;
import com.saa.model.tsr.CobroTransferencia;
import com.saa.model.tsr.TempCobroTransferencia;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad CobroTransferencia.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface CobroTransferenciaService extends EntityService<CobroTransferencia>{

	/**
	 * Almacena el cobro con transferencia real de un cobro realizado
	 * @param tempCobroTransferencia: Entidad TempCobroTransferencia
	 * @param cobro				: Entidad del cobro
	 * @throws Throwable			: Excepcion
	 */
	void saveCobroTransferenciaReal(TempCobroTransferencia tempCobroTransferencia, Cobro cobro) throws Throwable;
	
	/**
	 * Recupera la suma del valor de transferencia
	 * @param idCobro	: Id de cobro
	 * @return			: Suma de transferencias cobradas
	 * @throws Throwable: Excepcion
	 */
	Double sumaValorTransferencia(Long idCobro) throws Throwable;
}

package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.Cobro;
import com.saa.model.tesoreria.MotivoCobro;
import com.saa.model.tesoreria.TempMotivoCobro;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad MotivoCobro.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Remote
public interface MotivoCobroService extends EntityService<MotivoCobro>{
 	 
	/**
	 * Almacena el motivo de cobro real de un cobro realizado
	 * @param tempMotivoCobro	: Entidad TempMotivoCobro
	 * @param cobro				: Entidad del cobro
	 * @throws Throwable		: Excepcion
	 */
	void saveMotivoCobroReal(TempMotivoCobro tempMotivoCobro, Cobro cobro) throws Throwable;
}
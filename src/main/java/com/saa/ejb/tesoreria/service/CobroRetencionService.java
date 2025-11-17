package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.Cobro;
import com.saa.model.tesoreria.CobroRetencion;
import com.saa.model.tesoreria.TempCobroRetencion;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad CobroRetencion.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Remote
public interface CobroRetencionService extends EntityService<CobroRetencion>{
 	 
	/**
	 * Almacena el cobro con retencion real de un cobro realizado
	 * @param tempCobroRetencion: Entidad TempCobroRetencion
	 * @param cobro			: Entidad del cobro
	 * @throws Throwable		: Excepcion
	 */
	void saveCobroRetencionReal(TempCobroRetencion tempCobroRetencion, Cobro cobro) throws Throwable;
	
	/**
	 * Recupera la suma del valor de retenciones
	 * @param idCobro	: Id de cobro
	 * @return			: Suma de retenciones cobrados
	 * @throws Throwable: Excepcion
	 */
	Double sumaValorRetencion(Long idCobro) throws Throwable;
}
package com.saa.ejb.cnt.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.Plantilla;

import jakarta.ejb.Local;

@Local
public interface PlantillaService extends EntityService <Plantilla> {
	
 	
	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	Plantilla selectById(Long id) throws Throwable;
	
	/**
	 * Recupera el codigo de la plantilla por codigo alterno en una empresa
	 * @param alterno	: Codigo alterno
	 * @param empresa	: Id de empresa
	 * @return			: Codigo del tipo de asiento
	 * @throws Throwable: Excepcion
	 */
	Long codigoByAlterno(int alterno, Long empresa) throws Throwable;
	
	/**
	 * Actualiza el estado de la plantilla y del detalle de plantilla
	 * @param idPlantilla	: Id de plantilla
	 * @param estado		: Estado
	 * @throws Throwable	: Excepcion
	 */
	void actualizaEstado(Long idPlantilla, Long estado)throws Throwable;

}

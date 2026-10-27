package com.saa.ejb.cnt.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.TipoAsiento;

import jakarta.ejb.Local;

@Local
public interface TipoAsientoService extends EntityService<TipoAsiento> {
	
 	 
	 /**
	 * Recupera entidad con el id
	 * @param id		: Id de la entidad
	 * @return			: Recupera entidad
	 * @throws Throwable: Excepcion
	 */
	TipoAsiento selectById(Long id) throws Throwable;
	
	/**
	 * Metodo que graba registros con parametro de empresa
	 * 
	 * @param object		: Arreglo de objetos a persistir
	 * @throws Throwable	: Excepcion en caso de error
	 */
	void save(Object[][] object, Object[] campos, Long empresa) throws Throwable;

	/**
	 * Metodo que realiza el select para los combos tipoEmpresa
	 * @param empresa	: Id de la empresa en la que se realiza la búsqueda
	 * @return			: Arreglo de objetos con resultado
	 * @throws Throwable: Excepcion
	 */
	 Object[][] comboTipoAsientoSelect(Long empresa) throws Throwable;
	
	/**
	 * Método que valida la existencia de tipos de asiento en una empresa 
	 * @param empresa	: Empresa en la que se valida
	 * @throws Throwable: Excepcion
	 */
	 String validaExisteTipoAsiento(Long empresa) throws Throwable;
	
	/**
	 * Recupera el codigo del tipo de asiento por alterno en una empresa
	 * @param alterno	: Codigo alterno
	 * @param empresa	: Id de empresa
	 * @return			: Codigo del tipo de asiento
	 * @throws Throwable: Excepcion
	 */
	 Long codigoByAlterno(int alterno, Long empresa) throws Throwable;
	

	/**
	 * Tipo de asiento AUTOMATICO (IN-A 7, EG-A 8, DI-A 9, ver {@code TipoAsientoAutomatico}) de una empresa:
	 * busca por codigo alterno Y {@code PLNTSSTM = 1}, no por el metodo viejo {@link #codigoByAlterno}.
	 * @param alterno	: Codigo alterno (7, 8 o 9)
	 * @param empresa	: Id de la empresa
	 * @return			: Tipo de asiento
	 * @throws Throwable: IncomeException si la empresa no tiene ese tipo del sistema
	 */
	com.saa.model.cnt.TipoAsiento tipoAutomatico(int alterno, Long empresa) throws Throwable;

}

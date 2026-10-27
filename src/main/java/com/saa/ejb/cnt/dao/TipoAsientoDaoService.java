/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.cnt.dao;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.TipoAsiento;

import jakarta.ejb.Local;
 
/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad TipoAsiento.
 */
@Local
public interface TipoAsientoDaoService  extends EntityDao<TipoAsiento>  {

	/**
	 * Recupera una instancia por el codigo alterno y la empresa
	 * @param alterno	: Codigo alterno
	 * @param empresa	: Id de la empresa
	 * @return			: Registro
	 * @throws Throwable: Excepcion
	 */
	List<TipoAsiento> selectByAlterno(int alterno, Long empresa) throws Throwable;
	
	/**
	 * Recupera los tipos de asientos que no tienen codigo alterno
	 * @param empresa	: Id de la empresa
	 * @return			: Listado de tipos de asiento sin alterno
	 * @throws Throwable: Excepcion
	 */
	List<TipoAsiento> selectByEmpresaSinAlterno(Long empresa) throws Throwable;

	/**
	 * Tipos de asiento de una empresa con el codigo alterno indicado Y {@code PLNTSSTM = 1} (tipo del
	 * sistema). Es la busqueda que usan los tipos automaticos IN-A / EG-A / DI-A (alternos 7, 8 y 9): el
	 * alterno 7 ya lo usa TipoAsientos.TRANSFERENCIAS, por eso aqui se exige que sea del sistema.
	 * {@link #selectByAlterno(int, Long)} no se toca.
	 * @param alterno	: Codigo alterno
	 * @param empresa	: Id de la empresa
	 * @return			: Tipos encontrados (normalmente 0 o 1)
	 * @throws Throwable: Excepcion
	 */
	List<TipoAsiento> selectByAlternoSistema(int alterno, Long empresa) throws Throwable;
}

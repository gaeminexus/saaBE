/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.cnt.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.SubdetalleAsiento;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad SubdetalleAsiento.
 */
@Local
public interface SubdetalleAsientoDaoService extends EntityDao<SubdetalleAsiento> {

	/**
	 * Recupera los subdetalles de asiento relacionados a un detalle de asiento
	 * @param idDetalleAsiento	: Id del detalle de asiento
	 * @return					: Lista de subdetalles de asiento
	 * @throws Throwable		: Excepcion
	 */
	List<SubdetalleAsiento> selectByIdDetalleAsiento(Long idDetalleAsiento) throws Throwable;
	
	/**
	 * Recupera los subdetalles de asiento por código de activo
	 * @param codigoActivo		: Código del activo fijo
	 * @return					: Lista de subdetalles de asiento
	 * @throws Throwable		: Excepcion
	 */
	List<SubdetalleAsiento> selectByCodigoActivo(String codigoActivo) throws Throwable;
	
	/**
	 * Recupera los subdetalles de asiento por categoría
	 * @param categoria			: Categoría del activo
	 * @return					: Lista de subdetalles de asiento
	 * @throws Throwable		: Excepcion
	 */
	List<SubdetalleAsiento> selectByCategoria(String categoria) throws Throwable;
	
	/**
	 * Recupera los subdetalles de asiento por responsable
	 * @param responsable		: Responsable o custodio del activo
	 * @return					: Lista de subdetalles de asiento
	 * @throws Throwable		: Excepcion
	 */
	List<SubdetalleAsiento> selectByResponsable(String responsable) throws Throwable;
}

package com.saa.ejb.cnt.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.SubdetalleAsiento;

import jakarta.ejb.Local;

@Local
public interface SubdetalleAsientoService extends EntityService<SubdetalleAsiento> {
	
	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	SubdetalleAsiento selectById(Long id) throws Throwable;
	
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
	
	/**
	 * Almacena el subdetalle de asiento y devuelve id
	 * @param subdetalleAsiento	: Entidad Subdetalle asiento
	 * @return					: Id de subdetalle de asiento
	 * @throws Throwable		: Excepcion
	 */
	Long saveSubdetalle(SubdetalleAsiento subdetalleAsiento) throws Throwable;
	
	/**
	 * Calcula el valor neto en libros del activo
	 * @param subdetalleAsiento	: Entidad Subdetalle asiento
	 * @return					: Valor neto en libros calculado
	 * @throws Throwable		: Excepcion
	 */
	Double calcularValorNetoLibros(SubdetalleAsiento subdetalleAsiento) throws Throwable;
	
	/**
	 * Calcula la base a depreciar del activo
	 * @param subdetalleAsiento	: Entidad Subdetalle asiento
	 * @return					: Base a depreciar calculada
	 * @throws Throwable		: Excepcion
	 */
	Double calcularBaseDepreciar(SubdetalleAsiento subdetalleAsiento) throws Throwable;
}

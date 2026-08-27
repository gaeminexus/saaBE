package com.saa.ejb.tsr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.CajaChica;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * Dao Service CajaChica.
 */
@Local
public interface CajaChicaDaoService extends EntityDao<CajaChica> {

	/**
	 * Cajas chicas de una empresa en un estado dado.
	 * @param idEmpresa : Id de la empresa
	 * @param estado    : Estado (rubro EstadoCajaChica), null para todas
	 * @return          : Cajas chicas
	 * @throws Throwable: Excepcion
	 */
	List<CajaChica> selectByEmpresaEstado(Long idEmpresa, Long estado) throws Throwable;

	/**
	 * Indica si ya existe una caja chica con ese nombre en la empresa.
	 * @param idEmpresa  : Id de la empresa
	 * @param nombre     : Nombre a validar
	 * @param idExcluir  : Id de caja a excluir de la validación (edición), null si no aplica
	 * @return           : true si ya existe otra caja con ese nombre
	 * @throws Throwable : Excepcion
	 */
	boolean existeNombreEnEmpresa(Long idEmpresa, String nombre, Long idExcluir) throws Throwable;

}

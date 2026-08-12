package com.saa.ejb.tsr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.Ingreso;

import jakarta.ejb.Local;

@Local
public interface IngresoDaoService extends EntityDao<Ingreso> {

	/**
	 * Recupera los ingresos de una empresa, filtrando opcionalmente por estado.
	 * @param idEmpresa  : Id de la empresa
	 * @param estado     : Estado del ingreso, null para todos
	 * @return           : Listado de ingresos
	 * @throws Throwable : Excepcion
	 */
	List<Ingreso> selectByEmpresaEstado(Long idEmpresa, Long estado) throws Throwable;
}

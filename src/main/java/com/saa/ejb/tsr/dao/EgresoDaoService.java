package com.saa.ejb.tsr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.Egreso;

import jakarta.ejb.Local;

@Local
public interface EgresoDaoService extends EntityDao<Egreso> {

	/**
	 * Recupera los egresos de una empresa, filtrando opcionalmente por estado.
	 * @param idEmpresa  : Id de la empresa
	 * @param estado     : Estado del egreso, null para todos
	 * @return           : Listado de egresos
	 * @throws Throwable : Excepcion
	 */
	List<Egreso> selectByEmpresaEstado(Long idEmpresa, Long estado) throws Throwable;
}

package com.saa.ejb.cxp.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxp.LotePago;

import jakarta.ejb.Local;

@Local
public interface LotePagoDaoService extends EntityDao<LotePago> {

	/**
	 * Recupera los lotes de pago de una empresa, del más reciente al más antiguo.
	 * @param idEmpresa  : Id de la empresa
	 * @return           : Listado de lotes
	 * @throws Throwable : Excepcion
	 */
	List<LotePago> selectByEmpresa(Long idEmpresa) throws Throwable;
}

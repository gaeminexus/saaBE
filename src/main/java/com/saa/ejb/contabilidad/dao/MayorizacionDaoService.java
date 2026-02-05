package com.saa.ejb.contabilidad.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.Mayorizacion;

import jakarta.ejb.Local;

@Local
public interface MayorizacionDaoService  extends EntityDao<Mayorizacion>  {
	
	/**
	 * Elimina los registros por codigo de mayorizacin
	 * @param idMayorizacion	: Codigo de mayorizacion
	 * @throws Throwable		: Excepcion
	 */
	void deleteByMayorizacion(Long idMayorizacion) throws Throwable;
	
	/**
	 * Recupera la mayorizacion por periodo
	 * @param idPeriodo: Id del periodo
	 * @return:	Lista de Mayorizacion
	 * @throws Throwable: si ocurre algun error	
	 */
	List<Mayorizacion> selectByPeriodo(Long idPeriodo) throws Throwable;
	
}

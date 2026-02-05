package com.saa.ejb.contabilidad.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.DetalleMayorAnalitico;

import jakarta.ejb.Local;

@Local
public interface DetalleMayorAnaliticoDaoService  extends EntityDao<DetalleMayorAnalitico>  {
	
	/**
	 * Recupera los detalles del mayor analitico por id del mayor analitico
	 * @param idMayorAnalitico: id del mayor analitico
	 * @return: 	Lista de DetalleMayorAnalitico
	 * @throws Throwable: si ocurre algun error
	 */
	List<DetalleMayorAnalitico> selectByIdMayorAnalitico(Long idMayorAnalitico) throws Throwable;
	
}

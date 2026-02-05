package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Aporte;

import jakarta.ejb.Local;

@Local
public interface AporteDaoService extends EntityDao<Aporte>{
	
	
	/*filtra todos los aporte por id de entidad
	 * @param :idEntidad
	 * @return Lista de Aporte
	 */
	List<Aporte> selectByEntidad(Long idEntidad) throws Throwable;
	
	Long selectCountByEntidad(Long idEntidad) throws Throwable;

}

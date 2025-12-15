package com.saa.ejb.credito.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.Direccion;

import jakarta.ejb.Local;

@Local
public interface DireccionDaoService extends EntityDao<Direccion> {
	
	
	/** Select dirección by parent idIdentidad
	 * @param idIdentidad
	 * @return lista de direcciones
	 */
	List<Direccion> selectByParent(Long idIdentidad);

}

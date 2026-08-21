package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.DetalleFormatoMarcacion;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService DetalleFormatoMarcacion.
 */
@Local
public interface DetalleFormatoMarcacionDaoService extends EntityDao<DetalleFormatoMarcacion> {

	/**
	 * Recupera los campos activos de un formato, en el orden en que se leen.
	 *
	 * @param idFormato		: Id del formato de marcacion
	 * @return				: Campos del formato ordenados
	 * @throws Throwable	: Excepcion
	 */
	List<DetalleFormatoMarcacion> selectByFormato(Long idFormato) throws Throwable;

}

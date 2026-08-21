package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.DetalleFormatoBancario;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService DetalleFormatoBancario.
 */
@Local
public interface DetalleFormatoBancarioDaoService extends EntityDao<DetalleFormatoBancario> {

	/**
	 * Recupera los campos activos de un formato, en el orden en que se escriben.
	 *
	 * @param idFormato		: Id del formato bancario
	 * @return				: Campos del formato ordenados
	 * @throws Throwable	: Excepcion
	 */
	List<DetalleFormatoBancario> selectByFormato(Long idFormato) throws Throwable;

}

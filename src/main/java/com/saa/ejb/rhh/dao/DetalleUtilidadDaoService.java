package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.DetalleUtilidad;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService DetalleUtilidad.
 */
@Local
public interface DetalleUtilidadDaoService extends EntityDao<DetalleUtilidad> {

	/**
	 * Recupera el detalle de un reparto, ordenado por beneficiario.
	 *
	 * @param idUtilidad	: Id del reparto
	 * @return				: Detalle del reparto
	 * @throws Throwable	: Excepcion
	 */
	List<DetalleUtilidad> selectByUtilidad(Long idUtilidad) throws Throwable;

	/**
	 * Elimina el detalle de un reparto. Lo usa el recalculo.
	 *
	 * @param idUtilidad	: Id del reparto
	 * @return				: Numero de filas eliminadas
	 * @throws Throwable	: Excepcion
	 */
	int eliminaByUtilidad(Long idUtilidad) throws Throwable;

}

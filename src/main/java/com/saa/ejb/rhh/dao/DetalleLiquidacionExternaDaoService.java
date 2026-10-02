package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.DetalleLiquidacionExterna;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService DetalleLiquidacionExterna.
 */
@Local
public interface DetalleLiquidacionExternaDaoService extends EntityDao<DetalleLiquidacionExterna> {

	/**
	 * Recupera el detalle de una liquidacion, ordenado por orden y luego codigo.
	 *
	 * @param idLiquidacion	: Id de la liquidacion
	 * @return				: Detalle de la liquidacion
	 * @throws Throwable	: Excepcion
	 */
	List<DetalleLiquidacionExterna> selectByLiquidacion(Long idLiquidacion) throws Throwable;

	/**
	 * Elimina el detalle de una liquidacion.
	 *
	 * <p>Lo usa <code>actualizar</code>: reemplaza todos los detalles borrando y
	 * volviendo a armar, mismo patron que
	 * <code>DetalleOrdenPagoNominaDaoService.eliminaByOrdenPago</code>.</p>
	 *
	 * @param idLiquidacion	: Id de la liquidacion
	 * @return				: Numero de filas eliminadas
	 * @throws Throwable	: Excepcion
	 */
	int eliminaByLiquidacion(Long idLiquidacion) throws Throwable;

}

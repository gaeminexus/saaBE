package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.DetalleOrdenPagoNomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService DetalleOrdenPagoNomina.
 */
@Local
public interface DetalleOrdenPagoNominaDaoService extends EntityDao<DetalleOrdenPagoNomina> {

	/**
	 * Recupera el detalle de una orden de pago, ordenado por beneficiario.
	 *
	 * @param idOrdenPago	: Id de la orden de pago
	 * @return				: Detalle de la orden
	 * @throws Throwable	: Excepcion
	 */
	List<DetalleOrdenPagoNomina> selectByOrdenPago(Long idOrdenPago) throws Throwable;

	/**
	 * Elimina el detalle de una orden de pago.
	 *
	 * <p>Lo usa la regeneracion de la orden mientras esta sin acreditar: se borra y se
	 * vuelve a armar, para que un cambio de cuenta bancaria del empleado se refleje.</p>
	 *
	 * @param idOrdenPago	: Id de la orden de pago
	 * @return				: Numero de filas eliminadas
	 * @throws Throwable	: Excepcion
	 */
	int eliminaByOrdenPago(Long idOrdenPago) throws Throwable;

}

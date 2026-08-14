package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.PagoAporte;

import jakarta.ejb.Local;

@Local
public interface PagoAporteDaoService extends EntityDao<PagoAporte> {

	// ========================================================================
	// SERVICIOS DE PAGO DE PRÉSTAMOS (§5.2 ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md)
	// ========================================================================

	/**
	 * Pagos de aporte vinculados a un pago de préstamo, ordenados por código ASC.
	 * El reverso de un PAGO_APORTES los recorre para generar los contra-movimientos en APRT.
	 *
	 * @param codigoPagoPrestamo Código del PagoPrestamo (PGPR)
	 * @return Lista de PagoAporte (vacía si no hay registros o si falla la consulta)
	 * @throws Throwable Si ocurre un error
	 */
	List<PagoAporte> selectByPagoPrestamo(Long codigoPagoPrestamo) throws Throwable;

}

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

	/**
	 * El o los PagoAporte generados para UN Aporte puntual (1:1 en la práctica —
	 * {@code DevolucionAporteServiceImpl.crearPagoAporteDevolucion} genera exactamente uno por
	 * fila de Aporte, pero la relación en el modelo no lo garantiza como UNIQUE).
	 *
	 * @param idAporte : Código del aporte (CRD.APRT)
	 * @return         : Listado; normalmente una sola fila
	 * @throws Throwable : Excepcion
	 */
	List<PagoAporte> selectByAporte(Long idAporte) throws Throwable;

}

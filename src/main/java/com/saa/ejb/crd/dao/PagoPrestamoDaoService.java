package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.PagoPrestamo;

import jakarta.ejb.Local;


@Local
public interface PagoPrestamoDaoService extends EntityDao<PagoPrestamo> {

	/**
	 * Busca todos los pagos asociados a un DetallePrestamo específico
	 *
	 * @param codigoDetallePrestamo Código del DetallePrestamo
	 * @return Lista de PagoPrestamo encontrados (vacía si no hay registros)
	 */
	List<PagoPrestamo> selectByIdDetallePrestamo(Long codigoDetallePrestamo);

	// ========================================================================
	// SERVICIOS DE PAGO DE PRÉSTAMOS (§5.2 ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md)
	// ========================================================================

	/**
	 * Pagos VIGENTES (anulado IS NULL OR anulado = 0) de una cuota, ordenados por código ASC.
	 * REEMPLAZA el uso directo de selectByIdDetallePrestamo en el motor de pagos nuevo:
	 * los pagos anulados por un reverso NO deben contar en la reconstrucción de saldos.
	 *
	 * @param codigoDetallePrestamo Código del DetallePrestamo
	 * @return Lista de PagoPrestamo vigentes (vacía si no hay registros o si falla la consulta)
	 */
	List<PagoPrestamo> selectVigentesByIdDetallePrestamo(Long codigoDetallePrestamo);

	/**
	 * Pagos de un evento (para anulación/consulta), ordenados por código ASC.
	 *
	 * @param codigoEvento Código del EventoPrestamo (EVPR)
	 * @return Lista de PagoPrestamo del evento (vacía si no hay registros o si falla la consulta)
	 */
	List<PagoPrestamo> selectByEvento(Long codigoEvento);

	/**
	 * Cuenta los pagos VIGENTES (anulado IS NULL OR anulado = 0) de una cuota.
	 * El reverso del abono a capital lo usa para verificar que ninguna cuota generada por
	 * el evento tenga pagos antes de borrarla.
	 *
	 * @param codigoDetallePrestamo Código del DetallePrestamo
	 * @return Cantidad de pagos vigentes (0 si no hay o si falla la consulta)
	 */
	Long contarVigentesByIdDetallePrestamo(Long codigoDetallePrestamo);

}

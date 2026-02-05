package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.DetallePrestamo;

import jakarta.ejb.Local;

@Local
public interface DetallePrestamoService extends EntityService<DetallePrestamo> {
	
	/**
	 * Consulta los detalles de préstamo por mes y año.
	 * @param mes: Mes del detalle de préstamo.
	 * @param anio: Año del detalle de préstamo.
	 * @return: Lista de DetallePrestamo que coinciden con el mes y año especificados.
	 * @throws Throwable: Si ocurre algún error durante la consulta.
	 */
	List<DetallePrestamo> selectByMesAnio(Long mes, Long anio) throws Throwable;

}
